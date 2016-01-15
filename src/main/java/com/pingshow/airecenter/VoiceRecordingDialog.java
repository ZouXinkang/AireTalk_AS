package com.pingshow.airecenter;

import java.io.File;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.view.Gravity;
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
import com.pingshow.airecenter.R;
import com.pingshow.codec.VoiceMemo_NB;

public class VoiceRecordingDialog extends Activity {
	private String SrcAudioPath;
	private VoiceMemo_NB vm;
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
	private PowerManager.WakeLock mWakeLock;
	
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.voice_rec_dialog);
	    
	    mPref=new MyPreference(this);
	    
	    mDensity=getResources().getDisplayMetrics().density;
	    	    
	    LayoutParams lp=getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
	    lp.dimAmount = 0.5f;
        lp.width=(int)(mDensity*720);
        lp.height=(int)(mDensity*340);
        getWindow().setAttributes(lp);
		
	    intent=getIntent();
	    
    	SrcAudioPath=intent.getStringExtra("path");
    	onVoiceMemoRecording(SrcAudioPath);
    	mPref.write("vociemessaging", true);
    	
    	mTimer=(TextView)findViewById(R.id.timer);
	    
	    ((Button)findViewById(R.id.done)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
				oStopMemoRecording();
				intent.putExtra("voicetime", voicetime);
				setResult(RESULT_OK,intent);
    			finish();
    		}}
    	);
	    
	    ((Button)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			oStopMemoRecording();
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
	}
	
	@Override
	protected void onPause() {
		oStopMemoRecording();
		mHandler.removeCallbacks(mRecordingDuration);
		finish();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		if (mWakeLock!=null) mWakeLock.release();
		super.onDestroy();
	}
	
	@Override
	public void onBackPressed() {
		oStopMemoRecording();
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
			ScaleAnimation sa = new ScaleAnimation(0.4f, 1, 0.4f, 1, mDensity*50, mDensity*50);
			sa.setDuration(800);
			as.addAnimation(sa);
			aa.setDuration(800);
			as.addAnimation(aa);
			as.setDuration(900);
			main.startAnimation(as);	
		}
	};

	private void oStopMemoRecording() {
		if (cdt!=null) {
			cdt.cancel();
			cdt = null;
		}
		if (vm!=null) 
		{
			vm.stop();
			vm = null;
			mPref.write("vociemessaging", false);
		}
	}
	
	private void onVoiceMemoRecording(String path)
	{	
		String Audiopath=path;
		try{
			vm=new VoiceMemo_NB(Audiopath);
			vm.start();
			cdt = new CountDownTimer(60000,1000) {
				@Override
				public void onTick(long millisUntilFinished) {
					voicetime=(int)(millisUntilFinished/1000);
					leftTime = voicetime+"";
					mHandler.post(mRecordingDuration);
				}
				@Override
				public void onFinish() {
					if (vm!=null)
					{
						oStopMemoRecording();
						intent.putExtra("voicetime", voicetime);
						setResult(RESULT_OK, intent);
						finish();
					}
				}
			};
			cdt.start();
		}catch (Exception e){
			vm=null;
		}
	}
}