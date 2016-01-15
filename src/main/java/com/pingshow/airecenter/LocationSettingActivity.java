package com.pingshow.airecenter;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.map.MapViewLocation;
import com.pingshow.beehive.BeeHiveService;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;

public class LocationSettingActivity extends Activity implements
		OnClickListener {

	private UserItemAdapter gridAdapter;
	private List<Map<String, String>> amperList;
	private List<Map<String, String>> orgList;
	private AsyncImageLoader asyncImageLoader;
	private int numColumns = 3;
	private GridView resultGridView;
	private AmpUserDB mADB;
	private ContactsQuery cq;
	private float mDensity = 1.0f;
	private MyPreference mPref;
	private LinearLayout authorizedList;
	private ArrayList<String> shared_friends;
	private TextView tv_direction_map;
	private boolean locationEnabled;
	
	public static LocationSettingActivity _this;


	Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			Toast.makeText(LocationSettingActivity.this, msg.arg1,
					Toast.LENGTH_LONG).show();
		};
	};
	private String myPhoneNumber;
	private String myNickName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.location_setting);

        neverSayNeverDie(this);  //tml|bj*** neverdie/

		if (getIntent().getBooleanExtra("launchFromSelf", false))
			this.overridePendingTransition(R.anim.freeze, R.anim.freeze);

		tv_direction_map = (TextView) findViewById(R.id.direction_map);
		tv_direction_map.setOnClickListener(this);
		mPref = new MyPreference(this);
		shared_friends = (ArrayList<String>) mPref.readArray("shared_friends");
		if (shared_friends == null) {
			shared_friends = new ArrayList<String>();
		}
		myPhoneNumber = mPref.read("myPhoneNumber", "++++");
		mDensity = getResources().getDisplayMetrics().density;

		mADB = new AmpUserDB(this);
		mADB.open();
		myNickName = mADB.getNicknameByAddress(myPhoneNumber);
		cq = new ContactsQuery(LocationSettingActivity.this);

		amperList = new ArrayList<Map<String, String>>();
		orgList = new ArrayList<Map<String, String>>();
		
		gridAdapter = new UserItemAdapter(LocationSettingActivity.this);

		resultGridView = (GridView) findViewById(R.id.friendsGridView);
		resultGridView.setNumColumns(numColumns);

		resultGridView.setAdapter(gridAdapter);
		resultGridView.setOnItemClickListener(onChooseUser);

		mHandler.post(mFetchFriends);
		
		((EditText)findViewById(R.id.keyword)).addTextChangedListener (new TextWatcher() {
	        @Override
	        public void afterTextChanged (Editable s) {
	        	onFilterUserQuery(s.toString());
	        	
	        	if (s.toString().length()==0)
	        	{
	        		((ImageView)findViewById(R.id.clear)).setVisibility(View.GONE);
	        		((EditText)findViewById(R.id.keyword)).setPadding((int)(16.f*mDensity), (int)(6.f*mDensity),(int)(16.f*mDensity), (int)(6.f*mDensity));
	        	}
	        	else{
	        		((ImageView)findViewById(R.id.clear)).setVisibility(View.VISIBLE);
	        		((EditText)findViewById(R.id.keyword)).setPadding((int)(56.f*mDensity), (int)(6.f*mDensity),(int)(16.f*mDensity), (int)(6.f*mDensity));
	        	}
	        }

	        @Override
	        public void onTextChanged (CharSequence s, int start, int before, int count) {
	        }

	        @Override
	        public void beforeTextChanged (CharSequence s, int start, int count, int after) {
	        }
	    });
		
		((ImageView)findViewById(R.id.clear)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((EditText)findViewById(R.id.keyword)).setText("");
				onFilterUserQuery("");
			}
		});

		((Button) findViewById(R.id.add))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						FrameLayout contactsList=((FrameLayout) findViewById(R.id.contacts));
						if (contactsList.getVisibility()!=View.VISIBLE)
							contactsList.setVisibility(View.VISIBLE);
						else
							contactsList.setVisibility(View.INVISIBLE);
					}
				});

		locationEnabled = mPref.readBoolean("locationEnabled", true);
		((ImageButton) findViewById(R.id.location))
				.setImageResource(locationEnabled ? R.drawable.slider_on
						: R.drawable.slider_off);
		((ImageButton) findViewById(R.id.location))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						locationEnabled = !locationEnabled;
						((ImageButton) arg0)
								.setImageResource(locationEnabled ? R.drawable.slider_on
										: R.drawable.slider_off);
						mPref.write("locationEnabled", locationEnabled);
					}
				});

		authorizedList = ((LinearLayout) findViewById(R.id.family));

		DialerFrame.setFrame(this, findViewById(android.R.id.content));

		IntentFilter intentToReceiveFilter = new IntentFilter();
		intentToReceiveFilter.addAction(Global.Action_Refresh_LOCATIONTIMER);
		LocationSettingActivity.this.registerReceiver(handleFreshItems, intentToReceiveFilter);
		
		DialerFrame.setFrame(this, findViewById(android.R.id.content));
		
		// //// SideBar

		((ImageView) findViewById(R.id.bar1))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent it = new Intent(LocationSettingActivity.this,
								MainActivity.class);
						it.putExtra("launchFromSelf", true);
						startActivity(it);
						finish();
					}
				});
		((ImageView) findViewById(R.id.bar6))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent it = new Intent(LocationSettingActivity.this,
								ShoppingActivity.class);
						it.putExtra("launchFromSelf", true);
						startActivity(it);
						finish();
					}
				});
		((ImageView) findViewById(R.id.bar7))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent it = new Intent(LocationSettingActivity.this,
								SecurityNewActivity.class);
						it.putExtra("launchFromSelf", true);
						startActivity(it);
						finish();
					}
				});
		((ImageView) findViewById(R.id.bar8))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
					}
				});

		mHandler.post(arrange);
		
		_this=this;
	}

	Runnable arrange = new Runnable() {
		public void run() {
			authorizedList.removeAllViews();
			if (shared_friends == null) {
				TextView v = new TextView(LocationSettingActivity.this);
				v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
				v.setTextColor(0xff567d98);
				v.setGravity(Gravity.CENTER_HORIZONTAL);
				v.setPadding(0, (int) (30.f * mDensity), 0, 0);
				v.setText("No instant contact assigned.");
				authorizedList.addView(v);
				return;
			} else {
				for (int i = 0; i < shared_friends.size(); i++) {
					String address = shared_friends.get(i);
					LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					final View v = inflater.inflate(R.layout.authorize_cell2,
							null, false);
					authorizedList.addView(v);

					String global = MyTelephony.attachPrefix(
							LocationSettingActivity.this, address);

					int idx = mADB.getIdxByAddress(global);
					ImageView iv = (ImageView) v.findViewById(R.id.photo);
					if (idx > 0 && iv != null) {
						Drawable photo = ImageUtil.getUserPhoto(
								LocationSettingActivity.this, idx);
						if (photo != null)
							iv.setImageDrawable(photo);
						else
							iv.setImageResource(R.drawable.bighead);
					}

					((TextView) v.findViewById(R.id.ticks)).setTag(global);

					String displayname = mADB.getNicknameByAddress(address);
					((TextView) v.findViewById(R.id.displayname))
							.setText(displayname);

					long endTime = mPref.readLong(address + "", 0);
					long currentTime = new Date().getTime() / 1000;
					Log.d("endTime===" + endTime + ";******currentTime==="
							+ currentTime);
					if (currentTime < endTime) {
						CountDownTimer timer = new CountDownTimer(
								(endTime - currentTime) * 1000, 1000) {
							@Override
							public void onTick(long millisUntilFinished) {

								((TextView) v.findViewById(R.id.ticks))
										.setText(DateFormat.format("mm:ss",
												millisUntilFinished));
								// Log.d("CountDown" + millisUntilFinished +
								// "");
							}

							@Override
							public void onFinish() {

								String address = (String) v.findViewById(
										R.id.ticks).getTag();
								for (String add : shared_friends) {
									if (address.equals(add)) {
										shared_friends.remove(add);
										break;
									}
								}
								sendeeAddress = address;
								mPref.writeArray("shared_friends",
										shared_friends);
								Intent intent = new Intent(Global.Action_Refresh_LOCATIONTIMER);
								sendBroadcast(intent);

							}
						};
						timer.start();
					}else {
						String addr= (String) v.findViewById(
								R.id.ticks).getTag();
						for (String add : shared_friends) {
							if (addr.equals(add)) {
								shared_friends.remove(add);
								break;
							}
						}
						mPref.writeArray("shared_friends",
								shared_friends);
						Intent intent = new Intent(Global.Action_Refresh_LOCATIONTIMER);
						sendBroadcast(intent);
					}
				}
			}
		}
	};

	OnItemClickListener onChooseUser = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {

			Map<String, String> map = amperList.get(position);
			String address = map.get("address");
			String global = MyTelephony.attachPrefix(
					LocationSettingActivity.this, address);
			boolean found = false;
			for (String add : shared_friends) {
				if (global.equals(add)) {
					found = true;
					break;
				}
			}
			if (!found) {
				Intent it = new Intent(LocationSettingActivity.this,
						CommonDialog.class);
				it.putExtra("msgContent",
						getString(R.string.request_location_sharing));
				it.putExtra("numItems", 2);
				it.putExtra("ItemCaption0", getString(R.string.cancel));
				it.putExtra("ItemResult0", RESULT_CANCELED);
				it.putExtra("ItemCaption1", getString(R.string.yes));
				it.putExtra("ItemResult1", RESULT_OK);
				startActivityForResult(it, 7);
				sendeeAddress = global;
			}
			
			((FrameLayout)findViewById(R.id.contacts)).setVisibility(View.GONE);
		}
	};
	
	private void onFilterUserQuery(String keyword) {
		
		if (amperList!=null)
		{
			amperList.clear();
		}
		
		for (Map<String, String> map: orgList)
		{
			if (keyword.length()==0 || ((String)map.get("displayName")).toLowerCase().contains(keyword.toLowerCase()))
			{
				amperList.add(map);
			}
		}
		
		gridAdapter.notifyDataSetChanged();
	}

	String sendeeAddress = "";

	Runnable mFetchFriends = new Runnable() {
		public void run() {
			amperList.clear();
			orgList.clear();
			Cursor c = mADB.fetchAllByTime();
			if (c != null && c.moveToFirst()) {
				do {
					String address = c.getString(1);
					if (address.startsWith("[<GROUP>]"))
						continue;
					int idx = c.getInt(3);
					if (idx < 50)
						continue;

					long contactId = cq.getContactIdByNumber(address);
					String disName = "";
					String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + "b.jpg";
					File f = new File(userphotoPath);
					if (!f.exists())
						userphotoPath = null;

					if (contactId > 0)
						disName = cq.getNameByContactId(contactId);
					else
						disName = c.getString(4);

					if (disName == null || disName.length() == 0)
						disName = String.valueOf((R.string.unknown_person));

					HashMap<String, String> map = new HashMap<String, String>();

					map.put("displayName", disName);
					map.put("address", address);
					map.put("idx", idx + "");
					map.put("checked", "0");
					map.put("imagePath", userphotoPath);

					amperList.add(map);
					orgList.add(map);
				} while (c.moveToNext());

				c.close();
			}

			mHandler.post(new Runnable() {
				public void run() {
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
			int count = amperList.size();
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

			Map<String, String> map = null;

			try {
				map = amperList.get(position);
			} catch (Exception e) {
				return convertView;
			}

			String imagePath = map.get("imagePath");

			foundViewHolder holder;

			if (convertView == null) {
				holder = new foundViewHolder();
				convertView = getLayoutInflater().inflate(R.layout.user_tiny_cell,
						null);

				holder.photoimage = (ImageView) convertView
						.findViewById(R.id.photo);
				holder.friendName = (TextView) convertView
						.findViewById(R.id.friendname);
				holder.checked = (ImageView) convertView
						.findViewById(R.id.checked);
				convertView.setTag(holder);
			} else {
				holder = (foundViewHolder) convertView.getTag();
			}

			holder.photoimage.setTag(imagePath);
			Drawable cachedImage = asyncImageLoader.loadDrawable(imagePath,
					new ImageCallback() {
						public void imageLoaded(Drawable imageDrawable,
								String path) {
							ImageView imageViewByTag = null;
							imageViewByTag = (ImageView) resultGridView
									.findViewWithTag(path);
							if (imageViewByTag != null && imageDrawable != null) {
								imageViewByTag.setImageDrawable(imageDrawable);
							}
						}
					});

			if (cachedImage != null && imagePath != null)
				holder.photoimage.setImageDrawable(cachedImage);
			else
				holder.photoimage.setImageResource(R.drawable.bighead);

			String disname = map.get("displayName");
			holder.friendName.setText(disname);

			String address = map.get("address");
			int status = ContactsOnline.getContactOnlineStatus(address);
			if (status > 0) {
				Drawable d = LocationSettingActivity.this.getResources().getDrawable(
						R.drawable.online_light);
				d.setBounds(0, 0, (int) (20.f * mDensity),
						(int) (20.f * mDensity));
				SpannableString spannable = new SpannableString("*" + disname);
				ImageSpan icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
				spannable.setSpan(icon, 0, 1,
						SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
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
	
	public FrameLayout getNotificationLayout()
	{
		return (FrameLayout)findViewById(R.id.notification);
	}

	@Override
	public void onDestroy() {
		LocationSettingActivity.this.unregisterReceiver(handleFreshItems);
		if (mADB != null && mADB.isOpen())
			mADB.close();
		amperList.clear();
		orgList.clear();
		System.gc();
		System.gc();
		_this=null;
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		DialerFrame.checkEmbeddedDialer(findViewById(android.R.id.content));
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.direction_map:
			if (!MyUtil.isISO_China(LocationSettingActivity.this, mPref, null) && MyUtil.hasGoogleMap(false, null))
			{
				Intent intent = new Intent(LocationSettingActivity.this, MapViewLocation.class);
				intent.putExtra("launchFromSelf", true);
				intent.putStringArrayListExtra("shared_friends", shared_friends);
				intent.putExtra("trackable", true);
				startActivity(intent);
			}
			else{
				Intent intent = new Intent(LocationSettingActivity.this, BDMapViewLocation.class);
				intent.putExtra("launchFromSelf", true);
				intent.putStringArrayListExtra("shared_friends", shared_friends);
				intent.putExtra("trackable", true);
				startActivity(intent);
			}
			
			mHandler.postDelayed(new Runnable(){
				public void run()
				{
					finish();
				}
			},1000);
			
			break;
		}
	}
	BroadcastReceiver handleFreshItems = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, final Intent intent) {
			Log.d("BroadcastReceiver location timer refresh run");
			if (intent.getAction().equals(Global.Action_Refresh_LOCATIONTIMER)) {
				shared_friends = (ArrayList<String>) mPref.readArray("shared_friends");
				Log.d("receiver::::shared_friends===="+shared_friends);
				mHandler.post(arrange);
			}
		}
	};
	private String mMsgText;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 7) {
			if (resultCode == RESULT_OK) {
				/*
				 * shared_friends.add(sendeeAddress);
				 * mPref.writeArray("shared_friends", shared_friends);
				 */
				mMsgText = "[<LOCATIONSHARING>]";
				int myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
				// String
				// global=MyTelephony.attachPrefix(LocationSettingActivity.this,
				// sendeeAddress);
				int idx = mADB.getIdxByAddress(sendeeAddress);
				//mPref.writeLong(idx + "", new Date().getTime() / 1000 + 3600);
				SendAgent agent = new SendAgent(this, myIdx, idx, true);
				agent.onSend(sendeeAddress, mMsgText, 0, "", "", false);
				mMsgText = "";
				//mHandler.post(arrange);
				// mHandler.postDelayed(authorizeByTCPMessage, 1500);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	public void close()
	{
		finish();
	}

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