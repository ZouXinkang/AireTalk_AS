package com.pingshow.airecenter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.GroupDB;
import com.pingshow.airecenter.view.PhotoGallery;
import com.pingshow.network.MyNet;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;

public class CorpMemberActivity extends Activity {
	private AmpUserDB mADB;
	private int mGroupID;
	float mDensity = 1.f;
	
	private PhotoGallery gallery;
	private ImageAdapter imageAdapter;
	private List<Map<String,Object>> memberList = new ArrayList<Map<String,Object>>();
	
	private ArrayList<String> sendeeList = new ArrayList<String>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.corp_member_dialog);
		
		mDensity = getResources().getDisplayMetrics().density;
			
		WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
	    lp.width=(int)(320.f*mDensity);
		lp.dimAmount = 0.5f;
		getWindow().setAttributes(lp);
		
		mADB = new AmpUserDB(this);
		mADB.open();
		
		((ImageView)findViewById(R.id.close)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		mGroupID=getIntent().getIntExtra("GroupID",0);
		GroupDB mGDB=new GroupDB(this);
		mGDB.open(true);
		sendeeList=mGDB.getGroupMembersByGroupIdx(mGroupID);
		
		((TextView)findViewById(R.id.group_name)).setText(mGDB.getGroupNameByGroupIdx(mGroupID));
		mGDB.close();
		
		for (int i=0;i<sendeeList.size();i++)
		{
			Map<String, Object> map = new HashMap<String, Object>();
			
			int idx=Integer.parseInt(sendeeList.get(i));
			String address=mADB.getAddressByIdx(idx);
			String displayname=mADB.getNicknameByIdx(idx);
			
			Drawable drawable=ImageUtil.getBigRoundedUserPhoto(CorpMemberActivity.this, idx);
			if (drawable==null)
				drawable=getResources().getDrawable(R.drawable.bighead);

			map.put("idx", ""+idx);
			map.put("address", address);
			map.put("displayname", displayname);
			map.put("photo", drawable);
			map.put("title", mADB.getInfoByIdx(idx,"title"));
			map.put("phone", mADB.getInfoByIdx(idx,"phone"));
			map.put("mobile", mADB.getInfoByIdx(idx,"mobile"));
			map.put("email", mADB.getInfoByIdx(idx,"email"));
			map.put("status", ContactsOnline.getContactOnlineStatus(address));
			memberList.add(map);
		}
		
		gallery=(PhotoGallery)findViewById(R.id.group_users);
		gallery.setVisibility(View.VISIBLE);
		imageAdapter = new ImageAdapter(this);
		gallery.setAdapter(imageAdapter);
		
		gallery.setOnItemClickListener(onPhotoClick);
		
		gallery.setSelection(getIntent().getIntExtra("Selection", 0));
	}

	protected void onDestroy() {
		if(mADB!=null && mADB.isOpen()) 
			mADB.close();
		System.gc();
		super.onDestroy();
	};
	
	int mIdx;
	String mDisplayname;
	String mAddress;
	
	OnItemClickListener onPhotoClick=new OnItemClickListener(){
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			
			mDisplayname=(String)memberList.get(position).get("displayname");
			mAddress=(String)memberList.get(position).get("address");
			
			mIdx=Integer.parseInt((String)memberList.get(position).get("idx"));
			
			String userphotoPath = Global.SdcardPath_inbox + "photo_" + mIdx + "b.jpg";
			File f = new File(userphotoPath);
			if (!f.exists())
			{
				new Thread(new Runnable(){
					public void run()
					{
						String userphotoPath = Global.SdcardPath_inbox + "photo_" + mIdx + "b.jpg";
						String remotefile = "profiles/photo_" + mIdx + ".jpg";
						int success = 0;
						int count = 0;
						do {
							MyNet net = new MyNet(CorpMemberActivity.this);
							success = net.Download(remotefile, userphotoPath, AireJupiter.myLocalPhpServer);
							if (success==1||success==0)
								break;
							MyUtil.Sleep(500);
						} while (++count < 2);
						
						if (success!=1)
						{
							count=0;
							do {
								MyNet net = new MyNet(CorpMemberActivity.this);
								success = net.Download(remotefile, userphotoPath, null);
								if (success==1||success==0)
									break;
								MyUtil.Sleep(500);
							} while (++count < 2);
						}
						
						if (success==1)
						{
							File f = new File(userphotoPath);
							if (f.exists())
							{
								Intent i = new Intent(CorpMemberActivity.this,MessageDetailActivity.class);
								i.putExtra("imagePath", userphotoPath);
								i.putExtra("displayname", mDisplayname);
								i.putExtra("address", mAddress);
								startActivity(i);
							}
						}
					}
				}).start();
			}
			else
			{
				Intent i = new Intent(CorpMemberActivity.this,MessageDetailActivity.class);
				i.putExtra("imagePath", userphotoPath);
				i.putExtra("displayname", mDisplayname);
				i.putExtra("address", mAddress);
				startActivity(i);
			}
		}
	};
	
	//alec:
	public static class Holder{
		public static ImageView photo = null;
		public static TextView title = null;
		public static TextView name = null;
	}
	
	class ImageAdapter extends BaseAdapter { 
		private Context context = null;
	    public ImageAdapter(Context context) {
			super();
			this.context = context;
		}

		@Override
		public int getCount() {
			return memberList.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		OnClickListener onTelephoneClick=new OnClickListener() {
			@Override
			public void onClick(View v) {
				TextView tv=(TextView)v;
				Uri uri = Uri.parse("tel:"+tv.getText());  
				Intent it = new Intent(Intent.ACTION_DIAL, uri);  
				startActivity(it);
			}
		};
		
		OnClickListener onEmailClick=new OnClickListener() {
			@Override
			public void onClick(View v) {
				TextView tv=(TextView)v;
				Uri uri = Uri.parse("mailto:"+tv.getText());  
				Intent it = new Intent(Intent.ACTION_SENDTO, uri);  
				startActivity(it);
			}
		};
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView==null){
				convertView = View.inflate(context, R.layout.corp_gallery_items, null);
			}
			Holder.photo=(ImageView)convertView.findViewById(R.id.photo);
			Holder.name=(TextView)convertView.findViewById(R.id.name);
	        
			String displayname=(String)memberList.get(position).get("displayname");
			Holder.name.setText(displayname);
			Holder.photo.setImageDrawable((Drawable)memberList.get(position).get("photo"));
			
			Holder.photo.setTag(""+memberList.get(position).get("idx"));
			
			if ((Integer)memberList.get(position).get("status")>0)
				Holder.photo.setBackgroundResource(R.drawable.empty_online);
			else
				Holder.photo.setBackgroundResource(R.drawable.empty);
			
			Holder.title=(TextView)convertView.findViewById(R.id.title);
			Holder.title.setText((String)memberList.get(position).get("title"));

			
			
			((TextView)convertView.findViewById(R.id.phone)).setText((String)memberList.get(position).get("phone"));
			
			String info=(String)memberList.get(position).get("mobile");
			if (info!=null)
			{
				((TextView)convertView.findViewById(R.id.mobile)).setText(info);
				((TextView)convertView.findViewById(R.id.mobile)).setVisibility(View.VISIBLE);
				((TextView)convertView.findViewById(R.id.mobile)).setOnClickListener(onTelephoneClick);
			}else{
				((TextView)convertView.findViewById(R.id.mobile)).setVisibility(View.INVISIBLE);
				((TextView)convertView.findViewById(R.id.mobile)).setOnClickListener(null);
			}
			
			info=(String)memberList.get(position).get("email");
			if (info!=null)
			{
				((TextView)convertView.findViewById(R.id.email)).setText(info);
				((TextView)convertView.findViewById(R.id.email)).setVisibility(View.VISIBLE);
				((TextView)convertView.findViewById(R.id.email)).setOnClickListener(onEmailClick);
			}else{
				((TextView)convertView.findViewById(R.id.email)).setVisibility(View.INVISIBLE);
				((TextView)convertView.findViewById(R.id.email)).setOnClickListener(null);
			}
		
			return convertView;
		}
	}
}
