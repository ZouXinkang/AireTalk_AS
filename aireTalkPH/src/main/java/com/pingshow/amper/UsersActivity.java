package com.pingshow.amper;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.GroupDB;
import com.pingshow.amper.db.RelatedUserDB;
import com.pingshow.amper.db.SmsDB;
import com.pingshow.amper.db.WTHistoryDB;
import com.pingshow.qrcode.PopwindowDialog;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;
import com.pingshow.voip.VideoCallActivity;
import com.pingshow.voip.core.VoipCall;
import com.pingshow.voip.core.VoipCore;

public class UsersActivity extends Activity {
	
	//tml|bj*** neverdie
	static public UsersActivity _this;
	
	static final int MAX_USERS=300;
	static int orientation=-1;
	static public boolean needRefresh=true;
	private MyPreference mPref;
	private AmpUserDB mADB;
	private RelatedUserDB mRDB;
	private GroupDB mGDB;
	private ContactsQuery cq;
	static private List<Map<String, String>> [] amperList;
	private UserItemAdapter [] friendAdapter=(UserItemAdapter[])new UserItemAdapter[2];
	static private List<Map<String, String>> orgList;

	private int displayType=0;
	
	private GridView friendGrid;
	private ListView friendList;
	private FrameLayout mDropDownList;
	private AsyncImageLoader asyncImageLoader;
	
	private ImageView mModeBtn;
	private ImageView mMoreBtn;
	
	private Handler mHandler=new Handler();
	
	private int numColumns=3;
	public static int numTrueFriends=1;
	private int editing=0;
	
	static public boolean forceRefresh=true;
	static public int sortMethod=0;
	
	private boolean largeScreen;
	private float mDensity = 1.f;
	
	static public boolean uiUAinFore = false;

	//jack 2.4.51 hardcode
	public static Activity myUsersActivity;
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.user_page);
		this.overridePendingTransition(R.anim.freeze, R.anim.freeze);
		_this = this;
		neverSayNeverDie(_this);  //tml|bj*** neverdie/

		myUsersActivity=this;

		mPref=new MyPreference(this);
		int ort = getResources().getConfiguration().orientation;
		needRefresh=((ort!=orientation)||needRefresh);
		orientation = ort;
		
		largeScreen=(findViewById(R.id.large)!=null);
		
		mDensity = getResources().getDisplayMetrics().density;
		
		if (largeScreen)
		{
			if (orientation!=1)
				numColumns=5;
			else
				numColumns=4;
		}else{
			if (orientation!=1)
				numColumns=4;
		}

		mADB = new AmpUserDB(this);
		mADB.open();
		
		mRDB = new RelatedUserDB(this);
		mRDB.open();
		
		mGDB = new GroupDB(this);
		mGDB.open();
		
		editing=0;
		cq = new ContactsQuery(this);
		
		displayType=mPref.readInt("displayType", 1);
		
		if (amperList==null)
		{
			amperList=(List<Map<String, String>>[])new ArrayList[2];
			amperList[0] = new ArrayList<Map<String, String>>();
			amperList[1] = new ArrayList<Map<String, String>>();
		}
		if (orgList == null) {
			orgList = new ArrayList<Map<String, String>>();
		}

		friendAdapter[0] = new UserItemAdapter(this, 0);
		friendAdapter[1] = new UserItemAdapter(this, 1);
		
		friendGrid = (GridView) findViewById(R.id.friendsGridView);
		friendGrid.setBackgroundResource(R.drawable.tiled_bg);
		friendList = (ListView) findViewById(R.id.friendsList);
		friendList.setBackgroundResource(R.drawable.tiled_bg);
		//tml*** temp alpha ui, CX
		mModeBtn=(ImageView)findViewById(R.id.mode);
		mMoreBtn=(ImageView)findViewById(R.id.more);
		
		if (displayType==1)
		{
			friendGrid.setVisibility(View.GONE);
			friendList.setVisibility(View.VISIBLE);
			mModeBtn.setImageResource(R.drawable.mode_grid);
		}else{
			friendGrid.setVisibility(View.VISIBLE);
			friendList.setVisibility(View.GONE);
			mModeBtn.setImageResource(R.drawable.mode_list);
		}
		
		friendGrid.setNumColumns(numColumns);
		
		friendGrid.setOnScrollListener(new OnScrollListener(){
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			}
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (mDropDownList.getVisibility()==View.VISIBLE)
				{
					mDropDownList.setVisibility(View.GONE);
					mMoreBtn.setImageResource(R.drawable.dropdown);  //tml*** beta ui, was dropdown
					mMoreBtn.setBackgroundResource(R.drawable.optionbtn);
					int mMoreBtnPad = 0;
					if (largeScreen) {
						mMoreBtnPad = (int) (15 * mDensity);
					} else {
						mMoreBtnPad = (int) (10 * mDensity);
					}
					mMoreBtn.setPadding(mMoreBtnPad, mMoreBtnPad, mMoreBtnPad, mMoreBtnPad);
				}
			}
        });
		
		friendList.setOnScrollListener(new OnScrollListener(){
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			}
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (mDropDownList.getVisibility()==View.VISIBLE)
				{
					mDropDownList.setVisibility(View.GONE);
					mMoreBtn.setImageResource(R.drawable.add2);  //tml*** beta ui, was dropdown
					mMoreBtn.setBackgroundResource(R.drawable.optionbtn);
					int mMoreBtnPad = 0;
					if (largeScreen) {
						mMoreBtnPad = (int) (15 * mDensity);
					} else {
						mMoreBtnPad = (int) (10 * mDensity);
					}
					mMoreBtn.setPadding(mMoreBtnPad, mMoreBtnPad, mMoreBtnPad, mMoreBtnPad);
				}
			}
        });

		friendGrid.setOnItemClickListener(onChooseUser);
		friendList.setOnItemClickListener(onChooseUser);
		
		friendGrid.setOnItemLongClickListener(onChooseUserLongClick);
		friendList.setOnItemLongClickListener(onChooseUserLongClick);
		
		//tml*** hw-menu substitute
//		try {
//			String[] myDevice = mPref.read("myBrandDeviceModelProduct", "---,---,---,---").split(",");
//			if (myDevice[0].contains("yotaphone") && myDevice[3].contains("yotaphone")
//					|| (Build.VERSION.SDK_INT >= 14 && !ViewConfiguration.get(this).hasPermanentMenuKey())) {
//				((ImageView) findViewById(R.id.menu)).setVisibility(View.VISIBLE);
//				((ImageView) findViewById(R.id.menu)).setOnClickListener(new OnClickListener() {
//					@Override
//					public void onClick(View v) {
//						openOptionsMenu();
//					}
//				});
//			} else {
//				((ImageView) findViewById(R.id.menu)).setVisibility(View.GONE);
//			}
//		} catch (Exception e) {
//			((ImageView) findViewById(R.id.menu)).setVisibility(View.GONE);
//		}
		
		((Button)findViewById(R.id.block)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				editing=2;
				mMoreBtn.setImageResource(R.drawable.done);
				mMoreBtn.setBackgroundResource(R.drawable.graybtn);
				int mMoreBtnPad = 0;
				if (largeScreen) {
					mMoreBtnPad = (int) (15 * mDensity);
				} else {
					mMoreBtnPad = (int) (10 * mDensity);
				}
				mMoreBtn.setPadding(mMoreBtnPad, mMoreBtnPad, mMoreBtnPad, mMoreBtnPad);
				friendAdapter[displayType].notifyDataSetChanged();
				mDropDownList.setVisibility(View.GONE);
			}
		});
		
