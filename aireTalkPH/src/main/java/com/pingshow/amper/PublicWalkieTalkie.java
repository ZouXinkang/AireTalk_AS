package com.pingshow.amper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.ContactsContract.CommonDataKinds;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.StudioGroupDB;
import com.pingshow.amper.message.PlayObject;
import com.pingshow.amper.view.HScroll;
import com.pingshow.amper.view.Strength;
import com.pingshow.amper.view.Tuner;
import com.pingshow.codec.Ampitude;
import com.pingshow.codec.RealTimeWTPlayer_WB;
import com.pingshow.codec.RealTimeWT_WB;
import com.pingshow.codec.Volume;
import com.pingshow.network.MyNet;
import com.pingshow.network.NetInfo;
import com.pingshow.network.RWTSocket;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;

public class PublicWalkieTalkie extends Activity{
	
	private final static String [] langs={"en","es","fr","zh","pt","ja","it","ko","de","iw","nl","ar","ru","my","th","x1","x2","x3","x4","x5","x6"};
	public static ArrayList<PlayObject> playlist = new ArrayList<PlayObject>();
	private	int padHeight=340;
	
	public static boolean recording = false;
	
	public static boolean playing = false;// -1 is not play, 0 for playing voice, 1 for sound effect, 2 for self sound effect
	private RealTimeWTPlayer_WB wtp=null;
	private Vibrator mVibrator;
	private AmpUserDB mADB = null;
	private Handler handlerTimer = new Handler();
	private Button btnSpeaking;
	private ImageView [] onAir=new ImageView[6];
	private boolean stop = false;
	private Handler mHandler = new Handler();
	float mDensity = 1.f;
	private boolean moving=false;
	
	private MyPreference mPref;
	private RealTimeWT_WB rwt;
	boolean temp=false;
	public Handler handler = null;
	private boolean sended = false;
	private int timer = 48;
	private long preTime = 0;
	private boolean newInChannel=true;
	private Tuner volumeController; 
	private Strength meter;
	private int limit;
	
	private View tunningPad;
	private ImageView eject;
	private ImageView retract;
	private boolean bOpened=false;
	
	private ListView mGroupList;
	private StudioGroupDB mSGDB = null;
	private QueryThreadHandler mThreadQueryHandler;
	private StudioGroupsAdapter mCursorAdapter;
	private Cursor mCursor;
	private String groupName;
	private ProgressDialog progress;
	
	static int myNetType=1;
	
	private int channel=1;
	private int language=0;
	
	private RWTSocket rwtSocket;
	
	private PowerManager.WakeLock wl;
	
	static public PublicWalkieTalkie instance=null;
	static public PublicWalkieTalkie getInstance() { 
		return instance;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		lockScreenOrientation();
		setContentView(R.layout.public_page);
		this.overridePendingTransition(R.anim.freeze, R.anim.freeze);
		
		neverSayNeverDie(PublicWalkieTalkie.this);  //tml|bj*** neverdie/
		
		mDensity = getResources().getDisplayMetrics().density;
		
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		RelativeLayout space=(RelativeLayout)findViewById(R.id.space);
		
		if ((View)findViewById(R.id.ldpi)!=null)
			padHeight=270;
		else if ((View)findViewById(R.id.large)!=null)
			padHeight=480;
		
		View v = inflater.inflate(R.layout.public_groups_inf, null, false);
		RelativeLayout.LayoutParams lp=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,(int)(mDensity*padHeight));
		lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		space.addView(v,lp);
		
