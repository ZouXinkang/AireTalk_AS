package com.pingshow.voip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ActivityManager;
import android.app.KeyguardManager;
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
import android.telephony.TelephonyManager;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.AmazonKindle;
import com.pingshow.airecenter.LocationSettingActivity;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MainActivity;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.SecurityNewActivity;
import com.pingshow.airecenter.SettingPage;
import com.pingshow.airecenter.ShoppingActivity;
import com.pingshow.airecenter.browser.MainBrowser;
import com.pingshow.airecenter.cons.Key;
import com.pingshow.network.NetInfo;
import com.pingshow.util.LedSpeakerUtil;
import com.pingshow.util.MyUtil;
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

//	private MediaPlayer mRingerPlayer = null;
	private VoipCall.State mPrevCallState;
//	private Vibrator mVibrator;
	private AudioManager mAudioManager;

	private Handler mHandler =  new Handler() ;
	
	public final static int CALLTYPE_FAFA=0;
	public final static int CALLTYPE_AIRECALL=1;
	public final static int CALLTYPE_CHATROOM=2;
	public final static int CALLTYPE_WEBCALL=3;
	public final static int CALLTYPE_FILETRANSFER=4;
	
	private boolean incomingChatroom=false;
	
	private static int CallType;//0: FafaCall, 1:AireCall  2.WebCall   3.FileTransfer
	
	private boolean needblink;  //wjx*** ledspeaker/
	
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
			callTypeName = "FILETRANSFER";
		} else {
			callTypeName = "UNKNOWN";
		}
		return callTypeName;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.e("*** !!! AIREVENUS/SERVICE-Y *** START START !!! *** voip " + getCallTypeName(CallType) + CallType + " " + SettingPage.vlib);
		
		mPref = new MyPreference(this);
		ringrdy = false;
		mAudioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
//		mAudioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_RINGER);
//		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		
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
			
			try {
				FileOutputStream file = new FileOutputStream(new File("/data/data/com.pingshow.airecenter/files/upnpc.ini"));
				String out = mPref.readInt("audio_local_port", 0)
						+ "\n" + mPref.readInt("video_local_port", 0)
						+ "\n";
				file.write(out.getBytes());
				file.flush();
				file.close();
			} catch(Exception e) {
				Log.e("AVfile " + e.getMessage());
			}
			
			boolean sipcall = (CallType==CALLTYPE_CHATROOM || CallType==CALLTYPE_AIRECALL || CallType==CALLTYPE_WEBCALL);
			
			int monitor = 0;
			/*if (mPref.readBoolean("RecvOnly", false))
				monitor=1;
			
			Log.d("monitor="+monitor);
			*/
			int sipTransportType = 0;  //TLS 0, UDP 1
			if (CallType == CALLTYPE_CHATROOM) {
				sipTransportType = 1;
			}
			
			if (mPref.read("moodcontent", "--").startsWith("p2p off"))
			{
				Log.e("voip.createVoipCore p2p off");
				mVoipCore = VoipCoreFactory.instance().createVoipCore(this, stun, 
						mPref.readInt("audio_local_port", 0), mPref.readInt("video_local_port", 0),
						0, monitor, 1, sipTransportType);
			}
			else if (CallType == CALLTYPE_WEBCALL) {  //sw|vivid*** webcall
				Log.e("voip.createVoipCore CALLTYPE_WEBCALL");
				mVoipCore = VoipCoreFactory.instance().createVoipCore(this, stun, 
						mPref.readInt("audio_local_port", 0), mPref.readInt("video_local_port", 0),
						sipcall?0:1, monitor, 0, 1);
			}
			else{
				Log.e("voip.createVoipCore DEFAULT");
				mVoipCore = VoipCoreFactory.instance().createVoipCore(this, stun, 
						mPref.readInt("audio_local_port", 0), mPref.readInt("video_local_port", 0),
						sipcall?0:1, monitor, 1, sipTransportType);
			}
			Log.e("*** voip.createVoipCore *** sipcall=" + sipcall + " " + getCallTypeName(CallType) + " Stun=" + stun + " transType" + sipTransportType);
			Log.i("*** voip.UPNP audio_local_port=" + mPref.readInt("audio_local_port", 0) + ", video=" + mPref.readInt("video_local_port", 0));
			displayCallStatus("VENUS sip=" + sipcall + " " + getCallTypeName(CallType) + " stun=" + stun + " AVport=" + mPref.readInt("audio_local_port", 0) + "/" + mPref.readInt("video_local_port", 0));
			
			//TLS***
//			int hardwareSupportedHD=1;
//			/*if (CPUTool.getNumCores()>=2||CPUTool.getMaxCpuFreq()>1250000) {
//				hardwareSupportedHD=1;
//			}*/
//			
//			int sipTransportType=0;
//			
//			if (mPref.read("moodcontent", "--").startsWith("udp")) {
//				sipTransportType=1;
//			}
//			
//			if (mPref.read("moodcontent", "--").startsWith("p2p off")||mPref.read("moodcontent", "--").startsWith("tlsp2p off")||mPref.read("moodcontent", "--").startsWith("udpp2p off"))
//			{
//				mVoipCore = VoipCoreFactory.instance().createVoipCore(this, stun, 
//						mPref.readInt("audio_local_port", 0), mPref.readInt("video_local_port", 0),
//						0, 0/*monitor*/,hardwareSupportedHD,sipTransportType);
//			}
//			else{
//				boolean ftmode=(AireVenus.getCallType()==AireVenus.CALLTYPE_FILETRANSFER);
//				mVoipCore = VoipCoreFactory.instance().createVoipCore(this, stun, 
//						mPref.readInt("audio_local_port", 0), mPref.readInt("video_local_port", 0),
//						sipcall?0:(ftmode?2:1), 0/*monitor*/,hardwareSupportedHD,sipTransportType);
//			}
			//***TLS
			
			try {
				initFromConf();
			} catch (VoipException e) {
				Log.e("no config ready yet");
			}
			TimerTask lTask = new TimerTask() {
				@Override
				public void run() {
					try {
						mVoipCore.iterate();
					} catch (RuntimeException e) {
						Log.w("iterate Exception");
					}
				}
			};
			
			mTimer.scheduleAtFixedRate(lTask, 0, 100);
			theVoip = this;
		}
		catch (Exception e) {
			Log.e("Cannot start Voip: "+e.getMessage());
		}
		
		if (CallType!=CALLTYPE_AIRECALL)
			mHandler.postDelayed(quitServiceY, 60000);  //170000
		
		if (mPref.readBoolean("usestanleysip"))
		{
			mHandler.postDelayed(runAlive, 60000);
		}
