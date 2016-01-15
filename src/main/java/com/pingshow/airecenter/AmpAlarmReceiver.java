
package com.pingshow.airecenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class AmpAlarmReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (!isConnectInternet(context)) return;
		ServiceZ.acquireStaticLock(context);
		context.startService(new Intent(context, ServiceNeverDie.class));
		Log.d("AmpAlarmReceiver!");
	}
	
	public boolean isConnectInternet(Context context) {
       ConnectivityManager conManager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE );
       NetworkInfo networkInfo = conManager.getActiveNetworkInfo();
       if (networkInfo != null ){
    	   return networkInfo.isAvailable();
       }
       return false ;
	}
}