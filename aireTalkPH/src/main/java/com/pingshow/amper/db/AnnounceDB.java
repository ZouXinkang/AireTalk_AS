package com.pingshow.amper.db;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AnnounceDB {
	private static final String Announce_DB_TABLE = "AnnounceDB";
	
	public static final String KEY_ID				= "_id";
	public static final String KEY_IDX				= "idx";
	public static final String KEY_TIME				= "time";
	public static final String KEY_R1				= "r1";
	
	private static final String DATABASE_NAME 		= "ann.db";
	private static final int DATABASE_VERSION 		= 1;
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static final String WT_DB_CREATE =
	    "CREATE TABLE IF NOT EXISTS " + Announce_DB_TABLE + " (" +
	    KEY_ID			+ " INTEGER PRIMARY KEY, " +
	    KEY_IDX			+ " INTEGER UNIQUE, " +
	    KEY_TIME        + " LONG NOT NULL, " +
	    KEY_R1			+ " VCHAR(32) NULL);";
	
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
			db.execSQL("DROP TABLE IF EXISTS " + Announce_DB_TABLE);
	      	onCreate(db);
	    }
	}
	
	public AnnounceDB(Context _context) {
		this.context = _context;
	    mDbHelper = null;
	    mDb = null;
	}
	
	public AnnounceDB open() throws SQLException {
		return open(false);
	}
	
	public boolean isOpen(){
		return mDbHelper!=null && mDb.isOpen();
	}
	
	public synchronized AnnounceDB open(boolean readOnly){
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
	
	public synchronized long insert(int idx) 
	{
		if(!mDb.isOpen()) return -1;
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_IDX, idx);
    	vals.put(KEY_TIME, new Date().getTime());
    	return mDb.insert(Announce_DB_TABLE, null, vals);
	}
	
	public boolean existAlready(int idx) {
		if(!mDb.isOpen()) return false;
		int n=0;
		Cursor c=mDb.query(true, Announce_DB_TABLE,
					null, KEY_IDX + "=" + idx, null,
					null, null, null, null);
		if (c!=null)
			n=c.getCount();
		c.close();
		return (n>0);
	}
	
	public void deleteTable(){
		if(!mDb.isOpen()) return;
		mDb.delete(Announce_DB_TABLE, null, null);
	}
}

