package com.pingshow.amper.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TimeLineDB {
	private static final String TimeLine_DB_TABLE = "TimeLineDB";
	
	public static final String KEY_POSTID			= "_id";
	public static final String KEY_HOST				= "host";
	public static final String KEY_WRITER			= "writer";
	public static final String KEY_PERMISSION		= "permission";
	public static final String KEY_TEXT				= "text";
	public static final String KEY_TIME				= "time";
	public static final String KEY_ATTACH			= "attach";
	public static final String KEY_LIKE_LIST		= "like_list";
	public static final String KEY_FOLLOWS			= "follows";
	public static final String KEY_DELETED			= "deleted";
	public static final String KEY_SERVER			= "server";
	public static final String KEY_LOCALHOST		= "local_host";
	public static final String KEY_NAME				= "name";
	public static final String KEY_R1				= "r1";
	public static final String KEY_R2				= "r2";
	public static final String KEY_R3				= "r3";
	
	private static final String DATABASE_NAME 		= "timeline.db";
	private static final int DATABASE_VERSION 		= 20;
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static final String TL_DB_CREATE =
	    "CREATE TABLE IF NOT EXISTS " + TimeLine_DB_TABLE + " (" +
	    KEY_POSTID		+ " INTEGER PRIMARY KEY, " +
	    KEY_HOST		+ " INTEGER NOT NULL, " +
	    KEY_WRITER		+ " INTEGER NOT NULL, " +
	    KEY_PERMISSION	+ " INTEGER DEFAULT 0, " +
	    KEY_TEXT        + " TEXT DEFAULT NULL, " +
	    KEY_TIME        + " LONG NOT NULL, " +
	    KEY_ATTACH  	+ " TEXT DEFAULT NULL, " +
	    KEY_LIKE_LIST	+ " TEXT DEFAULT NULL, " +
	    KEY_FOLLOWS		+ " INTEGER DEFAULT 0, " +
	    KEY_DELETED		+ " INTEGER DEFAULT 0, " +
	    KEY_SERVER   	+ " VCHAR(32) DEFAULT NULL, " +
	    KEY_LOCALHOST	+ " TEXT DEFAULT NULL, " +
	    KEY_NAME		+ " VCHAR(64) DEFAULT NULL, " +
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
	    	db.execSQL(TL_DB_CREATE);
	    }

	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TimeLine_DB_TABLE);
	      	onCreate(db);
	    }
	}
	
	public TimeLineDB(Context _context) {
		this.context = _context;
	    mDbHelper = null;
	    mDb = null;
	}
	
	public TimeLineDB open() throws SQLException {
		return open(false);
	}
	
	public boolean isOpen(){
		return mDbHelper!=null && mDb.isOpen();
	}
	
	public synchronized TimeLineDB open(boolean readOnly){
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
	
	public Cursor fetch(int idx){
		if(!mDb.isOpen()) return null;
		Cursor cursor=mDb.query(TimeLine_DB_TABLE, null, KEY_LOCALHOST+" LIKE '%<"+idx+">%' AND "+KEY_DELETED+" = 0", null, null, null, KEY_POSTID + " desc", null);
		return cursor;
	}
	
	public synchronized long insert(int post_id, int host, int localHost, int idx, String name, int permission, long time, String text, String attach, String likeList, String server, int follows, int deleted) 
	{
		if(!mDb.isOpen()) return -1;
    	Cursor cursor = getCursorByPostId(post_id);
    	int _id;
    	if (cursor.getCount()>0)
    	{
    		cursor.moveToFirst();
    		_id=cursor.getInt(0);
    		String localList=cursor.getString(11);
    		localList=addIDX(localList, ""+localHost);
    		cursor.close();
    		ContentValues vals = new ContentValues();
        	vals.put(KEY_HOST, host);
        	vals.put(KEY_WRITER, idx);
        	vals.put(KEY_PERMISSION, permission);
        	vals.put(KEY_TIME, time);
        	vals.put(KEY_TEXT, text);
        	vals.put(KEY_ATTACH, attach);
        	vals.put(KEY_LIKE_LIST, likeList);
        	vals.put(KEY_FOLLOWS, follows);
        	vals.put(KEY_DELETED, deleted);
        	vals.put(KEY_SERVER, server);
        	vals.put(KEY_LOCALHOST, localList);
        	vals.put(KEY_NAME, name);
    		mDb.update(TimeLine_DB_TABLE, vals, KEY_POSTID+"="+_id, null);
    		
    		return 0;
    	}
    	if (!cursor.isClosed()) cursor.close();
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_POSTID, post_id);
    	vals.put(KEY_HOST, host);
    	vals.put(KEY_WRITER, idx);
    	vals.put(KEY_PERMISSION, permission);
    	vals.put(KEY_TIME, time);
    	vals.put(KEY_TEXT, text);
    	vals.put(KEY_ATTACH, attach);
    	vals.put(KEY_LIKE_LIST, likeList);
    	vals.put(KEY_FOLLOWS, follows);
    	vals.put(KEY_DELETED, deleted);
    	vals.put(KEY_LOCALHOST, "<"+localHost+">");
    	vals.put(KEY_NAME, name);
    	vals.put(KEY_SERVER, server);
    	
    	return mDb.insert(TimeLine_DB_TABLE, null, vals);
	}
	
	public synchronized Cursor getCursorByPostId(int post_id) throws SQLException {
		if(!mDb.isOpen()) return null;
		return mDb.query(true, TimeLine_DB_TABLE,
					null, KEY_POSTID + "=" + post_id, null,
					null, null, null, null);
	}
	
	public void deleteTable(){
		if(!mDb.isOpen()) return;
		mDb.delete(TimeLine_DB_TABLE, null, null);
	}
	
	
	private String addIDX(String src, String idx)
	{
		if (src!=null && src.length()>0)
		{
			boolean found=false;
			String [] sIdxs=src.split(";");
			for (String sIdx:sIdxs)
			{
				if (sIdx.equals(idx))
				{
					found=true;
					break;
				}
			}
			if (!found)
				src=src+";<"+idx+">";
		}	
		else{
			src="<"+idx+">";
		}
		
		return src;
	}
	
	private String addLikes(String src, String idx)
	{
		if (src!=null && src.length()>0)
		{
			boolean found=false;
			String [] sIdxs=src.split(";");
			for (String sIdx:sIdxs)
			{
				if (sIdx.equals(idx))
				{
					found=true;
					break;
				}
			}
			if (!found)
				src=src+";"+idx;
		}	
		else{
			src=idx;
		}
		
		return src;
	}
	
	private String removeLikes(String src, String idx)
	{
		if (src!=null && src.length()>0)
		{
			String [] sIdxs=src.split(";");
			String newList="";
			for (String sIdx:sIdxs)
			{
				if (!sIdx.equals(idx))
				{
					if (newList.length()>0)
						newList=newList+";"+sIdx;
					else
						newList=sIdx;
				}
			}
			return newList;
		}	
		else
			return null;
	}
	
	public void setLikeIt(int post_id, int idx, boolean like) {
		if(!mDb.isOpen()) return;
		Cursor cursor = getCursorByPostId(post_id);
    	if (cursor.getCount()>0)
    	{
    		cursor.moveToFirst();
    		String likeList=cursor.getString(7);
    		cursor.close();
    		ContentValues vals = new ContentValues();
    		if (like)
    		{
    			likeList=addLikes(likeList, ""+idx);
    		}else{
    			likeList=removeLikes(likeList, ""+idx);
    		}
        	vals.put(KEY_LIKE_LIST, likeList);
    		mDb.update(TimeLine_DB_TABLE, vals, KEY_POSTID+"="+post_id, null);
    	}
	}
	
	public void remove(int post_id) {
		if(!mDb.isOpen()) return;
		Cursor cursor = getCursorByPostId(post_id);
    	if (cursor.getCount()>0)
    	{
    		cursor.close();
    		ContentValues vals = new ContentValues();
        	vals.put(KEY_DELETED, 1);
    		mDb.update(TimeLine_DB_TABLE, vals, KEY_POSTID+"="+post_id, null);
    	}
	}
	
	public String getLikeList(int post_id){
		if(!mDb.isOpen()) return null;
		String likeList=null;
		Cursor cursor = getCursorByPostId(post_id);
    	if (cursor.getCount()>0)
    	{
    		cursor.moveToFirst();
    		likeList=cursor.getString(7);
    		
    	}
    	if (cursor!=null)
    		cursor.close();
    	
    	return likeList;
	}
}

