package com.pingshow.amper.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StudioGroupDB {
	private static final String TABLE_NAME = "StudioGroupDB";
	
	public static final String KEY_ID				= "_id";
	public static final String KEY_NAME				= "name";
	public static final String KEY_CHANNEL			= "channel";
	public static final String KEY_HOT				= "hot";
	public static final String KEY_LOCK				= "lock";
	public static final String KEY_ISO				= "iso";
	public static final String KEY_R1				= "r1";
	public static final String KEY_R2				= "r2";
	public static final String KEY_R3				= "r3";
	
	private static final String DATABASE_NAME 		= "rwtgroups.db";
	private static final int DATABASE_VERSION 		= 3;
	
	static public String [] COMMON_PROJECTION={KEY_ID,KEY_NAME,KEY_CHANNEL,KEY_HOT,KEY_LOCK,KEY_ISO};
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static final String STUDIO_GROUP_DB_CREATE =
	    "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
	    KEY_ID			+ " INTEGER PRIMARY KEY, " +
	    KEY_NAME        + " TEXT NOT NULL,"+
	    KEY_CHANNEL		+ " TEXT NOT NULL,"+
	    KEY_HOT			+ " INTEGER,"+
	    KEY_LOCK		+ " INTEGER,"+
	    KEY_ISO			+ " CHAR(4) NOT NULL,"+
	    KEY_R1			+ " INTEGER, "+
	    KEY_R2			+ " INTEGER, "+
	    KEY_R3			+ " TEXT);";
	
	private final Context context;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	    	db.execSQL(STUDIO_GROUP_DB_CREATE);
	    	db.execSQL("CREATE INDEX "+KEY_CHANNEL+" ON "+TABLE_NAME+" ("+KEY_CHANNEL+")");
	    }

	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
	      	onCreate(db);
	    }
	}
	
	public StudioGroupDB(Context _context) {
		this.context = _context;
	    mDbHelper = null;
	    mDb = null;
	}
	
	public StudioGroupDB open() throws SQLException {
		return open(false);
	}
	
	public boolean isOpen(){
		return mDbHelper!=null && mDb.isOpen();
	}
	
	public synchronized StudioGroupDB open(boolean readOnly){
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
	
	public synchronized long insertGroup(String name, String channel, int hot, int lock, String iso) 
	{
		if(!mDb.isOpen()) return -1;
		boolean found=false;
		Cursor cursor=mDb.query(true, TABLE_NAME, null, KEY_CHANNEL+" = '"+channel+"'", null, null, null, null, null);
		if (cursor!=null)
		{
			if (cursor.moveToFirst())
			{
				//alec: update
				found=true;
				ContentValues vals = new ContentValues();
				vals.put(KEY_NAME, name);
				vals.put(KEY_HOT, hot);
				vals.put(KEY_LOCK, lock);
				vals.put(KEY_ISO, iso);
				mDb.update(TABLE_NAME, vals, KEY_CHANNEL+" = '"+channel+"'", null);
			}
			cursor.close();
		}
		if (found) return 1;
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_NAME, name);
    	vals.put(KEY_CHANNEL, channel);
    	vals.put(KEY_HOT, hot);
    	vals.put(KEY_LOCK, lock);
    	vals.put(KEY_ISO, iso);
    	return mDb.insert(TABLE_NAME, null, vals);
	}
	
	static final String [][] isoGrps={
		{"sa","bh","ae","eg","qa","ir","iq","jo","kw","lb","ye","ly","ma","om","sd","sy","tn"},
		{"mx","co","cu","pe","cr","gt","sv","hn","ni","pa","ve","ec","bo","py","uy","cl","gy","sr"},
		{"us","ca"},
		{"gb","ir"},
		{"nz","au"},
		{"nl","fr","de","lu","be"},
		{"tw","cn","hk"},
	};
	
	public synchronized Cursor getGroups(String iso){
		if(!mDb.isOpen()) return null;
		
		boolean inGroup=false;
		String syntax=KEY_ISO +"='"+iso+"'";
		for (int i=0;i<isoGrps.length && !inGroup;i++)
		{
			for (int j=0;j<isoGrps[i].length && !inGroup;j++)
			{
				if (isoGrps[i][j].equals(iso))
				{
					inGroup=true;
					syntax=KEY_ISO +"='"+iso+"'";
					for (int k=0;k<isoGrps[i].length;k++)
					{
						if (isoGrps[i][k].equals(iso)) continue;
							syntax=syntax+" OR "+KEY_ISO +"='"+isoGrps[i][k]+"'";
					}
					break;
				}
			}
		}
		Cursor cursor=mDb.query(TABLE_NAME, COMMON_PROJECTION, syntax, null, null, null, KEY_HOT+" DESC", "1000");
		return cursor;
	}
	
	public synchronized Cursor search(String key){
		if(!mDb.isOpen()) return null;
		Cursor cursor=mDb.query(TABLE_NAME, COMMON_PROJECTION, KEY_NAME+" LIKE '%"+key+"%'", null, null, null, KEY_HOT+" DESC", "1000");
		return cursor;
	}
	
	public void deleteTable(){
		if(!mDb.isOpen()) return;
		mDb.delete(TABLE_NAME, null, null);
	}
	
	public boolean isLocked(String ch){
		if(!mDb.isOpen()) return false;
		boolean locked=false;
		Cursor cursor=mDb.query(TABLE_NAME, new String [] {KEY_LOCK}, KEY_CHANNEL+"='"+ch+"'", null, null, null, null, null);
		if (cursor!=null)
		{
			if (cursor.moveToFirst())
				locked=(cursor.getInt(0)>0);
			cursor.close();
		}
		return locked;
	}
}

