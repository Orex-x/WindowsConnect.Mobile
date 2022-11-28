package com.example.windowsconnect.service;

import static com.example.windowsconnect.MainActivity._udpClient;
import static com.example.windowsconnect.service.UDPClient.reverse;

import com.example.windowsconnect.models.Command;
import com.example.windowsconnect.models.CommandHelper;
import com.example.windowsconnect.models.Device;
import com.example.windowsconnect.models.Host;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class AutoFinderHost {
    public static void Find(Device device){
        String message = CommandHelper.toJson(device);
        new MulticastPublisher(message, Command.requestAddDevice).start();
    }

    public static void RequestOpenConnection(Device device, ArrayList<Host> hosts){
        String message = CommandHelper.toJson(device);
        for (Host host : hosts) {
            _udpClient.sendMessage(message, Command.requestConnectDevice, host.localIP);
        }
    }
}

class MulticastPublisher extends Thread{
    private DatagramSocket socket;
    private InetAddress group;
    private String message;
    private int command;

    public MulticastPublisher(String message, int command){
        this.message = message;
        this.command = command;
    }

    public void run() {
        try{
            socket = new DatagramSocket();
            group = InetAddress.getByName("230.0.0.0");

            byte[] command_buffer = ByteBuffer.allocate(4).putInt(command).array();
            reverse(command_buffer);
            byte[] msg = UDPClient.join(command_buffer, message.getBytes());

            DatagramPacket packet = new DatagramPacket(msg, msg.length, group, Settings.UDP_SEND_PORT);
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
            socket = new MulticastSocket(Settings.UDP_LISTEN_PORT);
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



