
package com.pingshow.amper;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

@SuppressLint("Instantiatable")
abstract public class ServiceZ extends IntentService {
	abstract void doWakefulWork(Intent intent);
  
	public static final String LOCK_NAME_STATIC="com.pingshow.amper.ServiceZ";
	private static PowerManager.WakeLock lockStatic=null;
  
	public static void acquireStaticLock(Context context) {
		getLock(context).acquire();
	}
	
	public static void ReleaseStaticLock() {
		try{
			if (lockStatic!=null && lockStatic.isHeld())
			{
				lockStatic.release();
			}
		}catch(Exception e){}
	}
  
	synchronized private static PowerManager.WakeLock getLock(Context context) {
		if (lockStatic==null) {
			PowerManager mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
			lockStatic=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME_STATIC);
			lockStatic.setReferenceCounted(true);
		}
		return(lockStatic);
	}
  
	public ServiceZ(String name) {
		super(name);
	}
  
	@Override
	final protected void onHandleIntent(Intent intent) {
		try {
			doWakefulWork(intent);
		}catch(Exception e){}
	}
}