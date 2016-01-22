package com.pingshow.amper;

import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.pingshow.amper.map.MapViewLocation;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;

public class ServiceNeverDie extends ServiceZ {
	static int timerCount=2;  //0 to 2, immediete query
	private Message msg;
	public ServiceNeverDie() {
	    super("ServiceNeverDie");
	}

	@Override
	void doWakefulWork(Intent intent) {
		
		neverSayNeverDie(ServiceNeverDie.this);  //tml|bj*** neverdie/
		
		timerCount++;
		Log.d("ServiceNeverDie #"+(timerCount-2));
		msg = new Message();
		if (MyUtil.CheckServiceExists(ServiceNeverDie.this, "com.pingshow.amper.AireJupiter"))
		{
			if (AireJupiter.getInstance()!=null)
			{
				new Thread(new Runnable() {
					public void run() {
						AireJupiter.getInstance().do8mConnection();
						if ((timerCount%3)==0) //24 min ~ 28.5 min
							AireJupiter.getInstance().do30mConnection();
						
//						try {
//							Log.w("checkmap ? com.google.android.maps");
//				            Class.forName("com.google.android.maps.MapActivity");
//				        } catch (ClassNotFoundException e) {
//							Log.w("!@#$ ServiceNeverDie1 com.google.android.maps");
//				            return;
//				        } catch (NoClassDefFoundError e) {
//							Log.w("!@#$ ServiceNeverDie2 com.google.android.maps");
//							return;
//						} catch (Error e) {
//							Log.w("!@#$ ServiceNeverDie3 com.google.android.maps");
//							return;
//						}
						//tml*** check google map
						boolean hasGoogleMaps = MyUtil.hasGoogleMap(false, getApplicationContext(), "ServiceNeverDie");
						if (!hasGoogleMaps) {
							return;
						}
//				        
//						if (MapViewLocation.getInstance()==null)  //why ==null?
//						{
							long timeout = new MyPreference(ServiceNeverDie.this).readLong("SpeedupMapMonitor", 0);
							if (new Date().getTime() < timeout) {
								Log.d("Still in tracking period...");
								msg.arg1 = 1;
								mHandler.sendMessage(msg);
							} else if ((timerCount % 36) == 33) {  //~12 hours
								msg.arg1 = 1;
								mHandler.sendMessage(msg);
							}
//						}
						
						ReleaseStaticLock();
						
					}
				}).start();
			}
		} else {
			
		}
	}
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.arg1) {
			case 1:
				Log.d( "ServiceNeverDie.InitLocationMonitor......");
				if (AireJupiter.getInstance()!=null)
					AireJupiter.getInstance().refreshLocation();
				break;
			}
		};
	};

	//tml|bj*** neverdie
	public void neverSayNeverDie(Context context) {
		if (AireJupiter.getInstance()==null) {
			Log.e("AireJupiter is NULL");
			Intent vip1 = new Intent(context, AireVenus.class);
			context.stopService(vip1);
			Intent vip2 = new Intent(context, AireJupiter.class);
			context.stopService(vip2);
			
			Intent vip00 = new Intent(context, AireJupiter.class);
			context.startService(vip00);
		}
	}
}