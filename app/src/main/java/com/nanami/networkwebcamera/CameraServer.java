package com.nanami.networkwebcamera;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;


public class CameraServer {
    private final static String TAG = "CameraServer";
    private String hostAddress;
    private int port;
    private Socket mSocket;
    private boolean connectionEstablished = false;
    private byte[] imageByteArray;
    private Thread sendImageThread;
    private CameraImage mCameraImage;
    private boolean sendingStatus = false;

    // Constructor
    public CameraServer(String address, int port, CameraImage image){
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
             if (!establishConnection()) return;
        }
        BufferedOutputStream bos = getBufferdOS(mSocket);
        // TODO: Image sending Loop
        sendImageThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(sendingStatus) {
                    imageByteArray = mCameraImage.getByteArray();
                    sendByte(bos, imageByteArray);
                    sleep(10);
                }
                closeBufferedOS(bos);
            }
        });

        sendImageThread.run();
    }

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

    private BufferedOutputStream getBufferdOS(Socket soc) {
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
        try {
            bos.write(bytes, 0, bytes.length);
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
