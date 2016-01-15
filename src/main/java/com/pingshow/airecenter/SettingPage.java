package com.pingshow.airecenter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.zxing.WriterException;
import com.pingshow.airecenter.cons.Key;
import com.pingshow.network.MyNet;
import com.pingshow.network.MySocket;
import com.pingshow.qrcode.EncodingHandler;
import com.pingshow.util.HdmiUtil;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.LedSpeakerUtil;
import com.pingshow.util.MCrypt;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.util.ResizeImage;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;

public class SettingPage extends Page implements OnClickListener {
	public static final String vlib = "beta38r8";
	private final String versnotes = " TEST! " + vlib + ", ui, bcast";
	public static boolean TEST = false;
	public static int TESTcount = 0;
	public static int TESTpos = 0;
	public static boolean TESTvalueB = false;
	public static String TESTdataS = "";
	public static int TESTdataI = 0;
	public static boolean TESTdataB = false;
	private SpeechRecognizer mSpeechRecognizer;

	private MyPreference mPref;
	private String moodContent = null;
	private String photoPath = null;
	private int myIdx;
	private boolean photoChanged;
	private boolean moodChanged;
	private boolean emailChanged;
	private boolean nicknameChanged;
	private EditText emailView;
	private EditText nicknameView;
	private EditText moodView;
	private ImageView photoView;
	//tml*** passwords
	private Button retrievePW;
	private int retpwcount = 0;

	private final Handler mHandler = new Handler();
	private TextView hint;
	protected int sortMethod;
	private View layout;
	
	private boolean support1 = false;
	private boolean support2 = false;
	private boolean support3 = false;
	
	private static final int PHOTO_SIZE = 720;
	
	private int voloption;
	private int broption;
	
	public SettingPage(View currentView) 
	{
		Log.e("*** !!! SETTINGPAGE *** START START !!! ***");
		MainActivity._this.overridePendingTransition(R.anim.freeze, R.anim.freeze);
		
		layout=currentView;
		mPref = new MyPreference(MainActivity._this);
		hint = (TextView) currentView.findViewById(R.id.my_photo_hint);
		TextView et = (TextView) currentView.findViewById(R.id.phone_number);
		String phone = mPref.read("myPhoneNumber");
		if (MyTelephony.isPhoneNumber(phone)) {
			int len = phone.length();
			String A = phone.substring(0, len / 3);
			String C = phone.substring(len / 3 * 2 + 1);
			String B = "";
			int r = len / 3 * 2 + 1 - len / 3;
			for (int i = 0; i < r; i++)
				B += "●";
			et.setText(A + B + C);
		} else
			et.setText(phone);
		
		//tml*** beta ui
		TextView showVersion = (TextView) layout.findViewById(R.id.show_version);
		if (Log.enDEBUG) {
			showVersionNotes(false);
		} else {
			showVersionNotes(true);
		}
		showVersion.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (MyUtil.CheckServiceExists(MainActivity._this, "com.pingshow.airecenter.AireJupiter")) {
					if (AireJupiter.getInstance()!=null) {
						AireJupiter.getInstance().forceCheckUpdate();
						AireJupiter.checkShow = true;
					}
				}
			}
		});
		//***tml

		nicknameView = (EditText) currentView.findViewById(R.id.nickname);
		nicknameView.setText(mPref.read("myNickname", ""));
		nicknameView.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				nicknameChanged = true;
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
		});
		
		nicknameView.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus)
					checkNickname();
			}
		});
		
		try{
			myIdx = Integer.valueOf(mPref.read("myID", "0"), 16);
		}catch(Exception e){}
		
		moodContent = mPref.read("moodcontent", "");
		moodView = (EditText) currentView.findViewById(R.id.my_mood);
		
		if (moodContent.length() > 0) {
			moodView.setText(moodContent);
		}
		moodView.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				moodChanged = true;
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//tml*** secret test
				String secret = s.toString();
				if ((secret.toLowerCase().equals("debug(80aire"))
						|| secret.toLowerCase().equals("log(80aire")
						|| secret.toLowerCase().equals("support1")
						|| secret.toLowerCase().equals("support2")
						|| secret.toLowerCase().equals("support3")
						|| (secret.toLowerCase().equals("test") && Log.enDEBUG)) {
					mHandler.post(showSecret);
				}
				//***tml
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
		});
		((EditText) currentView.findViewById(R.id.my_mood)).requestFocus();  //tml*** prefocus

		emailView = (EditText) currentView.findViewById(R.id.email);
		emailView.setText(mPref.read("email", ""));
		emailView.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				emailChanged = true;
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
		});

		emailView.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus)
					checkEmail();
			}
		});

		photoView = (ImageView) currentView.findViewById(R.id.photo);
		photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		String path = mPref.read("myPhotoPath", null);

		if (path == null) {
			path = Global.SdcardPath_sent + "myself_photo_" + myIdx + ".jpg";
			if (MyUtil.checkSDCard(MainActivity._this) && (new File(path).exists()))
				mPref.write("myPhotoPath", path);
			else
				path = null;
		}

		if (path == null || !MyUtil.checkSDCard(MainActivity._this) || !(new File(path).exists()))
			photoView.setImageResource(R.drawable.empty);
		else {
			try {
				Drawable photo = ImageUtil.loadBitmapSafe(path, 1);
				photoView.setImageDrawable(photo);
				hint.setVisibility(View.GONE);
			} catch (Exception e) {
				System.gc();
				System.gc();
				photoView.setImageResource(R.drawable.empty);
			}
		}

		photoView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onPickPictureOption();
			}
		});

		photoPath = mPref.read("myPhotoPath", null);
		if (photoPath == null) {
			int c = mPref.readInt("needPhotoTip", 0);
			if ((c % 4) == 0)
				mHandler.postDelayed(showTooltip, 1000);
			mPref.write("needPhotoTip", ++c);
		}

		//tml|li*** qr addf
		((ImageButton) currentView.findViewById(R.id.my_qr)).setOnClickListener(new OnClickListener() {
			@Override
    		public void onClick(View v) {
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
					try {
						String address = mPref.read("myPhoneNumber", "----");
						String nickname = mPref.read("myNickname", "");
						int idx = Integer.parseInt(mPref.read("myID", "0"), 16);
						String qrContent = "AddAireFriend," + address + "," + idx + "," + nickname;
						//li*** encrypt
						byte[] bytes = MCrypt.encrypt(qrContent.getBytes());
						qrContent = Base64.encodeToString(bytes, Base64.DEFAULT);
						
						Bitmap qrCodeBitmap = EncodingHandler.createQRCode(qrContent, 300);
						Dialog qrCode = new Dialog(MainActivity._this);
						View view = View.inflate(MainActivity._this.getApplicationContext(), R.layout.user_qrcode, null);
						ImageView iv = (ImageView) view.findViewById(R.id.iv_qrcode);
						iv.setImageBitmap(qrCodeBitmap);
						
						qrCode.setTitle(nickname);
						qrCode.setCanceledOnTouchOutside(true);
						qrCode.setContentView(view);
						
						qrCode.show();
					} catch (WriterException e) {
						Log.e("build qr !@#$ " + e.getMessage());
					}
				} else {
					Toast.makeText(MainActivity._this, MainActivity._this.getString(R.string.old_sdk), Toast.LENGTH_SHORT).show();
				}
    		}
		});
		
		//tml*** passwords
		retrievePW = (Button) currentView.findViewById(R.id.retrievepw);
		retrievePW.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				long timenow, timepast, timelast;
				retpwcount = mPref.readInt("retpwcount", 0);
				timelast = mPref.readLong("retpwspam", 0);
				timenow = System.currentTimeMillis();
				timepast = timenow - timelast;
				if (timepast > 300000) {
					retpwcount = 0;
					mPref.write("retpwcount", retpwcount);
				}
				if (retpwcount < 3) {
					retpwcount++;
					mPref.write("retpwcount", retpwcount);
					timelast = System.currentTimeMillis();
					mPref.writeLong("retpwspam", timelast);
					mHandler.post(showRetrievePWTooltip);
					try {
						MyNet myNet = new MyNet (MainActivity._this);
						String myidx = mPref.read("myPhoneNumber", "0");
						Log.i("tml pwid " + myidx);
						myidx = URLEncoder.encode(myidx,"UTF-8");
						String Return = "";
						Return = myNet.doPostHttps("sendpwdemail_aire_www.php",
								"id=" + myidx, null);
						Log.i("tml RETRIEVEPW DONE> " + Return);
					} catch (Exception e) {
						Log.e("pw ohuh " + e.getMessage());
					}
				} else {
					mHandler.post(showRetPWSpamTooltip);
				}
			}
		});
		//***tml
		
		((CheckedTextView) layout.findViewById(R.id.blockstranger)).setChecked(mPref.readBoolean("BlockStrangers", false));
		((CheckedTextView) layout.findViewById(R.id.blockstranger)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked=((CheckedTextView)v).isChecked();
				((CheckedTextView)v).setChecked(!checked);
				mPref.write("BlockStrangers", ((CheckedTextView)v).isChecked());  //alec*** no save sett/
			}
		});
		
		((CheckedTextView) layout.findViewById(R.id.notification_sound)).setChecked(mPref.readBoolean("notification_sound", true));
		((CheckedTextView) layout.findViewById(R.id.notification_sound)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked=((CheckedTextView)v).isChecked();
				((CheckedTextView)v).setChecked(!checked);
				mPref.write("notification_sound", ((CheckedTextView)v).isChecked());  //alec*** no save sett/
			}
		});
		
		//tml*** normal ring

		voloption = mPref.readInt("incRingVolume", 2);
		String s_voloption = "";
	    if (voloption == 0) {
	    	s_voloption = MainActivity._this.getResources().getString(R.string.mute);
	    } else if (voloption == 1) {
	    	s_voloption = MainActivity._this.getResources().getString(R.string.bitrateoptionL);
	    } else if (voloption == 2) {
	    	s_voloption = MainActivity._this.getResources().getString(R.string.bitrateoptionM);
	    } else if (voloption == 3) {
	    	s_voloption = MainActivity._this.getResources().getString(R.string.bitrateoptionH);
	    }
	    ((Button) layout.findViewById(R.id.ringvolume)).setText(s_voloption);

	    ((Button) layout.findViewById(R.id.ringvolume)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity._this);
				CharSequence[] options = {MainActivity._this.getResources().getString(R.string.mute),
						MainActivity._this.getResources().getString(R.string.bitrateoptionL),
						MainActivity._this.getResources().getString(R.string.bitrateoptionM),
						MainActivity._this.getResources().getString(R.string.bitrateoptionH)};
				int index = mPref.readInt("incRingVolume", 2);
				
				builder.setTitle(MainActivity._this.getResources().getString(R.string.bitrateoption));
				builder.setSingleChoiceItems(options, index, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int index) {
						switch (index) {
							case 0:
								mPref.write("incRingVolume", index);
								((Button) layout.findViewById(R.id.ringvolume))
										.setText(MainActivity._this.getResources().getString(R.string.mute));
								break;
							case 1:
								mPref.write("incRingVolume", index);
								((Button) layout.findViewById(R.id.ringvolume))
										.setText(MainActivity._this.getResources().getString(R.string.bitrateoptionL));
								break;
							case 2:
								mPref.write("incRingVolume", index);
								((Button) layout.findViewById(R.id.ringvolume))
										.setText(MainActivity._this.getResources().getString(R.string.bitrateoptionM));
								break;
							case 3:
								mPref.write("incRingVolume", index);
								((Button) layout.findViewById(R.id.ringvolume))
										.setText(MainActivity._this.getResources().getString(R.string.bitrateoptionH));
								break;
						}
						Log.i("tml incRingVolume=" + mPref.readInt("incRingVolume", 2));
						dialog.dismiss();
					}
				});
				
				builder.create().show();
			}
	    });
		
		((CheckedTextView) layout.findViewById(R.id.normal_ring)).setChecked(mPref.readBoolean("normal_ring", true));
		((CheckedTextView) layout.findViewById(R.id.normal_ring)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked = ((CheckedTextView) v).isChecked();
				((CheckedTextView) v).setChecked(!checked);
				mPref.write("normal_ring", ((CheckedTextView) v).isChecked());  //alec|tml*** no save sett/
			}
		});
		//***tml
		
		//tml*** voice control
