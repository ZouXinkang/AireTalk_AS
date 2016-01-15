package com.pingshow.airecenter;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.hardware.Camera;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnGenericMotionListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.pingshow.airecenter.browser.MainBrowser;
import com.pingshow.airecenter.cons.Key;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.beehive.BeeHiveService;
import com.pingshow.homesafeguard.HomeSafeguardService;
import com.pingshow.homesafeguard.HomeSafeguardService.HomeSafeguadBinder;
import com.pingshow.homesafeguard.HomeSafeguardService.ReadDataListener;
import com.pingshow.homesafeguard.HomeSafeguardService.TriggerNotifyRun;
import com.pingshow.homesafeguard.UsbData;
import com.pingshow.homesafeguard.UsbDevice;
import com.pingshow.homesafeguard.UsbDeviceMatcher;
import com.pingshow.iot.BlueComm;
import com.pingshow.iot.BlueDeviceListAdapter;
import com.pingshow.iot.Utils;
import com.pingshow.iot.BlueComm.OnDataAvailableListener;
import com.pingshow.iot.BlueComm.OnServiceDiscoverListener;
import com.pingshow.network.MyNet;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MCrypt;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.video.capture.AndroidVideoApi9JniWrapper;
import com.pingshow.video.capture.hwconf.AndroidCameraConfiguration;
import com.pingshow.voip.AireVenus;

public class SecurityNewActivity extends Activity implements ImageGetter{
	
	private AmpUserDB mADB;
	private float mDensity=1.0f;
	private MyPreference mPref;
	private LinearLayout authorizedList;
	private List<String> instants;
	public static final int MAXUSERS = 5;
	
	static public SecurityNewActivity _this;
	
	private SecurityHistoryAdapter mAdapter;
	private ListView mList;
	private List<Map<String, String>> items = new ArrayList<Map<String, String>>();

	
	private boolean bluetooth_support = true;

	private static final long SCAN_PERIOD = 30000;
	private static final int SCAN_PERIODi = (int) (SCAN_PERIOD / 1000);
	private boolean iniAcquiringHR = false;
	
	public static final String VEEPOO = "veepoo";
	public static final String VEEPOO_tag = "VEEPOO_BLUE";
	public static final int MAX_VEEPOO_DEVICES = 2;
	private boolean readPW_VEEPOO = false;
	public static boolean readHR_VEEPOO = false;
	public static final UUID VEEPOO_DEVICE_SRV = UUID
			.fromString("F0080001-0451-4000-B000-000000000000");
	public static final UUID VEEPOO_DEVICE_DAT = UUID
			.fromString("F0080002-0451-4000-B000-000000000000");
	public static final UUID VEEPOO_DEVICE_CFG = UUID
			.fromString("F0080003-0451-4000-B000-000000000000");
	public static final UUID VEEPOO_HR_SRV = UUID
			.fromString("F0040001-0451-4000-B000-000000000000");
	public static final UUID VEEPOO_HR_DAT = UUID
			.fromString("F0040002-0451-4000-B000-000000000000");
	public static final UUID VEEPOO_HR_CFG = UUID
			.fromString("F0040003-0451-4000-B000-000000000000");

	public static final String ACCULIFE = "acculife";
	public static final String ACCULIFE_tag = "ACCULIFE_BLUE";
	public static final int MAX_ACCULIFE_DEVICES = 2;
	private boolean readPW_ACCULIFE = false;
	public static boolean readHR_ACCULIFE = false;
	public static final UUID ACCULIFE_DEVICE_DAT = UUID
			.fromString("0000180A-0000-1000-8000-00805F9B34FB");
	public static final UUID ACCULIFE_HR_SRV = UUID
			.fromString("0000AA00-0000-1000-8000-00805F9B34FB");
	public static final UUID ACCULIFE_HR_DAT = UUID
			.fromString("0000180D-0000-1000-8000-00805F9B34FB");
	public static final UUID ACCULIFE_HR_CFG = UUID
			.fromString("0000AA02-0000-1000-8000-00805F9B34FB");
	public static final UUID ACCULIFE_BATT_DAT = UUID
			.fromString("0000180f-0000-1000-8000-00805F9B34FB");
	public static final UUID ACCULIFE_ALRT_DAT = UUID
			.fromString("00001802-0000-1000-8000-00805F9B34FB");
	//tml*** blue io
	private int getWatchSaveSpot (String addr, String watch) {
		if (watch.contains(VEEPOO))
		{
			for (int i = 0; i < MAX_VEEPOO_DEVICES; i++) {
				String device = mPref.read(VEEPOO_tag + i, "");
				if (addr != null) {
					if (device.contains(addr)) {
//						Log.d("myblue  getVeepooSaveSpot+ADDR=" + i);
						return i;
					}
				} else {
					if (device.length() == 0) {
						Log.d("myblue  getVeepooSaveSpot=" + i);
						return i;
					}
				}
			}
		}
		else if (watch.equals(ACCULIFE))
		{
			for (int i = 0; i < MAX_ACCULIFE_DEVICES; i++) {
				String device = mPref.read(ACCULIFE_tag + i, "");
				if (addr != null) {
					if (device.contains(addr)) {
//						Log.d("myblue  getAcculifeSaveSpot+ADDR=" + i);
						return i;
					}
				} else {
					if (device.length() == 0) {
						Log.d("myblue  getAcculifeSaveSpot=" + i);
						return i;
					}
				}
			}
		}
		Log.e("myblue  getWatchSaveSpot(" + watch + ":" + addr + ")=n/a");
		return -1;
	}
	//tml*** blue io
	private String[] getWatchDeviceInfo (String addr, String watch) {
		if (watch.contains(VEEPOO)) {
			String[] info = new String[3];
			for (int i = 0; i < MAX_VEEPOO_DEVICES; i++) {
				String device = mPref.read(VEEPOO_tag + i, "");
				if (device.contains(addr)) {
					info[0] = device;
					info[1] = mPref.read(VEEPOO_tag + i + "P", "");
					info[2] = mPref.read(VEEPOO_tag + i + "S", "");
					return info;
				}
			}
		}
		else if (watch.equals(ACCULIFE))
		{
			String[] info = new String[3];
			for (int i = 0; i < MAX_ACCULIFE_DEVICES; i++) {
				String device = mPref.read(ACCULIFE_tag + i, "");
				if (device.contains(addr)) {
					info[0] = device;
					info[1] = mPref.read(ACCULIFE_tag + i + "P", "");
					info[2] = mPref.read(ACCULIFE_tag + i + "S", "");
					return info;
				}
			}
		}
		Log.e("myblue  getWatchDeviceInfo !@#$ invalid (" + watch + ":" + addr + ")");
		return null;
	}
	//tml*** blue io
	public String uidName (UUID uuid) {
		String name = "";
		if (uuid.equals(VEEPOO_DEVICE_SRV)) {
			name = "VEEPOO_DEV_SEV";
		} else if (uuid.equals(VEEPOO_DEVICE_DAT)) {
			name = "VEEPOO_DEV_DATA";
		} else if (uuid.equals(VEEPOO_DEVICE_CFG)) {
			name = "VEEPOO_DEV_CONF";
		} else if (uuid.equals(VEEPOO_HR_SRV)) {
			name = "VEEPOO_HR_SEV";
		} else if (uuid.equals(VEEPOO_HR_DAT)) {
			name = "VEEPOO_HR_DATA";
		} else if (uuid.equals(VEEPOO_HR_CFG)) {
			name = "VEEPOO_HR_CONF";
		} else {
			name = uuid.toString();
		}
		return name;
	}

	private BluetoothAdapter mBluetoothAdapter;
	private BlueComm mBlueComm;
	private ListView deviceList_scan;
	private BlueDeviceListAdapter mBlueDeviceListAdapter;
	private boolean mScanning = false;
	private int scanTimer = 0;
	
	//tml*** sec more
	private int cur_lang0;
	private String cur_lang = "";
	
	public static String basepath = null;
	
	int maskColor = 0xBBFF0000;
	
	static public SecurityNewActivity getInstance()
	{
		return _this;
	}
	
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		}
	};
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.security_main);
		
		_this = this;
        
		mPref = new MyPreference(this);
		mPref.write(Key.SELFVIDIO, false);
		//tml*** redirect preregister
		boolean registered = mPref.readBoolean("AireRegistered", false);
		if (!registered) {
			Intent intent = new Intent();
			intent.setClass(SecurityNewActivity.this, SplashScreen.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			finish();
			return;
		}
		//tml|li*** blue io
		bluetooth_support = true;
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Log.e("myblue not supported1");
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
			bluetooth_support = false;
		}

		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		if (mBluetoothAdapter == null) {
			Log.e("myblue not supported2");
			Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
			bluetooth_support = false;
		}

        mBlueComm = new BlueComm(this);
		if (!mBlueComm.initialize()) {
			Log.e("myblue  BlueComm initialize error");
			Toast.makeText(this, R.string.bluetooth_error, Toast.LENGTH_LONG).show();
			bluetooth_support = false;
			mBlueComm = null;
		}
		//***tml
		
		if (getIntent().getBooleanExtra("launchFromSelf", false))
			this.overridePendingTransition(R.anim.freeze, R.anim.freeze);
		
        neverSayNeverDie(_this);  //tml|bj*** neverdie/
		
		instants=mPref.readArray("instants");

	    mDensity = getResources().getDisplayMetrics().density;

	    mADB = new AmpUserDB(this);
		mADB.open();
		
		((TextView)findViewById(R.id.tab_monitoring)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				stopCamera();  //tml*** suv mask
				swtichToPage(R.id.tab_monitoring);
				((TextView)findViewById(R.id.tab_monitoring)).requestFocus();  //tml*** prefocus
			}
	    });
		((TextView)findViewById(R.id.tab_monitoring)).requestFocus();  //tml*** prefocus
		
		((TextView)findViewById(R.id.tab_history)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				stopCamera();  //tml*** suv mask
				basepath = null;
				swtichToPage(R.id.tab_history);
				((TextView)findViewById(R.id.tab_history)).requestFocus();  //tml*** prefocus
			}
	    });
		
		((TextView)findViewById(R.id.tab_setting)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				swtichToPage(R.id.tab_setting);
				((TextView)findViewById(R.id.tab_setting)).requestFocus();  //tml*** prefocus
			}
	    });
		
	    ((Button)findViewById(R.id.add)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
//				Intent it=new Intent(SecurityNewActivity.this, SelectUserActivity.class);
//				it.putExtra("limit", 1);
//				it.putExtra("exclude", (ArrayList<String>)instants);
//				startActivityForResult(it, 1000);
				//tml*** secmax
				if (instants.size() < MAXUSERS) {
					Intent it=new Intent(SecurityNewActivity.this, SelectUserActivity.class);
					it.putExtra("limit", 1);
					it.putExtra("exclude", (ArrayList<String>)instants);
					startActivityForResult(it, 1000);
				} else {
					mHandler.post(instantMaxTooltip);
				}
			}
	    });
	    
	    //tml*** sec more
	    cur_lang0 = mPref.readInt("ssendLang0", 0);
	    if (cur_lang0 == 0) {
	    	cur_lang = getResources().getString(R.string.sel_lang_us); 
	    } else if (cur_lang0 == 1) {
	    	cur_lang = getResources().getString(R.string.sel_lang_ar); 
	    } else if (cur_lang0 == 2) {
	    	cur_lang = getResources().getString(R.string.sel_lang_zh); 
	    } else if (cur_lang0 == 3) {
	    	cur_lang = getResources().getString(R.string.sel_lang_es); 
	    } else if (cur_lang0 == 4) {
	    	cur_lang = getResources().getString(R.string.sel_lang_fr); 
	    }
	    ((Button) findViewById(R.id.send_language_show)).setText(cur_lang);
	    
	    ((Button) findViewById(R.id.send_language_show)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				AlertDialog.Builder builder = new AlertDialog.Builder(SecurityNewActivity.this);
				CharSequence[] options = {getResources().getString(R.string.sel_lang_us),
						getResources().getString(R.string.sel_lang_ar),
						getResources().getString(R.string.sel_lang_zh),
						getResources().getString(R.string.sel_lang_es),
						getResources().getString(R.string.sel_lang_fr)};
				int index = mPref.readInt("ssendLang0", 0);
				
				builder.setTitle(getResources().getString(R.string.sel_language));
				builder.setSingleChoiceItems(options, index, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int index) {
						switch (index) {
							case 0:
								cur_lang = getResources().getString(R.string.sel_lang_us);
								cur_lang0 = index;
								mPref.write("ssendLang0", cur_lang0);
								mPref.write("ssendLang", "us_english");
								break;
							case 1:
								cur_lang = getResources().getString(R.string.sel_lang_ar);
								cur_lang0 = index;
								mPref.write("ssendLang0", cur_lang0);
								mPref.write("ssendLang", "arabic");
								break;
							case 2:
								cur_lang = getResources().getString(R.string.sel_lang_zh);
								cur_lang0 = index;
								mPref.write("ssendLang0", cur_lang0);
								mPref.write("ssendLang", "chinese");
								break;
							case 3:
								cur_lang = getResources().getString(R.string.sel_lang_es);
								cur_lang0 = index;
								mPref.write("ssendLang0", cur_lang0);
								mPref.write("ssendLang", "spanish");
								break;
							case 4:
								cur_lang = getResources().getString(R.string.sel_lang_fr);
								cur_lang0 = index;
								mPref.write("ssendLang0", cur_lang0);
								mPref.write("ssendLang", "french");
								break;
						}
					    ((Button) findViewById(R.id.send_language_show)).setText(cur_lang);
						Log.i("tml SLANG> " + index + " " + mPref.read("ssendLang"));
					    dialog.dismiss();
					}
				});
				
				builder.create().show();
			}
	    });
	    //***tml
	    
