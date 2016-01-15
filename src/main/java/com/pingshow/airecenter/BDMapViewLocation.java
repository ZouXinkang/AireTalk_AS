package com.pingshow.airecenter;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.LocationClient;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.MKGeneralListener;
import com.baidu.mapapi.map.ItemizedOverlay;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MKEvent;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayItem;
import com.baidu.mapapi.map.RouteOverlay;
import com.baidu.mapapi.search.MKAddrInfo;
import com.baidu.mapapi.search.MKBusLineResult;
import com.baidu.mapapi.search.MKDrivingRouteResult;
import com.baidu.mapapi.search.MKPoiResult;
import com.baidu.mapapi.search.MKRoute;
import com.baidu.mapapi.search.MKRoutePlan;
import com.baidu.mapapi.search.MKSearch;
import com.baidu.mapapi.search.MKSearchListener;
import com.baidu.mapapi.search.MKSuggestionResult;
import com.baidu.mapapi.search.MKTransitRouteResult;
import com.baidu.mapapi.search.MKWalkingRouteResult;
import com.baidu.mapapi.utils.DistanceUtil;
import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.map.bd.BDLocationUpdate;
import com.pingshow.airecenter.map.bd.IGetLocationService;
import com.pingshow.util.ImageUtil;
public class BDMapViewLocation extends Activity{
	public LocationClient mLocationClient = null;
	private GeoPoint friendgeo, mygeo;
	private ItemizedOverlay<OverlayItem> overlay;
	protected MapView mapView;// 注意：mapView 需要处理 onResume onPause desotroy
	protected BMapManager manager;// 地图引擎管理工具
	protected MKSearch search;
	protected MKSearchListener listener;
	protected MapController mc;
	
	protected List<Overlay> mapOverlays;
	
	protected ArrayList<String> shared_friends;
	private float myaccuracy;
	private TextView AddrView;
	private RadioGroup rgp;

	private int svTrack;
	private LocationData[] friendLocation;
	private LocationData myLocation;
	private Handler mHandler = new Handler();;
	private long position_time;
	private boolean mTrackable;
	private BDLocationUpdate mLocation;

	private MyPreference mPref;
	private boolean TrackMark = false;
	private boolean isTrack = true;
	private int friendLat = 0, friendLon = 0;
	private int myLat = 0, myLon = 0;
	private int newfriendLat = 0, newfriendLon = 0;

	private final int LOC_MIN_TIME = 3600000;
	private final int LOC_MIN_DISTANCE = 200;

	private Drawable myDrawable;
	private Drawable[] friendDrawable;
	private OverlayItem[] friendItem;
	private OverlayItem myItem;
	private int newWidth = 75;
	private int newHeight = 75;
	boolean turnon = false;
	private float mDensity;
	public static final String MyLocation_Change = "com.pingshow.airecenter.MyLocationChange";
	private Bitmap resizedMyBitmap=null;
	private Bitmap resizedFriendBitmap=null;
	int length;
	int[] idx;
	String[] mAddress;

