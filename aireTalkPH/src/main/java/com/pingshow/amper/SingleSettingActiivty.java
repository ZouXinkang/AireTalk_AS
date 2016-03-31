package com.pingshow.amper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.util.ImageUtil;

import java.util.ArrayList;

/**
 * Created by jack on 2016/3/17.
 */
public class SingleSettingActiivty extends Activity implements View.OnClickListener {
    private ImageView chattotop;
    private ImageView groupmsg;
    private static boolean TOP = false;
    private static boolean SHIELD = false;
    private ImageView cancel;
    private String mIdx;
    private String mNickname;
    private String imagePath;
    private ImageView addToGroup;
    private MyPreference mPref;
    private String myIdx;
    private AmpUserDB mADB;
    private String members;
    private String groupName;
    private ProgressDialog progress;
    //jack very bad code
    public static SingleSettingActiivty mySingleSettingActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlechat_setting);

        mySingleSettingActivity = this;

        chattotop = (ImageView) findViewById(R.id.iv_switch_chattotop);
        groupmsg = (ImageView) findViewById(R.id.iv_switch_groupmsg);
        cancel = (ImageView) findViewById(R.id.cancel);
        addToGroup = (ImageView) findViewById(R.id.iv_avatar2);

        addToGroup.setOnClickListener(this);
        chattotop.setOnClickListener(this);
        groupmsg.setOnClickListener(this);
        cancel.setOnClickListener(this);

        mIdx = getIntent().getStringExtra("mIdx");
        mNickname = getIntent().getStringExtra("mNickname");
        imagePath = getIntent().getStringExtra("imagePath");
        initData(imagePath, mNickname);

        mPref = new MyPreference(this);
        myIdx = Integer.parseInt(mPref.read("myID", "0"), 16) + "";


        mADB = new AmpUserDB(this);
        mADB.open();

    }

    private void initData(String imagePath, String mNickname) {
        ImageView iv_avatar = (ImageView) this.findViewById(R.id.iv_avatar);
        TextView tv_username = (TextView) this.findViewById(R.id.tv_username);


        Drawable photo = ImageUtil.getBitmapAsRoundCorner(imagePath, 1, 4);

        if (photo == null) {
            photo = getResources().getDrawable(R.drawable.bighead);
        }

        iv_avatar.setImageDrawable(photo);
        tv_username.setText(mNickname);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_avatar2:
                // TODO: 2016/3/17 选择好友,创建群组
                Intent intent = new Intent(this, MembersListActivity.class);
                ArrayList<String> excludeList = new ArrayList<String>();
                excludeList.add(mIdx);
                intent.putExtra("Exclude", excludeList);
//                startActivityForResult(intent, 0);
                startActivity(intent);
                break;
            case R.id.cancel:
                finish();
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

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == 0) {
//            if (resultCode == RESULT_OK) {
//
//                //显示进度条
//                progress = ProgressDialog.show(
//                        SingleSettingActiivty.this, "",
//                        getString(R.string.in_progress), true, true);
//
//                String idxArray = data.getStringExtra("idx");
//                String[] items = idxArray.split(" ");
//                final ArrayList<String> sendeeList = new ArrayList<String>();
//
//                //jack 2.4.51 初始化成员(聊天个人+本身),拼接选择成员,并组合数据
//                members = mIdx;
//                groupName = mPref.read("myNickname") + "、" + mADB.getNicknameByIdx(Integer.parseInt(mIdx));
//
//                for (int i = 0; i < items.length; i++) {
//                    if (!items[i].equals(mIdx)) {
//                        sendeeList.add(items[i]);
//                        members += "," + items[i];
//                        groupName += "、" + mADB.getNicknameByIdx(Integer.parseInt(items[i]));
//                    }
//                }
//
//                Log.d("SingleSettingActiivty", "single 1 " + members);
//                Log.d("SingleSettingActiivty", groupName);
//
//                    //jack 生成群聊
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            // TODO: 2016/3/18 请求指定的php
//                            String Return;
//                            try {
//                                int c = 0;
//                                do {
//                                    MyNet net = new MyNet(SingleSettingActiivty.this);
//                                    Return = net.doPostHttps("create_group.php",
//                                            "id=" + myIdx + "&members=" + members + "&name="
//                                                    + URLEncoder.encode(groupName, "UTF-8"),
//                                            null);
//
//                                    com.pingshow.amper.Log.i("SingleSettingActiivty" + Return);
//
//                                    if (Return.startsWith("Done"))
//                                        break;
//                                    MyUtil.Sleep(250);
//                                } while (++c < 3);
//
//                                int groupidx = 0;
//                                if (Return.startsWith("Done")) {
//                                    Return = Return.substring(5);
//                                    groupidx = Integer.parseInt(Return);
//                                }
//
//                                if (groupidx == 0) {
//                                    if (progress != null && progress.isShowing())
//                                        progress.dismiss();
//                                    return;
//                                }
//
//                                GroupDB gdb = new GroupDB(SingleSettingActiivty.this);
//                                gdb.open();
//                                //jack 2.4.51 操作数据库
//                                sendeeList.add(myIdx);
//                                sendeeList.add(mIdx);
//                                for (int i = 0; i < sendeeList.size(); i++) {
//                                    int idx = Integer.parseInt(sendeeList.get(i));
//                                    gdb.insertGroup(groupidx, groupName, idx);
//                                }
//                                gdb.close();
//
//                                AmpUserDB mADB = new AmpUserDB(SingleSettingActiivty.this);
//                                mADB.open();
//                                mADB.insertUser("[<GROUP>]" + groupidx, groupidx + 100000000,
//                                        groupName);
//
//                                // TODO: 2016/3/21 创建图片,以后做多用户头像
//                                String photoPath = Global.SdcardPath_sent + "tmp.jpg";
//                                String outFilename = Global.SdcardPath_sent + "temp_group.jpg";
//                                ResizeImage.ResizeXY(SingleSettingActiivty.this, photoPath, outFilename, 320, 100); // tml*** bitmap quality, 240->320
//                                photoPath = outFilename;
//
//                                File f = new File(photoPath);
//                                String localPath = Global.SdcardPath_inbox + "photo_"
//                                        + (groupidx + 100000000) + ".jpg";
//                                File f2 = new File(localPath);
//                                f.renameTo(f2);
//
//                                int count = 0;
//                                do {
//                                    //jack 2.4.51
//                                    MyNet net = new MyNet(SingleSettingActiivty.this);
//                                    Return = net.doPostAttach("uploadgroupphoto.php",
//                                            groupidx, 0, localPath, AireJupiter.myPhpServer_default2A); // httppost
//                                    if (Return.startsWith("Done"))
//                                        break;
//                                    MyUtil.Sleep(250);
//                                } while (++count < 3);
//
//                                SendAgent agent = new SendAgent(SingleSettingActiivty.this, Integer.parseInt(myIdx), 0,
//                                        true);
//
//                                agent.setAsGroup(groupidx);
//                                ArrayList<String> addressList = new ArrayList<String>();
//                                String allMembers = myIdx + "," + members;
//
//                                Log.d("SingleSettingActiivty", "single 2 " + allMembers);
//
//                                String[] sendeeList = allMembers.split(",");
//                                try {
//                                    for (int i = 0; i < sendeeList.length; i++)
//                                        addressList.add(mADB.getAddressByIdx(Integer
//                                                .parseInt(sendeeList[i])));
//
//                                    Log.d("SingleSettingActiivty", "addressList:" + addressList);
//
//                                    agent.onMultipleSend(addressList, ":)(Y)", 0, null, null);
//                                } catch (Exception e) {
//                                }
//
//                                mADB.close();
//                                if (progress != null && progress.isShowing())
//                                    progress.dismiss();
//
//                                // TODO: 2016/3/21 跳转到群组界面
//                                Intent it = new Intent(SingleSettingActiivty.this, ConversationActivity.class);
//                                it.putExtra("SendeeNumber", "[<GROUP>]" + groupidx);
//                                it.putExtra("SendeeDisplayname", groupName);
//                                startActivity(it);
//                                finish();
//                            } catch (UnsupportedEncodingException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }).start();
//            }
//        }
//    }

    @Override
    protected void onDestroy() {
        if (mADB != null && mADB.isOpen())
            mADB.close();
        System.gc();
        System.gc();
        super.onDestroy();
    }
}
