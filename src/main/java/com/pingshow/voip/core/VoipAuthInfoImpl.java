
package com.pingshow.voip.core;


class VoipAuthInfoImpl implements VoipAuthInfo {
	protected final long d;
	private native long newVoipAuthInfo(String username, String userid, String passwd, String ha1,String realm);
	private native void delete(long ptr);
	protected VoipAuthInfoImpl(String username,String password, String realm)  {
		d = newVoipAuthInfo(username,null,password,null,realm);
	}
	protected void finalize() throws Throwable {
		delete(d);
	}
}
