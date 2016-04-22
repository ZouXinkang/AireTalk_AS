package com.pingshow.amper;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;


import com.pingshow.amper.bean.GroupMsg;
import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.GroupDB;
import com.pingshow.amper.db.SmsDB;
import com.pingshow.amper.map.bd.SendMyBDLocation;
import com.pingshow.amper.view.AudioMsgPlayer;
import com.pingshow.amper.view.ProgressBar;
import com.pingshow.codec.VoiceMemoPlayer_NB;
import com.pingshow.codec.VoicePlayer2_MP;
import com.pingshow.gif.GifView;
import com.pingshow.gif.GifView.GifImageType;
import com.pingshow.network.MyNet;
import com.pingshow.network.NetInfo;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyProfile;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.util.OpenDifferentFile;
import com.pingshow.util.ResizeImage;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;
import com.pingshow.voip.VideoCallActivity;


public class ConversationActivity extends Activity implements OnClickListener {

    static private ArrayList<SMS> TalkList = new ArrayList<SMS>();
    private ArrayList<GifView> GifList = new ArrayList<GifView>();
    private String mAddress;
    private String mNickname;
    private String SrcAudioPath;
    private String SrcImagePath;
    private String SrcVideoPath;
    private long mContactId = -1;
    private int mIdx;
    private int myIdx;
    private int mAttached = 0;
    private SendAgent agent;
    private SendFileAgent fileAgent;
    private static MsgListAdapter msgListAdapter;
    private ArrayList<String> sendeeList;//alec
    private ArrayList<String> addressList = new ArrayList<String>();
    private Handler mHandler = new Handler();
    private SmsDB mDB;
    private VoiceMemoPlayer_NB mp2;
    private String mMsgText;
    private MyPreference mPref;
    private EditText mInput;
    int changing = 0;
    int cursorPos = 0;
    private Vibrator mVibrator;
    private String beforeS = "";
    private String afterS = "";
    private boolean isSmile = false;
    public static String sender = null;
    private long enterTime = 0;
    private VoiceMemoPlayer_NB vmp = null;
    private VoicePlayer2_MP myVP1, myVP2 = null;
    private Button moresms, callbtn, profilebtn;
    private Button mSend;
    private ImageView mVoice;
    private ImageView speaker;
    private AnimationDrawable spAnimation;
    private boolean state = true;
    private String curFilePath = null;
    private boolean AnimationDrawablestate = true;
    private int listnumber = 30;
    private ListView listview;
    private Drawable myphoto;
    private Drawable friendPhoto;
    private Map<Integer, Drawable> friendsPhotoMap = new HashMap<Integer, Drawable>();
    public static boolean fileDownloading = false;
    public static boolean fileUploading = false;
    public static long smsId = 0;
    private long rowid;
    private Map<Long, SpannableString> spannableCache = new HashMap<Long, SpannableString>();
    private long rowID = 0;
    //	private RelativeLayout messageitem;
    private boolean showitem = false;
    private boolean isVideo = false;
    private Bitmap videobitmap = null;
    private int voicetime = 0;
    private Animation fadein, fadeout;
    private AmpUserDB mADB;
    private boolean largeScreen = false;
    private float size = 24.f;
    private float size2 = 67.f;
    private boolean inGroup = false;
    private boolean needScrollToEnd = true;
    private int mGroupID;
    boolean smileClicked = false;
    private boolean mCallMode = false;
    private boolean mFromGroup = false;
    private int mFromCallMode = 0;
    private TextView mSendee;
    private int widthScreenZ;
    private boolean isSTB = false;
    private boolean broadcastConf = false;
    long msg_smsid;
    long msg_org_smsid;

    static private ConversationActivity msgpage;
    private String imagePath;
    private String fileDownloadUrl;
    private ImageView mSetting;
    private GroupDB mGDB;


    static public ConversationActivity getInstance() {
        return msgpage;
    }

    private Map<String, ProgressBar> mProgress = new HashMap<String, ProgressBar>();

    public ProgressBar getProgressBar(String fn) {
        ProgressBar p = mProgress.get(fn);
        return p;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//		setContentView(R.layout.conversation);
        setContentView(R.layout.conversation_new);
        this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

        largeScreen = (findViewById(R.id.large) != null);
        if (largeScreen) {
            size = 36.f;
            size2 = 100.f;
        }
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        widthScreenZ = displaymetrics.widthPixels;

        Intent intent = getIntent();
        imagePath = intent.getStringExtra("photopath");//jack 2.4.51 图片
        mAddress = intent.getStringExtra("SendeeNumber");
        mNickname = intent.getStringExtra("SendeeDisplayname");
        mContactId = intent.getLongExtra("SendeeContactId", -1);
        mFromGroup = getIntent().getBooleanExtra("fromGroup", false);  //xwf*** beta ui3
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        android.util.Log.d("发送消息", mAddress + "--------" + mNickname);

        //tml*** detect stb
        if (mAddress.startsWith(Global.STB_HeaderName + Global.STB_Name1)
                || mAddress.startsWith(Global.STB_HeaderName + Global.STB_Name2)) {
            if (mAddress.length() == Global.STB_NameLength)
                isSTB = true;
        }

        inGroup = mAddress.startsWith("[<GROUP>]");
        //tml*** chatview
        mFromCallMode = intent.getIntExtra("FromCallMode", 0);
        mCallMode = intent.getBooleanExtra("CallMode", false);
        if (mCallMode) {
            int height = displaymetrics.heightPixels;
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.gravity = Gravity.CENTER;
//		    lp.width = (int) (width - 20 * wDensity);
            lp.height = (int) (height * 0.8);
            getWindow().setAttributes(lp);
        }
        //***tml
        mADB = new AmpUserDB(ConversationActivity.this);
        mADB.open();
        //jack
        mGDB = new GroupDB(ConversationActivity.this);
        mGDB.open();

        mPref = new MyPreference(this);
        mPref.write("GuardYou", false);
        MyProfile.init(ConversationActivity.this);

        mDB = new SmsDB(this);
        mDB.open();

        ArrangeTalkList();

        mIdx = mADB.getIdxByAddress(mAddress);

        myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);

        msgListAdapter = new MsgListAdapter(this);

        listview = (ListView) findViewById(R.id.talklist);

        boolean success = false;
        String conversationBg = mPref.read("BackgroundImage", null); // set talk list background
        if (conversationBg != null && new File(conversationBg).exists()) {
            Bitmap bgImage = null;
            boolean HDSize = false;

            try {
                BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                bitmapOptions.inPurgeable = true;
                bitmapOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(conversationBg, bitmapOptions);
                if (bitmapOptions.outHeight > 1000 || bitmapOptions.outWidth > 1900)
                    HDSize = true;
            } catch (Exception e) {
            } catch (OutOfMemoryError e) {
                System.gc();
                System.gc();
            }

            try {
                bgImage = ImageUtil.loadBitmapSafe(HDSize ? 2 : 1, conversationBg);
                if (bgImage != null) {
                    Drawable drawable = new BitmapDrawable(bgImage);
                    if (drawable != null) {
                        ((ImageView) findViewById(R.id.bkimg)).setImageDrawable(drawable);
                        success = true;
                    }
                }
            } catch (OutOfMemoryError e) {
            } finally {
                bgImage = null;
            }
        }

		/*
        if (!success)
		{
			SimpleDateFormat sdf=new SimpleDateFormat("HH");
			int hr=Integer.parseInt(sdf.format(new Date()));
			int res;
			if (hr>7 && hr<=16)
				res=R.drawable.bkimg_day;
			else if (hr>16 && hr<=17)
				res=R.drawable.bkimg_sunset;
			else if (hr>5 && hr<=7)
				res=R.drawable.bkimg_dawn;
			else
				res=R.drawable.bkimg_night;
			((ImageView)findViewById(R.id.bkimg)).setImageResource(res);
		}*/

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = null;
        if (!mADB.isFafauser(mIdx)) {
            v = inflater.inflate(R.layout.inflate_stranger, null, false);
//			((ImageView) findViewById(R.id.attachment)).setVisibility(View.INVISIBLE);
        } else if (inGroup) {
//			v = inflater.inflate(R.layout.inflate_group_member, null, false);
//			v.setVisibility(View.GONE);
            // TODO: 2016/4/8  ,当数据库中不存在此分组时,发送广播查询群成员
            mGroupID = Integer.parseInt(mAddress.substring(9));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int memberCount = mGDB.getGroupMemberCount(mGroupID);
                    ArrayList<String> membersList = mGDB.getGroupMembersByGroupIdx(mGroupID);
                    for (String idx : membersList) {
                        //如果idx为0就是第一次登陆,没有写入idx,应该查询php重新写入数据库
                        if ("0".equals(idx)) {
                            Intent it = new Intent(Global.Action_InternalCMD);
                            it.putExtra("Command", Global.CMD_JOIN_A_NEW_GROUP);
                            it.putExtra("GroupID", mGroupID);
                            android.util.Log.d("刷新group", "删除分组后,查询分组并写入数据库");
                            ConversationActivity.this.sendBroadcast(it);
                        }
                        break;
                    }
                }
            }).start();

        } else {
            v = inflater.inflate(R.layout.inflate_call_view, null, false);
        }
        if (v != null) {
            listview.addHeaderView(v);
            moresms = (Button) v.findViewById(R.id.moresms);
        }
        if (moresms != null) moresms.setOnClickListener(this);

//		callbtn = (Button) v.findViewById(R.id.call);
//		if (callbtn!=null) callbtn.setOnClickListener(this);
//		profilebtn = (Button) v.findViewById(R.id.view_profile);
//		if (profilebtn!=null) profilebtn.setOnClickListener(this);

        listview.setAdapter(msgListAdapter);
        if (TalkList.size() > 0) {
            listview.setSelection(TalkList.size() - 1);
        }

        mSendee = (TextView) findViewById(R.id.sendee);

        //jack
        mSetting = (ImageView) findViewById(R.id.right);
        if (inGroup) {
            String szGroup = getResources().getString(R.string.the_group);
            mSendee.setText(szGroup + ": " + mNickname);
            sendeeList = mGDB.getGroupMembersByGroupIdx(mGroupID);
            try {
                for (int i = 0; i < sendeeList.size(); i++)
                    addressList.add(mADB.getAddressByIdx(Integer.parseInt(sendeeList.get(i))));
            } catch (Exception e) {
            }
            //显示群组成员
			arrangePickedUsers();

            //jack 2.4.51
            mSetting.setVisibility(View.VISIBLE);
            //点击进入群组设置
            mSetting.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ConversationActivity.this, GroupSettingActivity.class);
                    intent.putExtra("groupname", mNickname);
                    intent.putExtra("GroupID", mGroupID + "");
                    // TODO: 2016/4/6 测试
                    intent.putExtra("rowid", rowid);
                    startActivity(intent);
                }
            });
        } else {
            //jack 2.4.51
            mSetting = (ImageView) findViewById(R.id.right);
            mSetting.setImageResource(R.drawable.icon_single_setting);
            mSetting.setVisibility(View.VISIBLE);

            mSetting.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ConversationActivity.this, SingleSettingActiivty.class);
                    intent.putExtra("mIdx", mIdx + "");
                    intent.putExtra("imagePath", imagePath);
                    intent.putExtra("mNickname", mNickname);
                    startActivity(intent);
                }
            });


            mSendee.setText(mNickname);
            if (mNickname == null) mSendee.setText(mAddress);
            //tml*** chatview
            String calleeTitle = "";
            if (mNickname == null) {
                calleeTitle = mAddress;
            } else {
                calleeTitle = mNickname;
            }
            if (mCallMode) {
                String inCall = getResources().getString(R.string.in_call);
                calleeTitle = inCall + " " + calleeTitle;
                mHandler.postDelayed(flash_sendee, 1000);
            }
            mSendee.setText(calleeTitle);
            //***tml
        }

        getphoto();

        listview.setOnItemLongClickListener(mLongPressTalkListItem);

        //jack 2.4.51
        listview.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //hide functions
                LinearLayout functions = (LinearLayout) findViewById(R.id.functions);
                if (functions.isShown()) {
                    functions.setVisibility(View.GONE);
                }
                //clean focus
                ((EditText) findViewById(R.id.msginput)).clearFocus();

                return false;
            }
        });

        //jack 2.4.51
        ((EditText) findViewById(R.id.msginput)).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    InputMethodManager imanager = (InputMethodManager) ConversationActivity.this
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imanager.hideSoftInputFromWindow(((EditText) findViewById(R.id.msginput)).getWindowToken(), 0);
                }

            }
        });


        ((ImageView) findViewById(R.id.voicesms)).setOnClickListener(this);
//		((ImageView)findViewById(R.id.picturesms)).setOnClickListener(this);
//		((ImageView)findViewById(R.id.videosms)).setOnClickListener(this);
//		((ImageView)findViewById(R.id.photosms)).setOnClickListener(this);
//		((ImageView)findViewById(R.id.filesms)).setOnClickListener(this);
//		((ImageView)findViewById(R.id.location)).setOnClickListener(this);
        //((ImageView)findViewById(R.id.guard)).setOnClickListener(this);
        ((Button) findViewById(R.id.sendmsg)).setOnClickListener(this);
        ((ImageView) findViewById(R.id.attachment)).setOnClickListener(this);
        ((LinearLayout) findViewById(R.id.functions)).setVisibility(View.VISIBLE);

        //tml|xwf*** beta ui2
        ((ImageView) findViewById(R.id.call)).setOnClickListener(this);
        ((ImageView) findViewById(R.id.videocall)).setOnClickListener(this);
        ((ImageView) findViewById(R.id.walkietalkie)).setOnClickListener(this);
        ((ImageView) findViewById(R.id.picmsg)).setOnClickListener(this);
        ((TextView) findViewById(R.id.picmsg_name)).setText(getResources().getString(R.string.fafauser_pic) +
                " / " + getResources().getString(R.string.file));
        ((ImageView) findViewById(R.id.location)).setOnClickListener(this);
        ((ImageView) findViewById(R.id.guard)).setOnClickListener(this);
        ((EditText) findViewById(R.id.msginput)).setFocusable(true);
        ((EditText) findViewById(R.id.msginput)).setFocusableInTouchMode(true);
        ((EditText) findViewById(R.id.msginput)).requestFocus();
        ((EditText) findViewById(R.id.msginput)).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (((LinearLayout) findViewById(R.id.functions)).getVisibility() == View.VISIBLE) {
                    showFunctions(true);
                }
                return false;
            }
        });


//		if (isSTB || inGroup) {  //tml*** detect STB
        if (true) {  //tml|alex*** rwt byebye X
            ((ImageView) findViewById(R.id.walkietalkie)).setImageResource(R.drawable.func_vm);
            ((TextView) findViewById(R.id.walkietalkie_desc)).setText(getString(R.string.fafauser_vmemo));
        }
        //tml*** broadcast
        if (mPref.readBoolean("BROADCAST", false))
            if (inGroup) {
                ((ImageView) findViewById(R.id.videocall)).setImageResource(R.drawable.func_call);
                ((TextView) findViewById(R.id.videocall_desc)).setText(getString(R.string.begin_bcast));
            }

        if (!AmazonKindle.canHandleCameraIntent(ConversationActivity.this)) {
            mPref.write("video_support", false);
        }

        if (AmazonKindle.IsKindle()) {
            ((ImageView) findViewById(R.id.location)).setBackgroundColor(Color.GRAY);
            ((ImageView) findViewById(R.id.location)).setEnabled(false);

            if (!AmazonKindle.hasMicrophone(ConversationActivity.this)) {
                ((ImageView) findViewById(R.id.call)).setBackgroundColor(Color.GRAY);
                ((ImageView) findViewById(R.id.call)).setEnabled(false);

                ((ImageView) findViewById(R.id.videocall)).setBackgroundColor(Color.GRAY);
                ((ImageView) findViewById(R.id.videocall)).setEnabled(false);

                ((ImageView) findViewById(R.id.walkietalkie)).setBackgroundColor(Color.GRAY);
                ((ImageView) findViewById(R.id.walkietalkie)).setEnabled(false);
            }
        }
        //***tml

//		if (!AmazonKindle.canHandleCameraIntent(this)){
//			((ImageView)findViewById(R.id.photosms)).setEnabled(false);
//		}

