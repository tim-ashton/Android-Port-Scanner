/*
 * Created by Tim Ashton on 14/01/15.
 */

package com.timashton.portscanner;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;


public class MainActivity extends ActionBarActivity
        implements MainInputFragment.MainInputCallbacks
        , ScanWorkerFragment.ScanFragmentCallbacks {

    private static final String TAG_SCAN_WORKER_FRAGMENT = "scan_worker_fragment";
    private static final String TAG_MAIN_INPUT_FRAGMENT = "main_input_fragment";
    private static final String TAG_SCAN_DESC_FRAGMENT = "scan_desc_fragment";
    private static final String TAG_SCAN_LIST_FRAGMENT = "scan_list_fragment";

    ScanWorkerFragment mScanWorkerFragment;
    MainInputFragment mMainInputFragment;  // TODO could localize.


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getFragmentManager();

        if (savedInstanceState == null) {
            fm.beginTransaction()
                    .add(R.id.container, MainInputFragment.newInstance(), TAG_MAIN_INPUT_FRAGMENT)
                    .commit();
        }

        // If the Fragment is non-null, then it is being retained
        // over a configuration change.
        // Otherwise a new one is required.
        mScanWorkerFragment = (ScanWorkerFragment) fm.findFragmentByTag(TAG_SCAN_WORKER_FRAGMENT);
        if (mScanWorkerFragment == null) {
            mScanWorkerFragment = ScanWorkerFragment.newInstance();
            fm.beginTransaction().add(mScanWorkerFragment, TAG_SCAN_WORKER_FRAGMENT).commit();
        }

    }


    @Override
    public void startScan(String hostName, int startPort, int endPort) {

        getFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.animator.slide_in_left,
                        R.animator.slide_out_top,
                        R.animator.slide_in_bottom,
                        R.animator.slide_out_right)
                .replace(R.id.container, ScanDescriptionFragment.newInstance(), TAG_SCAN_DESC_FRAGMENT)
                .add(R.id.container, ScannerListFragment.newInstance(), TAG_SCAN_LIST_FRAGMENT)
                .addToBackStack(null)
                .commit();

        mScanWorkerFragment.startScannerThread(hostName, startPort, endPort);
    }

    @Override
    public void addPortFound(String portFoundResult) {
        Log.e("mainactivity", portFoundResult );

        ScannerListFragment resultListFragment = (ScannerListFragment) getFragmentManager()
                .findFragmentByTag(TAG_SCAN_LIST_FRAGMENT);

        if (resultListFragment != null) {
            resultListFragment.updateListView(portFoundResult);
        }
    }
}
