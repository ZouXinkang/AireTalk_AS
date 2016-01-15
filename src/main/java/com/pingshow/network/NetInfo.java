package com.pingshow.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public class NetInfo {

	public boolean netExists=false;
	public boolean isRoaming=false;
	public int netType=0;
	
	public static int WIFI=3;
	public static int MOBILE_3G=2;
	public static int MOBILE_OTHER=1;
	public static int DISCONNECTED=0;
	
	public NetInfo(Context context)
	{
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo ni = (NetworkInfo) cm.getActiveNetworkInfo();         
		if( ni != null && ni.isConnected() ) {
			netExists = true;		
			String Type = ni.getTypeName().toLowerCase();
			
			if(Type.equals("wifi")) {
				netType=WIFI;//WiFi;
			}
			else if(Type.equals("mobile")) {
				int netSubType = ni.getSubtype();
				if (netSubType == 13)//NETWORK_TYPE_LTE
					netType=WIFI;
				else if (netSubType == TelephonyManager.NETWORK_TYPE_UMTS 
					|| netSubType == TelephonyManager.NETWORK_TYPE_HSDPA
					|| netSubType == TelephonyManager.NETWORK_TYPE_HSPA
					|| netSubType == TelephonyManager.NETWORK_TYPE_HSUPA)
					netType=MOBILE_3G;
				else
					netType=MOBILE_OTHER;
			}
			else if(Type.contains("wimax") || Type.contains("lte")) {
				netType=WIFI;
			}else
				netType=MOBILE_OTHER;
	    } else {
	    	netExists = false;
	    	netType=DISCONNECTED;
	    }
		netType=WIFI;//alec always WIFI for STB
	}
	
	public boolean isConnected() {
    	return netExists; 
    }
    public long ipToLong(String strIp){
        long[] ip = new long[4];
        int position1 = strIp.indexOf(".");
        int position2 = strIp.indexOf(".", position1 + 1);
        int position3 = strIp.indexOf(".", position2 + 1);

        ip[0] = Long.parseLong(strIp.substring(0, position1));
        ip[1] = Long.parseLong(strIp.substring(position1+1, position2));
        ip[2] = Long.parseLong(strIp.substring(position2+1, position3));
        ip[3] = Long.parseLong(strIp.substring(position3+1));
        return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
    }
    public String longToIP(long longIp){
        StringBuffer sb = new StringBuffer("");
        sb.append(String.valueOf((longIp >>> 24)));
        sb.append(".");

        sb.append(String.valueOf((longIp & 0x00FFFFFF) >>> 16));
        sb.append(".");

        sb.append(String.valueOf((longIp & 0x0000FFFF) >>> 8));
        sb.append(".");

        sb.append(String.valueOf((longIp & 0x000000FF)));
        return sb.toString();
    }
    
    public String longToIPForServer(long longIp){
        StringBuffer sb = new StringBuffer("");
        
        sb.append(String.valueOf((longIp & 0x000000FF)));
        sb.append(".");
        
        sb.append(String.valueOf((longIp & 0x0000FFFF) >>> 8));
        sb.append(".");
        
        sb.append(String.valueOf((longIp & 0x00FFFFFF) >>> 16));
        sb.append(".");
        
        sb.append(String.valueOf((longIp >>> 24)));
        return sb.toString();
    }
}