//		messageitem = (RelativeLayout)findViewById(R.id.messageitem);
        speaker = (ImageView) findViewById(R.id.speaker);
        spAnimation = (AnimationDrawable) speaker.getDrawable();

        fadein = AnimationUtils.loadAnimation(this, R.anim.push_up_in_fast);
        fadeout = AnimationUtils.loadAnimation(this, R.anim.fadeout);

        ((ImageView) findViewById(R.id.cancel)).setOnClickListener(this);

        ((ImageView) findViewById(R.id.smile)).setOnClickListener(this);

        mDensity = getResources().getDisplayMetrics().density;

        ImageView iv = (ImageView) findViewById(R.id.voice);
        iv.setOnClickListener(this);
        ImageView deleteiv = (ImageView) findViewById(R.id.deletefile);
        deleteiv.setOnClickListener(this);
        mSend = (Button) findViewById(R.id.sendmsg);
        mVoice = (ImageView) findViewById(R.id.voicesms);
        mInput = (EditText) findViewById(R.id.msginput);
        mInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() > 0) {
//					if (mSend.getVisibility()==View.INVISIBLE)
//					{
//						mSend.setVisibility(View.VISIBLE);
//						mVoice.setVisibility(View.INVISIBLE);
//					}
                    toggleSendVoiceBtn(0);
                } else {
//					mSend.setVisibility(View.INVISIBLE);
//					mVoice.setVisibility(View.VISIBLE);
                    if (mAttached == 0) {
                        toggleSendVoiceBtn(1);
                    }
                }
                if (isSmile) {
                    Smiley sm = new Smiley();

                    if (sm.hasSmileys(s.toString()) > 0) {
                        SpannableString spannable = new SpannableString(s
                                .toString());
                        for (int i = 0; i < Smiley.MAXSIZE; i++) {
                            for (int j = 0; j < sm.getCount(i); j++) {
                                ImageSpan icon = new ImageSpan(
                                        ConversationActivity.this,
                                        R.drawable.sm01 + i,
                                        ImageSpan.ALIGN_BOTTOM);
                                spannable.setSpan(
                                        icon,
                                        sm.getStart(i, j),
                                        sm.getEnd(i, j),
                                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }
                        mInput.setText(spannable);
                        mInput.setSelection(cursorPos);
                    }
                } else if (isVideo) {

                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                afterS = s.toString();
                Smiley sm = new Smiley();
                if (!afterS.equals(beforeS)
                        && sm.hasSmileys(afterS.substring(start, start + count)) > 0) {
                    isSmile = true;
                } else {
                    isSmile = false;
                }
                afterS = "";
                beforeS = "";
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                beforeS = s.toString();
                cursorPos = mInput.getSelectionStart();
                state = false;
            }
        });

        if (intent.getIntExtra("attachment", 0) == 1)
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    String audioPath = getIntent().getStringExtra("audioPath");
                    if (audioPath != null) {
                        if (mp2 != null && mp2.isPlaying())
                            return;
                        if (myVP1 != null && myVP1.isPlaying())  //tml*** new vmsg
                            return;
                        onPlayVoiceMemo(audioPath);
                        getIntent().putExtra("attachment", 0);
                    }
                }
            }, 300);

        IntentFilter intentToReceiveFilter = new IntentFilter();
        intentToReceiveFilter.addAction(Global.Action_SMS_Fail);
        intentToReceiveFilter.addAction(Global.Action_MsgGot);
        intentToReceiveFilter.addAction(Global.Action_MsgSent);
        intentToReceiveFilter.addAction(Global.Action_InternalCMD);
        intentToReceiveFilter.addAction(Global.ACTION_PLAY_OVER);
        intentToReceiveFilter.addAction(Global.Action_Hide_Group_Icon);
        this.registerReceiver(HandleListChanged, intentToReceiveFilter);

        msgpage = this;

        //tml*** chatview
        if (mCallMode) {
            if (callbtn != null) callbtn.setVisibility(View.INVISIBLE);
            toggleSendVoiceBtn(2);
        }
        //tml*** dev control
        if (((mAddress.equals("news_service") && mNickname.equals("Hot News")) || mIdx == 4)) {
            ((ImageView) findViewById(R.id.smile)).setVisibility(View.INVISIBLE);
            if (callbtn != null) callbtn.setVisibility(View.INVISIBLE);
            ((RelativeLayout) findViewById(R.id.inputFrameLayout)).setVisibility(View.INVISIBLE);
            ((LinearLayout) findViewById(R.id.functions)).setVisibility(View.INVISIBLE);
        } else if (((mAddress.equals("support") && mNickname.equals("Support")) || mIdx == 2)) {
        }
        //tml*** secret test
        if (mPref.readBoolean("TESTING", false) && Log.enDEBUG && !inGroup) {
            ((ImageView) findViewById(R.id.location)).setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (AireJupiter.getInstance() != null
                            && AireJupiter.getInstance().tcpSocket.isLogged(false)) {
                        AireJupiter.getInstance().tcpSocket
                                .send(mAddress, Global.Master_Parse + "test", 0, null, null, 0, null);
                    }

                    String atip = ContactsOnline.getContactSipIP(mAddress);
                    if (atip == null) atip = "---";
                    String content = mNickname + "\n" + mAddress + "\n" + mIdx + "  h." + Integer.toHexString(mIdx)
                            + "\n" + mContactId + "\n" + atip;
                    Intent it = new Intent(ConversationActivity.this, Tooltip.class);
                    it.putExtra("Content", content);
                    startActivity(it);
                    return true;
                }
            });
        }
        checkSecurityAccess();

    }

    private void arrangePickedUsers() {
        if (!inGroup || sendeeList == null || sendeeList.size() == 0) return;

//        RelativeLayout s = (RelativeLayout) findViewById(R.id.members);
//        s.removeAllViews();

        try {

            int count = sendeeList.size();
//            int width = (int) ((float) s.getWidth() / mDensity) - (largeScreen ? 60 : 45);
//            if (width < 0) {
//                int w = getWindowManager().getDefaultDisplay().getWidth();
//                width = (int) ((float) w / mDensity) - (largeScreen ? 45 : 30);
//            }
//            int space = width / count;
//            int w = 60;
//            int p = 5;
//            if (largeScreen) {
//                w = 90;
//                p = 8;
//            }

            for (int i = 0; i < count; i++) {
//                ImageView a = new ImageView(this);
//                TextView t = new TextView(this);
//                a.setBackgroundResource(R.drawable.empty);
//                a.setPadding((int) (mDensity * p), (int) (mDensity * p), (int) (mDensity * p), (int) (mDensity * p));
//                a.setClickable(true);
                int idx = Integer.parseInt(sendeeList.get(i));
                String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";

                Drawable photo = ImageUtil.getBitmapAsRoundCorner(userphotoPath, 1, 4);
                android.util.Log.d("ConversationActivity", "路径 "+userphotoPath+" photo:" + photo+" 结果");
//                if (photo != null)
//                    a.setImageDrawable(photo);
//                else
//                    a.setImageResource(R.drawable.bighead);

                friendsPhotoMap.put(Integer.valueOf(idx), photo);

//                RelativeLayout.LayoutParams lp = null;
//                lp = new RelativeLayout.LayoutParams((int) (mDensity * w), (int) (mDensity * w));
//                lp.leftMargin = (int) (mDensity * space) * (count - i - 1);
//                lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//                a.setId(i * 2 + 1);
//                s.addView(a, lp);
//
//                lp = new RelativeLayout.LayoutParams((int) (mDensity * (space - w)), (int) (mDensity * w));
//                lp.addRule(RelativeLayout.RIGHT_OF, a.getId());
//                lp.addRule(RelativeLayout.CENTER_VERTICAL);
//                t.setGravity(Gravity.CENTER_VERTICAL);
//                t.setText(mADB.getNicknameByIdx(idx));
//                t.setTextSize(9);
//                t.setTextColor(0xD0FFFFFF);
//                s.addView(t, lp);
//
//                if (i < count - 1) {
//                    AnimationSet as = new AnimationSet(false);
//                    as.setInterpolator(new AccelerateInterpolator());
//                    TranslateAnimation ta = new TranslateAnimation(mDensity * (-width + space), 0, 0, 0);
//                    ta.setDuration(500 + 100 * (count - i));
//                    as.addAnimation(ta);
//                    as.setDuration(500 + 100 * (count - i));
//                    a.startAnimation(as);
//                    t.startAnimation(as);
//                }
            }
        } catch (Exception e) {
        }
    }

    void getphoto() {
        try {
            String path = Global.SdcardPath_inbox + "photo_" + mIdx + ".jpg";

            friendPhoto = ImageUtil.getBitmapAsRoundCorner(path, 2, 4);

            if (friendPhoto == null)
                friendPhoto = getResources().getDrawable(R.drawable.bighead);

            path = mPref.read("myPhotoPath", null);
            if (path != null && path.length() > 0)
                myphoto = ImageUtil.getBitmapAsRoundCorner(path, 2, 10);

            if (myphoto == null)
                myphoto = getResources().getDrawable(R.drawable.bighead);
        } catch (Exception e) {
        }
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(flash_sendee);
        mHandler.removeCallbacks(unflash_sendee);

        try {
            if (vmp != null) {
                vmp.stop();
                vmp = null;
            }
            if (myVP2 != null) {  //tml*** new vmsg
                myVP2.stop();
                myVP2 = null;
            }
        } catch (Exception e) {
            vmp = null;
            myVP2 = null;
            Log.e("Converse onDestroy !@#$ " + e.getMessage());
        }
        if (msgListAdapter != null) {
            msgListAdapter.clear();
        }
        if (mDB != null && mDB.isOpen())
            mDB.close();
        if (mADB != null && mADB.isOpen())
            mADB.close();
        if (mGDB != null && mGDB.isOpen())
            mGDB.close();
        unregisterReceiver(HandleListChanged);

        spAnimation = null;

        String draft = mInput.getText().toString().trim();
        if (draft.length() != 0 && !draft.equals(R.string.textinput)) {
            mPref.write("draft" + mContactId, draft);
        } else if (draft.length() == 0) {
            mPref.delect("draft" + mContactId);
        }

        if (null != SrcAudioPath && mAttached == 1) {
            File file = new File(SrcAudioPath);
            if (file.exists())
                file.delete();
        }
        spannableCache = null;
        msgpage = null;
        super.onDestroy();
        System.gc();
        System.gc();
    }

    private PowerManager.WakeLock mWakeLock;

    void startWakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP, "PlayingVoiceMemo");
        }

        mWakeLock.acquire();
    }

    void stopWakeLock() {
        try {
            if (mWakeLock != null) mWakeLock.release();
        } catch (Exception e) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Integer.parseInt(Build.VERSION.SDK) >= 5
                && Integer.parseInt(Build.VERSION.SDK) <= 7)
            disableKeyguard();
        if (mPref.read("draft" + mContactId, null) != null && state) {
            String draft1 = mPref.read("draft" + mContactId);
            mInput.setText(draft1);
            mInput.setSelection(draft1.length());
        }
        sender = mAddress;

        if (needScrollToEnd)
            mHandler.post(RefreshStatus);

        int unread = mDB.getUnreadCountByAddress(mAddress);
        if (unread > 0) {

            MessageActivity.needToBeRefresh = true;

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDB.setMessageReadByAddress(ConversationActivity.this, mAddress);
                }
            }, 200);
        }
//		MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        try {
            if (Integer.parseInt(Build.VERSION.SDK) >= 5
                    && Integer.parseInt(Build.VERSION.SDK) <= 7)
                reenableKeyguard();
            stopPlayingVoice();
            if (mp2 != null) {
                mp2.stop();
                mp2 = null;
            }
            if (myVP1 != null) {  //tml*** new vmsg
                myVP1.stop();
                myVP1 = null;
            }
            sender = null;
//			MobclickAgent.onPause(this);
            stopWakeLock();
        } catch (Exception e) {
            Log.e("Converse pause !@#$ " + e.getMessage());
        }
