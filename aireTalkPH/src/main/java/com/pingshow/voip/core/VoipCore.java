
package com.pingshow.voip.core;

import java.util.Vector;


@SuppressWarnings("unchecked")
public interface VoipCore {

	static public class GlobalState {
		static private Vector values = new Vector();
		static public GlobalState GlobalOff = new GlobalState(0,"GlobalOff");       
		static public GlobalState GlobalStartup = new GlobalState(1,"GlobalStartup");
		static public GlobalState GlobalOn = new GlobalState(2,"GlobalOn");
		static public GlobalState GlobalShutdown = new GlobalState(3,"GlobalShutdown");

		private final int mValue;
		private final String mStringValue;

		private GlobalState(int value,String stringValue) {
			mValue = value;
			values.addElement(this);
			mStringValue=stringValue;
		}
		public static GlobalState fromInt(int value) {
			for (int i=0; i<values.size();i++) {
				GlobalState state = (GlobalState) values.elementAt(i);
				if (state.mValue == value) return state;
			}
			throw new RuntimeException("state not found ["+value+"]");
		}
		public String toString() {
			return mStringValue;
		}
	}

	static public class RegistrationState {
		private static Vector values = new Vector();

		public static RegistrationState RegistrationNone = new RegistrationState(0,"RegistrationNone");       
		public static RegistrationState RegistrationProgress  = new RegistrationState(1,"RegistrationProgress");
		public static RegistrationState RegistrationOk = new RegistrationState(2,"RegistrationOk");
		public static RegistrationState RegistrationCleared = new RegistrationState(3,"RegistrationCleared");
		public static RegistrationState RegistrationFailed = new RegistrationState(4,"RegistrationFailed");

		private final int mValue;
		private final String mStringValue;

		private RegistrationState(int value, String stringValue) {
			mValue = value;
			values.addElement(this);
			mStringValue=stringValue;
		}
		public static RegistrationState fromInt(int value) {

			for (int i=0; i<values.size();i++) {
				RegistrationState state = (RegistrationState) values.elementAt(i);
				if (state.mValue == value) return state;
			}
			throw new RuntimeException("state not found ["+value+"]");
		}
		public String toString() {
			return mStringValue;
		}
	}

	static public class STUNC {
		static private Vector values = new Vector();
		static public STUNC DEFAULT = new STUNC(0,"NoFirewall");       
		static public STUNC UseNatAddress  = new STUNC(1,"UseNatAddress");
		static public STUNC UseStun = new STUNC(2,"UseStun");
		
		private final int mValue;
		private final String mStringValue;

		private STUNC(int value,String stringValue) {
			mValue = value;
			values.addElement(this);
			mStringValue=stringValue;
		}
		public static STUNC fromInt(int value) {
			for (int i=0; i<values.size();i++) {
				STUNC state = (STUNC) values.elementAt(i);
				if (state.mValue == value) return state;
			}
			throw new RuntimeException("Not found ["+value+"]");
		}
		public String toString() {
			return mStringValue;
		}
		public int value(){
			return mValue;
		}
	}
	static public class Transport {
		public final static Transport udp =new Transport("udp");
		public final static Transport tcp =new Transport("tcp");
		private final String mStringValue;

		private Transport(String stringValue) {
			mStringValue=stringValue;
		}
		public String toString() {
			return mStringValue;
		}		
	}
	
	public void clearProxyConfigs();
	public void addProxyConfig(VoipProxyConfig proxyCfg) throws VoipCoreException;
	public void setDefaultProxyConfig(VoipProxyConfig proxyCfg);
	public VoipProxyConfig getDefaultProxyConfig() ;

	void clearAuthInfos();
	void addAuthInfo(VoipAuthInfo info);
	public VoipAddress interpretUrl(String destination) throws VoipCoreException;
	public VoipCall invite(String destination)throws VoipCoreException;
	public VoipCall invite(VoipAddress to)throws VoipCoreException;
	public void terminateCall(VoipCall aCall);
	public VoipCall getCurrentCall(); 
	public VoipAddress getRemoteAddress();
	public boolean isIncall();
	public boolean isInComingInvitePending();
	public void iterate();
	public void acceptCall(VoipCall aCall) throws VoipCoreException;
	public void setNetworkReachable(boolean isReachable);
	public void destroy();
	public void setMuteSpeaker(int bmute);
	public int isSpeakerMuted();

	public void setNetType(int local, int remote);
	public void muteMic(boolean isMuted);
	public boolean isMicMuted();
	public PayloadType findPayloadType(String mime,int clockRate); 
	public void enablePayloadType(PayloadType pt, boolean enable) throws VoipCoreException;
	public void sendDtmf(char number);
	public void playDtmf(char number,int duration);
	public void stopDtmf();
	
	public void enableSpeaker(boolean value);

	public boolean isSpeakerEnabled();

	public void setStunServer(String stun_server);
	public String getStunServer();
	public void setFirewallPolicy(STUNC pol);
	public VoipCall inviteAddressWithParams(VoipAddress destination, VoipCallParams params) throws VoipCoreException ;
	public VoipCallParams createDefaultCallParameters();
	void enableKeepAlive(boolean enable);
	boolean isKeepAliveEnabled();
	
	boolean isVideoEnabled();
	void enableVideo(boolean vcap_enabled, boolean display_enabled);
	
	void setVideoWindow(Object w);
	void setPreviewWindow(Object w);
	void setDeviceRotation(int rotation);
	public VideoSize getPreferredVideoSize();
	public VideoSize getVideoSize(VoipCall call);
	public void setPreferredVideoSize(VideoSize vSize);
	public int getVideoDevice();
	public void setVideoDevice(int id);
	boolean getVideoEnabled();
	boolean isRunningP2P();
	int getStatus();
	int [] getPorts();
	void copyHistogram(byte data[]);
	public boolean checkVideoAvailable();
	public int getFileTransferProgress();
	public void setOutgoingFilePath(String path);
	public int isFileTransferMode();
	public String getTransferFilename();
	
	public int updateCall(VoipCall call, VoipCallParams params);
	
	public boolean setVideoZoom(int x, int y, int ratio, int reserved);  //vivid*** zoom
	public boolean zoomVideo(int i, int x1, int y1);
	public int [] getVideoRemoteResolution();
}
