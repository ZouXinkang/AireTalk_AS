package com.pingshow.airecenter;

import java.util.Date;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import com.pingshow.airecenter.R;

public class FeedbackActivity extends Activity {
	
	private Handler mHandler=new Handler();
	private MyPreference mPref;
	private RadioButton rb1 = null;
	private RadioButton rb2 = null;
	private RadioButton rb3 = null;
	private RadioButton rb4 = null;
	private RadioButton rb5 = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feedback_1);
		this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
		
		rb1 = (RadioButton) findViewById(R.id.sugg_ilove_rb1);
		rb2 = (RadioButton) findViewById(R.id.sugg_ilove_rb2);
		rb3 = (RadioButton) findViewById(R.id.sugg_ilove_rb3);
		rb4 = (RadioButton) findViewById(R.id.sugg_ilove_rb4);
		rb5 = (RadioButton) findViewById(R.id.sugg_ilove_rb5);
		
		mPref = new MyPreference(FeedbackActivity.this);
		long last=mPref.readLong("feedback_has_been_delivered",0);
		if (new Date().getTime()-last<3600000)
		{
			((Button)findViewById(R.id.next)).setEnabled(false);
			mHandler.postDelayed(feedbackDelivered,1000);
		}
		
		((Button)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		((Button)findViewById(R.id.next)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int level = 0;
				
				if (rb1.isChecked()) {
					level = 5;
				}
				else if (rb2.isChecked()) {
					level = 4;
				}
				else if (rb3.isChecked()) {
					level = 3;
				}
				else if (rb4.isChecked()) {
					level = 2;
				}
				else if (rb5.isChecked()) {
					level = 1;
				}
				if (level==0) return;

				mPref.write("level", level);
				Intent intent = new Intent(FeedbackActivity.this, FeedbackActivity2.class);
				startActivityForResult(intent, 100);
			}
		});
	}
	
	Runnable feedbackDelivered=new Runnable(){
		public void run(){
			Intent it2 = new Intent(FeedbackActivity.this, CommonDialog.class);
			it2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			String title=getString(R.string.feedback_has_been_sent);
			it2.putExtra("msgContent", title);
			it2.putExtra("numItems", 1);
			it2.putExtra("ItemCaption0", getString(R.string.OK));
			it2.putExtra("ItemResult0", 0);
			startActivity(it2);
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == 100){
			if (resultCode==RESULT_OK)
				this.finish();
		}
	}
}
