package com.example.windowsconnect.core;

import static com.example.windowsconnect.core.Boot.tcpClient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;

import com.example.windowsconnect.models.Command;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class Sender {

    private final Context _context;

    public Sender(Context context){
        _context = context;
    }

    public void sendImage(Uri uri){
        if(tcpClient != null){
            try {
                InputStream iStream = _context.getContentResolver().openInputStream(uri);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    long fileLength = getFileLength(uri);
                    tcpClient.sendMessage(getFileName(uri), Command.saveFile, true);
                    tcpClient.sendMessage(iStream, fileLength, false);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("Range")
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = _context.getContentResolver().query(uri,
                    null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @SuppressLint("Range")
    public long getFileLength(Uri uri) {
        long result = 0;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = _context.getContentResolver().query(uri, null,
                    null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                }
            } finally {
                cursor.close();
            }
        }
        return result;
    }
}
