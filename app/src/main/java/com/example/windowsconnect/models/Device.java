package com.example.windowsconnect.models;

import java.util.Date;

public class Device {
    public String Name;
    public Date DateConnect;

    public Device() {
    }

    public Device(String name) {
        Name = name;
    }

    public Device(String name, Date dateConnect) {
        Name = name;
        DateConnect = dateConnect;
    }
}
