
package com.pingshow.video.capture.hwconf;

import java.util.List;

import android.hardware.Camera.Size;
import android.os.Build;

import com.pingshow.airecenter.Log;
import com.pingshow.voip.core.Version;


public class AndroidCameraConfiguration {
	public static AndroidCamera[] retrieveCameras() {
		initCamerasCache();
		return camerasCache;
	}
	
	public static boolean hasSeveralCameras() {
		initCamerasCache();
		return camerasCache.length > 1;
	}

	public static boolean hasFrontCamera() {
		initCamerasCache();
		for (AndroidCamera cam:  camerasCache) {
			if (cam.frontFacing) 
				return true;
		}
		return false;
	}
	
	private static AndroidCameraConfiguration.AndroidCamera[] camerasCache;
	
	private static void initCamerasCache() {
		// cache already filled ?
		if (camerasCache != null)
			return;
		
		try {
			Log.d("Version "+Build.VERSION.SDK);
			if (Version.sdk() < 9)
				camerasCache = AndroidCameraConfiguration.probeCamerasSDK5();
			else
				camerasCache = AndroidCameraConfiguration.probeCamerasSDK9();
		} catch (Exception exc) {
			Log.e("Error: cannot retrieve cameras information (busy ?)"+exc);
			exc.printStackTrace();
			camerasCache = new AndroidCamera[0];
		}
	}
	
	static AndroidCamera[] probeCamerasSDK5() {
		return AndroidCameraConfigurationReader5.probeCameras();
	}
	
	static  AndroidCamera[] probeCamerasSDK9() {
		return AndroidCameraConfigurationReader9.probeCameras();
	}

	/**
	 * Default: no front; rear=0; default=rear
	 * @author Guillaume Beraudo
	 *
	 */
	static public  class AndroidCamera {
		public AndroidCamera(int i, boolean f, int o, List<Size> r) {
			this.id = i;
			this.frontFacing = f;
			this.orientation = o;
			this.resolutions = r;
		}
		public int id;
		public boolean frontFacing; // false => rear facing
		public int orientation;
		public List<Size> resolutions;
	}
}