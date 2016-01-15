package com.pingshow.airecenter.contacts;

import java.util.ArrayList;

import com.pingshow.util.MyTelephony;

public class ContactsOnline {
	
	static public class OnlineStatus {
		public String address;
		public int status;
		public String sipIP;
	}
	
	static public ArrayList<OnlineStatus> mOnlineList=new ArrayList<OnlineStatus>();
	
	static public void resetContactOnlineList()
	{
		mOnlineList.clear();
	}
	
	static public void addContact(String address)
	{
		OnlineStatus c=new OnlineStatus();
		c.address=address;
		c.status=-1;
		mOnlineList.add(c);
	}
	
	static public void addContact_0(String address)
	{
		OnlineStatus c=new OnlineStatus();
		c.address=address;
		c.status=-1;
		mOnlineList.add(0,c);
	}
	
	static public void addContact(String address, int status)
	{
		OnlineStatus c=new OnlineStatus();
		c.address=address;
		c.status=status;
		mOnlineList.add(c);
	}
	
	static public void addContactSipIP(String address, String SipIP)
	{
		OnlineStatus c;
		for(int j=0;j<mOnlineList.size();j++)
		{
			c=mOnlineList.get(j);
			if (MyTelephony.SameNumber(address, c.address))
			{
				c.sipIP=SipIP;
				return;
			}
		}
	}
	
	static public String getContactSipIP(String number)
	{
		OnlineStatus c;
		for(int j=0;j<mOnlineList.size();j++)
		{
			c=mOnlineList.get(j);
			if (MyTelephony.SameNumber(number, c.address))
				return c.sipIP;
		}
		return null;
	}
	
	static public boolean setContactOnlineStatus(String address, int onlineType) 
	{
		for(int i = 0;i<mOnlineList.size();i++){
			if(MyTelephony.SameNumber(mOnlineList.get(i).address, address)){
				OnlineStatus c=mOnlineList.get(i);
				c.status=onlineType;
				return true;
			}
		}
		addContact(address, onlineType);
		return true;
	}
	
	static public int getContactOnlineStatus(String address) 
	{
		for(int j=0;j<mOnlineList.size();j++)
		{
			OnlineStatus c=mOnlineList.get(j);
			if (MyTelephony.SameNumber(address, c.address))
				return c.status;
		}
		return 0;
	}
	
	static public void setAllfriendsOffline()
	{
		for(int j=0;j<mOnlineList.size();j++)
		{
			OnlineStatus c=mOnlineList.get(j);
			if (c.status>0) c.status=0;
		}
	}
}
