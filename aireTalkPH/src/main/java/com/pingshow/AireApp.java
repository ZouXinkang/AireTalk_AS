package com.pingshow;

import android.app.Application;
import android.content.Context;

public class AireApp extends Application {
	public static Context context;

	@Override
	public void onCreate() {
		super.onCreate();
		context = getApplicationContext();
	}
}
