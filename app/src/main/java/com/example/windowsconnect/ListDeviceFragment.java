package com.example.windowsconnect;

import static com.example.windowsconnect.core.Boot._udpClient;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.example.windowsconnect.adapters.HostAdapter;
import com.example.windowsconnect.interfaces.HostAdapterListener;
import com.example.windowsconnect.interfaces.ListDeviceFragmentListener;
import com.example.windowsconnect.interfaces.IUDPClient;
import com.example.windowsconnect.interfaces.udp.IAddHost;
import com.example.windowsconnect.models.Host;
import com.example.windowsconnect.service.AutoFinderHost;
import com.example.windowsconnect.service.Settings;
import com.example.windowsconnect.service.UDPClient;

import java.util.ArrayList;


public class ListDeviceFragment extends Fragment implements HostAdapterListener{

    ArrayList<Host> hosts = new ArrayList<>();
    ListView listView;
    Button btnScanQR, btnConnect;
    HostAdapter adapter;
    EditText edtIP;
    private ListDeviceFragmentListener _listener;

    private ProgressBar progress;

    public ListDeviceFragment(ListDeviceFragmentListener listener){
        _listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v =  inflater.inflate(R.layout.fragment_list_hosts, container, false);

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

        listView = v.findViewById(R.id.recyclerView);
        btnScanQR = v.findViewById(R.id.btnScanQR);
        btnConnect = v.findViewById(R.id.btnConnect);
        progress = v.findViewById(R.id.progress);
        edtIP = v.findViewById(R.id.edtIP);
        adapter = new HostAdapter(getContext(), R.layout.host_item ,hosts, this);
        listView.setAdapter(adapter);

        _udpClient.addAddHostListener(host -> {
            for (Host h : hosts) {
                if(h.localIP.equals(host.localIP)) return;
            }

            hosts.add(host);

            getActivity().runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            });
        });

        btnScanQR.setOnClickListener(view -> _listener.scanQR());

        btnConnect.setOnClickListener(view -> {
            String ip = edtIP.getText().toString();
            if(ip.length() > 0){
                _listener.requestConnectHost(new Host(5000, ip, "some think host", ""));
            }
        });


        return v;
    }

    @Override
    public void click(int position) {
        Host host = hosts.get(position);
        _listener.requestConnectHost(host);
    }
}