package com.pingshow.amper;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Date;

import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.google.zxing.WriterException;
import com.pingshow.amper.register.BaseRequestListener;
import com.pingshow.amper.register.BeforeRegisterActivity;
import com.pingshow.codec.VoiceMemoPlayer_NB;
import com.pingshow.codec.VoicePlayer2_MP;
import com.pingshow.codec.VoiceRecord2_MR;
import com.pingshow.network.MyNet;
import com.pingshow.network.MySocket;
import com.pingshow.qrcode.EncodingHandler;
import com.pingshow.util.DataCleanManager;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MCrypt;
import com.pingshow.util.MyProfile;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.util.ResizeImage;
import com.pingshow.voip.AireVenus;
import com.tencent.tauth.TAuthView;
import com.tencent.tauth.TencentOpenAPI;
import com.tencent.tauth.http.Callback;
import com.tencent.tauth.http.TDebug;
import com.weibo.net.AccessToken;
import com.weibo.net.ShareActivity;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboDialogListener;
import com.weibo.net.WeiboException;

public class SettingActivity extends Activity {
	public static final String vlib = "beta35t3r";
	private final String versnotes = "";
//	private final String versnotes = " TEST!\n" + vlib + ", stime, usrphp";
	private String secretValue = "";
	
	private MyPreference mPref;
	private int setTalkBackground = 0;
	private String moodContent = null;
	private String photoPath = null;
	private boolean photoChanged = false;
	private boolean moodChanged = false;
	private boolean emailChanged = false;
	private boolean nicknameChanged = false;
	private EditText emailView;
	private EditText nicknameView;
	private EditText moodView;
	private ImageView photoView;
	private final Handler mHandler = new Handler();
	
	public static final int PHOTO_SIZE = 320;  //720

	private Uri uriOrig = null;
	// facebook
	private AsyncFacebookRunner mAsyncRunner;
	private Facebook facebook;

	// weibo
	private static final String CONSUMER_KEY = "2748488668";
	private static final String CONSUMER_SECRET = "bc38614d7c45d659426a1d41124d5018";
	private Weibo mWeibo;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting_page);
		this.overridePendingTransition(R.anim.freeze, R.anim.freeze);

		neverSayNeverDie(SettingActivity.this);  //tml|bj*** neverdie/

		mPref = new MyPreference(this);

		TextView et = (TextView) findViewById(R.id.phone_number);
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

//		jack 2.4.51 logout
		TextView logout = (TextView) findViewById(R.id.tv_logout);
		logout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent logout = new Intent(SettingActivity.this, BeforeRegisterActivity.class);
				ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					am.clearApplicationUserData();
					startActivity(logout);
					finish();
				}else {
					Toast.makeText(SettingActivity.this, "Android版本过低!退出失败...", Toast.LENGTH_SHORT).show();
				}
