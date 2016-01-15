package com.pingshow.airecenter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

public class InflateImageTask extends AsyncTask<Object, Object, Object> {
	private ImageView gView;
	private String iconurl;
	private int fileImageResId;
	public InflateImageTask(String url,int fileImageResId) {
		this.iconurl = url;
		this.fileImageResId = fileImageResId;
	}

	@Override
	protected void onPostExecute(Object result) {
		if (result != null) {
			this.gView.setImageBitmap((Bitmap) result);
			this.gView = null;
		}else{
			this.gView.setImageResource(fileImageResId);
		}
	}
	@Override
	protected Object doInBackground(Object... views) {
		Bitmap bmp = downImage(iconurl);
		this.gView = (ImageView) views[0];
		return bmp;
	}
	public Bitmap downImage(String ImageUrl){
        Bitmap bitmapOrg = null;
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds=true;
			BitmapFactory.decodeFile(ImageUrl, options);
			options.inJustDecodeBounds=false;
			options.inSampleSize = options.outHeight/100;
			bitmapOrg = BitmapFactory.decodeFile(ImageUrl, options);
			return bitmapOrg;
		}catch (OutOfMemoryError e2) {
		}catch (Exception e1) {}
		return null;
	}
}
