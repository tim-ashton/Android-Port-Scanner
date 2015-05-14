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

public class ScanDescriptionFragment extends Fragment {

    private static final String TAG = ScanDescriptionFragment.class.getName();

    public static ScanDescriptionFragment newInstance() {
        return new ScanDescriptionFragment();
    }

    public ScanDescriptionFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        View rootView = inflater.inflate(R.layout.fragment_scan_description, container, false);
        return rootView;
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.i(TAG, "onPause()");
    }

}