//		picturebitmap.recycle();
//		picturebitmap = null;
        System.gc();
        super.onPause();
    }


    float mDensity = 1.f;

    static public Bitmap decodeResource(Resources resources, int id) {
        TypedValue value = new TypedValue();
        resources.openRawResource(id, value);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inTargetDensity = value.density = 160;
        return BitmapFactory.decodeResource(resources, id, opts);
    }

    private void ArrangeTalkList() {
        TalkList.clear();
        Cursor c = mDB.fetchMessages(mAddress, listnumber);
        if (c == null) return;

        do {
            SMS msg = new SMS(c);
            TalkList.add(msg);
        } while (c.moveToNext());

        if (c != null && !c.isClosed()) c.close();

        if (TalkList.size() < listnumber) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (moresms != null) moresms.setVisibility(View.GONE);
//					if (callbtn!=null) callbtn.setVisibility(View.VISIBLE);
                    //tml*** dev control
                    if (!((mAddress.equals("news_service") && mNickname.equals("Hot News")) || mIdx == 4)) {
                        if (callbtn != null) callbtn.setVisibility(View.VISIBLE);
                    } else {
                        if (callbtn != null) callbtn.setVisibility(View.INVISIBLE);
                    }
                    if (profilebtn != null) profilebtn.setVisibility(View.VISIBLE);
                }
            });
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (moresms != null) moresms.setVisibility(View.VISIBLE);
                    if (callbtn != null) callbtn.setVisibility(View.GONE);
                    if (profilebtn != null) profilebtn.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private AdapterView.OnItemLongClickListener mLongPressTalkListItem = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> av, View v, int position,
                                       long id) {
            return handleLongPress(position - 1);
        }
    };

    private OnLongClickListener mLongPressBalloonView = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            int position = Integer.parseInt(v.getTag().toString());
            return handleLongPress(position);
        }
    };

    final int action_nothing = 0;
    final int action_resend = 1;
    final int action_save_as = 2;
    int item2_action;

    boolean handleLongPress(int position) {
        if (position == -1) {
            return false;
        }
        if (mp2 != null && mp2.isPlaying())
            return false;
        if (myVP1 != null && myVP1.isPlaying())  //tml*** new vmsg
            return false;
        if (TalkList != null) {
            final SMS msg = TalkList.get(position);
            msg_smsid = msg.smsid;
            msg_org_smsid = msg.org_smsid;
            CharSequence[] lpressOptions = null;

            item2_action = action_nothing;

            Log.e("test handleLongPress " + msg.attached + " " + msg.status);
            if (msg.att_path_img == null && msg.attached != 0) {
                if (msg.status != -2 && msg.status != 1 && msg.type == 2) {
                    lpressOptions = new CharSequence[3];
                    lpressOptions[2] = getResources().getString(R.string.send_again);
                    lpressOptions[1] = getResources().getString(R.string.copysms);  //share
                    item2_action = action_resend;
                } else {
                    lpressOptions = new CharSequence[2];
                    lpressOptions[1] = getResources().getString(R.string.copysms);  //share
                }
            } else {
                if (msg.status != -2 && msg.status != 1 && msg.type == 2) {
                    lpressOptions = new CharSequence[3];
                    lpressOptions[2] = getResources().getString(R.string.send_again);
                    lpressOptions[1] = getResources().getString(R.string.copysms);  //copy
                    item2_action = action_resend;
                } else if (msg.att_path_img != null) {
                    lpressOptions = new CharSequence[3];
                    lpressOptions[2] = getResources().getString(R.string.save_photo_SD);
                    lpressOptions[1] = getResources().getString(R.string.copysms);  //share
                    item2_action = action_save_as;
                } else {
                    lpressOptions = new CharSequence[2];
                    lpressOptions[1] = getResources().getString(R.string.copysms);  //copy
                }
            }
            lpressOptions[0] = getResources().getString(R.string.delete_msg);

            new AlertDialog.Builder(ConversationActivity.this)
                    .setTitle(mNickname)
                    .setItems(lpressOptions, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0)  //delete
                            {
                                new AlertDialog.Builder(ConversationActivity.this)
                                        .setTitle(R.string.delete_confirm)
                                        .setMessage(R.string.delete_thread_confirm)
                                        .setPositiveButton(R.string.yes,
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        if (ConversationActivity.fileUploading) {
                                                            for (int i = 0; i < TalkList.size(); i++) {
                                                                final SMS msg = TalkList.get(i);
                                                                if (msg_smsid == msg.smsid) {
                                                                    new MyNet(ConversationActivity.this).stopUploading(msg.att_path_aud);
                                                                    ConversationActivity.fileUploading = false;
                                                                    break;
                                                                }
                                                            }
                                                        }

                                                        mDB.deleteSingleMsg(msg_smsid, msg_org_smsid);
                                                        spannableCache.remove(msg.smsid);
                                                        ArrangeTalkList();
                                                        msgListAdapter.notifyDataSetChanged();
                                                        Intent it = new Intent(Global.Action_HistroyThread);
                                                        sendBroadcast(it);
                                                    }
                                                })
                                        .setNegativeButton(R.string.no,
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                    }
                                                }).show();
                            } else if (which == 1)  //copy/share
                            {
                                if (msg.content.startsWith("(g.f")) {
                                    copyToClipboard("", null);
                                    return;
                                }
                                //tml*** copypaste
                                String path = msg.att_path_aud;
                                if (path == null)
                                    path = msg.att_path_img;
                                copyToClipboard(msg.content, path);
                            } else if (which == 2)  //save/send
                            {
                                if (item2_action == action_save_as) {
                                    if (!MyUtil.checkSDCard(ConversationActivity.this))
                                        return;
                                    String aSrcImagePath = msg.att_path_img;
                                    File fromFile = new File(aSrcImagePath);
                                    String[] items = aSrcImagePath.split("/");
                                    File toFile = new File(Global.SdcardPath_downloads + items[items.length - 1]);
                                    MyUtil.copyFile(fromFile, toFile, true, ConversationActivity.this);
                                    //tml*** beta ui, save dialog
                                    Toast.makeText(getApplicationContext(), Global.SdcardPath_downloads,
                                            Toast.LENGTH_SHORT).show();
                                } else if (item2_action == action_resend) {
                                    if (ConversationActivity.fileUploading) {
                                        Toast.makeText(getApplicationContext(), getString(R.string.fileuploading),
                                                Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    SendAgent failagent = new SendAgent(ConversationActivity.this,
                                            myIdx, mIdx, false);
                                    failagent.setRowId(msg.smsid);
                                    if (msg.attached == 9) {
                                        msg.content = msg.content.substring(msg.content.indexOf("(vdo)")) + "1";
                                        msg.attached = 8;
                                    } else if (msg.attached == 8) {
                                        msg.content = msg.content.substring(msg.content.indexOf("(fl)"));
                                    }
                                    failagent.onSend(mAddress, msg.content,
                                            msg.attached, msg.att_path_aud, msg.att_path_img, false);
                                    mDB.updateFailCountById((int) msg.smsid, 0);
                                }
                            }
                        }
                    }).show();
        }
        return true;
    }

    private void copyToClipboard(String text, String path) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        Log.e("test copyToClipboard: " + text + ", " + path);
        if (text.contains("(Vm)"))
            text = text.replace("(Vm)", "");
        if (text.contains("(iMG)"))
            text = text.replace("(iMG)", "");
        if (text.contains("(vdo)"))
            text = text.replace("(vdo)", "");
        if (text.contains("(fl)"))
            text = text.replace("(fl)", "");
        clipboardManager.setText(text);
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    OnClickListener mOnClickPhoto = new OnClickListener() {
        public void onClick(View v) {
            if (mADB.isFafauser(mIdx)) {
                Intent it = new Intent(ConversationActivity.this, FunctionActivity.class);
                it.putExtra("Contact_id", mContactId);
                it.putExtra("Address", mAddress);
                it.putExtra("Nickname", mNickname);
                if (mContactId > 0)
                    it.putExtra("AireNickname", mADB.getNicknameByAddress(mAddress));
                it.putExtra("Idx", mIdx);
                it.putExtra("fromConversation", true);
                it.putExtra("CallMode", mCallMode);
                startActivity(it);
            }
        }
    };

    //tml|li*** uri null yota fix
    public String getPath(Uri uri, int requestCode) {
        try {
            Log.e("getPath2 uriPath=" + uri.getPath() + " string=" + uri.toString());
            if (uri.toString().startsWith("content:")) {
                String result = null;
                Uri contentUri;
                Cursor cursor;
                String[] column = {MediaStore.Images.Media.DATA};
                if (requestCode == 101) {
                    String uriStr = uri.toString();
                    String id = null;
                    String sel = MediaStore.Images.Media._ID + "=?";
                    if (uriStr.contains("%3A")) {
                        id = uriStr.split("%3A")[1];
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        Log.e("getPath2 cursor1: contentUri=" + contentUri.toString() + " sel=" + sel + " id=" + id);
                        cursor = getContentResolver().query(contentUri, column, sel, new String[]{id}, null);
                    } else {
                        contentUri = uri;
                        Log.e("getPath2 cursor2: contentUri=" + contentUri.toString());
                        cursor = getContentResolver().query(contentUri, column, null, null, null);
                    }
                } else {
                    contentUri = uri;
                    Log.e("getPath2 cursor3: contentUri=" + contentUri.toString() + " column=" + column[0]);
                    cursor = getContentResolver().query(contentUri, column, null, null, null);
                }

                if (cursor == null) {
                    result = uri.getPath();
                } else {
                    cursor.moveToFirst();
                    int idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    if (idx >= 0) {
                        result = cursor.getString(idx);
                        cursor.close();
                    }
                }
                Log.e("getPath2 result=" + result);
                return result;
            } else if (uri.toString().startsWith("file:")) {
                String uriStr = uri.toString();
                if (uriStr.contains("sdcard")) {
                    return uriStr.substring(uriStr.indexOf("sdcard"));
                } else if (uriStr.contains("storage")) {
                    return uriStr.substring(uriStr.indexOf("storage"));
                }
            }
        } catch (Exception e) {
            Log.e("getPath2 !@#$ " + e.getMessage());
        }
        return null;
    }

    public String getPath(Uri uri) {
        if (uri.toString().startsWith("content:")) {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = managedQuery(uri, projection, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            //cursor.close();
            return path;
        } else if (uri.toString().startsWith("file:")) {
            String uriStr = uri.toString();
            return uriStr.substring(uriStr.indexOf("sdcard"));
        }
        return "";
    }

    public static boolean videoRecording = false;

    public void onFileTransfer() {
        if (mAttached != 8 && mAttached != 0) {
            Toast.makeText(this, getString(R.string.fileandvideosingle), Toast.LENGTH_SHORT).show();
            return;
        }
        isVideo = false;
        if (MyUtil.checkSDCard(this)) {
            startActivityForResult(new Intent(this, FileBrowerActivity.class),
                    20);
        } else {
            Toast.makeText(this, getString(R.string.no_sdcard),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void onPickVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(Intent.createChooser(intent,
                getString(R.string.chose_file)), 101);
    }

    private Uri outputFileUri;

    public void onVoiceSMS() {
        if (mAttached == 8) {
            Toast.makeText(ConversationActivity.this,
                    getString(R.string.fileandvideosingle),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        SrcAudioPath = Global.SdcardPath_sent + getRandomName() + ".amr";
//		SrcAudioPath = Global.SdcardPath_sent + getRandomName() + ".mp3";  //tml*** new vmsg
        Intent it = new Intent(ConversationActivity.this,
                VoiceRecordingDialog.class);
        it.putExtra("path", SrcAudioPath);
        startActivityForResult(it, 15);
    }

    private void onTakePicture() {
        if (mAttached == 8) {
            Toast.makeText(this, getString(R.string.fileandvideosingle),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (!AmazonKindle.canHandleCameraIntent(this)) {
            Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File file = new File(Global.SdcardPath_sent + "tmp.jpg");
            outputFileUri = Uri.fromFile(file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            startActivityForResult(intent, 3);
            mPref.write("vociemessaging", true);// take photo not popupDialog
        } catch (Exception e) {
            Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
        }
    }

    public void onPickPicture() {
        if (mAttached == 8) {
            Toast.makeText(this, getString(R.string.fileandvideosingle),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        String title = getResources().getString(R.string.choose_photo_source);
        startActivityForResult(Intent.createChooser(intent, title), 1);
    }

    public void onPickPictureOption() {
        final CharSequence[] items = {  //tml|xwf*** beta ui2
                getResources().getString(R.string.photo_gallery),
                getResources().getString(R.string.takepicture),
                getResources().getString(R.string.videomemo),
                getResources().getString(R.string.filememo)};
        final CharSequence[] items_noCamera = {  //tml|xwf*** beta ui2
                getResources().getString(R.string.photo_gallery),
                getResources().getString(R.string.filememo)};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (!AmazonKindle.canHandleCameraIntent(this)) {
            builder.setItems(items_noCamera, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    if (item == 0)
                        onPickPicture();
                    else if (item == 1)  //tml|xwf*** beta ui2
                        onFileTransfer();
                    dialog.dismiss();
                }
            });
        } else {
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    if (item == 0)
                        onPickPicture();
                    else if (item == 1)
                        onTakePicture();
                    else if (item == 2)
                        onPickVideo();
                    else if (item == 3)  //tml|xwf*** beta ui2
                        onFileTransfer();
                    dialog.dismiss();
                }
            });
        }

        builder.setTitle(ConversationActivity.this.getResources().getString(R.string.chose_file));
        builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public static String getRandomName() {
        return ("" + new Date().getTime());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 3) // take photo not popupDialog
            mPref.write("vociemessaging", false);

        if (requestCode == 230) {
            if (resultCode == RESULT_OK)
                MakeCall.Call(ConversationActivity.this, mAddress, false);
        } else if (requestCode == 131) {  //tml|xwf*** beta ui2
            if (resultCode == RESULT_OK) {
                if (AireJupiter.getInstance() != null && AireJupiter.getInstance().tcpSocket.isLogged(false)) {
                    AireJupiter.getInstance().tcpSocket.send(mAddress, Global.SUV_ON, 0, null, null, 0, null);
                }
            } else if (resultCode == CommonDialog.STOP_SUV) {
                if (AireJupiter.getInstance() != null && AireJupiter.getInstance().tcpSocket.isLogged(false)) {
                    AireJupiter.getInstance().tcpSocket.send(mAddress, Global.SUV_OFF, 0, null, null, 0, null);
                }
            }
        } else if (requestCode == 132) {  //tml*** iot control
            if (resultCode == RESULT_OK) {
                if (AireJupiter.getInstance() != null && AireJupiter.getInstance().tcpSocket.isLogged(false)) {
                    AireJupiter.getInstance().tcpSocket.send(mAddress, Global.SUV_ON_IOTALL, 0, null, null, 0, null);
                }
            } else if (resultCode == CommonDialog.STOP_SUV) {
                if (AireJupiter.getInstance() != null && AireJupiter.getInstance().tcpSocket.isLogged(false)) {
                    AireJupiter.getInstance().tcpSocket.send(mAddress, Global.SUV_OFF_IOTALL, 0, null, null, 0, null);
                }
            }
        } else if (requestCode == 7) {
            if (resultCode == RESULT_OK) {
                mAttached = 0;
                long lon = mPref.readLong("longitude", 116349386);
                long lat = mPref.readLong("latitude", 39976279);
                mMsgText = "here I am (" + ((float) lat / 1000000.f) + "," + ((float) lon / 1000000.f) + ")";
                agent = new SendAgent(ConversationActivity.this, myIdx, mIdx, true);

                if (inGroup) {
                    agent.setAsGroup(mGroupID);

                    android.util.Log.d("发送消息", "mAttached == 0 定位" + " addressList " + addressList + " mMsgText " + mMsgText + " mAttached " + mAttached + " SrcAudioPath " + SrcAudioPath + " SrcImagePath " + SrcImagePath);
                    GroupMsg groupMsg = new GroupMsg("", mAttached + "", "", mMsgText, "");
                    if (agent.onGroupSend(groupMsg))
                        addMsgtoTalklist(false);
                } else {
                    if (agent.onSend(mAddress, mMsgText, 0, null, null, false))
                        addMsgtoTalklist(false);
                }
            }
        } else if (resultCode == RESULT_OK) {
            if (requestCode == 1 || requestCode == 3) {
                if (requestCode == 1) {
                    if (data == null) return;
                    if (null == data.getData()) return;
                    Uri selectedImageUri = data.getData();
                    SrcImagePath = getPath(selectedImageUri);
                } else if (requestCode == 3)
                    SrcImagePath = Global.SdcardPath_sent + "tmp.jpg";
                Log.d("onActivityResult:SrcImagePath===" + SrcImagePath);
                mAttached |= 2;// image
                String filename = Global.SdcardPath_sent + getRandomName() + ".jpg";

                if (SrcImagePath == null) {
                    int result = ResizeImage.saveFromStream(this, data, filename, 1280, 1280, 95);
                    if (result == -1) {
                        Toast.makeText(this, R.string.img_error, Toast.LENGTH_SHORT).show();
                        mAttached = 0;
                        return;
                    }
                } else {
                    int result = ResizeImage.Resize(this, SrcImagePath, filename, 1280, 1280, 95);
                    if (result == -1) {
                        Toast.makeText(this, R.string.img_error, Toast.LENGTH_SHORT).show();
                        mAttached = 0;
                        return;
                    }
                }
                SrcImagePath = filename;

                ShowAttchment(0);
            } else if (requestCode == 200) // back from smile activity
            {
                int index = data.getIntExtra("index", 0);
                if (index >= 75) { //gif
                    agent = new SendAgent(ConversationActivity.this, myIdx, mIdx, true);
                    mMsgText = (String) SmileyActivity.smiles[index][0];

                    if (inGroup) {
                        agent.setAsGroup(mGroupID);
                        android.util.Log.d("发送消息", "mAttached == 8 表情" + " addressList " + addressList + " mMsgText " + mMsgText + " mAttached " + mAttached + " SrcAudioPath " + SrcAudioPath + " SrcImagePath " + SrcImagePath);
                        GroupMsg groupMsg = new GroupMsg("", mAttached + "", "", mMsgText, "");
                        if (agent.onGroupSend(groupMsg))
                            addMsgtoTalklist(false);
                    } else {
                        if (agent.onSend(mAddress, mMsgText, 0, null, null, false))
                            addMsgtoTalklist(false);
                    }
                } else {
                    EditText msginput = (EditText) findViewById(R.id.msginput);
                    int indexCursor = msginput.getSelectionStart();
                    msginput.getText().insert(indexCursor,
                            String.valueOf(SmileyActivity.smiles[index][0]));
                }
                smileClicked = false;
            } else if (requestCode == 15) {
                mAttached |= 1;
                SrcAudioPath = data.getStringExtra("path");

                voicetime = 60 - data.getIntExtra("voicetime", 60);

                agent = new SendAgent(ConversationActivity.this, myIdx, mIdx, true);

                SrcImagePath = null;
                mMsgText = "(Vm)" + voicetime;

                if (inGroup) {
                    agent.setAsGroup(mGroupID);

                    android.util.Log.d("发送消息", "mAttached == 15 语音" + " addressList " + addressList + " mMsgText " + mMsgText + " mAttached " + mAttached + " SrcAudioPath " + SrcAudioPath + " SrcImagePath " + SrcImagePath);
                    GroupMsg groupMsg = new GroupMsg("", mAttached + "", SrcAudioPath, mMsgText, "");
                    if (agent.onGroupSend(groupMsg)) {
                        addMsgtoTalklist(false);
                        playSoundTouch();
                    }
                } else {
                    if (agent.onSend(mAddress, mMsgText, mAttached, SrcAudioPath, SrcImagePath, false)) {
                        addMsgtoTalklist(false);
                        playSoundTouch();
                    }
                }
                SrcAudioPath = null;

            } else if (requestCode == 40) {

                fileAgent = new SendFileAgent(this, myIdx, true);

                if (inGroup) {
                    fileAgent.setAsGroup(mGroupID);
                    android.util.Log.d("发送消息", "requestCode ==40  未知" + " addressList " + addressList + " mMsgText " + mMsgText + " mAttached " + mAttached + " SrcAudioPath " + SrcAudioPath + " SrcImagePath " + SrcImagePath);

                    if (!fileAgent.onMultipleSend(addressList, mMsgText, mAttached, SrcAudioPath, SrcImagePath))
                        mSend.setEnabled(true);
                    else {
                        addMsgtoTalklist(true);
                        playSoundTouch();
                    }
                } else {
                    if (!fileAgent.onSend(mAddress, mMsgText, mAttached, SrcAudioPath, SrcImagePath, false))
                        mSend.setEnabled(true);
                    else {
                        addMsgtoTalklist(true);
                        playSoundTouch();
                    }
                }
                SrcAudioPath = null;
//				mVoice.setVisibility(View.VISIBLE);
//				mSend.setVisibility(View.INVISIBLE);
                toggleSendVoiceBtn(1);
                if (mCallMode) {  //chatview

                } else {

                }
            }
            if (requestCode == 101) {
                mAttached = 8;
                Uri selectedImageUri = data.getData();
                SrcVideoPath = getPath(selectedImageUri);
                //tml|li*** uri null yota fix
                if (SrcVideoPath == null || !(SrcVideoPath.length() > 0)) {
                    SrcVideoPath = getPath(selectedImageUri, requestCode);
                }
                if (SrcVideoPath != null) {
                    SrcAudioPath = SrcVideoPath;
                    ShowAttchment(0);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.file_transfer_error), Toast.LENGTH_LONG).show();
                }
            } else if (requestCode == 20) { // show file attach icon
                SrcVideoPath = data.getStringExtra("filePath");
                mAttached = 8;
                SrcAudioPath = SrcVideoPath;
                ShowAttchment(1);
            } else if (requestCode == 30) { // show video attach icon
                videoRecording = false;
                mAttached = 8;
                Uri selectedImageUri = data.getData();
//				SrcVideoPath = getPath(selectedImageUri).toString();
                SrcVideoPath = getPath(selectedImageUri);  //tml|li*** uri null yota fix
                SrcAudioPath = SrcVideoPath;
                ShowAttchment(0);
            } else if (requestCode == 50) { // download file
                if (!MyUtil.checkSDCard(this)) {
                    return;
                }
                final String filename = curFilePath.substring(
                        curFilePath.lastIndexOf("/") + 1,
                        curFilePath.lastIndexOf("_"));
                if (AireJupiter.getInstance() != null) {
                    AireJupiter.getInstance().showNotification(filename, null,
                            false, android.R.drawable.stat_sys_download,
                            getString(R.string.downloading));
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String prex = curFilePath.substring(0,
                                curFilePath.lastIndexOf("/") + 1);
                        String suffix = curFilePath.substring(curFilePath
                                .lastIndexOf("_"));
                        String tmpCurFilePath = "";
                        try {
                            tmpCurFilePath = prex
                                    + URLEncoder.encode(filename, "UTF-8")
                                    + suffix;

                        } catch (UnsupportedEncodingException e) {
                        }
                        MyNet myNet = new MyNet(ConversationActivity.this);
                        fileDownloading = true;

                        if (fileDownloadUrl != null) {
                            myNet.DownloadGroupFile(
                                    fileDownloadUrl,
                                    Global.SdcardPath_downloads
                                            + filename.replace(" ", ""), type == 1 ? 9 : 10);

                        } else {
                            myNet.Download(
                                    tmpCurFilePath,
                                    Global.SdcardPath_downloads
                                            + filename.replace(" ", ""), type == 1 ? 9 : 10, obligate1_phpIP);
                        }
                        //downloaded, 9 is video, 10 is file
                        obligate1_phpIP = null;
                    }
                }).start();
            }
        } else if (resultCode == RESULT_CANCELED && requestCode == 40) {
            mSend.setEnabled(true);
            if (null != SrcVideoPath) {
                (new File(SrcVideoPath)).delete();
            }
            SrcVideoPath = null;
            mAttached = 0;
            ShowAttchment(0);
//			if (mVoice.getVisibility()==View.INVISIBLE)
//			{
//				mVoice.setVisibility(View.VISIBLE);
//				mSend.setVisibility(View.INVISIBLE);
//			}
            toggleSendVoiceBtn(1);
        } else if (resultCode == RESULT_CANCELED && requestCode == 200) {
            smileClicked = false;
        } else if (resultCode != RESULT_OK && requestCode == 30) {// record
            if (null != SrcAudioPath) {
                try {
                    (new File(SrcAudioPath)).delete();
                } catch (Exception e) {
                }
            }
            SrcAudioPath = null;
            mAttached = 0;
            videoRecording = false;
            ShowAttchment(0);
        } else if (resultCode == RESULT_CANCELED && requestCode == 15) {// voice memo cancelled
            if (null != SrcAudioPath) {
                try {
                    (new File(SrcAudioPath)).delete();
                } catch (Exception e) {
                }
            }
            SrcAudioPath = null;
            mAttached = 0;
        }
    }

    @SuppressLint("NewApi")
    private void showVideoBitmap(ImageView imageView) {
        if (SrcVideoPath == null)
            return;

        if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
            videobitmap = ThumbnailUtils.createVideoThumbnail(new File(
                    SrcVideoPath).getAbsolutePath(), Video.Thumbnails.MICRO_KIND);
        }
        if (videobitmap == null)
            videobitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sm70);
        if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
            Bitmap bubbleblue = null;
            bubbleblue = BitmapFactory.decodeResource(ConversationActivity.this.getResources(),
                    R.drawable.novideo);
            Drawable[] array = new Drawable[2];
            array[1] = new BitmapDrawable(bubbleblue);
            array[0] = new BitmapDrawable(videobitmap);
            LayerDrawable layers = new LayerDrawable(array);
            layers.setLayerInset(0, 0, 0, 0, 0);
            layers.setLayerInset(1, 0, 0, 0, 0);
            layers.setBounds(0, 0, (int) (66.f * mDensity), (int) (66.f * mDensity));

            imageView.setImageDrawable(layers);
            bubbleblue = null;
        } else
            imageView.setImageResource(R.drawable.start_play);
        videobitmap = null;

    }

    private void ShowAttchment(int fileType) {
        float piclen = 0;
        float voicelen = 0;
        float filesize = 0;
        FrameLayout f = (FrameLayout) findViewById(R.id.attachedframe);
        ImageView ivp = (ImageView) findViewById(R.id.picture);
        ImageView ivv = (ImageView) findViewById(R.id.voice);
        ImageView ivd = (ImageView) findViewById(R.id.video);
        ImageView ivf = (ImageView) findViewById(R.id.file);
        ImageView ivdelete = (ImageView) findViewById(R.id.deletefile);
        TextView smsinfo = (TextView) findViewById(R.id.smsinfo);
        TextView voicesmsinfo = (TextView) findViewById(R.id.voice_smsinfo);
        smsinfo.setVisibility(View.GONE);
        ivdelete.setVisibility(View.GONE);
        listview.setSelection(TalkList.size() - 1);
        if (mAttached != 0) {
            if ((mAttached & 2) == 2) {
                File file = new File(SrcImagePath);
                piclen = ((float) file.length() / 1024.f);
                Drawable img = null;
                try {
                    img = Drawable.createFromPath(SrcImagePath);
                } catch (OutOfMemoryError e) {
                    System.gc();
                    System.gc();
                }
                if (img != null) {
                    ivp.setImageDrawable(img);
                    ivp.setVisibility(View.VISIBLE);
                }
            } else
                ivp.setVisibility(View.GONE);
            if ((mAttached & 1) == 1) {
                File file = new File(SrcAudioPath);
                voicelen = (float) file.length() / 1024.f;
                ivv.setVisibility(View.VISIBLE);
                voicesmsinfo.setVisibility(View.VISIBLE);
                voicesmsinfo.setText(voicetime + " s");
            } else {
                voicesmsinfo.setVisibility(View.GONE);
                ivv.setVisibility(View.GONE);
            }
            if (mAttached == 8) {
                File file = new File(SrcAudioPath);
                filesize = (float) file.length() / 1024.f;
                if (fileType == 0) {// video
                    ivd.setVisibility(View.VISIBLE);
                    ivf.setVisibility(View.GONE);
                } else {
                    ivd.setVisibility(View.GONE);
                    ivf.setVisibility(View.VISIBLE);
                }

                showVideoBitmap(ivd);
            } else {
                ivd.setVisibility(View.GONE);
                ivf.setVisibility(View.GONE);
            }
            if (fileType == -1) {
                f.setVisibility(View.GONE);
            } else {
                f.setVisibility(View.VISIBLE);
            }
            float totalsize = filesize + voicelen + piclen;
            totalsize = totalsize - ((totalsize * 100 - (int) (totalsize * 100)) / 100.f);
            smsinfo.setVisibility(View.VISIBLE);
            smsinfo.setText(totalsize + " KB");
            ivdelete.setVisibility(View.VISIBLE);
        } else
            f.setVisibility(View.GONE);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                listview.setSelection(TalkList.size() - 1);
            }
        });

