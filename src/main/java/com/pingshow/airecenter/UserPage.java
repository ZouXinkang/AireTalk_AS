package com.pingshow.airecenter;

import java.io.File;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
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
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.SearchPage.UserItemAdapter;
import com.pingshow.airecenter.cons.Key;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.contacts.RelatedUserInfo;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.GroupDB;
import com.pingshow.airecenter.db.RelatedUserDB;
import com.pingshow.airecenter.db.SmsDB;
import com.pingshow.airecenter.view.RoundImageview;
import com.pingshow.network.MyNet;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.VideoConf;

public class UserPage extends Page {

	static final int MAX_USERS = 300;
	static int orientation = -1;
	public static boolean needRefresh = true;
	private MyPreference mPref;
	private AmpUserDB mADB;
	private SmsDB mSmsDB;
	// private RelatedUserDB mRDB;
	private GroupDB mGDB;
	private ContactsQuery cq;
	static private List<Map<String, String>> amperList;
	static private List<Map<String, String>> orgList;
	private static UserItemAdapter friendAdapter;

	private GridView friendGrid;
	private AsyncImageLoader asyncImageLoader;

	private Handler mHandler = new Handler();

	private int numColumns = 4;
	public static int numTrueFriends = 1;
	private static int editing = 0;

	static public boolean forceRefresh = true;
	static public int sortMethod = 1;

	private float mDensity = 1.f;
	private View layout;
	private boolean largeScreen;

	private boolean unreadMsg = false;

	public static String passKeyword;
	private ArrayList<String> sendeeList = null;

	// private StrangerItemAdapter strangeAdapter;
	// static private List<Map<String, String>> amperListStrange;
	// private GridView resultGridView;
	// private RelatedUserDB mRDB;

