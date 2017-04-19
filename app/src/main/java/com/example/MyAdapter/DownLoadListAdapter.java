package com.example.MyAdapter;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mylatouttest.MyApplication;
import com.example.mylatouttest.R;
import com.example.song.Hash;
import com.example.song.SongGetter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by 铖哥 on 2017/4/10.
 */

public class DownLoadListAdapter extends BaseAdapter {

    private ArrayList<Hash> resource;
    private Context context;
    private int[] to;
    private int layoutID;
    private String title;
    private String singer;
    private Toaster toaster = new Toaster();
    private MyApplication myApplication = MyApplication.getApplication();
    private List<String> DownQuene = new ArrayList<>();
    String hash = null;
    String path = null;
    Map<String, String> map = new HashMap<>();
    ArrayList<Map<String, String>> data = myApplication.getData();

    public DownLoadListAdapter(Context context, ArrayList<Hash> resource, int layoutID, int[] to) {
        super();
        this.context = context;
        this.resource = resource;
        this.layoutID = layoutID;
        this.to = to;
    }

    @Override
    public int getCount() {
        return resource.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder viewHolder = new ViewHolder();

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(layoutID, parent, false);
            viewHolder.down_bt = (ImageButton) view.findViewById(R.id.down_bt);
            viewHolder.down_singer = (TextView) view.findViewById(R.id.down_singer);
            viewHolder.down_title = (TextView) view.findViewById(R.id.item_title);
            viewHolder.down_play = (Button) view.findViewById(R.id.down_play);
            view.setTag(viewHolder);

        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }


        title = resource.get(position).getSongName();
        title = title.replaceAll("<em>", "");
        title = title.replaceAll("</em>", "");
        viewHolder.down_title.setText(title);

        singer = resource.get(position).getSingerName();  //去除异常的字符串

        singer = singer.replaceAll("<em>", "");
        singer = singer.replaceAll("</em>", "");
        viewHolder.down_singer.setText(singer);

        viewHolder.down_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SongGetter.download(resource.get(position).getFileHash());
                    }
                }).start();
            }
        });

        viewHolder.down_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                        boolean commom = false;
                        hash = resource.get(position).getFileHash();
                        for(String str : DownQuene){
                            if(str==hash)
                            commom = true;
                        }
                        if(!commom)
                            DownQuene.add(hash);



                if(!download.isAlive()){
                    download.start();
                }


            }
        });

        return view;
    }

    class ViewHolder {
        ImageButton down_bt;
        TextView down_title;
        TextView down_singer;
        Button down_play;
    }

    Thread download =  new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                while (DownQuene.size() != 0) {
                    String hash = DownQuene.get(0);
                    path = SongGetter.download(hash);

                    if (!path.equals("exist")) {
                        map.put("title", SongGetter.getSongData(hash).getData().getAudio_name().replaceAll("<em>", "").replaceAll("</em>", ""));
                        map.put("data", path);
                        map.put("singer", SongGetter.getSongData(hash).getData().getAuthor_name().replaceAll("<em>", "").replaceAll("</em>", ""));
                        map.put("fulltitle", SongGetter.getSongData(hash).getData().getAudio_name().replaceAll("<em>", "").replaceAll("</em>", ""));
                        map.put("duration", SongGetter.getSongData(hash).getData().getTimelength());
                        map.put("position", data.size() + "");


                        SQLiteDatabase db = myApplication.getDp();
                        ContentValues values = new ContentValues();
                        values.put("title", map.get("title"));
                        values.put("data", map.get("data"));
                        values.put("singer", map.get("singer"));
                        values.put("fulltitle", map.get("fulltitle"));
                        values.put("duration", map.get("duration"));
                        db.insert("MyMusic", null, values);  //导入到自己的数据库

                        toaster.sendEmptyMessage(1);

                        data.add(map);
                        myApplication.setFinaldata(data);
                        DownQuene.remove(0);
//
//                        myApplication.setPosition(position);
//                        myApplication.setIsPlay(true);
//                        Intent intent = new Intent("com.example.MainActivity.STARTMUSIC");
//                        intent.putExtra("POSITION", true);
//                        intent.putExtra("LOCATION", data.size() - 1);
//                        context.sendBroadcast(intent);
//                        Intent intent2 = new Intent("notification_play_pause");
//                        intent2.putExtra("LIST", true);
//                        context.sendBroadcast(intent2);

                    } else
                        toaster.sendEmptyMessage(3);
                }

            } catch (Exception e) {
                e.printStackTrace();
                toaster.sendEmptyMessage(0);
            }


        }
    });

    class Toaster extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show();
            } else if (msg.what == 1) {
                Toast.makeText(context, "下载成功", Toast.LENGTH_SHORT).show();
            } else if (msg.what == 3) {
                Toast.makeText(context, "您已经下载过这首歌了", Toast.LENGTH_SHORT).show();
            }

        }
    }
}
