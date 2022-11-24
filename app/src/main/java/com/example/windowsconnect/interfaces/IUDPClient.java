package com.example.windowsconnect.interfaces;

import com.example.windowsconnect.models.Host;

public interface IUDPClient {
    void addHost(Host host);
    void openConnection(Host host);
}
