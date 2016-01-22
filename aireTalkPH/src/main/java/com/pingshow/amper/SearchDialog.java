package com.pingshow.amper;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.WriterException;
import com.pingshow.amper.LeftMenuAdapter.ViewHolder;
import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.contacts.RelatedUserInfo;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.GroupDB;
import com.pingshow.amper.db.RelatedUserDB;
import com.pingshow.amper.register.BeforeRegisterActivity;
import com.pingshow.network.MyNet;
import com.pingshow.qrcode.CaptureActivity;
import com.pingshow.qrcode.EncodingHandler;
import com.pingshow.qrcode.FindFriendsActivity;
import com.pingshow.qrcode.MyqrcodeActivity;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.util.MCrypt;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;

public class SearchDialog extends Activity {
	
	private MyPreference mPref;
//	private UserItemAdapter gridAdapter;
	private List<Map<String, String>> amperList;
	private AsyncImageLoader asyncImageLoader;
	private int numColumns=3;
	private GridView resultGridView;
	private AmpUserDB mADB;
	private RelatedUserDB mRDB;
	private ContactsQuery cq;
	private boolean searching;
	private final static int MAX_SEARCHED_ITEMS=450;
	private boolean isHide = false;
	private boolean hasFacebook = true;
	
	private volatile int search_mode = 0;
	private String passKeyword = "";
	
	private final static int POINT=1;
	private final static int CONTACTS=2;
	private final static int NEIGHBORHOOD=3;
	private final static int SEARCH=4;
	
