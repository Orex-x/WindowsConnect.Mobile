package com.example.windowsconnect;

import static android.view.KeyEvent.KEYCODE_DEL;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.KeyEvent.KEYCODE_UNKNOWN;
import static com.example.windowsconnect.core.Boot.host;
import static com.example.windowsconnect.core.Boot.tcpClient;
import static com.example.windowsconnect.core.Boot.udpClient;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MotionEventCompat;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.example.windowsconnect.models.Command;

import java.nio.ByteBuffer;

public class TouchPadActivity extends AppCompatActivity {

    private View _virtualTouchPad;

    private Button _btnESC, _btnTab, _btnCaps, _btnShift, _btnCtrl, _btnAlt, _btnEnter, _btnBackspace, _btnNextTrack;
    private ImageButton _btnKeyboard;

    @Override
    protected void onDestroy() {
        udpClient.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_touch_pad);

        _virtualTouchPad = findViewById(R.id.virtualTouchPad);

        _btnESC  = findViewById(R.id.btnESC);
        _btnTab = findViewById(R.id.btnTab);
        _btnCaps  = findViewById(R.id.btnCaps);
        _btnShift = findViewById(R.id.btnShift);
        _btnCtrl = findViewById(R.id.btnCtrl);
        _btnKeyboard = findViewById(R.id.btnKeyboard);
        _btnAlt = findViewById(R.id.btnAlt);
        _btnEnter = findViewById(R.id.btnEnter);
        _btnBackspace = findViewById(R.id.btnBackspace);
        _btnNextTrack = findViewById(R.id.btnNextTrack);

        udpClient.prepare(host.localIP);

        tcpClient.addICloseConnectionListener(this::onBackPressed);

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
            udpClient.sendMessageWithoutClose(packet);

            return true;
        });
        EditText editText = findViewById(R.id.edt);
        editText.setRawInputType(0x00000000);


        editText.setOnKeyListener((v, keyCode, event) -> {
            char c;
            switch (event.getKeyCode()){
                case KEYCODE_DEL:
                    if(event.getAction() == KeyEvent.ACTION_UP){
                        Log.i("Key", String.valueOf(keyCode));
                        btnTouch(8, Command.upKeyboardHardwareKeyPress);
                    }

                    if(event.getAction() == KeyEvent.ACTION_DOWN){
                        Log.i("Key", String.valueOf(keyCode));
                        btnTouch(8, Command.downKeyboardHardwareKeyPress);
                    }
                    break;
                case KEYCODE_ENTER:
                    if(event.getAction() == KeyEvent.ACTION_UP){
                        Log.i("Key", String.valueOf(keyCode));
                        btnTouch(13, Command.upKeyboardHardwareKeyPress);
                    }

                    if(event.getAction() == KeyEvent.ACTION_DOWN){
                        Log.i("Key", String.valueOf(keyCode));
                        btnTouch(13, Command.downKeyboardHardwareKeyPress);
                    }
                    break;
                case KEYCODE_UNKNOWN:
                    c = event.getCharacters().charAt(0);
                    keyboardPress(c);
                default:
                    if(event.getAction() == KeyEvent.ACTION_UP){
                        c = (char) event.getUnicodeChar();
                        keyboardPress(c);
                    }
                    break;
            }
            return true;
        });

        _btnKeyboard.setOnClickListener((v) ->  {
            editText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(0, 0);
        });



        setClick(_btnNextTrack, 176);
        setClick(_btnESC, 27);
        setClick(_btnTab, 9);
        setClick(_btnShift, 16);
        setClick(_btnCtrl, 17);
        setClick(_btnCaps, 20);
        setClick(_btnAlt, 18);
        setClick(_btnEnter, 13);
        setClick(_btnBackspace, 8);
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
        udpClient.sendMessageWithoutClose(packet);
    }

    public void keyboardPress(char code){
        ByteBuffer byteBuffer = ByteBuffer.allocate(6);
        byteBuffer.putChar(code);
        byteBuffer.putInt(Command.keyboardPress);
        byte[] packet = byteBuffer.array();
        udpClient.sendMessageWithoutClose(packet);
    }
}