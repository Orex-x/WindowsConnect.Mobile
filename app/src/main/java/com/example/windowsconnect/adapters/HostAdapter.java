package com.example.windowsconnect.adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.windowsconnect.R;
import com.example.windowsconnect.interfaces.HostAdapterListener;
import com.example.windowsconnect.models.Host;

import java.util.List;

public class HostAdapter extends ArrayAdapter<Host> {

    private LayoutInflater inflater;
    private int layout;
    private List<Host> hosts;
    private final HostAdapterListener _listener;

    public HostAdapter(Context context, int resource, List<Host> hosts, HostAdapterListener listener) {
        super(context, resource, hosts);
        _listener = listener;
        this.hosts = hosts;
        this.layout = resource;
        this.inflater = LayoutInflater.from(context);
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View view = inflater.inflate(this.layout, parent, false);

        TextView txtName = view.findViewById(R.id.txtName);
        Host host = hosts.get(position);

        txtName.setText(host.getName());

        view.setOnClickListener(view1 -> {
            _listener.click(position);
        });

        return view;
    }
}