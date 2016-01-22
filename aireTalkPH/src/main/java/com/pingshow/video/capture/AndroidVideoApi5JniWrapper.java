
package com.pingshow.video.capture;

import java.util.List;

import com.pingshow.video.AndroidVideoWindowImpl;
import com.pingshow.video.capture.hwconf.AndroidCameraConfiguration;
import com.pingshow.video.capture.hwconf.AndroidCameraConfiguration.AndroidCamera;
import com.pingshow.voip.core.Version;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import com.pingshow.amper.Log;
import android.view.SurfaceView;
 

public class AndroidVideoApi5JniWrapper {
	
	public static native void setAndroidSdkVersion(int version);
	public static native void putImage(long nativePtr, byte[] buffer);
	
	static public int detectCameras(int[] indexes, int[] frontFacing, int[] orientation) {
		Log.d("detectCameras\n");
		AndroidCamera[] cameras = AndroidCameraConfiguration.retrieveCameras();
		
		int nextIndex = 0;
		for (AndroidCamera androidCamera : cameras) {
			if (nextIndex == 2) {
				Log.w("Returning only the 2 first cameras (increase buffer size to retrieve all)");
				break;
			}
			// skip already added cameras
			indexes[nextIndex] = androidCamera.id;
			frontFacing[nextIndex] = androidCamera.frontFacing?1:0;
			orientation[nextIndex] = androidCamera.orientation;
			nextIndex++;
		}
		return cameras.length;
	}
	
	/**
	 * Return the hw-available available resolution best matching the requested one.
	 * Best matching meaning :
	 * - try to find the same one
	 * - try to find one just a little bigger (ex: CIF when asked QVGA)
	 * - as a fallback the nearest smaller one
	 * @param requestedW Requested video size width
	 * @param requestedH Requested video size height
	 * @return int[width, height] of the chosen resolution, may be null if no
	 * resolution can possibly match the requested one
	 */
	static public int[] selectNearestResolutionAvailable(int cameraId, int requestedW, int requestedH) {
		Log.d("selectNearestResolutionAvailable 5: " + cameraId + ", " + requestedW + "x" + requestedH);
		
		return selectNearestResolutionAvailableForCamera(cameraId, requestedW, requestedH);
	}
	
	static public void activateAutoFocus(Object cam) {
		Log.d("Turning on autofocus on camera " + cam);
		Camera camera = (Camera) cam;
		if (camera != null && (camera.getParameters().getFocusMode() == Parameters.FOCUS_MODE_AUTO || camera.getParameters().getFocusMode() == Parameters.FOCUS_MODE_MACRO))
			camera.autoFocus(null); // We don't need to do anything after the focus finished, so we don't need a callback
	}
	
	public static Object startRecording(int cameraId, int width, int height, int fps, int rotation, final long nativePtr) {
		Log.d("startRecording5 (" + cameraId + ", " + width + ", " + height + ", " + fps + ", " + rotation + ", " + nativePtr + ")");
		Camera camera = Camera.open(); 
		
		applyCameraParameters(camera, width, height, rotation, fps);
		  
		camera.setPreviewCallback(new Camera.PreviewCallback() {
			public void onPreviewFrame(byte[] data, Camera camera) {
				// forward image data to JNI
				putImage(nativePtr, data);
			}
		});
		
		camera.startPreview();
		
		if (AndroidVideoWindowImpl.mVideoPreviewView!=null)//alec
			setPreviewDisplaySurface(camera,AndroidVideoWindowImpl.mVideoPreviewView);
		
		Log.d("Returning camera object: " + camera);
		return camera; 
	} 
	
	public static void stopRecording(Object cam) {
		Log.d("stopRecording(" + cam + ")"); 
		Camera camera = (Camera) cam;
		 
		if (camera != null) {
			camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.release(); 
		} else {
			Log.i("Cannot stop recording ('camera' is null)");
		}
	} 
	
	public static void setPreviewDisplaySurface(Object cam, Object surf) {
		//Log.d("setPreviewDisplaySurface(" + cam + ", " + surf + ")");
		try {
			Camera camera = (Camera) cam;
			SurfaceView surface = (SurfaceView) surf;
			camera.setPreviewDisplay(surface.getHolder());
		} catch (Exception exc) {
			exc.printStackTrace(); 
		} catch (IncompatibleClassChangeError e)
		{
			
		}
	}
	
	protected static int[] selectNearestResolutionAvailableForCamera(int id, int requestedW, int requestedH) {
		// inversing resolution since webcams only support landscape ones
		if (requestedH > requestedW) {
			int t = requestedH;
			requestedH = requestedW;
			requestedW = t;
		}
				
		AndroidCamera[] cameras = AndroidCameraConfiguration.retrieveCameras();
		List<Size> supportedSizes = null;
		for(AndroidCamera c: cameras) {
			if (c.id == id)
				supportedSizes = c.resolutions;
		}
		if (supportedSizes == null) {
			Log.e("Failed to retrieve supported resolutions.");
			return null;
		}
		/*Log.d(supportedSizes.size() + " supported resolutions :");
		for(Size s : supportedSizes) {
			Log.d("\t" + s.width + "x" + s.height);
		}*/
		int r[] = null;
		
		int rW = Math.max(requestedW, requestedH);
		int rH = Math.min(requestedW, requestedH);
		
		try { 
			// look for nearest size
			Size result = null;
			int req = rW * rH;
			int minDist = Integer.MAX_VALUE;
			int useDownscale = 0;
			for(Size s: supportedSizes) {
				int dist = Math.abs(req - s.width * s.height);
				if (dist < minDist) {
					minDist = dist;
					result = s;
					useDownscale = 0;
				}
				
				/* MS2 has a NEON downscaler, so we test this too */
				int downScaleDist = Math.abs(req - s.width * s.height / 4);
				if (downScaleDist < minDist) {
					minDist = downScaleDist;
					result = s;
					useDownscale = 1;
				}
				if (s.width == rW && s.height == rH) {
					result = s;
					useDownscale = 0;
					break;
				}
			}
			r = new int[] {result.width, result.height, useDownscale};
			Log.d("resolution selection done (" + r[0] + ", " + r[1] + ", " + r[2] + ")");
			return r;
		} catch (Exception exc) {
			Log.e("resolution selection failed");
			exc.printStackTrace();
			return null;
		}	
	}
	
	protected static void applyCameraParameters(Camera camera, int width, int height, int rotation, int requestedFps) {
		Parameters params = camera.getParameters();
		 
		params.setPreviewSize(width, height);
		
		if (Version.sdkAboveOrEqual(8))
		{
			try{
				if (rotation==0)
					camera.setDisplayOrientation(90);
		
				List<Integer> supported = params.getSupportedPreviewFrameRates();
				if (supported != null) {
					int nearest = Integer.MAX_VALUE;
					for(Integer fr: supported) {
						int diff = Math.abs(fr.intValue() - requestedFps);
						if (diff < nearest) {
							nearest = diff;
							params.setPreviewFrameRate(fr.intValue());
						}
					}
					Log.d("Preview framerate set:" + params.getPreviewFrameRate());
				}
			}catch(Exception e){}
		}
		
		camera.setParameters(params);
	}
}
