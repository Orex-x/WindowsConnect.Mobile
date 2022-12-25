package com.example.windowsconnect.supportListeners;

import com.example.windowsconnect.interfaces.IListenerSupport;
import com.example.windowsconnect.interfaces.ITCPClient;
import com.example.windowsconnect.interfaces.IUDPClient;
import com.example.windowsconnect.interfaces.udp.IOpenConnection;
import com.example.windowsconnect.models.Host;

import java.util.ArrayList;

public class UdpClientListenerSupport implements IUDPClient, IListenerSupport<IUDPClient> {
    ArrayList<IUDPClient> list = new ArrayList<>();

   @Override
    public void addListener(IUDPClient listener) {
       list.add(listener);
    }

    @Override
    public void removeListener(IUDPClient listener) {
        list.remove(listener);
    }

    @Override
    public void removeAllListeners(){
        list.clear();
    }


    @Override
    public void addHost(Host host) {
        for (IUDPClient i : list) {
            i.addHost(host);
        }
    }

    @Override
    public void openConnection(Host host) {
        for (IUDPClient i : list) {
            i.openConnection(host);
        }
    }
}
