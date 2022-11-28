package com.example.windowsconnect.models;

public class Host {
    public int port;
    public String localIP;
    public String name;
    public String macAddress;
    public String status;

    public Host() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Host(int port, String localIP, String name, String macAddress) {
        this.port = port;
        this.localIP = localIP;
        this.name = name;
        this.macAddress = macAddress;
        this.status = "";
    }

    public void setLocalIP(String localIP) {
        this.localIP = localIP;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMacAddress(String macAddress) {
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
