package com.example.windowsconnect.interfaces;

public interface ITCPClient {
    void setProgressUploadFile(int progress);
    void setWallPaper(String data);
    void closeConnection();
    void removeHostFromList();
    void setTextClipBoard(String text);
}
