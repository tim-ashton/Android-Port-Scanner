package com.timashton.portscanner;

/**
 * Created by tim on 3/01/15.
 */

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Observable;

public class Scanner extends Observable implements Runnable {

    private static String openPort;

    final private int startPort;
    final private int endPort;
    final private String host;


    public Scanner(String hostIP, int startPort, int portsPerThread) {
        this.host = hostIP;
        this.startPort = startPort;
        this.endPort = startPort + portsPerThread;
    }

    @Override
    public void run() {
        for (int port = startPort; port <= endPort; port++) {
            try {
                Socket scanSock = new Socket();
                scanSock.connect(new InetSocketAddress(host, port), 0);

                String portFound = "Found Open Port Number: " + port;
                synchronized (Scanner.class) {
                    openPort = portFound;
                }
                setChanged();
                notifyObservers();

                scanSock.close();
            } catch (Exception e) {
                // Do nothing this is expected
            }
        }
    }


    public synchronized String getNextPort() {
        return openPort;
    }
}

