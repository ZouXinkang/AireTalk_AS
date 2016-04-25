package com.pingshow.amper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.pingshow.amper.bean.Member;
import com.pingshow.amper.db.GroupDB;
import com.pingshow.network.MyNet;
import com.pingshow.util.GroupUpdateMessageSender;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.LBMUtil;
import com.pingshow.util.MyUtil;

import java.util.ArrayList;

/**
 * Created by jack on 2016/4/21.
 */
public class GroupMembersActivity extends Activity{
    public static final int CHOICE = 1;

    private MyPreference mPref;
    private ArrayList<Member> members;
    private GroupDB mGDB;
    private ListView mListView;
    private MyAdapter myAdapter;
    private int groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.members_container);
        initView();

        initData();

        logicProcess();
    }

    private void logicProcess() {
        Intent intent = getIntent();
        String str = intent.getStringExtra("members").trim();
        String[] idxs = str.split(" ");

        groupId = Integer.parseInt(intent.getStringExtra("groupId"));

        for (String idx : idxs) {
            if (idx.equals(mPref.read("myIdx"))){
                continue;
            }
            Member member = new Member();

            member.setIdx(Integer.parseInt(idx));
            member.setNickname(mGDB.getGroupMemberNameByGroupIdxAndMemberIdx(groupId, Integer.parseInt(idx)));
            Drawable photo = ImageUtil.getBitmapAsRoundCorner(Global.SdcardPath_inbox + "photo_" + idx + ".jpg", 1, 4);
            if (photo==null) {
                photo = getResources().getDrawable(R.drawable.bighead);
            }
            member.setPhoto(photo);

            members.add(member);
        }

        if (myAdapter==null) {
            myAdapter = new MyAdapter();
            mListView.setAdapter(myAdapter);
        }else{
            myAdapter.notifyDataSetChanged();
        }
    }

    private void initData() {
        mPref = new MyPreference(this);
        members = new ArrayList<>();

        mGDB = new GroupDB(GroupMembersActivity.this);
        mGDB.open();

    }

    private void initView() {
        ((ImageView) findViewById(R.id.done)).setVisibility(View.GONE);
        ((ImageView) findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mListView = (ListView) findViewById(R.id.lv_listview);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                //请求php更换群主
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String Return="";
                        try {
                            int c = 0;
                            do {
                                MyNet net = new MyNet(GroupMembersActivity.this);
                                Return = net.doPostHttps("update_group_creater.php",
                                        "id=" + mPref.read("myIdx") + "&gid=" + groupId + "&creater="
                                                + members.get(position).getIdx(),
                                        null);
                                Log.d("修改群主  修改群主的返回值: " + Return);
                                if (Return.startsWith("sucess")) {
                                    //发送群主变更消息和群主变更广播
                                    String content = String.format(getString(R.string.group_creater_changed), members.get(position).getNickname());
                                    GroupUpdateMessageSender.getInstance().send(GroupMembersActivity.this, Integer.parseInt(mPref.read("myIdx")), groupId, content);

                                    //发送本地广播
                                    Intent intent = new Intent(Global.Action_Refresh_Groupinfo);
                                    intent.putExtra("Command",Global.CMD_Refresh_Change_Manager);
                                    intent.putExtra("newCreater",  members.get(position).getIdx()+"");
                                    LBMUtil.sendBroadcast(GroupMembersActivity.this, intent);
                                    break;
                                }
                                MyUtil.Sleep(2000);
                            } while (++c < 3);
                        } catch (Exception e) {
                        }
                    }
                }).start();
                setResult(CHOICE);
                finish();

            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mGDB != null && mGDB.isOpen())
            mGDB.close();
        super.onDestroy();
    }

    private class MyAdapter extends BaseAdapter{
        @Override
        public int getCount() {
            return members.size();
        }

        @Override
        public Object getItem(int position) {
            return members.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder  holder;
            if (convertView==null) {
                holder = new ViewHolder();
                convertView = View.inflate(GroupMembersActivity.this,R.layout.user_tiny_cell_new,null);
                holder.memberPhoto = (ImageView)convertView.findViewById(R.id.photo);
                holder.memberName=(TextView)convertView.findViewById(R.id.friendname);
                convertView.setTag(holder);
            }else{
                holder = (ViewHolder)convertView.getTag();
            }

            Member member = members.get(position);
            holder.memberName.setText(member.getNickname());
            holder.memberPhoto.setImageDrawable(member.getPhoto());
            return convertView;
        }

        private class ViewHolder{
            ImageView memberPhoto;
            TextView memberName;
        }
    }
}
