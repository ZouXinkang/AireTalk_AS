package com.pingshow.amper.register;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Random;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.amper.CommonDialog;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.R;
import com.pingshow.amper.SplashScreen;
import com.pingshow.amper.map.LocationUpdate;
import com.pingshow.network.MyNet;
import com.pingshow.util.MyProfile;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;

public class RegisterActivity extends Activity {
	
	private MyPreference mPref;
	private EditText username_view;
	private EditText password1_view;
	private EditText password2_view;
	private EditText email_view;
	
	private String globalNumber = "";
	private String SubscribeId="";
	private String DeviceID="";//IMEI for GSM or MEID for WCDMA
	private String iso;
	
//	private boolean devIsPhone=false;
	private boolean withSIM=false;
	private boolean phoneNumberReadable=false;
	private boolean usePhoneNumberRegis = false;  //tml*** regisown
	private int pcode;
	private boolean SmsConfirmWaiting = false;
	private boolean popupSmsPending = false;
	private boolean byPassPcode=true;
	private int pcodePolicyChecked = -1;
	
	private ProgressDialog progress;
	
//	private boolean devIsPhone=false;
	String username="";
	String password="";
	String email="";
	int checked=0;
	
	static private LocationUpdate location=null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register_page);
		this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

		mPref=new MyPreference(this);
		MyProfile.init(RegisterActivity.this);
		
		username_view = (EditText)findViewById(R.id.username);
	    email_view = (EditText) findViewById(R.id.email);
	    password1_view =(EditText)findViewById(R.id.password1);
	    password2_view = (EditText) findViewById(R.id.password2);
	    
	    TelephonyManager tMgr=(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
	    DeviceID=tMgr.getDeviceId();
	    SubscribeId=tMgr.getSubscriberId();
	    iso=tMgr.getSimCountryIso().toLowerCase();
		Log.e("regis0A ::"
				+ " " + DeviceID
				+ "|" + SubscribeId
				+ "|" + iso);
		if (iso.equals("") || iso == null) {
			iso = mPref.read("iso", "");
		}
	    
	    TextView id_desc=(TextView)findViewById(R.id.id_desc);
		String _SimNumber = "";
		boolean readok=false;
	    if (DeviceID!=null && DeviceID.length()>8 && iso!=null && iso.length()>1)
	    {
	    	if (SubscribeId!=null && SubscribeId.length()>10 && !SubscribeId.contains("0000000"))
	    	{
//		    	devIsPhone=true;
	    		withSIM=true;
		    	
				String SimNumber=tMgr.getLine1Number();
				_SimNumber = SimNumber;
				//xwf  不管有没有读取到手机号码，只要判断是手机卡，就需要输入手机号登陆。
				phoneNumberReadable=true;
				
//			if(false)
				if (SimNumber!=null && SimNumber.length()>7)
				{
					
					readok = true;
					globalNumber=MyTelephony.addPrefix(iso, iso, SimNumber, true);
					if (MyTelephony.checkValidePhone(iso, globalNumber) && MyUtil.checkValidePhoneNumber(globalNumber))
					{
						phoneNumberReadable=true;
//						id_desc.setText(R.string.register_desc_0);
					}
//					else
//						id_desc.setText(R.string.register_desc_1);
				}
//				else
//					id_desc.setText(R.string.register_desc_1);
	    	}
	    	else
	    	{
	    		DeviceID=null;
	    		SubscribeId = "0000000";
//	    		id_desc.setText(R.string.register_desc_2);
	    	}
	    }
//	    else
//	    {
//	    	id_desc.setText(R.string.register_desc_2);
//	    }
	    
	    if (DeviceID==null) DeviceID="";
	    
//	    if (devIsPhone)
//	    {
	    	if (phoneNumberReadable)
			{
	    		
	    		if (readok==true){
	    			username_view.setText(globalNumber.trim());
	    		}else {
	    			username_view.setText("+86"+globalNumber.trim());
				}
				//tml*** regisown
//				username_view.setEnabled(false);
//				username_view.setFocusable(false);
//				username_view.setFocusableInTouchMode(false);
				username_view.setEnabled(true);
				username_view.setFocusable(true);
			}
			else
			{
				username_view.setEnabled(true);
				username_view.setFocusable(true);
//				username_view.setInputType(InputType.TYPE_CLASS_PHONE);
				username=mPref.read("myPhoneNumber","");
			}
	    	
	    	/* alec */
//	    	if (SubscribeId.length()>10)
//	    	{
	    		//tml*** regisown
//	    		password1_view.setVisibility(View.GONE);
//	    		password2_view.setVisibility(View.GONE);
//	    		((View)findViewById(R.id.hr1)).setVisibility(View.GONE);
//	    		((View)findViewById(R.id.hr2)).setVisibility(View.GONE);
//		    	String password=SubscribeId.substring(SubscribeId.length()-4, SubscribeId.length());
//		    	password1_view.setText(password);
//		    	password2_view.setText(password);
//	    	}
//	    }
	    
//	    if (!withSIM || !devIsPhone) {
		if (!withSIM) {
	    	username_view.setHint(R.string.username_hint);
	    } else
	    	username_view.setHint(R.string.phonenumber_hint);
	    
	    
	    username_view.setSelection(username_view.getText().toString().length());
		
		email_view.setText(mPref.read("email",""));
		
	    ((Button) findViewById(R.id.register)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			String xusername=username_view.getText().toString();
    			//tml*** airplane mode
    			boolean airplaneMode = MyUtil.isAirplaneModeOn(RegisterActivity.this);
    			if (airplaneMode) {
    				Message msg = new Message();
    				msg.obj = "fail,airplanemode";
    				mHandler.sendMessage(msg);
    				return;
    			}
    			
    			//tml*** regisown
    			boolean isPhoneNum = MyTelephony.isPhoneNumber(xusername);
    			if (isPhoneNum) {
    				usePhoneNumberRegis = true;
//    				if (!xusername.equals(globalNumber) || !phoneNumberReadable) {
//        				Message msg = new Message();
//        				msg.obj = "fail,invalid_phone";
//        				mHandler.sendMessage(msg);
//        				username_view.setText(globalNumber);
//        				return;
//    				}
    			} else {
    				usePhoneNumberRegis = true;
    			}
    			
//    			if (usePhoneNumberRegis && devIsPhone && withSIM && !check_phonenumber(xusername)) {
        		if (usePhoneNumberRegis && withSIM && !check_phonenumber(xusername)) {
    				// check phone number
    				Message msg = new Message();
    				msg.obj = "fail,phonenumber";
    				mHandler.sendMessage(msg);
    				return;
//    			} else if ((!usePhoneNumberRegis || !devIsPhone) && !check_username(xusername)) {
    			} else if (!usePhoneNumberRegis && !check_username(xusername)) {
    				// check user name 
    				Message msg = new Message();
    				msg.obj = "fail,username";
    				mHandler.sendMessage(msg);
    				return;
    			}
    			
    			xusername = MyTelephony.cleanPhoneNumber(xusername);
				if(usePhoneNumberRegis && !phoneNumberReadable && withSIM && MyTelephony.isPhoneNumber(xusername)){
					xusername=MyTelephony.addPrefix(iso, iso, xusername, true);
					if (!MyTelephony.checkValidePhone(iso, xusername) || !MyUtil.checkValidePhoneNumber(xusername)){

						String msgContent = getString(R.string.phonenumber_invalid);

		    			Intent it=new Intent(RegisterActivity.this, CommonDialog.class);
		        		it.putExtra("msgContent", msgContent);
		        		it.putExtra("numItems", 1);
		        		it.putExtra("ItemCaption0", getString(R.string.close));
		        		it.putExtra("ItemResult0", RESULT_OK);
		        		startActivity(it);
						return;
					}
				
					/*String msgContent = MyTelephony.getCountryNameByNumber(RegisterActivity.this, xusername)+"\n"+
						MyTelephony.doHyphenation(iso,xusername);
					
	    			Intent it=new Intent(RegisterActivity.this, RegisterConfirm.class);
	        		it.putExtra("msgContent", msgContent);
	        		startActivityForResult(it, 63);*/
//					Toast.makeText(getApplicationContext(), "注册事件处理", 0).show();
					//xwf
					Intent intent=new Intent(RegisterActivity.this, EulaDialog.class);
//					username=data.getStringExtra("username");
//					password=data.getStringExtra("password");
	    			startActivityForResult(intent, 56);
				}
				else{
	    			Intent intent=new Intent(RegisterActivity.this, EulaDialog.class);
	    			startActivityForResult(intent, 64);
				}
    		}}
    	);
	    
	    IntentFilter intentToReceiveFilter = new IntentFilter(); 

	    intentToReceiveFilter.addAction("com.pingshow.amper.registration"); 
		registerReceiver(waitingForRegistrationSMS, intentToReceiveFilter);

	    username_view.setOnFocusChangeListener(new OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus) 
			{
				if(!hasFocus)
				{
					username_view.setText(username_view.getText().toString().toLowerCase());
				}
			}
		});
		
