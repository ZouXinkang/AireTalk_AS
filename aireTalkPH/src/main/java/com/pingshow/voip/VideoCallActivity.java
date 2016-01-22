package com.pingshow.voip;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.media.ThumbnailUtils;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.MediaStore.Video;
import android.text.ClipboardManager;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ImageSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.AmazonKindle;
import com.pingshow.amper.ConversationActivity;
import com.pingshow.amper.Global;
import com.pingshow.amper.Log;
import com.pingshow.amper.MessageActivity;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.R;
import com.pingshow.amper.SMS;
import com.pingshow.amper.SendAgent;
import com.pingshow.amper.Smiley;
import com.pingshow.amper.UsersActivity;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.SmsDB;
import com.pingshow.amper.view.AudioMsgPlayer;
import com.pingshow.amper.view.ProgressBar;
import com.pingshow.gif.GifView;
import com.pingshow.gif.GifView.GifImageType;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.video.AndroidVideoWindowImpl;
import com.pingshow.video.capture.hwconf.AndroidCameraConfiguration;
import com.pingshow.voip.core.VideoSize;
import com.pingshow.voip.core.VoipCall;
import com.pingshow.voip.core.VoipCore;

//public class VideoCallActivity extends Activity {
public class VideoCallActivity extends Activity implements
		SeekBar.OnSeekBarChangeListener,OnGestureListener,OnDoubleTapListener {
	private SurfaceView mVideoViewReady;
	private SurfaceView mVideoCaptureViewReady;
	public static boolean launched = false;
	private VoipCall videoCall;
	private WakeLock mWakeLock;
	float mDensity = 1.f;
	
	AndroidVideoWindowImpl androidVideoWindowImpl;
	private MyPreference mPref;
	
	private int cVpos = 1;  //tml*** self corners
	private boolean goRecord = false;  //tml*** monitor record
	//yang*** zoom
	WindowManager myARWinmngr;
	DisplayMetrics metrics;
	private SeekBar seekbar;
    private GestureDetector detector;
    
    private String mAddress;
    private String mNickname;
    private long mContactId;
    private boolean restartVideo;
    private boolean chatViewON = false;

	private AmpUserDB mADB;
	private SmsDB mDB;
	static private ArrayList<SMS> TalkList = new ArrayList<SMS>();
	private static MsgListAdapter msgListAdapter;
	private ListView listview;
	private int mIdx, myIdx;
	private Drawable myphoto;
	private Drawable friendPhoto;
	private int listnumber = 30;
	private long msg_smsid;
	private long msg_org_smsid;
	private Map<Long, SpannableString> spannableCache = new HashMap<Long, SpannableString>();
	private ArrayList<GifView> GifList = new ArrayList<GifView>();
	private Map<Integer, Drawable> friendsPhotoMap = new HashMap<Integer, Drawable>();
	private long rowid, rowID = 0;
	private float size = 24.f, size2 = 67.f;
	private Map<String, ProgressBar> mProgress = new HashMap<String, ProgressBar>();
	private String mMsgText;
	private String SrcAudioPath = null;
	private String SrcImagePath = null;
	private String SrcVideoPath = null;
	private int mAttached = 0;
	private SendAgent agent;
	private boolean needScrollToEnd = true;
	
	public ProgressBar getProgressBar(String fn) {
		ProgressBar p = mProgress.get(fn);
		return p;
	}
	
	//private Timer mRefreshTimer = new Timer("GL RefreshTimer");

	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e("*** !!! VIDEOCALL *** START START !!! *** voip");
		if (AireVenus.instance()==null) {
			Log.e("No service running: avoid crash by finishing "+getClass().getName());
			// super.onCreate called earlier
			finish();
			return;
		}
		setContentView(R.layout.videocall);
		
		mPref = new MyPreference(this);
		mPref.write("LastPage", 0);
		mADB = new AmpUserDB(this);
		mADB.open();
		
		mAddress = getIntent().getStringExtra("address");

		String displayName = mADB.getNicknameByAddress(mAddress);
		if (displayName != null) {
			((TextView) findViewById(R.id.displayname)).setText(displayName);
		} else {
			((TextView) findViewById(R.id.displayname)).setText(getResources().getString(R.string.unknown_person));
		}
		((TextView) findViewById(R.id.displayname)).setAlpha((float) 0.5);
		
		mDensity = getResources().getDisplayMetrics().density;

		//yang*** zoom
		myARWinmngr = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
		metrics = new DisplayMetrics();
		detector = new GestureDetector(this);
//		EditText et = (EditText) findViewById(R.id.edt);
//		et.setText("offset x =" + zx + ",y=" + zy);
		seekbar = (SeekBar) findViewById(R.id.seekBar);
		seekbar.setOnSeekBarChangeListener(this);

		SurfaceView videoView = (SurfaceView) findViewById(R.id.video_surface);

		SurfaceView captureView = (SurfaceView) findViewById(R.id.video_capture_surface);
		captureView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		/* force surfaces Z ordering */
		fixZOrder(videoView, captureView);
	
		androidVideoWindowImpl = new AndroidVideoWindowImpl(videoView, captureView);
		androidVideoWindowImpl.setListener(new AndroidVideoWindowImpl.VideoWindowListener() {
			
			public void onVideoRenderingSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface) {
				Log.d("onVideoRenderingSurfaceReady");
				AireVenus.getLc().setVideoWindow(vw);
				mVideoViewReady = surface;
				/*
				try{
					mRefreshTimer.scheduleAtFixedRate(refreshOpenGLDisplay, 320, 100);
				}catch(Exception e){}
				*/
			}
			
			public void onVideoRenderingSurfaceDestroyed(AndroidVideoWindowImpl vw) {
				Log.d("VIDEO WINDOW destroyed!");
				mVideoViewReady = null; //alec
				if (AireVenus.getLc()!=null)
					AireVenus.getLc().setVideoWindow(null);
			}
			
			public void onVideoPreviewSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface) {
				mVideoCaptureViewReady = surface;
				Log.d("onVideoPreviewSurfaceReady");
				AireVenus.getLc().setPreviewWindow(mVideoCaptureViewReady);
			}
			
			public void onVideoPreviewSurfaceDestroyed(AndroidVideoWindowImpl vw) {
				// Remove references kept in jni code and restart camera
				// ServiceY.getLc().setPreviewWindow(null);
				// Commented to remove flicker.
			}
		});
		
		androidVideoWindowImpl.init();
		
		videoCall = AireVenus.getLc().getCurrentCall();
		if (videoCall != null) {
			updatePreview(true);
		}
			
		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE,"VideoCall");
		mWakeLock.acquire();
		
		ToggleButton mMute = (ToggleButton) findViewById(R.id.mute);
		mMute.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (AireVenus.isready())
				{
					VoipCore p = AireVenus.instance().getVoipCore();
					p.muteMic(isChecked);
				}
			}
		});
		
		ImageButton mHangup = (ImageButton) findViewById(R.id.hangup);
		mHangup.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.e("voip.HANGUP1 VC *** USER PRESSED ***");
				VoipCore p=AireVenus.getLc();
				if (p!=null)
				{
					VoipCall c=p.getCurrentCall();
					if (c!=null) p.terminateCall(c);
					vibrate();
					mAlwaysChangingPhoneAngle = -1;
					Log.e("voip.HANGUP1 VC *** USER PRESSED *** OK");
					//setResult(RESULT_OK);//alec: let AireVenus close the activity naturally
					//finish();
					if (AireVenus.instance() == null) {
						finish();
					}
				} else {
					Log.e("voip.HANGUP1 VC *** USER PRESSED *** getLc=null");
				}
			}
		});
		
		ImageButton mSwitch = (ImageButton) findViewById(R.id.switchcamera);
		mSwitch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (AndroidCameraConfiguration.retrieveCameras().length<2) return;
				int id = AireVenus.getLc().getVideoDevice();
				id = (id + 1) % AndroidCameraConfiguration.retrieveCameras().length;
				AireVenus.getLc().setVideoDevice(id);
				int rotation = (360 - mAlwaysChangingPhoneAngle) % 360;
				AireVenus.getLc().setDeviceRotation(rotation);
				CallManager.getInstance().updateCall();
				// previous call will cause graph reconstruction -> regive preview window
				if (mVideoCaptureViewReady != null)
					AireVenus.getLc().setPreviewWindow(mVideoCaptureViewReady);
			}
		});
		
		//tml*** chatview2
		mNickname = getIntent().getStringExtra("nickname");
		mContactId = getIntent().getLongExtra("contactid", -20);
		restartVideo = getIntent().getBooleanExtra("RestartVideo", false);
		
		mDB = new SmsDB(this);
		mDB.open();
		
		ArrangeTalkList();

		mIdx = mADB.getIdxByAddress(mAddress);
		myIdx = Integer.parseInt(mPref.read("myID","0"), 16);
		
		msgListAdapter = new MsgListAdapter(this);
		
		listview = (ListView) findViewById(R.id.talklist);
		listview.setAdapter(msgListAdapter);
		if (TalkList.size() > 0) {
			listview.setSelection(TalkList.size() - 1);
		}

		getphoto();
		
		listview.setOnItemLongClickListener(mLongPressTalkListItem);
		
		chatViewON = false;
		((ImageButton) findViewById(R.id.chatview)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				chatViewON = true;
				((LinearLayout) findViewById(R.id.chatframe)).setVisibility(View.VISIBLE);
				((ToggleButton) findViewById(R.id.mute)).setEnabled(false);
				((ImageButton) findViewById(R.id.hangup)).setEnabled(false);
				((ImageButton) findViewById(R.id.switchcamera)).setEnabled(false);
				((ImageButton) findViewById(R.id.chatview)).setEnabled(false);
				((SeekBar) findViewById(R.id.seekBar)).setVisibility(View.INVISIBLE);
				((SeekBar) findViewById(R.id.seekBar)).setEnabled(false);

				if (needScrollToEnd)
					mHandler.post(RefreshStatus);
				
				int unread = mDB.getUnreadCountByAddress(mAddress);
				if (unread > 0) {
					MessageActivity.needToBeRefresh = true;
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							mDB.setMessageReadByAddress(VideoCallActivity.this, mAddress);
						}
					}, 200);
				}
				
				if (mPref.read("draft" + mContactId, null) != null) {
					String draft1 = mPref.read("draft" + mContactId);
					((EditText) findViewById(R.id.msginput)).setText(draft1);
					((EditText) findViewById(R.id.msginput)).setSelection(draft1.length());
				}

				Display getOrient = getWindowManager().getDefaultDisplay();
		        int orientcVpos = getOrient.getRotation();
				int w = (pW < pH) ? pW : pH;
				int h = (pW < pH) ? pH : pW;
				if (orientcVpos == Surface.ROTATION_0) {
					cVpos = 2;
					RelativeLayout.LayoutParams params;
					params = new RelativeLayout.LayoutParams(
							(int)(100f*mDensity), (int)(100f*h/w*mDensity));
					params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
					params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
					params.setMargins((int)(15f*mDensity), (int)(15f*mDensity), 0, 0);
					((SurfaceView) findViewById(R.id.video_capture_surface)).setLayoutParams(params);
				} else {
					cVpos = 1;
					RelativeLayout.LayoutParams params;
					params = new RelativeLayout.LayoutParams(
							(int)(100f*h/w*mDensity), (int)(100f*mDensity));
					params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
					params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					params.setMargins((int)(15f*mDensity), 0, 0, (int)(15f*mDensity));
					((SurfaceView) findViewById(R.id.video_capture_surface)).setLayoutParams(params);
				}
			}
		});

		((ImageView) findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String draft = ((EditText) findViewById(R.id.msginput)).getText().toString().trim();
				if (draft.length() != 0 && !draft.equals(R.string.textinput)) {
					mPref.write("draft" + mContactId, draft);
				} else if (draft.length() == 0) {
					mPref.delect("draft" + mContactId);
				}
				
				int unread = mDB.getUnreadCountByAddress(mAddress);
				if (unread > 0) {
					MessageActivity.needToBeRefresh = true;
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							mDB.setMessageReadByAddress(VideoCallActivity.this, mAddress);
						}
					}, 200);
				}
				
				chatViewON = false;
				((LinearLayout) findViewById(R.id.chatframe)).setVisibility(View.GONE);
				((ToggleButton) findViewById(R.id.mute)).setEnabled(true);
				((ImageButton) findViewById(R.id.hangup)).setEnabled(true);
				((ImageButton) findViewById(R.id.switchcamera)).setEnabled(true);
				((ImageButton) findViewById(R.id.chatview)).setEnabled(true);
				boolean envzoom = mPref.readBoolean("enVideoZoom", true);
				if (envzoom) {
					((SeekBar) findViewById(R.id.seekBar)).setVisibility(View.VISIBLE);
					((SeekBar) findViewById(R.id.seekBar)).setEnabled(true);
				}
				hideKeyboard();
			}
		});

		((Button) findViewById(R.id.sendmsg)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onSend();
			}
		});
		//***tml
		
		//tml*** self corners
		captureView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Display getOrient = getWindowManager().getDefaultDisplay();
		        int orientcVpos = getOrient.getRotation();
				int w = (pW < pH) ? pW : pH;
				int h = (pW < pH) ? pH : pW;
				try {
					if (orientcVpos == Surface.ROTATION_0) {
						if (chatViewON && cVpos == 3) {
							cVpos = 1;
						}
					} else {
						if (chatViewON && cVpos == 2) {
							cVpos = 0;
						}
					}
					if (cVpos == 0) {
						cVpos = 1;
						RelativeLayout.LayoutParams params;
						if (orientcVpos == Surface.ROTATION_0) {
							params = new RelativeLayout.LayoutParams(
									(int)(100f*mDensity), (int)(100f*h/w*mDensity));
							params.setMargins((int)(15f*mDensity), 0, 0, (int)(90f*mDensity));
						} else {
							params = new RelativeLayout.LayoutParams(
									(int)(100f*h/w*mDensity), (int)(100f*mDensity));
							params.setMargins((int)(15f*mDensity), 0, 0, (int)(15f*mDensity));
						}
						params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
						params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
						((SurfaceView) findViewById(R.id.video_capture_surface)).setLayoutParams(params);
					} else if (cVpos == 1) {
						cVpos = 2;
						RelativeLayout.LayoutParams params;
						if (orientcVpos == Surface.ROTATION_0) {
							params = new RelativeLayout.LayoutParams(
									(int)(100f*mDensity), (int)(100f*h/w*mDensity));
							params.setMargins((int)(15f*mDensity), (int)(15f*mDensity), 0, 0);
						} else {
							params = new RelativeLayout.LayoutParams(
									(int)(100f*h/w*mDensity), (int)(100f*mDensity));
							params.setMargins((int)(15f*mDensity), (int)(15f*mDensity), 0, 0);
						}
						params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
						params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
						((SurfaceView) findViewById(R.id.video_capture_surface)).setLayoutParams(params);
					} else if (cVpos == 2) {
						cVpos = 3;
						RelativeLayout.LayoutParams params;
						if (orientcVpos == Surface.ROTATION_0) {
							params = new RelativeLayout.LayoutParams(
									(int)(100f*mDensity), (int)(100f*h/w*mDensity));
							params.setMargins(0, (int)(15f*mDensity), (int)(15f*mDensity), 0);
						} else {
							params = new RelativeLayout.LayoutParams(
									(int)(100f*h/w*mDensity), (int)(100f*mDensity));
							params.setMargins(0, (int)(15f*mDensity), (int)(90f*mDensity), 0);
						}
						params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
						params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
						((SurfaceView) findViewById(R.id.video_capture_surface)).setLayoutParams(params);
					} else { //if (cVpos == 3) {
						cVpos = 0;
						RelativeLayout.LayoutParams params;
						if (orientcVpos == Surface.ROTATION_0) {
							params = new RelativeLayout.LayoutParams(
									(int)(100f*mDensity), (int)(100f*h/w*mDensity));
							params.setMargins(0, 0, (int)(15f*mDensity), (int)(90f*mDensity));
						} else {
							params = new RelativeLayout.LayoutParams(
									(int)(100f*h/w*mDensity), (int)(100f*mDensity));
							params.setMargins(0, 0, (int)(90f*mDensity), (int)(15f*mDensity));
						}
						params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
						params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
						((SurfaceView) findViewById(R.id.video_capture_surface)).setLayoutParams(params);
					}
				} catch (Exception e) {
					Log.e("self OHUH: @" + orientcVpos + "." + cVpos + "_" + e.getMessage());
					orientcVpos = Surface.ROTATION_0;
					cVpos = 0;
				}
			}
		});
		//***tml
