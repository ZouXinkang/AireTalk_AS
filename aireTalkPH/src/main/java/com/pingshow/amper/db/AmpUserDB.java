package com.pingshow.amper.db;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.R;
import com.pingshow.util.MyTelephony;

public class AmpUserDB {
	private static final String AMP_USER_DB_TABLE = "AmpUserDB";
	
	public static final String KEY_ID				= "_id";
	public static final String KEY_ADDRESS			= "address";
	public static final String KEY_TIME				= "time";
	public static final String KEY_IDX				= "idx";
	public static final String KEY_DELETED			= "itphcount";
	public static final String KEY_MOOD				= "mood";
	public static final String KEY_PHTO				= "photo_from_net";
	public static final String KEY_NICKNAME			= "nickname";
	public static final String KEY_BLOCKED			= "blocked";
	public static final String KEY_INVITED			= "invited";
	
	private static final String DATABASE_NAME 		= "ampuser.db";
	private static final int DATABASE_VERSION 		= 2;
	
	static public String [] COMMON_PROJECTION={KEY_ID,KEY_ADDRESS,KEY_TIME,KEY_IDX,KEY_NICKNAME};
	
	static public String [] BLK_PROJECTION={KEY_ID,KEY_ADDRESS,KEY_NICKNAME,KEY_IDX};
	
	static public String [] KEY_BLOCKED_PROJ={KEY_BLOCKED};
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private int myIdx;
	private String myNickname;
	
	private static final String FFY_DB_CREATE =
	    "CREATE TABLE IF NOT EXISTS " + AMP_USER_DB_TABLE + " (" +
	    KEY_ID			+ " INTEGER PRIMARY KEY, " +
	    KEY_ADDRESS		+ " CHAR(32) UNIQUE NOT NULL, " +
	    KEY_TIME        + " LONG NOT NULL, " +
		KEY_IDX			+ " INTEGER, "+
		KEY_DELETED		+ " INTEGER DEFAULT 0, " +
		KEY_MOOD		+ " TEXT NULL, "+
		KEY_PHTO		+ " INTEGER NULL," +
		KEY_NICKNAME	+ " TEXT NULL, " +
		KEY_BLOCKED		+ " INTEGER DEFAULT 0); ";
	
