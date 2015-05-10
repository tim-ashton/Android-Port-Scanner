/*
 * Created by Tim Ashton on 05 May 2015.
 */

package com.timashton.portscanner;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class MainInputFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = MainInputFragment.class.getName();

    private Button mStartScanButton;
    private EditText mHostnameEt;
    private EditText mStartPortEt;
    private EditText mEndPortEt;

    private String mHostname;
    private int mStartport;
    private int mEndPort;

    private boolean mValidHostName;
//    private boolean mValid

    private OnMainInputListener mListener;

    /*
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment MainInputFragment.
     */
    public static MainInputFragment newInstance() {
        return new MainInputFragment();
    }

    public MainInputFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        setRetainInstance(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main_input, container, false);

        mStartScanButton = (Button) view.findViewById(R.id.button_start_scan);
        mStartScanButton.setOnClickListener(this);
        mStartScanButton.setEnabled(false); // enable after everything is set and valid

        mHostnameEt = (EditText) view.findViewById(R.id.et_ip_hostname);
        mStartPortEt = (EditText) view.findViewById(R.id.et_start_port);
        mEndPortEt = (EditText) view.findViewById(R.id.et_end_port);

        mHostnameEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mHostname = mHostnameEt.getText().toString();
                    // TODO - run the asynchtask
                }
            }
        });

        mStartPortEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mStartport = IsValidPort(mStartPortEt.getText().toString());
                }
            }
        });

        mEndPortEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mEndPort = IsValidPort(mEndPortEt.getText().toString());
                }
            }
        });

        return view;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.i(TAG, "onAttach()");
        try {
            mListener = (OnMainInputListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetach()");
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "onClick()");
        //mStartScanButton.setEnabled(false); TODO





    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnMainInputListener {
        public void onMainInput(); // TODO - Do something
    }


    /*
    Private Async task to resolve and check the hosts existence in the background.

    Report success or failure back to the activity in onPostExecute();
     */
    private class PingHostTask extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            //check if string is ip
            //mHostIsIpAddress = IsValidIP(mHostname);
        }

        @Override
        protected Void doInBackground(String... ignore) {
            // Resolve and ping
            return null;
        }

        @Override
        protected void onPostExecute(Void ignore) {
            // Report success/faiure
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

    /*
    IsValidPort(String port)

    Check that the port supplied is an integer and it is within port range (0 - 65535)
     */
    private int IsValidPort(String port) {
            if (port == null || port.isEmpty()) {
            return 0;
        }

        int portNumber = 0;

        port = port.trim();
        try {
            portNumber = Integer.parseInt(port);
        } catch (NumberFormatException ex) {
            return 0;
        }

        if((portNumber < 1) || (portNumber > 65535))
        {
            return 0;
        }

        return portNumber;
    }

}

