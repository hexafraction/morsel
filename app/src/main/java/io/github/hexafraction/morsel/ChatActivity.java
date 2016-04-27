package io.github.hexafraction.morsel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Severity;
import io.github.hexafraction.morsel.net.NetworkOperationsSingleton;

import java.io.IOException;
import java.io.InterruptedIOException;

public class ChatActivity extends Activity {

    public static final String CHAN = "io.github.hexafraction.morsel.intent.SV_CHAN";
    String channel;
    volatile TextView chat;
    Thread keepalive = new Thread(new Runnable() {
        @Override
        public void run() {
            while(run){
                try {
                    NetworkOperationsSingleton.getInstance().getSockOutput().writeChar('p');
                    Thread.sleep(1000);
                } catch (IOException e) {
                    Bugsnag.notify(e, Severity.ERROR);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ChatActivity.this, "Error keeping the connection open.", Toast.LENGTH_SHORT).show();


                            finish();
                        }
                    });
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    });
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        channel = getIntent().getStringExtra(CHAN);
        try {
            NetworkOperationsSingleton.getInstance().getSockOutput().writeChar('j');

            NetworkOperationsSingleton.getInstance().getSockOutput().writeUTF(channel);

            NetworkOperationsSingleton.getInstance().getSockOutput().flush();
        } catch (IOException e) {
            Toast.makeText(ChatActivity.this, "Failed to join channel.", Toast.LENGTH_SHORT).show();

            Bugsnag.notify(e, Severity.ERROR);
            finish();
        }
        chat = (TextView) findViewById(R.id.chatView);
        chat.setMovementMethod(new ScrollingMovementMethod());
        chat.setText("");

        findViewById(R.id.btnDash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    NetworkOperationsSingleton.getInstance().getSockOutput().writeChar('-');
                    NetworkOperationsSingleton.getInstance().getSockOutput().flush();
                } catch (IOException e) {
                    Toast.makeText(ChatActivity.this, "Something bad happened.", Toast.LENGTH_SHORT).show();
                    Log.wtf("EXCEPTION", e);

                    Bugsnag.notify(e, Severity.ERROR);
                    ChatActivity.this.finish();
                }
            }
        });
        findViewById(R.id.btnDot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    NetworkOperationsSingleton.getInstance().getSockOutput().writeChar('.');
                    NetworkOperationsSingleton.getInstance().getSockOutput().flush();
                } catch (IOException e) {
                    Toast.makeText(ChatActivity.this, "Something bad happened.", Toast.LENGTH_SHORT).show();
                    Log.wtf("EXCEPTION", e);

                    Bugsnag.notify(e, Severity.ERROR);
                    ChatActivity.this.finish();
                }
            }
        });
        findViewById(R.id.btnNewline).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    NetworkOperationsSingleton.getInstance().getSockOutput().writeChar('n');
                    NetworkOperationsSingleton.getInstance().getSockOutput().flush();
                } catch (IOException e) {
                    Toast.makeText(ChatActivity.this, "Something bad happened.", Toast.LENGTH_SHORT).show();
                    Log.wtf("EXCEPTION", e);

                    Bugsnag.notify(e, Severity.ERROR);
                    ChatActivity.this.finish();
                }
            }
        });
        findViewById(R.id.btnSpace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    NetworkOperationsSingleton.getInstance().getSockOutput().writeChar(' ');
                    NetworkOperationsSingleton.getInstance().getSockOutput().flush();
                } catch (IOException e) {
                    Toast.makeText(ChatActivity.this, "Something bad happened.", Toast.LENGTH_SHORT).show();
                    Log.wtf("EXCEPTION", e);

                    Bugsnag.notify(e, Severity.ERROR);
                    ChatActivity.this.finish();
                }
            }
        });
        findViewById(R.id.btnDel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    NetworkOperationsSingleton.getInstance().getSockOutput().writeChar('d');
                    NetworkOperationsSingleton.getInstance().getSockOutput().flush();
                } catch (IOException e) {
                    Toast.makeText(ChatActivity.this, "Something bad happened.", Toast.LENGTH_SHORT).show();
                    Log.wtf("EXCEPTION", e);

                    Bugsnag.notify(e, Severity.ERROR);
                    ChatActivity.this.finish();
                }
            }
        });
        findViewById(R.id.btnSlash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    NetworkOperationsSingleton.getInstance().getSockOutput().writeChar('/');
                    NetworkOperationsSingleton.getInstance().getSockOutput().flush();
                } catch (IOException e) {
                    Toast.makeText(ChatActivity.this, "Something bad happened.", Toast.LENGTH_SHORT).show();
                    Log.wtf("EXCEPTION", e);

                    Bugsnag.notify(e, Severity.ERROR);
                    ChatActivity.this.finish();
                }
            }
        });
        recvThread = new Thread(new RecvThread());
        recvThread.start();
        keepalive.start();
    }

    volatile boolean run = true;
    Thread recvThread;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        run = false;
        recvThread.interrupt();
        keepalive.interrupt();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_clear_room) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirmation")
                    .setMessage("Are you sure you would like to clear all text?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            try {
                                NetworkOperationsSingleton.getInstance().getSockOutput().writeChar('c');
                                NetworkOperationsSingleton.getInstance().getSockOutput().flush();
                            } catch (IOException e) {
                                Toast.makeText(ChatActivity.this, "Something bad happened.", Toast.LENGTH_SHORT).show();
                                Log.wtf("EXCEPTION", e);

                                Bugsnag.notify(e, Severity.ERROR);
                                ChatActivity.this.finish();
                            }
                        }})
                    .setNegativeButton(android.R.string.no, null).show();
            return true;
        } else if (id == R.id.action_del_room) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirmation")
                    .setMessage("Are you sure you would like to delete this room?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            try {
                                NetworkOperationsSingleton.getInstance().getSockOutput().writeChar('x');
                                NetworkOperationsSingleton.getInstance().getSockOutput().flush();
                            } catch (IOException e) {
                                Toast.makeText(ChatActivity.this, "Something bad happened.", Toast.LENGTH_SHORT).show();
                                Log.wtf("EXCEPTION", e);

                                Bugsnag.notify(e, Severity.ERROR);
                                ChatActivity.this.finish();
                            }
                        }})
                    .setNegativeButton(android.R.string.no, null).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class RecvThread implements Runnable {

        @Override
        public void run() {
            while (run) {
                try {
                    final char val = NetworkOperationsSingleton.getInstance().getSockInput().readChar();
                    if(chat==null){
                        Log.wtf("WTF", "Skipping character"+val);
                        continue;
                    }
                    ChatActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            switch (val) {
                                case '.':
                                    chat.append("•");
                                    break;
                                case '-':
                                    //chat.append("—");
                                    chat.append("-");
                                    break;
                                case 'n':
                                    chat.append("\n");
                                    break;
                                case '/':
                                    chat.append("/");
                                    break;
                                case ' ':
                                    chat.append(" ");
                                    break;
                                case 'd':
                                    chat.setText(chat.getText().subSequence(0, chat.getText().length()-1));
                                    break;
                                case 'c':
                                    chat.setText("");
                                    break;
                                case 'x':
                                    Toast.makeText(ChatActivity.this, "Room deleted.", Toast.LENGTH_SHORT).show();
                                    finish();
                                case 'p':
                                    // keepalive only
                                    break;
                                default:

                                    Toast.makeText(ChatActivity.this, "Invalid character from server.", Toast.LENGTH_SHORT).show();
                                    Log.wtf("BAD CHAR", String.valueOf(val));
                                    Bugsnag.notify(new IllegalArgumentException("Invalid character: "+val), Severity.WARNING);

                            }
                            Layout l = chat.getLayout();
                            if(l!=null) {
                                final int scrollAmount = chat.getLineHeight()*(chat.getLineCount()+1) - chat.getHeight();
                                if (scrollAmount > 0)
                                    chat.scrollTo(0, scrollAmount);
                                else
                                    chat.scrollTo(0, 0);
                            }

                        }
                    });
                } catch (final InterruptedIOException e) {
                    return;
                } catch (final IOException e) {
                    ChatActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ChatActivity.this, "Something bad happened.", Toast.LENGTH_SHORT).show();
                            Log.wtf("EXCEPTION", e);
                            Bugsnag.notify(e, Severity.ERROR);
                            ChatActivity.this.finish();
                        }
                    });
                    return;
                }
            }
        }
    }
}
