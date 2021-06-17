package com.nanami.networkwebcamera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

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