	public UserPage(View v) {
		Log.e("*** !!! USERPAGE *** START START !!! ***");
		layout = v;

		mPref = new MyPreference(MainActivity._this);

		mDensity = MainActivity._this.getResources().getDisplayMetrics().density;
		largeScreen = ((View) layout.findViewById(R.id.large) != null);

		mADB = new AmpUserDB(MainActivity._this);
		mADB.open();

		mSmsDB = new SmsDB(MainActivity._this);
		mSmsDB.open();

		/*
		 * mRDB = new RelatedUserDB(MainActivity._this); mRDB.open();
		 */

		mGDB = new GroupDB(MainActivity._this);
		mGDB.open();

		editing = 0;
		cq = new ContactsQuery(MainActivity._this);

		if (amperList == null)
			amperList = new ArrayList<Map<String, String>>();
		if (orgList == null)
			orgList = new ArrayList<Map<String, String>>();

		friendAdapter = new UserItemAdapter(MainActivity._this, 0);

		friendGrid = (GridView) layout.findViewById(R.id.friendsGridView);

		friendGrid.setVisibility(View.VISIBLE);
		friendGrid.setNumColumns(numColumns);

		friendGrid.setOnItemClickListener(onChooseUser);
		// xia ����item��������������
		friendGrid.setOnItemLongClickListener(onLongChooseUser);
		((Button) layout.findViewById(R.id.block))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {

						if (editing > 0)
							editing = 0;
						else
							editing = 2;
						friendAdapter.notifyDataSetChanged();
					}
				});

		((Button) layout.findViewById(R.id.delete))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (editing > 0)
							editing = 0;
						else
							editing = 1;
						friendAdapter.notifyDataSetChanged();
					}
				});
		// tml*** beta ui2
		((Button) layout.findViewById(R.id.edit))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean visible = ((LinearLayout) layout
								.findViewById(R.id.editbar)).getVisibility() == View.VISIBLE;
						if (visible) {
							((LinearLayout) layout.findViewById(R.id.editbar))
									.setVisibility(View.GONE);
						} else {
							((LinearLayout) layout.findViewById(R.id.editbar))
									.setVisibility(View.VISIBLE);
							Animation anim = AnimationUtils.loadAnimation(
									MainActivity._this, R.anim.slide_slow_down);
							((LinearLayout) layout.findViewById(R.id.editbar))
									.startAnimation(anim);
						}
					}
				});

		((Button) layout.findViewById(R.id.conference))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						MainActivity._this.neverSayNeverDie(MainActivity._this,
								2);
						MainActivity._this.switchInflater(4);

					}
				});

		// tml|sw*** no airecall in china
		boolean isCN = MyUtil.isISO_China(MainActivity._this, mPref, null);
		boolean airecall = mPref.readBoolean("AIRECALL", false);
		if (airecall && isCN)
			isCN = false;
		if (AireCallPage.OverrideShowAireCall)
			isCN = false;
		if (isCN) {
			// ((Button) layout.findViewById(R.id.airecall)).setAlpha(0.5F);
			((Button) layout.findViewById(R.id.airecall))
					.setVisibility(View.GONE);
		} else {
			// float alpha1 = ((Button)
			// layout.findViewById(R.id.airecall)).getAlpha();
			// if (alpha1 < 1.0F) {
			// ((Button) layout.findViewById(R.id.airecall)).setAlpha(1.0F);
			// }
			((Button) layout.findViewById(R.id.airecall))
					.setVisibility(View.VISIBLE);
		}
		((Button) layout.findViewById(R.id.airecall))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean isCN = MyUtil.isISO_China(MainActivity._this,
								mPref, null);
						boolean airecall = mPref.readBoolean("AIRECALL", false);
						if (airecall && isCN)
							isCN = false;
						if (AireCallPage.OverrideShowAireCall)
							isCN = false;
						if (isCN) {
							Intent it = new Intent(MainActivity._this,
									Tooltip.class);
							String content = MainActivity._this
									.getString(R.string.airecall_notsupported);
							it.putExtra("Content", content);
							MainActivity._this.startActivity(it);
						} else {
							MainActivity._this.neverSayNeverDie(
									MainActivity._this, 2);
							MainActivity._this.switchInflater(3);
						}
					}
				});
		// ***tml

		// tml*** search add
		((Button) layout.findViewById(R.id.searchadd))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						EditText kw = (EditText) layout
								.findViewById(R.id.keyword);
						passKeyword = kw.getText().toString().trim();
						// if (passKeyword.length() > 0) {
						hideKeyboard(); // tml*** alpha ui/
						// }
						((EditText) layout.findViewById(R.id.keyword))
								.setText("");
						onFilterUserQuery("");
						MainActivity._this.switchInflater(1);
						// mKeyword = kw.getText().toString().trim();
						// hideKeyboard();
						//
						// int max=3;
						// if(mKeyword.getBytes().length != mKeyword.length())
						// max = 1; //chinese
						//
						// if (mKeyword.length() >= max) {
						// mHandler.post(popupProgressDialog);
						// if (!MyUtil.checkNetwork(MainActivity._this)) {
						// return;
						// }
						// new Thread(searchStrangers).start();
						// }
					}
				});
		// ***tml

		IntentFilter intentToReceiveFilter = new IntentFilter();
		intentToReceiveFilter.addAction(Global.Action_Refresh_Gallery);
		intentToReceiveFilter.addAction(Global.Action_Friends_Status_Updated);
		intentToReceiveFilter.addAction(Global.Action_UserPage_Command);
		MainActivity._this.registerReceiver(handleFreshItems,
				intentToReceiveFilter);

		friendGrid.setAdapter(friendAdapter);

		sortMethod = mPref.readInt("SortMethod", 1);// bytime defalut

		onFafaUserQuery();
		mHandler.post(new Runnable() {
			public void run() {
				friendAdapter.notifyDataSetChanged();
			}
		});

		((EditText) layout.findViewById(R.id.keyword))
				.addTextChangedListener(new TextWatcher() {
					@Override
					public void afterTextChanged(Editable s) {
						onFilterUserQuery(s.toString());

						if (s.toString().length() == 0) {
							((ImageView) layout.findViewById(R.id.clear))
									.setVisibility(View.GONE);
							((EditText) layout.findViewById(R.id.keyword))
									.setPadding((int) (16.f * mDensity),
											(int) (6.f * mDensity),
											(int) (16.f * mDensity),
											(int) (6.f * mDensity));
						} else {
							((ImageView) layout.findViewById(R.id.clear))
									.setVisibility(View.VISIBLE);
							((EditText) layout.findViewById(R.id.keyword))
									.setPadding((int) (56.f * mDensity),
											(int) (6.f * mDensity),
											(int) (16.f * mDensity),
											(int) (6.f * mDensity));
						}
					}

					@Override
					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
					}

					@Override
					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}
				});

		((ImageView) layout.findViewById(R.id.clear))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						((EditText) layout.findViewById(R.id.keyword))
								.setText("");
						onFilterUserQuery("");
					}
				});

		mHandler.postDelayed(mInstantQueryOnlineFriends, 3000);
	};

	String address;
	int idx;
	String mDisplayname;
	String nickname;
	private boolean broadcastConf = false;
	OnItemClickListener onChooseUser = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {

			if (editing > 0) {// 编辑状态

			} else {
				Map<String, String> map = amperList.get(position);
				address = map.get("address");
				// bree:一键通
				if (address.startsWith("[<GROUP>]")) {// 群组通话
					mGDB.open(true);
					int groupID = Integer.parseInt(address.substring(9));
					sendeeList = mGDB.getGroupMembersByGroupIdx(groupID);
					mGDB.close();

					if (mPref.readBoolean("EnableRadio")) {// 一键广播
						Log.i("---->一键广播");
						broadcastConf = true;
					} else {// 一键多方
						Log.i("---->一键多方");
						broadcastConf = false;
					}
					try {
						if (sendeeList.size() > 0 && sendeeList.size() <= 9) {
							AireVenus.setCallType(AireVenus.CALLTYPE_CHATROOM);
							mPref.write("incomingChatroom", false);

							int myIdx = 0;
							try {
								myIdx = Integer.parseInt(
										mPref.read("myID", "0"), 16);
								mPref.write("ChatroomHostIdx", myIdx);
							} catch (Exception e) {
							}

							String idx = "" + myIdx;
							MakeCall.ConferenceCall(MainActivity._this, idx,-1,false);

							new Thread(sendNotifyForJoinChatroom).start();
						}

					} catch (Exception e) {
					}

				} else {
					// 普通电话
					MakeCall.Call(MainActivity._this, address, true, false);
				}

			}
		}

	};
	OnItemLongClickListener onLongChooseUser = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			if (editing > 0) {
				return true;
			}
			Map<String, String> map = amperList.get(position);
			address = map.get("address");
			if (address.equals("----"))
				return true;
			setDeleteAndBlock(position, map);
			return true;
		}

	};

	private void refresh() {
		Log.d("refresh() friendAdapter");
		forceRefresh = true;
		needRefresh = true;
		ContactsOnline.setContactOnlineStatus(address, 0);
		onFafaUserQuery();
		editing = 0;
		friendAdapter.notifyDataSetChanged();
	}

	private void deleteUser() {
		if (!MyUtil.checkNetwork(MainActivity._this)) {
			return;
		}
		mADB.deleteContactByAddress(address);
		if (address.startsWith("[<GROUP>]"))// alec
		{
			int GroupID = 0;
			try {
				GroupID = Integer.parseInt(address.substring(9));
			} catch (Exception e) {
			}

			Intent it = new Intent(Global.Action_InternalCMD);
			it.putExtra("Command", Global.CMD_DELETE_GROUP);
			it.putExtra("GroupID", GroupID);
			MainActivity._this.sendBroadcast(it);
		} else if (idx > 50) {
			Intent it = new Intent(Global.Action_InternalCMD);
			it.putExtra("Command", Global.CMD_UPLOAD_FRIENDS);
			it.putExtra("type", 1);// Single Friend
			it.putExtra("serverType", 0);// Remove
			it.putExtra("idxlist", idx + "");
			MainActivity._this.sendBroadcast(it);
		}

		refresh();
	}

	private void blockUser() {
		if (!MyUtil.checkNetwork(MainActivity._this)) {
			return;
		}

		needRefresh = true;

		mADB.blockUserByAddress(address, 1);
		if (address.startsWith("[<GROUP>]"))// alec
		{
			int GroupID = 0;
			try {
				GroupID = Integer.parseInt(address.substring(9));
			} catch (Exception e) {
			}

			Intent it = new Intent(Global.Action_InternalCMD);
			it.putExtra("Command", Global.CMD_DELETE_GROUP);
			it.putExtra("GroupID", GroupID);
			MainActivity._this.sendBroadcast(it);
		} else {
			Intent it = new Intent(Global.Action_InternalCMD);
			it.putExtra("Command", Global.CMD_UPLOAD_FRIENDS);
			it.putExtra("type", 1);// Single Friend
			it.putExtra("serverType", 0);// Remove
			it.putExtra("idxlist", idx + "");
			MainActivity._this.sendBroadcast(it);
		}

		refresh();
	}

	private void unblockUser() {
		mADB.blockUserByAddress(address, 0);
		refresh();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {

	}

	public void onResume() {
		if (sortMethod > 0) {
			if (forceRefresh) {
				needRefresh = true;
				onFafaUserQuery();
				friendAdapter.notifyDataSetChanged();
			}
		}
	}

	// tml*** backpress fix
	public static boolean onBackPressed() {
		if (editing > 0) {
			editing = 0;
			friendAdapter.notifyDataSetChanged();
			return true;
		}
		return false;
	}

	int fresh_count = 0;

	final Runnable mInstantQueryOnlineFriends = new Runnable() {
		public void run() {
			if (MainActivity._this != null) {
				Intent it = new Intent(Global.Action_InternalCMD);
				it.putExtra("Command", Global.CMD_CHECK_ONLINE_FRIENDS);
				MainActivity._this.sendBroadcast(it);
			}

			if (fresh_count++ < 5)
				mHandler.postDelayed(mInstantQueryOnlineFriends, 120000);
		}
	};

	private UserItemAdapter getFriendAdapter() {
		return friendAdapter;
	}

	BroadcastReceiver handleFreshItems = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, final Intent intent) {
			Log.d("UserPage handleFreshItems=" + intent.getAction() + " "
					+ forceRefresh + needRefresh + " " + amperList.size() + " "
					+ editing);
			if (intent.getAction().equals(Global.Action_Refresh_Gallery)) {
				if (amperList != null && editing == 0) {
					needRefresh = true;
					onFafaUserQuery();
					// friendAdapter.notifyDataSetChanged();
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							getFriendAdapter().notifyDataSetChanged();
						}
					});
					// friendGrid.invalidateViews();
					// friendGrid.refreshDrawableState();
					// friendGrid.requestLayout();
				}
			} else if (intent.getAction().equals(
					Global.Action_Friends_Status_Updated)) {

				if (forceRefresh) {
					needRefresh = true;
					onFafaUserQuery();
					friendAdapter.notifyDataSetChanged();
					return;
				}
				if (amperList != null && amperList.size() > 1) {
					friendAdapter.notifyDataSetChanged();
				}
			} else if (intent.getAction()
					.equals(Global.Action_UserPage_Command)) {
				int cmd = intent.getIntExtra("Command", 0);
				Log.d("Action_UserPage_Command=" + cmd);
				if (cmd == 50)
					deleteUser();
				else if (cmd == 60)
					blockUser();
				else if (cmd == 40)
					unblockUser();
			}
		}
	};

	class ViewHolder {
		TextView friendName;
		RoundImageview photoimage;
		ImageView delete;
		TextView separator;
		ImageView online;
		TextView mood;
		TextView unread;
	}

	public class UserItemAdapter extends BaseAdapter {
		int type;
		Context icontext;

		public UserItemAdapter(Context context, int type) {
			icontext = context;
			this.type = type;
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
		public View getView(final int position, View convertView,
				ViewGroup parent) {

			final Map<String, String> map;
			try {
				map = amperList.get(position);
			} catch (Exception e) {
				if (convertView != null)
					return convertView;
				ViewHolder holder = new ViewHolder();
				if (type == 0)
					convertView = View.inflate(icontext,
							R.layout.userinfo_cell, null);
				convertView.setTag(holder);
				return convertView;
			}

			String imagePath = map.get("imagePath");

			final ViewHolder holder;

			int seperator = Integer.parseInt(map.get("seperator"));

			if (convertView == null) {
				holder = new ViewHolder();
				if (type == 0)
					convertView = View.inflate(icontext,
							R.layout.userinfo_cell, null);
				holder.photoimage = (RoundImageview) convertView.findViewById(R.id.photo);
				holder.friendName = (TextView) convertView
						.findViewById(R.id.friendname);
				holder.separator = (TextView) convertView
						.findViewById(R.id.separator);
				holder.delete = (ImageView) convertView
						.findViewById(R.id.delete);
				holder.unread = (TextView) convertView
						.findViewById(R.id.unread);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.photoimage.setTag(imagePath);
			final String address = map.get("address");

			Drawable cachedImage = asyncImageLoader.loadDrawable(imagePath,
					new ImageCallback() {
						public void imageLoaded(Drawable imageDrawable,
								String path) {
							ImageView imageViewByTag = null;
							if (type == 0)
								imageViewByTag = (ImageView) friendGrid
										.findViewWithTag(path);
							if (imageViewByTag != null && imageDrawable != null) {
								imageViewByTag.setImageDrawable(imageDrawable);
							}
						}
					});

			if (cachedImage != null && imagePath != null)
				holder.photoimage.setImageDrawable(cachedImage);
			else {
				if (address.startsWith("[<GROUP>]"))
					holder.photoimage.setImageResource(R.drawable.group_empty);
				else
					holder.photoimage.setImageResource(R.drawable.bighead);
			}
			int actual = Integer.parseInt(map.get("actual"));
			int unread = 0;
			String unreadStr = map.get("unread");
			if (unreadStr != null) {
				try {
					unread = Integer.parseInt(unreadStr);
				} catch (Exception e) {
				}
			}

			if (actual > 0 && unread > 0) {
				holder.unread.setVisibility(View.VISIBLE);
				holder.unread.setText("" + unread);
			} else {
				holder.unread.setVisibility(View.GONE);
			}

			String disname = map.get("displayName");

			if (MainActivity._this == null)
				return convertView;
			if (type == 0) {
				if (seperator == 0)
					holder.separator.setVisibility(View.GONE);
				else {
					if (getItemId(position) == numTrueFriends) {
						holder.separator.setTextSize(largeScreen ? 22 : 18);
						holder.separator.setTextColor(0xffffff);
						holder.separator.setText(MainActivity._this
								.getString(R.string.black_list));
					}
					holder.separator.setVisibility(View.VISIBLE);
				}
			} else
				holder.separator.setVisibility(View.GONE);

			if (disname.equals("-")) {
				if (type == 0)
					holder.separator.setVisibility(View.GONE);
				/*
				 * else holder.separator.setVisibility(View.VISIBLE);
				 */
				holder.photoimage.setVisibility(View.GONE);
				holder.friendName.setVisibility(View.GONE);
				holder.delete.setVisibility(View.GONE);
			} else if (disname.equals("----")) {
				if (type == 0) {
					holder.photoimage.setVisibility(View.INVISIBLE);
					holder.friendName.setVisibility(View.INVISIBLE);
					holder.delete.setVisibility(View.INVISIBLE);
					if (getItemId(position) == numTrueFriends) {
						holder.separator.setTextSize(largeScreen ? 22 : 18);
						holder.separator.setTextColor(0xffffffff);
						if (MainActivity._this == null)
							return convertView;
						holder.separator.setText(MainActivity._this
								.getString(R.string.black_list));
					}
				}
				// convertView.setClickable(true);
			} else {
				holder.friendName.setText(disname);
				holder.photoimage.setVisibility(View.VISIBLE);
				holder.friendName.setVisibility(View.VISIBLE);
				// convertView.setClickable(false);
				holder.delete.setVisibility((editing > 0) ? View.VISIBLE
						: View.INVISIBLE);
			}

			if (editing == 1) {
				holder.delete.setImageResource(R.drawable.delete);
			} else if (editing == 2) {
				holder.delete.setImageResource(R.drawable.block_big);
			}
			// bree������ɾ��ͼ��ĵ���¼�
			holder.delete.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					UserPage.this.address = address;
					if (address.equals("----"))
						return;
					setDeleteAndBlock(position, map);

				}
			});
			String b = map.get("blocked");
			if (MainActivity._this == null)
				return convertView;
			if (type == 0) {
				if (address.startsWith("[<GROUP>]")) {
					holder.photoimage
							.setBackgroundResource(R.drawable.group_bg);
				} else {
					holder.photoimage.setBackgroundResource(R.drawable.empty);
					int status = ContactsOnline.getContactOnlineStatus(address);
					if (status > 0) // tml TODO
					{
						Drawable d = MainActivity._this.getResources()
								.getDrawable(R.drawable.online_light);
						d.setBounds(0, 0, (int) (20.f * mDensity),
								(int) (20.f * mDensity));
						SpannableString spannable = new SpannableString("*"
								+ disname);
						ImageSpan icon = new ImageSpan(d,
								ImageSpan.ALIGN_BOTTOM);
						spannable.setSpan(icon, 0, 1,
								SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
						holder.friendName.setText(spannable);
					}
				}
				convertView.setBackgroundResource(R.drawable.null_draw);
				/*
				 * }else{
				 * 
				 * if (address.startsWith("[<GROUP>]"))
				 * holder.photoimage.setBackgroundResource(R.drawable.group_bg);
				 * else
				 * holder.photoimage.setBackgroundResource(R.drawable.empty);
				 * 
				 * if (address.equals("-"))
				 * convertView.setBackgroundResource(R.drawable.null_draw); else
				 * convertView.setBackgroundResource(R.drawable.lightblue_draw);
				 * }
				 */
			}

			return convertView;
		}
	}

	public void destroy() {
		mHandler.removeCallbacks(mInstantQueryOnlineFriends);
		try {
			if (MainActivity._this != null)
				MainActivity._this.unregisterReceiver(handleFreshItems);
		} catch (Exception e) {
		}
		if (mADB != null && mADB.isOpen())
			mADB.close();
		/*
		 * if (mRDB != null && mRDB.isOpen()) mRDB.close();
		 */
		if (mGDB != null && mGDB.isOpen())
			mGDB.close();
		if (mSmsDB != null && mSmsDB.isOpen())
			mSmsDB.close();
		System.gc();
		System.gc();
	}

	private void onFafaUserQuery() {

		Log.d("onFafaUserQuery need/forceRefresh " + needRefresh + " "
				+ forceRefresh);

		if (!needRefresh && !forceRefresh) {
			Log.d("onFafaUserQuery NO refresh");
			return;
		}

		if (amperList != null) {
			amperList.clear();
		}

		if (orgList != null) {
			orgList.clear();
		}

		HashMap<String, String> map;
		Cursor[] cursor = (Cursor[]) new Cursor[2];
		if (sortMethod == 0)
			cursor[0] = mADB.fetchAll();
		else if (sortMethod == 1 || sortMethod == 2)
			cursor[0] = mADB.fetchAllByTime();
		cursor[1] = mADB.fetchBlockedUsers();

		try {
			unreadMsg = false;
			int unknowns = 0;
			int numRF = cursor[1].getCount();

			for (int loop = 0; loop < 2; loop++) {
				if (loop == 0)
					numTrueFriends = cursor[loop].getCount();

				if (!cursor[loop].moveToFirst()) {
					MyUtil.Sleep(200);
					continue;
				}

				do {
					String address = cursor[loop].getString(1);
					if (address.startsWith("Done=")) { // tml*** getuserinfo
														// temp fix
						mADB.deleteContactByAddress(address);
						continue;
					}
					int idX = cursor[loop].getInt(3);
					long contactId = cq.getContactIdByNumber(address);
					String disName = "";
					String userphotoPath;

					userphotoPath = Global.SdcardPath_inbox + "photo_" + idX
							+ "b.jpg";
					if (!new File(userphotoPath).exists()) {
						userphotoPath = Global.SdcardPath_inbox + "photo_"
								+ idX + ".jpg";
						if (!new File(userphotoPath).exists())
							userphotoPath = null;
					}
					if (userphotoPath == null) {
						Log.w("null pic! " + address + " path=" + userphotoPath);
					}

					if (contactId > 0)
						disName = cq.getNameByContactId(contactId);
					else if (loop == 1)
						disName = cursor[loop].getString(2);
					else
						disName = cursor[loop].getString(4);

					if (disName == null || disName.length() == 0) {
						disName = MainActivity._this
								.getString(R.string.unknown_person);
						unknowns++; // tml|james*** unknown contacts error/
					}

					map = new HashMap<String, String>();

					map.put("displayName", disName);
					map.put("address", address);
					map.put("idx", idX + "");
					map.put("imagePath", userphotoPath);
					map.put("contactId", contactId + "");
					int count = mSmsDB.getUnreadCountByAddress(address);
					map.put("unread", "" + count);
					if (loop == 0) {
						map.put("actual", "1");
						map.put("seperator", "0");
						map.put("blocked", "0");
					} else {
						map.put("seperator", "0");
						map.put("blocked", "1");
						map.put("actual", "0");
					}
					if (sortMethod == 2) {
						int status = ContactsOnline
								.getContactOnlineStatus(address);
						if (status > 0) {
							amperList.add(0, map);
							orgList.add(0, map);
						} else {
							amperList.add(map);
							orgList.add(map);
						}
					} else {
						amperList.add(map);
						orgList.add(map);
					}
					if (count > 0) { // tml*** unread led
						unreadMsg = true;
					}
				} while (cursor[loop].moveToNext()
						&& amperList.size() <= MAX_USERS);

				if (loop == 0) {
					// SortList.sortMapList(amperList[0]);

					if (numRF > 0) {
						int count = amperList.size() % numColumns;
						if (count != 0) {
							for (int i = 0; i < numColumns - count; i++)
								addDummyMap(amperList, "----");
						}
					}
				}

				if (loop == 0 && numRF > 0) {
					if (amperList.size() > 0)
						for (int i = 0; i < numColumns; i++) {
							map = (HashMap<String, String>) amperList
									.get(amperList.size() - 1 - i);
							map.put("seperator", "1");
						}
				}

				// tml|james*** unknown contacts error
				int unknownBreak = ((numTrueFriends - 1) / 4);
				if (unknownBreak < 1)
					unknownBreak = 1;
				Log.d("check !@#$unknownsF " + unknowns + " >? " + unknownBreak
						+ "/" + (numTrueFriends - 1));
				if (unknowns > unknownBreak) {
					Intent intent = new Intent(Global.Action_InternalCMD);
					intent.putExtra("unknowns", true);
					intent.putExtra("Command", Global.CMD_DOWNLOAD_FRIENDS);
					MainActivity._this.sendBroadcast(intent);
					unknowns = 0;
				}
				// ***tml
			}

		} catch (Exception e) {
		}

		if (cursor[0] != null && !cursor[0].isClosed())
			cursor[0].close();

		if (cursor[1] != null && !cursor[1].isClosed())
			cursor[1].close();

		needRefresh = false;
		forceRefresh = false;

		// tml*** unread led
		if (MyUtil.CheckServiceExists(MainActivity._this,
				"com.pingshow.airecenter.AireJupiter")) {
			if (AireJupiter.getInstance() != null) {
				if (unreadMsg) {
					AireJupiter.getInstance().doCheckUnread();
				} else {
					AireJupiter.getInstance().unreadBlinkOff();
				}
			}
		}
	}

	private void onFilterUserQuery(String keyword) {

		if (amperList != null) {
			amperList.clear();
		}

		for (Map<String, String> map : orgList) {
			if (keyword.length() == 0
					|| ((String) map.get("displayName")).toLowerCase()
							.contains(keyword.toLowerCase())) {
				amperList.add(map);
			}
		}

		friendAdapter.notifyDataSetChanged();
	}

	void addDummyMap(List<Map<String, String>> list, String displayName) {
		HashMap<String, String> map;
		map = new HashMap<String, String>();
		// map.put("containChinese","no");
		map.put("displayName", displayName);
		map.put("address", "-");
		map.put("imagePath", null);
		map.put("contactId", "-20");
		map.put("seperator", "1");
		map.put("actual", "0");
		map.put("blocked", "0");
		list.add(map);
	}

	// tml*** alpha ui
	private void loadAsSerchPage() {
		MainActivity._this.switchInflater(1);
	}

	void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) MainActivity._this
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(
				((EditText) layout.findViewById(R.id.keyword)).getWindowToken(),
				0);
	}

	// ***tml
	// bree:
	public void setDeleteAndBlock(int position, Map<String, String> map) {
		long contact_id = -20;
		try {
			idx = Integer.parseInt(map.get("idx"));
			contact_id = Long.parseLong((String) map.get("contactId"));
			nickname = map.get("displayName");
		} catch (Exception e) {
		}

		if (mADB.isFafauser(address)) {
			if (editing == 0) {
				try {
					// Intent it;
					// if (address.startsWith("[<GROUP>]"))
					// {
					// it = new Intent(MainActivity._this,
					// GroupFunctionActivity.class);
					// int mGroupID=Integer.parseInt(address.substring(9));
					// ArrayList<String>
					// chosenList=mGDB.getGroupMembersByGroupIdx(mGroupID);
					// it.putExtra("chosenList", (Serializable) chosenList);
					// }else
					// it = new Intent(MainActivity._this,
					// FunctionActivity.class);
					// it.putExtra("Contact_id", contact_id);
					// it.putExtra("Address", address);
					// it.putExtra("Nickname", nickname);
					// it.putExtra("Online",
					// ContactsOnline.getContactOnlineStatus(address));
					// if (contact_id > 0)
					// it.putExtra("AireNickname",
					// mADB.getNicknameByAddress(address));
					// it.putExtra("Idx", Integer.parseInt(map.get("idx")));
					// MainActivity._this.startActivity(it);
					// tml*** beta ui3
					if (mADB.isOpen()) {
						mADB.updateLastContactTimeByIdx(
								Integer.parseInt(map.get("idx")),
								new Date().getTime());
						if (UserPage.sortMethod == 1)
							UserPage.forceRefresh = true;
					}

					Intent it = new Intent(MainActivity._this,
							ConversationActivity.class);
					it.putExtra("SendeeContactId", contact_id);
					it.putExtra("SendeeNumber", address);
					it.putExtra("SendeeDisplayname", nickname);
					it.putExtra("Idx", Integer.parseInt(map.get("idx")));
					MainActivity._this.startActivity(it);
				} catch (Exception e) {
				}
			} else if (editing == 1) {
				if (idx > 50) {
					Intent it = new Intent(MainActivity._this,
							ConfirmDialog.class);
					it.putExtra("Nickname", nickname);
					it.putExtra("Address", address);
					it.putExtra("Idx", idx);
					it.putExtra("Tag", position);
					it.putExtra("msgContent", String.format(MainActivity._this
							.getResources()
							.getString(R.string.delete_this_user), nickname));
					it.putExtra("numItems", 2);
					it.putExtra("ItemCaption0",
							MainActivity._this.getString(R.string.cancel));
					it.putExtra("ItemResult0", Activity.RESULT_CANCELED);
					it.putExtra("ItemCaption1",
							MainActivity._this.getString(R.string.delete));
					it.putExtra("ItemResult1", ConfirmDialog.DELETE_THIS_USER);
					MainActivity._this.startActivity(it);
				}
			} else if (editing == 2) {
				if (idx > 50 || idx == 4) {
					Intent it = new Intent(MainActivity._this,
							ConfirmDialog.class);
					it.putExtra("Tag", position);
					it.putExtra("Nickname", nickname);
					it.putExtra("Address", address);
					it.putExtra("Idx", idx);
					it.putExtra("msgContent", String.format(
							MainActivity._this.getResources().getString(
									R.string.block_this_user), nickname));
					it.putExtra("numItems", 2);
					it.putExtra("ItemCaption0",
							MainActivity._this.getString(R.string.cancel));
					it.putExtra("ItemResult0", Activity.RESULT_CANCELED);
					it.putExtra("ItemCaption1",
							MainActivity._this.getString(R.string.block));
					it.putExtra("ItemResult1", ConfirmDialog.BLOCK_THIS_USER);
					MainActivity._this.startActivity(it);
				}
			}
		} else {
			if (editing == 1) {
				if (idx > 50) {
					Intent it = new Intent(MainActivity._this,
							ConfirmDialog.class);
					it.putExtra("Nickname", nickname);
					it.putExtra("Address", address);
					it.putExtra("Idx", idx);
					it.putExtra("Tag", position);
					it.putExtra("msgContent", String.format(MainActivity._this
							.getResources()
							.getString(R.string.delete_this_user), nickname));
					it.putExtra("numItems", 2);
					it.putExtra("ItemCaption0",
							MainActivity._this.getString(R.string.cancel));
					it.putExtra("ItemResult0", Activity.RESULT_CANCELED);
					it.putExtra("ItemCaption1",
							MainActivity._this.getString(R.string.delete));
					it.putExtra("ItemResult1", ConfirmDialog.DELETE_THIS_USER);
					MainActivity._this.startActivity(it);
				}
			} else {
				Intent it = new Intent(MainActivity._this, ConfirmDialog.class);
				it.putExtra("Nickname", nickname);
				it.putExtra("Address", address);
				it.putExtra("Idx", idx);
				it.putExtra("msgContent", String.format(MainActivity._this
						.getResources().getString(R.string.unblock_this_user),
						nickname));
				it.putExtra("numItems", 2);
				it.putExtra("ItemCaption0",
						MainActivity._this.getString(R.string.unblock));
				it.putExtra("ItemResult0", ConfirmDialog.UNBLOCK_THIS_USER);
				it.putExtra("ItemCaption1",
						MainActivity._this.getString(R.string.cancel));
				it.putExtra("ItemResult1", Activity.RESULT_CANCELED);
				MainActivity._this.startActivity(it);
			}
		}
	}

	Runnable sendNotifyForJoinChatroom = new Runnable() {
		public void run() {
			String myIdxHex = mPref.read("myID", "0");

			String ServerIP = mPref.read("conferenceSipServer",
					AireJupiter.myConfSipServer_default);
			if (AireJupiter.getInstance() != null) {
				ServerIP = AireJupiter.getInstance().getIsoConf(ServerIP); // tml***
																			// china
																			// ip
			}
			long ip = MyUtil.ipToLong(ServerIP);
			String HexIP = Long.toHexString(ip);

			String content = Global.Call_Conference + "\n\n" + HexIP + "\n\n"
					+ myIdxHex;
			// tml*** broadcast
			if (broadcastConf) {
				mPref.write(Key.BCAST_CONF, 1);
				content = Global.Call_Conference + Global.Call_Broadcast
						+ "\n\n" + HexIP + "\n\n" + myIdxHex;
			} else {
				mPref.write(Key.BCAST_CONF, -1);
			}

			if (AireJupiter.getInstance() != null)
				AireJupiter.getInstance().updateCallDebugStatus(true, null);
			for (int i = 0; i < sendeeList.size(); i++) {
				int idx = Integer.parseInt(sendeeList.get(i));
				if (idx < 50)
					continue;

				String address = mADB.getAddressByIdx(idx);

				if (AireJupiter.getInstance() != null
						&& AireJupiter.getInstance().tcpSocket() != null) {
					if (AireJupiter.getInstance().isLogged()) {
						if (i > 0)
							MyUtil.Sleep(500);
						if (AireJupiter.getInstance() != null)
							AireJupiter.getInstance().updateCallDebugStatus(
									false, "\n>Conf " + address);
						Log.d("voip.inviteConf1 " + address + " " + content);
						AireJupiter.getInstance().tcpSocket()
								.send(address, content, 0, null, null, 0, null);
					}
				}
			}
		}
	};
}
