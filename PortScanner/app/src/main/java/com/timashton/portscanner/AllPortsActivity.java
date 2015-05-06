package com.timashton.portscanner;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


public class AllPortsActivity extends Activity implements Observer {

    private static final int PORTS_PER_THREAD = 655;


    private List<String> openPortList;
    private MyListAdapter adapter;

    private Button scanButton;
    private EditText mEdit;
    private ListView listView;

    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(this.getClass().getName(), "Enter onCreate");

        mContext = this;

        //init views and buttons
//        scanButton = (Button) findViewById(R.id.button_send);
//        mEdit = (EditText) findViewById(R.id.ip_address);
//        listView = (ListView) findViewById(R.id.test_list);


        openPortList = new ArrayList<String>();
        //adapter = new ArrayAdapter<String>(mContext, R.layout.list_item);
        adapter = new MyListAdapter(this, openPortList);
        listView.setAdapter(adapter);


        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //retrieve the IP from edittext
                String host = mEdit.getText().toString();


                int numOfThreads = 100;
                Thread[] threads = new Thread[numOfThreads];

                for (int i = 0; i < threads.length; i++) {
                    //Create a new scanner here then observe it
                    Scanner s = new Scanner(host, (i * PORTS_PER_THREAD), PORTS_PER_THREAD);

                    s.addObserver((Observer) mContext);

                    threads[i] = new Thread(s);
                    threads[i].start();

                }

            }
        });
    }

    @Override
    public void update(Observable observable, Object data) {
        Log.e(this.getClass().getName(), "Enter: update()");

        final Observable o = observable;

        this.runOnUiThread(new Runnable() {
            public void run() {
                //do your modifications here

                // for example
                openPortList.add(((Scanner) o).getNextPort());
                adapter.notifyDataSetChanged();
            }
        });
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

}
