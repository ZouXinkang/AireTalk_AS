package com.pingshow.airecenter;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.net.wifi.WifiManager;
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

import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.contacts.RWTOnline;
import com.pingshow.airecenter.contacts.RelatedUserInfo;
import com.pingshow.airecenter.db.AireCallLogDB;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.GroupDB;
import com.pingshow.airecenter.db.RelatedUserDB;
import com.pingshow.airecenter.db.SmsDB;
import com.pingshow.airecenter.db.WTHistoryDB;
import com.pingshow.airecenter.map.LocationUpdate;
import com.pingshow.airecenter.message.ParseSmsLine;
import com.pingshow.beehive.BeeHiveService;
import com.pingshow.homesafeguard.HomeSafeguardService;
import com.pingshow.iot.UsbComm;
import com.pingshow.network.MyNet;
import com.pingshow.network.MySocket;
import com.pingshow.network.NetInfo;
import com.pingshow.network.RWTSocket;
import com.pingshow.network.upnpc;
import com.pingshow.util.HdmiUtil;
import com.pingshow.util.LedSpeakerUtil;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.util.OpenDifferentFile;
import com.pingshow.util.ResizeImage;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;
import com.pingshow.voip.VoipConfigException;
import com.pingshow.voip.VoipException;

public class AireJupiter extends Service {
	
	static public String myFafaServer_default = "74.3.164.16";
	static public String myPhpServer_default = "74.3.165.66";
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
	
	private final int TIMER_6_MIN_INTERVAL = 300000; // 5 mins //tml*** neverdie, back to 3m from 1/
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
	
//	private String md5="";
	private int versionCode;
	private int myIdx;
	
	public MySocket tcpSocket = null;
	private RWTSocket rwtSocket = null;
	public boolean unautherized999=false;
	private int freqDivided=0;

	private boolean checkonce = true;
	
	public static boolean notifying=false;
	private systemNumberChange nbc;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	static public AireJupiter instance;

	static public AireJupiter getInstance() {
		return instance;
	}
	
	public MySocket tcpSocket()
	{
		return tcpSocket;
	}
	
	public boolean isLogged()
	{
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
		Log.e("*** !!! AIREJUPITER ***  START START !!! ***");

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
    	int thisPID = MyUtil.getAiretalkPID(this);
		Log.e("mnf:" + thisManuf + "|brnd:" + thisBrand + "|dvc:" + thisDevice + "|mdl:" + thisModel + "|prod:" + thisProduct
				+ "|ser:" + thisSerial + "|id:" + thisAID + "|sdk:" + thisSDK + "|os:" + thisRV
				+ "|cpu1:" + thisCpuset1 + "|cpu2:" + thisCpuset2 + "|pid:" + thisPID);
		
		instance=this;
		if (android.os.Build.VERSION.SDK_INT > 9) {
		    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		    StrictMode.setThreadPolicy(policy);
		}
		mPref = new MyPreference(this);
		mPref.write("JUPITERSTART_BLUE", true);  //tml*** blue io
		mPref.writeLong("outToServerLast", 0);  //tml*** outToServer period/
		mPref.write("pid", thisPID);
		cq = new ContactsQuery(this);
		shared_friends=(ArrayList<String>) mPref.readArray("shared_friends");
		mADB = new AmpUserDB(this);// alec
		mADB.open();
		
		mRDB = new RelatedUserDB(this);
		mRDB.open();
		
		mWTDB=new WTHistoryDB(this);
		mWTDB.open();
		
		mSmsDB = new SmsDB(AireJupiter.this);
		mSmsDB.open();

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		mPref.writeLong("last_dlf_status", 0);

		//md5 = checkApk();

		try {
			versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			Log.e("aj1 " + e.getMessage());
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
					Log.e("aj2 " + e.getMessage());
				}
				getSipFriendsAcc();
			}
		}, "Servers Stuff");
		thread_getServers.start();

		//tml*** usb dongle
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		dongle1_found = mPref.readBoolean("DONGLE1_FOUND", false);
		dongleTest_found = mPref.readBoolean("DONGLET_FOUND", false);
		
		try{
			nbc = new systemNumberChange(new Handler());
			getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, false, nbc);

			IntentFilter intentToReceiveFilter = new IntentFilter();
			intentToReceiveFilter.addAction(Global.Action_InternalCMD);
			intentToReceiveFilter.addAction(Global.Action_Contact);
			intentToReceiveFilter.addAction(Global.Action_SD_AvailableSpare);
			intentToReceiveFilter.addAction(Global.Action_FileDownload);
			intentToReceiveFilter.addAction(Global.Action_Start_Surveillance);
			intentToReceiveFilter.addAction(Global.Action_Start_Homesensor);
			intentToReceiveFilter.addAction(Global.Action_End_Homesensor);
			intentToReceiveFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
			intentToReceiveFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
			registerReceiver(InternalCommand, intentToReceiveFilter);
		}catch (Exception e) {
			Toast.makeText(this, R.string.no_sdcard, Toast.LENGTH_LONG).show();
		}
		
		try {
			new File(Global.SdcardPath).mkdir();
			new File(Global.SdcardPath_inbox).mkdir();
			new File(Global.SdcardPath_sent).mkdir();
			new File(Global.SdcardPath_downloads).mkdir();
			new File(Global.SdcardPath_temp).mkdir();
//			new File(Global.SdcardPath_record).mkdir();
			//tml*** browser save
			new File(Global.SdcardPath_record).mkdirs();
			new File(Global.SdcardPath_video).mkdirs();
			new File(Global.SdcardPath_image).mkdirs();
			new File(Global.SdcardPath_files).mkdirs();
			new File(Global.SdcardPath_music).mkdirs();
		} catch (Exception e) {
			Log.e("aj3 " + e.getMessage());
		}
		
		reconfigBeeHive();
		
		if (mPref.readBoolean("usestanleysip"))
			startServiceY_ForAireCall.run();

		if (DialerActivity.getDialer() != null) {  //tml*** abort dialer, crash
			Intent intent = new Intent(Global.KILL_dialer);
			sendBroadcast(intent);
		}
		
		if (mPref.readBoolean("SecurityDetecting", false))
		{
			//need to also auto start dongle/smartdevices
			mHandler.postDelayed(new Runnable(){
				@Override
				public void run() {
					String address=mPref.read("SuvHostAddress");
					Intent intent = new Intent(Global.Action_Start_Surveillance);
        			intent.putExtra("address", address);
					sendBroadcast(intent);
				}
			}, 1000);
		}
		
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//li*** Home safeguard
		Intent ImIntent = new Intent(getApplicationContext(), HomeSafeguardService.class);
		startService(ImIntent);
		return super.onStartCommand(intent, flags, startId);
	}
	
	
	private void startBeeHiveService()
	{
		if (!MyUtil.CheckServiceExists(getApplicationContext(), "com.pingshow.BeeHive.BeeHiveService")) {
			Intent ImIntent=new Intent(getApplicationContext(), BeeHiveService.class);
			startService(ImIntent);
		}
	}
	
	private void stopBeeHiveService()
	{
		if (MyUtil.CheckServiceExists(getApplicationContext(), "com.pingshow.BeeHive.BeeHiveService")) {
			Intent ImIntent=new Intent(getApplicationContext(), BeeHiveService.class);
			stopService(ImIntent);
		}
	}
	
	public void reconfigBeeHive()
	{
		startBeeHiveService();
	}
	
	public void rebuildAlarmReceiver() {  //tml temp fix, broadcast conflict ui
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.cancel(pendingInt);
		buildAlarmReceiver();
	}
	
	private void buildAlarmReceiver()
	{
		int interval=TIMER_6_MIN_INTERVAL;
		NetInfo ni = new NetInfo(AireJupiter.this);
		if (ni.netType==NetInfo.WIFI)
		{
			interval=TIMER_8_MIN_INTERVAL;
		}
		
		AlarmManager mgr=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
		Intent i=new Intent(AireJupiter.this, AmpAlarmReceiver.class);
	 	pendingInt=PendingIntent.getBroadcast(AireJupiter.this, 0, i, 0);
		mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + interval, 
				interval, pendingInt);
		Log.d("AmpAlarmReceiver interval@" + interval);
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(InternalCommand);// alec
		mNM.cancel(R.string.app_name);
		mPref.delect("tempCheckSameIN");  //tml*** sametime
		
		instance=null;
		
		if (tcpSocket!=null) tcpSocket.disconnect("aj destroy", true);
		if (rwtSocket!=null) rwtSocket.disconnect();
		
		if (m_upnpc!=null)
			m_upnpc.release();
		
		if (mADB != null && mADB.isOpen())
			mADB.close();
		if (mRDB != null && mRDB.isOpen())
			mRDB.close();
		if (mSmsDB != null && mSmsDB.isOpen())
			mSmsDB.close();
		if (mWTDB !=null && mWTDB.isOpen())
			mWTDB.close();
		
		if (mLocation!=null)
			mLocation.destroy();

		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.cancel(pendingInt);  Log.e("test AmpAlarmReceiver X1");
		
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

		if (new NetInfo(AireJupiter.this).netType==NetInfo.WIFI)
		{
			Log.d("(1.1) doUpnp in WiFi LAN");
			m_upnpc=new upnpc(mPref);
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
			Log.e("aj4 " + e.getMessage());
			myPhpServer = myPhpServer_default;
		}
		Log.d("@" + myPhoneNumber + " " + myPasswd + " " + iso + " " + mySipServer + " " + domainName + " " + myPhpServer);
	}
	
	private upnpc m_upnpc=null;
	
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
			Log.e("KILL OLD SOCKET: disconnect from TCP server...");
			tcpSocket.disconnect("aj new socket", true);
			tcpSocket = null;
		}
		tcpSocket = new MySocket(myPhoneNumber, myPasswd, this, mADB, mRDB, cq, myFafaServer, mSmsDB);
	}
	
	public RWTSocket new_rwt_socket() {
		if (rwtSocket == null)
		{
			rwtSocket = new RWTSocket(myPhoneNumber, myPasswd, AireJupiter.this, mADB, mWTDB);
		}
		return rwtSocket;
	}
	
	
	private void OnlineConnection(boolean force) {
		Log.d("(4) OnLineConnection");
		
		if (!tcpSocket.isLogged(force))
			tcpSocket.Login(versionCode);
		
		try{
			myIdx=Integer.parseInt(mPref.read("myID","0"),16);
			if (myIdx==0)
				tcpSocket.Login(versionCode);
		}catch(Exception e){
			Log.e("aj5 " + e.getMessage());
		}

		ScheduledThreadPoolExecutor thrExec = new ScheduledThreadPoolExecutor(1);  //tml*** new thread delay
		
		if (mADB!=null && mADB.getCount()<=1)
		{
			new Thread(downloadFriendList).start();
			
			if (mPref.readBoolean("firstEnter",false))
			{
				mPref.delect("firstEnter");
				
				if (mPref.readBoolean("permissionReadContacts",true))
				{
					searchFriendsByPhonebook.run();
					mInstantQueryOnlineFriends.run();
				}
			}
			else
			{
				mInstantQueryOnlineFriends.run();
			}
			
//			mHandler.postDelayed(getFreeTrialCredit, 13000);
//			mHandler.postDelayed(getConferenceServiceIP,15000);
//			mHandler.postDelayed(getFreeswitchServiceIP,11000);
//			mHandler.postDelayed(searchPossibleFriends,16000);
//			mHandler.postDelayed(searchFacebookFriends,31000);
//			mHandler.postDelayed(getRWTServerIP,36000);
			//tml*** new thread delay
			thrExec.setCorePoolSize(9);
			thrExec.schedule(getFreeTrialCredit, 13000, TimeUnit.MILLISECONDS);
//			thrExec.schedule(getConferenceServiceIP, 15000, TimeUnit.MILLISECONDS);
			thrExec.schedule(getFreeswitchServiceIP, 11000, TimeUnit.MILLISECONDS);
			thrExec.schedule(searchPossibleFriends, 16000, TimeUnit.MILLISECONDS);
			thrExec.schedule(searchFacebookFriends, 31000, TimeUnit.MILLISECONDS);
			thrExec.schedule(getRWTServerIP, 36000, TimeUnit.MILLISECONDS);
		}
		else
		{
			thrExec.setCorePoolSize(4);
			mInstantQueryOnlineFriends.run();
		}
		
//		mHandler.postDelayed(getSipCredit, 5000);
//		mHandler.postDelayed(securitySubscriptionThread, 8000);
//		mHandler.postDelayed(searchPossibleFriends,19000);
		//tml*** new thread delay
		thrExec.schedule(getConferenceServiceIP, 15000, TimeUnit.MILLISECONDS);
		thrExec.schedule(getSipCredit, 5000, TimeUnit.MILLISECONDS);
		thrExec.schedule(securitySubscriptionThread, 8000, TimeUnit.MILLISECONDS);
		thrExec.schedule(searchPossibleFriends, 19000, TimeUnit.MILLISECONDS);
//		boolean test1 = mPref.readBoolean("TEST_PHP");
//		boolean test2 = mPref.readBoolean("TEST_PHPTHRD");
//		Log.e(">>> test >>> delayrunnable, php/method " + test1 + test2);
//		if (test2) {
//			thrExec.schedule(securitySubscriptionThread, 8000, TimeUnit.MILLISECONDS);
//		} else {
//			mHandler.postDelayed(securitySubscriptionThread, 8000);
//		}
		
		if (AireVenus.instance() != null) {
			AireVenus.sip_login(mySipServer, myPhoneNumber);
		}
		
		//tml*** new thread delay
		thrExec.shutdown();
		int poola = thrExec.getActiveCount();
		int poolm = thrExec.getCorePoolSize();
		int poolz = thrExec.getPoolSize();
		
		Log.d("(5) OnLineConnection done   [" + poola + "/" + poolz + "/" + poolm + "]");
	}		
	
	private Runnable searchFacebookFriends=new Runnable()
	{
		public void run()
		{
			new Thread(new Runnable(){
				public void run()
				{
					if (MyUtil.isISO_China(AireJupiter.this, mPref, null)) return;
					if (!new NetInfo(AireJupiter.this).isConnected()) return;
					long last = mPref.readLong("facebookFriendsSynchronized", 0);
					String myFacebookID=mPref.read("myFacebookID","");
					
					long now = new Date().getTime();
					if (now - last < 72000000) // 20 hours
						return;// no need to check
					
					if (DialerActivity.getDialer()!=null)
					{
						mHandler.postDelayed(searchFacebookFriends, 30000);
						return;
					}
					
					if (myFacebookID.length()<1)
					{
						if (!mPref.readBoolean("ProfileCompleted",false))
						{
							mHandler.postDelayed(searchFacebookFriends, 37000);
							return;
						}
						Intent it = new Intent(AireJupiter.this, CommonDialog.class);
						it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						String title=getString(R.string.searching_friends_desc);
						it.putExtra("msgContent", title);
						it.putExtra("numItems", 2);
						it.putExtra("ItemCaption0", getString(R.string.no));
						it.putExtra("ItemResult0", CommonDialog.DONTSEARCHFACEBOOK);
						it.putExtra("ItemCaption1", getString(R.string.yes));
						it.putExtra("ItemResult1", CommonDialog.SEARCHFACEBOOK);
						showNotification(title, null, true, R.drawable.icon_sms, null);
						startActivity(it);
					}
					else{
						mHandler.post(new Runnable(){
							public void run()
							{
								Intent it=new Intent(AireJupiter.this, FacebookSearch.class);
								it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(it);
							}
						});
					}
				}
			}).start();
		}
	};
	
	public void notifyConnectionChanged() {
		
		if (!unautherized999)
		{
			mHandler.removeCallbacks(TryToConnectTCP);
			mHandler.postDelayed(TryToConnectTCP, 8000);
		}
		
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				
				if (!tcpSocket.isLogged(false))//alec
					ContactsOnline.setAllfriendsOffline();
				
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
		
	}
	
	final Runnable reconnectDebounce = new Runnable() {
		@Override
		public void run() {
			Thread thr_connection = new Thread(new Runnable() {
				public void run() {
					
					if (tcpSocket!=null && !tcpSocket.isLogged(false))
						tcpSocket.Login(versionCode);
					
					if (AireVenus.instance() != null)
					{
						if (AireVenus.destroying) {
							mHandler.postDelayed(mEndupServiceY,3000);
						}
						AireVenus.sip_login(mySipServer, myPhoneNumber);
						AireVenus.instance().renableCodec(false);//alec
						//mHandler.postDelayed(mEndupServiceY,3000);
					}

					if (tcpSocket!=null && tcpSocket.isLogged(false)){
						tcpSocket.queryFriendsOnlineStatus(true);
						sendPendingSMS();
						mHandler.post(refreshLocation);//alec, once if new connectivity comes, update location...
					}
					
					if (new NetInfo(AireJupiter.this).netType==NetInfo.WIFI)
					{
						m_upnpc=new upnpc(mPref);
						m_upnpc.start();
					}
					else
					{
						Log.d("voip.UPNP *** 0 ***, netType != WIFI");
						mPref.write("audio_local_port", 0);
						mPref.write("video_local_port", 0);
					}
					
					try{
						if (rwtSocket == null)
							new_rwt_socket();
						if (rwtSocket!=null)
						{
							rwtSocket.Login();
							rwtSocket.queryFriendsOnlineStatus();
						}
					}catch(Exception e){
						Log.e("aj6 " + e.getMessage());
					}
					
					AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
					am.cancel(pendingInt);  Log.e("test AmpAlarmReceiver X2");
					buildAlarmReceiver();
					
					reconfigBeeHive();
				}
			}, "onReconnect");
			thr_connection.start();
		}
	};
	
	public void refreshLocation()
	{
		mHandler.post(refreshLocation);
	}
	
	public void ReleaseLocationMoniter()
	{
		if (mLocation!=null)
			mLocation.destroy();
	}
	
	Runnable refreshLocation = new Runnable()
	{
		@Override
		public void run() {
			if (mLocation!=null)
				mLocation.destroy();
			mLocation=new LocationUpdate(instance, mHandler, tcpSocket, mPref, true);
		}
	};
	
