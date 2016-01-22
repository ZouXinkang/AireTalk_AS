
package com.pingshow.codec;

import com.pingshow.amper.Log;

public class Ncodec {
	static private boolean isloaded;
	
	public boolean load() {
		try {
			System.loadLibrary("ncodec");
			if (open(6400)==1)
				isloaded=true;
		} catch (Throwable e) {
			Log.e("Fail to loadlibrary libncodec");
		}
		return isloaded;
	}
 
	public native int open(int bitrate);
	public native int decode(byte encoded[], int offset, short lin[], int size, int offset_pcm);
	public native int encode(short lin[], int offset, byte encoded[], int offset2, int size);
	public native void close();
	
	public void release() {
		try{
			close();
		} catch (Throwable e) {
			Log.e("Fail to close libncodec");
		}
		isloaded=false;
	}
	
	public boolean isReady() {
		return isloaded;
	}
}
