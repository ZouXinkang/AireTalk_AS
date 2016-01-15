
package com.pingshow.voip;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.PlayService;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.SMS;
import com.pingshow.airecenter.UserPage;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.SmsDB;
import com.pingshow.airecenter.view.ProgressBar;
import com.pingshow.network.NetInfo;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.core.VoipAddress;
import com.pingshow.voip.core.VoipCall;
import com.pingshow.voip.core.VoipCall.State;
import com.pingshow.voip.core.VoipCore;
import com.pingshow.voip.core.VoipCoreException;
import com.pingshow.voip.core.VoipCoreListener;
import com.pingshow.voip.core.VoipProxyConfig;

public class FileTransferActivity extends Activity implements VoipCoreListener{
	
	private String mAddress="";
	private TextView mDisplayNameView;
	private ImageView mProfileImage;
	private ProgressBar mProgressBar;

	private TextView mStatus;
	private Button mHangup;
	private Button mAnswer;
	
	private boolean created=false;
	private boolean mComplete=false;
	
	private static FileTransferActivity theDialer;
	    
	private String mDisplayName=null;
	private PowerManager.WakeLock mWakeLock;
	private MyPreference mPref;
	private long contact_id;
	
	private AmpUserDB mADB;
	public boolean imCalling = false;
	private Vibrator mVibrator;
		
	private String mTransferedFilename=null;
	private static boolean Connected;
	public static boolean incomingCall;
	private long startTime;
	int mProgress;
	
	private String phoneNumber;
	private ContactsQuery cq;
	
	private boolean sendTerminateSignal=true;

	public boolean HangingUp=false;
	private Handler handler = new Handler(){
		@SuppressWarnings("unchecked")
		public void handleMessage(android.os.Message msg) {
			try{
				switch(msg.what)
				{
				case 1:
					TextView textview = (TextView)((HashMap<String, Object>)msg.obj).get("textview");
					String text = (String)((HashMap<String, Object>)msg.obj).get("text");
					textview.setText(text);
					break;
				case 2:
					ImageView imageview = (ImageView)((HashMap<String, Object>)msg.obj).get("imageview");
					Bitmap bm = (Bitmap)((HashMap<String, Object>)msg.obj).get("image");
					imageview.setImageBitmap(bm);
					break;
				case 3:
					Button btn = (Button)msg.obj;
					btn.setVisibility(msg.arg1);
					break;
				case 4:
					LinearLayout linear = (LinearLayout)msg.obj;
					linear.setVisibility(msg.arg1);
					linear.bringToFront();
					break;
				}
			}catch(Exception e){}
		};
	};
	
	public static FileTransferActivity getDialer() { 
		return theDialer;
	}

	protected static FileTransferActivity instance()
	{
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
		
		this.overridePendingTransition(R.anim.appear, R.anim.disappear);
		
		WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.5f;
		getWindow().setAttributes(lp);
		
		mPref = new MyPreference(FileTransferActivity.this);
		
		lockScreenOrientation();
		theDialer=this;

		mHandler.postDelayed(startDialerStuff, 500);
		
		try{
			new File("/mnt/sdcard/Download/").mkdir();
		}catch(Exception e){}
	}
	
