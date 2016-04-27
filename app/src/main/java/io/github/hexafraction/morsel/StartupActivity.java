package io.github.hexafraction.morsel;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Severity;
import io.github.hexafraction.morsel.net.NetworkOperationsSingleton;

import java.io.IOException;

public class StartupActivity extends Activity {

    public static final String PREFS_ID_CONN = "MorselConn";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bugsnag.init(this);
        //Log.wtf("CR", "OnCreate");
        try {
            NetworkOperationsSingleton.getInstance().disconnect();
        } catch (IOException e) {
            Bugsnag.notify(e, Severity.WARNING);
            Log.wtf("WTF", "IOException closing.");
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        final SharedPreferences connSettings = getSharedPreferences(PREFS_ID_CONN, 0);
        ((EditText) findViewById(R.id.svName)).setText(connSettings.getString("lastServer", ""));
        ((Button) findViewById(R.id.connBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String svAddr = ((EditText) findViewById(R.id.svName)).getText().toString();
                if(svAddr==null || svAddr.isEmpty()){
                    Toast.makeText(StartupActivity.this.getBaseContext(),"Server name cannot be blank",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                connSettings.edit().putString("lastServer", svAddr).apply();
                Intent intent = new Intent(StartupActivity.this, RoomSelActivity.class);
                intent.putExtra(RoomSelActivity.SV_HOST, svAddr);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_startup, menu);
        return true;
    }




}