//		captureView.setOnLongClickListener(new OnLongClickListener() {
//			@Override
//			public boolean onLongClick(View v) {
//				return false;
//			}
//		});
		
		mHandler.postDelayed(checkVideoEmpty, 15000);
		//tml*** monitor record
//		ImageButton mMonitor = (ImageButton) findViewById(R.id.monitorrecord);
//		mMonitor.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				VoipCore zVoipCore = AireVenus.instance().getVoipCore();
//				String mAddress = mPref.read("MonitorWho", "");
//				if (goRecord) {
//					if (AireJupiter.getInstance() != null
//							&& AireJupiter.getInstance().tcpSocket.isLogged()) {
//						AireJupiter.getInstance().tcpSocket.send(mAddress,
//								"GUARD record stop", 0, null, null, 0, null);
//					}
//					goRecord = false;
//				} else if (!goRecord) {
//					if (AireJupiter.getInstance() != null
//							&& AireJupiter.getInstance().tcpSocket.isLogged()) {
//						AireJupiter.getInstance().tcpSocket.send(mAddress,
//								"GUARD record start", 0, null, null, 0, null);
//					}
//					goRecord = true;
//				}
//			}
//		});
//		mSwitch.setVisibility(View.GONE);
//		mMonitor.setVisibility(View.VISIBLE);
//		
//		int maxSuvei = Global.MAX_SUVS;
//		boolean found = false;
//		String mAddress = mPref.read("MonitorWho", "");
//		for (int i = 0; i < maxSuvei; i++) {
//			String address = mPref.read("Suvei" + i);
//			if (address != null && !address.equals("")) {
//				Log.i("tml GUARD> " + address + "=" + mAddress);
//				if (address.equals(mAddress)) {
//					found = true;
//					captureView = (SurfaceView) findViewById (R.id.video_capture_surface);
//					captureView.setVisibility(View.GONE);
//					mHandler.removeCallbacks(checkVideoEmpty);
//					mSwitch.setVisibility(View.GONE);
//					mMonitor.setVisibility(View.VISIBLE);
//					Log.i("tml GUARD> " + address + "=" + mAddress + " " + found);
//					break;
//				}
//			}
//		}
//		if (!found) {
//			mHandler.postDelayed(checkVideoEmpty, 15000);
//		}
		//***tml
		
		//tml*** zoom
		boolean envzoom = mPref.readBoolean("enVideoZoom", true);
		if (envzoom) {
			((SeekBar) findViewById(R.id.seekBar)).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.incv)).setVisibility(View.VISIBLE);
		} else {
			((SeekBar) findViewById(R.id.seekBar)).setVisibility(View.INVISIBLE);
			((TextView) findViewById(R.id.incv)).setVisibility(View.INVISIBLE);
		}
		
		startOrientationSensor();
		
		setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
		
		mPref.write("recv_pkts", 0);
		mHandler.postDelayed(checkPkt, 3000);
		
		if (mPref.read("moodcontent", "--").endsWith("!!!!"))
			mHandler.postDelayed(displayP2P, 1000);
		
		instance=this;
	}
	
	
	void updatePreview(boolean cameraCaptureEnabled) {
		Log.e("tml updatePreview " + cameraCaptureEnabled);
		mVideoCaptureViewReady = null;
		((SurfaceView) findViewById(R.id.video_capture_surface)).setVisibility(View.VISIBLE);
		((FrameLayout) findViewById(R.id.video_frame)).requestLayout();
	}
	
	int pW,pH;
	@SuppressLint("NewApi")
	void fixZOrder(SurfaceView video, SurfaceView preview) {
		video.setZOrderOnTop(false);
		preview.setZOrderOnTop(true);
		
		VoipCore lVoipCore = AireVenus.instance().getVoipCore();
		if (lVoipCore!=null)
		{
			VoipCall myCall = lVoipCore.getCurrentCall();
			VideoSize v = lVoipCore.getVideoSize(myCall);
			pW=v.width;
			pH=v.height;
			Log.i("Preview window: "+pW+"x"+pH);
			
			Display getOrient = getWindowManager().getDefaultDisplay();

	        int orientation = getOrient.getRotation();
	        if (orientation==Surface.ROTATION_0)
	        {
	        	try{
					int w=(pW<pH)?pW:pH;
					int h=(pW>pH)?pW:pH;
					RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams((int)(100f*mDensity), (int)(100f*h/w*mDensity));
					lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
					lp.setMargins((int)(15f*mDensity), 0, 0, (int)(90f*mDensity));
					preview.setLayoutParams(lp);
				}catch(Exception e){
					Log.e("fixZOrder1 !@#$ " + e.getMessage());
				}
	        } else {
	        	try{
					int w=(pW>pH)?pW:pH;
					int h=(pW<pH)?pW:pH;
					RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams((int)(100f*w/h*mDensity), (int)(100f*mDensity));
					lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
					lp.setMargins((int)(15f*mDensity), 0, 0, (int)(15f*mDensity));
					preview.setLayoutParams(lp);
				}catch(Exception e){
					Log.e("fixZOrder2 !@#$ " + e.getMessage());
				}
	        }
		}
	}


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
    
	@Override
	protected void onResume() {
		if (AireVenus.getLc() != null && !AireVenus.getLc().isIncall()) {
			Log.e("VC onResume !isIncall");
			finish();
		} else if (getIntent().getBooleanExtra("restart", false)) {  //tml*** return Dialer view
			Log.e("VC restart!");
			finish();
			DialerActivity.getDialer().startVideoView(100);
			super.onResume();
			return;
		}
		
		super.onResume();
		disableKeyguard();
		if (mVideoViewReady != null)
			((GLSurfaceView)mVideoViewReady).onResume();
		launched=true;

		if (videoCall != null) {
			videoCall.enableCamera(true);
			updatePreview(true);
		}
		
		NotificationManager mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mNM.cancel(R.string.call);
		
		//tml*** chatview2
		IntentFilter intentToReceiveFilter = new IntentFilter();
		intentToReceiveFilter.addAction(Global.Action_SMS_Fail);
		intentToReceiveFilter.addAction(Global.Action_MsgGot);
		intentToReceiveFilter.addAction(Global.Action_MsgSent);
		intentToReceiveFilter.addAction(Global.Action_InternalCMD);
		intentToReceiveFilter.addAction(Global.ACTION_PLAY_OVER);
		intentToReceiveFilter.addAction(Global.MSG_UNREAD_YES);
		intentToReceiveFilter.addAction(Global.MSG_RETURN_NOM);
		this.registerReceiver(HandleListChanged, intentToReceiveFilter);
		
//		MobclickAgent.onResume(this);
		Log.e("*** !!! VIDEOCALL *** RESUME RESUME !!! *** voip");
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

	@Override
	protected void onDestroy() {
		if (androidVideoWindowImpl != null) { // Prevent App from crashing if correspondent hang up while you are rotating
			androidVideoWindowImpl.release();
		}
		instance=null;
		//mRefreshTimer.cancel();
		mHandler.removeCallbacks(checkVideoEmpty);
		mHandler.removeCallbacks(displayP2P);
		mHandler.removeCallbacks(checkPkt);
		
		NotificationManager mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mNM.cancel(R.string.call);
		
		//tml*** chatview2
		if (msgListAdapter != null) {
			msgListAdapter.clear();
		}
		if (mDB != null && mDB.isOpen())
			mDB.close();
		if (mADB!=null && mADB.isOpen()) 
			mADB.close();
		unregisterReceiver(HandleListChanged);
		spannableCache = null;
		
		mPref.delect("MonitorWho");  //tml*** monitor record/

		System.gc();
		System.gc();
		Log.e("*** VIDEOCALL *** DESTROY DESTROY ***");
		super.onDestroy();
	}
	
	@SuppressWarnings("deprecation")
	private void showNotification() {
		Notification notification = new Notification(R.drawable.icon_incall, 
				getString(R.string.app_name),
				System.currentTimeMillis());

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, VideoCallActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
		        
		notification.setLatestEventInfo(this, getString(R.string.app_name),
				getString(R.string.in_call), contentIntent);
		
		notification.defaults=0;
		notification.flags=Notification.FLAG_ONGOING_EVENT;

		NotificationManager mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mNM.notify(R.string.call, notification);
	}
	
	@Override
	protected void onPause() {
		// Send NoWebcam since Android 4.0 can't get the video from the
		// webcam if the activity is not in foreground
		if (videoCall != null)
			videoCall.enableCamera(false);
		
		if (isFinishing())
			videoCall = null; // release reference
		else
			showNotification();
		launched=false;
		
		synchronized (androidVideoWindowImpl) {
			/* this call will destroy native opengl renderer
			 * which is used by androidVideoWindowImpl
			 */
			if (AireVenus.getLc()!=null)
				AireVenus.getLc().setVideoWindow(null);
			
			androidVideoWindowImpl.setOpenGLESDisplay(0);//alec
		}

		if (mWakeLock.isHeld())	mWakeLock.release();
		reenableKeyguard();
		if (mOrientationHelper != null) mOrientationHelper.disable();
//		MobclickAgent.onPause(this);
		super.onPause();
		if (mVideoViewReady != null)
			((GLSurfaceView)mVideoViewReady).onPause();

		Log.e("*** !!! VIDEOCALL *** PAUSE PAUSE !!! ***");
	}
	
	private void vibrate()
	{
		Vibrator mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		long[] patern = {0,40,1000};
		mVibrator.vibrate(patern, -1);
	}

	public void bye() 
	{	
		vibrate();
		setResult(RESULT_OK);
		mAlwaysChangingPhoneAngle = -1;
		finish();
	}
	
	static VideoCallActivity instance;
	
	static VideoCallActivity getInstance()
	{
		return instance;
	}
	
	long enabletime;
    KeyguardManager mKeyguardManager;
    @SuppressWarnings("deprecation")
	KeyguardManager.KeyguardLock mKeyguardLock;
    boolean enabled;
    
	@SuppressWarnings("deprecation")
	void disableKeyguard() {
		if (AmazonKindle.IsKindle()) return;
    	if (mKeyguardManager == null) {
	        mKeyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
	        mKeyguardLock = mKeyguardManager.newKeyguardLock("VideoCall_KeyGuard");
	        enabled = true;
    	}
		if (enabled) {
			mKeyguardLock.disableKeyguard();
			enabled = false;
			enabletime = SystemClock.elapsedRealtime();
		}
	}
	
	@SuppressWarnings("deprecation")
	void reenableKeyguard() {
		if (AmazonKindle.IsKindle()) return;
		if (!enabled) {
			mKeyguardLock.reenableKeyguard();
			enabled = true;
		}
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
					vibrate();
					return;
				}
			}
		}
		super.onBackPressed();
	}
	
	//tml*** backpress safety
	private boolean mUseBackKey = true;
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && mUseBackKey) {
			Toast.makeText(VideoCallActivity.this, "Press back again to quit.", Toast.LENGTH_SHORT).show();
			mUseBackKey = false;
			return false;
		} else if (keyCode == KeyEvent.KEYCODE_BACK && !mUseBackKey) {
			mUseBackKey = true;
		}
		return super.onKeyDown(keyCode, event);
	}
	//***tml
	
	private OrientationEventListener mOrientationHelper;
	private synchronized void startOrientationSensor() {
		if (mOrientationHelper == null) {
			mOrientationHelper = new LocalOrientationEventListener(this);
		}
		mOrientationHelper.enable();
	}
	
	static public int mAlwaysChangingPhoneAngle = -1;
	int temptToRotate=0;
	private class LocalOrientationEventListener extends OrientationEventListener {
		int prevDeg = 0;  //tml debug
		
		public LocalOrientationEventListener(Context context) {
			super(context);
		}
		@Override
		public void onOrientationChanged(final int o) {
			if (o == OrientationEventListener.ORIENTATION_UNKNOWN) return;
			
			int degrees = 270;
			if (o < 60 || o >300) degrees=0;//portait
			else if (o<135) degrees=90;
			else if (o<225) degrees=180;
			else degrees = 270;
			if (prevDeg != degrees) {  //tml debug
				Log.d("Degrees>>  " + degrees);
				prevDeg = degrees;
			}
			
			if (mAlwaysChangingPhoneAngle == degrees) return;
			temptToRotate++;
			if (temptToRotate<2) return;
			
			temptToRotate=0;
			
			mAlwaysChangingPhoneAngle = degrees;
			
			int rotation = (360 - mAlwaysChangingPhoneAngle) % 360;
			
//			Log.d("Degrees|Rotation>>  " + mAlwaysChangingPhoneAngle + "|" + rotation);
			VoipCore vc=AireVenus.getLc();
			if (vc!=null)
			{
				vc.setDeviceRotation(rotation);
				VoipCall currentCall = vc.getCurrentCall();
				if (currentCall != null && currentCall.getCurrentParamsCopy().getVideoEnabled()) {
					vc.updateCall(currentCall, null);
					Log.e("********* RESTART VIDEOCALL");
				}
			}
		}
	}
	
	private int gVideoEmpty=0;
	private Handler mHandler=new Handler();
	
	Runnable checkVideoEmpty=new Runnable(){
		public void run()
		{
			VoipCore lc=AireVenus.getLc();
			if (lc!=null && lc.isIncall())
			{
				VoipCall c = lc.getCurrentCall();
				gVideoEmpty=c.isVideoEmpty();
				
				if (gVideoEmpty>0)
				{
					((TextView)findViewById(R.id.no_video_hint)).setVisibility(View.VISIBLE);
					mHandler.postDelayed(checkVideoEmpty, 5000);
				}else{
					((TextView)findViewById(R.id.no_video_hint)).setVisibility(View.GONE);
					mHandler.postDelayed(checkVideoEmpty, 15000);
				}
			}
		}
	};
	
	int nVideoDisplay;
	int nVideoEncFrames;
	private int displayP2Pcount = 0;
	private int pktssent0 = 0, pktsrecv0 = 0;
	int pktssentavg = 0, pktsrecvavg = 0;
	Runnable displayP2P=new Runnable(){
		public void run()
		{
			VoipCore lc=AireVenus.getLc();
			if (lc!=null && lc.isIncall())
			{
				displayP2Pcount++;
				try{
					int status=lc.getStatus();
					ImageView iv=(ImageView)findViewById(R.id.ind0);
					if ((status&0xF00)==0x100)
						iv.setImageResource(R.drawable.purple);
					else if ((status&0xF00)==0x200)
						iv.setImageResource(R.drawable.red);
					else if ((status&0xF00)==0x300)
						iv.setImageResource(R.drawable.blue);//PCMA
					else if ((status&0xF00)==0x400)
						iv.setImageResource(R.drawable.teal);//PCMU
					else if ((status&0xF00)==0x600)
						iv.setImageResource(R.drawable.orange);//OPUS
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
					
					int[] ports=lc.getPorts();
					if (displayP2Pcount < 300) {
						pktssentavg = ports[10] / displayP2Pcount;
						pktsrecvavg = ports[11] / displayP2Pcount;
					}
					int fps=ports[22]-nVideoDisplay;
					int encfps=ports[21]-nVideoEncFrames;
					String info="bitrate:"+ports[20]+" fps:"+encfps+" ("+pH+"x"+pW+")"+
						"\ndisplay fps:"+fps+
						"\nrelay local:"+ports[0]+" v:"+ports[1]+
						"\nrelay remote:"+ports[2]+" v:"+ports[3]+
						"\nice local:"+ports[4]+" v:"+ports[5]+
						"\nice remote:"+ports[6]+" ("+ports[7]+") v:"+ports[8]+" ("+ports[9]+")"+
						"\npkts sent:"+ports[10]+"+"+(ports[10]-pktssent0)+"/"+pktssentavg+" (mic:"+ports[16]+" ec:"+ports[17]+" enc:"+ports[15]+
						")\npkts recv:"+ports[11]+"+"+(ports[11]-pktsrecv0)+"/"+pktsrecvavg+" (ice:"+ports[12]+")"+
						"\nnAPPSent="+ports[18]+" nAPPRecvd="+ports[13]+
						"\npcktinQ:"+ports[14]+
						"\nnTicker:"+ports[19];
					pktssent0 = ports[10];
					pktsrecv0 = ports[11];
					((TextView)findViewById(R.id.debuginfo)).setText(info);
					((FrameLayout)findViewById(R.id.debug)).setVisibility(View.VISIBLE);
					nVideoDisplay=ports[22];
					nVideoEncFrames=ports[21];
				}catch(Exception e){
					Log.e("displayP2P !@#$ " + e.getMessage());
				}
				
				mHandler.postDelayed(displayP2P, 1000);
			}
		}
	};
	
	private Toast mToast;
	Runnable checkPkt=new Runnable(){

		public void run()
		{
			VoipCore lc=AireVenus.getLc();
			if (lc!=null && lc.isIncall())
			{
				try{
					int[] ports=lc.getPorts();
					
					int pkts_pre=mPref.readInt("recv_pkts", 0);
					if (ports[11]<=pkts_pre) {
						Log.e("vca.checkPkt>> " + ports[11] + "<=?" + pkts_pre);
						if (mToast!=null) {
							mToast.setText(getString(R.string.network_prompt));
						}else{
							mToast=Toast.makeText(getApplicationContext(), getString(R.string.network_prompt), 0);
						}
						//tml*** conn notify
						mToast.setDuration(Toast.LENGTH_SHORT);
						mToast.setGravity(Gravity.TOP, 0, 0);
						//***tml
						mToast.show();
					}
					mPref.write("recv_pkts", ports[11]);
					
				}catch(Exception e){
					Log.e("checkPkt !@#$ " + e.getMessage());
				}
				
				mHandler.postDelayed(checkPkt, 2000);
			}
		}
	};
	/*
	TimerTask refreshOpenGLDisplay = new TimerTask() {
		@Override
		public void run() {
			if (androidVideoWindowImpl!=null)
				androidVideoWindowImpl.requestRender();
		}
	};*/
	
	//yang*** zoom
	private int zoomRatio = 1;
	private int z_x = 1, z_y = 1;
	long lasttime = 0;
	long crrutetime = 0;
	float x1, x2, zx, y1, y2, zy;
	int mx=0;int my=0;
	private int zratio = 0;

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		Log.d("yang set seek bar progress=" + progress + "progress/100" + (progress / 100));
		zratio = progress;
		((TextView) findViewById(R.id.incv)).setText(progress + "%"
			//	+ ",x=" + zx + ",y=" + zy
				);
		resetXY();
		createZoomWindow((int) mx, (int) my, zratio);
		Log.d("yang progress zoom done");
	}
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	public void resetXY() {
//		zx = mx;
//		zy = my;
	}
	
	private void createZoomWindow(int x1, int y1, int zratio) {
		Log.d("yang progress in create zoom window");
		VoipCore zVoipCore = AireVenus.instance().getVoipCore();
//		zVoipCore.setVideoZoom(x1, y1, 320, 240); // 256 x1x2x3 zoom
		if (zratio == 0) {
			//resetall();
			mx=0;my=0;
			zVoipCore.zoomVideo(256, mx, my);
			Log.d("yang rest setVideoZoom 320x240 x1="+x1+",y1="+y1+"pw,ph="+pW+","+pH);
		} else if (0 < zratio && zratio <= 100) {
			Log.d("yang zoom in <1" + "zratio=" + zratio);
			zVoipCore.zoomVideo(((1080 - 512) * zratio / 100 + 256), x1, y1);
			Log.d("yang zoom zratio = " + ((1080 - 384) * zratio / 100 + 384) + "x1=" + x1 + ",y1=" + y1);
		}
		Log.d("vivid__test video_call_zoom_req x,y,r=" + x1 + "," + y1 + "," + (zratio));
	}
	
	float getDistance(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}
	
	private float beforeLenght, afterLenght;
	void onPointerDown(MotionEvent event) {
		if (event.getPointerCount() >= 2) {
			beforeLenght = getDistance(event);
		}
	}
	
	public void resetall(){
		x1=0;y1=0; x2=0; y2=0; zx=0; zy=0;mx=0;my=0;
	}

	int lastDist = 0, lastMovedDist = 0;
	int movedDist = 0;
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//tml*** pinch zoom
		boolean envzoom = mPref.readBoolean("enVideoZoom", true);
		if (envzoom && !chatViewON) {
			int action2 = event.getActionMasked();
			int action1 = event.getAction();
			int pointerCount = event.getPointerCount();
			int xDown2 = 0, yDown2 = 0, xDown1 = 0, yDown1 = 0;
			int xMove2 = 0, yMove2 = 0, xMove1 = 0, yMove1 = 0;
			int xUp2 = 0, yUp2 = 0, xUp1 = 0, yUp1 = 0;
			int xp2 = 0, yp2 = 0, xp1 = 0, yp1 = 0;
			int pinchStep = 5;
			
			if (pointerCount == 2) {
				switch (action2) {
					case MotionEvent.ACTION_DOWN:
//						xDown2 = (int) event.getX(1);
//						yDown2 = (int) event.getY(1);
						break;
					case MotionEvent.ACTION_MOVE:
						xMove2 = (int) event.getX(1);
						yMove2 = (int) event.getY(1);
						break;
					case MotionEvent.ACTION_UP:
//						xUp2 = (int) event.getX(1);
//						yUp2 = (int) event.getY(1);
						Log.d("pinch UP2_");
						break;
					default:
						return false;
				}
				
				switch (action1) {
					case MotionEvent.ACTION_DOWN:
//						xDown1 = (int) event.getX(0);
//						yDown1 = (int) event.getY(0);
						break;
					case MotionEvent.ACTION_MOVE:
						xMove1 = (int) event.getX(0);
						yMove1 = (int) event.getY(0);
						break;
					case MotionEvent.ACTION_UP:
//						xUp1 = (int) event.getX(0);
//						yUp1 = (int) event.getY(0);
						Log.d("pinch UP1_");
						break;
					default:
						return false;
				}
				
				int xDistSqr = (xMove1 - xMove2) * (xMove1 - xMove2);
				int yDistSqr = (yMove1 - yMove2) * (yMove1 - yMove2);
				int curDist = (int) Math.sqrt(xDistSqr + yDistSqr);
				if (lastDist == 0) {
//					Log.e("tmlpinch lastDist reset=" + curDist);
					lastDist = curDist;
				}
				movedDist = curDist - lastDist;
				movedDist = lastMovedDist + movedDist;
//				Log.e("tmlpinch movedDist=" + movedDist);
				if (Math.abs(movedDist) >= pinchStep) {
					if (movedDist >= pinchStep) {
						int inc = movedDist / pinchStep;
						int progress = ((SeekBar) findViewById(R.id.seekBar)).getProgress() - inc;
//						Log.e("tmlpinch out.inc=" + inc);
						if (progress >= 0) {
							((SeekBar) findViewById(R.id.seekBar)).setProgress(progress);
						} else {
							((SeekBar) findViewById(R.id.seekBar)).setProgress(0);
						}
					} else if (movedDist <= -pinchStep) {
						int inc = movedDist / -pinchStep;
						int progress = ((SeekBar) findViewById(R.id.seekBar)).getProgress() + inc;
//						Log.e("tmlpinch in.inc=" + inc);
						if (progress <= 100) {
							((SeekBar) findViewById(R.id.seekBar)).setProgress(progress);
						} else {
							((SeekBar) findViewById(R.id.seekBar)).setProgress(100);
						}
					}
					Log.d("pinch f1=" + xMove1 + "x" + yMove1 + " f2=" + xMove2 + "x" + yMove2 + " d=" + curDist + " " + movedDist);
					movedDist = 0;
				}
				lastDist = curDist;
				lastMovedDist = movedDist;
			} else {
				lastDist = 0; lastMovedDist = 0;
				movedDist = 0;
			}
		}
		//***tml
		return this.detector.onTouchEvent(event);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		boolean envzoom = mPref.readBoolean("enVideoZoom", true);
		if (envzoom && !chatViewON) {
			x1=e1.getX();
			x2 = e2.getX();
			y1=e1.getY();
			y2 = e2.getY();
//			e2.get
			zx = x1 - x2;
			zy = y2 - y1;

			WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
			int width = wm.getDefaultDisplay().getWidth();
			int height = wm.getDefaultDisplay().getHeight();
			Log.d("yang==zx,zy"+zx+","+zy+",width,height="+width+","+height);
			if ((int) zx <= 0) {
				Log.d("yang in zx<0");
				if (Math.abs((int) zx) < width / 4) {
					mx = mx - 16;
					if (mx < 0) {
						mx = 0;
					}
				}
				if (width / 4 <= Math.abs((int) zx)
						&& Math.abs((int) zx) < width / 2) {
					mx = mx - 16;
					if (mx < 0) {
						mx = 0;
					}
				}
				if (width / 2 <= Math.abs((int) zx)
						&& Math.abs((int) zx) < 3 * width / 4) {
					mx = mx - 32;
					if (mx < 0) {
						mx = 0;
					}
				}
				if (3 * width / 4 <= Math.abs((int) zx)
						&& Math.abs((int) zx) < width) {
					mx = mx - 32;
					if (mx < 0) {
						mx = 0;
					}
				}
			}
			
	     else if ((int) zx > 0) {
	    	 Log.d("yang in zx>0");
				if (Math.abs((int) zx) < width / 4) {
					mx = mx + 16;
					if(mx>256){
						mx=256;
					}
				}
				if (width / 4 <= Math.abs((int) zx)
						&& Math.abs((int) zx) < width / 2) {
					mx = mx + 16;
					if(mx>256){
						mx=256;
					}
				}
				if (width / 2 <= Math.abs((int) zx)
						&& Math.abs((int) zx) < 3 * width / 4) {
					mx = mx + 32;
					if(mx>256){
						mx=256;
					}
				}
				if (3 * width / 4 <= Math.abs((int) zx)
						&& Math.abs((int) zx) < width) {
					mx = mx + 32;
					if(mx>256){
						mx=256;
					}
				}
			}

			if ((int) zy <= 0) {
				Log.d("yang in zy<0");
				if (Math.abs((int) zy) < height / 10) {
					my = my - 16;
					if (my < 0) {
						my = 0;
					}
				}
				if (height /10 <= Math.abs((int) zy)
						&& Math.abs((int) zy) < height / 3) {
					my = my - 32;
					if (my < 0) {
						my = 0;
					}
				}
				if (height / 3 <= Math.abs((int) zy)
						&& Math.abs((int) zy) < 3 * height / 4) {
					my = my - 48;
					if (my < 0) {
						my = 0;
					}
				}
				if (3 * height / 4 <= Math.abs((int) zy)
						&& Math.abs((int) zy) < height) {
					my = my - 64;
					if (my < 0) {
						my = 0;
					}
				}
			}
	     	else if ((int) zy > 0) {
	     		Log.d("yang in zy>0");
				if (Math.abs((int) zy) < height / 10) {
					my = my + 16;
					if(my>256){
						my=256;
					}
				}
				if (height / 10 <= Math.abs((int) zy)
						&& Math.abs((int) zy) < height / 3) {
					my = my + 32;
					if(my>256){
						my=256;
					}
				}
				if (height / 3 <= Math.abs((int) zy)
						&& Math.abs((int) zy) < 3 * height / 4) {
					my = my + 48;
					if(my>256){
						my=256;
					}
				}
				if (3 * height / 4 <= Math.abs((int) zy)
						&& Math.abs((int) zy) < height) {
					my = my + 64;
					if(my>256){
						my=256;
					}
				}
			}
//			mx= 128;
			Log.d("zx =" + mx + ",zy=" + my + "screenw=+" + width + ",sreenH="
					+ height);
			if(my==256){
				makeTomast("Reaches the up edge");
			}
			if(my==0){
				makeTomast("Reaches the down edge");
			}
			if(mx==0){
				makeTomast("Reaches the left edge");
			}
			if(mx==256){
				makeTomast("Reaches the right edge");
			}
//			((TextView) findViewById(R.id.incv)).setText("x=" + mx + "y="+my);
			createZoomWindow((int) mx, (int) my, zratio);
		}
		
		return true;
	}

	public void makeTomast(String str){
		Log.d("Zedge: " + str);
//		 Toast tst = Toast.makeText(getApplicationContext(),
//		 str, Toast.LENGTH_SHORT);
//		 tst.setDuration(Toast.LENGTH_SHORT);
//		 tst.setGravity(Gravity.CENTER, 0, 0);
//		 tst.show();
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		boolean envzoom = mPref.readBoolean("enVideoZoom", true);
		if (envzoom && !chatViewON) {
			if(0<=zratio&& zratio<25){
				zratio=25;
			}else if(25<=zratio&&zratio<50){
				zratio=50;
			} else if(50<=zratio&&zratio<75){
				zratio=75;
			}else if(75<=zratio&&zratio<100){
				zratio=100;
			}else if( zratio==100){
				zratio= 0;
			}
			Log.d("yang ondoubletap");
			seekbar.setProgress(zratio);
			createZoomWindow((int) mx, (int) my, zratio);
		}
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}
	//***yang
	//tml*** chatview2
	private void ArrangeTalkList() {
		TalkList.clear();
		Cursor c = mDB.fetchMessages(mAddress, listnumber);
		if (c == null) return;

		do {
			SMS msg = new SMS(c);
			TalkList.add(msg);
		} while (c.moveToNext());
		
		if(c != null && !c.isClosed()) c.close();
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
		} catch (Exception e) {}
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

	final int action_nothing=0;
	final int action_resend=1;
	final int action_save_as=2;
	int item2_action;

	boolean handleLongPress(int position) {
		if (position==-1) {
			return false;
		}
//		if (mp2 != null && mp2.isPlaying())
//			return false;
		if (TalkList!=null) {
			
		
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
			} else
				d = new CharSequence[2];
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
		}
		d[0] = getResources().getString(R.string.delete_msg);
		d[1] = getResources().getString(R.string.copysms);
	  
		new AlertDialog.Builder(VideoCallActivity.this)
				.setTitle(mNickname)
				.setItems(d, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (which == 0) {
							new AlertDialog.Builder(VideoCallActivity.this)
									.setTitle(R.string.delete_confirm)
									.setMessage(R.string.delete_thread_confirm)
									.setPositiveButton(
											R.string.yes,
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialog,
														int whichButton) {
													
//													if (fileUploading)
//													{
//														for (int i=0;i<TalkList.size();i++)
//														{
//															final SMS msg = TalkList.get(i);
//															if (msg_smsid == msg.smsid)
//															{
//																new MyNet(VideoCallActivity.this).stopUploading(msg.att_path_aud);
//																fileUploading=false;
//																break;
//															}
//														}
//													}
													
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
								if (!MyUtil.checkSDCard(VideoCallActivity.this))
									return;
								String aSrcImagePath = msg.att_path_img;
								File fromFile = new File(aSrcImagePath);
								String[] items = aSrcImagePath.split("/");
								File toFile = new File(
										Global.SdcardPath_downloads
												+ items[items.length - 1]);
								MyUtil.copyFile(fromFile, toFile, true,
										VideoCallActivity.this);
								//tml*** beta ui, save dialog
								Toast.makeText(getApplicationContext(), Global.SdcardPath_downloads,
										Toast.LENGTH_SHORT).show();
							}else if (item2_action==action_resend) 
							{
								if (ConversationActivity.fileUploading) {
									Toast.makeText(getApplicationContext(),
											getString(R.string.fileuploading),
											Toast.LENGTH_SHORT).show();
									return;
								}
								SendAgent failagent = new SendAgent(
										VideoCallActivity.this,
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
		}
		return true;
	}

	@SuppressWarnings("deprecation")
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
		Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT)
				.show();
	}

	@SuppressLint("NewApi")
	public class MsgListAdapter extends BaseAdapter {
		private Context mContext;
		private boolean largeScreen = false;
		private boolean inGroup = false;

		public MsgListAdapter(Context context) {
			mContext = context;
			largeScreen = false;
			inGroup = false;
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

		@SuppressWarnings("deprecation")
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
//				holder.warnUnsent = (ImageView) convertView.findViewById(R.id.warnunsent);
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
				if (largeScreen)
				{
					holder.balloon.setMaxWidth((int)(480*mDensity));
					holder.balloon.setPadding((int)(16*mDensity), (int)(16*mDensity), (int)(16*mDensity), (int)(16*mDensity));
					holder.balloon.setTextSize(22);
				}else{
					holder.balloon.setMaxWidth((int)(240*mDensity));  //240
					holder.balloon.setPadding((int)(8*mDensity), (int)(8*mDensity), (int)(8*mDensity), (int)(8*mDensity));
					holder.balloon.setTextSize(16);
				}
				((RelativeLayout)convertView).addView(holder.balloon, new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				
				if (!msg.content.startsWith("(g.f"))
					holder.gifview.stop();
			}

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
				holder.balloon.setAutoLinkMask(Linkify.ALL);
			}
			String time = ShowBetterTime(TalkList.size() - position - 1);
			holder.tTime.setText(time);
			RelativeLayout.LayoutParams lpTime = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lpTime.addRule(RelativeLayout.CENTER_HORIZONTAL);
			lpTime.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			holder.tTime.setId(1);
			holder.tTime.setLayoutParams(lpTime);

			RelativeLayout.LayoutParams lpPhoto;
			if (largeScreen)
			{
				lpPhoto = new RelativeLayout.LayoutParams((int)(50.*mDensity), (int)(50.*mDensity));
				holder.photoimage.setPadding((int)(5.*mDensity), (int)(5.*mDensity), (int)(5.*mDensity), (int)(5.*mDensity));
			}else{
				lpPhoto = new RelativeLayout.LayoutParams((int)(40.*mDensity), (int)(40.*mDensity));
				holder.photoimage.setPadding((int)(4.*mDensity), (int)(4.*mDensity), (int)(4.*mDensity), (int)(4.*mDensity));
			}
			
			if (msg.type == 1) {
				lpPhoto.setMargins(largeScreen?9:6, 0, 0, 0);
				lpPhoto.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				if (inGroup)//alec
				{
					Drawable f=friendsPhotoMap.get(Integer.valueOf(msg.group_member));
					holder.photoimage.setImageDrawable(f);
				}
				else
					holder.photoimage.setImageDrawable(friendPhoto);
			} else {
				lpPhoto.setMargins(0, 0, largeScreen?9:6, 0);
				lpPhoto.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				holder.photoimage.setImageDrawable(myphoto);
			}
			lpPhoto.addRule(RelativeLayout.BELOW, holder.tTime.getId());
			lpPhoto.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			holder.photoimage.setLayoutParams(lpPhoto);
			
			holder.balloon.setId(2);
			holder.gifview.setId(3);
			holder.photoimage.setId(4);

			if (msg.content!=null && msg.content.startsWith("[<AGREESHARE>]")) {
				String[] res = msg.content.split(",");
				int relation = Integer.valueOf(res[2]);
				msg.content = mContext.getString(
						R.string.agree_share_sms,
						mContext.getResources().getStringArray(
								R.array.share_time)[relation - 1]);
			}
			
			if ((msg.type==2 || (msg.type==1 && !msg.content.contains("(fl)"))) 
					&& (msg.attached == 8 || msg.attached == 10) && msg.att_path_aud.contains(".mp4")) 
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
			
			RelativeLayout.LayoutParams lpProgress = new RelativeLayout.LayoutParams((int)(200.*mDensity), (int)(36.*mDensity));
			lpProgress.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			lpProgress.addRule(RelativeLayout.BELOW, holder.balloon.getId());
			lpProgress.addRule(RelativeLayout.LEFT_OF, holder.photoimage.getId());
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
									DisplayMetrics displaymetrics = new DisplayMetrics();
									getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
									Display getOrient = getWindowManager().getDefaultDisplay();
							        int orientcVpos = getOrient.getRotation();
							        int width = 0;
							        if (orientcVpos == Surface.ROTATION_0) {
										width = displaymetrics.widthPixels;
							        } else {
										width = displaymetrics.widthPixels / 2;
							        }
//									picturebitmap = ImageUtil.getBitmapAsRoundCornerWithAdaptiveDivision(3, 15, msg.att_path_img);
									//tml*** hotnews
									int widthSpace = width - (int) ((40 + 40 + 16 + 20 + 30) * mDensity);  //profilepicx2 + balloon padding + margins
									picturebitmap = ImageUtil.getBitmapAsRoundCornerWithAdaptiveDivision(2, 15, msg.att_path_img, widthSpace);
								}catch(Exception e){
									picturebitmap=null;
								}catch(OutOfMemoryError e){
									picturebitmap=null;
								}
								
								if (picturebitmap == null) {
									spannable = new SpannableString(getString(R.string.notfound_photo));
								} else {
									int start = sm.getStart(i, j);
									int end = sm.getEnd(i, j);
									
									ImageSpan icon = new ImageSpan(
											VideoCallActivity.this, picturebitmap,
											ImageSpan.ALIGN_BASELINE);
									spannable.setSpan(
													icon,
													start,
													end,
													SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
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
										if (Build.VERSION.SDK_INT >= 8) {
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
										d.setBounds(0, 0, (int)(30.f*mDensity), (int)(30.f*mDensity));
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
										Bitmap bubbleblue = BitmapFactory.decodeResource(VideoCallActivity.this.getResources(),
												R.drawable.videosms_play);
										Drawable[] array = new Drawable[2];
										array[1] = new BitmapDrawable(bubbleblue);
										array[0] = new BitmapDrawable(bitmap);
										layers= new LayerDrawable(array);
										layers.setLayerInset(0, 0, 0, 0, 0);
										layers.setLayerInset(1, 0, 0, 0, 0);
										layers.setBounds(0, 0, (int)(90.f*mDensity), (int)(90.f*mDensity));
										bubbleblue = null;
									}
									if (icon == null){
										if(msg.attached==9){
											if (Integer.parseInt(Build.VERSION.SDK) >= 8)
												icon = new ImageSpan(layers,ImageSpan.ALIGN_BASELINE);
											else
												icon = new ImageSpan(getResources().getDrawable(R.drawable.start_play),ImageSpan.ALIGN_BASELINE);
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
								bitmap = null;
							} else if(i>=75){
								hasGif = true;
								
								Drawable dra=getResources().getDrawable(R.drawable.em001+i-75);
								//int h, w;
								//h=(int)(dra.getIntrinsicHeight()/mDensity);
								//w=(int)(dra.getIntrinsicWidth()/mDensity);
								holder.gifview.setGifImageType(GifImageType.SYNC_DECODER);
								holder.gifview.setImageSize(150,150);
								//holder.gifview.setShowDimension(w,h);
								holder.gifview.setGifImage(R.drawable.em001+i-75);
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
				holder.balloon.setBackgroundResource(R.drawable.balloon_left);
			else{
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
				lpBubble.setMargins(largeScreen?(int)(60.*mDensity):(int)(50.*mDensity), 0, 0, 0);
				lpBubble.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				holder.gifview.setPadding(15, 3, 14, 8);
//				holder.photoimage.setOnClickListener(mOnClickPhoto);
			} else {
				lpBubble.setMargins(0, 0, largeScreen?(int)(60.*mDensity):(int)(50.*mDensity), 10);
				lpBubble.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				holder.gifview.setPadding(9, 3, 16, 8);
				holder.photoimage.setOnClickListener(null);
			}
			lpBubble.addRule(RelativeLayout.BELOW, holder.tTime.getId());
			
			holder.balloon.setLayoutParams(lpBubble);
			holder.balloon.setTag(TalkList.size() - position - 1);
			
			
			
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
				RelativeLayout.LayoutParams lpAudioMsgPlayer = new RelativeLayout.LayoutParams((int)(200.*mDensity), (int)(64.*mDensity));
				lpAudioMsgPlayer.addRule(RelativeLayout.ALIGN_TOP, holder.balloon.getId());
				if (msg.type==2)
				{
					lpAudioMsgPlayer.addRule(RelativeLayout.LEFT_OF, holder.photoimage.getId());
					holder.audmsg.setBackgroundResource(R.drawable.balloon_right);
				}else{
					lpAudioMsgPlayer.addRule(RelativeLayout.RIGHT_OF, holder.photoimage.getId());
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

		@SuppressWarnings("deprecation")
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
	}
	
	BroadcastReceiver HandleListChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			
			if (intent==null) return;
			
			if (intent.getAction().equals(Global.Action_MsgGot)) {
				
				ArrangeTalkList();
				
//				if (intent.getStringExtra("autoPath") != null
//						&& intent.getStringExtra("autoPath").length() != 0
//						&& intent.getIntExtra("msgAttach", 0) != 8) {
//					try {
//						if (vmp != null) {
//							vmp.stop();
//							vmp = null;
//						}
//					} catch (Exception e) {
//						vmp = null;
//						e.printStackTrace();
//						Log.e("Converse-listchange1:" + e.getMessage());
//					}
//					onPlayVoiceMemo(intent.getStringExtra("autoPath"));
//				}
//				if (mPref.readBoolean("recvVibrator", true)) {
//					long[] patern = { 0, 20, 1000 };
//					mVibrator.vibrate(patern, -1);
//				}
			} else if (intent.getAction().equals(Global.Action_MsgSent)) {
				if (AireJupiter.notifying) return;
				/*String address=intent.getStringExtra("SendeeAddress");
				if (address!=null && !mAddress.equals(address))
					return;*/
				ArrangeTalkList();
//			} else if (intent.getAction().equals(Global.Action_InternalCMD)) {
//				int command = intent.getIntExtra("Command", 0);
//				if (command == Global.CMD_INCOMING_CALL) {
//					mAttached |= 1; // audio
//					ShowAttchment(0);
//				}
//				return;
//			} else if (intent.getAction().equals(Global.ACTION_PLAY_OVER)) {
//				Log.d("ACTION_PLAY_OVER received");
//				if (spAnimation!=null)
//				{
//					spAnimation.stop();
//					AnimationDrawablestate = true;
//					speaker.setVisibility(View.GONE);
//				}
//				try {
//					if (vmp != null) {
//						vmp.stop();
//						vmp = null;
//					}
//				} catch (Exception e) {
//					vmp = null;
//					e.printStackTrace();
//					Log.e("Converse-listchange2:" + e.getMessage());
//				}
//				return;
			} else if (intent.getAction().equals(Global.Action_SMS_Fail)) {
				Toast.makeText(VideoCallActivity.this,
						getString(R.string.smsfail), Toast.LENGTH_SHORT).show();
				return;
			} else if (intent.getAction().equals(Global.MSG_UNREAD_YES)) {  //tml*** chatview2
				if (((ImageButton) findViewById(R.id.chatview)) != null) {
					if (((LinearLayout) findViewById(R.id.chatframe)).getVisibility() != View.VISIBLE) {
						mHandler.post(new Runnable () {
							@Override
							public void run() {
								((ImageButton) findViewById(R.id.chatview)).performClick();
							}
						});
					}
				}
			}

			if (msgListAdapter != null)
				msgListAdapter.notifyDataSetChanged();
			if (TalkList.size() > 0) {
				listview.setSelection(TalkList.size() - 1);
			}
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
//		if ((mAttached & 1) == 1)
//			msg.att_path_aud = SrcAudioPath;
//		if ((mAttached & 2) == 2)
//			msg.att_path_img = SrcImagePath;
//		if (mAttached == 8) {
//			msg.att_path_aud = SrcAudioPath;
//			if (msg.content.startsWith("(fl)")) {
//				msg.content = getString(R.string.filememo_send) + " " + msg.content;
//			} else {
//				msg.content = getString(R.string.video) + " " + msg.content;
//				msg.attached = 9;
//			}
//		}

		msg.longitudeE6 = mPref.readLong("longitude", 116349386);
		msg.latitudeE6 = mPref.readLong("latitude", 39976279);
		
		rowid = mDB.insertMessage(mAddress, msg.contactid,
				(new Date()).getTime(), 1, msg.status, msg.type, "",
				msg.content, msg.attached, msg.att_path_aud, msg.att_path_img,
				0, msg.longitudeE6, msg.latitudeE6, 0, mNickname, null, 0);
		msg.smsid = rowid;//huan
		
//		if (isFile) {
//			fileAgent.setRowId(rowid);
//		} else {
			agent.setRowId(rowid);
//		}

		mAttached = 0;
		TalkList.add(0, msg);
		((EditText) findViewById(R.id.msginput)).setText("");
		needScrollToEnd=true;
		mHandler.post(RefreshStatus);

//		if (isFile)
//			ShowAttchment(-1);
//		else {
//			ShowAttchment(0);
//		}

		mHandler.postDelayed(new Runnable() {
			public void run() {
				((Button) findViewById(R.id.sendmsg)).setEnabled(true);
//				String input=((EditText) findViewById(R.id.msginput)).getText().toString();
//				if (input.length()==0)
//				{
//					toggleSendVoiceBtn(1);
//				}
			}
		}, 1000);

		Intent it = new Intent(Global.Action_HistroyThread);
		sendBroadcast(it);
	}

	final Runnable RefreshStatus = new Runnable() {
		public void run() {
			if (msgListAdapter != null)
				msgListAdapter.notifyDataSetChanged();
			listview.setSelection(TalkList.size() - 1);
			needScrollToEnd=false;
		}
	};
	
	private void onSend() {
		((Button) findViewById(R.id.sendmsg)).setEnabled(false);
		Log.d("sendMsg addr=" + mAddress);

		mMsgText = ((EditText) findViewById(R.id.msginput)).getText().toString();
		mPref.delect("draft" + mContactId);
//		int len = mMsgText.length();
//		if (mAttached == 3)
//			mMsgText = "(Vm)(iMG)" + (len == 0 ? "" : ("\n" + mMsgText));
//		else if ((mAttached & 1) == 1)
//			mMsgText = "(Vm)" + (len == 0 ? "" : ("\n" + mMsgText));
//		else if ((mAttached & 2) == 2)
//			mMsgText = "(iMG)" + (len == 0 ? "" : ("\n" + mMsgText));
//		else if (mAttached == 8) {
//			if (ConversationActivity.fileUploading) {
//				Toast.makeText(getApplicationContext(),
//						getString(R.string.fileuploading),
//						Toast.LENGTH_SHORT).show();
//				mSend.setEnabled(true);
//				return;
//			}
//			File file = new File(SrcAudioPath);
//			NumberFormat format = DecimalFormat.getInstance();
//			format.setMaximumFractionDigits(2);
//			String length = format.format(file.length() / 1024.0).replace(",", "");
//			try {
//				if (Double.valueOf(length) > 102400) { // 100M
//					Toast.makeText(getApplicationContext(),
//							getString(R.string.fileLarge), Toast.LENGTH_SHORT)
//							.show();
//					mSend.setEnabled(true);
////					mVoice.setVisibility(View.VISIBLE);
////					mSend.setVisibility(View.INVISIBLE);
//					toggleSendVoiceBtn(1);
//					return;
//				}
//			} catch (Exception e) {}
//			if (isVideo)
//				mMsgText = "(vdo)" + length
//						+ (len == 0 ? " KB" : (" KB\n" + mMsgText));
//			else
//				mMsgText = "(fl)" + length
//						+ (len == 0 ? " KB" : (" KB\n" + mMsgText));
//		} else {
			if (mMsgText.trim().equals("")) {  //tml*** msg control
				((EditText) findViewById(R.id.msginput)).setText("");
				((Button) findViewById(R.id.sendmsg)).setEnabled(true);
				return;
			}
//		}
		if (mAddress == null || mAddress.length() == 0) {
			((Button) findViewById(R.id.sendmsg)).setEnabled(true);
			return;
		}
		mAddress = MyTelephony.attachPrefix(VideoCallActivity.this, mAddress);
		
		mADB.updateLastContactTimeByAddress(mAddress, new Date().getTime());
    	if (UsersActivity.sortMethod==1)
    		UsersActivity.needRefresh=true;

//		if (mAttached == 8) {
//			if(videobitmap!=null)
//				videobitmap.recycle();
//			
//			fileAgent = new SendFileAgent(this, myIdx, true);
//			
//			if (inGroup)
//			{
//				fileAgent.setAsGroup(mGroupID);
//				if (!fileAgent.onMultipleSend(addressList, mMsgText, mAttached, SrcAudioPath, SrcImagePath))
//				{
////					mSend.setEnabled(true);
////					mVoice.setVisibility(View.VISIBLE);
////					mSend.setVisibility(View.INVISIBLE);
//					toggleSendVoiceBtn(2);
//					toggleSendVoiceBtn(3);
//				}
//				else{
//					addMsgtoTalklist(true);
//					playSoundTouch();
//				}
//			}
//			else{
//				if (!fileAgent.onSend(mAddress, mMsgText, mAttached,
//						SrcAudioPath, SrcImagePath, false)) {
////					mSend.setEnabled(true);
////					mVoice.setVisibility(View.VISIBLE);
////					mSend.setVisibility(View.INVISIBLE);
//					toggleSendVoiceBtn(2);
//					toggleSendVoiceBtn(3);
//				} else {
//					addMsgtoTalklist(true);
//					playSoundTouch();
//				}
//			}
//			SrcAudioPath = null;
//		} else {
			agent=new SendAgent(VideoCallActivity.this, myIdx, mIdx, true);
			
//			if (inGroup)
//			{
//				agent.setAsGroup(mGroupID);
//				if (!agent.onMultipleSend(addressList, mMsgText, mAttached, SrcAudioPath, SrcImagePath))
//					mSend.setEnabled(true);
//				else {
//					addMsgtoTalklist(false);
//					playSoundTouch();
//				}
//			}
//			else{
				if (!agent.onSend(mAddress, mMsgText, mAttached, SrcAudioPath, SrcImagePath, false))
					((Button) findViewById(R.id.sendmsg)).setEnabled(true);
				else {
					addMsgtoTalklist(false);
//					playSoundTouch();
				}
//			}

			SrcAudioPath = null;
//		}
	}

	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(((EditText) findViewById(R.id.msginput)).getWindowToken(), 0);
	}
	//***tml
}
