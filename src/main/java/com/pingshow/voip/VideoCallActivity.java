package com.pingshow.voip;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
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
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.provider.MediaStore.Video;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ImageSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.AmazonKindle;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MainActivity;
import com.pingshow.airecenter.MessageDetailActivity;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.SMS;
import com.pingshow.airecenter.SendAgent;
import com.pingshow.airecenter.Smiley;
import com.pingshow.airecenter.UserPage;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.SmsDB;
import com.pingshow.airecenter.view.AudioMsgPlayer;
import com.pingshow.gif.GifView;
import com.pingshow.gif.GifView.GifImageType;
import com.pingshow.util.HdmiUtil;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.LedSpeakerUtil;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.video.AndroidVideoWindowImpl;
import com.pingshow.voip.core.VideoSize;
import com.pingshow.voip.core.VoipCall;
import com.pingshow.voip.core.VoipCore;

public class VideoCallActivity extends Activity {
	private SurfaceView mVideoViewReady;
	private SurfaceView mVideoCaptureViewReady;
	private SurfaceView videoView;
	private SurfaceView captureView;
	public static boolean launched = false;
	private VoipCall videoCall;
	private WakeLock mWakeLock;
	private float mDensity = 1.f;
	
	private boolean isMuted;
	private boolean isHeld;
	
	private boolean isFullscreen=true;
	public static boolean minimized;
	private String mAddress;
    private String mNickname;
    private long mContactId;
    private boolean restartVideo;
    private boolean chatViewON = false;
	private int mIdx;
	private int myIdx;
	static private ArrayList<SMS> TalkList = new ArrayList<SMS>();
	private static MsgListAdapter msgListAdapter;
	private ListView listview;
	private int listnumber = 90;
	private Drawable friendPhoto;
	private Drawable myphoto;
	long msg_smsid;
	long msg_org_smsid;
	private Map<Long, SpannableString> spannableCache = new HashMap<Long, SpannableString>();
	private ArrayList<GifView> GifList = new ArrayList<GifView>();
	private boolean inGroup = false;
	private Map<Integer, Drawable> friendsPhotoMap = new HashMap<Integer, Drawable>();
	private long rowid;
	private long rowID=0;
	private float size=24.f;
	private float size2=67.f;
	private Map<String, ProgressBar> mProgress = new HashMap<String, ProgressBar>();
	private String mMsgText;
	private SendAgent agent;
	private String SrcAudioPath;
	private String SrcImagePath;
	private String SrcVideoPath;
	private int mAttached = 0;
	private boolean needScrollToEnd=true;
	private int sW, sH;
	private int saveCallVol = 0;
	private AudioManager mAudioManager;
	private boolean cameraDisabled = false;
	
	public ProgressBar getProgressBar(String fn)
	{
		ProgressBar p = mProgress.get(fn);
		return p;
	}
	
	AndroidVideoWindowImpl androidVideoWindowImpl;

	//tml*** self corners/
	private int cVpos = 0;
	//tml*** monitor record/
	MyPreference mPref;

	//tml|wjx*** for no video hint
	private AmpUserDB mADB;
	private SmsDB mDB;
	private Bitmap photoBitmap;
	private ImageView remotePhoto;
	
	private String[] cpuSet = new String[4];
	
	//private Timer mRefreshTimer = new Timer("GL RefreshTimer");
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.overridePendingTransition(R.anim.freeze, R.anim.freeze);
		
		if (AireVenus.instance() == null) {
			Log.e("exit.No service running: avoid crash by finishing "+getClass().getName());
			// super.onCreate called earlier
			finish();
			return;
		}
		setContentView(R.layout.videocall);
		String scrnLay = ((FrameLayout) findViewById(R.id.video_frame)).getTag().toString();
		Log.e("*** !!! VIDEOCALL ***  START START !!! *** voip (" + scrnLay + ")");

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		sW = metrics.widthPixels;
		sH = metrics.heightPixels;
		
//		MyPreference mPref = new MyPreference(this);
		mPref = new MyPreference(this);  //tml*** monitor record/

		//tml|wjx*** serial for no video hint
		mADB = new AmpUserDB(this);
		mADB.open();
		
		mAddress=getIntent().getStringExtra("address");
		
		String displayName = mADB.getNicknameByAddress(mAddress);
		if (displayName != null) {
			((TextView) findViewById(R.id.displayname)).setText(displayName);
		} else {
			((TextView) findViewById(R.id.displayname)).setText(getResources().getString(R.string.unknown_person));
		}
		((TextView) findViewById(R.id.displayname)).setAlpha((float) 0.5);
		
		mDensity = getResources().getDisplayMetrics().density;

		videoView = (SurfaceView) findViewById(R.id.video_surface); 

		captureView = (SurfaceView) findViewById(R.id.video_capture_surface);
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
		
		ImageView mMute = (ImageView) findViewById(R.id.mute);
		mMute.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (AireVenus.isready())
				{
					isMuted=!isMuted;
					VoipCore p = AireVenus.instance().getVoipCore();
					if (isMuted) {
						p.muteMic(true);
					} else {
						if (!isHeld)//alec
							p.muteMic(false);
					}
					((ImageView)v).setImageResource(isMuted?R.drawable.mute_on:R.drawable.mute_off);
				}
			}
		});
		
		//tml*** speaker
		ImageView mSpeaker = (ImageView) findViewById(R.id.speaker);
		if (mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) == 0) {
			mSpeaker.setImageResource(R.drawable.speaker_off);
		}
//		String hdmiconnected = HdmiUtil.getHdmiState();
//		if (hdmiconnected != null) {
//			if (hdmiconnected.equals("0")) {
//				LedSpeakerUtil.setSpeakerOn();
//				mSpeaker.setImageResource(R.drawable.speaker_on);
//			} else {
//				if (!DialerActivity.speakerOn) {
//					LedSpeakerUtil.setSpeakerOff();
//					mSpeaker.setImageResource(R.drawable.speaker_off);
//				}
//			}
//		}
		saveCallVol = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
		mSpeaker.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
//				Log.e("vol0 " + mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
				if (mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) > 0) {
					mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0, AudioManager.FLAG_SHOW_UI);
					((ImageView) v).setImageResource(R.drawable.speaker_off);
				} else {
					mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, saveCallVol, AudioManager.FLAG_SHOW_UI);
					((ImageView) v).setImageResource(R.drawable.speaker_on);
				}
