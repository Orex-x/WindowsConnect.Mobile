package com.example.windowsconnect;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.io.IOException;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Button _btnScan;
    private UDPClient _udpClient;
    private SeekBar _seekBarVolume;
    private TextView _txtVolume;

    @Override
    protected void onDestroy() {
        _udpClient.close();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _btnScan = findViewById(R.id.btnScan);
        _seekBarVolume = findViewById(R.id.seekBarVolume);
        _txtVolume = findViewById(R.id.txtVolume);


        _btnScan.setOnClickListener(v -> {
            scanCode();
        });
        _seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                _txtVolume.setText("Volume: " + i);
                String json = "{\"command\" : \"changeVolume\", \"value\" : " + i + "}";
                _udpClient.sendMessage(json);
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
               int port2 = Integer.parseInt(map.get("port").toString());
               _udpClient = new UDPClient(ip, port2);
             //  _udpClient.sendMessage("Connect");
               Toast.makeText(this, "Подключение успешно", Toast.LENGTH_SHORT).show();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
    });
}