	Runnable startDialerStuff=new Runnable(){
		public void run()
		{
			setContentView(R.layout.file_transfer);
			Connected=false;
			
			cq=new ContactsQuery(FileTransferActivity.this);
			mADB=new AmpUserDB(FileTransferActivity.this);
			mADB.open();
			
			mAddress="";
			mDisplayNameView = (TextView) findViewById(R.id.displayname);
			
		    contact_id=getIntent().getLongExtra("Contact_id",0);
			mProfileImage = (ImageView)findViewById(R.id.bighead);
			
			mHangup = (Button) findViewById(R.id.hangup); 
			mHangup.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					hangup();
				}
			});
			
			((ImageView) findViewById(R.id.close)).setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					hangup();
				}
			});
			
			mProgressBar=(ProgressBar)findViewById(R.id.progressbar);
			
			mAnswer=(Button)findViewById(R.id.answer);
			if (mAnswer!=null)
			{
				mAnswer.setOnClickListener(new OnClickListener(){
					public void onClick(View v) {
					    if (AireVenus.instance()!=null)
					    {
							VoipCore lVoipCore = AireVenus.instance().getVoipCore();
							if (lVoipCore.isInComingInvitePending()) {
								try {
									VoipCall vc=lVoipCore.getCurrentCall();
									lVoipCore.acceptCall(vc);
								} catch (VoipCoreException e) {
									lVoipCore = AireVenus.instance().getVoipCore();
									VoipCall myCall = lVoipCore.getCurrentCall();
									if (myCall != null) {
										lVoipCore.terminateCall(myCall);
									}
								}
								return;
							} else {	// invite is not pending, due to poor network condition
								lVoipCore = AireVenus.instance().getVoipCore();
								VoipCall myCall = lVoipCore.getCurrentCall();
								if (myCall != null) {
									lVoipCore.terminateCall(myCall);
								}
							}
					    }
					}
				});
			}
	     	
			PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
			if (mWakeLock == null) {
				mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
						PowerManager.ACQUIRE_CAUSES_WAKEUP, "FafaYou.InCall");
			}
			
			mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			mStatus = (TextView) findViewById(R.id.status_label);
			
			Intent intent=getIntent();
			phoneNumber=intent.getStringExtra("PhoneNumber");
			if (phoneNumber!=null)
			{
				phoneNumber=MyTelephony.attachPrefix(FileTransferActivity.this, phoneNumber);
				mAddress=phoneNumber;
			}
			
			Bitmap photo = getUserPhoto(mAddress);
			if (photo!=null) {
				mProfileImage.setImageBitmap(photo);
			}
		
			incomingCall=getIntent().getBooleanExtra("incomingCall", false);
			
			if (incomingCall)
			{
				mAnswer.setVisibility(View.VISIBLE);
				displayStatus(null,getString(R.string.file_transfer_incoming));
			}else{
				mHangup.setVisibility(View.VISIBLE);
				mAnswer.setVisibility(View.GONE);
				displayStatus(null,getString(R.string.file_transfer_waiting));
				
				mADB.updateLastContactTimeByAddress(phoneNumber, new Date().getTime());
				if (UserPage.sortMethod==1)
					UserPage.forceRefresh=true;
			}
			
			mDisplayName=intent.getStringExtra("DisplayName");
			if (mDisplayName!=null)
			{ 
				if (contact_id>0)
				{
					if (mDisplayName.length()>0)
						mDisplayName=cq.getNameByContactId(contact_id)+" ("+mDisplayName+")";
					else
						mDisplayName=cq.getNameByContactId(contact_id);
				}
				else if (mDisplayName.length()==0)
					mDisplayName="";
				
				mDisplayNameView.setText(mDisplayName);
			}
			else{
				if (phoneNumber!=null)
				{
					mDisplayName=mADB.getNicknameByAddress(phoneNumber);
					if (mDisplayName!=null) mDisplayNameView.setText(mDisplayName);
				}
			}
			
			if (AireJupiter.getInstance()!=null)
				AireJupiter.getInstance().StopEndingupServiceY();//alec
			
			created=true;
			
			new Thread(dialerStuff).start();
		}
	};
	
	private Bitmap getUserPhoto(String address)
	{
		if(!MyUtil.checkSDCard(getApplicationContext())) return null;
		if(address.length()==0) return null;
		
		int idx=mADB.getIdxByAddress(address);
		if (idx>0)
		{
//			String path=Global.SdcardPath_inbox+"photo_"+idx+".jpg";
//			return ImageUtil.getBitmapAsRoundCorner(1, 5, path);
			//tml*** userphoto fix
			String path = Global.SdcardPath_inbox + "photo_" + idx + "b.jpg";
			Bitmap bmp = ImageUtil.getBitmapAsRoundCorner(1, 5, path);
			if (bmp != null) return bmp;
			
			path = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
			bmp = ImageUtil.getBitmapAsRoundCorner(1, 5, path);
			if (bmp != null) return bmp;
		}
		return null;
	}
	
	Runnable dialerStuff=new Runnable(){
		public void run()
		{
			Log.d("wait for registered");
			int xcount=0;
			while ((AireVenus.instance()==null || !AireVenus.isready() || !AireVenus.instance().registered) && ++xcount<100)//alec
				MyUtil.Sleep(100);
			
			Log.d("wait for registered...done");
			
			try {
				
				if (AireVenus.isready()) {
					VoipCore lVoipCore = AireVenus.instance().getVoipCore();
					if (lVoipCore.isIncall() && incomingCall)
					{
						String IncomingNumber=lVoipCore.getRemoteAddress().getUserName();
						if (IncomingNumber!=null && IncomingNumber.length()>0)//alec
						{
							mAddress=IncomingNumber;//alec
							//alec
							int remote=ContactsOnline.getContactOnlineStatus(mAddress);
							if (remote==0) remote=1;
							Log.d("**** Net Type: REMOTE: "+remote+" ***");
							lVoipCore.setNetType(new NetInfo(FileTransferActivity.this).netType, remote);
							
							contact_id=cq.getContactIdByNumber(IncomingNumber);
							mDisplayName=mADB.getNicknameByAddress(mAddress);
							if (contact_id>0)
							{
								if (mDisplayName.length()>0)
									mDisplayName=cq.getNameByContactId(contact_id)+" ("+mDisplayName+")";
							}
							updateTextView(mDisplayNameView, mDisplayName);
							mProfileImage = (ImageView)findViewById(R.id.bighead);
							Bitmap photo = getUserPhoto(mAddress);
							
							updateImageView(mProfileImage, photo);
							
							if(lVoipCore.isInComingInvitePending()) 
							{
								callPending(lVoipCore.getCurrentCall());
								mADB.updateLastContactTimeByAddress(IncomingNumber, new Date().getTime());
								if (UserPage.sortMethod==1)
									UserPage.forceRefresh=true;
								
								incomingCall=true;
								
								updateButtonVisible(mAnswer, View.VISIBLE);
								
								displayStatus(null,getString(R.string.file_transfer_incoming));
							}
						}
						else{
							Log.e("Incoming call is null");
							finish();
						}
					}
					else if (!incomingCall)
					{
						updateButtonVisible(mAnswer, View.GONE);
						updateButtonVisible(mHangup, View.VISIBLE);
						
						if (phoneNumber.equals(AireJupiter.myPhoneNumber))
						{
							finish();
							return;
						}
						
						//alec
						int remote=0;
						String addrTo=mAddress;
						remote=ContactsOnline.getContactOnlineStatus(mAddress);
						addrTo=getYourSipServerByTCP(mAddress);
						
						lVoipCore.setNetType(new NetInfo(FileTransferActivity.this).netType, remote);
						if (addrTo.contains("@nonmember")) 
							finish();
						else if (!HangingUp)
						{
							if (AireJupiter.getInstance()!=null)
							{
								Log.d("wait for calleeGotCallRequest....");
								
								int c=0;
								while(!AireJupiter.getInstance().calleeGotCallRequest && ++c<100)
									MyUtil.Sleep(100);
								
								Log.d("wait for calleeGotCallRequest....Done");
							}
							
							AireVenus.instance().enableDisableCodec("speex",16000,false);
							AireVenus.instance().enableDisableCodec("AMR",8000,false);
							AireVenus.instance().enableDisableCodec("x-udpftp",1000,true);
							String path=mPref.read("FileTransferP2P");
							if (path!=null)
							{
								lVoipCore.setOutgoingFilePath(path);
								try{
									File file = new File(path);
									String shortName=path.substring(path.lastIndexOf("/")+1);
									shortName=shortName+" ("+file.length()/1024+"KB)";
									updateTextView(((TextView)findViewById(R.id.file_info)), shortName);
									storeSmsRecord(2, path, shortName);
								}catch(Exception e){}
							}
							
							if (!HangingUp) 
							{
								Log.d("wait for 2000....");
								MyUtil.Sleep(2000);
								Log.d("wait for 2000....Done");
							}
							if (!HangingUp) newOutgoingCall(addrTo, false);
						}
					}
					
					mWakeLock.acquire();
				}
			} catch (Exception e) {
				Log.e("Fail to start dialer");
				finish();
			}
		}
	};
	//zhao
	private void updateTextView(TextView view, String text)
	{
		HashMap<String, Object>map = new HashMap<String, Object>();
		map.put("textview", view);
		map.put("text", text);
		Message msg = new Message();
		msg.obj = map;
		msg.what = 1;
		handler.sendMessage(msg);
	}
	private void updateImageView(ImageView imageview, Bitmap bitmap)
	{
		if (bitmap==null) return;
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("imageview", imageview);
		map.put("image", bitmap);
		Message msg = new Message();
		msg.obj = map;
		msg.what = 2;
		handler.sendMessage(msg);
	}
	private void updateButtonVisible(Button button,int visible)
	{
		Message msg = new Message();
		msg.obj = button;
		msg.arg1 = visible;
		msg.what = 3;
		handler.sendMessage(msg);
	}
	
	@Override
	public void onBackPressed() {
		if (AireVenus.instance()!=null)
		{
			VoipCore lVoipCore = AireVenus.instance().getVoipCore();
			if (lVoipCore!=null)
			{
				VoipCall myCall = lVoipCore.getCurrentCall();
				if (myCall != null)
				{
					lVoipCore.terminateCall(myCall);
					return;
				}
			}
		}
		exitCallMode();
		super.onBackPressed();
	}
	
	@Override
	protected void onDestroy() {
		mHandler.removeCallbacks(startDialerStuff);
		mHandler.removeCallbacks(displayP2P);
		
		theDialer=null;
		if (created)
		{
			created=false;
			
			try{
				if (mWakeLock!=null) mWakeLock.release();
				if (mVibrator!=null) mVibrator.cancel();
			}catch (Exception e){}
			
			mADB.close();
			Connected = false;
		}
		
		System.gc();
		System.gc();
		
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (created)
		{
			if (Connected || !incomingCall)
			{
				mAnswer.setVisibility(View.GONE);
				mHangup.setVisibility(View.VISIBLE);
			}
			else if (incomingCall){
				mAnswer.setVisibility(View.VISIBLE);
			}
		}
		
		disableKeyguard();
		
//		MobclickAgent.onResume(this);
	}
	
	@Override
	protected void onPause() {
		
		reenableKeyguard();
		
		if (created)
		{
			if  (isFinishing())  {
				//restore audio settings
				
				Log.i("broadcast CALL END...");
				Intent it = new Intent(Global.Action_InternalCMD);
				it.putExtra("Command", Global.CMD_CALL_END);
				it.putExtra("immediately", 60000);
				
				if (AireJupiter.getInstance()!=null)
					AireJupiter.getInstance().attemptCall=false;
				
				this.sendBroadcast(it);
				
				AireVenus.instance().enableDisableCodec("AMR",8000,true);
				
				if (sendTerminateSignal)
				{
					if (mAddress.length()>0 && AireJupiter.getInstance()!=null)
					{
						AireJupiter.getInstance().terminateCallBySocket(mAddress);
					}
				}
			}
//			MobclickAgent.onPause(this);
		}
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
	
	String mMsg="";
	
	Runnable run_disp_msg= new Runnable(){
		public void run() {
			try{
				if (theDialer!=null)
					theDialer.mStatus.setText(mMsg);
			}catch(NullPointerException e) {}
		}
	};
		
	public void displayStatus(VoipCore p, String message) {
		if (theDialer!=null)
		{
			mMsg=message;
			mHandler.post(run_disp_msg);
		}
	}
	
	private Handler mHandler=new Handler();
	
	private Runnable timeElapsed = new Runnable() {
		@Override
		public void run() {
			if (Connected)
			{
				long sec=(new Date().getTime()-startTime)/1000;
				VoipCore lVoipCore=null;
				//displayStatus(null, DateUtils.formatElapsedTime(sec));
				try{
					if (AireVenus.instance()==null) return;
					lVoipCore = AireVenus.instance().getVoipCore();
					if (lVoipCore==null) return;
					mProgress=lVoipCore.getFileTransferProgress();
					displayStatus(null, DateUtils.formatElapsedTime(sec)+" / "+mProgress+"%");
					
					mProgressBar.setProgress((float)mProgress/100.f);
				}catch(Exception e){}
				
				if (incomingCall && lVoipCore!=null)
				{
					if (mTransferedFilename==null || (mTransferedFilename!=null && mTransferedFilename.length()==0))
					{
						String filename=lVoipCore.getTransferFilename();
						if (filename!=null) 
						{
							mTransferedFilename=filename;
							filename=filename.substring(filename.lastIndexOf("/")+1);
							updateTextView(((TextView)findViewById(R.id.file_info)), filename);
						}
					}
					
					if (mProgress==100)
					{
						mComplete=true;
						mHandler.postDelayed(new Runnable()
						{
							public void run(){
								if (AireVenus.instance() != null) {
									VoipCore lVoipCore = AireVenus.instance().getVoipCore();
									VoipCall myCall = lVoipCore.getCurrentCall();
									if (myCall != null) {
										lVoipCore.terminateCall(myCall);
									}
								}
							}
						},2200);	
					}
				}
				
				mHandler.postDelayed(timeElapsed, 1000);
			}
		}
	};
	
	public void displayWarning(VoipCore p, String message) {
	}
	public void globalState(VoipCore p, VoipCore.GlobalState state, String message) {
	}
	public void registrationState(final VoipCore p, final VoipProxyConfig cfg,final VoipCore.RegistrationState state,final String smessage) {
	};
	
	/*
	 * SIMON 030111
	 * callStates returned from Voipcore.c are
	 *         VoipCallIdle,
     *  VoipCallIncomingReceived, 	//<This is a new incoming call
     *  VoipCallOutgoingInit, 				//<An outgoing call is started 
      * VoipCallOutgoingProgress, 	//<An outgoing call is in progress 
      * VoipCallOutgoingRinging, 		//<An outgoing call is ringing at remote end 
        VoipCallOutgoingEarlyMedia, //<An outgoing call is proposed early media 
        VoipCallConnected, 				//<Connected, the call is answered 
        VoipCallStreamsRunning, 		//<The media streams are established and running
        VoipCallPausing, 						//<The call is pausing at the initiative of local end 
        VoipCallPaused, 						//< The call is paused, remote end has accepted the pause 
        VoipCallResuming, 					//<The call is being resumed by local end
        VoipCallRefered, 						//<The call is being transfered to another party, resulting in a new outgoing call to follow immediately
        VoipCallError, 							//<The call encountered an error
        VoipCallEnd,	 						//<The call ended normally
        VoipCallPausedByRemote, 		//<The call is paused by remote end
        VoipCallUpdatedByRemote, 	//<The call's parameters are updated, used for example when video is asked by remote 
        VoipCallIncomingEarlyMedia, //<We are proposing early media to an incoming call 
        VoipCallUpdated 						//<The remote accepted the call update initiated by us 

	 */
	
	public void callState(final VoipCore p,final VoipCall call, final State state, final String message) {
		if (state == VoipCall.State.OutgoingInit) {
			displayStatus(null,getString(R.string.file_transfer_waiting));
			enterCallMode(p);
			//alec:routeAudioToReceiver();
			//SIMON 030211
		} else if (state == VoipCall.State.OutgoingRinging) { 
			//SIMON resetCameraFromPreferences();
			displayStatus(null,getString(R.string.file_transfer_waiting));
		} else if (state == VoipCall.State.IncomingReceived) {
			displayStatus(null,getString(R.string.file_transfer_incoming));
			callPending(call);
			updateButtonVisible(mAnswer, View.VISIBLE);
		} else if (state == VoipCall.State.Connected) {
			displayStatus(null,getString(R.string.file_transfer_connected));
			enterCallMode(p);
			startTime=new Date().getTime();//alec
			Connected=true;
			mHandler.postDelayed(timeElapsed, 1000);
			if (AireJupiter.getInstance()!=null)
				AireJupiter.getInstance().attemptCall=false;//alec
		} else if (state == VoipCall.State.Error) {
			if (message.startsWith("Not found"))
			{
				if (!Connected)
				{
					displayStatus(null,getString(R.string.file_transfer_error));
					playSound();
				}
			}else if (message.startsWith("No response") || message.startsWith("Request Timeout"))
			{
				if (!Connected)
				{
					displayStatus(null,getString(R.string.try_again));
					playSound();
				}
			}else
				displayStatus(null,getString(R.string.file_transfer_error));

			if (!Connected)
			{
				if (AireVenus.instance() != null) {
					VoipCall myCall = p.getCurrentCall();
					if (myCall != null) {
						p.terminateCall(myCall);
					}
				}
				exitCallMode();
			}
		} else if (state == VoipCall.State.CallEnd) {
			if (message.startsWith("Call declined"))
			{
				displayStatus(null,getString(R.string.call_declined));
				playSound();
			}else
				displayStatus(null,mProgress==100?getString(R.string.file_transfer_finished):getString(R.string.file_transfer_stop));
			
			if (Connected)
			{
				long[] patern = {0,40,1000};
				mVibrator.vibrate(patern, -1);
			}
			exitCallMode();
		} else if (state == VoipCall.State.StreamsRunning) {
			
			if (mPref.read("moodcontent", "--").endsWith("!!!!"))
			{
				mHandler.postDelayed(displayP2P, 1000);
			}
			mHandler.post(new Runnable(){
				public void run()
				{
					mProgressBar.setVisibility(View.VISIBLE);
				}
			});
		}
	}
	
	void playSound()
	{
		if (!MyUtil.CheckServiceExists(FileTransferActivity.this, "com.pingshow.airecenter.PlayService")) {
			Intent intent1 = new Intent(FileTransferActivity.this, PlayService.class);
			intent1.putExtra("soundInCall", R.raw.termin);
			startService(intent1);
		}
	}
	
	public void show(VoipCore p) {
		// TODO Auto-generated method stub
	}
	
	private void enterCallMode(VoipCore p) 
	{
		mAnswer.setVisibility(View.GONE);
		mHangup.setVisibility(View.VISIBLE);
	}
	
	//alec
	public void responseCallBusy()
	{
		HangingUp=true;
		
		displayStatus(null,getString(R.string.call_declined));

		if (!Connected)
		{
			if (AireVenus.instance() != null) {
				VoipCore lVoipCore = AireVenus.instance().getVoipCore();
				VoipCall myCall = lVoipCore.getCurrentCall();
				if (myCall != null) {
					lVoipCore.terminateCall(myCall);
				}
			}
		}
		
		if (!MyUtil.CheckServiceExists(FileTransferActivity.this, "com.pingshow.airecenter.PlayService")) {
			Intent intent1 = new Intent(FileTransferActivity.this, PlayService.class);
			intent1.putExtra("soundInCall", R.raw.termin);
			startService(intent1);
		}
		exitCallMode();
	}
	
	Runnable run_finish = new Runnable(){
		public void run() {
			if (mTransferedFilename!=null && incomingCall)
			{
				if (mComplete)
				{
					try{
						String shortName=mTransferedFilename.substring(mTransferedFilename.lastIndexOf("/")+1);
						storeSmsRecord(1, mTransferedFilename, shortName);
					}catch(Exception e){}
					
					Intent it = new Intent(Global.Action_InternalCMD);
					it.putExtra("Command", Global.CMD_FILE_TRANSFERED);
					it.putExtra("Filename", mTransferedFilename);
					sendBroadcast(it);
				}
			}
			
			finish();
		}
	};
	
	private void exitCallMode()
	{
		exitCallMode(false);
	}
	
	private void exitCallMode(boolean finishing) 
	{
		try{
			mHandler.removeCallbacks(timeElapsed);
			
			if (AireVenus.instance()!=null)
				AireVenus.instance().callStopRing();
			
			if (!finishing)
			{
				mHandler.postDelayed(run_finish, 1500);
			}
			//alec: to delay 1500ms for user to know what happened
			
			imCalling = false;
		}catch(Exception e)
		{}
	}
	
	private void callPending(VoipCall call) {
		// Privacy setting to not share the user camera by default
		/*** Simon 030811 Disable to whole video stuff for now
		boolean prefVideoEnable = mPref.readBoolean(getString(R.string.pref_video_enable_key));
		boolean prefAutomaticallyShareMyCamera = mPref.readBoolean(getString(R.string.pref_video_automatically_share_my_video_key));
		getVideoManager().setMuted(!(prefVideoEnable && prefAutomaticallyShareMyCamera));
		call.enableCamera(prefAutomaticallyShareMyCamera);
		*****Simon */
	}
	
	private String getYourSipServerByTCP(String address)
	{
		AireJupiter x;
		if ((x=AireJupiter.getInstance())!=null)
		{
			String sipIP = x.getYourSipServer(address);
			if (sipIP != null && !sipIP.equals(x.mySipServer)) //simon 061811
			{
				address += "@" + sipIP;
				Log.d("Callee: "+address);
			}
		}
		return address;
	}

	public void newOutgoingCall(String address, boolean withVideo) {
		imCalling = true;
		VoipCore lVoipCore=null;
		if (AireVenus.instance()!=null)
		{
			lVoipCore = AireVenus.instance().getVoipCore(); 
			if (lVoipCore.isIncall()) {
				return;
			}
		}
		
		VoipAddress lAddress;
		try {
			lAddress = lVoipCore.interpretUrl(address);
		} catch (VoipCoreException e) {
			return;
		}
		lAddress.setDisplayName(mDisplayName);

		try {
			Log.i("Calling:"+address);
			CallManager.getInstance().inviteAddress(lAddress, withVideo);
		} catch (VoipCoreException e) {
			return;
		}
	}

	public void initFromConf() throws VoipException {
		try {
			AireVenus.instance().initFromConf();
		} catch (VoipConfigException e) {
			Log.e(e.getMessage());
		}
	}

	long enabletime;
    KeyguardManager mKeyguardManager;
    KeyguardManager.KeyguardLock mKeyguardLock;
    boolean enabled;
    
	void disableKeyguard() {
    	if (mKeyguardManager == null) {
	        mKeyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
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
		if (!enabled) {
			mKeyguardLock.reenableKeyguard();
			enabled = true;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_POWER)
			&& event.getRepeatCount() == 0 && !Connected && incomingCall) {
			if (AireVenus.instance()!=null) {
//				AireVenus.instance().stopRinging();
				AireVenus.instance().stopRing();  //tml*** new ring
			}
	        return true;
	    }
		else if (keyCode == KeyEvent.KEYCODE_HOME)
		{
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public void endUpDialer(String address)
	{
		if (AireVenus.instance()!=null)
		{
			VoipCore lVoipCore = AireVenus.instance().getVoipCore();
			if (lVoipCore!=null && lVoipCore.isIncall())//alec
			{
				VoipCall myCall = lVoipCore.getCurrentCall();
				if (myCall != null)
				{
					String remoteNumber=myCall.getRemoteAddress().getUserName();
					if (remoteNumber.equals(address))
					{
						Log.d("terminateCall");
						lVoipCore.terminateCall(myCall);
						sendTerminateSignal=false;
						return;//alec
					}
				}
			}
		}
		exitCallMode();
	}

	
	int surfaceAngel=90;
	
	public void lockScreenOrientation() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		if (Integer.parseInt(Build.VERSION.SDK) < 8) {
	    	surfaceAngel=90;
	    	return;
		}
		try{
			switch (((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRotation()) {
		    case Surface.ROTATION_90:
		        surfaceAngel=0;
		        break;
		    case Surface.ROTATION_180:
		    	surfaceAngel=270;
		        break;
		    case Surface.ROTATION_270: 
		    	surfaceAngel=180;
		        break;
		    default:
		    	surfaceAngel=90;
		    }
		}catch(Exception e){}
	}
	
	Runnable displayP2P=new Runnable(){
		public void run()
		{
			VoipCore lc=AireVenus.getLc();
			if (lc!=null && lc.isIncall())
			{
				try{
					int status=lc.getStatus();
					ImageView iv=(ImageView)findViewById(R.id.ind0);
					if ((status&0xF00)==0x100)
						iv.setImageResource(R.drawable.purple);//amr
					else if ((status&0xF00)==0x200)
						iv.setImageResource(R.drawable.red);//speex
					else if ((status&0xF00)==0x300)
						iv.setImageResource(R.drawable.blue);//PCMA
					else if ((status&0xF00)==0x400)
						iv.setImageResource(R.drawable.teal);//PCMU
					else if ((status&0xF00)==0x500)
						iv.setImageResource(R.drawable.green);//File
					else
						iv.setImageResource(R.drawable.gray);
					
					iv=(ImageView)findViewById(R.id.ind1);
					if (lc.isRunningP2P())
						iv.setImageResource(R.drawable.orange);
					else
						iv.setImageResource(R.drawable.yellow);
					
					iv=(ImageView)findViewById(R.id.ind2);
					int send=status&0xF;
					if (send==0x5)
						iv.setImageResource(R.drawable.red);
					else if (send==0x4)
						iv.setImageResource(R.drawable.green);
					else if (send==0x1)
						iv.setImageResource(R.drawable.teal);
					else
						iv.setImageResource(R.drawable.gray);
					
					iv=(ImageView)findViewById(R.id.ind3);
					int recv=status&0xF0;
					if (recv==0x50)
						iv.setImageResource(R.drawable.red);
					else if (recv==0x40)
						iv.setImageResource(R.drawable.blue);
					else if (recv==0x10)
						iv.setImageResource(R.drawable.purple);
					else
						iv.setImageResource(R.drawable.gray);
					
					((LinearLayout)findViewById(R.id.status)).setVisibility(View.VISIBLE);
					
				}catch(Exception e){}
				
				mHandler.postDelayed(displayP2P, 1000);
			}
		}
	};
	
	private void hangup()
	{
		mStatus.setText(R.string.file_transfer_stop);
		mHangup.setOnClickListener(null);
		((ImageView) findViewById(R.id.close)).setOnClickListener(null);
		HangingUp=true;
		sendTerminateSignal=true;
		try{ 
			mHandler.removeCallbacks(timeElapsed);
			mHandler.removeCallbacks(run_disp_msg);
			
			if (AireVenus.instance() != null) {
				VoipCore lVoipCore = AireVenus.instance().getVoipCore();
				VoipCall myCall = lVoipCore.getCurrentCall();
				if (myCall != null) {
					lVoipCore.terminateCall(myCall);
					Log.d("lVoipCore.terminateCall()");
					return;
				}
			} 
		}catch(Exception e){ }
		exitCallMode();
	}
	
	
	private void storeSmsRecord(int type, String filePath, String shortName)
	{
		SMS msg = new SMS();
		msg.displayname = mDisplayName;
		msg.address = mAddress;
		if (type==1)
			msg.content = "(fl) ["+shortName+"] "+getString(R.string.downloadsucess);
		else
			msg.content = "(fl) "+shortName + " " +getString(R.string.filememo_send);
		msg.contactid = contact_id;
		msg.read = 1;
		msg.type = type;
		msg.status = -1;
		msg.time = new Date().getTime();

		msg.attached = 8;
		msg.att_path_aud = filePath;
		msg.att_path_img = null;
		
		SmsDB mDB = new SmsDB(this);
		mDB.open();

		msg.longitudeE6 = mPref.readLong("longitude", 0);
		msg.latitudeE6 = mPref.readLong("latitude", 0);
		
		mDB.insertMessage(mAddress,
				msg.contactid, (new Date()).getTime(), 1, msg.status,
				msg.type, "", msg.content, msg.attached,
				msg.att_path_aud, msg.att_path_img, 0, msg.longitudeE6,
				msg.latitudeE6, 0, null, null, 0);
		
		mDB.close();
		
		Intent it = new Intent();
		it.setAction(Global.Action_MsgGot);
		sendBroadcast(it);
	}
}	
