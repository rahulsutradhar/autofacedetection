package test.in.mygate.cameraapp.util.ml;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

import test.in.mygate.cameraapp.util.AppConstant;

/**
 * Created by developers on 17/05/18.
 */

public class CameraMLPreview extends SurfaceView implements SurfaceHolder.Callback,
        Camera.PreviewCallback {

    private static final String TAG = CameraMLPreview.class.getName().toString();
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;

    /**
     * Constructor
     *
     * @param context
     * @param camera
     */
    public CameraMLPreview( Context context, Camera camera ) {
        super(context);
        mCamera = camera;

        // supported preview sizes
        mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        for ( Camera.Size str : mSupportedPreviewSizes )
            Log.e(TAG, str.width + "/" + str.height);


        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    @Override
    public void surfaceCreated( SurfaceHolder holder ) {
        Log.i(TAG, "onSurfaceCreated");

        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);

            AppConstant.FOCAL_LENGHT = mCamera.getParameters().getFocalLength();
            AppConstant.MAX_ZOOM_CAMERA = mCamera.getParameters().getMaxZoom();

            Log.i(TAG, "Focal length : " + AppConstant.FOCAL_LENGHT +
                    "\nMax Zoom Available : " + AppConstant.MAX_ZOOM_CAMERA);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();

        } catch ( IOException e ) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }

    }

    @Override
    public void surfaceChanged( SurfaceHolder holder, int format, int width, int height ) {
        Log.i(TAG, "onSurfaceChanged");
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if ( mHolder.getSurface() == null ) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch ( Exception e ) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            mCamera.setParameters(parameters);
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(mHolder);
            requestLayout();
            mCamera.startPreview();

        } catch ( Exception e ) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed( SurfaceHolder holder ) {
        Log.i(TAG, "onSurfaceDestroyed");
        //TODO empty. Take care of releasing the Camera preview in your activity.
        // Surface will be destroyed when we return, so stop the preview.
        mCamera.stopPreview();
        mCamera.release();
    }

    /**
     * Preview Callback, this returns each frame
     *
     * @param data
     * @param camera
     */
    @Override
    public void onPreviewFrame( byte[] data, Camera camera ) {
        Log.i(TAG, "onPreviewCallback: " + data.length);
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
            float ratio;
            if ( mPreviewSize.height >= mPreviewSize.width )
                ratio = (float) mPreviewSize.height / (float) mPreviewSize.width;
            else
                ratio = (float) mPreviewSize.width / (float) mPreviewSize.height;

            // One of these methods should be used, second method squishes preview slightly
            setMeasuredDimension(width, (int) (width * ratio));
            //setMeasuredDimension((int) (width * ratio), height);
        }
    }


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

}
