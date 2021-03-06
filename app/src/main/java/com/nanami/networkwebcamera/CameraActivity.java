package com.nanami.networkwebcamera;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraActivity extends Activity {
    private TextureView textureView = null;
    private CameraDevice cameraDevice = null;
    private CameraManager cameraManager = null;
    private CameraCaptureSession captureSession = null;
    private static String TAG = "CameraActivity";
    private int REQUEST_CODE_PERMISSIONS = 10;
    private String[] REQUIRED_PERMISSIONS = { Manifest.permission.CAMERA , Manifest.permission.INTERNET };
    private static int IMAGE_READER_MAX_IMAGES = 48;

    private ImageReader mImageReader;
    private Handler mBackgroundHandler = new Handler();

    private String hostIpAddr;
    private int hostPort;

    private CameraClient mClient;
    private CameraImage mCameraImage;

    private LinearProgressIndicator connectionProg;
    private Button exitButton;

    private final Handler handler = new Handler();

    // Image Size
    public int FRAME_WIDTH = 1280;
    public int FRAME_HEIGHT = 720;
    public final static int FRAME_ROTATION = 0;

    // Callback for ImagePreview
    public interface PreviewCallback {
        void onPreview(byte[] bytes);
    }

    private final PreviewCallback mPreviewCallback = new PreviewCallback() {
        @Override
        public void onPreview(byte[] bytes) {
            mCameraImage.setByteArray(bytes);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prohibit turning Off the screen automatically
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // generate view
        setContentView(R.layout.activity_camera);
        connectionProg = findViewById(R.id.connectionProg);
        mCameraImage = new CameraImage();

        // Set Button Listener
        exitButton = findViewById(R.id.closeButton);
        exitButton.setOnClickListener(v -> {
            exitButton.setEnabled(false);
            new Handler().postDelayed(() -> {
                exitButton.setEnabled(true);
            }, 1000L);
            onClosePressed(v);
        });

        // Get Connection Info from MainActivity
        Intent intent = getIntent();
        hostIpAddr = intent.getStringExtra("HOST_IP");
        hostPort = intent.getIntExtra("HOST_PORT", 8080);
        FRAME_WIDTH = intent.getIntExtra("PIC_WIDTH", 1280);
        FRAME_HEIGHT = intent.getIntExtra("PIC_HEIGHT", 720);

        // Create Client Instance
        mClient = new CameraClient(hostIpAddr, hostPort, mCameraImage, this);
        textureView = findViewById(R.id.textureView);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        setupImageReader();

        // To avoid Exception
        // Network Component can't be operated from the thread operating UI component.
        Thread clientStart = new Thread(() -> {
            mClient.start();
        });
        clientStart.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Process of launching camera
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
                    // NOP
                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                    // NOP
                }
            });
        }

    }

    // Fullscreen settings
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    // Related Permission
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

    private boolean allPermissionGranted(){
        Context baseContext = getBaseContext();
        for(String it : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_DENIED) return false;
        }
        return true;
    }

    // When back button pressed
    // Disconnect before kill Activity
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("Are you sure disconnecting server?")
                .setPositiveButton("OK", (dialog, which) -> {
                    if(mClient != null) {
                        mClient.stop();
                    }
                    if(captureSession != null) {
                        captureSession.close();
                    }
                    if(cameraDevice != null) {
                        cameraDevice.close();
                    }
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void onClosePressed(View v) {
        new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("Are you sure disconnecting server?")
                .setPositiveButton("OK", (dialog, which) -> {
                    if(mClient != null) {
                        mClient.stop();
                    }
                    if(captureSession != null) {
                        captureSession.close();
                    }
                    if(cameraDevice != null) {
                        cameraDevice.close();
                    }
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void unableConnect(){
        handler.post(() -> {
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Failed to connect.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        if(captureSession != null) {
                            captureSession.close();
                        }
                        if(cameraDevice != null) {
                            cameraDevice.close();
                        }
                        finish();
                    })
                    .show();
        });
    }

    public void lostConnection(){
        handler.post(() -> {
           new AlertDialog.Builder(this)
                   .setTitle("Error")
                   .setMessage("Connection Lost")
                   .setPositiveButton("EXIT", (dialog, which) -> {
                       if(mClient != null){
                           mClient.stop();
                       }
                       if(captureSession != null){
                           captureSession.close();
                       }
                       if(cameraDevice != null){
                           cameraDevice.close();
                       }
                       finish();
                   })
                   .show();
        });
    }

    public void hideProgressBar() {
        handler.post(() -> {
            connectionProg.hide();
        });
    }

    public void makeToastThroughHandler(String str){
        handler.post(() -> {
            Context context = getApplicationContext();
            int dur = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, str, dur);
            toast.show();
        });
    }

    // Setup Camera Session
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
        }, mBackgroundHandler);
    }

    // Setup Preview Session
    private void createCameraPreviewSession() throws CameraAccessException{
        if (cameraDevice == null) return;
        configureTransformKeepAspect(textureView, textureView.getWidth(), textureView.getHeight());
        SurfaceTexture texture = textureView.getSurfaceTexture();
        texture.setDefaultBufferSize(FRAME_WIDTH, FRAME_HEIGHT);
        Surface surface = new Surface(texture);

        CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        previewRequestBuilder.addTarget(surface);
        previewRequestBuilder.addTarget(mImageReader.getSurface());

        cameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                captureSession = session;
                try {
                    captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, mBackgroundHandler);
                } catch (CameraAccessException e){
                    Log.e(TAG, e.toString());
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
        }, null);
    }

    // Setup ImageReader to get CameraImage
    private void setupImageReader(){
        // Base Resolution and Format
        // JPEG is the lightest
        mImageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, IMAGE_READER_MAX_IMAGES);

        // Image processing in Listener
        mImageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if(image == null) return;

            Thread convertingThread = new Thread(() -> {
                // Image is converted to bitmap once
                Bitmap bitmap_orig = convertJpeg2Bitmap(image);
                Bitmap bitmap_resize = resizeBitmap(bitmap_orig, FRAME_WIDTH, FRAME_HEIGHT);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                bitmap_resize.compress(Bitmap.CompressFormat.JPEG, 60, out);
                // Bitmap bitmap_rotate = rotateBitmap(bitmap_resize, FRAME_ROTATION);
                byte[] bytes = out.toByteArray();
                if(mPreviewCallback != null) {
                    mPreviewCallback.onPreview(bytes);
                }
                image.close();
                sleep(5);
            });

            convertingThread.start();
        }, mBackgroundHandler);
    }

    private Bitmap convertJpeg2Bitmap(Image imageJpeg) {
        // ImageJPEG to JPEGByteArray
        ByteBuffer buffer = imageJpeg.getPlanes()[0].getBuffer();
        int size = buffer.capacity();
        byte[] bytes = new byte[size];
        buffer.get(bytes);

        // JPEGByteArray to Bitmap
        int length = bytes.length;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, length, null);
        return bitmap;
    }

    private byte[] convertBitmap2Jpeg(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] bytes = baos.toByteArray();
        return bytes;
    }

    private Bitmap resizeBitmap(Bitmap source, int width, int height) {
        int src_width = source.getWidth() ;
        int src_height = source.getHeight();
        int limit_width = (int)( src_width * 0.8 );
        int limit_height = (int)( src_height * 0.8 );

        if(width > limit_width) return source;
        if(height > limit_height) return source;

        Bitmap bitmap = Bitmap.createScaledBitmap(source, width, height, true );
        return bitmap;
    }

    private Bitmap rotateBitmap(Bitmap source, int degrees) {
        if (degrees == 0) return source;

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        int width = source.getWidth();
        int height = source.getHeight();

        return Bitmap.createBitmap(source, 0, 0, width, height, matrix, true);
    }

    // Resize TextureView
    private void configureTransformKeepAspect(TextureView textureView, int previewWidth, int previewHeight) {
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, textureView.getWidth(), textureView.getHeight());
        RectF bufferRect = new RectF(0, 0, previewHeight, previewWidth);
        PointF center = new PointF(viewRect.centerX(), viewRect.centerY());

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(center.x - bufferRect.centerX(), center.y - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

            float scale = Math.min((float) textureView.getWidth() / previewWidth, (float) textureView.getHeight() / previewHeight);
            matrix.postScale(scale, scale, center.x, center.y);

            matrix.postRotate(90 * (rotation - 2), center.x, center.y);
        } else {
            bufferRect.offset(center.x - bufferRect.centerX(), center.y - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

            float scale = Math.min((float) textureView.getWidth() / previewHeight, (float) textureView.getHeight() / previewWidth);
            matrix.postScale(scale, scale, center.x, center.y);

            matrix.postRotate(90 * rotation, center.x, center.y);
        }

        textureView.setTransform(matrix);
    }

    private void sleep(int ms){
        try {
            Thread.sleep(ms);
        }catch (InterruptedException e){
            Log.e(TAG, e.toString());
        }
    }
}