//	    String securitySubscription = mPref.read("SecurityDueDate", "---");
	    //tml|sw*** subscription php
//	    if (!securitySubscription.startsWith("success")) {
////	    	Log.e("suv disabled");
//	    	mPref.write("securityEnabled", false);
//	    	((ImageButton) findViewById(R.id.guard)).setImageResource(R.drawable.slider_off);
//	    	((ImageButton) findViewById(R.id.guard)).setEnabled(false);
//	    	((ImageButton) findViewById(R.id.guard)).setOnClickListener(null);
//		} else {
////			Log.e("suv enabled");
//	    	mPref.write("securityEnabled", true);
//	    	boolean securityEnabled = mPref.readBoolean("securityEnabled", true);
//		    ((ImageButton) findViewById(R.id.guard)).setImageResource(securityEnabled ? R.drawable.slider_on : R.drawable.slider_off);
//	    	((ImageButton) findViewById(R.id.guard)).setEnabled(true);
//		    ((ImageButton) findViewById(R.id.guard)).setOnClickListener(new OnClickListener() {
//				@Override
//				public void onClick(View v) {
//					 boolean securityEnabled = mPref.readBoolean("securityEnabled", true);
//					 securityEnabled =! securityEnabled;
//					 mPref.write("securityEnabled", securityEnabled);
//					 ((ImageButton) v).setImageResource(securityEnabled ? R.drawable.slider_on : R.drawable.slider_off);
//				}
//		    });
//	    }
	    //tml|sw*** new subs model
    	boolean securityEnabled = mPref.readBoolean("securityEnabled", true);
	    ((ImageButton) findViewById(R.id.guard)).setImageResource(securityEnabled ? R.drawable.slider_on : R.drawable.slider_off);
    	((ImageButton) findViewById(R.id.guard)).setEnabled(true);
	    ((ImageButton) findViewById(R.id.guard)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				 boolean securityEnabled = mPref.readBoolean("securityEnabled", true);
				 securityEnabled =! securityEnabled;
				 mPref.write("securityEnabled", securityEnabled);
				 ((ImageButton) v).setImageResource(securityEnabled ? R.drawable.slider_on : R.drawable.slider_off);
				 
				 if (!securityEnabled) {
					 ((ImageButton) findViewById(R.id.guard_homeiot)).setImageResource(R.drawable.slider_off);
				 }
			}
	    });
	    //tml*** iot control
		boolean securityHomeIOTEnabled = mPref.readBoolean("openHomeSafeguard", false);
		if (!securityHomeIOTEnabled || !securityEnabled) mPref.write("securityHomeIOT", false);
	    securityHomeIOTEnabled = mPref.readBoolean("securityHomeIOT", false);
	    ((ImageButton) findViewById(R.id.guard_homeiot)).setImageResource(securityHomeIOTEnabled ? R.drawable.slider_on : R.drawable.slider_off);
	    ((ImageButton) findViewById(R.id.guard_homeiot)).setEnabled(true);
	    ((ImageButton) findViewById(R.id.guard_homeiot)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean securityEnabled = mPref.readBoolean("securityEnabled", true);
				if (!securityEnabled) {
					Toast.makeText(getApplicationContext(), getString(R.string.security_off), 0).show();
					return;
				}
				boolean securityIOTEnabled = mPref.readBoolean("openHomeSafeguard", false);
				if (!securityIOTEnabled || (deviceAdapter != null && deviceAdapter.getCount() < 1)) {
					Toast.makeText(getApplicationContext(), getString(R.string.device_empty_notify), 0).show();
					return;
				}
				boolean securityHomeIOTEnabled = mPref.readBoolean("securityHomeIOT", false);
				
				securityHomeIOTEnabled =! securityHomeIOTEnabled;
				mPref.write("securityHomeIOT", securityHomeIOTEnabled);
				((ImageButton) v).setImageResource(securityHomeIOTEnabled ? R.drawable.slider_on : R.drawable.slider_off);
				
				Intent intent;
				if (securityHomeIOTEnabled) {
					intent = new Intent(Global.Action_Start_Homesensor);
				} else {
					intent = new Intent(Global.Action_End_Homesensor);
				}
				intent.putExtra("fromMe", true);
				sendBroadcast(intent);
			}
	    });
	    //tml*** iot control
	    boolean suvRunning = mPref.readBoolean("SecurityDetecting", false);
	    ((ImageButton) findViewById(R.id.guard_suv)).setImageResource(suvRunning ? R.drawable.slider_on : R.drawable.slider_off);
	    ((ImageButton) findViewById(R.id.guard_suv)).setEnabled(true);
	    ((ImageButton) findViewById(R.id.guard_suv)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean securityEnabled = mPref.readBoolean("securityEnabled", true);
				if (!securityEnabled) {
					Toast.makeText(getApplicationContext(), getString(R.string.security_off), 0).show();
					return;
				}
				boolean suvRunning = mPref.readBoolean("SecurityDetecting", false);
				
			    ((ImageButton) v).setImageResource(!suvRunning ? R.drawable.slider_on : R.drawable.slider_off);
				if (!suvRunning) {
					Intent intent = new Intent(Global.Action_Start_Surveillance);
	    			intent.putExtra("fromMe", true);
					sendBroadcast(intent);
				}
			}
	    });
	    
	    boolean recordingEnabled=mPref.readBoolean("recordingEnabled", true);
	    ((ImageButton)findViewById(R.id.recording)).setImageResource(recordingEnabled?R.drawable.slider_on:R.drawable.slider_off);
	    ((ImageButton)findViewById(R.id.recording)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				 boolean recordingEnabled=mPref.readBoolean("recordingEnabled", true);
				 recordingEnabled=!recordingEnabled;
				 mPref.write("recordingEnabled", recordingEnabled);
				 ((ImageButton)v).setImageResource(recordingEnabled?R.drawable.slider_on:R.drawable.slider_off);
			}
	    });
	    
	    ((CheckedTextView) findViewById(R.id.sound_detection)).setChecked(mPref.readBoolean("SoundDetection", false));
		((CheckedTextView) findViewById(R.id.sound_detection)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked=((CheckedTextView)v).isChecked();
				((CheckedTextView)v).setChecked(!checked);
				mPref.write("SoundDetection", !checked);
			}
		});
		
		((CheckedTextView) findViewById(R.id.motion_detection)).setChecked(mPref.readBoolean("MotionDetection", true));
		((CheckedTextView) findViewById(R.id.motion_detection)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked=((CheckedTextView)v).isChecked();
				((CheckedTextView)v).setChecked(!checked);
				mPref.write("MotionDetection", !checked);
			}
		});
		
		//tml*** suv sense
		((SeekBar) findViewById(R.id.seekBarSound)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {	
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (progress == 0) {
					((TextView) findViewById(R.id.showSoundSeek)).setText(
							getResources().getString(R.string.bitrateoptionL) + "(1)");
				} else if (progress == 1) {
					((TextView) findViewById(R.id.showSoundSeek)).setText("(2)");
				} else if (progress == 2) {
					((TextView) findViewById(R.id.showSoundSeek)).setText(
							getResources().getString(R.string.bitrateoptionM) + "(3)");
				} else if (progress == 3) {
					((TextView) findViewById(R.id.showSoundSeek)).setText("(4)");
				} else if (progress == 4) {
					((TextView) findViewById(R.id.showSoundSeek)).setText(
							getResources().getString(R.string.bitrateoptionH) + "(5)");
				} else {
					((TextView) findViewById(R.id.showSoundSeek)).setText(
							getResources().getString(R.string.bitrateoptionM) + "(3)");
				}
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mPref.write("SuvSoundSense", seekBar.getProgress());
				Log.i("tmlsuv SOUND SENSE=" + seekBar.getProgress() + "="
						+ ((TextView) findViewById(R.id.showSoundSeek)).getText());
			}
		});
		((SeekBar) findViewById(R.id.seekBarSound)).setProgress(mPref.readInt("SuvSoundSense", 2));

		((SeekBar) findViewById(R.id.seekBarMotion)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {	
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (progress == 0) {
					((TextView) findViewById(R.id.showMotionSeek)).setText(
							getResources().getString(R.string.bitrateoptionL) + "(1)");
				} else if (progress == 1) {
					((TextView) findViewById(R.id.showMotionSeek)).setText("(2)");
				} else if (progress == 2) {
					((TextView) findViewById(R.id.showMotionSeek)).setText(
							getResources().getString(R.string.bitrateoptionM) + "(3)");
				} else if (progress == 3) {
					((TextView) findViewById(R.id.showMotionSeek)).setText("(4)");
				} else if (progress == 4) {
					((TextView) findViewById(R.id.showMotionSeek)).setText(
							getResources().getString(R.string.bitrateoptionH) + "(5)");
				} else {
					((TextView) findViewById(R.id.showMotionSeek)).setText(
							getResources().getString(R.string.bitrateoptionM) + "(3)");
				}
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mPref.write("SuvMotionSense", seekBar.getProgress());
				Log.i("tmlsuv MOTION SENSE=" + seekBar.getProgress() + "="
						+ ((TextView) findViewById(R.id.showMotionSeek)).getText());
			}
		});
		((SeekBar) findViewById(R.id.seekBarMotion)).setProgress(mPref.readInt("SuvMotionSense", 2));
		//***tml
		
		//tml*** suv alarm
		((CheckedTextView) findViewById(R.id.alarmnoise)).setChecked(mPref.readBoolean("AlarmNoise", false));
		((CheckedTextView) findViewById(R.id.alarmnoise)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked = ((CheckedTextView) v).isChecked();
				((CheckedTextView) v).setChecked(!checked);
				mPref.write("AlarmNoise", !checked);
			}
		});
		//tml*** suv mask
		((ToggleButton) findViewById(R.id.suvTLeft)).setChecked(mPref.readBoolean("suvTLeft", true));
		((ToggleButton) findViewById(R.id.suvTRight)).setChecked(mPref.readBoolean("suvTRight", true));
		((ToggleButton) findViewById(R.id.suvBLeft)).setChecked(mPref.readBoolean("suvBLeft", true));
		((ToggleButton) findViewById(R.id.suvBRight)).setChecked(mPref.readBoolean("suvBRight", true));
		
		((ToggleButton) findViewById(R.id.suvTLeft)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean maskCheck = ((ToggleButton) v).isChecked();
				if (minimumSUVArea(!maskCheck)) {
					((ToggleButton) v).setChecked(true);
					return;
				}
				mPref.write("suvTLeft", maskCheck);
				if (maskCheck) {
					((LinearLayout) findViewById(R.id.previewTLeft)).setBackgroundColor(0x9900FF00);
				} else {
					((LinearLayout) findViewById(R.id.previewTLeft)).setBackgroundColor(0);
				}
				Log.d("suv Area TL " + maskCheck);
			}
		});
		
		((ToggleButton) findViewById(R.id.suvTRight)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean maskCheck = ((ToggleButton) v).isChecked();
				if (minimumSUVArea(!maskCheck)) {
					((ToggleButton) v).setChecked(true);
					return;
				}
				mPref.write("suvTRight", maskCheck);
				if (maskCheck) {
					((LinearLayout) findViewById(R.id.previewTRight)).setBackgroundColor(0x7700FF00);
				} else {
					((LinearLayout) findViewById(R.id.previewTRight)).setBackgroundColor(0);
				}
				Log.d("suv Area TR " + maskCheck);
			}
		});
		
		((ToggleButton) findViewById(R.id.suvBLeft)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean maskCheck = ((ToggleButton) v).isChecked();
				if (minimumSUVArea(!maskCheck)) {
					((ToggleButton) v).setChecked(true);
					return;
				}
				mPref.write("suvBLeft", maskCheck);
				if (maskCheck) {
					((LinearLayout) findViewById(R.id.previewBLeft)).setBackgroundColor(0x5500FF00);
				} else {
					((LinearLayout) findViewById(R.id.previewBLeft)).setBackgroundColor(0);
				}
				Log.d("suv Area BL " + maskCheck);
			}
		});
		
		((ToggleButton) findViewById(R.id.suvBRight)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean maskCheck = ((ToggleButton) v).isChecked();
				if (minimumSUVArea(!maskCheck)) {
					((ToggleButton) v).setChecked(true);
					return;
				}
				mPref.write("suvBRight", maskCheck);
				if (maskCheck) {
					((LinearLayout) findViewById(R.id.previewBRight)).setBackgroundColor(0x3300FF00);
				} else {
					((LinearLayout) findViewById(R.id.previewBRight)).setBackgroundColor(0);
				}
				Log.d("suv Area BR " + maskCheck);
			}
		});
		
		((ToggleButton) findViewById(R.id.showpreview)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean show = ((ToggleButton) v).isChecked();
				if (show) {
					startCamera();
				} else {
					stopCamera();
				}
			}
		});
		//***tml
		//tml*** suv save dest
		boolean suvsavedest = mPref.readBoolean("suvsavedest", false);
		String suvsavedest_ext = mPref.read("suvsavedest_ext", getResources().getString(R.string.recording_external));
		
		if (suvsavedest) {
			((RadioButton) findViewById(R.id.suvsave_default)).setChecked(false);
			((RadioButton) findViewById(R.id.suvsave_ext)).setChecked(true);
		} else {
			((RadioButton) findViewById(R.id.suvsave_default)).setChecked(true);
			((RadioButton) findViewById(R.id.suvsave_ext)).setChecked(false);
		}
		
		((RadioGroup) findViewById(R.id.radioSuvSave)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == R.id.suvsave_default) {
					mPref.write("suvsavedest", false);
				} else if (checkedId == R.id.suvsave_ext) {
					String suvsavedest_ext = mPref.read("suvsavedest_ext", getResources().getString(R.string.recording_external));
					boolean pathok = checkSaveDirectory(suvsavedest_ext);
					if (pathok) {
						mPref.write("suvsavedest", true);
					}
				} else {}
				Log.e("tmlssd suvsavedest=" + mPref.readBoolean("suvsavedest") + mPref.read("suvsavedest_ext"));
			}
		});

		if (suvsavedest) {
			String checkext = mPref.read("suvsavedest_ext");
			File folder = new File(checkext);
			if (!folder.exists()) {
				mHandler.post(showSuvSaveTooltip);
				((RadioButton) findViewById(R.id.suvsave_default)).setChecked(true);
				((RadioButton) findViewById(R.id.suvsave_ext)).setChecked(false);
			}
		}
		
		((Button) findViewById(R.id.sel_suvsaveext)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {  //TODO
				Intent it = new Intent(SecurityNewActivity.this, FileBrowerActivity.class);
				//Storage_USBSD SdcardPath_video
				File folder = new File(Global.Storage_USBSD);
				basepath = folder.getPath();
				it.putExtra("folderOnly", "true");
				startActivityForResult(it, 30);
			}
		});
		((TextView) findViewById(R.id.show_suvsaveext)).setText(suvsavedest_ext);
		//***tml
		
		String html=String.format(getString(R.string.security_instruction), "<b>","</b>","<img src='little_guard.png'>");
		html=html.replace("\n", "<br>");
		((TextView)findViewById(R.id.security_instruction)).setText(Html.fromHtml(html, this, null));
		
	    
	    authorizedList=((LinearLayout)findViewById(R.id.family));
	    
		DialerFrame.setFrame(this, findViewById(android.R.id.content));
	    //Bree：点击下载手机App
		((TextView)findViewById(R.id.tv_downloadApp)).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ImageView img = new ImageView(SecurityNewActivity._this);  
				img.setImageResource(R.drawable.app);  
				Builder builder = new AlertDialog.Builder(SecurityNewActivity._this,R.style.CustomDialog);
				AlertDialog create = builder.create();
				create.setView(img, 0, 0, 0, 0);
