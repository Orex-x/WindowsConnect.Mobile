package com.example.windowsconnect;

import static com.example.windowsconnect.core.Boot.databaseHelper;
import static com.example.windowsconnect.core.Boot.host;
import static com.example.windowsconnect.core.Boot.udpClient;
import static com.example.windowsconnect.core.Boot.tcpClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.windowsconnect.core.Boot;
import com.example.windowsconnect.models.Command;
import com.example.windowsconnect.models.CommandHelper;
import com.example.windowsconnect.models.Host;
import com.example.windowsconnect.models.MyFile;
import com.example.windowsconnect.service.ClipboardService;
import com.example.windowsconnect.service.RecordService;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity{

    private Button _btnDisconnect;
    private ImageButton _btnTouchPad;
    private ImageButton _btnSleep;
    private ImageButton _btnWakeUp;
    private ImageButton _btnPrint;
    private ImageButton _btnScreenStream;
    private ImageButton _btnPlayer;
    private ImageView _imageView;

    private SeekBar _seekBarVolume;
    private TextView _txtVolume;
    private TextView _txtHostName;
    private Boot _boot;

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

    private boolean screenOn = false;

    ActivityResultLauncher<Intent> startMediaProjection;

    FrameLayout frame;
  //  private ListDeviceFragment listDeviceFragment;

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

        _boot = Boot.getBoot(this);

        _btnDisconnect = findViewById(R.id.btnDisconnectConnect);
        _btnSleep = findViewById(R.id.btnSleep);
        _btnWakeUp = findViewById(R.id.btnWakeUp);
        _btnPrint = findViewById(R.id.btnPrint);
        _btnScreenStream = findViewById(R.id.btnScreenStream);
        _btnTouchPad = findViewById(R.id.btnTouchPad);
        _seekBarVolume = findViewById(R.id.seekBarVolume);
        _txtVolume = findViewById(R.id.txtVolume);
        _txtHostName = findViewById(R.id.txtHostName);
        _imageView = findViewById(R.id.imageView);
        _btnPlayer = findViewById(R.id.btnPlayer);

        _progressBarUploadFile = findViewById(R.id.progressBarUploadFile);

        frame = findViewById(R.id.frame);

       /* if (listDeviceFragment == null) {
            listDeviceFragment = new ListDeviceFragment(this);
        }*/

        if(!udpClient.isConnected()){
            _btnDisconnect.setText("Connect");
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
                            Intent activityIntent = new Intent(this,
                                    MainActivity.class);
                            activityIntent.setAction("stop");
                            PendingIntent contentIntent = PendingIntent.getActivity(this,
                                    0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                                String channelId = "001";
                                String channelName = "myChannel";
                                NotificationChannel channel = new NotificationChannel(channelId,
                                        channelName, NotificationManager.IMPORTANCE_NONE);
                                channel.setLightColor(Color.BLUE);
                                channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

                                NotificationManager manager = (NotificationManager)
                                        getSystemService(Context.NOTIFICATION_SERVICE);

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
                                        recordService.startForeground(1, notification,
                                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
                                    } else {
                                        recordService.startForeground(1, notification);
                                    }
                                }
                            } else {
                                recordService.startForeground(1, new Notification());
                            }

                           new Handler().postDelayed(() -> {
                               mediaProjection =
                                       projectionManager.getMediaProjection(result.getResultCode(),
                                               result.getData());
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
            if(!udpClient.isConnected()){
              /*  frame.setVisibility(View.VISIBLE);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.frame, listDeviceFragment, "LIST_FRAGMENT_TAG");
                fragmentTransaction.commit();*/
                Intent intent = new Intent(this, ConnectionActivity.class);
                startActivity(intent);
            }else{
                databaseHelper.deleteHost(host.localIP);
                _boot.closeConnection();
                closeConnection();
                _btnDisconnect.setText("Connect");
            }

        });

        setPermissions();

        Intent intent = new Intent(this, RecordService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);


        _boot.addConnectionOpenListener(this::connectionOpen);

    }

    public void connectionOpen(Host host){
        tcpClient.addICloseConnectionListener(this::closeConnection);
        tcpClient.addSetWallpaperListener(this::setWallPaper);
        tcpClient.addISetProgressUploadFileListener(this::setProgressUploadFile);
        runOnUiThread(() -> {
            setViewListeners(true);
            startService(new Intent(MainActivity.this, ClipboardService.class));
            //frame.setVisibility(View.GONE);
            _btnDisconnect.setText("Disconnect");
            _txtHostName.setText("Host: " + host.getName());
        });
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

    public void setViewListeners(boolean isActive){
        if(isActive){
            _seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    _txtVolume.setText("Volume: " + i);
                    if(udpClient.isConnected()){
                        //тут надо исправить
                        String json = CommandHelper.toJson(i);
                        udpClient.sendMessage(json, Command.changeVolume, host.localIP);
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
                if(host != null){
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("*/*");
                    //String[] mimetypes = {"application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/msword"};
                    //intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                    startActivityForResult(intent, REQUEST_TAKE_DOCUMENT);
                }
            });

            _btnTouchPad.setOnClickListener(v -> {
                Intent intent = new Intent(this, TouchPadActivity.class);
                startActivity(intent);
            });

            _btnPlayer.setOnClickListener(v -> {
                Intent intent = new Intent(this, PlayerActivity.class);
                startActivity(intent);
            });
        }else{
            _btnPrint.setOnClickListener(null);
            _btnPlayer.setOnClickListener(null);
            _btnTouchPad.setOnClickListener(null);
            _seekBarVolume.setOnSeekBarChangeListener(null);
        }
    }

   /* private void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volume up to flash on");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }
*/
/*
    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
        if(result.getContents() != null){
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> map = mapper.readValue(result.getContents(), Map.class);

                String ip = map.get("localIP").toString();
                int hostPort = Integer.parseInt(map.get("port").toString());
                String macAddress = map.get("macAddress").toString();
                String name = map.get("name").toString();


                requestConnectHost(new Host(hostPort, ip, name, macAddress));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    });*/

  /*  @Override
    public void requestConnectHost(Host host) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            CompletableFuture.runAsync(() -> {
                String json = CommandHelper.toJson(Settings.getDevice());
                int answer = Integer.parseInt(udpClient.sendMessageWithReceive(json, Command.requestConnectDevice, host.localIP));
                if(answer == 200){
                    databaseHelper.insertHost(host);
                }else{
                   runOnUiThread(() ->
                           Toast.makeText(
                                   MainActivity.this,
                                   "Конечный компьютер отверг подключение",
                                   Toast.LENGTH_SHORT).show());
                }
            });
        }
    }*/



    public void closeConnection() {
        runOnUiThread(() -> {
            _imageView.setImageResource(R.drawable.wallpaper);
            _txtHostName.setText("Host:");
            _btnDisconnect.setText("connect");
            setViewListeners(false);
            udpClient.setConnected(false);
            stopService(new Intent(this, ClipboardService.class));
        });
    }

    public void setWallPaper(byte[] data) {
        Bitmap bmp = BitmapFactory.decodeByteArray(data,0,data.length);
        runOnUiThread(() -> _imageView.setImageBitmap(bmp));
    }


  /*  @Override
    public void scanQR() {
        scanCode();
    }*/

    public void setProgressUploadFile(int progress) {
        runOnUiThread(() -> _progressBarUploadFile.setProgress(progress));
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
                            String json = CommandHelper.toJson(myFile);

                            tcpClient.sendMessage(json, Command.saveFile);
                            tcpClient.sendMessage(iStream, fileLength);
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
            if(grantResults.length > 0){
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish();
                }
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