		tunningPad = inflater.inflate(R.layout.public_inflate, null, false);
		lp=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,(int)(mDensity*padHeight));
		lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
		space.addView(tunningPad,lp);
		
		mGroupList=(ListView)findViewById(R.id.groups);
		
		eject=(ImageView)findViewById(R.id.eject);
		retract=(ImageView)findViewById(R.id.retract);
		
		mSGDB=new StudioGroupDB(this);
		mSGDB.open();
		
		mADB = new AmpUserDB(this);
		mADB.open();
		
		mPref=new MyPreference(getApplicationContext());
		
		meter=(Strength)findViewById(R.id.strength);
		volumeController=(Tuner)findViewById(R.id.volume);
		
	    btnSpeaking=(Button)findViewById(R.id.speaking_btn);
	    onAir[0]=(ImageView)findViewById(R.id.onair1);
	    onAir[1]=(ImageView)findViewById(R.id.onair2);
	    onAir[2]=(ImageView)findViewById(R.id.onair3);
	    onAir[3]=(ImageView)findViewById(R.id.onair4);
	    onAir[4]=(ImageView)findViewById(R.id.onair5);
	    onAir[5]=(ImageView)findViewById(R.id.onair6);
	    
	    channel=mPref.readInt("channel",1);
	    language=mPref.readInt("language",-1);
	    
	    btnSpeaking.setOnTouchListener(new OnTouchListener() {
	    	@Override
	    	public boolean onTouch(View v, MotionEvent event) {
	    		if(event.getAction()==MotionEvent.ACTION_DOWN){
	    			if((new Date().getTime()-preTime)<limit || recording){
	    				btnSpeaking.setEnabled(false);
	    				new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								btnSpeaking.setEnabled(true);
							}
						}, 1000);
	    				return true;
	    			}
	    			
	    			if (rwtSocket!=null && rwtSocket.isLogged())
	    			{
	    				rwt=new RealTimeWT_WB(0, myNetType, rwtSocket);
	    				rwt.setChannel(langs[language], channel);
	    				rwt.start();

		    			if (!MyUtil.CheckServiceExists(PublicWalkieTalkie.this, "com.pingshow.amper.PlayService")) {
							Intent intent1 = new Intent(PublicWalkieTalkie.this, PlayService.class);
							intent1.putExtra("soundInCall", R.raw.wts);
							startService(intent1);
						}
		    			
		    			mHandler.removeCallbacks(blinking);
	    				lightStatus="0R0";
	    				mHandler.post(setLightStatus);
		    			
		    			((com.pingshow.amper.view.LED)findViewById(R.id.led)).setNumber(48);
		    			handlerTimer.postDelayed(mCountingDown,1000);
		    			
		    			recording=true;
		    			preTime = new Date().getTime();
		    			stop = false;
		    			sended = false;
	    			}
	    			else{
	    				((com.pingshow.amper.view.LED)findViewById(R.id.led)).setNumber(0);
	    				new Thread(loginRWTServer,"loginRWTServer").start();
	    			}
	    		}
	    		else if(event.getAction()==MotionEvent.ACTION_UP && !sended)
	    		{
    				limit=(48-timer)*50+1500;
    				
	    			if (timer>=47){//too short
	    				timer=48;
	    				stop=true;
	    				
	    				if(rwt!=null) rwt.stop();
	    				rwt = null;
	    				
	    				if (rwtSocket!=null && rwtSocket.isLogged())
		    			{
		    				lightStatus="GGG";
		    				mHandler.post(setLightStatus);
		    			}else{
		    				lightStatus="000";
		    				mHandler.post(setLightStatus);
		    			}
	    				((com.pingshow.amper.view.LED)findViewById(R.id.led)).setNumber(numMembers);
	    				
	    				return false;
	    			}
	    			doPressUp();
	    			sended = true;
	    			
	    			if (rwtSocket!=null && rwtSocket.isLogged())
	    			{
	    				lightStatus="GGG";
	    				mHandler.post(setLightStatus);
	    			}else{
	    				lightStatus="000";
	    				mHandler.post(setLightStatus);
	    			}
	    		}
	    		return false;
	    	}
	    });
	    
		// zhao
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		
		PowerManager pm=(PowerManager)getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "publicChannel");
		
    	setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	
    	float vol=mPref.readFloat("volume", 1.f);
    	Volume.setVolume(vol);
    	volumeController.setVolume(vol);
    	
    	((Button)findViewById(R.id.bFafauser)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(PublicWalkieTalkie.this, UsersActivity.class));
				finish();
			}
		});
		((Button)findViewById(R.id.bMessage)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(PublicWalkieTalkie.this, MessageActivity.class));
				finish();
			}
		});
		
		if (!AmazonKindle.hasMicrophone_NoWarnning(this))
        	((Button)findViewById(R.id.bAireCall)).setVisibility(View.GONE);
		
		((Button)findViewById(R.id.bAireCall)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(PublicWalkieTalkie.this, SipCallActivity.class));
				finish();
			}
		});
		//tml*** beta ui, conference
		((Button)findViewById(R.id.bConference)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it = new Intent(PublicWalkieTalkie.this, PickupActivity.class);
				it.putExtra("conference", true);
				startActivity(it);
        		finish();
			}
		});
		//tml*** temp alpha ui, CX/
	    ((Button)findViewById(R.id.bSetting)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(PublicWalkieTalkie.this, SettingActivity.class));
				finish();
			}
		});
	    //tml*** beta ui2
        boolean largeScreen = (findViewById(R.id.large) != null);
        if (largeScreen) {
        	((Button) findViewById(R.id.bSetting)).setVisibility(View.VISIBLE);
        } else {
        	((Button) findViewById(R.id.bSetting)).setVisibility(View.GONE);
        }
	    
	    if (language==-1)
		{
    		String lan=Locale.getDefault().getLanguage();
    		for (int i=0;i<15;i++)
    		{
    			if (lan.equals(langs[i]))
    			{
    				mPref.write("language",i);
    				language=i;
    				break;
    			}
    		}
		}
	    
	    if (language==-1)
	    {
	    	mPref.write("language",0);
	    	language=0;
	    }
	    
	    mHandler.postDelayed(new Runnable(){
	    	public void run(){
	    		HScroll channelView=(HScroll)findViewById(R.id.channels);
	    		if (channelView!=null)
	    			channelView.setPosition(channel-1);
	    		
	    		language=mPref.readInt("language",0);
	    		HScroll languageView=(HScroll)findViewById(R.id.languages);
	    		if (languageView!=null)
	    			languageView.setPosition(language);
	    	}
	    },500);
	    
	    //tml|sw*** disable cb chatroom
	    eject.setVisibility(View.GONE);  //2nd update, set GONE
