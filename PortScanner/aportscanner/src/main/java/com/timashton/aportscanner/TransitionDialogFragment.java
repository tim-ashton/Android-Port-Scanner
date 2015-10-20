/*
 * Created by Tim Ashton on 28/05/15.
 *
 * TransitionDialogFragment
 *
 * The dialog that shows to the user while the scanning and moving to the result fragments
 * takes place.
 */

package com.timashton.aportscanner;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;


public class TransitionDialogFragment extends DialogFragment {

    public static final String TAG = TransitionDialogFragment.class.getName();

    private static final String TAG_DIALOG_TITLE = "current_title";
    private static final String TAG_INDETERMINATE = "indeterminate";
    private static final String TAG_PROGRESS_MULTIPLIER = "progress_multiplier";
    private static final String TAG_CURRENT_PROGRESS = "current_progress";

    private ProgressBar mProgressBar;
    private String mDialogTitle;
    private boolean mProgressIndeterminate = true;
    private int mCurrentProgress;
    private double mProgressMultiplier;

    static TransitionDialogFragment newInstance() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "newInstance()");
        }

        return new TransitionDialogFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        View v = inflater.inflate(R.layout.progress_dialog_fragment, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCancelable(false);

        mProgressBar = (ProgressBar) v.findViewById(R.id.dialog_transition_progress_bar);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstance) {
        super.onViewCreated(view, savedInstance);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onViewCreated()");
        }
        if (savedInstance != null) {
            mDialogTitle = savedInstance.getString(TAG_DIALOG_TITLE);
            mProgressIndeterminate = savedInstance.getBoolean(TAG_INDETERMINATE);
            mProgressMultiplier = savedInstance.getDouble(TAG_PROGRESS_MULTIPLIER);
            mCurrentProgress = savedInstance.getInt(TAG_CURRENT_PROGRESS);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onResume()");
        }

        // kep the activity on while scanning
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (mDialogTitle != null) {
            TextView textView = (TextView) getDialog().findViewById(R.id.dialog_transition_text);
            textView.setText(mDialogTitle);

            mProgressBar.setIndeterminate(mProgressIndeterminate);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // remove the keep screen on flag
        // should only be paused on configuration change
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }



    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onSaveInstanceState()");
        }
        bundle.putString(TAG_DIALOG_TITLE, mDialogTitle);
        bundle.putBoolean(TAG_INDETERMINATE, mProgressIndeterminate);
        bundle.putDouble(TAG_PROGRESS_MULTIPLIER, mProgressMultiplier);
        bundle.putInt(TAG_CURRENT_PROGRESS, mCurrentProgress);
    }

    /*
    Update the transition dialog with new text.
    Cancel the indeterminate progress bar.
     */
    public void updateDialog(boolean success, int avgPing) {

        TextView textView = (TextView) getDialog().findViewById(R.id.dialog_transition_text);
        ProgressBar progressbar = (ProgressBar) getDialog().findViewById(R.id.dialog_transition_progress_bar);

        // Update contents of mDialog
        // and set the indeterminate boolean as required
        if (success) {
            if (avgPing >= MainInputFragment.PING_TIMEOUT) {
                mDialogTitle = "Latency > " + MainInputFragment.PING_TIMEOUT + " ms";
                textView.setText(mDialogTitle);
            } else {
                mDialogTitle = "Latency " + avgPing + " ms";
                textView.setText(mDialogTitle);
            }

            progressbar.setIndeterminate(mProgressIndeterminate = false);
        } else {
            mDialogTitle = getResources().getString(R.string.dialog_transition_host_unreachable);
            textView.setText(mDialogTitle);

            mProgressBar.setIndeterminate(mProgressIndeterminate = false);
        }
    }

    /*
    Switch this dialog over to scanning mode.
    It will remain visible to the user for the duration of the scan.
     */
    public void switchToScanningMode(double progressMultiplier) {
        mProgressMultiplier = progressMultiplier;
        mCurrentProgress = 0;
        mDialogTitle = getResources().getString(R.string.fragment_scan_description_title);

        TextView textView = (TextView) getDialog().findViewById(R.id.dialog_transition_text);
        textView.setText(mDialogTitle);

        mProgressBar.setIndeterminate(mProgressIndeterminate = false);
    }

    /*
    Update the progress displayed by incrementing the progress.
   */
    public void updateProgress() {
        mProgressBar.setProgress((int) (mProgressMultiplier * ++mCurrentProgress));
    }
}
