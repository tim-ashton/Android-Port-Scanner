package com.timashton.aportscanner;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;

/*
 * Created by Tim Ashton on 6/06/15.
 *
 * This fragment reports all scan results back to the user.
 */
public class ScanFragment extends Fragment {

    public static final String TAG = ScanFragment.class.getName();
    private static final String TAG_BUNDLE_HOSTNAME = "host_name";
    private static final String TAG_PORTS_FOUND = "ports_found";
    private static final String TAG_DONE_SHOW_HIDE = "scan_done";
    private static final String TAG_START_PORT = "start_port";
    private static final String TAG_END_PORT = "end_port";
    private static final String TAG_AVG_PING = "avg_ping";
    private static final String ITEMS_LIST_TAG = "list_tag";

    private TextView mNumbPortsFoundTV;
    private int mNumbPortsFoundi = 0;


    private TextView mTitleDoneTV;
    private boolean showDoneMessage = false;

    private ScannerListAdapter mDemoListAdapter;
    private ArrayList<ScannerListItem> mItems;
    private ListView mFragmentListView;
    private int mOldFirstVisibleItem;

    public static ScanFragment newInstance(ScanData scanData) {

        ScanFragment scanFragment = new ScanFragment();

        Bundle args = new Bundle();
        args.putString(TAG_BUNDLE_HOSTNAME, scanData.hostName);
        args.putInt(TAG_START_PORT, scanData.startPort);
        args.putInt(TAG_END_PORT, scanData.endPort);
        args.putInt(TAG_AVG_PING, scanData.averagePing);
        scanFragment.setArguments(args);

        return scanFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreate()");
        }

        if (savedInstanceState != null) {
            mNumbPortsFoundi = savedInstanceState.getInt(TAG_PORTS_FOUND);
            showDoneMessage = savedInstanceState.getBoolean(TAG_DONE_SHOW_HIDE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateView()");
        }
        View view = inflater.inflate(R.layout.fragment_scan, container, false);

        AdView adView = (AdView) view.findViewById(R.id.adView);

        // Create an ad request.
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();

        adView.loadAd(adRequestBuilder.build());

        if (savedInstanceState != null) {
            mItems = savedInstanceState.getParcelableArrayList(ITEMS_LIST_TAG);
        } else {
            mItems = new ArrayList<>();
        }

        mNumbPortsFoundTV = (TextView) view.findViewById(R.id.tv_scan_desc_ports_found_number);
        mNumbPortsFoundTV.setText(Integer.toString(mNumbPortsFoundi));

        mTitleDoneTV = (TextView) view.findViewById(R.id.tv_scan_desc_title_done);
        if (!showDoneMessage) {
            mTitleDoneTV.setVisibility(View.INVISIBLE);
        }

        // Retrieve and show the hostname
        TextView hostnameTV = (TextView) view.findViewById(R.id.tv_scan_desc_host);
        hostnameTV.setText(getArguments().getString(TAG_BUNDLE_HOSTNAME));

        // Retrieve and show port range numbers
        TextView portRangeTV = (TextView) view.findViewById(R.id.tv_scan_desc_port_range_numbs);
        int startPort = getArguments().getInt(TAG_START_PORT);
        int endPort = getArguments().getInt(TAG_END_PORT);
        portRangeTV.setText(startPort + " - " + endPort);

        // Retrieve and show the ping time
        TextView hostPingTime = (TextView) view.findViewById(R.id.tv_scan_desc_host_ping_numb);
        Integer latency = getArguments().getInt(TAG_AVG_PING);
        if (latency > MainInputFragment.PING_TIMEOUT) {
            hostPingTime.setText("> " + latency.toString());
        } else {
            hostPingTime.setText(latency.toString());
        }

        mFragmentListView =
                (ListView) view.findViewById(R.id.fragment_list);

        mDemoListAdapter = new ScannerListAdapter(getActivity(), mItems);
        mFragmentListView.setAdapter(mDemoListAdapter);


        // Set the onScrollListener to notify the list adapter about any scroll state
        // changes. This allows the list adapter to know which item needs to be animated.
        mFragmentListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            public void onScroll(AbsListView view,
                                 int firstVisibleItem,
                                 int visibleItemCount,
                                 int totalItemCount) {
            }

            public void onScrollStateChanged(AbsListView view, int scrollState) {

                final ListView lw = mFragmentListView;

                if (scrollState == 0)

                    if (view.getId() == lw.getId()) {
                        final int currentFirstVisibleItem = lw.getFirstVisiblePosition();

                        if (currentFirstVisibleItem > mOldFirstVisibleItem) {
                            mDemoListAdapter.setScrollingUp(false);
                            mDemoListAdapter.setScrollingDown(true);
                        } else if (currentFirstVisibleItem < mOldFirstVisibleItem) {
                            mDemoListAdapter.setScrollingUp(true);
                            mDemoListAdapter.setScrollingDown(false);
                        }

                        mOldFirstVisibleItem = currentFirstVisibleItem;
                    }
            }
        });

        return view;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onSaveInstanceState()");
        }
        outState.putInt(TAG_PORTS_FOUND, mNumbPortsFoundi);
        outState.putBoolean(TAG_DONE_SHOW_HIDE, showDoneMessage);

        ArrayList<ScannerListItem> bundledListItems = mDemoListAdapter.getList();
        outState.putParcelableArrayList(ITEMS_LIST_TAG, bundledListItems);

    }

    /*
    Increment the number of ports found and update the UI.
     */
    public void incrementPortsFound() {
        mNumbPortsFoundTV.setText(Integer.toString(++mNumbPortsFoundi));
    }

    public void notifyScanDone() {
        showDoneMessage = true;
        mTitleDoneTV.setVisibility(View.VISIBLE);
    }

    public void updateListView(String text) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "updateListView()");
        }
        ScannerListItem item = new ScannerListItem(text);
        mItems.add(item);
        mDemoListAdapter.notifyDataSetChanged();
    }

    public int getNumbPortsFoundValue(){
        return mNumbPortsFoundi;
    }
}