//				.setView(img) 
//				.setTitle(R.string.scan_download)
//				.setPositiveButton(R.string.determine, null)
				create.show();  
			}
		});
		
		
////// SideBar
        
        ((ImageView)findViewById(R.id.bar1)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it=new Intent(SecurityNewActivity.this, MainActivity.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
      //tml*** beta ui2 X
//        ((ImageView)findViewById(R.id.bar6)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Intent it=new Intent(SecurityNewActivity.this, ShoppingActivity.class);
//				it.putExtra("launchFromSelf", true);
//				startActivity(it);
//				finish();
//			}
//		});
        ((ImageView)findViewById(R.id.bar7)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});
      //tml*** beta ui2 X
//        ((ImageView)findViewById(R.id.bar8)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
////				Intent it=new Intent(SecurityNewActivity.this, LocationSettingActivity.class);
//				Intent it=new Intent(SecurityNewActivity.this, MainBrowser.class);  //tml*** browser save/
//				it.putExtra("launchFromSelf", true);
//				startActivity(it);
//				finish();
//			}
//		});
      //tml*** beta ui2
        ((ImageView)findViewById(R.id.bar9)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//tml*** alpha iot ui
//				Intent it=new Intent(SecurityNewActivity.this, HomeIOTActivity.class);
//				it.putExtra("launchFromSelf", true);
//				startActivity(it);
//				finish();
				Intent it=new Intent(SecurityNewActivity.this, MainActivity.class);
				it.putExtra("launchFromSelf", true);
				it.putExtra("switchToInflate", 5);
				startActivity(it);
				finish();
			}
		});
        ((ImageView)findViewById(R.id.bar9)).setImageAlpha(150);  //temp until icon-faded available
		
	    mHandler.post(arrange);
	    
	    mList = (ListView) findViewById(R.id.history);
	    mHandler.postDelayed(arrangeHistory, 500);
	    
	    ((CheckedTextView) findViewById(R.id.send_sms)).setChecked(mPref.readBoolean("sendSMS", true));
		((CheckedTextView) findViewById(R.id.send_sms)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked=((CheckedTextView)v).isChecked();
				((CheckedTextView)v).setChecked(!checked);
				mPref.write("sendSMS", !checked);
			}
		});
		
		((CheckedTextView) findViewById(R.id.send_picture)).setChecked(mPref.readBoolean("sendPictures", true));
		((CheckedTextView) findViewById(R.id.send_picture)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked=((CheckedTextView)v).isChecked();
				((CheckedTextView)v).setChecked(!checked);
				mPref.write("sendPictures", !checked);
			}
		});
	    
		((CheckedTextView) findViewById(R.id.send_voice_call)).setChecked(mPref.readBoolean("sendCalls", true));
		((CheckedTextView) findViewById(R.id.send_voice_call)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked=((CheckedTextView)v).isChecked();
				((CheckedTextView)v).setChecked(!checked);
				 mPref.write("sendCalls", !checked);
			}
		});
		
		//tml*** beta ui
		boolean hideInstr = mPref.readBoolean("hideSUVinstr", false);
		if (hideInstr) {
			mHandler.post(runHideInstr);
		} else {
			mHandler.post(runShowInstr);
		}
		((ImageView) findViewById(R.id.instrhide)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mHandler.post(runHideInstr);
			}
		});
		((ImageView) findViewById(R.id.instrshow)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mHandler.post(runShowInstr);
			}
		});
		//***tml

        //tml*** beta ui2
        ((RelativeLayout) findViewById(R.id.sidebar_ghost)).setOnGenericMotionListener(sideBarMotionListener);
        ((ImageView) findViewById(R.id.menu_main)).setOnClickListener(new OnClickListener () {
			@Override
			public void onClick(View v) {
				((DrawerLayout) findViewById(R.id.main_content)).openDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));
			}
        });
        
		mHandler.post(checkSecurityStatus);
		//tml*** suv call limit
		boolean alertLimit = mPref.readBoolean("alertCallLimit", false);
		if (alertLimit) {
			mPref.write("alertCallLimit", false);
			mHandler.post(alertCallLimit);
		}
		//tml*** blue io
        ((Button) findViewById(R.id.blue_scan)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mScanning) {
					scanBlueDevice(false);
				} else {
					scanBlueDevice(true);
				}
			}
		});

		boolean jupiter = mPref.readBoolean("JUPITERSTART_BLUE", true);
		if (jupiter) {
			for (int i = 0; i < MAX_VEEPOO_DEVICES; i++) {
				mPref.delect(VEEPOO_tag + i);
			}
			mPref.write("JUPITERSTART_BLUE", false);
			Log.i("myblue  list cleared");
		}
		
        deviceList_scan = (ListView) findViewById(R.id.devicelist_scan);
        mBlueDeviceListAdapter = new BlueDeviceListAdapter(this);
        deviceList_scan.setAdapter(mBlueDeviceListAdapter);
        deviceList_scan.setOnItemClickListener(OnDeviceListClick);

		if (bluetooth_support) {
			mBluetoothAdapter.enable();
			mBlueComm.setOnServiceDiscoverListener(mOnServiceDiscover);
			mBlueComm.setOnDataAvailableListener(mOnDataAvailable);
			((Button) findViewById(R.id.blue_scan)).setEnabled(true);
		} else {
			((Button) findViewById(R.id.blue_scan)).setEnabled(true);
		}
		//***tml
		//li*** Home safeguard
		initHomeSafeguard();
		
		mHandler.post(updateDueDate);
		//tml*** iot status
		boolean suvUploaded = mPref.readBoolean("SUVSTATSUPLOADED", false);
		if (!suvUploaded) {
			String datastr = mPref.read("SUVSTATUS", "");
			if (AireJupiter.getInstance() != null) {
				AireJupiter.getInstance().uploadSuvStatus(datastr);
			}
		}
	}
	//tml*** iot control
	public void updateHomeIOTStatus() {
		mHandler.post(updateHomeIOTStatus);
	}
	
	Runnable updateHomeIOTStatus = new Runnable() {
		@Override
		public void run() {
			boolean securityHomeIOTEnabled = mPref.readBoolean("openHomeSafeguard", false);
			if (!securityHomeIOTEnabled) mPref.write("securityHomeIOT", false);
		    securityHomeIOTEnabled = mPref.readBoolean("securityHomeIOT", false);
		    ((ImageButton) findViewById(R.id.guard_homeiot)).setImageResource(securityHomeIOTEnabled ? R.drawable.slider_on : R.drawable.slider_off);
		    boolean securitySUVEnabled = mPref.readBoolean("SecurityDetecting", false);
		    ((ImageButton) findViewById(R.id.guard_suv)).setImageResource(securitySUVEnabled ? R.drawable.slider_on : R.drawable.slider_off);
		}
	};
	//***tml
	Runnable updateDueDate = new Runnable() {
		public void run() {
			String sub_status = mPref.read("SecurityDueDate", "---");
			//tml|sw*** subscription php
			if (sub_status.startsWith("expired")) {
				((TextView) findViewById(R.id.due_date)).setText(" " + getString(R.string.expired));
			} else if (sub_status.startsWith("failed")) {
				((TextView) findViewById(R.id.due_date)).setText(" " + getString(R.string.security_fail));
			} else if (sub_status.startsWith("success")) {
				((TextView) findViewById(R.id.due_date)).setText(" " + getString(R.string.security_active));
			} else {
				((TextView) findViewById(R.id.due_date)).setText("---");
			}
			
			if (!sub_status.startsWith("success")) {
				((TextView) findViewById(R.id.due_date)).setEnabled(true);
				((TextView) findViewById(R.id.due_date)).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent it = new Intent(SecurityNewActivity.this, ShoppingActivity.class);
						it.putExtra("launchFromSelf", true);
						startActivity(it);
						finish();
					}
				});
			} else {
				((TextView) findViewById(R.id.due_date)).setEnabled(false);
			}
		}
	};
	
	//li*** Home safeguard
	private ListView lv_devices;
	private UsbDeviceMatcher deviceMatcher;
	private List<UsbDevice> devices;
	private BaseAdapter deviceAdapter;
	private String[] deviceCategories;
	private boolean isOpenDeviceManger = false;
	private HomeSafeguadBinder homeSafeguadBinder;
	private ServiceConnection conn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			homeSafeguadBinder = (HomeSafeguadBinder) service;
		}
	};
	
	private void initHomeSafeguard() {
		Intent it = new Intent(getApplicationContext(), HomeSafeguardService.class);
		bindService(it, conn, Context.BIND_AUTO_CREATE);
		
		deviceMatcher = UsbDeviceMatcher.getInstance(getApplicationContext());
		
		
		findViewById(R.id.tab_safeguard).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				swtichToPage(R.id.tab_safeguard);
				((TextView)findViewById(R.id.tab_safeguard)).requestFocus();
			}
		});
		
		CheckBox cb_open_service = (CheckBox)findViewById(R.id.cb_open_service);
		final Button btn_add_device = (Button)findViewById(R.id.btn_add_device);
