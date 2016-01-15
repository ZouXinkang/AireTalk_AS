
package com.pingshow.network;

import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;

public class upnpc {
	private MyPreference mPref;
	private static int aud_port;
	private static int vid_port;
	
	public native int Init();
	public native void Uninit();
	public native short Get(int type);
	
	public upnpc(MyPreference pref)
	{
		mPref=pref;
		try {
			System.loadLibrary("upnp");
		} catch (Throwable e) {
			Log.e("Fail to loadlibrary libupnp");
		}
	}
	
	public void start() {
		new Thread("upnp init"){
			public void run(){
				try {
					mPref.write("doingUPNP",true);
					if (Init()==1)
					{
						aud_port=Get(0);
						vid_port=Get(1);
						
						Log.d("voip.UPNP *** "+aud_port+" ***");
						Log.d("voip.UPNP *** "+vid_port+" ***");
						
						mPref.write("audio_local_port",aud_port);
						mPref.write("video_local_port",vid_port);
					}
					else
					{
						Log.d("voip.UPNP *** 0 ***, lib Init() != 1");
						mPref.write("audio_local_port",0);
						mPref.write("video_local_port",0);
					}
					
				} catch (Exception e) {
					Log.e("Fail to do upnp");
					mPref.write("audio_local_port",0);
					mPref.write("video_local_port",0);
				}
				mPref.delect("doingUPNP");
			}
		}.start();
	}
	
	public int read_port(int type) {
		return Get(type);
	}
	
	public void release(){
		new Thread("free"){
			public void run(){
				try {
					Uninit();
				} catch (Exception e) {
					Log.e("Fail to delete port");
				}
			}
		}.start();
	}
}
