package com.pingshow.airecenter;

import java.io.File;

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
import android.widget.TextView;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.RelatedUserDB;
import com.pingshow.network.MyNet;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;

public class AddAsFriendActivity extends Activity {
	private String mAddress;
	private int mIdx;
	private String mNickname;
	private int mJoint;
	private boolean annoying=false;
	
	private static AddAsFriendActivity instance=null;
	private static String topAddress="";
	
	private Handler mHandler = new Handler();
	
	public static AddAsFriendActivity getInstance() { 
		return instance;
	}
	
	public static String getTopAddress() { 
		return topAddress;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.addfriend_dialog);
		
		instance=this;
		
		WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.5f;
		getWindow().setAttributes(lp);
		
		mAddress=getIntent().getStringExtra("Address");
		mNickname=getIntent().getStringExtra("Nickname");
		mIdx=getIntent().getIntExtra("Idx",0);
		mJoint=getIntent().getIntExtra("Joint",0);
		
		annoying=getIntent().getBooleanExtra("Annoying",false);
		
		Drawable photo=ImageUtil.getUserPhoto(this, mIdx);
		if (photo!=null)
			((ImageView)findViewById(R.id.photo)).setImageDrawable(photo);
		else
			((ImageView)findViewById(R.id.photo)).setImageResource(R.drawable.bighead);
			
		((ImageView)findViewById(R.id.photo)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				String userphotoPath = Global.SdcardPath_inbox + "photo_" + mIdx + "b.jpg";
				File f = new File(userphotoPath);
				if (!f.exists())
				{
					String remotefile = "profiles/photo_" + mIdx + ".jpg";
					int success = 0;
					int count = 0;
					
					try{
						do {
							MyNet net = new MyNet(AddAsFriendActivity.this);
							success = net.Download(remotefile, userphotoPath, AireJupiter.myLocalPhpServer);
							if (success==1||success==0)
								break;
							MyUtil.Sleep(500);
						} while (++count < 3);
						
						if (success!=1)
						{
							count=0;
							do {
								MyNet net = new MyNet(AddAsFriendActivity.this);
								success = net.Download(remotefile, userphotoPath, null);
								if (success==1||success==0)
									break;
								MyUtil.Sleep(500);
							} while (++count < 3);
						}
					}catch(Exception e){
					}
				}
				
				f = new File(userphotoPath);
				if (f.exists())
				{
					Intent i = new Intent(AddAsFriendActivity.this,MessageDetailActivity.class);
					i.putExtra("imagePath", userphotoPath);
					i.putExtra("displayname", mNickname);
					i.putExtra("address", mAddress);
					startActivity(i);
				}
			}
		});
		
		if (getIntent().getIntExtra("Stranger",0)==1)
		{
			((TextView)findViewById(R.id.unknown)).setText(String.format(getString(R.string.accept_this_stranger), mNickname));
			((TextView)findViewById(R.id.unknown)).setVisibility(View.VISIBLE);
			((TextView)findViewById(R.id.displayname)).setText(mNickname);
			((TextView)findViewById(R.id.displayname)).setVisibility(View.VISIBLE);
			
			if (annoying)
			{
				((Button)findViewById(R.id.block)).setVisibility(View.VISIBLE);
				((Button)findViewById(R.id.ignore)).setVisibility(View.GONE);
			}else{
				((Button)findViewById(R.id.ignore)).setVisibility(View.VISIBLE);
				((Button)findViewById(R.id.block)).setVisibility(View.GONE);
			}
			
			topAddress=mAddress;
		}else{
			((TextView)findViewById(R.id.displayname)).setText(mNickname);
			((TextView)findViewById(R.id.unknown)).setVisibility(View.GONE);
			((Button)findViewById(R.id.ignore)).setVisibility(View.GONE);
			
			topAddress="";
		}
		
		((Button)findViewById(R.id.add)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				MyPreference mPref=new MyPreference(AddAsFriendActivity.this);
				int Strange = getIntent().getIntExtra("Stranger", 0);
				Log.i("Inviting :: " + Strange + " " + mAddress + " " + mIdx + " " + mNickname);
				if (getIntent().getIntExtra("Stranger",0)==1)
				{
					try{
						if (!mAddress.equals(AireJupiter.myPhoneNumber))
						{
							AmpUserDB mADB = new AmpUserDB(AddAsFriendActivity.this);
							mADB.open();
							mADB.insertUser(mAddress, mIdx, mNickname);
							if(mADB.isOpen()) 
								mADB.close();
						}
						
						ContactsOnline.setContactOnlineStatus(mAddress, 2);
						RelatedUserDB mRDB=new RelatedUserDB(AddAsFriendActivity.this);
						mRDB.open();
						if (mRDB != null && mRDB.isOpen())
						{
							mRDB.deleteContactByAddress(mAddress);
							mRDB.close();
						}
					}catch(Exception e){
					}
					
					UserPage.forceRefresh=true;

					Intent intent = new Intent(Global.Action_Refresh_Gallery);
					sendBroadcast(intent);
					
					Intent it = new Intent(Global.Action_InternalCMD);
					it.putExtra("Command", Global.CMD_UPLOAD_FRIENDS);
					it.putExtra("type", 1);//Single Friend
					it.putExtra("serverType", 1);//add
					it.putExtra("idxlist", mIdx+"");
					sendBroadcast(it);
					
					try {
						SendAgent agent = new SendAgent(AddAsFriendActivity.this, 0, 0, false);
						agent.onSend(mAddress, Global.Hi_AddFriend1, 0, null, null, true);
					} catch (Exception e) {
					}
					
					setResult(RESULT_OK);
					
					NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					mNM.cancel(R.string.app_name);
				}
				else{
					Intent i=new Intent();
					i.putExtra("Address", mAddress);
					i.putExtra("Nickname", mNickname);
					i.putExtra("Idx", mIdx);
					
					UserPage.forceRefresh=true;
					
					setResult(RESULT_OK, i);
				}
				
				mPref.writeLong("last_dlf_status", 0);
				
				finish();
			}
		});
		
		((Button)findViewById(R.id.block)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try{
					AmpUserDB mADB = new AmpUserDB(AddAsFriendActivity.this);
					mADB.open();
					mADB.blockUserByAddress(mAddress,1);
					if(mADB.isOpen()) 
						mADB.close();
					RelatedUserDB mRDB=new RelatedUserDB(AddAsFriendActivity.this);
					mRDB.open();
					if (mRDB != null && mRDB.isOpen())
					{
						mRDB.deleteContactByAddress(mAddress);
						mRDB.close();
					}
					Intent intent = new Intent(Global.Action_Refresh_Gallery);
					sendBroadcast(intent);
					
					NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					mNM.cancel(R.string.app_name);
				}catch(Exception e){
				}
				setResult(RESULT_OK);
				finish();
			}
		});
		
		((Button)findViewById(R.id.ignore)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				mNM.cancel(R.string.app_name);
				finish();
			}
		});
		
		if (mJoint>0)
		{
			((TextView)findViewById(R.id.joint)).setText(String.format(getString(R.string.joint), mJoint));
		}
		else
			((TextView)findViewById(R.id.joint)).setVisibility(View.GONE);
	}
	
	protected void onDestroy() {
		instance=null;
		System.gc();
		super.onDestroy();
	};

}
