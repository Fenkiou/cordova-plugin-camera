package org.apache.cordova.camera;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import fr.indaclouds.retake_it_app.R;

public class CameraActivity extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;

    private static final String LOG_TAG = "CameraActivity";

    public static final int MEDIA_TYPE_IMAGE = 1;

    private static Uri imageUri;

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

        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90);

        Camera.Parameters parameters = mCamera.getParameters();

        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        mCamera.setParameters(parameters);

        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        ImageButton captureButton = (ImageButton) findViewById(R.id.capture_button);

        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );

        retakeButton = (Button) findViewById(R.id.retake_button);

        retakeButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        recreate();
                    }
                }
        );

        usePhotoButton = (Button) findViewById(R.id.use_photo_button);

        usePhotoButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                }
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }


    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
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
                releaseCamera();
                setResult(Activity.RESULT_OK);
            } catch (FileNotFoundException e) {
                Log.d(LOG_TAG, "File not found: " + e.getMessage());
                releaseCamera();
            } catch (IOException e) {
                Log.d(LOG_TAG, "Error accessing file: " + e.getMessage());
                releaseCamera();
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