//		final Button btn_manager_device = (Button)findViewById(R.id.btn_manager_device);
		lv_devices = (ListView) findViewById(R.id.lv_device);

		//li*** Home safeguard, open service
		boolean isOpenSafeguard = mPref.readBoolean("openHomeSafeguard", false);
		cb_open_service.setChecked(isOpenSafeguard);
		btn_add_device.setEnabled(isOpenSafeguard);
//		btn_manager_device.setEnabled(isOpenSafeguard);
		
		cb_open_service.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPref.write("openHomeSafeguard", isChecked);
				if(isChecked){
					homeSafeguadBinder.startService();
					lv_devices.setAdapter(deviceAdapter);
				}else{
					//tml*** iot control
					boolean securityHomeIOTEnabled = mPref.readBoolean("securityHomeIOT", false);
					if (securityHomeIOTEnabled) {
						Intent intent = new Intent(Global.Action_End_Homesensor);
						intent.putExtra("fromMe", true);
						sendBroadcast(intent);
					}

					homeSafeguadBinder.stopService();
					lv_devices.setAdapter(null);
				}
				btn_add_device.setEnabled(isChecked);
//				btn_manager_device.setEnabled(isChecked);
			}
		});

		//li*** Home safeguard, add device
		btn_add_device.setOnClickListener(new OnClickListener() {
			UsbData  _data;
			private AlertDialog dialog;
			@Override
			public void onClick(View v) {
				Builder ad = new AlertDialog.Builder(_this);
				View view = View.inflate(_this, R.layout.add_device_dialog, null);
				final TextView tv1 = (TextView) view.findViewById(R.id.tv1);
				final EditText et_name = (EditText) view.findViewById(R.id.et_name);
				final Spinner s_category = (Spinner) view.findViewById(R.id.s_category);
				final Button btn_confirm = (Button) view.findViewById(R.id.btn_confirm);
				final Button btn_cancel = (Button) view.findViewById(R.id.btn_cancel);
				final ReadDataListener readDataListener = new ReadDataListener() {
					@Override
					public void onReadData(UsbData data) {
						_data = data;
						tv1.setText(getString(R.string.device_found)+ Integer.toHexString(data.getAddress()));
					}
				};
				homeSafeguadBinder.registerReadDataListener(readDataListener);
				btn_confirm.setOnClickListener( new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if(_data == null){
							Toast.makeText(getApplicationContext(), getString(R.string.device_not_found), 0).show();
							return;
						}
						int address = _data.getAddress();
						String name = et_name.getText().toString().trim();
						int category = s_category.getSelectedItemPosition();
						if(TextUtils.isEmpty(name) || category == AdapterView.INVALID_POSITION
								|| name.contains(":") || name.contains(";")){
							Toast.makeText(getApplicationContext(), getString(R.string.device_invalidname), 0).show();
							return;
						}
						UsbDevice device = new UsbDevice(category, name, address, _data,true);
						if(deviceMatcher.addUsbDevice(device)){  
							devices.add(device);
							Toast.makeText(getApplicationContext(), getString(R.string.device_add_success), 0).show();
							Log.d(getString(R.string.device_add)+": " + device.toString());
							_data = null;
							homeSafeguadBinder.unregisterReadDataListener(readDataListener);
							deviceAdapter.notifyDataSetChanged();
							
							uploadSuvStatus();
							dialog.dismiss();
						}else{
							Toast.makeText(getApplicationContext(), getString(R.string.device_exist), 0).show();
							Log.d(getString(R.string.device_exist));
						}
						
					}

					

				});
				btn_cancel.setOnClickListener( new OnClickListener() {
					@Override
					public void onClick(View v) {
						homeSafeguadBinder.unregisterReadDataListener(readDataListener);
						dialog.dismiss();
					}
				});
				ad.setView(view);
				dialog = ad.show();
			}
		});
		
//		btn_manager_device.setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				isOpenDeviceManger = !isOpenDeviceManger;
//				deviceAdapter.notifyDataSetChanged();
//			}
//		});

		//li*** Home safeguard, list of device
		devices = deviceMatcher.getAllUsbDevice();
		deviceAdapter = new DeviceAdapter();
		deviceCategories = getResources().getStringArray(R.array.device);
		lv_devices.setAdapter(deviceAdapter);
		
	}
	private void uploadSuvStatus() {
		if (homeSafeguadBinder != null) {
			String datastr = homeSafeguadBinder.setUsbDeviceStatus(SecurityNewActivity.this, 0, false);
			if (AireJupiter.getInstance() != null) {
				AireJupiter.getInstance().uploadSuvStatus(datastr);
			}
		}
	}
	class DeviceAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			return devices.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if(convertView == null){
				convertView = View.inflate(getApplicationContext(), R.layout.item_device, null);
				holder = new ViewHolder();
				holder.tv_device = (TextView) convertView.findViewById(R.id.tv_device);
				holder.tv_category = (TextView) convertView.findViewById(R.id.tv_category);
				holder.cb_open_device = (CheckBox) convertView.findViewById(R.id.cb_open_device);
				holder.btn_edit = (Button) convertView.findViewById(R.id.btn_edit);
				holder.btn_delete = (Button) convertView.findViewById(R.id.btn_delete);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			final UsbDevice device = devices.get(position);
			holder.tv_device.setText(device.getName());
			holder.tv_category.setText(deviceCategories[device.getCategory()]);
			holder.cb_open_device.setChecked(device.isOpen());
			
			holder.cb_open_device.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					device.setOpen(isChecked);
					deviceMatcher.updateUsbDevice(device);
					
					//tml*** iot status
					uploadSuvStatus();
				}
			});
			
			holder.btn_edit.setOnClickListener(new OnClickListener() {
				private AlertDialog dialog;

				@Override
				public void onClick(View v) {
					final int address = device.getAddress();
					int category = device.getCategory();
					String name = device.getName();
					
					Builder ad = new AlertDialog.Builder(_this);
					View view = View.inflate(_this, R.layout.add_device_dialog, null);
					final TextView tv1 = (TextView) view.findViewById(R.id.tv1);
					final TextView tv_title = (TextView) view.findViewById(R.id.tv_title);
					final EditText et_name = (EditText) view.findViewById(R.id.et_name);
					final Spinner s_category = (Spinner) view.findViewById(R.id.s_category);
					final Button btn_confirm = (Button) view.findViewById(R.id.btn_confirm);
					final Button btn_cancel = (Button) view.findViewById(R.id.btn_cancel);
					
					tv1.setText(getString(R.string.device)+ Integer.toHexString(address));
					et_name.setText(name);
					s_category.setSelection(category);
					tv_title.setText(getString(R.string.device_edit));
					btn_confirm.setText(getString(R.string.device_edit));
					btn_confirm.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							if (address == 0) {
								Toast.makeText(getApplicationContext(), getString(R.string.device_not_found), 0).show();
								return;
							}
							String name = et_name.getText().toString().trim();
							int category = s_category.getSelectedItemPosition();
							if(TextUtils.isEmpty(name) || category == AdapterView.INVALID_POSITION){
								Toast.makeText(getApplicationContext(), R.string.device_empty_notify, 0).show();
								return;
							}
							device.setCategory(category);
							device.setName(name);
							if(deviceMatcher.updateUsbDevice(device)){  
								Toast.makeText(getApplicationContext(),getString( R.string.device_edit_success), 0).show();
								Log.d(getString(R.string.device_edit_success)+": " + device.toString());
								deviceAdapter.notifyDataSetChanged();
								uploadSuvStatus();
								dialog.dismiss();
							}else{
								Toast.makeText(getApplicationContext(), getString(R.string.device_exist), 0).show();
								Log.d(getString(R.string.device_exist));
							}
							
						}
					});
					btn_cancel.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							dialog.dismiss();
						}
					});
					ad.setView(view);
					dialog = ad.show();
				}
			});
			holder.btn_delete.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
					deviceMatcher.deleteUsbDevice(devices.remove(position).getAddress());
					deviceAdapter.notifyDataSetChanged();
					
					//tml*** iot status
					uploadSuvStatus();
					
				}
			});
			
