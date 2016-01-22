package com.pingshow.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.Global;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.voip.AireVenus;

public class NetworkMonitor extends BroadcastReceiver {
	
	TelephonyManager telMgr;
	SignalStrength sigstrength;
	int curr_nettype = NetInfo.DISCONNECTED;		//simon 061011
	int prev_nettype = NetInfo.DISCONNECTED;		//simon 061011

	public boolean checkNetworkConnection(Context context) {
		NetInfo ni=new NetInfo(context);
		
		boolean connected = false;
		if (ni.isConnected()) {
			try { Thread.sleep(100); } catch (Exception e) {}
			
			NetInfo ni1 = new NetInfo(context);
			if (ni.netType == ni1.netType && ni1.isConnected()) {
				try { Thread.sleep(100); } catch (Exception e) {}
				NetInfo ni2 = new NetInfo(context);
				if (ni1.netType == ni2.netType && ni2.isConnected()) {
					connected = true;
					prev_nettype = curr_nettype;	//simon 0601011
					curr_nettype = ni1.netType;		//simon 061011
				} 
			}
		}
		else{
			connected = false;
		}
		return connected;
    }
    
    /*simon 061011 
     * added to check nettype
     */
    public boolean netTypeChanged() {
    	if (curr_nettype > 0) {
			if (prev_nettype != curr_nettype) {
				return true;
			}
    	}
    	return false;
    }
    
	public void onReceive(Context context, Intent intent) {
      
		if (AireJupiter.getInstance() == null)
			return;
		
		//li*** change the sip-register flag when network was changed；
		if(netTypeChanged() && AireVenus.instance() != null){
			if(AireVenus.instance().inCall){
				AireVenus.instance().registered = false;
			}else{
				//stop venus
				context.stopService(new Intent(context, AireVenus.class));
			}
		}
		
		MyPreference myPrf=new MyPreference(AireJupiter.instance);
		
		if (checkNetworkConnection(context)) 
		{
			myPrf.write("connected", 1);

			Intent it = new Intent(Global.Action_InternalCMD);
			it.putExtra("Command", Global.CMD_RECONNECT_SOCKET);
			it.putExtra("netchanged", netTypeChanged());
			it.putExtra("curr_nettype", curr_nettype);
			it.putExtra("prev_nettype", prev_nettype);
			context.sendBroadcast(it);
			Log.d("Network is on!");
		}
		else
		{
			myPrf.write("connected", 0);
	    	Intent it = new Intent(Global.Action_InternalCMD);
			it.putExtra("Command", Global.CMD_SUDDENLY_NO_NETWORK);
			context.sendBroadcast(it);
			Log.d("Network is off!");
		}
	}
}