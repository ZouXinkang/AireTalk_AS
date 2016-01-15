package com.pingshow.airecenter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import com.pingshow.airecenter.R;

public class Tooltip extends Activity{
	
	private String URL;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tooltip);
		
		Intent intent=getIntent();
		float mDensity = getResources().getDisplayMetrics().density;
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		int width=intent.getIntExtra("width", 0);
		if (width>0)
		{
			lp.width=(int)(mDensity*width);
			lp.height=(int)(mDensity*intent.getIntExtra("height", 473));
		}
		lp.dimAmount = 0.3f;
		getWindow().setAttributes(lp);

		if (intent!=null)
		{
			String url=intent.getStringExtra("URL");
			if (url==null)
			{
				String content=intent.getStringExtra("Content");
				((TextView)findViewById(R.id.content)).setText(content);
			}else{
				((TextView)findViewById(R.id.content)).setVisibility(View.GONE);
				URL=intent.getStringExtra("URL");
				new Handler().post(loadWebContent);
			}
		}
		((TextView)findViewById(R.id.content)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		((ImageView)findViewById(R.id.close)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	Runnable loadWebContent=new Runnable()
	{
		public void run()
		{
			WebView webView=(WebView)findViewById(R.id.webview);
			webView.setVisibility(View.VISIBLE);
	        webView.loadUrl(URL);
		}
	};
}