//		if (!AmazonKindle.hasMicrophone_NoWarnning(this))
//			((Button)findViewById(R.id.bConference)).setVisibility(View.GONE);
		//tml*** beta ui
		((Button) findViewById(R.id.bConference)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it = new Intent(UsersActivity.this, PickupActivity.class);
				it.putExtra("conference", true);
//				startActivityForResult(it, 108);
//				mDropDownList.setVisibility(View.GONE);
				startActivity(it);
				finish();
			}
		});
		((ImageView)findViewById(R.id.add_group)).setVisibility(View.GONE);
		((ImageView)findViewById(R.id.add_group)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (editing > 0) {
					editing = 0;
					Map<String, String> map;
					for (int i = 0; i < amperList[displayType].size(); i++) {
						map = amperList[displayType].get(i);
						map.put("blocked", "0");
					}
					friendAdapter[displayType].notifyDataSetChanged();
					mDropDownList.setVisibility(View.GONE);
					mMoreBtn.setImageResource(R.drawable.add2);
					mMoreBtn.setBackgroundResource(R.drawable.optionbtn);
					int mMoreBtnPad = 0;
					if (largeScreen) {
						mMoreBtnPad = (int) (15 * mDensity);
					} else {
						mMoreBtnPad = (int) (10 * mDensity);
					}
					mMoreBtn.setPadding(mMoreBtnPad, mMoreBtnPad, mMoreBtnPad, mMoreBtnPad);
					
				}
				
				if (mGDB.getGroupCount()>25)
				{
					Intent it = new Intent(UsersActivity.this, CommonDialog.class);
					it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

					it.putExtra("msgContent", getString(R.string.group_number_exceeds));
					it.putExtra("numItems", 1);
					it.putExtra("ItemCaption0", getString(R.string.done));
					it.putExtra("ItemResult0", RESULT_OK);
					startActivity(it);
					return;
				}
				startActivityForResult(new Intent(UsersActivity.this,CreateGroupActivity.class),2001);
				mDropDownList.setVisibility(View.GONE);
			}
		});
		
		((Button)findViewById(R.id.delete)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				editing=1;
				mMoreBtn.setImageResource(R.drawable.done);
				mMoreBtn.setBackgroundResource(R.drawable.graybtn);
				int mMoreBtnPad = 0;
				if (largeScreen) {
					mMoreBtnPad = (int) (15 * mDensity);
				} else {
					mMoreBtnPad = (int) (10 * mDensity);
				}
				mMoreBtn.setPadding(mMoreBtnPad, mMoreBtnPad, mMoreBtnPad, mMoreBtnPad);
				friendAdapter[displayType].notifyDataSetChanged();
				mDropDownList.setVisibility(View.GONE);
			}
		});
		
//		((Button)findViewById(R.id.search)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				
//				if (mPref.readBoolean("permissionReadContacts",false))
//				{
//					Intent intent = new Intent();
//					intent.setAction(Global.Action_InternalCMD);
//					intent.putExtra("Command", Global.CMD_QUERY_360);
//					sendBroadcast(intent);
//				}
//
//				startActivity(new Intent(UsersActivity.this, SearchDialog.class));
//				
//				mDropDownList.setVisibility(View.GONE);
//			}
//		});
		((ImageView)findViewById(R.id.timeline)).setVisibility(View.GONE);
		((ImageView)findViewById(R.id.timeline)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (editing > 0) {
					editing = 0;
					Map<String, String> map;
					for (int i = 0; i < amperList[displayType].size(); i++) {
						map = amperList[displayType].get(i);
						map.put("blocked", "0");
					}
					friendAdapter[displayType].notifyDataSetChanged();
					mDropDownList.setVisibility(View.GONE);
					mMoreBtn.setImageResource(R.drawable.add2);
					mMoreBtn.setBackgroundResource(R.drawable.optionbtn);
					int mMoreBtnPad = 0;
					if (largeScreen) {
						mMoreBtnPad = (int) (15 * mDensity);
					} else {
						mMoreBtnPad = (int) (10 * mDensity);
					}
					mMoreBtn.setPadding(mMoreBtnPad, mMoreBtnPad, mMoreBtnPad, mMoreBtnPad);
				}
				
				int myIdx=0;
				try {
					myIdx=Integer.parseInt(mPref.read("myID","0"),16);
				} catch (Exception e) {}
				
				Intent i = new Intent(UsersActivity.this, TimeLine.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.putExtra("displayname", mPref.read("myNickname","Myself"));
				i.putExtra("address", mPref.read("myPhoneNumber", "----"));
				i.putExtra("Idx", myIdx);
				startActivity(i);
				if (mDropDownList.getVisibility() == View.VISIBLE) {  //tml*** longclick edit
					mDropDownList.setVisibility(View.GONE);
				}
			}
		});
		//tml*** temp alpha ui, CX
		mModeBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (displayType==0)
				{
					displayType=1;
					friendGrid.setVisibility(View.GONE);
					friendList.setVisibility(View.VISIBLE);
					mModeBtn.setImageResource(R.drawable.mode_grid);
				}else{
					displayType=0;
					friendGrid.setVisibility(View.VISIBLE);
					friendList.setVisibility(View.GONE);
					mModeBtn.setImageResource(R.drawable.mode_list);
				}
				mPref.write("displayType",displayType);
				friendAdapter[displayType].notifyDataSetInvalidated();
				
				if (mDropDownList.getVisibility()==View.VISIBLE)
				{
					mDropDownList.setVisibility(View.GONE);
					mMoreBtn.setImageResource(R.drawable.add2);  //tml*** beta ui, was dropdown
					mMoreBtn.setBackgroundResource(R.drawable.optionbtn);
					int mMoreBtnPad = 0;
					if (largeScreen) {
						mMoreBtnPad = (int) (15 * mDensity);
					} else {
						mMoreBtnPad = (int) (10 * mDensity);
					}
					mMoreBtn.setPadding(mMoreBtnPad, mMoreBtnPad, mMoreBtnPad, mMoreBtnPad);
				}
			}
		});
		
		//tml*** test
		((Button)findViewById(R.id.bFafauser)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});
	    
        ((Button)findViewById(R.id.bMessage)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(UsersActivity.this, MessageActivity.class));
				finish();
			}
		});
        ((Button)findViewById(R.id.bSearch)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(UsersActivity.this, PublicWalkieTalkie.class));
				finish();
			}
		});
        
        if (!AmazonKindle.hasMicrophone_NoWarnning(this))
        	((Button)findViewById(R.id.bAireCall)).setVisibility(View.GONE);
        
        ((Button)findViewById(R.id.bAireCall)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(UsersActivity.this, SipCallActivity.class));
				finish();
			}
		});
        //tml*** temp alpha ui, CX/
        ((Button)findViewById(R.id.bSetting)).setVisibility(View.GONE);
        ((Button)findViewById(R.id.bSetting)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(UsersActivity.this, SettingActivity.class));
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
        if (largeScreen) {
        	((Button) findViewById(R.id.bSetting)).setVisibility(View.GONE);
        }
        
        mDropDownList=(FrameLayout)findViewById(R.id.dropdown_list);
        
        mMoreBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(UsersActivity.this, PopwindowDialog.class));
				/*if (editing>0)
				{
					editing=0;
					Map<String, String> map;
					for(int i=0;i<amperList[displayType].size();i++)
					{
						map = amperList[displayType].get(i);
						map.put("blocked", "0");
					}
					friendAdapter[displayType].notifyDataSetChanged();
					mDropDownList.setVisibility(View.GONE);
					mMoreBtn.setImageResource(R.drawable.add2);  //tml*** beta ui, was dropdown
					mMoreBtn.setBackgroundResource(R.drawable.optionbtn);
					int mMoreBtnPad = 0;
					if (largeScreen) {
						mMoreBtnPad = (int) (15 * mDensity);
					} else {
						mMoreBtnPad = (int) (10 * mDensity);
					}
					mMoreBtn.setPadding(mMoreBtnPad, mMoreBtnPad, mMoreBtnPad, mMoreBtnPad);
					return;
				}
//				if (mDropDownList.getVisibility()==View.VISIBLE)
//				{
//					mDropDownList.setVisibility(View.GONE);
//					mMoreBtn.setImageResource(R.drawable.add2);  //tml*** beta ui, was dropdown
//				}else{
//					mDropDownList.setVisibility(View.VISIBLE);
//					//mMoreBtn.setImageResource(R.drawable.dropdown_up);
//				}
				//tml*** search add
				String passKeyword = ((EditText) findViewById(R.id.searchkeyword)).getText().toString().trim();
				if (passKeyword.length() > 0) {
					((EditText) findViewById(R.id.searchkeyword)).setText("");
				}
				((EditText) findViewById(R.id.searchkeyword)).setVisibility(View.GONE);
				((ImageView) findViewById(R.id.clearkeyword)).setVisibility(View.GONE);
				//tml*** longclick edit
				if (mPref.readBoolean("permissionReadContacts",false)) {
					Intent intent = new Intent();
					intent.setAction(Global.Action_InternalCMD);
					intent.putExtra("Command", Global.CMD_QUERY_360);
					sendBroadcast(intent);
				}
				//xwf
				Intent i = new Intent(UsersActivity.this, SearchDialog.class);
				i.putExtra("passKeyword", passKeyword);
				startActivity(i);
				mDropDownList.setVisibility(View.GONE);*/
			}
		});
        
        ((Button)findViewById(R.id.blacklist)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(UsersActivity.this, BlackListActivity.class));
				mDropDownList.setVisibility(View.GONE);
			}
		});
        //tml*** search add
        ((ImageView) findViewById(R.id.clearkeyword)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
	            InputMethodManager imm = (InputMethodManager) ((EditText) findViewById(R.id.searchkeyword)).getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	            if (imm != null) {
	                imm.hideSoftInputFromWindow(((EditText) findViewById(R.id.searchkeyword)).getWindowToken(), 0);
	            }
				((EditText) findViewById(R.id.searchkeyword)).setText("");
				onFilterUserQuery("");
				((EditText) findViewById(R.id.searchkeyword)).setVisibility(View.GONE);
				((ImageView) findViewById(R.id.clearkeyword)).setVisibility(View.GONE);
			}
		});

		((EditText) findViewById(R.id.searchkeyword)).addTextChangedListener (new TextWatcher() {
	        @Override
	        public void afterTextChanged (Editable s) {
	        	onFilterUserQuery(s.toString());
	        }
	        @Override
	        public void onTextChanged (CharSequence s, int start, int before, int count) {}
	        @Override
	        public void beforeTextChanged (CharSequence s, int start, int count, int after) {}
	    });
        //***tml
        
        IntentFilter intentToReceiveFilter = new IntentFilter();
		intentToReceiveFilter.addAction(Global.Action_Refresh_Gallery);
		intentToReceiveFilter.addAction(Global.Action_Friends_Status_Updated);
		registerReceiver(handleFreshItems, intentToReceiveFilter);
		
		mPref.write("LastPage", 0);
		
		friendGrid.setAdapter(friendAdapter[0]);
		friendList.setAdapter(friendAdapter[1]);
		
		sortMethod=mPref.readInt("SortMethod", 0);
		
		onFafaUserQuery();
		mHandler.post(new Runnable(){
			public void run()
			{
				friendAdapter[displayType].notifyDataSetChanged();
//				friendList.setOnScrollListener(onScrollUser);
//				listNormVisCount = friendList.getLastVisiblePosition() - friendList.getFirstVisiblePosition() + 1;
//				Log.e("test listNormVisCount" + listNormVisCount);
//				View convertView = friendList.getChildAt(3);
//				ViewHolder holder = (ViewHolder) convertView.getTag();
//				holder.friendName.setTextColor(Color.BLUE);
			}
		});
		
		mHandler.postDelayed(mInstantQueryOnlineFriends, 3000);
		
	};
