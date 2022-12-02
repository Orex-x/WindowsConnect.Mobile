package com.example.windowsconnect.service;


import static com.example.windowsconnect.MainActivity._tcpClient;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.windowsconnect.interfaces.ITCPClient;

public class ClipboardService extends Service implements ITCPClient {


    private static final String TAG = "ClipboardService";
    ClipboardManager myClipboard;
    ClipData myClip;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        _tcpClient.addListener(this);
        myClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        myClipboard.addPrimaryClipChangedListener(() -> {
            String text = myClipboard.getPrimaryClip().toString();
            Log.d(TAG, "copy: " + text);
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void setProgressUploadFile(int progress) {

    }

    @Override
    public void setWallPaper(String data) {

    }

    @Override
    public void closeConnection() {

    }

    @Override
    public void removeHostFromList() {

    }

    @Override
    public void setTextClipBoard(String text) {
        myClip = ClipData.newPlainText("text", text);
        myClipboard.setPrimaryClip(myClip);
        Log.d(TAG, "add clipboard: " + text);
    }
}
