package com.pingshow.airecenter;

import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.view.SurfaceView;
import android.view.View;

import com.pingshow.airecenter.view.ProgressBar;
import com.pingshow.codec.MotionDetect;
import com.pingshow.video.capture.AndroidVideoApi5JniWrapper;
import com.pingshow.video.capture.AndroidVideoApi9JniWrapper;
import com.pingshow.video.capture.hwconf.AndroidCameraConfiguration;
import com.pingshow.voip.core.Version;

public class Surveillance {

	static int _width = 640;
	static int _height = 480;
//	static int _width = 1280;  //alex*** security resolution ++/
//	static int _height = 720;
	private Camera cam;
	private SurfaceView mSurface;
	private MotionDetect md;
	private Handler mHandler=new Handler();
	private ProgressBar bar;
	
	void start(Context context, int to_idx, String to_address, SurfaceView surface, View view)
	{
		md=new MotionDetect();
		md.load(context, _width, _height, to_idx, to_address);
		mSurface=surface;
		bar=(ProgressBar)view;
		
		MyPreference mPref=new MyPreference(context);
		if (mPref.readBoolean("MotionDetection", true))
			startCamera.run();
		if (mPref.readBoolean("SoundDetection", false))
		{
			md.initSoundDetect();
			md.startSoundDetect();
			mHandler.postDelayed(refreshSound, 250);
		}
	}
	
	Runnable refreshSound=new Runnable()
	{
		public void run()
		{
			float avg=(float)md.sAvg;
			bar.setProgress((avg/1200));
			mHandler.postDelayed(refreshSound, 100);
		}
	};
	
	void stop()
	{
		Log.e("surveillance stop");
		if (cam!=null)
		{
			if (Version.sdkAboveOrEqual(9))
				AndroidVideoApi9JniWrapper.stopRecording(cam);
			else
				AndroidVideoApi5JniWrapper.stopRecording(cam);
		}
		
		md.release();
	}
	
	Runnable startCamera=new Runnable()
	{
		public void run()
		{
			if (cam==null)
			{
				int r=0;
				String Brand=Build.BRAND.toLowerCase();
				String Model=Build.MODEL.toLowerCase();
				
				int cameraId=1;
				if(AndroidCameraConfiguration.retrieveCameras().length>0)
					cameraId=cameraId%AndroidCameraConfiguration.retrieveCameras().length;
				else
					cameraId=0;
				
				if (Brand.contains("amazon"))//kindle
					cameraId=0;
				
				Log.d("startCamera:"+cameraId);
				
				if (Model.contains("k200") || Model.contains("s82"))//s802
				{
					r=0; //S802
				}else if (Model.contains("a9"))//a9
					r=90;
				else if (Brand.contains("amazon"))//kindle
					r=90;
				
				if (Version.sdkAboveOrEqual(9))
					cam=(Camera) AndroidVideoApi9JniWrapper.startRecording(cameraId, _width, _height, 5, r, 0);
				else
					cam=(Camera) AndroidVideoApi5JniWrapper.startRecording(cameraId, _width, _height, 5, 0, 0);
				
				if (cam!=null)
				{
					AndroidVideoApi9JniWrapper.setMotionDetect(md);
				}
	
				try {
					cam.setPreviewDisplay(mSurface.getHolder());
				} catch (Exception exc) {
					exc.printStackTrace(); 
				} catch (IncompatibleClassChangeError e){}
			}
		}
	};
}
