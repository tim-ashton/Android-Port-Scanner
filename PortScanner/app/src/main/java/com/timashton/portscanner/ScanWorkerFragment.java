package com.timashton.portscanner;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

/*
 * Created by Tim Ashton on 12/05/15.
 */


public class ScanWorkerFragment extends Fragment {

    private static final String TAG = ScanWorkerFragment.class.getName();
    private static final String HOSTNAME_TAG = "hostname_tag";
    private static final String START_PORT_TAG = "start_port_tag";
    private static final String END_PORT_TAG = "end_port_tag";

    private String mHostName;
    private int mStartPort;
    private int mEndPort;

    //TODO - callbacks


    public ScanWorkerFragment newInstance(String hostName, int startPort, int endPort){
        ScanWorkerFragment myFragment = new ScanWorkerFragment();

        Bundle args = new Bundle(3);
        args.putString(HOSTNAME_TAG, hostName);
        args.putInt(START_PORT_TAG, startPort);
        args.putInt(END_PORT_TAG, endPort);
        myFragment.setArguments(args);

        return myFragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mHostName = getArguments().getString(HOSTNAME_TAG);
        mStartPort = getArguments().getInt(START_PORT_TAG);
        mEndPort = getArguments().getInt(END_PORT_TAG);

    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        //TODO check some callbacks are created

    }




    @Override
    public void onDetach() {
        super.onDetach();
        // TODO - remove handlers
    }


    /*
     * Class DemoThread
     *
     * Inner thread class to run the dummy thread by overriding the thread.run method.
     * Make static because there should only ever be one of these.
     *
     * Allows parent to pause and restart the thread by calling the appropriate methods.
     *
     */
    static class DemoThread extends Thread {

        private static final String TAG = DemoThread.class.getName();

        private final Object mPauseLock;
        private boolean mPaused;
        private boolean mFinished;

        public DemoThread() {
            Log.i(TAG, "DemoRunnable()");
            mPauseLock = new Object();
            mPaused = false;
            mFinished = false;
        }


        @Override
        public void run() {
            Log.i(TAG, "run()");

            try {
                int i = 0;
                while (!mFinished) {
                    String countText = "Item: " + i++ + " added to list!";
                    Message msg = new Message();
                    msg.obj = countText;
                    //mHandler.sendMessage(msg);

                    //Add an item every 1/2 second
                    Thread.sleep(500);

                    if (i == 25) {
                        mFinished = true;
                    }

                    synchronized (mPauseLock) {
                        while (mPaused) {
                            try {
                                mPauseLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // TODO
            }
        }

        /*
         * Called on fragment pause.
         */
        public void onPause() {
            Log.i(TAG, "onPause()");
            synchronized (mPauseLock) {
                mPaused = true;
            }
        }

        /*
         * Called on fragment resume.
         */
        public void onResume() {
            Log.i(TAG, "onResume()");
            synchronized (mPauseLock) {
                mPaused = false;
                mPauseLock.notifyAll();
            }
        }
    }
}
