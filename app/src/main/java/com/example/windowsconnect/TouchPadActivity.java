package com.example.windowsconnect;

import static android.view.KeyEvent.ACTION_UP;
import static android.view.KeyEvent.KEYCODE_ALT_LEFT;
import static android.view.KeyEvent.KEYCODE_DEL;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.KeyEvent.KEYCODE_UNKNOWN;
import static com.example.windowsconnect.MainActivity._host;
import static com.example.windowsconnect.MainActivity._tcpClient;
import static com.example.windowsconnect.MainActivity._udpClient;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MotionEventCompat;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.windowsconnect.interfaces.ITCPClient;
import com.example.windowsconnect.models.Command;
import com.example.windowsconnect.service.RecordService;

import java.nio.ByteBuffer;

public class TouchPadActivity extends AppCompatActivity implements ITCPClient {

    private View _virtualTouchPad;
    private Handler handlerDestroy;
    private Button _btnESC, _btnTab, _btnCaps, _btnShift, _btnCtrl, _btnAlt, _btnEnter, _btnBackspace;
    private ImageButton _btnKeyboard;

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
        _btnKeyboard = findViewById(R.id.btnKeyboard);
        _btnAlt = findViewById(R.id.btnAlt);
        _btnEnter = findViewById(R.id.btnEnter);
        _btnBackspace = findViewById(R.id.btnBackspace);

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

/*        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() != 0){
                    char c = s.charAt(0);
                    keyboardPress(c);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                editText.getText().clear();
            }
        });*/
        _btnKeyboard.setOnClickListener((v) ->  {
            editText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(0, 0);
        });



        _btnESC.setOnTouchListener((v, event) -> {
            int action = MotionEventCompat.getActionMasked(event);
            if(action == 0){
                btnTouch(27, Command.downKeyboardHardwareKeyPress);
            }

            if(action == 1){
                btnTouch(27, Command.upKeyboardHardwareKeyPress);
            }
            return true;
        });

        _btnTab.setOnTouchListener((v, event) -> {
            int action = MotionEventCompat.getActionMasked(event);

            if(action == 0){
                btnTouch(9, Command.downKeyboardHardwareKeyPress);
            }

            if(action == 1){
                btnTouch(9, Command.upKeyboardHardwareKeyPress);
            }

            return true;
        });

        _btnShift.setOnTouchListener((v, event) -> {
            int action = MotionEventCompat.getActionMasked(event);
            if(action == 0){
                btnTouch(16, Command.downKeyboardHardwareKeyPress);
            }

            if(action == 1){
                btnTouch(16, Command.upKeyboardHardwareKeyPress);
            }
            return true;
        });

        _btnCtrl.setOnTouchListener((v, event) -> {
            int action = MotionEventCompat.getActionMasked(event);
            if(action == 0){
                btnTouch(17, Command.downKeyboardHardwareKeyPress);
            }

            if(action == 1){
                btnTouch(17, Command.upKeyboardHardwareKeyPress);
            }
            return true;
        });

        _btnCaps.setOnTouchListener((v, event) -> {
            int action = MotionEventCompat.getActionMasked(event);
            if(action == 0){
                btnTouch(20, Command.downKeyboardHardwareKeyPress);
            }

            if(action == 1){
                btnTouch(20, Command.upKeyboardHardwareKeyPress);
            }
            return true;
        });

        _btnAlt.setOnTouchListener((v, event) -> {
            int action = MotionEventCompat.getActionMasked(event);
            if(action == 0){
                btnTouch(18, Command.downKeyboardHardwareKeyPress);
            }

            if(action == 1){
                btnTouch(18, Command.upKeyboardHardwareKeyPress);
            }
            return true;
        });

        _btnEnter.setOnTouchListener((v, event) -> {
            int action = MotionEventCompat.getActionMasked(event);
            if(action == 0){
                btnTouch(13, Command.downKeyboardHardwareKeyPress);
            }

            if(action == 1){
                btnTouch(13, Command.upKeyboardHardwareKeyPress);
            }
            return true;
        });

        _btnBackspace.setOnTouchListener((v, event) -> {
            int action = MotionEventCompat.getActionMasked(event);
            if(action == 0){
                btnTouch(8, Command.downKeyboardHardwareKeyPress);
            }

            if(action == 1){
                btnTouch(8, Command.upKeyboardHardwareKeyPress);
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

    public void keyboardPress(char code){
        ByteBuffer byteBuffer = ByteBuffer.allocate(6);
        byteBuffer.putChar(code);
        byteBuffer.putInt(Command.keyboardPress);
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