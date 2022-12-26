package com.example.windowsconnect;

import static com.example.windowsconnect.core.Boot.udpClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.windowsconnect.adapters.HostAdapter;
import com.example.windowsconnect.core.Boot;
import com.example.windowsconnect.interfaces.HostAdapterListener;
import com.example.windowsconnect.interfaces.ListDeviceFragmentListener;
import com.example.windowsconnect.models.Host;
import com.example.windowsconnect.service.AutoFinderHost;
import com.example.windowsconnect.service.CaptureAct;
import com.example.windowsconnect.service.Settings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ConnectionActivity extends AppCompatActivity implements HostAdapterListener {

    ArrayList<Host> hosts = new ArrayList<>();
    ListView listView;
    Button btnScanQR, btnConnect;
    HostAdapter adapter;
    EditText edtIP;
    private ProgressBar progress;
    private Boot _boot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        _boot = Boot.getBoot(this);

        listView = findViewById(R.id.recyclerView);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnConnect = findViewById(R.id.btnConnect);
        progress = findViewById(R.id.progress);
        edtIP = findViewById(R.id.edtIP);
        adapter = new HostAdapter(this, R.layout.host_item ,hosts, this);
        listView.setAdapter(adapter);
        
        new Thread(){
            @Override
            public void run() {
                while (!udpClient.isConnected()){
                    AutoFinderHost.Find(Settings.getDevice());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        udpClient.addAddHostListener(host -> {
            for (Host h : hosts) {
                if(h.localIP.equals(host.localIP)) return;
            }

            hosts.add(host);

            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            });
        });

        btnScanQR.setOnClickListener(view -> scanCode());

        btnConnect.setOnClickListener(view -> {
            String ip = edtIP.getText().toString();
            if(ip.length() > 0){
               requestConnectHost(new Host(5000, ip, "some think host", ""));
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
                requestConnectHost(new Host(hostPort, ip, name, macAddress));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    });

    @Override
    public void click(int position) {
        Host host = hosts.get(position);
        requestConnectHost(host);
    }

    public void requestConnectHost(Host host){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            CompletableFuture.runAsync(() -> {
            boolean ok = _boot.requestConnectHost(host);
            if(ok){
                onBackPressed();
            }else{
                Toast.makeText(this, "Компьютор отверг подключение", Toast.LENGTH_SHORT).show();
            }
            });
        }
    }
}