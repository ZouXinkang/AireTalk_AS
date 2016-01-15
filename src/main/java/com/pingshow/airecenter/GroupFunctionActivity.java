package com.pingshow.airecenter;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.cons.Key;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;
import com.pingshow.voip.VideoCallActivity;

public class GroupFunctionActivity extends Activity {

	private ArrayList<String> memberList;
	private AmpUserDB mADB;
	private AsyncImageLoader asyncImageLoader;
	private LinearLayout verticalView;
	private float mDensity = 1.f;

	private String mAddress;
	private int mIdx;
	private String mNickname;
	private MyPreference mPref;
	private boolean broadcastConf = false;
	
	private ImageView photoView;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.group_func);

		mPref=new MyPreference(this);
		
		WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
	    mDensity = getResources().getDisplayMetrics().density;
//	    if (!mPref.readBoolean("BROADCAST", false)) {
	    if (!true) {
		    lp.width=(int)((480.f + 20.f + 20.f + 20.f + 20.f)*mDensity);
	    } else {
		    lp.width=(int)((720.f + 20.f + 20.f + 20.f + 20.f + 20.f)*mDensity);
	    }
		lp.dimAmount = 0.3f;
		getWindow().setAttributes(lp);
		
		mADB = new AmpUserDB(this);
		mADB.open();
		
		asyncImageLoader = new AsyncImageLoader(getApplicationContext());
		
		mAddress=getIntent().getStringExtra("Address");
		mIdx=getIntent().getIntExtra("Idx",0);
		
		memberList = (ArrayList<String>) getIntent().getExtras().getSerializable("chosenList");
		mNickname = getIntent().getStringExtra("Nickname");
		Log.e("tmlG mAddress=" + mAddress + " mNickname=" + mNickname
				+ " mIdx=" + mIdx + " callstate=" + AireVenus.callstate_AV);
		
		
		if (mNickname!=null)
			((TextView) findViewById(R.id.group_name)).setText(mNickname);

		verticalView=(LinearLayout)findViewById(R.id.members);
		RelativeLayout h=null;
		if (memberList != null) {
			for (int i = 0; i < memberList.size(); i++) 
			{
				if (i==0 || i==5 || i==10)
				{
					h=new RelativeLayout(this);
					h.setPadding(0, 5, 0, 5);
					verticalView.addView(h,LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				}
				String IDX = memberList.get(i);
				int idx=Integer.parseInt(IDX);
				
				ImageView photo=new ImageView(this);
				photo.setId(i*2+1);
				RelativeLayout.LayoutParams rp=new RelativeLayout.LayoutParams((int)(mDensity*80),(int)(mDensity*80));
				if ((i%5)>0) rp.addRule(RelativeLayout.RIGHT_OF, i*2+1-2);
				else rp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
					
				rp.leftMargin=(int)(mDensity*25);
				h.addView(photo,rp);
				
				String userphotoPath= Global.SdcardPath_inbox + "photo_" + idx + "b.jpg";
				if (!new File(userphotoPath).exists()) {
					userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
					if (!new File(userphotoPath).exists())
						userphotoPath = null;
				}
				photo.setTag(userphotoPath);
				
				Drawable cachedImage = asyncImageLoader.loadDrawable(userphotoPath,
						new ImageCallback() {
							public void imageLoaded(Drawable imageDrawable, String path) {
								ImageView imageViewByTag = (ImageView) verticalView.findViewWithTag(path);	
								if (imageViewByTag != null && imageDrawable!=null) {
									imageViewByTag.setImageDrawable(imageDrawable);
								}
							}
						});
	
				if (cachedImage != null && userphotoPath != null)
					photo.setImageDrawable(cachedImage);
				else {
					photo.setImageResource(R.drawable.bighead);
				}
				
				TextView name=new TextView(this);
				name.setText(mADB.getNicknameByIdx(idx));
				name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
				name.setTextColor(0xff202030);
				name.setId(i*2);
				name.setMaxHeight((int)(mDensity*34));
				name.setGravity(Gravity.CENTER);
				RelativeLayout.LayoutParams rp2=new RelativeLayout.LayoutParams((int)(mDensity*80), LayoutParams.WRAP_CONTENT);
				rp2.addRule(RelativeLayout.BELOW, i*2+1);
				rp2.addRule(RelativeLayout.ALIGN_LEFT, i*2+1);
				h.addView(name,rp2);
			}
			
			verticalView.recomputeViewAttributes(h);
		}
		
		((Button)findViewById(R.id.chat)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getIntent().getBooleanExtra("fromConversation",false))
				{
					finish();
					return;
				}
				updateFriendLastContactTime();
				Intent it=new Intent(GroupFunctionActivity.this, ConversationActivity.class);
				it.putExtra("SendeeContactId", -20);
				it.putExtra("SendeeNumber", mAddress);
				it.putExtra("SendeeDisplayname", mNickname);
				startActivity(it);
				finish();
			}
		});
		
		((Button) findViewById(R.id.edit)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});

		((Button) findViewById(R.id.conference)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try{
					broadcastConf = false;  //tml*** broadcast
					if (memberList.size()>0 && memberList.size()<=9)
					{
						AireVenus.setCallType(AireVenus.CALLTYPE_CHATROOM);
						mPref.write("incomingChatroom",false);
						
						int myIdx=0;
						try {
							myIdx=Integer.parseInt(mPref.read("myID","0"),16);
							mPref.write("ChatroomHostIdx", myIdx);
						} catch (Exception e) {}
						
						String idx = "" + myIdx;
						MakeCall.ConferenceCall(getApplicationContext(), idx,-1,false);
						
						new Thread(sendNotifyForJoinChatroom).start();
					}
					
				}catch(Exception e){}
			}
		});
		//tml*** broadcast
		((Button) findViewById(R.id.broadcast)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try{
					broadcastConf = true;
					if (memberList.size()>0 && memberList.size()<=9)
					{
						AireVenus.setCallType(AireVenus.CALLTYPE_CHATROOM);
						mPref.write("incomingChatroom",false);
						
						int myIdx=0;
						try {
							myIdx=Integer.parseInt(mPref.read("myID","0"),16);
							mPref.write("ChatroomHostIdx", myIdx);
						} catch (Exception e) {}
						
						String idx = "" + myIdx;
						MakeCall.ConferenceCall(getApplicationContext(), idx,-1,false);
						
						new Thread(sendNotifyForJoinChatroom).start();
					}
					
				}catch(Exception e){}
			}
		});
