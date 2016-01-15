package com.pingshow.airecenter.db;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WTHistoryDB {
	private static final String WALKIE_TALKIE_DB_TABLE = "AmpUserDB";
	
	public static final String KEY_ID				= "_id";
	public static final String KEY_ADDRESS			= "address";
	public static final String KEY_TIME				= "time";
	public static final String KEY_IDX				= "idx";
	
	private static final String DATABASE_NAME 		= "wt.db";
	private static final int DATABASE_VERSION 		= 1;
	
	static public String [] COMMON_PROJECTION={KEY_ID,KEY_ADDRESS,KEY_TIME,KEY_IDX};
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static final String WT_DB_CREATE =
	    "CREATE TABLE IF NOT EXISTS " + WALKIE_TALKIE_DB_TABLE + " (" +
	    KEY_ID			+ " INTEGER PRIMARY KEY, " +
	    KEY_ADDRESS		+ " CHAR(32) UNIQUE NOT NULL, " +
	    KEY_TIME        + " LONG NOT NULL, " +
		KEY_IDX			+ " INTEGER);";
	
	private final Context context;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	    	db.execSQL(WT_DB_CREATE);
	    }

	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + WALKIE_TALKIE_DB_TABLE);
	      	onCreate(db);
	    }
	}
	
	public WTHistoryDB(Context _context) {
		this.context = _context;
	    mDbHelper = null;
	    mDb = null;
	}
	
	public WTHistoryDB open() throws SQLException {
		return open(false);
	}
	
	public boolean isOpen(){
		return mDbHelper!=null && mDb.isOpen();
	}
	
	public synchronized WTHistoryDB open(boolean readOnly){
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
	
	public synchronized Cursor fetchAll(int num) throws SQLException {
		if(!mDb.isOpen()) return null;
		Cursor cursor = mDb.query(true, WALKIE_TALKIE_DB_TABLE,
					COMMON_PROJECTION, null, null,
					null, null, null, ""+num);
		return cursor;
	}
	
	public synchronized Cursor fetchRecent(int num) throws SQLException {
		if(!mDb.isOpen()) return null;
		Cursor cursor = mDb.query(true, WALKIE_TALKIE_DB_TABLE,
					COMMON_PROJECTION, null, null,
					null, null, KEY_TIME + " desc", ""+num);
		return cursor;
	}
	
	public synchronized long insert(String address, int idx) 
	{
		if(!mDb.isOpen()) return -1;
		if (address.length()<6) return -1;
    	if (isInDataBase(address))
    	{
    		ContentValues vals = new ContentValues();
        	vals.put(KEY_TIME, new Date().getTime());
    		return mDb.update(WALKIE_TALKIE_DB_TABLE, vals, KEY_ADDRESS+"='"+address+"'", null);
    	}
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_ADDRESS, address);
    	vals.put(KEY_TIME, new Date().getTime());
    	vals.put(KEY_IDX, idx);
    	return mDb.insert(WALKIE_TALKIE_DB_TABLE, null, vals);
	}
	
	public synchronized int delete(String address) 
	{
		if(!mDb.isOpen()) return -1;
		return mDb.delete(WALKIE_TALKIE_DB_TABLE, KEY_ADDRESS + "='" + address+"'", null);
	}
	
	private boolean isInDataBase(String address) throws SQLException {
		if(!mDb.isOpen()) return false;
		Cursor c=mDb.query(true, WALKIE_TALKIE_DB_TABLE,
					null, KEY_ADDRESS + "='" + address +"'", null,
					null, null, null, null);
		if (c!=null && c.getCount()>0)
		{
			c.close();
			return true;
		}
		c.close();
		return false;
	}
}

