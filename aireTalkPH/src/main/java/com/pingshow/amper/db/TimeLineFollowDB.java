package com.pingshow.amper.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TimeLineFollowDB {
	private static final String TimeLine_Follow_DB_TABLE = "TimeLineFollowDB";
	
	public static final String KEY_POSTID			= "_id";
	public static final String KEY_FOLLOW			= "follow";
	public static final String KEY_WRITER			= "writer";
	public static final String KEY_NAME				= "name";
	public static final String KEY_PERMISSION		= "permission";
	public static final String KEY_TEXT				= "text";
	public static final String KEY_TIME				= "time";
	public static final String KEY_ATTACH			= "attach";
	public static final String KEY_DELETED			= "deleted";
	public static final String KEY_R1				= "r1";
	public static final String KEY_R2				= "r2";
	public static final String KEY_R3				= "r3";
	
	private static final String DATABASE_NAME 		= "tlfollow.db";
	private static final int DATABASE_VERSION 		= 7;
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static final String TL_FOLLOW_DB_CREATE =
	    "CREATE TABLE IF NOT EXISTS " + TimeLine_Follow_DB_TABLE + " (" +
	    KEY_POSTID		+ " INTEGER PRIMARY KEY, " +
	    KEY_FOLLOW		+ " INTEGER NOT NULL, " +
	    KEY_WRITER		+ " INTEGER NOT NULL, " +
	    KEY_NAME		+ " TEXT DEFAULT NULL, " +
	    KEY_PERMISSION	+ " INTEGER DEFAULT 0, " +
	    KEY_TEXT        + " TEXT DEFAULT NULL, " +
	    KEY_TIME        + " LONG NOT NULL, " +
	    KEY_ATTACH  	+ " TEXT DEFAULT NULL, " +
	    KEY_DELETED		+ " INTEGER DEFAULT 0, " +
	    KEY_R1			+ " INTEGER NULL, " +
	    KEY_R2			+ " VCHAR(32) NULL, " +
	    KEY_R3			+ " VCHAR(32) NULL);";
	
	private final Context context;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	    	db.execSQL(TL_FOLLOW_DB_CREATE);
	    }

	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TimeLine_Follow_DB_TABLE);
	      	onCreate(db);
	    }
	}
	
	public TimeLineFollowDB(Context _context) {
		this.context = _context;
	    mDbHelper = null;
	    mDb = null;
	}
	
	public TimeLineFollowDB open() throws SQLException {
		return open(false);
	}
	
	public boolean isOpen(){
		return mDbHelper!=null && mDb.isOpen();
	}
	
	public synchronized TimeLineFollowDB open(boolean readOnly){
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
	
	public void close() {
		if (mDbHelper != null) {
			mDbHelper.close();
			mDbHelper = null;
		}
	}
	
	public Cursor fetch(int postid){
		if(!mDb.isOpen()) return null;
		Cursor cursor=mDb.query(TimeLine_Follow_DB_TABLE, null, KEY_FOLLOW+" = "+postid+" AND "+KEY_DELETED+" = 0", null, null, null, KEY_TIME + " desc", null);
		return cursor;
	}
	
	public synchronized long insert(int post_id, int followId, int idx, String name, int permission, long time, String text, String attach, int deleted) 
	{
		if(!mDb.isOpen()) return -1;
    	Cursor cursor = getCursorByPostId(post_id);
    	int _id;
    	if (cursor.getCount()>0)
    	{
    		cursor.moveToFirst();
    		_id=cursor.getInt(0);
    		cursor.close();
    		ContentValues vals = new ContentValues();
        	vals.put(KEY_FOLLOW, followId);
        	vals.put(KEY_WRITER, idx);
        	vals.put(KEY_NAME, name);
        	vals.put(KEY_PERMISSION, permission);
        	vals.put(KEY_TIME, time);
        	vals.put(KEY_TEXT, text);
        	vals.put(KEY_ATTACH, attach);
        	vals.put(KEY_DELETED, deleted);
    		mDb.update(TimeLine_Follow_DB_TABLE, vals, KEY_POSTID+"="+_id, null);
    		
    		return 0;
    	}
    	if (!cursor.isClosed()) cursor.close();
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_POSTID, post_id);
    	vals.put(KEY_FOLLOW, followId);
    	vals.put(KEY_WRITER, idx);
    	vals.put(KEY_NAME, name);
    	vals.put(KEY_PERMISSION, permission);
    	vals.put(KEY_TIME, time);
    	vals.put(KEY_TEXT, text);
    	vals.put(KEY_ATTACH, attach);
    	vals.put(KEY_DELETED, deleted);
    	
    	return mDb.insert(TimeLine_Follow_DB_TABLE, null, vals);
	}
	
	public synchronized Cursor getCursorByPostId(int post_id) throws SQLException {
		if(!mDb.isOpen()) return null;
		return mDb.query(true, TimeLine_Follow_DB_TABLE,
					null, KEY_POSTID + "=" + post_id, null,
					null, null, null, null);
	}
	
	public void deleteTable(){
		if(!mDb.isOpen()) return;
		mDb.delete(TimeLine_Follow_DB_TABLE, null, null);
	}
	
	public void remove(int post_id) {
		if(!mDb.isOpen()) return;
		Cursor cursor = getCursorByPostId(post_id);
    	if (cursor.getCount()>0)
    	{
    		cursor.close();
    		ContentValues vals = new ContentValues();
        	vals.put(KEY_DELETED, 1);
    		mDb.update(TimeLine_Follow_DB_TABLE, vals, KEY_POSTID+"="+post_id, null);
    	}
	}
	
	public String getNameByIdx(int idx) {
		if(!mDb.isOpen()) return null;
		String name="";
		Cursor cursor=mDb.query(true, TimeLine_Follow_DB_TABLE, new String [] { KEY_NAME }, KEY_WRITER + "=" + idx, null, null, null, null, null);
		if (cursor!=null && cursor.getCount()>0)
    	{
			cursor.moveToFirst();
			name=cursor.getString(0);
    	}
		return name;
	}
}

