package com.pingshow.airecenter;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.zxing.WriterException;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.contacts.RelatedUserInfo;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.RelatedUserDB;
import com.pingshow.airecenter.Log;
import com.pingshow.network.MyNet;
import com.pingshow.qrcode.CaptureActivity;
import com.pingshow.qrcode.EncodingHandler;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.MCrypt;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.util.MyUtil;

public class SearchPage extends Page {

	private MyPreference mPref;
	private UserItemAdapter gridAdapter;
	private List<Map<String, String>> amperList;
	private AsyncImageLoader asyncImageLoader;
	private GridView resultGridView;
	private AmpUserDB mADB;
	private RelatedUserDB mRDB;
	private ContactsQuery cq;
	private boolean searching;
	private final static int MAX_SEARCHED_ITEMS=450;
	private float mDensity = 1.f;
	
	private View layout;

	private volatile int search_mode = 0;
	
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Toast.makeText(MainActivity._this, msg.obj.toString(), Toast.LENGTH_LONG).show();
		}
	};
	
	public SearchPage(View v) {
		Log.e("*** !!! SEARCHPAGE *** START START !!! ***");
		layout=v;
	    
	    mADB = new AmpUserDB(MainActivity._this);
		mADB.open();
		
		mRDB = new RelatedUserDB(MainActivity._this);
		mRDB.open();
		
		cq = new ContactsQuery(MainActivity._this);
		
	    mPref=new MyPreference(MainActivity._this);
	    amperList = new ArrayList<Map<String, String>>();
	    
	    mDensity = MainActivity._this.getResources().getDisplayMetrics().density;
	    
	    gridAdapter = new UserItemAdapter(MainActivity._this);
	    
	    resultGridView = (GridView) layout.findViewById(R.id.friendsGridView);
	    
	    resultGridView.setAdapter(gridAdapter);
	    resultGridView.setOnItemClickListener(onChooseUser);
	    
	    ((EditText)layout.findViewById(R.id.keyword)).addTextChangedListener (new TextWatcher() {
	        @Override
	        public void afterTextChanged (Editable s) {
	        	if (s.toString().length()==0)
	        	{
	        		((ImageView)layout.findViewById(R.id.clear)).setVisibility(View.GONE);
	        		((EditText)layout.findViewById(R.id.keyword)).setPadding((int)(16.f*mDensity), (int)(6.f*mDensity),(int)(16.f*mDensity), (int)(6.f*mDensity));
	        	}
	        	else{
	        		((ImageView)layout.findViewById(R.id.clear)).setVisibility(View.VISIBLE);
	        		((EditText)layout.findViewById(R.id.keyword)).setPadding((int)(56.f*mDensity), (int)(6.f*mDensity),(int)(16.f*mDensity), (int)(6.f*mDensity));
	        	}
	        }

	        @Override
	        public void onTextChanged (CharSequence s, int start, int before, int count) {
	        }

	        @Override
	        public void beforeTextChanged (CharSequence s, int start, int count, int after) {
	        }
	    });
//	    ((EditText) layout.findViewById(R.id.keyword)).requestFocus();  //tml*** prefocus
	    
	    ((ImageView)layout.findViewById(R.id.clear)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((EditText)layout.findViewById(R.id.keyword)).setText("");
			}
		});
	    
	    ((Button)layout.findViewById(R.id.search)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText kw = (EditText)layout.findViewById(R.id.keyword);
    			mKeyword=kw.getText().toString().trim();
    			hideKeyboard();
    			
    			int max=5;
    			if(mKeyword.getBytes().length != mKeyword.length())
    				max=1;//chinese
    			
    			if (mKeyword.length()>=max)
    			{
    				mHandler.post(popupProgressDialog);
	    			if (!MyUtil.checkNetwork(MainActivity._this)) return;
	    			search_mode = 1;
	    			new Thread(searchFriends).start();
    			} else {
    				Toast.makeText(MainActivity._this, MainActivity._this.getString(R.string.nickname_invalid), Toast.LENGTH_SHORT).show();
    			}
			}
		});
	    
	    //tml*** new friends
	    ((Button) layout.findViewById(R.id.nearby_search)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
    			hideKeyboard();
				mHandler.post(popupProgressDialog);
    			if (!MyUtil.checkNetwork(MainActivity._this)) return;
    			search_mode = 2;
    			new Thread(preSearchFriends).start();
			}
		});

	    //tml|li*** qr addf