//	@SuppressWarnings("unused")
//	public String checkApk() {
//		String s = null;
//		try {
//			File f;
//			int r = 0;
//			String filename = "/mnt/asec/com.pingshow.airecenter/pkg.apk";
//			while ((f = new File(filename)) == null || !f.exists()) {
//				r++;
//				if (r > 4) break;
//				filename = "/mnt/asec/com.pingshow.airecenter-" + r + "/pkg.apk";
//				Log.d(filename);
//			}
//			
//			if (!f.exists())
//			{
//				r=0;
//				filename = "/data/app/com.pingshow.airecenter.apk";
//				while ((f = new File(filename)) == null || !f.exists()) {
//					r++;
//					if (r > 4) break;
//					filename = "/data/app/com.pingshow.airecenter-" + r + ".apk";
//					Log.d(filename);
//				}
//			}
//
//			if (!f.exists())
//				return s;
//
//			InputStream is = new FileInputStream(f);
//
//			byte[] buffer = new byte[8192];
//
//			final char hexDigits[] = { 'a', '1', 'e', 'c', 'f', '0', 'b', '2', '3', 'd', '4', '5', '6', '7',
//					'8', '9'};
//			java.security.MessageDigest md = java.security.MessageDigest
//					.getInstance("MD5");
//			char str[] = new char[16 * 2];
//			int readBytes = 0;
//			while ((readBytes = is.read(buffer)) != -1) {
//				md.update(buffer);
//				byte tmp[] = md.digest();
//				int k = 0;
//				for (int i = 0; i < 16; i++) {
//					byte byte0 = tmp[i];
//					str[k++] = hexDigits[byte0 >>> 4 & 0xf];
//					str[k++] = hexDigits[byte0 & 0xf];
//				}
//			}
//			s = new String(str);
//		} catch (Exception e) {
//			Log.e("aj7 " + e.getMessage());
//		}
//		return s;
//	}
	
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
				if (!tcpSocket.queryFriendsOnlineStatus(true)) {
					mHandler.removeCallbacks(TryToConnectTCP);
					mHandler.postDelayed(TryToConnectTCP, 3000);
				}
			} else {
				int c=0;
				while (!tcpSocket.isLogged(false) && ++c < 4) {
					Log.e("In do10mJobs, not logged...");
					if (tcpSocket.disconnect("10m job", true))
					{
						NetInfo ni = new NetInfo(AireJupiter.this);
						if (ni.isConnected()) {
							if (tcpSocket.Login(versionCode))
							{
								tried=0;
								return;
							}
						}
					}
					MyUtil.Sleep(30000);
				}
			}
			
			doCheckUnread();  //tml*** unread led
		}
	};
	
	public void do30mConnection() {
		if (tcpSocket != null)
			do30mJobs.run();
	}
	
	public final Runnable do30mJobs = new Runnable() {
		@Override
		public void run() {
			if (tcpSocket==null) return;
			
			Log.d("Do... 30 min jobs");
			tcpSocket.keepAlive(true);  //tml*** keepalive/
			
			if (DialerActivity.getDialer()!=null) return;
			if (!new NetInfo(AireJupiter.this).isConnected()) return;

			freqDivided++;
			
			sendPendingSMS();
			onReceiveOfflineMessage.run();

			if ((freqDivided % 2)==0) // 1.5 hours
			{
				getFriendNicknames();
			}
			
			if ((freqDivided % 15)==0) // 7.5 hours
			{
				getRWTServerIP.run();
			}
			
			if ((freqDivided % 13)==1) // 6.5 hours
			{
				searchFriendsByPhonebook.run();
			}
			
			if ((freqDivided % 9)==0) //4.5 hours
			{
				searchPossibleFriends.run();
			}
			
			if ((freqDivided % 10)==0)//5 hours
			{
				downloadBigPhotoFromNet();
			}
			
			if ((freqDivided % 11)==0) // 5.5 hours
			{
				doCheckPhotoFromNet();
			}
			
			if ((freqDivided % 12)==0) // 6 hours
			{
				checkVersionUpdate(false);
//				getFriendNicknames();
			}
			
			if ((freqDivided % 6)==0) //6.5 hours
			{
				getFriendMoods();
			}
			
			if ((freqDivided % 5)==0) // 2.5 hours
			{
				if (mPref.read("myPhotoPath")!=null && !mPref.readBoolean("myPhotoUploaded", false))
	    		{
	    			Intent it = new Intent(Global.Action_InternalCMD);
	    			it.putExtra("Command", Global.CMD_UPLOAD_PROFILE_PHOTO);
	    			sendBroadcast(it);
	    		}
			}
			
			if ((freqDivided % 10)==0)//8 hours
			{
				getSipFriendsAcc();
			}
			
			if ((freqDivided % 16)==1)//8 hours
			{
				uploadAllFriends();
			}
		
			if ((freqDivided % 18)==2)//9 hours
			{
				clearSDCard.run();
			}
			//tml*** dev control
			if ((freqDivided % 24)==0) {//12 hours (6 now)
				airecenterAccess();
			}
			
			if ((freqDivided % 36)==0)//24 hours
			{
				//searchFacebookFriends.run();
				
				try {
					myPhpServer=InetAddress.getByName(myPhpServer_default).getHostAddress();
					Log.d("myPhpServer="+myPhpServer);
				} catch (UnknownHostException e) {
					Log.e("aj8 " + e.getMessage());
					e.printStackTrace();
				}
				
				mPref.write("myIpAddress","");
				mPref.write("myGeoLocation","");
			}
			
			if ((freqDivided % 24)==0)//18 hours
			{
				getFreeswitchServiceIP.run();
			}
			
			if ((freqDivided % 10)==0)//19 hours 38
			{
				getConferenceServiceIP.run();
			}
		}
	};
	
	final Runnable TryToConnectTCP = new Runnable() {
		public void run() {
			unautherized999=true;
			Thread thr = new Thread(mInstantQueryOnlineFriends,
					"Update Friend Status");
			thr.start();
		}
	};
	
	int tried=0;
	
	final Runnable mInstantQueryOnlineFriends = new Runnable() {
		public void run() {
			if (tcpSocket == null) return;
			
			Thread queryFriends_thread = new Thread(new Runnable() {
				public void run() {
					
					if (tcpSocket == null) return;
					
					if (!tcpSocket.isLogged(false)) {
						tried++;
						if (tcpSocket.Login(versionCode))
							tried = 0;
					}
					
					if (unautherized999) return;

					if (!tcpSocket.queryFriendsOnlineStatus(false))
					{
						if (tried < 4) {
							mHandler.removeCallbacks(TryToConnectTCP);
							mHandler.postDelayed(TryToConnectTCP, 30000);
						}
						else if ((tried % 20) == 4) {
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
	
	public void startServiceY(int mode)
	{
		mHandler.removeCallbacks(mEndupServiceY);
		attemptCall=true;
		
		if (AireVenus.instance() != null)
		{
//			if (AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL)
			if (AireVenus.getCallType() != mode)  //tml*** recheck venus
			{
				if (!AireVenus.instance().inCall) {
					Log.e("!!! STOPPING AireVenus/ServiceY *** AJ @ voip.startServiceY CallType !=");
					Intent itx = new Intent(AireJupiter.this, AireVenus.class);
		    		stopService(itx);
		    		MyUtil.Sleep(3000);
				} else {
					return;
				}
			}
		}

		AireVenus.setCallType(mode);
		
		if (AireVenus.instance()==null) {
			
			if (mPref.readBoolean("doingUPNP",false))
			{
				Log.d("doingUPNP, wait for 3 sec");
				MyUtil.Sleep(3000);
			}
			android.os.Process.setThreadPriority(-19);
			Intent VoipIntent = new Intent(Intent.ACTION_MAIN);
			VoipIntent.setClass(AireJupiter.this, AireVenus.class);
			startService(VoipIntent);
		}
		else{
			if (AireVenus.instance() != null) {
				AireVenus.sip_login(mySipServer, myPhoneNumber);
			}
		}
	}
	
	public void startServiceYForAireCall()
	{
		mHandler.post(startServiceY_ForAireCall);
	}
	
	Runnable startServiceY_ForAireCall=new Runnable()
	{
		public void run()
		{
			mHandler.removeCallbacks(mEndupServiceY);
			attemptCall=true;
			
			AireVenus.setCallType(AireVenus.CALLTYPE_AIRECALL);
			
			if (AireVenus.instance()==null) {
				if (mPref.readBoolean("doingUPNP",false))
				{
					Log.d("doingUPNP, wait for 3 sec");
					MyUtil.Sleep(3000);
				}
				android.os.Process.setThreadPriority(-19);
				Intent VoipIntent = new Intent(Intent.ACTION_MAIN);
				VoipIntent.setClass(AireJupiter.this, AireVenus.class);
				startService(VoipIntent);
			}
		}
	};

	private String curCalleeAddress = "";
	public String getYourSipServer(String CalleeAddress) {
//		String sip_ip = mySipServer;
		curCalleeAddress = CalleeAddress;
		String sip_ip = mPref.read("mySipServer", mySipServer);  //tml*** xcountry sip
//		String ipTmp = ContactsOnline.getContactSipIP(CalleeAddress);
//		if (ipTmp != null) {
//			Log.d("voip.getYourSipServer getContactSipIP " + ipTmp);
//			return ipTmp;
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
			if (attemptCall) return;
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
					
			if (attemptCall) return;
			
			if (mPref.readBoolean("usestanleysip"))
			{
				if (AireVenus.getCallType()==AireVenus.CALLTYPE_AIRECALL)
					return;
			}
			
			if (DialerActivity.getDialer()==null || AireVenus.runAsSipAccount)//alec
			{
				if (MyUtil.CheckServiceExists(getApplicationContext(),
						"com.pingshow.voip.AireVenus")) {
					Log.e("!!! STOPPING AireVenus/ServiceY *** AJ @ voip.callend||reconnect :: NULLdialer||runasSipAcc");
					Intent VoipIntent = new Intent(AireJupiter.this, AireVenus.class);
					stopService(VoipIntent);
				}
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
			}
			
			if (mPref.readBoolean("usestanleysip"))
			{
				mHandler.postDelayed(startServiceY_ForAireCall, 6000);
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
	private String mDisplayname=null;
	private long mRow_id;
	private int mGroupID;
	private String mGroupName;
	
	BroadcastReceiver InternalCommand = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context,final Intent intent) {
			if (intent.getAction().equals(Global.Action_InternalCMD)) {
				int command = intent.getIntExtra("Command", 0);
				switch (command) {
				case Global.CMD_SEARCH_POSSIBLE_FRIENDS:
					new Thread(new Runnable(){
						public void run()
						{
							MyUtil.Sleep(5000);
							Log.i("tmlf CMD_SEARCH_POSSIBLE_FRIENDS Sleep 5s, rdyGO");
							searchPossibleFriends.run();
						}
					}).start();
					break;
				case Global.CMD_STRANGER_COMING:
					UnknownAddress = intent.getStringExtra("Address");
					UnknownIdx = intent.getIntExtra("Idx",0);
					AnnoyingUser = intent.getBooleanExtra("Annoying",false);
					Log.i("addF.Unknown addr/idx=" + UnknownAddress + " " + UnknownIdx);
//					if (UnknownAddress == null || UnknownIdx <= 0) return;
//					mRDB.insertUser(UnknownAddress, UnknownIdx);
//					if (AddAsFriendActivity.getInstance()!=null) {
//						if (AddAsFriendActivity.getTopAddress().equals(UnknownAddress)) {
//							return;
//						}
//					}
					//tml*** getuserinfo
					if (UnknownIdx <= 0) {
						Log.e("addF !@#$ UnknownIdx<=0");
						return;
					}
					if (!TextUtils.isEmpty(UnknownAddress)) {
						mRDB.insertUser(UnknownAddress, UnknownIdx);
						if (AddAsFriendActivity.getInstance() != null) {
							if (AddAsFriendActivity.getTopAddress().equals(UnknownAddress)) {
								Log.d("addF AddAsFriendActivity.getTopAddress()");
								return;
							}
						}
					}
					//***tml
					
					(new Thread(new Runnable(){
						public void run()
						{
//							String nickname=mRDB.getNicknameByAddress(UnknownAddress);
//							String Return="";
//							if (nickname.length()==0||nickname.equals("Stranger?"))
//							{
//								int c=0;
//								do{
//									MyNet net = new MyNet(AireJupiter.this);
//									Return = net.doPostHttps("getusernickname.php","idx="+Integer.toHexString(UnknownIdx),null);
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
									Return = net.doPost("getuserinfo.php","idx=" + UnknownIdx
											+ "&id=" + myidx + "&password=" + mypw, null);
									if (Return.length() > 5) break;
									MyUtil.Sleep(500);
								} while (c++ < 3);
							} else {
								Return = "Done=" + UnknownAddress + "<Z>" + nickname;
							}
							//***tml
							
							String localfile = Global.SdcardPath_inbox + "photo_" + UnknownIdx + "b.jpg";
							File f = new File(localfile);
							if (!f.exists()) downloadPhoto(UnknownIdx, localfile);
							
							if (Return.length() > 5 && Return.startsWith("Done=")) {
//								nickname=Return.substring(5);
//								mRDB.updateNicknameByUID(UnknownIdx, nickname);
								//tml*** getuserinfo
								Return = Return.replace("Done=","");
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
								if (!mRDB.isFafauser(UnknownAddress) || !mRDB.isFafauser(UnknownIdx))
									mRDB.insertUser(UnknownAddress, UnknownIdx);
								if (mRDB.isFafauser(UnknownIdx))
									mRDB.updateNicknameByUID(UnknownIdx, nickname);
								//***tml
								
								if (!mPref.readBoolean("BlockStrangers", false) || mADB.isUserDeleted(UnknownAddress))//If blocking strangers
								{
									Intent it = new Intent(AireJupiter.this, AddAsFriendActivity.class);
									it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									it.putExtra("Address", UnknownAddress);
									it.putExtra("Idx", UnknownIdx);
									it.putExtra("Nickname", nickname);
									it.putExtra("Stranger", 1);
									it.putExtra("Annoying", AnnoyingUser);
									
									if (DialerActivity.getDialer()==null)
										startActivity(it);
									
									showNotification(String.format(getString(R.string.accept_this_stranger),nickname),
											it, true, R.drawable.icon_sms, null);
								} else {
									Log.e("addF.!@#$ BlockingStrangers");
								}
							} else {
								Log.e("addF.!@#$ getusernickname.php");
								if (!TextUtils.isEmpty(UnknownAddress)) {  //tml*** getuserinfo
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
					processIncomingSMS(intent.getStringExtra("originalSignal"));
					break;
				case Global.CMD_TRIGGER_SENDEE:  //tml upload3
					new Thread(new Runnable() {
						public void run() {
							if (tcpSocket != null) {
								if (!tcpSocket.isLogged(false))
									tcpSocket.Login(versionCode);

								mRow_id = intent.getLongExtra("row_id", 0);
								String Sendee = intent.getStringExtra("Sendee");
								int GroupID = intent.getIntExtra("GroupID", 0);
								Log.d("msg CMD_TRIGGER_SENDEE:" + Sendee + "/" + GroupID + " " + mRow_id);
								tcpSocket.send(Sendee,
									intent.getStringExtra("MsgText"),
									intent.getIntExtra("Attached", 0),
									intent.getStringExtra("remoteAudioPath"),
									intent.getStringExtra("remoteImagePath"),mRow_id,intent.getStringExtra("phpIP"),GroupID);
							} else {
								Log.e("msg CMD_TRIGGER_SENDEE tcpSocket !@#$");
							}
						}
					}).start();
					break;
				case Global.CMD_JOIN_A_NEW_GROUP://alec
					mGroupID = intent.getIntExtra("GroupID", 0);
					new Thread(new Runnable(){
						public void run()
						{
							String Return="";
							try {
								int c = 0;
								do {
									MyNet net = new MyNet(AireJupiter.this);
									Return = net.doPostHttps("query_group.php", "id=" + mGroupID, null);
									if (Return.startsWith("Done"))
										break;
									MyUtil.Sleep(2500);
								} while (++c < 3);
							} catch (Exception e) {
								Log.e("aj9 " + e.getMessage());
							}
							
							if (Return.startsWith("Done"))
							{
								try{
									Return=Return.substring(5);
									int s=Return.lastIndexOf(" ");
									mGroupName=Return.substring(0, s);
									
									Return=Return.substring(s+1);
									
									String [] m=Return.split(",");
									String creator="";
									
									GroupDB gdb=new GroupDB(AireJupiter.this);
									gdb.open();
									
									for (int i=0;i<m.length;i++)
									{
										int idx=Integer.parseInt(m[i]);
										if (idx==myIdx) continue;
										if (creator.length()==0)
											creator=mADB.getNicknameByIdx(idx);
										gdb.insertGroup(mGroupID, mGroupName, idx);
									}
									gdb.close();
									
									String localfile = Global.SdcardPath_inbox + "photo_" + (mGroupID + 100000000) + ".jpg";
									File f=new File(localfile);
									if (!f.exists())
									{
										String remotefile = "groups/photo_" + mGroupID + ".jpg";
										downloadAnyPhoto(remotefile, localfile, 3, true);
									}
									
									//handle unknown members:
									for (int i=0;i<m.length;i++)
									{
										int idx=Integer.parseInt(m[i]);
										if (idx==myIdx) continue;
										if (!mADB.isFafauser(idx))
										{
											if (tcpSocket.isLogged(false))
												tcpSocket.queryUserAddressByIdx(idx);//it will popup stranger dialog
											MyUtil.Sleep(3000);
										}
									}
									
									Intent it = new Intent(AireJupiter.this, JoinNewGroupActivity.class);
									it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									it.putExtra("Address", "[<GROUP>]"+mGroupID);
									it.putExtra("Idx", mGroupID+100000000);
									it.putExtra("Nickname", mGroupName);
									it.putExtra("Creator", creator);
									it.putExtra("GroupId", mGroupID);
									
									if (DialerActivity.getDialer()==null)
										startActivity(it);
									
									showNotification(String.format(getString(R.string.add_in_group),creator,mGroupName),
											it, true, R.drawable.icon_sms, null);
								}catch(Exception e){
									Log.e("aj10 " + e.getMessage());
								}
							}
						}
					}).start();
					break;
				case Global.CMD_JOIN_A_NEW_GROUP_VERIFIED://alec
					mGroupID = intent.getIntExtra("GroupID", 0);
					mGroupName = intent.getStringExtra("GroupName");
					new Thread(new Runnable(){
						public void run()
						{
							mADB.insertUser("[<GROUP>]"+mGroupID, mGroupID + 100000000, mGroupName);
							
							UserPage.needRefresh=true;
					    	
					    	Intent intent = new Intent(Global.Action_Refresh_Gallery);
							sendBroadcast(intent);
						}
					}).start();
					break;
				case Global.CMD_LEAVE_GROUP://some member leaves the group, and he notifies me.
					mGroupID = intent.getIntExtra("GroupID", 0);
					UnknownIdx = intent.getIntExtra("idx", 0);
					new Thread(new Runnable(){
						public void run()
						{
							GroupDB gdb=new GroupDB(AireJupiter.this);
							gdb.open();
							gdb.deleteGroupMember(mGroupID,UnknownIdx);
							int c=gdb.getGroupMemberCount(mGroupID);
							
							if (c==0)
							{
						    	mADB.deleteContactByAddress("[<GROUP>]"+mGroupID);
						    	UserPage.needRefresh=true;
						    	Intent intent = new Intent(Global.Action_Refresh_Gallery);
								sendBroadcast(intent);
								
								gdb.deleteGroup(mGroupID);
								
								try{
									mSmsDB.deleteThreadByAddress("[<GROUP>]"+mGroupID);
								}catch(Exception e){
									Log.e("aj11 " + e.getMessage());
								}
							}
							gdb.close();
						}
					}).start();
					break;
				case Global.CMD_DELETE_GROUP://I am going to delete a group, and I have to notify others.
					mGroupID = intent.getIntExtra("GroupID", 0);
					new Thread(new Runnable(){
						public void run()
						{
							try{
								GroupDB gdb=new GroupDB(AireJupiter.this);
								gdb.open();
								ArrayList<String> sendeeList=gdb.getGroupMembersByGroupIdx(mGroupID);
								ArrayList<String> addressList=new ArrayList<String>();
								for (int i=0;i<sendeeList.size();i++)
								{
									String address=mADB.getAddressByIdx(Integer.parseInt(sendeeList.get(i)));
									addressList.add(address);
								}
								SendAgent agent=new SendAgent(AireJupiter.this, myIdx, 0, true);
								
								agent.setAsGroup(mGroupID);
								agent.onMultipleSend(addressList, ":((Sk)", 0, null, null);
								
								gdb.deleteGroup(mGroupID);
								gdb.close();
								
								try {
									MyNet net = new MyNet(AireJupiter.this);
									net.doPostHttps("remove_group_member.php", "id=" + mGroupID
				    						+"&members=" + myIdx, null);
								} catch (Exception e) {
									Log.e("aj12 " + e.getMessage());
								}
							}catch(Exception e){
								Log.e("aj13 " + e.getMessage());
							}
						}
					}).start();
					break;
				case Global.CMD_GROUP_ADD_NEW_MEMBER:
					mGroupID = intent.getIntExtra("GroupID", 0);
					UnknownIdx = intent.getIntExtra("idx", 0);
					if (UnknownIdx<=0) break;
					new Thread(new Runnable(){
						public void run()
						{
							String mNickname=getString(R.string.unknown_person);
							if (!mADB.isFafauser(UnknownIdx))
							{
								if (tcpSocket.isLogged(false))
									tcpSocket.queryUserAddressByIdx(UnknownIdx);//it will popup stranger dialog
							}
							
							mNickname=mADB.getNicknameByIdx(UnknownIdx);
							
							GroupDB gdb=new GroupDB(AireJupiter.this);
							gdb.open();
							mGroupName=gdb.getGroupNameByGroupIdx(mGroupID);
							gdb.insertGroup(mGroupID, mGroupName, UnknownIdx);
							gdb.close();
							
							Intent it = new Intent(AireJupiter.this, CommonDialog.class);
							it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							String title=String.format(getString(R.string.group_invite_new_member),mNickname,mGroupName);
							it.putExtra("msgContent", title);
							it.putExtra("numItems", 1);
							it.putExtra("ItemCaption0", getString(R.string.OK));
							it.putExtra("ItemResult0", 0);
							showNotification(title, null, true, R.drawable.icon_sms, null);
							startActivity(it);
						}
					}).start();
					break;
				case Global.CMD_ADD_AS_RELATED_FRIEND:
					try {
						mGroupID = intent.getIntExtra("GroupID", 0);
						mGroupName = intent.getStringExtra("GroupName");
						mRDB.insertUser("[<GROUP>]"+mGroupID, 100000000+mGroupID, mGroupName, 0);
						
						Intent it = new Intent(Global.Action_Refresh_Gallery);
						sendBroadcast(it);
					} catch (Exception e) {
						Log.e("aj14 " + e.getMessage());
					}
					break;
				case Global.CMD_UPDATE_SENT_SMS_TIME:
					// alec: updates the status of the sent msg
					long sentTime = intent.getLongExtra("sentTime", 0);
					try {
						mSmsDB.setMessageSentById(mRow_id, SMS.STATUS_SENT, sentTime);
					} catch (Exception e) {
						Log.e("aj15 " + e.getMessage());
					}
					break;
				case Global.CMD_RECONNECT_SOCKET:
					if (tcpSocket != null)
						tcpSocket.logged = 0;
					mHandler.removeCallbacks(reconnectDebounce);
					mHandler.postDelayed(reconnectDebounce,4000);
					break;
				case Global.CMD_CHECK_ONLINE_FRIENDS:
					Thread thr = new Thread(mInstantQueryOnlineFriends,
							"Update Friend Status");
					thr.start();
					break;
				case Global.CMD_CHECK_ONLINE_FRIENDS_NOW:
					mInstantQueryOnlineFriends.run();
					break;
				case Global.CMD_SUDDENLY_NO_NETWORK:
					ContactsOnline.setAllfriendsOffline();
					RWTOnline.setAllfriendsOffline();
					Log.e("CMD_SUDDENLY_NO_NETWORK");
					if (tcpSocket != null)
						tcpSocket.disconnect("no network", true);
					if (rwtSocket != null)
						rwtSocket.disconnect();
					stopBeeHiveService();
					break;
				case Global.CMD_ONLINE_UPDATE:
					Thread thr2 = new Thread(mDownloadnUpdate, "download new apk");
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
					String title=Nickname + " " + getString(R.string.sharing_location_request);
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
					Log.e("CMD_INCOMING_CALL");
					mHandler.removeCallbacks(mEndupServiceY);
					startServiceY(AireVenus.CALLTYPE_FAFA);
					break;
				case Global.CMD_MAKE_OUTGOING_CALL:
					if (tcpSocket == null) return;
					calleeNumber = intent.getStringExtra("Callee");
					if (!MyUtil.checkNetwork(getApplicationContext())) {
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
					int idxIN = mPref.readInt("tempCheckSameIN", 0);
					Log.e("Check preDA SAMETIME in/out> " + idxIN + "/" + idxOUT);
					if (idxIN == idxOUT) {
						Log.e("tml SAMETIME!!! (preDA)");
						mPref.write("tempCheckSameIN", 0);
						Toast tst = Toast.makeText(AireJupiter.this,
								getString(R.string.call) + ": "
								+ getString(R.string.call_declined),
								Toast.LENGTH_LONG);
						tst.setGravity(Gravity.CENTER, 0, 0);
						LinearLayout tstLayout = (LinearLayout) tst.getView();
						TextView tstTV = (TextView) tstLayout.getChildAt(0);
						tstTV.setTextSize(30);
						tst.show();
						break;
					}
					//***tml
					
					attemptCall = true;

					if (AireVenus.getCallType()==AireVenus.CALLTYPE_FAFA || AireVenus.getCallType()==AireVenus.CALLTYPE_FILETRANSFER)
						new Thread(new Runnable() {
							public void run() {
								if (tcpSocket == null) return;
								android.os.Process.setThreadPriority(-19);
								
								calleeGotCallRequest = false;
								
								if (!tcpSocket.isLogged(false)) {  //tml*** precall login
									Log.e("CMD_MAKE_OUTGOING_CALL *** !tcpSocket.isLogged() ***");
									tcpSocket.Login(versionCode);
								}
								
								if (AireVenus.getCallType()==AireVenus.CALLTYPE_FAFA || AireVenus.getCallType()==AireVenus.CALLTYPE_FILETRANSFER)
								{
									if (tcpSocket != null && tcpSocket.isLogged(false)) {
										int ret = tcpSocket.sendCallRequest(calleeNumber);
										
										if (ret == 1)
											calleeGotCallRequest = true;
										else if (ret < 1) {
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
						calleeGotCallRequest=true;

					mHandler.post(new Runnable() {
						public void run() {
							
							if (AireVenus.getLc()!=null && MyUtil.CheckServiceExists(getApplicationContext(), "com.pingshow.voip.AireVenus"))
			    	        {
								boolean sipcall=(AireVenus.getCallType()==AireVenus.CALLTYPE_AIRECALL
										||AireVenus.getCallType()==AireVenus.CALLTYPE_CHATROOM
										||AireVenus.getCallType()==AireVenus.CALLTYPE_WEBCALL);
								if ((sipcall && !AireVenus.runAsSipAccount) || (!sipcall && AireVenus.runAsSipAccount))
								{
									Log.e("!!! STOPPING AireVenus/ServiceY *** AJ @ voip.outgoingCall :: sipcall!=runasSipAcc");
			    					Intent itx=new Intent(AireJupiter.this, AireVenus.class);
			    		    		stopService(itx);
			    		    		MyUtil.Sleep(3000);
								}
								boolean sendingFile=AireVenus.getCallType()==AireVenus.CALLTYPE_FILETRANSFER;
								if ((sendingFile && !AireVenus.runAsFileTransfer) || (!sendingFile && AireVenus.runAsFileTransfer))
								{
									AireVenus.instance().enableDisableCodec("AMR",8000,!sendingFile);
									AireVenus.instance().enableDisableCodec("speex",16000,!sendingFile);
								}
			    	        }
							
							if (!MyUtil.CheckServiceExists(getApplicationContext(), "com.pingshow.voip.AireVenus")) {
								
								if (mPref.readBoolean("doingUPNP",false))
								{
									MyUtil.Sleep(3000);
								}
								
								Intent VoipIntent = new Intent(Intent.ACTION_MAIN);
								VoipIntent.setClass(AireJupiter.this, AireVenus.class);
								startService(VoipIntent);
							}
							else{
								mHandler.removeCallbacks(mEndupServiceY);
								Log.d(AireVenus.CALLTYPE_AIRECALL+"==1??");
								if (AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL) {

									try {
										AireVenus.instance().initFromConf();
									} catch (VoipConfigException e) {
										Log.e("aj16 " + e.getMessage());
									} catch (VoipException e) {
										Log.e("aj17 " + e.getMessage());
									}

								}else{
									AireVenus.sip_login(mySipServer, myPhoneNumber);
								}
							}
						}
					});

					calleeContact_id = intent.getLongExtra("Contact_id", -1);
					calleeNumber = intent.getStringExtra("Callee");
					MakeVideoCall = intent.getBooleanExtra("VideoCall",true);
					
					if (intent.getBooleanExtra("Commercial",false))
						mDisplayname=intent.getStringExtra("Displayname");
					else
						mDisplayname=null;

					mHandler.postDelayed(new Runnable() {
						public void run() {
							Intent intent= new Intent(AireJupiter.this, DialerActivity.class);

							mPref.write("curCall", calleeNumber);
							if (mDisplayname==null)
								mDisplayname=mADB.getNicknameByAddress(calleeNumber);
							Log.d("toDialer! " + calleeContact_id + " " + calleeNumber + " " + mDisplayname + " " + MakeVideoCall);
							intent.putExtra("Contact_id", calleeContact_id);
							intent.putExtra("PhoneNumber", calleeNumber);
							intent.putExtra("DisplayName", mDisplayname);
							intent.putExtra("VideoCall", MakeVideoCall);
							intent.putExtra("Selfinit", true);  //tml*** abort dialer
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
							AireJupiter.this.startActivity(intent);
						}
					}, 100);
					break;
				case Global.CMD_CALL_END:
					Log.e("attempt CMD_CALL_END " + !attemptCall);
					if (attemptCall) return;
					int timeup=intent.getIntExtra("immediately", 2000);
					if (timeup>0)
					{
						Log.e("*** !!! Stop VENUS/ServiceY after "+timeup/1000+" seconds");
						mHandler.postDelayed(mEndupServiceY, timeup);
					}
					
					if (intent.getBooleanExtra("AireCall",false))
					{
						callingOut=true;
						mHandler.postDelayed(getSipCredit,2000);
					}
					break;
				case Global.CMD_LOGIN_FAILED:// alec
					unautherized999=true;
					
					long last = mPref.readLong("last_time_popup_999", 0);
					long now = new Date().getTime();
					if (now - last < 600000) // 10 minute
						return;// no need to popup
					
					mPref.write("accountUpdated", true);
					mPref.writeLong("last_time_popup_999", now);
					break;
				case Global.CMD_TCP_CONNECTION_UPDATE:
					unautherized999=false;
					break;
				case Global.CMD_CONNECTION_POOR:  //tml*** tcp test
					Log.e("CMD_CONNECTION_POOR");
					boolean force = intent.getBooleanExtra("ForcePoor", false);
					String from = intent.getStringExtra("warnFrom");
					int length = intent.getIntExtra("length01", 0);
					if (from == null) from = "POOR";
					toastWarning(1, force, from, length);
					break;
				case Global.CMD_REFRESH_CONN:  //tml*** tcp test
					Log.e("CMD_REFRESH_CONN");
					refreshWifi();
					break;
				case Global.CMD_UPLOAD_PROFILE_PHOTO:
					new Thread(new Runnable() {
						@Override
						public void run() {
							if (myIdx==0) return;
							String Return = "";
							try {
								int count = 0;
								String photoPath = mPref.read("myPhotoPath");
								do {
									MyNet net = new MyNet(AireJupiter.this);
									Return = net.doPostAttach("uploadphoto_aire.php", myIdx, 0, photoPath, null); // httppost
									if (Return.startsWith("Done"))
										break;
									MyUtil.Sleep(2500);
								} while (++count < 3);
							} catch (Exception e) {
								Log.e("aj18 " + e.getMessage());
							}
							
							if (Return.startsWith("Done")){
								tellFriendsProfileChanged(0, null);
								mPref.write("myPhotoUploaded", true);
							} else
								mPref.write("myPhotoUploaded", false);
						}
					}).start();
					break;
				case Global.CMD_UPLOAD_PROFILE_MOOD:
					if (tcpSocket==null) return;
					new Thread(new Runnable() {
						@Override
						public void run() {
							String moodContent = mPref.read("moodcontent");
							try {
								int count = 0;
								do {
									MyNet net = new MyNet(AireJupiter.this);
									String Return = net.doPostHttps("update_mood.php", "id=" + URLEncoder.encode(myPhoneNumber,"UTF-8")
				    						+"&password=" + URLEncoder.encode(myPasswd, "UTF-8")
				    						+"&mood=" + URLEncoder.encode(moodContent, "UTF-8"), null);
				    				if (Return.startsWith("Done"))
										break;
									MyUtil.Sleep(1500);
								} while (count++ < 3);
							} catch (Exception e) {
								Log.e("aj19 " + e.getMessage());
							}
							
							if(!tcpSocket.isLogged(false))
								mPref.write("moodcontentuploaded", false);
							else
								mPref.write("moodcontentuploaded", true);
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
				    				Return = net.doPostHttps("update_email.php", "id=" + URLEncoder.encode(myPhoneNumber,"UTF-8")
				    						+"&password="+ URLEncoder.encode(myPasswd, "UTF-8")
				    						+"&email=" + URLEncoder.encode(email, "UTF-8"), null);
				    				if (Return.startsWith("Done"))
				    					break;
				    				count++;
				    				MyUtil.Sleep(1500);
				    			} while (count < 4);
				    		} catch (Exception e) {
				    			Log.e("aj20 " + e.getMessage());
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
				    		try {
				    			int count = 0;
				    			String myNickname = mPref.read("myNickname");
				    			String gender=mPref.read("myGender","male");
				    			String myFacebookID=mPref.read("myFacebookID","");
				    			String myWeiboID=mPref.read("myWeiboID","");
				    			
				    			myIdx=Integer.parseInt(mPref.read("myID","0"),16);
				    			do {
				    				Return = net.doPostHttps("updateprofile_x.php", "idx=" + myIdx
				    						+"&gender="+gender
				    						+"&password=" + URLEncoder.encode(myPasswd, "UTF-8")
				    						+"&facebook="+myFacebookID
				    						+"&weibo="+myWeiboID
				    						+"&nickname=" + URLEncoder.encode(myNickname, "UTF-8")
				    						+"&version="+versionCode
				    						+"&device="+URLEncoder.encode(Build.BRAND+"/"+Build.PRODUCT+"/"+Build.MODEL, "UTF-8")
				    						, null);
				    				if (Return.startsWith("Done"))
				    					break;
				    				count++;
				    				MyUtil.Sleep(1500);
				    			} while (count < 4);
				    		} catch (Exception e) {
				    			Log.e("aj21 " + e.getMessage());
				    		}
				      
			    			mPref.write("nicknameUpdated", Return.startsWith("Done"));
				    	}
				    }).start();
				    break;
				case Global.CMD_UPDATE_CALL_LOG:
					mHandler.removeCallbacks(mUpdateCallLog);
					callLogBundle = intent.getExtras();
					
					if (!intent.getBooleanExtra("runAsSip",false))
						mHandler.postDelayed(mUpdateCallLog, 3000);
					else if (intent.getIntExtra("status",0)==1)//webcall log
					{
						//new Thread(mUpdateWebCallLog).start();
					}
					break;
				case Global.CMD_QUERY_360:
					new Thread(searchFriendsByPhonebook).start();
					break;
				case Global.CMD_UPLOAD_FRIENDS:
					uploadBuddyList(intent.getStringExtra("idxlist"),intent.getIntExtra("serverType",1));
					break;
				case Global.CMD_DOWNLOAD_PHOTO_FROMNET:
					int type = intent.getIntExtra("type",0);
					if (type==0)
						mHandler.post(new Runnable() {
							public void run() {
								new Thread(new Runnable(){
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
								new Thread(new Runnable(){
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
								UserPage.forceRefresh = true;
								getFriendNicknames();
							}
						});
					} else {
						new Thread(downloadFriendList).start();
					}
					//***tml
					break;
				case Global.CMD_FILE_TRANSFERED:
					String filename=intent.getStringExtra("Filename");
					if (filename!=null)
					{
						OpenDifferentFile openDifferentFile = new OpenDifferentFile(AireJupiter.this);
						openDifferentFile.openFile(filename);
					}
					break;
				case Global.CMD_UPDATE_SIP_CREDIT:
					mHandler.postDelayed(getSipCredit,1000);
					break;
				case Global.CMD_PARTITION_FILE:
					break;
				case Global.CMD_SUV_CALLLIMIT:  //tml*** suv call limit
					new Thread(new Runnable() {
						public void run() {
							int count = 0;
							List<String> instants = mPref.readArray("instants");
							String myNick = mPref.read("myNickname", "unknown");
		        			if (instants != null) {
		        				for (String address: instants) {
		        					tcpSocket.send(address, "(" + myNick + ") "
		        							+ getString(R.string.alertCallLimit), 0, null, null, 0, null);
		        					count++;
		        					if (count > SecurityNewActivity.MAXUSERS + 1) return;
		        					MyUtil.Sleep(1000);
		        				}
		        			}
							
						}
					}).start();
					break;
				}
			}
			else if (intent.getAction().equals(Global.Action_Start_Surveillance))
			{
				Log.d("suv Action_Start_Surveillance");
				int command = intent.getIntExtra("Command", 0);
				String address = intent.getStringExtra("address");
				boolean myself = intent.getBooleanExtra("fromMe", false);
				
				if (command == Global.CMD_SUVALARM_ON) {
					prepareAlarm();
				} else if (command == Global.CMD_SUVALARM_OFF) {
					stopAlarm();
				} else {
					boolean busy = intent.getBooleanExtra("busy", false);  //tml*** suv busy
					if (SurveillanceDialog.getInstance() != null || busy)
					{
						new Thread(new Runnable() {
							public void run() {
								String address = intent.getStringExtra("address");
								tcpSocket.send(address, "Sorry, I am busy.", 0, null, null, 0, null);
							}
						});
						return;
					} else {
						String nickname = "";
						if (myself) nickname = mPref.read("myNickname", "");
						else nickname = mADB.getNicknameByAddress(address);
						Intent it=new Intent(AireJupiter.this, SurveillanceDialog.class);
						it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						it.putExtra("nickname", nickname);
						startActivity(it);
						
						new Thread(new Runnable() {
							public void run() {
								//tml*** suv ctrl id
								String xaddress = intent.getStringExtra("address");
								boolean myself = intent.getBooleanExtra("fromMe", false);
								String nicknameOn = "";
								if (myself) nicknameOn = mPref.read("myNickname", "");
								if (xaddress != null && xaddress.length() > 0)
									nicknameOn = mADB.getNicknameByAddress(xaddress);
								
								int count = 0;
								List<String> instants = mPref.readArray("instants");
			        			if (instants != null) {
			        				for (String address : instants) {
			        					tcpSocket.send(address, getString(R.string.security_on)
			        							+ "  (" + getString(R.string.surveillance)
			        							+ ")  < " + nicknameOn
			        							, 0, null, null, 0, null);  //tml*** suv ctrl id
			        					count++;
			        					if (count > SecurityNewActivity.MAXUSERS + 1) return;
			        					MyUtil.Sleep(1000);
			        				}
			        			}
								
							}
						}).start();
					}
				}
			
			}
			else if (intent.getAction().equals(Global.Action_Start_Homesensor))  //tml*** iot control
			{
				Log.d("suv-Home Action_Start_Homesensor");
				mPref.write("securityHomeIOT", true);
				
				if (SecurityNewActivity.getInstance() != null) {
					SecurityNewActivity.getInstance().updateHomeIOTStatus();
				}
				
				new Thread(new Runnable() {
					public void run() {
						//tml*** suv ctrl id
						String xaddress = intent.getStringExtra("address");
						String nicknameOn = "";
						if (xaddress != null && xaddress.length() > 0)
							nicknameOn = mADB.getNicknameByAddress(xaddress);
						boolean fromMe = intent.getBooleanExtra("fromMe", false);
						if (fromMe) nicknameOn = mPref.read("myNickname", "");
						
						int count = 0;
						List<String> instants = mPref.readArray("instants");
	        			if (instants != null) {
	        				for (String address : instants) {
	        					tcpSocket.send(address, getString(R.string.security_on)
	        							+ "  (" + getString(R.string.home_iot_sensors)
	        							+ ")  < " + nicknameOn
	        							, 0, null, null, 0, null);  //tml*** suv ctrl id
	        					count++;
	        					if (count > SecurityNewActivity.MAXUSERS + 1) return;
	        					MyUtil.Sleep(1000);
	        				}
	        			}
						
					}
				}).start();
			}
			else if (intent.getAction().equals(Global.Action_End_Homesensor))  //tml*** iot control
			{
				Log.d("suv-Home Action_End_Homesensor");
				mPref.write("securityHomeIOT", false);
				
				if (SecurityNewActivity.getInstance() != null) {
					SecurityNewActivity.getInstance().updateHomeIOTStatus();
				}
				
				new Thread(new Runnable() {
					public void run() {
						//tml*** suv ctrl id
						String xaddress = intent.getStringExtra("address");
						String nicknameOff = "";
						if (xaddress != null && xaddress.length() > 0)
							nicknameOff = mADB.getNicknameByAddress(xaddress);
						boolean fromMe = intent.getBooleanExtra("fromMe", false);
						if (fromMe) nicknameOff = mPref.read("myNickname", "");
						
						int count = 0;
						List<String> instants = mPref.readArray("instants");
	        			if (instants != null) {
	        				for (String address : instants) {
	        					tcpSocket.send(address, getString(R.string.security_off)
	        							+ "  (" + getString(R.string.home_iot_sensors)
	        							+ ")  < " + nicknameOff
	        							, 0, null, null, 0, null);  //tml*** suv ctrl id
	        					count++;
	        					if (count > SecurityNewActivity.MAXUSERS + 1) return;
	        					MyUtil.Sleep(1000);
	        				}
	        			}
						
					}
				}).start();
			}
			else if (intent.getAction().equals(Global.Action_SD_AvailableSpare)) {
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
				it = new Intent(AireJupiter.this, MainActivity.class);
				if (intent.getBooleanExtra("err", false)) {
					Toast.makeText(AireJupiter.this,getString(R.string.downloaderror),Toast.LENGTH_SHORT).show();
					if (AireJupiter.getInstance() != null) {
						AireJupiter.getInstance().showNotification(filename, it,
								true, R.drawable.icon_sms,
								getString(R.string.downloaderror));
					}
				} else {
					Toast.makeText(AireJupiter.this,getString(R.string.downloadsucess),Toast.LENGTH_SHORT).show();

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
						Log.e("aj22 " + e.getMessage());
					}
					// refresh ConversationActivity listview
					it = new Intent();
					it.setAction(Global.Action_MsgGot);
					sendBroadcast(it);
				}
				ConversationActivity.fileDownloading = false;
			} else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
				//will skip and auto launch security page
				Log.w("myusb  detect connection");
				UsbDevice usbDevice = findMyUsbDevice(true, 0, 0, 0);
				if (usbDevice != null) {
					//device 1
					initMyUsbDongle(dongle1_found, null, dongle1_code, dongle1_pid, dongle1_vid);
					//device 2
					initMyUsbDongle(dongleTest_found, null, dongleTest_code, dongleTest_pid, dongleTest_vid);
				} else {
					Log.w("myusb  other/unknown connected");
				}
			} else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
				//tml*** usb dongle
				Log.w("myusb  detect disconnect");
				dongle1_found = mPref.readBoolean("DONGLE1_FOUND", false);
				dongleTest_found = mPref.readBoolean("DONGLET_FOUND", false);
				//device 1
				UsbDevice usbDevice = findMyUsbDevice(false, dongle1_code, dongle1_pid, dongle1_vid);
				if (usbDevice == null) {
					abortUsbDongle(dongle1_code);
				}
				//device 2
				usbDevice = findMyUsbDevice(false, dongleTest_code, dongleTest_pid, dongleTest_vid);
				if (usbDevice == null) {
					abortUsbDongle(dongleTest_code);
				}
			} else if (intent.getAction().equals(Global.Action_UsbPermission)) {
			} else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
				//tml*** blue comm
//				Log.w("myblue  detect connection");
//				BluetoothDevice blue = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//				if (blue != null) {
//					String blueName = blue.getName();
//					Log.e("myblue  found " + blueName);
//				} else {
//					Log.e("myblue  no blue found");
//				}
			} else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
				//tml*** blue comm
//				Log.w("myblue  detect disconnect");
//				BluetoothDevice blue = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			} else if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
				//tml*** blue comm
//				Log.w("myblue  detect state change");
//				BluetoothDevice blue = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//				boolean stillBonded1 = true;
//				if (blue != null) {
//					String blueName = blue.getName();
//					String blueAddr = blue.getAddress();
//					int blueBond = blue.getBondState();
//					Log.e("myblue  name=" + blueName + "  addr=" + blueAddr + "  bond=" + blueBond);
//					if (blueAddr.equals(myBlueMAC1) && blueBond == BluetoothDevice.BOND_NONE) {
//						stillBonded1 = false;
//					}
//				}
//				if (mBlueComm != null && !stillBonded1)
//					mBlueComm.setBlueComm(AireJupiter.this, null, null);
			} else if (intent.getAction().equals(BluetoothDevice.ACTION_UUID)) {
				//tml*** blue comm
//				BluetoothDevice blue = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//				Parcelable[] pExtras = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
//				
//				String blueName = blue.getName();
//				String blueAddr = blue.getAddress();
//				int blueBond = blue.getBondState();
//				int blueType = blue.getType();
//				BluetoothClass blueClasses = blue.getBluetoothClass();
//				int blueClass = blueClasses.getDeviceClass();
//				int blueClassMajor = blueClasses.getMajorDeviceClass();
//				String blueUUIDS = "";
//				String ioUUID = null;
//				for (Parcelable pblueUuids : pExtras) {
//					String uuid = pblueUuids.toString().toLowerCase();
//					blueUUIDS = uuid + ", " + blueUUIDS;
//					String useBlueUUID = myBlueUUID1;
//					if (mPref.readBoolean("BLUEID", false)) {
//						useBlueUUID = useBlueUUID + mPref.read("BLUEUUID", "1101");
//					} else {
//						useBlueUUID = useBlueUUID + "1101";
//					}
//					if (uuid.startsWith(useBlueUUID)) ioUUID = uuid;
//				}
//				ioUUID = "00001101-0000-1000-8000-00805f9b34fb";
//				Log.e("myblue  name=" + blueName + "  addr=" + blueAddr + "  bond=" + blueBond);
//				Log.e("myblue  type=" + blueType + "  class=" + blueClass + "  mjrclass=" + blueClassMajor);
//				Log.e("myblue  ids=" + blueUUIDS);
//				
//				if (blue != null && ioUUID != null && blueAddr.equals(myBlueMAC1)) {
//					mBlueComm = new BlueComm();
//					mBlueComm.setBlueComm(AireJupiter.this, blue, ioUUID);
//					new Thread(mBlueComm).start();
//				} else {
//					Log.e("myblue  not matching device");
//				}
			}
		}
	};
	
	//tml*** usb dongle, api 12+
	//dongle1 HC-12-USB
	private UsbManager mUsbManager;
	private UsbComm mUsbDongleComm1 = null;
	private UsbComm mUsbDongleComm2 = null;
	private boolean dongle1_found = false;
	private UsbDevice dongle1_usbDevice = null;
	private final int dongle1_code = UsbComm.dongle1_code;
	private final int dongle1_pid = UsbComm.dongle1_pid;
	private final int dongle1_vid = UsbComm.dongle1_vid;
	//test dongle (dell mouse, aire mouse, logitech mouse, myusb)
	private boolean dongleTest_found = false;
	private UsbDevice dongleTest_usbDevice = null;
	private final int dongleTest_code = UsbComm.dongleTest_code;
	private final int dongleTest_pid = UsbComm.dongleTest_pid;
	private final int dongleTest_vid = UsbComm.dongleTest_vid;
	
	public void initMyUsbDongle(boolean found, UsbDevice usbDevice, int myUsbCode, int pid, int vid) {
		if (!found || usbDevice == null) {
			usbDevice = findMyUsbDevice(false, myUsbCode, pid, vid);
		}
		if (usbDevice != null) {
			checkUsbPermissionActivity(usbDevice, myUsbCode, pid, vid);
		}
	}
	
	@SuppressLint("NewApi")
	public UsbDevice findMyUsbDevice(boolean justcheck, int myUsbCode, int pid, int vid) {
		HashMap<String, UsbDevice> usbMap = mUsbManager.getDeviceList();
		Set<String> usbSet = usbMap.keySet();
		UsbDevice usbDeviceFound = null;
		
		if (justcheck) Log.i("myusb  checking devices");
		else Log.i("myusb  checking device c" + myUsbCode + ":p" + pid + "/v" + vid);
		for (String set : usbSet) {
			UsbDevice usbDevice = usbMap.get(set);
			int foundpid = usbDevice.getProductId();
			int foundvid = usbDevice.getVendorId();
			if (justcheck) {
				if ((foundpid == dongle1_pid && foundvid == dongle1_vid)
						|| (foundpid == dongleTest_pid && foundvid == dongleTest_vid)) {
					Log.i("myusb  device(s) found");
					usbDeviceFound = usbDevice;
				}
			} else {
				if (foundpid == pid && foundvid == vid) {
					usbDongleFound(true, usbDevice, myUsbCode);
					usbDeviceFound = usbDevice;
					break;
				}
			}
		}
		if (!justcheck && usbDeviceFound == null) {
			usbDongleFound(false, null, myUsbCode);
		}
		return usbDeviceFound;
	}
	
	private void usbDongleFound(boolean found, UsbDevice usbDevice, int myUsbCode) {
		if (myUsbCode == dongle1_code) {
			if (dongle1_found) {
				if (found) {
					Log.i("myusb  dongle1 still present");
				} else {
					Log.e("myusb  dongle1 was disconnected!");
				}
			} else {
				Log.i("myusb  dongle1 c" + myUsbCode + " found=" + found);
			}
			dongle1_found = found;
			dongle1_usbDevice = usbDevice;
			mPref.write("DONGLE1_FOUND", dongle1_found);
		} else if (myUsbCode == dongleTest_code) {
			if (dongleTest_found) {
				if (found) {
					Log.i("myusb  dongleT still present");
				} else {
					Log.e("myusb  dongleT was disconnected!");
				}
			} else {
				Log.i("myusb  dongleT c" + myUsbCode + " found=" + found);
			}
			dongleTest_found = found;
			dongleTest_usbDevice = usbDevice;
			mPref.write("DONGLET_FOUND", dongleTest_found);
		} else {
			Log.e("myusb  dongle found unknown code!");
		}
	}
	
	private void checkUsbPermissionActivity(UsbDevice usbDevice, int myUsbCode, int pid, int vid) {
		if (usbDevice != null) {
			if (SecurityNewActivity.getInstance() != null) {
				Intent iToFront = new Intent(AireJupiter.this, SecurityNewActivity.class);
				iToFront.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(iToFront);
				
				Log.d("myusb  permission activity");
				Intent pIntent = new Intent(Global.Action_UsbPermissionActivity);
				pIntent.putExtra("dongle_code", myUsbCode);
				pIntent.putExtra("dongle_pid", pid);
				pIntent.putExtra("dongle_vid", vid);
				sendBroadcast(pIntent);
			} else {
				Log.w("myusb  device c" + myUsbCode + " connected, but not in Security");
//				mHandler.post(new Runnable() {
//					@Override
//					public void run() {
//						Toast tst = Toast.makeText(getApplicationContext(), "Go to Security to activate the Usb-Dongle", Toast.LENGTH_LONG);
//						tst.setGravity(Gravity.CENTER, 0, 0);
//						tst.show();
//					}
//				});
			}
			
		} else {
			Log.e("myusb  dongle !@#$ null, cancel permission");
			abortUsbDongle(myUsbCode);
		}
	}
	
	public UsbDevice getPermitUsbDevice(int myUsbCode) {
		if (myUsbCode == dongle1_code) {
			return dongle1_usbDevice;
		} else if (myUsbCode == dongleTest_code) {
			return dongleTest_usbDevice;
		} else {
			return null;
		}
	}
	
	public boolean startUsbDongleComm(int myUsbCode, boolean start, UsbDevice usbDevice, int pid, int vid) {
		UsbComm usbDongleComm = null;
//		boolean running = mPref.readBoolean("threadUsbComm" + myUsbCode, false);
//		if (running) {
//			Log.e("myusb  dongle thread already running");
//			return false;
//		}
		
		if (myUsbCode == dongle1_code) {
			if (mUsbDongleComm1 == null) {
				mUsbDongleComm1 = new UsbComm(AireJupiter.this, usbDevice, myUsbCode, pid, vid);
				usbDongleComm = mUsbDongleComm1;
			}
		} else if (myUsbCode == dongleTest_code) {
			if (mUsbDongleComm2 == null) {
				mUsbDongleComm2 = new UsbComm(AireJupiter.this, usbDevice, myUsbCode, pid, vid);
				usbDongleComm = mUsbDongleComm2;
			}
		}
		
		if (usbDongleComm != null) {
			try {
				if (start) {
					new Thread(usbDongleComm).start();
				}
				return true;
			} catch (IllegalThreadStateException e) {
				Log.e("myusb  dongle !@#$ thread already running");
				return false;
			}
		} else {
			Log.e("myusb  dongle UsbDongleComm null, maybe already running");
			return false;
		}
	}
	
	public void abortUsbDongle(int myUsbCode) {
		if (myUsbCode == dongle1_code) {
			dongle1_found = false;
			dongle1_usbDevice = null;
			if (mUsbDongleComm1 != null) {
				Log.e("myusb  dongle abort c" + myUsbCode);
				mUsbDongleComm1.abortUsbComm();
			}
			mUsbDongleComm1 = null;
		} else if (myUsbCode == dongleTest_code) {
			dongleTest_found = false;
			dongleTest_usbDevice = null;
			if (mUsbDongleComm2 != null) {
				Log.e("myusb  dongle abort c" + myUsbCode);
				mUsbDongleComm2.abortUsbComm();
			}
			mUsbDongleComm2 = null;
		} else if (myUsbCode == -1) {
			Log.e("myusb  dongle abort c" + myUsbCode);
			dongle1_found = false;
			dongle1_usbDevice = null;
			if (mUsbDongleComm1 != null) {
				mUsbDongleComm1.abortUsbComm();
			}
			mUsbDongleComm1 = null;
			dongleTest_found = false;
			dongleTest_usbDevice = null;
			if (mUsbDongleComm2 != null) {
				mUsbDongleComm2.abortUsbComm();
			}
			mUsbDongleComm2 = null;
		}
	}
	
	//tml*** blue comm
	//1101 1132 111f 1112 112f 1116 1106 1105 110a
	private final String myBlueUUID1 = "0000";
	private final String myBlueMAC1 = "48:59:29:09:2F:FA";
	private BlueComm mBlueComm = null;
	private class BlueComm implements Runnable {
		private Context _context;
		private BluetoothDevice _blueDevice;
		private String ioUUID = null;
		private BluetoothSocket blueSocket = null;
		private BluetoothServerSocket blueServerSocket = null;
		private InputStream inS = null;
		private DataInputStream dInS = null;

		public void setBlueComm(Context context, BluetoothDevice blueDevice, String uuid) {
			this._context = context;
			this._blueDevice = blueDevice;
			this.ioUUID = uuid;
			if (_blueDevice == null) {
				Log.e("myblue  nulled");
			}
		}
		
		@Override
		public void run() {
			Log.d("myblue  +++COMM+++ begin");
			
			try {
				BluetoothAdapter blueAdapter = BluetoothAdapter.getDefaultAdapter();
				BluetoothDevice blue = blueAdapter.getRemoteDevice(myBlueMAC1);
				String blueName = blue.getName();
				Log.d("myblue  connecting to " + blueName + "  " + ioUUID);
				
				blueSocket = blue.createInsecureRfcommSocketToServiceRecord(UUID.fromString(ioUUID));
				blueSocket.connect();
				
//				blueServerSocket = blueAdapter.listenUsingRfcommWithServiceRecord("inBlue", UUID.fromString(ioUUID));
//				blueSocket = blueServerSocket.accept();
				
				if (blueSocket != null) {
					Log.d("myblue  connected");
					inS = blueSocket.getInputStream();
//					dInS = new DataInputStream(inS);
					byte[] dataIN = new byte[1024];
					
					Log.d("myblue  IN active");
					int count = 0;
					while (_blueDevice != null) {
//						dInS.readFully(dataIN);
						
						if (inS.read(dataIN, 0, dataIN.length) != -1) {
							count++;
							String dataResult = MyUtil.bytesToHex(dataIN);
							Log.d("myblue  comm IN-RESULT= (" + count + ")   h." + dataResult);
						}
						
					}
				}
			} catch (IOException e) {
				Log.e("myblue   !@#$ " + e.getMessage());
				e.printStackTrace();
			}

			if (dInS != null)
				try {
					dInS.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (inS != null)
				try {
					inS.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (blueSocket != null)
				try {
					blueSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (blueServerSocket != null)
				try {
					blueServerSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			
			Log.d("myblue  ---COMM--- end");
		}
	}
	//***tml
	
	void uploadAllFriends()
	{
		Cursor cursor = mADB.fetchAll();
		if (cursor==null) return;
		String idxs="";
		int j=0;
		if (cursor.moveToFirst())
		{
			while(cursor.moveToNext())
			{
				int idx = cursor.getInt(3);
				String address = cursor.getString(1);
				if (address.startsWith("[<GROUP>]")) continue;
				if (idx <= 50) continue;
				if (j>0)
					idxs+="+";
				idxs+=idx;
				j++;
			}
		}
		if (!cursor.isClosed())
			cursor.close();
		uploadBuddyList(idxs,1);
	}
	
	String idxlist="";
	int buddylist_type;
	
	void uploadBuddyList(String idxs, int type)
	{
		idxlist=idxs;
		buddylist_type=type;
		if (idxlist!=null)
		{
			new Thread(new Runnable() {
				public void run() {
					int count=0;
					String Return="";
					String URL;
					if (buddylist_type==0)
						URL="removebuddy_aire.php";
					else
						URL="addbuddy_aire.php";
					try {
						do{
							MyNet net = new MyNet(AireJupiter.this);
							Return = net.doPostHttps(URL, "id=" + myIdx
									+ "&idxs=" + idxlist, null);
							if (Return.length() > 5 && !Return.startsWith("Error"))
								break;
							MyUtil.Sleep(500);
							if (Return.startsWith("Error")) {
							}
						}while(++count<3);
					} catch (Exception e) {
						Log.e("uploadBuddyList ohuh " + e.getMessage());
					}
				}
			}).start();
		}
		else {
			Log.e("tmlf uploadBuddyList idxlist ***NULL***");
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
			int count=0;
			String Return="";
			do{
				MyNet net = new MyNet(AireJupiter.this);//offlinemsg.php
				Return = net.doPostHttps("getsipaccount_and.php", "id="
						+ URLEncoder.encode(myPhoneNumber, "UTF-8")
						+ "&password=" + URLEncoder.encode(myPasswd, "UTF-8"), null);
				if (Return.length() > 5 && !Return.startsWith("Error"))
					break;
				MyUtil.Sleep(500);
			}while(++count<3);
			
			if (Return.startsWith("Done"))
			{
				Return=Return.substring(5);
				try{
					String [] items = Return.split("/");
					for(int i=0;i<items.length;i++)
					{
						String [] ss = items[i].split(":");
						if (ss.length==4)
						{
							int index=Integer.parseInt(ss[0]);
							mPref.write("aireSipAcount"+index, ss[1]);
							mPref.write("aireSipPassowrd"+index, ss[2]);
							mPref.write("aireSipServer"+index, ss[3]);
							Log.d("getSipFriendsAcc... user/pw/ip " + ss[1] + " " + ss[2] + " " + ss[3]);
						}
					}
					mPref.writeLong("last_time_get_sip_accounts_3", now);
				}catch(Exception e){
					Log.e("aj23 " + e.getMessage());
				}
			}
		}catch(Exception e){
			Log.e("aj24 " + e.getMessage());
		}
	};
	
	int toastResID;
	Runnable toastMessage=new Runnable()
	{
		public void run()
		{
			Toast.makeText(AireJupiter.this, toastResID, Toast.LENGTH_LONG).show();
		}
	};
	
	String locrequest = "";
	final private Runnable agreeToShareLocation = new Runnable() {
		public void run() {
			try {
				if (shared_friends==null) {
					shared_friends.add(msgGot.address);
					Log.d("agree::shared_friends===="+shared_friends);
				}else if (!shared_friends.contains(locrequest)) {
					shared_friends.add(locrequest);
				}
				mPref.writeArray("shared_friends", shared_friends);
		    	Intent intent = new Intent(Global.Action_Refresh_LOCATIONTIMER);
				sendBroadcast(intent);
				//alec
				int count=0;
				String ret="";
				while(++count<=3)
				{
					MyNet net = new MyNet(getApplicationContext());
					Log.d("count=="+count);
					ret = net.doPost("shareloc.php",
							"id=" + URLEncoder.encode(myPhoneNumber, "UTF-8")
									+ "&queryid="
									+ URLEncoder.encode(locrequest, "UTF-8")
									+ "&relation=1", null);
//					Log.d("return from shareloc.php====="+ret);
					if (ret.length()>1 && !ret.startsWith("Error"))
						break;
					MyUtil.Sleep(1500);
				}

				if (ret.startsWith("OK")) {
					
					toastResID=R.string.sharing_done;
					mHandler.post(toastMessage);
					
					long latitude = mPref.readLong("latitude", Global.DEFAULT_LAT);
					long longitude = mPref.readLong("longitude", Global.DEFAULT_LON);
					
					SendAgent sendAgent = new SendAgent(AireJupiter.this, myIdx, 0, false);
					sendAgent.onSend(locrequest, "[<AGREESHARE>]," + mPref.read("myID") + ",1," + latitude + ","
									+ longitude, 0, null, null, false);

					String globalNumber = MyTelephony.attachPrefix(
							AireJupiter.this, locrequest);
					long timeout = MyUtil.getSharingTimeout(1);
					mPref.writeLong(globalNumber,timeout);
					if (mPref.readLong("SpeedupMapMonitor", 0) < timeout*1000)
						mPref.writeLong("SpeedupMapMonitor", timeout*1000);
				} else {
					toastResID=R.string.nonetwork;
					mHandler.post(toastMessage);
				}
			} catch (Exception e) {
				Log.e("aj25 " + e.getMessage());
				toastResID=R.string.nonetwork;
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
					String tmpMsg = "";
					if ((msgGot.attached & 3) == 3) {
						tmpMsg = getString(R.string.voicememo) + ","
								+ getString(R.string.picmsg);
					} else if ((msgGot.attached & 1) == 1) {
						tmpMsg = getString(R.string.voicememo);
					} else if ((msgGot.attached & 2) == 2) {
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

					mHandler.post(HandleMessageComing);
				}
			}
		}, "processIncomingSMS")).start();
	}
	
	public void lanuchServiceYToJoinChatroom(String ip, int from,boolean isBroadcast)
	{
		if (DialerActivity.getDialer()!=null) return;
		
		mPref.write("joinSipAddress", ip);
		mPref.write("incomingChatroom", true);
		
		if (AireVenus.getLc()!=null && MyUtil.CheckServiceExists(getApplicationContext(), "com.pingshow.voip.AireVenus"))
        {
			if ((AireVenus.getCallType()==AireVenus.CALLTYPE_FAFA ||
					AireVenus.getCallType()==AireVenus.CALLTYPE_AIRECALL ||
					AireVenus.getCallType()==AireVenus.CALLTYPE_WEBCALL))
			{
				Log.e("!!! STOPPING AireVenus/ServiceY *** AJ @ voip.joinChatroom :: calltype fafa||airecall||webcall");
				Intent itx=new Intent(AireJupiter.this, AireVenus.class);
	    		stopService(itx);
	    		MyUtil.Sleep(3000);
			}
        }
		
		startServiceY(AireVenus.CALLTYPE_CHATROOM);
				
		calleeNumber=isBroadcast ? "1008" : "1007";
		String idx=""+from;
		mGroupID=from;
		
		for (int i=idx.length();i<7;i++)
			calleeNumber+="0";
	
		calleeNumber+=idx;
		mDisplayname=mADB.getNicknameByIdx(from);
		String address=mADB.getAddressByIdx(from);
		calleeContact_id = cq.getContactIdByNumber(address);
		
		mPref.write("ChatroomHostIdx", from);
		
		mHandler.postDelayed(new Runnable() {
			public void run() {
				Intent intent = new Intent(AireJupiter.this, DialerActivity.class);
				intent.putExtra("Contact_id", calleeContact_id);
				intent.putExtra("PhoneNumber", calleeNumber);
				intent.putExtra("DisplayName", mDisplayname);
				intent.putExtra("ChatroomHostIdx", mGroupID);
				intent.putExtra("VideoCall", false);
				intent.putExtra("Selfinit", true);  //tml*** abort dialer
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
				AireJupiter.this.startActivity(intent);
			}
		}, 1500);
	}
	
	Runnable searchPossibleFriends_delayed=new Runnable(){
		public void run()
		{
			new Thread(searchPossibleFriends).start();
		}
	};
	
	Runnable searchPossibleFriends=new Runnable(){
		public void run()
		{
			Log.i("tmlf searchPossibleFriends ProfileCompleted? " + mPref.readBoolean("ProfileCompleted",false));
			if (!mPref.readBoolean("ProfileCompleted",false))
			{
				mHandler.postDelayed(searchPossibleFriends_delayed, 19000);
				return;
			}
			
			long last = mPref.readLong("last_search_possible_friends", 0);
			long now = new Date().getTime();
			if (now - last < 35800000) { // ~10 hours
				Log.i("tmlf searchPossibleFriends now - last < 35800000, RETURN");
				return;// no need to update
			}
			Log.d("searchPossibleFriends");
			
			mPref.writeLong("last_search_possible_friends", now);
			
			new Thread(new Runnable(){
				public void run()
				{
					List<RelatedUserInfo> PossibleList=null;
					
					int relationship=UserPage.numTrueFriends/6+2;
					try {
						myIdx=Integer.parseInt(mPref.read("myID","0"),16);
						
						MyNet net = new MyNet(AireJupiter.this);
						PossibleList = net.doPostHttpsWithXML("possiblefriends_aire.php", "idx=" + myIdx
								+ "&relationship=" + relationship, null);
					} catch (Exception e) {
						Log.e("aj26 " + e.getMessage());
					}
					
					if (PossibleList!=null) 
					{
						boolean result=false;
						try{
							if (mRDB.isOpen() && mADB.isOpen() && PossibleList.size()>0)
							{
								try{
									mRDB.deleteAll(); //alec
								}catch(Exception e){
									Log.e("aj27 " + e.getMessage());
								}
								
								String nickname;
								int idx;
								int jf;
								for (int i=0;i<PossibleList.size();i++)
								{
									RelatedUserInfo r=PossibleList.get(i);
									idx=r.getIdx();
									jf=r.getjointfriends();
									String address=r.getAddress();
									if (mRDB.isUserBlocked(address)==1 || mADB.isUserBlocked(address)==1) continue;
									if (mADB.isFafauser(address) || mRDB.isFafauser(address)) continue;
									if (idx==myIdx) continue;
									nickname=r.getNickName();
									if (mRDB.insertUser(address, idx, nickname, jf)>0)
										result=true;
								}
							}
						}catch(Exception e){
							Log.e("aj28 " + e.getMessage());
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
	
	public void offlineMessage()
	{
		mHandler.postDelayed(new Runnable(){
			public void run()
			{
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
				
				for (int i=0;i<ParseSmsLine.unknownList.size();i++)
		    	{
		    		SMS m=ParseSmsLine.unknownList.get(i);
					Log.d("addF.stranger offline! " + m.type + " " + m.address);
					Intent intent = new Intent(Global.Action_InternalCMD);
					intent.putExtra("Command",Global.CMD_STRANGER_COMING);
					intent.putExtra("Address", m.address);
					intent.putExtra("Idx", m.type);
					intent.putExtra("Annoying", m.read>2);
					sendBroadcast(intent);
					MyUtil.Sleep(8000);
		    	}
				
				ParseSmsLine.unknownList.clear();
				
			} catch (Exception e) {
				Log.e("aj29 " + e.getMessage());
			}
		}
	};

	public void showNotification(String notificationText, Intent it, boolean bSound, int icon, String title) {
		Notification notification = new Notification(icon, notificationText, System.currentTimeMillis());

		mNM.cancel(R.string.app_name);

		if (it == null || ConversationActivity.sender != null)
			it = new Intent(this, SplashScreen.class);

		it.putExtra("fromNotification", true);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, it,
				PendingIntent.FLAG_UPDATE_CURRENT);

		if (title == null)
			title = getResources().getString(R.string.newmessage);

		notification.setLatestEventInfo(this, title, notificationText, contentIntent);

		final boolean ringb = mPref.readBoolean("notification_sound", true) && bSound;
		final boolean recv_vbr = mPref.readBoolean("recvVibrator", true);
		notification.defaults = Notification.DEFAULT_LIGHTS
				| (recv_vbr ? Notification.DEFAULT_VIBRATE : 0)
				| (ringb ? Notification.DEFAULT_SOUND : 0);

		if (recv_vbr)
			notification.vibrate = new long[] { 100, 250 };
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
			int type  = callLogBundle.getInt("type",1);
			long contact_id = callLogBundle.getLong("contact_id");
			long time = callLogBundle.getLong("time");
			int direction = callLogBundle.getInt("direction",1);
			int duration = callLogBundle.getInt("duration",0);
			int status = callLogBundle.getInt("status",0);
			Log.i("mUpdateCallLog " + address + " " + displayname + " " + type + " "
					+ contact_id + " " + time + " " + direction + " " + status);
			
			if (direction==2)//Outgoing
			{
				if (mSmsDB.isOpen())
				{
					mSmsDB.insertMessage(address, contact_id,
							(new Date()).getTime(), 1, -1, 2, "",
							(status>0?
							("(COt) "+getString(R.string.call_duration)+" "+DateUtils.formatElapsedTime(duration)):"(COt)"), 0, null, null,
							0, 0, 0, 0, displayname, null, 0);
					Intent it = new Intent();
					it.setAction(Global.Action_MsgSent);
					sendBroadcast(it);
				}
			}
			else{

				if (status==0)
				{
					mNM.cancel(R.string.app_name);
					
					Intent it = new Intent(AireJupiter.this, ConversationActivity.class);
					it.putExtra("ActivityType", 1);
					it.putExtra("SendeeNumber", address);
					it.putExtra("SendeeContactId", contact_id);
					it.putExtra("SendeeDisplayname", displayname);
					if(type == 1) // miss sys incall not show notification
						showNotification(displayname, it, true, R.drawable.missed,
							getResources().getString(R.string.fafamissed_call));
					
					int idx=mADB.getIdxByAddress(address);
					if (idx>0)
					{
						Intent it2 = new Intent(Global.Action_InternalCMD);
						it2.putExtra("Command", Global.CMD_TCP_MESSAGE_ARRIVAL);
						it2.putExtra("originalSignal", "210/"+Integer.toHexString(idx)+"/"+Integer.toHexString((int)(time/1000))+"/<Z>"
								+"Missed call");
						sendBroadcast(it2);
					}
				}else{
					if (mSmsDB.isOpen())
					{
						mSmsDB.insertMessage(address, contact_id,
								time, 1, -1, 1, "",
								"(iCc) "+getString(R.string.call_duration)+" "+DateUtils.formatElapsedTime(duration), 0, null, null,
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
			String myIpAddress=mPref.read("myIpAddress","");
			String myGeoLocation=mPref.read("myGeoLocation","");
			
			if (myGeoLocation.length()<10 || myIpAddress.length()<5)
			{
				try {
					int count = 0;
					String domain = getIsoDomain();  //tml*** china ip
					do {
						MyNet net = new MyNet(AireJupiter.this);
						myIpAddress = net.doAnyPostHttp("http://" + domain + "/test/p2.php");
						if (myIpAddress.length()>0) break;
						MyUtil.Sleep(500);
					} while (++count<3);
				}catch(Exception e){
					Log.e("aj30 " + e.getMessage());
				}
				
				if (myIpAddress.length()>0)
				{
					mPref.write("myIpAddress",myIpAddress);
					try {
						int count = 0;
						do {
							MyNet net = new MyNet(AireJupiter.this);
							myGeoLocation = net.doAnyPostHttp("http://geoip.maxmind.com/f?l=I1QhkLmrlQay&i="+myIpAddress);
							if (myGeoLocation.length()>0) break;
							MyUtil.Sleep(500);
						} while (++count<3);
					}catch(Exception e){
						Log.e("aj31 " + e.getMessage());
					}
					
					if (myGeoLocation.length()>10)
						mPref.write("myGeoLocation",myGeoLocation);
				}
			}
			
			try {
				String Return="";
				int count = 0;
				String web_url=mPref.read("referenceURL","");
				String encAddress=URLEncoder.encode(callLogBundle.getString("address"), "UTF-8");
				String domain = getIsoDomain();  //tml*** china ip
				do {
					MyNet net = new MyNet(AireJupiter.this);
					Return = net.doAnyPostHttps("https://" + domain + "/test/call_log.php",
							"action=calllog"+
							"&username="+encAddress+
							"&caller="+URLEncoder.encode(myPhoneNumber, "UTF-8")+
							"&starttime="+(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")).format(new Date())+
							"&duration="+callLogBundle.getInt("duration",0)+
							"&phonenumber="+encAddress+
							"&ipaddr="+myIpAddress+
							"&geolocation="+myGeoLocation+
							"&weburl="+web_url);
					if (Return.length()>0) break;
					MyUtil.Sleep(500);
				} while (++count<3);
			}catch(Exception e){
				Log.e("aj32 " + e.getMessage());
			}
		}
	};
	private ArrayList<String> shared_friends;
	
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
			
			Log.d("shared_friends===="+shared_friends);
			if (shared_friends==null) {
				shared_friends.add(msgGot.address);
				Log.d("pop::shared_friends===="+shared_friends);
			}else if (!shared_friends.contains(msgGot.address)) {
				shared_friends.add(msgGot.address);
				Log.d("pop2::shared_friends===="+shared_friends);
			}
			mPref.writeArray("shared_friends", shared_friends);
			long timeout = MyUtil.getSharingTimeout(1);
			mPref.writeLong(msgGot.address,timeout);
			Intent intent = new Intent(Global.Action_Refresh_LOCATIONTIMER);
			sendBroadcast(intent);
		} else {
			int interphoneType = -1;
			String TempContent = msgGot.content;
			if (TempContent.contains("(Vm)")) {
				TempContent = TempContent.replace("(Vm)", getString(R.string.voicememo));
			}
			if (TempContent.startsWith("(iMG)")) {
				TempContent = TempContent.replace("(iMG)", getString(R.string.picmsg));
			}
			if (TempContent.startsWith("(g.f")) {
				TempContent = getString(R.string.animated);
			}
			if (msgGot.attached == 4) {
				return;
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
			showNotification(msgGot.displayname + ": " + TempContent, i, true, R.drawable.icon_sms, null);
		}

		// Fetch call state, if the user is in a call or the phone is ringing we
		// don't want to show the popup
		TelephonyManager mTM = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		boolean callStateIdle = mTM.getCallState() == TelephonyManager.CALL_STATE_IDLE;

		if (!callStateIdle)
			return;
		if (DialerActivity.getDialer() != null)
			return;
		if (mPref.readBoolean("popupSms", true) && !mPref.readBoolean("vociemessaging", false)) 
		{
			/*
			Intent popup = new Intent(this, PopupDialog.class);
			popup.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			*/
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
			
			int idx=mADB.getIdxByAddress(msgGot.address);
			b.putInt("EXTRAS_FROM_IDX", idx);
			
			MyNotification m=new MyNotification();
			m.start(b);

			/*
			popup.putExtras(b);

			PopupDialog p = PopupDialog.getInstance();
			if (p != null)
				p.refresh(b);
			else
				startActivity(popup);
			*/
		}
	}
	
	final Runnable HandleMessageComing = new Runnable() {
		public void run() {
			Intent it = new Intent();
			if (ConversationActivity.sender == null
					|| !ConversationActivity.sender.equals(msgGot.address)
					|| (msgGot.attached & 4) == 4)
			{
				onPopupDialog();
			
				if (ConversationActivity.sender==null || !ConversationActivity.sender.equals(msgGot.address))
				{
					mADB.updateLastContactTimeByAddress(msgGot.address, new Date().getTime());
			    	
					UserPage.needRefresh=true;
					Intent intent = new Intent(Global.Action_Refresh_Gallery);
					sendBroadcast(intent);
				}
			}else {
				it.putExtra("autoPath", msgGot.att_path_aud);
				manySmsContent.setLength(0);
			}

			it.setAction(Global.Action_MsgGot);
			it.putExtra("msgAttach", msgGot.attached);
			it.putExtra("msgContent", msgContent);
			sendBroadcast(it);// Update fafa Lists
		}
	};
	
	public boolean downloadPhoto(int uid, String localfile)
	{
		return downloadPhoto(uid,localfile,3);
	}
	
	public boolean downloadPhoto216(int uid, String localfile)
	{
		return downloadPhoto(uid,localfile,216);
	}
	
	public boolean downloadPhoto(int uid, String localfile, int retry) {
		try {
			String remotefile = "profiles/photo_" + uid + ".jpg";
			int success = 0;
			int count = 0;
			
			if (retry!=216)
			{
				do {
					MyNet net = new MyNet(AireJupiter.this);
					success = net.Download(remotefile, localfile, myLocalPhpServer);
					if (success==1||success==0)
						break;
					MyUtil.Sleep(500);
				} while (++count < retry);
			}
			
			if (success!=1)
			{
				count = 0;
				do {
					MyNet net = new MyNet(AireJupiter.this);
					success = net.Download(remotefile, localfile, null);
					if (success==1||success==0)
						break;
					MyUtil.Sleep(500);
				} while (++count < retry);
			}

			Log.d("downloadPhoto" + success + " " + uid + " " + localfile + " " + retry);
			if (success==1) {
				try {
					mADB.updatePhotoByUID(uid, 1);
				} catch (Exception e) {
					Log.e("aj33 " + e.getMessage());
				}
				return true;
			}
		} catch (Exception e) {
			Log.e("photo Download failed.");
		}
		return false;
	}
	
	public boolean downloadAnyPhoto(String remotefile, String localfile, int retry, boolean dontTryLocalPhpServer) {
		try {
			int success = 0;
			int count = 0;
			if (!dontTryLocalPhpServer)
			{
				do {
					MyNet net = new MyNet(AireJupiter.this);
					success = net.Download(remotefile, localfile, myLocalPhpServer);
					if (success==1||success==0)
						break;
					MyUtil.Sleep(500);
				} while (++count < retry);
			}
			
			if (success!=1)
			{
				count=0;
				do {
					MyNet net = new MyNet(AireJupiter.this);
					success = net.Download(remotefile, localfile, null);
					if (success==1||success==0)
						break;
					MyUtil.Sleep(500);
				} while (++count < retry);
			}
			if (success==1) {
				return true;
			}
		} catch (Exception e) {
			Log.e("anyphoto Download failed.");
		}
		return false;
	}
	
	/**
	 * zhao when net connect,send pending sms again
	 */
	//tml upload5
	public void sendPendingSMS() {
		Cursor cursor = mSmsDB.fetchMessageByStatus(2);
		if (cursor==null) return;//alec
		try {
			myIdx=Integer.parseInt(mPref.read("myID","0"),16);
			if (cursor.getCount()>0)
			{
				Log.d("sendPendingSMS....Start Count:" + cursor.getCount());
				while (cursor.moveToNext()) {
					int fail_count = cursor.getInt(cursor.getColumnIndexOrThrow(SmsDB.KEY_FAIL_COUNT));
					if (fail_count >= 5) {
						continue;
					}
					String SendeeNumber = cursor.getString(cursor.getColumnIndexOrThrow(SmsDB.KEY_ADDRESS));
					String mMsgText = cursor.getString(cursor.getColumnIndexOrThrow(SmsDB.KEY_BODY));
					if (mMsgText.contains("[<AGREESHARE>]")) continue;//alec
					int mAttached = cursor.getInt(cursor.getColumnIndex(SmsDB.KEY_ATTACH_TYPE));
					String SrcAudioPath = cursor.getString(cursor.getColumnIndexOrThrow(SmsDB.KEY_VMEMO));
					String SrcImagePath = cursor.getString(cursor.getColumnIndexOrThrow(SmsDB.KEY_IMAGE));
					int idx=mADB.getIdxByAddress(SendeeNumber);
					SendAgent agent = new SendAgent(this, myIdx, idx, false);
					mMsgText = Global.Temp_Parse + mMsgText;  //tml*** 200timeout
					agent.onSend(SendeeNumber, mMsgText, mAttached, SrcAudioPath, SrcImagePath, false);
					agent.setRowId(cursor.getInt(cursor.getColumnIndex(SmsDB.KEY_SMS_ID)));
					mSmsDB.updateFailCountById(cursor.getInt(0), ++fail_count);
					
					if (SrcImagePath!=null || SrcAudioPath!=null)
						Thread.sleep(2000);
					else
						Thread.sleep(500);
				}
			} else {
				Log.e("sendPendingSMS no action");
			}
		} catch (Exception e) {
			Log.e("aj34 " + e.getMessage());
		} 
		finally {
			if(cursor!=null && !cursor.isClosed())
				cursor.close();
		}
		Log.d("sendPendingSMS....Over");
	};
	
	public void saveContactDrawableToFile(int idX, long contactId)
	{
		if (contactId>0 && MyUtil.checkSDCard(getApplicationContext()))
		{
			Drawable photo=cq.getPhotoById(this, contactId, false);
			if (photo!=null)
			{
				Bitmap bitmap = ((BitmapDrawable)photo).getBitmap();
				String DstThumbPath=Global.SdcardPath_inbox+"photo_"+idX+".jpg";
				ResizeImage.ResizeXY(this, bitmap, DstThumbPath, 80, 75);
				//TODO
				String DstImagePath=Global.SdcardPath_inbox+"photo_"+idX+"b.jpg";
				ResizeImage.ResizeXY(this, bitmap, DstImagePath, 240, 100);
			}
		}
	}
	
	public void doCheckPhotoFromNet() {
		if (!MyUtil.checkSDCard(getApplicationContext())) return;
		Log.d("doCheckPhotoFromNet");
		
		Cursor c = mADB.fetchPhotoVersion();
		if (c==null) return;
		StringBuffer idxBuffer = new StringBuffer("");
		if (c.moveToFirst()) {
			do {
				int idx = c.getInt(1); 
				idxBuffer.append(Integer.toHexString(idx)+"+");
			} while (c.moveToNext());
		}
		
		if(c!=null && !c.isClosed()) 
			c.close();
			
		if (idxBuffer.toString().length()!=0)
		{
			try {
				MyNet net = new MyNet(AireJupiter.this);
				String Return = net.doPostHttps("queryphotoAll.php","idx=" + idxBuffer.substring(0, idxBuffer.toString().length()-1),null);
				if (Return.length() > 5){
					Return = Return.substring(5);
					if (Return.length() > 0) {
						String[] versions = Return.split("\\+");
						String[] idxs = idxBuffer.substring(0, idxBuffer.toString().length()-1).split("\\+");
						if(versions.length>0){
							boolean result = false;
							for(int i = 0;i<versions.length;i++){
								int idx10 = Integer.valueOf(idxs[i], 16);
								int fafaVersion = mADB.getPhotoVersionByIdx(idx10);
								int netVersion = 0;
								boolean ret;
								try {
									netVersion = Integer.valueOf(versions[i],16);
									if (fafaVersion != netVersion && netVersion != 0){
										String localfile = Global.SdcardPath_inbox + "photo_" + idx10 + "b.jpg";
										ret = downloadPhoto(idx10,localfile);
										result |= ret;
										MyUtil.Sleep(500);
									}
								} catch (Exception e) {
									Log.e("aj35 " + e.getMessage());
								}
							}
							
							if (result) {
								Intent intent = new Intent(Global.Action_Refresh_Gallery);
								sendBroadcast(intent);
							}
						}
					}
				}
			} 
			catch (Exception e) {
				Log.e("aj36 " + e.getMessage());
			}
		}
	}
	
	
	private void downloadBigPhotoFromNet() {
		if (!MyUtil.checkSDCard(getApplicationContext())) return;
		if (!new NetInfo(AireJupiter.this).isConnected()) return;
		
		int time = new Date().getHours();
		if (time>=2 && time<8) // early in the morning
		{
			Log.d("downloadBigPhotoFromNet at 0"+time+":00");
			Cursor c = mADB.fetchAll();
			if (c!=null && c.moveToFirst()) {
				do{
					int idx = c.getInt(3);
					String address=c.getString(1);
					if (!address.startsWith("[<GROUP>]"))
					{
						String localPhoto=Global.SdcardPath_inbox + "photo_" + idx + "b.jpg";
						if (!new File(localPhoto).exists())
						{
							int count=0;
							int success=0;
							String remotefile = "profiles/photo_" + idx + ".jpg";
							do {
								try{
									MyNet net = new MyNet(AireJupiter.this);
									success = net.Download(remotefile, localPhoto, myLocalPhpServer);
								}catch(Exception e){
									Log.e("aj37 " + e.getMessage());
								}
								if (success==1||success==0)
									break;
								MyUtil.Sleep(500);
							} while (++count < 2);
							
							if (success!=1)
							{
								count=0;
								do {
									try{
										MyNet net = new MyNet(AireJupiter.this);
										success = net.Download(remotefile, localPhoto, null);
									}catch(Exception e){
										Log.e("aj38 " + e.getMessage());
									}
									if (success==1||success==0)
										break;
									MyUtil.Sleep(500);
								} while (++count < 2);
							}
						}
						MyUtil.Sleep(500);
					}
				}while(c.moveToNext());
				c.close();
			}
		}
	}
	
	Runnable downloadFriendList=new Runnable(){
		public void run()
		{
			Log.d("downloadFriendList(buddylist)");
			
			new Thread(new Runnable(){
				public void run()
				{
					List<RelatedUserInfo> PossibleList=null;
					try {
						myIdx=Integer.parseInt(mPref.read("myID","0"),16);
						int count = 0;
						do {
							MyNet net = new MyNet(AireJupiter.this);
							PossibleList = net.doPostHttpsWithXML("getbuddylist_aire.php", "idx=" + myIdx, null);
							if (PossibleList!=null) break;
							MyUtil.Sleep(1500);
						} while (count++ < 3);
					} catch (Exception e) {
						Log.e("aj39 " + e.getMessage());
					}
					
					if (PossibleList!=null) 
					{
						boolean result=false;
						if (mRDB.isOpen() && mADB.isOpen() && PossibleList.size()>0)
						{
							String unknown=getString(R.string.unknown_person);
							String nickname;
							int idx;
							for (int i=0;i<PossibleList.size();i++)
							{
								RelatedUserInfo r=PossibleList.get(i);
								idx=r.getIdx();
								if (idx==myIdx) continue;
								String address=r.getAddress();
								if (address.length()<6) continue;
								if (mRDB.isUserBlocked(address)==1||mADB.isUserBlocked(address)==1) continue;
								if (mADB.isFafauser(address) || mRDB.isFafauser(address)) continue;
								nickname=r.getNickName();
								if (nickname==null) continue;
								if (nickname.length()<2 || nickname.equals(unknown) || nickname.equals("null")) continue;
								
								if (mADB.insertUser(address, idx, nickname)>0)
								{
									ContactsOnline.setContactOnlineStatus(address,0);
									long contactId=cq.getContactIdByNumber(address);
									if (contactId>0)
										saveContactDrawableToFile(idx, contactId);
									result=true;
								}
							}
						}
						
						if (result) {
							Intent intent = new Intent(Global.Action_Refresh_Gallery);
							sendBroadcast(intent);
							
							getFriendNicknames();
							
							doCheckPhotoFromNet();
							
							getFriendMoods();
						}
					}
				}
			}).start();
		}
	};
	
	public void getFriendNicknames() {
		Log.d("doGetFriendNicknames");
		Cursor c = mADB.fetchAll();
		if (c==null) return;
		StringBuffer idxBuffer = new StringBuffer("");
		
		if (c.moveToFirst()) {
			do {
				int idx = c.getInt(3);
				String address = c.getString(1);
				if (idx==myIdx) continue;
				if (address.startsWith("[<GROUP>]")) continue;
				idxBuffer.append(Integer.toHexString(idx)+"+");
			} while (c.moveToNext());
		}
		
		if(c!=null && !c.isClosed()) 
			c.close();
		
		c = mRDB.fetchAll();
		if (c!=null)
		{
			if (c.moveToFirst()) {
				do {
					int idx = c.getInt(3); 
					idxBuffer.append(Integer.toHexString(idx)+"+");
				} while (c.moveToNext());
			}
			
			if(c!=null && !c.isClosed()) 
				c.close();
			}
			
		if (idxBuffer.toString().length()>0)
		{
			try {
				int count=0;
				do{
					MyNet net = new MyNet(AireJupiter.this);
					
					Log.d(idxBuffer.substring(0, idxBuffer.toString().length()-1));
					
					String Return = net.doPostHttps("getusernickname.php","idx=" + idxBuffer.substring(0, idxBuffer.toString().length()-1),null);
					if (Return.length() > 5 && Return.startsWith("Done")){
						Return = Return.substring(5);
						if (Return.length() > 0) {
							String[] idxs = idxBuffer.substring(0, idxBuffer.toString().length()-1).split("\\+");
							Pattern p = Pattern.compile("<Z->");
							String[] nicknames = p.split(Return, idxs.length+1);
							if(nicknames.length>0){
								for(int i = 0;i<nicknames.length;i++){
									try {
										int idx10 = Integer.valueOf(idxs[i], 16);
										mADB.updateNicknameByUID(idx10, nicknames[i]);
										mRDB.updateNicknameByUID(idx10, nicknames[i]);
									} catch (Exception e) {
										Log.e("aj40 " + e.getMessage());
									}
								}
							}
						}
						Intent intent = new Intent(Global.Action_Refresh_Gallery);
						sendBroadcast(intent);
						break;
					}
					MyUtil.Sleep(1500);
				}while(count++<3);
			} 
			catch (Exception e) {
				Log.e("aj41 " + e.getMessage());
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
		if (c==null) return;
		StringBuffer idxBuffer = new StringBuffer("");
		
		if (c.moveToFirst()) {
			do {
				int idx = c.getInt(3);
				String address = c.getString(1);
				if (address.startsWith("[<GROUP>]")) continue;
				idxBuffer.append(Integer.toHexString(idx)+"+");
			} while (c.moveToNext());
		}
		
		if(c!=null && !c.isClosed()) 
			c.close();
			
		if (idxBuffer.toString().length()>0)
		{
			try {
				int count=0;
				myIdx=Integer.parseInt(mPref.read("myID","0"),16);
				do{
					MyNet net = new MyNet(AireJupiter.this);
					String Return = net.doPostHttps("getusermood.php","idx=" + idxBuffer.substring(0, idxBuffer.toString().length()-1),null);
					if (Return.length() > 5 && Return.startsWith("Done")){
						Return = Return.substring(5);
						if (Return.length() > 0) {
							String[] idxs = idxBuffer.substring(0, idxBuffer.toString().length()-1).split("\\+");
							Pattern p = Pattern.compile("<Z->");
							String[] moodText = p.split(Return, idxs.length+1);
							if(moodText.length>0){
								for(int i = 0;i<moodText.length;i++){
									if (moodText[i]!=null && moodText[i].length()>0){
										int idx10 = Integer.valueOf(idxs[i], 16);
										if (idx10==myIdx)
											mPref.write("moodcontent",moodText[i]);
										else
											mADB.updateMoodByUID(idx10, moodText[i]);
									}
								}
							}
						}
						break;
					}
					MyUtil.Sleep(1500);
				}while(count++<3);
			} 
			catch (Exception e) {
				Log.e("aj42 " + e.getMessage());
			}
		}
		
		mPref.writeLong("last_time_checking_mood", now);
	}
	
	public void downloadRelatedUsersPhotos() {
		if (!MyUtil.checkSDCard(getApplicationContext())) return;
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
				idxBuffer.append(Integer.toHexString(idx)+"+");
			} while (c.moveToNext());
		}
		
		if(c!=null && !c.isClosed()) 
			c.close();
			
		if (idxBuffer.toString().length()!=0)
		{
			try {
				MyNet net = new MyNet(AireJupiter.this);
				String Return = net.doPostHttps("queryphotoAll.php","idx=" + idxBuffer.substring(0, idxBuffer.toString().length()-1),null);
				if (Return.length() > 5){
					Return = Return.substring(5);
					if (Return.length() > 0) {
						String[] versions = Return.split("\\+");
						String[] idxs = idxBuffer.substring(0, idxBuffer.toString().length()-1).split("\\+");
						if(versions.length>0){
							boolean result = false;
							for(int i = 0;i<versions.length;i++){
								int idx10 = Integer.valueOf(idxs[i], 16);
								int fafaVersion = mRDB.getPhotoVersionByIdx(idx10);
								int netVersion = 0;
								try {
									netVersion = Integer.valueOf(versions[i],16);
									if (fafaVersion != netVersion && netVersion != 0){
										String localfile = Global.SdcardPath_inbox + "photo_" + idx10 + "b.jpg";
										result |= downloadPhoto(idx10,localfile);
										MyUtil.Sleep(500);
									}
								} catch (Exception e) {
									Log.e("aj43 " + e.getMessage());
								}
							}
							
							if (result) {
								Intent intent = new Intent(Global.Action_Refresh_Gallery);
								sendBroadcast(intent);
							}
						}
					}
				}
			} 
			catch (Exception e) {
				Log.e("aj44 " + e.getMessage());
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
	
	public static final String[] fafaHelpers = {"support"};
	public static final int[] helperIdx = {2};
//	public static final String[] fafaHelpers = {};
//	public static final int[] helperIdx = {};
	private static String helper_photo_path[] = {
		"/data/data/com.pingshow.airecenter/files/help_1.jpg"};
	private void addAmperHelpers() {
		if (!mADB.isFafauser(fafaHelpers[0])) {
			try {
				copyAssetsFromPackage();
			} catch (IOException e) {
				Log.e("aj45 " + e.getMessage());
			}
			//tml*** support, TODO
//			if (mPref.read("iso", "us").equals("cn")) {
//				mADB.insertUser("support", 2, getString(R.string.helper_name1));
//				mADB.updatePhotoByUID(2, 1);
//			} else {
//				mADB.insertUser("support", 2, getString(R.string.helper_name1));
//				mADB.updatePhotoByUID(2, 1);
//			}
			for (int i = 0; i < fafaHelpers.length; i++) {
				mADB.insertUser(fafaHelpers[i], helperIdx[i], getString(R.string.helper_name1 + i));
				mADB.updatePhotoByUID(helperIdx[i], 1);
			}
		}
	}
	
	Runnable popupWelcomeDialog=new Runnable(){
		public void run(){
			if (!mPref.readBoolean("ProfileCompleted",false))
			{
				mHandler.postDelayed(popupWelcomeDialog, 25000);
				return;
			}
			Intent it = new Intent(Global.Action_InternalCMD);
			long now=new Date().getTime()/1000;
			it.putExtra("Command", Global.CMD_TCP_MESSAGE_ARRIVAL);
			it.putExtra("originalSignal", "210/2/"+Integer.toHexString((int)now)+"/<Z>"+getString(R.string.aire_welcome_desc));
			sendBroadcast(it);
		}
	};
	
	void tellFriendsProfileChanged(int mode, String newMood) {
		
		notifying=true;
		
		Cursor c = mADB.fetchAll();
		if (c.moveToFirst()) {
			myIdx=Integer.parseInt(mPref.read("myID","0"),16);
			String text;
			if (mode == 0)// photo
				text = "[<NEWPHOTO>]";
			else
				text = "[<NEWMOOD>]" + newMood;
			do {
				if (c.getInt(3)>50) {
					String address=c.getString(1);
					if (ContactsOnline.getContactOnlineStatus(address)>0 && !address.startsWith("[<GROUP>]"))
					{
						try {
							Log.i("tml tellFriendsProfileChanged! > " + address);
							SendAgent agent = new SendAgent(AireJupiter.this, myIdx, 0, false);
							agent.onSend(address, text, 0, null, null, true);
						} catch (Exception e) {
							Log.e("aj46 " + e.getMessage());
						}
						MyUtil.Sleep(1000);
					}
				}
			} while (c.moveToNext());
		}
		if(c!=null && !c.isClosed())
			c.close();
		
		notifying=false;
	}

	
	String mDownload_HyperLink = "http://71.19.247.49/downloads/aire-stb.apk";

	Runnable mDownloadnUpdate = new Runnable() {
		public void run() {
			if (mDownload_HyperLink.length() > 0) {
				try {
					MyNet net = new MyNet(getApplicationContext());
					String localfile = Global.SdcardPath_downloads + "AIRE.apk";
					Log.d("*** Download AIRE APK! BEGIN ***");
					if (net.anyDownload(mDownload_HyperLink, localfile)) {
						Log.d("*** Download AIRE APK! DONE ***");
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.parse("file://" + localfile),
								"application/vnd.android.package-archive");
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
					}
				} catch (Exception e) {
					Log.e("Download APK failed.");
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
	
	public void forceCheckUpdate () {
		Thread thr_connection = new Thread(checkVersion_go, "forceCheckUpdate");
		thr_connection.start();
	}
	//***tml
	
//	private void checkVersionUpdate() {
	public void checkVersionUpdate(boolean checknow) {
//		long last = mPref.readLong("last_checking_version", 0);
//		long now = new Date().getTime();
//		if (now - last < 18000000) // 5hrs
//			return;// no need to check
//		mPref.writeLong("last_checking_version", now);
		if (!checknow) {  //tml*** beta ui
			long last = mPref.readLong("last_checking_version", 0);
			long now = new Date().getTime();
			if (now - last < 18000000) // 5hrs
				return;// no need to check
			mPref.writeLong("last_checking_version", now);
		}
		
		Log.d("check Version Update...");
		MyNet net = new MyNet(AireJupiter.this);
		String Return = net.doPostHttps("checkaireversion_stb.php", "detail=0",null);
		if (Return.length() > 0) {
			int latest_versionCode = 1;
			Pattern p = Pattern.compile(";");
			String[] items = p.split(Return, 10);
			if (items[0].startsWith("latest_version_code"))
				latest_versionCode = MyUtil.getIntValue(items[0], 0);

			if (latest_versionCode > versionCode)
			{
				Log.d("New version found! "+latest_versionCode);
				Return = net.doPostHttps("checkaireversion_stb.php", "detail=1",null);
				p = Pattern.compile(";");
				items = p.split(Return, 10);

				int forceUpdate = MyUtil.getIntValue(items[1], 0);
				String version_Name = MyUtil.getStringValue(items[3]);

				String title = String.format(getString(R.string.new_version_found), version_Name);
				
				if (forceUpdate > 0) title += getString(R.string.force_update);
				
				title += (" "+getString(R.string.dont_worry));
				mDownload_HyperLink = MyUtil.getStringValue(items[4]);
				Intent it = new Intent(AireJupiter.this, CommonDialog.class);
				it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				it.putExtra("msgContent", title);
				it.putExtra("numItems", 2);
				it.putExtra("ItemCaption0", getString(R.string.no));
				it.putExtra("ItemResult0", 0);
				it.putExtra("ItemCaption1", getString(R.string.download));
				it.putExtra("ItemResult1", CommonDialog.DOWNLOAD);
				
				startActivity(it);
			} else {  //tml*** beta ui
				if (checkShow) {
					String title = (getString(R.string.no) + " " + getString(R.string.update));
					Intent it = new Intent(AireJupiter.this, CommonDialog.class);
					it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					it.putExtra("msgContent", title);
					it.putExtra("numItems", 0);
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
	
	
	Runnable clearSDCard=new Runnable(){
		public void run(){
			try{
				String currentPath = Global.SdcardPath_inbox;
				File[] files = new File(currentPath).listFiles();
				for (File file : files)
				{
					String fn=file.getName();
					if (fn.startsWith("photo_"))
					{
						try{
							int end=fn.indexOf("b.jpg");
							if (end!=-1)
							{
								String a=fn.substring(6,end);
								int idx=Integer.parseInt(a);
								if (!mADB.isFafauser(idx) && !mRDB.isFafauser(idx))
									file.delete();
							}else{
								end=fn.indexOf(".jpg");
								if (end!=-1)
								{
									String a=fn.substring(6,end);
									int idx=Integer.parseInt(a);
									if (!mADB.isFafauser(idx) && !mRDB.isFafauser(idx))
										file.delete();
								}
							}
						}catch(Exception e){
							Log.e("aj47 " + e.getMessage());
						}
					}
				}
			}catch(Exception e){
				Log.e("aj48 " + e.getMessage());
			}
		}
	};
	
	private Runnable searchFriendsByPhonebook_delayed=new Runnable()
	{
		public void run()
		{
			new Thread(searchFriendsByPhonebook).start();
		}
	};
	
	private Runnable searchFriendsByPhonebook=new Runnable()
	{
		public void run()
		{
			if (!new NetInfo(AireJupiter.this).isConnected()) return;
			
			if (!mPref.readBoolean("ProfileCompleted",false))
			{
				mHandler.postDelayed(searchFriendsByPhonebook_delayed, 11000);
				return;
			}
			
			new Thread(new Runnable(){
				public void run()
				{
					long last = mPref.readLong("last_query_sid_time", 0);
					
					long now = new Date().getTime();
					if (now - last < 3600000) // 1 hours
						return;// no need to check
					
					if (DialerActivity.getDialer()!=null)
					{
						mHandler.postDelayed(searchFriendsByPhonebook_delayed, 30000);
						return;
					}
					
					Log.d("Query By Phonebook");
					
					mPref.writeLong("last_query_sid_time", now);
					
					String addr="";
					
					try{
						Cursor cursor = getContentResolver().query(
								CommonDataKinds.Phone.CONTENT_URI,
								new String[] { CommonDataKinds.Phone.CONTACT_ID,
										CommonDataKinds.Phone.NUMBER },null,
								null, CommonDataKinds.Phone.LAST_TIME_CONTACTED + " desc");
			
						if (cursor.moveToFirst()) {
							String phonenumber;
							String lastn="";
							int i=0;
							do {
								phonenumber = MyTelephony.cleanPhoneNumber(cursor.getString(1));
								if (phonenumber!=null && phonenumber.length() >= 7) {
									phonenumber = MyTelephony.attachPrefix(AireJupiter.this, phonenumber);
									if (!phonenumber.equals(myPhoneNumber) && !mADB.isFafauser(phonenumber))
									{
										if (phonenumber.startsWith("+"))//alec
										{
											if (!phonenumber.equals(lastn))
											{
												if (i!=0) addr+=",";
												addr+=phonenumber;
												i++;
												lastn=phonenumber;
												if (i>360) break;
											}
										}
									}
								}
							} while (cursor.moveToNext());
						}
						if(cursor!=null && !cursor.isClosed())
							cursor.close();
					
					}catch(Exception e){
						Log.e("aj49 " + e.getMessage());
					}
					
					List<RelatedUserInfo> PossibleList=null;
					
					try {
						int count = 0;
						do {
							MyNet net = new MyNet(AireJupiter.this);
							PossibleList = net.doPostHttpsWithXML("queryusers_aire.php", "addr=" + URLEncoder.encode(addr,"UTF-8"), null);
							if (PossibleList!=null) break;
							MyUtil.Sleep(1500);
						} while (count++ < 3);
					} catch (Exception e) {
						Log.e("aj50 " + e.getMessage());
					}
					
					if (PossibleList!=null) 
					{
						boolean result=false;
						try{
							String myIdHex=mPref.read("myID","0");
							myIdx=Integer.parseInt(myIdHex,16);
							
							if (mRDB.isOpen() && mADB.isOpen() && PossibleList.size()>0)
							{
								String myNickname = mPref.read("myNickname");
								String nickname;
								int idx;
								for (int i=0;i<PossibleList.size();i++)
								{
									RelatedUserInfo r=PossibleList.get(i);
									idx=r.getIdx();
									String address=r.getAddress();
									if (mRDB.isUserBlocked(address)==1 || mADB.isUserBlocked(address)==1) continue;
									if (mADB.isUserDeleted(address)) continue;
									if (mADB.isFafauser(address)) continue;
									if (idx==myIdx) continue;
									nickname=r.getNickName();
									if (mADB.insertUser(address, idx, nickname)>0)
									{
										mRDB.deleteContactByAddress(address);
										ContactsOnline.setContactOnlineStatus(address,1);
										if (tcpSocket.isLogged(false)){
											tcpSocket.send(address,"[<NEWUSERJOINS>]" + myIdHex + "<Z>" + myPhoneNumber+ "<Z>" + myNickname, 0, null, null, 0, null);
											MyUtil.Sleep(500);
										}
										result=true;
									}
								}
							}
						}catch(Exception e){
							Log.e("aj51 " + e.getMessage());
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
	
	public String getFriendLocation(String address)
	{
		if (!mADB.isOpen()) return null;
		int idx = mADB.getIdxByAddress(address);
		if (idx<0) return "nonmember";
		
		String Return="";
		try{
			myIdx=Integer.parseInt(mPref.read("myID","0"),16);
			int c=0;
			do{
				MyNet net = new MyNet(AireJupiter.this);
				Return = net.doPostHttps("getlocation_aire.php","idx="+ myIdx +"&who="+idx, null);
				if (Return.length()>5) break;
				MyUtil.Sleep(500);
			}while(c++<2);
		}catch(Exception e){
			Log.e("aj52 " + e.getMessage());
		}
		
		if (Return.startsWith("Done"))
		{
			return Return.substring(5);
		}
		
		return "";
	}
	
	
	Runnable getRWTServerIP=new Runnable()
	{
		public void run()
		{
			new Thread(new Runnable(){
				public void run() 
				{
					Log.d("getRWTServerIP");
					long last = mPref.readLong("last_time_get_rwt_server", 0);
					long now = new Date().getTime();
					if (now - last < 3600000) // 1 hours
						return;// no need to update
					String Return="";
					try {
						int count = 0;
						myIdx=Integer.parseInt(mPref.read("myID","0"),16);
						String iso=mPref.read("iso","tw");
						String lang=Locale.getDefault().getLanguage();
						do {
							MyNet net = new MyNet(AireJupiter.this);
							Return = net.doPostHttps("getrwtservice.php","idx="+myIdx+
									"&iso="+iso+
									"&lang="+lang
									,null);
							if (Return.startsWith("Done"))
								break;
							MyUtil.Sleep(2500);
						} while (++count < 3);
					} catch (Exception e) {
						Log.e("aj53 " + e.getMessage());
					}
					
					if (Return.length()>10)
					{
						Return=Return.substring(5);
						try{
							String [] items=Return.split(":");
							int port=Integer.parseInt(items[1]);
							mPref.write("RWTServerIP", items[0]);
							mPref.write("RWTServerPort", port);
						}catch (Exception e){
							Log.e("aj54 " + e.getMessage());
						}
						
						mPref.writeLong("last_time_get_rwt_server", now);
					}
				}
			}).start();
		}
	};
	
	Runnable getConferenceServiceIP=new Runnable()
	{
		public void run()
		{
			new Thread(new Runnable(){
				public void run() 
				{
					long last = mPref.readLong("last_time_get_conference_server", 0);
					long now = new Date().getTime();
					if (now - last < 43200000) { // 12 hours
						Log.d("getConferenceServiceIP - NOT yet");
						return;// no need to update
					}
					String Return="";
					try {
						int count = 0;
						myIdx=Integer.parseInt(mPref.read("myID","0"),16);
						String iso=mPref.read("iso","tw");
						String lang=Locale.getDefault().getLanguage();
						long latitude = mPref.readLong("latitude", Global.DEFAULT_LAT);
						long longitude = mPref.readLong("longitude", Global.DEFAULT_LON);
						Log.d("getConferenceServiceIP " + "idx="+myIdx+"&iso="+iso+"&lang="+lang+
								"&lat="+latitude+"&lon="+longitude+"&os=and");
						do {
							MyNet net = new MyNet(AireJupiter.this);
							Return = net.doPostHttps("getconferenceservice.php","idx="+myIdx+
									"&iso="+iso+
									"&lang="+lang+
									"&lat="+latitude+
									"&lon="+longitude+
									"&os=and"
									,null);
							if (Return.startsWith("Done="))
								break;
							MyUtil.Sleep(2500);
						} while (++count < 3);
					} catch (Exception e) {
						Log.e("aj55 " + e.getMessage());
					}
					
					if (Return.startsWith("Done=") && Return.length()>10)
					{
						Return=Return.substring(5);
						try{
							mPref.write("conferenceSipServer", Return);
						}catch (Exception e){
							Log.e("aj56 " + e.getMessage());
						}
					}
					
					mPref.writeLong("last_time_get_conference_server", now);
				}
			}).start();
		}
	};
	
	Runnable getFreeswitchServiceIP=new Runnable()
	{
		public void run()
		{
			new Thread(new Runnable(){
				public void run() 
				{
					Log.d("getFreeswitchServiceIP");
					long last = mPref.readLong("last_time_get_freeswitch_server", 0);
					long now = new Date().getTime();
					if (now - last < 43200000) // 12 hours
						return;// no need to update
					String Return="";
					try {
						int count = 0;
						myIdx=Integer.parseInt(mPref.read("myID","0"),16);
						String iso=mPref.read("iso","tw");
						String lang=Locale.getDefault().getLanguage();
						long latitude = mPref.readLong("latitude", Global.DEFAULT_LAT);
						long longitude = mPref.readLong("longitude", Global.DEFAULT_LON);
						do {
							MyNet net = new MyNet(AireJupiter.this);
							Return = net.doPostHttps("getfreeswitchservice.php","idx="+myIdx+
									"&iso="+iso+
									"&lang="+lang+
									"&lat="+latitude+
									"&lon="+longitude+
									"&os=stb"
									,null);
							if (Return.startsWith("Done="))
								break;
							MyUtil.Sleep(2500);
						} while (++count < 3);
					} catch (Exception e) {
						Log.e("aj57 " + e.getMessage());
					}
					
					if (Return.startsWith("Done=") && Return.length()>10)
					{
						Return=Return.substring(5);
						try{
							mPref.write("pstnSipServer", Return);
						}catch (Exception e){
							Log.e("aj58 " + e.getMessage());
						}
					}
					
					mPref.writeLong("last_time_get_freeswitch_server", now);
				}
			}).start();
		}
	};
	
	boolean callingOut=false;
	Runnable updateCreditInSipActivity=new Runnable(){
		public void run()
		{
			float credit=mPref.readFloat("Credit", 0);
			
			if (AireCallPage.CallLogRowId!=-1 && callingOut)
			{
				AireCallLogDB mCLDB=new AireCallLogDB(AireJupiter.this);
				float cost=AireCallPage.previousCredit-credit;
				mCLDB.open();
				mCLDB.update(AireCallPage.CallLogRowId, cost);
				mCLDB.close();
				AireCallPage.CallLogRowId=-1;
				callingOut=true;
			}
			
			AireCallPage.updateCredit(credit);
			ShoppingActivity.updateCredit(credit);
		}
	};
	Runnable getSipCredit=new Runnable(){
		public void run()
		{
			new Thread(new Runnable() {
				public void run() {
					try {
						int count=0;
						String android_id = Secure.getString(getContentResolver(), Secure.ANDROID_ID); 
						String Return="";
						do{
							MyNet net = new MyNet(AireJupiter.this);
							Return = net.doPostHttps("getsipcredit.php", "id=" + URLEncoder.encode(myPhoneNumber,"UTF-8")
									+"&password=" + URLEncoder.encode(myPasswd, "UTF-8")
									+"&imei=" + android_id, null);
							if (Return.length() > 5 && !Return.startsWith("Error"))
								break;
							MyUtil.Sleep(500);
						}while(++count<3);
						if (Return.startsWith("Done="))
						{
							float credit=Float.parseFloat(Return.substring(5));
							mPref.writeFloat("Credit", credit);
							mHandler.post(updateCreditInSipActivity);
						}
					} catch (Exception e) {
						Log.e("aj59 " + e.getMessage());
					}
				}
			}).start();
		}
	};
	
	Runnable getFreeTrialCredit=new Runnable(){
		public void run()
		{
			new Thread(new Runnable() {
				public void run() {
					try {
						int count=0;
						String android_id = Secure.getString(getContentResolver(), Secure.ANDROID_ID); 
						String Return="";
						do{
							MyNet net = new MyNet(AireJupiter.this);
							Return = net.doPostHttps("getfreetrialcredit.php", "id=" + URLEncoder.encode(myPhoneNumber,"UTF-8")
									+"&password=" + URLEncoder.encode(myPasswd, "UTF-8")
									+"&imei=" + android_id, null);
							if (Return.length() > 5 && !Return.startsWith("Error"))
								break;
							MyUtil.Sleep(500);
						}while(++count<3);
						if (Return.startsWith("Done="))
						{
							float credit=Float.parseFloat(Return.substring(5));
							mPref.writeFloat("Credit", credit);
							mHandler.post(updateCreditInSipActivity);
						}
					} catch (Exception e) {
						Log.e("aj60 " + e.getMessage());
					}
				}
			}).start();
		}
	};
	
	public void requestSecuritySubscription()
	{
		new Thread(securitySubscriptionThread).start();
	}
	
	Runnable securitySubscriptionThread=new Runnable()
	{
		public void run()
		{
			try {
				int count=0;
//				String android_id = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
				String Return="";
				String domain = getIsoDomain();  //tml*** china ip
				do {
					MyNet net = new MyNet(AireJupiter.this);
//					Return = net.doPostHttps("check_subscription.php", "id=" + URLEncoder.encode(myPhoneNumber,"UTF-8")
//							+"&password=" + URLEncoder.encode(myPasswd, "UTF-8")
//							+"&imei=" + android_id, null);
					//tml|sw*** subscription php/
					String myname = URLEncoder.encode(mPref.read("myPhoneNumber", "0"), "UTF-8");
					String myidx = URLEncoder.encode(mPref.read("myID", "0"), "UTF-8");
//					if (mPref.readBoolean("TEST_PHP", false)) {
//						Return = net.doAnyPostHttps("https://42.121.94.216/webcall/acphp/check_subscription.php",
//								"username=" + myname
//								+ "&idx=" + myidx);
//					} else {
						Return = net.doAnyPostHttps("https://" + domain + "/webcall/acphp/check_subscription_new.php",
								"username=" + myname
								+ "&idx=" + myidx);
//					}
					if (Return.length() > 5 && !Return.startsWith("Error"))
						break;
					MyUtil.Sleep(500);
				} while (++count < 3);
				//tml|sw*** subscription php
				if (!Return.startsWith("Error")) {
					String status = Return.toLowerCase();
					mPref.write("SecurityDueDate", status);
					
					if (ShoppingActivity.getInstance() != null)
						ShoppingActivity.getInstance().updateDueDate();
				}
			} catch (Exception e) {
				Log.e("aj61 " + e.getMessage());
			}
		}
	};
	//tml*** iot status
	private String suvDevString = "";
	public void uploadSuvStatus(String data) {
		if (data != null && data.length() > 0) {
			suvDevString = data;
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
				String myname = URLEncoder.encode(mPref.read("myPhoneNumber", "0"), "UTF-8");
				String myidx = URLEncoder.encode(mPref.read("myID", "0"), "UTF-8");
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
				if(uploaded){
					Log.d("uploadSuvStatus : " + deviceString);
				}
			} catch (Exception e) {
				Log.e("uploadSuvStatus !@#$ " + e.getMessage());
			}
			
			mPref.write("SUVSTATSUPLOADED", uploaded);
		}
	};
	
	//tml*** unread led
	private volatile boolean needblink = false;
	private volatile boolean blinking = false;
	public void doCheckUnread() {
		if (!needblink && !blinking) {
			checkUnread.run();
		}
	}
	
	public void unreadBlinkOff() {
		Log.d("unreadBlinkOff0");
		needblink = false;
		LedSpeakerUtil.setLedOff();
	}
	
	Runnable checkUnread = new Runnable() {
		public void run() {
			Cursor curADB = mADB.fetchAllByTime();
			if (curADB != null) {
				int adbSize = curADB.getCount();
				int totalUnread = 0;
				
				curADB.moveToFirst();
				for (int loop = 0; loop < adbSize; loop++) {
					String address = curADB.getString(1);
					int unreadCount = mSmsDB.getUnreadCountByAddress(address);
					totalUnread = totalUnread + unreadCount;
					boolean moveNext = curADB.moveToNext();
					if (!moveNext) break;
				}
				
				if (totalUnread > 0) {
					needblink = true;
					blinking = true;
					Thread unreadblink = new Thread() {
						@Override
						public void run() {
							while (needblink) {
								MyUtil.Sleep(3000);
								if (!needblink) break;
								LedSpeakerUtil.setLedOn();
								MyUtil.Sleep(500);
								if (!needblink) break;
								LedSpeakerUtil.setLedOff();
							}
							needblink = false;
							blinking = false;
							LedSpeakerUtil.setLedOff();
							Log.d("unreadBlinkOff1");
						}
					};
					unreadblink.start();
				} else {
					needblink = false;
					blinking = false;
					LedSpeakerUtil.setLedOff();
				}
			}
		}
	};
	//***tml
	//tml*** suv alarm
	private AudioTrack mAudioTrack = null;
	private InputStream inS = null;
	private DataInputStream dinS = null;
	public volatile boolean alarmActive = false;
	
	private void prepareAlarm () {
		if (mPref.readBoolean("AlarmNoise", true)) {
			if (mAudioTrack == null) {
				Log.e("ALARM prepare");
				LedSpeakerUtil.setSpeakerOn();
				int iMinBufSize = AudioTrack.getMinBufferSize(16000, 
						AudioFormat.CHANNEL_CONFIGURATION_MONO, 
						AudioFormat.ENCODING_PCM_16BIT);
				mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
						AudioFormat.CHANNEL_CONFIGURATION_MONO, 
						AudioFormat.ENCODING_PCM_16BIT,
						iMinBufSize, AudioTrack.MODE_STREAM);
				AudioManager mAudioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
				float maxVol0 = AudioTrack.getMaxVolume();
				mAudioTrack.setStereoVolume(maxVol0, maxVol0);
				int maxVol1 = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
				int maxVol2 = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol1, 0);
				mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, maxVol2, 0);

			    new Thread(playAlarm).start();
			}
		}
	}
	
	Runnable playAlarm = new Runnable () {
		@Override
		public void run() {
		    alarmActive = true;
			startAlarm();
		}
	};
	
	private void startAlarm () {
		try {
			int bufferSize = 5120;
		    byte[] audiobuff = new byte[bufferSize];
		    int i = 0;
		    AssetManager am = getAssets();
		    String musicfile;
		    musicfile = "alarm1.pcm";
		    
		    inS = am.open(musicfile);
			dinS = new DataInputStream(inS);
			mAudioTrack.play();
			while(((i = dinS.read(audiobuff, 0, bufferSize)) != -1)) {
				mAudioTrack.write(audiobuff, 0, i);
				if (!alarmActive) {
					Log.e("ALARM mid-break");
					break;
				}
		    }
			
			LedSpeakerUtil.setSpeakerOff();
			if (mAudioTrack != null) {
				mAudioTrack.stop();
				mAudioTrack.flush();
				mAudioTrack.release();
			}
			if (inS != null) inS.close();
			if (dinS != null) dinS.close();
			Log.e("ALARM reach END");
		} catch (Exception e) {
			Log.e("ALARM ERR " + e.getMessage());
			if (mAudioTrack != null) {
				mAudioTrack.stop();
				mAudioTrack.release();
				mAudioTrack = null;
			}
		} finally {
			mAudioTrack = null;
			inS = null;
			dinS = null;
			alarmActive = false;
		}
	}
	
	private void stopAlarm () {
		Log.e("ALARM attempt stop " + alarmActive);
		if (alarmActive) {
			alarmActive = false;
			mHandler.removeCallbacks(playAlarm);
		}
	}
	//***tml
	//tml*** dev control
	public void airecenterAccess() {
		mHandler.post(airecenterAccess);
	}
	
	Runnable airecenterAccess = new Runnable() {
		public void run() {
			String myidx0 = mPref.read("myID", "0");  //hex
			int myidx1 = Integer.parseInt(myidx0, 16);  //dec
			MyNet net = new MyNet(AireJupiter.this);
			String Return = net.doPostHttps("checkaccount.php", "idx=" + myidx1, null);
			Log.e("CHECKACCOUNT (" + myidx0 + ":" + myidx1 + ") Return: " + Return);
			
			if (Return != null) {
				if (Return.equals("Failed")) {
					mPref.write("airecenterNoK", true);
					stopSelf();
				} else {
					mPref.write("airecenterNoK", false);
				}
			}
		}
	};
	//***tml
	//tml|yang*** cec
//	public static int execRootCmdSilent(String param) {
//		if (param != null && !param.equals("")) {
//			try {
//				Process localProcess = Runtime.getRuntime().exec("su");
//				Object localObject = localProcess.getOutputStream();
//				DataOutputStream localDataOutputStream = new DataOutputStream((OutputStream) localObject);
//				String str = String.valueOf(param);
//				localObject = str + "\n";
//				
//				localDataOutputStream.writeBytes((String) localObject);
//				localDataOutputStream.flush();
//				localDataOutputStream.writeBytes("exit\n");
//				localDataOutputStream.flush();
//				localProcess.waitFor();
//				localObject = localProcess.exitValue();
//				Log.e("execRootCmdSilent = " + param);
//				return (Integer) localObject;
//			} catch (Exception e) {
//				Log.e("execRootCmdSilent ERR " + e.getMessage());
//			}
//		}
//		return -1;
//	}

	public static void hdmiCmdExec(String param) {
		Log.d("set hdmi: " + param);
		String cmd = "";
		if (param.equals("switch")) {
			cmd = "0x14";
		} else if (param.equals("on")) {
			cmd = "e 0";
		} else if (param.equals("off")) {
			cmd = "d 0";
		}
		HdmiUtil.setStatus(cmd, "/sys/class/amhdmitx/amhdmitx0/cec");
		HdmiUtil.setStatus("0x00", "/sys/class/amhdmitx/amhdmitx0/cec_config");
		HdmiUtil.setStatus("0x0B", "/sys/class/amhdmitx/amhdmitx0/cec_config");
		HdmiUtil.setStatus(cmd, "/sys/class/amhdmitx/amhdmitx0/cec");
		HdmiUtil.setStatus("0x00", "/sys/class/amhdmitx/amhdmitx0/cec_config");
		HdmiUtil.setStatus("0x0B", "/sys/class/amhdmitx/amhdmitx0/cec_config");
		HdmiUtil.setStatus(cmd, "/sys/class/amhdmitx/amhdmitx0/cec");
		HdmiUtil.setStatus("0x00", "/sys/class/amhdmitx/amhdmitx0/cec_config");
		HdmiUtil.setStatus("0x0B", "/sys/class/amhdmitx/amhdmitx0/cec_config");
	}
	
	public static void hdmiCmdExecToAdptr() {
		Log.d("set hdmi2 adptr led");
		String cmd0 = "VIC:0";
		String cmd1 = "VIC:1";
		HdmiUtil.setStatus(cmd0, "/sys/class/amhdmitx/amhdmitx0/cec_led_state");
		HdmiUtil.setStatus(cmd1, "/sys/class/amhdmitx/amhdmitx0/cec_led_state");
		Log.d("hdmi2 @ " + HdmiUtil.getHdmiCECState2());
	}
	
	public static void hdmiCmdExecSetCEC() {
		Log.d("set hdmi cfg");
		HdmiUtil.setStatus("0x0B", "/sys/class/amhdmitx/amhdmitx0/cec_config");
	}
	//***tml
	//tml test
	public String TESTseeVolumes(String from) {
		AudioManager mAudioManager = ((AudioManager)getSystemService(Context.AUDIO_SERVICE));
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
		String volumestates = "(" + from + "," + modeS + "," + mic + "," + spkr + ")"
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
	//tml*** alert toast
	public void toastWarning(int mode, boolean force, String from, final int length) {
		if (mode == 1) {
			Log.e("*** Alert! AireCenter connection may be poor  " + mode + force + " <" + from);
//			long now = System.currentTimeMillis();
//			long last = mPref.readLong("last_warn_net_time", 0);
//			long elapsed = now - last;
//			if (elapsed < 30000 && !force) {
//				Log.e("*** Alert too soon, no toast");
//				return;
//			}
//			mPref.writeLong("last_warn_net_time", now);
			
			mHandler.post(new Runnable () {
				@Override
				public void run() {
					Toast tst1 = Toast.makeText(AireJupiter.this,
							getString(R.string.poornetwork),
							length);
					tst1.setGravity(Gravity.TOP|Gravity.RIGHT, 50, 50);
					try {
						if (seeTopAct(5).contains("pingshow") && length == Toast.LENGTH_LONG) {
							LinearLayout tstLay1 = (LinearLayout) tst1.getView();
							TextView tstTV1 = (TextView) tstLay1.getChildAt(0);
							tstTV1.setTextSize(30);
							tstTV1.setGravity(Gravity.CENTER);
							tstTV1.setTextColor(Color.YELLOW);
						} else {
							LinearLayout tstLay1 = (LinearLayout) tst1.getView();
							TextView tstTV1 = (TextView) tstLay1.getChildAt(0);
							tstTV1.setGravity(Gravity.CENTER);
							tstTV1.setTextColor(Color.YELLOW);
						}
					} catch (ClassCastException e) {}
					tst1.show();
				}
			});
		} else if (mode == 2) {
		}
	}
	
	public String seeTopAct(int topN) {
		ActivityManager mAm = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		List<ActivityManager.RunningTaskInfo> taskInfo = mAm.getRunningTasks(10);
		String name = "topApp";
		
		if (topN == 0 || topN == 1) {
			name = taskInfo.get(0).topActivity.getClassName().toLowerCase();
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
//				if (name.startsWith("com.pingshow.airecenter")) break;
//			}
			for (int i = (listSize - 1); i > -1; i--) {
				name = taskInfo.get(i).topActivity.getClassName().toLowerCase();
//				Log.e("TASK " + i + " " + name);
				if (name.contains("pingshow")) break;
			}
		}
		Log.d("seeTopAct=" + name);
		return name;
	}
	
	public void resetWifi() {
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (wifi.isWifiEnabled()) {
			Log.e("wifi OFF'ing");
			wifi.setWifiEnabled(false);
		} else {
			Log.e("wifi ON'ing");
			wifi.setWifiEnabled(true);
		}
	}
	public void refreshWifi() {
		try {
			long now = new Date().getTime();
			long last = mPref.readLong("last_wifi_reass_time", 0);
			if (now - last < 3600000) {// 60m
				return;// no need to do it
			}
			mPref.writeLong("last_wifi_reass_time", now);
			WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			wifi.reassociate();
		} catch (Exception e) {}
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
			if (!savedsip.equals(AireVenus.mySipServer_USA)) sip = savedsip;
		} else {
			mPref.write("mySipServer", sip);
		}
		AireVenus.mySipServer_default = sip;
		AireJupiter.mySipServer_default = sip;  //tml*** xcountry sip
		Log.d("isoSip " + iso + " " + savedsip + ">" + sip);
		return sip;
	}
	//***tml
	//tml test
	private String calldStatus = "";
	private int statusLines = 0;
	public void updateCallDebugStatus(boolean reset, String message) {
		if (message == null) message = "";
		if (Log.enDEBUG) {
			if (reset) {
				calldStatus = "";
				statusLines = 0;
				return;
			}

			statusLines++;
			if (statusLines > 15) {
				if (calldStatus.contains("\n"))
					calldStatus = message + calldStatus.substring(0, calldStatus.lastIndexOf("\n"));
			} else {
				calldStatus = message + calldStatus;
			}
		}
	}
	//tml test
	public String getCallDebugStatus() {
		return calldStatus;
	}
	
}
