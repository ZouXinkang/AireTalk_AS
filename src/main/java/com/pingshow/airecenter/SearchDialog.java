package com.pingshow.airecenter;

import java.io.File;
import java.net.URLEncoder;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.contacts.RelatedUserInfo;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.RelatedUserDB;
import com.pingshow.network.MyNet;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.util.MyUtil;

public class SearchDialog extends Activity {
	
	private MyPreference mPref;
	private UserItemAdapter gridAdapter;
	private List<Map<String, String>> amperList;
	private AsyncImageLoader asyncImageLoader;
	private int numColumns=3;
	private GridView resultGridView;
	private AmpUserDB mADB;
	private RelatedUserDB mRDB;
	private ContactsQuery cq;
	private boolean searching;
	private final static int MAX_SEARCHED_ITEMS=450;
	
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Toast.makeText(SearchDialog.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
		}
	};
	
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.search_page);
	    this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
	    
	    mADB = new AmpUserDB(this);
		mADB.open();
		
		mRDB = new RelatedUserDB(this);
		mRDB.open();
		
		if (getResources().getConfiguration().orientation!=1)
			numColumns=4;
		
		cq = new ContactsQuery(this);
		
	    mPref=new MyPreference(this);
	    amperList = new ArrayList<Map<String, String>>();
	    
	    gridAdapter = new UserItemAdapter(this);
	    
	    resultGridView = (GridView) findViewById(R.id.friendsGridView);
	    resultGridView.setNumColumns(numColumns);
	    
	    resultGridView.setAdapter(gridAdapter);
	    resultGridView.setOnItemClickListener(onChooseUser);
	    resultGridView.setBackgroundResource(R.drawable.tiled_bg);
	    
	    ((ImageView)findViewById(R.id.search)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			EditText kw = (EditText)findViewById(R.id.keyword);
    			mKeyword=kw.getText().toString().trim();
    			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    			
    			int max=3;
    			if(mKeyword.getBytes().length != mKeyword.length())
    				max=1;//chinese
    			
    			if (mKeyword.length()>=max)
    			{
    				v.setEnabled(false);
    				mHandler.post(popupProgressDialog);
	    			if (!MyUtil.checkNetwork(getApplicationContext())) return;
	    			new Thread(searchFriends).start();
    			}
    		}}
        );
        
        ((ImageView)findViewById(R.id.clear)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			((EditText)findViewById(R.id.keyword)).setText("");
    		}}
        );
        
        ((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	    
	    ((Button)findViewById(R.id.facebook_search)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it=new Intent(SearchDialog.this, FacebookSearch.class);
				it.putExtra("Mode", 0);
				startActivityForResult(it,103);
			}
		});
	    
	    ((Button)findViewById(R.id.phonebook_search)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it=new Intent(SearchDialog.this, FacebookSearch.class);
				it.putExtra("Mode", 1);
				startActivityForResult(it,103);
			}
		});
	}
	
	OnItemClickListener onChooseUser=new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			Map<String, String> map = amperList.get(position);
			
			String address=map.get("address");
			
			if (mADB.isFafauser(address))
			{
				Intent it=new Intent(SearchDialog.this, FunctionActivity.class);
				long contactId = cq.getContactIdByNumber(address);
				it.putExtra("Contact_id", contactId);
				it.putExtra("Address", address);
				it.putExtra("Nickname", map.get("displayName"));
				it.putExtra("Idx", Integer.parseInt(map.get("idx")));
				startActivity(it);
			}
			else{
				Intent it=new Intent(SearchDialog.this, AddAsFriendActivity.class);
				it.putExtra("Address", address);
				it.putExtra("Nickname", map.get("displayName"));
				it.putExtra("Idx", Integer.parseInt(map.get("idx")));
				startActivityForResult(it, 10);
			}
		}
	};
	
	@Override  
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		super.onActivityResult(requestCode, resultCode, data);  
		if (requestCode==10)
		{
			if (resultCode==RESULT_OK) {
				
				if (!MyUtil.checkNetwork(getApplicationContext())) return;
				
				String address=data.getExtras().getString("Address");
				String nickname=data.getExtras().getString("Nickname");
				int idx=data.getExtras().getInt("Idx");
				mADB.insertUser(address, idx, nickname);
				mRDB.deleteContactByAddress(address);
				
				ContactsOnline.setContactOnlineStatus(address, 0);
				UserPage.forceRefresh=true;
				
				Intent it = new Intent(Global.Action_InternalCMD);
				it.putExtra("Command", Global.CMD_UPLOAD_FRIENDS);
				it.putExtra("type", 1);//Single Friend
				it.putExtra("serverType", 1);//add
				it.putExtra("idxlist", idx+"");
				sendBroadcast(it);
				
				try {
					int myIdx=Integer.parseInt(mPref.read("myID","0"),16);
					SendAgent agent = new SendAgent(this, myIdx, 0, false);
					agent.onSend(address, Global.Hi_AddFriend1, 0, null, null, true);
				} catch (Exception e) {}
				
				Intent it2 = new Intent(Global.Action_Refresh_Gallery);
				sendBroadcast(it2);
				
				Intent it3 = new Intent(Global.Action_InternalCMD);
				it3.putExtra("Command", Global.CMD_SEARCH_POSSIBLE_FRIENDS);
				sendBroadcast(it3);
			}
		}else if (requestCode==103)
		{
			if (resultCode==RESULT_OK) {
				amperList.clear();
				String fb_result=data.getStringExtra("search_result");
				if (fb_result.length()==0) return;
				int found=0;
				try{
					String [] items=fb_result.split(",");
					if (items.length>0)
					{
						for (int i=0;i<items.length;i++)
						{
							HashMap<String, String> map = new HashMap<String, String>();
							int idx=mADB.getIdxByAddress(items[i]);
							String disName="";
							
							long contactid=cq.getContactIdByNumber(items[i]);
							if (contactid>0)
								disName=cq.getNameByContactId(contactid);
							else
								disName=mADB.getNicknameByAddress(items[i]);
							
							String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
							
							map.put("displayName", disName);
							map.put("address", items[i]);
							map.put("idx", idx+"");
							map.put("imagePath", userphotoPath);
							
							amperList.add(map);
							found++;
						}
					}
				}catch(Exception e){}
				
				Message msg = new Message();
				msg.obj = String.format(getString(R.string.search_result), found);
				mHandler.sendMessage(msg);
				
				mHandler.post(new Runnable(){
					public void run(){
						gridAdapter.notifyDataSetChanged();
						((FrameLayout)findViewById(R.id.search_ext)).setVisibility(View.GONE);
					}
				});
			}
		}
	}
	
	private ProgressDialog progressDialog;
	
	Runnable popupProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				if (progressDialog==null)
					progressDialog = ProgressDialog.show(SearchDialog.this, "", getString(R.string.in_progress), true, true);
			}catch(Exception e){}
		}
	};
	
	Runnable dismissProgress=new Runnable() {
		@Override
		public void run() {
			try{
				if (progressDialog != null && progressDialog.isShowing())
					progressDialog.dismiss();
				if (progressDialog != null) progressDialog.cancel();
				progressDialog = null;
				((ImageView)findViewById(R.id.search)).setEnabled(true);
			}catch(Exception e){
				progressDialog = null;
			}
		}
	};

	
	String mKeyword="";
	Runnable searchFriends=new Runnable(){
		public void run()
		{
			int count=0;
			List<RelatedUserInfo> foundUserXML=null;
			amperList.clear();
			
			searching=true;
			
			try {
				do {
					MyNet net = new MyNet(SearchDialog.this);
					foundUserXML = net.doPostHttpsWithXML("search_aire.php", "k=" + URLEncoder.encode(mKeyword), null);
					if (foundUserXML!=null) break;
					MyUtil.Sleep(1500);
				} while (count++ < 3 && searching);
			} catch (Exception e) {}
			
			if (foundUserXML!=null && foundUserXML.size()>0)
			{
				StringBuffer idxBuffer = new StringBuffer("");
				
				int myID=Integer.parseInt(mPref.read("myID","0"),16);
				String disName;
				int idx;
				for (int i=0;i<foundUserXML.size() && i<MAX_SEARCHED_ITEMS;i++)
				{
					RelatedUserInfo r=foundUserXML.get(i);
					idx=r.getIdx();
					String address=r.getAddress();
					if (mADB.isUserBlocked(address)==1 || mRDB.isUserBlocked(address)==1) continue;
					if (idx==myID) continue;
					disName=r.getNickName();

					HashMap<String, String> map = new HashMap<String, String>();
					
					String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
					
					map.put("displayName", disName);
					map.put("address", address);
					map.put("idx", idx+"");
					map.put("imagePath", userphotoPath);
					
					idxBuffer.append(Integer.toHexString(idx)+"+");
					
					amperList.add(map);
				}
				
				mHandler.post(new Runnable(){
					public void run(){
						gridAdapter.notifyDataSetChanged();
						((FrameLayout)findViewById(R.id.search_ext)).setVisibility(View.GONE);
					}
				});
				
				mHandler.post(dismissProgress);
				
				Message msg = new Message();
				msg.obj = String.format(getString(R.string.search_result), foundUserXML.size());
				mHandler.sendMessage(msg);
				
				if (AireJupiter.getInstance()!=null && MyUtil.checkSDCard(getApplicationContext()) && amperList.size()>0 && searching)
				{
					try {
						MyNet net = new MyNet(SearchDialog.this);
						String Return = net.doPostHttps("queryphotoAll.php","idx=" + idxBuffer.substring(0, idxBuffer.toString().length()-1), null);
						if (Return.length() > 5 && searching){
							Return = Return.substring(5);
							if (Return.length() > 0) {
								String[] versions = Return.split("\\+");
								String[] idxs = idxBuffer.substring(0, idxBuffer.toString().length()-1).split("\\+");
								if(versions.length>0){
									boolean result = false;
									for(int i = 0;i<versions.length && searching;i++){
										int idx10 = Integer.valueOf(idxs[i], 16);
										int netVersion = 0;
										try {
											netVersion = Integer.valueOf(versions[i],16);
											if (netVersion != 0){
												String localfile = Global.SdcardPath_inbox + "photo_" + idx10 + ".jpg";
												if (!new File(localfile).exists())
												{
													result = AireJupiter.getInstance().downloadPhoto(idx10,localfile);
													if (result)
													{
														mHandler.post(new Runnable(){
															public void run(){
																gridAdapter.notifyDataSetChanged();
															}
														});
													}
												}
											}
										} catch (Exception e) {}
									}
								}
							}
						}
					}catch(Exception e){}
				}
			}
			else
				mHandler.post(new Runnable(){
					public void run(){
						gridAdapter.notifyDataSetChanged();
					}
				});
			mHandler.post(dismissProgress);
		}
	};
	
	@Override
	protected void onDestroy() {
		if (mADB != null && mADB.isOpen())
			mADB.close();
		if (mRDB != null && mRDB.isOpen())
			mRDB.close();
		searching=false;
		amperList.clear();
		System.gc();
		System.gc();
		mPref.writeLong("last_show_time",new Date().getTime());
		super.onDestroy();
	}
	
	class foundViewHolder {
		TextView friendName;
		ImageView photoimage;
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
				convertView = View.inflate(icontext, R.layout.user_searched_cell, null);
				
				holder.photoimage = (ImageView) convertView.findViewById(R.id.photo);
				holder.friendName = (TextView) convertView.findViewById(R.id.friendname);
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
			
			return convertView;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
//		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
//		MobclickAgent.onPause(this);
		super.onPause();
	}

}