package com.example.windowsconnect.service;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.windowsconnect.interfaces.ITCPClient;
import com.example.windowsconnect.interfaces.tcp.ICloseConnection;
import com.example.windowsconnect.interfaces.tcp.IRemoveHostFromList;
import com.example.windowsconnect.interfaces.tcp.ISetProgressUploadFile;
import com.example.windowsconnect.interfaces.tcp.ISetWallpaper;
import com.example.windowsconnect.models.Command;
import com.example.windowsconnect.supportListeners.SuperSupportListener;
import com.example.windowsconnect.supportListeners.TcpClientListenerSupport;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TCPClient {

    private static TcpClientListenerSupport listeners = new TcpClientListenerSupport();


    public void addListener(ITCPClient listener) {
        listeners.addListener(listener);
    }

    public void removeListener(ITCPClient listener) {
        listeners.removeListener(listener);
    }

    private static final SuperSupportListener superSupportListener = SuperSupportListener.getListenerInfo();

    public void addSetWallpaperListener(ISetWallpaper l){
        superSupportListener.iSetWallpapers.add(l);
    }

    public void addICloseConnectionListener(ICloseConnection l){
        superSupportListener.iCloseConnections.add(l);
    }

    public void addIRemoveHostFromListsListener(IRemoveHostFromList l){
        superSupportListener.iRemoveHostFromLists.add(l);
    }

    public void addISetProgressUploadFileListener(ISetProgressUploadFile l){
        superSupportListener.iSetProgressUploadFiles.add(l);
    }

    private Socket _clientSocket = null;
    private OutputStream _outputStream = null;
    private InputStream _inputStream = null;
    private Thread _threadReceived;


    public TCPClient(String ip) {
        new TCPRegister(ip).start();
    }


    public class TCPRegister extends Thread {

        private String ip;

        public TCPRegister(String ip) {
            this.ip = ip;
        }

        @Override
        public void run() {
            try {
                _clientSocket = new Socket(ip, Settings.TCP_PORT);
                _outputStream = _clientSocket.getOutputStream();
                _inputStream = _clientSocket.getInputStream();
                _threadReceived = new TCPReceivedThread();
                _threadReceived.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean dispose(){
        _threadReceived.interrupt();
        try {
            _clientSocket.close();
            _outputStream.close();
            _inputStream.close();
            listeners.removeAllListeners();
        } catch (IOException e) {
            return false;
        }
        return true;
    }



    public void sendMessage(byte[] data, int command) {
        new TCPSendMessageThread(data, command).start();
    }

    public void sendMessage(String message, int command) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        new TCPSendMessageThread(data, command).start();
    }

    public void sendMessage(InputStream stream, long l) {
        new TCPSendMessageThreadStream(stream, l).start();
    }

    private class TCPSendMessageThread extends Thread {
        byte[] data;
        int command;
        public TCPSendMessageThread(byte[] data, int command){
            this.data = data;
            this.command = command;
        }

        @Override
        public void run() {
            super.run();
            byte[] packet_length = ByteBuffer.allocate(4).putInt(data.length).array();
            byte[] command_buffer = ByteBuffer.allocate(4).putInt(command).array();
            try {
                _outputStream.write(packet_length, 0, packet_length.length);
                _outputStream.write(command_buffer, 0, command_buffer.length);
                _outputStream.write(data, 0, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private class TCPSendMessageThreadStream extends Thread {
        InputStream stream;
        long length;
        public TCPSendMessageThreadStream(InputStream stream, long length){
            this.stream = stream;
            this.length = length;
        }

        @Override
        public void run() {
            super.run();
            byte[] packet_length  = ByteBuffer.allocate(8).putLong(length).array();
            try {
                _outputStream.write(packet_length, 0, packet_length.length);

                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];

                int len;
                long count = 0;
                while ((len = stream.read(buffer)) != -1) {
                    _outputStream.write(buffer, 0, len);
                    listeners.setProgressUploadFile((int)getProgress(length, count));
                    count += len;
                }
                listeners.setProgressUploadFile(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public long getProgress(long sum, long value)
        {
            return value * 100 / sum;
        }
    }

    public class TCPReceivedThread extends Thread {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            int length;
            byte[] data;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    int command = read4Bytes();
                    if(command == -1) continue;

                    switch (command) {
                        case Command.setWallpaper:
                            length = read4Bytes();
                            if(length == -1) break;
                            data = readBytes(length);
                            superSupportListener.setWallpaper(data);
                            break;
                        case Command.closeConnection:
                            superSupportListener.removeHostFromList();
                            superSupportListener.closeConnection();
                            break;
                        case Command.setTextClipBoard:
                            length = read4Bytes();
                            if(length == -1) break;
                            data = readBytes(length);
                            listeners.setTextClipBoard(new String(data, StandardCharsets.UTF_8));
                            break;
                    }

                }catch (SocketException e) {
                    listeners.closeConnection();
                    superSupportListener.closeConnection();
                    e.printStackTrace();
                    break;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private int read4Bytes() throws IOException {
            byte[] buffer = new byte[4];
            int bytesReceived = _inputStream.read(buffer, 0, 4);
            if (bytesReceived == -1)
            {
                superSupportListener.closeConnection();
                return -1;
            }

            if (bytesReceived != 4)
                return -1;


            ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
            byteBuffer.put(buffer);
            byteBuffer.rewind();
            return byteBuffer.getInt();
        }

        private byte[] readBytes(int length){
            byte[] buffer = new byte[length];
            int bytesReceived = 0;
            int count = 0;
            do
            {
                try {
                    bytesReceived = _inputStream.read(buffer, count, buffer.length - count);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                count += bytesReceived;
                if(count > length) break;
            }
            while (count != length);

            return buffer;
        }
    }
}

