package com.pingshow.amper.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class HistogramView extends View {

	private Path mPath;
	private Paint mPaint;
	private Paint mPaintGrid;
	private Path mPathGrid;
	private float bottom=0;
	private float xScale, yScale;
	 
	public HistogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPath = new Path();
        mPathGrid = new Path();
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(1);
        mPaint.setColor(0xc000ff00);
        
        mPaintGrid.setStyle(Paint.Style.STROKE);
        mPaintGrid.setStrokeWidth(0.4f);
        mPaintGrid.setColor(0xffffff00);
    }
	
	public void feedData(byte[]src)
	{
		if (xScale==0||bottom==0)
        {
        	xScale=getWidth()/360f;
        	yScale=getHeight()/40f;
        	bottom=getHeight();
        	
        	mPathGrid.moveTo(0, bottom-10);
            mPathGrid.lineTo(360, bottom-10);
            
            mPathGrid.moveTo(0, bottom-20);
            mPathGrid.lineTo(360, bottom-20);
        }
		
		mPath.reset();
		mPath.moveTo(0, bottom);
        for (int i=0;i<360;i++)
		{
        	mPath.lineTo(i, bottom-(float)src[i]);
        }
        mPath.lineTo(360, bottom);
        mPath.close();
        
        invalidate();
	}
	
	@Override 
	protected void onDraw(Canvas canvas) {
		canvas.save();
		canvas.scale(xScale, yScale, 0, bottom);
        canvas.drawPath(mPath, mPaint);
        canvas.drawPath(mPathGrid, mPaintGrid);
        canvas.restore();
    }
}
