package com.pingshow.amper;

import java.io.File;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.GroupDB;
import com.pingshow.amper.db.SmsDB;
import com.pingshow.amper.map.MapViewLocation;
import com.pingshow.amper.map.SelfMapView;
import com.pingshow.amper.map.bd.MyBDLocation;
import com.pingshow.network.MyNet;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;
import com.pingshow.util.ResizeImage;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;
import com.pingshow.voip.core.VoipCore;

public class FunctionActivity extends Activity {
	private String mAddress;
	private int mIdx;
	private long mContactId;
	private String mNickname;
	private AmpUserDB mADB;
	private ProgressDialog progress = null;
	private MyPreference mPref;
	private int myIdx = 0;
	private boolean bMySelf = false;
	
	private String myPhoneNumber;
	private ImageView mPhotoView;
	
	private int mAttached=0;
	private String mMsgText="";
	private String SrcAudioPath;
	private String SrcImagePath;
	private String SrcVideoPath;
	private long rowid;
	private boolean extended;
	private boolean largeScreen=false;
	private boolean inGroup=false;
	private int mGroupID;
	private boolean mFromConv = false;
	private boolean mFromGroup = false;
	float mDensity = 1.f;
	private boolean mCallMode = false;
	private boolean isSTB = false;
	
	private ArrayList<String> sendeeList = new ArrayList<String>();
	
	@SuppressLint("NewApi") @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.func_dialog);
		
		neverSayNeverDie(FunctionActivity.this);  //tml|bj*** neverdie/
		
		mDensity = getResources().getDisplayMetrics().density;
		largeScreen=(findViewById(R.id.large)!=null);
		
		WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
	    if (!largeScreen)
	    	lp.width=(int)(320.f*mDensity);
		lp.dimAmount = 0.5f;
		getWindow().setAttributes(lp);
		
		mAddress=getIntent().getStringExtra("Address");
		mNickname=getIntent().getStringExtra("Nickname");
		mIdx=getIntent().getIntExtra("Idx",0);
		mContactId=getIntent().getLongExtra("Contact_id",-20);
		mCallMode = getIntent().getBooleanExtra("CallMode", false);  //tml*** chatview
		mFromConv = getIntent().getBooleanExtra("fromConversation", false);  //tml*** beta ui
		mFromGroup = getIntent().getBooleanExtra("fromGroup", false);
		Log.i("FUNCT_D addr=" + mAddress + " nick=" + mNickname + " idx=" + mIdx
				+ " cid=" + mContactId + " call=" + mCallMode + " conv=" + mFromConv);

		//tml*** detect stb
		if (mAddress.startsWith(Global.STB_HeaderName + Global.STB_Name1)) {
			if (mAddress.length() == Global.STB_NameLength)
				isSTB = true;
		}
		
		mPref=new MyPreference(this);
		myPhoneNumber=mPref.read("myPhoneNumber","++++");
		mADB = new AmpUserDB(this);
		mADB.open();
		
		//tml*** dev control
		try{
			myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
			bMySelf = (myIdx == mIdx);
		} catch (Exception e) {}
		//***tml
		
		inGroup=mAddress.startsWith("[<GROUP>]");
		
		Drawable photo;
		mPhotoView=((ImageView)findViewById(R.id.photo));
		photo=ImageUtil.getBigRoundedUserPhoto(this, mIdx);
		if (photo!=null)
			mPhotoView.setImageDrawable(photo);
		else
			mPhotoView.setImageResource(inGroup?R.drawable.group_empty:R.drawable.bighead);
		
