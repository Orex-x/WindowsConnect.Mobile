package com.example.windowsconnect;

import static com.example.windowsconnect.MainActivity._udpClient;
import static com.example.windowsconnect.MainActivity._tcpClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MotionEventCompat;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.windowsconnect.models.Command;
import com.example.windowsconnect.models.CommandHelper;

import java.nio.ByteBuffer;

public class TouchPadActivity extends AppCompatActivity {

    private View _virtualTouchPad;


    //для виртуального тачпада
    static int countClick = 0;
    static boolean click = false;
    static boolean multiClick = false;
    static boolean multiTouchUp = false;

    TextView txtLog;

    @Override
    protected void onDestroy() {
        _udpClient.close();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_touch_pad);
        _virtualTouchPad = findViewById(R.id.virtualTouchPad);
        txtLog = findViewById(R.id.txtLog);

        _udpClient.prepare();
        _virtualTouchPad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
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

                txtLog.setText("x " + x + "y " + y + "action " + actionEvent + "pointer " + pointer);
                return true;
            }
        });
    }
}