////				Method method = am.getClass().getDeclaredMethod("clearApplicationUserData", IPackageDataObserver.class);
//				try {
//					Method method = am.getClass().getDeclaredMethod("clearApplicationUserData", IPackageDataObserver.class);
//					method.invoke(am, "com.pingshow.amper", new ClearUserDataObserver());
//				} catch (IllegalAccessException e) {
//					e.printStackTrace();
//				} catch (InvocationTargetException e) {
//					e.printStackTrace();
//				} catch (NoSuchMethodException e) {
//					e.printStackTrace();
//				}

			}
		});

		nicknameView = (EditText) findViewById(R.id.nickname);
		nicknameView.setText(mPref.read("myNickname", ""));
		nicknameView.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				nicknameChanged = true;
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
		nicknameView.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus)
					checkNickname();
			}
		});
		
		//li*** qr addf
		((ImageButton) findViewById(R.id.my_qr)).setOnClickListener(new OnClickListener() {
			@Override
    		public void onClick(View v) {
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
					try {
						String address = mPref.read("myPhoneNumber", "----");
						String nickname = mPref.read("myNickname", "");
						int idx = Integer.parseInt(mPref.read("myID", "0"), 16);
						String qrContent = "AddAireFriend," + address + "," + idx + "," + nickname;
						//li*** encrypt
						Log.d("qr preXN= " + qrContent);
						byte[] bytes = MCrypt.encrypt(qrContent.getBytes());
						qrContent = Base64.encodeToString(bytes, Base64.DEFAULT);
						Log.d("qr postXN= " + qrContent);

						DisplayMetrics displaymetrics = new DisplayMetrics();
						getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
						int width = displaymetrics.widthPixels;
						int height = displaymetrics.heightPixels;
						if (width > height) width = height;
						Bitmap qrCodeBitmap = EncodingHandler.createQRCode(qrContent, width * 3 / 5);
						Dialog qrCode = new Dialog(SettingActivity.this);
						View view = View.inflate(SettingActivity.this, R.layout.user_qrcode, null);
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
					Toast.makeText(SettingActivity.this, getString(R.string.old_sdk), Toast.LENGTH_SHORT).show();
				}
    		}
		});

		moodContent = mPref.read("moodcontent", "");
		moodView = (EditText) findViewById(R.id.my_mood);
		if (moodContent.length() > 0) {
			moodView.setText(moodContent);
		}
		moodView.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				moodChanged = true;
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				//tml*** secret test
				String secret = s.toString();
				if (secret.toLowerCase().equals("log(80aire")
						|| (secret.toLowerCase().equals("debug(80aire") && Log.enDEBUG)
						|| (secret.toLowerCase().equals("debugoff") && Log.enDEBUG)
						|| (secret.toLowerCase().equals("msg280") && Log.enDEBUG)
						|| (secret.toLowerCase().equals("newtcpchina") && Log.enDEBUG)
						|| (secret.toLowerCase().equals("newtcpusa") && Log.enDEBUG)
						|| secret.toLowerCase().equals("vc8443")
						|| secret.toLowerCase().equals("pay")
						|| (secret.toLowerCase().endsWith("test1") && Log.enDEBUG)
						|| (secret.toLowerCase().endsWith("test2") && Log.enDEBUG)
						|| (secret.toLowerCase().endsWith("test3") && Log.enDEBUG)) {
					mHandler.post(showSecret);
				}
				//***tml
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
		});

		emailView = (EditText) findViewById(R.id.email);
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

		photoView = (ImageView) findViewById(R.id.photo);
		photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		String path = mPref.read("myPhotoPath", null);

		if (path == null) {
			int uid = Integer.valueOf(mPref.read("myID", "0"), 16);
			path = Global.SdcardPath_sent + "myself_photo_" + uid + ".jpg";
			if (MyUtil.checkSDCard(this) && (new File(path).exists()))
				mPref.write("myPhotoPath", path);
			else
				path = null;
		}

		if (path == null || !MyUtil.checkSDCard(this) || !(new File(path).exists())) {
//			photoView.setImageResource(R.drawable.empty);
			//xwf*** circle pic
			BitmapFactory.decodeResource(getResources(), R.drawable.empty);
			photoView.setImageBitmap(ImageUtil.getCircleBitmapResouce(getResources(), R.drawable.empty));
		} else {
			try {
//				Drawable photo = ImageUtil.getBitmapAsRoundCorner(path, 3, 10);
//				photoView.setImageDrawable(photo);
				//xwf*** circle pic
				Bitmap photo = ImageUtil.getCircleBitmapPath(path, 3, 10);
				photoView.setImageBitmap(photo);
				TextView hint = (TextView) findViewById(R.id.my_photo_hint);
				hint.setVisibility(View.GONE);
			} catch (Exception e) {
				System.gc();
				System.gc();
//				photoView.setImageResource(R.drawable.empty);
				Bitmap photo = ImageUtil.getCircleBitmapResouce(getResources(), R.drawable.empty);
				photoView.setImageBitmap(photo);  //xwf*** circle pic
			}
		}

		photoView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onPickPictureOption();
			}
		});
		//tml*** temp alpha ui
        ((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			Intent myIntent = new Intent(SettingActivity.this, UsersActivity.class);
    			myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(myIntent);
				finish();
    		}}
        );
		((Button) findViewById(R.id.bFafauser))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(SettingActivity.this,
								UsersActivity.class));
						finish();
					}
				});
		if (!AmazonKindle.hasMicrophone_NoWarnning(this))
        	((Button)findViewById(R.id.bAireCall)).setVisibility(View.GONE);
		
		((Button)findViewById(R.id.bAireCall)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(SettingActivity.this, SipCallActivity.class));
				finish();
			}
		});

		((Button) findViewById(R.id.bSearch))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(SettingActivity.this,
								PublicWalkieTalkie.class));
						finish();
					}
				});
		((Button) findViewById(R.id.bMessage))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(SettingActivity.this,
								MessageActivity.class));
						finish();
					}
				});
		//tml*** beta ui, conference
		((Button)findViewById(R.id.bConference)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it = new Intent(SettingActivity.this, PickupActivity.class);
				it.putExtra("conference", true);
				startActivity(it);
        		finish();
			}
		});
        //tml*** beta ui2
		((Button) findViewById(R.id.bSetting)).setVisibility(View.GONE);
        if (mPref.read("iso", "cn").equals("cn")) {
        	((Button) findViewById(R.id.bSearch)).setVisibility(View.GONE);
        	((Button) findViewById(R.id.bSetting)).setVisibility(View.VISIBLE);
        } else {
        	((Button) findViewById(R.id.bSearch)).setVisibility(View.GONE);
        	((Button) findViewById(R.id.bSetting)).setVisibility(View.GONE);
        }

		mPref.write("LastPage", 3);

		String photoPath = mPref.read("myPhotoPath", null);
		if (photoPath == null) {
			int c = mPref.readInt("needPhotoTip", 0);
			if ((c % 4) == 0)
				mHandler.postDelayed(showTooltip, 1000);
			mPref.write("needPhotoTip", ++c);
		}

		final ToggleButton popup_smsdlg = (ToggleButton) findViewById(R.id.smspopup);
		popup_smsdlg.setChecked(mPref.readBoolean("popupSms", true));
		popup_smsdlg.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPref.write("popupSms", popup_smsdlg.isChecked());
			}
		});

		final ToggleButton ringingBtn = (ToggleButton) findViewById(R.id.notification_sound);
		ringingBtn.setChecked(mPref.readBoolean("notification_sound", true));
		ringingBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPref.write("notification_sound", ringingBtn.isChecked());
			}
		});

		final ToggleButton receivevibrateBtn = (ToggleButton) findViewById(R.id.receive_vibrate);
		receivevibrateBtn.setChecked(mPref.readBoolean("recvVibrator", true));
		receivevibrateBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPref.write("recvVibrator", receivevibrateBtn.isChecked());
			}
		});

		final ToggleButton wtBtn = (ToggleButton) findViewById(R.id.wt_sound_out);
		wtBtn.setChecked(mPref.readBoolean("wtSoundOut", true));
		wtBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPref.write("wtSoundOut", wtBtn.isChecked());
			}
		});
		//tml*** zoom
		final ToggleButton vzBtn = (ToggleButton) findViewById(R.id.en_vzoom);
		vzBtn.setChecked(mPref.readBoolean("enVideoZoom", true));
		vzBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPref.write("enVideoZoom", vzBtn.isChecked());
			}
		});
		
		final ToggleButton useBkImg = (ToggleButton) findViewById(R.id.usebkimg);
		useBkImg.setChecked(mPref.read("BackgroundImage",null)!=null);
		useBkImg.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String img=mPref.read("BackgroundImage",null);
				if (img==null)
				{
					setTalkBackground=1;
					onPickPicture();
				}
				else{
					mPref.write("BackgroundImage",null);
				}
			}
		});

