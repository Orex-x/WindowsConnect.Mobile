package com.example.windowsconnect.interfaces;

import com.example.windowsconnect.models.Host;

public interface ListDeviceFragmentListener {
    void connectHost(Host host);
    void openConnection(Host host);
    void scanQR();
}
