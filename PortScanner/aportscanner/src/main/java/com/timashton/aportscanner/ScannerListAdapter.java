/*
 * Created by Tim Ashton on 14/05/15.
 */

package com.timashton.aportscanner;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;


public class ScannerListAdapter  extends BaseAdapter {

    private final String TAG = ScannerListAdapter.class.getName();

    private Activity mContext;
    private ArrayList<ScannerListItem> mList;
    private boolean mIsScrollingUp = false;
    private boolean mIsScrollingDown = false;


    public ScannerListAdapter(Activity context, ArrayList<ScannerListItem> list) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "ScannerListAdapter()");
        }
        mContext = context;
        mList = list;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int pos) {
        return mList.get(pos);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ScannerListItem item = mList.get(position);
        ViewHolder holder;

        // If it is a new item, create it otherwise use the existing item previously created in
        // this method.
        if (convertView == null) {

            LayoutInflater li = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.list_item, null);

            holder = new ViewHolder();
            holder.mListItemText = (TextView) convertView.findViewById(R.id.list_item_title);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.mListItemText.setText(item.getItemText());

        // Get ready to animate the item(s)
        Animation animation;

        // This item is new or being seen for the first time.
        if (item.isNewItem() && !mIsScrollingUp && !mIsScrollingDown) {
            animation = AnimationUtils.loadAnimation(mContext, R.anim.flip_down);
            item.setIsNewItem(false);

            convertView.startAnimation(animation);
        }

        // This item is not new. Apply the scrolling animation. (just using flip_down for demo)
        if (mIsScrollingUp || mIsScrollingDown) {
            animation = AnimationUtils.loadAnimation(mContext, R.anim.flip_down);

            convertView.startAnimation(animation);
        }
        return convertView;
    }

    private static class ViewHolder {
        public TextView mListItemText;

    }

    /* public void setScrollingUp(boolean scrollingUp)
     *
     * Allows the caller to tell this list adapter that the list is being scrolled up.
     */
    public void setScrollingUp(boolean scrollingUp){
        mIsScrollingUp = scrollingUp;
    }

    /*
     * public void setScrollingDown(boolean scrollingDown)
     *
     * Allows the caller to tell this list adapter that the list is being scrolled down.
     */
    public void setScrollingDown(boolean scrollingDown){
        mIsScrollingDown = scrollingDown;
    }

    public ArrayList<ScannerListItem> getList(){
        return mList;
    }

}
