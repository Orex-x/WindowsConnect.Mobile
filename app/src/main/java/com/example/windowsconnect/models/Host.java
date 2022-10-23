package com.example.windowsconnect.models;

public class Host {
    public int port;
    public String localIP;
    public String name;
    public String macAddress;

    public Host() {
    }

    public Host(int port, String localIP, String name, String macAddress) {
        this.port = port;
        this.localIP = localIP;
        this.name = name;
        this.macAddress = macAddress;
    }

    public int getPort() {
        return port;
    }

    public String getLocalIP() {
        return localIP;
    }

    public String getName() {
        return name;
    }

    public String getMacAddress() {
        return macAddress;
    }
}
