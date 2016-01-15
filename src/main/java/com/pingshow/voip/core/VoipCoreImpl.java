
package com.pingshow.voip.core;

import java.io.IOException;

import com.pingshow.airecenter.Log;



class VoipCoreImpl implements VoipCore {

	@SuppressWarnings("unused")
	private final VoipCoreListener mListener;
	private long d = 0;
//	private native long newVoipCore(VoipCoreListener listener, String object, String mode, int sdkVersion, int monotor);
	//sw|vivid*** webcall
	private native long newVoipCore(VoipCoreListener listener, String object, String mode, int sdkVersion, int monotor, int hd, int siptype);
	private native void iterate(long d);
	private native long getDefaultProxyConfig(long d);

	private native void setDefaultProxyConfig(long d,long proxyCfgNativePtr);
	private native int addProxyConfig(VoipProxyConfig jprtoxyCfg,long d,long proxyCfgNativePtr);
	private native void clearAuthInfos(long d);
	
	private native void clearProxyConfigs(long d);
	private native void addAuthInfo(long d,long authInfoNativePtr);
	private native long invite(long d,String uri);
	private native void terminateCall(long d, long call);
	private native long getRemoteAddress(long d);
	private native boolean  isInCall(long d);
	private native boolean isInComingInvitePending(long d);
	private native void acceptCall(long d, long call);
	private native void delete(long d);
	private native void setNetworkStateReachable(long d,boolean isReachable);
	private native void setMuteSpeaker(long nativeptr, int mute);//alec
	private native int isSpeakerMuted(long nativeptr);//alec
	private native void setNetType(long d, int local, int remote);//alec
	private native int isRunningP2P(long d);//alec
	private native int getStatus(long d);//alec
	private native int[] getPorts(long d);//alec
	public native int [] getVideoSize(long ptrCall);//alec
	private native void copyHistogram(long d,byte data[]);//alec
	private native int getFileTransferProgress(long d);//alec
	private native void setOutgoingFilePath(long d, String path);//alec
	private native int isFileTransferMode(long d);//alec
	private native String getTransferFilename(long d);//alec
	private native void muteMic(long d,boolean isMuted);
	private native long interpretUrl(long d,String destination);
	private native long inviteAddress(long d,long to);
	private native long inviteAddressWithParams(long nativePtrLc,long to, long nativePtrParam);
	private native void sendDtmf(long d,char dtmf);
	private native void clearCallLogs(long d);
	private native boolean isMicMuted(long d);
	private native long findPayloadType(long nativePtr, String mime, int clockRate);
	private native int enablePayloadType(long nativePtr, long payloadType,	boolean enable);
	private native long getCurrentCall(long d) ;
	private native void playDtmf(long d,char dtmf,int duration);
	private native void stopDtmf(long d);
	
	//alec
	private native void setDeviceRotation(long d, int rotation);
	private native void enableVideo(long d,boolean vcap_enabled,boolean display_enabled);
	private native boolean isVideoEnabled(long d);
	private native int setVideoDevice(long d, int id);
	private native int getVideoDevice(long d);
	private native void setVideoWindowId(long d, Object wid);
	private native void setPreviewWindowId(long d, Object wid);
	private native void setPreferredVideoSize(long d, int width, int heigth);
	private native int[] getPreferredVideoSize(long d);
	private native boolean getVideoEnabled(long d);
	private native boolean getVideoAvailable(long d);
	private native void setSpeaker(long d, boolean enabled);
	private native void setLocalPort(long d, int audio, int video);
	
	private native void setFirewallPolicy(long d, int enum_value);
	private native void setStunServer(long d, String stun_server);
	private native String getStunServer(long d);
	private native long createDefaultCallParams(long d);
	private native int updateCall(long ptrLc, long ptrCall, long ptrParams);
	private native void enableKeepAlive(long d,boolean enable);
	private native boolean isKeepAliveEnabled(long d);
	//vivid*** zoom
	private native boolean setVideoZoom(long d, int x, int y, int ratio, int reserved);
	private native int [] getVideoRemoteResolution(long d);
	private native boolean checkVideoReady(long d);
	private native boolean zoomVideo(long d, int ratio, int x, int y);

//	VoipCoreImpl(VoipCoreListener listener, String stun, int audio_port, int video_port, int mode, int monitor) throws IOException {
	//sw|vivid*** webcall
	VoipCoreImpl(VoipCoreListener listener, String stun, int audio_port, int video_port, int mode, int monitor, int hd, int siptype) throws IOException {
		mListener=listener;
		//setLocalPort(0, audio_port, video_port);
		String Mode=null;
		if (mode==1)//Normal Call with ICE
			Mode="1";
		else if (mode==2)//File Transfer Mode
			Mode="File Transfer";
		else
			Mode=null; //disable ICE
//		d = newVoipCore(listener, stun, Mode, Version.sdk(), monitor);
		d = newVoipCore(listener, stun, Mode, Version.sdk(), monitor, hd, siptype);  //sw|vivid*** webcall
	}
	
