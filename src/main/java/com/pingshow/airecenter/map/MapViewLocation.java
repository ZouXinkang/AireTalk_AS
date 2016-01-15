package com.pingshow.airecenter.map;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.DialerFrame;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.LocationSettingActivity;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MainActivity;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.SecurityNewActivity;
import com.pingshow.airecenter.ShoppingActivity;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.util.ImageUtil;
import com.pingshow.voip.DialerActivity;
import com.pingshow.voip.VideoCallActivity;

public class MapViewLocation extends MapActivity implements OnClickListener{

	protected ArrayList<String> shared_friends;
	private MapView mapView;
	private MapController mc;
	private List<Overlay> mapOverlays;
	private float myaccuracy;
	private GeoPoint friendgeo, mygeo;
	private TextView AddrView;
	private RadioGroup rgp;
	private RadioButton walkingradioButton, drivingradioButton;
	private String type = null;

	private int origZoom;
	private GeoPoint origCenter;
	private int svTrack;
	//private String mAddress;
	private ToggleButton myzoom, yourzoom;
	private String address;

	private final int AUTOTRACK_INTERVAL_DRIVING = 15;
	private final int AUTOTRACK_INTERVAL_WALKING = 35;
	private final int TURNONGPS = 0;
	private final int TURNOFFGPS = 1;
	private Location[] friendLocation;
	private Location myLocation;
	private Handler mHandler = new Handler();;
	private long position_time;
	private boolean mTrackable;
	private LocationUpdate mLocation;

	private MyPreference mPref;
	private MyMap[] friendmap;
	private MyMap mylayer;
	private boolean TrackMark = false;
	private boolean isTrack = true;
	private int friendLat = 0, friendLon = 0;
	private int myLat = 0, myLon = 0;
	private int newfriendLat = 0, newfriendLon = 0;

	private final int LOC_MIN_TIME = 3600000;
	private final int LOC_MIN_DISTANCE = 200;

	private MapItems[] friendMarker;
	private MapItems myMarker;
	
	private OverlayItem[] friendmarker;
	private OverlayItem mymarker;
	private int newWidth = 75;
	private int newHeight = 75;
	private LineItemizedOverlay mOverlay;
	boolean turnon = false;
	private float mDensity;
	public static final String MyLocation_Change = "com.pingshow.airecenter.MyLocationChange";
	private Bitmap resizedMyBitmap=null;
	private Bitmap resizedFriendBitmap=null;
	int length;
	int[] idx;
	String[] mAddress;
	private TextView tv_location_setting;
	static public MapViewLocation _this = null;

	static public MapViewLocation getInstance() {
		return _this;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_page);
		
		if (getIntent().getBooleanExtra("launchFromSelf", false))
			this.overridePendingTransition(R.anim.freeze, R.anim.freeze);
		
		mDensity = getResources().getDisplayMetrics().density;

		newWidth = (int) (50 * mDensity);
		newHeight = (int) (50 * mDensity);

		mPref = new MyPreference(this);
		_this = this;

		walkingradioButton = (RadioButton) findViewById(R.id.walking);
		drivingradioButton = (RadioButton) findViewById(R.id.driving);

		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setDrawingCacheQuality(MapView.DRAWING_CACHE_QUALITY_LOW);
		mapView.setDrawingCacheEnabled(true);
		mapView.setBuiltInZoomControls(true);

		mapOverlays = mapView.getOverlays();
		mc = mapView.getController();

		tv_location_setting = (TextView) findViewById(R.id.location_setting);
		tv_location_setting.setOnClickListener(this);
		length=0;
		
		shared_friends=getIntent().getStringArrayListExtra("shared_friends");
		if (shared_friends!=null) {
			length=shared_friends.size();
		} else {
			shared_friends = new ArrayList<String>();
		}
		Log.d("loc shared_friends===" + shared_friends + " length===" + length);
		idx=new int[length];
		mAddress=new String[length];
		friendLocation=new Location[length];
		friendmap=new MyMap[length];
		friendMarker=new MapItems[length];
		friendmarker=new OverlayItem[length];

		mTrackable=getIntent().getBooleanExtra("trackable", false);
		
		getphoto();
		//tml*** solo loc X
//		if (mTrackable){
//			myLat = (int) mPref.readLong("latitude", Global.DEFAULT_LAT);
//			myLon = (int) mPref.readLong("longitude", Global.DEFAULT_LON);
//		}
//		else{
//			myLat = (int) getIntent().getLongExtra("mylatitude", Global.DEFAULT_LAT);
//			myLon = (int) getIntent().getLongExtra("mylongitude", Global.DEFAULT_LON);
//		}
		
