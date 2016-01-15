package com.pingshow.airecenter.register;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.json.JSONException;
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
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.pingshow.airecenter.CommonDialog;
import com.pingshow.airecenter.FacebookSearch;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.network.MyNet;
import com.pingshow.util.MyUtil;
import com.pingshow.util.ResizeImage;

public class FacebookLoginDialog extends Activity{
	
	private TextView mFacebookName;
	private String username;
	private String email;
	private String password;
	private String DeviceID;
	
	private AsyncFacebookRunner mAsyncRunner;
	private Facebook facebook;
	private MyPreference mPref;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fb_login_page);
        mFacebookName=(TextView)findViewById(R.id.username);
        mPref=new MyPreference(this);
        
        facebook = new Facebook("133881496748582");
        mAsyncRunner=new AsyncFacebookRunner(facebook);

        facebook.authorize(this, new String[]{"email","read_friendlists"}, new com.facebook.android.Facebook.DialogListener() {
            @Override
            public void onComplete(Bundle values) {
            	Intent intent=new Intent(FacebookLoginDialog.this, EulaDialog.class);
        		startActivityForResult(intent, 64);
            }

            @Override
            public void onFacebookError(FacebookError error) {
            	
            	((Button) findViewById(R.id.back)).setEnabled(true);
            	
            	Intent it=new Intent(FacebookLoginDialog.this, CommonDialog.class);
    			it.putExtra("msgContent", error.getMessage());
    			it.putExtra("numItems", 1);
    			it.putExtra("ItemCaption0", getString(R.string.done));
    			it.putExtra("ItemResult0", RESULT_OK);
    			startActivity(it);
    			
            	//setResult(RESULT_CANCELED);
            	//finish();
            }

            @Override
            public void onError(DialogError e) {
            	
            	((Button) findViewById(R.id.back)).setEnabled(true);
            	
            	Intent it=new Intent(FacebookLoginDialog.this, CommonDialog.class);
    			it.putExtra("msgContent", e.getMessage());
    			it.putExtra("numItems", 1);
    			it.putExtra("ItemCaption0", getString(R.string.done));
    			it.putExtra("ItemResult0", RESULT_OK);
    			startActivity(it);
    			
            	//setResult(RESULT_CANCELED);
            	//finish();
            }

            @Override
            public void onCancel() {
            	setResult(RESULT_CANCELED);
            	finish();
            }
        });
        
        new Thread(copyEula).start();
    }
	
	public void requestUserData() {
		Bundle params = new Bundle();
        params.putString("fields", "name, email, picture, gender");
        params.putString("type", "large");
        mAsyncRunner.request("me", params, new UserRequestListener());
    }
	
	private boolean downloadPhoto(String remote, String uid) 
	{
		String tmp=Global.SdcardPath_sent +"tmp.jpg";
		String out=Global.SdcardPath_sent +uid+".jpg";
		try {
			boolean success = false;
			int count = 0;
			do {
				MyNet net = new MyNet(FacebookLoginDialog.this);
				if (success = net.anyDownload(remote, tmp))
					break;
				count++;
				MyUtil.Sleep(500);
			} while (!success && count < 3);
			
			ResizeImage.ResizeXY(this,tmp,out,240,100);
			return true;
		} catch (Exception e) {
			Log.e("Download failed.");
		}
		return false;
	}
	
	ProgressDialog progress;
	Runnable popupProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				progress = ProgressDialog.show(FacebookLoginDialog.this, "", getString(R.string.in_progress), true, true);
			}catch(Exception e){
			}
		}
	};
	
	Runnable dismissProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				if (progress!=null)
				{
					if (progress.isShowing()){
						progress.dismiss();
					}
				}
			}catch(Exception e){
			}
		}
	};
	
	
	public class UserRequestListener extends BaseRequestListener {
        @Override
        public void onComplete(final String response, final Object state) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(response);

                final String picURL = jsonObject.getString("picture");
                final String nickname = jsonObject.getString("name");
                final String gender = jsonObject.getString("gender");
                email = jsonObject.getString("email");
                final String fbUID = jsonObject.getString("id");
                
                username = FacebookSearch.FacebookIDtoAireID(fbUID);
                
                password = fbUID.substring(fbUID.length()-4, fbUID.length());
                mPref.write("myNickname",nickname);
                mPref.write("myFacebookID",fbUID);
                mPref.write("myGender",gender);
                
                new Thread(new Runnable(){
                	public void run()
                	{
		                mHandler.post(popupProgressDialog);
		                TelephonyManager tMgr=(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		        	    DeviceID=tMgr.getDeviceId();
		        	    if (DeviceID==null) DeviceID="";//alec
		        	    
		    			String Return="";
		    			try{
			    			int count=0;
			    			do {
								MyNet net = new MyNet(FacebookLoginDialog.this);
								Return=net.doPostHttps("preregister_aire.php", "id=" + URLEncoder.encode(username,"UTF-8")
											+"&password="+ password
											+"&email=" + URLEncoder.encode(email, "UTF-8")
											+"&eula=1"
											+"&pcode=0000"
											+"&nickname=" + URLEncoder.encode(nickname,"UTF-8")
											+"&imei=" + URLEncoder.encode(DeviceID, "UTF-8")
											+"&acct=0",null);
								Log.d("preregister_aire Return="+Return);
								if (Return.length()!=0 && !Return.startsWith("Error"))//no network
									break;
								MyUtil.Sleep(1500);
							    count++;
							}while(count<3);
		    			}catch(Exception e){
		    			}
		    			
		    			mHandler.post(dismissProgressDialog);
		    			
		    			if (Return.length()==0)
		    				Return="fail,nonetwork";
		    			
		    			Message msg = new Message();
		    			msg.obj = Return;
		    			mHandler.sendMessage(msg);
                	}
                }).start();
    			
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                    	mFacebookName.setText(nickname);
                    	if (!downloadPhoto("http://graph.facebook.com/"+fbUID+"/picture?type=large",username))
                    		downloadPhoto(picURL,username);
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

		@Override
		public void onFacebookError(FacebookError e, Object state) {
		}
    }
	
	Runnable doActualRegister=new Runnable()
	{
		@Override
		public void run() {
			mHandler.post(popupProgressDialog);
			MyNet net = new MyNet(FacebookLoginDialog.this);
			String Return="";
			int count=0;
			do{
				try{
					Return=net.doPostHttps("register_aire.php", "id="+URLEncoder.encode(username,"UTF-8"), null);
				}catch (Exception e){
				}
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
	
	public void onResume() {    
        super.onResume();
        facebook.extendAccessTokenIfNeeded(this, null);
//        MobclickAgent.onResume(this);
    }
	
	@Override
	protected void onPause() {
//		MobclickAgent.onPause(this);
		super.onPause();
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==64)
        {
        	if (data.getIntExtra("agreement",0)==0)
        	{
        		Intent it=new Intent(this, CommonDialog.class);
    			it.putExtra("msgContent", getString(R.string.eula_disagree));
    			it.putExtra("numItems", 1);
    			it.putExtra("ItemCaption0", getString(R.string.done));
    			it.putExtra("ItemResult0", RESULT_OK);
    			startActivity(it);
    			
    			((Button)findViewById(R.id.back)).setEnabled(true);
    			((Button)findViewById(R.id.back)).setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View arg0) {
						finish();
					}
    			});
    			return;
        	}
        	
        	if (resultCode==RESULT_OK)
        		requestUserData();
        }
        else
        	facebook.authorizeCallback(requestCode, resultCode, data);
    }
    
    Runnable copyEula=new Runnable()
	{
		public void run()
		{
			String destURL="/data/data/com.pingshow.airecenter/files/eula.html";
			File FileToCopy = new File(destURL);
			if (!FileToCopy.exists())
			{
				try {
					copyFromPackage(R.raw.eula, FileToCopy.getName());
				} catch (IOException e) {
				}
			}
		}
	};
	
	private void copyFromPackage(int ressourceId, String target) throws IOException {
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
    
    Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			String Return = msg.obj.toString().toLowerCase(); 
			if (Return.startsWith("ok,") || Return.contains("ok,registration") || Return.startsWith("fail,accountexists"))
			{
				TelephonyManager tMgr=(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
				String SubscribeId=tMgr.getSubscriberId();
				
				mPref.write("AireRegistered", true);
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
				else if (Return.startsWith("fail,accountexists"))
				{
					mPref.write("accountUpdated", true);
					mPref.write("firstRegister", false);
				}
				Toast.makeText(FacebookLoginDialog.this,R.string.welcome,Toast.LENGTH_LONG).show();
				mPref.write("LoginByFacebook", true);
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
			if (Return.startsWith("email")||Return.startsWith("invalid_email")) {
				errMsg=getString(R.string.email_invalid);
			}else if (Return.startsWith("mismatch")) {
				errMsg=getString(R.string.passwords_dont_match);
			}else if (Return.startsWith("accountexists")) {
				errMsg=getString(R.string.account_exists);
			}else if (Return.startsWith("nonmember")||Return.startsWith("membernotfound")) {
				errMsg=getString(R.string.nonmember);
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
			
			Intent it=new Intent(FacebookLoginDialog.this, CommonDialog.class);
			it.putExtra("msgContent", errMsg);
			it.putExtra("numItems", 1);
			it.putExtra("ItemCaption0", getString(R.string.done));
			it.putExtra("ItemResult0", RESULT_OK);
			startActivity(it);
		}
	};
}
