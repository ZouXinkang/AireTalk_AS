package com.pingshow.amper;

import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.pingshow.voip.AireVenus;
import com.pingshow.voip.core.Version;

public class CommonDialog extends Activity {
	
	public static int DOWNLOAD=999;
	public static int GOOGLEPLAY=990;
	public static int CHINAPLAY=993;
	public static int SHARING=777;
	public static int SEARCHFACEBOOK=888;
	public static int DONTSEARCHFACEBOOK=889;
	public static int INVITATION=666;
	public static int SETNET = 555;
	
	public static int AGREE_DOWNFRIENDS=222;
	public static int NOTAGREE_DOWNFRIENDS=111;
	
	public static int STOP_SUV = 333;
	
	int [] Results;
	private int relation = 1;
	
	private boolean largeScreen=false;
	
	private TextView msgcontent;
	private Intent intent;
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.common_dialog);
	    
	    msgcontent=(TextView)findViewById(R.id.msgcontent);
	    
	    largeScreen=(findViewById(R.id.large)!=null);
	    	    
	    WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.5f;
		getWindow().setAttributes(lp);
		
		float mDensity=getResources().getDisplayMetrics().density;
	    intent=getIntent();
	    
	    if (intent.getBooleanExtra("longContent", false))
	    {
	    	msgcontent.setLines(13);
	    	msgcontent.setTextSize(16);
	    	msgcontent.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
	    }
	    
	    msgcontent.setText(intent.getStringExtra("msgContent"));
	    
	    int numItems=intent.getIntExtra("numItems", 0);
	    String [] Captions=new String[numItems];
	    Results=new int[numItems];
	    for(int i=0;i<numItems;i++)
	    {
	    	Captions[i]=intent.getStringExtra("ItemCaption"+i);
	    	Results[i]=intent.getIntExtra("ItemResult"+i,-1);
	    }
	    
		LinearLayout buttonlist=(LinearLayout)findViewById(R.id.buttons);
        
        for(int i=0;i<numItems;i++)
        {
    		Button btn=new Button(this);
            
    		btn.setBackgroundResource(R.drawable.plainbtn);
    		btn.setShadowLayer(2, 0, 2, 0xd0000000);
    		
    		btn.setTextColor(0xffffffff);
            btn.setTextSize(largeScreen?22:18);
            LayoutParams a=new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,(int)(mDensity*(largeScreen?60:44)));
            if (largeScreen)
            {
	            a.bottomMargin=(int)(mDensity*8);
	            a.topMargin=(int)(mDensity*8);
	            a.leftMargin=(int)(mDensity*12);
	            a.rightMargin=(int)(mDensity*12);
            }else{
            	a.bottomMargin=(int)(mDensity*8);
	            a.topMargin=(int)(mDensity*8);
	            a.leftMargin=(int)(mDensity*4);
	            a.rightMargin=(int)(mDensity*4);
            }
            a.weight=1;
            btn.setGravity(Gravity.CENTER);
            btn.setLayoutParams(a);
            btn.setText(Captions[i]);
            btn.setId(i);
            if (numItems>1 && i==numItems-1)
            {
            	btn.setFocusableInTouchMode(true);
            	btn.requestFocus();
            }
    		
            buttonlist.addView(btn);
    		
    		btn.setOnClickListener(new OnClickListener() {
	    		public void onClick(View v) {
	    			setResult(Results[v.getId()]);
	    			if (Results[v.getId()]==DOWNLOAD)
	    			{
	    				Intent it = new Intent(Global.Action_InternalCMD);
	    				it.putExtra("Command", Global.CMD_ONLINE_UPDATE);
	    				sendBroadcast(it);
	    			}
	    			else if (Results[v.getId()]==GOOGLEPLAY)
	    			{
	    				Uri uri = Uri.parse("market://details?id=com.pingshow.amper");
	    				Intent it = new Intent(Intent.ACTION_VIEW, uri);  
	    				startActivity(it);
	    			}
	    			else if (Results[v.getId()]==CHINAPLAY)  //tml*** alt update
	    			{
	    				Uri uri = Uri.parse("market://details?id=com.pingshow.amper");  
	    				Intent it = new Intent(Intent.ACTION_VIEW, uri);  
	    				startActivity(it);
	    			}
	    			else if (Results[v.getId()]==SETNET)
	    			{
	    				if (Version.sdkAboveOrEqual(13))
	    					startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); 
	    				else
	    					startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)); 
	    			}
	    			else if (Results[v.getId()]==SHARING)
	    			{
	    				Intent it = new Intent(Global.Action_InternalCMD);
	    				it.putExtra("Command", Global.CMD_SHARING_AGREE);
	    				it.putExtra("relation", relation);
	    				sendBroadcast(it);
	    			}
	    			else if (Results[v.getId()]==SEARCHFACEBOOK)
	    			{
	    				Intent it=new Intent(CommonDialog.this, FacebookSearch.class);
	    				startActivity(it);
	    			}
	    			else if (Results[v.getId()]==DONTSEARCHFACEBOOK)
	    			{
	    				MyPreference mPref = new MyPreference(CommonDialog.this);
	    				mPref.writeLong("facebookFriendsSynchronized", new Date().getTime()+432000000);
	    			}
	    			finish();
	    		}}
	    	);
        }
	}
	
}