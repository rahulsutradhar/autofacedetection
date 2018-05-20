package test.in.mygate.cameraapp.util.ml;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.IOException;
import java.util.List;

import test.in.mygate.cameraapp.util.AppConstant;
import test.in.mygate.cameraapp.util.GeneralHelper;

/**
 * Created by developers on 17/05/18.
 */

public class CameraMLPreview extends SurfaceView implements SurfaceHolder.Callback,
        Camera.PreviewCallback {

    private static final String TAG = CameraMLPreview.class.getName().toString();
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private volatile List<Camera.Size> mSupportedPreviewSizes;
    private volatile Camera.Size mPreviewSize;
    private Context mContext;
    private Activity mActivity;

    private int measuredWidth = 0, measuredHeight = 0;

    private volatile boolean isPreviewRunning = false;
    private int cameraId = -1;

    // for checking the orientation
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    //ML Kit classes
    private FirebaseVisionFaceDetectorOptions options;
    private FirebaseVisionImageMetadata metadata;
    private FirebaseVisionFaceDetector detector;

    //interface variables
    private FaceDetectedInFrame faceDetectedInFrame;
    private NoFaceDetectedInFrame noFaceDetectedInFrame;

    /**
     * Constructor
     *
     * @param context
     * @param camera
     */
    public CameraMLPreview( Activity activity, Context context, Camera camera ) {
        super(context);
        mCamera = camera;
        mContext = context;
        mActivity = activity;

        // supported preview sizes
        mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        for ( Camera.Size str : mSupportedPreviewSizes )
            Log.e(TAG, str.width + "/" + str.height);

        //set the display orientation
        GeneralHelper.setCameraDisplayOrientation(mActivity, getBackFacingCameraId());

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);


        //this creates the firebase detector instance once
        getFirebaseVisionFaceDetector();

        //interface intialization
        faceDetectedInFrame = (FaceDetectedInFrame) context;
        noFaceDetectedInFrame = (NoFaceDetectedInFrame) context;
    }

    @Override
    public void surfaceCreated( SurfaceHolder holder ) {
        Log.i(TAG, "onSurfaceCreated");

        // The Surface has been created, now tell the camera where to draw the preview.
        try {

            if ( isPreviewRunning ) {
                return;
            }

            AppConstant.WIDTH_PREVIEW = mPreviewSize.width;
            AppConstant.HEIGHT_PREVIEW = mPreviewSize.height;
            Log.i(TAG, "onSurfaceCreated - FRAME SIZE SET - Width: " + mPreviewSize.width +
                    "  Height: " + mPreviewSize.height);

            mCamera.setPreviewDisplay(holder);

            AppConstant.FOCAL_LENGHT = mCamera.getParameters().getFocalLength();
            AppConstant.MAX_ZOOM_CAMERA = (int) Math.round((mCamera.getParameters().getMaxZoom() * 2) / 3);

            Log.i(TAG, "Focal length : " + AppConstant.FOCAL_LENGHT +
                    "\nMax Zoom Available : " + AppConstant.MAX_ZOOM_CAMERA);

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            parameters.setPictureSize(mPreviewSize.width, mPreviewSize.height);
            //TODO set Rotation
            if ( Build.VERSION.SDK_INT > 17 ) {
                mCamera.enableShutterSound(true);
            }
            mCamera.setParameters(parameters);
            mCamera.setDisplayOrientation(GeneralHelper.getCameraDisplayOrientation());
            mCamera.setPreviewCallback(this);
            requestLayout();
            isPreviewRunning = true;
            mCamera.startPreview();

            //this stores the area of the camera preview and face end points
            calculateOptimalArea();

        } catch ( IOException e ) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }

    }

    @Override
    public void surfaceChanged( SurfaceHolder holder, int format, int width, int height ) {
        Log.i(TAG, "onSurfaceChanged");
        synchronized (this) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if ( mHolder.getSurface() == null ) {
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                isPreviewRunning = false;
                mCamera.stopPreview();
            } catch ( Exception e ) {
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                AppConstant.WIDTH_PREVIEW = mPreviewSize.width;
                AppConstant.HEIGHT_PREVIEW = mPreviewSize.height;

                Log.i(TAG, "onSurfaceChanged - FRAME SIZE SET - Width: " + mPreviewSize.width +
                        "  Height: " + mPreviewSize.height);

                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
                parameters.setPictureSize(mPreviewSize.width, mPreviewSize.height);
                if ( Build.VERSION.SDK_INT > 17 ) {
                    mCamera.enableShutterSound(true);
                }
                mCamera.setParameters(parameters);
                mCamera.setDisplayOrientation(GeneralHelper.getCameraDisplayOrientation());
                mCamera.setPreviewCallback(this);
                mCamera.setPreviewDisplay(mHolder);
                requestLayout();
                isPreviewRunning = true;
                mCamera.startPreview();

            } catch ( Exception e ) {
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

    @Override
    public void surfaceDestroyed( SurfaceHolder holder ) {

        synchronized (this) {
            try {
                if ( mCamera != null ) {
                    isPreviewRunning = false;
                    Log.i(TAG, "onSurfaceDestroyed");
                    //TODO empty. Take care of releasing the Camera preview in your activity.
                    // Surface will be destroyed when we return, so stop the preview.
                    mCamera.setPreviewCallback(null);
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                }
            } catch ( RuntimeException re ) {
                re.printStackTrace();
            }
        }

    }

    @Override
    protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);


        if ( mSupportedPreviewSizes != null ) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }

        if ( mPreviewSize != null ) {
            float previewRatio = (float) mPreviewSize.height / (float) mPreviewSize.width;
            // previewRatio is height/width because camera preview size are in landscape.
            float measuredSizeRatio = (float) width / (float) height;

            if ( previewRatio >= measuredSizeRatio ) {
                measuredHeight = height;
                measuredWidth = (int) ((float) height * previewRatio);
            } else {
                measuredWidth = width;
                measuredHeight = (int) ((float) width / previewRatio);
            }
            Log.i(TAG, "Preview size: " + width + "w, " + height + "h" +
                    "\nPreview size calculated: " + measuredWidth + "w, " + measuredHeight + "h" +
                    "\nCAMERA Size: " + mPreviewSize.width + "w, " + mPreviewSize.height + "h");

            setMeasuredDimension(measuredWidth, measuredHeight);
        }

    }


    /**
     * Calculate the optimal size
     *
     * @param sizes
     * @param w
     * @param h
     * @return
     */
    private Camera.Size getOptimalPreviewSize( List<Camera.Size> sizes, int w, int h ) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if ( sizes == null )
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for ( Camera.Size size : sizes ) {
            double ratio = (double) size.height / size.width;
            if ( Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE )
                continue;

            if ( Math.abs(size.height - targetHeight) < minDiff ) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if ( optimalSize == null ) {
            minDiff = Double.MAX_VALUE;
            for ( Camera.Size size : sizes ) {
                if ( Math.abs(size.height - targetHeight) < minDiff ) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    /**
     * Returns the camera ID for the Back Facing camera
     *
     * @return
     */
    private int getBackFacingCameraId() {
        if ( mCamera != null ) {
            for ( int i = 0; i < mCamera.getNumberOfCameras(); i++ ) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if ( info.facing == Camera.CameraInfo.CAMERA_FACING_BACK ) {
                    Log.d(TAG, "Camera found Back ID : " + i);
                    cameraId = i;
                    break;
                }
            }
        }

        return cameraId;
    }

    /**
     * Preview Callback, this returns each frame
     *
     * @param data
     * @param camera
     */
    @Override
    public void onPreviewFrame( byte[] data, Camera camera ) {
        Log.i(TAG, "onPreviewCallback: ");

        if ( !isPreviewRunning ) {
            return;
        }
        if ( mHolder == null ) {
            return;
        }

        try {
            synchronized (this) {
                if ( data != null ) {
                    /**
                     * Capture the Frame only when it is TRUE
                     * and lock it until its FALSE
                     */
                    Log.i(TAG, "isFrame LOCKED : " + AppConstant.LOCK_FRAME);
                    if ( AppConstant.LOCK_FRAME ) {
                        Log.i(TAG, "isFrame LOCKED --> Has Captured : " + AppConstant.LOCK_FRAME);
                        //capture so lock it by setting false
                        AppConstant.LOCK_FRAME = false;

                        letFirebaseMLDetectFace(data);
                    }
                }
            }
        } catch ( NullPointerException npe ) {
            npe.printStackTrace();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Firebase Facedetect calalback
     *
     * @param data
     */
    private synchronized void letFirebaseMLDetectFace( byte[] data ) {

        getFirebaseVisionFaceDetector().detectInImage(getFirebaseVisionImage(data))
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess( List<FirebaseVisionFace> firebaseVisionFaces ) {
                        Log.i(TAG, "FaceDetected Success Callback");
                        if ( firebaseVisionFaces != null ) {
                            if ( firebaseVisionFaces.size() > 0 ) {

                                //TODO handle Face detected here
                                Log.i(TAG, "FaceDetected: " + firebaseVisionFaces.get(0).getBoundingBox());

                                //handle face recognization
                                handleFaceDetected(firebaseVisionFaces);

                            } else {
                                //TODO handle no face detected here
                                //handle NO face recognization
                                handleNoFaceDetected();
                            }
                        } else {
                            //handle NO face recognization
                            handleNoFaceDetected();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure( @NonNull Exception e ) {
                        Log.e(TAG, "FaceDetected ERROR Callback:  " + e.getMessage());
                        //handle NO face recognization
                        handleNoFaceDetected();
                    }
                });
    }

    /**
     * This method Returns the firebase vision face detectior instance
     *
     * @return
     */
    private FirebaseVisionFaceDetector getFirebaseVisionFaceDetector() {
        if ( detector == null ) {
            //Create Firebase Vision FaceDetector Options instance
            options = new FirebaseVisionFaceDetectorOptions.Builder()
                    .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                    .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                    .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                    .setMinFaceSize(0.15f)
                    .setTrackingEnabled(true)
                    .build();

            detector = FirebaseVision.getInstance()
                    .getVisionFaceDetector(options);
        }
        return detector;
    }

    /**
     * This method creates a FirebaseVisionImage using the
     * byte[] of the image and the meta data created
     *
     * @param imageData
     * @return
     */
    private synchronized FirebaseVisionImage getFirebaseVisionImage( byte[] imageData ) {
        FirebaseVisionImage firebaseVisionImage = null;
        if ( imageData != null ) {
            firebaseVisionImage = FirebaseVisionImage
                    .fromByteArray(imageData, getFirebaseVisionImageMetadata());
        }
        return firebaseVisionImage;
    }


    /**
     * Returns FirebaseVisionImageMetaData
     *
     * @return
     */
    private FirebaseVisionImageMetadata getFirebaseVisionImageMetadata() {
        if ( metadata == null ) {
            metadata = new FirebaseVisionImageMetadata.Builder()
                    .setWidth(mPreviewSize.width)
                    .setHeight(mPreviewSize.height)
                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                    .setRotation(getRotationCompensation())
                    .build();
        }
        return metadata;
    }

    /**
     * Calculate the Rotation for Firebase Vision Image
     *
     * @return
     */
    public int getRotationCompensation() {
        int result;

        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        /*int deviceRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);*/

        //TODO: need to refactor this rotation according to the camera sensor rotation
        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.

        switch ( GeneralHelper.getCameraDisplayOrientation() ) {
            case 0:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                result = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                result = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                result = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                result = FirebaseVisionImageMetadata.ROTATION_0;
        }

        return result;
    }


    /**
     * Helper method to display message and zoom,
     * which is passed to the activity and it is handle there
     */

    /**
     * Hanlde when atleast one face is detected
     *
     * @param firebaseVisionFaces
     */
    private void handleFaceDetected( List<FirebaseVisionFace> firebaseVisionFaces ) {
        faceDetectedInFrame.faceDetected(firebaseVisionFaces);
    }

    /**
     * Handle when no face is detected
     * this is passed in the activity class
     */
    private void handleNoFaceDetected() {
        noFaceDetectedInFrame.noFaceDetected();
    }

    /**
     * This Calculates the end area points of frame and the facial
     */
    private void calculateOptimalArea() {
        AppConstant.AREA_OF_FRAME = (AppConstant.WIDTH_PREVIEW * AppConstant.HEIGHT_PREVIEW);
        AppConstant.MIN_FACIAL_AREA = (int) Math.round((AppConstant.AREA_OF_FRAME * 8) / 100);
        AppConstant.MAX_FACIAL_AREA = (int) Math.round((AppConstant.AREA_OF_FRAME * 16) / 100);

        Log.i(TAG, "AREA OF FRAME : " + AppConstant.AREA_OF_FRAME +
                "\nMIN FACIAL AREA : " + AppConstant.MIN_FACIAL_AREA +
                "\nMAX FACIAL AREA : " + AppConstant.MAX_FACIAL_AREA);

    }

    @Override
    protected void onDraw( Canvas canvas ) {
        super.onDraw(canvas);
    }

    public boolean isPreviewRunning() {
        return isPreviewRunning;
    }

    public void setPreviewRunning( boolean previewRunning ) {
        isPreviewRunning = previewRunning;
    }
}