		friendLat = (int) getIntent().getLongExtra("latitude", Global.DEFAULT_LAT);
		friendLon = (int) getIntent().getLongExtra("longitude", Global.DEFAULT_LON);

		enablemyLocation();
//		enableFriendLocation();  //tml*** solo loc X
		Log.d("locs my:" + myLat + "," + myLon + " f:" + friendLat + "," + friendLon);
		//tml*** solo loc X
//		if ((Math.abs(myLat) > Math.abs(friendLat))) {
			firstMyAddOverlays();
//		} else {
//			firstFAddOverlays();
//		}
//		
//		appoint();

		/*myzoom = (ToggleButton) findViewById(R.id.myzoom);
		yourzoom = (ToggleButton) findViewById(R.id.yourzoom);
		AddrView = (TextView) findViewById(R.id.user_address);

		myzoom.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton button,
					boolean isChecked) {
				boolean yourChecked = yourzoom.isChecked();
				if (!isChecked && !yourChecked) { // ok, the focus changed,. let
													// zomyzooom to my position
													// as mych as possible
					mc.animateTo(origCenter);
					mc.setZoom(origZoom);
				} else if (isChecked && !yourChecked) {
					mc.animateTo(mylayer.getMyLocation());
					int maxZoomLevel = mapView.getMaxZoomLevel() - 2;
					if (maxZoomLevel > 20)
						maxZoomLevel = 20;
					mc.setZoom(maxZoomLevel);
					if (yourzoom.isChecked()) {
						yourzoom.setChecked(false);
					}
					((RelativeLayout) findViewById(R.id.address_view)).setVisibility(View.VISIBLE);
				} else if (isChecked && yourChecked) {
					mc.animateTo(origCenter);
					mc.setZoom(origZoom);
					yourzoom.setChecked(false);
					myzoom.setChecked(false);
				}
				Thread th = new Thread(new Runnable() {
					public void run() {
						showAddress();
					}
				});
				th.start();
				System.gc();
				System.gc();
			}
		});

		yourzoom.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton button,
					boolean isChecked) {
				boolean myChecked = myzoom.isChecked();
				if (!isChecked && !myChecked) { // ok, the focus changed,. let
												// zomyzooom to my position as
												// mych as possible
					mc.animateTo(origCenter);
					mc.setZoom(origZoom);
				} else if (isChecked && !myChecked) {
					mc.animateTo(friendmap.getMyLocation());
					int maxZoomLevel = mapView.getMaxZoomLevel() - 2;
					if (maxZoomLevel > 20)
						maxZoomLevel = 20;
					mc.setZoom(maxZoomLevel);
					if (myzoom.isChecked()) {
						myzoom.setChecked(false);
					}
					((RelativeLayout) findViewById(R.id.address_view)).setVisibility(View.VISIBLE);
				} else if (isChecked && myChecked) {
					mc.animateTo(origCenter);
					mc.setZoom(origZoom);
					yourzoom.setChecked(false);
					myzoom.setChecked(false);
				}
				Thread th = new Thread(new Runnable() {
					public void run() {
						showAddress();
					}
				});
				th.start();
				System.gc();
				System.gc();
			}
		});
		
		ToggleButton autotrack = (ToggleButton) findViewById(R.id.autotrack);
		autotrack.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				mHandler.removeCallbacks(updateAddrTask);
				if (isChecked) {
					rgp.setVisibility(View.VISIBLE);
					walkingradioButton.setChecked(true);
				} else {
					rgp.setVisibility(View.GONE);
					mapOverlays.clear();
					if ((Math.abs(mylayer.getMyLocation().getLatitudeE6()) > Math
							.abs(friendmap.getMyLocation().getLatitudeE6()))) {
						firstMyAddOverlays();
					} else {
						firstFAddOverlays();
					}
					TrackMark = true;
					isTrack = true;
				}
				System.gc();
				System.gc();
			}
		});

		svTrack = AUTOTRACK_INTERVAL_DRIVING;
		rgp = (RadioGroup) findViewById(R.id.select_directions);
		rgp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == drivingradioButton.getId()) {
					type = "driving";
					svTrack = AUTOTRACK_INTERVAL_DRIVING;
				} else if (checkedId == walkingradioButton.getId()) {
					type = "walking";
					svTrack = AUTOTRACK_INTERVAL_WALKING;
				}
				UpdateDirections();
				TrackMark = true;
				System.gc();
				System.gc();
			}
		});

		ToggleButton showtraffic = (ToggleButton) findViewById(R.id.showtraffic);
		showtraffic.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton button,
					boolean isChecked) {
				if (isChecked) // ok, the focus changed,. let zomyzooom to my
								// position as mych as possible
					mapView.setTraffic(true);
				else
					mapView.setTraffic(false);
				mPref.write("TrafficView", isChecked);
				System.gc();
				System.gc();
			}
		});

		ToggleButton showsatellite = (ToggleButton) findViewById(R.id.showsatellite);
		showsatellite.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton button,
					boolean isChecked) {
				//changeTextColor(isChecked);
				if (isChecked)
					mapView.setSatellite(true);
				else
					mapView.setSatellite(false);
				mPref.write("SatelliteView", isChecked);
				System.gc();
				System.gc();
			}
		});
		
		((ImageView) findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				finish();
			}
		});*/
		
