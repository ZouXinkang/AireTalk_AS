package com.pingshow.voip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.telephony.TelephonyManager;

import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.AmazonKindle;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.SettingActivity;
import com.pingshow.network.NetInfo;
import com.pingshow.util.CPUTool;
import com.pingshow.video.capture.AndroidVideoApi5JniWrapper;
import com.pingshow.video.capture.hwconf.AndroidCameraConfiguration;
import com.pingshow.voip.core.PayloadType;
import com.pingshow.voip.core.Version;
import com.pingshow.voip.core.VideoSize;
import com.pingshow.voip.core.VoipAuthInfo;
import com.pingshow.voip.core.VoipCall;
import com.pingshow.voip.core.VoipCall.State;
import com.pingshow.voip.core.VoipCore;
import com.pingshow.voip.core.VoipCore.RegistrationState;
import com.pingshow.voip.core.VoipCore.STUNC;
import com.pingshow.voip.core.VoipCoreException;
import com.pingshow.voip.core.VoipCoreFactory;
import com.pingshow.voip.core.VoipCoreListener;
import com.pingshow.voip.core.VoipProxyConfig;

public class AireVenus extends Service implements VoipCoreListener {

	static final int MAX_RECEIVED=100;
	 
	static final public String _Password = "1111";
	static final public String _Domain = "1.34.148.152";
	static final public String _StunServer = "74.3.165.159";
	static public String mySipServer_default = "96.44.173.84";
	static final public String mySipServer_USA = "74.3.163.8";  //96.44.173.84 (freesw)
	static final public String mySipServer_China = "112.124.20.150";  //115.29.234.27 (freesw)
	
	private static AireVenus theVoip;
	private VoipCore mVoipCore;
	private VoipProxyConfig mDefaultProxyConfig, myProxy;
	private MyPreference mPref;
	public static boolean runAsSipAccount=false;
	public static boolean runAsFileTransfer=false;
	public static boolean destroying=false;
	
	private Timer mTimer = new Timer("voip scheduler");

//	private MediaPlayer mRingerPlayer;
	private VoipCall.State mPrevCallState;
	private Vibrator mVibrator;
	private AudioManager mAudioManager;

	private Handler mHandler =  new Handler() ;
	
	public final static int CALLTYPE_FAFA=0;
	public final static int CALLTYPE_AIRECALL=1;
	public final static int CALLTYPE_CHATROOM=2;
	public final static int CALLTYPE_WEBCALL=3;
	public final static int CALLTYPE_FILETRANSFER=4;
	
	private boolean incomingChatroom=false;
	
	private static int CallType;//0: FafaCall, 1:AireCall  2.WebCall   3.FileTransfer
	
	static boolean isready() {
		return (theVoip!=null);
	}
	static public AireVenus instance()  {
		return theVoip;
	}
	
	//alec
	static public void setCallType(int type)  {
		Log.i("voip.setCallType = " + getCallTypeName(type) + type);
		CallType = type;
	}
	
	static public int getCallType()  {
		return CallType;
	}
	//tml***
	static public String getCallTypeName (int type) {
		String callTypeName;
		if (type == 0) {
			callTypeName = "FAFA";
		} else if (type == 1) {
			callTypeName = "AIRECALL";
		} else if (type == 2) {
			callTypeName = "CHATROOM";
		} else if (type == 3) {
			callTypeName = "WEBCALL";
		} else if (type == 4) {
			callTypeName = "FILETX";
		} else {
			callTypeName = "UNKNOWN";
		}
		return callTypeName;
	}
	
