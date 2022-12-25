package com.example.windowsconnect;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import com.example.windowsconnect.core.Boot;
import com.example.windowsconnect.interfaces.udp.IOpenConnection;
import com.example.windowsconnect.models.Host;
import com.example.windowsconnect.service.ClipboardService;
import com.example.windowsconnect.ui.TouchpadFragment;
import com.example.windowsconnect.ui.HomeFragment;
import com.example.windowsconnect.ui.NotificationsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.lang.reflect.InvocationTargetException;

public class HomeActivity extends AppCompatActivity {


    private BottomNavigationView _bottomNavigationView;

    private TouchpadFragment _touchpadFragment;
    private HomeFragment _homeFragment;
    private NotificationsFragment _notificationsFragment;

    private final String TOUCHPAD_FRAGMENT_TAG = "TouchPadFragmentTag";
    private final String HOME_FRAGMENT_TAG = "HomeFragmentTag";
    private final String NOTIFICATIONS_FRAGMENT_TAG = " NotificationsFragmentTag";

    private String _currentFragment = HOME_FRAGMENT_TAG;

    Boot _boot;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);



        if (savedInstanceState != null) {
            _touchpadFragment = (TouchpadFragment)
                    getSupportFragmentManager().findFragmentByTag(TOUCHPAD_FRAGMENT_TAG);

            _homeFragment = (HomeFragment)
                    getSupportFragmentManager().findFragmentByTag(HOME_FRAGMENT_TAG);

            _notificationsFragment = (NotificationsFragment)
                    getSupportFragmentManager().findFragmentByTag(NOTIFICATIONS_FRAGMENT_TAG);

            _currentFragment = savedInstanceState.getString("_currentFragment");
        }

        _touchpadFragment = init(_touchpadFragment, TouchpadFragment.class);
        _homeFragment = init(_homeFragment, HomeFragment.class);
        _notificationsFragment = init(_notificationsFragment, NotificationsFragment.class);

        _bottomNavigationView = findViewById(R.id.nav_view);

         switchFragment(_homeFragment, HOME_FRAGMENT_TAG);

        _bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()){
                case R.id.navigation_home : {
                    switchFragment(_homeFragment, HOME_FRAGMENT_TAG);
                    return true;
                }
                case R.id.navigation_touchpad : {
                    switchFragment(_touchpadFragment, TOUCHPAD_FRAGMENT_TAG);
                    return true;
                }
                case R.id.navigation_player : {
                    switchFragment(_notificationsFragment, NOTIFICATIONS_FRAGMENT_TAG);
                    return true;
                }
            }
            return false;
        });

        _boot = Boot.getBoot(this);

        _boot.addConnectionOpenListener(host -> {
            startService(new Intent(this, ClipboardService.class));
            _homeFragment.initListeners();
        });
    }

    public <T> void switchFragment(T t, String TAG){
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.nav_host_fragment_activity_home, (Fragment) t, TAG);
            fragmentTransaction.commit();
            _currentFragment = TAG;
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public <T> T init(T t, Class<T> tClass){
        if(t == null){
            try {
                t = tClass.getDeclaredConstructor().newInstance();
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return t;
    }
}