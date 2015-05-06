/*
 * Created by Tim Ashton on 14/01/15.
 */

package com.timashton.portscanner;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;


public class MainActivity extends ActionBarActivity
        implements MainInputFragment.OnFragmentInteractionListener {


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_activity);

        FragmentManager fm = getFragmentManager();

        if (savedInstanceState == null) {
            fm.beginTransaction()
                    .add(R.id.container, MainInputFragment.newInstance())
                    .commit();
        }

    }


    @Override
    public void onFragmentInteraction() {
        //TODO
    }
}
