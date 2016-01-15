package com.pingshow.airecenter.map.bd;

import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.MKGeneralListener;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MKEvent;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.search.MKAddrInfo;
import com.baidu.mapapi.search.MKBusLineResult;
import com.baidu.mapapi.search.MKDrivingRouteResult;
import com.baidu.mapapi.search.MKPoiResult;
import com.baidu.mapapi.search.MKSearch;
import com.baidu.mapapi.search.MKSearchListener;
import com.baidu.mapapi.search.MKSuggestionResult;
import com.baidu.mapapi.search.MKTransitRouteResult;
import com.baidu.mapapi.search.MKWalkingRouteResult;
import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.util.ImageUtil;

public class BDSelfMapView extends Activity {
	private MapView mapview;// 注意：mapview 需要处理 onResume onPause desotroy
	private BMapManager manager;// 地图引擎管理工具
	private MapController mc;
	
	private MKSearch search;
	private MKSearchListener listener;
	
	private LocationClient mLocationClient = null;
	private MyLocationOverlay myLocationOverlay;
	LocationData myData = new LocationData();
	private MyLocationListener myLocationListener;
	private GeoPoint mygeo;
	private String myAddrInfo="";
	
	private TextView AddrView;
	private RadioButton walkingradioButton, drivingradioButton;
	private ToggleButton myzoom, yourzoom;
	private ToggleButton autotrack;
	
	private String address;

	private Handler mHandler = null;
	private long position_time;

	private int origZoom;
	
	private float newWidth = 30f;
	private float newHeight = 30f;
	
	private float mDensity;
	private Bitmap resizedMyBitmap = null;
	private MyPreference mPref;
	private BitmapDrawable myDrawable;

	static public BDSelfMapView instance = null;
	