	static public BDMapViewLocation _this = null;
	private IGetLocationService locationService;
	static public BDMapViewLocation getInstance() {
		return _this;
	}
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			locationService=(IGetLocationService) service;
			Log.d("locationService不是空connect"+System.currentTimeMillis());
			locationService.refreshLocation();
			mygeo=locationService.getMyGeo();
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {
			locationService=null;
		}
    };
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		checkKey();
		setContentView(R.layout.baidu_map_page);
		
		if (getIntent().getBooleanExtra("launchFromSelf", false))
			this.overridePendingTransition(R.anim.freeze, R.anim.freeze);
		
		this.bindService(new Intent(this, AireJupiter.class), this.serviceConnection, BIND_AUTO_CREATE);
		Log.d("绑定服务"+System.currentTimeMillis());
		mapView = (MapView) findViewById(R.id.mapview);
		search = new MKSearch();// 搜索
		listener = new MySearchListener();// 结果处理
		// ②放大和缩小的按钮——MapView extends ViewGroup
		mapView.setBuiltInZoomControls(true);// addView
		mapView.setDrawingCacheQuality(MapView.DRAWING_CACHE_QUALITY_LOW);
		mapView.setDrawingCacheEnabled(true);
		// ①放大级别
		mc = mapView.getController();// 对于Controller 管理某个具体的mapView信息
		mc.setZoom(12);// 放大级别的设置(在V2.0以后的版本支持3-19的放大级别，如果是V1.x3-18)
		
		search.init(manager, listener);// 将查询和结果处理关联在一起
		
		
		
		mDensity = getResources().getDisplayMetrics().density;

		newWidth = (int) (29 * mDensity);
		newHeight = (int) (25 * mDensity);

		mPref = new MyPreference(this);
		_this = this;
	
		mapOverlays = mapView.getOverlays();
		mc = mapView.getController();

		((TextView) findViewById(R.id.location_setting)).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent=new Intent(BDMapViewLocation.this, LocationSettingActivity.class);
				intent.putExtra("launchFromSelf", true);
				startActivity(intent);
				mHandler.postDelayed(new Runnable(){
					public void run()
					{
						finish();
					}
				}, 1000);
			}
		});
		
		length=0;
		
		shared_friends=getIntent().getStringArrayListExtra("shared_friends");
		if (shared_friends!=null) {
			length=shared_friends.size();
		}
		Log.d("shared_friends==="+shared_friends+"length==="+length);
		idx=new int[length];
		mAddress=new String[length];
		friendLocation=new LocationData[length];
		friendItem=new OverlayItem[length];
		friendDrawable=new Drawable[length];
		
		mTrackable=getIntent().getBooleanExtra("trackable", false);
		
		if (mTrackable){
			myLat = (int) mPref.readLong("latitude", Global.DEFAULT_LAT);
			myLon = (int) mPref.readLong("longitude", Global.DEFAULT_LON);
		}
		else{
			myLat = (int) getIntent().getLongExtra("mylatitude", Global.DEFAULT_LAT);
			myLon = (int) getIntent().getLongExtra("mylongitude", Global.DEFAULT_LON);
		}
		
		friendLat = (int) getIntent().getLongExtra("latitude", Global.DEFAULT_LAT);
		friendLon = (int) getIntent().getLongExtra("longitude", Global.DEFAULT_LON);

		//tml*** beta ui
        ((Button)findViewById(R.id.back)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		mHandler.postDelayed(initOverlay, 500);
        
		Thread th = new Thread(new Runnable() {
			public void run() {
				//showAddress();
				System.gc();
				System.gc();
			}
		});
		th.start();
		
		if (mTrackable)
		{
			mPref.write("TrackTime", 900000);
			mPref.write("TrackDistance", 30);
			
			mHandler.postDelayed(autoUpdateLocation, 5000);
		}
		
		//CoordinateConvert converter  = new CoordinateConvert();  
	
		DialerFrame.setFrame(this, findViewById(android.R.id.content));
		
////// SideBar
        
        ((ImageView)findViewById(R.id.bar1)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it=new Intent(BDMapViewLocation.this, MainActivity.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
        ((ImageView)findViewById(R.id.bar6)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it=new Intent(BDMapViewLocation.this, ShoppingActivity.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
        ((ImageView)findViewById(R.id.bar7)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it=new Intent(BDMapViewLocation.this, SecurityNewActivity.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
        ((ImageView)findViewById(R.id.bar8)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});
		
	}
	
	Runnable initOverlay=new Runnable() {
		
		@Override
		public void run() {
			getphoto();
			enableFriendLocation();
			enablemyLocation();
			
			if (mapOverlays != null) {
				mapOverlays.clear();
			}
			overlay = new ItemizedOverlay<OverlayItem>(myDrawable, mapView);
			overlay.addItem(myItem);
			Log.d("add myitem"+mygeo.getLatitudeE6());
			for (int j = 0; j < length; j++) {
				overlay.addItem(friendItem[j]);
			}
			mapOverlays.add(overlay);
			mapView.refresh();
			appoint();
		}
	};
	
	Runnable autoUpdateLocation=new Runnable() {
		@Override
		public void run() {
			if (AireJupiter.getInstance()!=null)
			{
				if (mLocation==null)
					mLocation=new BDLocationUpdate(BDMapViewLocation.this, mHandler, AireJupiter.getInstance().tcpSocket(), mPref, false);
			}
		}
	};

	private double getMaxDistance() {
		double max=0;
		double[] distance=new double[length];
		for (int i = 0; i < length; i++) {
			if (friendItem[i]!=null) {
				Log.d("friendItem[i]不为空");
				friendgeo=friendItem[i].getPoint();
			}
			if (friendgeo!=null) {
				distance[i] = DistanceUtil.getDistance(friendgeo, mygeo)/1000;
				//distance[i]=getDistance(friendgeo, mygeo);
			}
		}
		for (int i = 0; i < distance.length; i++) {
			max=distance[i];
			if (max<distance[i]) {
				max=distance[i];
			}
		}
		return max;
	}
	
	void appoint() {
		double dis = getMaxDistance();
		if (dis < 2)
			mc.setZoom(16);
		else if (friendgeo.getLatitudeE6() == 0
				&& friendgeo.getLongitudeE6() == 0) {
			mc.setZoom(15);
			mc.animateTo(mygeo);
		} else if (dis < 3)
			mc.setZoom(15);
		else if (dis < 4)
			mc.setZoom(14);
		else if (dis < 6)
			mc.setZoom(13);
		else if (dis < 12)
			mc.setZoom(12);
		else if (dis < 25)
			mc.setZoom(11);
		else if (dis < 60)
			mc.setZoom(10);
		else if (dis < 100)
			mc.setZoom(9);
		else if (dis < 200)
			mc.setZoom(8);
		else if (dis < 600)
			mc.setZoom(7);
		else if (dis < 1200)
			mc.setZoom(6);
		else if (dis < 2500)
			mc.setZoom(5);
		else if (dis < 5000)
			mc.setZoom(4);
		else if (dis < 9000)
			mc.setZoom(3);
		else
			mc.setZoom(2);
/*
		int latHalf = (friendgeo.getLatitudeE6() + mygeo.getLatitudeE6()) / 2;
		int lonHalf = (friendgeo.getLongitudeE6() + mygeo.getLongitudeE6()) / 2;
		GeoPoint geohalf = new GeoPoint(latHalf, lonHalf);*/
		mc.animateTo(mygeo);
		/*origZoom = mapView.getZoomLevel();
		origCenter = geohalf;*/
	}
	
	void changeTextColor(boolean isChecked) {
		if (isChecked) {
			AddrView.setTextColor(0xffffffff);
			AddrView.setShadowLayer(2f, 2f, 2f, 0xff000000);
		} else {
			AddrView.setTextColor(0xff000000);
			AddrView.setShadowLayer(2f, 2f, 2f, 0xffffffff);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mapView.onResume();
		if (locationService!=null) {
			locationService.startLocation();
			locationService.refreshLocation();
		}
		
		IntentFilter intentToReceiveFilter = new IntentFilter();
		intentToReceiveFilter.addAction(BDMapViewLocation.MyLocation_Change);
		this.registerReceiver(MyLocationChange, intentToReceiveFilter);
		
		DialerFrame.checkEmbeddedDialer(findViewById(android.R.id.content));
	}
	
	private OverlayItem fmarker;
	
	@Override
	protected void onPause() {
		unregisterReceiver(MyLocationChange);
		mapView.onPause();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		svTrack = 0;
		mHandler.removeCallbacks(autoUpdateLocation);
		mHandler.removeCallbacks(updateAddrTask);
		//mHandler.removeCallbacks(addmypoint);
		//mHandler.removeCallbacks(addfriendpoint);
		mHandler.removeCallbacks(friendLocationChange);
		
		disableMyLocation();
		
		_this = null;
		
		if (mLocation!=null)
			mLocation.destroy();
		/*
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
				&& turnon) {
			toggleGPS();
		}*/
		
		mPref.write("TrackTime", LOC_MIN_TIME);
		mPref.write("TrackDistance", LOC_MIN_DISTANCE);
		
		try{
			if(resizedMyBitmap!=null && !resizedMyBitmap.isRecycled())
				resizedMyBitmap.recycle();
			if(resizedFriendBitmap!=null && !resizedFriendBitmap.isRecycled())
				resizedFriendBitmap.recycle();
		}catch(Exception e){}
		
		mapOverlays.clear();
		mapView.destroy();
		
		System.gc();
		System.gc();
		super.onDestroy();
	}

	 @Override
     protected void onSaveInstanceState(Bundle outState) {
             super.onSaveInstanceState(outState);
             mapView.onSaveInstanceState(outState);

     }

     @Override
     protected void onRestoreInstanceState(Bundle savedInstanceState) {
             super.onRestoreInstanceState(savedInstanceState);
             mapView.onRestoreInstanceState(savedInstanceState);
     }
	
	@Override
	public void onBackPressed() {
		svTrack = 0;
		mHandler.removeCallbacks(autoUpdateLocation);
		mHandler.removeCallbacks(updateAddrTask);
		//mHandler.removeCallbacks(addmypoint);
		//mHandler.removeCallbacks(addfriendpoint);
		mHandler.removeCallbacks(friendLocationChange);
		System.gc();
		super.onBackPressed();
	}

	private void enablemyLocation() {
		if (mTrackable) {
			myLon = (int) mPref.readLong("longitude", 116349386);
			myLat = (int) mPref.readLong("latitude", 39976279);
			myaccuracy = mPref.readFloat("accuracy", 80.0f);
			Log.d("mygeo************pref" + myLat + ":" + myLon);
		} else {
			myLon = (int) getIntent().getLongExtra("mylongitude", 116349386);
			myLat = (int) getIntent().getLongExtra("mylatitude", 39976279);
			myaccuracy = 0;
			Log.d("mygeo&&&&&&&&&&&&&" + myLat + ":" + myLon);
		}
		if (myLat == 0 && myLat == 0) {
			myLon = 116349386;
			myLat = 39976279;
		}
		if (mygeo == null) {
			mygeo = new GeoPoint(myLat, myLon);
			Log.d("mygeo为空，所以初始化为：@@@@@@@@@@@@" + myLat + ":" + myLon);
		}
		myItem = new OverlayItem(mygeo, "myitem", "myitem");
		myItem.setMarker(myDrawable);
	}

	private void enableFriendLocation() {
		
		if (length!=0) {
		for (int i = 0; i < length; i++) {
			mAddress[i]=shared_friends.get(i);
			Log.d("mAddress["+i+"]====="+mAddress[i]);
			if (!mAddress[i].equals("support")) {
				Message msg = new Message();
				final AireJupiter x = AireJupiter.getInstance();
				if (x == null)
					return;
				int errorCode = 0;
				String Return = "";
				try {
					Return = x.getFriendLocation(mAddress[i]);
					Log.d("Return==x.getFriendLocation=="+Return);
					errorCode = 0;
				} catch (Exception e) {
					errorCode = -1;
					msg.arg1 = 1;
					handleAutoTrac.sendMessage(msg);
				}
				if (Return != null && Return.length() > 5) {
					String[] items = Return.split("/");
					try {
						newfriendLat = Integer.parseInt(items[0]) + 3512113;
						newfriendLon = Integer.parseInt(items[1]) - 10958121;
						position_time = Long.parseLong(items[2], 16);
						position_time *= 1000;
						Log.d("newfriendLat===="+newfriendLat+"newfriendLon===="+newfriendLon);
					} catch (NumberFormatException e) {
						errorCode = -1;
					}

					if (errorCode < 0) {
						msg.arg1 = 2;
						handleAutoTrac.sendMessage(msg);
					} else {
						Thread th = new Thread(new Runnable() {
							public void run() {
								// showAddress(position_time);
							}
						});
						th.start();
					}
				} else {
					msg.arg1 = 2;
					handleAutoTrac.sendMessage(msg);
				}

				if (newfriendLat != 0 && newfriendLon != 0) {
					friendLat = newfriendLat;
					friendLon = newfriendLon;
					Log.d("friendLat===="+friendLat+"friendLon===="+friendLon);
				}
				
					friendgeo = new GeoPoint(friendLat, friendLon);
					friendLocation[i] = new LocationData();
					friendLocation[i].latitude=friendLat;
					friendLocation[i].longitude=friendLon;
					friendLocation[i].accuracy=(float) 60.0;
					
					friendItem[i] = new OverlayItem(friendgeo, "f" + i, "f"
							+ i);
					friendItem[i].setMarker(friendDrawable[i]);
			}
		}
		}
		//mHandler.post(addfriendpoint);
	}

	private Runnable updateAddrTask = new Runnable() {
		public void run() {
			if (mTrackable)
			{
				(new Thread(runTrackingFriend, "runAutoTrack")).start();
				mHandler.postDelayed(this, svTrack * 1000);
			}
		}
	};

	private Runnable runTrackingFriend = new Runnable() {
		public void run() {
			if (!mTrackable) return;
			Log.i("runTrackingFriend...");
			
			final AireJupiter x = AireJupiter.getInstance();
			if (x == null)
				return;
			if (TrackMark) {
				mPref.write("TrackTime", svTrack * 1000);
				mPref.write("TrackDistance", 25);
				TrackMark = false;
			}
			int errorCode = 0;
			Message msg = new Message();
			int newmyLon = (int) mPref.readLong("longitude", Global.DEFAULT_LON);
			int newmyLat = (int) mPref.readLong("latitude", Global.DEFAULT_LAT);

			if (newmyLon == myLon && newmyLat == myLat
					&& newfriendLat == friendLat
					&& newfriendLon == friendLon)
				return;
			myLon = newmyLon;
			myLat = newmyLat;
			myaccuracy = mPref.readFloat("accuracy", 80.0f);
			myLocation = new LocationData();
			myLocation.accuracy=myaccuracy;
			mygeo = new GeoPoint(newmyLat, newmyLon);
			myItem = new OverlayItem(mygeo, "m", "m");
			if (svTrack != 0) {
				String Return = "";
				for (int i=0;i<length;i++) {
				try {
					Return = x.getFriendLocation(mAddress[i]);
					Log.d(Return);
					errorCode = 0;
				} catch (Exception e) {
					errorCode = -1;
					msg.arg1 = 1;
					handleAutoTrac.sendMessage(msg);
				}

				if (Return != null) {
					String[] items = Return.split("/");

					try {
						newfriendLat = Integer.parseInt(items[0])+3512113;
					} catch (NumberFormatException e) {
						errorCode = -1;
					}
					try {
						newfriendLon = Integer.parseInt(items[1])-10958121;
					} catch (NumberFormatException e) {
						errorCode = -1;
					}
					try {
						position_time = Long.parseLong(items[2], 16);
						position_time *= 1000;
					} catch (NumberFormatException e) {
						errorCode = -1;
					}

					if (errorCode < 0) {
						msg.arg1 = 2;
						handleAutoTrac.sendMessage(msg);
					} else {
						Thread th = new Thread(new Runnable() {
							public void run() {
								//showAddress(position_time);
							}
						});
						th.start();
					}
				///
					if (newfriendLat != 0 && newfriendLon != 0) {
						friendLat = newfriendLat;
						friendLon = newfriendLon; 
					}
					friendgeo = new GeoPoint(friendLat, friendLon);
					friendItem[i] = new OverlayItem(friendgeo, "a", "a");

				} else {
					msg.arg1 = 2;
					handleAutoTrac.sendMessage(msg);
				}

				Log.d(newfriendLat + "..." + newfriendLon);

					mHandler.removeCallbacks(friendLocationChange);
					mHandler.post(friendLocationChange);
				}
			}
		}
	};
	private Handler handleAutoTrac = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.arg1) {
			case 1:
				Toast.makeText(BDMapViewLocation.this, R.string.nonetwork,
						Toast.LENGTH_SHORT).show();
				break;
			case 2:
				Toast.makeText(BDMapViewLocation.this,
						R.string.location_not_found, Toast.LENGTH_LONG).show();
				break;
			}
		};
	};

	private void getphoto() {

		Bitmap bubbleblue = BitmapFactory.decodeResource(
				BDMapViewLocation.this.getResources(),
				R.drawable.bubble_backgroundn);
		Bitmap bubblegreen = BitmapFactory.decodeResource(
				BDMapViewLocation.this.getResources(),
				R.drawable.bubble_backgroundr);

		try {
			Drawable myPhoto = null;

			String mypath = mPref.read("myPhotoPath", null);
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
					Log.d("mDensity;"+mDensity+"newWidth"+newWidth);
					Log.d("myscaleWidth"+myscaleWidth+"width"+width);
					Log.d("bubblegreen.getWidth()"+bubbleblue.getWidth());
					Log.d("resizedMyBitmap"+resizedMyBitmap.getWidth());
			        bmp = Bitmap.createBitmap(bubbleblue.getWidth(), bubbleblue.getHeight(), bubbleblue.getConfig());   
			        Paint paint = new Paint();   
			        Canvas canvas = new Canvas(bmp);   
			        //首先绘制第一张图片，很简单，就是和方法中getDstImage一样   
			        canvas.drawBitmap(bubbleblue, 0, 0, paint);         
			           
			        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_OVER));   
			        canvas.drawBitmap(resizedMyBitmap, (float)mDensity*5.3f, (float)mDensity*4.6f, paint); 
			        myDrawable=new BitmapDrawable(bmp);
			        resizedMyBitmap.recycle();
			        resizedMyBitmap=null;
			        //myItem.setMarker(myDrawable);
				}catch(OutOfMemoryError e){}
				//resizedBitmap = null;
				//resizedBitmap.recycle();
			}
			myPhoto = null;
		} catch (Exception e) {
			e.printStackTrace();
		}

		AmpUserDB mFFdb = new AmpUserDB(BDMapViewLocation.this);
		mFFdb.open(true);
		String path[] = new String[length];
		Drawable[] friendPhoto = new Drawable[length];
		Log.d("shared_friends===="+shared_friends);
		if (length != 0) {

			for (int i = 0; i < length; i++) {
				try {
				mAddress[i] = shared_friends.get(i);
				idx[i] = mFFdb.getIdxByAddress(mAddress[i]);
				
					path[i] = Global.SdcardPath_inbox + "photo_" + idx[i]
							+ ".jpg";
					Log.d("path["+i+"]"+path[i]);
					try {
						friendPhoto[i] = ImageUtil.loadBitmapSafe(path[i], 2);
					} catch (OutOfMemoryError e) {
						System.gc();
						System.gc();
					}

					if (friendPhoto[i] == null)
						friendPhoto[i] = BDMapViewLocation.this.getResources()
								.getDrawable(R.drawable.bighead);

					if (friendPhoto[i] != null) {
						BitmapDrawable bm2 = (BitmapDrawable) friendPhoto[i];
						int height = bm2.getBitmap().getHeight();
						int width = bm2.getBitmap().getWidth();
						float scaleWidth = ((float) newWidth) / width;
						float scaleHeight = ((float) newHeight) / height;
						Matrix matrix = new Matrix();
						matrix.postScale(scaleWidth, scaleHeight);
						try {
							resizedFriendBitmap = Bitmap.createBitmap(
									bm2.getBitmap(), 0, 0, width, height,
									matrix, true);

							Bitmap bmp = null;
							// 下面这个Bitmap中创建的函数就可以创建一个空的Bitmap
							bmp = Bitmap.createBitmap(bubblegreen.getWidth(),
									bubblegreen.getHeight(),
									bubblegreen.getConfig());
							Paint paint = new Paint();
							Canvas canvas = new Canvas(bmp);
							// 首先绘制第一张图片，很简单，就是和方法中getDstImage一样
							canvas.drawBitmap(bubblegreen, 0, 0, paint);

							paint.setXfermode(new PorterDuffXfermode(
									Mode.SRC_OVER));
							canvas.drawBitmap(resizedFriendBitmap,
									(float) mDensity * 5.3f,
									(float) mDensity * 4.6f, paint);
							friendDrawable[i] = new BitmapDrawable(bmp);
							// friendItem.setMarker(myDrawable);
							resizedFriendBitmap.recycle();
							resizedFriendBitmap=null;

						} catch (OutOfMemoryError e) {
							Log.e("OutOfMemoryError!!!");
							System.gc();
							System.gc();
						}
					}
					friendPhoto[i] = null;
				} catch (Exception e) {
				}
			}
		}
		mFFdb.close();
		bubbleblue = null;
		bubblegreen = null;
		System.gc();
		System.gc();
	}

	private void UpdateDirections() {
		mHandler.removeCallbacks(updateAddrTask);
		mHandler.postDelayed(updateAddrTask, svTrack * 1000);
	}

	private void disableMyLocation() {/*
		for (int i = 0; i < length; i++) {
		friendmap[i].disableMyLocation();
		}
		mylayer.disableMyLocation();
		mylayer.disableCompass();
	*/}


	Runnable friendLocationChange = new Runnable() {
		@Override
		public void run() {
			mapOverlays.clear();
			mHandler.post(initOverlay);
		}
	};
	