//		if (inGroup) {
		if (inGroup || mCallMode) {  //tml*** chatview
			mPhotoView.setClickable(false);
		} else
			mPhotoView.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					//tml*** temp alpha ui
					if ((mAddress.equals("support") && mNickname.equals("Support")) || mIdx == 2) {
						String url = "http://airetalk.com";
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse(url));
						startActivity(i);
					} else {
						Intent i = new Intent(FunctionActivity.this, TimeLine.class);
						i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						i.putExtra("displayname", mNickname);
						i.putExtra("address", mAddress);
						i.putExtra("Idx", mIdx);
						startActivity(i);
					}
				}
			});
		
		/*
		mPhotoView.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				String userphotoPath = Global.SdcardPath_inbox + "photo_" + mIdx + "b.jpg";
				File f = new File(userphotoPath);
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
		});*/
		
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
			
			ImageView mEdit=((ImageView)findViewById(R.id.edit));
			mEdit.setVisibility(View.VISIBLE);
			mEdit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent it=new Intent(FunctionActivity.this, PickupActivity.class);
					it.putExtra("Exclude", sendeeList);
					startActivityForResult(it, 108);
				}
			});
			
			arrangePickedUsers();
		}

		mPref.delect("MonitorWho");  //tml*** monitor record/
		int maxSuvei = Global.MAX_SUVS;
		boolean found=false;
		for (int i=0;i<maxSuvei;i++)
		{
			String address;
			if ((address=mPref.read("Suvei"+i))!=null)
			{
				if (address.equals(mAddress))
				{
					mPref.write("MonitorWho", mAddress);
					found=true;
					break;
				}
			}
		}
		((ImageView)findViewById(R.id.guard)).setVisibility(found?View.VISIBLE:View.GONE);
		((ImageView)findViewById(R.id.guard)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it = new Intent(FunctionActivity.this, CommonDialog.class);
				it.putExtra("msgContent", String.format(getString(R.string.send_suvei), mNickname));
				it.putExtra("numItems", 3);
				it.putExtra("ItemCaption0", getString(R.string.cancel));
				it.putExtra("ItemResult0", RESULT_CANCELED);
				it.putExtra("ItemCaption1", getString(R.string.stop));
				it.putExtra("ItemResult1", CommonDialog.STOP_SUV);
				it.putExtra("ItemCaption2", getString(R.string.start));
				it.putExtra("ItemResult2", RESULT_OK);
				startActivityForResult(it, 131);
			}
		});
		//tml*** multi suvei
		((ImageView)findViewById(R.id.guard)).setOnLongClickListener(new OnLongClickListener() {
			@Override
	        public boolean onLongClick(View v) {
				String warning = "";
				Log.i("SuvDelWARN " + mAddress);
				if (mPref.readBoolean("SuvDelWARN" + mAddress, false)) {  //last contact, warning msg
					warning = "\n\n" + getString(R.string.suvdelwarming);
				}
				Intent it = new Intent(FunctionActivity.this, CommonDialog.class);
				it.putExtra("msgContent", mNickname + "\n" + getString(R.string.delsuv) + warning);
				it.putExtra("numItems", 2);
				it.putExtra("ItemCaption0", getString(R.string.cancel));
				it.putExtra("ItemResult0", RESULT_CANCELED);
				it.putExtra("ItemCaption1", getString(R.string.yes));
				it.putExtra("ItemResult1", RESULT_OK);
				startActivityForResult(it, 139);
				return true;
			}
		});
		
		String mood=mADB.getMoodByAddress(mAddress);
		if (mood!=null && mood.length()>0)
			((TextView)findViewById(R.id.mood)).setText(mood);
		else
			((TextView)findViewById(R.id.mood)).setVisibility(View.GONE);
		
		((ImageView)findViewById(R.id.chat)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mFromConv)
				{
					finish();
					return;
				}
				
				updateFriendLastContactTime();
				Intent it=new Intent(FunctionActivity.this, ConversationActivity.class);
				it.putExtra("SendeeContactId", mContactId);
				it.putExtra("SendeeNumber", mAddress);
				it.putExtra("SendeeDisplayname", mNickname);
				it.putExtra("fromGroup", mFromGroup);
				startActivity(it);
				finish();
			}
		});
		
		((ImageView)findViewById(R.id.close)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		((ImageView)findViewById(R.id.call)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
				if (inGroup)
				{
					try{
						
						if (sendeeList.size()>0 && sendeeList.size()<=9)
						{
							AireVenus.setCallType(AireVenus.CALLTYPE_CHATROOM);
							mPref.write("incomingChatroom",false);

							new Thread(sendNotifyForJoinChatroom).start();
							
							int myIdx=0;
							try {
								myIdx=Integer.parseInt(mPref.read("myID","0"),16);
								mPref.write("ChatroomHostIdx", myIdx);
							} catch (Exception e) {}
							
							String idx = "" + myIdx;
							MakeCall.ConferenceCall(getApplicationContext(), idx);
						} else {
							if (sendeeList.size() > 9)
								Toast.makeText(getApplicationContext(), "Conference does not support more than 9 people", Toast.LENGTH_LONG).show();
						}
						
					}catch(Exception e){}
				}
				else
					MakeCall.Call(FunctionActivity.this, mAddress, false);
			}
		});
		
		if (!AmazonKindle.canHandleCameraIntent(FunctionActivity.this)){
			mPref.write("video_support", false);
			((TextView)findViewById(R.id.videocall_desc)).setEnabled(false);
		}
		
		if (AmazonKindle.IsKindle())
		{
			((ImageView)findViewById(R.id.location)).setEnabled(false);
			((TextView)findViewById(R.id.locate_desc)).setEnabled(false);
			
			if (!AmazonKindle.hasMicrophone(FunctionActivity.this))
			{
				((ImageView)findViewById(R.id.call)).setEnabled(false);
				((TextView)findViewById(R.id.call_desc)).setEnabled(false);
				
				((ImageView)findViewById(R.id.videocall)).setEnabled(false);
				((TextView)findViewById(R.id.videocall_desc)).setEnabled(false);
				
				((ImageView)findViewById(R.id.voicemsg)).setEnabled(false);
				((TextView)findViewById(R.id.voice_desc)).setEnabled(false);
				
				((ImageView)findViewById(R.id.walkietalkie)).setEnabled(false);
				((TextView)findViewById(R.id.walkietalkie_desc)).setEnabled(false);
			}
		}
		
		if (!mPref.readBoolean("video_support", true))
			((ImageView)findViewById(R.id.videocall)).setEnabled(false);
		else{
			((ImageView)findViewById(R.id.videocall)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
					MakeCall.Call(FunctionActivity.this, mAddress, true, false);
				}
			});
		}
		
		((ImageView)findViewById(R.id.walkietalkie)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
				if (!MyUtil.checkNetwork(FunctionActivity.this))
					return;
				Intent it=new Intent(FunctionActivity.this, WalkieTalkieDialog.class);
				it.putExtra("Contact_id", mContactId);
				it.putExtra("Address", mAddress);
				it.putExtra("Idx", mIdx);
				startActivity(it);
			}
		});
		
		((ImageView)findViewById(R.id.voicemsg)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!MyUtil.checkSDCard(FunctionActivity.this))
					return;
				
				if (!AmazonKindle.hasMicrophone(FunctionActivity.this))
				{
					finish();
					return;
				}
				
				if (mFromConv) {
					ConversationActivity.getInstance().onVoiceSMS();
					finish();
				} else {
					updateFriendLastContactTime();
					
					Intent it = new Intent(FunctionActivity.this,
							VoiceRecordingDialog.class);
					SrcAudioPath = Global.SdcardPath_sent + ConversationActivity.getRandomName() + ".amr";
//					SrcAudioPath = Global.SdcardPath_sent + ConversationActivity.getRandomName() + ".mp3";  //tml*** new vmsg
					it.putExtra("path", SrcAudioPath);
					startActivityForResult(it, 27);
				}
			}
		});
		
		((ImageView)findViewById(R.id.picmsg)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!MyUtil.checkSDCard(FunctionActivity.this))
					return;
				if (mFromConv) {
//					ConversationActivity.getInstance().onPickPicture();
					ConversationActivity.getInstance().onPickPictureOption();
					finish();
				} else {
					onPickPictureOption();
				}
			}
		});
		
		((ImageView)findViewById(R.id.file)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mFromConv) {
					ConversationActivity.getInstance().onFileTransfer();
					finish();
				} else {
					startActivityForResult(new Intent(FunctionActivity.this, FileBrowerActivity.class),20);
				}
			}
		});
		
		
		
		if(mIdx<50){
			((ImageView)findViewById(R.id.location)).setImageResource(R.drawable.func_loc_2);
		}
		else{
			long endtime = mPref.readLong(mAddress, 0);
			if (new Date().getTime() / 1000 < endtime) // Whether send the request for sharing
				((ImageView)findViewById(R.id.location)).setImageResource(R.drawable.func_loc_2);
		}
		
		((ImageView)findViewById(R.id.location)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mFromConv) {
					ConversationActivity.getInstance().sendLocation();
					finish();
				} else {
//					try {
//	    				Class.forName("com.google.android.maps.MapActivity");
//					} catch (ClassNotFoundException e) {
//						Toast.makeText(FunctionActivity.this, R.string.nonsupport_googlemap, Toast.LENGTH_LONG).show();
//					    return;
//					} catch (NoClassDefFoundError e) {
//						Toast.makeText(FunctionActivity.this, R.string.nonsupport_googlemap, Toast.LENGTH_LONG).show();
//					    return;
//					}
					//tml*** check google map
					boolean isCN = mPref.read("iso").equals("cn");
					boolean hasGoogleMaps = MyUtil.hasGoogleMap(!isCN, getApplicationContext(), "func location");
					if (!hasGoogleMaps & !isCN) {
						return;
					}
					
					mHandler.post(popupProgressDialog);
					(new Thread(new Runnable() {
						public void run() {
							if (onQueryLocation()==1)
								finish();
						}
					})).start();
				}
			}
		});
		
		if (inGroup)
		{
			((ImageView)findViewById(R.id.videocall)).setEnabled(false);
			((ImageView)findViewById(R.id.walkietalkie)).setEnabled(false);
			((ImageView)findViewById(R.id.location)).setEnabled(false);
		}
		
		if (isSTB) {  //tml*** detect STB
			((ImageView)findViewById(R.id.walkietalkie)).setEnabled(false);
		}
		
		if (!mCallMode) {
			extended=mPref.readBoolean("funcExtended",true);
			((LinearLayout)findViewById(R.id.func_2)).setVisibility(extended?View.VISIBLE:View.GONE);
			((LinearLayout)findViewById(R.id.func_1_desc)).setVisibility(extended?View.VISIBLE:View.GONE);
			((LinearLayout)findViewById(R.id.func_2_desc)).setVisibility(extended?View.VISIBLE:View.GONE);
		}
		
		((ImageView)findViewById(R.id.extend)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				extended=!extended;
				if (extended)
				{
					AnimationSet as = new AnimationSet(false);
					AlphaAnimation aa = new AlphaAnimation(0,1);
					TranslateAnimation ta = new TranslateAnimation(0,0,-80,0);
					aa.setDuration(300);
					ta.setDuration(300);
					as.addAnimation(aa);
					as.addAnimation(ta);
					as.setDuration(300);
					((LinearLayout)findViewById(R.id.func_2)).startAnimation(as);
					((LinearLayout)findViewById(R.id.func_2_desc)).startAnimation(as);
				}
				
				((LinearLayout)findViewById(R.id.func_1_desc)).setVisibility(extended?View.VISIBLE:View.GONE);
				((LinearLayout)findViewById(R.id.func_2_desc)).setVisibility(extended?View.VISIBLE:View.GONE);
				((LinearLayout)findViewById(R.id.func_2)).setVisibility(extended?View.VISIBLE:View.GONE);
				mPref.write("funcExtended", extended);
			}
		});
		
		if (AireJupiter.getInstance() != null) {
			if (AireJupiter.getInstance().tcpSocket != null) {
				AireJupiter.getInstance().tcpSocket.keepAlive(false);
			}
		}

		//tml*** secret test
		if (mPref.readBoolean("TESTING", false) && Log.enDEBUG) {
			((ImageView) findViewById(R.id.location)).setOnLongClickListener(new OnLongClickListener() {
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
		if (mFromConv) {
			((ImageView) findViewById(R.id.chat)).setAlpha(0.2f);
			((ImageView) findViewById(R.id.chat)).setEnabled(false);
		}
		//tml*** friend invite
//		boolean reInv = mPref.readBoolean("Inviting:" + mAddress, false);
//		if (reInv) {
//			((ImageView) findViewById(R.id.call)).setAlpha(0.2f);
//			((ImageView) findViewById(R.id.call)).setEnabled(false);
//			((ImageView) findViewById(R.id.videocall)).setAlpha(0.2f);
//			((ImageView) findViewById(R.id.videocall)).setEnabled(false);
//			((ImageView) findViewById(R.id.chat)).setAlpha(0.2f);
//			((ImageView) findViewById(R.id.chat)).setEnabled(false);
//			((ImageView) findViewById(R.id.walkietalkie)).setAlpha(0.2f);
//			((ImageView) findViewById(R.id.walkietalkie)).setEnabled(false);
//			((LinearLayout) findViewById(R.id.func_1_desc)).setVisibility(View.INVISIBLE);
//			((ImageView) findViewById(R.id.voicemsg)).setAlpha(0.2f);
//			((ImageView) findViewById(R.id.voicemsg)).setEnabled(false);
//			((ImageView) findViewById(R.id.picmsg)).setAlpha(0.2f);
//			((ImageView) findViewById(R.id.picmsg)).setEnabled(false);
//			((ImageView) findViewById(R.id.file)).setAlpha(0.2f);
//			((ImageView) findViewById(R.id.file)).setEnabled(false);
//			((ImageView) findViewById(R.id.location)).setAlpha(0.2f);
//			((ImageView) findViewById(R.id.location)).setEnabled(false);
//			((LinearLayout) findViewById(R.id.func_2_desc)).setVisibility(View.INVISIBLE);
//			((RelativeLayout) findViewById(R.id.reinvite)).setVisibility(View.VISIBLE);
//			
//			((Button) findViewById(R.id.readd)).setOnClickListener(new OnClickListener() {
//				@Override
//				public void onClick(View v) {
//					try {
//						Log.e("tml SENDING ADDF REQUEST.retry");
//						int myIdx = Integer.parseInt(mPref.read("myID","0"),16);
//						SendAgent agent = new SendAgent(FunctionActivity.this, myIdx, 0, false);
//						agent.onSend(mAddress, Global.Hi_AddFriend1, 0, null, null, true);
//					} catch (Exception e) {}
//				}
//			});
//		}
		//tml*** chatview
		if (mCallMode) {
			((ImageView) findViewById(R.id.voicemsg)).setAlpha(0.2f);
			((ImageView) findViewById(R.id.voicemsg)).setEnabled(false);
			((ImageView)findViewById(R.id.extend)).setVisibility(View.GONE);
			((ImageView) findViewById(R.id.guard)).setVisibility(View.GONE);
			((LinearLayout) findViewById(R.id.func_1)).setVisibility(View.GONE);
			((LinearLayout) findViewById(R.id.func_1_desc)).setVisibility(View.GONE);
			((LinearLayout) findViewById(R.id.callpanel)).setVisibility(View.VISIBLE);

			((ToggleButton) findViewById(R.id.mute)).setOnClickListener(new OnClickListener () {
				@Override
				public void onClick(View v) {
					boolean isChecked = ((ToggleButton) v).isChecked();
					VoipCore vp = AireVenus.instance().getVoipCore();
					if (isChecked) {
						vp.muteMic(true);
						mPref.write("chatCallMute", true);
					} else {
						vp.muteMic(false);
						mPref.write("chatCallMute", false);
					}
//					Log.e("FUNC-CALL MUTE " + !isChecked);
				}
			});
			boolean mute = mPref.readBoolean("chatCallMute", false);
			if (mute) ((ToggleButton) findViewById(R.id.mute)).setChecked(true);

			((ToggleButton) findViewById(R.id.speaker)).setOnClickListener(new OnClickListener () {
				@Override
				public void onClick(View v) {
					boolean isChecked = ((ToggleButton) v).isChecked();
					if (isChecked) {
						DialerActivity.getDialer().routeAudioToSpeaker();
						mPref.write("chatCallSpkr", true);
					} else {
						DialerActivity.getDialer().routeAudioToReceiver();
						mPref.write("chatCallSpkr", false);
					}
//					Log.e("FUNC-CALL SPKR " + !isChecked);
				}
			});
			boolean spkr = mPref.readBoolean("chatCallSpkr", false);
			if (spkr) ((ToggleButton) findViewById(R.id.speaker)).setChecked(true);
//			((ImageView) findViewById(R.id.location)).setVisibility(View.GONE);
//			((TextView) findViewById(R.id.locate_desc)).setVisibility(View.GONE);
		}
		//tml*** dev control
		if ((mAddress.equals("news_service") && mNickname.equals("Hot News")) || mIdx == 4) {
			if (!bMySelf) {
				((ImageView) findViewById(R.id.extend)).setVisibility(View.GONE);
				((ImageView) findViewById(R.id.guard)).setVisibility(View.GONE);
//				((LinearLayout) findViewById(R.id.func_1)).setVisibility(View.GONE);
//				((LinearLayout) findViewById(R.id.func_1_desc)).setVisibility(View.GONE);
				((LinearLayout) findViewById(R.id.func_2)).setVisibility(View.GONE);
				((LinearLayout) findViewById(R.id.func_2_desc)).setVisibility(View.GONE);
				((ImageView) findViewById(R.id.call)).setVisibility(View.GONE);
				((TextView) findViewById(R.id.call_desc)).setVisibility(View.GONE);
				((ImageView) findViewById(R.id.videocall)).setVisibility(View.GONE);
				((TextView) findViewById(R.id.videocall_desc)).setVisibility(View.GONE);
	//			((ImageView) findViewById(R.id.chat)).setVisibility(View.GONE);
	//			((TextView) findViewById(R.id.chat_desc)).setVisibility(View.GONE);
				((ImageView) findViewById(R.id.walkietalkie)).setVisibility(View.GONE);
				((TextView) findViewById(R.id.walkietalkie_desc)).setVisibility(View.GONE);
//				((ImageView) findViewById(R.id.voicemsg)).setVisibility(View.GONE);
//				((TextView) findViewById(R.id.voice_desc)).setVisibility(View.GONE);
//				((ImageView) findViewById(R.id.picmsg)).setVisibility(View.GONE);
//				((TextView) findViewById(R.id.pic_desc)).setVisibility(View.GONE);
//				((ImageView) findViewById(R.id.file)).setVisibility(View.GONE);
//				((TextView) findViewById(R.id.file_desc)).setVisibility(View.GONE);
//				((ImageView) findViewById(R.id.location)).setVisibility(View.GONE);
//				((TextView) findViewById(R.id.locate_desc)).setVisibility(View.GONE);
			}
		} else if ((mAddress.equals("support") && mNickname.equals("Support")) || mIdx == 2) {
			if (!bMySelf) {
//				((ImageView) findViewById(R.id.extend)).setVisibility(View.GONE);
//				((ImageView) findViewById(R.id.guard)).setVisibility(View.GONE);
//				((LinearLayout) findViewById(R.id.func_1)).setVisibility(View.GONE);
//				((LinearLayout) findViewById(R.id.func_1_desc)).setVisibility(View.GONE);
//				((LinearLayout) findViewById(R.id.func_2)).setVisibility(View.GONE);
//				((LinearLayout) findViewById(R.id.func_2_desc)).setVisibility(View.GONE);
			}
		}
		//***tml
	}
	
	private void updateFriendLastContactTime()
	{
		if (UsersActivity.sortMethod==1)
		{
			if (mADB.isOpen())
			{
				mADB.updateLastContactTimeByIdx(mIdx, new Date().getTime());
				UsersActivity.forceRefresh=true;
			}
		}
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
			
			for(int i=0; i<sendeeList.size(); i++)
			{
				int idx=Integer.parseInt(sendeeList.get(i));
				if (idx<50) continue;
				
				String address=mADB.getAddressByIdx(idx);
				
				if (AireJupiter.getInstance()!=null && AireJupiter.getInstance().tcpSocket!=null)
				{
					if (AireJupiter.getInstance().tcpSocket.isLogged(false))
					{
						if (i>0) MyUtil.Sleep(500);
						Log.d("voip.inviteConf1 " + address + " " + content);
						AireJupiter.getInstance().tcpSocket.send(address, content, 0, null, null, 0, null);
					}
				}
			}
		}
	};
	private void onPickPicture() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(Intent.createChooser(intent,
				getString(R.string.choose_photo_source)), 1);
	}
	
	private void onPickVideo() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("video/*");
		startActivityForResult(Intent.createChooser(intent,
				getString(R.string.chose_file)), 101);
	}
	
	private Uri outputFileUri;
	
	private void onTakePicture() {
		if (!AmazonKindle.canHandleCameraIntent(this)){
			Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
			return;
		}
		try{
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			File file = new File(Global.SdcardPath_sent + "tmp.jpg");
			outputFileUri = Uri.fromFile(file);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
			startActivityForResult(intent, 3);
		}catch(Exception e){
			Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
		}
	}
	//tml|li*** uri null yota fix
	public String getPath(Uri uri, int requestCode) {
		try {
			Log.e("getPath2 uriPath=" + uri.getPath() + " string=" + uri.toString());
			if (uri.toString().startsWith("content:")) {
				String result = null;
				Uri contentUri;
				Cursor cursor;
				String[] column = { MediaStore.Images.Media.DATA };
				if (requestCode == 101) {
					String uriStr = uri.toString();
					String id = null;
					String sel = MediaStore.Images.Media._ID + "=?";
					if (uriStr.contains("%3A")) {
						id = uriStr.split("%3A")[1];
						contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
						Log.e("getPath2 cursor1: contentUri=" + contentUri.toString() + " sel=" + sel + " id=" + id);
						cursor = getContentResolver().query(contentUri, column, sel, new String[]{ id }, null);
					} else {
						contentUri = uri;
						Log.e("getPath2 cursor2: contentUri=" + contentUri.toString());
						cursor = getContentResolver().query(contentUri, column, null, null, null);
					}
				} else {
					contentUri = uri;
					Log.e("getPath2 cursor3: contentUri=" + contentUri.toString() + " column=" + column[0]);
					cursor = getContentResolver().query(contentUri, column, null, null, null);
				}
				
				if (cursor == null) {
			        result = uri.getPath();
			    } else { 
			        cursor.moveToFirst();
			        int idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
			        if (idx >= 0) {
			        	result = cursor.getString(idx);
			        	cursor.close();
			        }
			    }
		        Log.e("getPath2 result=" + result);
				return result;
			} else if (uri.toString().startsWith("file:")) {
				String uriStr = uri.toString();
				if (uriStr.contains("sdcard")) {
					return uriStr.substring(uriStr.indexOf("sdcard"));
				} else if (uriStr.contains("storage")) {
					return uriStr.substring(uriStr.indexOf("storage"));
				}
			}
		} catch(Exception e) {
			Log.e("getPath2 !@#$ " + e.getMessage());
		}
		return null;
	}
	
	public String getPath(Uri uri) {
		if (uri.toString().startsWith("content:")) {
			String[] projection = { MediaStore.Images.Media.DATA };
			Cursor cursor = managedQuery(uri, projection, null, null, null);
			int column_index = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			String path = cursor.getString(column_index);
			return path;
		} else if (uri.toString().startsWith("file:")) {
			String uriStr = uri.toString();
			return uriStr.substring(uriStr.indexOf("sdcard"));
		}
		return "";
	}
	
	private void onPickPictureOption() {
		final CharSequence[] items = {
				getResources().getString(R.string.photo_gallery),
				getResources().getString(R.string.takepicture),
				getResources().getString(R.string.videomemo)
				};
		final CharSequence[] items_noCamera = {
				getResources().getString(R.string.photo_gallery)};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (!AmazonKindle.canHandleCameraIntent(this)){
			builder.setItems(items_noCamera, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
					if (item == 0) onPickPicture();
					dialog.dismiss();
				}
			});
		}
		else{
			builder.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
					if (item == 0)
						onPickPicture();
					else if (item == 1)
						onTakePicture();
					else if (item == 2)
						onPickVideo();
					dialog.dismiss();
				}
			});
		}
		builder.setTitle(FunctionActivity.this.getResources().getString(
				R.string.choose_photo_source));
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
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
		Log.e("*** FUNCT_D *** DESTROY ***");
		super.onDestroy();
	};
	
	private int onQueryLocation() {
		if(mIdx<50){
			mHandler.post(dismissProgressDialog);
			onLaunchMapView(FunctionActivity.this, -122033418, 37309488,
					new Date().getTime(), mNickname, mAddress, mContactId);
			return 1;
		}
		
		String Return="";
		long pos_time=0;
		int errorCode = 0;
		int lat = 0, lon = 0;
		long endtime = mPref.readLong(mAddress, 0);
		Message msg = new Message();
		
		if (new Date().getTime() / 1000 - endtime > 0) // Whether send the request for sharing
		{
			try {
				int count=0;
				do{
					MyNet net = new MyNet(FunctionActivity.this);
					Return = net.doPost("querysharing.php",
							"queryid=" + URLEncoder.encode(mAddress,"UTF-8") +
							"&id=" + URLEncoder.encode(myPhoneNumber,"UTF-8"), null);
				}while((Return.length()==0||Return.startsWith("Error")) && ++count<3);
				
				mHandler.post(dismissProgressDialog);
				
				if (Return.length() == 0) {
					msg.arg1 = R.string.nonetwork;
					mHandler.sendMessage(msg);
					return 1;
				} else if (Return.startsWith("NonMember")) {
					msg.arg1 = R.string.nonmember_no_service;
					mHandler.sendMessage(msg);
					return 1;
				} else {
					int relation = Integer.parseInt(Return);
					if (relation == 0 || new Date().getTime() / 1000 - endtime > 0)
					{
						Intent it = new Intent(FunctionActivity.this,
								CommonDialog.class);
						it.putExtra("msgContent",
								getString(R.string.request_location_sharing));
						it.putExtra("numItems", 2);
						it.putExtra("ItemCaption0", getString(R.string.cancel));
						it.putExtra("ItemResult0", RESULT_CANCELED);
						it.putExtra("ItemCaption1", getString(R.string.yes));
						it.putExtra("ItemResult1", RESULT_OK);
						startActivityForResult(it, 7);
						return 0;
					}
				}
			} catch (Exception e) {
				msg.arg1 = R.string.nonetwork;
				mHandler.sendMessage(msg);
				return 1;
			}
		}
		
		mHandler.post(dismissProgressDialog);

		try {
			if (AireJupiter.getInstance() == null) {
				msg.arg1 = R.string.nonetwork;
				mHandler.sendMessage(msg);
				return 1;
			}
			Return = AireJupiter.getInstance().getFriendLocation(mAddress);
		} catch (Exception e) {
			msg.arg1 = R.string.nonetwork;
			mHandler.sendMessage(msg);
			return 1;
		}
		if (Return == null) {
			msg.arg1 = R.string.nonetwork;
			mHandler.sendMessage(msg);
			return 1;
		}

		String items[] = Return.split("/");
		if (items.length>=3)
		{
			try {
				lat = Integer.parseInt(items[0])+3512113;
				lon = Integer.parseInt(items[1])-10958121;
				pos_time = Long.parseLong(items[2], 16);
				pos_time *= 1000;
			} catch (NumberFormatException e) {
				errorCode = -1;
				lat=0;
				lon=0;
			}
		}

		if (errorCode < 0) {
			msg.arg1 = R.string.location_not_found;
			mHandler.sendMessage(msg);
		} else {
			onLaunchMapView(FunctionActivity.this, lon, lat,
					pos_time, mNickname, mAddress, mContactId);
		}
		return 1;
	}
	
	private Intent mPickedIntent;
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 109) { //user agees to add Member
			if (resultCode == RESULT_OK) {
				new Thread(addNewMembersInGroup).start();
			}
		}
		else if (requestCode == 108) { //invite more users
			if (resultCode == RESULT_OK) {
				mPickedIntent=data;
				Intent it = new Intent(FunctionActivity.this, CommonDialog.class);
				it.putExtra("msgContent",getString(R.string.add_new_member_in_group));
				it.putExtra("numItems", 2);
				it.putExtra("ItemCaption0", getString(R.string.cancel));
				it.putExtra("ItemResult0", RESULT_CANCELED);
				it.putExtra("ItemCaption1", getString(R.string.yes));
				it.putExtra("ItemResult1", RESULT_OK);
				startActivityForResult(it, 109);
			}
		}else if (requestCode == 7) {
			if (resultCode == RESULT_OK) {
				mAttached = 0;
				mMsgText = "[<LOCATIONSHARING>]";
				onSendMessage(true);
				finish();
			}
		}else if (requestCode == 20) { // show file attach icon
			if (resultCode == RESULT_OK) {
				SrcVideoPath = data.getStringExtra("filePath");
				
				mAttached = 8;
				SrcAudioPath = SrcVideoPath;
				onSendMessage(false);
				/*
				if (inGroup)
				{
					mAttached = 8;
					SrcAudioPath = SrcVideoPath;
					onSendMessage(false);
				}else{
					mPref.write("FileTransferP2P",SrcVideoPath);
					MakeCall.FileTransferCall(FunctionActivity.this, mAddress);
				}*/
				updateFriendLastContactTime();
				finish();
			}
		}else if (requestCode == 27) { // voice memo
			if (resultCode == RESULT_OK) {
				mAttached = 1;
				SrcVideoPath = null;
				int voicetime = 60 - data.getIntExtra("voicetime", 60);	
				onSendMessage(false, voicetime);
				finish();
			} else {
				new File(SrcAudioPath).delete();
			}
		}else if (requestCode == 1 || requestCode == 3){
			if (resultCode == RESULT_OK) {
				if (requestCode == 1) {
					if (null==data.getData()) return;
					Uri selectedImageUri = data.getData();
					SrcImagePath = getPath(selectedImageUri);
				}
				else if (requestCode == 3)
					SrcImagePath = Global.SdcardPath_sent + "tmp.jpg";
				Log.d("onActivityResult:SrcImagePath==="+SrcImagePath);
				mAttached = 2;// image
				String filename = Global.SdcardPath_sent + ConversationActivity.getRandomName() + ".jpg";
				
				if (SrcImagePath==null)
				{
					int result = ResizeImage.saveFromStream(this, data, filename, 1280, 1280, 95);
					if (result == -1) {
						Toast.makeText(this, R.string.img_error, Toast.LENGTH_SHORT)
								.show();
						return;
					}
				}
				else{
					int result = ResizeImage.Resize(this, SrcImagePath, filename, 1280, 1280, 95);
					if (result == -1) {
						Toast.makeText(this, R.string.img_error, Toast.LENGTH_SHORT)
								.show();
						return;
					}
				}
				SrcImagePath = filename;
				onSendMessage(false);
				updateFriendLastContactTime();
				finish();
			}
		}
		else if (requestCode == 101){
			if (resultCode == RESULT_OK) {
				mAttached = 8;// file
				
				if (null==data.getData()) return;
				Uri selectedImageUri = data.getData();
				SrcVideoPath = getPath(selectedImageUri);
				//tml|li*** uri null yota fix
				if (SrcVideoPath == null || !(SrcVideoPath.length() > 0)) {
					SrcVideoPath = getPath(selectedImageUri, requestCode);
				}
				
				if (SrcVideoPath == null) {
					Toast.makeText(getApplicationContext(), getString(R.string.file_transfer_error), Toast.LENGTH_LONG).show();
				} else {
					onSendMessage(false);
				}
				updateFriendLastContactTime();
				finish();
			}
		}
		else if (requestCode == 131)
		{
			if (resultCode == RESULT_OK) {
				if (AireJupiter.getInstance()!=null && AireJupiter.getInstance().tcpSocket.isLogged(false)){
					AireJupiter.getInstance().tcpSocket.send(mAddress,"GUARD", 0, null, null, 0, null);
				}
			}else if (resultCode == CommonDialog.STOP_SUV) {
				if (AireJupiter.getInstance()!=null && AireJupiter.getInstance().tcpSocket.isLogged(false)){
					AireJupiter.getInstance().tcpSocket.send(mAddress,"GUARD REST", 0, null, null, 0, null);
				}
			}
		} else if (requestCode == 139) {  //tml*** multi suvei
			if (resultCode == RESULT_OK) {
				int maxSuvei = Global.MAX_SUVS;
				
				for (int j = 0; j < maxSuvei; j++) {
					String addr = mPref.read("Suvei" + j);
					if (addr.equals(mAddress)) {
						mPref.delect("Suvei" + j);
					    Intent intent = new Intent();
				        intent.setAction(Global.Action_Refresh_Gallery);
				        sendBroadcast(intent);
						if (AireJupiter.getInstance() != null
								&& AireJupiter.getInstance().tcpSocket.isLogged(false)) {
							mHandler.post(new Runnable () {
								@Override
								public void run() {
									if (mPref.readBoolean("SuvDelWARN" + mAddress, false)) {  //confirmed del
										AireJupiter.getInstance().tcpSocket.send(mAddress, "REVOKE my GUARD access FINAL", 0, null, null, 0, null);
									} else {  //del access
										AireJupiter.getInstance().tcpSocket.send(mAddress, "REVOKE my GUARD access", 0, null, null, 0, null);
									}
								}
							});
						}
						mPref.delect("SuvDelWARN" + mAddress);
						((ImageView) findViewById(R.id.guard)).setVisibility(View.GONE);
//						Log.e("tmlsuv del.Suvei[" + j + "]=" + mAddress);
						break;
					}
				}
			}
		}
	}
	
	private void onSendMessage(boolean dontPutInSmsDB)
	{
		onSendMessage(dontPutInSmsDB, 0);
	}
	
	private void onSendMessage(boolean dontPutInSmsDB, int duration) {
		boolean ret=false;
		SendAgent agent=null;
		SendFileAgent fileAgent=null;
		ArrayList<String> addressList=new ArrayList<String>();
		
		if (inGroup)
		{
			try{
				for (int i=0;i<sendeeList.size();i++)
					addressList.add(mADB.getAddressByIdx(Integer.parseInt(sendeeList.get(i))));
			}catch(Exception e){}
		}
		
		if (mMsgText.length() == 0) {
			if ((mAttached & 1) == 1)
				mMsgText += "(Vm)"+duration;
			if ((mAttached & 2) == 2)
				mMsgText += "(iMG)";
		}
		
		if (mAttached == 8) {
			SrcAudioPath = SrcVideoPath;
			SrcImagePath=null;
			File file = new File(SrcAudioPath);
			NumberFormat format = DecimalFormat.getInstance();
			format.setMaximumFractionDigits(2);
			String length = format.format(file.length() / 1024.0).replace(",", "");
			if (Double.valueOf(length) > 102400) { // 100M
				Toast.makeText(getApplicationContext(),
						getString(R.string.fileLarge), Toast.LENGTH_SHORT)
						.show();
				return;
			}
			mMsgText = "(fl)" + length + " KB";
			
			try{
				int myIdx=Integer.parseInt(mPref.read("myID","0"),16);
				fileAgent = new SendFileAgent(this, myIdx, true);
				if (inGroup)
				{
					fileAgent.setAsGroup(mGroupID);
					ret=fileAgent.onMultipleSend(addressList, mMsgText, mAttached, SrcAudioPath, SrcImagePath);
				}else
					ret=fileAgent.onSend(mAddress, mMsgText, mAttached, SrcAudioPath, SrcImagePath, false);
			}catch(Exception e){}
		}
		else{
			try{
				int myIdx=Integer.parseInt(mPref.read("myID","0"),16);
				int idx=mADB.getIdxByAddress(mAddress);
	
				agent = new SendAgent(this, myIdx, idx, true);
				
				if (inGroup)
				{
					agent.setAsGroup(mGroupID);
					ret=agent.onMultipleSend(addressList, mMsgText, mAttached, SrcAudioPath, SrcImagePath);
				}
				else{
					ret=agent.onSend(mAddress, mMsgText, mAttached, SrcAudioPath,
						SrcImagePath, false);
				}
			}catch(Exception e){}
		}
		
		if (ret && !dontPutInSmsDB) 
		{
			if (mAttached==8)
				mMsgText = getString(R.string.filememo_send) + " " + mMsgText;
			
			SMS msg = new SMS();
			msg.displayname = mNickname;
			msg.address = mAddress;
			msg.content = mMsgText;
			msg.contactid = mContactId;
			msg.read = 1;
			msg.type = 2;
			msg.status = 2;// pending
			msg.time = new Date().getTime();

			msg.attached = mAttached;
			if ((mAttached & 1) == 1)
				msg.att_path_aud = SrcAudioPath;
			if ((mAttached & 2) == 2)
				msg.att_path_img = SrcImagePath;
			if (mAttached==8)
			{
				msg.att_path_aud = SrcAudioPath;
				msg.att_path_img = null;
			}
			
			SmsDB mDB = new SmsDB(this);
			mDB.open();

			msg.longitudeE6 = mPref.readLong("longitude", 0);
			msg.latitudeE6 = mPref.readLong("latitude", 0);

			if (mMsgText.startsWith("[<LOCATIONSHARING>]")) {
				mMsgText = getResources().getString(R.string.ask_share);
				msg.content = mMsgText;
			}
			rowid = mDB.insertMessage(mAddress,
					msg.contactid, (new Date()).getTime(), 1, msg.status,
					msg.type, "", msg.content, msg.attached,
					msg.att_path_aud, msg.att_path_img, 0, msg.longitudeE6,
					msg.latitudeE6, 0, null, null, 0);

			if (agent!=null)
				agent.setRowId(rowid);
			else
				fileAgent.setRowId(rowid);

			mDB.close();// alec
		}
		
		mAttached = 0;// alec
		mMsgText = "";

		Toast.makeText(this, R.string.msg_is_sent, Toast.LENGTH_SHORT).show();
	}
	
	Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			Toast.makeText(FunctionActivity.this, msg.arg1,
					Toast.LENGTH_LONG).show();
		};
	};
	
	public static void onLaunchMapView(Context context, long longitude, long latitude, long time, 
			String displayName, String sendee, long mContactid)
	{
		//tml|xwf*** baidu map
		Intent intent;
		MyPreference mPref = new MyPreference(context);
		String iso = mPref.read("iso", "cn");
		//tml*** check google map
		boolean isCN = iso.equals("cn");
		boolean hasGoogleMaps = MyUtil.hasGoogleMap(!isCN, context, "onLaunchMapView");
		if (isCN) {
			intent = new Intent(context, MyBDLocation.class);
		} else if (hasGoogleMaps) {
			intent = new Intent(context, MapViewLocation.class);
		} else {
			return;
		}
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
			String displayName, String sendee,long mContactid, boolean showMeOnly)
	{	
		MyPreference mPref = new MyPreference(context);
		String iso = mPref.read("iso", "cn");
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

		//tml*** check google map
		boolean isCN = iso.equals("cn");
		boolean hasGoogleMaps = MyUtil.hasGoogleMap(!isCN, context, "onLaunchStaticMapView");
		
		if (showMeOnly)
		{
			if (hasGoogleMaps) {
				Intent intent=new Intent(context, SelfMapView.class);
		        intent.putExtra("DisplayName", displayName);
				intent.putExtra("latitude", latitude);
				intent.putExtra("longitude", longitude);
		        intent.putExtra("time", time);
				intent.putExtra("Address", sendee);
				context.startActivity(intent);
			}
		} else {
//			Intent intent=new Intent(context, MapViewLocation.class);
//	        intent.putExtra("DisplayName", displayName);
//			intent.putExtra("latitude", latitude);
//			intent.putExtra("longitude", longitude);
//			intent.putExtra("trackable", false);
//			intent.putExtra("mylatitude", mylatitude);
//			intent.putExtra("mylongitude", mylongitude);
//	        intent.putExtra("time", time);
//			intent.putExtra("Address", sendee);
//			context.startActivity(intent);
			//xwf*** baidu map
			if (!isCN) {
				if (hasGoogleMaps) {
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
				}
			} else {
				Intent intent=new Intent(context, MyBDLocation.class);
		        intent.putExtra("DisplayName", displayName);
				intent.putExtra("latitude", latitude);
				intent.putExtra("longitude", longitude);
				intent.putExtra("trackable", true);
				intent.putExtra("mylatitude", mylatitude);
				intent.putExtra("mylongitude", mylongitude);
		        intent.putExtra("time", time);
				intent.putExtra("Address", sendee);
				context.startActivity(intent);
			}
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
//		MobclickAgent.onResume(this);
	}
	
	@Override
	public void onPause() {
//		MobclickAgent.onPause(this);
		super.onPause();
	}
	
	private void arrangePickedUsers()
	{
		if (sendeeList==null || sendeeList.size()==0) return;
		
		RelativeLayout s=(RelativeLayout)findViewById(R.id.members);
		s.removeAllViews();
		
		try{
			
			int count=sendeeList.size();
			int width=(int)((float)s.getWidth()/mDensity)-(largeScreen?60:45);
			if (width<0) {
				int w=getWindowManager().getDefaultDisplay().getWidth();
				width = (int)((float)w/mDensity)-(largeScreen?45:30);
			}
			int space=width/count;
			if (largeScreen)
			{
				if (space>110) space=110;
			}else{
				if (space>100) space=100;
			}
			
			for(int i=0;i<count;i++)
			{
				ImageView a=new ImageView(this);
				a.setBackgroundResource(R.drawable.empty);
				if (largeScreen)
					a.setPadding((int)(mDensity*8), (int)(mDensity*8), (int)(mDensity*8), (int)(mDensity*8));
				else
					a.setPadding((int)(mDensity*5), (int)(mDensity*5), (int)(mDensity*5), (int)(mDensity*5));
				a.setClickable(true);
				int idx=Integer.parseInt(sendeeList.get(i));
				String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
				
				Drawable photo=ImageUtil.getBitmapAsRoundCorner(userphotoPath,1,4);
				if (photo!=null)
					a.setImageDrawable(photo);
				else
					a.setImageResource(R.drawable.bighead);
				
				RelativeLayout.LayoutParams lp=null;
				if (largeScreen)
					lp = new RelativeLayout.LayoutParams((int)(mDensity*90), (int)(mDensity*90));
				else
					lp = new RelativeLayout.LayoutParams((int)(mDensity*60), (int)(mDensity*60));
				lp.addRule(RelativeLayout.CENTER_IN_PARENT);
				s.addView(a, lp);
				
				if (i<count)
				{
					AnimationSet as = new AnimationSet(false);
				    as.setInterpolator(new AccelerateInterpolator());
					TranslateAnimation ta = new TranslateAnimation(mDensity*space*(count-i)*(((i%2)==0)?-1:1),0,0,0);
					ta.setDuration(1000+100*(count-i));
					as.addAnimation(ta);
					AlphaAnimation aa = new AlphaAnimation(1f,0f);
					ta.setDuration(1500);
					as.addAnimation(aa);
					as.setDuration(1500);
					a.startAnimation(as);
				}
			}
		}catch(Exception e){}
		
		mHandler.postDelayed(new Runnable(){
			public void run(){
				RelativeLayout s=(RelativeLayout)findViewById(R.id.members);
				if (s!=null)
					s.removeAllViews();
			}
		}, 1500);
	}
	
	Runnable addNewMembersInGroup=new Runnable()
	{
		public void run()
		{
			try{
				String idxArray=mPickedIntent.getStringExtra("idx");
				String [] items=idxArray.split(" ");
				String newMemberArray="";
				
				int myIdx=Integer.parseInt(mPref.read("myID","0"),16);
				SendAgent agent=new SendAgent(FunctionActivity.this, myIdx, 0, true);
				
				agent.setAsGroup(mGroupID);
				ArrayList<String> addressList=new ArrayList<String>();
				try{
					for(int i=0;i<items.length;i++)
					{
						int idx=Integer.parseInt(items[i]);
						for (String id:sendeeList)
							addressList.add(mADB.getAddressByIdx(Integer.parseInt(id)));
						agent.onMultipleSend(addressList, ":-o$_$"+idx, 0, null, null);
						MyUtil.Sleep(1000);
					}
				}catch(Exception e){}
				
				GroupDB gdb=new GroupDB(FunctionActivity.this);
				gdb.open();
				for(int i=0;i<items.length;i++)
				{
					int idx=Integer.parseInt(items[i]);
					if (idx<50) continue;
					sendeeList.add(items[i]);
					if (i==0)
						newMemberArray=newMemberArray+items[i];
					else
						newMemberArray=newMemberArray+","+items[i];
					gdb.insertGroup(mGroupID, mNickname, idx);
				}
				
				String Return="";
				int c=0;
				do {
					MyNet net = new MyNet(FunctionActivity.this);
					Return = net.doPostHttps("add_group_member.php", "id=" + mGroupID
    						+"&members=" + newMemberArray, null);
					if (Return.startsWith("Done"))
						break;
					MyUtil.Sleep(2500);
				} while (++c < 3);
				
				gdb.close();
				
				try{
					addressList.clear();
					for (String id:items)
						addressList.add(mADB.getAddressByIdx(Integer.parseInt(id)));
					agent.onMultipleSend(addressList, ":)(Y)", 0, null, null);
				}catch(Exception e){}
				
				mHandler.post(new Runnable(){
					public void run()
					{
						arrangePickedUsers();
					}
				});
			}catch(Exception e){}
		}
	};

	//tml|bj*** neverdie
	public void neverSayNeverDie(Context context) {
		if (AireJupiter.getInstance()==null) {
			Log.e("AireJupiter is NULL");
			Intent vip1 = new Intent(context, AireVenus.class);
			context.stopService(vip1);
			Intent vip2 = new Intent(context, AireJupiter.class);
			context.stopService(vip2);
			
			Intent vip00 = new Intent(context, AireJupiter.class);
			context.startService(vip00);
		}
	}
	
}
