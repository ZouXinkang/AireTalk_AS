
package com.pingshow.video.capture;

import java.util.List;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

import com.pingshow.amper.Log;
import com.pingshow.video.AndroidVideoWindowImpl;
 
public class AndroidVideoApi9JniWrapper {	
	static public int detectCameras(int[] indexes, int[] frontFacing, int[] orientation) {
		return AndroidVideoApi5JniWrapper.detectCameras(indexes, frontFacing, orientation);
	}
	
	static public int[] selectNearestResolutionAvailable(int cameraId, int requestedW, int requestedH) {
		Log.d("selectNearestResolutionAvailable 9: " + cameraId + ", " + requestedW + "x" + requestedH);
		return AndroidVideoApi5JniWrapper.selectNearestResolutionAvailableForCamera(cameraId, requestedW, requestedH);
	}
	
	public static Object startRecording(int cameraId, int width, int height, int fps, int rotation, final long nativePtr) {
		Log.d("startRecording9 (" + cameraId + ", " + width + ", " + height + ", " + fps + ", " + rotation + ", " + nativePtr + ")");
		try {
		Camera camera = Camera.open(cameraId);
		
		Parameters params = camera.getParameters();
		
		params.setPreviewSize(width, height);
		
		int[] chosenFps = findClosestEnclosingFpsRange(fps*1000, params.getSupportedPreviewFpsRange());
		params.setPreviewFpsRange(chosenFps[0], chosenFps[1]);
		
		camera.setParameters(params);
		
		int bufferSize = (width * height * ImageFormat.getBitsPerPixel(params.getPreviewFormat())) / 8;
		if (bufferSize < 204750) {  //tml*** relay vid buffer
			bufferSize = bufferSize + 4095;
		}
		Log.d("startRecording9 bufferSize w*h*bpp*1.05=" + bufferSize);
		camera.addCallbackBuffer(new byte[bufferSize]);
		camera.addCallbackBuffer(new byte[bufferSize]);
		
		camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
			public void onPreviewFrame(byte[] data, Camera camera) {
				// forward image data to JNI
				AndroidVideoApi5JniWrapper.putImage(nativePtr, data);
				
				camera.addCallbackBuffer(data);
			}
		});
		
		setCameraDisplayOrientation(rotation, cameraId, camera);
		 
		camera.startPreview();
		Log.d("Returning camera object: " + camera);
		
		//alec
		if (AndroidVideoWindowImpl.mVideoPreviewView!=null)
			setPreviewDisplaySurface(camera,AndroidVideoWindowImpl.mVideoPreviewView);
		
		return camera; 
		} catch (Exception exc) {
			Log.e("Error to startRecording9... startPreview error"+exc.getMessage());
			exc.printStackTrace();
			return null;
		}
	}
	
	public static void stopRecording(Object cam) {
		AndroidVideoApi8JniWrapper.stopRecording(cam);
	} 
	
	public static void setPreviewDisplaySurface(Object cam, Object surf) {
		Log.d("Api9 setPreviewDisplaySurface");
		AndroidVideoApi5JniWrapper.setPreviewDisplaySurface(cam, surf);
	}
	
	private static void setCameraDisplayOrientation(int rotationDegrees, int cameraId, Camera camera) {
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + rotationDegrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - rotationDegrees + 360) % 360;
		}
		
		Log.w("Camera preview orientation: "+ result);
		try {
			camera.setDisplayOrientation(result);
		} catch (Exception exc) {
			Log.e("Failed to execute: camera[" + camera + "].setDisplayOrientation(" + result + ")");
			exc.printStackTrace();
		}
	}
	
	private static int[] findClosestEnclosingFpsRange(int expectedFps, List<int[]> fpsRanges) {
		Log.d("Searching for closest fps range from " + expectedFps);
		// init with first element
		int[] closestRange = fpsRanges.get(0);
		int measure = Math.abs(closestRange[0] - expectedFps)
				+ Math.abs(closestRange[1] - expectedFps);
		for (int[] curRange : fpsRanges) {
			if (curRange[0] > expectedFps || curRange[1] < expectedFps) continue;
			int curMeasure = Math.abs(curRange[0] - expectedFps)
					+ Math.abs(curRange[1] - expectedFps);
			if (curMeasure < measure) {
				closestRange=curRange;
				measure = curMeasure;
				Log.d("a better range has been found: w="+closestRange[0]+",h="+closestRange[1]);
			}
		}
		Log.d("The closest fps range is w="+closestRange[0]+",h="+closestRange[1]);
		return closestRange;
	}
}
