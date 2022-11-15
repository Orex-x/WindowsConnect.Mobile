package com.example.windowsconnect.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.EncoderProfiles;
import android.media.MediaCodecInfo;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.IBinder;
import android.telecom.VideoProfile;
import android.view.Surface;
import android.view.WindowMetrics;
import android.widget.Toast;

import com.example.windowsconnect.MainActivity;
import com.example.windowsconnect.R;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;


public class RecordService extends Service {
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;

    LocalSocket receiver;
    LocalServerSocket server;
    LocalSocket sender;

    private final String SOCKET_ADDRESS = "socket1";

    private boolean running;
    private int width = 720;
    private int height = 1080;
    private int dpi;


    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }

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
        try {
            server = new LocalServerSocket(SOCKET_ADDRESS);
            sender = new LocalSocket();
            sender.connect(new LocalSocketAddress(SOCKET_ADDRESS));
            initRecorder(sender);
            createVirtualDisplay();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mediaRecorder.start();
            try {
                receiver = server.accept();
                System.out.println("---------server.accept();------------- ");

                int ret = 0;
                while ((ret = receiver.getInputStream().read()) != -1)
                {
                    System.out.println( "ret =" + ret);
                }

                System.out.println("ret =" + ret);
            } catch (IOException e) {
                e.printStackTrace();
            }


        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        running = true;
        return true;
    }

    public boolean stopRecord() {
        if (!running) {
            return false;
        }
        running = false;
        mediaRecorder.stop();
        mediaRecorder.reset();
        virtualDisplay.release();
        mediaProjection.stop();

        return true;
    }

    private void createVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
    }

    private void initRecorder(LocalSocket sender) {
        try {
            String path = getApplicationInfo().dataDir;
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setVideoSize(width, height);
            mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
            mediaRecorder.setOutputFile(path + "/file.mp4");
            //  mediaRecorder.setOutputFile(sender.getFileDescriptor());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mediaRecorder.setVideoEncodingProfileLevel(2, 4);
            }
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