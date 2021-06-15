package com.nanami.networkwebcamera;

import android.util.Log;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;


public class CameraClient {
    private final static String TAG = "CameraServer";
    private String hostAddress;
    private int port;
    private Socket mSocket;
    private boolean connectionEstablished = false;
    private byte[] imageByteArray;
    private Thread sendImageThread;
    private CameraImage mCameraImage;
    private boolean sendingStatus = false;
    private final static String CRLF = "\r\n";

    // Constructor
    public CameraClient(String address, int port, CameraImage image){
        hostAddress = address;
        this.port = port;
        mCameraImage = image;
    }

    // establish connection to host computer
    // if connection already established, this returns "true"
    private boolean establishConnection() {
        if (connectionEstablished) return true;
        try {
            mSocket = new Socket(hostAddress, port);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            return false;
        }
        return true;
    }

    public void start() {
        sendingStatus = true;
        startLoop();
    }

    public void stop() {
        sendingStatus = false;
        stopLoop();
    }

    private void startLoop() {
        if (!connectionEstablished) {
             if (!establishConnection()) {
                 Log.d(TAG, "Connection cannot be established.");
                 return;
             }
        }
        BufferedOutputStream bos = getBufferedOS(mSocket);
        // TODO: Image sending Loop
        sendImageThread = new Thread(() -> {
            while(sendingStatus) {
                imageByteArray = mCameraImage.getByteArray();
                if(imageByteArray == null) continue;
                sendByte(bos, imageByteArray);
                sleep(100/6);
            }
            closeBufferedOS(bos);
        });

        sendImageThread.start();
    }

    // Methods for catching some exceptions

    private void stopLoop() {
        try {
            if(sendImageThread != null) {
                sendImageThread.join();
                sendImageThread = null;
            }
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        }
    }

    private BufferedOutputStream getBufferedOS(Socket soc) {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(soc.getOutputStream());
        }catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return bos;
    }

    private void closeBufferedOS(BufferedOutputStream bos) {
        try {
            bos.close();
        }catch (IOException e){
            Log.e(TAG, e.toString());
        }
    }

    private void sendByte(BufferedOutputStream bos, byte[] bytes){
        byte[] dataSize = ByteBuffer.allocate(4).putInt(bytes.length).array();
        try {
            // First 5 bytes -> 0xff + <data size (4 bytes)>
            bos.write(0xff);
            bos.write(dataSize, 0, 4);
            bos.write(bytes, 0, bytes.length);
            bos.flush();
            Log.d(TAG, "Send " + bytes.length + " bytes");
        }catch (IOException e){
            Log.e(TAG, e.toString());
        }
    }

    private void sleep(int ms){
        try {
            Thread.sleep(ms);
        }catch (InterruptedException e){
            Log.e(TAG, e.toString());
        }
    }
}
