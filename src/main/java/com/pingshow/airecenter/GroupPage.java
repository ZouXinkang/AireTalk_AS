package com.pingshow.airecenter;

import java.io.File;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.cons.Key;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.GroupDB;
import com.pingshow.network.MyNet;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.util.MyUtil;

public class GroupPage extends Page {

	private UserItemAdapter gridAdapter;
	private MyPreference mPref;
	private List<Map<String, String>> amperList;
	private ArrayList<String> chosenList = new ArrayList<String>();
	private AsyncImageLoader asyncImageLoader;
	private GridView userGridView;
	private AmpUserDB mADB;
	private ContactsQuery cq;
	private int mCount = 0;
	private float mDensity = 1.0f;
	private String groupName;
	private String photoPath;
	private boolean photoAssigned=false;

	private ProgressDialog progress;

	static public MainActivity _this;

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		}
	};

	private View layout;

	public GroupPage(View v) {
		Log.e("*** !!! GROUPPAGE *** START START !!! ***");
		layout = v;
		mPref = new MyPreference(MainActivity._this);

		mDensity = MainActivity._this.getResources().getDisplayMetrics().density;

		mADB = new AmpUserDB(MainActivity._this);
		mADB.open();

		cq = new ContactsQuery(MainActivity._this);

		amperList = new ArrayList<Map<String, String>>();

		gridAdapter = new UserItemAdapter(MainActivity._this);

		userGridView = (GridView) layout.findViewById(R.id.friends);

		userGridView.setAdapter(gridAdapter);
		userGridView.setOnItemClickListener(onChooseUser);

		((Button) layout.findViewById(R.id.done))
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						chosenList.clear();

						for (Map<String, String> map : amperList) {
							if (map.get("checked").equals("1")) {
								chosenList.add(map.get("idx"));
							}
						}

						if (chosenList == null || chosenList.size() == 0)
							return;

						groupName = ((EditText) layout.findViewById(R.id.name))
								.getText().toString();

						if (groupName != null) {
							groupName = groupName.trim();
							boolean chinese = groupName.toLowerCase().equals(groupName.toUpperCase());

							if (groupName.length() < (chinese ? 3 : 6)
									|| groupName.length() > 30) {
								Intent int2 = new Intent(MainActivity._this, CommonDialog.class);
								int2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
										| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
										| Intent.FLAG_ACTIVITY_SINGLE_TOP);
								int2.putExtra("msgContent", MainActivity._this.getString(R.string.nickname_invalid));
								int2.putExtra("numItems", 1);
								int2.putExtra("ItemCaption0", MainActivity._this.getString(R.string.done));
								int2.putExtra("ItemResult0", -1);
								MainActivity._this.startActivity(int2);
								return;
							}
						} else
							return;

						InputMethodManager imm = (InputMethodManager) MainActivity._this.getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(((EditText)layout.findViewById(R.id.name)).getWindowToken(), 0);

						Intent it = new Intent(MainActivity._this, GroupDialogActivity.class);
						it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						it.putExtra("chosenList", (Serializable) chosenList);
						it.putExtra("groupname", groupName);
						MainActivity._this.startActivityForResult(it, 100);
					}
				});
		((EditText) layout.findViewById(R.id.name)).requestFocus();  //tml*** prefocus
		mHandler.post(mFetchFriends);
		
		//tml|phoebe*** alpha ui
		((Button) layout.findViewById(R.id.done)).setEnabled(false);
		((Button) layout.findViewById(R.id.done)).setAlpha(70);
		((EditText) layout.findViewById(R.id.name)).addTextChangedListener (new TextWatcher() {
	        @Override
	        public void afterTextChanged (Editable s) {
	        	if (s.toString().length() == 0) {
	        		((Button) layout.findViewById(R.id.done)).setEnabled(false);
	        		((Button) layout.findViewById(R.id.done)).setAlpha(70);
	        	} else {
	        		((Button) layout.findViewById(R.id.done)).setEnabled(true);
	        		((Button) layout.findViewById(R.id.done)).setAlpha(255);
	        	}
	        }

	        @Override
	        public void onTextChanged (CharSequence s, int start, int before, int count) {}

	        @Override
	        public void beforeTextChanged (CharSequence s, int start, int count, int after) {}
	    });
		//***tml
		
		IntentFilter intentToReceiveFilter = new IntentFilter();
		intentToReceiveFilter.addAction(Global.Action_Create_Group);
		MainActivity._this.registerReceiver(handleRegisterGroup, intentToReceiveFilter);
		
		if(mPref.readBoolean(Key.MULTI_MEMBER_CONF, false)){
			maxGroupNum = 50;
		}
	}

	BroadcastReceiver handleRegisterGroup = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context,final Intent intent) {
			if (intent.getAction().equals(Global.Action_Create_Group)) 
			{
				progress = ProgressDialog.show(MainActivity._this, "", MainActivity._this.getString(R.string.in_progress), true, true);
	    		
				photoAssigned=intent.getBooleanExtra("photoAssigned", false);
				if (photoAssigned)
				{
					photoPath=intent.getStringExtra("groupPhotoPath");
				}
				new Thread(registerGroup,"registerGroup").start();
			}
		}
	};
	int maxGroupNum = 15;
	OnItemClickListener onChooseUser = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Map<String, String> map = amperList.get(position);
			if (map.get("checked").equals("0")) {
				if (mCount >= maxGroupNum)
					return;
				map.put("checked", "1");
				mCount++;
			} else {
				map.put("checked", "0");
				mCount--;
			}

			gridAdapter.notifyDataSetInvalidated();
		}
	};

	Runnable mFetchFriends = new Runnable() {
		public void run() {
			amperList.clear();
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
//					File f = new File(userphotoPath);
//					if (!f.exists())
//						userphotoPath = null;
					//tml*** userphoto fix
					if (!new File(userphotoPath).exists()) {
						userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
						if (!new File(userphotoPath).exists())
							userphotoPath = null;
					}
					Log.e("tml userphotoPath=" + userphotoPath);

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
					map.put("status", String.valueOf(ContactsOnline.getContactOnlineStatus(address)));
					amperList.add(map);
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

	Runnable registerGroup = new Runnable() {
		public void run() {
			chosenList.remove("0");
			String Return = "";
			String members = "";
			for (int i = 0; i < chosenList.size(); i++) {
				String id = chosenList.get(i);
				if (i > 0)
					members += ",";
				members += id;
			}

			int myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);

			try {
				int c = 0;
				do {
					MyNet net = new MyNet(MainActivity._this);
					Return = net.doPostHttps("create_group.php",
							"id=" + myIdx + "&members=" + members + 
							"&name=" + URLEncoder.encode(groupName, "UTF-8"),
							null);
					if (Return.startsWith("Done"))
						break;
					MyUtil.Sleep(2500);
				} while (++c < 3);
			} catch (Exception e) {
			}

			int groupidx = 0;
			if (Return.startsWith("Done")) {
				Return = Return.substring(5);
				groupidx = Integer.parseInt(Return);
			}

			if (groupidx == 0) {
				mHandler.post(new Runnable(){
					public void run()
					{
						if (progress != null && progress.isShowing())
							progress.dismiss();
					}
				});
				return;
			}

			GroupDB gdb = new GroupDB(MainActivity._this);
			gdb.open();

			for (int i = 0; i < chosenList.size(); i++) {
				int idx = Integer.parseInt(chosenList.get(i));
				gdb.insertGroup(groupidx, groupName, idx);
			}
			gdb.close();

			mADB.insertUser("[<GROUP>]" + groupidx, groupidx + 100000000, groupName);
			if (photoAssigned)
			{
				File f=new File(photoPath);
				String localPath=Global.SdcardPath_inbox +"photo_"+(groupidx+100000000)+"b.jpg";
				File f2=new File(localPath);
				f.renameTo(f2);
//				Log.e("tmlg photoPath=" + photoPath + " " + f.exists());
//				Log.e("tmlg localPath=" + localPath + " " + f2.exists());
				
				try {
					int count = 0;
					do {
						MyNet net = new MyNet(MainActivity._this);
						Return = net.doPostAttach("uploadgroupphoto.php", groupidx, 0, localPath, null);
						if (Return.startsWith("Done"))
							break;
						MyUtil.Sleep(2500);
					} while (++count < 3);
				} catch (Exception e) {}
			}

			SendAgent agent = new SendAgent(MainActivity._this, myIdx, 0, true);

			agent.setAsGroup(groupidx);
			ArrayList<String> addressList = new ArrayList<String>();
			try {
				for (int i = 0; i < chosenList.size(); i++)
					addressList.add(mADB.getAddressByIdx(Integer
							.parseInt(chosenList.get(i))));
				agent.onMultipleSend(addressList, ":)(Y)", 0, null, null);
			} catch (Exception e) {
			}

			mADB.close();
			
			mHandler.post(new Runnable(){
				public void run()
				{
					if (progress != null && progress.isShowing())
						progress.dismiss();
					
					UserPage.forceRefresh=true;
					MainActivity._this.switchInflater(0);
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
				convertView = View.inflate(icontext, R.layout.user_tiny_cell,
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
							imageViewByTag = (ImageView) userGridView
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
				Drawable d = MainActivity._this.getResources().getDrawable(
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

	@Override
	public void destroy() {
		try {
			if (MainActivity._this != null)
				MainActivity._this.unregisterReceiver(handleRegisterGroup);
		} catch (Exception e) {}
		if (mADB != null && mADB.isOpen())
			mADB.close();
		amperList.clear();
		chosenList.clear();
		System.gc();
		System.gc();
	}
}
