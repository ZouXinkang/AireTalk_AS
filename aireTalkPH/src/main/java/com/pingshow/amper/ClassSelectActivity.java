package com.pingshow.amper;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.pingshow.amper.view.FlipToggleView;
import com.pingshow.amper.view.FlipToggleView.clickCallback;
import com.pingshow.network.MyNet;
import com.pingshow.util.MyUtil;

public class ClassSelectActivity extends Activity{

	int SelectedClass=0;
	int PreviousClass=0;
	private MyPreference mPref;
	
	public FlipToggleView btn[]=new FlipToggleView[3];

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.class_select_page);
		this.overridePendingTransition(R.anim.push_up_in, R.anim.freeze);
		
		mPref=new MyPreference(this);
		PreviousClass=SelectedClass=mPref.readInt("SelectedClass",0);
		
		onToggleListener onToggle=new onToggleListener();
		
		btn[0]=(FlipToggleView)findViewById(R.id.standard);
		btn[1]=(FlipToggleView)findViewById(R.id.premium);
		btn[2]=(FlipToggleView)findViewById(R.id.business);
		
		btn[0].init(0, getString(R.string.standard_class), R.drawable.standard, onToggle);
		btn[1].init(1, getString(R.string.premium_class), R.drawable.premium, onToggle);
		btn[2].init(2, getString(R.string.business_class), R.drawable.business, onToggle);
		
		for (int i=0;i<3;i++)
			btn[i].setChecked((i==SelectedClass));
		
		((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			finish();
    		}}
    	);
	}
	
	@Override
	protected void onDestroy() {
		if (SelectedClass!=PreviousClass)
		{
			mPref.write("SelectedClass", SelectedClass);
			new Thread(new Runnable(){
				public void run()
				{
					int c=0;
					int myIdx=0;
					try{
						myIdx=Integer.parseInt(mPref.read("myID","0"),16);
					
						String Return="";
						do{
							MyNet net = new MyNet(getApplicationContext());
							Return = net.doPost("selectclass.php","idx="+myIdx+
									"&class="+SelectedClass,null);
							if (Return.length()>5) break;
							MyUtil.Sleep(500);
						}while(c++<3);
					}catch(Exception e){}
				}
			}).start();
		}
		super.onDestroy();
	}

	class onToggleListener implements clickCallback {
		@Override
		public void onSelect(int index) {
			for (int i=0;i<3;i++)
			{
				if (index==i) {
					SelectedClass=i;
					continue;
				}
				btn[i].setChecked(false);
			}
		}
    }
}
