package test.in.mygate.cameraapp.ml;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import test.in.mygate.cameraapp.R;
import test.in.mygate.cameraapp.util.ml.CameraMLPreview;
import test.in.mygate.cameraapp.util.GeneralHelper;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static test.in.mygate.cameraapp.util.GeneralHelper.getOutputMediaFile;

public class CameraMLActivity extends AppCompatActivity {

    private static final String TAG = CameraMLActivity.class.getName().toString();

    private FrameLayout cameraPreviewLayout;
    private LinearLayout afterClickedLayout;
    private Button discardButton, saveButton;
    private TextView textFaceDetectStatus;

    private CameraMLActivity cameraMLActivity;

    private CameraMLPreview mMLPreview;
    private Camera mCamera;
    private byte[] imageData = null;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera_ml);

        cameraMLActivity = this;

        setViews();
    }

    private void setViews() {
        cameraPreviewLayout = (FrameLayout) findViewById(R.id.camera_preview);
        discardButton = (Button) findViewById(R.id.discard_button);
        saveButton = (Button) findViewById(R.id.save_button);
        afterClickedLayout = (LinearLayout) findViewById(R.id.after_click_layout);
        textFaceDetectStatus = (TextView) findViewById(R.id.face_detected_status);

        afterClickedLayout.setVisibility(View.INVISIBLE);

        discardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                //TODO discard picture
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                //TODO save picture
            }
        });
    }

    private void startCameraPreview() {
        // Create an instance of Camera
        mCamera = GeneralHelper.getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mMLPreview = new CameraMLPreview(this, this, mCamera);
        cameraPreviewLayout.addView(mMLPreview);
    }

    /**
     * Callback Fo PictureCallback
     */
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken( byte[] data, Camera camera ) {

            /**
             * and display wheather user wants to save the image or discard
             */
            imageData = data;
        }
    };

    private void savePicture() {
        if ( imageData != null ) {
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if ( pictureFile == null ) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(imageData);
                fos.close();

                //broadcast the gallery
                GeneralHelper.broadCastGalery(cameraMLActivity, pictureFile);

                //TODO need to start the preview again, with a delay
                //startCameraPreview();

            } catch ( FileNotFoundException e ) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch ( IOException e ) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        } else {
            startCameraPreview();
            Log.d(TAG, "Image data is NULL");
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        startCameraPreview();
    }
}
