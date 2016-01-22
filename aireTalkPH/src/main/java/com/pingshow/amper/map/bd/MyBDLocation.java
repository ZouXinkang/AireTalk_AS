package com.pingshow.amper.map.bd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.pingshow.amper.Log;
import com.pingshow.amper.R;

//xwf*** baidu map
public class MyBDLocation extends Activity {
	// 定位相关
	LocationClient mLocClient;
	public MyLocationListenner myListener = new MyLocationListenner();
//	private LocationMode mCurrentMode;// 定位模式
	BitmapDescriptor mCurrentMarker;// Marker图标

	private MapView mapView;
	BaiduMap mBaiduMap;
	boolean isFirstLoc = true;// 是否首次定位
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SDKInitializer.initialize(getApplicationContext());
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_bdlocation);
//		mCurrentMode = LocationMode.NORMAL;// 设置定位模式为普通
		mCurrentMarker = BitmapDescriptorFactory// 构建mark图标
				.fromResource(R.drawable.icon_marka);
		// 地图初始化
		mapView = (MapView) findViewById(R.id.my_location_bmapView);
		mCancel = (ImageView) findViewById(R.id.cancel);

		mBaiduMap = mapView.getMap();
		
		Intent intent =  getIntent();
		myLatitude = 0.000001*intent.getLongExtra("latitude", 0);
		myLongitude = 0.000001*intent.getLongExtra("longitude", 0);
		mCancel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		

		// 开启定位图层
		// mBaiduMap.setMyLocationEnabled(true);
		// mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(
		// mCurrentMode, true, null));
		// 定位初始化
		mLocClient = new LocationClient(this);
		mLocClient.registerLocationListener(myListener);// 注册监听函数：

		LocationClientOption option = new LocationClientOption();
		option.setCoorType("bd09ll");// 返回的定位结果是百度经纬度,默认值gcj02
		option.setScanSpan(1000);// 设置发起定位请求的间隔时间为5000ms
		option.setIsNeedAddress(true);// 返回的定位结果包含地址信息
		option.setNeedDeviceDirect(true);// 返回的定位结果包含手机机头的方向
		mLocClient.setLocOption(option);
		mLocClient.start();
		pointDraw();
	}

	double latitude;
	double longitude;
	private ImageView mCancel;
	double myLatitude;
	double myLongitude;
	public void pointDraw(){
		//定义Maker坐标点  
//		LatLng point = new LatLng(39.995226, 116.430643);  
		LatLng point = new LatLng(myLatitude, myLongitude);
//		Toast.makeText(getApplicationContext(), "myLatitude"+myLatitude+"myLongitude"+myLongitude, 1).show();
		Log.d("bdmap lat/long " + myLatitude + " " + myLongitude);
		//构建Marker图标  
		BitmapDescriptor bitmap = BitmapDescriptorFactory
		    .fromResource(R.drawable.icon_markb);
		//构建MarkerOption，用于在地图上添加Marker  
		OverlayOptions option = new MarkerOptions()  
		    .position(point)  
		    .icon(bitmap);  
		//在地图上添加Marker，并显示  
		mBaiduMap.addOverlay(option);
	}

	/**
	 * 
	 * @author LIUBO
	 * @date 2015-3-20下午5:55:19
	 * @TODO
	 * @param longitude
	 * @param latitude
	 * @param bitmap
	 * @param baiduMap
	 */
	private void overlay(LatLng point, BitmapDescriptor bitmap,
			BaiduMap baiduMap) {
		// 构建MarkerOption，用于在地图上添加Marker
		OverlayOptions option = new MarkerOptions().position(point)
				.icon(bitmap);
		// 在地图上添加Marker，并显示
		baiduMap.addOverlay(option);
	}
	

	/**
	 * 定位SDK监听函数
	 */
	public class MyLocationListenner implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			// map view 销毁后不在处理新接收的位置
			if (location == null || mapView == null)
				return;

			MyLocationData locData = new MyLocationData.Builder()
					.accuracy(location.getRadius())
					// 此处设置开发者获取到的方向信息，顺时针0-360
					.direction(100).latitude(location.getLatitude())
					.longitude(location.getLongitude()).build();
			mBaiduMap.setMyLocationData(locData);
			if (isFirstLoc) {
				isFirstLoc = false;
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				LatLng ll = new LatLng(latitude, longitude);
				// 设置缩放比例,更新地图状态
				float f = mBaiduMap.getMaxZoomLevel();// 19.0
				MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(ll,
						f - 4);
				mBaiduMap.animateMapStatus(u);
				overlay(ll, mCurrentMarker, mBaiduMap);
			}
		}

		public void onReceivePoi(BDLocation poiLocation) {
		}
	}

	@Override
	protected void onPause() {
		mapView.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		mapView.onResume();
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		// 退出时销毁定位
		mLocClient.stop();
		// 关闭定位图层
		mBaiduMap.setMyLocationEnabled(false);
		mapView.onDestroy();
		mapView = null;
		super.onDestroy();
	}
}
