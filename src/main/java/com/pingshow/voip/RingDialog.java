package com.pingshow.voip;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MainActivity;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.SecurityNewActivity;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.core.VoipCall;
import com.pingshow.voip.core.VoipCore;

public class RingDialog extends Activity 
{
	private String mAddress;
	private boolean videoCall;
	private String mDisplayName;
	
	public static RingDialog instance=null;
	private MyPreference mPref;

	private Handler mHandler = new Handler();
	
	private SpeechRecognizer mSpeechRecognizer;
	private MyRecognitionListener voiceListener;
	private int prev_rmsdB_bar = 0;
	
	public static RingDialog getInstance()
	{
		return instance;
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPref = new MyPreference(RingDialog.this);
		
		setContentView(R.layout.ring_dialog);
		this.overridePendingTransition(R.anim.push_up_in, R.anim.slide_top_to_bottom);
		
		int w=getResources().getDisplayMetrics().widthPixels;
		int h=getResources().getDisplayMetrics().heightPixels;
		float mDensity=getResources().getDisplayMetrics().density;
	    WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.alpha=0.5f;
		lp.x=(w-(int)(mDensity*290));
		lp.y=(h-(int)(mDensity*84));
		getWindow().setAttributes(lp);
		
		videoCall=getIntent().getBooleanExtra("VideoCall",false);
		mAddress=getIntent().getStringExtra("PhoneNumber");
		
		if (videoCall)
			((TextView)findViewById(R.id.status)).setText(R.string.incoming_video_call);
		else
			((TextView)findViewById(R.id.status)).setText(R.string.incoming_call);
		
		if (mAddress!=null)
		{
			AmpUserDB mADB=new AmpUserDB(this);
			mADB.open(true);
			mDisplayName=mADB.getNicknameByAddress(mAddress);
			if (mDisplayName!=null) ((TextView)findViewById(R.id.displayname)).setText(mDisplayName);
			int idx=mADB.getIdxByAddress(mAddress);
			Drawable photo=ImageUtil.getBigRoundedUserPhoto(this, idx);
			if (photo!=null)
				((ImageView)findViewById(R.id.photo)).setImageDrawable(photo);
			
			mADB.close();
		}
		
		((Button)findViewById(R.id.answer)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				
				if (AireVenus.instance()!=null)
				{
//					AireVenus.instance().stopRinging();
					AireVenus.instance().stopRing();  //tml*** new ring
				}
				
				Intent lIntent = new Intent();
				lIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				lIntent.setClass(RingDialog.this, DialerActivity.class);
				lIntent.putExtra("incomingCall", true);
				lIntent.putExtra("PhoneNumber", mAddress);
				lIntent.putExtra("VideoCall", videoCall);
				lIntent.putExtra("answerCall", true);
				startActivity(lIntent);
				finish();

				//tml*** cec
				boolean autoHDMIauto = mPref.readBoolean("HDMIctrl_auto", true);
				boolean autoHDMItv = mPref.readBoolean("HDMIctrl_tv", true);
				boolean autoHDMIinput = mPref.readBoolean("HDMIctrl_input", true);
				if (!autoHDMIauto && (autoHDMItv || autoHDMIinput)) {
					Thread hdmi = new Thread() {
						@Override
						public void run() {
							boolean autoHDMItv = mPref.readBoolean("HDMIctrl_tv", true);
							boolean autoHDMIinput = mPref.readBoolean("HDMIctrl_input", true);
							if (autoHDMItv) hdmiCmdExec("on");
							if (autoHDMIinput) hdmiCmdExec("switch");
						}
					};
					hdmi.start();
				}
				//***tml
			}
		});
		
		((ImageView)findViewById(R.id.hangup)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				if (AireVenus.instance()!=null)
				{
					VoipCore lVoipCore = AireVenus.instance().getVoipCore();
					if (lVoipCore!=null)
					{
						VoipCall myCall = lVoipCore.getCurrentCall();
						if (myCall != null)
						{
							lVoipCore.terminateCall(myCall);
						}
					}
				}
				
				Log.d("updateCallLog....");
				int dur=0;
				
				Intent it = new Intent(Global.Action_InternalCMD);
				it.putExtra("Command", Global.CMD_UPDATE_CALL_LOG);
				it.putExtra("address", mAddress);
				it.putExtra("displayname", mDisplayName);
				it.putExtra("contact_id", 0);
				it.putExtra("time", new Date().getTime());
				it.putExtra("type", 1);
				it.putExtra("duration", dur);
				it.putExtra("direction", 1);//1: incoming, 2:outgoing
				it.putExtra("status", 0);
				
				it.putExtra("runAsSip", false);
				it.putExtra("starttime", new Date().getTime());
				RingDialog.this.sendBroadcast(it);
				
				Log.d("broadcast CALL END...");
				Intent it2 = new Intent(Global.Action_InternalCMD);
				it2.putExtra("Command", Global.CMD_CALL_END);
				it2.putExtra("immediately", 90000);
				it2.putExtra("AireCall", false);
				
				if (AireJupiter.getInstance()!=null)
					AireJupiter.getInstance().attemptCall=false;
				
				RingDialog.this.sendBroadcast(it2);
				finish();
			}
		});
		
		instance=this;

		//tml*** voice control
		if (mPref.readBoolean("voice_control", false)) {
//		if (mPref.readBoolean("voice_control", false)
//				&& !(AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM)) {
			prev_rmsdB_bar = 0;
			if (!MyUtil.isISO_China(RingDialog.this, mPref, null)) {
				mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
		        voiceListener = new MyRecognitionListener();
		        mSpeechRecognizer.setRecognitionListener(voiceListener);
			}
			((ImageView) findViewById(R.id.voicerecogn)).setVisibility(View.VISIBLE);
		} else {
			((ImageView) findViewById(R.id.voicerecogn)).setVisibility(View.GONE);
		}
        //**tml
	}
	//tml*** voice control
	private class MyRecognitionListener implements RecognitionListener {
        @Override
        public void onBeginningOfSpeech() {
        	Log.e("tmlvoice onBeginningOfSpeech");
        }
        @Override
        public void onBufferReceived(byte[] buffer) {
//        	Log.e("tmlvoice onBufferReceived");
        }
        @Override
        public void onEndOfSpeech() {
        	Log.e("tmlvoice onEndOfSpeech");
			((ImageView) findViewById(R.id.voicerecogn))
					.setBackground(getResources().getDrawable(R.drawable.bg_round));
        }
        
        @Override
        public void onError(int error) {
			((ImageView) findViewById(R.id.voicerecogn))
					.setBackground(getResources().getDrawable(R.drawable.bg_round));
			mSpeechRecognizer.cancel();
			if (voiceIntent != null) {
				Log.e("tmlvoice tryagain 1ERROR=" + error);
				if (!mPref.readBoolean("normal_ring", true))
					startSpeechListen("start");
			} else {
				Log.e("tmlvoice tryagain 0ERROR=" + error);
				if (!mPref.readBoolean("normal_ring", true)) {
					mHandler.postDelayed(voiceRecogn, 500);
				}
			}
        }
        
        @Override
        public void onEvent(int eventType, Bundle params) {
//        	Log.e("tmlvoice onEvent");
        }
        @Override
        public void onPartialResults(Bundle partialResults) {
//        	Log.e("tmlvoice onPartialResults");
        }
        @Override
        public void onReadyForSpeech(Bundle params) {
//        	Log.e("tmlvoice onReadyForSpeech");
        }
        
        @Override
        public void onResults(Bundle results) {
			ArrayList<String> matches = null;
			String [] cases_answer = {
					getResources().getString(R.string.v_answer0a),
					getResources().getString(R.string.v_answer0b),
					getResources().getString(R.string.v_answer0c),
					getResources().getString(R.string.v_answer0d),
					getResources().getString(R.string.v_answer0e),
					getResources().getString(R.string.v_answer0f),
					getResources().getString(R.string.v_answer0g),
					getResources().getString(R.string.v_answer0h),
					getResources().getString(R.string.v_answer1a),
					getResources().getString(R.string.v_answer1b),
					getResources().getString(R.string.v_answer1c),
					getResources().getString(R.string.v_answer1d),
					getResources().getString(R.string.v_answer1e),
					getResources().getString(R.string.v_answer1f),
					getResources().getString(R.string.v_answer1g),
					getResources().getString(R.string.v_answer2a),
					getResources().getString(R.string.v_answer2b),
					getResources().getString(R.string.v_answer2c),
					getResources().getString(R.string.v_answer2d),
					getResources().getString(R.string.v_answer2e),
					getResources().getString(R.string.v_answer2f),
					getResources().getString(R.string.v_answer3a)};
			String [] cases_answerCancel = {
					getResources().getString(R.string.v_end0a),
					getResources().getString(R.string.v_end1a),
					getResources().getString(R.string.v_end2a)};
			
			if (results != null) {
				matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
				String matchA = "", matchAC = "", spoke = "";
	        	if (matches != null && matches.size() > 0) {
        			boolean matchEDcasesA = false, matchEDcasesAC = false, matchEDcasesH = false;
	            	for (int i = 0; i < matches.size(); i++) {
	            		spoke = matches.get(i).toLowerCase(Locale.getDefault()) + "," + spoke;
	            	}

	            	for (int j = 0; j < cases_answer.length; j++) {
	            		if (spoke.contains(cases_answer[j].toLowerCase(Locale.getDefault()))) {
	            			matchA = cases_answer[j].toLowerCase(Locale.getDefault());
	            			matchEDcasesA = true;
	            			break;
	            		}
	            	}
	            	for (int j = 0; j < cases_answerCancel.length; j++) {
	            		if (spoke.contains(cases_answerCancel[j].toLowerCase(Locale.getDefault()))) {
	            			matchAC = cases_answerCancel[j].toLowerCase(Locale.getDefault());
	            			matchEDcasesAC = true;
	            			break;
	            		}
	            	}
	        		
		        	Log.e("tmlvoice RD onResults (" + matches.size() + ") matches >>> " + spoke);
	            	if (matchEDcasesA) {
        				Log.e("tmlvoice MATCH.answer SUCCESS! = " + matchA);
	            		toastMaker(getResources().getString(R.string.call) + ": " 
	            				+ getResources().getString(R.string.answer), 
	            				20, Toast.LENGTH_LONG, Gravity.CENTER, 0, 0);
        				runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
	            				((ImageView) findViewById(R.id.voicerecogn))
    									.setBackground(getResources().getDrawable(R.drawable.bg_round_on1));
	            				((Button) findViewById(R.id.answer)).performClick();
                            }
        				});
	            	} else if (matchEDcasesAC) {
        				Log.e("tmlvoice MATCH.answerCancel SUCCESS! = " + matchAC);
	            		toastMaker(getResources().getString(R.string.call) + ": " 
	            				+ getResources().getString(R.string.cancel), 
	            				20, Toast.LENGTH_LONG, Gravity.CENTER, 0, 0);
        				runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
	            				((ImageView) findViewById(R.id.voicerecogn))
    									.setBackground(getResources().getDrawable(R.drawable.bg_round_on1));
	            				((Button) findViewById(R.id.hangup)).performClick();
                            }
        				});
	            	} else {
        				Log.e("tmlvoice TRYAGAIN!");
        				mHandler.postDelayed(runRESpeechListen0, 50);
        			}
	        	} else {
    				Log.e("tmlvoice tryagain MATCHES=0");
    				mHandler.postDelayed(runRESpeechListen0, 50);
	        	}
			} else {
				Log.e("tmlvoice tryagain RESULTS=null");
				mHandler.postDelayed(runRESpeechListen0, 50);
        	}
        }
        
        @Override
        public void onRmsChanged(float rmsdB) {
        	int rmsdB_bar = (int) (rmsdB * 4);
        	if (rmsdB_bar < 2) {
        		rmsdB_bar = 2;
        	} else if (rmsdB_bar > 100) {
        		rmsdB_bar = 100;
        	}
        	if (rmsdB_bar > prev_rmsdB_bar) {
        		if (((ImageView) findViewById(R.id.voicerecogn)).getDrawable()
        				!= getResources().getDrawable(R.drawable.bg_round_on1)) {
    				((ImageView) findViewById(R.id.voicerecogn))
							.setBackground(getResources().getDrawable(R.drawable.bg_round_on1));
        		}
        	} else if (rmsdB_bar < prev_rmsdB_bar) {
        		if (((ImageView) findViewById(R.id.voicerecogn)).getDrawable()
        				!= getResources().getDrawable(R.drawable.bg_round)) {
    				((ImageView) findViewById(R.id.voicerecogn))
							.setBackground(getResources().getDrawable(R.drawable.bg_round));
        		}
        	}
        	prev_rmsdB_bar = rmsdB_bar;
        }
	}
	//***tml
	@Override
	protected void onDestroy() {
		//tml*** voice control
		if (mSpeechRecognizer != null) {
			destroyVoice();
		}
		mHandler.removeCallbacks(voiceRecogn);
		mHandler.removeCallbacks(runSpeechListen);
		mHandler.removeCallbacks(runRESpeechListen0);
		mHandler.removeCallbacks(runRESpeechListen1);
		//***tml
		instance=null;
		super.onDestroy();
	}
	
	public void bye()
	{
		finish();
	}
	//tml*** answer view
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		int source = event.getSource();
		int bstate = event.getButtonState();
