
package com.pingshow.video.capture.hwconf;

import java.util.ArrayList;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Size;

import com.pingshow.airecenter.Log;
import com.pingshow.video.capture.hwconf.AndroidCameraConfiguration.AndroidCamera;

class AndroidCameraConfigurationReader5  {
	static public AndroidCamera[] probeCameras() {
		List<AndroidCamera> cam = new ArrayList<AndroidCamera>(1);
		
		Camera camera = Camera.open();
		List<Size> r = camera.getParameters().getSupportedPreviewSizes();
		camera.release();
		
		// Defaults
		if (Hacks.isGalaxySOrTab()) {
			Log.d("Hack Galaxy S : has one or more cameras");
			if (Hacks.isGalaxySOrTabWithFrontCamera()) {
				Log.d("Hack Galaxy S : HAS a front camera with id=2");
				cam.add(new AndroidCamera(2, true, 90, r));
			} else {
				Log.d("Hack Galaxy S : NO front camera");
			}
			Log.d("Hack Galaxy S : HAS a rear camera with id=1");
			cam.add(new AndroidCamera(1, false, 90, r));
		} else {
			cam.add(new AndroidCamera(0, false, 90, r));

			if (Hacks.hasTwoCamerasRear0Front1()) {
				Log.d("Hack SPHD700 has 2 cameras a rear with id=0 and a front with id=1");
				cam.add(new AndroidCamera(1, true, 90, r));
			}
		}
		
		AndroidCamera[] result = new AndroidCamera[cam.size()];
		result = cam.toArray(result);
		return result;
		
	}
}
