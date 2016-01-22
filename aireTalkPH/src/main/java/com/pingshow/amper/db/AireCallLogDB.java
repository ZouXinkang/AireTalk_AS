package com.pingshow.amper.db;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AireCallLogDB {
	private static final String TABLE_NAME = "calllog";
	
	public static final String KEY_ID				= "_id";
	public static final String KEY_NAME				= "display_name";
	public static final String KEY_ADDRESS			= "address";
	public static final String KEY_CONTACTID		= "contact_id";
	public static final String KEY_TIME				= "time";
	public static final String KEY_DURATION			= "duration";
	public static final String KEY_COST				= "cost";
	public static final String KEY_R1				= "r1";
	public static final String KEY_R2				= "r2";
	public static final String KEY_R3				= "r3";
	
	private static final String DATABASE_NAME 		= "calllog.db";
	private static final int DATABASE_VERSION 		= 3;
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static final String TABLE_CREATE =
	    "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
	    KEY_ID			+ " INTEGER PRIMARY KEY, " +
	    KEY_NAME        + " TEXT, "+
	    KEY_ADDRESS		+ " TEXT NOT NULL, " +
	    KEY_CONTACTID	+ " LONG, " +
	    KEY_TIME        + " LONG NOT NULL, " +
	    KEY_DURATION	+ " INTEGER, "+
	    KEY_COST		+ " FLOAT, " +
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
	    	db.execSQL(TABLE_CREATE);
	    }

	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
	      	onCreate(db);
	    }
	}
	
	public AireCallLogDB(Context _context) {
		this.context = _context;
	    mDbHelper = null;
	    mDb = null;
	}
	
	public AireCallLogDB open() throws SQLException {
		return open(false);
	}
	
	public boolean isOpen(){
		return mDbHelper!=null && mDb.isOpen();
	}
	
	public synchronized AireCallLogDB open(boolean readOnly){
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
	
	public synchronized long insert(String name, String address, long contact_id) 
	{
		if(!mDb.isOpen()) return -1;
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_NAME, name);
    	vals.put(KEY_ADDRESS, address);
    	vals.put(KEY_CONTACTID, contact_id);
    	vals.put(KEY_COST, 0.f);
    	vals.put(KEY_DURATION, 0);
    	vals.put(KEY_TIME, new Date().getTime());
    	
    	return mDb.insert(TABLE_NAME, null, vals);
	}
	
	public synchronized int update(long rowid, float cost, int duration)
	{
		ContentValues vals = new ContentValues();
    	vals.put(KEY_COST, cost);
    	vals.put(KEY_DURATION, duration);
		return mDb.update(TABLE_NAME, vals, KEY_ID+"="+rowid, null);
	}
	
	public synchronized int update(long rowid, float cost)
	{
		ContentValues vals = new ContentValues();
    	vals.put(KEY_COST, cost);
		return mDb.update(TABLE_NAME, vals, KEY_ID+"="+rowid, null);
	}
	
	public synchronized int delete(long rowid){
		if (mDb.delete(TABLE_NAME, KEY_ID + "=" + rowid, null) > 0)
			return 1;
		return 0;
	}
	
	public synchronized int deleteAll(){
		if (mDb.delete(TABLE_NAME, null, null) > 0)
			return 1;
		return 0;
	}
	
	public synchronized Cursor fetch(){
		if(!mDb.isOpen()) return null;
		Cursor cursor=mDb.query(TABLE_NAME, null, null, null, KEY_ADDRESS, null, KEY_TIME + " desc", "15");
		return cursor;
	}
	
	public synchronized Cursor fetchAll(){
		if(!mDb.isOpen()) return null;
		Cursor cursor=mDb.query(TABLE_NAME, null, null, null, null, null, KEY_TIME + " desc");
		return cursor;
	}
	
	public synchronized Cursor fetchLike(String key){
		if(!mDb.isOpen()) return null;
		Cursor cursor=mDb.query(TABLE_NAME, null, KEY_ADDRESS + " LIKE '%"+key+"%'", null, KEY_ADDRESS, null, KEY_TIME + " desc", "15");
		return cursor;
	}
	//tml*** search airecall
	public synchronized Cursor fetchLikeName(String key){
		if(!mDb.isOpen()) return null;
		Cursor cursor=mDb.query(TABLE_NAME, null, KEY_NAME + " LIKE '%"+key+"%'", null, KEY_NAME, null, KEY_TIME + " desc", "15");
		return cursor;
	}
	
	public void deleteTable(){
		if(!mDb.isOpen()) return;
		mDb.delete(TABLE_NAME, null, null);
	}
}

