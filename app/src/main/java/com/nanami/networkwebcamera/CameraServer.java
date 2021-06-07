package com.nanami.networkwebcamera;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class CameraServer {
    private final static String TAG = "CameraServer";
    private String hostAddress;
    private int port;
    private Socket mSocket;
    private boolean connectionEstablished = false;
    private byte[] imageByteArray;
    private Thread sendImageThread;

    // Constructor
    public CameraServer(String address, int port){
        hostAddress = address;
        this.port = port;
    }

    // establish connection to host computer
    // if connection already established, this returns "true"
    private boolean establishConnection() {
        if (connectionEstablished) return true;
        try {
            mSocket = new Socket(hostAddress, port);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public void startLoop() {
       if (!connectionEstablished) {
            if (!establishConnection()) return;
       }
       // TODO: Image sending Loop
    }

}
