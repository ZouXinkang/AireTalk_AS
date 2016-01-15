package com.pingshow.airecenter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.browser.MainBrowser;
import com.pingshow.airecenter.register.BeforeRegisterActivity;
import com.pingshow.beehive.BeeHiveService;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnGenericMotionListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ShoppingActivity extends Activity {

	float mDensity = 1.f;
	private int numOfPackages=4;
	private int chosenIndex;
	private MyPreference mPref;
	private String dangbei = "com.dangbeimarket";
	private String appstore = "com.android.vending";
	
	static public ShoppingActivity _this;
	
	private int selectedPlan=0;
	private Float [] package_price={5f, 10f, 20f, 50f};
	
//	private Float [] security_price={3.99f, 40f};
	private Float [] security_price={7.95f, 79f};  //tml*** new price
	
	private Handler mHandler=new Handler();
	
	public static ShoppingActivity getInstance()
	{
		return _this;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.shopping_view);

		_this = this;  //tml|bj*** neverdie, moved here
		
		mPref = new MyPreference(this);

		//tml*** redirect preregister
		boolean registered = mPref.readBoolean("AireRegistered", false);
		if (!registered) {
			Intent intent = new Intent();
			intent.setClass(ShoppingActivity.this, SplashScreen.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			finish();
			return;
		}
		
        neverSayNeverDie(_this);  //tml|bj*** neverdie/
		
		mDensity = getResources().getDisplayMetrics().density;
		
		if (getIntent().getBooleanExtra("launchFromSelf", false))
			this.overridePendingTransition(R.anim.freeze, R.anim.freeze);
		
		mHandler.post(updateDueDate);
		
		((ImageView) findViewById(R.id.plan1)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					selectedPlan=0;
					((ImageView) findViewById(R.id.plan1)).setImageResource(R.drawable.checkbox_checked);
					((ImageView) findViewById(R.id.plan2)).setImageResource(R.drawable.checkbox_uncheck);
				}
			});
		
		((ImageView) findViewById(R.id.plan2)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				selectedPlan=1;
				((ImageView) findViewById(R.id.plan1)).setImageResource(R.drawable.checkbox_uncheck);
				((ImageView) findViewById(R.id.plan2)).setImageResource(R.drawable.checkbox_checked);
			}
		});
		
		((TextView)findViewById(R.id.header)).setText(getString(R.string.aire_call).toUpperCase());
		((TextView)findViewById(R.id.header2)).setText(getString(R.string.home_security).toUpperCase());
		
		((TextView)findViewById(R.id.header)).setText(getString(R.string.aire_call).toUpperCase());
		
		((Button) findViewById(R.id.buy_security)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				
			}
		});

		//tml|sw*** no airecall in china
		boolean isCN = MyUtil.isISO_China(ShoppingActivity.this, mPref, null);
		boolean airecall = mPref.readBoolean("AIRECALL", false);
		if (airecall && isCN) isCN = false;
		if (AireCallPage.OverrideShowAireCall) isCN = false;
		if (isCN) {
			((LinearLayout) findViewById(R.id.shop_airecall)).setVisibility(View.GONE);
			((View) findViewById(R.id.selected_1)).setVisibility(View.GONE);
			((ImageView) findViewById(R.id.img2)).requestFocus();
			selectTab(1);
		} else {
			((ImageView) findViewById(R.id.img1)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					selectTab(0);
					((ImageView) findViewById(R.id.img1)).requestFocus();  //tml*** prefocus
				}
			});
			((ImageView) findViewById(R.id.img1)).requestFocus();  //tml*** prefocus
		}

		((ImageView) findViewById(R.id.img2))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						selectTab(1);
						((ImageView) findViewById(R.id.img2)).requestFocus();  //tml*** prefocus
					}
				});
		
		((ImageView) findViewById(R.id.img3)).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
