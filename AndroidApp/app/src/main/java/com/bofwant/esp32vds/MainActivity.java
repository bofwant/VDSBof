package com.bofwant.esp32vds;

import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnFragmentInteractionListener,GraphFragment.OnFragmentInteractionListener,ControlFragment.OnFragmentInteractionListener {

    //private TextView mTextMessage;
    public boolean conected = false;

    final HomeFragment fragment1 = new HomeFragment();
    final GraphFragment fragment2 = new GraphFragment();
    final SettingsFragment fragment3 = new SettingsFragment();
    boolean inSettings=false;
    final ControlFragment fragment4 = new ControlFragment();
    final FragmentManager fm = getSupportFragmentManager();

    Fragment active = fragment1;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            FragmentTransaction ft;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    //mTextMessage.setText(R.string.title_home);
                    if(inSettings){
                        ft = getFragmentManager().beginTransaction();
                        ft.remove(fragment3).commit();
                        inSettings=false;
                    }
                    fm.beginTransaction().hide(active).show(fragment1).commit();
                    active = fragment1;
                    return true;
                case R.id.navigation_graph:
                    //mTextMessage.setText(R.string.title_Graph);
                    if(inSettings){
                        ft = getFragmentManager().beginTransaction();
                        ft.remove(fragment3).commit();
                        inSettings=false;
                    }
                    fm.beginTransaction().hide(active).show(fragment2).commit();
                    active = fragment2;
                    return true;
                case R.id.navigation_settings:
                    //mTextMessage.setText(R.string.title_settings);
                    fm.beginTransaction().hide(active).commit();
                    ft = getFragmentManager().beginTransaction();
                    ft.add(R.id.main_container,fragment3).commit();
                    inSettings=true;
                    return true;
                case R.id.navigation_control:
                    //mTextMessage.setText(R.string.title_control);
                    if(inSettings){
                        ft = getFragmentManager().beginTransaction();
                        ft.remove(fragment3).commit();
                        inSettings=false;
                    }
                    fm.beginTransaction().hide(active).show(fragment4).commit();
                    active = fragment4;
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        //ToggleButton conectButton = (ToggleButton) findViewById(R.id.conectButton);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        fm.beginTransaction().add(R.id.main_container, fragment4, "4").hide(fragment4).commit();
        //fm.beginTransaction().add(R.id.main_container, fragment3, "3").hide(fragment3).commit();
        fm.beginTransaction().add(R.id.main_container, fragment2, "2").hide(fragment2).commit();
        fm.beginTransaction().add(R.id.main_container,fragment1, "1").commit();




    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public static class UdpClientHandler extends Handler {
        public static final int UPDATE_STATE = 0;
        public static final int UPDATE_MSG = 1;
        public static final int UPDATE_END = 2;
        private MainActivity parent;

        public UdpClientHandler(MainActivity parent) {
            super();
            this.parent = parent;
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case UPDATE_STATE:
                    //parent.updateState((String)msg.obj);
                    break;
                case UPDATE_MSG:
                    //parent.updateRxMsg((String)msg.obj);
                    break;
                case UPDATE_END:
                    //parent.clientEnd();
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    }
}
