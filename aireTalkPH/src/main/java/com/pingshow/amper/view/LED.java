package com.pingshow.amper.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.pingshow.amper.R;

public class LED extends View {
	private float mDensity;
	private Bitmap [] number=new Bitmap[11];
	private Bitmap background;
	private int [] num=new int[3];

    public LED(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDensity=this.getResources().getDisplayMetrics().density;
        for (int i=0;i<11;i++)
        	number[i] = BitmapFactory.decodeResource(getResources(), R.drawable.seg0+i);
        background = BitmapFactory.decodeResource(getResources(), R.drawable.led);
        setNumber(0);
    }
    
    public void setNumber(int n) {
    	if (n<0)
    	{
    		num[0]=num[1]=num[2]=10;
    	}else{
	    	num[0]=(n/100)%10;
	    	num[1]=(n/10)%10;
	    	num[2]=n%10;
    	}
    	this.postInvalidate();
    }
    
    @Override
	protected void onDraw(Canvas canvas) {
    	canvas.drawBitmap(background, 0, 0, null);
    	canvas.drawBitmap(number[num[0]], 2.f*mDensity, 2.f, null);
		canvas.drawBitmap(number[num[1]], 21.f*mDensity, 2.f, null);
		canvas.drawBitmap(number[num[2]], 40.f*mDensity, 2.f, null);
	}
}