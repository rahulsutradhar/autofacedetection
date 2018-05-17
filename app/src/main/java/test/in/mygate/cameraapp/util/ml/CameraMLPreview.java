package test.in.mygate.cameraapp.util.ml;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

import test.in.mygate.cameraapp.util.AppConstant;

/**
 * Created by developers on 17/05/18.
 */

public class CameraMLPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = CameraMLPreview.class.getName().toString();
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraMLPreview( Context context, Camera camera ) {
        super(context);
        mCamera = camera;

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
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setDisplayOrientation(90);
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


}
