package com.example.windowsconnect.models;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class MyFile {
    public String name;
    public long length;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public long getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public MyFile(String name, long length) {
        this.name = name;
        this.length = length;
    }

    public MyFile() {
    }

}