/*	private boolean IsMyLatitudeMax(MyMap mylayer,MyMap[] friendmap) {
		boolean flag=true;
		int max=Math.abs(mylayer.getMyLocation().getLatitudeE6());
		for (int i = 0; i < length; i++) {
			int y= Math.abs(friendmap[i].getMyLocation().getLatitudeE6());
			if (max<y) {
				max=y;
				flag=false;
			}
		}
		return flag;
	}*/
	
	BroadcastReceiver MyLocationChange = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!mTrackable) return;
			if (intent.getAction().equals(MyLocation_Change)) {
				Log.i("MyLocationChange...");
				
				if (!TrackMark) {
					for (int i=0;i<length;i++) {
					if (!mAddress[i].equals("support"))
					{
						Message msg = new Message();
						final AireJupiter x = AireJupiter.getInstance();
						if (x == null)
							return;
						int errorCode = 0;
						String Return = "";
							try {
								Return = x.getFriendLocation(mAddress[i]);
								errorCode = 0;
							} catch (Exception e) {
								errorCode = -1;
								msg.arg1 = 1;
								handleAutoTrac.sendMessage(msg);
							}
							if (Return != null && Return.length() > 5) {
								String[] items = Return.split("/");
								try {
									newfriendLat = Integer.parseInt(items[0]) + 3512113;
									newfriendLon = Integer.parseInt(items[1]) - 10958121;
									position_time = Long
											.parseLong(items[2], 16);
									position_time *= 1000;
								} catch (NumberFormatException e) {
									errorCode = -1;
								}

								if (errorCode < 0) {
									msg.arg1 = 2;
									handleAutoTrac.sendMessage(msg);
								} else {
									Thread th = new Thread(new Runnable() {
										public void run() {
											//showAddress(position_time);
										}
									});
									th.start();
								}
							} else {
								msg.arg1 = 2;
								handleAutoTrac.sendMessage(msg);
							}

							if (newfriendLat != 0 && newfriendLon != 0) {
								friendLat = newfriendLat;
								friendLon = newfriendLon;
							}

							if (newfriendLat != friendLat
									|| newfriendLon != friendLon) {
								friendgeo = new GeoPoint(friendLat, friendLon);
								
								friendItem[i] = new OverlayItem(friendgeo, "a",
										"a");
							}
						}
					}
					int newmyLon = (int) mPref.readLong("longitude", Global.DEFAULT_LON);
					int newmyLat = (int) mPref.readLong("latitude", Global.DEFAULT_LAT);
					float newaccuracy = mPref.readFloat("accuracy", 80.0f);
					if (newmyLon != myLon || newmyLat != myLat || newaccuracy != myaccuracy)
					{
						myLon = newmyLon;
						myLat = newmyLat;
						myaccuracy = newaccuracy;
						myLocation = new LocationData();
						myLocation.accuracy=myaccuracy;
						mygeo = new GeoPoint(newmyLat, newmyLon);
						myItem = new OverlayItem(mygeo, "m", "m");
					}

					mHandler.removeCallbacks(friendLocationChange);
					mHandler.post(friendLocationChange);
				}
			}
			mHandler.post(initOverlay);
		}
	};
	
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
					Toast.makeText(BDMapViewLocation.this, "无网络", 1).show();
				}

			}

			@Override
			public void onGetPermissionState(int iError) {
				// TODO 授权的验证
				if (iError == MKEvent.ERROR_PERMISSION_DENIED) {
					Toast.makeText(BDMapViewLocation.this, "无权限", 1).show();
				}

			}

		});

	}
	
	private String myAddressInfo="";
	private String frAdressInfo="";
	private String addressInfo="";

	private class MySearchListener implements MKSearchListener {


		@Override
		public void onGetAddrResult(MKAddrInfo arg0, int arg1) {
			if (arg1 == 0) {
				addressInfo = arg0.strAddr;
			} else {
				Toast.makeText(getApplicationContext(), "未查询到结果", 1).show();
			}
		}

		@Override
		public void onGetDrivingRouteResult(MKDrivingRouteResult result,
				int iError) {
			if (result != null && iError == 0) {
				RouteOverlay drivingverlay = new RouteOverlay(BDMapViewLocation.this,
						mapView);

				if (result.getNumPlan() > 0) {
					MKRoutePlan plan = result.getPlan(0);// “一条”驾车或步行的路线

					MKRoute mkroute = plan.getRoute(0);
					drivingverlay.setData(mkroute);

					mapView.getOverlays().add(drivingverlay);
					mapView.refresh();
				}
			} else {
				Toast.makeText(getApplicationContext(), "未查询到结果", 1).show();
			}
		}

		@Override
		public void onGetWalkingRouteResult(MKWalkingRouteResult result,
				int iError) {
			if (result != null && iError == 0) {
				RouteOverlay walkingoverlay = new RouteOverlay(BDMapViewLocation.this,
						mapView);

				if (result.getNumPlan() > 0) {
					MKRoutePlan plan = result.getPlan(0);// “一条”驾车或步行的路线

					MKRoute mkroute = plan.getRoute(0);
					walkingoverlay.setData(mkroute);

					mapView.getOverlays().add(walkingoverlay);
					mapView.refresh();
				}
			} else {
				Toast.makeText(getApplicationContext(), "未查询到结果", 1).show();
			}

		}

		@Override
		public void onGetBusDetailResult(MKBusLineResult arg0, int arg1) {
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
	}
	
	public FrameLayout getNotificationLayout()
	{
		return (FrameLayout)findViewById(R.id.notification);
	}
}
