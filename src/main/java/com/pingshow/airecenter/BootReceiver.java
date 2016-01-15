
package com.pingshow.airecenter;


import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.pingshow.util.MyUtil;


public class BootReceiver extends BroadcastReceiver {
	private Context mContext;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		mContext=context;
		MyPreference prf=new MyPreference(context);
		prf.writeArray("shared_friends", new ArrayList<String>());
		
		Log.e("aloha! AMP BootReceiver! " + prf.readBoolean("AireRegistered", false));
		if (!MyUtil.CheckServiceExists(context, "com.pingshow.airecenter.AireJupiter") && prf.readBoolean("AireRegistered", false)){
			Log.e("aloha! AMP BootReceiver! GO!");
			Handler mHandler=new Handler();
			mHandler.postDelayed(new Runnable(){
				public void run()
				{
					Intent FafaServiceIntent = new Intent(Intent.ACTION_MAIN);
					FafaServiceIntent.setClass(mContext, AireJupiter.class);
					mContext.startService(FafaServiceIntent);
				}
			}, 7000);
		} else {
			Log.e("aloha! AMP BootReceiver! ALREADY EXISTS!");
		}
	}
}