	/*Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Toast.makeText(SearchDialog.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
		}
	};*/
	
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.search_page);
	    ListView addFriend = (ListView) findViewById(R.id.list_add);
	    AddFriendAdapter addFriendsAdapter = new AddFriendAdapter(SearchDialog.this);
	    ((ListView)addFriend.findViewById(R.id.list_add)).setAdapter(addFriendsAdapter);
	    
	    ((ImageView)findViewById(R.id.cancel_add_friends)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	  //xwf
		OnItemClickListener onClickMenuListener=new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				if (position==0)
				{
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
						Intent openCameraIntent = new Intent(SearchDialog.this, CaptureActivity.class);
						startActivityForResult(openCameraIntent, 105);
					} else {
						Toast.makeText(SearchDialog.this, getString(R.string.old_sdk), Toast.LENGTH_SHORT).show();
					}
				}
				else if (position==1)
				{
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
					
					startActivity(new Intent(SearchDialog.this, MyqrcodeActivity.class));
				}}
				else if (position==2)
				{
					Intent intent = new Intent(SearchDialog.this, FindFriendsActivity.class);
					intent.putExtra("Friend", FindFriendsActivity.POINT);
					startActivity(intent);
					
				}
				else if (position==3)
				{
					Intent intent = new Intent(SearchDialog.this, FindFriendsActivity.class);
					intent.putExtra("Friend", FindFriendsActivity.CONTACTS);
					startActivity(intent);
				}
				else if (position==4)
				{
					Intent intent = new Intent(SearchDialog.this, FindFriendsActivity.class);
					intent.putExtra("Friend", FindFriendsActivity.NEIGHBORHOOD);
					startActivity(intent);
				}
				else if (position==5)
				{
					Intent intent = new Intent(SearchDialog.this, FindFriendsActivity.class);
					intent.putExtra("Friend", FindFriendsActivity.SEARCH);
					startActivity(intent);
				}
			}
		};
		mADB = new AmpUserDB(this);
		mADB.open();
		cq = new ContactsQuery(this);
		mPref=new MyPreference(this);
		mRDB = new RelatedUserDB(this);
		mRDB.open();
	    ((ListView)addFriend.findViewById(R.id.list_add)).setOnItemClickListener(onClickMenuListener);
	    /*
	    mPref=new MyPreference(this);
	    Intent itnt = getIntent();
	    mADB = new AmpUserDB(this);
		mADB.open();
		
		mRDB = new RelatedUserDB(this);
		mRDB.open();
		cq = new ContactsQuery(this);
	      
//	    setContentView(R.layout.search_page_new);
	    this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

		neverSayNeverDie(SearchDialog.this);  //tml|bj*** neverdie/
	    
	    mADB = new AmpUserDB(this);
		mADB.open();
		
		mRDB = new RelatedUserDB(this);
		mRDB.open();
	    
	    Intent itnt = getIntent();
		
		if (getResources().getConfiguration().orientation!=1)
			numColumns=4;
		//tml*** beta ui2
//		String parentTag = ((RelativeLayout) findViewById(R.id.parent)).getTag().toString();
//		if (parentTag != null && parentTag.equals("normal"))
//			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		cq = new ContactsQuery(this);
		
	    mPref=new MyPreference(this);
	    amperList = new ArrayList<Map<String, String>>();
	    
	    gridAdapter = new UserItemAdapter(this);
	    
	    resultGridView = (GridView) findViewById(R.id.friendsGridView);
	    resultGridView.setNumColumns(numColumns);
	    
	    resultGridView.setAdapter(gridAdapter);
	    resultGridView.setOnItemClickListener(onChooseUser);
	    resultGridView.setBackgroundResource(R.drawable.tiled_bg);
	    
	    //li*** beta ui2
	    ((EditText) findViewById(R.id.keyword)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				hide(R.id.rl_search);
			}
		});

	    ((ImageView)findViewById(R.id.search)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			EditText kw = (EditText)findViewById(R.id.keyword);
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
	    			if (!MyUtil.checkNetwork(SearchDialog.this)) return;
	    			search_mode = 1;
	    			new Thread(searchFriends).start();
    			} else {
    				Toast.makeText(SearchDialog.this, getString(R.string.nickname_invalid), Toast.LENGTH_SHORT).show();
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
        
        if (!MyUtil.isAppInstalled(this,"com.facebook.katana"))
		{
			((Button) findViewById(R.id.facebook_search)).setVisibility(View.GONE);
			hasFacebook = false;  //li*** beta ui2
		}
		
        //tml*** new friends
        ((Button)findViewById(R.id.possible_friends)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				hide(R.id.possible_friends);  //li*** beta ui2
				mHandler.post(popupProgressDialog);
				if (!MyUtil.checkNetwork(SearchDialog.this)) return;
				search_mode = 4;
				new Thread(searchFriends).start();
				Toast.makeText(SearchDialog.this, getString(R.string.possible_friends), Toast.LENGTH_SHORT).show();
			}
		});
		//***tml
        
	    ((Button)findViewById(R.id.nearby_search)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mHandler.post(popupProgressDialog);
				if (!MyUtil.checkNetwork(SearchDialog.this)) return;
				search_mode = 2;
				new Thread(preSearchFriends).start();
				hide(R.id.nearby_search);  //li*** beta ui2
			}
		});

//	    ((Button)findViewById(R.id.nearby_search_global)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				mHandler.post(popupProgressDialog);
//				if (!MyUtil.checkNetwork(SearchDialog.this)) return;
//				search_mode = 3;
//				new Thread(preSearchFriends).start();
//			}
//		});
	    //li*** qr addf
	    ((Button) findViewById(R.id.qr_friends)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
					Intent openCameraIntent = new Intent(SearchDialog.this, CaptureActivity.class);
					startActivityForResult(openCameraIntent, 105);
				} else {
					Toast.makeText(SearchDialog.this, getString(R.string.old_sdk), Toast.LENGTH_SHORT).show();
				}
			}
		});
	    //tml|li*** qr addf
	    ((ImageButton) findViewById(R.id.my_qr)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
					try {
						String address = mPref.read("myPhoneNumber", "----");
						String nickname = mPref.read("myNickname", "");
						int idx = Integer.parseInt(mPref.read("myID", "0"), 16);
						String qrContent = "AddAireFriend," + address + "," + idx + "," + nickname;
						//li*** encrypt
						Log.d("qr preXN= " + qrContent);
						byte[] bytes = MCrypt.encrypt(qrContent.getBytes());
						qrContent = Base64.encodeToString(bytes, Base64.DEFAULT);
						Log.d("qr postXN= " + qrContent);
						
						DisplayMetrics displaymetrics = new DisplayMetrics();
						getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
						int width = displaymetrics.widthPixels;
						int height = displaymetrics.heightPixels;
						if (width > height) width = height;
						Bitmap qrCodeBitmap = EncodingHandler.createQRCode(qrContent, width * 3 / 5);
						Dialog qrCode = new Dialog(SearchDialog.this);
						View view = View.inflate(SearchDialog.this, R.layout.user_qrcode, null);
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
					Toast.makeText(SearchDialog.this, getString(R.string.old_sdk), Toast.LENGTH_SHORT).show();
				}
			}
		});
	    
	    ((Button)findViewById(R.id.facebook_search)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				hide(R.id.facebook_search);  //li*** beta ui2
				Intent it=new Intent(SearchDialog.this, FacebookSearch.class);
				it.putExtra("Mode", 0);
				startActivityForResult(it,103);
			}
		});
	    
	    ((Button)findViewById(R.id.phonebook_search)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				hide(R.id.phonebook_search);  //li*** beta ui2
				Intent it=new Intent(SearchDialog.this, FacebookSearch.class);
				it.putExtra("Mode", 1);
				startActivityForResult(it,103);
			}
		});
	    //li*** beta ui2
	    ((ImageView) findViewById(R.id.iv_arrow)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				hideOrShow(-1);
			}
		});
		
		
	    //tml*** search add
	    passKeyword = itnt.getStringExtra("passKeyword");
	    if (passKeyword != null) {
	    	if (!passKeyword.equals("")) {
			    ((EditText) findViewById(R.id.keyword)).setText(passKeyword);
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						((ImageView) findViewById(R.id.search)).performClick();
					}
				}, 300);
	    	}
	    }
	    //tml*** new friends
	    int c = mPref.readInt("searchFriendTips", 0);
		if (c < 3) {
			mHandler.postDelayed(showTooltip, 250);
			mPref.write("searchFriendTips", ++c);
		}
		//xwf
		
	}
	//li*** beta ui2
	private void hide(int viewId) {
		if (!isHide) {
			hideOrShow(viewId);
		}
	}
	
	private void hideOrShow(int viewId) {
		int[] vis = {R.id.ll_scan,
				R.id.nearby_search,
				R.id.possible_friends,
				R.id.phonebook_search,
				R.id.facebook_search,
				R.id.rl_search};
		for (int i = 0; i <vis.length; i++) {
			if (!hasFacebook && vis[i] == R.id.facebook_search) {
				continue;
			}
			if (vis[i] != viewId) {
				findViewById(vis[i]).setVisibility(isHide ? View.VISIBLE : View.GONE);
			}
		}
		isHide = !isHide;
		((ImageView) findViewById(R.id.iv_arrow)).setImageResource(isHide ? R.drawable.ic_expand_more_white : R.drawable.ic_expand_less_white);
	}
	//***li
	
	OnItemClickListener onChooseUser=new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			Map<String, String> map = amperList.get(position);
			
			String address=map.get("address");
			Log.d("hello! " + address + " " + map.get("displayName") + " " + Integer.parseInt(map.get("idx")));
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
		else if (requestCode==103)
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
							if (!new File(userphotoPath).exists())
							{
								userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
								if (!new File(userphotoPath).exists())
									userphotoPath=null;
							}
							if (userphotoPath == null) {
								Log.w("null pic! " + disName + " path=" + userphotoPath);
							}
							
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
				msg.obj = String.format(getString(R.string.loading)
						+ " " + getString(R.string.search_result), found);
				mHandler.sendMessage(msg);
				
				mHandler.post(new Runnable(){
					public void run(){
						gridAdapter.notifyDataSetChanged();
//						((FrameLayout)findViewById(R.id.search_ext)).setVisibility(View.GONE);  //li*** beta ui2 X
					}
				});
			}
		} else if (requestCode == 105) {  //tml|li*** qr addf
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
									Intent it = new Intent(SearchDialog.this, FunctionActivity.class);
									long contactId = cq.getContactIdByNumber(address);
									it.putExtra("Contact_id", contactId);
									it.putExtra("Address", address);
									it.putExtra("Nickname", nickname);
									it.putExtra("Idx", idx);
									startActivity(it);
								} else {
									Intent it = new Intent(SearchDialog.this, AddAsFriendActivity.class);
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
					Toast.makeText(SearchDialog.this, getString(R.string.qr_invalid) + falseScan, Toast.LENGTH_SHORT).show();
				}
			}
		}
	}
	
	private ProgressDialog progressDialog;
	Runnable popupProgressDialog = new Runnable() {
		@Override
		public void run() {
			try {
				if (progressDialog == null)
					progressDialog = ProgressDialog.show(SearchDialog.this, "", getString(R.string.in_progress), true, true);
			} catch (Exception e) {}
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
	
	//tml*** new friends
	Runnable preSearchFriends = new Runnable() {
		public void run() {
			int count = 0;
			String Return = null;
			boolean preSearchRdy = false;
			searching = true;
			
			try {
				MyNet net = new MyNet(SearchDialog.this);
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
	
	Runnable showTooltip = new Runnable() {
    	public void run() {
    		Intent it=new Intent(SearchDialog.this, Tooltip.class);
            it.putExtra("Content", getString(R.string.help_instantfriend));
            startActivity(it);
    	}
    };
	//***tml
	
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
					MyNet net = new MyNet(SearchDialog.this);
					String myIdx = mPref.read("myID", "0");  //hex
//					foundUserXML = net.doPostHttpWithXML("search_aire.php", "k=" + URLEncoder.encode(mKeyword), null);
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
//						((FrameLayout)findViewById(R.id.search_ext)).setVisibility(View.GONE);  //li*** beta ui2 X
					}
				});
				
				mHandler.post(dismissProgress);
				
				Message msg = new Message();
				msg.obj = String.format(getString(R.string.search_result), foundUserXML.size());
				mHandler.sendMessage(msg);
				
				if (AireJupiter.getInstance()!=null && MyUtil.checkSDCard(SearchDialog.this) && amperList.size()>0 && searching)
				{
					try {
						MyNet net = new MyNet(SearchDialog.this);
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
	//xwf
	@Override
	protected void onDestroy() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);  //tml*** beta ui2
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
	*/
	
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
									Intent it = new Intent(SearchDialog.this, FunctionActivity.class);
									long contactId = cq.getContactIdByNumber(address);
									it.putExtra("Contact_id", contactId);
									it.putExtra("Address", address);
									it.putExtra("Nickname", nickname);
									it.putExtra("Idx", idx);
									startActivity(it);
								} else {
									Intent it = new Intent(SearchDialog.this, AddAsFriendActivity.class);
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
					Toast.makeText(SearchDialog.this, getString(R.string.qr_invalid) + falseScan, Toast.LENGTH_SHORT).show();
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
	
}