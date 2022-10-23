package com.example.windowsconnect;

import android.os.AsyncTask;

import com.example.windowsconnect.models.Host;

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
    private Host _host;

    public UDPClient(Host host) {
        _host = host;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        try {
            address = InetAddress.getByName(_host.localIP);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message){
        new AsyncSendMessage().execute(message);
    }

    public void WakeUp(){
        byte[] data = new byte[102];

        for (int i = 0; i <= 5; i++) // первые шесть байт - нулевые
            data[i] = (byte) 0xff;

        String[] macDigits = GetMacDigits(_host.macAddress);
        if (macDigits.length != 6){
            return;
        }

        final int start = 6;
        for (int i = 0; i < 16; i++) // создаем нужную последовательность байт для пакета
            for (int x = 0; x < 6; x++)
                data[start + i * 6 + x] = (byte) Integer.parseInt(macDigits[x], 16);

        new AsyncSendBytes().execute(data);
    }

    private String[] GetMacDigits(String mac) // парсим MAC
    {
        return mac.split(String.valueOf(mac.contains("-") ? '-' : ':'));
    }

    public void close() {
        socket.close();
    }

    private class AsyncSendMessage extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            byte[] msg = strings[0].getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet
                    = new DatagramPacket(msg, msg.length, address, _host.port);
            try {
                socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private class AsyncSendBytes extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... bytes) {

            DatagramPacket packet
                    = new DatagramPacket(bytes[0], bytes[0].length, address, _host.port);
            try {
                socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }



}
