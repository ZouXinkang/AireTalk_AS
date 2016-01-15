package com.pingshow.airecenter;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.GroupDB;
import com.pingshow.airecenter.map.MapViewLocation;
import com.pingshow.airecenter.map.SelfMapView;
import com.pingshow.beehive.BeeHiveService;
import com.pingshow.network.MyNet;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;

public class FunctionActivity extends Activity {
	private String mAddress;
	private int mIdx;
	private long mContactId;
	private String mNickname;
	private AmpUserDB mADB;
	private ProgressDialog progress = null;
	private MyPreference mPref;
	
	private ImageView mPhotoView;
	
	private boolean inGroup=false;
	private boolean inCorpGroup=false;
	private int mGroupID;
	float mDensity = 1.f;
	private String iso;
	
	private List<Map<String,Object>> memberList = new ArrayList<Map<String,Object>>();
	
	private ArrayList<String> sendeeList = new ArrayList<String>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.func_dialog);
		
        neverSayNeverDie(this);  //tml|bj*** neverdie/
		
		mDensity = getResources().getDisplayMetrics().density;
		
		WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
//	    lp.width=(int)(mDensity*688);
		lp.dimAmount = 0.3f;
		getWindow().setAttributes(lp);
		
		mAddress=getIntent().getStringExtra("Address");
		mNickname=getIntent().getStringExtra("Nickname");
		mIdx=getIntent().getIntExtra("Idx",0);
		mContactId=getIntent().getLongExtra("Contact_id",-20);
		Log.e("tmlF mAddress=" + mAddress + " mNickname=" + mNickname
				+ " mIdx=" + mIdx + " mContactId=" + mContactId + " callstate=" + AireVenus.callstate_AV);
		
		mPref=new MyPreference(this);
		mADB = new AmpUserDB(this);
		mADB.open();
		
		inGroup=mAddress.startsWith("[<GROUP>]");
		inCorpGroup=(mIdx>200000000);
		
		Drawable photo;
		mPhotoView=((ImageView)findViewById(R.id.photo));
		photo=ImageUtil.getUserPhoto(this, mIdx);
		if (photo!=null)
			mPhotoView.setImageDrawable(photo);
		else
			mPhotoView.setImageResource(inGroup?R.drawable.group_empty:R.drawable.bighead);
		
		if (inGroup)
			mPhotoView.setClickable(false);
		else
		mPhotoView.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				String userphotoPath = Global.SdcardPath_inbox + "photo_" + mIdx + "b.jpg";
				File f = new File(userphotoPath);
				//tml*** beta ui
				if ((mAddress.equals("support") && mNickname.equals("Support")) || mIdx == 2) {
					String url = "http://airetalk.com/airecenter/support.php";
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(url));
					startActivity(i);
				} else {
					if (!f.exists())
					{
						mHandler.post(popupProgressDialog);
						new Thread(new Runnable(){
							public void run()
							{
								String userphotoPath = Global.SdcardPath_inbox + "photo_" + mIdx + "b.jpg";
								String remotefile = "profiles/photo_" + mIdx + ".jpg";
								int success = 0;
								int count = 0;
								do {
									MyNet net = new MyNet(FunctionActivity.this);
									success = net.Download(remotefile, userphotoPath, AireJupiter.myLocalPhpServer);
									if (success==1||success==0)
										break;
									MyUtil.Sleep(500);
								} while (++count < 2);
								
								if (success!=1)
								{
									count=0;
									do {
										MyNet net = new MyNet(FunctionActivity.this);
										success = net.Download(remotefile, userphotoPath, null);
										if (success==1||success==0)
											break;
										MyUtil.Sleep(500);
									} while (++count < 2);
								}
								
								mHandler.post(dismissProgressDialog);
								
								if (success==1)
								{
									File f = new File(userphotoPath);
									if (f.exists())
									{
										Intent i = new Intent(FunctionActivity.this,MessageDetailActivity.class);
										i.putExtra("imagePath", userphotoPath);
										i.putExtra("displayname", mNickname);
										i.putExtra("address", mAddress);
										startActivity(i);
									}
								}
							}
						}).start();
					}
					else
					{
						Intent i = new Intent(FunctionActivity.this,MessageDetailActivity.class);
						i.putExtra("imagePath", userphotoPath);
						i.putExtra("displayname", mNickname);
						i.putExtra("address", mAddress);
						startActivity(i);
					}
				}
			}
		});
		
		if (mContactId>0)
		{
			String AireNickname=getIntent().getStringExtra("AireNickname");
			if (AireNickname!=null && AireNickname.length()>0)
				((TextView)findViewById(R.id.displayname)).setText(mNickname+" ("+AireNickname+")");
			else
				((TextView)findViewById(R.id.displayname)).setText(mNickname);
		}
		else
			((TextView)findViewById(R.id.displayname)).setText(mNickname);
		
		if (inGroup)
		{
			mPhotoView.setBackgroundResource(R.drawable.group_bg);
			mGroupID=Integer.parseInt(mAddress.substring(9));
			GroupDB mGDB=new GroupDB(this);
			mGDB.open(true);
			sendeeList=mGDB.getGroupMembersByGroupIdx(mGroupID);
			mGDB.close();
			
			if (inCorpGroup)
			{
				mPhotoView.setVisibility(View.INVISIBLE);
				for (int i=0;i<sendeeList.size();i++)
				{
					Map<String, Object> map = new HashMap<String, Object>();
					
					int idx=Integer.parseInt(sendeeList.get(i));
					String address=mADB.getAddressByIdx(idx);
					String displayname=mADB.getNicknameByIdx(idx);
					
					Drawable drawable=ImageUtil.getBigRoundedUserPhoto(FunctionActivity.this, idx);
					if (drawable==null)
						drawable=getResources().getDrawable(R.drawable.bighead);
						
					map.put("idx", idx);
					map.put("address", address);
					map.put("displayname", displayname);
					map.put("photo", drawable);
					map.put("status", ContactsOnline.getContactOnlineStatus(address));
					memberList.add(map);
				}
			}
		}
		
		
		String mood=mADB.getMoodByAddress(mAddress);
		if (mood!=null && mood.length()>0)
			((TextView)findViewById(R.id.mood)).setText(mood);
		else
			((TextView)findViewById(R.id.mood)).setVisibility(View.GONE);
		
