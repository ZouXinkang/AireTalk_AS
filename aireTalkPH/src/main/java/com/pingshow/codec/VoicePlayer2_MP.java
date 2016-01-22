package com.pingshow.codec;

import java.io.File;
import java.io.IOException;

import com.pingshow.amper.Global;
import com.pingshow.amper.Log;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

//tml*** new vmsg
public class VoicePlayer2_MP {

	private MediaPlayer myMediaPlay;
	private String mFilePath;
	private Context mContext;
	
	public VoicePlayer2_MP(Context context, String filepath)
	{
		mContext = context;
		mFilePath = filepath;
		init(filepath);
	}

	private boolean init(String filepath)
	{
		try {
			myMediaPlay = new MediaPlayer();
			myMediaPlay.setDataSource(filepath);
		} catch (IllegalArgumentException e) {
			Log.e("VoicePlayer2_MP init1 !@#$ " + e.getMessage());
			myMediaPlay = null;
		} catch (SecurityException e) {
			Log.e("VoicePlayer2_MP init2 !@#$ " + e.getMessage());
			myMediaPlay = null;
		} catch (IllegalStateException e) {
			Log.e("VoicePlayer2_MP init3 !@#$ " + e.getMessage());
			myMediaPlay = null;
		} catch (IOException e) {
			Log.e("VoicePlayer2_MP init4 !@#$ " + e.getMessage());
			myMediaPlay = null;
		}
		
		if (myMediaPlay != null) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean start()
	{
		if (myMediaPlay != null) {
			try {
				myMediaPlay.prepare();
				myMediaPlay.start();
				Log.d("VoicePlayer2_MP start:" + mFilePath);
				
				myMediaPlay.setOnCompletionListener(new OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						Log.d("VoicePlayer2_MP complete");
						if (myMediaPlay != null) {
							if (mFilePath.contains("interphonevoice")) {
								Intent intent = new Intent();
								intent.setAction(Global.ACTION_PLAY_AUDIO);
								intent.putExtra("clear", 1);
								mContext.sendBroadcast(intent);
								
								File file = new File(mFilePath);
								if (file.exists()) {
									file.delete();
								}
							} else {
								Intent intent = new Intent();
								intent.setAction(Global.ACTION_PLAY_OVER);
								mContext.sendBroadcast(intent);
							}
						}
					}
				});
			} catch (Exception e) {
				Log.e("VoicePlayer2_MP start !@#$ " + e.getMessage());
				myMediaPlay = null;
			}
		}

		if (myMediaPlay != null) {
			return true;
		} else {
			return false;
		}
	}

	public boolean stop()
	{
		if (myMediaPlay != null) {
			try {
				myMediaPlay.stop();
				myMediaPlay.release();
				Log.d("VoicePlayer2_MP stop");
			} catch (IllegalStateException e) {
				Log.e("VoicePlayer2_MP stop !@#$ " + e.getMessage());
				myMediaPlay.release();
				myMediaPlay = null;
			}
		}
		
		System.gc();
		if (myMediaPlay != null) {
			myMediaPlay = null;
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isPlaying()
	{
		if (myMediaPlay != null) {
			boolean playing = myMediaPlay.isPlaying();
			return playing;
		} else {
			return false;
		}
	}
	
}
