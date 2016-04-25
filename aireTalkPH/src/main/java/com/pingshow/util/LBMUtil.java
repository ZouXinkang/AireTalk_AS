package com.pingshow.util;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.pingshow.amper.Log;

public class LBMUtil {
	public static void registerReceiver(Context context, BroadcastReceiver broadcastReceiver, IntentFilter intentFilter){
		LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter);
		Log.d("LBMUtil: registerReceiver--> broadcastReceiver: " + broadcastReceiver.getClass().getSimpleName());
	}
	
	public static void unregisterReceiver(Context context,BroadcastReceiver broadcastReceiver){
		LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
		Log.d("LBMUtil: unregisterReceiver--> broadcastReceiver: "+ broadcastReceiver.getClass().getSimpleName());
	}
	public static void sendBroadcast(Context context, Intent intent){
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
		Log.d("LBMUtil: sendBroadcast--> action: "+ intent.getAction());
	}
}