//		mSend.setVisibility(View.VISIBLE);
//		mVoice.setVisibility(View.INVISIBLE);
        toggleSendVoiceBtn(0);
    }

    final Runnable RefreshStatus = new Runnable() {
        public void run() {
            if (msgListAdapter != null)
                msgListAdapter.notifyDataSetChanged();
            listview.setSelection(TalkList.size() - 1);
            needScrollToEnd = false;
        }
    };

    private void addMsgtoTalklist(boolean isFile) {
        SMS msg = new SMS();
        msg.displayname = mNickname;
        msg.address = mAddress;
        msg.content = mMsgText;
        msg.contactid = mContactId;
        msg.read = 1;
        msg.type = 2;
        msg.status = SMS.STATUS_PENING;
        msg.time = new Date().getTime();
        msg.attached = mAttached;
        if ((mAttached & 1) == 1)
            msg.att_path_aud = SrcAudioPath;
        if ((mAttached & 2) == 2)
            msg.att_path_img = SrcImagePath;
        if (mAttached == 8) {
            msg.att_path_aud = SrcAudioPath;
            if (msg.content.startsWith("(fl)")) {
                msg.content = getString(R.string.filememo_send) + " " + msg.content;
            } else {
                msg.content = getString(R.string.video) + " " + msg.content;
                msg.attached = 9;
            }
        }

        msg.longitudeE6 = mPref.readLong("longitude", 116349386);
        msg.latitudeE6 = mPref.readLong("latitude", 39976279);

        rowid = mDB.insertMessage(mAddress, msg.contactid,
                (new Date()).getTime(), 1, msg.status, msg.type, "",
                msg.content, msg.attached, msg.att_path_aud, msg.att_path_img,
                0, msg.longitudeE6, msg.latitudeE6, 0, mNickname, null, 0);
        msg.smsid = rowid;//huan

        if (isFile) {
            fileAgent.setRowId(rowid);
        } else {
            agent.setRowId(rowid);
        }

        mAttached = 0;
        TalkList.add(0, msg);
        mInput.setText("");
        needScrollToEnd = true;
        mHandler.post(RefreshStatus);

        if (isFile)
            ShowAttchment(-1);
        else {
            ShowAttchment(0);
        }

        mHandler.postDelayed(new Runnable() {
            public void run() {
                mSend.setEnabled(true);
                String input = mInput.getText().toString();
                if (input.length() == 0) {
//					mVoice.setVisibility(View.VISIBLE);
//					mSend.setVisibility(View.INVISIBLE);
                    toggleSendVoiceBtn(1);
                }
            }
        }, 1000);

        Intent it = new Intent(Global.Action_HistroyThread);
        sendBroadcast(it);
    }

    // type = 1 is video ,type = 2 is file
    private int type = 1;
    private String obligate1_phpIP = null;

    private void dialogFileDownload(String filepath, String len) {
        this.curFilePath = filepath;
        NetInfo myNet = new NetInfo(this);
        try {
            String msgContent = "";
            String filename = curFilePath.substring(curFilePath.lastIndexOf("/") + 1, curFilePath.lastIndexOf("_"));
            Intent it = new Intent(this, CommonDialog.class);
            if (myNet.netType >= NetInfo.MOBILE_3G) {
                msgContent = getString(R.string.file_dnwifinet, len,
                        Global.SdcardPath_downloads + filename);
            } else if (myNet.netType == NetInfo.MOBILE_OTHER) {
                msgContent = getString(R.string.file_dnothernet);
                it.putExtra("msgContent", msgContent);
                it.putExtra("numItems", 1);
                it.putExtra("ItemCaption0", getString(R.string.close));
                it.putExtra("ItemResult0", RESULT_CANCELED);
                startActivity(it);
                return;
            } else {
                Toast.makeText(this, R.string.file_dnnotnet, Toast.LENGTH_SHORT).show();
                return;
            }
            it.putExtra("msgContent", msgContent);
            it.putExtra("numItems", 2);
            it.putExtra("ItemCaption0", getString(R.string.cancel));
            it.putExtra("ItemResult0", RESULT_CANCELED);
            it.putExtra("ItemCaption1", getString(R.string.download));
            it.putExtra("ItemResult1", RESULT_OK);
            startActivityForResult(it, 50);
        } catch (Exception e) {
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode
                && (new Date().getTime() - enterTime) < 1000)
            return true;
        return super.onKeyDown(keyCode, event);
    }

    private AudioMsgPlayer playingMsg;

    @SuppressLint("NewApi")
    public class MsgListAdapter extends BaseAdapter {
        private Context mContext;

        public MsgListAdapter(Context context) {
            mContext = context;
        }

        public int getCount() {
            return TalkList.size();
        }

        public Object getItem(int position) {
            return TalkList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public void clear() {
            GifView g = null;
            while (GifList.size() > 0 && (g = GifList.get(0)) != null) {
                g.stop();
                GifList.remove(g);
            }
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            SMS msg = TalkList.get(TalkList.size() - position - 1);
            //tml*** hotnews

            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(mContext, R.layout.conversation_cell, null);
                holder.tTime = (TextView) convertView.findViewById(R.id.time);
                holder.balloon = (TextView) convertView.findViewById(R.id.conversation);
                holder.photoimage = (ImageView) convertView.findViewById(R.id.conversation_photo);
                holder.gifview = (GifView) convertView.findViewById(R.id.gifview);
                holder.progress = (ProgressBar) convertView.findViewById(R.id.progressbar);
                holder.audmsg = (AudioMsgPlayer) convertView.findViewById(R.id.audio_msg);
                holder.username = (TextView) convertView.findViewById(R.id.username);  //tml*** group ui1
                holder.title = (RelativeLayout) convertView.findViewById(R.id.title);
                GifList.add(holder.gifview);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
                //alec: finally I did implement recycling.
                //		without this part, the textview will reuse the previous height
                ((RelativeLayout) convertView).removeView(holder.balloon);
                holder.balloon = new TextView(mContext);
                holder.balloon.setTextColor(0xff000000);
                if (largeScreen) {
//					holder.balloon.setMaxWidth((int)(480*mDensity));
                    holder.balloon.setMaxWidth((int) (0.7 * widthScreenZ));  //tml*** hotnews
                    holder.balloon.setPadding((int) (16 * mDensity), (int) (16 * mDensity), (int) (16 * mDensity), (int) (16 * mDensity));
                    holder.balloon.setTextSize(22);
                } else {
//					holder.balloon.setMaxWidth((int)(240*mDensity));  //240
                    holder.balloon.setMaxWidth((int) (0.7 * widthScreenZ));  //tml*** hotnews
                    holder.balloon.setPadding((int) (8 * mDensity), (int) (8 * mDensity), (int) (8 * mDensity), (int) (8 * mDensity));
                    holder.balloon.setTextSize(16);
                }
                ((RelativeLayout) convertView).addView(holder.balloon, new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                if (!msg.content.startsWith("(g.f"))
                    holder.gifview.stop();
            }
            //tml*** group ui1
            holder.tTime.setId(1);
            holder.balloon.setId(2);
            holder.gifview.setId(3);
            holder.photoimage.setId(4);
            holder.username.setId(5);
            holder.title.setId(6);

            //final String orgContent = msg.content;
            if (msg.attached == 8) {
                if (msg.content.startsWith(getString(R.string.video)) && msg.content.contains("(vdo)")
                        && msg.content.lastIndexOf("KB") + 3 == msg.content.length()) {
                    msg.content = msg.content.substring(0, msg.content.length() - 1);
                }
            }
            if (msg.attached == 9) {// video uploaded
                msg.content = "(vdo)";
            }
            if (msg.attached == 0 && msg.att_path_aud == null) {
//				holder.balloon.setAutoLinkMask(Linkify.ALL);
                //tml*** phone intent
                Linkify.addLinks(holder.balloon, Linkify.ALL);
                holder.balloon.setAutoLinkMask(Linkify.ALL);
                holder.balloon.setLinksClickable(true);
                if (holder.balloon.getLinksClickable())
                    holder.balloon.setMovementMethod(LinkMovementMethod.getInstance());
            }
            //tml*** group ui1
            RelativeLayout.LayoutParams lpTitle = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);  //tml*** group ui1
            lpTitle.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            holder.title.setLayoutParams(lpTitle);

            String time = ShowBetterTime(TalkList.size() - position - 1);
            holder.tTime.setText(time);
            RelativeLayout.LayoutParams lpTime = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lpTime.addRule(RelativeLayout.CENTER_HORIZONTAL);
            lpTime.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//			holder.tTime.setId(1);  //tml*** group ui1 X
            holder.tTime.setLayoutParams(lpTime);

            RelativeLayout.LayoutParams lpPhoto;
            if (largeScreen) {
                lpPhoto = new RelativeLayout.LayoutParams((int) (50. * mDensity), (int) (50. * mDensity));
                holder.photoimage.setPadding((int) (5. * mDensity), (int) (5. * mDensity), (int) (5. * mDensity), (int) (5. * mDensity));
            } else {
                lpPhoto = new RelativeLayout.LayoutParams((int) (40. * mDensity), (int) (40. * mDensity));
                holder.photoimage.setPadding((int) (4. * mDensity), (int) (4. * mDensity), (int) (4. * mDensity), (int) (4. * mDensity));
            }

            if (msg.type == 1) {
                lpPhoto.setMargins(largeScreen ? 9 : 6, 0, 0, 0);
                lpPhoto.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                if (inGroup)//alec
                {
                    Drawable f = friendsPhotoMap.get(Integer.valueOf(msg.group_member));
                    holder.photoimage.setImageDrawable(f);
                    //tml*** group ui1
                    RelativeLayout.LayoutParams lpName = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                    lpName.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    lpName.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    lpName.addRule(RelativeLayout.BELOW, holder.tTime.getId());  //tml*** group ui1
                    lpName.setMargins(largeScreen ? (int) (6 * mDensity) : (int) (4 * mDensity), 0,
                            0, largeScreen ? (int) (2 * mDensity) : (int) (1 * mDensity));
                    String nickname = mADB.getNicknameByIdx(msg.group_member);
                    //群组中的陌生人
                    if (nickname.isEmpty()) {
                        android.util.Log.d("MsgListAdapter", "陌生人消息     "+msg.group_member);
                        nickname = mGDB.getGroupMemberNameByGroupIdxAndMemberIdx(mGroupID, msg.group_member);
                    }
                    holder.username.setText(nickname);
                    holder.username.setVisibility(View.VISIBLE);
                    holder.username.setLayoutParams(lpName);
                } else {
                    holder.photoimage.setImageDrawable(friendPhoto);
                    holder.username.setVisibility(View.GONE);
                }
            } else {
                lpPhoto.setMargins(0, 0, largeScreen ? 9 : 6, 0);
                lpPhoto.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                holder.photoimage.setImageDrawable(myphoto);
                holder.username.setVisibility(View.GONE);
            }
//			lpPhoto.addRule(RelativeLayout.BELOW, holder.tTime.getId());
            lpPhoto.addRule(RelativeLayout.BELOW, holder.title.getId());  //tml*** group ui1
            lpPhoto.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            holder.photoimage.setLayoutParams(lpPhoto);
            //tml*** group ui1 X
//			holder.balloon.setId(2);
//			holder.gifview.setId(3);
//			holder.photoimage.setId(4);

            if (msg.content != null && msg.content.startsWith("[<AGREESHARE>]")) {
                String[] res = msg.content.split(",");
                int relation = Integer.valueOf(res[2]);
                msg.content = mContext.getString(
                        R.string.agree_share_sms,
                        mContext.getResources().getStringArray(
                                R.array.share_time)[relation - 1]);
            }

            if ((msg.type == 2 || (msg.type == 1 && !msg.content.contains("(fl)")))
                    && (msg.attached == 8 || msg.attached == 10) && msg.att_path_aud.contains(".mp4")) {
                msg.attached = 9;
                msg.content = "(vdo)";
            }

            String s = msg.content;
            if (msg.attached == 8) {
                if (!(msg.content.startsWith(getString(R.string.video)) && msg.content.contains("(vdo)"))) {

                    int zhao = msg.att_path_aud.lastIndexOf("_");
                    int zhao2 = msg.att_path_aud.lastIndexOf(".");
                    if (zhao != -1 && zhao2 != -1 && zhao2 < zhao) {
                        try {
                            if (!s.startsWith(getString(R.string.filememo_recv)))
                                s = s.substring(getString(R.string.filememo_send).length() + 1, s.length());
                            if (msg.type == 1)
                                s = s.replace("(fl)", "(fl) " + msg.att_path_aud.substring(msg.att_path_aud.lastIndexOf("/") + 1, msg.att_path_aud.lastIndexOf("_")) + " ");
                            else
                                s = s.replace("(fl)", "(fl) " + msg.att_path_aud.substring(msg.att_path_aud.lastIndexOf("/") + 1, msg.att_path_aud.length()) + " ");
                        } catch (Exception e) {
                            if (msg.type == 1)
                                s = getString(R.string.filememo_recv) + " (fl)";
                            else
                                s = getString(R.string.filememo_send) + " (fl)";
                        }
                    }
                }
            }
            if (msg.attached == 10 && s.startsWith(getApplicationContext().getResources().getString(R.string.file))) {
                s = s.replaceFirst(getApplicationContext().getResources().getString(R.string.file), "(fl)");
            }

            //alec
            if (s.startsWith("here I am ("))
                s = "(mAp)";
            else if (s.equals("Missed call"))
                s = "(mCl) " + getString(R.string.missed_call);
            else if (s.startsWith("(Vm)") && s.length() > 4)
                s += '"';

            RelativeLayout.LayoutParams lpProgress = new RelativeLayout.LayoutParams((int) (200. * mDensity), (int) (36. * mDensity));
            lpProgress.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lpProgress.addRule(RelativeLayout.BELOW, holder.balloon.getId());
            lpProgress.addRule(RelativeLayout.LEFT_OF, holder.photoimage.getId());
            holder.progress.setLayoutParams(lpProgress);

            Smiley sm = new Smiley();
            boolean hasGif = false;
            if (sm.hasSmileys(s) > 0) {
                if (spannableCache.get(msg.smsid) == null || msg.smsid == rowID) {
                    SpannableString spannable = new SpannableString(s);
                    for (int i = 0; i < Smiley.MAXSIZE; i++) {
                        for (int j = 0; j < sm.getCount(i); j++) {
                            if (i == (Smiley.MAXSIZE - 1)) {//picture
                                Bitmap picturebitmap = null;
                                try {
//									picturebitmap = ImageUtil.getBitmapAsRoundCornerWithAdaptiveDivision(3, 15, msg.att_path_img);
                                    //tml*** hotnews
                                    int widthSpace = widthScreenZ - (int) ((50 + 50 + 20 + 20 + 20) * mDensity);  //profilepicx2 + paddings + margins
                                    picturebitmap = ImageUtil.getBitmapAsRoundCornerWithAdaptiveDivision(2, 15, msg.att_path_img, widthSpace);
                                } catch (Exception e) {
                                    picturebitmap = null;
                                } catch (OutOfMemoryError e) {
                                    picturebitmap = null;
                                }

                                if (picturebitmap == null) {
                                    spannable = new SpannableString(getString(R.string.notfound_photo));
                                } else {
                                    int start = sm.getStart(i, j);
                                    int end = sm.getEnd(i, j);

                                    ImageSpan icon = new ImageSpan(
                                            ConversationActivity.this, picturebitmap,
                                            ImageSpan.ALIGN_BASELINE);
                                    spannable.setSpan(
                                            icon,
                                            start,
                                            end,
                                            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                                picturebitmap = null;
                                break;
                            } else if (i <= 74) {
                                ImageSpan icon = null;
                                LayerDrawable layers = null;
                                Bitmap bitmap = null;
                                if (i >= 66) {
                                    Drawable d = null;
                                    if (i == 69) { // video
                                        if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
                                            try {
                                                bitmap = ThumbnailUtils
                                                        .createVideoThumbnail(
                                                                new File(msg.att_path_aud).getAbsolutePath(),
                                                                Video.Thumbnails.MICRO_KIND);
                                                if (bitmap != null)
                                                    d = new BitmapDrawable(bitmap);
                                            } catch (Exception e) {
                                            }
                                        }
                                        if (d == null)
                                            d = getResources().getDrawable(R.drawable.start_play);
                                        d.setBounds(0, 0, (int) (30.f * mDensity), (int) (30.f * mDensity));
                                    } else if (i == 70) {
                                        d = getResources().getDrawable(R.drawable.sm01 + i);
                                        d.setBounds(0, 0, (int) (30.f * mDensity), (int) (30.f * mDensity));
                                    } else if (i == 71) {
                                        d = getResources().getDrawable(R.drawable.mapview);
                                        d.setBounds(0, 0, (int) (size2 * mDensity), (int) (size2 * mDensity));
                                    } else if (i == 72) {
                                        d = getResources().getDrawable(android.R.drawable.sym_call_missed);
                                        d.setBounds(0, 0, (int) (size * mDensity), (int) (size * mDensity));
                                    } else if (i == 73) {
                                        d = getResources().getDrawable(android.R.drawable.sym_call_outgoing);
                                        d.setBounds(0, 0, (int) (size * mDensity), (int) (size * mDensity));
                                    } else if (i == 74) {
                                        d = getResources().getDrawable(android.R.drawable.sym_call_incoming);
                                        d.setBounds(0, 0, (int) (size * mDensity), (int) (size * mDensity));
                                    } else if (i == 66) {
                                        d = getResources().getDrawable(R.drawable.sm67);
                                        d.setBounds(0, 0, (int) (150.f * mDensity), (int) (30.f * mDensity));
                                    }
                                    if (msg.attached == 9) {
                                        Bitmap bubbleblue = BitmapFactory.decodeResource(ConversationActivity.this.getResources(),
                                                R.drawable.videosms_play);
                                        Drawable[] array = new Drawable[2];
                                        array[1] = new BitmapDrawable(bubbleblue);
                                        array[0] = new BitmapDrawable(bitmap);
                                        layers = new LayerDrawable(array);
                                        layers.setLayerInset(0, 0, 0, 0, 0);
                                        layers.setLayerInset(1, 0, 0, 0, 0);
                                        layers.setBounds(0, 0, (int) (90.f * mDensity), (int) (90.f * mDensity));
                                        bubbleblue = null;
                                    }
                                    if (icon == null) {
                                        if (msg.attached == 9) {
                                            if (Integer.parseInt(Build.VERSION.SDK) >= 8)
                                                icon = new ImageSpan(layers, ImageSpan.ALIGN_BASELINE);
                                            else
                                                icon = new ImageSpan(getResources().getDrawable(R.drawable.start_play), ImageSpan.ALIGN_BASELINE);
                                            layers = null;
                                        } else
                                            icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
                                    }
                                } else {
                                    icon = new ImageSpan(mContext, R.drawable.sm01 + i, ImageSpan.ALIGN_BOTTOM);
                                }
                                spannable.setSpan(icon, sm.getStart(i, j),
                                        sm.getEnd(i, j),
                                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                                bitmap = null;
                            } else if (i >= 75) {
                                hasGif = true;

                                Drawable dra = getResources().getDrawable(R.drawable.em001 + i - 75);
                                //int h, w;
                                //h=(int)(dra.getIntrinsicHeight()/mDensity);
                                //w=(int)(dra.getIntrinsicWidth()/mDensity);
                                holder.gifview.setGifImageType(GifImageType.SYNC_DECODER);
                                holder.gifview.setImageSize(150, 150);
                                //holder.gifview.setShowDimension(w,h);
                                holder.gifview.setGifImage(R.drawable.em001 + i - 75);
                                break;
                            }
                        }
                    }
                    if (!hasGif)
                        spannableCache.put(msg.smsid, spannable);
                    if (null != msg.att_path_aud && !msg.att_path_aud.startsWith("ulfiles/"))
                        rowID = 0;
                    holder.balloon.setText(spannable);
                } else
                    holder.balloon.setText(spannableCache.get(msg.smsid));
            } else {
                holder.balloon.setText(s);
            }

            if (msg.type == 1)
                holder.balloon.setBackgroundResource(R.drawable.balloon_left);
            else {
                if (msg.status == SMS.STATUS_PENING) {
                    holder.balloon.setBackgroundResource(R.drawable.balloon_right_pending);
                    if (msg.attached == 8 || msg.attached == 9) {
                        holder.progress.setProgress((float) msg.progress / 100.f);
                        holder.progress.setVisibility(View.VISIBLE);
                        mProgress.put(msg.att_path_aud, holder.progress);
                    }
                } else {
                    holder.balloon.setBackgroundResource(R.drawable.balloon_right);
                    holder.progress.setVisibility(View.GONE);
                }
            }

            RelativeLayout.LayoutParams lpBubble = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            if (msg.type == 1) {
                lpBubble.setMargins(largeScreen ? (int) (60. * mDensity) : (int) (50. * mDensity), 0, 0, 0);
                lpBubble.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                holder.gifview.setPadding(15, 3, 14, 8);
//				holder.photoimage.setOnClickListener(mOnClickPhoto);
                //xwf*** beta ui3
                if (inGroup) {
                    ContactsQuery cq = new ContactsQuery(ConversationActivity.this);
                    final int idx = msg.group_member;
                    final String address = mADB.getAddressByIdx(idx);
                    final long contact_id = cq.getContactIdByNumber(address);
                    final String Nickname = mADB.getNicknameByIdx(idx);
                    holder.photoimage.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent it = new Intent(ConversationActivity.this, FunctionActivity.class);
                            it.putExtra("Contact_id", contact_id);
                            it.putExtra("Address", address);
                            it.putExtra("Nickname", Nickname);
                            it.putExtra("Online", ContactsOnline.getContactOnlineStatus(address));
                            it.putExtra("fromGroup", true);
                            if (contact_id > 0)
                                it.putExtra("AireNickname", mADB.getNicknameByAddress(address));
                            it.putExtra("Idx", idx);
                            startActivity(it);
                        }
                    });
                } else {
                    holder.photoimage.setOnClickListener(mOnClickPhoto);
                }
            } else {
                lpBubble.setMargins(0, 0, largeScreen ? (int) (60. * mDensity) : (int) (50. * mDensity), 10);
                lpBubble.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                holder.gifview.setPadding(9, 3, 16, 8);
                holder.photoimage.setOnClickListener(null);
            }
//			lpBubble.addRule(RelativeLayout.BELOW, holder.tTime.getId());
            lpBubble.addRule(RelativeLayout.BELOW, holder.title.getId());  //tml*** group ui1

            holder.balloon.setLayoutParams(lpBubble);
            holder.balloon.setTag(TalkList.size() - position - 1);
            holder.balloon.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (mp2 != null && mp2.isPlaying())
                        return;
                    if (myVP1 != null && myVP1.isPlaying())  //tml*** new vmsg
                        return;
//					if (vmp != null)
                    if (vmp != null || myVP2 != null)  //tml*** new vmsg
                    {
                        if (playingMsg != null) {
                            playingMsg.stop();
                            playingMsg = null;
                        }
                        stopPlayingVoice();
                        return;
                    }
                    int position = Integer.parseInt(v.getTag().toString());
                    SMS msg = TalkList.get(position);
                    Log.d("msgs.onClick " + " 1=" + msg.displayname + " 2=" + msg.address + " 3=" + msg.content
                            + " 4=" + msg.contactid + " 5=" + msg.read + " 6=" + msg.type + " 7=" + msg.status
                            + " 8=" + msg.time + " 9=" + msg.attached + " 10=" + msg.longitudeE6 + " 11=" + msg.latitudeE6
                            + " 12=" + msg.smsid);
                    if (msg.content.startsWith("here I am (")) {
//						try {
//							Class.forName("com.google.android.maps.MapActivity");
//						} catch (ClassNotFoundException e) {
//							Toast.makeText(ConversationActivity.this, R.string.nonsupport_googlemap, Toast.LENGTH_LONG).show();
//							return;
//						} catch (NoClassDefFoundError e) {
//							Toast.makeText(ConversationActivity.this, R.string.nonsupport_googlemap, Toast.LENGTH_LONG).show();
//							return;
//						}
                        //tml*** check google map
                        boolean isCN = MyUtil.isISO_China(ConversationActivity.this, mPref, null);
                        boolean hasGoogleMaps = MyUtil.hasGoogleMap(!isCN, getApplicationContext(), "conv message");
                        if (isCN) {
                            //tml|xwf*** baidu map
                            Intent intent = new Intent(ConversationActivity.this, SendMyBDLocation.class);
                            startActivity(intent);
                        } else if (hasGoogleMaps) {
                            try {
                                String s = msg.content.substring(11);
                                String[] l = s.split(",");
                                l[1] = l[1].substring(0, l[1].length() - 1);
                                long lat = (long) (Double.parseDouble(l[0]) * 1E6);
                                long lon = (long) (Double.parseDouble(l[1]) * 1E6);
                                Log.i("LOC " + msg.content + " parsed:" + lat + "," + lon);
                                FunctionActivity.onLaunchStaticMapView(ConversationActivity.this, lon, lat,
                                        msg.longitudeE6, msg.latitudeE6,
                                        msg.time, msg.displayname, msg.address, msg.contactid, msg.type == 2);
                            } catch (Exception e) {
                            }
                        }
                        return;
                    }
                    if (msg.content.equals("Missed call") && msg.type == 1) {
                        Intent it = new Intent(ConversationActivity.this,
                                CommonDialog.class);
                        it.putExtra("msgContent", String.format(getString(R.string.call_back), mNickname));
                        it.putExtra("numItems", 2);
                        it.putExtra("ItemCaption0", getString(R.string.cancel));
                        it.putExtra("ItemResult0", RESULT_CANCELED);
                        it.putExtra("ItemCaption1", getString(R.string.yes));
                        it.putExtra("ItemResult1", RESULT_OK);
                        startActivityForResult(it, 230);
                        return;
                    }
                    if (msg.attached == 0) return;
                    if (!MyUtil.checkSDCard(ConversationActivity.this))
                        return;
                    if (msg.attached == 1) {
                        onPlayVoiceMemo(msg.att_path_aud);
                        try {
                            View p = (View) v.getParent();
                            AudioMsgPlayer a = (AudioMsgPlayer) p.findViewWithTag("audio");
                            playingMsg = a;
                            a.play();
                        } catch (Exception e) {
                        }
                        return;
                    }
                    if (msg.attached == 2) {
                        File file = new File(msg.att_path_img);
                        if (!file.exists()) {
                            return;
                        }
                    }
                    if (msg.attached == 9) {
                        if (msg.att_path_aud == null) {
                            Toast.makeText(ConversationActivity.this,
                                    getString(R.string.video_err),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        File file = new File(msg.att_path_aud);
                        if (!file.exists()) {
                            return;
                        }
                    }
                    if (msg.att_path_aud != null) {
                        File file = new File(msg.att_path_aud);
                        if ((msg.attached == 8 || msg.attached == 10 || msg.attached == 9) && file.exists()) {
                            OpenDifferentFile openDifferentFile = new OpenDifferentFile(
                                    ConversationActivity.this);
                            openDifferentFile.openFile(msg.att_path_aud);
                            return;
                        }
                    }
                    if (msg.attached == 8 || msg.attached == 9) {
                        if (fileDownloading) {
                            Toast.makeText(ConversationActivity.this,
                                    getString(R.string.filedownloading),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (msg.att_path_aud == null) {
                            Toast.makeText(ConversationActivity.this,
                                    getString(R.string.file_err),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        smsId = msg.smsid;
                        obligate1_phpIP = msg.obligate1;
                        if (obligate1_phpIP == null || obligate1_phpIP.length() == 0)
                            obligate1_phpIP = AireJupiter.myPhpServer_default;
                        //jack hardcode don't like
                        fileDownloadUrl = msg.att_path_img;
                        if (msg.content.startsWith(getString(R.string.video)) && msg.content.contains("(vdo)")) {
                            String len = msg.content.substring(msg.content.indexOf("(vdo)") + 5);
                            type = 1;
                            try {
                                dialogFileDownload(msg.att_path_aud, len.substring(0, len.indexOf("KB") + 3));
                            } catch (Exception e) {
                                try {
                                    dialogFileDownload(msg.att_path_aud, len);
                                } catch (Exception e2) {
                                }
                            }
                            rowID = msg.smsid;
                        } else if (msg.content.startsWith(getString(R.string.filememo_recv)) && msg.content.contains("(fl)")) {
                            String len = msg.content.substring(msg.content.indexOf("(fl)") + 4);
                            type = 2;
                            try {
                                // TODO: 2016/4/6 jack 因为ios Url改变,所以parse Url为准
                                dialogFileDownload(msg.att_path_aud, len.substring(0, len.indexOf("KB") + 3));
                            } catch (Exception e) {
                                dialogFileDownload(msg.att_path_aud, len);
                            }
                            rowID = msg.smsid;
                        }
                        return;
                    }

                    enterTime = new Date().getTime();

                    if (msg.obligate1 != null && msg.obligate1.startsWith("http")) {
                        try {
//							String title=msg.content.substring(6,msg.content.indexOf("\n", 7));
                            String title = msg.content.substring(6);  //wjx*** hot news fix/
                            Intent i = new Intent(ConversationActivity.this, WebViewActivity.class);
                            i.putExtra("URL", msg.obligate1);
                            i.putExtra("Title", title);
                            startActivity(i);
                        } catch (Exception e) {
                        }
                    } else {
                        Intent i = new Intent(ConversationActivity.this, MessageDetailActivity.class);
                        i.putExtra("imagePath", msg.att_path_img);
                        i.putExtra("audioPath", msg.att_path_aud);
                        i.putExtra("msgContent", msg.content);
                        i.putExtra("longitude", msg.longitudeE6);
                        i.putExtra("latitude", msg.latitudeE6);
                        i.putExtra("displayname", mNickname);
                        i.putExtra("time", msg.time);
                        i.putExtra("type", msg.type);
                        i.putExtra("status", msg.status);
                        i.putExtra("address", msg.address);
                        startActivity(i);
                    }
                }
            });
            holder.balloon.setOnLongClickListener(mLongPressBalloonView);
            holder.gifview.setTag(TalkList.size() - position - 1);
            holder.gifview.setLayoutParams(lpBubble);
            if (hasGif) {
                holder.gifview.setVisibility(View.VISIBLE);
                holder.balloon.setVisibility(View.GONE);
            } else {
                holder.balloon.setVisibility(View.VISIBLE);
                holder.gifview.setVisibility(View.GONE);
            }

            holder.gifview.setOnLongClickListener(mLongPressBalloonView);

            if (msg.content.startsWith("(Vm)")) {
                RelativeLayout.LayoutParams lpAudioMsgPlayer = new RelativeLayout.LayoutParams((int) (200. * mDensity), (int) (64. * mDensity));
                lpAudioMsgPlayer.addRule(RelativeLayout.ALIGN_TOP, holder.balloon.getId());
                if (msg.type == 2) {
                    lpAudioMsgPlayer.addRule(RelativeLayout.LEFT_OF, holder.photoimage.getId());
                    holder.audmsg.setBackgroundResource(R.drawable.balloon_right);
                } else {
                    lpAudioMsgPlayer.addRule(RelativeLayout.RIGHT_OF, holder.photoimage.getId());
                    holder.audmsg.setBackgroundResource(R.drawable.balloon_left);
                }
                holder.audmsg.setLayoutParams(lpAudioMsgPlayer);
                holder.audmsg.setTag("audio");

                try {
                    int sec = Integer.parseInt(msg.content.substring(4));
                    holder.audmsg.setDuration(sec);
                } catch (Exception e) {
                    holder.audmsg.setDuration(5);
                }
                holder.audmsg.setVisibility(View.VISIBLE);
                holder.audmsg.bringToFront();
            } else
                holder.audmsg.setVisibility(View.GONE);

            int index = TalkList.indexOf(msg);
            if (index < TalkList.size() - 1) {
                if (msg.time - TalkList.get(index + 1).time < 60000)
                    holder.tTime.setVisibility(View.GONE);
                else
                    holder.tTime.setVisibility(View.VISIBLE);
            } else
                holder.tTime.setVisibility(View.VISIBLE);

            return convertView;
        }

        long preTime = 0;
        long curTime = 0;

        private String ShowBetterTime(int position) {
            if (position == TalkList.size() - 1) {
                preTime = 0;
            } else {
                if (position == 0) {
                    preTime = TalkList.get(position).time;
                } else
                    preTime = TalkList.get(position - 1).time;
            }
            curTime = TalkList.get(position).time;
            String preFormat = DateUtils.formatDateTime(
                    getApplicationContext(), preTime,
                    DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE);
            String mFormat = DateUtils.formatDateTime(getApplicationContext(),
                    curTime, DateUtils.FORMAT_SHOW_YEAR
                            | DateUtils.FORMAT_SHOW_DATE);
            String curFormat = DateUtils.formatDateTime(
                    getApplicationContext(), new Date().getTime(),
                    DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE);
            String tFormat;

            if (preFormat.equals(mFormat) && curFormat.equals(mFormat))
                tFormat = DateUtils.formatDateTime(getApplicationContext(),
                        curTime, DateUtils.FORMAT_SHOW_TIME
                                | DateUtils.FORMAT_CAP_AMPM);
            else
                tFormat = DateUtils.formatDateTime(getApplicationContext(),
                        curTime, DateUtils.FORMAT_SHOW_TIME
                                | DateUtils.FORMAT_SHOW_WEEKDAY
                                | DateUtils.FORMAT_SHOW_YEAR
                                | DateUtils.FORMAT_SHOW_DATE
                                | DateUtils.FORMAT_CAP_AMPM);

            return tFormat;
        }
    }

    class ViewHolder {
        TextView tTime;
        TextView balloon;
        ImageView photoimage;
        GifView gifview;
        ProgressBar progress;
        AudioMsgPlayer audmsg;
        ImageView warnUnsent;
        TextView username;  //tml*** group ui1
        RelativeLayout title;
    }

    BroadcastReceiver HandleListChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent == null) return;

            if (intent.getAction().equals(Global.Action_MsgGot)) {

                ArrangeTalkList();

                if (intent.getStringExtra("autoPath") != null
                        && intent.getStringExtra("autoPath").length() != 0
                        && intent.getIntExtra("msgAttach", 0) != 8) {
                    try {
                        if (vmp != null) {
                            vmp.stop();
                            vmp = null;
                        }
                        if (myVP2 != null) {  //tml*** new vmsg
                            myVP2.stop();
                            myVP2 = null;
                        }
                    } catch (Exception e) {
                        vmp = null;
                        myVP2 = null;
                        Log.e("Converse listchange1 !@#$ " + e.getMessage());
                    }
                    onPlayVoiceMemo(intent.getStringExtra("autoPath"));
                }
                if (mPref.readBoolean("recvVibrator", true)) {
                    long[] patern = {0, 20, 1000};
                    mVibrator.vibrate(patern, -1);
                }
            } else if (intent.getAction().equals(Global.Action_MsgSent)) {
                if (AireJupiter.notifying) return;
                /*String address=intent.getStringExtra("SendeeAddress");
				if (address!=null && !mAddress.equals(address))
					return;*/
                ArrangeTalkList();
            } else if (intent.getAction().equals(Global.Action_InternalCMD)) {
                int command = intent.getIntExtra("Command", 0);
                if (command == Global.CMD_INCOMING_CALL) {
                    //TODO
                    mAttached |= 1; // audio
                    ShowAttchment(0);
                }
                return;
            } else if (intent.getAction().equals(Global.ACTION_PLAY_OVER)) {
                Log.d("ACTION_PLAY_OVER received");
                if (spAnimation != null) {
                    spAnimation.stop();
                    AnimationDrawablestate = true;
                    speaker.setVisibility(View.GONE);
                }
                try {
                    if (vmp != null) {
                        vmp.stop();
                        vmp = null;
                    }
                    if (myVP2 != null) {  //tml*** new vmsg
                        myVP2.stop();
                        myVP2 = null;
                    }
                } catch (Exception e) {
                    vmp = null;
                    myVP2 = null;
                    Log.e("Converse listchange2 !@#$ " + e.getMessage());
                }
                return;
            } else if (intent.getAction().equals(Global.Action_SMS_Fail)) {
                Toast.makeText(ConversationActivity.this,
                        getString(R.string.smsfail), Toast.LENGTH_SHORT).show();
                return;
            }else if (intent.getAction().equals(Global.Action_Hide_Group_Icon)) {
                //jack 收到此广播隐藏群设置图标
                android.util.Log.d("ConversationActivity", "隐藏图标");
                mSetting.setVisibility(View.INVISIBLE);
            }

            if (msgListAdapter != null)
                msgListAdapter.notifyDataSetChanged();
            if (TalkList.size() > 0) {
                listview.setSelection(TalkList.size() - 1);
            }
        }
    };

    public void onPlayVoiceMemo(String path) {
        if (vmp != null)
            return;
        if (myVP2 != null)
            return;
        try {
            if (path.endsWith("amr")) {
                vmp = new VoiceMemoPlayer_NB(this);
                vmp.setDataSource(path);
                vmp.prepare();
                vmp.start();
            } else {
                //tml*** new vmsg
                myVP2 = new VoicePlayer2_MP(ConversationActivity.this, path);
                myVP2.start();
            }
//			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);  //tml*** voicemsg fix
            speaker.setVisibility(View.VISIBLE);
            if (spAnimation != null) spAnimation.start();
            AnimationDrawablestate = false;
        } catch (IllegalArgumentException e) {
            Log.e("Converse playvoice1 !@#$ " + e.getMessage());
            vmp = null;
            myVP2 = null;
            return;
        } catch (IllegalStateException e) {
            Log.e("Converse playvoice2 !@#$ " + e.getMessage());
            vmp = null;
            myVP2 = null;
            return;
        } catch (IOException e) {
            Log.e("Converse playvoice3 !@#$ " + e.getMessage());
            vmp = null;
            myVP2 = null;
            return;
        }
        startWakeLock();
    }

    long enabletime;
    KeyguardManager mKeyguardManager;
    KeyguardManager.KeyguardLock mKeyguardLock;
    boolean enabled;

    void disableKeyguard() {
        if (AmazonKindle.IsKindle()) return;
        if (mKeyguardManager == null) {
            mKeyguardManager = (KeyguardManager) this
                    .getSystemService(Context.KEYGUARD_SERVICE);
            mKeyguardLock = mKeyguardManager.newKeyguardLock("MessageActivity");
            enabled = true;
        }
        if (enabled) {
            mKeyguardLock.disableKeyguard();
            enabled = false;
            enabletime = SystemClock.elapsedRealtime();
        }
    }

    void reenableKeyguard() {
        if (AmazonKindle.IsKindle()) return;
        if (!enabled) {
            try {
                if (Integer.parseInt(Build.VERSION.SDK) < 5)
                    Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            mKeyguardLock.reenableKeyguard();
            enabled = true;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Integer.parseInt(Build.VERSION.SDK) < 5
                || Integer.parseInt(Build.VERSION.SDK) > 7)
            disableKeyguard();

    }

    @Override
    public void onStop() {
        if (Integer.parseInt(Build.VERSION.SDK) < 5
                || Integer.parseInt(Build.VERSION.SDK) > 7)
            reenableKeyguard();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (smileClicked = true) {
            smileClicked = false;
        }
        if (AnimationDrawablestate) {
            super.onBackPressed();
        } else {
            stopPlayingVoice();
        }

        if (mCallMode) {  //tml*** chatview
            if (mFromCallMode == 1) {
                ContactsQuery cq = new ContactsQuery(ConversationActivity.this);
                Intent itc = new Intent(ConversationActivity.this, VideoCallActivity.class);
                itc.putExtra("address", mAddress);
                itc.putExtra("nickname", mADB.getNicknameByAddress(mAddress));
                itc.putExtra("contactid", cq.getContactIdByNumber(mAddress));
                itc.putExtra("RestartVideo", true);
                startActivity(itc);
            } else if (mFromCallMode == 2) {
                Intent itc = new Intent(ConversationActivity.this, DialerActivity.class);
                itc.putExtra("RestartVideo", false);
                startActivity(itc);
            } else {
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent itcall = new Intent(Global.MSG_RETURN_NOM);
                    itcall.putExtra("RestartVideo", true);
                    sendBroadcast(itcall);
                }
            }, 500);
            finish();
        }
    }

    private void playSoundTouch() {
        if (mPref.readBoolean("sendVibrator", true)) {
            long[] patern = {0, 40, 1000};
            mVibrator.vibrate(patern, -1);
        }
    }

    public void stopPlayingVoice() {
//		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);  //tml*** voicemsg fix
        try {
            if (vmp != null) {
                vmp.stop();
                vmp = null;
                spAnimation.stop();
                speaker.setVisibility(View.GONE);
                AnimationDrawablestate = true;
            }
            if (myVP2 != null) {  //tml*** new vmsg
                myVP2.stop();
                myVP2 = null;
                spAnimation.stop();
                speaker.setVisibility(View.GONE);
                AnimationDrawablestate = true;
            }
            if (playingMsg != null) {
                playingMsg.stop();
                playingMsg = null;
            }
        } catch (Exception e) {
            Log.e("Converse stopplayvoice1 !@#$ " + e.getMessage());
        }
    }

    public void sendLocation() {
        Intent it = new Intent();
        it = new Intent(ConversationActivity.this,
                CommonDialog.class);
        it.putExtra("msgContent", String.format(getString(R.string.send_location), mNickname));
        it.putExtra("numItems", 2);
        it.putExtra("ItemCaption0", getString(R.string.cancel));
        it.putExtra("ItemResult0", RESULT_CANCELED);
        it.putExtra("ItemCaption1", getString(R.string.yes));
        it.putExtra("ItemResult1", RESULT_OK);
        startActivityForResult(it, 7);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
//		case R.id.view_profile:
//			{
//				Intent it=new Intent(ConversationActivity.this, FunctionActivity.class);
//				it.putExtra("Contact_id", mContactId);
//				it.putExtra("Address", mAddress);
//				it.putExtra("Nickname", mNickname);
//				if (mContactId>0)
//					it.putExtra("AireNickname", mADB.getNicknameByAddress(mAddress));
//				it.putExtra("Idx", mIdx);
//				it.putExtra("fromConversation", true);
//				it.putExtra("CallMode", mCallMode);
//				startActivity(it);
//			}
//			break;
            case R.id.attachment:
//			if (mAttached!=0) return;
//			if(!showitem){
//				messageitem.startAnimation(fadein);
//				messageitem.setVisibility(View.VISIBLE);
//				mHandler.post(new Runnable() {
//					@Override
//					public void run() {
//						listview.scrollBy(0,messageitem.getHeight());
//					}
//				});
//				showitem=true;
//			}else{
//				messageitem.startAnimation(fadeout);
//				mHandler.postDelayed(new Runnable() {
//					@Override
//				    public void run() {
//						messageitem.setVisibility(View.GONE);
//						listview.scrollBy(0,-messageitem.getHeight());
//					}
//				},350);
//				showitem=false;
//			}
//			mHandler.post(new Runnable() {
//				@Override
//				public void run() {
//					listview.setSelection(TalkList.size() - 1);
//				}
//			});
                //tml*** beta ui
//			Intent ita = new Intent(ConversationActivity.this, FunctionActivity.class);
//			ita.putExtra("Contact_id", mContactId);
//			ita.putExtra("Address", mAddress);
//			ita.putExtra("Nickname", mNickname);
//			if (mContactId > 0) ita.putExtra("AireNickname", mADB.getNicknameByAddress(mAddress));
//			ita.putExtra("Idx", mIdx);
//			ita.putExtra("fromConversation", true);
//			ita.putExtra("CallMode", mCallMode);
//			startActivity(ita);
                //tml|xwf*** beta ui2
                showFunctions(true);
                break;
            case R.id.voicesms:
                if (!MyUtil.checkSDCard(ConversationActivity.this)) {
                    Toast.makeText(this, getString(R.string.no_sdcard),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!AmazonKindle.hasMicrophone(this))
                    return;

//			messageitem.setVisibility(View.GONE);
                showitem = false;
//			if (mAttached == 8) {
//				Toast.makeText(ConversationActivity.this,
//						getString(R.string.fileandvideosingle),
//						Toast.LENGTH_SHORT).show();
//				return;
//			}
//			SrcAudioPath = Global.SdcardPath_sent + getRandomName() + ".amr";
//			Intent it = new Intent(ConversationActivity.this,
//					VoiceRecordingDialog.class);
//			it.putExtra("path", SrcAudioPath);
//			startActivityForResult(it, 15);
                onVoiceSMS();
                break;
//		case R.id.picturesms:
//			messageitem.setVisibility(View.GONE);
//			showitem=false;
//			onPickPicture();
//			break;
//		case R.id.photosms:
//			messageitem.setVisibility(View.GONE);
//			showitem=false;
//			onTakePicture();
//			break;
//		case R.id.videosms:
//			messageitem.setVisibility(View.GONE);
//			showitem=false;
//			onPickVideo();
//			break;
//		case R.id.filesms:
//			messageitem.setVisibility(View.GONE);
//			showitem=false;
//			onFileTransfer();
//			break;
//		case R.id.location:
//			messageitem.setVisibility(View.GONE);
//			showitem=false;
//			sendLocation();
//			break;
			/*
		case R.id.guard:
			messageitem.setVisibility(View.GONE);
			showitem=false;
			it = new Intent(ConversationActivity.this,
					CommonDialog.class);
			it.putExtra("msgContent", String.format(getString(R.string.send_suvei), mNickname));
			it.putExtra("numItems", 3);
			it.putExtra("ItemCaption0", getString(R.string.cancel));
			it.putExtra("ItemResult0", RESULT_CANCELED);
			it.putExtra("ItemCaption1", getString(R.string.stop));
			it.putExtra("ItemResult1", 333);
			it.putExtra("ItemCaption2", getString(R.string.start));
			it.putExtra("ItemResult2", RESULT_OK);
			startActivityForResult(it, 131);
			break;*/
            case R.id.moresms:
                listnumber += 20;
                int len1 = TalkList.size();
                ArrangeTalkList();
                msgListAdapter.notifyDataSetChanged();
                int len2 = TalkList.size();
                listview.setSelectionFromTop(len2 - len1, moresms.getHeight() + 10);
                break;
//		case R.id.call:
//			if (!AmazonKindle.hasMicrophone(ConversationActivity.this))
//				return;
//			MakeCall.Call(ConversationActivity.this, mAddress, false);
//			break;
            case R.id.smile:
                if (!smileClicked) {
                    startActivityForResult(new Intent(ConversationActivity.this, SmileyActivity.class), 200);
                    smileClicked = true;
                }
                break;
            case R.id.sendmsg:
                onSend();
                showFunctions(false);  //tml|xwf*** beta ui2
                break;
            case R.id.cancel:
//			finish();
                //tml*** chatview
                if (mFromCallMode == 1) {
                    //xwf*** beta ui3
                    startActivity(new Intent(ConversationActivity.this, MessageActivity.class));
                    finish();
//				ContactsQuery cq = new ContactsQuery(ConversationActivity.this);
//				Intent itc = new Intent(ConversationActivity.this, VideoCallActivity.class);
//				itc.putExtra("address", mAddress);
//				itc.putExtra("nickname", mADB.getNicknameByAddress(mAddress));
//				itc.putExtra("contactid", cq.getContactIdByNumber(mAddress));
//				itc.putExtra("RestartVideo", true);
//				startActivity(itc);
                } else if (mFromCallMode == 2) {
                    Intent itc = new Intent(ConversationActivity.this, DialerActivity.class);
                    itc.putExtra("RestartVideo", false);
                    startActivity(itc);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent itcall = new Intent(Global.MSG_RETURN_NOM);
                            itcall.putExtra("RestartVideo", true);
                            sendBroadcast(itcall);
                        }
                    }, 500);
                } else {
                }
                finish();
                break;
            case R.id.voice:
                if (mp2 != null && mp2.isPlaying())
                    return;
                if (myVP1 != null && myVP1.isPlaying())  //tml*** new vmsg
                    return;
                if (vmp != null)
                    return;
                if (SrcAudioPath != null && (new File(SrcAudioPath).length() > 0)) {
                    try {
                        if (SrcAudioPath.endsWith("amr")) {
                            mp2 = new VoiceMemoPlayer_NB(ConversationActivity.this);
                            mp2.setDataSource(SrcAudioPath);
                            mp2.prepare();
                            mp2.start();
                        } else {
                            //tml*** new vmsg
                            myVP1 = new VoicePlayer2_MP(ConversationActivity.this, SrcAudioPath);
                            myVP1.start();
                        }
                        speaker.setVisibility(View.VISIBLE);
                        spAnimation.start();
                        AnimationDrawablestate = false;
                    } catch (IOException e) {
                        Log.e("Converse voice1 !@#$ " + e.getMessage());
                        mp2 = null;
                        myVP1 = null;
                        return;
                    } catch (IllegalArgumentException e) {
                        Log.e("Converse voice2 !@#$ " + e.getMessage());
                        mp2 = null;
                        myVP1 = null;
                        return;
                    } catch (IllegalStateException e) {
                        Log.e("Converse voice3 !@#$ " + e.getMessage());
                        mp2 = null;
                        myVP1 = null;
                        return;
                    }
                }
                break;
            case R.id.deletefile:
                SrcVideoPath = null;
                SrcImagePath = null;
                SrcAudioPath = null;
                mAttached = 0;
                ShowAttchment(0);