//		Log.e("tml answer0 Action=" + action + " Source=" + source + " ButtonState=" + bstate);
		if (action == 0 && source == 8194 && bstate == 4) {
			Log.e("tml answer0view answer");
			((Button) findViewById(R.id.answer)).performClick();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		Log.e("tml answer0 keyCode=" + keyCode);
		if (keyCode == 4) {
			Log.e("tml answer0view hangup");
			((ImageView) findViewById(R.id.hangup)).performClick();
			return false;
		}
		return true;
	}
	
	@Override
    protected void onResume() {
		if ((AireVenus.instance() != null) && (AireVenus.callstate_AV == null)) {
			Log.e("tml answer0view resume");
			Intent it=new Intent(RingDialog.this, MainActivity.class);
			it.putExtra("launchFromSelf", true);
			startActivity(it);
			finish();
		}
    	super.onResume();
	}
	//***tml
	//tml*** voice control
	private Intent voiceIntent;
	Runnable voiceRecogn = new Runnable() {
		@Override
		public void run() {
			Log.e("tmlv RD init voiceRecogn");
			voiceIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
			voiceIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
	        try {
				if (!(AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM)) {
					mHandler.post(runSpeechListen);
					Log.e("tmlv init voiceRecogn startListening");
				} else {
					mHandler.postDelayed(runSpeechListen, 1500);
					Log.e("tmlv init voiceRecogn startListening CONF");
				}
	        } catch (Exception e) {
	        	destroyVoice();
	        	Log.e("tmlv RD voiceRecogn.ERR " + e.getMessage());
	            Toast.makeText(getApplicationContext(), "ERROR: VoiceRecogn not supported", Toast.LENGTH_SHORT).show();
	        }
		}
	};
	
	public void voiceRecogn() {
		mHandler.post(voiceRecogn);
	}
	
	private void destroyVoice() {
		if (mSpeechRecognizer != null) {
			Log.e("destroying RD voiceRecogn");
			mHandler.removeCallbacks(voiceRecogn);
			mHandler.removeCallbacks(runSpeechListen);
			mHandler.removeCallbacks(runRESpeechListen0);
			mHandler.removeCallbacks(runRESpeechListen1);
			mSpeechRecognizer.destroy();
			mSpeechRecognizer = null;
			voiceListener = null;
			voiceIntent = null;
		}
	}

	public void startSpeechListen(String mode) {
		Log.e("tmlvoice startSpeechListen(" + mode + ")");
		if (mSpeechRecognizer != null && voiceIntent != null) {
			if (mode.equals("stop")) {
				mSpeechRecognizer.stopListening();
			} else if (mode.equals("cancel")) {
				mSpeechRecognizer.cancel();
			} else if (mode.equals("restart0")) {
				if(!mPref.readBoolean("normal_ring", true)) {
					mSpeechRecognizer.stopListening();
					mSpeechRecognizer.startListening(voiceIntent);
				}
			} else if (mode.equals("restart1")) {
				mSpeechRecognizer.stopListening();
				mSpeechRecognizer.startListening(voiceIntent);
			} else if (mode.equals("crestart")) {
				mSpeechRecognizer.cancel();
				mSpeechRecognizer.startListening(voiceIntent);
			} else if (mode.equals("start")) {
				mSpeechRecognizer.startListening(voiceIntent);
			}
		}
		if (mSpeechRecognizer != null && mode.equals("destroy")) {
			destroyVoice();
		}
		
	}

	Runnable runSpeechListen = new Runnable() {
		@Override
		public void run() {
			startSpeechListen("start");
		}
	};
	
	Runnable runRESpeechListen0 = new Runnable() {
		@Override
		public void run() {
			startSpeechListen("restart0");
		}
	};

	Runnable runRESpeechListen1 = new Runnable() {
		@Override
		public void run() {
			startSpeechListen("restart1");
		}
	};
	//***tml
	//tml*** beta ui
	public void toastMaker(String text, int textsize, int length, int gravity, int goffx, int goffy) {
		if (text != null) {
			if (!(length == 0 || length == 1)) length = Toast.LENGTH_SHORT;
			Toast tst = Toast.makeText(instance, text, length);
			tst.setGravity(gravity, goffx, goffy);
			if (textsize > 0) {
				LinearLayout tstLayout = (LinearLayout) tst.getView();
				TextView tstTV = (TextView) tstLayout.getChildAt(0);
				tstTV.setTextSize(textsize);
			}
			tst.show();
		}
	}

	//tml*** cec
	public void hdmiCmdExec(String param) {
		if (AireJupiter.getInstance() != null && param != null) {
			AireJupiter.hdmiCmdExec(param);
		}
	}
}
