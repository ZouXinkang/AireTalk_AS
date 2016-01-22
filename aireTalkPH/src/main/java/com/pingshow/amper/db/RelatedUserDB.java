package com.pingshow.amper.db;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.pingshow.amper.Log;
import com.pingshow.amper.R;
import com.pingshow.util.MyTelephony;

public class RelatedUserDB {
	private static final String RELATED_USER_DB_TABLE = "RelatedUserDB";
	
	public static final String KEY_ID				= "_id";
	public static final String KEY_ADDRESS			= "address";
	public static final String KEY_TIME				= "time";
	public static final String KEY_IDX				= "idx";
	public static final String KEY_ITPH_COUNT		= "itphcount";
	public static final String KEY_MOOD				= "mood";
	public static final String KEY_PHTO				= "photo_from_net";
	public static final String KEY_NICKNAME			= "nickname";
	public static final String KEY_BLOCKED			= "blocked";
	public static final String KEY_JOINT			= "joint";
	
	private static final String DATABASE_NAME 		= "relateduser.db";
	private static final int DATABASE_VERSION 		= 5;
	
	static public String [] COMMON_PROJECTION={KEY_ID,KEY_ADDRESS,KEY_TIME,KEY_IDX,KEY_NICKNAME};
	
	static public String [] BLK_PROJECTION={KEY_ID,KEY_ADDRESS,KEY_NICKNAME,KEY_IDX};
	
	static public String [] KEY_BLOCKED_PROJ={KEY_BLOCKED};
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static final String _DB_CREATE =
	    "CREATE TABLE IF NOT EXISTS " + RELATED_USER_DB_TABLE + " (" +
	    KEY_ID			+ " INTEGER PRIMARY KEY, " +
	    KEY_ADDRESS		+ " CHAR(32) UNIQUE NOT NULL, " +
	    KEY_TIME        + " LONG NOT NULL, " +
		KEY_IDX			+ " INTEGER, "+
		KEY_ITPH_COUNT	+ " INTEGER DEFAULT 0, " +
		KEY_MOOD		+ " TEXT NULL, "+
		KEY_PHTO		+ " INTEGER NULL," +
		KEY_NICKNAME	+ " TEXT NULL, " +
		KEY_BLOCKED		+ " INTEGER DEFAULT 0," +
		KEY_JOINT		+ " INTEGER DEFAULT 0);";
	
