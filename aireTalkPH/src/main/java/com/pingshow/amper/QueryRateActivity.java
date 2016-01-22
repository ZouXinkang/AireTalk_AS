package com.pingshow.amper;

import java.net.URLEncoder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.pingshow.amper.view.FlipToggleView;
import com.pingshow.network.MyNet;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;

public class QueryRateActivity extends Activity {
	private MyPreference mPref;
	private Handler mHandler=new Handler();
	private String lastQueried;
	private DQCurrency currency;
	private int SelectedClass;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.query_rate_page);

		this.overridePendingTransition(R.anim.push_up_in, R.anim.freeze);
		currency=new DQCurrency(this);
		
		mPref=new MyPreference(this);
		
		String last=mPref.read("lastQueriedNumber");
		if (last!=null)
			((EditText)findViewById(R.id.number)).setText(last);
		
		((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		((Button)findViewById(R.id.done)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try{
					progress = ProgressDialog.show(QueryRateActivity.this, "", getString(R.string.in_progress), true, true);
				}catch(Exception e){}
				getGlobalNumber();
				new Thread(onQureyRate).start();
				
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(((EditText)findViewById(R.id.number)).getWindowToken(), 0);
			}
		});
		
		((EditText)findViewById(R.id.number)).setOnEditorActionListener(new OnEditorActionListener(){

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				try{
					progress = ProgressDialog.show(QueryRateActivity.this, "", getString(R.string.in_progress), true, true);
				}catch(Exception e){}
				getGlobalNumber();
				new Thread(onQureyRate).start();
				return false;
			}
		});
		
		SelectedClass=mPref.readInt("SelectedClass",0);
		final int stringResId[]={R.string.standard_class,R.string.premium_class, R.string.business_class};
		final int imageResId[]={R.drawable.standard, R.drawable.premium, R.drawable.business};
		((FlipToggleView)findViewById(R.id.selected_class)).init(0, getString(stringResId[SelectedClass]), imageResId[SelectedClass], null);
		((FlipToggleView)findViewById(R.id.selected_class)).setChecked(true);
	}
	
	@Override
	protected void onDestroy() {
		if (progress!=null && progress.isShowing())
			progress.dismiss();
		super.onDestroy();
	}
	
	private ProgressDialog progress;
	private String resultString;
	private String global;
	
	void getGlobalNumber()
	{
		String number=((EditText)findViewById(R.id.number)).getText().toString();
		
		global=number;

		MyTelephony.init(QueryRateActivity.this);
		if (MyTelephony.validWithCurrentISO(number))
		{
			global=MyTelephony.addPrefixWithCurrentISO(number);
		}else{
			if (MyTelephony.validLandLineWithCurrentISO(number))
				global=MyTelephony.addPrefixLandLineWithCurrentISO(number);
		}
		
		if (!global.startsWith("+"))
		{
			global=MyTelephony.attachPrefix(QueryRateActivity.this, number);
		}
		if (!global.startsWith("+"))
		{
			global=MyTelephony.attachFixedPrefix(QueryRateActivity.this, number);
		}
		
		((EditText)findViewById(R.id.number)).setText(global);
	}
	
	Runnable dismissProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			if (progress!=null && progress.isShowing()){
				try{
					progress.dismiss();
				}catch(Exception e){}
			}
		}
	};
	
	Runnable QueryFinished=new Runnable()
	{
		@Override
		public void run() {
			try{
				TextView results=(TextView)findViewById(R.id.result);
				results.setText(resultString);
				
				AnimationSet as = new AnimationSet(false);
			    as.setInterpolator(new AccelerateInterpolator());
			    AlphaAnimation aa= new AlphaAnimation(0, 1.0f);
				ScaleAnimation sa = new ScaleAnimation(0.2f, 1f, 0.2f, 1f, results.getWidth()/2, results.getHeight()/2);
				sa.setDuration(700);
				as.addAnimation(sa);
				aa.setDuration(500);
				as.addAnimation(aa);
				as.setDuration(700);
				results.startAnimation(as);
				results.setVisibility(View.VISIBLE);
			}catch(Exception e){}
		}
	};
	
	private Runnable onQureyRate = new Runnable() 
	{
		@Override
		public void run() {
			if (global.length() > 0) 
			{	
				MyUtil.Sleep(1500);
				
				try {
					if (lastQueried!=null && lastQueried.equals(global))
					{	
						mHandler.post(dismissProgressDialog);
						return;
					}
					
					mPref.write("lastQueriedNumber", global);
					
					lastQueried=global;
					
					int count=0;
					String Return="";
					
					try{
						do{
							MyNet net = new MyNet(QueryRateActivity.this);
							Return=net.doPost("queryrate.php", "num="+URLEncoder.encode(global,"UTF-8")+
								"&callplan="+SelectedClass, null);
							if (Return.length()>0) break;
						}while(++count<2);
					}catch(Exception e){
						Log.e("queryrate.php !@#$ " + e.getMessage());
					}
					
					mHandler.post(dismissProgressDialog);
					
					if (Return.startsWith("Done"))
					{
						Return=Return.replace("\n", "");
						String[] items = Return.split(",");
						if (items.length>2)
						{
							float rate=Float.parseFloat(items[1]);
		                    float curr=currency.getCurrencyFromUSD();
		                   
		                    if (currency.isUsingUSD())
		                    	resultString = String.format(getString(R.string.precise_rate), 
										currency.translate("USD"),
										rate,
										currency.translate("EUR"),
										"�",
										currency.getEuroCurrency()*rate,
										MyTelephony.getCountryNameByNumber(QueryRateActivity.this, global),
										items[2]);
		                    else
		                    	resultString = String.format(getString(R.string.precise_rate), 
										currency.translate("USD"),
										rate,
										currency.getCurrencyCode(),
										currency.getCurrencySymbol(),
										curr*rate,
										MyTelephony.getCountryNameByNumber(QueryRateActivity.this, global),
										items[2]);
							
							mHandler.post(QueryFinished);
						}
					}
				} catch (Exception e) {
					Log.e("onQureyRate !@#$ " + e.getMessage());
				}
			}
			else
				mHandler.post(dismissProgressDialog);
		}
	};
}
