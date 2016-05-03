package com.pingshow.voip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.xwalk.core.XWalkActivity;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.pingshow.amper.AddCallDialog;
import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.AmazonKindle;
import com.pingshow.amper.ConversationActivity;
import com.pingshow.amper.DQRates;
import com.pingshow.amper.Global;
import com.pingshow.amper.Log;
import com.pingshow.amper.MakeCall;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.PickupActivity;
import com.pingshow.amper.PlayService;
import com.pingshow.amper.R;
import com.pingshow.amper.SMS;
import com.pingshow.amper.SendAgent;
import com.pingshow.amper.SettingActivity;
import com.pingshow.amper.SipCallActivity;
import com.pingshow.amper.UsersActivity;
import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AireCallLogDB;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.SmsDB;
import com.pingshow.amper.view.FlipToggleView;
import com.pingshow.amper.view.HistogramView;
import com.pingshow.amper.view.PhotoGallery;
import com.pingshow.codec.VoiceRecord2_MR;
import com.pingshow.network.MyNet;
import com.pingshow.network.MySocket;
import com.pingshow.network.NetInfo;
import com.pingshow.util.CPUTool;
import com.pingshow.util.CallPhp;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MCrypt;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.core.Version;
import com.pingshow.voip.core.VoipAddress;
import com.pingshow.voip.core.VoipCall;
import com.pingshow.voip.core.VoipCall.State;
import com.pingshow.voip.core.VoipCore;
import com.pingshow.voip.core.VoipCoreException;
import com.pingshow.voip.core.VoipCoreListener;
import com.pingshow.voip.core.VoipProxyConfig;
import android.webkit.ValueCallback;
import android.net.http.SslError;

