package com.pingshow.amper;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.amper.bean.FriendsEntity;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.view.RefreshableView;
import com.pingshow.amper.view.indexableListView.IndexEntity;
import com.pingshow.amper.view.indexableListView.IndexHeaderEntity;
import com.pingshow.amper.view.indexableListView.IndexableStickyListView;
import com.pingshow.util.MyUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by jack on 2016/5/19.
 * 创建字母索引列表
 */
public class UsersNewActivity extends Activity{

    /**
     * View
     */
    private SearchView mSearchView;
    private View headerView;
    private IndexableStickyListView mIndexableStickyListView;

    /**
     * 朋友集合
     */
    public static int numTrueFriends = 1;
    public static final int MAX_USERS = 300;
    private List<FriendsEntity> friends = new ArrayList<FriendsEntity>();
    private List<FriendsEntity> orgFriends = new ArrayList<>();

    /**
     * 数据库
     */
    private AmpUserDB mADB;
    private ContactsQuery cq;
    boolean queryFlag = true;

    /**
     * 等待对话框
     */
    private LoadingDialog loadingDialog;

    /**
     * 判断是否是大屏
     */
    private boolean largeScreen;
    private float mDensity = 1.f;

    /**
     * 下拉刷新
     */
    private RefreshableView refreshableView;
    private boolean refresh = true;//jack 控制刷新的boolean变量


    public static final int QUERY_FINISH = 0;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case QUERY_FINISH:
//                    if (loadingDialog != null) {
//                        loadingDialog.dismiss();
//                    }
                    boolean result = (boolean) msg.obj;
                    if (result) {
                        Log.d("queryResult: " + result);
                        Log.d("friends集合" + friends);

                        FriendsAdapter friendsAdapter = new FriendsAdapter(UsersNewActivity.this, largeScreen, mDensity);
                        mIndexableStickyListView.setAdapter(friendsAdapter);
                        //绑定数据
                        mIndexableStickyListView.bindDatas(friends,header);
                    } else {
                        //查询失败,不再显示
                        Toast.makeText(UsersNewActivity.this, "查询失败,不再显示", Toast.LENGTH_SHORT).show();
                    }

