package com.pingshow.airecenter;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.cons.Key;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.network.MyNet;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;
import com.pingshow.voip.VideoCallActivity;

public class PickupPage extends Page {
	
	private UserItemAdapter gridAdapter;
	private List<Map<String, String>> amperList;
	private AsyncImageLoader asyncImageLoader;
	private GridView resultGridView;
	private AmpUserDB mADB;
	private ContactsQuery cq;
	private int mCount=0;
	private float mDensity=1.0f;
	private boolean broadcastConf = false;
	
	static public MainActivity _this;
	
	private ArrayList<String> chatroomMemberslist = new ArrayList<String>();
	
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		}
	};
	
	private View layout;
	public PickupPage(View v) {
		Log.e("*** !!! CONFPAGE *** START START !!! ***");
		layout =v;
		mPref = new MyPreference(MainActivity._this);

	    mDensity = MainActivity._this.getResources().getDisplayMetrics().density;

	    mADB = new AmpUserDB(MainActivity._this);
		mADB.open();
		
		cq = new ContactsQuery(MainActivity._this);
		
	    amperList = new ArrayList<Map<String, String>>();
	    
	    gridAdapter = new UserItemAdapter(MainActivity._this);
	    
	    resultGridView = (GridView)layout.findViewById(R.id.friends);
	    
	    resultGridView.setAdapter(gridAdapter);
	    resultGridView.setOnItemClickListener(onChooseUser);
	    
	    if (MainActivity._this.getIntent().getBooleanExtra("conference", false))
	    	((TextView)layout.findViewById(R.id.topic)).setText(String.valueOf((R.string.conference)));
        
    	
        ((Button)layout.findViewById(R.id.done)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			broadcastConf = false;
    			int count=0;
    			//xwf
    			try {
					InetAddress[] allByName = InetAddress.getAllByName("www.xingfafa.com");
					for (InetAddress inetAddress : allByName) {
						Log.d("ip"+inetAddress.toString());
						System.out.println("访问ip:"+inetAddress);
					}
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}
    			
    			for (int i=0;i<amperList.size();i++)
    			{
    				Map<String, String> map = amperList.get(i);
    				if (map.get("checked").equals("1"))
    				{
    					int idx=Integer.parseInt(map.get("idx"));
    					if (idx<50) continue;
    					chatroomMemberslist.add(idx+"");
    					count++;
    				}
    			}
    			
    			if (count>0)
    			{
					if (chatroomMemberslist.size()>0 && chatroomMemberslist.size()<=9)
					{
						AireVenus.setCallType(AireVenus.CALLTYPE_CHATROOM);
						mPref.write("incomingChatroom",false);
						
						int myIdx=0;
						try {
							myIdx=Integer.parseInt(mPref.read("myID","0"),16);
							mPref.write("ChatroomHostIdx", myIdx);
						} catch (Exception e) {}
						
						String idx = "" + myIdx;
						MakeCall.ConferenceCall(MainActivity._this, idx,-1,false);
//						MakeCall.SipCall(MainActivity._this, dialNumber, MainActivity._this.getString(R.string.conference), false);
						new Thread(sendNotifyForJoinChatroom).start();
					}
    			}
    		}}
        );
        //tml*** broadcast
        ((Button)layout.findViewById(R.id.done_bcast)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			try {
    			//bree  kickall.php
    			MyNet net = new MyNet(AireApp.context);
    			String domain;
				if (mPref.readBoolean("incomingChatroom")) {
					domain = mPref.read("joinSipAddress",
							AireJupiter.myConfSipServer_default);
				} else {
					domain = mPref.read("conferenceSipServer",
							AireJupiter.myConfSipServer_default);
					if (AireJupiter.getInstance() != null) {
						domain = AireJupiter.getInstance().getIsoConf(domain); 
					}
				}
				String phpip = AireJupiter.myPhpServer_default;
				if (AireJupiter.getInstance() != null) {
					phpip = AireJupiter.getInstance().getIsoPhp(0, true,
							"74.3.165.66");
				}
    			
					String Return = net.doAnyPostHttp("http://" + phpip
							+ "/onair/conference/customer/kickall.php", "room="
							+ mPref.readInt("ChatroomHostIdx", 0) + "&ip=" + domain);
					Log.d("kickall.php Return=" + Return);
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
    			broadcastConf = true;
    			int count=0;
    			//xwf
    			try {
					InetAddress[] allByName = InetAddress.getAllByName("www.xingfafa.com");
					for (InetAddress inetAddress : allByName) {
						Log.d("ip"+inetAddress.toString());
						System.out.println("访问ip:"+inetAddress);
					}
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}
    			for (int i=0;i<amperList.size();i++)
    			{
    				Map<String, String> map = amperList.get(i);
    				if (map.get("checked").equals("1"))
    				{
    					int idx=Integer.parseInt(map.get("idx"));
    					if (idx<50) continue;
    					chatroomMemberslist.add(idx+"");
    					count++;
    				}
    			}
    			
    			if (count>0)
    			{
					if (chatroomMemberslist.size()>0 && chatroomMemberslist.size()<=9)
					{
						AireVenus.setCallType(AireVenus.CALLTYPE_CHATROOM);
						mPref.write("incomingChatroom",false);
						
						int myIdx=0;
						try {
							myIdx=Integer.parseInt(mPref.read("myID","0"),16);
							mPref.write("ChatroomHostIdx", myIdx);
						} catch (Exception e) {}
						
						String idx = "" + myIdx;
						MakeCall.ConferenceCall(MainActivity._this, idx,-1,false);
						
						new Thread(sendNotifyForJoinChatroom).start();
						mPref.write("isBrocasting", false);
					}
    			}
    		}}
        );
