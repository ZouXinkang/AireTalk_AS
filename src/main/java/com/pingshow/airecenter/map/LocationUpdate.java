package com.pingshow.airecenter.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.network.MyNet;
import com.pingshow.network.MySocket;
import com.pingshow.util.MyUtil;


public class LocationUpdate {

	private final int LOC_MIN_TIME = 10800000; // 3hr
	private final int LOC_MIN_DISTANCE = 500; // 500m
	
	static private MyLocationListener mll = null;
	private Location MyLocation = null;
	
	private MyPreference mPref;
	private Context mContext;
	private Handler mHandler;
	
	static int inst=0;
	
	public LocationUpdate(Context context, Handler handler, MySocket tcp_socket, MyPreference Pref, boolean autoRelease)
	{
		mContext=context;
		mHandler=handler;
		mPref=Pref;
		
		InitLocationMonitor(autoRelease);
		inst++;
	}
	
	public LocationUpdate(Context context, MyPreference Pref)
	{
		mContext=context;
		mPref=Pref;
	}

	private void InitLocationMonitor(boolean autoRelease) {
		try {
			LocationManager lmgr = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
			
			if (mPref.read("iso",null)==null)
				getMyLocFromIpAddress(false);
			
			if (lmgr != null) {
				if (mll != null)
				{
					Log.d("LOCATION_SERVICE removeUpdates(mll)");
					lmgr.removeUpdates(mll);
					inst--;
					Log.d("num mll="+inst);
				}
				
				Location LocationGPS=null, LocationNet=null;
				
				if (lmgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					try {
						Log.d("GPS_PROVIDER is enabled");
						LocationGPS = lmgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					} catch (Exception e) {
						Log.e("InitLocationMonitor Fail:" + e.getMessage());
					}
				}
			  
				if (lmgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
					try {
						Log.d("NETWORK_PROVIDER is enabled");
						LocationNet = lmgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					} catch (Exception e) {
						Log.e("InitLocationMonitor Fail:" + e.getMessage());
					}
				}
				
				long gpsTime=0, netTime=0;
				if (LocationGPS!=null)
					gpsTime=LocationGPS.getTime();
				if (LocationNet!=null)
					netTime=LocationNet.getTime();
				String bestProvider="";
	
				if (netTime>gpsTime)
				{
					bestProvider=LocationManager.NETWORK_PROVIDER;
					MyLocation=LocationNet;
				}else if (netTime<gpsTime){
					bestProvider=LocationManager.GPS_PROVIDER;
					MyLocation=LocationGPS;
				}
				
				if (bestProvider.length()>0)
				{
					mll = new MyLocationListener();
					lmgr.requestLocationUpdates(
							bestProvider,
							mPref.readInt("TrackTime", LOC_MIN_TIME),
							mPref.readInt("TrackDistance", LOC_MIN_DISTANCE),
							mll);
				}
			  
				if (MyLocation != null)
				{
					updatePreference((long) (MyLocation.getLatitude() * 1E6), (long) (MyLocation.getLongitude() * 1E6));
					uploadLocationToServer();
				}
				else
					getMyLocFromIpAddress(false);
				
				if (autoRelease)
				{
					mHandler.postDelayed(autoReleaseMll, 60000);
				}
			}
			else{
				getMyLocFromIpAddress(false);
			}
		}catch (Exception e) {
		}
	}

	Runnable autoReleaseMll=new Runnable(){
		public void run() {
			try {
				Class.forName("com.google.android.maps.MapActivity");
			}catch (ClassNotFoundException e) {
				LocationManager lmgr = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
				if (lmgr != null)
					if (mll != null)
					{
						lmgr.removeUpdates(mll);
						inst--;
						Log.d("num mll="+inst);
					}
				return;
			} catch (NoClassDefFoundError e) {
				LocationManager lmgr = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
				if (lmgr != null)
					if (mll != null)
					{
						lmgr.removeUpdates(mll);
						inst--;
						Log.d("num mll="+inst);
					}
				return;
			}
			if (MapViewLocation.getInstance() != null)
			{
				mHandler.postDelayed(autoReleaseMll, 60000);
				return;
			}			
			LocationManager lmgr = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
			if (lmgr != null) {
				if (mll != null)
				{
					lmgr.removeUpdates(mll);
					inst--;
					Log.d("InitLocationMonitor...Released");
					Log.d("num mll="+inst);
				}
				mll=null;
			}
		}
	};
	