	private final Context context;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	    	db.execSQL(FFY_DB_CREATE);
	    }

	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + AMP_USER_DB_TABLE);
	      	onCreate(db);
	    }
	}
	
	public AmpUserDB(Context _context) {
		this.context = _context;
	    mDbHelper = null;
	    mDb = null;
	    
	    MyPreference pref=new MyPreference(_context);
	    try{
        	myIdx=Integer.parseInt(pref.read("myID","0"),16);
        }catch(Exception e){
        }
	    
	    myNickname=pref.read("myNickname","Myself");
	}
	
	public AmpUserDB open() throws SQLException {
		return open(false);
	}
	
	public boolean isOpen(){
		return mDbHelper!=null && mDb.isOpen();
	}
	
	public synchronized AmpUserDB open(boolean readOnly){
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
	
	public synchronized Cursor fetchAll() throws SQLException {
		if(!mDb.isOpen()) return null;
		Cursor cursor = mDb.query(true, AMP_USER_DB_TABLE,
					COMMON_PROJECTION, KEY_BLOCKED+"=0 AND "+KEY_DELETED+">=0", null,
					null, null, KEY_NICKNAME + " COLLATE LOCALIZED ASC", null);
		return cursor;
	}
	
	public synchronized Cursor fetchAllByTime() throws SQLException {
		if(!mDb.isOpen()) return null;
		Cursor cursor = mDb.query(true, AMP_USER_DB_TABLE,
					COMMON_PROJECTION, KEY_BLOCKED+"=0 AND "+KEY_DELETED+">=0", null,
					null, null, KEY_TIME + " DESC", null);
		return cursor;
	}
	
	public synchronized long insertUser(String address,int idx) 
	{
		return insertUser(address, idx, context.getResources().getString(R.string.unknown_person));
	}
	
	public synchronized long insertStranger(String address, int idx) 
	{
		if(!mDb.isOpen()) return -1;
		if (address.length()<6) return -1;
    	Cursor cursor = getCursorByAddress(address);
    	if (cursor.getCount()>0)
    	{
    		cursor.close();
    		ContentValues vals = new ContentValues();
        	vals.put(KEY_ADDRESS, address);
        	vals.put(KEY_TIME, new Date().getTime());
        	vals.put(KEY_IDX, idx);
        	vals.put(KEY_DELETED, -1);
        	Log.w("addF.mADB insertStranger1=" + address + " " + idx);
    		return mDb.update(AMP_USER_DB_TABLE, vals, KEY_ADDRESS+"='"+address+"'", null);
    	}
    	if (!cursor.isClosed()) cursor.close();
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_ADDRESS, address);
    	vals.put(KEY_TIME, new Date().getTime());
    	vals.put(KEY_IDX, idx);
    	vals.put(KEY_DELETED, -1);
    	Log.w("addF.mADB insertStranger2=" + address + " " + idx);
    	return mDb.insert(AMP_USER_DB_TABLE, null, vals);
	}
	
	public synchronized long insertUser(String address,int idx, String nickname) 
	{
		if(!mDb.isOpen()) return -1;
		if (address.length()<6) return -1;
    	Cursor cursor = getCursorByAddress(address);
    	if (cursor.getCount()>0)
    	{
    		cursor.close();
    		ContentValues vals = new ContentValues();
        	vals.put(KEY_ADDRESS, address);
        	vals.put(KEY_TIME, new Date().getTime());
        	vals.put(KEY_IDX, idx);
        	vals.put(KEY_DELETED, 0);
        	vals.put(KEY_NICKNAME, nickname);
        	Log.w("addF.mADB insertUser1=" + address + " " + idx + " " + nickname);
    		return mDb.update(AMP_USER_DB_TABLE, vals, KEY_ADDRESS+"='"+address+"'", null);
    	}
    	if (!cursor.isClosed()) cursor.close();
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_ADDRESS, address);
    	vals.put(KEY_TIME, new Date().getTime());
    	vals.put(KEY_IDX, idx);
    	vals.put(KEY_DELETED, 0);
    	vals.put(KEY_NICKNAME, nickname);
    	Log.w("addF.mADB insertUser2=" + address + " " + idx + " " + nickname);
    	return mDb.insert(AMP_USER_DB_TABLE, null, vals);
	}
	
	public synchronized Cursor getCursorByAddress(String address) throws SQLException {
		if(!mDb.isOpen()) return null;
		return mDb.query(true, AMP_USER_DB_TABLE,
					null, KEY_ADDRESS + "='" + address +"'", null,
					null, null, null, null);
	}
	
	public synchronized Cursor getFafaFriends() throws SQLException {
		if(!mDb.isOpen()) return null;
		return mDb.query(true, AMP_USER_DB_TABLE,
					null, KEY_IDX + "!=0 AND "+KEY_DELETED+">=0", null,
					null, null, KEY_TIME + " desc", null);
	}
	
	public synchronized int getCount() throws SQLException {
		int c=0;
		if(!mDb.isOpen()) return 0;
		Cursor cursor=mDb.query(true, AMP_USER_DB_TABLE,
				new String[]{KEY_IDX}, KEY_BLOCKED+"=0 AND "+KEY_DELETED+">=0", null,
				null, null, null, null);
		if (cursor!=null)
		{
			c=cursor.getCount();
			cursor.close();
		}
    	Log.w("addF.mADB getCount=" + c);
		return c;
	}
	
	public synchronized String getAddressByIdx(int idx) throws SQLException {
		String address="";
		if(!mDb.isOpen()) return address;
		Cursor cursor =mDb.query(true, AMP_USER_DB_TABLE,
					null, KEY_IDX + "=" + idx, null,
					null, null, null, null);
		if(cursor.moveToFirst())
			address=cursor.getString(1);
		cursor.close();
		Log.w("addF.mADB getAddressByIdx=" + address + " " + idx);
		return address;
	}
	
	public synchronized int updateLastContactTimeByAddress(String address, long lctime) {
		try{
			if(!mDb.isOpen()) return -1;
			ContentValues vals = new ContentValues();
	    	vals.put(KEY_ADDRESS, address);
	    	vals.put(KEY_TIME, lctime);
		    return mDb.update(AMP_USER_DB_TABLE, vals, KEY_ADDRESS+"='"+address+"'", null);
		}catch(Exception e){}
		return -1;
	}
	
	public synchronized int updateLastContactTimeByIdx(int idx, long lctime) {
		try{
			if(!mDb.isOpen()) return -1;
			ContentValues vals = new ContentValues();
	    	vals.put(KEY_TIME, lctime);
		    return mDb.update(AMP_USER_DB_TABLE, vals, KEY_IDX+"="+idx, null);
		}catch(Exception e){}
		return -1;
	}
	
	public synchronized int markAsDeleted(String address) {
		Log.w("addF.mADB markAsDeleted=" + address);
		if(address==null || address.length()==0) return 0;
		if(!mDb.isOpen()) return 0;
		ContentValues vals = new ContentValues();
    	vals.put(KEY_DELETED, -1);
	    return mDb.update(AMP_USER_DB_TABLE, vals, KEY_ADDRESS+"='"+address+"'", null);
	}
	
	public synchronized int markAsUnDeleted(String address) {
		Log.w("addF.mADB markAsUnDeleted=" + address);
		if(address==null || address.length()==0) return 0;
		if(!mDb.isOpen()) return 0;
		ContentValues vals = new ContentValues();
    	vals.put(KEY_DELETED, 0);
	    return mDb.update(AMP_USER_DB_TABLE, vals, KEY_ADDRESS+"='"+address+"'", null);
	}
	
	public synchronized boolean isUserDeleted(String address) throws SQLException {
		if(!mDb.isOpen()) return false;
		int val=0;
		Cursor cursor = mDb.query(true, AMP_USER_DB_TABLE,
					null, KEY_ADDRESS + "='" + address +"'", null,
					null, null, null, null);
		if (cursor.moveToFirst())
		{
			val=cursor.getInt(4);
		}
		cursor.close();
		Log.w("addF.mADB isUserDeleted=" + address + " " + (val==-1));
		return (val==-1);
	}
	
	public synchronized String getAddressById(long _id) throws SQLException {
		if(!mDb.isOpen()) return "";
		String address="";
		Cursor cursor = mDb.query(true, AMP_USER_DB_TABLE,
					null, KEY_ID + "='" + _id +"'", null,
					null, null, null, null);
		if (cursor.moveToFirst())
		{
			address=cursor.getString(1);
		}
		cursor.close();
		Log.w("addF.mADB getAddressById=" + address + " " + _id);
		return address;
	}
	
	public synchronized String getMoodByAddress(String address) throws SQLException {
		String mood="";
		if(!mDb.isOpen()) return mood;
		Cursor cursor = mDb.query(true, AMP_USER_DB_TABLE,
					new String [] {KEY_MOOD}, KEY_ADDRESS + "='" + address +"'", null,
					null, null, null, null);
		if (cursor.moveToFirst())
			mood=cursor.getString(0);
		cursor.close();
		Log.w("addF.mADB getMoodByAddress=" + address + " " + mood);
		return mood;
	}
	
	public synchronized int getIdxByAddress(String address) throws SQLException {
		int idx = -1;
		if (mDb!=null) {
		if(!mDb.isOpen()) return idx;
		if(address!=null){
			Cursor cursor =mDb.query(true, AMP_USER_DB_TABLE,
					new String [] {KEY_IDX}, KEY_ADDRESS + " = '" + address+"'", null,
					null, null, null, null);
			if(cursor.moveToFirst())
				idx = cursor.getInt(0);
			cursor.close();
		}
		}
		Log.w("addF.mADB getIdxByAddress=" + address + " " + idx);
		return idx;
	}
	
	public synchronized boolean deleteContactByAddress(String address) 
	{
		Log.w("addF.mADB deleteContactByAddress=" + address);
		if(!mDb.isOpen()) return false;
		if (markAsDeleted(address) > 0)
	    	return true;
	    return false;
	}
	
	public boolean isFafauser(String address)
	{
		if(!mDb.isOpen()) return false;
		Cursor cursor = mDb.query(true, AMP_USER_DB_TABLE, null, KEY_BLOCKED+"=0 AND "+KEY_DELETED+">=0", null,null, null, null, null);
		while(cursor.moveToNext())
		{
			if (MyTelephony.SameNumber(cursor.getString(1), address)){
				cursor.close();
				Log.w("addF.mADB isFafauser TRUE " + address);
				return true;
			}
		}
		cursor.close();
		Log.w("addF.mADB isFafauser FALSE " + address);
		return false;
	}
	
	public boolean isFafauser(int idx)
	{
		if(!mDb.isOpen()) return false;
		Cursor cursor = mDb.query(true, AMP_USER_DB_TABLE, null, KEY_IDX+"="+idx+" AND "+KEY_DELETED+">=0", null,null, null, null, null);
		if (cursor!=null)
		{
			if (cursor.getCount()>0)
			{
				cursor.close();
				Log.w("addF.mADB isFafauser TRUE " + idx);
				return true;
			}
			cursor.close();
		}
		Log.w("addF.mADB isFafauser TRUE " + idx);
		return false;
	}
	
	public int getPhotoVersionByAddress(String address)
	{
		if(!mDb.isOpen()) return 0;
		int hasPhoto=0;
		Cursor cursor =mDb.query(true, AMP_USER_DB_TABLE,
				new String [] {KEY_PHTO}, KEY_ADDRESS + " = '" + address+"'", null,
				null, null, null, null);
		if(cursor.moveToFirst())
			hasPhoto = cursor.getInt(0);
		cursor.close();
		Log.w("addF.mADB getPhotoVersionByAddress=" + address + " " + hasPhoto);
		return hasPhoto;
	}
	
	public int getPhotoVersionByIdx(int idx)
	{
		if(!mDb.isOpen()) return 0;
		int hasPhoto=0;
		Cursor cursor =mDb.query(true, AMP_USER_DB_TABLE,
				new String [] {KEY_PHTO}, KEY_IDX + " =" + idx, null,
				null, null, null, null);
		if(cursor.moveToFirst())
			hasPhoto = cursor.getInt(0);
		cursor.close();
		Log.w("addF.mADB getPhotoVersionByIdx=" + idx + " " + hasPhoto);
		return hasPhoto;
	}
	
	public synchronized int updatePhotoByUID(int uid, int version) {
		if(uid==0) return 0;
		if(!mDb.isOpen()) return -1;
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_PHTO, version);
    	Log.w("addF.mADB updatePhotoByUID=" + uid + " " + version);
	    return mDb.update(AMP_USER_DB_TABLE, vals, KEY_IDX+"="+uid, null);
	}
	
	public synchronized int updateMoodByUID(int uid, String moodText) {
		if(!mDb.isOpen()) return -1;
		if(uid==0) return 0;
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_MOOD, moodText);
    	Log.w("addF.mADB updateMoodByUID=" + uid + " " + moodText);
	    return mDb.update(AMP_USER_DB_TABLE, vals, KEY_IDX+"="+uid, null);
	}
	
	public synchronized Cursor fetchPhotoVersion() throws SQLException {
		if(!mDb.isOpen()) return null;
		Cursor cursor = mDb.query(true, AMP_USER_DB_TABLE,
					new String [] {KEY_ADDRESS,KEY_IDX,KEY_PHTO}, null, null,	//simon 061011
					null, null, KEY_TIME + " desc", null);
		return cursor;
	}
	
	public synchronized int updateNicknameByUID(int uid, String nickname) {
		if(!mDb.isOpen()) return -1;
		if(uid<50) return 0;
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_NICKNAME, nickname);
		Log.w("addF.mADB updateNicknameByUID=" + uid + " " + nickname);
	    return mDb.update(AMP_USER_DB_TABLE, vals, KEY_IDX+"="+uid, null);
	}
	
	public String getNicknameByAddress(String address)
	{
		if(!mDb.isOpen()) return "";
		String nickname="";
		Cursor cursor =mDb.query(true, AMP_USER_DB_TABLE,
				new String [] {KEY_NICKNAME }, KEY_ADDRESS + " = '" + address+"'", null,
				null, null, null, null);
		if(cursor.moveToFirst())
			nickname = cursor.getString(0);
		cursor.close();
		Log.w("addF.mADB getNicknameByAddress=" + address + " " + nickname);
		return nickname;
	}
	
	public String getNicknameByIdx(int idx)
	{
		if(!mDb.isOpen()) return "";
		
		if (idx==myIdx)
		{
			return myNickname;
		}
		
		String nickname="";
		Cursor cursor =mDb.query(true, AMP_USER_DB_TABLE,
				new String [] {KEY_NICKNAME }, KEY_IDX + " = " + idx, null,
				null, null, null, null);
		if(cursor.moveToFirst())
			nickname = cursor.getString(0);
		cursor.close();
		Log.w("addF.mADB getNicknameByIdx=" + idx + " " + nickname);
		return nickname;
	}
	
	public synchronized int blockUserByAddress(String address, int block) {
		if(!mDb.isOpen()) return -1;
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_BLOCKED, block);
    	vals.put(KEY_DELETED, 0);
		Log.w("addF.mADB blockUserByAddress=" + address + " " + block);
	    return mDb.update(AMP_USER_DB_TABLE, vals, KEY_ADDRESS+"='"+address+"'", null);
	}
	
	public synchronized int isUserBlocked(String address) {
		int blocked=0;
		if (mDb.isOpen())
		{
			Cursor cursor = mDb.query(true, AMP_USER_DB_TABLE,
					KEY_BLOCKED_PROJ, KEY_ADDRESS + " = '" + address+"'", null,
					null, null, null, null);
			if (cursor.moveToFirst())
			{
				blocked=cursor.getInt(0);
			}
			cursor.close();
		}
		Log.w("addF.mADB isUserBlocked=" + address + " " + blocked);
		return blocked;
	}
	
	public synchronized int isUserBlocked(int idx) {
		int blocked=0;
		if (mDb.isOpen())
		{
			Cursor cursor = mDb.query(true, AMP_USER_DB_TABLE,
					KEY_BLOCKED_PROJ, KEY_IDX + "="+idx, null,
					null, null, null, null);
			if (cursor.moveToFirst())
			{
				blocked=cursor.getInt(0);
				cursor.close();
			}
		}
		Log.w("addF.mADB isUserBlocked=" + idx + " " + blocked);
		return blocked;
	}
	
	public synchronized Cursor fetchBlockedUsers() throws SQLException {
		if(!mDb.isOpen()) return null;
		Cursor cursor = mDb.query(true, AMP_USER_DB_TABLE,
					BLK_PROJECTION, KEY_BLOCKED+"=1", null,
					null, null, KEY_NICKNAME + " COLLATE LOCALIZED ASC", null);
		return cursor;
	}
	
	public void deleteTable(){
		if(!mDb.isOpen()) return;
		mDb.delete(AMP_USER_DB_TABLE, null, null);
	}

	//jack update
	public synchronized boolean updateGroupname(String address,int idx, String nickname){
		if(!mDb.isOpen()) return false;
		if (address.length()<6) return false;
		ContentValues vals = new ContentValues();
		vals.put(KEY_ADDRESS, address);
		vals.put(KEY_TIME, new Date().getTime());
		vals.put(KEY_IDX, idx);
		vals.put(KEY_DELETED, 0);
		vals.put(KEY_NICKNAME, nickname);
		Log.d("addF.mADB updateUser1=" + address + " " + idx + " " + nickname);
		return mDb.update(AMP_USER_DB_TABLE, vals, KEY_ADDRESS + "='" + address + "'", null) > 0;
	}
}

