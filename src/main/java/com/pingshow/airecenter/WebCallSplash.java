package com.pingshow.airecenter;

import java.io.File;
import java.net.URLEncoder;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.register.BeforeRegisterActivity;
import com.pingshow.network.MyNet;
import com.pingshow.network.NetInfo;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;

@SuppressLint("NewApi")
public class WebCallSplash extends Activity{
	
	float mDensity = 1.f;
	private MyPreference mPref;
	public Handler mHandler = new Handler();
	private boolean largeScreen=false;
	private String mAddress;
	private boolean Canceled=false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webcall_dialog);
		this.overridePendingTransition(R.anim.appear, R.anim.disappear);
		
		mDensity = getResources().getDisplayMetrics().density;
		largeScreen=(findViewById(R.id.large)!=null);
		
		WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
	    if (!largeScreen)
	    	lp.width=(int)(320.*mDensity);
		lp.dimAmount = 0.3f;
		getWindow().setAttributes(lp);
		
		mPref=new MyPreference(getApplicationContext());
		
		checkServiceX();
		
		((Button)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				Canceled=true;
				finish();
			}
		});
		
		NetInfo ni = new NetInfo(this);
		if (!ni.isConnected())
		{
			((TextView)findViewById(R.id.status_label)).setText(R.string.nonetwork);
			return;
		}
		
		mHandler.postDelayed(handleWebCall, 1000);
	}
	
	@Override
	protected void onDestroy() {
		mHandler.removeCallbacks(handleWebCall);
		super.onDestroy();
	}
	
	Runnable handleWebCall=new Runnable(){
		public void run()
		{
			Intent intent = getIntent();
			if (intent!=null && !Canceled)
			{
				Uri data = intent.getData();
			    if (data!=null && !Canceled)
			    {
			    	int ret=0;
			    	String param = data.getHost();
					try {
						byte[] base64=Base64.decode(param, Base64.NO_WRAP|Base64.URL_SAFE);
						String url=MyUtil.decryptTCPCmd(base64);
						String[] items = url.split("/");
				    	if (items.length>5)
				    		ret=startWebCall(items[1], items[0], items[2], items[4], items[5]);
					} catch (Exception e) {
						e.printStackTrace();
					} catch (NoClassDefFoundError e){
						ret=0;
						((TextView)findViewById(R.id.status_label)).setText(R.string.webcall_not_support);
					}

					if (ret==1)
					{
						finish();
						return;
					}
					else{
						return;
					}
			    }
	    	}
			else
				finish();
		}
	};
	
	void checkServiceX()
	{
		if (mPref.readBoolean("AireRegistered"))
		{
			TelephonyManager tMgr =(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			String StoredSubscribeId=mPref.read("SubscribeId","");
			String SubscribeId=tMgr.getSubscriberId();
			
			if (SubscribeId!=null && !SubscribeId.equals(StoredSubscribeId) && !StoredSubscribeId.equals("0000000"))//User changed the SIM card
			{
				mPref.write("AireRegistered",false);
				if (MyUtil.CheckServiceExists(getApplicationContext(), "com.pingshow.airecenter.AireJupiter"))//Stop ServiceX
		        {
		    		Intent itx=new Intent(this, AireJupiter.class);
		    		stopService(itx);
		    	}
			}
		}
		
		if (mPref.readBoolean("AireRegistered",false))
		{
			if (!MyUtil.CheckServiceExists(getApplicationContext(), "com.pingshow.airecenter.AireJupiter"))//Start ServiceX
	        {
	    		Intent x=new Intent(WebCallSplash.this, AireJupiter.class);
	    		startService(x);
	    	}
		}
		else {
			if (MyUtil.CheckServiceExists(getApplicationContext(), "com.pingshow.airecenter.AireJupiter"))//Stop ServiceX
	        {
	    		Intent x=new Intent(WebCallSplash.this, AireJupiter.class);
	    		stopService(x);
	    	}
		}
	}
	
	int startWebCall(String func, String callee, String name, String sipId, String refURL)
	{
		if (Canceled) return 0;
		
		Intent intent = new Intent();
		
		callee=MyTelephony.attachPrefix(this, callee);
		
		if (!mPref.readBoolean("AireRegistered",false))
			intent.setClass(WebCallSplash.this, BeforeRegisterActivity.class);
		else if (!mPref.readBoolean("ProfileCompleted",false))
			intent.setClass(WebCallSplash.this, ProfileActivity.class);
		else 
		{
			if (func.equals("sip"))
			{
				String key="sip_"+callee;
				String Return="success=true";
				
				if (!mPref.read("moodcontent", "--").equals("I love aire"))
				{
					long time=mPref.readLong(key,0);
					long now=new Date().getTime();
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
								MyNet net = new MyNet(WebCallSplash.this);
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
				}
				
				if (!Canceled && Return.startsWith("success=true"))
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
										MyNet net = new MyNet(WebCallSplash.this);
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
					if (!Canceled) {
						AireVenus.setCallType(AireVenus.CALLTYPE_WEBCALL);
						MakeCall.SipCall(WebCallSplash.this, callee, name, false);
					}
				}
				else{
					((TextView)findViewById(R.id.status_label)).setText(R.string.webcall_not_valid);
					return 0;
				}
			}
			else
			{
				AireVenus.setCallType(AireVenus.CALLTYPE_FAFA);
				MakeCall.Call(WebCallSplash.this, callee, false);
			}
			return 1;
		}
		startActivity(intent);
		return 1;
	}
}
