package com.example.windowsconnect;

import static com.example.windowsconnect.MainActivity._host;
import static com.example.windowsconnect.MainActivity._tcpClient;
import static com.example.windowsconnect.MainActivity._udpClient;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MotionEventCompat;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.example.windowsconnect.interfaces.ITCPClient;
import com.example.windowsconnect.models.Command;

import java.nio.ByteBuffer;

public class TouchPadActivity extends AppCompatActivity implements ITCPClient {

    private View _virtualTouchPad;
    private Handler handlerDestroy;
    private Button _btnESC, _btnTab, _btnCaps, _btnShift, _btnCtrl;
    boolean shiftDown = false;
    @Override
    protected void onDestroy() {
        _tcpClient.removeListener(this);
        _udpClient.close();
        super.onDestroy();
    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_touch_pad);

        handlerDestroy = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                onBackPressed();
            }
        };

       _tcpClient.addListener(this);

        _virtualTouchPad = findViewById(R.id.virtualTouchPad);

        _btnESC  = findViewById(R.id.btnESC);
        _btnTab = findViewById(R.id.btnTab);
        _btnCaps  = findViewById(R.id.btnCaps);
        _btnShift = findViewById(R.id.btnShift);
        _btnCtrl = findViewById(R.id.btnCtrl);

        _udpClient.prepare(_host.localIP);

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

        _btnESC.setOnTouchListener((v, event) -> {
            int action = MotionEventCompat.getActionMasked(event);
            if(action == 0){
                btnTouch(27, Command.downButtonCSCTE);
            }

            if(action == 1){
                btnTouch(27, Command.upButtonCSCTE);
            }
            return true;
        });

        _btnTab.setOnTouchListener((v, event) -> {
            int action = MotionEventCompat.getActionMasked(event);

            if(action == 0){
                btnTouch(9, Command.downButtonCSCTE);
            }

            if(action == 1){
                btnTouch(9, Command.upButtonCSCTE);
            }

            return true;
        });



        _btnShift.setOnTouchListener((v, event) -> {
            int action = MotionEventCompat.getActionMasked(event);
            if(action == 0){
                btnTouch(16, Command.downButtonCSCTE);
            }

            if(action == 1){
                btnTouch(16, Command.upButtonCSCTE);
            }
            return true;
        });

        _btnCtrl.setOnTouchListener((v, event) -> {
            int action = MotionEventCompat.getActionMasked(event);
            if(action == 0){
                btnTouch(17, Command.downButtonCSCTE);
            }

            if(action == 1){
                btnTouch(17, Command.upButtonCSCTE);
            }
            return true;
        });

        _btnCaps.setOnTouchListener((v, event) -> {
            int action = MotionEventCompat.getActionMasked(event);
            if(action == 0){
                btnTouch(20, Command.downButtonCSCTE);
            }

            if(action == 1){
                btnTouch(20, Command.upButtonCSCTE);
            }
            return true;
        });

      /*  _btnESC.setOnClickListener((v) -> {
            btnClick(27);
        });
        _btnTab.setOnClickListener((v) -> {
            btnClick(9);
        });
        _btnShift.setOnClickListener((v) -> {
            btnClick(16);
        });
        _btnCtrl.setOnClickListener((v) -> {
            btnClick(17);
        });
        _btnCaps.setOnClickListener((v) -> {
            btnClick(20);
        });*/

    }

    public void btnTouch(int code, int command){
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.putInt(code);
        byteBuffer.putInt(command);
        byte[] packet = byteBuffer.array();
        _udpClient.sendMessageWithoutClose(packet);
    }

    public void btnClick(int code){
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.putInt(code);
        byteBuffer.putInt(Command.clickButtonCSCTE);
        byte[] packet = byteBuffer.array();
        _udpClient.sendMessageWithoutClose(packet);
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

    @Override
    public void removeHostFromList() {

    }

    @Override
    public void setTextClipBoard(String text) {

    }
}