//	    ((Button) layout.findViewById(R.id.qr_friends)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
//					Intent openCameraIntent = new Intent(MainActivity._this, CaptureActivity.class);
//					MainActivity._this.startActivityForResult(openCameraIntent, 105);
//				} else {
//					Toast.makeText(MainActivity._this, MainActivity._this.getString(R.string.old_sdk), Toast.LENGTH_SHORT).show();
//				}
//			}
//		});
	    
		((ImageButton) layout.findViewById(R.id.my_qr)).setOnClickListener(new OnClickListener() {
			@Override
    		public void onClick(View v) {
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
					try {
						String address = mPref.read("myPhoneNumber", "----");
						String nickname = mPref.read("myNickname", "");
						int idx = Integer.parseInt(mPref.read("myID", "0"), 16);
						String qrContent = "AddAireFriend," + address + "," + idx + "," + nickname;
						//li*** encrypt
						byte[] bytes = MCrypt.encrypt(qrContent.getBytes());
						qrContent = Base64.encodeToString(bytes, Base64.DEFAULT);
						
						Bitmap qrCodeBitmap = EncodingHandler.createQRCode(qrContent, 300);
						Dialog qrCode = new Dialog(MainActivity._this);
						View view = View.inflate(MainActivity._this.getApplicationContext(), R.layout.user_qrcode, null);
						ImageView iv = (ImageView) view.findViewById(R.id.iv_qrcode);
						iv.setImageBitmap(qrCodeBitmap);
						
						qrCode.setTitle(nickname);
						qrCode.setCanceledOnTouchOutside(true);
						qrCode.setContentView(view);
						
						qrCode.show();
					} catch (WriterException e) {
						Log.e("build qr !@#$ " + e.getMessage());
					}
				} else {
					Toast.makeText(MainActivity._this, MainActivity._this.getString(R.string.old_sdk), Toast.LENGTH_SHORT).show();
				}
    		}
		});
		//***tml
	    
	    ((EditText)layout.findViewById(R.id.keyword)).setImeActionLabel(MainActivity._this.getString(R.string.search), EditorInfo.IME_ACTION_SEARCH);
	    ((EditText)layout.findViewById(R.id.keyword)).setOnEditorActionListener(new OnEditorActionListener(){
			@Override
			public boolean onEditorAction(TextView v, int arg1, KeyEvent arg2) {
				EditText kw = (EditText)layout.findViewById(R.id.keyword);
    			mKeyword=kw.getText().toString().trim();
//    			hideKeyboard();  //tml*** alpha ui, CX/
    			
    			int max=3;
    			if(mKeyword.getBytes().length != mKeyword.length())
    				max=1;//chinese
    			
    			if (mKeyword.length()>=max)
    			{
    				mHandler.post(popupProgressDialog);
	    			if (!MyUtil.checkNetwork(MainActivity._this)) return false;
	    			new Thread(searchFriends).start();
	    			return true;
    			}
				return false;
			}
	    });
	    //tml*** beta ui2
	    ((Button) layout.findViewById(R.id.add_group)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MainActivity._this.switchInflater(2);
			}
		});
	    
	    //tml*** alpha ui
	    ((EditText) layout.findViewById(R.id.keyword)).setText(UserPage.passKeyword);
	    
	    IntentFilter intentToReceiveFilter = new IntentFilter();
		intentToReceiveFilter.addAction(Global.Action_SearchPage_Adding);
		MainActivity._this.registerReceiver(handleAddingUser, intentToReceiveFilter);
		
		//tml*** search add
//		if (((EditText) layout.findViewById(R.id.keyword)).getText().toString().trim().length() > 0) {
		if (UserPage.passKeyword != null && UserPage.passKeyword.length() > 0) {
			Log.d("tml begin SEARCH from X");
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					((Button) layout.findViewById(R.id.search)).performClick();
				}
			}, 300);
			UserPage.passKeyword = "";
		}
		//tml*** new friend
