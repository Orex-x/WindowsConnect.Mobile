package com.example.windowsconnect.interfaces;

import com.example.windowsconnect.models.Host;

public interface ListDeviceFragmentListener {
    void requestConnectHost(Host host);
    void scanQR();
}
