
package com.pingshow.voip.core;

import com.pingshow.amper.Log;
import com.pingshow.voip.core.VoipCore.RegistrationState;

class VoipProxyConfigImpl implements VoipProxyConfig {

	protected final long d;
	
	private native int getState(long d);
	private native void setExpires(long d, int delay);

	boolean ownPtr = false;
	protected VoipProxyConfigImpl(String identity,String proxy,String route, boolean enableRegister) throws VoipCoreException {
		d = newVoipProxyConfig();
		setIdentity(identity);
		setProxy(proxy);
		enableRegister(enableRegister);
		ownPtr=true;
	}
	protected VoipProxyConfigImpl(long aNativePtr)  {
		d = aNativePtr;
		ownPtr=false;
	}
	protected void finalize() throws Throwable {
		if (ownPtr) delete(d);
	}
	private native long newVoipProxyConfig();
	private native void delete(long ptr);

	private native void edit(long ptr);
	private native void done(long ptr);
	
	private native void setIdentity(long ptr,String identity);
	private native String getIdentity(long ptr);
	private native int setProxy(long ptr,String proxy);
	private native String getProxy(long ptr);
	
	private native void startRegister(long ptr);
	private native void enableRegister(long ptr,boolean value);
	private native boolean isRegisterEnabled(long ptr);
	
	private native boolean isRegistered(long ptr);
	private native void setDialPrefix(long ptr, String prefix);
	
	private native String normalizePhoneNumber(long ptr,String number);
	
	private native String getDomain(long ptr);
	
	private native void setDialEscapePlus(long ptr, boolean value);
	
	private native String getRoute(long ptr);
	private native int setRoute(long ptr,String uri);
	private native void enablePublish(long ptr,boolean enable);
	private native boolean publishEnabled(long ptr);
	
	public void enableRegister(boolean value) {
		Log.e("enableRegister run!!!");
		enableRegister(d,value);
	}
	public void done() {
		done(d);
	}
	public void edit() {
		edit(d);
	}
	public void setIdentity(String identity) throws VoipCoreException {
		setIdentity(d,identity);
	}
	public void setProxy(String proxyUri) throws VoipCoreException {
		if (setProxy(d,proxyUri)!=0) {
			throw new VoipCoreException("Bad proxy address ["+proxyUri+"]");
		}
	}
	public String normalizePhoneNumber(String number) {
		return normalizePhoneNumber(d,number);
	}
	public void setDialPrefix(String prefix) {
		setDialPrefix(d, prefix);
	}
	public String getDomain() {
		return getDomain(d);
	}
	public void setDialEscapePlus(boolean value) {
		 setDialEscapePlus(d,value);
	}
	public String getIdentity() {
		return getIdentity(d);
	}
	public String getProxy() {
		return getProxy(d);
	}
	public boolean isRegistered() {
		return isRegistered(d);
	}
	public boolean registerEnabled() {
		return isRegisterEnabled(d);
	}
	public String getRoute() {
		return getRoute(d);
	}
	public void setRoute(String routeUri) throws VoipCoreException {
		if (setRoute(d, routeUri) != 0) {
			throw new VoipCoreException("cannot set route ["+routeUri+"]");
		}
	}
	public void enablePublish(boolean enable) {
		enablePublish(d,enable);
	}
	public RegistrationState getState() {
		return RegistrationState.fromInt(getState(d));
	}

	public void setExpires(int delay) {
		setExpires(d, delay);
	}
	public boolean publishEnabled() {
		return publishEnabled(d); 
	}
	@Override
	public void startRegister() throws VoipCoreException {
		Log.e("startRegister run!!!");
		startRegister(d);
	}
}
