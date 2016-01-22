package com.pingshow.amper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.pingshow.amper.register.BeforeRegisterActivity;
import com.pingshow.amper.view.FlipImageButton;
import com.pingshow.util.MyTelephony;

public class PurchaseActivity extends Activity{
	private MyPreference mPref;
	private Handler mHandler=new Handler();
	private DQCurrency currency;
	private ProgressDialog progress;
	private CountryRatesAdapter rateAdapter;
	private ListView mList;
	float mDensity = 1.f;
	int SelectedClass=0;
	private int chosenIndex;
	private int numOfPackages=0;
	
	private List<Map<String, Object>> countryList=new ArrayList<Map<String, Object>>();
	
//	private String [] package_name={
//		    "AireCall $5 Pack",
//		    "AireCall $10 Pack",  
//		    "AireCall $20 Pack",  
//		    "AireCall $50 Pack",
//		    "","","","","","","","","",""
//		};
//	private Float [] package_price={4.95f, 9.50f, 18.50f, 45.0f};
	private String [] package_name={
		    "AireCall $5 Pack",
		    "AireCall $10 Pack", 
		    "","","","","","","","","",""
		};
	private Float [] package_price = {4.95f, 9.50f};  //tml*** limit purchase
	private Float [] package_credit = {5.0f, 10.0f};  //tml*** limit purchase
	
	static PurchaseActivity instance=null;
	static public void updateCredit(float credit)
	{
		if (instance!=null)
		{
			TextView tv=(TextView)instance.findViewById(R.id.credit);
	        if (tv!=null) tv.setText(String.format(instance.getString(R.string.credit), credit));
		}
	}
	
	@SuppressLint("InlinedApi")
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.purchase_page);
		
		mDensity = getResources().getDisplayMetrics().density;

		if (getIntent().getBooleanExtra("pushIn", false))
			this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
		else
			this.overridePendingTransition(R.anim.push_up_in, R.anim.freeze);
		mPref=new MyPreference(this);
		
		currency=new DQCurrency(this);
		
