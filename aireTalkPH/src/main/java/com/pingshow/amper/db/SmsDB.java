package com.pingshow.amper.db;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Handler;

import com.pingshow.amper.Log;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.util.MyTelephony;

public class SmsDB {
	
	private static final String SMS_DB_TABLE = "smsDB";
	
	//_id, thread_id, address, person, date, 
    //protocol, read, status, type, reply_path_present, subject, body, 
	// service_center 
	
	public static final String KEY_SMS_ID		= "_id";
	public static final String KEY_ADDRESS		= "address";
	public static final String KEY_PERSON		= "person";
	public static final String KEY_DATE			= "date";
	public static final String KEY_READ			= "read";
	public static final String KEY_STATUS		= "status";
	public static final String KEY_TYPE			= "type";
	public static final String KEY_SUBJECT		= "subject";
	public static final String KEY_BODY			= "body";
	public static final String KEY_ATTACH_TYPE	= "attach_type";
	public static final String KEY_VMEMO		= "vmemo_path";
	public static final String KEY_IMAGE		= "image_path";
	public static final String KEY_ORG_SMSID	= "org_id";
	public static final String KEY_LONGITUDE	= "longitude";
	public static final String KEY_LATITUDE		= "latitude";
	public static final String KEY_AD_FLAG		= "ad_flag";
	public static final String KEY_DISPLAYNAME	= "display_name";
	public static final String KEY_FAIL_COUNT 	= "fail_count";
	public static final String KEY_OBLIGATE1 	= "obligate1";
	public static final String KEY_OBLIGATE2 	= "obligate2";
	public static final String KEY_OBLIGATE3 	= "obligate3";
	public static final String KEY_OBLIGATE4 	= "obligate4";
	public static final String KEY_OBLIGATE5 	= "obligate5";
	public static final String[] COMMON_PROJECTION = new String[] {
		KEY_SMS_ID,
		KEY_ADDRESS,
		KEY_PERSON,
		KEY_DATE,
		KEY_READ,
		KEY_BODY,
		KEY_ATTACH_TYPE,
		KEY_IMAGE,
		KEY_DISPLAYNAME,
		KEY_FAIL_COUNT,
		KEY_VMEMO,
		KEY_OBLIGATE1,
		KEY_OBLIGATE2,
		KEY_OBLIGATE3,
		KEY_OBLIGATE4,
		KEY_OBLIGATE5
    };

	private static final String DATABASE_NAME = "smsdb";
	private static final int DATABASE_VERSION = 2;
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	private ContactsQuery cq;
	