//		mPref.write("voice_control", false);  //temp disable
//		((CheckedTextView) layout.findViewById(R.id.voice_control)).setVisibility(View.GONE);  //temp disable
//		((ImageView) layout.findViewById(R.id.help_voice)).setVisibility(View.GONE);  //temp disable

		if (MyUtil.isISO_China(MainActivity._this, mPref, "us")) {
			((CheckedTextView) layout.findViewById(R.id.voice_control)).setVisibility(View.GONE);
			((ImageView) layout.findViewById(R.id.help_voice)).setVisibility(View.GONE);
		}
		((CheckedTextView) layout.findViewById(R.id.voice_control)).setChecked(mPref.readBoolean("voice_control", false));
		((CheckedTextView) layout.findViewById(R.id.voice_control)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked = ((CheckedTextView) v).isChecked();
				((CheckedTextView) v).setChecked(!checked);
				mPref.write("voice_control", ((CheckedTextView) v).isChecked());
				if (!((CheckedTextView) layout.findViewById(R.id.normal_ring)).isChecked()
						&& ((CheckedTextView) v).isChecked()) {
					((CheckedTextView) layout.findViewById(R.id.normal_ring)).setChecked(true);
					mPref.write("normal_ring", true);
				}
			}
		});
		
		((ImageView) layout.findViewById(R.id.help_voice)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mHandler.post(vanswerTooltip);
			}
		});
		//***tml
		//tml*** cec
		((ImageView) layout.findViewById(R.id.help_HDMI)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mHandler.post(hdmianswerTooltip);
			}
		});

        mHandler.postDelayed(new Runnable () {
			@Override
			public void run() {
				boolean input = mPref.readBoolean("HDMIctrl_input", true);
				boolean tvon = mPref.readBoolean("HDMIctrl_tv", true);
				if ((input || tvon) && AireJupiter.getInstance() != null) {
					AireJupiter.hdmiCmdExecSetCEC();
				}
			}
        }, 2000);
        
        boolean cauto = mPref.readBoolean("HDMIctrl_auto", true);
		boolean input = mPref.readBoolean("HDMIctrl_input", true);
		boolean tvon = mPref.readBoolean("HDMIctrl_tv", true);
		((CheckedTextView) layout.findViewById(R.id.HDMI_auto)).setChecked(cauto);
		((CheckedTextView) layout.findViewById(R.id.HDMI_auto)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked = ((CheckedTextView) v).isChecked();
				((CheckedTextView) v).setChecked(!checked);
				mPref.write("HDMIctrl_auto", ((CheckedTextView) v).isChecked());
				
				boolean input = ((CheckedTextView) layout.findViewById(R.id.HDMI_input)).isChecked();
				boolean tvon = ((CheckedTextView) layout.findViewById(R.id.HDMI_tv)).isChecked();
				if (!input && !tvon) {
					((CheckedTextView) layout.findViewById(R.id.HDMI_input)).setChecked(true);
					((CheckedTextView) layout.findViewById(R.id.HDMI_tv)).setChecked(true);
					if (AireJupiter.getInstance() != null) {
						AireJupiter.hdmiCmdExecSetCEC();
					}
					mPref.write("HDMIctrl_input", true);
					mPref.write("HDMIctrl_tv", true);
				}
				
				Log.i("tml CEC " + mPref.readBoolean("HDMIctrl_auto", true)
						+ mPref.readBoolean("HDMIctrl_input", true)
						+ mPref.readBoolean("HDMIctrl_tv", true));
			}
		});
		
		((CheckedTextView) layout.findViewById(R.id.HDMI_input)).setChecked(input);
		((CheckedTextView) layout.findViewById(R.id.HDMI_input)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked = ((CheckedTextView) v).isChecked();
				((CheckedTextView) v).setChecked(!checked);
				if (!checked) {
					if (AireJupiter.getInstance() != null) {
						AireJupiter.hdmiCmdExecSetCEC();
					}
				} else {
					boolean cauto = ((CheckedTextView) layout.findViewById(R.id.HDMI_auto)).isChecked();
					boolean tvon = ((CheckedTextView) layout.findViewById(R.id.HDMI_tv)).isChecked();
					if (cauto && !tvon) {
						((CheckedTextView) layout.findViewById(R.id.HDMI_auto)).setChecked(false);
						mPref.write("HDMIctrl_auto", false);
					}
				}
				
				mPref.write("HDMIctrl_input", ((CheckedTextView) v).isChecked());
				Log.i("tml CEC " + mPref.readBoolean("HDMIctrl_auto", true)
						+ mPref.readBoolean("HDMIctrl_input", true)
						+ mPref.readBoolean("HDMIctrl_tv", true));
			}
		});

		((CheckedTextView) layout.findViewById(R.id.HDMI_tv)).setChecked(tvon);
		((CheckedTextView) layout.findViewById(R.id.HDMI_tv)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked = ((CheckedTextView) v).isChecked();
				((CheckedTextView) v).setChecked(!checked);
				if (!checked) {
					if (AireJupiter.getInstance() != null) {
						AireJupiter.hdmiCmdExecSetCEC();
					}
				} else {
					boolean cauto = ((CheckedTextView) layout.findViewById(R.id.HDMI_auto)).isChecked();
					boolean input = ((CheckedTextView) layout.findViewById(R.id.HDMI_input)).isChecked();
					if (cauto && !input) {
						((CheckedTextView) layout.findViewById(R.id.HDMI_auto)).setChecked(false);
						mPref.write("HDMIctrl_auto", false);
					}
				}
				
				mPref.write("HDMIctrl_tv", ((CheckedTextView) v).isChecked());
				Log.i("tml CEC " + mPref.readBoolean("HDMIctrl_auto", true)
						+ mPref.readBoolean("HDMIctrl_input", true)
						+ mPref.readBoolean("HDMIctrl_tv", true));
			}
		});
		//***tml
