
package com.pingshow.voip;

import com.pingshow.airecenter.Log;

public class P2P {

	public native int open(String myidx);
	public native int start(int w, int h, int rotate);
	public native String generate(String user);
	public native String jniParseOffer(String sdp);
	public native int jniParseAnswer(String sdp);
	public native int feed(byte yuv[]);
	public native void setWindow(Object wid, int index);
	public native int jniNumOfStreams();
	public native void jniUseSession(int h);
	public native int jniNumOfPkts(int index);
	public native void close();
	
	private int width;
	private int height;
	
	private static P2P pp=null;
	static public P2P getInstance()
	{
		return pp;
	}

	public boolean init(String myIdx, int w, int h) {
		try {
			System.loadLibrary("p2p");
			width=w;
			height=h;
			open(myIdx);
			pp=this;
		} catch (Throwable e) {
			Log.e("tmlvc Fail to loadlibrary libp2p");
		}
		return (pp!=null);
	}
	
	public void startEncoder(int rotate) {
		start(width,height,rotate);
	}
	
	public String generateDSP(String user) {
		return generate(user);
	}
	
	public String parseOffer(String sdp) {
		return jniParseOffer(sdp);
	}
	
	public int parseAnswer(String sdp) {
		return jniParseAnswer(sdp);
	}
	
	public void sendYUV(byte yuv[]) {
		feed(yuv);
	}
	
	public synchronized void setVideoWindow(Object wid, int index)
	{
		setWindow(wid, index);
	}
	
	public int numOfStreams() {
		return jniNumOfStreams();
	}
	
	public void useSession(int idx)
	{
		jniUseSession(idx);
	}
	
	public int getNumOfPackets(int index)
	{
		return jniNumOfPkts(index);
	}
	
	public void release() {
		try{
			close();
		}catch(Exception e){}
		pp=null;
	}
}