//	    eject.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Toast tst = Toast.makeText(getApplicationContext(),
//						"Studio Chatrooms feature\n"
//						+ "will no longer be supported",
//						Toast.LENGTH_LONG);
//				tst.setGravity(Gravity.CENTER, 0, 0);
//				tst.show();
////				if (moving) return;
////				if (!bOpened)
////				{
////					moving=true;
////					AnimationSet as = new AnimationSet(false);
////				    as.setInterpolator(new AccelerateInterpolator());
////					TranslateAnimation ta = new TranslateAnimation(0,0,0,(int)(mDensity*-(padHeight+40)));
////					ta.setDuration(300);
////					as.addAnimation(ta);
////					as.setDuration(300);
////					tunningPad.startAnimation(as);
////					mHandler.postDelayed(new Runnable(){
////						public void run()
////						{
////							tunningPad.setVisibility(View.GONE);
////							moving=false;
////						}
////					}, 290);
////					
////					mHandler.postDelayed(new Runnable(){
////						public void run()
////						{
////							((HorizontalScrollView)findViewById(R.id.scroller)).scrollTo(0, 0);
////							
////							mCursorAdapter.setSel(-1);
////							
////							if (rwtSocket!=null && rwtSocket.isLogged())
////								rwtSocket.leaveChannel();
////							lightStatus="000";
////							mHandler.post(setLightStatus);
////							
////							mCursorAdapter.notifyDataSetInvalidated();
////						}
////					}, 330);
////					bOpened=true;
////				}
//			}
//		});
	    
	    retract.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (moving) return;
				if (bOpened)
				{
					moving=true;
					AnimationSet as = new AnimationSet(false);
					TranslateAnimation ta = new TranslateAnimation(0,0,(int)(mDensity*-(padHeight+40)),0);
					ta.setDuration(500);
					as.addAnimation(ta);
					as.setDuration(500);
					tunningPad.startAnimation(as);
					mHandler.postDelayed(new Runnable(){
						public void run()
						{
							tunningPad.setVisibility(View.VISIBLE);
							tunningPad.invalidate();
						}
					}, 590);
					bOpened=false;
					
					mHandler.postDelayed(new Runnable(){
						public void run()
						{
							tunningPad.setVisibility(View.VISIBLE);
							tunningPad.invalidate();
							moving=false;
							
							new Thread(new Runnable(){
								public void run()
								{
									if (rwtSocket!=null && rwtSocket.isLogged())
									{
										int ch=rwtSocket.getCurrentChannelFlag();
										int flag=(langs[language].charAt(0)<<24)|(langs[language].charAt(1)<<16)|channel;
										if (ch!=flag)
										{
											rwtSocket.leaveChannel();
											if (playing && wtp!=null)
											{
												wtp.clear();
											}
											MyUtil.Sleep(200);
											rwtSocket.channelSwitch(channel,langs[language]);
										}
										else{
											mHandler.removeCallbacks(blinking);
											lightStatus="GGG";
						    				mHandler.post(setLightStatus);
										}
									}
									
									new Thread(queryGroupsFromServer).start();
								}
							},"ACTION_TUNING switching channel").start();
						}
					}, 1500);
				}
			}
		});
	    
	    ((ImageView)findViewById(R.id.search)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((HorizontalScrollView)findViewById(R.id.scroller)).scrollTo(600, 0);
				((EditText)findViewById(R.id.topickey)).requestFocus();
			}
	    });
	    
	    ((ImageView)findViewById(R.id.searchtopic)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String topicKey=((EditText)findViewById(R.id.topickey)).getText().toString();
				if (topicKey!=null)
	    		{
	    			topicKey=topicKey.trim();
	        		if (topicKey.length()>0)
	        		{
	        			onGroupQuery(topicKey);
	        			((HorizontalScrollView)findViewById(R.id.scroller)).scrollTo(0, 0);
	        		}else
						onGroupQuery();
	    		}
				else
					onGroupQuery();
				
				InputMethodManager imm = (InputMethodManager)getSystemService(
  				      Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
			}
	    });
	    
	    ImageView add=(ImageView)findViewById(R.id.add_group);
	    
	    add.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) 
			{
				long now = new Date().getTime();
				long last = mPref.readLong("last_create_studio_group", 0);
				if (now - last < 259200000) // 3 days
				{
					Intent int2 = new Intent(getApplicationContext(), CommonDialog.class);
        	    	int2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
        	    			| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        	    			| Intent.FLAG_ACTIVITY_SINGLE_TOP);
        	    	int2.putExtra("msgContent", getString(R.string.create_wt_group_statement));
        	    	int2.putExtra("numItems", 1);
        	    	int2.putExtra("ItemCaption0", getString(R.string.done));
        	    	int2.putExtra("ItemResult0", -1);
        	    	startActivity(int2);
					return;
				}
				
				groupName=((EditText)findViewById(R.id.name)).getText().toString();
	    		if (groupName!=null)
	    		{
	    			groupName=groupName.trim();
	        		
	        		if (groupName.length()<3 || groupName.length()>120)
	        		{
	        	    	Intent int2 = new Intent(getApplicationContext(), CommonDialog.class);
	        	    	int2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
	        	    			| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
	        	    			| Intent.FLAG_ACTIVITY_SINGLE_TOP);
	        	    	int2.putExtra("msgContent", getString(R.string.nickname_invalid));
	        	    	int2.putExtra("numItems", 1);
	        	    	int2.putExtra("ItemCaption0", getString(R.string.done));
	        	    	int2.putExtra("ItemResult0", -1);
	        	    	startActivity(int2);
	        	    	return;
	        		}
	    		}
	    		else return;
	    		
	    		progress = ProgressDialog.show(PublicWalkieTalkie.this, "", getString(R.string.in_progress), true, true);
	    		
				new Thread(registerGroup,"registerGroup").start();
			}
		});
	    
	    recording=false;
    	instance=this;
    	
    	mPref.write("LastPage", 2);
    	
    	int c=mPref.readInt("studioTips",0);
		if (c<2)
		{
			mHandler.postDelayed(showTooltip, 250);
			mPref.write("studioTips",++c);
		}
		
		mHandler.postDelayed(new Runnable(){
			@Override
			public void run() {
				new Thread(queryGroupsFromServer).start();
			}
		}, 500);
		
		if (!AmazonKindle.hasMicrophone_NoWarnning(this))
	    {
	    	btnSpeaking.setEnabled(false);
	    	Toast.makeText(this, R.string.no_microphone, Toast.LENGTH_LONG).show();
	    }
		
		mHandler.postDelayed(new Runnable(){
			@Override
			public void run() {
				onGroupQuery();
			}
		}, 750);
	}
	
	Runnable registerGroup=new Runnable()
	{
		public void run()
		{
			String iso=mPref.read("iso","sa");
			String Return="";
			String myOwnNickname=mPref.read("myNickname");
			String myPhoneNumber = mPref.read("myPhoneNumber", "aaaaaa");
			String myPasswd = mPref.read("password", "1111");
			try {
				int c = 0;
				do {
					MyNet net = new MyNet(PublicWalkieTalkie.this);
					Return = net.doPostHttps("create_studio_group.php", "name=" + URLEncoder.encode(groupName+" (by "+myOwnNickname+")", "UTF-8")
							+"&iso="+iso
							+"&locked=0"
							+"&passcode="
							+"&id="+ URLEncoder.encode(myPhoneNumber,"UTF-8")
							+"&password=" + URLEncoder.encode(myPasswd, "UTF-8")
							, null);
					if (Return.startsWith("Done"))
						break;
					MyUtil.Sleep(2500);
				} while (++c < 3);
			} catch (Exception e) {}
			
			if (progress!=null && progress.isShowing())
				progress.dismiss();
			
			String channel;
			if (Return.startsWith("Done"))
			{
				channel=Return.substring(5);
				mSGDB.insertGroup(groupName+" (by "+myOwnNickname+")", channel, 50, 0, iso);
				
				mHandler.post(new Runnable(){
					public void run()
					{
						onGroupQuery();
					}
				});
				
				long now=new Date().getTime();
				mPref.writeLong("last_create_studio_group", now);
			}
		}
	};
	
	
	Runnable queryGroupsFromServer=new Runnable()
	{
		public void run()
		{
			long now = new Date().getTime();
			long last = mPref.readLong("last_query_public_studio", 0);
			if (now - last < 180000) // 3 minutes
				return;
			
			Log.d("query_studio_groups");
			String Return="";
			try {
				int c = 0;
				do {
					MyNet net = new MyNet(PublicWalkieTalkie.this);
					Return = net.doPostHttps("query_studio_groups.php", "name=1", null);
					if (Return.length()>4)
						break;
					MyUtil.Sleep(2500);
				} while (++c < 3);
			} catch (Exception e) {}
			
			if (Return.length()>4)
			{
				String [] g=Return.split(";");
				
				for (int i=0;i<g.length && i<500;i++)
				{
					String [] items=g[i].split("/");
					if (items.length>4)
					{
						try {
							int hot=Integer.parseInt(items[2]);
							String iso=(items[3]);
							int locked=Integer.parseInt(items[4]);
							mSGDB.insertGroup(URLDecoder.decode(items[0], "UTF-8"), items[1], hot, locked, iso);
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				
				Log.d("query_studio_groups done");
				
				mHandler.post(new Runnable(){
					public void run()
					{
						onGroupQuery();
					}
				});
				
				mPref.writeLong("last_query_public_studio", now);
			}
		}
	};
	
	public void onGroupQuery() {
		if (mCursor != null && !mCursor.isClosed())
			mCursor.close();
		
		String iso=mPref.read("iso","sa");
		mCursor = mSGDB.getGroups(iso);
		
		if (mCursor == null) {
			if (mCursorAdapter != null)
				mCursorAdapter.changeCursor(null);
			return;
		}

		if (mThreadQueryHandler == null)
			mThreadQueryHandler = new QueryThreadHandler(getContentResolver());
		try {
			mThreadQueryHandler.startQuery(0, null, CommonDataKinds.Phone.CONTENT_URI, 
					new String [] {CommonDataKinds.Phone.CONTACT_ID}, 
					CommonDataKinds.Phone.CONTACT_ID + "=0", null,
					null);//alec: let it query useless curser to avoid NullException
		} catch (Exception e) {}
	}
	
	public void onGroupQuery(String key) {
		
		mCursor = mSGDB.search(key);
		
		if (mCursor == null) {
			if (mCursorAdapter != null)
				mCursorAdapter.changeCursor(null);
			return;
		}

		if (mThreadQueryHandler == null)
			mThreadQueryHandler = new QueryThreadHandler(getContentResolver());
		try {
			mThreadQueryHandler.startQuery(0, null, CommonDataKinds.Phone.CONTENT_URI, 
					new String [] {CommonDataKinds.Phone.CONTACT_ID}, 
					CommonDataKinds.Phone.CONTACT_ID + "=0", null,
					null);//alec: let it query useless curser to avoid NullException
		} catch (Exception e) {}
	}

	private class QueryThreadHandler extends AsyncQueryHandler {
		public QueryThreadHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor c) {
			try {
				c = mCursor;
				if (mCursorAdapter == null) {
					mCursorAdapter = new StudioGroupsAdapter(PublicWalkieTalkie.this, mCursor);
					mGroupList.setAdapter(mCursorAdapter);
					mGroupList.setOnItemClickListener(onChannelClickListener);
				} else
					mCursorAdapter.changeCursor(mCursor);
			} catch (Exception e) {
				Log.e("pwt onQueryComplete !@#$ " + e.getMessage());
			}
		}

		@Override
		protected void onInsertComplete(int token, Object cookie, Uri uri) {
			onGroupQuery();
		}

		@Override
		protected void onUpdateComplete(int token, Object cookie, int result) {
			onGroupQuery();
		}

		@Override
		protected void onDeleteComplete(int token, Object cookie, int result) {
			onGroupQuery();
		}
	}
	
	private int channelToPermit;
	private int channelToJump;
	private int positionToJump;
	OnItemClickListener onChannelClickListener=new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			
			if (position==mCursorAdapter.getSel())
				return;
			
			String channel=(String)view.getTag();
			try{
				Log.d(channel);
				channelToJump=Integer.parseInt(channel, 16);
			}catch(Exception e){}
			
			mCursorAdapter.setSel(position);
			positionToJump=position;
			
			if (mSGDB.isLocked(channel))
			{
				channelToPermit=channelToJump;
				channelToJump=0;
				Intent it=new Intent(PublicWalkieTalkie.this, ChannelPCodeActivity.class);
				
				String storedPcode=mPref.read("channel:"+channelToPermit, null);
				it.putExtra("storedPcode",storedPcode);
				startActivityForResult(it, 16);
				return;
			}
			else{
				new Thread(tuneToGroup).start();
			}
		}
	};
	
	Runnable showTooltip=new Runnable(){
    	public void run()
    	{
    		Intent it=new Intent(PublicWalkieTalkie.this,Tooltip.class);
            it.putExtra("Content", getString(R.string.help_public_radio_2));
            startActivity(it);
    		Intent it2=new Intent(PublicWalkieTalkie.this,Tooltip.class);
            it2.putExtra("Content", getString(R.string.help_public_radio));
            startActivity(it2);
    	}
    };
	
	final Runnable updateMeter=new Runnable(){
    	public void run(){
    		meter.setPosition(Ampitude.getAmp());
    		mHandler.postDelayed(updateMeter, (recording||playing)?50:1000);
    	}
    };
    
    String lightStatus="000";
    final Runnable setLightStatus=new Runnable()
    {
    	public void run(){
    		for (int i=0;i<3;i++)
    		{
    			char a=lightStatus.charAt(i);
    			switch(a)
    			{
    			case 'R':
    				onAir[i].setImageResource(R.drawable.radio_ind_send);
    				onAir[i+3].setImageResource(R.drawable.radio_ind_send);
    				break;
    			case 'Y':
    				onAir[i].setImageResource(R.drawable.radio_ind_recv);
    				onAir[i+3].setImageResource(R.drawable.radio_ind_recv);
    				break;
    			case 'G':
    				onAir[i].setImageResource(R.drawable.radio_ind_conn);
    				onAir[i+3].setImageResource(R.drawable.radio_ind_conn);
    				break;
    			default:
    				onAir[i].setImageResource(R.drawable.radio_ind);
    				onAir[i+3].setImageResource(R.drawable.radio_ind);
    				break;
    			}
    		}
    	}
    };
	
	int fromPosition=0;
	
	private Runnable mCountingDown = new Runnable() {
		@Override
		public void run() {
			if(stop) return;
			if(timer<0) timer = 48;
			
			if(timer==0 && !sended){
				sended = true;
				doPressUp();
				return;
			}
			timer--;
			((com.pingshow.amper.view.LED)findViewById(R.id.led)).setNumber(timer);
			handlerTimer.postDelayed(mCountingDown, 1000);
		}
	};
	
	private void doPressUp(){
		handlerTimer.removeCallbacks(mCountingDown);
		if(rwt!=null) 
		{
			rwt.stop();
			rwt=null;
		
			if (!MyUtil.CheckServiceExists(PublicWalkieTalkie.this, "com.pingshow.amper.PlayService")) {
				Intent intent1 = new Intent(PublicWalkieTalkie.this, PlayService.class);
				intent1.putExtra("soundInCall", R.raw.wtf);
				startService(intent1);
			}
		}
		
		if (rwtSocket!=null)
		{
			new Thread(new Runnable(){
				public void run()
				{
					long now = new Date().getTime();
					long last = mPref.readLong("last_rwt_botton_up", 0);
					if (now - last < 60000) // 60 sec
						return;// no need to update
					if (rwtSocket!=null)
					{
						if (!rwtSocket.isLogged())
							rwtSocket.Login();
						
						if (!bOpened) rwtSocket.channelSwitch(channel, langs[language]);
					}
					mPref.writeLong("last_rwt_botton_up", now);
				}
			}).start();
		}
		
		timer = 48;
		startVibrating();
		recording=false;
		preTime = new Date().getTime();
		((com.pingshow.amper.view.LED)findViewById(R.id.led)).setNumber(numMembers);
		
		mHandler.postDelayed(new Runnable(){
			public void run()
			{
				btnSpeaking.setEnabled(true);
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
					myNetType=new NetInfo(PublicWalkieTalkie.this).netType;
				}
				
				if (rwtSocket!=null)
				{
					if (bOpened)
						rwtSocket.channelSwitch(channelToJump);
					else
						rwtSocket.channelSwitch(channel,langs[language]);
				}
				/*
				mHandler.removeCallbacks(blinking);
				mHandler.postDelayed(blinking,0);*/
			}
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
//		MobclickAgent.onResume(this);
		
		if (!wl.isHeld()) wl.acquire();
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(Global.ACTION_PLAY_AUDIO);
		filter.addAction(Global.ACTION_TUNING);
		filter.addAction(Global.ACTION_TUNING_START);
		filter.addAction(Global.Action_Raw_Audio_Playback);
		filter.addAction(Global.Action_Chatroom_Members);
		registerReceiver(publicRadioReceiver, filter);
		
		new Thread(loginRWTServer,"loginRWTServer").start();
		
		mHandler.postDelayed(updateMeter, 2000);
	}
	
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
	
	private void startVibrating(){
		long[] patern = {0,40,1000};
		mVibrator.vibrate(patern, -1);
	}
	
	@Override
	protected void onPause() {
		if (Integer.parseInt(Build.VERSION.SDK) >= 5 && Integer.parseInt(Build.VERSION.SDK) <= 7)
			reenableKeyguard();
		mHandler.removeCallbacks(updateMeter);
		unregisterReceiver(publicRadioReceiver);
		
		if (wl!=null) wl.release();
		if (recording)
		{
			if(rwt!=null) rwt.stop();
			rwt=null;
			handlerTimer.removeCallbacks(mCountingDown);
			timer = 48;
		}
		if (isFinishing())
		{
			instance=null;
			if (wtp!=null)
			{
				wtp.clear();
			}
		}
		else{
			if (rwtSocket!=null && rwtSocket.isLogged())
				rwtSocket.leaveChannel();
		}
		if (wtp!=null)
		{
			wtp.release();
			wtp=null;
			playing=false;
		}
//		MobclickAgent.onPause(this);
		super.onPause();
	}

	protected void onDestroy() {
		if (mCursor != null && !mCursor.isClosed())
			mCursor.close();
		mHandler.removeCallbacks(loginRWTServer);
		mHandler.removeCallbacks(mCountingDown);
		mHandler.removeCallbacks(blinking);
		if(mVibrator!=null) mVibrator.cancel();
		if(mADB!=null && mADB.isOpen()) mADB.close();
		if(mSGDB!=null && mSGDB.isOpen()) mSGDB.close();
		
		if (rwtSocket!=null && rwtSocket.isLogged())
			rwtSocket.leaveChannel();
		
		mPref.writeFloat("volume", volumeController.getVolume());
		
		setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
		
		System.gc();
		System.gc();
		super.onDestroy();
	};
	
	int blinkSeq=0;
	final Runnable blinking=new Runnable(){
		public void run(){
			if (blinkSeq==-1) return;
			if (!playing && !recording)
			{
				if (rwtSocket!=null && rwtSocket.isLogged())
				{
					int r=blinkSeq%4;
					switch (r)
					{
					case 0:
						lightStatus="00G";break;
					case 1:
						lightStatus="0G0";break;
					case 2:
						lightStatus="G00";break;
					case 3:
						lightStatus="0G0";break;
					}
    				mHandler.post(setLightStatus);
    				blinkSeq++;
    				mHandler.postDelayed(blinking,250);
				}else{
					lightStatus="000";
    				mHandler.post(setLightStatus);
				}
			}
		}
	};
	
	public void onDisconnect()
	{
		try{
			lightStatus="000";
			mHandler.post(setLightStatus);
		}catch(Exception e){}
	}
	
	int CurrentRecvIdx=0;
	static int numMembers=0;
	
	BroadcastReceiver publicRadioReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(Global.Action_Chatroom_Members)){
				numMembers=intent.getIntExtra("num", 0);
				mHandler.removeCallbacks(blinking);
				if (!recording && !playing){
					((com.pingshow.amper.view.LED)findViewById(R.id.led)).setNumber(numMembers);
					lightStatus="GGG";
    				mHandler.post(setLightStatus);
				}
			}
			else if(intent.getAction().equals(Global.Action_Raw_Audio_Playback)){
				if (intent.getBooleanExtra("private", true)) return;//private
				
				if (recording) return;
				if (WalkieTalkieDialog.getInstance()!=null) return;
				
				int idx=intent.getIntExtra("from", 0);
				//int seq=intent.getIntExtra("seq", 0);
				
				if (idx<50||newInChannel||CurrentRecvIdx==0) 
					CurrentRecvIdx=idx;
				
				if (!lightStatus.equals("YYY"))
				{
					lightStatus="YYY";
					mHandler.post(setLightStatus);
				}
				
				if (wtp==null && CurrentRecvIdx==idx)
				{
					wtp=new RealTimeWTPlayer_WB(PublicWalkieTalkie.this, myNetType, true);
					try{
						int len=intent.getIntExtra("length", 0);
						wtp.append(intent.getByteArrayExtra("raw"), len);
						wtp.run();

						btnSpeaking.setEnabled(false);
						
						startVibrating();
					}catch(Exception e){}
					
					newInChannel=false;
				}
				else if (CurrentRecvIdx==idx)
				{
					try{
						int len=intent.getIntExtra("length", 0);
						wtp.append(intent.getByteArrayExtra("raw"), len);
					}catch(Exception e){}
				}
				else
					return;
				playing = true;
			}
			else if(intent.getAction().equals(Global.ACTION_TUNING_START))
			{
				if(recording||rwtSocket==null) return;
				if(intent.getIntExtra("bar", 0)==0)//Channels
					channel=intent.getIntExtra("pos", 1)+1;
				else
					language=intent.getIntExtra("pos", 0);
				int ch=rwtSocket.getCurrentChannel();
				int flag=(langs[language].charAt(0)<<24)|(langs[language].charAt(1)<<16)|channel;
				if (ch!=flag)
				{
					mHandler.removeCallbacks(blinking);
					mHandler.postDelayed(blinking,0);
				}
				else{
					mHandler.removeCallbacks(blinking);
					lightStatus="GGG";
    				mHandler.post(setLightStatus);
				}
			}
			else if(intent.getAction().equals(Global.ACTION_TUNING))
			{
				newInChannel=true;
				
				if(intent.getIntExtra("bar", 0)==0){//Channels
					channel=intent.getIntExtra("pos", 1)+1;
					mPref.write("channel",channel);
				}
				else{
					language=intent.getIntExtra("pos", 0);
					mPref.write("language",language);
				}
				
				new Thread(new Runnable(){
					public void run()
					{
						if (rwtSocket!=null && rwtSocket.isLogged())
						{
							int ch=rwtSocket.getCurrentChannel();
							int flag=(langs[language].charAt(0)<<24)|(langs[language].charAt(1)<<16)|channel;
							if (ch!=flag)
							{
								rwtSocket.leaveChannel();
								if (playing && wtp!=null)
								{
									wtp.clear();
								}
								
								//alec: 
								MyUtil.Sleep(1000);
								rwtSocket.channelSwitch(channel,langs[language]);
							}
							else{
								mHandler.removeCallbacks(blinking);
								lightStatus="GGG";
			    				mHandler.post(setLightStatus);
							}
						}
						else if (rwtSocket!=null)
							rwtSocket.Login();
					}
				},"ACTION_TUNING switching channel").start();
			}
			else if(intent.getAction().equals(Global.ACTION_PLAY_AUDIO))
			{
				if(intent.getIntExtra("clear", 0)==1){
					if (!MyUtil.CheckServiceExists(PublicWalkieTalkie.this, "com.pingshow.amper.PlayService")) {
						Intent intent1 = new Intent(PublicWalkieTalkie.this, PlayService.class);
						intent1.putExtra("soundInCall", R.raw.wtf);
						startService(intent1);
					}
					playing=false;
					if (wtp!=null)
					{
						wtp.release();
						wtp=null;
						Log.d("**** wtp released ****");
					}
					if (rwtSocket!=null && rwtSocket.isLogged())
					{
						lightStatus="GGG";
	    				mHandler.post(setLightStatus);
					}else{
						lightStatus="000";
	    				mHandler.post(setLightStatus);
					}
					
					if (AmazonKindle.hasMicrophone_NoWarnning(PublicWalkieTalkie.this))
						btnSpeaking.setEnabled(true);
					CurrentRecvIdx=0;
				}
			}
		}
	};
	
	public void lockScreenOrientation() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		/*
		if (Integer.parseInt(Build.VERSION.SDK) < 8) {
	    	return;
		}
		try{
			switch (((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRotation()) {
		    case Surface.ROTATION_90:
		    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		        break;
		    case Surface.ROTATION_180:
		    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		        break;
		    case Surface.ROTATION_270: 
		    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
		        break;
		    default:
		    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		    }
		}catch(Exception e){}*/
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
	
	Runnable tuneToGroup=new Runnable(){
		public void run()
		{
			if (bOpened && rwtSocket!=null && rwtSocket.isLogged())
			{
				int ch=rwtSocket.getCurrentChannelFlag();
				if (ch!=channelToJump)
				{
					mHandler.removeCallbacks(blinking);
					mHandler.postDelayed(blinking,0);
					
					mCursorAdapter.setSel(positionToJump);
					
					rwtSocket.leaveChannel();
					if (playing && wtp!=null)
						wtp.clear();
					
					MyUtil.Sleep(1000);
					rwtSocket.channelSwitch(channelToJump);
					
					mHandler.post(new Runnable(){
						public void run()
						{
							mCursorAdapter.notifyDataSetInvalidated();
						}
					});
				}
				else
					mHandler.removeCallbacks(blinking);
			}
		}
	};
	
	private String passcode;
	@Override  
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		super.onActivityResult(requestCode, resultCode, data);  
		if(requestCode==16)
		{
			if (resultCode==RESULT_OK) {
				passcode=data.getStringExtra("pcode_input");
				progress = ProgressDialog.show(PublicWalkieTalkie.this, "", getString(R.string.in_progress), true, true);
				new Thread(permitToGroup).start();
			}
		}
	}
	
	private Runnable permitToGroup=new Runnable()
	{
		public void run()
		{
			String Return="";
			try {
				int c = 0;
				do {
					MyNet net = new MyNet(PublicWalkieTalkie.this);
					Return = net.doPostHttps("permit_studio_groups.php", 
							"&channel="+Integer.toHexString(channelToPermit)
							+"&passcode="+passcode, null);
					if (Return.length()>0)
						break;
					MyUtil.Sleep(2500);
				} while (++c < 2);
			} catch (Exception e) {}
			
			if (progress!=null && progress.isShowing())
				progress.dismiss();
			
			if (Return.startsWith("Done"))
			{
				mPref.write("channel:"+channelToPermit, passcode);
				channelToJump=channelToPermit;
				new Thread(tuneToGroup).start();
			}
			else if (Return.startsWith("Failed"))
			{
				mPref.write("channel:"+channelToPermit, null);
				Intent int2 = new Intent(getApplicationContext(), CommonDialog.class);
    	    	int2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
    	    			| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
    	    			| Intent.FLAG_ACTIVITY_SINGLE_TOP);
    	    	int2.putExtra("msgContent", getString(R.string.password_error));
    	    	int2.putExtra("numItems", 1);
    	    	int2.putExtra("ItemCaption0", getString(R.string.done));
    	    	int2.putExtra("ItemResult0", -1);
    	    	startActivity(int2);
			}
		}
	};
	
	//tml|bj*** neverdie
	public void neverSayNeverDie(Context context) {
		if (AireJupiter.getInstance()==null) {
			Log.e("AireJupiter is NULL");
			Intent vip1 = new Intent(context, AireVenus.class);
			context.stopService(vip1);
			Intent vip2 = new Intent(context, AireJupiter.class);
			context.stopService(vip2);
			
			Intent vip00 = new Intent(context, AireJupiter.class);
			context.startService(vip00);
		}
	}
	
}
