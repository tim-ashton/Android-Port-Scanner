/*
 * Created by Tim Ashton on 14/05/15.
 */

package com.timashton.portscanner;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ScanDescriptionFragment extends Fragment {

    private static final String TAG = ScanDescriptionFragment.class.getName();
    private static final String TAG_BUNDLE_HOSTNAME = "host_name";

    private TextView mNumbPortsFoundTV;
    private int mNumbPortsFoundi = 0;
    private TextView mTitleDoneTV;

    public static ScanDescriptionFragment newInstance(String hostName) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "newInstance()");
        }
        ScanDescriptionFragment scanDescriptionFragment = new ScanDescriptionFragment();

        Bundle args = new Bundle();
        args.putString(TAG_BUNDLE_HOSTNAME, hostName);
        scanDescriptionFragment.setArguments(args);

        return scanDescriptionFragment;
    }

    public ScanDescriptionFragment() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "ScanDescriptionFragment()");
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
        View view = inflater.inflate(R.layout.fragment_scan_description, container, false);

        mNumbPortsFoundTV = (TextView) view.findViewById(R.id.tv_scan_desc_ports_found_number);
        mTitleDoneTV = (TextView) view.findViewById(R.id.tv_scan_desc_title_done);
        mTitleDoneTV.setVisibility(View.INVISIBLE);

        TextView hostnameTV = (TextView) view.findViewById(R.id.tv_scan_desc_host);
        hostnameTV.setText(getArguments().getString(TAG_BUNDLE_HOSTNAME));

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onPause()");
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onResume()");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onDetach()");
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onDestroy()");
        }
    }

    /*
    Increment the number of ports found and update the UI.
     */
    public void incrementPortsFound() {
        mNumbPortsFoundTV.setText(" " + ++mNumbPortsFoundi);
    }

    public void notifyScanDone() {
        mTitleDoneTV.setVisibility(View.VISIBLE);
    }

}