//				Log.e("vol1 " + mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
//				String speakerstate = LedSpeakerUtil.getSpeakerStatus();
//				if (speakerstate != null) {
//					if (speakerstate.equals("VIC:0")) {
//						LedSpeakerUtil.setSpeakerOn();
//						((ImageView) v).setImageResource(R.drawable.speaker_on);
//					} else {
//						LedSpeakerUtil.setSpeakerOff();
//						((ImageView) v).setImageResource(R.drawable.speaker_off);
//					}
//				}
			}
		});
		//***tml
		
		ImageView mHold= (ImageView) findViewById(R.id.hold);
		mHold.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (AireVenus.isready())
				{
					isHeld=!isHeld;
					VoipCore p = AireVenus.instance().getVoipCore();
					if (isHeld) {
						p.muteMic(true);
						p.setMuteSpeaker(1);
					} else {
						if (!isMuted) //alec
							p.muteMic(false);
						p.setMuteSpeaker(0);
					}
					((ImageView)v).setImageResource(isHeld?R.drawable.hold_on:R.drawable.hold_off);
				}
			}
		});
		
		Button mHangup = (Button) findViewById(R.id.hangup);
		mHangup.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.e("voip.HANGUP1 VC *** USER PRESSED ***");
				VoipCore p=AireVenus.getLc();
				if (p!=null)
				{
					VoipCall c=p.getCurrentCall();
					if (c!=null) p.terminateCall(c);
					setResult(RESULT_OK);
					Log.e("voip.exit.HANGUP1 VC *** USER PRESSED *** OK");
					finish();
				} else {
					Log.e("voip.HANGUP1 VC *** USER PRESSED *** getLc=null");
				}
			}
		});
		mHangup.setEnabled(false);  //tml*** beta ui
		mHandler.postDelayed(delayENHangUp, 2000);
		
//		ImageView mMinimize = (ImageView) findViewById(R.id.minimize);
//		mMinimize.setVisibility(View.GONE);
//		mMinimize.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View arg0) {
////				minimized=true;
////				Intent it;
////				if (MainActivity._this!=null)
////					it=new Intent(VideoCallActivity.this, MainActivity.class);
////				else if (ShoppingActivity._this!=null)
////					it=new Intent(VideoCallActivity.this, ShoppingActivity.class);
////				else if (LocationSettingActivity._this!=null)
////					it=new Intent(VideoCallActivity.this, LocationSettingActivity.class);
////				else if (BDMapViewLocation._this!=null)
////					it=new Intent(VideoCallActivity.this, BDMapViewLocation.class);
////				else if (SecurityNewActivity._this!=null)
////					it=new Intent(VideoCallActivity.this, SecurityNewActivity.class);
////				//tml*** browser save
////				else if (MainBrowser._this!=null)
////					it=new Intent(VideoCallActivity.this, MainBrowser.class);
////				//***tml
////				else
////					it=new Intent(VideoCallActivity.this, MainActivity.class);
////				it.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
////				startActivity(it);
//			}
//		});
		
		videoView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
//				cVpos = 0; //tml*** self corners/
//				isFullscreen=!isFullscreen;
//				if (isFullscreen)
//				{
//					try{
//						RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
//						videoView.setLayoutParams(lp);
//					}catch(Exception e){}
//					
//					try{
//						int w=(pW>pH)?pW:pH;
//						int h=(pW<pH)?pW:pH;
//						RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams((int)(240f*w/h), 240);
//						lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//						lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//						lp.setMargins(0, 0, (int)(20f*mDensity), (int)(15f*mDensity));
//						captureView.setLayoutParams(lp);
////						captureView.setClickable(true);  //tml*** self corners TODO
//					}catch(Exception e){}
//					
//					mHandler.removeCallbacks(showPanel);
//					mHandler.removeCallbacks(hidePanel);
//					mHandler.postDelayed(showPanel, 200);
//					mHandler.postDelayed(hidePanel, 200);
//				}
//				else{
//					LinearLayout panel=(LinearLayout)findViewById(R.id.panel);
//					try{
//						RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
//						lp.addRule(RelativeLayout.ABOVE, panel.getId());
//						videoView.setLayoutParams(lp);
//					}catch(Exception e){}
//					
//					try{
//						int w=(pW>pH)?pW:pH;
//						int h=(pW<pH)?pW:pH;
//						RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams((int)(200f*w/h), 200);
//						lp.addRule(RelativeLayout.ABOVE, panel.getId());
//						lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//						lp.setMargins(0, 0, (int)(60f*mDensity), 0);
//						captureView.setLayoutParams(lp);
////						captureView.setClickable(false);  //tml*** self corners TODO
//					}catch(Exception e){}
//
//					mHandler.removeCallbacks(showPanel);
//					mHandler.postDelayed(showPanel, 500);
//				}
				
				isFullscreen = true;
				mHandler.removeCallbacks(showPanel);
				mHandler.removeCallbacks(hidePanel);
				mHandler.postDelayed(showPanel, 200);
				mHandler.postDelayed(hidePanel, 3000);
			}
		});
		
		//tml*** chatview
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
		((ImageView) findViewById(R.id.chatview)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int chatViewState = ((LinearLayout) findViewById(R.id.chatframe)).getVisibility();
				if (chatViewState == View.GONE) {
					chatViewON = true;
					captureView.setEnabled(false);
					((LinearLayout) findViewById(R.id.chatframe)).setVisibility(View.VISIBLE);

					if (needScrollToEnd)
						mHandler.post(RefreshStatus);

					if (mPref.read("draft" + mContactId, null) != null) {
						String draft1 = mPref.read("draft" + mContactId);
						((EditText) findViewById(R.id.msginput)).setText(draft1);
						((EditText) findViewById(R.id.msginput)).setSelection(draft1.length());
					}

					int w = (pW > pH) ? pW : pH;
					int h = (pW > pH) ? pH : pW;
					if (sW > sH) {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int)(240f*w/h), 240);
						cVpos = 0;
						params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
						params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
						params.setMargins(0, 0, (int)(20f*mDensity), (int)(15f*mDensity));
						captureView.setLayoutParams(params);
					} else {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(200, (int)(200f*h/w));
						cVpos = 2;
						params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
						params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
						params.setMargins((int)(20f*mDensity), (int)(15f*mDensity), 0, 0);
						captureView.setLayoutParams(params);
					}
				} else {
					chatViewON = false;
					captureView.setEnabled(true);
					String draft = ((EditText) findViewById(R.id.msginput)).getText().toString().trim();
					if (draft.length() != 0 && !draft.equals(R.string.textinput)) {
						mPref.write("draft" + mContactId, draft);
					} else if (draft.length() == 0) {
						mPref.delect("draft" + mContactId);
					}
					
					hideKeyboard();
					((LinearLayout) findViewById(R.id.chatframe)).setVisibility(View.GONE);
				}

				int unread = mDB.getUnreadCountByAddress(mAddress);
				if (unread > 0) {
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							mDB.setMessageReadByAddress(VideoCallActivity.this, mAddress);
						}
					}, 200);
				}
				
//				if (isFullscreen) {
					mHandler.removeCallbacks(showPanel);
					mHandler.removeCallbacks(hidePanel);
					mHandler.postDelayed(hidePanel, 1000);
