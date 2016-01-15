package com.pingshow.airecenter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.pingshow.airecenter.R;
import com.pingshow.voip.DialerActivity;
import com.pingshow.voip.VideoCallActivity;

public class DialerFrame {
	
	static View parent;
	static Handler mHandler=new Handler();
	static Context mContext;

	public static void checkEmbeddedDialer(View view)
	{
		parent=view;
		mHandler.post(refreshStatus);
	}
	
	static Runnable refreshStatus=new Runnable()
	{
		public void run()
		{
			if ((DialerActivity.getDialer()!=null) || VideoCallActivity.minimized)
	    	{
	    		LinearLayout oncallView=(LinearLayout) parent.findViewById(R.id.oncall);
	    		((TextView)oncallView.findViewById(R.id.displayname)).setText(DialerActivity.getDialer().getCurrentOnCallName());
	    		((TextView)oncallView.findViewById(R.id.status)).setText(DialerActivity.getDialer().getCurrentOnCallStatus());
	    		Bitmap photo=DialerActivity.getDialer().getCurrentDrawable();
		    	if (photo!=null)
		    		((ImageView)oncallView.findViewById(R.id.photo)).setImageBitmap(photo);
	    		oncallView.setVisibility(View.VISIBLE);

	    		mHandler.postDelayed(refreshStatus, 1000);
	    	}
			else
			{
				LinearLayout oncallView=(LinearLayout) parent.findViewById(R.id.oncall);
				oncallView.setVisibility(View.GONE);
			}
		}
	};
	
	public static void setFrame(Context context, View view)
	{
		parent=view;
		mContext=context;
		LinearLayout oncallView=(LinearLayout) view.findViewById(R.id.oncall);
        oncallView.setVisibility(View.GONE);
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View oncall = inflater.inflate(R.layout.inflate_oncall, null, false);
		oncallView.addView(oncall);
		
		((ImageView)oncall.findViewById(R.id.hangup)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (DialerActivity.getDialer()!=null)
				{
					DialerActivity.getDialer().hangupCall();
				}
			}
		});
		
		oncallView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (DialerActivity.getDialer()!=null)
				{
					Intent lIntent = new Intent(mContext, DialerActivity.class);
					lIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					mContext.startActivity(lIntent);
				}
			}
		});
	}
}