	static private String getMyIp(String szURL)
	{
		String Return="";
		HttpURLConnection urlConnection=null;
		try{
			URL url = new URL(szURL);
			urlConnection = (HttpURLConnection) url.openConnection();
			java.io.InputStream is = urlConnection.getInputStream();
			String line = "";
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			while ((line = reader.readLine()) != null)
				if (line.length() > 0)
					Return = Return + trimBOM(line.trim());
		       	is.close();
		}
		catch(Exception e)
		{
		}
		finally {
		     if (urlConnection!=null) urlConnection.disconnect();
		}
		return Return;
	}
	
	static public String getAddressFromIp(String szURL) throws IOException{
		String Return="";
		HttpURLConnection urlConnection=null;
		try{
			URL url = new URL(szURL);
			urlConnection = (HttpURLConnection) url.openConnection();
			java.io.InputStream is = urlConnection.getInputStream();
			String line = "";
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			while ((line = reader.readLine()) != null)
				if (line.length() > 0)
					Return = Return + trimBOM(line.trim());
		       	is.close();
		}
		catch(Exception e)
		{
		}
		finally {
		     if (urlConnection!=null) urlConnection.disconnect();
		}
		return Return;
	}
	
	static private String trimBOM(String s)
	{
		while(s.length()>=1 && s.charAt(0)==0xFEFF)
			s=s.substring(1);
		return s;
	}