	protected void finalize() throws Throwable {
		
	}
	
	public synchronized void addAuthInfo(VoipAuthInfo info) {
		try{
			isValid();
			addAuthInfo(d,((VoipAuthInfoImpl)info).d);
		}catch(Exception e){}
	}

	public synchronized VoipProxyConfig getDefaultProxyConfig() {
		try{
			isValid();
			long p = getDefaultProxyConfig(d);
			if (p!=0) {
				return new VoipProxyConfigImpl(p); 
			} else {
				return null;
			}
		}catch(Exception e){}
		return null;
	}

	public synchronized VoipCall invite(String uri) {
		isValid();
		long p = invite(d,uri);
		if (p!=0) {
			return new VoipCallImpl(p); 
		} else {
			return null;
		}
	}

	public synchronized void iterate() {
		isValid();
		iterate(d);
	}

	public synchronized void setDefaultProxyConfig(VoipProxyConfig proxyCfg) {
		isValid();
		setDefaultProxyConfig(d,((VoipProxyConfigImpl)proxyCfg).d);
	}
	public synchronized void addProxyConfig(VoipProxyConfig proxyCfg) throws VoipCoreException{
		isValid();
		if (addProxyConfig(proxyCfg,d,((VoipProxyConfigImpl)proxyCfg).d) !=0) {
			throw new VoipCoreException("error proxy config");
		}
	}
	public synchronized void clearAuthInfos() {
		isValid();
		clearAuthInfos(d);
		
	}
	public synchronized void clearProxyConfigs() {
		isValid();
		clearProxyConfigs(d);
	}
	public synchronized void terminateCall(VoipCall aCall) {
		isValid();
		if (aCall!=null)terminateCall(d,((VoipCallImpl)aCall).d);
	}
	public synchronized VoipAddress getRemoteAddress() {
		isValid();
		long ptr = getRemoteAddress(d);
		if (ptr==0) {
			return null;
		} else {
			return new VoipAddressImpl(ptr);
		}
	}
	public synchronized  boolean isIncall() {
		isValid();
		return isInCall(d);
	}
	public synchronized boolean isInComingInvitePending() {
		isValid();
		return isInComingInvitePending(d);
	}
	public synchronized void acceptCall(VoipCall aCall) {
		isValid();
		acceptCall(d,((VoipCallImpl)aCall).d);
		
	}
	public synchronized void destroy() {
		if (d==0) return;//alec
		isValid();
		delete(d);
		d = 0;
	}
	
	private void isValid() {
		if (d == 0) {
			throw new RuntimeException("object already destroyed");
		}
	}
	public void setNetworkReachable(boolean isReachable) {
		setNetworkStateReachable(d,isReachable);
	}
	public void setMuteSpeaker(int mute) {//alec
		setMuteSpeaker(d, mute);
	}
	public int isSpeakerMuted()
	{
		return isSpeakerMuted(d);
	}
	public void muteMic(boolean isMuted) {
		muteMic(d,isMuted);
	}
	public PayloadType findPayloadType(String mime, int clockRate) {
		isValid();
		long playLoadType = findPayloadType(d, mime, clockRate);
		if (playLoadType == 0) {
			return null;
		} else {
			return new PayloadTypeImpl(playLoadType);
		}
	}
	public void enablePayloadType(PayloadType pt, boolean enable)
			throws VoipCoreException {
		isValid();
		if (enablePayloadType(d,((PayloadTypeImpl)pt).nativePtr,enable) != 0) {
			throw new VoipCoreException("cannot enable payload type ["+pt+"]");
		}
		
	}
	public VoipAddress interpretUrl(String destination) throws VoipCoreException {
		long lAddress = interpretUrl(d,destination);
		if (lAddress != 0) {
			return new VoipAddressImpl(lAddress,true);
		} else {
			throw new VoipCoreException("Cannot interpret ["+destination+"]");
		}
	}
	public VoipCall invite(VoipAddress to) throws VoipCoreException { 
		long p = inviteAddress(d,((VoipAddressImpl)to).d);
		if (p!=0) {
			return new VoipCallImpl(p); 
		} else {
			throw new VoipCoreException("Unable to invite address ");
		}
	}

	public void sendDtmf(char number) {
		sendDtmf(d,number);
	}
	public boolean isMicMuted() {
		return isMicMuted(d);
	}
	public void setNetType(int local, int remote)
	{
		setNetType(d,local,remote);
	}
	
	public synchronized VoipCall getCurrentCall() {
		try{
			isValid();
			long p = getCurrentCall(d);
			if (p!=0) {
				return new VoipCallImpl(p); 
			} else {
				return null;
			}
		}catch(Exception e){}
		return null;
	}
	
