package io.github.hexafraction.morsel;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Severity;
import io.github.hexafraction.morsel.net.NetworkOperationsSingleton;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class RoomSelActivity extends Activity {
    public final static String SV_HOST = "io.github.hexafraction.morsel.intent.SV_HOST";
    private NetworkOperationsSingleton netops;
    private ListView chanListView;
    private String hostname;
    private String[] channels;
    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.w("W", "RoomSelActivity closing.");

        //netops.disconnect(getParent()==null?this:getParent());

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_sel);

        chanListView = (ListView) findViewById(R.id.chanList);
        netops = NetworkOperationsSingleton.getInstance();
        hostname = getIntent().getStringExtra(SV_HOST);
        findViewById(R.id.joinBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinChannel(String.valueOf(((TextView) findViewById(R.id.rmName)).getText()));
            }
        });
        netops.connect(hostname, this, new Runnable() {
            @Override
            public void run() {
                new ChanListTask().execute();
            }
        });
    }

    protected class ChanListTask extends AsyncTask<Void, Void, String[]> {

        @Override
        protected String[] doInBackground(Void... params) {
            try {
                netops.getSockOutput().writeChar('l');
                int len = netops.getSockInput().readInt();
                String[] chans = new String[len];
                for(int i = 0; i < len; i++){
                    chans[i] = netops.getSockInput().readUTF();
                }
                return chans;
            } catch (IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RoomSelActivity.this, "Error getting list of channels.", Toast.LENGTH_SHORT).show();

                        RoomSelActivity.this.finish();
                    }
                });
                Bugsnag.notify(e, Severity.ERROR);
                Log.e("E", "IOException getting channel list.");
                return null;
            }

        }

        @Override
        protected void onPostExecute(final String[] rslt) {
            if(rslt==null) {
                RoomSelActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
            } else {
                chanListView.setAdapter(new ArrayAdapter<String>(RoomSelActivity.this, android.R.layout.simple_list_item_1, rslt));
                chanListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        joinChannel(rslt[position]);
                    }
                });
            }


        }
    }

    private void joinChannel(String chan) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.CHAN, chan);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_room_sel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        return super.onOptionsItemSelected(item);
    }
}
