package com.pingshow.amper;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.IBinder;

public class PlayService extends Service
{
	private MediaPlayer mdMediaPlayer = null;
	private int type;
	@Override
	public IBinder onBind(Intent intent) 
	{
		return null;
	}

	@Override
	public void onCreate() 
	{
		super.onCreate();
	}
 
	@Override
	public void onDestroy() 
	{
		if (mdMediaPlayer!=null)
		{
			mdMediaPlayer.stop();
			mdMediaPlayer.release();
			Log.e("PlayService MediaPlayer onDestroy");
		}
		super.onDestroy();
	}

	@Override
	public void onStart(Intent intent, int startId) 
	{
		if (intent==null || startId!=1) {
			stopSelf();
			return;
		}
		super.onStart(intent, startId);
		int interphoneType = intent.getIntExtra("interphoneType", -1);
		int resource=0;
		type = intent.getIntExtra("type", 0);
		if(interphoneType!=-1){
			//resource=R.raw.vex01+interphoneType;
		}else{
			resource=intent.getIntExtra("soundInCall", R.raw.termin);
		}
		
		try{
			mdMediaPlayer = MediaPlayer.create(this, resource);
			mdMediaPlayer.setOnCompletionListener(new OnCompletionListener() 
			{
				@Override
				public void onCompletion(MediaPlayer md) 
				{
					Intent intent = new Intent();
					intent.setAction(Global.ACTION_PLAY_AUDIO);
					intent.putExtra("clear", type);
					sendBroadcast(intent);
					stopSelf();
				}
			});
			mdMediaPlayer.setOnErrorListener(new OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					Log.d("playing Error");
					Intent intent = new Intent();
					intent.setAction(Global.ACTION_PLAY_AUDIO);
					intent.putExtra("clear", type);
					sendBroadcast(intent);
					stopSelf();
					return false;
				}
			});
			mdMediaPlayer.start();
		}catch(Exception e){
			Intent it = new Intent();
			it.setAction(Global.ACTION_PLAY_AUDIO);
			it.putExtra("clear", type);
			sendBroadcast(it);
			stopSelf();
		}
	}
}
