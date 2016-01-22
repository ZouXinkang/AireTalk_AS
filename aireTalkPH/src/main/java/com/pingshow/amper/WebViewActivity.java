package com.pingshow.amper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

public class WebViewActivity extends Activity{
	private Handler mHandler = new Handler();
	private WebView mBrowser;
	private String URL;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview_page);
		
		mBrowser=(WebView)findViewById(R.id.webview);
		URL=getIntent().getStringExtra("URL");
		
		((TextView)findViewById(R.id.title)).setText(getIntent().getStringExtra("Title"));
		
		((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		mHandler.post(startBrowser);
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	private Runnable startBrowser=new Runnable(){
		public void run(){
			try{
//				mBrowser.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
//				mBrowser.getSettings().setJavaScriptEnabled(true);
//				mBrowser.getSettings().setUseWideViewPort(true);
//				mBrowser.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
//				mBrowser.getSettings().setPluginState(PluginState.ON);
//				mBrowser.getSettings().setAllowFileAccess(false);
//				mBrowser.getSettings().setUserAgentString("Android");
//				
//				mBrowser.loadUrl(URL);
				//wjx*** hot news fix
				mBrowser.getSettings().setJavaScriptEnabled(true);
				mBrowser.getSettings().setPluginState(PluginState.ON);
//				mBrowser.getSettings().setPluginsEnabled(true);
				mBrowser.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
				mBrowser.getSettings().setAllowFileAccess(true);
				mBrowser.getSettings().setDefaultTextEncodingName("UTF-8");
				mBrowser.getSettings().setLoadWithOverviewMode(true);
				mBrowser.getSettings().setUseWideViewPort(true);
				mBrowser.setVisibility(View.VISIBLE);
				
				mBrowser.loadUrl(URL);
				mBrowser.setWebViewClient(new WebViewClient() {
					public boolean shouldOverrideUrlLoading(WebView view, String url) {
						if (url.indexOf(".3gp") != -1
								|| url.indexOf(".mp4") != -1
								|| url.indexOf(".flv") != -1) {
							Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
							view.getContext().startActivity(intent);
							return true;
						} else {
							view.loadUrl(url);
							return true;
						}
					}
				});
				//***wjx
			}catch(Exception e){}
			catch(Error e){}
		}
	};
}
