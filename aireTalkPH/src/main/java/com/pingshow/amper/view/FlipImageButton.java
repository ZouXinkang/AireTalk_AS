package com.pingshow.amper.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pingshow.amper.R;

public class FlipImageButton extends RelativeLayout {
	private float mDensity;
	private TextView caption;
	private ImageView _image;
	
    public FlipImageButton(Context context) {
        super(context);
        mDensity=this.getResources().getDisplayMetrics().density;
        
        caption=new TextView(context);
        _image=new ImageView(context);
    }
    
    public void setText(String title)
    {
    	RelativeLayout.LayoutParams c=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
    	c.addRule(RelativeLayout.CENTER_HORIZONTAL);
    	c.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
    	caption.setLayoutParams(c);
    	caption.setTextColor(0xffffff50);
    	caption.setText(title);
    	caption.setTextSize(15);
    	caption.setShadowLayer(2, 1, 2, 0x80000000);
    	caption.setPadding(0, (int)(mDensity*10), 0, (int)(mDensity*10));
    	this.addView(caption);
    }
    
    public void setImage(int ResID)
    {
    	RelativeLayout.LayoutParams a=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
    	a.addRule(RelativeLayout.CENTER_IN_PARENT);
    	_image.setLayoutParams(a);
    	_image.setImageResource(ResID);
    	this.addView(_image);
    }
    
    public void flip()
	{
    	AnimationSet as = new AnimationSet(true);
	    as.setInterpolator(new AccelerateInterpolator());
		ScaleAnimation sa = new ScaleAnimation(1, -1, 1, 1, getWidth()/2, getHeight()/2);
		sa.setDuration(300);
		as.addAnimation(sa);
		as.setDuration(300);
		startAnimation(as);
	}
}