package com.pingshow.amper;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.format.DateUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.contacts.RWTOnline;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.GroupDB;
import com.pingshow.amper.db.WTHistoryDB;
import com.pingshow.amper.message.PlayObject;
import com.pingshow.codec.RealTimeWTPlayer_WB;
import com.pingshow.codec.RealTimeWT_WB;
import com.pingshow.codec.VoiceMemoPlayer_NB;
import com.pingshow.codec.VoicePlayer2_MP;
import com.pingshow.codec.VoiceRecord2_MR;
import com.pingshow.network.NetInfo;
import com.pingshow.network.RWTSocket;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;

public class WalkieTalkieDialog extends Activity{
	
	public final static int interphoneSmile_length=49;
	
	public static ArrayList<PlayObject> playlist = new ArrayList<PlayObject>();
	
	public static boolean recording = false;
	
	public static boolean playing = false;
	private VoiceMemoPlayer_NB vmp=null;
	private VoicePlayer2_MP myVP = null;
	private RealTimeWTPlayer_WB wtp=null;
	private Vibrator mVibrator;
	private TextView speakingBubble = null;
	private AmpUserDB mADB = null;
	private Handler handlerTimer = new Handler();
	private String displayName = null;
	private Button btnSpeaking;
	private boolean stop = false;
	private Handler mHandler = new Handler();
	float mDensity = 1.f;
	private WTHistoryDB mWTDB;
	private boolean useRealTime=false;
	private boolean inGroup;
	private int mGroupID;
	//private PowerManager.WakeLock wl;
	
	private MyPreference mPrf;
//	private VoiceMemo_NB vm;
	private VoiceRecord2_MR myVR;
	private RealTimeWT_WB rwt;
	private String SrcAudioPath;
	private String mAddress;
	private int myIdx;
	private int mIdx;
	private ContactsQuery cq;
	boolean temp=false;
	public Handler handler = null;
	private boolean sended = false;
	private int timer = 59;
	private ImageView mTalking;
	private TextView date;
	private long preTime = 0;
	private boolean largeScreen=false;
	
	private RWTSocket rwtSocket;
	static int myNetType=1;
	
	static Bundle data=null;
	
	static public WalkieTalkieDialog instance=null;
	static public WalkieTalkieDialog getInstance() { 
		return instance;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR); 
		setContentView(R.layout.walkietalkie);
		this.overridePendingTransition(R.anim.appear, R.anim.disappear);
		
		mDensity = getResources().getDisplayMetrics().density;
		largeScreen=(findViewById(R.id.large)!=null);
		
		WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
	    if (!largeScreen)
	    	lp.width=(int)(320.*mDensity);
		lp.dimAmount = 0.3f;
		getWindow().setAttributes(lp);
		
		cq=new ContactsQuery(this);
		mADB = new AmpUserDB(this);
		mADB.open();
		
		mWTDB=new WTHistoryDB(this);
		mWTDB.open();
		
		mPrf=new MyPreference(getApplicationContext());
		
		myNetType=new NetInfo(this).netType;//alec
		
		myIdx=Integer.parseInt(mPrf.read("myID","0"),16);
		
		((ImageView)findViewById(R.id.close)).setOnClickListener(new OnClickListener() {
	    	@Override
	    	public void onClick(View v) {
	    		if (playing) return;
	    		finish();
	    		data=null;
	    	}
	    });
		
		final ImageView wt_autoOut=(ImageView)findViewById(R.id.wtout);
		if (wt_autoOut!=null)
		{
			if (mPrf.readBoolean("wtSoundOut", true))
				wt_autoOut.setImageResource(R.drawable.wtout_on);
			else
				wt_autoOut.setImageResource(R.drawable.wtout_off);
			
			wt_autoOut.setOnClickListener(new OnClickListener() {
		    	@Override
		    	public void onClick(View v) {
		    		if (mPrf.readBoolean("wtSoundOut", true))
		    		{
		    			mPrf.write("wtSoundOut", false);
		    			wt_autoOut.setImageResource(R.drawable.wtout_off);
		    		}else{
		    			mPrf.write("wtSoundOut", true);
		    			wt_autoOut.setImageResource(R.drawable.wtout_on);
		    		}
		    	}
		    });
		}
		
