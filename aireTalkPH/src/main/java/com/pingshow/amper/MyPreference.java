package com.pingshow.amper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceActivity;

public class MyPreference {

	private SharedPreferences settings;
	
	public MyPreference(Context context)
	{
		settings = context.getSharedPreferences("amper", PreferenceActivity.MODE_PRIVATE);
	}
	
	public void write(String tag, String str)
	{
		Editor edit = settings.edit();   
		edit.putString(tag, str);
		edit.commit(); 
	}
	
	public void write(String tag, boolean value)
	{
		Editor edit = settings.edit();   
		edit.putBoolean(tag, value);
		edit.commit(); 
	}
	
	public void write(String tag, int value)
	{
		Editor edit = settings.edit();   
		edit.putInt(tag, value);
		edit.commit(); 
	}
	
	public void writeLong(String tag, long value)
	{
		Editor edit = settings.edit();   
		edit.putLong(tag, value);
		edit.commit(); 
	}
	
	public void writeFloat(String tag, float value)
	{
		Editor edit = settings.edit();   
		edit.putFloat(tag, value);
		edit.commit(); 
	}
	
	public String read(String tag)
	{
		return settings.getString(tag,"");
	}
	
	public String read(String tag,String defaultv)
	{
		return settings.getString(tag,defaultv);
	}
	
	public boolean readBoolean(String tag)
	{
		return settings.getBoolean(tag,false);
	}
	
	public boolean readBoolean(String tag, boolean defaultv)
	{
		return settings.getBoolean(tag,defaultv);
	}
	
	public int readInt(String tag)
	{
		return settings.getInt(tag,0);
	}
	
	public int readInt(String tag,int defaultv)
	{
		return settings.getInt(tag,defaultv);
	}
	
	public long readLong(String tag,long defaultv)
	{
		return settings.getLong(tag,defaultv);
	}
	
	public float readFloat(String tag,float defaultv)
	{
		return settings.getFloat(tag,defaultv);
	}
	
	public void delect(String tag){
		Editor edit = settings.edit(); 
		edit.remove(tag);
		edit.commit(); 
	}
}
