package com.pingshow.amper.register;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.amper.CommonDialog;
import com.pingshow.amper.Global;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.R;
import com.pingshow.network.MyNet;
import com.pingshow.util.MyUtil;
import com.pingshow.util.ResizeImage;
import com.weibo.net.AccessToken;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboDialogListener;
import com.weibo.net.WeiboException;
import com.weibo.net.WeiboParameters;

public class WeiboLoginDialog extends Activity {

	private static final String CONSUMER_KEY = "2748488668";
	private static final String CONSUMER_SECRET = "bc38614d7c45d659426a1d41124d5018";

	private TextView mWeiboName;
	private String username;
	private String email;
	private String password;
	private String DeviceID;
	private Weibo mWeibo;
	private MyPreference mPref;

	public String userId;
	public Map<String, Object> map = new HashMap<String, Object>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fb_login_page);
		mWeiboName = (TextView) findViewById(R.id.username);
		mPref = new MyPreference(this);
		
		((TextView)findViewById(R.id.title)).setText(R.string.weibo_sign_in);
		((TextView)findViewById(R.id.id_desc)).setText(R.string.weibo_login_desc);
		((ImageView)findViewById(R.id.logo)).setImageResource(R.drawable.sina);
		
		((Button) findViewById(R.id.back)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		new Thread(copyEula).start();
		
		mWeibo = Weibo.getInstance();
		mWeibo.setupConsumerConfig(CONSUMER_KEY, CONSUMER_SECRET);
		mWeibo.setRedirectUrl("http://www.sina.com");
		mWeibo.authorize(WeiboLoginDialog.this, new AuthDialogListener());
	}

	public void requestUserData() {
		try {
			getUserId(mWeibo);
			getUserData(mWeibo);
		} catch (Exception e) {
		}
	}

	private void getUserId(Weibo weibo) throws Exception {
		// https://api.weibo.com/2/account/get_uid.json
		String url = Weibo.SERVER + "account/get_uid.json";
		WeiboParameters bundle = new WeiboParameters();
		bundle.add("source", Weibo.getAppKey());
		// weibo.request(context, url, params, httpMethod, token)
		String rlt = weibo.request(this, url, bundle, "GET",
				mWeibo.getAccessToken());
		JSONObject jsonObject = new JSONObject(rlt);
		userId = jsonObject.getString("uid");
	}

	private void getUserData(Weibo weibo) throws Exception {
		String url = Weibo.SERVER + "users/show.json";
		WeiboParameters bundle = new WeiboParameters();
		bundle.add("source", Weibo.getAppKey());
		bundle.add("uid", userId);
		String rlt = weibo.request(this, url, bundle, "GET",
				mWeibo.getAccessToken());
		
		JSONObject jsonObject = new JSONObject(rlt);
		final String picURL = jsonObject.getString("avatar_large");
		final String nickname = jsonObject.getString("screen_name");
		final String gender = jsonObject.getString("gender");
		
        double wb64=Double.valueOf(userId);
        int high=(int)(wb64/4294967296.);
        long low=(long)(wb64-high*4294967296.);
        String hexId="";
        String hexId_low="";
        if (high>0)
        {
        	hexId=Integer.toHexString(high);
        	hexId_low=Long.toHexString(low);
        	while (hexId_low.length()<8)
        		hexId_low="0"+hexId_low;
        }
        else
        	hexId_low=Long.toHexString(low);
         
		email = userId+"@sina.com";
		username = "wb" + hexId + hexId_low;
		password = userId.substring(userId.length()-4, userId.length());
		// password = userId;
		Handler handler = new Handler();
		handler.post(new Runnable() {
			@Override
			public void run() {
				mWeiboName.setText(nickname);
				downloadPhoto(picURL, username);
			}
		});

		mPref.write("myNickname", nickname);
		mPref.write("myWeiboID", userId);
		mPref.write("myGender", gender);

		new Thread(new Runnable() {
			@Override
			public void run() {
				mHandler.post(popupProgressDialog);
				TelephonyManager tMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
				DeviceID = tMgr.getDeviceId();
				String Return = "";
				try {
					int count = 0;
					do {
						MyNet net = new MyNet(WeiboLoginDialog.this);
						Return = net.doPostHttps(
								"preregister_aire.php",
								"id=" + URLEncoder.encode(username, "UTF-8")
										+ "&password=" + password + "&email="
										+ URLEncoder.encode(email, "UTF-8")
										+ "&eula=1" + "&pcode=0000"
										+ "&nickname="
										+ URLEncoder.encode(nickname, "UTF-8")
										+ "&imei="
										+ URLEncoder.encode(DeviceID, "UTF-8")
										+ "&acct=0", null);
						Log.d("preregister_aireWb=" + email + " " + URLEncoder.encode(email, "UTF-8"));
						Log.d("preregister_aireWb Return="+Return);
						if (Return.length() != 0 && !Return.startsWith("Error"))// no network
							break;
						MyUtil.Sleep(1500);
						count++;
					} while (count < 3);
				} catch (Exception e) {
				}

				mHandler.post(dismissProgressDialog);

				if (Return.length() == 0)
					Return = "fail,nonetwork";

				Message msg = new Message();
				msg.obj = Return;
				mHandler.sendMessage(msg);
			}
		}).start();
	}


	private boolean downloadPhoto(String remote, String uid) {
		String tmp = Global.SdcardPath_sent + "tmp.jpg";
		String out = Global.SdcardPath_sent + uid + ".jpg";
		try {
			boolean success = false;
			int count = 0;
			do {
				MyNet net = new MyNet(WeiboLoginDialog.this);
				if (success = net.anyDownload(remote, tmp))
					break;
				count++;
				MyUtil.Sleep(500);
			} while (!success && count < 3);

			ResizeImage.ResizeXY(this, tmp, out, 240, 100);
			return true;
		} catch (Exception e) {
			Log.e("wb downloadPhoto !@#$ " + e.getMessage());
		}
		return false;
	}

	ProgressDialog progress;
	Runnable popupProgressDialog = new Runnable() {
		@Override
		public void run() {
			try {
				progress = ProgressDialog.show(WeiboLoginDialog.this, "",
						getString(R.string.in_progress), true, true);
			} catch (Exception e) {
			}
		}
	};

	Runnable dismissProgressDialog = new Runnable() {
		@Override
		public void run() {
			if (progress.isShowing()) {
				progress.dismiss();
			}
		}
	};

	
	Runnable doActualRegister = new Runnable() {
		@Override
		public void run() {
			mHandler.post(popupProgressDialog);
			MyNet net = new MyNet(WeiboLoginDialog.this);
			String Return = "";
			int count = 0;
			do {
				try {
					Return = net.doPostHttps("register.php",
							"id=" + URLEncoder.encode(username, "UTF-8"), null);
				} catch (Exception e) {
				}
				Log.d("register Return=" + Return);
				if (Return.length() != 0 && !Return.startsWith("Error"))// no
																		// network
					break;
				MyUtil.Sleep(1500);
			} while (++count < 3);

			mHandler.post(dismissProgressDialog);

			if (Return.length() == 0)
				Return = "fail,nonetwork";

			Message msg = new Message();
			msg.obj = Return;
			mHandler.sendMessage(msg);
		}
	};

	// sina
	class AuthDialogListener implements WeiboDialogListener {
		@Override
		public void onComplete(Bundle values) {
			String token = values.getString("access_token");
			String expires_in = values.getString("expires_in");

			AccessToken accessToken = new AccessToken(token, CONSUMER_SECRET);
			accessToken.setExpiresIn(expires_in);
			Weibo.getInstance().setAccessToken(accessToken);

			Intent intent=new Intent(WeiboLoginDialog.this, EulaDialog.class);
    		startActivityForResult(intent, 64);
		}

		@Override
		public void onError(com.weibo.net.DialogError e) {
			Toast.makeText(getApplicationContext(),
					"Auth error : " + e.getMessage(), Toast.LENGTH_LONG).show();
			((Button) findViewById(R.id.back)).setEnabled(true);
		}

		@Override
		public void onCancel() {
			Toast.makeText(getApplicationContext(), "Auth cancel",
					Toast.LENGTH_LONG).show();
			((Button) findViewById(R.id.back)).setEnabled(true);
		}

		@Override
		public void onWeiboException(WeiboException e) {
			Toast.makeText(getApplicationContext(),
					"Auth exception : " + e.getMessage(), Toast.LENGTH_LONG)
					.show();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 64) {
			if (data.getIntExtra("agreement", 0) == 0) {
				Intent it = new Intent(this, CommonDialog.class);
				it.putExtra("msgContent", getString(R.string.eula_disagree));
				it.putExtra("numItems", 1);
				it.putExtra("ItemCaption0", getString(R.string.done));
				it.putExtra("ItemResult0", RESULT_OK);
				startActivity(it);

				((Button) findViewById(R.id.back)).setEnabled(true);
				((Button) findViewById(R.id.back))
						.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View arg0) {
								finish();
							}
						});
				return;
			}

			if (resultCode == RESULT_OK)
				requestUserData();
		}
	}

	Runnable copyEula = new Runnable() {
		@Override
		public void run() {
			String destURL = "/data/data/com.pingshow.amper/files/eula.html";
			File FileToCopy = new File(destURL);
			if (!FileToCopy.exists()) {
				try {
					copyFromPackage(R.raw.eula, FileToCopy.getName());
				} catch (IOException e) {
				}
			}
		}
	};

	private void copyFromPackage(int ressourceId, String target)
			throws IOException {
		FileOutputStream lOutputStream = openFileOutput(target, 0);
		InputStream lInputStream = getResources().openRawResource(ressourceId);
		int readByte;
		byte[] buff = new byte[8048];
		while ((readByte = lInputStream.read(buff)) != -1) {
			lOutputStream.write(buff, 0, readByte);
		}
		lOutputStream.flush();
		lOutputStream.close();
		lInputStream.close();
	}

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			String Return = msg.obj.toString().toLowerCase();
			if (Return.startsWith("ok,") || Return.contains("ok,registration")
					|| Return.startsWith("fail,accountexists")) {
				mPref.write("Registered", true);
				mPref.write("myPhoneNumber", username);
				mPref.write("password", password);
				mPref.write("myIMEI", (DeviceID == null) ? "" : DeviceID);
				mPref.write("email", email);
				mPref.write("SubscribeId", "");
				mPref.write("firstRegister", true);
				mPref.write("firstEnter", true);

				if (Return.startsWith("ok,by_email")) {
					(new Thread(doActualRegister)).start();
					return;
				} else if (Return.startsWith("fail,accountexists")) {
					mPref.write("accountUpdated", true);
					mPref.write("firstRegister", false);
				}
				Toast.makeText(WeiboLoginDialog.this, R.string.welcome,
						Toast.LENGTH_LONG).show();
				mPref.write("LoginByWeibo", true);
				setResult(RESULT_OK);
				finish();
				return;
			}

			if (Return.startsWith("fail,")) {
				Return = Return.substring(5);
				if (progress != null && progress.isShowing())
					progress.dismiss();
			}
			String errMsg = "";
			if (Return.startsWith("email")
					|| Return.startsWith("invalid_email")) {
				errMsg = getString(R.string.email_invalid);
			} else if (Return.startsWith("mismatch")) {
				errMsg = getString(R.string.passwords_dont_match);
			} else if (Return.startsWith("accountexists")) {
				errMsg = getString(R.string.account_exists);
			} else if (Return.startsWith("nonmember")
					|| Return.startsWith("membernotfound")) {
				errMsg = getString(R.string.nonmember);
			} else if (Return.startsWith("pwderror")) {
				errMsg = getString(R.string.password_error);
			} else if (Return.startsWith("username")) {
				errMsg = getString(R.string.username_invalid);
			} else if (Return.startsWith("phonenumber")) {
				errMsg = getString(R.string.phonenumber_invalid);
			} else if (Return.startsWith("wrong_username")) {
				errMsg = getString(R.string.no_sim_hint);
			} else if (Return.startsWith("invalid_username")) { // invalid
																// username
				errMsg = getString(R.string.username_invalid);
			} else if (Return.startsWith("nonetwork")
					|| Return.startsWith("error")
					|| Return.startsWith("invalid")) {
				errMsg = getString(R.string.nonetwork);
			} else if (Return.equals("registered")) { // already registered,
														// just exist
				errMsg = getString(R.string.account_exists);
			} else if (Return.equals("pingshow")) { // pngshow registration
													// failed
				errMsg = "Registration failed!, network issue";
			} else if (Return.equals("nosipserver")) {// Internal error, no sip
														// server
				errMsg = "Registration failed!, internal error";
			} else if (Return.equals("findpwd_input_err")) {
				errMsg = getString(R.string.findpwd_input_err);
			} else { // list of failed sip servers
				errMsg = Return;
			}

			Intent it = new Intent(WeiboLoginDialog.this, CommonDialog.class);
			it.putExtra("msgContent", errMsg);
			it.putExtra("numItems", 1);
			it.putExtra("ItemCaption0", getString(R.string.done));
			it.putExtra("ItemResult0", RESULT_OK);
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
//		MobclickAgent.onPause(this);
		super.onPause();
	}
}
