package com.timashton.aportscanner;

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
 *
 * Fragment which contains the boss of the thread pool.
 */


public class ScanWorkerFragment extends Fragment {

    public static final String TAG = ScanWorkerFragment.class.getName();

    private static final int MAX_PORTS_PER_THREAD = 20;

    private static final int UPDATE_PROGRESS = 0;
    private static final int ADD_PORT = 1;
    private static final int SCAN_FINISHED = 2;

    private static ScannerThread mScannerThread;
    private static ScanFragmentCallbacks mCallbacks;
    private static ScanWorkerHandler mScanWorkerHandler;

    public interface ScanFragmentCallbacks {
        void addPortFound(String portFoundResult);

        void scanFinished();

        void updateProgress();
    }


    public static ScanWorkerFragment newInstance() {
        return new ScanWorkerFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onAttach()");
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
            Log.d(TAG, "onCreate()");
        }
        setRetainInstance(true);

        if (savedInstanceState == null) {
            mScanWorkerHandler = new ScanWorkerHandler(this);
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
            Log.d(TAG, "onPause()");
        }
        if (mScannerThread != null) {
            if (!mScannerThread.scannerThreadFinished) {
                mScannerThread.onPause();
            }
        }
    }

    /*
     * Call the onResume() of the DemoThread to resume execution
     * of the threads.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onResume()");
        }
        if (mScannerThread != null) {
            if (!mScannerThread.scannerThreadFinished) {
                mScannerThread.onResume();
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDetach()");
        }
        mCallbacks = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDestroy()");
        }
        mScannerThread = null;
    }

    public void startScannerThread(ScanData scanData) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "startScannerThread()");
        }

        //Prepare lists of port numbers
        List<Integer> ports = new ArrayList<>();
        for (int i = scanData.startPort; i <= scanData.endPort; i++) {
            ports.add(i);
        }
        List<List<Integer>> portSubList = subList(ports, MAX_PORTS_PER_THREAD);

        // Create the new boss thread
        mScannerThread = new ScannerThread(scanData.hostName, portSubList, scanData.averagePing);
        mScannerThread.start();
    }

    /*
    This inner class is the handler used to pass all messages from the scanner runnables back to the
    UI thread.
     */
    private static class ScanWorkerHandler extends Handler {

        private final WeakReference<ScanWorkerFragment> scanWorkerFragmentWeakReference;

        public ScanWorkerHandler(ScanWorkerFragment myClassInstance) {
            scanWorkerFragmentWeakReference = new WeakReference<>(myClassInstance);
        }

        @Override
        public void handleMessage(Message msg) {
            ScanWorkerFragment myClass = scanWorkerFragmentWeakReference.get();
            if (myClass != null) {

                switch(msg.what){
                    case UPDATE_PROGRESS:
                        mCallbacks.updateProgress();
                        break;
                    case ADD_PORT:
                        mCallbacks.addPortFound(msg.obj.toString());
                        break;
                    case SCAN_FINISHED:
                        mCallbacks.scanFinished();
                        break;

                }
            }
        }
    }


    /*
     * Class ScannerThread
     *
     * Inner thread class to run the boss thread by overriding the thread.run method.
     * Make static because there should only ever be one of these running in the port
     * scanner application at any time.
     */
    public static class ScannerThread extends Thread
            implements ScannerRunnable.ScanRunnableCallbacks {

        public static final String INNER_TAG = ScannerThread.class.getName();
        private final int MAX_SOCK_TIMEOUT = 2000; //maximum allowable timeout 2 sec.

        private final Object pauseLock;
        private boolean paused;
        private boolean scannerThreadFinished;
        private String threadHostname;
        private int threadSockTimeout;
        private List<List<Integer>> threadPorts;
        private Map<Integer, ScannerRunnable> scannerRunnables;

        public ScannerThread(String hostName, List<List<Integer>> ports, int avgPing) {
            if (BuildConfig.DEBUG) {
                Log.d(INNER_TAG, "ScannerThread()");
            }
            threadHostname = hostName;
            threadPorts = ports;

            threadSockTimeout = calculateSockTimeout(avgPing);
            threadSockTimeout = threadSockTimeout > MAX_SOCK_TIMEOUT
                    ? MAX_SOCK_TIMEOUT : threadSockTimeout;

            pauseLock = new Object();
            paused = false;
            scannerThreadFinished = false;
        }

        @SuppressWarnings("unused")
        private ScannerThread() {
            pauseLock = new Object();
            paused = false;
            scannerThreadFinished = false;
        }

        @Override
        public void run() {
            if (BuildConfig.DEBUG) {
                Log.i(INNER_TAG, "run()");
            }
            scannerRunnables = new HashMap<>();
            ExecutorService es = Executors.newCachedThreadPool();

            // Create and add Scanner Runnables to the ExecutorService.
            for (int i = 0; i < threadPorts.size(); i++) {
                scannerRunnables.put(i,
                        new ScannerRunnable(threadHostname, threadPorts.get(i), this, threadSockTimeout));

                es.execute(scannerRunnables.get(i));
            }
            es.shutdown();

            try {
                es.awaitTermination(10, TimeUnit.MINUTES);

                synchronized (pauseLock) {
                    while (paused) {
                        try {
                            pauseLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (InterruptedException e) {
                // May have been cancelled.
                Log.e(TAG, e.toString());
            }

            mScanWorkerHandler.sendMessage(mScanWorkerHandler.obtainMessage (SCAN_FINISHED, null));
            scannerThreadFinished = true;
        }

        @Override
        public void onPortFound(String portFoundResult) {
            mScanWorkerHandler.sendMessage(mScanWorkerHandler.obtainMessage (ADD_PORT, portFoundResult));
        }

        @Override
        public void incrementProgress() {
            mScanWorkerHandler.sendMessage(mScanWorkerHandler.obtainMessage (UPDATE_PROGRESS, null));
        }

        /*
        * Called on fragment pause.
        * Pauses the thread and removes the spinner.
        */
        public void onPause() {
            if (BuildConfig.DEBUG) {
                Log.d(INNER_TAG, "onPause()");
            }

            for (int i = 0; i < threadPorts.size(); i++) {
                if(scannerRunnables.get(i) != null){
                    scannerRunnables.get(i).suspend();
                }
            }

            synchronized (pauseLock) {
                paused = true;
            }
        }

        /*
         * Called on fragment resume.
         * Resumes the thread and the spinner.
         */
        public void onResume() {
            if (BuildConfig.DEBUG) {
                Log.d(INNER_TAG, "onResume()");
            }

            for (int i = 0; i < threadPorts.size(); i++) {
                scannerRunnables.get(i).resume();
            }

            synchronized (pauseLock) {
                paused = false;
                pauseLock.notifyAll();
            }
        }

        /*
        Calculate a timeout depending on the average ping detected.

        Arbitrary multipliers depending on latency detected when starting the scan which
        multiply a higher number if a lower latency was detected.
         */
        int calculateSockTimeout(int averagePing) {
            final double belowFifty = 15.0;
            final double belowOneHundred = 10.0;
            final double oneHundredToTwoFifty = 5.0;
            final double twoFiftyToFourHundred = 4.5;
            final double fourHundredToFiveFifty = 3.25;
            final double defaultMultiplier = 2.0;

            int result;

            if(averagePing < 50){
                result = (int)(averagePing * belowFifty);
            }
            else if(averagePing < 100){
                result = (int)(averagePing * belowOneHundred);
            }
            else if(averagePing < 250){
                result = (int)(averagePing * oneHundredToTwoFifty);
            }
            else if(averagePing < 400){
                result = (int)(averagePing * twoFiftyToFourHundred);
            }
            else if(averagePing < 550){
                result = (int)(averagePing * fourHundredToFiveFifty);
            }
            else {
                result = (int)(averagePing * defaultMultiplier);
            }
            return result;
        }
    }

    /*
    Splits a list of any type into sub lists of length L
     */
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