public class DialerActivity extends Activity implements VoipCoreListener,
		SensorEventListener {

	private final int RINGLIMITX = 50000;

	private Toast mToast;
	private String mAddress = "";
	private TextView mDisplayNameView;
	private ImageView mProfileImage;
	private boolean isMuted;
	private TextView mStatus;
	private Button mHangup, mHangup2;
	private ToggleButton mSpeaker;
	private ToggleButton mMute;
	private ToggleButton mHold;
	private ToggleButton mChatView;
	private Button mSwitchConf;
	private LinearLayout mAnswerSlide;
	private Button mHideKeypad;
	private boolean videoCall;
	private boolean launchingVideo = false;
	private boolean streamsRunning = false;
	private boolean shouldConsumeCredit = false;
	private boolean incomingChatroom = false;
	public static long PSTNCallLogRowId = -1;
	public static float previousCredit;
	private boolean shouldCheckPSTNinChatroom = false;
	public static boolean isMobileNumber = true;
	public static int cIndex;
	public static String cIso = "us";
	private XWalkView xWalkWebView; // tml*** vidconf

	private boolean created = false;
	private int confLogDuration = 0;

	private static DialerActivity theDialer;

	private String mDisplayName = null;
	private AudioManager mAudioManager;
	private PowerManager.WakeLock mWakeLock;
	private MyPreference mPref;
	private long contact_id;
	private int PreviousVolume;

	private AmpUserDB mADB;
	private String sysIncomingNumber = null;
	private boolean way = false;
	private boolean BluetoothSco = false;

	public boolean imCalling = false;

	private Sensor proximitySensor;

	private boolean bSamsung;
	private boolean HTCA6390;
	private boolean bMotorola;
	private boolean htc4g;
	private boolean zxZTE;
	private boolean htc_evo;
	private boolean htc_oneS;
	private boolean bSamsungi997;
	private boolean bPD1000;
	private boolean NexusS;
	private boolean bSamsungi9100;
	private boolean HTCG11;
	private boolean HTCG12;
	private boolean HTCSensation;
	private boolean bGalaxyNexus;
	private boolean HTC_6400;
	private boolean Moto860;
	private boolean Google40;
	// private boolean Google42;
	private boolean zteU950;
	private boolean SonyRK30sdk;
	private boolean bSamsungS3;
	private boolean filemate1;
	private boolean isKindle;
	private boolean dontUseInCallMode = false;

	final int VIDEO_VIEW_ACTIVITY = 100;

	private Vibrator mVibrator;
	private AireCallLogDB mCLDB;

	private SensorManager mSensorManager;

	private RelativeLayout mMainFrame;
	private static boolean Connected;
	public static boolean incomingCall;
	// public static volatile boolean amInCall = false;
	// public static volatile boolean amInCall2 = false;
	private long startTime;

	private String phoneNumber;
	private MyPhoneStateListener phoneListener = null;
	private TelephonyManager tMgr = null;
	private ContactsQuery cq;

	private boolean sendTerminateSignal = true;
	private boolean bCommercial = false;
	private String DTMFString = "";
	private float consumedCredit;
	public static boolean rejectHangingup = false;
	private int SelectedClass;

	private boolean recordingVoiceMail;
	// private VoiceMemo_NB vm;
	private VoiceRecord2_MR myVR;

	private ToneGenerator tg = null;

	public boolean HangingUp = false;

	static public boolean uiDAinFore = false;

	private Handler handler = new Handler() {
		@SuppressWarnings("unchecked")
		public void handleMessage(android.os.Message msg) {
			try {
				switch (msg.what) {
					case 1:
						TextView textview = (TextView) ((HashMap<String, Object>) msg.obj)
								.get("textview");
						String text = (String) ((HashMap<String, Object>) msg.obj)
								.get("text");
						textview.setText(text);
						break;
					case 2:
						ImageView imageview = (ImageView) ((HashMap<String, Object>) msg.obj)
								.get("imageview");
						Bitmap bm = (Bitmap) ((HashMap<String, Object>) msg.obj)
								.get("image");
						imageview.setImageBitmap(bm);
						break;
					case 3:
						Button btn = (Button) msg.obj;
						btn.setVisibility(msg.arg1);
						break;
					case 4:
						LinearLayout linear = (LinearLayout) msg.obj;
						linear.setVisibility(msg.arg1);
						linear.bringToFront();
						break;
				}
			} catch (Exception e) {
				Log.e("da handler !@#$ " + e.getMessage());
			}
		};
	};

	public static DialerActivity getDialer() {
		return theDialer;
	}

	protected static DialerActivity instance() {
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

		Log.e("*** !!! DIALER *** START START !!! *** voip");
		Log.e("getNumCores() ====" + CPUTool.getNumCores());
		Log.e("getCpuName() ====" + CPUTool.getCpuName());
		Log.e("getMaxCpuFreq() ====" + CPUTool.getMaxCpuFreq());
		Log.e("getMinCpuFreq() ====" + CPUTool.getMinCpuFreq());
		Log.e("getCurCpuFreq() ====" + CPUTool.getCurCpuFreq());
		mPref = new MyPreference(DialerActivity.this);
		mPref.write("LastPage", 0);
		ringrdy = false;
		emptyCall = 0;
		prevRecvpkts = 0;

		lockScreenOrientation(0); // tml*** dialer rot, remove->works
		theDialer = this;

		if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) { // tml|yang***
			// vidconf
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				getWindow().setFlags(
						WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
						WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
				XWalkPreferences.setValue(
						XWalkPreferences.ANIMATABLE_XWALK_VIEW, false);
				xWalkWebView = new XWalkView(theDialer, DialerActivity.this);
				//bree:设置XWalkView背景为黑色，并去除ssh警告对话框
				xWalkWebView.setBackgroundColor(Color.BLACK);
				xWalkWebView.setResourceClient(new XWalkResourceClient(xWalkWebView){
					@Override
					public void onReceivedSslError(XWalkView view,
												   ValueCallback<Boolean> callback, SslError error) {
						callback.onReceiveValue(true);
					}
				});
				xWalkWebView.clearCache(false);
				Log.i("vidConfxwalk xWalkWebView ready "
						+ xWalkWebView.getXWalkVersion() + ","
						+ xWalkWebView.getAPIVersion());
			}
		}

		// runOnUiThread(new Runnable () {
		// @Override
		// public void run() {
		// if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM
		// || AireVenus.getCallType() == AireVenus.CALLTYPE_FAFA) {
		// //tml|yang*** vidconf
		// if (android.os.Build.VERSION.SDK_INT >=
		// android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
		// getWindow().setFlags(
		// WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
		// WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
		// XWalkPreferences.setValue(XWalkPreferences.ANIMATABLE_XWALK_VIEW,
		// false);
		// xWalkWebView = new XWalkView(theDialer, DialerActivity.this);
		// xWalkWebView.clearCache(false);
		// Log.i("vidConfxwalk xWalkWebView ready " +
		// xWalkWebView.getXWalkVersion() + "," + xWalkWebView.getAPIVersion());
		// }
		// }
		// }
		//
		// });

		mHandler.postDelayed(startDialerStuff, 500);

	}

	boolean checkHWEC(String product, String model) {
		final String[] hw = {
				// Samsung Galaxy S3
				"sgh-i9300", "i9305", "shv-e210", "sgh-t999", "shg-i747",
				"sgh-n064", "sgh-n035", "sch-j021", "sch-r530", "sch-i535",
				"sch-s960",
				"i9308",
				"i939",
				// Galaxy S4
				"shv-e300", "shv-e330", "i9505", "i9506", "sgh-i337",
				"sgh-m919", "sch-i545", "sph-l720",
				"sch-r970",
				// Galaxy Note2
				"gt-n7100", "gt-n7102", "gt-n7105", "gt-n7108", "sch-i605",
				"sch-r950", "sgh-i317", "sgh-t889", "sph-l900", "sch-n719",
				"sgh-n025", "shv-e250",
				// Galaxy Note3
				"n9006", "n9005", "sm-n900" };

		final String[] exclude_product = { "ja3gchnduoszn", "ja3gzs" };

		for (String h : exclude_product) {
			if (product.contains(h))
				return false;
		}

		for (String h : hw) {
			if (model.contains(h))
				return true;
		}
		return false;
	}
	private String myUuid;
	Runnable startDialerStuff = new Runnable() {
		@SuppressWarnings("deprecation")
		public void run() {
			setContentView(R.layout.dialer);
			Connected = false;
			launchingVideo = false;

			tg = new ToneGenerator(-1, 75);

			// register call status listener
			phoneListener = new MyPhoneStateListener();
			tMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			tMgr.listen(phoneListener, MyPhoneStateListener.LISTEN_CALL_STATE);

			cq = new ContactsQuery(DialerActivity.this);
			mADB = new AmpUserDB(DialerActivity.this);
			mADB.open();

			mCLDB = new AireCallLogDB(DialerActivity.this);
			mCLDB.open();

			mAddress = "";
			mDisplayNameView = (TextView) findViewById(R.id.displayname);

			String Brand = Build.BRAND.toLowerCase();
			String Product = Build.PRODUCT.toLowerCase();
			String Model = Build.MODEL.toLowerCase();
			String scrnLay = ((RelativeLayout) findViewById(R.id.talking_frame))
					.getTag().toString();

			Log.i(scrnLay + " BRAND:" + Brand + " PRODUCT:" + Product
					+ " MODEL:" + Model);

			// alec compatibility
			bSamsung = Brand.contains("samsung") || Model.contains("sph-")
					|| Model.contains("sgh-") || Model.contains("gt-");
			bGalaxyNexus = Model.contains("galaxy nexus")
					|| Model.contains("mb8") || Model.contains("droidx");
			zteU950 = Model.contains("u950");
			Google40 = Version.sdkAboveOrEqual(14);
			// Google42=Version.sdkAboveOrEqual(17);
			if (bSamsung) {
				bSamsungi997 = Model.contains("i997");
				// bSamsungi9003=Model.contains("i9003");
				bPD1000 = Model.contains("gt-p1000");
				bSamsungi9100 = Model.contains("i9100")
						|| Product.contains("i9100") || Model.contains("i9108");
				bSamsungS3 = Model.contains("i747") || Product.contains("d2uc")
						|| Model.contains("i9300") || Model.contains("i9305")
						|| Model.contains("i535");
			}

			bMotorola = Model.contains("xt502") || Model.contains("firetrap");
			htc4g = Product.contains("htc_supersonic");
			HTCG11 = Model.contains("incredible s")
					|| Product.contains("htc_vivo");
			HTCG12 = Model.contains("desire s") || Product.contains("htc_saga");
			HTCSensation = Product.contains("htc_pyramid")
					|| Model.contains("sensation");
			NexusS = Model.contains("nexus s");
			HTC_6400 = Model.contains("adr6400") || Model.contains("adr6350")
					|| Model.contains("desire hd");
			Moto860 = Model.contains("mb860");
			htc_evo = Model.startsWith("evo");
			htc_oneS = Product.contains("ville") || Model.contains("vle_u");
			SonyRK30sdk = Product.contains("rk30sdk")
					|| Model.contains("rk30sdk");

			HTCA6390 = Product.contains("huangshan")
					|| Brand.contains("htccn_chs_cmcc")
					|| Model.contains("a6390");
			isKindle = Brand.contains("amazon");
			filemate1 = Brand.contains("filemate");

			// ZTE/P729CU_UNICOM/ZTE-U V880
			zxZTE = Brand.contains("zte") || Model.contains("v880");

			dontUseInCallMode = (htc_evo || HTCSensation || SonyRK30sdk || isKindle);

			try {
				FileOutputStream file = new FileOutputStream(new File(
						"/mnt/sdcard/.com.amper/.proinfo"));
				String out = Brand + "\n" + Product + "\n" + Model + "\n";

				// alec: always 0;
				out += "0\n";

				if (Brand.contains("xiaomi"))
					out += "32\n256\n64\n256\n30000\n";
				else if (Model.contains("gt-i95"))
					out += "64\n512\n64\n512\n30000\n";
				else
					out += "128\n600\n128\n600\n30000\n";

				if (mPref.readBoolean("enable_double_audio", false))
					out += "1\n";
				else
					out += "0\n";

				if (mPref.readBoolean("enable_jitter_buffer", false))
					out += "1\n";
				else
					out += "0\n";

				if (mPref.readBoolean("enable_jitter_compensation", false))
					out += "1\n";
				else
					out += "0\n";

				if (mPref.readBoolean("enable_antijitter", true))
					out += "1\n";
				else
					out += "0\n";

				if (mPref.readBoolean("flush_audio", true))
					out += "1\n";
				else
					out += "0\n";

				if (mPref.readBoolean("enable_ec", true))
					out += "1\n";
				else
					out += "0\n";

				if (mPref.readBoolean("enable_dump_raw", false))
					out += "1\n";
				else
					out += "0\n";

				out += "256\n"; // speaker gain, default is 256
				out += "0\n"; // 1: xiaomi Mi2 phone, 0: default
				out += "0\n"; // 1: surval mode, only one way video, 0: two ways
				// video

				// sw*** cpu freq
				int freqx = getMaxCPUFreqMHz();
				if (freqx <= 0) { // if file !exist, may return -1
					freqx = 1500;
				}
				Log.d("SWW : CPU freq " + freqx);
				out += Integer.toString(freqx) + "\n";

				file.write(out.getBytes());
				file.flush();
				file.close();
			} catch (Exception e) {
				Log.e("startDialerStuff1 !@#$ " + e.getMessage());
			}

			mAudioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
			Bluetooth.init();
			if (Bluetooth.isAvailable() && !bPD1000) {
				if (Bluetooth.isSupported()) {
					mAudioManager.setMode(AudioManager.MODE_IN_CALL);
					mAudioManager.setMode(AudioManager.MODE_NORMAL);
					mAudioManager.setSpeakerphoneOn(true);
					mAudioManager.setBluetoothScoOn(true);
					mAudioManager.setMicrophoneMute(false);
					Bluetooth.enable(true);
					BluetoothSco = true;
				}
			}

			PreviousVolume = mAudioManager
					.getStreamVolume(AudioManager.STREAM_VOICE_CALL);

			contact_id = getIntent().getLongExtra("Contact_id", 0);
			videoCall = getIntent().getBooleanExtra("VideoCall", false);
			bCommercial = (AireVenus.getCallType() == AireVenus.CALLTYPE_WEBCALL);

			mProfileImage = (ImageView) findViewById(R.id.bighead);
			// mSurface=(SurfaceView)findViewById(R.id.video_capture_surface);
			// FrameLayout f=(FrameLayout)findViewById(R.id.selfview);
			// if (videoCall && Version.sdkAboveOrEqual(9))
			// {
			// mSurface.setVisibility(View.VISIBLE);
			// f.setVisibility(View.VISIBLE);
			// }else{
			// mSurface.setVisibility(View.GONE);
			// f.setVisibility(View.GONE);
			// }

			consumedCredit = 0;

			mHangup = (Button) findViewById(R.id.hangup);
			mHangup.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Log.e("voip.HANGUP1 DA *** USER PRESSED ***");
					//bree：用户挂断的时候  kick
					if (mPref.readInt("BCAST_CONF", -1) >= 0 && mPref.readBoolean("pay", false)) {
						CallPhp.callHangupPhp(myUuid, DialerActivity.this);
						CallPhp.callHangupPhp2(myUuid, DialerActivity.this);
					}

					mPref.write("tempCheckSameIN", 0); // tml*** sametime

					if (rejectHangingup
							&& AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL) {
						// if (rejectHangingup && AireVenus.getCallType() ==
						// AireVenus.CALLTYPE_AIRECALL) { //tml*** switch conf
						Log.e("voip.HANGUP1 DA *** USER PRESSED *** cancelled "
								+ AireVenus.getCallTypeName(AireVenus
								.getCallType()) + rejectHangingup);
						return;
					}

					// if (AireJupiter.getInstance() != null)
					// AireJupiter.getInstance().setSwitchCall(false,
					// "mHangup"); //tml*** switch conf

					if (recordingVoiceMail)
						oStopMemoRecording();

					Intent VoipIntent = new Intent(Intent.ACTION_MAIN);
					VoipIntent.setClass(DialerActivity.this, PlayService.class);
					stopService(VoipIntent);

					mStatus.setText(R.string.call_end);
					mHangup.setOnClickListener(null);
					HangingUp = true;
					sendTerminateSignal = true;
					try {
						mHandler.removeCallbacks(timeElapsed);
						mHandler.removeCallbacks(run_disp_msg);
						mHandler.removeCallbacks(run_mute);

						if (AireVenus.instance() != null) {
							VoipCore lVoipCore = AireVenus.instance()
									.getVoipCore();
							VoipCall myCall = lVoipCore.getCurrentCall();
							if (myCall != null) {
								lVoipCore.terminateCall(myCall);
								Log.e("voip.HANGUP1 DA *** USER PRESSED *** OK");
								return;
							} else {
								Log.e("voip.HANGUP1 DA *** USER PRESSED *** myCall=null");
							}
						}
					} catch (Exception e) {
						Log.e("mHangup !@#$ " + e.getMessage());
					}
					exitCallMode("mHangup");
				}
			});
			// tml*** beta ui
			mHangup2 = (Button) findViewById(R.id.hangup2);
			mHangup2.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Log.e("voip.USER *** DA PRESSED HANGUP2 ***");
					// if (AireJupiter.getInstance() != null)
					// AireJupiter.getInstance().setSwitchCall(false,
					// "mHangup2"); //tml*** switch conf
					onBackPressed();
				}
			});
			// ***tml
			mSpeaker = (ToggleButton) findViewById(R.id.speaker);
			mSpeaker.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView,
											 boolean isChecked) {
					if (isChecked)
						routeAudioToSpeaker();
					else
						routeAudioToReceiver();
					// Log.e("DIAL-CALL SPKR " + isChecked);
				}
			});

			((FrameLayout) findViewById(R.id.keypad_panel))
					.setVisibility(View.INVISIBLE);
			((ToggleButton) findViewById(R.id.keypad))
					.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView,
													 boolean isChecked) {
							flipKeypad(isChecked);
						}
					});

			mHideKeypad = (Button) findViewById(R.id.hide_keypad);
			mHideKeypad.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					flipKeypad(false);
					((ToggleButton) findViewById(R.id.keypad))
							.setChecked(false);
				}
			});

			if (BluetoothSco) {
				Drawable bt = getResources().getDrawable(R.drawable.bluetooth);
				bt.setBounds(0, 0, bt.getIntrinsicWidth(),
						bt.getIntrinsicHeight());
				mSpeaker.setCompoundDrawables(null, bt, null, null);
				mSpeaker.setTextOn(getResources().getString(R.string.bluetooth));
				mSpeaker.setTextOff(getResources()
						.getString(R.string.bluetooth));
			}

			mMute = (ToggleButton) findViewById(R.id.mute);
			if (mPref.readInt("BCAST_CONF", -1) == 0) {
				mMute.setClickable(false);
			}
			mMute.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView,
											 boolean isChecked) {
					if (AireVenus.isready()) {
						VoipCore p = AireVenus.instance().getVoipCore();
						if (isChecked) {
							p.muteMic(true);
						} else {
							if (!mHold.isChecked())// alec
								p.muteMic(false);
						}
					}
					// Log.e("DIAL-CALL MUTE " + isChecked);
				}
			});

			// alec implemented the phone hold: mic and speaker are muted
			mHold = (ToggleButton) findViewById(R.id.hold);
			mHold.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView,
											 boolean isChecked) {
					if (AireVenus.isready()) {
						VoipCore p = AireVenus.instance().getVoipCore();
						if (isChecked) {
							p.muteMic(true);
							p.setMuteSpeaker(1);
						} else {
							if (!mMute.isChecked()) // alec
								p.muteMic(false);
							p.setMuteSpeaker(0);
						}
					}
				}
			});
			// tml*** chatview
			mPref.write("chatCallMute", false);
			mPref.write("chatCallSpkr", false);
			// tml*** switch conf, 0a
			// mSwitchConf = (Button) findViewById(R.id.switchconf);
			// mSwitchConf.setVisibility(View.GONE); //still testing, also
			// View.VISIBLE in callstate
			// mSwitchConf.setOnClickListener(new OnClickListener() {
			// @Override
			// public void onClick(View v) {
			// Log.e("--------- SWITCH CALL --------- begin");
			// if (AireJupiter.getInstance() != null)
			// AireJupiter.getInstance().setSwitchCall(true, "GO");
			// incomingChatroom = false;
			// mPref.write("incomingChatroom", incomingChatroom);
			//
			// new Thread(sendNotifyForJoinChatroom).start();
			// }
			// });

			mChatView = (ToggleButton) findViewById(R.id.chatview);
			mChatView.setVisibility(View.VISIBLE);
			mChatView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String address = mAddress;
					String nickname = mADB.getNicknameByAddress(mAddress);
					Long contactId = cq.getContactIdByNumber(mAddress);
					Log.i("callchat addr/nick/contact == " + address + "/"
							+ nickname + "/" + contactId);
					Intent it = new Intent(DialerActivity.this,
							ConversationActivity.class);
					it.putExtra("SendeeContactId", contactId);
					it.putExtra("SendeeNumber", mAddress);
					it.putExtra("SendeeDisplayname", nickname);
					it.putExtra("CallMode", true);
					it.putExtra("FromCallMode", 2);

					((ToggleButton) v).setChecked(false);
					if (mMute.isChecked()) {
						mPref.write("chatCallMute", true);
					} else {
						mPref.write("chatCallMute", false);
					}
					if (mSpeaker.isChecked()) {
						mPref.write("chatCallSpkr", true);
					} else {
						mPref.write("chatCallSpkr", true);
						mSpeaker.performClick();
					}

					startActivity(it);
				}
			});
			// if (!Log.enDEBUG) mChatView.setVisibility(View.GONE); //test only
			if ((AireVenus.instance() != null && (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM || AireVenus
					.getCallType() == AireVenus.CALLTYPE_AIRECALL))
					|| videoCall) {
				mChatView.setVisibility(View.GONE);
				// mSwitchConf.setVisibility(View.GONE); //tml*** switch conf
			}
			// ***tml

			// alec: test1
			/*
			 * if (htc4g) { // simon 061811 routeAudioToSpeaker(); try {
			 * Thread.sleep(500); } catch (InterruptedException e) {} } else {
			 * if (mSpeaker.isChecked()) { routeAudioToSpeaker(); } else {
			 * routeAudioToReceiver(); } }
			 */

			handlerHold = new Handler() {
				public void handleMessage(Message msg) {
					mHold.setChecked(true);
				};
			};

			mAnswerSlide = (LinearLayout) findViewById(R.id.answerslide);
			SlideControl s = new SlideControl(DialerActivity.this);
			mAnswerSlide.addView(s);

			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			if (mWakeLock == null) {
				mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
						| PowerManager.ACQUIRE_CAUSES_WAKEUP, "FafaYou.InCall");
			}

			mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

			mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			proximitySensor = mSensorManager
					.getDefaultSensor(Sensor.TYPE_PROXIMITY);
			mMainFrame = (RelativeLayout) findViewById(R.id.talking_frame);

			mStatus = (TextView) findViewById(R.id.status_label);

			Intent intent = getIntent();
			// phoneNumber=intent.getStringExtra("PhoneNumber");
			// if (phoneNumber!=null)
			// {
			// phoneNumber=MyTelephony.attachPrefix(DialerActivity.this,
			// phoneNumber);
			// mAddress=phoneNumber;
			// }
			String _phoneNumber = intent.getStringExtra("PhoneNumber");
			if (_phoneNumber != null) {
				phoneNumber = MyTelephony.attachPrefix(DialerActivity.this,
						_phoneNumber);
				mAddress = phoneNumber;
			}
			Log.d("startDialer to-id " + _phoneNumber + ">" + phoneNumber);

			if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {
				boolean bIncome = mPref.readBoolean("incomingChatroom");
				if (bIncome) {
					incomingChatroom = true;
					mPref.delect("incomingChatroom");

					// startRinging();
					prepareRing(true, 1, AireVenus.getCallType(),
							"DA CALLTYPE_CHATROOM"); // tml*** new ring
				}

				mHandler.postDelayed(refreshChatRoomMember, 15000);
			}

			Bitmap photo = getUserPhoto(mAddress);

			if (photo != null) {
				mProfileImage.setImageBitmap(photo);
			}
			if (bCommercial)
				mProfileImage.setBackgroundColor(0x00000000);

			incomingCall = getIntent().getBooleanExtra("incomingCall", false);
			// tml*** incall rot
			// if (incomingCall) {
			// amInCall = getIntent().getBooleanExtra("willBeInCall", false);
			// }
			// amInCall2 = amInCall;
			// ***tml

			mHandler.post(connectStatusShow); // tml*** beta ui
			if (incomingCall || incomingChatroom) {
				if (mPref.readInt("BCAST_CONF", -1) >= 0 && mPref.readBoolean("pay", false)) {
					mHangup.setVisibility(View.VISIBLE);
				}else
					mHangup.setVisibility(View.GONE);
				mHangup2.setVisibility(View.VISIBLE); // tml*** beta ui
				mHold.setVisibility(View.GONE);
				mAnswerSlide.setVisibility(View.VISIBLE);
				displayStatus(null, getString(R.string.incoming_call));
				// tml*** incall rot
				// if (amInCall) {
				// // mHangup.setVisibility(View.VISIBLE);
				// // mAnswerSlide.setVisibility(View.GONE);
				// updateButtonVisible(mHangup, View.VISIBLE);
				// updateLinearLayoutVisible(mAnswerSlide, View.GONE);
				// Connected = true;
				// } else {
				// // mHangup.setVisibility(View.GONE);
				// // mAnswerSlide.setVisibility(View.VISIBLE);
				// updateButtonVisible(mHangup, View.GONE);
				// updateLinearLayoutVisible(mAnswerSlide, View.VISIBLE);
				// displayStatus(null,getString(R.string.incoming_call));
				// }
				// ***tml
			} else {
				mHangup.setVisibility(View.VISIBLE);
				mHangup2.setVisibility(View.GONE); // tml*** beta ui
				mHold.setVisibility(View.VISIBLE);
				mAnswerSlide.setVisibility(View.GONE);
				displayStatus(null,
						videoCall ? getString(R.string.making_video_call)
								: getString(R.string.making_call));
				// tml*** incall rot
				// if (amInCall2) {
				// // mHangup.setVisibility(View.VISIBLE);
				// // mAnswerSlide.setVisibility(View.GONE);
				// updateButtonVisible(mHangup, View.VISIBLE);
				// updateLinearLayoutVisible(mAnswerSlide, View.GONE);
				// } else {
				// // mHangup.setVisibility(View.GONE);
				// // mAnswerSlide.setVisibility(View.VISIBLE);
				// updateButtonVisible(mHangup, View.GONE);
				// updateLinearLayoutVisible(mAnswerSlide, View.VISIBLE);
				// displayStatus(null,videoCall?getString(R.string.making_video_call):getString(R.string.making_call));
				// amInCall = true;
				// }
				// ***tml

				mADB.updateLastContactTimeByAddress(phoneNumber,
						new Date().getTime());
				if (UsersActivity.sortMethod == 1)
					UsersActivity.forceRefresh = true;
			}

			mDisplayName = intent.getStringExtra("DisplayName");

			if (AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL) {
				if (mDisplayName.length() < 1)
					mDisplayName = phoneNumber;
				mDisplayNameView.setText(mDisplayName);
				((ToggleButton) findViewById(R.id.keypad))
						.setVisibility(View.VISIBLE);

				float credit = mPref.readFloat("Credit", 0);
				showCredit(credit);
				((TextView) findViewById(R.id.credit))
						.setVisibility(View.VISIBLE);

				((TextView) findViewById(R.id.country))
						.setVisibility(View.VISIBLE);
				((TextView) findViewById(R.id.country)).setText(MyTelephony
						.getCountryNameByIndex(SipCallActivity.cIndex,
								DialerActivity.this));

				SelectedClass = mPref.readInt("SelectedClass", 0);
				FlipToggleView selClass = (FlipToggleView) findViewById(R.id.selected_class);
				if (selClass != null) {
					final int stringResId[] = { R.string.standard_class,
							R.string.premium_class, R.string.business_class };
					final int imageResId[] = { R.drawable.standard,
							R.drawable.premium, R.drawable.business };
					selClass.init(0, getString(stringResId[SelectedClass]),
							imageResId[SelectedClass], null);
					selClass.setChecked(true);
					selClass.setVisibility(View.VISIBLE);

					AnimationSet as = new AnimationSet(false);
					as.setInterpolator(new AccelerateInterpolator());
					TranslateAnimation ta = new TranslateAnimation(0, 0, 0,
							-300);
					ta.setDuration(200);
					ta.setStartOffset(2800);
					as.addAnimation(ta);
					as.setDuration(3000);
					selClass.startAnimation(as);

					new Thread() {
						@Override
						public void run() {
							try {
								synchronized (this) {
									wait(5000);
								}
							} catch (InterruptedException ex) {
								Log.e("startDialerStuff2 !@#$ "
										+ ex.getMessage());
							}
							mHandler.post(new Runnable() {
								public void run() {
									FlipToggleView fl = (FlipToggleView) findViewById(R.id.selected_class);
									if (fl != null)
										fl.setVisibility(View.GONE);
								}
							});
						}
					}.start();
				}

			} else if (mDisplayName != null) {
				if (contact_id > 0) {
					if (mDisplayName.length() > 0)
						mDisplayName = cq.getNameByContactId(contact_id) + " ("
								+ mDisplayName + ")";
					else
						mDisplayName = cq.getNameByContactId(contact_id);
				} else if (mDisplayName.length() == 0
						&& AireVenus.runAsSipAccount)
					mDisplayName = "";

				mDisplayNameView.setText(mDisplayName);
			} else {
				if (phoneNumber != null) {
					mDisplayName = mADB.getNicknameByAddress(phoneNumber);
					if (mDisplayName != null)
						mDisplayNameView.setText(mDisplayName);
				}
			}

			if (AireJupiter.getInstance() != null)
				AireJupiter.getInstance().StopEndingupServiceY();// alec

			// alec
			if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {
				previousCredit = mPref.readFloat("Credit", 0);
				((Button) findViewById(R.id.add))
						.setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								Intent it = new Intent(DialerActivity.this,
										AddCallDialog.class);
								it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivityForResult(it, 200);
							}
						});

				// tml*** vidconf
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
					((LinearLayout) findViewById(R.id.topVWin_holder))
							.post(new Runnable() {
								@Override
								public void run() {
									int titleH = ((FrameLayout) findViewById(R.id.title))
											.getMeasuredHeight();
									RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ((LinearLayout) findViewById(R.id.topVWin_holder))
											.getLayoutParams();
									params.bottomMargin = titleH;
									((LinearLayout) findViewById(R.id.topVWin_holder))
											.setLayoutParams(params);
								}
							});

					((SurfaceView) findViewById(R.id.topVWin_surface))
							.post(new Runnable() {
								@Override
								public void run() {
									int titleH = ((FrameLayout) findViewById(R.id.title))
											.getMeasuredHeight();
									RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ((SurfaceView) findViewById(R.id.topVWin_surface))
											.getLayoutParams();
									params.bottomMargin = titleH;
									((SurfaceView) findViewById(R.id.topVWin_surface))
											.setLayoutParams(params);
								}
							});

					((FrameLayout) findViewById(R.id.title))
							.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									if (((ImageView) findViewById(R.id.controls_expand))
											.getVisibility() != View.VISIBLE) {
										mHandler.removeCallbacks(showControls);
										mHandler.removeCallbacks(hideControls);
										mHandler.postDelayed(hideControls, 300);
										mHandler.removeCallbacks(showMembers);
										mHandler.removeCallbacks(hideMembers);
										mHandler.postDelayed(hideMembers, 300);
									} else {
										mHandler.postDelayed(showControls, 300);
										mHandler.postDelayed(hideControls, 6000);
										mHandler.postDelayed(showMembers, 300);
										mHandler.postDelayed(hideMembers, 10000);
									}
								}
							});
				}

				((ToggleButton) findViewById(R.id.video))
						.setOnCheckedChangeListener(new OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(
									CompoundButton buttonView, boolean isChecked) {
								if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
									if (isChecked) {
										initVidConf(false);
										mHandler.removeCallbacks(showControls);
										mHandler.removeCallbacks(hideControls);
										mHandler.postDelayed(hideControls, 3000);
										mHandler.removeCallbacks(showMembers);
										mHandler.removeCallbacks(hideMembers);
										mHandler.postDelayed(hideMembers, 3000);
									} else {
										endVidConf();
										mHandler.postDelayed(showMembers, 300);
									}
								} else {
									if (isChecked) {
										Toast tst = Toast.makeText(
												theDialer,
												getResources().getString(
														R.string.vidconf_sdk),
												Toast.LENGTH_SHORT);
										tst.show();
										buttonView.setChecked(false);
									}
								}
							}
						});
				// ***tml
			}

			if (AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL) {

			} else if (AireVenus.getCallType() == AireVenus.CALLTYPE_WEBCALL) {
				((ToggleButton) findViewById(R.id.keypad))
						.setVisibility(View.VISIBLE);
				FrameLayout fl = (FrameLayout) findViewById(R.id.sip_title);
				if (fl != null) {
					fl.setVisibility(View.VISIBLE);

					AnimationSet as = new AnimationSet(false);
					as.setInterpolator(new AccelerateInterpolator());
					TranslateAnimation ta = new TranslateAnimation(0, 0, 0,
							-500);
					ta.setDuration(200);
					ta.setStartOffset(2800);
					as.addAnimation(ta);
					as.setDuration(3000);
					fl.startAnimation(as);

					new Thread() {
						@Override
						public void run() {
							try {
								synchronized (this) {
									wait(5000);
								}
							} catch (InterruptedException ex) {
								Log.e("startDialerStuff3 !@#$ "
										+ ex.getMessage());
							}
							mHandler.post(new Runnable() {
								public void run() {
									FrameLayout fl = (FrameLayout) findViewById(R.id.sip_title);
									if (fl != null)
										fl.setVisibility(View.GONE);
								}
							});
						}
					}.start();
				}
			}

			created = true;

			new Thread(dialerStuff).start();

			InitDtmfKeyTone();

			// setVolumeControlStream(AudioManager.STREAM_RING);
			//
			// if (mAudioManager.isMusicActive())
			// mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
			// setVolumeControlStream(AudioManager.STREAM_MUSIC); //tml*** mute
			// error

			mSensorManager.registerListener(DialerActivity.this,
					proximitySensor, SensorManager.SENSOR_DELAY_FASTEST);

			if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {
				imageAdapter = new ImageAdapter(DialerActivity.this);
				PhotoGallery galley = (PhotoGallery) findViewById(R.id.members);
				if (galley != null) {
					galley.setAdapter(imageAdapter);
					galley.setLongClickable(true);
					galley.setOnItemLongClickListener(onRemoveUserLongClick);
				}
			}
			// bree：是对方的安全监护人时自动接听
			if (mPref.readBoolean("GuardYou", false)) {
				answer();
			}
		}
	};

	public void startDialerStuff() {

	}

	OnItemLongClickListener onRemoveUserLongClick = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
									   int arg2, long arg3) {
			String displayname = (String) memberList.get(arg2).get(
					"displayname");
			String uuid = (String) memberList.get(arg2).get("uuid");
			String Return = "";
			try {
				MyNet net = new MyNet(DialerActivity.this);
				int room = mPref.readInt("ChatroomHostIdx");
				if (Integer.parseInt(mPref.read("myID", "0"), 16) != room)
					return true; // tml*** only hostkicker
				String roomNumber = String.format("%07d", room);
				String domain;
				if (incomingChatroom) {
					domain = mPref.read("joinSipAddress",
							AireJupiter.myConfSipServer_default);
				} else {
					domain = mPref.read("conferenceSipServer",
							AireJupiter.myConfSipServer_default);
					if (AireJupiter.getInstance() != null) {
						domain = AireJupiter.getInstance().getIsoConf(domain); // tml***
						// china
						// ip
					}
				}

				// tml*** china ip
				String phpip = AireJupiter.myPhpServer_default;
				if (AireJupiter.getInstance() != null) {
					phpip = AireJupiter.getInstance().getIsoPhp(0, true,
							"74.3.165.66");
				}

				Return = net.doAnyPostHttp("http://" + phpip
						+ "/onair/conference/customer/hangup.php", "room="
						+ roomNumber + "&ip=" + domain + "&uuid=" + uuid);

				if (Return.toLowerCase().contains("ok")) {
					memberList.remove(arg2);
					imageAdapter.notifyDataSetChanged();
					String msg = getString(R.string.end_call) + " :"
							+ displayname;
					Toast.makeText(DialerActivity.this, msg, Toast.LENGTH_LONG)
							.show();
				} else {
					// Toast.makeText(DialerActivity.this,
					// "Failed to kick :"+displayname,
					// Toast.LENGTH_LONG).show();
				}
			} catch (Exception e) {
				Log.e("hangup.php1 !@#$ " + e.getMessage());
			}

			try {
				boolean found = false;
				for (int i = 0; i < memberList.size(); i++) {
					int idx = Integer.parseInt((String) memberList.get(i).get(
							"idx"));
					if (idx == 0) {
						found = true;
						break;
					}
				}
				if (!found)
					shouldCheckPSTNinChatroom = false;
			} catch (Exception e) {
				Log.e("hangup.php2 !@#$ " + e.getMessage());
			}

			return true;
		}

	};

	Runnable kickOutAllMembers = new Runnable() {
		public void run() {
			for (int i = 0; i < memberList.size(); i++) {
				String uuid = (String) memberList.get(i).get("uuid");
				try {
					MyNet net = new MyNet(DialerActivity.this);
					int room = mPref.readInt("ChatroomHostIdx");
					String roomNumber = String.format("%07d", room);
					String domain;
					if (incomingChatroom) {
						domain = mPref.read("joinSipAddress",
								AireJupiter.myConfSipServer_default);
					} else {
						domain = mPref.read("conferenceSipServer",
								AireJupiter.myConfSipServer_default);
						if (AireJupiter.getInstance() != null) {
							domain = AireJupiter.getInstance().getIsoConf(
									domain); // tml*** china ip
						}
					}

					// tml*** china ip
					String phpip = AireJupiter.myPhpServer_default;
					if (AireJupiter.getInstance() != null) {
						phpip = AireJupiter.getInstance().getIsoPhp(0, true,
								"74.3.165.66");
					}

					if (i > 0)
						MyUtil.Sleep(250);
					net.doAnyPostHttp("http://" + phpip
							+ "/onair/conference/customer/hangup.php", "room="
							+ roomNumber + "&ip=" + domain + "&uuid=" + uuid);
				} catch (Exception e) {
					Log.e("hangup.php3 !@#$ " + e.getMessage());
				}
			}
		}
	};

	void flipKeypad(boolean showup) {
		if (showup) {
			FrameLayout main = (FrameLayout) findViewById(R.id.keypad_panel);
			AnimationSet as = new AnimationSet(false);
			as.setInterpolator(new AccelerateInterpolator());
			AlphaAnimation aa = new AlphaAnimation(0, 1.0f);
			ScaleAnimation sa = new ScaleAnimation(0, 1, 0.6f, 1,
					main.getWidth() / 2, main.getHeight() / 2);
			sa.setDuration(300);
			as.addAnimation(sa);
			aa.setDuration(200);
			as.addAnimation(aa);
			as.setDuration(300);
			main.startAnimation(as);
			main.setVisibility(View.VISIBLE);
			mHideKeypad.setVisibility(View.VISIBLE);
			if (DTMFString.length() > 0)
				mDisplayNameView.setText(DTMFString);
		} else {
			FrameLayout main = (FrameLayout) findViewById(R.id.keypad_panel);
			AnimationSet as = new AnimationSet(false);
			as.setInterpolator(new AccelerateInterpolator());
			AlphaAnimation aa = new AlphaAnimation(1, 0.f);
			ScaleAnimation sa = new ScaleAnimation(1, 0, 1, 0.6f,
					main.getWidth() / 2, main.getHeight() / 2);
			sa.setDuration(500);
			as.addAnimation(sa);
			aa.setDuration(200);
			aa.setStartOffset(200);
			as.addAnimation(aa);
			as.setDuration(500);
			main.startAnimation(as);
			main.setVisibility(View.INVISIBLE);
			mHideKeypad.setVisibility(View.GONE);
			mDisplayNameView.setText(mDisplayName);
			if (DTMFString.length() > 0)
				DTMFString += " ";
		}
	}

	private Bitmap getUserPhoto(String address) {
		if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {
			if (incomingChatroom) {
				int idx = getIntent().getIntExtra("ChatroomHostIdx", 0);
				String path = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
				return ImageUtil.getBitmapAsRoundCorner(1, 5, path);
			}
			return ImageUtil.drawableToBitmap(getResources().getDrawable(
					R.drawable.group_bg));
		}

		if (!MyUtil.checkSDCard(DialerActivity.this))
			return null;
		if (address.length() == 0)
			return null;

		if (bCommercial) {
			String path = Global.SdcardPath_inbox + address + ".png";
			return ImageUtil.getBitmapAsRoundCorner(1, 5, path);
		} else {
			int idx = mADB.getIdxByAddress(address);
			if (idx > 0) {
				String path = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
				return ImageUtil.getBitmapAsRoundCorner(1, 5, path);
			} else if (contact_id > 0) {
				BitmapDrawable d = (BitmapDrawable) cq.getPhotoById(this,
						contact_id, false);
				if (d != null)
					return d.getBitmap();
			}
		}
		return null;
	}

	void checkIfStartWithVideo() {
		if (videoCall && Version.sdkAboveOrEqual(9) && streamsRunning == false) {
			// int n=AndroidCameraConfiguration.retrieveCameras().length;
			// startSelfPreview(n>1?1:0,mSurface,surfaceAngel);
		}
	}

	Runnable dialerStuff = new Runnable() {
		public void run() {
			if (!incomingCall)
				checkIfStartWithVideo();

			Log.d("!!  wait for registered");

			int xcount = 0;
			while ((AireVenus.instance() == null || !AireVenus.isready() || !AireVenus
					.instance().registered) && ++xcount < 100)
				// alec
				MyUtil.Sleep(100);

			if (AireVenus.instance() != null) {
				Log.d("!!  wait for registered...done " + AireVenus.isready()
						+ " " + AireVenus.instance().registered + " " + xcount
						+ " in:" + incomingCall);
			} else {
				Intent istop = new Intent(DialerActivity.this, AireVenus.class);
				stopService(istop); // tml*** airevenus fail
				Log.d("!!  wait for registered...ohuh !@#$AireVenus " + xcount);
			}

			try {

				/*
				 * if (AireVenus.isready() && getIntent().getData() != null &&
				 * !AireVenus.instance().getVoipCore().isIncall()) {
				 * updateLinearLayoutVisible(mAnswerSlide, View.GONE);
				 * updateButtonVisible(mHangup, View.VISIBLE);
				 * 
				 * //alec this part is useless String
				 * callee=getIntent().getData().toString();
				 * newOutgoingCall(callee.substring("tel://".length()),
				 * videoCall); getIntent().setData(null); }
				 */
				// tml*** switch conf
				// boolean switchCall = false;
				// if (AireJupiter.getInstance() != null)
				// switchCall = AireJupiter.getInstance().getSetSwitchCall();
				// if (switchCall) {
				// // incomingCall = false;
				// incomingChatroom = mPref.readBoolean("incomingChatroom",
				// false);
				// }
				// Log.e("--------- SWITCH CALL --------- dialerStuff " +
				// switchCall + " " + mAddress + " " + phoneNumber + " incCall:"
				// + incomingCall + " incChat:" + incomingChatroom);

				if (AireVenus.isready()) {
					VoipCore lVoipCore = AireVenus.instance().getVoipCore();
					if (lVoipCore.isIncall() && incomingCall)
					// if (lVoipCore.isIncall() && incomingCall && !switchCall)
					// //tml*** switch call
					{
						mPref.write("tempCheckSameIN", 0); // tml*** sametime
						String IncomingNumber = lVoipCore.getRemoteAddress()
								.getUserName();
						String debugN = "--";
						if (IncomingNumber != null
								&& IncomingNumber.length() > 7) // tml test
							debugN = IncomingNumber.substring(0, 5) + "..";
						mPref.write("lastCallSip", "I:" + debugN);
						if (IncomingNumber != null
								&& IncomingNumber.length() > 0)// alec
						{
							mAddress = IncomingNumber;// alec

							videoCall = lVoipCore.getVideoEnabled();

							checkIfStartWithVideo();

							int remote = 1;
							if (AireVenus.runAsSipAccount)// alec
								remote = 3;
							else
								remote = ContactsOnline
										.getContactOnlineStatus(mAddress);
							if (remote == 0)
								remote = 1;
							Log.d("**** Net Type: REMOTE: " + remote + " ***");
							lVoipCore.setNetType(new NetInfo(
									DialerActivity.this).netType, remote);

							contact_id = cq
									.getContactIdByNumber(IncomingNumber);
							mDisplayName = mADB.getNicknameByAddress(mAddress);
							if (contact_id > 0) {
								if (mDisplayName.length() > 0)
									mDisplayName = cq
											.getNameByContactId(contact_id)
											+ " (" + mDisplayName + ")";
							}
							updateTextView(mDisplayNameView, mDisplayName);
							mProfileImage = (ImageView) findViewById(R.id.bighead);
							Bitmap photo = getUserPhoto(mAddress);

							updateImageView(mProfileImage, photo);

							if (AireVenus.instance() != null)
								AireVenus.instance().renableCodec(videoCall);// alec

							if (lVoipCore.isInComingInvitePending()) {
								callPending(lVoipCore.getCurrentCall());
								mADB.updateLastContactTimeByAddress(
										IncomingNumber, new Date().getTime());
								if (UsersActivity.sortMethod == 1)
									UsersActivity.forceRefresh = true;

								incomingCall = true;

								updateButtonVisible(mHangup, View.GONE);
								updateButtonVisible(mHangup2, View.VISIBLE); // tml***
								// beta
								// ui
								updateButtonVisible(mHold, View.GONE);
								updateLinearLayoutVisible(mAnswerSlide,
										View.VISIBLE);

								displayStatus(
										null,
										videoCall ? getString(R.string.incoming_video_call)
												: getString(R.string.incoming_call));

							} else {
								configureMuteAndSpeakerButtons();
								mSensorManager.registerListener(
										DialerActivity.this, proximitySensor,
										SensorManager.SENSOR_DELAY_FASTEST);
							}

						} else {
							Log.e("Incoming call is null");
							finish();
						}
					} else if (!incomingCall) {
						// Log.e("--------- SWITCH CALL --------- outgoingCall "
						// + mAddress + " " + phoneNumber + " incChat:" +
						// incomingChatroom);
						Log.i("voip.DA oooutgoing! >> " + mAddress + " "
								+ phoneNumber + " inChat:" + incomingChatroom);
						if (incomingChatroom) {
							mPref.write("tempCheckSameIN", 0); // tml***
							// sametime
							mPref.write("lastCallSip", "O:" + "CONF");
							lVoipCore.setNetType(new NetInfo(
									DialerActivity.this).netType, 3);
							// if (switchCall) { //tml*** switch conf
							// mPref.write("way", true);
							// Intent it = new Intent(Global.Action_AnswerCall);
							// sendBroadcast(it);
							// }
						} else {
							if (phoneNumber.equals(AireJupiter.myPhoneNumber)) {
								finish();
								return;
							}

							updateLinearLayoutVisible(mAnswerSlide, View.GONE);
							updateButtonVisible(mHangup2, View.GONE); // tml***
							// beta
							// ui
							updateButtonVisible(mHold, View.VISIBLE);
							updateButtonVisible(mHangup, View.VISIBLE);

							if (!BluetoothSco && !dontUseInCallMode)
								mAudioManager
										.setMode(AudioManager.MODE_IN_CALL);

							// alec
							int remote = 0;
							String addrTo = mAddress;

							// tml*** sametime, DA already up
							int idxIN = mPref.readInt("tempCheckSameIN");
							int idxOUT = mADB.getIdxByAddress(phoneNumber);
							String debugN = "--";
							if (addrTo != null && addrTo.length() > 7) // tml
								// test
								debugN = addrTo.substring(0, 5) + "..";
							mPref.write("lastCallSip", "O:" + debugN);
							Log.d("Check postDA SAMETIME in/out> " + idxIN
									+ "/" + idxOUT);
							if (idxIN == idxOUT) {
								Log.e("voip.callout SAMETIME!!! (postDA)");
								mPref.write("tempCheckSameIN", 0);
								responseCallBusy();
								return;
							}
							// ***tml

							if (AireVenus.runAsSipAccount) {
								remote = 3;
								streamsRunning = false;
							} else {
								remote = ContactsOnline
										.getContactOnlineStatus(mAddress);

								xcount = 0;
								while ((AireJupiter.getInstance() == null || !AireJupiter
										.getInstance().calleeGotCallRequest)
										&& ++xcount < 100) {
									MyUtil.Sleep(100);
								}

								addrTo = getYourSipServerByTCP(mAddress);
							}
							if (addrTo != null) { // tml test
								if (addrTo.contains("@")) {
									String debugN2[] = addrTo.split("@");
									if (debugN2[0].length() > 7)
										debugN2[0] = debugN2[0].substring(0, 5)
												+ "..";
									debugN = debugN2[0] + "@" + debugN2[1];
								} else {
									if (addrTo.length() > 7)
										debugN = addrTo.substring(0, 5) + "..";
								}
							} else {
								debugN = "null";
							}
							mPref.write("lastCallSip", "O:" + debugN);

							lVoipCore.setNetType(new NetInfo(
									DialerActivity.this).netType, remote);
							if (addrTo.contains("@nonmember"))
								finish();
							else if (!HangingUp) {
								if (AireJupiter.getInstance() != null
										&& !AireVenus.runAsSipAccount) {
									if (AireVenus.instance() != null) {
										// AireVenus.instance().startRingBack();
										AireVenus
												.instance()
												.prepareRing(
														true,
														0,
														AireVenus.getCallType(),
														"!incomingCall !HangingUp !runAsSip"); // tml***
										// new
										// ring
									}

									Log.d("wait for calleeGotCallRequest....");

									int c = 0;
									while (!AireJupiter.getInstance().calleeGotCallRequest
											&& ++c < 350)
										// alec: 35 sec timeout
										MyUtil.Sleep(100);

									Log.d("wait for calleeGotCallRequest....Done");
								}

								if (AireVenus.instance() != null)
									AireVenus.instance()
											.renableCodec(videoCall);// alec

								if (!HangingUp && !AireVenus.runAsSipAccount) {
									MyUtil.Sleep(1000);
									// Log.d("wait for....Done");
								}
								if (!HangingUp && !incomingChatroom)
									newOutgoingCall(addrTo, videoCall,
											"outChat");
							}
						}
					}

					mWakeLock.acquire();
				} else {
					Log.e("exit.AireVenus NOT READY");
					finish(); // tml*** airevenus fail
				}
			} catch (Exception e) {
				Log.e("exit.Fail to start dialer " + e.getMessage());
				finish();
			}
		}
	};

	// zhao
	private void updateTextView(TextView view, String text) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("textview", view);
		map.put("text", text);
		Message msg = new Message();
		msg.obj = map;
		msg.what = 1;
		handler.sendMessage(msg);
	}

	private void updateImageView(ImageView imageview, Bitmap bitmap) {
		if (bitmap == null)
			return;
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("imageview", imageview);
		map.put("image", bitmap);
		Message msg = new Message();
		msg.obj = map;
		msg.what = 2;
		handler.sendMessage(msg);
	}

	private void updateButtonVisible(Button button, int visible) {
		Message msg = new Message();
		msg.obj = button;
		msg.arg1 = visible;
		msg.what = 3;
		handler.sendMessage(msg);
	}

	private void updateLinearLayoutVisible(LinearLayout linear, int visible) {
		Message msg = new Message();
		msg.obj = linear;
		msg.arg1 = visible;
		msg.what = 4;
		handler.sendMessage(msg);
	}

	private String SrcAudioPath;

	@SuppressWarnings("deprecation")
	private void onVoiceMemoRecording(String path) {
		try {
			// vm=new VoiceMemo_NB(path);
			// vm.start();
			// tml*** new vmsg
			myVR = new VoiceRecord2_MR(DialerActivity.this, path,
					MediaRecorder.OutputFormat.RAW_AMR,
					MediaRecorder.OutputFormat.DEFAULT, 8000);
			myVR.start();
		} catch (Exception e) {
			Log.e("onVoiceMemoRecording !@#$ " + e.getMessage());
			myVR = null;
		}
	}

	private void oStopMemoRecording() {
		// if (vm!=null)
		// {
		// vm.stop();
		// vm = null;
		// }
		if (myVR != null) { // tml*** new vmsg
			myVR.stop();
			myVR = null;
		}
		int dur = (int) ((new Date().getTime() - startTime) / 1000);
		onSendMessage(SrcAudioPath, dur);
	}

	private void onSendMessage(String SrcAudioPath, int duration) {
		try {
			SendAgent agent = null;
			int myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
			int idx = mADB.getIdxByAddress(mAddress);
			agent = new SendAgent(this, myIdx, idx, true);
			boolean ret = agent.onSend(mAddress, "(Vm)" + duration, 1,
					SrcAudioPath, null, false);
			if (ret) {
				SMS msg = new SMS();
				msg.displayname = mDisplayName;
				msg.address = mAddress;
				msg.content = "(Vm)" + duration;
				msg.contactid = contact_id;
				msg.read = 1;
				msg.type = 2;
				msg.status = 2;// pending
				msg.time = new Date().getTime();

				msg.attached = 1;
				msg.att_path_aud = SrcAudioPath;
				msg.att_path_img = null;
				SmsDB mDB = new SmsDB(this);
				mDB.open();

				msg.longitudeE6 = mPref.readLong("longitude", 0);
				msg.latitudeE6 = mPref.readLong("latitude", 0);

				long rowid = mDB.insertMessage(mAddress, msg.contactid,
						(new Date()).getTime(), 1, msg.status, msg.type, "",
						msg.content, msg.attached, msg.att_path_aud,
						msg.att_path_img, 0, msg.longitudeE6, msg.latitudeE6,
						0, null, null, 0);

				agent.setRowId(rowid);

				mDB.close();
			}
		} catch (Exception e) {
			Log.e("onSendMessage !@#$ " + e.getMessage());
		}
	}

	BroadcastReceiver SliderResponse = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i("BRx SliderResponse=" + intent.getAction().toString());
			if (intent.getAction().equals(
					Global.Action_Sip_Photo_Download_Complete)) {
				Bitmap photo = getUserPhoto(mAddress);
				if (photo != null) {
					mProfileImage = (ImageView) findViewById(R.id.bighead);
					if (mProfileImage != null)
						mProfileImage.setImageBitmap(photo);
				}
			} else if (intent.getAction().equals(Global.Action_AnswerCall)) {
				//bree	如果是新的广播自动打开摄像头
				if (mPref.readInt("BCAST_CONF", -1) >= 0 && mPref.readBoolean("pay", false)){
					((ToggleButton) findViewById(R.id.video)).setChecked(true);
				}
				mPref.write("tempCheckSameIN", 0); // tml*** sametime

				stopSelfPreview();
				mHangup2.setVisibility(View.GONE); // tml*** beta ui
				mHold.setVisibility(View.VISIBLE);
				mAnswerSlide.setVisibility(View.GONE);
				if (!BluetoothSco && !dontUseInCallMode)
					mAudioManager.setMode(AudioManager.MODE_IN_CALL);

				if (incomingChatroom) {
					mHandler.removeCallbacks(ringLimit);
					mHandler.postDelayed(callToChatroom, 200);
				} else
					mHandler.postDelayed(answerCall, 200);
			} else if (intent.getAction().equals(Global.ACTION_PLAY_AUDIO)) {
				if (intent.getIntExtra("clear", 0) == 3) {
					recordingVoiceMail = true;
					startTime = new Date().getTime();// alec
					mHandler.postDelayed(timeElapsed, 1000);

					SrcAudioPath = Global.SdcardPath_sent
							+ ConversationActivity.getRandomName() + ".amr";
					// SrcAudioPath = Global.SdcardPath_sent +
					// ConversationActivity.getRandomName() + ".mp3"; //tml***
					// new vmsg
					onVoiceMemoRecording(SrcAudioPath);
				}
			} else if (intent.getAction().equals(Global.MSG_UNREAD_YES)) { // tml***
				// chatview
				if (((ToggleButton) findViewById(R.id.chatview)) != null) {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							if (!mChatView.isChecked()) {
								mChatView.setChecked(true);
							}
						}
					});
				}
			} else if (intent.getAction().equals(Global.MSG_RETURN_NOM)) { // tml***
				// chatview
				if (((ToggleButton) findViewById(R.id.chatview)) != null) {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							mChatView.setChecked(false);
						}
					});
				}

				if (((ToggleButton) findViewById(R.id.mute)) != null) {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							if (((ToggleButton) findViewById(R.id.mute)) != null) {
								boolean mute = mPref.readBoolean(
										"chatCallMute", false);
								if (mute) {
									if (!((ToggleButton) findViewById(R.id.mute))
											.isChecked()) {
										((ToggleButton) findViewById(R.id.mute))
												.setChecked(true);
									}
								} else {
									if (((ToggleButton) findViewById(R.id.mute))
											.isChecked()) {
										((ToggleButton) findViewById(R.id.mute))
												.setChecked(false);
									}
								}
							}
						}
					});
				}

				if (((ToggleButton) findViewById(R.id.speaker)) != null) {
					boolean spkr = mPref.readBoolean("chatCallSpkr", false);
					if (spkr) {
						if (((ToggleButton) findViewById(R.id.speaker))
								.isChecked()) {
							mHandler.postDelayed(new Runnable() {
								@Override
								public void run() {
									if (((ToggleButton) findViewById(R.id.speaker)) != null) {
										((ToggleButton) findViewById(R.id.speaker))
												.setChecked(false);
									}
								}
							}, 1300);
						}
					} else {
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								if (((ToggleButton) findViewById(R.id.speaker)) != null) {
									((ToggleButton) findViewById(R.id.speaker))
											.setChecked(false);
								}
							}
						});
					}
				}
			}
		}
	};

	Runnable callToChatroom = new Runnable() {
		public void run() {
			// stopRinging();
			stopRing(); // tml*** new ring

			if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM
					&& incomingChatroom) {
				try {
					if (!AmazonKindle.hasMicrophone(DialerActivity.this)) {
						mHandler.postDelayed(new Runnable() {
							public void run() {
								exitCallMode("AmazonKindle noMic");
							}
						}, 1500);
						return;
					}
				} catch (Exception e) {
					Log.e("callToChatroom !@#$ " + e.getMessage());
				}

				if (!BluetoothSco && !dontUseInCallMode)
					mAudioManager.setMode(AudioManager.MODE_IN_CALL);

				setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);// alec
				setStreamVolume(AudioManager.STREAM_VOICE_CALL, 1.0f, 0);// alec
				//bree:如果不是新广播
				if (!(mPref.readInt("BCAST_CONF", -1) >= 0 && mPref.readBoolean("pay", false))) {
					newOutgoingCall(mAddress, videoCall, "inChat");
				}
			}
		}
	};

	Runnable answerCall = new Runnable() {
		public void run() {
			try {
				if (!AmazonKindle.hasMicrophone(DialerActivity.this)) {
					if (AireVenus.instance() != null) {
						VoipCore lVoipCore = AireVenus.instance().getVoipCore();
						VoipCall myCall = lVoipCore.getCurrentCall();
						if (myCall != null) {
							lVoipCore.terminateCall(myCall);
						}
					}
					return;
				}
			} catch (Exception e) {
				Log.e("answerCall1 !@#$ " + e.getMessage());
			}

			if (AireVenus.instance() != null) {
				VoipCore lVoipCore = AireVenus.instance().getVoipCore();
				if (lVoipCore.isInComingInvitePending()) {
					try {
						VoipCall vc = lVoipCore.getCurrentCall();
						lVoipCore.acceptCall(vc);
					} catch (VoipCoreException e) {
						Log.e("answerCall2 !@#$ " + e.getMessage());
						lVoipCore = AireVenus.instance().getVoipCore();
						VoipCall myCall = lVoipCore.getCurrentCall();
						if (myCall != null) {
							lVoipCore.terminateCall(myCall);
						}
					}
					return;
				} else { // invite is not pending, due to poor network condition
					lVoipCore = AireVenus.instance().getVoipCore();
					VoipCall myCall = lVoipCore.getCurrentCall();
					if (myCall != null) {
						lVoipCore.terminateCall(myCall);
					}
				}
			}
		}
	};

	@Override
	public void onBackPressed() {
		Log.e("voip.HANGUP2b DA *** USER PRESSED ***");
		mPref.write("tempCheckSameIN", 0); // tml*** sametime

		if (AireVenus.instance() != null) {
			VoipCore lVoipCore = AireVenus.instance().getVoipCore();
			if (lVoipCore != null) {
				VoipCall myCall = lVoipCore.getCurrentCall();
				if (myCall != null) {
					lVoipCore.terminateCall(myCall);
					Log.e("voip.HANGUP2b DA *** USER PRESSED *** OK");
					return;
				} else {
					Log.e("voip.HANGUP2b DA *** USER PRESSED *** myCall=null");
				}
			} else {
				Log.e("voip.HANGUP2b DA *** USER PRESSED *** lVoipCore=null");
			}

			if (recordingVoiceMail)
				oStopMemoRecording();
			Intent VoipIntent = new Intent(Intent.ACTION_MAIN);
			VoipIntent.setClass(DialerActivity.this, PlayService.class);
			stopService(VoipIntent);
		}
		exitCallMode("onBackPressed");
		super.onBackPressed();
	}

	private void updateCallLog() {
		Log.d("updateCallLog....");
		int dur = 0;
		int direction = incomingCall ? 1 : 2;
		int status = Connected ? 1 : 0;
		if (status != 0)
			dur = (int) ((new Date().getTime() - startTime) / 1000);

		if (AireVenus.getCallType() == AireVenus.CALLTYPE_FAFA) {
			Intent it = new Intent(Global.Action_InternalCMD);
			it.putExtra("Command", Global.CMD_UPDATE_CALL_LOG);
			it.putExtra("address", mAddress);
			it.putExtra("displayname", mDisplayName);
			it.putExtra("contact_id", contact_id);
			it.putExtra("time", new Date().getTime());
			it.putExtra("type", sysIncomingNumber == null ? 1 : 2);
			it.putExtra("duration", dur);
			it.putExtra("direction", direction);// 1: incoming, 2:outgoing
			it.putExtra("status", status);

			// for webcall log
			it.putExtra("runAsSip", false);
			it.putExtra("starttime", startTime);
			this.sendBroadcast(it);
		} else if (AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL) {
			long callLogRowId = SipCallActivity.getLatestCallLogRowId();
			if (callLogRowId != -1) {
				Log.i("CallLogRowId=" + callLogRowId + "  credit="
						+ consumedCredit + "  dur=" + dur);
				mCLDB.update(callLogRowId, consumedCredit, dur);
			} else {
				Log.w("CallLogRowId=" + callLogRowId);
			}
		} else if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {
			if (PSTNCallLogRowId != -1) {
				Log.i("PSTNCallLogRowId=" + PSTNCallLogRowId + "  credit="
						+ consumedCredit + "  dur=" + confLogDuration);
				mCLDB.update(PSTNCallLogRowId, consumedCredit, confLogDuration);
			}
		}
	}

	@Override
	protected void onDestroy() {
		if (mSensorManager != null)
			mSensorManager.unregisterListener(this);// wanghuan
		uiDAinFore = false; // tml*** conn notify/
		if (mPref != null)
			mPref.write("tempCheckSameIN", 0); // tml*** sametime
		mPref.write("curCall", "");
		AireVenus.callstate_AV = null; // tml***
		// if (AireJupiter.getInstance() != null)
		// AireJupiter.getInstance().setSwitchCall(false, "DA onDestroy");
		// //tml*** switch conf
		mHandler.removeCallbacks(startDialerStuff);
		mHandler.removeCallbacks(displayHistogram);
		mHandler.removeCallbacks(displayP2P);
		mHandler.removeCallbacks(checkCurrentCall); // tml*** check empty call
		mHandler.removeCallbacks(checkPkt);
		mHandler.removeCallbacks(checkP2PMode);
		mHandler.removeCallbacks(refreshChatRoomMember);
		mHandler.removeCallbacks(ringLimit);
		theDialer = null;

		if (xWalkWebView != null) { // tml|yangjun*** vidconf
			// ((LinearLayout)
			// findViewById(R.id.topVWin_holder)).removeView(xWalkWebView);
			xWalkWebView.onDestroy();
			xWalkWebView = null;
			Log.e("vidConfxwalk Destroyed");
		}
		// stopRinging();
		stopRing(); // tml*** new ring
		if (AireVenus.instance() != null)
			AireVenus.instance().stopRing();
		// if(rwb!=null){ //yang*** speex player
		// rwb.stop();
		// rwb.release();
		// }
		if (created) {
			created = false;
			if (launchingVideo)
				exitCallMode(true, "onDestroy,created,launchingVideo");

			setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);// alec
			NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			mNM.cancel(R.string.call);
			mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
					PreviousVolume, 0);
			mAudioManager.setMode(AudioManager.MODE_NORMAL);
			mAudioManager.unloadSoundEffects();
			mAudioManager.setSpeakerphoneOn(false);
			try {
				if (mWakeLock != null)
					mWakeLock.release();
				if (mVibrator != null)
					mVibrator.cancel();
			} catch (Exception e) {
				Log.e("da onDestroy !@#$ " + e.getMessage());
			}

			if (BluetoothSco)
				Bluetooth.enable(false);

			mADB.close();
			mCLDB.close();
			Connected = false;
			launchingVideo = false;

			if (tg != null)
				tg.release();
		}

		memberList.clear();
		if (AireJupiter.getInstance() != null) {
			AireJupiter.getInstance().TESTseeVolumes("DAdestroy");
		}

		System.gc();
		System.gc();
		mPref.write("GuardYou", false);
		Log.e("*** DIALER *** DESTROY DESTROY *** voip " + SettingActivity.vlib);
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.e("*** !!! DIALER *** RESUME RESUME !!! ***");
		uiDAinFore = true; // tml*** conn notify/
		if (created) {
			if (incomingChatroom) {
				if (Connected) {
					mAnswerSlide.setVisibility(View.GONE);
					mHangup2.setVisibility(View.GONE); // tml*** beta ui
					mHold.setVisibility(View.VISIBLE);
					mHangup.setVisibility(View.VISIBLE);
				} else {
					mHangup.setVisibility(View.GONE);
					mHangup2.setVisibility(View.VISIBLE); // tml*** beta ui
					mHold.setVisibility(View.GONE);
					mAnswerSlide.setVisibility(View.VISIBLE);
				}
			} else if (Connected || !incomingCall) {
				mAnswerSlide.setVisibility(View.GONE);
				mHangup2.setVisibility(View.GONE); // tml*** beta ui
				mHold.setVisibility(View.VISIBLE);
				mHangup.setVisibility(View.VISIBLE);
			} else if (incomingCall) {
				mHangup.setVisibility(View.GONE);
				mHangup2.setVisibility(View.VISIBLE); // tml*** beta ui
				mHold.setVisibility(View.GONE);
				mAnswerSlide.setVisibility(View.VISIBLE);
			}
		}

		disableKeyguard();

		IntentFilter intentToReceiveFilter = new IntentFilter();
		intentToReceiveFilter.addAction(Global.Action_AnswerCall);
		intentToReceiveFilter
				.addAction(Global.Action_Sip_Photo_Download_Complete);
		intentToReceiveFilter.addAction(Global.ACTION_PLAY_AUDIO);
		intentToReceiveFilter.addAction(Global.MSG_UNREAD_YES);
		intentToReceiveFilter.addAction(Global.MSG_RETURN_NOM);
		this.registerReceiver(SliderResponse, intentToReceiveFilter);

		// MobclickAgent.onResume(this);

		if (addingList.size() > 0)
			new Thread(sendNotifyForJoinChatroom).start();
		if (addingList2.size() > 0)
			new Thread(addPSTNtoJoinChatroom).start();
