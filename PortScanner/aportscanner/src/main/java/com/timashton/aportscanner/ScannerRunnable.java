/*
 * Created by Tim Ashton on 3/01/15.
 */

package com.timashton.aportscanner;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class ScannerRunnable implements Runnable {

    private static String TAG = ScannerRunnable.class.getName();
    private static ScanRunnableCallbacks mCallbacks;

    private final String mHostName;
    private final List<Integer> mPorts;
    private final int mSockTimeout;
    private volatile boolean mSuspended = false;
    final private Object mObject = new Object();


    public ScannerRunnable(String hostname
            , List<Integer> ports
            , ScanWorkerFragment.ScannerThread scannerThread
            , int socketTimeout) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "ScannerRunnable()");
        }
        mHostName = hostname;
        mPorts = ports;
        mSockTimeout = socketTimeout;
        mCallbacks = scannerThread;
    }

    public interface ScanRunnableCallbacks {
        void onPortFound(String portFoundResult);
        void incrementProgress();
    }

    public void suspend() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "suspend()");
        }
        mSuspended = true;
    }

    public void resume() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "resume()");
        }
        mSuspended = false;
        synchronized (mObject) {
            mObject.notifyAll();
        }
    }

    @Override
    public void run() {
        int portNumber;
        while (!Thread.currentThread().isInterrupted() && !mPorts.isEmpty()) {
            if (!mSuspended) {

                try {
                    portNumber = mPorts.remove(0);
                } catch (IndexOutOfBoundsException e) {
                    // This should never happen but log and stop the thread if it does.
                    Log.i(TAG, e.toString());
                    break;
                }

                try {
                    synchronized (ScannerRunnable.class) {
                        mCallbacks.incrementProgress();
                    }
                    Socket scanSock = new Socket();
                    scanSock.connect(new InetSocketAddress(mHostName, portNumber), mSockTimeout);
                    String portFound = "Found Open Port Number: " + portNumber;
                    synchronized (ScannerRunnable.class) {
                        mCallbacks.onPortFound(portFound);
                    }
                    scanSock.close();
                } catch (IOException e) {
                    // Do nothing - no port found.
                }
            } else {
                //Has been suspended
                try {
                    while (mSuspended) {
                        synchronized (mObject) {
                            mObject.wait();
                        }
                    }
                } catch (InterruptedException e) {
                    Log.i(TAG, e.toString());
                }
            }
        }
    }
}

