package com.pingshow.amper;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class FullScreenImageAdapter extends PagerAdapter {
	 
    private Activity _activity;
    private ArrayList<String> _imagePaths;
    private LayoutInflater inflater;
 
    // constructor
    public FullScreenImageAdapter(Activity activity,
            ArrayList<String> imagePaths) {
        this._activity = activity;
        this._imagePaths = imagePaths;
    }
 
    @Override
    public int getCount() {
        return this._imagePaths.size();
    }
 
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
    }
     
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        ImageView imgDisplay;
  
        inflater = (LayoutInflater) _activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = inflater.inflate(R.layout.image_viewer, container, false);
  
        imgDisplay = (ImageView) viewLayout.findViewById(R.id.imgDisplay);
        
        String filepath=_imagePaths.get(position);
        BitmapFactory.Options options = new BitmapFactory.Options();
        //options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(filepath, options);
        
        if (options.outHeight>2400 || options.outWidth>2400)
        	options.inSampleSize=4;
        else if (options.outHeight>1200 || options.outWidth>1200)
        	options.inSampleSize=2;
        options.inJustDecodeBounds=false;
        
        try{
        	Bitmap bitmap = BitmapFactory.decodeFile(filepath, options);
        	imgDisplay.setImageBitmap(bitmap);
        }catch(Exception e){}
        catch(Error e){}
        
        ((ViewPager) container).addView(viewLayout);
  
        return viewLayout;
    }
     
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((RelativeLayout) object);
    }
}