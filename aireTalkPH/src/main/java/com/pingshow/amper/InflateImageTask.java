package com.pingshow.amper;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore.Video;
import android.widget.ImageView;

import com.pingshow.util.ImageUtil;

public class InflateImageTask extends AsyncTask<Object, Object, Object> {
	private ImageView gView;
	private String iconurl;
	private int fileImageResId;
	private boolean forceStop=false;
	private Context mContext;
	public InflateImageTask(Context context, String url,int fileImageResId) {
		this.iconurl = url;
		this.fileImageResId = fileImageResId;
		this.mContext=context;
	}
	public void stop()
	{
		forceStop=true;
	}
	@Override
	protected void onPostExecute(Object result) {
		if (result != null) {
			if (!forceStop)
				this.gView.setImageBitmap((Bitmap) result);
			this.gView = null;
		}else{
			if (!forceStop)
				this.gView.setImageResource(fileImageResId);
		}
	}
	@Override
	protected Object doInBackground(Object... views) {
		if (forceStop) return null;
		Bitmap bmp = downImage(iconurl);
		this.gView = (ImageView) views[0];
		return bmp;
	}
	public Bitmap downImage(String ImageUrl){
        if (forceStop) return null;
        if (ImageUrl.endsWith(".mp4")||ImageUrl.endsWith(".MP4")||ImageUrl.endsWith(".3gp"))
        {
        	try {
        		if(Integer.parseInt(Build.VERSION.SDK) >= 8){
        			Bitmap bitmapOrg = ThumbnailUtils.createVideoThumbnail(new File(ImageUrl).getAbsolutePath(), Video.Thumbnails.MICRO_KIND);
        			Bitmap videobitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sm70);
        			Drawable[] array = new Drawable[2];
        			array[0] = new BitmapDrawable(bitmapOrg);
        			array[1] = new BitmapDrawable(videobitmap);
        			LayerDrawable layers= new LayerDrawable(array);
        			layers.setLayerInset(0, 0, 0, 0, 0);
        			layers.setLayerInset(1, 5, 25, 25, 5);
        			Bitmap bmp=ImageUtil.drawableToBitmap(layers);
        			return bmp;
        		}
        	}catch (OutOfMemoryError e2) {
    		}catch (Exception e1) {}
        }
        else if (ImageUrl.endsWith(".jpg") || ImageUrl.endsWith(".JPG") || ImageUrl.endsWith(".bmp") || ImageUrl.endsWith(".png"))
        {
        	try {
    			BitmapFactory.Options options = new BitmapFactory.Options();
    			options.inJustDecodeBounds=true;
    			if (forceStop) return null;
    			BitmapFactory.decodeFile(ImageUrl, options);
    			options.inJustDecodeBounds=false;
    			options.inSampleSize = options.outHeight/100;
    			if (forceStop) return null;
    			Bitmap bitmapOrg = BitmapFactory.decodeFile(ImageUrl, options);
    			return bitmapOrg;
    		}catch (OutOfMemoryError e2) {
    		}catch (Exception e1) {}
        }
		return null;
	}
}