//		if (addingList3.size() > 0)
//			new Thread(addPhonetoJoinChatroom).start();
	}

	@Override
	protected void onPause() {
		uiDAinFore = false; // tml*** conn notify/
		if (mSensorManager != null)
			mSensorManager.unregisterListener(this);// wanghuan
		unregisterReceiver(SliderResponse);// alec
		reenableKeyguard();

		// stopRinging();
		// stopRing(); //tml*** new ring
		DialerActivity.addingList.clear();
		DialerActivity.addingList2.clear();

		if (created) {
			if (launchingVideo) {
				super.onPause();
				return;
			}

			if (isFinishing()) {
				// restore audio settings

				stopSelfPreview();

				if (bSamsung || zxZTE)
					mAudioManager.setMode(AudioManager.MODE_NORMAL);
				mAudioManager.setSpeakerphoneOn(false);

				Log.d("broadcast CALL END...");
				Intent it = new Intent(Global.Action_InternalCMD);
				it.putExtra("Command", Global.CMD_CALL_END);
				it.putExtra("immediately", AireVenus.runAsSipAccount ? 2000
						: (isRunningP2P ? 90000 : 60000));
				it.putExtra(
						"AireCall",
						(AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL)
								|| shouldCheckPSTNinChatroom);

				if (AireJupiter.getInstance() != null)
					AireJupiter.getInstance().attemptCall = false;
				this.sendBroadcast(it);

				if (sendTerminateSignal) {
					if (mAddress.length() > 0
							&& AireJupiter.getInstance() != null
							&& AireVenus.runAsSipAccount == false) {
						AireJupiter.getInstance().terminateCallBySocket(
								mAddress);
						Log.e("onPause > terminateCallBySocket");
					}
				}
			} else {
				showNotification();
			}

			controlBkgndMusic(2);

			// MobclickAgent.onPause(this);
		}
		Log.e("*** !!! DIALER *** PAUSE PAUSE !!! ***");
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
		// TODO Auto-generated method stub

	}

	public void byeReceived(VoipCore p, String from) {
		// TODO Auto-generated method stub

	}

	public void displayMessage(VoipCore p, String message) {
		// TODO Auto-generated method stub
	}

	String mMsg = "";

	Runnable run_disp_msg = new Runnable() {
		public void run() {
			try {
				if (theDialer != null)
					theDialer.mStatus.setText(mMsg);
			} catch (NullPointerException e) {
				Log.e("run_disp_msg !@#$ " + e.getMessage());
			}
		}
	};

	public void displayStatus(VoipCore p, String message) {
		if (theDialer != null) {
			mMsg = message;
			mHandler.post(run_disp_msg);
		}
	}

	private void showCredit(float credit) {
		TextView tv = (TextView) findViewById(R.id.credit);
		if (tv != null)
			tv.setText(String.format(getString(R.string.credit), credit));
	}

	private Handler mHandler = new Handler();
	private int lastMinute = -1;
	private Runnable timeElapsed = new Runnable() {
		@Override
		public void run() {
			if (Connected || recordingVoiceMail)
			// if (Connected || recordingVoiceMail || amInCall || amInCall2)
			// //tml*** incall rot
			{
				long sec = (new Date().getTime() - startTime) / 1000;
				displayStatus(null, DateUtils.formatElapsedTime(sec));

				mHandler.postDelayed(timeElapsed, 1000);

				if (shouldCheckPSTNinChatroom)
					confLogDuration++;

				if (shouldCheckPSTNinChatroom && lastMinute != ((int) sec / 60)) {
					lastMinute = ((int) sec / 60);
					float credit = mPref.readFloat("Credit", 0);
					if (credit > -0.02) {
						float rate = 0.005f;

						if (isMobileNumber)
							rate = DQRates.getMobileRateByIso(cIso, 1);
						else
							rate = DQRates.getFixedRateByIso(cIso, 1);

						if (rate == 0)
							rate = 0.005f;

						consumedCredit += rate;

						credit -= rate;
						mPref.writeFloat("Credit", credit);

						showCredit(credit);

						if (PSTNCallLogRowId != -1) {
							mCLDB.update(PSTNCallLogRowId, consumedCredit,
									confLogDuration);
						}
					} else {
						Log.e("AireCall Terminate due to credit:" + credit);
						if (AireVenus.instance() != null) {
							VoipCore lVoipCore = AireVenus.instance()
									.getVoipCore();
							if (lVoipCore != null) {
								VoipCall myCall = lVoipCore.getCurrentCall();
								if (myCall != null) {
									lVoipCore.terminateCall(myCall);
									return;
								}
							}
						}
						exitCallMode("AireCall noCredit1");
					}
				} else if (AireVenus.runAsSipAccount && streamsRunning
						&& shouldConsumeCredit
						&& lastMinute != ((int) sec / 60)) {
					lastMinute = ((int) sec / 60);
					float credit = mPref.readFloat("Credit", 0);

					if (credit > -0.02) {
						float rate = 0.005f;

						if (SipCallActivity.isMobileNumber)
							rate = DQRates.getMobileRateByIso(
									SipCallActivity.cIso, SelectedClass);
						else
							rate = DQRates.getFixedRateByIso(
									SipCallActivity.cIso, SelectedClass);

						if (rate == 0)
							rate = 0.005f;

						consumedCredit += rate;

						credit -= rate;
						mPref.writeFloat("Credit", credit);

						showCredit(credit);

						long callLogRowId = SipCallActivity
								.getLatestCallLogRowId();
						if (callLogRowId != -1) {
							Log.i("Call log update during call " + callLogRowId
									+ " consumedCredit=" + consumedCredit);
							mCLDB.update(callLogRowId, consumedCredit,
									(int) sec);
						}
					} else {

						Log.e("AireCall Terminate due to credit:" + credit);
						if (AireVenus.instance() != null) {
							VoipCore lVoipCore = AireVenus.instance()
									.getVoipCore();
							if (lVoipCore != null) {
								VoipCall myCall = lVoipCore.getCurrentCall();
								if (myCall != null) {
									lVoipCore.terminateCall(myCall);
									return;
								}
							}
						}
						exitCallMode("AireCall noCredit2");
					}
				}
			}
		}
	};

	public void displayWarning(VoipCore p, String message) {
		// TODO Auto-generated method stub
	}

	public void globalState(VoipCore p, VoipCore.GlobalState state,
							String message) {
		/*
		 * if (state == VoipCore.GlobalState.GlobalOn) { try{
		 * AireVenus.instance().initFromConf(); } catch (VoipConfigException ec)
		 * { Log.w("no valid settings found "+ec.getMessage()); } catch
		 * (Exception e ) { Log.e("Cannot get initial config "+e.getMessage());
		 * } if (getIntent().getData() != null) {
		 * newOutgoingCall(getIntent().getData
		 * ().toString().substring("tel://".length()), videoCall);
		 * getIntent().setData(null); } }
		 */
	}

	public void registrationState(final VoipCore p, final VoipProxyConfig cfg,
								  final VoipCore.RegistrationState state, final String smessage) {
		/* nop */
	};

	/*
	 * SIMON 030111 callStates returned from Voipcore.c are VoipCallIdle,
	 * VoipCallIncomingReceived, //<This is a new incoming call
	 * VoipCallOutgoingInit, //<An outgoing call is started
	 * VoipCallOutgoingProgress, //<An outgoing call is in progress
	 * VoipCallOutgoingRinging, //<An outgoing call is ringing at remote end
	 * VoipCallOutgoingEarlyMedia, //<An outgoing call is proposed early media
	 * VoipCallConnected, //<Connected, the call is answered
	 * VoipCallStreamsRunning, //<The media streams are established and running
	 * VoipCallPausing, //<The call is pausing at the initiative of local end
	 * VoipCallPaused, //< The call is paused, remote end has accepted the pause
	 * VoipCallResuming, //<The call is being resumed by local end
	 * VoipCallRefered, //<The call is being transfered to another party,
	 * resulting in a new outgoing call to follow immediately VoipCallError,
	 * //<The call encountered an error VoipCallEnd, //<The call ended normally
	 * VoipCallPausedByRemote, //<The call is paused by remote end
	 * VoipCallUpdatedByRemote, //<The call's parameters are updated, used for
	 * example when video is asked by remote VoipCallIncomingEarlyMedia, //<We
	 * are proposing early media to an incoming call VoipCallUpdated //<The
	 * remote accepted the call update initiated by us
	 */

	// tml|bj*** sametime (autoanswer)
	// Runnable autoAnswer=new Runnable(){
	// public void run(){
	// Intent it = new Intent(Global.Action_AnswerCall);
	// sendBroadcast(it);
	// }
	// };

	public static String callstate_DA = null; // tml***/

	public void callState(final VoipCore p, final VoipCall call,
						  final State state, final String message) {
		callstate_DA = state.toString(); // tml***/
		// boolean switchCall = false;
		// if (AireJupiter.getInstance() != null)
		// switchCall = AireJupiter.getInstance().getSetSwitchCall(); //tml***
		// switch conf

		if (state == VoipCall.State.OutgoingInit) {
			if (incomingChatroom)
				displayStatus(null, getString(R.string.incoming_call));
			else
				displayStatus(null,
						videoCall ? getString(R.string.making_video_call)
								: getString(R.string.making_call));
			// if (!switchCall) //tml*** switch conf, 0g,1d
			enterCallMode(p);
			// alec:routeAudioToReceiver();
			// SIMON 030211
		} else if (state == VoipCall.State.OutgoingRinging) {
			// SIMON resetCameraFromPreferences();
			displayStatus(null,
					videoCall ? getString(R.string.making_video_call)
							: getString(R.string.making_call));
		} else if (state == VoipCall.State.IncomingReceived) {
			displayStatus(null,
					videoCall ? getString(R.string.incoming_video_call)
							: getString(R.string.incoming_call));
			callPending(call);
			updateLinearLayoutVisible(mAnswerSlide, View.VISIBLE);
			updateButtonVisible(mHangup2, View.VISIBLE);
			updateButtonVisible(mHold, View.GONE);
			updateButtonVisible(mHangup, View.GONE);
		} else if (state == VoipCall.State.Connected) {
			displayStatus(null, getString(R.string.call_connected));
			// Log.e("--------- SWITCH CALL --------- Connected " + switchCall);
			// if (!switchCall) { //tml*** switch conf, 0g,1d
			enterCallMode(p);

			setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);// alec
			setStreamVolume(AudioManager.STREAM_VOICE_CALL, 1.0f, 0);// alec
			// }
			mHandler.post(connectStatusHide); // tml*** beta ui

			startTime = new Date().getTime();// alec
			Connected = true;
			mHandler.postDelayed(timeElapsed, 1000);
			if (AireJupiter.getInstance() != null)
				AireJupiter.getInstance().attemptCall = false;// alec
		} else if (state == VoipCall.State.Error) {
			if (message.startsWith("Not found")
					|| message.startsWith("No response")
					|| message.startsWith("Request Timeout")) {
				if (!Connected) {
					if (AireVenus.getCallType() == AireVenus.CALLTYPE_FAFA) {
						displayStatus(null, getString(R.string.voice_mail));
						playVoiceMail();
						return;
					} else {
						displayStatus(null, getString(R.string.try_again));
						playSound(R.raw.termin);
					}
				}
			} else
				displayStatus(null, getString(R.string.call_end));

			if (!Connected) {
				if (AireVenus.instance() != null) {
					VoipCall myCall = p.getCurrentCall();
					if (myCall != null) {
						p.terminateCall(myCall);
					}
				}
				exitCallMode("callState.Error");
			}
		} else if (state == VoipCall.State.CallEnd) {
			if (message.startsWith("Call declined")) {
				displayStatus(null, getString(R.string.call_declined));
				playSound(R.raw.termin);
			} else
				displayStatus(null, getString(R.string.call_end));
			if (Connected) {
				long[] patern = { 0, 40, 1000 };
				mVibrator.vibrate(patern, -1);
			}

			// Log.e("--------- SWITCH CALL --------- CallEnd " + switchCall);
			// if (!switchCall) //tml*** switch conf, 0e,1c
			exitCallMode("callState.CallEnd");
		} else if (state == VoipCall.State.StreamsRunning) {
			mHandler.post(connectStatusHide); // tml*** beta ui
			if (AireJupiter.getInstance() != null) {
				AireJupiter.getInstance().TESTseeVolumes("Streaming");
			}

			if (!streamsRunning) {
				stopSelfPreview();

				// alec
				if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {
					mHandler.postDelayed(new Runnable() {
						public void run() {
							((Button) findViewById(R.id.add))
									.setVisibility(View.VISIBLE);
							//bree:如果不是新广播就显示video
							if (!(mPref.readInt("BCAST_CONF", -1) >= 0 && mPref.readBoolean("pay", false))) {
								((ToggleButton) findViewById(R.id.video))
										.setVisibility(View.VISIBLE); // tml***
							}

						}
					}, 2000);
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
						mHandler.postDelayed(hideControls, 5000); // tml***
						// vidconf
					}
				} else if (AireVenus.getCallType() == AireVenus.CALLTYPE_FAFA) {
					// mHandler.postDelayed(new Runnable() {
					// public void run()
					// {
					// ((Button)
					// findViewById(R.id.switchconf)).setVisibility(View.VISIBLE);
					// //tml*** switch conf
					// }
					// }, 2500);
				}

				streamsRunning = true;

				mHandler.postDelayed(checkP2PMode, 1500);

				if (call.getCurrentParamsCopy().getVideoEnabled()
						&& Version.isVideoCapable()) {
					mHandler.postDelayed(new Runnable() {
						public void run() {
							if (!VideoCallActivity.launched) {
								mHandler.postDelayed(run_spearkout, 2000);
								startVideoView(VIDEO_VIEW_ACTIVITY);
							}
						}
					}, 500);
				} else {
					mPref.write("recv_pkts", 0);
					mHandler.postDelayed(checkPkt, 3000);
					if (mPref.read("moodcontent", "--").endsWith("!!!!")) {
						mHandler.postDelayed(displayP2P, 1000);
						// mHandler.postDelayed(displayHistogram, 250);
					}
				}
				mHandler.postDelayed(checkCurrentCall, 2000); // tml*** check
				// empty call
			}
		}
	}

	Runnable run_spearkout = new Runnable() {
		public void run() {
			routeAudioToSpeaker();
		}
	};

	public void startVideoView(int requestCode) {
		stopSelfPreview();// alec
		VideoCallActivity.mAlwaysChangingPhoneAngle = -1;
		Intent lIntent = new Intent();
		lIntent.setClass(this, VideoCallActivity.class);
		lIntent.putExtra("address", mAddress);
		lIntent.putExtra("nickname", mADB.getNicknameByAddress(mAddress));
		lIntent.putExtra("contactid", cq.getContactIdByNumber(mAddress));
		startActivityForResult(lIntent, requestCode);
		launchingVideo = true;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == VIDEO_VIEW_ACTIVITY) {
			if (resultCode == RESULT_OK) {
				exitCallMode("onActivityResult,VIDEO_VIEW_ACTIVITY,OK");
				routeAudioToReceiver();

				if (mAddress.length() > 0 && AireJupiter.getInstance() != null) {
					AireJupiter.getInstance().attemptCall = false;
					AireJupiter.getInstance().terminateCallBySocket(mAddress);
					Log.e("tml onActivityResult > terminateCallBySocket");
				}
			}
		}
	}

	public static ArrayList<String> addingList = new ArrayList<String>();
	public static ArrayList<String> addingList2 = new ArrayList<String>();

	private Runnable sendNotifyForJoinChatroom = new Runnable() {
		public void run() {
			String myIdxHex = mPref.read("myID", "0");

			String ServerIP;
			if (incomingChatroom) {
				ServerIP = mPref.read("joinSipAddress",
						AireJupiter.myConfSipServer_default);
				int room = mPref.readInt("ChatroomHostIdx");
				myIdxHex = String.format("%x", room);
			} else {
				ServerIP = mPref.read("conferenceSipServer",
						AireJupiter.myConfSipServer_default);
				if (AireJupiter.getInstance() != null) {
					ServerIP = AireJupiter.getInstance().getIsoConf(ServerIP); // tml***
					// china
					// ip
				}
			}

			long ip = MyUtil.ipToLong(ServerIP);
			String HexIP = Long.toHexString(ip);

			String content;
			// boolean switchCall = false;
			// if (AireJupiter.getInstance() != null)
			// switchCall = AireJupiter.getInstance().getSetSwitchCall();
			// //tml*** switch conf
			// if (switchCall) {
			// content = Global.Call_Conference_Switch + "\n\n" + HexIP + "\n\n"
			// + myIdxHex;
			// addingList.add(Integer.toString(mADB.getIdxByAddress(mAddress)));
			// Log.e("--------- SWITCH CALL --------- 200");
			// } else {
			content = Global.Call_Conference + "\n\n" + HexIP + "\n\n"
					+ myIdxHex;
			// }

			for (int i = 0; i < addingList.size(); i++) {
				int idx = Integer.parseInt(addingList.get(i));
				if (idx < 50)
					continue;

				String address = mADB.getAddressByIdx(idx);

				if (AireJupiter.getInstance() != null
						&& AireJupiter.getInstance().tcpSocket != null
						&& address.length() > 0) {
					MyUtil.Sleep(500);
					if (AireJupiter.getInstance().tcpSocket.isLogged(false)) {
						Log.d("voip.inviteConf1 " + address + " " + content);
						AireJupiter.getInstance().tcpSocket.send(address,
								content, 0, null, null, 0, null);
					}
				}
			}
		}
	};

	private Runnable addPSTNtoJoinChatroom = new Runnable() {
		public void run() {
			String myIdxHex = mPref.read("myID", "0");
			int myIdx = Integer.parseInt(myIdxHex, 16);

			String ServerIP;
			if (incomingChatroom) {
				ServerIP = mPref.read("joinSipAddress",
						AireJupiter.myConfSipServer_default);
			} else {
				ServerIP = mPref.read("conferenceSipServer",
						AireJupiter.myConfSipServer_default);
				if (AireJupiter.getInstance() != null) {
					ServerIP = AireJupiter.getInstance().getIsoConf(ServerIP); // tml***
					// china
					// ip
				}
			}

//			String room = String.format("%07d", myIdx);

			//jack 以前的bug
			int room = mPref.readInt("ChatroomHostIdx");
			MCrypt mc = new MCrypt();

			String pass = "aireping*$857";
			try {
				pass = MCrypt.bytesToHex(mc.encrypt(pass));
			} catch (Exception e) {
				Log.e("addPSTNtoJoinChatroom1 !@#$ " + e.getMessage());
			}
			String myUsername = String.format("**%d", myIdx);
			String myPasswd = mPref.read("password", "1111");
			for (int i = 0; i < addingList2.size(); i++) {
				String address = addingList2.get(i);
				String number = MyTelephony.cleanPhoneNumber2(address);
				String globalnumber = address;

//				globalnumber = MyTelephony.addPrefixWithCurrentISO(globalnumber);

				Log.d("DialerActivity  初始化:" + globalnumber);

				MyTelephony.init(DialerActivity.this);
				if (MyTelephony.validWithCurrentISO(number)) {
//					globalnumber = MyTelephony.attachPrefix(DialerActivity.this, number);

					globalnumber = MyTelephony.addPrefixWithCurrentISO(number);
					Log.d("DialerActivity  第一个:" + globalnumber);

				}else {
					isMobileNumber = false;
					globalnumber = MyTelephony.attachFixedPrefix(
							DialerActivity.this, number);
					Log.d("DialerActivity  第一个Fixed:"+globalnumber);
				}



				if (!globalnumber.startsWith("+")) {
					globalnumber = MyTelephony.attachPrefix(
							DialerActivity.this, number);
					if (globalnumber.startsWith("+"))
						isMobileNumber = true;
					Log.d("DialerActivity  第二个:"+globalnumber);
				}

				if (!globalnumber.startsWith("+")) {
					globalnumber = MyTelephony.attachFixedPrefix(
							DialerActivity.this, number);
					if (globalnumber.startsWith("+"))
						isMobileNumber = false;
					Log.d("DialerActivity  第三个:"+globalnumber);

				}

				cIndex = MyTelephony.getCountryIndexByNumber(globalnumber, 1);
				if (cIndex == -1) { // tml*** country iso fix
					cIndex = MyTelephony.getCountryIndexByNumber(globalnumber,
							3);
					if (cIndex == -1)
						cIndex = 0;
				}
				cIso = MyTelephony.getCountryIsoByIndex(cIndex);

				Log.d("AireCall!!= " + globalnumber + " " + number + " "
						+ cIndex + " " + cIso + " " + isMobileNumber);

				long contact_id = cq.getContactIdByNumber(globalnumber);
				String displayname = globalnumber;
				if (contact_id > 0)
					displayname = cq.getNameByContactId(contact_id);
				PSTNCallLogRowId = mCLDB.insert(displayname, globalnumber,
						contact_id);

				try {
					address = MCrypt.bytesToHex(mc.encrypt(globalnumber));
				} catch (Exception e) {
					Log.e("addPSTNtoJoinChatroom2 !@#$ " + e.getMessage());
				}
				String Return = "";
				if (i > 0)
					MyUtil.Sleep(250);
				try {
					Log.i("voip.inviteConf2 " + "room=" + room + "&ip="
							+ ServerIP + "&callee=" + address + "&pass=" + pass
							+ "&user=" + myUsername + "&userpw=" + myPasswd);
					MyNet net = new MyNet(DialerActivity.this);

					// tml*** china ip
					String phpip = AireJupiter.myPhpServer_default;
					if (AireJupiter.getInstance() != null) {
						phpip = AireJupiter.getInstance().getIsoPhp(0, true,
								"74.3.165.66");
					}

					Return = net.doAnyPostHttp("http://" + phpip
									+ "/onair/conference/customer/addcallandroid.php",
							"room=" + room + "&ip=" + ServerIP + "&callee="
									+ address + "&pass=" + pass + "&user="
									+ myUsername + "&userpw=" + myPasswd);
				} catch (Exception e) {
					Log.e("addPSTNtoJoinChatroom addcallandroid.php !@#$ "
							+ e.getMessage());
				}
				Log.d("Return=" + Return);

				shouldCheckPSTNinChatroom = true;
			}

			mHandler.post(new Runnable() {
				public void run() {
					float credit = mPref.readFloat("Credit", 0);
					showCredit(credit);
					((TextView) findViewById(R.id.credit))
							.setVisibility(View.VISIBLE);
					((TextView) findViewById(R.id.country))
							.setVisibility(View.VISIBLE);
					((TextView) findViewById(R.id.country)).setText(MyTelephony
							.getCountryNameByIndex(cIndex, DialerActivity.this));
				}
			});
		}
	};

	private Runnable readChatroomMemberThread = new Runnable() {
		public void run() {
			String Return = "";
			try {
				MyNet net = new MyNet(DialerActivity.this);
				int room = mPref.readInt("ChatroomHostIdx");
				String roomNumber = String.format("%07d", room);
				String domain;
				if (incomingChatroom) {
					domain = mPref.read("joinSipAddress",
							AireJupiter.myConfSipServer_default);
				} else {
					domain = mPref.read("conferenceSipServer",
							AireJupiter.myConfSipServer_default);
					if (AireJupiter.getInstance() != null) {
						domain = AireJupiter.getInstance().getIsoConf(domain); // tml***
						// china
						// ip
					}
				}

				// tml*** china ip
				String phpip = AireJupiter.myPhpServer_default;
				if (AireJupiter.getInstance() != null) {
					phpip = AireJupiter.getInstance().getIsoPhp(0, true,
							"74.3.165.66");
				}
				//bree:如果是新的广播就换ip
				if (mPref.readInt("BCAST_CONF", -1) >= 0 && mPref.readBoolean("pay", false)) {
					Return = net.doAnyPostHttp("http://" + phpip
							+ "/onair/conference/customer/conf1.php", "room="
							+ roomNumber + "&ip=61.136.101.118");
				}else {
					Return = net.doAnyPostHttp("http://" + phpip + "/onair/conference/customer/conf1.php",
							"room="+roomNumber+"&ip="+domain);

				}

			} catch (Exception e) {
				Log.e("readChatroomMemberThread conf1.php !@#$ "
						+ e.getMessage());
			}

			// Log.d("readChatroomMemberThread Return="+Return);
			int mSize0 = memberList.size();
			if (Return.length() > 10) {
				memberList.clear();
				try {
					String[] items = Return.split(";");
					int myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
					for (int j = 0; j < items.length; j++) {
						String item = items[j];
						if (item.startsWith("sofia/internal/**")
								|| item.startsWith("sofia/external/")) {
							try {
								int idx = 0;
								if (item.contains("internal")) {
									item = item
											.substring(17, item.indexOf('@'));
									idx = Integer.parseInt(item);
									if (myIdx == idx)
									{
										myUuid=items[j+1];
										continue;
									}

								} else {
									item = item
											.substring(15, item.indexOf('@'));
									if (item.startsWith("00177")
											&& item.length() > 5)
										item = item.substring(5);
									else if (item.startsWith("00111")
											&& item.length() > 7)
										item = item.substring(7);
								}

								String uuid = items[j + 1];
								if (idx > 0) {
									String displayname = mADB
											.getNicknameByIdx(idx);
									String address = mADB.getAddressByIdx(idx);
									if (address.length() == 0)
										displayname = getResources().getString(
												R.string.unknown_person);
									// tml*** conf chat
									// create group startdate,
									// [<CONF>][<GROUP>]startdate, list
									// check if list contains nickname
									// else check getusernickname.php
									// save nickname
									// update list/group if +1

									Drawable drawable = ImageUtil
											.getBigRoundedUserPhoto(
													DialerActivity.this, idx);
									if (drawable == null)
										drawable = getResources().getDrawable(
												R.drawable.bighead);

									Map<String, Object> map = new HashMap<String, Object>();
									map.put("idx", "" + idx);
									map.put("address", address);
									map.put("displayname", displayname);
									map.put("photo", drawable);
									map.put("uuid", uuid);
									memberList.add(map);
									//bree:去除重复的好友
									memberList=MyUtil.sigleList(memberList);
								} else {

									long contactId = cq
											.getContactIdByNumber("+" + item);
									Drawable drawable = cq.getPhotoById(
											DialerActivity.this, contactId,
											false);
									if (drawable == null)
										drawable = getResources().getDrawable(
												R.drawable.bighead);

									Map<String, Object> map = new HashMap<String, Object>();
									map.put("idx", "" + idx);
									map.put("address", "+" + item);
									map.put("displayname", "+" + item);
									map.put("photo", drawable);
									map.put("uuid", uuid);
									memberList.add(map);
									//bree:去除重复的好友
									memberList=MyUtil.sigleList(memberList);
								}
							} catch (Exception e) {
								Log.e("readChatroomMemberThread2 !@#$ "
										+ e.getMessage());
							}
						}
					}
				} catch (Exception e) {
					Log.e("readChatroomMemberThread3 !@#$ " + e.getMessage());
				}

				int mSize1 = memberList.size();
				if (mSize0 != mSize1) {
					mHandler.post(showChatroomMembers);
				}
			}
		}
	};

	private Runnable showChatroomMembers = new Runnable() {
		public void run() {
			((FrameLayout) findViewById(R.id.members_view))
					.setVisibility(View.VISIBLE);
			imageAdapter.notifyDataSetChanged();
			((PhotoGallery) findViewById(R.id.members)).setSelection(memberList
					.size() - 1);
			mHandler.postDelayed(hideMembers, 2000); // tml*** vidconf
		}
	};

	private Runnable refreshChatRoomMember = new Runnable() {
		public void run() {
			new Thread(readChatroomMemberThread).start();
			mHandler.postDelayed(refreshChatRoomMember, 15000);
		}
	};

	// tml*** switch conf, 0c
	// public void runDialerStuff(String confNumber, String from) {
	// Log.e("--------- SWITCH CALL --------- dialerstuff " + from);
	// mAddress = confNumber;
	// phoneNumber = confNumber;
	// mHandler.removeCallbacks(checkCurrentCall);
	// mHandler.removeCallbacks(checkPkt);
	// mHandler.removeCallbacks(checkP2PMode);
	// mHandler.removeCallbacks(timeElapsed);
	// // mHandler.removeCallbacks(dialerStuff);
	// // mHandler.post(dialerStuff);
	// new Thread(dialerStuff).start();
	// }
	// tml*** switch conf, 0e,1e
	// public void readySwitchCall() {
	// new Thread(new Runnable() {
	// public void run() {
	// Log.e("--------- SWITCH CALL --------- switching");
	// AireVenus.setCallType(AireVenus.CALLTYPE_CHATROOM);
	//
	// int myIdx = 0;
	// try {
	// myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
	// mPref.write("ChatroomHostIdx", myIdx);
	// String dialNumber = "1007";
	// String idx = "" + myIdx;
	// for (int i = idx.length(); i < 7 ; i++)
	// dialNumber += "0";
	// dialNumber += idx;
	// MakeCall.SipCall(DialerActivity.this, dialNumber, "Conference Call",
	// false);
	// } catch (Exception e) {
	// Log.e("voip.switch conf failed");
	// if (AireJupiter.getInstance() != null)
	// AireJupiter.getInstance().setSwitchCall(false, "readySwitchCall !@#$");
	// exitCallMode("switch conf failed");
	// }
	// }
	// }).start();
	// }
	// private boolean switchingCallView = false;
	// public void switchToConf() {
	// Log.e("--------- SWITCH CALL --------- switching view");
	// switchingCallView = true;
	//
	// mHandler.post(new Runnable() {
	// public void run() {
	// mChatView.setVisibility(View.GONE);
	// mSwitchConf.setVisibility(View.GONE);
	// }
	// });
	// mHandler.postDelayed(new Runnable() {
	// public void run() {
	// ((Button) findViewById(R.id.add)).setVisibility(View.VISIBLE);
	// ((ToggleButton) findViewById(R.id.video)).setVisibility(View.VISIBLE);
	// }
	// }, 1000);
	// mHandler.postDelayed(refreshChatRoomMember, 10000);
	//
	// previousCredit = mPref.readFloat("Credit", 0);
	// ((Button) findViewById(R.id.add)).setOnClickListener(new
	// OnClickListener() {
	// public void onClick(View v) {
	// Intent it = new Intent(DialerActivity.this, AddCallDialog.class);
	// it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	// startActivityForResult(it, 200);
	// }
	// });
	//
	// if (android.os.Build.VERSION.SDK_INT >=
	// android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
	// ((LinearLayout) findViewById(R.id.topVWin_holder)).post(new Runnable () {
	// @Override
	// public void run() {
	// int titleH = ((FrameLayout)
	// findViewById(R.id.title)).getMeasuredHeight();
	// RelativeLayout.LayoutParams params =
	// (RelativeLayout.LayoutParams) ((LinearLayout)
	// findViewById(R.id.topVWin_holder)).getLayoutParams();
	// params.bottomMargin = titleH;
	// ((LinearLayout)
	// findViewById(R.id.topVWin_holder)).setLayoutParams(params);
	// }
	// });
	//
	// ((SurfaceView) findViewById(R.id.topVWin_surface)).post(new Runnable () {
	// @Override
	// public void run() {
	// int titleH = ((FrameLayout)
	// findViewById(R.id.title)).getMeasuredHeight();
	// RelativeLayout.LayoutParams params =
	// (RelativeLayout.LayoutParams) ((SurfaceView)
	// findViewById(R.id.topVWin_surface)).getLayoutParams();
	// params.bottomMargin = titleH;
	// ((SurfaceView)
	// findViewById(R.id.topVWin_surface)).setLayoutParams(params);
	// }
	// });
	//
	// ((FrameLayout) findViewById(R.id.title)).setOnClickListener(new
	// OnClickListener() {
	// @Override
	// public void onClick(View v) {
	// if (((ImageView) findViewById(R.id.controls_expand)).getVisibility() !=
	// View.VISIBLE) {
	// mHandler.removeCallbacks(showControls);
	// mHandler.removeCallbacks(hideControls);
	// mHandler.postDelayed(hideControls, 300);
	// mHandler.removeCallbacks(showMembers);
	// mHandler.removeCallbacks(hideMembers);
	// mHandler.postDelayed(hideMembers, 300);
	// } else {
	// mHandler.postDelayed(showControls, 300);
	// mHandler.postDelayed(hideControls, 6000);
	// mHandler.postDelayed(showMembers, 300);
	// mHandler.postDelayed(hideMembers, 10000);
	// }
	// }
	// });
	//
	// mHandler.postDelayed(hideControls, 5000);
	// }
	//
	// ((ToggleButton) findViewById(R.id.video)).setOnCheckedChangeListener(new
	// OnCheckedChangeListener() {
	// @Override
	// public void onCheckedChanged(CompoundButton buttonView, boolean
	// isChecked) {
	// if (android.os.Build.VERSION.SDK_INT >=
	// android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
	// if (isChecked) {
	// initVidConf(false);
	// mHandler.removeCallbacks(showControls);
	// mHandler.removeCallbacks(hideControls);
	// mHandler.postDelayed(hideControls, 3000);
	// mHandler.removeCallbacks(showMembers);
	// mHandler.removeCallbacks(hideMembers);
	// mHandler.postDelayed(hideMembers, 3000);
	// } else {
	// endVidConf();
	// mHandler.postDelayed(showMembers, 300);
	// }
	// } else {
	// if (isChecked) {
	// Toast tst = Toast.makeText(theDialer,
	// getResources().getString(R.string.vidconf_sdk), Toast.LENGTH_SHORT);
	// tst.show();
	// buttonView.setChecked(false);
	// }
	// }
	// }
	// });
	//
	// imageAdapter = new ImageAdapter(DialerActivity.this);
	// PhotoGallery galley = (PhotoGallery)findViewById(R.id.members);
	// if (galley != null) {
	// galley.setAdapter(imageAdapter);
	// galley.setLongClickable(true);
	// galley.setOnItemLongClickListener(onRemoveUserLongClick);
	// }
	//
	// switchingCallView = false;
	// Log.e("--------- SWITCH CALL --------- switch view done");
	// }
	// ***tml

	void playSound(int resource) {
		if (!MyUtil.CheckServiceExists(DialerActivity.this,
				"com.pingshow.amper.PlayService")) {
			Intent intent1 = new Intent(DialerActivity.this, PlayService.class);
			intent1.putExtra("soundInCall", resource);
			intent1.putExtra("type", 3);
			startService(intent1);
		}
	}

	void playVoiceMail() {
		if (!MyUtil.CheckServiceExists(DialerActivity.this,
				"com.pingshow.amper.PlayService")) {
			Intent intent1 = new Intent(DialerActivity.this, PlayService.class);

			String lang = Locale.getDefault().getLanguage();
			int c = R.raw.voicemail_en;
			final String langs[] = { "en_GB", "en", "zh_CN", "zh", "ko", "ja",
					"ar", "fr", "de", "es", "pt", "ru", "tr" };
			final int res[] = { R.raw.voicemail_uk, R.raw.voicemail_en,
					R.raw.voicemail_cn, R.raw.voicemail_zh, R.raw.voicemail_ko,
					R.raw.voicemail_ja, R.raw.voicemail_ar, R.raw.voicemail_fr,
					R.raw.voicemail_de, R.raw.voicemail_es, R.raw.voicemail_pt,
					R.raw.voicemail_ru, R.raw.voicemail_tr };
			for (int i = 0; i < langs.length; i++) {
				if (lang.startsWith(langs[i])) {
					c = res[i];
					break;
				}
			}

			intent1.putExtra("soundInCall", c);
			intent1.putExtra("type", 3);
			startService(intent1);
		}
	}

	public void show(VoipCore p) {
		// TODO Auto-generated method stub
	}

	@SuppressWarnings("deprecation")
	private void enterCallMode(VoipCore p) {
		mAnswerSlide.setVisibility(View.GONE);
		mHangup2.setVisibility(View.GONE); // tml*** beta ui
		mHold.setVisibility(View.VISIBLE);
		mHangup.setVisibility(View.VISIBLE);
		/*
		 * VoipAddress remote=p.getRemoteAddress(); if (remote!=null){ //TO DO }
		 */

		configureMuteAndSpeakerButtons();

		// alec: test2
		if (htc4g) { // simon 061811
			routeAudioToSpeaker();
			MyUtil.Sleep(500);
		} else {
			if (mSpeaker.isChecked()) {
				routeAudioToSpeaker();
			} else {
				if (!mAudioManager.isWiredHeadsetOn()) { // tml*** earphone
					// routing
					routeAudioToReceiver();
				}
			}
		}

		setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
		setStreamVolume(AudioManager.STREAM_VOICE_CALL, 1.0f, 0); // alec
	}

	// alec
	public void responseCallBusy() {
		Log.e("begin! responseCallBusy");
		HangingUp = true;

		// displayStatus(null,getString(R.string.call_declined));
		mHandler.postDelayed(new Runnable() {
			public void run() {
				displayStatus(null, getString(R.string.call_declined));
			}
		}, 1000);

		mHandler.postDelayed(new Runnable() {
			public void run() {
				if (!Connected) {
					Log.e("responseCallBusy > !Connected");
					if (AireVenus.instance() != null) {
						VoipCore lVoipCore = AireVenus.instance().getVoipCore();
						VoipCall myCall = lVoipCore.getCurrentCall();
						if (myCall != null) {
							lVoipCore.terminateCall(myCall);
							Log.e("responseCallBusy > lVoipCore.terminateCall(myCall)");
						} else {
							Log.e("responseCallBusy > myCall == null");
						}
					}
				} else {
					Log.e("responseCallBusy > Connected");
				}

				if (!MyUtil.CheckServiceExists(DialerActivity.this,
						"com.pingshow.amper.PlayService")) {
					Intent intent1 = new Intent(DialerActivity.this,
							PlayService.class);
					intent1.putExtra("soundInCall", R.raw.termin);
					startService(intent1);
				}
				exitCallMode("responseCallBusy");
			}
		}, 2000);
	}

	Runnable run_mute = new Runnable() {
		public void run() {
			try {
				AireVenus y;
				if ((y = AireVenus.instance()) != null) {
					mMute.setChecked(y.getVoipCore().isMicMuted());
					if (mAudioManager.isSpeakerphoneOn())
						mSpeaker.setChecked(true);
					else
						mSpeaker.setChecked(false);
					mHold.setChecked(y.getVoipCore().isSpeakerMuted() == 1
							&& y.getVoipCore().isMicMuted());
				}
			} catch (Exception e) {
				Log.e("run_mute !@#$ " + e.getMessage());
			}
		}
	};

	private void configureMuteAndSpeakerButtons() {
		// alec: this might cause galaxy pad crash
		mHandler.post(run_mute);
	}

	Runnable run_finish = new Runnable() {
		public void run() {
			Log.e("dialer activity run_finish!!!");
			// tml*** resume dialer unstuck
			Intent runf = new Intent();
			runf.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			runf.setClass(DialerActivity.this, UsersActivity.class);
			startActivity(runf);
			finish();
		}
	};

	public void exitCallMode2(String from) {
		if (AireVenus.instance() != null) {
			VoipCore lVoipCore = AireVenus.instance().getVoipCore();
			VoipCall myCall = lVoipCore.getCurrentCall();
			if (myCall != null) {
				lVoipCore.terminateCall(myCall);
				Log.d("exit.exitCallMode2");
			}
		}
		exitCallMode(from);
	}

	private void exitCallMode(String from) {
		exitCallMode(false, from);
	}

	private void exitCallMode(boolean finishing, String from) {
		try {
			Log.e("voip.exitCallMode fin=" + finishing + " (" + from + ")");
			mHandler.removeCallbacks(timeElapsed);
			// amInCall = false; //tml*** incall rot/
			mPref.write("way", false);
			mPref.write("tempCheckSameIN", 0); // tml*** sametime
			// alec:routeAudioToReceiver();

			if (mSensorManager != null)
				mSensorManager.unregisterListener(this);// wanghuan

			if (AireVenus.instance() != null)
				AireVenus.instance().callStopRing();
			stopRing();

			if (!finishing)
				mHandler.postDelayed(run_finish, 1500);// alec: to delay 1500ms
			// for user to know what
			// happened

			if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM
					&& !incomingChatroom) {
				new Thread(kickOutAllMembers).start();
			}

			updateCallLog();

			imCalling = false;
			// finish(); //simon 062311
			// if(mAudioManager.isMusicActive()) //tml*** mute error, CX
			// mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
			controlBkgndMusic(2);
			tMgr.listen(phoneListener, MyPhoneStateListener.LISTEN_NONE);
		} catch (Exception e) {
			Log.e("exitCallMode !@#$ " + e.getMessage());
		}
		// Jerry, 031214, On hangup, reset MySocket.BufferedIdxForCall to 0
		Log.d("DialerActivity in exitCallMode, MySocket.BufferedIdxForCall set to 0 on HangingUp...");
		MySocket.BufferedIdxForCall = 0;
	}

	boolean cancelSpeaker = false;

	@SuppressWarnings("deprecation")
	public void routeAudioToSpeaker() {
		Log.d("routeAudioToSpeaker!!!");

		if (BluetoothSco || isKindle)
			return;

		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		if (htc_oneS)// yangjun
		{
			am.setMode(AudioManager.MODE_IN_CALL);
			am.setSpeakerphoneOn(true);
		} else if (bGalaxyNexus)// alec
		{
			am.setMode(AudioManager.MODE_IN_CALL);
			am.setMode(AudioManager.MODE_NORMAL);
			am.setSpeakerphoneOn(true);
		} else if (zteU950) {
			am.setMode(AudioManager.MODE_NORMAL);
			am.setRouting(AudioManager.MODE_NORMAL, AudioManager.ROUTE_SPEAKER,
					AudioManager.ROUTE_ALL);
			am.setSpeakerphoneOn(true);
		} else if (bSamsung || zxZTE) {
			if (bSamsungS3) {
				am.setSpeakerphoneOn(true);
			} else if (bSamsungi997) {
				am.setSpeakerphoneOn(true);
				am.setMode(AudioManager.MODE_NORMAL);
			} else if (bPD1000) {
				am.setSpeakerphoneOn(true);
				am.setMode(AudioManager.MODE_IN_CALL);
				am.setMode(AudioManager.MODE_NORMAL);
			} else if (bSamsungi9100) {
				am.setSpeakerphoneOn(true);
			} else {
				am.setMode(AudioManager.MODE_IN_CALL);
				am.setSpeakerphoneOn(true);
			}
		} else if (NexusS || Google40 || SonyRK30sdk) {
			am.setSpeakerphoneOn(true);
		} else if (bMotorola || htc4g) {
			am.setMode(AudioManager.MODE_NORMAL);
		} else if (HTCG12 || htc_evo || HTCSensation) {
			am.setMode(AudioManager.MODE_NORMAL);
			am.setSpeakerphoneOn(true);
		} else if (HTC_6400 || Moto860) {
			am.setMode(AudioManager.MODE_NORMAL);
			am.setSpeakerphoneOn(true);
		} else {
			cancelSpeaker = true;
			am.setSpeakerphoneOn(true);
		}

		if (AireVenus.instance() != null) {
			VoipCore lVoipCore = AireVenus.instance().getVoipCore();
			if (lVoipCore.isIncall()) {
				lVoipCore.enableSpeaker(true);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void routeAudioToReceiver() {
		if (BluetoothSco || isKindle)
			return;
		Log.d("routeAudioToReceiver!!!");

		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		way = mPref.readBoolean("way"); // when hangup ,way equals false
		// ;outcall and recv incall is true

		if (bGalaxyNexus) {
			am.setMode(AudioManager.MODE_IN_CALL);
			am.setMode(AudioManager.MODE_NORMAL);
			am.setSpeakerphoneOn(false);
		} else if (htc_oneS)// yangjun
		{
			am.setMode(AudioManager.MODE_IN_CALL);
			am.setSpeakerphoneOn(false);
		} else if (htc_evo || HTCSensation || SonyRK30sdk) {
			am.setSpeakerphoneOn(false);
			// am.setMode(AudioManager.MODE_NORMAL);
		} else if (zteU950) {
			am.setMode(AudioManager.MODE_NORMAL);
			am.setRouting(AudioManager.MODE_NORMAL,
					AudioManager.ROUTE_EARPIECE, AudioManager.ROUTE_ALL);
			am.setSpeakerphoneOn(false);
		} else if (bSamsung) {
			if (bSamsungS3) {
				am.setSpeakerphoneOn(false);
			} else if (way) {
				if (bSamsungi997) {
					am.setMode(AudioManager.MODE_NORMAL);
					am.setSpeakerphoneOn(false);
				} else {
					am.setMode(AudioManager.MODE_IN_CALL);
					am.setMode(AudioManager.MODE_NORMAL);
					am.setSpeakerphoneOn(false);
				}
			} else
				am.setMode(AudioManager.MODE_NORMAL);
		} else if (NexusS || Google40 || filemate1) {
			Log.e("routeAudioToReceiver NexusS|Google40|filemate1 way=" + way);
			if (way) {
				if (filemate1) { // tml*** audio routing, filemate/tablet
					if (am.isSpeakerphoneOn())
						am.setSpeakerphoneOn(false);
					return;
				} else {
					// am.setMode(AudioManager.MODE_NORMAL); //tml*** ringtone
					// vol, X
					am.setSpeakerphoneOn(false);
				}
			} else {
				am.setMode(AudioManager.MODE_NORMAL);
			}
		} else if (bMotorola || htc4g) {
			am.setSpeakerphoneOn(false);
			if (way) {
				am.setMode(AudioManager.MODE_IN_CALL);
			} else {
				am.setRouting(AudioManager.MODE_NORMAL,
						AudioManager.ROUTE_EARPIECE, AudioManager.ROUTE_ALL);
				am.setMode(AudioManager.MODE_NORMAL);
			}
		} else if (zxZTE) {
			am.setSpeakerphoneOn(false);
			if (!way) {
				am.setMode(AudioManager.MODE_IN_CALL);
				am.setMode(AudioManager.MODE_NORMAL);
			} else {
				am.setMode(AudioManager.MODE_IN_CALL);
				am.setSpeakerphoneOn(false);
			}
		} else if (HTCA6390) {
			if (!cancelSpeaker)
				am.setSpeakerphoneOn(false);
			else {
				am.setMode(AudioManager.MODE_IN_CALL);
				am.setSpeakerphoneOn(false);
				cancelSpeaker = false;
			}
		} else if (HTCG11) {
			am.setMode(AudioManager.MODE_IN_CALL);
			am.setMode(AudioManager.MODE_NORMAL);
		} else if (HTC_6400 || Moto860) {
			am.setMode(AudioManager.MODE_IN_CALL);
			am.setMode(AudioManager.MODE_NORMAL);
			am.setSpeakerphoneOn(false);
		} else {
			am.setSpeakerphoneOn(false);
		}

		if (AireVenus.instance() != null) {
			VoipCore lVoipCore = AireVenus.instance().getVoipCore();
			if (lVoipCore.isIncall()) {
				lVoipCore.enableSpeaker(false);
			}
		}
	}

	void setStreamVolume(final int stream, final float rate, final int flags) {
		int vol = (int) (rate * mAudioManager.getStreamMaxVolume(stream));
		mAudioManager.setStreamVolume(stream, vol, flags);
	}

	private void callPending(VoipCall call) {
		// Privacy setting to not share the user camera by default
		/***
		 * Simon 030811 Disable to whole video stuff for now boolean
		 * prefVideoEnable =
		 * mPref.readBoolean(getString(R.string.pref_video_enable_key)); boolean
		 * prefAutomaticallyShareMyCamera =
		 * mPref.readBoolean(getString(R.string.
		 * pref_video_automatically_share_my_video_key));
		 * getVideoManager().setMuted(!(prefVideoEnable &&
		 * prefAutomaticallyShareMyCamera));
		 * call.enableCamera(prefAutomaticallyShareMyCamera); Simon
		 */
	}

	private String getYourSipServerByTCP(String address) {
		AireJupiter x;
		if ((x = AireJupiter.getInstance()) != null) {
			String sipIP = x.getYourSipServer(address);
			if (sipIP != null && !sipIP.equals(x.mySipServer)) // simon 061811
			{
				address += "@" + sipIP;
				Log.d("voip.(getYourSipServer) Callee2: " + address);
			} else if (sipIP == null) {
				Log.d("voip.(getYourSipServer) Callee0: " + address + "@null");
			} else {
				Log.d("voip.(getYourSipServer) Callee1: " + address + "@same");
			}
		}
		return address;
	}

	public void newOutgoingCall(String address, boolean withVideo, String from) {
		Log.d("newOutgoingCall1 to " + address + " (" + from + ")");
		imCalling = true;
		VoipCore lVoipCore = null;
		if (AireVenus.instance() != null) {
			lVoipCore = AireVenus.instance().getVoipCore();
			Log.d("AireVenus is good");
			if (lVoipCore.isIncall()) {
				Log.e("is BUSY INcall");
				return;
			}
		} else {
			Log.d("AireVenus is bad");
		}
		if (AireVenus.runAsSipAccount && address.startsWith("+")) {
			String SipServer = mPref.read("aireSipServer", "192.168.0.1");
			if (AireVenus.getCallType() == AireVenus.CALLTYPE_AIRECALL) {
				address = address.substring(1);
				shouldConsumeCredit = true;
			} else if (AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM
					&& address.startsWith("+"))
				address = address.substring(1);
			else if (SipServer.equals("218.104.51.41")
					&& address.startsWith("+86"))
				address = address.substring(3);
			else if (SipServer.equals("54.249.19.120"))
				address = address.substring(1);
			else if (SipServer.equals("50.112.137.243"))
				address = address.substring(1);
		}

		Log.d("newOutgoingCall2 runAsSipAccount=" + AireVenus.runAsSipAccount
				+ ",  address=" + address);
		VoipAddress lAddress;
		try {
			lAddress = lVoipCore.interpretUrl(address);
		} catch (VoipCoreException e) {
			Log.e("interpretUrl VoipCoreException ???");
			return;
		}
		lAddress.setDisplayName(mDisplayName);

		try {
			// Log.e("--------- SWITCH CALL --------- newOutgoingCall " +
			// address);
			Log.i("*** newOutgoingCall3 Calling: " + mDisplayName + " "
					+ address + " vid:" + withVideo);
			CallManager.getInstance().inviteAddress(lAddress, withVideo);
		} catch (VoipCoreException e) {
			Log.e("inviteAddress VoipCoreException withVideo=" + withVideo);
			return;
		}
	}

	public void initFromConf() throws VoipException {
		try {
			AireVenus.instance().initFromConf();
		} catch (VoipConfigException e) {
			Log.e("DA initFromConf !@#$ " + e.getMessage());
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// nop
	}

	protected void hideScreen(boolean isHidden) {
		if (isHidden) {
			if (((FrameLayout) findViewById(R.id.keypad_panel)).getVisibility() == View.VISIBLE)
				return;
			mMainFrame.setVisibility(View.INVISIBLE);
		} else {
			mMainFrame.setVisibility(View.VISIBLE);
		}
	}

	long enabletime;
	KeyguardManager mKeyguardManager;
	@SuppressWarnings("deprecation")
	KeyguardManager.KeyguardLock mKeyguardLock;
	boolean enabled;

	@SuppressWarnings("deprecation")
	void disableKeyguard() {
		if (AmazonKindle.IsKindle())
			return;
		if (mKeyguardManager == null) {
			mKeyguardManager = (KeyguardManager) this
					.getSystemService(Context.KEYGUARD_SERVICE);
			mKeyguardLock = mKeyguardManager.newKeyguardLock("FafaYou");
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
		if (AmazonKindle.IsKindle())
			return;
		if (!enabled) {
			mKeyguardLock.reenableKeyguard();
			enabled = true;
		}
	}

	private boolean mUseBackKey = true;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d("tml onKeyDown> " + keyCode);
		if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP
				|| keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_POWER)
				&& event.getRepeatCount() == 0 && !Connected && incomingCall) {
			if (AireVenus.instance() != null) {
				// AireVenus.instance().stopRinging();
				AireVenus.instance().stopRing(); // tml*** new ring
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_HOME) {
			Log.d("tml KEYCODE_HOME");
			return true;
		}
		// tml*** backpress safety
		else if (keyCode == KeyEvent.KEYCODE_BACK && mUseBackKey) {
			Toast.makeText(DialerActivity.this, "Press back again to quit.",
					Toast.LENGTH_SHORT).show();
			mUseBackKey = false;
			return false;
		} else if (keyCode == KeyEvent.KEYCODE_BACK && !mUseBackKey) {
			// if (AireJupiter.getInstance() != null)
			// AireJupiter.getInstance().setSwitchCall(false, "onbackpress");
			// //tml*** switch conf
			mUseBackKey = true;
		}
		// ***tml
		// return false;
		return super.onKeyDown(keyCode, event);
	}

	@SuppressWarnings("deprecation")
	private void showNotification() {
		Notification notification = new Notification(R.drawable.icon_incall,
				getString(R.string.app_name), System.currentTimeMillis());

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, DialerActivity.class),
				PendingIntent.FLAG_UPDATE_CURRENT);

		notification.setLatestEventInfo(this, getString(R.string.app_name),
				getString(R.string.in_call), contentIntent);

		notification.defaults = 0;
		notification.flags = Notification.FLAG_ONGOING_EVENT;

		NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNM.notify(R.string.call, notification);
	}

	void InitDtmfKeyTone() {
		final int keyarray[] = { R.id.key0, R.id.key1, R.id.key2, R.id.key3,
				R.id.key4, R.id.key5, R.id.key6, R.id.key7, R.id.key8,
				R.id.key9 };
		ImageView key;
		for (int i = 0; i < 10; i++) {
			key = (ImageView) findViewById(keyarray[i]);
			setDigitListener(key, Character.forDigit(i, 10));
		}
		key = (ImageView) findViewById(R.id.keyStar);
		setDigitListener(key, '*');
		key = (ImageView) findViewById(R.id.keyHash);
		setDigitListener(key, '#');
	}

	private void setDigitListener(ImageView view, char dtmf) {
		class DialKeyListener implements OnTouchListener {
			final char mKeyCode;
			boolean mIsDtmfStarted = false;

			DialKeyListener(char aKeyCode) {
				mKeyCode = aKeyCode;
			}

			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN && Connected
						&& mIsDtmfStarted == false) {
					VoipCore p = AireVenus.instance().getVoipCore();
					if (p != null) {
						// p.playDtmf(mKeyCode, -1);
						mIsDtmfStarted = true;
						if (p.isIncall()) {
							p.sendDtmf(mKeyCode);
							DTMFString += mKeyCode;
							mDisplayNameView.setText(DTMFString);
						}

						if (mKeyCode == '*')
							tg.startTone(ToneGenerator.TONE_DTMF_S, 150);
						else if (mKeyCode == '#')
							tg.startTone(ToneGenerator.TONE_DTMF_S, 150);
						else
							tg.startTone(ToneGenerator.TONE_DTMF_0
									+ (mKeyCode - '0'), 150);
					}
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					stopDtmf();
				}
				return false;
			}

			private void stopDtmf() {
				/*
				 * VoipCore p = AireVenus.instance().getVoipCore(); if (p!=null)
				 * p.stopDtmf();
				 */
				mIsDtmfStarted = false;
			}
		}
		;
		DialKeyListener lListener = new DialKeyListener(dtmf);
		view.setOnTouchListener(lListener);
	}

	public void endUpDialer(String address) {
		if (AireVenus.instance() != null) {
			VoipCore lVoipCore = AireVenus.instance().getVoipCore();
			if (lVoipCore != null && lVoipCore.isIncall())// alec
			{
				VoipCall myCall = lVoipCore.getCurrentCall();
				if (myCall != null) {
					String remoteNumber = myCall.getRemoteAddress()
							.getUserName();
					if (remoteNumber.equals(address)) {
						Log.d("terminateCall");
						lVoipCore.terminateCall(myCall);
						sendTerminateSignal = false;
						return;// alec
					}
				}
			}
		}
		exitCallMode("endUpDialer");
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (videoCall)
			return;
		if (event.values[0] != event.sensor.getMaximumRange()
				&& event.values[0] <= 3.0f)// alec fix milestone
			hideScreen(true);
		else
			hideScreen(false);
	}

	private Handler handlerHold = null;

	private class MyPhoneStateListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			switch (state) {
				case TelephonyManager.CALL_STATE_OFFHOOK: // incall get through
					sysIncomingNumber = incomingNumber;
					Log.d("hold Aire call");
					handlerHold.sendEmptyMessage(0);
					break;
				case TelephonyManager.CALL_STATE_RINGING:
					break;
				default:
					break;
			}
			super.onCallStateChanged(state, incomingNumber);
		}
	}

	private Camera mCamera = null;

	@SuppressLint("NewApi")
	private void startSelfPreview(int CameraId, SurfaceView surf, int rotation) {
		try {
			if (mCamera == null) {
				mCamera = Camera.open(CameraId);
				mCamera.setPreviewDisplay(surf.getHolder());
				mCamera.setPreviewCallback(null);
				mCamera.setDisplayOrientation(rotation);
				mCamera.startPreview();
			}
		} catch (Exception exc) {
			Log.e("startSelfPreview1 !@#$ " + exc.getMessage());
		} catch (NoSuchMethodError e) {
			Log.e("startSelfPreview2 !@#$ " + e.getMessage());
		}
	}

	private void stopSelfPreview() {
		if (mCamera == null)
			return;
		try {
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
			// handler.post(new Runnable(){
			// public void run()
			// {
			// try{
			// FrameLayout f=(FrameLayout)findViewById(R.id.selfview);
			// f.setVisibility(View.GONE);
			// mSurface.setVisibility(View.GONE);
			// }catch(Exception e) {
			// Log.e("da31 " + e.getMessage());
			// }
			// }
			// });
		} catch (Exception exc) {
			Log.e("stopSelfPreview1 !@#$ " + exc.getMessage());
		} catch (NoSuchMethodError e) {
			Log.e("stopSelfPreview2 !@#$ " + e.getMessage());
		}
	}

	public static List<Map<String, Object>> memberList = new ArrayList<Map<String, Object>>();
	private ImageAdapter imageAdapter;

	public static class Holder {
		public static ImageView iv = null;
		public static TextView tv = null;
	}

	class ImageAdapter extends BaseAdapter {
		private Context context = null;

		public ImageAdapter(Context context) {
			super();
			this.context = context;
		}

		@Override
		public int getCount() {
			return memberList.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = View.inflate(context, R.layout.gallery_items,
						null);
			}
			Holder.iv = (ImageView) convertView
					.findViewById(R.id.gallery_imageview);
			Holder.tv = (TextView) convertView
					.findViewById(R.id.gallery_textview);

			String displayname = (String) memberList.get(position).get(
					"displayname");
			Holder.tv.setText(displayname);
			Holder.iv.setImageDrawable((Drawable) memberList.get(position).get(
					"photo"));
			Holder.iv.setBackgroundResource(R.drawable.empty);

			return convertView;
		}
	}

	int surfaceAngel = 90;

	@SuppressLint("NewApi")
	public void lockScreenOrientation(int mode) {
		if (mode == 1) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else if (mode == 2) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		}
		if (Build.VERSION.SDK_INT < 8) {
			surfaceAngel = 90;
			return;
		}
		try {
			switch (((WindowManager) getSystemService(WINDOW_SERVICE))
					.getDefaultDisplay().getRotation()) {
				case Surface.ROTATION_90:
					surfaceAngel = 0;
					break;
				case Surface.ROTATION_180:
					surfaceAngel = 270;
					break;
				case Surface.ROTATION_270:
					surfaceAngel = 180;
					break;
				default:
					surfaceAngel = 90;
			}
		} catch (Exception e) {
			Log.e("lockScreenOrientation !@#$ " + e.getMessage());
		}
	}

	private int displayP2Pcount = 0;
	private int pktssent0 = 0, pktsrecv0 = 0;
	int pktssentavg = 0, pktsrecvavg = 0;
	Runnable displayP2P = new Runnable() {
		public void run() {
			VoipCore lc = AireVenus.getLc();
			if (lc != null && lc.isIncall()) {
				displayP2Pcount++;
				try {
					int status = lc.getStatus();
					ImageView iv = (ImageView) findViewById(R.id.ind0);
					if ((status & 0xF00) == 0x100)
						iv.setImageResource(R.drawable.purple);// amr
					else if ((status & 0xF00) == 0x200)
						iv.setImageResource(R.drawable.red);// speex
					else if ((status & 0xF00) == 0x300)
						iv.setImageResource(R.drawable.blue);// PCMA
					else if ((status & 0xF00) == 0x400)
						iv.setImageResource(R.drawable.teal);// PCMU
					else if ((status & 0xF00) == 0x600)
						iv.setImageResource(R.drawable.orange);// OPUS
					else
						iv.setImageResource(R.drawable.gray);

					iv = (ImageView) findViewById(R.id.ind1);
					if (lc.isRunningP2P())
						iv.setImageResource(R.drawable.orange);
					else
						iv.setImageResource(R.drawable.yellow);

					iv = (ImageView) findViewById(R.id.ind2);
					int send = status & 0xF;
					if (send == 0x5)
						iv.setImageResource(R.drawable.red);
					else if (send == 0x4)
						iv.setImageResource(R.drawable.green);
					else if (send == 0x1)
						iv.setImageResource(R.drawable.teal);
					else
						iv.setImageResource(R.drawable.gray);

					iv = (ImageView) findViewById(R.id.ind3);
					int recv = status & 0xF0;
					if (recv == 0x50)
						iv.setImageResource(R.drawable.red);
					else if (recv == 0x40)
						iv.setImageResource(R.drawable.blue);
					else if (recv == 0x10)
						iv.setImageResource(R.drawable.purple);
					else
						iv.setImageResource(R.drawable.gray);

					((LinearLayout) findViewById(R.id.status))
							.setVisibility(View.VISIBLE);

					int[] ports = lc.getPorts();
					if (displayP2Pcount < 300) {
						pktssentavg = ports[10] / displayP2Pcount;
						pktsrecvavg = ports[11] / displayP2Pcount;
					}
					String info = "relay local:" + ports[0] + " v:" + ports[1]
							+ "\nrelay remote:" + ports[2] + " v:" + ports[3]
							+ "\nice local:" + ports[4] + " v:" + ports[5]
							+ "\nice remote:" + ports[6] + " (" + ports[7]
							+ ") v:" + ports[8] + " (" + ports[9] + ")"
							+ "\npkts sent:" + ports[10] + "+"
							+ (ports[10] - pktssent0) + "/" + pktssentavg
							+ " (mic:" + ports[16] + " ec:" + ports[17]
							+ " enc:" + ports[15] + ")\npkts recv:" + ports[11]
							+ "+" + (ports[11] - pktsrecv0) + "/" + pktsrecvavg
							+ " (ice:" + ports[12] + ")" + "\nnAPPSent="
							+ ports[18] + " nAPPRecvd=" + ports[13]
							+ "\npcktinQ:" + ports[14] + "\nnTicker:"
							+ ports[19];
					pktssent0 = ports[10];
					pktsrecv0 = ports[11];
					/*
					 * int pkts_pre=mPref.readInt("recv_pkts", 0); if
					 * (ports[11]<=pkts_pre) {
					 * Toast.makeText(DialerActivity.this,
					 * "Your network is poor,please check your network connection!"
					 * , 0).show(); } mPref.write("recv_pkts", ports[11]);
					 */
					((TextView) findViewById(R.id.debuginfo)).setText(info);
					((FrameLayout) findViewById(R.id.debug))
							.setVisibility(View.VISIBLE);
				} catch (Exception e) {
					Log.e("displayP2P !@#$ " + e.getMessage());
				}

				mHandler.postDelayed(displayP2P, 1000);
			}
		}
	};
	// tml*** check empty call
	// voiplib also does this, but at 15s
	private int emptyCall = 0;
	private int prevRecvpkts = 0;
	Runnable checkCurrentCall = new Runnable() {
		@Override
		public void run() {
			int emptyLimit = 30;
			VoipCore lc = AireVenus.getLc();
			if (lc != null && lc.isIncall()) {
				int[] ports = lc.getPorts();
				if (ports != null && ports.length > 11) {
					int recvpkts = ports[11];
					if ((recvpkts - prevRecvpkts) == 0) {
						emptyCall++;
						Log.e("voip.myCall is empty/broken " + emptyCall + "/"
								+ emptyLimit);
					} else {
						emptyCall = 0;
					}
					prevRecvpkts = recvpkts;
				}
			}

			if (emptyCall == emptyLimit) {
				VoipCore lVoipCore = AireVenus.instance().getVoipCore();
				VoipCall myCall = lVoipCore.getCurrentCall();
				Log.e("voip.myCall was empty/broken!!!");
				if (myCall != null) {
					lVoipCore.terminateCall(myCall);
				} else {
					if (VideoCallActivity.getInstance() != null) {
						VideoCallActivity.getInstance().bye();
					}
					finish();
				}
				Intent it = new Intent(Global.Action_InternalCMD);
				it.putExtra("Command", Global.CMD_CONNECTION_POOR);
				it.putExtra("ForcePoor", true);
				it.putExtra("warnFrom", "callbroken");
				sendBroadcast(it);
			}

			mHandler.postDelayed(checkCurrentCall, 1000);
		}
	};

	Runnable checkPkt = new Runnable() {
		public void run() {
			VoipCore lc = AireVenus.getLc();
			if (lc != null && lc.isIncall()) {
				try {
					int[] ports = lc.getPorts();

					int pkts_pre = mPref.readInt("recv_pkts", 0);
					if (ports[11] <= pkts_pre) {
						Log.e("da.checkPkt>> " + ports[11] + "<=?" + pkts_pre);
						if (mToast != null) {
							mToast.setText(getString(R.string.network_prompt));
						} else {
							mToast = Toast.makeText(getApplicationContext(),
									getString(R.string.network_prompt), 0);
						}
						// tml*** conn notify
						mToast.setDuration(Toast.LENGTH_SHORT);
						mToast.setGravity(Gravity.TOP, 0, 0);
						// ***tml
						mToast.show();
					}
					mPref.write("recv_pkts", ports[11]);

				} catch (Exception e) {
					Log.e("checkPkt !@#$ " + e.getMessage());
				}

				mHandler.postDelayed(checkPkt, 2000);
			}
		}
	};

	private boolean isRunningP2P = true;
	Runnable checkP2PMode = new Runnable() {
		public void run() {
			try {
				VoipCore lc = AireVenus.getLc();
				if (lc != null && lc.isIncall()) {
					isRunningP2P = lc.isRunningP2P();
				}
			} catch (Exception e) {
				Log.e("checkP2PMode !@#$ " + e.getMessage());
			}
		}
	};

	Runnable displayHistogram = new Runnable() {
		public void run() {
			VoipCore lc = AireVenus.getLc();
			if (lc != null && lc.isIncall()) {
				try {
					byte[] data = new byte[360];
					lc.copyHistogram(data);
					((HistogramView) findViewById(R.id.history)).feedData(data);
				} catch (Exception e) {
					Log.e("displayHistogram !@#$ " + e.getMessage());
				}
				mHandler.postDelayed(displayHistogram, 250);
			}
		}
	};

	// private MediaPlayer mRingerPlayer;
	// private Timer mRingLimit;
	// private void startRinging_old() { //conf
	// try {
	// mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	// if (mVibrator !=null) {
	// long[] patern = {0,1000,1000};
	// mVibrator.vibrate(patern, 1);
	// }
	// // if (mRingerPlayer == null) {
	// // mRingerPlayer = new MediaPlayer();
	// // try{
	// // mRingerPlayer.setAudioStreamType(AudioManager.STREAM_RING);
	// // mRingerPlayer.setDataSource(getApplicationContext(),
	// RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
	// // mRingerPlayer.prepare();
	// // mRingerPlayer.setLooping(true);
	// // mRingerPlayer.start();
	// // }catch (Exception e){
	// // Log.e("da39 " + e.getMessage());
	// // mRingerPlayer=null;
	// // }
	// // } else {
	// // Log.w("already ringing");
	// // }
	// //tml|sw*** audio break
	// controlBkgndMusic(0);
	// if (mAudioTrack == null && !ringrdy) {
	// Log.d("tml DA STARTringing ***** DO!");
	// int iMinBufSize = AudioTrack.getMinBufferSize(16000,
	// AudioFormat.CHANNEL_CONFIGURATION_MONO,
	// AudioFormat.ENCODING_PCM_16BIT);
	// mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
	// AudioFormat.CHANNEL_CONFIGURATION_MONO,
	// AudioFormat.ENCODING_PCM_16BIT,
	// iMinBufSize, AudioTrack.MODE_STREAM);
	//
	// mHandler.post(new Runnable () {
	// @Override
	// public void run() {
	// ringrdy = true;
	// ringTask = new MyRingerTask(1);
	// ringTask.execute();
	// }
	// });
	// } else {
	// Log.w("already ringing");
	// }
	// //***tml
	// mHandler.postDelayed(ringLimit, RINGLIMITX);//30 sec
	// } catch (Exception e) {
	// Log.e("da40 " + e.getMessage());
	// Log.e("cannot handle incoming call "+e.getMessage());
	// }
	// }

	Runnable ringLimit = new Runnable() {
		@Override
		public void run() {
			Log.e("ringLimit !!!");
			// stopRinging();
			stopRing(); // tml*** new ring

			mStatus.setText(R.string.call_end);
			mHangup.setOnClickListener(null);
			HangingUp = true;
			sendTerminateSignal = true;
			try {
				mHandler.removeCallbacks(timeElapsed);
				mHandler.removeCallbacks(run_disp_msg);
				mHandler.removeCallbacks(run_mute);

				if (AireVenus.instance() != null) {
					VoipCore lVoipCore = AireVenus.instance().getVoipCore();
					VoipCall myCall = lVoipCore.getCurrentCall();
					if (myCall != null) {
						lVoipCore.terminateCall(myCall);
						Log.e("voip.ringLimit terminateCall()");
						return;
					}
					Log.d("getCurrentCall==null");
				}
			} catch (Exception e) {
				Log.e("ringLimit !@#$ " + e.getMessage());
			}
			exitCallMode("ringLimit");
		}
	};

	// tml*** new ring
	private AudioTrack mAudioTrack = null;
	private InputStream inS = null;
	private DataInputStream dinS = null;
	private volatile boolean ringrdy = false;

	@SuppressWarnings("deprecation")
	public void prepareRing(boolean en, int mode, int calltype, String from) {
		if (en) {
			Log.d("RING." + mode + AireVenus.getCallTypeName(calltype)
					+ " DA *** PREP! (" + from + ")");
			if (mode == 1) { // incoming
				if (mAudioTrack == null) {
					int iMinBufSize = AudioTrack.getMinBufferSize(16000,
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT);
					mAudioTrack = new AudioTrack(AudioManager.STREAM_RING,
							16000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT, iMinBufSize,
							AudioTrack.MODE_STREAM);
					maxiVol(1, 1);
					controlBkgndMusic(0);
					mHandler.postDelayed(ringLimit, RINGLIMITX);
					StartRing startRing = new StartRing(mode, calltype, true);
					new Thread(startRing).start();
				}
			} else if (mode == 0) { // outgoing
				if (mAudioTrack == null) {
					int iMinBufSize = AudioTrack.getMinBufferSize(16000,
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT);
					mAudioTrack = new AudioTrack(AudioManager.STREAM_RING,
							16000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT, iMinBufSize,
							AudioTrack.MODE_STREAM);
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
				AireJupiter.getInstance().TESTseeVolumes("DAring");
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

			if (mode == 1) { // incoming
				String intx = Integer.toString(rng.nextInt(8) + 1);
				//Hsia 修改来电铃声为叮咚叮咚
				ringfile = "ringx" + ".raw";
//				ringfile = "r16k_" + intx + ".raw";
			} else { // outgoing
				String intx = Integer.toString(rng.nextInt(8) + 1);
				ringfile = "r16k_" + intx + ".raw";
			}

			Log.d("RING." + ringfile + " *** DO! " + ringrdy);
			if (ringrdy && mAudioTrack != null) {
				AssetManager inAssets = getAssets();
				inS = inAssets.open(ringfile);
				dinS = new DataInputStream(inS);

				mAudioTrack.play();
				while (((i = dinS.read(audiobuff, 0, buffSize)) > -1)) {
					mAudioTrack.write(audiobuff, 0, i);
					if (!ringrdy || theDialer == null)
						break;
				}
			} else {
				Log.e("RING mAudioTrack null");
			}

			audiobuff = null;
			Log.d("RING reach END.force(" + !ringrdy + ")");
			if (ringrdy && theDialer != null) { // incoming
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
					StartRing startRing = new StartRing(mode,
							AireVenus.getCallType(), false);
					new Thread(startRing).start();
				}
			}
		} catch (Exception e) {
			Log.d("DA RING !@#$ " + e.getMessage());
			stopRing();
		} finally {
			if (!repeat)
				ringrdy = false;
		}
	}

	Runnable runRepeat = new Runnable() {
		@Override
		public void run() {
		}
	};

	private void stopRing() {
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
			if (inS != null)
				inS.close();
			if (dinS != null)
				dinS.close();
			// Log.e("RING *** STOP!");
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
		if (divAM <= 0)
			divAM = 1;
		if (mode == 1) { // max vol
			prevVol1 = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			maxVol1 = audioManager
					.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			int sysMax = audioManager
					.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
			returnVol = 1;
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
					(int) (maxVol1 / divAM), 0);
			if (sysMax > 6)
				sysMax = sysMax - 3;
			else if (sysMax > 2)
				sysMax = sysMax - 1;
			audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, sysMax, 0);
			Log.d("maxiVol DA set." + mode + " " + divAM + "|" + prevVol1 + "|"
					+ maxVol1);
		} else if (mode == 0 && returnVol == 1) { // return vol
			maxVol1 = prevVol1;
			returnVol = 0;
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
					(int) (maxVol1 / divAM), 0);
			Log.d("maxiVol DA return." + mode + " " + divAM + "|" + prevVol1);
		}
	}

	boolean musicWasActive = false;

	private void controlBkgndMusic(int mode) {
		// AudioManager mAudioManager = ((AudioManager)
		// getSystemService(Context.AUDIO_SERVICE));
		Intent imediaplay = new Intent("com.android.music.musicservicecommand");
		if (mode == 0 && mAudioManager.isMusicActive()) {
			mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
			// imediaplay.putExtra("command", "pause");
			// sendBroadcast(imediaplay);
			musicWasActive = true;
			Log.d("controlBkgndMusic" + mode + musicWasActive);
		} else if (mode == 1 && musicWasActive) {
			mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
			// imediaplay.putExtra("command", "play");
			// sendBroadcast(imediaplay);
			Log.d("controlBkgndMusic" + mode + musicWasActive);
			musicWasActive = false;
		} else if (mode == 2) {
			mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
			Log.d("controlBkgndMusic" + mode + musicWasActive);
			musicWasActive = false;
		}
	}

	// ***tml

	// public void stopRinging_old() {
	// // if (mRingerPlayer !=null) {
	// // mRingerPlayer.stop();
	// // mRingerPlayer.release();
	// // mRingerPlayer=null;
	// // }
	// //tml|sw*** audio break
	// if (mAudioTrack != null) {
	// try {
	// Log.d("tml DA STOPring ***** DO!");
	// ringTask.cancel(true);
	// ringrdy = false;
	// // mAudioTrack.flush();
	// // mAudioTrack.stop();
	// // mAudioTrack.release();
	// // mAudioTrack = null;
	// if (inS != null) {
	// inS.close();
	// }
	// if (dinS != null) {
	// dinS.close();
	// }
	// inS = null;
	// dinS = null;
	// Log.d("tml AV STOPring ***** SUCCESS! ");
	// } catch (IOException e) {
	// Log.e("da stopRing " + e.getMessage());
	// }
	// }
	// //***tml
	// if (mVibrator!=null) {
	// mVibrator.cancel();
	// }
	// }
	//
	// //tml|sw*** audio break, new ringer code, so ui doesnt freeze
	// private MyRingerTask ringTask;
	// private class MyRingerTask extends AsyncTask<Void, Void, Void> {
	// int ringmode;
	// MyRingerTask (int mode) {
	// ringmode = mode;
	// }
	//
	// @Override
	// protected Void doInBackground(Void... params) {
	// try {
	// int bufferSize = 5120;
	// byte[] audiobuff = new byte[bufferSize];
	//
	// int i = 0;
	// AssetManager am = getAssets();
	// Random rng = new Random();
	// String intx = Integer.toString(rng.nextInt(8) + 1);
	// String musicfile = "r16k_" + intx + ".raw";
	// int gst = mAudioTrack.getStreamType();
	// int gsv = mAudioManager.getStreamVolume(gst);
	// // mAudioManager.setStreamMute(gst, false);
	// Log.i("tml ringfile> " + musicfile + " " + gst + "=" + gsv);
	//
	// inS = am.open(musicfile);
	// dinS = new DataInputStream(inS);
	//
	// if (ringrdy) {
	// inS = am.open(musicfile);
	// dinS = new DataInputStream(inS);
	//
	// mAudioTrack.play();
	// while(((i = dinS.read(audiobuff, 0, bufferSize)) != -1)) {
	// mAudioTrack.write(audiobuff, 0, i);
	// if (ringTask.isCancelled()) {
	// mAudioTrack.pause();
	// mAudioTrack.flush();
	// mAudioTrack.stop();
	// mAudioTrack.release();
	// Log.e("tml DA ring.mAudioTrack cleared!");
	// break;
	// }
	// }
	// }
	// // mAudioTrack.flush();
	// // mAudioTrack.stop();
	// // mAudioTrack.release();
	// mAudioTrack = null;
	// // inS.close();
	// // dinS.close();
	// // inS = null;
	// // dinS = null;
	// // Log.d("tml AV STOPring ***** SUCCESS! ");
	// } catch (Exception e) {
	// Log.e("av13 " + e.getMessage());
	// ringrdy = false;
	// mAudioTrack.stop();
	// mAudioTrack.release();
	// mAudioTrack = null;
	// if (inS != null) {
	// inS = null;
	// }
	// if (dinS != null) {
	// dinS = null;
	// }
	// }
	// return null;
	// }
	// }
	// //***tml
	//
	// //yang*** speex player
	// private RingerPlayer_WB rwb;
	// public synchronized void startRingBackSpeex(String from) {
	// Log.d("yang start ring back speex");
	// AssetManager am = getAssets();// u have get assets path from this code
	// Random random = new Random();
	// int numring = 2;
	// int i =random.nextInt(numring) + 1;
	// String ring = "ring"+i+".spx";
	// Log.d("yang ring "+ring);
	// try {
	// InputStream inputStream = am.open(ring);
	// byte[] data = toByteArray(inputStream);
	// ArrayList<byte[]> arrayList = new ArrayList<byte[]>();
	// byte[] newdata = new byte[data.length];
	// System.arraycopy(data, 0, newdata, 0, data.length);
	//
	// rwb= new RingerPlayer_WB(DialerActivity.this, 3, true);
	//
	// rwb.append(newdata, newdata.length);
	// rwb.run();
	//
	// } catch (IOException e) {
	// rwb.stop();
	// rwb.release();
	// }
	// }
	//
	// public static byte[] toByteArray(InputStream input) {
	// ByteArrayOutputStream output = new ByteArrayOutputStream();
	// byte[] buffer = new byte[4096];
	// int n = 0;
	// try {
	// while (-1 != (n = input.read(buffer))) {
	// output.write(buffer, 0, n);
	// }
	// } catch (IOException e) {
	// try {
	// output.close();
	// } catch (IOException e1) {
	// }
	// }
	// return output.toByteArray();
	// }
	// ***yang

	// sw*** cpu freq
	public static int getMaxCPUFreqMHz() {
		int maxFreq = -1;
		try {
			RandomAccessFile reader = new RandomAccessFile(
					"/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state",
					"r");
			boolean done = false;
			while (!done) {
				String line = reader.readLine();
				if (null == line) {
					done = true;
					break;
				}
				String[] splits = line.split("\\s+");
				assert (splits.length == 2);
				int timeInState = Integer.parseInt(splits[1]);
				if (timeInState > 0) {
					int freq = Integer.parseInt(splits[0]) / 1000;
					if (freq > maxFreq) {
						maxFreq = freq;
					}
				}
			}
		} catch (IOException ex) {
		}
		return maxFreq;
	}

	// ***sw
	// tml|yangjun*** vidconf
	Runnable showControls = new Runnable() {
		public void run() {
			FrameLayout titleView = (FrameLayout) findViewById(R.id.title);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) titleView
					.getLayoutParams();
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
			titleView.setLayoutParams(params);
			((ImageView) findViewById(R.id.controls_expand))
					.setVisibility(View.GONE);
			((LinearLayout) findViewById(R.id.panel))
					.setVisibility(View.VISIBLE);
			((FrameLayout) findViewById(R.id.controls))
					.setVisibility(View.VISIBLE);
		}
	};

	Runnable hideControls = new Runnable() {
		public void run() {
			FrameLayout titleView = (FrameLayout) findViewById(R.id.title);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) titleView
					.getLayoutParams();
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			titleView.setLayoutParams(params);
			((ImageView) findViewById(R.id.controls_expand))
					.setVisibility(View.VISIBLE);
			((LinearLayout) findViewById(R.id.panel)).setVisibility(View.GONE);
			((FrameLayout) findViewById(R.id.controls))
					.setVisibility(View.GONE);
		}
	};

	Runnable showMembers = new Runnable() {
		@Override
		public void run() {
			if (((FrameLayout) findViewById(R.id.members_view)).getVisibility() != View.VISIBLE)
				((FrameLayout) findViewById(R.id.members_view))
						.setVisibility(View.VISIBLE);
		}
	};

	Runnable hideMembers = new Runnable() {
		@Override
		public void run() {
			if (((LinearLayout) findViewById(R.id.topVWin_holder))
					.getChildCount() > 0)
				((FrameLayout) findViewById(R.id.members_view))
						.setVisibility(View.INVISIBLE);
		}
	};

	@SuppressLint("NewApi")
	private void initVidConf(boolean onlyInit) {
		try {
			if (xWalkWebView == null) {
				XWalkPreferences.setValue(
						XWalkPreferences.ANIMATABLE_XWALK_VIEW, false);
				xWalkWebView = new XWalkView(DialerActivity.this,
						DialerActivity.this);
				xWalkWebView.clearCache(false);
				Log.i("vidConfxwalk xWalkWebView ready");
				if (onlyInit)
					return;
			}

			// String vcUrl =
			// "https://115.29.171.94:8443/demos/demo_multiparty_video_only_b.html?";
			// if (mPref.readBoolean("VC8443", false)) vcUrl =
			// "https://115.29.171.94:8443/demos/demo_multiparty_video_only_b.html?";
			// int roomN = mPref.readInt("ChatroomHostIdx");
			// String vcUrl =
			// "http://115.29.171.94:8080/demos/demo_multiparty_video_only_b.html?";
			String vcUrl = "http://115.29.171.94:8000/demos/demo_multiparty_video_only_c.html?";
			if (mPref.readBoolean("VC8443", false))
				vcUrl = "http://115.29.171.94:8000/demos/demo_multiparty_video_only_c.html?";
			int roomN = mPref.readInt("ChatroomHostIdx");
			String vcRoom = "room=" + Integer.toString(roomN);
			String vcRes = "&res=2++";
			// String vcRes = "&res=3";

			// xwf*** broadcast
			if (mPref.readInt("BCAST_CONF", -1) >= 0) {
				if(mPref.readBoolean("pay", false)){
//					Toast.makeText(DialerActivity.this,"已支付，哈哈",0).show();
					newBrost("res=2");
				}else{
					vcUrl = "https://www.xingfafa.com:8443/demos/demo_multiparty_video_only_b_broadcast.html?";
					vcRoom = "room=" + Integer.toString(roomN);
					if (mPref.readInt("BCAST_CONF", 0) == 0) {
						vcRes = "&broadcast=0";
					} else {
						vcRes = "&broadcast=1";
					}
					Log.i("vidConfxwalk >> " + vcRoom + vcRes + " " + vcUrl);
					xWalkWebView.load(vcUrl + vcRoom + vcRes, null);

				}
			}else{
				Log.i("vidConfxwalk >> " + vcRoom + vcRes + " " + vcUrl);
				xWalkWebView.load(vcUrl + vcRoom + vcRes, null);
			}

			((SurfaceView) findViewById(R.id.topVWin_surface))
					.setVisibility(View.VISIBLE);
			// ((SurfaceView)
			// findViewById(R.id.topVWin_surface)).setZOrderOnTop(false);
			((LinearLayout) findViewById(R.id.topVWin_holder))
					.addView(xWalkWebView);

			// mHandler.postDelayed(new Runnable() {
			// @Override
			// public void run() {
			// if (mSpeaker != null && !mSpeaker.isChecked())
			// mSpeaker.performClick();
			// }
			// }, 1000);

			boolean hwAccelOk = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
			if (hwAccelOk) {
				((SurfaceView) findViewById(R.id.topVWin_surface))
						.setLayerType(View.LAYER_TYPE_NONE, null); // cannot do
				// other
				((LinearLayout) findViewById(R.id.topVWin_holder))
						.setLayerType(View.LAYER_TYPE_HARDWARE, null);
				xWalkWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null); // no
				// effect?
			}

			Log.d("vidConfxwalk showing, "
					+ hwAccelOk
					+ ":hw+:"
					+ ((SurfaceView) findViewById(R.id.topVWin_surface))
					.getLayerType()
					+ ((SurfaceView) findViewById(R.id.topVWin_surface))
					.isHardwareAccelerated()
					+ " "
					+ ((LinearLayout) findViewById(R.id.topVWin_holder))
					.getLayerType()
					+ ((LinearLayout) findViewById(R.id.topVWin_holder))
					.isHardwareAccelerated()
					+ " "
					+ xWalkWebView.getLayerType()
					+ xWalkWebView.isHardwareAccelerated()
					+ " "
					+ "sv:"
					+ !XWalkPreferences
					.getBooleanValue(XWalkPreferences.ANIMATABLE_XWALK_VIEW));
		} catch (Exception e) {
			Log.e("initVidConf !@#$ " + e.getMessage());
		}
	}
	private void newBrost(String vcRes) {
		String vcUrl;
		vcUrl = "https://bc.xingfafa.com/release/call.htm?";
		int myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
		String getIdx = "&idx=" + myIdx;
		String getPassword = mPref.read("password", "1111");
		String setBase64 = MyUtil.setBase64(getPassword);
		String base64 = MyUtil.getBase64(setBase64);
		MyPreference mPref = new MyPreference(this);
		String myNickname = "&nickname=" + myIdx;
		String newRoomN = "";
		if (getIdx.length() < 7) {
		} else {
			if (mPref.readInt("BCAST_CONF", -1) == 1) {// 主叫方
				newRoomN = "1009" + mPref.readInt("ChatroomHostIdx");
			} else if (mPref.readInt("BCAST_CONF", -1) == 0) {
				newRoomN = "1008" + mPref.readInt("ChatroomHostIdx");
			}
		}
		String BRoom = "&room=" + newRoomN;
		if (mPref.readInt("BCAST_CONF", -1) == 1) {
			xWalkWebView.load(vcUrl + vcRes + getIdx + "&pd=W1o2r3d4p5s6"
					+ setBase64 + myNickname + BRoom + "&broadcast=1", null);
			Log.d("new Brocasting URL:" + vcUrl + vcRes + getIdx
					+ "&pd=W1o2r3d4p5s6" + setBase64 + myNickname + BRoom
					+ "&broadcast=1");
		} else if (mPref.readInt("BCAST_CONF", -1) == 0) {
			vcRes = "res=0";
			xWalkWebView.load(vcUrl + vcRes + getIdx + "&pd=W1o2r3d4p5s6"
					+ setBase64 + myNickname + BRoom, null);
			Log.d("new Brocasting URL:" + vcUrl + vcRes + getIdx
					+ "&pd=W1o2r3d4p5s6" + setBase64 + myNickname + BRoom);
		}
	}
	private void endVidConf() {
		try {
			if (xWalkWebView != null) {
				((SurfaceView) findViewById(R.id.topVWin_surface))
						.setVisibility(View.GONE);
				((LinearLayout) findViewById(R.id.topVWin_holder))
						.removeView(xWalkWebView);
				xWalkWebView.onDestroy();
				xWalkWebView = null;

				// mHandler.postDelayed(new Runnable() {
				// @Override
				// public void run() {
				// if (mSpeaker != null && mSpeaker.isChecked())
				// mSpeaker.performClick();
				// }
				// }, 1000);

				Log.e("vidConfxwalk Destroyed");
			}
		} catch (Exception e) {
			Log.e("endVidConf !@#$ " + e.getMessage());
		}
	}

	// ***tml
	// tml*** beta ui
	Runnable connectStatusShow = new Runnable() {
		public void run() {
			//bree:如果不是新广播
			if (!(mPref.readInt("BCAST_CONF", -1) >= 0 && mPref.readBoolean("pay", false))) {
				((ProgressBar) findViewById(R.id.connect_status)).setVisibility(View.VISIBLE);
			}
		}
	};

	Runnable connectStatusHide = new Runnable() {
		public void run() {
			((ProgressBar) findViewById(R.id.connect_status))
					.setVisibility(View.GONE);
		}
	};

	private void answer() {
		// 1.自动接听
		mPref.write("tempCheckSameIN", 0); // tml*** sametime
		stopSelfPreview();
		mHangup2.setVisibility(View.GONE); // tml*** beta ui
		mHold.setVisibility(View.VISIBLE);
		mAnswerSlide.setVisibility(View.GONE);
		if (!BluetoothSco && !dontUseInCallMode)
			mAudioManager.setMode(AudioManager.MODE_IN_CALL);

		if (incomingChatroom) {
			mHandler.removeCallbacks(ringLimit);
			mHandler.postDelayed(callToChatroom, 200);
		} else
			mHandler.postDelayed(answerCall, 200);
		// 2.打开摄像头
		((ToggleButton) findViewById(R.id.video)).setChecked(true);
		((ToggleButton) findViewById(R.id.video)).isChecked();

	}
}