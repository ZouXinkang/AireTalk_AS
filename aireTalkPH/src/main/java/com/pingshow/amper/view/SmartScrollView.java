package com.pingshow.amper.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class SmartScrollView extends ScrollView {

	public SmartScrollView(Context context) {
        super(context);
    }

    public SmartScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SmartScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public interface SmartScrollViewListener {
        void onScrollChanged(SmartScrollView v, int cur_x, int cur_y, int prev_x, int prev_y);
    }
    
    private SmartScrollViewListener mSmartScrollViewListener;

    public void setSmartScrollViewListener(SmartScrollViewListener listener) {
        this.mSmartScrollViewListener = listener;
    }
	
    protected void onScrollChanged(int cur_x, int cur_y, int prev_x, int prev_y) {
    	if (mSmartScrollViewListener != null) {
        	mSmartScrollViewListener.onScrollChanged(this, cur_x, cur_y, prev_x, prev_y);
    	}
        super.onScrollChanged(cur_x, cur_y, prev_x, prev_y);
    }
}