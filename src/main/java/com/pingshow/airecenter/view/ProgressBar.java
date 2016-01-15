package com.pingshow.airecenter.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.pingshow.airecenter.R;

public class ProgressBar extends View {
	private float mDensity;
	private int length=0;
	private Bitmap barpic;
	private Bitmap background;
	private int width;

    public ProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDensity=this.getResources().getDisplayMetrics().density;
        barpic = BitmapFactory.decodeResource(getResources(), R.drawable.progress);
        background = BitmapFactory.decodeResource(getResources(), R.drawable.progress_bg);
        width=background.getWidth();
    }
    
    public void setImage(int progress, int progress_bg) {
    	barpic = BitmapFactory.decodeResource(getResources(), progress);
        background = BitmapFactory.decodeResource(getResources(), progress_bg);
        width=background.getWidth();
    }
    
    public void setProgress(float per) {
    	if (per>1) per=1;
    	else if (per<0) per=0;
    	length=(int)(per*width);
    	this.postInvalidate();
    }
    
    @SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
    	canvas.drawBitmap(background, 0, 0, null);
    	Rect rc=new Rect(0,0,length,(int)(15*mDensity));
    	Rect rc2=new Rect(0,0,length, background.getHeight());
		canvas.drawBitmap(barpic, rc, rc2, null);
	}
}