//			holder.cb_open_device.setVisibility(isOpenDeviceManger ? View.VISIBLE : View.GONE);
//			holder.btn_delete.setVisibility(isOpenDeviceManger ? View.VISIBLE : View.GONE);
//			holder.btn_edit.setVisibility(isOpenDeviceManger ? View.VISIBLE : View.GONE);
			
			return convertView;
		}
		
		class ViewHolder{
			public CheckBox cb_open_device;
			public Button btn_delete;
			public Button btn_edit;
			public TextView tv_device;
			public TextView tv_category;
		}
	}
	//tml|li*** blue io
	public static boolean blueDisconnect = false;
	public void clickBlueListview (int pos) {
		deviceList_scan.performItemClick(deviceList_scan.getAdapter().getView(pos, null, null), pos, deviceList_scan.getItemIdAtPosition(pos));
	}
	
    private OnItemClickListener OnDeviceListClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if (bluetooth_support) {
				final BluetoothDevice device = mBlueDeviceListAdapter.getDevice(position);
				if (device == null) {
					Log.e("myblue  selected device null?");
					return;
				}
				
				String dName = device.getName();
				String dAddr = device.getAddress();
				Log.i("myblue  selected device=" + dName + " " + dAddr);
				
				if (dName == null || dAddr == null) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(SecurityNewActivity.this, R.string.blue_unknown_device, Toast.LENGTH_SHORT).show();
						}
					});
					return;
				}
				
				dName = dName.toLowerCase();
				if (dName.contains(VEEPOO))
				{
					int pos = getWatchSaveSpot(dAddr.toLowerCase(), VEEPOO);
					if (pos != -1) {
						Log.e("myblue  already connected");
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(SecurityNewActivity.this, R.string.blue_alreadyconn_device, Toast.LENGTH_SHORT).show();
							}
						});
						return;
					}
					pos = getWatchSaveSpot(null, VEEPOO);
					if (pos == -1) {
						Log.e("myblue  too many active Veepoo devices");
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(SecurityNewActivity.this, R.string.blue_max_device, Toast.LENGTH_SHORT).show();
							}
						});
						return;
					}
				}
				else if (dName.contains(ACCULIFE))
				{
//					int pos = getWatchSaveSpot(dAddr.toLowerCase(), ACCULIFE);
//					if (pos != -1) {
//						Log.e("myblue  already connected");
//						runOnUiThread(new Runnable() {
//							@Override
//							public void run() {
//								Toast.makeText(SecurityNewActivity.this, R.string.blue_alreadyconn_device, Toast.LENGTH_SHORT).show();
//							}
//						});
//						return;
//					}
//					pos = getWatchSaveSpot(null, ACCULIFE);
//					if (pos == -1) {
//						Log.e("myblue  too many active Acculife devices");
//						runOnUiThread(new Runnable() {
//							@Override
//							public void run() {
//								Toast.makeText(SecurityNewActivity.this, R.string.blue_max_device, Toast.LENGTH_SHORT).show();
//							}
//						});
//						return;
//					}
				}
				else
				{
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(SecurityNewActivity.this, R.string.blue_unknown_device, Toast.LENGTH_SHORT).show();
						}
					});
					return;
				}
				
				if (mScanning) {
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					mScanning = false;
				}

				mBlueComm.connect(dAddr);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_connecting_device));
					}
				});
			}
		}
	};
	
	private BlueComm.OnDataAvailableListener mOnDataAvailable = new OnDataAvailableListener() {
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			String dName = gatt.getDevice().getName();
			final String dAddr = gatt.getDevice().getAddress();
			UUID uuid = characteristic.getUuid();
			
			if (dName == null || dAddr == null || uuid == null) {
				Log.e("myblue  onData read n/a? " + dName + " " + dAddr + " " + uidName(uuid));
				return;
			}
			
			dName = dName.toLowerCase();
			if (dName.contains(VEEPOO)
					|| uuid.equals(VEEPOO_DEVICE_DAT)
					|| uuid.equals(VEEPOO_HR_DAT))
			{
				byte[] value = characteristic.getValue();
				if (value != null) {
					if (VEEPOO_DEVICE_DAT.equals(uuid))
					{
						int length = value.length;
						Log.d("myblue  onData " + dName + " (" + uidName(uuid) + ") read" + length + " <- " + Utils.bytesToHexString(value));
						if (length > 3 && (byte) 1 == value[3]) {  //a1ffff01 1 value[3]
							readPW_VEEPOO = false;
							int pos = getWatchSaveSpot(null, VEEPOO);
							Log.i("myblue  verified! saving to " + VEEPOO_tag + pos);
							mPref.write(VEEPOO_tag + pos, dName + "\n" + dAddr);
							
							if (_this != null)
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(SecurityNewActivity.this, R.string.blue_connected_device, Toast.LENGTH_SHORT).show();
										((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_connected_device));
										
										String[] info = getWatchDeviceInfo(dAddr, VEEPOO);
										if (info != null) {
											((TextView) findViewById(R.id.heart_device1_name)).setText(info[0]);
											((TextView) findViewById(R.id.heart_device1_info2)).setText(getString(R.string.blue_acquiring_data));
										}
									}
								});
							readHeartRate(gatt, dName);
						} else {
							Log.e("myblue  pwd check fail");
//							if (_this != null)
//								runOnUiThread(new Runnable() {
//									@Override
//									public void run() {
//										Toast.makeText(SecurityNewActivity.this, R.string.blue_connectfail_veepoo, Toast.LENGTH_SHORT).show();
//										((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_connectfail_veepoo));
//									}
//								});
						}
						
					}
					else if (VEEPOO_HR_DAT.equals(uuid))
					{
						String pulseData = Utils.bytesToHexString(value);
						if (pulseData.length() >= 6)
						{
							int pulse = Integer.parseInt(pulseData.substring(2, 4), 16);
							int pulsestatus = Integer.parseInt(pulseData.substring(4, 6), 16);
							Log.d("myblue  onData " + dName + " (" + uidName(uuid) + ") read <- " + pulseData + " > " + pulse + " -" + pulsestatus);
							int pos = getWatchSaveSpot(dAddr, VEEPOO);
							if (pos == -1) return;
							
							if (pulse == 0) {
								mPref.write(VEEPOO_tag + pos + "P", Integer.toString(pulse));
							} else if (pulse == 1) {
								mPref.write(VEEPOO_tag + pos + "P", getString(R.string.heart_error) + " E01");
							} else if (pulse == 2) {
								mPref.write(VEEPOO_tag + pos + "P", getString(R.string.heart_error) + " E02");
							} else if (pulse == 3) {
								mPref.write(VEEPOO_tag + pos + "P", getString(R.string.heart_abnormal));
							} else {
								mPref.write(VEEPOO_tag + pos + "P", Integer.toString(pulse));
							}
							
							if (pulsestatus == 0) {
								mPref.write(VEEPOO_tag + pos + "S", getString(R.string.heart_normal));
							} else {
								mPref.write(VEEPOO_tag + pos + "S", getString(R.string.heart_arrhythmia));
							}
							
							if (pulse == 0 && iniAcquiringHR) return;
							iniAcquiringHR = false;
							
							if (_this != null)
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										String[] info = getWatchDeviceInfo(dAddr, VEEPOO);
										if (info != null) {
											((TextView) findViewById(R.id.heart_device1_info)).setText(info[1]);
											((TextView) findViewById(R.id.heart_device1_info2)).setText("(" + info[2] + ")");
										}
									}
								});
							
							if (pulse == 3 || pulsestatus == 1) {
								String heart = "";
								if (pulse == 3) heart = getString(R.string.heart_abnormal);
								if (pulsestatus == 1) heart = getString(R.string.heart_arrhythmia);
								startSendingMsg(VEEPOO, getString(R.string.watch_heart), heart);
							}
						}
						else
						{
							Log.e("myblue  onData " + dName + " (" + uidName(uuid) + ") read ERROR? " + pulseData);
						}
					}
					else
					{
						Log.e("myblue  onData read n/a? " + dName + " " + uidName(uuid));
					}
				} else {
					Log.e("myblue  onData read NULL? " + dName + " " + uidName(uuid));
				}
			}
			else if (dName.contains(ACCULIFE))
			{
				//TODO
				byte[] value = characteristic.getValue();
				if (value != null) {

					int length = value.length;
					String data = Utils.bytesToHexString(value);
					Log.d("myblue  onData " + dName + " (" + uidName(uuid) + ") read" + length + " <- " + data);
					//TODO
				} else {
					Log.e("myblue  onData read NULL? " + dName + " " + uidName(uuid));
				}
			}
			else
			{
				Log.e("myblue  onData read ... unknown: " + dName + " " + uuid.toString());
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			String dName = gatt.getDevice().getName();
			UUID uuid = characteristic.getUuid();

			if (dName == null || uuid == null) {
				Log.e("myblue  onData write n/a? " + dName + " " + uidName(uuid));
				return;
			}
			
			dName = dName.toLowerCase();
			if (dName.contains(VEEPOO)
					|| uuid.equals(VEEPOO_DEVICE_CFG)
					|| uuid.equals(VEEPOO_HR_CFG))
			{
				iniAcquiringHR = true;
				byte[] value = characteristic.getValue();
				Log.d("myblue  onData " + dName + " (" + uidName(uuid) + ") write -> " + Utils.bytesToHexString(value));
			}
			else if (dName.contains(ACCULIFE))
			{
				iniAcquiringHR = true;
				byte[] value = characteristic.getValue();
				Log.d("myblue  onData " + dName + " (" + uidName(uuid) + ") write -> " + Utils.bytesToHexString(value));
			}
			else
			{
				Log.e("myblue  onData write ... unknown: " + dName + " " + uuid.toString());
			}
		}
	};
	
	private void scanBlueDevice(final boolean enable) {
		if (bluetooth_support) {
			if (enable) {
				if (mBlueDeviceListAdapter != null)
					mBlueDeviceListAdapter.clear();
				((ListView) findViewById(R.id.devicelist_scan)).setVisibility(View.VISIBLE);
				Log.i("myblue  begin device scanning");
				scanTimer = SCAN_PERIODi;
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						Log.i("myblue  device scanning timeout");
						mScanning = false;
						mBluetoothAdapter.stopLeScan(mLeScanCallback);
					}
				}, SCAN_PERIOD);

				mScanning = true;
				mHandler.post(scanCountdown);
				mBluetoothAdapter.startLeScan(mLeScanCallback);
			} else {
				Log.i("myblue  stop device scanning");
				mHandler.removeCallbacks(scanCountdown);
				mHandler.postDelayed(hide_blue_scanresults, 5000);
				if (_this != null)
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							((Button) findViewById(R.id.blue_scan)).setText(getString(R.string.blue_scan));
//							((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_scanstop));
						}
					});
				mScanning = false;
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
			}
		}
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mBlueDeviceListAdapter.addDevice(device);
					mBlueDeviceListAdapter.notifyDataSetChanged();
				}
			});
		}
	};

	private BlueComm.OnServiceDiscoverListener mOnServiceDiscover = new OnServiceDiscoverListener() {
		@Override
		public void onServiceDiscover(BluetoothGatt gatt) {
			String dName = gatt.getDevice().getName();
			
			if (dName == null) {
				Log.e("myblue  service discover ... unknown: " + dName);
				return;
			}
			
			if (SecurityNewActivity.blueDisconnect) {
				Log.e("myblue  disconnect, onDiscover aborted");
				SecurityNewActivity.blueDisconnect = false;
				return;
			}
			
			dName = dName.toLowerCase();
			if (dName.contains(VEEPOO))
			{
				checkPassword(gatt, dName);
			}
			else if (dName.contains(ACCULIFE))
			{
				readHeartRate(gatt, dName);
			}
			else
			{
				Log.e("myblue  service discover ... unknown: " + dName);
			}
		}
	};
	
	private Runnable scanCountdown = new Runnable () {
		@Override
		public void run() {
			if (mScanning) {
				mHandler.removeCallbacks(hide_blue_scanresults);
				((Button) findViewById(R.id.blue_scan)).setText(Integer.toString(scanTimer));
				if (scanTimer == SCAN_PERIODi)
					((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_scanning));
				scanTimer--;
				if (scanTimer >= 0) {
					mHandler.postDelayed(scanCountdown, 1000);
				} else {
					mHandler.postDelayed(hide_blue_scanresults, 5000);
				}
			} else {
				((Button) findViewById(R.id.blue_scan)).setText(getString(R.string.blue_scan));
				if (!readPW_VEEPOO) ((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_scanstop));
				mHandler.postDelayed(hide_blue_scanresults, 5000);
			}
		}
	};
	
	private Runnable hide_blue_scanresults = new Runnable () {
		@Override
		public void run() {
			((Button) findViewById(R.id.blue_scan)).setText(getString(R.string.blue_scan));
			if (!readPW_VEEPOO) ((TextView) findViewById(R.id.blue_scan_status)).setText("");
			if (mBlueDeviceListAdapter != null)
				mBlueDeviceListAdapter.clear();
			((ListView) findViewById(R.id.devicelist_scan)).setVisibility(View.GONE);
		}
	};

	private void checkPassword(BluetoothGatt gatt, String devName) {
		if (devName.contains(VEEPOO))
		{
			final BluetoothGattService service = gatt.getService(VEEPOO_DEVICE_SRV);
			BluetoothGattCharacteristic pswChar = service.getCharacteristic(VEEPOO_DEVICE_CFG);
			byte[] psw = {(byte) 0xa1, 0x00, 0x00, 0x00};
			Log.i("myblue  set pw " + VEEPOO + " -> " + Utils.bytesToHexString(psw));
			
			pswChar.setValue(psw);
			mBlueComm.writeCharacteristic(pswChar);
			
			new Thread(new Runnable() {

				@Override
				public void run() {
					readPW_VEEPOO = true;
					BluetoothGattCharacteristic data = service.getCharacteristic(VEEPOO_DEVICE_DAT);

					Log.i("myblue  confirming pw: " + VEEPOO);
					if (_this != null)
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_verifying_device));
							}
						});
					
					boolean readok = true;
					int c = 0;
					while (readPW_VEEPOO && readok && c < 5) {
						c++;
						if (!readPW_VEEPOO) break;
						MyUtil.Sleep(2000);
						readok = mBlueComm.readCharacteristic(data);
					}
					
					if (c == 5)
						if (_this != null)
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(SecurityNewActivity.this, R.string.blue_connectfail_veepoo, Toast.LENGTH_SHORT).show();
									((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_connectfail_veepoo));
								}
							});
					readPW_VEEPOO = false;
					Log.i("myblue  confirm pw - finished: " + VEEPOO + "  /" + c + "/" + readPW_VEEPOO + "/" + readok);
				}
			}).start();
		}
		else if (devName.contains(ACCULIFE))
		{
			//TODO do they have a password?
		}
	}

	private void readHeartRate(BluetoothGatt gatt, String devName) {
		if (devName.contains(VEEPOO))
		{
			final BluetoothGattService gattService = gatt.getService(VEEPOO_HR_SRV);
			
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (readHR_VEEPOO) return;
					readHR_VEEPOO = true;
					BluetoothGattCharacteristic config = gattService.getCharacteristic(VEEPOO_HR_CFG);
					BluetoothGattCharacteristic data = gattService.getCharacteristic(VEEPOO_HR_DAT);
					byte[] value = new byte[2];
					value[0] = (byte) 0xaa;
					value[1] = (byte) 0x01;
					Log.i("myblue  request data " + VEEPOO + " -> " + Utils.bytesToHexString(value));
					
					config.setValue(value);
					mBlueComm.writeCharacteristic(config);
					
					boolean readok = true;
					while (readHR_VEEPOO && readok) {
						if (!readHR_VEEPOO) break;
						MyUtil.Sleep(1000);
						readok = mBlueComm.readCharacteristic(data);
					}

					Log.i("myblue  request data - finished: " + VEEPOO + "  /" + readHR_VEEPOO + "/" + readok);
				}
			}).start();
		}
		else if (devName.contains(ACCULIFE))
		{
			final BluetoothGattService gattService = gatt.getService(ACCULIFE_HR_SRV);
			
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (readHR_ACCULIFE) return;
					readHR_ACCULIFE = true;
					BluetoothGattCharacteristic config = gattService.getCharacteristic(ACCULIFE_HR_CFG);
					BluetoothGattCharacteristic data = gattService.getCharacteristic(ACCULIFE_HR_DAT);
					byte[] value = new byte[2];
					value[0] = (byte) 0xaa;
					value[1] = (byte) 0x01;
					Log.i("myblue  request data " + ACCULIFE + " -> " + Utils.bytesToHexString(value));
					
					config.setValue(value);
					mBlueComm.writeCharacteristic(config);
					
					boolean readok = true;
					while (readHR_ACCULIFE && readok) {
						if (!readHR_ACCULIFE) break;
						MyUtil.Sleep(1000);
						if (!readHR_ACCULIFE) break;
						readok = mBlueComm.readCharacteristic(data);
					}

					Log.i("myblue  request data - finished: " + ACCULIFE + "  /" + readHR_ACCULIFE + "/" + readok);
				}
			}).start();
		}
	}
	
	public void onDisconnected (String addr, String watch, int pos) {
		String[] info = getWatchDeviceInfo(addr, watch);
		
		if (info != null) {
			if (info[0].contains(VEEPOO + "0") || pos == 0)
			{
				mHandler.postDelayed(new Runnable () {
					@Override
					public void run() {
						((TextView) findViewById(R.id.heart_device1_info)).setText("--");
						((TextView) findViewById(R.id.heart_device1_info2)).setText(getString(R.string.blue_disconnected_device));
					}
				}, 500);
			}
			else if (info[0].contains(ACCULIFE + "0") || pos == 0)
			{
				mHandler.postDelayed(new Runnable () {
					@Override
					public void run() {
						((TextView) findViewById(R.id.heart_device1_info)).setText("--");
						((TextView) findViewById(R.id.heart_device1_info2)).setText(getString(R.string.blue_disconnected_device));
					}
				}, 500);
			}
		}
		
	}
	
	//***tml
	//tml*** beta ui
	Runnable runShowInstr = new Runnable() {
		public void run() {
			((ImageView) findViewById(R.id.instrhide)).setVisibility(View.VISIBLE);
			((ImageView) findViewById(R.id.instrshow)).setVisibility(View.GONE);
			mPref.write("hideSUVinstr", false);
			((LinearLayout) findViewById(R.id.suvinstr_view)).setVisibility(View.VISIBLE);
			((ImageView) findViewById(R.id.guard_pic)).getLayoutParams().height = (int) (200 * mDensity);
			((ImageView) findViewById(R.id.guard_pic)).getLayoutParams().width = (int) (240 * mDensity);
		}
	};

	Runnable runHideInstr = new Runnable() {
		public void run() {
			((ImageView) findViewById(R.id.instrhide)).setVisibility(View.GONE);
			((ImageView) findViewById(R.id.instrshow)).setVisibility(View.VISIBLE);
			mPref.write("hideSUVinstr", true);
			((LinearLayout) findViewById(R.id.suvinstr_view)).setVisibility(View.GONE);
			((ImageView) findViewById(R.id.guard_pic)).getLayoutParams().height = (int) (50 * mDensity);
			((ImageView) findViewById(R.id.guard_pic)).getLayoutParams().width = (int) (60 * mDensity);
		}
	};
	//***tml
	
	Runnable checkSecurityStatus = new Runnable() {
		public void run() {
			if (AireJupiter.getInstance() != null)
				AireJupiter.getInstance().requestSecuritySubscription();
			
//			mHandler.postDelayed(securityEnableRefresh, 10000);  //tml|sw*** new subs model, X
		}
	};
	//tml|sw*** new subs model, X
