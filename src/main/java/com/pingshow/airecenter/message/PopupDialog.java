package com.pingshow.airecenter.message;

import java.util.ArrayList;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Html;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.ConversationActivity;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MainActivity;
import com.pingshow.airecenter.MessageDetailActivity;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.Smiley;
import com.pingshow.airecenter.db.SmsDB;
import com.pingshow.airecenter.map.MapViewLocation;
import com.pingshow.util.ImageUtil;

public class PopupDialog extends Activity {
	
	private String addressFrom;
	private String DisplayName;
	private long contactid;
	private String audio_path;
	private String image_path;
	private long latitudeE6;
	private long longitudeE6;
	private String msgContent;
	private long time;
	private int interphoneType = -1;
	private int attachment;
	private int type;
	private boolean isMissedCallDialog;
	private Handler mhandler=new Handler();
	private SmsDB mDB;
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.msg_dialog);
	    
	    WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
	    float mDensity = getResources().getDisplayMetrics().density;
	    lp.width=(int)(640.f*mDensity);
		lp.dimAmount = 0.3f;
		getWindow().setAttributes(lp);
	    
	    setup(getIntent().getExtras());
	    
        theDialog=this;
	}

	private void setup(Bundle bundle) {
	   
	    addressFrom=bundle.getString("EXTRAS_FROM_ADDRESS");
	    contactid=bundle.getLong("EXTRAS_CONTACT_ID", 0);
	    DisplayName=bundle.getString("EXTRAS_DISPLAY_NAME");
	    
	    time=bundle.getLong("EXTRAS_TIME");
	    msgContent=bundle.getString("EXTRAS_MESSAGE_BODY");
	    
		audio_path=bundle.getString("EXTRAS_AUDIO_PATH");
		image_path=bundle.getString("EXTRAS_IMAGE_PATH");
		latitudeE6=bundle.getLong("EXTRAS_LATITUDE", Global.DEFAULT_LAT);
		longitudeE6=bundle.getLong("EXTRAS_LONGITUDE", Global.DEFAULT_LON);
		msgContent=bundle.getString("EXTRAS_MESSAGE_BODY");
		attachment=bundle.getInt("EXTRAS_ATTACHMENT");
		type=bundle.getInt("EXTRAS_SMS_TYPE");
		isMissedCallDialog=bundle.getBoolean("EXTRAS_FROM_MISSED_CALL",false);
		
	    Button button = (Button)findViewById(R.id.close);
        button.setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			finish();
    		}});
        
		if (addressFrom!=null)
		{
			int idx = bundle.getInt("EXTRAS_FROM_IDX");
			Drawable photo=ImageUtil.getUserPhoto(PopupDialog.this, idx);

			if (photo != null)
				((ImageView)findViewById(R.id.user_photo)).setImageDrawable(photo);
			else {
				if (addressFrom.startsWith("[<GROUP>]"))
					((ImageView)findViewById(R.id.user_photo)).setImageResource(R.drawable.group_empty);
				else
					((ImageView)findViewById(R.id.user_photo)).setImageResource(R.drawable.bighead);
			}
		}
		
        button = (Button)findViewById(R.id.reply);
        if (isMissedCallDialog)
        {
        	button.setText(getString(R.string.checkout));
        }
        else{
	        if (attachment==0 && !msgContent.startsWith("[<AGREESHARE>]"))
	        	button.setText(getString(R.string.reply));
	        else
	        	button.setText(getString(R.string.checkout));
        }
        
        if(msgContent.contains("itph*")){
        	interphoneType=Integer.parseInt(msgContent.substring(6))-1;
			String tFormat=DateUtils.formatDateTime(getApplicationContext(), time, 
					DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_CAP_AMPM);
        	msgContent = "(vExp)\n"+tFormat;
        }else if (msgContent.startsWith("(iPh)")){
        	String tFormat=DateUtils.formatDateTime(getApplicationContext(), time, 
					DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_CAP_AMPM);
        	msgContent+="\n"+tFormat;
        }else if (msgContent.contains("(vdo)")|| msgContent.contains("(fl)")){
        	String tFormat=DateUtils.formatDateTime(getApplicationContext(), time, 
					DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_CAP_AMPM);
        	msgContent+="\n"+tFormat;
        }else if (msgContent.startsWith("here I am (")){
        	String tFormat=DateUtils.formatDateTime(getApplicationContext(), time, 
					DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_CAP_AMPM);
        	msgContent="(mAp)\n" + tFormat;
        }else if (msgContent.equals("Missed call")){
        	String tFormat=DateUtils.formatDateTime(getApplicationContext(), time, 
					DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_CAP_AMPM);
        	msgContent="(mCl) "+ tFormat;
        }else if (msgContent.startsWith("(Vm)") && msgContent.length()>4){
        	msgContent+='"';
        }
		
        button.setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			if (isMissedCallDialog)
    			{
    				Intent it=new Intent(PopupDialog.this, MainActivity.class);
    				it.putExtra("sel_mode", 0);
    				it.putExtra("fromPopupDialog", true);
    				startActivity(it);
    			}
    			else if(msgContent.startsWith("[<AGREESHARE>]")){
    				MyPreference mPref = new MyPreference(PopupDialog.this);
    				Intent it=new Intent(PopupDialog.this, MapViewLocation.class);
    				it.putStringArrayListExtra("shared_friends", (ArrayList<String>) mPref.readArray("shared_friends"));
    				startActivity(it);
					//FunctionActivity.onLaunchMapView(PopupDialog.this, longitudeE6, latitudeE6, time, DisplayName, addressFrom,contactid);
    			}
    			else if (attachment==0 || attachment==1 || attachment==8){
	    			Intent i=new Intent(PopupDialog.this, ConversationActivity.class);
	    			
	    			i.putExtra("ActivityType", 1);
	    			i.putExtra("SendeeNumber", addressFrom);
	    			i.putExtra("SendeeContactId", contactid);
	    			i.putExtra("SendeeDisplayname", DisplayName);
	    			i.putExtra("fromPopupDialog", true);
	    			i.putExtra("audioPath", audio_path);
	    			i.putExtra("attachment", attachment);
	    			
	    			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    			startActivity(i);
    			}else if (attachment==4){
	    			
    			}else{
	    			Intent i=new Intent(PopupDialog.this, MessageDetailActivity.class);
	    			
	    			i.putExtra("msgContent", msgContent);
	    			i.putExtra("imagePath", image_path);
	    			i.putExtra("audioPath", audio_path);
	    			i.putExtra("longitude", longitudeE6);
	    			i.putExtra("latitude", latitudeE6);
	    			i.putExtra("displayname", DisplayName);
	    			i.putExtra("time", time);
	    			i.putExtra("type", type);
	    			
	    			if(mDB==null) mDB=new SmsDB(PopupDialog.this);
	    			if(!mDB.isOpen()) mDB.open();
	    			mDB.setMessageReadByAddress(PopupDialog.this,addressFrom);
	    			if(mDB!=null && mDB.isOpen()) mDB.close();
	    			
	    			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    			startActivity(i);
    			}
    			AireJupiter.manySmsContent.setLength(0);
    			NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    			nm.cancel(R.string.app_name);
    			finish();
    		}});
        
		TextView tv=(TextView)findViewById(R.id.user_name);
		tv.setText(DisplayName);
		
		tv=(TextView)findViewById(R.id.msgcontent);
		
		if(msgContent.startsWith("[<AGREESHARE>]")){
			try{
				String[] res=msgContent.split(",");
				int relation = Integer.valueOf(res[2]);
				String message = getResources().getString(R.string.agree_share_sms,getResources().getStringArray(R.array.share_time)[relation-1]);
				tv.setText(message);
			}catch(Exception e){
			}
			return;
		}

		Smiley sm1 = new Smiley();
		if (sm1.hasSmileys(msgContent)>0)
		{
			SpannableString spannable = new SpannableString(msgContent);
			for(int i=0;i<Smiley.MAXSIZE;i++)
			{
				for(int j=0;j<sm1.getCount(i);j++)
				{ 
					if(i==(Smiley.MAXSIZE-1)){
						try{
							BitmapFactory.Options options = new BitmapFactory.Options();    
							options.inSampleSize = 12;
							Bitmap bitmap = BitmapFactory.decodeFile(image_path, options); 
							int start = sm1.getStart(i,j);
							int end = sm1.getEnd(i,j);
							ImageSpan icon = new ImageSpan(PopupDialog.this, bitmap, ImageSpan.ALIGN_BOTTOM);
							spannable.setSpan(icon, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
						}catch(OutOfMemoryError e){}
					}else{
						ImageSpan icon = null;
						if(i>=66){
							BitmapFactory.Options options = new BitmapFactory.Options();    
							options.inSampleSize = 1;
							Bitmap bitmap = null;
							if(i==71){//map
								options.inSampleSize = 2;
								bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mapview, options); 
							}else if(i==72){//missed call
								bitmap = BitmapFactory.decodeResource(getResources(), android.R.drawable.sym_call_missed, options); 
							}else if(i>=75){//gif
								options.inSampleSize = 2;
								bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.em001+i-75, options); 
							}
							/*
							else if(i==69){ // video
								bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.start_play, options); 
							}*/
							else
								bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sm01+i, options); 
							icon = new ImageSpan(PopupDialog.this, bitmap, ImageSpan.ALIGN_BOTTOM);
						}else{
							icon = new ImageSpan(PopupDialog.this, R.drawable.sm01+i, ImageSpan.ALIGN_BOTTOM);
						}
				        spannable.setSpan(icon, sm1.getStart(i,j), sm1.getEnd(i,j), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}
			}
			tv.setText(spannable);
		}
		else
			tv.setText(msgContent);
		
		if(AireJupiter.manySmsContent.toString().split("<br>").length > 1){
			tv.setText(Html.fromHtml(AireJupiter.manySmsContent.toString()));
			tv.setTextSize(28);
			tv=(TextView)findViewById(R.id.msgcontent);
			tv.setText(getString(R.string.RecvManySms));
		}
	}
	
	
	private Bundle bundle;
	final Runnable mRefreshViews=new Runnable()
	{
		@Override
		public void run() {
			setup(bundle);
		}
	};
	public void refresh(Bundle b)
	{
		bundle=b;
		mhandler.post(mRefreshViews);
	}
	
	private static PopupDialog theDialog;
	public static PopupDialog getInstance() { 
		if (theDialog == null) {
			return null;
		} else {
			return theDialog;
		}
	}
	
	long enabletime;
    KeyguardManager mKeyguardManager;
    KeyguardManager.KeyguardLock mKeyguardLock;
    boolean enabled;
    
	void disableKeyguard() {
    	if (mKeyguardManager == null) {
	        mKeyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
	        mKeyguardLock = mKeyguardManager.newKeyguardLock("FafaYou");
	        enabled = true;
    	}
		if (enabled) {
			mKeyguardLock.disableKeyguard();
			enabled = false;
			enabletime = SystemClock.elapsedRealtime();
			Log.d("disableKeyguard");
		}
	}
	
	void reenableKeyguard() {
		if (!enabled) {
				try {
					if (Integer.parseInt(Build.VERSION.SDK) < 5)
						Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			mKeyguardLock.reenableKeyguard();
			enabled = true;
		}
	}
	
	@Override
	protected void onPause() {
		if (Integer.parseInt(Build.VERSION.SDK) >= 5 && Integer.parseInt(Build.VERSION.SDK) <= 7)
			reenableKeyguard();
		super.onPause();
	}
	@Override
	protected void onResume() {
		super.onResume();
		if (Integer.parseInt(Build.VERSION.SDK) >= 5 && Integer.parseInt(Build.VERSION.SDK) <= 7)
			disableKeyguard();
	}
	@Override
	public void onStart() {
		super.onStart();
		if (Integer.parseInt(Build.VERSION.SDK) < 5 || Integer.parseInt(Build.VERSION.SDK) > 7)
			disableKeyguard();
	}
	@Override
	public void onStop() {
		if (Integer.parseInt(Build.VERSION.SDK) < 5 || Integer.parseInt(Build.VERSION.SDK) > 7)
			reenableKeyguard();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		theDialog=null;
		AireJupiter.manySmsContent.setLength(0);
		System.gc();
		System.gc();
		super.onDestroy();
	}
}
