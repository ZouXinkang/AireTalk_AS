package com.pingshow.qrcode;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.pingshow.amper.AddAsFriendActivity;
import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.FacebookSearch;
import com.pingshow.amper.FunctionActivity;
import com.pingshow.amper.Global;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.R;
import com.pingshow.amper.SearchDialog;
import com.pingshow.amper.SendAgent;
import com.pingshow.amper.UsersActivity;
import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.contacts.RelatedUserInfo;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.RelatedUserDB;
import com.pingshow.network.MyNet;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.MCrypt;
import com.pingshow.util.AsyncImageLoader.ImageCallback;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import com.pingshow.util.MyUtil;

public class FindFriendsActivity extends Activity {
	private ImageView mCancel;
	private GridView mFriendsGrid;
	private int friendExtra;

	public final static int POINT=1;
	public final static int CONTACTS=2;
	public final static int NEIGHBORHOOD=3;
	
	public final static int SEARCH=4;
	private volatile int search_mode = 0;
	private boolean searching;
	private MyPreference mPref;
	private List<Map<String, String>> amperList;
	private final static int MAX_SEARCHED_ITEMS=450;
	private AmpUserDB mADB;
	private RelatedUserDB mRDB;
	private UserItemAdapter gridAdapter;
	private AsyncImageLoader asyncImageLoader;
	private int numColumns=3;
	private ContactsQuery cq;
	private boolean isHide = false;
	private boolean hasFacebook = true;
	private LinearLayout mSearchVisibility;
	
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Toast.makeText(FindFriendsActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.findfriends_setpage);
		mCancel = (ImageView) findViewById(R.id.cancel_findfriends_setpage);
		mFriendsGrid = (GridView) findViewById(R.id.friendsGridViewSet);
		mSearchVisibility = (LinearLayout) findViewById(R.id.LL_Search_Friends);
		gridAdapter = new UserItemAdapter(this);
		amperList = new ArrayList<Map<String, String>>();
		mFriendsGrid.setNumColumns(numColumns);	    
		mFriendsGrid.setAdapter(gridAdapter);
		mFriendsGrid.setOnItemClickListener(onChooseUser);
		mFriendsGrid.setBackgroundResource(R.drawable.tiled_bg);
	    
