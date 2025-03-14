package com.dji.FPVDemo;

/**
 * Created by Lenovo on 2018/11/12.
 */
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import android.graphics.Bitmap;
import android.util.Log;


public class Client extends Thread {

    private static final String TAG = Client.class.getSimpleName();
    private String mServerHost;// 服务器地址
    private int mServerPort;// 服务端口号
    //private String mSendMessage;// 要发送的消息
    //private  Bitmap bitmap;

    public Client(String serverHost, int serverPort) {
        // TODO Auto-generated constructor stub
        this.mServerHost = serverHost;
        this.mServerPort = serverPort;
        //this.mSendMessage = sendMessage;
        //this.bitmap=bitmap;
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        super.run();
        Socket socket = new Socket();
        try {
            Log.d(TAG, "请求连接到服务器...");
            SocketAddress socketAddress = new InetSocketAddress(mServerHost,
                    mServerPort);
            socket.connect(socketAddress, 0);// 设置目标地址,请求超时限制

            // 判断是否连接成功
            if (socket.isConnected()) {

                // 发送消息
                DataOutputStream dos = new DataOutputStream(
                        socket.getOutputStream());
                Log.d(TAG, "连接成功,开始发送消息,发送内容：" );


//                ByteArrayOutputStream bout=new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.PNG,80,bout);
//                dos.write(bout.toByteArray());


//                dos.writeUTF(mSendMessage);// 服务区/客户端双方的写/读方式要一直,否则会报错
                dos.flush();

                // 接收服务器消息
                DataInputStream dis = new DataInputStream(
                        socket.getInputStream());
                Log.d(TAG, "接收到服务器反馈消息：" + dis.readUTF());

                dos.close();
                dis.close();
            } else {
                Log.e(TAG, "未能成功连接至服务器！");
            }

        } catch (Exception e) {


            // TODO Auto-generated catch block
            e.printStackTrace();
            if (e instanceof SocketTimeoutException) {
                Log.e(TAG, "连接超时!");
            } else {
                Log.e(TAG, "通讯过程发生异常:" + e.toString());
            }
        }


//        finally {
//            try {
//                socket.close();
//                this.interrupt();
//                Log.d(TAG, "关闭连接.");
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
    }

}