//		if (withSIM && devIsPhone)
//		{
//			int pcode_used=mPref.readInt("PCODE1",0);
//			if (pcode_used!=0)
//				popupPcodeRegister();
//		}

		Log.e("regis0B ::"
				+ " " + DeviceID
				+ "|" + SubscribeId
				+ "|" + iso
				+ "|" + withSIM
				+ "|" + _SimNumber
				+ "|" + phoneNumberReadable);
		new Thread(copyEula).start();
	}
	
	@Override
	protected void onDestroy() {
		mHandler.removeCallbacks(dismissProgressDialog);
		unregisterReceiver(waitingForRegistrationSMS);
		System.gc();
		System.gc();
		super.onDestroy();
	}
	
	private void copyFromPackage(int ressourceId, String target)
	{
		try{
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
		}catch(Exception e){}
	}
	
	Runnable copyEula=new Runnable()
	{
		public void run()
		{
			try{
				pcodePolicyChecked=mPref.readInt("pcodePolicyChecked",-1);
				if (pcodePolicyChecked==-1)
				{
					MyNet net = new MyNet(RegisterActivity.this);
					String Return=net.doPostHttps("checkpcodepolicy.php", "iso=" + iso, null);
					Log.d("checkpcodepolicy Return="+Return);
					if (Return.equals("pcode"))
					{
						byPassPcode=false;
						pcodePolicyChecked=0;
					}
					else
						pcodePolicyChecked=1;
					mPref.write("pcodePolicyChecked", pcodePolicyChecked);
				}
				else if (pcodePolicyChecked==1)
					byPassPcode=true;
				else if (pcodePolicyChecked==0)
					byPassPcode=false;
				
			}catch(Exception e){}
							
			String destURL="/data/data/com.pingshow.amper/files/eula.html";
			File FileToCopy = new File(destURL);
			if (!FileToCopy.exists())
			{
				try {
					copyFromPackage(R.raw.eula, FileToCopy.getName());
				} catch (Exception e) {}
			}
		}
	};
	
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			String Return = msg.obj.toString().toLowerCase(); 
			if (Return.startsWith("ok,") || Return.contains("ok,registration"))
			{
				mPref.write("Registered", true);
				mPref.write("myPhoneNumber", username);
				mPref.write("password", password);
				mPref.write("myIMEI", (DeviceID==null)?"":DeviceID);
				mPref.write("email", email);
				mPref.write("SubscribeId", (SubscribeId==null)?"":SubscribeId);
				mPref.write("firstRegister", true);
				mPref.write("firstEnter", true);
				
				if (Return.startsWith("ok,by_email"))
				{
					(new Thread(doActualRegister)).start();
					return;
				}
				else if (Return.startsWith("ok,accountupdated"))
				{
					mPref.write("accountUpdated", true);
					mPref.write("firstRegister", false);
				}
				
				Toast.makeText(RegisterActivity.this,R.string.welcome,Toast.LENGTH_LONG).show();
				setResult(RESULT_OK);
				finish();
				return;
			}

			if (Return.startsWith("fail,")){
				Return = Return.substring(5);
				if (progress!=null && progress.isShowing())
					progress.dismiss();
			}
			String errMsg="";
			if (Return.startsWith("eula")) {
				errMsg=getString(R.string.eula_disagree);
			}else if (Return.startsWith("email")||Return.startsWith("invalid_email")) {
				errMsg=getString(R.string.email_invalid);
			}else if (Return.startsWith("mismatch")) {
				errMsg=getString(R.string.passwords_dont_match);
			}else if (Return.startsWith("accountexists")) {
				errMsg=getString(R.string.account_exists);
			}else if (Return.startsWith("nonmember")||Return.startsWith("membernotfound")) {
				errMsg=getString(R.string.nonmember);
		    }else if (Return.startsWith("pwdemailfailed")) {
		    	errMsg=getString(R.string.pwdemailfailed);
		    }else if (Return.startsWith("pwdemailsuccess")) {
		    	errMsg=getString(R.string.pwdemailsuccess);
	        }else if (Return.startsWith("pwderror")) {
	        	errMsg=getString(R.string.password_error);
			}else if (Return.startsWith("username")) {
				errMsg=getString(R.string.username_invalid);
			}else if (Return.startsWith("phonenumber")) {
				errMsg=getString(R.string.phonenumber_invalid);
			}else if (Return.startsWith("wrong_username")) {
				errMsg=getString(R.string.no_sim_hint);
			}else if (Return.startsWith("invalid_username")) { // invalid username
				errMsg=getString(R.string.username_invalid);
			}else if (Return.startsWith("invalid_phone")) { // invalid phone #
				errMsg=getString(R.string.phonenumber_invalid);
			}else if (Return.startsWith("nonetwork") || Return.startsWith("error") || Return.startsWith("invalid")) {
				errMsg=getString(R.string.nonetwork);
			}else if (Return.equals("registered")){	// already registered, just exist
				errMsg=getString(R.string.account_exists);
//			}else if (Return.equals("overcredit")){
//				popupPcodeRegister();
			}else if (Return.equals("pingshow")) {	// pngshow registration failed
				errMsg="Registration failed!, network issue";
			}else if (Return.equals("nosipserver")) {//  Internal error, no sip server
				errMsg="Registration failed!, internal error";
			}else if(Return.equals("findpwd_input_err")){
				errMsg=getString(R.string.findpwd_input_err);
			}else if(Return.equals("airplanemode")){
				errMsg=getString(R.string.airplane_mode);
			}else{	// list of failed sip servers
				errMsg=Return;
			}
			
			Intent it=new Intent(RegisterActivity.this, CommonDialog.class);
			it.putExtra("msgContent", errMsg);
			it.putExtra("numItems", 1);
			it.putExtra("ItemCaption0", getString(R.string.done));
			it.putExtra("ItemResult0", RESULT_OK);
			startActivity(it);
		}
	};
	
	Runnable popupProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				progress = ProgressDialog.show(RegisterActivity.this, "", getString(R.string.in_progress), true, true);
			}catch(Exception e){}
		}
	};
	
	//alec
	static int retryTimes=0;
	Runnable dismissProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			if (progress.isShowing()){
				try{
					progress.dismiss();
				}catch(Exception e){}
				
//				if(popupSmsPending && SmsConfirmWaiting){
//					retryTimes++;
//					if (retryTimes>=3)//alec
//					{
//						//User tried 3 times, and no pcode received
//						username=mPref.read("username","");
//						password=mPref.read("password");//alec
//						(new Thread(doActualRegister)).start();
//					}
//					else{
//						popupPcodeRegister();
//						popupSmsPending = false;
//					}
//				}
			}
		}
	};
	
	//alec
	public void showNotification(String message, Intent it,
			boolean bSound, int icon, String title) {
		NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = new Notification(icon, message,
				System.currentTimeMillis());
		mNM.cancel(R.string.app_name);

		if (it == null)
			it = new Intent(this, SplashScreen.class);

		it.putExtra("fromNotification", true);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, it,
				PendingIntent.FLAG_UPDATE_CURRENT);

		if (title == null)
			title = getResources().getString(R.string.newmessage);

		notification.setLatestEventInfo(this, title, message, contentIntent);

		final boolean ringb = mPref.readBoolean("ringing", true) && bSound;
		final boolean recv_vbr = mPref.readBoolean("recvVibrator", true);
		notification.defaults = Notification.DEFAULT_LIGHTS
				| (recv_vbr ? Notification.DEFAULT_VIBRATE : 0)
				| (ringb ? Notification.DEFAULT_SOUND : 0);

		if (recv_vbr) notification.vibrate = new long[] { 100, 250 };
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		mNM.notify(R.string.app_name, notification);
	}
	
	//alec
	Runnable popupFakeRegisterSms=new Runnable()
	{
		@Override
		public void run() {
			Intent it = new Intent();
		    it.setAction("com.pingshow.amper.registration");
		    String body=getResources().getString(R.string.registration_body)+" "+pcode;
		    showNotification("+447780010807: "+ body, null, true, android.R.drawable.sym_action_email, null);
			it.putExtra("Passcode", pcode);
			sendBroadcast(it);
		}
	};
	
	Runnable popupSmsPendingDialog=new Runnable()
	{
		@Override
		public void run() {
//			progress = ProgressDialog.show(RegisterActivity.this, "", getString(R.string.waiting_for_sms_to_register), true, false);
//			SmsConfirmWaiting=true;
		}
	};
	
	Runnable popupTimeoutDialog=new Runnable()
	{
		@Override
		public void run() {
			Intent it=new Intent(RegisterActivity.this, CommonDialog.class);
			it.putExtra("msgContent", getString(R.string.sms_confirm_timeout));
			it.putExtra("numItems", 1);
			it.putExtra("ItemCaption0", getString(R.string.done));
			it.putExtra("ItemResult0", RESULT_OK);
			startActivity(it);
			SmsConfirmWaiting=false;
		}
	};
	
	public static boolean check_email_syntax(String email) {
		return email.matches("^[0-9a-zA-Z_\\-\\.]+@[0-9a-zA-Z_\\-\\.]+$");
	}
	
	public static boolean check_legal_name(String tmp) {
		return tmp.matches("/?[a-z][_a-z0-9]*");//alec lower case only, no space
	}
	
	public static boolean check_username(String uname) {
		if (uname.startsWith("+")) return false;
		if (uname.length()<6) return false;
		return check_legal_name(uname);
	}
	
	public boolean check_phonenumber(String phnumber) {
		if (phnumber==null || phnumber.length()<8) return false;
		if(!MyTelephony.isPhoneNumber(phnumber)) return false;
		if (!withSIM && phnumber.startsWith("+") && phnumber.length()>11) return true;
		phnumber = MyTelephony.addPrefix(iso, iso, phnumber, true);
		if (MyTelephony.checkValidePhone(iso, phnumber) && MyUtil.checkValidePhoneNumber(phnumber)){
			username = phnumber;
			return true;
		}
		return false;
	}
	
	Runnable doActualRegister=new Runnable()
	{
		@Override
		public void run() {
			mHandler.post(popupProgressDialog);
			MyNet net = new MyNet(RegisterActivity.this);
			String Return="";
			int count=0;
			do{
				try{
					Return=net.doPostHttps("register_aire.php", "id="+URLEncoder.encode(username,"UTF-8"), null);
				}catch (Exception e){}
				Log.d("register Return="+Return);
				if (Return.length()!=0 && !Return.startsWith("Error"))//no network
					break;
				MyUtil.Sleep(1500);
			}while(++count<3);
			
			mHandler.post(dismissProgressDialog);
			
			if (Return.length()==0)
				Return="fail,nonetwork";

			Message msg = new Message();
			msg.obj = Return;
			mHandler.sendMessage(msg);
		}
	};
	
	
	Runnable mRegisterTask = new Runnable() {
        public void run() {
        	username = MyTelephony.cleanPhoneNumber(username_view.getText().toString());
        	password = password1_view.getText().toString();
        	String password2 = password2_view.getText().toString();
        	email = email_view.getText().toString().trim();
            
//        	if (usePhoneNumberRegis && devIsPhone && withSIM && !check_phonenumber(username)) {
            if (usePhoneNumberRegis && withSIM && !check_phonenumber(username)) {
				// check phone number
				Message msg = new Message();
				msg.obj = "fail,phonenumber";
				mHandler.sendMessage(msg);
				return;
//			}else if (!devIsPhone && !check_username(username)) {
			}else if (!check_username(username)) {
				// check user name 
				Message msg = new Message();
				msg.obj = "fail,username";
				mHandler.sendMessage(msg);
				return;
			}else if (password.length()<4) {
				Message msg = new Message();
				msg.obj = "fail,pwderror";
				mHandler.sendMessage(msg);
				return;
			}else if (!password.equals(password2)) {	// check password
				Message msg = new Message();
				msg.obj = "fail,mismatch";
				mHandler.sendMessage(msg);
				return;
			}else if (!check_email_syntax(email) || email.length()<5){	
				// check email syntax
				Message msg = new Message();
				msg.obj = "fail,email";
				mHandler.sendMessage(msg);
				return;
			}
			else if (checked != 1) {	// eula not chcked
				Message msg = new Message();
				msg.obj = "fail,eula";
				mHandler.sendMessage(msg);
				return;
			}
			
			Random random=new Random();
			pcode=Math.abs(random.nextInt()%9000)+1000;
			
			try{
				mHandler.post(popupProgressDialog);
				int count=0;
				String Return="";
				do {
					MyNet net = new MyNet(RegisterActivity.this);
					Return=net.doPostHttps("preregister_aire.php", "id=" + URLEncoder.encode(username,"UTF-8")
								+"&password="+ URLEncoder.encode(password, "UTF-8")
								+"&email=" + URLEncoder.encode(email, "UTF-8")
								+"&eula=" + checked
								+"&pcode=" + pcode
								+"&sms="+(byPassPcode?"0":"1")
								+"&nickname=" + URLEncoder.encode(email.substring(0,email.indexOf('@')),"UTF-8")
								+"&imei=" + URLEncoder.encode(DeviceID, "UTF-8")
								+"&acct=0",null);
					if (Return.length()!=0 && !Return.startsWith("Error"))//no network
						break;
					MyUtil.Sleep(1500);
				    count++;
				}while(count<3);
				
				mHandler.post(dismissProgressDialog);
				
				mPref.write("myPhoneNumber", username);
				mPref.write("email", email);
				
				if (Return.length()==0)//no network
				{
					Return="fail,nonetwork";
				}
				else if (Return.startsWith("pending,"))
				{
					mHandler.post(popupSmsPendingDialog);
					popupSmsPending = true;
					if (byPassPcode)
					{
						if (withSIM) 
							mHandler.postDelayed(popupFakeRegisterSms, 6000);
					}
					
					mHandler.postDelayed(dismissProgressDialog, 30000);
					
					for(int i=1;i<6;i++){
						if(mPref.readInt("PCODE"+i,0)==0){
							mPref.write("PCODE"+i,pcode);
							break;
						}
					}
					
					mPref.write("username",username);
					mPref.write("password", password);
					return;
				}
				else
					mPref.write("email", email);
				
				Message msg = new Message();
				msg.obj = Return;
				mHandler.sendMessage(msg);
			}catch(Exception e){}
        }
    };
    
    Runnable mStraightRegisterTask = new Runnable() {
        public void run() {
        	username = MyTelephony.cleanPhoneNumber(username_view.getText().toString());
        	password = password1_view.getText().toString();
        	String password2 = password2_view.getText().toString();
        	email = email_view.getText().toString().trim();
        	
        	if (password.length()<4) {
				Message msg = new Message();
				msg.obj = "fail,pwderror";
				mHandler.sendMessage(msg);
				return;
			}else if (!password.equals(password2)) {	// check password
				Message msg = new Message();
				msg.obj = "fail,mismatch";
				mHandler.sendMessage(msg);
				return;
			} else if (!check_email_syntax(email)){	
				// check email syntax
				Message msg = new Message();
				msg.obj = "fail,email";
				mHandler.sendMessage(msg);
				return;
			}else if (checked != 1) {	// eula not chcked
				Message msg = new Message();
				msg.obj = "fail,eula";
				mHandler.sendMessage(msg);
				return;
			}
        		
			MyNet net = new MyNet(RegisterActivity.this);
			try{
				mHandler.post(popupProgressDialog);
				String src=username+":"+password+":"+email;
				String Md5=getMD5(src.getBytes());
				int count=0;
				String Return="";
				do{
					Return=net.doPostHttps("register_aire.php", "id=" + URLEncoder.encode(username,"UTF-8")
							+"&password="+ URLEncoder.encode(password, "UTF-8")
							+"&email=" + URLEncoder.encode(email, "UTF-8")
							+"&eula=" + checked
							+"&verified=1"
							+"&code=" + Md5
							+"&nickname=" + URLEncoder.encode(email.substring(0,email.indexOf('@')),"UTF-8")
							+"&imei=" + URLEncoder.encode(DeviceID, "UTF-8"),null);
					Log.d("register_aire=" + email + " " + URLEncoder.encode(email, "UTF-8"));
					Log.d("register_aire Return="+Return);
					 if (Return.length()!=0 && !Return.startsWith("Error"))//no network
						break;
					count++;
					MyUtil.Sleep(1500);
				}while(count<3);
				
				mPref.write("accountUpdated", true);//alec: ensure 120 will be sent
			
				mHandler.post(dismissProgressDialog);
				if (Return.length()==0)
					Return="fail,nonetwork";
				
				Message msg = new Message();
				msg.obj = Return;
				mHandler.sendMessage(msg);
				
			}catch(Exception e){}
        }
    };
    
    BroadcastReceiver waitingForRegistrationSMS = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent){
			if (intent.getAction().equals("com.pingshow.amper.registration") && SmsConfirmWaiting)
			{
				SmsConfirmWaiting=false;
				mHandler.post(dismissProgressDialog);
				int ps=intent.getIntExtra("Passcode",0);
				if (ps!= 0)
				{
					for(int i=1;i<6;i++){
						if(mPref.readInt("PCODE"+i,0)==ps){
							// clear all pcode
							for(int j=1;j<6;j++)
								mPref.delect("PCODE"+j);
							(new Thread(doActualRegister)).start();
							break;
						}
					}
				}
			}
		}
	};
	
	static public String getMD5(byte[] source)
	{
		String s = null;
		final char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
		try
		{
			java.security.MessageDigest md = java.security.MessageDigest.getInstance( "MD5" );
			md.update(source);
			byte tmp[] = md.digest();
			char str[] = new char[16 * 2];
			int k = 0;
			for (int i = 0; i < 16; i++) {
				byte byte0 = tmp[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];                                           
				str[k++] = hexDigits[byte0 & 0xf];
			}
			s = new String(str);
	   }catch(Exception e){
		   Log.e("getMD5 !@#$ " + e.getMessage());
	   }
	   return s;
	}
	
	void popupPcodeRegister()
	{
//		mHandler.postDelayed(new Runnable(){
//			public void run(){
//				Intent intent=new Intent(RegisterActivity.this, PcodeActivity.class);
//				startActivityForResult(intent, 48);
//			}
//		},500);
	}
	
	@Override  
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		super.onActivityResult(requestCode, resultCode, data);  
		if(requestCode==16)
		{
			if (resultCode==RESULT_OK) {
				username=data.getStringExtra("username");
				password=data.getStringExtra("password");
				Intent intent=new Intent(RegisterActivity.this, EulaDialog.class);
    			startActivityForResult(intent, 56);
			}
		}
		else if (requestCode==63)
		{
			if (resultCode==RESULT_OK) {
				Intent intent=new Intent(RegisterActivity.this, EulaDialog.class);
				startActivityForResult(intent, 64);
			}
		}
		else if (requestCode==64)
		{
			if (resultCode==RESULT_OK) {
				checked=data.getIntExtra("agreement",0);

				String xusername = MyTelephony.cleanPhoneNumber(username_view.getText().toString());
				if(!phoneNumberReadable && MyTelephony.isPhoneNumber(xusername)){
					// phoneNumberReadable is true, if the user were invited
					Cursor cursor1 = null;
					try {
						String md5=RegisterActivity.getMD5(xusername.getBytes());
						String md5globalNumber = "["+md5.substring(0,8).toUpperCase()+"]";
						
						cursor1 = getContentResolver().query(Uri.parse("content://sms/inbox"), new String[]{"body"}, null, null, "date desc");
						while (cursor1.moveToNext())
						{
							String body = cursor1.getString(0);
							if(body.startsWith(md5globalNumber) || body.endsWith(md5globalNumber)){
								phoneNumberReadable = true;
								break;
							}
						}
					}catch (Exception e) {}
					finally{
						if (cursor1!=null && !cursor1.isClosed()) cursor1.close();
					}
				}
				
				if (phoneNumberReadable)
				{
					if (usePhoneNumberRegis) {  //tml*** regisown
						Thread th = new Thread(mStraightRegisterTask);
						th.start();
					} else {
						Thread th = new Thread(mRegisterTask);
						th.start();
					}
				}else{
					Thread th = new Thread(mRegisterTask);
					th.start();
				}
			}
		}
		else if (requestCode==48)
		{
			if (resultCode==RESULT_OK) {
				boolean found=false;
				int ps=data.getIntExtra("pcode_input",0);
				if (ps!=0)
				{
					for(int i=1;i<6;i++){
						if(mPref.readInt("PCODE"+i,0)==ps){
							found=true;//alec
							username=mPref.read("username","");
							password=mPref.read("password");//alec
							(new Thread(doActualRegister)).start();
							break;
						}
					}
				}
				if (!found)//alec
				{
					Toast.makeText(this,getString(R.string.pcode_check), Toast.LENGTH_LONG).show();
					popupPcodeRegister();
				}
			}
		}
		else if(requestCode==120){
			if (resultCode==RESULT_OK) {
				Toast.makeText(RegisterActivity.this,R.string.welcome,Toast.LENGTH_LONG).show();
				setResult(RESULT_OK);
				finish();	
			}
		}
	}
	
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
