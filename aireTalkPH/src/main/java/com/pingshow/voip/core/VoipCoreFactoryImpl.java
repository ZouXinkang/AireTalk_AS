
package com.pingshow.voip.core;

import java.io.IOException;

import android.os.Build;

import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.voip.AireVenus;


public class VoipCoreFactoryImpl extends VoipCoreFactory {
	static int arm=0;
	static {
		if (arm==0)
		{
			try{
				if (Build.class.getField("CPU_ABI").get(null).toString().startsWith("armeabi-v7"))
					arm=7;
				else
					arm=6;
			} catch (Throwable e) {
				arm=6;
			}
		}
		try{
			if (arm==7)
				System.loadLibrary("fafavoip.7");
			else if (arm==6)
				System.loadLibrary("fafavoip");
		}
		catch(UnsatisfiedLinkError e)
		{
			try{
				System.loadLibrary("fafavoip.7");
			}catch(UnsatisfiedLinkError e2){
				System.loadLibrary("fafavoip");
			}
		}
	}
	@Override
	public VoipAuthInfo createAuthInfo(String username, String password,
			String realm) {
		return new VoipAuthInfoImpl(username,password,realm);
	}

	@Override
	public VoipAddress createVoipAddress(String username,
			String domain, String displayName) {
		return new VoipAddressImpl(username,domain,displayName);
	}

	@Override
	public VoipAddress createVoipAddress(String identity) {
		return new VoipAddressImpl(identity);
	}

	@Override
	public VoipCore createVoipCore(VoipCoreListener listener, String stun, int audio_port, int video_port, int mode, int monotor,int hardwareSupportedHD,int sipTransportType) throws VoipCoreException {
		try {
			Log.e("sipTransportType==="+sipTransportType);
			return new VoipCoreImpl(listener, stun, audio_port, video_port, mode, monotor,hardwareSupportedHD,sipTransportType);
		} catch (IOException e) {
			throw new VoipCoreException("Cannot create Voip",e);
		}
	}

	@Override
	public VoipProxyConfig createProxyConfig(String identity, String proxy,
			String route, boolean enableRegister) throws VoipCoreException {
		return new VoipProxyConfigImpl(identity,proxy,route,enableRegister);
	}

}