//		Log.e("tml AV AUDIO MODE= " + mAudioManager.getMode());
	}
	
	private Runnable runAlive = new Runnable() {
		@Override
		public void run() {
			if (mPref.readBoolean("usestanleysip"))
			{
				try {
					if (!inCall && (DialerActivity.getDialer()==null))
						theVoip.initFromConf();
					mHandler.postDelayed(runAlive, 60000);
				} catch (VoipConfigException e) {
					Log.e("av2 " + e.getMessage());
					e.printStackTrace();
				} catch (VoipException e) {
					Log.e("av3 " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	};
	
	// SImon : to log in to sip server of the specified domain
	public static boolean sip_login(String domain, String username) {
		if (instance()!=null)
			return instance().sipProxyChange(username, domain);
		return false;
	}
	public void authInfoRequested(VoipCore p, String realm, String username) {

	}
	public void byeReceived(VoipCore p, String from) {

	}
	public void displayMessage(VoipCore p, String message) {
	}
	
	public void displayStatus(final VoipCore p, final String message) {
		if (message.startsWith("Contacting"))
			DialerActivity.rejectHangingup = true;
		else
			DialerActivity.rejectHangingup = false;
		displayCallStatus("core: " + message);
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
		displayCallStatus("["+state+"] " + registered);
		Log.i("voip.AV regis: new ["+state+"]  reg:" + registered);
	}
	
	private boolean hangUpDelay=false;

	public static String callstate_AV = null;  //tml***/
	public void callState(final VoipCore p,final VoipCall call, final State state, final String message) {
//		Log.i("prev_state = " + mPrevCallState + " new state ["+state+"]  " + message);
		displayCallStatus("["+state+"] " + message);  //tml test
		Log.i("voip.AV call: new ["+state+"] " + message + "  //  prev=" + mPrevCallState);
		callstate_AV = state.toString();  //tml*** return Dialer view
		if (destroying) {
			mPref.write("curCall", "");
			callstate_AV = null;
			return;
		}
		if (state == VoipCall.State.IncomingReceived && !call.equals(mVoipCore.getCurrentCall())) 
		{
			//no multicall support, just decline
			mVoipCore.terminateCall(call);
			Log.e("exit.IncomingReceived: no multicall support, just decline, terminateCall");
//			stopRingBack();
			stopRing();  //tml*** new ring
			return;
		}
		if (state==VoipCall.State.CallReleased)//alec
		{ 
			mPrevCallState=state;
			inCall = false;//alec
			mPref.write("curCall", "");
			callstate_AV = null;
			return;
		}
		if (state==VoipCall.State.OutgoingEarlyMedia)
		{
			if (runAsSipAccount)
			{
				mHandler.postDelayed(new Runnable(){
					public void run(){
//						stopRingBack();
						stopRing();  //tml*** new ring
					}
				}, 1250);  //possible audiohw overlap @ 1250
				mHandler.post(destroySpeechListen);  //tml*** voice control
			}
		}
		if (mPrevCallState == VoipCall.State.Connected)
		{
			// SIMON 030211:
			// need to move connect message to the head of the queue
		    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO); // Simon 030811 move it to highest priority
			needblink=false;  //wjx*** ledspeaker/
		    if (!DialerActivity.speakerOn) LedSpeakerUtil.setSpeakerOff();
		    
		    boolean incoming=false;
		    if (DialerActivity.getDialer()!=null)
		    	incoming=DialerActivity.incomingCall;

		    if (incoming)
		    {
		    	if (DialerActivity.getDialer()!=null) DialerActivity.getDialer().callState(p,call,state,message);

//	 			stopRingBack();
		    	stopRing();  //tml*** new ring
//	 			long[] patern = {0,80,1000};
//	 			mVibrator.vibrate(patern, -1);
		    }
		    else{
		    	NetInfo ni = new NetInfo(AireVenus.this);
				int netType = ni.netType;
			    mHandler.postDelayed(new Runnable(){
			    	public void run()
			    	{
//			    		stopRingBack();
			    		stopRing();  //tml*** new ring
//			 			long[] patern = {0,80,1000};
//			 			mVibrator.vibrate(patern, -1);
			 			
			 			if (DialerActivity.getDialer()!=null)  
			 				DialerActivity.getDialer().callState(p,call,state,message);
			    	}
			    }, 3200-netType*1000);
		    }
			//tml*** secure ring
//			String mAddress = p.getRemoteAddress().getUserName();
//			boolean secureRing = false;
//			List<String> instants = mPref.readArray("instants");
//			if (instants != null) {
//				for (String address : instants) {
//					if (address.equals(mAddress)
//							&& (mPref.readBoolean("autoAnswer:" + mAddress, true))) {
//						secureRing = true;
//						break;
//					}
//				}
//			}
//			if (!secureRing) {
//				AireJupiter.getInstance().unreadBlinkOff();  //tml*** unread led
//			    LedSpeakerUtil.setLedOn();  //wjx*** ledspeaker/
//			}
			inCall = true;//alec
		}
		else if (state == VoipCall.State.CallEnd) {
			
			//wjx*** ledspeaker
			needblink=false;
			LedSpeakerUtil.setSpeakerOff();
		    LedSpeakerUtil.setLedOff();
		    //***wjx
//			stopRingBack();
		    stopRing();  //tml*** new ring
			controlBkgndMusic(2);
//			mVibrator.cancel();
			
			VoipCall c=mVoipCore.getCurrentCall();
			if (c!=null && call!=null && !call.equals(c)) return;
			
			if (DialerActivity.getDialer()!=null)  
 				DialerActivity.getDialer().callState(p,call,state,message);
			
			if (RingDialog.getInstance()!=null)  
				RingDialog.getInstance().bye();
			
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
			
			inCall = false;//alec
			mPref.write("curCall", "");
			callstate_AV = null;
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
					Log.e("voip.exit.IncomingReceived IncomingNumber=null, terminate Call!");
//					stopRingBack();
					stopRing();  //tml*** new ring
					return;
				}
				//Wakeup Dialer
				if (DialerActivity.getDialer()==null)
				{
//					if (isAireCenterRunning() || mPref.readBoolean("autoAnswer:"+IncomingNumber, true))
					//tml*** auto2
					if (isAireCenterRunning()
							|| (mPref.readBoolean("autoAnswer:"+IncomingNumber, false)
									|| mPref.readBoolean("autoAnswer2:"+IncomingNumber, false)))
					//***tml
					{
						Intent lIntent = new Intent();
						lIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						lIntent.setClass(this, DialerActivity.class);
						lIntent.putExtra("incomingCall", true);
						lIntent.putExtra("PhoneNumber", IncomingNumber);
						lIntent.putExtra("VideoCall", mVoipCore.getVideoEnabled());
						startActivity(lIntent);
					}
					else{
						Intent lIntent = new Intent();
						lIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						lIntent.setClass(this, RingDialog.class);
						lIntent.putExtra("PhoneNumber", IncomingNumber);
						lIntent.putExtra("VideoCall", mVoipCore.getVideoEnabled());
						startActivity(lIntent);
						//tml*** voice
						if(mPref.readBoolean("voice_control", false)
								&& !mPref.readBoolean("normal_ring", true)) {
							maxiMic(1, 1);
						}
					}
				}
				//tml*** secure ring
				String mAddress = IncomingNumber;
				boolean secureRing = false;
				boolean secureCEC = false;
				List<String> instants = mPref.readArray("instants");
    			if (instants != null) {
    				for (String address : instants) {
    					if (address.equals(mAddress)) {
    						if (mPref.readBoolean("autoAnswer:" + mAddress, false)) {
    							secureCEC = true;
    						}
    						secureRing = true;
    						break;
    					}
    				}
    			}
				if (!secureRing) {
					//wjx*** ledspeaker
					needblink=true;
					Thread t1=new Thread() {
						@Override
						public void run() {
							Log.e("t1 start:::needblink==="+needblink);
							while (needblink) {
									MyUtil.Sleep(100);
									if (!needblink) break;
									LedSpeakerUtil.setLedOn();
									MyUtil.Sleep(100);
									LedSpeakerUtil.setLedOff();
								}
							}
					};
					t1.start();
					LedSpeakerUtil.setSpeakerOn();
					//***wjx
//					startRinging("IncomingReceived !secureRing");
					prepareRing(true, 1, CallType, "IncomingReceived !secureRing");  //tml*** new ring
				} else {
					Log.e("secureRing!");
				}
				if (!secureCEC) {
					//tml*** cec
					boolean autoHDMIauto = mPref.readBoolean("HDMIctrl_auto", true);
					boolean autoHDMItv = mPref.readBoolean("HDMIctrl_tv", true);
					boolean autoHDMIinput = mPref.readBoolean("HDMIctrl_input", true);
					if (autoHDMIauto && (autoHDMItv || autoHDMIinput)) {
						Thread hdmi = new Thread() {
							@Override
							public void run() {
								boolean autoHDMItv = mPref.readBoolean("HDMIctrl_tv", true);
								boolean autoHDMIinput = mPref.readBoolean("HDMIctrl_input", true);
								if (autoHDMItv) hdmiCmdExec("on");
								if (autoHDMIinput) hdmiCmdExec("switch");
							}
						};
						hdmi.start();
					}
				}
				Log.e("secure? " + secureRing + secureCEC);
				//***tml
//				startRinging();
			}
			else
			{
				//Callee is in call...
				mVoipCore.terminateCall(call);
				Log.d("exit.TelephonyManager in Call, terminate Call");
//				stopRingBack();
				stopRing();  //tml*** new ring
			}
			inCall = true;//alec
		}

		if (state == VoipCall.State.OutgoingInit //alec
				&& (DialerActivity.getDialer()!=null)) {
			if (!incomingChatroom && CallType != CALLTYPE_WEBCALL) {
//				startRingBack("OutgoingInit !incomingChatroom");
				prepareRing(true, 0, CallType, "OutgoingInit !incomingChatroom");  //tml*** new ring
			}
			
			inCall = true;//alec
			//wjx*** ledspeaker, outgoing no
//			needblink=true;
//			Thread t2=new Thread() {
//
//				@Override
//				public void run() {
//					Log.e("t2:::needblink==="+needblink);
//					while (needblink) {
//							try {
//								Thread.sleep(100);
//							} catch (InterruptedException e) {
//								e.printStackTrace();
//							}
//							LedSpeakerUtil.setLedOn();
//							try {
//								Thread.sleep(100);
//							} catch (InterruptedException e) {
//								e.printStackTrace();
//							}
//							LedSpeakerUtil.setLedOff();
//						}
//					LedSpeakerUtil.setLedOn();
//					}
//			};
//			t2.start();
			//***wjx
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
			Log.e("av4 " + e.getMessage());
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
			Log.e("av5 " + e.getMessage());
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
			Log.e("av6 " + e.getMessage());
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
//			enableDisableCodec("opus",48000,true); //opus
//			enableDisableCodec("opus",16000,false);
//			enableDisableCodec("opus",8000,false);

		}
		Log.d("voip.renableCodec " + getCallTypeName(CallType) + " V" + withVideo);
	}

	public void initFromConf() throws VoipConfigException, VoipException {

		String SipServer_default = mPref.read("mySipServer", mySipServer_China);
		if (AireJupiter.getInstance() != null)
			SipServer_default = AireJupiter.getInstance().getIsoSip();  //tml*** china ip
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
				Log.e("av8 " + e.getMessage());
			}
			
			
			if(mPref.readBoolean("usestanleysip"))
			{
				SipServer = mPref.read("sipserver4s", "74.3.165.175");
				lUserName = mPref.read("username4s", "6345077");
				password = mPref.read("password14s", "aire634507718!!sw");
			} else {
				lUserName = "**"+myIdx;
				password = mPref.read("password", _Password);
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
				Log.e("av9 " + e.getMessage());
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
		Log.e("voip.INIT/REGIS " + getCallTypeName(CallType) + CallType
				+ " " + lUserName + ":" + password + "@" + SipServer + " " + iso
				+ " /" + runAsSipAccount + "/" + runAsFileTransfer);
		mPref.write("lastRegisSip", getCallTypeName(CallType) + " " + SipServer);
		
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
//				enableDisableCodec("opus",48000,true); //opus
////				enableDisableCodec("speex",16000,true);
//				enableDisableCodec("H264",90000,false);
//				enableDisableCodec("PCMU",8000,true);
//				enableDisableCodec("PCMA",8000,true);
//				enableDisableCodec("AMR",8000,false);
			}
			else{
				Log.i("voip.CODECS > OTHER SipServer");
				enableDisableCodec("speex",16000,true);
				enableDisableCodec("H264",90000,false);
				enableDisableCodec("PCMU",8000,false);
				enableDisableCodec("PCMA",8000,false);
				enableDisableCodec("AMR",8000,false);
				enableDisableCodec("opus",16000,false);
//				enableDisableCodec("opus",48000,true); //opus
////				enableDisableCodec("speex",16000,true);
//				enableDisableCodec("H264",90000,false);
//				enableDisableCodec("PCMU",8000,false);
//				enableDisableCodec("PCMA",8000,false);
//				enableDisableCodec("AMR",8000,false);
			}
			
			if (AmazonKindle.IsKindle())
			{
				Log.i("voip.CODECS > KINDLE");
				enableDisableCodec("speex",16000,false);
				enableDisableCodec("PCMU",8000,true);
				enableDisableCodec("PCMA",8000,true);
				enableDisableCodec("opus",16000,false);
			}
			
//			enableDisableCodec("opus",8000,false);
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
					
					boolean usingHD720p=mPref.readBoolean("EnableHD", true);
					//tml|sw*** hd2/
					boolean usingHD1080p=mPref.readBoolean("EnableHD2", false);
					Log.i("hd AV 720/1080=" + Boolean.toString(usingHD720p) + Boolean.toString(usingHD1080p));
					
//					mVoipCore.setPreferredVideoSize(VideoSize.createStandard(usingHD720p?VideoSize.HD720p:VideoSize.VGA, false));
					//tml|sw*** hd2
					mVoipCore.setPreferredVideoSize(VideoSize.createStandard(
							(usingHD1080p?VideoSize.HD1080p
									:(usingHD720p?VideoSize.HD720p:VideoSize.SVGA)), false));  //SVGA or HD480p
					
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
		}
		else {
			mVoipCore.enableVideo(false, false);
			
			mPref.write("video_support", false);
		}

		//stun server
