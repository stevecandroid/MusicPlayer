package com.example.MyAdapter;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.Utils.ToastHelper;
import com.example.mylatouttest.MyApplication;
import com.example.mylatouttest.R;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 铖哥 on 2017/4/1.
 */

public class MySimpleAdapter extends BaseAdapter {

    private Context context;
    private List<Map<String, String>> resource;
    private List<Map<String, String>> data;
    private int layoutID;
    private LayoutInflater inflater;
    private MyApplication myApplication = MyApplication.getApplication();
    private ArrayList<Integer> pos = myApplication.getPos();
    private Animation love;
    private List<Integer> deletePos = new ArrayList<>();


    public MySimpleAdapter(Context context, ArrayList<Map<String, String>> resource, int layoutID) {
        this.context = context;
        this.resource = resource;
        this.layoutID = layoutID;
        inflater = LayoutInflater.from(context);
        love = AnimationUtils.loadAnimation(context, R.anim.downloadanim);

        data = myApplication.getData();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ShowOrHideCheckBox");
        intentFilter.addAction("DeleteEnsure");
        Receiver receiver = new Receiver();
        context.registerReceiver(receiver, intentFilter);

        if (resource == myApplication.getRecentdata()) {
            myApplication.setData(resource);
        }
        if (resource == myApplication.getFinaldata()) {
            myApplication.setData(resource);
        }
        if (resource == myApplication.getLikedata()) {
            myApplication.setData(resource);
        }

    }

    @Override
    public int getCount() {
        return resource.size();
    }