//		String versionName = "1.0.0";
//		try {
//			versionName = getPackageManager().getPackageInfo(getPackageName(),
//					0).versionName;
//		} catch (NameNotFoundException e) {
//		}
//		((TextView) findViewById(R.id.version)).setText("Version: " + versionName);
		//tml*** beta ui
		if (Log.enDEBUG) {
			showVersionNotes(false);
		} else {
			showVersionNotes(true);
		}
		
		((RelativeLayout) findViewById(R.id.update)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (MyUtil.CheckServiceExists(SettingActivity.this, "com.pingshow.amper.AireJupiter")) {
					if (AireJupiter.getInstance()!=null) {
						AireJupiter.getInstance().forceCheckUpdate();
						AireJupiter.checkShow = true;
					}
				}
			}
		});
		//***tml
		
		((RelativeLayout) findViewById(R.id.share))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (((LinearLayout) findViewById(R.id.extend))
								.getVisibility() == View.GONE) {
							((LinearLayout) findViewById(R.id.extend))
									.setVisibility(View.VISIBLE);
						} else {
							((LinearLayout) findViewById(R.id.extend))
									.setVisibility(View.GONE);
						}
					}
				});

		((TextView) findViewById(R.id.share_by_email))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent it = new Intent(Intent.ACTION_SEND);
						it.putExtra(Intent.EXTRA_TEXT,
								getString(AmazonKindle.IsKindle()?R.string.invitation_amazon:R.string.invitation));
						it.putExtra(Intent.EXTRA_SUBJECT,
								getString(R.string.invitation_title));
						it.setType("text/plain");
						startActivity(it);
					}
				});
		
		((RelativeLayout) findViewById(R.id.feedback))
			.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent it = new Intent(SettingActivity.this, FeedbackActivity.class);
					startActivity(it);
				}
			});
		//tml*** beta ui
		((RelativeLayout) findViewById(R.id.settings_more)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((LinearLayout) findViewById(R.id.settings_morepanel)).getVisibility() == View.GONE) {
					((LinearLayout) findViewById(R.id.settings_morepanel)).setVisibility(View.VISIBLE);
				} else {
					((LinearLayout) findViewById(R.id.settings_morepanel)).setVisibility(View.GONE);
				}
			}
		});
		
		TelephonyManager tMgr=(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		String SubscribeId=tMgr.getSubscriberId();
		String iso=tMgr.getSimCountryIso();
		if (SubscribeId!=null && SubscribeId.length()>10 && !SubscribeId.contains("0000000") && iso!=null && iso.length()>0)
		{
			((TextView) findViewById(R.id.share_by_sms)).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.share_by_sms)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					try{
						Intent it = new Intent(Intent.ACTION_VIEW);
						it.putExtra("sms_body", getString(AmazonKindle.IsKindle()?R.string.invitation_amazon:R.string.invitation));
						it.setType("vnd.android-dir/mms-sms");
						startActivity(it);
					}catch(Exception e){}
				}
			});
		}
		else
			((TextView) findViewById(R.id.share_by_sms)).setVisibility(View.GONE);
		
		if (AmazonKindle.IsKindle())
		{
			((TextView) findViewById(R.id.share_by_sms)).setVisibility(View.GONE);
			((TextView) findViewById(R.id.share_on_qq)).setVisibility(View.GONE);
			((TextView) findViewById(R.id.share_on_weibo)).setVisibility(View.GONE);
		}
		
		if (!MyUtil.isAppInstalled(this,"com.facebook.katana"))
		{
			((TextView) findViewById(R.id.share_on_facebook)).setVisibility(View.GONE);
		}
		
		((TextView) findViewById(R.id.share_on_facebook))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						shareFaceBook();
					}
				});

		((TextView) findViewById(R.id.share_on_qq))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						auth(mAppid, "_self");
					}
				});

		((TextView) findViewById(R.id.share_on_weibo))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						shareWeibo();
					}
				});
		
		
		((RelativeLayout) findViewById(R.id.bkimg)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setTalkBackground=1;
				onPickPicture();
			}
		});
		
		final ToggleButton bsBtn = (ToggleButton) findViewById(R.id.blockstranger);
		bsBtn.setChecked(mPref.readBoolean("BlockStrangers", false));
		bsBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPref.write("BlockStrangers", bsBtn.isChecked());
			}
		});
		
		registerIntentReceivers();

		if (moodContent.endsWith("!!!!!!!"))
		{
			((RelativeLayout)findViewById(R.id.secret)).setVisibility(View.VISIBLE);
			((TextView)findViewById(R.id.secret_setting)).setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					Intent it = new Intent(SettingActivity.this, SecretActivity.class);
					startActivity(it);
				}
			});
		}

		if (mPref.readBoolean("secretDebug", false) && Log.enDEBUG) {
    		Intent it = new Intent(SettingActivity.this, Tooltip.class);
    		String content = mPref.read("secretDebugInfo", "")
    				+ "lastcall: " + mPref.read("lastRegisSip", "n/a") + "\n" +  mPref.read("lastCallSip", "n/a") + "\n";
            it.putExtra("Content", content);
            startActivity(it);
		} else {
			mPref.delect("secretDebug");
			mPref.delect("secretDebugInfo");
			mPref.delect("TESTING");
		}
	}

	
	public void shareFaceBook() {
		facebook = new Facebook("133881496748582");
		mAsyncRunner = new AsyncFacebookRunner(facebook);
		facebook.authorize(SettingActivity.this, new String[] { "publish_actions" },
			new Facebook.DialogListener() {
				@Override
				public void onComplete(Bundle values) {
					shareOnFacebookWall(facebook,"http://www.pingshow.net/img/aire.png",getString(R.string.invitation_title),
							getString(AmazonKindle.IsKindle()?R.string.invitation_amazon:R.string.invitation),"AIRE","http://www.airetalk.com");
				}
	
				@Override
				public void onFacebookError(FacebookError error) {
					Intent it = new Intent(SettingActivity.this,
							CommonDialog.class);
					it.putExtra("msgContent", error.getMessage());
					it.putExtra("numItems", 1);
					it.putExtra("ItemCaption0", getString(R.string.done));
					it.putExtra("ItemResult0", RESULT_OK);
					startActivity(it);
				}
	
				@Override
				public void onError(com.facebook.android.DialogError e) {
					Intent it = new Intent(SettingActivity.this,
							CommonDialog.class);
					it.putExtra("msgContent", e.getMessage());
					it.putExtra("numItems", 1);
					it.putExtra("ItemCaption0", getString(R.string.done));
					it.putExtra("ItemResult0", RESULT_OK);
					startActivity(it);
				}
	
				@Override
				public void onCancel() {
					Intent it = new Intent(SettingActivity.this,
							CommonDialog.class);
					it.putExtra("msgContent", "Canceled");
					it.putExtra("numItems", 1);
					it.putExtra("ItemCaption0", getString(R.string.done));
					it.putExtra("ItemResult0", RESULT_OK);
					startActivity(it);
				}
			});
	}
	
	private String mMsg="";
	Runnable displayStatus=new Runnable()
	{
		public void run() {
			Intent it = new Intent(SettingActivity.this,
					CommonDialog.class);
			it.putExtra("msgContent", mMsg);
			it.putExtra("numItems", 1);
			it.putExtra("ItemCaption0", getString(R.string.done));
			it.putExtra("ItemResult0", RESULT_OK);
			startActivity(it);
		}
	};
	
	private void shareOnFacebookWall(Facebook fb, String imageurl, String caption, String description, String name, String linkurl)
    {
        if(fb != null)
        {
            if(fb.isSessionValid())
            {
                Bundle b = new Bundle();
                b.putString("picture", imageurl);
                b.putString("caption",caption);
                b.putString("description",description );
                b.putString("name",name);
                b.putString("link",linkurl);
                try {
                    String strRet = "";
                    strRet = fb.request("/me/feed",b,"POST");
                    JSONObject json;
                    try {
                        json = Util.parseJson(strRet);
                        if(!json.isNull("id"))
                        {
                        	mMsg="Shared Successfully";
                        	mHandler.postDelayed(displayStatus,1500);
                        }
                        else
                        {
                            Log.e("shareOnFacebookWall Error: " + strRet);
                            mMsg="Fail to share. Please try later";
                        	mHandler.postDelayed(displayStatus,1500);
                        }
                    } catch (FacebookError e) {
                        mMsg="Failed to share :"+e.getMessage();
                    	mHandler.postDelayed(displayStatus,1500);
                    }
                } catch (Exception e) {
                	mMsg="Failed to share :"+e.getMessage();
                	mHandler.postDelayed(displayStatus,1500);
                }
            }
        }
    }
        
	public class UserRequestListener extends BaseRequestListener {
        @Override
        public void onComplete(final String response, final Object state) {
        	
        }

		@Override
		public void onFacebookError(FacebookError e, Object state) {
        	mMsg=e.getMessage();
        	mHandler.postDelayed(displayStatus,1500);
		}
    }

	// share weibo
	public void shareWeibo() {
		mWeibo = Weibo.getInstance();
		mWeibo.setupConsumerConfig(CONSUMER_KEY, CONSUMER_SECRET);
		mWeibo.setRedirectUrl("http://www.sina.com");
		mWeibo.authorize(SettingActivity.this, new AuthDialogListener());
	}

	class AuthDialogListener implements WeiboDialogListener {
		@Override
		public void onComplete(Bundle values) {
			String token = values.getString("access_token");
			String expires_in = values.getString("expires_in");

			AccessToken accessToken = new AccessToken(token, CONSUMER_SECRET);
			accessToken.setExpiresIn(expires_in);
			Weibo.getInstance().setAccessToken(accessToken);
			shareToSinaWeibo();
		}

		@Override
		public void onError(com.weibo.net.DialogError e) {
			Toast.makeText(getApplicationContext(),
					"Auth error : " + e.getMessage(), Toast.LENGTH_LONG).show();
		}

		@Override
		public void onCancel() {
			Toast.makeText(getApplicationContext(), "Auth cancel",
					Toast.LENGTH_LONG).show();
		}

		@Override
		public void onWeiboException(WeiboException e) {
			Toast.makeText(getApplicationContext(),
					"Auth exception : " + e.getMessage(), Toast.LENGTH_LONG)
					.show();
		}
	}

	public void shareToSinaWeibo() {
		try {
			String date=DateUtils.formatDateTime(getApplicationContext(),
					new Date().getTime(), DateUtils.FORMAT_SHOW_TIME
							| DateUtils.FORMAT_SHOW_YEAR
							| DateUtils.FORMAT_SHOW_DATE
							| DateUtils.FORMAT_CAP_AMPM);
			share2weibo(getString(AmazonKindle.IsKindle()?R.string.invitation_amazon:R.string.invitation)+" "+date, null);
			Intent i = new Intent(SettingActivity.this, ShareActivity.class);
			startActivity(i);
		} catch (WeiboException e) {
		}
	}

	private void share2weibo(String content, String picPath)
			throws WeiboException {
		Weibo weibo = Weibo.getInstance();
		weibo.share2weibo(this, weibo.getAccessToken().getToken(), weibo
				.getAccessToken().getSecret(), content, picPath);
	}

	Runnable showTooltip = new Runnable() {
		@Override
		public void run() {
			Intent it = new Intent(SettingActivity.this, Tooltip.class);
			it.putExtra("Content", getString(R.string.help_photo_is_needed));
			startActivity(it);
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
//		MobclickAgent.onResume(this);
	}
	
	@Override
	protected void onPause() {
		mHandler.removeCallbacks(showTooltip);
//		MobclickAgent.onPause(this);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		if (photoChanged && photoPath != null) {
			mPref.write("myPhotoPath", photoPath);
			mPref.write("myPhotoUploaded", false);
			Intent it = new Intent(Global.Action_InternalCMD);
			it.putExtra("Command", Global.CMD_UPLOAD_PROFILE_PHOTO);
			sendBroadcast(it);
			photoChanged = false;
		}

		if (moodChanged) {
			String last = mPref.read("moodcontent");
			moodContent = moodView.getText().toString().trim();
			Log.d("settings moodChanged " + last + "|" + moodContent);
			if (!last.equals(moodContent)) {
				mPref.write("moodcontent", moodContent);
				if (!moodContent.contains("!!!!"))
				{
					mPref.write("moodcontentuploaded", false);
					Intent it = new Intent(Global.Action_InternalCMD);
					it.putExtra("Command", Global.CMD_UPLOAD_PROFILE_MOOD);
					sendBroadcast(it);
				}
			}
			moodChanged = false;
		}

		if (emailChanged) {
			String last = mPref.read("email", "");
			String email = emailView.getText().toString().trim();
			Log.d("settings emailChanged " + last + "|" + email);
			if (!email.equals(last)) {
				if (email != null && email.length() > 6
						&& check_email_syntax(email)) {
					mPref.write("email", email);
					mPref.write("emailuploaded", false);
					Intent it = new Intent(Global.Action_InternalCMD);
					it.putExtra("Command", Global.CMD_UPLOAD_PROFILE_EMAIL);
					sendBroadcast(it);
				} else {
					Toast.makeText(this, R.string.email_invalid, Toast.LENGTH_LONG).show();
				}
			}
			emailChanged = false;
		}

		if (nicknameChanged) {
			String last = mPref.read("myNickname", "");
			String nickname = nicknameView.getText().toString().trim();
			boolean chinese=nickname.toLowerCase().equals(nickname.toUpperCase());
			Log.d("settings nicknameChanged " + last + "|" + nickname);
			if (!last.equals(nickname)) {
				if (checkNickname()) {
					mPref.write("myNickname", nickname);
					mPref.write("nicknameUpdated", false);
					Intent it = new Intent(Global.Action_InternalCMD);
					it.putExtra("Command", Global.CMD_UPDATE_MY_NICKNAME);
					sendBroadcast(it);
				}
			}
			nicknameChanged = false;
		}
		mPref.writeLong("last_show_time",new Date().getTime());

		super.onDestroy();
	}

	private void onPickPictureOption() {
		final CharSequence[] items = {
				getResources().getString(R.string.photo_gallery),
				getResources().getString(R.string.takepicture)};
		final CharSequence[] items_noCamera = {
				getResources().getString(R.string.photo_gallery)};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (!AmazonKindle.canHandleCameraIntent(this)){
			builder.setItems(items_noCamera, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
					if (item == 0) onPickPicture();
					dialog.dismiss();
				}
			});
		}
		else{
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
		}
		builder.setTitle(this.getResources().getString(
				R.string.choose_photo_source));
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
//		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		Intent intent = new Intent(Intent.ACTION_PICK);  //yang*** profpic fix
		intent.setType("image/*");
		if (AmazonKindle.IsKindle())
		{
			String title = getResources().getString( R.string.choose_photo_source);
			startActivityForResult(Intent.createChooser(intent, title), 16);
			return;
		}
		if (setTalkBackground == 1) {
			String title = getResources().getString( R.string.choose_photo_source);
			startActivityForResult(Intent.createChooser(intent, title), 1);
		} else {
			intent.putExtra("crop", "true");
			intent.putExtra("aspectX", 1);
			intent.putExtra("aspectY", 1);
//			intent.putExtra("outputX", PHOTO_SIZE+PHOTO_SIZE);
//			intent.putExtra("outputY", PHOTO_SIZE+PHOTO_SIZE);
			intent.putExtra("outputX", PHOTO_SIZE);  //yang*** profpic fix
			intent.putExtra("outputY", PHOTO_SIZE);
			intent.putExtra("return-data", true);
			//li*** fill picture
			intent.putExtra("scale", true);
			intent.putExtra("scaleUpIfNeeded", true);
			startActivityForResult(intent, 1);
		}
	}

	public String getPath(Uri uri) {
		Log.d("getPath=" + uri.getPath() + "\n  string=" + uri.toString());
		if (uri.toString().startsWith("content:")) {
			try {
				String[] projection = { MediaStore.Images.Media.DATA };
				Cursor cursor = managedQuery(uri, projection, null, null, null);
				int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				if (cursor != null) {
					cursor.moveToFirst();
					String path = cursor.getString(column_index);
					Log.d("getPath cursor " + column_index + " path=" + path);
					return path;
				}
			} catch (Exception e) {
			}
		} else if (uri.toString().startsWith("file:")) {
			String uriStr = uri.toString();
			Log.d("file://  uri " + uri.toString());
			if (uriStr.indexOf("sdcard") == -1) {
				Log.d("判断Url不包含sdcard字段  " + uriStr.substring(uriStr.indexOf("/storage")));
				return uriStr.substring(uriStr.indexOf("/storage"));
			} else {
				Log.d("判断Url包含sdcard字段  " + uriStr.substring(uriStr.indexOf("sdcard")));
				return uriStr.substring(uriStr.indexOf("sdcard"));
			}
		}
		return "";
	}

	private void onTakePicture() {
		if (!AmazonKindle.canHandleCameraIntent(this)){
			Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
			return;
		}
		try {
			if (AmazonKindle.IsKindle())
			{
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				photoPath = Global.SdcardPath_sent + "tmp.jpg";
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(photoPath)));
				startActivityForResult(intent, 8);
			}else{
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				photoPath = Global.SdcardPath_sent + "tmp.jpg";
				intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(photoPath)));
				startActivityForResult(intent, 20);
			}
		} catch (Exception e) {
			Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d("tmlpic requestCode=" + requestCode + " resultCode=" + resultCode + " data=" + data);
		if (requestCode == 20 || requestCode==1 || requestCode==3 || requestCode==7 || requestCode==8 || requestCode==16)
		{
			Message msg = new Message();
//			if (resultCode == RESULT_OK && data != null) {
			if (resultCode == RESULT_OK) {  //li*** profpic fix
				if (requestCode == 20) {
					boolean HDSize=false;
			    	try{
						BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
						bitmapOptions.inJustDecodeBounds=true;
						bitmapOptions.inPurgeable=true;
						BitmapFactory.decodeFile(photoPath, bitmapOptions);
						if (bitmapOptions.outHeight>2000)
							HDSize=true;
			    	}catch(Exception e){
			    	}catch(OutOfMemoryError e){
			    	}
			    	
					if (HDSize)
					{
						Bitmap bmp=ImageUtil.loadBitmapSafe(2, photoPath);
						try {
							uriOrig = Uri.parse(MediaStore.Images.Media.insertImage(
									getContentResolver(), bmp, null, null));
						} catch (Exception e) {}
					}else{
						try {
							uriOrig = Uri.parse(MediaStore.Images.Media.insertImage(
									getContentResolver(), photoPath, null, null));
						} catch (Exception e) {}
						catch(OutOfMemoryError e){}
					}
					
					try{
						startActivityForResult(getCropImageIntent(uriOrig), 3);
					}catch(Exception e) {
						android.util.Log.d("SettingActivity", e.getMessage());
					}
				
				} else if (requestCode == 7) {
					
					try{
//						Drawable photo = ImageUtil.getBitmapAsRoundCorner(photoPath, 3, 10);// alec
						Bitmap  photo = ImageUtil.getCircleBitmapPath(photoPath, 3, 10);  //xwf*** circle pic
						if (photo != null) {
							TextView hint = (TextView) findViewById(R.id.my_photo_hint);
							hint.setVisibility(View.GONE);
//							photoView.setImageDrawable(photo);
							photoView.setImageBitmap(photo);  //xwf*** circle pic
							photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
							photoChanged = true;
						}
					}catch(Exception e){}

//				} else if (requestCode == 1 || requestCode == 3) {
				} else if ((requestCode == 1 || requestCode == 3) && data != null) {  //li*** profpic fix
					String SrcImagePath = "";
					try {
						Uri uri = null;
						if (requestCode == 1) {
							if (setTalkBackground == 1) {
								if (data != null && data.getData() != null)
								{
									mPref.write("BackgroundImage", getPath(data.getData()));
									((ToggleButton)findViewById(R.id.usebkimg)).setChecked(true);
								}
								setTalkBackground = 0;
								return;
							}
						}

						Bitmap bitmap = data.getParcelableExtra("data");
						String uriString = MediaStore.Images.Media.insertImage(
								getContentResolver(), bitmap, null, null);
						uri = Uri.parse(uriString);
						SrcImagePath = getPath(uri);
						Log.d("tmlpic SrcImagePath=" + SrcImagePath);

						int uid = Integer.valueOf(mPref.read("myID", "0"), 16);
						String outFilename = Global.SdcardPath_sent + "myself_photo_" + uid + ".jpg";
						ResizeImage.ResizeXY(this, SrcImagePath, outFilename, PHOTO_SIZE, 100);

						String outFilename2 = Global.SdcardPath_inbox + "photo_" + uid + "b.jpg";
						ResizeImage.ResizeXY(this, SrcImagePath, outFilename2, PHOTO_SIZE, 100);

						photoPath = outFilename;

						if (uriOrig != null)
							getContentResolver().delete(uriOrig, null, null);
						getContentResolver().delete(uri, null, null);

						if (requestCode == 3)//taken from camera
						{
							Intent it = new Intent(SettingActivity.this, PictureRotationActivity.class);
							startActivityForResult(it, 7);
						} else {
//							Drawable photo = ImageUtil.getBitmapAsRoundCorner(outFilename, 3, 10);// alec
							Bitmap photo = ImageUtil.getCircleBitmapPath(photoPath, 3, 10);  //xwf*** circle pic
							if (photo != null) {
								TextView hint = (TextView) findViewById(R.id.my_photo_hint);
								hint.setVisibility(View.GONE);
//								photoView.setImageDrawable(photo);
								photoView.setImageBitmap(photo);  //xwf*** circle pic
								photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
								photoChanged = true;
							}
						}

					} catch (Exception e) {
						msg.what = 40;//DATA_ERROR;
						handler.sendMessage(msg);
						Log.e("onActR !@#$ " + e.getMessage());
					}
				}else if (requestCode == 8) {
					try {
						int uid = Integer.valueOf(mPref.read("myID", "0"), 16);
						String outFilename = Global.SdcardPath_sent + "myself_photo_" + uid + ".jpg";
						ResizeImage.ResizeXY(this, photoPath, outFilename, PHOTO_SIZE, 100);
						
						String outFilename2 = Global.SdcardPath_inbox + "photo_" + uid + "b.jpg";
						ResizeImage.ResizeXY(this, photoPath, outFilename2, PHOTO_SIZE, 100);
						
						photoPath = outFilename;
						
//						Drawable photo = ImageUtil.getBitmapAsRoundCorner(photoPath, 3, 10);// alec
						Bitmap photo = ImageUtil.getCircleBitmapPath(photoPath, 3, 10);  //xwf*** circle pic
						if (photo != null) {
							TextView hint = (TextView) findViewById(R.id.my_photo_hint);
							hint.setVisibility(View.GONE);
//							photoView.setImageDrawable(photo);
							photoView.setImageBitmap(photo);  //xwf*** circle pic
							photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
							photoChanged = true;
						}
	
					} catch (Exception e) {
					}
				}
//				else if (requestCode == 16) {
				else if (requestCode == 16 && data != null) {  //li*** profpic fix				
					try {
						Bitmap bitmap=null;
						try {
							Uri selectedImageUri = data.getData(); // URI of the photo
							bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri); 
						}catch (Exception e) {
						}
						
						if (bitmap!=null)
						{
							if (setTalkBackground == 1) {
								if (data != null && data.getData() != null)
								{
									String outFilename = Global.SdcardPath_sent + "chatbg" + ".jpg";
									ResizeImage.SaveAs(bitmap,outFilename,100);
									mPref.write("BackgroundImage", getPath(data.getData()));
									((ToggleButton)findViewById(R.id.usebkimg)).setChecked(true);
								}
								setTalkBackground = 0;
								return;
							}
							int uid = Integer.valueOf(mPref.read("myID", "0"), 16);
							String outFilename = Global.SdcardPath_sent + "myself_photo_" + uid + ".jpg";
							ResizeImage.ResizeBitmapXY(this, bitmap, outFilename, PHOTO_SIZE, 100);
							
							String outFilename2 = Global.SdcardPath_inbox + "photo_" + uid + "b.jpg";
							ResizeImage.ResizeBitmapXY(this, bitmap, outFilename2, PHOTO_SIZE, 100);
							
							photoPath = outFilename;
							
//							Drawable photo = ImageUtil.getBitmapAsRoundCorner(photoPath, 3, 10);// alec
							Bitmap photo = ImageUtil.getCircleBitmapPath(photoPath, 3, 10);  //xwf*** circle pic
							if (photo != null) {
								TextView hint = (TextView) findViewById(R.id.my_photo_hint);
								hint.setVisibility(View.GONE);
//								photoView.setImageDrawable(photo);
								photoView.setImageBitmap(photo);  //xwf*** circle pic
								photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
								photoChanged = true;
							}
						}
					} catch (Exception e) {
					}
				}
			} else {
				msg.what = 40;//DATA_ERROR;
				handler.sendMessage(msg);
			}
		}
		else
			facebook.authorizeCallback(requestCode, resultCode, data);
	}
	
	public static Intent getCropImageIntent(Uri photoUri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(photoUri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
//		intent.putExtra("outputX", PHOTO_SIZE + PHOTO_SIZE);
//		intent.putExtra("outputY", PHOTO_SIZE + PHOTO_SIZE);
		intent.putExtra("outputX", PHOTO_SIZE);// FIXME: 2016/4/30 jack
		intent.putExtra("outputY", PHOTO_SIZE);
		intent.putExtra("return-data", true);
		return intent;
	}
	
	public boolean check_email_syntax(String email) {
		if (email == null || email.length() > 64 || email.length() < 6)
			return false;
		return email.matches("^[0-9a-zA-Z_\\-\\.]+@[0-9a-zA-Z_\\-\\.]+$");
	}

	private void checkEmail() {
		String email = emailView.getText().toString().trim();
		if (emailChanged && email != null && email.length() > 0) {
			if (!check_email_syntax(email)) {
				Intent int2 = new Intent(getApplicationContext(),
						CommonDialog.class);
				int2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
						| Intent.FLAG_ACTIVITY_SINGLE_TOP);
				int2.putExtra("msgContent", getString(R.string.email_invalid));
				int2.putExtra("numItems", 1);
				int2.putExtra("ItemCaption0", getString(R.string.done));
				int2.putExtra("ItemResult0", -1);
				startActivity(int2);
			}
		}
	}

	private boolean checkNickname() {
		String nickname = nicknameView.getText().toString().trim();
		boolean chinese = nickname.toLowerCase().equals(nickname.toUpperCase());
		
		if (nicknameChanged && nickname != null && nickname.length() < (chinese ? 2 : 6)) {
			Intent int2 = new Intent(getApplicationContext(),
					CommonDialog.class);
			int2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			int2.putExtra("msgContent", getString(R.string.nickname_invalid));
			int2.putExtra("numItems", 1);
			int2.putExtra("ItemCaption0", getString(R.string.done));
			int2.putExtra("ItemResult0", -1);
			startActivity(int2);
			nicknameChanged = false;
			return false;
		}
		return true;
	}

	// QQ
	private static final String CALLBACK = "tencentauth://auth.qq.com";

	public String mAppid = "100279586";
	private final String scope = "get_user_info,get_user_profile,add_share,add_topic,list_album,upload_pic,add_album";
	private AuthReceiver receiver;

	public String mAccessToken, mOpenId;

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 10://QQ_ROOM:
				shareQQroom();
				break;
			case 20://SUCCESS_SHARE:
				Toast.makeText(SettingActivity.this, R.string.shared_successfully,
						Toast.LENGTH_SHORT).show();
				break;
			case 30://FAIL_SHARE:
				Toast.makeText(SettingActivity.this, R.string.shared_failed,
						Toast.LENGTH_SHORT).show();
				break;
			case 40:
				Toast.makeText(SettingActivity.this, R.string.img_error,
						Toast.LENGTH_LONG).show();
				break;
			default:
				break;
			}
		}
	};

	public static final int PROGRESS = 0;

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		switch (id) {
		case PROGRESS:
			dialog = new ProgressDialog(this);
			((ProgressDialog) dialog).setMessage(getString(R.string.progress_wait));
			break;
		}
		return dialog;
	}

	private void shareQQroom() {
		Bundle bundle = null;
		bundle = new Bundle();
		bundle.putString("title", getString(R.string.invitation_title));
		bundle.putString("url", "http://play.google.com/store/apps/details?id=com.pingshow.amper"
						+ "#" + System.currentTimeMillis());
		bundle.putString("comment", getString(AmazonKindle.IsKindle()?R.string.invitation_amazon:R.string.invitation));
		bundle.putString("summary", getString(AmazonKindle.IsKindle()?R.string.invitation_amazon:R.string.invitation));
		bundle.putString("images", "http://www.pingshow.net/img/aire.png");
		bundle.putString("type", "5");
		bundle.putString("playurl", "http://player.youku.com/player.php/sid/XMzI3ODkzMjE2/v.swf");
		TencentOpenAPI.addShare(mAccessToken, mAppid, mOpenId, bundle,
				new Callback() {
					@Override
					public void onSuccess(final Object obj) {
						Message msg = new Message();
						msg.what = 20;//SUCCESS_SHARE;
						handler.sendMessage(msg);
					}

					@Override
					public void onFail(final int ret, final String msg) {
						Message msg1 = new Message();
						msg1.what = 30;//FAIL_SHARE;
						handler.sendMessage(msg1);
					}
				});
	}

	// QQ
	private void auth(String clientId, String target) {
		Intent intent = new Intent(SettingActivity.this,
				com.tencent.tauth.TAuthView.class);

		intent.putExtra(TAuthView.CLIENT_ID, clientId);
		intent.putExtra(TAuthView.SCOPE, scope);
		intent.putExtra(TAuthView.TARGET, target);
		intent.putExtra(TAuthView.CALLBACK, CALLBACK);
		startActivity(intent);

	}

	private void registerIntentReceivers() {
		receiver = new AuthReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(TAuthView.AUTH_BROADCAST);
		registerReceiver(receiver, filter);
	}

	public class AuthReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle exts = intent.getExtras();
			String access_token = exts.getString(TAuthView.ACCESS_TOKEN);
			String error_ret = exts.getString(TAuthView.ERROR_RET);

			if (access_token != null) {
				mAccessToken = access_token;
				shareQQroom();
				showDialog(PROGRESS);
				TencentOpenAPI.openid(access_token, new Callback() {
					@Override
					public void onSuccess(final Object obj) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								dismissDialog(PROGRESS);
								// setOpenIdText(((OpenId)obj).getOpenId());
							}
						});
					}

					@Override
					public void onFail(int ret, final String msg) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								dismissDialog(PROGRESS);
								TDebug.msg(msg, getApplicationContext());
							}
						});
					}
				});
			}
			if (error_ret != null) {
			}
		}
	}
	//tml*** secret test
	Runnable showSecret = new Runnable() {
    	public void run() {
    		Intent it = new Intent(SettingActivity.this, Tooltip.class);
    		String secret = moodView.getText().toString();
    		String content = "";
    		if (secret.toLowerCase().equals("debug(80aire")) {
    			String myidx = Integer.toString(Integer.parseInt(mPref.read("myID", "0"), 16));
    			String mymac = mPref.read("myPhoneNumber", "??????????????");
    			String mynotes = versnotes;
    			String iso = mPref.read("iso", "cn");
    			String roamid = mPref.read("myRoamId", "");
    			String lastSip = mPref.read("lastRegisSip", "n/a");
    			String lastCall = mPref.read("lastCallSip", "n/a");
    			if (AireJupiter.getInstance() != null) AireJupiter.getInstance().getIsoDomain();
				mPref.write("TESTING", true);
				if (Log.enDEBUG) {
	    			content = myidx + ",  " + mymac + ",  iso:" + iso + "/" + roamid
	    					+ "\ntcp: " + MySocket.ServerDM_d
	    					+ "\ntcp: " + MySocket.ServerIP_d
	            			+ "\nphp: " + AireJupiter.myAcDomain_default
	            			+ "\nphp: " + AireJupiter.myPhpServer
	            			+ "\nlast-sip: " + lastSip
	            			+ "\n" + lastCall;
				} else {
    				content = " iso:" + iso + "/" + roamid
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
			} else if (secret.toLowerCase().equals("debugoff")) {
				mPref.write("TESTING", false);
				content = "debug disabled";
			} else if (secret.toLowerCase().equals("vc8443")) {
				boolean vc8081 = mPref.readBoolean("VC8443", false);
				mPref.write("VC8443", !vc8081);
				vc8081 = mPref.readBoolean("VC8443", false);
				content = "VC8443:" + Boolean.toString(vc8081);
			}else if (secret.toLowerCase().equals("pay")) {
				boolean pay = mPref.readBoolean("pay", false);
				mPref.write("pay", !pay);
				pay = mPref.readBoolean("pay", false);
				content = "pay:" + Boolean.toString(pay);
			}else if (secret.toLowerCase().equals("msg280")) {
//				boolean msg280 = mPref.readBoolean("SLOWMSG", false);
//				mPref.write("SLOWMSG", !msg280);
//				msg280 = mPref.readBoolean("SLOWMSG", false);
//				content = "MSG280:" + Boolean.toString(msg280);
			} else if (secret.toLowerCase().equals("newtcpchina")) {
				boolean newtcp = mPref.readBoolean("NEWTCPCHINA", false);
				mPref.write("NEWTCPCHINA", !newtcp);
				mPref.write("NEWTCPUSA", false);
				newtcp = mPref.readBoolean("NEWTCPCHINA", false);
				content = "NEWTCP China:" + Boolean.toString(newtcp);
				AireJupiter.getInstance().tcpSocket.disconnectAndReconnect("new server", false);
			} else if (secret.toLowerCase().equals("newtcpusa")) {
				boolean newtcp = mPref.readBoolean("NEWTCPUSA", false);
				mPref.write("NEWTCPUSA", !newtcp);
				mPref.write("NEWTCPCHINA", false);
				newtcp = mPref.readBoolean("NEWTCPUSA", false);
				content = "NEWTCP USA:" + Boolean.toString(newtcp);
				AireJupiter.getInstance().tcpSocket.disconnectAndReconnect("new server", false);
			} else if (secret.toLowerCase().endsWith("test1")) {
				secretValue = moodView.getText().toString().trim();
				secretValue = secretValue.substring(0, secretValue.indexOf("test1")).trim();
				content = "test1";
				mHandler.post(runTest1);
			} else if (secret.toLowerCase().endsWith("test2")) {
				secretValue = moodView.getText().toString().trim();
				secretValue = secretValue.substring(0, secretValue.indexOf("test2")).trim();
				content = "test2";
				mHandler.post(runTest2);
			} else if (secret.toLowerCase().endsWith("test3")) {
				secretValue = moodView.getText().toString().trim();
				secretValue = secretValue.substring(0, secretValue.indexOf("test3")).trim();
//				content = "test3";
				mHandler.post(runTest3);
				content = "";
			}
    		
    		if (Log.enDEBUG) {
        		mPref.write("secretDebug", true);
        		String contentPop = "";
        		contentPop = contentPop + "Debug1:" + mPref.readBoolean("TESTING", false) + "\n";
        		contentPop = contentPop + "VC8443:" + mPref.readBoolean("VC8443", false) + "\n";
//        		contentPop = contentPop + "MSG280:" + mPref.readBoolean("SLOWMSG", false) + "\n";
        		contentPop = contentPop + "NewTCP cn/us:" + mPref.readBoolean("NEWTCPCHINA", false) + "/" + mPref.readBoolean("NEWTCPUSA", false) + "\n";
        		contentPop = contentPop + "(iso:" + mPref.read("iso", "--") + "/" + mPref.read("myRoamId", "--") + ")\n";
        		mPref.write("secretDebugInfo", contentPop);
    		} else {
    			mPref.delect("secretDebug");
    			mPref.delect("secretDebugInfo");
    		}
    		
    		moodContent = mPref.read("moodcontent", "");
    		moodView.setText(moodContent);
    		moodChanged = false;
    		if (content.length() > 0) {
	            it.putExtra("Content", content);
	            startActivity(it);
    		}
    	}
	};
	VoiceRecord2_MR myVR;
	VoicePlayer2_MP myVP;
	VoiceMemoPlayer_NB vmp;
	String outputFile;
	private Runnable runTest1 = new Runnable() {
		@Override
		public void run() {
			
			String android_id = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
			String myPhoneNumber = mPref.read("myPhoneNumber", "----");
			String myPasswd = mPref.read("password", "1111");
			MyNet net = new MyNet(SettingActivity.this);
			String Return = "";
			try {
				Return = net.doPostHttps("getsipcredit_test.php", "id=" + URLEncoder.encode(myPhoneNumber,"UTF-8")
						+"&password=" + URLEncoder.encode(myPasswd, "UTF-8")
						+"&imei=" + android_id, null);
			} catch (UnsupportedEncodingException e) {
			}
			
//			MyUtil.greaterThanTimer_sysMillis(SettingActivity.this, "testingTimerG", 15000, true);
//			MyUtil.lessThanTimer_sysMillis(SettingActivity.this, "testingTimerL", 15000, true);
			
			//test VoicePlayer2_MP
//			outputFile = Environment.getExternalStorageDirectory()
//					.getAbsolutePath() + "/testmp" + ".mp3";
//			
//			myVP = new VoicePlayer2_MP(SettingActivity.this, outputFile);
//			myVP.start();
//			
//			mHandler.postDelayed(new Runnable() {
//
//				@Override
//				public void run() {
//					myVP.stop();
//					myVP = null;
//				}
//				
//			}, 10000);
			
			//test record, then play with both players
//			if (myVP != null) {
//				Log.e("vm myAudioPlayer stop");
//				myVP.stop();
//				myVP = null;
//			}
//			if (vmp != null) {
//				Log.e("vm VoiceMemoPlayer_NB stop");
//				vmp.stop();
//				vmp = null;
//			}
//			
//			if (myVR == null) {
//			    try {
//			    	int outformat = MediaRecorder.OutputFormat.RAW_AMR;  //0
//			    	if (secretValue.equals("1")) {
//			    		outformat = MediaRecorder.OutputFormat.AMR_NB;  //3
//			    	} else if (secretValue.equals("2")) {
//			    		outformat = MediaRecorder.OutputFormat.AMR_WB;  //4
//			    	} else if (secretValue.equals("3")) {
//			    		outformat = MediaRecorder.OutputFormat.MPEG_4;  //2
//			    	} else if (secretValue.equals("4")) {
//			    		outformat = MediaRecorder.OutputFormat.RAW_AMR;  //3 +DEFAULT
//			    	} else if (secretValue.equals("5")) {
//			    		outformat = MediaRecorder.OutputFormat.THREE_GPP;  //1
//			    	} else if (secretValue.equals("6")) {
//			    		outformat = MediaRecorder.OutputFormat.AAC_ADTS;  //6
//			    	} else {
//			    		outformat = MediaRecorder.OutputFormat.DEFAULT;  //0
//			    	}
//
//					outputFile = Environment.getExternalStorageDirectory()
//				            .getAbsolutePath() + "/myrecording" + ".amr";
//					File file = new File(outputFile);
//					if (file.exists()) {
//						file.delete();
//					}
//					
//					myVR = new VoiceRecord2_MR(SettingActivity.this, outputFile,
//							MediaRecorder.OutputFormat.RAW_AMR,
//							MediaRecorder.OutputFormat.DEFAULT, 8000);
//					myVR.start();
//				} catch (Exception e) {
//					Log.e("vm myAudioRecorder !@#$ " + e.getMessage());
//					Toast.makeText(SettingActivity.this, "myAudioRecorder !@#$", Toast.LENGTH_SHORT).show();
//					myVR = null;
//				}
//			} else {
//				Log.e("vm myAudioRecorder stop");
//				myVR.stop();
//				myVR = null;
//				
//				MyUtil.Sleep(500);
//				
//				try {
//					Log.e("vm myAudioPlayer");
//					myVP = new VoicePlayer2_MP(SettingActivity.this, outputFile);
//					myVP.start();
//					
//					new Thread(new Runnable() {
//						@Override
//						public void run() {
//							while (myVP.isPlaying()) {
//								MyUtil.Sleep(100);
//							}
//							
//							try {
//								Log.e("vm myAudioPlayer stop");
//								Log.e("vm VoiceMemoPlayer_NB");
//								vmp = new VoiceMemoPlayer_NB(SettingActivity.this);
//								vmp.setDataSource(outputFile);
//								vmp.prepare();
//								vmp.start();
//								
//								MyUtil.Sleep(5000);
//								
//								if (vmp != null) {
//									Log.e("vm VoiceMemoPlayer_NB stop");
//									vmp.stop();
//									vmp = null;
//								}
//								
//							} catch (Exception e) {
//								Log.e("vm VoiceMemoPlayer_NB !@#$ " + e.getMessage());
//								Toast.makeText(SettingActivity.this, "VoiceMemoPlayer_NB !@#$", Toast.LENGTH_SHORT).show();
//								vmp = null;
//							}
//						}
//					}).start();
//					
//				} catch (Exception e) {
//					Log.e("vm myAudioPlayer !@#$ " + e.getMessage());
//					Toast.makeText(SettingActivity.this, "myAudioPlayer !@#$", Toast.LENGTH_SHORT).show();
//					myVP = null;
//					vmp = null;
//				}
//				
//			}
		    
//			int numCodecs = MediaCodecList.getCodecCount();
//			String codec = "";
//			for (int i = 0; i < numCodecs; i++) {
//				MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
//				if (codecInfo.isEncoder()) {
//					codec = codec + "  " + codecInfo.getName() + "{";
//					String[] types = codecInfo.getSupportedTypes();
//					for (int j = 0; j < types.length; j++) {
//						codec = codec + types[j] + ",";
//					}
//					codec = codec + "}";
//				}
//			}
//			Log.e("ENC > " + codec);
//			for (int i = 0; i < numCodecs; i++) {
//				MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
//				if (!codecInfo.isEncoder()) {
//					codec = codec + "  " + codecInfo.getName() + "{";
//					String[] types = codecInfo.getSupportedTypes();
//					for (int j = 0; j < types.length; j++) {
//						codec = codec + types[j] + ",";
//					}
//					codec = codec + "}";
//				}
//			}
//			Log.e("DEC > " + codec);
		}
	};
	private Runnable runTest2 = new Runnable() {
		@Override
		public void run() {
			MyNet net = new MyNet(SettingActivity.this);
			int idxi = Integer.parseInt(secretValue);
			int myidx = Integer.parseInt(mPref.read("myID", "0"), 16);
			String pw = mPref.read("password", "1111");
			String Return = net.doPost("getuserinfo.php","idx=" + idxi + "&id=" + myidx + "&password=" + pw, null);
			Log.e("test getuserinfo=" + Return);

			String idxh = Integer.toHexString(Integer.parseInt(secretValue));
			String Return2 = net.doPost("getusernickname.php","idx=" + idxh, null);
			Log.e("test getusernickname=" + Return2);
		}
	};
	private Runnable runTest3 = new Runnable() {
		@Override
		public void run() {
			Intent it = new Intent();
			it.setClass(SettingActivity.this, SecretActivity.class);
			it.putExtra("secret", secretValue);
			startActivity(it);
		}
	};
	//tml*** beta ui
	private void showVersionNotes (boolean simple) {
		TextView showVersion = (TextView) findViewById(R.id.version);
		String versionNotes = "";
		String versionName = "1.0.0";
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
		}
		if (simple) {
			versionNotes = "Version: " + versionName;
		} else {
			versionNotes = "Version: " + versionName + versnotes;
		}
		
		showVersion.setText(versionNotes);
	}
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
	
	private ProgressDialog progressDialog;
	Runnable popupProgressDialog = new Runnable() {
		@Override
		public void run() {
			try {
				if (progressDialog == null)
					progressDialog = ProgressDialog.show(SettingActivity.this, "", getString(R.string.in_progress), true, true);
			} catch (Exception e) {}
		}
	};
	
	Runnable dismissProgress = new Runnable() {
		@Override
		public void run() {
			try {
				if (progressDialog != null && progressDialog.isShowing())
					progressDialog.dismiss();
				if (progressDialog != null) progressDialog.cancel();
					progressDialog = null;
				((ImageView) findViewById(R.id.search)).setEnabled(true);
			} catch (Exception e) {
				progressDialog = null;
			}
		}
	};

	//jack 删除数据
//	class ClearUserDataObserver extends IPackageDataObserver.Stub {
//		public void onRemoveCompleted(final String packageName, final boolean succeeded) {
//            final Message msg = mHandler.obtainMessage(CLEAR_USER_DATA);
//            msg.arg1 = succeeded?OP_SUCCESSFUL:OP_FAILED;
//            mHandler.sendMessage(msg);
//		}
//	}
}