//		if (UserPage.doInstantFriend) {
//			Log.e("doInstantFriend from MAIN=" + UserPage.doInstantFriend);
//			UserPage.doInstantFriend = false;
//			mHandler.postDelayed(new Runnable() {
//				@Override
//				public void run() {
//					((Button) layout.findViewById(R.id.nearby_search)).performClick();
//				}
//			}, 300);
//		}
	}
	
	BroadcastReceiver handleAddingUser = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context,final Intent intent) {
			if (intent.getAction().equals(Global.Action_SearchPage_Adding)) 
			{
				if (!MyUtil.checkNetwork(MainActivity._this)) return;
				
				mADB.insertUser(mAddress, mIdx, mDisplayname);
				mRDB.deleteContactByAddress(mAddress);
				
				ContactsOnline.setContactOnlineStatus(mAddress, 0);
				UserPage.forceRefresh=true;
				
				Intent it = new Intent(Global.Action_InternalCMD);
				it.putExtra("Command", Global.CMD_UPLOAD_FRIENDS);
				it.putExtra("type", 1);//Single Friend
				it.putExtra("serverType", 1);//add
				it.putExtra("idxlist", mIdx+"");
				MainActivity._this.sendBroadcast(it);
				
				try {
					int myIdx=Integer.parseInt(mPref.read("myID","0"),16);
					SendAgent agent = new SendAgent(MainActivity._this, myIdx, 0, false);
					agent.onSend(mAddress, Global.Hi_AddFriend1, 0, null, null, true);
				} catch (Exception e) {
					Log.e("sp " + e.getMessage());
				}
				
				Intent it3 = new Intent(Global.Action_InternalCMD);
				it3.putExtra("Command", Global.CMD_SEARCH_POSSIBLE_FRIENDS);
				MainActivity._this.sendBroadcast(it3);
			}
		}
	};
	
	private int mIdx;
	private String mDisplayname;
	private String mAddress;
	
	OnItemClickListener onChooseUser=new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			Map<String, String> map = amperList.get(position);
			
			String address=map.get("address");
			boolean isFafauser = mADB.isFafauser(address);
			Log.d("hello! " + isFafauser + " " + address + " " + map.get("displayName") + " " + Integer.parseInt(map.get("idx")));
			if (isFafauser) {
				Intent it=new Intent(MainActivity._this, FunctionActivity.class);
				long contactId = cq.getContactIdByNumber(address);
				it.putExtra("Contact_id", contactId);
				it.putExtra("Address", address);
				it.putExtra("Nickname", map.get("displayName"));
				it.putExtra("Idx", Integer.parseInt(map.get("idx")));
				MainActivity._this.startActivity(it);
			} else {
				mDisplayname=map.get("displayName");
				mAddress=address;
				mIdx=Integer.parseInt(map.get("idx"));
				
				Intent it=new Intent(MainActivity._this, CommonDialog.class);
				it.putExtra("Address", mAddress);
				it.putExtra("Nickname", mDisplayname);
				it.putExtra("Idx", mIdx);
				it.putExtra("msgContent", String.format(MainActivity._this.getString(R.string.add_as_friend_desc), mDisplayname));
				it.putExtra("numItems", 2);
				it.putExtra("ItemCaption0", MainActivity._this.getString(R.string.cancel));
				it.putExtra("ItemResult0", Activity.RESULT_CANCELED);
				it.putExtra("ItemCaption1", MainActivity._this.getString(R.string.add_as_friend));
				it.putExtra("ItemResult1", CommonDialog.ADD_NEW_USER);
				MainActivity._this.startActivity(it);
			}
		}
	};
	
	private ProgressDialog progressDialog;
	
	Runnable popupProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				if (progressDialog==null)
					progressDialog = ProgressDialog.show(MainActivity._this, "", MainActivity._this.getString(R.string.in_progress), true, true);
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
			}catch(Exception e){
				progressDialog = null;
			}
		}
	};

	//tml*** new friends
	Runnable preSearchFriends = new Runnable() {
		public void run() {
			int count = 0;
			String Return = null;
			boolean preSearchRdy = false;
			searching = true;
			
			try {
				MyNet net = new MyNet(MainActivity._this);
//				int myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
				String myIdx = mPref.read("myID", "0");  //hex
				//tml*** china ip
				String phpip = AireJupiter.myPhpServer_default;
				if (AireJupiter.getInstance() != null) {
					phpip = AireJupiter.getInstance().getIsoPhp(0, true, "74.3.165.66");
				}
				
				do {
					Return = net.doPost("getfriend.php", "idx=" + myIdx, phpip);
					Log.i("preSearch " + myIdx + " return=" + Return);
					
					if (Return.toLowerCase().contains("ok")) {
						preSearchRdy = true;
						break;
					}
					MyUtil.Sleep(1500);
				} while (count++ < 3 && searching);
			} catch (Exception e) {
				Log.e("getfriend.php ERR " + e.getMessage());
				preSearchRdy = false;
			}
			Log.e("Return=" + Return);
			
			if (preSearchRdy) {
				if (search_mode == 2) {
					MyUtil.Sleep(2000);
				} else if (search_mode == 3) {
					MyUtil.Sleep(5000);
				} else {
					MyUtil.Sleep(3000);
				}
				new Thread(searchFriends).start();
			} else {
				mHandler.post(dismissProgress);
			}
		}
	};
	//***tml
	
	String mKeyword="";
	Runnable searchFriends=new Runnable(){
		public void run()
		{
			int count=0;
			List<RelatedUserInfo> foundUserXML=null;
			amperList.clear();
			
			searching=true;
			//tml*** china ip
			String phpip = AireJupiter.myPhpServer_default;
			if (AireJupiter.getInstance() != null) {
				phpip = AireJupiter.getInstance().getIsoPhp(0, true, "74.3.165.66");
			}
			
			try {
				do {
					MyNet net = new MyNet(MainActivity._this);
					String myIdx = mPref.read("myID", "0");  //hex
//					foundUserXML = net.doPostHttpsWithXML("search_aire.php", "k=" + URLEncoder.encode(mKeyword), null);
					if (search_mode == 1) {
						foundUserXML = net.doPostHttpWithXML("search_aire.php", "k=" + URLEncoder.encode(mKeyword), null);
					} else if (search_mode == 2) {  //tml*** new friends
						foundUserXML = net.doPostHttpWithXML("getfriend2.php", "idx=" + myIdx + "&local=1", phpip);
					} else if (search_mode == 3) {  //tml*** new friends
						foundUserXML = net.doPostHttpWithXML("getfriend2.php", "idx=" + myIdx + "&local=0", phpip);
					} else if (search_mode == 4) {  //tml*** new friends
						Random rng = new Random();
						int randomDiv = rng.nextInt(5) + 1;
						int relationship = UserPage.numTrueFriends / randomDiv + 2;
						foundUserXML = net.doPostHttpWithXML("possiblefriends_aire.php", "idx=" + myIdx
								+ "&relationship=" + relationship, null);
					} else {
						return;
					}
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
					if (!new File(userphotoPath).exists())
					{
						userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + "b.jpg";
						if (!new File(userphotoPath).exists())
							userphotoPath=null;
					}
					if (userphotoPath == null) {
						Log.w("null pic! " + address + " path=" + userphotoPath);
					}
					
					map.put("displayName", disName);
					map.put("address", address);
					map.put("idx", idx+"");
					map.put("imagePath", userphotoPath);
					
					idxBuffer.append(Integer.toHexString(idx)+"+");
					
					amperList.add(map);
				}
				
				mHandler.post(dismissProgress);
				
				Message msg = new Message();
				msg.obj = String.format(MainActivity._this.getString(R.string.loading)
						+ " " + MainActivity._this.getString(R.string.search_result), foundUserXML.size());
				mHandler.sendMessage(msg);
				
				if (AireJupiter.getInstance()!=null && MyUtil.checkSDCard(MainActivity._this) && amperList.size()>0 && searching)
				{
					try {
						MyNet net = new MyNet(MainActivity._this);
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
			} else {
				Log.e("searchFriends foundUserXML ERR");
			}
			mHandler.post(new Runnable(){
				public void run(){
					gridAdapter.notifyDataSetChanged();
				}
			});
			
			mHandler.post(dismissProgress);
		}
	};
	
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
			String searchAddr = map.get("address");
			String searchIdx = map.get("idx");
			//tml*** search pic
			if (imagePath == null && searchIdx != null && searchAddr != null) {
				imagePath = Global.SdcardPath_inbox + "photo_" + searchIdx + ".jpg";
				if (!new File(imagePath).exists()) {
					imagePath = Global.SdcardPath_inbox + "photo_" + searchIdx + "b.jpg";
					if (!new File(imagePath).exists()) {
						imagePath = null;
						Log.w("Search " + searchAddr + " " + searchIdx + " has no pic!");
					}
				}
			}
			
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
//			Log.e("tml test pic " + imagePath);
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
	
	void hideKeyboard()
	{
		if (MainActivity._this != null) {
			InputMethodManager imm = (InputMethodManager)MainActivity._this.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(((EditText)layout.findViewById(R.id.keyword)).getWindowToken(), 0);
		}
	}
	
	@Override
	public void destroy() {
		hideKeyboard();
		try {
			if (MainActivity._this != null)
				MainActivity._this.unregisterReceiver(handleAddingUser);
		} catch (Exception e) {}
		if (mADB != null && mADB.isOpen())
			mADB.close();
		if (mRDB != null && mRDB.isOpen())
			mRDB.close();
		searching=false;
		amperList.clear();
		System.gc();
		System.gc();
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 105) {  //tml|li*** qr addf
			if (resultCode == Activity.RESULT_OK) {
				Bundle bundle = data.getExtras();
				String scanResult = bundle.getString("result");
				String[] items = null;
				boolean success = true;
				String falseScan = "";
				
				if (scanResult != null) {
					//li*** encrypt
					try {
						byte[] bytes = MCrypt.encrypt(Base64.decode(scanResult.getBytes(), Base64.DEFAULT));
						scanResult = new String(bytes);
					} catch (Exception e) {
						Log.e("qr decrypt !@#$ " + e.getMessage());
					}
					
					if (scanResult.contains(",")) {
						items = scanResult.split(",");
						
						if (items.length == 4 && items[0].equals("AddAireFriend")) {
							for (int i = 0; i < 4; i++) {
								if (items[i] == null || items[i].length() == 0) {
									success = false;
									break;
								}
								Log.d("scan friend item" + i + " " + items[i]);
							}
							
							try {
								if (!success) throw new IOException();
								mAddress = items[1];
								mIdx = Integer.parseInt(items[2]);
								mDisplayname = items[3];
								if (mADB.isFafauser(mAddress)) {
									Intent it=new Intent(MainActivity._this, FunctionActivity.class);
									long contactId = cq.getContactIdByNumber(mAddress);
									it.putExtra("Contact_id", contactId);
									it.putExtra("Address", mAddress);
									it.putExtra("Nickname", mDisplayname);
									it.putExtra("Idx", mIdx);
									MainActivity._this.startActivity(it);
								} else {
									Intent it=new Intent(MainActivity._this, CommonDialog.class);
									it.putExtra("Address", mAddress);
									it.putExtra("Nickname", mDisplayname);
									it.putExtra("Idx", mIdx);
									it.putExtra("msgContent", String.format(MainActivity._this.getString(R.string.add_as_friend_desc), mDisplayname));
									it.putExtra("numItems", 2);
									it.putExtra("ItemCaption0", MainActivity._this.getString(R.string.cancel));
									it.putExtra("ItemResult0", Activity.RESULT_CANCELED);
									it.putExtra("ItemCaption1", MainActivity._this.getString(R.string.add_as_friend));
									it.putExtra("ItemResult1", CommonDialog.ADD_NEW_USER);
									MainActivity._this.startActivity(it);
								}
							} catch (Exception e) {
								success = false;
							}
						} else {
							success = false;
							falseScan = "\n" + scanResult;
						}
					} else {
						success = false;
						falseScan = "\n" + scanResult;
					}
				}
				
				if (!success) {
					Toast.makeText(MainActivity._this, MainActivity._this.getString(R.string.qr_invalid) + falseScan, Toast.LENGTH_SHORT).show();
				}
			}
		}
	}
}
