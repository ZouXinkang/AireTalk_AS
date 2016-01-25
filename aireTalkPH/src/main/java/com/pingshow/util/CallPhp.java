package com.pingshow.util;

import java.io.IOException;

import android.content.Context;
import android.widget.Toast;

import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.Log;
import com.pingshow.amper.MainActivity;
import com.pingshow.amper.MyPreference;
import com.pingshow.network.MyNet;
import com.pingshow.voip.DialerActivity;

public class CallPhp {
	/*
	 * 新广播
	 */
	public static void callKickallPhp(Context context) {
		try {
			MyPreference mPref=new MyPreference(context);
			//bree  kickall.php
			MyNet net = new MyNet(context);
			String domain;
			
			if (mPref.readBoolean("incomingChatroom")) {
				domain = mPref.read("joinSipAddress",
						AireJupiter.myConfSipServer_default);
			} else {
				domain = mPref.read("conferenceSipServer",
						AireJupiter.myConfSipServer_default);
				if (AireJupiter.getInstance() != null) {
					domain = AireJupiter.getInstance().getIsoConf(domain); 
				}
			}
			String phpip = AireJupiter.myPhpServer_default;
			if (AireJupiter.getInstance() != null) {
				phpip = AireJupiter.getInstance().getIsoPhp(0, true,
						"74.3.165.66");
			}
			int room = mPref.readInt("ChatroomHostIdx",0);
			String roomNumber = String.format("%07d", room);
				String Return = net.doAnyPostHttp("http://" + phpip
						+ "/onair/conference/customer/kickall.php", "room="
						+ roomNumber + "&ip=61.136.101.118");
				Log.d("kickall.php Return=" + Return);
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				Log.d("kickall.php Return=error");
				e2.printStackTrace();
			}
	}
	/*
	 * 新广播
	 */
	public static void calllock(String myId,Context context) {
		try {
			// bree lock.php
			MyPreference mPref=new MyPreference(context);
			MyNet net = new MyNet(context);
			String domain;
			if (mPref.readBoolean("incomingChatroom")) {
				domain = mPref.read("joinSipAddress",
						AireJupiter.myConfSipServer_default);
			} else {
				domain = mPref.read("conferenceSipServer",
						AireJupiter.myConfSipServer_default);
				if (AireJupiter.getInstance() != null) {
					domain = AireJupiter.getInstance().getIsoConf(domain);
				}
			}
			String phpip = AireJupiter.myPhpServer_default;
			if (AireJupiter.getInstance() != null) {
				phpip = AireJupiter.getInstance().getIsoPhp(0, true,
						"74.3.165.66");
			}
			int room = mPref.readInt("ChatroomHostIdx", 0);
			String roomNumber = String.format("%07d", room);
			String Return = net.doAnyPostHttp("http://" + phpip
					+ "/onair/conference/customer/lock.php", "room="
					+ roomNumber + "&ip=61.136.101.118" + "&id=" + myId);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			Log.d("lock.php Return=error");
			e2.printStackTrace();
		}
	}
	public static void calllock2(String myId,Context context) {
		try {
			// bree lock.php
			MyPreference mPref=new MyPreference(context);
			MyNet net = new MyNet(context);
			String domain;
			
			if (mPref.readBoolean("incomingChatroom")) {
				domain = mPref.read("joinSipAddress",
						AireJupiter.myConfSipServer_default);
			} else {
				domain = mPref.read("conferenceSipServer",
						AireJupiter.myConfSipServer_default);
				if (AireJupiter.getInstance() != null) {
					domain = AireJupiter.getInstance().getIsoConf(domain);
				}
			}
			String phpip = AireJupiter.myPhpServer_default;
			if (AireJupiter.getInstance() != null) {
				phpip = AireJupiter.getInstance().getIsoPhp(0, true,
						"74.3.165.66");
			}
			int room = mPref.readInt("ChatroomHostIdx", 0);
			String roomNumber = String.format("%07d", room);
			String Return = net.doAnyPostHttp("http://" + phpip
					+ "/onair/conference/customer/lock2.php", "room="
							+ roomNumber + "&ip=61.136.101.118" + "&id=" + myId);
			Log.d("calllock2 Return=" + Return);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			Log.d("calllock2 Return=error");
			e2.printStackTrace();
		}
	}
	
	/*
	 * 心广播
	 */
	public static void callHangupPhp(String uuid,Context context){
		try {
			MyPreference mPref=new MyPreference(context);
			MyNet net = new MyNet(context);
			int room = mPref.readInt("ChatroomHostIdx");
//			if (Integer.parseInt(mPref.read("myID", "0"), 16) != room)
//				return ; // tml*** only hostkicker
			String roomNumber = String.format("%07d", room);
			// tml*** china ip
			String phpip = AireJupiter.myPhpServer_default;
			if (AireJupiter.getInstance() != null) {
				phpip = AireJupiter.getInstance().getIsoPhp(0, true,
						"74.3.165.66");
			}

			String Return = net.doAnyPostHttp("http://" + phpip
					+ "/onair/conference/customer/hangup.php", "room="
					+ roomNumber + "&ip=61.136.101.118" + "&uuid=" + uuid);

			Log.d("HangupPhp:"+Return+"uuid:"+uuid);
		} catch (Exception e) {
			Log.e("da4 " + e.getMessage());
		}
	}
	/*
	 * 心广播
	 */
	public static void callHangupPhp2(String uuid,Context context){
		try {
			MyPreference mPref=new MyPreference(context);
			MyNet net = new MyNet(context);
			int room = mPref.readInt("ChatroomHostIdx");
//			if (Integer.parseInt(mPref.read("myID", "0"), 16) != room)
//				return ; // tml*** only hostkicker
			String roomNumber = String.format("%07d", room);
			// tml*** china ip
			String phpip = AireJupiter.myPhpServer_default;
			if (AireJupiter.getInstance() != null) {
				phpip = AireJupiter.getInstance().getIsoPhp(0, true,
						"74.3.165.66");
			}

			String Return = net.doAnyPostHttp("http://" + phpip
					+ "/onair/conference/customer/hangup2.php", "room="
					+ roomNumber + "&ip=61.136.101.118" + "&uuid=" + uuid);

			Log.d("HangupPhp2:"+Return+"uuid:"+uuid);
		} catch (Exception e) {
			Log.e("da4 " + e.getMessage());
		}
	}
	
}
