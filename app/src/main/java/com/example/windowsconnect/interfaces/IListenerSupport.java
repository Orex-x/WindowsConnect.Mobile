package com.example.windowsconnect.interfaces;

public interface IListenerSupport <T> {
    /**
     * Регистрирует нового прослушивателя
     */
    void addListener(T listener);

    /**
     * Удаляет ранее зарегистрированного прослушивателя
     */
    void removeListener(T listener);

    void removeAllListeners();
}
