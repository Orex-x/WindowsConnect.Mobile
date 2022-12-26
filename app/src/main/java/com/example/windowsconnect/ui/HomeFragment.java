package com.example.windowsconnect.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.windowsconnect.R;
import com.example.windowsconnect.core.Boot;


import static com.example.windowsconnect.core.Boot.tcpClient;




public class HomeFragment extends Fragment {

    ImageView _imageView;
    TextView _txtHostName;
    Boot _boot;

    private byte[] _wallPaper;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("_txtHostName", _txtHostName.getText().toString());
        outState.putByteArray("_wallPaper", _wallPaper);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onSaveInstanceState(new Bundle());
        super.onDestroy();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        if(savedInstanceState != null){
            _txtHostName.setText(savedInstanceState.getString("_txtHostName"));
            setWallPaper(savedInstanceState.getByteArray("_wallPaper"));
        }

        _imageView = v.findViewById(R.id.imageView);
        _txtHostName = v.findViewById(R.id.txtHostName);

        _boot = Boot.getBoot();

        _boot.addConnectionOpenListener(host ->
                getActivity().runOnUiThread(() ->
                        _txtHostName.setText("Host: " + host.getName())));

        return v;
    }

    public void setWallPaper(byte[] array){
        Bitmap bmp = BitmapFactory.decodeByteArray(array,0,array.length);
        getActivity().runOnUiThread(() -> _imageView.setImageBitmap(bmp));
    }

    public void initListeners(){
        tcpClient.addSetWallpaperListener(data -> {
            byte[] x = Base64.decode(data, Base64.DEFAULT);
            _wallPaper = x;
            setWallPaper(x);
        });
    }
}