		((ImageView)findViewById(R.id.cancel_findfriends_setpage)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		initData();
		int defaultValue=0;
		//xwf 判断调用哪个方法
		friendExtra = getIntent().getIntExtra("Friend", defaultValue);
		
		switch (friendExtra) {
		case POINT:
			pointSynchronize();
			break;
		case CONTACTS:
			findContactsFriends();
			break;
		case NEIGHBORHOOD:
			neighborhoodFriends();
			break;
		case SEARCH:
			searchFriends();
			break;

		default:
			Toast.makeText(FindFriendsActivity.this, "您的选择有误", 0).show();
			break;
		}
	}
	/**
	 * 查找好友
	 */
	private void searchFriends() {
		//TODO 
		mSearchVisibility.setVisibility(View.VISIBLE);
		((ImageView)findViewById(R.id.searchs)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				EditText kw = (EditText)findViewById(R.id.keywords);
    			mKeyword=kw.getText().toString().trim();
    			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    			
    			int max=5;
    			if(mKeyword.getBytes().length != mKeyword.length())
    				max=1;//chinese
    			
    			if (mKeyword.length()>=max)
    			{
    				v.setEnabled(false);
    				mHandler.post(popupProgressDialog);
	    			if (!MyUtil.checkNetwork(FindFriendsActivity.this)) return;
	    			search_mode = 1;
	    			new Thread(searchFriends).start();
    			} else {
    				Toast.makeText(FindFriendsActivity.this, getString(R.string.nickname_invalid), Toast.LENGTH_SHORT).show();
    			}
			}
		});
	}
	/**
	 * 可能认识的人
	 */
	private void neighborhoodFriends() {
		mHandler.post(popupProgressDialog);
		if (!MyUtil.checkNetwork(FindFriendsActivity.this)) return;
		search_mode = 4;
		new Thread(searchFriends).start();
		Toast.makeText(FindFriendsActivity.this, getString(R.string.possible_friends), Toast.LENGTH_SHORT).show();
	}
	/**
	 * 查找通讯录的好友
	 */
	private void findContactsFriends() {
		Intent it=new Intent(FindFriendsActivity.this, FacebookSearch.class);
		it.putExtra("Mode", 1);
		startActivityForResult(it,103);
	}
	/**
	 * 同步一起点击
	 */
	private void pointSynchronize() {
		mHandler.post(popupProgressDialog);
		if (!MyUtil.checkNetwork(FindFriendsActivity.this)) return;
		search_mode = 2;
		new Thread(preSearchFriends).start();
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 105) {  //tml|li*** qr addf
			if (resultCode == RESULT_OK) {
				Bundle bundle = data.getExtras();
				String scanResult = bundle.getString("result");
				String[] items = null;
				boolean success = true;
				String falseScan = "";
				
				if (scanResult != null) {
					//li*** encrypt
					Log.d("qr preXN= " + scanResult);
					try {
						byte[] bytes = MCrypt.encrypt(Base64.decode(scanResult.getBytes(), Base64.DEFAULT));
						scanResult = new String(bytes);
					} catch (Exception e) {
						Log.e("qr decrypt !@#$ " + e.getMessage());
					}
					Log.d("qr postXN= " + scanResult);
					
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
								String address = items[1];
								int idx = Integer.parseInt(items[2]);
								String nickname = items[3];
								if (mADB.isFafauser(address)) {
									Intent it = new Intent(FindFriendsActivity.this, FunctionActivity.class);
									long contactId = cq.getContactIdByNumber(address);
									it.putExtra("Contact_id", contactId);
									it.putExtra("Address", address);
									it.putExtra("Nickname", nickname);
									it.putExtra("Idx", idx);
									startActivity(it);
								} else {
									Intent it = new Intent(FindFriendsActivity.this, AddAsFriendActivity.class);
									it.putExtra("Address", address);
									it.putExtra("Nickname", nickname);
									it.putExtra("Idx", idx);
									startActivityForResult(it, 10);
								}
							} catch (Exception e) {
								e.printStackTrace();
								success = false;
							}
						} else {
							success = false;
//							falseScan = "\n" + scanResult;
						}
					} else {
						success = false;
//						falseScan = "\n" + scanResult;
					}
				}
				
				if (!success) {
					Toast.makeText(FindFriendsActivity.this, getString(R.string.qr_invalid) + falseScan, Toast.LENGTH_SHORT).show();
				}
			}
		}else if (requestCode==10)
		{
			if (resultCode==RESULT_OK) {
				
				if (!MyUtil.checkNetwork(this)) return;
				
				String address=data.getExtras().getString("Address");
				String nickname=data.getExtras().getString("Nickname");
				int idx=data.getExtras().getInt("Idx");
				mADB.insertUser(address, idx, nickname);
				mRDB.deleteContactByAddress(address);
				
				ContactsOnline.setContactOnlineStatus(address, 0);
				UsersActivity.forceRefresh=true;
				
				Intent it = new Intent(Global.Action_InternalCMD);
				it.putExtra("Command", Global.CMD_UPLOAD_FRIENDS);
				it.putExtra("type", 1);//Single Friend
				it.putExtra("serverType", 1);//add
				it.putExtra("idxlist", idx+"");
				sendBroadcast(it);
				
				try {
					Log.d("SENDING ADDF REQUEST.original");
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
		}
	}
	//xwf*** new friends
	Runnable preSearchFriends = new Runnable() {
			public void run() {
				int count = 0;
				String Return = null;
				boolean preSearchRdy = false;
				searching = true;
				
				try {
					MyNet net = new MyNet(FindFriendsActivity.this);
//					int myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
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
					Log.e("getfriend.php !@#$ " + e.getMessage());
					preSearchRdy = false;
				}
				
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
		
		Runnable dismissProgress = new Runnable() {
			@Override
			public void run() {
				try {
					if (progressDialog != null && progressDialog.isShowing())
						progressDialog.dismiss();
					if (progressDialog != null) progressDialog.cancel();
						progressDialog = null;
					((ImageView) findViewById(R.id.search)).setEnabled(true);
				} catch (Exception e) {
					progressDialog = null;
				}
			}
		};
	String mKeyword="";	
	Runnable searchFriends=new Runnable(){
			public void run()
			{
				Log.d("begin searchFriends!");
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
						MyNet net = new MyNet(FindFriendsActivity.this);
						String myIdx = mPref.read("myID", "0");  //hex
//						foundUserXML = net.doPostHttpWithXML("search_aire.php", "k=" + URLEncoder.encode(mKeyword), null);
						if (search_mode == 1) {
							foundUserXML = net.doPostHttpWithXML("search_aire.php", "k=" + URLEncoder.encode(mKeyword), null);
						} else if (search_mode == 2) {  //tml*** new friends
							foundUserXML = net.doPostHttpWithXML("getfriend2.php", "idx=" + myIdx + "&local=1", phpip);
						} else if (search_mode == 3) {  //tml*** new friends
							foundUserXML = net.doPostHttpWithXML("getfriend2.php", "idx=" + myIdx + "&local=0", phpip);
						} else if (search_mode == 4) {  //tml*** new friends
							Random rng = new Random();
							int randomDiv = rng.nextInt(5) + 1;
							int relationship = UsersActivity.numTrueFriends / randomDiv + 2;
							int myIdx2 = Integer.parseInt(myIdx, 16);
							foundUserXML = net.doPostHttpWithXML("possiblefriends_aire.php", "idx=" + myIdx2
									+ "&relationship=" + relationship, null);
						} else {
							return;
						}
						if (foundUserXML!=null) break;
						MyUtil.Sleep(1500);
					} while (count++ < 3 && searching);
				} catch (Exception e) {
					Log.e("searchFriends php !@#$ " + e.getMessage());
				}
				
				if (foundUserXML != null && foundUserXML.size() > 0)
				{
					StringBuffer idxBuffer = new StringBuffer("");
					
					int myID=Integer.parseInt(mPref.read("myID","0"),16);
					String disName;
					int idx;
					String searchinfo = "";
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
							Log.w("null pic! " + disName + " path=" + userphotoPath);
						}
						
						map.put("displayName", disName);
						map.put("address", address);
						map.put("idx", idx+"");
						map.put("imagePath", userphotoPath);
						
						idxBuffer.append(Integer.toHexString(idx)+"+");
						
						searchinfo = searchinfo + " " + disName + "," + address + "," + idx;
						Log.i("searchFriends ==" + searchinfo);
						amperList.add(map);
					}
					
					mHandler.post(new Runnable(){
						public void run(){
							gridAdapter.notifyDataSetChanged();
//							((FrameLayout)findViewById(R.id.search_ext)).setVisibility(View.GONE);  //li*** beta ui2 X
						}
					});
					
					mHandler.post(dismissProgress);
					
					Message msg = new Message();
					msg.obj = String.format(getString(R.string.search_result), foundUserXML.size());
					mHandler.sendMessage(msg);
					
					if (AireJupiter.getInstance()!=null && MyUtil.checkSDCard(FindFriendsActivity.this) && amperList.size()>0 && searching)
					{
						try {
							MyNet net = new MyNet(FindFriendsActivity.this);
							String Return = net.doPost("queryphotoAll.php","idx=" + idxBuffer.substring(0, idxBuffer.toString().length()-1), null);
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
		
		OnItemClickListener onChooseUser=new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Map<String, String> map = amperList.get(position);
				
				String address=map.get("address");
				Log.d("hello! " + address + " " + map.get("displayName") + " " + Integer.parseInt(map.get("idx")));
				if (mADB.isFafauser(address))
				{
					Intent it=new Intent(FindFriendsActivity.this, FunctionActivity.class);
					long contactId = cq.getContactIdByNumber(address);
					it.putExtra("Contact_id", contactId);
					it.putExtra("Address", address);
					it.putExtra("Nickname", map.get("displayName"));
					it.putExtra("Idx", Integer.parseInt(map.get("idx")));
					startActivity(it);
				}
				else{
					Intent it=new Intent(FindFriendsActivity.this, AddAsFriendActivity.class);
					it.putExtra("Address", address);
					it.putExtra("Nickname", map.get("displayName"));
					it.putExtra("Idx", Integer.parseInt(map.get("idx")));
					startActivityForResult(it, 10);
				}
			}
		};
	
	private ProgressDialog progressDialog;
	Runnable popupProgressDialog = new Runnable() {
		@Override
		public void run() {
			try {
				if (progressDialog == null)
					progressDialog = ProgressDialog.show(FindFriendsActivity.this, "", getString(R.string.in_progress), true, true);
			} catch (Exception e) {}
		}
	};
	/**
	 * 初始化的数据
	 */
	private void initData() {
		mPref=new MyPreference(this);
		amperList = new ArrayList<Map<String, String>>();
		mADB = new AmpUserDB(this);
		mADB.open();
		mRDB = new RelatedUserDB(this);
		mRDB.open();
		cq = new ContactsQuery(this);
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
			Drawable cachedImage = asyncImageLoader.loadDrawable(imagePath, new ImageCallback() {				
				public void imageLoaded(Drawable imageDrawable, String path) {
					ImageView imageViewByTag=null;
					imageViewByTag = (ImageView) mFriendsGrid.findViewWithTag(path);
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
	
}


