
package com.pingshow.amper.view;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.HorizontalScrollView;

import com.pingshow.amper.Log;

public class MyHorizontalScrollView extends HorizontalScrollView {
	
	public boolean menuOut=false;
	
    public MyHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public MyHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MyHorizontalScrollView(Context context) {
        super(context);
        init(context);
    }

    void init(Context context) {
        // remove the fading as the HSV looks better without it
        setHorizontalFadingEdgeEnabled(false);
        setVerticalFadingEdgeEnabled(false);
    }

    public void initViews(View[] children,int scrollToViewIdx, SizeCallback sizeCallback) {
        ViewGroup parent = (ViewGroup) getChildAt(0);

        for (int i = 0; i < children.length; i++) {
            if (i==0) children[i].setVisibility(View.INVISIBLE);
            else children[i].setVisibility(View.VISIBLE);
            parent.addView(children[i]);
        }
        
        Log.i("******* initViews *****");

        OnGlobalLayoutListener listener = new MyOnGlobalLayoutListener(parent, children, scrollToViewIdx, sizeCallback);
        getViewTreeObserver().addOnGlobalLayoutListener(listener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    class MyOnGlobalLayoutListener implements OnGlobalLayoutListener {
        ViewGroup parent;
        View[] children;
        int scrollToViewIdx;
        int scrollToViewPos = 0;
        SizeCallback sizeCallback;

        public MyOnGlobalLayoutListener(ViewGroup parent, View[] children, int scrollToViewIdx, SizeCallback sizeCallback) {
            this.parent = parent;
            this.children = children;
            this.scrollToViewIdx = scrollToViewIdx;
            this.sizeCallback = sizeCallback;
        }

        @SuppressWarnings("deprecation")
		@Override
        public void onGlobalLayout() {
        	Log.i("******* onGlobalLayout *****");

            final HorizontalScrollView me = MyHorizontalScrollView.this;

            // The listener will remove itself as a layout listener to the HSV
            try{
            	me.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }catch(Error e){}

            // Allow the SizeCallback to 'see' the Views before we remove them and re-add them.
            // This lets the SizeCallback prepare View sizes, ahead of calls to SizeCallback.getViewSize().
            sizeCallback.onGlobalLayout();

            parent.removeViewsInLayout(0, children.length);

            final int w = me.getMeasuredWidth();
            final int h = me.getMeasuredHeight();

            int[] dims = new int[2];
            scrollToViewPos = 0;
            for (int i = 0; i < children.length; i++) {
                sizeCallback.getViewSize(i, w, h, dims);
                children[i].setVisibility(View.VISIBLE);
                parent.addView(children[i], dims[0], dims[1]);
                if (i < scrollToViewIdx) {
                    scrollToViewPos += dims[0];
                }
            }
            
            //me.scrollTo(scrollToViewPos, 0);
            
            new Handler().post(new Runnable(){
            	public void run()
            	{
            		me.scrollTo(scrollToViewPos, 0);
            	}
            });
        }
    }

    public interface SizeCallback {
        public void onGlobalLayout();
        public void getViewSize(int idx, int w, int h, int[] dims);
    }
}
