package com.pingshow.amper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.pingshow.AireApp;
import com.pingshow.amper.bean.Group;
import com.pingshow.amper.bean.GroupEntity;
import com.pingshow.amper.bean.GroupMsg;
import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.contacts.RWTOnline;
import com.pingshow.amper.contacts.RelatedUserInfo;
import com.pingshow.amper.db.AireCallLogDB;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.AnnounceDB;
import com.pingshow.amper.db.GroupDB;
import com.pingshow.amper.db.RelatedUserDB;
import com.pingshow.amper.db.SmsDB;
import com.pingshow.amper.db.StudioGroupDB;
import com.pingshow.amper.db.TimeLineDB;
import com.pingshow.amper.db.TransactionDB;
import com.pingshow.amper.db.WTHistoryDB;
import com.pingshow.amper.map.LocationUpdate;
import com.pingshow.amper.message.CmdParser;
import com.pingshow.amper.message.ParseSmsLine;
import com.pingshow.amper.message.PopupDialog;
import com.pingshow.network.MyNet;
import com.pingshow.network.MySocket;
import com.pingshow.network.NetInfo;
import com.pingshow.network.RWTSocket;
import com.pingshow.network.upnpc;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.util.OpenDifferentFile;
import com.pingshow.util.ResizeImage;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;
import com.pingshow.voip.VoipConfigException;
import com.pingshow.voip.VoipException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class AireJupiter extends Service {

    static public String myFafaServer_default = "74.3.164.16";
    static public String myPhpServer_default = "42.121.54.216";
    static public String myPhpServer_default2A = "42.121.54.216";
    static public String myPhpServer_default2B = "php.xingfafa.com.cn";
    static public String myLocalPhpServer = "1.34.148.152";
    static public String mySipServer_default = "1.34.148.152";
    static public String myPhpServer_main = "115.29.185.116";
    static public String myPhpServer_Xingfafa = "42.121.54.216";
    static public String myPhpServer_PS = "74.3.165.158";
    static public String myConfSipServer_default = "96.44.173.84";
    static public String myConfServer_China = "115.29.234.27";

    static public String myAcDomain_default = "www.pingshow.net";
    static public String myAcDomain_USA = "www.pingshow.net";
    static public String myAcDomain_China = "airecenter.xingfafa.com.cn";

    private final int TIMER_6_MIN_INTERVAL = 300000; // 5 mins  //tml|bj*** neverdie/
    private final int TIMER_8_MIN_INTERVAL = 300000; // 5 mins

    static public String myPhoneNumber;
    static public String myPasswd;

    static public String myPhpServer = myPhpServer_default;
    public String mySipServer = mySipServer_default;
    public String myFafaServer = myFafaServer_default;

    private MyPreference mPref;
    private ContactsQuery cq;
    private AmpUserDB mADB;
    private RelatedUserDB mRDB;
    private WTHistoryDB mWTDB;
    private SmsDB mSmsDB;
    public Handler mHandler = new Handler();
    private Bundle callLogBundle;

    private ArrayList<String> unanswered = new ArrayList<String>();
    public boolean calleeGotCallRequest;

    private PendingIntent pendingInt;

    private LocationUpdate mLocation;

    private SMS msgGot;
    private String msgContent = "";
    private NotificationManager mNM;

    public boolean attemptCall = false;
    private String tmpAddress = "";
    private String UnknownAddress = "";
    private int UnknownIdx;
    private boolean AnnoyingUser;

    private String md5 = "";
    private int versionCode;
    private int myIdx;

    //	public static MySocket tcpSocket = null;
    public MySocket tcpSocket = null;  //tml*** static socket
    public static RWTSocket rwtSocket = null;
    public boolean unautherized999 = false;
    private int freqDivided = 0;
    private int paypalRetries = 0;

    private boolean checkonce = true;

    public static boolean notifying = false;
    private systemNumberChange nbc;
    private ArrayList<String> idxs;
    private GroupDB mGDB;
    private List<GroupEntity> groups;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static public AireJupiter instance;

    static public AireJupiter getInstance() {
        return instance;
    }

    public MySocket tcpSocket()  //alex|tml push, was missing return tcpSocket
    {
        return tcpSocket;
    }

    public boolean isLogged() {
        if (tcpSocket != null)
            return tcpSocket.isLogged(false);
        return false;
    }

    public boolean isTcpOk() {
        if (tcpSocket != null)
            return tcpSocket.getTcpStatus();
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("*** !!! AIREJUPITER *** START START !!! ***");

        String thisManuf = Build.MANUFACTURER.toLowerCase();
        String thisBrand = Build.BRAND.toLowerCase();
        String thisDevice = Build.DEVICE.toLowerCase();
        String thisModel = Build.MODEL.toLowerCase();
        String thisProduct = Build.PRODUCT.toLowerCase();
        String thisSerial = Build.SERIAL.toLowerCase();
        String thisAID = Secure.getString(getContentResolver(), Secure.ANDROID_ID).toLowerCase();
        int thisSDK = Build.VERSION.SDK_INT;
        String thisRV = Build.VERSION.RELEASE.toLowerCase();
        String thisCpuset1 = Build.CPU_ABI;
        String thisCpuset2 = Build.CPU_ABI2;
        Log.e("mnf:" + thisManuf + "|brnd:" + thisBrand + "|dvc:" + thisDevice + "|mdl:" + thisModel + "|prod:" + thisProduct
                + "|ser:" + thisSerial + "|id:" + thisAID + "|sdk:" + thisSDK + "|os:" + thisRV
                + "|cpu1:" + thisCpuset1 + "|cpu2:" + thisCpuset2);

        instance = this;
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

//		boolean isDebuggable =  ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
//		Log.e("debug mode=" + isDebuggable + BuildConfig.DEBUG);

        mPref = new MyPreference(this);
        cq = new ContactsQuery(this);

        mADB = new AmpUserDB(this);// alec
        mADB.open();

        mRDB = new RelatedUserDB(this);
        mRDB.open();

        mWTDB = new WTHistoryDB(this);
        mWTDB.open();

        mSmsDB = new SmsDB(AireJupiter.this);
        mSmsDB.open();

        mGDB = new GroupDB(AireJupiter.this);
        mGDB.open();


        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mPref.writeLong("last_dlf_status", 0);
        mPref.write("myBrandDeviceModelProduct", thisBrand + "," + thisDevice + "," + thisModel + "," + thisProduct);

        //md5 = checkApk();

        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
        }

        // alec
        Thread thread_getServers = new Thread(new Runnable() {
            public void run() {
                getServers();
                new_tcp_socket();
                addAmperHelpers();
                try {
                    onReconnect(startConnection_beginning);
                    buildAlarmReceiver();
                } catch (Exception e) {
                }
                getSipFriendsAcc();
            }
        }, "Servers Stuff");
        thread_getServers.start();

        try {
            nbc = new systemNumberChange(new Handler());
            getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, false, nbc);

            IntentFilter intentToReceiveFilter = new IntentFilter();
            intentToReceiveFilter.addAction(Global.Action_InternalCMD);
            intentToReceiveFilter.addAction(Global.Action_Contact);
            intentToReceiveFilter.addAction(Global.Action_SD_AvailableSpare);
            intentToReceiveFilter.addAction(Global.Action_FileDownload);
            registerReceiver(InternalCommand, intentToReceiveFilter);
        } catch (Exception e) {
            Toast.makeText(this, R.string.no_sdcard, Toast.LENGTH_LONG).show();
        }

        try {
            new File(Global.SdcardPath).mkdir();
            new File(Global.SdcardPath_inbox).mkdir();
            new File(Global.SdcardPath_sent).mkdir();
            new File(Global.SdcardPath_timeline).mkdir();
            new File(Global.SdcardPath_downloads).mkdir();
        } catch (Exception e) {
        }

        try {
            int uid = Integer.valueOf(mPref.read("myID", "0"), 16);
            if (uid != 0) {
                String myselfPhoto = Global.SdcardPath_inbox + "photo_" + uid + "b.jpg";
                File dst = new File(myselfPhoto);
                if (!dst.exists()) {
                    String path = mPref.read("myPhotoPath", null);
                    if (path != null) {
                        File src = new File(path);
                        if (MyUtil.checkSDCard(this) && src.exists()) {
                            MyUtil.copyFile(src, dst, true, this);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }


    private void buildAlarmReceiver() {
        int interval = TIMER_6_MIN_INTERVAL;
        NetInfo ni = new NetInfo(AireJupiter.this);
        if (ni.netType == NetInfo.WIFI) {
            interval = TIMER_8_MIN_INTERVAL;
            Log.d("AlarmReceiver 8-minute interval");
        } else
            Log.d("AlarmReceiver 6-minute interval");

        AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(AireJupiter.this, AmpAlarmReceiver.class);
        pendingInt = PendingIntent.getBroadcast(AireJupiter.this, 0, i, 0);
        mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + interval,
                interval, pendingInt);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(InternalCommand);// alec
        mNM.cancel(R.string.app_name);
        mPref.delect("tempCheckSameIN");  //tml*** sametime
        instance = null;
        if (tcpSocket != null) tcpSocket.disconnect("aj destroy", true);
        if (rwtSocket != null) rwtSocket.disconnect();

        if (m_upnpc != null)
            m_upnpc.release();

        if (mADB != null && mADB.isOpen())
            mADB.close();
        if (mRDB != null && mRDB.isOpen())
            mRDB.close();
        if (mSmsDB != null && mSmsDB.isOpen())
            mSmsDB.close();
        if (mWTDB != null && mWTDB.isOpen())
            mWTDB.close();
        if (mGDB != null && mGDB.isOpen())
            mGDB.close();

        if (mLocation != null)
            mLocation.destroy();

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(pendingInt);

        getContentResolver().unregisterContentObserver(nbc);//alec

        Log.e("*** !!! AIREJUPITER *** DESTROY DESTROY !!! ***");
        super.onDestroy();
    }

    public void onReconnect(Runnable thr) // simon 061011
    {
        Log.d("(2) onReconnect");
        Thread thr_connection = new Thread(thr, "onReconnect");
        thr_connection.start();
    }

    // function called from FafaYou Activity to get all the server IP and stuffs
    public void getServers() {
        Log.d("(1) getServers");

        if (new NetInfo(AireJupiter.this).netType == NetInfo.WIFI) {
            Log.d("(1.1) doUpnp in WiFi LAN");
            m_upnpc = new upnpc(mPref);
            m_upnpc.start();
        }

        myFafaServer = myFafaServer_default;

        myPhoneNumber = mPref.read("myPhoneNumber", "----");
        mySipServer = mPref.read("mySipServer", mySipServer_default);
        myPasswd = mPref.read("password", "1111");

        String domainName = "php.xingfafa.com.cn";
        String iso = mPref.read("iso", "cn");
        try {
            if (!MyUtil.isISO_China(AireJupiter.this, mPref, null))
                domainName = "php.airetalk.org";
            myPhpServer = InetAddress.getByName(domainName).getHostAddress();
        } catch (UnknownHostException e) {
            myPhpServer = myPhpServer_default;
        }
        Log.d("@" + myPhoneNumber + " " + myPasswd + " " + iso + " " + mySipServer + " " + domainName + " " + myPhpServer);
    }

    private upnpc m_upnpc = null;

    Runnable startConnection_beginning = new Runnable() {
        public void run() {
            Log.d("(3) startConnection_beginning");
            OnlineConnection(true);
            checkVersionUpdate(false);
            refreshLocation();
        }
    };

    public void new_tcp_socket() {
        if (tcpSocket != null) {
            Log.e("disconnect from TCP server...");
            tcpSocket.disconnect("aj new socket", true);
            tcpSocket = null;
        }
        tcpSocket = new MySocket(myPhoneNumber, myPasswd, this, mADB, mRDB, cq, myFafaServer, mSmsDB);
    }

    public RWTSocket new_rwt_socket() {
//		if (rwtSocket == null)
//		{
//			rwtSocket = new RWTSocket(myPhoneNumber, myPasswd, AireJupiter.this, mADB, mWTDB);
//		}
//		return rwtSocket;
        //tml|alex*** rwt byebye X
        return null;
    }


    private void OnlineConnection(boolean force) {
        Log.d("(4) OnLineConnection");

        if (!tcpSocket.isLogged(force))
            tcpSocket.Login(versionCode);

        try {
            myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
            if (myIdx == 0)
                tcpSocket.Login(versionCode);
        } catch (Exception e) {
        }

        if (mADB != null && mADB.getCount() <= 2) {
            new Thread(downloadFriendList).start();
            new Thread(downloadGroupList).start();

            if (mPref.readBoolean("firstEnter", false)) {
                mPref.delect("firstEnter");

                if (mPref.readBoolean("permissionReadContacts", true)) {
                    searchFriendsByPhonebook.run();
                    mInstantQueryOnlineFriends.run();
                }

                mHandler.postDelayed(updateFirstLocation, 13000);

                mHandler.postDelayed(popupWelcomeDialog, 41000);
            } else {
                mInstantQueryOnlineFriends.run();
            }

            if (MyUtil.checkSDCard(this)) {
                int myuid = Integer.valueOf(mPref.read("myID", "0"), 16);
                String localfile = Global.SdcardPath_sent + "myself_photo_" + myuid + ".jpg";
                File f = new File(localfile);
                if (!f.exists()) {
                    String remotefile = "profiles/photo_" + myuid + ".jpg";
                    if (downloadAnyPhoto(remotefile, localfile, 3, false))
                        mPref.write("myPhotoPath", localfile);
                }
            }

            mHandler.postDelayed(getFreeTrialCredit, 6000);

            mHandler.postDelayed(queryStudioGroupsFromServer, 9000);

            mHandler.postDelayed(getFreeswitchServiceIP, 12000);

//			mHandler.postDelayed(getConferenceServiceIP,15000);

//			mHandler.postDelayed(searchPossibleFriends,18000);  //tml*** new friends, remove

            mHandler.postDelayed(getLastestAireCallPackages, 21000);

            mHandler.postDelayed(getAnnouncement, 24000);

            mHandler.postDelayed(searchFacebookFriends, 31000);

            mHandler.postDelayed(getRWTServerIP, 36000);
        } else
            mInstantQueryOnlineFriends.run();

        mHandler.postDelayed(getSipCredit, 5000);

        mHandler.postDelayed(getConferenceServiceIP, 15000);

        if (AireVenus.instance() != null) {
            AireVenus.sip_login(mySipServer, myPhoneNumber);
        }

        Log.d("(5) OnLineConnection done");
    }

    private Runnable searchFacebookFriends = new Runnable() {
        public void run() {
            new Thread(new Runnable() {
                public void run() {
                    if (MyUtil.isISO_China(AireJupiter.this, mPref, null)) return;
                    if (!MyUtil.isAppInstalled(AireJupiter.this, "com.facebook.katana")) return;
                    if (!new NetInfo(AireJupiter.this).isConnected()) return;
                    long last = mPref.readLong("facebookFriendsSynchronized", 0);
                    String myFacebookID = mPref.read("myFacebookID", "");

                    long now = new Date().getTime();
                    if (now - last < 72000000) // 20 hours
                        return;// no need to check

                    if (DialerActivity.getDialer() != null) {
                        mHandler.postDelayed(searchFacebookFriends, 30000);
                        return;
                    }

                    if (myFacebookID.length() < 1) {
                        if (!mPref.readBoolean("ProfileCompleted", false)) {
                            mHandler.postDelayed(searchFacebookFriends, 37000);
                            return;
                        }

                        mHandler.post(new Runnable() {
                            public void run() {
                                Intent it = new Intent(AireJupiter.this, CommonDialog.class);
                                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                String title = getString(R.string.searching_friends_desc);
                                it.putExtra("msgContent", title);
                                it.putExtra("numItems", 2);
                                it.putExtra("ItemCaption0", getString(R.string.no));
                                it.putExtra("ItemResult0", CommonDialog.DONTSEARCHFACEBOOK);
                                it.putExtra("ItemCaption1", getString(R.string.yes));
                                it.putExtra("ItemResult1", CommonDialog.SEARCHFACEBOOK);
                                showNotification(title, null, true, R.drawable.icon_sms, null);
                                startActivity(it);
                            }
                        });
                    } else {
                        mHandler.post(new Runnable() {
                            public void run() {
                                Intent it = new Intent(AireJupiter.this, FacebookSearch.class);
                                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(it);
                            }
                        });
                    }
                }
            }).start();
        }
    };
    private Toast mToast;

    public void notifyConnectionChanged() {

        if (!unautherized999) {
            mHandler.removeCallbacks(TryToConnectTCP);
            mHandler.postDelayed(TryToConnectTCP, 8000);
        }

        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if (!tcpSocket.isLogged(false)) {//alec
                    Log.e("notifyConnectionChanged network prompt");
//					if (mToast!=null) {
//						mToast.setText(getString(R.string.network_prompt));
//					}else{
//						mToast=Toast.makeText(getApplicationContext(), getString(R.string.network_prompt), 0);
//					}
//					mToast.show();
                    //tml*** conn notify
                    if (UsersActivity.uiUAinFore || DialerActivity.uiDAinFore) {
                        if (mToast != null) {
                            mToast.setText(getString(R.string.network_prompt));
                        } else {
                            mToast = Toast.makeText(getApplicationContext(),
                                    getString(R.string.network_prompt), Toast.LENGTH_LONG);
                        }
                        mToast.setDuration(Toast.LENGTH_LONG);
                        mToast.setGravity(Gravity.CENTER, 0, 0);
                        mToast.show();
                    }
                    //***tml
                    ContactsOnline.setAllfriendsOffline();
                }

                Intent it2 = new Intent(Global.Action_InternalCMD);
                it2.putExtra("Command", Global.CMD_TCP_CONNECTION_UPDATE);
                sendBroadcast(it2);
            }
        }, 20000);
    }

    public void notifyReconnectTCP() {

        mHandler.removeCallbacks(TryToConnectTCP);
        mHandler.postDelayed(TryToConnectTCP, 500);
    }

    public void notifyReconnectRWTServer() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (PublicWalkieTalkie.getInstance() != null) {
                    PublicWalkieTalkie.getInstance().keepAlive();
                }
            }
        }, 5000);
    }

    final Runnable reconnectDebounce = new Runnable() {
        @Override
        public void run() {
            Thread thr_connection = new Thread(new Runnable() {
                public void run() {

                    if (tcpSocket != null && !tcpSocket.isLogged(false))
                        tcpSocket.Login(versionCode);

                    if (AireVenus.instance() != null) {
                        if (AireVenus.destroying) {
                            mHandler.postDelayed(mEndupServiceY, 3000);
                        }
                        AireVenus.sip_login(mySipServer, myPhoneNumber);
                        AireVenus.instance().renableCodec(false);//alec
                        //mHandler.postDelayed(mEndupServiceY,3000);
                    }

                    if (tcpSocket != null && tcpSocket.isLogged(false)) {
                        tcpSocket.queryFriendsOnlineStatus("reconnectDebounce");
                        sendPendingSMS();
                        mHandler.post(refreshLocation);//alec, once if new connectivity comes, update location...
                    }

                    if (new NetInfo(AireJupiter.this).netType == NetInfo.WIFI) {
                        m_upnpc = new upnpc(mPref);
                        m_upnpc.start();
                    } else {
                        mPref.write("audio_local_port", 0);
                        mPref.write("video_local_port", 0);
                    }

                    try {
                        if (rwtSocket == null)
                            new_rwt_socket();
                        if (rwtSocket != null) {
                            rwtSocket.Login();
                            rwtSocket.queryFriendsOnlineStatus();
                        }
                    } catch (Exception e) {
                    }

                    AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                    am.cancel(pendingInt);
                    buildAlarmReceiver();
                }
            }, "onReconnect");
            thr_connection.start();
        }
    };

    public void refreshLocation() {
        mHandler.post(refreshLocation);
    }

    public void ReleaseLocationMoniter() {
        if (mLocation != null)
            mLocation.destroy();
    }

    Runnable refreshLocation = new Runnable() {
        @Override
        public void run() {
            if (mLocation != null)
                mLocation.destroy();
            mLocation = new LocationUpdate(instance, mHandler, tcpSocket, mPref, true);
        }
    };

    @SuppressWarnings("unused")
    public String checkApk() {
        String s = null;
        try {
            File f;
            int r = 0;
            String filename = "/mnt/asec/com.pingshow.amper/pkg.apk";
            while ((f = new File(filename)) == null || !f.exists()) {
                r++;
                if (r > 4) break;
                filename = "/mnt/asec/com.pingshow.amper-" + r + "/pkg.apk";
                Log.d(filename);
            }

            if (!f.exists()) {
                r = 0;
                filename = "/data/app/com.pingshow.amper.apk";
                while ((f = new File(filename)) == null || !f.exists()) {
                    r++;
                    if (r > 4) break;
                    filename = "/data/app/com.pingshow.amper-" + r + ".apk";
                    Log.d(filename);
                }
            }

            if (!f.exists())
                return s;

            InputStream is = new FileInputStream(f);

            byte[] buffer = new byte[8192];

            final char hexDigits[] = {'a', '1', 'e', 'c', 'f', '0', 'b', '2', '3', 'd', '4', '5', '6', '7',
                    '8', '9'};
            java.security.MessageDigest md = java.security.MessageDigest
                    .getInstance("MD5");
            char str[] = new char[16 * 2];
            int readBytes = 0;
            while ((readBytes = is.read(buffer)) != -1) {
                md.update(buffer);
                byte tmp[] = md.digest();
                int k = 0;
                for (int i = 0; i < 16; i++) {
                    byte byte0 = tmp[i];
                    str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                    str[k++] = hexDigits[byte0 & 0xf];
                }
            }
            s = new String(str);
        } catch (Exception e) {
        }
        return s;
    }

    public void do8mConnection() {
        if (tcpSocket != null) {
            do10mJobs.run();
        } else {
            Log.e("tcpSocket NULL: no Jobs");
        }
    }

    final Runnable do10mJobs = new Runnable() {
        @Override
        public void run() {

            Log.d("Do... 10 min jobs");

            if (tcpSocket.isLogged(false)) {
                if (!tcpSocket.queryFriendsOnlineStatus("do10mJobs")) {
                    mHandler.removeCallbacks(TryToConnectTCP);
                    mHandler.postDelayed(TryToConnectTCP, 3000);
                }
            } else {
                int c = 0;
                while (!tcpSocket.isLogged(false) && ++c < 4) {
                    Log.e("do10mJobs, not logged...");
                    if (tcpSocket.disconnect("10m job", true)) {
                        NetInfo ni = new NetInfo(AireJupiter.this);
                        if (ni.isConnected()) {
                            if (tcpSocket.Login(versionCode)) {
                                tried = 0;
                                return;
                            }
                        }
                    }
                    MyUtil.Sleep(30000);
                }
            }

            if (PublicWalkieTalkie.getInstance() == null && WalkieTalkieDialog.getInstance() == null) {
                if (rwtSocket != null)
                    rwtSocket.disconnect();
            } else if (PublicWalkieTalkie.getInstance() != null) {
                if (rwtSocket != null)
                    PublicWalkieTalkie.getInstance().keepAlive();
            } else if (WalkieTalkieDialog.getInstance() != null) {
                if (rwtSocket != null)
                    WalkieTalkieDialog.getInstance().keepAlive();
            }
        }
    };

    public void do30mConnection() {
        if (tcpSocket != null)
            do30mJobs.run();
    }

    public final Runnable do30mJobs = new Runnable() {
        @Override
        public void run() {
            if (tcpSocket == null) return;

            Log.d("Do... 30 min jobs");
            tcpSocket.keepAlive(true);  //tml*** keepalive/

            if (DialerActivity.getDialer() != null) return;
            if (!new NetInfo(AireJupiter.this).isConnected()) return;

            freqDivided++;

            sendPendingSMS();
            onReceiveOfflineMessage.run();

            if ((freqDivided % 15) == 4) // 7.5 hours
            {
                getRWTServerIP.run();
            }

            if ((freqDivided % 2) == 0) // 1.5 hours
            {
                getSipCredit.run();
                getFriendNicknames();
            }

            if ((freqDivided % 4) == 2) // 2 hours
            {
                resendPendingPayments.run();
            }

            if ((freqDivided % 13) == 1) // 6.5 hours
            {
                searchFriendsByPhonebook.run();
            }

            if ((freqDivided % 24) == 2) //10 hours
            {
//				searchPossibleFriends.run();  //tml*** new friends, remove
            }

            if ((freqDivided % 10) == 0)//5 hours
            {
                downloadBigPhotoFromNet();
            }

            if ((freqDivided % 11) == 0) // 5.5 hours
            {
                doCheckPhotoFromNet();
            }

            if ((freqDivided % 12) == 0) // 6 hours
            {
                checkVersionUpdate(false);
//				getFriendNicknames();
            }

            if ((freqDivided % 6) == 0) //6.5 hours
            {
                getFriendMoods();
            }

            if ((freqDivided % 5) == 0) // 2.5 hours
            {
                if (mPref.read("myPhotoPath") != null && !mPref.readBoolean("myPhotoUploaded", false)) {
                    Intent it = new Intent(Global.Action_InternalCMD);
                    it.putExtra("Command", Global.CMD_UPLOAD_PROFILE_PHOTO);
                    sendBroadcast(it);
                }
            }

            if ((freqDivided % 5) == 0)//8 hours
            {
                getSipFriendsAcc();
            }

            if ((freqDivided % 16) == 1)//8 hours
            {
                uploadAllFriends();
            }

            if ((freqDivided % 18) == 2)//9 hours
            {
                clearSDCard.run();
            }

            if ((freqDivided % 20) == 1) // 10 hours
            {
                getAnnouncement.run();
            }

            if ((freqDivided % 24) == 0)//24 hours
            {
                //searchFacebookFriends.run();//alec: no need to bother users

                try {
                    myPhpServer = InetAddress.getByName(myPhpServer_default).getHostAddress();
                    Log.d("myPhpServer=" + myPhpServer);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

                mPref.write("myIpAddress", "");
                mPref.write("myGeoLocation", "");
            }

            if ((freqDivided % 72) == 0)//36 hours
            {
                getLastestAireCallPackages.run();
            }

            if ((freqDivided % 24) == 0)//18 hours
            {
                getFreeswitchServiceIP.run();
            }

            if ((freqDivided % 5) == 0)//19 hours, 38
            {
                getConferenceServiceIP.run();
            }
        }
    };

    final Runnable TryToConnectTCP = new Runnable() {
        public void run() {
            unautherized999 = true;
            Thread thr = new Thread(mInstantQueryOnlineFriends,
                    "Update Friend Status");
            thr.start();
        }
    };

    int tried = 0;

    final Runnable mInstantQueryOnlineFriends = new Runnable() {
        public void run() {
//			if (tcpSocket == null) return;
            if (tcpSocket == null) {  //tml*** keepalive
                Log.e("mInstantQueryOnlineFriends ***** tcpSocket NULL *****");
//				restartService(instance, 1000);
//				stopSelf();
                return;
            }

            Thread queryFriends_thread = new Thread(new Runnable() {
                public void run() {

                    if (tcpSocket == null) return;

                    if (!tcpSocket.isLogged(false)) {
                        tried++;
                        if (tcpSocket.Login(versionCode))
                            tried = 0;

                        Log.e("***** tcpSocket.Login reconnect ***** #" + tried);
                    }

                    if (unautherized999) return;

                    if (!tcpSocket.queryFriendsOnlineStatus("queryFriends_thread")) {
                        if (tried < 4) {
                            mHandler.removeCallbacks(TryToConnectTCP);
                            mHandler.postDelayed(TryToConnectTCP, 30000);
                        } else if ((tried % 20) == 4) {
                            getServers();
                            if (!tcpSocket.isLogged(false)) {
                                if (tcpSocket.disconnect("fail fonline still", true))
                                    if (tcpSocket.Login(versionCode))
                                        tried = 0;
                            }
                        }
                    }
                }
            }, "queryFriends_thread");
            queryFriends_thread.start();

            System.gc();
            System.gc();
        }
    };

    public void startServiceY(int mode) {
        mHandler.removeCallbacks(mEndupServiceY);
        attemptCall = true;

        if (AireVenus.instance() != null) {
//			if (AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL)
            if (AireVenus.getCallType() != mode)  //tml*** recheck venus
            {
                if (!AireVenus.instance().inCall) {
                    Log.e("!!! STOPPING AireVenus/ServiceY *** AJ @ voip.startServiceY CallType !=");
                    Intent itx = new Intent(AireJupiter.this, AireVenus.class);
                    stopService(itx);
                    MyUtil.Sleep(3000);
                }
            }
        }

        AireVenus.setCallType(mode);

        if (AireVenus.instance() == null) {

//			Log.e("--------- SWITCH CALL --------- venus restarting");
            if (mPref.readBoolean("doingUPNP", false)) {
                Log.d("doingUPNP, wait for 3 sec");
                MyUtil.Sleep(3000);
            }
            android.os.Process.setThreadPriority(-19);
            Intent VoipIntent = new Intent(Intent.ACTION_MAIN);
            VoipIntent.setClass(AireJupiter.this, AireVenus.class);
            startService(VoipIntent);
        } else {
            if (AireVenus.destroying)
                mHandler.removeCallbacks(mEndupServiceY);
            AireVenus.instance().forceRegister();
        }
    }

    private String curCalleeAddress = "";

    public String getYourSipServer(String CalleeAddress) {
//		String sip_ip = mySipServer;
        curCalleeAddress = CalleeAddress;
        String sip_ip = mPref.read("mySipServer", mySipServer);  //tml*** xcountry sip
//		String ipTmp = ContactsOnline.getContactSipIP(CalleeAddress);
//		if (ipTmp != null) {
//			Log.d("voip.getYourSipServer getContactSipIP " + ipTmp);
//			r eturn ipTmp;
//		}
        if (tcpSocket == null || !tcpSocket.isLogged(false)) {
            Log.d("voip.getYourSipServer tcpSocket !@#$");
            return mySipServer;
        }

        sip_ip = tcpSocket.tcpGetCalleeSip(CalleeAddress);
        //tml*** xcountry sip
//		long now = new Date().getTime();
//		long last = mPref.readLong("last_getCalleeSip", 0);
//		if (now - last > 10800000) {  //3hrs
//			sip_ip = tcpSocket.tcpGetCalleeSip(CalleeAddress);
//			mPref.writeLong("last_getCalleeSip", now);
//			Log.d("voip.getYourSipServer tcpGetCalleeSip " + sip_ip);
//		} else {
//			String ipTmp = ContactsOnline.getContactSipIP(CalleeAddress);
//			mPref.writeLong("last_getCalleeSip", now);
//			if (ipTmp != null && ipTmp.length() > 0) {
//				Log.d("voip.getYourSipServer getContactSipIP " + ipTmp);
//				return ipTmp;
//			}
//		}

        if (sip_ip == null) {
            Log.d("voip.getYourSipServer tcpGetSip !@#$");
            return mySipServer;
        } else {
            Log.d("voip.getYourSipServer " + CalleeAddress + "@" + sip_ip);
        }
        ContactsOnline.addContactSipIP(CalleeAddress, sip_ip);
        return sip_ip;
    }

    Runnable terminate_call_by_tcp = new Runnable() {
        public void run() {
            if (attemptCall) {
                return;
            }
            if (tcpSocket != null && tcpSocket.isLogged(false))
                tcpSocket.sendTerminateCommand(tmpAddress);
        }
    };

    public void terminateCallBySocket(String address) {
        tmpAddress = address;
        mHandler.postDelayed(terminate_call_by_tcp, 2000);
    }

    public void StopEndingupServiceY() {
        mHandler.removeCallbacks(mEndupServiceY);
    }

    final Runnable mEndupServiceY = new Runnable() {
        public void run() {

            if (attemptCall) {
                return;
            }

            if (DialerActivity.getDialer() == null || AireVenus.runAsSipAccount)//alec
            {
                if (MyUtil.CheckServiceExists(AireJupiter.this, "com.pingshow.voip.AireVenus")) {
                    Log.e("!!! STOPPING AireVenus/ServiceY *** AJ @ voip.callend||reconnect :: NULLdialer||runasSipAcc");
                    Intent VoipIntent = new Intent(AireJupiter.this, AireVenus.class);
                    stopService(VoipIntent);
                }
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
            }

            if (unanswered.size() > 0) {
                //tml|alex*** unnecessary missed call msg
//				if (!AireVenus.runAsSipAccount)
//				{
//					for (int i = 0; i < unanswered.size(); i++) {
//						int n = 1;
//						String callee = unanswered.get(i);
//						int idx=mADB.getIdxByAddress(callee);
//						SendAgent agent = new SendAgent(AireJupiter.this, myIdx, idx, true);
//						
//						for (int j = i + 1; j < unanswered.size(); j++) {
//							if (callee.equals(unanswered.get(j)))
//								n++;
//						}
//						unanswered.remove(callee);
//						agent.onSend(callee, "[<MISSEDREMIND>]" + n, 0, null, null,
//								true);
//					}
//				}
                unanswered.clear();
            }

            System.gc();
            System.gc();
        }
    };


    private long calleeContact_id;
    private String calleeNumber;
    private boolean MakeVideoCall = false;
    private String mDisplayname = null;
    private long mRow_id;
    private int mGroupID;
    private String mGroupName;

    BroadcastReceiver InternalCommand = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (intent.getAction().equals(Global.Action_InternalCMD)) {
                int command = intent.getIntExtra("Command", 0);
                switch (command) {
                    case Global.CMD_SEARCH_POSSIBLE_FRIENDS:
                        new Thread(new Runnable() {
                            public void run() {
//							MyUtil.Sleep(5000);
//							searchPossibleFriends.run();  //tml*** new friends, remove
                            }
                        }).start();
                        break;
                    case Global.CMD_STRANGER_COMING:
                        UnknownAddress = intent.getStringExtra("Address");
                        UnknownIdx = intent.getIntExtra("Idx", 0);
                        AnnoyingUser = intent.getBooleanExtra("Annoying", false);
                        Log.i("addF.Unknown addr/idx/annoy=" + UnknownAddress + " " + UnknownIdx + " " + AnnoyingUser);
//					if (UnknownAddress == null || UnknownIdx <= 0) return;
//					mADB.insertStranger(UnknownAddress, UnknownIdx);
//					mRDB.insertUser(UnknownAddress, UnknownIdx);
//					if (AddAsFriendActivity.getInstance()!=null) {
//						if (AddAsFriendActivity.getTopAddress().equals(UnknownAddress)) {
//							return;
//						}
//					}
//					UsersActivity.forceRefresh=true;
                        //tml*** getuserinfo
                        if (UnknownIdx <= 0) {
                            Log.e("addF !@#$ UnknownIdx<=0");
                            return;
                        }
                        if (!TextUtils.isEmpty(UnknownAddress)) {
                            mADB.insertStranger(UnknownAddress, UnknownIdx);
                            mRDB.insertUser(UnknownAddress, UnknownIdx);
                            if (AddAsFriendActivity.getInstance() != null) {
                                if (AddAsFriendActivity.getTopAddress().equals(UnknownAddress)) {
                                    Log.d("addF AddAsFriendActivity.getTopAddress()");
                                    return;
                                }
                            }
                        }
                        UsersActivity.forceRefresh = true;
                        //***tml

                        (new Thread(new Runnable() {
                            public void run() {
//							String nickname=mRDB.getNicknameByAddress(UnknownAddress);
//							String Return="";
//							if (nickname.length()==0||nickname.equals("Stranger?"))
//							{
//								int c=0;
//								do{
//									MyNet net = new MyNet(AireJupiter.this);
//									Return = net.doPost("getusernickname.php","idx="+Integer.toHexString(UnknownIdx),null);
//									if (Return.length()>5) break;
//									MyUtil.Sleep(500);
//								}while(c++<3);
//							}
//							else{
//								Return="Done="+nickname;
//							}
                                //tml*** getuserinfo
                                boolean getuserinfo = false;
                                String Return = "";
                                String nickname = "";
                                if (!TextUtils.isEmpty(UnknownAddress)) {
                                    nickname = mRDB.getNicknameByAddress(UnknownAddress);
                                    if (nickname.length() == 0 || nickname.equals("Stranger?"))
                                        getuserinfo = true;
                                } else {
                                    getuserinfo = true;
                                }

                                if (getuserinfo) {
                                    int c = 0;
                                    int myidx = Integer.parseInt(mPref.read("myID", "0"), 16);
                                    String mypw = mPref.read("password", "1111");
                                    MyNet net = new MyNet(AireJupiter.this);
                                    do {
                                        Return = net.doPost("getuserinfo.php", "idx=" + UnknownIdx
                                                + "&id=" + myidx + "&password=" + mypw, null);
                                        if (Return.length() > 5) break;
                                        MyUtil.Sleep(500);
                                    } while (c++ < 3);
                                } else {
                                    Return = "Done=" + UnknownAddress + "<Z>" + nickname;
                                }
                                //***tml

                                String localfile = Global.SdcardPath_inbox + "photo_" + UnknownIdx + ".jpg";
                                File f = new File(localfile);
                                if (!f.exists()) downloadPhoto(UnknownIdx, localfile);

                                if (Return.length() > 5 && Return.startsWith("Done=")) {
//								nickname=Return.substring(5);
//								mADB.updateNicknameByUID(UnknownIdx, nickname);
//								mRDB.updateNicknameByUID(UnknownIdx, nickname);
                                    //tml*** getuserinfo
                                    Return = Return.replace("Done=", "");
                                    if (!Return.contains("<Z>")) {
                                        Log.e("addF non-<Z> !@#$ getuserinfo.php");
                                        return;
                                    }
                                    String gotUserInfo[] = Return.split("<Z>");
                                    if (gotUserInfo.length < 2) {
                                        Log.e("addF split<2 !@#$ getuserinfo.php");
                                        return;
                                    }
                                    UnknownAddress = gotUserInfo[0];
                                    nickname = gotUserInfo[1];
                                    if (TextUtils.isEmpty(UnknownAddress) || TextUtils.isEmpty(nickname)) {
                                        Log.e("addF get error !@#$ getuserinfo.php");
                                        return;
                                    }
                                    if (!mADB.isFafauser(UnknownAddress) || !mADB.isFafauser(UnknownIdx))
                                        mADB.insertStranger(UnknownAddress, UnknownIdx);
                                    if (!mRDB.isFafauser(UnknownAddress) || !mRDB.isFafauser(UnknownIdx))
                                        mRDB.insertUser(UnknownAddress, UnknownIdx);
                                    if (mADB.isFafauser(UnknownIdx))
                                        mADB.updateNicknameByUID(UnknownIdx, nickname);
                                    if (mRDB.isFafauser(UnknownIdx))
                                        mRDB.updateNicknameByUID(UnknownIdx, nickname);
                                    //***tml
                                    if (!mPref.readBoolean("BlockStrangers", false))//If blocking strangers
                                    {
                                        //jack 2.4.51版本
                                        if (mADB.isUserDeleted(UnknownAddress)) {
                                            Intent it = new Intent(AireJupiter.this, AddAsFriendActivity.class);
                                            //判断是删除的了用户
                                            android.util.Log.d("AireJupiter", "mADB.isUserDeleted(UnknownAddress):" + mADB.isUserDeleted(UnknownAddress));
                                            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            it.putExtra("Address", UnknownAddress);
                                            it.putExtra("Idx", UnknownIdx);
                                            it.putExtra("Nickname", nickname);
                                            it.putExtra("Stranger", 1);
                                            it.putExtra("Annoying", AnnoyingUser);

                                            if (DialerActivity.getDialer() == null) {
                                                startActivity(it);
                                            }
                                            showNotification(String.format(getString(R.string.accept_this_stranger), nickname),
                                                    it, true, R.drawable.icon_sms, null);
                                        }


                                    } else {
                                        Log.e("addF !@#$ BlockingStrangers");
                                    }
                                } else {
                                    Log.e("addF !@#$ getuserinfo.php");
                                    if (!TextUtils.isEmpty(UnknownAddress)) {  //tml*** getuserinfo
                                        mADB.deleteContactByAddress(UnknownAddress);
                                        mRDB.deleteContactByAddress(UnknownAddress);
                                    }
                                }

                                Log.d("addF.Done addr/idx/annoy=" + UnknownAddress + " " + UnknownIdx + " " + AnnoyingUser);
                                Intent intent = new Intent(Global.Action_Refresh_Gallery);
                                sendBroadcast(intent);
                            }
                        })).start();
                        break;
                    case Global.CMD_TCP_MESSAGE_ARRIVAL:
                        String originalSignal = intent.getStringExtra("originalSignal");
                        if (originalSignal.startsWith("860")) {
                            processIncomingGroupSMS(originalSignal);
                        } else {
                            processIncomingSMS(originalSignal);
                        }
                        break;
                    case Global.CMD_TCP_COMMAND_ARRIVAL:

                        String cmdStr = intent.getStringExtra("cmdStr");
                        new CmdParser(AireApp.context).parseCmd(cmdStr);

                        break;
                    case Global.CMD_TRIGGER_SENDEE:
                        new Thread(new Runnable() {
                            public void run() {
                                if (tcpSocket != null) {
                                    if (!tcpSocket.isLogged(false))
                                        tcpSocket.Login(versionCode);

                                    mRow_id = intent.getLongExtra("row_id", 0);
                                    String Sendee = intent.getStringExtra("Sendee");
                                    int GroupID = intent.getIntExtra("GroupID", 0);
                                    String msgText = intent.getStringExtra("MsgText");
                                    Log.d("msg CMD_TRIGGER_SENDEE:" + Sendee + "/" + GroupID + " " + mRow_id);
                                    boolean SMSoK = tcpSocket.send(
                                            Sendee,
                                            msgText,
                                            intent.getIntExtra("Attached", 0),
                                            intent.getStringExtra("remoteAudioPath"),
                                            intent.getStringExtra("remoteImagePath"),
                                            mRow_id,
                                            intent.getStringExtra("phpIP"), GroupID);
                                } else {
                                    Log.e("msg CMD_TRIGGER_SENDEE tcpSocket !@#$");
                                }
                            }
                        }).start();
                        break;
//                    case Global.CMD_GROUP_SENDEE://jack,暂时弃用
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (tcpSocket!=null){
//                                    if (!tcpSocket.isLogged(false))
//                                        tcpSocket.Login(versionCode);
//                                    // TODO: 2016/3/29 发送group msg
//                                    mRow_id = intent.getLongExtra("row_id", 0);
//                                    int GroupID = intent.getIntExtra("GroupID", 0);
//                                    String attachmentURL = intent.getStringExtra("attachmentURL");
//                                    String msgText = intent.getStringExtra("MsgText");
//                                    boolean SMSoK = tcpSocket.send850(Integer.toHexString(GroupID), "{\"content\":" + msgText + ",\"attachmentURL\":"+attachmentURL+"}");
//                                    Log.d("msg CMD_GROUP_SENDEE:" +GroupID + " " + mRow_id+"----"+SMSoK);
//                                }else {
//                                    Log.e("msg CMD_GROUP_SENDEE tcpSocket !@#$");
//                                }
//                            }
//                        }).start();
//                        break;
                    case Global.CMD_ADDF_280:
                        //not used yet
                        break;
                    case Global.CMD_JOIN_A_NEW_GROUP://alec
                        mGroupID = intent.getIntExtra("GroupID", 0);
                        final boolean empty = intent.getBooleanExtra("Empty", false);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                boolean deleteResult = true;
                                //群组存在的话,清除Group和组员
                                if (!empty) {
                                    deleteResult = mGDB.deleteGroup(mGroupID);
                                    android.util.Log.d("AireJupiter", "deleteResult:" + deleteResult + "adasdasdasd");
                                }
                                if (deleteResult) {
                                    //查询组员(call php),并插入数据库(insert db)
                                    queryGroupPhpAndInsertDB(mGroupID, mGDB, mADB);

                                }

                            }
                        }).start();
//
//                                        String localfile = Global.SdcardPath_inbox + "photo_" + (mGroupID + 100000000) + ".jpg";
//                                        File f = new File(localfile);
//                                        if (!f.exists()) {
//                                            String remotefile = "groups/photo_" + mGroupID + ".jpg";
//                                            downloadAnyPhoto(remotefile, localfile, 3, true);
//                                        }
//
//                                        //handle unknown members:
//                                        for (int i = 0; i < m.length; i++) {
//                                            int idx = Integer.parseInt(m[i]);
//                                            if (idx == myIdx) continue;
//                                            if (!mADB.isFafauser(idx)) {
//                                                if (tcpSocket.isLogged(false))
//                                                    tcpSocket.queryUserAddressByIdx(idx);//it will popup stranger dialog
//                                                MyUtil.Sleep(3000);
//                                            }
//                                        }

                        //jack 2.4.51 不再弹出加好友对话框
//                                        Intent it = new Intent(AireJupiter.this, JoinNewGroupActivity.class);
//                                        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                        it.putExtra("Address", "[<GROUP>]" + mGroupID);
//                                        it.putExtra("Idx", mGroupID + 100000000);
//                                        it.putExtra("Nickname", mGroupName);
//                                        it.putExtra("Creator", creator);
//                                        it.putExtra("GroupId", mGroupID);

//                                        if (DialerActivity.getDialer() == null)
//                                            startActivity(it);
//
//                                        showNotification(String.format(getString(R.string.add_in_group), creator, mGroupName),
//                                                it, true, R.drawable.icon_sms, null);

                        break;
                    case Global.CMD_JOIN_A_NEW_GROUP_VERIFIED://alec
                        mGroupID = intent.getIntExtra("GroupID", 0);
                        mGroupName = intent.getStringExtra("GroupName");
                        new Thread(new Runnable() {
                            public void run() {
                                mADB.insertUser("[<GROUP>]" + mGroupID, mGroupID + 100000000, mGroupName);

                                UsersActivity.needRefresh = true;

                                Intent intent = new Intent(Global.Action_Refresh_Gallery);
                                sendBroadcast(intent);
                            }
                        }).start();
                        break;
                    case Global.CMD_LEAVE_GROUP://some member leaves the group, and he notifies me.
                        mGroupID = intent.getIntExtra("GroupID", 0);
                        UnknownIdx = intent.getIntExtra("idx", 0);
                        idxs = intent.getStringArrayListExtra("idxs");
                        final String nameBuffer = intent.getStringExtra("nameBuffer");
                        // TODO: 2016/4/6 删除
//                        final int rowid = intent.getIntExtra("rowid", 0);
                        if (UnknownIdx > 0) {
                            new Thread(new Runnable() {
                                public void run() {
                                    GroupDB gdb = new GroupDB(AireJupiter.this);
                                    gdb.open();
                                    gdb.deleteGroupMember(mGroupID, UnknownIdx);
                                    int c = gdb.getGroupMemberCount(mGroupID);

                                    if (c == 0) {
                                        mADB.deleteContactByAddress("[<GROUP>]" + mGroupID);
                                        UsersActivity.needRefresh = true;
                                        Intent intent = new Intent(Global.Action_Refresh_Gallery);
                                        sendBroadcast(intent);

                                        gdb.deleteGroup(mGroupID);

                                        try {
                                            mSmsDB.deleteThreadByAddress("[<GROUP>]" + mGroupID);
                                        } catch (Exception e) {
                                        }
                                    }
                                    gdb.close();
                                }
                            }).start();
                        }
                        //jack 2.4.51
                        if (idxs!=null&&idxs.size() > 0) {
                            //在子线程中操作
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    //组合网络请求数据
                                    StringBuffer members = new StringBuffer("");
                                    for (int i = 0; i < idxs.size(); i++) {
                                        if (i > 0) members.append(",");
                                        members.append(idxs.get(i));
                                    }
                                    // TODO: 2016/3/28 请求服务器删除这些好友
                                    String Return = "";
                                    try {

                                        android.util.Log.d("删除组成员", "组ID:" + mGroupID);

                                        MyNet net = new MyNet(AireJupiter.this);
                                        Return = net.doPostHttps("remove_group_member.php", "id=" + mGroupID
                                                + "&members=" + members.toString(), null);
                                    } catch (Exception e) {
                                    }
                                    // TODO: 2016/3/28   从数据库中删除这些好友
                                    if (Return.startsWith("Done")) {

                                        GroupDB gdb = new GroupDB(AireJupiter.this);
                                        gdb.open();
                                        android.util.Log.d("删除组成员", "组成员开始: "+gdb.getGroupMembersByGroupIdx(mGroupID).toString());
                                        for (String idx : idxs) {
                                            boolean result = gdb.deleteGroupMember(mGroupID, Integer.parseInt(idx));
                                        }
                                        android.util.Log.d("删除组成员", "组成员结果: " + gdb.getGroupMembersByGroupIdx(mGroupID).toString());

                                        // TODO: 2016/4/5 发送TCP删除好友,往数据库中插入删除好友信息数据
                                        SendAgent agent = new SendAgent(AireJupiter.this, myIdx, 0, true);
                                        agent.setAsGroup(mGroupID);
                                        // TODO: 2016/4/6 将加人消息写入数据库
                                        String obligate1 = null;
                                        if (AireJupiter.getInstance() != null) {  //tml*** china ip
                                            obligate1 = AireJupiter.getInstance().getIsoPhp(0, true, null);
                                        } else {
                                            obligate1 = AireJupiter.myPhpServer_default;
                                        }
                                        String address = "[<GROUP>]" + mGroupID;

                                        ContactsQuery cq = new ContactsQuery(AireJupiter.this);
                                        long contactid = cq.getContactIdByNumber(address);

                                        boolean flag = ConversationActivity.sender != null && MyTelephony.SameNumber(ConversationActivity.sender, "[<GROUP>]" + mGroupID);
                                        int read = (flag == true ? 1 : 0);

                                        //jack 国际化从分组删除人
                                        String content = String.format(getString(R.string.group_removed_members), nameBuffer);

                                        String displayname = mADB.getNicknameByAddress(address);
                                        SmsDB smsDB = new SmsDB(AireJupiter.this);
                                        smsDB.open();

                                        // TODO: 2016/4/7 之后删除
                                        android.util.Log.d("删除组成员", "msg.address  " + address
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
                                                + "  msg.group_member  " + myIdx);
                                        smsDB.insertMessage(address, contactid, (new Date()).getTime(), read, -1, 1, "", content, 0, null, null, 0, 0, 0, 0, displayname, obligate1, myIdx);
                                        smsDB.close();
                                        GroupMsg groupAdd = new GroupMsg("groupUpdate", "0", "", content, "");
                                        agent.onGroupSend(groupAdd);
                                    }
                                }
                            }).start();

                        }
                        break;
                    case Global.CMD_DELETE_GROUP://I am going to delete a group, and I have to notify others.
                        mGroupID = intent.getIntExtra("GroupID", 0);
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    GroupDB gdb = new GroupDB(AireJupiter.this);
                                    gdb.open();
                                    ArrayList<String> sendeeList = gdb.getGroupMembersByGroupIdx(mGroupID);
                                    ArrayList<String> addressList = new ArrayList<String>();
                                    for (int i = 0; i < sendeeList.size(); i++) {
                                        String address = mADB.getAddressByIdx(Integer.parseInt(sendeeList.get(i)));
                                        addressList.add(address);
                                    }
                                    SendAgent agent = new SendAgent(AireJupiter.this, myIdx, 0, true);

                                    agent.setAsGroup(mGroupID);
                                    agent.onMultipleSend(addressList, ":((Sk)", 0, null, null);

                                    gdb.deleteGroup(mGroupID);
                                    gdb.close();

                                    try {
                                        MyNet net = new MyNet(AireJupiter.this);
                                        net.doPostHttps("remove_group_member.php", "id=" + mGroupID
                                                + "&members=" + myIdx, null);
                                    } catch (Exception e) {
                                    }
                                } catch (Exception e) {
                                }
                            }
                        }).start();
                        break;
                    case Global.CMD_GROUP_ADD_NEW_MEMBER:
                        mGroupID = intent.getIntExtra("GroupID", 0);
                        UnknownIdx = intent.getIntExtra("idx", 0);
                        idxs = intent.getStringArrayListExtra("idxs");

                        if (UnknownIdx > 0) {
                            new Thread(new Runnable() {
                                public void run() {
                                    String mNickname = getString(R.string.unknown_person);
                                    if (!mADB.isFafauser(UnknownIdx)) {
                                        if (tcpSocket.isLogged(false))
                                            tcpSocket.queryUserAddressByIdx(UnknownIdx);//it will popup stranger dialog
                                    }

                                    mNickname = mADB.getNicknameByIdx(UnknownIdx);

                                    GroupDB gdb = new GroupDB(AireJupiter.this);
                                    gdb.open();
                                    mGroupName = gdb.getGroupNameByGroupIdx(mGroupID);
                                    gdb.insertGroup(mGroupID, mGroupName, UnknownIdx);
                                    gdb.close();

                                    Intent it = new Intent(AireJupiter.this, CommonDialog.class);
                                    it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    String title = String.format(getString(R.string.group_invite_new_member), mNickname, mGroupName);
                                    it.putExtra("msgContent", title);
                                    it.putExtra("numItems", 1);
                                    it.putExtra("ItemCaption0", getString(R.string.OK));
                                    it.putExtra("ItemResult0", 0);
                                    showNotification(title, null, true, R.drawable.icon_sms, null);
                                    startActivity(it);
                                }
                            }).start();
                        }
                        //jack 2.4.51
                        if (idxs.size() > 0) {
                            // TODO: 2016/3/21 加入已有group
                            new Thread(new Runnable() {
                                public void run() {

                                    //jack 拼接数据
                                    StringBuffer idxSB = new StringBuffer("");
                                    StringBuffer nicknames = new StringBuffer("");
                                    for (int i = 0; i < idxs.size(); i++) {
                                        String mNickname = getString(R.string.unknown_person);
                                        UnknownIdx = Integer.parseInt(idxs.get(i));
                                        mNickname = mADB.getNicknameByIdx(UnknownIdx);
                                        if (!mADB.isFafauser(UnknownIdx)) {
                                            if (tcpSocket.isLogged(false))
                                                tcpSocket.queryUserAddressByIdx(UnknownIdx);//it will popup stranger dialog
                                        }
                                        if (i > 0) {
                                            idxSB.append(",");
                                            nicknames.append(",");
                                        }
                                        idxSB.append(idxs.get(i));
                                        nicknames.append(mNickname);
                                    }

                                    //jack
                                    // TODO: 2016/3/21 请求php将好友加入group
                                    String Return = "";
                                    try {
                                        MyNet net = new MyNet(AireJupiter.this);
                                        Return = net.doPostHttps("add_group_member.php", "id=" + mGroupID
                                                + "&members=" + idxSB.toString(), null);
                                        android.util.Log.d("群组加人", "id= " + mGroupID + " members=" + idxSB.toString());
                                    } catch (Exception e) {
                                        android.util.Log.d("群组加人", "add_group_member.php" + e.getMessage());
                                    }
                                    if (Return.startsWith("Done")) {
                                        //jack 请求网络成功,加入数据库
                                        GroupDB gdb = new GroupDB(AireJupiter.this);
                                        gdb.open();
                                        mGroupName = gdb.getGroupNameByGroupIdx(mGroupID);
                                        for (int i = 0; i < idxs.size(); i++) {
                                            gdb.insertGroup(mGroupID, mGroupName, Integer.parseInt(idxs.get(i)),1);
                                        }
                                        gdb.close();

                                        // TODO: 2016/3/28 暂时显示将谁加入分组
                                        Intent it = new Intent(AireJupiter.this, CommonDialog.class);
                                        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        String title = String.format(getString(R.string.group_invite_new_member), nicknames.toString(), mGroupName);
                                        it.putExtra("msgContent", title);
                                        it.putExtra("numItems", 1);
                                        it.putExtra("ItemCaption0", getString(R.string.OK));
                                        it.putExtra("ItemResult0", 0);
                                        showNotification(title, null, true, R.drawable.icon_sms, null);
                                        startActivity(it);
                                    }
                                }
                            }).start();
                        }
                        break;
                    case Global.CMD_ADD_AS_RELATED_FRIEND:
                        try {
                            mGroupID = intent.getIntExtra("GroupID", 0);
                            mGroupName = intent.getStringExtra("GroupName");
                            mRDB.insertUser("[<GROUP>]" + mGroupID, 100000000 + mGroupID, mGroupName, 0);

                            Intent it = new Intent(Global.Action_Refresh_Gallery);
                            sendBroadcast(it);
                        } catch (Exception e) {
                        }
                        break;
                    case Global.CMD_UPDATE_SENT_SMS_TIME:
                        // alec: updates the status of the sent msg
                        long sentTime = intent.getLongExtra("sentTime", 0);
                        try {
                            mSmsDB.setMessageSentById(mRow_id, SMS.STATUS_SENT, sentTime);
                        } catch (Exception e) {
                        }
                        break;
                    case Global.CMD_RECONNECT_SOCKET:
                        if (tcpSocket != null)
                            tcpSocket.logged = 0;
                        mHandler.removeCallbacks(reconnectDebounce);
                        mHandler.postDelayed(reconnectDebounce, 4000);
                        break;
                    case Global.CMD_CHECK_ONLINE_FRIENDS:
                        Thread thr = new Thread(mInstantQueryOnlineFriends,
                                "Update Friend Status");
                        thr.start();
                        break;
                    case Global.CMD_CHECK_ONLINE_FRIENDS_NOW:
                        new Thread(mInstantQueryOnlineFriends,
                                "Update Friend Status").start();
                        break;
                    case Global.CMD_SUDDENLY_NO_NETWORK:
                        ContactsOnline.setAllfriendsOffline();
                        RWTOnline.setAllfriendsOffline();
                        Log.e("CMD_SUDDENLY_NO_NETWORK");
                        if (tcpSocket != null)
                            tcpSocket.disconnect("no network", true);
                        if (rwtSocket != null)
                            rwtSocket.disconnect();
                        break;
                    case Global.CMD_ONLINE_UPDATE:
                        Thread thr2 = new Thread(mDownloadnUpdate,
                                "download new apk");
                        thr2.start();
                        break;
                    case Global.CMD_LOCATION_SHARING:
                        Intent it = new Intent(AireJupiter.this, CommonDialog.class);
                        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        String address = intent.getStringExtra("Sender");
                        String Nickname = mADB.getNicknameByAddress(address);
                        locrequest = address;
                        long contact_id = cq.getContactIdByNumber(address);
                        if (contact_id > 0)
                            Nickname = cq.getNameByContactId(contact_id);
                        String title = Nickname + " " + getString(R.string.sharing_location_request);
                        it.putExtra("msgContent", title);
                        it.putExtra("numItems", 2);
                        it.putExtra("ItemCaption0", getString(R.string.no));
                        it.putExtra("ItemResult0", 0);
                        it.putExtra("ItemCaption1", getString(R.string.yes));
                        it.putExtra("ItemResult1", CommonDialog.SHARING);
                        it.putExtra("locationshare", true);
                        showNotification(title, null, true, R.drawable.icon_sms, null);
                        startActivity(it);
                        break;
                    case Global.CMD_SHARING_AGREE:
                        new Thread(agreeToShareLocation).start();
                        break;
                    case Global.CMD_INCOMING_CALL:
                        mHandler.removeCallbacks(mEndupServiceY);
                        startServiceY(AireVenus.CALLTYPE_FAFA);
                        break;
                    case Global.CMD_MAKE_OUTGOING_CALL:
                        if (tcpSocket == null) return;
                        calleeNumber = intent.getStringExtra("Callee");
                        if (!MyUtil.checkNetwork(AireJupiter.this)) {
                            return;
                        }
                        Log.d("CMD_MAKE_OUTGOING_CALL ok");
                        mHandler.removeCallbacks(terminate_call_by_tcp);
                        mHandler.removeCallbacks(mEndupServiceY);
                        if (AireVenus.instance() != null) {
                            AireVenus.instance().cancelQuitServiceY();
                        }

                        //tml*** sametime, prevent DA
                        int idxOUT = mADB.getIdxByAddress(calleeNumber);
                        int idxIN = mPref.readInt("tempCheckSameIN");
                        Log.d("voip.CMD_MAKE_OUTGOING_CALL Check preDA SAMETIME in/out> " + idxIN + "/" + idxOUT);
                        if (idxIN == idxOUT) {
                            Log.e("voip.CMD_MAKE_OUTGOING_CALL SAMETIME!!! (preDA)");
                            mPref.write("tempCheckSameIN", 0);
                            Toast tst = Toast.makeText(AireJupiter.this,
                                    getString(R.string.call) + ": "
                                            + getString(R.string.call_declined),
                                    Toast.LENGTH_LONG);
                            tst.setGravity(Gravity.CENTER, 0, 0);
                            LinearLayout tstLayout = (LinearLayout) tst.getView();
                            TextView tstTV = (TextView) tstLayout.getChildAt(0);
                            tstTV.setTextSize(20);
                            tst.show();
                            break;
                        }
                        //***tml

                        attemptCall = true;

                        if (AireVenus.getCallType() == AireVenus.CALLTYPE_FAFA || AireVenus.getCallType() == AireVenus.CALLTYPE_FILETRANSFER)
                            new Thread(new Runnable() {
                                public void run() {
                                    if (tcpSocket == null) return;
                                    android.os.Process.setThreadPriority(-19);

                                    calleeGotCallRequest = false;

                                    if (!tcpSocket.isLogged(false)) {
                                        Log.e("CMD_MAKE_OUTGOING_CALL *** !tcpSocket.isLogged() ***");
                                        tcpSocket.Login(versionCode);
                                    }

                                    if (AireVenus.getCallType() == AireVenus.CALLTYPE_FAFA || AireVenus.getCallType() == AireVenus.CALLTYPE_FILETRANSFER) {
                                        if (tcpSocket != null && tcpSocket.isLogged(false)) {
                                            int ret = tcpSocket.sendCallRequest(calleeNumber);

                                            if (ret == 1) {
                                                calleeGotCallRequest = true;

                                            } else if (ret < 1) {
                                                unanswered.add(calleeNumber);
                                                Log.e("voip.unanswered still!");
                                            } else if (ret == 2) {  //tml|alex*** iphone push
                                                ret = tcpSocket.sendCallRequestApple(calleeNumber);
                                                if (ret == 1)
                                                    calleeGotCallRequest = true;
                                                else if (ret < 1) {
                                                    unanswered.add(calleeNumber);
                                                    Log.e("voip.unanswered still!");
                                                } else {
                                                    if (DialerActivity.getDialer() != null)
                                                        DialerActivity.getDialer().exitCallMode2("sendCallRequest badret4");
                                                }
                                            } else {
                                                if (DialerActivity.getDialer() != null)
                                                    DialerActivity.getDialer().exitCallMode2("sendCallRequest badret2");
                                            }
                                        } else {
                                            mHandler.postDelayed(new Runnable() {
                                                public void run() {
                                                    if (DialerActivity.getDialer() != null)
                                                        DialerActivity.getDialer().exitCallMode2("sendCallRequest badret0");
                                                }
                                            }, 3000);
                                        }
                                    }
                                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
                                }
                            }).start();
                        else
                            calleeGotCallRequest = true;

                        mHandler.post(new Runnable() {
                            public void run() {

                                if (AireVenus.getLc() != null && MyUtil.CheckServiceExists(AireJupiter.this, "com.pingshow.voip.AireVenus"))
//							if (AireVenus.getLc()!=null && MyUtil.CheckServiceExists(AireJupiter.this, "com.pingshow.voip.AireVenus") || switchingCall)  //tml*** switch conf
                                {
                                    boolean sipcall = (AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL
                                            || AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM
                                            || AireVenus.getCallType() == AireVenus.CALLTYPE_WEBCALL);
                                    if ((sipcall && !AireVenus.runAsSipAccount) || (!sipcall && AireVenus.runAsSipAccount))
//								if ((sipcall && !AireVenus.runAsSipAccount) || (!sipcall && AireVenus.runAsSipAccount) || switchingCall)  //tml*** switch conf, 0d
                                    {
                                        Log.e("!!! STOPPING AireVenus/ServiceY *** AJ @ voip.outgoingCall :: sipcall!=runasSipAcc");
                                        Intent itx = new Intent(AireJupiter.this, AireVenus.class);
                                        stopService(itx);
                                        MyUtil.Sleep(3000);
//			    					Log.e("--------- SWITCH CALL --------- venus stopped");
                                    }
                                    boolean sendingFile = AireVenus.getCallType() == AireVenus.CALLTYPE_FILETRANSFER;
                                    if ((sendingFile && !AireVenus.runAsFileTransfer) || (!sendingFile && AireVenus.runAsFileTransfer)) {
                                        AireVenus.instance().enableDisableCodec("AMR", 8000, !sendingFile);
                                        AireVenus.instance().enableDisableCodec("speex", 16000, !sendingFile);
                                    }
                                }

                                if (!MyUtil.CheckServiceExists(AireJupiter.this, "com.pingshow.voip.AireVenus")) {

                                    if (mPref.readBoolean("doingUPNP", false)) {
                                        MyUtil.Sleep(3000);
                                    }

//								Log.e("--------- SWITCH CALL --------- venus restarting");
                                    Intent VoipIntent = new Intent(Intent.ACTION_MAIN);
                                    VoipIntent.setClass(AireJupiter.this, AireVenus.class);
                                    startService(VoipIntent);
                                } else {
                                    mHandler.removeCallbacks(mEndupServiceY);
                                    if (mPref.readBoolean("enable_tls", false)) {
                                        try {
                                            if (AireVenus.instance() != null)
                                                AireVenus.instance().initFromConf();
                                        } catch (VoipConfigException e) {
                                            e.printStackTrace();
                                        } catch (VoipException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        AireVenus.sip_login(mySipServer, myPhoneNumber);
                                    }
                                }
                            }
                        });

                        calleeContact_id = intent.getLongExtra("Contact_id", -1);
                        calleeNumber = intent.getStringExtra("Callee");
                        MakeVideoCall = intent.getBooleanExtra("VideoCall", true);

                        mDisplayname = intent.getStringExtra("Displayname");

                        mHandler.postDelayed(new Runnable() {
                            public void run() {
//							Log.e("--------- SWITCH CALL --------- switch view " + switchingCall);
//							if (!switchingCall) {
                                Intent intent = new Intent(AireJupiter.this, DialerActivity.class);

                                mPref.write("curCall", calleeNumber);
                                if (mDisplayname == null)
                                    mDisplayname = mADB.getNicknameByAddress(calleeNumber);
                                Log.d("toDialer! " + calleeContact_id + " " + calleeNumber + " " + mDisplayname + " " + MakeVideoCall);
                                intent.putExtra("Contact_id", calleeContact_id);
                                intent.putExtra("PhoneNumber", calleeNumber);
                                intent.putExtra("DisplayName", mDisplayname);
                                intent.putExtra("VideoCall", MakeVideoCall);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                AireJupiter.this.startActivity(intent);
//							} else {
                                //tml*** switch conf
//								if (DialerActivity.getDialer() != null) {
//									DialerActivity.getDialer().switchToConf();
//									DialerActivity.getDialer().runDialerStuff(calleeNumber, "CMD_MAKE_OUTGOING_CALL");
//								}
//							}
                            }
                        }, 100);
                        break;
                    case Global.CMD_CALL_END:
                        Log.e("attempt CMD_CALL_END " + !attemptCall);
                        if (attemptCall) {
                            return;
                        }
                        int timeup = intent.getIntExtra("immediately", 2000);
                        Log.d("Stop ServiceY after " + timeup / 1000 + " seconds");
                        mHandler.postDelayed(mEndupServiceY, timeup);

                        if (intent.getBooleanExtra("AireCall", false)) {
                            callingOut = true;
                            mHandler.postDelayed(getSipCredit, 2000);
                        }
                        break;
                    case Global.CMD_LOGIN_FAILED:// alec
                        unautherized999 = true;

                        long last = mPref.readLong("last_time_popup_999", 0);
                        long now = new Date().getTime();
                        if (now - last < 600000) // 10 minute
                            return;// no need to popup

                        mPref.write("accountUpdated", true);
                        mPref.writeLong("last_time_popup_999", now);
                        break;
                    case Global.CMD_TCP_CONNECTION_UPDATE:
                        unautherized999 = false;
                        break;
                    case Global.CMD_CONNECTION_POOR:  //tml*** tcp test
                        Log.e("CMD_CONNECTION_POOR");
                        boolean force = intent.getBooleanExtra("ForcePoor", false);
                        String from = intent.getStringExtra("warnFrom");
                        if (from == null) from = "POOR";
                        toastWarning(1, force, from);
                        break;
                    case Global.CMD_UPLOAD_PROFILE_PHOTO:
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (myIdx == 0) return;
                                String photoPath = mPref.read("myPhotoPath");
                                String Return = "";
                                try {
                                    int count = 0;
                                    do {
                                        MyNet net = new MyNet(AireJupiter.this);
                                        //Hsia:更正头像
                                        Return = net.doPostAttach("uploadphoto_aire.php", myIdx, 0, photoPath, myPhpServer_default2A); // httppost
                                        if (Return.startsWith("Done"))
                                            break;
                                        MyUtil.Sleep(2500);
                                    } while (++count < 3);
                                } catch (Exception e) {
                                }

                                Log.d("CMD_UPLOAD_PROFILE_PHOTO " + Return + " " + photoPath);
                                if (Return.startsWith("Done")) {
                                    doPostProfileUpdateInTimeLine(photoPath);
                                    tellFriendsProfileChanged(0, null);
                                    mPref.write("myPhotoUploaded", true);
                                } else
                                    mPref.write("myPhotoUploaded", false);
                            }
                        }).start();
                        break;
                    case Global.CMD_UPLOAD_PROFILE_MOOD:
                        if (tcpSocket == null) return;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String moodContent = mPref.read("moodcontent");
                                try {
                                    int count = 0;
                                    do {
                                        MyNet net = new MyNet(AireJupiter.this);
                                        String Return = net.doPostHttps("update_mood.php", "id=" + URLEncoder.encode(myPhoneNumber, "UTF-8")
                                                + "&password=" + URLEncoder.encode(myPasswd, "UTF-8")
                                                + "&mood=" + URLEncoder.encode(moodContent, "UTF-8"), null);
                                        if (Return.startsWith("Done"))
                                            break;
                                        MyUtil.Sleep(1500);
                                    } while (count++ < 3);
                                } catch (Exception e) {
                                }

                                if (!tcpSocket.isLogged(false))
                                    mPref.write("moodcontentuploaded", false);
                                else
                                    mPref.write("moodcontentuploaded", true);
                                doPostMoodInTimeLine(moodContent);
                                tellFriendsProfileChanged(1, moodContent);
                            }
                        }).start();
                        break;
                    case Global.CMD_UPLOAD_PROFILE_EMAIL:
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                MyNet net = new MyNet(AireJupiter.this);
                                String Return = "";
                                try {
                                    int count = 0;
                                    String email = mPref.read("email");
                                    do {
                                        Return = net.doPostHttps("update_email.php", "id=" + URLEncoder.encode(myPhoneNumber, "UTF-8")
                                                + "&password=" + URLEncoder.encode(myPasswd, "UTF-8")
                                                + "&email=" + URLEncoder.encode(email, "UTF-8"), null);
                                        Log.d("update_email=" + email + " " + URLEncoder.encode(email, "UTF-8"));
                                        Log.d("update_email Return=" + Return);
                                        if (Return.startsWith("Done"))
                                            break;
                                        count++;
                                        MyUtil.Sleep(1500);
                                    } while (count < 4);
                                } catch (Exception e) {
                                }

                                if (Return.startsWith("Done"))
                                    mPref.write("emailuploaded", true);
                                else
                                    mPref.write("emailuploaded", false);
                            }
                        }).start();
                        break;
                    case Global.CMD_UPDATE_MY_NICKNAME:
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                MyNet net = new MyNet(AireJupiter.this);
                                String Return = "";
                                String myNickname = "";
                                try {
                                    int count = 0;
                                    myNickname = mPref.read("myNickname");
                                    String gender = mPref.read("myGender", "male");
                                    String myFacebookID = mPref.read("myFacebookID", "");
                                    String myWeiboID = mPref.read("myWeiboID", "");

                                    myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
                                    do {
                                        Return = net.doPostHttps("updateprofile_x.php", "idx=" + myIdx
                                                + "&gender=" + gender
                                                + "&password=" + URLEncoder.encode(myPasswd, "UTF-8")
                                                + "&facebook=" + myFacebookID
                                                + "&weibo=" + myWeiboID
                                                + "&nickname=" + URLEncoder.encode(myNickname, "UTF-8")
                                                + "&version=" + versionCode
                                                + "&device=" + URLEncoder.encode(Build.BRAND + "/" + Build.PRODUCT + "/" + Build.MODEL, "UTF-8")
                                                , null);
                                        if (Return.startsWith("Done"))
                                            break;
                                        count++;
                                        MyUtil.Sleep(1500);
                                    } while (count < 4);
                                } catch (Exception e) {
                                }
                                //bree
                                if (Return.startsWith("Done")) {
                                    tellFriendsProfileChanged(2, myNickname);
                                }
                                mPref.write("nicknameUpdated", Return.startsWith("Done"));
                            }
                        }).start();
                        break;
                    case Global.CMD_UPDATE_CALL_LOG:
                        mHandler.removeCallbacks(mUpdateCallLog);
                        callLogBundle = intent.getExtras();

                        if (!intent.getBooleanExtra("runAsSip", false))
                            mHandler.postDelayed(mUpdateCallLog, 3000);
                        else if (intent.getIntExtra("status", 0) == 1)//webcall log
                        {
                            //new Thread(mUpdateWebCallLog).start();
                        }
                        break;
                    case Global.CMD_QUERY_360:
                        new Thread(searchFriendsByPhonebook).start();
                        break;
                    case Global.CMD_UPLOAD_FRIENDS:
                        uploadBuddyList(intent.getStringExtra("idxlist"), intent.getIntExtra("serverType", 1));
                        break;
                    case Global.CMD_DOWNLOAD_PHOTO_FROMNET:
                        int type = intent.getIntExtra("type", 0);
                        if (type == 0)
                            mHandler.post(new Runnable() {
                                public void run() {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            doCheckPhotoFromNet();
                                        }
                                    }).start();
                                }
                            });
                        else
                            mHandler.post(new Runnable() {
                                public void run() {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            downloadRelatedUsersPhotos();
                                        }
                                    }).start();
                                }
                            });
                        break;
                    case Global.CMD_DOWNLOAD_FRIENDS:
//					new Thread(downloadFriendList).start();
                        //tml|james*** unknown contacts error
                        boolean unknowns = intent.getBooleanExtra("unknowns", false);
                        if (unknowns && checkonce) {
                            checkonce = false;
                            mHandler.post(new Runnable() {
                                public void run() {
                                    UsersActivity.forceRefresh = true;
                                    getFriendNicknames();
                                }
                            });
                        } else {
                            new Thread(downloadFriendList).start();
                            new Thread(downloadGroupList).start();
                        }
                        //***tml
                        break;
                    case Global.CMD_FILE_TRANSFERED:
                        String filename = intent.getStringExtra("Filename");
                        if (filename != null) {
                            OpenDifferentFile openDifferentFile = new OpenDifferentFile(AireJupiter.this);
                            openDifferentFile.openFile(filename);
                        }
                        break;
                    case Global.CMD_UPDATE_SIP_CREDIT:
                        mHandler.postDelayed(getSipCredit, 1000);
                        break;
                    case Global.CMD_CHECK_PAYPAL_AGAIN:
                        mHandler.postDelayed(resendPendingPayments, 90000);
                        break;
                }
            } else if (intent.getAction().equals(Global.Action_SD_AvailableSpare)) {

                if (intent.getIntExtra("SDAvailable", 0) == 0)
                    Toast.makeText(AireJupiter.this, getString(R.string.sd_availablespare),
                            Toast.LENGTH_LONG).show();
                else if (intent.getIntExtra("SDAvailable", 0) == -1)
                    Toast.makeText(AireJupiter.this, getString(R.string.sd_notfound),
                            Toast.LENGTH_LONG).show();

            } else if (intent.getAction().equals(Global.Action_FileDownload)
                    && (intent.getIntExtra("attached", 0) == 9 || intent
                    .getIntExtra("attached", 0) == 10)) {
                String curFilePath = intent.getStringExtra("filename");
                String filename = curFilePath.substring(curFilePath
                        .lastIndexOf("/") + 1);
                Intent it = new Intent();
                it = new Intent(AireJupiter.this, MessageActivity.class);
                if (intent.getBooleanExtra("err", false)) {
                    Toast.makeText(AireJupiter.this, getString(R.string.downloaderror), Toast.LENGTH_SHORT).show();
                    if (AireJupiter.getInstance() != null) {
                        AireJupiter.getInstance().showNotification(filename, it,
                                true, R.drawable.icon_sms,
                                getString(R.string.downloaderror));
                    }
                } else {
                    Toast.makeText(AireJupiter.this, getString(R.string.downloadsucess), Toast.LENGTH_SHORT).show();

                    showNotification(filename, it,
                            true, R.drawable.icon_sms,
                            getString(R.string.downloadsucess));
                    try {
                        int tmpAttach = intent.getIntExtra("attached", 0);
                        mSmsDB.setMessageBodyById(
                                ConversationActivity.smsId,
                                tmpAttach,
                                tmpAttach == 9 ? "(vdo)"
                                        : getString(R.string.downloadedfile, filename), curFilePath);
                    } catch (Exception e) {
                    }
                    // refresh ConversationActivity listview
                    it = new Intent();
                    it.setAction(Global.Action_MsgGot);
                    sendBroadcast(it);
                }
                ConversationActivity.fileDownloading = false;
            }
        }
    };

    //只解析群组信息
    private void processIncomingGroupSMS(String originalSignal) {
        // TODO: 2016/3/31 解析群组信息
        LineToParse = originalSignal;
        (new Thread(new Runnable() {
            public void run() {
                ArrayList<SMS> smslist = ParseSmsLine.ParseGroupSMS(AireJupiter.this, LineToParse, cq, mADB, mPref);
                if (smslist.size() > 0) {
                    msgGot = smslist.get(smslist.size() - 1);
                    // TODO: 2016/4/1 删除
                    android.util.Log.d("860Socket", "msgGot: " + msgGot.toString());
                    String tmpMsg = "";
                    if ((msgGot.attached & 3) == 3) {
                        tmpMsg = getString(R.string.voicememo) + ","
                                + getString(R.string.picmsg);
                    } else if ((msgGot.attached & 1) == 1) {
                        tmpMsg = getString(R.string.voicememo);
                    } else if ((msgGot.attached & 2) == 2 || msgGot.attached == 16) {
                        tmpMsg = getString(R.string.picmsg);
                    } else if ((msgGot.attached & 4) == 4) {
                        tmpMsg = getString(R.string.interphone);
                    } else if (msgGot.attached == 8) {
                        if (msgGot.content.startsWith(getString(R.string.video))
                                && msgGot.content.contains("(vdo)")) {
                            tmpMsg = getString(R.string.videomemo);
                        } else if (msgGot.content.startsWith(getString(R.string.filememo_recv))
                                && msgGot.content.contains("(fl)")) {
                            tmpMsg = getString(R.string.filememo_recv);
                        }
                    } else {
                        tmpMsg = getString(R.string.textmessage);
                    }
                    manySmsContent.append("<br>"
                            + getString(R.string.sms_parese_stringbuf,
                            msgGot.displayname, tmpMsg));

                    if (manySmsContent.toString().split("<br>").length > 3) {
                        String tmp = manySmsContent.toString().substring(
                                manySmsContent.toString().substring(4)
                                        .indexOf("<br>") + 4);
                        manySmsContent.setLength(0);
                        manySmsContent.append(tmp);
                    }
                    if (manySmsContent.toString().startsWith("<br>")) {
                        String tmp = manySmsContent.substring(4);
                        manySmsContent.setLength(0);
                        manySmsContent.append(tmp);
                    }
                    // TODO: 2016/4/1 之后删除
                    android.util.Log.d("860Socket", "tmpMsg " + tmpMsg);
                    mHandler.post(HandleMessageComing);
                }
            }
        }, "processIncomingSMS")).start();
    }

    //查询群组成员,并将群组成员插入DB
    private void queryGroupPhpAndInsertDB(int mGroupID, GroupDB gdb, AmpUserDB adb) {
        String Return = "";
        Group groupInfo = null;
        Gson gson = null;
        try {
            int c = 0;
            do {
                MyNet net = new MyNet(AireJupiter.this);
                Return = net.doPostHttps("query_group_v2.php", "id=" + mGroupID, null);
                Log.i("query_group Return=" + Return);
                gson = new Gson();
                groupInfo = gson.fromJson(Return, Group.class);
                //请求成功
                if (groupInfo.getCode() == 200) break;
                MyUtil.Sleep(2500);
            } while (++c < 3);
        } catch (Exception e) {
        }
        if (groupInfo != null) {
            if (groupInfo.getCode() == 200) {
                String groupname = groupInfo.getGname();

                android.util.Log.d("AireJupiter", groupname);

                // TODO: 2016/4/5  存入组信息
                adb.insertUser("[<GROUP>]" + mGroupID, mGroupID + 100000000,
                        groupname);
                //获取群组信息成功
                List<Group.MembersEntity> members = groupInfo.getMembers();
                Boolean inThisGroup = false;
                for (Group.MembersEntity member : members) {
                    // TODO: 2016/4/7 判断是否还有自己,没有自己的话就删除group
                    if (member.getIdx().equals(mPref.read("myIdx"))) inThisGroup = true;

                    //将数据插入数据库,应该数据库中没有这个group
                    gdb.GdbbeginTransaction();
                    long l = gdb.insertGroup(mGroupID, groupname, Integer.parseInt(member.getIdx()), member.getRank());
                    gdb.GdbEndTransaction();
                }
                // TODO: 2016/4/7 不在此群组中,删除此群
                if (!inThisGroup) {
                    android.util.Log.d("AireJupiter", "删除群组");
                    mADB.deleteContactByAddress("[<GROUP>]" + mGroupID);
                    UsersActivity.needRefresh = true;
                    Intent intent = new Intent(Global.Action_Refresh_Gallery);
                    sendBroadcast(intent);

                    gdb.deleteGroup(mGroupID);

                    try {
                        mSmsDB.deleteThreadByAddress("[<GROUP>]" + mGroupID);
                    } catch (Exception e) {
                    }
                }
            }
        }
        //下载图片
        try {
            String localfile = Global.SdcardPath_inbox + "photo_" + (mGroupID + 100000000) + ".jpg";
            File f = new File(localfile);
            if (!f.exists()) {
                String remotefile = "groups/photo_" + mGroupID + ".jpg";
                downloadAnyPhoto(remotefile, localfile, 3, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void uploadAllFriends() {
        Cursor cursor = mADB.fetchAll();
        if (cursor == null) return;
        String idxs = "";
        int j = 0;
        if (cursor.moveToFirst()) {
            while (cursor.moveToNext()) {
                int idx = cursor.getInt(3);
                String address = cursor.getString(1);
                if (address.startsWith("[<GROUP>]")) continue;
                if (idx <= 50) continue;
                if (j > 0)
                    idxs += "+";
                idxs += idx;
                j++;
            }
        }
        if (!cursor.isClosed())
            cursor.close();
        uploadBuddyList(idxs, 1);
    }

    String idxlist = "";
    int buddylist_type;

    void uploadBuddyList(String idxs, int type) {
        idxlist = idxs;
        buddylist_type = type;
        if (idxlist != null) {
            new Thread(new Runnable() {
                public void run() {
                    int count = 0;
                    String Return = "";
                    String URL;
                    if (buddylist_type == 0)
                        URL = "removebuddy_aire.php";
                    else
                        URL = "addbuddy_aire.php";
                    do {
                        MyNet net = new MyNet(AireJupiter.this);
                        Return = net.doPost(URL, "id=" + myIdx
                                + "&idxs=" + idxlist, null);
                        if (Return.length() > 5 && !Return.startsWith("Error"))
                            break;
                        MyUtil.Sleep(500);
                    } while (++count < 3);
                }
            }).start();
        }
    }

    public void getSipFriendsAcc() {
        Log.d("getSipFriendsAcc...");
        long last = mPref.readLong("last_time_get_sip_accounts_3", 0);
        long now = new Date().getTime();
        if (now - last < 21600000 && last > 0) { // 6 hours
            Log.d("getSipFriendsAcc... noCheck yet*");
            return;// no need to update
        }
        try {
            int count = 0;
            String Return = "";
            do {
                MyNet net = new MyNet(AireJupiter.this);//offlinemsg.php
                Return = net.doPostHttps("getsipaccount_and.php", "id="
                        + URLEncoder.encode(myPhoneNumber, "UTF-8")
                        + "&password=" + URLEncoder.encode(myPasswd, "UTF-8"), null);
                if (Return.length() > 5 && !Return.startsWith("Error"))
                    break;
                MyUtil.Sleep(500);
            } while (++count < 3);

            if (Return.startsWith("Done")) {
                Return = Return.substring(5);
                try {
                    String[] items = Return.split("/");
                    for (int i = 0; i < items.length; i++) {
                        String[] ss = items[i].split(":");
                        if (ss.length == 4) {
                            int index = Integer.parseInt(ss[0]);
                            mPref.write("aireSipAcount" + index, ss[1]);
                            mPref.write("aireSipPassowrd" + index, ss[2]);
                            mPref.write("aireSipServer" + index, ss[3]);
                            Log.d("getSipFriendsAcc... user/pw/ip " + ss[1] + " " + ss[2] + " " + ss[3]);
                        }
                    }
                    mPref.writeLong("last_time_get_sip_accounts_3", now);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
    }

    ;

    int toastResID;
    Runnable toastMessage = new Runnable() {
        public void run() {
            Toast.makeText(AireJupiter.this, toastResID, Toast.LENGTH_LONG).show();
        }
    };

    String locrequest = "";
    final private Runnable agreeToShareLocation = new Runnable() {
        public void run() {
            try {
                //alec
                int count = 0;
                String ret = "";
                while (++count <= 3) {
                    MyNet net = new MyNet(getApplicationContext());
                    ret = net.doPost("shareloc.php",
                            "id=" + URLEncoder.encode(myPhoneNumber, "UTF-8")
                                    + "&queryid="
                                    + URLEncoder.encode(locrequest, "UTF-8")
                                    + "&relation=1", null);
                    if (ret.length() > 1 && !ret.startsWith("Error"))
                        break;
                    MyUtil.Sleep(1500);
                }

                if (ret.startsWith("OK")) {

                    toastResID = R.string.sharing_done;
                    mHandler.post(toastMessage);

                    long latitude = mPref.readLong("latitude", 39976279);
                    long longitude = mPref.readLong("longitude", 116349386);

                    SendAgent sendAgent = new SendAgent(AireJupiter.this, myIdx, 0, false);
                    sendAgent.onSend(locrequest, "[<AGREESHARE>]," + mPref.read("myID") + ",1," + latitude + ","
                            + longitude, 0, null, null, false);

                    String globalNumber = MyTelephony.attachPrefix(
                            AireJupiter.this, locrequest);
                    long timeout = MyUtil.getSharingTimeout(1);
                    mPref.writeLong(globalNumber, timeout);
                    if (mPref.readLong("SpeedupMapMonitor", 0) < timeout * 1000)
                        mPref.writeLong("SpeedupMapMonitor", timeout * 1000);
                } else {
                    toastResID = R.string.nonetwork;
                    mHandler.post(toastMessage);
                }
            } catch (Exception e) {
                toastResID = R.string.nonetwork;
                mHandler.post(toastMessage);
            }
        }
    };

    private String LineToParse;
    public static StringBuffer manySmsContent = new StringBuffer();

    private void processIncomingSMS(String signalLine) {
        LineToParse = signalLine;
        (new Thread(new Runnable() {
            public void run() {
                ArrayList<SMS> smslist = ParseSmsLine.Parse2(AireJupiter.this, LineToParse, cq, mADB, mPref);
                if (smslist.size() > 0) {
                    msgGot = smslist.get(smslist.size() - 1);
                    android.util.Log.d("Socket210", "msgGot: " + msgGot);
                    String tmpMsg = "";
                    if ((msgGot.attached & 3) == 3) {
                        tmpMsg = getString(R.string.voicememo) + ","
                                + getString(R.string.picmsg);
                    } else if ((msgGot.attached & 1) == 1) {
                        tmpMsg = getString(R.string.voicememo);
                    } else if ((msgGot.attached & 2) == 2 || msgGot.attached == 16) {
                        tmpMsg = getString(R.string.picmsg);
                    } else if ((msgGot.attached & 4) == 4) {
                        tmpMsg = getString(R.string.interphone);
                    } else if (msgGot.attached == 8) {
                        if (msgGot.content.startsWith(getString(R.string.video))
                                && msgGot.content.contains("(vdo)")) {
                            tmpMsg = getString(R.string.videomemo);
                        } else if (msgGot.content.startsWith(getString(R.string.filememo_recv))
                                && msgGot.content.contains("(fl)")) {
                            tmpMsg = getString(R.string.filememo_recv);
                        }
                    } else {
                        tmpMsg = getString(R.string.textmessage);
                    }
                    manySmsContent.append("<br>"
                            + getString(R.string.sms_parese_stringbuf,
                            msgGot.displayname, tmpMsg));

                    if (manySmsContent.toString().split("<br>").length > 3) {
                        String tmp = manySmsContent.toString().substring(
                                manySmsContent.toString().substring(4)
                                        .indexOf("<br>") + 4);
                        manySmsContent.setLength(0);
                        manySmsContent.append(tmp);
                    }
                    if (manySmsContent.toString().startsWith("<br>")) {
                        String tmp = manySmsContent.substring(4);
                        manySmsContent.setLength(0);
                        manySmsContent.append(tmp);
                    }
                    android.util.Log.d("Socket210", "tmpMsg: " + tmpMsg);

                    mHandler.post(HandleMessageComing);
                }
            }
        }, "processIncomingSMS")).start();
    }

    //tml*** switch conf
//	private boolean switchingCall = false;
//	public void setSwitchCall(boolean set, String from) {
//		switchingCall = set;
//		Log.w("voip.setSwitchCall (" + from + ") " + switchingCall);
//	}
//	
//	public boolean getSetSwitchCall() {
//		return switchingCall;
//	}

    public void lanuchServiceYToJoinChatroom(String ip, int from, boolean isBroadcast) {
        if (DialerActivity.getDialer() != null) return;
//		if (DialerActivity.getDialer() != null && !switchingCall) return;  //tml*** switch conf
        if (AireVenus.instance() != null)
            AireVenus.instance().deregisterSip();

        mPref.write("joinSipAddress", ip);
        mPref.write("incomingChatroom", true);

        if (AireVenus.getLc() != null && MyUtil.CheckServiceExists(AireJupiter.this, "com.pingshow.voip.AireVenus"))
//		if (AireVenus.getLc() != null && MyUtil.CheckServiceExists(AireJupiter.this, "com.pingshow.voip.AireVenus") || switchingCall)  //tml*** switch conf, 1b
        {
            if ((AireVenus.getCallType() == AireVenus.CALLTYPE_FAFA ||
                    AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL ||
                    AireVenus.getCallType() == AireVenus.CALLTYPE_WEBCALL)) {
                Log.e("!!! STOPPING AireVenus/ServiceY *** AJ @ voip.joinChatroom :: calltype fafa||airecall||webcall");
                Intent itx = new Intent(AireJupiter.this, AireVenus.class);
                stopService(itx);
                MyUtil.Sleep(3000);
//				Log.e("--------- SWITCH CALL --------- venus stopped");
            }
        }

        startServiceY(AireVenus.CALLTYPE_CHATROOM);

        calleeNumber = isBroadcast ? "1008" : "1007";
        String idx = "" + from;
        mGroupID = from;

        for (int i = idx.length(); i < 7; i++)
            calleeNumber += "0";

        calleeNumber += idx;
        mDisplayname = mADB.getNicknameByIdx(from);
        String address = mADB.getAddressByIdx(from);
        calleeContact_id = cq.getContactIdByNumber(address);

        mPref.write("ChatroomHostIdx", from);

        mHandler.postDelayed(new Runnable() {
            public void run() {
//				Log.e("--------- SWITCH CALL --------- switch view " + switchingCall);
//				if (!switchingCall) {
                Intent intent = new Intent(AireJupiter.this, DialerActivity.class);
                intent.putExtra("Contact_id", calleeContact_id);
                intent.putExtra("PhoneNumber", calleeNumber);
                intent.putExtra("DisplayName", mDisplayname);
                intent.putExtra("ChatroomHostIdx", mGroupID);
                intent.putExtra("VideoCall", false);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                AireJupiter.this.startActivity(intent);
//				} else {
                //tml*** switch conf
//					if (DialerActivity.getDialer() != null) {
//						DialerActivity.getDialer().switchToConf();
//						DialerActivity.getDialer().runDialerStuff(calleeNumber, "YToJoinChatroom");
//					}
//				}
            }
        }, 1000);
    }
    //tml*** new friends, remove
//	Runnable searchPossibleFriends_delayed=new Runnable(){
//		public void run()
//		{
//			new Thread(searchPossibleFriends).start();
//		}
//	};

    Runnable searchPossibleFriends = new Runnable() {
        public void run() {
            if (!mPref.readBoolean("ProfileCompleted", false)) {
//				mHandler.postDelayed(searchPossibleFriends_delayed, 19000);  //tml*** new friends, remove
                return;
            }

            long last = mPref.readLong("last_search_possible_friends", 0);
            long now = new Date().getTime();
            if (now - last < 35800000) // ~10 hours
                return;// no need to update
            Log.d("searchPossibleFriends");

            mPref.writeLong("last_search_possible_friends", now);

            new Thread(new Runnable() {
                public void run() {
                    List<RelatedUserInfo> PossibleList = null;

                    int relationship = UsersActivity.numTrueFriends / 6 + 2;
                    try {
                        myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);

                        MyNet net = new MyNet(AireJupiter.this);
                        PossibleList = net.doPostHttpWithXML("possiblefriends_aire.php", "idx=" + myIdx
                                + "&relationship=" + relationship, null);
                    } catch (Exception e) {
                    }

                    if (PossibleList != null) {
                        boolean result = false;
                        try {
                            if (mRDB.isOpen() && mADB.isOpen() && PossibleList.size() > 0) {
                                try {
                                    mRDB.deleteAll(); //alec
                                } catch (Exception e) {
                                }

                                String nickname;
                                int idx;
                                int jf;
                                for (int i = 0; i < PossibleList.size(); i++) {
                                    RelatedUserInfo r = PossibleList.get(i);
                                    idx = r.getIdx();
                                    jf = r.getjointfriends();
                                    String address = r.getAddress();
                                    if (mRDB.isUserBlocked(address) == 1 || mADB.isUserBlocked(address) == 1)
                                        continue;
                                    if (mADB.isFafauser(address) || mRDB.isFafauser(address))
                                        continue;
                                    if (idx == myIdx) continue;
                                    nickname = r.getNickName();
                                    if (mRDB.insertUser(address, idx, nickname, jf) > 0)
                                        result = true;
                                }
                            }
                        } catch (Exception e) {
                        }

                        if (result) {
                            Intent intent = new Intent(Global.Action_Refresh_Gallery);
                            sendBroadcast(intent);
                        }

                        Intent intent = new Intent();
                        intent.setAction(Global.Action_InternalCMD);
                        intent.putExtra("Command", Global.CMD_DOWNLOAD_PHOTO_FROMNET);
                        intent.putExtra("type", 1);
                        sendBroadcast(intent);
                    }
                }
            }).start();
        }
    };

    public void offlineMessage() {
        mHandler.postDelayed(new Runnable() {
            public void run() {
                new Thread(onReceiveOfflineMessage, "onReceiveSMSFromHttp").start();
            }
        }, 7500);
    }

    final private Runnable onReceiveOfflineMessage = new Runnable() {
        public void run() {
            try {
                Log.d("onReceiveOfflineMessage");
                MyNet net = new MyNet(AireJupiter.this);//offlinemsg.php
                String Return = net.doPostHttps("olm.php", "id="
                        + URLEncoder.encode(myPhoneNumber, "UTF-8")
                        + "&password=" + URLEncoder.encode(myPasswd, "UTF-8"), null);
//				Log.d("offlineMsg Receive: " + Return);

                if (Return != null && !Return.equals("None")) {
                    if (Return.startsWith("MSG:")) {
                        Return = Return.substring(Return.indexOf('=') + 1);

                        ArrayList<SMS> smslist = ParseSmsLine.Parse(
                                AireJupiter.this, Return, cq, mADB, mRDB, mPref);
                        if (smslist.size() > 0) {
                            for (int i = 0; i < smslist.size(); i++) {
                                msgGot = smslist.get(i);
                                String tmpMsg = "";
                                if ((msgGot.attached & 3) == 3) {
                                    tmpMsg = getString(R.string.voicememo)
                                            + ","
                                            + getString(R.string.picmsg);
                                } else if ((msgGot.attached & 1) == 1) {
                                    tmpMsg = getString(R.string.voicememo);
                                } else if ((msgGot.attached & 2) == 2) {
                                    tmpMsg = getString(R.string.picmsg);
                                } else if ((msgGot.attached & 4) == 4) {
                                    tmpMsg = getString(R.string.interphone);
                                } else if (msgGot.attached == 8) {
                                    if (msgGot.content
                                            .startsWith(getString(R.string.video))
                                            && msgGot.content.contains("(vdo)")) {
                                        tmpMsg = getString(R.string.videomemo);
                                    } else if (msgGot.content
                                            .startsWith(getString(R.string.filememo_recv))
                                            && msgGot.content.contains("(fl)")) {
                                        tmpMsg = getString(R.string.filememo_recv);
                                    }
                                } else {
                                    tmpMsg = getString(R.string.textmessage);
                                }
                                manySmsContent.append("<br>"
                                        + getString(
                                        R.string.sms_parese_stringbuf,
                                        msgGot.displayname, tmpMsg));

                                if (manySmsContent.toString().split("<br>").length > 3) {
                                    String tmp = manySmsContent
                                            .toString()
                                            .substring(
                                                    manySmsContent.toString()
                                                            .substring(4)
                                                            .indexOf("<br>") + 4);
                                    manySmsContent.setLength(0);
                                    manySmsContent.append(tmp);
                                }
                                if (manySmsContent.toString()
                                        .startsWith("<br>")) {
                                    String tmp = manySmsContent.substring(4);
                                    manySmsContent.setLength(0);
                                    manySmsContent.append(tmp);
                                }
                            }
                            mHandler.post(HandleMessageComing);
                        }
                    }
                }

                MyUtil.Sleep(1000);

                for (int i = 0; i < ParseSmsLine.unknownList.size(); i++) {
                    SMS m = ParseSmsLine.unknownList.get(i);
                    Log.d("addF.stranger offline! " + m.type + " " + m.address);
                    Intent intent = new Intent(Global.Action_InternalCMD);
                    intent.putExtra("Command", Global.CMD_STRANGER_COMING);
                    intent.putExtra("Address", m.address);
                    intent.putExtra("Idx", m.type);
                    intent.putExtra("Annoying", m.read > 2);
                    sendBroadcast(intent);
                    MyUtil.Sleep(8000);
                }

                ParseSmsLine.unknownList.clear();

            } catch (Exception e) {
            }
        }
    };

    public void showNotification(String notificationText, Intent it,
                                 boolean bSound, int icon, String title) {
        Notification notification = new Notification(icon, notificationText,
                System.currentTimeMillis());

        mNM.cancel(R.string.app_name);

        if (it == null || ConversationActivity.sender != null)
            it = new Intent(this, SplashScreen.class);

        it.putExtra("fromNotification", true);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT);

        if (title == null)
            title = getResources().getString(R.string.newmessage);

        notification.setLatestEventInfo(this, title, notificationText,
                contentIntent);

        final boolean ringb = mPref.readBoolean("notification_sound", true) && bSound;
        final boolean recv_vbr = mPref.readBoolean("recvVibrator", true);
        notification.defaults = Notification.DEFAULT_LIGHTS
                | (recv_vbr ? Notification.DEFAULT_VIBRATE : 0)
                | (ringb ? Notification.DEFAULT_SOUND : 0);

        if (recv_vbr)
            notification.vibrate = new long[]{100, 250};
        notification.flags = Notification.FLAG_AUTO_CANCEL
                | Notification.FLAG_SHOW_LIGHTS;
        notification.ledARGB = Color.GREEN;
        notification.ledOnMS = 200;
        notification.ledOffMS = 1000;

        mNM.notify(R.string.app_name, notification);
    }

    final Runnable mUpdateCallLog = new Runnable() {
        public void run() {
            String address = callLogBundle.getString("address");
            String displayname = callLogBundle.getString("displayname");
            int type = callLogBundle.getInt("type", 1);
            long contact_id = callLogBundle.getLong("contact_id");
            long time = callLogBundle.getLong("time");
            int direction = callLogBundle.getInt("direction", 1);
            int duration = callLogBundle.getInt("duration", 0);
            int status = callLogBundle.getInt("status", 0);

            if (direction == 2)//Outgoing
            {
                if (mSmsDB.isOpen()) {
                    mSmsDB.insertMessage(address, contact_id,
                            (new Date()).getTime(), 1, -1, 2, "",
                            (status > 0 ?
                                    ("(COt) " + getString(R.string.call_duration) + " " + DateUtils.formatElapsedTime(duration)) : "(COt)"), 0, null, null,
                            0, 0, 0, 0, displayname, null, 0);
                    Intent it = new Intent();
                    it.setAction(Global.Action_MsgSent);
                    sendBroadcast(it);
                }
            } else {

                if (status == 0) {
                    mNM.cancel(R.string.app_name);

                    Intent it = new Intent(AireJupiter.this, ConversationActivity.class);
                    it.putExtra("ActivityType", 1);
                    it.putExtra("SendeeNumber", address);
                    it.putExtra("SendeeContactId", contact_id);
                    it.putExtra("SendeeDisplayname", displayname);
                    if (type == 1) // miss sys incall not show notification
                        showNotification(displayname, it, true, R.drawable.missed,
                                getResources().getString(R.string.fafamissed_call));

                    int idx = mADB.getIdxByAddress(address);
                    if (idx > 0) {
                        Intent it2 = new Intent(Global.Action_InternalCMD);
                        it2.putExtra("Command", Global.CMD_TCP_MESSAGE_ARRIVAL);
                        it2.putExtra("originalSignal", "210/" + Integer.toHexString(idx) + "/" + Integer.toHexString((int) (time / 1000)) + "/<Z>"
                                + "Missed call");
                        sendBroadcast(it2);
                    }
                } else {
                    if (mSmsDB.isOpen()) {
                        mSmsDB.insertMessage(address, contact_id,
                                time, 1, -1, 1, "",
                                "(iCc) " + getString(R.string.call_duration) + " " + DateUtils.formatElapsedTime(duration), 0, null, null,
                                0, 0, 0, 0, displayname, null, 0);
                        Intent it = new Intent();
                        it.setAction(Global.Action_MsgGot);
                        sendBroadcast(it);
                    }
                }
            }
        }
    };

    final Runnable mUpdateWebCallLog = new Runnable() {
        public void run() {
            String myIpAddress = mPref.read("myIpAddress", "");
            String myGeoLocation = mPref.read("myGeoLocation", "");

            if (myGeoLocation.length() < 10 || myIpAddress.length() < 5) {
                try {
                    int count = 0;
                    String domain = getIsoDomain();  //tml*** china ip
                    do {
                        MyNet net = new MyNet(AireJupiter.this);
                        myIpAddress = net.doAnyPostHttp("http://" + domain + "/test/p2.php", null);
                        if (myIpAddress.length() > 0) break;
                        MyUtil.Sleep(500);
                    } while (++count < 3);
                } catch (Exception e) {
                }

                if (myIpAddress.length() > 0) {
                    mPref.write("myIpAddress", myIpAddress);
                    try {
                        int count = 0;
                        do {
                            MyNet net = new MyNet(AireJupiter.this);
                            myGeoLocation = net.doAnyPostHttp("http://geoip.maxmind.com/f?l=I1QhkLmrlQay&i=" + myIpAddress, null);
                            if (myGeoLocation.length() > 0) break;
                            MyUtil.Sleep(500);
                        } while (++count < 3);
                    } catch (Exception e) {
                    }

                    if (myGeoLocation.length() > 10)
                        mPref.write("myGeoLocation", myGeoLocation);
                }
            }

            try {
                String Return = "";
                int count = 0;
                String web_url = mPref.read("referenceURL", "");
                String encAddress = URLEncoder.encode(callLogBundle.getString("address"), "UTF-8");
                String domain = getIsoDomain();  //tml*** china ip
                do {
                    MyNet net = new MyNet(AireJupiter.this);
                    Return = net.doAnyPostHttps("http://" + domain + "/test/call_log.php",
                            "action=calllog" +
                                    "&username=" + encAddress +
                                    "&caller=" + URLEncoder.encode(myPhoneNumber, "UTF-8") +
                                    "&starttime=" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")).format(new Date()) +
                                    "&duration=" + callLogBundle.getInt("duration", 0) +
                                    "&phonenumber=" + encAddress +
                                    "&ipaddr=" + myIpAddress +
                                    "&geolocation=" + myGeoLocation +
                                    "&weburl=" + web_url);
                    if (Return.length() > 0) break;
                    MyUtil.Sleep(500);
                } while (++count < 3);
            } catch (Exception e) {
            }
        }
    };

    private KeyguardManager myKM = null;

    private boolean checkScreenLocked() {
        if (myKM == null) myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (myKM.inKeyguardRestrictedInputMode())
            return true;
        return false;
    }

    private ActivityManager mAm;

    private boolean shouldPopupDialog() {
        if (checkScreenLocked()) return true;
        mAm = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        List<ActivityManager.RunningTaskInfo> taskInfo = mAm.getRunningTasks(1);
        String name = taskInfo.get(0).topActivity.getClassName();
        Log.d("Top Activity:" + name);

        if (name.startsWith("com.pingshow.amper"))
            return true;

        return false;
    }

    private void onPopupDialog() {
        mNM.cancel(R.string.app_name);

        Intent i = new Intent(this, ConversationActivity.class);
        i.putExtra("ActivityType", 1);
        i.putExtra("SendeeNumber", msgGot.address);
        i.putExtra("SendeeContactId", msgGot.contactid);
        i.putExtra("SendeeDisplayname", msgGot.displayname);
        i.putExtra("audioPath", msgGot.att_path_aud);

        if (msgGot.content.startsWith("[<AGREESHARE>]")) {
            String[] res = msgGot.content.split(",");
            String address = mADB.getAddressByIdx(Integer.valueOf(res[1], 16));
            long id = cq.getContactIdByNumber(address);
            if (id > 0)
                address = cq.getNameByContactId(id);
            int relation = Integer.valueOf(res[2]);
            String content = AireJupiter.this
                    .getResources()
                    .getString(
                            R.string.agree_share_sms,
                            getResources().getStringArray(R.array.share_time)[relation - 1]);
            showNotification(msgGot.displayname + ": " + content, i, true,
                    R.drawable.icon_sms, null);
        } else {
            int interphoneType = -1;
            String TempContent = msgGot.content;
            if (TempContent.contains("(Vm)")) {
                TempContent = TempContent.replace("(Vm)",
                        getString(R.string.voicememo));
            }
            if (TempContent.startsWith("(iMG)")) {
                TempContent = TempContent.replace("(iMG)",
                        getString(R.string.picmsg));
            }
            if (TempContent.startsWith("(g.f")) {
                TempContent = getString(R.string.animated);
            }
            if (msgGot.attached == 4) {
                if (TempContent.contains("itph*")) {
                    interphoneType = Integer.parseInt(TempContent.substring(6)) - 1;
                    TempContent = getString(R.string.interphone_smile);
                } else if (msgGot.content.startsWith("(iPh)")) {
                    TempContent = getString(R.string.interphone);
                }
                //tml|alex*** rwt byebye X
//				i = new Intent(this, WalkieTalkieDialog.class);
//				i.putExtra("Address", msgGot.address);
//				i.putExtra("Contact_id", msgGot.contactid);
//				i.putExtra("SendeeDisplayname", msgGot.displayname);
//				i.putExtra("audioPath", msgGot.att_path_aud);
//				i.putExtra("interphoneType", interphoneType);
//				
//				if (mPref.readBoolean("wtSoundOut", true) && DialerActivity.getDialer()==null)
//				{
//					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//					startActivity(i);
//					return;
//				}
            }
            if (msgGot.attached == 8) {
                if (msgGot.content.startsWith(getString(R.string.video))
                        && msgGot.content.contains("(vdo)")) {
                    TempContent = getString(R.string.videomemo);
                } else if (msgGot.content
                        .startsWith(getString(R.string.filememo_recv))
                        && msgGot.content.contains("(fl)")) {
                    TempContent = getString(R.string.filememo_recv);
                }
            }
            showNotification(msgGot.displayname + ": " + TempContent, i, true,
                    R.drawable.icon_sms, null);
        }

        // Fetch call state, if the user is in a call or the phone is ringing we
        // don't want to show the popup
        TelephonyManager mTM = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        boolean callStateIdle = mTM.getCallState() == TelephonyManager.CALL_STATE_IDLE;

        if (!callStateIdle)
            return;
        if (DialerActivity.getDialer() != null)
            return;
        if (mPref.readBoolean("popupSms", true)
                && !mPref.readBoolean("vociemessaging", false)
                && WalkieTalkieDialog.playing == false
                && WalkieTalkieDialog.recording == false
                && !ConversationActivity.videoRecording
                && !VoiceRecordingDialog.voiceRording) {
            if (shouldPopupDialog()) {
                Intent popup = new Intent(this, PopupDialog.class);
                popup.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

                Bundle b = new Bundle();
                b.putString("EXTRAS_FROM_ADDRESS", msgGot.address);
                b.putString("EXTRAS_DISPLAY_NAME", msgGot.displayname);
                b.putString("EXTRAS_MESSAGE_BODY", msgGot.content);
                b.putLong("EXTRAS_TIME", msgGot.time);
                b.putLong("EXTRAS_CONTACT_ID", msgGot.contactid);
                b.putInt("EXTRAS_UNREAD_COUNT", 0);
                b.putInt("EXTRAS_ATTACHMENT", msgGot.attached);
                b.putString("EXTRAS_AUDIO_PATH", msgGot.att_path_aud);
                b.putString("EXTRAS_IMAGE_PATH", msgGot.att_path_img);
                b.putLong("EXTRAS_LONGITUDE", msgGot.longitudeE6);
                b.putLong("EXTRAS_LATITUDE", msgGot.latitudeE6);
                b.putInt("EXTRAS_SMS_TYPE", msgGot.type);

                popup.putExtras(b);
                PopupDialog p = PopupDialog.getInstance();
                if (p != null)
                    p.refresh(b);
                else
                    startActivity(popup);
            } else {

            }
        }
    }

    final Runnable HandleMessageComing = new Runnable() {
        public void run() {
            Intent it = new Intent();
            if (ConversationActivity.sender == null
                    || !ConversationActivity.sender.equals(msgGot.address)
                    || (msgGot.attached & 4) == 4) {
                if (mADB.isFafauser(msgGot.address)) {
                    onPopupDialog();
                }

                if (ConversationActivity.sender == null || !ConversationActivity.sender.equals(msgGot.address)) {
                    mADB.updateLastContactTimeByAddress(msgGot.address, new Date().getTime());
                    if (UsersActivity.sortMethod == 1)
                        UsersActivity.needRefresh = true;
                }
            } else {
                it.putExtra("autoPath", msgGot.att_path_aud);
                manySmsContent.setLength(0);
            }

            it.setAction(Global.Action_MsgGot);
            it.putExtra("msgAttach", msgGot.attached);
            it.putExtra("msgContent", msgContent);
            sendBroadcast(it);// Update fafa Lists
        }
    };

    public boolean downloadPhoto(int uid, String localfile) {
        return downloadPhoto(uid, localfile, 3);
    }

    public boolean downloadPhoto216(int uid, String localfile) {
        return downloadPhoto(uid, localfile, 216);
    }

    public boolean downloadPhoto(int uid, String localfile, int retry) {
        try {
            String remotefile = "profiles/thumbs/photo_" + uid + ".jpg";
            int success = 0;
            int count = 0;

            if (retry != 216) {
                do {
                    MyNet net = new MyNet(AireJupiter.this);
                    //bree
                    success = net.DownloadUserPhoto(remotefile, localfile);
//					success = net.Download(remotefile, localfile, myLocalPhpServer);
                    if (success == 1 || success == 0)
                        break;
                    MyUtil.Sleep(500);
                } while (++count < retry);
            }

            if (success != 1) {
                count = 0;
                do {
                    MyNet net = new MyNet(AireJupiter.this);
                    //bree
                    success = net.DownloadUserPhoto(remotefile, localfile);
//					success = net.Download(remotefile, localfile, null);
                    if (success == 1 || success == 0)
                        break;
                    MyUtil.Sleep(500);
                } while (++count < retry);
            }

            if (success == 1) {
                try {
                    mADB.updatePhotoByUID(uid, 1);
                } catch (Exception e) {
                    Log.e("Download photo1 !@#$ " + e.getMessage());
                }
                return true;
            }
        } catch (Exception e) {
            Log.e("Download photo2 !@#$ " + e.getMessage());
        }
        return false;
    }

    public boolean downloadAnyPhoto(String remotefile, String localfile, int retry, boolean dontTryLocalPhpServer) {
        try {
            int success = 0;
            int count = 0;
            if (!dontTryLocalPhpServer) {
                do {
                    MyNet net = new MyNet(AireJupiter.this);
                    //bree
//					success = net.Download(remotefile, localfile, myLocalPhpServer);
                    success = net.DownloadUserPhoto(remotefile, localfile);
                    if (success == 1 || success == 0)
                        break;
                    MyUtil.Sleep(500);
                } while (++count < retry);
            }

            if (success != 1) {
                count = 0;
                do {
                    MyNet net = new MyNet(AireJupiter.this);
                    //bree
//					success = net.Download(remotefile, localfile, null);
                    success = net.DownloadUserPhoto(remotefile, localfile);
                    if (success == 1 || success == 0)
                        break;
                    MyUtil.Sleep(500);
                } while (++count < retry);
            }
            if (success == 1) {
                return true;
            }
        } catch (Exception e) {
            Log.e("downloadAnyPhoto !@#$ " + e.getMessage());
        }
        return false;
    }

    /**
     * zhao when net connect,send pending sms again
     */
    public void sendPendingSMS() {
        Cursor cursor = mSmsDB.fetchMessageByStatus(2);
        if (cursor == null) return;//alec
        try {
            myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
            if (cursor.getCount() > 0) {
                Log.d("sendPendingSMS....Start Count:" + cursor.getCount());
                while (cursor.moveToNext()) {
                    int fail_count = cursor.getInt(cursor.getColumnIndexOrThrow(SmsDB.KEY_FAIL_COUNT));
                    if (fail_count >= 2) {
                        continue;
                    }
                    String SendeeNumber = cursor.getString(cursor.getColumnIndexOrThrow(SmsDB.KEY_ADDRESS));
                    String mMsgText = cursor.getString(cursor.getColumnIndexOrThrow(SmsDB.KEY_BODY));
                    if (mMsgText.contains("[<AGREESHARE>]")) continue;//alec
                    int mAttached = cursor.getInt(cursor.getColumnIndex(SmsDB.KEY_ATTACH_TYPE));
                    String SrcAudioPath = cursor.getString(cursor.getColumnIndexOrThrow(SmsDB.KEY_VMEMO));
                    String SrcImagePath = cursor.getString(cursor.getColumnIndexOrThrow(SmsDB.KEY_IMAGE));
                    int idx = mADB.getIdxByAddress(SendeeNumber);
                    SendAgent agent = new SendAgent(this, myIdx, idx, false);
                    mMsgText = Global.Temp_Parse + mMsgText;  //tml*** 200timeout
                    agent.onSend(SendeeNumber, mMsgText, mAttached, SrcAudioPath, SrcImagePath, false);
                    agent.setRowId(cursor.getInt(cursor.getColumnIndex(SmsDB.KEY_SMS_ID)));
                    mSmsDB.updateFailCountById(cursor.getInt(0), ++fail_count);

                    if (SrcImagePath != null || SrcAudioPath != null)
                        Thread.sleep(2000);
                    else
                        Thread.sleep(500);
                }
            } else {
                Log.e("sendPendingSMS no action");
            }
        } catch (Exception e) {
            Log.e("sendPendingSMS !@#$ " + e.getMessage());
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }
        Log.d("sendPendingSMS....Over");
    }

    ;

    public void saveContactDrawableToFile(int idX, long contactId) {
        if (contactId > 0 && MyUtil.checkSDCard(this)) {
            Drawable photo = cq.getPhotoById(this, contactId, false);
            if (photo != null) {
                Bitmap bitmap = ((BitmapDrawable) photo).getBitmap();
                String DstThumbPath = Global.SdcardPath_inbox + "photo_" + idX + ".jpg";
                ResizeImage.ResizeXY(this, bitmap, DstThumbPath, 100, 90);  //tml*** bitmap quality, 70>90
                //TODO
                String DstImagePath = Global.SdcardPath_inbox + "photo_" + idX + "b.jpg";
                ResizeImage.ResizeXY(this, bitmap, DstImagePath, 320, 100);  //tml*** bitmap quality, 240->320
            }
        }
    }

    public void doCheckPhotoFromNet() {
        if (!MyUtil.checkSDCard(AireJupiter.this)) return;
        Log.d("doCheckPhotoFromNet");

        Cursor c = mADB.fetchPhotoVersion();
        if (c == null) return;
        StringBuffer idxBuffer = new StringBuffer("");
        if (c.moveToFirst()) {
            do {
                int idx = c.getInt(1);
                idxBuffer.append(Integer.toHexString(idx) + "+");
            } while (c.moveToNext());
        }

        if (c != null && !c.isClosed())
            c.close();

        if (idxBuffer.toString().length() != 0) {
            try {
                MyNet net = new MyNet(AireJupiter.this);
                String Return = net.doPost("queryphotoAll.php", "idx=" + idxBuffer.substring(0, idxBuffer.toString().length() - 1), null);
                if (Return.length() > 5) {
                    Return = Return.substring(5);
                    if (Return.length() > 0) {
                        String[] versions = Return.split("\\+");
                        String[] idxs = idxBuffer.substring(0, idxBuffer.toString().length() - 1).split("\\+");
                        if (versions.length > 0) {
                            boolean result = false;
                            for (int i = 0; i < versions.length; i++) {
                                int idx10 = Integer.valueOf(idxs[i], 16);
                                int fafaVersion = mADB.getPhotoVersionByIdx(idx10);
                                int netVersion = 0;
                                boolean ret;
                                try {
                                    netVersion = Integer.valueOf(versions[i], 16);
                                    if (fafaVersion != netVersion && netVersion != 0) {
                                        String localfile = Global.SdcardPath_inbox + "photo_" + idx10 + ".jpg";
                                        ret = downloadPhoto(idx10, localfile);
                                        if (ret)//delete big one
                                            new File(Global.SdcardPath_inbox + "photo_" + idx10 + "b.jpg").delete();
                                        result |= ret;
                                        MyUtil.Sleep(500);
                                    }
                                } catch (Exception e) {
                                }
                            }

                            if (result) {
                                Intent intent = new Intent(Global.Action_Refresh_Gallery);
                                sendBroadcast(intent);
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }


    private void downloadBigPhotoFromNet() {
        if (!MyUtil.checkSDCard(AireJupiter.this)) return;
        if (!new NetInfo(AireJupiter.this).isConnected()) return;

        int time = new Date().getHours();
        if (time >= 2 && time < 8) // early in the morning
        {
            Log.d("downloadBigPhotoFromNet at 0" + time + ":00");
            Cursor c = mADB.fetchAll();
            if (c != null && c.moveToFirst()) {
                do {
                    int idx = c.getInt(3);
                    String address = c.getString(1);
                    if (!address.startsWith("[<GROUP>]")) {
                        String localPhoto = Global.SdcardPath_inbox + "photo_" + idx + "b.jpg";
                        if (!new File(localPhoto).exists()) {
                            int count = 0;
                            int success = 0;
                            String remotefile = "profiles/photo_" + idx + ".jpg";
                            do {
                                try {
                                    MyNet net = new MyNet(AireJupiter.this);
                                    success = net.Download(remotefile, localPhoto, myLocalPhpServer);
                                } catch (Exception e) {
                                }
                                if (success == 1 || success == 0)
                                    break;
                                MyUtil.Sleep(500);
                            } while (++count < 2);

                            if (success != 1) {
                                count = 0;
                                do {
                                    try {
                                        MyNet net = new MyNet(AireJupiter.this);
                                        success = net.Download(remotefile, localPhoto, null);
                                    } catch (Exception e) {
                                    }
                                    if (success == 1 || success == 0)
                                        break;
                                    MyUtil.Sleep(500);
                                } while (++count < 2);
                            }
                        }
                        MyUtil.Sleep(500);
                    }
                } while (c.moveToNext());
                c.close();
            }
        }
    }

    Runnable downloadFriendList = new Runnable() {
        public void run() {

            new Thread(new Runnable() {
                public void run() {
                    List<RelatedUserInfo> PossibleList = null;
                    try {
                        myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
                        Log.d("downloadFriendList(buddylist) " + myIdx);
                        int count = 0;
                        do {
                            MyNet net = new MyNet(AireJupiter.this);
                            PossibleList = net.doPostHttpWithXML("getbuddylist_aire.php", "idx=" + myIdx, null);
                            if (PossibleList != null) break;
                            MyUtil.Sleep(1500);
                        } while (count++ < 3);
                    } catch (Exception e) {
                    }

                    if (PossibleList != null) {
                        boolean result = false;
                        if (mRDB.isOpen() && mADB.isOpen() && PossibleList.size() > 0) {
                            String unknown = getString(R.string.unknown_person);
                            String nickname;
                            int idx;
                            for (int i = 0; i < PossibleList.size(); i++) {
                                RelatedUserInfo r = PossibleList.get(i);
                                idx = r.getIdx();
                                if (idx == myIdx) continue;
                                String address = r.getAddress();
                                if (address.length() < 6) continue;
                                if (mRDB.isUserBlocked(address) == 1 || mADB.isUserBlocked(address) == 1)
                                    continue;
                                if (mADB.isFafauser(address) || mRDB.isFafauser(address)) continue;
                                nickname = r.getNickName();
                                if (nickname == null) continue;
                                if (nickname.length() < 2 || nickname.equals(unknown) || nickname.equals("null"))
                                    continue;

                                if (mADB.insertUser(address, idx, nickname) > 0) {
                                    ContactsOnline.setContactOnlineStatus(address, 0);
                                    long contactId = cq.getContactIdByNumber(address);
                                    if (contactId > 0)
                                        saveContactDrawableToFile(idx, contactId);
                                    result = true;
                                }
                            }
                        }

                        if (result) {
                            UsersActivity.forceRefresh = true;
                            Intent intent = new Intent(Global.Action_Refresh_Gallery);
                            sendBroadcast(intent);

                            getFriendNicknames();

                            doCheckPhotoFromNet();

                            getFriendMoods();
                        }

                        Log.d("load mADB DONE " + PossibleList.size() + result);
                    } else {
                        Log.d("getbuddylist_aire.php NULL");
                    }
                }
            }).start();
        }
    };

    // TODO: 2016/4/7 查询grouplist 登录或当friendlist列表发生改变的时候调用
    Runnable downloadGroupList = new Runnable() {
        public void run() {

            new Thread(new Runnable() {
                public void run() {
                    try {
                        myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
                        Log.d("downloadGroupList(grouplist) " + myIdx);
                        int count = 0;
                        MyNet net = new MyNet(AireJupiter.this);
                        do {
                            // TODO: 2016/4/7 解析XML存入数据库
                            String response  = net.doPostHttps("get_group_list_aire.php", "id=" + myIdx, null);

                            android.util.Log.d("查询group列表", "groupList:----"+response);
                            // TODO: 2016/4/8 解析xml
                            groups = parseXML(response);
                            if (groups !=null)break;
                            MyUtil.Sleep(1500);
                        } while (count++ < 3);
                    } catch (Exception e) {
                    }

                    if (groups != null) {
                        if (mGDB.isOpen() && groups.size() > 0) {
                            String groupName;
                            int groupIdx;
                            for (int i = 0; i < groups.size(); i++) {
                                GroupEntity group = groups.get(i);
                                groupIdx = group.getIdx();
                                groupName = group.getNn();
                                mGDB.insertGroup(groupIdx,groupName,0);
                                mADB.insertUser("[<GROUP>]" + groupIdx, groupIdx + 100000000,
                                        groupName);
                                //下载图片
                                try {
                                    String localfile = Global.SdcardPath_inbox + "photo_" + groupIdx + ".jpg";
                                    File f = new File(localfile);
                                    if (!f.exists()) {
                                        String remotefile = "groups/photo_" + groupIdx + ".jpg";
                                        downloadAnyPhoto(remotefile, localfile, 3, true);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            //刷新界面
                            UsersActivity.forceRefresh = true;
                            Intent intent = new Intent(Global.Action_Refresh_Gallery);
                            sendBroadcast(intent);
                        }
                        android.util.Log.d("查询group列表", "groups.size():" + groups.size());
                    } else {
                        Log.d("get_group_list_aire.php NULL");
                    }
                }
            }).start();
        }
    };

    private List<GroupEntity> parseXML(String response) {

        try {
            XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = xmlPullParserFactory.newPullParser();
            parser.setInput(new StringReader(response));

            int eventType = parser.getEventType();
            GroupEntity groupEntity = null;
            List<GroupEntity> groups = null;
            while(eventType != XmlPullParser.END_DOCUMENT)  {
                switch (eventType){
                    case XmlPullParser.START_DOCUMENT: //开始读取XML文档
                        //实例化集合类
                        groups = new ArrayList<>();
                        break;
                    case XmlPullParser.START_TAG://开始读取某个标签
                        if("g".equals(parser.getName())) {
                            //通过getName判断读到哪个标签，然后通过nextText()获取文本节点值，或通过getAttributeValue(i)获取属性节点值
                            groupEntity = new GroupEntity();
                        }else if ("idx".equals(parser.getName())) {
                            groupEntity.setIdx(Integer.parseInt(parser.nextText()));
                        }else if("nn".equals(parser.getName())){
                            groupEntity.setNn(parser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG://读完一个Person，可以将其添加到集合类中
                        if ("g".equals(parser.getName())){
                            groups.add(groupEntity);
                        }
                        break;
                }
                eventType = parser.next();
            }
            return groups;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void getFriendNicknames() {
        Log.d("doGetFriendNicknames");
        Cursor c = mADB.fetchAll();
        if (c == null) return;
        StringBuffer idxBuffer = new StringBuffer("");

        if (c.moveToFirst()) {
            do {
                int idx = c.getInt(3);
                String address = c.getString(1);
                if (idx == myIdx) continue;
                if (address.startsWith("[<GROUP>]")) continue;
                idxBuffer.append(Integer.toHexString(idx) + "+");
            } while (c.moveToNext());
        }

        if (c != null && !c.isClosed())
            c.close();

        c = mRDB.fetchAll();
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    int idx = c.getInt(3);
                    idxBuffer.append(Integer.toHexString(idx) + "+");
                } while (c.moveToNext());
            }

            if (c != null && !c.isClosed())
                c.close();
        }

        if (idxBuffer.toString().length() > 0) {
            try {
                int count = 0;
                do {
                    MyNet net = new MyNet(AireJupiter.this);

                    Log.d(idxBuffer.substring(0, idxBuffer.toString().length() - 1));

                    String Return = net.doPost("getusernickname.php", "idx=" + idxBuffer.substring(0, idxBuffer.toString().length() - 1), null);
                    if (Return.length() > 5 && Return.startsWith("Done")) {
                        Return = Return.substring(5);
                        if (Return.length() > 0) {
                            String[] idxs = idxBuffer.substring(0, idxBuffer.toString().length() - 1).split("\\+");
                            Pattern p = Pattern.compile("<Z->");
                            String[] nicknames = p.split(Return, idxs.length + 1);
                            if (nicknames.length > 0) {
                                for (int i = 0; i < nicknames.length; i++) {
                                    try {
                                        int idx10 = Integer.valueOf(idxs[i], 16);
                                        mADB.updateNicknameByUID(idx10, nicknames[i]);
                                        mRDB.updateNicknameByUID(idx10, nicknames[i]);
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                        Intent intent = new Intent(Global.Action_Refresh_Gallery);
                        sendBroadcast(intent);
                        break;
                    }
                    MyUtil.Sleep(1500);
                } while (count++ < 3);
            } catch (Exception e) {
            }
        }
    }

    public void getFriendMoods() {
        Log.d("getFriendMoods");
        long last = mPref.readLong("last_time_checking_mood", 0);
        long now = new Date().getTime();
        if (now - last < 360000) // 1 hour
            return;// no need to update
        Cursor c = mADB.fetchAll();
        if (c == null) return;
        StringBuffer idxBuffer = new StringBuffer("");

        if (c.moveToFirst()) {
            do {
                int idx = c.getInt(3);
                String address = c.getString(1);
                if (address.startsWith("[<GROUP>]")) continue;
                idxBuffer.append(Integer.toHexString(idx) + "+");
            } while (c.moveToNext());
        }

        if (c != null && !c.isClosed())
            c.close();

        if (idxBuffer.toString().length() > 0) {
            try {
                int count = 0;
                myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
                do {
                    MyNet net = new MyNet(AireJupiter.this);
                    String Return = net.doPost("getusermood.php", "idx=" + idxBuffer.substring(0, idxBuffer.toString().length() - 1), null);
                    if (Return.length() > 5 && Return.startsWith("Done")) {
                        Return = Return.substring(5);
                        if (Return.length() > 0) {
                            String[] idxs = idxBuffer.substring(0, idxBuffer.toString().length() - 1).split("\\+");
                            Pattern p = Pattern.compile("<Z->");
                            String[] moodText = p.split(Return, idxs.length + 1);
                            if (moodText.length > 0) {
                                for (int i = 0; i < moodText.length; i++) {
                                    if (moodText[i] != null && moodText[i].length() > 0) {
                                        int idx10 = Integer.valueOf(idxs[i], 16);
                                        if (idx10 == myIdx)
                                            mPref.write("moodcontent", moodText[i]);
                                        else
                                            mADB.updateMoodByUID(idx10, moodText[i]);
                                    }
                                }
                            }
                        }
                        break;
                    }
                    MyUtil.Sleep(1500);
                } while (count++ < 3);
            } catch (Exception e) {
            }
        }

        mPref.writeLong("last_time_checking_mood", now);
    }

    public void downloadRelatedUsersPhotos() {
        if (!MyUtil.checkSDCard(AireJupiter.this)) return;
        Log.d("downloadRelatedUsersPhotos");
        long last = mPref.readLong("downloadRelatedUsersPhotos", 0);
        long now = new Date().getTime();
        if (now - last < 30000) // 30 sec
            return;// no need to update
        Cursor c = mRDB.fetchPhotoVersion();
        StringBuffer idxBuffer = new StringBuffer("");
        if (c.moveToFirst()) {
            do {
                int idx = c.getInt(1);
                idxBuffer.append(Integer.toHexString(idx) + "+");
            } while (c.moveToNext());
        }

        if (c != null && !c.isClosed())
            c.close();

        if (idxBuffer.toString().length() != 0) {
            try {
                MyNet net = new MyNet(AireJupiter.this);
                String Return = net.doPost("queryphotoAll.php", "idx=" + idxBuffer.substring(0, idxBuffer.toString().length() - 1), null);
                if (Return.length() > 5) {
                    Return = Return.substring(5);
                    if (Return.length() > 0) {
                        String[] versions = Return.split("\\+");
                        String[] idxs = idxBuffer.substring(0, idxBuffer.toString().length() - 1).split("\\+");
                        if (versions.length > 0) {
                            boolean result = false;
                            for (int i = 0; i < versions.length; i++) {
                                int idx10 = Integer.valueOf(idxs[i], 16);
                                int fafaVersion = mRDB.getPhotoVersionByIdx(idx10);
                                int netVersion = 0;
                                try {
                                    netVersion = Integer.valueOf(versions[i], 16);
                                    if (fafaVersion != netVersion && netVersion != 0) {
                                        String localfile = Global.SdcardPath_inbox + "photo_" + idx10 + ".jpg";
                                        result |= downloadPhoto(idx10, localfile);
                                        MyUtil.Sleep(500);
                                    }
                                } catch (Exception e) {
                                }
                            }

                            if (result) {
                                Intent intent = new Intent(Global.Action_Refresh_Gallery);
                                sendBroadcast(intent);
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        mPref.writeLong("downloadRelatedUsersPhotos", now);
    }

    private void copyFromPackage(int ressourceId, String target)
            throws IOException {
        FileOutputStream lOutputStream = openFileOutput(target, 0);
        InputStream lInputStream = getResources().openRawResource(ressourceId);
        int readByte;
        byte[] buff = new byte[8048];
        while ((readByte = lInputStream.read(buff)) != -1) {
            lOutputStream.write(buff, 0, readByte);
        }
        lOutputStream.flush();
        lOutputStream.close();
        lInputStream.close();
    }

    private void copyAssetsFromPackage() throws IOException {
        File lFileToCopy = new File(helper_photo_path[0]);
        if (!lFileToCopy.exists())
            copyFromPackage(R.raw.helper_1, lFileToCopy.getName());
    }

    //tml*** temp alpha ui?
    public static final String[] fafaHelpers = {"support", "news_service"};
    public static final String[] fafaHelperNickname = {"Support", "Hot News"};
    public static final int[] helperIdx = {2, 4};
    private static String helper_photo_path[] = {
            "/data/data/com.pingshow.amper/files/help_1.jpg",
            "/data/data/com.pingshow.amper/files/help_1.jpg"};

    private void addAmperHelpers() {
        if (!mADB.isFafauser(fafaHelpers[1])) {
            try {
                copyAssetsFromPackage();
            } catch (IOException e) {
            }
            for (int i = 0; i < fafaHelpers.length; i++) {
                mADB.insertUser(fafaHelpers[i], helperIdx[i], fafaHelperNickname[i]);
                mADB.updatePhotoByUID(helperIdx[i], 1);
            }
        }
    }

    Runnable popupWelcomeDialog = new Runnable() {
        public void run() {
            //tml*** beta ui, removed
//			if (!mPref.readBoolean("ProfileCompleted",false))
//			{
//				mHandler.postDelayed(popupWelcomeDialog, 25000);
//				return;
//			}
//			Intent it = new Intent(Global.Action_InternalCMD);
//			long now=new Date().getTime()/1000;
//			it.putExtra("Command", Global.CMD_TCP_MESSAGE_ARRIVAL);
//			it.putExtra("originalSignal", "210/2/"+Integer.toHexString((int)now)+"/<Z>"+getString(R.string.aire_welcome_desc));
//			sendBroadcast(it);
        }
    };

    void tellFriendsProfileChanged(int mode, String newMood) {
        notifying = true;
        Cursor c = mADB.fetchAll();
        if (c.moveToFirst()) {
            myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
            String text;
            if (mode == 0)// photo
                text = "[<NEWPHOTO>]";
            else
                text = "[<NEWMOOD>]" + newMood;
            do {
                if (c.getInt(3) > 50) {
                    String address = c.getString(1);
                    int receiver = mADB.getIdxByAddress(address);
                    if (ContactsOnline.getContactOnlineStatus(address) > 0 && !address.startsWith("[<GROUP>]")) {
                        try {
                            Log.i("tml tellFriendsProfileChanged! > " + address);
                            //BREE
                            if (mode == 2) {
                                AireJupiter
                                        .getInstance()
                                        .tcpSocket()
                                        .sendCmd(Integer.toHexString(receiver) + "", "{\"cmd\":UpdateNickname,\"nickname\":" + newMood + "}");
                            } else {
                                SendAgent agent = new SendAgent(AireJupiter.this, myIdx, 0, false);
                                agent.onSend(address, text, 0, null, null, true);
                            }
                        } catch (Exception e) {
                        }
                        MyUtil.Sleep(1000);
                    }
                }
            } while (c.moveToNext());
        }
        if (c != null && !c.isClosed())
            c.close();

        notifying = false;
    }


    String mDownload_HyperLink = "http://71.19.247.49/downloads/aire-android.apk";

    Runnable mDownloadnUpdate = new Runnable() {
        public void run() {
            if (mDownload_HyperLink.length() > 0) {
                try {
                    MyNet net = new MyNet(getApplicationContext());
                    String localfile = Global.SdcardPath_downloads + "AIRE.apk";
                    if (net.anyDownload(mDownload_HyperLink, localfile)) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse("file://" + localfile),
                                "application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    Log.e("Download APK !@#$ " + e.getMessage());
                }
            }
        }
    };

    //tml*** beta ui
    public static boolean checkShow = false;
    Runnable checkVersion_go = new Runnable() {
        public void run() {
            Log.d("tml checkVersion_go");
            checkVersionUpdate(true);
        }
    };

    public void forceCheckUpdate() {
        Thread thr_connection = new Thread(checkVersion_go, "forceCheckUpdate");
        thr_connection.start();
    }
    //***tml

    private void checkVersionUpdate(boolean checknow) {
        if (!checknow) {  //tml*** beta ui
            long last = mPref.readLong("last_checking_version", 0);
            long now = new Date().getTime();
            if (now - last < 18000000) // 5hrs
                return;// no need to check
            mPref.writeLong("last_checking_version", now);
        }

        Log.d("check Version Update...");
        MyNet net = new MyNet(AireJupiter.this);
        String Return = net.doPost("checkaireversion.php", "detail=0", null);
//		String Return = "";
        if (Log.enDEBUG) {  //tml*** alt update
            Return = net.doPost("checkaireversion.php", "detail=0", null);
        } else {
            Return = net.doPost("checkaireversion.php", "detail=0", null);
        }
        if (Return.length() > 0) {
            int latest_versionCode = 1;
            Pattern p = Pattern.compile(";");
            String[] items = p.split(Return, 10);
            if (items[0].startsWith("latest_version_code"))
                latest_versionCode = MyUtil.getIntValue(items[0], 0);

            if (latest_versionCode > versionCode) {
                Log.d("New version found! " + latest_versionCode);
                Return = net.doPost("checkaireversion.php", "detail=1", null);
                if (Log.enDEBUG) {  //tml*** alt update
                    Return = net.doPost("checkaireversion.php", "detail=1", null);
                } else {
                    Return = net.doPost("checkaireversion.php", "detail=1", null);
                }
                p = Pattern.compile(";");
                items = p.split(Return, 10);

                int forceUpdate = MyUtil.getIntValue(items[1], 0);
                String version_Name = MyUtil.getStringValue(items[3]);

                String title = String.format(getString(R.string.new_version_found), version_Name);

                if (forceUpdate > 0) title += getString(R.string.force_update);

                title += (" " + getString(R.string.dont_worry));
                mDownload_HyperLink = MyUtil.getStringValue(items[4]);
                Intent it = new Intent(AireJupiter.this, CommonDialog.class);
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                it.putExtra("msgContent", title);
//				it.putExtra("numItems", 3);
                if (Log.enDEBUG) {  //tml*** alt update
                    it.putExtra("numItems", 2);
                } else {
                    it.putExtra("numItems", 3);
                }
                it.putExtra("ItemCaption0", getString(R.string.cancel));
                it.putExtra("ItemResult0", 0);
                it.putExtra("ItemCaption1", getString(R.string.download));
                it.putExtra("ItemResult1", CommonDialog.DOWNLOAD);
//				it.putExtra("ItemCaption2", getString(R.string.update));
//				it.putExtra("ItemResult2", CommonDialog.GOOGLEPLAY);
                if (!Log.enDEBUG) {  //tml*** alt update
                    it.putExtra("ItemCaption2", getString(R.string.update));
                    if (!MyUtil.isISO_China(AireJupiter.this, mPref, null)) {
                        it.putExtra("ItemResult2", CommonDialog.GOOGLEPLAY);
                    } else {
                        it.putExtra("ItemResult2", CommonDialog.CHINAPLAY);
                    }
                }
                startActivity(it);
            } else {  //tml*** beta ui
                if (checkShow) {
                    String title = (getString(R.string.no) + " " + getString(R.string.update));
                    Intent it = new Intent(AireJupiter.this, CommonDialog.class);
                    it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    it.putExtra("msgContent", title);
                    it.putExtra("numItems", 1);
                    it.putExtra("ItemCaption0", getString(R.string.done));
                    it.putExtra("ItemResult0", 0);
                    startActivity(it);
                    checkShow = false;
                }
            }
        }
    }

    private class systemNumberChange extends ContentObserver {
        public systemNumberChange(Handler handler) {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            cq.clearContactCursor();
        }
    }


    Runnable clearSDCard = new Runnable() {
        public void run() {
            try {
                int myIdx = Integer.valueOf(mPref.read("myID", "0"), 16);
                String currentPath = Global.SdcardPath_inbox;
                File[] files = new File(currentPath).listFiles();
                for (File file : files) {
                    String fn = file.getName();
                    if (fn.startsWith("photo_")) {
                        try {
                            int end = fn.indexOf("b.jpg");
                            if (end != -1) {
                                String a = fn.substring(6, end);
                                int idx = Integer.parseInt(a);
                                if (myIdx == idx) continue;
                                if (!mADB.isFafauser(idx) && !mRDB.isFafauser(idx))
                                    file.delete();
                            } else {
                                end = fn.indexOf(".jpg");
                                if (end != -1) {
                                    String a = fn.substring(6, end);
                                    int idx = Integer.parseInt(a);
                                    if (myIdx == idx) continue;
                                    if (!mADB.isFafauser(idx) && !mRDB.isFafauser(idx))
                                        file.delete();
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    };

    private Runnable searchFriendsByPhonebook_delayed = new Runnable() {
        public void run() {
            new Thread(searchFriendsByPhonebook).start();
        }
    };

    private Runnable searchFriendsByPhonebook = new Runnable() {
        public void run() {
            if (!new NetInfo(AireJupiter.this).isConnected()) return;

            if (!mPref.readBoolean("ProfileCompleted", false)) {
                mHandler.postDelayed(searchFriendsByPhonebook_delayed, 11000);
                return;
            }

            new Thread(new Runnable() {
                public void run() {
                    long last = mPref.readLong("last_query_sid_time", 0);

                    long now = new Date().getTime();
                    if (now - last < 3600000) // 1 hours
                        return;// no need to check

                    if (DialerActivity.getDialer() != null) {
                        mHandler.postDelayed(searchFriendsByPhonebook_delayed, 30000);
                        return;
                    }

                    Log.d("Query By Phonebook");

                    mPref.writeLong("last_query_sid_time", now);

                    String addr = "";

                    try {
                        Cursor cursor = getContentResolver().query(
                                CommonDataKinds.Phone.CONTENT_URI,
                                new String[]{CommonDataKinds.Phone.CONTACT_ID,
                                        CommonDataKinds.Phone.NUMBER}, null,
                                null, CommonDataKinds.Phone.LAST_TIME_CONTACTED + " desc");

                        if (cursor.moveToFirst()) {
                            String phonenumber;
                            String lastn = "";
                            int i = 0;
                            do {
                                phonenumber = MyTelephony.cleanPhoneNumber(cursor.getString(1));
                                if (phonenumber != null && phonenumber.length() >= 7) {

                                    if (MyTelephony.validWithCurrentISO(phonenumber))
                                        phonenumber = MyTelephony.addPrefixWithCurrentISO(phonenumber);

                                    if (!phonenumber.startsWith("+"))
                                        phonenumber = MyTelephony.attachPrefix(AireJupiter.this, phonenumber);

                                    if (!phonenumber.equals(myPhoneNumber) && !mADB.isFafauser(phonenumber)) {
                                        if (phonenumber.startsWith("+"))//alec
                                        {
                                            if (!phonenumber.equals(lastn)) {
                                                if (i != 0) addr += ",";
                                                addr += phonenumber;
                                                i++;
                                                lastn = phonenumber;
                                                if (i > 360) break;
                                            }
                                        }
                                    }
                                }
                            } while (cursor.moveToNext());
                        }
                        if (cursor != null && !cursor.isClosed())
                            cursor.close();

                    } catch (Exception e) {
                    }

                    List<RelatedUserInfo> PossibleList = null;

                    try {
                        int count = 0;
                        do {
                            MyNet net = new MyNet(AireJupiter.this);
                            PossibleList = net.doPostHttpWithXML("queryusers_aire.php", "addr=" + URLEncoder.encode(addr, "UTF-8"), null);
                            if (PossibleList != null) break;
                            MyUtil.Sleep(1500);
                        } while (count++ < 3);
                    } catch (Exception e) {
                    }

                    if (PossibleList != null) {
                        boolean result = false;
                        try {
                            String myIdHex = mPref.read("myID", "0");
                            myIdx = Integer.parseInt(myIdHex, 16);

                            if (mRDB.isOpen() && mADB.isOpen() && PossibleList.size() > 0) {
                                String myNickname = mPref.read("myNickname");
                                String nickname;
                                int idx;
                                for (int i = 0; i < PossibleList.size(); i++) {
                                    RelatedUserInfo r = PossibleList.get(i);
                                    idx = r.getIdx();
                                    String address = r.getAddress();
                                    if (mRDB.isUserBlocked(address) == 1 || mADB.isUserBlocked(address) == 1)
                                        continue;
                                    if (mADB.isUserDeleted(address)) continue;
                                    if (mADB.isFafauser(address)) continue;
                                    if (idx == myIdx) continue;
                                    nickname = r.getNickName();
                                    if (mADB.insertUser(address, idx, nickname) > 0) {
                                        mRDB.deleteContactByAddress(address);
                                        ContactsOnline.setContactOnlineStatus(address, 1);
                                        if (tcpSocket.isLogged(false)) {
                                            tcpSocket.send(address, "[<NEWUSERJOINS>]" + myIdHex + "<Z>" + myPhoneNumber + "<Z>" + myNickname, 0, null, null, 0, null);
                                            MyUtil.Sleep(500);
                                        }
                                        result = true;
                                    }
                                }
                            }
                        } catch (Exception e) {
                        }

                        if (result) {
                            Intent intent = new Intent(Global.Action_Refresh_Gallery);
                            sendBroadcast(intent);
                        }

                        Intent intent = new Intent();
                        intent.setAction(Global.Action_InternalCMD);
                        intent.putExtra("Command", Global.CMD_DOWNLOAD_PHOTO_FROMNET);
                        intent.putExtra("type", 0);
                        sendBroadcast(intent);
                    }
                }
            }).start();
        }
    };

    public String getFriendLocation(String address) {
        if (!mADB.isOpen()) return null;
        int idx = mADB.getIdxByAddress(address);
        if (idx < 0) return "nonmember";

        String Return = "";
        try {
            myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
            int c = 0;
            do {
                MyNet net = new MyNet(AireJupiter.this);
                Return = net.doPost("getlocation_aire.php", "idx=" + myIdx + "&who=" + idx, null);
                if (Return.length() > 5) break;
                MyUtil.Sleep(500);
            } while (c++ < 2);
        } catch (Exception e) {
        }

        if (Return.startsWith("Done")) {
            return Return.substring(5);
        }

        return "";
    }


    Runnable getRWTServerIP = new Runnable() {
        public void run() {
            //tml|alex*** rwt byebye X
//			new Thread(new Runnable(){
//				public void run() 
//				{
//					Log.d("getRWTServerIP");
//					long last = mPref.readLong("last_time_get_rwt_server", 0);
//					long now = new Date().getTime();
//					if (now - last < 3600000) // 1 hours
//						return;// no need to update
//					String Return="";
//					try {
//						int count = 0;
//						myIdx=Integer.parseInt(mPref.read("myID","0"),16);
//						String iso=mPref.read("iso","tw");
//						String lang=Locale.getDefault().getLanguage();
//						do {
//							MyNet net = new MyNet(AireJupiter.this);
//							Return = net.doPost("getrwtservice.php","idx="+myIdx+
//									"&iso="+iso+
//									"&lang="+lang
//									,null);
//							if (Return.startsWith("Done"))
//								break;
//							MyUtil.Sleep(2500);
//						} while (++count < 3);
//					} catch (Exception e) {}
//					
//					if (Return.length()>10)
//					{
//						Return=Return.substring(5);
//						try{
//							String [] items=Return.split(":");
//							int port=Integer.parseInt(items[1]);
//							mPref.write("RWTServerIP", items[0]);
//							mPref.write("RWTServerPort", port);
//						}catch (Exception e){}
//						
//						mPref.writeLong("last_time_get_rwt_server", now);
//					}
//				}
//			}).start();
        }
    };

    Runnable getConferenceServiceIP = new Runnable() {
        public void run() {
            new Thread(new Runnable() {
                public void run() {
                    long last = mPref.readLong("last_time_get_conference_server", 0);
                    long now = new Date().getTime();
                    if (now - last < 1) { // 12 hours, 43200000
                        Log.d("getConferenceServiceIP - NOT yet");
                        return;// no need to update
                    }
                    String Return = "";
                    try {
                        int count = 0;
                        myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
                        String iso = mPref.read("iso", "tw");
                        String lang = Locale.getDefault().getLanguage();
                        long latitude = mPref.readLong("latitude", 39976279);
                        long longitude = mPref.readLong("longitude", 116349386);
                        Log.d("getConferenceServiceIP " + "idx=" + myIdx + "&iso=" + iso + "&lang=" + lang +
                                "&lat=" + latitude + "&lon=" + longitude + "&os=and");
                        do {
                            MyNet net = new MyNet(AireJupiter.this);
                            Return = net.doPost("getconferenceservice.php", "idx=" + myIdx +
                                    "&iso=" + iso +
                                    "&lang=" + lang +
                                    "&lat=" + latitude +
                                    "&lon=" + longitude +
                                    "&os=and"
                                    , null);
                            if (Return.startsWith("Done="))
                                break;
                            MyUtil.Sleep(2500);
                        } while (++count < 3);
                    } catch (Exception e) {
                        Log.e("getConferenceServiceIP !@#$ " + e.getMessage());
                    }
                    if (Return.startsWith("Done=") && Return.length() > 10) {
                        Return = Return.substring(5);
                        try {
                            mPref.write("conferenceSipServer", Return);
                        } catch (Exception e) {
                        }
                    }

                    mPref.writeLong("last_time_get_conference_server", now);
                }
            }).start();
        }
    };

    Runnable getFreeswitchServiceIP = new Runnable() {
        public void run() {
            new Thread(new Runnable() {
                public void run() {
                    Log.d("getFreeswitchServiceIP");
                    long last = mPref.readLong("last_time_get_freeswitch_server", 0);
                    long now = new Date().getTime();
                    if (now - last < 43200000) // 12 hours
                        return;// no need to update
                    String Return = "";
                    try {
                        int count = 0;
                        myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
                        String iso = mPref.read("iso", "tw");
                        String lang = Locale.getDefault().getLanguage();
                        long latitude = mPref.readLong("latitude", 39976279);
                        long longitude = mPref.readLong("longitude", 116349386);
                        do {
                            MyNet net = new MyNet(AireJupiter.this);
                            Return = net.doPost("getfreeswitchservice.php", "idx=" + myIdx +
                                    "&iso=" + iso +
                                    "&lang=" + lang +
                                    "&lat=" + latitude +
                                    "&lon=" + longitude +
                                    "&os=and"
                                    , null);
                            if (Return.startsWith("Done="))
                                break;
                            MyUtil.Sleep(2500);
                        } while (++count < 3);
                    } catch (Exception e) {
                    }

                    if (Return.startsWith("Done=") && Return.length() > 10) {
                        Return = Return.substring(5);
                        try {
                            mPref.write("pstnSipServer", Return);
                        } catch (Exception e) {
                        }
                    }

                    mPref.writeLong("last_time_get_freeswitch_server", now);
                }
            }).start();
        }
    };
    //hot news
    Runnable getAnnouncement = new Runnable() {
        public void run() {
            if (!mADB.isFafauser(4)) return;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        MyNet net = new MyNet(AireJupiter.this);

                        long latitude = mPref.readLong("latitude", 39976279);
                        long longitude = mPref.readLong("longitude", 116349386);
                        String iso = mPref.read("iso", "");
                        String gender = mPref.read("myGender", "male");
                        String lang = Locale.getDefault().getLanguage();
                        if (mPref.read("moodcontent", "").equals("!!!!!!!")) {
                            lang = "test";
                        }
                        String Return = net.doPost("aspm.php", "idx=" + myIdx
                                + "&lang=" + lang
                                + "&iso=" + iso
                                + "&lat=" + latitude
                                + "&lon=" + longitude
                                + "&gender=" + gender
                                , null);
                        if (Return.length() > 5 && Return.startsWith("Done")) {
                            String[] items = Return.split("/");

                            if (items.length > 6) {
                                int index = Integer.parseInt(items[1]);
                                AnnounceDB ANDB = new AnnounceDB(AireJupiter.this);
                                ANDB.open();
                                if (!ANDB.existAlready(index))//alec
                                {
                                    ANDB.insert(index);
                                    int idx = Integer.parseInt(items[2]);
                                    String imgUrl = URLDecoder.decode(items[5], "UTF-8");
                                    Log.i("getAnnouncement imgUrl=" + imgUrl);

                                    int attType = Integer.parseInt(items[4]);
                                    boolean imgDownloaded = false;
                                    String filename = "";
                                    if (imgUrl != null && imgUrl.length() > 10) {
                                        try {
                                            MyNet net2 = new MyNet(getApplicationContext());

                                            filename = "img_" + idx + "_" + Integer.toHexString(index) + ".jpg";
                                            String localfile = Global.SdcardPath_inbox + filename;
                                            if (net2.anyDownload(imgUrl, localfile)) {
                                                imgDownloaded = true;
                                            }
                                            Log.i("getAnnouncement localfile=" + localfile + " " + imgDownloaded);
                                        } catch (Exception e) {
                                        }
                                    }

                                    String content = URLDecoder.decode(items[6], "UTF-8");
                                    String url = null;

                                    if (attType == 16)
                                        url = URLDecoder.decode(items[7], "UTF-8");
                                    int time = (int) (new Date().getTime() / 1000);
                                    Intent it2 = new Intent(Global.Action_InternalCMD);
                                    it2.putExtra("Command", Global.CMD_TCP_MESSAGE_ARRIVAL);

                                    if (imgDownloaded) {
                                        if (url != null)
                                            it2.putExtra("originalSignal", "210/" + idx + "/" + Integer.toHexString(time) + "/<Z>(iMG)\n"
                                                    + content + "<Z>" + attType + "<Z><Z>" + filename + "<Z>" + ((url != null) ? url : "") + "<Z>");
                                    } else {
                                        it2.putExtra("originalSignal", "210/" + idx + "/" + Integer.toHexString(time) + "/<Z>"
                                                + content);
                                    }

                                    sendBroadcast(it2);
                                }
                                ANDB.close();
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }).start();
        }
    };

    boolean callingOut = false;
    Runnable updateCreditInSipActivity = new Runnable() {
        public void run() {
            float credit = mPref.readFloat("Credit", 0);
            long callLogRowId = SipCallActivity.getCallLogRowId();

            if (callLogRowId != -1 && callingOut) {
                AireCallLogDB mCLDB = new AireCallLogDB(AireJupiter.this);
                float cost = SipCallActivity.previousCredit - credit;
                mCLDB.open();
                Log.i("Returned from Server: CallLogRowId=" + callLogRowId + "  credit=" + cost);
                mCLDB.update(callLogRowId, cost);
                mCLDB.close();
                SipCallActivity.popCallLogRowId();
                callingOut = true;
            } else if (DialerActivity.PSTNCallLogRowId != -1 && callingOut) {
                AireCallLogDB mCLDB = new AireCallLogDB(AireJupiter.this);
                float cost = DialerActivity.previousCredit - credit;
                mCLDB.open();
                Log.i("CallLogRowId=" + DialerActivity.PSTNCallLogRowId + "  credit=" + cost);
                mCLDB.update(DialerActivity.PSTNCallLogRowId, cost);
                mCLDB.close();
                //DialerActivity.PSTNCallLogRowId=-1;
                callingOut = true;
            }

            passCredit = credit;
            SipCallActivity.updateCredit(credit);
            PurchaseActivity.updateCredit(credit);
        }
    };

    //tml*** limit purchase
    public float passCredit = 0;

    public float getCredit() {
        return passCredit;
    }

    Runnable getSipCredit = new Runnable() {
        public void run() {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Log.d("getSipCredit");
                        int count = 0;
                        String android_id = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
                        String Return = "";
                        do {
                            MyNet net = new MyNet(AireJupiter.this);
                            Return = net.doPostHttps("getsipcredit.php", "id=" + URLEncoder.encode(myPhoneNumber, "UTF-8")
                                    + "&password=" + URLEncoder.encode(myPasswd, "UTF-8")
                                    + "&imei=" + android_id, null);
                            if (Return.length() > 5 && !Return.startsWith("Error"))
                                break;
                            MyUtil.Sleep(500);
                        } while (++count < 3);
                        if (Return.startsWith("Done=")) {
                            float credit = Float.parseFloat(Return.substring(5));
                            mPref.writeFloat("Credit", credit);
                            mHandler.post(updateCreditInSipActivity);
                        }
                    } catch (Exception e) {
                    }
                }
            }).start();
        }
    };


    Runnable getFreeTrialCredit = new Runnable() {
        public void run() {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Log.d("getFreeTrialCredit");
                        int count = 0;
                        String android_id = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
                        String iso = mPref.read("iso", "tw");
                        String Return = "";
                        do {
                            MyNet net = new MyNet(AireJupiter.this);
                            Return = net.doPostHttps("getfreetrialcredit.php", "id=" + URLEncoder.encode(myPhoneNumber, "UTF-8")
                                    + "&password=" + URLEncoder.encode(myPasswd, "UTF-8")
                                    + "&iso=" + iso
                                    + "&imei=" + android_id, null);
                            if (Return.length() > 5 && !Return.startsWith("Error"))
                                break;
                            MyUtil.Sleep(500);
                        } while (++count < 3);
                    } catch (Exception e) {
                    }
                }
            }).start();
        }
    };
    //tml*** iot status
    private String suvDevString = "", suvDevUserName = "", suvDevUserIdx = "";

    public void uploadSuvStatus(boolean reset, String data, String devUserName, String devUserIdx) {
        if (reset) {
            suvDevUserIdx = devUserIdx;
            new Thread(clearSuvStatus).start();
        } else if (data != null && data.length() > 0) {
            suvDevString = data;
            suvDevUserName = devUserName;
            suvDevUserIdx = devUserIdx;
            new Thread(uploadSuvStatus).start();
        }
    }

    //tml*** iot status
    Runnable uploadSuvStatus = new Runnable() {
        public void run() {
            boolean uploaded = false;

            try {
                int count = 0;
                String Return = "";

                MyNet net = new MyNet(AireJupiter.this);
                String domain = getIsoDomain();  //tml*** china ip
                String myname = URLEncoder.encode(suvDevUserName, "UTF-8");
                String myidx = URLEncoder.encode(suvDevUserIdx, "UTF-8");
                String deviceString = URLEncoder.encode(suvDevString, "UTF-8");
                deviceString = deviceString.replace("%26", "&");
                deviceString = deviceString.replace("%3D", "=");

                do {
                    Return = net.doAnyPostHttps("https://" + domain + "/webcall/acphp/iot.php",
                            "id=" + myname
                                    + "&idx=" + myidx
                                    + deviceString);
                    if (Return.startsWith("success")) break;
                    MyUtil.Sleep(500);
                } while (++count < 3);

                uploaded = Return.startsWith("success");

            } catch (Exception e) {
                Log.e("uploadSuvStatus !@#$ " + e.getMessage());
            }
        }
    };
    //tml*** iot status
    Runnable clearSuvStatus = new Runnable() {
        public void run() {
            try {
                int count = 0;
                String Return = "";

                MyNet net = new MyNet(AireJupiter.this);
                String domain = getIsoDomain();  //tml*** china ip
                String myidx = URLEncoder.encode(suvDevUserIdx, "UTF-8");

                do {
                    Return = net.doAnyPostHttps("https://" + domain + "/webcall/acphp/iotclear.php",
                            "idx=" + myidx);
                    if (Return.startsWith("success")) break;
                    MyUtil.Sleep(500);
                } while (++count < 3);

            } catch (Exception e) {
                Log.e("uploadSuvStatus !@#$ " + e.getMessage());
            }
        }
    };

    Runnable getLastestAireCallPackages = new Runnable() {
        public void run() {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Log.d("getLastestAireCallPackages");
                        int c = 0;
                        String iso = mPref.read("iso", "tw");
                        String lang = Locale.getDefault().getLanguage();
                        String Return = "";
                        do {
                            MyNet net = new MyNet(AireJupiter.this);
                            Return = net.doPostHttps("getairecallpackages.php", "iso=" + iso
                                    + "&lang=" + lang
                                    + "&os=and", null);
                            if (Return.length() > 5 && !Return.startsWith("Error"))
                                break;
                            MyUtil.Sleep(500);
                        } while (++c < 3);
                        if (Return.length() > 5) {
                            String[] seg = Return.split("<Y>");
                            if (seg.length > 0) {
                                int count = 0;
                                for (int i = 0; i < seg.length; i++) {
                                    String[] p = seg[i].split("/");
                                    if (p.length > 2) {
                                        int num = Integer.parseInt(p[0]);
                                        String name = p[1];
                                        float price = Float.parseFloat(p[2]);

                                        if (num >= 0 && name != null && price > 0) {
                                            mPref.write("PackageName" + i, name);
                                            mPref.writeFloat("PackagePrice" + i, price);
                                            count++;
                                        }
                                    }
                                }
                                mPref.write("numOfPackagesNew", count);
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }).start();
        }
    };


    Runnable resendPendingPayments = new Runnable() {
        public void run() {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Log.d("resendPendingPayments");

                        TransactionDB mTDB = new TransactionDB(AireJupiter.this);
                        mTDB.open();
                        Cursor c = mTDB.getPendingTransactions();

                        if (c.getCount() == 0) {
                            c.close();
                            mTDB.close();
                            return;
                        }

                        String android_id = "";
                        String pay_key = "";
                        String payment_id = "";
                        String app_id = "";
                        String amount = "0";
                        try {
                            if (c != null) c.moveToFirst();
                            android_id = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
                            pay_key = c.getString(5);
                            payment_id = c.getString(4);
                            app_id = c.getString(6);
                            amount = c.getString(3);
                        } catch (Exception e) {
                        }

                        if (c != null && !c.isClosed())
                            c.close();

                        String Return = "";
                        int count = 0;
                        do {
                            try {
                                MyNet net = new MyNet(AireJupiter.this);

                                if (pay_key != null && pay_key.length() > 0 && app_id != null && app_id.length() > 0) {
                                    Return = net.doPostHttps(".paypal/paybypaypal.php", "idx=" + myIdx
                                            + "&id=" + URLEncoder.encode(myPhoneNumber, "UTF-8")
                                            + "&password=" + URLEncoder.encode(myPasswd, "UTF-8")
                                            + "&imei=" + URLEncoder.encode(android_id, "UTF-8")
                                            + "&amount=" + URLEncoder.encode(amount, "UTF-8")
                                            + "&pay_key=" + URLEncoder.encode(pay_key, "UTF-8")
                                            + "&app_id=" + URLEncoder.encode(app_id, "UTF-8")
                                            , null);
                                } else if (payment_id != null && payment_id.length() > 0) {
                                    Return = net.doPostHttps(".paypal/paybycard.php", "idx=" + myIdx
                                            + "&id=" + URLEncoder.encode(myPhoneNumber, "UTF-8")
                                            + "&password=" + URLEncoder.encode(myPasswd, "UTF-8")
                                            + "&imei=" + URLEncoder.encode(android_id, "UTF-8")
                                            + "&amount=" + URLEncoder.encode(amount, "UTF-8")
                                            + "&payment_id=" + URLEncoder.encode(payment_id, "UTF-8")
                                            , null);
                                }
                            } catch (Exception e) {
                                Log.e("resendPendingPayments !@#$ " + e.getMessage());
                            }
                            if (Return.length() > 0)
                                break;
                            MyUtil.Sleep(2500);
                        } while (++count < 3);

                        Log.d(Return);

                        if (Return.startsWith("Done")) {
                            if (pay_key != null && pay_key.length() > 0 && app_id != null && app_id.length() > 0)
                                mTDB.updateByPayKey(pay_key, 1);
                            else if (payment_id != null && payment_id.length() > 0)
                                mTDB.updateByPaymentId(payment_id, 1);

                            mHandler.postDelayed(getSipCredit, 2000);
                            paypalRetries = 0;
                        } else {
                            if (paypalRetries < 10)
                                mHandler.postDelayed(resendPendingPayments, 60000);
                            paypalRetries++;
                        }

                        if (mTDB != null && mTDB.isOpen())
                            mTDB.close();

                    } catch (Exception e) {
                    }
                }
            }).start();
        }
    };

    Runnable updateFirstLocation = new Runnable() {
        public void run() {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        int count = 0;
                        String Return = "";

                        do {
                            try {
                                myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
                            } catch (Exception e) {
                            }
                            if (myIdx == 0) MyUtil.Sleep(1000);
                        } while (myIdx == 0 && count++ < 5);

                        if (myIdx == 0) return;

                        count = 0;
                        long latitude = mPref.readLong("latitude", 21560393);
                        long longitude = mPref.readLong("longitude", 39193726);
                        String iso = mPref.read("iso", "");
                        do {
                            MyNet net = new MyNet(AireJupiter.this);
                            Return = net.doPostHttps("updatefirstlocation.php", "idx=" + myIdx
                                    + "&iso=" + iso
                                    + "&lat=" + latitude
                                    + "&lon=" + longitude, null);
                            if (Return.startsWith("Done")) break;
                            MyUtil.Sleep(1000);
                        } while (++count < 3);
                    } catch (Exception e) {
                    }
                }
            }).start();
        }
    };

    Runnable queryStudioGroupsFromServer = new Runnable() {
        public void run() {
            new Thread(new Runnable() {
                public void run() {
                    long now = new Date().getTime();
                    long last = mPref.readLong("last_query_public_studio", 0);
                    if (now - last < 180000) // 3 minutes
                        return;

                    Log.d("query_studio_groups");
                    String Return = "";
                    try {
                        int c = 0;
                        do {
                            MyNet net = new MyNet(AireJupiter.this);
                            Return = net.doPostHttps("query_studio_groups.php", "name=1", null);
                            if (Return.length() > 4)
                                break;
                            MyUtil.Sleep(2500);
                        } while (++c < 1);
                    } catch (Exception e) {
                    }

                    if (Return.length() > 4) {
                        String[] g = Return.split(";");

                        StudioGroupDB mSGDB = null;
                        mSGDB = new StudioGroupDB(AireJupiter.this);
                        mSGDB.open();

                        for (int i = 0; i < g.length && i < 500; i++) {
                            String[] items = g[i].split("/");
                            if (items.length > 4) {
                                try {
                                    int hot = Integer.parseInt(items[2]);
                                    String iso = (items[3]);
                                    int locked = Integer.parseInt(items[4]);
                                    mSGDB.insertGroup(URLDecoder.decode(items[0], "UTF-8"), items[1], hot, locked, iso);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        if (mSGDB.isOpen())
                            mSGDB.close();

                        Log.d("query_studio_groups done");

                        mPref.writeLong("last_query_public_studio", now);
                    }
                }
            }).start();
        }
    };

    void doPostProfileUpdateInTimeLine(String photoPath) {
        String Return = "";
        String ufile = "";
        int c = 0;
        do {
            try {
                MyNet net = new MyNet(AireJupiter.this);
                Return = net.doPostAttach("timelineupload.php", 0, 0, photoPath, myLocalPhpServer);
            } catch (Exception e) {
            }

            if (Return.startsWith("Done")) {
                ufile = Return.substring(5);
                MyUtil.copyFile(photoPath, Global.SdcardPath_timeline + ufile, true);
                break;
            }
            MyUtil.Sleep(1500);
        } while (++c < 3);

        String myNickname = mPref.read("myNickname");
        String statement = String.format(getString(R.string.profile_updated), myNickname);
        TimeLineDB mTLDB = new TimeLineDB(AireJupiter.this);
        mTLDB.open();
        c = 0;
        do {
            try {
                MyNet net = new MyNet(AireJupiter.this);
                Return = net.doPost("timelinepost.php", "id=" + myIdx +
                        "&writer=" + myIdx +
                        "&name=" + URLEncoder.encode(myNickname, "UTF-8") +
                        "&text=" + URLEncoder.encode(statement, "UTF-8") +
                        "&pms=0" +
                        "&attaches=" + URLEncoder.encode(ufile, "UTF-8") +
                        "&srvr=" + URLEncoder.encode(myLocalPhpServer, "UTF-8")
                        , null);
            } catch (Exception e) {
            }
            if (Return.length() > 5) break;
            MyUtil.Sleep(500);
        } while (c++ < 3);

        if (Return.startsWith("Done=")) {
            int post_id = Integer.parseInt(Return.substring(5));
            mTLDB.insert(post_id, myIdx, myIdx, myIdx, myNickname, 0, new Date().getTime(), statement, ufile, null, myLocalPhpServer, 0, 0);
        }

        mTLDB.close();
    }

    void doPostMoodInTimeLine(String newMood) {
        String Return = "";
        int c = 0;
        TimeLineDB mTLDB = new TimeLineDB(AireJupiter.this);
        mTLDB.open();
        String myNickname = mPref.read("myNickname");
        do {
            try {
                MyNet net = new MyNet(AireJupiter.this);
                Return = net.doPost("timelinepost.php", "id=" + myIdx +
                        "&writer=" + myIdx +
                        "&name=" + URLEncoder.encode(myNickname, "UTF_8") +
                        "&text=" + URLEncoder.encode(newMood, "UTF-8") +
                        "&srvr=" + URLEncoder.encode(myLocalPhpServer, "UTF-8") +
                        "&pms=0"
                        , null);
            } catch (Exception e) {
            }
            if (Return.length() > 5) break;
            MyUtil.Sleep(500);
        } while (c++ < 3);

        if (Return.startsWith("Done=")) {
            int post_id = Integer.parseInt(Return.substring(5));
            mTLDB.insert(post_id, myIdx, myIdx, myIdx, myNickname, 0, new Date().getTime(), newMood, null, null, null, 0, 0);
        }

        mTLDB.close();
    }

    //tml*** keepalive
    public void restartService(Context context, int delay) {
        if (delay == 0) {
            delay = 1;
        }
        Log.e("AJ restarting AireJupiter");
        Intent restartIntent = new Intent(context, AireJupiter.class);
        PendingIntent intent = PendingIntent.getService(context, 0,
                restartIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, intent);
        System.exit(2);
    }

    //***tml
    //tml*** alert toast
    public void toastWarning(int mode, boolean force, String from) {
        if (mode == 1) {
            Log.e("*** Alert! AireTalk connection may be poor  " + mode + force + " <" + from);
            long now = System.currentTimeMillis();
            long last = mPref.readLong("last_warn_net_time", 0);
            long elapsed = now - last;
            if (elapsed < 60000 && !force) {
                Log.e("*** Alert too soon, no toast");
                return;
            }
            mPref.writeLong("last_warn_net_time", now);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast tst1 = Toast.makeText(AireJupiter.this,
                            getString(R.string.poornetwork),
                            Toast.LENGTH_LONG);
                    tst1.setGravity(Gravity.TOP, 0, 50);
                    try {
                        if (seeTopAct(1).startsWith("com.pingshow.amper")) {
                            LinearLayout tstLay1 = (LinearLayout) tst1.getView();
                            TextView tstTV1 = (TextView) tstLay1.getChildAt(0);
                            tstTV1.setTextSize(16);
                            tstTV1.setGravity(Gravity.CENTER);
                            tstTV1.setTextColor(Color.YELLOW);
                            tst1.show();
                        } else if (seeTopAct(5).startsWith("com.pingshow.amper")) {
//							LinearLayout tstLay1 = (LinearLayout) tst1.getView();
//							TextView tstTV1 = (TextView) tstLay1.getChildAt(0);
//							tstTV1.setGravity(Gravity.CENTER);
//							tstTV1.setTextColor(Color.YELLOW);
//							tst1.show();
                        } else {

                        }
                    } catch (ClassCastException e) {
                    }
                }
            });
        } else if (mode == 2) {
        }
    }

    //tml test
    public String TESTseeVolumes(String from) {
        AudioManager mAudioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
        int max0 = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        int max1 = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_DTMF);
        int max2 = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int max3 = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        int max4 = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        int max5 = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
        int max6 = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
        int vol0 = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        int vol1 = mAudioManager.getStreamVolume(AudioManager.STREAM_DTMF);
        int vol2 = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int vol3 = mAudioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        int vol4 = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
        int vol5 = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
        int vol6 = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        int mode = mAudioManager.getMode();
        String modeS = "";
        if (mode == -2) {
            modeS = "invalid";
        } else if (mode == 0) {
            modeS = "norm";
        } else if (mode == 1) {
            modeS = "ring";
        } else if (mode == 2) {
            modeS = "call";
        } else if (mode == 3) {
            modeS = "comm";
        }
        boolean mic = mAudioManager.isMicrophoneMute();
        boolean spkr = mAudioManager.isSpeakerphoneOn();
        boolean ear = mAudioManager.isWiredHeadsetOn();
        String volumestates = "(" + from + "," + modeS + "," + mic + "," + spkr + "," + ear + ")"
                + " alm:" + vol0 + "/" + max0
                + " dtf:" + vol1 + "/" + max1
                + " msc:" + vol2 + "/" + max2
                + " ntf:" + vol3 + "/" + max3
                + " rng:" + vol4 + "/" + max4
                + " sys:" + vol5 + "/" + max5
                + " call:" + vol6 + "/" + max6;
        Log.d("SEE VOL!  " + volumestates);
        return volumestates;
    }

    public String seeTopAct(int topN) {
        ActivityManager mAm = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = mAm.getRunningTasks(10);
        String name = "topApp";

        if (topN == 0 || topN == 1) {
            name = taskInfo.get(0).topActivity.getClassName();
        } else {
            int listApps = taskInfo.size();
            int listSize = 1;

            if (listApps < topN) {
                listSize = listApps;
            } else {
                listSize = topN;
            }

//			for (int i = 0; i < listSize; i++) {
//				name = taskInfo.get(i).topActivity.getClassName();
////				Log.e("TASK " + i + " " + name);
//				if (name.startsWith("com.pingshow.amper")) break;
//			}
            for (int i = (listSize - 1); i > -1; i--) {
                name = taskInfo.get(i).topActivity.getClassName();
//				Log.e("TASK " + i + " " + name);
                if (name.startsWith("com.pingshow.amper")) break;
            }
        }
        Log.d("seeTopAct=" + name);
        return name;
    }

    //***tml
    //tml*** china ip
    public String getIsoDomain() {
        String domain = myAcDomain_China;
        if (!MyUtil.isISO_China(AireJupiter.this, mPref, null)) {
            domain = myAcDomain_USA;
        }
        myAcDomain_default = domain;
        return domain;
    }

    public String getIsoPhp(int phpN, boolean useip, String ip) {
        String php = myPhpServer_default2A;
        if (!useip) {
            php = myPhpServer_default2B;
        }

        if (!MyUtil.isISO_China(AireJupiter.this, mPref, null)) {
            if (phpN == 0) {
                php = AireJupiter.myPhpServer_default;
            } else {
                php = AireJupiter.myPhpServer;
            }
            if (ip != null) php = ip;
        }
//		myPhpServer_default = php;
        return php;
    }

    public String getIsoConf(String ip) {
        String useip = myConfServer_China;
        String iso = mPref.read("iso", "cn");
        if (!MyUtil.isISO_China(AireJupiter.this, mPref, null)) {
            if (ip == null) {
                useip = myConfSipServer_default;
            } else {
                useip = ip;
            }
        }
        Log.d("isoConf " + iso + " " + ip + ">" + useip);
        return useip;
    }

    public String getIsoSip() {
        String sip = AireVenus.mySipServer_China;
        String iso = mPref.read("iso", "cn");
        String savedsip = mPref.read("mySipServer", sip);
        if (!MyUtil.isISO_China(AireJupiter.this, mPref, null)) {
            sip = AireVenus.mySipServer_USA;
            if (!savedsip.equals(AireVenus.mySipServer_USA)) {
                sip = savedsip;
            }
        } else {
            mPref.write("mySipServer", sip);
        }
        AireVenus.mySipServer_default = sip;
        AireJupiter.mySipServer_default = sip;  //tml*** xcountry sip
        Log.d("isoSip " + iso + " " + savedsip + ">" + sip);
        return sip;
    }
    //***tml
}
