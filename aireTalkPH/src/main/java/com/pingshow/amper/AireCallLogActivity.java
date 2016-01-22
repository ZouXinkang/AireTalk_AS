package com.pingshow.amper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AireCallLogDB;

public class AireCallLogActivity extends Activity {
	static public String myIMEI;
	public static ProgressDialog progressDialog;
	private ListView mListView;
	private AireCallLogDB mCLDB;
	
	private QueryThreadHandler mThreadQueryHandler;
	public CallLogAdapter mCursorAdapter;
	public Cursor mCursor;
	private ContactsQuery cq;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calllog_page);
		
		this.overridePendingTransition(R.anim.push_up_in, R.anim.freeze);
		mListView = (ListView)findViewById(R.id.list);
		
		mCLDB=new AireCallLogDB(this);
		mCLDB.open();
		
		cq = new ContactsQuery(this);
		
		((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		((ImageView)findViewById(R.id.delete)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if (mCursor.getCount()==0) return;
				
				CharSequence[] d = new CharSequence[2];
				
				d[0]=getResources().getString(R.string.delete_all_call_log);
				d[1]=getResources().getString(R.string.cancel);
				
				new AlertDialog.Builder(AireCallLogActivity.this)
                .setTitle(getString(R.string.delete_confirm))
                .setItems(d, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    	if (which==0)
                        {
                        	new AlertDialog.Builder(AireCallLogActivity.this)
                            .setTitle(R.string.delete_confirm)
                            .setMessage(R.string.delete_all_call_log)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                	mCLDB.deleteAll();
                                	onCallLogQuery();
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
		});
		
		onCallLogQuery();
		
		mListView.setOnItemLongClickListener(onChooseUserLongClick);
		mListView.setOnItemClickListener(onChooseUserClick);
	}
	
	private OnItemClickListener onChooseUserClick = new OnItemClickListener()
	{
		@Override  
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
		{
			String address=(String)view.getTag();
			if (SipCallActivity.getInstance()!=null)
			{
				SipCallActivity.getInstance().setSelectedAddress(address);
				finish();
			}
		}
	};
	
	long selectedRowID;
	OnItemLongClickListener onChooseUserLongClick = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int position, long id) {
			try{
				String displayname=((TextView)arg1.findViewById(R.id.displayname)).getText().toString();
				
				selectedRowID=id;
				
				CharSequence[] d = new CharSequence[2];
				
				d[0]=getResources().getString(R.string.delete_this_call_log);
				d[1]=getResources().getString(R.string.cancel);
				
				new AlertDialog.Builder(AireCallLogActivity.this)
	                .setTitle(displayname)
	                .setItems(d, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int which) {
	                    	if (which==0)
	                    	{
	                    		mCLDB.delete(selectedRowID);
	                        	onCallLogQuery();
	                        	mCursorAdapter.notifyDataSetChanged();
	                    	}
	                    }
	                })
	                .show();
			}catch(Exception e){}
			return true;
		}
	};

	public void onCallLogQuery() {
		if (mCursor != null && !mCursor.isClosed())
			mCursor.close();
		
		mCursor = mCLDB.fetchAll();
		
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
					mCursorAdapter = new CallLogAdapter(AireCallLogActivity.this, mCursor, cq);
					mListView.setAdapter(mCursorAdapter);
				} else
					mCursorAdapter.changeCursor(mCursor);
			} catch (Exception e) {
				Log.e("airecall onQueryComplete !@#$ " + e.getMessage());
			}
		}

		@Override
		protected void onInsertComplete(int token, Object cookie, Uri uri) {
			onCallLogQuery();
		}

		@Override
		protected void onUpdateComplete(int token, Object cookie, int result) {
			onCallLogQuery();
		}

		@Override
		protected void onDeleteComplete(int token, Object cookie, int result) {
			onCallLogQuery();
		}
	}
	
	class CallLogAdapter extends CursorAdapter implements Filterable{

		private ContactsQuery cq;
		Context mContext;
		private Drawable empty;
		private DQCurrency currency;

		public CallLogAdapter(Context context, Cursor c, ContactsQuery _cq) {
			super(context, c);
			cq=_cq;
			mContext=context;
			currency=new DQCurrency(context);
			empty=context.getResources().getDrawable(R.drawable.bighead);
		}

		public final class ViewBinder {
			public TextView vDisplayName;
			public TextView vAddress;
			public ImageView vPhoto;
			public TextView vCost;
		}

		@Override
		public void bindView(final View view, final Context context, final Cursor c) 
		{
			ViewBinder holder = new ViewBinder();
			
			holder.vDisplayName = (TextView) view.findViewById(R.id.displayname);
			holder.vAddress= (TextView) view.findViewById(R.id.address);
			holder.vPhoto= (ImageView) view.findViewById(R.id.photo);
			holder.vCost=(TextView) view.findViewById(R.id.cost);
			
			long contactId=-1;
			String address=c.getString(2);
			contactId=c.getLong(3);
			
			String displayname=c.getString(c.getColumnIndex("display_name"));
			holder.vDisplayName.setText(displayname);
			
			if (contactId>0)
				holder.vPhoto.setImageDrawable(cq.getPhotoById(mContext, contactId, false));
			else
				holder.vPhoto.setImageDrawable(empty);
			
			String tFormat = DateUtils.formatDateTime(mContext,
					c.getLong(4), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_CAP_AMPM);
			int sec=c.getInt(5);
			if (sec>0)
			{
				holder.vCost.setText(String.format("%1$s $%2$.3f",currency.translate("USD"), c.getFloat(6)));
				holder.vCost.setVisibility(View.VISIBLE);
			}
			else
				holder.vCost.setVisibility(View.INVISIBLE);
			String duration=context.getResources().getString(R.string.call_duration)+" "+DateUtils.formatElapsedTime(sec);
			holder.vAddress.setText(tFormat+" "+duration);
			
			view.setTag(address);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context).inflate(R.layout.contact_cell, null);
		}
	}

	@Override
	protected void onDestroy() {
		if (mCursor != null && !mCursor.isClosed())
			mCursor.close();
		if (mCLDB.isOpen())
			mCLDB.close();
		System.gc();
		System.gc();
		super.onDestroy();
	}
}
