package com.pingshow.airecenter.register;

import java.net.URLEncoder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.CommonDialog;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.map.LocationUpdate;
import com.pingshow.network.MyNet;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;

public class LoginActivity extends Activity {

	private MyPreference mPref;
	private EditText username_view;
	private EditText password1_view;
	
	private String globalNumber;
	private String SubscribeId="";
	private String DeviceID="";//IMEI for GSM or MEID for WCDMA
	private String iso;
	
	private boolean devIsPhone=false;
	private boolean withSIM=false;
	private boolean phoneNumberReadable=false;
	private boolean noNeedPwd=false;
	
	private ProgressDialog progress;
	
	String username="";
	String password="";
	int checked=0;
	
	static private LocationUpdate location=null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_page);
		this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
		
		mPref=new MyPreference(this);
		
		username_view = (EditText)findViewById(R.id.username);
		password1_view = (EditText)findViewById(R.id.password1);
		
		TelephonyManager tMgr=(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		DeviceID=tMgr.getDeviceId();
		//SubscribeId=tMgr.getSubscriberId();
		//iso=tMgr.getSimCountryIso().toLowerCase();
		SubscribeId=null;//STB has no SIM card
	    iso=null;//STB has no SIM card
		
		TextView id_desc=(TextView)findViewById(R.id.id_desc);
		
		if (DeviceID!=null && DeviceID.length()>8 && iso!=null && iso.length()>1)
		{
			if (SubscribeId!=null && SubscribeId.length()>10 && !SubscribeId.contains("0000000"))
			{
				devIsPhone=true;
				withSIM=true;
				
				String SimNumber=tMgr.getLine1Number();
				if (SimNumber!=null && SimNumber.length()>6)
				{
					globalNumber=MyTelephony.addPrefix(iso, iso, SimNumber, true);
					if (MyTelephony.checkValidePhone(iso, globalNumber) && MyUtil.checkValidePhoneNumber(globalNumber))
					{
						phoneNumberReadable=true;
						id_desc.setText(R.string.login_desc_0);
					}
					else
						id_desc.setText(R.string.login_desc_1);
				}
				else
					id_desc.setText(R.string.login_desc_1);
			}
			else{
				DeviceID=null;
				SubscribeId = "0000000";
				id_desc.setText(R.string.login_desc_2);
			}
		}
		else
		{
			id_desc.setText(R.string.login_desc_2);
		}
		
		if (DeviceID==null) DeviceID="";
		
		if (devIsPhone)
		{
			if (phoneNumberReadable)
			{
				username_view.setText(globalNumber);
				username_view.setEnabled(false);
				username_view.setFocusable(false);
				username_view.setFocusableInTouchMode(false);
			}
			else{
				username_view.setEnabled(true);
				username_view.setFocusable(true);
				username_view.setInputType(InputType.TYPE_CLASS_PHONE);
				username=mPref.read("myPhoneNumber","");
			}
			
			if (SubscribeId.length()>10)
	    	{
				password1_view.setVisibility(View.GONE);
				((View)findViewById(R.id.hr1)).setVisibility(View.GONE);
		    	((Button) findViewById(R.id.forget)).setVisibility(View.GONE);
		    	String password=SubscribeId.substring(SubscribeId.length()-4, SubscribeId.length());
		    	password1_view.setText(password);

				noNeedPwd=true;
	    	}
		}
		
		if (!withSIM || !devIsPhone)
			username_view.setHint(R.string.username_hint);
		else
			username_view.setHint(R.string.phonenumber_hint);
		
		username_view.setSelection(username_view.getText().toString().length());
		
		((Button) findViewById(R.id.forget)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent it=new Intent(LoginActivity.this, CommonDialog.class);
				it.putExtra("msgContent", getString(R.string.retrieve_password));
				it.putExtra("numItems", 2);
				it.putExtra("ItemCaption0", getString(R.string.yes));
				it.putExtra("ItemResult0", RESULT_OK);
				it.putExtra("ItemCaption1", getString(R.string.cancel));
				it.putExtra("ItemResult1", RESULT_CANCELED);
				startActivityForResult(it, 228);
			}
		});
		
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
		
		((Button) findViewById(R.id.forget)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			Intent it=new Intent(LoginActivity.this, CommonDialog.class);
        		it.putExtra("msgContent", getString(R.string.retrieve_password));
        		it.putExtra("numItems", 2);
        		it.putExtra("ItemCaption0", getString(R.string.yes));
        		it.putExtra("ItemResult0", RESULT_OK);
        		it.putExtra("ItemCaption1", getString(R.string.cancel));
        		it.putExtra("ItemResult1", RESULT_CANCELED);
        		startActivityForResult(it, 228);
    		}
    	});
		
		((Button)findViewById(R.id.login)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Thread th = new Thread(mLoginTask);
				th.start();
			}}
		);
	}
	
	Runnable doRetrievePassword=new Runnable()
	{
		@Override
		public void run() {
    		if (!check_username(username) && !check_phonenumber(username))
    		{
        		Message msg = new Message();
				msg.obj = "fail,findpwd_input_err";
				mHandler.sendMessage(msg);
				return;
    		}
    		
    		mHandler.post(popupProgressDialog);
    		
    		int count=0;
    		String Return="";
    		do{
	        	MyNet net = new MyNet(LoginActivity.this);
				try{
					if(phoneNumberReadable)
						Return=net.doPostHttps("sendpwdemail_aire.php", "id="+URLEncoder.encode(globalNumber,"UTF-8"),null);
					else
						Return=net.doPostHttps("sendpwdemail_aire.php", "id="+URLEncoder.encode(username,"UTF-8"),null);
					
					Log.d("send password email Return="+Return);
					if (Return.length()!=0 && !Return.startsWith("Error"))
						break;
					MyUtil.Sleep(1500);
				}catch (Exception e){
				}
    		}while(++count<3);
    		
    		mHandler.post(dismissProgressDialog);
    		
    		if (Return.length()==0)
				Return="fail,nonetwork";
    		
    		Message msg = new Message();
			msg.obj = Return;
			mHandler.sendMessage(msg);
		}
	};
	
	@Override  
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		super.onActivityResult(requestCode, resultCode, data);  
		if (requestCode==228)
		{
			if (resultCode==RESULT_OK) {
				if(phoneNumberReadable)
            	{
            		username=globalNumber;//alec
            		mHandler.post(doRetrievePassword);
            	}
            	else
            	{
            		Intent intent=new Intent(LoginActivity.this, RetrievePwdActivity.class);
        			intent.putExtra("devIsPhone", devIsPhone);
        			intent.putExtra("phoneNumberReadable", phoneNumberReadable);
        			intent.putExtra("globalNumber", globalNumber);
        			startActivityForResult(intent, 32);
				}
			}
		}else if (requestCode==32)
		{
			if (resultCode==RESULT_OK) {
				username=data.getStringExtra("username");
				Thread th = new Thread(doRetrievePassword);
				th.start();
			}
		}
	}
	
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			String Return = msg.obj.toString().toLowerCase(); 
			if (Return.startsWith("ok,") || Return.contains("ok,registration"))
			{
				mPref.write("AireRegistered", true);
				mPref.write("myPhoneNumber", username);
				mPref.write("password", password);
				mPref.write("myIMEI", (DeviceID==null)?"":DeviceID);
				mPref.write("SubscribeId", (SubscribeId==null)?"":SubscribeId);
				mPref.write("firstEnter", true);
				
				if (Return.startsWith("ok,by_email"))
				{
					Intent it=new Intent(LoginActivity.this, CommonDialog.class);
					it.putExtra("msgContent", getString(R.string.email_register));
					it.putExtra("numItems", 1);
					it.putExtra("ItemCaption0", getString(R.string.done));
					it.putExtra("ItemResult0", RESULT_OK);
					startActivityForResult(it,120);
					return;
				}
				Toast.makeText(LoginActivity.this,R.string.welcome,Toast.LENGTH_LONG).show();
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
				errMsg=getString(R.string.login_nonmember);
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
			}else if (Return.equals("pingshow")) {	// pngshow registration failed
				errMsg="Registration failed!, network issue";
			}else if (Return.equals("nosipserver")) {//  Internal error, no sip server
				errMsg="Registration failed!, internal error";
			}else if(Return.equals("findpwd_input_err")){
				errMsg=getString(R.string.findpwd_input_err);
			}else{	// list of failed sip servers
				errMsg=Return;
			}
			
			Intent it=new Intent(LoginActivity.this, CommonDialog.class);
			it.putExtra("msgContent", errMsg);
			it.putExtra("numItems", 1);
			it.putExtra("ItemCaption0", getString(R.string.done));
			it.putExtra("ItemResult0", RESULT_OK);
			startActivity(it);
			
			if (noNeedPwd && Return.startsWith("pwderror"))
			{
				password1_view.setText("");
				password1_view.setVisibility(View.VISIBLE);
				((View)findViewById(R.id.hr1)).setVisibility(View.VISIBLE);
		    	((Button) findViewById(R.id.forget)).setVisibility(View.VISIBLE);
			}
		}
	};
	
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

	Runnable mLoginTask = new Runnable() {
		public void run() {
			
			username = MyTelephony.cleanPhoneNumber(username_view.getText().toString());
        	password = password1_view.getText().toString();
        	
			Message msg = new Message();
			if (password.length()<4)
			{
				msg.obj = "fail,pwderror";
				mHandler.sendMessage(msg);
				return;
			}else if (devIsPhone && withSIM && !check_phonenumber(username)){
				msg.obj = "fail,phonenumber";
				mHandler.sendMessage(msg);
				return;
			}else if (!devIsPhone)
			{
				if (!withSIM && check_phonenumber(username))
				{
					msg.obj = "fail,wrong_username";
					mHandler.sendMessage(msg);
					return;
				}
				else if (!check_username(username))
				{
					msg.obj = "fail,username";
					mHandler.sendMessage(msg);
					return;
				}
			}
			
			try{
				mHandler.post(popupProgressDialog);
				int count=0;
				String Return="";
				do{
					MyNet net = new MyNet(LoginActivity.this);
					Return= net.doPostHttps("login_aire.php",
							"id="+URLEncoder.encode(username,"UTF-8") +
							"&password="+URLEncoder.encode(password,"UTF-8"),null);
					if (Return.length()!=0 && !Return.startsWith("Error"))//no network
						break;
					count++;
					MyUtil.Sleep(1500);
				}while(count<3);
				
				//tml
				if (Return.length() != 0) {
					MyNet net = new MyNet(LoginActivity.this);
					Return= net.doPostHttps("login_aire.php",
							"id=" + URLEncoder.encode(username,"UTF-8")
							+ "&password=" + URLEncoder.encode(password,"UTF-8")
							+ "&idx=" + URLEncoder.encode(username,"UTF-8")
							+ "&areaid=" + URLEncoder.encode(username,"UTF-8")
							+ "&email=" + URLEncoder.encode(username,"UTF-8")
							+ "&address=" + URLEncoder.encode(username,"UTF-8"),null);
				}
				
				mHandler.post(dismissProgressDialog);
				if (Return.length()==0)
					Return="fail,nonetwork";
				
				msg.obj = Return;
				mHandler.sendMessage(msg);
				
			}catch(Exception e){
			}
		}
	};
	
	
	Runnable popupProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			progress = ProgressDialog.show(LoginActivity.this, "", getString(R.string.in_progress), true, true);
		}
	};
	
	Runnable dismissProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				if (progress.isShowing())
					progress.dismiss();
			}catch(Exception e){
			}
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
