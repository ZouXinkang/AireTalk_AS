
package com.pingshow.voip.core;

public class VoipAddressImpl implements VoipAddress {
	protected final long d;
	boolean ownPtr = false;
	private native long newVoipAddressImpl(String uri,String displayName);
	
	private native void delete(long ptr);
	private native String getUserName(long ptr);
	private native void setDisplayName(long ptr,String name);
	
	protected VoipAddressImpl(String identity)  {
		d = newVoipAddressImpl(identity, null);
	}
	protected VoipAddressImpl(String username,String domain,String displayName)  {
		d = newVoipAddressImpl("sip:"+username+"@"+domain, displayName);
	}
	protected VoipAddressImpl(long aNativePtr,boolean javaOwnPtr)  {
		d = aNativePtr;
		ownPtr=javaOwnPtr;
	}
	protected VoipAddressImpl(long aNativePtr)  {
		d = aNativePtr;
		ownPtr=false;
	}
	protected void finalize() throws Throwable {
		if (ownPtr) delete(d);
	}

	public String getUserName() {
		return getUserName(d);
	}
	
	public void setDisplayName(String name) {
		setDisplayName(d,name);
	}
}
