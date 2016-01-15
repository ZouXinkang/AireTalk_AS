package com.pingshow.airecenter;


import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.widget.ImageView;
import com.pingshow.airecenter.R;
import com.pingshow.beehive.ClientThread;

public class ScreenSharing extends Activity {
	static ScreenSharing instance;
	private ImageView mScreen;
	private Handler mHandler=new Handler();
	private WakeLock mWakeLock;
	static int empty=0;
	
	public static ScreenSharing getInstance()
	{
		return instance;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_sharing);
		
		mScreen=(ImageView)findViewById(R.id.screen);
		try{
			mScreen.setImageBitmap(ClientThread.videoImage);
		}catch(Exception e){}
		instance=this;
		mHandler.postDelayed(close, 6000);
		
		mHandler.postDelayed(refresh,33);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		if (mWakeLock.isHeld())	mWakeLock.release();
		reenableKeyguard();
	}
	
	private Runnable refresh=new Runnable(){
		public void run()
		{
			try{
				if (ClientThread.videoImage!=null)
				{
					mScreen.setImageBitmap(ClientThread.videoImage);
					ClientThread.videoImage=null;
					empty=0;
				}else
					empty++;
			}catch(Exception e){}
			
			mHandler.postDelayed(refresh,33);
		}
	};
	
	@Override
	public void onDestroy()
	{
		mHandler.removeCallbacks(close);
		mHandler.removeCallbacks(refresh);
		instance=null;
		super.onDestroy();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE,"ScreenSharing");
		mWakeLock.acquire();
		disableKeyguard();
	}
	
	long enabletime;
    KeyguardManager mKeyguardManager;
    KeyguardManager.KeyguardLock mKeyguardLock;
    boolean enabled;
    
	void disableKeyguard() {
		if (AmazonKindle.IsKindle()) return;
    	if (mKeyguardManager == null) {
	        mKeyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
	        mKeyguardLock = mKeyguardManager.newKeyguardLock("VideoCall_KeyGuard");
	        enabled = true;
    	}
		if (enabled) {
			mKeyguardLock.disableKeyguard();
			enabled = false;
			enabletime = SystemClock.elapsedRealtime();
		}
	}
	
	void reenableKeyguard() {
		if (AmazonKindle.IsKindle()) return;
		if (!enabled) {
			mKeyguardLock.reenableKeyguard();
			enabled = true;
		}
	}
	
	public void Destroy()
	{
		finish();
	}
	
	private Runnable close=new Runnable(){
		public void run()
		{
			if (empty>=100)
				finish();
			else
				mHandler.postDelayed(close, 6000);
		}
	};
	
	public void launchDefaultPlayer(String url)
	{
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);
	}
}
	
