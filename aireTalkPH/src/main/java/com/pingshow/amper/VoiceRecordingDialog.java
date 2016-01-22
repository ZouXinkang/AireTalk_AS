package com.pingshow.amper;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pingshow.codec.VoiceMemo_NB;
import com.pingshow.codec.VoiceRecord2_MR;

public class VoiceRecordingDialog extends Activity {
	private String SrcAudioPath;
//	private VoiceMemo_NB vm;
	private VoiceRecord2_MR myVR;
	private String leftTime="60";
	private CountDownTimer cdt;
	private MyPreference mPref = null;
	private Handler mHandler=new Handler();
	private TextView mTimer;
	
	public static int REC_DONE=444;
	public static int REC_CANCEL=333;
	int voicetime=0;
	private Intent intent;
	float mDensity;
	private boolean largeScreen=false;
	
	private PowerManager.WakeLock mWakeLock;
	
	public static boolean voiceRording;
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    voiceRording=true;
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.voice_rec_dialog);
	    this.overridePendingTransition(R.anim.push_up_in, R.anim.slide_top_to_bottom);
	    largeScreen=(findViewById(R.id.large)!=null);
	    
	    mPref=new MyPreference(this);
	    
	    mDensity=getResources().getDisplayMetrics().density;
	    	    
	    LayoutParams lp=getWindow().getAttributes();    
        lp.x=0;
        lp.y=LayoutParams.FILL_PARENT;
        lp.width=LayoutParams.FILL_PARENT;
        lp.height=(int)(mDensity*(largeScreen?320.f:214.f));
        
        lp.gravity=Gravity.BOTTOM | Gravity.LEFT;
        lp.flags=LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        getWindow().setAttributes(lp);
		
	    intent=getIntent();
	    
    	SrcAudioPath=intent.getStringExtra("path");
    	onVoiceMemoRecording(SrcAudioPath);
    	mPref.write("vociemessaging", true);
    	
    	mTimer=(TextView)findViewById(R.id.timer);
	    
	    ((Button)findViewById(R.id.done)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
				onStopMemoRecording();
				intent.putExtra("voicetime", voicetime);
				setResult(RESULT_OK,intent);
    			finish();
    		}}
    	);
	    
	    ((Button)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			onStopMemoRecording();
    			setResult(REC_CANCEL);
    			new File(SrcAudioPath).delete();
    			finish();
    		}}
    	);
	    
	    PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
				PowerManager.ACQUIRE_CAUSES_WAKEUP, "Aire.Voice.Msg");
		if (mWakeLock != null)
			mWakeLock.acquire();
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
	}
	
	@Override
	protected void onPause() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		onStopMemoRecording();
		mHandler.removeCallbacks(mRecordingDuration);
		finish();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		if (mWakeLock!=null) mWakeLock.release();
		super.onDestroy();
	}
	
	@Override
	public void onBackPressed() {
		onStopMemoRecording();
		setResult(REC_CANCEL);
		new File(SrcAudioPath).delete();
		super.onBackPressed();
	}
	
	final Runnable mRecordingDuration = new Runnable() {        
		public void run() {
			mTimer.setText(leftTime);
			if (leftTime.equals("10"))
				mTimer.setTextColor(0xC0ffc600);
			LinearLayout main=(LinearLayout)findViewById(R.id.timer_ani);
			AnimationSet as = new AnimationSet(false);
		    as.setInterpolator(new AccelerateInterpolator());
		    AlphaAnimation aa= new AlphaAnimation(0, 1.0f);
			ScaleAnimation sa = new ScaleAnimation(0.4f, 1, 0.4f, 1, mDensity*64, mDensity*75);
			sa.setDuration(800);
			as.addAnimation(sa);
			aa.setDuration(800);
			as.addAnimation(aa);
			as.setDuration(900);
			main.startAnimation(as);	
		}
	};

	private void onStopMemoRecording() {
		voiceRording = false;
		if (cdt != null) {
			cdt.cancel();
			cdt = null;
		}
//		if (vm!=null) 
//		{
//			vm.stop();
//			vm = null;
//			mPref.write("vociemessaging", false);
//		}
		if (myVR != null) {  //tml*** new vmsg
			myVR.stop();
			myVR = null;
			mPref.write("vociemessaging", false);
		}
	}
	
	private void onVoiceMemoRecording(String path)
	{	
		String Audiopath = path;
		try {
//			vm=new VoiceMemo_NB(Audiopath);
//			vm.start();
			//tml*** new vmsg
			if (myVR == null) {
				myVR = new VoiceRecord2_MR(VoiceRecordingDialog.this, Audiopath,
						MediaRecorder.OutputFormat.RAW_AMR,
						MediaRecorder.OutputFormat.DEFAULT, 8000);
				myVR.start();
			    
				cdt = new CountDownTimer(60000, 1000) {
					@Override
					public void onTick(long millisUntilFinished) {
						voicetime = (int) (millisUntilFinished / 1000);
						leftTime = voicetime+"";
						mHandler.post(mRecordingDuration);
					}
					@Override
					public void onFinish() {
//						if (vm!=null) {
						if (myVR != null) {  //tml*** new vmsg
							onStopMemoRecording();
							intent.putExtra("voicetime", voicetime);
							setResult(RESULT_OK, intent);
							finish();
						}
					}
				};
				cdt.start();
			} else {
				onStopMemoRecording();
    			setResult(REC_CANCEL);
    			mPref.write("vociemessaging", false);
    			finish();
			}
		} catch (Exception e) {
//			vm=null;
			//tml*** new vmsg
			Log.e("onVoiceMemoRecording !@#$ " + e.getMessage());
			myVR = null;
			onStopMemoRecording();
			setResult(REC_CANCEL);
			mPref.write("vociemessaging", false);
			finish();
		}
	}
}