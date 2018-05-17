package test.in.mygate.cameraapp.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import test.in.mygate.cameraapp.events.FaceDetect;

import static android.content.ContentValues.TAG;

/**
 * Created by developers on 15/05/18.
 */

public class FaceDetectionListener implements Camera.FaceDetectionListener {

    private volatile FaceDetect faceDetect = new FaceDetect();
    Paint paint = new Paint();

    int index = 0;

    @Override
    public void onFaceDetection( final Camera.Face[] faces, Camera camera ) {
        Log.i(TAG, "onFaceDetection " + faces.length);

        if ( faces.length > 0 ) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "onFaceDetection Passed Initial" + AppConstant.IS_FACE_DETECTED);
                    if ( !AppConstant.IS_FACE_DETECTED ) {
                        Log.i(TAG, "onFaceDetection Passed" + AppConstant.IS_FACE_DETECTED);
                        AppConstant.IS_FACE_DETECTED = true;
                        calculateFacialArea(faces);
                    }
                }
            }, 2000);

        }
    }

    private synchronized void calculateFacialArea( final Camera.Face[] faces ) {
        Rect uRect = null;
        for ( int i = 0; i < faces.length; i++ ) {
            int left = faces[i].rect.left;
            int right = faces[i].rect.right;
            int top = faces[i].rect.top;
            int bottom = faces[i].rect.bottom;
            uRect = new Rect(left, top, right, bottom);

            int area = uRect.height() * uRect.width();
            Log.d("FaceDetection", "face detected: " + faces.length +
                    " Face 1 Location Left: " + left +
                    " Right: " + right + " Top: " + top + " Bottom: " + bottom + " Area: " + area);

            Log.i("FaceDetection", index++ + " Area of Face " + area);

            faceDetect.setArea(area);

            //pass the facial area and calculate for the zoom controls
            EventBus.getDefault().post(faceDetect);
        }

    }

    private void drawRectangle( Rect rect ) {
        Log.i("FaceDetection", "Rectangle Draw called");
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        Canvas canvas = new Canvas();
        canvas.drawRect(rect, paint);

    }
}