//		((CheckedTextView) layout.findViewById(R.id.hdvideo)).setChecked(mPref.readBoolean("EnableHD", false));
//		((CheckedTextView) layout.findViewById(R.id.hdvideo)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				boolean checked=((CheckedTextView)v).isChecked();
//				((CheckedTextView)v).setChecked(!checked);
//			}
//		});
		//tml*** hd2
		((CheckedTextView) layout.findViewById(R.id.hdauto)).setChecked(mPref.readBoolean("EnableHD0", false));
		((CheckedTextView) layout.findViewById(R.id.hdauto)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((CheckedTextView) layout.findViewById(R.id.hdvideo)).isChecked()) {
					((CheckedTextView) layout.findViewById(R.id.hdvideo)).setChecked(false);
				}
				if (((CheckedTextView) layout.findViewById(R.id.hdvideo2)).isChecked()) {
					((CheckedTextView) layout.findViewById(R.id.hdvideo2)).setChecked(false);
				}
				if (((CheckedTextView)v).isChecked()) {
					((CheckedTextView)v).setChecked(false);
				} else {
					((CheckedTextView)v).setChecked(true);
				}
				mPref.write("EnableHD0", ((CheckedTextView)v).isChecked());  //alec*** no save sett/
				mPref.write("EnableHD", false);
				mPref.write("EnableHD2", false);
				Log.i("hd HDdef " + Boolean.toString(((CheckedTextView)v).isChecked())
						+ Boolean.toString(mPref.readBoolean("EnableHD"))
						+ Boolean.toString(mPref.readBoolean("EnableHD2")) + " "
						+ ((Button) layout.findViewById(R.id.brselection)).getText());
				//alec|tml*** no save sett
				if (AireVenus.instance() != null && DialerActivity.getDialer() == null) {
					Log.e("!!! STOPPING AireVenus/ServiceY *** SettingPage @ voip.settings-video");
					Intent itx = new Intent(MainActivity._this, AireVenus.class);
					MainActivity._this.stopService(itx);
				}
			}
		});
		
		((CheckedTextView) layout.findViewById(R.id.hdvideo)).setChecked(mPref.readBoolean("EnableHD", true));
		((CheckedTextView) layout.findViewById(R.id.hdvideo)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((CheckedTextView) layout.findViewById(R.id.hdauto)).isChecked()) {
					((CheckedTextView) layout.findViewById(R.id.hdauto)).setChecked(false);
				}
				if (((CheckedTextView) layout.findViewById(R.id.hdvideo2)).isChecked()) {
					((CheckedTextView) layout.findViewById(R.id.hdvideo2)).setChecked(false);
				}
				if (((CheckedTextView)v).isChecked()) {
					((CheckedTextView)v).setChecked(false);
				} else {
					((CheckedTextView)v).setChecked(true);
				}
				mPref.write("EnableHD0", false);
				mPref.write("EnableHD", ((CheckedTextView)v).isChecked());  //alec*** no save sett/
				mPref.write("EnableHD2", false);
				Log.i("hd HD720 " + Boolean.toString(mPref.readBoolean("EnableHD0"))
						+ Boolean.toString(((CheckedTextView)v).isChecked())
						+ Boolean.toString(mPref.readBoolean("EnableHD2")) + " "
						+ ((Button) layout.findViewById(R.id.brselection)).getText());
				//alec|tml*** no save sett
				if (AireVenus.instance() != null && DialerActivity.getDialer() == null) {
					Log.e("!!! STOPPING AireVenus/ServiceY *** SettingPage @ voip.settings-video");
					Intent itx = new Intent(MainActivity._this, AireVenus.class);
					MainActivity._this.stopService(itx);
				}
			}
		});
		
		((CheckedTextView) layout.findViewById(R.id.hdvideo2)).setChecked(mPref.readBoolean("EnableHD2", false));
		((CheckedTextView) layout.findViewById(R.id.hdvideo2)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((CheckedTextView) layout.findViewById(R.id.hdauto)).isChecked()) {
					((CheckedTextView) layout.findViewById(R.id.hdauto)).setChecked(false);
				}
				if (((CheckedTextView) layout.findViewById(R.id.hdvideo)).isChecked()) {
					((CheckedTextView) layout.findViewById(R.id.hdvideo)).setChecked(false);
				}
				if (((CheckedTextView)v).isChecked()) {
					((CheckedTextView)v).setChecked(false);
				} else {
					((CheckedTextView)v).setChecked(true);
				}
				mPref.write("EnableHD0", false);
				mPref.write("EnableHD", false);
				mPref.write("EnableHD2", ((CheckedTextView)v).isChecked());  //alec|tml*** no save sett/
				Log.i("hd HD1080 " + Boolean.toString(mPref.readBoolean("EnableHD0"))
						+ Boolean.toString(mPref.readBoolean("EnableHD"))
						+ Boolean.toString(((CheckedTextView)v).isChecked()) + " "
						+ ((Button) layout.findViewById(R.id.brselection)).getText());
				//alec|tml*** no save sett
				if (AireVenus.instance() != null && DialerActivity.getDialer() == null) {
					Log.e("!!! STOPPING AireVenus/ServiceY *** SettingPage @ voip.settings-video");
					Intent itx = new Intent(MainActivity._this, AireVenus.class);
					MainActivity._this.stopService(itx);
				}
			}
		});
		//bree一键广播
		((CheckedTextView) layout.findViewById(R.id.fast_radio)).setChecked(mPref.readBoolean("EnableRadio", false));
		((CheckedTextView) layout.findViewById(R.id.fast_radio)).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				((CheckedTextView) layout.findViewById(R.id.fast_radio)).setChecked(!mPref.readBoolean("EnableRadio", false));
				mPref.write("EnableRadio", ((CheckedTextView)v).isChecked());
			}
		});
		((CheckedTextView) layout.findViewById(R.id.ctv_vc720)).setChecked(mPref.readBoolean("VC720", false));
		((CheckedTextView) layout.findViewById(R.id.ctv_vc720)).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				((CheckedTextView) layout.findViewById(R.id.ctv_vc720)).setChecked(!mPref.readBoolean("VC720", false));
				mPref.write("VC720", ((CheckedTextView)v).isChecked());
				Log.i("Bree---->VC720"+mPref.readBoolean("VC720"));
			}
		});
		//li
		((CheckedTextView) layout.findViewById(R.id.multi_member_conf)).setChecked(mPref.readBoolean(Key.MULTI_MEMBER_CONF, false));
		((CheckedTextView) layout.findViewById(R.id.multi_member_conf)).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				((CheckedTextView) v).setChecked(!mPref.readBoolean(Key.MULTI_MEMBER_CONF, false));
				mPref.write(Key.MULTI_MEMBER_CONF, ((CheckedTextView)v).isChecked());
				Log.i("Lee---->MULTI_MEMBER_CONF:"+mPref.readBoolean(Key.MULTI_MEMBER_CONF));
			}
		});
		//***tml
		//tml*** bitrate
//		((SeekBar) layout.findViewById(R.id.seekBarBR)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
//			
//			@Override
//			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//				if (progress == 0) {
//					((TextView) layout.findViewById(R.id.showBRSeek)).setText(
//							MainActivity._this.getResources().getString(R.string.bitrateoptionL));
//				} else if (progress == 1) {
//					((TextView) layout.findViewById(R.id.showBRSeek)).setText(
//							MainActivity._this.getResources().getString(R.string.bitrateoptionM));
//				} else if (progress == 2) {
//					((TextView) layout.findViewById(R.id.showBRSeek)).setText(
//							MainActivity._this.getResources().getString(R.string.bitrateoptionH));
//				} else {
//					((TextView) layout.findViewById(R.id.showBRSeek)).setText(
//							MainActivity._this.getResources().getString(R.string.bitrateoptionM));
//				}
//			}
//
//			@Override
//			public void onStartTrackingTouch(SeekBar seekBar) {}
//
//			@Override
//			public void onStopTrackingTouch(SeekBar seekBar) {
//				mPref.write("BitrateSel", seekBar.getProgress());  //alec|tml*** no save sett/
//				Log.i("tmlhd BITRATE=" + seekBar.getProgress() + "="
//						+ ((TextView) layout.findViewById(R.id.showBRSeek)).getText() + " "
//						+ Boolean.toString(mPref.readBoolean("EnableHD"))
//						+ Boolean.toString(mPref.readBoolean("EnableHD2")));
//			}
//			
//		});
//		((SeekBar) layout.findViewById(R.id.seekBarBR)).setProgress(mPref.readInt("BitrateSel", 1));

		broption = mPref.readInt("BitrateSel", 1);
		String s_broption = "";
	    if (broption == 0) {
	    	s_broption = MainActivity._this.getResources().getString(R.string.bitrateoptionL);
	    } else if (broption == 1) {
	    	s_broption = MainActivity._this.getResources().getString(R.string.bitrateoptionM);
	    } else if (broption == 2) {
	    	s_broption = MainActivity._this.getResources().getString(R.string.bitrateoptionH);
	    }
	    ((Button) layout.findViewById(R.id.brselection)).setText(s_broption);

	    ((Button) layout.findViewById(R.id.brselection)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity._this);
				CharSequence[] options = {MainActivity._this.getResources().getString(R.string.bitrateoptionL),
						MainActivity._this.getResources().getString(R.string.bitrateoptionM),
						MainActivity._this.getResources().getString(R.string.bitrateoptionH)};
				int index = mPref.readInt("BitrateSel", 1);
				
				builder.setTitle(MainActivity._this.getResources().getString(R.string.bitrateoption));
				builder.setSingleChoiceItems(options, index, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int index) {
						switch (index) {
							case 0:
								mPref.write("BitrateSel", index);
								((Button) layout.findViewById(R.id.brselection))
										.setText(MainActivity._this.getResources().getString(R.string.bitrateoptionL));
								break;
							case 1:
								mPref.write("BitrateSel", index);
								((Button) layout.findViewById(R.id.brselection))
										.setText(MainActivity._this.getResources().getString(R.string.bitrateoptionM));
								break;
							case 2:
								mPref.write("BitrateSel", index);
								((Button) layout.findViewById(R.id.brselection))
										.setText(MainActivity._this.getResources().getString(R.string.bitrateoptionH));
								break;
						}
						Log.i("tmlhd BITRATE=" + index + "="
								+ ((Button) layout.findViewById(R.id.brselection)).getText() + " "
								+ Boolean.toString(mPref.readBoolean("EnableHD", true))
								+ Boolean.toString(mPref.readBoolean("EnableHD2")));
						dialog.dismiss();
					}
				});
				
				builder.create().show();
			}
	    });
		//***tml
		
		sortMethod = mPref.readInt("SortMethod", 1);
		((CheckedTextView) layout.findViewById(R.id.ch_Alphabetical)).setChecked(sortMethod==0);
		((CheckedTextView) layout.findViewById(R.id.ch_mostRecent)).setChecked(sortMethod==1);
		
		((CheckedTextView) layout.findViewById(R.id.ch_Alphabetical)).setOnClickListener(this);
		((CheckedTextView) layout.findViewById(R.id.ch_mostRecent)).setOnClickListener(this);
		
		currentView.findViewById(R.id.setting_cancel).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
