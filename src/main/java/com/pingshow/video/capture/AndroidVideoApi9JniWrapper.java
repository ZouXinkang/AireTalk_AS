
package com.pingshow.video.capture;

import java.util.List;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

import com.pingshow.airecenter.Log;
import com.pingshow.codec.MotionDetect;
import com.pingshow.video.AndroidVideoWindowImpl;
 
public class AndroidVideoApi9JniWrapper {
	
	static MotionDetect mMotionDetect;
	public static void setMotionDetect(MotionDetect md)
	{
		mMotionDetect=md;
	}
	
	static public int detectCameras(int[] indexes, int[] frontFacing, int[] orientation) {
		return AndroidVideoApi5JniWrapper.detectCameras(indexes, frontFacing, orientation);
	}
	
	static public int[] selectNearestResolutionAvailable(int cameraId, int requestedW, int requestedH) {
		Log.d("video.selectNearestResolutionAvailable 9: " + cameraId + ", " + requestedW + "x" + requestedH);
		return AndroidVideoApi5JniWrapper.selectNearestResolutionAvailableForCamera(cameraId, requestedW, requestedH);
	}
	
	private static int mCameraId;
	private static int width2, height2, prvwFormat2, widthSz2;
	public static Object startRecording(int cameraId, int width, int height, int fps, int rotation, final long nativePtr) {
		Log.e("video.startRecording9 (" + cameraId + ", " + width + ", " + height + ", " + fps + ", " + rotation + ", " + nativePtr + ")");
		width2 = width;
		height2 = height;
		try {
		Camera camera = Camera.open(cameraId);
		
		Parameters params = camera.getParameters();
		
		params.setPreviewSize(width, height);
		mCameraId = cameraId;
		
//		String focusmode = params.getFocusMode();
//		Log.e("tmlvc focusmode0=" + focusmode);
//		params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED); //tml suv
//		focusmode = params.getFocusMode();
//		Log.e("tmlvc focusmode1=" + focusmode);
		
		int[] chosenFps = findClosestEnclosingFpsRange(fps*1000, params.getSupportedPreviewFpsRange());
		params.setPreviewFpsRange(chosenFps[0], chosenFps[1]);
		
		camera.setParameters(params);
		int prvwFormat = ImageFormat.getBitsPerPixel(params.getPreviewFormat());
		prvwFormat2 = prvwFormat;
		int widthSz = (width * prvwFormat) / 8;
		widthSz2 = widthSz;
//		int bufferSize = (width * height * prvwFormat) / 8;
		int bufferSize = height * widthSz;
//		Log.e("video.WH=" + width2 + "x" + height2 + " prvwF=" + prvwFormat);  //12
		camera.addCallbackBuffer(new byte[bufferSize]);
		camera.addCallbackBuffer(new byte[bufferSize]);
		
		camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
			public void onPreviewFrame(byte[] data, Camera camera) {
				camera.cancelAutoFocus();
				// forward image data to JNI
				//tml|sw*** flicker
//				final int minLines = 3;
//				int y128 = 0;
//				for (int i = height2 - minLines; i < height2; i++) {
//					for (int j = 0; j < widthSz2; j++) {
//						if (data[i * widthSz2 + j] == 128) {
//							y128 = y128 + 1;
//						}
//					}
//				}
//				if (y128 == widthSz2 * minLines) {
//					Log.e("     FLICKER!!! FLICKER!!! FLICKER!!!");
//					return;
//				}
				//***tml
				if (nativePtr != 0) {
					AndroidVideoApi5JniWrapper.putImage(nativePtr, data);
				} else {
					if (mMotionDetect != null) {
						mMotionDetect.picture_detect(data, data.length); //tml suv
					}
				}
				camera.addCallbackBuffer(data);
			}
		});
		
		setCameraDisplayOrientation(rotation, cameraId, camera);
		
		camera.startPreview();
		Log.d("video.Returning camera object: " + camera);
		
		if (nativePtr!=0)
		{
			if (AndroidVideoWindowImpl.mVideoPreviewView!=null)//alec
				setPreviewDisplaySurface(camera,AndroidVideoWindowImpl.mVideoPreviewView);
		}
		
		return camera; 
		} catch (Exception exc) {
			Log.e("video.Error to startRecording9... startPreview error"+exc.getMessage());
			exc.printStackTrace();
			return null;
		}
	}
	
	public static void stopRecording(Object cam) {
//		Camera camera = (Camera) cam; //tml suv
//		Parameters params = camera.getParameters();
//		params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//		camera.setParameters(params);
		
		AndroidVideoApi8JniWrapper.stopRecording(cam);
		
		mMotionDetect = null;
	} 
	
	public static void setPreviewDisplaySurface(Object cam, Object surf) {
		Log.d("video.Api9 setPreviewDisplaySurface");
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
		
		Log.w("video.Camera preview orientation: "+ result);
		try {
			camera.setDisplayOrientation(result);
		} catch (Exception exc) {
			Log.e("video.Failed to execute: camera[" + camera + "].setDisplayOrientation(" + result + ")");
			exc.printStackTrace();
		}
	}
	
	private static int[] findClosestEnclosingFpsRange(int expectedFps, List<int[]> fpsRanges) {
		Log.d("video.Searching for closest fps range from " + expectedFps);
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
				Log.d("video.a better range has been found: w="+closestRange[0]+",h="+closestRange[1]);
			}
		}
		Log.d("video.The closest fps range is w="+closestRange[0]+",h="+closestRange[1]);
		return closestRange;
	}
}
