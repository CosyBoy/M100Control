package com.dji.FPVDemo;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import static android.content.ContentValues.TAG;

public class FtpClient  extends Thread{

//    private final String hostname = "ftp.dlptest.com"; //"192.168.1.119";
//    private final String userName = "anonymous";
//    private final String password = "";
//    private String filePath;
//    private FTPClient ftpClient = null;
//
//    private boolean login = false;
//    boolean storeFile = false;
//
//    public FtpClient(String filePath) {
//        this.filePath = filePath;
//    }
//
    private TextureView textureView=null;

    public void FtpClient(TextureView textureView){
        this.textureView=textureView;
    }

    public  void  run(){
        FTPClient ftp = new FTPClient();
        try {
            ftp.connect("192.168.31.16",  8090);
            ftp.login("user", "12345");
            //ftp.login("anonymous", "");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String mPath = Environment.getExternalStorageDirectory().toString()
                + "/Pictures/" + "1.jpg";
        FileInputStream input = null;
        File file;
        for (int i=1;i<11;i++){
            getBitmap(textureView);
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
    public void getBitmap(TextureView vv)
    {
        String mPath = Environment.getExternalStorageDirectory().toString()
                + "/Pictures/" + "1.jpg";
        //Toast.makeText(getApplicationContext(), "Capturing Screenshot: " + mPath, Toast.LENGTH_SHORT).show();

        Bitmap bm = vv.getBitmap();
        if(bm == null)
            Log.e(TAG,"bitmap is null");


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
    }


    }




//    public void connect() {
//        FTPClient ftp = new FTPClient();
//        try {
//            ftp.connect("192.168.31.16",  8090);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//        try {
//            ftp.login("user", "12345");
//            //ftp.login("anonymous", "");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        String mPath = Environment.getExternalStorageDirectory().toString()
//                + "/Pictures/" + "1.jpg";
//        File file = new File(mPath);
//
//        FileInputStream input = null;
//        try {
//            input = new FileInputStream(file);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        try {
//            ftp.setFileType(FTP.BINARY_FILE_TYPE);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        //ftp.enterLocalPassiveMode();
//        try {
//            if (!ftp.storeFile("1.jpg", input)) {
//                System.out.println("upload failed!");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try {
//            input.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        int reply = ftp.getReplyCode();
//
//        if(!FTPReply.isPositiveCompletion(reply)) {
//            System.out.println("upload failed!");
//        }
//
//        try {
//            ftp.logout();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try {
//            ftp.disconnect();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//    }




