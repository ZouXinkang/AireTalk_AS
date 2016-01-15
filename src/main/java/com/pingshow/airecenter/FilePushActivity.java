package com.pingshow.airecenter;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import com.pingshow.airecenter.R;
import com.pingshow.beehive.ClientThread;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;

public class FilePushActivity extends Activity {
	static FilePushActivity instance;
	private ImageView mScreen;
	private SurfaceView mVideo;
	private Handler mHandler=new Handler();
	private WakeLock mWakeLock;
	private MediaPlayer mp=null;
	private int mType;
	static int empty=0;
	private String filePath;
	private AudioManager mAudioManager;
	private int PreviousVolume;
	private boolean alreadyExists;
	//tml*** browser save
	private MimeTypeMap mimeMap;
	private String mTypeFull;
	
	public static FilePushActivity getInstance()
	{
		return instance;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_push);
		
		mScreen=(ImageView)findViewById(R.id.screen);
		mVideo=(SurfaceView)findViewById(R.id.video);
		
		setup(getIntent());
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mAudioManager = ((AudioManager)getSystemService(Context.AUDIO_SERVICE));
		PreviousVolume=mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		
		instance=this;
	}
	
	public void setup(Intent intent)
	{
		mType=intent.getIntExtra("mimeType",0);
		filePath=intent.getStringExtra("filePath");
		alreadyExists=intent.getBooleanExtra("alreadyExists", false);
		
		if (mp!=null) {
			try{
				mp.stop();
				mp.release();
			}catch(Exception e){}
			mp=null;
		}
		
		switch(mType)
		{
		case 1://audio
			mVideo.setVisibility(View.INVISIBLE);
			mScreen.setImageResource(R.drawable.music_push);
			mScreen.setVisibility(View.VISIBLE);
			break;
		case 2://video
			mVideo.setVisibility(View.VISIBLE);
			mScreen.setVisibility(View.INVISIBLE);
			break;
		case 3://image
			mScreen.setImageResource(R.drawable.image);
			mScreen.setVisibility(View.VISIBLE);
			mVideo.setVisibility(View.INVISIBLE);
			break;
		}
		
		Log.d("setup done *********");
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		if (mWakeLock.isHeld())	mWakeLock.release();
		reenableKeyguard();
	}
	
	public void tryToPlay()
	{
		Log.i("tryToPlay");
		if (mp!=null) {
			try {
				mp.start();
			} catch (Exception e) {
				Log.e("tryToPlay Failed :"+e.getMessage());
			}
			return;
		}
		setSource();
	}
	
	
	private void setSource()
	{
		Log.d("FilePushAcitivity: setSource()");
		mp=new MediaPlayer();
		if (mp!=null && (mType==1||mType==2))
		{
			try {
				FileInputStream fis = new FileInputStream(filePath);  
		        mp.setDataSource(fis.getFD());
				mp.prepare();
				mp.setOnErrorListener(new OnErrorListener() {
					@Override
					public boolean onError(MediaPlayer mp, int what, int extra) {
						Log.e("playing onError what="+what+","+extra);
						pauseAndGo(mp);
						return true;
					}
				});
				mp.setOnCompletionListener(new OnCompletionListener(){
					@Override
					public void onCompletion(MediaPlayer mp) {
						Log.d("onCompletion");
					}
				});

				if (mp.getVideoHeight()>0 && mp.getVideoWidth()>0)
				{
					Log.i("** has Video **");
					mp.setDisplay(mVideo.getHolder());
				}
				else
					mHandler.post(hideVideo);
				
				mp.start();
			} catch (IllegalArgumentException e) {
				Log.w("*** mp ***  IllegalArgumentException: "+e.getMessage());
				mp.release();
				mp=null;
				e.printStackTrace();
			} catch (IllegalStateException e) {
				Log.w("*** mp *** IllegalStateException "+e.getMessage());
				mp.release();
				mp=null;
				e.printStackTrace();
			} catch (IOException e) {
				Log.w("*** mp *** IOException "+e.getMessage());
				mp.release();
				mp=null;
				e.printStackTrace();
			}
		}
		else if (alreadyExists)
		{
			Log.i("alreadyExists, so finalizeStreaming");
			mHandler.post(finalizeStreaming);
		}
	}
	
	private void setVideoSize() {

        // // Get the dimensions of the video
        int videoWidth = mp.getVideoWidth();
        int videoHeight = mp.getVideoHeight();
        float videoProportion = (float) videoWidth / (float) videoHeight;

        // Get the width of the screen
        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        float screenProportion = (float) screenWidth / (float) screenHeight;

        // Get the SurfaceView layout parameters
        android.view.ViewGroup.LayoutParams lp = mVideo.getLayoutParams();
        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        // Commit the layout parameters
        mVideo.setLayoutParams(lp);
    }
	
	Runnable hideVideo=new Runnable()
	{
		public void run()
		{
			mVideo.setVisibility(View.GONE);
		}
	};
	
	public void pauseAndGo(MediaPlayer mp)
	{
		int pos=0;
		try{
			if (mp.isPlaying())
			{
				pos=mp.getCurrentPosition();
				mp.stop();
			}
			mp.reset();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			FileInputStream fis = new FileInputStream(filePath);  
	        mp.setDataSource(fis.getFD());  
			mp.prepare();
			mp.seekTo(pos);
			
			mp.setOnErrorListener(new OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					Log.e("playing onError what="+what+","+extra);
					pauseAndGo(mp);
					return true;
				}
			});
			
			if (mp.getVideoHeight()>0 && mp.getVideoWidth()>0)
			{
				Log.i("** has Video **");
				mp.setDisplay(mVideo.getHolder());
			}
			else
				mHandler.post(hideVideo);
			
			mp.start();
			
			
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void finalizeFile()
	{
		Log.i("finalizeStreaming in FilePushActivity");
		mHandler.post(finalizeStreaming);
	}
	
	private void launchApp(String mime)
	{
		File file = new File(filePath);
		if(!file.exists()) return;
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);
      
		intent.setDataAndType(Uri.fromFile(file),mime);
		try {
			startActivityForResult(intent, 0);
		} catch (Exception e) {
			intent.setDataAndType(Uri.fromFile(file),"*/*");
			startActivityForResult(intent, 0);
		} 
	}
	
	protected Runnable finalizeStreaming=new Runnable()
	{
		public void run()
		{
			//tml*** browser save
			MyUtil.copyFile(new File(filePath), new File(fileDest(filePath, mType)), true, getApplicationContext());
			
			if ((mType==1||mType==2) && mp!=null)
				pauseAndGo(mp);
			else if ((mType==1||mType==2) && mp==null)
				setSource();
			else if (mType==3)
			{
				Log.d("finalizeStreaming: "+filePath);

				try{
					int sizeDividedBy=1;
					
					try{
						BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
						bitmapOptions.inJustDecodeBounds=true;
						BitmapFactory.decodeFile(filePath, bitmapOptions);
						Log.i("image size:"+bitmapOptions.outWidth+","+bitmapOptions.outHeight);
						if (bitmapOptions.outHeight>4000 || bitmapOptions.outWidth>4000)
							sizeDividedBy=5;
						else if (bitmapOptions.outHeight>3000 || bitmapOptions.outWidth>3000)
							sizeDividedBy=4;
						else if (bitmapOptions.outHeight>2000 || bitmapOptions.outWidth>2000)
							sizeDividedBy=3;
						else if (bitmapOptions.outHeight>1000 || bitmapOptions.outWidth>1000)
							sizeDividedBy=2;
						if (bitmapOptions.outHeight<0 || bitmapOptions.outWidth<0)
						{
							Log.i("Decode error");
							return;
						}
						Log.i("sizeDividedBy="+sizeDividedBy);
			    	}catch(Exception e){
			    	}catch(OutOfMemoryError e){
			    		System.gc();
						System.gc();
			    	}
			    	try{
			    		Log.i("loadBitmapSafe");
						Bitmap bmp=ImageUtil.loadBitmapSafe(sizeDividedBy, filePath);
						Log.i("loadBitmapSafe done");
						mScreen.setImageBitmap(bmp);
						Log.i("setImageBitmap done");
			    	}catch(Exception e){
			    		Log.e("BMP decode error");
			    	}catch(OutOfMemoryError e){
			    		System.gc();
						System.gc();
			    	}
				}catch(Exception e){}
				
				System.gc();
				System.gc();
			}
			else if (mType==4)//apk
				launchApp("application/vnd.android.package-archive");
			else if (mType==5)//text
				launchApp("text/plain");
			else if (mType==6)//pdf
				launchApp("application/pdf");
			else if (mType==7)//word
				launchApp("application/msword");
			else if (mType==8)//xls
				launchApp("application/vnd.ms-excel");
			else if (mType==9)//ppt
				launchApp("application/vnd.ms-powerpoint");
		}
	};
	
	@Override
	public void onDestroy()
	{
		if (mp!=null)
		{
			mp.stop();
			mp.release();
		}
		mp=null;
		instance=null;
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, PreviousVolume, 0);
		super.onDestroy();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE,"ScreenSharing");
		mWakeLock.acquire();
		disableKeyguard();
	}
	
	long enabletime;
    KeyguardManager mKeyguardManager;
    KeyguardManager.KeyguardLock mKeyguardLock;
    boolean enabled;
    
	void disableKeyguard() {
		if (AmazonKindle.IsKindle()) return;
    	if (mKeyguardManager == null) {
	        mKeyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
	        mKeyguardLock = mKeyguardManager.newKeyguardLock("VideoCall_KeyGuard");
	        enabled = true;
    	}
		if (enabled) {
			mKeyguardLock.disableKeyguard();
			enabled = false;
			enabletime = SystemClock.elapsedRealtime();
		}
	}
	
	void reenableKeyguard() {
		if (AmazonKindle.IsKindle()) return;
		if (!enabled) {
			mKeyguardLock.reenableKeyguard();
			enabled = true;
		}
	}
	
	public void Destroy()
	{
		finish();
	}
	
	public void pause()
	{
		if (mp!=null)
		{
			try{
				mp.pause();
			}catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void resume()
	{
		if (mp!=null)
		{
			try{
				mp.start();
			}catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void volumeUp()
	{
		if (mp!=null)
		{
			int cur=mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			int max=mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			cur+=(max/10);
			if (cur>max) cur=max;
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, cur, 0);
			mAudioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);  //tml*** aireshare/
		}
	}
	
	public void volumeDown()
	{
		if (mp!=null)
		{
			int cur=mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			int max=mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			cur-=(max/10);
			if (cur<0) cur=0;
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, cur, 0);
			mAudioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);  //tml*** aireshare/
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode==0)
		{
			finish();
		}
	}
	
	//tml*** browser save
	private String getMIMEdest(String mime) {
		Log.i("tml AIRESHARE MIME>> " + mime);
		String type = "";

		if (mime.equals("mp3") || mime.equals("mid") || mime.equals("wav")
				|| mime.equals("amr") || mime.equals("wma")) {
			type = Global.SdcardPath_music;// audio
		} else if (mime.equals("3gp") || mime.equals("mp4") || mime.equals("avi")
				|| mime.equals("rmvb") || mime.equals("m4v")|| mime.equals("ogg")) {
			type = Global.SdcardPath_video;// video/end
		} else if (mime.equals("jpg") || mime.equals("gif") || mime.equals("png")
				|| mime.equals("jpeg") || mime.equals("bmp")) {
			type = Global.SdcardPath_image;// image/end
		} else {
			type = Global.SdcardPath_files;// "*/*";
		}

		return type;
	}

	public String fileDest(String fLoc, int type) {
		File file = new File(fLoc);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String currentDateandTime = sdf.format(new Date());
		String mime = "";
		String fExt = null;
		if (type == 1) {
			fExt = "mp3";
			mime = "audio/mpeg";
		} else if (type == 2) {
			fExt = "mp4";
			mime = "video/mp4";
		} else if (type == 3) {
			fExt = "png";
			mime = "image/png";
		} else if (type == 4) {
			mime = "application/vnd.android.package-archive";
		} else if (type == 5) {
			fExt = "txt";
			mime = "text/plain";
		} else if (type == 6) {
			mime = "application/pdf";
		} else if (type == 7) {
			mime = "application/msword";
		} else if (type == 8) {
			mime = "application/vnd.ms-excel";
		} else if (type == 9) {
			mime = "application/vnd.ms-powerpoint";
		} else {
			mime = "";
		}
		if (fExt == null) {
			if (mime == "") {
				fExt = "dat";
			} else {
				try {
					fExt = mimeMap.getExtensionFromMimeType(mime);
				} catch (Exception e) {
					fExt = "dat";
				}
			}
		}

		String datName = file.getName();
		String fName = datName.substring(0, datName.lastIndexOf("."));
		String fdest = getMIMEdest(fExt) + currentDateandTime + "_AS" + fName + "." + fExt;
		Log.i("tml AIRESHARE>> " + file.getPath() + " >> " + fdest);
		
		return fdest;
	}
	//***tml
	
}
