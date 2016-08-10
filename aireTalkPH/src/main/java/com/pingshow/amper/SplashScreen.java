package com.pingshow.amper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.pingshow.amper.db.AireCallLogDB;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.AnnounceDB;
import com.pingshow.amper.db.GroupDB;
import com.pingshow.amper.db.RelatedUserDB;
import com.pingshow.amper.db.SmsDB;
import com.pingshow.amper.db.TimeLineDB;
import com.pingshow.amper.db.TimeLineFollowDB;
import com.pingshow.amper.db.TransactionDB;
import com.pingshow.amper.db.WTHistoryDB;
import com.pingshow.amper.map.LocationUpdate;
import com.pingshow.amper.register.BeforeRegisterActivity;
import com.pingshow.network.MyNet;
import com.pingshow.util.MyProfile;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Date;


public class SplashScreen extends Activity {
	
    private Thread mSplashThread;
    private MyPreference mPref;
    private boolean showSplash = false;
    private String mAddress = null;
	private MyProfile myProfile;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

    	mPref = new MyPreference(SplashScreen.this);
		myProfile = new MyProfile(this);
//		MyProfile.init(SplashScreen.this);
    	
    	long last = mPref.readLong("last_show_time", 0);
    	if (new Date().getTime() - last > 300000)//5 minutes
    		showSplash = true;
    	
    	//webcall
		Intent intent = getIntent();
		if (intent != null) {
			Uri data = intent.getData();
			if (data != null) {
				int ret = 0;
				String param = data.getHost();
				try {
					byte[] base64 = Base64.decode(param, Base64.NO_WRAP | Base64.URL_SAFE);
					String url = MyUtil.decryptTCPCmd(base64);
					String[] items = url.split("/");
					checkServiceX();
					if (items.length > 5)
						ret = startWebCall(items[1], items[0], items[2], items[4], items[5]);
				} catch (Exception e) {
					Log.e("load webcall !@#$ e.getMessage()");
				}

				if (ret == 1) {
					finish();
					return;
				} else {
					checkServiceX();
					return;
				}
			}
		}
		
		new Thread() { 
			@Override 
			public void run() { 
				copyAssetsToPhone("rootca.pem");
			};
		}.start();

		//tml*** update socketloc
		LocationUpdate location = new LocationUpdate(SplashScreen.this, mPref);
	    location.getMyLocFromIpAddress();
	    location.getMyRoamId();  //tml*** xcountry sip
	        
