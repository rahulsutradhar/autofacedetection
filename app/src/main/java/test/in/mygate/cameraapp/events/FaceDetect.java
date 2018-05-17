package test.in.mygate.cameraapp.events;

/**
 * Created by developers on 16/05/18.
 */

public class FaceDetect {

    private int flagFaceDetect = 0;
    private volatile float area;

    public int getFlagFaceDetect() {
        return flagFaceDetect;
    }

    public void setFlagFaceDetect( int flagFaceDetect ) {
        this.flagFaceDetect = flagFaceDetect;
    }

    public float getArea() {
        return area;
    }

    public void setArea( float area ) {
        this.area = area;
    }
}
