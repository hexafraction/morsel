package io.github.hexafraction.morsel.net;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Severity;

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NetworkOperationsSingleton {
    private static NetworkOperationsSingleton ourInstance = new NetworkOperationsSingleton();

    public static NetworkOperationsSingleton getInstance() {
        return ourInstance;
    }

    private Socket socket;
    private DataInputStream sockInput;
    private DataOutputStream sockOutput;
    private Thread connControlThread;
    private volatile boolean xmitFunctional = false;
    private volatile boolean recvFunctional = false;
    private LinkedBlockingQueue<ConnectionRequest> reqs = new LinkedBlockingQueue<ConnectionRequest>();
    private volatile ByteQueue xmit;
    private volatile ByteQueue recv;
    public DataInputStream getSockInput() {
        return sockInput;
    }

    public DataOutputStream getSockOutput() {
        return sockOutput;
    }

    private NetworkOperationsSingleton() {
    }
    private Thread rpt, tpt;

    public void disconnect() throws IOException {
        if(tpt!=null) tpt.interrupt();
        if(rpt!=null) rpt.interrupt();
        if(xmit!=null) xmit.interruptReader();
        if(recv!=null) recv.interruptReader();
        if(socket!=null) socket.close();
    }

    private class ReceivePipeThread implements Runnable {
        InputStream recv;

        public ReceivePipeThread(InputStream recv, ByteQueue queue) {
            this.recv = recv;
            this.queue = queue;
        }

        ByteQueue queue;
        @Override
        public void run() {
            while(true){
                try {
                    queue.getOutputStream().write(recv.read());
                } catch (InterruptedIOException e){
                    Log.w("INTERRUPT", e);
                    return;
                } catch (IOException e) {
                    Log.wtf("EXCEPTION", e);
                    Bugsnag.notify(e, Severity.ERROR);
                    return;
                }
            }
        }
    }

    private class TransmitPipeThread implements Runnable {
        OutputStream xmit;
        ByteQueue queue;

        public TransmitPipeThread(OutputStream xmit, ByteQueue queue) {
            this.xmit = xmit;
            this.queue = queue;
        }

        @Override
        public void run() {
            while(true) {
                try {
                    xmit.write(queue.getInputStream().read());
                } catch (InterruptedIOException e) {

                    return;
                } catch (IOException e) {
                    Log.wtf("EXCEPTION", e);
                    Bugsnag.notify(e, Severity.ERROR);
                    return;
                }
            }
        }
    }

    protected void connect(final ConnectionRequest cr) {

        synchronized (this) {
            xmitFunctional = false;
            recvFunctional = false;
            try {
                //if (sockOutput != null) sockOutput.writeChar('b'); // bye
                if (socket != null) {
                    socket.shutdownInput();
                    socket.shutdownOutput();
                    socket.close();

                }
            } catch (IOException e) {
                Log.wtf("NET", "IOException closing connection.");
                Bugsnag.notify(e, Severity.ERROR);
            }
            cr.cx.runOnUiThread(new Runnable() {
                public void run() {
                    cr.pd.setMessage("Cleaning up");
                }
            });
            if (cr.getHost() == null) {
                cr.cx.runOnUiThread(new Runnable() {
                    public void run() {
                        cr.pd.dismiss();
                        cr.cx.finish();
                    }
                });
                return;
            }
            cr.cx.runOnUiThread(new Runnable() {
                public void run() {
                    cr.pd.setMessage("Looking up host");
                }
            });
            InetAddress addr = null;
            try {
                addr = InetAddress.getByName(cr.getHost());
            } catch (UnknownHostException e) {

                cr.cx.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(cr.cx, "Unknown or misspelled hostname.", Toast.LENGTH_SHORT).show();
                        cr.pd.dismiss();
                        cr.cx.finish();
                    }
                });
                return;
            }
            cr.cx.runOnUiThread(new Runnable() {
                public void run() {
                    cr.pd.setMessage("Connecting to port");
                }
            });
            try {
                NetworkOperationsSingleton.this.socket = new Socket();
                socket.connect(new InetSocketAddress(addr, 18718), 5000);
            } catch (IOException e) {

                cr.cx.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(cr.cx, "Failed to connect.", Toast.LENGTH_SHORT).show();
                        cr.pd.dismiss();
                        cr.cx.finish();
                    }
                });
                return;
            }

            DataInputStream dis;
            DataOutputStream dos;
            try {
                recv = new ByteQueue(1024);
                xmit = new ByteQueue(1024);
                if(rpt!=null) rpt.interrupt();
                if(tpt!=null) tpt.interrupt();

                rpt = new Thread(new ReceivePipeThread(socket.getInputStream(), recv), "recv");
                tpt = new Thread(new TransmitPipeThread(socket.getOutputStream(), xmit), "xmit");
                rpt.start();
                tpt.start();
                dis = new DataInputStream(recv.getInputStream());
                dos = new DataOutputStream(xmit.getOutputStream());
                cr.cx.runOnUiThread(new Runnable() {
                    public void run() {
                        cr.pd.setMessage("Handshaking with server");
                    }
                });
                final int magic = dis.readInt();
                if (magic != 0x64607012) {

                    cr.cx.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(cr.cx, "Error while handshaking. Got magic: "+Integer.toHexString(magic), Toast.LENGTH_SHORT).show();
                            cr.pd.dismiss();
                            cr.cx.finish();
                        }
                    });
                    Bugsnag.notify(new IllegalStateException("Incorrect magic value: 0x"+Integer.toHexString(magic)), Severity.ERROR);
                    return;
                }
                dos.writeInt(0x77345466);

            } catch (IOException e) {
                Bugsnag.notify(e, Severity.WARNING);
                cr.cx.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(cr.cx, "Failed to open bidirectional streams.", Toast.LENGTH_SHORT).show();
                        cr.pd.dismiss();
                        cr.cx.finish();
                    }
                });
                return;

            }
            NetworkOperationsSingleton.this.sockInput = dis;
            NetworkOperationsSingleton.this.sockOutput = dos;
            xmitFunctional = true;
            recvFunctional = true;
            cr.cx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cr.pd.dismiss();
                }
            });
            cr.cx.runOnUiThread(cr.callback);
        }
    }


    public void connect(String host, final Activity cx, Runnable connCallback) {

        final ConnectionRequest cr = new ConnectionRequest(host, cx, ProgressDialog.show(cx, "Connecting...", "Notifying network operations layer", true, true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Toast.makeText(cx, "Canceled connection attempt.", Toast.LENGTH_SHORT).show();
                try {
                    if(recv!=null) recv.interruptReader();
                    socket.close();

                } catch (IOException e) {

                }
                cx.finish();
            }
        }), connCallback);
        AsyncTask.<Void, Void, Void>execute(new Runnable() {
            @Override
            public void run() {
                connect(cr);
            }
        });
    }

    public void disconnect(final Activity cx) {
        ConnectionRequest cr = new ConnectionRequest(null, cx, ProgressDialog.show(cx, "Disconnecting...", "Notifying network operations layer", true, false), null);
        try {
            reqs.put(cr);
        } catch (InterruptedException e) {
            Toast.makeText(cx, "Something went horribly wrong (InterruptedException)", Toast.LENGTH_SHORT).show();
            Bugsnag.notify(e, Severity.ERROR);
        }
    }

    private class ConnectionRequest {
        final String host;
        final Activity cx;
        final ProgressDialog pd;
        final Runnable callback;

        public String getHost() {
            return host;
        }

        public Activity getCx() {
            return cx;
        }

        public ProgressDialog getPd() {
            return pd;
        }

        public Runnable getCallback() {
            return callback;
        }

        public ConnectionRequest(String host, Activity cx, ProgressDialog pd, Runnable callback) {
            this.pd = pd;
            this.host = host;
            this.cx = cx;
            this.callback = callback;
        }
    }
}

