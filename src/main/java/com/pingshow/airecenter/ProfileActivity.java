package com.pingshow.airecenter;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.register.EnterWithoutRegister;
import com.pingshow.airecenter.register.RegisterActivity;
import com.pingshow.network.MyNet;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;
import com.pingshow.util.ResizeImage;

public class ProfileActivity extends Activity {
	private MyPreference mPref;
	private String photoPath = null;
	private ImageView photoView;
	private boolean photoChanged;
	private Handler mHandler=new Handler();
	private String defaultPW = "ping1111";
	
	private static final int PHOTO_SIZE = 720;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPref=new MyPreference(this);
        
        if (mPref.readBoolean("ProfileCompleted", false) && mPref.readBoolean("accphpdone", false))
        {
        	Intent intent = new Intent();
        	intent.setClass(ProfileActivity.this, MainActivity.class);
			startActivity(intent);
			finish();
			return;
        }
        setContentView(R.layout.profile_page);
        this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        photoView = (ImageView)findViewById(R.id.photo);
        photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
		int myuid = 0;
		try{
			myuid=Integer.valueOf(mPref.read("myID", "0"), 16);
		}catch(Exception e){}
		
		if (myuid>0)
		{
			photoPath = Global.SdcardPath_sent + "myself_photo_" + myuid + ".jpg";
			Drawable photo = ImageUtil.loadBitmapSafe(photoPath, 1);
			if (photo != null) {
				((TextView)findViewById(R.id.my_photo_hint)).setVisibility(View.GONE);
				photoView.setImageDrawable(photo);
				mPref.write("myPhotoPath", photoPath);
			}
			else
				photoPath=null;
		}
        
        photoView.setOnClickListener(new OnClickListener() {
	    	@Override
	    	public void onClick(View v) {
	    		onPickPictureOption();
	    	}
	    });
        
        String gender=mPref.read("myGender",null);
        if (gender!=null)
        {
        	((RadioButton)findViewById(R.id.male)).setChecked(gender.equals("male")?true:false);
        	((RadioButton)findViewById(R.id.male)).setEnabled(false);
        	((RadioButton)findViewById(R.id.female)).setEnabled(false);
        }
        else
        	((RadioButton)findViewById(R.id.male)).setChecked(true);
        
