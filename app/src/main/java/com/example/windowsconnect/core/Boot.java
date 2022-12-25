package com.example.windowsconnect.core;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.view.View;

import com.example.windowsconnect.interfaces.ITCPClient;
import com.example.windowsconnect.interfaces.IUDPClient;
import com.example.windowsconnect.interfaces.tcp.ICloseConnection;
import com.example.windowsconnect.interfaces.udp.IOpenConnection;
import com.example.windowsconnect.models.Host;
import com.example.windowsconnect.service.AutoFinderHost;
import com.example.windowsconnect.service.ClipboardService;
import com.example.windowsconnect.service.Database;
import com.example.windowsconnect.service.Settings;
import com.example.windowsconnect.service.TCPClient;
import com.example.windowsconnect.service.UDPClient;
import com.example.windowsconnect.supportListeners.SuperSupportListener;

import java.util.ArrayList;

public class Boot {

    private static final SuperSupportListener superSupportListener = SuperSupportListener.getListenerInfo();

    private final Context _context;
    private final Database databaseHelper;


    public static UDPClient _udpClient;
    public static TCPClient _tcpClient;
    public static Host _host;

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

        _udpClient = new UDPClient();

        _udpClient.addOpenConnectionListener(host -> {
            try{
                _host = host;
                _tcpClient = new TCPClient(host.localIP);

                _tcpClient.addICloseConnectionListener(() -> {
                    _host = null;
                    _udpClient.setConnected(false);
                    _tcpClient.dispose();
                    requestOpenConnection();
                });

                _udpClient.setConnected(true);
                _udpClient.sendMessage("200", -1 , host.localIP, Settings.UDP_SEND_WITH_RECEIVE_PORT);
                superSupportListener.connectionOpen(host);
            }catch (Exception ex){
                try{
                    _udpClient.sendMessage("500", -1 , host.localIP, Settings.UDP_SEND_WITH_RECEIVE_PORT);
                }catch (Exception ex2){

                }
            }
        });

        requestOpenConnection();
    }

    public void requestOpenConnection(){
        ArrayList<Host> hosts = databaseHelper.getAllHosts();
        if(hosts.size() > 0){
            new Thread(){
                @Override
                public void run() {
                    while (!_udpClient.isConnected()){
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

    public void addConnectionOpenListener(IOpenConnection listener){
        superSupportListener.iOpenConnections.add(listener);
    }
}