//			mSend.setVisibility(View.INVISIBLE);
//			mVoice.setVisibility(View.VISIBLE);
                toggleSendVoiceBtn(1);
                break;
            case R.id.call:  //tml|xwf*** beta ui2
                finish();
                if (inGroup) {
                    broadcastConf = false;  //tml*** broadcast
                    try {

                        if (sendeeList.size() > 0 && sendeeList.size() <= 9) {
                            AireVenus.setCallType(AireVenus.CALLTYPE_CHATROOM);
                            mPref.write("incomingChatroom", false);

                            new Thread(sendNotifyForJoinChatroom).start();

                            int myIdx = 0;
                            try {
                                myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
                                mPref.write("ChatroomHostIdx", myIdx);
                            } catch (Exception e) {
                            }

                            String idx = "" + myIdx;
                            MakeCall.ConferenceCall(getApplicationContext(), idx);
                        } else {
                            if (sendeeList.size() > 9)
                                Toast.makeText(getApplicationContext(), "Conference does not support more than 9 people", Toast.LENGTH_LONG).show();
                        }

                    } catch (Exception e) {
                        Log.e("Conv Call !@#$ " + e.getMessage());
                    }
                } else {
                    MakeCall.Call(ConversationActivity.this, mAddress, false);
                }
                break;
            case R.id.videocall:  //tml|xwf*** beta ui2
                finish();
                if (inGroup) {
                    if (mPref.readBoolean("BROADCAST", false)) {
                        broadcastConf = true;  //tml*** broadcast
                    } else {
                        broadcastConf = false;
                    }
                    try {

                        if (sendeeList.size() > 0 && sendeeList.size() <= 9) {
                            AireVenus.setCallType(AireVenus.CALLTYPE_CHATROOM);
                            mPref.write("incomingChatroom", false);

                            new Thread(sendNotifyForJoinChatroom).start();

                            int myIdx = 0;
                            try {
                                myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
                                mPref.write("ChatroomHostIdx", myIdx);
                            } catch (Exception e) {
                            }

                            String idx = "" + myIdx;
                            MakeCall.ConferenceCall(getApplicationContext(), idx);
                        } else {
                            if (sendeeList.size() > 9)
                                Toast.makeText(getApplicationContext(), "Conference does not support more than 9 people", Toast.LENGTH_LONG).show();
                        }

                    } catch (Exception e) {
                        Log.e("Conv VideoCall !@#$ " + e.getMessage());
                    }
                } else {
                    MakeCall.Call(ConversationActivity.this, mAddress, true, false);
                }
                break;
            case R.id.walkietalkie:  //tml|xwf*** beta ui2
