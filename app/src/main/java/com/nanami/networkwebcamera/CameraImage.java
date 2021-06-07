package com.nanami.networkwebcamera;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CameraImage {
    private byte[] imageByteArray;
    private final Lock lock = new ReentrantLock();

    public CameraImage() {

    }

    public void setByteArray(byte[] image) {
        lock.lock();
        try {
            imageByteArray = image;
        } finally {
            lock.unlock();
        }
    }

    public byte[] getByteArray() {
        lock.lock();
        try {
            return imageByteArray;
        } finally {
            lock.unlock();
        }
    }

}
