package com.pingshow.amper.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.pingshow.amper.R;

public class ProgressBar extends View {
	private float mDensity;
	private int length=0;
	private Bitmap barpic;
	private Bitmap background;

    public ProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDensity=this.getResources().getDisplayMetrics().density;
        barpic = BitmapFactory.decodeResource(getResources(), R.drawable.progress);
        background = BitmapFactory.decodeResource(getResources(), R.drawable.progress_bg);
    }
    
    public void setProgress(float per) {
    	length=(int)(per*background.getWidth());
    	this.postInvalidate();
    }
    
    @SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
    	canvas.drawBitmap(background, 0, 0, null);
    	Rect rc=new Rect(0,0,length,(int)(15*mDensity));
		canvas.drawBitmap(barpic, rc, rc, null);
	}
}