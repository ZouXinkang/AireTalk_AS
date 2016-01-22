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

public class FlipToggleView extends RelativeLayout {
	private float mDensity;
	private TextView caption;
	private ImageView check;
	private ImageView stars;
	private boolean checked;
	clickCallback clickCB;
	private int id;

    public FlipToggleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDensity=this.getResources().getDisplayMetrics().density;
        
        caption=new TextView(context);
        check=new ImageView(context);
        stars=new ImageView(context);
    }
    
    public void setChecked(boolean checked)
    {
    	if (check!=null) 
    		check.setVisibility(checked?View.VISIBLE:View.INVISIBLE);
    	this.checked=checked;
    }
    
    public void init(int id, String title, int ResID, clickCallback clickcb)
    {
    	this.id=id;
    	clickCB=clickcb;
    	
    	RelativeLayout.LayoutParams a=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,(int)(mDensity*30f));
    	a.addRule(RelativeLayout.CENTER_IN_PARENT);
    	stars.setLayoutParams(a);
    	
    	RelativeLayout.LayoutParams c=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
    	c.addRule(RelativeLayout.CENTER_IN_PARENT);
    	caption.setLayoutParams(c);
    	caption.setText(title);
    	caption.setTextSize(16);
    	
    	caption.setTextColor(0xff800080);
    	stars.setImageResource(ResID);
    	
    	this.addView(stars);
        this.addView(caption);
    	
    	if (clickcb!=null)
    	{
    		RelativeLayout.LayoutParams b=new RelativeLayout.LayoutParams((int)(mDensity*30f),(int)(mDensity*30f));
        	b.addRule(RelativeLayout.CENTER_VERTICAL);
        	b.leftMargin=(int)(mDensity*10);
        	check.setLayoutParams(b);
        	check.setImageResource(R.drawable.okay);
        	check.setVisibility(View.INVISIBLE);
        	this.addView(check);
    	}
    }
    
    private void flip()
	{
    	AnimationSet as = new AnimationSet(true);
	    as.setInterpolator(new AccelerateInterpolator());
		ScaleAnimation sa = new ScaleAnimation(1, 1, 1, -1, getWidth()/2, getHeight()/2);
		sa.setDuration(250);
		as.addAnimation(sa);
		as.setDuration(250);
		startAnimation(as);
		
		if (check!=null) check.setVisibility(checked?View.VISIBLE:View.INVISIBLE);
		
		if (clickCB!=null) clickCB.onSelect(id);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!checked)
		{	
			checked=true;
			flip();
		}else
			flip();
		return super.onTouchEvent(event);
	}
	
	public interface clickCallback {
		public void onSelect(int index);
	}
}