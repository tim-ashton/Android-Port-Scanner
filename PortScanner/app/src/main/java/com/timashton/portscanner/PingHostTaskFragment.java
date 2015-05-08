/*
 * Created by Tim Ashton on 8/05/15.
 *
 * PingHostTaskFragment runs an async task that continues across device
 * configuration changes. It checks that the hostname is an IP. If the host
 * name is not an IP an attempt to resolve the hostname is made.
 * This is followed by a ping request to check the remote host is "alive".
 */

package com.timashton.portscanner;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class PingHostTaskFragment extends Fragment {

    interface PingHostCallbacks {
        void onPreExecute();
        void onPostExecute(boolean success);
    }

    private static String TAG_HOSTNAME =  "hostname";
    private PingHostCallbacks mCallbacks;
    private PingHostTask mTask;
    private String mHostname;


    /*
    Pass the hostname to a new fragment instance and return it to the
    caller.
     */
    public static PingHostTaskFragment newInstance(String hostName) {

        PingHostTaskFragment fragment = new PingHostTaskFragment();
        Bundle bundle = new Bundle(2);
        bundle.putString(TAG_HOSTNAME, hostName);
        fragment.setArguments(bundle);
        return fragment ;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (PingHostCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement PingHostCallbacks");
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHostname = getArguments().getString(TAG_HOSTNAME);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        // Create and execute the background task.
        mTask = new PingHostTask();
        mTask.execute();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    /*
    Private Async task to resolve and check the hosts existence in the background.

    Report success or failure back to the activity in onPostExecute();
     */
    private class PingHostTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            if (mCallbacks != null) {
                mCallbacks.onPreExecute();
            }
        }

        @Override
        protected Void doInBackground(Void... ignore) {

            // TODO check the host here!
            return null;
        }

        @Override
        protected void onPostExecute(Void ignore) {
            if (mCallbacks != null) {

                //TODO report back to the activity
                mCallbacks.onPostExecute(true);
            }
        }
    }

    /*
   IsValidIP()
   Tests the string parameter to see it is in valid format 255.255.255.255
    */
    private static boolean IsValidIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        ip = ip.trim();
        if ((ip.length() < 6) & (ip.length() > 15)) {
            return false;
        }

        try {
            Pattern pattern = Pattern.compile(
                    "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                            "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
            Matcher matcher = pattern.matcher(ip);
            return matcher.matches();
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }
}
