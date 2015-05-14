/*
 * Created by Tim Ashton on 05 May 2015.
 */

package com.timashton.portscanner;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;


public class MainInputFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = MainInputFragment.class.getName();
    private static final int INVALID_PORT = -1;
    private static final int MAX_PORTS = 100;

    private Button mStartScanButton;
    private EditText mHostnameEt;
    private EditText mStartPortEt;
    private EditText mEndPortEt;

    private String mHostname;
    private String mStartPortStr;
    private String mEndPortStr;
    private int mStartPort;
    private int mEndPort;

    private boolean mHostOk;
    private boolean mPortsOk;


    private MainInputCallbacks mCallbacks;

    public interface MainInputCallbacks {
        void startScan(String hostName, int startPort, int endPort);
    }

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
        //TODO setRetainInstance(true);
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
                    if (mHostname == null || mHostname.isEmpty()) {
                        return;
                    }
                    new PingHostTask().execute(mHostname);
                    checkReadyToScan();
                }
            }
        });

        mStartPortEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mStartPortStr = mStartPortEt.getText().toString();
                    mStartPort = isValidPort(mStartPortStr);
                    checkReadyToScan();
                }
            }
        });

        mEndPortEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mEndPortStr = mEndPortEt.getText().toString();
                    mEndPort = isValidPort(mEndPortStr);
                    checkReadyToScan();
                }
            }
        });

        mEndPortEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                int result = actionId & EditorInfo.IME_MASK_ACTION;
                if(result == EditorInfo.IME_ACTION_DONE){
                    mEndPort = isValidPort(mEndPortEt.getText().toString());
                    checkReadyToScan();
                }
                return true;
            }
        });

        return view;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.i(TAG, "onAttach()");
        try {
            mCallbacks = (MainInputCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MainInputCallbacks");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetach()");
        mCallbacks = null;
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "onClick()");
        mStartScanButton.setEnabled(false);
        mCallbacks.startScan(mHostname, mStartPort, mEndPort);

    }




    /*
    Private Async task to resolve and check the hosts existence in the background.

    Report success or failure back to the activity in onPostExecute();
     */
    private class PingHostTask extends AsyncTask<String, Void, Void> {
        private boolean success = true;

        @Override
        protected Void doInBackground(String... values) {
            try {
                InetAddress address = InetAddress.getByName(values[0]);
                address.isReachable(5000);
            } catch (IOException e) {
                    success = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void ignore) {
            mHostOk = success;
        }
    }



    /*
    IsValidPort(String port)

    Check that the port supplied is an integer and it is within port range (0 - 65535)
     */
    private int isValidPort(String port) {
        if (port == null || port.isEmpty()) {
            return INVALID_PORT;
        }

        int portNumber;
        port = port.trim();

        try {
            portNumber = Integer.parseInt(port);
        } catch (NumberFormatException ex) {
            return INVALID_PORT;
        }

        if((portNumber < 1) || (portNumber > 65535))
        {
            return INVALID_PORT;
        }

        return portNumber;
    }


    private void checkReadyToScan(){

        boolean bothPortsValid = (mStartPort != INVALID_PORT) && (mEndPort != INVALID_PORT);
        boolean startLessEnd = (mStartPort < mEndPort);
        boolean lessThanMax = ((mEndPort - mStartPort) <= MAX_PORTS);

        boolean ready = (bothPortsValid && startLessEnd && lessThanMax && mHostOk);

        mStartScanButton.setEnabled(ready);
        hideKeyboard(ready);
    }

    private void hideKeyboard(boolean readyToScan) {
        if (readyToScan == false){
            return;
        }

        // Check if no view has focus:
        View view = this.getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager =
                    (InputMethodManager) this.getActivity()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);

            inputManager.hideSoftInputFromWindow(view.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

}

