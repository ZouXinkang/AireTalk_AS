package com.pingshow.airecenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.pingshow.util.MyUtil;

public class PowerOnReceiver extends BroadcastReceiver{
	static Context mContext;
	static Handler mHandler;
	@Override
	public void onReceive(Context context, Intent intent) {
		if (!MyUtil.CheckServiceExists(context, "com.pingshow.airecenter.AireJupiter"))
		{
			mContext=context;
			if (mHandler==null) mHandler=new Handler();
			mHandler.removeCallbacks(startServivceX);
			mHandler.postDelayed(startServivceX, 5000);
		}
	}
	
	static Runnable startServivceX=new Runnable(){
		public void run()
		{
			if (new MyPreference(mContext).readBoolean("AireRegistered"))
			{
				Intent itx=new Intent(mContext, AireJupiter.class);
				mContext.startService(itx);
			}
		}
	};
}
