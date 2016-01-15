package com.pingshow.voip.core;

class PayloadTypeImpl implements PayloadType {

	protected final long nativePtr;
	
	private native String toString(long ptr);
	private native String getMime(long ptr);
	private native int getRate(long ptr);

	protected PayloadTypeImpl(long aNativePtr)  {
		nativePtr = aNativePtr;
	}
	
	public int getRate() {
		return getRate(nativePtr);
	}

	public String getMime() {
		return getMime(nativePtr);
	}
	
	public String toString() {
		return toString(nativePtr);
	}
}
