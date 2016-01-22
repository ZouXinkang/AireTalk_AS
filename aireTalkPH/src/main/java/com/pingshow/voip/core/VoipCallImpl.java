
package com.pingshow.voip.core;

import com.pingshow.voip.core.VoipCallParams;
import com.pingshow.voip.core.VoipCallParamsImpl;



class VoipCallImpl implements VoipCall {

	protected final long d;
	boolean ownPtr = false;
	native private void ref(long ownPtr);
	native private void unref(long ownPtr);
	private native boolean isIncoming(long d);
	native private long getRemoteAddress(long d);
	native private int getState(long d);
	private native void enableEchoLimiter(long d,boolean enable);
	private native long getReplacedCall(long d);
	private native long getCurrentParamsCopy(long nativePtr);
	private native void enableCamera(long nativePtr, boolean enabled);
	private native int isVideoEmpty(long nativePtr);

	protected VoipCallImpl(long aNativePtr)  {
		d = aNativePtr;
		ref(d);
	}
	protected void finalize() throws Throwable {
		unref(d);
	}
	public VoipAddress getRemoteAddress() {
		long lNativePtr = getRemoteAddress(d);
		if (lNativePtr!=0) {
			return new VoipAddressImpl(lNativePtr); 
		} else {
			return null;
		}
	}
	public State getState() {
		return VoipCall.State.fromInt(getState(d));
	}
	public VoipCallParams getCurrentParamsCopy() {
		return new VoipCallParamsImpl(getCurrentParamsCopy(d));
	}
	public boolean equals(Object call) {
		return d == ((VoipCallImpl)call).d;
	}
	public void enableEchoLimiter(boolean enable) {
		enableEchoLimiter(d,enable);
	}
	public void enableCamera(boolean enabled) {
		enableCamera(d, enabled);
	}
	
	public int isVideoEmpty() {
		return isVideoEmpty(d);
	}
	
}