//	private int topItem = 0;
//	private int lastItem = -1;
//	private int listNormVisCount = 0;
//	OnScrollListener onScrollUser = new OnScrollListener() {
//		private boolean scrolling = false;
//		private int lastScrollState = -1;
//		
//		@Override
//		public void onScrollStateChanged(AbsListView view, int scrollState) {
//			if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
//				scrolling = false;
//				if (lastScrollState == OnScrollListener.SCROLL_STATE_FLING) {
//					scrolling = true;
//				}
//			} else if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
//				scrolling = false;
//			} else {
//				scrolling = true;
//			}
//			lastScrollState = scrollState;
//		}
//
//		@Override
//		public void onScroll(AbsListView view, int firstVisibleItem,
//				int visibleItemCount, int totalItemCount) {
//			if (scrolling) {
//				topItem = firstVisibleItem + 1;
//				myScrollTask = new MyScrollTask(firstVisibleItem + 1);
//				myScrollTask.execute();
//			} else {
//				if (firstVisibleItem == 0 && listNormVisCount == visibleItemCount) {
//					topItem = firstVisibleItem;
//					myScrollTask = new MyScrollTask(firstVisibleItem);
//					myScrollTask.execute();
//				}
//			}
//			lastItem = topItem;
//		}
//	};
//	
//	private MyScrollTask myScrollTask;
//	private class MyScrollTask extends AsyncTask<Void, Void, Void> {
//		
//		MyScrollTask (int firstItem) {
//			
//		}
//
//		@Override
//		protected Void doInBackground(Void... params) {
//			return null;
//		}
//		
//		@Override
//		protected void onPostExecute(Void result) {
//			friendAdapter[displayType].notifyDataSetChanged();
//			super.onPostExecute(result);
//		}
//		
//	}
	
	OnItemClickListener onChooseUser=new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			if (editing==0)
			{
				if (mDropDownList.getVisibility() == View.VISIBLE) {  //tml*** longclick edit
					mDropDownList.setVisibility(View.GONE);
				} else {
					Map<String, String> map = amperList[displayType].get(position);
					
					String address = map.get("address");
					//tml*** beta ui3
					int idx = 0;
					if (!address.startsWith("[<GROUP>]")) {
						idx = Integer.parseInt(map.get("idx"), 16);
					} else {
						idx = Integer.parseInt(map.get("idx"));
					}
					
					if (address.equals("----")) return;
					long contact_id=Long.parseLong((String)map.get("contactId"));
					
					String Nickname=map.get("displayName");
					
					if (mADB.isFafauser(address))
					{
//						try{
//							Intent it=new Intent(UsersActivity.this, FunctionActivity.class);
//							it.putExtra("Contact_id", contact_id);
//							it.putExtra("Address", address);
//							it.putExtra("Nickname", Nickname);
//							it.putExtra("Online", ContactsOnline.getContactOnlineStatus(address));
//							if (contact_id>0)
//								it.putExtra("AireNickname", mADB.getNicknameByAddress(address));
//							it.putExtra("Idx", Integer.parseInt(map.get("idx")));
//							startActivity(it);
//						}catch(Exception e){}
						//tml*** beta ui3
						if (sortMethod == 1 && mADB.isOpen()) {
							mADB.updateLastContactTimeByIdx(idx, new Date().getTime());
						}
						Intent it=new Intent(UsersActivity.this, ConversationActivity.class);
						it.putExtra("SendeeContactId", contact_id);
						it.putExtra("SendeeNumber", address);
						it.putExtra("SendeeDisplayname", Nickname);
						//jack 2.4.51 hardcode
						it.putExtra("photopath",map.get("imagePath"));
						startActivity(it);
					}
					else{
						try{
							Intent it=new Intent(UsersActivity.this, AddAsFriendActivity.class);
							it.putExtra("Address", address);
							it.putExtra("Nickname", Nickname);
							it.putExtra("Idx", Integer.parseInt(map.get("idx")));
							it.putExtra("Joint", mRDB.getJointFriendsByAddress(address));
							startActivityForResult(it, 10);
						}catch(Exception e){}
					}
				}
				
//				if (mDropDownList.getVisibility()==View.VISIBLE)  //tml*** longclick edit, remove
//					mDropDownList.setVisibility(View.GONE);
			}
		}
	};
	
	private ArrayList<String> chatroomMemberslist = new ArrayList<String>();
	
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
			
			for(int i=0; i<chatroomMemberslist.size(); i++)
			{
				int idx=Integer.parseInt(chatroomMemberslist.get(i));
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
	
	@Override  
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);  
		if (requestCode==108)
		{
			if (resultCode==RESULT_OK) {
				try{
					chatroomMemberslist.clear();
					String idxArray=data.getStringExtra("idx");
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
		}else if (requestCode==2001)
		{
			if (resultCode==RESULT_OK) {
				needRefresh=true;
				onFafaUserQuery();
				friendAdapter[displayType].notifyDataSetChanged();
			}
		}
		else if (requestCode==10)
		{
			if (resultCode==RESULT_OK) {
				
				if (!MyUtil.checkNetwork(this)) {
					return;
				}
				
				needRefresh=true;
				String address=data.getExtras().getString("Address");
				String nickname=data.getExtras().getString("Nickname");
				int idx=data.getExtras().getInt("Idx");
				
				if (!address.equals(AireJupiter.myPhoneNumber))
					mADB.insertUser(address, idx, nickname);
				
				ContactsOnline.setContactOnlineStatus(address, 0);
				mRDB.deleteContactByAddress(address);
				onFafaUserQuery();
				friendAdapter[displayType].notifyDataSetChanged();
				
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
				
				Intent it3 = new Intent(Global.Action_InternalCMD);
				it3.putExtra("Command", Global.CMD_SEARCH_POSSIBLE_FRIENDS);
				sendBroadcast(it3);
			}else if (requestCode==1017) {
				needRefresh=true;
				onFafaUserQuery();
				friendAdapter[displayType].notifyDataSetChanged();
			}
		} else if (requestCode == 3838) {
		}
	}
	
	
	OnItemLongClickListener onChooseUserLongClick = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int position, long id) {
//			if (!MyUtil.checkNetwork(UsersActivity.this))
//				return true;
//			
//			Map<String, String> map = amperList[displayType].get(position);
//			
//			String address = map.get("address");
//			if (address.startsWith("[<GROUP>]")) {
//				int GroupID=0;
//				try{
//					GroupID=Integer.parseInt(address.substring(9));
//				}catch(Exception e){}
//				Intent it = new Intent(UsersActivity.this, JoinNewGroupActivity.class);
//				it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				it.putExtra("Address", "[<GROUP>]"+GroupID);
//				it.putExtra("Idx", GroupID+100000000);
//				it.putExtra("Nickname", map.get("displayName"));
//				it.putExtra("Creator", "");
//				it.putExtra("Edit", true);
//				it.putExtra("GroupId", GroupID);
//				startActivity(it);
//				return true;
//			}
//			if (address.equals("----")) return true;
//			if (mADB.isFafauser(address))
//			{
//				try{
//					long contactId = Integer.parseInt(map.get("contactId"));
//					int idx=Integer.parseInt(map.get("idx"));
//					Intent it=new Intent(UsersActivity.this, WalkieTalkieDialog.class);
//					it.putExtra("Contact_id", contactId);
//					it.putExtra("Address", address);
//					it.putExtra("Idx", idx);
//					startActivity(it);
//				}catch(Exception e){}
//			}
			//tml*** longclick edit
			if (editing == 0) {
				if (mDropDownList.getVisibility() == View.VISIBLE) {
					mDropDownList.setVisibility(View.GONE);
				} else {
					mDropDownList.setVisibility(View.VISIBLE);
				}
			}
			return true;
			//***tml
//			return false;
		}
	};
	
	int fresh_count=0;
	
	final Runnable mInstantQueryOnlineFriends = new Runnable() {
		public void run() {
			Intent it = new Intent(Global.Action_InternalCMD);
			it.putExtra("Command", Global.CMD_CHECK_ONLINE_FRIENDS);
			sendBroadcast(it);
			
			if (fresh_count++<5)
				mHandler.postDelayed(mInstantQueryOnlineFriends, 120000);
		}
	};
	
	BroadcastReceiver handleFreshItems = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context,final Intent intent) {
			if (intent.getAction().equals(Global.Action_Refresh_Gallery)) {
				if (amperList != null && editing==0)
				{
					needRefresh=true;
					Log.d("tml friendlist receiver1! " + editing + forceRefresh + needRefresh + " " + intent.getAction());
					onFafaUserQuery();
					friendAdapter[displayType].notifyDataSetChanged();
				}
			}
			else if (intent.getAction().equals(Global.Action_Friends_Status_Updated)) {
				if (forceRefresh)
				{
					needRefresh=true;
					Log.d("tml friendlist receiver2! " + editing + forceRefresh + needRefresh + " " + intent.getAction());
					onFafaUserQuery();
					friendAdapter[displayType].notifyDataSetChanged();
					return;
				}
				if (amperList != null && amperList[displayType].size()>1)
				{
					Log.d("tml friendlist receiver3! " + editing + forceRefresh + needRefresh + " " + intent.getAction());
					friendAdapter[displayType].notifyDataSetChanged();
				}
			}
		}
	};
	
	/*
	private boolean isChineseChar(String name, String [] spell)
	{
		boolean containChinese = false;
		if(name.getBytes().length != name.length()){
			String str = ChineseTospell.t(name);
			if(str.getBytes().length != str.length()){
				containChinese = true;
				spell[0] = ChineseTospell.toPinYin(str);
			}
		}
		return containChinese;
	}*/
	class ViewHolder {
		TextView friendName;
		ImageView photoimage;
		ImageView delete;
		ImageView securityiam;
		TextView mask;
		TextView separator;
		ImageView online;
		TextView mood;
	}

	public  class UserItemAdapter extends BaseAdapter {
		Context icontext;
		int type;

		public UserItemAdapter(Context context, int type) {
			icontext = context;
			this.type=type;
			asyncImageLoader = new AsyncImageLoader(context);
		}

		@Override
		public int getCount() {
			int count=amperList[type].size();
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
		
		OnClickListener blockCandidate=new OnClickListener(){
			@Override
			public void onClick(View v) {
				Map<String, String> map;
				int position=Integer.parseInt((String)v.getTag());
				for(int i=0;i<amperList[type].size();i++)
				{
					map = amperList[type].get(i);
					if (i==position)
					{
						String b=map.get("blocked");
						if (b.equals("1"))
							map.put("blocked", "0");
						else
							map.put("blocked", "1");
					}
					else
						map.put("blocked", "0");
				}
				friendAdapter[type].notifyDataSetChanged();
			}
		};
		
		OnClickListener deleteBlockedUser=new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (editing==0) return;
				
				int position=Integer.parseInt((String)v.getTag());
				Map<String, String> map = amperList[type].get(position);
				
				String address = map.get("address");
				
				if (editing==2)//BLOCK
				{
					String b=map.get("blocked");
					if (b.equals("0"))
						return;
					String actual = map.get("actual");
					
					if (actual.equals("1"))
					{
						int idx=mADB.getIdxByAddress(address);
						if (idx<50 && idx!=4) return;
						
						mADB.blockUserByAddress(address, 1);//Block User
						
						if (address.startsWith("[<GROUP>]"))//alec
						{
							int GroupID=0;
							try{
								GroupID=Integer.parseInt(address.substring(9));
							}catch(Exception e){}
							
							Intent it = new Intent(Global.Action_InternalCMD);
							it.putExtra("Command", Global.CMD_DELETE_GROUP);
							it.putExtra("GroupID", GroupID);
							sendBroadcast(it);
						}
						else if (idx>50)
						{
							Intent it = new Intent(Global.Action_InternalCMD);
							it.putExtra("Command", Global.CMD_UPLOAD_FRIENDS);
							it.putExtra("type", 1);//Single Friend
							it.putExtra("serverType", 0);//Remove
							it.putExtra("idxlist",idx+"");
							sendBroadcast(it);
							
							//tml*** multi suvei
							int maxSuvei = Global.MAX_SUVS;
							for (int j = 0; j < maxSuvei; j++) {
								String addr = mPref.read("Suvei" + j);
								if (addr.equals(address)) {
									mPref.delect("Suvei" + j);
									if (AireJupiter.getInstance() != null
											&& AireJupiter.getInstance().tcpSocket.isLogged(false)) {
										AireJupiter.getInstance().tcpSocket
										.send(address, "REVOKE my GUARD access FINAL", 0, null, null, 0, null);
									}
									break;
								}
							}
							//***tml
						}
						
						ComposeActivity.deleteUserInList(idx);
					}
					else{
						//The user is not in user list
						mRDB.blockUserByAddress(address, 1);//Block User
					}
				}
				else if (editing==1)//Delete
				{
//					mPref.delect("Inviting:" + address);  //tml*** friend invite
					String b=map.get("blocked");
					if (b.equals("0"))
						return;
					String actual = map.get("actual");
					if (actual.equals("1"))
					{
						int idx=mADB.getIdxByAddress(address);
						if (idx<50) return;
						
						mADB.deleteContactByAddress(address);//remove user
						
						if (address.startsWith("[<GROUP>]"))//alec
						{
							int GroupID=0;
							try{
								GroupID=Integer.parseInt(address.substring(9));
							}catch(Exception e){}
							
							Intent it = new Intent(Global.Action_InternalCMD);
							it.putExtra("Command", Global.CMD_DELETE_GROUP);
							it.putExtra("GroupID", GroupID);
							sendBroadcast(it);
						}
						else{
							Intent it = new Intent(Global.Action_InternalCMD);
							it.putExtra("Command", Global.CMD_UPLOAD_FRIENDS);
							it.putExtra("type", 1);//Single Friend
							it.putExtra("serverType", 0);//Remove
							it.putExtra("idxlist",idx+"");
							sendBroadcast(it);
							
							//tml*** multi suvei
							int maxSuvei = Global.MAX_SUVS;
							for (int j = 0; j < maxSuvei; j++) {
								String addr = mPref.read("Suvei" + j);
								if (addr.equals(address)) {
									mPref.delect("Suvei" + j);
									if (AireJupiter.getInstance() != null
											&& AireJupiter.getInstance().tcpSocket.isLogged(false)) {
										AireJupiter.getInstance().tcpSocket
										.send(address, "REVOKE my GUARD access FINAL", 0, null, null, 0, null);
									}
									break;
								}
							}
							//***tml
						}
						
						ComposeActivity.deleteUserInList(idx);
					}
					else{
						//The user is not in user list
						mRDB.deleteContactByAddress(address);//remove user
					}
					
					try{
						SmsDB smsDB=new SmsDB(UsersActivity.this);
						smsDB.open();
						smsDB.deleteThreadByAddress(address);
						smsDB.close();
					}catch(Exception e){}
				}
				
				try{
					WTHistoryDB wtDB=new WTHistoryDB(UsersActivity.this);
					wtDB.open();
					wtDB.delete(address);
					wtDB.close();
				}catch(Exception e){}
				
				Vibrator mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				long[] patern = { 0, 40, 1000 };
				mVibrator.vibrate(patern, -1);
				
				needRefresh=true;
				onFafaUserQuery();
				friendAdapter[displayType].notifyDataSetChanged();
			}
		};

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			Map<String, String> map=null;
			try{
				map = amperList[type].get(position);
			}catch(Exception e){
				if (convertView!=null) return convertView;
				ViewHolder holder = new ViewHolder();
				if (type==0)
					convertView = View.inflate(icontext, R.layout.userinfo_cell, null);
				else
					convertView = View.inflate(icontext, R.layout.userinfo_cell_2, null);
				convertView.setTag(holder);
				return convertView;
			}
			
			String imagePath = map.get("imagePath");
			
			ViewHolder holder;

			int seperator = Integer.parseInt(map.get("seperator"));
			
			if (convertView == null) {
				holder = new ViewHolder();
				if (type==0)
					convertView = View.inflate(icontext, R.layout.userinfo_cell, null);
				else
					convertView = View.inflate(icontext, R.layout.userinfo_cell_2, null);
				
				holder.photoimage = (ImageView) convertView.findViewById(R.id.photo);
				holder.friendName = (TextView) convertView.findViewById(R.id.friendname);
				holder.separator = (TextView) convertView.findViewById(R.id.separator);
				holder.delete = (ImageView) convertView.findViewById(R.id.delete);
				holder.securityiam = (ImageView) convertView.findViewById(R.id.security_iam);  //tml*** beta ui, security
				holder.mask = (TextView) convertView.findViewById(R.id.mask);
				if (type==1)
				{
					holder.online = (ImageView) convertView.findViewById(R.id.online);
					holder.mood = (TextView) convertView.findViewById(R.id.mood);
				}
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.delete.setOnClickListener(blockCandidate);
			holder.delete.setTag(""+position);
			
			holder.mask.setOnClickListener(deleteBlockedUser);
			holder.mask.setTag(""+position);
			
			holder.photoimage.setTag(imagePath);
			String address=map.get("address");
			
			Drawable cachedImage = asyncImageLoader.loadDrawable(imagePath, new ImageCallback() {				
				public void imageLoaded(Drawable imageDrawable, String path) {
					ImageView imageViewByTag=null;
					if (type==0)
						imageViewByTag = (ImageView) friendGrid.findViewWithTag(path);
					else
						imageViewByTag = (ImageView) friendList.findViewWithTag(path);
					if (imageViewByTag != null && imageDrawable!=null) {
						imageViewByTag.setImageDrawable(imageDrawable);
					}
				}
			});
			
			if (cachedImage != null && imagePath!=null)
				holder.photoimage.setImageDrawable(cachedImage);
			else{
				if (address.startsWith("[<GROUP>]"))
					holder.photoimage.setImageResource(R.drawable.group_empty);
				else
					holder.photoimage.setImageResource(R.drawable.bighead);
			}
			
			boolean dummy = false;
			String disname = map.get("displayName");
			
			if (type==0)
			{
				if (seperator==0) 
					holder.separator.setVisibility(View.GONE);
				else
					holder.separator.setVisibility(View.VISIBLE);
			}
			else
				holder.separator.setVisibility(View.GONE);
			
			if (disname.equals("-"))
			{
				if (type==0)
					holder.separator.setVisibility(View.GONE);
				else
					holder.separator.setVisibility(View.VISIBLE);
				holder.photoimage.setVisibility(View.GONE);
				holder.friendName.setVisibility(View.GONE);
				holder.delete.setVisibility(View.GONE);
			}
			else if (disname.equals("----"))
			{
				if (type==0)
				{
					holder.photoimage.setVisibility(View.INVISIBLE);
					holder.friendName.setVisibility(View.INVISIBLE);
					holder.delete.setVisibility(View.INVISIBLE);
				}
				else
				{
					holder.photoimage.setVisibility(View.GONE);
					holder.friendName.setVisibility(View.GONE);
					holder.delete.setVisibility(View.GONE);
				}
				convertView.setClickable(true);
				dummy=true;
			}
			else
			{
				holder.friendName.setText(disname);
				holder.photoimage.setVisibility(View.VISIBLE);
				holder.friendName.setVisibility(View.VISIBLE);
				convertView.setClickable(false);
				dummy=false;
				holder.delete.setVisibility((editing>0)?View.VISIBLE:View.INVISIBLE);
//				if (position == topItem) {
//					holder.friendName.setTextColor(Color.BLUE);
//				} else {
//					holder.friendName.setTextColor(Color.BLACK);
//				}
			}
			
			//tml*** beta ui, security
			int maxSuvei = Global.MAX_SUVS;
			boolean securityman = false;
			for (int i = 0; i < maxSuvei; i++) {
				String thisaddress;
				if ((thisaddress = mPref.read("Suvei" + i)) != null) {
					if (thisaddress.equals(address)) {
						//tml*** suv onoff alert
						String addr = mPref.read("SuveiON" + i);
						boolean suvON = addr.equals(address);
//						Log.i("securityiam=" + address + " on:" + suvON);
						if (suvON) {
							holder.securityiam.setImageResource(R.drawable.security_mark);
							if (orientation == 1) {
								if (largeScreen) {
									holder.securityiam.setPadding(0, 0,
											(int) (15 * mDensity), (int) (15 * mDensity));
								} else {
									holder.securityiam.setPadding(0, 0,
											(int) (10 * mDensity), (int) (10 * mDensity));
								}
							} else if (orientation == 2) {  //landscape
								if (largeScreen) {
									if (displayType == 0) {
										holder.securityiam.setPadding((int) (25 * mDensity), 0,
												(int) (0 * mDensity), (int) (0 * mDensity));
									} else {
										holder.securityiam.setPadding(0, 0,
												(int) (10 * mDensity), (int) (10 * mDensity));
									}
								} else {
									if (displayType == 0) {
										holder.securityiam.setPadding((int) (25 * mDensity), 0,
												(int) (0 * mDensity), (int) (0 * mDensity));
									} else {
										holder.securityiam.setPadding(0, 0,
												(int) (5 * mDensity), (int) (5 * mDensity));
									}
								}
							}
						} else {
							holder.securityiam.setImageResource(R.drawable.security_mark2);
							if (orientation == 1) {
								if (largeScreen) {
									holder.securityiam.setPadding((int) (5 * mDensity), 0,
											(int) (25 * mDensity), (int) (25 * mDensity));
								} else {
									holder.securityiam.setPadding((int) (5 * mDensity), 0,
											(int) (20 * mDensity), (int) (20 * mDensity));
								}
							} else if (orientation == 2) {  //landscape
								if (largeScreen) {
									if (displayType == 0) {
										holder.securityiam.setPadding((int) (30 * mDensity), 0,
												(int) (10 * mDensity), (int) (10 * mDensity));
									} else {
										holder.securityiam.setPadding((int) (5 * mDensity), 0,
												(int) (20 * mDensity), (int) (20 * mDensity));
									}
								} else {
									if (displayType == 0) {
										holder.securityiam.setPadding((int) (30 * mDensity), 0,
												(int) (5 * mDensity), (int) (5 * mDensity));
									} else {
										holder.securityiam.setPadding((int) (5 * mDensity), 0,
												(int) (15 * mDensity), (int) (15 * mDensity));
									}
								}
							}
						}
						
						holder.securityiam.setVisibility(View.VISIBLE);
						securityman = true;
						break;
					}
				}
			}
			if (!securityman) {
//				Log.i("tml !securityiam");
				holder.securityiam.setVisibility(View.GONE);
			}
			//***tml
			
			if (editing==1)
			{
				holder.delete.setImageResource(R.drawable.delete);
				holder.mask.setText(R.string.delete_confirm);
			}else if (editing==2){
				holder.delete.setImageResource(R.drawable.block_big);
				holder.mask.setText(R.string.put_in_blacklist);
			}
			
			String b = map.get("blocked");
			if (editing==0)
				holder.mask.setVisibility(View.GONE);
			else if (b.equals("1") && !dummy)
			{
				holder.mask.setVisibility(View.VISIBLE);
			}else
				holder.mask.setVisibility(View.GONE);
			
			if (type==1)
			{
				int actual=Integer.parseInt(map.get("actual"));
				if (actual==1)
				{
					if (address.startsWith("[<GROUP>]"))
					{
						holder.online.setVisibility(View.INVISIBLE);
						holder.mood.setText(R.string.the_group);
					}
					else{
						holder.online.setVisibility(View.VISIBLE);
						int status=ContactsOnline.getContactOnlineStatus(address);
						if (status>0)
							holder.online.setImageResource(R.drawable.online);
						else
							holder.online.setImageResource(R.drawable.offline);
						String mood=mADB.getMoodByAddress(address);
						holder.mood.setText(mood);
					}
					convertView.setBackgroundResource(R.drawable.null_draw);
				}else{
					holder.online.setVisibility(View.INVISIBLE);
					holder.mood.setText("");
					convertView.setBackgroundResource(R.drawable.lightblue_draw);
				}
				
				if (address.startsWith("[<GROUP>]")) {
					holder.photoimage.setBackgroundResource(R.drawable.group_bg);
				} else {
					holder.photoimage.setBackgroundResource(R.drawable.empty);
				}
			}
			else if (type==0)
			{
				int actual=Integer.parseInt(map.get("actual"));
				if (actual==1)
				{
					if (address.startsWith("[<GROUP>]"))
					{
						holder.photoimage.setBackgroundResource(R.drawable.group_bg);
					}
					else{
						holder.photoimage.setBackgroundResource(R.drawable.empty);
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
					}
					convertView.setBackgroundResource(R.drawable.null_draw);
				}else{
					
					if (address.startsWith("[<GROUP>]")) {
						holder.photoimage.setBackgroundResource(R.drawable.group_bg);
					} else {
						holder.photoimage.setBackgroundResource(R.drawable.empty);
					}
					
					if (address.equals("-"))
						convertView.setBackgroundResource(R.drawable.null_draw);
					else
						convertView.setBackgroundResource(R.drawable.lightblue_draw);
				}
			}
			
			return convertView;
		}
	}
	
	@Override
	public void onPause() {
		uiUAinFore = false;  //tml*** conn notify/
//		MobclickAgent.onPause(this);
		System.gc();
		System.gc();
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		uiUAinFore = true;  //tml*** conn notify/
		//tml*** return Dialer view
		if ((AireVenus.instance() != null) && (AireVenus.callstate_AV != null)) {
			Log.e("resume-check callstate " + AireVenus.callstate_AV);
			if (AireVenus.callstate_AV.equals(VoipCall.State.Connected.toString())
					|| AireVenus.callstate_AV.equals(VoipCall.State.IncomingReceived.toString())
					|| AireVenus.callstate_AV.equals(VoipCall.State.StreamsRunning.toString())) {
				Log.e("attempt return dialer ...");
				Intent reconn = new Intent();
				VoipCore lVoipCore = AireVenus.instance().getVoipCore();
				boolean isVideoCall = lVoipCore.getVideoEnabled();
				if (DialerActivity.getDialer() == null) {
					Log.e("dialer never created, resume creation");
					String IncomingNumber = lVoipCore.getRemoteAddress().getUserName();
					reconn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					reconn.setClass(this, DialerActivity.class);
					reconn.putExtra("incomingCall", true);
					reconn.putExtra("PhoneNumber", IncomingNumber);
					reconn.putExtra("VideoCall", isVideoCall);
					startActivity(reconn);
				} else {
					Log.e("dialer resume");
					reconn.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					if (isVideoCall) {
						reconn.setClass(this, VideoCallActivity.class);
						reconn.putExtra("restart", true);
					} else {
						reconn.setClass(this, DialerActivity.class);
					}
					startActivity(reconn);
				}
			}
		}
		//***tml
		if (sortMethod > 0) {
			if (forceRefresh) {
				needRefresh = true;
				onFafaUserQuery();
				friendAdapter[displayType].notifyDataSetChanged();
			}
		}
		mMoreBtn.setImageResource(R.drawable.dropdown);  //tml*** beta ui, was dropdown
		mMoreBtn.setBackgroundResource(R.drawable.optionbtn);
		int mMoreBtnPad = 0;
		if (largeScreen) {
			mMoreBtnPad = (int) (15 * mDensity);
		} else {
			mMoreBtnPad = (int) (10 * mDensity);
		}
		mMoreBtn.setPadding(mMoreBtnPad, mMoreBtnPad, mMoreBtnPad, mMoreBtnPad);
//		MobclickAgent.onResume(this);
	}
	
	private long onDown = 0, onDownNow = 0;
	private boolean onDowned = false;
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
		int action = event.getAction();
