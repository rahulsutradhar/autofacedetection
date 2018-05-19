package test.in.mygate.cameraapp.util.ml;

import com.google.firebase.ml.vision.face.FirebaseVisionFace;

import java.util.List;

/**
 * Created by developers on 18/05/18.
 */

public interface FaceDetectedInFrame {

    void faceDetected( List<FirebaseVisionFace> firebaseVisionFaces );
}
