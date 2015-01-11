package com.timashton.portscanner;

import android.view.View;
import android.widget.TextView;

/**
 * Created by tim on 11/01/15.
 */
public class MyListViewHolder {
    public TextView mTVItem;
    public MyListViewHolder(View base) {
        mTVItem = (TextView) base.findViewById(R.id.list_TV);
    }
}