//		int source = event.getSource();
//		int hwscan = event.getScanCode();
//		int devid = event.getDeviceId();
//		int label = event.getDisplayLabel();
//		Log.e("test onKeyDown Action=" + action + " KeyCode=" + keycode + " Source=" + source + " HWScan=" + hwscan + " DevID=" + devid + " Label=" + label);
		//tml*** hw-menu substitute
		onDownNow = System.currentTimeMillis();
		if (action == MotionEvent.ACTION_DOWN && !onDowned) {
			onDowned = true;
			mHandler.postDelayed(onDownReset, 1300);
			onDown = onDownNow;
		} else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			onDowned = false;
			mHandler.removeCallbacks(onDownReset);
		}
		
		if ((onDownNow - onDown) > 1000 
				&& action == MotionEvent.ACTION_DOWN && keycode == KeyEvent.KEYCODE_MENU
				&& onDowned) {
			onDowned = false;
			Log.e("substitute/override open options menu");
			openOptionsMenu();
			return true;
		}
	    return super.onKeyDown(keycode, event);
	}
	
	@Override
	public void onBackPressed()
	{
		if (editing > 0) {  //tml*** backpress fix
			editing = 0;
			friendAdapter[displayType].notifyDataSetChanged();
			return;
		} else {
			super.onBackPressed();
		}
	}
	//tml*** hw-menu substitute
	Runnable onDownReset = new Runnable() {
		@Override
		public void run() {
			onDowned = false;
		}
	};
	
