package com.dji.FPVDemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
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

import java.io.BufferedInputStream;
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

import dji.common.battery.BatteryState;
import dji.common.camera.SettingsDefinitions;   //This class contains all the enums and setting classes for the DJI Camera.
import dji.common.camera.SystemState;             //This class provides general information and current status of the camera
import dji.common.error.DJIError;               //Class that handles all errors that are not handled by individual components
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightOrientationMode;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.gimbal.GimbalMode;
import dji.common.gimbal.GimbalState;
import dji.common.product.Model;                 //Specifies all the supported products (Aircraft and Handheld)
import dji.common.useraccount.UserAccountState;   //Class used to manage the DJI account.
import dji.common.util.CommonCallbacks;         //Interfaces of common callbacks used to return results of asynchronous operations.
import dji.sdk.base.BaseProduct;                   //Abstract class for all DJI Products. Aircraft and HandHeld objects are subclasses of BaseProduct and can be accessed from getProduct in DJISDKManager. Additional components can be found in Aircraft and HandHeld that are unique to those products only.
import dji.sdk.camera.Camera;                   //This class contains the media manager and playback manager, which manage the camera's media content. It provides methods to change camera settings and perform camera actions. This object is available from the Aircraft or HandHeld object, which is a subclass of BaseProduct.

import dji.sdk.camera.VideoFeeder;           //Class that manages live video feed from DJI products to the mobile device.
import dji.sdk.codec.DJICodecManager;            //Class that handles encoding and decoding of media
import dji.sdk.flightcontroller.Compass;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;
import dji.sdk.useraccount.UserAccountManager;   //Class used to manage the DJI account.



import static android.os.Environment.DIRECTORY_PICTURES;
import static dji.common.flightcontroller.FlightOrientationMode.*;



public class MainActivity extends Activity implements SurfaceTextureListener,OnClickListener{

    private static final String TAG = MainActivity.class.getName();
//    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;    //4.6 works
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;    //4.8.1  ?


    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    private ControlUav server;

    protected TextureView mVideoSurface = null;

    private  Button takeoffBtn,landBtn,sendBtn,controlBtn,stopBtn;

    private TextView recordingTime;


    private Handler handler;

    private String toConnect;

    private FlightController flightController;

    private Gimbal gimbal;


    private UavStatusInfo DS = null;

    private TextView t,text_change,text_voltage,text_current,text_temp,text_getcharge_status,text_longitude,text_latitude,text_altitude,text_isflying,text_connect_status,text_velocity_x,text_velocity_y,text_velocity_z,text_gimbal_roll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DS = new UavStatusInfo();
        DS.setDrone_id("m100");
        DS.setConnect_status(1);

        Intent intent=getIntent();
        toConnect=intent.getStringExtra("iptoconnect");
        handler = new Handler();


        initUI();
        initFlightController();

