package com.pingshow.airecenter;

import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.pingshow.airecenter.map.MapViewLocation;
import com.pingshow.beehive.BeeHiveService;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;

public class ServiceNeverDie extends ServiceZ {
	static int timerCount=1;  //0 to 2, immediete query
	private Message msg;
	public ServiceNeverDie() {
	    super("ServiceNeverDie");
	}

	@Override
	void doWakefulWork(Intent intent) {
		
        neverSayNeverDie(ServiceNeverDie.this);  //tml|bj*** neverdie/
        
		timerCount++;
		Log.d("alarm.ServiceNeverDie #"+(timerCount-1));
		msg = new Message();
		if (MyUtil.CheckServiceExists(getApplicationContext(), "com.pingshow.airecenter.AireJupiter"))
		{
			if (AireJupiter.getInstance()!=null)
			{
				new Thread(new Runnable() {
					public void run() {
						AireJupiter.getInstance().do8mConnection();
						if ((timerCount%3)==0) //24 min ~ 28.5 min
							AireJupiter.getInstance().do30mConnection();
						
						try {
				            Class.forName("com.google.android.maps.MapActivity");
				        }catch (ClassNotFoundException e) {
				            return;
				        }catch (NoClassDefFoundError e){
							return;
						}
				        
						if (MapViewLocation.getInstance()==null)
						{
							long timeout=new MyPreference(ServiceNeverDie.this).readLong("SpeedupMapMonitor",0);
							if (new Date().getTime()<timeout)
							{
								Log.d("Still in traking period...");
								msg.arg1=1;
								mHandler.sendMessage(msg);
							}
							else if ((timerCount%18)==0){//~2.4 hours to 2.85 hours
								msg.arg1=1;
								mHandler.sendMessage(msg);
							}
						}
						
						ReleaseStaticLock();
					}
				}).start();
			}
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

	//tml|bj*** neverdie, reset all services
	public void neverSayNeverDie(Context context) {
		if (AireJupiter.getInstance()==null) {
			Log.e("SND AireJupiter.getInstance() is null, RESETTING");
			Intent vip0 = new Intent(context, BeeHiveService.class);
			context.stopService(vip0);
			Intent vip1 = new Intent(context, AireVenus.class);
			context.stopService(vip1);
			Intent vip2 = new Intent(context, AireJupiter.class);
			context.stopService(vip2);
			
			Intent vip00 = new Intent(context, AireJupiter.class);
			context.startService(vip00);
		}
	}
	//***tml
}