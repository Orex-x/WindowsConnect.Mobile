package com.example.windowsconnect.service;

import android.os.AsyncTask;

import com.example.windowsconnect.interfaces.UdpReceiveListDeviceFragmentListener;
import com.example.windowsconnect.models.Command;
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
                        int command = Integer.parseInt(map.get("command").toString());

                        switch (command){
                            case Command.setHostInfo:
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


    public String sendMessageWithReceive(String message, int command){
        SendMessageThreadWithReceive task = new SendMessageThreadWithReceive(message, command);
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
        int command;
        private volatile String value;

        public SendMessageThreadWithReceive(String message, int command){
            this.message = message;
            this.command = command;
        }
        @Override
        public void run() {
            try {
                byte[] lengthBuffer = new byte[4];

                byte[] command_buffer = ByteBuffer.allocate(4).putInt(command).array();
                reverse(command_buffer);
                byte[] msg = UDPClient.join(command_buffer, message.getBytes(StandardCharsets.UTF_8));


                DatagramSocket socket = new DatagramSocket(Settings.UDP_SEND_PORT);
                InetAddress address = InetAddress.getByName(_ip);

                //Send
                DatagramPacket packet = new DatagramPacket(msg, msg.length, address, Settings.UDP_SEND_PORT);
                socket.send(packet);

                //Receive
                DatagramPacket packetLength = new DatagramPacket(lengthBuffer, lengthBuffer.length);
                socket.receive(packetLength);

                ByteBuffer byteBuffer = ByteBuffer.allocate(4);
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

    public void sendMessage(String message, int command){
        new SendMessageThread(message, command).start();
    }

    public void prepare(){
        new SendMessagePrepareThread().start();
    }

    public void close(){
        new SendMessageCloseThread().start();
    }

    public void sendMessageWithoutClose(byte[] message, int command){
        new SendMessageWithoutCloseThread(message, command).start();
    }

    public void sendMessageWithoutClose(byte[] message){
        reverse(message);
        new SendMessageWithoutCloseThread(message).start();
    }

    private DatagramSocket _socketWithoutClose;
    private InetAddress _addressWithoutClose;
    class SendMessagePrepareThread extends Thread{
        @Override
        public void run() {
            try {

                _socketWithoutClose = new DatagramSocket(Settings.UDP_SEND_PORT);
                _addressWithoutClose = InetAddress.getByName(_ip);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class SendMessageWithoutCloseThread extends Thread{
        byte[] message;
        int command;
        public SendMessageWithoutCloseThread(byte[] message, int command){
            this.message = message;
            this.command = command;
        }

        public SendMessageWithoutCloseThread(byte[] message){
            this.message = message;
        }
        @Override
        public void run() {
            try {

                //byte[] command_buffer = ByteBuffer.allocate(4).putInt(command).array();
               // reverse(command_buffer);
               // byte[] msg = UDPClient.join(command_buffer, message);

                DatagramPacket packet = new DatagramPacket(message, message.length, _addressWithoutClose, Settings.UDP_SEND_PORT);
                _socketWithoutClose.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class SendMessageCloseThread extends Thread{
        @Override
        public void run() {
            try {
                _socketWithoutClose.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class SendMessageThread extends Thread{
        String message;
        int command;
        public SendMessageThread(String message, int command){
            this.message = message;
            this.command = command;
        }
        @Override
        public void run() {
            try {
                byte[] command_buffer = ByteBuffer.allocate(4).putInt(command).array();
                reverse(command_buffer);
                byte[] msg = UDPClient.join(command_buffer, message.getBytes(StandardCharsets.UTF_8));

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

    // Метод объединения двух массивов в Java
    public static byte[] join(byte[] a, byte[] b)
    {
        byte[] c = new byte[a.length + b.length];

        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        return c;
    }

    public static void reverse(byte[] data) {
        for (int left = 0, right = data.length - 1; left < right; left++, right--) {
            // swap the values at the left and right indices
            byte temp = data[left];
            data[left]  = data[right];
            data[right] = temp;
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