//		mVoipCore.setStunServer(null);  //TLS*** CX for tls?
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
		inCall = false;
		mPref.write("curCall", "");
		callstate_AV = null;
		destroying=true;
		mHandler.removeCallbacks(quitServiceY);
		mHandler.removeCallbacks(runAlive);
		if (AireJupiter.getInstance()!=null)
			AireJupiter.getInstance().StopEndingupServiceY();
		
		deregisterSip();//alec
		
		if (mTimer!=null)
			mTimer.cancel();
		mTimer=null;
		try{//alec, in case nativePtr==0
			if (mVoipCore!=null) {
				mVoipCore.terminateCall(mVoipCore.getCurrentCall());
				Log.d("exit.AireVenus destroying, terminate Call");
				mVoipCore.destroy();
			}
		}catch(RuntimeException e){
			Log.e("av10 " + e.getMessage());
		}
		catch(Exception e){
			Log.e("av11 " + e.getMessage());
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
	    mAudioManager.setMode(AudioManager.MODE_NORMAL);  //tml|sw*** audio break/
		//SoundPool.release();
		runAsSipAccount=false;
		runAsFileTransfer=false;
		destroying=false;
		Log.e("*** AIREVENUS/SERVICE-Y *** DESTROY DESTROY *** voip " + SettingPage.vlib);
		super.onDestroy();
	}
	
	public static VoipCore getLc() {
		if (instance()==null) return null;
		return instance().getVoipCore();
	}

	//tml*** new ring
	private AudioTrack mAudioTrack = null;
	private InputStream inS = null;
	private DataInputStream dinS = null;
	private volatile boolean ringrdy = false;
	public void prepareRing(boolean en, int mode, int calltype, String from) {
		if (en) {
			Log.e("RING." + mode + getCallTypeName(calltype) + " AV *** PREP! (" + from + ")");
			if (mode == 1) {  //incoming
				if (mAudioTrack == null) {
					if (!mPref.readBoolean("normal_ring", true)) {
						int iMinBufSize = AudioTrack.getMinBufferSize(16000, 
								AudioFormat.CHANNEL_CONFIGURATION_MONO, 
								AudioFormat.ENCODING_PCM_16BIT);
						mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, 
								AudioFormat.CHANNEL_CONFIGURATION_MONO, 
								AudioFormat.ENCODING_PCM_16BIT, 
								iMinBufSize, AudioTrack.MODE_STREAM);
					} else {
						int iMinBufSize = AudioTrack.getMinBufferSize(16000, 
								AudioFormat.CHANNEL_CONFIGURATION_MONO, 
								AudioFormat.ENCODING_PCM_16BIT);
						mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, 
								AudioFormat.CHANNEL_CONFIGURATION_MONO, 
								AudioFormat.ENCODING_PCM_16BIT, 
								iMinBufSize, AudioTrack.MODE_STREAM);
					}
					int voloption = mPref.readInt("incRingVolume", 2);
					if (mPref.readBoolean("voice_control", false)) {
						if (mPref.readBoolean("normal_ring", true)) {
							maxiVol(1, ((double) voloption / 3));
						} else {
							maxiVol(1, ((double) voloption / 3));  //lessen vol
						}
						if (!(AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM)) {
							mHandler.postDelayed(voiceRecogn, 500);
						} else {
//							mHandler.postDelayed(voiceRecogn, 1000);  //out CHATROOM
							mHandler.post(destroySpeechListen);
						}
					} else {
						maxiVol(1, ((double) voloption / 3));
					}
					controlBkgndMusic(0);
					StartRing startRing = new StartRing(mode, calltype, true);
					new Thread(startRing).start();
				}
			} else if (mode == 0) {  //outgoing
				if (mAudioTrack == null) {
					int iMinBufSize = AudioTrack.getMinBufferSize(16000, 
							AudioFormat.CHANNEL_CONFIGURATION_MONO, 
							AudioFormat.ENCODING_PCM_16BIT);
					mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, 
							AudioFormat.CHANNEL_CONFIGURATION_MONO, 
							AudioFormat.ENCODING_PCM_16BIT, 
							iMinBufSize, AudioTrack.MODE_STREAM);
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
				if (mPref.readBoolean("normal_ring", true)) {
					if (!mPref.readBoolean("voice_control", false)) {
//					if (!mPref.readBoolean("voice_control", false)
//							|| (mPref.readBoolean("voice_control", false)
//									&& AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM)) {
						ringfile = "incring3_1616.pcm";  //complete normal ring
					} else {
						ringfile = "incring4_1616.pcm";  //repeat
						if (first) {
							if (!(AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM)) {
								mHandler.postDelayed(runSpeechListen, 1500);
							} else {
//								mHandler.postDelayed(runSpeechListen, 2200);  //out CHATROOM
							}
						} else {
							mHandler.postDelayed(runSpeechListen, 1500);
						}
					}
				} else {
					String intx = Integer.toString(rng.nextInt(8) + 1);
					ringfile = "r16k_" + intx + ".raw";
				}
			} else {  //outgoing
				String intx = Integer.toString(rng.nextInt(8) + 1);
				ringfile = "r16k_" + intx + ".raw";
			}

			Log.e("RING." + ringfile + " *** DO! " + ringrdy + " first." + first);
			if (ringrdy && mAudioTrack != null) {
				AssetManager inAssets = getAssets();
				inS = inAssets.open(ringfile);
				dinS = new DataInputStream(inS);

				mAudioTrack.play();
				while(((i = dinS.read(audiobuff, 0, buffSize)) > -1)) {
					mAudioTrack.write(audiobuff, 0, i);
					if (!ringrdy || theVoip == null) break;
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
				if (mode == 1 && mPref.readBoolean("normal_ring", true)
						&& mPref.readBoolean("voice_control", false)) {
					if (ringrdy) {
						repeat = true;
						mHandler.postDelayed(runRepeatVC, 1500);
					}
				} else {
					if (ringrdy) {
						repeat = true;
						StartRing startRing = new StartRing(mode, CallType, false);
						new Thread(startRing).start();
					}
				}
			}
		} catch (Exception e) {
			Log.e("AV RING ERR " + e.getMessage());
			stopRing();
		} finally {
			if (!repeat) ringrdy = false;
		}
	}

	Runnable runRepeatVC = new Runnable () {
		@Override
		public void run() {
			maxiMic(0, 1);
			mHandler.post(stopSpeechListen);
//			new Thread(runREPlayRing).start();
			if (theVoip != null) {
				StartRing startRing = new StartRing(1, CallType, false);
				new Thread(startRing).start();
			}
		}
	};

	Runnable runREPlayRing = new Runnable() {
		@Override
		public void run() {
			Log.e("runREPlayRing");
			ringrdy = true;
			playRing(1, false);
		}
	};
	
	public void stopRing() {
		mHandler.removeCallbacks(voiceRecogn);
		mHandler.removeCallbacks(runSpeechListen);
		mHandler.removeCallbacks(runRepeatVC);
		mHandler.removeCallbacks(runREPlayRing);
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
			if (inS != null) inS.close();
			if (dinS != null) dinS.close();
//			Log.e("RING *** STOP!");
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
		if (divAM < 0) divAM = 1;
		if (mode == 1) {  //max vol
			prevVol1 = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			maxVol1 = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			returnVol = 1;
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (maxVol1 * divAM), 0);
			Log.d("maxiVol AV set." + mode + " " + divAM + "|" + prevVol1 + "|" + (maxVol1 * divAM));
		} else if (mode == 0 && returnVol == 1) {  //return vol
			maxVol1 = prevVol1;
			returnVol = 0;
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (maxVol1 * divAM), 0);
			Log.d("maxiVol AV return." + mode + " " + divAM + "|" + prevVol1);
		}
	}
	
	boolean musicWasActive = false;
	private void controlBkgndMusic (int mode) {
//		AudioManager mAudioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
		Intent imediaplay = new Intent("com.android.music.musicservicecommand");
		if (mode == 0 && mAudioManager.isMusicActive()) {
//			mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);  //also muting ring
//			imediaplay.putExtra("command", "pause");
//			sendBroadcast(imediaplay);
			musicWasActive = true;
			Log.d("controlBkgndMusic" + mode + musicWasActive + mAudioManager.isMusicActive());
		} else if (mode == 1 && musicWasActive) {
			mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
//			imediaplay.putExtra("command", "play");
//			sendBroadcast(imediaplay);
			Log.d("controlBkgndMusic" + mode + musicWasActive + mAudioManager.isMusicActive());
			musicWasActive = false;
		} else if (mode == 2) {
			mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
			Log.d("controlBkgndMusic" + mode + musicWasActive + mAudioManager.isMusicActive());
			musicWasActive = false;
		}
	}
	//***tml
	
