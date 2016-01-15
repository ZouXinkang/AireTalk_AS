package com.pingshow.airecenter;

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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.content.ClipboardManager;
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
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.cons.Key;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.GroupDB;
import com.pingshow.airecenter.db.SmsDB;
import com.pingshow.airecenter.view.AudioMsgPlayer;
import com.pingshow.airecenter.view.ProgressBar;
import com.pingshow.codec.VoiceMemoPlayer_NB;
import com.pingshow.codec.VoicePlayer2_MP;
import com.pingshow.gif.GifView;
import com.pingshow.gif.GifView.GifImageType;
import com.pingshow.network.MyNet;
import com.pingshow.network.NetInfo;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.util.OpenDifferentFile;
import com.pingshow.util.ResizeImage;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;

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
	private ArrayList<String> addressList=new ArrayList<String>();
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
	private Button mVoice;
	private ImageView speaker;
	private AnimationDrawable spAnimation;
	private boolean state = true;
	private String curFilePath = null;
	private boolean AnimationDrawablestate = true;
	private int listnumber = 90;
	private ListView listview;
	private Drawable myphoto;
	private Drawable friendPhoto;
	private Map<Integer, Drawable> friendsPhotoMap = new HashMap<Integer, Drawable>();
	public static boolean fileDownloading = false;
	public static boolean fileUploading = false;
	public static long smsId = 0;
	private long rowid;
	private Map<Long, SpannableString> spannableCache = new HashMap<Long, SpannableString>();
	private long rowID=0;
	private boolean isVideo = false;
	private Bitmap videobitmap=null;
	private int voicetime = 0;
	private AmpUserDB mADB;
	private boolean largeScreen=true;
	private float size=24.f;
	private float size2=67.f;
	private boolean inGroup=false;
	private boolean needScrollToEnd=true;
	private int mGroupID;
	boolean smileClicked=false;
	private boolean mFromCallMode = false;
	
	long msg_smsid;
	long msg_org_smsid;
	
	static public ConversationActivity msgpage;
	
	static public ConversationActivity getInstance()
	{
		return msgpage;
	}
	
	private Map<String, ProgressBar> mProgress = new HashMap<String, ProgressBar>();
	public ProgressBar getProgressBar(String fn)
	{
		ProgressBar p=mProgress.get(fn);
		return p;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.conversation);
		this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

		size=36.f;
		size2=100.f;
		
		Intent intent = getIntent();
		mAddress = intent.getStringExtra("SendeeNumber");
		mNickname = intent.getStringExtra("SendeeDisplayname");
		mContactId = intent.getLongExtra("SendeeContactId", -1);
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		float wDensity = getResources().getDisplayMetrics().density;
		
		inGroup=mAddress.startsWith("[<GROUP>]");
		mFromCallMode = intent.getBooleanExtra("FromCallMode", false);
		
		mADB = new AmpUserDB(ConversationActivity.this);
		mADB.open();
		mPref = new MyPreference(this);
		
		mDB = new SmsDB(this);
		mDB.open();

		ArrangeTalkList();
		
		mIdx=mADB.getIdxByAddress(mAddress);
		
		myIdx=Integer.parseInt(mPref.read("myID","0"), 16);

		msgListAdapter = new MsgListAdapter(this);

		listview = (ListView) findViewById(R.id.talklist);
		
		boolean success=false;
		String conversationBg = mPref.read("BackgroundImage", null); // set talk list background
		if(conversationBg!=null && new File(conversationBg).exists()){
			Bitmap bgImage = null;
			boolean HDSize=false;
			
			try{
				BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
				bitmapOptions.inPurgeable=true;
				bitmapOptions.inJustDecodeBounds=true;
				BitmapFactory.decodeFile(conversationBg, bitmapOptions);
				if (bitmapOptions.outHeight>1000 || bitmapOptions.outWidth>1900)
					HDSize=true;
	    	}catch(Exception e){
	    	}catch(OutOfMemoryError e){
	    		System.gc();
				System.gc();
	    	}
	    	
			try {
				bgImage=ImageUtil.loadBitmapSafe(HDSize?2:1, conversationBg);
				if (bgImage!=null)
				{
					Drawable drawable = new BitmapDrawable(bgImage);
					if(drawable!=null)
					{
						((ImageView)findViewById(R.id.bkimg)).setImageDrawable(drawable);
						success=true;
					}
				}
			}catch (OutOfMemoryError e) {}
		}
		else
			((ImageView)findViewById(R.id.bkimg)).setVisibility(View.GONE);
		
		DialerFrame.setFrame(this, findViewById(android.R.id.content));
		
		if (inGroup)
		{
			((RelativeLayout)findViewById(R.id.members)).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.mood)).setVisibility(View.GONE);  //tml*** beta ui3
		} else {
			//tml*** beta ui3
			String mood = mADB.getMoodByAddress(mAddress);
			if (mood != null && mood.length() > 0) {
				((TextView) findViewById(R.id.mood)).setText(mood);
			} else {
				((TextView) findViewById(R.id.mood)).setVisibility(View.GONE);
			}
		}
		
		listview.setAdapter(msgListAdapter);
		if (TalkList.size() > 0) {
			listview.setSelection(TalkList.size() - 1);
		}

		TextView tv = (TextView) findViewById(R.id.sendee);
		
		if (inGroup)
		{
			String szGroup=getResources().getString(R.string.the_group);
			tv.setText(szGroup+": "+mNickname);
			mGroupID=Integer.parseInt(mAddress.substring(9));
			GroupDB mGDB=new GroupDB(this);
			mGDB.open(true);
			sendeeList=mGDB.getGroupMembersByGroupIdx(mGroupID);
//			mGDB.close();
			try{
				for (int i=0;i<sendeeList.size();i++)
					addressList.add(mADB.getAddressByIdx(Integer.parseInt(sendeeList.get(i))));
			}catch(Exception e){
			} finally {
				mGDB.close();  //tml*** sqlite leak
			}
			arrangePickedUsers();
		}
		else{
			tv.setText(mNickname);
			if(mNickname==null) tv.setText(mAddress);
		}
		
		getphoto();
		
		listview.setOnItemLongClickListener(mLongPressTalkListItem);
		
		((Button) findViewById(R.id.voicesms)).setOnClickListener(this);
		((Button) findViewById(R.id.media)).setOnClickListener(this);
		((Button) findViewById(R.id.filesms)).setOnClickListener(this);
		((Button) findViewById(R.id.sendmsg)).setOnClickListener(this);
		((Button) findViewById(R.id.call)).setOnClickListener(this);  //tml*** beta ui3
		((Button) findViewById(R.id.videocall)).setOnClickListener(this);  //tml*** beta ui3
		
		if (!MyUtil.canHandleCameraIntent(getApplicationContext())){
			((ImageView)findViewById(R.id.photosms)).setEnabled(false);
		}
		
		speaker = (ImageView) findViewById(R.id.speaker);
		spAnimation = (AnimationDrawable) speaker.getDrawable();

		((ImageView) findViewById(R.id.cancel)).setOnClickListener(this);
		((ImageView) findViewById(R.id.removeall)).setOnClickListener(this);
		((ImageView) findViewById(R.id.smile)).setOnClickListener(this);
		((ImageView) findViewById(R.id.attachments)).setOnClickListener(this);  //tml*** beta ui3

		//tml*** beta ui3
		if (inGroup) {
			((Button) findViewById(R.id.call)).setText(getResources().getString(R.string.conference));
//			if (!mPref.readBoolean("BROADCAST", false)) {
			if (!true) {
				((Button) findViewById(R.id.videocall)).setVisibility(View.INVISIBLE);
			} else {
				((Button) findViewById(R.id.videocall)).setVisibility(View.VISIBLE);
				((Button) findViewById(R.id.videocall)).setText(getResources().getString(R.string.broadcast));
			}
		}
		
		mDensity = getResources().getDisplayMetrics().density;

		mSend = (Button) findViewById(R.id.sendmsg);
		mVoice = (Button) findViewById(R.id.voicesms);
		mInput = (EditText) findViewById(R.id.msginput);
		
		mInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (!isSmile)
					return;
				Smiley sm = new Smiley();

				if (sm.hasSmileys(s.toString()) > 0) {
					SpannableString spannable = new SpannableString(s
							.toString());
					for (int i = 0; i < Smiley.MAXSIZE; i++) {
						for (int j = 0; j < sm.getCount(i); j++) {
							ImageSpan icon = new ImageSpan(
									ConversationActivity.this, R.drawable.sm01
											+ i, ImageSpan.ALIGN_BOTTOM);
							spannable.setSpan(icon, sm.getStart(i, j),
									sm.getEnd(i, j),
									SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
					}
					mInput.setText(spannable);
					mInput.setSelection(cursorPos);
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
		((EditText) findViewById(R.id.msginput)).requestFocus();  //tml*** prefocus

		if (intent.getIntExtra("attachment", 0) == 1)
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					String audioPath = getIntent().getStringExtra("audioPath");
					if (audioPath != null) {
						if (mp2 != null && mp2.isPlaying())
							return;
						if (myVP1 != null && myVP1.isPlaying())
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
		this.registerReceiver(HandleListChanged, intentToReceiveFilter);
		
		msgpage=this;
		
		//tml*** beta ui
		if (AireVenus.callstate_AV != null) {
			((Button)findViewById(R.id.voicesms)).setAlpha(50);
			((Button)findViewById(R.id.voicesms)).setEnabled(false);
		}
		
		//tml*** beta ui3
		if ((mAddress.equals("support") && mNickname.equals("Support")) || mIdx == 2) {
			((Button) findViewById(R.id.call)).setText(getResources().getString(R.string.helper_faq));
			((Button) findViewById(R.id.call))
					.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
		}
		if (DialerActivity.getDialer() != null) {
			((Button) findViewById(R.id.call)).setAlpha(50);
			((Button) findViewById(R.id.call)).setEnabled(false);
			((Button) findViewById(R.id.videocall)).setAlpha(50);
			((Button) findViewById(R.id.videocall)).setEnabled(false);
		}
		if (!mPref.readBoolean("video_support", true)) {
			((Button) findViewById(R.id.videocall)).setEnabled(false);
		} else {
			((Button) findViewById(R.id.videocall)).setEnabled(true);
		}
		//***tml
		
	}
	
	private void arrangePickedUsers()
	{
		if (!inGroup || sendeeList==null || sendeeList.size()==0) return;
		
		RelativeLayout s=(RelativeLayout)findViewById(R.id.members);
		s.removeAllViews();
		
		try{
			int count=sendeeList.size();
			int width=(int)((float)s.getWidth()/mDensity)-90;
			if (width<0) {
				int w=getWindowManager().getDefaultDisplay().getWidth();
				width = (int)((float)w/mDensity)-120;
			}
			int space=width/count;
			int w=120;
			
			for(int i=0;i<count;i++)
			{
				ImageView a=new ImageView(this);
				TextView t=new TextView(this);
				a.setClickable(true);
				int idx=Integer.parseInt(sendeeList.get(i));
				
				Drawable photo=ImageUtil.getUserPhoto(this, idx);
				if (photo!=null)
					a.setImageDrawable(photo);
				else
					a.setImageResource(R.drawable.bighead);
				
				friendsPhotoMap.put(Integer.valueOf(idx), photo);
				
				RelativeLayout.LayoutParams lp=null;
				lp = new RelativeLayout.LayoutParams((int)(mDensity*w), (int)(mDensity*w));
				lp.leftMargin=(int)(mDensity*space)*(count-i-1);
				lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				a.setId(i*2+1);
				s.addView(a, lp);
				
				lp = new RelativeLayout.LayoutParams((int)(mDensity*(space-w)), (int)(mDensity*w));
				lp.addRule(RelativeLayout.RIGHT_OF, a.getId());
				lp.addRule(RelativeLayout.CENTER_VERTICAL);
				t.setGravity(Gravity.CENTER_VERTICAL);
				t.setText(mADB.getNicknameByIdx(idx));
				t.setTextSize(18);
				t.setMaxLines(2);
				lp.leftMargin=5;
				t.setTextColor(0xff27394b);
				s.addView(t, lp);
				
				if (i<count-1)
				{
					AnimationSet as = new AnimationSet(false);
					as.setInterpolator(new AccelerateInterpolator());
					TranslateAnimation ta = new TranslateAnimation(mDensity*(-width+space),0,0,0);
					ta.setDuration(500+100*(count-i));
					as.addAnimation(ta);
					as.setDuration(500+100*(count-i));
					a.startAnimation(as);
					t.startAnimation(as);
				}
			}
		}catch(Exception e){}
	}

	void getphoto() 
	{
		try{
			friendPhoto=ImageUtil.getUserPhoto(this, mIdx);
			
			if (friendPhoto == null)
				friendPhoto = getResources().getDrawable(R.drawable.bighead);
	
			String path=mPref.read("myPhotoPath", null);
			if (path != null && path.length() > 0)
				myphoto=ImageUtil.getBitmapAsRoundCorner(path, 2, 0);
			
			if (myphoto==null)
				myphoto=getResources().getDrawable(R.drawable.bighead);
		}catch(Exception e){}
	}
	
	@Override
	protected void onDestroy() {
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
		} finally {
			//tml*** sqlite leak
			if (msgListAdapter!=null)
			{
				msgListAdapter.clear();
			}
			if (mDB != null && mDB.isOpen())
				mDB.close();
			if (mADB!=null && mADB.isOpen()) 
				mADB.close();
			//***tml
		}
//		if (msgListAdapter!=null)
//		{
//			msgListAdapter.clear();
//		}
//		if (mDB != null && mDB.isOpen())
//			mDB.close();
//		if (mADB!=null && mADB.isOpen()) 
//			mADB.close();
		unregisterReceiver(HandleListChanged);
		
		spAnimation=null;
		
		String draft = mInput.getText().toString().trim();
		if (draft.length() != 0 && !draft.equals(R.string.textinput)) {
			mPref.write("draft" + mContactId, draft);
			//Toast.makeText(getApplicationContext(), getString(R.string.savedraft), Toast.LENGTH_SHORT).show();
		} else if (draft.length() == 0) {
			mPref.delect("draft" + mContactId);
		}
		
		if (null!=SrcAudioPath && mAttached==1) {
			File file = new File(SrcAudioPath);
			if (file.exists())
				file.delete();
		}
		spannableCache=null;
		msgpage=null;
		super.onDestroy();
		System.gc();
		System.gc();
	}
	
	private PowerManager.WakeLock mWakeLock;
	void startWakeLock()
	{
		if (mWakeLock == null) {
			PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
					PowerManager.ACQUIRE_CAUSES_WAKEUP, "PlayingVoiceMemo");
		}
		
		mWakeLock.acquire();
	}
	
	void stopWakeLock()
	{
		try{
			if (mWakeLock!=null) mWakeLock.release();
		}catch (Exception e){}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (Integer.parseInt(Build.VERSION.SDK) >= 5
				&& Integer.parseInt(Build.VERSION.SDK) <= 7)
			disableKeyguard();
		if (mPref.read("draft" + mContactId, null)!=null && state) {
			String draft1 = mPref.read("draft" + mContactId);
			mInput.setText(draft1);
			mInput.setSelection(draft1.length());
		}
		sender = mAddress;
	
		if (needScrollToEnd)
			mHandler.post(RefreshStatus);
		
		int unread = mDB.getUnreadCountByAddress(mAddress);
		if (unread > 0) {
			
			UserPage.forceRefresh=true;
			
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mDB.setMessageReadByAddress(ConversationActivity.this, mAddress);
				}
			}, 200);
		}
		
		DialerFrame.checkEmbeddedDialer(findViewById(android.R.id.content));
		