//        if (!mPref.readBoolean("BROADCAST", false))
        if (!true)
        	((Button)layout.findViewById(R.id.done_bcast)).setVisibility(View.GONE);
        
        mHandler.post(mFetchFriends);
        
        //tml*** beta ui
		if (DialerActivity.getDialer() != null) {
			((Button) layout.findViewById(R.id.done)).setAlpha(50);
			((Button) layout.findViewById(R.id.done)).setEnabled(false);
		}
	}
	
	Runnable finishPage=new Runnable()
	{
		public void run()
		{
			MainActivity._this.switchInflater(0);
		}
	};
	
	
	private MyPreference mPref;
	Runnable sendNotifyForJoinChatroom=new Runnable(){
		public void run()
		{	
			String myIdxHex=mPref.read("myID","0");
			mPref.write("isBrocasting", true);
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
			
			long now=new Date().getTime();

			if (AireJupiter.getInstance() != null)
				AireJupiter.getInstance().updateCallDebugStatus(true, null);
			for(int i=0; i<chatroomMemberslist.size(); i++)
			{
				int idx=Integer.parseInt(chatroomMemberslist.get(i));
				if (idx<50) continue;
				
				String address=mADB.getAddressByIdx(idx);
				
				mADB.updateLastContactTimeByAddress(address, now);
				
				if (AireJupiter.getInstance()!=null && AireJupiter.getInstance().tcpSocket()!=null)
				{
					if (AireJupiter.getInstance().isLogged())
					{
						if (i>0) MyUtil.Sleep(500);
						if (AireJupiter.getInstance() != null)
							AireJupiter.getInstance().updateCallDebugStatus(false, "\n>Conf " + address);
						Log.d("voip.inviteConf1 " + address + " " + ServerIP + " " + content);
						AireJupiter.getInstance().tcpSocket().send(address, content, 0, null, null, 0, null);
					}
				}
			}
			
			mHandler.postDelayed(finishPage, 2000);
		}
	};
	OnItemClickListener onChooseUser=new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			Map<String, String> map = amperList.get(position);
			if (map.get("checked").equals("0"))
			{
				if (mCount>=15) return;
				map.put("checked", "1");
				mCount++;
			}else{
				map.put("checked", "0");
				mCount--;
			}
			
			gridAdapter.notifyDataSetInvalidated();
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
					String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + "b.jpg";
//					File f = new File(userphotoPath);
//					if (!f.exists()) userphotoPath=null;
					//tml*** userphoto fix
					if (!new File(userphotoPath).exists()) {
						userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
						if (!new File(userphotoPath).exists())
							userphotoPath = null;
					}
					if (userphotoPath == null) {
						Log.w("null pic! " + address + " path=" + userphotoPath);
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
				Drawable d=MainActivity._this.getResources().getDrawable(R.drawable.online_light);
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

	@Override
	public void destroy() {
		if (mADB != null && mADB.isOpen())
			mADB.close();
		amperList.clear();
		System.gc();
		System.gc();
	}
	
}