	private static final String SMS_DB_CREATE =
	    "CREATE TABLE IF NOT EXISTS " + SMS_DB_TABLE + " (" +
	    KEY_SMS_ID		+ " INTEGER PRIMARY KEY, " +
	    KEY_ADDRESS		+ " VARCHAR(32) NOT NULL, " +
	    KEY_PERSON		+ " INTEGER NULL, " +
	    KEY_DATE        + " LONG NOT NULL, " +
		KEY_READ        + " INTEGER, " +
		KEY_STATUS		+ " INTEGER, " +
		KEY_TYPE 		+ " INTEGER, " +
		KEY_SUBJECT     + " VARCHAR(32) NULL, " +
		KEY_BODY        + " TEXT NULL, " +
		KEY_ATTACH_TYPE	+ " INTEGER DEFAULT 0, " +
		KEY_VMEMO		+ " VARCHAR(32) NULL, " +
		KEY_IMAGE       + " VARCHAR(32) NULL, " +
		KEY_ORG_SMSID	+ " INTEGER NULL," +
		KEY_LONGITUDE	+ " INTEGER NULL," +
		KEY_LATITUDE	+ " INTEGER NULL, " +
		KEY_AD_FLAG		+ " INTEGER NULL," +
		KEY_DISPLAYNAME + " VARCHAR(64)	NULL,"+
		KEY_FAIL_COUNT + "  INTEGER	NULL,"+
		KEY_OBLIGATE1 + "  VARCHAR(64)	NULL,"+
		KEY_OBLIGATE2 + "  VARCHAR(64)	NULL,"+
		KEY_OBLIGATE3 + "  VARCHAR(64)	NULL,"+
		KEY_OBLIGATE4 + "  INTEGER	NULL,"+
		KEY_OBLIGATE5 + "  INTEGER	NULL);";
	private final Context context;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	    	Log.d("SMSDB: Creating Database");
	    	db.execSQL(SMS_DB_CREATE);
	    }

	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d("SMSDB: Upgrading Database");
			db.execSQL("DROP TABLE IF EXISTS " + SMS_DB_TABLE);
	      	onCreate(db);
	    }
	}
	
	public boolean isDatabaseExits()
	{
		try{
			if (!mDb.isOpen()) return false;
			Cursor c=mDb.rawQuery("SELECT * FROM " + SMS_DB_TABLE + " LIMIT 0,1", null);
			if (c!=null)
			{
				if (c.moveToFirst())
				{
					c.close();
					return true;
				}
				c.close();
			}
		}catch (Exception e) {}
		return false;
	}
	
	
	public SmsDB(Context _context) {
		this.context = _context;
	    mDbHelper = null;
	    mDb = null;
	    cq=new ContactsQuery(_context);
	}
	
	public SmsDB open() throws SQLException {
		return open(false);
	}
	public boolean isOpen(){
		return mDbHelper!=null && mDb.isOpen();
	}
	public SmsDB open(boolean readOnly) throws SQLException {
		if (mDbHelper == null) {
			mDbHelper = new DatabaseHelper(context);
			try {
				if (readOnly) {
					mDb = mDbHelper.getReadableDatabase();
				} else {
					mDb = mDbHelper.getWritableDatabase();
				}
			} catch (Exception e) {}
		}
		return this;
	}
	
	public void close() {
		if (mDbHelper != null) {
			mDbHelper.close();
			mDbHelper = null;
		}
	}
	
	public synchronized long insertMessage(
			String address, long person, long date,
			int read, int status, int type, String subject,	String body,
			int attach_type, String vmemo, String img_path, int sms_org_id, 
			long longitude, long latitude, int ad_flag, String displayname, String obligate1_phpIP, int group_member) {
		
		if(!mDb.isOpen()) return -1;
		Log.w("msg.SMSDB insertMessage " + address + " " + displayname + " " + body + " " + attach_type + " " + sms_org_id);
    	ContentValues vals = new ContentValues();
    	address = MyTelephony.cleanPhoneNumber(address);
    	vals.put(KEY_ADDRESS, address);
    	vals.put(KEY_DATE, date);
    	vals.put(KEY_READ, read);
    	vals.put(KEY_STATUS, status);
    	vals.put(KEY_TYPE, type);
    	vals.put(KEY_SUBJECT, subject);
    	vals.put(KEY_BODY, body);
    	vals.put(KEY_ATTACH_TYPE, attach_type);
    	vals.put(KEY_VMEMO, vmemo);
    	vals.put(KEY_IMAGE, img_path);
    	vals.put(KEY_ORG_SMSID, sms_org_id);
    	vals.put(KEY_LONGITUDE, longitude);
    	vals.put(KEY_LATITUDE, latitude);
    	vals.put(KEY_AD_FLAG, ad_flag);
    	vals.put(KEY_FAIL_COUNT,0);
    	vals.put(KEY_OBLIGATE1,obligate1_phpIP);
    	vals.put(KEY_AD_FLAG,group_member);
    	if (displayname==null && cq!=null)
    	{
    		long contactid=cq.getContactIdByNumber(address);
			if (contactid>0)
				displayname=cq.getNameByContactId(contactid);
			vals.put(KEY_PERSON, contactid);
    	}
    	else
    		vals.put(KEY_PERSON, person);
    	
		vals.put(KEY_DISPLAYNAME, displayname);

		return mDb.insert(SMS_DB_TABLE, null, vals);
	}

	public synchronized boolean deleteMessage(long contactId) {
		return deleteMessage(contactId, true);
	}

	public synchronized boolean deleteMessage(long contactId, boolean showToast) {
		if(!mDb.isOpen()) return false;
		if (mDb.delete(SMS_DB_TABLE, KEY_SMS_ID + "=" + String.valueOf(contactId), null) > 0) {
	    	return true;
	    }
	    return false;
	}
	
	public Cursor fetchMessages(String address,int index) throws SQLException {
		if(!mDb.isOpen()) return null;
		boolean found = false;
			Cursor mCursor =
			mDb.query(true, SMS_DB_TABLE,
					null,
					KEY_ADDRESS + "='" + address+"'", null,
					null, null, KEY_SMS_ID+" desc", "0,"+index+"");
		if (mCursor != null) {
			found = mCursor.moveToFirst();
		}
		if (!found) {
			if (mCursor != null) {
				mCursor.close();
			}
			return null;
		}
		return mCursor;
	}

	public Cursor fetchMessageByStatus(int status)
	{
		if (!mDb.isOpen()) return null;
		Cursor mCursor =
			mDb.query(true, SMS_DB_TABLE,
					null,
					KEY_STATUS + "=" + status +" AND type=2", null,
					null, null, null, null);
		return mCursor;
	}
	
	public int getThreadCountByAddress(String address) throws SQLException {
		if (!mDb.isOpen()) return 0;
		int count=0;
		Cursor cursor = mDb.query(true, SMS_DB_TABLE,
					null,
					KEY_ADDRESS + "='" + address+"'", null,
					null, null, null, null);
		if (cursor != null) {
			count = cursor.getCount();
			cursor.close();
		}
		return count;
	}
	
	public int getUnreadCountByAddress(String address) throws SQLException {
		if (!mDb.isOpen()) return 0;
		int count=0;
		Cursor cursor = mDb.query(true, SMS_DB_TABLE,
					null,
					KEY_ADDRESS + "='" + address+"' AND "+ KEY_READ+"=0", null,
					null, null, null, null);
		if (cursor != null) {
			count = cursor.getCount();
			cursor.close();
		}
		return count;
	}
	
	private String mAddress;
	public synchronized void setMessageReadByAddress(Context context, String address) throws SQLException 
	{
		if(!mDb.isOpen()) return;
		Handler mHandler= new Handler();
		mAddress=address;
		mHandler.post(new Runnable() {			
			@Override
			public void run() {
				if(!mDb.isOpen()) return;
				final ContentValues values1 = new ContentValues();
				values1.put(KEY_READ, 1);		
				mDb.update(SMS_DB_TABLE, values1, KEY_ADDRESS + "='" + mAddress +"' AND "+ KEY_READ+"=0", null);
			}
		});
	}
	
	public Cursor getCursorBySMSId(long sms_id) throws SQLException 
	{
		return mDb.query(true, SMS_DB_TABLE,
					COMMON_PROJECTION,
					KEY_SMS_ID + "=" + sms_id, null,
					null, null, null, null);
	}
	
	public synchronized void setMessageSentById(long rowid, int status, long serverTime) throws SQLException 
	{
		if(!mDb.isOpen()) return;
		ContentValues values1 = new ContentValues();
		values1.put(KEY_STATUS, status);
		values1.put(KEY_DATE, serverTime);
		mDb.update(SMS_DB_TABLE, values1, KEY_SMS_ID + "=" + rowid, null);
	}
	public synchronized int setMessageBodyById(long rowid, int attached, String newBody,String vememo) throws SQLException 
	{
		if(!mDb.isOpen()) return -1;
		ContentValues values = new ContentValues();
		values.put(KEY_BODY, newBody);
		values.put(KEY_ATTACH_TYPE,attached);
		values.put(KEY_VMEMO,vememo);
		return mDb.update(SMS_DB_TABLE, values, KEY_SMS_ID + "=" + rowid, null);
	}
	public Cursor fetchThreads() {
		if (!mDb.isOpen()) return null;
	    return mDb.query(SMS_DB_TABLE,
	    		COMMON_PROJECTION, null, null, KEY_ADDRESS, null, KEY_DATE +" desc");
	}
	
	public synchronized void deleteThreadByAddress(String address) throws SQLException {
		if (!mDb.isOpen()) return;
		Cursor c = mDb.query(true, SMS_DB_TABLE,
				null,
				KEY_ADDRESS + "='" + address +"'", null,
				null, null, null, null);
		if (c.moveToFirst()) {
			do{
				int att=c.getInt(c.getColumnIndex(KEY_ATTACH_TYPE));
				if (att!=0 && att<8)
				{
					String path=c.getString(c.getColumnIndex(KEY_VMEMO));
					if (path!=null)
					{
						File file = new File(path); 
						file.delete();
					}
					path=c.getString(c.getColumnIndex(KEY_IMAGE));
					if (path!=null)
					{
						File file = new File(path); 
						file.delete(); 
					}
				}
			}
			while (c.moveToNext());
		}
		c.close();
		
		mDb.delete(SMS_DB_TABLE, KEY_ADDRESS + "='" + address +"'", null);
	}
	
	public void deleteSingleMsg(long sms_id, long org_sms_id) throws SQLException {
		if (!mDb.isOpen()) return;
		Cursor c = mDb.query(true, SMS_DB_TABLE,
				null,
				KEY_SMS_ID + "=" + sms_id, null,
				null, null, null, null);
		if (c.moveToFirst()) {
			int att=c.getInt(c.getColumnIndex(KEY_ATTACH_TYPE));
			if (att!=0 && att<8)
			{
				String path=c.getString(c.getColumnIndex(KEY_VMEMO));
				if (path!=null)
				{
					File file = new File(path); 
					file.delete();
				}
				path=c.getString(c.getColumnIndex(KEY_IMAGE));
				if (path!=null)
				{
					File file = new File(path); 
					file.delete(); 
				}
			}
		}
		c.close();
		
		mDb.delete(SMS_DB_TABLE, KEY_SMS_ID + "=" + sms_id, null);
		
		if (org_sms_id!=0)
			context.getContentResolver().delete(Uri.parse("content://sms"), 
				"_id = "+ org_sms_id, null);
	}
	
	public synchronized int updateDisNameByAddress(String address,String disName){
		if (!mDb.isOpen()) return -1;
	    ContentValues values = new ContentValues();
	    values.put(KEY_DISPLAYNAME, disName);
	    return mDb.update(SMS_DB_TABLE, values, KEY_ADDRESS + "='" + address+"'", null);
	}
	
	public synchronized int updateFailCountById(int _id,int failCount){
		if (!mDb.isOpen()) return -1;
	    ContentValues values = new ContentValues();
	    values.put(KEY_FAIL_COUNT, failCount);
	    return mDb.update(SMS_DB_TABLE, values, KEY_SMS_ID + "=" + _id, null);
	}
	
	public void deleteTable(){
		if(!mDb.isOpen()) return;
		mDb.delete(SMS_DB_TABLE, null, null);
	}
}
