
package com.pingshow.amper;


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

		if (!MyUtil.CheckServiceExists(context, "com.pingshow.amper.AireJupiter") && prf.readBoolean("Registered", false)){
			Handler mHandler=new Handler();
			mHandler.postDelayed(new Runnable(){
				public void run()
				{
					Intent FafaServiceIntent = new Intent(Intent.ACTION_MAIN);
					FafaServiceIntent.setClass(mContext, AireJupiter.class);
					mContext.startService(FafaServiceIntent);
				}
			}, 10000);
		} else {
		}
	}
}
