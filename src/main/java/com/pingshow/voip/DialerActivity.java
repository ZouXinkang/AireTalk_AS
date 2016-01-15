package com.pingshow.voip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.PhoneNumberUtils;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.pingshow.airecenter.AireApp;
import com.pingshow.airecenter.AireCallPage;
import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.AmazonKindle;
import com.pingshow.airecenter.ConversationActivity;
import com.pingshow.airecenter.DQRates;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MainActivity;
import com.pingshow.airecenter.MakeCall;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.PlayService;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.SelectUserActivity;
import com.pingshow.airecenter.SettingPage;
import com.pingshow.airecenter.UserPage;
import com.pingshow.airecenter.adapter.MemberAdapter;
import com.pingshow.airecenter.adapter.MultiMemberAdapter;
import com.pingshow.airecenter.bean.ChatroomMember;
import com.pingshow.airecenter.cons.Key;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.db.AireCallLogDB;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.network.MyNet;
import com.pingshow.network.MySocket;
import com.pingshow.network.NetInfo;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.LedSpeakerUtil;
import com.pingshow.util.MCrypt;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.core.Version;
import com.pingshow.voip.core.VoipAddress;
import com.pingshow.voip.core.VoipCall;
import com.pingshow.voip.core.VoipCall.State;
import com.pingshow.voip.core.VoipCore;
import com.pingshow.voip.core.VoipCoreException;
import com.pingshow.voip.core.VoipCoreListener;
import com.pingshow.voip.core.VoipProxyConfig;

public class DialerActivity extends Activity implements VoipCoreListener {
    // private static boolean ENvidconf = VideoConf.EN_VC;

    private boolean isMultiMember;
    private int curGroupIndex = -1;

    private final int RINGLIMITX = 50000;

    private String mAddress = "";
    private TextView mDisplayNameView;
    private ImageView mProfileImage;

    private TextView mStatus;
    private TextView mTimerLabel;
    private Button mHangup;
    private Button mAnswer;
    private ImageView mMinimize;
    private ImageView mChatView;
    private ImageView mMute;
    private ImageView mSpeaker;
    private ImageView mHold;
    private Button mHideKeypad;
    private boolean videoCall;
    private boolean launchingVideo = false;
    private boolean streamsRunning = false;
    private boolean shouldConsumeCredit = false;
    private boolean incomingChatroom = false;
    public static long PSTNCallLogRowId = -1;
    public static float previousCredit;
    private boolean shouldCheckPSTNinChatroom = false;
    public static boolean isMobileNumber = true;
    public static int cIndex;
    public static String cIso = "us";
    private Bitmap photoBitmap = null;
    private XWalkView xWalkWebView = null;

    private boolean created = false;
    private static DialerActivity theDialer;


    // private VideoConf videoConf=null;

    private String mDisplayName = null;
    private AudioManager mAudioManager;
    private PowerManager.WakeLock mWakeLock;
    private MyPreference mPref;
    private long contact_id;
    private int PreviousVolume;
    public static boolean speakerOn = false;

    private AmpUserDB mADB;
    private String sysIncomingNumber = null;
    private boolean BluetoothSco = false;

    public boolean imCalling = false;

    final int VIDEO_VIEW_ACTIVITY = 100;

    private static boolean Connected;
    public static boolean incomingCall;
    private long startTime;

    private String phoneNumber;
    private ContactsQuery cq;

    private boolean sendTerminateSignal = true;
    private boolean bCommercial = false;
    private String DTMFString = "";
    private float consumedCredit;
    public static boolean rejectHangingup = false;

    private ToneGenerator tg = null;

    public boolean HangingUp = false;

    private boolean isMuted;
    private boolean isHeld;
    private boolean isKeypadShowed;

    public static boolean minimized;
    private boolean isVideoCall = false;

    private String[] cpuSet = new String[4];
    private int testValue1 = 1;

    private boolean needblink; // wjx*** ledspeaker/

    private SpeechRecognizer mSpeechRecognizer;
    private MyRecognitionListener voiceListener;
    private boolean isHost;// 是否为主叫方
    String mainTag1 = "main1", mainTag2 = "main2";
    String subTag1 = "sub1", subTag2 = "sub2";
    LinearLayout mainVC, subVC;
    private boolean isOnceMem;
    private boolean isNowMem;
    private int multiNum = 3;//li*** 每组个数
    private Handler handler = new Handler() {
        @SuppressWarnings("unchecked")
        public void handleMessage(android.os.Message msg) {
            try {
                switch (msg.what) {
                    case 1:
                        TextView textview = (TextView) ((HashMap<String, Object>) msg.obj)
                                .get("textview");
                        String text = (String) ((HashMap<String, Object>) msg.obj)
                                .get("text");
                        textview.setText(text);
                        break;
                    case 2:
                        ImageView imageview = (ImageView) ((HashMap<String, Object>) msg.obj)
                                .get("imageview");
                        Bitmap bm = (Bitmap) ((HashMap<String, Object>) msg.obj)
                                .get("image");
                        imageview.setImageBitmap(bm);
                        break;
                    case 3:
                        Button btn = (Button) msg.obj;
                        btn.setVisibility(msg.arg1);
                        if (msg.arg1 == View.VISIBLE)
                            btn.requestFocus();
                        break;
                    case 4:
                        LinearLayout linear = (LinearLayout) msg.obj;
                        linear.setVisibility(msg.arg1);
                        linear.bringToFront();
                        break;
                    case 5: // tml*** prefocus
                        Button btn2 = (Button) msg.obj;
                        int vis = btn2.getVisibility();
                        if (vis == View.VISIBLE)
                            btn2.requestFocus();
                        break;
                    case 6:
                        if (isOnceMem && !isNowMem) {
                            endCall();
                        }
                        break;
                }
            } catch (Exception e) {
                Log.e("da1 " + e.getMessage());
            }
        }

        ;
    };

    private int myIdx;

    public static DialerActivity getDialer() {
        return theDialer;
    }

    protected static DialerActivity instance() {
        if (theDialer == null) {
            throw new RuntimeException("DialerActivity not instanciated yet");
        } else {
            return theDialer;
        }
    }

    public boolean resetMyProxy() {
        return true;
    }

