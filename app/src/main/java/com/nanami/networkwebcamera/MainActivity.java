package com.nanami.networkwebcamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

public class MainActivity extends AppCompatActivity{

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 1000;

    private String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    private EditText ipFirst3;
    private EditText ipSecond3;
    private EditText ipThird3;
    private EditText ipLast3;
    private EditText portNo;

    private TextView deviceIP;
    private TextView connectionType;
    private TextView wifi_ssid;

    private Button connectButton;

    private Spinner res_select;

    private int pic_width = 1280;
    private int pic_height = 720;

    // Listener for Resolution spinner
    private AdapterView.OnItemSelectedListener res_select_listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch((String) parent.getItemAtPosition(position)){
                case "1080p":
                    pic_width = 1920;
                    pic_height = 1080;
                    Log.d(TAG, "Resolution: 1080p");
                    break;
                case "720p":
                    pic_width = 1280;
                    pic_height = 720;
                    Log.d(TAG, "Resolution: 720p");
                    break;
                case "360p":
                    pic_width = 640;
                    pic_height = 360;
                    Log.d(TAG, "Resolution: 360p");
                    break;
                default :
                    pic_width = 1280;
                    pic_height = 720;
                    Log.d(TAG, "Default Resolution: 720p");
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // NOP
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Prepare GUI
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ipFirst3 = findViewById(R.id.ip_addr_1);
        ipSecond3 = findViewById(R.id.ip_addr2);
        ipThird3 = findViewById(R.id.ip_addr3);
        ipLast3 = findViewById(R.id.ip_addr4);
        portNo = findViewById(R.id.portNo);
        connectionType = findViewById(R.id.textView4);
        wifi_ssid = findViewById(R.id.textView6);
        deviceIP = findViewById(R.id.textView8);
        connectButton = findViewById(R.id.button2);
        connectButton.setOnClickListener(v -> {
            connectButton.setEnabled(false);
            new Handler().postDelayed(() -> connectButton.setEnabled(true), 1000L);
            callCameraActivity(v);
        });
        res_select = findViewById(R.id.res_select);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.res_list, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        res_select.setAdapter(adapter);
        res_select.setOnItemSelectedListener(res_select_listener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Permission Check
        checkPermission();

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
        }else{
            connectionType.setText("Wi-Fi Local Network");
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            wifi_ssid.setText(wifiInfo.getSSID());
            int unformattedIP = wifiInfo.getIpAddress();
            deviceIP.setText(String.format("%d.%d.%d.%d", (unformattedIP >> 0)&0xff, (unformattedIP >> 8)&0xff, (unformattedIP >> 16)&0xff, (unformattedIP >> 24)&0xff));
        }
    }

    // Called when connect button pressed
    public void callCameraActivity(View view){
        String hostIpAddr = ipFirst3.getText().toString() + "." + ipSecond3.getText().toString() + "." + ipThird3.getText().toString() + "." + ipLast3.getText().toString();
        int hostPort = Integer.parseInt(portNo.getText().toString());

        // To call CameraActivity
        Intent intent = new Intent(this, CameraActivity.class);
        // Give some parameter
        intent.putExtra("HOST_IP", hostIpAddr);
        intent.putExtra("HOST_PORT", hostPort);
        intent.putExtra("PIC_WIDTH", pic_width);
        intent.putExtra("PIC_HEIGHT", pic_height);
        // Call
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode != REQUEST_CODE_PERMISSIONS){
            Toast.makeText(this, "Some Permission was not granted.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkPermission() {
        ArrayList<String> neededReq = new ArrayList<>();
        for(String permissionName : REQUIRED_PERMISSIONS){
            int isGranted = ContextCompat.checkSelfPermission(this, permissionName);
            if(isGranted == PERMISSION_DENIED){
                neededReq.add(permissionName);
            }
        }
        if(!neededReq.isEmpty()){
            String[] forConv = new String[neededReq.size()];
            ActivityCompat.requestPermissions(this, neededReq.toArray(forConv), REQUEST_CODE_PERMISSIONS);
        }
    }
}