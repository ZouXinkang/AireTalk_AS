
package com.pingshow.video.capture.hwconf;

import java.util.ArrayList;
import java.util.List;

import com.pingshow.video.capture.hwconf.AndroidCameraConfiguration.AndroidCamera;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;


class AndroidCameraConfigurationReader9 {
	static public AndroidCamera[] probeCameras() {
		List<AndroidCamera> cam = new ArrayList<AndroidCamera>(Camera.getNumberOfCameras());
		
		for(int i=0; i<Camera.getNumberOfCameras(); i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			Camera c = Camera.open(i);
			cam.add(new AndroidCamera(i, info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT, info.orientation, c.getParameters().getSupportedPreviewSizes()));
			c.release();
		}
		
		AndroidCamera[] result = new AndroidCamera[cam.size()];
		result = cam.toArray(result);
		return result;
	}
}
