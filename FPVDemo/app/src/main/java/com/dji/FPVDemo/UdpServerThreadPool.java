package com.dji.FPVDemo;

import android.content.Context;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;

/**
 * Created by Lenovo on 2018/12/9.
 */

public class UdpServerThreadPool {
    private static final String TAG="UDPSocket";
    private static final int POOL_SIZE=5;
    private static final int BUFFER_LENGTH=1024;
    private byte[] receiveByte= new byte[BUFFER_LENGTH];


    protected DatagramSocket socket = null;
    protected boolean moreQuotes = true;

    private FlightController flightController;
    private float mPitch;
    private  float mRoll;
    private float mYaw;
    private float mThrottle;


    private Context mContext;
    private ExecutorService mThreadPool;
    private long lastReceiveTime = 0;


    public UdpServerThreadPool(Context context) {

        this.mContext = context;

        int cpuNumbers = Runtime.getRuntime().availableProcessors();
        // 根据CPU数目初始化线程池
        mThreadPool = Executors.newFixedThreadPool(cpuNumbers * POOL_SIZE);
        // 记录创建对象时的时间
        lastReceiveTime = System.currentTimeMillis();
    }

    public void receiveMessage() {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                while (moreQuotes) {
                    try {
                        byte[] buf = new byte[256];

                        // Receive request
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        socket.receive(packet);


                        String receiveString=new String(packet.getData());
                        String[] array=receiveString.split(",");
                        mPitch=Float.parseFloat(array[0]);
                        mRoll=Float.parseFloat(array[1]);
                        mYaw=Float.parseFloat(array[2]);
                        mThrottle=Float.parseFloat(array[3]);

                        buf = receiveString.getBytes();

                        // Send the response to the client at "address" and "port"
                        InetAddress address = packet.getAddress();
                        int port = packet.getPort();
                        packet = new DatagramPacket(buf, buf.length, address, port);
                        socket.send(packet);
                        flightController.sendVirtualStickFlightControlData(new FlightControlData(mPitch, mRoll, mRoll, mThrottle), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {

                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        moreQuotes = false;
                    }
                }
                socket.close();
            }
        });
    }

}
