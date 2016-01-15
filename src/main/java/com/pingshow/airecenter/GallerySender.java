package com.pingshow.airecenter;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.GroupDB;
import com.pingshow.airecenter.db.SmsDB;
import com.pingshow.beehive.BeeHiveService;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.ResizeImage;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;

public class GallerySender extends Activity {
	
	private UserItemAdapter gridAdapter;
	private List<Map<String, String>> amperList;
	private AsyncImageLoader asyncImageLoader;
	private int numColumns=5;
	private GridView resultGridView;
	private AmpUserDB mADB;
	private ContactsQuery cq;
	private float mDensity=1.0f;
	private MyPreference mPref;
	
	private String mAddress;
	private String mNickname;
	private int mAttached=0;
	private String mMsgText="";
	private String SrcAudioPath;
	private String SrcImagePath;
	private String SrcVideoPath;
	private long rowid;
	
	private boolean inGroup=false;
	private int mGroupID;
	
	static public GallerySender _this;
	
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gallery_sender_main);
		neverSayNeverDie(this);  //tml|bj*** neverdie/
		
		mPref = new MyPreference(this);
		mDensity = getResources().getDisplayMetrics().density;

	    mADB = new AmpUserDB(this);
		mADB.open();
		
//		cq = new ContactsQuery(MainActivity._this);
		cq = new ContactsQuery(this);  //tml*** gallery share crash/
		
	    amperList = new ArrayList<Map<String, String>>();
	    
