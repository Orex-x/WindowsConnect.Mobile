package com.example.windowsconnect.models;

public class MyFile {
    public String name;
    public byte[] data;
    public int length;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public MyFile(String name, byte[] data, int length) {
        this.name = name;
        this.data = data;
        this.length = length;
    }

    public MyFile() {
    }
}
