package com.example.windowsconnect;

import static com.example.windowsconnect.core.Boot._host;
import static com.example.windowsconnect.core.Boot._tcpClient;
import static com.example.windowsconnect.core.Boot._udpClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MotionEventCompat;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.example.windowsconnect.models.Command;
import com.example.windowsconnect.models.CommandHelper;

import java.nio.ByteBuffer;

public class PlayerActivity extends AppCompatActivity {

    private View _virtualTouchPad;
    private TextView _txtVolume;
    private SeekBar _seekBarVolume;
    private Button _btnBack, _btnPause, _btnNext;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch _switchControlVolume;
    private boolean _controlVolume;
    private AudioManager _audioManager;
    private int _volumeLevel;


    @Override
    protected void onDestroy() {
        _udpClient.close();
        super.onDestroy();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        _virtualTouchPad = findViewById(R.id.virtualTouchPad);
        _txtVolume = findViewById(R.id.txtVolume);
        _seekBarVolume = findViewById(R.id.seekBarVolume);
        _btnBack = findViewById(R.id.btnBack);
        _btnPause = findViewById(R.id.btnPause);
        _btnNext = findViewById(R.id.btnNext);
        _switchControlVolume = findViewById(R.id.switchControlVolume);

        _udpClient.prepare(_host.localIP);
        _tcpClient.addICloseConnectionListener(this::onBackPressed);

        _audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        _virtualTouchPad.setOnTouchListener((v, event) -> {
            int x = (int)event.getX();
            int y = (int)event.getY();
            int actionEvent = MotionEventCompat.getActionMasked(event);
            int pointer = event.getPointerCount();

            ByteBuffer byteBuffer = ByteBuffer.allocate(20);
            byteBuffer.putInt(x);
            byteBuffer.putInt(y);
            byteBuffer.putInt(actionEvent);
            byteBuffer.putInt(pointer);
            byteBuffer.putInt(Command.virtualTouchPadChanged);

            byte[] packet = byteBuffer.array();
            _udpClient.sendMessageWithoutClose(packet);

            return true;
        });

        _seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                _txtVolume.setText("Volume: " + i);
                String json = CommandHelper.toJson(i);
                _udpClient.sendMessageWithoutClose(json, Command.changeVolume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        _switchControlVolume.setOnCheckedChangeListener((buttonView, isChecked) -> {
            _controlVolume = isChecked;
            if(isChecked) _volumeLevel = _seekBarVolume.getProgress();
        });


        setClick(_btnBack, 177);
        setClick(_btnPause, 179);
        setClick(_btnNext, 176);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(_controlVolume){
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    _volumeLevel += 5;
                    if(_volumeLevel > 100) _volumeLevel = 100;
                  break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    _volumeLevel -= 5;
                    if(_volumeLevel < 0) _volumeLevel = 0;
                    break;
            }
            String json = CommandHelper.toJson(_volumeLevel);
            _udpClient.sendMessageWithoutClose(json, Command.changeVolume);
            _seekBarVolume.setProgress(_volumeLevel);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setClick(Button btn, int code){
        btn.setOnTouchListener((view, event) -> {
            int action = MotionEventCompat.getActionMasked(event);
            if(action == 0){
                btnTouch(code, Command.downKeyboardHardwareKeyPress);
            }

            if(action == 1 || action == 3){
                btnTouch(code, Command.upKeyboardHardwareKeyPress);
            }
            return true;
        });
    }

    public void btnTouch(int code, int command){
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.putInt(code);
        byteBuffer.putInt(command);
        byte[] packet = byteBuffer.array();
        _udpClient.sendMessageWithoutClose(packet);
    }
}