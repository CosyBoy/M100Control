package com.dji.FPVDemo;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.*;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;



/**
 * Created by Lenovo on 2018/12/5.
 */

public class ControlUav {
//    static final int socketServerPort=7896;
//    static final int socketClientPORT = 11000;
//    private  SendVirtualStickDataTask mSendVirtualStickDataTask;
//    private Timer mSendVirtualStickDataTimer;
//    DatagramSocket serverSocketUDP;
//
//    private FlightController mFlightController;
//    private float mPitch;
//    private  float mRoll;
//    private float mYaw;
//    private float mThrottle;
//
//    public  ControlUav(FlightController f){
//        this.mFlightController=f;
//        Thread serverThread=new Thread(new ServerThread());
//        serverThread.start();
//    }
//
//    private class ServerThread extends Thread{
//        InetAddress IPAddress;
//        private void enviarMensagemUDP(String mensagem){
//
//            String mensagemEnviada;
//            mensagemEnviada = "Recebido "+mensagem;
//
//            int msgTamanho = mensagemEnviada.length();
//            byte[] transmitido = mensagemEnviada.getBytes();
//            DatagramPacket p = new DatagramPacket(transmitido, msgTamanho, IPAddress, socketClientPORT);
//            try {
//
//                serverSocketUDP.send(p);
//                //teste git
//            }
//            catch(IOException e){
//
//            }
//        }
//        @Override
//        public void run(){
//            try {
//                serverSocketUDP=new DatagramSocket(socketServerPort);
//                byte[] buffer=new byte[1024];
//                while (true){
//                    DatagramPacket mDatagramPacket=new DatagramPacket(buffer,buffer.length);
//                    serverSocketUDP.receive(mDatagramPacket);
//
//                    enviarMensagemUDP("shoudao");
//
//                    String receiveString=new String(mDatagramPacket.getData());
//                    String[] array=receiveString.split(",");
//                    mPitch=Float.parseFloat(array[0]);
//                    mRoll=Float.parseFloat(array[1]);
//                    mYaw=Float.parseFloat(array[2]);
//                    mThrottle=Float.parseFloat(array[3]);
//                    if (mSendVirtualStickDataTimer==null){
//                        mSendVirtualStickDataTask=new SendVirtualStickDataTask();
//                        mSendVirtualStickDataTimer=new Timer();
//                        mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask,0);
//                    }
//                }
//            } catch (java.io.IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    class SendVirtualStickDataTask extends TimerTask{
//
//        @Override
//        public void run() {
//            if (mFlightController!=null){
//                mFlightController.sendVirtualStickFlightControlData(new FlightControlData(mPitch, mRoll, mRoll, mThrottle), new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(DJIError djiError) {
//
//                    }
//                });
//            }
//        }
//    }

    private static final String TAG = "ServerDrone";


    static final int socketServerPORT = 7896;
  //  static final int socketClientPORT = 11000;

    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    private Timer mSendVirtualStickDataTimer;

    //Socket
    DatagramSocket serverSocketUDP;

    private FlightController mFlightController;
    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;


    //construtor
    public ControlUav(FlightController f){
        this.mFlightController = f;
        Thread ServerThread = new Thread(new ServerThread());
        ServerThread.start();
    }

    private class ServerThread extends Thread {

        InetAddress IPAddress;
        int port;

        private void sendMessageUDP(String message){     //向pc发送信息

            String messagesend;
            messagesend = "roger "+message;

            int msgLength = messagesend.length();
            byte[] buf = messagesend.getBytes();
            DatagramPacket p = new DatagramPacket(buf, msgLength, IPAddress, port);
            try {

                serverSocketUDP.send(p);
            }
            catch(IOException e){

            }
        }

        private void takeOff(){
            if (mFlightController != null) {
                mFlightController.startTakeoff(
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {

                                } else {

                                }
                            }
                        }
                );
            }
        }

        private void startLand(){
            if (mFlightController != null){

                mFlightController.startLanding(
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {

                                } else {

                                }
                            }
                        }
                );

            }
        }

        @Override
        public void run() {

            try{

                serverSocketUDP = new DatagramSocket(socketServerPORT);

                //buffer
                byte[] buf = new byte[256];

                while(true){

                    Log.d(TAG, "wait pc send");

                    DatagramPacket packet = new DatagramPacket(buf, buf.length);

                    serverSocketUDP.receive(packet);

                    String receiveString = new String(packet.getData(), 0, packet.getLength());

                    IPAddress = packet.getAddress();
                    port = packet.getPort();
                    sendMessageUDP(receiveString);


                    receiveString=new String(packet.getData());
                    String[] array=receiveString.split(",");
                    mPitch=Float.parseFloat(array[0]);
                    mRoll=Float.parseFloat(array[1]);
                    mYaw=Float.parseFloat(array[2]);
                    mThrottle=Float.parseFloat(array[3]);
//                    switch (receiveString){
//
//                        case "takeoff":
//                            takeOff();
//                            break;
//
//                        case "startland":
//                            startLand();
//                            break;
//
//                        case "up":
//                            mPitch = 0f;
//                            mRoll = 0f;
//                            mYaw = 0f;
//                            mThrottle = 2f;
//                            break;
//
//                        case "down":
//                            mPitch = 0f;
//                            mRoll = 0f;
//                            mYaw = 0f;
//                            mThrottle = -2f;
//                            break;
//
//
//                    }

                    if (null == mSendVirtualStickDataTimer) {
                        mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                        mSendVirtualStickDataTimer = new Timer();
                        mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
                    }

                }

            }catch(IOException e){

            }

        }

    }

    class SendVirtualStickDataTask extends TimerTask {

        @Override
        public void run() {

            if (mFlightController != null) {
                mFlightController.sendVirtualStickFlightControlData(
                        new FlightControlData(
                                mPitch, mRoll, mYaw, mThrottle
                        ), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                Log.d(TAG, "Dados enviados!");
                            }
                        }
                );
            }
        }
    }
}