//				} else {
//					videoView.performClick();
//				}
			}
		});

		((ImageView) findViewById(R.id.cancelchat)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				chatViewON = false;
				captureView.setEnabled(true);
				String draft = ((EditText) findViewById(R.id.msginput)).getText().toString().trim();
				if (draft.length() != 0 && !draft.equals(R.string.textinput)) {
					mPref.write("draft" + mContactId, draft);
				} else if (draft.length() == 0) {
					mPref.delect("draft" + mContactId);
				}

				int unread = mDB.getUnreadCountByAddress(mAddress);
				if (unread > 0) {
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							mDB.setMessageReadByAddress(VideoCallActivity.this, mAddress);
						}
					}, 200);
				}
				
				hideKeyboard();
				((LinearLayout) findViewById(R.id.chatframe)).setVisibility(View.GONE);
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
				int w = (pW > pH) ? pW : pH;
				int h = (pW > pH) ? pH : pW;
				try {
					if (!mPref.readBoolean("autoAnswer:" + mAddress, false)) {
						if (cVpos == 0) {
							cVpos = 1;
							if (isFullscreen) {
								RelativeLayout.LayoutParams params;
								if (sW > sH) {
									params = new RelativeLayout.LayoutParams((int)(240f*w/h), 240);
									params.setMargins((int)(20f*mDensity), 0, 0, (int)(15f*mDensity));
								} else {
									params = new RelativeLayout.LayoutParams(200, (int)(200f*h/w));
									params.setMargins((int)(20f*mDensity), 0, 0, (int)(200f*mDensity));
								}
								params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
								params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
								captureView.setLayoutParams(params);
							} else {}
						} else if (cVpos == 1) {
							cVpos = 2;
							if (isFullscreen) {
								RelativeLayout.LayoutParams params;
								if (sW > sH) {
									params = new RelativeLayout.LayoutParams((int)(240f*w/h), 240);
									params.setMargins((int)(20f*mDensity), (int)(15f*mDensity), 0, 0);
								} else {
									params = new RelativeLayout.LayoutParams(200, (int)(200f*h/w));
									params.setMargins((int)(20f*mDensity), (int)(15f*mDensity), 0, 0);
								}
								params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
								params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
								captureView.setLayoutParams(params);
							} else {}
						} else if (cVpos == 2) {
							cVpos = 3;
							if (isFullscreen) {
								RelativeLayout.LayoutParams params;
								if (sW > sH) {
									params = new RelativeLayout.LayoutParams((int)(240f*w/h), 240);
									params.setMargins(0, (int)(15f*mDensity), (int)(20f*mDensity), 0);
								} else {
									params = new RelativeLayout.LayoutParams(200, (int)(200f*h/w));
									params.setMargins(0, (int)(15f*mDensity), (int)(20f*mDensity), 0);
								}
								params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
								params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
								captureView.setLayoutParams(params);
							} else {}
						} else { //if (cVpos == 3) {
							cVpos = 0;
							if (isFullscreen) {
								RelativeLayout.LayoutParams params;
								if (sW > sH) {
									params = new RelativeLayout.LayoutParams((int)(240f*w/h), 240);
									params.setMargins(0, 0, (int)(20f*mDensity), (int)(15f*mDensity));
								} else {
									params = new RelativeLayout.LayoutParams(200, (int)(200f*h/w));
									params.setMargins(0, 0, (int)(20f*mDensity), (int)(200f*mDensity));
								}
								params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
								params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
								captureView.setLayoutParams(params);
							} else {}
						}
					}
				} catch (Exception e) {
					Log.e("self !@#$: @" + cVpos + "_" + e.getMessage());
					cVpos = 0;
				}
			}
		});
		//***tml
		//tml*** disable video
		captureView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (cameraDisabled) {
					cameraDisabled = false;
					if (videoCall != null) {
						videoCall.enableCamera(true);
						updatePreview(true);
						mPref.write("disableVideo", false);
					}
				} else {
					cameraDisabled = true;
					if (videoCall != null) {
						videoCall.enableCamera(false);
						updatePreview(false);
						mPref.write("disableVideo", true);
					}
				}
				Log.e("disabling video " + cameraDisabled);
				return true;
			}
		});
		//tml*** zoom
		ImageView mZoom = (ImageView) findViewById(R.id.zoom);
		mZoom.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((LinearLayout) findViewById(R.id.zoom_control)).getVisibility() == View.VISIBLE) {
					((LinearLayout) findViewById(R.id.zoom_control)).setVisibility(View.GONE);
					mPref.write("showZoomCtrl", false);
				} else {
					((LinearLayout) findViewById(R.id.zoom_control)).setVisibility(View.VISIBLE);
					mPref.write("showZoomCtrl", true);
				}
			}
		});
		if (mPref.readBoolean("showZoomCtrl", false)) {
			((LinearLayout) findViewById(R.id.zoom_control)).setVisibility(View.VISIBLE);
		} else {
			((LinearLayout) findViewById(R.id.zoom_control)).setVisibility(View.GONE);
		}
		ImageView bleft = (ImageView) findViewById(R.id.zoom_left);
		bleft.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				z_x = z_x - 16;
				if (z_x <= 0) {
					z_x = 0;
				}
				if(zoomRatio>1)
				{
					if (z_x <= 64) {
						z_x = 64;
					}
				}
				createZoomWindow(z_x, z_y, zoomRatio);
			}
		});
		ImageView bright = (ImageView) findViewById(R.id.zoom_right);
		bright.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				z_x = z_x + 16;
				if(zoomRatio>1)
				{
					if (z_x >=208) {
						z_x = 208;
					}
				}
				createZoomWindow(z_x, z_y, zoomRatio);
			}
		});
		ImageView bup = (ImageView) findViewById(R.id.zoom_up);
		bup.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				z_y = z_y + 16;
				if (z_y <= 0) {
					z_y = 0;
				}if(zoomRatio>1)
				{
					if (z_y >=208) {
						z_y = 208;
					}
				}
				createZoomWindow(z_x, z_y, zoomRatio);
			}
		});
		ImageView bdown = (ImageView) findViewById(R.id.zoom_down);
		bdown.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				z_y = z_y - 16;if(zoomRatio>1)
				{
					if (z_y <=64) {
						z_y = 64;
					}
				}
				createZoomWindow(z_x, z_y, zoomRatio);
			}
		});
		Button bzoom = (Button) findViewById(R.id.zoominfo);
		bzoom.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				VoipCore zVoipCore = AireVenus.instance().getVoipCore();
				int remoteVideoSize[] = zVoipCore.getVideoRemoteResolution();
				int pWin0 = remoteVideoSize[0];
				int pHin0 = remoteVideoSize[1];
				Log.e("tmlz vivid__test video_call_zoom_req getVideoRemoteResolution=" + pWin0 + "x" + pHin0);
				if (zoomRatio == 1) {
					zoomRatio = 2;
					((Button) findViewById(R.id.zoominfo)).setText("2");
				} else if (zoomRatio == 2) {
					zoomRatio = 3;
					((Button) findViewById(R.id.zoominfo)).setText("3");
				} else if (zoomRatio == 3) {
					zoomRatio = 4;
					((Button) findViewById(R.id.zoominfo)).setText("4");
				} else {
					zoomRatio = 1;
					((Button) findViewById(R.id.zoominfo)).setText("1");
				}
				
				createZoomWindow(z_x, z_y, zoomRatio);
			}
		});
		//***tml
		
		VoipCore vc=AireVenus.getLc();
		if (vc!=null)
		{
			String Brand=Build.BRAND.toLowerCase();
			String Product=Build.PRODUCT.toLowerCase();
			String Model=Build.MODEL.toLowerCase();
			//yang
			if (Model.contains("k200") || Product.contains("k200") || Model.contains("mk809iv") || Brand.contains("mbx") || Model.contains("s82"))//s802
				vc.setDeviceRotation(0); //S802
			else if (Model.contains("a9"))//a9
				vc.setDeviceRotation(90);
			else if (Brand.contains("softwinners") || Product.contains("mars_ml220") || Model.contains("mele m9"))
				vc.setDeviceRotation(0);
			else if (Brand.contains("rk30sdk") && Product.contains("rk30sdk") && Model.contains("rk30sdk"))
				vc.setDeviceRotation(0); //mele
			else if (Model.contains("gt-n8013"))//Galaxy Note 10.1
				vc.setDeviceRotation(0);
			else if (Model.contains("sm-t530nu"))//samsuang pad
			     vc.setDeviceRotation(0);
			else if (Brand.contains("amazon"))//kindle
			     vc.setDeviceRotation(90);
			else
				vc.setDeviceRotation(0);
			
			VoipCall currentCall = vc.getCurrentCall();
			if (currentCall != null && currentCall.getCurrentParamsCopy().getVideoEnabled()) {
				vc.updateCall(currentCall, null);
			}
		}

		//tml|wjx*** for no video hint
		remotePhoto = (ImageView)findViewById(R.id.remote_photo);
		LayoutParams params = (LayoutParams) remotePhoto.getLayoutParams();
		photoBitmap = getUserPhoto(mAddress);

		if (photoBitmap != null) {
			Drawable drawable = new BitmapDrawable(photoBitmap);
			remotePhoto.setBackground(drawable);
			params.width = 250;
			params.height = 250;
		} else {
			remotePhoto.setBackground(getResources().getDrawable(R.drawable.empty_online));
			params.width = 250;
			params.height = 250;
		}
		
