/*
 * Created by Tim Ashton on 14/01/15.
 */

package com.timashton.portscanner;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;


public class MainActivity extends Activity
        implements MainInputFragment.MainInputCallbacks
        , ScanWorkerFragment.ScanFragmentCallbacks {

    private static final String TAG = MainActivity.class.getName();
    private static final String TAG_SCAN_WORKER_FRAGMENT = "scan_worker_fragment";
    private static final String TAG_MAIN_INPUT_FRAGMENT = "main_input_fragment";
    private static final String TAG_SCAN_DESC_FRAGMENT = "scan_desc_fragment";
    private static final String TAG_SCAN_LIST_FRAGMENT = "scan_list_fragment";

    private static final int TRANSITION_DELAY = 350; //time to wait before animating frag slide

    private ProgressDialog mProgressSpinner;
    ScanWorkerFragment mScanWorkerFragment;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onCreate()");
        }

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
    public void onResume(){
        super.onResume();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onResume()");
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onPause()");
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onDestroy()");
        }
        mProgressSpinner = null;
        mScanWorkerFragment = null;
    }

    @Override
    public void startScan(String hostName, int startPort, int endPort, int avgPing) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "startScan()");
        }

        final String host = hostName;
        final int sPort = startPort;
        final int ePort = endPort;
        final int avg = avgPing;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getFragmentManager().beginTransaction()
                        .setCustomAnimations(
                                R.animator.slide_in_left,
                                R.animator.slide_out_top,
                                R.animator.slide_in_bottom,
                                R.animator.slide_out_right)
                        .replace(R.id.container,
                                ScanDescriptionFragment.newInstance(host),
                                TAG_SCAN_DESC_FRAGMENT)
                        .add(R.id.container,
                                ScannerListFragment.newInstance(),
                                TAG_SCAN_LIST_FRAGMENT)
                        .addToBackStack(null)
                        .commit();

                mScanWorkerFragment.startScannerThread(host, sPort, ePort, avg);
            }
        }, TRANSITION_DELAY);
    }

    @Override
    public void addPortFound(String portFoundResult) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "addPortFound()");
        }

        ScannerListFragment resultListFragment =
                (ScannerListFragment) getFragmentManager()
                        .findFragmentByTag(TAG_SCAN_LIST_FRAGMENT);

        ScanDescriptionFragment resultDescriptionFragment =
                (ScanDescriptionFragment) getFragmentManager()
                        .findFragmentByTag(TAG_SCAN_DESC_FRAGMENT);

        if (resultListFragment != null) {
            resultListFragment.updateListView(portFoundResult);
        }

        if (resultDescriptionFragment != null) {
            resultDescriptionFragment.incrementPortsFound();
        }
    }

    @Override
    public void scanFinished() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "scanFinished()");
        }
        ScanDescriptionFragment resultDescriptionFragment =
                (ScanDescriptionFragment) getFragmentManager()
                        .findFragmentByTag(TAG_SCAN_DESC_FRAGMENT);
        if (resultDescriptionFragment != null) {
            resultDescriptionFragment.notifyScanDone();
        }
    }

    @Override
    public void scanStarting() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "scanStarting()");
        }
        showSpinner();
    }

    @Override
    public void scanStopping() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "scanStopping()");
        }
        hideSpinner();
    }


    /*
     * showSpinner()
     * Show the spinner progress dialog.
     */
    public void showSpinner() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "showSpinner()");
        }
        if (mProgressSpinner == null) {
            mProgressSpinner = this.createProgressDialog(this);
            mProgressSpinner.show();
        } else {
            mProgressSpinner.show();
        }
    }

    /* hideSpinner()
     * Hide the spinner progress dialog.
     */
    public void hideSpinner() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "hideSpinner()");
        }
        if (mProgressSpinner != null) {
            mProgressSpinner.dismiss();
        }
    }


    /* createProgressDialog(Context mContext)
     *
     * Create a custom progress dialog that runs while items are being
     * added to the list fragment.
     */
    public ProgressDialog createProgressDialog(Context mContext) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "createProgressDialog()");
        }
        ProgressDialog dialog = new ProgressDialog(mContext);
        try {
            dialog.show();
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        }
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.progress_spinner);
        return dialog;
    }

}