    public boolean changeMyProxy(String newServer) {
        return true;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("*** !!! DIALER *** START START !!! *** voip");

        mPref = new MyPreference(DialerActivity.this);
        ringrdy = false;
        speakerOn = false;
        emptyCall = 0;
        prevRecvpkts = 0;
        mPref.readInt("ChatroomHostIdx");

        myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
        isHost = mPref.readInt("ChatroomHostIdx") == myIdx;

        lockScreenOrientation();
        mPref.write("disableVideo", false); // tml*** disable video
        theDialer = this;
        consumedCredit = 0;

        isMultiMember = mPref.readBoolean(Key.MULTI_MEMBER_CONF, false);

        allowMouse = true; // tml*** answer view
        // tml*** abort dialer, when no call
        boolean selfinit = getIntent().getBooleanExtra("Selfinit", false);
        if (AireVenus.instance() != null) {
            if (AireVenus.callstate_AV == null && !selfinit) {
                Log.e("exit.AV1 onCreate >> inCall0 - redirect to MAIN");
                Intent it = new Intent(DialerActivity.this, MainActivity.class);
                startActivity(it);
                finish();
                return;
            } else {
                // Log.e("onCreate >> AV1 inCall1 D:" + created);
            }
        } else {
            if (AireVenus.callstate_AV == null && !selfinit) {
                Log.e("exit.AV0 onCreate >> inCall0 - redirect to MAIN");
                Intent it = new Intent(DialerActivity.this, MainActivity.class);
                startActivity(it);
                finish();
                return;
            } else {
                // Log.e("onCreate >> AV0 inCall1 D:" + created);
            }
        }
        // ***tml

        if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) { // tml|yang***
            // vidconf
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
                XWalkPreferences.setValue(
                        XWalkPreferences.ANIMATABLE_XWALK_VIEW, false);
                // if (Log.enDEBUG)
                // XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING,
                // true);
                xWalkWebView = new XWalkView(theDialer, DialerActivity.this);
                xWalkWebView.clearCache(false);
                Log.i("vidConfxwalk xWalkWebView ready "
                        + xWalkWebView.getXWalkVersion() + ","
                        + xWalkWebView.getAPIVersion());
            }
        }

        mHandler.postDelayed(startDialerStuff, 250);
        // tml*** voice control
        if (mPref.readBoolean("voice_control", false)) {
            if (!MyUtil.isISO_China(DialerActivity.this, mPref, null)) {
                mSpeechRecognizer = SpeechRecognizer
                        .createSpeechRecognizer(this);
                voiceListener = new MyRecognitionListener();
                mSpeechRecognizer.setRecognitionListener(voiceListener);
            }
        }
        // **tml

        showTestDialog(); // tml test

    }

    // tml test
    private void showTestDialog() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String testmode = "DEMO TEST:";
                boolean t1 = mPref.readBoolean("BROADCAST", false);
                boolean t2 = mPref.readBoolean("VCx8", false);
                if (t1 || t2) {
                    if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {
                        if (t1)
                            testmode = testmode + "\n\n_ " + "BROADCAST";
                        if (t2)
                            testmode = testmode + "\n\n_ " + "VCx8";
                        Toast tst = Toast.makeText(DialerActivity.this,
                                testmode, Toast.LENGTH_LONG);
                        tst.setGravity(Gravity.CENTER, 0, 0);
                        tst.show();
                    }
                }
            }
        });
    }

    // tml*** voice control
    private class MyRecognitionListener implements RecognitionListener {
        @Override
        public void onBeginningOfSpeech() {
            Log.e("tmlvoice onBeginningOfSpeech");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.e("tmlvoice onBufferReceived");
            if (Log.enDEBUG)
                ((TextView) findViewById(R.id.voiceresults))
                        .append(" onBufferReceived (" + buffer.length + ")\n");
        }

        @Override
        public void onEndOfSpeech() {
            Log.e("tmlvoice onEndOfSpeech");
            ((ProgressBar) findViewById(R.id.voicerms)).setProgress(2);
        }

        @Override
        public void onError(int error) {
            ((ProgressBar) findViewById(R.id.voicerms)).setProgress(2);
            mSpeechRecognizer.cancel();
            String errortext = "";
            if (error == 3) {
                errortext = "audio";
            } else if (error == 5) {
                errortext = "client/other";
            } else if (error == 9) {
                errortext = "permissions";
            } else if (error == 2) {
                errortext = "network1";
            } else if (error == 1) {
                errortext = "network2";
            } else if (error == 7) {
                errortext = "no match";
            } else if (error == 8) {
                errortext = "busy";
            } else if (error == 4) {
                errortext = "server";
            } else if (error == 6) {
                errortext = "timeout";
            }
            if (voiceIntent != null) {
                Log.e("tmlvoice tryagain 1ERROR=" + error + " " + errortext);
                if (!mPref.readBoolean("normal_ring", true))
                    startSpeechListen("start");
            } else {
                Log.e("tmlvoice tryagain 0ERROR=" + error + " " + errortext);
                if (!mPref.readBoolean("normal_ring", true)) {
                    mHandler.postDelayed(voiceRecogn, 500);
                }
            }
            if (Log.enDEBUG)
                ((TextView) findViewById(R.id.voiceresults)).append(" e"
                        + error + " (" + errortext + ")\n");
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            // Log.e("tmlvoice onEvent");
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            // Log.e("tmlvoice onPartialResults");
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            // Log.e("tmlvoice onReadyForSpeech");
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = null;
            String[] cases_answer = {
                    getResources().getString(R.string.v_answer0a),
                    getResources().getString(R.string.v_answer0b),
                    getResources().getString(R.string.v_answer0c),
                    getResources().getString(R.string.v_answer0d),
                    getResources().getString(R.string.v_answer0e),
                    getResources().getString(R.string.v_answer0f),
                    getResources().getString(R.string.v_answer0g),
                    getResources().getString(R.string.v_answer0h),
                    getResources().getString(R.string.v_answer1a),
                    getResources().getString(R.string.v_answer1b),
                    getResources().getString(R.string.v_answer1c),
                    getResources().getString(R.string.v_answer1d),
                    getResources().getString(R.string.v_answer1e),
                    getResources().getString(R.string.v_answer1f),
                    getResources().getString(R.string.v_answer1g),
                    getResources().getString(R.string.v_answer2a),
                    getResources().getString(R.string.v_answer2b),
                    getResources().getString(R.string.v_answer2c),
                    getResources().getString(R.string.v_answer2d),
                    getResources().getString(R.string.v_answer2e),
                    getResources().getString(R.string.v_answer2f),
                    getResources().getString(R.string.v_answer3a)};
            String[] cases_answerCancel = {
                    getResources().getString(R.string.v_end0a),
                    getResources().getString(R.string.v_end1a),
                    getResources().getString(R.string.v_end2a)};
            String[] cases_hangup = {getResources()
                    .getString(R.string.v_end0a)};

            if (results != null) {
                matches = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String matchA = "", matchAC = "", spoke = "";
                if (matches != null && matches.size() > 0) {
                    boolean matchEDcasesA = false, matchEDcasesAC = false, matchEDcasesH = false;
                    for (int i = 0; i < matches.size(); i++) {
                        spoke = matches.get(i).toLowerCase(Locale.getDefault())
                                + "," + spoke;
                    }

                    if (((Button) findViewById(R.id.answer)).getVisibility() == View.VISIBLE) {
                        for (int j = 0; j < cases_answer.length; j++) {
                            if (spoke.contains(cases_answer[j]
                                    .toLowerCase(Locale.getDefault()))) {
                                matchA = cases_answer[j].toLowerCase(Locale
                                        .getDefault());
                                matchEDcasesA = true;
                                break;
                            }
                        }
                        for (int j = 0; j < cases_answerCancel.length; j++) {
                            if (spoke.contains(cases_answerCancel[j]
                                    .toLowerCase(Locale.getDefault()))) {
                                matchAC = cases_answerCancel[j]
                                        .toLowerCase(Locale.getDefault());
                                matchEDcasesAC = true;
                                break;
                            }
                        }
                    } else if (((Button) findViewById(R.id.hangup))
                            .getVisibility() == View.VISIBLE) {
                        // for (int j = 0; j < cases_hangup.length; j++) {
                        // if
                        // (spoke.contains(cases_hangup[j].toLowerCase(Locale.getDefault())))
                        // {
                        // match =
                        // cases_hangup[j].toLowerCase(Locale.getDefault());
                        // matchEDcasesH = true;
                        // break;
                        // }
                        // }
                    }

                    if (Log.enDEBUG)
                        ((TextView) findViewById(R.id.voiceresults))
                                .append(spoke);
                    Log.e("tmlvoice DA onResults (" + matches.size()
                            + ") matches >>> " + spoke);
                    if (matchEDcasesA) {
                        if (Log.enDEBUG)
                            ((TextView) findViewById(R.id.voiceresults))
                                    .append(" !!!MATCH.A" + "\n");
                        Log.e("tmlvoice MATCH.answer SUCCESS! = " + matchA);
                        toastMaker(
                                getResources().getString(R.string.call)
                                        + ": "
                                        + getResources().getString(
                                        R.string.answer), 20,
                                Toast.LENGTH_LONG, Gravity.CENTER, 0, 0);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((ImageView) findViewById(R.id.voicerecogn))
                                        .setBackground(getResources()
                                                .getDrawable(
                                                        R.drawable.bg_round_on1));
                                ((ProgressBar) findViewById(R.id.voicerms))
                                        .setIndeterminate(true);
                                ((Button) findViewById(R.id.answer))
                                        .performClick();
                            }
                        });
                    } else if (matchEDcasesAC) {
                        if (Log.enDEBUG)
                            ((TextView) findViewById(R.id.voiceresults))
                                    .append(" !!!MATCH.AC" + "\n");
                        Log.e("tmlvoice MATCH.answerCancel SUCCESS! = "
                                + matchAC);
                        toastMaker(
                                getResources().getString(R.string.call)
                                        + ": "
                                        + getResources().getString(
                                        R.string.cancel), 20,
                                Toast.LENGTH_LONG, Gravity.CENTER, 0, 0);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((ImageView) findViewById(R.id.voicerecogn))
                                        .setBackground(getResources()
                                                .getDrawable(
                                                        R.drawable.bg_round_on1));
                                ((ProgressBar) findViewById(R.id.voicerms))
                                        .setIndeterminate(true);
                                onBackPressed();
                            }
                        });
                    } else if (matchEDcasesH) {

                    } else {
                        if (Log.enDEBUG)
                            ((TextView) findViewById(R.id.voiceresults))
                                    .append("\n");
                        Log.e("tmlvoice TRYAGAIN!");
                        mHandler.postDelayed(runRESpeechListen0, 50);
                    }
                } else {
                    Log.e("tmlvoice tryagain MATCHES=0");
                    mHandler.postDelayed(runRESpeechListen0, 50);
                    if (Log.enDEBUG)
                        ((TextView) findViewById(R.id.voiceresults))
                                .append(" matches0" + "\n");
                }
            } else {
                Log.e("tmlvoice tryagain RESULTS=null");
                mHandler.postDelayed(runRESpeechListen0, 50);
                if (Log.enDEBUG)
                    ((TextView) findViewById(R.id.voiceresults))
                            .append(" results null" + "\n");
            }
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            int rmsdB_bar = (int) (rmsdB * 4);
            if (rmsdB_bar < 2) {
                rmsdB_bar = 2;
            } else if (rmsdB_bar > 100) {
                rmsdB_bar = 100;
            }
            if (rmsdB_bar != ((ProgressBar) findViewById(R.id.voicerms))
                    .getProgress()) {
                ((ProgressBar) findViewById(R.id.voicerms))
                        .setProgress(rmsdB_bar);
            }
        }
    }

    // ***tml
    // alec*** vidconf
    // void setGLRenderer()
    // {
    // // int []res_id={R.id.preview_2, R.id.preview_3, R.id.preview_4,
    // R.id.preview_5};
    // int []res_id={R.id.preview_2, R.id.preview_3, R.id.preview_4};
    // for (int i=0;i<VideoConf.maxDisplays;i++)
    // {
    // SurfaceView vw=(SurfaceView)findViewById(res_id[i]);
    // if (vw!=null)
    // {
    // GLVideoWindow videoWindow = new GLVideoWindow(vw);
    // videoWindow.setListener(new GLVideoWindow.VideoWindowListener() {
    // public void onVideoRenderingSurfaceReady(GLVideoWindow vw, SurfaceView
    // surface) {
    // int i=vw.getIndex();
    // if (P2P.getInstance()!=null)
    // {
    // P2P.getInstance().setVideoWindow(vw, i);
    // }
    // Log.d("tmlvc GLVideoWindow.Listener ConfVideo [" + i + "] SurfaceReady");
    // }
    //
    // public void onVideoRenderingSurfaceDestroyed(GLVideoWindow vw) {
    // int i=vw.getIndex();
    // if (P2P.getInstance()!=null)
    // {
    // P2P.getInstance().setVideoWindow(null, i);
    // }
    // Log.d("tmlvc GLVideoWindow.Listener ConfVideo [" + i + "] Destroyed!");
    // }
    // });
    //
    // videoWindow.init(i);
    // }
    // }
    // }
    // ***alec

    private MemberAdapter memberAdapter;
    private MultiMemberAdapter multiMemberAdapter;
    Runnable startDialerStuff = new Runnable() {


        public void run() {
            setContentView(R.layout.dialer);

            Connected = false;
            launchingVideo = false;

            tg = new ToneGenerator(-1, 75);

            // register call status listener
            cq = new ContactsQuery(DialerActivity.this);
            mADB = new AmpUserDB(DialerActivity.this);
            mADB.open();

            mAddress = "";
            mDisplayNameView = (TextView) findViewById(R.id.displayname);

            String Brand = Build.BRAND.toLowerCase();
            String Product = Build.PRODUCT.toLowerCase();
            String Model = Build.MODEL.toLowerCase();
            String scrnLay = ((RelativeLayout) findViewById(R.id.talking_frame))
                    .getTag().toString();

            Log.i(scrnLay + " BRAND:" + Brand + " PRODUCT:" + Product
                    + " MODEL:" + Model);

            try {
                FileOutputStream file = new FileOutputStream(new File(
                        "/mnt/sdcard/.com.airecenter/.proinfo"));
                String out = Brand + "\n" + Product + "\n" + Model + "\n";

                // alec: always 0;
                out += "0\n";

                out += "128\n600\n128\n600\n30000\n";

                if (mPref.readBoolean("enable_double_audio", false))
                    out += "1\n";
                else
                    out += "0\n";

                if (mPref.readBoolean("enable_jitter_buffer", false))
                    out += "1\n";
                else
                    out += "0\n";

                if (mPref.readBoolean("enable_jitter_compensation", false))
                    out += "1\n";
                else
                    out += "0\n";

                if (mPref.readBoolean("enable_antijitter", true))
                    out += "1\n";
                else
                    out += "0\n";

                if (mPref.readBoolean("flush_audio", true))
                    out += "1\n";
                else
                    out += "0\n";

                if (mPref.readBoolean("enable_ec", true))
                    out += "1\n";
                else
                    out += "0\n";

                if (mPref.readBoolean("enable_dump_raw", false))
                    out += "1\n";
                else
                    out += "0\n";

                // OPUS
                // out+="128\n"; // 300Hz //128
                // out+="300\n"; // 500Hz //300
                // out+="128\n"; // 5KHz //128
                // out+="128\n"; // 6KHz //128
                // out+="0\n"; // 300Hz
                // out+="100\n"; // 500Hz
                // out+="128\n"; // 5KHz
                // out+="300\n"; // 6KHz
                // out+="0\n"; // 300Hz
                // out+="100\n"; // 500Hz
                // out+="128\n"; // 5KHz
                // out+="200\n"; // 6KHz
                out += "400\n"; // 300Hz //beta10+
                out += "300\n"; // 500Hz
                out += "600\n"; // 5KHz
                out += "300\n"; // 6KHz
                // out+="300\n"; // 300Hz //new mic
                // out+="0\n"; // 500Hz
                // out+="100\n"; // 5KHz
                // out+="250\n"; // 6KHz
                out += "0\n"; // LowPass Filter @6K
                // AMR
                out += "256\n"; // 300Hz
                out += "300\n"; // 500Hz
                out += "200\n"; // 5KHz
                // //bitrate ratio of 128K or 64K
                // if (mPref.read("moodcontent", "--").startsWith("bitrate")){
                // String bit=mPref.read("moodcontent", "--").substring(7, 9);
                // out+=bit+"\n"; // BitRate
                // }else{
                // out+="20\n";
                // }
                // tml*** bitrate
                if (mPref.readInt("BitrateSel", 1) >= 0) {
                    String bit = Integer.toString(mPref
                            .readInt("BitrateSel", 1));
                    if (bit.equals("0")) {
                        bit = "512000";
                    } else if (bit.equals("1")) {
                        bit = "960000";
                    } else if (bit.equals("2")) {
                        bit = "1600000";
                    } else {
                        bit = "960000";
                    }
                    out += bit + "\n"; // BitRate
                    Log.e("tml DA bitrate=" + bit);
                } else {
                    // bitrate ratio of 128K or 64K
                    if (mPref.read("moodcontent", "--").startsWith("bitrate")) {
                        String bit = mPref.read("moodcontent", "--").substring(
                                7, 9);
                        out += bit + "\n"; // BitRate
                    } else {
                        out += "1000000\n";
                    }
                }
                // ***tml
                // out+="10\n"; // BitRate, China use 3 to 5, USA and Taiwan use
                // 10-20
                out += "0\n"; // Force sending side go relay or P2P
                // tml*** hd2 720P/1080P
                if (mPref.readBoolean("EnableHD", true)) {
                    out += "1\n"; // enable
                    Log.i("tmlhd DA 720 true > out+=1");
                } else {
                    out += "0\n";
                    Log.i("tmlhd DA 720 false > out+=0");
                }
                if (mPref.readBoolean("EnableHD2", false)) {
                    out += "1\n"; // enable
                    Log.i("tmlhd DA 1080 true > out+=1");
                } else {
                    out += "0\n";
                    Log.i("tmlhd DA 1080 false > out+=0");
                }
                // ***tml
                // tml|sw*** mixer path, "1" is for new MIC, "0" is for OLD mic
                if (mPref.readBoolean("OldMic_mixer", false)) {
                    out += "0\n";
                } else {
                    out += "1\n";
                }

                file.write(out.getBytes());
                file.flush();
                file.close();
            } catch (Exception e) {
                Log.e("da2 " + e.getMessage());
            }

            mAudioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
            Bluetooth.init();
            if (Bluetooth.isAvailable()) {
                if (Bluetooth.isSupported()) {
                    Log.i("Bluetooth isAvailable isSupported");
                    mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                    mAudioManager.setMode(AudioManager.MODE_NORMAL);
                    mAudioManager.setSpeakerphoneOn(true);
                    mAudioManager.setBluetoothScoOn(true);
                    mAudioManager.setMicrophoneMute(false);
                    Bluetooth.enable(true);
                    BluetoothSco = true;
                }
            }

            // PreviousVolume=mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            PreviousVolume = mPref.readInt("PreviousVolume", mAudioManager
                    .getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL));

            contact_id = getIntent().getLongExtra("Contact_id", 0);
            videoCall = getIntent().getBooleanExtra("VideoCall", false);
            bCommercial = (AireVenus.getCallType() == AireVenus.CALLTYPE_WEBCALL);

            mProfileImage = (ImageView) findViewById(R.id.bighead);

            mHangup = (Button) findViewById(R.id.hangup);
            mHangup.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    endCall();
                }

            });
            // mHangup.setEnabled(false); //tml*** beta ui
            // mMinimize = (ImageView) findViewById(R.id.minimize);
            // mMinimize.setOnClickListener(new OnClickListener() {
            // @Override
            // public void onClick(View arg0) {
            // minimized = true;
            // // tml*** vidconf
            // // mHandler.removeCallbacks(showPanel);
            // // mHandler.removeCallbacks(hidePanel);
            // // mHandler.post(resumePanel);
            // // if (((ToggleButton)
            // // findViewById(R.id.video)).isChecked()) {
            // // ((ToggleButton)
            // // findViewById(R.id.video)).setChecked(false);
            // // }
            // // ***tml
            // Intent it;
            // if (MainActivity._this != null)
            // it = new Intent(DialerActivity.this, MainActivity.class);
            // else if (ShoppingActivity._this != null)
            // it = new Intent(DialerActivity.this,
            // ShoppingActivity.class);
            // else if (LocationSettingActivity._this != null)
            // it = new Intent(DialerActivity.this,
            // LocationSettingActivity.class);
            // else if (BDMapViewLocation._this != null)
            // it = new Intent(DialerActivity.this,
            // BDMapViewLocation.class);
            // else if (SecurityNewActivity._this != null)
            // it = new Intent(DialerActivity.this,
            // SecurityNewActivity.class);
            // // tml*** browser save
            // else if (MainBrowser._this != null)
            // it = new Intent(DialerActivity.this, MainBrowser.class);
            // // ***tml
            // else
            // it = new Intent(DialerActivity.this, MainActivity.class);
            // it.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            // startActivity(it);
            // }
            // });
            // tml*** alpha ui
            // tml*** chatview
            mChatView = (ImageView) findViewById(R.id.chatview);
            if ((AireVenus.instance() != null && (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM || AireVenus
                    .getCallType() == AireVenus.CALLTYPE_AIRECALL))
                    || videoCall) {
                mChatView.setVisibility(View.GONE);
            }
            mChatView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    minimized = true;
                    String address = mAddress;
                    String nickname = mADB.getNicknameByAddress(mAddress);
                    Long contactId = cq.getContactIdByNumber(mAddress);
                    Log.e("tmlf call addr/nick/contact == " + address + "/"
                            + nickname + "/" + contactId);
                    Intent it = new Intent(DialerActivity.this,
                            ConversationActivity.class);
                    it.putExtra("SendeeContactId", contactId);
                    it.putExtra("SendeeNumber", address);
                    it.putExtra("SendeeDisplayname", nickname);
                    it.putExtra("FromCallMode", true);
                    startActivity(it);
                }
            });
            // if (videoCall) {
            // ((ImageView) findViewById(R.id.minimize))
            // .setVisibility(View.GONE);
            // }

            mAnswer = (Button) findViewById(R.id.answer);
            if (mAnswer != null) {
                mAnswer.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        mPref.write("tempCheckSameIN", 0); // tml*** sametime

                        if (mHangup.getVisibility() == View.VISIBLE) { // tml***
                            // beta
                            // ui
                            updateButtonVisible(mHangup, View.GONE);
                        }

                        if (mSpeechRecognizer != null) { // tml*** voice control
                            destroyVoice();
                        }
                        // mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                        if (incomingChatroom)
                            mHandler.postDelayed(callToChatroom, 200);
                        else
                            mHandler.postDelayed(answerCall, 200);

                        mHandler.removeCallbacks(ringLimit);// alec
                        mHandler.removeCallbacks(delayENHangUp);
                        mHandler.postDelayed(delayENHangUp, 2000);

                        // tml*** cec
                        boolean autoHDMIauto = mPref.readBoolean(
                                "HDMIctrl_auto", true);
                        boolean autoHDMItv = mPref.readBoolean("HDMIctrl_tv",
                                true);
                        boolean autoHDMIinput = mPref.readBoolean(
                                "HDMIctrl_input", true);
                        if (!autoHDMIauto && (autoHDMItv || autoHDMIinput)) {
                            Thread hdmi = new Thread() {
                                @Override
                                public void run() {
                                    boolean autoHDMItv = mPref.readBoolean(
                                            "HDMIctrl_tv", true);
                                    boolean autoHDMIinput = mPref.readBoolean(
                                            "HDMIctrl_input", true);
                                    if (autoHDMItv)
                                        hdmiCmdExec("on");
                                    if (autoHDMIinput)
                                        hdmiCmdExec("switch");
                                }
                            };
                            hdmi.start();
                        }
                        // ***tml
                    }
                });
            }

            ((FrameLayout) findViewById(R.id.keypad_panel))
                    .setVisibility(View.INVISIBLE);
            ((ImageView) findViewById(R.id.keypad))
                    .setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            isKeypadShowed = !isKeypadShowed;
                            flipKeypad(isKeypadShowed);
                        }
                    });

            mHideKeypad = (Button) findViewById(R.id.hide_keypad);
            mHideKeypad.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    flipKeypad(false);
                    isKeypadShowed = false;
                }
            });

            mMute = (ImageView) findViewById(R.id.mute);

            mMute.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (AireVenus.isready()) {
                        VoipCore p = AireVenus.instance().getVoipCore();
                        if (mPref.readInt(Key.BCAST_CONF, -1) == 0) {// bree：不是主叫方并且是广播失去点击事件,默认mute
                            return;
                        } else {
                            isMuted = !isMuted;
                            if (isMuted) {
                                p.muteMic(true);
                            } else {
                                if (!isHeld)// alec
                                    p.muteMic(false);
                            }

                        }
                        mMute.setImageResource(isMuted ? R.drawable.mute_on
                                : R.drawable.mute_off);
                    }
                }
            });
            // tml*** speaker
            mSpeaker = (ImageView) findViewById(R.id.speaker);
            mSpeaker.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Log.e("vol0 " +
                    // mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
                    if (mAudioManager
                            .getStreamVolume(AudioManager.STREAM_VOICE_CALL) > 0) {
                        mAudioManager.setStreamVolume(
                                AudioManager.STREAM_VOICE_CALL, 0,
                                AudioManager.FLAG_SHOW_UI);
                        ((ImageView) v)
                                .setImageResource(R.drawable.speaker_off);
                    } else {
                        mAudioManager.setStreamVolume(
                                AudioManager.STREAM_VOICE_CALL, PreviousVolume,
                                AudioManager.FLAG_SHOW_UI);
                        ((ImageView) v).setImageResource(R.drawable.speaker_on);
                    }
                    // Log.e("vol1 " +
                    // mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
                    // String speakerstate = LedSpeakerUtil.getSpeakerStatus();
                    // if (speakerstate != null) {
                    // // if (speakerstate.equals("VIC:0")) {
                    // if (!speakerOn) {
                    // speakerOn = true;
                    // LedSpeakerUtil.setSpeakerOn();
                    // ((ImageView) v).setImageResource(R.drawable.speaker_on);
                    // } else {
                    // speakerOn = false;
                    // LedSpeakerUtil.setSpeakerOff();
                    // ((ImageView) v).setImageResource(R.drawable.speaker_off);
                    // }
                    // }
                }
            });

            // alec implemented the phone hold: mic and speaker are muted
            mHold = (ImageView) findViewById(R.id.hold);
            mHold.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (AireVenus.isready()) {
                        isHeld = !isHeld;
                        VoipCore p = AireVenus.instance().getVoipCore();
                        if (isHeld) {
                            p.muteMic(true);
                            p.setMuteSpeaker(1);
                        } else {
                            if (!isMuted) // alec
                                p.muteMic(false);
                            p.setMuteSpeaker(0);
                        }
                        mHold.setImageResource(isHeld ? R.drawable.hold_on
                                : R.drawable.hold_off);
                    }
                }
            });

            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (mWakeLock == null) {
                mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP, "FafaYou.InCall");
            }

            mStatus = (TextView) findViewById(R.id.status_label);
            mTimerLabel = (TextView) findViewById(R.id.conf_timer);

            Intent intent = getIntent();
            // phoneNumber=intent.getStringExtra("PhoneNumber");
            // if (phoneNumber!=null)
            // {
            // phoneNumber=MyTelephony.attachPrefix(DialerActivity.this,
            // phoneNumber);
            // mAddress=phoneNumber;
            // }
            String _phoneNumber = intent.getStringExtra("PhoneNumber");
            if (_phoneNumber != null) {
                phoneNumber = MyTelephony.attachPrefix(DialerActivity.this,
                        _phoneNumber);
                mAddress = phoneNumber;
            }
            Log.d("startDialer to-id " + _phoneNumber + ">" + phoneNumber);

            if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {
                boolean bIncome = mPref.readBoolean("incomingChatroom");
                if (bIncome) {
                    incomingChatroom = true;
                    mPref.delect("incomingChatroom");

                    // wjx*** ledspeaker
                    needblink = true;
                    Thread t1 = new Thread() {
                        @Override
                        public void run() {
                            Log.e("t1:::needblink===" + needblink);
                            while (needblink) {
                                MyUtil.Sleep(100);
                                LedSpeakerUtil.setLedOn();
                                MyUtil.Sleep(100);
                                LedSpeakerUtil.setLedOff();
                            }
                        }
                    };
                    t1.start();
                    LedSpeakerUtil.setSpeakerOn();
                    // ***wjx
                    // startRinging("CALLTYPE_CHATROOM bIncome");
                    // voice control, needs to tell apart from 1/0
                    prepareRing(true, 1, AireVenus.getCallType(),
                            "DA CALLTYPE_CHATROOM"); // tml*** new ring
                }

                // ListView memberView2 = (ListView) findViewById(R.id.members);
                // float mDensity = getResources().getDisplayMetrics().density;
                // RelativeLayout.LayoutParams lvparams =
                // (RelativeLayout.LayoutParams) memberView2.getLayoutParams();
                // lvparams.width = (int) (200 * mDensity);
                // memberView2.setLayoutParams(lvparams);

                mHandler.postDelayed(refreshChatRoomMember, 10000);
            }

            photoBitmap = getUserPhoto(mAddress);

            if (photoBitmap != null) {
                mProfileImage.setImageBitmap(photoBitmap);
            }
            if (bCommercial)
                mProfileImage.setBackgroundColor(0x00000000);

            incomingCall = getIntent().getBooleanExtra("incomingCall", false);

            if (incomingCall || incomingChatroom) {
                if (getIntent().getBooleanExtra("answerCall", false)) {
                    // mHangup.setVisibility(View.VISIBLE);
                    if (mAnswer != null)
                        mAnswer.setVisibility(View.GONE);
                    mHandler.postDelayed(autoAnswer, 500);
                } else {
                    // mHangup.setVisibility(View.GONE); //tml*** beta ui
                    mHangup.setVisibility(View.VISIBLE);
                    if (mAnswer != null) {
                        mAnswer.setVisibility(View.VISIBLE);
                        mAnswer.requestFocus(); // tml*** prefocus
                    }
                }
                displayStatus(null, getString(R.string.incoming_call));
            } else {
                mHangup.setVisibility(View.VISIBLE);
                mHangup.requestFocus(); // tml*** prefocus
                if (mAnswer != null)
                    mAnswer.setVisibility(View.GONE);
                displayStatus(null,
                        videoCall ? getString(R.string.making_video_call)
                                : getString(R.string.making_call));

                mADB.updateLastContactTimeByAddress(phoneNumber,
                        new Date().getTime());
                if (UserPage.sortMethod == 1)
                    UserPage.forceRefresh = true;
            }

            mDisplayName = intent.getStringExtra("DisplayName");

            if (AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL) {
                mProfileImage.setVisibility(View.GONE);// alec

                if (mDisplayName.length() < 1)
                    mDisplayName = phoneNumber;
                mDisplayNameView.setText(mDisplayName);

                float credit = mPref.readFloat("Credit", 0);
                showCredit(credit);

                // int selectedClass=mPref.readInt("SelectedClass",0);

                // final String [] classes={getString(R.string.standard_class),
                // getString(R.string.premium_class),
                // getString(R.string.business_class)};
                // ((TextView)findViewById(R.id.class_select)).setText(classes[selectedClass]);
                // ((TextView)findViewById(R.id.class_select)).setVisibility(View.VISIBLE);

                ((TextView) findViewById(R.id.credit))
                        .setVisibility(View.VISIBLE);

                ((TextView) findViewById(R.id.country))
                        .setVisibility(View.VISIBLE);
                String countryDesc = MyTelephony.getCountryNameByIndex(
                        AireCallPage.cIndex, DialerActivity.this)
                        + " "
                        + PhoneNumberUtils.formatNumber(mAddress);
                ((TextView) findViewById(R.id.country)).setText(countryDesc);
            } else if (mDisplayName != null) {
                if (contact_id > 0) {
                    if (mDisplayName.length() > 0)
                        mDisplayName = cq.getNameByContactId(contact_id) + " ("
                                + mDisplayName + ")";
                    else
                        mDisplayName = cq.getNameByContactId(contact_id);
                } else if (mDisplayName.length() == 0
                        && AireVenus.runAsSipAccount)
                    mDisplayName = "";

                mDisplayNameView.setText(mDisplayName);
            } else {
                if (phoneNumber != null) {
                    mDisplayName = mADB.getNicknameByAddress(phoneNumber);
                    if (mDisplayName != null)
                        mDisplayNameView.setText(mDisplayName);
                }
            }

            if (AireJupiter.getInstance() != null)
                AireJupiter.getInstance().StopEndingupServiceY();// alec

            // alec
            if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {
                previousCredit = mPref.readFloat("Credit", 0);
                ((ImageView) findViewById(R.id.add))
                        .setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                Intent it = new Intent(DialerActivity.this,
                                        SelectUserActivity.class);
                                it.putExtra("limit", 5);
                                startActivityForResult(it, 200);
                            }
                        });
                // tml|yangjun*** vidconf
                ((RelativeLayout) findViewById(R.id.panel_fake))
                        .setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                if (((LinearLayout) findViewById(R.id.panel))
                                        .getVisibility() == View.VISIBLE) {
                                    mHandler.removeCallbacks(showPanel);
                                    mHandler.removeCallbacks(hidePanel);
                                    mHandler.postDelayed(hidePanel, 300);
                                } else {
                                    mHandler.removeCallbacks(showPanel);
                                    mHandler.removeCallbacks(hidePanel);
                                    mHandler.postDelayed(showPanel, 300);
                                    mHandler.postDelayed(hidePanel, 6000);
                                }
                            }
                        });

                ((ToggleButton) findViewById(R.id.video))
                        .setOnCheckedChangeListener(new OnCheckedChangeListener() {


                            @Override
                            public void onCheckedChanged(
                                    CompoundButton btnView, boolean isChecked) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                                    openVideoView(isChecked);
                                } else {
                                    if (isChecked) {
                                        toastMaker(
                                                getResources().getString(
                                                        R.string.vidconf_sdk),
                                                0, Toast.LENGTH_SHORT, 0, 0, 0);
                                        btnView.setChecked(false);
                                    }
                                }
                            }
                        });
                // ***tml
            }

            if (AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL) {
                ((ImageView) findViewById(R.id.keypad))
                        .setVisibility(View.VISIBLE);
            } else if (AireVenus.getCallType() == AireVenus.CALLTYPE_WEBCALL) {
                ((ImageView) findViewById(R.id.keypad))
                        .setVisibility(View.VISIBLE); // sw|vivid*** webcall
            }

            created = true;

            new Thread(dialerStuff).start();

            InitDtmfKeyTone();

            // setVolumeControlStream(AudioManager.STREAM_RING);
            //
            // if (mAudioManager.isMusicActive())
            // mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            // setVolumeControlStream(AudioManager.STREAM_MUSIC); //tml*** mute
            // error, CX

            if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {

                if (isMultiMember) {

                    multiMemberAdapter = new MultiMemberAdapter(multiMemberList, multiNum);
                    ListView members = (ListView) findViewById(R.id.members);
                    if (members != null) {
                        members.setAdapter(multiMemberAdapter);
                    }
                } else {
                    memberAdapter = new MemberAdapter(memberList);
                    ListView members = (ListView) findViewById(R.id.members);
                    if (members != null) {
                        // members.setAdapter(imageAdapter);
                        members.setAdapter(memberAdapter);
                        members.setLongClickable(true);
                        members.setOnItemLongClickListener(onRemoveUserLongClick);
                        members.setOnItemClickListener(onMuteUserClick);
                    }
                }
                // tml*** oldconf bighead
                // PhotoGallery galley=(PhotoGallery)findViewById(R.id.members);
                // if (galley!=null)
                // {
                // galley.setAdapter(imageAdapter);
                // galley.setLongClickable(true);
                // galley.setOnItemLongClickListener(onRemoveUserLongClick);
                // }
            }
            // tml test
            if (mPref.read("moodcontent", "--").endsWith("!!!!") && Log.enDEBUG) {
                mHandler.post(displayVOIP);
            }
            if (mPref.readBoolean("CallTestBtns", false) && Log.enDEBUG) {
                mHandler.post(showTests);
                ((Button) findViewById(R.id.TEST1))
                        .setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String micVloc = "/sys/devices/i2c-4/4-001b/mic_volume";
                                String micV0 = MyUtil.getStatus(micVloc); // 1-127
                                Log.e("aloha micV was= " + micV0);
                                String micV1 = "1";
                                try {
                                    int micV1i = Integer.parseInt(micV0);
                                    micV1i = micV1i - (1 * testValue1);
                                    micV1 = Integer.toString(micV1i);
                                    if (micV1i < 1 || micV1i > 127) {
                                        if (micV1i < 1)
                                            micV1 = "1";
                                        else
                                            micV1 = "127";
                                    }

                                    boolean setok = MyUtil.setStatus2(micV1,
                                            micVloc);
                                    micV0 = MyUtil.getStatus(micVloc);
                                    Log.e("aloha micV now= " + micV0);
                                    if (!setok) {
                                        micV0 = "errIO";
                                    }
                                } catch (Exception e) {
                                    micV0 = "errN";
                                }
                                ((TextView) findViewById(R.id.TESTINFO))
                                        .setText(micV0);
                            }
                        });
                ((Button) findViewById(R.id.TEST2))
                        .setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String micVloc = "/sys/devices/i2c-4/4-001b/mic_volume";
                                String micV0 = MyUtil.getStatus(micVloc); // 1-127
                                Log.e("aloha micV was= " + micV0);
                                String micV1 = "1";
                                try {
                                    int micV1i = Integer.parseInt(micV0);
                                    micV1i = micV1i + (1 * testValue1);
                                    micV1 = Integer.toString(micV1i);
                                    if (micV1i < 1 || micV1i > 127) {
                                        if (micV1i < 1)
                                            micV1 = "1";
                                        else
                                            micV1 = "127";
                                    }

                                    boolean setok = MyUtil.setStatus2(micV1,
                                            micVloc);
                                    micV0 = MyUtil.getStatus(micVloc);
                                    Log.e("aloha micV now= " + micV0);
                                    if (!setok) {
                                        micV0 = "errIO";
                                    }
                                } catch (Exception e) {
                                    micV0 = "errN";
                                }
                                ((TextView) findViewById(R.id.TESTINFO))
                                        .setText(micV0);
                            }
                        });
                ((ToggleButton) findViewById(R.id.TEST3))
                        .setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                boolean testcheck = ((ToggleButton) v)
                                        .isChecked();
                                if (testcheck) {
                                    testValue1 = 10;
                                } else {
                                    testValue1 = 1;
                                }
                                ((TextView) findViewById(R.id.TESTINFO))
                                        .setText(Integer.toString(testValue1)
                                                + "x");
                            }
                        });
            }
            if (mPref.readBoolean(Key.SELFVIDIO, false)) {
                openVideoView(true);
            }
        }
    };

    Runnable delayENHangUp = new Runnable() { // tml*** beta ui
        public void run() {
            Log.d("delayENHangUp");
            mAnswer.setVisibility(View.GONE);
            mHangup.setVisibility(View.VISIBLE);
            mHangup.setEnabled(true);
            mHangup.requestFocus(); // tml*** prefocus
        }
    };

    // tml*** vidconf
    Runnable resumePanel = new Runnable() {
        public void run() {
            ((LinearLayout) findViewById(R.id.panel))
                    .setVisibility(View.VISIBLE);
            ((LinearLayout) findViewById(R.id.controls))
                    .setVisibility(View.VISIBLE);
            ((RelativeLayout) findViewById(R.id.panel_fake))
                    .setVisibility(View.INVISIBLE);
        }
    };

    Runnable showPanel = new Runnable() {
        public void run() {
            ((LinearLayout) findViewById(R.id.panel))
                    .setVisibility(View.VISIBLE);
            ((LinearLayout) findViewById(R.id.controls))
                    .setVisibility(View.VISIBLE);
            ((ListView) findViewById(R.id.members)).setVisibility(View.VISIBLE);
            ((RelativeLayout) findViewById(R.id.panel_fake)).getBackground()
                    .setAlpha(255);
            ((ImageView) findViewById(R.id.f1)).getBackground().setAlpha(255);
            ((ImageView) findViewById(R.id.f2)).getBackground().setAlpha(255);
            ((ImageView) findViewById(R.id.f3)).getBackground().setAlpha(255);
            ((ImageView) findViewById(R.id.f4)).getBackground().setAlpha(255);
        }
    };

    Runnable hidePanel = new Runnable() {
        public void run() {
            ((LinearLayout) findViewById(R.id.panel))
                    .setVisibility(View.INVISIBLE);
            ((LinearLayout) findViewById(R.id.controls))
                    .setVisibility(View.INVISIBLE);
            ((ListView) findViewById(R.id.members))
                    .setVisibility(View.INVISIBLE);
            ((RelativeLayout) findViewById(R.id.panel_fake))
                    .setVisibility(View.VISIBLE);
            ((RelativeLayout) findViewById(R.id.panel_fake)).getBackground()
                    .setAlpha(70);
            ((ImageView) findViewById(R.id.f1)).getBackground().setAlpha(130);
            ((ImageView) findViewById(R.id.f2)).getBackground().setAlpha(130);
            ((ImageView) findViewById(R.id.f3)).getBackground().setAlpha(130);
            ((ImageView) findViewById(R.id.f4)).getBackground().setAlpha(130);
        }
    };
    OnItemClickListener onMuteUserClick = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                                long arg3) {
            if (isHost) {// bree：如果是主叫方
                ChatroomMember member = memberList.get(position);
                String address = mADB.getAddressByIdx(member.getIdx());
                boolean isSpeak = member.isSpeak();
                member.setSpeak(!isSpeak);
                String id = member.getId();

                new Thread(new MuteTask(id, isSpeak ? "1" : "0")).start();

                memberAdapter.changeList(memberList);
                nullEndCall();

            }
        }
    };

    class MuteTask implements Runnable {

        private String id;
        private String mute;

        MuteTask(String id, String mute) {

            this.id = id;
            this.mute = mute;

        }

        @Override
        public void run() {
            String Return = "";
            try {
                MyNet net = new MyNet(DialerActivity.this);
                int room = mPref.readInt("ChatroomHostIdx");
                String roomNumber = String.format("%07d", room);
                String domain;
                if (incomingChatroom) {
                    domain = mPref.read("joinSipAddress",
                            AireJupiter.myConfSipServer_default);
                } else {
                    domain = mPref.read("conferenceSipServer",
                            AireJupiter.myConfSipServer_default);
                    if (AireJupiter.getInstance() != null) {
                        domain = AireJupiter.getInstance().getIsoConf(domain); // tml***
                        // china
                        // ip
                    }
                }

                // tml*** china ip
                String phpip = AireJupiter.myPhpServer_default;
                if (AireJupiter.getInstance() != null) {
                    phpip = AireJupiter.getInstance().getIsoPhp(0, true,
                            "74.3.165.66");
                }

                Return = net.doAnyPostHttp("http://" + phpip
                        + "/onair/conference/customer/conf_mute.php", "room="
                        + roomNumber + "&ip=" + domain + "&id=" + id + "&mute="
                        + mute);
            } catch (Exception e) {
                Log.e("da13 " + e.getMessage());
            }

            Log.d("mute.php Return=" + Return);
            if (Return.length() > 10) {
            }

        }

    }

    // ***tml
    OnItemLongClickListener onRemoveUserLongClick = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
            String displayname = memberList.get(arg2).getDisplayName();
            String uuid = memberList.get(arg2).getUuid();
            String Return = "";
            try {
                MyNet net = new MyNet(DialerActivity.this);
                int room = mPref.readInt("ChatroomHostIdx");
                if (Integer.parseInt(mPref.read("myID", "0"), 16) != room)
                    return true; // tml*** only hostkicker
                String roomNumber = String.format("%07d", room);
                String domain;
                if (incomingChatroom) {
                    domain = mPref.read("joinSipAddress",
                            AireJupiter.myConfSipServer_default);
                } else {
                    domain = mPref.read("conferenceSipServer",
                            AireJupiter.myConfSipServer_default);
                    if (AireJupiter.getInstance() != null) {
                        domain = AireJupiter.getInstance().getIsoConf(domain); // tml***
                        // china
                        // ip
                    }
                }

                // tml*** china ip
                String phpip = AireJupiter.myPhpServer_default;
                if (AireJupiter.getInstance() != null) {
                    phpip = AireJupiter.getInstance().getIsoPhp(0, true,
                            "74.3.165.66");
                }

                Return = net.doAnyPostHttp("http://" + phpip
                        + "/onair/conference/customer/hangup.php", "room="
                        + roomNumber + "&ip=" + domain + "&uuid=" + uuid);

                if (Return.toLowerCase().contains("ok")) {
                    memberList.remove(arg2);
                    memberAdapter.changeList(memberList);
                    nullEndCall();
                    String msg = getString(R.string.end_call) + " :"
                            + displayname;
                    Toast.makeText(DialerActivity.this, msg, Toast.LENGTH_LONG)
                            .show();
                } else {
                    // Toast.makeText(DialerActivity.this,
                    // "Failed to kick :"+displayname,
                    // Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e("da4 " + e.getMessage());
            }

            try {
                boolean found = false;
                for (int i = 0; i < memberList.size(); i++) {
                    int idx = memberList.get(i).getIdx();
                    if (idx == 0) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    shouldCheckPSTNinChatroom = false;
            } catch (Exception e) {
                Log.e("da5 " + e.getMessage());
            }

            return true;
        }

    };

    Runnable kickOutAllMembers = new Runnable() {
        public void run() {
            for (int i = 0; i < memberList.size(); i++) {
                String uuid = memberList.get(i).getUuid();
                try {
                    MyNet net = new MyNet(DialerActivity.this);
                    int room = mPref.readInt("ChatroomHostIdx");
                    String roomNumber = String.format("%07d", room);
                    String domain;
                    if (incomingChatroom) {
                        domain = mPref.read("joinSipAddress",
                                AireJupiter.myConfSipServer_default);
                    } else {
                        domain = mPref.read("conferenceSipServer",
                                AireJupiter.myConfSipServer_default);
                        if (AireJupiter.getInstance() != null) {
                            domain = AireJupiter.getInstance().getIsoConf(
                                    domain); // tml*** china ip
                        }
                    }

                    // tml*** china ip
                    String phpip = AireJupiter.myPhpServer_default;
                    if (AireJupiter.getInstance() != null) {
                        phpip = AireJupiter.getInstance().getIsoPhp(0, true,
                                "74.3.165.66");
                    }

                    if (i > 0)
                        MyUtil.Sleep(250);
                    net.doAnyPostHttp("http://" + phpip
                            + "/onair/conference/customer/hangup.php", "room="
                            + roomNumber + "&ip=" + domain + "&uuid=" + uuid);
                } catch (Exception e) {
                    Log.e("da6 " + e.getMessage());
                }
            }
        }
    };

    void flipKeypad(boolean showup) {
        if (showup) {
            FrameLayout main = (FrameLayout) findViewById(R.id.keypad_panel);
            AnimationSet as = new AnimationSet(false);
            as.setInterpolator(new AccelerateInterpolator());
            AlphaAnimation aa = new AlphaAnimation(0, 1.0f);
            ScaleAnimation sa = new ScaleAnimation(0, 1, 0.6f, 1,
                    main.getWidth() / 2, main.getHeight() / 2);
            sa.setDuration(300);
            as.addAnimation(sa);
            aa.setDuration(200);
            as.addAnimation(aa);
            as.setDuration(300);
            main.startAnimation(as);
            main.setVisibility(View.VISIBLE);
            mHideKeypad.setVisibility(View.VISIBLE);
            if (DTMFString.length() > 0)
                mDisplayNameView.setText(DTMFString);
        } else {
            FrameLayout main = (FrameLayout) findViewById(R.id.keypad_panel);
            AnimationSet as = new AnimationSet(false);
            as.setInterpolator(new AccelerateInterpolator());
            AlphaAnimation aa = new AlphaAnimation(1, 0.f);
            ScaleAnimation sa = new ScaleAnimation(1, 0, 1, 0.6f,
                    main.getWidth() / 2, main.getHeight() / 2);
            sa.setDuration(500);
            as.addAnimation(sa);
            aa.setDuration(200);
            aa.setStartOffset(200);
            as.addAnimation(aa);
            as.setDuration(500);
            main.startAnimation(as);
            main.setVisibility(View.INVISIBLE);
            mHideKeypad.setVisibility(View.GONE);
            mDisplayNameView.setText(mDisplayName);
            if (DTMFString.length() > 0)
                DTMFString += " ";
        }
    }

    private Bitmap getUserPhoto(String address) {
        if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {
            if (incomingChatroom) {
                int idx = getIntent().getIntExtra("ChatroomHostIdx", 0);

                String path = Global.SdcardPath_inbox + "photo_" + idx
                        + "b.jpg";
                Bitmap bmp = ImageUtil.loadBitmapSafe(1, path);
                if (bmp != null)
                    return bmp;

                path = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
                bmp = ImageUtil.loadBitmapSafe(1, path);
                if (bmp != null)
                    return bmp;
            }
            return ImageUtil.drawableToBitmap(getResources().getDrawable(
                    R.drawable.group_empty));
        }

        if (!MyUtil.checkSDCard(getApplicationContext()))
            return null;
        if (address.length() == 0)
            return null;

        if (bCommercial) {
            String path = Global.SdcardPath_inbox + address + ".png";
            return ImageUtil.loadBitmapSafe(1, path);
        } else {
            int idx = mADB.getIdxByAddress(address);
            if (idx > 0) {
                // String path=Global.SdcardPath_inbox+"photo_"+idx+"b.jpg";
                // return ImageUtil.loadBitmapSafe(1, path);
                // tml*** userphoto fix
                String path = Global.SdcardPath_inbox + "photo_" + idx
                        + "b.jpg";
                Bitmap bmp = ImageUtil.loadBitmapSafe(1, path);
                if (bmp != null)
                    return bmp;

                path = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
                bmp = ImageUtil.loadBitmapSafe(1, path);
                if (bmp != null)
                    return bmp;
            }
        }
        return null;
    }

    Runnable dialerStuff = new Runnable() {
        public void run() {
            Log.d("!!  wait for registered");

            int xcount = 0;
            while ((AireVenus.instance() == null || !AireVenus.isready() || !AireVenus
                    .instance().registered) && ++xcount < 100)
                // alec
                MyUtil.Sleep(100);

            if (AireVenus.instance() != null) {
                Log.d("!!  wait for registered...done " + AireVenus.isready()
                        + " " + AireVenus.instance().registered + " " + xcount);
            } else {
                Intent istop = new Intent(DialerActivity.this, AireVenus.class);
                stopService(istop); // tml*** airevenus fail
                Log.d("!!  wait for registered...ohuh !@#$AireVenus " + xcount);
            }

            try {
                if (AireVenus.isready()) {
                    VoipCore lVoipCore = AireVenus.instance().getVoipCore();
                    if (lVoipCore.isIncall() && incomingCall) {
                        Log.i("tml DA iiincoming! <<");
                        mPref.write("tempCheckSameIN", 0); // tml*** sametime
                        runOnUiThread(new Runnable() { // tml*** prefocus
                            @Override
                            public void run() {
                                ((Button) findViewById(R.id.answer))
                                        .requestFocus();
                            }
                        });
                        String IncomingNumber = lVoipCore.getRemoteAddress()
                                .getUserName();
                        if (IncomingNumber != null
                                && IncomingNumber.length() > 0)// alec
                        {
                            mAddress = IncomingNumber;// alec

                            videoCall = lVoipCore.getVideoEnabled();

                            int remote = 1;
                            if (AireVenus.runAsSipAccount)// alec
                                remote = 3;
                            else
                                remote = ContactsOnline
                                        .getContactOnlineStatus(mAddress);
                            if (remote == 0)
                                remote = 1;
                            Log.d("**** Net Type: REMOTE: " + remote + " ***");
                            lVoipCore.setNetType(new NetInfo(
                                    DialerActivity.this).netType, remote);

                            contact_id = cq
                                    .getContactIdByNumber(IncomingNumber);
                            mDisplayName = mADB.getNicknameByAddress(mAddress);
                            if (contact_id > 0) {
                                if (mDisplayName.length() > 0)
                                    mDisplayName = cq
                                            .getNameByContactId(contact_id)
                                            + " (" + mDisplayName + ")";
                            }
                            updateTextView(mDisplayNameView, mDisplayName);
                            mProfileImage = (ImageView) findViewById(R.id.bighead);
                            Bitmap photo = getUserPhoto(mAddress);

                            updateImageView(mProfileImage, photo);

                            if (lVoipCore.isInComingInvitePending()) {
                                callPending(lVoipCore.getCurrentCall());
                                mADB.updateLastContactTimeByAddress(
                                        IncomingNumber, new Date().getTime());
                                if (UserPage.sortMethod == 1)
                                    UserPage.forceRefresh = true;

                                incomingCall = true;

                                // updateButtonVisible(mHangup, View.GONE);
                                // //tml*** beta ui
                                displayStatus(
                                        null,
                                        videoCall ? getString(R.string.incoming_video_call)
                                                : getString(R.string.incoming_call));
                                // alec
                                boolean autoAnswerCall = false;
                                // boolean
                                // emergencyCallIn=mPref.readBoolean("emergencyCallIn",false);

                                // if ((mADB.isFafauser(mAddress) &&
                                // mPref.readBoolean("autoAnswer:"+mAddress,true)
                                // && mPref.readBoolean("way")==false))
                                // tml*** auto2
                                if ((mADB.isFafauser(mAddress)
                                        && (mPref.readBoolean("autoAnswer:"
                                        + mAddress, false) || mPref
                                        .readBoolean("autoAnswer2:"
                                                + mAddress, false)) && mPref
                                        .readBoolean("way") == false))
                                // ***tml
                                {
                                    List<String> instants = mPref
                                            .readArray("instants");
                                    if (instants != null) {
                                        for (String address : instants) {
                                            if (address.equals(mAddress)) {
                                                autoAnswerCall = true;
                                                // tml*** suv alarm
                                                Intent intent = new Intent(
                                                        Global.Action_Start_Surveillance);
                                                intent.putExtra("Command",
                                                        Global.CMD_SUVALARM_OFF);
                                                sendBroadcast(intent);
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    // tml*** voice control
                                    if (!(getIntent().getBooleanExtra(
                                            "answerCall", false))) {
                                        if (mPref.readBoolean("voice_control",
                                                false)) {
                                            // if
                                            // (mPref.readBoolean("voice_control",
                                            // false)
                                            // && !(AireVenus.getCallType() ==
                                            // AireVenus.CALLTYPE_CHATROOM)) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((LinearLayout) findViewById(R.id.voiceinfo))
                                                            .setVisibility(View.VISIBLE);
                                                    ((TextView) findViewById(R.id.voiceresults))
                                                            .setVisibility(View.VISIBLE);
                                                }
                                            });
                                        }
                                    }
                                }

                                // if (autoAnswerCall)
                                // mHandler.postDelayed(autoAnswer, 1500);
                                // else
                                // {
                                // if (mAnswer!=null)
                                // updateButtonVisible(mAnswer, View.VISIBLE);
                                // }
                                // tml*** auto2
                                if (autoAnswerCall) {
                                    mAnswer.setClickable(false);
                                    mHandler.postDelayed(autoAnswer, 1000);
                                } else {
                                    mAnswer.setClickable(true);
                                    if (mAnswer != null)
                                        updateButtonVisible(mAnswer,
                                                View.VISIBLE);
                                }
                                // ***tml

                            } else {
                                configureMuteAndSpeakerButtons();
                            }
                        } else {
                            Log.e("exit.Incoming call is null");
                            finish();
                        }

                    } else if (!incomingCall) {
                        Log.i("voip.DA oooutgoing! >> " + mAddress + " "
                                + phoneNumber + " inChat:" + incomingChatroom);
                        if (incomingChatroom) {
                            mPref.write("tempCheckSameIN", 0); // tml***
                            // sametime
                            mPref.write("lastCallSip", "O:" + "CONF");
                            runOnUiThread(new Runnable() { // tml*** prefocus
                                @Override
                                public void run() {
                                    ((Button) findViewById(R.id.answer))
                                            .requestFocus();
                                }
                            });
                            lVoipCore.setNetType(new NetInfo(
                                    DialerActivity.this).netType, 3);

                            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);// alec
                            // setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                            // 1.0f, 0);//alec
                            mAudioManager.setStreamVolume(
                                    AudioManager.STREAM_VOICE_CALL,
                                    PreviousVolume, 0);

                            // tml*** auto2
                            // mAddress and phoneNumber are not userID's
                            // mAddress =
                            // mADB.getNicknameByAddress(phoneNumber);
                            // CANNOT AUTO ANSWER = can stay in chatroom
                            // forever, blocking all further calls
                            // boolean autoAnswerCall = false;
                            // if (mPref.readBoolean("autoAnswer2:" + mAddress,
                            // false)) {
                            // List<String> instants =
                            // mPref.readArray("instants");
                            // if (instants != null) {
                            // for (String address: instants) {
                            // if (address.equals(mAddress)) {
                            // autoAnswerCall = true;
                            // break;
                            // }
                            // }
                            // }
                            // }
                            // if (autoAnswerCall) {
                            // mAnswer.setClickable(false);
                            // mHandler.postDelayed(autoAnswer, 1000);
                            // }
                            // ***tml
                            // tml*** voice control
                            if (!(getIntent().getBooleanExtra("answerCall",
                                    false))) {
                                if (mPref.readBoolean("voice_control", false)) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((LinearLayout) findViewById(R.id.voiceinfo))
                                                    .setVisibility(View.VISIBLE);
                                            ((TextView) findViewById(R.id.voiceresults))
                                                    .setVisibility(View.VISIBLE);
                                        }
                                    });
                                }
                            }
                        } else {
                            runOnUiThread(new Runnable() { // tml*** prefocus
                                @Override
                                public void run() {
                                    ((Button) findViewById(R.id.hangup))
                                            .requestFocus();
                                }
                            });
                            if (phoneNumber.equals(AireJupiter.myPhoneNumber)) {
                                Log.e("exit.own# " + phoneNumber + " "
                                        + AireJupiter.myPhoneNumber);
                                finish();
                                return;
                            }

                            if (mAnswer != null)
                                updateButtonVisible(mAnswer, View.GONE);
                            updateButtonVisible(mHangup, View.VISIBLE);

                            // alec
                            int remote = 0;
                            String addrTo = mAddress;

                            // tml*** sametime, DA already up
                            int idxIN = mPref.readInt("tempCheckSameIN");
                            int idxOUT = mADB.getIdxByAddress(phoneNumber);
                            String debugN = "--";
                            if (addrTo != null && addrTo.length() > 7) // tml
                                // test
                                debugN = addrTo.substring(0, 5) + "..";
                            mPref.write("lastCallSip", "O:" + debugN);
                            Log.e("Check postDA SAMETIME in/out> " + idxIN
                                    + "/" + idxOUT);
                            if (idxIN == idxOUT) {
                                Log.e("tml SAMETIME!!! (postDA)");
                                mPref.write("tempCheckSameIN", 0);
                                responseCallBusy();
                                return;
                            }
                            // ***tml

                            if (AireVenus.runAsSipAccount) {
                                remote = 3;
                                streamsRunning = false;
                            } else {
                                remote = ContactsOnline
                                        .getContactOnlineStatus(mAddress);

                                xcount = 0;
                                while ((AireJupiter.getInstance() == null || !AireJupiter
                                        .getInstance().calleeGotCallRequest)
                                        && ++xcount < 100) {
                                    MyUtil.Sleep(100);
                                }

                                addrTo = getYourSipServerByTCP(mAddress);
                            }
                            if (addrTo != null) { // tml test
                                if (addrTo.contains("@")) {
                                    String debugN2[] = addrTo.split("@");
                                    if (debugN2[0].length() > 7)
                                        debugN2[0] = debugN2[0].substring(0, 5)
                                                + "..";
                                    debugN = debugN2[0] + "@" + debugN2[1];
                                } else {
                                    if (addrTo.length() > 7)
                                        debugN = addrTo.substring(0, 5) + "..";
                                }
                            } else {
                                debugN = "null";
                            }
                            mPref.write("lastCallSip", "O:" + debugN);

                            lVoipCore.setNetType(new NetInfo(
                                    DialerActivity.this).netType, remote);
                            if (addrTo.contains("@nonmember")) {
                                Log.e("exit.nonMember " + addrTo);
                                finish();
                            } else if (!HangingUp) {

                                if (AireJupiter.getInstance() != null
                                        && !AireVenus.runAsSipAccount) {
                                    if (AireVenus.instance() != null) {
                                        // AireVenus.instance().startRingBack("!incomingCall !HangingUp !runAsSipAccount");
                                        AireVenus
                                                .instance()
                                                .prepareRing(
                                                        true,
                                                        0,
                                                        AireVenus.getCallType(),
                                                        "!incomingCall !HangingUp !runAsSip"); // tml***
                                        // new
                                        // ring
                                    }

                                    Log.d("wait for calleeGotCallRequest....");

                                    int c = 0;
                                    while (!AireJupiter.getInstance().calleeGotCallRequest
                                            && ++c < 350)
                                        // alec: 35 sec timeout
                                        MyUtil.Sleep(100);

                                    Log.d("wait for calleeGotCallRequest....Done");
                                }

                                if (AireVenus.instance() != null)
                                    AireVenus.instance()
                                            .renableCodec(videoCall);// alec

                                if (!HangingUp && !AireVenus.runAsSipAccount) {
                                    // Log.d("wait for 2000....");
                                    MyUtil.Sleep(1000);
                                    Log.d("wait for 2000....Done");
                                }
                                if (!HangingUp && !incomingChatroom)
                                    newOutgoingCall(addrTo, videoCall,
                                            "outChat");
                            }
                        }
                    }

                    mWakeLock.acquire();
                } else {
                    Log.e("exit.AireVenus NOT READY");
                    finish(); // tml*** airevenus fail
                }
            } catch (Exception e) {
                Log.e("exit.Fail to start dialer " + e.getMessage());
                finish();
            }
        }
    };

    // zhao
    private void updateTextView(TextView view, String text) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("textview", view);
        map.put("text", text);
        Message msg = new Message();
        msg.obj = map;
        msg.what = 1;
        handler.sendMessage(msg);
    }

    private void updateImageView(ImageView imageview, Bitmap bitmap) {
        if (bitmap == null)
            return;
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("imageview", imageview);
        map.put("image", bitmap);
        Message msg = new Message();
        msg.obj = map;
        msg.what = 2;
        handler.sendMessage(msg);
    }

    private void updateButtonVisible(Button button, int visible) {
        Message msg = new Message();
        msg.obj = button;
        msg.arg1 = visible;
        msg.what = 3;
        handler.sendMessage(msg);
    }

    private void updateLinearLayoutVisible(LinearLayout linear, int visible) {
        Message msg = new Message();
        msg.obj = linear;
        msg.arg1 = visible;
        msg.what = 4;
        handler.sendMessage(msg);
    }

    private void requestButtonFocus(Button button) { // tml*** prefocus
        Message msg = new Message();
        msg.obj = button;
        msg.what = 6;
        handler.sendMessage(msg);
    }

    BroadcastReceiver SliderResponse = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    Global.Action_Sip_Photo_Download_Complete)) {
                Bitmap photo = getUserPhoto(mAddress);
                if (photo != null) {
                    mProfileImage = (ImageView) findViewById(R.id.bighead);
                    if (mProfileImage != null)
                        mProfileImage.setImageBitmap(photo);
                }
            } else if (intent.getAction().equals(Global.Action_AnswerCall)) {
                Log.d("voip.AutoAnswer Action_AnswerCall");
                if (mAnswer != null)
                    updateButtonVisible(mAnswer, View.GONE);

                if (incomingChatroom) {
                    mHandler.removeCallbacks(ringLimit);
                    mHandler.postDelayed(callToChatroom, 200);
                } else
                    mHandler.postDelayed(answerCall, 200);
            } else if (intent.getAction().equals(Global.KILL_dialer)) // tml***
            // abort
            // dialer
            {
                Log.e("*** exit.Broadcast! FORCE KILL Dialer! restart to MAIN");
                Intent it = new Intent(DialerActivity.this, MainActivity.class);
                startActivity(it);
                finish();
            } else if (Global.Action_Video_Open.equals(intent.getAction())) {
                Log.i("收到广播，开始Video View。");
                if (isMultiMember && AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {
                    int groupIndex = intent.getIntExtra("Group_Index", -1);
                    if (groupIndex >= 0)
                        initVidConf(groupIndex);
                }
            } else if (Global.Action_Video_Close.equals(intent.getAction())) {
                Log.i("收到广播，关闭Video View。");
                if (isMultiMember && AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {
                    openVideoView(false);
                    curGroupIndex = -1;
                }
            }
        }
    };

    Runnable callToChatroom = new Runnable() {
        public void run() {
            // stopRinging();
            stopRing(); // tml*** new ring
            if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM
                    && incomingChatroom) {
                newOutgoingCall(mAddress, false, "inChat");
            }
        }
    };

    Runnable answerCall = new Runnable() {
        public void run() {
            if (AireVenus.instance() != null) {
                VoipCore lVoipCore = AireVenus.instance().getVoipCore();
                if (lVoipCore.isInComingInvitePending()) {
                    try {
                        VoipCall vc = lVoipCore.getCurrentCall();
                        lVoipCore.acceptCall(vc);

                    } catch (VoipCoreException e) {
                        Log.e("da7 " + e.getMessage());
                        lVoipCore = AireVenus.instance().getVoipCore();
                        VoipCall myCall = lVoipCore.getCurrentCall();
                        if (myCall != null) {
                            lVoipCore.terminateCall(myCall);
                            Log.d("exit.Failed to answer, so terminate call");
                        }
                    }
                    return;
                } else { // invite is not pending, due to poor network condition
                    lVoipCore = AireVenus.instance().getVoipCore();
                    VoipCall myCall = lVoipCore.getCurrentCall();
                    if (myCall != null) {
                        lVoipCore.terminateCall(myCall);
                        // tml*** beta ui
                        Toast tst = Toast.makeText(theDialer,
                                getString(R.string.call) + ": "
                                        + getString(R.string.no_video_hint),
                                Toast.LENGTH_LONG);
                        tst.setGravity(Gravity.CENTER, 0, 0);
                        LinearLayout tstLayout = (LinearLayout) tst.getView();
                        TextView tstTV = (TextView) tstLayout.getChildAt(0);
                        tstTV.setTextSize(26);
                        tst.show();
                        // ***tml
                        Log.d("exit.invite is not pending, due to poor network condition, so terminate call");
                    }
                }
            }
        }
    };

    @Override
    public void onBackPressed() {
        Log.e("voip.HANGUP2b DA *** USER PRESSED ***");
        mPref.write("tempCheckSameIN", 0); // tml*** sametime

        if (AireVenus.instance() != null) {
            VoipCore lVoipCore = AireVenus.instance().getVoipCore();
            if (lVoipCore != null) {
                VoipCall myCall = lVoipCore.getCurrentCall();
                if (myCall != null) {
                    lVoipCore.terminateCall(myCall);
                    Log.e("voip.exit.HANGUP2b DA *** USER PRESSED *** OK");
                    return;
                } else {
                    Log.e("voip.HANGUP2b DA *** USER PRESSED *** myCall=null");
                }
            } else {
                Log.e("voip.HANGUP2b DA *** USER PRESSED *** lVoipCore=null");
            }
        }
        exitCallMode("onBackPressed");
        super.onBackPressed();
    }


    private void updateCallLog() {
        Log.d("updateCallLog....");
        int dur = 0;
        int direction = incomingCall ? 1 : 2;
        int status = Connected ? 1 : 0;
        if (status != 0)
            dur = (int) ((new Date().getTime() - startTime) / 1000);

        if (AireVenus.getCallType() == AireVenus.CALLTYPE_FAFA) {
            Intent it = new Intent(Global.Action_InternalCMD);
            it.putExtra("Command", Global.CMD_UPDATE_CALL_LOG);
            it.putExtra("address", mAddress);
            it.putExtra("displayname", mDisplayName);
            it.putExtra("contact_id", contact_id);
            it.putExtra("time", new Date().getTime());
            it.putExtra("type", sysIncomingNumber == null ? 1 : 2);
            it.putExtra("duration", dur);
            it.putExtra("direction", direction);// 1: incoming, 2:outgoing
            it.putExtra("status", status);

            // for webcall log
            it.putExtra("runAsSip", false);
            it.putExtra("starttime", startTime);
            this.sendBroadcast(it);
        } else if (AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL) {
            if (AireCallPage.CallLogRowId != -1) {
                Log.i("CallLogRowId=" + AireCallPage.CallLogRowId + "  credit="
                        + consumedCredit + "  dur=" + dur);
                AireCallLogDB mCLDB = new AireCallLogDB(this);
                mCLDB.open();
                mCLDB.update(AireCallPage.CallLogRowId, consumedCredit, dur);
                mCLDB.close();
            }
        }
    }

    @Override
    protected void onDestroy() {
        // bree
        mPref.write(Key.SELFVIDIO, false);
        // LedSpeakerUtil.setSpeakerOff(); //demo box speaker
        mPref.delect(Key.BCAST_CONF); // tml*** broadcast
        mPref.write("disableVideo", false); // tml*** disable video
        if (mPref != null)
            mPref.write("tempCheckSameIN", 0); // tml*** sametime
        mPref.write("curCall", "");
        AireVenus.callstate_AV = null; // tml***
        if (SelectUserActivity.instance() != null) {
            SelectUserActivity.instance().closeSelectUser();
        }
        // stopRinging();
        stopRing(); // tml*** new ring
        LedSpeakerUtil.setSpeakerOff(); // tml*** speaker
        // if(rwb!=null){ //yang*** speex player
        // rwb.stop();
        // rwb.release();
        // }
        if (mSpeechRecognizer != null) { // tml*** voice control
            Log.e("destroying DA voiceRecogn");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((LinearLayout) findViewById(R.id.voiceinfo))
                            .setVisibility(View.GONE);
                    ((TextView) findViewById(R.id.voiceresults))
                            .setVisibility(View.GONE);
                }
            });
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
            voiceListener = null;
            voiceIntent = null;
        }

        if (postDialerWarn) { // tml*** tcp test
            if (AireJupiter.getInstance() != null) {
                Intent it = new Intent(Global.Action_InternalCMD);
                it.putExtra("Command", Global.CMD_CONNECTION_POOR);
                it.putExtra("ForcePoor", true);
                sendBroadcast(it);
            }
            postDialerWarn = false;
        }

        if (xWalkWebView != null) { // tml|yangjun*** vidconf
            ((LinearLayout) findViewById(R.id.topVWin_holder))
                    .removeView(xWalkWebView);
            xWalkWebView.onDestroy();
            xWalkWebView = null;
            Log.e("vidConfxwalk Destroyed");
        }

        mHandler.removeCallbacks(checkCPU); // tml*** setCPU2
        if (cpuSet[0] != null && cpuSet[2] != null && cpuSet[3] != null) { // tml|yang***
            // setCPU
            MyUtil.setCPU(true, cpuSet[0], cpuSet[2], cpuSet[3]);
            MyUtil.getCPU(true);
        }

        mHandler.removeCallbacks(startDialerStuff);
        mHandler.removeCallbacks(displayP2P);
        mHandler.removeCallbacks(displayVOIP); // tml test
        mHandler.removeCallbacks(checkCurrentCall); // tml*** check empty call
        mHandler.removeCallbacks(refreshChatRoomMember);
        mHandler.removeCallbacks(ringLimit);
        mHandler.removeCallbacks(addPSTNInfoHide);
        theDialer = null;
        minimized = false;

        if (created) {
            created = false;
            if (launchingVideo)
                exitCallMode(true, "onDestroy,created,launchingVideo");

            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);// alec
            NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNM.cancel(R.string.call);
            mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                    PreviousVolume, 0);
            PreviousVolume = mAudioManager
                    .getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            mPref.write("PreviousVolume", PreviousVolume);
            mAudioManager.setMode(AudioManager.MODE_NORMAL);
            mAudioManager.unloadSoundEffects();
            mAudioManager.setSpeakerphoneOn(false);
            // Log.i("tml destroyDA AUDIO MODE= " + mAudioManager.getMode());
            try {
                if (mWakeLock != null)
                    mWakeLock.release();
            } catch (Exception e) {
                Log.e("da8 " + e.getMessage());
            }

            if (BluetoothSco)
                Bluetooth.enable(false);

            mADB.close();
            Connected = false;
            launchingVideo = false;

            if (mPref.readBoolean("emergencyCallIn", false)) {
                mPref.write("emergencyCallIn", false);
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        String toAddress = mPref.read("SuvHostAddress", "");

                        Intent intent = new Intent(
                                Global.Action_Start_Surveillance);
                        intent.putExtra("address", toAddress);
                        sendBroadcast(intent);
                    }
                }, 1000);
            }

            if (tg != null)
                tg.release();
        }

        memberList.clear();
        updateCallDebugStatus(true, null);
        if (AireJupiter.getInstance() != null) {
            AireJupiter.getInstance().TESTseeVolumes("DAdestroy");
        }

        System.gc();
        // System.gc();

        Log.e("*** !!! DIALER *** DESTROY DESTROY !!! *** voip "
                + SettingPage.vlib);
        mPref.write("selfVidio", false);// 自动拨打
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("*** !!! DIALER *** RESUME RESUME !!! ***");
        // tml*** abort dialer, test
        if (AireVenus.instance() != null) {
            if (!AireVenus.instance().inCall) {
                Log.d("DA.RESUME AVgood >> inCall0 DA(" + created + ")");
            } else {
                Log.d("DA.RESUME AVgood >> inCall1 DA(" + created + ")");
            }
        } else {
            Log.d("DA.RESUME AVnull?! >> inCall? DA(" + created + ")");
        }
        // ***tml

        minimized = false;
        if (created) {
            if (incomingChatroom) {
                if (Connected) {
                    mAnswer.setVisibility(View.GONE);
                    mHangup.setVisibility(View.VISIBLE);
                    mHangup.requestFocus(); // tml*** prefocus
                } else {
                    // mHangup.setVisibility(View.GONE);
                    mAnswer.setVisibility(View.VISIBLE);
                    mAnswer.requestFocus(); // tml*** prefocus
                }
            } else if (Connected || !incomingCall) {
                if (mAnswer != null)
                    mAnswer.setVisibility(View.GONE);
                if (!isVideoCall) {
                    mHangup.setVisibility(View.VISIBLE);
                    mHangup.requestFocus(); // tml*** prefocus
                }
            } else if (incomingCall) {
                // mHangup.setVisibility(View.GONE); //tml*** beta ui
                if (mAnswer != null) {
                    mAnswer.setVisibility(View.VISIBLE);
                    mAnswer.requestFocus(); // tml*** prefocus
                }
            }
        }

        disableKeyguard();

        IntentFilter intentToReceiveFilter = new IntentFilter();
        intentToReceiveFilter.addAction(Global.Action_AnswerCall);
        intentToReceiveFilter
                .addAction(Global.Action_Sip_Photo_Download_Complete);
        intentToReceiveFilter.addAction(Global.KILL_dialer);
        intentToReceiveFilter.addAction(Global.Action_Video_Close);
        intentToReceiveFilter.addAction(Global.Action_Video_Open);
        this.registerReceiver(SliderResponse, intentToReceiveFilter);

        // MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        try { // tml*** unregistered rcvr destroy
            unregisterReceiver(SliderResponse);// alec
        } catch (IllegalArgumentException e) {
        }
        reenableKeyguard();

        // stopRinging();
        stopRing(); // tml*** new ring

        if (created) {
            if (mSpeechRecognizer != null) { // tml*** voice control
                Log.e("tmlv onPause");
                mSpeechRecognizer.cancel();
            }
            if (launchingVideo) {
                super.onPause();
                return;
            }

            if (isFinishing() && !minimized) {
                // restore audio settings

                // mAudioManager.setMode(AudioManager.MODE_NORMAL);
                mAudioManager.setSpeakerphoneOn(false);

                Log.d("broadcast CALL END...");
                Intent it = new Intent(Global.Action_InternalCMD);
                it.putExtra("Command", Global.CMD_CALL_END);
                if (mPref.readBoolean("usestanleysip")
                        && (AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL))
                    it.putExtra("immediately", 0);
                else
                    it.putExtra("immediately", AireVenus.runAsSipAccount ? 2000
                            : (mPref.readBoolean("usestanleysip") ? 2000
                            : 90000));
                it.putExtra(
                        "AireCall",
                        (AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL));

                if (AireJupiter.getInstance() != null)
                    AireJupiter.getInstance().attemptCall = false;

                this.sendBroadcast(it);

                if (sendTerminateSignal) {
                    if (mAddress.length() > 0
                            && AireJupiter.getInstance() != null
                            && AireVenus.runAsSipAccount == false) {
                        Log.e("exit.onPause sendTerminateSignal");
                        AireJupiter.getInstance().terminateCallBySocket(
                                mAddress);
                    }
                }
            } else {
                showNotification();
            }

            controlBkgndMusic(2);

            // MobclickAgent.onPause(this);
        }
        Log.e("*** !!! DIALER *** PAUSE PAUSE !!! ***");
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        disableKeyguard();
    }

    @Override
    public void onStop() {
        reenableKeyguard();
        super.onStop();
    }

    public void authInfoRequested(VoipCore p, String realm, String username) {

    }

    public void byeReceived(VoipCore p, String from) {

    }

    public void displayMessage(VoipCore p, String message) {
    }

    String mMsg = "";

    Runnable run_disp_msg = new Runnable() {
        public void run() {
            try {
                if (((TextView) findViewById(R.id.conf_timer)).getVisibility() == View.VISIBLE) {
                    if (theDialer != null)
                        theDialer.mTimerLabel.setText(mMsg);
                } else {
                    if (theDialer != null)
                        theDialer.mStatus.setText(mMsg);
                }
            } catch (NullPointerException e) {
                Log.e("da9 " + e.getMessage());
            }
        }
    };

    public void displayStatus(VoipCore p, String message) {
        if (theDialer != null) {
            mMsg = message;
            mHandler.post(run_disp_msg);
        }
    }

    private void showCredit(float credit) {
        TextView tv = (TextView) findViewById(R.id.credit);
        if (tv != null)
            tv.setText(String.format(getString(R.string.credit), credit));
    }

    private Handler mHandler = new Handler();
    private int lastMinute = -1;
    private Runnable timeElapsed = new Runnable() {
        @Override
        public void run() {
            if (Connected) {
                long sec = (new Date().getTime() - startTime) / 1000;
                displayStatus(null, DateUtils.formatElapsedTime(sec));

                mHandler.postDelayed(timeElapsed, 1000);

                if (AireVenus.runAsSipAccount && streamsRunning
                        && shouldConsumeCredit
                        && lastMinute != ((int) sec / 60)) {
                    lastMinute = ((int) sec / 60);
                    float credit = mPref.readFloat("Credit", 0);

                    if (credit > -0.02) {
                        float rate = 0.005f;

                        if (AireCallPage.isMobileNumber)
                            rate = DQRates.getMobileRateByIso(
                                    AireCallPage.cIso, 0);
                        else
                            rate = DQRates.getFixedRateByIso(AireCallPage.cIso,
                                    0);

                        if (rate == 0)
                            rate = 0.005f;

                        consumedCredit += rate;

                        credit -= rate;
                        mPref.writeFloat("Credit", credit);

                        showCredit(credit);
                    } else {

                        Log.e("AireCall Terminate due to credit:" + credit);
                        if (AireVenus.instance() != null) {
                            VoipCore lVoipCore = AireVenus.instance()
                                    .getVoipCore();
                            if (lVoipCore != null) {
                                VoipCall myCall = lVoipCore.getCurrentCall();
                                if (myCall != null) {
                                    lVoipCore.terminateCall(myCall);
                                    return;
                                }
                            }
                        }
                        exitCallMode("AireCall noCredit");
                    }
                }
            }
        }
    };

    public void displayWarning(VoipCore p, String message) {
    }

    public void globalState(VoipCore p, VoipCore.GlobalState state,
                            String message) {
        /*
		 * if (state == VoipCore.GlobalState.GlobalOn) { try{
		 * AireVenus.instance().initFromConf(); } catch (VoipConfigException ec)
		 * { Log.w("no valid settings found "+ec.getMessage()); } catch
		 * (Exception e ) { Log.e("Cannot get initial config "+e.getMessage());
		 * } if (getIntent().getData() != null) {
		 * newOutgoingCall(getIntent().getData
		 * ().toString().substring("tel://".length()), videoCall);
		 * getIntent().setData(null); } }
		 */
    }

    public void registrationState(final VoipCore p, final VoipProxyConfig cfg,
                                  final VoipCore.RegistrationState state, final String smessage) {
		/* nop */
    }

    ;

	/*
	 * SIMON 030111 callStates returned from Voipcore.c are VoipCallIdle,
	 * VoipCallIncomingReceived, //<This is a new incoming call
	 * VoipCallOutgoingInit, //<An outgoing call is started
	 * VoipCallOutgoingProgress, //<An outgoing call is in progress
	 * VoipCallOutgoingRinging, //<An outgoing call is ringing at remote end
	 * VoipCallOutgoingEarlyMedia, //<An outgoing call is proposed early media
	 * VoipCallConnected, //<Connected, the call is answered
	 * VoipCallStreamsRunning, //<The media streams are established and running
	 * VoipCallPausing, //<The call is pausing at the initiative of local end
	 * VoipCallPaused, //< The call is paused, remote end has accepted the pause
	 * VoipCallResuming, //<The call is being resumed by local end
	 * VoipCallRefered, //<The call is being transfered to another party,
	 * resulting in a new outgoing call to follow immediately VoipCallError,
	 * //<The call encountered an error VoipCallEnd, //<The call ended normally
	 * VoipCallPausedByRemote, //<The call is paused by remote end
	 * VoipCallUpdatedByRemote, //<The call's parameters are updated, used for
	 * example when video is asked by remote VoipCallIncomingEarlyMedia, //<We
	 * are proposing early media to an incoming call VoipCallUpdated //<The
	 * remote accepted the call update initiated by us
	 */

    Runnable autoAnswer = new Runnable() {
        public void run() {
            Intent it = new Intent(Global.Action_AnswerCall);
            sendBroadcast(it);
        }
    };

    public static String callstate_DA = null; // tml***/

    public void callState(final VoipCore p, final VoipCall call,
                          final State state, final String message) {
        callstate_DA = state.toString(); // tml***/
        if (state == VoipCall.State.OutgoingInit) {
            if (incomingChatroom)
                displayStatus(null, getString(R.string.incoming_call));
            else
                displayStatus(null,
                        videoCall ? getString(R.string.making_video_call)
                                : getString(R.string.making_call));
            enterCallMode(p);
            // alec:routeAudioToReceiver();
            // SIMON 030211
        } else if (state == VoipCall.State.OutgoingRinging) {
            // SIMON resetCameraFromPreferences();
            displayStatus(null,
                    videoCall ? getString(R.string.making_video_call)
                            : getString(R.string.making_call));
        } else if (state == VoipCall.State.IncomingReceived) {
            displayStatus(null,
                    videoCall ? getString(R.string.incoming_video_call)
                            : getString(R.string.incoming_call));
            callPending(call);
            if (mAnswer != null) {
                mAnswer.setVisibility(View.VISIBLE);
                mAnswer.requestFocus(); // tml*** prefocus
            }

            // updateButtonVisible(mHangup, View.GONE);
        } else if (state == VoipCall.State.Connected) {
            needblink = false; // wjx*** ledspeaker
            // if (!speakerOn) LedSpeakerUtil.setSpeakerOff();
            // LedSpeakerUtil.setSpeakerOn(); //demo box speaker
            // ***wjx
            displayStatus(null, getString(R.string.call_connected));
            enterCallMode(p);

            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);// alec
            // setStreamVolume(AudioManager.STREAM_VOICE_CALL, 1.0f, 0);//alec
            mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                    PreviousVolume, 0);

            startTime = new Date().getTime();// alec
            Connected = true;
            mHandler.postDelayed(timeElapsed, 1000);
            if (AireJupiter.getInstance() != null)
                AireJupiter.getInstance().attemptCall = false;// alec

            // AireJupiter.getInstance().unreadBlinkOff(); //tml*** unread led
            // LedSpeakerUtil.setLedOn(); //wjx*** ledspeaker
        } else if (state == VoipCall.State.Error) {
            if (message.startsWith("Not found")) {
                if (!Connected) {
                    displayStatus(null, getString(R.string.callee_not_found));
                    // playSound();
                }
            } else if (message.startsWith("No response")
                    || message.startsWith("Request Timeout")) {
                if (!Connected) {
                    displayStatus(null, getString(R.string.try_again));
                    // playSound();
                }
            } else
                displayStatus(null, getString(R.string.call_end));

            if (!Connected) {
                if (AireVenus.instance() != null) {
                    VoipCall myCall = p.getCurrentCall();
                    if (myCall != null) {
                        p.terminateCall(myCall);
                        Log.d("exit.call state Error, terminate Call");
                    }
                }
                exitCallMode("callState.Error");
            }
        } else if (state == VoipCall.State.CallEnd) {
            // wjx*** ledspeaker
            needblink = false;
            LedSpeakerUtil.setSpeakerOff();
            LedSpeakerUtil.setLedOff();
            // ***wjx
            if (message.startsWith("Call declined")) {
                displayStatus(null, getString(R.string.call_declined));
                // playSound();
            } else
                displayStatus(null, getString(R.string.call_end));

            exitCallMode("callState.CallEnd");
        } else if (state == VoipCall.State.StreamsRunning) {
            if (AireJupiter.getInstance() != null) {
                AireJupiter.getInstance().TESTseeVolumes("Streaming");
            }
            if (!streamsRunning) {
                // alec
                if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            ((ImageView) findViewById(R.id.add))
                                    .setVisibility(View.VISIBLE);
                            if (!isMobileNumber)
                                ((ToggleButton) findViewById(R.id.video))
                                        .setVisibility(View.VISIBLE); // tml***
                            // vidconf
                        }
                    }, 1000);
                }

                streamsRunning = true;

                if (call.getCurrentParamsCopy().getVideoEnabled()
                        && Version.isVideoCapable()) {
                    isVideoCall = true;
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (!VideoCallActivity.launched) {
                                mHandler.postDelayed(run_spearkout, 2000);
                                startVideoView(VIDEO_VIEW_ACTIVITY);
                            }
                        }
                    }, 500);

                    // mHandler.post(new Runnable(){
                    // public void run()
                    // {
                    // mHangup.setVisibility(View.INVISIBLE);
                    // }
                    // });
                } else {
                    if (mPref.read("moodcontent", "--").endsWith("!!!!")) {
                        mHandler.postDelayed(displayP2P, 1000);
                    }
                }
                mHandler.postDelayed(checkCurrentCall, 2000); // tml*** check
                // empty call

                if (isMultiMember && (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM)) {
                    mHandler.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            openVideoView(true);
//							//是分组的话自动打开video
//							if(curGroupIndex >= 0 && !isHost){
//								openVideoView();
//							}
                        }
                    }, 500);
                }
            }

            // if(mPref.readInt(Key.BCAST_CONF, -1) == 0){
            // setMute(true);
            // }

        }
    }

    Runnable run_spearkout = new Runnable() {
        public void run() {
            routeAudioToSpeaker();
        }
    };

    private void startVideoView(int requestCode) {
        Intent lIntent = new Intent(DialerActivity.this,
                VideoCallActivity.class);
        lIntent.putExtra("address", mAddress);
        startActivityForResult(lIntent, requestCode);
        launchingVideo = true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_VIEW_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                exitCallMode("onActivityResult,VIDEO_VIEW_ACTIVITY,OK");
                routeAudioToReceiver();

                if (mAddress.length() > 0 && AireJupiter.getInstance() != null) {
                    AireJupiter.getInstance().attemptCall = false;
                    AireJupiter.getInstance().terminateCallBySocket(mAddress);
                }
            }
        } else if (requestCode == 200) {
            if (resultCode == RESULT_OK && data != null) {
                int type = data.getIntExtra("type", -1);
                if (type == 1) {
                    String number = data.getStringExtra("result");
                    addingList2.clear();
                    String global = MyTelephony.attachPrefix(
                            DialerActivity.this, number);
                    Log.d("SelPh# ConfAdd Call2 " + global);
                    addingList2.add(global);

                    if (addingList2.size() > 0)
                        new Thread(addPSTNtoJoinChatroom).start();

                } else if (type == 0) {
                    ArrayList<String> idxList = data.getExtras()
                            .getStringArrayList("idxList");
                    if (idxList != null && idxList.size() > 0) {
                        addingList = idxList;
                        if (addingList.size() > 0)
                            new Thread(sendNotifyForJoinChatroom).start();
                    }
                }
            }
        }
    }

    public static ArrayList<String> addingList = new ArrayList<String>();
    public static ArrayList<String> addingList2 = new ArrayList<String>();

    private Runnable sendNotifyForJoinChatroom = new Runnable() {
        public void run() {
            String myIdxHex = mPref.read("myID", "0");

            String ServerIP;
            if (incomingChatroom) {
                ServerIP = mPref.read("joinSipAddress",
                        AireJupiter.myConfSipServer_default);
                int room = mPref.readInt("ChatroomHostIdx");
                myIdxHex = String.format("%x", room);
            } else {
                ServerIP = mPref.read("conferenceSipServer",
                        AireJupiter.myConfSipServer_default);
                if (AireJupiter.getInstance() != null) {
                    ServerIP = AireJupiter.getInstance().getIsoConf(ServerIP); // tml***
                    // china
                    // ip
                }
            }

            long ip = MyUtil.ipToLong(ServerIP);
            String HexIP = Long.toHexString(ip);

            String content = Global.Call_Conference + "\n\n" + HexIP + "\n\n"
                    + myIdxHex;
            // tml*** broadcast
            if (mPref.readInt(Key.BCAST_CONF, -1) >= 0) {
                content = Global.Call_Conference + Global.Call_Broadcast
                        + "\n\n" + HexIP + "\n\n" + myIdxHex;
            }

            long now = new Date().getTime();

            for (int i = 0; i < addingList.size(); i++) {
                int idx = Integer.parseInt(addingList.get(i));
                if (idx < 50)
                    continue;

                String address = mADB.getAddressByIdx(idx);

                mADB.updateLastContactTimeByAddress(address, now);

                if (AireJupiter.getInstance() != null
                        && AireJupiter.getInstance().tcpSocket() != null
                        && address.length() > 0) {
                    MyUtil.Sleep(500);
                    if (AireJupiter.getInstance().isLogged()) {
                        updateCallDebugStatus(false, "\n>Conf " + address);
                        Log.d("voip.inviteConf1 " + address + " " + content);
                        AireJupiter.getInstance().tcpSocket()
                                .send(address, content, 0, null, null, 0, null);
                    } else {
                        Log.e("AireJupiter.getInstance().isLogged() = FALSE");
                    }
                } else {
                    Log.e("AireJupiter.getInstance().tcpSocket() = NULL");
                }
            }
        }
    };

    private Runnable addPSTNtoJoinChatroom = new Runnable() {
        public void run() {
            String myIdxHex = mPref.read("myID", "0");
            int myIdx = Integer.parseInt(myIdxHex, 16);

            String ServerIP;
            if (incomingChatroom) {
                ServerIP = mPref.read("joinSipAddress",
                        AireJupiter.myConfSipServer_default);
            } else {
                ServerIP = mPref.read("conferenceSipServer",
                        AireJupiter.myConfSipServer_default);
                if (AireJupiter.getInstance() != null) {
                    ServerIP = AireJupiter.getInstance().getIsoConf(ServerIP); // tml***
                    // china
                    // ip
                }
            }

            String room = String.format("%07d", myIdx);
            MCrypt mc = new MCrypt();

            String pass = "aireping*$857";
            try {
                pass = MCrypt.bytesToHex(mc.encrypt(pass));
            } catch (Exception e) {
                Log.e("da10 " + e.getMessage());
            }
            String myUsername = String.format("**%d", myIdx);
            String myPasswd = mPref.read("password", "1111");
            for (int i = 0; i < addingList2.size(); i++) {
                String address = addingList2.get(i);
                String number = MyTelephony.cleanPhoneNumber2(address);
                String globalnumber = address;
                Log.d("SelPh# ConfAdd! = " + globalnumber + " " + number);

                MyTelephony.init(DialerActivity.this);
                if (MyTelephony.validWithCurrentISO(number)) {
                    globalnumber = MyTelephony.attachPrefix(
                            DialerActivity.this, number);
                    Log.d("SelPh# globalnumber1");
                } else {
                    isMobileNumber = false;
                    globalnumber = MyTelephony.attachFixedPrefix(
                            DialerActivity.this, number);
                    Log.d("SelPh# globalnumber2");
                }

                if (!globalnumber.startsWith("+")) {
                    globalnumber = MyTelephony.attachPrefix(
                            DialerActivity.this, number);
                    if (globalnumber.startsWith("+"))
                        isMobileNumber = true;
                    Log.d("SelPh# globalnumber3");
                }
                if (!globalnumber.startsWith("+")) {
                    globalnumber = MyTelephony.attachFixedPrefix(
                            DialerActivity.this, number);
                    if (globalnumber.startsWith("+"))
                        isMobileNumber = false;
                    Log.d("SelPh# globalnumber4");
                }

                cIndex = MyTelephony.getCountryIndexByNumber(globalnumber);
                cIso = MyTelephony.getCountryIsoByIndex(cIndex);

                Log.d("SelPh# ConfAdd!!= " + globalnumber + " " + number + " "
                        + cIndex + " " + cIso + " " + isMobileNumber);

                try {
                    address = MCrypt.bytesToHex(mc.encrypt(globalnumber));
                } catch (Exception e) {
                    Log.e("da11 " + e.getMessage());
                }
                String Return = "";
                if (i > 0)
                    MyUtil.Sleep(250);
                try {
                    updateCallDebugStatus(false, "\n>Conf " + address);
                    Log.i("voip.inviteConf2 " + "room=" + room + "&ip="
                            + ServerIP + "&callee=" + address + "&pass=" + pass
                            + "&user=" + myUsername + "&userpw=" + myPasswd);
                    MyNet net = new MyNet(DialerActivity.this);

                    // tml*** china ip
                    String phpip = AireJupiter.myPhpServer_default;
                    if (AireJupiter.getInstance() != null) {
                        phpip = AireJupiter.getInstance().getIsoPhp(0, true,
                                "74.3.165.66");
                    }

                    Return = net.doAnyPostHttp("http://" + phpip
                                    + "/onair/conference/customer/addcallandroid.php",
                            "room=" + room + "&ip=" + ServerIP + "&callee="
                                    + address + "&pass=" + pass + "&user="
                                    + myUsername + "&userpw=" + myPasswd);
                } catch (Exception e) {
                    Log.e("da12 " + e.getMessage());
                }
                // Log.d("addPSTN Return="+Return);

                shouldCheckPSTNinChatroom = true;
            }

            // tml*** beta ui
            mHandler.post(addPSTNInfoShow);
        }
    };

    // tml*** beta ui
    private Runnable addPSTNInfoShow = new Runnable() {
        public void run() {
            float credit = mPref.readFloat("Credit", 0);
            showCredit(credit);
            ((TextView) findViewById(R.id.credit)).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.country)).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.country)).setText(MyTelephony
                    .getCountryNameByIndex(cIndex, DialerActivity.this));
            mHandler.postDelayed(addPSTNInfoHide, 4000);
        }
    };
    private Runnable addPSTNInfoHide = new Runnable() {
        public void run() {
            if (((TextView) findViewById(R.id.credit)) != null) {
                ((TextView) findViewById(R.id.credit)).setVisibility(View.GONE);
            }
            if (((TextView) findViewById(R.id.country)) != null) {
                ((TextView) findViewById(R.id.country))
                        .setVisibility(View.GONE);
            }
        }
    };

    private Runnable readChatroomMemberThread = new Runnable() {
        public void run() {
            String Return = "";
            try {
                MyNet net = new MyNet(DialerActivity.this);
                int room = mPref.readInt("ChatroomHostIdx");
                String roomNumber = String.format("%07d", room);
                String domain;
                if (incomingChatroom) {
                    domain = mPref.read("joinSipAddress",
                            AireJupiter.myConfSipServer_default);
                } else {
                    domain = mPref.read("conferenceSipServer",
                            AireJupiter.myConfSipServer_default);
                    if (AireJupiter.getInstance() != null) {
                        domain = AireJupiter.getInstance().getIsoConf(domain); // tml***
                        // china
                        // ip
                    }
                }

                // tml*** china ip
                String phpip = AireJupiter.myPhpServer_default;
                if (AireJupiter.getInstance() != null) {
                    phpip = AireJupiter.getInstance().getIsoPhp(0, true,
                            "74.3.165.66");
                }

                Return = net.doAnyPostHttp("http://" + phpip
                        + "/onair/conference/customer/conf1.php", "room="
                        + roomNumber + "&ip=" + domain);
            } catch (Exception e) {
                Log.e("da13 " + e.getMessage());
            }

            Log.d("conf1.php Return=" + Return);
            if (Return.length() > 10) {
                memberList.clear();
                parseChatroomMember(Return);
                mHandler.post(showChatroomMembers);
            }
        }

        private void parseChatroomMember(String Return) {
			/*
			 * <html
			 * xmlns="http://www.w3.org/1999/xhtml"><head></head><body><pre>
			 * 1264;sofia/internal/**1217793@115.29.234.27;9d
			 * 0ea68e-f0da-4042-97ac-e01c6f54af03;
			 * **1217793;**1217793;hear|speak;
			 * 0;0;0;3001263;sofia/internal/**1238870
			 * 
			 * @115.29.234.27;97774453-4025
			 * -4ae2-993b-4c8866671d26;**1238870;**1238870
			 * ;hear|speak|talking|floor;0;0;0;300 <br/></body></html>
			 */
            try {
                // 去掉html标签
                String str = Return.split("<pre>")[1].split("<br/>")[0];

                // 获取每个成员
                String[] members = str.split(";300");
                int myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);

                // 解析每个成员
                for (String member : members) {
                    String[] items = member.split(";");
                    try {
                        String id = items[0];
                        int idx = 0;
                        Boolean isSpeak = member.contains("speak");
                        String idStr = items[1];
                        if (idStr.contains("internal")) {
                            idStr = idStr.substring(17, idStr.indexOf('@'));
                            idx = Integer.parseInt(idStr);
                            if (myIdx == idx)
                                continue;
                        } else {
                            idStr = idStr.substring(15, idStr.indexOf('@'));
                            if (idStr.startsWith("00177") && idStr.length() > 5)
                                idStr = idStr.substring(5);
                            else if (idStr.startsWith("00111")
                                    && idStr.length() > 7)
                                idStr = idStr.substring(7);
                        }

                        String uuid = items[2];
                        Drawable drawable;
                        String displayname;
                        if (idx > 0) {
                            displayname = mADB.getNicknameByIdx(idx);
                            String address = mADB.getAddressByIdx(idx);
                            if (address.length() == 0)
                                displayname = getResources().getString(
                                        R.string.unknown_person);

                            // tml*** oldconf bighead
                            drawable = ImageUtil.getUserPhoto(
                                    DialerActivity.this, idx);
                            if (drawable == null)
                                drawable = getResources().getDrawable(
                                        R.drawable.bighead);
                            // ***tml

                        } else {

                            // tml*** oldconf bighead
                            long contactId = cq.getContactIdByNumber("+"
                                    + idStr);
                            drawable = cq.getPhotoById(DialerActivity.this,
                                    contactId, false);
                            if (drawable == null)
                                drawable = getResources().getDrawable(
                                        R.drawable.bighead);
                            // ***tml
                            displayname = "+" + idStr;
                        }
                        ChatroomMember cm = new ChatroomMember();
                        cm.setIdx(idx);
                        cm.setId(id);
                        cm.setSpeak(isSpeak);
                        cm.setAddress("+" + idStr);
                        cm.setDisplayName(displayname);
                        // map.put("photo", drawable); //tml***
                        // oldconf bighead
                        cm.setPhoto(drawable);
                        cm.setUuid(uuid);
                        Log.d("chat member : " + cm);
                        memberList.add(cm);

                    } catch (Exception e) {
                        Log.e("da14 " + e.getMessage());
                    }
                }
                Collections.sort(memberList, comparator);
                multiMemberList = toMultiList(memberList, multiNum);

            } catch (Exception e) {
                Log.e("da15 " + e.getMessage());
            }
        }
    };
    //li*** 成员根据名字排序比较器。
    Comparator<ChatroomMember> comparator = new Comparator<ChatroomMember>() {

        @Override
        public int compare(ChatroomMember lhs, ChatroomMember rhs) {
            return lhs.getDisplayName().compareTo(rhs.getDisplayName());
        }
    };

    private Runnable showChatroomMembers = new Runnable() {
        public void run() {
            // ((ListView)findViewById(R.id.members)).setVisibility(memberList.size()>0?View.VISIBLE:View.GONE);
            // imageAdapter.notifyDataSetChanged();
            ((ListView) findViewById(R.id.members)).setVisibility(memberList
                    .size() > 0 ? View.VISIBLE : View.INVISIBLE);
            if (isMultiMember) {
                multiMemberAdapter.changeList(multiMemberList);
            } else {
                memberAdapter.changeList(memberList);
            }
            nullEndCall();
            // tml*** oldconf bighead
            // ((FrameLayout)findViewById(R.id.members_view)).setVisibility(View.VISIBLE);
            // imageAdapter.notifyDataSetChanged();
            // ((PhotoGallery)findViewById(R.id.members)).setSelection(memberList.size()-1);
            // ***tml
        }

    };

    private int emptyConf = 0;
    private Runnable refreshChatRoomMember = new Runnable() {
        public void run() {
            new Thread(readChatroomMemberThread).start();
            int emptyConfLimit = 20;
            if (memberList.size() == 0) {
                emptyConf++;
                Log.e("voip.myConf is empty " + emptyConf + "/"
                        + emptyConfLimit);
            } else {
                emptyConf = 0;
            }

            mHandler.postDelayed(refreshChatRoomMember, 15000);
            // if (emptyConf == emptyConfLimit) {
            // VoipCore lVoipCore = AireVenus.instance().getVoipCore();
            // VoipCall myCall = lVoipCore.getCurrentCall();
            // if (AireVenus.instance() != null) {
            // AireVenus.instance().displayCallStatus("myConf was empty!!!");
            // }
            // if (myCall != null) {
            // lVoipCore.terminateCall(myCall);
            // Log.e("voip.exit.myConf1 was empty!!!");
            // } else {
            // if (VideoCallActivity.getInstance() != null) {
            // VideoCallActivity.getInstance().bye();
            // }
            // finish();
            // Log.e("voip.exit.myConf2 was empty!!!");
            // }
            // } else {
            // mHandler.postDelayed(refreshChatRoomMember, 15000);
            // }

        }
    };

    void playSound() {
        if (!MyUtil.CheckServiceExists(getApplicationContext(),
                "com.pingshow.airecenter.PlayService")) {
            Intent intent1 = new Intent(DialerActivity.this, PlayService.class);
            intent1.putExtra("soundInCall", R.raw.termin);
            startService(intent1);
        }
    }

    public void show(VoipCore p) {
    }

    private void enterCallMode(VoipCore p) {
        Log.e("enterCallMode! enterCallMode!");
        if (mAnswer != null)
            mAnswer.setEnabled(false);
        // mAnswer.setVisibility(View.GONE);
        // mHangup.setVisibility(View.VISIBLE);
        mHandler.removeCallbacks(delayENHangUp);
        mHandler.postDelayed(delayENHangUp, 2000);

        // tml*** secure ring
        String mAddress = p.getRemoteAddress().getUserName();
        boolean secureRing = false;
        List<String> instants = mPref.readArray("instants");
        if (instants != null) {
            for (String address : instants) {
                if (address.equals(mAddress)
                        && (mPref.readBoolean("autoAnswer:" + mAddress, false))) {
                    mChatView.setVisibility(View.GONE);
                    secureRing = true;
                    break;
                }
            }
        }
        AireJupiter.getInstance().unreadBlinkOff(); // tml*** unread led
        if (!secureRing) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.e("enterCallMode setLedOn");
                    LedSpeakerUtil.setLedOn(); // wjx*** ledspeaker/
                }
            }, 1000);
        }
		/*
		 * VoipAddress remote=p.getRemoteAddress(); if (remote!=null){ //TODO }
		 */

        configureMuteAndSpeakerButtons();

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        // setStreamVolume(AudioManager.STREAM_VOICE_CALL, 1.0f, 0); //alec
        mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                PreviousVolume, 0);
    }

    // alec
    public void responseCallBusy() {
        HangingUp = true;

        displayStatus(null, getString(R.string.call_declined));

        if (!Connected) {
            if (AireVenus.instance() != null) {
                VoipCore lVoipCore = AireVenus.instance().getVoipCore();
                VoipCall myCall = lVoipCore.getCurrentCall();
                if (myCall != null) {
                    lVoipCore.terminateCall(myCall);
                    Log.d("exit.responseCallBusy, terminate Call");
                }
            }
        }

        if (!MyUtil.CheckServiceExists(getApplicationContext(),
                "com.pingshow.airecenter.PlayService")) {
            Intent intent1 = new Intent(DialerActivity.this, PlayService.class);
            intent1.putExtra("soundInCall", R.raw.termin);
            startService(intent1);
        }
        exitCallMode("responseCallBusy");
    }

    Runnable run_mute = new Runnable() {
        public void run() {
            try {
                AireVenus y;
                if ((y = AireVenus.instance()) != null) {
                    isMuted = y.getVoipCore().isMicMuted();
                    mMute.setImageResource(isMuted ? R.drawable.mute_on
                            : R.drawable.mute_off);
                    isHeld = y.getVoipCore().isSpeakerMuted() == 1 && isMuted;
                    mHold.setImageResource(isHeld ? R.drawable.hold_on
                            : R.drawable.hold_off);
                    // tml*** speaker
                    if (mAudioManager
                            .getStreamVolume(AudioManager.STREAM_VOICE_CALL) == 0) {
                        mSpeaker.setImageResource(R.drawable.speaker_off);
                    }
                    // String hdmiconnected = HdmiUtil.getHdmiState();
                    // if (hdmiconnected != null) {
                    // if (hdmiconnected.equals("0")) {
                    // LedSpeakerUtil.setSpeakerOn();
                    // mSpeaker.setImageResource(R.drawable.speaker_on);
                    // } else {
                    // if (!speakerOn) {
                    // LedSpeakerUtil.setSpeakerOff();
                    // mSpeaker.setImageResource(R.drawable.speaker_off);
                    // }
                    // }
                    // }
                    // Log.i("tml run_mute Audio");
                }
            } catch (Exception e) {
                Log.e("da16 " + e.getMessage());
            }
        }
    };

    private void configureMuteAndSpeakerButtons() {
        // alec: this might cause galaxy pad crash
        mHandler.post(run_mute);
    }

    Runnable run_finish = new Runnable() {
        public void run() {
            Log.e("exit.dialer activity run_finish!!!");
            finish();
        }
    };

    public void killViewMode() {
        Log.e("!!!attempt to kill-ING Dialer!!!");
        if (VideoCallActivity.getInstance() != null) {
            VideoCallActivity.getInstance().bye();
        }
        finish();
    }

    // tml*** tcp test
    private boolean postDialerWarn = false;

    public void exitCallMode2(String from) {
        postDialerWarn = true;
        if (AireVenus.instance() != null) {
            VoipCore lVoipCore = AireVenus.instance().getVoipCore();
            VoipCall myCall = lVoipCore.getCurrentCall();
            if (myCall != null) {
                lVoipCore.terminateCall(myCall);
                Log.d("exit.exitCallMode2");
            }
        }
        exitCallMode(from);
    }

    private void exitCallMode(String from) {
        exitCallMode(false, from);
    }

    private void exitCallMode(boolean finishing, String from) {
        try {
            Log.e("voip.exitCallMode fin=" + finishing + " (" + from + ")");
            // wjx*** ledspeaker
            needblink = false;
            LedSpeakerUtil.setSpeakerOff();
            LedSpeakerUtil.setLedOff();
            // ***wjx
            if (mSpeechRecognizer != null) { // tml*** voice control
                Log.e("destroying DA voiceRecogn");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((LinearLayout) findViewById(R.id.voiceinfo))
                                .setVisibility(View.GONE);
                        ((TextView) findViewById(R.id.voiceresults))
                                .setVisibility(View.GONE);
                    }
                });
                mSpeechRecognizer.destroy();
                mSpeechRecognizer = null;
                voiceIntent = null;
            }
            mHandler.removeCallbacks(timeElapsed);
            mHandler.removeCallbacks(addPSTNInfoHide);
            mPref.write("way", false);
            mPref.write("tempCheckSameIN", 0); // tml*** sametime
            // alec:routeAudioToReceiver();

            if (AireVenus.instance() != null)
                AireVenus.instance().callStopRing();
            stopRing();

            if (!finishing)
                mHandler.postDelayed(run_finish, 1500);// alec: to delay 1500ms
            // for user to know what
            // happened

            if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM
                    && !incomingChatroom) {
                new Thread(kickOutAllMembers).start();
            }

            updateCallLog();

            imCalling = false;
            // finish(); //simon 062311
            // if(mAudioManager.isMusicActive()) //tml*** mute error, CX
            // mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            controlBkgndMusic(2);
        } catch (Exception e) {
            Log.e("exitCallMode !@#$ " + e.getMessage());
        }
        // Jerry, 031214, On hangup, reset MySocket.BufferedIdxForCall to 0
        Log.d("DialerActivity in exitCallMode, MySocket.BufferedIdxForCall set to 0 on HangingUp...");
        MySocket.BufferedIdxForCall = 0;
    }

    private void routeAudioToSpeaker() {
        // Log.e(">> routeAudioToSpeaker() << EMPTY");
    }

    private void routeAudioToReceiver() {
        // Log.e(">> routeAudioToReceiver() << EMPTY");
    }

    private void callPending(VoipCall call) {
        // Privacy setting to not share the user camera by default
        /***
         * Simon 030811 Disable to whole video stuff for now boolean
         * prefVideoEnable =
         * mPref.readBoolean(getString(R.string.pref_video_enable_key)); boolean
         * prefAutomaticallyShareMyCamera =
         * mPref.readBoolean(getString(R.string.
         * pref_video_automatically_share_my_video_key));
         * getVideoManager().setMuted(!(prefVideoEnable &&
         * prefAutomaticallyShareMyCamera));
         * call.enableCamera(prefAutomaticallyShareMyCamera); Simon
         */
    }

    private String getYourSipServerByTCP(String address) {
        AireJupiter x;
        if ((x = AireJupiter.getInstance()) != null) {
            String sipIP = x.getYourSipServer(address);
            if (sipIP != null && !sipIP.equals(x.mySipServer)) // simon 061811
            {
                address += "@" + sipIP;
                Log.d("voip.(getYourSipServer) Callee2: " + address);
            } else if (sipIP == null) {
                Log.d("voip.(getYourSipServer) Callee0: " + address + "@null");
            } else {
                Log.d("voip.(getYourSipServer) Callee1: " + address + "@same");
            }
        }
        return address;
    }

    public void newOutgoingCall(String address, boolean withVideo, String from) {
        Log.d("newOutgoingCall1 to " + address + " (" + from + ")");
        imCalling = true;
        VoipCore lVoipCore = null;
        if (AireVenus.instance() != null) {
            Log.d("AireVenus is good");
            lVoipCore = AireVenus.instance().getVoipCore();
            if (lVoipCore.isIncall()) {
                Log.e("is BUSY INcall");
                return;
            }
        } else {
            Log.d("AireVenus is bad");
        }
        if (AireVenus.runAsSipAccount && address.startsWith("+")) {
            String SipServer = mPref.read("aireSipServer", "192.168.0.1");
            if (AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL) {
                address = address.substring(1);
                shouldConsumeCredit = true;
            } else if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM
                    && address.startsWith("+"))
                address = address.substring(1);
            else if (SipServer.equals("218.104.51.41")
                    && address.startsWith("+86"))
                address = address.substring(3);
            else if (SipServer.equals("54.249.19.120"))
                address = address.substring(1);
            else if (SipServer.equals("50.112.137.243"))
                address = address.substring(1);

            if (mPref.readBoolean("usestanleysip"))
                shouldConsumeCredit = false;
        }

        Log.d("newOutgoingCall2 runAsSipAccount=" + AireVenus.runAsSipAccount
                + ",  address=" + address);
        VoipAddress lAddress;
        try {
            lAddress = lVoipCore.interpretUrl(address);
        } catch (VoipCoreException e) {
            Log.e("interpretUrl VoipCoreException ??? " + e.getMessage());
            return;
        }

        try {
            Log.i("*** newOutgoingCall3 Calling: " + mDisplayName + " "
                    + address + " vid:" + withVideo);
            CallManager.getInstance().inviteAddress(lAddress, withVideo);
        } catch (VoipCoreException e) {
            Log.e("inviteAddress VoipCoreException withVideo=" + withVideo
                    + " " + e.getMessage());
            return;
        }
    }

    public void initFromConf() throws VoipException {
        try {
            AireVenus.instance().initFromConf();
        } catch (VoipConfigException e) {
            Log.e("DA initFromConf " + e.getMessage());
        }
    }

    protected void hideScreen(boolean isHidden) {
    }

    long enabletime;
    KeyguardManager mKeyguardManager;
    KeyguardManager.KeyguardLock mKeyguardLock;
    boolean enabled;

    void disableKeyguard() {
        if (AmazonKindle.IsKindle())
            return;
        if (mKeyguardManager == null) {
            mKeyguardManager = (KeyguardManager) this
                    .getSystemService(Context.KEYGUARD_SERVICE);
            mKeyguardLock = mKeyguardManager.newKeyguardLock("FafaYou");
            enabled = true;
        }
        if (enabled) {
            mKeyguardLock.disableKeyguard();
            enabled = false;
            enabletime = SystemClock.elapsedRealtime();
        }
    }

    void reenableKeyguard() {
        if (AmazonKindle.IsKindle())
            return;
        if (!enabled) {
            mKeyguardLock.reenableKeyguard();
            enabled = true;
        }
    }

    // tml*** answer view
    private boolean allowMouse = true;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int source = event.getSource();
        int bstate = event.getButtonState();
        // Log.e("tml answer0 Action=" + action + " Source=" + source +
        // " ButtonState=" + bstate);
        if (action == 0 && source == 8194 && bstate == 4 && allowMouse) {
            Log.e("tml answer0view answer");
            ((Button) findViewById(R.id.answer)).performClick();
            allowMouse = false;
            return true;
        }
        return false;
    }

    private boolean mUseBackKey = true;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("tml keycode=" + keyCode + " src=" + event.getSource());
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_POWER)
                && event.getRepeatCount() == 0 && !Connected && incomingCall) {
            if (AireVenus.instance() != null) {
                // AireVenus.instance().stopRinging();
                AireVenus.instance().stopRing(); // tml*** new ring
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_HOME) {
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (mAnswer != null) {
                // mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                if (AireVenus.instance() != null) {
                    VoipCore lVoipCore = AireVenus.instance().getVoipCore();
                    if (lVoipCore.isInComingInvitePending()) {
                        try {
                            VoipCall vc = lVoipCore.getCurrentCall();
                            lVoipCore.acceptCall(vc);
                        } catch (VoipCoreException e) {
                            Log.e("da20 " + e.getMessage());
                            lVoipCore = AireVenus.instance().getVoipCore();
                            VoipCall myCall = lVoipCore.getCurrentCall();
                            if (myCall != null) {
                                lVoipCore.terminateCall(myCall);
                                Log.d("exit.KeyEvent.KEYCODE_ENTER, terminate call");
                            }
                        }
                        return true;
                    }
                }
            }
        }
        // tml*** backpress safety
        else if (keyCode == KeyEvent.KEYCODE_BACK && mUseBackKey) {
            Toast.makeText(DialerActivity.this, getString(R.string.back_again),
                    Toast.LENGTH_SHORT).show();
            mUseBackKey = false;
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_BACK && !mUseBackKey) {
            mUseBackKey = true;
        }
        // ***tml
        return super.onKeyDown(keyCode, event);
    }

    private void showNotification() {
        Notification notification = new Notification(R.drawable.icon_incall,
                getString(R.string.app_name), System.currentTimeMillis());

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, DialerActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        notification.setLatestEventInfo(this, getString(R.string.app_name),
                getString(R.string.in_call), contentIntent);

        notification.defaults = 0;
        notification.flags = Notification.FLAG_ONGOING_EVENT;

        NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNM.notify(R.string.call, notification);
    }

    // ***tml

    void InitDtmfKeyTone() {
        final int keyarray[] = {R.id.key0, R.id.key1, R.id.key2, R.id.key3,
                R.id.key4, R.id.key5, R.id.key6, R.id.key7, R.id.key8,
                R.id.key9};
        Button key;
        for (int i = 0; i < 10; i++) {
            key = (Button) findViewById(keyarray[i]);
            setDigitListener(key, Character.forDigit(i, 10));
        }
        key = (Button) findViewById(R.id.keyStar);
        setDigitListener(key, '*');
        key = (Button) findViewById(R.id.keyHash);
        setDigitListener(key, '#');
    }

    private void setDigitListener(Button view, char dtmf) {
        class DialKeyListener implements OnTouchListener {
            final char mKeyCode;
            boolean mIsDtmfStarted = false;

            DialKeyListener(char aKeyCode) {
                mKeyCode = aKeyCode;
            }

            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && Connected
                        && mIsDtmfStarted == false) {
                    VoipCore p = AireVenus.instance().getVoipCore();
                    if (p != null) {
                        // p.playDtmf(mKeyCode, -1);
                        mIsDtmfStarted = true;
                        if (p.isIncall()) {
                            p.sendDtmf(mKeyCode);
                            DTMFString += mKeyCode;
                            mDisplayNameView.setText(DTMFString);
                        }

                        if (mKeyCode == '*')
                            tg.startTone(ToneGenerator.TONE_DTMF_S, 150);
                        else if (mKeyCode == '#')
                            tg.startTone(ToneGenerator.TONE_DTMF_S, 150);
                        else
                            tg.startTone(ToneGenerator.TONE_DTMF_0
                                    + (mKeyCode - '0'), 150);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopDtmf();
                }
                return false;
            }

            private void stopDtmf() {
				/*
				 * VoipCore p = AireVenus.instance().getVoipCore(); if (p!=null)
				 * p.stopDtmf();
				 */
                mIsDtmfStarted = false;
            }
        }
        ;
        DialKeyListener lListener = new DialKeyListener(dtmf);
        view.setOnTouchListener(lListener);
    }

    public void endUpDialer(String address) {
        if (AireVenus.instance() != null) {
            VoipCore lVoipCore = AireVenus.instance().getVoipCore();
            if (lVoipCore != null && lVoipCore.isIncall())// alec
            {
                VoipCall myCall = lVoipCore.getCurrentCall();
                if (myCall != null) {
                    String remoteNumber = myCall.getRemoteAddress()
                            .getUserName();
                    if (remoteNumber.equals(address)) {
                        Log.d("exit.endUpDialer, terminateCall");
                        lVoipCore.terminateCall(myCall);
                        sendTerminateSignal = false;
                        return;// alec
                    }
                }
            }
        }
        exitCallMode("endUpDialer");
    }

    public static List<ChatroomMember> memberList = new ArrayList<ChatroomMember>();
    public static List<List<ChatroomMember>> multiMemberList = new ArrayList<List<ChatroomMember>>();

    // private ImageAdapter imageAdapter;

    public List<List<ChatroomMember>> toMultiList(List<ChatroomMember> list, int multiNum) {
        List<List<ChatroomMember>> multiList = new ArrayList<List<ChatroomMember>>();
        for (int i = 0; i < (list.size() / multiNum + 1); i++) {
            List<ChatroomMember> partList = new ArrayList<ChatroomMember>();
            for (int j = 0; j < multiNum; j++) {
                int index = i * multiNum + j;

                if (index >= list.size()) break;

                partList.add(list.get(index));
            }
            if (partList.size() > 0)
                multiList.add(partList);
        }
        return multiList;
    }

    int surfaceAngel = 90;

    @SuppressLint("NewApi")
    public void lockScreenOrientation() {
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        if (Integer.parseInt(Build.VERSION.SDK) < 8) {
            surfaceAngel = 90;
            return;
        }
        try {
            switch (((WindowManager) getSystemService(WINDOW_SERVICE))
                    .getDefaultDisplay().getRotation()) {
                case Surface.ROTATION_90:
                    surfaceAngel = 0;
                    break;
                case Surface.ROTATION_180:
                    surfaceAngel = 270;
                    break;
                case Surface.ROTATION_270:
                    surfaceAngel = 180;
                    break;
                default:
                    surfaceAngel = 90;
            }
        } catch (Exception e) {
            Log.e("da21 " + e.getMessage());
        }
    }

    private int displayP2Pcount = 0;
    private int pktssent0 = 0, pktsrecv0 = 0;
    int pktssentavg = 0, pktsrecvavg = 0;
    Runnable displayP2P = new Runnable() {
        public void run() {
            VoipCore lc = AireVenus.getLc();
            if (lc != null && lc.isIncall()) {
                displayP2Pcount++;
                try {
                    int status = lc.getStatus();
                    ImageView iv = (ImageView) findViewById(R.id.ind0);
                    if ((status & 0xF00) == 0x100)
                        iv.setImageResource(R.drawable.purple);// amr
                    else if ((status & 0xF00) == 0x200)
                        iv.setImageResource(R.drawable.red);// speex
                    else if ((status & 0xF00) == 0x300)
                        iv.setImageResource(R.drawable.blue);// PCMA
                    else if ((status & 0xF00) == 0x400)
                        iv.setImageResource(R.drawable.teal);// PCMU
                    else if ((status & 0xF00) == 0x600)
                        iv.setImageResource(R.drawable.orange);// OPUS
                    else
                        iv.setImageResource(R.drawable.gray);

                    iv = (ImageView) findViewById(R.id.ind1);
                    if (lc.isRunningP2P())
                        iv.setImageResource(R.drawable.orange);
                    else
                        iv.setImageResource(R.drawable.yellow);

                    iv = (ImageView) findViewById(R.id.ind2);
                    int send = status & 0xF;
                    if (send == 0x5)
                        iv.setImageResource(R.drawable.red);
                    else if (send == 0x4)
                        iv.setImageResource(R.drawable.green);
                    else if (send == 0x1)
                        iv.setImageResource(R.drawable.teal);
                    else
                        iv.setImageResource(R.drawable.gray);

                    iv = (ImageView) findViewById(R.id.ind3);
                    int recv = status & 0xF0;
                    if (recv == 0x50)
                        iv.setImageResource(R.drawable.red);
                    else if (recv == 0x40)
                        iv.setImageResource(R.drawable.blue);
                    else if (recv == 0x10)
                        iv.setImageResource(R.drawable.purple);
                    else
                        iv.setImageResource(R.drawable.gray);

                    ((LinearLayout) findViewById(R.id.status))
                            .setVisibility(View.VISIBLE);

                    int[] ports = lc.getPorts();
                    if (displayP2Pcount < 300) {
                        pktssentavg = ports[10] / displayP2Pcount;
                        pktsrecvavg = ports[11] / displayP2Pcount;
                    }
                    String info = "cpu:" + MyUtil.getCPU(false)[1] + " ("
                            + MyUtil.getCPU(false)[2] + "-"
                            + MyUtil.getCPU(false)[3] + ")" + "\nrelay local:"
                            + ports[0] + " v:" + ports[1] + "\nrelay remote:"
                            + ports[2] + " v:" + ports[3] + "\nice local:"
                            + ports[4] + " v:" + ports[5] + "\nice remote:"
                            + ports[6] + " (" + ports[7] + ") v:" + ports[8]
                            + " (" + ports[9] + ")" + "\npkts sent:"
                            + ports[10] + "+" + (ports[10] - pktssent0) + "/"
                            + pktssentavg + " (mic:" + ports[16] + " ec:"
                            + ports[17] + " enc:" + ports[15] + ")\npkts recv:"
                            + ports[11] + "+" + (ports[11] - pktsrecv0) + "/"
                            + pktsrecvavg + " (ice:" + ports[12] + ")"
                            + "\nnAPPSent=" + ports[18] + " nAPPRecvd="
                            + ports[13] + "\npcktinQ:" + ports[14]
                            + "\nnTicker:" + ports[19];
                    pktssent0 = ports[10];
                    pktsrecv0 = ports[11];
                    ((TextView) findViewById(R.id.debuginfo)).setText(info);
                    ((FrameLayout) findViewById(R.id.debug))
                            .setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Log.e("da22 " + e.getMessage());
                }

                mHandler.postDelayed(displayP2P, 1000);
            }
        }
    };

    // tml test
    private void updateCallDebugStatus(boolean reset, String message) {
        if (AireJupiter.getInstance() != null)
            AireJupiter.getInstance().updateCallDebugStatus(reset,
                    "\n" + message);
    }

    // tml test
    Runnable displayVOIP = new Runnable() {
        public void run() {
            String callStatus = "";
            if (AireJupiter.getInstance() != null)
                callStatus = AireJupiter.getInstance().getCallDebugStatus();
            if (((TextView) findViewById(R.id.debuginfo2)).getVisibility() != View.VISIBLE) {
                ((TextView) findViewById(R.id.debuginfo2))
                        .setVisibility(View.VISIBLE);
            }
            ((TextView) findViewById(R.id.debuginfo2)).setText(callStatus);
            mHandler.postDelayed(displayVOIP, 500);
        }
    };

    Runnable showTests = new Runnable() {
        public void run() {
            ((RelativeLayout) findViewById(R.id.TESTINGONLY))
                    .setVisibility(View.VISIBLE);
        }
    };

    // private MediaPlayer mRingerPlayer = null;
    // private Timer mRingLimit;
    // private void startRinging_old (String from) {
    // // startRingBackSpeex("speex"); //yang*** speex player/
    // try {
    // //// if (mRingerPlayer == null) {
    // //// mRingerPlayer = new MediaPlayer();
    // //// try{
    // //// mRingerPlayer.setAudioStreamType(AudioManager.STREAM_RING);
    // //// mRingerPlayer.setDataSource(getApplicationContext(),
    // RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
    // //// mRingerPlayer.prepare();
    // //// mRingerPlayer.setLooping(true);
    // //// mRingerPlayer.start();
    // //// }catch (Exception e){
    // //// Log.e("da23 " + e.getMessage());
    // //// mRingerPlayer=null;
    // //// }
    // //// } else {
    // //// Log.w("already ringing");
    // //// }
    // //tml|sw*** audio break
    // controlBkgndMusic(0);
    // if (mAudioTrack == null && !ringrdy) {
    // Log.d("tml DA STARTringing ***** DO! (" + from + ")");
    // if (mPref.readBoolean("normal_ring", true)) {
    // int iMinBufSize = AudioTrack.getMinBufferSize(16000,
    // AudioFormat.CHANNEL_CONFIGURATION_MONO,
    // AudioFormat.ENCODING_PCM_16BIT);
    // mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
    // AudioFormat.CHANNEL_CONFIGURATION_MONO,
    // AudioFormat.ENCODING_PCM_16BIT,
    // iMinBufSize, AudioTrack.MODE_STREAM);
    // } else {
    // int iMinBufSize = AudioTrack.getMinBufferSize(16000,
    // AudioFormat.CHANNEL_CONFIGURATION_MONO,
    // AudioFormat.ENCODING_PCM_16BIT);
    // mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
    // AudioFormat.CHANNEL_CONFIGURATION_MONO,
    // AudioFormat.ENCODING_PCM_16BIT,
    // iMinBufSize, AudioTrack.MODE_STREAM);
    // }
    // float maxVol0 = AudioTrack.getMaxVolume();
    // mAudioTrack.setStereoVolume(maxVol0, maxVol0);
    // int maxVol1 =
    // mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    // prevVol1 = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    // Log.e("tml previousVol=" + prevVol1);
    // if (mPref.readBoolean("voice_control", false)) {
    // mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol1/2, 0);
    // } else {
    // mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol1, 0);
    // }
    // mHandler.post(new Runnable () {
    // @Override
    // public void run() {
    // ringrdy = true;
    // ringTask = new MyRingerTask(0);
    // ringTask.execute();
    // }
    // });
    // } else {
    // Log.w("already ringing");
    // }
    // //***tml
    // mHandler.postDelayed(ringLimit, RINGLIMITX);//30 sec
    // } catch (Exception e) {
    // Log.e("cannot handle incoming call " + e.getMessage());
    // //tml|sw*** audio break
    // mAudioTrack = null;
    // ringrdy = false;
    // //***tml
    // }
    // }
    // tml*** check empty call
    // voiplib also does this, but at 15s
    // can catch it first by setting limit to 14
    private int emptyCall = 0;
    private int prevRecvpkts = 0;
    private int timerCall = 199;
    Runnable checkCurrentCall = new Runnable() {
        @Override
        public void run() {
            int emptyLimit = 14;
            int timerLimit = 200;
            timerCall++;
            VoipCore lc = AireVenus.getLc();
            if (lc != null && lc.isIncall()) {
                int[] ports = lc.getPorts();
                int recvpkts = ports[11];
                if ((recvpkts - prevRecvpkts) == 0) {
                    emptyCall++;
                    Log.e("voip.myCall is empty/broken " + emptyCall + "/"
                            + emptyLimit);
                } else {
                    emptyCall = 0;
                }
                prevRecvpkts = recvpkts;
            }

            if (emptyCall == emptyLimit) {
                VoipCore lVoipCore = AireVenus.instance().getVoipCore();
                VoipCall myCall = lVoipCore.getCurrentCall();
                updateCallDebugStatus(false, "myCall was empty/broken!!!");
                if (myCall != null) {
                    lVoipCore.terminateCall(myCall);
                    Log.e("voip.exit.myCall1 was empty/broken!!!");
                } else {
                    if (VideoCallActivity.getInstance() != null) {
                        VideoCallActivity.getInstance().bye();
                    }
                    finish();
                    Log.e("voip.exit.myCall2 was empty/broken!!!");
                }
                Intent it = new Intent(Global.Action_InternalCMD);
                it.putExtra("Command", Global.CMD_CONNECTION_POOR);
                it.putExtra("ForcePoor", true);
                sendBroadcast(it);
            }

            if (timerCall == timerLimit) { // tml temp fix, broadcast conflict
                // ui
                if (AireJupiter.getInstance() != null) {
                    AireJupiter.getInstance().rebuildAlarmReceiver();
                    timerCall = 0;
                }
            }

            mHandler.postDelayed(checkCurrentCall, 1000);
        }
    };

    Runnable ringLimit = new Runnable() {
        @Override
        public void run() {
            Log.e("ringLimit !!!");
            // stopRinging();
            stopRing(); // tml*** new ring

            mStatus.setText(R.string.call_end);
            mHangup.setOnClickListener(null);
            HangingUp = true;
            sendTerminateSignal = true;
            try {
                mHandler.removeCallbacks(timeElapsed);
                mHandler.removeCallbacks(run_disp_msg);
                mHandler.removeCallbacks(run_mute);
                mHandler.removeCallbacks(addPSTNInfoHide);

                if (AireVenus.instance() != null) {
                    VoipCore lVoipCore = AireVenus.instance().getVoipCore();
                    VoipCall myCall = lVoipCore.getCurrentCall();
                    if (myCall != null) {
                        lVoipCore.terminateCall(myCall);
                        Log.e("voip.exit.ringLimit terminateCall()");
                        return;
                    }
                    Log.d("getCurrentCall==null");
                }
            } catch (Exception e) {
                Log.e("da24 " + e.getMessage());
            }
            exitCallMode("ringLimit");
        }
    };

    // tml*** new ring
    private AudioTrack mAudioTrack = null;
    private InputStream inS = null;
    private DataInputStream dinS = null;
    private volatile boolean ringrdy = false;

    public void prepareRing(boolean en, int mode, int calltype, String from) {
        if (en) {
            Log.e("RING." + mode + AireVenus.getCallTypeName(calltype)
                    + " DA *** PREP! (" + from + ")");
            if (mode == 1) { // incoming
                if (mAudioTrack == null) {
                    if (!mPref.readBoolean("normal_ring", true)) {
                        int iMinBufSize = AudioTrack.getMinBufferSize(16000,
                                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                AudioFormat.ENCODING_PCM_16BIT);
                        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                                16000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                AudioFormat.ENCODING_PCM_16BIT, iMinBufSize,
                                AudioTrack.MODE_STREAM);
                    } else {
                        int iMinBufSize = AudioTrack.getMinBufferSize(16000,
                                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                AudioFormat.ENCODING_PCM_16BIT);
                        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                                16000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                AudioFormat.ENCODING_PCM_16BIT, iMinBufSize,
                                AudioTrack.MODE_STREAM);
                    }
                    int voloption = mPref.readInt("incRingVolume", 2);
                    if (mPref.readBoolean("voice_control", false)) {
                        if (mPref.readBoolean("normal_ring", true)) {
                            maxiVol(1, ((double) voloption / 3));
                        } else {
                            maxiVol(1, ((double) voloption / 3)); // lessen vol?
                        }
                        if (!(AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM)) {
                            mHandler.postDelayed(voiceRecogn, 500);
                        } else {
                            mHandler.postDelayed(voiceRecogn, 1000);
                        }
                    } else {
                        maxiVol(1, ((double) voloption / 3));
                    }
                    controlBkgndMusic(0);
                    mHandler.postDelayed(ringLimit, RINGLIMITX);
                    StartRing startRing = new StartRing(mode, calltype, true);
                    new Thread(startRing).start();
                }
            } else if (mode == 0) { // outgoing
                if (mAudioTrack == null) {
                    int iMinBufSize = AudioTrack.getMinBufferSize(16000,
                            AudioFormat.CHANNEL_CONFIGURATION_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            16000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, iMinBufSize,
                            AudioTrack.MODE_STREAM);
                    maxiVol(1, 0.5);
                    controlBkgndMusic(0);
                    StartRing startRing = new StartRing(mode, calltype, true);
                    new Thread(startRing).start();
                }
            }
        }
    }

    private class StartRing implements Runnable {
        int _mode;
        int _calltype;
        boolean _first;

        StartRing(int mode, int calltype, boolean first) {
            _mode = mode;
            _calltype = calltype;
            _first = first;
        }

        @Override
        public void run() {
            if (AireJupiter.getInstance() != null) {
                AireJupiter.getInstance().TESTseeVolumes("DAring");
            }
            ringrdy = true;
            playRing(_mode, _first);
        }
    }

    public void playRing(int mode, boolean first) {
        boolean repeat = false;
        try {
            int buffSize = 5120;
            byte[] audiobuff = new byte[buffSize];
            int i = 0;
            Random rng = new Random();
            String ringfile;

            if (mode == 1) { // incoming
                if (mPref.readBoolean("normal_ring", true)) {
                    if (!mPref.readBoolean("voice_control", false)) {
                        // if (!mPref.readBoolean("voice_control", false)
                        // || (mPref.readBoolean("voice_control", false)
                        // && AireVenus.getCallType() ==
                        // AireVenus.CALLTYPE_CHATROOM)) {
                        ringfile = "incring3_1616.pcm"; // complete normal ring
                    } else {
                        ringfile = "incring4_1616.pcm"; // repeat
                        if (first) {
                            if (!(AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM)) {
                                mHandler.postDelayed(runSpeechListen, 1500);
                            } else {
                                mHandler.postDelayed(runSpeechListen, 1500); // in
                                // CHATROOM
                            }
                        } else {
                            mHandler.postDelayed(runSpeechListen, 1500);
                        }
                    }
                } else {
                    String intx = Integer.toString(rng.nextInt(8) + 1);
                    ringfile = "r16k_" + intx + ".raw";
                }
            } else { // outgoing
                String intx = Integer.toString(rng.nextInt(8) + 1);
                ringfile = "r16k_" + intx + ".raw";
            }

            Log.e("RING." + ringfile + " *** DO! " + ringrdy + " first."
                    + first);
            if (ringrdy && mAudioTrack != null) {
                AssetManager inAssets = getAssets();
                inS = inAssets.open(ringfile);
                dinS = new DataInputStream(inS);

                mAudioTrack.play();
                while (((i = dinS.read(audiobuff, 0, buffSize)) > -1)) {
                    mAudioTrack.write(audiobuff, 0, i);
                    if (!ringrdy || theDialer == null)
                        break;
                }
            } else {
                Log.e("RING mAudioTrack null");
                stopRing();
                audiobuff = null;
                ringrdy = false;
                inS = null;
                dinS = null;
                return;
            }

            audiobuff = null;
            Log.e("RING reach END.force(" + !ringrdy + ")");
            if (ringrdy && theDialer != null) { // incoming
                Log.d("RING repeat");
                if (inS != null) {
                    inS.close();
                    inS = null;
                }
                if (dinS != null) {
                    dinS.close();
                    dinS = null;
                }
                if (mode == 1 && mPref.readBoolean("normal_ring", true)
                        && mPref.readBoolean("voice_control", false)) {
                    if (ringrdy) {
                        repeat = true;
                        mHandler.postDelayed(runRepeatVC, 1500);
                    }
                } else {
                    if (ringrdy) {
                        repeat = true;
                        StartRing startRing = new StartRing(mode,
                                AireVenus.getCallType(), false);
                        new Thread(startRing).start();
                    }
                }
            }
        } catch (Exception e) {
            Log.e("DA RING ERR " + e.getMessage());
            stopRing();
        } finally {
            if (!repeat)
                ringrdy = false;
        }
    }

    Runnable runRepeatVC = new Runnable() {
        @Override
        public void run() {
            maxiMic(0, 1);
            startSpeechListen("stop");
            // new Thread(runREPlayRing).start();
            if (theDialer != null) {
                StartRing startRing = new StartRing(1, AireVenus.getCallType(),
                        false);
                new Thread(startRing).start();
            }
        }
    };

    // Runnable runREPlayRing = new Runnable() {
    // @Override
    // public void run() {
    // Log.e("runREPlayRing");
    // ringrdy = true;
    // playRing(1, false);
    // }
    // };

    private void stopRing() {
        mHandler.removeCallbacks(voiceRecogn);
        mHandler.removeCallbacks(runSpeechListen);
        mHandler.removeCallbacks(runRESpeechListen0);
        mHandler.removeCallbacks(runRESpeechListen1);
        mHandler.removeCallbacks(runRepeatVC);
        // mHandler.removeCallbacks(runREPlayRing);
        try {
            ringrdy = false;
            if (mAudioTrack != null) {
                mAudioTrack.stop();
                mAudioTrack.flush();
                mAudioTrack.release();
                mAudioTrack = null;
                maxiVol(0, 1);
                Log.e("RING *** STOP!");
            }
            if (inS != null)
                inS.close();
            if (dinS != null)
                dinS.close();
            // Log.e("RING *** STOP!");
        } catch (IOException e) {
            Log.e("DA stopRing ERR " + e.getMessage());
        } finally {
            mAudioTrack = null;
            inS = null;
            dinS = null;
            maxiMic(0, 1);
            maxiVol(0, 1);
        }
    }

    private int prevVol1, returnVol = 0;

    private void maxiVol(int mode, double divAM) {
        AudioManager audioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
        int maxVol1 = 1;
        if (divAM < 0)
            divAM = 1;
        if (mode == 1) { // max vol
            prevVol1 = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            maxVol1 = audioManager
                    .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            returnVol = 1;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    (int) (maxVol1 * divAM), 0);
            Log.d("maxiVol DA set." + mode + " " + divAM + "|" + prevVol1 + "|"
                    + (maxVol1 * divAM));
        } else if (mode == 0 && returnVol == 1) { // return vol
            maxVol1 = prevVol1;
            returnVol = 0;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    (int) (maxVol1 * divAM), 0);
            Log.d("maxiVol DA return." + mode + " " + divAM + "|" + prevVol1);
        }
    }

    boolean musicWasActive = false;

    private void controlBkgndMusic(int mode) {
        // AudioManager mAudioManager = ((AudioManager)
        // getSystemService(Context.AUDIO_SERVICE));
        Intent imediaplay = new Intent("com.android.music.musicservicecommand");
        if (mode == 0 && mAudioManager.isMusicActive()) {
            // mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            // //also muting ring
            // imediaplay.putExtra("command", "pause");
            // sendBroadcast(imediaplay);
            musicWasActive = true;
            Log.d("controlBkgndMusic" + mode + musicWasActive
                    + mAudioManager.isMusicActive());
        } else if (mode == 1 && musicWasActive) {
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            // imediaplay.putExtra("command", "play");
            // sendBroadcast(imediaplay);
            Log.d("controlBkgndMusic" + mode + musicWasActive
                    + mAudioManager.isMusicActive());
            musicWasActive = false;
        } else if (mode == 2) {
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            Log.d("controlBkgndMusic" + mode + musicWasActive
                    + mAudioManager.isMusicActive());
            musicWasActive = false;
        }
    }

    // ***tml

    // public void stopRinging_old() {
    // // if(rwb!=null){ //yang*** speex player
    // // rwb.stop();
    // // rwb.release();
    // // }
    // // if (mRingerPlayer !=null) {
    // // mRingerPlayer.stop();
    // // mRingerPlayer.release();
    // // mRingerPlayer=null;
    // // }
    // //tml|sw*** audio break
    // if (mAudioTrack != null && ringrdy) {
    // try {
    // Log.d("tml DA STOPring ***** DO!");
    // ringTask.cancel(true);
    // ringrdy = false;
    // // mAudioTrack.flush();
    // // mAudioTrack.stop();
    // // mAudioTrack.release();
    // if (inS != null) {
    // inS.close();
    // }
    // if (dinS != null) {
    // dinS.close();
    // }
    // inS = null;
    // dinS = null;
    // mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, prevVol1, 0);
    // prevVol1 = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    // Log.e("tml resumeVol=" + prevVol1);
    // Log.d("tml DA STOPring ***** SUCCESS!");
    // } catch (IOException e) {
    // Log.e("da stopRing " + e.getMessage());
    // }
    // }
    // //***tml
    // }
    //
    // //tml|sw*** audio break, new ringer code, so ui doesnt freeze
    // private MyRingerTask ringTask;
    // private class MyRingerTask extends AsyncTask<Void, Void, Void> {
    // int ringmode;
    // MyRingerTask (int mode) {
    // ringmode = mode;
    // }
    //
    // @Override
    // protected Void doInBackground(Void... params) {
    // try {
    // int bufferSize = 5120;
    // byte[] audiobuff = new byte[bufferSize];
    //
    // int i = 0;
    // AssetManager am = getAssets();
    // String musicfile;
    // if (mPref.readBoolean("normal_ring", true) && ringmode == 0) {
    // musicfile = "incring3_1616.pcm";
    // } else {
    // Random rng = new Random();
    // String intx = Integer.toString(rng.nextInt(8) + 1);
    // if (SettingPage.TEST) {
    // intx = Integer.toString(SettingPage.TESTvalueI);
    // }
    // musicfile = "r16k_" + intx + ".raw";
    // if (SettingPage.TESTvalueI == 0) musicfile = "incring3_1616.pcm";
    // }
    // Log.i("tml ringfile> " + musicfile);
    //
    // if (ringrdy) {
    // inS = am.open(musicfile);
    // dinS = new DataInputStream(inS);
    //
    // mAudioTrack.play();
    // while(((i = dinS.read(audiobuff, 0, bufferSize)) > -1) ) {
    // mAudioTrack.write(audiobuff, 0, i);
    // if (ringTask.isCancelled()) {
    // mAudioTrack.pause();
    // mAudioTrack.flush();
    // mAudioTrack.stop();
    // mAudioTrack.release();
    // Log.e("tml DA ring.mAudioTrack mid-cleared!");
    // break;
    // }
    // }
    // }
    // // mAudioTrack.stop();
    // // mAudioTrack.flush();
    // // mAudioTrack.release();
    // mAudioTrack = null;
    // // inS.close();
    // // dinS.close();
    // // inS = null;
    // // dinS = null;
    // Log.d("tml DA MyRingerTask END");
    // } catch (Exception e) {
    // Log.e("tml DA MyRingerTask ERR " + e.getMessage());
    // ringrdy = false;
    // mAudioTrack.stop();
    // mAudioTrack.release();
    // mAudioTrack = null;
    // if (inS != null) {
    // inS = null;
    // }
    // if (dinS != null) {
    // dinS = null;
    // }
    // }
    // return null;
    // }
    //
    // @Override
    // public void onPostExecute(Void result) {
    // }
    // }
    // ***tml

    // yang*** speex player
    // private RingerPlayer_WB rwb;
    // public synchronized void startRingBackSpeex(String from) {
    // Log.d("yang start ring back speex");
    // AssetManager am = getAssets();// u have get assets path from this code
    // Random random = new Random();
    // int numring = 2;
    // int i =random.nextInt(numring) + 1;
    // String ring = "ring"+i+".spx";
    // Log.d("yang ring "+ring);
    // try {
    // InputStream inputStream = am.open(ring);
    // byte[] data = toByteArray(inputStream);
    // ArrayList<byte[]> arrayList = new ArrayList<byte[]>();
    // byte[] newdata = new byte[data.length];
    // System.arraycopy(data, 0, newdata, 0, data.length);
    //
    // rwb= new RingerPlayer_WB(DialerActivity.this, 3, true);
    //
    // rwb.append(newdata, newdata.length);
    // rwb.run();
    //
    // } catch (IOException e) {
    // rwb.stop();
    // rwb.release();
    // e.printStackTrace();
    // }
    // }
    //
    // public static byte[] toByteArray(InputStream input) {
    // ByteArrayOutputStream output = new ByteArrayOutputStream();
    // byte[] buffer = new byte[4096];
    // int n = 0;
    // try {
    // while (-1 != (n = input.read(buffer))) {
    // output.write(buffer, 0, n);
    // }
    // } catch (IOException e) {
    // e.printStackTrace();
    // try {
    // output.close();
    // } catch (IOException e1) {
    // e1.printStackTrace();
    // }
    // }
    // return output.toByteArray();
    // }
    // ***yang

    public String getCurrentOnCallName() {
        return mDisplayName;
    }

    public String getCurrentOnCallStatus() {
        // return mStatus.getText().toString(); //tml*** oldconf bighead/
        if (theDialer == null)
            return "";
        String str = "";
        try {
            // str = ((TextView)
            // findViewById(R.id.status_label)).getText().toString();
            // tml*** vidconf
            if (((TextView) findViewById(R.id.conf_timer)).getVisibility() == View.VISIBLE) {
                str = ((TextView) findViewById(R.id.conf_timer)).getText()
                        .toString();
            } else {
                str = ((TextView) findViewById(R.id.status_label)).getText()
                        .toString();
            }
        } catch (Exception e) {
            Log.e("da25 " + e.getMessage());
        }
        return str;
    }

    public Bitmap getCurrentDrawable() {
        return photoBitmap;
    }

    public void hangupCall() {
        Log.d("hangupCall when minimized");
        if (rejectHangingup
                && AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL)
            return;
        mStatus.setText(R.string.call_end);
        mHangup.setOnClickListener(null);
        HangingUp = true;
        sendTerminateSignal = true;
        try {
            if (mSpeechRecognizer != null) { // tml*** voice control
                Log.e("destroying DA voiceRecogn");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((LinearLayout) findViewById(R.id.voiceinfo))
                                .setVisibility(View.GONE);
                        ((TextView) findViewById(R.id.voiceresults))
                                .setVisibility(View.GONE);
                    }
                });
                mSpeechRecognizer.destroy();
                mSpeechRecognizer = null;
                voiceIntent = null;
            }
            mHandler.removeCallbacks(timeElapsed);
            mHandler.removeCallbacks(run_disp_msg);
            mHandler.removeCallbacks(run_mute);
            mHandler.removeCallbacks(addPSTNInfoHide);

            if (AireVenus.instance() != null) {
                VoipCore lVoipCore = AireVenus.instance().getVoipCore();
                VoipCall myCall = lVoipCore.getCurrentCall();
                if (myCall != null) {
                    lVoipCore.terminateCall(myCall);
                    Log.d("exit.mHangup Pressed");
                    return;
                }
                Log.d("getCurrentCall==null");
            }
        } catch (Exception e) {
            Log.e("da26 " + e.getMessage());
        }
        exitCallMode("hangupCall");
    }

    // tml*** voice control
    private Intent voiceIntent;
    Runnable voiceRecogn = new Runnable() {
        @Override
        public void run() {
            Log.e("tmlv DA init voiceRecogn");
            voiceIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault());
            voiceIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
            try {
                if (!mPref.readBoolean("normal_ring", true)) {
                    if (!(AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM)) {
                        maxiMic(1, 1);
                        mHandler.post(runSpeechListen);
                        Log.e("tmlv init voiceRecogn startListening");
                    } else {
                        maxiMic(1, 1);
                        mHandler.postDelayed(runSpeechListen, 1500);
                        Log.e("tmlv init voiceRecogn startListening CONF");
                    }
                }
            } catch (Exception e) {
                destroyVoice();
                Log.e("tmlv DA voiceRecogn.ERR " + e.getMessage());
                Toast.makeText(getApplicationContext(),
                        "ERROR: VoiceRecogn not supported", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    };

    public void voiceRecogn() {
        mHandler.post(voiceRecogn);
    }

    Runnable hideVoiceInfo = new Runnable() {
        @Override
        public void run() {
            ((TextView) findViewById(R.id.voiceresults))
                    .setVisibility(View.GONE);
            ((ProgressBar) findViewById(R.id.voicerms)).setIndeterminate(false);
            ((LinearLayout) findViewById(R.id.voiceinfo))
                    .startAnimation(AnimationUtils.loadAnimation(theDialer,
                            R.anim.fadeout));
            ((LinearLayout) findViewById(R.id.voiceinfo))
                    .setVisibility(View.GONE);
        }
    };

    private void destroyVoice() {
        if (mSpeechRecognizer != null) {
            Log.e("destroying DA voiceRecogn");
            mHandler.removeCallbacks(voiceRecogn);
            mHandler.removeCallbacks(runSpeechListen);
            mHandler.removeCallbacks(runRESpeechListen0);
            mHandler.removeCallbacks(runRESpeechListen1);
            mHandler.removeCallbacks(runRepeatVC);
            // mHandler.removeCallbacks(runREPlayRing);
            maxiMic(0, 1);
            mHandler.postDelayed(hideVoiceInfo, 1000);
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
            voiceIntent = null;
        }
    }

    public void startSpeechListen(String mode) {
        Log.e("tmlvoice startSpeechListen(" + mode + ")");
        if (mSpeechRecognizer != null && voiceIntent != null) {
            if (mode.equals("stop")) {
                mSpeechRecognizer.stopListening();
            } else if (mode.equals("cancel")) {
                mSpeechRecognizer.cancel();
            } else if (mode.equals("restart0")) {
                if (!mPref.readBoolean("normal_ring", true)) {
                    mSpeechRecognizer.stopListening();
                    mSpeechRecognizer.startListening(voiceIntent);
                }
            } else if (mode.equals("restart1")) {
                mSpeechRecognizer.stopListening();
                mSpeechRecognizer.startListening(voiceIntent);
            } else if (mode.equals("crestart")) {
                mSpeechRecognizer.cancel();
                mSpeechRecognizer.startListening(voiceIntent);
            } else if (mode.equals("start")) {
                mSpeechRecognizer.startListening(voiceIntent);
            }
        }
        if (mSpeechRecognizer != null && mode.equals("destroy")) {
            destroyVoice();
        }

    }

    Runnable runSpeechListen = new Runnable() {
        @Override
        public void run() {
            maxiMic(1, 1);
            startSpeechListen("start");
        }
    };

    Runnable runRESpeechListen0 = new Runnable() {
        @Override
        public void run() {
            startSpeechListen("restart0");
        }
    };

    Runnable runRESpeechListen1 = new Runnable() {
        @Override
        public void run() {
            startSpeechListen("restart1");
        }
    };

    Runnable destroySpeechListen = new Runnable() {
        @Override
        public void run() {
            if (!mPref.readBoolean("voice_control", false)) {
                startSpeechListen("destroy");
            }
        }
    };

    private int prevMic1, returnMic = 0;

    private void maxiMic(int mode, double divAM) {
        int maxMic1 = 1, setMic;
        if (divAM < 0)
            divAM = 1;
        if (mode == 1) { // max vol
            prevMic1 = mAudioManager
                    .getStreamVolume(AudioManager.STREAM_SYSTEM);
            maxMic1 = mAudioManager
                    .getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
            if ((int) (maxMic1 * divAM) < prevMic1) {
                setMic = prevMic1;
            } else {
                setMic = (int) (maxMic1 * divAM);
            }
            mAudioManager
                    .setStreamVolume(AudioManager.STREAM_SYSTEM, setMic, 0);
            returnMic = 1;
            Log.d("maxiMic DA set." + mode + " " + divAM + "|" + prevMic1 + "|"
                    + setMic);
        } else if (mode == 0 && returnMic == 1) { // return mic
            maxMic1 = prevMic1;
            setMic = (int) (maxMic1 * divAM);
            mAudioManager
                    .setStreamVolume(AudioManager.STREAM_SYSTEM, setMic, 0);
            returnMic = 0;
            Log.d("maxiMic DA return." + mode + " " + divAM + "|" + prevMic1);
        }
    }

    // ***tml
    // tml*** beta ui
    public void toastMaker(String text, int textsize, int length, int gravity,
                           int goffx, int goffy) {
        if (text != null) {
            if (!(length == 0 || length == 1))
                length = Toast.LENGTH_SHORT;
            Toast tst = Toast.makeText(theDialer, text, length);
            try {
                tst.setGravity(gravity, goffx, goffy);
                if (textsize > 0) {
                    LinearLayout tstLayout = (LinearLayout) tst.getView();
                    TextView tstTV = (TextView) tstLayout.getChildAt(0);
                    tstTV.setTextSize(textsize);
                }
            } catch (ClassCastException e) {
            }
            tst.show();
        }
    }

    // tml*** cec
    public void hdmiCmdExec(String param) {
        if (AireJupiter.getInstance() != null && param != null) {
            AireJupiter.hdmiCmdExec(param);
        }
    }

    /**
     * 初始化crosswalk，加载多方video。
     *
     * @param groupIndex 加载视频的成员小组索引，从0开始；拼接在room后面。＜0 时表示加载全部成员video；不在room后面拼接。
     */
    public void initVidConf(int groupIndex) {
        if (groupIndex != -1 && curGroupIndex == groupIndex) return;
        try {
            if (xWalkWebView == null) {
                XWalkPreferences.setValue(
                        XWalkPreferences.ANIMATABLE_XWALK_VIEW, false);
                // if (Log.enDEBUG)
                // XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING,
                // true);
                xWalkWebView = new XWalkView(theDialer, DialerActivity.this);
                xWalkWebView.clearCache(false);
                Log.i("vidConfxwalk xWalkWebView ready");
            }

//          String vcUrl = "https://www.xingfafa.com:8443/demos/demo_multiparty_video_only_b.html?";
            String vcUrl = "http://115.29.171.94:8000/demos/demo_multiparty_video_only_b.html?";
            // String vcUrl =
            // "https://www.xingfafa.com:8443/demos/demo_multiparty_video_only_c.html?";
            int roomN = mPref.readInt("ChatroomHostIdx");
            String vcRoom = "room=" + Integer.toString(roomN);

            //li*** 分批加载video。
            if (isMultiMember && groupIndex >= 0) {
                vcRoom += groupIndex;
            }

            String vcRes = "&res=2";
            if (mPref.readBoolean("VC240"))
                vcRes = "&res=1";
            else if (mPref.readBoolean("VC720"))
                vcRes = "&res=3";

            // tml|victor*** vc8
            if (mPref.readBoolean("VCx8", false)) {
                vcUrl = "http://115.29.171.94:8080/demos/demo_multiparty_video_only_8.html?";
            }
            // tml*** broadcast
            if (mPref.readInt(Key.BCAST_CONF, -1) >= 0) {
                String BAddress = MyTelephony.attachPrefix(DialerActivity.this,
                        getIntent().getStringExtra("PhoneNumber"));
                int myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
                String getIdx = "&idx=" + myIdx;
                String getPassword = mPref.read("password", "1111");
                String setBase64 = MyUtil.setBase64(getPassword);
                String base64 = MyUtil.getBase64(setBase64);
                MyPreference mPref = new MyPreference(this);
                String myNickname = "&nickname=" + mPref.read("myNickname");
                String newRoomN = "";
                if (getIdx.length() < 7) {
                    newRoomN = MakeCall.ConferenceCall(AireApp.context, myIdx + "", mPref.readInt(Key.BCAST_CONF, -1), true);
                } else {
                    if (mPref.readInt(Key.BCAST_CONF, -1) == 1) {//主叫方
                        newRoomN = "1007" + mPref.readInt("ChatroomHostIdx");
                    } else if (mPref.readInt(Key.BCAST_CONF, -1) == 0) {
                        newRoomN = "1008" + mPref.readInt("ChatroomHostIdx");
                    }
                }
                String BRoom = "&room=" + newRoomN;
                vcUrl = "http://bc.xingfafa.com/release/call.htm?";
                xWalkWebView.load(vcUrl + vcRes + getIdx + "&pd=W1o2r3d4p5s6" + setBase64 + myNickname + BRoom, null);
                Log.i("Brocasting URL:" + vcUrl + vcRes + getIdx + "&pd=W1o2r3d4p5s6" + setBase64 + myNickname + BRoom);
            } else {
                Log.i("vidConfxwalk >> " + vcRoom + vcRes + " " + vcUrl);
                xWalkWebView.load(vcUrl + vcRoom + vcRes, null);
            }


            ((SurfaceView) findViewById(R.id.topVWin_surface))
                    .setVisibility(View.VISIBLE);
            // ((SurfaceView)
            // findViewById(R.id.topVWin_surface)).setZOrderOnTop(false);
            ((LinearLayout) findViewById(R.id.topVWin_holder))
                    .addView(xWalkWebView);

            boolean hwAccelOk = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
            if (hwAccelOk) {
                ((SurfaceView) findViewById(R.id.topVWin_surface))
                        .setLayerType(View.LAYER_TYPE_NONE, null); // cannot do
                // other
                ((LinearLayout) findViewById(R.id.topVWin_holder))
                        .setLayerType(View.LAYER_TYPE_HARDWARE, null);
                xWalkWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null); // no
                // effect?
            }

            updateCallDebugStatus(
                    false,
                    "\n>VidConf "
                            + vcRoom
                            + vcRes
                            + "  hw+"
                            + ((SurfaceView) findViewById(R.id.topVWin_surface))
                            .isHardwareAccelerated()
                            + xWalkWebView.isHardwareAccelerated()
                            + "  sv:"
                            + !XWalkPreferences
                            .getBooleanValue(XWalkPreferences.ANIMATABLE_XWALK_VIEW));
            Log.d("vidConfxwalk showing, "
                    + hwAccelOk
                    + ":hw+:"
                    + ((SurfaceView) findViewById(R.id.topVWin_surface))
                    .getLayerType()
                    + ((SurfaceView) findViewById(R.id.topVWin_surface))
                    .isHardwareAccelerated()
                    + " "
                    + ((LinearLayout) findViewById(R.id.topVWin_holder))
                    .getLayerType()
                    + ((LinearLayout) findViewById(R.id.topVWin_holder))
                    .isHardwareAccelerated()
                    + " "
                    + xWalkWebView.getLayerType()
                    + xWalkWebView.isHardwareAccelerated()
                    + " "
                    + "sv:"
                    + !XWalkPreferences
                    .getBooleanValue(XWalkPreferences.ANIMATABLE_XWALK_VIEW));

            mTimerLabel.setVisibility(View.VISIBLE);

            curGroupIndex = groupIndex;

        } catch (Exception e) {
            Log.e("initVidConf !@#$ " + e.getMessage());
        }
    }

    public void endVidConf() {
        try {
            if (xWalkWebView != null) {
                ((SurfaceView) findViewById(R.id.topVWin_surface))
                        .setVisibility(View.GONE);
                ((LinearLayout) findViewById(R.id.topVWin_holder))
                        .removeView(xWalkWebView);
                xWalkWebView.onDestroy();
                xWalkWebView = null;
                Log.e("vidConfxwalk Destroyed");

                mTimerLabel.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e("endVidConf !@#$ " + e.getMessage());
        }
    }

    // ***tml
    // tml*** setCPU2
    Runnable checkCPU = new Runnable() {
        @Override
        public void run() {
            MyUtil.setCPU(false, null, null, null);
            MyUtil.getCPU(true);
            mHandler.postDelayed(checkCPU, 20000);
        }
    };

    List<ChatroomMember> currentVideoMembers;
    private boolean isOpenVideoView;

    /**
     * 重新加载VideoView
     *
     * @param groupIndex 加载视频的成员小组索引，从0开始；拼接在room后面。＜0 时表示加载全部成员video；不在room后面拼接。
     */
    public void notifyOpenVideo(int groupIndex) {
        if (isHost) {
            if (curGroupIndex == groupIndex) {
                Log.i("正是这群鬼，不用再看了！！！");
                return;
            }
//			curGroupIndex = groupIndex;
            reloadVideoView(groupIndex);
            new Thread(new notifyOpenVideoTask()).start();
        }
    }

    public void reloadVideoView(int groupIndex) {
        if (curGroupIndex == groupIndex) return;
        endVidConf();
        initVidConf(groupIndex);
    }

    class notifyOpenVideoTask implements Runnable {

        @Override
        public void run() {

            if (multiMemberList.size() < 1) {
                Log.i("人都没有，查看个蛋啊！！！");
                return;
            }

            //获取要通知的成员。
            List<ChatroomMember> cms = multiMemberList.get(curGroupIndex);

            //通知之前的关闭
            if (currentVideoMembers != null)
                for (ChatroomMember cm : currentVideoMembers) {
                    String address = mADB.getAddressByIdx(cm.getIdx());
                    String content = Global.Call_Conference_Video_Close;
                    sendText(address, content);
                }

            //通知开启视频
            if (cms != null)
                for (ChatroomMember cm : cms) {
                    String address = mADB.getAddressByIdx(cm.getIdx());
                    String content = Global.Call_Conference_Video_Open + "&" + curGroupIndex;
                    sendText(address, content);
                }

            //更新当前开启的Video的成员。
            currentVideoMembers = cms;
        }

    }


    private void endCall() {
        Log.e("voip.HANGUP1 DA *** USER PRESSED ***");
        mPref.write("tempCheckSameIN", 0); // tml*** sametime

        if (mAnswer.getVisibility() == View.VISIBLE) { // tml***
            // beta ui
            updateButtonVisible(mAnswer, View.GONE);
        }

        if (rejectHangingup
                && AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL) {
            Log.e("voip.HANGUP1 DA *** USER PRESSED *** cancelled "
                    + AireVenus.getCallTypeName(AireVenus.getCallType())
                    + rejectHangingup);
            return;
        }

        mStatus.setText(R.string.call_end);
        mHangup.setOnClickListener(null);
        HangingUp = true;
        sendTerminateSignal = true;
        try {
            if (mSpeechRecognizer != null) { // tml*** voice control
                destroyVoice();
            }
            mHandler.removeCallbacks(timeElapsed);
            mHandler.removeCallbacks(run_disp_msg);
            mHandler.removeCallbacks(run_mute);
            mHandler.removeCallbacks(addPSTNInfoHide);

            if (AireVenus.instance() != null) {
                VoipCore lVoipCore = AireVenus.instance().getVoipCore();
                VoipCall myCall = lVoipCore.getCurrentCall();
                if (myCall != null) {
                    lVoipCore.terminateCall(myCall);
                    Log.e("voip.exit.HANGUP1 DA *** USER PRESSED *** OK");
                    return;
                } else {
                    Log.e("voip.HANGUP1 DA *** USER PRESSED *** myCall=null");
                }
            }
        } catch (Exception e) {
            Log.e("mHangup !@#$ " + e.getMessage());
        }
        exitCallMode("mHangup");
    }

//	public void openVideo(int groupIndex) {
//		if(!isHost){
//			curGroupIndex = groupIndex;
//		}
//		if(streamsRunning || isHost)
//			openVideoView(true);
//	}

    /**
     * 当没有人在通话时自动挂断
     */
    private void nullEndCall() {
        if (memberList.size() > 0) {
            isOnceMem = true;
            isNowMem = true;
        } else {
            isNowMem = false;
            Message msg = handler.obtainMessage();
            msg.what = 6;
            handler.sendMessage(msg);

        }
    }

    private void sendText(String address, String content) {
        Log.i("发送消息！address ： " + address + " content:　" + content);
        MyUtil.Sleep(500);
        if (AireJupiter.getInstance() != null
                && AireJupiter.getInstance().tcpSocket() != null) {
            AireJupiter.getInstance().tcpSocket().send(address, content, 0, null, null, 0, null);
        }
    }

    public void openVideoView(boolean isOpen) {

        if (isOpenVideoView == isOpen) return;


        // tml|yang*** setCPU
        if (cpuSet[0] == null) {
            cpuSet = MyUtil.getCPU(true);
            MyUtil.setCPU(false, "performance",
                    null, null);
            MyUtil.getCPU(true);
            // new Thread(checkCPU).start();
            // //tml*** setCPU2
        }

        if (isOpen) {
            showTestDialog(); // tml test
            if (!isMultiMember) {
                initVidConf(-1);
            }
            mHandler.removeCallbacks(showPanel);
            mHandler.removeCallbacks(hidePanel);
            mHandler.postDelayed(hidePanel, 1500);
        } else {
            endVidConf();
            mHandler.removeCallbacks(showPanel);
            mHandler.removeCallbacks(hidePanel);
            mHandler.post(resumePanel);
            mHandler.removeCallbacks(checkCPU); // tml***
            // setCPU2
            if (cpuSet[0] != null
                    && cpuSet[2] != null
                    && cpuSet[3] != null) { // tml|yang***
                // setCPU
                MyUtil.setCPU(true, cpuSet[0],
                        cpuSet[2], cpuSet[3]);
                MyUtil.getCPU(true);
            }
        }
        isOpenVideoView = isOpen;
        ((ToggleButton) findViewById(R.id.video)).setChecked(isOpen);
    }
}