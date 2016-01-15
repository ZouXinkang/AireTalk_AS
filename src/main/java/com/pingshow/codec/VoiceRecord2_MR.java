package com.pingshow.codec;

import com.pingshow.airecenter.Log;

import android.content.Context;
import android.media.MediaRecorder;

//tml*** new vmsg
public class VoiceRecord2_MR {

	private MediaRecorder myMediaRec;
	private String mFilePath;
	private Context mContext;
	
	public VoiceRecord2_MR (Context context, String filepath, int format, int encode, int samplingRate)
	{
		mContext = context;
		mFilePath = filepath;
		init(filepath, format, encode, samplingRate);
	}
	
	@SuppressWarnings("deprecation")
	private boolean init(String filepath, int format, int encode, int samplingRate) {
		if (format == -1 || format > 6) {
			format = MediaRecorder.OutputFormat.RAW_AMR;
		}
		if (encode == -1 || encode > 6) {
			encode = MediaRecorder.OutputFormat.DEFAULT;
		}
		try {
			myMediaRec = new MediaRecorder();
		    myMediaRec.setAudioSource(MediaRecorder.AudioSource.MIC);
		    myMediaRec.setOutputFormat(format);
		    myMediaRec.setAudioEncoder(encode);
		    myMediaRec.setAudioSamplingRate(samplingRate);
		    myMediaRec.setOutputFile(filepath);
		} catch (Exception e) {
			Log.e("VoiceRecord2_MR init !@#$ " + e.getMessage());
			myMediaRec = null;
		}

		if (myMediaRec != null) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean start()
	{
		if (myMediaRec != null) {
			try {
				myMediaRec.prepare();
			    myMediaRec.start();
				Log.d("VoiceRecord2_MR start:" + mFilePath);
			} catch (Exception e) {
				Log.e("VoiceRecord2_MR start !@#$ " + e.getMessage());
				myMediaRec = null;
			}
		}
		
		if (myMediaRec != null) {
			return true;
		} else {
			return false;
		}
	}

	public boolean stop()
	{
		if (myMediaRec != null) {
			try {
				myMediaRec.stop();
				myMediaRec.release();
				Log.d("VoiceRecord2_MR stop");
			} catch (IllegalStateException e) {
				Log.e("VoiceRecord2_MR stop !@#$ " + e.getMessage());
				myMediaRec.release();
				myMediaRec = null;
			}
		}
		
		System.gc();
		if (myMediaRec != null) {
			myMediaRec = null;
			return true;
		} else {
			return false;
		}
	}
}
