
package com.pingshow.voip.core;

import com.pingshow.voip.core.VoipCallParams;
import com.pingshow.voip.core.VoipCallParamsImpl;


public class VoipCallParamsImpl implements VoipCallParams {
	protected final long d;
	
	public VoipCallParamsImpl(long d) {
		this.d = d;
	}
	private native void enableVideo(long nativePtr, boolean b);
	private native boolean getVideoEnabled(long d);
	private native long getCurrentParamsCopy(long d);
	private native void audioBandwidth(long d, int bw);
	private native void destroy(long d);
	
	
	public boolean getVideoEnabled() {
		return getVideoEnabled(d);
	}

	public void setVideoEnabled(boolean b) {
		enableVideo(d, b);
	}
	
	@Override
	protected void finalize() throws Throwable {
		destroy(d);
		super.finalize();
	}
	
	public VoipCallParams getCurrentParamsCopy() {
		return new VoipCallParamsImpl(getCurrentParamsCopy(d));
	}
}
