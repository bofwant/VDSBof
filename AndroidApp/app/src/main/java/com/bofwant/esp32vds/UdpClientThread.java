package com.bofwant.esp32vds;

import android.content.Context;
import android.os.Debug;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UdpClientThread extends Thread{

    String dstAddress;
    int dstPort;
    private boolean running;
    MainActivity.UdpClientHandler handler;



    DatagramSocket socket;
    InetAddress address;
    Context context;

    public UdpClientThread(String addr, int port, MainActivity.UdpClientHandler handler, Context context) {
        super();
        dstAddress = addr;
        dstPort = port;
        this.handler = handler;
        this.context=context;

    }

    public void setRunning(boolean running){
        this.running = running;
    }

    private void sendState(String state){
        handler.sendMessage(
                Message.obtain(handler,
                        MainActivity.UdpClientHandler.UPDATE_STATE, state));
    }

    @Override
    public void run() {
        sendState("connecting...");
        ByteBuffer byteBuffer=ByteBuffer.allocate(4000);// 4000 byte buffer
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        running = true;

        try {
            Log.d("udp","starting socket");
            socket = new DatagramSocket(dstPort);

            address = InetAddress.getByName(dstAddress);

            // start request
            byte[] buf = context.getResources().getString(R.string.esp_start).getBytes();
            DatagramPacket packet =
                    new DatagramPacket(buf, buf.length, address, dstPort);
            socket.send(packet);
            Log.d("udp",context.getResources().getString(R.string.esp_start)+" sent");
            //sendState(context.getResources().getString(R.string.tag_conected));
            // get response
            while(running){
                buf = new byte[2000];
                packet = new DatagramPacket(buf, buf.length);

                Log.d("udp","waiting");
                socket.receive(packet);
                Log.d("udp packet",String.valueOf(packet.getLength()));

                if(packet.getLength()<200){
                    String line = new String(packet.getData(), 0, packet.getLength());
                    if(line.equals("start")){
                        sendState(context.getResources().getString(R.string.tag_conected));
                    }
                    handler.sendMessage(Message.obtain(handler, MainActivity.UdpClientHandler.UPDATE_MSG, line));

                }else{
                    byteBuffer.clear();
                    byteBuffer.put(packet.getData());
                    for (int i=0;i<packet.getLength();i=i+2){
                        Log.d("udp read","number i="+String.valueOf(i/2)+" value="+String.valueOf(byteBuffer.getShort(i)));
                    }

                }

            }

            buf = context.getResources().getString(R.string.esp_stop).getBytes();
            packet = new DatagramPacket(buf, buf.length, address, dstPort);
            socket.send(packet);
            Log.d("udp",context.getResources().getString(R.string.esp_stop)+" sent");
            sendState(context.getResources().getString(R.string.tag_disconected));


        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(socket != null){
                socket.close();
                handler.sendEmptyMessage(MainActivity.UdpClientHandler.UPDATE_END);
            }
        }

    }
}