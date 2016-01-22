package com.pingshow.amper.map;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.R;
import com.pingshow.util.ImageUtil;

public class SelfMapView extends MapActivity {

	private MapView mapView;
	private MapController mc;
	private List<Overlay> mapOverlays;
	private float myaccuracy;
	private GeoPoint mygeo;
	private TextView AddrView;
	private RadioButton walkingradioButton, drivingradioButton;

	private MapItems myMarker;
	private Location myLocation;

	private int origZoom;
	private ToggleButton myzoom, yourzoom;
	private String address;
	
	private Handler mHandler = null;
	private long position_time;

	private MyPreference mPref;
	private int myLat = 0, myLon = 0;
	private OverlayItem mymarker;
	private int newWidth = 75;
	private int newHeight = 75;
	boolean turnon = false;
	private float mDensity;
	private Bitmap resizedMyBitmap=null;

	static public SelfMapView instance = null;

	static public SelfMapView getInstance() {
		return instance;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_page);
		this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
		
		mDensity = getResources().getDisplayMetrics().density;

		newWidth = (int) (50 * mDensity);
		newHeight = (int) (50 * mDensity);

		mPref = new MyPreference(this);
		instance = this;

		mHandler = new Handler();

		((ImageView) findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				finish();
			}
		});
		
		((ImageView) findViewById(R.id.close)).setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				((RelativeLayout) findViewById(R.id.address_view)).setVisibility(View.GONE);
			}
		});

		walkingradioButton = (RadioButton) findViewById(R.id.walking);
		drivingradioButton = (RadioButton) findViewById(R.id.driving);
		walkingradioButton.setVisibility(View.GONE);
		drivingradioButton.setVisibility(View.GONE);

		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setDrawingCacheQuality(MapView.DRAWING_CACHE_QUALITY_LOW);
		mapView.setDrawingCacheEnabled(true);
		mapView.setBuiltInZoomControls(true);
		
		position_time=getIntent().getLongExtra("time", new Date().getTime());

		mapOverlays = mapView.getOverlays();
		mc = mapView.getController();

		getphoto();
		enablemyLocation();
		addOverlays();
		appoint();

		myzoom = (ToggleButton) findViewById(R.id.myzoom);
		yourzoom = (ToggleButton) findViewById(R.id.yourzoom);
		AddrView = (TextView) findViewById(R.id.user_address);
		((ToggleButton) findViewById(R.id.autotrack)).setVisibility(View.GONE);
		yourzoom.setVisibility(View.GONE);
		
		myzoom.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton button,
					boolean isChecked) {
				if (!isChecked) {
					mc.animateTo(mygeo);
					mc.setZoom(origZoom);
				} else if (isChecked) {
					mc.animateTo(mygeo);
					int maxZoomLevel = mapView.getMaxZoomLevel() - 2;
					if (maxZoomLevel > 20)
						maxZoomLevel = 20;
					mc.setZoom(maxZoomLevel);
					((RelativeLayout) findViewById(R.id.address_view)).setVisibility(View.VISIBLE);
				}
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

		showsatellite.setChecked(mPref.readBoolean("SatelliteView", false));

		showtraffic.setChecked(mPref.readBoolean("TrafficView", false));
		
		//changeTextColor(mPref.readBoolean("SatelliteView", false));

		Thread th = new Thread(new Runnable() {
			public void run() {
				showAddress(position_time);
				System.gc();
				System.gc();
			}
		});
		th.start();
	}

	void appoint() {
		mc.setZoom(15);
		mc.animateTo(mygeo);
		origZoom = mapView.getZoomLevel();
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
	protected void onDestroy() {
		mapOverlays.clear();

		if(!resizedMyBitmap.isRecycled())
			resizedMyBitmap.recycle();
		instance = null;
		
		System.gc();
		System.gc();
		super.onDestroy();
	}

	private void enablemyLocation() {
		myLon = (int) getIntent().getLongExtra("longitude", 116349386);
		myLat = (int) getIntent().getLongExtra("latitude", 39976279);
		
		myaccuracy = mPref.readFloat("accuracy", 50.0f);
		myLocation = new Location("myLocation");
		myLocation.setLatitude(myLat);
		myLocation.setLongitude(myLon);
		myLocation.setAccuracy(myaccuracy);
		mygeo = new GeoPoint(myLat, myLon);
		
		if (myMarker != null)
			myMarker.removeAll();
		mymarker = new OverlayItem(mygeo, "m", "m");
		myMarker.addOverlay(mymarker);
	}

	void addOverlays() {
		mapOverlays.add(myMarker);
	}

	private void showAddress(long pos_time) {
		String addr = getAddrFromGeo(SelfMapView.this, myLat, myLon);
		String time = DateUtils.formatDateTime(SelfMapView.this,
				pos_time, DateUtils.FORMAT_SHOW_TIME
						| DateUtils.FORMAT_SHOW_DATE);
		address = getResources().getString(R.string.mylocation_address)
				+ ": " + time + "\n" + addr;
		
		mHandler.post(new Runnable() {
			public void run() {
				AddrView.setText(address);
			}
		});
	}

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

	private void getphoto() {

		Bitmap bubbleblue = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.bubble_backgroundn);

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
					Drawable[] array = new Drawable[2];
					array[0] = new BitmapDrawable(bubbleblue);
					array[1] = new BitmapDrawable(resizedMyBitmap);
					LayerDrawable layers = new LayerDrawable(array);
					layers.setLayerInset(0, 0, 0, 0, 0);
					layers.setLayerInset(1, 8, 8, 8, 42);
	
					myMarker = new MapItems(layers, this, mapView, 0);
					array[0] = null;
					array[1] = null;
				}catch(OutOfMemoryError e){}
			}
			myPhoto = null;
		} catch (Exception e) {
			Log.e("selfmap getphoto " + e.getMessage());
		}

		bubbleblue=null;
		System.gc();
		System.gc();
	}
}