	public void forceRegister() {
		if (CallType != AireVenus.CALLTYPE_FAFA)
			return;
		try {
			myProxy.enableRegister(true);
			myProxy.startRegister();
		} catch (Exception e) {
			Log.e("forceRegister !@#$ " + e.getMessage());
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate() {
		super.onCreate();
		Log.e("*** !!! AIREVENUS/SERVICE-Y *** START START !!! *** voip " + getCallTypeName(CallType) + CallType);
		
		mPref = new MyPreference(this);
		ringrdy = false;
		mAudioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
		mAudioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_RINGER);
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		
		int hardwareSupportedHD = 0;
		if (CPUTool.getNumCores() >= 2 || CPUTool.getMaxCpuFreq() > 1250000) {
			hardwareSupportedHD = 1;
		}
		
		destroying = false;
		/*
		SoundPool = new SoundPool(4, AudioManager.STREAM_VOICE_CALL, 0);
		SoundPool.load(this, R.raw.ringback1, 1);
		SoundPool.load(this, R.raw.ringback2, 1);
		SoundPool.load(this, R.raw.ringback3, 1);
		SoundPool.load(this, R.raw.ringback4, 1);
		*/
		
		try {
			String stun = mPref.read("StunServer", _StunServer);
			
			try{
				FileOutputStream file = new FileOutputStream(new File("/data/data/com.pingshow.amper/files/upnpc.ini"));
				String out = mPref.readInt("audio_local_port", 0)
						+ "\n" + mPref.readInt("video_local_port", 0)
						+ "\n";
				file.write(out.getBytes());
				file.flush();
				file.close();
			} catch(Exception e) {
				Log.e("AVfile !@#$ " + e.getMessage());
			}
			
			boolean sipcall = (CallType==CALLTYPE_CHATROOM || CallType==CALLTYPE_AIRECALL || CallType==CALLTYPE_WEBCALL);

			int sipTransportType = 0;  //TLS 0, UDP 1
			if (CallType == CALLTYPE_CHATROOM) {
				sipTransportType = 1;
			} else if (mPref.readBoolean("enable_tls", true)) {
				sipTransportType = 0;
			}
			
			if (mPref.read("moodcontent", "--").startsWith("p2p off"))
			{
				Log.e("voip.createVoipCore p2p off");
				mVoipCore = VoipCoreFactory.instance().createVoipCore(this, stun, 
						mPref.readInt("audio_local_port", 0), mPref.readInt("video_local_port", 0),
						0, 0/*monitor*/,hardwareSupportedHD,sipTransportType);
			}
			else if (CallType == CALLTYPE_WEBCALL) {  //sw|vivid*** webcall
				Log.e("voip.createVoipCore CALLTYPE_WEBCALL");
				mVoipCore = VoipCoreFactory.instance().createVoipCore(this, stun, 
						mPref.readInt("audio_local_port", 0), mPref.readInt("video_local_port", 0),
						sipcall?0:1, 0, 0, 1);
			}
			else{
				Log.e("voip.createVoipCore DEFAULT");
				boolean ftmode=(AireVenus.getCallType()==AireVenus.CALLTYPE_FILETRANSFER);
				mVoipCore = VoipCoreFactory.instance().createVoipCore(this, stun, 
						mPref.readInt("audio_local_port", 0), mPref.readInt("video_local_port", 0),
						sipcall?0:(ftmode?2:1), 0/*monitor*/,hardwareSupportedHD,sipTransportType);
			}
			Log.e("*** voip.createVoipCore *** sipcall=" + sipcall + " " + getCallTypeName(CallType) + " Stun=" + stun + " transType" + sipTransportType);
			Log.i("*** voip.audio_local_port" + mPref.readInt("audio_local_port", 0) + " video" + mPref.readInt("video_local_port", 0));
			
			try {
				initFromConf();
			} catch (VoipException e) {
				Log.e("no config ready yet " + e.getMessage());
			}
			TimerTask lTask = new TimerTask() {
				@Override
				public void run() {
					try {
						mVoipCore.iterate();
					} catch (RuntimeException e) {
						Log.w("iterate Exception " + e.getMessage());
					}
				}
			};
			
			mTimer.scheduleAtFixedRate(lTask, 0, 100);
			theVoip = this;
		}
		catch (Exception e) {
			Log.e("!@#$ CANNOT START VOIP !@#$ " + e.getMessage());
		}
		
		mHandler.postDelayed(quitServiceY, 170000);
	}
	
	// SImon : to log in to sip server of the specified domain
	public static boolean sip_login(String domain, String username) {
		Log.d("voip.sip_login " + username + " @ " + domain);
		if (instance()==null) {
			return false;
		}
		return instance().sipProxyChange(username, domain);
	}
	public void authInfoRequested(VoipCore p, String realm, String username) {

	}
	public void byeReceived(VoipCore p, String from) {
		// TODO Auto-generated method stub

	}
	public void displayMessage(VoipCore p, String message) {
		// TODO Auto-generated method stub
	}
	
	public void displayStatus(final VoipCore p, final String message) {
		if (message.startsWith("Contacting"))
			DialerActivity.rejectHangingup=true;
		else
			DialerActivity.rejectHangingup=false;
		Log.i("voip.AV core: " + message);
	}
	//tml*** preAV reg
	public void quitServiceY () {
		mHandler.post(quitServiceY);
	}

	public void cancelQuitServiceY () {
		mHandler.removeCallbacks(quitServiceY);
	}
	
	public boolean inCall = false;
	private Runnable quitServiceY = new Runnable() {
		public void run() {
			if (!inCall && (DialerActivity.getDialer()==null))
			{
				Log.e("voip.AireVenus stopSelf...");
				destroying=true;
				stopSelf();
			} else {
				Log.i("voip.AireVenus dont stopSelf... check again in 3m");
				mHandler.postDelayed(quitServiceY, 170000);  //170000
			}
		}
	};
	
	//alec
	public void displayStatus(final VoipCore p, final int resourseID) {
		if (DialerActivity.getDialer()!=null)  {
			mHandler.post(new Runnable() {
				public void run() {
					if (DialerActivity.getDialer()!=null)
					{
						String msg=getString(resourseID);
						DialerActivity.getDialer().displayStatus(p,msg);	
					}
				}
			});
		}
	}
	
	
	public void globalState(final VoipCore p, final VoipCore.GlobalState state, final String message) {
		Log.i("voip.AV global ["+state+"]");
	}
	
	/*
	private VoipCore registration_lc;
	private VoipProxyConfig registration_cfg;
	private VoipCore.RegistrationState registration_state;
	private String registration_smessage;
	*/
	public boolean registered = false;
	
	public void registrationState(final VoipCore p, final VoipProxyConfig cfg,final VoipCore.RegistrationState state,final String smessage) {
		
		if (state == VoipCore.RegistrationState.RegistrationOk 
				&& p.getDefaultProxyConfig().isRegistered()) {
			registered = true;
		}
		if (state == VoipCore.RegistrationState.RegistrationCleared ) {
			registered = false;
		}
		
		if (state == VoipCore.RegistrationState.RegistrationFailed) {
			//mHandler.postDelayed(quitServiceY, 10000);
			registered = false;
		}
		Log.i("voip.AV regis: new ["+state+"]  reg:" + registered);
	}
	
	private boolean hangUpDelay=false;

	public static String callstate_AV = null;  //tml***/
	public void callState(final VoipCore p,final VoipCall call, final State state, final String message) {
//		Log.i("prev_state = " + mPrevCallState + " new state ["+state+"]  " + message);
		Log.i("voip.AV call: new ["+state+"]  " + message + "  //  prev=" + mPrevCallState);
		callstate_AV = state.toString();  //tml*** return Dialer view
//		boolean switchCall = false;
//		if (AireJupiter.getInstance() != null)
//			switchCall = AireJupiter.getInstance().getSetSwitchCall(); //tml*** switch conf
		
		if (destroying) {
			mPref.write("curCall", "");
			callstate_AV = null;
			return;
		}
		if (state == VoipCall.State.IncomingReceived && !call.equals(mVoipCore.getCurrentCall())) 
		{
			//no multicall support, just decline
			mVoipCore.terminateCall(call);
			Log.e("IncomingReceived: no multicall support, just decline, terminateCall");
//			stopRingBack();
			stopRing();  //tml*** new ring
			return;
		}
		if (state==VoipCall.State.CallReleased)//alec
		{ 
			mPrevCallState=state;
			
//			Log.e("--------- SWITCH CALL --------- CallReleased " + switchCall);
//			if (!switchCall) {  //tml*** switch conf, 0e,1c
				inCall = false;//alec
				mPref.write("curCall", "");
				callstate_AV = null;
				return;
//			}
		}
		if (state==VoipCall.State.OutgoingEarlyMedia)
		{
			if (runAsSipAccount)
			{
//				Log.e("--------- SWITCH CALL --------- OutgoingEarlyMedia ");
				mHandler.postDelayed(new Runnable(){
					public void run(){
//						stopRingBack();
						stopRing();  //tml*** new ring
					}
				}, 1250);  //possible audiohw overlap @ 1250
			}
		}
		if (mPrevCallState == VoipCall.State.Connected)
		{
//			if (AireJupiter.getInstance() != null)
//				AireJupiter.getInstance().setSwitchCall(false, "VoipCall.State.Connected");  //tml*** switch conf
			// SIMON 030211:
			// need to move connect message to the head of the queue
		    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO); // Simon 030811 move it to highest priority
			
		    boolean incoming=false;
		    if (DialerActivity.getDialer()!=null)
		    	incoming=DialerActivity.incomingCall;
		    
		    if (incoming)
		    {
		    	if (DialerActivity.getDialer()!=null) DialerActivity.getDialer().callState(p,call,state,message);

//	 			stopRingBack();
	 			stopRing();  //tml*** new ring
	 			long[] patern = {0,80,1000};
	 			mVibrator.vibrate(patern, -1);
		    }
		    else{
		    	NetInfo ni = new NetInfo(AireVenus.this);
				int netType = ni.netType;
			    mHandler.postDelayed(new Runnable(){
			    	public void run()
			    	{
//			    		stopRingBack();
			    		stopRing();  //tml*** new ring
			 			long[] patern = {0,80,1000};
			 			mVibrator.vibrate(patern, -1);
			 			
			 			if (DialerActivity.getDialer()!=null)  
			 				DialerActivity.getDialer().callState(p,call,state,message);
			    	}
			    }, 3200-netType*1000);
		    }
		    
			inCall = true;//alec
			
		}
		else if (state == VoipCall.State.CallEnd) {
//			stopRingBack();
			stopRing();  //tml*** new ring
			controlBkgndMusic(2);
			mVibrator.cancel();
			
			VoipCall c=mVoipCore.getCurrentCall();
			if (c!=null && call!=null && !call.equals(c)) return;
			
			if (DialerActivity.getDialer()!=null)  
 				DialerActivity.getDialer().callState(p,call,state,message);
			
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
			
			hangUpDelay=true;
			mHandler.postDelayed(new Runnable(){
				public void run()
				{
					hangUpDelay=false;
//					if (VideoCallActivity.launched && VideoCallActivity.getInstance()!=null)
//						VideoCallActivity.getInstance().bye();
					if (VideoCallActivity.getInstance() != null) {  //tml*** pause video finish
						VideoCallActivity.getInstance().bye();
					}
				}
			}, 500);

//			Log.e("--------- SWITCH CALL --------- CallEnd " + switchCall);  //tml*** switch conf, 0e,1c
//			if (!switchCall) {
				inCall = false;//alec
				mPref.write("curCall", "");
				callstate_AV = null;
//			}
		} else {
			if (DialerActivity.getDialer()!=null)  
 				DialerActivity.getDialer().callState(p,call,state,message);
		}
		
		if (state == VoipCall.State.Error)
		{
//			stopRingBack();
			stopRing();  //tml*** new ring
			controlBkgndMusic(2);
//			if (message.contains("Not found")) {  //tml*** xcountry sip
//				if (CallType == CALLTYPE_FAFA || CallType == CALLTYPE_CHATROOM) {
//					mPref.writeLong("last_getCalleeSip", 0);
//					//call aj public to get address for mpref
//				}
//			}
//			if (AireJupiter.getInstance() != null)
//				AireJupiter.getInstance().setSwitchCall(false, "VoipCall.State.Error");  //tml*** switch conf
			mPref.write("curCall", "");
			callstate_AV = null;
		}

		if (state == VoipCall.State.IncomingReceived) 
		{	
			TelephonyManager tMgr =(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			if (hangUpDelay==false 
				&& tMgr.getCallState()==TelephonyManager.CALL_STATE_IDLE )
				//TODO && VoiceMemo_NB.bRecording==false)
			{
				String IncomingNumber=mVoipCore.getRemoteAddress().getUserName();
				if (IncomingNumber==null || IncomingNumber.length()<6)
				{
					mVoipCore.terminateCall(call);
					Log.e("voip.IncomingReceived IncomingNumber=null, terminate Call!");
//					stopRingBack();
					stopRing();  //tml*** new ring
					return;
				}
				//Wakeup Dialer
				if (DialerActivity.getDialer()==null)
				{
					Intent lIntent = new Intent();
					lIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					lIntent.setClass(this, DialerActivity.class);
					lIntent.putExtra("incomingCall", true);
//					lIntent.putExtra("willBeInCall", true);  //tml*** incall rot/
					lIntent.putExtra("PhoneNumber", IncomingNumber);
					lIntent.putExtra("VideoCall", mVoipCore.getVideoEnabled());
					startActivity(lIntent);
				}
//				startRinging();
				prepareRing(true, 1, CallType, "IncomingReceived");  //tml*** new ring
			}
			else
			{
				//Callee is in call...
				mVoipCore.terminateCall(call);
//				stopRingBack();
				stopRing();  //tml*** new ring
			}
			inCall = true;//alec
		}

		if (state == VoipCall.State.OutgoingInit //alec
				&& (DialerActivity.getDialer() != null)) {
			if (!incomingChatroom && CallType != CALLTYPE_WEBCALL) {
//				startRingBack();
				prepareRing(true, 0, CallType, "OutgoingInit !incomingChatroom");  //tml*** new ring
			}
			inCall = true;//alec
		}
		
		mPrevCallState = state;
	}

	public void show(VoipCore p) {
	}
	
	public boolean mySetStunServer(VoipCore xVoipCore, String stun) {
		//stun server
		xVoipCore.setStunServer(stun);
		xVoipCore.setFirewallPolicy((stun!=null && stun.length()>0) ? STUNC.UseStun : STUNC.DEFAULT);
		return true;
	}
	
	public void deregisterSip() {
		if (mVoipCore != null) {
			Log.e("voip.unregisterSip --- !!!");
			mVoipCore.clearProxyConfigs();
		}
	}
	
	private String CurSipServer="";
	public boolean sipProxyChange(String xUserName, String xSipServer) 
	{	
		Log.e("voip.sipProxyChange! " + xUserName + " " + xSipServer + "=?" + CurSipServer);
		if (xSipServer.contentEquals(CurSipServer))
		{
			if (myProxy!=null && myProxy.getState()==RegistrationState.RegistrationOk)
				return true;
		}
		
		String lProxy = "sip:" + xSipServer;
		String lIdentity = "sip:" + xUserName + "@" + xSipServer;
		
		try {
			if (mVoipCore!=null)
				myProxy = mVoipCore.getDefaultProxyConfig();
		}catch(Exception e){
			Log.e("sipProxyChange1 !@#$ " + e.getMessage());
		}
		
		try {
			if (myProxy == null) {
				myProxy = VoipCoreFactory.instance().createProxyConfig(lIdentity, lProxy, null,true);
				mVoipCore.addProxyConfig(myProxy);
				mVoipCore.setDefaultProxyConfig(myProxy);
			} else {
				/*
				myProxy = VoipCoreFactory.instance().createProxyConfig(lIdentity, lProxy, null,true);
				mVoipCore.addProxyConfig(myProxy);
				mVoipCore.setDefaultProxyConfig(myProxy);
				*/
				myProxy.edit();
				myProxy.setIdentity(lIdentity);
				myProxy.setProxy(lProxy);
				myProxy.enableRegister(true);
				myProxy.done();
			}
			int c = 0;
			while (myProxy.getState()!=RegistrationState.RegistrationOk && c++<50)
				Thread.sleep(100);
			
			CurSipServer=xSipServer;
			return true;
		} catch (Exception e) {
			Log.e("sipProxyChange2 !@#$ " + e.getMessage());
			return false;
		}
	}
	
	public boolean sipProxyAdd(String xUserName, String xSipServer) {
		
		String lProxy = "sip:" + xSipServer;
		String lIdentity = "sip:" + xUserName + "@" + xSipServer;
		Log.e("voip.sipProxyAdd! " + lIdentity);
		
		try {
			mDefaultProxyConfig = VoipCoreFactory.instance().createProxyConfig(lIdentity, lProxy, null,true);
			mVoipCore.addProxyConfig(mDefaultProxyConfig);
			mVoipCore.setDefaultProxyConfig(mDefaultProxyConfig);
		} catch (Exception e) {
			Log.e("sipProxyAdd !@#$ " + e.getMessage());
			return false;
		}
		return true;
	}
	
	public boolean sipProxyDel() {
		return false;
	}
	
	public void enableDisableCodec(String codec, int rate, boolean enabled) {
		try{
			PayloadType pt = mVoipCore.findPayloadType(codec, rate);
			if (pt !=null) {
				mVoipCore.enablePayloadType(pt, enabled);
			} else {
				Log.e("voip.enableDisableCodec findPayloadType=null");
			}
		}catch(VoipCoreException e){
			Log.e("enableDisableCodec !@#$ " + e.getMessage());
		}
	}
	
	public void renableCodec(boolean withVideo)
	{
		//alec: called before calling out
		if (withVideo && CallType==CALLTYPE_FAFA)
		{
			String Model=Build.MODEL.toLowerCase();
			if (Model.contains("gt-n8013"))//Galaxy Note 10.1
			{
				enableDisableCodec("opus",16000,true);
				enableDisableCodec("speex",16000,true);
				enableDisableCodec("AMR",8000,false);
				enableDisableCodec("opus",8000,false);
			}
			else{
				enableDisableCodec("speex",16000,true);
				enableDisableCodec("AMR",8000,true);
				enableDisableCodec("opus",16000,true);
				enableDisableCodec("opus",8000,false);
			}
		}
		else if (withVideo && CallType==CALLTYPE_CHATROOM)
		{
			enableDisableCodec("speex",16000,true);
			enableDisableCodec("opus",16000,false);
			enableDisableCodec("opus",8000,false);
		}
		Log.d("voip.renableCodec " + getCallTypeName(CallType) + " V" + withVideo);
	}

	public void initFromConf() throws VoipConfigException, VoipException {

		String SipServer_default = mPref.read("mySipServer", mySipServer_China);
//		if (AireJupiter.getInstance() != null)
//			SipServer_default = AireJupiter.getInstance().getIsoSip();  //tml*** china ip
		String lUserName;
		String SipServer;
		String password;
		String iso = mPref.read("iso", "cn");
		
		incomingChatroom=false;
		
		if (CallType==CALLTYPE_AIRECALL)
		{
			int myIdx=0;
			try{
				myIdx=Integer.parseInt(mPref.read("myID","0"),16);
			}catch(Exception e){
				Log.e("initFromConf1 !@#$ " + e.getMessage());
			}
			
			lUserName = "**"+myIdx;
			password = mPref.read("password", _Password);
			
//			String iso = mPref.read("iso", "ar");
			String myPhoneNumber=mPref.read("myPhoneNumber", "++++++");
			if (iso.equals("ar") || myPhoneNumber.startsWith("+9")
					|| myPhoneNumber.startsWith("+4") || myPhoneNumber.startsWith("+3")
					|| myPhoneNumber.startsWith("+2")) {
				SipServer = mPref.read("pstnSipServer", "144.76.2.81");
			} else {
//				SipServer = mPref.read("pstnSipServer", "71.19.247.49");
				SipServer = mPref.read("pstnSipServer", SipServer_default);
			}
			
			mPref.write("aireSipServer", SipServer);
			runAsSipAccount=true;
		}
		else if (CallType==CALLTYPE_CHATROOM)
		{
			int myIdx=0;
			try{
				myIdx=Integer.parseInt(mPref.read("myID","0"),16);
			}catch(Exception e){
				Log.e("initFromConf2 !@#$ " + e.getMessage());
			}
			
			lUserName = "**"+myIdx;
			password = mPref.read("password", _Password);
			
			if (mPref.readBoolean("incomingChatroom"))
			{
				Log.d("tmlconf invitee");
				incomingChatroom=true;
				SipServer = mPref.read("joinSipAddress", SipServer_default);
			} else {
				Log.d("tmlconf inviter");
				SipServer = mPref.read("conferenceSipServer", SipServer_default);
				if (AireJupiter.getInstance() != null) {
					SipServer = AireJupiter.getInstance().getIsoConf(SipServer);  //tml*** china ip
				}
			}
			mPref.write("aireSipServer", SipServer);
			runAsSipAccount=true;
		}
		else if (CallType==CALLTYPE_WEBCALL)
		{
			lUserName = mPref.read("aireSipAcount","aire");
			password = mPref.read("aireSipPassowrd", "aire");
			SipServer = mPref.read("aireSipServer", "192.168.0.1");
			runAsSipAccount=true;
		}
		else
		{
			lUserName = mPref.read("myPhoneNumber","+++++");
			password = mPref.read("password", _Password);
			SipServer = mPref.read("mySipServer", _Domain);
			runAsSipAccount=false;
			runAsFileTransfer=(CallType==CALLTYPE_FILETRANSFER);
		}
		//bree:如果是广播并且是新的广播
		if (mPref.readInt("BCAST_CONF", -1) >= 0 && mPref.readBoolean("pay", false)) {
				SipServer="61.136.101.118";
			}
		Log.e("voip.INIT/REGIS " + getCallTypeName(CallType) + CallType
				+ " " + lUserName + ":" + password + "@" + SipServer + " " + iso
				+ " /" + runAsSipAccount + "/" + runAsFileTransfer);
		mPref.write("lastRegisSip", getCallTypeName(CallType) + ", " + SipServer);
		
//		Log.d("*** runAsFileTransfer="+runAsFileTransfer);
		
//		Log.i("voip.Register: " + lUserName + "@" + SipServer);
						
		if (CallType==CALLTYPE_AIRECALL 
			|| CallType==CALLTYPE_CHATROOM 
			|| CallType==CALLTYPE_WEBCALL)
		{
			if (SipServer.equals("204.74.213.5"))
			{
				Log.i("voip.CODECS > 204.74.213.5");
				enableDisableCodec("speex",16000,false);
				enableDisableCodec("H264",90000,false);
				enableDisableCodec("PCMU",8000,true);
				enableDisableCodec("PCMA",8000,true);
				enableDisableCodec("AMR",8000,false);
				enableDisableCodec("opus",16000,false);
			}
			else if (SipServer.equals("74.3.165.175"))
			{
				Log.i("voip.CODECS > 74.3.165.175");
				enableDisableCodec("speex",16000,true);
				enableDisableCodec("H264",90000,false);
				enableDisableCodec("PCMU",8000,true);
				enableDisableCodec("PCMA",8000,true);
				enableDisableCodec("AMR",8000,false);
				enableDisableCodec("opus",16000,false);
			}
			else{
				Log.i("voip.CODECS > OTHER SipServer");
				enableDisableCodec("speex",16000,true);
				enableDisableCodec("H264",90000,false);
				enableDisableCodec("PCMU",8000,false);
				enableDisableCodec("PCMA",8000,false);
				enableDisableCodec("AMR",8000,false);
				enableDisableCodec("opus",16000,false);
			}
			
			if (AmazonKindle.IsKindle())
			{
				Log.i("voip.CODECS > KINDLE");
				enableDisableCodec("speex",16000,false);
				enableDisableCodec("PCMU",8000,true);
				enableDisableCodec("PCMA",8000,true);
				enableDisableCodec("opus",16000,false);
			}
			
			enableDisableCodec("opus",8000,false);
		}
		else
		{
			String Model=Build.MODEL.toLowerCase();
			if (Model.contains("gt-n8013"))//Galaxy Note 10.1
			{
				Log.i("voip.CODECS > GALAXYNOTE");
				enableDisableCodec("opus",16000,true);
				enableDisableCodec("speex",16000,true);
				enableDisableCodec("opus",8000,false);
				enableDisableCodec("AMR",8000,false);
			}
			else if (mPref.read("moodcontent", "--").endsWith("!!!!!!!"))
			{
				Log.i("voip.CODECS > !!!!!!!");
				enableDisableCodec("opus",16000,mPref.readBoolean("enable_opus_16k", true));
				enableDisableCodec("speex",16000,mPref.readBoolean("enable_speex", true));
				enableDisableCodec("opus",8000,mPref.readBoolean("enable_opus_8k", true));
				enableDisableCodec("AMR",8000,true);
			}
			else{
				Log.i("voip.CODECS > OTHER2 " + Model);  //video,call
				enableDisableCodec("opus",16000,true);
				enableDisableCodec("speex",16000,true);
				enableDisableCodec("opus",8000,false);
				enableDisableCodec("AMR",8000,true);
			}
			
			enableDisableCodec("H264",90000,true);
			enableDisableCodec("PCMU",8000,false);
			enableDisableCodec("PCMA",8000,false);
		}
		
		Version.dumpCapabilities();
		
		if (Version.isVideoCapable() && mVoipCore.checkVideoAvailable())
		{
			if (CallType==CALLTYPE_FAFA || CallType==CALLTYPE_CHATROOM)
			{
				AndroidVideoApi5JniWrapper.setAndroidSdkVersion(Version.sdk());
			
				if (AndroidCameraConfiguration.retrieveCameras().length>0)
				{
					mVoipCore.enableVideo(true, true);
					mVoipCore.setPreferredVideoSize(VideoSize.createStandard(Version.sdk()>10?VideoSize.VGA:VideoSize.CIF, false));
//					if (mPref.readBoolean("V1080", false)) {
//						mVoipCore.setPreferredVideoSize(VideoSize.createStandard(Version.sdk()>10?VideoSize.HD1080p:VideoSize.CIF, false));
//					} else {
//						mVoipCore.setPreferredVideoSize(VideoSize.createStandard(Version.sdk()>10?VideoSize.VGA:VideoSize.CIF, false));
//					}
					
					int id = 1;
					id %= AndroidCameraConfiguration.retrieveCameras().length;
					mVoipCore.setVideoDevice(id);
				}
				else{
					mVoipCore.enableVideo(false, false);
					mPref.write("video_support", false);
				}
			}
			else
				mVoipCore.enableVideo(false, false);
			mPref.write("video_support", true);
		}/*
		else if (Version.isSamsung30Pad())
		{
			AndroidVideoApi5JniWrapper.setAndroidSdkVersion(Version.sdk());
		
			mVoipCore.enableVideo(true, true);
			mVoipCore.setPreferredVideoSize(VideoSize.createStandard(VideoSize.VGA, false));
			
			int id = 1;
			id %= AndroidCameraConfiguration.retrieveCameras().length;
			mVoipCore.setVideoDevice(id);
			mPref.write("video_support", true);
		}*/
		else {
			mVoipCore.enableVideo(false, false);
			
			mPref.write("video_support", false);
		}

		//stun server
		//mVoipCore.setStunServer(null);
		mVoipCore.setFirewallPolicy(STUNC.DEFAULT);
		
		//auth
		mVoipCore.clearAuthInfos();
		VoipAuthInfo lAuthInfo = VoipCoreFactory.instance().createAuthInfo(lUserName, password, null);
		mVoipCore.addAuthInfo(lAuthInfo);

		//Proxy server
		mVoipCore.clearProxyConfigs();
		
		String lProxy;
		VoipProxyConfig lDefaultProxyConfig;
		String lIdentity;

		lProxy="sip:"+SipServer;
		
		//get Default Proxy if any
		lDefaultProxyConfig = mVoipCore.getDefaultProxyConfig();
		
		lIdentity = "sip:" + lUserName + "@" + SipServer;
		try {
			if (lDefaultProxyConfig == null) {
				lDefaultProxyConfig = VoipCoreFactory.instance().createProxyConfig(lIdentity, lProxy, null,true);
				mVoipCore.addProxyConfig(lDefaultProxyConfig);
				if ((CallType==CALLTYPE_WEBCALL) && SipServer.equals("204.74.213.5"))
				{
					lDefaultProxyConfig.edit();
					lDefaultProxyConfig.enableRegister(false);
					lDefaultProxyConfig.done();
				}
				mVoipCore.setDefaultProxyConfig(lDefaultProxyConfig);
			} else {
				lDefaultProxyConfig.edit();
				lDefaultProxyConfig.setIdentity(lIdentity);
				lDefaultProxyConfig.setProxy(lProxy);
				if ((CallType==CALLTYPE_WEBCALL) && SipServer.equals("204.74.213.5"))
					lDefaultProxyConfig.enableRegister(false);
				else
					lDefaultProxyConfig.enableRegister(true);
				lDefaultProxyConfig.done();
			}
			lDefaultProxyConfig = mVoipCore.getDefaultProxyConfig();

			if (lDefaultProxyConfig !=null) {
				//escape +
				lDefaultProxyConfig.setDialEscapePlus(false);
				//outbound proxy
				lDefaultProxyConfig.setRoute(null);
			}
			//init network state
			ConnectivityManager lConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo lInfo = lConnectivityManager.getActiveNetworkInfo();
			mVoipCore.setNetworkReachable((lInfo!=null) ?
					(lConnectivityManager.getActiveNetworkInfo().getState()==NetworkInfo.State.CONNECTED):false); 
		} catch (VoipCoreException e) {
			Log.e("initFromConf3 !@#$ " + e.getMessage());
			throw new VoipConfigException("Wrong setting",e);
		}
	}

	public VoipCore getVoipCore() {
		return mVoipCore;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onDestroy() {
		//xwf 清除注册状态
		registered = false;
		mPref.write("curCall", "");
		callstate_AV = null;
		destroying=true;
		mHandler.removeCallbacks(quitServiceY);
		if (AireJupiter.getInstance()!=null)
			AireJupiter.getInstance().StopEndingupServiceY();
		
		deregisterSip();//alec
		
		if (mTimer!=null)
			mTimer.cancel();
		mTimer=null;
		try{//alec, in case nativePtr==0
			if (mVoipCore!=null) {
				mVoipCore.terminateCall(mVoipCore.getCurrentCall());
				mVoipCore.destroy();
			}
		}catch(RuntimeException e){
			Log.e("av13 " + e.getMessage());
		}
		catch(Exception e){
			Log.e("av14 " + e.getMessage());
		}
		mVoipCore=null;
		theVoip=null;
//		stopRingBack();
//		stopRinging();
		stopRing();  //tml*** new ring
//		if(rwb!=null){  //yang*** speex player
//		rwb.stop();
//		rwb.release();
//		}
		mAudioManager.setMode(AudioManager.MODE_NORMAL);  //tml*** audio break/
		//SoundPool.release();
		runAsSipAccount=false;
		runAsFileTransfer=false;
		destroying=false;
		Log.e("*** AIREVENUS/SERVICE-Y *** DESTROY DESTROY *** voip " + SettingActivity.vlib);
		super.onDestroy();
	}
	
	public static VoipCore getLc() {
		if (instance()==null) return null;
		return instance().getVoipCore();
	}

	//tml*** new ring()
	private AudioTrack mAudioTrack = null;
	private InputStream inS = null;
	private DataInputStream dinS = null;
	private volatile boolean ringrdy = false;
	@SuppressWarnings("deprecation")
	public void prepareRing(boolean en, int mode, int calltype, String from) {
		if (en) {
			Log.d("RING." + mode + getCallTypeName(calltype) + " AV *** PREP! (" + from + ")");
			if (mode == 1) {  //incoming
				if (mAudioTrack == null) {
					int iMinBufSize = AudioTrack.getMinBufferSize(16000, 
							AudioFormat.CHANNEL_CONFIGURATION_MONO, 
							AudioFormat.ENCODING_PCM_16BIT);
					mAudioTrack = new AudioTrack(AudioManager.STREAM_RING, 16000, 
							AudioFormat.CHANNEL_CONFIGURATION_MONO, 
							AudioFormat.ENCODING_PCM_16BIT, 
							iMinBufSize, AudioTrack.MODE_STREAM);
					maxiVol(1, 1);
					controlBkgndMusic(0);
					StartRing startRing = new StartRing(mode, calltype, true);
					new Thread(startRing).start();
				}
			} else if (mode == 0) {  //outgoing
				if (mAudioTrack == null) {
					int iMinBufSize = AudioTrack.getMinBufferSize(16000, 
							AudioFormat.CHANNEL_CONFIGURATION_MONO, 
							AudioFormat.ENCODING_PCM_16BIT);
					mAudioTrack = new AudioTrack(AudioManager.STREAM_RING, 16000, 
							AudioFormat.CHANNEL_CONFIGURATION_MONO, 
							AudioFormat.ENCODING_PCM_16BIT, 
							iMinBufSize, AudioTrack.MODE_STREAM);
					maxiVol(1, 3);
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
				AireJupiter.getInstance().TESTseeVolumes("AVring");
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
			
			if (mode == 1) {  //incoming
				String intx = Integer.toString(rng.nextInt(8) + 1);
				ringfile = "r16k_" + intx + ".raw";
			} else {  //outgoing
				String intx = Integer.toString(rng.nextInt(8) + 1);
				ringfile = "r16k_" + intx + ".raw"; 
			}

			Log.d("RING." + ringfile + " *** DO! " + ringrdy);
			if (ringrdy && mAudioTrack != null) {
				AssetManager inAssets = getAssets();
				inS = inAssets.open(ringfile);
				dinS = new DataInputStream(inS);

				mAudioTrack.play();
				while(((i = dinS.read(audiobuff, 0, buffSize)) > -1) ) {
					mAudioTrack.write(audiobuff, 0, i);
					if (!ringrdy || theVoip == null) break;
			    }
			} else {
				Log.e("RING mAudioTrack null");
			}

			audiobuff = null;
			Log.d("RING reach END.force(" + !ringrdy + ")");
			if (ringrdy && theVoip != null) {  //incoming
				Log.d("RING repeat");
				if (inS != null) {
					inS.close();
					inS = null;
				}
				if (dinS != null) {
					dinS.close();
					dinS = null;
				}
				if (ringrdy) {
					repeat = true;
					StartRing startRing = new StartRing(mode, CallType, false);
					new Thread(startRing).start();
				}
			}
		} catch (Exception e) {
			Log.e("AV RING !@#$ " + e.getMessage());
			stopRing();
		} finally {
			if (!repeat) ringrdy = false;
		}
	}

	Runnable runRepeat = new Runnable () {
		@Override
		public void run() {
		}
	};
	
	public void stopRing() {
		mHandler.removeCallbacks(runRepeat);
		try {
			ringrdy = false;
			if (mAudioTrack != null) {
				mAudioTrack.stop();
				mAudioTrack.flush();
				mAudioTrack.release();
				mAudioTrack = null;
				maxiVol(0, 0);
				Log.d("RING *** STOP!");
			}
			if (inS != null) inS.close();
			if (dinS != null) dinS.close();
		} catch (IOException e) {
			Log.e("DA stopRing !@#$ " + e.getMessage());
		} finally {
			mAudioTrack = null;
			inS = null;
			dinS = null;
			maxiVol(0, 0);
		}
	}
	
	private int prevVol1, returnVol = 0;
	private int maxVol1 = 1;
	private void maxiVol(int mode, double divAM) {
		AudioManager audioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
		if (divAM <= 0) divAM = 1;
		if (mode == 1) {  //max vol
			prevVol1 = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			maxVol1 = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			int sysMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
			returnVol = 1;
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (maxVol1 / divAM), 0);
			if (sysMax > 6) sysMax = sysMax - 3;
			else if (sysMax > 2) sysMax = sysMax - 1;
			audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, sysMax, 0);
			Log.d("maxiVol AV set." + mode + " " + divAM + "|" + prevVol1 + "|" + maxVol1);
		} else if (mode == 0 && returnVol == 1) {  //return vol
			maxVol1 = prevVol1;
			returnVol = 0;
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (maxVol1 / divAM), 0);
			Log.d("maxiVol AV return." + mode + " " + divAM + "|" + prevVol1);
		}
	}
	
	boolean musicWasActive = false;
	private void controlBkgndMusic (int mode) {
//		AudioManager mAudioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
		Intent imediaplay = new Intent("com.android.music.musicservicecommand");
		if (mode == 0 && mAudioManager.isMusicActive()) {
			mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
//			imediaplay.putExtra("command", "pause");
//			sendBroadcast(imediaplay);
			musicWasActive = true;
			Log.d("controlBkgndMusic" + mode + musicWasActive);
		} else if (mode == 1 && musicWasActive) {
			mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
//			imediaplay.putExtra("command", "play");
//			sendBroadcast(imediaplay);
			Log.d("controlBkgndMusic" + mode + musicWasActive);
			musicWasActive = false;
		} else if (mode == 2) {
			mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
			Log.d("controlBkgndMusic" + mode + musicWasActive);
			musicWasActive = false;
		}
	}
	//***tml
	
//	private synchronized void startRinging_old()  {
//		try {
//			if (mVibrator !=null) {
//				long[] patern = {0,1000,1000};
//				mVibrator.vibrate(patern, 1);
//			}
////			if (mRingerPlayer == null) {
////				mRingerPlayer = new MediaPlayer();
////				try{
////					mRingerPlayer.setAudioStreamType(AudioManager.STREAM_RING);
////					mRingerPlayer.setDataSource(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
////					mRingerPlayer.prepare();
////					mRingerPlayer.setLooping(true);
////					mRingerPlayer.start();
////				}catch (Exception e){
////					Log.e("av15 " + e.getMessage());
////					mRingerPlayer=null;
////				}
////			} else {
////				Log.w("already ringing");
////			}
//			//tml|sw*** audio break
//			controlBkgndMusic(0);
//			if (mAudioTrack == null && !ringrdy) {
//				Log.d("tml AV STARTringing ***** DO!");
//				int iMinBufSize = AudioTrack.getMinBufferSize(16000, 
//						AudioFormat.CHANNEL_CONFIGURATION_MONO, 
//						AudioFormat.ENCODING_PCM_16BIT);
//				mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
//						AudioFormat.CHANNEL_CONFIGURATION_MONO, 
//						AudioFormat.ENCODING_PCM_16BIT,
//						iMinBufSize, AudioTrack.MODE_STREAM);
//				
//				mHandler.post(new Runnable () {
//					@Override
//		            public void run() {
//						ringrdy = true;
//						ringTask = new MyRingerTask(1);
//						ringTask.execute();
//					}
//				});
//				
////				new Thread(new Runnable() {
////					public void run() {
////					}
////				}).start();
//			} else {
//				Log.w("already ringing");
//			}
//			//***tml
//		} catch (Exception e) {
//			Log.e("cannot handle incoming call "+e.getMessage());
//			//tml|sw*** audio break
//			mAudioTrack = null;
//			ringrdy = false;
//			//***tml
//		}
//	}
//	
//	public synchronized void stopRinging_old() {
////		if (mRingerPlayer !=null) {
////			mRingerPlayer.stop();
////			mRingerPlayer.release();
////			mRingerPlayer=null;
////		}
//		//tml|sw*** audio break
//		if (mAudioTrack != null && ringrdy) {
//			try {
//				Log.d("tml AV STOPring ***** DO!");
//				ringTask.cancel(true);
//				ringrdy = false;
////				mAudioTrack.flush();
////				mAudioTrack.stop();
////				mAudioTrack.release();
////				mAudioTrack = null;
//				if (inS != null) {
//					inS.close();
//				}
//				if (dinS != null) {
//					dinS.close();
//				}
//				inS = null;
//				dinS = null;
//				Log.d("tml AV STOPring ***** SUCCESS! ");
//			} catch (IOException e) {
//				Log.e("av stopRing " + e.getMessage());
//			}
//		}
//		//***tml
//		if (mVibrator!=null) {
//			mVibrator.cancel();
//		}
//	}
//	
//	public synchronized void startRingBack_old()  {
//		try {
////			if (mRingerPlayer == null) {
////				try{
////					Random r = new Random();
////					mRingerPlayer = MediaPlayer.create(this, R.raw.ringback1+r.nextInt(7));
////					mRingerPlayer.setLooping(true);
////					mRingerPlayer.start();
////					
////				    //sp=SoundPool.play(r.nextInt(4)+1, 1, 1, 0, -1, 1);
////				}catch(Exception e) {
////					Log.e("av17 " + e.getMessage());
////					mRingerPlayer=null;
////				}
////			}
//			//tml|sw*** audio break
//			controlBkgndMusic(0);
//			if (mAudioTrack == null && !ringrdy) {
//				Log.d("tml AV STARTringback ***** DO!");
//				int iMinBufSize = AudioTrack.getMinBufferSize(16000, 
//						AudioFormat.CHANNEL_CONFIGURATION_MONO, 
//						AudioFormat.ENCODING_PCM_16BIT);
//				mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
//						AudioFormat.CHANNEL_CONFIGURATION_MONO, 
//						AudioFormat.ENCODING_PCM_16BIT,
//						iMinBufSize, AudioTrack.MODE_STREAM);
//				
//				mHandler.post(new Runnable () {
//					@Override
//		            public void run() {
//						ringrdy = true;
//						ringTask = new MyRingerTask(1);
//						ringTask.execute();
//					}
//				});
//				
////				new Thread(new Runnable() {
////					public void run() {
////						ringrdy = true;
////						ringTime(0);
////					}
////				}).start();
//			}
//			//***tml
//		} catch (Exception e) {
//			Log.e("av18 " + e.getMessage());
//			//tml|sw*** audio break
//			mAudioTrack = null;
//			ringrdy = false;
//			//***tml
//		}
//	}
//	
//	private synchronized void stopRingBack_old() {
//		/*if (sp>0)
//		{
//			SoundPool.pause(sp);
//			SoundPool.stop(sp);
//		}*/
////		if (mRingerPlayer !=null) {
////			mRingerPlayer.stop();
////			mRingerPlayer.release();
////			mRingerPlayer=null;
////		}
//		//tml|sw*** audio break
//		if (mAudioTrack != null && ringrdy) {
//			try {
//				Log.d("tml AV STOPring ***** DO!");
//				ringTask.cancel(true);
//				ringrdy = false;
////				mAudioTrack.flush();
////				mAudioTrack.stop();
////				mAudioTrack.release();
////				mAudioTrack = null;
//				if (inS != null) {
//					inS.close();
//				}
//				if (dinS != null) {
//					dinS.close();
//				}
//				inS = null;
//				dinS = null;
//				Log.d("tml AV STOPring ***** SUCCESS! ");
//			} catch (IOException e) {
//				Log.e("av stopRingB " + e.getMessage());
//			}
//		}
//		//***tml
//	}
//	
//	//tml|sw*** audio break, new ringer code, so ui doesnt freeze
//	private MyRingerTask ringTask;
//	private class MyRingerTask extends AsyncTask<Void, Void, Void> {
//		int ringmode;
//		MyRingerTask (int mode) {
//			ringmode = mode;
//		}
//		
//		@Override
//		protected Void doInBackground(Void... params) {
//    		try {
//				int bufferSize = 5120;
//			    byte[] audiobuff = new byte[bufferSize];
//
//				int i = 0;
//				AssetManager am = getAssets();
//				Random rng = new Random();
//				String intx = Integer.toString(rng.nextInt(8) + 1);
//				String musicfile = "r16k_" + intx + ".raw";
//				int gst = mAudioTrack.getStreamType();
//				int gsv = mAudioManager.getStreamVolume(gst);
////				mAudioManager.setStreamMute(gst, false);
//				Log.i("tml ringfile> " + musicfile + " " + gst + "=" + gsv);
//				
//				inS = am.open(musicfile);
//				dinS = new DataInputStream(inS);
//				
//				if (ringrdy) {
//					inS = am.open(musicfile);
//					dinS = new DataInputStream(inS);
//
//					mAudioTrack.play();
//					while(((i = dinS.read(audiobuff, 0, bufferSize)) != -1)) {
//						mAudioTrack.write(audiobuff, 0, i);
//						if (ringTask.isCancelled()) {
//							mAudioTrack.pause();
//							mAudioTrack.flush();
//							mAudioTrack.stop();
//							mAudioTrack.release();
//							Log.e("tml AV ring.mAudioTrack cleared!");
//							break;
//						}
//				    }
//				}
////				mAudioTrack.flush();
////				mAudioTrack.stop();
////				mAudioTrack.release();
//				mAudioTrack = null;
////				inS.close();
////				dinS.close();
////				inS = null;
////				dinS = null;
////				Log.d("tml AV STOPring ***** SUCCESS! ");
//    		} catch (Exception e) {
//				Log.e("av13 " + e.getMessage());
//				ringrdy = false;
//				mAudioTrack.stop();
//				mAudioTrack.release();
//				mAudioTrack = null;
//				if (inS != null) {
//					inS = null;
//				}
//				if (dinS != null) {
//					dinS = null;
//				}
//			}
//			return null;
//		}
//	}
//	//***tml
//
//	//yang*** speex player
//	private RingerPlayer_WB rwb;
//	public synchronized void startRingBackSpeex(String from)  {
//		Log.d("yang start ring back speex");
//		AssetManager am = getAssets();// u have get assets path from this code
//		Random random = new Random();
//		int numring = 2;
//		int i =random.nextInt(numring) + 1; 
//		String ring = "ring"+i+".spx";
//		Log.d("yang ring "+ring);
//		try {
//			InputStream inputStream = am.open(ring);
//			byte[] data = toByteArray(inputStream);
//			ArrayList<byte[]> arrayList = new ArrayList<byte[]>();
//			byte[] newdata = new byte[data.length]; 
//			System.arraycopy(data, 0, newdata, 0, data.length);
//			
//			rwb= new RingerPlayer_WB(AireVenus.this, 3, true);
//			
//			rwb.append(newdata, newdata.length);
//			rwb.run();
//
//		} catch (IOException e) {
//			rwb.stop();
//			rwb.release();
//			e.printStackTrace();
//		}
//	}
//
//	public static byte[] toByteArray(InputStream input) {
//		ByteArrayOutputStream output = new ByteArrayOutputStream();
//		byte[] buffer = new byte[4096];
//		int n = 0;
//		try {
//			while (-1 != (n = input.read(buffer))) {
//				output.write(buffer, 0, n);
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//			try {
//				output.close();
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
//		}
//		return output.toByteArray();
//	}
	//***yang
	
	public void callStopRing()
	{
		Log.d("ServiceY :callStopRing");
//		stopRingBack();
//		stopRinging();
		stopRing();  //tml*** new ring
	}
	//tml*** china ip
//	public String getIsoSip() {
//		String sip = mySipServer_China;
//		String iso = mPref.read("iso", "cn");
//		String savedsip = mPref.read("mySipServer", sip);
//		if (!iso.equals("cn")) {
//			sip = mySipServer_USA;
//			if (!savedsip.equals(mySipServer_USA)) sip = savedsip;
//		} else {
//			mPref.write("mySipServer", sip);
//		}
//		mySipServer_default = sip;
//		AireJupiter.mySipServer_default = sip;  //tml*** xcountry sip
//		Log.d("isoSip " + iso + " " + savedsip + ">" + sip);
//		return sip;
//	}
}

