package com.dji.FPVDemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.camera.SettingsDefinitions;   //This class contains all the enums and setting classes for the DJI Camera.
import dji.common.camera.SystemState;             //This class provides general information and current status of the camera
import dji.common.error.DJIError;               //Class that handles all errors that are not handled by individual components
import dji.common.flightcontroller.FlightOrientationMode;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.product.Model;                 //Specifies all the supported products (Aircraft and Handheld)
import dji.common.useraccount.UserAccountState;   //Class used to manage the DJI account.
import dji.common.util.CommonCallbacks;         //Interfaces of common callbacks used to return results of asynchronous operations.
import dji.sdk.base.BaseProduct;                   //Abstract class for all DJI Products. Aircraft and HandHeld objects are subclasses of BaseProduct and can be accessed from getProduct in DJISDKManager. Additional components can be found in Aircraft and HandHeld that are unique to those products only.
import dji.sdk.camera.Camera;                   //This class contains the media manager and playback manager, which manage the camera's media content. It provides methods to change camera settings and perform camera actions. This object is available from the Aircraft or HandHeld object, which is a subclass of BaseProduct.
import dji.sdk.camera.MediaManager;
import dji.sdk.camera.VideoFeeder;           //Class that manages live video feed from DJI products to the mobile device.
import dji.sdk.codec.DJICodecManager;            //Class that handles encoding and decoding of media
import dji.sdk.flightcontroller.Compass;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.useraccount.UserAccountManager;   //Class used to manage the DJI account.

import static android.os.Environment.DIRECTORY_PICTURES;
import static dji.common.flightcontroller.FlightOrientationMode.*;


public class MainActivity extends Activity implements SurfaceTextureListener,OnClickListener{

    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    private ControlUav server;

    protected TextureView mVideoSurface = null;

    private  Button takeoffBtn,landBtn,sendBtn,controlBtn;

    private TextView recordingTime;


    private Handler handler;

    private String toConnect;

    private FlightController flightController;





    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent=getIntent();
        toConnect=intent.getStringExtra("iptoconnect");

        handler = new Handler();