//		((ImageView)findViewById(R.id.close)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				finish();
//			}
//		});
		
		((Button)findViewById(R.id.chat)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//tml*** beta ui
				//tml*** support
//				if ((mAddress.equals("support") && mNickname.equals("Support")) || mIdx == 2) {
//					String url = "http://www.airecenter.com/m_supportrequest.php";
//					Intent i = new Intent(Intent.ACTION_VIEW);
//					i.setData(Uri.parse(url));
//					startActivity(i);
//				} else {
					if (getIntent().getBooleanExtra("fromConversation",false))
					{
						finish();
						return;
					}
					updateFriendLastContactTime();
					Intent it=new Intent(FunctionActivity.this, ConversationActivity.class);
					it.putExtra("SendeeContactId", mContactId);
					it.putExtra("SendeeNumber", mAddress);
					it.putExtra("SendeeDisplayname", mNickname);
					startActivity(it);
					finish();
//				}
			}
		});
		((Button) findViewById(R.id.videocall)).requestFocus();  //tml*** prefocus
		
		((Button)findViewById(R.id.call)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//tml*** beta ui
				//tml*** support
				if ((mAddress.equals("support") && mNickname.equals("Support")) || mIdx == 2) {
					String url = "http://airetalk.com/airecenter/faq.php";
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(url));
					startActivity(i);
				} else {
					updateFriendLastContactTime();
					finish();
					MakeCall.Call(FunctionActivity.this, mAddress, false);
				}
			}
		});
		
		if (!mPref.readBoolean("video_support", true))
			((Button)findViewById(R.id.videocall)).setEnabled(false);
		else
			((Button)findViewById(R.id.videocall)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					//tml*** beta ui
					//tml*** support
					if (AireVenus.callstate_AV == null) {
//						if ((mAddress.equals("support") && mNickname.equals("Support")) || mIdx == 2) {
//							String url = "http://airetalk.com/airecenter/manual.php";
//							Intent i = new Intent(Intent.ACTION_VIEW);
//							i.setData(Uri.parse(url));
//							startActivity(i);
//						} else {
							updateFriendLastContactTime();
							finish();
							MakeCall.Call(FunctionActivity.this, mAddress, true, false);
//						}
					}
				}
			});

		if (AireJupiter.getInstance() != null) {
			if (AireJupiter.getInstance().tcpSocket != null) {
				AireJupiter.getInstance().tcpSocket.keepAlive(false);
			}
		}
		//tml*** auto answer