		((ImageView) findViewById(R.id.close)).setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				((RelativeLayout) findViewById(R.id.address_view)).setVisibility(View.GONE);
			}
		});
		//tml*** beta ui
        ((Button)findViewById(R.id.back)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
        
		/*showsatellite.setChecked(mPref.readBoolean("SatelliteView", false));

		showtraffic.setChecked(mPref.readBoolean("TrafficView", false));*/
		
		//changeTextColor(mPref.readBoolean("SatelliteView", false));

		Thread th = new Thread(new Runnable() {
			public void run() {
				showAddress();
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
			//mHandler.postDelayed(updateAddrTask, 20000);
		}
		
		
		DialerFrame.setFrame(this, findViewById(android.R.id.content));
		
////// SideBar
        
        ((ImageView)findViewById(R.id.bar1)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it=new Intent(MapViewLocation.this, MainActivity.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
        ((ImageView)findViewById(R.id.bar6)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it=new Intent(MapViewLocation.this, ShoppingActivity.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
        ((ImageView)findViewById(R.id.bar7)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it=new Intent(MapViewLocation.this, SecurityNewActivity.class);
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
	
	Runnable autoUpdateLocation=new Runnable() {
		@Override
		public void run() {
			if (AireJupiter.getInstance()!=null)
			{
				if (mLocation==null)
					mLocation=new LocationUpdate(MapViewLocation.this, mHandler, AireJupiter.getInstance().tcpSocket(), mPref, false);
			}
		}
	};

	private double getMaxDistance() {
		double max=0;
		double[] distance=new double[length];
		for (int i = 0; i < length; i++) {
			if (friendmap[i]!=null) {
				Log.d("friendmap[i]不为空");
				friendgeo=friendmap[i].getMyLocation();
			}
			if (friendgeo!=null) {
				distance[i]=getDistance(friendgeo, mygeo);
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
		IntentFilter intentToReceiveFilter = new IntentFilter();
		intentToReceiveFilter.addAction(MapViewLocation.MyLocation_Change);
		this.registerReceiver(MyLocationChange, intentToReceiveFilter);
		
		DialerFrame.checkEmbeddedDialer(findViewById(android.R.id.content));
	}
	
	Runnable refreshStatus=new Runnable()
	{
		public void run()
		{
			if ((DialerActivity.minimized && DialerActivity.getDialer()!=null) || VideoCallActivity.minimized)
			{
				LinearLayout oncallView=(LinearLayout) findViewById(R.id.oncall);
	    		((TextView)oncallView.findViewById(R.id.displayname)).setText(DialerActivity.getDialer().getCurrentOnCallName());
	    		((TextView)oncallView.findViewById(R.id.status)).setText(DialerActivity.getDialer().getCurrentOnCallStatus());
				mHandler.postDelayed(refreshStatus,1000);
			}
			else
			{
				LinearLayout oncallView=(LinearLayout) findViewById(R.id.oncall);
				oncallView.setVisibility(View.GONE);
			}
		}
	};
	private OverlayItem fmarker;
	
	@Override
	protected void onPause() {
		unregisterReceiver(MyLocationChange);
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		svTrack = 0;
		mapOverlays.clear();
		mHandler.removeCallbacks(autoUpdateLocation);
		mHandler.removeCallbacks(updateAddrTask);
		mHandler.removeCallbacks(addmypoint);
		mHandler.removeCallbacks(addfriendpoint);
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
		}catch(Exception e){
		}
		
		System.gc();
		System.gc();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		svTrack = 0;
		mHandler.removeCallbacks(autoUpdateLocation);
		mHandler.removeCallbacks(updateAddrTask);
		mHandler.removeCallbacks(addmypoint);
		mHandler.removeCallbacks(addfriendpoint);
		mHandler.removeCallbacks(friendLocationChange);
		System.gc();
		super.onBackPressed();
	}

	/*
	private void toggleGPS() {
		Intent gpsIntent = new Intent();
		gpsIntent.setClassName("com.android.settings",
				"com.android.settings.widget.SettingsAppWidgetProvider");
		gpsIntent.addCategory("android.intent.category.ALTERNATIVE");
		gpsIntent.setData(Uri.parse("custom:3"));
		try {
			PendingIntent.getBroadcast(this, 0, gpsIntent, 0).send();
		} catch (CanceledException e) {
			e.printStackTrace();
		}
	}*/

	private void enablemyLocation() {
		if (mTrackable)
		{
//			myLon = (int) mPref.readLong("longitude", Global.DEFAULT_LON);
//			myLat = (int) mPref.readLong("latitude", Global.DEFAULT_LAT);
			myaccuracy = mPref.readFloat("accuracy", 80.0f);
		}
		else{
//			myLon = (int) getIntent().getLongExtra("mylongitude", Global.DEFAULT_LON);
//			myLat = (int) getIntent().getLongExtra("mylatitude", Global.DEFAULT_LAT);
			myaccuracy = 0;
		}
//		if (myLat == 0 && myLat == 0) {
//			myLon = (int)Global.DEFAULT_LON;
//			myLat = (int)Global.DEFAULT_LAT;
//		}
		//tml*** solo loc
		myLat = friendLat;
		myLon = friendLon;
		
		myLocation = new Location("myLocation");
		myLocation.setLatitude(myLat);
		myLocation.setLongitude(myLon);
		myLocation.setAccuracy(myaccuracy);
		mygeo = new GeoPoint(myLat, myLon);
		mylayer = new MyMap(this, mapView, myLocation, mygeo);
		mylayer.enableMyLocation();
		mylayer.enableCompass();
		mylayer.onLocationChanged(myLocation);
		mHandler.post(addmypoint);
		if (myMarker != null)
			myMarker.removeAll();
		mymarker = new OverlayItem(mygeo, "m", "m");
		myMarker.addOverlay(mymarker);
	}

	private void enableFriendLocation() {
		if (length==0) {
			friendgeo = new GeoPoint(friendLat, friendLon);

			if (fMarker!= null)
				fMarker.removeAll();
			fmarker= new OverlayItem(friendgeo, "f" , "f");
			fMarker.addOverlay(fmarker);
		}
		for (int i = 0; i < length; i++) {
			mAddress[i]=shared_friends.get(i);
			Log.d("mAddress[i]====="+mAddress[i]);
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
					friendLocation[i] = new Location("friendLocation");
					friendLocation[i].setLatitude(friendLat);
					friendLocation[i].setLongitude(friendLon);
					friendLocation[i].setAccuracy((float) 60.0);

					friendmap[i] = new MyMap(this, mapView, friendLocation[i],
							friendgeo);
					friendmap[i].enableMyLocation();
					friendmap[i].onLocationChanged(friendLocation[i]);
					if (friendMarker[i] != null)
						friendMarker[i].removeAll();
					friendmarker[i] = new OverlayItem(friendgeo, "f" + i, "f"
							+ i);
					friendMarker[i].addOverlay(friendmarker[i]);
				
			}
		}
		mHandler.post(addfriendpoint);
	}

	void firstMyAddOverlays() {
		mapOverlays.add(mylayer);
		mapOverlays.add(myMarker);
		if (shared_friends!=null) {
			for (int i = 0; i < length; i++) {
				mapOverlays.add(friendmap[i]);
				mapOverlays.add(friendMarker[i]);
			}
		}
	}

	void firstFAddOverlays() {
		if (shared_friends != null) {
			for (int i = 0; i < length; i++) {
				mapOverlays.add(friendmap[i]);
				mapOverlays.add(friendMarker[i]);
			}
		}
		mapOverlays.add(mylayer);
		mapOverlays.add(myMarker);
	}

	private Runnable addmypoint = new Runnable() {
		public void run() {
			(new Thread(addmypoint1, "addmypoint1")).start();
			mHandler.postDelayed(this, 10000);
		}
	};
	private Runnable addfriendpoint = new Runnable() {
		public void run() {
			(new Thread(addfriendpoint1, "addfriendpoint1")).start();
			mHandler.postDelayed(this, 15000);
		}
	};
	private Runnable addmypoint1 = new Runnable() {
		@Override
		public void run() {
			myLocation.setLatitude((double)myLat);
			myLocation.setLongitude((double)myLon);
			myLocation.setAccuracy(myaccuracy);
			mylayer.onLocationChanged(myLocation);
		}
	};
	private Runnable addfriendpoint1 = new Runnable() {
		@Override
		public void run() {
			for (int i = 0; i < length;i++) {
				if (friendmap[i] != null) {
					if (friendmap[i].getLastFix() != null)// alec
						friendmap[i].onLocationChanged(friendmap[i]
								.getLastFix());
				}
			}
		}
	};
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
			myLocation = new Location("myLocation");
			myLocation.setAccuracy(myaccuracy);
			mygeo = new GeoPoint(newmyLat, newmyLon);
			mylayer.setMyLocation(mygeo);
			mylayer.setLastFix(myLocation);
			if (myMarker != null)
				myMarker.removeAll();
			mymarker = new OverlayItem(mygeo, "m", "m");
			myMarker.addOverlay(mymarker);
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
					friendmap[i].setMyLocation(friendgeo);
					if (friendMarker[i] != null)
						friendMarker[i] .removeAll();
					friendmarker[i] = new OverlayItem(friendgeo, "a", "a");
					friendMarker[i].addOverlay(friendmarker[i]);
				} else {
					msg.arg1 = 2;
					handleAutoTrac.sendMessage(msg);
				}

				Log.d(newfriendLat + "..." + newfriendLon);

				if (isTrack) {
					new GoogleDirection()
							.execute((double) mygeo.getLatitudeE6() / 1000000
									+ "," + (double) mygeo.getLongitudeE6()
									/ 1000000, (double) friendLat / 1000000
									+ "," + (double) friendLon / 1000000);
				} else {

					mHandler.removeCallbacks(friendLocationChange);
					mHandler.post(friendLocationChange);
				}
				}
			}
		}
	};
	private Handler handleAutoTrac = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.arg1) {
			case 1:
				Toast.makeText(MapViewLocation.this, R.string.nonetwork,
						Toast.LENGTH_SHORT).show();
				break;
			case 2:
				Toast.makeText(MapViewLocation.this,
						R.string.location_not_found, Toast.LENGTH_LONG).show();
				break;
			}
		};
	};
	private MapItems fMarker;

	private void showAddress() {
		showAddress(new Date().getTime());
	}

	private void showAddress(long pos_time) {/*
		String addr;
		//if (myzoom.isChecked()) {
			addr = getAddrFromGeo(MapViewLocation.this, myLat, myLon);
			if (!mTrackable)
				pos_time = getIntent().getLongExtra("time", 0);
			String time = DateUtils.formatDateTime(MapViewLocation.this,
					pos_time, DateUtils.FORMAT_SHOW_TIME
							| DateUtils.FORMAT_SHOW_DATE);
			final String myaddress = getResources().getString(R.string.mylocation_address)
					+ ": " + time + "\n" + addr;
		} else {
			addr = getAddrFromGeo(MapViewLocation.this, friendLat, friendLon);
			long lTime = getIntent().getLongExtra("time", 0);
			String time = DateUtils.formatDateTime(MapViewLocation.this,
					lTime, DateUtils.FORMAT_SHOW_TIME
							| DateUtils.FORMAT_SHOW_DATE);

			if (time != null && time.length() > 0)
				address = getResources().getString(R.string.friend_address)
						+ ": " + time + "\n" + addr;
			else
				address = getResources().getString(R.string.friend_address)
						+ ": " + addr;
		}

		mHandler.post(new Runnable() {
			public void run() {
				AddrView.setText(myaddress);
			}
		});
	*/}

	public String getAddrFromGeo(Context context, int lat, int lon) {
		if (lat == 0 && lon == 0)
			return "";// alec

		String addr = "";

		Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
		List<Address> addresses;
		try {
			addresses = geoCoder.getFromLocation((double) (lat) / 1E6,
					(double) (lon) / 1E6, 1);

			if (addresses != null && addresses.size() > 0) {

				int i = 0;
				String al = "";

				while ((al = addresses.get(0).getAddressLine(i)) != null) {
					if (addr.length() > 0 && i != 0)
						addr += ",  ";
					addr += al;
					i++;
				}
			}
		} catch (IllegalArgumentException e) {// Errors in lat/lng
		} catch (IOException e) {// network not available
		}
		return addr;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	public double getDistance(GeoPoint A, GeoPoint B) {
		double lat1 = A.getLatitudeE6() / 1E6;
		double lat2 = B.getLatitudeE6() / 1E6;
		double lon1 = A.getLongitudeE6() / 1E6;
		double lon2 = B.getLongitudeE6() / 1E6;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
				* Math.sin(dLon / 2);
		double c = 2.0 * Math.asin(Math.sqrt(a));
		return c * 6371.0;
	}

	private void getphoto() {

		Bitmap bubbleblue = BitmapFactory.decodeResource(
				MapViewLocation.this.getResources(),
				R.drawable.bubble_backgroundn);
//		Bitmap bubblegreen = BitmapFactory.decodeResource(
//				MapViewLocation.this.getResources(),
//				R.drawable.bubble_backgroundr);
		//tml*** solo loc
		AmpUserDB mFFdb = new AmpUserDB(MapViewLocation.this);
		mFFdb.open(true);
		try {
			Drawable myPhoto = null;

//			String mypath = mPref.read("myPhotoPath", null);
			//tml*** solo loc
			String st_addr = getIntent().getStringExtra("Address");
			int id = mFFdb.getIdxByAddress(st_addr);
			String mypath = Global.SdcardPath_inbox + "photo_" + id + ".jpg";
			
			if (mypath != null && mypath.length() > 0) {
				try {
					myPhoto = ImageUtil.loadBitmapSafe(mypath, 2);
				} catch (OutOfMemoryError e) {
					System.gc();
					System.gc();
				}
			}

			if (myPhoto == null)
				myPhoto = MapViewLocation.this.getResources()
						.getDrawable(R.drawable.bighead);

			if (myPhoto != null) {
				try {
					BitmapDrawable bm2 = (BitmapDrawable) myPhoto;
					int height = bm2.getBitmap().getHeight();
					int width = bm2.getBitmap().getWidth();
					float myscaleWidth = ((float) newWidth) / height;
					float myscaleHeight = ((float) newHeight) / width;
					Matrix mymatrix = new Matrix();
					mymatrix.postScale(myscaleWidth, myscaleHeight);
					resizedMyBitmap = Bitmap.createBitmap(bm2.getBitmap(), 0,
							0, width, height, mymatrix, true);
					Drawable[] array = new Drawable[2];
					array[0] = new BitmapDrawable(bubbleblue);
					array[1] = new BitmapDrawable(resizedMyBitmap);
					LayerDrawable layers = new LayerDrawable(array);
					layers.setLayerInset(0, 0, 0, 0, 0);
					layers.setLayerInset(1, 8, 8, 8, 42);

					myMarker = new MapItems(layers,
							MapViewLocation.this, mapView, 0);
					array[0] = null;
					array[1] = null;
				} catch (OutOfMemoryError e) {
				}
				// resizedBitmap = null;
				// resizedBitmap.recycle();
			}
			myPhoto = null;
		} catch (Exception e) {
			Log.e("getphoto " + e.getMessage());
		}

//		AmpUserDB mFFdb = new AmpUserDB(MapViewLocation.this);
//		mFFdb.open(true);
//		String path[] = new String[length];
//		Drawable[] friendPhoto = new Drawable[length];
//		Log.d("shared_friends===="+shared_friends);
//		if (length==0) {
//			String st_addr=getIntent().getStringExtra("Address");
//			int id=mFFdb.getIdxByAddress(st_addr);
//			mFFdb.close();
//
//			try {
//				Drawable fPhoto = null;
//				String fpath = Global.SdcardPath_inbox + "photo_" + id + ".jpg";
//				try{
//					fPhoto = ImageUtil.loadBitmapSafe(fpath, 2);
//				}catch(OutOfMemoryError e){
//					System.gc();
//					System.gc();
//				}
//
//				if (fPhoto == null)
//					fPhoto = getResources().getDrawable(R.drawable.bighead);
//
//				if (fPhoto != null) {
//					BitmapDrawable bm2 = (BitmapDrawable) fPhoto;
//					int height = bm2.getBitmap().getHeight();
//					int width = bm2.getBitmap().getWidth();
//					float scaleWidth = ((float) newWidth) / width;
//					float scaleHeight = ((float) newHeight) / height;
//					Matrix matrix = new Matrix();
//					matrix.postScale(scaleWidth, scaleHeight);
//					try{
//						resizedFriendBitmap = Bitmap.createBitmap(bm2.getBitmap(), 0,
//								0, width, height, matrix, true);
//						Drawable[] array = new Drawable[2];
//						array[0] = new BitmapDrawable(bubblegreen);
//						array[1] = new BitmapDrawable(resizedFriendBitmap);
//		
//						LayerDrawable layers = new LayerDrawable(array);
//						layers.setLayerInset(0, 0, 0, 0, 0);
//						layers.setLayerInset(1, 8, 8, 8, 42);
//						fMarker = new MapItems(layers, this, mapView, 0);
//						array[0] = null;
//						array[1] = null;
//					}catch(OutOfMemoryError e){}
//				}
//				fPhoto = null;
//			} catch (Exception e) {
//			}
//		}
//		
//		for (int i = 0; i < length; i++) {
//			if (shared_friends != null) {
//				mAddress[i] = shared_friends.get(i);
//				idx[i] = mFFdb.getIdxByAddress(mAddress[i]);
//			}
//
//			try {
//				path[i] = Global.SdcardPath_inbox + "photo_" + idx[i] + ".jpg";
//				try {
//					friendPhoto[i] = ImageUtil.loadBitmapSafe(path[i], 2);
//				} catch (OutOfMemoryError e) {
//					System.gc();
//					System.gc();
//				}
//
//				if (friendPhoto[i] == null)
//					friendPhoto[i] = MapViewLocation.this.getResources()
//							.getDrawable(R.drawable.bighead);
//
//				if (friendPhoto[i] != null) {
//					BitmapDrawable bm2 = (BitmapDrawable) friendPhoto[i];
//					int height = bm2.getBitmap().getHeight();
//					int width = bm2.getBitmap().getWidth();
//					float scaleWidth = ((float) newWidth) / width;
//					float scaleHeight = ((float) newHeight) / height;
//					Matrix matrix = new Matrix();
//					matrix.postScale(scaleWidth, scaleHeight);
//					try {
//						resizedFriendBitmap = Bitmap.createBitmap(
//								bm2.getBitmap(), 0, 0, width, height, matrix,
//								true);
//						Drawable[] array = new Drawable[2];
//						array[0] = new BitmapDrawable(bubblegreen);
//						array[1] = new BitmapDrawable(resizedFriendBitmap);
//
//						LayerDrawable layers = new LayerDrawable(array);
//						layers.setLayerInset(0, 0, 0, 0, 0);
//						layers.setLayerInset(1, 8, 8, 8, 42);
//						friendMarker[i] = new MapItems(layers,
//								MapViewLocation.this, mapView, 0);
//						Log.d("friendMarker["+i+"] creat sucess!!");
//						array[0] = null;
//						array[1] = null;
//					} catch (OutOfMemoryError e) {
//						Log.e("OutOfMemoryError!!!");
//						System.gc();
//						System.gc();
//					}
//				}
//				friendPhoto[i] = null;
//			} catch (Exception e) {
//			}
//		}
		mFFdb.close();
		bubbleblue = null;
//		bubblegreen = null;
		System.gc();
		System.gc();
	}

	private void UpdateDirections() {
		mHandler.removeCallbacks(updateAddrTask);
		mHandler.postDelayed(updateAddrTask, svTrack * 1000);
		new GoogleDirection().execute(
				(double) mylayer.getMyLocation().getLatitudeE6() / 1E6 + "," +
				(double) mylayer.getMyLocation().getLongitudeE6() / 1E6,
				(double) friendLat / 1E6 + "," + (double) friendLon / 1E6);
	}

	private void disableMyLocation() {
		for (int i = 0; i < length; i++) {
		friendmap[i].disableMyLocation();
		}
		mylayer.disableMyLocation();
		mylayer.disableCompass();
	}

	private class GoogleDirection extends
			AsyncTask<String, Integer, List<GeoPoint>> {
		private final String mapAPI = "http://maps.google.com/maps/api/directions/json?"
				+ "origin={0}&destination={1}&language=zh-TW&sensor=true&mode="
				+ type;
		private String _from;
		private String _to;
		private List<GeoPoint> _points = new ArrayList<GeoPoint>();

		@Override
		protected List<GeoPoint> doInBackground(String... params) {
			if (params.length < 0)
				return null;
			_from = params[0];
			_to = params[1];
			String url = MessageFormat.format(mapAPI, _from, _to);
			Log.i(_from + "," + _to);
			Log.i(url);
			HttpGet get = new HttpGet(url);
			String strResult = "";
			try {
				HttpParams httpParameters = new BasicHttpParams();
				HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
				HttpClient httpClient = new DefaultHttpClient(httpParameters);
				HttpResponse httpResponse = null;
				httpResponse = httpClient.execute(get);
				if (httpResponse.getStatusLine().getStatusCode() == 200) {
					strResult = EntityUtils.toString(httpResponse.getEntity());
					JSONObject jsonObject = new JSONObject(strResult);
					JSONArray routeObject = jsonObject.getJSONArray("routes");
					String polyline = routeObject.getJSONObject(0)
							.getJSONObject("overview_polyline")
							.getString("points");
					if (polyline.length() > 0) {
						decodePolylines(polyline);
					}
				}
			} catch (Exception e) {
				Log.d(e.toString());
				mHandler.post(new Runnable() {
					public void run() {
						Toast.makeText(MapViewLocation.this,
								R.string.notfound_direction, Toast.LENGTH_SHORT)
								.show();
						mapOverlays.clear();
						if (IsMyLatitudeMax(mylayer,friendmap)){
							firstMyAddOverlays();
						} else {
							firstFAddOverlays();
						}
						/*
						if (myzoom.isChecked() && !yourzoom.isChecked()) {
							mc.animateTo(mylayer.getMyLocation());
						} else if (yourzoom.isChecked() && !myzoom.isChecked()) {
							mc.animateTo(friendmap.getMyLocation());
						} else*/
							appoint();
						System.gc();
					}
				});
			}catch(OutOfMemoryError e){}
			return _points;
		}

		private void decodePolylines(String poly) {
			int len = poly.length();
			int index = 0;
			int lat = 0;
			int lng = 0;

			while (index < len) {
				int b, shift = 0, result = 0;
				do {
					b = poly.charAt(index++) - 63;
					result |= (b & 0x1f) << shift;
					shift += 5;
				} while (b >= 0x20);
				int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
				lat += dlat;

				shift = 0;
				result = 0;
				do {
					b = poly.charAt(index++) - 63;
					result |= (b & 0x1f) << shift;
					shift += 5;
				} while (b >= 0x20);
				int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
				lng += dlng;

				GeoPoint p = new GeoPoint((int) (((double) lat / 1E5) * 1E6),
						(int) (((double) lng / 1E5) * 1E6));
				_points.add(p);

			}
		}

		protected void onPostExecute(List<GeoPoint> points) {
			if (points.size() > 0) {
				mOverlay = new LineItemizedOverlay(points);
				mapOverlays.clear();
				if (IsMyLatitudeMax(mylayer,friendmap)) {
					firstMyAddOverlays();
				} else {
					firstFAddOverlays();
				}
				mapOverlays.add(0, mOverlay);
				/*if (myzoom.isChecked() && !yourzoom.isChecked()) {
					mc.animateTo(mylayer.getMyLocation());
				} else if (yourzoom.isChecked() && !myzoom.isChecked()) {
					mc.animateTo(friendmap.getMyLocation());
				} else*/
					appoint();
				System.gc();
			}
		}
	}

	Runnable friendLocationChange = new Runnable() {
		@Override
		public void run() {
			mapOverlays.clear();
			if (IsMyLatitudeMax(mylayer,friendmap)) {
				firstMyAddOverlays();
			} else {
				firstFAddOverlays();
			}
			mapView.invalidate();
			/*if (myzoom.isChecked() && !yourzoom.isChecked()) {
				mc.animateTo(mylayer.getMyLocation());
			} else if (yourzoom.isChecked() && !myzoom.isChecked()) {
				mc.animateTo(friendmap.getMyLocation());
			} else*/
				appoint();
		}
	};
	
	private boolean IsMyLatitudeMax(MyMap mylayer,MyMap[] friendmap) {
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
	}
	
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
								friendmap[i].setMyLocation(friendgeo);
								if (friendMarker[i] != null)
									friendMarker[i].removeAll();
								friendmarker[i] = new OverlayItem(friendgeo, "a",
										"a");
								friendMarker[i].addOverlay(friendmarker[i]);
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
						myLocation = new Location("myLocation");
						myLocation.setAccuracy(myaccuracy);
						mygeo = new GeoPoint(newmyLat, newmyLon);
						mylayer.setMyLocation(mygeo);
						mylayer.setLastFix(myLocation);
						if (myMarker != null)
							myMarker.removeAll();
						mymarker = new OverlayItem(mygeo, "m", "m");
						myMarker.addOverlay(mymarker);
					}

					mHandler.removeCallbacks(friendLocationChange);
					mHandler.post(friendLocationChange);
				}
			}
		}
	};

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.location_setting:
			Intent intent=new Intent(MapViewLocation.this, LocationSettingActivity.class);
			intent.putExtra("launchFromSelf", true);
			startActivity(intent);
			finish();
			//mHandler.post(popupProgressDialog);
			break;
		}
	}
	
	public FrameLayout getNotificationLayout()
	{
		return (FrameLayout)findViewById(R.id.notification);
	}
}
