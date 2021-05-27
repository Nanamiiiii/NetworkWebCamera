package com.nanami.networkwebcamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

public class CameraActivity extends AppCompatActivity{
    private TextureView textureView = null;
    private CameraDevice cameraDevice = null;
    private CameraManager cameraManager = null;
    private CameraCaptureSession captureSession = null;
    private static String TAG = "Camera2Test";
    private int REQUEST_CODE_PERMISSIONS = 10;
    private String[] REQUIRED_PERMISSIONS = { Manifest.permission.CAMERA };
    private static int IMAGE_READER_MAX_IMAGES = 4;

    private ImageReader mImageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        textureView = findViewById(R.id.textureView);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (textureView.isAvailable()) {
            try {
                openCamera();
            } catch (CameraAccessException e){
                Log.e(TAG, e.toString());
            }
        } else {
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                    try {
                        openCamera();
                    } catch (CameraAccessException e){
                        Log.e(TAG, e.toString());
                    }
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

                }
            });
        }

    }

    private void openCamera() throws CameraAccessException {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, 0x01);
            return;
        }
        cameraManager.openCamera("0", new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
                try {
                    createCameraPreviewSession();
                } catch (CameraAccessException e){
                    Log.e(TAG, e.toString());
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                if(cameraDevice == null) return;
                cameraDevice.close();
                cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                if(cameraDevice == null) return;
                cameraDevice.close();
                cameraDevice = null;
            }
        }, null);
    }

    private void createCameraPreviewSession() throws CameraAccessException{
        if (cameraDevice == null) return;
        SurfaceTexture texture = textureView.getSurfaceTexture();
        texture.setDefaultBufferSize(1920, 1080);
        Surface surface = new Surface(texture);

        CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        previewRequestBuilder.addTarget(surface);

        cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                captureSession = session;
                try {
                    captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
                } catch (CameraAccessException e){
                    Log.e(TAG, e.toString());
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }

        }, null);
    }

    private void setupImageReader(){
        mImageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, IMAGE_READER_MAX_IMAGES);
    }

    private boolean allPermissionGranted(){
        Context baseContext = getBaseContext();
        for(String it : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_DENIED) return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if(requestCode == REQUEST_CODE_PERMISSIONS) {
            if(allPermissionGranted()) {
                try {
                    openCamera();
                } catch (CameraAccessException e){
                    Log.e(TAG, e.toString());
                }
            }else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