        initUI();

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };

        Camera camera = FPVDemoApplication.getCameraInstance();

        if (camera != null) {

            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(SystemState cameraSystemState) {
                    if (null != cameraSystemState) {

                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;

                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideoRecording = cameraSystemState.isRecording();

                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                recordingTime.setText(timeString);

                                /*
                                 * Update recordingTime TextView visibility and mRecordBtn's check state
                                 */
                                if (isVideoRecording){
                                    recordingTime.setVisibility(View.VISIBLE);
                                }else
                                {
                                    recordingTime.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });

        }

    }

    protected void onProductChange() {
        initPreviewer();
        loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        showToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    @Override
    public void onResume() {
        //initPreviewer();

        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();
        initFlightController();


        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();

        super.onDestroy();
    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);

        recordingTime = (TextView) findViewById(R.id.timer);




        takeoffBtn = (Button) findViewById(R.id.btn_takeoff);
        landBtn = (Button) findViewById(R.id.btn_land);
        sendBtn=(Button) findViewById(R.id.btn_send);
        controlBtn=(Button)findViewById(R.id.btn_control);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        takeoffBtn.setOnClickListener(this);
        landBtn.setOnClickListener(this);
        sendBtn.setOnClickListener(this);
        controlBtn.setOnClickListener(this);

        recordingTime.setVisibility(View.INVISIBLE);



    }

    private void initPreviewer() {

        BaseProduct product = FPVDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
//        Aircraft aircraft=MApplication.getAircraftInstance();
//        FlightController flightController=aircraft.getFlightController();



        switch (v.getId()) {
            case R.id.btn_takeoff:{
                flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        //DialogUtils.showDialogBasedOnError(getContext(), djiError);

                    }
                });
                break;
            }
            case R.id.btn_land:{
                flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        //DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });
                break;
            }
            case R.id.btn_send: {

//                flightController.setFlightOrientationMode(AIRCRAFT_HEADING,null);
//                flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
//                //flightController.setFlightOrientationMode(FlightOrientationMode.AIRCRAFT_HEADING,null);
//
//                flightController.setVerticalControlMode(VerticalControlMode.POSITION);
//                flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
//                flightController.setYawControlMode(YawControlMode.ANGLE);
//               // flightController.setVirtualStickModeEnabled(true,null);
//
//
//                if(flightController.isVirtualStickControlModeAvailable()){
//                    Toast.makeText(getApplicationContext(), "切换到VirtualStick模式 " , Toast.LENGTH_SHORT).show();
//                }




                //flightController.sendVirtualStickFlightControlData(new FlightControlData(0f, 0f, 60f, 2f),null);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FTPClient ftp = new FTPClient();
                        try {
                            ftp.connect(toConnect,  8090);
                            ftp.login("user", "12345");
                            //ftp.login("anonymous", "");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                       // String mPath = Environment.getExternalStorageDirectory().toString()
                        //    + "/Pictures/" + "1.jpg";
                        FileInputStream input = null;
                        File file;

                         for (int i=1;i<10000;i++){
                             String mPath = Environment.getExternalStorageDirectory().toString()
                                     + "/Pictures/" + "1.jpg";
                             Bitmap bm = mVideoSurface.getBitmap();
//                             if(bm == null)
//                                 Log.e(TAG,"bitmap is null");


                             OutputStream fout = null;
                             File imageFile = new File(mPath);

                             try {
                                 fout = new FileOutputStream(imageFile);
                                 bm.compress(Bitmap.CompressFormat.JPEG, 90, fout);
                                 fout.flush();
                                 fout.close();
                             } catch (FileNotFoundException e) {
                                 Log.e(TAG, "FileNotFoundException");
                                 e.printStackTrace();
                             } catch (IOException e) {
                                 Log.e(TAG, "IOException");
                                 e.printStackTrace();
                             }
                            file = new File(mPath);
                            try {
                                input = new FileInputStream(file);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            try {
                                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                                //ftp.enterLocalPassiveMode();
                                if (!ftp.storeFile(i+".jpg", input)) {
                                    System.out.println("upload failed!");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            try {
                                input.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            int reply = ftp.getReplyCode();
                            if(!FTPReply.isPositiveCompletion(reply)) {
                                System.out.println("upload failed!");
                            }
                        }

                        try {
                            ftp.logout();
                            ftp.disconnect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }).start();

                break;
            }

            case R.id.btn_control:{
                server = new ControlUav(flightController);
                break;
            }
            default:
                break;
        }
    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode){

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        showToast("Switch Camera Mode Succeeded");
                    } else {
                        showToast(error.getDescription());
                    }
                }
            });
            }
            if(camera.isMediaDownloadModeSupported()){
                MediaManager mediaManager=camera.getMediaManager();

            }
    }

    // Method for taking photo
    private void captureAction(){

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {

            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.BURST; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null == djiError) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if (djiError == null) {
                                                showToast("take photo: success");
                                            } else {
                                                showToast(djiError.getDescription());
                                            }
                                        }
                                    });
                                }
                            }, 2000);
                        }
                    }
            });
        }
    }

    // Method for starting recording
    private void startRecord(){

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError)
                {
                    if (djiError == null) {
                        showToast("Record video: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord(){

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback(){

                @Override
                public void onResult(DJIError djiError)
                {
                    if(djiError == null) {
                        showToast("Stop recording: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }

    }

//    public void getBitmap(TextureView vv)
//    {
//        String mPath = Environment.getExternalStorageDirectory().toString()
//                + "/Pictures/" + "1.jpg";
//        Toast.makeText(getApplicationContext(), "Capturing Screenshot: " + mPath, Toast.LENGTH_SHORT).show();
//
//        Bitmap bm = vv.getBitmap();
//        if(bm == null)
//            Log.e(TAG,"bitmap is null");
//
//
//        OutputStream fout = null;
//        File imageFile = new File(mPath);
//
//        try {
//            fout = new FileOutputStream(imageFile);
//            bm.compress(Bitmap.CompressFormat.JPEG, 90, fout);
//            fout.flush();
//            fout.close();
//        } catch (FileNotFoundException e) {
//            Log.e(TAG, "FileNotFoundException");
//            e.printStackTrace();
//        } catch (IOException e) {
//            Log.e(TAG, "IOException");
//            e.printStackTrace();
//        }
//    }


//    private void uploadFile(){                         //单个照片传输可行，
//        new Thread(new Runnable() {
//            public void run() {
//                FtpClient ftp=new FtpClient();
//                ftp.connect();
//
//            }
//        }).start();
//    }


//     private void uploadFile(){                                  //1
//        new Thread(new Runnable() {
//            public void run() {
//                FTPClient ftp = new FTPClient();
//                try {
//                    ftp.connect("192.168.31.16",  8090);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//
//                try {
//                    ftp.login("user", "12345");
//                    //ftp.login("anonymous", "");
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                String mPath = Environment.getExternalStorageDirectory().toString()
//                        + "/Pictures/" + "1.jpg";
//                File file = new File(mPath);
//
//                FileInputStream input = null;
//                try {
//                    input = new FileInputStream(file);
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    ftp.setFileType(FTP.BINARY_FILE_TYPE);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                //ftp.enterLocalPassiveMode();
//                try {
//                    if (!ftp.storeFile("1.jpg", input)) {
//                        System.out.println("upload failed!");
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    input.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                int reply = ftp.getReplyCode();
//
//                if(!FTPReply.isPositiveCompletion(reply)) {
//                    System.out.println("upload failed!");
//                }
//
//                try {
//                    ftp.logout();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    ftp.disconnect();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }

//    private void uploadFile(){                      //2
//        new Thread(new Runnable() {
//            public void run() {
//                    FTPClient ftp = new FTPClient();
//                    try {
//                        ftp.connect("192.168.31.16",  8090);
//                        ftp.login("user", "12345");
//                        //ftp.login("anonymous", "");
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                    String mPath = Environment.getExternalStorageDirectory().toString()
//                        + "/Pictures/" + "1.jpg";
//                    FileInputStream input = null;
//                    File file;
//                for (int i=1;i<11;i++){
//                        getBitmap(mVideoSurface);
//                        file = new File(mPath);
//                        try {
//                            input = new FileInputStream(file);
//                        } catch (FileNotFoundException e) {
//                            e.printStackTrace();
//                        }
//                        try {
//                            ftp.setFileType(FTP.BINARY_FILE_TYPE);
//                            //ftp.enterLocalPassiveMode();
//                            if (!ftp.storeFile(i+".jpg", input)) {
//                                System.out.println("upload failed!");
//                            }
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//
//                        try {
//                            input.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        int reply = ftp.getReplyCode();
//                        if(!FTPReply.isPositiveCompletion(reply)) {
//                            System.out.println("upload failed!");
//                        }
//                    }
//
//                    try {
//                        ftp.logout();
//                        ftp.disconnect();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();
//    }

    private void initFlightController() {

        Aircraft aircraft = MApplication.getAircraftInstance();
        if (aircraft == null || !aircraft.isConnected()) {
            showToast("Disconnected");
            flightController = null;
            return;
        } else {
            flightController = aircraft.getFlightController();
            flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            flightController.setYawControlMode(YawControlMode.ANGLE);
            flightController.setVerticalControlMode(VerticalControlMode.POSITION);
            flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

            flightController.setFlightOrientationMode(FlightOrientationMode.AIRCRAFT_HEADING, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    showToast("Set FlightOrientation Mode to AIRCRAFT_HEADING");
                }
            });
            flightController.setTripodModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    showToast("Set TripodMode False");
                }
            });
            flightController.setTerrainFollowModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    showToast("set TerrainFollow Mode false");
                }
            });
            flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null){
                        showToast(djiError.getDescription());
                    }else
                    {
                        showToast("Enable Virtual Stick Success");
                    }
                }
            });

        }





    }
}
