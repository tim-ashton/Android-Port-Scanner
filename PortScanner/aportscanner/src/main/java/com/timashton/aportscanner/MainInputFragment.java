/*
 * Created by Tim Ashton on 05 May 2015.
 */

package com.timashton.aportscanner;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class MainInputFragment extends Fragment
        implements View.OnClickListener
        , PopupMenu.OnMenuItemClickListener
        , PopupMenu.OnDismissListener {

    public static final String TAG = MainInputFragment.class.getName();

    private static final String DEFAULT_HOSTNAME = "localhost";
    private static final String DEFAULT_START_PORT = "1";
    private static final String DEFAULT_END_PORT = "100";
    private static final String STORED_RECENT_HOSTS_FILENAME = "recent_hosts";
    private static final String PREFS_SCAN_CLICKED = "scan_clicked";
    private static final int MAX_RECENT_SIZE = 5;
    private static final int MAX_PORTS = 100;
    private static final int LAST_PORT = 65535;
    public final static int PING_TIMEOUT = 1500; // ms used by transition dialog


    private static MainInputCallbacks mCallbacks;
    private Button mStartScanButton;
    private EditText mHostnameEt;

    private ArrayList<String> mRecentHostList = new ArrayList<>();
    private String mHostname;
    private int mStartPort = 0;
    private int mEndPort = 0;


    public interface MainInputCallbacks {

        void onPreExecute();

        void onProgressUpdate(boolean success, int avgPing);

        void onPostExecute(ScanData scanData, boolean runScan);
    }

    public static MainInputFragment newInstance() {
        return new MainInputFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreate()");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateView()");
        }
        View view = inflater.inflate(R.layout.fragment_main_input, container, false);

        mStartScanButton = (Button) view.findViewById(R.id.button_start_scan);
        mStartScanButton.setOnClickListener(this);

        ImageButton showRecentHosts = (ImageButton) view.findViewById(R.id.button_show_recent_hosts);
        showRecentHosts.setOnClickListener(this);

        mHostnameEt = (EditText) view.findViewById(R.id.et_ip_hostname);

        mHostnameEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    InputMethodManager inputManager =
                            (InputMethodManager) getActivity()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.showSoftInput(mHostnameEt, InputMethodManager.SHOW_IMPLICIT);

                }
            }
        });

        EditText startPortEt = (EditText) view.findViewById(R.id.et_start_port);
        startPortEt.setText(DEFAULT_START_PORT);
        try {
            mStartPort = Integer.parseInt(DEFAULT_START_PORT);
        } catch (NumberFormatException e) {
            Log.e(TAG, e.toString());
        }

        EditText endPortEt = (EditText) view.findViewById(R.id.et_end_port);
        endPortEt.setText(DEFAULT_END_PORT);
        try {
            mEndPort = Integer.parseInt(DEFAULT_END_PORT);
        } catch (NumberFormatException e) {
            Log.e(TAG, e.toString());
        }


        // Add a TextWatcher to the hostname EditText
        mHostnameEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mHostname = mHostnameEt.getText().toString();
                mStartScanButton.setEnabled(checkReadyToScan());
            }
        });

        // Add a TextWatcher to the start port EditText
        // for afterTextChanged
        startPortEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    if (s != null && !s.toString().isEmpty()) {
                        mStartPort = Integer.parseInt(s.toString());
                    } else {
                        mStartPort = 0;
                    }
                } catch (NumberFormatException ex) {
                    Log.e(TAG, ex.toString());
                    mStartPort = 0;
                }
                mStartScanButton.setEnabled(checkReadyToScan());
            }
        });

        // Add a TextWatcher to the end port edit text
        endPortEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    if (s != null && !s.toString().isEmpty()) {
                        mEndPort = Integer.parseInt(s.toString());
                    } else {
                        mEndPort = 0;
                    }
                } catch (NumberFormatException ex) {
                    Log.e(TAG, ex.toString());
                    mStartPort = 0;
                }
                mStartScanButton.setEnabled(checkReadyToScan());
            }
        });


        // Check what to do when the "done" button is pressed on the keypad
        endPortEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                int result = actionId & EditorInfo.IME_MASK_ACTION;
                if (result == EditorInfo.IME_ACTION_DONE) {

                    // if everything is ok to scan go ahead.
                    // Any issue with user input should already be handled and displayed to user.
                    if (checkReadyToScan()) {
                        mStartScanButton.callOnClick();
                    }
                }
                return true;
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mStartScanButton.setEnabled(checkReadyToScan());
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onAttach()");
        }
        try {
            mCallbacks = (MainInputCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MainInputCallbacks");
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onResume()");
        }

        // Get preferences and check if this is first use.
        SharedPreferences settings = getActivity().getPreferences(MainActivity.MODE_PRIVATE);
        if (settings.getBoolean(PREFS_SCAN_CLICKED, false)) {

            // Loads any recent entries into mRecentHostList from file
            mRecentHostList = loadRecentHostList();
            if (mRecentHostList.isEmpty()) {
                mHostnameEt.setText(DEFAULT_HOSTNAME);
            } else {
                // as entries are always added to position 0
                // 0 is the most recent host scanned
                mHostnameEt.setText(mRecentHostList.get(0));
            }

        } else {
            mHostnameEt.setText(DEFAULT_HOSTNAME);
        }

        mHostnameEt.requestFocus();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveRecentHostList();
    }


    /*
    Detach the callbacks so not to leak memory.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDetach()");
        }
        mCallbacks = null;
    }


    /*
    Disable the scan button
    start the ping host task to determine the latency and
    show the transition dialog.
     */
    @Override
    public void onClick(View v) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onClick()");
        }
        switch (v.getId()) {
            case R.id.button_start_scan:
                startScan();
                break;
            case R.id.button_show_recent_hosts:
                showPopup();
                break;
        }


    }

    /*
    Start the scan
     */
    private void startScan() {
        mStartScanButton.setEnabled(false);

        // Save a boolean to indicate that the app has been used once
        // This is used because we want to display either localhost for first use
        // or whatever the most recent search was in the hostname EditText
        // Only do this if this is the first time the app is used
        SharedPreferences settings = getActivity().getPreferences(MainActivity.MODE_PRIVATE);
        if (!settings.getBoolean(PREFS_SCAN_CLICKED, false)) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(PREFS_SCAN_CLICKED, true);
            editor.apply();
        }

        // Add this host to the recent hosts list
        addItemToRecentHosts(mHostname);

        // start the scan
        new PingHostTask().execute(mHostname);
    }

    /*
    Show the recent hosts menu.
     */
    private void showPopup() {

        // Don't do anything if there are no recent entries
        if (mRecentHostList.isEmpty()) {
            return;
        }

        PopupMenu popup = new PopupMenu(getActivity(), mHostnameEt);
        for (String item : mRecentHostList) {
            popup.getMenu().add(item);
        }

        popup.setOnMenuItemClickListener(this);
        popup.setOnDismissListener(this);

        // Animate the arrow to point up.
        flipArrowUp(getActivity().findViewById(R.id.button_show_recent_hosts));
        popup.show();
    }


    /*
    Private Async task to resolve and check the hosts existence in the background.

    Report success or failure back to the activity in onPostExecute();
     */

    private class PingHostTask extends AsyncTask<String, Boolean, Boolean> {

        private final String INNER_TAG = PingHostTask.class.getName();
        private static final int FIRST_DIALOG_DISPLAY_TIME = 2000;
        private static final int SECOND_DIALOG_DISPLAY_TIME = 1500;


        private int averagePingTime = 0;

        @Override
        protected void onPreExecute() {
            if (BuildConfig.DEBUG) {
                Log.d(INNER_TAG, "onPreExecute()");
            }
            mCallbacks.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... values) {
            if (BuildConfig.DEBUG) {
                Log.d(INNER_TAG, "doInBackground()");
            }

            String host = values[0];
            boolean success = true;
            long startPingTestTime;

            // use the total ping test time to determine whether to keep the dialog up
            int totalPingTestTime = 0;

            InetAddress address = null;
            try {
                address = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                success = false;
            }

            if (success) {
                startPingTestTime = System.currentTimeMillis();
                averagePingTime = getHostLatency(address);
                totalPingTestTime += (System.currentTimeMillis() - startPingTestTime);
            }

            // Adjust the time so the user actually sees a mDialog
            // and wait ..
            if (totalPingTestTime < FIRST_DIALOG_DISPLAY_TIME) {
                int waitTime = FIRST_DIALOG_DISPLAY_TIME - totalPingTestTime;

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


            // Ask the mDialog to update its contents
            publishProgress(success);

            // Just busy wait for 1 second before dismissing the mDialog
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
                Log.d(INNER_TAG, "onProgressUpdate()");
            }
            mCallbacks.onProgressUpdate(values[0], averagePingTime);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (BuildConfig.DEBUG) {
                Log.d(INNER_TAG, "onPostExecute()");
            }
            mCallbacks.onPostExecute(new ScanData(mHostname, mStartPort, mEndPort, averagePingTime), success);

        }
    }


    /*
    IsValidPort(String port)

    Check that the port is within port range (0 - 65535)
     */
    private boolean isValidPort(int portNumber) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "isValidPort()");
        }
        return !((portNumber < 1) || (portNumber > LAST_PORT));
    }


    /*
    Returns true if both ports are valid and within the proper range.
    start port < end port
    range <= 100
    start port > 0
    end port < LAST_PORT (65535)

    Uses isValidPort to check range 1 - 65535
     */
    private boolean checkReadyToScan() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "checkReadyToScan()");
        }
        TextView textView = (TextView) getActivity().findViewById(R.id.tv_enter_port_sub);

        // Don't have both valid ports (1-65535)
        if (!((isValidPort(mStartPort)) && (isValidPort(mEndPort)))) {
            textView.setText(R.string.fragment_main_input_tv_invalid_port);
            textView.setTextColor(getResources().getColor(R.color.light_red));
            return false;
        }

        // start port is greater than or equal to end (cannot scan this range)
        if ((mStartPort >= mEndPort)) {
            textView.setText(R.string.fragment_main_input_tv_start_less_end);
            textView.setTextColor(getResources().getColor(R.color.light_red));
            return false;
        }

        // Check that less than 100 ports have been selected
        if (!((mEndPort - mStartPort) <= MAX_PORTS)) {
            textView.setText(R.string.fragment_main_input_tv_invalid_port_range);
            textView.setTextColor(getResources().getColor(R.color.light_red));
            return false;
        }

        // All good if made it here.
        textView.setText(R.string.fragment_main_input_tv_enter_port_sub);
        textView.setTextColor(getResources().getColor(R.color.small_text_grey));
        // just block for now. Ticket raised to enhance.
        if (mHostname == null || mHostname.isEmpty()) {
            TextView hostNameSub = (TextView) getActivity().findViewById(R.id.tv_enter_ip_sub);
            hostNameSub.setText(R.string.fragment_main_input_tv_host_required);
            hostNameSub.setTextColor(getResources().getColor(R.color.light_red));
            return false;
        }

        TextView hostNameSub = (TextView) getActivity().findViewById(R.id.tv_enter_ip_sub);
        hostNameSub.setText(R.string.fragment_main_input_tv_enter_ip_sub);
        hostNameSub.setTextColor(getResources().getColor(R.color.small_text_grey));

        return true;
    }


    /*
    Attempt to check the host latency. Firstly on the isReachable but if that fails
    attempt to contact some known ports on the host. If all else fails which happens when all attempts
    time out, then return a large value so the user is shown > 1000ms.

    @param hostAddress the address of the host to test
    TODO move this into network helper stuff when implementing ping
     */
    private int getHostLatency(@NonNull InetAddress hostAddress) {

        final int timesToPing = 5;

        final int ftp = 21;
        final int ssh = 22;
        final int http = 80;
        final int https = 443;

        long startPingTime;
        int totalPingTime = 0;
        boolean pingTimeDetermined = false;

        try {
            startPingTime = System.currentTimeMillis();
            for (int i = 0; i < timesToPing; i++) {

                boolean reachable = hostAddress.isReachable(PING_TIMEOUT);
                if (!reachable) {
                    pingTimeDetermined = false;
                    break;
                }

                //made it to the end of the loops
                if (i == (timesToPing - 1)) {
                    pingTimeDetermined = true;
                }
            }
            totalPingTime += (System.currentTimeMillis() - startPingTime);
        } catch (Exception e) {
            // if this throws just carry on but set done to false
            pingTimeDetermined = false;
        }

        //Try HTTP
        if (!pingTimeDetermined) {
            totalPingTime = getTotalPingTime(
                    hostAddress,
                    timesToPing,
                    http,
                    PING_TIMEOUT
            );
            pingTimeDetermined = totalPingTime != -1;
        }

        // Try HTTPS
        if (!pingTimeDetermined) {
            totalPingTime = getTotalPingTime(
                    hostAddress,
                    timesToPing,
                    https,
                    PING_TIMEOUT
            );
            pingTimeDetermined = totalPingTime != -1;
         }

        // Try FTP Port
        if (!pingTimeDetermined) {
            totalPingTime = getTotalPingTime(
                    hostAddress,
                    timesToPing,
                    ftp,
                    PING_TIMEOUT
            );
            pingTimeDetermined = totalPingTime != -1;
        }

        // Try SSH port
        if (!pingTimeDetermined) {
            totalPingTime = getTotalPingTime(
                    hostAddress,
                    timesToPing,
                    ssh,
                    PING_TIMEOUT
            );
            pingTimeDetermined = totalPingTime != -1;
        }

        if (pingTimeDetermined) {
            return totalPingTime / timesToPing;
        } else {
            return PING_TIMEOUT;
        }

    }

    /*
    Returns the average host latency by scanning a single port multiple time.
    or -1 for an error.

    @param hostAddress the address of the host to test
    @param timesToScan how many time to do a socket connect
    @param portNumber the port number to attempt this scan
    @param timeOut maximum time to wait for a connection
     */
    private int getTotalPingTime(
            @NonNull InetAddress hostAddress,
            int timesToScan,
            int portNumber,
            int timeOut) {

        long startPingTime;
        int totalPingTime = 0;

        // set the start ping time to now
        startPingTime = System.currentTimeMillis();
        try {
            for (int i = 0; i < timesToScan; i++) {

                Socket scanSock = new Socket();
                scanSock.connect(new InetSocketAddress(hostAddress, portNumber), timeOut);
                scanSock.close();
            }
            totalPingTime += (System.currentTimeMillis() - startPingTime);
        } catch (Exception e) {
            // failed
            totalPingTime = -1;
        }
        return totalPingTime;
    }


    /*
   Add the host string to the array list into position 0 of the list.
    */
    private void addItemToRecentHosts(@NonNull String host) {

        // Menu List should never be null when this method is called
        // so log an error if menu list is null.
        if (mRecentHostList == null) {
            Log.e(TAG, "addItemToRecentHosts() - Menu List was Null");
            return;
        }

        // if new item already in the list remove it here and then re-add it to pos 0.
        for (int i = 0; i < mRecentHostList.size(); i++) {
            if (host.equals(mRecentHostList.get(i))) {
                mRecentHostList.remove(i);
            }
        }

        // Otherwise add to position so it is seen as the most recent item.
        mRecentHostList.add(0, host);

        // if adding the new item made the list to big, remove the last item
        if (mRecentHostList.size() > MAX_RECENT_SIZE) {
            mRecentHostList.remove(mRecentHostList.size() - 1);
        }

    }


    /*
    Saves the recent hosts list to file.
     */
    private void saveRecentHostList() {
        try {

            FileOutputStream fos =
                    getActivity().openFileOutput(STORED_RECENT_HOSTS_FILENAME, Context.MODE_PRIVATE);

            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(mRecentHostList);
            oos.close();

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /*
     Reads the recent hosts list from file.
     */
    private ArrayList<String> loadRecentHostList() {

        ArrayList<String> recentList = new ArrayList<>();
        Object fileObject = null;
        try {
            FileInputStream recentHostsRead = getActivity().openFileInput(STORED_RECENT_HOSTS_FILENAME);
            ObjectInputStream ois = new ObjectInputStream(recentHostsRead);
            fileObject = ois.readObject();
            ois.close();

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        // If the file object is an array list add the items to the list
        if (fileObject instanceof ArrayList<?>) {

            // Get the List.
            ArrayList<?> al = (ArrayList<?>) fileObject;
            if (al.size() > 0) {
                for (int i = 0; i < al.size(); i++) {

                    // check these are strings
                    Object o = al.get(i);
                    if (o instanceof String) {
                        // add the strings to menulist
                        recentList.add((String) o);
                    }
                }
            }
        }
        return recentList;
    }


    private void flipArrowDown(final View v) {
        v.clearAnimation();

        final Animator in = AnimatorInflater.loadAnimator(
                getActivity(),
                R.animator.card_flip_left_in);
        in.setTarget(v);


        Animator.AnimatorListener flipOutListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                v.findViewById(R.id.button_show_recent_hosts)
                        .setBackgroundResource(R.drawable.ic_keyboard_arrow_down_black_24dp);
                in.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        };

        Animator out = AnimatorInflater.loadAnimator(getActivity(),
                R.animator.card_flip_left_out);
        out.setTarget(v);
        out.addListener(flipOutListener);
        out.start();

    }

    private void flipArrowUp(final View v) {
        v.clearAnimation();

        final Animator in = AnimatorInflater.loadAnimator(getActivity(),
                R.animator.card_flip_right_in);
        in.setTarget(v);


        // A listener is required to determine the end fo the first animation
        Animator.AnimatorListener flipOutListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                v.findViewById(R.id.button_show_recent_hosts)
                        .setBackgroundResource(R.drawable.ic_keyboard_arrow_up_black_24dp);
                in.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        };

        Animator out = AnimatorInflater.loadAnimator(getActivity(),
                R.animator.card_flip_right_out);
        out.setTarget(v);
        out.addListener(flipOutListener);
        out.start();
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        mHostnameEt.setText(item.toString());
        return false;
    }

    @Override
    public void onDismiss(PopupMenu menu) {
        flipArrowDown(getActivity().findViewById(R.id.button_show_recent_hosts));
    }
}

