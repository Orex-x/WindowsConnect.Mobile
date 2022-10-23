package com.example.windowsconnect;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.windowsconnect.models.Command;
import com.example.windowsconnect.models.CommandHelper;
import com.example.windowsconnect.models.Device;
import com.example.windowsconnect.models.Host;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Button _btnDisconnect;
    private ImageButton _btnSleep;
    private ImageButton _btnWakeUp;

    private UDPClient _udpClient;
    private SeekBar _seekBarVolume;
    private TextView _txtVolume;
    private Device _device;
    private Host _host;

    @Override
    protected void onDestroy() {
        _udpClient.close();
        super.onDestroy();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        _device = new Device(android.os.Build.MODEL, new Date());

        _btnDisconnect = findViewById(R.id.btnDisconnect);
        _btnSleep = findViewById(R.id.btnSleep);
        _btnWakeUp = findViewById(R.id.btnWakeUp);
        _seekBarVolume = findViewById(R.id.seekBarVolume);
        _txtVolume = findViewById(R.id.txtVolume);

        if(_udpClient == null) scanCode();

        _btnSleep.setOnClickListener(v -> {
            _udpClient.sendMessage(CommandHelper.createCommand(Command.sleep, ""));
        });

        _btnWakeUp.setOnClickListener(v -> {
            _udpClient.WakeUp();
        });

        _btnDisconnect.setOnClickListener(v -> {
            _udpClient.close();
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
               String ip = map.get("ip").toString();
               String macAddress = map.get("macAddress").toString();
               String name = map.get("name").toString();
               int port = Integer.parseInt(map.get("port").toString());
               _host = new Host(port, ip, name, macAddress);
               _udpClient = new UDPClient(_host);
               String json = CommandHelper.createCommand(Command.addDevice, _device);
               _udpClient.sendMessage(json);
               Toast.makeText(this, "Подключение успешно", Toast.LENGTH_SHORT).show();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
    });
}