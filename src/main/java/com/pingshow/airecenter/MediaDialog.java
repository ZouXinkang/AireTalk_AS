package com.pingshow.airecenter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.pingshow.airecenter.R;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.ImageUtil;

public class MediaDialog extends Activity {
	
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.media_dialog);
	    float mDensity = getResources().getDisplayMetrics().density;
	    WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.5f;
		lp.width=(int)(687.f*mDensity);
		lp.height=(int)(320.f*mDensity);
		getWindow().setAttributes(lp);
	    
	    ((ImageView)findViewById(R.id.close)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			setResult(RESULT_CANCELED);
    			finish();
    		}
	    });
	    
	    ((Button)findViewById(R.id.photo)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			Intent it=new Intent();
    			it.putExtra("action", 0);
    			setResult(RESULT_OK, it);
    			finish();
    		}
	    });
	    
	    ((Button)findViewById(R.id.picture)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			Intent it=new Intent();
    			it.putExtra("action", 1);
    			setResult(RESULT_OK, it);
    			finish();
    		}
	    });
	    
	    ((Button)findViewById(R.id.video)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			Intent it=new Intent();
    			it.putExtra("action", 2);
    			setResult(RESULT_OK, it);
    			finish();
    		}
	    });
	    
	    ((Button)findViewById(R.id.record)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			Intent it=new Intent();
    			it.putExtra("action", 3);
    			setResult(RESULT_OK, it);
    			finish();
    		}
	    });
	}
}