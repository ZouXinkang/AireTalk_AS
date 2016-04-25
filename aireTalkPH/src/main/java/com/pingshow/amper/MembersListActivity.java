package com.pingshow.amper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.*;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.amper.bean.GroupMsg;
import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.GroupDB;
import com.pingshow.amper.db.SmsDB;
import com.pingshow.network.MyNet;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.GroupUpdateMessageSender;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.util.ResizeImage;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jack on 2016/3/18.
 */
public class MembersListActivity extends Activity {
    private ListView mListView;
    private FriendsAdapter adapter;
    private float mDensity = 1.0f;

    //jack db
    private List<Map<String, String>> amperList;

    public static final int SHOWMEMBERS = 0;

    private AmpUserDB mADB;
    private ContactsQuery cq;
    private ArrayList<String> excludeList;
    private StringBuffer nameBuffer;
    private AsyncImageLoader asyncImageLoader;

    private ProgressDialog progress;
    private String members;
    private String groupName;
    private MyPreference mPref;
    private String myIdx;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SHOWMEMBERS:

                    if (adapter == null) {
                        adapter = new FriendsAdapter(MembersListActivity.this);
                        mListView.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }

                    break;
            }
        }
    };
    private String groupID;
    private String mIdx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.members_container);
        mListView = (ListView) findViewById(R.id.lv_listview);
        mDensity = getResources().getDisplayMetrics().density;

        initData();

        mListView.setOnItemClickListener(onChooseUser);

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        findViewById(R.id.done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //更新Adapter所需要的字符串
                StringBuffer idxBuffer = new StringBuffer("");
                //发送加群广播需要的集合
                ArrayList<String> addressList = new ArrayList<String>();
                int count = 0;
                for (int i = 0; i < amperList.size(); i++) {
                    Map<String, String> map = amperList.get(i);
                    if (map.get("checked").equals("2")) {
                        String idx = map.get("idx");
                        idxBuffer.append(idx + " ");
                        nameBuffer.append(mADB.getNicknameByIdx(Integer.parseInt(idx)) + ",");
                        addressList.add(mADB.getAddressByIdx(Integer
                                .parseInt(idx)));
                        count++;
                    }
                }

                //选中的有效人数大于0,判断是否是群组加人还是新生成群组
                if (count > 0) {
                    String idxArray = idxBuffer.toString();
                    if (!TextUtils.isEmpty(groupID)) {
                        // TODO: 2016/3/21 已经是群组,发送将人加入群组的广播,并返回群设置界面

                        //发送广播call Php,4/18防止对象被回收,使用string代替
                        Intent addMembers = new Intent(Global.Action_InternalCMD);
                        addMembers.putExtra("Command", Global.CMD_GROUP_ADD_NEW_MEMBER);
                        addMembers.putExtra("GroupID", Integer.parseInt(groupID));
                        addMembers.putExtra("MembersIdxs", idxArray);
                        sendBroadcast(addMembers);

                        // TODO: 2016/4/6 将加人消息写入数据库 对指定的人发送加群的tcp, 4/25 整合到AireJupiter中
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        // TODO: 2016/3/21 本来不是群组,之后创建群组 ,
                        //开启进入conversationActivity
                        createGroup(idxArray);
                    }
                }else{
                    finish();
                }
            }
        });


    }

    private void refreshGallery() {
        // TODO: 2016/4/18 刷新UserActivity
        UsersActivity.forceRefresh=true;
        Intent intent = new Intent(Global.Action_Refresh_Gallery);
        sendBroadcast(intent);
    }

    private void insertMsg(final String content) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String obligate1 = null;
                if (AireJupiter.getInstance() != null) {  //tml*** china ip
                    obligate1 = AireJupiter.getInstance().getIsoPhp(0, true, null);
                } else {
                    obligate1 = AireJupiter.myPhpServer_default;
                }
                String address = "[<GROUP>]" + groupID;

                ContactsQuery cq = new ContactsQuery(MembersListActivity.this);
                long contactid = cq.getContactIdByNumber(address);

                boolean flag = ConversationActivity.sender != null && MyTelephony.SameNumber(ConversationActivity.sender, "[<GROUP>]" + groupID);
                int read = (flag == true ? 1 : 0);

                String displayname = mADB.getNicknameByAddress(address);
                SmsDB smsDB = new SmsDB(MembersListActivity.this);
                smsDB.open();

                // TODO: 2016/4/7 之后删除
                android.util.Log.d("已知群组添加人", "将加人消息插入数据库:----------------"
                        + "msg.address  " + address
                        + "  msg.contactid  " + contactid
                        + "  msg.time  方法中"
                        + "  msg.read  1"
                        + "  msg.status  -1"
                        + "  msg.type  1"
                        + "  msg.subject  为空串"
                        + "  msg.content  " + content
                        + "  msg.attached  0"
                        + "  msg.att_path_aud  null"
                        + "  msg.att_path_img  null"
                        + "  msg.longitudeE6  0"
                        + "  msg.latitudeE6  0"
                        + "  msg.displayname  " + displayname
                        + "  msg.obligate1  " + obligate1
                        + "  msg.group_member  " + Integer.parseInt(myIdx));
                smsDB.insertMessage(address, contactid, (new Date()).getTime(), read, -1, 1, "", content, 0, null, null, 0, 0, 0, 0, displayname, obligate1, Integer.parseInt(myIdx));
                smsDB.close();
            }
        }).start();
    }

    //jack 创建群组
    private void createGroup(String idxArray) {
        //显示进度条
        progress = ProgressDialog.show(
                MembersListActivity.this, "",
                getString(R.string.in_progress), true, true);

        String[] items = idxArray.split(" ");
        final ArrayList<String> sendeeList = new ArrayList<String>();

        //jack 2.4.51 初始化成员(聊天个人+本身),拼接选择成员,并组合数据
        mIdx = excludeList.get(0);//自己
        members = myIdx+","+mIdx;//自己+聊天的人
        groupName = mPref.read("myNickname") + "、" + mADB.getNicknameByIdx(Integer.parseInt(mIdx));

        for (int i = 0; i < items.length; i++) {
            if (!items[i].equals(mIdx)) {
                sendeeList.add(items[i]);
                members += "," + items[i];
                groupName += "、" + mADB.getNicknameByIdx(Integer.parseInt(items[i]));
            }
        }

        //jack 生成群聊
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO: 2016/3/18 请求指定的php
                String Return;
                try {
                    int c = 0;
                    do {
                        MyNet net = new MyNet(MembersListActivity.this);
                        Return = net.doPostHttps("create_group.php",
                                "id=" + myIdx + "&members=" + members + "&name="
                                        + URLEncoder.encode(groupName, "UTF-8"),
                                null);

                        if (Return.startsWith("Done"))
                            break;
                        MyUtil.Sleep(250);
                    } while (++c < 3);

                    int groupidx = 0;
                    if (Return.startsWith("Done")) {
                        Return = Return.substring(5);
                        groupidx = Integer.parseInt(Return);
                    }

                    if (groupidx == 0) {
                        if (progress != null && progress.isShowing())
                            progress.dismiss();
                        return;
                    }

                    GroupDB gdb = new GroupDB(MembersListActivity.this);
                    gdb.open();
                    //jack 2.4.51 操作数据库
                    sendeeList.add(myIdx);
                    sendeeList.add(mIdx);
                    for (int i = 0; i < sendeeList.size(); i++) {
                        int idx = Integer.parseInt(sendeeList.get(i));
                        // TODO: 2016/4/6  将创建者加入数据库
                        if (myIdx.equals(sendeeList.get(i))){
                            gdb.insertGroup(groupidx, mPref.read("myNickname"), idx,0);
                        }else {
                            gdb.insertGroup(groupidx,  mADB.getNicknameByIdx(idx), idx, 1);
                        }
                    }
                    gdb.close();

                    AmpUserDB mADB = new AmpUserDB(MembersListActivity.this);
                    mADB.open();
                    mADB.insertUser("[<GROUP>]" + groupidx, groupidx + 100000000,
                            groupName);

//                    // TODO: 2016/3/21 创建图片,以后做多用户头像
//                    String photoPath = Global.SdcardPath_sent + "tmp.jpg";
//                    String outFilename = Global.SdcardPath_sent + "temp_group.jpg";
////                    ResizeImage.ResizeXY(MembersListActivity.this, photoPath, outFilename, 320, 100); // tml*** bitmap quality, 240->320
//                    photoPath = outFilename;
//
//                    File f = new File(photoPath);
//                    String localPath = Global.SdcardPath_inbox + "photo_"
//                            + (groupidx + 100000000) + ".jpg";
//                    File f2 = new File(localPath);
//                    f.renameTo(f2);
//
//                    int count = 0;
//                    do {
//                        //jack 2.4.51
//                        MyNet net = new MyNet(MembersListActivity.this);
//                        Return = net.doPostAttach("uploadgroupphoto.php",
//                                groupidx, 0, localPath, AireJupiter.myPhpServer_default2A); // httppost
//                        if (Return.startsWith("Done"))
//                            break;
//                        MyUtil.Sleep(250);
//                    } while (++count < 3);

                    mADB.close();
                    if (progress != null && progress.isShowing())
                        progress.dismiss();

                    //刷新界面
                    refreshGallery();

                    // TODO: 2016/3/21 跳转到群组界面
                    Intent it = new Intent(MembersListActivity.this, ConversationActivity.class);
                    it.putExtra("SendeeNumber", "[<GROUP>]" + groupidx);
                    it.putExtra("SendeeDisplayname", groupName);
                    startActivity(it);
                    //关闭singleSettingActivity/ConversationActivity/MemberListActivity
                    SingleSettingActiivty.mySingleSettingActivity.finish();
                    ConversationActivity.getInstance().finish();
                    finish();

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void initData() {
        //init object data
        mADB = new AmpUserDB(this);
        mADB.open();
        cq = new ContactsQuery(this);

        //增加组员的名字
        nameBuffer = new StringBuffer("");
        mPref = new MyPreference(this);

        amperList = new ArrayList<Map<String, String>>();

        //获取已经在群里的人的集合和GroupID
        excludeList = (ArrayList<String>) (getIntent().getSerializableExtra("Exclude"));
        groupID = getIntent().getStringExtra("GroupID");

        myIdx = Integer.parseInt(mPref.read("myID","0"),16) + "";

        //jack 组装数据
        new Thread(mFetchFriends).start();
    }

    //jack holder
    class FriendsViewHolder {
        TextView friendName;
        ImageView photoimage;
        ImageView checked;
    }

    private class FriendsAdapter extends BaseAdapter {
        public FriendsAdapter(Context context) {
            asyncImageLoader = new AsyncImageLoader(context);
        }

        @Override
        public int getCount() {
            return amperList.size();
        }

        @Override
        public Object getItem(int position) {
            return amperList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Map<String, String> map ;

            try {
                map = amperList.get(position);
            } catch (Exception e) {
                return convertView;
            }

            String imagePath = map.get("imagePath");

            FriendsViewHolder holder;

            if (convertView == null) {
                holder = new FriendsViewHolder();
                convertView = View.inflate(MembersListActivity.this, R.layout.user_tiny_cell_new, null);

                holder.photoimage = (ImageView) convertView.findViewById(R.id.photo);
                holder.friendName = (TextView) convertView.findViewById(R.id.friendname);
                holder.checked = (ImageView) convertView.findViewById(R.id.checked);
                convertView.setTag(holder);
            } else {
                holder = (FriendsViewHolder) convertView.getTag();
            }

            holder.photoimage.setTag(imagePath);
            Drawable cachedImage = asyncImageLoader.loadDrawable(imagePath, new AsyncImageLoader.ImageCallback() {
                public void imageLoaded(Drawable imageDrawable, String path) {
                    ImageView imageViewByTag = (ImageView) mListView.findViewWithTag(path);
                    if (imageViewByTag != null && imageDrawable != null) {
                        imageViewByTag.setImageDrawable(imageDrawable);
                    }
                }
            });

            if (cachedImage != null && imagePath != null)
                holder.photoimage.setImageDrawable(cachedImage);
            else
                holder.photoimage.setImageResource(R.drawable.bighead);

            String disname = map.get("displayName");
            holder.friendName.setText(disname);

            String address = map.get("address");
            int status = ContactsOnline.getContactOnlineStatus(address);
            if (status > 0) {
                Drawable d = MembersListActivity.this.getResources().getDrawable(R.drawable.online_light);
                d.setBounds(0, 0, (int) (15.f * mDensity), (int) (15.f * mDensity));
                SpannableString spannable = new SpannableString("*" + disname);
                ImageSpan icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
                spannable.setSpan(icon, 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.friendName.setText(spannable);
            }

            String checked = map.get("checked");
            if (checked.equals("1")) {
                holder.checked.setImageResource(R.drawable.icon_check_blue);
                holder.checked.setVisibility(View.VISIBLE);
            } else if (checked.equals("2")) {
                holder.checked.setImageResource(R.drawable.okay);
                holder.checked.setVisibility(View.VISIBLE);
            } else {
                holder.checked.setVisibility(View.GONE);
            }

            return convertView;
        }
    }

    Runnable mFetchFriends = new Runnable() {
        public void run() {

            Cursor c = mADB.fetchAllByTime();
            if (c != null && c.moveToFirst()) {
                do {
                    String address = c.getString(1);
                    if (address.startsWith("[<GROUP>]")) continue;
                    int idx = c.getInt(3);
                    if (idx < 50) continue;

                    long contactId = cq.getContactIdByNumber(address);
                    String disName = "";
                    String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
                    File f = new File(userphotoPath);
                    if (!f.exists()) userphotoPath = null;

                    if (contactId > 0)
                        disName = cq.getNameByContactId(contactId);
                    else
                        disName = c.getString(4);

                    if (disName == null || disName.length() == 0)
                        disName = MembersListActivity.this.getString(R.string.unknown_person);


                    HashMap<String, String> map = new HashMap<String, String>();

                    map.put("displayName", disName);
                    map.put("address", address);
                    map.put("idx", idx + "");
                    map.put("checked", "0");
                    map.put("imagePath", userphotoPath);

                    if (excludeList != null)//alec Exclude some users
                    {
                        try {
                            for (String ecidx : excludeList) {
                                if (Integer.parseInt(ecidx) == idx) {
                                    //代表已经选择的
                                    map.put("checked", "1");
                                    break;
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                    //将用户存入map
                    amperList.add(map);
                } while (c.moveToNext());
                c.close();
            }
            mHandler.sendEmptyMessage(SHOWMEMBERS);
        }
    };

    AdapterView.OnItemClickListener onChooseUser = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view,
                                int position, long id) {
            Map<String, String> map = amperList.get(position);
            if (map.get("checked").equals("0")) {
                map.put("checked", "2");
            } else if(map.get("checked").equals("2")){
                    map.put("checked", "0");
            }
            adapter.notifyDataSetInvalidated();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mADB != null && mADB.isOpen())
            mADB.close();
//        amperList.clear();
        nameBuffer.delete(0, nameBuffer.length());
        System.gc();
        System.gc();
    }
}