//	Runnable securityEnableRefresh = new Runnable() {
//		public void run() {
//		    String securitySubscription = mPref.read("SecurityDueDate", "---");
//		    //tml|sw*** subscription php
//		    if (!securitySubscription.startsWith("success")) {
////		    	Log.e("suv disabled");
//		    	mPref.write("securityEnabled", false);
//		    	((ImageButton) findViewById(R.id.guard)).setImageResource(R.drawable.slider_off);
//		    	((ImageButton) findViewById(R.id.guard)).setEnabled(false);
//		    	((ImageButton) findViewById(R.id.guard)).setOnClickListener(null);
//			} else {
////				Log.e("suv enabled");
//		    	mPref.write("securityEnabled", true);
//		    	boolean securityEnabled = mPref.readBoolean("securityEnabled", true);
//			    ((ImageButton) findViewById(R.id.guard)).setImageResource(securityEnabled ? R.drawable.slider_on : R.drawable.slider_off);
//		    	((ImageButton) findViewById(R.id.guard)).setEnabled(true);
//			    ((ImageButton) findViewById(R.id.guard)).setOnClickListener(new OnClickListener() {
//					@Override
//					public void onClick(View v) {
//						 boolean securityEnabled = mPref.readBoolean("securityEnabled", true);
//						 securityEnabled =! securityEnabled;
//						 mPref.write("securityEnabled", securityEnabled);
//						 ((ImageButton) v).setImageResource(securityEnabled ? R.drawable.slider_on : R.drawable.slider_off);
//					}
//			    });
//		    }
//		}
//	};
	private void swtichToPage(int id)
	{
		((TextView)findViewById(R.id.tab_monitoring)).setBackgroundResource((id==R.id.tab_monitoring)?R.drawable.tab_static:R.drawable.optionbtn);
		((TextView)findViewById(R.id.tab_history)).setBackgroundResource((id==R.id.tab_history)?R.drawable.tab_static:R.drawable.optionbtn);
		((TextView)findViewById(R.id.tab_setting)).setBackgroundResource((id==R.id.tab_setting)?R.drawable.tab_static:R.drawable.optionbtn);
		((TextView)findViewById(R.id.tab_safeguard)).setBackgroundResource((id==R.id.tab_safeguard)?R.drawable.tab_static:R.drawable.optionbtn);  //li*** Home safeguard
		
		((TextView)findViewById(R.id.tab_monitoring)).setTextColor((id==R.id.tab_monitoring)?0xff3997d5:0xff2e3545);
		((TextView)findViewById(R.id.tab_history)).setTextColor((id==R.id.tab_history)?0xff3997d5:0xff2e3545);
		((TextView)findViewById(R.id.tab_setting)).setTextColor((id==R.id.tab_setting)?0xff3997d5:0xff2e3545);
		((TextView)findViewById(R.id.tab_safeguard)).setTextColor((id==R.id.tab_safeguard)?0xff3997d5:0xff2e3545);  //li*** Home safeguard
		
		((LinearLayout)findViewById(R.id.monitoring)).setVisibility((id==R.id.tab_monitoring)?View.VISIBLE:View.GONE);
		((LinearLayout)findViewById(R.id.history_page)).setVisibility((id==R.id.tab_history)?View.VISIBLE:View.GONE);
		((LinearLayout)findViewById(R.id.setting_page)).setVisibility((id==R.id.tab_setting)?View.VISIBLE:View.GONE);
		((LinearLayout)findViewById(R.id.home_safeguard_page)).setVisibility((id==R.id.tab_safeguard)?View.VISIBLE:View.GONE);  //li*** Home safeguard
		
		scanBlueDevice(false);  //tml*** blue io
	}
	
	 @Override
    public Drawable getDrawable(String arg0) {
        int id = 0;

        if(arg0.equals("little_guard.png")){
            id = R.drawable.little_guard;
        }
        LevelListDrawable d = new LevelListDrawable();
        Drawable empty = getResources().getDrawable(id);
        d.addLevel(0, 0, empty);
        d.setBounds(0, 0, empty.getIntrinsicWidth(), empty.getIntrinsicHeight());
        return d;
    }
	
	LayoutInflater inflater;
	Runnable arrange=new Runnable()
	{
		public void run()
		{	
			authorizedList.removeAllViews();
			if (instants.size()==0)
			{
				TextView v=new TextView(SecurityNewActivity.this);
				v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				v.setTextColor(0xff567d98);
				v.setGravity(Gravity.CENTER_HORIZONTAL);
				v.setPadding(0, (int)(30.f*mDensity), 0, 0);
				v.setText(R.string.no_instant_contact);
				authorizedList.addView(v);
				return;
			}
			
			inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			View v2 = inflater.inflate(R.layout.inflate_auto_answer_ind, null, false);
			authorizedList.addView(v2);
			((Button)v2.findViewById(R.id.done2)).setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					mHandler.post(showAutoAnswerTooltip);
				}
			});
			//tml*** auto2
			((Button)v2.findViewById(R.id.done3)).setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					mHandler.post(showAutoAnswerTooltip2);
				}
			});
//			((Button)v2.findViewById(R.id.done)).setOnClickListener(new OnClickListener(){
//				@Override
//				public void onClick(View v) {
//					mHandler.post(showAutoAnswerTooltip0);
//				}
//			});
			
			
			for (String address: instants)
			{
				View v = inflater.inflate(R.layout.authorize_cell, null, false);
				authorizedList.addView(v);
				
				String global=MyTelephony.attachPrefix(SecurityNewActivity.this, address);
			
				int idx=mADB.getIdxByAddress(global);
				ImageView iv=(ImageView)v.findViewById(R.id.photo);
				EditText name=((EditText)v.findViewById(R.id.displayname));
				if (idx>0 && iv!=null)
				{	
					Drawable photo=ImageUtil.getUserPhoto(SecurityNewActivity.this, idx);
					if (photo!=null)
						iv.setImageDrawable(photo);
					else
						iv.setImageResource(R.drawable.bighead);
					iv.setVisibility(View.VISIBLE);
					
					String displayname=mADB.getNicknameByAddress(global);
					name.setText(displayname);
					name.setEnabled(false);
				}
				else{
					if (address.length()>6 && global.startsWith("+"))
					{
						name.setText(global);
						iv.setVisibility(View.INVISIBLE);
						name.setEnabled(false);
					}
					else{
						iv.setVisibility(View.INVISIBLE);
						name.setEnabled(true);
						name.setText(null);
						name.setHint(R.string.phonenumber_hint);
						name.setTag("++++");
					}
				}
				
//				((ImageView)v.findViewById(R.id.delete)).setTag(global);
				((ImageView)v.findViewById(R.id.delete)).setTag(address);  //li*** contact del bug
				
				((ImageView)v.findViewById(R.id.delete)).setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						String address=(String)v.getTag();
						for (String add: instants)
						{
							if (address.equals(add))
							{
								instants.remove(add);
								break;
							}
						}
						sendeeAddress=address;
//						mHandler.postDelayed(deauthorizeByTCPMessage, 1500);
						new Thread(deauthorizeByTCPMessage).start();
						mPref.writeArray("instants", instants);
//						mPref.write("autoAnswer:"+address, false);
//						mPref.write("autoAnswer2:"+address, false); //tml*** auto2/
						mPref.delect("autoAnswer:"+address);
						mPref.delect("autoAnswer2:"+address); //tml*** auto2/
						mHandler.post(arrange);
					}
				});
				
//				boolean allowed=mPref.readBoolean("autoAnswer:"+global, false);
//				((ImageView)v.findViewById(R.id.autoanswer)).setImageResource(allowed?R.drawable.checked:R.drawable.unchecked);
//				((ImageView)v.findViewById(R.id.autoanswer)).setTag(global);
//				
//				((ImageView)v.findViewById(R.id.autoanswer)).setOnClickListener(new OnClickListener(){
//					@Override
//					public void onClick(View v) {
//						String address=(String)v.getTag();
//						String key="autoAnswer:"+address;
//						boolean allowed=mPref.readBoolean(key, false);
//						if (allowed)
//						{
//							mPref.write(key, false);
//							((ImageView)v).setImageResource(R.drawable.unchecked);
//						}
//						else{
//							mPref.write(key, true);
//							((ImageView)v).setImageResource(R.drawable.checked);
//						}
//					}
//				});
				//tml*** auto2
				boolean allowed = mPref.readBoolean("autoAnswer:" + global, true);
				boolean allowed2 = mPref.readBoolean("autoAnswer2:" + global, false);
				if (allowed) mPref.write("autoAnswer:" + global, true);
				if (allowed2) mPref.write("autoAnswer2:" + global, true);
				
				((RadioGroup) v.findViewById(R.id.radioSex)).setTag(global);
