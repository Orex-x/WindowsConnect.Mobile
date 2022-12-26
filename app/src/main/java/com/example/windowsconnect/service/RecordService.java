package com.example.windowsconnect.service;


import static com.example.windowsconnect.core.Boot.host;
import static com.example.windowsconnect.core.Boot.udpClient;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import java.io.IOException;


public class RecordService extends Service {
    private MediaProjection mediaProjection;
    private static MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;

    private boolean running;
    private int width = 720;
    private int height = 1080;
    private int dpi;


    protected ParcelFileDescriptor[] mParcelFileDescriptors;
    protected ParcelFileDescriptor mParcelRead;
    protected ParcelFileDescriptor mParcelWrite;

    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread serviceThread = new HandlerThread("service_thread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
        running = false;
        mediaRecorder = new MediaRecorder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void setMediaProject(MediaProjection project) {
        mediaProjection = project;
    }

    public boolean isRunning() {
        return running;
    }

    public void setConfig(int width, int height, int dpi) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }

    public boolean startRecord() {
        if (mediaProjection == null || running) {
            return false;
        }


        initRecorder();

        createVirtualDisplay();

        mediaRecorder.start();

        running = true;
        return true;
    }

    public boolean stopRecord() {
        if (!running) {
            return false;
        }
        udpClient.close();
        running = false;
        mediaRecorder.stop();
        mediaRecorder.reset();
        virtualDisplay.release();
        mediaProjection.stop();

        return true;
    }

    private void createVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(),
                null, null);
    }

    private void initRecorder() {
        try {
            mParcelFileDescriptors = ParcelFileDescriptor.createPipe();
            mParcelRead = new ParcelFileDescriptor(mParcelFileDescriptors[0]);
            mParcelWrite = new ParcelFileDescriptor(mParcelFileDescriptors[1]);



            ParcelFileDescriptor.AutoCloseInputStream inputStream =
                    new ParcelFileDescriptor.AutoCloseInputStream(mParcelRead);
            ParcelFileDescriptor.AutoCloseOutputStream outputStream =
                    new ParcelFileDescriptor.AutoCloseOutputStream(mParcelWrite);

            new Thread(() -> {

                byte[] packet = new byte[5 * 1024 * 1024];
                udpClient.prepare(host.localIP);
                while (true){
                    try {
                        int byteCount = inputStream.read(packet);
                        if(byteCount == -1) break;
                        udpClient.sendMessageWithoutClose(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                udpClient.close();

            }).start();

            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_2_TS);
            mediaRecorder.setOutputFile(mParcelWrite.getFileDescriptor());
            mediaRecorder.setVideoSize(width, height);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
            mediaRecorder.setVideoFrameRate(30);

            mediaRecorder.prepare();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public class RecordBinder extends Binder {
        public RecordService getRecordService() {
            return RecordService.this;
        }
    }
}