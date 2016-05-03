package com.pingshow.amper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.pingshow.amper.UsersActivity.UserItemAdapter;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.SmsDB;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.voip.AireVenus;

public class MessageActivity extends Activity {
	static public String myIMEI;

	private SmsDB mDB;
	private MyPreference mPref;
	private AmpUserDB mADB;

	public static ProgressDialog progressDialog;
	private ListView mHistoryList;
	private ContactsQuery cq;
	
	private QueryThreadHandler mThreadQueryHandler;
	public MessageThreadAdapter mCursorAdapter;
	public Cursor mCursor;
	
	static public boolean needToBeRefresh;
	
	private boolean largeScreen=false;
	
	static public MessageActivity instance = null;
	static public MessageActivity getInstance() {
		return instance;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_page);
		this.overridePendingTransition(R.anim.freeze, R.anim.freeze);

		neverSayNeverDie(MessageActivity.this);  //tml|bj*** neverdie/

		instance = this;
		mPref = new MyPreference(this);
		cq = new ContactsQuery(this);
		
		((Button)findViewById(R.id.bFafauser)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MessageActivity.this, UsersActivity.class));
				finish();
			}
		});
		
		if (!AmazonKindle.hasMicrophone_NoWarnning(this))
        	((Button)findViewById(R.id.bAireCall)).setVisibility(View.GONE);
		
		((Button)findViewById(R.id.bAireCall)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MessageActivity.this, SipCallActivity.class));
				finish();
			}
		});
		((Button)findViewById(R.id.bSearch)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MessageActivity.this, PublicWalkieTalkie.class));
				finish();
			}
		});
		//tml*** beta ui, conference
		((Button)findViewById(R.id.bConference)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it = new Intent(MessageActivity.this, PickupActivity.class);
				it.putExtra("conference", true);
				startActivity(it);
        		finish();
			}
		});
		//tml*** temp alpha ui, CX
	    ((Button)findViewById(R.id.bSetting)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MessageActivity.this, SettingActivity.class));
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
        
	    ((ImageView)findViewById(R.id.theword)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
//				startActivity(new Intent(MessageActivity.this, ComposeActivity.class));
				int myIdx=0;
				try {
					myIdx=Integer.parseInt(mPref.read("myID","0"),16);
				} catch (Exception e) {}
				
				Intent i = new Intent(MessageActivity.this, TimeLine.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.putExtra("displayname", mPref.read("myNickname","Myself"));
				i.putExtra("address", mPref.read("myPhoneNumber", "----"));
				i.putExtra("Idx", myIdx);
				startActivity(i);			
			}
		});
	    
	    mDB = new SmsDB(this);
		mDB.open();

		mADB = new AmpUserDB(this);
		mADB.open();
		
		mHistoryList = (ListView) findViewById(R.id.history);
		mHistoryList.setBackgroundResource(R.drawable.tiled_bg);
		
		IntentFilter intentToReceiveFilter = new IntentFilter(); 
		intentToReceiveFilter.addAction(Global.Action_MsgGot);
		intentToReceiveFilter.addAction(Global.Action_MsgSent);
		intentToReceiveFilter.addAction(Global.Action_HistroyThread); 
        this.registerReceiver(HandleListChanged, intentToReceiveFilter);

		onThreadsQuery();
		
		needToBeRefresh=false;
	    
		mPref.write("LastPage", 1);
	}

	public void onThreadsQuery() {
		if (!mDB.isOpen())
			return;

		mCursor = mDB.fetchThreads();
		if (mCursor == null) {
			if (mCursorAdapter != null)
				mCursorAdapter.changeCursor(null);
			return;
		}

		if (mThreadQueryHandler == null)
			mThreadQueryHandler = new QueryThreadHandler(getContentResolver());
		try {
			mThreadQueryHandler.startQuery(0, null, CommonDataKinds.Phone.CONTENT_URI, 
					new String [] {CommonDataKinds.Phone.CONTACT_ID}, 
					CommonDataKinds.Phone.CONTACT_ID + "=0", null,
					null);//alec: let it query useless curser to avoid NullException
		} catch (Exception e) {}
	}

	private class QueryThreadHandler extends AsyncQueryHandler {
		public QueryThreadHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor c) {
			try {
				c = mCursor;
				if (mCursorAdapter == null) {
					mCursorAdapter = new MessageThreadAdapter(MessageActivity.this,
							mCursor, mDB, mADB, cq, largeScreen);
					mHistoryList.setAdapter(mCursorAdapter);
					mHistoryList.setOnItemClickListener(OnHistoryClickListener);
					mHistoryList.setOnItemLongClickListener(mLongPressTalkListItem);
				} else
					mCursorAdapter.changeCursor(mCursor);
			} catch (Exception e) {
				Log.e("msg onQueryComplete !@#$ " + e.getMessage());
			}
		}

		@Override
		protected void onInsertComplete(int token, Object cookie, Uri uri) {
			onThreadsQuery();
		}

		@Override
		protected void onUpdateComplete(int token, Object cookie, int result) {
			onThreadsQuery();
		}

		@Override
		protected void onDeleteComplete(int token, Object cookie, int result) {
			onThreadsQuery();
		}
	}

	private long contact_id;
	private String Address;
	private OnItemClickListener OnHistoryClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Cursor c = mDB.getCursorBySMSId(id);
			if (c.moveToFirst()) {
				Address = c.getString(1);// address

				contact_id = cq.getContactIdByNumber(Address);
				String displayname=Address;
				if (contact_id>0)
					displayname=cq.getNameByContactId(contact_id);
				else 
					displayname=mADB.getNicknameByAddress(Address);
				
				TextView tvDis = (TextView) view.findViewById(R.id.displayname);
				String tmp = tvDis.getText().toString();
				int index = tmp.lastIndexOf("(");
				String itemDisname = tmp.substring(0,
						index == -1 ? tmp.length() : index).trim();
//				if (!displayname.equals(itemDisname))
//					mDB.updateDisNameByAddress(Address, displayname);
				if (displayname != null) {  //tml*** addfriend crash
					if (!displayname.equals(itemDisname))
						mDB.updateDisNameByAddress(Address, displayname);
				}

				Intent intent = new Intent(MessageActivity.this,
						ConversationActivity.class);

				intent.putExtra("ActivityType", 1);
				intent.putExtra("SendeeNumber", Address);
				intent.putExtra("SendeeContactId", contact_id);
				intent.putExtra("SendeeDisplayname", displayname);

				//jack 16/5/3 将头像路径传入ConversationActivity,解决近期聊天界面显示的bug
				String photopath = Global.SdcardPath_inbox+"photo_"+mADB.getIdxByAddress(Address)+".jpg";
				intent.putExtra("photopath", photopath);

				startActivity(intent);

				TextView unread = (TextView) view.findViewById(R.id.unread);
				if (unread.getVisibility() == View.VISIBLE) {
					mCursorAdapter.notifyDataSetChanged();
					NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					mNM.cancel(R.string.app_name);
				}
			}
			if (c != null && !c.isClosed())
				c.close();
		}
	};
	
	private AdapterView.OnItemLongClickListener mLongPressTalkListItem 
		= new AdapterView.OnItemLongClickListener() { 
		@Override 
		public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
			if (!mDB.isOpen()) return false;
			Cursor c=mDB.getCursorBySMSId(id);
			if (c.moveToFirst())
			{
				Address = c.getString(1);// address
				contact_id=cq.getContactIdByNumber(Address);
				String displayname=Address;
				if (contact_id>0)
					displayname=cq.getNameByContactId(contact_id);
				else 
					displayname=mADB.getNicknameByAddress(Address);
				
				CharSequence[] d = new CharSequence[2];
				
				d[0]=getResources().getString(R.string.delete_thread);
				d[1]=getResources().getString(R.string.cancel);
				
				new AlertDialog.Builder(MessageActivity.this)
	                .setTitle(displayname)
	                .setItems(d, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int which) {
	                    	if (which==0)
	                        {
	                        	new AlertDialog.Builder(MessageActivity.this)
	                            .setTitle(R.string.delete_confirm)
	                            .setMessage(R.string.delete_thread_confirm)
	                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	                                public void onClick(DialogInterface dialog, int whichButton) {
	                                	mDB.deleteThreadByAddress(Address);
	                                	onThreadsQuery();
	                                	mCursorAdapter.notifyDataSetChanged();
	                                }
	                            })
	                            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
	                                public void onClick(DialogInterface dialog, int whichButton) {
	                                }
	                            })
	                            .show();
	                        }
	                    }
	                })
	                .show();
			}
			if(c!=null && !c.isClosed())
				c.close();
			return true;
		}
	};
	
	BroadcastReceiver HandleListChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent){
			//Global.Action_MsgGot or Global.Action_HistroyThread
			if (AireJupiter.notifying) return;
			onThreadsQuery();
			if (mCursorAdapter!=null)
				mCursorAdapter.notifyDataSetChanged();
		}
	};

	@Override
	protected void onDestroy() {
		unregisterReceiver(HandleListChanged);
		if (mCursor != null && !mCursor.isClosed())
			mCursor.close();
		if (mDB != null && mDB.isOpen())
			mDB.close();
		if (mADB != null && mADB.isOpen())
			mADB.close();
		instance = null;
		System.gc();
		System.gc();
		
		mPref.writeLong("last_show_time",new Date().getTime());
		super.onDestroy();
	}
	
	@Override
	public void onPause() {
//		MobclickAgent.onPause(this);
		System.gc();
		System.gc();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		boolean flag = getIntent().getBooleanExtra("fromNotification", false);
		String filename = getIntent().getStringExtra("filename");
		if (flag && filename != null) {
			NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			mNM.cancel(R.string.app_name);
		}
//		MobclickAgent.onResume(this);
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		if (needToBeRefresh) onThreadsQuery();
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
	public  class UserItemAdapter extends BaseAdapter {
		Context icontext;
		int type;

		public UserItemAdapter(Context context, int type) {
			icontext = context;
			this.type=type;
		}
		
		@Override
		public int getCount() {
			return 0;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return null;
		}
		
	}
}