//	int prevVol1, prevVol2;
//	private synchronized void startRinging_old(String from)  {
////		startRingBackSpeex("speex");  //yang*** speex player/
//		try {
//////			if (mVibrator !=null) {
//////				long[] patern = {0,1000,1000};
//////				mVibrator.vibrate(patern, 1);
//////			}
//////			if (mRingerPlayer == null) {
//////				mRingerPlayer = new MediaPlayer();
//////				try{
//////					mRingerPlayer.setAudioStreamType(AudioManager.STREAM_RING);
//////					mRingerPlayer.setDataSource(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
//////					mRingerPlayer.prepare();
//////					mRingerPlayer.setLooping(true);
//////					mRingerPlayer.start();
//////				}catch (Exception e){
//////					Log.e("av12 " + e.getMessage());
//////					mRingerPlayer=null;
//////				}
//////			} else {
//////				Log.w("already ringing");
//////			}
//			//tml|sw*** audio break
//			controlPlayMusic(0);
//			if (mAudioTrack == null && !ringrdy) {
//				Log.d("tml AV STARTringing ***** DO! (" + from + ")");
//				if (mPref.readBoolean("normal_ring", true)) {
//					int iMinBufSize = AudioTrack.getMinBufferSize(16000, 
//							AudioFormat.CHANNEL_CONFIGURATION_MONO, 
//							AudioFormat.ENCODING_PCM_16BIT);
//					mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
//							AudioFormat.CHANNEL_CONFIGURATION_MONO, 
//							AudioFormat.ENCODING_PCM_16BIT,
//							iMinBufSize, AudioTrack.MODE_STREAM);
//				} else {
//					int iMinBufSize = AudioTrack.getMinBufferSize(16000, 
//							AudioFormat.CHANNEL_CONFIGURATION_MONO, 
//							AudioFormat.ENCODING_PCM_16BIT);
//					mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
//							AudioFormat.CHANNEL_CONFIGURATION_MONO, 
//							AudioFormat.ENCODING_PCM_16BIT,
//							iMinBufSize, AudioTrack.MODE_STREAM);
//				}
//				float maxVol0 = AudioTrack.getMaxVolume();
//				mAudioTrack.setStereoVolume(maxVol0, maxVol0);
//				int maxVol1 = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//				prevVol1 = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//				Log.e("tml previousVolI=" + prevVol1);
//				if (mPref.readBoolean("voice_control", false)) {
//					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol1/2, 0);
//				} else {
//					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol1, 0);
//				}
//				int nowVol1 = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//				Log.e("tml nowVolI=" + nowVol1);
//				mHandler.post(new Runnable () {
//					@Override
//		            public void run() {
//						ringrdy = true;
//						ringTask = new MyRingerTask(0);
//						ringTask.execute();
//					}
//				});
//			} else {
//				Log.w("already ringing");
//			}
//			//***tml
//		} catch (Exception e) {
//			Log.e("cannot handle incoming call " + e.getMessage());
//			//tml|sw*** audio break
//			mAudioTrack = null;
//			ringrdy = false;
//			//***tml
//		}
//	}
//	
//	public synchronized void stopRinging_old() {
////		if(rwb!=null){  //yang*** speex player
////			rwb.stop();
////			rwb.release();
////		}
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
//				mAudioTrack.flush();
//				mAudioTrack.stop();
//				mAudioTrack.release();
//				mAudioTrack = null;
//				if (inS != null) {
//					inS.close();
//				}
//				if (dinS != null) {
//					dinS.close();
//				}
//				inS = null;
//				dinS = null;
//				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, prevVol1, 0);
//				prevVol1 = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//				Log.e("tml resumeVol=" + prevVol1);
//				Log.d("tml AV STOPring ***** SUCCESS!");
//			} catch (IOException e) {
//				Log.e("av stopRinging " + e.getMessage());
//			}
//		}
//		//***tml
////		if (mVibrator!=null) {
////			mVibrator.cancel();
////		}
//	}
//	
//	public synchronized void startRingBack_old(String from)  {
////		startRingBackSpeex("speex");  //yang*** speex player/
//		try {
//////			if (mRingerPlayer == null) {
//////				try{
//////					Random r = new Random();
//////					mRingerPlayer = new MediaPlayer();
//////					mRingerPlayer = MediaPlayer.create(this, R.raw.ringback1+r.nextInt(7));
//////					mRingerPlayer.setLooping(true);
//////					mRingerPlayer.start();
//////					
//////				    //sp=SoundPool.play(r.nextInt(4)+1, 1, 1, 0, -1, 1);
//////				}catch(Exception e) {
//////					Log.e("av13 " + e.getMessage());
//////					mRingerPlayer=null;
//////				}
//////			}
//			//tml|sw*** audio break
//			controlPlayMusic(0);
//			if (mAudioTrack == null && !ringrdy) {
//				Log.d("tml AV STARTringback ***** DO! (" + from + ")");
////				if (mPref.readBoolean("normal_ring", true)) {
////					int iMinBufSize = AudioTrack.getMinBufferSize(8000, 
////							AudioFormat.CHANNEL_CONFIGURATION_MONO, 
////							AudioFormat.ENCODING_PCM_8BIT);
////					mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
////							AudioFormat.CHANNEL_CONFIGURATION_MONO, 
////							AudioFormat.ENCODING_PCM_8BIT,
////							iMinBufSize, AudioTrack.MODE_STREAM);
////				} else {
//					int iMinBufSize = AudioTrack.getMinBufferSize(16000, 
//							AudioFormat.CHANNEL_CONFIGURATION_MONO, 
//							AudioFormat.ENCODING_PCM_16BIT);
//					mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
//							AudioFormat.CHANNEL_CONFIGURATION_MONO, 
//							AudioFormat.ENCODING_PCM_16BIT,
//							iMinBufSize, AudioTrack.MODE_STREAM);
////				}
//				float maxVol0 = AudioTrack.getMaxVolume();
//				mAudioTrack.setStereoVolume(maxVol0, maxVol0);
//				int maxVol1 = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2;
//				prevVol1 = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//				Log.e("tml previousVolO=" + prevVol1);
//				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol1, 0);
//				int nowVol1 = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//				Log.e("tml nowVolO=" + nowVol1);
//				mHandler.post(new Runnable () {
//					@Override
//		            public void run() {
//						ringrdy = true;
//						ringTask = new MyRingerTask(1);
//						ringTask.execute();
//					}
//				});
//			}
//			//***tml
//		} catch (Exception e) {
//			Log.e("av14 " + e.getMessage());
//			//tml|sw*** audio break
//			mAudioTrack = null;
//			ringrdy = false;
//			//***tml
//		}
//	}
//	
//	private synchronized void stopRingBack_old() {
////		if(rwb!=null){  //yang*** speex player
////			rwb.stop();
////			rwb.release();
////		}
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
//				Log.d("tml AV STOPringB ***** DO!");
//				ringTask.cancel(true);
//				ringrdy = false;
//				mAudioTrack.flush();
//				mAudioTrack.stop();
//				mAudioTrack.release();
//				mAudioTrack = null;
//				if (inS != null) {
//					inS.close();
//				}
//				if (dinS != null) {
//					dinS.close();
//				}
//				inS = null;
//				dinS = null;
//				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, prevVol1, 0);
//				prevVol1 = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//				Log.e("tml resumeVol=" + prevVol1);
//				Log.d("tml AV STOPringB ***** SUCCESS!");
//			} catch (IOException e) {
//				Log.e("av stopRingB " + e.getMessage());
//			}
//		}
//		//***tml
//	}
//	
//	//tml|sw*** audio break, new ringer code, so ui doesnt freeze
//	private MyRingerTask ringTask;
////	private AudioTrack mAudioTrack = null;
////	private InputStream inS = null;
////	private DataInputStream dinS = null;
////	private volatile boolean ringrdy = false;
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
//				String musicfile;
//				if (mPref.readBoolean("normal_ring", true) && ringmode == 0) {
//					musicfile = "incring3_1616.pcm";
//				} else {
//					Random rng = new Random();
//					String intx = Integer.toString(rng.nextInt(8) + 1);
//					musicfile = "r16k_" + intx + ".raw";
//				}
//				Log.i("tml ringfile> " + musicfile);
//
//				boolean fdatardy = false;
////				while (ringrdy) {
//				if (ringrdy) {
////					if (!fdatardy) {
//						inS = am.open(musicfile);
//						dinS = new DataInputStream(inS);
////						fdatardy = true;
////					}
//					mAudioTrack.play();
//					while(((i = dinS.read(audiobuff, 0, bufferSize)) != -1)) {
//						mAudioTrack.write(audiobuff, 0, i);
//						if (ringTask.isCancelled()) {
////							mAudioTrack.pause();
////							mAudioTrack.flush();
////							mAudioTrack.stop();
////							mAudioTrack.release();
//							Log.e("tml AV ring.mAudioTrack mid-cleared0!");
//							break;
//						}
//				    }
//				}
////				mAudioTrack.stop();
////				mAudioTrack.flush();
////				mAudioTrack.release();
////				mAudioTrack = null;
////				inS.close();
////				dinS.close();
////				inS = null;
////				dinS = null;
//				Log.d("tml AV MyRingerTask END");
//    		} catch (Exception e) {
//				Log.e("tml AV MyRingerTask ERR " + e.getMessage());
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
//		
//		@Override
//		public void onPostExecute(Void result) {
//		}
//	}
//
////	boolean musicWasActive = false;
//	public void controlPlayMusic (int mode) {
////		Intent imediaplay = new Intent(Intent.ACTION_MEDIA_BUTTON);
////		if (mode == 0 && mAudioManager.isMusicActive()) {
////			imediaplay.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE));
////			sendOrderedBroadcast(imediaplay, null);
////			imediaplay.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE));
////			sendOrderedBroadcast(imediaplay, null);
////			musicWasActive = true;
////		} else if (mode == 1 && musicWasActive) {
////			imediaplay.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
////			sendOrderedBroadcast(imediaplay, null);
////			imediaplay.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY));
////			sendOrderedBroadcast(imediaplay, null);
////			musicWasActive = false;
////		}
//		Intent imediaplay = new Intent("com.android.music.musicservicecommand");
//		if (mode == 0 && mAudioManager.isMusicActive()) {
//			imediaplay.putExtra("command", "pause");
//			sendBroadcast(imediaplay, null);
//			musicWasActive = true;
//		} else if (mode == 1 && musicWasActive) {
//			imediaplay.putExtra("command", "play");
//			sendBroadcast(imediaplay, null);
//			musicWasActive = false;
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
	
	private KeyguardManager myKM=null;
	private boolean checkScreenLocked()
	{
		if (myKM==null) myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		if (myKM.inKeyguardRestrictedInputMode())
		 	return true;
		return false;
	}
	
	private ActivityManager mAm;
	private boolean isAireCenterRunning()
	{
		if (checkScreenLocked()) return true;
		mAm = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

		List< ActivityManager.RunningTaskInfo > taskInfo = mAm.getRunningTasks(30);
		String name=taskInfo.get(0).topActivity.getClassName();
		Log.d("Top Activity:" + name);
		
		if (name.startsWith("com.pingshow.airecenter"))
			return true;
		
		for (ActivityManager.RunningTaskInfo task:taskInfo)
		{
			if (task.topActivity.getClassName().startsWith("com.pingshow.airecenter"))
			{
				if (MainActivity._this!=null)
					MainActivity._this.close();
				else if (SecurityNewActivity._this!=null)
					SecurityNewActivity._this.close();
				else if (LocationSettingActivity._this!=null)
					LocationSettingActivity._this.close();
				else if (ShoppingActivity._this!=null)
					ShoppingActivity._this.close();
				//tml*** browser save
				else if (MainBrowser._this!=null)
					MainBrowser._this.close();
				//***tml
				
				break;
			}
		}
		
		return false;
	}
	
	//tml*** voice control
	Runnable voiceRecogn = new Runnable() {
		@Override
		public void run() {
			if (DialerActivity.getDialer() != null) {
				DialerActivity.getDialer().voiceRecogn();
			} else if (RingDialog.getInstance() != null) {
				RingDialog.getInstance().voiceRecogn();
			}
		}
	};

	Runnable runSpeechListen = new Runnable() {
		@Override
		public void run() {
			if (DialerActivity.getDialer() != null) {
				maxiMic(1, 1);
				DialerActivity.getDialer().startSpeechListen("start");
			} else if (RingDialog.getInstance() != null) {
				maxiMic(1, 1);
				RingDialog.getInstance().startSpeechListen("start");
			}
		}
	};

	Runnable stopSpeechListen = new Runnable() {
		@Override
		public void run() {
			if (DialerActivity.getDialer() != null) {
				maxiMic(0, 1);
				DialerActivity.getDialer().startSpeechListen("stop");
			} else if (RingDialog.getInstance() != null) {
				maxiMic(0, 1);
				RingDialog.getInstance().startSpeechListen("stop");
			}
		}
	};

	Runnable destroySpeechListen = new Runnable() {
		@Override
		public void run() {
			if (mPref.readBoolean("voice_control", false)) {
				Log.e("DO destroying AV voiceRecogn");
				mHandler.removeCallbacks(voiceRecogn);
				mHandler.removeCallbacks(runSpeechListen);
				mHandler.removeCallbacks(stopSpeechListen);
				mHandler.removeCallbacks(runRepeatVC);
				mHandler.removeCallbacks(runREPlayRing);
				if (DialerActivity.getDialer() != null) {
					maxiMic(0, 1);
					DialerActivity.getDialer().startSpeechListen("destroy");
				} else if (RingDialog.getInstance() != null) {
					maxiMic(0, 1);
					RingDialog.getInstance().startSpeechListen("destroy");
				}
			}
		}
	};

	private int prevMic1, returnMic = 0;
	private void maxiMic(int mode, double divAM) {
		int maxMic1 = 1, setMic;
		if (divAM < 0) divAM = 1;
		if (mode == 1) {  //max vol
			prevMic1 = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
			maxMic1 = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
			if ((int) (maxMic1 * divAM) < prevMic1) {
				setMic = prevMic1;
			} else {
				setMic = (int) (maxMic1 * divAM);
			}
			mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, setMic, 0);
			returnMic = 1;
			Log.d("maxiMic AV set." + mode + " " + divAM + "|" + prevMic1 + "|" + setMic);
		} else if (mode == 0 && returnMic == 1) {  //return mic
			maxMic1 = prevMic1;
			returnMic = 0;
			setMic = (int) (maxMic1 * divAM);
			mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, setMic, 0);
			returnMic = 0;
			Log.d("maxiMic AV return." + mode + " " + divAM + "|" + prevMic1);
		}
	}
	//***tml
	//tml*** cec
	public void hdmiCmdExec(String param) {
		if (AireJupiter.getInstance() != null && param != null) {
			AireJupiter.hdmiCmdExec(param);
		}
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
	//tml test
	private void displayCallStatus (String s) {
		if (AireJupiter.getInstance() != null)
			AireJupiter.getInstance().updateCallDebugStatus(false, "\n" + s);
	}
}

