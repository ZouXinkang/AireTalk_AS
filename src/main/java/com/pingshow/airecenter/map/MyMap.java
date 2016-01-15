package com.pingshow.airecenter.map;

import android.content.Context;
import android.graphics.Canvas;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class MyMap extends MyLocationOverlay{
	
	GeoPoint geopiont;
	Location lastFix;
	MapView mapView1;
	Location lastFix1;
	GeoPoint myLocation;
	long when1;
	
	public MyMap(Context context, MapView mapView, Location lastFix, GeoPoint geopiont) {
		super(context, mapView);
		this.geopiont=geopiont;
		this.lastFix1=lastFix;
	}
	
	@Override
	protected void drawMyLocation(Canvas canvas, MapView mapView,
		Location lastFix, GeoPoint myLocation, long when) {
		super.drawMyLocation(canvas, mapView, lastFix1, geopiont, when);
	}
	@Override
	public GeoPoint getMyLocation() {
		return geopiont;
	}
	
	public void setMyLocation(GeoPoint geopiont){
		this.geopiont=geopiont;
	}
	public void setLastFix(Location loc){
		lastFix1=loc;
	}
}
