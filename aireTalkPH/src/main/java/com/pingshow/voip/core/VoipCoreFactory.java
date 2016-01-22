
package com.pingshow.voip.core;

@SuppressWarnings("unchecked")
abstract public class VoipCoreFactory {
	
	private static String factoryName = "com.pingshow.voip.core.VoipCoreFactoryImpl";
	
	static VoipCoreFactory theVoipCoreFactory; 

	public static void setFactoryClassName (String className) {
		factoryName = className;
	}
	
	public static VoipCoreFactory instance() {
		try {
		if (theVoipCoreFactory == null) {
			Class lFactoryClass = Class.forName(factoryName);
			theVoipCoreFactory = (VoipCoreFactory) lFactoryClass.newInstance();
		}
		} catch (Exception e) {
			System.err.println("cannot instanciate factory ["+factoryName+"]");
		}
		return theVoipCoreFactory;
	}
	abstract public VoipAuthInfo createAuthInfo(String username,String password, String realm);
//	abstract public VoipCore createVoipCore(VoipCoreListener listener, String stun, int audio_port, int video_port, int mode, int monotor,int hardwareSupportedHD,int sipTransportType) throws VoipCoreException;
	//sw|vivid*** webcall
	abstract public VoipCore createVoipCore(VoipCoreListener listener, String stun, int audio_port, int video_port, int mode, int monotor, int hd, int siptype) throws VoipCoreException;
	abstract public VoipAddress createVoipAddress(String username,String domain,String displayName);
	abstract public VoipAddress createVoipAddress(String address);
	abstract public VoipProxyConfig createProxyConfig(String identity, String proxy,String route,boolean enableRegister) throws VoipCoreException;
}

