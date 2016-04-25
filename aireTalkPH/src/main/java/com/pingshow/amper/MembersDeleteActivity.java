package com.pingshow.amper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.pingshow.amper.bean.Member;
import com.pingshow.amper.db.GroupDB;
import com.pingshow.util.ImageUtil;

import java.util.ArrayList;

/**
 * Created by jack on 2016/4/25.
 */
public class MembersDeleteActivity extends Activity{
    public static final int DELETE = 5;
    private ListView mListView;
    private MyPreference mPref;
    private ArrayList<Member> members;
    private GroupDB mGDB;
    private int groupId;
    private MyAdapter myAdapter;
    private ImageView mChecked;
    private ArrayList<Member> membersDelete;
    private StringBuffer idxsBuffer;
    private StringBuffer nameBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.members_container);
        initView();

        initData();

        logicProcess();
    }
    private void initView() {
        ((ImageView) findViewById(R.id.done)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (membersDelete.size()!=0) {
                    //确定要删除人
                    idxsBuffer = new StringBuffer("");
                    nameBuffer = new StringBuffer("");
                    for (Member member : membersDelete) {
                        idxsBuffer.append(member.getIdx() + " ");
                        nameBuffer.append(member.getNickname() + ",");
                    }
                    //发送广播call Php
                    Intent deleteMembers = new Intent(Global.Action_InternalCMD);
                    deleteMembers.putExtra("Command", Global.CMD_LEAVE_GROUP);
                    deleteMembers.putExtra("GroupID", groupId);
                    deleteMembers.putExtra("MembersIdxs", idxsBuffer.toString());
                    deleteMembers.putExtra("nameBuffer", nameBuffer.toString());
                    sendBroadcast(deleteMembers);

                    setResult(DELETE);
                    finish();
                }
            }
        });
        ((ImageView) findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mListView = (ListView) findViewById(R.id.lv_listview);

    }

    private void initData() {
        mPref = new MyPreference(this);
        members = new ArrayList<>();

        //要被删除的群成员
        membersDelete = new ArrayList<>();

        mGDB = new GroupDB(MembersDeleteActivity.this);
        mGDB.open();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (members.get(position).getChecked()==0) {
                    members.get(position).setChecked(1);
                    membersDelete.add(members.get(position));
                }else{
                    members.get(position).setChecked(0);
                    membersDelete.remove(members.get(position));
                }
                myAdapter.notifyDataSetChanged();
            }
        });
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
            member.setChecked(0);//0代表没有选中
            members.add(member);
        }

        if (myAdapter==null) {
            myAdapter = new MyAdapter();
            mListView.setAdapter(myAdapter);
        }else{
            myAdapter.notifyDataSetChanged();
        }

    }


    private class MyAdapter extends BaseAdapter {
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
                convertView = View.inflate(MembersDeleteActivity.this,R.layout.user_tiny_cell_new,null);
                holder.memberPhoto = (ImageView)convertView.findViewById(R.id.photo);
                holder.memberName=(TextView)convertView.findViewById(R.id.friendname);
                holder.memberChecked = (ImageView)convertView.findViewById(R.id.checked);
                convertView.setTag(holder);
            }else{
                holder = (ViewHolder)convertView.getTag();
            }

            Member member = members.get(position);
            holder.memberName.setText(member.getNickname());
            holder.memberPhoto.setImageDrawable(member.getPhoto());

            if (member.getChecked() == 0) {
                holder.memberChecked.setVisibility(View.GONE);
            }else{
                holder.memberChecked.setVisibility(View.VISIBLE);
            }
            return convertView;
        }

        private class ViewHolder{
            ImageView memberPhoto;
            TextView memberName;
            ImageView memberChecked;
        }
    }

    @Override
    protected void onDestroy() {
        if (mGDB != null && mGDB.isOpen())
            mGDB.close();
        super.onDestroy();
    }
}
