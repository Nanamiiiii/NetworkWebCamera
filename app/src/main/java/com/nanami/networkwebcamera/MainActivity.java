package com.nanami.networkwebcamera;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    EditText ipFirst3;
    EditText ipSecond3;
    EditText ipThird3;
    EditText ipLast3;
    EditText portNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ipFirst3 = findViewById(R.id.ip_addr_1);
        ipSecond3 = findViewById(R.id.ip_addr2);
        ipThird3 = findViewById(R.id.ip_addr3);
        ipLast3 = findViewById(R.id.ip_addr4);
        portNo = findViewById(R.id.portNo);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check Network Connection
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean wifi_connected = false;
        for(Network network : connManager.getAllNetworks()){
            NetworkInfo networkInfo = connManager.getNetworkInfo(network);
            if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                wifi_connected |= networkInfo.isConnected();
                Log.d(TAG, "Connected to WiFi.");
            }
        }
        if(!wifi_connected) {
            Log.d(TAG, "WiFi connection not found.");
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("This app can be run under the Wi-Fi local network.")
                    .setPositiveButton("EXIT", (dialog, which) -> {
                        finish();
                    }).show();
        }
    }

    public void callCameraActivity(View view){
        String hostIpAddr = ipFirst3.getText().toString() + "." + ipSecond3.getText().toString() + "." + ipThird3.getText().toString() + "." + ipLast3.getText().toString();
        int hostPort = Integer.parseInt(portNo.getText().toString());

        // To call CameraActivity
        Intent intent = new Intent(this, CameraActivity.class);
        // Give some parameter
        intent.putExtra("HOST_IP", hostIpAddr);
        intent.putExtra("HOST_PORT", hostPort);
        // Call
        startActivity(intent);
    }
}