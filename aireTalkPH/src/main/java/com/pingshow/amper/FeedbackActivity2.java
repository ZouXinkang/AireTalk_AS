package com.pingshow.amper;

import java.net.URLEncoder;
import java.util.Date;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.pingshow.network.MyNet;
import com.pingshow.util.MyUtil;

public class FeedbackActivity2 extends Activity {
	private MyPreference mPref = null;
	String suggestion="";
	String demerits="";
	String serialCode="";

	Handler handler = new Handler();
	
	Runnable processSuccess=new Runnable(){
		public void run(){
			Intent it = new Intent(Global.Action_InternalCMD);
			long now = new Date().getTime() / 1000;
			it.putExtra("Command", Global.CMD_TCP_MESSAGE_ARRIVAL);
			it.putExtra("originalSignal","210/2/" + Integer.toHexString((int) now) + "/<Z>"
					+ getResources().getString(R.string.suggCustomer) + serialCode);
			sendBroadcast(it);
			setResult(RESULT_OK);
			finish();
		}
	};
	
	Runnable processFalied=new Runnable(){
		public void run(){
			Intent it2 = new Intent(FeedbackActivity2.this, CommonDialog.class);
			it2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			String title=getString(R.string.smsfail);
			it2.putExtra("msgContent", title);
			it2.putExtra("numItems", 1);
			it2.putExtra("ItemCaption0", getString(R.string.OK));
			it2.putExtra("ItemResult0", 0);
			startActivity(it2);
			setResult(RESULT_OK);
			finish();
		}
	};
	
	private ProgressDialog progressDialog;
	
	Runnable popupProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				if (progressDialog==null)
					progressDialog = ProgressDialog.show(FeedbackActivity2.this, "", getString(R.string.in_progress), true, true);
			}catch(Exception e){}
		}
	};
	
	Runnable dismissProgress=new Runnable() {
		@Override
		public void run() {
			try{
				if (progressDialog != null && progressDialog.isShowing())
					progressDialog.dismiss();
				if (progressDialog != null) progressDialog.cancel();
				progressDialog = null;
			}catch(Exception e){
				progressDialog = null;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feedback_2);
		this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
		
		mPref = new MyPreference(this);
		
		((Button)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		((Button) findViewById(R.id.next)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if (((CheckBox) findViewById(R.id.sugg_checkbox1)).isChecked()) {
					demerits += "audio ";
				}
				if (((CheckBox) findViewById(R.id.sugg_checkbox2)).isChecked()) {
					demerits += "video ";
				}
				if (((CheckBox) findViewById(R.id.sugg_checkbox3)).isChecked()) {
					demerits += "msg ";
				}
				if (((CheckBox) findViewById(R.id.sugg_checkbox4)).isChecked()) {
					demerits += "friend ";
				}
				if (((CheckBox) findViewById(R.id.sugg_checkbox5)).isChecked()) {
					demerits += "crash";
				}
				
				((Button) findViewById(R.id.next)).setEnabled(false);
				setData();
			}
		});
	}

	private void setData() {
		handler.post(popupProgressDialog);
		new Thread(new Runnable(){
			
			public void run()
			{
				int versionCode=216;
				try {
					versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
				} catch (NameNotFoundException e) {}
				String myPhoneNumber = mPref.read("myPhoneNumber", "");
				String myPasswd = mPref.read("password", "1111");
				int level = mPref.readInt("level", 5);
				
				try {
					suggestion=((EditText)findViewById(R.id.suggsay_text)).getText().toString();
					int count = 0;
					String Return="";
					do {
						MyNet net = new MyNet(FeedbackActivity2.this);
						Return = net.doPostHttps("feedback.php", "id=" + URLEncoder.encode(myPhoneNumber, "UTF-8")
								+ "&password=" + URLEncoder.encode(myPasswd, "UTF-8")
								+ "&level=" + level
								+ "&demerits=" + demerits
								+ "&version=" + versionCode 
								+ "&comment=" + URLEncoder.encode(suggestion, "UTF-8"), null);
						if (Return.startsWith("Done=")) break;
						MyUtil.Sleep(1500);
					} while (count++ < 3);
					
					if (Return.startsWith("Done=")){
						serialCode=Return.substring(5);
						mPref.writeLong("feedback_has_been_delivered", new Date().getTime());
						handler.postDelayed(processSuccess,3000);
						handler.postDelayed(dismissProgress,2000);
					}
					else{
						handler.post(dismissProgress);
						handler.post(processFalied);
					}
				} catch (Exception e) {
				}
			}
		}).start();
	}
}
