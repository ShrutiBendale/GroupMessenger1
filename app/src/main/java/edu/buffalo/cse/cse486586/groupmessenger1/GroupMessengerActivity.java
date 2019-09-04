package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;



/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {


    static final int SERVER_PORT = 10000;
    static int keynumber = 0;
    private static final String TAG = "GroupMessaging";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.i(TAG, "check 1");


        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));


        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        try {
            ServerSocket serverSocket = new ServerSocket(10000);
            Log.i(TAG,"check 2");
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                tv.append("\t" + msg+"\n");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });


    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }
        Uri Uri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");



        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            try {
                while(true) {
                    Socket socket = serverSocket.accept();
                    DataInputStream message = new DataInputStream(socket.getInputStream());
                    Log.i(TAG, "Server check");
                    String msg = message.readUTF(); //https://stackoverflow.com/questions/19564643/datainputstream-and-outputstream-write-read-string-with-length?rq=1
                    publishProgress(msg);
                    socket.close();
                    }
                }
            catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String... strings) {

            String strReceived = strings[0].trim();
            TextView TextView = (TextView) findViewById(R.id.textView1);
            TextView.append(strReceived + "\t\n");

            //Passing the key-value pair to the insert() function using the content provider
            //https://developer.android.com/reference/android/content/ContentValues
            ContentValues keyValuePair = new ContentValues();
            keyValuePair.put("key", Integer.toString(keynumber));
            keynumber++;
            keyValuePair.put("value",strReceived);
            getContentResolver().insert(Uri,keyValuePair);

            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
            for (int i = 11108; i <= 11124; i+=4)
            {
                int remotePort = i;
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),remotePort);
                String msgToSend = msgs[0];
                DataOutputStream clientmessage = new DataOutputStream(socket.getOutputStream());
                clientmessage.writeUTF(msgToSend); //Writes to string
                Log.i(TAG, " client socket check port "+i+" "+msgToSend);
                clientmessage.flush();
                socket.close();
            }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


}