//        if (!mPref.readBoolean("BROADCAST", false))
        if (!true)
        	((Button) findViewById(R.id.broadcast)).setVisibility(View.GONE);
		
		photoView = (ImageView)findViewById(R.id.photo);

		Drawable photo;
		photo=ImageUtil.getBigRoundedUserPhoto(this, mIdx);
		if (photo!=null)
			photoView.setImageDrawable(photo);
		else
			photoView.setImageResource(R.drawable.group_empty);
		
		//tml*** beta ui
		if (DialerActivity.getDialer() != null) {
			((Button) findViewById(R.id.conference)).setAlpha(50);
			((Button) findViewById(R.id.conference)).setEnabled(false);
			((Button) findViewById(R.id.broadcast)).setAlpha(50);
			((Button) findViewById(R.id.broadcast)).setEnabled(false);
		}
		//***tml
	}
	
	
	Runnable sendNotifyForJoinChatroom=new Runnable(){
		public void run()
		{
			String myIdxHex=mPref.read("myID","0");

			String ServerIP = mPref.read("conferenceSipServer", AireJupiter.myConfSipServer_default);
			if (AireJupiter.getInstance() != null) {
				ServerIP = AireJupiter.getInstance().getIsoConf(ServerIP);  //tml*** china ip
			}
			long ip=MyUtil.ipToLong(ServerIP);
			String HexIP=Long.toHexString(ip);
			
			String content=Global.Call_Conference + "\n\n"+HexIP+"\n\n"+myIdxHex;
			//tml*** broadcast
			if (broadcastConf) {
				mPref.write(Key.BCAST_CONF, 1);
				content = Global.Call_Conference + Global.Call_Broadcast + "\n\n"+HexIP+"\n\n"+myIdxHex;
			} else {
				mPref.write(Key.BCAST_CONF, -1);
			}

			if (AireJupiter.getInstance() != null)
				AireJupiter.getInstance().updateCallDebugStatus(true, null);
			for(int i=0; i<memberList.size(); i++)
			{
				int idx=Integer.parseInt(memberList.get(i));
				if (idx<50) continue;
				
				String address=mADB.getAddressByIdx(idx);
				
				if (AireJupiter.getInstance()!=null && AireJupiter.getInstance().tcpSocket()!=null)
				{
					if (AireJupiter.getInstance().isLogged())
					{
						if (i>0) MyUtil.Sleep(500);
						if (AireJupiter.getInstance() != null)
							AireJupiter.getInstance().updateCallDebugStatus(false, "\n>Conf " + address);
						Log.d("voip.inviteConf1 " + address + " " + content);
						AireJupiter.getInstance().tcpSocket().send(address, content, 0, null, null, 0, null);
					}
				}
			}
		}
	};
	
	private void updateFriendLastContactTime()
	{
		if (UserPage.sortMethod==1)
		{
			if (mADB.isOpen())
			{
				mADB.updateLastContactTimeByIdx(mIdx, new Date().getTime());
				UserPage.forceRefresh=true;
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mADB != null && mADB.isOpen())
			mADB.close();
	}
}
