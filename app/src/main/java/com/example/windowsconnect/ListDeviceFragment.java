package com.example.windowsconnect;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.example.windowsconnect.adapters.HostAdapter;
import com.example.windowsconnect.interfaces.HostAdapterListener;
import com.example.windowsconnect.interfaces.ListDeviceFragmentListener;
import com.example.windowsconnect.interfaces.IUDPClient;
import com.example.windowsconnect.models.Host;
import com.example.windowsconnect.service.AutoFinderHost;
import com.example.windowsconnect.service.Settings;
import com.example.windowsconnect.service.UDPClient;

import java.util.ArrayList;


public class ListDeviceFragment extends Fragment implements HostAdapterListener, IUDPClient {

    ArrayList<Host> hosts = new ArrayList<>();
    ListView listView;
    Button btnScanQR;
    HostAdapter adapter;
    private ListDeviceFragmentListener _listener;

    public Handler handler;
    private ProgressBar progress;
    private UDPClient _udpClient;

    public ListDeviceFragment(ListDeviceFragmentListener listener, UDPClient udpClient){
        _listener = listener;
        _udpClient = udpClient;
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
        progress = v.findViewById(R.id.progress);
        adapter = new HostAdapter(getContext(), R.layout.host_item ,hosts, this);
        listView.setAdapter(adapter);

        _udpClient.addListener(this);

        btnScanQR.setOnClickListener(view -> _listener.scanQR());

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                progress.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            }
        };

        return v;
    }

    @Override
    public void click(int position) {
        Host host = hosts.get(position);
        _listener.connectHost(host);
    }

    @Override
    public void addHost(Host host) {
        for (Host h : hosts) {
            if(h.localIP.equals(host.localIP)) return;
        }

        hosts.add(host);
        handler.sendMessage(new Message());
    }

    @Override
    public void openConnection(Host host) {

    }
}