package com.example.windowsconnect.ui;

import static android.view.KeyEvent.KEYCODE_DEL;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.KeyEvent.KEYCODE_UNKNOWN;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.core.view.MotionEventCompat;
import androidx.fragment.app.Fragment;

import com.example.windowsconnect.R;
import com.example.windowsconnect.models.Command;

import static com.example.windowsconnect.core.Boot.host;
import static com.example.windowsconnect.core.Boot.udpClient;

import java.nio.ByteBuffer;

public class TouchpadFragment extends Fragment{

    private View _virtualTouchPad;
    private Button _btnESC, _btnTab, _btnCaps, _btnShift, _btnCtrl, _btnAlt, _btnEnter, _btnBackspace, _btnNextTrack;
    private ImageButton _btnKeyboard;

    @Override
    public void onDestroy() {
       // _tcpClient.removeListener(this);
        udpClient.close();
        super.onDestroy();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_touchpad, container, false);
        

        _virtualTouchPad = v.findViewById(R.id.virtualTouchPad);

        _btnESC = v.findViewById(R.id.btnESC);
        _btnTab = v.findViewById(R.id.btnTab);
        _btnCaps = v.findViewById(R.id.btnCaps);
        _btnShift = v.findViewById(R.id.btnShift);
        _btnCtrl = v.findViewById(R.id.btnCtrl);
        _btnKeyboard = v.findViewById(R.id.btnKeyboard);
        _btnAlt = v.findViewById(R.id.btnAlt);
        _btnEnter = v.findViewById(R.id.btnEnter);
        _btnBackspace = v.findViewById(R.id.btnBackspace);
        _btnNextTrack = v.findViewById(R.id.btnNextTrack);

        udpClient.prepare(host.localIP);

        _virtualTouchPad.setOnTouchListener((view, event) -> {
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
        EditText editText = v.findViewById(R.id.edt);
        editText.setRawInputType(0x00000000);

        editText.setOnKeyListener((view, keyCode, event) -> {
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


        _btnKeyboard.setOnClickListener((view) ->  {
            editText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
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

        return v;
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