//		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
		if (Integer.parseInt(Build.VERSION.SDK) >= 5
				&& Integer.parseInt(Build.VERSION.SDK) <= 7)
			reenableKeyguard();
		
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
		
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
//		MobclickAgent.onPause(this);
		stopWakeLock();
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
		
		if(c!=null && !c.isClosed()) c.close();
		
		if (TalkList.size() < listnumber) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (moresms!=null) moresms.setVisibility(View.GONE);
					if (callbtn!=null) callbtn.setVisibility(View.VISIBLE);
					if (profilebtn!=null) profilebtn.setVisibility(View.VISIBLE);
				}
			});
		} else {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (moresms!=null) moresms.setVisibility(View.VISIBLE);
					if (callbtn!=null) callbtn.setVisibility(View.GONE);
					if (profilebtn!=null) profilebtn.setVisibility(View.VISIBLE);
				}
			});
		}
	}

	private AdapterView.OnItemLongClickListener mLongPressTalkListItem = new AdapterView.OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> av, View v, int position,
				long id) {
			return handleLongPress(position - 1);
//			return handleLongPress(position);
		}
	};

	private OnLongClickListener mLongPressBalloonView = new OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			int position = Integer.parseInt(v.getTag().toString());
			return handleLongPress(position);
		}
	};
	
	final int action_nothing=0;
	final int action_resend=1;
	final int action_save_as=2;
	int item2_action;

	boolean handleLongPress(int position) {
		if (mp2 != null && mp2.isPlaying())
			return false;
		if (myVP1 != null && myVP1.isPlaying())
			return false;
		final SMS msg = TalkList.get(position);
		msg_smsid = msg.smsid;
		msg_org_smsid = msg.org_smsid;
		CharSequence[] d = null;
		
		item2_action=action_nothing;
		
		if (msg.att_path_img == null && msg.attached != 0) {
			if (msg.status != -2 && msg.status != 1 && msg.type == 2) {
				d = new CharSequence[3];
				d[2] = getResources().getString(R.string.send_again);
				item2_action=action_resend;
//			} else
//				d = new CharSequence[2];
			//tml*** browser save
			} else {
				d = new CharSequence[3];
				d[2] = getResources().getString(R.string.save_file_SD);
				item2_action=action_save_as;
			}
			//***tml
		} else {
			if (msg.status != -2 && msg.status != 1 && msg.type == 2) {
				d = new CharSequence[3];
				d[2] = getResources().getString(R.string.send_again);
				item2_action=action_resend;
			} else if (msg.att_path_img != null){
				d = new CharSequence[3];
				d[2] = getResources().getString(R.string.save_photo_SD);
				item2_action=action_save_as;
			} else
				d = new CharSequence[2];
			//tml*** browser save
//			} else {
//				d = new CharSequence[3];
//				d[2] = getResources().getString(R.string.save_file_SD);
//				item2_action=action_save_as;
//			}
			//***tml
		}
		d[0] = getResources().getString(R.string.delete_msg);
		d[1] = getResources().getString(R.string.copysms);
	  
		new AlertDialog.Builder(ConversationActivity.this)
				.setTitle(mNickname)
				.setItems(d, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (which == 0) {
							new AlertDialog.Builder(ConversationActivity.this)
									.setTitle(R.string.delete_confirm)
									.setMessage(R.string.delete_thread_confirm)
									.setPositiveButton(
											R.string.yes,
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialog,
														int whichButton) {
													
													/*
													if (ConversationActivity.fileUploading)
													{
														for (int i=0;i<TalkList.size();i++)
														{
															final SMS msg = TalkList.get(i);
															if (msg_smsid == msg.smsid)
															{
																new MyNet(ConversationActivity.this).stopUploading(msg.att_path_aud);
																ConversationActivity.fileUploading=false;
																break;
															}
														}
													}*/
													
													mDB.deleteSingleMsg(
															msg_smsid,
															msg_org_smsid);
													spannableCache.remove(msg.smsid);
													ArrangeTalkList();
													msgListAdapter
															.notifyDataSetChanged();
													Intent it = new Intent(Global.Action_HistroyThread);
													sendBroadcast(it);
												}
											})
									.setNegativeButton(
											R.string.no,
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialog,
														int whichButton) {
												}
											}).show();
						} else if (which == 1) {
							if (msg.content.startsWith("(g.f"))
							{
								copyToClipboard("");
								return;
							}
							copyToClipboard(msg.content);
						} else if (which == 2) {
							if (item2_action==action_save_as){
								if (!MyUtil.checkSDCard(getApplicationContext()))
									return;
//								String aSrcImagePath = msg.att_path_img;
//								File fromFile = new File(aSrcImagePath);
//								String[] items = aSrcImagePath.split("/");
//								File toFile = new File(
//										Global.SdcardPath_downloads
//												+ items[items.length - 1]);
//								MyUtil.copyFile(fromFile, toFile, true,
//										ConversationActivity.this);
								//tml*** browser save
								if (msg.att_path_img != null) {
									String aSrcImagePath = msg.att_path_img;
									File fromFile = new File(aSrcImagePath);
									String[] items = aSrcImagePath.split("/");
									String toPath = getMIMEdest(items[items.length - 1]);
									File toFile = new File(toPath + items[items.length - 1]);
									Log.e("conv1>> " + fromFile.getPath()
											+ "\n>> " + toFile.getPath());
									if (checkFileExist(fromFile.getPath())) {
										Toast tst = Toast.makeText(getApplicationContext(),
												"Saved to: " + toPath,
												Toast.LENGTH_LONG);
										LinearLayout tstLayout = (LinearLayout) tst.getView();
									    TextView tstTV = (TextView) tstLayout.getChildAt(0);
									    tstTV.setTextSize(30);
									    tst.show();
										MyUtil.copyFile(fromFile, toFile, true,
												getApplicationContext());
									}
								} else if (msg.att_path_aud != null) {
									String aSrcImagePath = msg.att_path_aud;
									File fromFile = new File(aSrcImagePath);
									String[] items = aSrcImagePath.split("/");
									String toPath = getMIMEdest(items[items.length - 1]);
									File toFile = new File(toPath + items[items.length - 1]);
									Log.e("conv1>> " + fromFile.getPath()
											+ "\n>> " + toFile.getPath());
									if (checkFileExist(fromFile.getPath())) {
										Toast tst = Toast.makeText(getApplicationContext(),
												"Saved to: " + toPath,
												Toast.LENGTH_LONG);
										LinearLayout tstLayout = (LinearLayout) tst.getView();
									    TextView tstTV = (TextView) tstLayout.getChildAt(0);
									    tstTV.setTextSize(30);
									    tst.show();
										MyUtil.copyFile(fromFile, toFile, true,
												getApplicationContext());
									}
								} else {
									Toast.makeText(ConversationActivity.this,
											getString(R.string.file_err),
											Toast.LENGTH_SHORT).show();
								}
								//***tml
							} else if (item2_action==action_resend) 
							{
								/*
								if (ConversationActivity.fileUploading) {
									Toast.makeText(getApplicationContext(),
											getString(R.string.fileuploading),
											Toast.LENGTH_SHORT).show();
									return;
								}*/
								SendAgent failagent = new SendAgent(
										ConversationActivity.this,
										myIdx, mIdx,false);
								failagent.setRowId(msg.smsid);
								if(msg.attached==9){
									msg.content = msg.content.substring(msg.content.indexOf("(vdo)"))+"1";
									msg.attached = 8;
								}else if(msg.attached==8){
									msg.content = msg.content.substring(msg.content.indexOf("(fl)"));
								}
								failagent.onSend(mAddress, msg.content,
										msg.attached, msg.att_path_aud, msg.att_path_img,
										false);
								mDB.updateFailCountById((int) msg.smsid, 0);
							}
						}
					}
				}).show();
		return true;
	}
	//tml*** copy clipboard
	private void copyToClipboard(String text) {
		if (text.contains("(Vm)"))
			text = text.replace("(Vm)", "");
		if (text.contains("(iMG)"))
			text = text.replace("(iMG)", "");
		if (text.contains("(vdo)"))
			text = text.replace("(vdo)", "");
		if (text.contains("(fl)"))
			text = text.replace("(fl)", "");
		ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		clipboardManager.setText(text);
//		clipboardManager.setPrimaryClip(clip);
		Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT)
				.show();
	}

	OnClickListener mOnClickPhoto=new OnClickListener() {
		public void onClick(View v) {
			Intent it=new Intent(ConversationActivity.this, FunctionActivity.class);
			it.putExtra("Contact_id", mContactId);
			it.putExtra("Address", mAddress);
			it.putExtra("Nickname", mNickname);
			if (mContactId>0)
				it.putExtra("AireNickname", mADB.getNicknameByAddress(mAddress));
			it.putExtra("Idx", mIdx);
			it.putExtra("fromConversation", true);
			startActivity(it);
		}
	};

	public String getPath(Uri uri, int requestCode) {
		try{
			Log.e("tml CA uriPath=" + uri.getPath() + " string=" + uri.toString());
			if (uri.toString().startsWith("content:")) {
				String result=null;
				Uri contentUri;
				Cursor cursor;
				String[] column = { MediaStore.Images.Media.DATA };
				if (requestCode==101)
				{
					String uriStr = uri.toString();
					String id = null;
//					if (uriStr.contains("%3A"))
//						id = uriStr.split("%3A")[1];
//					else if (uriStr.contains(":"))
//						id = uriStr.split(":/")[1];
//					String sel = MediaStore.Images.Media._ID + "=?";
//					Log.e("tml CA cursor: contentUri=" + contentUri.toString() + " column=" + column[0] + " sel=" + sel + " id=" + id);
//					cursor = getContentResolver().query(contentUri, column, sel, new String[]{ id }, null);
					//tml*** chat video bug
					String sel = MediaStore.Images.Media._ID + "=?";
					if (uriStr.contains("%3A")) {
						id = uriStr.split("%3A")[1];
						contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
						Log.e("tml CA cursor1: contentUri=" + contentUri.toString() + " sel=" + sel + " id=" + id);
						cursor = getContentResolver().query(contentUri, column, sel, new String[]{ id }, null);
					} else {
						contentUri = uri;
						Log.e("tml CA cursor2: contentUri=" + contentUri.toString());
						cursor = getContentResolver().query(contentUri, column, null, null, null);
					}
				}else{
					contentUri = uri;
					Log.e("tml CA cursor3: contentUri=" + contentUri.toString() + " column=" + column[0]);
					cursor = getContentResolver().query(contentUri, column, null, null, null);
				}
				
				if (cursor == null) {
			        result = uri.getPath();
			    } else { 
			        cursor.moveToFirst();
			        int idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
			        if (idx>=0)
			        {
			        	result = cursor.getString(idx);
			        	cursor.close();
			        }
			    }
		        Log.e("tml CA getPath result=" + result);
				return result;
			} else if (uri.toString().startsWith("file:")) {
				String uriStr = uri.toString();
				if (uriStr.contains("sdcard")) {
					return uriStr.substring(uriStr.indexOf("sdcard"));
				} else if (uriStr.contains("storage")) {  //tml*** chat video bug
					return uriStr.substring(uriStr.indexOf("storage"));
				}
			}
		}catch(Exception e){
			Log.e("tml CA.err getPath " + e.getMessage());
		}
		return null;
	}
	public static boolean videoRecording  = false;

	private void onFileTransfer() {
		if (mAttached != 8 && mAttached != 0) {
			Toast.makeText(this, getString(R.string.fileandvideosingle), Toast.LENGTH_SHORT).show();
			return;
		}
		isVideo = false;
		if (MyUtil.checkSDCard(getApplicationContext())) {
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
		startActivityForResult(Intent.createChooser(intent, getString(R.string.chose_file)), 101);
	}
	
	private void onVideoRecord()
	{
		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // With and without this, it yields same resolution
		startActivityForResult(intent, 231);
	}

	private Uri outputFileUri;

	private void onTakePicture() {
		if (mAttached == 8) {
			Toast.makeText(this, getString(R.string.fileandvideosingle),
					Toast.LENGTH_SHORT).show();
			return;
		}
		if (!MyUtil.canHandleCameraIntent(getApplicationContext())){
			Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
			return;
		}
		try{
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			File file = new File(Global.SdcardPath_sent + "tmp.jpg");
			outputFileUri = Uri.fromFile(file);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
			startActivityForResult(intent, 3);
			mPref.write("vociemessaging", true);// take photo not popupDialog
		}catch(Exception e){
			Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
		}
	}

	private void onPickPicture() {
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

	public static String getRandomName() {
		return ("" + new Date().getTime());
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 3) // take photo not popupDialog
			mPref.write("vociemessaging", false);
		
		if (requestCode == 404) {
			if (resultCode == RESULT_OK)
			{
				TalkList.clear();
				mDB.deleteThreadByAddress(mAddress);
				mHandler.post(RefreshStatus);
			}
		}
		else if (requestCode == 230) {
			if (resultCode == RESULT_OK)
				MakeCall.Call(ConversationActivity.this, mAddress, false);
		}
		else if (requestCode == 109) {
			if (resultCode == RESULT_OK)
			{
				int act=data.getIntExtra("action", -1);
				if (act==0)
					onPickPicture();
				else if (act==1)
					onTakePicture();
				else if (act==2)
//					onPickVideo();
					onFileTransfer();  //tml*** beta ui3
				else if (act==3)
					onVideoRecord();
			}
		}
		else if (requestCode == 231) {
			if (resultCode == RESULT_OK)
			{
				if (null==data.getData()) return;
				mAttached = 8;
				Uri selectedImageUri = data.getData();
				SrcVideoPath = getPath(selectedImageUri, requestCode).toString();
				if (SrcVideoPath!=null)
				{
					SrcAudioPath = SrcVideoPath;
					onSend();
				}
			}
		}
		else if (requestCode == 7) {
			if (resultCode == RESULT_OK) {
				mAttached = 0;
				long lon = mPref.readLong("longitude", Global.DEFAULT_LON);
				long lat = mPref.readLong("latitude", Global.DEFAULT_LAT);
				mMsgText = "here I am ("+((float)lat/1000000.f)+","+((float)lon/1000000.f)+")";
				agent=new SendAgent(ConversationActivity.this, myIdx, mIdx, true);
				
				if (inGroup)
				{
					agent.setAsGroup(mGroupID);
					if (agent.onMultipleSend(addressList, mMsgText, 0, null, null))
						addMsgtoTalklist(false);
				}else{
					if (agent.onSend(mAddress, mMsgText, 0, null, null, false))
						addMsgtoTalklist(false);
				}
			}
		}else if (resultCode == RESULT_OK) {
			if (requestCode == 1 || requestCode == 3) {
				if (requestCode == 1) {
					if (null==data.getData()) return;
					Uri selectedImageUri = data.getData();
					SrcImagePath = getPath(selectedImageUri, requestCode);
				}
				else if (requestCode == 3)
					SrcImagePath = Global.SdcardPath_sent + "tmp.jpg";

				mAttached |= 2;// image
				String filename = Global.SdcardPath_sent + getRandomName() + ".jpg";
				
				if (SrcImagePath==null)
				{
					int result = ResizeImage.saveFromStream(this, data, filename,
							1280, 1280, 95);
					if (result == -1) {
						Toast.makeText(this, R.string.img_error, Toast.LENGTH_SHORT)
							.show();
						return;
					}
				}
				else{
					int result = ResizeImage.Resize(this, SrcImagePath, filename,
							1280, 1280, 95);
					if (result == -1) {
						Toast.makeText(this, R.string.img_error, Toast.LENGTH_SHORT)
							.show();
						return;
					}
				}
				SrcImagePath = filename;

				onSend();
			}
			else if (requestCode == 200) // back from smile activity
			{
				int index = data.getIntExtra("index", 0);
				if(index>=75){ //gif
					agent=new SendAgent(ConversationActivity.this, myIdx, mIdx, true);
					mMsgText = (String)SmileyActivity.smiles[index][0];
					
					if (inGroup)
					{
						agent.setAsGroup(mGroupID);
						if (agent.onMultipleSend(addressList, mMsgText, 0, null, null))
							addMsgtoTalklist(false);
					}else{
						if (agent.onSend(mAddress, mMsgText, 0,null, null, false))
							addMsgtoTalklist(false);
					}
				}else{
					EditText msginput = (EditText) findViewById(R.id.msginput);
					int indexCursor = msginput.getSelectionStart();
					msginput.getText().insert(indexCursor,
							String.valueOf(SmileyActivity.smiles[index][0]));
				}
				smileClicked=false;
			} else if (requestCode == 15) {
				mAttached |= 1;
				SrcAudioPath = data.getStringExtra("path");
				
				voicetime = 60-data.getIntExtra("voicetime", 60);
				
				agent=new SendAgent(ConversationActivity.this, myIdx, mIdx, true);
								
				SrcImagePath=null;
				mMsgText="(Vm)"+voicetime;
				
				if (inGroup)
				{
					agent.setAsGroup(mGroupID);
					if (agent.onMultipleSend(addressList, mMsgText, mAttached, SrcAudioPath, SrcImagePath))
					{
						addMsgtoTalklist(false);
						playSoundTouch();
					}
				}else{
					if (agent.onSend(mAddress, mMsgText, mAttached, SrcAudioPath, SrcImagePath, false))
					{
						addMsgtoTalklist(false);
						playSoundTouch();
					}
				}
				SrcAudioPath = null;
				
			} else if (requestCode == 40) {
				
				fileAgent = new SendFileAgent(this, myIdx, true);
				
				if (inGroup)
				{
					fileAgent.setAsGroup(mGroupID);
					if (!fileAgent.onMultipleSend(addressList, mMsgText, mAttached, SrcAudioPath, SrcImagePath))
						mSend.setEnabled(true);
					else{
						addMsgtoTalklist(true);
						playSoundTouch();
					}
				}
				else{
					if (!fileAgent.onSend(mAddress, mMsgText, mAttached, SrcAudioPath, SrcImagePath, false))
						mSend.setEnabled(true);
					else {
						addMsgtoTalklist(true);
						playSoundTouch();
					}
				}
				SrcAudioPath = null;
			}
			if (requestCode == 101){
				if (data==null) return;
				try{
					mAttached = 8;
					Uri selectedImageUri = data.getData();
					if (selectedImageUri==null) return;
					SrcVideoPath = getPath(selectedImageUri, requestCode);
					if (SrcVideoPath!=null)
					{
						SrcAudioPath = SrcVideoPath;
						onSend();
					}
				}catch(Exception e){}
			}
			else if (requestCode == 20) { // show file attach icon
				SrcVideoPath = data.getStringExtra("filePath");
				mAttached = 8;
				SrcAudioPath = SrcVideoPath;
				onSend();
			} else if (requestCode == 30) { // show video attach icon
				videoRecording = false;
				mAttached = 8;
				Uri selectedImageUri = data.getData();
				SrcVideoPath = getPath(selectedImageUri, requestCode);
				SrcAudioPath = SrcVideoPath;
				onSend();
			} else if (requestCode == 50) { // download file
				if (!MyUtil.checkSDCard(getApplicationContext())) {
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
//						myNet.Download(
//								tmpCurFilePath,
//								Global.SdcardPath_downloads
//										+ filename.replace(" ", ""), type==1?9:10,obligate1_phpIP);//downloaded, 9 is video, 10 is file
						//tml*** browser save
						myNet.Download(
								tmpCurFilePath,
								getMIMEdest(filename.replace(" ", ""))
										+ filename.replace(" ", ""), type==1?9:10,obligate1_phpIP);//downloaded, 9 is video, 10 is file
						Log.e("conv2>> " + getMIMEdest(filename.replace(" ", "")));
						obligate1_phpIP = null;
					}
				}).start();
			}
		} else if (resultCode == RESULT_CANCELED && requestCode == 40) {
			mSend.setEnabled(true);
			if (null!=SrcVideoPath){
				(new File(SrcVideoPath)).delete();
			}
			SrcVideoPath = null;
			mAttached = 0;
		}else if (resultCode == RESULT_CANCELED && requestCode == 200) {
			smileClicked=false;
		}else if (resultCode != RESULT_OK && requestCode == 30) {// record
			if (null!=SrcAudioPath){
				try{
					(new File(SrcAudioPath)).delete();
				}catch(Exception e){}
			}
			SrcAudioPath = null;
			mAttached = 0;
			videoRecording = false;
		}else if (resultCode == RESULT_CANCELED && requestCode == 15) {// voice memo cancelled
			if (null!=SrcAudioPath){
				try{
					(new File(SrcAudioPath)).delete();
				}catch(Exception e){}
			}
			SrcAudioPath = null;
			mAttached = 0;
		}
	}

	@SuppressLint("NewApi")
	private void showVideoBitmap(ImageView imageView) {
		if (SrcVideoPath == null)
			return;
		
		if(Integer.parseInt(Build.VERSION.SDK) >= 8){
			videobitmap = ThumbnailUtils.createVideoThumbnail(new File(
					SrcVideoPath).getAbsolutePath(), Video.Thumbnails.MICRO_KIND);
		}
		if (videobitmap == null)
			videobitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sm70);
		if (Integer.parseInt(Build.VERSION.SDK) >= 8){
			Bitmap bubbleblue = BitmapFactory.decodeResource(ConversationActivity.this.getResources(),
					R.drawable.videosms_play);
			Drawable[] array = new Drawable[2];
			array[1] = new BitmapDrawable(bubbleblue);
			array[0] = new BitmapDrawable(videobitmap);
			LayerDrawable layers= new LayerDrawable(array);
			layers.setLayerInset(0, 0, 0, 0, 0);
			layers.setLayerInset(1, 0, 0, 0, 0);
			layers.setBounds(0, 0, (int)(66.f*mDensity), (int)(66.f*mDensity));
		
			imageView.setImageDrawable(layers);
		}
		else
			imageView.setImageResource(R.drawable.start_play);
		videobitmap=null;
	}
	

	final Runnable RefreshStatus = new Runnable() {
		public void run() {
			if (msgListAdapter != null)
				msgListAdapter.notifyDataSetChanged();
			listview.setSelection(TalkList.size() - 1);
			needScrollToEnd=false;
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
			
			/*
			if (msg.content.startsWith("(fl)")) {
				msg.content = getString(R.string.filememo_send) + " " + msg.content;
			} else {
				msg.content = getString(R.string.video) + " " + msg.content;
				msg.attached = 9;
			}*/
			
			if (msg.content.startsWith("(fl)")) {
				String filename=SrcAudioPath.substring(SrcAudioPath.lastIndexOf('/')+1);
				String part2=msg.content.substring(4);
				msg.content = "(fl)  " + filename + "  " + part2;
			} else {
				msg.content = getString(R.string.video) + " " + msg.content;
				msg.attached = 9;
			}
		}

		msg.longitudeE6 = mPref.readLong("longitude", Global.DEFAULT_LON);
		msg.latitudeE6 = mPref.readLong("latitude", Global.DEFAULT_LAT);
		
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
		needScrollToEnd=true;
		mHandler.post(RefreshStatus);

		mHandler.postDelayed(new Runnable() {
			public void run() {
				mSend.setEnabled(true);
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
		try{
			String msgContent = "";
			String filename = curFilePath.substring(curFilePath.lastIndexOf("/") + 1, curFilePath.lastIndexOf("_"));
			Intent it = new Intent(this, CommonDialog.class);
//			if (myNet.netType >= NetInfo.MOBILE_3G) {
//				msgContent = getString(R.string.file_dnwifinet, len,
//						Global.SdcardPath_downloads + filename);
			//tml*** browser save
			if (myNet.netType >= NetInfo.MOBILE_3G) {
				msgContent = getString(R.string.file_dnwifinet, len,
						getMIMEdest(filename) + filename);
				Log.e("conv3>> " + getMIMEdest(filename) + filename);
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
		}catch(Exception e){}
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
		
		public void clear()
		{
			GifView g=null;
			while(GifList.size()>0 && (g=GifList.get(0))!=null)
			{
				g.stop();
				GifList.remove(g);
			}
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			SMS msg = TalkList.get(TalkList.size() - position - 1);
			if (convertView==null) {
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
			}
			else{
				holder = (ViewHolder) convertView.getTag();
				//alec: finally I did implement recycling.
				//		without this part, the textview will reuse the previous height
				((RelativeLayout)convertView).removeView(holder.balloon);
				holder.balloon = new TextView(mContext);
				holder.balloon.setTextColor(0xff000000);
				holder.balloon.setMaxWidth((int)(640*mDensity));
				holder.balloon.setPadding((int)(44.*mDensity), (int)(44.*mDensity), (int)(44.*mDensity), (int)(44.*mDensity));
				holder.balloon.setTextSize(30);
				if (msg.content.startsWith("(Vm)")) {  //tml|yang*** chat width
					holder.balloon.setWidth((int) (430 * mDensity));
				}
				((RelativeLayout)convertView).addView(holder.balloon, new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				
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
						&& msg.content.lastIndexOf("KB")+3 == msg.content.length()) {
					msg.content = msg.content.substring(0,msg.content.length() - 1);
				}
			}
			if (msg.attached == 9) {// video uploaded
				msg.content= "(vdo)";
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
			lpPhoto = new RelativeLayout.LayoutParams((int)(75.*mDensity), (int)(75.*mDensity));
			
			if (msg.type == 1) {
				lpPhoto.setMargins((int)(120.*mDensity), 0, 0, 0);
				lpPhoto.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				if (inGroup)//alec
				{
					Drawable f=friendsPhotoMap.get(Integer.valueOf(msg.group_member));
					holder.photoimage.setImageDrawable(f);
					//tml*** group ui1
					RelativeLayout.LayoutParams lpName = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
					lpName.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
					lpName.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					lpName.addRule(RelativeLayout.BELOW, holder.tTime.getId());  //tml*** group ui1
					lpName.setMargins((int) (120 * mDensity), 0, 0, (int) (10 * mDensity));
					String nickname = mADB.getNicknameByIdx(msg.group_member);
					holder.username.setText(nickname);
					holder.username.setVisibility(View.VISIBLE);
					holder.username.setLayoutParams(lpName);
				}
				else
				{
					holder.photoimage.setImageDrawable(friendPhoto);
					holder.username.setVisibility(View.GONE);  //tml*** group ui1
				}
			} else {
				lpPhoto.setMargins(0, 0, (int)(120.*mDensity), 0);
				lpPhoto.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				holder.photoimage.setImageDrawable(myphoto);
				holder.username.setVisibility(View.GONE);  //tml*** group ui1
			}
//			lpPhoto.addRule(RelativeLayout.BELOW, holder.tTime.getId());
			lpPhoto.addRule(RelativeLayout.BELOW, holder.title.getId());  //tml*** group ui1
			lpPhoto.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			holder.photoimage.setLayoutParams(lpPhoto);
			//tml*** group ui1 X
//			holder.balloon.setId(2);
//			holder.gifview.setId(3);
//			holder.photoimage.setId(4);

			if (msg.content!=null && msg.content.startsWith("[<AGREESHARE>]")) {
				String[] res = msg.content.split(",");
				int relation = Integer.valueOf(res[2]);
				msg.content = mContext.getString(
						R.string.agree_share_sms,
						mContext.getResources().getStringArray(
								R.array.share_time)[relation - 1]);
			}
			
			if ((msg.type==2 || (msg.type==1 && !msg.content.contains("(fl)"))) 
					&& (msg.attached == 8 || msg.attached == 10) && (msg.att_path_aud.contains(".mp4") || msg.att_path_aud.contains(".3gp")))
			{
				msg.attached=9;
				msg.content="(vdo)";
			}

			String s = msg.content;
			if (msg.attached == 8) {				
				if (!(msg.content.startsWith(getString(R.string.video)) && msg.content.contains("(vdo)"))) {
					
					int zhao=msg.att_path_aud.lastIndexOf("_");
					int zhao2=msg.att_path_aud.lastIndexOf(".");
					if (zhao!=-1 && zhao2!=-1 && zhao2<zhao)
					{
						try{
							if (!s.startsWith(getString(R.string.filememo_recv)))
								s = s.substring(getString(R.string.filememo_send).length()+1, s.length());
							if(msg.type==1)
								s = s.replace("(fl)", "(fl) " + msg.att_path_aud.substring(msg.att_path_aud.lastIndexOf("/") + 1, msg.att_path_aud.lastIndexOf("_"))+" ");
							else
								s = s.replace("(fl)", "(fl) " + msg.att_path_aud.substring(msg.att_path_aud.lastIndexOf("/") + 1, msg.att_path_aud.length())+" ");
						}catch(Exception e){
							if(msg.type==1)
								s = getString(R.string.filememo_recv)+" (fl)";
							else
								s = getString(R.string.filememo_send)+" (fl)";
						}
					}
				}
			}
			if (msg.attached == 10 && s.startsWith(getApplicationContext().getResources().getString(R.string.file))){
				s = s.replaceFirst(getApplicationContext().getResources().getString(R.string.file), "(fl)");
			}
			
			//alec
			if (s.startsWith("here I am ("))
				s="(mAp)";
			else if (s.equals("Missed call"))
				s="(mCl) "+getString(R.string.missed_call);
			else if (s.startsWith("(Vm)") && s.length()>4)
				s+='"';
			
			RelativeLayout.LayoutParams lpProgress = new RelativeLayout.LayoutParams((int)(430.*mDensity), (int)(15.*mDensity));
			lpProgress.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			lpProgress.addRule(RelativeLayout.BELOW, holder.balloon.getId());
			lpProgress.addRule(RelativeLayout.LEFT_OF, holder.photoimage.getId());
			lpProgress.rightMargin=(int)(20*mDensity);
			holder.progress.setLayoutParams(lpProgress);
			
			Smiley sm = new Smiley();
			boolean hasGif = false;
			if (sm.hasSmileys(s) > 0) {
				if(spannableCache.get(msg.smsid)==null || msg.smsid==rowID){
					SpannableString spannable = new SpannableString(s);
					for (int i = 0; i < Smiley.MAXSIZE; i++) {
						for (int j = 0; j < sm.getCount(i); j++) {
							if (i == (Smiley.MAXSIZE - 1)) {//picture
								Bitmap picturebitmap=null;
								try{
									picturebitmap = ImageUtil.getBitmapAsRoundCornerWithAdaptiveDivision(3, 15, msg.att_path_img);
								}catch(Exception e){
								}catch(OutOfMemoryError e){}
								
								if (picturebitmap == null) {
									spannable = new SpannableString(getString(R.string.notfound_photo));
								} else {
									ImageSpan icon = new ImageSpan(ConversationActivity.this, picturebitmap, ImageSpan.ALIGN_BOTTOM);
									spannable.setSpan(icon, sm.getStart(i, j), sm.getEnd(i, j), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
								}
								picturebitmap=null;
								break;
							} else if(i<=74){
								ImageSpan icon = null;
								LayerDrawable layers = null;
								Bitmap bitmap = null;
								if (i >= 66) {
									Drawable d=null;
									if (i == 69) { // video
										if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
											try {
												bitmap = ThumbnailUtils
														.createVideoThumbnail(
																new File(msg.att_path_aud).getAbsolutePath(),
																Video.Thumbnails.MICRO_KIND);
												if (bitmap!=null)
													d = new BitmapDrawable(bitmap);
											} catch (Exception e) {}
										}
										if (d==null)
											d = getResources().getDrawable(R.drawable.start_play);
										d.setBounds(0, 0, (int)(30.f*mDensity), (int)(30.f*mDensity));
									}
									else if (i==70){
										d = getResources().getDrawable(R.drawable.sm01 + i);
										d.setBounds(0, 0, (int)(50.f*mDensity), (int)(50.f*mDensity));
									}
									else if (i==71){
										d = getResources().getDrawable(R.drawable.mapview);
										d.setBounds(0, 0, (int)(size2*mDensity), (int)(size2*mDensity));
									}
									else if (i==72){
										d = getResources().getDrawable(android.R.drawable.sym_call_missed);
										d.setBounds(0, 0, (int)(size*mDensity), (int)(size*mDensity));
									}
									else if (i==73){
										d = getResources().getDrawable(android.R.drawable.sym_call_outgoing);
										d.setBounds(0, 0, (int)(size*mDensity), (int)(size*mDensity));
									}
									else if (i==74){
										d = getResources().getDrawable(android.R.drawable.sym_call_incoming);
										d.setBounds(0, 0, (int)(size*mDensity), (int)(size*mDensity));
									}
									else if (i==66){
										d = getResources().getDrawable(R.drawable.sm67);
										d.setBounds(0, 0, (int)(150.f*mDensity), (int)(30.f*mDensity));
									}
									if(msg.attached==9){
										Bitmap bubbleblue = BitmapFactory.decodeResource(ConversationActivity.this.getResources(),
												R.drawable.videosms_play);
										Drawable[] array = new Drawable[2];
										array[1] = new BitmapDrawable(bubbleblue);
										array[0] = new BitmapDrawable(bitmap);
										layers= new LayerDrawable(array);
										layers.setLayerInset(0, 0, 0, 0, 0);
										layers.setLayerInset(1, 0, 0, 0, 0);
										layers.setBounds(0, 0, (int)(190.f*mDensity), (int)(190.f*mDensity));
									}
									if (icon == null){
										if(msg.attached==9){
											if (Integer.parseInt(Build.VERSION.SDK) >= 8)
												icon = new ImageSpan(layers,ImageSpan.ALIGN_BOTTOM);
											else
												icon = new ImageSpan(getResources().getDrawable(R.drawable.start_play),ImageSpan.ALIGN_BOTTOM);
											layers=null;
										}else
											icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
									}
								} else {
									icon = new ImageSpan(mContext, R.drawable.sm01 + i, ImageSpan.ALIGN_BOTTOM);
								}
								spannable.setSpan(icon, sm.getStart(i, j),
										sm.getEnd(i, j),
										SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
							} else if(i>=75){
								hasGif = true;
								int res=R.drawable.em001+i-75;
								Drawable dra=getResources().getDrawable(res);
								int w=dra.getIntrinsicWidth();
								int h=dra.getIntrinsicHeight();
								holder.gifview.setGifImageType(GifImageType.SYNC_DECODER);
								holder.gifview.setImageSize(w+w/2, h+h/2);
								holder.gifview.setShowDimension(w+w/2, h+h/2);
								holder.gifview.setGifImage(res);
								break;
							}
						}
					}
					if(!hasGif)
						spannableCache.put(msg.smsid, spannable);
					if(null!=msg.att_path_aud && !msg.att_path_aud.startsWith("ulfiles/"))
						rowID=0;
					holder.balloon.setText(spannable);
				}
				else
					holder.balloon.setText(spannableCache.get(msg.smsid));
			}
			else{
				holder.balloon.setText(s);
			}
			
			if (msg.type==1)
			{
				holder.balloon.setBackgroundResource(R.drawable.balloon_left);
				holder.progress.setVisibility(View.GONE);
			}else{
				if (msg.status==SMS.STATUS_PENING)
				{
					holder.balloon.setBackgroundResource(R.drawable.balloon_right_pending);
					if (msg.attached==8 || msg.attached==9)
					{
						holder.progress.setProgress((float)msg.progress/100.f);
						holder.progress.setVisibility(View.VISIBLE);
						mProgress.put(msg.att_path_aud, holder.progress);
					}
				}else
				{
					holder.balloon.setBackgroundResource(R.drawable.balloon_right);
					holder.progress.setVisibility(View.GONE);
				}
			}
			
			RelativeLayout.LayoutParams lpBubble = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			if (msg.type == 1) {
				lpBubble.setMargins((int)(215.*mDensity), 0, 0, 0);
				lpBubble.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				holder.gifview.setPadding(10, 0, 10, 0);
				holder.photoimage.setOnClickListener(mOnClickPhoto);
			} else {
				lpBubble.setMargins(0, 0, (int)(215.*mDensity), 10);
				lpBubble.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				holder.gifview.setPadding(10, 0, 10, 0);
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
					if (myVP1 != null && myVP1.isPlaying())
						return;
//					if (vmp != null)
					if (vmp != null || myVP2 != null)  //tml*** new vmsg
					{
						if (playingMsg!=null)
						{
							playingMsg.stop();
							playingMsg=null;
						}
						stopPlayingVoice();
						return;
					}
					int position = Integer.parseInt(v.getTag().toString());
					SMS msg = TalkList.get(position);
					Log.d("msgs.onClick 1=" + msg.displayname + " 2=" + msg.address +  " 3=" + msg.content
							+ " 4=" + msg.contactid + " 5=" + msg.read + " 6=" + msg.type + " 7=" + msg.status 
							+ " 8=" + msg.time + " 9=" + msg.attached + " 10=" + msg.longitudeE6 + " 11=" + msg.latitudeE6
							+ " 12=" + msg.smsid);
					//tml*** phone intent
					boolean hasPhoneLink = Linkify.addLinks((TextView) v, Linkify.PHONE_NUMBERS);
//					if (hasPhoneLink) {
//					}
					//***tml
					if(msg.content.startsWith("here I am (")){
						//tml*** reenable mapview
						try {
							Class.forName("com.google.android.maps.MapActivity");
						}catch (ClassNotFoundException e) {
							Toast.makeText(ConversationActivity.this, R.string.nonsupport_googlemap, Toast.LENGTH_LONG).show();
							return;
						}catch (NoClassDefFoundError e)
						{
							Toast.makeText(ConversationActivity.this, R.string.nonsupport_googlemap, Toast.LENGTH_LONG).show();
							return;
						}
						String iso = mPref.read("iso","us");
						try{
							String s=msg.content.substring(11);
							String [] l=s.split(",");
							l[1]=l[1].substring(0, l[1].length()-1);
							long lat=(long)(Double.parseDouble(l[0])*1E6);
							long lon=(long)(Double.parseDouble(l[1])*1E6);
							Log.i("LOC " + msg.content + " parsed:" + lat + "," + lon);
							FunctionActivity.onLaunchStaticMapView(ConversationActivity.this, lon, lat, 
									msg.longitudeE6, msg.latitudeE6,
									msg.time, msg.displayname, msg.address, msg.contactid, msg.type==2, iso);
						}catch(Exception e){}
						return;
					}
					if(msg.content.equals("Missed call") && msg.type==1){
						
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
					if (!MyUtil.checkSDCard(getApplicationContext()))
						return;
					if (msg.attached == 1) {
						onPlayVoiceMemo(msg.att_path_aud);
						try{
							View p=(View)v.getParent();
							AudioMsgPlayer a=(AudioMsgPlayer)p.findViewWithTag("audio");
							playingMsg=a;
							a.play();
						}catch(Exception e){}
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
					if(msg.att_path_aud!=null){
						File file = new File(msg.att_path_aud);
						if ((msg.attached == 8 || msg.attached == 10 || msg.attached==9) && file.exists()){
							OpenDifferentFile openDifferentFile = new OpenDifferentFile(
									ConversationActivity.this);
							openDifferentFile.openFile(msg.att_path_aud);
							return;
						}
					}
					if (msg.attached == 8 || msg.attached == 9)
					{
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
						if(obligate1_phpIP==null || obligate1_phpIP.length()==0)
			    			obligate1_phpIP = AireJupiter.myPhpServer_default;
						if (msg.content.startsWith(getString(R.string.video)) && msg.content.contains("(vdo)")){
							String len = msg.content.substring(msg.content.indexOf("(vdo)") + 5);
							type = 1;
							try {
								dialogFileDownload(msg.att_path_aud, len.substring(0, len.indexOf("KB")+3));
						    } catch (Exception e) {
						    	try {
						    		dialogFileDownload(msg.att_path_aud, len);
						    	} catch (Exception e2) {}
						    }
						    rowID=msg.smsid;
						}else if (msg.content.startsWith(getString(R.string.filememo_recv)) && msg.content.contains("(fl)")){
							String len = msg.content.substring(msg.content.indexOf("(fl)") + 4);
							type = 2;
							try {
								dialogFileDownload(msg.att_path_aud, len.substring(0, len.indexOf("KB")+3));
						    } catch (Exception e) {
						    	dialogFileDownload(msg.att_path_aud, len);
						    }
						    rowID=msg.smsid;
						}
						return;
					}
					
					enterTime = new Date().getTime();
					
					if (msg.obligate1!=null && msg.obligate1.startsWith("http"))
					{
						/*
						try{
							String title=msg.content.substring(6);
							Intent i = new Intent(ConversationActivity.this,WebViewActivity.class);
							i.putExtra("URL", msg.obligate1);
							i.putExtra("Title", title);
							startActivity(i);
						}catch(Exception e){}*/
					}
					else
					{
						Intent i = new Intent(ConversationActivity.this,MessageDetailActivity.class);
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
			if (hasGif)
			{
				holder.gifview.setVisibility(View.VISIBLE);
				holder.balloon.setVisibility(View.GONE);
			}else{
				holder.balloon.setVisibility(View.VISIBLE);
				holder.gifview.setVisibility(View.GONE);
			}
			
			holder.gifview.setOnLongClickListener(mLongPressBalloonView);
			
			if (msg.content.startsWith("(Vm)"))
			{
				RelativeLayout.LayoutParams lpAudioMsgPlayer = new RelativeLayout.LayoutParams((int)(430.*mDensity), (int)(75.*mDensity));
				lpAudioMsgPlayer.addRule(RelativeLayout.ALIGN_TOP, holder.balloon.getId());
//				lpAudioMsgPlayer.width = (int)(430*mDensity);  //tml*** chat width
				if (msg.type==2)
				{
					lpAudioMsgPlayer.addRule(RelativeLayout.LEFT_OF, holder.photoimage.getId());
					lpAudioMsgPlayer.rightMargin=(int)(20*mDensity);
					holder.audmsg.setBackgroundResource(R.drawable.balloon_right);
				}else{
					lpAudioMsgPlayer.addRule(RelativeLayout.RIGHT_OF, holder.photoimage.getId());
					lpAudioMsgPlayer.leftMargin=(int)(20*mDensity);
					holder.audmsg.setBackgroundResource(R.drawable.balloon_left);
				}
				holder.audmsg.setLayoutParams(lpAudioMsgPlayer);
				holder.audmsg.setTag("audio");
				
				try{
					int sec=Integer.parseInt(msg.content.substring(4));
					holder.audmsg.setDuration(sec);
				}
				catch(Exception e){
					holder.audmsg.setDuration(5);
				}
				holder.audmsg.setVisibility(View.VISIBLE);
				holder.audmsg.bringToFront();
			}
			else
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
		TextView username;  //tml*** group ui1
		RelativeLayout title;
	}
	
	BroadcastReceiver HandleListChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			
			if (intent==null) return;
			
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
					}
					onPlayVoiceMemo(intent.getStringExtra("autoPath"));
				}
				if (mPref.readBoolean("recvVibrator", true)) {
					long[] patern = { 0, 20, 1000 };
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
				}
				return;
			} else if (intent.getAction().equals(Global.ACTION_PLAY_OVER)) {
				Log.d("ACTION_PLAY_OVER received");
				if (spAnimation!=null)
				{
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
				}
				return;
			} else if (intent.getAction().equals(Global.Action_SMS_Fail)) {
				Toast.makeText(ConversationActivity.this,
						getString(R.string.smsfail), Toast.LENGTH_SHORT).show();
				return;
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
			speaker.setVisibility(View.VISIBLE);
			if (spAnimation!=null) spAnimation.start();
			AnimationDrawablestate = false;
		} catch (IllegalArgumentException e) {
			Log.e("cva1 " + e.getMessage());
			vmp = null;
			myVP2 = null;
			return;
		} catch (IllegalStateException e) {
			Log.e("cva2 " + e.getMessage());
			vmp = null;
			myVP2 = null;
			return;
		} catch (Exception e) {
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
		if (smileClicked=true) {
			smileClicked=false;
		}
		if (AnimationDrawablestate) {
			super.onBackPressed();
		} else {
			stopPlayingVoice();
		}
	}

	private void playSoundTouch() {
		if (mPref.readBoolean("sendVibrator", true)) {
			long[] patern = { 0, 40, 1000 };
			mVibrator.vibrate(patern, -1);
		}
	}

	public void stopPlayingVoice() {
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
			if (playingMsg!=null)
			{
				playingMsg.stop();
				playingMsg=null;
			}
		} catch (Exception e) {
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.view_profile:
			{
				Intent it=new Intent(ConversationActivity.this, FunctionActivity.class);
				it.putExtra("Contact_id", mContactId);
				it.putExtra("Address", mAddress);
				it.putExtra("Nickname", mNickname);
				if (mContactId>0)
					it.putExtra("AireNickname", mADB.getNicknameByAddress(mAddress));
				it.putExtra("Idx", mIdx);
				it.putExtra("fromConversation", true);
				startActivity(it);
			}
			break;
		
		case R.id.voicesms:
			if(!MyUtil.checkSDCard(getApplicationContext())){
				Toast.makeText(this, getString(R.string.no_sdcard),
						Toast.LENGTH_SHORT).show();
				return;
			}
			if (mAttached == 8) {
				Toast.makeText(ConversationActivity.this,
						getString(R.string.fileandvideosingle),
						Toast.LENGTH_SHORT).show();
				return;
			}
			SrcAudioPath = Global.SdcardPath_sent + getRandomName() + ".amr";
			Intent it = new Intent(ConversationActivity.this,
					VoiceRecordingDialog.class);
			it.putExtra("path", SrcAudioPath);
			startActivityForResult(it, 15);
			break;
		case R.id.media:
			{
				Intent it3 = new Intent(ConversationActivity.this, MediaDialog.class);
				startActivityForResult(it3, 109);
			}
			break;
		case R.id.filesms:
			onFileTransfer();
			break;
		case R.id.location:
			it = new Intent(ConversationActivity.this,
					CommonDialog.class);
			it.putExtra("msgContent", String.format(getString(R.string.send_location),mNickname));
			it.putExtra("numItems", 2);
			it.putExtra("ItemCaption0", getString(R.string.cancel));
			it.putExtra("ItemResult0", RESULT_CANCELED);
			it.putExtra("ItemCaption1", getString(R.string.yes));
			it.putExtra("ItemResult1", RESULT_OK);
			it.putExtra("Address", mAddress);
			it.putExtra("Nickname", mNickname);
			it.putExtra("Idx", mIdx);
			startActivityForResult(it, 7);
			break;
		case R.id.moresms:
			listnumber += 20;
			int len1 = TalkList.size();
			ArrangeTalkList();
			msgListAdapter.notifyDataSetChanged();
			int len2 = TalkList.size();
			listview.setSelectionFromTop(len2 - len1, moresms.getHeight()+10);
			break;
		case R.id.call:  //tml*** beta ui3
			if (inGroup) {
				try {
					broadcastConf = false;  //tml*** broadcast
					if (sendeeList.size() > 0 && sendeeList.size() <= 9)
					{
						AireVenus.setCallType(AireVenus.CALLTYPE_CHATROOM);
						mPref.write("incomingChatroom", false);
						
						int myIdx = 0;
						try {
							myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
							mPref.write("ChatroomHostIdx", myIdx);
						} catch (Exception e) {}
						
						String idx = "" + myIdx;
						MakeCall.ConferenceCall(getApplicationContext(), idx,-1,false);
						new Thread(sendNotifyForJoinChatroom).start();
					}
					
				} catch (Exception e) {}
			} else {
				//tml*** support
				if ((mAddress.equals("support") && mNickname.equals("Support")) || mIdx == 2) {
					String url = "http://airetalk.com/airecenter/faq.php";
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(url));
					startActivity(i);
				} else {
					MakeCall.Call(ConversationActivity.this, mAddress, false);
				}
			}
			break;
		case R.id.videocall:  //tml*** beta ui3
			if (inGroup) {
				try {
					broadcastConf = true;  //tml*** broadcast
					if (sendeeList.size() > 0 && sendeeList.size() <= 9)
					{
						AireVenus.setCallType(AireVenus.CALLTYPE_CHATROOM);
						mPref.write("incomingChatroom", false);
						
						int myIdx = 0;
						try {
							myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
							mPref.write("ChatroomHostIdx", myIdx);
						} catch (Exception e) {}
						
						String idx = "" + myIdx;
						
						MakeCall.ConferenceCall(getApplicationContext(), idx,-1,false);
						new Thread(sendNotifyForJoinChatroom).start();
					}
					
				} catch (Exception e) {}
			} else {
				MakeCall.Call(ConversationActivity.this, mAddress, true, false);
			}
			break;
		case R.id.attachments:  //tml*** beta ui3
			Intent it3 = new Intent(ConversationActivity.this, MediaDialog.class);
			startActivityForResult(it3, 109);
			break;
		case R.id.smile:
			if (!smileClicked) {
				startActivityForResult(new Intent(ConversationActivity.this,SmileyActivity.class), 200);
				smileClicked=true;
			}
			break;
		case R.id.sendmsg:
			onSend();
			break;
		case R.id.cancel:
//			if (mFromCallMode) {  //tml*** chatview
//				Intent itm = new Intent(ConversationActivity.this, MainActivity.class);
//				startActivity(itm);
//				finish();
//			} else {
				finish();
//			}
			break;
		case R.id.removeall:
			Intent it2 = new Intent(ConversationActivity.this, CommonDialog.class);
			it2.putExtra("msgContent", getString(R.string.delete_thread_confirm));
			it2.putExtra("numItems", 2);
			it2.putExtra("ItemCaption0", getString(R.string.cancel));
			it2.putExtra("ItemResult0", RESULT_CANCELED);
			it2.putExtra("ItemCaption1", getString(R.string.yes));
			it2.putExtra("ItemResult1", RESULT_OK);
			startActivityForResult(it2, 404);
			break;
		case R.id.voice:
			if (mp2 != null && mp2.isPlaying())
				return;
			if (myVP1 != null && myVP1.isPlaying())
				return;
			if (vmp != null)
				return;
			if (myVP2 != null)
				return;
			if (SrcAudioPath != null &&  (new File(SrcAudioPath).length()>0)) {
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
					Log.e("cva3 " + e.getMessage());
				} catch (IllegalArgumentException e) {
				} catch (IllegalStateException e) {
				}
			}
			break;
		}
	}
	
	private void onSend()
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
			/*if (ConversationActivity.fileUploading) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.fileuploading),
						Toast.LENGTH_SHORT).show();
				mSend.setEnabled(true);
				return;
			}*/
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
					return;
				}
			} catch (Exception e) {}
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
    	if (UserPage.sortMethod==1)
    		UserPage.needRefresh=true;

		if (mAttached == 8) {
			if(videobitmap!=null)
				videobitmap.recycle();
			
			fileAgent = new SendFileAgent(this, myIdx, true);
			
			if (inGroup)
			{
				fileAgent.setAsGroup(mGroupID);
				if (!fileAgent.onMultipleSend(addressList, mMsgText, mAttached, SrcAudioPath, SrcImagePath))
				{
					mSend.setEnabled(true);
				}
				else{
					addMsgtoTalklist(true);
					playSoundTouch();
				}
			}
			else{
				if (!fileAgent.onSend(mAddress, mMsgText, mAttached, SrcAudioPath, SrcImagePath, false)) {
					mSend.setEnabled(true);
				} else {
					addMsgtoTalklist(true);
					playSoundTouch();
				}
			}
			SrcAudioPath = null;
		} else {
			agent=new SendAgent(ConversationActivity.this, myIdx, mIdx, true);
			
			if (inGroup)
			{
				agent.setAsGroup(mGroupID);
				if (!agent.onMultipleSend(addressList, mMsgText, mAttached, SrcAudioPath, SrcImagePath))
					mSend.setEnabled(true);
				else {
					addMsgtoTalklist(false);
					playSoundTouch();
				}
			}
			else{
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

	//tml*** browser save
	private String getMIMEdest(String fName) {
		String type = "";
		String end = fName
				.substring(fName.indexOf(".") + 1, fName.length())
				.toLowerCase();

		if (end.equals("mp3") || end.equals("mid") || end.equals("wav")
				|| end.equals("amr") || end.equals("wma")) {
			type = Global.SdcardPath_music;// audio
		} else if (end.equals("3gp") || end.equals("mp4") || end.equals("avi")
				|| end.equals("rmvb") || end.equals("m4v")|| end.equals("ogg")) {
			type = Global.SdcardPath_video;// video/end
		} else if (end.equals("jpg") || end.equals("gif") || end.equals("png")
				|| end.equals("jpeg") || end.equals("bmp")) {
			type = Global.SdcardPath_image;// image/end
		} else {
			type = Global.SdcardPath_files;// "*/*";
		}

		Log.e("detect MIME dir>> " + type);
		return type;
	}
	
	private String convertFN(String fName) {
		String newfName = fName;
		if (fName.contains("(Vm)")) {
			newfName = fName.replace("(Vm)", "");
		}
		if (fName.contains("(iMG)")) {
			newfName = fName.replace("(iMG)", "");
		}
		if (fName.contains("(vdo)")) {
			newfName = fName.replace("(vdo)", "");
		}
		if (fName.contains("(fl)")) {
			newfName = fName.replace("(fl)", "");
		}
		
		newfName.trim();
		newfName = newfName.substring(0, newfName.indexOf(".") + 5).trim();
		Log.e("CONVERTFN>> {" + newfName + "}");
		
		return newfName;
	}
	
	private boolean checkFileExist(String file) {
		File fileSrc = new File(file);
		if (fileSrc.exists()) {
			return true;
		} else {
			Log.e("FDNE@@ " + fileSrc.getPath());
			Toast tst = Toast.makeText(getApplicationContext(),
					getString(R.string.filenotfound),
					Toast.LENGTH_LONG);
			LinearLayout tstLayout = (LinearLayout) tst.getView();
		    TextView tstTV = (TextView) tstLayout.getChildAt(0);
		    tstTV.setTextSize(30);
		    tst.show();
			return false;
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
	            	linkedPhone = linkedPhone.replace("tel:","");
	            } else if (linkedPhone.startsWith("voicemail:")) {
	            	linkedPhone = linkedPhone.replace("voicemail:","");
	            } else {
	    			linkHandled = false;
	    			super.startActivity(intent);
	    			return;
	            }
	            
				Log.d("link phone! " + iAction.toString() + " " + linkedPhone);
				
				//tml|sw*** no airecall in china
				boolean isCN = MyUtil.isISO_China(ConversationActivity.this, mPref, null);
				if (AireCallPage.OverrideShowAireCall) isCN = false;
				if (isCN) {
					final CharSequence[] listItems = {
							getResources().getString(R.string.contacts),
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
							            uriPhone = MyTelephony.cleanPhoneNumber3(uriPhone);
							            UserPage.passKeyword = uriPhone;
							            MainActivity._this.switchInflater(1);
							            finish();
									}
									dialog.dismiss();
								}
							});
							
							adBuilder.setTitle(linkedPhone);
							adBuilder.setNegativeButton(R.string.cancel,
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int item) {}
									});
							AlertDialog aDialogPh = adBuilder.create();
							aDialogPh.show();
						}
					});
				} else {
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
							            UserPage.passKeyword = uriPhone;
							            MainActivity._this.switchInflater(1);
							            finish();
									}
									dialog.dismiss();
								}
							});
							
							adBuilder.setTitle(linkedPhone);
							adBuilder.setNegativeButton(R.string.cancel,
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int item) {}
									});
							AlertDialog aDialogPh = adBuilder.create();
							aDialogPh.show();
						}
					});
				}
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
	//tml*** beta ui3
	private boolean broadcastConf = false;
	Runnable sendNotifyForJoinChatroom=new Runnable(){
		public void run()
		{
			String myIdxHex=mPref.read("myID","0");

			String ServerIP = mPref.read("conferenceSipServer", AireJupiter.myConfSipServer_default);
			if (AireJupiter.getInstance() != null) {
				ServerIP = AireJupiter.getInstance().getIsoConf(ServerIP);  //tml*** china ip
			}
			long ip=MyUtil.ipToLong(ServerIP);
			String HexIP=Long.toHexString(ip);
			
			String content=Global.Call_Conference + "\n\n"+HexIP+"\n\n"+myIdxHex;
			//tml*** broadcast
			if (broadcastConf) {
				mPref.write(Key.BCAST_CONF, 1);
				content = Global.Call_Conference + Global.Call_Broadcast + "\n\n"+HexIP+"\n\n"+myIdxHex;
			} else {
				mPref.write(Key.BCAST_CONF, -1);
			}

			if (AireJupiter.getInstance() != null)
				AireJupiter.getInstance().updateCallDebugStatus(true, null);
			for(int i=0; i<sendeeList.size(); i++)
			{
				int idx=Integer.parseInt(sendeeList.get(i));
				if (idx<50) continue;
				
				String address=mADB.getAddressByIdx(idx);
				
				if (AireJupiter.getInstance()!=null && AireJupiter.getInstance().tcpSocket()!=null)
				{
					if (AireJupiter.getInstance().isLogged())
					{
						if (i>0) MyUtil.Sleep(500);
						if (AireJupiter.getInstance() != null)
							AireJupiter.getInstance().updateCallDebugStatus(false, "\n>Conf " + address);
						Log.d("voip.inviteConf1 " + address + " " + content);
						AireJupiter.getInstance().tcpSocket().send(address, content, 0, null, null, 0, null);
					}
				}
			}
		}
	};
}
