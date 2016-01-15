package com.pingshow.airecenter;

public abstract class Page {
	public abstract void destroy(); 
	
	public String getName()
	{
		Class<?> enclosingClass = getClass().getEnclosingClass();
		if (enclosingClass != null)
			return enclosingClass.getName();
		return getClass().getName();
	}
}
