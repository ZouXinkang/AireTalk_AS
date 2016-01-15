package com.pingshow.airecenter;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.db.GroupDB;
import com.pingshow.util.ImageUtil;

public class JoinNewGroupActivity extends Activity {
	private int mIdx;
	private String mNickname;
	private int mGroupID;
	private boolean mEdit;
	private ArrayList<String> sendeeList = new ArrayList<String>();
	private Handler mHandler=new Handler();
	float mDensity = 1.f;
	
	private static JoinNewGroupActivity instance=null;
	
	public static JoinNewGroupActivity getInstance() { 
		return instance;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.join_group_dialog);
		
		mDensity = getResources().getDisplayMetrics().density;
		
		instance=this;
		
		WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.3f;
		getWindow().setAttributes(lp);
		
		mNickname=getIntent().getStringExtra("Nickname");
		mIdx=getIntent().getIntExtra("Idx",0);
		String creator=getIntent().getStringExtra("Creator");
		mGroupID=getIntent().getIntExtra("GroupId",0);
		mEdit=getIntent().getBooleanExtra("Edit",false);
		
		GroupDB mGDB=new GroupDB(this);
		mGDB.open(true);
		sendeeList=mGDB.getGroupMembersByGroupIdx(mGroupID);
		sendeeList.add("0");
		mGDB.close();
		
		Drawable photo=ImageUtil.getBigRoundedUserPhoto(this, mIdx);
		if (photo!=null)
			((ImageView)findViewById(R.id.photo)).setImageDrawable(photo);
		else
			((ImageView)findViewById(R.id.photo)).setImageResource(R.drawable.group_empty);
		
		if (mEdit)
		{
			((TextView)findViewById(R.id.info)).setText(getString(R.string.the_group)+": "+mNickname);
			((Button)findViewById(R.id.add)).setVisibility(View.GONE);
			((Button)findViewById(R.id.ignore)).setVisibility(View.GONE);
		}else{
			((TextView)findViewById(R.id.info)).setText(String.format(getString(R.string.add_in_group), creator, mNickname));
		
			((Button)findViewById(R.id.add)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					mNM.cancel(R.string.app_name);
					
					Intent it = new Intent(Global.Action_InternalCMD);
					it.putExtra("Command", Global.CMD_JOIN_A_NEW_GROUP_VERIFIED);
					it.putExtra("GroupID", mGroupID);
					it.putExtra("GroupName", mNickname);
					sendBroadcast(it);
					
					finish();
				}
			});
			
			((Button)findViewById(R.id.ignore)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					mNM.cancel(R.string.app_name);
					
					Intent it = new Intent(Global.Action_InternalCMD);
					it.putExtra("Command", Global.CMD_ADD_AS_RELATED_FRIEND);
					it.putExtra("GroupID", mGroupID);
					it.putExtra("GroupName", mNickname);
					sendBroadcast(it);
					
					finish();
				}
			});
		}
		
		((ImageView)findViewById(R.id.close)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		mHandler.postDelayed(new Runnable(){
			public void run()
			{
				arrangePickedUsers();
			}
		},100);
	}
	
	protected void onDestroy() {
		instance=null;
		System.gc();
		super.onDestroy();
	};
	
	private void arrangePickedUsers()
	{
		if (sendeeList==null || sendeeList.size()==0) return;
		
		RelativeLayout s=(RelativeLayout)findViewById(R.id.members);
		s.removeAllViews();
		
		try{
			int count=sendeeList.size();
			int width=(int)((float)s.getMeasuredWidth()/mDensity);
			if (width<=0) {
				int w=getWindowManager().getDefaultDisplay().getWidth();
				width = (int)((float)w/mDensity)-50;
			}
			int space=(width-20)/(count+1);
			for(int i=0;i<count;i++)
			{
				ImageView a=new ImageView(this);
				a.setBackgroundResource(R.drawable.empty);
				int idx=Integer.parseInt(sendeeList.get(i));
				
				String userphotoPath;
				if (idx==0) {
					MyPreference mPref=new MyPreference(this);
					userphotoPath = mPref.read("myPhotoPath", "");
				}
				else {
					userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
					//tml*** userphoto fix
					if (!new File(userphotoPath).exists()) {
						userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
						if (!new File(userphotoPath).exists())
							userphotoPath = null;
					}
				}
				
				Drawable photo=ImageUtil.getBitmapAsRoundCorner(userphotoPath,1,4);
				if (photo!=null)
					a.setImageDrawable(photo);
				else
					a.setImageResource(R.drawable.bighead);
				
				RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams((int)(mDensity*60), (int)(mDensity*60));
				lp.leftMargin=(int)(mDensity*((count-i)*space-30));
				if (lp.leftMargin<0) 
					lp.leftMargin=0;
				else if (lp.leftMargin>(int)(mDensity*(width-80))) 
					lp.leftMargin=(int)(mDensity*(width-80));
				lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				s.addView(a, lp);
			}
		}catch(Exception e){}
	}
}
