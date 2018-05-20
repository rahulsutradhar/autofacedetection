package test.in.mygate.cameraapp.ml;

import android.graphics.Rect;
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
import test.in.mygate.cameraapp.util.ml.Utils;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static test.in.mygate.cameraapp.util.AppConstant.AREA_OF_FRAME;
import static test.in.mygate.cameraapp.util.GeneralHelper.getOutputMediaFile;

public class CameraMLActivity extends AppCompatActivity implements FaceDetectedInFrame,
        NoFaceDetectedInFrame {

    private static final String TAG = CameraMLActivity.class.getName().toString();

    private FrameLayout cameraPreviewLayout;
    private LinearLayout afterClickedLayout, faceStatusLayout;
    private Button discardButton, saveButton;
    private TextView textFaceDetectStatus;
    private TextView textFaceAreaStatus, textFaceDirectionStatus;
    private TextView textFaceTiltStatus, textFaceFocusStatus;

    private CameraMLActivity cameraMLActivity;

    private CameraMLPreview mMLPreview;
    private Camera mCamera;
    private volatile byte[] imageData = null;


    private volatile int cameraZoomLabel = 0;

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
        faceStatusLayout = (LinearLayout) findViewById(R.id.face_status_layout);
        textFaceAreaStatus = (TextView) findViewById(R.id.face_area_status);
        textFaceDirectionStatus = (TextView) findViewById(R.id.face_direction_status);
        textFaceTiltStatus = (TextView) findViewById(R.id.face_tilt_status);
        textFaceFocusStatus = (TextView) findViewById(R.id.face_center_focused);

        textFaceAreaStatus.setText("Face need to cover " + AppConstant.MIN_FACE_PERCENT +
                "% of Frame Area and Aligh with center");

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

    /**
     * Starts the camera and Preview Screen
     */
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
                fos.write(GeneralHelper.rotate(imageData));
                //fos.write(imageData);
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
        AppConstant.LOCK_FRAME = true;
        AppConstant.MAX_FACIAL_AREA = 0;
        AppConstant.MIN_FACIAL_AREA = 0;
        AppConstant.HEIGHT_PREVIEW = 0;
        AppConstant.WIDTH_PREVIEW = 0;

        //set the zoom label to 0
        cameraZoomLabel = 0;

        //update UI
        afterClickedLayout.setVisibility(View.INVISIBLE);
        textFaceDetectStatus.setText("");
        textFaceDetectStatus.setVisibility(View.INVISIBLE);
    }

    @Override
    public void noFaceDetected() {
        //set this true so that it start to capture frame again
        AppConstant.LOCK_FRAME = true;

        //update the status in frame
        textFaceDetectStatus.setText("");
        textFaceDetectStatus.setVisibility(View.INVISIBLE);
        afterClickedLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    public synchronized void faceDetected( List<FirebaseVisionFace> firebaseVisionFace ) {
        //set this true so that it start to capture frame again
        AppConstant.LOCK_FRAME = true;

        //calculate facial area for 1-face
        Log.i(TAG, "Face Co-Ordinate: " + firebaseVisionFace.get(0).getBoundingBox());

        //check and update the zoom label
        // checkForOptimalFacialArea(facialArea);

        //clickPicture();

        verifyFaceStatus(firebaseVisionFace.get(0));
    }

    public void verifyFaceStatus( FirebaseVisionFace face ) {
        //this verify of face is inside frame
        if ( verifyFaceInsideFrame(face) ) {
            //this check if face has reached minimum face Area
            if ( verifyMinimumFaceArea(face) ) {
                //this method check if the face is intersected at the center
                if ( verifyFaceAlignWithCenter(face) ) {

                }
            }
        }
    }

    /**
     * This method checks if face is inside Frame
     *
     * @param face
     */
    private synchronized boolean verifyFaceInsideFrame( FirebaseVisionFace face ) {
        boolean isFaceInside = false;
        Rect faceRect = face.getBoundingBox();

        //if face is outside frame from left and right or top and bottom
        if ( (faceRect.left < Utils.getFrameDistanceLeft() &&
                faceRect.right > Utils.getFrameDistanceRight()) ||
                (faceRect.top < Utils.getFrameDistanceTop() &&
                        faceRect.bottom > Utils.getFrameDistanceBottom()) ) {

            //move camera back or object
            textFaceDirectionStatus.setText("Face outside frame, Put Inside");
            textFaceDirectionStatus.setTextColor(getResources().getColor(R.color.color_red));
            isFaceInside = false;
            Log.i(TAG, "onFaceDetected:  " + "Outside Frame, Move Camera Back");
        }
        //if face is outside frame from left
        else if ( faceRect.left < Utils.getFrameDistanceLeft() ) {
            textFaceDirectionStatus.setText("Move Camera LEFT");
            textFaceDirectionStatus.setTextColor(getResources().getColor(R.color.color_yellow));
            isFaceInside = false;
            Log.i(TAG, "onFaceDetected:  " + "Move Camera LEFT");
        }
        //if face is outside frame from right
        else if ( faceRect.right > Utils.getFrameDistanceRight() ) {
            textFaceDirectionStatus.setText("Move Camera RIGHT");
            textFaceDirectionStatus.setTextColor(getResources().getColor(R.color.color_yellow));
            isFaceInside = false;
            Log.i(TAG, "onFaceDetected:  " + "Move Camera RIGHT");
        }
        //if face is outside frame from top
        else if ( faceRect.top < Utils.getFrameDistanceTop() ) {
            textFaceDirectionStatus.setText("Move Camera TOP");
            textFaceDirectionStatus.setTextColor(getResources().getColor(R.color.color_yellow));
            isFaceInside = false;
            Log.i(TAG, "onFaceDetected:  " + "Move Camera TOP");
        }
        //if face is outside frame from bottom
        else if ( faceRect.bottom > Utils.getFrameDistanceBottom() ) {
            textFaceDirectionStatus.setText("Move Camera BOTTOM");
            textFaceDirectionStatus.setTextColor(getResources().getColor(R.color.color_yellow));
            isFaceInside = false;
            Log.i(TAG, "onFaceDetected:  " + "Move Camera BOTTOM");
        } else {
            textFaceDirectionStatus.setText("Face is inside frame");
            textFaceDirectionStatus.setTextColor(getResources().getColor(R.color.color_green));
            isFaceInside = true;
            Log.i(TAG, "onFaceDetected:  " + "Face is inside frame");
        }
        return isFaceInside;
    }

    /**
     * This method verify the minimum facial Area Required
     *
     * @param face
     * @return
     */
    private synchronized boolean verifyMinimumFaceArea( FirebaseVisionFace face ) {
        boolean isMinimumFaceArea = false;
        Rect faceRect = face.getBoundingBox();
        int faceArea = (faceRect.height() * faceRect.width());

        //check minimum facial area required
        if ( faceArea < Utils.getMinFaceAreaRequired() ) {
            //need to zoom the camera
            if ( cameraZoomLabel < AppConstant.MAX_CAMERA_ZOOM_AVAILABLE ) {
                Log.i(TAG, "verifyMinimumFaceArea: LESS: ZOOM  -> " + faceArea +
                        "\nRequired Area  -> " + Utils.getMinFaceAreaRequired());
                cameraZoomLabel++;

                textFaceAreaStatus.setText("Facial Area is less than " + AppConstant.MIN_FACE_PERCENT + "%");
                textFaceAreaStatus.setTextColor(getResources().getColor(R.color.color_red));
                zoomCamera(cameraZoomLabel);
            } else {
                cameraZoomLabel = 0;
                zoomCamera(cameraZoomLabel);

                textFaceAreaStatus.setText("Camera Max Zoom reached, please adjust position");
                textFaceAreaStatus.setTextColor(getResources().getColor(R.color.color_yellow));
                Log.i(TAG, "verifyMinimumFaceArea: LESS: Camera Max Zoom Reached, set to 0");
            }
            isMinimumFaceArea = false;
        }
        // check maximum facial area required, it should not increase more than this
        else if ( faceArea > Utils.getMaxFaceAreaRequired() ) {
            //if zoom is available then zoom out
            if ( cameraZoomLabel > 1 ) {
                cameraZoomLabel--;
                zoomCamera(cameraZoomLabel);

                textFaceAreaStatus.setText("Facial Area is More than " + AppConstant.MAX_FACIAL_AREA + "%");
                textFaceAreaStatus.setTextColor(getResources().getColor(R.color.color_red));
                Log.i(TAG, "verifyMinimumFaceArea: MORE: Camera is Zoomint Out");
            }
            //zoom is not available that means, position need to be adjusted
            else {
                textFaceAreaStatus.setText("Facial Area is More than " + AppConstant.MAX_FACIAL_AREA +
                        "% Please Ajust Camera");
                textFaceAreaStatus.setTextColor(getResources().getColor(R.color.color_yellow));
                Log.i(TAG, "verifyMinimumFaceArea: MORE: Camera Zoom Not Available");
            }
            isMinimumFaceArea = false;
        } else {
            isMinimumFaceArea = true;

            textFaceAreaStatus.setText("Optimal Area Reached");
            textFaceAreaStatus.setTextColor(getResources().getColor(R.color.color_green));
            Log.i(TAG, "verifyMinimumFaceArea: OPTIMAL: " + faceArea +
                    "\nRequired Area : " + Utils.getMinFaceAreaRequired());
        }

        return isMinimumFaceArea;
    }

    /**
     * This method chek=cks if face intersects center focus points
     *
     * @param face
     * @return
     */
    private synchronized boolean verifyFaceAlignWithCenter( FirebaseVisionFace face ) {
        boolean isFaceIntersectsCenter = false;
        Rect faceRect = face.getBoundingBox();

        //face is intersected at center
        if ( faceRect.left < Utils.getFrameCenterWidth() &&
                faceRect.right > Utils.getFrameCenterWidth() &&
                faceRect.top < Utils.getFrameCenterHeight() &&
                faceRect.bottom > Utils.getFrameCenterHeight() ) {

            isFaceIntersectsCenter = true;
            textFaceFocusStatus.setText("Face is intersected at Center");
            textFaceFocusStatus.setTextColor(getResources().getColor(R.color.color_green));
            Log.i(TAG, "verifyFaceAlignWithCenter: Face is Intersected at center");
        }
        //face is not intersected at center
        else {
            isFaceIntersectsCenter = false;
            textFaceFocusStatus.setText("Face Need to be intersected at Center");
            textFaceFocusStatus.setTextColor(getResources().getColor(R.color.color_red));
            Log.i(TAG, "verifyFaceAlignWithCenter: Face NOT Intersected at center");
        }

        return isFaceIntersectsCenter;
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
            if ( cameraZoomLabel < AppConstant.MAX_CAMERA_ZOOM_AVAILABLE ) {
                Log.i(TAG, "GO ZOOM IN, less than optimal");
                cameraZoomLabel++;

                //reset the capture lock variable to let it capture
                AppConstant.LOCK_FRAME = true;

                //zoom
                zoomCamera(cameraZoomLabel);

                textFaceDetectStatus.setVisibility(View.VISIBLE);
                textFaceDetectStatus.setText("Face Detected Less than Optimal");
                textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_white));
                afterClickedLayout.setVisibility(View.INVISIBLE);
            } else {
                /**
                 * If Zoom reaches Max of the Camera, then restart the zoom from 0
                 */
                cameraZoomLabel = 0;

                //reset the capture lock variable to let it capture
                AppConstant.LOCK_FRAME = true;

                zoomCamera(cameraZoomLabel);

                textFaceDetectStatus.setVisibility(View.VISIBLE);
                textFaceDetectStatus.setText("Can't zoom Anymore ");
                textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_red));
                afterClickedLayout.setVisibility(View.INVISIBLE);
                Log.i(TAG, "Maximum Zoom of the Camera Reached");
            }

        } else if ( facialArea > AREA_OF_FRAME ) {

            if ( cameraZoomLabel >= 1 ) {
                Log.i(TAG, "MORE THAN OPTIMAL ZOOM OUT");
                cameraZoomLabel--;

                //reset the capture lock variable to let it capture
                AppConstant.LOCK_FRAME = true;

                zoomCamera(cameraZoomLabel);
                textFaceDetectStatus.setVisibility(View.VISIBLE);
                textFaceDetectStatus.setText("Face Detected More than Optimal, Zooming OUT");
                textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_white));
                afterClickedLayout.setVisibility(View.INVISIBLE);
            } else {
                //zoom need to be 0
                cameraZoomLabel = 0;

                //reset the capture lock variable to let it capture
                AppConstant.LOCK_FRAME = true;

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
            AppConstant.LOCK_FRAME = false;
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
