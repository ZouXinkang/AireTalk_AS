
package com.pingshow.codec;

import com.pingshow.amper.Log;

public class Scodec {
	static private boolean isloaded;
	private int typeEncoder;

	public boolean load(int type, int mode) {
		typeEncoder=type;
		try {
			System.loadLibrary("scodec");
			if (open(typeEncoder,mode)==1)
				isloaded=true;
		} catch (Throwable e) {
			Log.e("Fail to loadlibrary libscodec");
		}
		return isloaded;
	}
 
	public native int open(int type, int mode);
	public native int decode(byte encoded[], int offset, short lin[], int size, int offset_pcm);
	public native int encode(short lin[], int offset, byte encoded[], int offset2, int size);
	public native void close(int type);
	
	public void release() {
		try{
			close(typeEncoder);
		}catch(Exception e){}
		isloaded=false;
	}
	
	public boolean isReady() {
		return isloaded;
	}
}
