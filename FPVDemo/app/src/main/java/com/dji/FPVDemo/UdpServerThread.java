package com.dji.FPVDemo;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;


/**
 * Created by Lenovo on 2018/12/9.
 */

public class UdpServerThread extends Thread{
    protected DatagramSocket socket = null;

    protected boolean moreQuotes = true;
    private boolean work=false;

    private FlightController flightController;
    private float mPitch;
    private  float mRoll;
    private float mYaw;
    private float mThrottle;



    public UdpServerThread(FlightController flightController) throws SocketException {
        super();
        this.flightController=flightController;
        socket = new DatagramSocket(7896);
    }
    @Override
    public void run() {

        while (moreQuotes) {
            try {
                byte[] buf = new byte[256];
                byte[] buf1 = new byte[256];
                work=flightController.isVirtualStickControlModeAvailable();
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
                buf1="roger".getBytes();
                // Send the response to the client at "address" and "port"
                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                packet = new DatagramPacket(buf1, buf1.length, address, port);
                socket.send(packet);

                if (work) {
                    packet = new DatagramPacket(buf, buf.length, address, port);
                    socket.send(packet);
                }
                flightController.sendVirtualStickFlightControlData(new FlightControlData(mPitch, mRoll, mRoll, mThrottle),null);
            } catch (IOException e) {
                e.printStackTrace();
                moreQuotes = false;
            }
        }
        socket.close();
    }



}
