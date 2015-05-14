package com.timashton.portscanner;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
 * Created by Tim Ashton on 12/05/15.
 */


public class ScanWorkerFragment extends Fragment {

    private static final String TAG = ScanWorkerFragment.class.getName();

    private static final int MAX_PORTS_PER_THREAD = 20;

    private static ScanFragmentCallbacks mCallbacks;
    private static PortFoundHandler mHandler;

    public interface ScanFragmentCallbacks {
        void addPortFound(String portFoundResult);
    }


    public static ScanWorkerFragment newInstance(){
        return new ScanWorkerFragment();
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        Log.i(TAG, "onAttach(activity)");
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
        Log.i(TAG, "onCreate(savedInstanceState)");
        setRetainInstance(true);


        if (savedInstanceState == null) {
            mHandler = new PortFoundHandler(this);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
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

    public void startScannerThread(String hostName, int startPort, int endPort) {
        Log.i(TAG, "startScannerThread()");

        //Prepare lists of port numbers
        List<Integer> ports  = new ArrayList<>();
        for(int i = startPort; i <= endPort; i++){
            ports.add(i);
        }
        List<List<Integer>> portSubList = subList(ports, MAX_PORTS_PER_THREAD);

        // Create the new boss thread
        ScannerThread mScannerThread = new ScannerThread(hostName, portSubList);
        mScannerThread.start();
    }


    /*
     * Class ScannerThread
     *
     * Inner thread class to run the boss thread by overriding the thread.run method.
     * Make static because there should only ever be one of these running in the port
     * scanner application at any time.
     */
    public static class ScannerThread extends Thread implements ScannerRunnable.ScanRunnableCallbacks {

        private static final String TAG = ScannerThread.class.getName();

        private String threadHostname;
        private List<List<Integer>> threadPorts;

        public ScannerThread(String hostName, List<List<Integer>> ports) {
            threadHostname = hostName;
            threadPorts = ports;
        }

        @SuppressWarnings("unused")
        private ScannerThread() {
        }

        @Override
        public void run() {
            ExecutorService es = Executors.newCachedThreadPool();

            // Create and add Scanner Runnables to the ExecutorService.
            for (int i = 0; i < threadPorts.size(); i++) {
                es.execute(new ScannerRunnable(threadHostname, threadPorts.get(i), this));
            }
            es.shutdown();

            try {
                // TODO allow configurable timeout
                while (!es.awaitTermination(10, TimeUnit.MINUTES)) {  // Wait for all threads};

                    //TODO - do I need to allow pausing of the boss thread ?
                }
            } catch (InterruptedException e) {
                Log.e(TAG, e.toString());
            }
        }

        @Override
        public void onPortFound(String portFoundResult) {
            Message msg = new Message();
            msg.obj = portFoundResult;
            mHandler.sendMessage(msg);
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
