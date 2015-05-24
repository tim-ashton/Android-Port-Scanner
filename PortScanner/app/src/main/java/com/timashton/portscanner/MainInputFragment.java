/*
 * Created by Tim Ashton on 05 May 2015.
 */

package com.timashton.portscanner;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
    private int mAvgPing;


    private MainInputCallbacks mCallbacks;

    public interface MainInputCallbacks {
        void startScan(String hostName, int startPort, int endPort, int avgPing);
    }

    /*
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment MainInputFragment.
     */
    public static MainInputFragment newInstance() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "newInstance()");
        }
        return new MainInputFragment();
    }

    public MainInputFragment() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "MainInputFragment()");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onCreate()");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onCreateView()");
        }
        View view = inflater.inflate(R.layout.fragment_main_input, container, false);

        mStartScanButton = (Button) view.findViewById(R.id.button_start_scan);
        mStartScanButton.setOnClickListener(this);
        mStartScanButton.setEnabled(false); // enable after everything is set and valid

        mHostnameEt = (EditText) view.findViewById(R.id.et_ip_hostname);
        mStartPortEt = (EditText) view.findViewById(R.id.et_start_port);
        mEndPortEt = (EditText) view.findViewById(R.id.et_end_port);

        InputMethodManager imgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imgr.showSoftInput(getView(), InputMethodManager.SHOW_IMPLICIT);

        mHostnameEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mHostname = mHostnameEt.getText().toString();
                    if (mHostname == null || mHostname.isEmpty()) {
                        return;
                    }
                    mStartScanButton.setEnabled(checkReadyToScan());
                }

            }
        });

        mStartPortEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mStartPortStr = mStartPortEt.getText().toString();
                    mStartPort = isValidPort(mStartPortStr);
                    mStartScanButton.setEnabled(checkReadyToScan());
                }

            }
        });

        mEndPortEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mEndPortStr = mEndPortEt.getText().toString();
                    mEndPort = isValidPort(mEndPortStr);
                    mStartScanButton.setEnabled(checkReadyToScan());
                }
            }
        });

        // Check what to do when the "done" button is pressed on the keypad
        mEndPortEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                int result = actionId & EditorInfo.IME_MASK_ACTION;
                if (result == EditorInfo.IME_ACTION_DONE) {
                    mEndPort = isValidPort(mEndPortEt.getText().toString());
                    mStartScanButton.setEnabled(checkReadyToScan());
                    mStartScanButton.callOnClick();
                }
                return true;
            }
        });

        return view;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onAttach()");
        }
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
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onDetach()");
        }
        mCallbacks = null;
    }

    @Override
    public void onClick(View v) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onClick()");
        }
        mStartScanButton.setEnabled(false);
        new PingHostTask().execute(mHostname);
    }


    /*
    Private Async task to resolve and check the hosts existence in the background.

    Report success or failure back to the activity in onPostExecute();
     */
    private class PingHostTask extends AsyncTask<String, Boolean, Boolean> {

        private final String INNER_TAG = PingHostTask.class.getName();
        private static final int FIRST_DIALOG_DISPLAY_TIME = 2000;
        private static final int SECOND_DIALOG_DISPLAY_TIME = 1500;
        private static final int PING_TIMEOUT = 5000; // 5 sec
        private static final int PING_QTY = 3;
        private Dialog dialog;

        @Override
        protected void onPreExecute() {
            if (BuildConfig.DEBUG) {
                Log.i(INNER_TAG, "onPreExecute()");
            }
            // Create the custom Dialog
            dialog = new Dialog(getActivity());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_transition);
            dialog.setCancelable(false);

            dialog.show();
        }

        @Override
        protected Boolean doInBackground(String... values) {
            if (BuildConfig.DEBUG) {
                Log.i(INNER_TAG, "doInBackground()");
            }
            boolean success = true;
            Long startPingTime = System.currentTimeMillis();
            int totalPingTime = 0;

            for (int i = 0; i < PING_QTY; i++) {
                startPingTime = System.currentTimeMillis();
                try {
                    InetAddress address = InetAddress.getByName(values[0]);
                    address.isReachable(PING_TIMEOUT);
                } catch (IOException e) {
                    success = false;
                }
                totalPingTime += (System.currentTimeMillis() - startPingTime);
            }
            mAvgPing = (int) (totalPingTime / PING_QTY);

            // Adjust the time so the user actually sees a dialog
            // and wait ..
            if (totalPingTime < FIRST_DIALOG_DISPLAY_TIME) {
                int waitTime = FIRST_DIALOG_DISPLAY_TIME - totalPingTime;

                // Just busy wait to make up a bit of time :/
                int timer = 0;
                while (timer < waitTime) {
                    try {
                        Thread.sleep(100);
                        timer += 100;

                    } catch (InterruptedException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }


            // Ask the dialog to update its contents
            publishProgress(success);

            // Just busy wait for 1 second before dismissing the dialog
            int timer = 0;
            while (timer < SECOND_DIALOG_DISPLAY_TIME) {
                try {
                    Thread.sleep(100);
                    timer += 100;

                } catch (InterruptedException e) {
                    Log.e(TAG, e.toString());
                }
            }

            return success;
        }

        @Override
        protected void onProgressUpdate(Boolean... values) {
            if (BuildConfig.DEBUG) {
                Log.i(INNER_TAG, "onProgressUpdate()");
            }
            TextView textView = (TextView) dialog.findViewById(R.id.dialog_transition_text);
            ProgressBar progressbar = (ProgressBar) dialog.findViewById(R.id.dialog_transition_progress_bar);

            // Update contents of dialog
            if (values[0]) {
                textView.setText("Latency " + mAvgPing + " ms");
                progressbar.setIndeterminate(false);
            } else {
                textView.setText(R.string.dialog_transition_host_unreachable);
                progressbar.setIndeterminate(false);
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (BuildConfig.DEBUG) {
                Log.i(INNER_TAG, "onPostExecute()");
            }
            if (success) {
                dialog.dismiss();
                mCallbacks.startScan(mHostname, mStartPort, mEndPort, mAvgPing);
            } else {
                dialog.dismiss();
            }
        }
    }


    /*
    IsValidPort(String port)

    Check that the port supplied is an integer and it is within port range (0 - 65535)
     */
    private int isValidPort(String port) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "isValidPort()");
        }
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

        if ((portNumber < 1) || (portNumber > 65535)) {
            return INVALID_PORT;
        }

        return portNumber;
    }


    private boolean checkReadyToScan() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "checkReadyToScan()");
        }
        boolean bothPortsValid = (mStartPort != INVALID_PORT) && (mEndPort != INVALID_PORT);
        boolean startLessEnd = (mStartPort < mEndPort);
        boolean lessThanMax = ((mEndPort - mStartPort) <= MAX_PORTS);
        boolean hasHostName = mHostname != null;

        return (bothPortsValid && startLessEnd && lessThanMax);
    }

    private void hideKeyboard(boolean readyToScan) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "hideKeyboard()");
        }
        if (!readyToScan) {
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

