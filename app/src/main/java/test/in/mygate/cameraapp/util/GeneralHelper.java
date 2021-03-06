package test.in.mygate.cameraapp.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

/**
 * Created by developers on 14/05/18.
 */

public class GeneralHelper {

    private volatile static int cameraDisplayOrientation = 0;

    public static boolean checkCameraHardware( Context context ) {
        if ( context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) ) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch ( Exception e ) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /**
     * Create a File for saving an image or video
     */
    public static File getOutputMediaFile( int type ) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if ( !mediaStorageDir.exists() ) {
            if ( !mediaStorageDir.mkdirs() ) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if ( type == MEDIA_TYPE_IMAGE ) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if ( type == MEDIA_TYPE_VIDEO ) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    /**
     * This broadcast the gallery that an image is saved in file
     *
     * @param context
     */
    public static void broadCastGalery( Context context, File outputFile ) {

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ) {
            final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            final Uri contentUri = Uri.fromFile(outputFile);
            scanIntent.setData(contentUri);
            context.sendBroadcast(scanIntent);
        } else {
            final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://" + Environment.getExternalStorageDirectory()));
            context.sendBroadcast(intent);
        }
    }

    /**
     * @param activity
     * @param cameraId
     */
    public static void setCameraDisplayOrientation( Activity activity, int cameraId ) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();

        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch ( rotation ) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if ( info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        Log.i("GeneralHelper", "ROTATION VALUE : Device : " + rotation +
                "  Result Rotation : " + result);
        cameraDisplayOrientation = result;
    }

    /**
     * Rotate the image according to the display orientation
     *
     * @param data
     * @return
     */
    public static byte[] rotate( byte[] data ) {
        Matrix matrix = new Matrix();
        //Device.getOrientation() is used in order to support the emulator and an actual device
        matrix.postRotate(getCameraDisplayOrientation());
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if ( bitmap.getWidth() < bitmap.getHeight() ) {
            //no rotation needed
            return data;
        }
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true
        );
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, blob);
        byte[] bm = blob.toByteArray();

        return bm;
    }

    /**
     * Returns the camera Display orientation
     *
     * @return
     */
    public static int getCameraDisplayOrientation() {
        return cameraDisplayOrientation;
    }
}
