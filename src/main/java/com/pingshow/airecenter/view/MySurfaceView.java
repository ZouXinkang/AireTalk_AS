package com.pingshow.airecenter.view;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import com.pingshow.airecenter.Log;
import com.pingshow.video.capture.hwconf.AndroidCameraConfiguration;

public class MySurfaceView extends SurfaceView implements Callback, Camera.PreviewCallback
{
    private SurfaceHolder mHolder;

    private Camera mCamera;
    private boolean isPreviewRunning = false;
    
    private int _width=640;
    private int _height=480;

    public MySurfaceView(Context context, AttributeSet attrs) {
    	super(context, attrs);
       	mHolder = super.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    @Override
    public SurfaceHolder getHolder() {
    	return mHolder;
    }
    public void setVideoSize(int width, int height)
    {
    	_width=width;
    	_height=height;
    }

    @Override
    protected void onDraw(Canvas canvas) {
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
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

    public Camera getCamera(){
    	return mCamera;
    }
    
    public void surfaceCreated(SurfaceHolder holder) {
    	Log.e("surfaceCreated");
    	mHolder=holder;
        synchronized (this) {
        	
        	int cameraId=1;
			if(AndroidCameraConfiguration.retrieveCameras().length>0)
				cameraId=cameraId%AndroidCameraConfiguration.retrieveCameras().length;
			else
				cameraId=0;
			
            mCamera = Camera.open(cameraId);
            Camera.Parameters p = mCamera.getParameters();
//            p.setPreviewSize(_width, _height);  //sw*** autofocus
            
            int[] chosenFps = findClosestEnclosingFpsRange(1000, p.getSupportedPreviewFpsRange());
    		p.setPreviewFpsRange(chosenFps[0], chosenFps[1]);
    		
            mCamera.setParameters(p);
            try {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();
			} catch (IOException e) {
				e.printStackTrace();
			}
            
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (this) {
            try {
                if (mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.reconnect();
                    isPreviewRunning = false;
                    mCamera.release();
                    mCamera=null;
                }
            } catch (Exception e) {
                Log.e(e.getMessage());
            }
        }
    }
    
    public void onPreviewFrame(byte[] data, Camera camera) {
        if(mHolder == null) return;
    	try {
            synchronized (mHolder) {
                Log.d("onPreviewFrame...");
            }
        } finally {
        }
	}
}