package com.example.windowsconnect.service;

import android.os.AsyncTask;

import com.example.windowsconnect.interfaces.UdpReceiveListDeviceFragmentListener;
import com.example.windowsconnect.models.Host;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UDPClient {

    private String _ip;
    private boolean connected;
    private static UdpReceiveListDeviceFragmentListener udpReceiveListDeviceFragmentListener;
    public static void setUdpReceiveListDeviceFragmentListener(UdpReceiveListDeviceFragmentListener l){
        udpReceiveListDeviceFragmentListener = l;
    }

    public void setIp(String ip) {
        _ip = ip;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public UDPClient() {
        connected = false;
        Receive();
    }

    public void Receive(){
        new AsyncReceiveStatic().execute(Settings.UDP_LISTEN_PORT);
    }

    private static class AsyncReceiveStatic extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... strings) {
            int port = (int) strings[0];

            while (true){
                try {
                    byte[] lengthBuffer = new byte[4];
                    DatagramSocket socket = new DatagramSocket(port);
                    DatagramPacket packetLength = new DatagramPacket(lengthBuffer, lengthBuffer.length);
                    socket.receive(packetLength);

                    ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
                    byteBuffer.put(lengthBuffer);
                    byteBuffer.rewind();
                    int length = byteBuffer.getInt();

                    byte[] buffer = new byte[length];

                    DatagramPacket packetMessage = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packetMessage);

                    String json = new String(buffer, StandardCharsets.UTF_8);
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


    public String sendMessageWithReceive(String message){
        SendMessageThreadWithReceive task = new SendMessageThreadWithReceive(message);
        Thread thread = new Thread(task);
        thread.start();
        try {
            thread.join();
            return task.getValue();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    class SendMessageThreadWithReceive extends Thread{
        String message;
        private volatile String value;
        public SendMessageThreadWithReceive(String message){
            this.message = message;
        }
        @Override
        public void run() {
            try {
                byte[] lengthBuffer = new byte[4];
                byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

                DatagramSocket socket = new DatagramSocket(Settings.UDP_SEND_PORT);
                InetAddress address = InetAddress.getByName(_ip);

                //Send
                DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, address, Settings.UDP_SEND_PORT);
                socket.send(packet);

                //Receive
                DatagramPacket packetLength = new DatagramPacket(lengthBuffer, lengthBuffer.length);
                socket.receive(packetLength);

                ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
                byteBuffer.put(lengthBuffer);
                byteBuffer.rewind();
                int length = byteBuffer.getInt();

                byte[] buffer = new byte[length];

                DatagramPacket packetMessage = new DatagramPacket(buffer, buffer.length);
                socket.receive(packetMessage);

                value = new String(buffer, StandardCharsets.UTF_8);

                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public String getValue() {
            return value;
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

                DatagramSocket socket = new DatagramSocket(Settings.UDP_SEND_PORT);
                InetAddress address = InetAddress.getByName(_ip);

                DatagramPacket packet = new DatagramPacket(msg, msg.length, address, Settings.UDP_SEND_PORT);

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
                DatagramSocket socket = new DatagramSocket(Settings.UDP_SEND_PORT);
                InetAddress address = InetAddress.getByName(_ip);

                DatagramPacket packet = new DatagramPacket(bytes[0], bytes[0].length, address, Settings.UDP_SEND_PORT);

                socket.send(packet);
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    public void WakeUp(String macAddress){
        byte[] data = new byte[102];

        for (int i = 0; i <= 5; i++) // первые шесть байт - нулевые
            data[i] = (byte) 0xff;

        String[] macDigits = GetMacDigits(macAddress);
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

}