//		setProgBarGIF();
		//***tml
		
		if (mPref.readBoolean("autoAnswer:"+mAddress,false) && mPref.readBoolean("way")==false)
		{
			((ImageView) findViewById(R.id.chatview)).setVisibility(View.GONE);  //tml*** chatview
			videoView = (SurfaceView) findViewById(R.id.video_surface);
			videoView.setVisibility(View.GONE);
			mHandler.removeCallbacks(checkVideoEmpty);
			RelativeLayout.LayoutParams rl=new RelativeLayout.LayoutParams((int)(640*mDensity),(int)(480*mDensity));
			rl.addRule(RelativeLayout.CENTER_IN_PARENT);
			captureView.setLayoutParams(rl);
		}
		else{
//			mHandler.postDelayed(checkVideoEmpty, 8000);
			//tml|wjx*** for no video hint
			VoipCore lc=AireVenus.getLc();
			if (lc!=null && lc.isIncall())
			{
				VoipCall c = lc.getCurrentCall();
				isVideoReady=lc.checkVideoReady();
			}
			//noVideoWarning = false;
			Thread checkVideoReady = new Thread() {
				public void run() {
					Log.d("wjx:isVideoReady0==="+isVideoReady);
//					if (isVideoReady){
//						((LinearLayout)findViewById(R.id.rl_no_video_hint)).setVisibility(View.GONE);
//						((ProgressBar)findViewById(R.id.no_video_hint)).setVisibility(View.GONE);
//					}
					VoipCore lc = AireVenus.getLc();
					runOnUiThread(new Runnable() {
					     @Override
					     public void run() {
							((SurfaceView) findViewById(R.id.video_surface)).setVisibility(View.INVISIBLE);
					     }
					});
					while (!lc.checkVideoReady()) {
						MyUtil.Sleep(1000);
					}
					runOnUiThread(new Runnable() {
					     @Override
					     public void run() {
							((SurfaceView) findViewById(R.id.video_surface)).setVisibility(View.VISIBLE);
//							((LinearLayout)findViewById(R.id.rl_no_video_hint)).setVisibility(View.GONE);
							((ImageView)findViewById(R.id.remote_photo)).setVisibility(View.INVISIBLE);
							((ProgressBar)findViewById(R.id.no_video_hint)).setVisibility(View.GONE);
//							((GifView)findViewById(R.id.no_video_hint)).setVisibility(View.GONE);
//							((GifView)findViewById(R.id.no_video_hint)).stop();
					     }
					});
					runOnUiThread(hidePanel);
					mHandler.postDelayed(checkVideoEmpty, 5000);
				};
			};
			checkVideoReady.start();
			//***tml
		}

		//tml*** open orient
		startOrientationSensor();
		
		setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
		
		if (mPref.read("moodcontent", "--").endsWith("!!!!") && Log.enDEBUG)
			mHandler.postDelayed(displayP2P, 1000);
		
