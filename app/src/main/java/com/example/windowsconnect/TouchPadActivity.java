package com.example.windowsconnect;

import static com.example.windowsconnect.MainActivity._host;
import static com.example.windowsconnect.MainActivity._tcpClient;
import static com.example.windowsconnect.MainActivity._udpClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MotionEventCompat;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.windowsconnect.interfaces.ITCPClient;
import com.example.windowsconnect.models.Command;

import java.nio.ByteBuffer;

public class TouchPadActivity extends AppCompatActivity implements ITCPClient {

    private View _virtualTouchPad;
    TextView txtLog;
    private Handler handlerDestroy;

    @Override
    protected void onDestroy() {
        _tcpClient.removeListener(this);
        _udpClient.close();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_touch_pad);

        handlerDestroy = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                onDestroy();
            }
        };

//        _tcpClient.addListener(this);

        _virtualTouchPad = findViewById(R.id.virtualTouchPad);
        txtLog = findViewById(R.id.txtLog);


        _udpClient.prepare(_host.localIP);

        _virtualTouchPad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                StringBuilder stringBuilder = new StringBuilder();
                int actionEvent = event.getAction();
                int pointer = event.getPointerCount();
                stringBuilder.append("action " + actionEvent + "pointer " + pointer);
                stringBuilder.append("\n");

                for(int i = 0; i < event.getPointerCount(); i++){
                    int x = (int) event.getX(i);
                    int y = (int) event.getY(i);

                    stringBuilder.append("x" + i + " " + x + "y" + i + " " + y);
                    stringBuilder.append("\n");
                }



            /*    ByteBuffer byteBuffer = ByteBuffer.allocate(20);
                byteBuffer.putInt(x);
                byteBuffer.putInt(y);
                byteBuffer.putInt(actionEvent);
                byteBuffer.putInt(pointer);
                byteBuffer.putInt(Command.virtualTouchPadChanged);

                byte[] packet = byteBuffer.array();
                _udpClient.sendMessageWithoutClose(packet);*/

                txtLog.setText(stringBuilder.toString());
                return true;
            }
        });
    }

    @Override
    public void setProgressUploadFile(int progress) {

    }

    @Override
    public void setWallPaper(String data) {

    }

    @Override
    public void closeConnection() {
        handlerDestroy.sendMessage(new Message());
    }
}