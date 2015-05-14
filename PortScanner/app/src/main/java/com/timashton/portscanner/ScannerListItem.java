/*
 * Created by Tim Ashton on 14/05/15.
 */

package com.timashton.portscanner;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;


public class ScannerListItem implements Parcelable {

    private static final String TAG = ScannerListItem.class.getName();
    private static final String NEW_ITEM_TAG = "new_item";
    private static final String TEXT_TAG = "item_text";

    private boolean mNewItem;
    private String mText;

    public ScannerListItem(String text){
        mNewItem = true;
        mText = text;
    }

    public ScannerListItem(boolean newItem, String text){
        mNewItem = newItem;
        mText = text;
    }

    private ScannerListItem(Parcel in) {
        Bundle bundle = in.readBundle();
        mNewItem = bundle.getBoolean(NEW_ITEM_TAG);
        mText = bundle.getString(TEXT_TAG);
    }

    public boolean isNewItem(){
        return mNewItem;
    }

    public String getItemText(){
        return mText;
    }

    public void setIsNewItem(boolean newItem){
        mNewItem = newItem;
    }

    public void setItemText(String text){
        mText = text;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(NEW_ITEM_TAG, mNewItem);
        bundle.putString(TEXT_TAG, mText);
        dest.writeBundle(bundle);
    }

    public static final Parcelable.Creator<ScannerListItem> CREATOR
            = new Parcelable.Creator<ScannerListItem>() {
        public ScannerListItem createFromParcel(Parcel in) {
            return new ScannerListItem(in);
        }

        public ScannerListItem[] newArray(int size) {
            return new ScannerListItem[size];
        }
    };
}