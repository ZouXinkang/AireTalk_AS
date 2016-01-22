package com.pingshow.amper.view;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

import com.pingshow.amper.Global;

public class HScroll extends HorizontalScrollView {
	private final String namespace = "http://www.airetalk.com";
	private float mWidth,mDensity;
	private float mx;
	private float interval=34;
	private int offset=139;
	private int dest;
	private int barId=0;
	private Handler mHandler=new Handler();
	private Context mContext;

    public HScroll(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDensity=this.getResources().getDisplayMetrics().density;
        barId = attrs.getAttributeIntValue(namespace,"barId", 0);
        interval = attrs.getAttributeIntValue(namespace,"interval", 34);
        offset = attrs.getAttributeIntValue(namespace,"offset", 139);
        mContext=context;
    }

    public HScroll(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDensity=this.getResources().getDisplayMetrics().density;
        barId = attrs.getAttributeIntValue(namespace,"barId", 0);
        interval = attrs.getAttributeIntValue(namespace,"interval", 34);
        offset = attrs.getAttributeIntValue(namespace,"offset", 139);
        mContext=context;
    }

    public HScroll(Context context) {
        super(context);
        mDensity=this.getResources().getDisplayMetrics().density;
        mContext=context;
    }
    
    public int getPosition() {
    	float curX = this.getScrollX();
    	mWidth=this.getWidth();
    	return (int)((curX+mWidth/2-4-mDensity*offset)/(interval*mDensity));
    }
    
    
    public void setPosition(int pos) {
    	mWidth=this.getWidth();
    	dest=(int)(pos*(interval*mDensity)+4+(offset+interval/2)*mDensity-mWidth/2);
    	mHandler.removeCallbacks(scrolls);
    	mHandler.postDelayed(scrolls,50);
    }
    
    Runnable scrolls=new Runnable()
    {
    	public void run()
    	{
    		float curX = HScroll.this.getScrollX();
    		if (Math.abs(dest-curX)>5 && Math.abs(dest-curX)<3000)
    		{
	    		int step=(int)((dest-curX)/2);
	    		HScroll.this.scrollBy(step, 0);
	    		mHandler.postDelayed(scrolls,50);
    		}
    		else{
    			HScroll.this.scrollTo(dest, 0);
    		}
    	}
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float curX;
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
            	mHandler.removeCallbacks(scrolls);
                mx = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                curX = event.getX();
                this.scrollBy((int) (mx - curX), 0);
                mx = curX;
                break;
            case MotionEvent.ACTION_UP:
            	int pos=getPosition();
            	setPosition(pos);
            	mHandler.removeCallbacks(scrollResult);
            	mHandler.removeCallbacks(scrollStart);
            	mHandler.post(scrollStart);
            	mHandler.postDelayed(scrollResult, 1000);
                break;
        }

        return true;
    }
    
    Runnable scrollResult=new Runnable(){
		public void run(){
			int pos=getPosition();
			Intent it = new Intent(Global.ACTION_TUNING);
			it.putExtra("pos", pos);
			it.putExtra("bar", barId);
			mContext.sendBroadcast(it);
		}
	};
	
	Runnable scrollStart=new Runnable(){
		public void run(){
			int pos=getPosition();
			Intent it = new Intent(Global.ACTION_TUNING_START);
			it.putExtra("pos", pos);
			it.putExtra("bar", barId);
			mContext.sendBroadcast(it);
		}
	};
}