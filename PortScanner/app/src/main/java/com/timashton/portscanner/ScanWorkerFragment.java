package com.timashton.portscanner;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
 * Created by Tim Ashton on 12/05/15.
 */


public class ScanWorkerFragment extends Fragment {

    private static final String TAG = ScanWorkerFragment.class.getName();

    private static final int MAX_PORTS_PER_THREAD = 20;

    private static ScannerThread mScannerThread;
    private static ScanFragmentCallbacks mCallbacks;
    private static PortFoundHandler mPortFoundHandler;
    private static Handler mScanDoneHandler;

    public interface ScanFragmentCallbacks {
        void addPortFound(String portFoundResult);
        void scanFinished();
        void scanStarting();
        void scanStopping();
    }


    public static ScanWorkerFragment newInstance() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "newInstance()");
        }
        return new ScanWorkerFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onAttach()");
        }
        try {
            mCallbacks = (ScanFragmentCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ScanFragmentCallbacks");
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onCreate()");
        }
        setRetainInstance(true);

        if (savedInstanceState == null) {
            mPortFoundHandler = new PortFoundHandler(this);
            mScanDoneHandler = new Handler() {
                public void handleMessage(Message msg) {
                    mCallbacks.scanFinished();
                }
            };
        }
    }

    /*
     * Call the onPause() of the DemoThread to pause execution
     * of the thread.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onPause()");
        }
        if ((mScannerThread != null) && !mScannerThread.mFinished) {
            mScannerThread.onPause();
        }
    }

    /*
     * Call the onResume() of the DemoThread to resume execution
     * of the thread.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onResume()");
        }
        if ((mScannerThread != null) && !mScannerThread.mFinished) {
            mScannerThread.onResume();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onDetach()");
        }
        mScannerThread = null;
        mCallbacks = null;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onDestroy()");
        }
    }

    private static class PortFoundHandler extends Handler {

        private final WeakReference<ScanWorkerFragment> myClassWeakReference;

        public PortFoundHandler(ScanWorkerFragment myClassInstance) {
            myClassWeakReference = new WeakReference<>(myClassInstance);
        }

        @Override
        public void handleMessage(Message msg) {
            ScanWorkerFragment myClass = myClassWeakReference.get();
            if (myClass != null) {
                mCallbacks.addPortFound(msg.obj.toString());
            }
        }
    }

    public void startScannerThread(String hostName, int startPort, int endPort, int avgPing) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "startScannerThread()");
        }

        //Prepare lists of port numbers
        List<Integer> ports = new ArrayList<>();
        for (int i = startPort; i <= endPort; i++) {
            ports.add(i);
        }
        List<List<Integer>> portSubList = subList(ports, MAX_PORTS_PER_THREAD);

        // Create the new boss thread
        mScannerThread = new ScannerThread(hostName, portSubList, avgPing);
        mScannerThread.start();
        mCallbacks.scanStarting();
    }


    /*
     * Class ScannerThread
     *
     * Inner thread class to run the boss thread by overriding the thread.run method.
     * Make static because there should only ever be one of these running in the port
     * scanner application at any time.
     */
    public static class ScannerThread extends Thread implements ScannerRunnable.ScanRunnableCallbacks {

        private static final String INNER_TAG = ScannerThread.class.getName();
        private static final int MAX_SOCK_TIMEOUT = 1250; //maximum allowable timeout 1.25 sec.
        private static final double TIMEOUT_THRESHOLD_MULTIPLIER = 1.5; // accommodates a lot of jitter.

        @SuppressWarnings("")
        private final Object mPauseLock;
        private boolean mPaused;
        private boolean mFinished;

        private String threadHostname;
        private int threadSockTimeout;
        private List<List<Integer>> threadPorts;

        public ScannerThread(String hostName, List<List<Integer>> ports, int avgPing) {
            if (BuildConfig.DEBUG) {
                Log.i(INNER_TAG, "ScannerThread()");
            }
            threadHostname = hostName;
            threadPorts = ports;
            threadSockTimeout = (int)(avgPing*TIMEOUT_THRESHOLD_MULTIPLIER);
            threadSockTimeout = threadSockTimeout > MAX_SOCK_TIMEOUT
                    ? MAX_SOCK_TIMEOUT : threadSockTimeout;

            mPauseLock = new Object();
            mPaused = false;
            mFinished = false;
        }

        @SuppressWarnings("unused")
        private ScannerThread() {
            mPauseLock = new Object();
            mPaused = false;
            mFinished = false;
        }

        @Override
        public void run() {
            if (BuildConfig.DEBUG) {
                Log.i(INNER_TAG, "run()");
            }
            Map<Integer, ScannerRunnable> scannerRunnables = new HashMap<>();
            ExecutorService es = Executors.newCachedThreadPool();

            // Create and add Scanner Runnables to the ExecutorService.
            for (int i = 0; i < threadPorts.size(); i++) {
                scannerRunnables.put(i,
                        new ScannerRunnable(threadHostname, threadPorts.get(i), this, threadSockTimeout));

                es.execute(scannerRunnables.get(i));
            }
            es.shutdown();

            try {
                // TODO allow configurable timeout
                // TODO Allow to be cancelled

                while (!es.awaitTermination(10, TimeUnit.MINUTES)) {
                    synchronized (mPauseLock) {
                        while (mPaused) {

                            mPauseLock.wait();
                        }
                    }
                }
            } catch (InterruptedException e) {
                // May have been cancelled.
                Log.e(TAG, e.toString());
            }
            mCallbacks.scanStopping();
            mScanDoneHandler.sendMessage(new Message());
            mFinished = true;
            if (BuildConfig.DEBUG) {
                Log.i(INNER_TAG, "run() - Exiting run method.");
            }
        }

        @Override
        public void onPortFound(String portFoundResult) {
            if (BuildConfig.DEBUG) {
                Log.i(INNER_TAG, "onPortFound()");
            }
            Message msg = new Message();
            msg.obj = portFoundResult;
            mPortFoundHandler.sendMessage(msg);
        }

        /*
        * Called on fragment pause.
        * Pauses the thread and removes the spinner.
        */
        public void onPause() {
            if (BuildConfig.DEBUG) {
                Log.i(INNER_TAG, "onPause()");
            }
            synchronized (mPauseLock) {
                mPaused = true;
            }
            mCallbacks.scanStopping();
        }

        /*
         * Called on fragment resume.
         * Resumes the thread and the spinner.
         */
        public void onResume() {
            if (BuildConfig.DEBUG) {
                Log.i(INNER_TAG, "onResume()");
            }
            synchronized (mPauseLock) {
                mPaused = false;
                mPauseLock.notifyAll();
            }
            mCallbacks.scanStarting();
        }
    }

    // Splits a list into sublists of length L
    private static <T> List<List<T>> subList(List<T> list, final int L) {
        List<List<T>> parts = new ArrayList<>();
        final int N = list.size();
        for (int i = 0; i < N; i += L) {
            parts.add(new ArrayList<>(
                            list.subList(i, Math.min(N, i + L)))
            );
        }
        return parts;
    }
}
