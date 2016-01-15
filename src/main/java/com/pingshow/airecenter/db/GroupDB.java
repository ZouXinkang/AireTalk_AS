package com.pingshow.airecenter.db;

import java.util.ArrayList;
import java.util.Date;

import com.pingshow.airecenter.Log;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class GroupDB {
	private static final String GROUP_DB_TABLE = "GroupDB";
	
	public static final String KEY_ID				= "_id";
	public static final String KEY_GROUPIDX			= "groupidx";
	public static final String KEY_TIME				= "time";
	public static final String KEY_IDX				= "idx";
	public static final String KEY_NAME				= "name";
	public static final String KEY_R1				= "r1";
	public static final String KEY_R2				= "r2";
	public static final String KEY_R3				= "r3";
	
	private static final String DATABASE_NAME 		= "group.db";
	private static final int DATABASE_VERSION 		= 7;
	
	static public String [] COMMON_PROJECTION={KEY_ID,KEY_GROUPIDX,KEY_GROUPIDX,KEY_IDX};
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static final String GROUP_DB_CREATE =
	    "CREATE TABLE IF NOT EXISTS " + GROUP_DB_TABLE + " (" +
	    KEY_ID			+ " INTEGER PRIMARY KEY, " +
	    KEY_GROUPIDX	+ " INTEGER NOT NULL, " +
	    KEY_TIME        + " LONG NOT NULL, " +
	    KEY_IDX			+ " INTEGER, "+
	    KEY_NAME        + " TEXT NOT NULL,"+
	    KEY_R1			+ " INTEGER, "+
	    KEY_R2			+ " INTEGER, "+
	    KEY_R3			+ " TEXT );";
	
	private final Context context;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	    	db.execSQL(GROUP_DB_CREATE);
	    }

	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + GROUP_DB_TABLE);
	      	onCreate(db);
	    }
	}
	
	public GroupDB(Context _context) {
		this.context = _context;
	    mDbHelper = null;
	    mDb = null;
	}
	
	public GroupDB open() throws SQLException {
		return open(false);
	}
	
	public boolean isOpen(){
		return mDbHelper!=null && mDb.isOpen();
	}
	
	public synchronized GroupDB open(boolean readOnly){
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
	
	public synchronized long insertGroup(int groupidx, String name, int idx) 
	{
		if(!mDb.isOpen()) return -1;
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_GROUPIDX, groupidx);
    	vals.put(KEY_IDX, idx);
    	vals.put(KEY_TIME, new Date().getTime());
    	vals.put(KEY_NAME, name);
		Log.w("addG.mGDB insertGroup=" + groupidx + " " + name + " " + idx);
    	return mDb.insert(GROUP_DB_TABLE, null, vals);
	}
	
	public synchronized String getGroupNameByGroupIdx(int groupidx) throws SQLException {
		if(!mDb.isOpen()) return null;
		String name="";
		Cursor cursor=mDb.query(true, GROUP_DB_TABLE,
				null, KEY_GROUPIDX + "=" + groupidx, null,
				null, null, null, null);
		if (cursor!=null)
		{
			if (cursor.moveToFirst())
				name=cursor.getString(2);
			cursor.close();
		}
		Log.w("addG.mGDB getGroupNameByGroupIdx=" + groupidx + " " + name);
		return name;
	}
	
	public synchronized ArrayList<String> getGroupMembersByGroupIdx(int groupidx) throws SQLException {
		if(!mDb.isOpen()) return null;
		ArrayList<String> list = new ArrayList<String>();
		Cursor cursor=mDb.query(true, GROUP_DB_TABLE,
				null, KEY_GROUPIDX + "=" + groupidx, null,
				null, null, null, null);
		if (cursor!=null)
		{
			while (cursor.moveToNext())
			{
				list.add(cursor.getString(3));
			}
			cursor.close();
		}
		String lmembers = "";
		for (int i = 0; i < list.size(); i++) {
			lmembers = lmembers + "," + list.get(i);
		}
		Log.w("addG.mGDB getGroupMembersByGroupIdx=" + groupidx + " " + lmembers);
		return list;
	}
	
	public synchronized int getGroupMemberCount(int groupidx){
		if(!mDb.isOpen()) return -1;
		int num=0;
		Cursor cursor=mDb.query(true, GROUP_DB_TABLE,
				null, KEY_GROUPIDX + "=" + groupidx, null,
				null, null, null, null);
		if (cursor!=null)
		{
			while (cursor.moveToNext())
			{
				num++;
			}
			cursor.close();
		}
		Log.w("addG.mGDB getGroupMemberCount=" + num);
		return num;
	}
	
	public synchronized boolean deleteGroup(int groupidx) 
	{	
		Log.w("addG.mGDB deleteGroup " + groupidx);
		if(!mDb.isOpen()) return false;
		if (mDb.delete(GROUP_DB_TABLE, KEY_GROUPIDX + "=" + groupidx, null) > 0)
	    	return true;
	    return false;
	}
	
	public synchronized boolean deleteGroupMember(int groupidx, int idx) 
	{	
		Log.w("addG.mGDB deleteGroupMember " + groupidx + " " + idx);
		if(!mDb.isOpen()) return false;
		if (mDb.delete(GROUP_DB_TABLE, KEY_GROUPIDX + "=" + groupidx +" AND "+KEY_IDX+" = "+idx, null) > 0)
	    	return true;
	    return false;
	}
	
	public synchronized int getGroupCount(){
		if(!mDb.isOpen()) return -1;
		int num=0;
		Cursor cursor=mDb.query(false, GROUP_DB_TABLE, null, null, null, KEY_GROUPIDX, null, null, null);
		if (cursor!=null)
		{
			num=cursor.getCount();
			/*while(cursor.moveToNext())
				Log.d(cursor.getInt(1)+" "+cursor.getInt(3)+" "+cursor.getString(4));*/
			cursor.close();
		}
		Log.w("addG.mGDB getGroupCount=" + num);
		return num;
	}
}

