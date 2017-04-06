package com.example.mylatouttest;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.MusicService.MusicService;
import com.example.VolumechangeReceiver.VolumnChangeReceiver;
import com.example.local_music.LocalMusic;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private VolumnChangeReceiver volumnChangeReceiver;
    private MessageReceiver messageReceiver;


    int position;
    int max;
    boolean ispause = true; //判断播放状态
    private MusicService musicService;
    SeekBar seekbar;

    ImageButton main_list_bt;
    ImageButton main_play_pause_bt;
    ImageButton main_like_bt;
    ImageButton main_recent_bt;
    ImageButton main_next_bt;
    ImageButton STOP;
    static TextView lrc;
    LyricInfo lyricInfo;
    TextView main_fulltitle_tv;
    TextView main_count_tv;
    boolean stopThread = false ;
    Thread lyricThread = new Thread() ;


    ArrayList<Map<String, String>> mapArrayList = null;


    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.MBind) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("info", "service disconnected");
        }
    };

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("com.example.MusicService.PROGRESS")) {
                seekbar.setProgress(intent.getIntExtra("PROGRESS", 0));
            } // 修改进度

            if (intent.getAction().equals("com.example.MusicService.DETIAL")) {
                main_fulltitle_tv.setText(intent.getStringExtra("TITLE"));
                main_count_tv.setText(intent.getIntExtra("COUNT", 0) + "");
                max = intent.getIntExtra("MAXPROGRESS", 0);
                seekbar.setMax(intent.getIntExtra("MAXPROGRESS", 0));
            } //接受并初始化/修改 当前歌曲 以及歌曲数目

            if (intent.getAction().equals("com.example.MusicService.ARRAY")) {
                mapArrayList = (ArrayList<Map<String, String>>) intent.getSerializableExtra("ARRAY");
            }   // 中转继续传给其他活动

            if (intent.getAction().equals("com.example.LocalMusic.PLAY")) {
                ispause = false;
                main_play_pause_bt.setImageResource(R.drawable.pausewhite);
            }

            if(intent.getAction().equals("com.example.MusicService.ISPLAY")){

                    if( intent.getBooleanExtra("ISPLAY",false) )
                    main_play_pause_bt.setImageResource(R.drawable.pausewhite);
                    else
                        main_play_pause_bt.setImageResource(R.drawable.startwhite);
            }

            if(intent.getAction().equals("com.example.MusicService.LRC")){
                File file = new File(Environment.getExternalStorageDirectory().getPath() + "//Musiclrc");
                File[] files;
                files = file.listFiles();

                Log.e("lrc",files[intent.getIntExtra("LRC",-1)].toString());
                getLRC(files[intent.getIntExtra("LRC",-1)],lyricInfo);



                lyricThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for(int i = 0 ; i < lyricInfo.lineinfo.size() && !stopThread ;i++) {
                            Message message = new Message();
                            message.obj = lyricInfo.lineinfo.get(i).line;
                            long start = System.currentTimeMillis();
                            long gap = 0;
                            long stop1 = 0;
                            Log.e("info",(lyricInfo.lineinfo.get(i).start+""));
                            try {
                                while (!stopThread &&
                                        (System.currentTimeMillis() - start - gap < (lyricInfo.lineinfo.get(i).start - lyricInfo.lineinfo.get(i - 1).start) )){
                                    if(stop1 != 0) {
                                        stop1 = 0 ;
                                    }
                                    while(ispause){
                                         if(stop1 == 0)
                                             stop1 = System.currentTimeMillis();

                                    }
                                    if(stop1 != 0){
                                        gap = gap + System.currentTimeMillis()-stop1;
                                    }

                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }


                            handler.sendMessage(message);
                        }

                    }
                });

                lyricThread.start();
            }

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("info","MainAcitivit CREATE");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.musicplayer_main);

        Intent intent = new Intent("com.example.MainActivity.REQUSETRES");
        sendBroadcast(intent);

        lyricInfo = new LyricInfo();
        lyricInfo.lineinfo = new ArrayList<>();

        readytoplay();



        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {     //按下进度条 先调用onStartTrackingTouch一次，再调用onProgressChanged一次
                seekBar.setMax(max);
                if (ispause) {
                    main_play_pause_bt.setImageResource(R.drawable.pausewhite);
                    ispause = false;
                }
                Intent intent = new Intent("com.example.MainActivity.ISSEEKBARTOUCH");
                intent.putExtra("ISSEEKBARTOUCH", true);
                sendBroadcast(intent);
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Intent intent2 = new Intent("com.example.MainActivity.STARTMUSIC");
                intent2.putExtra("PROGRESS", seekBar.getProgress()-1);
                intent2.putExtra("SEEK", true);
                sendBroadcast(intent2);

                Intent intent = new Intent("com.example.MainActivity.ISSEEKBARTOUCH");
                sendBroadcast(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.e("info","MainAcitivit Destory");
        unregisterReceiver(volumnChangeReceiver);
        unregisterReceiver(messageReceiver);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_list_bt:
                Intent acitvityintent = new Intent(this, LocalMusic.class);
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("arrayList", (ArrayList) mapArrayList);
                acitvityintent.putExtras(bundle);
                startActivity(acitvityintent);
                break;

            case R.id.main_play_pause_bt:
                Toast.makeText(this, "play", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent("com.example.MainActivity.STARTMUSIC");
                Intent intentnotify1 = new Intent("com.example.MusicService.NOTIFI");
                if (ispause) {
                    intent.putExtra("ISPAUSE", true);
                    sendBroadcast(intent);
                    intentnotify1.putExtra("PLAY",true);
                    sendBroadcast(intentnotify1);
                    main_play_pause_bt.setImageResource(R.drawable.pausewhite);
                    ispause = false;
                } else {
                    musicService.pauseMusic();
                    sendBroadcast(intentnotify1);
                    ispause = true;
                    main_play_pause_bt.setImageResource(R.drawable.startwhite);
                }


                break;

            case R.id.main_like_bt:
                Toast.makeText(this, "like", Toast.LENGTH_SHORT).show();

                break;

            case R.id.main_recent_bt:
                Toast.makeText(this, "recent", Toast.LENGTH_SHORT).show();
                break;

            case R.id.NEXT:
                seekbar.setProgress(0);
                Intent intentnext = new Intent("com.example.MainActivity.STARTMUSIC");
                intentnext.putExtra("NEXT", true);
                stopThread = true;
                stopThread = false;

                sendBroadcast(intentnext);

                Intent intentnotify = new Intent("com.example.MusicService.NOTIFI");
                intentnotify.putExtra("PLAY",true);
                sendBroadcast(intentnotify);

                main_play_pause_bt.setImageResource(R.drawable.pausewhite);
                ispause = false;
                break;

            case R.id.STOP:
             Log.e("reset","reset");   musicService.resetMusic();break;


        }
    }


    private void registerMyReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION"); //音量变化广播
        intentFilter.addAction("android.intent.action.HEADSET_PLUG"); //耳机插拔广播
        volumnChangeReceiver = new VolumnChangeReceiver();
        registerReceiver(volumnChangeReceiver, intentFilter); //注册广播

        messageReceiver = new MessageReceiver();

        intentFilter.addAction("com.example.MusicService.PROGRESS");
        intentFilter.addAction("com.example.MusicService.DETIAL");
        intentFilter.addAction("com.example.MusicService.ARRAY");
        intentFilter.addAction("com.example.LocalMusic.PLAY");
        intentFilter.addAction("com.example.MusicService.ISPLAY");
        intentFilter.addAction("com.example.MusicService.ISPAUSE");
        intentFilter.addAction("com.example.MusicService.LRC");

        registerReceiver(messageReceiver, intentFilter);

    }


    long time;int key = 0 ;
    @Override
    public void onBackPressed() {
        if(key++ == 0 ) {
            time = System.currentTimeMillis();
            Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
        }
        else if(System.currentTimeMillis()-time>2000){
            key = 0 ;
        }else finish();



    }

    private boolean checkPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }//运行权限


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.e("permit", "permit");
            return true;
        }
        Log.e("permit", "didnt permit");
        return false;
    }

    private void insertDesign() {
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;            //隐藏导航栏
            decorView.setSystemUiVisibility(option);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);                //隐藏状态栏
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void getView() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ActionBar actionBar;
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        main_list_bt = (ImageButton) findViewById(R.id.main_list_bt);
        main_play_pause_bt = (ImageButton) findViewById(R.id.main_play_pause_bt);
        main_like_bt = (ImageButton) findViewById(R.id.main_like_bt);
        main_recent_bt = (ImageButton) findViewById(R.id.main_recent_bt);
        main_next_bt = (ImageButton) findViewById(R.id.NEXT);
        main_fulltitle_tv = (TextView) findViewById(R.id.tv);
        main_count_tv = (TextView) findViewById(R.id.main_count_tv);
        lrc = (TextView)findViewById(R.id.lrc) ;
        seekbar = (SeekBar) findViewById(R.id.seekBar);
        STOP = (ImageButton)findViewById(R.id.STOP);

        STOP.setOnClickListener(this);
        main_like_bt.setOnClickListener(this);
        main_recent_bt.setOnClickListener(this);
        main_list_bt.setOnClickListener(this);
        main_play_pause_bt.setOnClickListener(this);
        main_next_bt.setOnClickListener(this);

    }



    private void readytoplay() {

        while (!checkPermission()) {
            sleep(500);
        }


        Intent intent = new Intent(MainActivity.this, MusicService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
        startService(intent);

        getView();
        insertDesign();
        registerMyReceiver();

    }

    void sleep(int time) {
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    class LyricInfo{
        List<LineInfo> lineinfo;
        String artist;
        String title;
    }

    class LineInfo{

        int start;
        String line;
    }

    private static Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            lrc.setText(msg.obj.toString());

        }
    };

    void getLRC(File file , LyricInfo lyricinfo){
        try {
            FileInputStream fip = new FileInputStream(file);
            InputStreamReader ips = new InputStreamReader(fip);
            BufferedReader bufferedReader = new BufferedReader(ips);

            lyricinfo.lineinfo = new ArrayList<>();

            String Line;

            while((Line = bufferedReader.readLine()) != null){

                int last = Line.indexOf(']');

                if(Line.startsWith("[ar:"))
                {
                    lyricinfo.artist = Line.substring(4,last);

                }

                if(Line.startsWith("[ti:")){
                    lyricinfo.title = Line.substring(4,last);
                }

                if(Line.startsWith("[0")){
                    LineInfo currentlineinfo = new LineInfo();

                    currentlineinfo.line =  Line.substring(last + 1 ).trim();
                    currentlineinfo.start = (int)(Integer.parseInt(Line.substring(1,3).trim())*60*1000 + Double.parseDouble(Line.substring(4,last).trim())*1000);
                    lyricinfo.lineinfo.add(currentlineinfo);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
