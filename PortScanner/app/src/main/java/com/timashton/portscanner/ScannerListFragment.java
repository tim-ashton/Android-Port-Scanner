/*
 * Created by Tim Ashtonon 1/02/15.
 */

package com.timashton.portscanner;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class ScannerListFragment extends Fragment {

    private static final String TAG = ScannerListFragment.class.getName();
    private static final String ITEMS_LIST_TAG = "list_tag";

    private ScannerListAdapter mDemoListAdapter;
    private ArrayList<ScannerListItem> mItems;
    private ListView mFragmentListView;
    private int mOldFirstVisibleItem;

    public static ScannerListFragment newInstance() {
        return new ScannerListFragment();
    }

    public ScannerListFragment() {
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
        View rootView = inflater.inflate(R.layout.fragment_scanner_list, container, false);

        mFragmentListView =
                (ListView) rootView.findViewById(R.id.fragment_list);

        if (savedInstanceState != null) {
            mItems = savedInstanceState.getParcelableArrayList(ITEMS_LIST_TAG);
        } else {
            mItems = new ArrayList<>();
        }

        mDemoListAdapter = new ScannerListAdapter(getActivity(), mItems);
        mFragmentListView.setAdapter(mDemoListAdapter);


        // Set the onScrollListener to notify the demo list adapter about any scroll state
        // changes.
        mFragmentListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }

            public void onScrollStateChanged(AbsListView view, int scrollState) {

                final ListView lw = mFragmentListView;

                if (scrollState == 0)
                    Log.i(TAG, "scrolling stopped.");

                if (view.getId() == lw.getId()) {
                    final int currentFirstVisibleItem = lw.getFirstVisiblePosition();

                    if (currentFirstVisibleItem > mOldFirstVisibleItem) {
                        Log.i(TAG, "Scrolling down");
                        mDemoListAdapter.setScrollingUp(false);
                        mDemoListAdapter.setScrollingDown(true);
                    } else if (currentFirstVisibleItem < mOldFirstVisibleItem) {
                        Log.i(TAG, "Scrolling up.");
                        mDemoListAdapter.setScrollingUp(true);
                        mDemoListAdapter.setScrollingDown(false);
                    }

                    mOldFirstVisibleItem = currentFirstVisibleItem;
                }
            }
        });

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        ArrayList<ScannerListItem> bundledListItems = (ArrayList) mDemoListAdapter.getList();
        savedState.putParcelableArrayList(ITEMS_LIST_TAG, bundledListItems);
    }


    public void updateListView(String text) {
        Log.i(TAG, "updateListView(String text)");
        ScannerListItem item = new ScannerListItem(text);
        mItems.add(item);
        mDemoListAdapter.notifyDataSetChanged();
    }

}
