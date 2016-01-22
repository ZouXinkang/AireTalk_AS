package com.pingshow.amper;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.RelatedUserDB;

public class BlackListActivity extends Activity {
	static public String myIMEI;

	private AmpUserDB mADB;
	private RelatedUserDB mRDB;

	private ListView mBlackList;
	
	private QueryThreadHandler mThreadQueryHandler;
	public BlackListAdapter mCursorAdapter;
	public MergeCursor mCursor;
	
	private String mDisplayname;
	private String mAddress;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.blacklist_page);

		this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

		mADB = new AmpUserDB(this);
		mADB.open();
		
		mRDB = new RelatedUserDB(this);
		mRDB.open();
		mBlackList = (ListView)findViewById(R.id.blacklist);
		
		((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		onBlockedQuery();
	}

	public void onBlockedQuery() {
		
		Cursor [] cursor=new Cursor[2];
		cursor[0] = mADB.fetchBlockedUsers();
		cursor[1] = mRDB.fetchBlockedUsers();
		
		mCursor= new MergeCursor(cursor);
		
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
					mCursorAdapter = new BlackListAdapter(BlackListActivity.this, mCursor);
					mBlackList.setAdapter(mCursorAdapter);
					mBlackList.setOnItemClickListener(unblockUserListener);
				} else
					mCursorAdapter.changeCursor(mCursor);
			} catch (Exception e) {
				Log.e("blacklist onQueryComplete !@#$ " + e.getMessage());
			}
		}

		@Override
		protected void onInsertComplete(int token, Object cookie, Uri uri) {
			onBlockedQuery();
		}

		@Override
		protected void onUpdateComplete(int token, Object cookie, int result) {
			onBlockedQuery();
		}

		@Override
		protected void onDeleteComplete(int token, Object cookie, int result) {
			onBlockedQuery();
		}
	}
	
	OnItemClickListener unblockUserListener=new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			mAddress=(String)view.getTag();
			TextView t=((TextView)view.findViewById(R.id.displayname));
			mDisplayname=t.getText().toString();
			Intent it=new Intent(BlackListActivity.this, CommonDialog.class);
			it.putExtra("msgContent", String.format(getResources().getString(R.string.unblock_this_user),mDisplayname));
			it.putExtra("numItems", 2);
			it.putExtra("ItemCaption0", getString(R.string.unblock));
			it.putExtra("ItemResult0", RESULT_OK);
			it.putExtra("ItemCaption1", getString(R.string.cancel));
			it.putExtra("ItemResult1", RESULT_CANCELED);
			startActivityForResult(it, 40);
		}
	};
	
	@Override  
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		super.onActivityResult(requestCode, resultCode, data);  
		if (requestCode==40)
		{
			if (resultCode==RESULT_OK) {
				mADB.blockUserByAddress(mAddress, 0);
				mRDB.blockUserByAddress(mAddress, 0);
				onBlockedQuery();
				
				UsersActivity.forceRefresh=true;
				Intent it = new Intent(Global.Action_Refresh_Gallery);
				sendBroadcast(it);
			}
		}
	}

	@Override
	protected void onDestroy() {
		if (mCursor != null && !mCursor.isClosed())
			mCursor.close();
		if (mADB != null && mADB.isOpen())
			mADB.close();
		if (mRDB != null && mRDB.isOpen())
			mRDB.close();
		System.gc();
		System.gc();
		super.onDestroy();
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