//			if (isSTB || inGroup) {  //tml*** detect STB
                if (true) {  //tml|alex*** rwt byebye X
                    if (!MyUtil.checkSDCard(ConversationActivity.this)) {
                        Toast.makeText(this, getString(R.string.no_sdcard), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!AmazonKindle.hasMicrophone(this))
                        return;
                    showitem = false;
                    onVoiceSMS();
                } else {
                    if (!MyUtil.checkNetwork(ConversationActivity.this))
                        return;
                    Intent it = new Intent(ConversationActivity.this, WalkieTalkieDialog.class);
                    it.putExtra("Contact_id", mContactId);
                    it.putExtra("Address", mAddress);
                    it.putExtra("Idx", mIdx);
                    startActivity(it);
                }
                break;
            case R.id.picmsg:  //tml|xwf*** beta ui2
                if (!MyUtil.checkSDCard(ConversationActivity.this))
                    return;
                onPickPictureOption();
                break;
            case R.id.location:  //tml|xwf*** beta ui2
                onChooseLocation();
                break;
            case R.id.guard:  //tml|xwf*** beta ui2
                onPickSUVOption();  //tml*** iot control
                break;
        }

    }

    //tml*** iot control
    public void onPickSUVOption() {
        final CharSequence[] items = {
                getResources().getString(R.string.surveillance),
                getResources().getString(R.string.home_iot_sensors),
                getResources().getString(R.string.suv_status),
                getResources().getString(R.string.home_monitor)};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                int activityResult = 0;
                String msgContent = null;
                if (item == 0) {
                    msgContent = String.format(getString(R.string.send_suvei), mNickname);
                    activityResult = 131;
                } else if (item == 1) {
                    msgContent = String.format(getString(R.string.send_suv_iotall), mNickname);
                    activityResult = 132;
                } else if (item == 2) {
                    msgContent = null;
                    activityResult = 232;
                } else if (item == 3) {
                    if (AireJupiter.getInstance() != null
                            && AireJupiter.getInstance().tcpSocket() != null) {
                        AireJupiter.getInstance().tcpSocket()
                                .send(mAddress, Global.MONITOR, 0, null, null, 0, null);
                        mPref.write("GuardYou", true);
                    }
                }


                if (activityResult != 0 && msgContent != null) {
                    Intent itg = new Intent(ConversationActivity.this, CommonDialog.class);
                    itg.putExtra("msgContent", msgContent);
                    itg.putExtra("numItems", 3);
                    itg.putExtra("ItemCaption0", getString(R.string.cancel));
                    itg.putExtra("ItemResult0", RESULT_CANCELED);
                    itg.putExtra("ItemCaption1", getString(R.string.stop));
                    itg.putExtra("ItemResult1", CommonDialog.STOP_SUV);
                    itg.putExtra("ItemCaption2", getString(R.string.start));
                    itg.putExtra("ItemResult2", RESULT_OK);
                    startActivityForResult(itg, activityResult);
                } else if (activityResult == 232) {
                    Intent itg = new Intent(ConversationActivity.this, SuvStatusActivity.class);
                    itg.putExtra("SendeeNumber", mAddress);
                    itg.putExtra("SendeeDisplayname", mNickname);
                    startActivity(itg);
                }
                dialog.dismiss();
            }
        });

        builder.setTitle(ConversationActivity.this.getResources().getString(R.string.security));
        builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    //tml|xwf*** beta ui2
    private void showFunctions(boolean show) {
        if (!show) {
            ((LinearLayout) findViewById(R.id.functions)).setVisibility(View.GONE);
            return;
        }
        hideKeyboard();
        if (((LinearLayout) findViewById(R.id.functions)).getVisibility() == View.GONE) {
            ((LinearLayout) findViewById(R.id.functions)).setVisibility(View.VISIBLE);
            checkSecurityAccess();
        } else {
            ((LinearLayout) findViewById(R.id.functions)).setVisibility(View.GONE);
        }
    }

    private void checkSecurityAccess() {
        boolean found1 = false;
//		for (int i = 0; i < Global.MAX_SUVS; i++) {
//			String address = mPref.read("Suvei" + i);
//			if (address != null) {
//				if (address.equals(mAddress)) {
//					found = true;
//					break;
//				}
//			}
//		}
        found1 = MyProfile.load().givenSecurityAccess(mAddress);
        if (!found1) {
            ((ImageView) findViewById(R.id.guard)).setBackgroundColor(Color.GRAY);
            if (largeScreen) {
                ((ImageView) findViewById(R.id.guard)).setPadding((int) (20 * mDensity),
                        (int) (15 * mDensity), (int) (20 * mDensity), (int) (40 * mDensity));
            } else {
                ((ImageView) findViewById(R.id.guard)).setPadding((int) (20 * mDensity),
                        (int) (10 * mDensity), (int) (20 * mDensity), (int) (30 * mDensity));
            }
            ((ImageView) findViewById(R.id.guard)).setEnabled(false);
        } else {
            ((ImageView) findViewById(R.id.guard)).setBackgroundResource(R.drawable.tabbtn);
            if (largeScreen) {
                ((ImageView) findViewById(R.id.guard)).setPadding((int) (20 * mDensity),
                        (int) (15 * mDensity), (int) (20 * mDensity), (int) (40 * mDensity));
            } else {
                ((ImageView) findViewById(R.id.guard)).setPadding((int) (20 * mDensity),
                        (int) (10 * mDensity), (int) (20 * mDensity), (int) (30 * mDensity));
            }
            ((ImageView) findViewById(R.id.guard)).setEnabled(true);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(((EditText) findViewById(R.id.msginput)).getWindowToken(), 0);
    }

    Runnable sendNotifyForJoinChatroom = new Runnable() {
        public void run() {
            String myIdxHex = mPref.read("myID", "0");

            String ServerIP = mPref.read("conferenceSipServer", AireJupiter.myConfSipServer_default);
            if (AireJupiter.getInstance() != null) {
                ServerIP = AireJupiter.getInstance().getIsoConf(ServerIP);  //tml*** china ip
            }
            long ip = MyUtil.ipToLong(ServerIP);
            String HexIP = Long.toHexString(ip);

            String content = Global.Call_Conference + "\n\n" + HexIP + "\n\n" + myIdxHex;
            //tml*** broadcast
            if (broadcastConf) {
                mPref.write("BCAST_CONF", 1);
                content = Global.Call_Conference + Global.Call_Broadcast + "\n\n" + HexIP + "\n\n" + myIdxHex;
            } else {
                mPref.write("BCAST_CONF", -1);
            }

            for (int i = 0; i < sendeeList.size(); i++) {
                int idx = Integer.parseInt(sendeeList.get(i));
                if (idx < 50) continue;

                String address = mADB.getAddressByIdx(idx);

                if (AireJupiter.getInstance() != null && AireJupiter.getInstance().tcpSocket != null) {
                    if (AireJupiter.getInstance().tcpSocket.isLogged(false)) {
                        if (i > 0) MyUtil.Sleep(500);
                        Log.d("voip.inviteConf1 " + address + " " + content);
                        AireJupiter.getInstance().tcpSocket.send(address, content, 0, null, null, 0, null);
                    }
                }
            }
        }
    };

    private void onChooseLocation() {
        final CharSequence[] items = {  //tml|xwf*** beta ui2
                getResources().getString(R.string.mylocation_address),
                getResources().getString(R.string.sharing_location)};
        final CharSequence[] items_G = {  //tml|xwf*** beta ui2
                getResources().getString(R.string.mylocation_address)};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (isSTB || inGroup) {  //tml*** detect STB, +group
            builder.setItems(items_G, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    if (item == 0)
                        sendLocation();
                    dialog.dismiss();
                }
            });
        } else {
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    if (item == 0)
                        sendLocation();
                    else if (item == 1)
                        shareLocation();
                    dialog.dismiss();
                }
            });
        }

        builder.setTitle(ConversationActivity.this.getResources().getString(R.string.fafauser_map));
        builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void shareLocation() {
//		try {
//			Class.forName("com.google.android.maps.MapActivity");
//		} catch (ClassNotFoundException e) {
//			Toast.makeText(ConversationActivity.this, R.string.nonsupport_googlemap, Toast.LENGTH_LONG).show();
//		    return;
//		} catch (NoClassDefFoundError e) {
//			Toast.makeText(ConversationActivity.this, R.string.nonsupport_googlemap, Toast.LENGTH_LONG).show();
//		    return;
//		}
        //tml*** check google map
        boolean isCN = MyUtil.isISO_China(ConversationActivity.this, mPref, null);
        boolean hasGoogleMaps = MyUtil.hasGoogleMap(!isCN, getApplicationContext(), "conv shareLocation");
        if (!hasGoogleMaps & !isCN) {
            return;
        }

        mHandler.post(popupProgressDialog);
        (new Thread(new Runnable() {
            public void run() {
                if (onQueryLocation() == 1)
                    finish();
            }
        })).start();

    }

    private int onQueryLocation() {
        if (mIdx < 50) {
            mHandler.post(dismissProgressDialog);
            FunctionActivity.onLaunchMapView(ConversationActivity.this, -122033418, 37309488,
                    new Date().getTime(), mNickname, mAddress, mContactId);
            return 1;
        }

        String Return = "";
        long pos_time = 0;
        int errorCode = 0;
        int lat = 0, lon = 0;
        long endtime = mPref.readLong(mAddress, 0);
        Message msg = new Message();

        if (new Date().getTime() / 1000 - endtime > 0) // Whether send the request for sharing
        {
            try {
                int count = 0;
                String myPhoneNumber = mPref.read("myPhoneNumber", "++++");
                do {
                    MyNet net = new MyNet(ConversationActivity.this);
                    Return = net.doPost("querysharing.php",
                            "queryid=" + URLEncoder.encode(mAddress, "UTF-8") +
                                    "&id=" + URLEncoder.encode(myPhoneNumber, "UTF-8"), null);
                } while ((Return.length() == 0 || Return.startsWith("Error")) && ++count < 3);

                mHandler.post(dismissProgressDialog);

                if (Return.length() == 0) {
                    msg.arg1 = R.string.nonetwork;
                    mHandler.sendMessage(msg);
                    return 1;
                } else if (Return.startsWith("NonMember")) {
                    msg.arg1 = R.string.nonmember_no_service;
                    mHandler.sendMessage(msg);
                    return 1;
                } else {
                    int relation = Integer.parseInt(Return);
                    if (relation == 0 || new Date().getTime() / 1000 - endtime > 0) {
                        Intent it = new Intent(ConversationActivity.this,
                                CommonDialog.class);
                        it.putExtra("msgContent",
                                getString(R.string.request_location_sharing));
                        it.putExtra("numItems", 2);
                        it.putExtra("ItemCaption0", getString(R.string.cancel));
                        it.putExtra("ItemResult0", RESULT_CANCELED);
                        it.putExtra("ItemCaption1", getString(R.string.yes));
                        it.putExtra("ItemResult1", RESULT_OK);
                        startActivityForResult(it, 7);
                        return 0;
                    }
                }
            } catch (Exception e) {
                msg.arg1 = R.string.nonetwork;
                mHandler.sendMessage(msg);
                return 1;
            }
        }

        mHandler.post(dismissProgressDialog);

        try {
            if (AireJupiter.getInstance() == null) {
                msg.arg1 = R.string.nonetwork;
                mHandler.sendMessage(msg);
                return 1;
            }
            Return = AireJupiter.getInstance().getFriendLocation(mAddress);
        } catch (Exception e) {
            msg.arg1 = R.string.nonetwork;
            mHandler.sendMessage(msg);
            return 1;
        }
        if (Return == null) {
            msg.arg1 = R.string.nonetwork;
            mHandler.sendMessage(msg);
            return 1;
        }

        String items[] = Return.split("/");
        if (items.length >= 3) {
            try {
                lat = Integer.parseInt(items[0]) + 3512113;
                lon = Integer.parseInt(items[1]) - 10958121;
                pos_time = Long.parseLong(items[2], 16);
                pos_time *= 1000;
            } catch (NumberFormatException e) {
                errorCode = -1;
                lat = 0;
                lon = 0;
            }
        }

        if (errorCode < 0) {
            msg.arg1 = R.string.location_not_found;
            mHandler.sendMessage(msg);
        } else {
            FunctionActivity.onLaunchMapView(ConversationActivity.this, lon, lat,
                    pos_time, mNickname, mAddress, mContactId);
        }
        return 1;
    }

    private ProgressDialog progress = null;
    Runnable popupProgressDialog = new Runnable() {
        @Override
        public void run() {
            try {
                progress = ProgressDialog.show(ConversationActivity.this, "", getString(R.string.in_progress), true, true);
            } catch (Exception e) {
            }
        }
    };

    Runnable dismissProgressDialog = new Runnable() {
        @Override
        public void run() {
            try {
                if (progress.isShowing())
                    progress.dismiss();
            } catch (Exception e) {
            }
        }
    };

    //***tml
    private void onSend()  //TODO multithread in sendfileagent
    {
        mSend.setEnabled(false);

        mMsgText = mInput.getText().toString();
        mPref.delect("draft" + mContactId);
        int len = mMsgText.length();
        if (mAttached == 3)
            mMsgText = "(Vm)(iMG)" + (len == 0 ? "" : ("\n" + mMsgText));
        else if ((mAttached & 1) == 1)
            mMsgText = "(Vm)" + (len == 0 ? "" : ("\n" + mMsgText));
        else if ((mAttached & 2) == 2)
            mMsgText = "(iMG)" + (len == 0 ? "" : ("\n" + mMsgText));
        else if (mAttached == 8) {
            if (ConversationActivity.fileUploading) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.fileuploading),
                        Toast.LENGTH_SHORT).show();
                mSend.setEnabled(true);
                return;
            }
            File file = new File(SrcAudioPath);
            NumberFormat format = DecimalFormat.getInstance();
            format.setMaximumFractionDigits(2);
            String length = format.format(file.length() / 1024.0).replace(",", "");
            try {
                if (Double.valueOf(length) > 102400) { // 100M
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.fileLarge), Toast.LENGTH_SHORT)
                            .show();
                    mSend.setEnabled(true);
