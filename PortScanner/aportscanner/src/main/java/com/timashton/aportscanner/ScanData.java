package com.timashton.aportscanner;

/*
 * Created by Tim Ashton on 3/06/15.
 *
 * A simple public class to hold the scan data for passing around between fragments.
 */
public class ScanData {

    public String hostName = "";
    public int startPort;
    public int endPort;
    public int averagePing;

    public ScanData(String host, int start, int end, int avg){
        hostName = host;
        startPort = start;
        endPort = end;
        averagePing = avg;
    }
}
