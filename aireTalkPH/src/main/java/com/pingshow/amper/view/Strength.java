package com.pingshow.amper.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.pingshow.amper.R;

public class Strength extends View {
	private final String namespace = "http://www.airetalk.com";
	private float mWidth,mDensity;
	private Handler mHandler=new Handler();
	private Context mContext;
	private int length=0;
	private Bitmap panpic;
	private Bitmap background;

    public Strength(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDensity=this.getResources().getDisplayMetrics().density;
        panpic = BitmapFactory.decodeResource(getResources(), R.drawable.strength);
        background = BitmapFactory.decodeResource(getResources(), R.drawable.strength_bg);
        mContext=context;
    }
    
    public void setPosition(float per) {
    	mWidth=this.getWidth();
    	length=(int)(133*per);
    	length=length/7*7;
    	length=(int)((4+length)*mDensity);
    	this.postInvalidate();
    }
    
    @Override
	protected void onDraw(Canvas canvas) {
    	canvas.drawBitmap(background, 0, 0, null);
    	Rect rc=new Rect(0,0,length,(int)(32*mDensity));
		canvas.drawBitmap(panpic, rc, rc, null);
	}
    
}