package com.example.windowsconnect.core;

import android.content.Context;
import android.os.Build;

import com.example.windowsconnect.interfaces.udp.IOpenConnection;
import com.example.windowsconnect.models.Command;
import com.example.windowsconnect.models.CommandHelper;
import com.example.windowsconnect.models.Host;
import com.example.windowsconnect.service.AutoFinderHost;
import com.example.windowsconnect.service.Database;
import com.example.windowsconnect.service.Settings;
import com.example.windowsconnect.service.TCPClient;
import com.example.windowsconnect.service.UDPClient;
import com.example.windowsconnect.supportListeners.SuperSupportListener;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class Boot {

    private static final SuperSupportListener superSupportListener = SuperSupportListener.getListenerInfo();

    private final Context _context;


    public static Database databaseHelper;
    public static UDPClient udpClient;
    public static TCPClient tcpClient;
    public static Host host;

    private static Boot _boot;

    public static Boot getBoot(Context context){
        if(_boot == null){
            _boot = new Boot(context);
        }
        return _boot;
    }

    public static Boot getBoot(){
        return _boot;
    }

    private Boot(Context context) {
        _context = context;
        databaseHelper = new Database(context);

        udpClient = new UDPClient();

        udpClient.addOpenConnectionListener(host -> {
            try{
                Boot.host = host;
                tcpClient = new TCPClient(host.localIP);

                tcpClient.addICloseConnectionListener(this::closeConnection);

                udpClient.setConnected(true);
                udpClient.sendMessage("200", -1 , host.localIP, Settings.UDP_SEND_WITH_RECEIVE_PORT);
                superSupportListener.connectionOpen(host);
            }catch (Exception ex){
                try{
                    udpClient.sendMessage("500", -1 , host.localIP, Settings.UDP_SEND_WITH_RECEIVE_PORT);
                }catch (Exception ex2){

                }
            }
        });

        requestOpenConnection();
    }

    public void closeConnection(){
        Boot.host = null;
        udpClient.setConnected(false);
        tcpClient.dispose();
        requestOpenConnection();
    }

    public void requestOpenConnection(){
        ArrayList<Host> hosts = databaseHelper.getAllHosts();
        if(hosts.size() > 0){
            new Thread(){
                @Override
                public void run() {
                    while (!udpClient.isConnected()){
                        AutoFinderHost.RequestOpenConnection(Settings.getDevice(), hosts);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
        }
    }

    public boolean requestConnectHost(Host host) {
        String json = CommandHelper.toJson(Settings.getDevice());
        int answer = Integer.parseInt(udpClient.sendMessageWithReceive(json, Command.requestConnectDevice, host.localIP));
        if(answer == 200){
            databaseHelper.insertHost(host);
            return true;
        }else{
            return false;
        }
    }

    public void addConnectionOpenListener(IOpenConnection listener){
        superSupportListener.iOpenConnections.add(listener);
    }
}
