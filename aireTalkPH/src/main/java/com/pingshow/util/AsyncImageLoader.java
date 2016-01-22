package com.pingshow.util;

import java.lang.ref.SoftReference;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

public class AsyncImageLoader {

	private HashMap<String, SoftReference<Drawable>> imageCache;
	private HashMap<String, SoftReference<Bitmap>> BitmapCache;
	private Context _context;

	public AsyncImageLoader(Context context) {
		imageCache = new HashMap<String, SoftReference<Drawable>>();
		BitmapCache = new HashMap<String, SoftReference<Bitmap>>();
		_context = context;
	}

	public Drawable loadDrawable(final String path,
			final ImageCallback imageCallback) {
		if (imageCache.containsKey(path)) {
			SoftReference<Drawable> softReference = imageCache.get(path);
			Drawable bitmap = softReference.get();
			if (bitmap != null) {
				return bitmap;
			}
		}
		final Handler handler = new Handler() {
			public void handleMessage(Message message) {
				imageCallback.imageLoaded((Drawable) message.obj, path);
			}
		};
		new Thread() {
			@Override
			public void run() {
				Drawable drawable = loadBitmapFromPath(path);
				imageCache.put(path, new SoftReference<Drawable>(drawable));
				Message message = handler.obtainMessage(0, drawable);
				handler.sendMessage(message);
			}
		}.start();
		return null;
	}
	
	public Bitmap loadBitmap(final String path, final ImageBitmapCallback imageCallback) {
		if (BitmapCache.containsKey(path)) {
			SoftReference<Bitmap> softReference = BitmapCache.get(path);
			Bitmap bitmap = softReference.get();
			if (bitmap != null) {
				return bitmap;
			}
		}
		final Handler handler = new Handler() {
			public void handleMessage(Message message) {
				imageCallback.imageBitmapLoaded((Bitmap) message.obj, path);
			}
		};
		new Thread() {
			@Override
			public void run() {
				Bitmap bmp = ImageUtil.loadBitmapSafeWithAdaptiveDivision(1, path, 0);
				BitmapCache.put(path, new SoftReference<Bitmap>(bmp));
				Message message = handler.obtainMessage(0, bmp);
				handler.sendMessage(message);
			}
		}.start();
		return null;
	}
	
	public Drawable loadBitmapFromPath(String path)
	{
		if (path==null) return null;
		int division=1;
		if (path.endsWith("b.jpg"))
			division=3;
		return ImageUtil.getBitmapAsRoundCorner(path,division,5);
	}
	
	public Bitmap loadImageFromRes(int id) {
		Bitmap friendPhoto;
		
		friendPhoto = ImageUtil.toRoundCorner(
				ImageUtil.drawableToBitmap(_context.getResources().getDrawable(
						id)), 8);
		return friendPhoto;
	}

	public interface ImageCallback {
		public void imageLoaded(Drawable imageDrawable, String path);
	}
	
	public interface ImageBitmapCallback {
		public void imageBitmapLoaded(Bitmap imageBitmap, String path);
	}

	public void release()
	{
		if (imageCache!=null) imageCache.clear();
		imageCache=null;
		if (BitmapCache!=null) BitmapCache.clear();
		BitmapCache=null;
	}
}
