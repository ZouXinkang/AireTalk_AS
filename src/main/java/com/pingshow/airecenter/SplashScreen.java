package com.pingshow.airecenter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.view.Gravity;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.map.LocationUpdate;
import com.pingshow.airecenter.register.BeforeRegisterActivity;
import com.pingshow.airecenter.register.EnterWithoutRegister;
import com.pingshow.network.MyNet;
import com.pingshow.network.NetInfo;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;

public class SplashScreen extends Activity {
	public static final boolean SUPPORT_ACC = false;
	final static boolean buildAsNoRegistration=true;
	
    private Thread mSplashThread;
    private MyPreference mPref;
    private boolean showSplash=false;
    private String mAddress=null;
    private boolean allowBack=false;
    private boolean goConference = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
//		MobclickAgent.setSessionContinueMillis(60000);

    	mPref=new MyPreference(SplashScreen.this);
    	
    	//tml|mj|wjx*** new mac test
//		register = new EnterWithoutRegister(this, mPref);
//		runOnUiThread(new Runnable() {
//			@Override
//			public void run() {
//				WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//				boolean wifion = wifiMgr.isWifiEnabled();
//				int wifistateN = wifiMgr.getWifiState();
//				boolean wifien = wifiMgr.setWifiEnabled(true);
//				String testmac = wifiMgr.getConnectionInfo().getMacAddress().replace(":", "");
////				String testmac = register.getMacAddress().replace(":", "");
//				Toast tst = Toast.makeText(getApplicationContext(),
//				"WLAN MAC = " + wifion + wifistateN + wifien + " " + testmac,
//				Toast.LENGTH_LONG);
//				tst.setGravity(Gravity.CENTER, 0, 0);
//				tst.show();
//			}
//		});
//		finish();
//		return;

    	if (buildAsNoRegistration)
    	{
    		boolean registered=mPref.readBoolean("AireRegistered", false);
    		long last=mPref.readLong("last_show_time",0);
        	if (!registered || new Date().getTime()-last>300000) //5 minutes
        		showSplash=true;
    	}
    	else
    		showSplash=false;

		Intent intent = getIntent();
		goConference = intent.getBooleanExtra("goConference", false);  //tml*** conference shortcut
		if (intent!=null)
		{
			Uri data = intent.getData();
		    if (data!=null)
		    {
		    	int ret=0;
		    	String param = data.getHost();
				try {
					byte[] base64=Base64.decode(param, Base64.NO_WRAP|Base64.URL_SAFE);
					String url=MyUtil.decryptTCPCmd(base64);
					String[] items = url.split("/");
			    	checkServiceX();
			    	if (items.length>5)
			    		ret=startWebCall(items[1], items[0], items[2], items[4], items[5]);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (ret==1)
				{
					finish();
					return;
				}
				else{
					checkServiceX();
					return;
				}
		    }
    	}
		//tml|vivid*** TLS
		new Thread(){ 
			@Override 
			public void run() { 
				copyAssetsToPhone("rootca.pem");
			}; 
		}.start();
		//***tml
    	
    	
//		String uploadType="daily";
//		long now=new Date().getTime();
//		long last_umeng_init=mPref.readLong("last_umeng_init", 0);
//		if (now-last_umeng_init>86400000)
//		{
//			MobclickAgent.updateOnlineConfig(this);
//			uploadType = MobclickAgent.getConfigParams(this, "uploadType");
//			Log.d("umeng uploadType "+uploadType);
//			mPref.writeLong("last_umeng_init", now);
//			
//			if(uploadType.equals("realtime"))
//				MobclickAgent.setDefaultReportPolicy(this, ReportPolicy.REALTIME);
//			else if(uploadType.equals("launch"))
//				MobclickAgent.setDefaultReportPolicy(this, ReportPolicy.BATCH_AT_LAUNCH);
//			else
//				MobclickAgent.setDefaultReportPolicy(this, ReportPolicy.DAILY);
//			
//			MobclickAgent.onError(this);
//		}
		
//		if (mPref.read("iso",null)==null)//get iso by IpAddress
//        {
//			LocationUpdate location=new LocationUpdate(SplashScreen.this, mPref);
// 	        location.getMyLocFromIpAddress();
//        }
    	
        // Start animating the image
    	if (showSplash)
    	{
        	setContentView(R.layout.splash_page);
    		//tml*** new get iso
    		new Thread() {
    			@Override
    			public void run() {
    				//tml*** update socketloc
    				LocationUpdate location = new LocationUpdate(SplashScreen.this, mPref);
    			    location.getMyLocFromIpAddress(false);
    			    location.getMyRoamId();  //tml*** xcountry sip
    			};
    		}.start();
        	
        	ImageView logo=(ImageView)findViewById(R.id.logo);
		    
        	AnimationSet as = new AnimationSet(false);
		    as.setInterpolator(new AccelerateInterpolator());
		    ScaleAnimation ta = new ScaleAnimation(1.5f, 1, 1.5f, 1, 120, 46);
			ta.setDuration(1500);
			as.addAnimation(ta);
			as.setDuration(1500);
			logo.startAnimation(as);
			
		    // The thread to wait for splash screen events
	    	mSplashThread =  new Thread(){
	    		@Override
	    		public void run(){
	    			
	    			checkServiceX();
	    			
	    			if (!mPref.readBoolean("shortcutCreated")) {
    					setShortCut();
	    			}
    					
	    			try {
	    				synchronized(this){
	    					wait(3000);
	    				}
	    			} 
	    			catch(InterruptedException ex){    				
	    			}
	    			
	    			mPref.writeLong("last_show_time",new Date().getTime());
	    			
	    			startAmper();
	    		}
	    	};
	    	mSplashThread.start();
    	}
    	else{
    		checkServiceX();
    		startAmper();
    	}
	}
	
