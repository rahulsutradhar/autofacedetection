package test.in.mygate.cameraapp.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
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

import test.in.mygate.cameraapp.util.ml.Utils;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

/**
 * Created by developers on 14/05/18.
 */

public class GeneralHelper {

    private volatile static int cameraDisplayOrientation = 0;

    public static boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
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
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /**
     * Create a File for saving an image or video
     */
    public static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
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
    public static void broadCastGalery(Context context, File outputFile) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
    public static void setCameraDisplayOrientation(Activity activity, int cameraId) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();

        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
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
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
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
    public static Bitmap rotate(byte[] data) {
        Matrix matrix = new Matrix();
        //Device.getOrientation() is used in order to support the emulator and an actual device
        matrix.postRotate(getCameraDisplayOrientation());
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bitmap.getWidth() < bitmap.getHeight()) {
            //no rotation needed
            return bitmap;
        }
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true
        );
        /*ByteArrayOutputStream blob = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, blob);
        byte[] bm = blob.toByteArray();
*/
        return rotatedBitmap;
    }

    /**
     * Crop image
     *
     * @param rotatedImage
     * @return
     */
    public static byte[] cropImage(Bitmap rotatedImage, Rect faceRect) {

        int constF = 100;
        int widthBitmap = rotatedImage.getWidth();
        int heightBitmap = rotatedImage.getHeight();
        int x, y, h, w;

        //get the left start index
        if ((faceRect.left - constF) > 0) {
            x = faceRect.left - constF;
        } else {
            x = 0;
        }

        //get top start index
        if ((faceRect.top - constF) > 0) {
            y = faceRect.top - constF;
        } else {
            y = 0;
        }

        //get width
        if ((faceRect.right + constF) > widthBitmap) {
            w = widthBitmap;
        } else {
            w = faceRect.right + constF;
        }

        //get height
        if ((faceRect.bottom + constF) > heightBitmap) {
            h = heightBitmap;
        } else {
            h = faceRect.bottom + constF;
        }

        Log.d("GeneralHelper", "Face - " + faceRect + "\nNew rect " + x + ", " + y + ", " + w + ", " + h);

        Bitmap targetBitmap = cropImageBitmap(rotatedImage, x, y, w, h);

        if (targetBitmap == null) {
            Log.i("CROPIMAGE", "BITMAP IS NULL");
        }


        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        targetBitmap.compress(Bitmap.CompressFormat.JPEG, 100, blob);
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


    /**
     * This method crops the Source Bitmap to a new Bitmap (subset)
     *
     * @param srcBitmap    : source of the bitmap which need to be croped
     * @param startX       : starting distance from Left of the srcBitmap
     * @param startY       : starting distance from Top of the srcBitmap
     * @param targetWidth  : width of the new Bitmap that is need to be croped
     * @param targetHeight : height of the new Bitmap that is need to be croped
     * @return : null if conditions doesnot match else the Croped Bitmap
     */
    public static Bitmap cropImageBitmap(Bitmap srcBitmap, int startX, int startY,
                                         int targetWidth, int targetHeight) {

        if (srcBitmap == null) {
            return null;
        }

        int widthSrcBitmap = srcBitmap.getWidth();
        int heightSrcBitmap = srcBitmap.getHeight();

        //check if start left is a valid position in the srcBitmap
        if (startX < 0 || startX > widthSrcBitmap) {
            return null;
        }

        //check if start Top is a valid position in the srcBitmap
        if (startY < 0 || startY > heightSrcBitmap) {
            return null;
        }

        //check if desire crop width exist in the source srcBitmap
        int requireWidth = startX + targetWidth;
        if (requireWidth < 0 || requireWidth > widthSrcBitmap) {
            return null;
        }

        //check if desire height exist in source the srcBitmap
        int requireHeight = startY + targetHeight;
        if (requireHeight < 0 || requireHeight > heightSrcBitmap) {
            return null;
        }

        /**
         * Since all the condition is passed that means, desire co-ordinate exist in the bitmap
         * So, now we can create a subset of the SourceBitmap
         */
        Bitmap cropedBitmap = Bitmap.createBitmap(srcBitmap, startX, startY,
                targetWidth, targetHeight);

        return cropedBitmap;
    }


}
