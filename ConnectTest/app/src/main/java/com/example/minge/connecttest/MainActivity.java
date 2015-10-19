package com.example.minge.connecttest;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends ActionBarActivity {
    private EditText et_ip, et_port, et_message;
    private Button btn_connect, btn_send, btn_disconnect;
    private TextView tv_state;
    private ListView lv_receive;
    private ArrayAdapter<String> listViewAdapter;
    private ArrayList<String> listViewItems;

    private Socket client;
    private DataInputStream input;
    private PrintWriter output;

    private void initial() {
        et_ip = (EditText) findViewById(R.id.et_ip);
        et_port = (EditText) findViewById(R.id.et_port);
        et_message = (EditText) findViewById(R.id.et_message);
        btn_connect = (Button) findViewById(R.id.btn_connect);
        btn_send = (Button) findViewById(R.id.btn_send);
        btn_disconnect = (Button) findViewById(R.id.btn_disconnect);
        tv_state = (TextView) findViewById(R.id.tv_state);
        lv_receive = (ListView) findViewById(R.id.lv_receive);
        listViewItems = new ArrayList<String>();
        //listViewItems.add("123");
        listViewAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, listViewItems);
        lv_receive.setAdapter(listViewAdapter);

        et_message.setEnabled(false);
        btn_disconnect.setEnabled(false);
        btn_send.setEnabled(false);
        /*
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork()
                .penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects().detectLeakedClosableObjects()
                .penaltyLog().penaltyDeath().build());*/
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initial();

        btn_connect.setOnClickListener(new Btn_Connect());
        btn_send.setOnClickListener(new Btn_Send());
        btn_disconnect.setOnClickListener(new Btn_Disconnect());
    }

    private class Btn_Connect implements Button.OnClickListener {
        @Override
        public void onClick(View v) {
            final String ip = et_ip.getText().toString();
            final int port = Integer.parseInt(et_port.getText().toString());
            tv_state.setText("Waitting to connect......");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        client = new Socket(ip, port);

                        if (client.isConnected()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    uiConnectedSetting();
                                    try {
                                        output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), "utf-8")), true);
                                    } catch (IOException e) {
                                        Log.i("Connect", e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                            });
                            new ClientReceive().start();
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    uiDisconnectSetting("Disconnect");
                                }
                            });
                        }
                    } catch (Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                uiDisconnectSetting("Fail");
                            }
                        });
                        Log.e("Connect", e.getMessage());
                    }
                }
            }).start();
        }
    }

    private class Btn_Send implements Button.OnClickListener {
        @Override
        public void onClick(View v) {
            if (client.isConnected()) {
                String message = et_message.getText().toString();
                if (!client.isOutputShutdown() && message.length() > 0) {
                    output.print(message);
                    output.flush();

                    et_message.setText("");
                } else {
                    Toast.makeText(getApplicationContext(), "Empty Input", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private class Btn_Disconnect implements Button.OnClickListener {
        @Override
        public void onClick(View v) {
            try {
                client.close();
                uiDisconnectSetting("Disconnect");
            } catch (IOException e) {
                Log.i("Connect", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void uiConnectedSetting() {
        tv_state.setText("Connected");
        btn_connect.setEnabled(false);
        btn_send.setEnabled(true);
        btn_disconnect.setEnabled(true);
        et_message.setEnabled(true);
        et_ip.setEnabled(false);
        et_port.setEnabled(false);
        Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
    }

    private void uiDisconnectSetting(String message) {
        tv_state.setText(message);
        btn_disconnect.setEnabled(false);
        btn_connect.setEnabled(true);
        btn_send.setEnabled(false);
        et_message.setText("");
        et_message.setEnabled(false);
        et_port.setEnabled(true);
        et_ip.setEnabled(true);
        et_ip.hasFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        clearListView();
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private class ClientReceive extends Thread {
        private String message;
        private DataInputStream serverInput;

        public ClientReceive() throws IOException {
            serverInput = new DataInputStream(client.getInputStream());
        }

        @Override
        public void run() {
            while (!client.isClosed()) {
                try {
                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
                    int c;
                    byte[] buffer = new byte[1024];
                    if ((c = serverInput.read(buffer)) >= 0)
                        bao.write(buffer, 0, c);
                    message = new String(bao.toByteArray(), "utf-8");

                    if (message.length() > 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addItemInList(message);
                                Toast.makeText(getApplicationContext(), "Received!!!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.i("Chat", e.getMessage());
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.i("Chat", e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private void addItemInList(String item) {
        listViewItems.add(0, item);
        lv_receive.setAdapter(listViewAdapter);
    }

    private void clearListView() {
        listViewItems.clear();
        listViewAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            input.close();
            output.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds listViewItems to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
