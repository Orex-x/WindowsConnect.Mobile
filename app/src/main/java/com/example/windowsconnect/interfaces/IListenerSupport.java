package com.example.windowsconnect.interfaces;

public interface IListenerSupport <T> {
    void addListener(T listener);
    void removeListener(T listener);
    void removeAllListeners();
}