        // The callback for receiving the raw H264 video data for camera live view

//        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {        //4.6 works
//
//            @Override
//            public void onReceive(byte[] videoBuffer, int size) {
//                if (mCodecManager != null) {
//                    mCodecManager.sendDataToDecoder(videoBuffer, size);
//                }
//            }
//        };
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {     //4.8.1 ?

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };

//        Camera camera = FPVDemoApplication.getCameraInstance();
//
//        if (camera != null) {
//
//            camera.setSystemStateCallback(new SystemState.Callback() {
//                @Override
//                public void onUpdate(SystemState cameraSystemState) {
//                    if (null != cameraSystemState) {
//
//                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
//                        int minutes = (recordTime % 3600) / 60;
//                        int seconds = recordTime % 60;
//
//                        final String timeString = String.format("%02d:%02d", minutes, seconds);
//                        final boolean isVideoRecording = cameraSystemState.isRecording();
//
//                        MainActivity.this.runOnUiThread(new Runnable() {
//
//                            @Override
//                            public void run() {
//
//                                recordingTime.setText(timeString);
//
//                                /*
//                                 * Update recordingTime TextView visibility and mRecordBtn's check state
//                                 */
//                                if (isVideoRecording){
//                                    recordingTime.setVisibility(View.VISIBLE);
//                                }else
//                                {
//                                    recordingTime.setVisibility(View.INVISIBLE);
//                                }
//                            }
//                        });
//                    }
//                }
//            });
//
//        }

    }

    protected void onProductChange() {
        initPreviewer();
        //loginAccount();
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

        //initFlightController();


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
        stopBtn=(Button)findViewById(R.id.btn_switch);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        takeoffBtn.setOnClickListener(this);
        landBtn.setOnClickListener(this);
        sendBtn.setOnClickListener(this);
        controlBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);

        recordingTime.setVisibility(View.INVISIBLE);


        t=(TextView) findViewById(R.id.textt);
        text_change=(TextView) findViewById(R.id.text_change);
        text_voltage=(TextView) findViewById(R.id.text_voltage);
        text_current=(TextView) findViewById(R.id.text_current);
        text_temp=(TextView) findViewById(R.id.text_temp);
        text_getcharge_status=(TextView) findViewById(R.id.text_getcharge_status);
        text_longitude=(TextView) findViewById(R.id.text_longitude);
        text_latitude=(TextView) findViewById(R.id.text_latitude);
        text_altitude=(TextView) findViewById(R.id.text_altitude);
        text_isflying=(TextView) findViewById(R.id.text_isflying);
        text_connect_status=(TextView) findViewById(R.id.text_connect_status);

        text_velocity_x= (TextView)findViewById(R.id.text_velocity_x);
        text_velocity_y= (TextView)findViewById(R.id.text_velocity_y);
        text_velocity_z= (TextView)findViewById(R.id.text_velocity_z);

        text_gimbal_roll=(TextView)findViewById(R.id.text_gimbal_roll);

        t.setText("\n无人机实时状态参数显示\n");




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
//                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);//4.6 works
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);  //4.8.1  ?
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback

            //VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);   //4.6 works
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null); //4.8.1 ?
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
                showToast("起飞！");
                break;
            }
            case R.id.btn_land:{
                flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        //DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });
                showToast("降落！");
                break;
            }


            case R.id.btn_send: {

                showToast("开始发送数据！");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FTPClient ftp = new FTPClient();
                        try {
                            ftp.connect(toConnect,  8090);
                            ftp.login("user", "12345");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }



                        for (int i=1,j=1;i<10000;i++){

                            if(i%10!=0){                       //? 2019.1.10  11:35
                                j=i%10;
                            }

                            String mPath = Environment.getExternalStorageDirectory().toString()  //检测失真是否时在存入sd卡时产生
                                    + "/Pictures/" +j +".jpg";



                            FileInputStream input = null;
                            File file;

                            Bitmap bm = mVideoSurface.getBitmap();
//                             if(bm == null)
//                                 Log.e(TAG,"bitmap is null");


                            FileOutputStream fout = null;
                            File imageFile = new File(mPath);

                            try {
                                fout = new FileOutputStream(imageFile);
                                bm.compress(Bitmap.CompressFormat.JPEG, 100, fout);
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

                            BufferedInputStream in = new BufferedInputStream(input);
                            try {
                                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                                ftp.setFileTransferMode(FTP.BINARY_FILE_TYPE);
                              //  ftp.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);  //19.1.7




                                //ftp.enterLocalPassiveMode();
                                if (!ftp.storeFile(i+".jpg", in)) {
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
//            case R.id.btn_send: {
//
//                showToast("开始发送数据！");
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        FTPClient ftp = new FTPClient();
//                        try {
//                            ftp.connect(toConnect,  8090);
//                            ftp.login("user", "12345");
//                            //ftp.login("anonymous", "");
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//
//                       // String mPath = Environment.getExternalStorageDirectory().toString()
//                        //    + "/Pictures/" + "1.jpg";
//                        FileInputStream input = null;
//                        File file;
//
//                         for (int i=1;i<10000;i++){
//                             String mPath = Environment.getExternalStorageDirectory().toString()
//                                    + "/Pictures/" + "1.jpg";

//
//                             Bitmap bm = mVideoSurface.getBitmap();
////                             if(bm == null)
////                                 Log.e(TAG,"bitmap is null");
//
//
//                             OutputStream fout = null;
//                             File imageFile = new File(mPath);
//
//                             try {
//                                 fout = new FileOutputStream(imageFile);
//                                 bm.compress(Bitmap.CompressFormat.JPEG, 100, fout);
//                                 fout.flush();
//                                 fout.close();
//                             } catch (FileNotFoundException e) {
//                                 Log.e(TAG, "FileNotFoundException");
//                                 e.printStackTrace();
//                             } catch (IOException e) {
//                                 Log.e(TAG, "IOException");
//                                 e.printStackTrace();
//                             }
//                            file = new File(mPath);
//                            try {
//                                input = new FileInputStream(file);
//                            } catch (FileNotFoundException e) {
//                                e.printStackTrace();
//                            }
//                            try {
//                                ftp.setFileType(FTP.BINARY_FILE_TYPE);
//                                ftp.setFileTransferMode(FTP.BINARY_FILE_TYPE);
//                                //ftp.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);  //19.1.7
//
//
//                                //ftp.enterLocalPassiveMode();
//                                if (!ftp.storeFile(i+".jpg", input)) {
//                                    System.out.println("upload failed!");
//                                }
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//
//                            try {
//
//                                input.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                            int reply = ftp.getReplyCode();
//                            if(!FTPReply.isPositiveCompletion(reply)) {
//                                System.out.println("upload failed!");
//                            }
//                        }
//
//                        try {
//                            ftp.logout();
//                            ftp.disconnect();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//
//                    }
//                }).start();
//
//                break;
//            }

            case R.id.btn_control:{
                showToast("接收指令！");
                server = new ControlUav(flightController);
                break;
            }

            case R.id.btn_switch:{
                showToast("紧急停止启动！");
                if (server!=null)              //方案一，使用volatile
                    server.changebool();


                break;
            }

            default:
                break;
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
            //showToast("Disconnected");
            flightController = null;
            return;
        } else {
            aircraft.getBattery().setStateCallback(new BatteryState.Callback() {
                @Override
                public void onUpdate(BatteryState djiBatteryState) {
                    DS.setCharge(djiBatteryState.getChargeRemainingInPercent());
                    DS.setVoltage(djiBatteryState.getVoltage());
                    DS.setCurrent(djiBatteryState.getCurrent());
                    DS.setTemperature(djiBatteryState.getTemperature());
                    DS.setCharge_status(djiBatteryState.getCurrent() > 0 ? 1 : 0);
                    updateUI();
                }
            });

            flightController = aircraft.getFlightController();

            if (flightController != null) {
                flightController.setStateCallback(
                        new FlightControllerState.Callback() {
                            @Override
                            public void onUpdate(FlightControllerState
                                                         djiFlightControllerCurrentState) {
                                DS.setIsflying(djiFlightControllerCurrentState.isFlying());
                                DS.setAltitude(djiFlightControllerCurrentState.getAircraftLocation().getAltitude());
                                DS.setLatitude(djiFlightControllerCurrentState.getAircraftLocation().getLatitude());
                                DS.setLongitude(djiFlightControllerCurrentState.getAircraftLocation().getLongitude());

                                DS.setVelocityX(djiFlightControllerCurrentState.getVelocityX());
                                DS.setVelocityY(djiFlightControllerCurrentState.getVelocityY());
                                DS.setVelocityZ(djiFlightControllerCurrentState.getVelocityZ());

                                updateUI();
                            }
                        });
            }

            flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            flightController.setYawControlMode(YawControlMode.ANGLE);
            flightController.setVerticalControlMode(VerticalControlMode.POSITION);
            flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

            flightController.setFlightOrientationMode(FlightOrientationMode.AIRCRAFT_HEADING, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    //showToast("Set FlightOrientation Mode to AIRCRAFT_HEADING");
                }
            });
            flightController.setTripodModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    //showToast("Set TripodMode False");
                }
            });
            flightController.setTerrainFollowModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    //showToast("set TerrainFollow Mode false");
                }
            });
            flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null){
                        //showToast(djiError.getDescription());
                    }else
                    {
                        //showToast("Enable Virtual Stick Success");
                    }
                }
            });

            gimbal = aircraft.getGimbal();
            if(gimbal!=null){
                gimbal.setStateCallback(
                        new GimbalState.Callback() {
                            @Override
                            public void onUpdate(@NonNull GimbalState gimbalState) {
                                DS.setRollFineTuneInDegrees(gimbalState.getRollFineTuneInDegrees());
                            }
                        }
                );
            }
            gimbal.setMode(GimbalMode.YAW_FOLLOW,null);
        }





    }
    private void updateUI(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text_change.setText("电池电量： "+DS.getCharge()+"%");
                text_voltage.setText("当前电压： "+DS.getVoltage()+"mV");
                text_current.setText("当前电流： "+DS.getCurrent()+"mA");
                text_temp.setText("电池温度： "+DS.getTemperature()+"摄氏度");
                text_getcharge_status.setText("充电状态： "+(DS.getCharge_status()==1?"充电中":"放电中"));
                text_longitude.setText("经度："+DS.getLongitude());
                text_latitude.setText("纬度："+DS.getLatitude());
                text_altitude.setText("飞行高度： "+DS.getAltitude()+"米");
                text_isflying.setText("飞行状态： "+(DS.getIsflying()==true?"正在飞行中":"静止中"));

                text_velocity_x.setText("x轴速度： "+DS.getVelocityX());
                text_velocity_y.setText("y轴速度： "+DS.getVelocityY());
                text_velocity_z.setText("z轴速度： "+DS.getVelocityZ());

                text_gimbal_roll.setText("云台roll的偏转"+DS.getRollFineTuneInDegrees());
            }
        });
    }




}
