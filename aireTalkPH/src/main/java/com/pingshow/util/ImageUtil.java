package com.pingshow.util;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.pingshow.amper.Global;
import com.pingshow.amper.Log;

public class ImageUtil {

	public static Bitmap drawableToBitmap(Drawable drawable) {

		Bitmap bitmap = Bitmap
				.createBitmap(
						drawable.getIntrinsicWidth(),
						drawable.getIntrinsicHeight(),
						drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
								: Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	public static Bitmap toGrayscale(BitmapDrawable bm1) {
		int width, height;
		Bitmap bitmap = bm1.getBitmap();
		height = bitmap.getHeight();
		width = bitmap.getWidth();

		Bitmap bmpGrayscale = Bitmap.createBitmap(width, height,
				Bitmap.Config.RGB_565);
		Canvas c = new Canvas(bmpGrayscale);
		Paint paint = new Paint();
		ColorMatrix cm = new ColorMatrix();
		cm.setSaturation(0);
		ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
		paint.setColorFilter(f);
		c.drawBitmap(bitmap, 0, 0, paint);
		return bmpGrayscale;
	}

	public static BitmapDrawable toGrayscale(BitmapDrawable bm1, int pixels) {
		return new BitmapDrawable(toRoundCorner(toGrayscale(bm1), pixels));
	}

	public static Bitmap toRoundCorner(Bitmap bitmap, int pixels) {

		try {
			int w = bitmap.getWidth();
			int h = bitmap.getHeight();
			Bitmap output = Bitmap.createBitmap(w, h, Config.ARGB_8888);
			Canvas canvas = new Canvas(output);

			final int color = 0xff424242;
			final Paint paint = new Paint();
			final Rect rect = new Rect(0, 0, w, h);
			final RectF rectF = new RectF(rect);
			final float roundPx = pixels;

			paint.setAntiAlias(true);
			canvas.drawARGB(0, 0, 0, 0);
			paint.setColor(color);
			canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

			paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
			canvas.drawBitmap(bitmap, rect, rect, paint);

			return output;
		} catch (OutOfMemoryError e) {
			return bitmap;
		}
	}

	public static BitmapDrawable toRoundCorner(BitmapDrawable bitmapDrawable,
			int pixels) {
		Bitmap bitmap = bitmapDrawable.getBitmap();
		bitmapDrawable = new BitmapDrawable(toRoundCorner(bitmap, pixels));
		return bitmapDrawable;
	}

	public static BitmapDrawable toRoundCorner2(Bitmap bitmap, int pixels) {
		BitmapDrawable bitmapDrawable = new BitmapDrawable(toRoundCorner(
				bitmap, pixels));
		return bitmapDrawable;
	}

	public static Drawable loadBitmapSafe(String photoPath, int divid) {
		Drawable photo = null;
		try {
			Bitmap bitmapOrg = null;
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			bitmapOptions.inSampleSize = divid;
			bitmapOptions.inPurgeable = true;
			bitmapOrg = BitmapFactory.decodeFile(photoPath, bitmapOptions);
			if (bitmapOrg != null && bitmapOrg.getHeight() > 0)
				photo = new BitmapDrawable(bitmapOrg);
		} catch (Exception e) {
		} catch (OutOfMemoryError e) {
			System.gc();
			System.gc();
		}
		return photo;
	}

	public static Bitmap loadBitmapSafe(int divid, String photoPath) {
		Bitmap bitmapOrg = null;
		try {
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			bitmapOptions.inSampleSize = divid;
			bitmapOptions.inPurgeable = true;
			bitmapOrg = BitmapFactory.decodeFile(photoPath, bitmapOptions);
		} catch (Exception e) {
		} catch (OutOfMemoryError e) {
			System.gc();
			System.gc();
		}
		return bitmapOrg;
	}

	public static Bitmap loadBitmapSafeWithAdaptiveDivision(int divid,
			String photoPath, int screenW) {
		Bitmap bitmapOrg = null;
		try {
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			bitmapOptions.inPurgeable = true;
			bitmapOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(photoPath, bitmapOptions);
			//if (bitmapOptions.outHeight>640 || bitmapOptions.outWidth>640)
			//divid*=2;
			//tml*** hotnews
			double screenPortion = 0;
			int factor = 1;
			if (screenW <= 0) {
				if (bitmapOptions.outHeight > 640
						|| bitmapOptions.outWidth > 640)
					divid = divid * 4;
			} else {
				screenPortion = (double) bitmapOptions.outWidth
						/ (double) screenW;

				if (screenPortion >= 0.90) {
					while (screenPortion >= 0.90) {
						screenPortion = screenPortion / 2;
						factor = factor * 2;
					}
				} else {
					factor = 0;
				}

				divid = factor;
			}
			//***tml
			Log.d("convBmp divid=" + divid + " w=" + bitmapOptions.outWidth
					+ "/" + screenW + " p" + screenPortion);
			bitmapOptions = new BitmapFactory.Options();
			bitmapOptions.inSampleSize = divid;
			bitmapOptions.inPurgeable = true;
			bitmapOrg = BitmapFactory.decodeFile(photoPath, bitmapOptions);
			int bmpnewz = bitmapOrg.getWidth();
			Log.d("convBmp new=" + bmpnewz);
		} catch (Exception e) {
			Log.e("Bitmap resize !@#$ " + e.getMessage());
		} catch (OutOfMemoryError e) {
			System.gc();
			System.gc();
		}
		return bitmapOrg;
	}

	static public Drawable getBitmapAsRoundCorner(String path, int division,
			int radius) {
		Bitmap photo = ImageUtil.loadBitmapSafe(division, path);// alec
		if (photo != null)
			return ImageUtil.toRoundCorner2(photo, radius);
		return null;
	}

	static public Bitmap getBitmapAsRoundCorner(int division, int radius,
			String path) {
		Bitmap photo = ImageUtil.loadBitmapSafe(division, path);// alec
		if (photo != null)
			return ImageUtil.toRoundCorner(photo, radius);
		return null;
	}

	static public Bitmap getBitmapAsRoundCornerWithAdaptiveDivision(
			int division, int radius, String path, int screenW) {
		Bitmap photo = ImageUtil.loadBitmapSafeWithAdaptiveDivision(division,
				path, screenW);// alec
		if (photo != null)
			return ImageUtil.toRoundCorner(photo, radius);
		return null;
	}

	static public Bitmap makeFriendPhotoAsRoundCorner(String photoPath) {
		return null;
	}

	public static class FlushedInputStream extends FilterInputStream {
		public FlushedInputStream(InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public long skip(long n) throws IOException {
			long totalBytesSkipped = 0L;
			while (totalBytesSkipped < n) {
				long bytesSkipped = in.skip(n - totalBytesSkipped);
				if (bytesSkipped == 0L) {
					int b = read();
					if (b < 0) {
						break; // we reached EOF
					} else {
						bytesSkipped = 1; // we read one byte
					}
				}
				totalBytesSkipped += bytesSkipped;
			}
			return totalBytesSkipped;
		}
	}

	static public Drawable getBigRoundedUserPhoto(Context context, int idx) {
		Drawable photo = null;
		String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx
				+ "b.jpg";
		File f = new File(userphotoPath);
		if (!f.exists()) {
			userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
			f = new File(userphotoPath);
			if (f.exists())
				photo = ImageUtil.getBitmapAsRoundCorner(userphotoPath, 1, 5);
		} else
			photo = ImageUtil.getBitmapAsRoundCorner(userphotoPath, 2, 7);

		return photo;
	}

	static public Bitmap combineImages(Context context, Bitmap base,
			Bitmap top, boolean center) {
		Bitmap cs = null;

		int width, height = 0;

		if (base.getWidth() > top.getWidth()) {
			width = base.getWidth();
			height = base.getHeight();
		} else {
			width = top.getWidth();
			height = top.getHeight();
		}

		cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		int screenWidth = ((Activity) context).getWindowManager()
				.getDefaultDisplay().getWidth();
		Canvas comboImage = new Canvas(cs);
		Rect src = new Rect(0, 0, top.getWidth(), top.getHeight());
		Rect dst;
		if (center) {
			int s = (int) (120f / screenWidth * width);
			dst = new Rect((width - s) / 2, (height - s) / 2, (width - s) / 2
					+ s, (height - s) / 2 + s);
		} else {
			int s = (int) (60f / screenWidth * width);
			dst = new Rect(0, height - s, s, height);
		}

		comboImage.drawBitmap(base, 0f, 0f, null);
		comboImage.drawBitmap(top, src, dst, null);

		return cs;
	}
	//xwf*** circle pic
	/**
	 * @param bitmap input source bitmap
	 * @return output circle bitmap
	 */
	public static Bitmap toRoundBitmap(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		float roundPx;
		float left, top, right, bottom, dst_left, dst_top, dst_right, dst_bottom;
		if (width <= height) {
			roundPx = width / 2;
			top = 0;
			bottom = width;
			left = 0;
			right = width;
			height = width;
			dst_left = 0;
			dst_top = 0;
			dst_right = width;
			dst_bottom = width;
		} else {
			roundPx = height / 2;
			float clip = (width - height) / 2;
			left = clip;
			right = width - clip;
			top = 0;
			bottom = height;
			width = height;
			dst_left = 0;
			dst_top = 0;
			dst_right = height;
			dst_bottom = height;
		}
		
		Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		final int color1 = 0xffffffff;
		final Paint paint = new Paint();
		final Rect src = new Rect((int) left, (int) top, (int) right,
				(int) bottom);
		final Rect dst = new Rect((int) dst_left, (int) dst_top,
				(int) dst_right, (int) dst_bottom);
		final RectF rectF = new RectF(dst);
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color1);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, src, dst, paint);
		
		return output;
	}
	
	public static Bitmap getCircleBitmapPath(String path, int division, int radius) {		
		Bitmap photo = ImageUtil.loadBitmapSafe(division, path);
		if (photo != null) {
//			return toRoundBitmap(toRoundCorner(photo, radius));
			return toRoundBitmap(photo);
		}
		return null;
	}
	
	public static Bitmap getCircleBitmapResouce(Resources resources, int id) {
		return toRoundBitmap(BitmapFactory.decodeResource(resources, id));
	}
	//***xwf

	/**
	 *  处理图片
	 * @param bm 所要转换的bitmap
	 * @param newWidth 新的宽
	 * @param newHeight 新的高
	 * @return 指定宽高的bitmap
	 */
	public static Bitmap zoomImg(Bitmap bm, int newWidth ,int newHeight){
		// 获得图片的宽高
		int width = bm.getWidth();
		int height = bm.getHeight();
		// 计算缩放比例
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// 取得想要缩放的matrix参数
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		// 得到新的图片   www.2cto.com
		Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
		return newbm;
	}
}