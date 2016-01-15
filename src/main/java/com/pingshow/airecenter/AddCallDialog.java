package com.pingshow.airecenter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.bean.ChatroomMember;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.voip.DialerActivity;

public class AddCallDialog extends Activity {

	final int limit=3;
	private UserItemAdapter gridAdapter;
	private List<Map<String, String>> amperList;
	private List<String> contactsList;
	private AsyncImageLoader asyncImageLoader;
	private int numColumns=3;
	private GridView resultGridView;
	private ListView Contacts_LV;
	private AmpUserDB mADB;
	private ContactsQuery cq;
	private int mCount=0;
	private int mCount2=0;
	private ArrayList<String> excludeList;
	private QueryContactHandler mContactQueryHandler;
	private Cursor mContactCursor = null;
	private ContactAdapter mContactCursorAdapter;
	private int selectionMode=0;
	private MyPreference mPref;
	private float mDensity=1.0f;
	
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Toast.makeText(AddCallDialog.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.add_call_dialog);
		
		mDensity = getResources().getDisplayMetrics().density;
			
		WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.5f;
		getWindow().setAttributes(lp);
		
		mADB = new AmpUserDB(this);
		mADB.open();
		
		((ImageView)findViewById(R.id.close)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
		
		mADB = new AmpUserDB(this);
		mADB.open();
			
		numColumns=3;
		
		cq = new ContactsQuery(this);
		
	    amperList = new ArrayList<Map<String, String>>();
	    contactsList = new ArrayList<String>();
	    
	    gridAdapter = new UserItemAdapter(this);
	    
	    resultGridView = (GridView) findViewById(R.id.pickup);
	    Contacts_LV = (ListView) findViewById(R.id.addressbook);
	    resultGridView.setNumColumns(numColumns);
	    
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.inflate_credit, null, false);
		Contacts_LV.addHeaderView(v);
		
		mPref=new MyPreference(this);
		float credit=mPref.readFloat("Credit",0);
		TextView tv=(TextView)v.findViewById(R.id.credit);
        if (tv!=null) tv.setText(String.format(getString(R.string.credit), credit));
	    
	    //excludeList=(ArrayList<String>)getIntent().getSerializableExtra("Exclude");
	    
	    resultGridView.setAdapter(gridAdapter);
	    resultGridView.setOnItemClickListener(onChooseUser);
	    
        ((ImageView)findViewById(R.id.done)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			int count=0;
    			if (selectionMode==0)
    			{
    				for (int i=0;i<amperList.size() && count<3;i++)
        			{
        				Map<String, String> map = amperList.get(i);
        				if (map.get("checked").equals("1"))
        				{
        					DialerActivity.addingList.add(map.get("idx"));
        					count++;
        				}
        			}
    			}else if (selectionMode==1)
    			{
    				float credit=mPref.readFloat("Credit",0);
    				if (credit<0.010)
    				{
    					Intent it = new Intent(AddCallDialog.this, CommonDialog.class);
    					it.putExtra("msgContent", getString(R.string.credit_not_enough));
    					it.putExtra("numItems", 1);
    					it.putExtra("ItemCaption0", getString(R.string.done));
    					it.putExtra("ItemResult0", RESULT_OK);
    					startActivity(it);
    					return;
    				}
    				
    				for (int i=0;i<contactsList.size() && count<3;i++)
        			{
    					String address = contactsList.get(i);
    					DialerActivity.addingList2.add(address);
    					count++;
        			}
    			}
				finish();
    		}}
        );
        
        ((ToggleButton)findViewById(R.id.users)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				((ToggleButton)findViewById(R.id.address)).setChecked(false);
				((ToggleButton)findViewById(R.id.users)).setEnabled(false);
				((ToggleButton)findViewById(R.id.address)).setEnabled(true);
				Contacts_LV.setVisibility(View.GONE);
				resultGridView.setVisibility(View.VISIBLE);
				selectionMode=0;
			}
		});
		
		((ToggleButton)findViewById(R.id.address)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				((ToggleButton)findViewById(R.id.users)).setChecked(false);
				((ToggleButton)findViewById(R.id.address)).setEnabled(false);
				((ToggleButton)findViewById(R.id.users)).setEnabled(true);
				Contacts_LV.setVisibility(View.VISIBLE);
				resultGridView.setVisibility(View.GONE);
				selectionMode=1;
			}
		});
        
        mHandler.post(mFetchFriends);
        mHandler.postDelayed(new Runnable(){
        	public void run()
        	{
        		onContactQuery();
        	}
        }, 1000);
	}
	
	public void onContactQuery() 
	{
		if (mContactCursor!=null && !mContactCursor.isClosed())
			mContactCursor.close();
		
		if (mContactQueryHandler == null)
			mContactQueryHandler = new QueryContactHandler(getContentResolver());
		
		mContactQueryHandler.startQuery(0, null,
				ContactsContract.Contacts.CONTENT_URI,
				new String[]{"_id","display_name"},
				ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", null,
				ContactsContract.Contacts.LAST_TIME_CONTACTED+" desc");
	}
    
    private class QueryContactHandler extends AsyncQueryHandler {
		public QueryContactHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor c) {
			try {
				mContactCursor=c;
				
				if (mContactCursorAdapter == null) {
					mContactCursorAdapter = new ContactAdapter(AddCallDialog.this, mContactCursor, cq);
					Contacts_LV.setAdapter(mContactCursorAdapter);
					Contacts_LV.setOnItemClickListener(OnContactClickListener);
		        } else {
		        	mContactCursorAdapter.changeCursor(mContactCursor);
		        }
			} catch (Exception e) {
				Log.e("onQueryComplete");
				e.printStackTrace();
			}
		}
			
		@Override
		protected void onInsertComplete(int token, Object cookie, Uri uri) {
			onContactQuery();
		}

		@Override
		protected void onUpdateComplete(int token, Object cookie, int result) {
			onContactQuery();
		}

		@Override
		protected void onDeleteComplete(int token, Object cookie, int result) {
			onContactQuery();
		}
	}
    
    private OnItemClickListener OnContactClickListener = new OnItemClickListener()
	{
		@Override  
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
		{
			try{
				ImageView checkbox=(ImageView)view.findViewById(R.id.checked);
				String address=((TextView)view.findViewById(R.id.address)).getText().toString();
				if (checkbox.getVisibility()==View.VISIBLE)
				{
					checkbox.setVisibility(View.GONE);
					contactsList.remove(address);
					mCount2--;
				}else{
					if (mCount2>=limit) return;
					checkbox.setVisibility(View.VISIBLE);
					contactsList.add(address);
					mCount2++;
				}
			}catch(Exception e){
			}
		}
	};

	OnItemClickListener onChooseUser=new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			Map<String, String> map = amperList.get(position);
			if (map.get("checked").equals("0"))
			{
				if (mCount>=limit) return;
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
					if (DialerActivity.memberList!=null)//alec Exclude some users
					{
						boolean found=false;
						try{
							for (ChatroomMember a: DialerActivity.memberList)
							{
								if (a.getIdx()==idx){
									found=true;
									break;
								}
							}
						}catch(Exception e){
						}
						if (found) continue;
					}
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
		contactsList.clear();
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
				convertView = View.inflate(icontext, R.layout.user_add_cell, null);
				
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
}
