package com.example.windowsconnect.supportListeners;


import com.example.windowsconnect.interfaces.tcp.ICloseConnection;
import com.example.windowsconnect.interfaces.tcp.IRemoveHostFromList;
import com.example.windowsconnect.interfaces.tcp.ISetProgressUploadFile;
import com.example.windowsconnect.interfaces.udp.IAddHost;
import com.example.windowsconnect.interfaces.udp.IConnectionOpen;
import com.example.windowsconnect.interfaces.udp.IOpenConnection;
import com.example.windowsconnect.interfaces.tcp.ISetWallpaper;
import com.example.windowsconnect.models.Host;

import java.util.ArrayList;
import java.util.List;

public class SuperSupportListener implements
        IOpenConnection, IConnectionOpen, ISetWallpaper,
        ICloseConnection, IRemoveHostFromList, ISetProgressUploadFile, IAddHost{
    private static SuperSupportListener _listener;

    public static SuperSupportListener getListenerInfo() {
        if(_listener == null){
            _listener = new SuperSupportListener();
        }
        return _listener;
    }

    public List<IOpenConnection> iOpenConnections = new ArrayList<>();
    public List<IConnectionOpen> iConnectionOpens = new ArrayList<>();
    public List<ISetWallpaper> iSetWallpapers = new ArrayList<>();
    public List<ICloseConnection> iCloseConnections = new ArrayList<>();
    public List<IRemoveHostFromList> iRemoveHostFromLists = new ArrayList<>();
    public List<ISetProgressUploadFile> iSetProgressUploadFiles = new ArrayList<>();
    public List<IAddHost> iAddHosts = new ArrayList<>();

    @Override
    public void openConnection(Host host) {
        for (IOpenConnection item : iOpenConnections) item.openConnection(host);
    }

    @Override
    public void connectionOpen(Host host) {
        for (IConnectionOpen item : iConnectionOpens) item.connectionOpen(host);
    }

    @Override
    public void setWallpaper(byte[] data) {
        for (ISetWallpaper item : iSetWallpapers) item.setWallpaper(data);
    }

    @Override
    public void closeConnection() {
        for (ICloseConnection item : iCloseConnections) item.closeConnection();
    }

    @Override
    public void removeHostFromList() {
        for (IRemoveHostFromList item : iRemoveHostFromLists) item.removeHostFromList();
    }

    @Override
    public void setProgressUploadFile(int progress) {
        for (ISetProgressUploadFile item : iSetProgressUploadFiles) item.setProgressUploadFile(progress);
    }

    @Override
    public void addHost(Host host) {
        for (IAddHost item : iAddHosts) item.addHost(host);
    }
}
