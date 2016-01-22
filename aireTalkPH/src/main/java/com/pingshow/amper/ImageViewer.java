package com.pingshow.amper;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

public class ImageViewer extends Activity {
	
	private FullScreenImageAdapter adapter;
	static ImageViewer instance;
	
	static ImageViewer getInstance()
	{
		return instance;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_viewer_zoomable);
		this.overridePendingTransition(R.anim.freeze, R.anim.freeze);

		ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
		
		try{
			ArrayList <String> images=getIntent().getExtras().getStringArrayList("images");  
			adapter = new FullScreenImageAdapter(this, images);
		}catch(Exception e)
		{
			finish();
			return;
		}
		viewPager.setAdapter(adapter);
		viewPager.setCurrentItem(0);
		
		instance=this;
	}
	
	@Override
	protected void onDestroy()
	{
		instance=null;
		super.onDestroy();
	}
	
}
