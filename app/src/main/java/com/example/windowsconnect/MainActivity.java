package com.example.windowsconnect;

import static android.content.ContentValues.TAG;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MotionEventCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.windowsconnect.interfaces.ITCPClient;
import com.example.windowsconnect.interfaces.ListDeviceFragmentListener;
import com.example.windowsconnect.models.Command;
import com.example.windowsconnect.models.CommandHelper;
import com.example.windowsconnect.models.Host;
import com.example.windowsconnect.models.MyFile;
import com.example.windowsconnect.models.Point;
import com.example.windowsconnect.service.AutoFinderHost;
import com.example.windowsconnect.service.CaptureAct;
import com.example.windowsconnect.service.RecordService;
import com.example.windowsconnect.service.Settings;
import com.example.windowsconnect.service.TCPClient;
import com.example.windowsconnect.service.UDPClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ListDeviceFragmentListener, ITCPClient {

    private Button _btnDisconnect;
    private ImageButton _btnSleep;
    private ImageButton _btnWakeUp;
    private ImageButton _btnPrint;
    private ImageButton _btnScreenStream;
    private ImageView _imageView;

    private UDPClient _udpClient;
    private TCPClient _tcpClient;
    private SeekBar _seekBarVolume;
    private TextView _txtVolume;
    private TextView _txtHostName;
    private TextView _txtVirtualTouchPadLog;
    private View _virtualTouchPad;
    private Host _host;

    private Handler handler, handlerProgressBar;

    private static final int REQUEST_TAKE_DOCUMENT = 2;

    private static final int RECORD_REQUEST_CODE  = 101;
    private static final int AUDIO_REQUEST_CODE   = 103;
    private static final int FOREGROUND_SERVICE_REQUEST_CODE = 104;
    private static final int CAMERA_REQUEST_CODE = 105;
    private static final int INTERNET_REQUEST_CODE = 106;
    private static final int ACCESS_NETWORK_STATE_REQUEST_CODE = 107;
    private static final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 108;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 108;

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private RecordService recordService;

    private ProgressBar _progressBarUploadFile;

    //пасхалка, чтобы вызвать звук залипания клавиш (для степы),
    // надо нажать на название хоста 3 раза
    private int countClick = 0;

    //для виртуального тачпада
    static int doubleClick = 0;
    static boolean click = false;
    static boolean multiClick = false;
    static boolean multiTouchUp = false;

    private boolean screenOn = false;

    ActivityResultLauncher<Intent> startMediaProjection;

    FrameLayout frame;
    private ListDeviceFragment listDeviceFragment;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        setContentView(R.layout.activity_main);

        _btnDisconnect = findViewById(R.id.btnDisconnectConnect);
        _btnSleep = findViewById(R.id.btnSleep);
        _btnWakeUp = findViewById(R.id.btnWakeUp);
        _btnPrint = findViewById(R.id.btnPrint);
        _btnScreenStream = findViewById(R.id.btnScreenStream);
        _seekBarVolume = findViewById(R.id.seekBarVolume);
        _txtVolume = findViewById(R.id.txtVolume);
        _txtHostName = findViewById(R.id.txtHostName);
        _imageView = findViewById(R.id.imageView);
        _virtualTouchPad = findViewById(R.id.virtualTouchPad);
        _txtVirtualTouchPadLog = findViewById(R.id.txtVirtualTouchPadLog);

        _progressBarUploadFile = findViewById(R.id.progressBarUploadFile);

        frame = findViewById(R.id.frame);




        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                _imageView.setImageBitmap((Bitmap) msg.obj);
            }
        };

        handlerProgressBar = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                _progressBarUploadFile.setProgress((int) msg.obj);
            }
        };

        _udpClient = new UDPClient();

        if (listDeviceFragment == null) {
            listDeviceFragment = new ListDeviceFragment(this, _udpClient);
        }

        if(!_udpClient.isConnected()){
            _btnDisconnect.setText("Connect");
            new Thread(){
                @Override
                public void run() {
                    while (!_udpClient.isConnected()){
                        AutoFinderHost.Find(Settings.getDevice());
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
        }else{
            _btnDisconnect.setText("Disconnect");
        }

        _btnSleep.setOnClickListener(v -> {
            //_udpClient.sendMessage(CommandHelper.createCommand(Command.sleep, ""));
        });

        _btnWakeUp.setOnClickListener(v -> {
            //_udpClient.WakeUp();
        });

        _btnScreenStream.setEnabled(false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            startMediaProjection = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {


                            Intent activityIntent = new Intent(this, MainActivity.class);
                            activityIntent.setAction("stop");
                            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                                String channelId = "001";
                                String channelName = "myChannel";
                                NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
                                channel.setLightColor(Color.BLUE);
                                channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

                                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                                if (manager != null) {
                                    manager.createNotificationChannel(channel);
                                    Notification notification = new Notification.
                                            Builder(getApplicationContext(), channelId)
                                            .setOngoing(true)
                                            .setSmallIcon(R.mipmap.ic_launcher)
                                            .setCategory(Notification.CATEGORY_SERVICE)
                                            .setContentTitle("cancel")
                                            .setContentIntent(contentIntent)
                                            .build();
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        recordService.startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
                                    } else {
                                        recordService.startForeground(1, notification);
                                    }
                                }
                            } else {
                                recordService.startForeground(1, new Notification());
                            }

                           new Handler().postDelayed(() -> {
                               mediaProjection = projectionManager.getMediaProjection(result.getResultCode(), result.getData());
                               recordService.setMediaProject(mediaProjection);
                               recordService.startRecord();
                               _btnScreenStream.setImageResource(R.drawable.air_play_on);
                           }, 0);
                        }
                    }
            );
        }
        _btnScreenStream.setOnClickListener(v ->{
            if (screenOn) {
                screenOn = false;
                recordService.stopRecord();
                _btnScreenStream.setImageResource(R.drawable.air_play_off);
            } else {
                screenOn = true;
                Intent captureIntent = projectionManager.createScreenCaptureIntent();
               // startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
                startMediaProjection.launch(captureIntent);
            }
        });


        _btnDisconnect.setOnClickListener(v -> {
            if(!_udpClient.isConnected()){
                frame.setVisibility(View.VISIBLE);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.frame, listDeviceFragment, "LIST_FRAGMENT_TAG");
                fragmentTransaction.commit();
            }else{
                _udpClient = null;
                _btnDisconnect.setText("Connect");
            }

        });

        setPermissions();

        Intent intent = new Intent(this, RecordService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);

    }

    public void setPermissions(){

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.INTERNET}, INTERNET_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_NETWORK_STATE}, ACCESS_NETWORK_STATE_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.RECORD_AUDIO}, AUDIO_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.FOREGROUND_SERVICE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.FOREGROUND_SERVICE}, FOREGROUND_SERVICE_REQUEST_CODE);
        }
    }



    public void setViewListeners(){
        _seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                _txtVolume.setText("Volume: " + i);
                if(_udpClient.isConnected()){
                    String json = CommandHelper.createCommand(Command.changeVolume, i);
                    _udpClient.sendMessage(json);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        _btnPrint.setOnClickListener(v ->{
            if(_host != null){
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                //String[] mimetypes = {"application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/msword"};
                //intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                startActivityForResult(intent, REQUEST_TAKE_DOCUMENT);
            }
        });

        _virtualTouchPad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int)event.getX();
                int y = (int)event.getY();


                if (event.getPointerCount() > 1) {
                    // Multitouch event
                    _txtVirtualTouchPadLog.setText("Multitouch " + "x: " + x + "y: " + y);
                    if(_udpClient.isConnected()){
                        String command = "";
                        switch (MotionEventCompat.getActionMasked(event)) {
                            case MotionEvent.ACTION_POINTER_DOWN:
                                multiClick = true;
                                new Handler().postDelayed(() -> multiClick = false, 80);
                                command = CommandHelper.createCommand(Command.virtualMultiTouchDown, new Point(x, y));
                                _udpClient.sendMessage(command);
                                break;
                            case MotionEvent.ACTION_MOVE:
                                command = CommandHelper.createCommand(Command.virtualMultiTouchMove, new Point(x, y));
                                _udpClient.sendMessage(command);
                                break;
                            case MotionEvent.ACTION_POINTER_UP:
                                if(multiClick){
                                    command = CommandHelper.createCommand(Command.virtualSingleTouchRightClick, "");
                                }else{
                                    multiClick = false;
                                    multiTouchUp = true;
                                    new Handler().postDelayed(() -> multiTouchUp = false, 100);
                                    command = CommandHelper.createCommand(Command.virtualMultiTouchUp, new Point(x, y));
                                }
                                _udpClient.sendMessage(command);
                                break;
                        }
                    }
                } else {
                    // Single touch event
                    _txtVirtualTouchPadLog.setText("Single touch " + "x: " + x + "y: " + y);
                    if(_udpClient.isConnected()){
                        String command = "";
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                click = true;
                                new Handler().postDelayed(() -> click = false, 80);
                                command = CommandHelper.createCommand(Command.virtualSingleTouchDown, new Point(x, y));
                                _udpClient.sendMessage(command);
                                break;
                            case MotionEvent.ACTION_MOVE:
                                if(!multiTouchUp){
                                    command = CommandHelper.createCommand(Command.virtualSingleTouchMove, new Point(x, y));
                                    _udpClient.sendMessage(command);
                                }
                                break;
                            case MotionEvent.ACTION_UP:
                                if(click){
                                    if(!multiClick){
                                        if(!multiTouchUp){
                                            command = CommandHelper.createCommand(Command.virtualSingleTouchLeftClick, "");
                                        }
                                    }
                                }else{
                                    click = false;
                                    command = CommandHelper.createCommand(Command.virtualSingleTouchUp, new Point(x, y));
                                }
                                 _udpClient.sendMessage(command);
                                break;
                        }
                    }
                }

                return true;
            }
        });




    }

    private void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volume up to flash on");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }


    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
        if(result.getContents() != null){
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> map = mapper.readValue(result.getContents(), Map.class);

                String ip = map.get("localIP").toString();
                int hostPort = Integer.parseInt(map.get("port").toString());
                String macAddress = map.get("macAddress").toString();
                String name = map.get("name").toString();


                connectHost(new Host(hostPort, ip, name, macAddress));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    });

    @Override
    public void connectHost(Host host) {
        _udpClient.setIp(host.localIP);
        String json = CommandHelper.createCommand(Command.addDevice, Settings.getDevice());
        int answer = Integer.parseInt(_udpClient.sendMessageWithReceive(json));

        if(answer == 200){
            _host = host;
            _tcpClient = new TCPClient(this, host.localIP);
            _udpClient.setConnected(true);
            frame.setVisibility(View.GONE);

            setViewListeners();
            _btnDisconnect.setText("Disconnect");
            _txtHostName.setText("Host: " + host.getName());
            Toast.makeText(this, "Подключение успешно", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Конечный компьютер отверг подключение", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void scanQR() {
        scanCode();
    }

    @Override
    public void setProgressUploadFile(int progress) {
        Message message = new Message();
        message.obj = progress;
        handlerProgressBar.sendMessage(message);
    }

    @Override
    public void setWallPaper(String data) {
        byte[] x = Base64.decode(data, Base64.DEFAULT);  //convert from base64 to byte array
        Bitmap bmp = BitmapFactory.decodeByteArray(x,0,x.length);
        Message message = new Message();
        message.obj = bmp;
        handler.sendMessage(message);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case REQUEST_TAKE_DOCUMENT:
                if(resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    try {
                        InputStream iStream = getContentResolver().openInputStream(uri);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            long fileLength = getFileLength(uri);
                            MyFile myFile = new MyFile(getFileName(uri), fileLength);
                            String command = CommandHelper.createCommand(Command.saveFile, myFile);
                            _tcpClient.sendMessageWithReceived(command);
                            _tcpClient.sendMessage(iStream, fileLength);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case RECORD_REQUEST_CODE:{
                if(resultCode == RESULT_OK) {
                    // delay needed because getMediaProjection() throws an error if it's called too soon
                    new Handler().postDelayed(() -> {
                        mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                        recordService.setMediaProject(mediaProjection);
                        recordService.startRecord();
                        _btnScreenStream.setImageResource(R.drawable.air_play_on);
                    }, 5000);
                }
                break;
            }
        }
    }


    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            RecordService.RecordBinder binder = (RecordService.RecordBinder) service;
            recordService = binder.getRecordService();
            recordService.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
            _btnScreenStream.setEnabled(true);
            _btnScreenStream.setImageResource(recordService.isRunning() ?  R.drawable.air_play_on : R.drawable.air_play_off);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_REQUEST_CODE ||
                requestCode == AUDIO_REQUEST_CODE ||
                requestCode == INTERNET_REQUEST_CODE ||
                requestCode == CAMERA_REQUEST_CODE ||
                requestCode == ACCESS_NETWORK_STATE_REQUEST_CODE ||
                requestCode == READ_EXTERNAL_STORAGE_REQUEST_CODE ||
                requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE ||
                requestCode == FOREGROUND_SERVICE_REQUEST_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
            }
        }
    }

    @SuppressLint("Range")
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @SuppressLint("Range")
    public long getFileLength(Uri uri) {
        long result = 0;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                }
            } finally {
                cursor.close();
            }
        }
        return result;
    }
}