//		mHandler.postDelayed(hidePanel, 5000);
				
		instance=this;
		
		//tml|yang*** setCPU
		if (cpuSet[0] == null) {
			cpuSet = MyUtil.getCPU(true);
			MyUtil.setCPU(false, null, null, null);
			MyUtil.getCPU(true);
//			new Thread(checkCPU).start();  //tml*** setCPU2
		}
	}

	Runnable delayENHangUp = new Runnable() {  //tml*** beta ui
		public void run() {
			Log.d("delayENHangUp");
			((Button) findViewById(R.id.hangup)).setEnabled(true);
			((Button) findViewById(R.id.hangup)).requestFocus();  //tml*** prefocus
		}
	};
	
	Runnable showPanel=new Runnable()
	{
		@Override
		public void run() {
			((LinearLayout)findViewById(R.id.panel)).setVisibility(View.VISIBLE);
			((LinearLayout)findViewById(R.id.controls)).setVisibility(View.VISIBLE);
		}
	};
	
	Runnable hidePanel=new Runnable()
	{
		@Override
		public void run() {
			((LinearLayout)findViewById(R.id.panel)).setVisibility(View.GONE);
			((LinearLayout)findViewById(R.id.controls)).setVisibility(View.GONE);
		}
	};
	
	void updatePreview(boolean cameraCaptureEnabled) {
		//mVideoCaptureViewReady = null;
//		findViewById(R.id.video_capture_surface).setVisibility(View.VISIBLE);
//		findViewById(R.id.video_frame).requestLayout();
		if (!cameraCaptureEnabled) {  //tml*** disable video
			((SurfaceView) findViewById(R.id.video_capture_surface)).setBackgroundColor(Color.GRAY);
			((SurfaceView) findViewById(R.id.video_capture_surface)).setVisibility(View.GONE);
			((SurfaceView) findViewById(R.id.video_capture_surface)).setVisibility(View.VISIBLE);
			findViewById(R.id.video_frame).requestLayout();
		} else {
			((SurfaceView) findViewById(R.id.video_capture_surface)).setBackgroundColor(Color.TRANSPARENT);
			((SurfaceView) findViewById(R.id.video_capture_surface)).setVisibility(View.VISIBLE);
			findViewById(R.id.video_frame).requestLayout();
		}
	}
	
	int pW,pH;
	void fixZOrder(SurfaceView video, SurfaceView preview) {
		video.setZOrderOnTop(false);
		preview.setZOrderOnTop(true);

		VoipCore lVoipCore = AireVenus.instance().getVoipCore();
		if (lVoipCore != null)
		{
			VoipCall myCall = lVoipCore.getCurrentCall();
			VideoSize v = lVoipCore.getVideoSize(myCall);
			pW = v.width;
			pH = v.height;
			Log.i("Preview window: "+pW+"x"+pH);
			try {
				int w = (pW > pH) ? pW : pH;
				int h = (pW > pH) ? pH : pW;
				RelativeLayout.LayoutParams lp;
				if (sW > sH) {
					lp = new RelativeLayout.LayoutParams((int)(240f*w/h), 240);
					lp.setMargins(0, 0, (int)(20f*mDensity), (int)(15f*mDensity));
				} else {
					lp = new RelativeLayout.LayoutParams(200, (int)(200f*h/w));
					lp.setMargins(0, 0, (int)(20f*mDensity), (int)(200f*mDensity));
				}
				lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				preview.setLayoutParams(lp);
			} catch (Exception e) {}
		}
	}

 
	@Override
	protected void onResume() {
		if (AireVenus.getLc() != null && !AireVenus.getLc().isIncall()) {
			Log.e("exit.VC onRESUME !isIncall");
			finish();
			super.onResume();
			return;
		}
		
		super.onResume();
		
		minimized=false;
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

		//tml*** chatview
		IntentFilter intentToReceiveFilter = new IntentFilter();
		intentToReceiveFilter.addAction(Global.Action_SMS_Fail);
		intentToReceiveFilter.addAction(Global.Action_MsgGot);
		intentToReceiveFilter.addAction(Global.Action_MsgSent);
		intentToReceiveFilter.addAction(Global.Action_InternalCMD);
		intentToReceiveFilter.addAction(Global.ACTION_PLAY_OVER);
		intentToReceiveFilter.addAction(Global.MSG_UNREAD_YES);
		this.registerReceiver(HandleListChanged, intentToReceiveFilter);
		
//		MobclickAgent.onResume(this);
		Log.e("*** !!! VIDEOCALL *** RESUME RESUME !!! ***");
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
		LedSpeakerUtil.setSpeakerOff();  //tml*** speaker
		mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, saveCallVol, 0);

		mHandler.removeCallbacks(checkCPU);  //tml*** setCPU2
		if (cpuSet[0] != null && cpuSet[2] != null && cpuSet[3] != null) {  //tml|yang*** setCPU
			MyUtil.setCPU(true, cpuSet[0], cpuSet[2], cpuSet[3]);
			MyUtil.getCPU(true);
		}
		
		minimized=false;
		instance=null;
		//mRefreshTimer.cancel();
		mHandler.removeCallbacks(checkVideoEmpty);
		
		NotificationManager mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mNM.cancel(R.string.call);
		updateCallDebugStatus(true, null);

		//tml*** chatview
		if (msgListAdapter != null) {
			msgListAdapter.clear();
		}
		if (mDB != null && mDB.isOpen())
			mDB.close();
		if (mADB!=null && mADB.isOpen()) 
			mADB.close();
		try {  //tml*** unregistered rcvr destroy
			unregisterReceiver(HandleListChanged);
		} catch (IllegalArgumentException e) {
		}
		spannableCache = null;
		
		
		
		System.gc();
		System.gc();
		Log.e("*** !!! VIDEOCALL *** DESTROY DESTROY !!! *** voip");
		super.onDestroy();
	}
	
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
//		MobclickAgent.onPause(this);
		super.onPause();
		if (mVideoViewReady != null)
			((GLSurfaceView)mVideoViewReady).onPause();
		
		Log.e("*** !!! VIDEOCALL *** PAUSE PAUSE !!! ***");
	}

	public void bye() 
	{
		Log.e("*** !!! VIDEOCALL *** BYE BYE !!! ***");
		setResult(RESULT_OK);
		finish();
	}
	
	static VideoCallActivity instance;
	
	static VideoCallActivity getInstance()
	{
		return instance;
	}
	
	long enabletime;
    KeyguardManager mKeyguardManager;
    KeyguardManager.KeyguardLock mKeyguardLock;
    boolean enabled;
    
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
			Toast.makeText(VideoCallActivity.this, getString(R.string.back_again), Toast.LENGTH_SHORT).show();
			mUseBackKey = false;
			return false;
		} else if (keyCode == KeyEvent.KEYCODE_BACK && !mUseBackKey) {
			mUseBackKey = true;
		}
		return super.onKeyDown(keyCode, event);
	}
	//***tml
	//tml*** open orient
	private OrientationEventListener mOrientationHelper;
	private synchronized void startOrientationSensor() {
		if (mOrientationHelper == null) {
			mOrientationHelper = new LocalOrientationEventListener(this);
		}
		mOrientationHelper.enable();
	}
	
	public int mAlwaysChangingPhoneAngle = -1;
	int temptToRotate = 0;
	private class LocalOrientationEventListener extends OrientationEventListener {
		public LocalOrientationEventListener(Context context) {
			super(context);
		}
		
		@Override
		public void onOrientationChanged(final int o) {
			if (o == OrientationEventListener.ORIENTATION_UNKNOWN
					&& mAlwaysChangingPhoneAngle != -1) return;
			
			int degrees = 270;
			if (o == -1) degrees = 270;
			else if (o >= (270 + 45) || o < 45) degrees = 0;
			else if (o < (90 + 45)) degrees = 90;
			else if (o < (180 + 45)) degrees = 180;
			else if (o < (270 + 45)) degrees = 270;
			else degrees = 270;
			
			if (mAlwaysChangingPhoneAngle == degrees) return;
			Log.e("vorient cur/prev deg=" + degrees + "/" + mAlwaysChangingPhoneAngle);
			
			temptToRotate++;
			if (temptToRotate < 2 && mAlwaysChangingPhoneAngle != -1) return;
			temptToRotate = 0;
			mAlwaysChangingPhoneAngle = degrees;
			
			int rotation = (360 - mAlwaysChangingPhoneAngle) % 360;
			
			Log.d("vorient rotation=" + rotation);
			VoipCore vc = AireVenus.getLc();
			if (vc != null) {
				vc.setDeviceRotation(rotation);
				VoipCall currentCall = vc.getCurrentCall();
				if (currentCall != null && currentCall.getCurrentParamsCopy().getVideoEnabled()) {
					vc.updateCall(currentCall, null);
					Log.e("vorient ********* RESTART VIDEOCALL " + rotation);
				}
			}
		}
	}
	//***tml
	
	private int gVideoEmpty=0;
	private Handler mHandler=new Handler();
	private boolean isVideoReady=false;
	private boolean noVideoWarning = false;
	Runnable checkVideoEmpty = new Runnable() {
		public void run()
		{
			VoipCore lc=AireVenus.getLc();
			if (lc!=null && lc.isIncall())
			{
				VoipCall c = lc.getCurrentCall();
				gVideoEmpty=c.isVideoEmpty();
				
//				if (gVideoEmpty>0)
//					((TextView)findViewById(R.id.no_video_hint)).setVisibility(View.VISIBLE);
//				else
//					((TextView)findViewById(R.id.no_video_hint)).setVisibility(View.GONE);
//				mHandler.postDelayed(checkVideoEmpty, 8000);
				//tml|wjx*** for no video hint
//				VideoSize vSize=lc.getVideoRemoteResolution(c);
//				Log.d("wjx:VideoCallActivity::getVideoRemoteResolution: width="+vSize.width+";vSize.height="+vSize.height);
				int remoteVideoSize[] = lc.getVideoRemoteResolution();
				int pWin0 = remoteVideoSize[0];
				int pHin0 = remoteVideoSize[1];
				Log.d("checkVideo=" + gVideoEmpty + ":" + pWin0 + "x" + pHin0 + " warn=" + noVideoWarning);
				
				if (gVideoEmpty > 0) {
//					((LinearLayout)findViewById(R.id.rl_no_video_hint)).setVisibility(View.GONE);
					((ProgressBar)findViewById(R.id.no_video_hint)).setVisibility(View.VISIBLE);
//					setProgBarGIF();
//					((GifView)findViewById(R.id.no_video_hint)).setVisibility(View.VISIBLE);
					//tml*** tcp test
					if (noVideoWarning) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Intent it = new Intent(Global.Action_InternalCMD);
								it.putExtra("Command", Global.CMD_CONNECTION_POOR);
								it.putExtra("ForcePoor", true);
								it.putExtra("warnFrom", "novideo");
								it.putExtra("length01", 0);
								sendBroadcast(it);
							}
						});
					}
					noVideoWarning = !noVideoWarning;
					mHandler.postDelayed(checkVideoEmpty, 3000);
				} else {
					noVideoWarning = false;
//					((LinearLayout)findViewById(R.id.rl_no_video_hint)).setVisibility(View.GONE);
					((ProgressBar)findViewById(R.id.no_video_hint)).setVisibility(View.GONE);
//					((GifView)findViewById(R.id.no_video_hint)).setVisibility(View.GONE);
//					((GifView)findViewById(R.id.no_video_hint)).stop();
					mHandler.postDelayed(checkVideoEmpty, 15000);
				}
				//***tml
			}
		}
	};
	
	//tml*** monitor record
	private MediaRecorder recorder;
	private Camera camera;
	private boolean recording = false;
	private String filenameToRecord;
	private long startRecordingTime=0;
	
	public void onVideoRecording() {
		mHandler.post(new Runnable() {
			public void run() {
				Log.e("vca gooo MonitorRecord");
//				startRecording();
			}
		});
	}
	
	public void startRecording() {
		if (recording) {
			return;
		}
		
//		if (suv!=null)
//		{
//			suv.stop();
//			suv = null;
//		}
		
		initRecorder();
	}

	private void initRecorder() {
		if (recording) {
			destroyRecorder();
		}
		
		init();
		try {
			recorder.setOnErrorListener(new OnErrorListener() {
				@Override
				public void onError(MediaRecorder mr, int what, int extra) {
					Log.e("vca recorder error=" + what);
				}
				
			});
			recorder.setOnInfoListener(new OnInfoListener() {
				@Override
				public void onInfo(MediaRecorder mr, int what, int extra) {
					Log.i("vca recorder info=" + what);
				}
			});
			recorder.prepare();
			Thread.sleep(500);
			recorder.start();
		} catch (Exception e) {
			destroyRecorder();
		} 
		
		recording = true;

		List <Map <String, String>> items = mPref.readMapArray("recordHistory");
		Map <String, String> map = new HashMap <String, String>();

		startRecordingTime = new Date().getTime();

		map.put("0", filenameToRecord);
		map.put("1", "" + startRecordingTime);
		map.put("2", "03:00");
		map.put("3", MyUtil.getDate(1));
		items.add(0, map);

		mPref.writeMap("recordHistory", items);
	}
	
	public void init() {
//		camera = mSurfaceView2.getCamera();
		camera.unlock();
		recorder = new MediaRecorder();
		recorder.setCamera(camera);
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		
		CamcorderProfile cp = CamcorderProfile.get(0, CamcorderProfile.QUALITY_LOW);
		recorder.setProfile(cp);
		Log.d("vca mProfile videoFrameRate::" + cp.videoFrameRate
				+ "videoBitRate::" + cp.videoBitRate
				+ "fileformat::" + cp.fileFormat);
		
		recorder.setVideoSize(640, 480);
		recorder.setMaxDuration(600000);  //10m
		recorder.setMaxFileSize(0);

//		recorder.setPreviewDisplay(mSurfaceView2.getHolder().getSurface());

		filenameToRecord = generateFilename();
		recorder.setOutputFile(filenameToRecord);
	}
	
	private String generateFilename() {
		File[] files = new File(Global.SdcardPath_record).listFiles();
		//tml*** suv save dest
		if (mPref.readBoolean("suvsavedest", false)) {
			//TODO
			String suvsavedest_ext = mPref.read("suvsavedest_ext", getResources().getString(R.string.recording_external));
			File extpath = new File(suvsavedest_ext);
			String extpathName = extpath.getPath();
		} else {
			//TODO
		}
		int max=-1;
		for (File file : files) {
			String name = file.getName();
			String tmp;
			
			if(name.startsWith(".")) {
				continue;
			}
			if (!file.isDirectory() && name.contains("vid_")) {
				int dot = name.lastIndexOf(".");
				if (dot > 3) {
					tmp = name.substring(dot - 3, dot);
					int d = 0;
					
					try {
						d = Integer.parseInt(tmp);
					} catch (Exception e) {
						Log.e("vca record Filename error " + e.getMessage());
					}
					if (d > max) {
						max=d;
					}
				}
			}
		}
		
		max++;

		//tml*** suv save dest
		if (mPref.readBoolean("suvsavedest", false)) {
			//TODO
			String suvsavedest_ext = mPref.read("suvsavedest_ext", getResources().getString(R.string.recording_external));
			File extpath = new File(suvsavedest_ext);
			String extpathName = extpath.getPath();
		} else {
			//TODO
		}
		Log.d(Global.SdcardPath_record + "vidrec_" + String.format("%03d", max) + ".mp4");
		return (Global.SdcardPath_record + "vidrec_" + String.format("%03d", max) + ".mp4");
	}
	
	private void destroyRecorder() {
		Log.d("vca Releasing media recorder");
		if (recorder != null) {
			if (recording) {
				try {
					recorder.setOnErrorListener(null);
					recorder.setOnInfoListener(null);
					recorder.stop();

				} catch (RuntimeException e) {
					Log.e("vca destroyRecorder error " + e.getMessage());
					
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						Log.e("vca sleep for second stop error!! " + e1.getMessage());
					}
					
					try {
						recorder.stop();
					} catch (Exception e2) {
						Log.e("stop fail2 " + e2.getMessage());
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e3) {
							Log.e("vca sleep for reset error Error " + e3.getMessage());
						}
					}
				}
				
				recording = false;
			}
			
			recorder.reset();
			recorder.release();
			recorder = null;
		}
	}
	//***tml
	
	private int nVideoDisplay;
	private int nVideoEncFrames;
	private int displayP2Pcount = 0;
	private int pktssent0 = 0, pktsrecv0 = 0;
	private int pktssentavg = 0, pktsrecvavg = 0;
	private int memoryAvail = 0;
