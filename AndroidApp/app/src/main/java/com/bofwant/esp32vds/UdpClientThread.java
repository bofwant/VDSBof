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
            sendState(context.getResources().getString(R.string.tag_conected));

            // get response
            buf = new byte[256];
            packet = new DatagramPacket(buf, buf.length);

            Log.d("udp","waiting");
            socket.receive(packet);
            String line = new String(packet.getData(), 0, packet.getLength());

            handler.sendMessage(
                    Message.obtain(handler, MainActivity.UdpClientHandler.UPDATE_MSG, line));

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