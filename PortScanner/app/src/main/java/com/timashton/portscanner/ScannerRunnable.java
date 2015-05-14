/*
 * Created by Tim Ashton on 3/01/15.
 */

package com.timashton.portscanner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class ScannerRunnable implements Runnable {

    private static ScanRunnableCallbacks mCallbacks;

    final private String mHostName;
    final private List<Integer> mPorts;

    public ScannerRunnable(String hostname, List<Integer> ports, ScanWorkerFragment.ScannerThread scannerThread) {
        mHostName = hostname;
        mPorts = ports;
        mCallbacks = scannerThread;
    }

    public interface ScanRunnableCallbacks {
        void onPortFound(String portFoundResult);
    }

    @Override
    public void run() {
        for (int i = 0; i < mPorts.size(); i++) {
            try {
                Socket scanSock = new Socket();
                scanSock.connect(new InetSocketAddress(mHostName, mPorts.get(i)), 0);

                String portFound = "Found Open Port Number: " + mPorts.get(i);

                synchronized (ScannerRunnable.class) {
                    mCallbacks.onPortFound(portFound);
                }
                scanSock.close();
            } catch (IOException e) {
                // Do nothing - no port found.
            }
        }
    }
}