//	private int[] memoryHeap = new int[13];
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
					//tml*** memory
					if (displayP2Pcount % 10 == 0) {
						if (displayP2Pcount == 10) {
							memoryAvail = MyUtil.getMemoryRAM(getApplicationContext(), 3)[0];
//							memoryHeap = MyUtil.getMemoryHEAP(MainActivity._this, mPref.readInt("pid", 0), 13);
						} else {
							memoryAvail = MyUtil.getMemoryRAM(getApplicationContext(), 1)[0];
//							memoryHeap = MyUtil.getMemoryHEAP(MainActivity._this, mPref.readInt("pid", 0), 10);
						}
//						if (memoryHeap == null)
//							for (int m = 0; m < memoryHeap.length; m++)
//								memoryHeap[m] = 0;
					}
					
					int[] ports=lc.getPorts();
					if (displayP2Pcount % 5 == 0) {
						if (displayP2Pcount < 300) {
							pktssentavg = ports[10] / displayP2Pcount;
							pktsrecvavg = ports[11] / displayP2Pcount;
						}
					}
					int fps=ports[22]-nVideoDisplay;
					int encfps=ports[21]-nVideoEncFrames;
					String info="cpu:" + MyUtil.getCPU(false)[1] +
						" (" + MyUtil.getCPU(false)[2] + "-" + MyUtil.getCPU(false)[3] + ")" + 
						"\nbitrate:"+ports[20]+" fps:"+encfps+" ("+pW+"x"+pH+")"+
						"\ndisplay fps:"+fps+
						"\nrelay local:"+ports[0]+" v:"+ports[1]+
						"\nrelay remote:"+ports[2]+" v:"+ports[3]+
						"\nice local:"+ports[4]+" v:"+ports[5]+
						"\nice remote:"+ports[6]+" ("+ports[7]+") v:"+ports[8]+" ("+ports[9]+")"+
						"\npkts sent:"+ports[10]+"+"+(ports[10]-pktssent0)+"/"+pktssentavg+" (mic:"+ports[16]+" ec:"+ports[17]+" amr:"+ports[15]+
						")\npkts recv:"+ports[11]+"+"+(ports[11]-pktsrecv0)+"/"+pktsrecvavg+" (ice:"+ports[12]+")"+
						"\nnAPPSent="+ports[18]+" nAPPRecvd="+ports[13]+
						"\npcktinQ:"+ports[14]+
						"\nnTicker:"+ports[19]+
						"\nmem+" + memoryAvail +