//	@Override
//	public boolean onKeyLongPress(int keycode, KeyEvent event) {
//		int action = event.getAction();
//		int source = event.getSource();
//		int hwscan = event.getScanCode();
//		int devid = event.getDeviceId();
//		int label = event.getDisplayLabel();
//		Log.e("test onKeyLongPress Action=" + action + " KeyCode=" + keycode + " Source=" + source + " HWScan=" + hwscan + " DevID=" + devid + " Label=" + label);
//	    return super.onKeyLongPress(keycode, event);
//	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_sort, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean dosort = true;
		switch (item.getItemId()) {
		case R.id.sortmode:  //tml*** temp alpha ui
			if (displayType==0)	{
				displayType=1;
				friendGrid.setVisibility(View.GONE);
				friendList.setVisibility(View.VISIBLE);
				mModeBtn.setImageResource(R.drawable.mode_grid);
			} else {
				displayType=0;
				friendGrid.setVisibility(View.VISIBLE);
				friendList.setVisibility(View.GONE);
				mModeBtn.setImageResource(R.drawable.mode_list);
			}
			mPref.write("displayType",displayType);
			friendAdapter[displayType].notifyDataSetInvalidated();
			
			if (mDropDownList.getVisibility() == View.VISIBLE) {
				mDropDownList.setVisibility(View.GONE);
			}
			dosort = false;
			break;
		case R.id.sortbyname:
			if (sortMethod==0) return false;
			mPref.write("SortMethod", sortMethod=0);
			break;
		case R.id.sortbytime:
			if (sortMethod==1) return false;
			mPref.write("SortMethod", sortMethod=1);
			break;
		case R.id.sortbystatus:
			if (sortMethod==2) return false;
			mPref.write("SortMethod", sortMethod=2);
			break;
		case R.id.gosettings:  //tml*** temp alpha ui
			startActivity(new Intent(UsersActivity.this, SettingActivity.class));
			dosort = false;
			finish();
			break;
		case R.id.gosearch:  //tml*** search add
            InputMethodManager imm = (InputMethodManager) ((EditText) findViewById(R.id.searchkeyword)).getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(((EditText) findViewById(R.id.searchkeyword)).getWindowToken(), 0);
            }
			if (((EditText) findViewById(R.id.searchkeyword)).getVisibility() == View.VISIBLE) {
				((EditText) findViewById(R.id.searchkeyword)).setText("");
				onFilterUserQuery("");
				((EditText) findViewById(R.id.searchkeyword)).setVisibility(View.GONE);
				((ImageView) findViewById(R.id.clearkeyword)).setVisibility(View.GONE);
			} else {
				if (editing > 0) {
					editing = 0;
					Map<String, String> map;
					for (int i=0; i < amperList[displayType].size(); i++) {
						map = amperList[displayType].get(i);
						map.put("blocked", "0");
					}
					friendAdapter[displayType].notifyDataSetChanged();
					mMoreBtn.setImageResource(R.drawable.add2);
					mMoreBtn.setBackgroundResource(R.drawable.optionbtn);
					int mMoreBtnPad = 0;
					if (largeScreen) {
						mMoreBtnPad = (int) (15 * mDensity);
					} else {
						mMoreBtnPad = (int) (10 * mDensity);
					}
					mMoreBtn.setPadding(mMoreBtnPad, mMoreBtnPad, mMoreBtnPad, mMoreBtnPad);
				}
				mDropDownList.setVisibility(View.GONE);
				((EditText) findViewById(R.id.searchkeyword)).setVisibility(View.VISIBLE);
				((ImageView) findViewById(R.id.clearkeyword)).setVisibility(View.VISIBLE);
				((EditText) findViewById(R.id.searchkeyword)).requestFocus();
			}
			break;
		}
		if (dosort) {  //tml*** temp alpha ui
			needRefresh=true;
			onFafaUserQuery();
			friendAdapter[displayType].notifyDataSetChanged();
		}
		if (mDropDownList.getVisibility() == View.VISIBLE) {  //tml*** longclick edit
			mDropDownList.setVisibility(View.GONE);
		}
		return true;
	}
	
	@Override
	protected void onDestroy() {
		uiUAinFore = false;  //tml*** conn notify/
		mHandler.removeCallbacks(mInstantQueryOnlineFriends);
		unregisterReceiver(handleFreshItems);
		if (mADB != null && mADB.isOpen())
			mADB.close();
		if (mRDB != null && mRDB.isOpen())
			mRDB.close();
		if (mGDB != null && mGDB.isOpen())
			mGDB.close();
		
		if (TimeLineAdapter.asyncImageLoader!=null)
			TimeLineAdapter.asyncImageLoader.release();
		TimeLineAdapter.asyncImageLoader=null;
		System.gc();
		System.gc();
		
		mPref.writeLong("last_show_time",new Date().getTime());
		super.onDestroy();
	}
	
	private synchronized void onFafaUserQuery() {
		
		Log.d("onFafaUserQuery");
		
		if (!needRefresh && !forceRefresh)
		{
			Log.d("onFafaUserQuery no need to refresh");
			return;
		}
		
		if (amperList!=null)
		{
			amperList[0].clear();
			amperList[1].clear();
		}
		if (orgList != null) {  //tml*** search add
			orgList.clear();
		}
		
		HashMap<String, String> map;
//		Cursor [] cursor=(Cursor[])new Cursor[2];
		Cursor [] cursor=(Cursor[])new Cursor[1];  //tml*** new friends
		if (sortMethod==0)
			cursor[0] = mADB.fetchAll();
		else if (sortMethod==1||sortMethod==2)
			cursor[0] = mADB.fetchAllByTime();
//		cursor[1] = mRDB.fetchAll();  //tml*** new friends, remove
		
		try{
			int unknowns = 0;
			int start=1;
//			int numRF=cursor[1].getCount();
			int numRF = 0;  //tml*** new friends
			
			for (int loop=0;loop<2;loop++)
			{
				if (loop==0)
					numTrueFriends=cursor[loop].getCount();
				
				if (!cursor[loop].moveToFirst())
					continue;
				
				do {
					String address = cursor[loop].getString(1);
					if (address.startsWith("Done=")) {  //tml*** getuserinfo temp fix
						mADB.deleteContactByAddress(address);
						continue;
					}
//					if (address.startsWith("[<CONF>][<GROUP>]")) continue;
					int idX=cursor[loop].getInt(3);
					long contactId = cq.getContactIdByNumber(address);
					String disName="";
					String userphotoPath;

					userphotoPath = Global.SdcardPath_inbox + "photo_" + idX + "b.jpg";
					if (!new File(userphotoPath).exists())
					{
						userphotoPath = Global.SdcardPath_inbox + "photo_" + idX + ".jpg";
						if (!new File(userphotoPath).exists())
							userphotoPath=null;
					}
					
					if (contactId>0)
						disName = cq.getNameByContactId(contactId);
					else
						disName = cursor[loop].getString(4);
					
					if (disName==null || disName.length()==0) {
						disName=getString(R.string.unknown_person);
						unknowns++;  //tml|james*** unknown contacts error/
					}
					
					map = new HashMap<String, String>();
					
					/*
					String containChinese = "no";
					
					String [] spell = new String[1];
					if (isChineseChar(disName, spell))
					{
						containChinese = "yes";
						map.put("change", spell[0]);
					}
					
					map.put("containChinese",containChinese);
					*/
					map.put("displayName", disName);
					map.put("address", address);
					map.put("idx", idX+"");
					map.put("imagePath", userphotoPath);
					map.put("contactId", contactId + "");
					map.put("seperator", "0");
					map.put("blocked", "0");
					
					if (loop==0)
						map.put("actual", "1");
					else
						map.put("actual", "0");
					
					if (sortMethod==2)
					{
						int status=ContactsOnline.getContactOnlineStatus(address);
						if (status>0) {
							amperList[0].add(0, map);
							orgList.add(0, map);  //tml*** search add
						} else {
							amperList[0].add(map);
							orgList.add(map);  //tml*** search add
						}
					} else {
						amperList[0].add(map);
						orgList.add(map);  //tml*** search add
					}
				}while (cursor[loop].moveToNext() && amperList[0].size() <= MAX_USERS);
				
				if (loop==0)
				{
					//SortList.sortMapList(amperList[0]);
	
					for (int i=0;i<amperList[0].size();i++)
						amperList[1].add(amperList[0].get(i));
					
					if (numRF>0)
					{
						int count=amperList[0].size()%numColumns;
						if (count!=0)
						{
							for(int i=0;i<numColumns-count;i++)
								addDummyMap(amperList[0],"----");
						}
					}
				}else{
					for (int i=start;i<amperList[0].size();i++)
						amperList[1].add(amperList[0].get(i));
				}
				
				if (loop==0 && numRF>0)
				{
					if (amperList[0].size()>0)
						for(int i=0;i<numColumns;i++)
						{
							map = (HashMap<String, String>)amperList[0].get(amperList[0].size()-1-i);
							map.put("seperator", "1");
						}
				}
				
				start=amperList[0].size();
			
				if (loop==0 && numRF>0)//ListView Separator
					addDummyMap(amperList[1],"-");
				
				//tml|james*** unknown contacts error
				int unknownBreak = ((numTrueFriends - 2) / 4);
				if (unknownBreak < 1) unknownBreak = 1;
				Log.d("check !@#$unknownsF " + unknowns + " >? " + unknownBreak + "/" + (numTrueFriends - 1));
				if (unknowns > unknownBreak) {
					Intent intent = new Intent(Global.Action_InternalCMD);
					intent.putExtra("unknowns", true);
					intent.putExtra("Command", Global.CMD_DOWNLOAD_FRIENDS);
					sendBroadcast(intent);
					unknowns = 0;
				}
				//***tml
			}
		
		}catch(Exception e){}
		
		if(cursor[0]!=null && !cursor[0].isClosed())
			cursor[0].close();

//		if(cursor[1]!=null && !cursor[1].isClosed())  //tml*** new friends, remove
//			cursor[1].close();
		
		needRefresh=false;
		forceRefresh=false;
	}
	
	void addDummyMap(List<Map<String,String>> list, String displayName)
	{
		HashMap<String, String> map;
		map = new HashMap<String, String>();
		//map.put("containChinese","no");
		map.put("displayName", displayName);
		map.put("address", "-");
		map.put("imagePath", null);
		map.put("contactId", "-20");
		map.put("seperator", "1");
		map.put("actual", "0");
		map.put("blocked", "0");
		list.add(map);
	}
	//tml*** search add
	private void onFilterUserQuery(String keyword) {
		if (amperList != null) {
			amperList[displayType].clear();
		}
		
		for (Map<String, String> map: orgList) {
			if (keyword.length() == 0
					|| ((String) map.get("displayName")).toLowerCase()
							.contains(keyword.toLowerCase())) {
				amperList[displayType].add(map);
			}
		}
		
		friendAdapter[displayType].notifyDataSetChanged();
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
//			restartMain(this, 500);
		}
	}
	public void restartMain(Context context, int delay) {
	    if (delay == 0) {
	        delay = 1;
	    }
	    Log.e("MAIN restarting app");
	    Intent restartIntent = new Intent(context, UsersActivity.class);
	    PendingIntent intent = PendingIntent.getActivity(context, 0,
	    		restartIntent, PendingIntent.FLAG_CANCEL_CURRENT  | Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	    manager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, intent);
	    System.exit(2);
	}
	//***tml
}
