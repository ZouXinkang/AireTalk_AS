package com.pingshow.amper.map.bd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.R;

//xwf*** baidu map
public class SendMyBDLocation extends Activity {
	private MyPreference mPrf;

	private MapView mMapView;
	BaiduMap mBaiduMap;
	private ImageView mCancel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// xwf
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		// 在使用SDK各组件之前初始化context信息，传入ApplicationContext
		// 注意该方法要再setContentView方法之前实现
		SDKInitializer.initialize(getApplicationContext());
		setContentView(R.layout.my_bdlocation);
		mPrf = new MyPreference(this);
		sendMyLongitude = mPrf.readLong("longitude", 0l);
		sendMyLatitude = mPrf.readLong("latitude", 0l);
		sendMyLongitude = 0.000001 * sendMyLongitude;
		sendMyLatitude = 0.000001 * sendMyLatitude;

		// 获取地图控件引用
		mMapView = (MapView) findViewById(R.id.my_location_bmapView);
		mCancel = (ImageView) findViewById(R.id.cancel);
		mBaiduMap = mMapView.getMap();
		pointDraw();
		// 设置缩放比例,更新地图状态
		float f = mBaiduMap.getMaxZoomLevel();// 19.0
		MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(point, f - 4);
		mBaiduMap.animateMapStatus(u);
		// 点击取消，finish地图页面
		mCancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	double latitude;
	double longitude;

	double sendMyLongitude;
	double sendMyLatitude;

	private LatLng point;

	public void pointDraw() {
		point = new LatLng(sendMyLatitude, sendMyLongitude);
//		Toast.makeText(getApplicationContext(), "sendMyLatitude"+sendMyLatitude+"sendMyLongitude"+sendMyLongitude, 1).show();
		Log.d("bdmap send lat/long " + sendMyLatitude + " " + sendMyLongitude);
		// 构建Marker图标
		BitmapDescriptor bitmap = BitmapDescriptorFactory
				.fromResource(R.drawable.icon_markb);
		// 构建MarkerOption，用于在地图上添加Marker
		OverlayOptions option = new MarkerOptions().position(point)
				.icon(bitmap);
		// 在地图上添加Marker，并显示
		mBaiduMap.addOverlay(option);
	}

	private void overlay(LatLng point, BitmapDescriptor bitmap,
			BaiduMap baiduMap) {
		// 构建MarkerOption，用于在地图上添加Marker
		OverlayOptions option = new MarkerOptions().position(point)
				.icon(bitmap);
		// 在地图上添加Marker，并显示
		baiduMap.addOverlay(option);
	}

	@Override
	protected void onPause() {
		mMapView.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		mMapView.onResume();
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		// 退出时销毁定位
		// 关闭定位图层
		mBaiduMap.setMyLocationEnabled(false);
		mMapView.onDestroy();
		mMapView = null;
		super.onDestroy();
	}
}
