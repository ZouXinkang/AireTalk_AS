package com.pingshow.voip;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;

class SlideControl extends SurfaceView implements SurfaceHolder.Callback {

	private Bitmap mBitmap;
	private ViewThread mThread;
	private Context mContext;
	private int mX=20;
	private int offset;
	private int padding;
	private boolean bAnswered;
	private Paint p1;
	private Paint p2;
	private Paint p3;
	float mDensity;
	private int thumb_width;
	private int shade=255;
	boolean pressDown=false;

	public SlideControl(Context context) {
		super(context);
		mContext=context;
		bAnswered=false;
		p1=new Paint();
		mDensity=getResources().getDisplayMetrics().density;
		offset=(int)(20.f*mDensity);
		padding=(int)(20.f*mDensity);
		mX=offset;
		p1.setTextSize(24*mDensity);
		p1.setAntiAlias(true);
		p1.setColor(shade<<24|0xffffff);
		p2=new Paint();
		p2.setColor(0x40606060);
		p2.setStrokeWidth(3);
		p2.setAntiAlias(true);
		p2.setStyle(Paint.Style.STROKE);
		p3=new Paint();
		p3.setColor(0x80000000);
		p3.setStyle(Paint.Style.FILL);
		mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.slider);
		thumb_width=mBitmap.getWidth();
		getHolder().addCallback(this);
		mThread = new ViewThread(this);
	}

	public void doDraw(Canvas canvas) {
		canvas.drawRect(new RectF(offset,padding,getWidth()-offset,getHeight()-padding), p3);
		canvas.drawRoundRect(new RectF(offset-1,padding-1,getWidth()-offset+1,getHeight()-padding+1),10,10,p2);
		shade=230-mX*255/(getWidth()-60); 
		if (shade<30) shade=30;
		canvas.drawBitmap(mBitmap, mX, padding, null);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!mThread.isAlive()) {
			mThread = new ViewThread(this);
			mThread.setRunning(true);
			mThread.start();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mThread.isAlive()) {
			mThread.setRunning(false);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (bAnswered)
			return true;
		
		mX = (int) event.getX() - thumb_width / 2;
		
		if (event.getAction()==MotionEvent.ACTION_DOWN && event.getX() < getWidth()/2)
			pressDown=true;
		
		if (event.getAction()==MotionEvent.ACTION_UP || !pressDown)
		{
			mX=0;
			pressDown=false;
		}
		
		if (mX < offset)
			mX = offset;
		else if (mX >= getWidth() - thumb_width - 10 - offset) 
		{
			mX = getWidth() - thumb_width - offset;
			if (!bAnswered && pressDown)
			{
				bAnswered=true;
				MyPreference mp=new MyPreference(mContext);
				mp.write("way", true);
				Intent it = new Intent(Global.Action_AnswerCall);
				mContext.sendBroadcast(it);
				mThread.setRunning(false);
				invalidate();
			}
		}
		return true;
	}
}

class ViewThread extends Thread {
	private SlideControl mPanel;
	private SurfaceHolder mHolder;
	private boolean mRun = false;

	public ViewThread(SlideControl panel) {
		mPanel = panel;
		mHolder = mPanel.getHolder();
	}

	public void setRunning(boolean run) {
		mRun = run;
	}

	@Override
	public void run() {
		Canvas canvas = null;
		while (mRun) {
			canvas = mHolder.lockCanvas();
			if (canvas != null) {
				mPanel.doDraw(canvas);
				mHolder.unlockCanvasAndPost(canvas);
			}
		}
	}
}