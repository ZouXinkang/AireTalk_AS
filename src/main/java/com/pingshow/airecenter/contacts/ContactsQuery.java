package com.pingshow.airecenter.contacts;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContacts.Data;

import com.pingshow.airecenter.Log;
import com.pingshow.util.MyTelephony;

public class ContactsQuery {
	
	private Context mContext;
	static private Cursor mPhoneCursor;
	
	public ContactsQuery(Context context)
	{
		mContext=context;
	}
	
	public void clearContactCursor()
	{
		if (mPhoneCursor!=null && !mPhoneCursor.isClosed()) 
			mPhoneCursor.close();

		mPhoneCursor=null;
	}
	
	private void queryPhones()
	{
		clearContactCursor();
		
		try{
			mPhoneCursor = mContext.getContentResolver().query(
							CommonDataKinds.Phone.CONTENT_URI,
								new String [] {
									CommonDataKinds.Phone.CONTACT_ID,
									CommonDataKinds.Phone.NUMBER},
							null, 
							null, null);
		}catch(Exception e){
		}
	}
	
	public synchronized long getContactIdByNumber(String Number) 
	{
		long id=-1;
		if (!MyTelephony.isPhoneNumber(Number))
		{
			return -20;
		}
		if (mPhoneCursor==null) 
			queryPhones();
		
		try{
			if (mPhoneCursor!=null && mPhoneCursor.moveToFirst())
				do{
					String phoneNumber = mPhoneCursor.getString(1);
					phoneNumber=MyTelephony.cleanPhoneNumber(phoneNumber);
					if (MyTelephony.SameNumber(phoneNumber, Number))
					{
						id=mPhoneCursor.getLong(0);
						break;
					}
				}while (mPhoneCursor.moveToNext());
		}catch(Exception e){
		}
		
		return id;
	}
	
	public String getNameByContactId(long contactId)
	{
		if (contactId<=0) return "";
		try{
			Cursor c = mContext.getContentResolver().query(
								ContactsContract.Contacts.CONTENT_URI,
								null,
								ContactsContract.Contacts._ID + "=" + contactId + " AND " +
								ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1"
								, null, null);
			if (c!=null)
			{
				if (c.moveToFirst())
				{
					String name=c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if(c!=null && !c.isClosed())
						c.close();
					return name;
				}
				if(c!=null && !c.isClosed())
					c.close();
			}
		}catch(Exception e){
		}
		
		return "";
	}
	
	public String getNameByContactId(long contactId, String defaultVal)
	{
		if (contactId<=0) return defaultVal;
		try{
			Cursor c = mContext.getContentResolver().query(
								ContactsContract.Contacts.CONTENT_URI,
								null,
								ContactsContract.Contacts._ID + "=" + contactId + " AND " +
								ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1"
								, null, null);
			if (c!=null)
			{
				if (c.moveToFirst())
				{
					String name=c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if(c!=null && !c.isClosed())
						c.close();
					return name;
				}
				if(c!=null && !c.isClosed())
					c.close();
			}
		}catch(Exception e){
		}
		
		return defaultVal;
	}
	
	public String getPrimaryNumberByContactId(long contactId) 
	{
		String s="";
		try{
			Cursor c = mContext.getContentResolver().query(
								CommonDataKinds.Phone.CONTENT_URI,
								null,
								CommonDataKinds.Phone.CONTACT_ID + "=" + contactId,
								null, null);
			
			if(c!=null)
			{
				if (c.moveToFirst())
					s = c.getString(c.getColumnIndex(CommonDataKinds.Phone.NUMBER));
					c.close();
			}
			s=MyTelephony.cleanPhoneNumber(s);
		}catch(Exception e){
		}
		return s;
	}
	
	public String getPossibleGlobalNumberByContactId(long contactId, String UserNumber) 
	{
		try{
			Cursor c = mContext.getContentResolver().query(
								CommonDataKinds.Phone.CONTENT_URI,
								null,
								CommonDataKinds.Phone.CONTACT_ID + "=" + contactId, null, null);
			while (c.moveToNext()) {
				String phoneNumber = c.getString(c.getColumnIndex(CommonDataKinds.Phone.NUMBER));
				phoneNumber=MyTelephony.cleanPhoneNumber(phoneNumber);
				
				if (MyTelephony.SameNumber(phoneNumber, UserNumber))
				{
					if(c!=null && !c.isClosed())
						c.close();
					return phoneNumber;
				}
			}
			if(c!=null && !c.isClosed())
				c.close();
		}catch(Exception e){
		}
		return UserNumber;
	}
	