	private final Context context;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	    	db.execSQL(_DB_CREATE);
	    }

	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + RELATED_USER_DB_TABLE);
	      	onCreate(db);
	    }
	}
	
	public RelatedUserDB(Context _context) {
		this.context = _context;
	    mDbHelper = null;
	    mDb = null;
	}
	
	public RelatedUserDB open() throws SQLException {
		return open(false);
	}
	
	public boolean isOpen(){
		return mDbHelper!=null && mDb.isOpen();
	}
	
	public synchronized RelatedUserDB open(boolean readOnly){
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
		Cursor cursor = mDb.query(true, RELATED_USER_DB_TABLE,
					COMMON_PROJECTION, KEY_BLOCKED+"=0", null,
					null, null, KEY_JOINT + " DESC", null);
		return cursor;
	}
	
	public synchronized long insertUser(String address,int idx) 
	{
		if (!mDb.isOpen()) return -1;
		if (address.length()<6) return -1;
		Cursor cursor = getCursorByAddress(address);
    	if (cursor.getCount()>0)
    	{
    		cursor.close();
    		return -1;
    	}
    	if (!cursor.isClosed()) cursor.close();
    	Log.w("addF.mRDB insertUser1=" + address + " " + idx);
		return insertUser(address, idx, "Stranger?", 0);
	}
	
	public synchronized long insertUser(String address,int idx, String nickname, int jointfriends) 
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
        	vals.put(KEY_NICKNAME, nickname);
        	vals.put(KEY_JOINT, jointfriends);
        	Log.w("addF.mRDB insertUser2=" + address + " " + idx + " " + nickname);
    		return mDb.update(RELATED_USER_DB_TABLE, vals, KEY_ADDRESS+"='"+address+"'", null);
    	}
    	if (!cursor.isClosed()) cursor.close();
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_ADDRESS, address);
    	vals.put(KEY_TIME, new Date().getTime());
    	vals.put(KEY_IDX, idx);
    	vals.put(KEY_NICKNAME, nickname);
    	vals.put(KEY_JOINT, jointfriends);
    	Log.w("addF.mRDB insertUser3=" + address + " " + idx + " " + nickname);
    	return mDb.insert(RELATED_USER_DB_TABLE, null, vals);
	}
	
	
	public synchronized Cursor getCursorByAddress(String address) throws SQLException {
		if(!mDb.isOpen()) return null;
		return mDb.query(true, RELATED_USER_DB_TABLE,
					null, KEY_ADDRESS + "='" + address +"'", null,
					null, null, null, null);
	}
	
	public synchronized String getFriendIdByAddress(String address) throws SQLException {
		if (!mDb.isOpen()) return "";
		if (address== null || address.length()==0) return "";
		String idx = "";
		Cursor  cursor =mDb.query(true, RELATED_USER_DB_TABLE,
					null, KEY_ADDRESS + "='" + address +"'", null,
					null, null, null, null);
		if(cursor.moveToFirst())
		{
			idx=Integer.toHexString(cursor.getInt(3));
		}
		cursor.close();
		Log.w("addF.mRDB getFriendIdByAddress=" + address + " " + idx);
		return idx;
	}
	
	public synchronized Cursor getFafaFriends() throws SQLException {
		if(!mDb.isOpen()) return null;
		return mDb.query(true, RELATED_USER_DB_TABLE,
					null, KEY_IDX + "!=0", null,
					null, null, KEY_TIME + " desc", null);
	}
	
	public synchronized int getCount() throws SQLException {
		int c=0;
		if(!mDb.isOpen()) return 0;
		Cursor cursor =mDb.query(true, RELATED_USER_DB_TABLE,
				null, null, null,
				null, null, null, null);
		if (cursor!=null)
		{
			c=cursor.getCount();
			cursor.close();
		}
    	Log.w("addF.mRDB getCount=" + c);
		return c;
	}
	
	public synchronized String getAddressByIdx(int idx) throws SQLException {
		String address="";
		if(!mDb.isOpen()) return address;
		Cursor cursor =mDb.query(true, RELATED_USER_DB_TABLE,
					null, KEY_IDX + "=" + idx, null,
					null, null, null, null);
		if(cursor.moveToFirst())
			address=cursor.getString(1);
		cursor.close();
		Log.w("addF.mRDB getAddressByIdx=" + address + " " + idx);
		return address;
	}
	
	public synchronized String getAddressById(long _id) throws SQLException {
		if(!mDb.isOpen()) return "";
		String address="";
		Cursor cursor = mDb.query(true, RELATED_USER_DB_TABLE,
					null, KEY_ID + "='" + _id +"'", null,
					null, null, null, null);
		if (cursor.moveToFirst())
		{
			address=cursor.getString(1);
		}
		cursor.close();
		Log.w("addF.mRDB getAddressById=" + address + " " + _id);
		return address;
	}
	
	public synchronized String getMoodByAddress(String address) throws SQLException {
		String mood="";
		if(!mDb.isOpen()) return mood;
		Cursor cursor = mDb.query(true, RELATED_USER_DB_TABLE,
					new String [] {KEY_MOOD}, KEY_ADDRESS + "='" + address +"'", null,
					null, null, null, null);
		if (cursor.moveToFirst())
			mood=cursor.getString(0);
		cursor.close();
		Log.w("addF.mRDB getMoodByAddress=" + address + " " + mood);
		return mood;
	}
	
	public synchronized int getIdxByAddress(String address) throws SQLException {
		int idx = -1;
		if(!mDb.isOpen()) return idx;
		if(address!=null && address.startsWith("+")){
			Cursor cursor =mDb.query(true, RELATED_USER_DB_TABLE,
					new String [] {KEY_IDX}, KEY_ADDRESS + " = '" + address+"'", null,
					null, null, null, null);
			if(cursor.moveToFirst())
				idx = cursor.getInt(0);
			cursor.close();
		}else if(address!=null){
			Cursor cursor = mDb.query(true, RELATED_USER_DB_TABLE, null, null, null,null, null, null, null);
			while(cursor.moveToNext())
			{
				if (MyTelephony.SameNumber(cursor.getString(1), address)){
					idx = cursor.getInt(3);
					break;
				}
			}
			cursor.close();
		}
		Log.w("addF.mRDB getIdxByAddress=" + address + " " + idx);
		return idx;
	}
	
	public synchronized boolean deleteContactByAddress(String address) 
	{	
		Log.w("addF.mRDB deleteContactByAddress=" + address);
		if(!mDb.isOpen()) return false;
		if (mDb.delete(RELATED_USER_DB_TABLE, KEY_ADDRESS + "='" + address+"'", null) > 0)
	    	return true;
	    return false;
	}
	
	public boolean isFafauser(String address)
	{
		if(!mDb.isOpen()) return false;
		Cursor cursor = mDb.query(true, RELATED_USER_DB_TABLE, null, null, null,null, null, null, null);
		while(cursor.moveToNext())
		{
			if (MyTelephony.SameNumber(cursor.getString(1), address)){
				cursor.close();
				Log.w("addF.mRDB isFafauser TRUE " + address);
				return true;
			}
		}
		cursor.close();
		Log.w("addF.mRDB isFafauser FALSE " + address);
		return false;
	}
	
	public boolean isFafauser(int idx)
	{
		if(!mDb.isOpen()) return false;
		Cursor cursor = mDb.query(true, RELATED_USER_DB_TABLE, null, KEY_IDX+"="+idx, null,null, null, null, null);
		if (cursor!=null)
		{
			if (cursor.getCount()>0)
			{
				cursor.close();
				Log.w("addF.mRDB isFafauser TRUE " + idx);
				return true;
			}
			cursor.close();
		}
		Log.w("addF.mRDB isFafauser FALSE " + idx);
		return false;
	}
	
	public int getPhotoVersionByAddress(String address)
	{
		if(!mDb.isOpen()) return 0;
		int hasPhoto=0;
		Cursor cursor =mDb.query(true, RELATED_USER_DB_TABLE,
				new String [] {KEY_PHTO}, KEY_ADDRESS + " = '" + address+"'", null,
				null, null, null, null);
		if(cursor.moveToFirst())
			hasPhoto = cursor.getInt(0);
		cursor.close();
		Log.w("addF.mRDB getPhotoVersionByAddress=" + address + " " + hasPhoto);
		return hasPhoto;
	}
	
	public int getPhotoVersionByIdx(int idx)
	{
		if(!mDb.isOpen()) return 0;
		int hasPhoto=0;
		Cursor cursor =mDb.query(true, RELATED_USER_DB_TABLE,
				new String [] {KEY_PHTO}, KEY_IDX + " =" + idx, null,
				null, null, null, null);
		if(cursor.moveToFirst())
			hasPhoto = cursor.getInt(0);
		cursor.close();
		Log.w("addF.mRDB getPhotoVersionByIdx=" + idx + " " + hasPhoto);
		return hasPhoto;
	}
	
	public synchronized int updatePhotoByUID(int uid, int version) {
		if(uid==0) return 0;
		if(!mDb.isOpen()) return -1;
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_PHTO, version);
    	Log.w("addF.mRDB updatePhotoByUID=" + uid + " " + version);
	    return mDb.update(RELATED_USER_DB_TABLE, vals, KEY_IDX+"="+uid, null);
	}
	
	public synchronized int updateMoodByUID(int uid, String moodText) {
		if(!mDb.isOpen()) return -1;
		if(uid==0) return 0;
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_MOOD, moodText);
    	Log.w("addF.mRDB updateMoodByUID=" + uid + " " + moodText);
	    return mDb.update(RELATED_USER_DB_TABLE, vals, KEY_IDX+"="+uid, null);
	}
	
	public synchronized Cursor fetchPhotoVersion() throws SQLException {
		if(!mDb.isOpen()) return null;
		Cursor cursor = mDb.query(true, RELATED_USER_DB_TABLE,
					new String [] {KEY_ADDRESS,KEY_IDX,KEY_PHTO}, null, null,	//simon 061011
					null, null, KEY_TIME + " desc", null);
		return cursor;
	}
	
	public synchronized int updateNicknameByUID(int uid, String nickname) {
		if(!mDb.isOpen()) return -1;
		if(uid==0) return 0;
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_NICKNAME, nickname);
		Log.w("addF.mRDB updateNicknameByUID=" + uid + " " + nickname);
	    return mDb.update(RELATED_USER_DB_TABLE, vals, KEY_IDX+"="+uid, null);
	}
	
	public String getNicknameByAddress(String address)
	{
		if(!mDb.isOpen()) return "";
		String nickname="";
		Cursor cursor =mDb.query(true, RELATED_USER_DB_TABLE,
				new String [] { KEY_NICKNAME }, KEY_ADDRESS + " = '" + address+"'", null,
				null, null, null, null);
		if(cursor.moveToFirst())
			nickname = cursor.getString(0);
		cursor.close();
		Log.w("addF.mRDB getNicknameByAddress=" + address + " " + nickname);
		return nickname;
	}
	
	public synchronized int blockUserByAddress(String address, int block) {
		if(!mDb.isOpen()) return -1;
    	ContentValues vals = new ContentValues();
    	vals.put(KEY_BLOCKED, block);
		Log.w("addF.mRDB blockUserByAddress=" + address + " " + block);
	    return mDb.update(RELATED_USER_DB_TABLE, vals, KEY_ADDRESS+"='"+address+"'", null);
	}
	
	public synchronized int isUserBlocked(String address) {
		int blocked=0;
		if(mDb.isOpen()){
			Cursor cursor = mDb.query(true, RELATED_USER_DB_TABLE,
					KEY_BLOCKED_PROJ, KEY_ADDRESS + " = '" + address+"'", null,
					null, null, null, null);
			if (cursor.moveToFirst())
			{
				blocked=cursor.getInt(0);
			}
			cursor.close();
		}
		Log.w("addF.mRDB isUserBlocked=" + address + " " + blocked);
		return blocked;
	}
	
	public synchronized int isUserBlocked(int idx) {
		int blocked=0;
		if(mDb.isOpen()){
			Cursor cursor = mDb.query(true, RELATED_USER_DB_TABLE,
					KEY_BLOCKED_PROJ, KEY_IDX + "="+idx, null,
					null, null, null, null);
			if (cursor.moveToFirst())
			{
				blocked=cursor.getInt(0);
				cursor.close();
			}
		}
		Log.w("addF.mRDB isUserBlocked=" + idx + " " + blocked);
		return blocked;
	}
	
	public synchronized Cursor fetchBlockedUsers() throws SQLException {
		if(!mDb.isOpen()) return null;
		Cursor cursor = mDb.query(true, RELATED_USER_DB_TABLE,
					BLK_PROJECTION, KEY_BLOCKED+"=1", null,
					null, null, KEY_NICKNAME + " COLLATE LOCALIZED ASC", null);
		return cursor;
	}
	
	public synchronized int getJointFriendsByAddress(String address) {
		int jf=0;
		Cursor cursor = mDb.query(true, RELATED_USER_DB_TABLE,
				new String[]{KEY_JOINT}, KEY_ADDRESS + "='"+address+"'", null,
				null, null, null, null);
		if (cursor.moveToFirst())
		{
			jf=cursor.getInt(0);
		}
		cursor.close();
		Log.w("addF.mRDB getJointFriendsByAddress=" + address + " " + jf);
		return jf;
	}
	
	public synchronized boolean deleteAll() 
	{	
		if(!mDb.isOpen()) return false;
		if (mDb.delete(RELATED_USER_DB_TABLE, null, null) > 0)
	    	return true;
	    return false;
	}
	
	public void deleteTable(){
		if(!mDb.isOpen()) return;
		mDb.delete(RELATED_USER_DB_TABLE, null, null);
	}
}

