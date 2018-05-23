package test.in.mygate.cameraapp.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
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
import static test.in.mygate.cameraapp.util.GeneralHelper.getOutputMediaFile;

public class CameraMLActivity extends AppCompatActivity implements FaceDetectedInFrame,
        NoFaceDetectedInFrame {

    private static final String TAG = CameraMLActivity.class.getName().toString();

    private FrameLayout cameraPreviewLayout;
    private LinearLayout afterClickedLayout, faceStatusLayout;
    private Button discardButton, saveButton;
    private TextView textFaceDetectStatus;
    private TextView textFaceAreaStatus, textFaceDirectionStatus;
    private TextView textFaceCurrentStatus, textFaceFocusStatus;

    private CameraMLActivity cameraMLActivity;

    private CameraMLPreview mMLPreview;
    private Camera mCamera;
    private volatile byte[] imageData = null;


    private volatile int cameraZoomLabel = 0;

    private volatile int countConditionProbability = 0;
    private volatile Rect faceRect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        textFaceCurrentStatus = (TextView) findViewById(R.id.face_current_status);
        textFaceFocusStatus = (TextView) findViewById(R.id.face_center_focused);


        afterClickedLayout.setVisibility(View.INVISIBLE);

        discardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO discard picture
                initialSettings();

            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        public void onPictureTaken(byte[] data, Camera camera) {

            /**
             * and display wheather user wants to save the image or discard
             */
            imageData = data;
        }
    };

    private final Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            try {
                //keep empty
                MediaActionSound sound = new MediaActionSound();
                sound.play(MediaActionSound.SHUTTER_CLICK);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void savePicture() {
        if (imageData != null) {
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                Bitmap rotatedImage = GeneralHelper.rotate(imageData);
                byte[] cropedImage = GeneralHelper.cropImage(rotatedImage,faceRect);
                fos.write(cropedImage);
                fos.close();

                //broadcast the gallery
                GeneralHelper.broadCastGalery(cameraMLActivity, pictureFile);

                //TODO need to start the preview again, with a delay
                initialSettings();

            } catch (FileNotFoundException e) {
                initialSettings();
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
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

    /**
     * Initial setting for the UI
     */
    private void initialSettings() {
        //start preview
        startCameraPreview();

        //set the zoom label to 0
        if (cameraZoomLabel > 0) {
            cameraZoomLabel = 0;
            zoomCamera(cameraZoomLabel);
        }
        countConditionProbability = 0;

        //update UI
        textFaceDetectStatus.setText("");
        afterClickedLayout.setVisibility(View.INVISIBLE);

        //update the other message text
        textFaceAreaStatus.setText("Face need to cover " + AppConstant.MIN_FACE_PERCENT +
                "% of Frame Area and Aligh with center");
        textFaceAreaStatus.setTextColor(getResources().getColor(R.color.color_white));
        textFaceAreaStatus.setVisibility(View.INVISIBLE);

        textFaceDirectionStatus.setText("Please put face inside the frame");
        textFaceDirectionStatus.setTextColor(getResources().getColor(R.color.color_white));
        textFaceDirectionStatus.setVisibility(View.VISIBLE);

        textFaceFocusStatus.setText("Face Need to be focused at Center Point");
        textFaceFocusStatus.setTextColor(getResources().getColor(R.color.color_white));
        textFaceFocusStatus.setVisibility(View.INVISIBLE);

        textFaceCurrentStatus.setText("Face need to be Straight");
        textFaceCurrentStatus.setTextColor(getResources().getColor(R.color.color_white));
        textFaceCurrentStatus.setVisibility(View.INVISIBLE);

        faceStatusLayout.setVisibility(View.VISIBLE);
        textFaceDetectStatus.setVisibility(View.VISIBLE);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //reset the capture lock variable
                AppConstant.LOCK_FRAME = true;
            }
        }, 3000);

    }

    @Override
    public void noFaceDetected() {
        //set this true so that it start to capture frame again
        AppConstant.LOCK_FRAME = true;

        //update the status in frame
        textFaceDetectStatus.setText("No Face detected");
        textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_red));
        afterClickedLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    public synchronized void faceDetected(List<FirebaseVisionFace> firebaseVisionFace) {
        //set this true so that it start to capture frame again
        AppConstant.LOCK_FRAME = true;

        //calculate facial area for 1-face
        Log.i(TAG, "Face Co-Ordinate: " + firebaseVisionFace.get(0).getBoundingBox());

        textFaceDetectStatus.setText("Face detecting.");
        textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_yellow));
        afterClickedLayout.setVisibility(View.INVISIBLE);

        // verifyFaceStatus(firebaseVisionFace.get(0));

        verifyFrame(firebaseVisionFace.get(0));
    }

    /**
     * Check If photo is inside frame
     *
     * @param face
     */
    private void verifyFrame(FirebaseVisionFace face) {
        //this verify of face is inside frame
        if (verifyFaceInsideFrame(face)) {
            AppConstant.LOCK_FRAME = false;
            faceRect = face.getBoundingBox();

            //click photo
            takePhotoAndCrop();
        }
    }


    /**
     * This method check the condition for different case for the face
     *
     * @param face
     */
    public void verifyFaceStatus(FirebaseVisionFace face) {
        //this verify of face is inside frame
        if (verifyFaceInsideFrame(face)) {
            //this check if face has reached minimum face Area
            if (verifyMinimumFaceArea(face)) {
                //this method check if the face is intersected at the center
                if (verifyFaceAlignWithCenter(face)) {
                    //this method check if the face is tilted or not
                    if (verifyFaceLandmarks(face)) {

                        //if countConditionProbability is 2 then click photo
                        countConditionProbability++;
                        if (countConditionProbability > 2) {

                            AppConstant.LOCK_FRAME = false;
                            textFaceDetectStatus.setVisibility(View.VISIBLE);
                            textFaceDetectStatus.setText("Hold on Clicking photo in 3 seconds");
                            textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_yellow));

                            //take picture in next 3 seconds
                            clickPicture();
                        }
                    }
                } else {
                    countConditionProbability = 0;
                }
            } else {
                countConditionProbability = 0;
            }
        } else {
            countConditionProbability = 0;
        }
    }

    /**
     * This method checks if face is inside Frame
     *
     * @param face
     */
    private synchronized boolean verifyFaceInsideFrame(FirebaseVisionFace face) {
        boolean isFaceInside = false;
        Rect faceRect = face.getBoundingBox();

        //if face is outside frame from left and right or top and bottom
        if ((faceRect.left < Utils.getFrameDistanceLeft() &&
                faceRect.right > Utils.getFrameDistanceRight()) ||
                (faceRect.top < Utils.getFrameDistanceTop() &&
                        faceRect.bottom > Utils.getFrameDistanceBottom())) {

            //move camera back or object
            textFaceDirectionStatus.setText("Face outside frame, Put Inside");
            textFaceDirectionStatus.setTextColor(getResources().getColor(R.color.color_red));
            isFaceInside = false;
            Log.i(TAG, "onFaceDetected:  " + "Outside Frame, Move Camera Back");

            /**
             * Also check if camera is zoomed, then zoom out
             */
            if (cameraZoomLabel > 0) {
                //zoom out
                cameraZoomLabel--;
                zoomCamera(cameraZoomLabel);
                Log.i(TAG, "onFaceDetected:  " + "Outside Frame, Camera Zoom is available then, zoom out");
            }


        }
        //if face is outside frame from left
        else if (faceRect.left < Utils.getFrameDistanceLeft()) {
            textFaceDirectionStatus.setText("Move Camera LEFT");
            textFaceDirectionStatus.setTextColor(getResources().getColor(R.color.color_yellow));
            isFaceInside = false;
            Log.i(TAG, "onFaceDetected:  " + "Move Camera LEFT");
        }
        //if face is outside frame from right
        else if (faceRect.right > Utils.getFrameDistanceRight()) {
            textFaceDirectionStatus.setText("Move Camera RIGHT");
            textFaceDirectionStatus.setTextColor(getResources().getColor(R.color.color_yellow));
            isFaceInside = false;
            Log.i(TAG, "onFaceDetected:  " + "Move Camera RIGHT");
        }
        //if face is outside frame from top
        else if (faceRect.top < Utils.getFrameDistanceTop()) {
            textFaceDirectionStatus.setText("Move Camera TOP");
            textFaceDirectionStatus.setTextColor(getResources().getColor(R.color.color_yellow));
            isFaceInside = false;
            Log.i(TAG, "onFaceDetected:  " + "Move Camera TOP");
        }
        //if face is outside frame from bottom
        else if (faceRect.bottom > Utils.getFrameDistanceBottom()) {
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
    private synchronized boolean verifyMinimumFaceArea(FirebaseVisionFace face) {
        boolean isMinimumFaceArea = false;
        Rect faceRect = face.getBoundingBox();
        int faceArea = (faceRect.height() * faceRect.width());

        //check minimum facial area required
        if (faceArea < Utils.getMinFaceAreaRequired()) {
            //need to zoom the camera
            if (cameraZoomLabel < AppConstant.MAX_CAMERA_ZOOM_AVAILABLE) {
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
        else if (faceArea > Utils.getMaxFaceAreaRequired()) {
            //if zoom is available then zoom out
            if (cameraZoomLabel > 0) {
                cameraZoomLabel--;
                zoomCamera(cameraZoomLabel);

                textFaceAreaStatus.setText("Facial Area is More than " + AppConstant.MAX_FACE_PERCENT + "%");
                textFaceAreaStatus.setTextColor(getResources().getColor(R.color.color_red));
                Log.i(TAG, "verifyMinimumFaceArea: MORE: Camera is Zooming Out");
            }
            //zoom is not available that means, position need to be adjusted
            else {
                textFaceAreaStatus.setText("Facial Area is More than " + AppConstant.MAX_FACE_PERCENT +
                        "% Please Adjust Camera");
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
    private synchronized boolean verifyFaceAlignWithCenter(FirebaseVisionFace face) {
        boolean isFaceIntersectsCenter = false;
        Rect faceRect = face.getBoundingBox();

        //face is intersected at center
        if (faceRect.left < Utils.getFrameCenterWidth() &&
                faceRect.right > Utils.getFrameCenterWidth() &&
                faceRect.top < Utils.getFrameCenterHeight() &&
                faceRect.bottom > Utils.getFrameCenterHeight()) {

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
     * This method check if the face is stright out of the image plane
     * Can also check if Eyes are open or not
     *
     * @param face
     * @return
     */
    private synchronized boolean verifyFaceLandmarks(FirebaseVisionFace face) {
        boolean islandmarkVerified = false;
        int OFF_SET_POSITIVE = 12;
        int OFF_SET_NEGATIVE = -12;

        //check if face is turned right or left HeadEulerAngleY
        if (face.getHeadEulerAngleY() > OFF_SET_POSITIVE) {
            textFaceCurrentStatus.setText("Head is Turned RIGHT, Make Straight");
            textFaceCurrentStatus.setTextColor(getResources().getColor(R.color.color_red));
            Log.i(TAG, "verifyFaceLandmarks: Heade is turned RIGHT");
            islandmarkVerified = false;
            return islandmarkVerified;
        } else if (face.getHeadEulerAngleY() < OFF_SET_NEGATIVE) {
            textFaceCurrentStatus.setText("Head is Turned LEFT, Make Straight");
            textFaceCurrentStatus.setTextColor(getResources().getColor(R.color.color_red));
            Log.i(TAG, "verifyFaceLandmarks: Heade is turned LEFT");
            islandmarkVerified = false;
            return islandmarkVerified;
        } else {
            islandmarkVerified = true;
            Log.i(TAG, "verifyFaceLandmarks: Head is STRAIGHT from Left right condition");
        }

        //check if face is turned top or bottom
        if (face.getHeadEulerAngleZ() > OFF_SET_POSITIVE) {
            textFaceCurrentStatus.setText("Head is Turned UP, Make Straight");
            textFaceCurrentStatus.setTextColor(getResources().getColor(R.color.color_red));
            Log.i(TAG, "verifyFaceLandmarks: Heade is turned UP");
            islandmarkVerified = false;
            return islandmarkVerified;
        } else if (face.getHeadEulerAngleZ() < OFF_SET_NEGATIVE) {
            textFaceCurrentStatus.setText("Head is Turned BOTTOM, Make Straight");
            textFaceCurrentStatus.setTextColor(getResources().getColor(R.color.color_red));
            Log.i(TAG, "verifyFaceLandmarks: Heade is turned BOTTOM");
            islandmarkVerified = false;
            return islandmarkVerified;
        } else {
            islandmarkVerified = true;
            Log.i(TAG, "verifyFaceLandmarks: Head is STRAIGHT from Left right condition");
        }

        if (islandmarkVerified) {
            textFaceCurrentStatus.setText("Head is Straight");
            textFaceCurrentStatus.setTextColor(getResources().getColor(R.color.color_green));
        }
        return islandmarkVerified;
    }

    /**
     * Zoom Camera
     *
     * @param newZoomValue
     */
    private void zoomCamera(int newZoomValue) {
        //this check if camera is running or not NULL
        if (mCamera != null) {
            if (mMLPreview != null) {
                if (mMLPreview.isPreviewRunning()) {

                    if (mCamera.getParameters().isZoomSupported()) {
                        Camera.Parameters parameters = mCamera.getParameters();
                        parameters.setZoom(newZoomValue);
                        mCamera.setParameters(parameters);

                        Log.i(TAG, " Camera ZOOMED : " + newZoomValue);
                    } else {
                        Log.e(TAG, "Camera ZOOMED NOT Supported : " + newZoomValue);
                    }
                }
            }
        }
    }

    /**
     * Capture photo
     */
    private void clickPicture() {
        if (mCamera != null) {
            if (mMLPreview != null) {
                if (mMLPreview.isPreviewRunning()) {

                    /**
                     * This method clicks photo is 3 seconds
                     */
                    new CountDownTimer(3000, 1000) {
                        public void onFinish() {

                            try {

                                if (mCamera != null) {
                                    // When timer is finished
                                    mCamera.takePicture(mShutterCallback, null, mPicture);

                                    //show UI
                                    clearView();
                                    textFaceDetectStatus.setText("Clicked, please choose options");
                                    textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_green));
                                    afterClickedLayout.setVisibility(View.VISIBLE);
                                    Log.i(TAG, "photo clicked");
                                }
                            } catch (RuntimeException re) {
                                re.printStackTrace();
                                Log.e(TAG, "Camera.takePicture : " + re.getMessage());
                            }
                        }

                        public void onTick(long millisUntilFinished) {
                            // millisUntilFinished    The amount of time until finished.
                            textFaceDetectStatus.setText("Hold on Clicking photo in " + (millisUntilFinished / 1000) + " seconds");
                            textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_yellow));
                            Log.i(TAG, "photo clicked waiting seconds");
                        }
                    }.start();
                }
            }
        }

    }

    /**
     * This removes the view from the camera
     */
    private void clearView() {
        //mMLPreview.removeFrame();
        faceStatusLayout.setVisibility(View.INVISIBLE);
    }


    private void takePhotoAndCrop() {
        if (mCamera != null) {
            if (mMLPreview != null) {
                if (mMLPreview.isPreviewRunning()) {

                    mCamera.takePicture(mShutterCallback, null, mPicture);

                    //show UI
                    clearView();
                    textFaceDetectStatus.setText("Clicked, please choose options");
                    textFaceDetectStatus.setTextColor(getResources().getColor(R.color.color_green));
                    afterClickedLayout.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