        ((RadioButton)findViewById(R.id.male)).setOnCheckedChangeListener(new OnCheckedChangeListener(){
        	@Override
        	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        		mPref.write("myGender", isChecked?"male":"female");
			}
        });
        
        ((Button)findViewById(R.id.done)).setOnClickListener(new OnClickListener(){
        	@Override
			public void onClick(View v) {
        		
        		if (mPref.readBoolean("LoginByFacebook"))
        		{
	        		if (!mPref.readBoolean("fbSearched",false))
	        		{
	        			mPref.write("fbSearched",true);
	        			mHandler.post(doSearchFacebookFriends);
	        			return;
	        		}
        		}
        		
        		finishProfile.run();
			}
        });
        
        if (photoPath==null)
        	mHandler.postDelayed(showTooltip, 1000);
        
        new Thread(downloadProfilePhoto).start();
        new Thread(getPersonalInfo).start();
        mHandler.post(popupProgressDialog);
	}
	
	private boolean downloadAnyPhoto(String remotefile, String localfile, int retry, boolean dontTryLocalPhpServer) {
		try {
			int success = 0;
			int count = 0;
			if (!dontTryLocalPhpServer)
			{
				do {
					MyNet net = new MyNet(ProfileActivity.this);
					success = net.Download(remotefile, localfile, AireJupiter.myLocalPhpServer);
					if (success==1||success==0)
						break;
					MyUtil.Sleep(500);
				} while (++count < retry);
			}
			
			if (success!=1)
			{
				count=0;
				do {
					MyNet net = new MyNet(ProfileActivity.this);
					success = net.Download(remotefile, localfile, null);
					if (success==1||success==0)
						break;
					MyUtil.Sleep(500);
				} while (++count < retry);
			}
			if (success==1) {
				return true;
			}
		} catch (Exception e) {
			Log.e("Download failed.");
		}
		return false;
	}
	
	Runnable downloadProfilePhoto=new Runnable()
	{
		public void run()
		{
			int myidx = 0;
			int count = 0;
			while(myidx==0 && count++<3)
			{
				try{
					myidx=Integer.valueOf(mPref.read("myID", "0"), 16);
				}catch(Exception e){}
				if (myidx>0) break;
				MyUtil.Sleep(1000);
			}
			
			if (MyUtil.checkSDCard(getApplicationContext()))
			{
				String localfile = Global.SdcardPath_sent + "myself_photo_" + myidx + ".jpg";
				File f=new File(localfile);
				if (!f.exists())
				{
					String remotefile = "profiles/photo_" + myidx + ".jpg";
					if (downloadAnyPhoto(remotefile, localfile, 1, true))
					{
						mPref.write("myPhotoPath", localfile);
						mHandler.postDelayed(new Runnable(){
							public void run()
							{
								String localfile=mPref.read("myPhotoPath");
								Drawable photo = ImageUtil.loadBitmapSafe(localfile, 1);// alec
								if (photo != null) {
									((TextView)findViewById(R.id.my_photo_hint)).setVisibility(View.GONE);
									photoView.setImageDrawable(photo);
								}
							}
						}, 0);
					}
					else{
						Log.e("downloadAnyPhoto Error");
					}
				}
			}
		}
	};
	
	Runnable getPersonalInfo=new Runnable()
	{
		public void run()
		{
			int myidx = 0;
			try {
				myidx = Integer.valueOf(mPref.read("myID","0"), 16);
			}catch(Exception e){}
			
			if (myidx>0)
			{
				int count=0;
				String Return="";
				String password=mPref.read("password", "1111");
				do {
					try{
						MyNet net = new MyNet(ProfileActivity.this);
						int id1 = myidx;
						String pw1 = URLEncoder.encode(password,"UTF-8");
						Return= net.doPostHttps("profile_read.php",
								"idx="+id1 +
								"&password="+pw1,null);
					}catch(Exception e){
						profileError(1);
						finish();
						return;
					}
					if (Return.length()!=0 && !Return.startsWith("Error"))//no network
						break;
				} while (++count < 3);
			
				if (Return.startsWith("OK"))
				{
					String [] itms=Return.split(",");
					if (itms.length>2)
					{
						String nickname=null;
						try {
							nickname = URLDecoder.decode(itms[1], "UTF-8");
							mPref.write("myNickname", nickname);
						} catch (UnsupportedEncodingException e) {
							profileError(2);
							finish();
							return;
						}
						
						String email=null;
						try {
							email = URLDecoder.decode(itms[2], "UTF-8");
							mPref.write("email", email);
						} catch (UnsupportedEncodingException e) {
							profileError(3);
							finish();
							return;
						}

//						String rpassword = null;
//						try {
//							if (!(itms[3] == null)) {
//								rpassword = URLDecoder.decode(itms[3], "UTF-8");
//								mPref.write("passworduser", rpassword);
//							}
//						} catch (UnsupportedEncodingException e) {
//						}
						
						mHandler.post(showProfileInfo);
					}
				} else {
					mHandler.post(dismissProgress);
				}
			} else {
				mHandler.post(dismissProgress);
			}
		}
	};
	
	Runnable showProfileInfo=new Runnable()
	{
		public void run()
		{
			String nickname=mPref.read("myNickname");
    		if (nickname!=null)
    		{
    			if (nickname.equals(EnterWithoutRegister.emailshort)) {  //tml*** login fix
    				nickname = "";
    			}
    			((EditText)findViewById(R.id.nickname)).setText(nickname);
    		}

    		String email=mPref.read("email");
    		if (nickname!=null)
    		{
    			if (email.equals(EnterWithoutRegister.email)) {  //tml*** login fix
    				email = "";
    			}
    			((EditText)findViewById(R.id.email)).setText(email);
    		}
    		//tml|sw*** account php
    		int count=0;
			String Return="";
			//tml*** china ip
			String domain = AireJupiter.myAcDomain_default;
			if (AireJupiter.getInstance() != null) {
				domain = AireJupiter.getInstance().getIsoDomain();
			}
			do {
				try {
					MyNet net = new MyNet(ProfileActivity.this);
					String myidx2 = URLEncoder.encode(mPref.read("myID", "0"), "UTF-8");
					Return = net.doAnyPostHttps("https://" + domain + "/webcall/acphp/check_account.php",
							"idx=" + myidx2);
				} catch (Exception e) {
					Log.e("check acc php ohuh> " + e.getMessage());
					mPref.write("accphpdone", false);
					profileError(5);
					finish();
					return;
				}
				if (Return.length() != 0 && !Return.startsWith("Error")) {
					break;
				}
			} while (++count < 3);
			
			if (Return.startsWith("success")) {
				mPref.write("accphpdone", true);
			}
			
    		if (mPref.readBoolean("accphpdone", false)) {
    			((EditText) findViewById(R.id.password1)).setText(defaultPW);
    			((EditText) findViewById(R.id.password2)).setText(defaultPW);
				((EditText) findViewById(R.id.password1)).setHint(null);
				((EditText) findViewById(R.id.password2)).setHint(null);
				((EditText) findViewById(R.id.password1)).setEnabled(false);
				((EditText) findViewById(R.id.password2)).setEnabled(false);
    		}
    		//***tml
            mHandler.post(dismissProgress);
		}
	};
	
	Runnable finishProfile=new Runnable()
	{
		public void run()
		{
            mHandler.post(popupProgressDialog);
			if (photoChanged && photoPath!=null)
    		{
    			mPref.write("myPhotoPath", photoPath);
    			mPref.write("myPhotoUploaded", false);
    			Intent it = new Intent(Global.Action_InternalCMD);
    			it.putExtra("Command", Global.CMD_UPLOAD_PROFILE_PHOTO);
    			sendBroadcast(it);
    		}
    		
    		boolean male=((RadioButton)findViewById(R.id.male)).isChecked();
    		mPref.write("myGender",male?"male":"female");
    		
    		String email=((EditText)findViewById(R.id.email)).getText().toString();
    		if (email!=null)
    		{
    			email=email.trim();
    			
        		if (email.length()>5 && RegisterActivity.check_email_syntax(email))
        		{
        			mPref.write("email", email);
        		}
        		else
        		{
        	    	Intent int2 = new Intent(getApplicationContext(), CommonDialog.class);
        	    	int2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
        	    			| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        	    			| Intent.FLAG_ACTIVITY_SINGLE_TOP);
        	    	int2.putExtra("msgContent", getString(R.string.email_invalid));
        	    	int2.putExtra("numItems", 1);
        	    	int2.putExtra("ItemCaption0", getString(R.string.done));
        	    	int2.putExtra("ItemResult0", -1);
        	    	startActivity(int2);
                    mHandler.post(dismissProgress);
        	    	return;
        		}
    		}
    		
    		String nickname=((EditText)findViewById(R.id.nickname)).getText().toString();
    		if (nickname!=null)
    		{
        		nickname=nickname.trim();
        		boolean chinese=nickname.toLowerCase().equals(nickname.toUpperCase());
        		
        		if (nickname.length()>=(chinese?2:6))
        		{
        			mPref.write("ProfileCompleted", true);
        			mPref.write("myNickname", nickname);
        			
					Intent it = new Intent(Global.Action_InternalCMD);
					it.putExtra("Command", Global.CMD_UPDATE_MY_NICKNAME);
					sendBroadcast(it);

					mPref.write("emailuploaded", false);
					Intent it2 = new Intent(Global.Action_InternalCMD);
					it2.putExtra("Command", Global.CMD_UPLOAD_PROFILE_EMAIL);
					sendBroadcast(it2);
					
					//tml|sw*** account php/
//        			Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
//        			startActivity(intent);
//        			finish();
        		}
        		else
        		{
        	    	Intent int2 = new Intent(getApplicationContext(), CommonDialog.class);
        	    	int2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
        	    			| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        	    			| Intent.FLAG_ACTIVITY_SINGLE_TOP);
        	    	int2.putExtra("msgContent", getString(R.string.nickname_invalid));
        	    	int2.putExtra("numItems", 1);
        	    	int2.putExtra("ItemCaption0", getString(R.string.done));
        	    	int2.putExtra("ItemResult0", -1);
        	    	startActivity(int2);
                    mHandler.post(dismissProgress);
					return;  //tml|sw*** account php/
        		}
    		}
    		
    		//tml|sw*** account php
			WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			MyNet myNet = new MyNet (ProfileActivity.this);
    		String password1 = ((EditText) findViewById(R.id.password1)).getText().toString();
    		String password2 = ((EditText) findViewById(R.id.password2)).getText().toString();

    		if (password1 != null && password2 != null) {
    			String returnUnique = "";
    			if (!mPref.readBoolean("accphpdone", false)) {
    	    		try {
    	    			int count = 0;
    					String mynick = URLEncoder.encode(nickname, "UTF-8");
    					String myemail = URLEncoder.encode(email, "UTF-8");
    					//tml*** china ip
    					String domain = AireJupiter.myAcDomain_default;
    					if (AireJupiter.getInstance() != null) {
    						domain = AireJupiter.getInstance().getIsoDomain();
    					}
    	    			do {
    						returnUnique = myNet.doAnyPostHttps("https://" + domain + "/webcall/acphp/check_email_name.php",
    								"nickname=" + mynick
    								+ "&email=" + myemail);
    						
    						if (returnUnique.equals("success")) {
    							break;
    						}
    						count++;
    						MyUtil.Sleep(100);
    					} while (count < 3);
    				} catch (Exception e) {
    					Log.e("acc php0 ohuh> " + e.getMessage());
    					profileError(6);
    					finish();
    					return;
    				}
    			} else {
    				returnUnique = "ok";
    			}
    			
	    		if (returnUnique.equals("failed") || returnUnique.startsWith("Error") || returnUnique.length() == 0) {
        	    	Intent int3 = new Intent(getApplicationContext(), CommonDialog.class);
        	    	int3.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
        	    			| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        	    			| Intent.FLAG_ACTIVITY_SINGLE_TOP);
        	    	int3.putExtra("msgContent", getString(R.string.acc_unique));
        	    	int3.putExtra("numItems", 1);
        	    	int3.putExtra("ItemCaption0", getString(R.string.done));
        	    	int3.putExtra("ItemResult0", -1);
        	    	startActivity(int3);
	    		} else if (password1.length() < 6) {
        	    	Intent int3 = new Intent(getApplicationContext(), CommonDialog.class);
        	    	int3.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
        	    			| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        	    			| Intent.FLAG_ACTIVITY_SINGLE_TOP);
        	    	int3.putExtra("msgContent", getString(R.string.password_makeerror));
        	    	int3.putExtra("numItems", 1);
        	    	int3.putExtra("ItemCaption0", getString(R.string.done));
        	    	int3.putExtra("ItemResult0", -1);
        	    	startActivity(int3);
    			} else if (!password1.equals(password2)) {
        	    	Intent int3 = new Intent(getApplicationContext(), CommonDialog.class);
        	    	int3.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
        	    			| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        	    			| Intent.FLAG_ACTIVITY_SINGLE_TOP);
        	    	int3.putExtra("msgContent", getString(R.string.passwords_dont_match));
        	    	int3.putExtra("numItems", 1);
        	    	int3.putExtra("ItemCaption0", getString(R.string.done));
        	    	int3.putExtra("ItemResult0", -1);
        	    	startActivity(int3);
    			} else {
    				if (!mPref.readBoolean("accphpdone", false)) {
        				mPref.write("passworduser", password1);
        				
    					try {
    						int count = 0;
    						String macaddr = wifiMgr.getConnectionInfo().getMacAddress().replace(":", "");
    						macaddr = mPref.read("macPrefix1", "1111") + macaddr;
    						macaddr = URLEncoder.encode(macaddr, "UTF-8");
    						String mynick = URLEncoder.encode(nickname, "UTF-8");
    						String myname = "ac" + macaddr;
    						myname = URLEncoder.encode(myname, "UTF-8");
    						String myidx = URLEncoder.encode(mPref.read("myID", "0"), "UTF-8");
    						String mypw = URLEncoder.encode(password1, "UTF-8");
    						String myprod = URLEncoder.encode("ac-100", "UTF-8");
    						String myemail = URLEncoder.encode(email, "UTF-8");
    						String Return = "";
    						boolean accphpdone = true;
    						boolean nonetwork = false;
    						//tml*** china ip
    						String domain = AireJupiter.myAcDomain_default;
    						if (AireJupiter.getInstance() != null) {
    							domain = AireJupiter.getInstance().getIsoDomain();
    						}
    					
    						do {
//    							String mycountry = myNet.doPost("getiso.php", "", null);
//    							mycountry = mycountry.substring(mycountry.lastIndexOf("=") + 1, mycountry.length());
//    							mycountry = URLEncoder.encode("","UTF-8");
    							Return = myNet.doAnyPostHttps("https://" + domain + "/webcall/acphp/create_entry.php",
    									"nickname=" + mynick
    									+ "&username=" + myname
    									+ "&password=" + mypw
    									+ "&idx=" + myidx
    									+ "&product=" + myprod
    									+ "&email=" + myemail
    									+ "&address=" + macaddr);

    							MyUtil.Sleep(500);
    							if (Return.startsWith("success")) {
    								break;
    							} else if (Return.toLowerCase().contains("error")) {
    								nonetwork = true;
    								break;
    							}
    							count++;
    						} while (count < 3);
    						if (count == 3 || nonetwork) {
    							accphpdone = false;
    	    					Log.e("PREG create_entry >>> FAIL (connection)");
    						}
    						mPref.write("accphpdone", accphpdone);
    					} catch (Exception e) {
    						Log.e("acc php1 ohuh> " + e.getMessage());
    						profileError(7);
    						finish();
    						return;
    					}
    				} else {
    					Log.e("PREG create_entry >>> DONE");
    				}
					
    				if (mPref.readBoolean("accphpdone", false)) {
            			Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            			startActivity(intent);
            			finish();
    				} else {
    					profileError(8);
    				}
        		
        		}
				//***tml
    		}
            mHandler.post(dismissProgress);
		}
	};
	/*
	 * REGISTRATION/LOGIN ERROR! Please try later.
	 * Error Codes : Cause = Solution
	 * 0 : default, this code should never show
	 * 1 : profile_read.php, net or server issue = check net and php server & restart airetalk
	 * 2 : profile_read.php returned bad nickname = check net and php server & restart airetalk
	 * 3 : profile_read.php returned bad email = check net and php server & restart airetalk
	 * 5 : check_account.php, net or server issue = check net and php server & restart airetalk
	 * 6 : check_email_name.php, net or server issue = check net and php server & restart airetalk
	 * 7 : create_entry.php, net or server issue = check net and php server & restart airetalk
	 * 8 : account creation incomplete = check net and all phps & restart airetalk
	 */
	private void profileError(int code) {
    	Intent int3 = new Intent(getApplicationContext(), CommonDialog.class);
    	int3.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
    			| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
    			| Intent.FLAG_ACTIVITY_SINGLE_TOP);
    	int3.putExtra("msgContent", getString(R.string.registrationerror)
    			+ "\nError Code " + code);
    	int3.putExtra("numItems", 1);
    	int3.putExtra("ItemCaption0", getString(R.string.done));
    	int3.putExtra("ItemResult0", -1);
    	startActivity(int3);
    	mHandler.post(dismissProgress);
	}
	
	Runnable showTooltip=new Runnable(){
		public void run()
		{
			Intent it=new Intent(ProfileActivity.this,Tooltip.class);
	        it.putExtra("Content", getString(R.string.help_complete_profile));
		    startActivity(it);
		}
	};
	
	Runnable doSearchFacebookFriends=new Runnable()
	{
		public void run()
		{
			Intent it=new Intent(ProfileActivity.this, FacebookSearch.class);
			startActivityForResult(it, 70);
		}
	};
	
	@Override
	protected void onPause() {
    	mHandler.removeCallbacks(showTooltip);
//    	MobclickAgent.onPause(this);
    	super.onPause();
    }
	
	private void onPickPictureOption()
	{
		final CharSequence[] items = {
				getResources().getString(R.string.photo_gallery),
				getResources().getString(R.string.takepicture)};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(this.getResources().getString(R.string.choose_photo_source)); 
		builder.setItems(items, new DialogInterface.OnClickListener() {     
			public void onClick(DialogInterface dialog, int item) {         
				if (item==0)
					onPickPicture();
				else if (item==1)
					onTakePicture();
				dialog.dismiss();
			} 
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {     
			public void onClick(DialogInterface dialog, int item) {         
				
			} 
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void onPickPicture() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		String title = getResources().getString(R.string.choose_photo_source);
		startActivityForResult(Intent.createChooser(intent, title), 1);
	}
	
	private void onTakePicture()
	{
		try{
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			photoPath=Global.SdcardPath_sent + "tmp.jpg";
			intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(photoPath)));
			startActivityForResult(intent, 3);
		}catch(Exception e){
			Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) { 
	    if (requestCode == 70)
	    {
	    	if (resultCode == Activity.RESULT_OK) {
	    		mHandler.post(finishProfile);
	    	}
	    }
	    else if (requestCode == 1 || requestCode == 3 || requestCode == 7) 
		{
			if (resultCode == Activity.RESULT_OK) {
				if (requestCode == 7) {

					try {
						Drawable photo = ImageUtil.getBitmapAsRoundCorner(photoPath, 3, 10);// alec
						if (photo != null) {
							((TextView)findViewById(R.id.my_photo_hint)).setVisibility(View.GONE);
							photoView.setImageDrawable(photo);
							photoChanged = true;
						}
					} catch (Exception e) {
					}

				} else if (requestCode == 1) {
					if (data == null) return;
					try {
						int uid = Integer.valueOf(mPref.read("myID","0"), 16);
						String outFilename = Global.SdcardPath_sent + "myself_photo_" + uid + ".jpg";
						
						ResizeImage.ResizeXYFromStream(this, data, outFilename, PHOTO_SIZE, 100);

						Drawable photo = ImageUtil.loadBitmapSafe(outFilename, 1);// alec
						if (photo != null) {
							((TextView)findViewById(R.id.my_photo_hint)).setVisibility(View.GONE);
							photoView.setImageDrawable(photo);
							photoChanged = true;
						}

					} catch (Exception e) {
					}
				}
				else if (requestCode == 3) {				
					try {
						int uid = Integer.valueOf(mPref.read("myID","0"), 16);
						
						String outFilename = Global.SdcardPath_sent + "myself_photo_" + uid + ".jpg";
						ResizeImage.ResizeXY(this, photoPath, outFilename, PHOTO_SIZE, 100);
						
						String outFilename2 = Global.SdcardPath_inbox + "photo_" + uid + "b.jpg";
						ResizeImage.ResizeXY(this, photoPath, outFilename2, PHOTO_SIZE, 100);
						
						Drawable photo = ImageUtil.loadBitmapSafe(outFilename, 1);// alec
						if (photo != null) {
							((TextView)findViewById(R.id.my_photo_hint)).setVisibility(View.GONE);
							photoView.setImageDrawable(photo);
							photoChanged = true;
						}
					} catch (Exception e) {
					}
				}
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
//		MobclickAgent.onResume(this);
	}

	private ProgressDialog progressDialog;
	Runnable popupProgressDialog = new Runnable() {
		@Override
		public void run() {
			try{
				if (progressDialog == null)
					progressDialog = ProgressDialog.show(ProfileActivity.this, "", ProfileActivity.this.getString(R.string.in_progress), true, false);
			} catch (Exception e) {}
		}
	};
	
	Runnable dismissProgress = new Runnable() {
		@Override
		public void run() {
			try{
				if (progressDialog != null && progressDialog.isShowing())
					progressDialog.dismiss();
				if (progressDialog != null) progressDialog.cancel();
					progressDialog = null;
			} catch (Exception e) {
				progressDialog = null;
			}
		}
	};
}