		speakingBubble = (TextView)findViewById(R.id.timer);
		speakingBubble.setVisibility(View.INVISIBLE);
		
		mTalking=(ImageView)findViewById(R.id.talking);
		mTalking.setVisibility(View.INVISIBLE);
		
		date=(TextView)findViewById(R.id.date);
		
	    btnSpeaking=(Button)findViewById(R.id.speaking_btn);
	    btnSpeaking.setOnTouchListener(new OnTouchListener() {
	    	@Override
	    	public boolean onTouch(View v, MotionEvent event) {
	    		if(event.getAction()==MotionEvent.ACTION_DOWN){
	    			if((new Date().getTime()-preTime)<1500 || recording){ 
	    				btnSpeaking.setEnabled(false);
	    				new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								btnSpeaking.setEnabled(true);
							}
						}, 1000);
	    				return true;
	    			}
	    		
	    			mIdx=mADB.getIdxByAddress(currentAddress);
	    			useRealTime=(myNetType>1 && rwtSocket!=null && rwtSocket.isLogged() && RWTOnline.getContactOnlineStatus(mIdx)>1);
	    			
	    			Log.d("useRealTime= "+useRealTime+" myNetType="+myNetType+" online:"+RWTOnline.getContactOnlineStatus(mIdx));
	    			
	    			if (!MyUtil.CheckServiceExists(WalkieTalkieDialog.this, "com.pingshow.amper.PlayService")) {
						Intent intent1 = new Intent(WalkieTalkieDialog.this, PlayService.class);
						intent1.putExtra("soundInCall", useRealTime?R.raw.wts:R.raw.burst);
						startService(intent1);
					}
	    			
	    			recording=true;
	    			preTime = new Date().getTime();
	    			stop = false;
	    			sended = false;
	    			if (useRealTime)
	    			{
	    				if (rwtSocket!=null && rwtSocket.isLogged())
	    				{
		    				rwt=new RealTimeWT_WB(mIdx, myNetType, rwtSocket);
		    				rwt.setChannel("en",0);//private
		    				rwt.start();
	    				}
	    			}else{
	    				SrcAudioPath=Global.SdcardPath_sent+getRandomName()+".amr";
//		    			vm=new VoiceMemo_NB(SrcAudioPath);
//		    			vm.start();
	    				//tml*** new vmsg
	    				try {
		    				myVR = new VoiceRecord2_MR(WalkieTalkieDialog.this, SrcAudioPath,
									MediaRecorder.OutputFormat.RAW_AMR,
									MediaRecorder.OutputFormat.DEFAULT, 8000);
							myVR.start();
	    				} catch (Exception e) {
	    					myVR = null;
	    					return false;
	    				}
	    				
	    			}
	    			speakingBubble.setVisibility(View.VISIBLE);
	    			
	    			handlerTimer.postDelayed(mCountingDown,100);
	    			
	    			date.setText("");
	    		}
	    		else if(event.getAction()==MotionEvent.ACTION_UP && !sended)
	    		{
	    			if (timer>=58){//too short
	    				timer = 59;
	    				speakingBubble.setText("59");
	    				speakingBubble.setVisibility(View.INVISIBLE);
	    				stop = true;
	    				
//	    				if(vm!=null) vm.stop();
//	    				vm = null;
	    				if (myVR != null) {  //tml*** new vmsg
	    					myVR.stop();
	    					myVR = null;
	    				}
	    				
	    				if(rwt!=null) rwt.stop();
	    				rwt = null;
	    				
	    				mHandler.postDelayed(playStoredVoice, 1500);
	    				return false;
	    			}
	    			speakingBubble.setVisibility(View.INVISIBLE);
	    			
	    			doPressUp();
	    			sended = true;
	    		}
	    		return false;
	    	}
	    });
	    
		// zhao
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		
		//PowerManager pm=(PowerManager)getSystemService(Context.POWER_SERVICE);
		//wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "dimVoice");
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(Global.ACTION_PLAY_AUDIO);
		filter.addAction(Global.Action_Friends_Status_Updated);
		filter.addAction(Global.Action_AnswerCall);
		filter.addAction(Global.Action_SMS_Fail);
		filter.addAction(Global.Action_Raw_Audio_Playback);
		registerReceiver(WalkieTalkieReceiver, filter);
		
    	setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	
    	mHandler.postDelayed(keepAliveInWT, 120000);
    	
    	if (data==null)
    		data=getIntent().getExtras();
    	setupDialog(data);
    	
    	recording=false;
    	instance=this;
    	
    	if (!AmazonKindle.hasMicrophone(this))
    	{
			finish();
			return;
		}
    	
    	new Thread(loginRWTServer,"loginRWTServer").start();
	}
	
	private Bundle bundle;
	final Runnable mRefreshViews=new Runnable()
	{
		@Override
		public void run() {
			setupDialog(bundle);
		}
	};
	public void refresh(Bundle b)
	{
		bundle=b;
		mHandler.post(mRefreshViews);
	}
	
	int fromPosition=0;
	String currentAddress="";
	
	void setupDialog(Bundle b)
	{
		mAddress=b.getString("Address");
		int mIdx=mADB.getIdxByAddress(mAddress);	
		long contactId=cq.getContactIdByNumber(mAddress);
		boolean switching=b.getBoolean("Switching",false);
		
		mWTDB.insert(mAddress, mIdx);
		
		RelativeLayout s=(RelativeLayout)findViewById(R.id.side_users);
		s.removeAllViews();
		
		inGroup=mAddress.startsWith("[<GROUP>]");
		if (inGroup)
			mGroupID=Integer.parseInt(mAddress.substring(9));

		if (contactId>0)
			displayName = cq.getNameByContactId(contactId);
		else
		    displayName = mADB.getNicknameByAddress(mAddress);
		
		((TextView)findViewById(R.id.displayname)).setText(displayName);
		
		Drawable photo=ImageUtil.getBigRoundedUserPhoto(this, mIdx);
		if (photo!=null) 
			((ImageView)findViewById(R.id.photo)).setImageDrawable(photo);
		else {
			if (inGroup)
				((ImageView)findViewById(R.id.photo)).setImageResource(R.drawable.group_empty);
			else
				((ImageView)findViewById(R.id.photo)).setImageResource(R.drawable.bighead);
		}
		
		if (inGroup)
		{
			((ImageView)findViewById(R.id.photo)).setBackgroundResource(R.drawable.group_bg);
		}else{
			((ImageView)findViewById(R.id.photo)).setBackgroundResource(R.drawable.empty);
		}
		
		if (!currentAddress.equals(mAddress) && switching)
		{
			LinearLayout main=(LinearLayout)findViewById(R.id.mainphoto);
			AnimationSet as = new AnimationSet(false);
		    as.setInterpolator(new AccelerateInterpolator());
		    TranslateAnimation ta;
		    if (largeScreen)
		    	ta = new TranslateAnimation(mDensity*(170+90*fromPosition),0,mDensity*40,0);
		    else
		    	ta = new TranslateAnimation(mDensity*(110+40*fromPosition),0,mDensity*40,0);
			ScaleAnimation sa = new ScaleAnimation(0.5f,1,0.5f,1);
			sa.setDuration(200);
			ta.setDuration(200);
			as.addAnimation(sa);
			as.addAnimation(ta);
			as.setDuration(200);
			main.startAnimation(as);
		}
		currentAddress=mAddress;
		mIdx=mADB.getIdxByAddress(currentAddress);
		
		int count=1;
		Cursor c=mWTDB.fetchRecent(5);
		
		if (c!=null)
		{
			count=c.getCount()-1;
			if (count>0)
			{
				c.moveToLast();
				do
				{
					ImageView a=new ImageView(this);
					int idx=c.getInt(3);
					String address=mADB.getAddressByIdx(idx);
					if (address.startsWith("[<GROUP>]"))
						a.setBackgroundResource(R.drawable.group_bg);
					else{
						if (ContactsOnline.getContactOnlineStatus(address)>0)
							a.setBackgroundResource(R.drawable.empty_online);
						else
							a.setBackgroundResource(R.drawable.empty);
					}
					if (largeScreen)
						a.setPadding((int)(mDensity*7), (int)(mDensity*7), (int)(mDensity*7), (int)(mDensity*7));
					else
						a.setPadding((int)(mDensity*5), (int)(mDensity*5), (int)(mDensity*5), (int)(mDensity*5));
						
					a.setClickable(true);
					a.setOnClickListener(new View.OnClickListener(){
						@Override
						public void onClick(View v) {
							if (playing) return;
							Bundle b=new Bundle();
							String s=(String)v.getTag();
							try{
								String [] address=s.split(",");
								b.putString("Address", address[0]);
								b.putBoolean("Switching", true);
								fromPosition=Integer.parseInt(address[1]);
								setupDialog(b);
							}catch(Exception e){}
							date.setText("");
						}
					});
					
					String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
					photo=ImageUtil.getBitmapAsRoundCorner(userphotoPath,1,4);
					if (photo!=null)
						a.setImageDrawable(photo);
					else {
						if (address.startsWith("[<GROUP>]"))
							a.setImageResource(R.drawable.group_empty);
						else
							a.setImageResource(R.drawable.bighead);
					}
					
					a.setTag(c.getString(1)+","+(count-1));
					
					RelativeLayout.LayoutParams lp=null;
					
					if (largeScreen)
					{
						lp = new RelativeLayout.LayoutParams(
								(int)(mDensity*80), (int)(mDensity*80));
						lp.leftMargin=(int)(mDensity*90)*(count-1);
					}else{
						lp = new RelativeLayout.LayoutParams(
								(int)(mDensity*60), (int)(mDensity*60));
						lp.leftMargin=(int)(mDensity*40)*(count-1);
					}

					lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					s.addView(a, lp);
					count--;
				}while(c.moveToPrevious() && count>0);
			}
			c.close();
		}
		b.remove("Switching");
		data=b;
	}
	
	private Runnable mCountingDown = new Runnable() {
		@Override
		public void run() {
			if(stop) return;
			if(timer<0) timer = 59;
			if(timer==0 && !sended){
				sended = true;
				speakingBubble.setVisibility(View.INVISIBLE);
				doPressUp();
				return;
			}
			if(timer<10)
				speakingBubble.setTextColor(0x80f17d00);
			else
				speakingBubble.setTextColor(0xff4c4d51);
			speakingBubble.setText((timer--)+"");
			handlerTimer.postDelayed(mCountingDown, 1000);
		}
	};
	
	BitmapDrawable resizeDrawable(Bitmap bitmapOrg)
	{
		int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        float scaleWidth=76.f/width;
        float scaleHeight=76.f/height;
       
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);
        if(!bitmapOrg.isRecycled())
        	bitmapOrg.recycle();
        return new BitmapDrawable(resizedBitmap);
	}
	
	private void doPressUp(){
		handlerTimer.removeCallbacks(mCountingDown);
//		if(vm!=null) vm.stop();
//		vm=null;
		if (myVR != null) {  //tml*** new vmsg
			myVR.stop();
			myVR = null;
		}
		if(rwt!=null) rwt.stop();
		rwt=null;
		
		if (!MyUtil.CheckServiceExists(WalkieTalkieDialog.this, "com.pingshow.amper.PlayService")) {
			Intent intent1 = new Intent(WalkieTalkieDialog.this, PlayService.class);
			intent1.putExtra("soundInCall", useRealTime?R.raw.wtf:R.raw.beep);
			startService(intent1);
		}
		
		if (useRealTime)
		{
			
		}
		else
		{
			if(mAddress==null) return;
			
			int size=0;
			try{
				File f=new File(SrcAudioPath);
				size=(int)f.length();
			}catch(Exception e){}
			if (size>6)
			{
				int idx=mADB.getIdxByAddress(mAddress);
				SendAgent sa = new SendAgent(getApplicationContext(),myIdx,idx,false);
				if (inGroup)
				{
					GroupDB gdb=new GroupDB(WalkieTalkieDialog.this);
					gdb.open(true);
					ArrayList<String> sendeeList=gdb.getGroupMembersByGroupIdx(mGroupID);
					ArrayList<String> addressList=new ArrayList<String>();
					for (int i=0;i<sendeeList.size();i++)
					{
						String address=mADB.getAddressByIdx(Integer.parseInt(sendeeList.get(i)));
						addressList.add(address);
					}
					sa.setAsGroup(mGroupID);
					sa.onMultipleSend(addressList, "(iPh)", 4, SrcAudioPath, null);
					gdb.close();
				}
				else
					sa.onSend(mAddress, "(iPh)", 4, SrcAudioPath, null, true);
				mHandler.postDelayed(playStoredVoice, 1000);
			}
			else
				Log.e("Failed to record vm");
		}
		
		if (rwtSocket!=null)
		{
			new Thread(new Runnable(){
				public void run()
				{
					if (rwtSocket!=null)
					{
						MyUtil.Sleep(1000);
						if (!rwtSocket.isLogged())
							rwtSocket.Login();
						if (!playing && !recording)
							rwtSocket.queryFriendsOnlineStatus();
					}
				}
			}).start();
		}
		
		timer = 59;
		speakingBubble.setText("59");
		
		mHandler.postDelayed(new Runnable(){
			public void run()
			{
				mADB.updateLastContactTimeByAddress(mAddress, new Date().getTime());
				if (UsersActivity.sortMethod==1)
					UsersActivity.forceRefresh=true;
			}
		}, 1250);
	}
	
	final Runnable loginRWTServer = new Runnable(){
		public void run()
		{
			if (AireJupiter.getInstance()!=null)
				rwtSocket=AireJupiter.getInstance().new_rwt_socket();
			
			if (rwtSocket!=null)
			{
				if (!rwtSocket.isLogged())
				{
					rwtSocket.Login();
					mPrf.writeLong("last_rwt_query_status", 0);
				}
				
				if (!playing && !recording)
					rwtSocket.queryFriendsOnlineStatus();
			}
		}
	};
	
	
	Runnable keepAliveInWT=new Runnable()
	{
		public void run()
		{
			keepAlive();
			mHandler.postDelayed(keepAliveInWT, 120000);
		}
	};
	
	public void keepAlive()
	{
		new Thread(loginRWTServer,"loginRWTServer").start();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (Integer.parseInt(Build.VERSION.SDK) >= 5 && Integer.parseInt(Build.VERSION.SDK) <= 7)
			disableKeyguard();
		
		freezePlayList=false;
		mHandler.postDelayed(playStoredVoice, 500);
//		MobclickAgent.onResume(this);
	}
	
	private Runnable playStoredVoice=new Runnable(){
		public void run(){
			onPlayObjects();
		}
	};
	
	private void startVibrating(){
		long[] patern = {0,40,1000};
		mVibrator.vibrate(patern, -1);
	}
	
	@Override
	protected void onPause() {
		if (Integer.parseInt(Build.VERSION.SDK) >= 5 && Integer.parseInt(Build.VERSION.SDK) <= 7)
			reenableKeyguard();
		if (recording)
		{
//			if(vm!=null) vm.stop();
//			vm=null;
			if (myVR != null) {  //tml*** new vmsg
				myVR.stop();
				myVR = null;
			}
			handlerTimer.removeCallbacks(mCountingDown);
			mTalking.setVisibility(View.INVISIBLE);
			timer = 59;
			speakingBubble.setText("59");
		}
		if (isFinishing())
		{
			stopPlayingVoice();
			//instance=null;
		}
		instance=null;
		Intent intent1 = new Intent(WalkieTalkieDialog.this, PlayService.class);
		stopService(intent1);
//		MobclickAgent.onPause(this);
		super.onPause();
	}

	protected void onDestroy() {
		unregisterReceiver(WalkieTalkieReceiver);
		mHandler.removeCallbacks(keepAliveInWT);
		mHandler.removeCallbacks(loginRWTServer);
		mHandler.removeCallbacks(playStoredVoice);
		mHandler.removeCallbacks(mCountingDown);
		if(mVibrator!=null) mVibrator.cancel();
		if(mADB!=null && mADB.isOpen()) mADB.close();
		if(mWTDB!=null && mWTDB.isOpen()) mWTDB.close();
		
		if (wtp!=null)
			wtp.release();
		
		setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

		System.gc();
		System.gc();
		super.onDestroy();
	};
	
	@Override
	public void onStart() {
		super.onStart();
		if (Integer.parseInt(Build.VERSION.SDK) < 5 || Integer.parseInt(Build.VERSION.SDK) > 7)
			disableKeyguard();
	}
	@Override
	public void onStop() {
		if (Integer.parseInt(Build.VERSION.SDK) < 5 || Integer.parseInt(Build.VERSION.SDK) > 7)
			reenableKeyguard();
		super.onStop();
	}
    KeyguardManager mKeyguardManager;
    KeyguardManager.KeyguardLock mKeyguardLock;
    boolean enabled;
	void disableKeyguard() {
		if (AmazonKindle.IsKindle()) return;
    	if (mKeyguardManager == null) {
	        mKeyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
	        mKeyguardLock = mKeyguardManager.newKeyguardLock("FafaYou");
	        enabled = true;
    	}
		if (enabled) {
			mKeyguardLock.disableKeyguard();
			enabled = false;
		}
	}
	
	void reenableKeyguard() {
		if (AmazonKindle.IsKindle()) return;
		if (!enabled) {
			try {
				if (Integer.parseInt(Build.VERSION.SDK) < 5)
					Thread.sleep(1000);
			} catch (InterruptedException e) {}
			mKeyguardLock.reenableKeyguard();
			enabled = true;
		}
	}
	
	int CurrentRecvIdx=0;
	
	BroadcastReceiver WalkieTalkieReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(Global.ACTION_PLAY_AUDIO))
			{
				CurrentRecvIdx=0;
				if(intent.getIntExtra("clear", 0)==1){
					playing=false;
					vmp=null;
					myVP = null;
					if (wtp!=null)
					{
						//wtp.release();
						wtp=null;
					}
					mHandler.postDelayed(playStoredVoice, 1500);//try play next one
					
					if (!MyUtil.CheckServiceExists(WalkieTalkieDialog.this, "com.pingshow.amper.PlayService")) {
						Intent intent1 = new Intent(WalkieTalkieDialog.this, PlayService.class);
						intent1.putExtra("soundInCall", R.raw.beep);
						startService(intent1);
					}
					mTalking.setVisibility(View.INVISIBLE);
					btnSpeaking.setEnabled(true);
					
					return;
				}
				
				if (recording || playing) return;
				
				currentAddress=mAddress;
				onPlayObjects();
			}
			else if (intent.getAction().equals(Global.Action_AnswerCall))
			{
				stopPlayingVoice();
			}else if(intent.getAction().equals(Global.Action_SMS_Fail)){
				Toast.makeText(WalkieTalkieDialog.this, getString(R.string.smsfail), Toast.LENGTH_SHORT).show();
			}
			else if(intent.getAction().equals(Global.Action_Raw_Audio_Playback)){
				if (!intent.getBooleanExtra("private", true)) return;
				if (recording) return;
				int idx=intent.getIntExtra("from", 0);
				//int seq=intent.getIntExtra("seq", 0);
				
				if (idx<50||CurrentRecvIdx==0) 
					CurrentRecvIdx=idx;
				
				if (wtp==null && CurrentRecvIdx==idx)
				{
					wtp=new RealTimeWTPlayer_WB(WalkieTalkieDialog.this, ContactsOnline.getContactOnlineStatus(currentAddress), false);
					try{
						int len=intent.getIntExtra("length", 0);
						wtp.append(intent.getByteArrayExtra("raw"), len);
						wtp.run();
						
						mTalking.setVisibility(View.VISIBLE);
						btnSpeaking.setEnabled(false);
						
						startVibrating();
					}catch(Exception e){}
				}else if (CurrentRecvIdx==idx){
					try{
						int len=intent.getIntExtra("length", 0);
						wtp.append(intent.getByteArrayExtra("raw"), len);
					}catch(Exception e){}
				}
				else 
					return;
				playing = true;
			}
		}
	};

	public static String getRandomName()
	{
        return (""+new Date().getTime());
	}
	
	public static void addPlayObject(int idx, int type, long time, String filePath)
	{
		PlayObject po=new PlayObject();
		po.filePath=filePath;
		po.from=idx;
		po.type=type;
		po.time=time;
		playlist.add(po);
	}
	
	boolean freezePlayList=false;
	public void stopPlayingVoice()
	{
		try{
			if (vmp!=null) {
				vmp.stop();
				vmp=null;
			}
			if (myVP != null) {  //tml*** new vmsg
				myVP.stop();
				myVP = null;
			}
		}catch(Exception e){}
		freezePlayList=true;
	}
	
	@Override
	protected void onRestart()
	{
		super.onRestart();
		freezePlayList=false;
		mHandler.postDelayed(playStoredVoice, 500);
	}
	
	private String _path;
	private void onPlayObjects()
	{
		if (playlist.size()>0 && !recording && freezePlayList==false)
		{
			//if (!wl.isHeld()) wl.acquire();
			
			PlayObject po=playlist.get(0);
			
			File f=new File(po.filePath);
			if (f.length()<7)
			{
				f.delete();
				playlist.remove(po);
				onPlayObjects();
				return;
			}
			
			if (po.type==-1)//voice
			{
				if (po.filePath.endsWith("amr")) {
					try {
						vmp = new VoiceMemoPlayer_NB(this);
						vmp.setDataSource(po.filePath);
					} catch (Exception e) {
						vmp = null;
						playlist.remove(po);
						return;
					} 
					try {  
					    vmp.prepare();
					    vmp.start();
					    playing = true;
					}catch (Exception e) {
						vmp = null;
						playlist.remove(po);
						return;
					}
				} else {  //tml*** new vmsg
					try {
						myVP = new VoicePlayer2_MP(WalkieTalkieDialog.this, po.filePath);
					} catch (Exception e) {
						myVP = null;
						playlist.remove(po);
						return;
					} 
					try {
						myVP.start();
					    playing = true;
						_path = po.filePath;
					}catch (Exception e) {
						myVP = null;
						playlist.remove(po);
						return;
					}
				}
			}
			
			if (!mAddress.equals(mADB.getAddressByIdx(po.from)))
			{
				mAddress = mADB.getAddressByIdx(po.from);
				Bundle b=new Bundle();
				b.putString("Address", mAddress);
				setupDialog(new Bundle(b));
			}
			
			startVibrating();
			
			if ((new Date().getTime()-po.time)<3600000)
				date.setText(DateUtils.formatDateTime(getApplicationContext(),
						po.time, DateUtils.FORMAT_SHOW_TIME
						| DateUtils.FORMAT_CAP_AMPM));
			else
				date.setText(DateUtils.formatDateTime(getApplicationContext(),
						po.time, DateUtils.FORMAT_SHOW_DATE
						| DateUtils.FORMAT_SHOW_TIME
						| DateUtils.FORMAT_CAP_AMPM));
			
			playlist.remove(po);
			
			mTalking.setVisibility(View.VISIBLE);
			btnSpeaking.setEnabled(false);
		}
	}
	
	@Override
	public void onBackPressed() {
		if (playing) return;
		finish();
		data=null;
		super.onBackPressed();
	}
}
