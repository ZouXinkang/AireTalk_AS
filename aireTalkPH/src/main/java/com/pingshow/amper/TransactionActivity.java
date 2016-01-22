package com.pingshow.amper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
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

import com.pingshow.amper.db.TransactionDB;

public class TransactionActivity extends Activity {
	static public String myIMEI;
	public static ProgressDialog progressDialog;
	private ListView mListView;
	private TransactionDB mTDB;
	
	private QueryThreadHandler mThreadQueryHandler;
	public TransactionListAdapter mCursorAdapter;
	public Cursor mCursor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.transaction_page);

		this.overridePendingTransition(R.anim.push_up_in, R.anim.freeze);
		mListView = (ListView)findViewById(R.id.list);
		
		mTDB=new TransactionDB(this);
		mTDB.open();
		
		((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		onTransactionQuery();
	}

	public void onTransactionQuery() {
		mCursor = mTDB.getTransactions();
		
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
					mCursorAdapter = new TransactionListAdapter(TransactionActivity.this, mCursor);
					mListView.setAdapter(mCursorAdapter);
				} else
					mCursorAdapter.changeCursor(mCursor);
			} catch (Exception e) {
				Log.e("trans onQueryComplete !@#$ " + e.getMessage());
			}
		}

		@Override
		protected void onInsertComplete(int token, Object cookie, Uri uri) {
			onTransactionQuery();
		}

		@Override
		protected void onUpdateComplete(int token, Object cookie, int result) {
			onTransactionQuery();
		}

		@Override
		protected void onDeleteComplete(int token, Object cookie, int result) {
			onTransactionQuery();
		}
	}
	

	@Override
	protected void onDestroy() {
		if (mCursor != null && !mCursor.isClosed())
			mCursor.close();
		if (mTDB.isOpen())
			mTDB.close();
		System.gc();
		System.gc();
		super.onDestroy();
	}
}
