package com.pingshow.airecenter.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.pingshow.airecenter.R;

public class AudioMsgPlayer extends View {
	private float mDensity;
	private boolean playing=false;
	private Bitmap start;
	private Bitmap stop;
	private Bitmap audmsg;
	private int duration;
	private int pos;
	private Paint mPaint;
	private Handler mHandler=new Handler();

    public AudioMsgPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDensity=this.getResources().getDisplayMetrics().density;
        start = BitmapFactory.decodeResource(getResources(), R.drawable.start_playing);
        stop = BitmapFactory.decodeResource(getResources(), R.drawable.stop_playing);
        audmsg = BitmapFactory.decodeResource(getResources(), R.drawable.audio_msg);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(0xff202020);
        mPaint.setTextSize(28*mDensity);
        //mPaint.setTextAlign(Paint.Align.CENTER);
    }
    
    public void setDuration(int duration) {
    	this.duration=duration;
    	this.pos=duration;
    	this.postInvalidate();
    }
    
    public void play()
    {
    	pos=duration;
    	playing=true;
    	mHandler.postDelayed(stepPlaying,250);
    }
    
    public void stop()
    {
    	pos=duration;
    	playing=false;
    	this.postInvalidate();
    	mHandler.removeCallbacks(stepPlaying);
    }
    
    Runnable stepPlaying=new Runnable()
    {
    	public void run()
    	{
    		if (pos>0)
    		{
    			pos--;
    			mHandler.postDelayed(stepPlaying,1000);
    		}
    		else{
    			pos=duration;
    			playing=false;
    		}
    		postInvalidate();
    	}
    };
    
    @SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
    	Rect dst=new Rect((int)(300*mDensity),(int)(20*mDensity),(int)(336*mDensity),(int)(56*mDensity));
    	canvas.drawBitmap(playing?stop:start, null, dst, null);
    	Rect dst2=new Rect((int)(110*mDensity),(int)(15*mDensity),(int)(130*mDensity),(int)(55*mDensity));
    	canvas.drawBitmap(audmsg, null, dst2, null);
		String s=String.format("%1$d:%2$02d", pos/60, pos%60);
		canvas.drawText(s, 0, s.length(), (int)(190*mDensity), (int)(48*mDensity), mPaint);
	}
}