//				MainActivity._this.switchInflater(0);  //alec|tml*** no save sett, CX
			}
		});
		
		currentView.findViewById(R.id.setting_save).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//alec|tml*** no save sett, CX
//				mPref.write("SortMethod", sortMethod);
//				UserPage.forceRefresh=true;
//				UserPage.needRefresh=true;
//				
//				if (photoChanged && photoPath != null) {
//					
//					photoPath = Global.SdcardPath_sent + "myself_photo_" + myIdx + ".jpg";
//					String tmpPhotoPath = Global.SdcardPath_sent + "tmp_myself_photo_" + myIdx + ".jpg";
//					MyUtil.copyFile(new File(tmpPhotoPath), new File(photoPath), true, MainActivity._this);
//					
//					mPref.write("myPhotoPath", photoPath);
//					mPref.write("myPhotoUploaded", false);
//					Intent it = new Intent(Global.Action_InternalCMD);
//					it.putExtra(Key.COMMAND, Global.CMD_UPLOAD_PROFILE_PHOTO);
//					MainActivity._this.sendBroadcast(it);
//					photoChanged = false;
//				}
//
//				if (moodChanged) {
//					moodContent = moodView.getText().toString().trim();
//					if (!mPref.read("moodcontent").equals(moodContent)) {
//						mPref.write("moodcontent", moodContent);
//						if (!moodContent.contains("!!!!"))
//						{
//							mPref.write("moodcontentuploaded", false);
//							Intent it = new Intent(Global.Action_InternalCMD);
//							it.putExtra(Key.COMMAND, Global.CMD_UPLOAD_PROFILE_MOOD);
//							MainActivity._this.sendBroadcast(it);
//						}
//					}
//					moodChanged = false;
//				}
//
//				if (emailChanged) {
//					String last = mPref.read("email", "");
//					String email = emailView.getText().toString().trim();
//					if (email != null && email.length() > 6
//							&& check_email_syntax(email) && !email.equals(last)) {
//						mPref.write("email", email);
//						mPref.write("emailuploaded", false);
//						Intent it = new Intent(Global.Action_InternalCMD);
//						it.putExtra(Key.COMMAND, Global.CMD_UPLOAD_PROFILE_EMAIL);
//						MainActivity._this.sendBroadcast(it);
//					}
//					emailChanged = false;
//				}
//
//				if (nicknameChanged) {
//					String last = mPref.read("myNickname", "");
//					String nickname = nicknameView.getText().toString().trim();
//					boolean chinese = nickname.toLowerCase().equals(
//							nickname.toUpperCase());
//
//					if (nickname != null && nickname.length() >= (chinese ? 2 : 6)
//							&& !last.equals(nickname)) {
//						mPref.write("myNickname", nickname);
//						mPref.write("nicknameUpdated", false);
//						Intent it = new Intent(Global.Action_InternalCMD);
//						it.putExtra(Key.COMMAND, Global.CMD_UPDATE_MY_NICKNAME);
//						MainActivity._this.sendBroadcast(it);
//					}
//					nicknameChanged = false;
//				}
//				
//				mPref.write("BlockStrangers", ((CheckedTextView) layout.findViewById(R.id.blockstranger)).isChecked());
//				mPref.write("notification_sound", ((CheckedTextView) layout.findViewById(R.id.notification_sound)).isChecked());
//				//tml*** normal ring
//				mPref.write("normal_ring", ((CheckedTextView) layout.findViewById(R.id.normal_ring)).isChecked());
//				mPref.write("EnableHD", ((CheckedTextView) layout.findViewById(R.id.hdvideo)).isChecked());
//				//tml*** hd2/
//				mPref.write("EnableHD2", ((CheckedTextView) layout.findViewById(R.id.hdvideo2)).isChecked());
//				Log.e("ENABLE HD>> " + mPref.readBoolean("EnableHD", true) + "|" + mPref.readBoolean("EnableHD2", false));
//				//tml*** bitrate/
//				mPref.write("BitrateSel", ((SeekBar) layout.findViewById(R.id.seekBarBR)).getProgress());
//				Log.e("BITRATE SEL>> " + mPref.readInt("BitrateSel", 4));
//				
//				if (AireJupiter.getInstance()!=null)
//					AireJupiter.getInstance().reconfigBeeHive();
//				
//				MainActivity._this.switchInflater(0);
//				
//				if (AireVenus.instance()!=null && DialerActivity.getDialer()==null)
//				{
//					Intent itx=new Intent(MainActivity._this, AireVenus.class);
//					MainActivity._this.stopService(itx);
//				}
			}
		});
		
		//tml*** beta ui2
