package test.in.mygate.cameraapp.util;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by developers on 15/05/18.
 */

public class AppConstant {

    public static int MAX_CAMERA_ZOOM_AVAILABLE = 0;
    public static float FOCAL_LENGHT = 1;

    //end points for face capture
    public volatile static int MIN_FACIAL_AREA = 0;
    public volatile static int MAX_FACIAL_AREA = 0;

    //bolean variable to lock the capture
    public static volatile boolean IS_FACE_DETECTED = false;
    public static volatile boolean LOCK_FRAME = true;

    //preview screen size of the camera
    public volatile static int WIDTH_PREVIEW = 0;
    public volatile static int HEIGHT_PREVIEW = 0;

    //area of the frame
    public volatile static int AREA_OF_FRAME = 0;

    //Define the constanct percent for the frame
    public static final int FRAME_WIDTH_PERCENT = 80;
    public static final int FRAME_HEIGHT_PERCENT = 60;

    //define optimal percentage of face required
    public static final int MIN_FACE_PERCENT = 28;
    public static final int MAX_FACE_PERCENT = 40;

}
