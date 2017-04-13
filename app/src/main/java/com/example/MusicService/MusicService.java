package com.example.MusicService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.example.mylatouttest.MainActivity;
import com.example.mylatouttest.MyApplication;
import com.example.mylatouttest.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MusicService extends Service {

    private MyApplication myApplication;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private ArrayList<Map<String, String>> data = new ArrayList<>();
    private char play_mode = 'o';  // o 顺序播放 r 随机播放 l 单曲循环
    private MBind mbind = new MBind();
    public int position = 0; //当前播放曲目的位置
    private RemoteViews contentView;
    private Notification notification;
    private PendingIntent pendingIntent;
    private NotificationCompat.Builder builder;
    private SQLiteDatabase db;

    MusicReceiver musicReceiver = new MusicReceiver();

    public class MusicReceiver extends BroadcastReceiver {

        //接收器
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("com.example.MainActivity.STARTMUSIC")) {

                data = myApplication.getData();

                if (intent.getBooleanExtra("NEXT", false)) {
                    contentView.setImageViewResource(R.id.play_image, R.drawable.pause);
                    notification = builder.setContent(contentView).build();
                    startForeground(1, notification);

                    setPosition();
                    mediaPlayer.reset();
                }

                if(intent.getBooleanExtra("PRE",false)){
                    contentView.setImageViewResource(R.id.play_image, R.drawable.pause);
                    notification = builder.setContent(contentView).build();
                    startForeground(1, notification);
                    if(position!=0) {
                        position = position - 1;
                        mediaPlayer.reset();
                    }else
                        Toast.makeText(context, "已经是第一首了", Toast.LENGTH_SHORT).show();
                }

                if (intent.getBooleanExtra("POSITION", false)) {
                    setPosition(intent.getIntExtra("LOCATION", 0));
                    mediaPlayer.reset(); //同样的 不reset就变成继续了
                }

                upgradeDataNotification();

                mainMessageCallBack();

                initMediaPlayer(data.get(position).get("data"));

                Intent intentRecent = new Intent("ChangeRecent");
                sendBroadcast(intentRecent);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ContentValues values = new ContentValues();
                        values.put("singer", data.get(position).get("singer"));
                        values.put("duration", data.get(position).get("duration"));
                        values.put("title", data.get(position).get("title"));
                        values.put("position", position);
                        Cursor cursor = db.query("Recent",null,null,null,null,null,null,null);

                        db.delete("Recent","title=?",new String[]{data.get(position).get("title")});
                        db.insert("Recent", null, values);  //同歌曲不加入啊
                    }
                }).start();  //播放历史 加入数据库存下

                Intent intentchangeMain = new Intent("CHANGEMAINBUTTON");
                sendBroadcast(intentchangeMain);
            }

            progressCallBack();


            if (intent.getBooleanExtra("SEEK", false)) {
                mediaPlayer.seekTo(intent.getIntExtra("PROGRESS", 0));
            }

            if (intent.getAction().equals("com.example.LocalMusic.MODE")) {
                play_mode = intent.getCharExtra("MODE", 'o');
            }

            if (intent.getAction().equals("notification_play_pause")) {
                if (mediaPlayer.isPlaying() && !intent.getBooleanExtra("LIST",false)) {
                    mediaPlayer.pause();
                    myApplication.setIsPlay(false);
                    contentView.setImageViewResource(R.id.play_image, R.drawable.playdark);
                    notification = builder.setContent(contentView).build();
                      //设置前台服务图标
                } else {                                                            //Notification的点击事件 无法自己修改自己的图标、只能通过发广播
                    myApplication.setIsPlay(true);
                    Intent intentstartmusic = new Intent("com.example.MainActivity.STARTMUSIC");
                    if(!intent.getBooleanExtra("LIST",false)){
                        sendBroadcast(intentstartmusic);          //如果是列表中选择，则列表内启动服务播放。否则 继续播放
                    }
                    contentView.setImageViewResource(R.id.play_image, R.drawable.pause);
                    notification = builder.setContent(contentView).build(); //设置Notification的图标
                }
                startForeground(1, notification);
                Intent intentchangeMain = new Intent("CHANGEMAINBUTTON");
                sendBroadcast(intentchangeMain);
            }

            if (intent.getAction().equals("CHANGENEXT")) {

                myApplication.setIsPlay(true);
                Intent intentplay = new Intent("com.example.MainActivity.STARTMUSIC");
                intentplay.putExtra("NEXT", true);
                sendBroadcast(intentplay);
            }

            if (intent.getAction().equals("com.example.MainActivity.REQUSETRES")) {
                mainMessageCallBack();
            }


        }
    }


    @Override
    public void onCreate() {

        super.onCreate();

        myApplication = (MyApplication) getApplication();
        data = myApplication.getData();

        db = myApplication.getDp();


        registerMyReceiver();//注册广播
        mainMessageCallBack();// 初始化界面信息

        Intent arraylistIntent = new Intent("com.example.MusicService.ARRAY");
        arraylistIntent.putExtra("ARRAY", data);
        sendBroadcast(arraylistIntent);            //将歌曲信息列表传给其他活动！


        try {
            SharedPreferences share = getSharedPreferences("data", MODE_PRIVATE);
            play_mode = share.getString("MODE", "orl").charAt(2);
        } catch (Exception e) {             //根据上次退出的选择模式
            e.printStackTrace();
        }

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) { //音乐播放完毕的监听器


                mediaPlayer.reset(); //音乐停后不会reset,先reset 否则不能下一首

                setPosition();  //播放完根据模式选择位置

                Intent intent = new Intent("com.example.MusicService.PROGRESS"); //复位进度条
                sendBroadcast(intent);

                Intent intent2 = new Intent("com.example.MainActivity.STARTMUSIC"); //自动播放
                sendBroadcast(intent2);
            }
        });


        initNotification();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mbind;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(musicReceiver);
        super.onDestroy();
    }

    private void progressCallBack() {   //返回播放进度信息

        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent("com.example.MusicService.PROGRESS");
                while (mediaPlayer.isPlaying()) {
                    if (!myApplication.isSeekBarTouch()) {
                        myApplication.setProgress(mediaPlayer.getCurrentPosition());
                        try {
                            Thread.sleep(400);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        sendBroadcast(intent);
                    }
                }
            }
        }).start();

    }

    private void mainMessageCallBack() {  //返回 歌曲数量 以及 当前歌曲
        Intent detialIntent = new Intent("com.example.MusicService.DETIAL");

        myApplication.setSeekBarMax(Integer.parseInt(data.get(position).get("duration")));
        myApplication.setBottomTitle(data.get(position).get("title"));
        myApplication.setBottomSinger(data.get(position).get("singer"));

        detialIntent.putExtra("COUNT", data.size());
        sendBroadcast(detialIntent);
    }

    private void initMediaPlayer(String location) {
        try {
            File file = new File(location);

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.reset();              //初始化播放器 并播放
            }
            mediaPlayer.setDataSource(file.getPath());
            mediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mediaPlayer.start();
    }



    public void play(String location){

        try {
            File file = new File(location);

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.reset();              //播放特定歌曲
            }
            mediaPlayer.setDataSource(file.getPath());
            mediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaPlayer.start();
    }


    public int setPosition() {

        if (play_mode == 'o') {
            position++;
            mediaPlayer.reset();          //根据模式选择下一首播放歌曲的位置
        } else if (play_mode == 'r') {
            position = (int) (Math.rint(Math.random() * data.size()));
        }

        myApplication.setPosition(position);
        return position;
    }

    public int setPosition(int position) {
        this.position = position;
        return position;
    }

    public void pauseMusic() {
        myApplication.setIsPlay(false);
        if (mediaPlayer.isPlaying()) {       //停止音乐
            mediaPlayer.pause();
        }
    }

    public void resetMusic() {
        mediaPlayer.reset();
    }


    public class MBind extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    void registerMyReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.MainActivity.STARTMUSIC");
        intentFilter.addAction("com.example.MainActivity.NEXTMUSIC");
        intentFilter.addAction("com.example.LocalMusic.MODE");
        intentFilter.addAction("notification_play_pause");
        intentFilter.addAction("CHANGENEXT");
        intentFilter.addAction("com.example.MainActivity.REQUSETRES");
        intentFilter.addAction("LISTEN");
        registerReceiver(musicReceiver, intentFilter);
    }

    void initNotification() {

        contentView = new RemoteViews(getPackageName(), R.layout.notification);
        contentView.setTextViewText(R.id.title_tv, data.get(position).get("title"));
        contentView.setTextViewText(R.id.singer_tv, data.get(position).get("singer"));
        contentView.setImageViewResource(R.id.next_image, R.drawable.nextdark);
        contentView.setImageViewResource(R.id.lyric_image, R.drawable.lyric);
        contentView.setImageViewResource(R.id.head_image, R.drawable.music);
        contentView.setImageViewResource(R.id.play_image, R.drawable.playdark);


        Intent intent = new Intent("notification_play_pause");
        contentView.setOnClickPendingIntent(R.id.play_image, PendingIntent.getBroadcast(MusicService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
                                                                                //点击事件
        Intent intent2 = new Intent("CHANGENEXT");
        contentView.setOnClickPendingIntent(R.id.next_image, PendingIntent.getBroadcast(this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT));


        Intent intentstartactivity = new Intent(MusicService.this, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(MusicService.this, 0, intentstartactivity, 0);
        builder = new NotificationCompat.Builder(MusicService.this);
        notification = builder
                .setContentIntent(pendingIntent)
                .setContent(contentView)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.delete)
                .build();
        startForeground(1, notification);


    }

    void upgradeDataNotification() {
        contentView.setTextViewText(R.id.title_tv, data.get(position).get("title"));
        contentView.setTextViewText(R.id.singer_tv, data.get(position).get("singer"));
        notification = builder.setContent(contentView).build();
        startForeground(1, notification);
    }




}
