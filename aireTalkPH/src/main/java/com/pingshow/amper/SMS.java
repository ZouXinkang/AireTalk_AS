package com.pingshow.amper;

import java.io.Serializable;

import android.database.Cursor;

import com.pingshow.amper.db.SmsDB;

public class SMS implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public static final int STATUS_PENING=2;
	public static final int STATUS_SENT=-2;
	public static final int STATUS_SENT_PAID=-1;
	
	public long smsid;
	public String address;
	public String displayname;
	public long contactid;
	public String content;
	public long time;
	public long expiration;
	public int type;
	public int read;
	public int attached;
	public String att_path_aud;
	public String att_path_img;
	public int status;
	public long org_smsid;
	public int group_member;
	public long longitudeE6;
	public long latitudeE6;
	public String obligate1;
	public int _unread;
	public int _smsinthd;
	public int progress;
	
	public SMS()
	{
		
	}
	
	public SMS(Cursor c)
	{
		type=c.getInt(c.getColumnIndex(SmsDB.KEY_TYPE));
		smsid=c.getLong(c.getColumnIndex(SmsDB.KEY_SMS_ID));
		address=c.getString(c.getColumnIndex(SmsDB.KEY_ADDRESS));
		displayname=address;
		contactid=c.getLong(c.getColumnIndex(SmsDB.KEY_PERSON));
		time=c.getLong(c.getColumnIndex(SmsDB.KEY_DATE));
		read=c.getInt(c.getColumnIndex(SmsDB.KEY_READ));
		status=c.getInt(c.getColumnIndex(SmsDB.KEY_STATUS));
		content=c.getString(c.getColumnIndex(SmsDB.KEY_BODY));
		attached=c.getInt(c.getColumnIndex(SmsDB.KEY_ATTACH_TYPE));
		att_path_aud=c.getString(c.getColumnIndex(SmsDB.KEY_VMEMO));
		att_path_img=c.getString(c.getColumnIndex(SmsDB.KEY_IMAGE));
		org_smsid=c.getLong(c.getColumnIndex(SmsDB.KEY_ORG_SMSID));
		longitudeE6=c.getLong(c.getColumnIndex(SmsDB.KEY_LONGITUDE));
		latitudeE6=c.getLong(c.getColumnIndex(SmsDB.KEY_LATITUDE));
		group_member=c.getInt(c.getColumnIndex(SmsDB.KEY_AD_FLAG));
		obligate1=c.getString(c.getColumnIndex(SmsDB.KEY_OBLIGATE1));
		progress=c.getInt(c.getColumnIndex(SmsDB.KEY_OBLIGATE4));
	}
}
