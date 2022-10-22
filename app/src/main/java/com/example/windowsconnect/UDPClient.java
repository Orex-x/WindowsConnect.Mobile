package com.example.windowsconnect;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class UDPClient {

    private DatagramSocket socket;
    private InetAddress address;
    private int _port;
    private String _host;

    public UDPClient(String host, int port) {
        _host = host;
        _port = port;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        try {
            address = InetAddress.getByName(_host);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message){
        new AsyncRequest().execute(message);
    }


    public void close() {
        socket.close();
    }

    private class AsyncRequest extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            byte[] msg = strings[0].getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet
                    = new DatagramPacket(msg, msg.length, address, _port);
            try {
                socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

}
