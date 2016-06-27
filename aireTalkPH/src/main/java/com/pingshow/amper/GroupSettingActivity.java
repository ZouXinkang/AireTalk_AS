package com.pingshow.amper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.pingshow.amper.bean.Group;
import com.pingshow.amper.bean.Member;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.GroupDB;
import com.pingshow.amper.db.SmsDB;
import com.pingshow.amper.view.ExpandGridView;
import com.pingshow.network.MyNet;
import com.pingshow.util.GroupUpdateMessageSender;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.LBMUtil;
import com.pingshow.util.MyUtil;
import com.pingshow.util.ResizeImage;
import com.pingshow.amper.Log;

import java.io.File;
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
    private GroupDB mGDB;
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
    private StringBuffer idxsBuffer;
    private MyPreference mPref;
    private String groupID;
    private StringBuffer nameBuffer;
//    private int rowid;

    private boolean is_admin = false;
    private ImageView mGroupPhoto;
    private RelativeLayout mChangeGroupCreater;
    private ImageView mRefresh;
    private Animation animation;
    private SmsDB mSmsDB;

    //群组头像
    private String photoPath;
    private Uri uriOrig = null;
    private boolean photoAssigned = false;
    private String localPath;
    private TextView mGroupName;

    private boolean refreshing = false;
    private LoadingDialog loadingDialog;
    private boolean isGroupnameChanged = false;
    private Drawable newGroupPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        //初始化视图和点击事件
        initView();

        //初始化组合数据
        initData();

        //显示成员
        showMembers();
    }

    BroadcastReceiver InternalCommand = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Global.Action_Refresh_Groupinfo)) {
                int command = intent.getIntExtra("Command", 0);
                int groupId = Integer.parseInt(groupID);
                boolean result;
                if (loadingDialog!=null) {
                    loadingDialog.dismiss();
                }
                switch (command) {
                    case Global.CMD_Refresh_Add_Members:

                        //添加用户成功
                        try {
                            String idxArray = intent.getStringExtra("idxs");
                            String[] items = idxArray.split(" ");
                            for (int i = 0; i < items.length; i++) {
                                sendeeList.add(items[i]);
                                members.add(getMember(Integer.parseInt(groupID), items[i]));
                            }
                            adapter.getCount();
                            adapter.notifyDataSetChanged();

                        } catch (Exception e) {
                        }
                        break;
                    case Global.CMD_Refresh_Cut_Members:

                        String[] idxs = intent.getStringExtra("idxs").trim().split(",");
                        for (String idx : idxs) {
                            sendeeList.remove(idx);
                            for (int i = 0; i < members.size(); i++) {
                                if ((members.get(i).getIdx() + "").equals(idx)) {
                                    members.remove(i);
                                }
                            }
                        }
                        adapter.getCount();
                        adapter.notifyDataSetChanged();

                        break;
                    case Global.CMD_Refresh_Rename_Groupname:
                        // TODO: 2016/4/25 设置一个值表示群名称改变
                        isGroupnameChanged = true;
                        groupname = intent.getStringExtra("groupname");
                        //修改conversation界面的群名称
                        mGroupName.setText(groupname);
                        result = mADB.updateGroupname("[<GROUP>]" + groupId, groupId + 100000000,
                                groupname);
                        Log.d("修改组名的结果: " + result);
                        break;
                    case Global.CMD_Refresh_Change_Manager:
                        //群主变更成功
                        try {
                            is_admin = false;
                            mChangeGroupCreater.setVisibility(View.GONE);
//                            mGroupPhoto.setClickable(false);

                            int myIdx = Integer.parseInt(mPref.read("myIdx"));
                            int newCreaterIdx = Integer.parseInt(intent.getStringExtra("newCreater"));

                            result = mGDB.updateGroupCreater(groupId, myIdx, newCreaterIdx);
                            Log.d("修改群主结果: " + result);

                            adapter.getCount();
                            adapter.notifyDataSetChanged();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case Global.CMD_Refresh_Logout_Group:
                        //关闭之前的页面,
                        ConversationActivity instance = ConversationActivity.getInstance();
                        if (instance != null) {
                            instance.finish();
                        }
                        finish();
                        break;
                    case Global.CMD_Refresh_Group_Photo:
                        //设置页面显示
                        mGroupPhoto.setImageDrawable(newGroupPhoto);
                        mGroupPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        //刷新界面
                        UsersActivity.forceRefresh = true;
                        UsersActivity.needRefresh = true;
                        Intent it = new Intent(Global.Action_Refresh_Gallery);
                        sendBroadcast(it);
                        break;

                    case Global.CMD_Refresh_Successful:
                        //重新设置数据
                        int creatorIdx = mGDB.getGroupCreator(groupId);
                        if (creatorIdx == Integer.parseInt(mPref.read("myID", "0"), 16)) {
                            is_admin = true;
                        } else {
                            is_admin = false;
                        }

                        sendeeList = mGDB.getGroupMembersByGroupIdx(groupId);
                        Log.d("群组加人:  groupID" + groupID + "groupname" + groupname + "sendeeList.size():" + sendeeList.size());

                        //填充数据
                        members.clear();
                        for (String idx : sendeeList) {
                            Member member = getMember(groupId, idx);
                            members.add(member);
                        }

                        if (!is_admin) {
                            mChangeGroupCreater.setVisibility(View.GONE);
//                            mGroupPhoto.setClickable(false);
                        } else {
                            mChangeGroupCreater.setVisibility(View.VISIBLE);
//                            mGroupPhoto.setClickable(true);
                        }

                        mRefresh.clearAnimation();
                        //显示新的群组头像
                        showGroupPhoto();
                        //显示最新的群名称
                        mGroupName.setText(intent.getStringExtra("DownloadGroupname"));

                        adapter.getCount();
                        adapter.notifyDataSetChanged();

                        // TODO: 2016/4/18 刷新UserActivity
                        refreshing = false;
                        break;
                    case Global.CMD_Refresh_Failed:
//                        if (mRefresh!=null) {
                            mRefresh.clearAnimation();
                        refreshing = false;
//                        }
                        Toast.makeText(GroupSettingActivity.this, getResources().getString(R.string.refresh_failed), Toast.LENGTH_SHORT).show();
                        break;

                    case Global.CMD_Close_Activity:
                        int removeGroupId = Integer.parseInt(intent.getStringExtra("GroupId"));
                        if (removeGroupId==groupId) {
                            ConversationActivity ca = ConversationActivity.getInstance();
                            if (ca != null) {
                                ca.finish();
                            }
                           finish();
                        }
                        break;

                    case Global.CMD_Refresh_Group_Member:
                        int needToRefreshGroupId = Integer.parseInt(intent.getStringExtra("GroupId"));
                        if (needToRefreshGroupId == groupId) {
                            sendeeList = mGDB.getGroupMembersByGroupIdx(Integer.parseInt(groupID));
                            Log.d("群组成员变动:  groupID" + groupID + "groupname" + groupname + "sendeeList.size():" + sendeeList.size());

                            //填充数据
                            members.clear();
                            for (String idx : sendeeList) {
                                Member member = getMember(Integer.parseInt(groupID), idx);
                                members.add(member);
                            }
                            adapter.getCount();
                            adapter.notifyDataSetChanged();
                        }
                        break;
                }
                sendRefreshCommand();
            }
        }
    };

    private void initView() {
        groupname = getIntent().getStringExtra("groupname");
        groupID = getIntent().getStringExtra("GroupID");
        // TODO: 2016/4/20 路径
        localPath = Global.SdcardPath_inbox + "photo_"
                + (Integer.parseInt(groupID) + 100000000) + ".jpg";

        //设置群名称
        mGroupName = (TextView) findViewById(R.id.tv_groupname);
        mGroupName.setText(groupname);
        RelativeLayout mChangeGroupName = (RelativeLayout) findViewById(R.id.re_change_groupname);

        gridView = (ExpandGridView) findViewById(R.id.gridview);
        mCancel = (ImageView) findViewById(R.id.cancel);
        chattotop = (ImageView) findViewById(R.id.iv_switch_chattotop);
        groupmsg = (ImageView) findViewById(R.id.iv_switch_groupmsg);
        exit_grp = (Button) findViewById(R.id.btn_exit_grp);
        tv_m_total = (TextView) findViewById(R.id.tv_m_total);

        animation = AnimationUtils.loadAnimation(this, R.anim.grouprefresh);
        LinearInterpolator lin = new LinearInterpolator();
        animation.setInterpolator(lin);

        //强制刷新
        mRefresh = (ImageView) findViewById(R.id.iv_refresh);
        mRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //播放动画
                if (!refreshing) {
                    mRefresh.clearAnimation();
                    refreshing = true;
                    mRefresh.setAnimation(animation);
                    //重新查询,并更新界面
                    Log.d("刷新group  删除分组后,查询分组并写入数据库");
                    requeryGroupInfo(groupID);
                }
            }
        });

        // TODO: 2016/4/19 群主才有的功能
        mGroupPhoto = (ImageView) findViewById(R.id.group_photo);
        mChangeGroupCreater = (RelativeLayout) findViewById(R.id.re_change_group_creater);

        //设置群头像
        showGroupPhoto();

        mGroupPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPickPictureOption();
            }
        });

        mCancel.setOnClickListener(this);
        chattotop.setOnClickListener(this);
        groupmsg.setOnClickListener(this);
        exit_grp.setOnClickListener(this);
        //群主管理权移交
        mChangeGroupCreater.setOnClickListener(this);
        //改群名字
        mChangeGroupName.setOnClickListener(this);
    }

    //初始化数据
    private void initData() {
        IntentFilter intentToReceiveFilter = new IntentFilter();
        intentToReceiveFilter.addAction(Global.Action_Refresh_Groupinfo);
        LBMUtil.registerReceiver(this, InternalCommand, intentToReceiveFilter);

        mADB = new AmpUserDB(GroupSettingActivity.this);
        mADB.open();

        mGDB = new GroupDB(GroupSettingActivity.this);
        mGDB.open();

        mSmsDB = new SmsDB(GroupSettingActivity.this);
        mSmsDB.open();

        mPref = new MyPreference(this);

        //群组成员
        members = new ArrayList<Member>();
        sendeeList = new ArrayList<>();

        //被删除的群组成员
        idxsBuffer = new StringBuffer("");

        // TODO: 2016/4/6  查询群组中的创建者,如果是创建者才显示删人按钮'
        int creatorIdx = mGDB.getGroupCreator(Integer.parseInt(groupID));
        if (creatorIdx == Integer.parseInt(mPref.read("myID", "0"), 16)) is_admin = true;

        //顺序要在click方法下
        if (!is_admin) {
            mChangeGroupCreater.setVisibility(View.GONE);
//            mGroupPhoto.setClickable(false);
        }

        sendeeList.clear();
        sendeeList = mGDB.getGroupMembersByGroupIdx(Integer.parseInt(groupID));
        Log.d("群组加人:  groupID" + groupID + "groupname" + groupname + "sendeeList.size():" + sendeeList.size());

        //填充数据
        members.clear();
        for (String idx : sendeeList) {
            Member member = getMember(Integer.parseInt(groupID), idx);
            members.add(member);
        }
    }


    private void showGroupPhoto() {
        Drawable photo = ImageUtil.getBitmapAsRoundCorner(localPath, 3,
                10);// alec
        if (photo != null) {
            mGroupPhoto.setImageDrawable(photo);
            mGroupPhoto
                    .setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            mGroupPhoto.setImageResource(R.drawable.bighead);
            mGroupPhoto
                    .setScaleType(ImageView.ScaleType.FIT_CENTER);
        }

    }

    private void onPickPictureOption() {
        final CharSequence[] items = {
                getResources().getString(R.string.photo_gallery),
                getResources().getString(R.string.takepicture)};
        final CharSequence[] items_noCamera = {getResources().getString(
                R.string.photo_gallery)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0)
                    onPickPicture();
                else if (item == 1)
                    onTakePicture();
                dialog.dismiss();
            }
        });

        builder.setTitle(this.getResources().getString(
                R.string.choose_photo_source));
        builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {

                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @SuppressLint("ShowToast")
    private void onTakePicture() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            photoPath = Global.SdcardPath_sent + "tmp.jpg";
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(new File(photoPath)));
            startActivityForResult(intent, 20);
        } catch (Exception e) {
            Toast.makeText(this, R.string.take_picture_error,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void onPickPicture() {
        Intent intent = new Intent(Intent.ACTION_PICK);  //li*** fill picture
        intent.setType("image/*");

        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 240);
        intent.putExtra("outputY", 240);
        //li*** fill picture
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);
        intent.putExtra("return-data", true);
        startActivityForResult(intent, 1);
    }

    private void requeryGroupInfo(final String groupID) {
        final int mGroupID = Integer.parseInt(groupID);
        //显示弹窗
        showLoadingDialog(getResources().getString(R.string.Refreshing));
        new Thread(new Runnable() {

            @Override
            public void run() {
                GroupDB gdb = new GroupDB(getApplicationContext());
                gdb.open();
                AmpUserDB adb = new AmpUserDB(getApplicationContext());
                adb.open();
                SmsDB smsDB = new SmsDB(getApplicationContext());
                smsDB.open();

                String Return = "";
                Group groupInfo = null;
                Gson gson = null;
                try {
                    int c = 0;
                    do {
                        MyNet net = new MyNet(GroupSettingActivity.this);
                        Return = net.doPostHttps("query_group_v2.php", "id=" + mGroupID, null);
                        com.pingshow.amper.Log.i("query_group Return=" + Return);
                        gson = new Gson();
                        groupInfo = gson.fromJson(Return, Group.class);
                        //请求成功
                        if (groupInfo.getCode() == 200) break;
                        MyUtil.Sleep(2500);
                    } while (++c < 3);
                } catch (Exception e) {
                    //请求网络失败或者数据返回错误
                    Intent intent = new Intent(Global.Action_Refresh_Groupinfo);
                    intent.putExtra("Command", Global.CMD_Refresh_Failed);
                    LBMUtil.sendBroadcast(GroupSettingActivity.this, intent);
                    return;
                }
                if (groupInfo != null) {
                    if (groupInfo.getCode() == 200) {
                        String downloadGroupname = groupInfo.getGname();

                        // TODO: 2016/4/5  存入组信息,4/18 groupname
                        adb.updateGroupname("[<GROUP>]" + mGroupID, mGroupID + 100000000,
                                downloadGroupname);

                        //获取群组信息成功
                        List<Group.MembersEntity> members = groupInfo.getMembers();
                        Boolean inThisGroup = false;
                        //删除数据库群组
                        gdb.deleteGroup(mGroupID);
                        for (Group.MembersEntity member : members) {
                            //判断是否还在群中
                            if (member.getIdx().equals(mPref.read("myIdx"))) inThisGroup = true;
                            gdb.insertGroup(mGroupID, member.getNickname(), Integer.parseInt(member.getIdx()), member.getRank());

                            //下载头像
                            String localfile = Global.SdcardPath_inbox + "photo_" + member.getIdx() + ".jpg";
                            boolean ret = AireJupiter.getInstance().downloadPhoto(Integer.parseInt(member.getIdx()), localfile);
                            Log.d("GroupSettingActivity  本地文件: " + localfile + " ret:" + ret);
                            if (ret)//delete big one
                                new File(Global.SdcardPath_inbox + "photo_" + member.getIdx() + "b.jpg").delete();
                        }
                        // TODO: 2016/4/7 不在此群组中,删除此群
                        if (!inThisGroup) {
                            adb.deleteContactByAddress("[<GROUP>]" + mGroupID);
                            gdb.deleteGroup(mGroupID);

                            try {
                                smsDB.deleteThreadByAddress("[<GROUP>]" + mGroupID);
                            } catch (Exception e) {
                            }
                            // TODO: 2016/4/12  发送广播隐藏群组显示
                            UsersActivity.needRefresh = true;
                            Intent hideintent = new Intent(Global.Action_Hide_Group_Icon);
                            hideintent.putExtra("GroupId",groupID);
                            sendBroadcast(hideintent);

                            sendRefreshCommand();
                            //关闭当前页面
                            ConversationActivity instance = ConversationActivity.getInstance();
                            if (instance != null) {
                                instance.finish();
                            }
                            finish();
                            //不在这个群中就不下载照片了
                            return;
                        }

                        //下载图片,不管原图片是否存在,都去下载
                        try {
                            String localfile = Global.SdcardPath_inbox + "photo_" + (mGroupID + 100000000) + ".jpg";
                            File f = new File(localfile);
                            String remotefile = "groups/thumbs/photo_" + mGroupID + ".jpg";
                            AireJupiter.getInstance().downloadAnyPhoto(remotefile, localfile, 3, true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //所有操作完成
                        Intent intent = new Intent(Global.Action_Refresh_Groupinfo);
                        intent.putExtra("Command", Global.CMD_Refresh_Successful);
                        intent.putExtra("DownloadGroupname", downloadGroupname);
                        LBMUtil.sendBroadcast(GroupSettingActivity.this, intent);
                    }
                    adb.close();
                    gdb.close();
                    smsDB.close();
                } else {
                    //请求网络失败或者数据返回错误
                    Intent intent = new Intent(Global.Action_Refresh_Groupinfo);
                    intent.putExtra("Command", Global.CMD_Refresh_Failed);
                    LBMUtil.sendBroadcast(GroupSettingActivity.this, intent);
                }
            }
        }).start();
    }

    private void sendRefreshCommand() {
        UsersActivity.forceRefresh = true;
        Intent refreshIntent = new Intent(Global.Action_Refresh_Gallery);
        sendBroadcast(refreshIntent);
    }


    /**
     * 根据idx获取member对象
     *
     * @param idx
     * @return
     */
    private Member getMember(int groupId, String idx) {
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
        //设置昵称,因为广播需要时间所以做判断
        String nickname = mADB.getNicknameByIdx(Integer.parseInt(idx));
        if (nickname != null) {
            //陌生人的话
            nickname = mGDB.getGroupMemberNameByGroupIdxAndMemberIdx(groupId, Integer.parseInt(idx));
        }
        member.setNickname(nickname);
        return member;
    }

    //显示成员头像和昵称的gridview
    private void showMembers() {
        adapter = new GridAdapter(this, members);
        gridView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (isGroupnameChanged) {
            Intent intent = new Intent();
            intent.putExtra("newGroupName", groupname);
            setResult(1, intent);
        }
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                //关闭页面
                // TODO: 2016/3/23 关闭页面前发送广播,将删除的好友移出group
                if (isGroupnameChanged) {
                    Intent intent = new Intent();
                    intent.putExtra("newGroupName", groupname);
                    setResult(1, intent);
                }
                finish();
                break;
            case R.id.re_change_group_creater:
                //跳转到群成员界面移交管理权
                Intent intent1 = new Intent(this, GroupMembersActivity.class);
                StringBuffer idxs = new StringBuffer();
                for (String idx : sendeeList) {
                    idxs.append(idx + " ");
                }
                intent1.putExtra("members", idxs.toString().trim());
                intent1.putExtra("groupId", groupID);
                startActivityForResult(intent1, 0);

                break;
            case R.id.re_change_groupname:
                Intent intent2 = new Intent(this, GroupNameActivity.class);
                intent2.putExtra("groupId", groupID);
                intent2.putExtra("groupname", groupname);
                startActivityForResult(intent2, 0);
                break;
            case R.id.btn_exit_grp:
                if (is_admin) {
                    //如果是群主,要求移交管理权权限
                    Toast.makeText(this, "请移交管理员权限", Toast.LENGTH_SHORT).show();
                    break;
                }

                Intent it = new Intent(Global.Action_InternalCMD);
                it.putExtra("Command", Global.CMD_DELETE_GROUP);
                it.putExtra("GroupID", Integer.parseInt(groupID));
                sendBroadcast(it);

                showLoadingDialog(getResources().getString(R.string.waiting));

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
            //群信息操作
            if (resultCode == RESULT_OK) {
                //添加人进群
                showLoadingDialog(getResources().getString(R.string.adding));
            } else if (resultCode == 1) {
                //选择了新的群主
                showLoadingDialog(getResources().getString(R.string.transferring));
            } else if (resultCode == 3) {
                //更新群名称
                showLoadingDialog(getResources().getString(R.string.saving));
            } else if (resultCode == 5) {
                //删除群成员
                showLoadingDialog(getResources().getString(R.string.deleting));
            }

        } else if (requestCode == 20) {
            boolean HDSize = false;
            try {
                BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                bitmapOptions.inJustDecodeBounds = true;
                bitmapOptions.inPurgeable = true;
                BitmapFactory.decodeFile(photoPath, bitmapOptions);
                if (bitmapOptions.outHeight > 1000)
                    HDSize = true;
            } catch (Exception e) {
            } catch (OutOfMemoryError e) {
            }
            if (HDSize) {
                Bitmap bmp = ImageUtil.loadBitmapSafe(2, photoPath);
                try {
                    uriOrig = Uri.parse(MediaStore.Images.Media
                            .insertImage(getContentResolver(), bmp, null,
                                    null));
                } catch (Exception e) {
                }
            } else {
                try {
                    uriOrig = Uri.parse(MediaStore.Images.Media
                            .insertImage(getContentResolver(), photoPath,
                                    null, null));
                } catch (Exception e) {
                } catch (OutOfMemoryError e) {
                }
            }
            startActivityForResult(getCropImageIntent(uriOrig), 3);
        } else if (requestCode == 1 || requestCode == 3) {
            if (data == null)
                return;
            String SrcImagePath = "";
            try {
                Uri uri = null;
                Bitmap bitmap = data.getParcelableExtra("data");
                uri = Uri.parse(MediaStore.Images.Media.insertImage(
                        getContentResolver(), bitmap, null, null));
                SrcImagePath = getPath(uri);

                String outFilename = Global.SdcardPath_sent
                        + "temp_group.jpg";
                ResizeImage.ResizeXY(this, SrcImagePath, outFilename, 320,
                        100); // tml*** bitmap quality, 240->320

                photoPath = outFilename;
                photoAssigned = true;

                if (uriOrig != null)
                    getContentResolver().delete(uriOrig, null, null);
                getContentResolver().delete(uri, null, null);

                if (requestCode == 3)// taken from camera
                {
                    Intent it = new Intent(GroupSettingActivity.this,
                            PictureRotationActivity.class);
                    it.putExtra("photoPath", outFilename);
                    startActivityForResult(it, 7);
                } else {
                    newGroupPhoto = ImageUtil.getBitmapAsRoundCorner(
                            outFilename, 3, 10);
                    if (newGroupPhoto != null) {
                        renameFileAndRefreash();
                    }
                }

            } catch (Exception e) {
            }
        } else if (requestCode == 7) {
            newGroupPhoto = ImageUtil.getBitmapAsRoundCorner(photoPath, 3,
                    10);// alec
            if (newGroupPhoto != null) {
                renameFileAndRefreash();
            }
        }

    }

    private void showLoadingDialog(String description) {
        loadingDialog = new LoadingDialog(this, description);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.show();
    }

    private void renameFileAndRefreash() {
        showLoadingDialog(getResources().getString(R.string.photo_uploading));
        if (photoAssigned) {
            File f = new File(photoPath);
            File f2 = new File(localPath);
            f.renameTo(f2);
            // TODO: 2016/4/20 上传照片
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String Return;
                        int count = 0;
                        do {
                            //jack 2.4.51
                            MyNet net = new MyNet(GroupSettingActivity.this);
                            Return = net.doPostAttach("uploadgroupphoto.php",
                                    Integer.parseInt(groupID), 0, localPath, AireJupiter.myPhpServer_default2A); // httppost
                            if (Return.startsWith("Done"))
                                break;
                            MyUtil.Sleep(2000);
                        } while (++count < 3);
                        if (Return.startsWith("Done")) {
                            //发送更换头像的tcp
//                            String content = String.format(getString(R.string.group_photo_changed), mPref.read("myNickname"));

                            GroupUpdateMessageSender.getInstance().send(GroupSettingActivity.this, Integer.parseInt(mPref.read("myIdx")), Integer.parseInt(groupID), "Group_Photo",mPref.read("myNickname"),null,mPref.read("myIdx"));

                            //发送本地广播
                            Intent intent = new Intent(Global.Action_Refresh_Groupinfo);
                            intent.putExtra("Command", Global.CMD_Refresh_Group_Photo);
                            LBMUtil.sendBroadcast(GroupSettingActivity.this, intent);
                        } else {
                            //请求网络失败或者数据返回错误
                            Intent intent = new Intent(Global.Action_Refresh_Groupinfo);
                            intent.putExtra("Command", Global.CMD_Refresh_Failed);
                            LBMUtil.sendBroadcast(GroupSettingActivity.this, intent);
                        }
                    } catch (Exception e) {
                    }
                }
            }).start();
        }
    }

    @Override
    protected void onDestroy() {
        LBMUtil.unregisterReceiver(this, InternalCommand);
        if (mADB != null && mADB.isOpen())
            mADB.close();
        if (mGDB != null && mGDB.isOpen())
            mGDB.close();
        if (mSmsDB != null && mSmsDB.isOpen())
            mSmsDB.close();

        super.onDestroy();

    }


    /**
     * Created by jack on 2016/3/15.
     */
    private class GridAdapter extends BaseAdapter {
        private List<Member> members;
        private Context context;

        public GridAdapter(Context context, ArrayList<Member> members) {
            this.context = context;
            this.members = members;
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
                holder.tv_nickname = (TextView) convertView.findViewById(R.id.tv_nickname);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }


            //最后一个item,减人按钮,同时是管理员
            if (position == getCount() - 1 && is_admin) {
                holder.tv_nickname.setText("");
                holder.iv_avatar.setImageResource(R.drawable.icon_btn_deleteperson);
                convertView.setVisibility(View.VISIBLE);
                holder.iv_avatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO: 2016/4/25 跳转到删除群成员页面
                        Intent it = new Intent(context, MembersDeleteActivity.class);
                        StringBuffer idxs = new StringBuffer();
                        for (String idx : sendeeList) {
                            idxs.append(idx + " ");
                        }
                        it.putExtra("members", idxs.toString().trim());
                        it.putExtra("groupId", groupID);
                        startActivityForResult(it, 0);
                    }
                });
            } else if ((is_admin && position == getCount() - 2)
                    || (!is_admin && position == getCount() - 1)) {
                //添加群组成员按钮
                holder.tv_nickname.setText("");
                holder.iv_avatar.setImageResource(R.drawable.jy_drltsz_btn_addperson);
                // 正处于删除模式下,隐藏添加按钮
                holder.iv_avatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

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

                convertView.setVisibility(View.VISIBLE);
            }
            return convertView;
        }

        private class ViewHolder {
            ImageView iv_avatar;
            TextView tv_nickname;
        }
    }

    public static Intent getCropImageIntent(Uri photoUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(photoUri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 240);
        intent.putExtra("outputY", 240);
        intent.putExtra("return-data", true);
        return intent;
    }

    public String getPath(Uri uri) {
        if (uri.toString().startsWith("content:")) {
            try {
                String[] projection = {MediaStore.Images.Media.DATA};
                Cursor cursor = managedQuery(uri, projection, null, null, null);
                int column_index = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor != null) {
                    cursor.moveToFirst();
                    String path = cursor.getString(column_index);
                    Log.d("content://  uri " + uri.toString());
                    Log.d("content://  path " + path);
                    return path;
                }
            } catch (Exception e) {
            }
        } else if (uri.toString().startsWith("file:")) {
//            String uriStr = uri.toString();
//            android.util.Log.d("GroupSettingActivity", "uriStr "+uriStr.toString());
//            return uriStr.substring(uriStr.indexOf("sdcard"));
            String uriStr = uri.toString();
            Log.d("file://  uri " + uri.toString());
            if (uriStr.indexOf("sdcard") == -1) {
                Log.d("判断Url不包含sdcard字段  " + uriStr.substring(uriStr.indexOf("/storage")));
                return uriStr.substring(uriStr.indexOf("/storage"));
            } else {
                Log.d("判断Url包含sdcard字段  " + uriStr.substring(uriStr.indexOf("sdcard")));
                return uriStr.substring(uriStr.indexOf("sdcard"));
            }
        }
        return "";
    }
}
