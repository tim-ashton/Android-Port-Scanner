/*
 * Created by Tim Ashton on 05 May 2015.
 */

package com.timashton.portscanner;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;


public class MainInputFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = MainInputFragment.class.getName();

    private Button mStartScanButton;
    private EditText mHostname;
    private EditText mStartPort;
    private EditText mEndPort;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main_input, container, false);

        mStartScanButton = (Button) view.findViewById(R.id.button_start_scan);
        mStartScanButton.setOnClickListener(this);

        mHostname = (EditText) view.findViewById(R.id.et_ip_hostname);
        mStartPort = (EditText) view.findViewById(R.id.et_start_port);
        mEndPort = (EditText) view.findViewById(R.id.et_end_port);

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
        mStartScanButton.setEnabled(false);

        String hostName = mHostname.getText().toString();
        String startPort = mStartPort.getText().toString();
        String endPort = mEndPort.getText().toString();
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

}
