package com.pingshow.airecenter;

import android.app.Application;
import android.content.Context;

import com.pingshow.util.CrashHandler;
import com.pingshow.util.WriteLogs;

public class AireApp extends Application {
	public static Context context;

	@Override
	public void onCreate() {
		super.onCreate();
		CrashHandler instance = CrashHandler.getInstance();
		instance.init(this);
		WriteLogs.getInstance(this).start();
		context = getApplicationContext();
	}
}
