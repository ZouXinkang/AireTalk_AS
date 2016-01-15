package com.pingshow.airecenter.map.bd;

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
import android.os.Binder;
import android.os.Handler;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.MKGeneralListener;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MKEvent;
import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.pingshow.airecenter.BDMapViewLocation;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.network.MyNet;
import com.pingshow.network.MySocket;

public class BDLocationUpdate extends Binder implements IGetLocationService{

	private LocationClient mLocationClient;
	private BMapManager manager;
	private MyLocationListener myListener;
	private GeoPoint mygeo;
	
	private MyPreference mPref;
	private Context mContext;
	private Handler mHandler;

	static int inst = 0;

	public BDLocationUpdate(Context context, Handler handler,
			MySocket tcp_socket, MyPreference Pref, boolean autoRelease) {
		mContext = context;
		mHandler = handler;
		mPref = Pref;

		InitLocationMonitor(autoRelease);
		inst++;
	}

	public BDLocationUpdate(Context context, MyPreference Pref) {
		mContext = context;
		mPref = Pref;
	}

	private void InitLocationMonitor(boolean autoRelease) {
		startLocation();
		refreshLocation();
		if (mPref.read("iso", null) == null)
			getMyLocFromIpAddress();
		
		mygeo = getMyGeo();

		if (mygeo == null) {
			refreshLocation();
			mygeo = getMyGeo();
		}

		if (mygeo != null) {
			Log.d("BDlocation update,getmygeo" + mygeo.getLatitudeE6());
			updatePreference((long) (mygeo.getLatitudeE6()),
					(long) (mygeo.getLongitudeE6()));
			uploadLocationToServer();
		} else
			getMyLocFromIpAddress();
		if (autoRelease)
		{
			mHandler.postDelayed(autoReleaseMll, 60000);
		}
	}

	Runnable autoReleaseMll=new Runnable(){
		public void run() {
			if (BDMapViewLocation.getInstance() != null)
			{
				mHandler.postDelayed(autoReleaseMll, 60000);
				return;
			}			
			stopListener();
		}
	};