	public void enableSpeaker(boolean value) {
		setSpeaker(d, value);
	}
	
	public boolean isSpeakerEnabled() {
		return false;
	}
	public void playDtmf(char number, int duration) {
		playDtmf(d,number, duration);
		
	}
	public void stopDtmf() {
		stopDtmf(d);
	}
	
	public String getStunServer() {
		return getStunServer(d);
	}
	public void setFirewallPolicy(STUNC pol) {
		setFirewallPolicy(d,pol.value());
	}
	public void setStunServer(String stunServer) {
		setStunServer(d,stunServer);
	}
	
	public VoipCallParams createDefaultCallParameters() {
		return new VoipCallParamsImpl(createDefaultCallParams(d));
	}
	
	public VoipCall inviteAddressWithParams(VoipAddress to, VoipCallParams params) throws VoipCoreException {
		long ptrDestination = ((VoipAddressImpl)to).d;
		long ptrParams =((VoipCallParamsImpl)params).d;
		
		long p = inviteAddressWithParams(d, ptrDestination, ptrParams);
		if (p!=0) {
			return new VoipCallImpl(p); 
		} else {
			throw new VoipCoreException("Unable to invite with params");
		}
	}

	public void enableKeepAlive(boolean enable) {
		enableKeepAlive(d,enable);
		
	}
	public boolean isKeepAliveEnabled() {
		return isKeepAliveEnabled(d);
	}
	
	public synchronized void setDeviceRotation(int rotation) {
		setDeviceRotation(d, rotation);
	}
	
	public synchronized void enableVideo(boolean vcap_enabled, boolean display_enabled) {
		enableVideo(d,vcap_enabled, display_enabled);
	}
	public synchronized boolean isVideoEnabled() {
		return isVideoEnabled(d);
	}
	
	public synchronized void setPreviewWindow(Object w) {
		setPreviewWindowId(d,w);
	}
	public synchronized void setVideoWindow(Object w) {
		setVideoWindowId(d,w);
	}
	
	public synchronized void setPreferredVideoSize(VideoSize vSize) {
		setPreferredVideoSize(d, vSize.width, vSize.height);
	}

	public synchronized VideoSize getPreferredVideoSize() {
		int[] nativeSize = getPreferredVideoSize(d);

		VideoSize vSize = new VideoSize();
		vSize.width = nativeSize[0];
		vSize.height = nativeSize[1];
		return vSize;
	}
	
	public synchronized int updateCall(VoipCall call, VoipCallParams params) {
		long ptrCall = ((VoipCallImpl) call).d;
		long ptrParams = params!=null ? ((VoipCallParamsImpl)params).d : 0;

		return updateCall(d, ptrCall, ptrParams);
	}
	public void setVideoDevice(int id) {
		if (setVideoDevice(d, id) != 0) {
			Log.e("Failed to set video device to id:"+id);
		}
	}
	public int getVideoDevice() {
		return getVideoDevice(d);
	}
	
	public boolean getVideoEnabled()
	{
		return getVideoEnabled(d);
	}
	
	public boolean checkVideoAvailable()
	{
		return getVideoAvailable(d);
	}
	
	public boolean isRunningP2P()
	{
		return (isRunningP2P(d)>0);
	}
	
	public int getStatus()
	{
		return getStatus(d);
	}
	
	public int[] getPorts()
	{
		return getPorts(d);
	}
	
	public void copyHistogram(byte data[])
	{
		copyHistogram(d,data);
	}
	
	public int getFileTransferProgress()
	{
		return getFileTransferProgress(d);
	}
	
	public void setOutgoingFilePath(String path)
	{
		setOutgoingFilePath(d, path);
	}
	
	public int isFileTransferMode()
	{
		return isFileTransferMode(d);
	}
	
	public String getTransferFilename()
	{
		return getTransferFilename(d);
	}
	
	public synchronized VideoSize getVideoSize(VoipCall call) {
		VideoSize vSize = new VideoSize();
		try{
			long ptrCall = ((VoipCallImpl) call).d;
			
			int[] nativeSize = getVideoSize(ptrCall);
			vSize.width = nativeSize[0];
			vSize.height = nativeSize[1];
		}catch(Exception e){}
		return vSize;
	}
	//vivid*** zoom
	public boolean setVideoZoom(int x, int y, int ratio, int reserved) {
		return setVideoZoom(d, x, y, ratio, reserved);
	}
	
	public int [] getVideoRemoteResolution() {
		return getVideoRemoteResolution(d);
	}
	
	public boolean checkVideoReady() {
		return checkVideoReady(d);
	}
	
	public boolean zoomVideo(int i, int x1, int y1) {
		return zoomVideo(d, i,x1,y1);
	}
	//***vivid
}