//						selectTab(2);
						//tml*** dangbei
						PackageManager myPackageMngr = getPackageManager();
						Intent iapp1 = myPackageMngr.getLaunchIntentForPackage(dangbei);
						Intent iapp2 = myPackageMngr.getLaunchIntentForPackage(appstore);
						if (MyUtil.isISO_China(ShoppingActivity.this, mPref, "us")) {
							if ((iapp1 != null) && (iapp1.resolveActivity(myPackageMngr) != null)) {
								iapp1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(iapp1);
							} else if ((iapp2 != null) && (iapp2.resolveActivity(myPackageMngr) != null)) {
								iapp2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(iapp2);
							}
						} else {
							if ((iapp2 != null) && (iapp2.resolveActivity(myPackageMngr) != null)) {
								iapp2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(iapp2);
							}
						}
					}
				});
		//tml*** dangbei
		PackageManager myPackageMngr = getPackageManager();
		ApplicationInfo myAppInfo;
		Drawable seeAppIcon = null;
		String seeAppName = null;
		Intent iapp1 = myPackageMngr.getLaunchIntentForPackage(dangbei);
		Intent iapp2 = myPackageMngr.getLaunchIntentForPackage(appstore);
		try {
			if (MyUtil.isISO_China(ShoppingActivity.this, mPref, "us")) {
				if (iapp1 != null) {
					myAppInfo = myPackageMngr.getApplicationInfo(dangbei, 0);
					seeAppName = (String) myPackageMngr.getApplicationLabel(myAppInfo);
					seeAppIcon = myPackageMngr.getApplicationIcon(myAppInfo);
					if (seeAppName != null) ((TextView) findViewById(R.id.partners_name)).setText(seeAppName);
					else ((TextView) findViewById(R.id.partners_name)).setText("DangBei");
				} else if (iapp2 != null) {
					myAppInfo = myPackageMngr.getApplicationInfo(appstore, 0);
					seeAppName = (String) myPackageMngr.getApplicationLabel(myAppInfo);
					seeAppIcon = myPackageMngr.getApplicationIcon(myAppInfo);
					if (seeAppName != null) ((TextView) findViewById(R.id.partners_name)).setText(seeAppName);
					else ((TextView) findViewById(R.id.partners_name)).setText("App Store");
				}
			} else {
				if (iapp2 != null) {
					myAppInfo = myPackageMngr.getApplicationInfo(appstore, 0);
					seeAppName = (String) myPackageMngr.getApplicationLabel(myAppInfo);
					seeAppIcon = myPackageMngr.getApplicationIcon(myAppInfo);
					if (seeAppName != null) ((TextView) findViewById(R.id.partners_name)).setText(seeAppName);
					else ((TextView) findViewById(R.id.partners_name)).setText("App Store");
				}
			}
			
			if (seeAppIcon != null) {
				((ImageView) findViewById(R.id.img3)).setImageDrawable(seeAppIcon);
				float mden = getResources().getDisplayMetrics().density;
				((ImageView) findViewById(R.id.img3)).setPadding((int) (5 * mden), (int) (5 * mden), (int) (5 * mden), (int) (5 * mden));
			}
		} catch (NameNotFoundException e) {}
		//***tml
		
		((Button)findViewById(R.id.terms)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it=new Intent(ShoppingActivity.this, Tooltip.class);
				it.putExtra("width", 780);
				it.putExtra("height", 473);
				String lang=Locale.getDefault().getLanguage();
				it.putExtra("URL", "http://www.airetalk.com/terms_policy_airecenter.php?d=and&l="+lang);
	            startActivity(it);
			}
		});
		
		((Button)findViewById(R.id.terms_security)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it=new Intent(ShoppingActivity.this, Tooltip.class);
				it.putExtra("width", 780);
				it.putExtra("height", 473);
				String lang=Locale.getDefault().getLanguage();
				it.putExtra("URL", "http://www.airetalk.com/security_terms_airecenter.php?d=and&l="+lang);
	            startActivity(it);
			}
		});
		
		((Button)findViewById(R.id.airecall)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent it = new Intent(ShoppingActivity.this, MainActivity.class);
				it.putExtra("launchFromSelf", true);
				it.putExtra("switchToInflate", 3);
				startActivity(it);
				finish();
			}
		});
		
		mHandler.post(new Runnable(){
			public void run()
			{
				String lang=Locale.getDefault().getLanguage();
				((WebView)findViewById(R.id.partners)).loadUrl("http://www.airetalk.com/partners.php?d=and&l="+lang);
			}
		});
		
		((Button)findViewById(R.id.buy)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				chosenIndex=1;
				mHandler.post(new Runnable(){
					public void run()
					{
						onBuyPressed();
					}
				});
			}
		});
		
		((Button)findViewById(R.id.buy_security)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onSubscribeSecurity();
			}
		});
		
		DialerFrame.setFrame(this, findViewById(android.R.id.content));
		
		float credit=mPref.readFloat("Credit",0);
        TextView tv=(TextView)findViewById(R.id.credit);
        if (tv!=null) tv.setText(String.format(getString(R.string.credit), credit));
		
		////// SideBar

		((ImageView) findViewById(R.id.bar1)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it = new Intent(ShoppingActivity.this, MainActivity.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
		//tml*** beta ui2 X
//		((ImageView) findViewById(R.id.bar6)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				
//			}
//		});
		((ImageView) findViewById(R.id.bar7)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it = new Intent(ShoppingActivity.this, SecurityNewActivity.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
		//tml*** beta ui2 X
        ((ImageView)findViewById(R.id.bar9)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//tml*** alpha iot ui
//				Intent it=new Intent(ShoppingActivity.this, HomeIOTActivity.class);
//				it.putExtra("launchFromSelf", true);
//				startActivity(it);
//				finish();
				Intent it=new Intent(ShoppingActivity.this, MainActivity.class);
				it.putExtra("launchFromSelf", true);
				it.putExtra("switchToInflate", 5);
				startActivity(it);
				finish();
			}
		});
        ((Button)findViewById(R.id.back)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it=new Intent(ShoppingActivity.this, MainActivity.class);
				it.putExtra("launchFromSelf", true);
				it.putExtra("switchToInflate", 5);
				startActivity(it);
				finish();
			}
		});
        ((ImageView)findViewById(R.id.bar9)).setImageAlpha(150);  //temp until icon-faded available
        //tml*** beta ui2 X
