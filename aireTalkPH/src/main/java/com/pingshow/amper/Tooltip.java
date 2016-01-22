package com.pingshow.amper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;

public class Tooltip extends Activity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tooltip);
		this.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
		
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.3f;
		getWindow().setAttributes(lp);
		
		Intent intent=getIntent();
		if (intent!=null)
		{
			String content=intent.getStringExtra("Content");
			((TextView)findViewById(R.id.content)).setText(content);
		}
		((TextView)findViewById(R.id.content)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
}
