package com.pingshow.amper;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.*;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.amper.bean.Member;
import com.pingshow.amper.bean.Person;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.GroupDB;
import com.pingshow.amper.view.ExpandGridView;
import com.pingshow.util.ImageUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jack on 2016/3/15.
 * 群组设置界面
 */
public class GroupSettingActivity extends Activity implements View.OnClickListener {

    private ImageView mCancel;
    private ImageView chattotop;

    private static boolean TOP = false;
    private static boolean SHIELD = false;
    private ExpandGridView gridView;

    /**
     * 已在群用户idx
     */
    private ArrayList<String> sendeeList;
    /**
     * 群用户集合
     */
    private ArrayList<Member> members;

    private AmpUserDB mADB;
    private GridAdapter adapter;
    private ImageView groupmsg;
    private String groupname;
    /**
     * 退出分组
     */
    private Button exit_grp;
    /**
     * 用户统计
     */
    private TextView tv_m_total;
    /**
     * 已被删除的成员
     */
    private ArrayList<String> removeMembers;
    private MyPreference mPref;
    private String groupID;
    private StringBuffer nameBuffer;
//    private int rowid;

    private boolean is_admin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);
        //初始化组合数据
        initData();

        //初始化视图和点击事件
        initView();

        //显示成员
        showMembers();
    }

    private void initView() {
        //设置群名称
        ((TextView) findViewById(R.id.tv_groupname)).setText(groupname);

        gridView = (ExpandGridView) findViewById(R.id.gridview);
        mCancel = (ImageView) findViewById(R.id.cancel);
        chattotop = (ImageView) findViewById(R.id.iv_switch_chattotop);
        groupmsg = (ImageView) findViewById(R.id.iv_switch_groupmsg);
        exit_grp = (Button) findViewById(R.id.btn_exit_grp);
        tv_m_total = (TextView) findViewById(R.id.tv_m_total);

        mCancel.setOnClickListener(this);
        chattotop.setOnClickListener(this);
        groupmsg.setOnClickListener(this);
        exit_grp.setOnClickListener(this);
    }

    //初始化数据
    private void initData() {
        mADB = new AmpUserDB(GroupSettingActivity.this);
        mADB.open();

        mPref = new MyPreference(this);

        //群组成员
        members = new ArrayList<Member>();

        //被删除的群组成员
        removeMembers = new ArrayList<String>();

        groupname = getIntent().getStringExtra("groupname");
        groupID = getIntent().getStringExtra("GroupID");

        GroupDB mGDB = new GroupDB(this);
        mGDB.open(true);
        // TODO: 2016/4/6  查询群组中的创建者,如果是创建者才显示删人按钮'
        int creatorIdx = mGDB.getGroupCreator(Integer.parseInt(groupID));
        if (creatorIdx==Integer.parseInt(mPref.read("myIdx")))is_admin=true;

        sendeeList = mGDB.getGroupMembersByGroupIdx(Integer.parseInt(groupID));
        mGDB.close();

        //填充数据
        for (String idx : sendeeList) {
            Member member = getMember(idx);
            members.add(member);
        }
    }

    /**
     * 根据idx获取member对象
     *
     * @param idx
     * @return
     */
    private Member getMember(String idx) {
        Member member = new Member();

        //设置idx
        member.setIdx(Integer.parseInt(idx));
        //设置头像
        String userphotoPath;
        if (Integer.parseInt(idx) == Integer.parseInt(mPref.read("myIdx"))) {
            int uid = Integer.valueOf(mPref.read("myID", "0"), 16);
            userphotoPath = Global.SdcardPath_sent + "myself_photo_" + uid + ".jpg";
        } else
            userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";

        Drawable photo = ImageUtil.getBitmapAsRoundCorner(userphotoPath, 1, 4);
        if (photo == null) {
            photo = getResources().getDrawable(R.drawable.bighead);
        }
        member.setPhoto(photo);
        //设置昵称
        String nickname = mADB.getNicknameByIdx(Integer.parseInt(idx));
        member.setNickname(nickname);
        return member;
    }

    //显示成员头像和昵称的gridview
    private void showMembers() {
        adapter = new GridAdapter(this, members);
        gridView.setAdapter(adapter);
        //设置touchlistener方便用户退出删除模式
        gridView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (adapter.isInDeleteMode) {
                            adapter.isInDeleteMode = false;
                            adapter.notifyDataSetChanged();
                            return true;
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                //关闭页面
                // TODO: 2016/3/23 关闭页面前发送广播,将删除的好友移出group
                removeMembersFromGroup();
                finish();
                break;

            case R.id.btn_exit_grp:
                Toast.makeText(this, "删除并退出", Toast.LENGTH_SHORT).show();
                //// TODO: 2016/3/16 退出进入到信息界面......
                break;

            case R.id.iv_switch_chattotop:
                //是否屏蔽群消息
                if (!TOP) {
                    chattotop.setBackground(getResources().getDrawable(R.drawable.open_icon));
                    TOP = true;
                } else {
                    chattotop.setBackground(getResources().getDrawable(R.drawable.close_icon));
                    TOP = false;
                }
                break;
            case R.id.iv_switch_groupmsg:
                //是否屏蔽群消息
                if (!SHIELD) {
                    groupmsg.setBackground(getResources().getDrawable(R.drawable.open_icon));
                    SHIELD = true;
                } else {
                    groupmsg.setBackground(getResources().getDrawable(R.drawable.close_icon));
                    SHIELD = false;
                }
                break;
        }
    }

    //接受Pickup界面返回数据
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                try {
                    String idxArray = data.getStringExtra("idx");
                    String[] items = idxArray.split(" ");

                    for (int i = 0; i < items.length; i++) {
                        members.add(getMember(items[i]));
                        sendeeList.add(items[i]);
                    }
                    adapter.notifyDataSetChanged();

                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mADB != null && mADB.isOpen())
            mADB.close();
        super.onDestroy();

    }


    /**
     * Created by jack on 2016/3/15.
     */
    private class GridAdapter extends BaseAdapter {

        private List<Member> members;
        private Context context;
        public boolean isInDeleteMode;

        public GridAdapter(Context context, ArrayList<Member> members) {
            this.context = context;
            this.members = members;
            isInDeleteMode = false;
            nameBuffer = new StringBuffer("");

        }

        @Override
        public int getCount() {
            tv_m_total.setText("(" + members.size() + ")");
            if (is_admin) {
                return members.size() + 2;
            } else {
                return members.size() + 1;
            }
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
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(context, R.layout.social_chatsetting_gridview_item, null);
                holder.iv_avatar = (ImageView) convertView.findViewById(R.id.iv_avatar);
                holder.badge_delete = (ImageView) convertView.findViewById(R.id.badge_delete);
                holder.tv_nickname = (TextView) convertView.findViewById(R.id.tv_nickname);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }


            //最后一个item,减人按钮,同时是管理员
            if (position == getCount() - 1 && is_admin) {
                holder.tv_nickname.setText("");
                holder.badge_delete.setVisibility(View.GONE);
                holder.iv_avatar.setImageResource(R.drawable.icon_btn_deleteperson);

                if (isInDeleteMode) {
                    //在删除模式下
                    convertView.setVisibility(View.GONE);
                } else {
                    convertView.setVisibility(View.VISIBLE);
                }
                holder.iv_avatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isInDeleteMode = true;
                        notifyDataSetChanged();
                    }
                });
            } else if ((is_admin && position == getCount() - 2)
                    || (!is_admin && position == getCount() - 1)) {
                //添加群组成员按钮
                holder.tv_nickname.setText("");
                holder.badge_delete.setVisibility(View.GONE);
                holder.iv_avatar.setImageResource(R.drawable.jy_drltsz_btn_addperson);
                // 正处于删除模式下,隐藏添加按钮
                if (isInDeleteMode) {
                    convertView.setVisibility(View.GONE);
                } else {
                    convertView.setVisibility(View.VISIBLE);
                }
                holder.iv_avatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO: 2016/3/23 将移出好友的从group删除
                        removeMembersFromGroup();

                        // TODO: 2016/3/17 使用添加好友页面,PickupActivity
                        Intent it = new Intent(context, MembersListActivity.class);
                        it.putExtra("Exclude", sendeeList);
                        it.putExtra("GroupID", groupID);

                        startActivityForResult(it, 0);
                    }
                });
            } else {
                final Member member = members.get(position);
                String nickname = member.getNickname();
                Drawable avatar = member.getPhoto();
                holder.tv_nickname.setText(nickname);
                holder.iv_avatar.setImageDrawable(avatar);

                if (isInDeleteMode) {
                    //如果是删除模式下,同时不是自己显示删除按钮,
                    if (member.getIdx() != Integer.parseInt(mPref.read("myIdx"))) {
                        holder.badge_delete.setVisibility(View.VISIBLE);
                    } else {
                        //判断一定要加,不加会出现复用的问题
                        holder.badge_delete.setVisibility(View.GONE);
                    }
                } else {
                    holder.badge_delete.setVisibility(View.GONE);
                }
                holder.iv_avatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isInDeleteMode) {
                            //  删除模式下,不能删除自己
                            if (member.getIdx() != Integer.parseInt(mPref.read("myIdx"))) {
                                deleteMemberFromGroup(member);
                            }
                        }
                    }
                });
                holder.badge_delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isInDeleteMode) {
                            //  删除模式下,不能删除自己
                            if (member.getIdx() != Integer.parseInt(mPref.read("myIdx"))) {
                                deleteMemberFromGroup(member);
                            }
                        }
                    }
                });

                convertView.setVisibility(View.VISIBLE);
            }
            return convertView;
        }

        //删除分组内的成员
        private void deleteMemberFromGroup(Member member) {
            // TODO: 2016/3/15 目前只做了群主删除群成员,没做群主退群操作
            members.remove(member);
            //将删除的好友加入集合中
            removeMembers.add(member.getIdx()+"");
            nameBuffer.append(member.getNickname()+",");
            sendeeList.remove(member.getIdx() + "");
            notifyDataSetChanged();
        }

        private class ViewHolder {
            ImageView iv_avatar;
            TextView tv_nickname;
            ImageView badge_delete;
        }
    }

    /**
     * 将好友从group里面删除
     */
    private void removeMembersFromGroup() {
        //发送广播call Php
        Intent deleteMembers = new Intent(Global.Action_InternalCMD);
        deleteMembers.putExtra("Command",Global.CMD_LEAVE_GROUP);
        deleteMembers.putExtra("GroupID", Integer.parseInt(groupID));
        deleteMembers.putStringArrayListExtra("idxs", removeMembers);
        deleteMembers.putExtra("nameBuffer",nameBuffer.toString());
        sendBroadcast(deleteMembers);
        //清空列表
        removeMembers.clear();
        nameBuffer.delete(0,nameBuffer.length());
    }

}
