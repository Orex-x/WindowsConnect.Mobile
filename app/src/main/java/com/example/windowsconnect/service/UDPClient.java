package com.example.windowsconnect.service;

import android.os.AsyncTask;

import com.example.windowsconnect.interfaces.UdpReceiveListDeviceFragmentListener;
import com.example.windowsconnect.interfaces.UdpReceiveMainActivityListener;
import com.example.windowsconnect.models.Command;
import com.example.windowsconnect.models.Host;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UDPClient {

    private int _port;
    private String _ip;
    private static UdpReceiveListDeviceFragmentListener udpReceiveListDeviceFragmentListener;
    public static void setUdpReceiveListDeviceFragmentListener(UdpReceiveListDeviceFragmentListener l){
        udpReceiveListDeviceFragmentListener = l;
    }

    public UDPClient(String ip, int port) {
        _ip = ip;
        _port = port;
    }


    public static void sendMessage(String message, String ip, String port){
        byte[] msg = message.getBytes(StandardCharsets.UTF_8);
        InetAddress address = null;
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(5000);
            address = InetAddress.getByName(ip);
            DatagramPacket packet
                    = new DatagramPacket(msg, msg.length, address, Integer.parseInt(port));
            socket.send(packet);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    public static void Receive(int port){
        new AsyncReceiveStatic().execute(port);
    }
    private static class AsyncReceiveStatic extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... strings) {
            int port = (int) strings[0];

            while (true){
                try {
                    DatagramSocket socket = new DatagramSocket(port);
                    byte[] message = new byte[8000];
                    DatagramPacket datagram = new DatagramPacket(message, message.length);
                    socket.receive(datagram);

                    String json = new String(message, StandardCharsets.UTF_8);
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        Map<String, Object> map = mapper.readValue(json, Map.class);
                        String command = map.get("command").toString();

                        switch (command){
                            case "setHostInfo":
                                if(udpReceiveListDeviceFragmentListener != null){
                                    Map<String, Object> mapHost = (Map<String, Object>) map.get("value");
                                    Host host = new Host();
                                    host.port = Integer.parseInt(mapHost.get("port").toString());
                                    host.localIP =  mapHost.get("localIP").toString();
                                    host.name =  mapHost.get("name").toString();
                                    host.macAddress =  mapHost.get("macAddress").toString();
                                    udpReceiveListDeviceFragmentListener.addHost(host);
                                }
                                break;
                        }
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void sendMessage(String message){
        new SendMessageThread(message).start();
    }

    class SendMessageThread extends Thread{
        String message;
        public SendMessageThread(String message){
            this.message = message;
        }
        @Override
        public void run() {
            try {
                byte[] msg = message.getBytes(StandardCharsets.UTF_8);

                DatagramSocket socket = new DatagramSocket(_port);
                InetAddress address = InetAddress.getByName(_ip);

                DatagramPacket packet = new DatagramPacket(msg, msg.length, address, _port);

                socket.send(packet);
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void sendMessage(byte[] data){
        new AsyncSendBytes().execute(data);
    }

    private class AsyncSendBytes extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... bytes) {
            try {
                DatagramSocket socket = new DatagramSocket(_port);
                InetAddress address = InetAddress.getByName(_ip);

                DatagramPacket packet = new DatagramPacket(bytes[0], bytes[0].length, address, _port);

                socket.send(packet);
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /*

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

    }
    private String[] GetMacDigits(String mac) { // парсим MAC
        return mac.split(String.valueOf(mac.contains("-") ? '-' : ':'));
    }

*/
}
