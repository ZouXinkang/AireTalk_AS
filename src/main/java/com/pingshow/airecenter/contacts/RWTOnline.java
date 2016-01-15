package com.pingshow.airecenter.contacts;

import java.util.ArrayList;

public class RWTOnline {
	
	static public class OnlineStatus {
		public int idx;
		public int status;
	}
	
	static public ArrayList<OnlineStatus> mOnlineList=new ArrayList<OnlineStatus>();
	
	static public void resetContactOnlineList()
	{
		mOnlineList.clear();
	}
	
	static public void addContact(int idx)
	{
		OnlineStatus c=new OnlineStatus();
		c.idx=idx;
		c.status=0;
		mOnlineList.add(c);
	}
	
	static public void addContact(int idx, int status)
	{
		OnlineStatus c=new OnlineStatus();
		c.idx=idx;
		c.status=status;
		mOnlineList.add(c);
	}
	
	static public boolean setContactOnlineStatus(int idx, int onlineType) 
	{
		for(int i = 0;i<mOnlineList.size();i++){
			if(mOnlineList.get(i).idx==idx){
				OnlineStatus c=mOnlineList.get(i);
				c.status=onlineType;
				return true;
			}
		}
		addContact(idx, onlineType);
		return true;
	}
	
	static public int getContactOnlineStatus(int idx) 
	{
		for(int j=0;j<mOnlineList.size();j++)
		{
			OnlineStatus c=mOnlineList.get(j);
			if (c.idx==idx)
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
