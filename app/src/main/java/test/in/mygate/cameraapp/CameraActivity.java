package test.in.mygate.cameraapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import test.in.mygate.cameraapp.events.FaceDetect;
import test.in.mygate.cameraapp.util.AppConstant;
import test.in.mygate.cameraapp.util.CameraPreview;
import test.in.mygate.cameraapp.util.GeneralHelper;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static test.in.mygate.cameraapp.util.GeneralHelper.getOutputMediaFile;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = CameraActivity.class.getName().toString();

    private FrameLayout cameraPreviewLayout;
    private LinearLayout afterClickedLayout;
    private Button discardButton, saveButton;
    private TextView textFaceDetectStatus;

    private Camera mCamera;
    private CameraPreview mPreview;
    private CameraActivity cameraActivity;
    private int currentZoomLabel = 0;
    private boolean isOptimalPercentageReached = false;
    int index = 0;
    private boolean isVariableSet = false;

    private volatile int zoomLabel = 0;

    private byte[] imageData = null;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);

        cameraActivity = this;
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
                startCameraPreview();
                isVariableSet = false;
                zoomLabel = 0;
                AppConstant.IS_FACE_DETECTED = false;
                afterClickedLayout.setVisibility(View.INVISIBLE);
                textFaceDetectStatus.setText("");
                textFaceDetectStatus.setVisibility(View.INVISIBLE);
                AppConstant.IS_FACE_DETECTED = false;
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                savePicture();
            }
        });
    }

    /**
     * Storing the optimal value for the face detect percentage
     */
    private void setVariable() {
        //minum facial area is 50% of the camera preview
        AppConstant.MIN_FACIAL_AREA = (int) Math.round((getAreaOfPreview() * 25) / 100);
        //max facial area is 60% of the camera preview
        AppConstant.MAX_FACIAL_AREA = (int) Math.round((getAreaOfPreview() * 35) / 100);

        Log.i(TAG, "Preview Area : " + getAreaOfPreview());
        Log.i(TAG, "MIN AREA : " + AppConstant.MIN_FACIAL_AREA);
        Log.i(TAG, "MAX AREA : " + AppConstant.MAX_FACIAL_AREA);
    }

    private void startCameraPreview() {
        // Create an instance of Camera
        mCamera = GeneralHelper.getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        cameraPreviewLayout.addView(mPreview);
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

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public synchronized void onFaceDetected( FaceDetect faceDetect ) {

        /**
         * One time Initialization for calculating the optimal facial area upon the preview area
         */
        if ( !isVariableSet ) {
            Log.i(TAG, "Reset Variable for Zoom and Optimal");
            zoomLabel = 0;
            isVariableSet = true;
            setVariable();
        }

        Log.i(TAG, index++ + " Face Area : " + faceDetect.getArea() + "\nView Area : " + getAreaOfPreview());

        if ( faceDetect.getArea() < AppConstant.MIN_FACIAL_AREA ) {
            /**
             * increase the zoom label: until this condition is false
             */
            if ( zoomLabel < AppConstant.MAX_ZOOM_CAMERA ) {
                Log.i(TAG, "GO ZOOM IN");
                zoomLabel++;
                zoomInCamera(zoomLabel);
                AppConstant.IS_FACE_DETECTED = false;

                textFaceDetectStatus.setVisibility(View.VISIBLE);
                textFaceDetectStatus.setText("Face Detected ");
                textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_white));
            } else {
                /**
                 * If Zoom reaches Max of the Camera, then restart the zoom from 0
                 */
                zoomLabel = 0;
                zoomInCamera(zoomLabel);
                AppConstant.IS_FACE_DETECTED = false;

                textFaceDetectStatus.setVisibility(View.VISIBLE);
                textFaceDetectStatus.setText("Can't zoom Anymore ");
                textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_red));
                Log.i(TAG, "Maximum Zoom of the Camera Reached");
            }

        } else if ( faceDetect.getArea() > AppConstant.MAX_FACIAL_AREA ) {

            if ( zoomLabel >= 1 ) {
                Log.i(TAG, "GO ZOOM OUT");
                zoomLabel--;

                zoomInCamera(zoomLabel);
                textFaceDetectStatus.setVisibility(View.VISIBLE);
                textFaceDetectStatus.setText("Face Detected ");
                textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_white));
            } else {

                Log.i(TAG, "Facial Area is More than Optimal Required, please move you camera");
                textFaceDetectStatus.setVisibility(View.VISIBLE);
                textFaceDetectStatus.setText("Facial Area is More than Optimal Required, please move you camera ");
                textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_red));
            }
            AppConstant.IS_FACE_DETECTED = false;
        } else {
            mCamera.stopFaceDetection();
            Log.i(TAG, "Alreaday in Optimal, Don't change");

            textFaceDetectStatus.setVisibility(View.VISIBLE);
            textFaceDetectStatus.setText("Photo clicked, please choose options?");
            textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_green));

            //take picture
            clickPicture();

            AppConstant.IS_FACE_DETECTED = false;
        }
    }

    int zooIndex = 0;

    /**
     * Zoom The camera
     *
     * @param newZoomValue
     */
    private synchronized void zoomInCamera( int newZoomValue ) {
        zooIndex++;
        Log.i(TAG, zooIndex + " Camera Zoomed Initiated: " + newZoomValue);
        if ( mCamera.getParameters().isZoomSupported() ) {

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setZoom(newZoomValue);
            parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            mCamera.setParameters(parameters);

            Log.i(TAG, zooIndex + " Camera ZOOMED : " + newZoomValue);
        } else {
            Log.e(TAG, "Camera ZOOMED NOT Supported : " + newZoomValue);
        }

    }

    /**
     * Take Picture
     */
    private void clickPicture() {
        if ( mCamera != null ) {
            afterClickedLayout.setVisibility(View.VISIBLE);
            mCamera.takePicture(null, null, mPicture);
        }
    }

    /**
     * Save Picture in the file
     */
    private void savePicture() {
        if ( imageData != null ) {
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if ( pictureFile == null ) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                //convert byte array to bitmap
               /* Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

                Matrix matrix = new Matrix();
                matrix.postRotate(90F);
                Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                //convert bitmap to byte array
                int bytes = newBitmap.getByteCount();
                ByteBuffer buffer = ByteBuffer.allocate(bytes); //Create a new buffer
                newBitmap.copyPixelsToBuffer(buffer);
                byte[] array = buffer.array();*/


                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(imageData);
                fos.close();

                //broadcast the gallery
                GeneralHelper.broadCastGalery(cameraActivity, pictureFile);

                afterClickedLayout.setVisibility(View.INVISIBLE);
                textFaceDetectStatus.setText("");
                textFaceDetectStatus.setVisibility(View.INVISIBLE);
                imageData = null;

                //TODO need to start the preview again, with a delay
                startCameraPreview();

            } catch ( FileNotFoundException e ) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch ( IOException e ) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        } else {
            startCameraPreview();
            Log.d(TAG, "Image data is NULL");
        }
        isVariableSet = false;
        AppConstant.IS_FACE_DETECTED = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraPreview();
        isVariableSet = false;
        zoomLabel = 0;

        textFaceDetectStatus.setVisibility(View.INVISIBLE);
        textFaceDetectStatus.setText("");
        afterClickedLayout.setVisibility(View.INVISIBLE);
        AppConstant.IS_FACE_DETECTED = false;

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    /**
     * Area for the view of camera
     *
     * @return
     */
    public int getAreaOfPreview() {
        return (cameraPreviewLayout.getWidth() * cameraPreviewLayout.getHeight());
    }
}
