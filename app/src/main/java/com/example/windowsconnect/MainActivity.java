package com.example.windowsconnect;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.windowsconnect.interfaces.ListDeviceFragmentListener;
import com.example.windowsconnect.interfaces.UdpReceiveMainActivityListener;
import com.example.windowsconnect.models.Command;
import com.example.windowsconnect.models.CommandHelper;
import com.example.windowsconnect.models.Host;
import com.example.windowsconnect.models.MyFile;
import com.example.windowsconnect.service.AutoFinderHost;
import com.example.windowsconnect.service.CaptureAct;
import com.example.windowsconnect.service.Settings;
import com.example.windowsconnect.service.TCPClient;
import com.example.windowsconnect.service.UDPClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ListDeviceFragmentListener, UdpReceiveMainActivityListener {

    private Button _btnDisconnect;
    private ImageButton _btnSleep;
    private ImageButton _btnWakeUp;
    private ImageButton _btnPrint;
    private ImageView _imageView;

    private UDPClient _udpClient;
    private SeekBar _seekBarVolume;
    private TextView _txtVolume;
    private TextView _txtHostName;
    private Host _host;

    private Handler handler;

    private static final int REQUEST_TAKE_DOCUMENT = 2;

    //пасхалка, чтобы вызвать звук залипания клавиш (для степы),
    // надо нажать на название хоста 3 раза
    private int countClick = 0;


    FrameLayout frame;
    private ListDeviceFragment listDeviceFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _btnDisconnect = findViewById(R.id.btnDisconnectConnect);
        _btnSleep = findViewById(R.id.btnSleep);
        _btnWakeUp = findViewById(R.id.btnWakeUp);
        _btnPrint = findViewById(R.id.btnPrint);
        _seekBarVolume = findViewById(R.id.seekBarVolume);
        _txtVolume = findViewById(R.id.txtVolume);
        _txtHostName = findViewById(R.id.txtHostName);
        _imageView = findViewById(R.id.imageView);

        frame = findViewById(R.id.frame);

        _txtHostName.setOnClickListener(view ->{
            if(countClick++ == 3){
                countClick = 0;
                if(_udpClient != null){
                    String json = CommandHelper.createCommand(Command.playStepasSound, "");
                    _udpClient.sendMessage(json);
                }
            }
        });

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                _imageView.setImageBitmap((Bitmap) msg.obj);
            }
        };

        UDPClient.Receive(Settings.UDP_LISTEN_PORT);
        TCPClient.setUdpReceiveMainActivityListener(this);
        TCPClient.received();

        if (listDeviceFragment == null) {
            listDeviceFragment = new ListDeviceFragment(this);
        }

        if(_udpClient == null){
            _btnDisconnect.setText("Connect");
            new Thread(){
                @Override
                public void run() {
                    while (_udpClient == null){
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

        _btnPrint.setOnClickListener(v ->{
            if(_host != null){
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                //String[] mimetypes = {"application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/msword"};
                //intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                startActivityForResult(intent, REQUEST_TAKE_DOCUMENT);
            }
        });

        _btnDisconnect.setOnClickListener(v -> {
            if(_udpClient == null){
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

        _seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                _txtVolume.setText("Volume: " + i);
                if(_udpClient != null){
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
        _host = new Host(host.getPort(), host.getLocalIP(), host.getName(), host.getMacAddress());
        _udpClient = new UDPClient(host.localIP, host.port);
        String json = CommandHelper.createCommand(Command.addDevice, Settings.getDevice());
        _udpClient.sendMessage(json);
        frame.setVisibility(View.GONE);
        _btnDisconnect.setText("Disconnect");
        _txtHostName.setText("Host: " + host.getName());
        Toast.makeText(this, "Подключение успешно", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void scanQR() {
        scanCode();
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



                        //String command = CommandHelper.createCommand(Command.saveFile, myFile);


                        //byte[] hz = getDataFromURI(uri);
                        //String json = new String(utf8, StandardCharsets.UTF_8);
                        InputStream iStream = getContentResolver().openInputStream(uri);
                        byte[] inputData = getBytes(iStream);


                        MyFile myFile = new MyFile(getFileName(uri), inputData, inputData.length);
                        String command = CommandHelper.createCommand(Command.saveFile, myFile);
                        TCPClient.sendMessage(command, _host.localIP);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }}
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    public byte[] getDataFromURI(final Uri uri) throws IOException {
        InputStream is = getContentResolver().openInputStream(uri);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    // Nothing here should throw IOException in reality - work out what you want to do.
    public byte[] convertStream(Charset encoding, Uri uri) throws IOException {
        InputStream is = getContentResolver().openInputStream(uri);
        InputStreamReader contentReader = new InputStreamReader(is, encoding);

        int readCount;
        char[] buffer = new char[4096];
        try (ByteArrayOutputStream converted = new ByteArrayOutputStream()) {
            try (Writer writer = new OutputStreamWriter(converted, StandardCharsets.UTF_8)) {
                while ((readCount = contentReader.read(buffer, 0, buffer.length)) != -1) {
                    writer.write(buffer, 0, readCount);
                }
            }
            return converted.toByteArray();
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
}