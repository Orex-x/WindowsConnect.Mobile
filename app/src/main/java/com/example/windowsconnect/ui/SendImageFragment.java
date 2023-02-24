package com.example.windowsconnect.ui;

import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.example.windowsconnect.R;
import com.example.windowsconnect.interfaces.ISendImageFragment;


public class SendImageFragment extends Fragment {

    private ImageView _img;
    private Button _btnSend, _btnBack;
    private Uri _uri;
    private ISendImageFragment _l;

    public SendImageFragment(ISendImageFragment l, Uri uri){
        _uri = uri;
        _l = l;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_send_image, container, false);

        _img = v.findViewById(R.id.img);
        _btnBack = v.findViewById(R.id.btnBack);
        _btnSend = v.findViewById(R.id.btnSend);

        _img.setImageURI(_uri);

        _btnSend.setOnClickListener(view -> {
            _l.send(_uri);
            onDestroy();
        });

        _btnBack.setOnClickListener(view -> {
            _l.back();
            onDestroy();
        });

        return v;
    }
}