//		((RelativeLayout) layout.findViewById(R.id.bar8_btn)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Intent it = new Intent(MainActivity._this, MainBrowser.class);  //tml*** browser save/
//				it.putExtra("launchFromSelf", true);
//				MainActivity._this.startActivity(it);
//				MainActivity._this.finish();
//			}
//		});
		((RelativeLayout) layout.findViewById(R.id.bar6_btn)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it = new Intent(MainActivity._this, ShoppingActivity.class);
				it.putExtra("launchFromSelf", true);
				MainActivity._this.startActivity(it);
				MainActivity._this.finish();
			}
		});
		
		//tml*** testing TODO
		if (Log.enDEBUG) {
			if (mPref.readBoolean("TESTING", false)) {
				((RelativeLayout) layout.findViewById(R.id.TESTINGONLY)).setVisibility(View.VISIBLE);
			} else {
				((RelativeLayout) layout.findViewById(R.id.TESTINGONLY)).setVisibility(View.GONE);
			}
			mAudioManager = ((AudioManager) MainActivity._this.getSystemService(Context.AUDIO_SERVICE));
			
			((CheckBox) currentView.findViewById(R.id.show_test)).setOnCheckedChangeListener(new OnCheckedChangeListener () {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (!isChecked) {
						mPref.write("TESTING", false);
						((RelativeLayout) layout.findViewById(R.id.TESTINGONLY)).setVisibility(View.GONE);
						Intent it = new Intent(MainActivity._this, Tooltip.class);
						String content = "test buttons disabled";
			            it.putExtra("Content", content);
			            MainActivity._this.startActivity(it);
					}
				}
			});
			String Tb0 = "Tb0";
			String Tb1 = "Tb1";
			String Tb2 = "Tb2";
			String Tb3 = "Tb3";
			String Tb4 = "Tb4";
			String Tb5 = "Tb5";
			String Tb6 = "Tb6";
			String Tb7 = "Tb7";
			String Tb8 = "Tb8";
			String Tb9 = "Tb9";
			((ToggleButton) currentView.findViewById(R.id.TEST0)).setTag(Tb0);
			((ToggleButton) currentView.findViewById(R.id.TEST1)).setTag(Tb1);
			((ToggleButton) currentView.findViewById(R.id.TEST2)).setTag(Tb2);
			((ToggleButton) currentView.findViewById(R.id.TEST3)).setTag(Tb3);
			((ToggleButton) currentView.findViewById(R.id.TEST4)).setTag(Tb4);
			((ToggleButton) currentView.findViewById(R.id.TEST5)).setTag(Tb5);
			((ToggleButton) currentView.findViewById(R.id.TEST6)).setTag(Tb6);
			((ToggleButton) currentView.findViewById(R.id.TEST7)).setTag(Tb7);
			((ToggleButton) currentView.findViewById(R.id.TEST8)).setTag(Tb8);
			((ToggleButton) currentView.findViewById(R.id.TEST9)).setTag(Tb9);
			((ToggleButton) currentView.findViewById(R.id.TEST0)).setChecked(mPref.readBoolean(Tb0, false));
			((ToggleButton) currentView.findViewById(R.id.TEST1)).setChecked(mPref.readBoolean(Tb1, false));
			((ToggleButton) currentView.findViewById(R.id.TEST2)).setChecked(mPref.readBoolean(Tb2, false));
			((ToggleButton) currentView.findViewById(R.id.TEST3)).setChecked(mPref.readBoolean(Tb3, false));
			((ToggleButton) currentView.findViewById(R.id.TEST4)).setChecked(mPref.readBoolean(Tb4, false));
			((ToggleButton) currentView.findViewById(R.id.TEST5)).setChecked(mPref.readBoolean(Tb5, false));
			((ToggleButton) currentView.findViewById(R.id.TEST6)).setChecked(mPref.readBoolean(Tb6, false));
			((ToggleButton) currentView.findViewById(R.id.TEST7)).setChecked(mPref.readBoolean(Tb7, false));
			((ToggleButton) currentView.findViewById(R.id.TEST8)).setChecked(mPref.readBoolean(Tb8, false));
			((ToggleButton) currentView.findViewById(R.id.TEST9)).setChecked(mPref.readBoolean(Tb9, false));
			
			((ToggleButton) currentView.findViewById(R.id.TEST0)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String ptag = "AIRECALL";  //
					boolean btncheck = ((ToggleButton) v).isChecked();
					testState(v, ptag, null, btncheck);
					
//					MainActivity._this.showAirecall(btncheck);

//					String tcploc = moodView.getText().toString().toLowerCase().trim();
//					if (tcploc.equals("china") || tcploc.equals("usa")) {
//						if (tcploc.equals("china")) {
//							mPref.write("NEWTCPCHINA", true);
//							mPref.write("NEWTCPUSA", false);
//						} else if (tcploc.equals("usa")) {
//							mPref.write("NEWTCPCHINA", false);
//							mPref.write("NEWTCPUSA", true);
//						}
//					} else {
//						if (btncheck) {
//							mPref.write("NEWTCPCHINA", false);
//							mPref.write("NEWTCPUSA", false);
//							Toast.makeText(MainActivity._this, "invalid server", Toast.LENGTH_LONG).show();
//						}
//					}
//					AireJupiter.getInstance().tcpSocket.disconnectAndReconnect("new server", false);
					
//					String cpu = moodView.getText().toString().toLowerCase().trim();
//					MyUtil.setCPU(false, null, null, cpu);
				}
			});
			((ToggleButton) currentView.findViewById(R.id.TEST1)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String ptag = "BROADCAST";
					boolean btncheck = ((ToggleButton) v).isChecked();
					testState(v, ptag, null, btncheck);
					
					boolean othervc = ((ToggleButton) layout.findViewById(R.id.TEST2)).isChecked();
					if (btncheck && othervc == btncheck) {
						((ToggleButton) layout.findViewById(R.id.TEST2)).performClick();
					}
				}
			});
			((ToggleButton) currentView.findViewById(R.id.TEST2)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String ptag = "VCx8";
					boolean btncheck = ((ToggleButton) v).isChecked();
					testState(v, ptag, null, btncheck);
					
					boolean othervc = ((ToggleButton) layout.findViewById(R.id.TEST1)).isChecked();
					if (btncheck && othervc == btncheck) {
						((ToggleButton) layout.findViewById(R.id.TEST1)).performClick();
					}
					
					/*
					 * HC-12-USB
					 * usbkey=/dev/bus/usb/001/00X
					 * usbdat name=/dev/bus/usb/001/00X id=100X pid=22336 vid=1154
					 * usbdat prot=0 ifc=2 uifid=1,0,  cls=2 subcl=0
					 * cls=2	USB_CLASS_COMM 
					 * subcl=0	USB_CLASS_PER_INTERFACE 
					 * Ep[0/3|0] is USB_DIR_IN  > stb (interrupt)
					 * Ep[1/3|1] is USB_DIR_OUT > usb (bulk)
					 * Ep[2/3|1] is USB_DIR_IN  > stb (bulk)
					 */
//					Log.i("=======");
//					UsbManager usbManager = (UsbManager) MainActivity._this.getSystemService(Context.USB_SERVICE);
//					HashMap<String, UsbDevice> usbMap = usbManager.getDeviceList();
//					
//					Set<String> set1 = usbMap.keySet();
//					for (String s1 : set1) {
//						Log.e("usbkey=" + s1);
//						UsbDevice usbd = usbMap.get(s1);
//						String name = usbd.getDeviceName();
//						int id = usbd.getDeviceId();
//						int pid = usbd.getProductId();
//						int vid = usbd.getVendorId();
//						int prtcl = usbd.getDeviceProtocol();
//						int ifc = usbd.getInterfaceCount();
//						UsbInterface[] uif = new UsbInterface[ifc];
//						String uifid = "";
//						for (int i = 0; i < ifc; i++) {
//							uif[i] = usbd.getInterface(i);
//							uifid = uif[i].getId() + "," + uifid;
//						}
//						int cls = usbd.getDeviceClass();
//						int subcls = usbd.getDeviceSubclass();
//						Log.e("usbinfo  name=" + name + " id=" + id + " pid=" + pid
//								+ " vid=" + vid);
//						Log.e("usbinfo  prtcl=" + prtcl + " ifc=" + ifc
//								+ " uifid=" + uifid + " " + " cls=" + cls + " subcl=" + subcls);
//						Log.e("----------");
//					}
//					Log.i("=======");
				}
			});
			((ToggleButton) currentView.findViewById(R.id.TEST3)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String ptag = "USB_OUT";
					boolean btncheck = ((ToggleButton) v).isChecked();
					testState(v, ptag, null, btncheck);
					AireJupiter.getInstance().abortUsbDongle(-1);
				}
			});
			((ToggleButton) currentView.findViewById(R.id.TEST4)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String ptag = "USB_IN_2";
					boolean btncheck = ((ToggleButton) v).isChecked();
					testState(v, ptag, null, btncheck);
					AireJupiter.getInstance().abortUsbDongle(-1);

//					String ptag = "BLUEID";
//					boolean btncheck = ((ToggleButton) v).isChecked();
//					testState(v, ptag, null, btncheck);
//					mPref.write("BLUEUUID", moodView.getText().toString());
//					Log.e("myblue  " + moodView.getText().toString());
					
//					MyUtil.getCPU(true);
//					MyUtil.setCPU(false, moodView.getText().toString(), null, null);
//					MyUtil.getCPU(true);
					
//					Thread t = new Thread() {
//					    public void run() {
//							String fimage[] = moodView.getText().toString().split(",");
//							int testbreak = 200000;
//							if (fimage.length == 3) {
//								try {
//									testbreak = Integer.parseInt(fimage[2]);
//								} catch (Exception e) {
//									Log.e("suv!!! testvalue ERR " + e.getMessage());
//								}
//							}
//							Log.e("  --- suv TESTimages " + fimage[0] + " " + fimage[1] + " @" + testbreak);
//							
//							File image1 = new File("/storage/emulated/0/AireTalk/images/", fimage[0] + ".bmp");
//							File image2 = new File("/storage/emulated/0/AireTalk/images/", fimage[1] + ".bmp");
//							
//							if (!image1.exists()) {
//								image1 = new File("/storage/emulated/0/AireTalk/images/", fimage[0] + ".jpg");
//								if (!image1.exists()) {
//									Log.e("suv!!! FILE ERR");
//									return;
//								}
//							}
//
//							if (!image2.exists()) {
//								image2 = new File("/storage/emulated/0/AireTalk/images/", fimage[1] + ".jpg");
//								if (!image2.exists()) {
//									Log.e("suv!!! FILE ERR");
//									return;
//								}
//							}
//							
//							BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//							Bitmap bitmap1 = BitmapFactory.decodeFile(image1.getAbsolutePath(), bmOptions);
//							Bitmap bitmap2 = BitmapFactory.decodeFile(image2.getAbsolutePath(), bmOptions);
//							
//							MotionDetect mMotionDetect = new MotionDetect();
//							mMotionDetect.load(MainActivity._this, 1280, 720, 0, "");
//							mMotionDetect.detect_V2(null, 0, 1280, 720, testbreak, bitmap1);
//							mMotionDetect.detect_V2(null, 0, 1280, 720, testbreak, bitmap2);
//					    }
//					};
//					t.start();
				}
			});
			((ToggleButton) currentView.findViewById(R.id.TEST5)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
//					String ptag = "qwerty";
//					boolean btncheck = ((ToggleButton) v).isChecked();
//					testState(v, ptag, null, btncheck);
				}
			});
			((ToggleButton) currentView.findViewById(R.id.TEST6)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
//					String ptag = "testSUV2";
//					boolean btncheck = ((ToggleButton) v).isChecked();
//					testState(v, ptag, "NEW", btncheck);
					
					Intent it = new Intent(MainActivity._this, SurveillanceDialog.class);
					it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					it.putExtra("nickname", "test");
					MainActivity._this.startActivity(it);
				}
			});
			((ToggleButton) currentView.findViewById(R.id.TEST7)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String ptag = "testSUV";
					boolean btncheck = ((ToggleButton) v).isChecked();
					testState(v, ptag, "NTFY", btncheck);
				}
			});
			((ToggleButton) currentView.findViewById(R.id.TEST8)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String ptag = "testSUV3";
					boolean btncheck = ((ToggleButton) v).isChecked();
					testState(v, ptag, "REC", btncheck);
				}
			});
			((ToggleButton) currentView.findViewById(R.id.TEST9)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
//					String ptag = "TEST_PHP";
//					boolean btncheck = ((ToggleButton) v).isChecked();
//					testState(v, ptag, null, btncheck);
					
//					int testx = Integer.parseInt(moodView.getText().toString());
//					int testv = testx % 100;
//					Log.e("test " + testv);
					
					String content = MyUtil.testFirmwareRW();
		    		Intent it = new Intent(MainActivity._this, Tooltip.class);
		            it.putExtra("Content", content);
		            MainActivity._this.startActivity(it);
				}
			});

			if (mPref.readBoolean("voice_control", false)) {
				class MyRecognitionListener implements RecognitionListener {
					int maxCount = 5;
		            @Override
		            public void onBeginningOfSpeech() {
		            }
		            @Override
		            public void onBufferReceived(byte[] buffer) {
		            }
		            @Override
		            public void onEndOfSpeech() {
		            }
		            @Override
		            public void onError(int error) {
						mSpeechRecognizer.cancel();
						String errortext = "";
						if (error == 3) {
							errortext = "\n . e" + error + " (audio)\n";
						} else if (error == 5) {
							errortext = "\n . e" + error + " (client/other)\n";
						} else if (error == 9) {
							errortext = "\n . e" + error + " (permissions)\n";
						} else if (error == 2) {
							errortext = "\n . e" + error + " (network1)\n";
						} else if (error == 1) {
							errortext = "\n . e" + error + " (network2)\n";
						} else if (error == 7) {
							errortext = "\n . e" + error + " (no match)\n";
						} else if (error == 8) {
							errortext = "\n . e" + error + " (busy)\n";
						} else if (error == 4) {
							errortext = "\n . e" + error + " (server)\n";
						} else if (error == 6) {
							errortext = "\n . e" + error + " (timeout)\n";
						}
						((TextView) MainActivity._this.findViewById(R.id.testresultsT)).append(errortext);
						if (voiceIntent != null) {
							Log.e("tmlvoice tryagain 1ERROR=" + error);
							mSpeechRecognizer.startListening(voiceIntent);
						} else {
							Log.e("tmlvoice tryagain 0ERROR=" + error);
							mHandler.post(voiceRecogn);
						}
		            }
		            @Override
		            public void onEvent(int eventType, Bundle params) {
		            }
		            @Override
		            public void onPartialResults(Bundle partialResults) {
		            	ArrayList<String> matches = null;
		    			
		    			if (partialResults != null) {
		    				matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		    				String spokePartial = "";
		    	        	if (matches != null && matches.size() > 0) {
		    	            	for (int i = 0; i < matches.size(); i++) {
		    	            		spokePartial = matches.get(i).toLowerCase(Locale.getDefault()) + ", " + spokePartial;
		    	            	}
		    		        	Log.e("tmlvoice onPartialResults (" + matches.size() + ") spokePartial >>> " + spokePartial);
		    	        	}
		    			}
		            }
		            @Override
		            public void onReadyForSpeech(Bundle params) {
		            	if (TESTcount == maxCount) {
		            		((ToggleButton) layout.findViewWithTag("TEST" + Integer.toString(TESTpos))).performClick();
		            	} else {
			            	TESTcount++;
			            	((TextView) MainActivity._this.findViewById(R.id.testresultsT)).append(TESTcount + " RDY! ... ");
		            	}
		            }
		            @Override
		            public void onResults(Bundle results) {
		    			ArrayList<String> matches = null;
		    			
		    			if (results != null) {
		    				matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		    				String spoke = "";
		    	        	if (matches != null && matches.size() > 0) {
		    	            	for (int i = 0; i < matches.size(); i++) {
		    	            		spoke = matches.get(i).toLowerCase(Locale.getDefault()) + ", " + spoke;
		    	            	}
	    	            		TESTdataS = TESTdataS + spoke;
		    	            	((TextView) MainActivity._this.findViewById(R.id.testresultsT)).append(spoke);
		    		        	Log.e("tmlvoice onResults (" + matches.size() + ") spoke >>> " + spoke);
		        				mSpeechRecognizer.stopListening();
		        				mSpeechRecognizer.startListening(voiceIntent);
		    	        	} else {
		        				Log.e("tmlvoice tryagain MATCHES=0");
		        				mSpeechRecognizer.stopListening();
		        				mSpeechRecognizer.startListening(voiceIntent);
		        				((TextView) MainActivity._this.findViewById(R.id.testresultsT)).append("matches=0");
		    	        	}
		    			} else {
		    				Log.e("tmlvoice tryagain RESULTS=null");
		    				mSpeechRecognizer.stopListening();
		    				mSpeechRecognizer.startListening(voiceIntent);
		    				((TextView) MainActivity._this.findViewById(R.id.testresultsT)).append("results=null");
		            	}
		            }
		            @Override
		            public void onRmsChanged(float rmsdB) {
		            }
				}
				MyRecognitionListener voiceListener = new MyRecognitionListener();
				mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity._this);
		        mSpeechRecognizer.setRecognitionListener(voiceListener);
			}
		} else {
			((TextView) currentView.findViewById(R.id.testresultsT)).setVisibility(View.GONE);
			((RelativeLayout) currentView.findViewById(R.id.TESTINGONLY)).setVisibility(View.GONE);
		}

	}
	//tml*** voice control
	Runnable vanswerTooltip = new Runnable() {
    	public void run() {
    		String help = MainActivity._this.getString(R.string.vanswer_help1) + "\n"
    				+ ". " + MainActivity._this.getString(R.string.v_answer0) + ", "
    				+ MainActivity._this.getString(R.string.v_answer1) + ", "
    	    		+ MainActivity._this.getString(R.string.v_answer2) + ", "
    	    		+ MainActivity._this.getString(R.string.v_answer3) + "\n"
    	    		+ MainActivity._this.getString(R.string.vanswer_help2) + "\n"
    	    		+ ". " + MainActivity._this.getString(R.string.v_end0) + ", "
    				+ MainActivity._this.getString(R.string.v_end1) + ", "
    	    		+ MainActivity._this.getString(R.string.v_end2) + "\n\n"
    	    		+ MainActivity._this.getString(R.string.vanswer_help3);
    		Intent it = new Intent(MainActivity._this, Tooltip.class);
            it.putExtra("Content", help);
            MainActivity._this.startActivity(it);
    	}
    };
	//tml*** cec
	Runnable hdmianswerTooltip = new Runnable() {
    	public void run() {
    		String help = MainActivity._this.getString(R.string.hdmi_help) + "\n\n"
    				+ MainActivity._this.getString(R.string.hdmiauto_help) + "\n"
    				+ MainActivity._this.getString(R.string.hdmiinput_help) + "\n"
    	    		+ MainActivity._this.getString(R.string.hdmitv_help) + "\n\n"
    	    		+ MainActivity._this.getString(R.string.hdmi_FW);
    		Intent it = new Intent(MainActivity._this, Tooltip.class);
            it.putExtra("Content", help);
            MainActivity._this.startActivity(it);
    	}
    };
	
	//tml*** passwords
	Runnable showRetrievePWTooltip = new Runnable() {
    	public void run() {
    		Intent it = new Intent(MainActivity._this,Tooltip.class);
            it.putExtra("Content", MainActivity._this.getString(R.string.where_password));
            MainActivity._this.startActivity(it);
    	}
    };
	Runnable showRetPWSpamTooltip = new Runnable() {
    	public void run() {
    		Intent it = new Intent(MainActivity._this,Tooltip.class);
            it.putExtra("Content", MainActivity._this.getString(R.string.where_passwordspam));
            MainActivity._this.startActivity(it);
    	}
    };
    //***tml

	public boolean check_email_syntax(String email) {
		if (email == null || email.length() > 64 || email.length() < 6)
			return false;
		boolean emailok = email.matches("^[0-9a-zA-Z_\\-\\.]+@[0-9a-zA-Z_\\-\\.]+$");
		Log.e("emailok " + emailok);
		return emailok;
	}

	private void checkEmail() {
		String email = emailView.getText().toString().trim();
		if (emailChanged && email != null && email.length() > 0) {
			if (!check_email_syntax(email)) {
				Intent int2 = new Intent(MainActivity._this, CommonDialog.class);
				int2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
						| Intent.FLAG_ACTIVITY_SINGLE_TOP);
				int2.putExtra("msgContent",
						MainActivity._this.getString(R.string.email_invalid));
				int2.putExtra("numItems", 1);
				int2.putExtra("ItemCaption0",
						MainActivity._this.getString(R.string.done));
				int2.putExtra("ItemResult0", -1);
				MainActivity._this.startActivity(int2);
			}
		}
	}

	private boolean checkNickname() {
		String nickname = nicknameView.getText().toString().trim();
		boolean chinese = nickname.toLowerCase().equals(nickname.toUpperCase());
		
		if (nicknameChanged && nickname != null && nickname.length() < (chinese ? 2 : 6)) {
			Intent int2 = new Intent(MainActivity._this, CommonDialog.class);
			int2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			int2.putExtra("msgContent",
					MainActivity._this.getString(R.string.nickname_invalid));
			int2.putExtra("numItems", 1);
			int2.putExtra("ItemCaption0",
					MainActivity._this.getString(R.string.done));
			int2.putExtra("ItemResult0", -1);
			MainActivity._this.startActivity(int2);
			nicknameChanged = false;
			return false;
		}
		return true;
	}

	private void onPickPictureOption() {
		final CharSequence[] items = {
				MainActivity._this.getResources().getString(R.string.photo_gallery),
				MainActivity._this.getResources().getString(R.string.takepicture) };
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity._this);
		builder.setTitle(MainActivity._this.getResources().getString(R.string.choose_photo_source));
		builder.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {
				if (item == 0)
					onPickPicture();
				else if (item == 1)
					onTakePicture();
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int item) {

					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void onPickPicture() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		String title = MainActivity._this.getResources().getString(R.string.choose_photo_source);
		MainActivity._this.startActivityForResult(Intent.createChooser(intent, title), 1);
	}

	private void onTakePicture() {
		try {
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			photoPath = Global.SdcardPath_sent + "tmp.jpg";
			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(photoPath)));
			MainActivity._this.startActivityForResult(intent, 3);
		} catch (Exception e) {
			Toast.makeText(MainActivity._this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
		}
	}

	Runnable showTooltip = new Runnable() {
		@Override
		public void run() {
			Intent it = new Intent(MainActivity._this, Tooltip.class);
			it.putExtra("Content", MainActivity._this.getString(R.string.help_photo_is_needed));
			MainActivity._this.startActivity(it);
		}
	};

	@Override
	public void destroy() {
		mHandler.post(voiceRecogndestroy);
		mPref.writeLong("last_show_time", new Date().getTime());

		//alec*** no save sett
		if (photoChanged && photoPath != null)
		{
			mPref.write("myPhotoPath", photoPath);
			mPref.write("myPhotoUploaded", false);
			Intent it = new Intent(Global.Action_InternalCMD);
			it.putExtra("Command", Global.CMD_UPLOAD_PROFILE_PHOTO);
			MainActivity._this.sendBroadcast(it);
			photoChanged = false;
		}
		
		if (moodChanged) {
			moodContent = moodView.getText().toString().trim();
			Log.e("moodContent=" + moodContent);
			mPref.write("moodcontent", moodContent);
			if (!moodContent.contains("!!!!"))
			{
				mPref.write("moodcontentuploaded", false);
				Intent it = new Intent(Global.Action_InternalCMD);
				it.putExtra("Command", Global.CMD_UPLOAD_PROFILE_MOOD);
				MainActivity._this.sendBroadcast(it);
			}
		}
		
		if (emailChanged) {
			String last = mPref.read("email", "");
			String email = emailView.getText().toString().trim();
			if (email != null && email.length() > 6 && check_email_syntax(email) && !email.equals(last)) {
				mPref.write("email", email);
				mPref.write("emailuploaded", false);
				Intent it = new Intent(Global.Action_InternalCMD);
				it.putExtra("Command", Global.CMD_UPLOAD_PROFILE_EMAIL);
				MainActivity._this.sendBroadcast(it);
			}
			emailChanged = false;
		}
		
		checkNickname();
		if (nicknameChanged) {
			String last = mPref.read("myNickname", "");
			String nickname = nicknameView.getText().toString().trim();
			boolean chinese = nickname.toLowerCase().equals(nickname.toUpperCase());

			if (nickname != null && nickname.length() >= (chinese ? 2 : 6) && !last.equals(nickname)) {
				mPref.write("myNickname", nickname);
				mPref.write("nicknameUpdated", false);
				Intent it = new Intent(Global.Action_InternalCMD);
				it.putExtra("Command", Global.CMD_UPDATE_MY_NICKNAME);
				MainActivity._this.sendBroadcast(it);
			}
			nicknameChanged = false;
		}
		//***alec
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1 || requestCode == 3 || requestCode == 7) 
		{
			if (resultCode == Activity.RESULT_OK) {
				if (requestCode == 7) {

					try {
						Drawable photo = ImageUtil.getBitmapAsRoundCorner(photoPath, 3, 10);// alec
						if (photo != null) {

							hint.setVisibility(View.GONE);
							photoView.setImageDrawable(photo);
							photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
							photoChanged = true;
						}
					} catch (Exception e) {
					}

				} else if (requestCode == 1) {
					if (data == null) return;
					try {
						
						String outFilename = Global.SdcardPath_sent + "tmp_myself_photo_" + myIdx + ".jpg";
						photoPath = outFilename;
						
						ResizeImage.ResizeXYFromStream(MainActivity._this, data, outFilename, PHOTO_SIZE, 100);

						Drawable photo = ImageUtil.loadBitmapSafe(outFilename, 1);// alec
						if (photo != null) {
							hint.setVisibility(View.GONE);
							photoView.setImageDrawable(photo);
							photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
							photoChanged = true;
						}

					} catch (Exception e) {
					}
				}
				else if (requestCode == 3) {				
					try {
						String outFilename = Global.SdcardPath_sent + "tmp_myself_photo_" + myIdx + ".jpg";
						ResizeImage.ResizeXY(MainActivity._this, photoPath, outFilename, PHOTO_SIZE, 100);
						photoPath = outFilename;
						
						String outFilename2 = Global.SdcardPath_inbox + "photo_" + myIdx + "b.jpg";
						ResizeImage.ResizeXY(MainActivity._this, photoPath, outFilename2, PHOTO_SIZE, 100);
						
						Drawable photo = ImageUtil.loadBitmapSafe(outFilename, 1);// alec
						if (photo != null) {
							hint.setVisibility(View.GONE);
							photoView.setImageDrawable(photo);
							photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
							photoChanged = true;
						}
					} catch (Exception e) {
					}
				}
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ch_Alphabetical://name
			sortMethod=0;
			break;
		case R.id.ch_mostRecent://time
			sortMethod=1;
			break;
		default:
			break;
		}
		UserPage.forceRefresh=true;
		UserPage.needRefresh=true;
		mPref.write("SortMethod", sortMethod);  //alec*** no save sett/
		((CheckedTextView) layout.findViewById(R.id.ch_Alphabetical)).setChecked(sortMethod==0);
		((CheckedTextView) layout.findViewById(R.id.ch_mostRecent)).setChecked(sortMethod==1);
	}
	
	//tml*** secret test
	Runnable showSecret = new Runnable() {
    	public void run() {
    		Intent it = new Intent(MainActivity._this, Tooltip.class);
    		String secret = moodView.getText().toString();
    		String content = "";
    		if (secret.toLowerCase().equals("debug(80aire")) {
    			String myidx = Integer.toString(Integer.parseInt(mPref.read("myID", "0"), 16));
    			String mymac = mPref.read("myPhoneNumber", "??????????????");
				String phpdone = Boolean.toString(mPref.readBoolean("accphpdone", false));
    			String mynotes = versnotes;
    			String iso = mPref.read("iso", "cn");
    			String lastSip = mPref.read("lastRegisSip", "n/a");
    			String lastCall = mPref.read("lastCallSip", "n/a");
    			if (AireJupiter.getInstance() != null) AireJupiter.getInstance().getIsoDomain();
    			if (Log.enDEBUG) {
        			content = myidx + ",  " + mymac
        					+ "\ncreate_entry(" + phpdone + "),  iso:" + iso
        					+ "\ntcp: " + MySocket.ServerDM_d + ":" + MySocket.ServerIP_d
                			+ "\nphp: " + AireJupiter.myAcDomain_default + ":" + AireJupiter.myPhpServer
	            			+ "\n-lastcall: " + lastSip + " " + lastCall;
    			} else {
    				content = "(" + phpdone + "),  iso:" + iso
    						+ "\n" + mynotes;
    			}
    		} else if (secret.toLowerCase().equals("log(80aire")) {
    			if (Log.enDEBUG) {
    				Log.enDEBUG = false;
    				showVersionNotes(true);
    			} else {
    				Log.enDEBUG = true;
    				showVersionNotes(false);
    			}
    			content = Boolean.toString(Log.enDEBUG);
			} else if (secret.toLowerCase().equals("support1")) {
				String mymac = mPref.read("myPhoneNumber", "??????????????");
				mymac = mymac.substring(mymac.length() - 12, mymac.length());
				String phpdone = Boolean.toString(mPref.readBoolean("accphpdone", false));
				content = mymac + "  (" + phpdone + ")";
			} else if (secret.toLowerCase().equals("support2")) {
				//nothing yet
			} else if (secret.toLowerCase().equals("support3")) {
				//nothing yet
			} else if (secret.toLowerCase().equals("test") && Log.enDEBUG) {
				int testview = ((RelativeLayout) layout.findViewById(R.id.TESTINGONLY)).getVisibility();
				if (testview == View.VISIBLE) {
					mPref.write("TESTING", false);
					((RelativeLayout) layout.findViewById(R.id.TESTINGONLY)).setVisibility(View.GONE);
					content = "test buttons disabled";
				} else {
					mPref.write("TESTING", true);
					((RelativeLayout) layout.findViewById(R.id.TESTINGONLY)).setVisibility(View.VISIBLE);
					((CheckBox) layout.findViewById(R.id.show_test)).setChecked(true);
	    			String myidx = Integer.toString(Integer.parseInt(mPref.read("myID", "0"), 16));
	    			String mymac = mPref.read("myPhoneNumber", "??????????????");
					String phpdone = Boolean.toString(mPref.readBoolean("accphpdone", false));
	    			String iso = mPref.read("iso", "cn");
	        		content = "test buttons ENABLED\n\n"
	        				+ myidx + ",  " + mymac
        					+ "\ncreate_entry(" + phpdone + "),  iso:" + iso;
				}
			}
    		moodContent = mPref.read("moodcontent", "");
    		moodView.setText(moodContent);
    		moodChanged = false;
            it.putExtra("Content", content);
            MainActivity._this.startActivity(it);
    	}
	};
	//tml*** beta ui
	private void showVersionNotes (boolean simple) {
		TextView showVersion = (TextView) layout.findViewById(R.id.show_version);
		String versionNotes = "";
		
		if (simple) {
			versionNotes = Integer.toString(rwVersionCode(2));
		} else {
			versionNotes = Integer.toString(rwVersionCode(2)) + versnotes;
		}
		
		showVersion.setText(versionNotes);
	}
    //tml*** alpha ui
	public int rwVersionCode (int mode) {
		int versionCode = 0;
		try {
			versionCode = MainActivity._this.getPackageManager()
					.getPackageInfo(MainActivity._this.getPackageName(), 0).versionCode;  //get
			if (mode == 1) {
				versionCode = mPref.readInt("versionCode", 0);  //read
			} else if (mode == 2) {
				mPref.write("versionCode", versionCode);  //write
			}
		} catch (NameNotFoundException e) {
		}
		return versionCode;
	}
	
	//test *** test *** test *** test *** test
	private void testState (View v, String tag, String toasttag, boolean check) {
		String btntag = v.getTag().toString();
		try {
			TESTpos = Integer.parseInt(btntag.substring(btntag.length() - 1));
		} catch (Exception e) {
			TESTpos = 99;
		}
		mPref.write(btntag, check);
		mPref.write(tag, check);
		if (toasttag == null) {
			toasttag = "";
		}
		
		boolean chk_btn = mPref.readBoolean(btntag);
		boolean chk_tag = mPref.readBoolean(tag);
		Log.e("TEST" + TESTpos + " " + toasttag + "(" + tag + "):" + chk_btn + "." + chk_tag);
		((TextView) layout.findViewById(R.id.TESTINFO)).setText("_");
		Toast tst = Toast.makeText(MainActivity._this, toasttag + "(" + tag + ")" + ":" + mPref.readBoolean(tag), Toast.LENGTH_SHORT);
		tst.setGravity(Gravity.CENTER, 0, 0);
		tst.show();
	}
	
	Runnable StopreturnView = new Runnable() {
		@Override
		public void run() {
			int getV = ((ScrollView) layout.findViewById(R.id.maincontent)).getVisibility();
			if (getV == View.GONE) {  //start,hide
				((ScrollView) layout.findViewById(R.id.maincontent)).setVisibility(View.VISIBLE);
				mHandler.post(voiceResults);
			}
		}
	};

	Runnable StartreturnView = new Runnable() {
		@Override
		public void run() {
			int getV = ((ScrollView) layout.findViewById(R.id.maincontent)).getVisibility();
			if (getV == View.VISIBLE) {  //start,hide
				((ScrollView) layout.findViewById(R.id.maincontent)).setVisibility(View.GONE);
				LedSpeakerUtil.setSpeakerOn();
				startRinging();
				mHandler.post(voiceRecogn);
			}
		}
	};
	
	private AudioManager mAudioManager;
	private MyRingerTask ringTask;
	private AudioTrack mAudioTrack = null;
	private InputStream inS = null;
	private DataInputStream dinS = null;
	private volatile boolean ringrdy = false;
	
	private void startRinging ()  {
		try {
			if (mAudioTrack == null && !ringrdy) {
				Log.d("tml STARTringing ***** DO!");
				if (mPref.readBoolean("normal_ring", true)) {
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
				float maxVol0 = AudioTrack.getMaxVolume();
				mAudioTrack.setStereoVolume(maxVol0, maxVol0);
				int maxVol1 = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
				int maxVol2 = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
				if (TESTdataB) {
					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (maxVol1 * 0.66), 0);
					mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, (int) (maxVol1 * 0.66), 0);
				} else {
					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol1, 0);
					mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, maxVol2, 0);
				}
				mHandler.post(new Runnable () {
					@Override
		            public void run() {
						ringrdy = true;
						ringTask = new MyRingerTask(0);
						ringTask.execute();
					}
				});
			} else {
				Log.w("already ringing");
			}
		} catch (Exception e) {
			Log.e("cannot handle incoming call " + e.getMessage());
			mAudioTrack = null;
			ringrdy = false;
		}
	}
	
	public void stopRinging() {
		if (mAudioTrack != null && ringrdy) {
			try {
				Log.d("tml STOPring ***** DO!");
				ringTask.cancel(true);
				ringrdy = false;
				if (inS != null) {
					inS.close();
				}
				if (dinS != null) {
					dinS.close();
				}
				inS = null;
				dinS = null;
				Log.d("tml STOPring ***** SUCCESS!");
			} catch (IOException e) {
				Log.e("stopRing " + e.getMessage());
			}
		}
	}
	
	private class MyRingerTask extends AsyncTask<Void, Void, Void> {
		int ringmode;
		MyRingerTask (int mode) {
			ringmode = mode;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
    		try {
				int bufferSize = 5120;
			    byte[] audiobuff = new byte[bufferSize];

				int i = 0;
				AssetManager am = MainActivity._this.getAssets();
				String musicfile;
				if (mPref.readBoolean("normal_ring", true) && ringmode == 0) {
					musicfile = "incring3_1616.pcm";
				} else {
					String intx = null;
					intx = Integer.toString(SettingPage.TESTpos);
					musicfile = "r16k_" + intx + ".raw"; 
					if (SettingPage.TESTpos == 0) musicfile = "incring3_1616.pcm";
				}
				Log.i("tml ringfile> " + musicfile);

				if (ringrdy) {
					inS = am.open(musicfile);
					dinS = new DataInputStream(inS);

					mAudioTrack.play();
					while(((i = dinS.read(audiobuff, 0, bufferSize)) > -1) ) {
						mAudioTrack.write(audiobuff, 0, i);
						if (ringTask.isCancelled()) {
							mAudioTrack.pause();
							mAudioTrack.flush();
							mAudioTrack.stop();
							mAudioTrack.release();
							Log.e("tml ring.mAudioTrack mid-cleared!");
							break;
						}
				    }
				}
				mAudioTrack = null;
				Log.d("tml MyRingerTask END");
    		} catch (Exception e) {
				Log.e("tml MyRingerTask ERR " + e.getMessage());
				ringrdy = false;
				mAudioTrack.stop();
				mAudioTrack.release();
				mAudioTrack = null;
				if (inS != null) {
					inS = null;
				}
				if (dinS != null) {
					dinS = null;
				}
			}
			return null;
		}

		@Override
		public void onPostExecute(Void result) {
		}
	}

	private Intent voiceIntent;
	Runnable voiceRecogn = new Runnable() {
		@Override
		public void run() {
			if (mPref.readBoolean("voice_control", false)) {
				Log.e("tmlv init voiceRecogn");
				voiceIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
				voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
				voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
				voiceIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
		        try {
		        	mSpeechRecognizer.startListening(voiceIntent);
		        } catch (Exception e) {
		        	Log.e("tmlv DA voiceRecogn.ERR " + e.getMessage());
		        }
			}
		}
	};
	
	Runnable voiceRecogndestroy = new Runnable() {
		@Override
		public void run() {
			if (mSpeechRecognizer != null) {
				mSpeechRecognizer.destroy();
			}
			if (voiceIntent != null) {
				voiceIntent = null;
			}
		}
	};
	
	Runnable stopSpeech = new Runnable() {
		@Override
		public void run() {
			stopRinging();
			LedSpeakerUtil.setSpeakerOff();
			if (mPref.readBoolean("voice_control", false)) {
				mHandler.removeCallbacks(voiceRecogn);
				if (mSpeechRecognizer != null) {
					mSpeechRecognizer.stopListening();
					mSpeechRecognizer.cancel();
				}
				if (voiceIntent != null) {
					voiceIntent = null;
				}
			}
		}
	};
	
	Runnable voiceResults = new Runnable() {
		@Override
		public void run() {
			((TextView) MainActivity._this.findViewById(R.id.testresultsT)).setText("");
			Intent it = new Intent(MainActivity._this, Tooltip.class);
            it.putExtra("Content", TESTdataS);
            MainActivity._this.startActivity(it);
            TESTdataS = "";
		}
	};
	
	public int execRootCmdTEST1(String param) {
		if (param != null && !param.equals("")) {
			try {
				Process localProcess = Runtime.getRuntime().exec("sh");
				Object localObject = localProcess.getOutputStream();
				DataOutputStream localDataOutputStream = new DataOutputStream((OutputStream) localObject);
				String str = String.valueOf(param);
				localObject = str + "\n";
				
				localDataOutputStream.writeBytes((String) localObject);
				localDataOutputStream.flush();
				localDataOutputStream.writeBytes("exit\n");
				localDataOutputStream.flush();
				localProcess.waitFor();
				localObject = localProcess.exitValue();
				Log.e("execRootCmdSilent = " + param);
				return (Integer) localObject;
			} catch (Exception e) {
				Log.e("execRootCmdSilent ERR " + e.getMessage());
			}
		}
		return -1;
	}

	public void execRootCmdTEST2(String write, String path) {
		try {
			String command[] = {"sh", "-c", "echo " + write + " > " + path};
			HdmiUtil.getStatus(path);
			Log.e("test" + TESTpos + " " + command[0] + " " + command[1] + " " + command[2]);
			Process process = Runtime.getRuntime().exec(command);
			BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			int read;
		    char[] buffer = new char[4096];
		    StringBuffer output = new StringBuffer();
		    while ((read = in.read(buffer)) > 0) {
		        output.append(buffer, 0, read);
		    }
		    in.close();
			process.waitFor();
		} catch (IOException e) {
			Log.e("test err0" + e.getMessage());
		} catch (InterruptedException e) {
			Log.e("test err1" + e.getMessage());
		}
	}

	public void execRootCmdTEST3(String param) {
		if (param != null && !param.equals("")) {
			Log.d("yang param=" + param);
//			String cec0F = "echo 0x0B > /sys/class/amhdmitx/amhdmitx0/cec_config";
			String [] str1 = param.split(">");
			if (str1.length > 1) {
				String  newpathString = str1[1];
				Log.d("yang []=" + str1[0] + " ; 1=" + str1[1]);
				String cmd = str1[0].substring(4).trim().toLowerCase(); 
				Log.d("yang new docec cmd ; newpahtString=" + cmd + "; path=" + newpathString);
				doCECTEST3(cmd, newpathString);
			}
		}
	}
	
	public void doCECTEST3(String status, String patch) {
		BufferedWriter bfwriter;
		try {
			bfwriter = new BufferedWriter(new FileWriter(patch));
			bfwriter.write(status);
			bfwriter.flush();
			bfwriter.close();
		} catch (FileNotFoundException e) {
			Log.e("doCECTEST3 ERR1 " + e.getMessage());
		} catch (IOException e) {
			Log.e("doCECTEST3 ERR2 " + e.getMessage());
		}
	}
	//*** test *** test *** test *** test *** test
	
}

/* test code save



 */
