package com.example.windowsconnect.supportListeners;

import com.example.windowsconnect.interfaces.IListenerSupport;
import com.example.windowsconnect.interfaces.ITCPClient;

import java.util.ArrayList;

public class TcpClientListenerSupport implements ITCPClient, IListenerSupport<ITCPClient> {

    ArrayList<ITCPClient> list = new ArrayList<>();

    @Override
    public void addListener(ITCPClient listener) {
        list.add(listener);
    }

    @Override
    public void removeListener(ITCPClient listener) {
        list.remove(listener);
    }

    @Override
    public void removeAllListeners(){
        list.clear();
    }

    @Override
    public synchronized void setProgressUploadFile(int progress) {
        for (ITCPClient i : list) {
            i.setProgressUploadFile(progress);
        }
    }

    @Override
    public synchronized void setWallPaper(String data) {
        for (ITCPClient i : list) {
            i.setWallPaper(data);
        }
    }

    @Override
    public synchronized void closeConnection() {
       for(int i = list.size()-1; i>=0; i--){
           list.get(i).closeConnection();
       }
    }

    @Override
    public synchronized void removeHostFromList() {
       for(int i = list.size()-1; i>=0; i--){
           list.get(i).removeHostFromList();
       }
    }

    @Override
    public void setTextClipBoard(String text) {
        for (ITCPClient i : list) {
            i.setTextClipBoard(text);
        }
    }
}