//				((RadioButton) v.findViewById(R.id.autoanswer0)).setChecked((!allowed && !allowed2)? true : false);
//				((RadioButton) v.findViewById(R.id.autoanswer0)).setTag(global);
				((RadioButton) v.findViewById(R.id.autoanswer)).setChecked(allowed? true : false);
				((RadioButton) v.findViewById(R.id.autoanswer)).setTag(global);
				((RadioButton) v.findViewById(R.id.autoanswer2)).setChecked(allowed2? true : false);
				((RadioButton) v.findViewById(R.id.autoanswer2)).setTag(global);
				((RadioGroup) v.findViewById(R.id.radioSex)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						Object vObj1 = group.getTag();
						String address = (String) vObj1;
						String key = "autoAnswer:" + address;
						String key2 = "autoAnswer2:" + address;
						if (checkedId == R.id.autoanswer) {
							mPref.write(key, true);
							mPref.write(key2, false);
						} else if (checkedId == R.id.autoanswer2) {
							mPref.write(key, false);
							mPref.write(key2, true);
//						} else if (checkedId == R.id.autoanswer0) {
//							mPref.write(key, false);
//							mPref.write(key2, false);
						} else {
							mPref.write(key, false);
							mPref.write(key2, false);
						}
						Log.i("suv state:" + address + ":" + Boolean.toString(mPref.readBoolean(key, false))
								+ "|" + Boolean.toString(mPref.readBoolean(key2, false)));
					}
				});
				((RadioButton) v.findViewById(R.id.autoanswer)).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v0) {
//						Object vObj1 = v0.getTag();
//						String address = (String) vObj1;
//						String key = "autoAnswer:" + address;
//						boolean allowed = mPref.readBoolean(key, false);
//						String key2 = "autoAnswer2:" + address;
//						boolean allowed2 = mPref.readBoolean(key2, false);
//						if (allowed) {
//							mPref.write(key, false);
//							((RadioButton) v0).setChecked(false);  //locks in false
//							((RadioButton) v0).toggle();  //does not untoggle
//							Log.e("tml attempt uncheck");
//						} else {
//							mPref.write(key, true);
//							mPref.write(key2, false);
//							((RadioButton) v0).setChecked(true);  //does not work
//							((RadioButton) v0).toggle();  //does not retoggle
//							Log.e("tml attempt normal");
//						}
//						Log.i("suv state:" + address + ":" + Boolean.toString(mPref.readBoolean(key, false))
//								+ "|" + Boolean.toString(mPref.readBoolean(key2, false)));
					}
				});
				((RadioButton) v.findViewById(R.id.autoanswer2)).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v0) {
//						Object vObj1 = v0.getTag();
//						String address = (String) vObj1;
//						String key = "autoAnswer:" + address;
//						boolean allowed = mPref.readBoolean(key, false);
//						String key2 = "autoAnswer2:" + address;
//						boolean allowed2 = mPref.readBoolean(key2, false);
//						if (allowed2) {
//							mPref.write(key2, false);
//							((RadioButton) v0).setChecked(false);  //locks in false
//							((RadioButton) v0).toggle();  //does not untoggle
//							Log.e("tml attempt uncheck");
//						} else {
//							mPref.write(key, false);
//							mPref.write(key2, true);
//							((RadioButton) v0).setChecked(true);  //does not work
//							((RadioButton) v0).toggle();  //does not retoggle
//							Log.e("tml attempt normal");
//						}
//						Log.i("suv state:" + address + ":" + Boolean.toString(mPref.readBoolean(key, false))
//								+ "|" + Boolean.toString(mPref.readBoolean(key2, false)));
					}
				});
				Log.i("suv state:" + address + ":" + Boolean.toString(mPref.readBoolean("autoAnswer:" + global))
						+ "|" + Boolean.toString(mPref.readBoolean("autoAnswer2:" + global)));
				//**tml
			}
		}
	};
	
	public void refreshHistory()
	{
		mHandler.postDelayed(arrangeHistory, 500);
	}
	
	Runnable arrangeHistory=new Runnable()
	{
		@Override
		public void run() {
			items=mPref.readMapArray("recordHistory");
			if (mAdapter==null)
				mAdapter = new SecurityHistoryAdapter(SecurityNewActivity.this);
			
			if (mAdapter!=null)
			{
				mAdapter.setItemList(items);
				mList.setAdapter(mAdapter);
				mAdapter.notifyDataSetChanged();
			}
		}
	};
	//tml*** upload suv
	public void notifyHistory (int info) {
		if (mAdapter!=null) {
			mHandler.post(new Runnable () {
				@Override
				public void run() {
					mAdapter.notifyDataSetChanged();
				}
			});
		}
	}
	
	public void onDeleteFile(String fn)
	{
		for (Map<String, String> map : items)
		{
			String filePath=map.get("0");
			if (filePath.equals(fn))
			{
				items.remove(map);
				
				String thumbnailPath=Global.SdcardPath_record+"thumb_"+filePath.substring(filePath.lastIndexOf("/")+1)+".jpg";
				//tml*** suv save dest
				if (mPref.readBoolean("suvsavedest", false)) {
					//TODO
					String suvsavedest_ext = mPref.read("suvsavedest_ext", getResources().getString(R.string.recording_external));
					File extpath = new File(suvsavedest_ext);
					String extpathName = extpath.getPath();
				} else {
					//TODO
				}
				new File(thumbnailPath).delete();
				break;
			}
		}
        
        mPref.writeMap("recordHistory", items);
        
        mAdapter.notifyDataSetChanged();
	}
	//tml*** suv call limit
	Runnable alertCallLimit = new Runnable() {
    	public void run() {
    		String myNick = mPref.read("myNickname", "unknown");
    		String text = "(" + myNick + ") " + getString(R.string.alertCallLimit);
    		Intent it = new Intent(SecurityNewActivity.this, Tooltip.class);
            it.putExtra("Content", text);
            startActivity(it);
    	}
    };
	Runnable showAutoAnswerTooltip=new Runnable(){
    	public void run()
    	{
    		String text = getString(R.string.auto_answer_tooltip);  //tml*** auto2
    		Intent it=new Intent(SecurityNewActivity.this,Tooltip.class);
            it.putExtra("Content", text);
            startActivity(it);
    	}
    };
    //tml*** auto2
	Runnable showAutoAnswerTooltip2 = new Runnable() {
    	public void run() {
    		String text = getString(R.string.auto_answer_tooltip2);
    		Intent it = new Intent(SecurityNewActivity.this, Tooltip.class);
            it.putExtra("Content", text);
            startActivity(it);
    	}
    };
    
	Runnable showAutoAnswerTooltip0 = new Runnable() {
    	public void run() {
    		String text = getString(R.string.auto_answer_tooltip0);
    		Intent it = new Intent(SecurityNewActivity.this, Tooltip.class);
            it.putExtra("Content", text);
            startActivity(it);
    	}
    };
    //***tml
    //tml*** suv save dest
	Runnable showSuvSaveTooltip = new Runnable() {
    	public void run()
    	{
    		Intent it=new Intent(SecurityNewActivity.this, Tooltip.class);
            it.putExtra("Content", getString(R.string.suvsave_tooltip));
            startActivity(it);
    	}
    };
    //***tml
    //tml*** secmax
	Runnable instantMaxTooltip = new Runnable(){
    	public void run()
    	{
    		Intent it=new Intent(SecurityNewActivity.this, Tooltip.class);
            it.putExtra("Content", getString(R.string.sec_max_tooltip));
            startActivity(it);
    	}
    };
    //***tml
	
	public FrameLayout getNotificationLayout()
	{
		return (FrameLayout)findViewById(R.id.notification);
	}
	
	String sendeeAddress="";
	Runnable authorizeByTCPMessage=new Runnable(){
		public void run()
		{
			if (AireJupiter.getInstance()!=null && AireJupiter.getInstance().isLogged())
			{
				AireJupiter.getInstance().tcpSocket().send(sendeeAddress,"I am your GUARD", 0, null, null, 0, null);
			}
		}
	};
	
	Runnable deauthorizeByTCPMessage=new Runnable(){
		public void run()
		{
			if (AireJupiter.getInstance()!=null && AireJupiter.getInstance().isLogged())
			{
				AireJupiter.getInstance().tcpSocket().send(sendeeAddress,"I am NOT your GUARD", 0, null, null, 0, null);
			}
		}
	};
	@Override
	protected void onPause() {
		stopCamera();  //tml*** suv mask
		scanBlueDevice(false);  //tml|li*** blue io
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		//tml|li*** blue io
		mHandler.removeCallbacks(scanCountdown);
		scanBlueDevice(false);
		unbindService(conn);  //li*** Home safeguard
		stopCamera();  //tml*** suv mask
		try {  //tml*** unregistered rcvr destroy
			unregisterReceiver(SecurityController);  //tml*** suv save dest/
		} catch (IllegalArgumentException e) {
		}
		basepath = null;
		if (mADB != null && mADB.isOpen())
			mADB.close();
		_this=null;
		System.gc();
		System.gc();
		super.onDestroy();

	}
	
	@Override
    protected void onResume() {
    	super.onResume();
    	DialerFrame.checkEmbeddedDialer(findViewById(android.R.id.content));
    	//tml*** suv save dest
		IntentFilter ifilter = new IntentFilter();
		ifilter.addAction(Global.Action_SecurityActivity);
		ifilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		ifilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		this.registerReceiver(SecurityController, ifilter);
		//***tml

		deviceAdapter.notifyDataSetChanged();  //li*** Home safeguard
	}

    @Override
    protected void onNewIntent(Intent intent) {
    	//required to update NEW intents
        super.onNewIntent(intent);
        setIntent(intent);
    }
    
	@Override  
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode==1000)
		{
			if (resultCode==RESULT_OK && data!=null)
			{
				int type=data.getIntExtra("type", -1);
				if (type==1)
				{
					String number = data.getStringExtra("result");
					
					instants = mPref.readArray("instants");
//					String global = MyTelephony.smartAddingPrefix(SecurityNewActivity.this, number, null);
					//tml*** notsmart query
					if (number.startsWith("011")) number = number.substring(3);
					else if (number.startsWith("00")) number = number.substring(2);
					else if (number.startsWith("0")) number = number.substring(1);
					String global = "+" + number;
					//***tml
    				Log.d("SelPh# insertSUV Call2 " + global);
					instants.add(global);
					mPref.writeArray("instants", instants);
					mHandler.post(arrange);
				}else if (type==0)
				{
					ArrayList<String> list=data.getExtras().getStringArrayList("addressList");
					if (list!=null && list.size()>0)
					{
						instants=mPref.readArray("instants");
						String address=list.get(0);
						instants.add(address);
						mPref.writeArray("instants", instants);
						mHandler.post(arrange);
						
						sendeeAddress=address;
//						mHandler.postDelayed(authorizeByTCPMessage, 1500);
						new Thread(authorizeByTCPMessage).start();
					}
				}
			}
		}
		else if (requestCode == 20)
		{
			if (resultCode==RESULT_OK)
			{
				try{
					String path = data.getStringExtra("filePath");
					String srcFilePath = data.getStringExtra("srcFilePath");
					String dstPath=path+srcFilePath.substring(srcFilePath.lastIndexOf("/"));
					MyUtil.copyFile(new File(srcFilePath), new File(dstPath), true, getApplicationContext(), true);
				}catch(Exception e){}
			}
		}
		//tml*** suv save dest
		else if (requestCode == 30) {
			if (resultCode == RESULT_OK) {
				try {
					String path = data.getStringExtra("filePath");
					mPref.write("suvsavedest_ext", path);
					String suvsavedest_ext = mPref.read("suvsavedest_ext", getResources().getString(R.string.recording_external));
					((TextView) findViewById(R.id.show_suvsaveext)).setText(suvsavedest_ext);
					basepath = null;
				} catch (Exception e) {}
			}
		}
		//***tml
	}
	
	//tml*** suv save dest
	public boolean checkSaveDirectory (String path) {
//		File folder = new File(Environment.getExternalStorageDirectory() + path);
		File folder = new File(path);
		if (path.equals(getResources().getString(R.string.recording_external))) {
			if (((RadioButton) findViewById(R.id.suvsave_ext)).isChecked()) {
				((RadioButton) findViewById(R.id.suvsave_default)).setChecked(true);
				((RadioButton) findViewById(R.id.suvsave_ext)).setChecked(false);
			}
			return false;
		} else if (!folder.exists()) {
			mHandler.post(showSuvSaveTooltip);
			mPref.write("suvsavedest_ext", getResources().getString(R.string.recording_external));
			if (((RadioButton) findViewById(R.id.suvsave_ext)).isChecked()) {
				((RadioButton) findViewById(R.id.suvsave_default)).setChecked(true);
				((RadioButton) findViewById(R.id.suvsave_ext)).setChecked(false);
			}
			return false;
		} else {
			Log.d("CHECK SUV SAVE DIR>> " + folder.getPath());
			return true;
		}
	}
	
	//will confuse device and acces, ac100 also has hidden 4 usb detected
	BroadcastReceiver SecurityController = new BroadcastReceiver() {
		@Override
	    public void onReceive(Context context, Intent intent) {
			String iAction = intent.getAction();
			if (iAction.equals(Global.Action_SecurityActivity)) {
				int command = intent.getIntExtra("Command", 0);
				if (command == 33) {  //tml*** multi suvei
					Log.e("SecurityController arrange");
					instants = mPref.readArray("instants");
					mHandler.post(arrange);
				}
			} else if (iAction.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
		        Log.d("HI USB");
			} else if (iAction.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
		        Log.d("BYE USB");
		        if (((RadioButton) findViewById(R.id.suvsave_ext)).isChecked()) {
			        checkSaveDirectory(mPref.read("suvsavedest_ext"));
		        }
			}
		}
	};
	//***tml
	
	public void close()
	{
		finish();
	}

	//tml|bj*** neverdie
	public void neverSayNeverDie(Context context) {
		if (AireJupiter.getInstance()==null) {
			Log.e("AireJupiter.getInstance() is null, RESETTING");
			Intent vip0 = new Intent(context, BeeHiveService.class);
			context.stopService(vip0);
			Intent vip1 = new Intent(context, AireVenus.class);
			context.stopService(vip1);
			Intent vip2 = new Intent(context, AireJupiter.class);
			context.stopService(vip2);
			
			Intent vip00 = new Intent(context, AireJupiter.class);
			context.startService(vip00);
		}
	}

	
	//tml*** suv mask
	private Camera cam = null;
	private StartCameraTask mStartCameraTask;
	
	
	private void startCamera() {
		((LinearLayout) findViewById(R.id.previewMasks)).setVisibility(View.VISIBLE);
		((SurfaceView) findViewById(R.id.preview)).setVisibility(View.VISIBLE);
		if (mPref.readBoolean("suvTLeft", true))
			((LinearLayout) findViewById(R.id.previewTLeft)).setBackgroundColor(0x9900FF00);
		if (mPref.readBoolean("suvTRight", true))
			((LinearLayout) findViewById(R.id.previewTRight)).setBackgroundColor(0x7700FF00);
		if (mPref.readBoolean("suvBLeft", true))
			((LinearLayout) findViewById(R.id.previewBLeft)).setBackgroundColor(0x5500FF00);
		if (mPref.readBoolean("suvBRight", true))
			((LinearLayout) findViewById(R.id.previewBRight)).setBackgroundColor(0x3300FF00);
		mStartCameraTask = new StartCameraTask();
		mStartCameraTask.execute();
	}

	private void stopCamera() {
		((ToggleButton) findViewById(R.id.showpreview)).setChecked(false);
		((LinearLayout) findViewById(R.id.previewMasks)).setVisibility(View.GONE);
		if (cam != null) {
			try {
				AndroidVideoApi9JniWrapper.stopRecording(cam);
				cam = null;
			} catch (Exception e) {
				Log.e("Preview cam stop !@#$ " + e.getMessage());
			}
		}
		((SurfaceView) findViewById(R.id.preview)).setVisibility(View.GONE);
	}

	private class StartCameraTask extends AsyncTask<Void, Void, Void> {

		StartCameraTask () {}
		
		@Override
		public void onPreExecute() {}
		
		@Override
		protected Void doInBackground(Void... params) {
			if (cam == null) {
				int rot = 0;
				String Brand = Build.BRAND.toLowerCase();
				String Model = Build.MODEL.toLowerCase();
				
				int cameraId = 1;
				if (AndroidCameraConfiguration.retrieveCameras().length > 0) {
					cameraId = cameraId % AndroidCameraConfiguration.retrieveCameras().length;
				} else {
					cameraId = 0;
				}
				if (Brand.contains("amazon")) cameraId = 0;
				
				if (Model.contains("k200") || Model.contains("s82")) {
					rot = 0;
				} else if (Model.contains("a9")) {
					rot = 90;
				} else if (Brand.contains("amazon")) {
					rot = 90;
				}
				
				Log.d("Preview cam start " + Brand + " " + Model + " " + cameraId + " " + rot);
				
				cam = (Camera) AndroidVideoApi9JniWrapper.startRecording(cameraId, 640, 480, 5, rot, 0);

				try {
					cam.setPreviewDisplay(((SurfaceView) findViewById(R.id.preview)).getHolder());
				} catch (Exception e) {
					Log.e("Preview cam load !@#$ " + e.getMessage());
				}
			} else {
				Log.e("Preview cam NULL");
			}
			return null;
		}
		
		@Override
		public void onPostExecute(Void result) {}
	}
	
	private boolean minimumSUVArea(boolean turnoff) {
		if (turnoff) {
			int detectQds = 0;
			if (mPref.readBoolean("suvTLeft", true)) detectQds++;
			if (mPref.readBoolean("suvTRight", true)) detectQds++;
			if (mPref.readBoolean("suvBLeft", true)) detectQds++;
			if (mPref.readBoolean("suvBRight", true)) detectQds++;
			if (detectQds <= 1) {
				Toast.makeText(SecurityNewActivity.this, getString(R.string.suv_area_warn), Toast.LENGTH_SHORT).show();
				return true;
			}
		}
		return false;
	}
	//***tml
	//tml*** iot control
	private void startSendingMsg(final String deviceName, final String deviceCat, final String heart)
	{	
		new Thread(new Runnable() {
			public void run() {
				triggerAireMsg(deviceName, deviceCat, heart);
			}
		}).start();

		new Thread(new Runnable() {
			public void run() {
				triggerInstantCall();
			}
		}).start();
		
	}
	
	private void triggerAireMsg(String deviceName, String deviceCat, String heart)
	{
		AmpUserDB mADB = new AmpUserDB(SecurityNewActivity.this);  //tml*** suv send more
		mADB.open();
		
		int myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
		//tml*** suv send more
		List<String> instants = mPref.readArray("instants");
		if (instants != null) {
			for (String address : instants) {
				int suvIdx = mADB.getIdxByAddress(address);
				Log.e("suv-home hello=" + address + " " + suvIdx);
				if (suvIdx != -1) {
					SendAgent agent = new SendAgent(SecurityNewActivity.this, myIdx, suvIdx, true);
					agent.onSend(address, heart
							+ "  " + deviceName + " (" + deviceCat + ")"
							, 0, null, null, true);
				}
				MyUtil.Sleep(100);
			}
		}
		
		mADB.close();
	}
	
	private void triggerInstantCall()
	{
		boolean sendSMS = mPref.readBoolean("sendSMS", true);
		boolean sendCalls = mPref.readBoolean("sendCalls", true);
		if (!sendSMS && !sendCalls) return;
		//tml*** suv send more
		long last = mPref.readLong("last_emergency_call", 0);
		long now = new Date().getTime();
		if (now - last < 15000) {  //15 sec
			Log.e("suv-home NO NOTIFY <10s (" + (now - last) + "s)");
			return;
		}
		mPref.writeLong("last_emergency_call", now);
		//***tml
		List<String> instants = mPref.readArray("instants");
		if (instants != null) {
			for (String number: instants) {
				Log.e("suv-home number=" + number);
				if (number != null && number.length() >= 7 && number.startsWith("+")) {
					if (number.startsWith("+")) number = number.substring(1);
					//tml*** suv send more
					onNewNotify(number);
					MyUtil.Sleep(100);
				}
			}
		}
	}
	//tml*** suv send more
	public void onNewNotify(String number) {
		TriggerNotifyRun notifyRunnable = new TriggerNotifyRun();
		if (notifyRunnable.setData(number))
			new Thread(notifyRunnable).start();
	}
	
	public class TriggerNotifyRun implements Runnable {
		private String _number;
		private String outNumber;
		private boolean sendSMS, sendCalls;
		private String myPhoneNumber, myPasswd, myNick, myIdx, myLang;
		
		public boolean setData(String number)
		{
		    this._number = number;
		    sendSMS = mPref.readBoolean("sendSMS", true);
			sendCalls = mPref.readBoolean("sendCalls", true);
			MCrypt mc = new MCrypt();

			myPhoneNumber = mPref.read("myPhoneNumber", "++++");
			myPasswd = mPref.read("password", "1111");
			myNick = mPref.read("myNickname", "unknown");
			myIdx = mPref.read("myID", "0");
			myLang = mPref.read("ssendLang", "us_english");

			try {
				Log.i("suv-home " + myPhoneNumber + " " + myIdx + " " + myNick + " " + myPasswd + " " + _number + " " + myLang);
				myPhoneNumber = MCrypt.bytesToHex(mc.encrypt(myPhoneNumber)).toLowerCase();
				myIdx = MCrypt.bytesToHex(mc.encrypt(myIdx)).toLowerCase();
				myPasswd = MCrypt.bytesToHex(mc.encrypt(myPasswd)).toLowerCase();
				myNick = MCrypt.bytesToHex(mc.encrypt(myNick)).toLowerCase();
				outNumber = MCrypt.bytesToHex(mc.encrypt(_number)).toLowerCase();
				myLang = MCrypt.bytesToHex(mc.encrypt(myLang)).toLowerCase();
			} catch (Exception e) {
				Log.e("suv.notify encrypt (" + _number + ") !@#$ " + e.getMessage());
				return false;
			}
			return true;
		}
		
		@Override
		public void run()
		{
			try {
				MyNet net = new MyNet(getApplicationContext());
				//tml*** china ip
				String domain = AireJupiter.myAcDomain_default;
				if (AireJupiter.getInstance() != null) {
					domain = AireJupiter.getInstance().getIsoDomain();
				}
				
				if (sendSMS) {
					Log.e("suv-home EmergencySMS=" + _number);
					String Return = net.doAnyPost("http://" + domain + "/t/xmlrpc/customer/smsonly.php", 
							"username=" + myPhoneNumber
    						+ "&idx=" + myIdx
    						+ "&password=" + myPasswd
    						+ "&nickname=" + myNick
    						+ "&callee=" + outNumber
    						+ "&language=" + myLang);
					Log.i("suv-home ReturnSMS=" + Return);
				}
				
				if (sendCalls) {
					Log.e("suv EmergencyCALL=" + _number);
					String Return = net.doAnyPost("http://" + domain + "/t/xmlrpc/customer/callonly.php", 
							"username=" + myPhoneNumber
    						+ "&idx=" + myIdx
    						+ "&password=" + myPasswd
    						+ "&nickname=" + myNick
    						+ "&callee=" + outNumber
    						+ "&language=" + myLang);
					Log.i("suv-home ReturnCALL=" + Return);
					//tml*** suv call limit
					if (Return.equals("success")) {
						mPref.write("alertCallLimit", true);
						
						long now = new Date().getTime();
						long last = mPref.readLong("last_suv_maxalert_warn", 0);
						long elapsed = now - last;
						if (elapsed < 300000) {  //5m
							return;
						}
						mPref.writeLong("last_suv_maxalert_warn", now);
						
						Intent intent = new Intent(Global.Action_InternalCMD);
						intent.putExtra("Command", Global.CMD_SUV_CALLLIMIT);
						getApplicationContext().sendBroadcast(intent);
					} else if (Return.equals("successok")) {  //limit of calls per month reached
						mPref.write("alertCallLimit", false);
					}
				}
			} catch (Exception e) {
				Log.e("suv.notify php (" + _number + ") !@#$ " + e.getMessage());
			}
		}
	}
	//tml*** beta ui2
	private OnGenericMotionListener sideBarMotionListener = new OnGenericMotionListener() {
		@Override
		public boolean onGenericMotion(View v, MotionEvent event) {
			int action = event.getAction();
			int source = event.getSource();
			
			if (source == InputDevice.SOURCE_MOUSE) {
				switch (action) {
					case MotionEvent.ACTION_HOVER_ENTER:
						if (v.getId() == R.id.sidebar_ghost) {
							Log.d("aloha mouse onto sidebar zone");
							if (!((DrawerLayout) findViewById(R.id.main_content)).isDrawerOpen((RelativeLayout) findViewById(R.id.sidebar_frame_drawer))) {
								((DrawerLayout) findViewById(R.id.main_content)).openDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));
							}
						}
						break;
					case MotionEvent.ACTION_HOVER_EXIT:
						if (v.getId() == R.id.sidebar_ghost) {
							Log.d("aloha mouse left sidebar zone");
							if (((DrawerLayout) findViewById(R.id.main_content)).isDrawerOpen((RelativeLayout) findViewById(R.id.sidebar_frame_drawer))) {
								mHandler.postDelayed(new Runnable() {
									@Override
									public void run() {
										((DrawerLayout) findViewById(R.id.main_content)).closeDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));
									}
								}, 500);
							}
						}
						break;
				}
			}
			return false;
		}
	};
}