        //Start animating the image
    	if (showSplash)
    	{
        	setContentView(R.layout.splash_page);
        	
        	ImageView logo = (ImageView)findViewById(R.id.logo);
		    
		    AnimationSet as = new AnimationSet(false);
		    as.setInterpolator(new AccelerateInterpolator());
//		    ScaleAnimation ta = new ScaleAnimation(1.5f, 1, 1.5f, 1, 120, 46);
		    ScaleAnimation ta = new ScaleAnimation(1.5f, 1, 1.5f, 1, 140, 140);  //tml*** logo/
			ta.setDuration(1500);
			as.addAnimation(ta);
			as.setDuration(1500);
			logo.startAnimation(as);
			
		    //The thread to wait for splash screen events
	    	mSplashThread = new Thread() {
	    		@Override
	    		public void run() {
	    			
	    			checkServiceX();

	    			//tml*** shortcut update, versionCode
					if (rwVersionCode(1) < 2302) {
//    					boolean shortcut = mPref.readBoolean("shortcut2Created");
						boolean shortcut = myProfile.isShortcut2Created();
						if (shortcut) {
							setShortCut(false);
						}
						int getvCode = rwVersionCode(2);
						Log.i("version " + getvCode);
						setShortCut(true);
					}
					//***tml
	    			
	    			try {
	    				synchronized(this) {
	    					wait(3000);
	    				}
	    			}  catch(InterruptedException e) {
	    			}
	    			
	    			mPref.writeLong("last_show_time", new Date().getTime());
	    			
	    			startAmper();
	    		}
	    	};
	    	mSplashThread.start();
    	}
    	else
    	{
    		checkServiceX();
    		startAmper();
    	}
	}
	
	Runnable deleteUserData = new Runnable()
	{
		public void run()
		{
			try{
//				mPref.delect("myPhotoPath");
//				MyProfile.load().saveMyPhotoPath(null, true);
				//jack
				myProfile.saveMyPhotoPath(null,true);

				AireCallLogDB a=new AireCallLogDB(SplashScreen.this);
				a.open();
				a.deleteTable();
				a.close();
				
				AmpUserDB b=new AmpUserDB(SplashScreen.this);
				b.open();
				b.deleteTable();
				b.close();
				
				SmsDB f=new SmsDB(SplashScreen.this);
				f.open();
				f.deleteTable();
				f.close();
				
				AnnounceDB c=new AnnounceDB(SplashScreen.this);
				c.open();
				c.deleteTable();
				c.close();
				
				GroupDB d=new GroupDB(SplashScreen.this);
				d.open();
				d.deleteTable();
				d.close();
				
				RelatedUserDB e=new RelatedUserDB(SplashScreen.this);
				e.open();
				e.deleteTable();
				e.close();
				
				TransactionDB g=new TransactionDB(SplashScreen.this);
				g.open();
				g.deleteTable();
				g.close();
				
				WTHistoryDB h=new WTHistoryDB(SplashScreen.this);
				h.open();
				h.deleteTable();
				h.close();
				
				TimeLineDB t=new TimeLineDB(SplashScreen.this);
				t.open();
				t.deleteTable();
				t.close();
				
				TimeLineFollowDB tf=new TimeLineFollowDB(SplashScreen.this);
				tf.open();
				tf.deleteTable();
				tf.close();
				
			}catch(Exception e){}
		}
	};
	
	private void checkServiceX() {
		//alec*** roaming, I give up SIM card change... Comment this will bring some side effect
//		if (mPref.readBoolean("Registered"))
//		{
//			TelephonyManager tMgr =(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
//			String StoredSubscribeId=mPref.read("SubscribeId","");
//			String SubscribeId=tMgr.getSubscriberId();
//			
//			if (SubscribeId!=null && !SubscribeId.equals(StoredSubscribeId) && !StoredSubscribeId.equals("0000000"))//User changed the SIM card
//			{
//				mPref.write("Registered",false);
//				mPref.delect("ProfileCompleted");
//				if (MyUtil.CheckServiceExists(this, "com.pingshow.amper.AireJupiter"))//Stop ServiceX
//		        {
//		    		Intent itx=new Intent(this, AireJupiter.class);
//		    		stopService(itx);
//		    	}
//				
//				mPref.delect("myPhoneNumber");
//				mPref.delect("mySipServer");
//				mPref.delect("password");
//				mPref.delect("myID");
//				mPref.delect("Credit");
//				mPref.delect("firstEnter");
//				
//				new Thread(deleteUserData).start();
//			}
//		}
		//***alec
//		boolean regis = mPref.readBoolean("Registered", false);
//		boolean regis = MyProfile.load().isRegistered();
		//jack
		boolean regis = myProfile.isRegistered();
		if (regis) {
			if (!MyUtil.CheckServiceExists(SplashScreen.this, "com.pingshow.amper.AireJupiter")) {
	    		Intent x = new Intent(SplashScreen.this, AireJupiter.class);
	    		startService(x);
	    	}
		} else {
			if (MyUtil.CheckServiceExists(SplashScreen.this, "com.pingshow.amper.AireJupiter")) {
	    		Intent x = new Intent(SplashScreen.this, AireJupiter.class);
	    		stopService(x);
	    	}
		}
	}
	
	private void startAmper()
	{
		Intent intent = new Intent();
//		boolean regis = mPref.readBoolean("Registered", false);
//		boolean prof = mPref.readBoolean("ProfileCompleted", false);
//		boolean enter1 = mPref.readBoolean("firstEnter", false);

//		boolean regis = MyProfile.load().isRegistered();
//		boolean prof = MyProfile.load().isProfileComplete();
//		boolean enter1 = MyProfile.load().isFirstEnter();
		//jack
		boolean regis = myProfile.isRegistered();
		boolean prof = myProfile.isProfileComplete();
		boolean enter1 = myProfile.isFirstEnter();

		Log.d("REG startAmper " + regis + prof + enter1);
		
		if (!regis) {
			intent.setClass(SplashScreen.this, BeforeRegisterActivity.class);
		} else if (!prof) {
			Log.d("SplashScreen  !prof");
			intent.setClass(SplashScreen.this, ProfileActivity.class);
		} else if (enter1) {
			Log.d("SplashScreen  enter1");
			intent.setClass(SplashScreen.this, ProfileActivity.class);
		} else {
			intent.setClass(SplashScreen.this, UsersActivity.class);
		}
		
		startActivity(intent);
		finish();
	}
	
	private int startWebCall(String func, String callee, String name, String sipId, String refURL)
	{
		Intent intent = new Intent();
		callee = MyTelephony.attachPrefix(this, callee);
//		boolean regis = mPref.readBoolean("Registered", false);
//		boolean prof = mPref.readBoolean("ProfileCompleted", false);

//		boolean regis = MyProfile.load().isRegistered();
//		boolean prof = MyProfile.load().isProfileComplete();

		//jack
		boolean regis = myProfile.isRegistered();
		boolean prof = myProfile.isProfileComplete();
		
		if (!regis) {
			intent.setClass(SplashScreen.this, BeforeRegisterActivity.class);
		} else if (!prof) {
			intent.setClass(SplashScreen.this, ProfileActivity.class);
		} else {
			if (func.equals("sip"))
			{
				String key = "sip_"+callee;
				long time = mPref.readLong(key,0);
				long now = new Date().getTime();
				String Return = "success=true";
				if (now > time)
				{
					try {
						int count = 0;
						//tml*** china ip
						String domain = AireJupiter.myAcDomain_default;
						if (AireJupiter.getInstance() != null) {
							domain = AireJupiter.getInstance().getIsoDomain();
						}
						do {
							MyNet net = new MyNet(SplashScreen.this);
							Return = net.doAnyPostHttps("http://" + domain + "/test/loginx.php", 
									"action=check_username&username="+URLEncoder.encode(callee, "UTF-8"));
							if (Return.startsWith("success="))
								break;
							MyUtil.Sleep(500);
						} while (++count < 3);
					} catch (Exception e) {
					}
					
					if (Return.startsWith("success=true"))
						mPref.writeLong(key, new Date().getTime() + 86400000);
				}
				
				if (Return.startsWith("success=true"))
				{
					mAddress = callee;
					
					new Thread(new Runnable() {
						@Override
						public void run() {
							boolean success = false;
							String localfile = Global.SdcardPath_inbox + mAddress + ".png";
							if (!new File(localfile).exists())
							{
								String remotefile = "http://74.3.165.158/onair/profiles/ads/" + mAddress.substring(1) + ".png";
								try {
									int count = 0;
									do {
										MyNet net = new MyNet(SplashScreen.this);
										success = net.anyDownload(remotefile, localfile);
										if (success) break;
										MyUtil.Sleep(500);
									} while (++count < 3);
								} catch (Exception e) {
								}
								
								if (success) {
									Intent it=new Intent(Global.Action_Sip_Photo_Download_Complete);
									sendBroadcast(it);
								}
							}
						}
					}).start();
					
					mPref.write("referenceURL",refURL);
					
					try {
						int index = Integer.parseInt(sipId);
						if (index >= 0 && index < 30)
						{
							mPref.write("aireSipAcount", mPref.read("aireSipAcount" + index, "aire"));
							mPref.write("aireSipPassowrd", mPref.read("aireSipPassowrd" + index, "aire"));
							mPref.write("aireSipServer", mPref.read("aireSipServer" + index, "127.0.0.1"));
						}
					} catch (Exception e) {
					}
					AireVenus.setCallType(AireVenus.CALLTYPE_WEBCALL);
					MakeCall.SipCall(SplashScreen.this, callee, name, false);
				}
				else
				{
					Builder MyAlertDialog = new AlertDialog.Builder(this);
					MyAlertDialog.setTitle(R.string.app_name);
					MyAlertDialog.setMessage(R.string.webcall_not_valid);
					MyAlertDialog.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
					      public void onClick(DialogInterface dialog, int which) {
					        finish();
					    }});
					MyAlertDialog.show();
					return 0;
				}
			}
			else
			{
				MakeCall.Call(SplashScreen.this, callee, false);
			}
			return 1;
		}
		
		startActivity(intent);
		return 1;
	}
	
	@Override
	public void onBackPressed()
	{
		return;
	}
	
	private void setShortCut(boolean mode) 
	{
//		mPref.write("shortcut2Created", true);
//		MyProfile.load().okShortcut2Created();
		//jack
		myProfile.okShortcut2Created();

		Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.setClassName(this.getPackageName(), "com.pingshow.amper.SplashScreen");
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
        intent.putExtra("duplicate", false);//alec
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.drawable.aire_icon_3s));
        //tml*** shortcut update
        if (mode) {
            intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        } else {
            intent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
        }
        sendBroadcast(intent);
	}
	
	private void copyAssetsToPhone(final String filename)
	{
		File file = new File(getFilesDir(), filename);
		Log.d("copyAssetsToPhone " + file.getAbsolutePath());
		if (file.exists() && file.length() > 0) {
			Log.d("config file exist, no copy");
		} else {
			new Thread() {
				public void run() {
					try {
						AssetManager am = getAssets();
						InputStream is = am.open(filename);
						
						File file = new File(getFilesDir(), filename);
						FileOutputStream fos = new FileOutputStream(file);
						int len = 0;
						byte[] buffer = new byte[1024];
						while ((len = is.read(buffer)) != -1) {
							fos.write(buffer, 0, len);
						}
						fos.close();
						is.close();
					} catch (Exception e) {
						Log.e("copyAssetsToPhone !@#$ " + e.getMessage());
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(getApplicationContext(), "Config asset copy failed", Toast.LENGTH_SHORT).show();
							}
						});
					}
				};
			}.start();
		}
	}

	//tml*** shortcut update
	public int rwVersionCode (int mode)
	{
		int versionCode = 0;
		try {
			versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
			if (mode == 1) {  //read
//				versionCode = mPref.readInt("versionCode", 0);
//				versionCode = MyProfile.load().getMyVersionCode();
				//jack
				versionCode = myProfile.getMyVersionCode();
			} else if (mode == 2) {  //write
//				mPref.write("versionCode", versionCode);
//				MyProfile.load().saveMyVersionCode(versionCode, false);
				//jack
				myProfile.saveMyVersionCode(versionCode, false);
			}
		} catch (NameNotFoundException e) {
			Log.e("rwVersionCode PKG !@#$ " + e.getMessage());
		}
		return versionCode;
	}
	
}