//	    gridAdapter = new UserItemAdapter(MainActivity._this);
	    gridAdapter = new UserItemAdapter(this);  //tml*** gallery share crash/
	    
	    resultGridView = (GridView)findViewById(R.id.friendsGridView);
	    resultGridView.setNumColumns(numColumns);
	    
	    resultGridView.setAdapter(gridAdapter);
	    resultGridView.setOnItemClickListener(onChooseUser);
	    
	    mHandler.post(mFetchFriends);
	    
	    LinearLayout oncallView=(LinearLayout) findViewById(R.id.oncall);
	    oncallView.setVisibility(View.GONE);
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View oncall = inflater.inflate(R.layout.inflate_oncall, null, false);
		oncallView.addView(oncall);
		
		((ImageView)oncall.findViewById(R.id.hangup)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (DialerActivity.getDialer()!=null)
				{
					DialerActivity.getDialer().hangupCall();
					((LinearLayout) findViewById(R.id.oncall)).setVisibility(View.GONE);
				}
			}
		});
	    
	    _this=this;
	}
	
	OnItemClickListener onChooseUser=new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			
			Map<String, String> map = amperList.get(position);
			mAddress=map.get("address");
			mNickname=map.get("displayName");
			String idxStr=map.get("idx");
			int idx=Integer.parseInt(idxStr);
			
			Intent it = new Intent(GallerySender.this, CommonDialog.class);

			it.putExtra("Address", mAddress);
			it.putExtra("Nickname", mNickname);
			it.putExtra("Idx", idx);
			it.putExtra("msgContent", String.format(getString(R.string.send_picture_to), mNickname));
			it.putExtra("numItems", 2);
			it.putExtra("ItemCaption0", getString(R.string.no));
			it.putExtra("ItemResult0", 0);
			it.putExtra("ItemCaption1", getString(R.string.yes));
			it.putExtra("ItemResult1", RESULT_OK);
			
			startActivityForResult(it, 100);
		}
	};
	
	Runnable mFetchFriends=new Runnable(){
		public void run()
		{
			amperList.clear();
			Cursor c = mADB.fetchAllByTime();
			if (c!=null && c.moveToFirst())
			{
				do {
					String address = c.getString(1);
					if (address.startsWith("[<GROUP>]")) continue;
					int idx=c.getInt(3);
					if (idx<50) continue;
					
					long contactId = cq.getContactIdByNumber(address);
					String disName="";
					String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
//					File f = new File(userphotoPath);
//					if (!f.exists()) userphotoPath=null;
					//tml*** userphoto fix
					if (!new File(userphotoPath).exists()) {
						userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + "b.jpg";
						if (!new File(userphotoPath).exists())
							userphotoPath = null;
					}
					
					if (contactId>0)
						disName=cq.getNameByContactId(contactId);
					else
						disName = c.getString(4);
					
					if (disName==null || disName.length()==0)
						disName=String.valueOf((R.string.unknown_person));
					
					HashMap<String, String> map = new HashMap<String, String>();
					
					map.put("displayName", disName);
					map.put("address", address);
					map.put("idx", idx+"");
					map.put("checked", "0");
					map.put("imagePath", userphotoPath);
					
					amperList.add(map);
				}while(c.moveToNext());
				
				c.close();
			}
			
			mHandler.post(new Runnable(){
				public void run(){
					gridAdapter.notifyDataSetChanged();
				}
			});
		}
	};
	
	class foundViewHolder {
		TextView friendName;
		ImageView photoimage;
		ImageView checked;
	}
	
	public class UserItemAdapter extends BaseAdapter {
		Context icontext;

		public UserItemAdapter(Context context) {
			icontext = context;
			asyncImageLoader = new AsyncImageLoader(context);
		}

		@Override
		public int getCount() {
			int count=amperList.size();
			return count;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			Map<String, String> map=null;
			
			try{
				map = amperList.get(position);
			}catch(Exception e){
				return convertView;
			}
			
			String imagePath = map.get("imagePath");
			
			foundViewHolder holder;

			if (convertView == null) {
				holder = new foundViewHolder();
				convertView = View.inflate(icontext, R.layout.user_tiny_cell, null);
				
				holder.photoimage = (ImageView) convertView.findViewById(R.id.photo);
				holder.friendName = (TextView) convertView.findViewById(R.id.friendname);
				holder.checked = (ImageView) convertView.findViewById(R.id.checked);
				convertView.setTag(holder);
			} else {
				holder = (foundViewHolder) convertView.getTag();
			}
			
			holder.photoimage.setTag(imagePath);
			Drawable cachedImage = asyncImageLoader.loadDrawable(imagePath, new ImageCallback() {				
				public void imageLoaded(Drawable imageDrawable, String path) {
					ImageView imageViewByTag=null;
					imageViewByTag = (ImageView) resultGridView.findViewWithTag(path);
					if (imageViewByTag != null && imageDrawable!=null) {
						imageViewByTag.setImageDrawable(imageDrawable);
					}
				}
			});
			
			if (cachedImage != null && imagePath!=null)
				holder.photoimage.setImageDrawable(cachedImage);
			else
				holder.photoimage.setImageResource(R.drawable.bighead);
			
			String disname = map.get("displayName");
			holder.friendName.setText(disname);
			
			String address=map.get("address");
			int status=ContactsOnline.getContactOnlineStatus(address);
			if (status>0)
			{
//				Drawable d=MainActivity._this.getResources().getDrawable(R.drawable.online_light);
				Drawable d=getResources().getDrawable(R.drawable.online_light);  //tml*** gallery share crash/
				d.setBounds(0, 0, (int)(20.f*mDensity), (int)(20.f*mDensity));
				SpannableString spannable = new SpannableString("*"+disname);
				ImageSpan icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
				spannable.setSpan(icon, 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
				holder.friendName.setText(spannable);
			}
			
			String checked = map.get("checked");
			if (checked.equals("1"))
				holder.checked.setVisibility(View.VISIBLE);
			else
				holder.checked.setVisibility(View.INVISIBLE);
			
			return convertView;
		}
	}
	
	
	Runnable onSendMessage=new Runnable()
	{
		public void run()
		{
			boolean ret=false;
			SendAgent agent=null;
			SendFileAgent fileAgent=null;
			ArrayList<String> addressList=new ArrayList<String>();
			
			inGroup=mAddress.startsWith("[<GROUP>]");
			
			if (inGroup)
			{
				try{
					mGroupID=Integer.parseInt(mAddress.substring(9));
					GroupDB mGDB=new GroupDB(GallerySender.this);
					mGDB.open(true);
					ArrayList<String> sendeeList=mGDB.getGroupMembersByGroupIdx(mGroupID);
					mGDB.close();
					
					for (int i=0;i<sendeeList.size();i++)
						addressList.add(mADB.getAddressByIdx(Integer.parseInt(sendeeList.get(i))));
				}catch(Exception e){}
			}
			
			if (mMsgText.length() == 0) {
				if ((mAttached & 1) == 1)
					mMsgText += "(Vm)";
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
					mHandler.post(new Runnable(){
						public void run()
						{
							Toast.makeText(GallerySender.this, getString(R.string.fileLarge), Toast.LENGTH_SHORT).show();
						}
					});
					return;
				}
				mMsgText = "(fl)" + length + " KB";
				
				try{
					int myIdx=Integer.parseInt(mPref.read("myID","0"),16);
					fileAgent = new SendFileAgent(GallerySender.this, myIdx, true);
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
		
					agent = new SendAgent(GallerySender.this, myIdx, idx, true);
					
					if (inGroup)
					{
						agent.setAsGroup(mGroupID);
						ret=agent.onMultipleSend(addressList, mMsgText, mAttached, SrcAudioPath, SrcImagePath);
					}
					else{
						ret=agent.onSend(mAddress, mMsgText, mAttached, SrcAudioPath, SrcImagePath, false);
					}
				}catch(Exception e){}
			}
			
			if (ret) 
			{
				if (mAttached==8)
					mMsgText = getString(R.string.filememo_send) + " " + mMsgText;
				
				SMS msg = new SMS();
				msg.displayname = mNickname;
				msg.address = mAddress;
				msg.content = mMsgText;
				msg.contactid = cq.getContactIdByNumber(mAddress);
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
				
				SmsDB mDB = new SmsDB(GallerySender.this);
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
	
			mHandler.post(new Runnable(){
				public void run()
				{
					Toast.makeText(GallerySender.this, R.string.msg_is_sent, Toast.LENGTH_SHORT).show();
					finish();
				}
			});
		}
	};
	
	public String getPath(Uri uri) {
		if (uri.toString().startsWith("content:")) {
			String[] projection = { MediaStore.Images.Media.DATA };
			Cursor cursor = managedQuery(uri, projection, null, null, null);
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			String path = cursor.getString(column_index);
			return path;
		} else if (uri.toString().startsWith("file:")) {
			String uriStr = uri.toString();
			return uriStr.substring(uriStr.indexOf("sdcard"));
		}
		return "";
	}
	
	public static String getRandomName() {
		return ("" + new Date().getTime());
	}
	
	@Override  
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		super.onActivityResult(requestCode, resultCode, data);  
		if (requestCode==100 && resultCode==RESULT_OK)
		{
			String type = getIntent().getType();
			if (type.startsWith("image/"))
			{
				Uri imageUri = (Uri) getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
				SrcImagePath = getPath(imageUri);
				String filename = Global.SdcardPath_sent + getRandomName() + ".jpg";
				
				if (SrcImagePath==null)
				{
					int result = ResizeImage.saveFromStream(this, data, filename, 1280, 1280, 95);
					if (result == -1) {
						Toast.makeText(this, R.string.img_error, Toast.LENGTH_SHORT).show();
						return;
					}
				}
				else{
					int result = ResizeImage.Resize(this, SrcImagePath, filename, 1280, 1280, 95);
					if (result == -1) {
						Toast.makeText(this, R.string.img_error, Toast.LENGTH_SHORT).show();
						return;
					}
				}
				SrcImagePath = filename;
				
				mAttached = 2;// image
				new Thread(onSendMessage).start();
			}
			else if (type.startsWith("video/")){
				Uri imageUri = (Uri) getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
				SrcVideoPath = getPath(imageUri);
				mAttached = 8;// video/file
				new Thread(onSendMessage).start();
			}
			else {  //tml*** browser save
				Uri imageUri = (Uri) getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
				SrcVideoPath = getPath(imageUri);
				mAttached = 8;// file
				new Thread(onSendMessage).start();
			}
		}
	}

	@Override
	public void onDestroy() {
		if (mADB != null && mADB.isOpen())
			mADB.close();
		amperList.clear();
		_this=null;
		System.gc();
		System.gc();
		super.onDestroy();
	}
	
	@Override
    protected void onResume() {
    	super.onResume();
    	if (DialerActivity.minimized && DialerActivity.getDialer()!=null)
    	{
    		LinearLayout oncallView=(LinearLayout) findViewById(R.id.oncall);
    		((TextView)oncallView.findViewById(R.id.displayname)).setText(DialerActivity.getDialer().getCurrentOnCallName());
    		((TextView)oncallView.findViewById(R.id.status)).setText(DialerActivity.getDialer().getCurrentOnCallStatus());
    		oncallView.setVisibility(View.VISIBLE);
    		
    		mHandler.postDelayed(refreshStatus,1000);
    	}
	}
	
	Runnable refreshStatus=new Runnable()
	{
		public void run()
		{
			if (DialerActivity.minimized && DialerActivity.getDialer()!=null)
			{
				LinearLayout oncallView=(LinearLayout) findViewById(R.id.oncall);
	    		((TextView)oncallView.findViewById(R.id.displayname)).setText(DialerActivity.getDialer().getCurrentOnCallName());
	    		((TextView)oncallView.findViewById(R.id.status)).setText(DialerActivity.getDialer().getCurrentOnCallStatus());
				mHandler.postDelayed(refreshStatus,1000);
			}
			else
			{
				LinearLayout oncallView=(LinearLayout) findViewById(R.id.oncall);
				oncallView.setVisibility(View.GONE);
			}
		}
	};

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