package com.nanami.networkwebcamera;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CameraImage {
    private byte[] imageByteArray;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock writeLock = rwl.writeLock();
    private final Lock readLock = rwl.readLock();

    // Constructor
    public CameraImage() {
        // NOP
    }

    public synchronized void setByteArray(byte[] image) {
        writeLock.lock();
        try {
            imageByteArray = image;
        } finally {
            writeLock.unlock();
        }
    }

    public synchronized byte[] getByteArray() {
        readLock.lock();
        try {
            return imageByteArray;
        } finally {
            readLock.unlock();
        }
    }

}
