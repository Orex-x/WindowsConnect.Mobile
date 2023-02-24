package com.example.windowsconnect.interfaces;

import android.net.Uri;

import java.util.ArrayList;

public interface ISendImageFragment {
    void send(Uri uri);
    void send(ArrayList<Uri> uris);
    void back();
}