	public Drawable getPhotoById(Context context, long id, boolean HighDensity)
	{
		if (id>0)
		{
			try{
				//alec
				Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
		        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);
		        if (input != null) {
		        	try {
						return Drawable.createFromStream(input, "profile");
					} catch (Exception e) {
					}
		        }
			}catch(Exception e){
			}
		}
        return null;
	}

	public boolean addContact(String address, String orignum, String disName,
			String photoPath) {
		try {
			long id = getContactIdByNumber(address);
			ContentValues values = new ContentValues();
			long rawContactId = -1;
			Bitmap sourceBitmap = null;
			if (id == -1) {
				// frist,to insert not date is in order to get rawContactId,
				// because the only way is to insert contacts in the address
				// book is visible
				Uri rawContactUri = mContext.getContentResolver().insert(
						RawContacts.CONTENT_URI, values);
				rawContactId = ContentUris.parseId(rawContactUri);
				Log.d("uri1:" + rawContactUri);

				// insert address
				values.clear();
				values.put(Data.RAW_CONTACT_ID, rawContactId);
				values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
				values.put(Phone.NUMBER, orignum);
				values.put(Phone.TYPE, Phone.TYPE_MOBILE);
				Uri uri1 = mContext.getContentResolver().insert(
						android.provider.ContactsContract.Data.CONTENT_URI,
						values);
				Log.d( "uri3:" + uri1);
			} else {
				Cursor rawContactsIdCur = mContext.getContentResolver().query(
						RawContacts.CONTENT_URI, null,
						RawContacts.CONTACT_ID + " = ?",
						new String[] { id + "" }, null);
				if (rawContactsIdCur.moveToFirst()) {
					rawContactId = rawContactsIdCur.getLong(rawContactsIdCur
							.getColumnIndex(RawContacts._ID));
				}
				Log.d("rawContactId:" + rawContactId);
				if(rawContactsIdCur!=null && !rawContactsIdCur.isClosed()) 
					rawContactsIdCur.close();

				String where = ContactsContract.Data.RAW_CONTACT_ID + " = "
						+ rawContactId + " AND "
						+ ContactsContract.Data.MIMETYPE + "='"
						+ StructuredName.CONTENT_ITEM_TYPE + "'";
				// delete disname
				mContext.getContentResolver().delete(
						android.provider.ContactsContract.Data.CONTENT_URI,
						where, null);

				if (photoPath != null) {
					try{
						sourceBitmap = BitmapFactory.decodeFile(photoPath);
					}catch(OutOfMemoryError e){}
					if (sourceBitmap != null) {
						where = ContactsContract.Data.RAW_CONTACT_ID
								+ " = "
								+ rawContactId
								+ " AND "
								+ ContactsContract.Data.MIMETYPE
								+ "='"
								+ ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
								+ "'";
						mContext.getContentResolver().delete(android.provider.ContactsContract.Data.CONTENT_URI,
										where, null);// delete disname
					}
				}
			}
			// insert disname
			if (disName == null) disName = address;
			values.clear();
			values.put(Data.RAW_CONTACT_ID, rawContactId);
			values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
			values.put(StructuredName.GIVEN_NAME, disName);
			mContext.getContentResolver().insert(
					android.provider.ContactsContract.Data.CONTENT_URI, values);

			if (photoPath != null) {
				try {
					sourceBitmap = BitmapFactory.decodeFile(photoPath);
				} catch (OutOfMemoryError e) {}
				if (sourceBitmap != null) {
					try{
						ByteArrayOutputStream os = new ByteArrayOutputStream();
						sourceBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
						byte[] avatar = os.toByteArray();
						setContactPhoto(mContext.getContentResolver(), avatar,
								rawContactId);
					}catch(Exception e){
					}
				}
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static void setContactPhoto(ContentResolver c, byte[] bytes,
			Long rawContactId) {
		ContentValues values = new ContentValues();
		int photoRow = -1;
		String where = ContactsContract.Data.RAW_CONTACT_ID + " = "
				+ rawContactId + " AND " + ContactsContract.Data.MIMETYPE
				+ "='"
				+ ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
				+ "'";
		Cursor cursor = c.query(ContactsContract.Data.CONTENT_URI, null, where,
				null, null);
		int idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID);
		if (cursor.moveToFirst()) {
			photoRow = cursor.getInt(idIdx);
		}
		if(cursor!=null && !cursor.isClosed()) 
			cursor.close();
		values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
		values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
		values.put(ContactsContract.CommonDataKinds.Photo.PHOTO, bytes);
		values.put(ContactsContract.Data.MIMETYPE,
				ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
		if (photoRow >= 0) {
			c.update(ContactsContract.Data.CONTENT_URI, values,
					ContactsContract.Data._ID + " = " + photoRow, null);
		} else {
			c.insert(ContactsContract.Data.CONTENT_URI, values);
		}
	}
}