//		mPref.write("autoAnswer2:" + mAddress, true);
//		mPref.delect("autoAnswer2:" + mAddress);
//		Intent intent = new Intent();
//		intent.setAction(Global.Action_Refresh_Gallery);
//		sendBroadcast(intent);
		//tml*** secret test
		if (mPref.readBoolean("TESTING", false) && Log.enDEBUG) {
			((ImageView)findViewById(R.id.photo)).setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					String atip = ContactsOnline.getContactSipIP(mAddress);
					if (atip == null) atip = "---";
					String content = mNickname + "\n" + mAddress + "\n" + mIdx + "  h." + Integer.toHexString(mIdx)
							+ "\n" + mContactId + "\n" + atip;
					Intent it = new Intent(FunctionActivity.this, Tooltip.class);
		            it.putExtra("Content", content);
		            startActivity(it);
					return true;
				}
			});
		}
		//tml*** beta ui
		//tml*** support
		if ((mAddress.equals("support") && mNickname.equals("Support")) || mIdx == 2) {
//			((Button) findViewById(R.id.chat)).setText(getResources().getString(R.string.helper_name1));
//			((Button) findViewById(R.id.chat))
//					.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
			((Button) findViewById(R.id.call)).setText(getResources().getString(R.string.helper_faq));
			((Button) findViewById(R.id.call))
					.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
//			((Button) findViewById(R.id.videocall)).setText(getResources().getString(R.string.helper_manual));
//			((Button) findViewById(R.id.videocall))
//					.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
		}
		if (DialerActivity.getDialer() != null) {
			((Button) findViewById(R.id.call)).setAlpha(50);
			((Button) findViewById(R.id.call)).setEnabled(false);
			((Button) findViewById(R.id.videocall)).setAlpha(50);
			((Button) findViewById(R.id.videocall)).setEnabled(false);
		}
		//***tml
	}
	
	private void updateFriendLastContactTime()
	{
		if (mADB.isOpen())
		{
			mADB.updateLastContactTimeByIdx(mIdx, new Date().getTime());
			if (UserPage.sortMethod==1)
				UserPage.forceRefresh=true;
		}
	}
	
	
	Runnable popupProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				progress = ProgressDialog.show(FunctionActivity.this, "", getString(R.string.in_progress), true, true);
			}catch(Exception e){}
		}
	};
	
	Runnable dismissProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				if (progress.isShowing())
					progress.dismiss();
			}catch(Exception e){}
		}
	};
	
	protected void onDestroy() {
		if(mADB!=null && mADB.isOpen()) 
			mADB.close();
		System.gc();
		super.onDestroy();
	};
	
	
	Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			Toast.makeText(FunctionActivity.this, msg.arg1,
					Toast.LENGTH_LONG).show();
		};
	};
	//tml*** reenable mapview
	public static void onLaunchMapView(Context context, long longitude, long latitude, long time, 
			String displayName, String sendee,long mContactid)
	{
		Intent intent=new Intent(context, MapViewLocation.class);
        intent.putExtra("DisplayName", displayName);
		intent.putExtra("latitude", latitude);
		intent.putExtra("longitude", longitude);
		intent.putExtra("trackable", true);
        intent.putExtra("time", time);
		intent.putExtra("Address", sendee);
		context.startActivity(intent);
	}
	
	public static void onLaunchStaticMapView(Context context, long longitude, long latitude,
			long mylongitude, long mylatitude, long time, 
			String displayName, String sendee,long mContactid, boolean showMeOnly, String iso)
	{	
		if (AmazonKindle.IsKindle())
		{
			Intent it = new Intent(context, CommonDialog.class);
			it.putExtra("msgContent", context.getResources().getString(R.string.nonsupport_googlemap));
			it.putExtra("numItems", 1);
			it.putExtra("ItemCaption0", context.getResources().getString(R.string.cancel));
			it.putExtra("ItemResult0", RESULT_CANCELED);
			context.startActivity(it);
			return;
		}
		
		if (showMeOnly)
		{
			Intent intent=new Intent(context, SelfMapView.class);
	        intent.putExtra("DisplayName", displayName);
			intent.putExtra("latitude", latitude);
			intent.putExtra("longitude", longitude);
	        intent.putExtra("time", time);
			intent.putExtra("Address", sendee);
			context.startActivity(intent);
		}
		else{
			//tml*** reenable mapview
			if (!iso.equals("cn") && MyUtil.hasGoogleMap(false, null)) {
				Intent intent=new Intent(context, MapViewLocation.class);
		        intent.putExtra("DisplayName", displayName);
				intent.putExtra("latitude", latitude);
				intent.putExtra("longitude", longitude);
				intent.putExtra("trackable", false);
				intent.putExtra("mylatitude", mylatitude);
				intent.putExtra("mylongitude", mylongitude);
		        intent.putExtra("time", time);
				intent.putExtra("Address", sendee);
				context.startActivity(intent);
			} else {
				Intent intent=new Intent(context, BDMapViewLocation.class);
		        intent.putExtra("DisplayName", displayName);
				intent.putExtra("latitude", latitude);
				intent.putExtra("longitude", longitude);
				intent.putExtra("trackable", false);
				intent.putExtra("mylatitude", mylatitude);
				intent.putExtra("mylongitude", mylongitude);
		        intent.putExtra("time", time);
				intent.putExtra("Address", sendee);
				context.startActivity(intent);
			}
		}
	}
	//***tml
	@Override
	public void onResume() {
		super.onResume();
//		MobclickAgent.onResume(this);
	}
	
	@Override
	public void onPause() {
//		MobclickAgent.onPause(this);
		if (!isFinishing()) finish();  //tml, can cause strange activity history if not finished
		super.onPause();
	}
	
	//tml|bj*** neverdie
	public void neverSayNeverDie(Context context) {
		if (AireJupiter.getInstance()==null) {
			Log.e("AireJupiter.getInstance() is null, RESETTING");
			Intent vip0 = new Intent(context, BeeHiveService.class);
			context.stopService(vip0);
			Intent vip1 = new Intent(context, AireVenus.class);
			context.stopService(vip1);
			Intent vip2 = new Intent(context, AireJupiter.class);
			context.stopService(vip2);
			
			Intent vip00 = new Intent(context, AireJupiter.class);
			context.startService(vip00);
		}
	}
}
