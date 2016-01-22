package com.pingshow.amper.map;

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

import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
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
				getMyLocFromIpAddress();
			
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
						Log.d("InitLocationMonitor GPS_PROVIDER is enabled");
						LocationGPS = lmgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					} catch (Exception e) {
						Log.e("InitLocationMonitor1 !@#$" + e.getMessage());
					}
				}
			  
				if (lmgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
					try {
						Log.d("InitLocationMonitor NETWORK_PROVIDER is enabled");
						LocationNet = lmgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					} catch (Exception e) {
						Log.e("InitLocationMonitor2 !@#$" + e.getMessage());
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
					updatePreference((long) (MyLocation.getLatitude() * 1E6), (long) (MyLocation.getLongitude() * 1E6), "InitLocationMonitor");
					uploadLocationToServer();
				}
				else
					getMyLocFromIpAddress();
				
				if (autoRelease)
				{
					mHandler.postDelayed(autoReleaseMll, 60000);
				}
			}
			else{
				getMyLocFromIpAddress();
			}
		}catch (Exception e) {}
	}

	Runnable autoReleaseMll=new Runnable(){
		public void run() {
			try {
				Log.w("checkmap ? com.google.android.maps");
				Class.forName("com.google.android.maps.MapActivity");
			} catch (ClassNotFoundException e) {
				LocationManager lmgr = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
				if (lmgr != null)
					if (mll != null)
					{
						lmgr.removeUpdates(mll);
						inst--;
						Log.d("num mll="+inst);
					}
				Log.w("!@#$ autoReleaseMll1 com.google.android.maps");
				return;
			} catch (NoClassDefFoundError e) {
				LocationManager lmgr = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
				if (lmgr != null)
					if (mll != null)
					{
						lmgr.removeUpdates(mll);
						inst--;
						Log.d("num mll="+inst);
					}
				Log.w("!@#$ autoReleaseMll2 com.google.android.maps");
				return;
			}
			
			long timeout=mPref.readLong("SpeedupMapMonitor",0);
			if (new Date().getTime()<timeout)
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
	
	public void getMyLocFromIpAddress() {
		final String getmyaddress="http://74.3.165.66/onair/getgeo.php";
		final String getmyip = "http://icanhazip.com/";
		 
		(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Log.d("getAddressFromIp...");
					String ipaddr=getMyIp(getmyip);
					String response=getAddressFromIp(getmyaddress+ipaddr);
					
					JSONObject jsonObject;
		            try {
		                jsonObject = new JSONObject(response);

		                String iso = jsonObject.getString("country_code").toLowerCase();
		                String lat = jsonObject.getString("latitude");
		                String lon = jsonObject.getString("longitude");
		                Log.d("getMyLocFromIpAddr iso=" + iso + " lat=" + lat + " lon=" + lon);

		                if (iso == null || iso == "") return;
						//{"ip":"123.195.204.129","country_code":"TW","country_name":"Taiwan","region_code":"03","region_name":"T'ai-pei","city":"Taipei","zipcode":"","latitude":25.0392,"longitude":121.525,"metro_code":"","areacode":""}
		                try {
							mPref.write("iso", iso);
//							mPref.write("iso", "cn");
							updatePreference((long) (Double.parseDouble(lat) * 1E6), (long) (Double.parseDouble(lon) * 1E6), "getMyLocFromIpAddress");
							uploadLocationToServer();
						}catch (Exception e) {}
		                
		            } catch (JSONException e) {
		                Log.e("getMyLocFromIpAddress1 !@#$ " + e.getMessage());
		            } catch (Exception e) {
		                Log.e("getMyLocFromIpAddress2 !@#$ " + e.getMessage());
		            }
				} catch (IOException e) {
	                Log.e("getMyLocFromIpAddress3 !@#$ " + e.getMessage());
				}
			}
		})).start();
		
	}

	//tml*** xcountry sip
	/*
	 * 0-10
	 * 3 : China
	 * 7 : USA
	 * 10 : Taiwan
	 */
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
	
	private void updatePreference(long latitude, long longitude, String from)
	{
		Log.d("updateLocPreference (" + from + ") longitude:"+longitude+", latitude:"+latitude);
		
		if (latitude != 0 && longitude != 0) {
			mPref.writeLong("longitude", longitude);
			mPref.writeLong("latitude", latitude);
			try{
				mPref.writeFloat("accuracy", MyLocation.getAccuracy());
			}catch(Exception e){}
			
			try {
				Log.w("checkmap ? com.google.android.maps");
				Class.forName("com.google.android.maps.MapActivity");
	     	} catch (ClassNotFoundException e) {
				Log.w("!@#$ updatePreference1 com.google.android.maps");
	            return;
	     	} catch (NoClassDefFoundError e) {
				Log.w("!@#$ updatePreference2 com.google.android.maps");
				return;
			} catch (Error e) {
				Log.w("!@#$ updatePreference3 com.google.android.maps");
				return;
			}
			//tml*** check google map
			boolean isCN = mPref.read("iso").equals("cn");
			boolean hasGoogleMaps = MyUtil.hasGoogleMap(!isCN, mContext, "updatePreference");
			if (!hasGoogleMaps) {
				return;
			}
			
	     	try {
				if (MapViewLocation.getInstance() != null) {
					Intent intent = new Intent();
					intent.setAction(MapViewLocation.MyLocation_Change);
					mContext.sendBroadcast(intent);
				}
			} catch (Exception e) {}
		}
	}

	class MyLocationListener implements LocationListener {
		public void onLocationChanged(Location location) {
			Log.d("onLocationChanged...");
			if (location != null) {
				MyLocation = location;

				updatePreference((long) (MyLocation.getLatitude() * 1E6), (long) (MyLocation.getLongitude() * 1E6), "MyLocationListener");
				uploadLocationToServer();
				
				try {
					Log.w("checkmap ? com.google.android.maps");
					Class.forName("com.google.android.maps.MapActivity");
				} catch (ClassNotFoundException e) {
					autoReleaseMll.run();
					Log.w("!@#$ MyLocationListener1 com.google.android.maps");
					return;
				} catch (NoClassDefFoundError e) {
					autoReleaseMll.run();
					Log.w("!@#$ MyLocationListener2 com.google.android.maps");
					return;
				}
				
				long timeout=mPref.readLong("SpeedupMapMonitor",0);
				if (new Date().getTime()>timeout)
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
				boolean longTime=true;
				
				try {
					Log.w("checkmap ? com.google.android.maps");
					Class.forName("com.google.android.maps.MapActivity");
				} catch (ClassNotFoundException e) {
					Log.w("!@#$ uploadLocationToServer1 com.google.android.maps");
				} catch (NoClassDefFoundError e) {
					Log.w("!@#$ uploadLocationToServer2 com.google.android.maps");
				} catch (Error e) {
					Log.w("!@#$ uploadLocationToServer3 com.google.android.maps");
				}
				
				long timeout=mPref.readLong("SpeedupMapMonitor",0);
				if (new Date().getTime()<timeout)
					longTime=false;
				else
					longTime=true;
				
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
						latitude=mPref.readLong("latitude", 39976279);
						longitude=mPref.readLong("longitude", 116349386);
					}
					if (latitude != 0 && longitude != 0 && mPref.readBoolean("Registered")) {
						
						try{
							int myIdx=Integer.parseInt(mPref.read("myID","0"),16);
							MyNet net = new MyNet(mContext);
							String Return=net.doPost("updatelocation_aire.php", "idx=" + myIdx
		    						+"&lat=" + latitude
		    						+"&lon=" + longitude, null);
							Log.d("Return="+Return);
						}catch(Exception e){}
						
						try {
							Log.w("checkmap ? com.google.android.maps");
							Class.forName("com.google.android.maps.MapActivity");
						} catch (ClassNotFoundException e) {
							Log.w("!@#$ uploadLocationToServer4 com.google.android.maps");
							return;
						} catch (NoClassDefFoundError e) {
							Log.w("!@#$ uploadLocationToServer5 com.google.android.maps");
							return;
						} catch (Error e) {
							Log.w("!@#$ uploadLocationToServer6 com.google.android.maps");
							return;
						}
						//tml*** check google map
						boolean isCN = mPref.read("iso").equals("cn");
						boolean hasGoogleMaps = MyUtil.hasGoogleMap(!isCN, mContext, "uploadLocationToServer");
						if (!hasGoogleMaps) {
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