	public void startLocation() {
		initManager();
		mLocationClient = new LocationClient(mContext);
		// 定位的参数信息
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);
		option.setAddrType("all");// 返回的定位结果包含地址信息
		option.setCoorType("bd09ll");// 返回的定位结果是百度经纬度,默认值gcj02
		option.setScanSpan(30000);// 设置发起定位请求的间隔时间为5000ms
		option.disableCache(true);// 禁止启用缓存定位
		option.setPoiNumber(5); // 最多返回POI个数
		option.setPoiDistance(1000); // poi查询距离
		option.setPoiExtraInfo(true); // 是否需要POI的电话和地址等详细信息
		mLocationClient.setLocOption(option);
		myListener = new MyLocationListener();
		mLocationClient.registerLocationListener(myListener);
		mLocationClient.start();// 将开启与获取位置分开，就可以尽量的在后面的使用中获取到位置。
		Log.d("Location start@@@@@@@@");
		refreshLocation();
	}

	public void refreshLocation() {
		if (mLocationClient != null && mLocationClient.isStarted()) {
			mLocationClient.requestLocation();
			Log.i("location update@@@@@@@ ");
		}
	}

	public GeoPoint getMyGeo() {
		return mygeo;
	}

	public class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			if (location == null){
				int type=mLocationClient.requestLocation();
				Log.d("first request false"+type);
			}
			LocationData data = new LocationData();
			data.latitude = location.getLatitude();
			data.longitude = location.getLongitude();
			int latitude = (int) (data.latitude * 1E6);
			int longitude = (int) (data.longitude * 1E6);
			Log.d("LocationListener worked@@@@@@@@@@" + latitude + ":" + longitude);
			mygeo = new GeoPoint(latitude, longitude);
			Log.d("BDlocation listener update,getmygeo" + mygeo.getLatitudeE6());
			updatePreference((long) (mygeo.getLatitudeE6()),
					(long) (mygeo.getLongitudeE6()));
			uploadLocationToServer();
			if (BDMapViewLocation.getInstance() == null)
				autoReleaseMll.run();
		}

		public void onReceivePoi(BDLocation poiLocation) {
			if (poiLocation == null) {
				return;
			}

		}
	}
	
	/**
	 * 停止，减少资源消耗
	 */
	public void stopListener() {
		if (mLocationClient != null && mLocationClient.isStarted()) {
			mLocationClient.unRegisterLocationListener(myListener);
			mLocationClient.stop();
		}
	}
	// 验证KEY
	private void initManager() {
		manager = new BMapManager(mContext);
		manager.init(Global.KEY, new MKGeneralListener() {

			@Override
			public void onGetPermissionState(int iError) {
				if (iError == MKEvent.ERROR_PERMISSION_DENIED) {
					Toast.makeText(mContext, "授权失败", 1).show();
				}

			}

			@Override
			public void onGetNetworkState(int iError) {
				if (iError == MKEvent.ERROR_NETWORK_CONNECT) {
					Toast.makeText(mContext, "无网络", 1).show();
				}
			}
		});
	}

	static private String getMyIp(String szURL) {
		String Return = "";
		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL(szURL);
			urlConnection = (HttpURLConnection) url.openConnection();
			java.io.InputStream is = urlConnection.getInputStream();
			String line = "";
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));
			while ((line = reader.readLine()) != null)
				if (line.length() > 0)
					Return = Return + trimBOM(line.trim());
			is.close();
		} catch (Exception e) {
		} finally {
			if (urlConnection != null)
				urlConnection.disconnect();
		}
		return Return;
	}

	static public String getAddressFromIp(String szURL) throws IOException {
		String Return = "";
		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL(szURL);
			urlConnection = (HttpURLConnection) url.openConnection();
			java.io.InputStream is = urlConnection.getInputStream();
			String line = "";
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));
			while ((line = reader.readLine()) != null)
				if (line.length() > 0)
					Return = Return + trimBOM(line.trim());
			is.close();
		} catch (Exception e) {
		} finally {
			if (urlConnection != null)
				urlConnection.disconnect();
		}
		return Return;
	}

	static private String trimBOM(String s) {
		while (s.length() >= 1 && s.charAt(0) == 0xFEFF)
			s = s.substring(1);
		return s;
	}

	public void getMyLocFromIpAddress() {
		final String getmyaddress = "http://www.telize.com/geoip/";
		final String getmyip = "http://icanhazip.com/";

		(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Log.d("getAddressFromIp...");
					String ipaddr = getMyIp(getmyip);
					String response = getAddressFromIp(getmyaddress + ipaddr);

					JSONObject jsonObject;
					try {
						jsonObject = new JSONObject(response);

						String iso = jsonObject.getString("country_code")
								.toLowerCase();
						String lat = jsonObject.getString("latitude");
						String lon = jsonObject.getString("longitude");

						// {"ip":"123.195.204.129","country_code":"TW","country_name":"Taiwan","region_code":"03","region_name":"T'ai-pei","city":"Taipei","zipcode":"","latitude":25.0392,"longitude":121.525,"metro_code":"","areacode":""}
						try {
							mPref.write("iso", iso);
							updatePreference(
									(long) (Double.parseDouble(lat) * 1E6),
									(long) (Double.parseDouble(lon) * 1E6));
							uploadLocationToServer();
						} catch (Exception e) {
						}

					} catch (JSONException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (IOException e) {
				}
			}
		})).start();
	}

	private void updatePreference(long latitude, long longitude) {
		Log.d("write to preference :mylongitude:" + longitude + ", mylatitude:"
				+ latitude);

		if (latitude != 0 && longitude != 0) {
			mPref.writeLong("longitude", longitude);
			mPref.writeLong("latitude", latitude);

			try {
				if (BDMapViewLocation.getInstance() != null) {
					Intent intent = new Intent();
					intent.setAction(BDMapViewLocation.MyLocation_Change);
					mContext.sendBroadcast(intent);
				}
			} catch (Exception e) {
			}
		}
	}

	public void uploadLocationToServer() {
		Thread updateLocation_Thread = new Thread(new Runnable() {
			public void run() {
				Log.d("uploadLocationToServer...");
				boolean longTime = true;

				long last = mPref.readLong("last_time_update_location", 0);
				long now = new Date().getTime();
				if (now - last < (longTime ? 3600000 : 300000)) // 1 hour or 5
																// minutes3600000 : 300000
					return;// no need to update

				mPref.writeLong("last_time_update_location", now);// alec

				try {
					long latitude;
					long longitude;

					if (mygeo == null) {
						refreshLocation();
						mygeo = getMyGeo();
					}
					if (mygeo != null) {
						latitude = (long) (mygeo.getLatitudeE6());
						longitude = (long) (mygeo.getLongitudeE6());
					} else {
						latitude = mPref.readLong("latitude", 39976279);
						longitude = mPref.readLong("longitude", 116349386);
					}
					if (latitude != 0 && longitude != 0 && mPref.readBoolean("AireRegistered")) {

						try {
							int myIdx = Integer.parseInt(
									mPref.read("myID", "0"), 16);
							MyNet net = new MyNet(mContext);
							String Return = net.doPost(
									"updatelocation_aire.php", "idx=" + myIdx
											+ "&lat=" + latitude + "&lon="
											+ longitude, null);
							Log.d("Return=" + Return);
							Log.d("uploadLocationToServer :mylongitude:"
									+ longitude + ", mylatitude:" + latitude);
						} catch (Exception e) {
						}


						if (BDMapViewLocation.getInstance() != null) {
							Intent intent = new Intent();
							intent.setAction(BDMapViewLocation.MyLocation_Change);
							mContext.sendBroadcast(intent);
						}
					}
				} catch (Exception e) {
				}
			}
		}, "location update");
		updateLocation_Thread.start();
	}

	public void destroy() {
		autoReleaseMll.run();
	}

}
