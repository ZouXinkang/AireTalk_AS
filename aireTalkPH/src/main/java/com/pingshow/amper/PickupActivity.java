package com.pingshow.amper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;

public class PickupActivity extends Activity {
	
	private UserItemAdapter gridAdapter;
	private List<Map<String, String>> amperList;
	private AsyncImageLoader asyncImageLoader;
	private int numColumns=3;
	private GridView resultGridView;
	private AmpUserDB mADB;
	private ContactsQuery cq;
	private int mCount=0;
	private ArrayList<String> excludeList;
	private float mDensity=1.0f;

	private MyPreference mPref;
	private boolean isConference = false;
	private ArrayList<String> chatroomMemberslist = new ArrayList<String>();
	
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Toast.makeText(PickupActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
		}
	};
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.pickup);
	    this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

		mPref = new MyPreference(this);
		
	    neverSayNeverDie(PickupActivity.this);  //tml|bj*** neverdie/
	    
	    mDensity = getResources().getDisplayMetrics().density;
	    
	    mADB = new AmpUserDB(this);
		mADB.open();
		
		if (getResources().getConfiguration().orientation!=1)
			numColumns=4;
		
		cq = new ContactsQuery(this);
		
	    amperList = new ArrayList<Map<String, String>>();
	    
	    gridAdapter = new UserItemAdapter(this);
	    
	    resultGridView = (GridView) findViewById(R.id.friends);
	    resultGridView.setNumColumns(numColumns);
	    
	    excludeList=(ArrayList<String>)getIntent().getSerializableExtra("Exclude");
	    
	    resultGridView.setAdapter(gridAdapter);
	    resultGridView.setOnItemClickListener(onChooseUser);
	    
	    isConference = getIntent().getBooleanExtra("conference", false);
	    
	    if (isConference)
	    	((TextView)findViewById(R.id.topic)).setText(getString(R.string.conference));
	    
        ((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
				finish();
    		}}
        );
        ((ImageView)findViewById(R.id.done)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			StringBuffer idxBuffer = new StringBuffer("");
    			int count=0;
    			for (int i=0;i<amperList.size();i++)
    			{
    				Map<String, String> map = amperList.get(i);
    				if (map.get("checked").equals("1"))
    				{
    					idxBuffer.append(map.get("idx")+" ");
    					count++;
    				}
    			}
    			if (count>0)
    			{
	    			Intent it=new Intent();
	    			it.putExtra("idx", idxBuffer.toString());
	    			setResult(RESULT_OK,it);
    			}
				finish();
    		}}
        );
        //tml*** beta ui, conference
        if (isConference) {
    		((Button) findViewById(R.id.bFafauser)).setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				startActivity(new Intent(PickupActivity.this, UsersActivity.class));
    				finish();
    			}
    		});
            ((Button) findViewById(R.id.bMessage)).setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				startActivity(new Intent(PickupActivity.this, MessageActivity.class));
    				finish();
    			}
    		});
            ((Button) findViewById(R.id.bSearch)).setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				startActivity(new Intent(PickupActivity.this, PublicWalkieTalkie.class));
    				finish();
    			}
    		});
            ((Button) findViewById(R.id.bAireCall)).setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				startActivity(new Intent(PickupActivity.this, SipCallActivity.class));
    				finish();
    			}
    		});
            ((Button) findViewById(R.id.bSetting)).setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				startActivity(new Intent(PickupActivity.this, SettingActivity.class));
    				finish();
    			}
    		});
            //tml*** beta ui2
            if (mPref.read("iso", "cn").equals("cn")) {
            	((Button) findViewById(R.id.bSearch)).setVisibility(View.GONE);
            	((Button) findViewById(R.id.bSetting)).setVisibility(View.GONE);
            } else {
            	((Button) findViewById(R.id.bSearch)).setVisibility(View.GONE);
            	((Button) findViewById(R.id.bSetting)).setVisibility(View.GONE);
            }
            boolean largeScreen = (findViewById(R.id.large) != null);
            if (largeScreen) {
            	((Button) findViewById(R.id.bSetting)).setVisibility(View.GONE);
            }
            

            ((ImageView) findViewById(R.id.done_conf)).setOnClickListener(new OnClickListener() {
        		public void onClick(View v)
        		{
        			StringBuffer idxBuffer = new StringBuffer("");
        			int count=0;
        			for (int i=0;i<amperList.size();i++)
        			{
        				Map<String, String> map = amperList.get(i);
        				if (map.get("checked").equals("1"))
        				{
        					idxBuffer.append(map.get("idx")+" ");
        					count++;
        				}
        			}
        			if (count>0)
        			{
    					try{
    						chatroomMemberslist.clear();
    						String idxArray=idxBuffer.toString();
    						String [] items=idxArray.split(" ");
    						for(int i=0;i<items.length;i++)
    						{
    							int idx=Integer.parseInt(items[i]);
    							if (idx<50) continue;
    							chatroomMemberslist.add(items[i]);
    						}
    						
    						if (chatroomMemberslist.size()>0 && chatroomMemberslist.size()<=9)
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
    						}
    						
    					}catch(Exception e){}
        			}
        		}}
            );
        	((ImageView) findViewById(R.id.cancel)).setVisibility(View.GONE);
        	((ImageView) findViewById(R.id.done)).setVisibility(View.GONE);
        	((FrameLayout) findViewById(R.id.options)).setVisibility(View.VISIBLE);
        	((FrameLayout) findViewById(R.id.done_frame)).setVisibility(View.VISIBLE);
        	mPref.write("LastPage", 0);
        } else {
        	((ImageView) findViewById(R.id.cancel)).setVisibility(View.VISIBLE);
        	((ImageView) findViewById(R.id.done)).setVisibility(View.VISIBLE);
        	((FrameLayout) findViewById(R.id.options)).setVisibility(View.GONE);
        	((FrameLayout) findViewById(R.id.done_frame)).setVisibility(View.GONE);
        }

        mHandler.post(mFetchFriends);
	}
	
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
					if (excludeList!=null)//alec Exclude some users
					{
						boolean found=false;
						try{
							for (String a:excludeList)
							{
								if (Integer.parseInt(a)==idx){
									found=true;
									break;
								}
							}
						}catch(Exception e){}
						if (found) continue;
					}
					long contactId = cq.getContactIdByNumber(address);
					String disName="";
					String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
					File f = new File(userphotoPath);
					if (!f.exists()) userphotoPath=null;
					
					if (contactId>0)
						disName=cq.getNameByContactId(contactId);
					else
						disName = c.getString(4);
					
					if (disName==null || disName.length()==0)
						disName=getString(R.string.unknown_person);
					
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
	
	@Override
	protected void onDestroy() {
		if (mADB != null && mADB.isOpen())
			mADB.close();
		amperList.clear();
		System.gc();
		System.gc();
		super.onDestroy();
	}
	
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
				Drawable d=getResources().getDrawable(R.drawable.online_light);
				d.setBounds(0, 0, (int)(13.f*mDensity), (int)(13.f*mDensity));
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
	//tml*** beta ui, conference
	Runnable sendNotifyForJoinChatroom = new Runnable() {
		public void run() {
			String myIdxHex = mPref.read("myID", "0");

			String ServerIP = mPref.read("conferenceSipServer", AireJupiter.myConfSipServer_default);
			if (AireJupiter.getInstance() != null) {
				ServerIP = AireJupiter.getInstance().getIsoConf(ServerIP);  //tml*** china ip
			}
			long ip = MyUtil.ipToLong(ServerIP);
			String HexIP = Long.toHexString(ip);
			
			String content = Global.Call_Conference + "\n\n" + HexIP + "\n\n" + myIdxHex;
			
			for (int i = 0; i < chatroomMemberslist.size(); i++)
			{
				int idx = Integer.parseInt(chatroomMemberslist.get(i));
				if (idx < 50) continue;
				
				String address = mADB.getAddressByIdx(idx);
				
				if (AireJupiter.getInstance() != null && AireJupiter.getInstance().tcpSocket != null)
				{
					if (AireJupiter.getInstance().tcpSocket.isLogged(false))
					{
						if (i > 0) MyUtil.Sleep(500);
						Log.d("voip.inviteConf1 " + address + " " + ServerIP + " " + content);
						AireJupiter.getInstance().tcpSocket.send(address, content, 0, null, null, 0, null);
					}
				}
			}
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