	void checkServiceX()
	{
		if (mPref.readBoolean("AireRegistered",false))
		{
			if (!MyUtil.CheckServiceExists(getApplicationContext(), "com.pingshow.airecenter.AireJupiter"))//Start ServiceX
	        {
	    		Intent x=new Intent(SplashScreen.this, AireJupiter.class);
	    		startService(x);
	    	}
			else{
				if (AireJupiter.getInstance()==null)
				{
					Log.d("ServiceX exists, but no instance...");
					Intent x=new Intent(SplashScreen.this, AireJupiter.class);
		    		startService(x);
				}
			}
		}
		else {
			if (MyUtil.CheckServiceExists(getApplicationContext(), "com.pingshow.airecenter.AireJupiter"))//Stop ServiceX
	        {
	    		Intent x=new Intent(SplashScreen.this, AireJupiter.class);
	    		stopService(x);
	    	}
		}
	}
	
	EnterWithoutRegister register;
	
	@SuppressWarnings("unused")
	void startAmper()
	{
		Intent intent = new Intent();
		
		//tml*** support
		if (buildAsNoRegistration && !SUPPORT_ACC)
		{
			Log.d("tml buildAsNoRegistration");
			if (!mPref.readBoolean("AireRegistered",false))
			{
				Log.d("tml !AireRegistered");
				NetInfo ni=new NetInfo(this);
				if (ni.isConnected())
				{
					register=new EnterWithoutRegister(this, mPref);
					
					//tml|mj|wjx*** new mac
					if (register.isMACok().equals("null")) {
						allowBack = true;
						Intent it = new Intent(this, CommonDialog.class);
						it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						String title=getString(R.string.register_nowifi);
						it.putExtra("msgContent", title);
						it.putExtra("numItems", 1);
						it.putExtra("ItemCaption0", getString(R.string.done));
						it.putExtra("ItemResult0", CommonDialog.DONTSEARCHFACEBOOK);
						startActivity(it);
						finish();
						return;
					}
					//***tml
					
					//tml|mj|wjx*** new mac test
//					allowBack = true;
//					Intent it = new Intent(this, CommonDialog.class);
//					it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//					String title = register.isMACok();
//					it.putExtra("msgContent", title);
//					it.putExtra("numItems", 1);
//					it.putExtra("ItemCaption0", getString(R.string.done));
//					it.putExtra("ItemResult0", CommonDialog.DONTSEARCHFACEBOOK);
//					startActivity(it);
//					finish();
//					return;
					//***tml
					new Thread(registerDone).start();
				}
				else{
					allowBack=true;
					Intent it = new Intent(this, CommonDialog.class);
					it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					String title=getString(R.string.nonetwork);
					it.putExtra("msgContent", title);
					it.putExtra("numItems", 1);
					it.putExtra("ItemCaption0", getString(R.string.done));
					it.putExtra("ItemResult0", CommonDialog.DONTSEARCHFACEBOOK);
					startActivity(it);
				}
				return;
			}
//			else if (!mPref.readBoolean("ProfileCompleted",false))
//				intent.setClass(SplashScreen.this, ProfileActivity.class);
//			else if (mPref.readBoolean("firstEnter",false))
//				intent.setClass(SplashScreen.this, MainActivity.class);
//			else
//				intent.setClass(SplashScreen.this, ProfileActivity.class);
			else if (!mPref.readBoolean("ProfileCompleted",false)) {
				Log.d("tml !ProfileCompleted");
				intent.setClass(SplashScreen.this, ProfileActivity.class);
			} else if (mPref.readBoolean("firstEnter",false)) {
				Log.d("tml firstEnter");
//				intent.setClass(SplashScreen.this, MainActivity.class);
				if (mPref.readBoolean("accphpdone", false)) {  //tml|sw*** account php
					intent.setClass(SplashScreen.this, MainActivity.class);
				} else {
					intent.setClass(SplashScreen.this, ProfileActivity.class);
				}
			} else {
				if (mPref.readBoolean("accphpdone", false)) {  //tml|sw*** account php
					intent.putExtra("goConference", goConference);  //tml*** conference shortcut
					Log.d("tml okGO " + goConference);
					intent.setClass(SplashScreen.this, MainActivity.class);
				} else {
					intent.setClass(SplashScreen.this, ProfileActivity.class);
				}
			}
		}
		else
		{
			Log.d("tml !buildAsNoRegistration");
			if (!mPref.readBoolean("AireRegistered",false))
				intent.setClass(SplashScreen.this, BeforeRegisterActivity.class);
			else if (!mPref.readBoolean("ProfileCompleted",false))
				intent.setClass(SplashScreen.this, ProfileActivity.class);
			else if (mPref.readBoolean("firstEnter",false))
				intent.setClass(SplashScreen.this, MainActivity.class);
			else
				intent.setClass(SplashScreen.this, ProfileActivity.class);
		}
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(intent);
		
		finish();
	}
	
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			String Return = msg.obj.toString().toLowerCase(); 
			if (Return.startsWith("ok"))
			{
				Intent intent = new Intent();
				intent.setClass(SplashScreen.this, ProfileActivity.class);
				startActivity(intent);
				
				checkServiceX();
				
				finish();
			}
		}
	};
	
	public static int errorCode = 0;
	/*
	 * REGISTRATION/LOGIN ERROR! Please try later.
	 * Error Codes : Cause = Solution
	 * 0 : default, this code should never show
	 * 1 : getMacAddress is unavailable = restart wifi in settings & restart airetalk
	 * 2 : username creation error = restart wifi in settings & restart airetalk
	 * 3 : login_aire.php, net or server issue = check net and php server & restart airetalk
	 * 4 : same as above
	 * 5 : same as above
	 * 6 : preregister_aire.php, net or server issue = check net and php server & restart airetalk
	 * 7 : same as above
	 * 8 : register_aire.php, net or server issue = check net and php server & restart airetalk
	 * 9 : same as above
	 * 10 : same as above
	 */
	Runnable registerDone=new Runnable()
	{
		public void run()
		{
			int regcount = 0;
			boolean regok = true;
			if (AireJupiter.getInstance() != null) {
				AireJupiter.getInstance().airecenterAccess();  //tml*** dev control
			}
			
			while(!register.isDone())
			{
				MyUtil.Sleep(3000);
				//tml*** register timeout/error
				//database conflict, mac address, CHECK! login_aire.php and create_entry.php
				regcount++;
				Log.e("EnterWithoutRegister >>> while(!register.isDone()) " + regcount + "/15");

				boolean access = mPref.readBoolean("airecenterNoK", true);  //tml*** dev control
				if (!access) regcount = 66;
				
				if (regcount > 15) {
					if (regcount == 66) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast tst = Toast.makeText(getApplicationContext(),
								"AireTV ACCESS DENIED", Toast.LENGTH_LONG);
								tst.setGravity(Gravity.CENTER, 0, 0);
								tst.show();
							}
						});
					} else {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast tst = Toast.makeText(getApplicationContext(), 
										getString(R.string.registrationerror)
										+ "\nError Code " + Integer.toString(errorCode), Toast.LENGTH_LONG);
								tst.setGravity(Gravity.CENTER, 0, 0);
								tst.show();
							}
						});
					}
					regok = false;
					break;
				}
			}
			//tml*** register timeout/error/
			if (regok) {
				Message msg = new Message();
				msg.obj = "ok";
				mHandler.sendMessage(msg);
			} else {
				//gone bad
				Intent vip2 = new Intent(SplashScreen.this, AireJupiter.class);
				stopService(vip2);
				finish();
			}
		}
	};
	
	int startWebCall(String func, String callee, String name, String sipId, String refURL)
	{
		Intent intent = new Intent();
		
		callee=MyTelephony.attachPrefix(this, callee);
		
		if (!mPref.readBoolean("AireRegistered",false))
			intent.setClass(SplashScreen.this, BeforeRegisterActivity.class);
		else if (!mPref.readBoolean("ProfileCompleted",false))
			intent.setClass(SplashScreen.this, ProfileActivity.class);
		else 
		{
			if (func.equals("sip"))
			{
				String key="sip_"+callee;
				long time=mPref.readLong(key,0);
				long now=new Date().getTime();
				String Return="success=true";
				if (now>time)
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
							Return = net.doAnyPostHttps("https://" + domain + "/test/loginx.php", 
									"action=check_username&username="+URLEncoder.encode(callee, "UTF-8"));
							if (Return.startsWith("success="))
								break;
							MyUtil.Sleep(500);
						} while (++count < 3);
					}catch(Exception e){}
					
					if (Return.startsWith("success=true"))
						mPref.writeLong(key,new Date().getTime()+86400000);
				}
				
				if (Return.startsWith("success=true"))
				{
					mAddress=callee;
					
					new Thread(new Runnable(){
						@Override
						public void run() {
							boolean success = false;
							String localfile = Global.SdcardPath_inbox+mAddress+".png";
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
								}catch(Exception e){}
								
								if (success)
								{
									Intent it=new Intent(Global.Action_Sip_Photo_Download_Complete);
									sendBroadcast(it);
								}
							}
						}
					}).start();
					
					mPref.write("referenceURL",refURL);
					
					try{
						int index=Integer.parseInt(sipId);
						if (index>=0 && index<30)
						{
							mPref.write("aireSipAcount", mPref.read("aireSipAcount"+index, "aire"));
							mPref.write("aireSipPassowrd", mPref.read("aireSipPassowrd"+index, "aire"));
							mPref.write("aireSipServer", mPref.read("aireSipServer"+index, "127.0.0.1"));
						}
					}catch(Exception e){}
					AireVenus.setCallType(AireVenus.CALLTYPE_WEBCALL);
					MakeCall.SipCall(SplashScreen.this, callee, name, false);
				}
				else{
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
	public void onBackPressed() {
		if (allowBack)
			super.onBackPressed();
	}
	
	private void setShortCut()
	{
		mPref.write("shortcutCreated", true);
		Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.setClassName(this.getPackageName(), "com.pingshow.airecenter.SplashScreen");
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
        intent.putExtra("duplicate", false);//alec
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.drawable.aire_icon));
        intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        sendBroadcast(intent);
	}
	
	//tml|vivid*** TLS
	private void copyAssetsToPhone(final String filename) {
		// 检查数据库是否存在 如果不存在 拷贝
		File file = new File(getFilesDir(), filename);
		System.out.println(file.getAbsolutePath());
		if (file.exists() && file.length() > 0) {
			Log.d("config file exist,needn't copy");
		} else {
			new Thread() {
				public void run() {
					try {
						// 获取到资产管理器
						AssetManager am = getAssets();
						InputStream is = am.open(filename);
						// data/data/包名/files/
						File file = new File(getFilesDir(), filename);
						FileOutputStream fos = new FileOutputStream(file);
						int len = 0;
						byte[] buffer = new byte[1024];
						while ((len = is.read(buffer)) != -1) {
							fos.write(buffer, 0, len);
						}
						fos.close();
						is.close();
						// 在主线程里面更新 ui
//						runOnUiThread(new Runnable() {
//							@Override
//							public void run() {
//								Toast.makeText(getApplicationContext(),
//								"Copy Success", 0).show();
//							}
//						});
					} catch (Exception e) {
						e.printStackTrace();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(getApplicationContext(),
								"Copy Failed", 0).show();
							}
						});
					}
				};
			}.start();
		}
	}
	
	//tml*** shortcut update
	public int rwVersionCode (int mode) {
		int versionCode = 0;
		try {
			versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;  //get
			if (mode == 1) {
				versionCode = mPref.readInt("versionCode", 0);  //read
			} else if (mode == 2) {
				mPref.write("versionCode", versionCode);  //write
			}
		} catch (NameNotFoundException e) {
		}
		return versionCode;
	}
}