	//tml*** new get iso
	private final int totalGetGeos = 2;
	private volatile int countGetGeos = 0;
	public void getMyLocFromIpAddress(boolean wait) {
		final String getmyaddress_us = "http://74.3.165.66/onair/getgeo.php";
		final String getmyaddress_cn = "http://42.121.54.216/onair/getgeo.php";
		final String getmyaddress_default = "http://42.121.54.216/onair/getgeo.php";
		String[] getGeos = {getmyaddress_us, getmyaddress_cn};
		int getGeosZ = getGeos.length;
		gotOneIso = false;
		countGetGeos = 0;
		
		for (int i = 0; i < getGeosZ; i++) {
			int pos = i + 1;
			getMyLocFromIpAddressTHR(getGeos[i], pos);
		}
		
		if (wait)
			synchronized (lock_iso) {
				try {
					lock_iso.wait(3000);
				} catch (Exception e) {}
			}
		
		Log.d("getMyLocFromIpAddr done " + countGetGeos + gotOneIso);
		
//		final String getmyaddress="http://74.3.165.66/onair/getgeo.php";  //tml*** new get iso
//		final String getmyip = "http://icanhazip.com/";
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					Log.d("getAddressFromIp...");
//					String ipaddr=getMyIp(getmyip);
//					String response=getAddressFromIp(getmyaddress+ipaddr);
//					
//					JSONObject jsonObject;
//		            try {
//		                jsonObject = new JSONObject(response);
//
//		                String iso = jsonObject.getString("country_code").toLowerCase();
//		                String lat = jsonObject.getString("latitude");
//		                String lon = jsonObject.getString("longitude");
//		                Log.d("getMyLocFromIpAddr iso=" + iso + " lat=" + lat + " lon=" + lon);
//		                
//		                if (iso == null || iso == "") return;
//						//{"ip":"123.195.204.129","country_code":"TW","country_name":"Taiwan","region_code":"03","region_name":"T'ai-pei","city":"Taipei","zipcode":"","latitude":25.0392,"longitude":121.525,"metro_code":"","areacode":""}
//		                try {
//							mPref.write("iso", iso);
////							mPref.write("iso", "cn");  //test
//							updatePreference((long) (Double.parseDouble(lat) * 1E6), (long) (Double.parseDouble(lon) * 1E6));
//							uploadLocationToServer();
//						}catch (Exception e) {
//						}
//		                
//		            } catch (JSONException e) {
//		                e.printStackTrace();
//		            } catch (Exception e) {
//		                e.printStackTrace();
//		            }
//				} catch (IOException e) {
//				}
//			}
//		}).start();
	}
	//tml*** new get iso
	private final Object lock_iso = new Object();
	private volatile boolean gotOneIso = false;
	public void getMyLocFromIpAddressTHR(final String addr, final int addrN) {
		(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Log.d("getMyLocFromIp-" + addrN + " " + addr);
					String response = getAddressFromIp(addr);
					
					JSONObject jsonObject;
		            try {
		                jsonObject = new JSONObject(response);

		                String iso = jsonObject.getString("country_code").toLowerCase();
		                Log.d("getMyLocFromIpAddr-" + addrN + " iso=" + iso);

		                if (iso == null || iso == "") {
			                Log.e("getMyLocFromIpAddr-" + addrN + " !@#$0 empty iso");
			                if (!gotOneIso) {
				                countGetGeos++;
								if (countGetGeos >= totalGetGeos) {
									Log.d("getMyLocFromIpAddr all getGeos tried");
					                synchronized (lock_iso) {
					                	lock_iso.notifyAll();
									}
								}
			                }
			                return;
		                }
		                
		                if (!gotOneIso) {
							try {
				                Log.d("getMyLocFromIpAddr-" + addrN + " save > iso=" + iso);
								mPref.write("iso", iso);
//								mPref.write("iso", "cn");  //test
								gotOneIso = true;
				                synchronized (lock_iso) {
				                	lock_iso.notifyAll();
								}
								uploadLocationToServer();
							} catch (Exception e) {
				                Log.e("getMyLocFromIpAddr-" + addrN + " !@#$1 " + e.getMessage());
							}
		                }
		                
		            } catch (JSONException e) {
		                Log.e("getMyLocFromIpAddr-" + addrN + " !@#$2 " + e.getMessage());
		            } catch (Exception e) {
		                Log.e("getMyLocFromIpAddr-" + addrN + " !@#$3 " + e.getMessage());
		            }
				} catch (IOException e) {
	                Log.e("getMyLocFromIpAddr-" + addrN + " !@#$4 " + e.getMessage());
				}

                if (!gotOneIso) {
    				countGetGeos++;
    				if (countGetGeos >= totalGetGeos) {
    					Log.d("getMyLocFromIpAddr all getGeos tried");
    	                synchronized (lock_iso) {
    	                	lock_iso.notifyAll();
    					}
    				}
                }
			}
		})).start();
	}

	//tml*** xcountry sip
	public void getMyRoamId() {
//		(new Thread(new Runnable() {
//			@Override
//			public void run() {
//				String Return = "";
//				try {
//					int count = 0;
//					MyNet net = new MyNet(mContext);
//					while (count++ <= 3) {
//						Return = net.doPost("getmyroamid.php", "", null);
//						if (Return.startsWith("roam")) break;
//						MyUtil.Sleep(1500);
//					}
//					
//					if (Return.startsWith("roam") && Return.contains(":")) {
//						String myCurRoamId = mPref.read("myRoamId", "");
//						String[] myRoamId = Return.split(":");
//						
//						mPref.write("myRoamId", myRoamId[1]);
//						if (AireJupiter.getInstance() != null) {
//							if (AireJupiter.getInstance().tcpSocket != null
//									&& AireJupiter.getInstance().tcpSocket.isLogged(false)) {
//								AireJupiter.getInstance().tcpSocket.updateMyRoamSip(myRoamId[1]);
//							}
//						}
//					}
//				} catch (Exception e) {
//					Log.e("getmyroamid.php !@#$ " + e.getMessage());
//				}
//			}
//		})).start();
	}
	
	private void updatePreference(long latitude, long longitude)
	{
		Log.d("longitude:"+longitude+", latitude:"+latitude);
		
		if (latitude != 0 && longitude != 0) {
			mPref.writeLong("longitude", longitude);
			mPref.writeLong("latitude", latitude);
			try{
				mPref.writeFloat("accuracy", MyLocation.getAccuracy());
			}catch(Exception e){
			}
			
			try {
	             Class.forName("com.google.android.maps.MapActivity");
	     	}catch (ClassNotFoundException e) {
	            return;
	     	}catch (NoClassDefFoundError e){
				return;
			}
	     	
	     	try {
				if (MapViewLocation.getInstance() != null) {
					Intent intent = new Intent();
					intent.setAction(MapViewLocation.MyLocation_Change);
					mContext.sendBroadcast(intent);
				}
			}catch (Exception e){
			}
		}
	}

	class MyLocationListener implements LocationListener {
		public void onLocationChanged(Location location) {
			Log.d("onLocationChanged...");
			if (location != null) {
				MyLocation = location;

				updatePreference((long) (MyLocation.getLatitude() * 1E6), (long) (MyLocation.getLongitude() * 1E6));
				uploadLocationToServer();
				
				try {
					Class.forName("com.google.android.maps.MapActivity");
				}catch (ClassNotFoundException e) {
					autoReleaseMll.run();
					return;
				}catch(NoClassDefFoundError e){
					autoReleaseMll.run();
					return;
				}
				
				if (MapViewLocation.getInstance() == null)
					autoReleaseMll.run();
			}
		}

		public void onProviderDisabled(String provider) {
			MyLocation = null;
			InitLocationMonitor(true);
			Log.d("onProviderDisabled..." + provider);
		}

		public void onProviderEnabled(String provider) {
			InitLocationMonitor(true);
			Log.d("onProviderEnabled...");
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}

	public void uploadLocationToServer() {
		Thread updateLocation_Thread = new Thread(new Runnable() {
			public void run() {
				boolean hasGoogleMap=true;
				boolean longTime=true;
				
				try {
					Class.forName("com.google.android.maps.MapActivity");
					if (hasGoogleMap)
					{
						if (MapViewLocation.getInstance() == null)
							longTime=true;
						else
							longTime=false;
					}
				}catch (ClassNotFoundException e) {
					hasGoogleMap=false;
				}catch(NoClassDefFoundError e){
					hasGoogleMap=false;
				}
				
				long last = mPref.readLong("last_time_update_location", 0);
				long now = new Date().getTime();
				if (now - last < (longTime?3600000:10000)) // 1 hour or 10 sec
					return;// no need to update
				
				Log.d("uploadLocationToServer...");
				
				mPref.writeLong("last_time_update_location", now);//alec
				
				try {
					long latitude;
					long longitude;
					if (MyLocation != null) {
						latitude = (long) (MyLocation.getLatitude() * 1E6);
						longitude = (long) (MyLocation.getLongitude() * 1E6);
						mPref.writeFloat("accuracy", MyLocation.getAccuracy());
					}else{
						latitude=mPref.readLong("latitude", Global.DEFAULT_LAT);
						longitude=mPref.readLong("longitude", Global.DEFAULT_LON);
					}
					if (latitude != 0 && longitude != 0 && mPref.readBoolean("AireRegistered")) {
						
						try{
							int myIdx=Integer.parseInt(mPref.read("myID","0"),16);
							MyNet net = new MyNet(mContext);
							String Return=net.doPost("updatelocation_aire.php", "idx=" + myIdx
		    						+"&lat=" + latitude
		    						+"&lon=" + longitude, null);
							Log.d("Return="+Return);
						}catch(Exception e){
						}
		      
						try {
							Class.forName("com.google.android.maps.MapActivity");
						}catch (ClassNotFoundException e) {
							return;
						}catch (NoClassDefFoundError e){
							return;
						}
						if (MapViewLocation.getInstance() != null) {
							Intent intent = new Intent();
							intent.setAction(MapViewLocation.MyLocation_Change);
							mContext.sendBroadcast(intent);
						}
					}
				} catch (Exception e) {
				}
			}
		}, "location update");
		updateLocation_Thread.start();
	}
	
	public void destroy()
	{
		autoReleaseMll.run();
	}
}
