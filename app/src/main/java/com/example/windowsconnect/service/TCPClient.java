package com.example.windowsconnect.service;

import com.example.windowsconnect.interfaces.UdpReceiveMainActivityListener;
import com.example.windowsconnect.models.Host;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TCPClient {
    private static UdpReceiveMainActivityListener udpReceiveMainActivityListener;
    public static void setUdpReceiveMainActivityListener(UdpReceiveMainActivityListener l){
        udpReceiveMainActivityListener = l;
    }


    public static void received(){
        new TCPMyThread().start();
    }

    public static class TCPMyThread extends Thread{
        @Override
        public void run() {
            ServerSocket server = null;
            InputStream is = null;
            BufferedReader br = null;
           while (true){
               try {
                   server = new ServerSocket(5002);
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