//		((ImageView) findViewById(R.id.bar8)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
////				Intent it=new Intent(ShoppingActivity.this, LocationSettingActivity.class);
//				Intent it=new Intent(ShoppingActivity.this, MainBrowser.class);  //tml*** browser save/
//				it.putExtra("launchFromSelf", true);
//				startActivity(it);
//				finish();
//			}
//		});

        //tml*** beta ui2
        ((RelativeLayout) findViewById(R.id.sidebar_ghost)).setOnGenericMotionListener(sideBarMotionListener);
        ((ImageView) findViewById(R.id.menu_main)).setOnClickListener(new OnClickListener () {
			@Override
			public void onClick(View v) {
				((DrawerLayout) findViewById(R.id.main_content)).openDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));
			}
        });
        
		mHandler.postDelayed(checkSecurityStatus, 3000);
		mHandler.postDelayed(checkSipCredit, 3000);
	}
	
	Runnable checkSecurityStatus=new Runnable(){
		public void run()
		{
			if (AireJupiter.getInstance()!=null)
				AireJupiter.getInstance().requestSecuritySubscription();
			
			mHandler.postDelayed(checkSecurityStatus, 30000);
		}
	};

	Runnable checkSipCredit = new Runnable () {
		public void run() {
			Intent intent = new Intent(Global.Action_InternalCMD);
			intent.putExtra("Command", Global.CMD_UPDATE_SIP_CREDIT);
			sendBroadcast(intent);
		}
	};
	
	private void selectTab(int i)
	{
		((LinearLayout) findViewById(R.id.credit_view)).setVisibility((i==0)?View.VISIBLE:View.GONE);
		((LinearLayout) findViewById(R.id.security_view)).setVisibility((i==1)?View.VISIBLE:View.GONE);
		((LinearLayout) findViewById(R.id.partner_view)).setVisibility((i==2)?View.VISIBLE:View.GONE);

		((View)findViewById(R.id.selected_1)).setVisibility((i==0)?View.VISIBLE:View.INVISIBLE);
		((View)findViewById(R.id.selected_2)).setVisibility((i==1)?View.VISIBLE:View.INVISIBLE);
		((View)findViewById(R.id.selected_3)).setVisibility((i==2)?View.VISIBLE:View.INVISIBLE);
	}
	
	Runnable updateDueDate = new Runnable() {
		public void run() {
			String sub_status = mPref.read("SecurityDueDate", "---");
			//((TextView) findViewById(R.id.payment_status)).setText(R.string.trial_due);
			//((TextView) findViewById(R.id.payment_status)).setText(R.string.next_billing);
			//((TextView) findViewById(R.id.due_date)).setText(R.string.deactivated);
			//tml|sw*** subscription php
			((TextView) findViewById(R.id.payment_status)).setText(R.string.subsc_ends);
			if (sub_status.startsWith("expired")) {
				((TextView) findViewById(R.id.due_date)).setText(R.string.expired);
			} else if (sub_status.startsWith("failed")) {
				((TextView) findViewById(R.id.due_date)).setText(R.string.security_fail);
			} else if (sub_status.startsWith("success")) {
				((TextView) findViewById(R.id.due_date)).setText(R.string.security_active);
			} else {
				((TextView) findViewById(R.id.due_date)).setText("---");
			}

			//tml|sw*** subscription php
//			try {
//				String date = sub_status.substring(0, sub_status.indexOf(":") - 3);
//				if (date != null) {
//					SimpleDateFormat idate = new SimpleDateFormat("yyyy-MM-dd");
//					SimpleDateFormat odate = new SimpleDateFormat("MMMM d, yyyy");
//					Date xdate = idate.parse(date);
//					date = odate.format(xdate);
//					((TextView) findViewById(R.id.due_date)).setText(date);
//				}
//			} catch (Exception e) {}
		}
	};
	
	public void updateDueDate()
	{
		mHandler.post(updateDueDate);
	}
	
	static public void updateCredit(float credit)
	{
		if (_this!=null)
		{
			TextView tv=(TextView)_this.findViewById(R.id.credit);
	        if (tv!=null) tv.setText(String.format(_this.getString(R.string.credit), credit));
		}
	}
	
	public FrameLayout getNotificationLayout()
	{
		return (FrameLayout)findViewById(R.id.notification);
	}
	
	public void onBuyPressed() 
	{
		if (chosenIndex<0 || chosenIndex>=numOfPackages) chosenIndex=0;
		String myUsername = mPref.read("myPhoneNumber", "----");
		try {
			String url = "https://airetalk.com/FirstData/paymentgate_get.php?"
					+"x_user3="+package_price[chosenIndex]
					+"&x_user1="+URLEncoder.encode(myUsername,"UTF-8")
					+"&x_user2="+Integer.parseInt(mPref.read("myID","0"),16)
					+"&x_amount="+package_price[chosenIndex]
					+"&button_code=Pay+Now+Airetalk+Payment";
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(url));
			startActivity(i);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public void onSubscribeSecurity() 
	{
		if (selectedPlan<0 || selectedPlan>1) selectedPlan=0;
		String myUsername = mPref.read("myPhoneNumber", "----");
		//tml*** china ip
		String domain = AireJupiter.myAcDomain_default;
		if (AireJupiter.getInstance() != null) {
			domain = AireJupiter.getInstance().getIsoDomain();
		}
		try {
			String url = "http://" + domain + "/FirstData/paymentgate_center.php?"
					+"x_user3="+security_price[selectedPlan]
					+"&x_user1="+Integer.parseInt(mPref.read("myID","0"),16)
					+"&x_user2="+URLEncoder.encode(myUsername,"UTF-8")
					+"&x_amount="+security_price[selectedPlan]
					+"&button_code=Buy+Security+Subscription+Plan";
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(url));
			startActivity(i);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	/*
	void showInstalledApps()
	{
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		PackageManager pm=getPackageManager();
		final List<ResolveInfo> pkgAppsList = pm.queryIntentActivities( mainIntent, 0);
		float mDensity=getResources().getDisplayMetrics().density;
		LinearLayout content=(LinearLayout)findViewById(R.id.content);
		LinearLayout hr=null;
		int i=0;
		for (ResolveInfo packageInfo : pkgAppsList) {
			if ((i%5)==0)
			{
				hr=new LinearLayout(this);
				hr.setOrientation(LinearLayout.HORIZONTAL);
				hr.setGravity(Gravity.LEFT);
				hr.setPadding((int)(40*mDensity), (int)(20*mDensity), (int)(40*mDensity), (int)(20*mDensity));
				content.addView(hr,LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT);
			}
			LinearLayout item=new LinearLayout(this);
			item.setOrientation(LinearLayout.VERTICAL);
			item.setGravity(Gravity.CENTER_HORIZONTAL);
			
			ImageView img=new ImageView(this);
			img.setImageDrawable(packageInfo.loadIcon(pm));
			item.addView(img,(int)(90*mDensity),(int)(90*mDensity));
			img.setBackgroundResource(R.drawable.optionbtn);
			
			TextView txt=new TextView(this);
			txt.setText(packageInfo.loadLabel(pm));
			txt.setTextColor(0xff2a3a4a);
			txt.setTypeface(null, Typeface.BOLD);
			txt.setTextSize(23);
			txt.setGravity(Gravity.CENTER_HORIZONTAL);
			item.addView(txt,(int)(160*mDensity),LayoutParams.WRAP_CONTENT);
			
			LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams((int)(160*mDensity),LayoutParams.WRAP_CONTENT);
			lp.leftMargin=(int)(10*mDensity);
			lp.rightMargin=(int)(10*mDensity);
			hr.addView(item, lp);
		    //Log.d("Installed package :" + packageInfo.packageName);
		    //Log.d("Source dir : " + packageInfo.sourceDir);
		    //Log.d("Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));
			
			i++;
		}
	}*/


	@Override
    protected void onResume() {
    	super.onResume();
    	DialerFrame.checkEmbeddedDialer(findViewById(android.R.id.content));
	}
	
	@Override
	protected void onDestroy()
	{
		_this=null;
		mHandler.removeCallbacks(checkSecurityStatus);
		mHandler.removeCallbacks(checkSipCredit);
		super.onDestroy();
	}
	
	public void close()
	{
		finish();
	}

	//tml|bj*** neverdie
	public void neverSayNeverDie(Context context) {
		if (AireJupiter.getInstance()==null) {
			Log.e("AireJupiter.getInstance() is null, RESETTING");
			Intent vip0 = new Intent(context, BeeHiveService.class);
			context.stopService(vip0);
			Intent vip1 = new Intent(context, AireVenus.class);
			context.stopService(vip1);
			Intent vip2 = new Intent(context, AireJupiter.class);
			context.stopService(vip2);
			
			Intent vip00 = new Intent(context, AireJupiter.class);
			context.startService(vip00);
		}
	}

	//tml*** beta ui2
	private OnGenericMotionListener sideBarMotionListener = new OnGenericMotionListener() {
		@Override
		public boolean onGenericMotion(View v, MotionEvent event) {
			int action = event.getAction();
			int source = event.getSource();
			
			if (source == InputDevice.SOURCE_MOUSE) {
				switch (action) {
					case MotionEvent.ACTION_HOVER_ENTER:
						if (v.getId() == R.id.sidebar_ghost) {
							Log.d("aloha mouse onto sidebar zone");
							if (!((DrawerLayout) findViewById(R.id.main_content)).isDrawerOpen((RelativeLayout) findViewById(R.id.sidebar_frame_drawer))) {
								((DrawerLayout) findViewById(R.id.main_content)).openDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));
							}
						}
						break;
					case MotionEvent.ACTION_HOVER_EXIT:
						if (v.getId() == R.id.sidebar_ghost) {
							Log.d("aloha mouse left sidebar zone");
							if (((DrawerLayout) findViewById(R.id.main_content)).isDrawerOpen((RelativeLayout) findViewById(R.id.sidebar_frame_drawer))) {
								mHandler.postDelayed(new Runnable() {
									@Override
									public void run() {
										((DrawerLayout) findViewById(R.id.main_content)).closeDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));
									}
								}, 500);
							}
						}
						break;
				}
			}
			return false;
		}
	};
}
