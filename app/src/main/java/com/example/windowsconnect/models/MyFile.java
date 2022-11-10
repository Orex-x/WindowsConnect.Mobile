package com.example.windowsconnect.models;

public class MyFile {
    public String name;
    public String data;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public MyFile(String name, String data) {
        this.name = name;
        this.data = data;
    }

    public MyFile() {
    }
}
