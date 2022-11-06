package com.example.windowsconnect.models;

import java.util.Date;

public class Device {
    public String Name;
    public String IP;
    public Date DateConnect;

    public Device() {
    }

    public Device(String name) {
        Name = name;
    }

    public Device(String name, String IP, Date dateConnect) {
        Name = name;
        this.IP = IP;
        DateConnect = dateConnect;
    }
}
