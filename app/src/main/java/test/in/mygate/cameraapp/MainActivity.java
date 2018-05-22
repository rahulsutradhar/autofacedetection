package test.in.mygate.cameraapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

import test.in.mygate.cameraapp.ml.CameraMLActivity;
import test.in.mygate.cameraapp.util.GeneralHelper;

public class MainActivity extends AppCompatActivity {

    private Button cameraOpenButton, cameraMLButton;
    private static final int CODE_WRITE_SETTINGS_PERMISSION = 332;
    private static String[] PERMISSIONS_ALL = { Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE };
    private static final int PERMISSION_REQUEST_CODE = 223;
    private boolean normalCamera = false;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setViews();

    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if ( id == R.id.action_settings ) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setViews() {
        cameraOpenButton = (Button) findViewById(R.id.camera_button);
        cameraMLButton = (Button) findViewById(R.id.camera_button_ml);

        cameraOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                normalCamera = true;
                handleCameraButtonClick();
            }
        });

        cameraMLButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                normalCamera = false;
                handleCameraButtonClick();
            }
        });
    }

    private void handleCameraButtonClick() {
        if ( GeneralHelper.checkCameraHardware(getApplicationContext()) ) {
            //now take permission
            if ( checkRuntimePermission() ) {
                openCameraActivity();
            }

        } else {
            Toast.makeText(getApplicationContext(), "Camera is not available" +
                    " for this device", Toast.LENGTH_SHORT).show();
        }
    }


    public boolean checkRuntimePermission() {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {

            boolean allPermissionsGranted = true;
            ArrayList<String> toReqPermissions = new ArrayList<>();

            for ( String permission : PERMISSIONS_ALL ) {
                if ( ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED ) {
                    toReqPermissions.add(permission);

                    allPermissionsGranted = false;
                }
            }
            if ( allPermissionsGranted ) {
                //TODO Now some permissions are very special and require Settings Activity to launch, as u might have seen in some apps. handleWriteSettingsPermission() is an example for WRITE_SETTINGS permission. If u don't need very special permission(s), replace handleWriteSettingsPermission() with initActivity().
                return true;
            } else {
                ActivityCompat.requestPermissions(this,
                        toReqPermissions.toArray(new String[toReqPermissions.size()]), PERMISSION_REQUEST_CODE);
                return false;
            }
        } else {
            return true;
        }
    }

    private void openCameraActivity() {
        if ( normalCamera ) {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(MainActivity.this, CameraMLActivity.class);
            startActivity(intent);
        }

    }

    @Override
    public void onRequestPermissionsResult( int requestCode, String[] permissions, int[] grantResults ) {
        if ( requestCode == PERMISSION_REQUEST_CODE ) {
            boolean allPermGranted = true;
            for ( int i = 0; i < grantResults.length; i++ ) {
                if ( grantResults[i] != PackageManager.PERMISSION_GRANTED ) {
                    Log.v("TAG", "Permission NOT granted");
                    allPermGranted = false;
                    Toast.makeText(getApplicationContext(), "You need to allow permission", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
            if ( allPermGranted ) {
                openCameraActivity();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