//	    numOfPackages=mPref.readInt("numOfPackagesNew",4);
	    numOfPackages = package_price.length;  //tml*** limit purchase
	    
	    for (int i=0;i<numOfPackages;i++)
	    {
	    	float price=mPref.readFloat("PackagePrice"+i,0);
	    	if (price==0) break;
	    	package_price[i]=price;
	    	package_name[i]=mPref.read("PackageName"+i);
	    }
	    
	    LinearLayout shelf=(LinearLayout)findViewById(R.id.packages);
	    LinearLayout hr=null;
	    for (int i=0;i<numOfPackages;i++)
	    {
	    	if ((i%2)==0)
	    	{
	    		hr=new LinearLayout(this);
	    		hr.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
	    		shelf.addView(hr);
	    	}
	    	LinearLayout ver=new LinearLayout(this);
	    	ver.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
	    
	    	LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,(int)(mDensity*80));
            
            a.bottomMargin=(int)(mDensity*5);
            a.topMargin=(int)(mDensity*5);
            a.leftMargin=(int)(mDensity*2);
            a.rightMargin=(int)(mDensity*2);
            a.weight=1;
            a.gravity=Gravity.CENTER;
            ver.setLayoutParams(a);
            ver.setOrientation(LinearLayout.VERTICAL);
            hr.addView(ver);
            
	    	TextView title=new TextView(this);
	    	title.setTextColor(0xffffffff);
	    	title.setTextSize(14);
	    	title.setLines(1);
	    	title.setText(package_name[i]);
	    	title.setGravity(Gravity.CENTER_HORIZONTAL);
	    	title.setShadowLayer(2, 0, 2, 0xd0000000);
	    	
	    	FlipImageButton btn=new FlipImageButton(this);

	    	float amount=package_price[i];
	    	
	    	btn.setBackgroundResource(R.drawable.packagebtn);
    		//btn.setImage(R.drawable.paypal);
    		btn.setPadding(0, 0, 0, 0);
    		
	    	if (currency.isUsingUSD())
	    		btn.setText(String.format("%1$s $%2$.2f\n~%3$s �%4$.2f", currency.translate("USD"),amount,
	    				currency.translate("EUR"),amount*currency.getEuroCurrency()));
	    	else{
	    		float rate=currency.getCurrencyFromUSD();
	    		if (rate>25)
	    			btn.setText(String.format("%1$s $%2$.2f\n~%3$s %4$s%5$.0f", currency.translate("USD"),amount,
		    	    		currency.translate(currency.getCurrencyCode()),currency.getCurrencySymbol(),amount*currency.getCurrencyFromUSD()));
	    		else
	    			btn.setText(String.format("%1$s $%2$.2f\n~%3$s %4$s%5$.2f", currency.translate("USD"),amount,
	    	    		currency.translate(currency.getCurrencyCode()),currency.getCurrencySymbol(),amount*currency.getCurrencyFromUSD()));
	    	}
     
            btn.setGravity(Gravity.CENTER);
            btn.setId(i);
    		btn.setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				FlipImageButton fb=(FlipImageButton)v;
    				fb.flip();
    				chosenIndex=v.getId();
    				mHandler.postDelayed(new Runnable(){
    					public void run()
    					{
    						onBuyPressed();
    					}
    				}, 500);
    			}
    		});
    		
    		ver.addView(title);
    		ver.addView(btn);
	    }
	    
	    float credit=mPref.readFloat("Credit",0);
        TextView tv=(TextView)findViewById(R.id.credit);
        if (tv!=null) tv.setText(String.format(getString(R.string.credit), credit));
        
        WebView webView=(WebView)findViewById(R.id.refound);
        String lang=Locale.getDefault().getLanguage();
        webView.loadUrl("http://www.airetalk.com/return_policy.php?d=and&l="+lang);
	    
	    instance=this;
	    
	    mHandler.post(initCountryList);
	}
	
	private Runnable initCountryList=new Runnable() {
		
		@Override
		public void run() {
			float c = currency.getCurrencyFromUSD();
			String symbol = currency.getCurrencySymbol();
			String code = currency.getCurrencyCode();

			SelectedClass = mPref.readInt("SelectedClass", 0);

			for (int i = 0; i < DQRates.Rates.length; i++) {
				try {
					Object aa = DQRates.Rates[i][SelectedClass + 1];
					String cc = String.valueOf(aa);
					float ff = Float.valueOf(cc);
					if (ff > 0) {
						Map<String, Object> map = new HashMap<String, Object>();

						String iso = (String) DQRates.Rates[i][0];
						String name = MyTelephony.getCountryNameByIso(iso,
								PurchaseActivity.this);
						float rate = ff * c;

						map.put("name", name);
						map.put("iso", iso);
						map.put("rate", String.format("%1$s %2$s%3$.2f", code,
								symbol, rate));
						countryList.add(map);
					}
				} catch (Exception e) {
				}
			}

			Collections.sort(countryList, new Comparator<Object>() {
				public int compare(Object o1, Object o2) {
					String s1 = (String) ((Map<String, Object>) o1).get("name");
					String s2 = (String) ((Map<String, Object>) o2).get("name");
					return s1.compareToIgnoreCase(s2);
				}
			});

			((ImageView) findViewById(R.id.cancel))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							finish();
						}
					});

			mList = (ListView) findViewById(R.id.rates);
			rateAdapter = new CountryRatesAdapter(PurchaseActivity.this,
					countryList);

			String lan=Locale.getDefault().getLanguage();
			String locale=Locale.getDefault().toString();
			if (locale.equals("zh_PRC"))
				lan="zh-Hans";
			else if (locale.equals("zh_TW"))
				lan="zh-Hant";
			
			String [] supported={"en", "zh-Hant", "zh-Hans", "es", "ar", "pt", "fr", "ja", "ko", "zh"};
			boolean found=false;
			for (String s: supported)
			{
				if (lan.equals(s))
				{
					found=true;
					break;
				}
			}
			if (!found) lan="en";
			
			((com.pingshow.amper.view.WebImageView) findViewById(R.id.banner)).setURL("img/banner-"+lan+".png");

			mList.setAdapter(rateAdapter);
			mList.setOnItemClickListener(onClickCountryListener);}
	};
	
	private Runnable dissmissProgress=new Runnable()
	{
		public void run()
		{
			if (progress!=null && progress.isShowing())
				progress.dismiss();
		}
	};
	
	/*
	private JSONObject confirmationJSON;
	private Runnable sendConfirmation=new Runnable()
	{
		public void run()
		{
			int count=0;
			
			String myPhoneNumber = mPref.read("myPhoneNumber", "----");
			String myPasswd = mPref.read("password", "1111");
			String android_id = Secure.getString(getContentResolver(), Secure.ANDROID_ID); 
			int myIdx=0;
			String pay_key="";
			String payment_id="";
			String app_id="";
			String amount="0";
			try{
				myIdx=Integer.parseInt(mPref.read("myID","0"),16);
				
				try{
					float Famount=Float.valueOf(confirmationJSON.getJSONObject("payment").getString("amount"));
					amount=String.format("%.2f", Famount);
				}catch(Exception e)
				{
					amount=confirmationJSON.getJSONObject("payment").getString("amount");
				}
				
				JSONObject proof=confirmationJSON.getJSONObject("proof_of_payment");
				
				JSONObject rest_api=null;
				JSONObject adaptive_payment=null;
				
				try{
					rest_api=proof.getJSONObject("rest_api");
				}catch(Exception e){}
				
				try{
					adaptive_payment=proof.getJSONObject("adaptive_payment");
				}catch(Exception e){}
				
				//paybyPayPal
				if (adaptive_payment!=null)
				{
					pay_key=adaptive_payment.getString("pay_key");
					app_id=adaptive_payment.getString("app_id");
				}
				else if (rest_api!=null){
					payment_id=rest_api.getString("payment_id");
				}
			}catch(Exception e){}
			
			
			TransactionDB mTDB=new TransactionDB(PurchaseActivity.this);
        	mTDB.open();
        	long rowid=mTDB.insert(package_name[chosenIndex], amount, payment_id, pay_key, app_id);
        	
			String Return="";
	        do {
	        	try{
	        		MyNet net = new MyNet(PurchaseActivity.this);
	        		
	        		if (pay_key.length()>0)
	        		{
						Return = net.doPostHttps(".paypal/paybypaypal.php", "idx="+myIdx
								+"&id=" + URLEncoder.encode(myPhoneNumber,"UTF-8")
								+"&password=" + URLEncoder.encode(myPasswd, "UTF-8")
								+"&imei=" + URLEncoder.encode(android_id, "UTF-8")
								+"&amount=" + URLEncoder.encode(amount, "UTF-8")
								+"&pay_key=" + URLEncoder.encode(pay_key, "UTF-8")
								+"&app_id=" + URLEncoder.encode(app_id, "UTF-8")
								,null);
	        		}
	        		else if (payment_id.length()>0)
	        		{
	        			Return = net.doPostHttps(".paypal/paybycard.php", "idx="+myIdx
								+"&id=" + URLEncoder.encode(myPhoneNumber,"UTF-8")
								+"&password=" + URLEncoder.encode(myPasswd, "UTF-8")
								+"&imei=" + URLEncoder.encode(android_id, "UTF-8")
								+"&amount=" + URLEncoder.encode(amount, "UTF-8")
								+"&payment_id=" + URLEncoder.encode(payment_id, "UTF-8")
								,null);
	        		}
	        	}catch(Exception e){
	        		Log.e("doPostHttps="+e.getMessage());
	        	}
				if (Return.length()>0)
					break;
				MyUtil.Sleep(2500);
			} while (++count < 3);
	        
	        Log.d(Return);
	        
	        mHandler.post(dissmissProgress);
	        
	        if (Return.startsWith("Done"))
	        {
	        	Intent it = new Intent(PurchaseActivity.this, CommonDialog.class);
				it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				String title=String.format(getString(R.string.thank_you_for_purchase), package_name[chosenIndex]);
				it.putExtra("msgContent", title);
				it.putExtra("numItems", 1);
				it.putExtra("ItemCaption0", getString(R.string.done));
				it.putExtra("ItemResult0", RESULT_OK);
				startActivity(it);
				
				mTDB.update(rowid, 1);
				
				Intent intent = new Intent(Global.Action_InternalCMD);
				intent.putExtra("Command", Global.CMD_UPDATE_SIP_CREDIT);
				PurchaseActivity.this.sendBroadcast(intent);
	        }
	        else if (Return.startsWith("Fail,NotApproved"))
	        {
	        	Intent it = new Intent(PurchaseActivity.this, CommonDialog.class);
				it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				String title=String.format(getString(R.string.thank_you_for_purchase), package_name[chosenIndex]);
				it.putExtra("msgContent", title);
				it.putExtra("numItems", 1);
				it.putExtra("ItemCaption0", getString(R.string.done));
				it.putExtra("ItemResult0", RESULT_OK);
				startActivity(it);
				
				Intent intent = new Intent(Global.Action_InternalCMD);
				intent.putExtra("Command", Global.CMD_CHECK_PAYPAL_AGAIN);
				PurchaseActivity.this.sendBroadcast(intent);
	        }
	        
	        mTDB.close();
		}
	};*/
	
	/*
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		if (requestCode == 0)
		{
		    if (resultCode == Activity.RESULT_OK) {
		        PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
		        if (confirm != null) {
		            try {
		            	confirmationJSON=confirm.toJSONObject();
		            	try{
							progress = ProgressDialog.show(PurchaseActivity.this, "", getString(R.string.in_progress), true, true);
						}catch(Exception e){}
						
		            	new Thread(sendConfirmation).start();
		            	
		            } catch (Exception e) {
		                Log.e("an extremely unlikely failure occurred: "+e.getMessage());
		            }
		        }
		    }
		    else if (resultCode == Activity.RESULT_CANCELED) {
		        Log.d("The user canceled.");
		    }
		    else if (resultCode == PaymentActivity.RESULT_PAYMENT_INVALID) {
		        Log.d("An invalid payment was submitted. Please see the docs.");
		    }
		}
	}*/
	
	@Override
	protected void onDestroy() {
		//stopService(new Intent(this, PayPalService.class));
		if (progress!=null && progress.isShowing())
			progress.dismiss();
		instance=null;
		super.onDestroy();
	}
	
	public void onBuyPressed() 
	{
		if (chosenIndex<0 || chosenIndex>=numOfPackages) chosenIndex=0;
		String myUsername = mPref.read("myPhoneNumber", "----");
		try {
			//tml*** limit purchase
			float credit = 0;
			if (AireJupiter.getInstance() != null) {
				credit = AireJupiter.getInstance().getCredit();
			} else {
				return;
			}
			float creditAllowed = (float) 20.0 - credit;  //limit 10, 20
			if (package_credit[chosenIndex] >= creditAllowed) {
				Log.d("credit allowed=" + creditAllowed + " credit want=" + package_credit[chosenIndex]);
				String errMsg = getString(R.string.max_credit);
				Intent it = new Intent(PurchaseActivity.this, CommonDialog.class);
				it.putExtra("msgContent", errMsg);
				it.putExtra("numItems", 1);
				it.putExtra("ItemCaption0", getString(R.string.done));
				it.putExtra("ItemResult0", RESULT_OK);
				startActivity(it);
				return;
			} else {
				Log.d("credit allowed=" + creditAllowed);
			}
			//***tml
			
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	OnItemClickListener onClickCountryListener=new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			
			Map<String, Object> map=null;
			map = countryList.get(position);
			
			String name=(String)map.get("name");
			String iso=(String)map.get("iso");
			String prefix=MyTelephony.getCountryPrefixByIso(iso);
			float fixedRate=DQRates.getFixedRateByIso(iso, SelectedClass);
			float mobileRate=DQRates.getMobileRateByIso(iso, SelectedClass);
			float c=1;
			String symbol=currency.getCurrencySymbol();
			String code=currency.getCurrencyCode();
			
			Intent it = new Intent(PurchaseActivity.this, CommonDialog.class);
			it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			int classes[]={R.string.standard_class,R.string.premium_class,R.string.business_class};
			String title="";
			
			if (currency.isUsingUSD())
			{
				c=currency.getEuroCurrency();
				title=String.format(getString(R.string.detail_rate),name,prefix,getString(classes[SelectedClass]),
					currency.translate("USD"), fixedRate, currency.translate("EUR"), "�", c*fixedRate,
					currency.translate("USD"), mobileRate, currency.translate("EUR"), "�", c*mobileRate);
			}else{
				c=currency.getCurrencyFromUSD();
				title=String.format(getString(R.string.detail_rate),name,prefix,getString(classes[SelectedClass]),
						currency.translate("USD"), fixedRate, code, symbol, c*fixedRate,
						currency.translate("USD"), mobileRate, code, symbol, c*mobileRate);
			}
			it.putExtra("longContent", true);
			it.putExtra("msgContent", title);
			it.putExtra("numItems", 1);
			it.putExtra("ItemCaption0", getString(R.string.done));
			it.putExtra("ItemResult0", RESULT_OK);
			startActivity(it);
		}
	};
}
