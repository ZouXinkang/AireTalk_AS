package com.pingshow.amper.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.pingshow.amper.R;
import com.pingshow.codec.Volume;

public class Tuner extends View{
	
	private Bitmap panpic;
	private float mDensity;
	private Matrix panRotate=new Matrix();
	private float thita=0;
	private float thita2=0;
	private float O;
	private float mx=0;
	private float my=0;

	public Tuner(Context context, AttributeSet attrs) {
		super(context, attrs);
		mDensity=this.getResources().getDisplayMetrics().density;
		panpic = BitmapFactory.decodeResource(getResources(), R.drawable.tuner_0);
		O=35*mDensity;
	}
	
	public void setVolume(float v)
	{
		thita=v*120;
		this.postInvalidate();
	}
	
	public float getVolume()
	{
		return thita/120;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		panRotate.setRotate(thita, O, O);
		canvas.drawBitmap(panpic, panRotate, null);
	}
	
	private float getAngle(float x, float y)
	{
		float t=(float)(57.29578*Math.atan((y-O)/(x-O)));
		if (x<O)
    		return t-180;
    	return t;
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                thita2=getAngle(event.getX(),event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
            	float t=getAngle(event.getX(),event.getY());
            	if (Math.abs(t-thita2)>60) return true; 
            	thita+=(t-thita2)/1.3;
            	thita2=t;
            	if (thita>224) thita=224;
            	else if (thita<0) thita=0;
            	this.postInvalidate();
            	Volume.setVolume(thita/175);
                break;
        }

        return true;
    }
}
