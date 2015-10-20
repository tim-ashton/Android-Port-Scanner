/*
 * Created by Tim Ashton on 14/01/15.
 */

package com.timashton.aportscanner;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;


public class MainActivity extends Activity
        implements MainInputFragment.MainInputCallbacks
        , ScanWorkerFragment.ScanFragmentCallbacks {

    private static final String TAG = MainActivity.class.getName();
    private static final String TAG_HIDE_KEYBOARD = "hide_keyboard";

    private static final int FRAGMENT_ANIMATION_DURATION = 1000;
    private static final int SHOW_KEYBOARD_DELAY = 250;

    private boolean allowKeyboard = true;

    private static ScanWorkerFragment mScanWorkerFragment;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get contents of bundle
        if (savedInstanceState != null) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Retrieved bundle onCreate()");
            }
            allowKeyboard = savedInstanceState.getBoolean(TAG_HIDE_KEYBOARD);

        } else {

            mScanWorkerFragment = ScanWorkerFragment.newInstance();

            final FragmentManager fm = getFragmentManager();

            // Add the fragments
            fm.beginTransaction()
                    .add(R.id.container, MainInputFragment.newInstance(), MainInputFragment.TAG)
                    .add(mScanWorkerFragment, ScanWorkerFragment.TAG)// Add the scan worker fragment
                    .commit();
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (!allowKeyboard) {
            getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(TAG_HIDE_KEYBOARD, allowKeyboard);
    }

    /*
    Hide the keyboard and block it re-appearing.
    Show the transition dialog.
     */
    @Override
    public void onPreExecute() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPreExecute()");
        }

        hideKeyboard(this);
        allowKeyboard = false;

        // Create and show the dialog.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        TransitionDialogFragment transitionDialogFragment = TransitionDialogFragment.newInstance();
        transitionDialogFragment.setCancelable(false);
        transitionDialogFragment.show(ft, TransitionDialogFragment.TAG);
    }

    /*
    Update the text and stop the progress bar in the
    transition dialog.
     */
    @Override
    public void onProgressUpdate(boolean success, int avgPing) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onProgressUpdate()");
        }

        TransitionDialogFragment transitionDialogFragment =
                (TransitionDialogFragment) getFragmentManager()
                        .findFragmentByTag(TransitionDialogFragment.TAG);

        if (transitionDialogFragment != null) {
            transitionDialogFragment.updateDialog(success, avgPing);
        }
    }

    /*
    Dismiss the transition dialog.
    Create the Scanner List fragment and the Scanner Description fragment, Show them.
    Start the thread pool handled by the ScanWorkerFragment.
     */
    @Override
    public void onPostExecute(final ScanData scanData, boolean runScan) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPostExecute()");
        }
        // Run the scan if a result came back from async task
        if (runScan) {

            final FragmentManager fm = getFragmentManager();
            final FragmentTransaction ft = fm.beginTransaction();

            ft.setCustomAnimations(
                    R.animator.slide_in_left,
                    R.animator.slide_out_top,
                    R.animator.slide_in_bottom,
                    R.animator.slide_out_right);

            ft.replace(R.id.container,
                    ScanFragment.newInstance(
                            scanData), ScanFragment.TAG)
                    .addToBackStack(null)
                    .commit();

            // Get the number of ports to be scanned work out a multiplying factor to make 100
            // by the end of the scan
            int numbOfPorts = scanData.endPort - scanData.startPort;
            double progressMultiplier = 100 / numbOfPorts;


            TransitionDialogFragment transitionDialogFragment =
                    (TransitionDialogFragment) getFragmentManager()
                            .findFragmentByTag(TransitionDialogFragment.TAG);

            if (transitionDialogFragment != null) {
                transitionDialogFragment.switchToScanningMode(progressMultiplier);
            }

            // Create and run this handler to delay the start of the scanner thread
            // until after the animation has finished.
            // This ensures that the slide animations on the fragments are seen as expected.
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    mScanWorkerFragment.startScannerThread(scanData);
                }
            }, FRAGMENT_ANIMATION_DURATION);

        } else {

            // Remove the dialog
            // Allow the keyboard to be shown
            // and show the keyboard after short delay. Delay allows the system to keep up
            // otherwise the keyboard will not be shown.S
            TransitionDialogFragment transitionDialogFragment =
                    (TransitionDialogFragment) getFragmentManager()
                            .findFragmentByTag(TransitionDialogFragment.TAG);

            if (transitionDialogFragment != null) {
                transitionDialogFragment.dismiss();
            }

            allowKeyboard = true;

            // Show the keyboard.
            // Need a short delay or the keyboard will not pop up
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    View view = getCurrentFocus();
                    if (view != null) {
                        InputMethodManager inputManager =
                                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            }, SHOW_KEYBOARD_DELAY);

        }
    }

    /*
    Add the found item to the scanner list.
     */
    @Override
    public void addPortFound(String portFoundResult) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "addPortFound()");
        }

        ScanFragment scanFragment =
                (ScanFragment) getFragmentManager()
                        .findFragmentByTag(ScanFragment.TAG);

        if (scanFragment != null) {
            scanFragment.updateListView(portFoundResult);
            scanFragment.incrementPortsFound();
        }
    }

    /*
    Re-enable (allow the keyboard)
    Notify the scan description that the scan is finished so that
    the description can be updated.
     */
    @Override
    public void scanFinished() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "scanFinished()");
        }

        ScanFragment scanFragment =
                (ScanFragment) getFragmentManager()
                        .findFragmentByTag(ScanFragment.TAG);

        if (scanFragment != null) {

            if(scanFragment.getNumbPortsFoundValue() == 0){
                scanFragment.updateListView(
                        getResources().getString(R.string.fragment_scan_list_none_found));
            }
            scanFragment.notifyScanDone();
        }

        TransitionDialogFragment transitionDialogFragment =
                (TransitionDialogFragment) getFragmentManager()
                        .findFragmentByTag(TransitionDialogFragment.TAG);

        if (transitionDialogFragment != null) {
            transitionDialogFragment.dismiss();
        }

        allowKeyboard = true;
    }


    /*
    Update the progress bar in the transition dialog fragment.
     */
    @Override
    public void updateProgress() {

        TransitionDialogFragment transitionDialogFragment =
                (TransitionDialogFragment) getFragmentManager()
                        .findFragmentByTag(TransitionDialogFragment.TAG);

        if (transitionDialogFragment != null) {
            transitionDialogFragment.updateProgress();
        }
    }

    /*
    Catch the back key press. If returning to the start screen
    show the soft keyboard.
     */
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            // any time the back button press registers it will take the app
            // to a screen where the keyboard is allowed.
            allowKeyboard = true;
        }
        return super.onKeyDown(keyCode, event);
    }


    /*
    Request the soft keyboard to be dismissed.
     */
    public static void hideKeyboard(Activity activity) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "hideKeyboard()");
        }

        // Check if no view has focus:
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager =
                    (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

            inputManager.hideSoftInputFromWindow(view.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
