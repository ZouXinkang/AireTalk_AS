package com.pingshow.airecenter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ImageSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import com.pingshow.airecenter.R;
import com.pingshow.codec.VoiceMemoPlayer_NB;
import com.pingshow.codec.VoicePlayer2_MP;
import com.pingshow.util.MyUtil;

public class MessageDetailActivity extends Activity{
	
	private long time;
	static private VoiceMemoPlayer_NB vmp = null;
	static private VoicePlayer2_MP myVP = null;
	
	private Handler mHandler=new Handler();
	private long enterTime = 0;
	
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
				WindowManager.LayoutParams.FLAG_FULLSCREEN);  
				requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.detail_message);
		enterTime = new Date().getTime();
		
		Intent intent = getIntent(); 
		String imagePath=intent.getStringExtra("imagePath");
		
		if (imagePath!=null && MyUtil.checkSDCard(getApplicationContext()))
		{
			try {
				String [] items=imagePath.split("/");
				String relative="../"+items[items.length-2]+"/"+items[items.length-1];
				
				FileOutputStream fout = new FileOutputStream(Global.SdcardPath_inbox+"tmp.html");
				
				int display_mode = getResources().getConfiguration().orientation;
				
				String html="<html><body bgcolor=#000000><div style='width:100%'>" +
					"<table width='100%' height='100%' border='0' cellpadding='0' cellspacing='0' style='position:relative'>" +
					"<td style='vertical-align:middle' align='center'><img src='"+relative+"' "+(display_mode==1?"width='100%'":"height='100%'")+" align='middle'>" +
					"</td></table></div></body></html>";
				
				fout.write(html.getBytes());
				fout.flush();
				fout.close();
			} catch (Exception e) {
			}
			
			WebView web = (WebView)findViewById(R.id.mms_image);
			
			float mDensity = getResources().getDisplayMetrics().density;
			
			web.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
			web.getSettings().setBuiltInZoomControls(true);
			web.getSettings().setUseWideViewPort(true);
			web.loadUrl("file://"+Global.SdcardPath_inbox+"tmp.html");
			
			if (mDensity<=1.f)
				web.setInitialScale(30);
			else if (mDensity<=1.5f)
				web.setInitialScale(60);
			else
				web.setInitialScale(70);
			
			web.setVisibility(View.VISIBLE);
		}
		
		String msgContent=intent.getStringExtra("msgContent");
		TextView bv=(TextView)findViewById(R.id.textcontent);
		
		int type=intent.getIntExtra("type",2);
		int status=intent.getIntExtra("status",-1);
		if (msgContent!=null)
		{
			msgContent=msgContent.replace("(iMG)", "");
			msgContent=msgContent.replace("(Vm)", "");
			if (msgContent.startsWith("\n"))
				msgContent=msgContent.substring(1);
		}
		
		if (msgContent!=null && msgContent.length()>0)
		{
			Smiley sm=new Smiley();
			
			if (sm.hasSmileys(msgContent)>0)
			{
				SpannableString spannable = new SpannableString(msgContent);
				for(int i=0;i<Smiley.MAXSIZE-1;i++)
				{
					for(int j=0;j<sm.getCount(i);j++)
					{
						Drawable d = getResources().getDrawable(R.drawable.sm01+i);
					    d.setBounds(0, 0, 36, 36);
				        ImageSpan icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
				        spannable.setSpan(icon, sm.getStart(i,j), sm.getEnd(i,j), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}
				bv.setText(spannable);
			}
			else
				bv.setText(msgContent);
			if (type==1)
				bv.setBackgroundResource(R.drawable.balloon_left);
			else{
				if (status==SMS.STATUS_PENING)
					bv.setBackgroundResource(R.drawable.balloon_right_pending);
				else
					bv.setBackgroundResource(R.drawable.balloon_right);
			}
			mHandler.postDelayed(dismissBalloon,2000);
		}
		else
			bv.setVisibility(View.GONE);
		
		/*
		ImageView iv = (ImageView)findViewById(R.id.find_me);
		
		if (longitude!=0 && latitude!=0 && address!=null && prf.readLong(address,0)!=0 
				&& new Date().getTime()/1000-prf.readLong(address,0)<=0){
			iv.setOnClickListener(mFindOnMap);
		}else{
			iv.setVisibility(View.GONE);
		}*/
		
		time=intent.getLongExtra("time", 0);
		if (time>0)
		{
			String tFormat=DateUtils.formatDateTime(getApplicationContext(), time, 
					DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_WEEKDAY|
					DateUtils.FORMAT_SHOW_YEAR|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_CAP_AMPM);
			TextView tv=(TextView)findViewById(R.id.msginfo);
			tv.setText(tFormat);
		}
		
		String audioPath=intent.getStringExtra("audioPath");
		if (audioPath!=null)
		{
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			onPlayVoiceMemo(audioPath);
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(KeyEvent.KEYCODE_BACK==keyCode && (new Date().getTime()-enterTime)<1000 && (vmp != null || myVP != null))
			return true;
		return super.onKeyDown(keyCode, event);
	}

	private Runnable dismissBalloon = new Runnable(){
		public void run()
		{
			((TextView)findViewById(R.id.textcontent)).setVisibility(View.INVISIBLE);
		}
	};
	/*
	private OnClickListener mFindOnMap = new OnClickListener() {
		public void onClick(View v) {
			ProfileActivity.onLaunchMapView(MessageDetailActivity.this, 
					longitude, 
					latitude, 
					time,
					displayname, address);
		}
	};
	*/

	public void onPlayVoiceMemo(String path){
		if (vmp!=null) return;
		if (myVP != null) return;  //tml*** new vmsg
		try {           
			if (path.endsWith("amr")) {
				vmp = new VoiceMemoPlayer_NB(MessageDetailActivity.this);
				vmp.setDataSource(path);
			    vmp.prepare(); 
			    vmp.start();
			} else {
				//tml*** new vmsg
				myVP = new VoicePlayer2_MP(MessageDetailActivity.this, path);
				myVP.start();
			}
		} catch (IOException e) {
			Log.e("mda1 " + e.getMessage());
			vmp = null;
			myVP = null;
			return;
		}catch (IllegalArgumentException e) {
			Log.e("mda1 " + e.getMessage());
			vmp = null;
			myVP = null;
			return;
		}catch (IllegalStateException e) {
			Log.e("mda1 " + e.getMessage());
			vmp = null;
			myVP = null;
			return;
		}
	}
	@Override
	protected void onDestroy()
	{
		try{
			if (vmp!=null) {
				vmp.stop();
				vmp=null;
			}
			if (myVP != null) {  //tml*** new vmsg
				myVP.stop();
				myVP = null;
			}
		}catch(Exception e){
			vmp = null;
			myVP = null;
		}
		setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
		mHandler.removeCallbacks(dismissBalloon);
		super.onDestroy();
	}
}
