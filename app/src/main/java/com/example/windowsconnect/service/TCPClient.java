package com.example.windowsconnect.service;

import android.os.AsyncTask;

import com.example.windowsconnect.interfaces.UdpReceiveMainActivityListener;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TCPClient {
    private static UdpReceiveMainActivityListener udpReceiveMainActivityListener;
    public static void setUdpReceiveMainActivityListener(UdpReceiveMainActivityListener l){
        udpReceiveMainActivityListener = l;
    }

    public static void sendMessage(byte[] data, String ip){
        new TCPSendMessageThread(data, ip).start();
    }

    public static void sendMessage(String message, String ip){
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        new TCPSendMessageThread(data, ip).start();
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
            try {
                Socket clientSocket = new Socket (ip, Settings.TCP_SEND_PORT);
                OutputStream out = clientSocket.getOutputStream();
                out.write(data);
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
    }
}
