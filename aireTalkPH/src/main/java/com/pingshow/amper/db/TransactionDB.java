package com.pingshow.amper.db;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TransactionDB {
	private static final String TRANSACTOIN_DB_TABLE = "TransactionDB";
	
	public static final String KEY_ID				= "_id";
	public static final String KEY_NAME				= "name";
	public static final String KEY_TIME				= "time";
	public static final String KEY_AMOUNT			= "amount";
	public static final String KEY_PAYMENTID		= "payment_id";
	public static final String KEY_PAYKEY			= "pay_key";
	public static final String KEY_APPID			= "app_id";
	public static final String KEY_STATUS			= "status";
	
	public static final String KEY_R1				= "r1";
	public static final String KEY_R2				= "r2";
	public static final String KEY_R3				= "r3";
	
	private static final String DATABASE_NAME 		= "trascation.db";
	private static final int DATABASE_VERSION 		= 2;
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static final String TRANSACTOIN_TABLE_CREATE =
	    "CREATE TABLE IF NOT EXISTS " + TRANSACTOIN_DB_TABLE + " (" +
	    KEY_ID			+ " INTEGER PRIMARY KEY, " +
	    KEY_NAME        + " TEXT NOT NULL,"+
	    KEY_TIME        + " LONG NOT NULL, " +
	    KEY_AMOUNT		+ " TEXT, "+
	    KEY_PAYMENTID	+ " TEXT, " +
	    KEY_PAYKEY		+ " TEXT, " +
	    KEY_APPID		+ " TEXT, " +
	    KEY_STATUS		+ " INTEGER, "+
	    KEY_R1			+ " INTEGER, "+
	    KEY_R2			+ " TEXT, "+
	    KEY_R3			+ " TEXT );";
	
	private final Context context;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	    	db.execSQL(TRANSACTOIN_TABLE_CREATE);
	    }

	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TRANSACTOIN_DB_TABLE);
	      	onCreate(db);
	    }
	}
	
	public TransactionDB(Context _context) {
		this.context = _context;
	    mDbHelper = null;
	    mDb = null;
	}
	
	public TransactionDB open() throws SQLException {
		return open(false);
	}
	
	public boolean isOpen(){
		return mDbHelper!=null && mDb.isOpen();
	}
	
	public synchronized TransactionDB open(boolean readOnly){
		try{
			if (mDbHelper == null) {
				mDbHelper = new DatabaseHelper(context);
				if (readOnly)
					mDb = mDbHelper.getReadableDatabase();
				else
					mDb = mDbHelper.getWritableDatabase();
			}
			return this;
		}catch(Exception e){
			mDbHelper = new DatabaseHelper(context);
			if (readOnly)
				mDb = mDbHelper.getReadableDatabase();
			else
				mDb = mDbHelper.getWritableDatabase();
			return this;
		}
	}
	
	public synchronized void close() {
		if (mDbHelper != null) {
			mDbHelper.close();
			mDbHelper = null;
		}
	}
	
	public synchronized long insert(String name, String amount, String payment_id, String paykey, String app_id) 
	{
		if(!mDb.isOpen()) return -1;
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_NAME, name);
    	vals.put(KEY_AMOUNT, amount);
    	vals.put(KEY_TIME, new Date().getTime());
    	vals.put(KEY_PAYMENTID, payment_id);
    	vals.put(KEY_PAYKEY, paykey);
    	vals.put(KEY_APPID, app_id);
    	vals.put(KEY_STATUS, 0);
    	
    	return mDb.insert(TRANSACTOIN_DB_TABLE, null, vals);
	}
	
	public synchronized int update(long rowid, int status)
	{
		ContentValues vals = new ContentValues();
    	vals.put(KEY_TIME, new Date().getTime());
    	vals.put(KEY_STATUS, status);
		return mDb.update(TRANSACTOIN_DB_TABLE, vals, KEY_ID+"="+rowid, null);
	}
	
	public synchronized int updateByPaymentId(String payment_id, int status)
	{
		ContentValues vals = new ContentValues();
    	vals.put(KEY_TIME, new Date().getTime());
    	vals.put(KEY_STATUS, status);
		return mDb.update(TRANSACTOIN_DB_TABLE, vals, KEY_PAYMENTID+"="+payment_id, null);
	}
	
	public synchronized int updateByPayKey(String paykey, int status)
	{
		ContentValues vals = new ContentValues();
    	vals.put(KEY_TIME, new Date().getTime());
    	vals.put(KEY_STATUS, status);
		return mDb.update(TRANSACTOIN_DB_TABLE, vals, KEY_PAYKEY+"="+paykey, null);
	}
	
	public synchronized Cursor getTransactions(){
		if(!mDb.isOpen()) return null;
		Cursor cursor=mDb.query(TRANSACTOIN_DB_TABLE, null, null, null, null, null, KEY_TIME + " desc", null);
		return cursor;
	}
	
	public synchronized Cursor getPendingTransactions(){
		if(!mDb.isOpen()) return null;
		Cursor cursor=mDb.query(TRANSACTOIN_DB_TABLE, null, KEY_STATUS+" = 0", null, null, null, KEY_TIME + " desc", null);
		return cursor;
	}
	
	public void deleteTable(){
		if(!mDb.isOpen()) return;
		mDb.delete(TRANSACTOIN_DB_TABLE, null, null);
	}
}

