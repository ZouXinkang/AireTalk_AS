package com.pingshow.voip.core;
public interface VoipProxyConfig {
	public void edit();
	public void done();
	public void setIdentity(String identity) throws VoipCoreException;
	public String getIdentity();
	public void setProxy(String proxyUri) throws VoipCoreException;
	public String getProxy();
	public void enableRegister(boolean value) throws VoipCoreException;
	public boolean registerEnabled();
	public String normalizePhoneNumber(String number);
	public void setDialPrefix(String prefix);
	public void setDialEscapePlus(boolean value);
	public String getDomain();
	public boolean isRegistered();
	public void setRoute(String routeUri) throws VoipCoreException;
	public String getRoute();
	public void enablePublish(boolean enable);
	public boolean publishEnabled();
	VoipCore.RegistrationState getState();
	void setExpires(int delay);
}
