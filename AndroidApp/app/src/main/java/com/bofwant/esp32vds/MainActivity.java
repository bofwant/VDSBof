package com.bofwant.esp32vds;

import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnFragmentInteractionListener,GraphFragment.OnFragmentInteractionListener,ControlFragment.OnFragmentInteractionListener {

    //private TextView mTextMessage;
    public boolean conected = false;
    public short sampleBuffer[]=new short[200000];
    UdpAdcHandler udpAdcHandler;
    UdpAdcThread udpAdcThread;
    Queue<String> txQueue;
    final HomeFragment fragment1 = new HomeFragment();
    final GraphFragment fragment2 = new GraphFragment();
    final SettingsFragment fragment3 = new SettingsFragment();
    boolean inSettings=false;
    final ControlFragment fragment4 = new ControlFragment();
    final FragmentManager fm = getSupportFragmentManager();

    int potValue,muxChanel;

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
        txQueue=new LinkedList<String>();
        if(txQueue.peek() == null){
            Log.d("udp control","queue empty");
        }
        setContentView(R.layout.activity_main);
        //mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        //ToggleButton conectButton = (ToggleButton) findViewById(R.id.conectButton);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        fm.beginTransaction().add(R.id.main_container, fragment4, "4").hide(fragment4).commit();
        //fm.beginTransaction().add(R.id.main_container, fragment3, "3").hide(fragment3).commit();
        fm.beginTransaction().add(R.id.main_container, fragment2, "2").hide(fragment2).commit();
        fm.beginTransaction().add(R.id.main_container,fragment1, "1").commit();

        udpAdcHandler = new UdpAdcHandler(this);



    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
    //Start the udp client and the connection with esp32
    public void ConectEsp32(){

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String address = SP.getString("serverIp_preference","192.168.4.1");
        int port =  Integer.valueOf(SP.getString("serverPort_preference","1260"));

        udpAdcThread = new UdpAdcThread(address, port, udpAdcHandler,this,this);
        udpAdcThread.start();
    }
    public void disconectEsp32(){
        udpAdcThread.setRunning(false);
        conected=false;
        fragment1.changePowerButton(false);

    }
    public  void UpdateSample(short sample,int pos){
        sampleBuffer[pos]=sample;
    }


    public static class UdpAdcHandler extends Handler {
        public static final int UPDATE_STATE = 0;
        public static final int UPDATE_MSG = 1;
        public static final int UPDATE_END = 2;
        public static final int STREAM_PKG = 3;
        private MainActivity parent;

        public UdpAdcHandler(MainActivity parent) {
            super();
            this.parent = parent;
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case UPDATE_STATE:
                    //parent.updateState((String)msg.obj);
                    parent.fragment1.logText.append("Update Status: "+(String)msg.obj);
                    parent.fragment1.logText.append("\n");
                    if(((String)msg.obj).equals(parent.getResources().getString(R.string.tag_conected))){
                        parent.conected=true;
                        parent.fragment1.changePowerButton(parent.conected);

                    }
                    break;
                case UPDATE_MSG:
                    //parent.updateRxMsg((String)msg.obj);
                    parent.fragment1.logText.append("Update MSG: "+(String)msg.obj);
                    parent.fragment1.logText.append("\n");
                    break;
                case UPDATE_END:
                    //parent.disconectEsp32();
                    Log.d("udp","UDP client stopped");
                    break;
                case STREAM_PKG:
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    }
}
