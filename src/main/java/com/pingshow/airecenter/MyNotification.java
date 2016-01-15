package com.pingshow.airecenter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.browser.MainBrowser;
import com.pingshow.airecenter.map.MapViewLocation;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;

public class MyNotification {

	static MyNotification instance=null;
	private FrameLayout frm;
	private Context mContext;
	private View notiView;
	private Handler mHandler=new Handler();
	private Bundle bundle;
	
	static MyNotification getInstance()
	{
		return instance;
	}
	
	public MyNotification()
	{
		if (MainActivity._this!=null)
		{
			mContext=MainActivity._this;
			frm=MainActivity._this.getNotificationLayout();
		}
		else if (ShoppingActivity._this!=null)
		{
			mContext=ShoppingActivity._this;
			frm=ShoppingActivity._this.getNotificationLayout();
		}
		else if (SecurityNewActivity._this!=null)
		{
			mContext=SecurityNewActivity._this;
			frm=SecurityNewActivity._this.getNotificationLayout();
		}
		else if (LocationSettingActivity._this!=null)
		{
			mContext=LocationSettingActivity._this;
			frm=LocationSettingActivity._this.getNotificationLayout();
		}
		else if (MyUtil.hasGoogleMap(false, null) && MapViewLocation._this!=null)
		{
			mContext=MapViewLocation._this;
			frm=MapViewLocation._this.getNotificationLayout();
		}
		else if (BDMapViewLocation._this!=null)
		{
			mContext=BDMapViewLocation._this;
			frm=BDMapViewLocation._this.getNotificationLayout();
		}
		//tml*** browser save
		else if (MainBrowser._this!=null)
		{
			mContext=MainBrowser._this;
			frm=MainBrowser._this.getNotificationLayout();
		}
		//***tml
		
		
		if (frm!=null)
		{
			float mDensity = mContext.getResources().getDisplayMetrics().density;
			boolean xlarge=(mContext.getResources().getDisplayMetrics().heightPixels>720);
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			notiView = inflater.inflate(R.layout.notification, null, false);
			frm.addView(notiView);
			RelativeLayout.LayoutParams params=(RelativeLayout.LayoutParams)frm.getLayoutParams();
			params.width=(int)(mDensity*(xlarge?280:260));
			params.height=(int)(mDensity*(xlarge?100:85));
			frm.setLayoutParams(params);
		}
	}
	
	private void setup(Bundle b)
	{
		bundle=b;
		
		if (frm==null || notiView==null) return;
		
		TextView name=(TextView)notiView.findViewById(R.id.displayname);
		name.setText(b.getString("EXTRAS_DISPLAY_NAME"));
		
		int attached=b.getInt("EXTRAS_ATTACHMENT");
		
		String str="";
		if (attached==0)
		{
			str="sent you a message.";
		}
		else if (attached==1)
		{
			str="sent you an audio message.";
		}else if (attached==2)
		{
			str="sent you a picture message.";
		}else if (attached==8)
		{
			str="sent you a file.";
		}
		
		TextView content=(TextView)notiView.findViewById(R.id.status);
		content.setText(str);
		
		int idx=b.getInt("EXTRAS_FROM_IDX");
		ImageView photo=(ImageView)notiView.findViewById(R.id.photo);
		photo.setImageDrawable(ImageUtil.getUserPhoto(mContext, idx));
		
		notiView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				
				Intent i=new Intent(mContext, ConversationActivity.class);
				
				i.putExtra("ActivityType", 1);
    			i.putExtra("SendeeNumber", bundle.getString("EXTRAS_FROM_ADDRESS"));
    			i.putExtra("SendeeContactId", bundle.getLong("EXTRAS_CONTACT_ID", -20));
    			i.putExtra("SendeeDisplayname", bundle.getString("EXTRAS_DISPLAY_NAME"));
    			i.putExtra("fromPopupDialog", true);
    			i.putExtra("audioPath", bundle.getString("EXTRAS_AUDIO_PATH"));
    			i.putExtra("attachment", bundle.getInt("EXTRAS_ATTACHMENT"));
    			
    			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    			mContext.startActivity(i);
			}
		});
		
		mHandler.removeCallbacks(dismissSelf);
		mHandler.postDelayed(dismissSelf, 5000);
	}
	
	private Runnable dismissSelf=new Runnable()
	{
		public void run()
		{
			frm.removeAllViews();
		}
	};
	
	public void start(Bundle b)
	{
		mHandler.removeCallbacks(dismissSelf);
		setup(b);
	}
}