                    break;
            }

        }
    };
    private IndexHeaderEntity<FriendsEntity> header;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_friends);
        //初始化视图
        initView();

        //初始化数据
        initData();

        //初始化事件
        initEvent();
    }

    private void initView() {
        //初始化searchView
        initSearchView();

        //设置headerView
//        initHeaderView();

        //设置indexableListView
        initIndexView();
    }

    private void initData() {
        //初始化DB
        mADB = new AmpUserDB(this);
        mADB.open();
        cq = new ContactsQuery(this);

        // TODO: 2016/5/20 判断是否是大屏 large的UI还没有写
        largeScreen = (findViewById(R.id.large) != null);

        mDensity = getResources().getDisplayMetrics().density;

        //查询数据库
        onDBQuery();

    }

    /**
     * 查询数据库并组合集合
     *
     * @return true 查询成功 false 查询失败
     */
    private void onDBQuery() {
//        showLoadingDialog(getResources().getString(R.string.waiting));
        //重置数据
        if (friends != null) {
            friends.clear();
        }

        if (orgFriends != null) {
            orgFriends.clear();
        }

        // TODO: 2016/5/21 显示群聊和新的朋友
        ArrayList<FriendsEntity> friendsHeaderList = new ArrayList<>();
        FriendsEntity headerEntity1 = new FriendsEntity();
        headerEntity1.setName("☆");
        headerEntity1.setDisplayName(getResources().getString(R.string.new_friends));//新的朋友
        FriendsEntity headerEntity2 = new FriendsEntity();
        headerEntity2.setDisplayName(getResources().getString(R.string.saved_groups));//群聊
        headerEntity2.setName("☆");
        friendsHeaderList.add(headerEntity1);
        friendsHeaderList.add(headerEntity2);

        header = new IndexHeaderEntity<>();
        header.setHeaderTitle("☆");
        header.setIndex("☆");
        header.setHeaderList(friendsHeaderList);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = mADB.fetchAllByTime();

                numTrueFriends = cursor.getCount();

                if (!cursor.moveToFirst()) {
                    queryFlag = false;
                } else {
                    int unknowns = 0;
                    FriendsEntity friendsEntity;

                    do {
                        String address = cursor.getString(1);
                        if (address.startsWith("Done=")) {
                            mADB.deleteContactByAddress(address);
                            continue;
                        } else if (address.startsWith("[<GROUP>]")) {
                            //过滤出群组
                            continue;
                        }

                        int idX = cursor.getInt(3);
                        long contactId = cq.getContactIdByNumber(address);
                        String disName = "";
                        String userphotoPath;

                        userphotoPath = Global.SdcardPath_inbox + "photo_" + idX + "b.jpg";
                        if (!new File(userphotoPath).exists()) {
                            userphotoPath = Global.SdcardPath_inbox + "photo_" + idX + ".jpg";
                            if (!new File(userphotoPath).exists())
                                userphotoPath = null;
                        }

                        if (contactId > 0)
                            disName = cq.getNameByContactId(contactId);
                        else
                            disName = cursor.getString(4);

                        if (disName == null || disName.length() == 0) {
                            disName = getString(R.string.unknown_person);
                            unknowns++;  //tml|james*** unknown contacts error/
                        }

                        friendsEntity = new FriendsEntity();
                        friendsEntity.setName(disName);
                        friendsEntity.setDisplayName(disName);
                        friendsEntity.setAddress(address);
                        friendsEntity.setIdx(idX + "");
                        friendsEntity.setImagePath(userphotoPath);
                        friendsEntity.setContactId(contactId + "");
                        friendsEntity.setSeperator("0");
                        friendsEntity.setBlocked("0");
                        friendsEntity.setActual("1");

                        String mood = mADB.getMoodByAddress(address);
                        if (TextUtils.isEmpty(mood)) {
                            friendsEntity.setMood("");
                        } else {
                            friendsEntity.setMood(mood);
                        }

                        friends.add(friendsEntity);
                        orgFriends.add(friendsEntity);

                        //jack 依据状态排序
//                        int status = ContactsOnline.getContactOnlineStatus(address);
//                        if (status > 0) {
//                            amperList[0].add(0, map);
//                            orgList.add(0, map);  //tml*** search add
//                        } else {
//                            amperList[0].add(map);
//                            orgList.add(map);  //tml*** search add
//                        }
                    } while (cursor.moveToNext() && friends.size() <= MAX_USERS);
                    //tml|james*** unknown contacts error
                    int unknownBreak = ((numTrueFriends - 2) / 4);
                    if (unknownBreak < 1) unknownBreak = 1;
                    Log.d("check !@#$unknownsF " + unknowns + " >? " + unknownBreak + "/" + (numTrueFriends - 1));
                    if (unknowns > unknownBreak) {
                        Intent intent = new Intent(Global.Action_InternalCMD);
                        intent.putExtra("unknowns", true);
                        intent.putExtra("Command", Global.CMD_DOWNLOAD_FRIENDS);
                        sendBroadcast(intent);
                    }
                }
                //释放资源
                if (cursor != null && !cursor.isClosed())
                    cursor.close();

                //查询完成发送消息
                Message msg = Message.obtain();
                msg.what = QUERY_FINISH;
                msg.obj = queryFlag;
                handler.sendMessage(msg);
            }
        }).start();
    }

    /**
     * 显示loading对话框
     */
    private void showLoadingDialog(String description) {
        loadingDialog = new LoadingDialog(this, description);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.show();
    }

    /**
     * 初始化点击事件
     */
    private void initEvent() {
        refreshableView = (RefreshableView) findViewById(R.id.refreshable_view);
        refreshableView.initContainer(mIndexableStickyListView.getListView());
        refreshableView.setOnRefreshListener(new RefreshableView.PullToRefreshListener() {
            @Override
            public void onRefresh() {//子线程回调的方法
                if (refresh) {
                    refresh = false;
                    AireJupiter instance = AireJupiter.getInstance();
                    if (instance == null) {
                        Intent vip2 = new Intent(getApplicationContext(), AireJupiter.class);
                        getApplicationContext().startService(vip2);
                    }

                    //jack 16/5/3 暂时补救 relogin,有时间重写
                    try {
                        int versionCode = getApplicationContext().getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                        instance.tcpSocket.Login(versionCode);
                        instance.notifyReconnectTCP();
                        instance.onReconnect(instance.startConnection_beginning);

                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }

                    MyUtil.Sleep(2000);

                    //刷新主界面
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            refresh = true;
                            UsersActivity.needRefresh = true;
                            Intent intent = new Intent(Global.Action_Refresh_Gallery);
                            sendBroadcast(intent);
                            refreshableView.finishRefreshing();
                        }
                    });
                }
            }
        }, 1);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mIndexableStickyListView.searchTextChange(newText);
                return true;
            }
        });

        mIndexableStickyListView.setOnItemContentClickListener(new IndexableStickyListView.OnItemContentClickListener() {
            @Override
            public void onItemClick(View v, IndexEntity indexEntity) {
                Toast.makeText(UsersNewActivity.this, "选择了" + ((FriendsEntity)indexEntity).getDisplayName(), Toast.LENGTH_SHORT).show();
            }
        });
        //item的title click点击事件
        mIndexableStickyListView.setOnItemTitleClickListener(new IndexableStickyListView.OnItemTitleClickListener() {
            @Override
            public void onItemClick(View v, String title) {
                Toast.makeText(UsersNewActivity.this, "点击了" + title, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //////////////////////////////////////*initView*//////////////////////////////////////

    //初始化indexableListView
    private void initIndexView() {
        mIndexableStickyListView = (IndexableStickyListView) findViewById(R.id.indexListView);
//        mIndexableStickyListView.addHeaderView(headerView);
        Log.d("初始化IndexView完成");

    }

    //头布局
    private void initHeaderView() {
        headerView = getLayoutInflater().from(this).inflate(R.layout.item_contact_list_header, null);
        Log.d("初始化HeaderView完成");
    }

    //自定义搜索布局
    private void initSearchView() {
        mSearchView = (SearchView) findViewById(R.id.searchview);
        if (mSearchView == null) {
            return;
        }

        int queryTextViewId = mSearchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        TextView queryTextView = (TextView) mSearchView.findViewById(queryTextViewId);
        queryTextView.setTextSize(18);
        LayoutParams layoutParams = (LayoutParams) queryTextView.getLayoutParams();
        layoutParams.bottomMargin = -8;
        queryTextView.setLayoutParams(layoutParams);

        int searchImageId = mSearchView.getContext().getResources()
                .getIdentifier("android:id/search_button", null, null);
        ImageView searchImg = (ImageView) mSearchView.findViewById(searchImageId);
        layoutParams = (LayoutParams) searchImg.getLayoutParams();
        layoutParams.leftMargin = -8;
        searchImg.setLayoutParams(layoutParams);

        int closeImageId = mSearchView.getContext().getResources()
                .getIdentifier("android:id/search_close_btn", null, null);

        ImageView closeImg = (ImageView) mSearchView.findViewById(closeImageId);
        closeImg.setBackgroundColor(Color.TRANSPARENT);

        int searchPlateId = mSearchView.getContext().getResources()
                .getIdentifier("android:id/search_plate", null, null);
        View searchPlateView = mSearchView.findViewById(searchPlateId);
        if (searchPlateView != null) {
            searchPlateView.setBackgroundColor(Color.TRANSPARENT);
        }
        Log.d("初始化SearchView完成");
    }

    @Override
    public void onBackPressed() {
//    mSearchView.onActionViewCollapsed();
            super.onBackPressed();

    }

    /**
     * 回收资源
     */
    @Override
    protected void onDestroy() {
        if (mADB != null && mADB.isOpen())
            mADB.close();
        super.onDestroy();
    }
}
