package org.apache.cordova.camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import fr.indaclouds.retake_it_app.R;

public class CameraActivity extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;
    private OrientationEventListener orientationListener;

    private static final String LOG_TAG = "CameraActivity";

    public static final int MEDIA_TYPE_IMAGE = 1;

    private static Uri imageUri;

    private ImageButton captureButton;
    private Button retakeButton;
    private Button usePhotoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        setContentView(R.layout.activity_camera);

        Intent intent = getIntent();
        String optionalImageUri = intent.getStringExtra(CameraLauncher.OPTIONAL_IMAGE_URI);
        imageUri = Uri.parse(intent.getStringExtra(CameraLauncher.IMAGE_URI));
        float alpha = Float.parseFloat(intent.getStringExtra(CameraLauncher.ALPHA)) / 100;

        setCameraPreview();

        captureButton = (ImageButton) findViewById(R.id.capture_button);
        captureButton.setRotation(270);

        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );

        retakeButton = (Button) findViewById(R.id.retake_button);
        usePhotoButton = (Button) findViewById(R.id.use_photo_button);

        retakeButton.setRotation(270);
        usePhotoButton.setRotation(270);

        retakeButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        recreate();
                    }
                }
        );

        usePhotoButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                }
        );

        orientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation >= 315 && orientation <= 360 || orientation >= 0 && orientation <= 45) {
                    captureButton.setRotation(0);
                    retakeButton.setRotation(0);
                    usePhotoButton.setRotation(0);
                    setCameraRotation(90);
                }
                else if (orientation >= 225 && orientation < 315) {
                    captureButton.setRotation(90);
                    retakeButton.setRotation(90);
                    usePhotoButton.setRotation(90);
                    setCameraRotation(0);
                }
                else if (orientation >= 135 && orientation < 225) {
                    captureButton.setRotation(180);
                    retakeButton.setRotation(180);
                    usePhotoButton.setRotation(180);
                    setCameraRotation(270);
                }
                else {
                    captureButton.setRotation(270);
                    retakeButton.setRotation(270);
                    usePhotoButton.setRotation(270);
                    setCameraRotation(180);
                }
            }
        };

        orientationListener.enable();

        setBackgroundPicture(optionalImageUri, alpha);
    }

    @Override
    protected void onStop() {
        super.onStop();
        orientationListener.disable();
        releaseCamera();
    }

    private void setCameraPreview() {
        mCamera = getCameraInstance();

        Camera.Parameters parameters = mCamera.getParameters();

        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);

        CameraPreview preview = (CameraPreview) findViewById(R.id.camera_preview);
        preview.init(mCamera);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        int width = displaymetrics.widthPixels;

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, (int) (width * 1.33));
        preview.setLayoutParams(params);
    }

    private void setCameraRotation(int rotation) {
        Camera.Parameters parameters = mCamera.getParameters();

        parameters.setRotation(rotation);

        mCamera.setParameters(parameters);
    }

    private void setBackgroundPicture(String imageUri, float alpha) {
        File imgFile = new  File(imageUri);

        if (imgFile.exists()){

            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            ImageView myImage = (ImageView) findViewById(R.id.background_picture);

            DisplayMetrics displaymetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

            int width = displaymetrics.widthPixels;

            if (myBitmap.getWidth() > myBitmap.getHeight()) {
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(myBitmap, (int) (width * 1.33), width, true);

                Matrix matrix = new Matrix();
                matrix.postRotate(90);

                myBitmap = Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
            }

            myImage.setImageBitmap(myBitmap);
            myImage.setAlpha(alpha);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, (int) (width * 1.33));

            myImage.setLayoutParams(params);

        }
    }

    /** A safe way to get an instance of the Camera object. */
    public Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
            Log.d(LOG_TAG, String.valueOf(e));
            // Camera is not available (in use or does not exist)
        }
        return c;
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }

    private PictureCallback mPicture = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            retakeButton.setVisibility(View.VISIBLE);
            usePhotoButton.setVisibility(View.VISIBLE);

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

            if (pictureFile == null){
                Log.d(LOG_TAG, "Error creating media file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                setResult(Activity.RESULT_OK);
            } catch (FileNotFoundException e) {
                Log.d(LOG_TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(LOG_TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Checks if external storage is available for read and write **/
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        if (!isExternalStorageWritable()) {
            return null;
        }

        File mediaFile;

        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(imageUri.getPath());
        } else {
            return null;
        }

        return mediaFile;
    }
}