package com.example.windowsconnect;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.example.windowsconnect.adapters.HostAdapter;
import com.example.windowsconnect.interfaces.HostAdapterListener;
import com.example.windowsconnect.interfaces.ListDeviceFragmentListener;
import com.example.windowsconnect.interfaces.UdpReceiveListDeviceFragmentListener;
import com.example.windowsconnect.models.Host;
import com.example.windowsconnect.service.AutoFinderHost;
import com.example.windowsconnect.service.Settings;
import com.example.windowsconnect.service.UDPClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;


public class ListDeviceFragment extends Fragment implements HostAdapterListener, UdpReceiveListDeviceFragmentListener {

    ArrayList<Host> hosts = new ArrayList<>();
    ListView listView;
    HostAdapter adapter;
    private ListDeviceFragmentListener _listener;

    private Handler handler;
    private ProgressBar progress;


    public ListDeviceFragment(ListDeviceFragmentListener listener){
        _listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v =  inflater.inflate(R.layout.fragment_list_hosts, container, false);


        listView = v.findViewById(R.id.recyclerView);
        progress = v.findViewById(R.id.progress);
        adapter = new HostAdapter(getContext(), R.layout.host_item ,hosts, this);
        listView.setAdapter(adapter);

        UDPClient.setUdpReceiveListDeviceFragmentListener(this);


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
}