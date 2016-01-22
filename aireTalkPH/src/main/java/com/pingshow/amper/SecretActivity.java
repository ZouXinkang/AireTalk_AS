package com.pingshow.amper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pingshow.amper.UsersActivity.UserItemAdapter;
import com.pingshow.amper.UsersActivity.ViewHolder;
import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.GroupDB;
import com.pingshow.amper.db.RelatedUserDB;
import com.pingshow.amper.db.SmsDB;
import com.pingshow.amper.db.WTHistoryDB;
import com.pingshow.amper.view.PhotoGallery;
import com.pingshow.amper.view.SmartScrollView;
import com.pingshow.amper.view.SmartScrollView.SmartScrollViewListener;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.MyUtil;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.voip.AireVenus;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.InputType;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SecretActivity extends Activity {
	
	private MyPreference mPref;
	private Handler mHandler = new Handler();
	private float mDensity = 1.f;
	
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
		mDensity = getResources().getDisplayMetrics().density;
	    
	    String secret = getIntent().getStringExtra("secret");
	    if (secret != null) {
			mADB = new AmpUserDB(this);
			mADB.open();
			mRDB = new RelatedUserDB(this);
			mRDB.open();
			mGDB = new GroupDB(this);
			mGDB.open();
			cq = new ContactsQuery(this);
	    	
	    	if (secret.equals("0"))
	    	{
	    		finish();
	    	}
	    	else if (secret.equals("1"))
	    	{
			    setContentView(R.layout.login_page_new);
	    	}
	    	else if (secret.equals("2"))
	    	{
			    setContentView(R.layout.register_page_new);
			    ((CheckBox) findViewById(R.id.showPassword)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							((EditText) findViewById(R.id.password1)).setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
						} else {
							((EditText) findViewById(R.id.password1)).setInputType(InputType.TYPE_CLASS_TEXT
									| InputType.TYPE_TEXT_VARIATION_PASSWORD);
						}
						((EditText) findViewById(R.id.password1)).setSelection(((EditText) findViewById(R.id.password1)).length());
					}
			    });
	    	}
	    	else if (secret.equals("3"))
	    	{
			    setContentView(R.layout.profile_page_new);
	    	}
	    	else if (secret.equals("4"))
	    	{
			    setContentView(R.layout.message_page_new);
	    	}
	    	else if (secret.equals("5"))
	    	{
			    setContentView(R.layout.user_page_new);
				if (amperList == null) {
					amperList = (List<Map<String, String>>[]) new ArrayList[2];
					amperList[0] = new ArrayList<Map<String, String>>();
					amperList[1] = new ArrayList<Map<String, String>>();
				}

				if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT)
					numColumns = 6;
				
				mainFriendsDrawer = (SlidingDrawer) findViewById(R.id.friendsSlidingDrawer);
				
				friendAdapter[0] = new UserItemAdapter(this, 0);
				friendAdapter[1] = new UserItemAdapter(this, 1);
				
				friendList = (ListView) findViewById(R.id.friendsList);
				friendList.setOnItemClickListener(onChooseUser);
				friendList.setOnItemLongClickListener(onChooseUserLongClick);
				friendList.setAdapter(friendAdapter[1]);
				
				friendGrid = (GridView) findViewById(R.id.friendsGridView);
				friendGrid.setOnItemClickListener(onChooseUser);
				friendGrid.setAdapter(friendAdapter[0]);
				if (numColumns != 4)
					friendGrid.setNumColumns(numColumns);
				
				onFafaUserQuery();
				friendGrid.post(new Runnable() {
					public void run() {
						friendAdapter[0].notifyDataSetChanged();
					}
				});
				friendList.post(new Runnable() {
					public void run() {
						friendAdapter[1].notifyDataSetChanged();
					}
				});
				mainFriendsDrawer.post(new Runnable() {
					public void run() {
						mainFriendsDrawer.open();
					}
				});
	    	}
	    	else if (secret.equals("6"))
	    	{
			    setContentView(R.layout.conversation_new);
			    ImageView more = (ImageView) findViewById(R.id.more);
			    more.setOnClickListener(new OnClickListener() {
					@SuppressLint("NewApi") @Override
					public void onClick(View v) {
						float mDensity = getResources().getDisplayMetrics().density;
						RelativeLayout input = (RelativeLayout) findViewById(R.id.inputFrameLayout);
						LinearLayout smore = (LinearLayout) findViewById(R.id.functions);
						
						if (smore.getVisibility() == View.GONE) {
							RelativeLayout.LayoutParams params;
							params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, (int) (50 * mDensity));
							params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
							params.addRule(RelativeLayout.ABOVE, R.id.functions);
							input.setLayoutParams(params);
							smore.setVisibility(View.VISIBLE);
						} else {
							RelativeLayout.LayoutParams params;
							params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, (int) (50 * mDensity));
							params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
							params.removeRule(RelativeLayout.ABOVE);
							input.setLayoutParams(params);
							smore.setVisibility(View.GONE);
						}
					}
				});
	    	}
	    	else if (secret.equals("7"))
	    	{
			    setContentView(R.layout.conference_page_new);
	    	}
	    	else if (secret.equals("8"))
	    	{
			    setContentView(R.layout.setting_page_new);
	    	}
	    	else if (secret.equals("9"))
	    	{
	    		//xwf
//			    setContentView(R.layout.search_page_new);
			    setContentView(R.layout.search_page);
	    	}
	    	else if (secret.equals("10"))
	    	{
			    setContentView(R.layout.secret_page);
	    	}
	    	else if (secret.equals("11"))
	    	{
			    setContentView(R.layout.secret_page);
	    	}
	    	else if (secret.equals("12"))
	    	{
			    setContentView(R.layout.secret_page);
	    	}
	    	else if (secret.equals("13"))
	    	{
			    setContentView(R.layout.secret_page);
	    	}
	    	else if (secret.equals("14"))
	    	{
			    setContentView(R.layout.secret_page);
	    	}
	    	else if (secret.equals("15"))
	    	{
			    setContentView(R.layout.secret_page);
	    	}
	    	else if (secret.equals("16"))
	    	{
			    setContentView(R.layout.secret_page);
	    	}
	    	else
	    	{
	    		finish();
	    	}
	    } else {
		    requestWindowFeature(Window.FEATURE_NO_TITLE);
		    setContentView(R.layout.secret_page);
		    
		    if (MyUtil.CheckServiceExists(SecretActivity.this, "com.pingshow.voip.AireVenus")) {
				Log.e("!!! STOPPING AireVenus/ServiceY *** SecretActivity @ ");
				Intent VoipIntent = new Intent(SecretActivity.this, AireVenus.class);
				stopService(VoipIntent);
			}
		    mPref = new MyPreference(this);
		    
		    ((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
				}
			});
		    
		    final ToggleButton tb1 = (ToggleButton) findViewById(R.id.enable_opus_16k);
		    tb1.setChecked(mPref.readBoolean("enable_opus_16k", true));
		    tb1.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPref.write("enable_opus_16k", tb1.isChecked());
				}
			});
		    
		    final ToggleButton tb2 = (ToggleButton) findViewById(R.id.enable_opus_8k);
		    tb2.setChecked(mPref.readBoolean("enable_opus_8k", true));
		    tb2.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPref.write("enable_opus_8k", tb2.isChecked());
				}
			});
		    
		    final ToggleButton tb11 = (ToggleButton) findViewById(R.id.enable_tls);
		    final ToggleButton tb12 = (ToggleButton) findViewById(R.id.enable_udp);
		    
		    tb11.setChecked(mPref.readBoolean("enable_tls", true));
		    tb12.setChecked(mPref.readBoolean("enable_udp", false));
		    
		    tb12.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					tb11.setChecked(!tb12.isChecked());
						mPref.write("enable_tls", tb11.isChecked());
						mPref.write("enable_udp", !tb11.isChecked());
				}
			});
		    
		    tb11.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					tb12.setChecked(!tb11.isChecked());
						Log.e(" tls isChecked()==="+tb11.isChecked());
						mPref.write("enable_tls", !tb12.isChecked());
						mPref.write("enable_udp", tb12.isChecked());
				}
			});
		    
		    final ToggleButton tb3 = (ToggleButton) findViewById(R.id.enable_speex);
		    tb3.setChecked(mPref.readBoolean("enable_speex", true));
		    tb3.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPref.write("enable_speex", tb3.isChecked());
				}
			});
		    
		    final ToggleButton tb4 = (ToggleButton) findViewById(R.id.enable_double_audio);
		    tb4.setChecked(mPref.readBoolean("enable_double_audio", false));
		    tb4.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPref.write("enable_double_audio", tb4.isChecked());
				}
			});
		    
		    final ToggleButton tb5 = (ToggleButton) findViewById(R.id.enable_jitter_control);
		    tb5.setChecked(mPref.readBoolean("enable_jitter_buffer", false));
		    tb5.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPref.write("enable_jitter_buffer", tb5.isChecked());
				}
			});
		    
		    final ToggleButton tb6 = (ToggleButton) findViewById(R.id.enable_jitter_compensation);
		    tb6.setChecked(mPref.readBoolean("enable_jitter_compensation", false));
		    tb6.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPref.write("enable_jitter_compensation", tb6.isChecked());
				}
			});
		    
		    final ToggleButton tb7 = (ToggleButton) findViewById(R.id.enable_antijitter);
		    tb7.setChecked(mPref.readBoolean("enable_antijitter", true));
		    tb7.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPref.write("enable_antijitter", tb7.isChecked());
				}
			});
		    
		    final ToggleButton tb8 = (ToggleButton) findViewById(R.id.flush_audio);
		    tb8.setChecked(mPref.readBoolean("flush_audio", true));
		    tb8.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPref.write("flush_audio", tb8.isChecked());
				}
			});
		    
		    final ToggleButton tb9 = (ToggleButton) findViewById(R.id.enable_ec);
		    tb9.setChecked(mPref.readBoolean("enable_ec", true));
		    tb9.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPref.write("enable_ec", tb9.isChecked());
					if (tb9.isChecked()==false)
						mPref.write("enable_dump_raw", false);
				}
			});
		    
		    final ToggleButton tb10 = (ToggleButton) findViewById(R.id.enable_dump_raw);
		    tb10.setChecked(mPref.readBoolean("enable_dump_raw", false));
		    tb10.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPref.write("enable_dump_raw", tb10.isChecked());
				}
			});
	    }
	    
	}
	
	@Override
	protected void onDestroy() {
		String secret = getIntent().getStringExtra("secret");
		if (secret == null) {
			Log.d("broadcast CALL END...");
			Intent it = new Intent(Global.Action_InternalCMD);
			it.putExtra("Command", Global.CMD_CALL_END);
			it.putExtra("immediately", 500);
			it.putExtra("AireCall", false);
			
			if (AireJupiter.getInstance()!=null)
				AireJupiter.getInstance().attemptCall=false;
			
			this.sendBroadcast(it);
		}
		super.onDestroy();
	}
	
	//test *** test *** test *** test *** test
	private AmpUserDB mADB;
	private RelatedUserDB mRDB;
	private GroupDB mGDB;
	private ContactsQuery cq;
	private UserItemAdapter[] friendAdapter = (UserItemAdapter[]) new UserItemAdapter[2];
	private AsyncImageLoader asyncImageLoader;
	private static List<Map<String, String>>[] amperList;
	private ListView friendList;
	private GridView friendGrid;
	private int numColumns = 4;
	private SlidingDrawer mainFriendsDrawer;

	class ViewHolder
	{
		TextView friendName;
		ImageView photoimage;
		TextView mood;
		TextView separator;
	}

	public class UserItemAdapter extends BaseAdapter {
		Context icontext;
		int type;

		public UserItemAdapter(Context context, int type) {
			icontext = context;
			this.type = type;
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
		
		OnClickListener blockCandidate = new OnClickListener(){
			@Override
			public void onClick(View v) {
			}
		};
		
		OnClickListener deleteBlockedUser = new OnClickListener(){
			@Override
			public void onClick(View v) {
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
					convertView = View.inflate(icontext, R.layout.userinfo_cell_new, null);
				else
					convertView = View.inflate(icontext, R.layout.userinfo_cell_2_new, null);
				
				holder.photoimage = (ImageView) convertView.findViewById(R.id.photo);
				holder.friendName = (TextView) convertView.findViewById(R.id.friendname);
				holder.separator = (TextView) convertView.findViewById(R.id.separator);
				holder.mood = (TextView) convertView.findViewById(R.id.mood);
					
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
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
			}
			else if (disname.equals("----"))
			{
				if (type==0)
				{
					holder.photoimage.setVisibility(View.INVISIBLE);
					holder.friendName.setVisibility(View.INVISIBLE);
				}
				else
				{
					holder.photoimage.setVisibility(View.GONE);
					holder.friendName.setVisibility(View.GONE);
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
			}
			
			if (type == 1) {
				if (address.startsWith("[<GROUP>]")) {
					holder.mood.setText(R.string.the_group);
				} else {
					String mood=mADB.getMoodByAddress(address);
					holder.mood.setText(mood);
				}
			}
			
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
			
			return convertView;
		}
	}
	
	@SuppressLint("NewApi") private class MyListDragListener implements OnDragListener
	{
		private String _data;
		
		MyListDragListener(String data) {
			_data = data;
		}
		
		@Override
		public boolean onDrag(View view, DragEvent event) {
			int action = event.getAction();
			switch (action) {
				case DragEvent.ACTION_DRAG_STARTED:
					break;
				case DragEvent.ACTION_DRAG_ENTERED:
					break;
				case DragEvent.ACTION_DRAG_EXITED:
					break;
				case DragEvent.ACTION_DROP:
				case DragEvent.ACTION_DRAG_ENDED:
					friendList.setAdapter(friendAdapter[1]);
					view.setOnDragListener(null);
					Log.e("tml drop " + _data);
					break;
				default: break;
			}
			return true;
		}
	}
	private void newFunctionDialog(int position)
	{
		Map<String, String> map = amperList[0].get(position);
		String Nickname = map.get("displayName");
		String address = map.get("address");
		String mood = mADB.getMoodByAddress(address);
		int status = ContactsOnline.getContactOnlineStatus(address);
		
		float mDensity = getResources().getDisplayMetrics().density;
		Dialog dialog = new Dialog(SecretActivity.this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		View inflateLayout = View.inflate(SecretActivity.this, R.layout.func_dialog_new, null);
		
		if (status > 0) {
			Drawable d = getResources().getDrawable(R.drawable.online_light);
			d.setBounds(0, 0, (int)(13.f*mDensity), (int)(13.f*mDensity));
			SpannableString spannable = new SpannableString("*" + Nickname);
			ImageSpan icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
			spannable.setSpan(icon, 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
			((TextView) inflateLayout.findViewById(R.id.displayname)).setText(spannable);
		} else {
			((TextView) inflateLayout.findViewById(R.id.displayname)).setText(Nickname);
		}
		((TextView) inflateLayout.findViewById(R.id.mood)).setText(mood);
		
		RelativeLayout.LayoutParams dialogParams
				= new RelativeLayout.LayoutParams((int)(205 * mDensity), LayoutParams.WRAP_CONTENT);
		dialog.setContentView(inflateLayout, dialogParams);
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
		dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
	}
	
	OnItemClickListener onChooseUser = new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			newFunctionDialog(position);
		}
	};
	
	private OnItemLongClickListener onChooseUserLongClick = new OnItemLongClickListener()
	{
		@SuppressLint("NewApi") @Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			Map<String, String> map = amperList[0].get(position);
			String nickname = map.get("displayName");
			Log.e("tml long " + nickname);
			
			view.setOnDragListener(new MyListDragListener(nickname));
			ClipData data = ClipData.newPlainText("userdata", nickname);
			DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
			view.startDrag(data, shadowBuilder, view, 0);
			return true;
		}
	};
	
	private synchronized void onFafaUserQuery()
	{
		
		Log.d("onFafaUserQuery");
		
		if (amperList!=null)
		{
			amperList[0].clear();
			amperList[1].clear();
		}
		
		HashMap<String, String> map;
		Cursor [] cursor=(Cursor[])new Cursor[1];  //tml*** new friends
		cursor[0] = mADB.fetchAll();
		
		try{
			int start=1;
			int numRF = 0;  //tml*** new friends
			int MAX_USERS = 300;
			int numColumns = 3;
			
			for (int loop=0;loop<2;loop++)
			{
				if (!cursor[loop].moveToFirst())
					continue;
				
				do{
					String address = cursor[loop].getString(1);
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
					}
					
					map = new HashMap<String, String>();
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
					
					amperList[0].add(map);
					
				}while (cursor[loop].moveToNext() && amperList[0].size() <= MAX_USERS);
				
				if (loop==0)
				{
	
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
			}
		
		}catch(Exception e){}
		
		if(cursor[0]!=null && !cursor[0].isClosed())
			cursor[0].close();
	}

	void addDummyMap(List<Map<String,String>> list, String displayName)
	{
		HashMap<String, String> map;
		map = new HashMap<String, String>();
		map.put("displayName", displayName);
		map.put("address", "-");
		map.put("imagePath", null);
		map.put("contactId", "-20");
		map.put("seperator", "1");
		map.put("actual", "0");
		list.add(map);
	}
	
	
	
	//test *** test *** test *** test *** test
}