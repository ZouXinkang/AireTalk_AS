package com.pingshow.amper.map;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;


@SuppressWarnings("rawtypes")
public class MapItems extends ItemizedOverlay{

	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	public MapItems(Drawable defaultMarker, Context context1, MapView mapView1,int type) 
	{  	
		super(type==0?boundCenterBottom(defaultMarker):boundCenter(defaultMarker));
	}
	
	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay); 
		populate();
	}
	
	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}
	
	public void removeAll() {
		mOverlays.clear();
	}
	
	public void removeHead(){
		mOverlays.remove(0);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}
}