//					mVoice.setVisibility(View.VISIBLE);
//					mSend.setVisibility(View.INVISIBLE);
                    toggleSendVoiceBtn(1);
                    return;
                }
            } catch (Exception e) {
            }
            if (isVideo)
                mMsgText = "(vdo)" + length
                        + (len == 0 ? " KB" : (" KB\n" + mMsgText));
            else
                mMsgText = "(fl)" + length
                        + (len == 0 ? " KB" : (" KB\n" + mMsgText));
        } else {
            if (mMsgText.trim().equals("")) {  //tml*** msg control
                mInput.setText("");
                mSend.setEnabled(true);
                return;
            }
        }
        if (mAddress == null || mAddress.length() == 0) {
            mSend.setEnabled(true);
            return;
        }
        mAddress = MyTelephony.attachPrefix(ConversationActivity.this, mAddress);

        mADB.updateLastContactTimeByAddress(mAddress, new Date().getTime());
        if (UsersActivity.sortMethod == 1)
            UsersActivity.needRefresh = true;

        if (mAttached == 8) {
            if (videobitmap != null)
                videobitmap.recycle();

            fileAgent = new SendFileAgent(this, myIdx, true);

            if (inGroup) {
                fileAgent.setAsGroup(mGroupID);
                android.util.Log.d("发送消息", "mAttached ==8 文件" + " addressList " + addressList + " mMsgText " + mMsgText + " mAttached " + mAttached + " SrcAudioPath " + SrcAudioPath + " SrcImagePath " + SrcImagePath);
                GroupMsg groupMsg = new GroupMsg("", mAttached + "", SrcAudioPath, mMsgText, "");
                //jack 2.4.51
                if (!fileAgent.onGroupSend(groupMsg)) {
//					mSend.setEnabled(true);
//					mVoice.setVisibility(View.VISIBLE);
//					mSend.setVisibility(View.INVISIBLE);
                    toggleSendVoiceBtn(2);
                    toggleSendVoiceBtn(3);
                } else {
                    addMsgtoTalklist(true);
                    playSoundTouch();
                }
            } else {
                if (!fileAgent.onSend(mAddress, mMsgText, mAttached,
                        SrcAudioPath, SrcImagePath, false)) {
//					mSend.setEnabled(true);
//					mVoice.setVisibility(View.VISIBLE);
//					mSend.setVisibility(View.INVISIBLE);
                    toggleSendVoiceBtn(2);
                    toggleSendVoiceBtn(3);
                } else {
                    addMsgtoTalklist(true);
                    playSoundTouch();
                }
            }
            SrcAudioPath = null;
        } else {
            agent = new SendAgent(ConversationActivity.this, myIdx, mIdx, true);

            if (inGroup) {
                agent.setAsGroup(mGroupID);
                GroupMsg groupMsg = null;
                android.util.Log.d("发送消息", "requestCode == else 文字和图片" + " addressList " + addressList + " mMsgText " + mMsgText + " mAttached " + mAttached + " SrcAudioPath " + SrcAudioPath + " SrcImagePath " + SrcImagePath);
                if (mAttached == 2) {
                    //图片
                    groupMsg = new GroupMsg("", mAttached + "", SrcImagePath, mMsgText, "");
                } else if (mAttached == 0) {
                    //文字
                    groupMsg = new GroupMsg("", mAttached + "", "", mMsgText, "");
                }
                if (!agent.onGroupSend(groupMsg)) {
                    mSend.setEnabled(true);
                } else {
                    addMsgtoTalklist(false);
                    playSoundTouch();
                }
            } else {
                if (!agent.onSend(mAddress, mMsgText, mAttached, SrcAudioPath, SrcImagePath, false))
                    mSend.setEnabled(true);
                else {
                    addMsgtoTalklist(false);
                    playSoundTouch();
                }
            }

            SrcAudioPath = null;
        }
    }

    //tml*** chatview
    Runnable flash_sendee = new Runnable() {
        @Override
        public void run() {
            if (mCallMode && mSendee != null) {
                ((TextView) findViewById(R.id.sendee)).setTextColor(Color.parseColor("#ffd200"));
                mHandler.postDelayed(unflash_sendee, 1000);
            }
        }
    };

    Runnable unflash_sendee = new Runnable() {
        @Override
        public void run() {
            if (mCallMode && mSendee != null) {
                ((TextView) findViewById(R.id.sendee)).setTextColor(Color.parseColor("#ffffff"));
                mHandler.postDelayed(flash_sendee, 1000);
            }
        }
    };

    private void toggleSendVoiceBtn(int mode) {
        if (mode == 0) {
            if (mCallMode) {
                mSend.setVisibility(View.VISIBLE);
                mVoice.setVisibility(View.INVISIBLE);
                mSend.setEnabled(true);
            } else {
                mSend.setVisibility(View.VISIBLE);
                mVoice.setVisibility(View.INVISIBLE);
            }
        } else if (mode == 1) {
            if (mCallMode) {
                mSend.setVisibility(View.VISIBLE);
                mVoice.setVisibility(View.INVISIBLE);
                mSend.setEnabled(false);
            } else {
                mSend.setVisibility(View.INVISIBLE);
                mVoice.setVisibility(View.VISIBLE);
            }
        } else if (mode == 2) {
            if (mCallMode) {
                mSend.setVisibility(View.VISIBLE);
                mVoice.setVisibility(View.INVISIBLE);
                mSend.setEnabled(false);
            } else {
                mSend.setVisibility(View.VISIBLE);
                mVoice.setVisibility(View.INVISIBLE);
            }
        } else {
            mSend.setEnabled(true);
        }
    }

    //***tml
    //tml*** phone intent
    private String linkedPhone;
    private boolean linkHandled = false;

    @Override
    public void startActivity(Intent intent) {
        String iAction = intent.getAction();
        if (iAction != null
                && (iAction.equals(Intent.ACTION_DIAL) || iAction.equals(Intent.ACTION_VIEW))
                && !linkHandled) {
            Uri iDataPhone = intent.getData();
            if (iDataPhone != null) {
                linkedPhone = iDataPhone.toString();
                if (linkedPhone.startsWith("tel:")) {
                    linkedPhone = linkedPhone.replace("tel:", "");
                } else if (linkedPhone.startsWith("voicemail:")) {
                    linkedPhone = linkedPhone.replace("voicemail:", "");
                } else {
                    linkHandled = false;
                    super.startActivity(intent);
                    return;
                }

                Log.d("link phone! " + iAction.toString() + " " + linkedPhone);
                final CharSequence[] listItems = {
                        getResources().getString(R.string.contacts),
                        getResources().getString(R.string.dial_this),
                        getResources().getString(R.string.search)};

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder adBuilder = new AlertDialog.Builder(ConversationActivity.this);
                        adBuilder.setItems(listItems, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                String uriPhone = linkedPhone;
                                if (item == 0) {
                                    linkHandled = true;
                                    Intent it = new Intent(Intent.ACTION_INSERT,
                                            ContactsContract.Contacts.CONTENT_URI);
                                    it.putExtra(ContactsContract.Intents.Insert.PHONE, uriPhone);
                                    startActivity(it);
                                    finish();
                                } else if (item == 1) {
                                    linkHandled = true;
                                    uriPhone = "tel:" + uriPhone;
                                    Intent it = new Intent(Intent.ACTION_DIAL);
                                    it.setData(Uri.parse(uriPhone));
                                    startActivity(it);
                                } else if (item == 2) {
                                    uriPhone = MyTelephony.cleanPhoneNumber3(uriPhone);
                                    Intent it = new Intent(ConversationActivity.this, SearchDialog.class);
                                    it.putExtra("passKeyword", uriPhone);
                                    startActivity(it);
                                    finish();
                                }
                                dialog.dismiss();
                            }
                        });

                        adBuilder.setTitle(linkedPhone);
                        adBuilder.setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int item) {
                                    }
                                });
                        AlertDialog aDialogPh = adBuilder.create();
                        aDialogPh.show();
                    }
                });
            } else {
                linkHandled = false;
                super.startActivity(intent);
            }
        } else {
            linkHandled = false;
            super.startActivity(intent);
        }
    }
    //***tml
}
