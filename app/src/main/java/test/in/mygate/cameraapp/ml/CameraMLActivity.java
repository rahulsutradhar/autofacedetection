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

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import test.in.mygate.cameraapp.R;
import test.in.mygate.cameraapp.util.AppConstant;
import test.in.mygate.cameraapp.util.ml.CameraMLPreview;
import test.in.mygate.cameraapp.util.GeneralHelper;
import test.in.mygate.cameraapp.util.ml.FaceDetectedInFrame;
import test.in.mygate.cameraapp.util.ml.NoFaceDetectedInFrame;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static test.in.mygate.cameraapp.util.AppConstant.AREA_OF_FRAME;
import static test.in.mygate.cameraapp.util.GeneralHelper.getOutputMediaFile;

public class CameraMLActivity extends AppCompatActivity implements FaceDetectedInFrame,
        NoFaceDetectedInFrame {

    private static final String TAG = CameraMLActivity.class.getName().toString();

    private FrameLayout cameraPreviewLayout;
    private LinearLayout afterClickedLayout;
    private Button discardButton, saveButton;
    private TextView textFaceDetectStatus;

    private CameraMLActivity cameraMLActivity;

    private CameraMLPreview mMLPreview;
    private Camera mCamera;
    private volatile byte[] imageData = null;

    private volatile int zoomLabel = 0;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
                initialSettings();

            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                //TODO save picture
                savePicture();
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
                initialSettings();

            } catch ( FileNotFoundException e ) {
                initialSettings();
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch ( IOException e ) {
                initialSettings();
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "Image data is NULL");
            initialSettings();
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        initialSettings();
    }

    private void initialSettings() {
        //start preview
        startCameraPreview();
        //reset the capture lock variable
        AppConstant.IS_FRAME_CAPTURED = true;
        AppConstant.MAX_FACIAL_AREA = 0;
        AppConstant.MIN_FACIAL_AREA = 0;
        AppConstant.HEIGHT_PREVIEW = 0;
        AppConstant.WIDTH_PREVIEW = 0;

        //set the zoom label to 0
        zoomLabel = 0;

        //update UI
        afterClickedLayout.setVisibility(View.INVISIBLE);
        textFaceDetectStatus.setText("");
        textFaceDetectStatus.setVisibility(View.INVISIBLE);
    }

    @Override
    public void noFaceDetected() {
        //set this true so that it start to capture frame again
        AppConstant.IS_FRAME_CAPTURED = true;

        //update the status in frame
        textFaceDetectStatus.setText("");
        textFaceDetectStatus.setVisibility(View.INVISIBLE);
        afterClickedLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    public synchronized void faceDetected( List<FirebaseVisionFace> firebaseVisionFace ) {
        //set this true so that it start to capture frame again
        AppConstant.IS_FRAME_CAPTURED = false;

        //calculate facial area for 1-face
        int facialArea = firebaseVisionFace.get(0).getBoundingBox().height() * firebaseVisionFace.get(0).getBoundingBox().width();
        Log.i(TAG, "Face Co-Ordinate: " + firebaseVisionFace.get(0).getBoundingBox() +
                "\nFacial Area: " + facialArea + "\nView Area: " + AREA_OF_FRAME);

        //check and update the zoom label
        //checkForOptimalFacialArea(facialArea);

        clickPicture();
    }

    /**
     * Check for the facial area and move for zoom
     *
     * @param facialArea
     */
    private synchronized void checkForOptimalFacialArea( int facialArea ) {

        if ( facialArea < AREA_OF_FRAME ) {
            Log.i(TAG, "Less than Optimal area");
            /**
             * increase the zoom label: until this condition is false
             */
            if ( zoomLabel < AppConstant.MAX_ZOOM_CAMERA ) {
                Log.i(TAG, "GO ZOOM IN, less than optimal");
                zoomLabel++;

                //reset the capture lock variable to let it capture
                AppConstant.IS_FRAME_CAPTURED = true;

                //zoom
                zoomCamera(zoomLabel);

                textFaceDetectStatus.setVisibility(View.VISIBLE);
                textFaceDetectStatus.setText("Face Detected Less than Optimal");
                textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_white));
                afterClickedLayout.setVisibility(View.INVISIBLE);
            } else {
                /**
                 * If Zoom reaches Max of the Camera, then restart the zoom from 0
                 */
                zoomLabel = 0;

                //reset the capture lock variable to let it capture
                AppConstant.IS_FRAME_CAPTURED = true;

                zoomCamera(zoomLabel);

                textFaceDetectStatus.setVisibility(View.VISIBLE);
                textFaceDetectStatus.setText("Can't zoom Anymore ");
                textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_red));
                afterClickedLayout.setVisibility(View.INVISIBLE);
                Log.i(TAG, "Maximum Zoom of the Camera Reached");
            }

        } else if ( facialArea > AREA_OF_FRAME ) {

            if ( zoomLabel >= 1 ) {
                Log.i(TAG, "MORE THAN OPTIMAL ZOOM OUT");
                zoomLabel--;

                //reset the capture lock variable to let it capture
                AppConstant.IS_FRAME_CAPTURED = true;

                zoomCamera(zoomLabel);
                textFaceDetectStatus.setVisibility(View.VISIBLE);
                textFaceDetectStatus.setText("Face Detected More than Optimal, Zooming OUT");
                textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_white));
                afterClickedLayout.setVisibility(View.INVISIBLE);
            } else {
                //zoom need to be 0
                zoomLabel = 0;

                //reset the capture lock variable to let it capture
                AppConstant.IS_FRAME_CAPTURED = true;

                Log.i(TAG, "MORE THAN OPTIMAL CANNOT ZOOM OUT");
                textFaceDetectStatus.setVisibility(View.VISIBLE);
                textFaceDetectStatus.setText("Facial Area is More than Optimal Required, please move you camera ");
                textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_red));
                afterClickedLayout.setVisibility(View.INVISIBLE);
            }
        } else {
            //optimal camera
            mCamera.stopFaceDetection();

            //stop capturing frame as optimal reached
            AppConstant.IS_FRAME_CAPTURED = false;
            Log.i(TAG, "Already in Optimal, Don't change");

            textFaceDetectStatus.setVisibility(View.VISIBLE);
            textFaceDetectStatus.setText("Photo clicked, please choose options?");
            textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_green));
            afterClickedLayout.setVisibility(View.VISIBLE);

            //take picture
            clickPicture();
        }
    }

    /**
     * Zoom Camera
     *
     * @param newZoomValue
     */
    private void zoomCamera( int newZoomValue ) {
        if ( mCamera.getParameters().isZoomSupported() ) {

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setZoom(newZoomValue);
            mCamera.setParameters(parameters);

            Log.i(TAG, " Camera ZOOMED : " + newZoomValue);
        } else {
            Log.e(TAG, "Camera ZOOMED NOT Supported : " + newZoomValue);
        }
    }

    /**
     * Capture photo
     */
    private void clickPicture() {
        if ( mCamera != null ) {
            if ( mMLPreview != null ) {
                if ( mMLPreview.isPreviewRunning() ) {

                    afterClickedLayout.setVisibility(View.VISIBLE);
                    mCamera.takePicture(null, null, mPicture);
                }
            }
        }
    }
}