    @Override
    public Object getItem(int position) {
        return 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, final View convertView, ViewGroup parent) {

        final View view;
        final ViewHolder viewHolder;

        if (convertView == null) {      //利用ViewHolder 以及 convertView 优化listview

            view = inflater.inflate(layoutID, parent, false);
            viewHolder = new ViewHolder();

            viewHolder.title_tv = (TextView) view.findViewById(R.id.item_title);
            viewHolder.singer_tv = (TextView) view.findViewById(R.id.item_singer);
            viewHolder.love_bt = (ImageButton) view.findViewById(R.id.item_love);
            viewHolder.play_bt = (Button) view.findViewById(R.id.item_play);
            viewHolder.card = (CardView) view.findViewById(R.id.card);
            viewHolder.duration = (TextView) view.findViewById(R.id.item_duration);
            viewHolder.checkBox = (CheckBox) view.findViewById(R.id.cb);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }


        viewHolder.singer_tv.setText(resource.get(position).get("singer"));
        viewHolder.title_tv.setText(resource.get(position).get("title"));
        viewHolder.title_tv.setTextColor(Color.BLACK);


        int duration = Integer.parseInt(resource.get(position).get("duration"));
        String strdur = duration / 1000 / 60 + ":" + duration % 60 + "";
        if (strdur.indexOf(':') == 1)
            strdur = "0" + strdur;
        if (strdur.length() < 5)
            strdur = strdur + "0";
        viewHolder.duration.setText(strdur);            //将毫秒格式的时间 修改成 00:00格式的时间并显示在listview 的item中


        viewHolder.love_bt.setImageResource(R.drawable.ic_blackheart); //默认设置为非红心

        if (pos.size() != 0 && pos != null) {
            for (Integer a : pos) {
                if (resource.get(position).get("duration").equals(a.toString())) {
                    viewHolder.love_bt.setImageResource(R.drawable.love);   //根据一开始读入的 喜欢歌曲的位置信息 来设置 是否为红心
                    break;
                }
            }
        }


        if (resource.get(position).get("isChecked").equals("true")) {
            viewHolder.checkBox.setChecked(true);
        } else {
            viewHolder.checkBox.setChecked(false);
        }

        if (myApplication.isDeleteAll()) {
            viewHolder.checkBox.setVisibility(View.VISIBLE);
        } else {
            viewHolder.checkBox.setVisibility(View.GONE);
        }

        viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resource.get(position).remove("isChecked");
                if (viewHolder.checkBox.isChecked()) {
                    resource.get(position).put("isChecked", "true");
                    deletePos.add(position);
                } else {
                    resource.get(position).put("isChecked", "false");
                    deletePos.remove(Integer.valueOf(position));
                }
            }
        });

        viewHolder.play_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                myApplication.setPosition(position);
                myApplication.setIsPlay(true);
                Intent intent = new Intent("com.example.MainActivity.STARTMUSIC");
                intent.putExtra("POSITION", true);
                intent.putExtra("LOCATION", position);

                context.sendBroadcast(intent);
                //点击后通知主界面更新图标并播放歌曲
                Intent intent2 = new Intent("notification_play_pause");
                intent2.putExtra("LIST", true);                                 //LIST表明点击事件是从LISTVIEW中发生的 播放音乐的位置要手动设置
                context.sendBroadcast(intent2);


            }
        });

        viewHolder.love_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SQLiteDatabase db = myApplication.getDp();
                Cursor cursor = db.query("Like", null, null, null, null, null, null, null);

                ContentValues values = new ContentValues();
                values.put("title", resource.get(position).get("title"));
                values.put("singer", resource.get(position).get("singer"));
                values.put("duration", resource.get(position).get("duration"));
                values.put("data", resource.get(position).get("data"));//准备要写入数据库的信息

                boolean common = false;
                if (cursor.moveToFirst()) {
                    do {
                        if (cursor.getString(cursor.getColumnIndex("title")).equals(values.get("title")) &&
                                cursor.getString(cursor.getColumnIndex("duration")).equals(values.get("duration"))) {

                            common = true;
                            db.delete("Like", "title=?", new String[]{String.valueOf(values.get("title"))});
                            pos.remove(Integer.valueOf(resource.get(position).get("duration")));
                            MySimpleAdapter.this.notifyDataSetChanged();
                            ToastHelper.showToast("已取消收藏");
//                            Toast.makeText(context,"已取消收藏",Toast.LENGTH_SHORT).show();
                            //找到相同的 取消收藏 并删除数据库对应行 pos对应数据

                        }
                    } while (cursor.moveToNext());
                }
                if (!common) {
                    db.insert("Like", null, values);
                    pos.add(Integer.valueOf(resource.get(position).get("duration")));
//                    Toast.makeText(context,"你收藏了该歌曲",Toast.LENGTH_SHORT).show();
                    ToastHelper.showToast("已收藏");
                    MySimpleAdapter.this.notifyDataSetChanged();
                    //若找不到相同的 则收藏歌曲
                }

                v.startAnimation(love);
                cursor.close();
            }

        });

        viewHolder.play_bt.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {

                View contentView = LayoutInflater.from(context).inflate(R.layout.popup, null);
                final PopupWindow popupWindow = new PopupWindow(contentView, 620, WindowManager.LayoutParams.WRAP_CONTENT, true);
                popupWindow.setAnimationStyle(R.style.popup);
                popupWindow.showAsDropDown(viewHolder.love_bt, -360, -20);      // 长按弹出窗口

                final CheckBox checkBox = (CheckBox) contentView.findViewById(R.id.checkBox);

                Button ensure = (Button) contentView.findViewById(R.id.ensure);
                Button quit = (Button) contentView.findViewById(R.id.quit);


                ensure.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SQLiteDatabase db = myApplication.getDp();

                        if (checkBox.isChecked()) {
                            //判断是否删除本地文件 如果为真 则获取文件的地址 并删除 文件 数据库的数据 我喜欢List中的数据
                            String path = data.get(position).get("data");
                            File file = new File(path);

                            if (file.delete()) {
                                ToastHelper.showToast("删除成功");
                            } else {
                                ToastHelper.showToast("删除失败");
                            }

                            db.delete("MyMusic", "title=?", new String[]{resource.get(position).get("title")});

                            for (Integer p : pos) {
                                if (p.equals(Integer.valueOf(resource.get(position).get("position")))) {
                                    pos.remove(p);
                                    break;
                                }
                            }

                        }


                        if (myApplication.getFinaldata() == resource)
                            db.delete("MyMusic", "title=?", new String[]{resource.get(position).get("title")});

                        if (myApplication.getLikedata() == resource) {
                            db.delete("Like", "title=?", new String[]{resource.get(position).get("title")});
                            pos.remove(Integer.valueOf(resource.get(position).get("duration")));
                        }

                        if (myApplication.getRecentdata() == resource)
                            db.delete("Recent", "title=?", new String[]{resource.get(position).get("title")});


                        //遍历查找pos数组(我喜欢的数组) 若存在相同的则去除

                        resource.remove(position);
                        MySimpleAdapter.this.notifyDataSetChanged();
                        //移除相关的数据 并 更新listview

                        popupWindow.dismiss();
                        //关闭popuwindows


                    }
                });

                quit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });

                return true;
            }
        });

        return view;
    }


    private class ViewHolder {
        TextView title_tv;
        TextView singer_tv;
        TextView duration;
        ImageButton love_bt;
        Button play_bt;
        CardView card;
        CheckBox checkBox;
    }

    class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("ShowOrHideCheckBox")) {
                if (!myApplication.isDeleteAll())
                    for (Integer i : deletePos) {
                        resource.get(i).remove("isChecked");
                        resource.get(i).put("isChecked", "false");
                    }
                notifyDataSetChanged();

            }


            if (intent.getAction().equals("DeleteEnsure")) {

                int position;
                SQLiteDatabase db = myApplication.getDp();
                Collections.sort(deletePos);
                boolean isLocal = myApplication.getFinaldata() == resource;
                boolean isLike = myApplication.getLikedata() == resource;
                boolean isRecent = myApplication.getRecentdata() == resource;
                boolean deleteFile = intent.getBooleanExtra("deleteFile", false);

                for (int i = deletePos.size() - 1; i >= 0; i--) {

                    position = deletePos.get(i);

                    if (isLocal)
                        db.delete("MyMusic", "title=?", new String[]{resource.get(position).get("title")});

                    if (isLike) {
                        db.delete("Like", "title=?", new String[]{resource.get(position).get("title")});
                        pos.remove(Integer.valueOf(resource.get(position).get("duration")));
                    }

                    if (isRecent)
                        db.delete("Recent", "title=?", new String[]{resource.get(position).get("title")});

                    if (deleteFile) {
                        String path = resource.get(position).get("data");
                        File file = new File(path);
                        if (file.delete()) {
                            ToastHelper.showToast("删除成功");

                        } else {
                            ToastHelper.showToast("删除失败");
                        }

                        db.delete("MyMusic", "title=?", new String[]{resource.get(position).get("title")});


                        for (Integer p : pos) {
                            if (p.equals(Integer.valueOf(resource.get(position).get("duration")))) {
                                pos.remove(p);
                                break;
                            }    //遍历查找pos数组(我喜欢的数组) 若存在相同的则去除
                        }

                    }

                    resource.remove(position);
                }
                deletePos.clear();
                MySimpleAdapter.this.notifyDataSetChanged();//移除相关的数据 并 更新listview

            }

        }

    }


}
