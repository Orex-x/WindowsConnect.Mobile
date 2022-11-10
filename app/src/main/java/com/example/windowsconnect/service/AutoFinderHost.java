package com.example.windowsconnect.service;

import com.example.windowsconnect.models.Command;
import com.example.windowsconnect.models.CommandHelper;
import com.example.windowsconnect.models.Device;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.logging.Handler;

public class AutoFinderHost {
    public static void Find(Device device){
        String message = CommandHelper.createCommand(Command.requestAddDevice, device);
        new MulticastPublisher(message).start();
    }
}

class MulticastPublisher extends Thread{
    private DatagramSocket socket;
    private InetAddress group;
    private byte[] buf;
    private String message;

    public MulticastPublisher(String message){
        this.message = message;
    }

    public void run() {
        try{
            socket = new DatagramSocket();
            group = InetAddress.getByName("230.0.0.0");
            buf = message.getBytes();

            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length, group, Settings.SEND_PORT);
            socket.send(packet);
            socket.close();
        }catch (Exception e){

        }

        socket.close();
    }
}

class MulticastReceiver extends Thread {
    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[256];

    public void run() {
        try{
            socket = new MulticastSocket(5003);
            InetAddress group = InetAddress.getByName("230.0.0.0");
            socket.joinGroup(group);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String received = new String(
                        packet.getData(), 0, packet.getLength());
                if ("end".equals(received)) {
                    break;
                }
            }
            socket.leaveGroup(group);
        }catch (Exception e){

        }

        socket.close();
    }
}