//						"\nheapDNO+" + memoryHeap[0] + "/" + memoryHeap[1] +
//								"  " + memoryHeap[2] + "/" + memoryHeap[3] +
//								"  " + memoryHeap[4] + "/" + memoryHeap[5] +
//								"\n     " + memoryHeap[6] + "/" + memoryHeap[7] + "/" + memoryHeap[8] + 
//								"  " + memoryHeap[9] +
						"\n" + getCallDebugStatus();
					pktssent0 = ports[10];
					pktsrecv0 = ports[11];
					((TextView)findViewById(R.id.debuginfo)).setText(info);
					((FrameLayout)findViewById(R.id.debug)).setVisibility(View.VISIBLE);
					nVideoDisplay=ports[22];
					nVideoEncFrames=ports[21];
				}catch(Exception e){}
				
				mHandler.postDelayed(displayP2P, 1000);
			}
		}
	};
	//tml test
	private void updateCallDebugStatus (boolean reset, String message) {
		if (AireJupiter.getInstance() != null)
			AireJupiter.getInstance().updateCallDebugStatus(reset, "\n" + message);
	}
	//tml test
	private String getCallDebugStatus() {
		String callStatus = "";
		if (AireJupiter.getInstance() != null)
			callStatus = AireJupiter.getInstance().getCallDebugStatus();
		return callStatus;
	}
	/*
	TimerTask refreshOpenGLDisplay = new TimerTask() {
		@Override
		public void run() {
			if (androidVideoWindowImpl!=null)
				androidVideoWindowImpl.requestRender();
		}
	};*/
	
	//tml|yang*** zoom
	private int zoomRatio = 1;
	private int z_x = 0, z_y = 0;
	
	private void createZoomWindow (int x1, int y1, int zratio) {  //x,y are flipped
		VoipCore zVoipCore = AireVenus.instance().getVoipCore();
		if (x1>256) x1=256;
		if (y1>256) y1=256;
		if(zratio==1) zVoipCore.zoomVideo(256, x1,y1);  //256 x1x2x3 zoom
		else if (zratio==2) zVoipCore.zoomVideo(384,x1,y1);  //256 x1x2x3 zoom
		else if (zratio==3) zVoipCore.zoomVideo(512, x1, y1);  //256 x1x2x3 zoom
		else if (zratio==4) zVoipCore.zoomVideo(768, x1, y1);  //256 x1x2x3 zoom
		else zVoipCore.zoomVideo(256, x1,y1);
		
		Log.e("tmlz vivid__test video_call_zoom_req x,y,r=" + x1 + "," + y1 + "," + (zratio * 256));
//		Toast tst = Toast.makeText(getApplicationContext(), "ZOOM SET", Toast.LENGTH_SHORT);
//		tst.setGravity(Gravity.TOP, 0, 0);
//		tst.show();
		//zoomSet = true;
	}
	
	//tml|wjx*** for no video hint
	private Bitmap getUserPhoto(String address)
	{
		if (!MyUtil.checkSDCard(getApplicationContext()))
			return null;
		if (address != null && address.length() == 0)
			return null;
		int idx = mADB.getIdxByAddress(address);
		if (idx > 0) {
//			String path = Global.SdcardPath_inbox + "photo_" + idx + "b.jpg";
//			return ImageUtil.loadBitmapSafe(1, path);
			//tml*** userphoto fix
			String path = Global.SdcardPath_inbox + "photo_" + idx + "b.jpg";
			Bitmap bmp = ImageUtil.loadBitmapSafe(1, path);
			if (bmp != null) return bmp;
			
			path = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
			bmp = ImageUtil.loadBitmapSafe(1, path);
			if (bmp != null) return bmp;
		}
		return null;
	}

	private void setProgBarGIF () {
//		GifView gifview;
//		gifview = (GifView) findViewById(R.id.no_video_hint);
//		int resprogbar = R.drawable.progbar01;
//		Drawable dra = getResources().getDrawable(resprogbar);
//		int wpb = dra.getIntrinsicWidth();
//		int hpb = dra.getIntrinsicHeight();
//		gifview.setGifImageType(GifImageType.SYNC_DECODER);
//		gifview.setImageSize(wpb, hpb);
//		gifview.setGifImage(resprogbar);
	}
	//***tml
	//tml*** chatview

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
			friendPhoto=ImageUtil.getUserPhoto(this, mIdx);
			
			if (friendPhoto == null)
				friendPhoto = getResources().getDrawable(R.drawable.bighead);
	
			String path = mPref.read("myPhotoPath", null);
			if (path != null && path.length() > 0)
				myphoto = ImageUtil.getBitmapAsRoundCorner(path, 2, 0);
			
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

	final int action_nothing=0;
	final int action_resend=1;
	final int action_save_as=2;
	int item2_action;

	boolean handleLongPress(int position) {
//		if (mp2 != null && mp2.isPlaying())
//			return false;
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
													
													/*
													if (VideoCallActivity.fileUploading)
													{
														for (int i=0;i<TalkList.size();i++)
														{
															final SMS msg = TalkList.get(i);
															if (msg_smsid == msg.smsid)
															{
																new MyNet(VideoCallActivity.this).stopUploading(msg.att_path_aud);
																VideoCallActivity.fileUploading=false;
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
//										VideoCallActivity.this);
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
									Toast.makeText(VideoCallActivity.this,
											getString(R.string.file_err),
											Toast.LENGTH_SHORT).show();
								}
								//***tml
							} else if (item2_action==action_resend) 
							{
								/*
								if (VideoCallActivity.fileUploading) {
									Toast.makeText(getApplicationContext(),
											getString(R.string.fileuploading),
											Toast.LENGTH_SHORT).show();
									return;
								}*/
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
		return true;
	}

	private OnLongClickListener mLongPressBalloonView = new OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			int position = Integer.parseInt(v.getTag().toString());
			return handleLongPress(position);
		}
	};
	
	private void copyToClipboard(String text) {  //tml*** copy clipboard
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
	
	private String getMIMEdest(String fName) {  //tml*** browser save
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
//				holder.progress = (ProgressBar) convertView.findViewById(R.id.progressbar);
				holder.audmsg = (AudioMsgPlayer) convertView.findViewById(R.id.audio_msg);
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
			lpPhoto = new RelativeLayout.LayoutParams((int)(75.*mDensity), (int)(75.*mDensity));
			
			if (msg.type == 1) {
				lpPhoto.setMargins((int)(10.*mDensity), 0, 0, 0);
				lpPhoto.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				if (inGroup)//alec
				{
					Drawable f=friendsPhotoMap.get(Integer.valueOf(msg.group_member));
					holder.photoimage.setImageDrawable(f);
				}
				else
					holder.photoimage.setImageDrawable(friendPhoto);
			} else {
				lpPhoto.setMargins(0, 0, (int)(10.*mDensity), 0);
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
			
//			RelativeLayout.LayoutParams lpProgress = new RelativeLayout.LayoutParams((int)(430.*mDensity), (int)(15.*mDensity));
//			lpProgress.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//			lpProgress.addRule(RelativeLayout.BELOW, holder.balloon.getId());
//			lpProgress.addRule(RelativeLayout.LEFT_OF, holder.photoimage.getId());
//			lpProgress.rightMargin=(int)(20*mDensity);
//			holder.progress.setLayoutParams(lpProgress);
			
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
									ImageSpan icon = new ImageSpan(VideoCallActivity.this, picturebitmap, ImageSpan.ALIGN_BOTTOM);
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
										Bitmap bubbleblue = BitmapFactory.decodeResource(VideoCallActivity.this.getResources(),
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
//				holder.progress.setVisibility(View.GONE);
			}else{
				if (msg.status==SMS.STATUS_PENING)
				{
					holder.balloon.setBackgroundResource(R.drawable.balloon_right_pending);
					if (msg.attached==8 || msg.attached==9)
					{
//						holder.progress.setProgress((float)msg.progress/100.f);
//						holder.progress.setVisibility(View.VISIBLE);
//						mProgress.put(msg.att_path_aud, holder.progress);
					}
				}else
				{
					holder.balloon.setBackgroundResource(R.drawable.balloon_right);
//					holder.progress.setVisibility(View.GONE);
				}
			}
			
			RelativeLayout.LayoutParams lpBubble = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			if (msg.type == 1) {
				lpBubble.setMargins((int)(110.*mDensity), 0, 0, 0);
				lpBubble.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				holder.gifview.setPadding(10, 0, 10, 0);
//				holder.photoimage.setOnClickListener(mOnClickPhoto);
			} else {
				lpBubble.setMargins(0, 0, (int)(110.*mDensity), 10);
				lpBubble.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				holder.gifview.setPadding(10, 0, 10, 0);
				holder.photoimage.setOnClickListener(null);
			}
			lpBubble.addRule(RelativeLayout.BELOW, holder.tTime.getId());
			
			holder.balloon.setLayoutParams(lpBubble);
			holder.balloon.setTag(TalkList.size() - position - 1);
			
			holder.balloon.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					int position = Integer.parseInt(v.getTag().toString());
					SMS msg = TalkList.get(position);
					if (msg.attached == 2) {
						File file = new File(msg.att_path_img);
						if (!file.exists()) {
							return;
						}
					} else {
						return;
					}
					
					if (msg.obligate1!=null && msg.obligate1.startsWith("http"))
					{
						/*
						try{
							String title=msg.content.substring(6);
							Intent i = new Intent(VideoCallActivity.this,WebViewActivity.class);
							i.putExtra("URL", msg.obligate1);
							i.putExtra("Title", title);
							startActivity(i);
						}catch(Exception e){}*/
					}
					else
					{
						Intent i = new Intent(VideoCallActivity.this,MessageDetailActivity.class);
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
//		ProgressBar progress;
		AudioMsgPlayer audmsg;
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
//					}
//					onPlayVoiceMemo(intent.getStringExtra("autoPath"));
//				}
//				if (mPrf.readBoolean("recvVibrator", true)) {
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
//					//TODO
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
//				}
//				return;
			} else if (intent.getAction().equals(Global.Action_SMS_Fail)) {
				Toast.makeText(VideoCallActivity.this,
						getString(R.string.smsfail), Toast.LENGTH_SHORT).show();
				return;
			} else if (intent.getAction().equals(Global.MSG_UNREAD_YES)) {  //tml*** chatview
				if (!(mPref.readBoolean("autoAnswer:" + mAddress, false)
						&& mPref.readBoolean("way") == false)) {
					if (((LinearLayout) findViewById(R.id.chatframe)).getVisibility() != View.VISIBLE) {
						mHandler.post(new Runnable () {
							@Override
							public void run() {
								((ImageView) findViewById(R.id.chatview)).performClick();
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

	private void onSend()
	{
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
//			/*if (ConversationActivity.fileUploading) {
//				Toast.makeText(getApplicationContext(),
//						getString(R.string.fileuploading),
//						Toast.LENGTH_SHORT).show();
//				mSend.setEnabled(true);
//				return;
//			}*/
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
    	if (UserPage.sortMethod==1)
    		UserPage.needRefresh=true;

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
//					mSend.setEnabled(true);
//				}
//				else{
//					addMsgtoTalklist(true);
//					playSoundTouch();
//				}
//			}
//			else{
//				if (!fileAgent.onSend(mAddress, mMsgText, mAttached, SrcAudioPath, SrcImagePath, false)) {
//					mSend.setEnabled(true);
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
//			
//			/*
//			if (msg.content.startsWith("(fl)")) {
//				msg.content = getString(R.string.filememo_send) + " " + msg.content;
//			} else {
//				msg.content = getString(R.string.video) + " " + msg.content;
//				msg.attached = 9;
//			}*/
//			
//			if (msg.content.startsWith("(fl)")) {
//				String filename=SrcAudioPath.substring(SrcAudioPath.lastIndexOf('/')+1);
//				String part2=msg.content.substring(4);
//				msg.content = "(fl)  " + filename + "  " + part2;
//			} else {
//				msg.content = getString(R.string.video) + " " + msg.content;
//				msg.attached = 9;
//			}
//		}

		msg.longitudeE6 = mPref.readLong("longitude", Global.DEFAULT_LON);
		msg.latitudeE6 = mPref.readLong("latitude", Global.DEFAULT_LAT);
		
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

		mHandler.postDelayed(new Runnable() {
			public void run() {
				((Button) findViewById(R.id.sendmsg)).setEnabled(true);
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

	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(((EditText) findViewById(R.id.msginput)).getWindowToken(), 0);
	}
	//***tml
	//tml*** setCPU2
	Runnable checkCPU = new Runnable () {
		@Override
		public void run() {
			MyUtil.setCPU(false, null, null, null);
			MyUtil.getCPU(true);
			mHandler.postDelayed(checkCPU, 20000);
		}
	};
}
