package test.in.mygate.cameraapp.util.ml;

import android.graphics.Rect;

/**
 * Created by developers on 20/05/18.
 */

public class Utils {

    /**
     * rectangle of the frame
     */
    private volatile static Rect photoFrame;

    /**
     * center distance of the previewScreen
     */
    private volatile static int frameCenterWidth;
    private volatile static int frameCenterHeight;

    /**
     * distance of the frame
     */
    private volatile static int frameDistanceLeft;
    private volatile static int frameDistanceTop;
    private volatile static int frameDistanceRight;
    private volatile static int frameDistanceBottom;

    /**
     * preview height and width
     */
    private volatile static int previewHeight;
    private volatile static int previewWidth;

    /**
     * Area of the Frame
     */
    private volatile static int photoFrameArea;

    /**
     * Minimum and maximum face Area
     */
    private volatile static int minFaceAreaRequired;
    private volatile static int maxFaceAreaRequired;

    /**************************
     * Getter Setter
     **************************/
    public static Rect getPhotoFrame() {
        return photoFrame;
    }

    public static void setPhotoFrame( Rect photoFrame ) {
        Utils.photoFrame = photoFrame;
    }

    public static int getFrameCenterWidth() {
        return frameCenterWidth;
    }

    public static void setFrameCenterWidth( int frameCenterWidth ) {
        Utils.frameCenterWidth = frameCenterWidth;
    }

    public static int getFrameCenterHeight() {
        return frameCenterHeight;
    }

    public static void setFrameCenterHeight( int frameCenterHeight ) {
        Utils.frameCenterHeight = frameCenterHeight;
    }

    public static int getFrameDistanceLeft() {
        return frameDistanceLeft;
    }

    public static void setFrameDistanceLeft( int frameDistanceLeft ) {
        Utils.frameDistanceLeft = frameDistanceLeft;
    }

    public static int getFrameDistanceTop() {
        return frameDistanceTop;
    }

    public static void setFrameDistanceTop( int frameDistanceTop ) {
        Utils.frameDistanceTop = frameDistanceTop;
    }

    public static int getFrameDistanceRight() {
        return frameDistanceRight;
    }

    public static void setFrameDistanceRight( int frameDistanceRight ) {
        Utils.frameDistanceRight = frameDistanceRight;
    }

    public static int getFrameDistanceBottom() {
        return frameDistanceBottom;
    }

    public static void setFrameDistanceBottom( int frameDistanceBottom ) {
        Utils.frameDistanceBottom = frameDistanceBottom;
    }

    public static int getPreviewHeight() {
        return previewHeight;
    }

    public static void setPreviewHeight( int previewHeight ) {
        Utils.previewHeight = previewHeight;
    }

    public static int getPreviewWidth() {
        return previewWidth;
    }

    public static void setPreviewWidth( int previewWidth ) {
        Utils.previewWidth = previewWidth;
    }

    public static int getPhotoFrameArea() {
        return photoFrameArea;
    }

    public static void setPhotoFrameArea( int photoFrameArea ) {
        Utils.photoFrameArea = photoFrameArea;
    }

    public static int getMinFaceAreaRequired() {
        return minFaceAreaRequired;
    }

    public static void setMinFaceAreaRequired( int minFaceAreaRequired ) {
        Utils.minFaceAreaRequired = minFaceAreaRequired;
    }

    public static int getMaxFaceAreaRequired() {
        return maxFaceAreaRequired;
    }

    public static void setMaxFaceAreaRequired( int maxFaceAreaRequired ) {
        Utils.maxFaceAreaRequired = maxFaceAreaRequired;
    }
}
