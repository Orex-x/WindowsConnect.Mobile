package com.example.windowsconnect.service;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.example.windowsconnect.interfaces.ITCPClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TCPClient {
    private static ITCPClient _tcpClientListener;

    private Socket _clientSocket = null;
    private OutputStream _outputStream = null;
    private InputStream _inputStream = null;
    private BufferedReader _bufferedReader = null;
    private ServerSocket _serverSocket = null;


    public TCPClient(ITCPClient l, String ip) {
        _tcpClientListener = l;
        new TCPRegister(ip).start();

    }


    public void sendMessage(byte[] data) {
        new TCPSendMessageThread(data).start();
    }

    public void sendMessage(String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        new TCPSendMessageThread(data).start();
    }

    public void sendMessage(InputStream stream, long l) {
        new TCPSendMessageThreadStream(stream, l).start();
    }

    public String sendMessageWithReceived(byte[] data) {
        TCPSendMessageWithRespondThread tcp = new TCPSendMessageWithRespondThread(data);
        Thread thread = new Thread(tcp);
        thread.start();
        try {
            thread.join();
            return tcp.getValue();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public String sendMessageWithReceived(String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        TCPSendMessageWithRespondThread t = new TCPSendMessageWithRespondThread(data);
        Thread thread = new Thread(t);
        thread.start();
        try {
            thread.join();
            return t.getValue();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }


    public class TCPRegister extends Thread {

        private String ip;

        public TCPRegister(String ip) {
            this.ip = ip;
        }

        @Override
        public void run() {
            try {
                _clientSocket = new Socket(ip, Settings.TCP_PORT);
                _outputStream = _clientSocket.getOutputStream();
                _inputStream = _clientSocket.getInputStream();
                new TCPReceivedThread().start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public class TCPReceivedThread extends Thread {
        @Override
        public void run() {
            byte[] headerBuffer = new byte[4];
            while (true) {
                try {
                    int bytesReceived =  _inputStream.read(headerBuffer, 0, 4);
                    if (bytesReceived != 4)
                        continue;

                    ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
                    byteBuffer.put(headerBuffer);
                    byteBuffer.rewind();
                    int length = byteBuffer.getInt();

                    byte[] buffer = new byte[length];

                    int count = 0;
                    do
                    {
                        bytesReceived = _inputStream.read(buffer, count, buffer.length - count);
                        count += bytesReceived;
                        if(count > length) break;
                    }
                    while (count != length);

                    String json = new String(buffer, StandardCharsets.UTF_8);

                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        Map<String, Object> map = mapper.readValue(json, Map.class);
                        String command = map.get("command").toString();

                        switch (command) {
                            case "setWallpaper":
                                if (_tcpClientListener != null) {
                                    String dataString = map.get("value").toString();
                                    _tcpClientListener.setWallPaper(dataString);
                                }
                                break;
                        }
                    } catch (IOException e) {
                        System.out.println("Пришло сообщение не json вида: " + json);
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class TCPSendMessageThread extends Thread {
        byte[] data;

        public TCPSendMessageThread(byte[] data){
            this.data = data;
        }

        @Override
        public void run() {
            super.run();
            byte[] packet_length  = ByteBuffer.allocate(4).putInt(data.length).array();
            try {
                _outputStream.write(packet_length, 0, packet_length.length);
                _outputStream.write(data, 0, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private class TCPSendMessageThreadStream extends Thread {
        InputStream stream;
        long length;
        public TCPSendMessageThreadStream(InputStream stream, long length){
             this.stream = stream;
             this.length = length;
        }

        @Override
        public void run() {
            super.run();
            byte[] packet_length  = ByteBuffer.allocate(8).putLong(length).array();
            try {
                _outputStream.write(packet_length, 0, packet_length.length);

                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];

                int len;
                long count = 0;
                while ((len = stream.read(buffer)) != -1) {
                    _outputStream.write(buffer, 0, len);
                    _tcpClientListener.setProgressUploadFile((int)getProgress(length, count));
                    count += len;
                }
                _tcpClientListener.setProgressUploadFile(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public long getProgress(long sum, long value)
        {
            return value * 100 / sum;
        }
    }

    public class TCPSendMessageWithRespondThread implements Runnable {
        private volatile String value;
        byte[] data;
        public TCPSendMessageWithRespondThread(byte[] data){
            this.data = data;
        }

        @Override
        public void run() {
            byte[] headerBuffer = new byte[4];
            byte[] packet_length  = ByteBuffer.allocate(4).putInt(data.length).array();
            try {
                _outputStream.write(packet_length, 0, packet_length.length);
                _outputStream.write(data, 0, data.length);
                _outputStream.flush();
                /*int bytesReceived =  _inputStream.read(headerBuffer, 0, 4);
                if (bytesReceived != 4)
                    return;

                ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
                byteBuffer.put(headerBuffer);
                byteBuffer.rewind();
                int length = byteBuffer.getInt();

                byte[] buffer = new byte[length];
                int count = 0;
                do
                {
                    bytesReceived = _inputStream.read(buffer, count, buffer.length - count);
                    count += bytesReceived;
                    if(count > length) break;
                }
                while (count != length);

                value = new String(buffer, StandardCharsets.UTF_8);*/
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getValue() {
            return value;
        }
    }







/*

    public static void sendMessage(byte[] data, String ip){
        new TCPSendMessageThread(data, ip).start();
    }

    public static void sendMessage(String message, String ip){
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        new TCPSendMessageThread(data, ip).start();
    }

    public static String sendMessageWithRespond(byte[] data, String ip){
        TCPSendMessageWithRespondThread tcp = new TCPSendMessageWithRespondThread(data, ip);
        Thread thread = new Thread(tcp);
        thread.start();
        try {
            thread.join();
            return tcp.getValue();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public static String sendMessageWithRespond(String message, String ip){
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        TCPSendMessageWithRespondThread tcp = new TCPSendMessageWithRespondThread(data, ip);
        Thread thread = new Thread(tcp);
        thread.start();
        try {
            thread.join();
            return tcp.getValue();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }


    public static class TCPSendMessageWithRespondThread implements Runnable {
        private volatile String value;
        byte[] data;
        String ip;
        public TCPSendMessageWithRespondThread(byte[] data, String ip){
            this.data = data;
            this.ip = ip;
        }

        @Override
        public void run() {
            Socket clientSocket = null;
            OutputStream out = null;
            InputStream is = null;
            BufferedReader br = null;
            byte[] packet_length  = ByteBuffer.allocate(4).putInt(data.length).array();
            try {
                clientSocket = new Socket(ip, Settings.TCP_SEND_PORT);
                out = clientSocket.getOutputStream();
                out.write(packet_length, 0, packet_length.length);
                out.write(data, 0, data.length);

                is = clientSocket.getInputStream();
                br = new BufferedReader(new InputStreamReader(is));
                value = br.readLine();

            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                clientSocket.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getValue() {
            return value;
        }
    }


    private static class TCPSendMessageThread extends Thread {
        byte[] data;
        String ip;

        public TCPSendMessageThread(byte[] data, String ip){
            this.data = data;
            this.ip = ip;
        }

        @Override
        public void run() {
            super.run();
            Socket clientSocket = null;
            OutputStream out = null;
            byte[] packet_length  = ByteBuffer.allocate(4).putInt(data.length).array();
            try {
                clientSocket = new Socket(ip, Settings.TCP_SEND_PORT);
                out = clientSocket.getOutputStream();
                out.write(packet_length, 0, packet_length.length);
                out.write(data, 0, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                clientSocket.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void received(){
        new TCPReceivedThread().start();
    }

    public static class TCPReceivedThread extends Thread{
        @Override
        public void run() {
            ServerSocket server = null;
            InputStream is = null;
            BufferedReader br = null;
           while (true){
               try {
                   server = new ServerSocket(Settings.TCP_LISTEN_PORT);
                   is = server.accept().getInputStream();
                   br = new BufferedReader(new InputStreamReader(is));
                   String json = br.readLine();

                  ObjectMapper mapper = new ObjectMapper();
                   try {
                       Map<String, Object> map = mapper.readValue(json, Map.class);
                       String command = map.get("command").toString();

                       switch (command){
                           case "setWallpaper":
                               if(udpReceiveMainActivityListener != null){
                                   String dataString =  map.get("value").toString();
                                  // byte[] data = dataString.getBytes(StandardCharsets.UTF_8);
                                   udpReceiveMainActivityListener.setWallPaper(dataString);
                               }
                               break;
                       }

                       server.close();
                       is.close();
                       br.close();
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }
               catch (Exception e) {
                   e.printStackTrace();
                   try {
                       server.close();
                       is.close();
                       br.close();
                   } catch (IOException ex) {
                       ex.printStackTrace();
                   }
               }
           }
        }
    }*/
}