	static public BDSelfMapView getInstance() {
		return instance;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 在加载布局之前必须校验KEY信息
		// 需要使用地图的引擎管理工具处理KEY的校验
		checkKey();
		setContentView(R.layout.map_page);
		mapview = (MapView) findViewById(R.id.mapview);
		// ②放大和缩小的按钮——MapView extends ViewGroup
		mapview.setBuiltInZoomControls(true);// addView

		// ①放大级别
		mc = mapview.getController();// 对于Controller 管理某个具体的mapview信息
		mc.setZoom(12);// 放大级别的设置(在V2.0以后的版本支持3-19的放大级别，如果是V1.x3-18)
		
		search = new MKSearch();// 搜索
		listener = new MySearchListener();// 结果处理
		search.init(manager, listener);// 将查询和结果处理关联在一起
		
		mDensity = getResources().getDisplayMetrics().density;
		
		newWidth = (float) (29 * mDensity);
		newHeight = (float) (25 * mDensity);

		mPref = new MyPreference(this);
		mHandler = new Handler();

		((ImageView) findViewById(R.id.cancel))
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						finish();
					}
				});

		((ImageView) findViewById(R.id.close))
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						((RelativeLayout) findViewById(R.id.address_view))
								.setVisibility(View.GONE);
					}
				});
		position_time = getIntent().getLongExtra("time", new Date().getTime());

		walkingradioButton = (RadioButton) findViewById(R.id.walking);
		drivingradioButton = (RadioButton) findViewById(R.id.driving);

		myzoom = (ToggleButton) findViewById(R.id.myzoom);// own location
		yourzoom = (ToggleButton) findViewById(R.id.yourzoom);// friend location
		AddrView = (TextView) findViewById(R.id.user_address);// location info
		autotrack = ((ToggleButton) findViewById(R.id.autotrack));// track line
		// 实时路况
		ToggleButton showtraffic = (ToggleButton) findViewById(R.id.showtraffic);
		// 卫星图
		ToggleButton showsatellite = (ToggleButton) findViewById(R.id.showsatellite);
		
		
		getphoto();
		startLocation();
		//enablemyLocation();
		appoint();

		showtraffic.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton button,
					boolean isChecked) {
				if (isChecked) {// ok, the focus changed,. let zomyzooom to my
								// position as mych as possible
					mapview.setTraffic(true);
					mapview.setSatellite(false);
				} else {
					mapview.setTraffic(false);
					mapview.setSatellite(false);
				}
				mPref.write("TrafficView", isChecked);
				System.gc();
				System.gc();
			}
		});
		
		showsatellite.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton button,
					boolean isChecked) {
				if (isChecked) {
					mapview.setSatellite(true);
					mapview.setTraffic(false);
				} else {
					mapview.setSatellite(false);
					mapview.setTraffic(false);
				}
				mPref.write("SatelliteView", isChecked);
				System.gc();
				System.gc();
			}
		});

		myzoom.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton button,
					boolean isChecked) {
				if (!isChecked) {
					mc.animateTo(mygeo);
					mc.setZoom(origZoom);
				} else if (isChecked) {
					refreshLocation();
					mc.animateTo(mygeo);
					int maxZoomLevel = mapview.getMaxZoomLevel() - 2;
					if (maxZoomLevel > 20)
						maxZoomLevel = 20;
					mc.setZoom(maxZoomLevel);
					((RelativeLayout) findViewById(R.id.address_view))
							.setVisibility(View.VISIBLE);
				}
				System.gc();
				System.gc();
			}
		});
		
		showsatellite.setChecked(mPref.readBoolean("SatelliteView", false));
		showtraffic.setChecked(mPref.readBoolean("TrafficView", false));
		
		walkingradioButton.setVisibility(View.GONE);
		drivingradioButton.setVisibility(View.GONE);
		autotrack.setVisibility(View.GONE);
		yourzoom.setVisibility(View.GONE);
	}


	private void startLocation() {
		mLocationClient = new LocationClient(getApplicationContext());
		myLocationListener = new MyLocationListener();
		// 定位的参数信息
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);
		option.setAddrType("all");// 返回的定位结果包含地址信息
		option.setCoorType("bd09ll");// 返回的定位结果是百度经纬度,默认值gcj02
		option.setScanSpan(5000);// 设置发起定位请求的间隔时间为5000ms
		option.disableCache(true);// 禁止启用缓存定位
		option.setPoiNumber(5); // 最多返回POI个数
		option.setPoiDistance(1000); // poi查询距离
		option.setPoiExtraInfo(true); // 是否需要POI的电话和地址等详细信息
		mLocationClient.setLocOption(option);

		mLocationClient.registerLocationListener(myLocationListener);
		refreshLocation();
		}

	private void checkKey() {
		// if(manager!=null)
		manager = new BMapManager(getApplicationContext());
		// strKey - 申请的授权验证码:null ""
		// listener - 注册回调事件
		manager.init(Global.KEY, new MKGeneralListener() {
			@Override
			public void onGetNetworkState(int iError) {
				// TODO 网络状态的判断(MKEvent常量信息)
				if (iError == MKEvent.ERROR_NETWORK_CONNECT) {
					Toast.makeText(BDSelfMapView.this, "无网络", 1).show();
				}

			}

			@Override
			public void onGetPermissionState(int iError) {
				// TODO 授权的验证
				if (iError == MKEvent.ERROR_PERMISSION_DENIED) {
					Toast.makeText(BDSelfMapView.this, "无权限", 1).show();
				}

			}

		});

	}

	private void appoint() {
		mc.setZoom(15);
		mc.animateTo(mygeo);
		origZoom = (int) mapview.getZoomLevel();
	}

	// 更新位置并保存
	public void refreshLocation() {
		if (mLocationClient != null && mLocationClient.isStarted()) {
			mLocationClient.start();
			mLocationClient.requestLocation();
			Log.i("update the location");
		}
	}

	// 获取经纬度信息
	public LocationData getLocation() {
		return myData;
	}
	public class MyLocationListener implements BDLocationListener{

		@Override
		public void onReceiveLocation(BDLocation location) {
			if (location == null)
				return;
			myLocationOverlay=new MyLocationOverlay(mapview);
			myData = new LocationData();
			myData.latitude = location.getLatitude();
			myData.longitude = location.getLongitude();
			myData.accuracy=location.getRadius();
			// 经纬度的数据获取
			myLocationOverlay.setData(myData);
			myLocationOverlay.setMarker(myDrawable);
			if (mapview.getOverlays()!=null) {
				mapview.getOverlays().remove(myLocationOverlay);
			} 
			mapview.getOverlays().add(myLocationOverlay);
			mapview.refresh();
			
			int latitude = (int) (myData.latitude * 1E6);
			int longitude = (int) (myData.longitude * 1E6);
			mygeo = new GeoPoint(latitude, longitude);
			// 移动地图
			mc.animateTo(mygeo);
			Log.d("animate to mylocation"+latitude+":"+longitude);
					search.reverseGeocode(mygeo);
					showAddress(position_time);
					System.gc();
					System.gc();
		}

		public void onReceivePoi(BDLocation poiLocation) {}
	}
	
	@Override
	protected void onResume() {
		mLocationClient.start();// 开启定位
		mapview.onResume();
		super.onResume();
	}
	@Override
	protected void onPause() {
		mLocationClient.stop();// 结束定位
		mapview.onPause();
		super.onPause();
	}
	@Override
	protected void onDestroy() {

	/*	if (!resizedMyBitmap.isRecycled())
			resizedMyBitmap.recycle();
		instance = null;*/

		if (mLocationClient != null && mLocationClient.isStarted()) {
			mLocationClient.stop();
			mLocationClient = null;
		}

		mapview.destroy();
		System.gc();
		System.gc();
		super.onDestroy();
	}
	
	@Override
	public void onBackPressed() {
		System.gc();
		System.gc();
		super.onBackPressed();
	}

	private void showAddress(long pos_time) {
		String time = DateUtils.formatDateTime(BDSelfMapView.this, pos_time,
				DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE);
		address = getResources().getString(R.string.mylocation_address) + ": "
				+ time + "\n" + myAddrInfo;

		mHandler.post(new Runnable() {
			public void run() {
				AddrView.setText(address);
			}
		});
	}
	
	private void getphoto() {

		Bitmap bubbleblue = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.bubble_backgroundn);

		try {
			Drawable myPhoto = null;

			String mypath = mPref.read("myPhotoPath", null);
			Log.d(mypath+"imagepath******************");
			if (mypath != null && mypath.length() > 0)
			{
				try{
					myPhoto = ImageUtil.loadBitmapSafe(mypath, 2);
				}catch(OutOfMemoryError e){
					System.gc();
					System.gc();
				}
			}

			if (myPhoto == null)
				myPhoto = getResources().getDrawable(R.drawable.bighead);

			if (myPhoto != null) {
				try{
					BitmapDrawable bm2 = (BitmapDrawable) myPhoto;
					int height = bm2.getBitmap().getHeight();
					int width = bm2.getBitmap().getWidth();
					float myscaleWidth = ((float) newWidth) / height;
					float myscaleHeight = ((float) newHeight) / width;
					Matrix mymatrix = new Matrix();
					mymatrix.postScale(myscaleWidth, myscaleHeight);
					resizedMyBitmap = Bitmap.createBitmap(bm2.getBitmap(), 0,
							0, width, height, mymatrix, true);
					Bitmap bmp = null;   
			        //下面这个Bitmap中创建的函数就可以创建一个空的Bitmap  
			        bmp = Bitmap.createBitmap(bubbleblue.getWidth(), bubbleblue.getHeight(), bubbleblue.getConfig());   
			        Paint paint = new Paint();   
			        Canvas canvas = new Canvas(bmp);   
			        //首先绘制第一张图片，很简单，就是和方法中getDstImage一样   
			        canvas.drawBitmap(bubbleblue, 0, 0, paint);         
			           
			        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_OVER));   
			        canvas.drawBitmap(resizedMyBitmap, (float)mDensity*5.3f, (float)mDensity*4.6f, paint); 
			        myDrawable=new BitmapDrawable(bmp);
				}catch(OutOfMemoryError e){}
			}
			myPhoto = null;
		} catch (Exception e) {
			Log.e(e.getMessage());
		}

		bubbleblue=null;
		System.gc();
		System.gc();
	}

	private class MySearchListener implements MKSearchListener {

		@Override
		public void onGetAddrResult(MKAddrInfo arg0, int arg1) {
			// TODO Auto-generated method stub
			/*Toast.makeText(getApplicationContext(), "地址信息" + arg0.strAddr, 1)
			.show();*/
			if (arg1 == 0) {
				myAddrInfo = arg0.strAddr;
			}else{
				Toast.makeText(getApplicationContext(), "地址信息查询失败" , 1)
				.show();
			}
		}

		@Override
		public void onGetBusDetailResult(MKBusLineResult arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onGetDrivingRouteResult(MKDrivingRouteResult arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onGetPoiDetailSearchResult(int arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onGetPoiResult(MKPoiResult arg0, int arg1, int arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onGetSuggestionResult(MKSuggestionResult arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onGetTransitRouteResult(MKTransitRouteResult arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onGetWalkingRouteResult(MKWalkingRouteResult arg0, int arg1) {
			// TODO Auto-generated method stub

		}

	}

}
