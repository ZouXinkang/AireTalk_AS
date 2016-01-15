package com.pingshow.airecenter;

import java.io.File;
import java.util.Date;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.util.ImageUtil;
import com.pingshow.voip.core.Version;

public class CommonDialog extends Activity {
	
	public static int DOWNLOAD=999;
	public static int GOOGLEPLAY=990;
	public static int SHARING=777;
	public static int SEARCHFACEBOOK=888;
	public static int DONTSEARCHFACEBOOK=889;
	public static int INVITATION=666;
	public static int SETNET = 555;
	public static int ADD_NEW_USER = 747;
	
	public static int AGREE_DOWNFRIENDS=222;
	public static int NOTAGREE_DOWNFRIENDS=111;
	
	int [] Results;
	private int relation = 1;
	
	private TextView msgcontent;
	private Intent intent;
	private ImageView user_photo;
	private TextView user_name;
	
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.common_dialog);
	    user_photo=(ImageView)findViewById(R.id.user_photo);
	    user_name=(TextView)findViewById(R.id.user_name);
	    msgcontent=(TextView)findViewById(R.id.msgcontent);
	    	    
	    WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.5f;
		getWindow().setAttributes(lp);
		
		float mDensity=getResources().getDisplayMetrics().density;
	    intent=getIntent();
	    if (intent!=null) {
			String mAddress = intent.getExtras().getString("Address");
			String nickname = intent.getExtras().getString("Nickname");
			int idX = intent.getExtras().getInt("Idx");
			if (mAddress!=null && nickname!=null && idX>0)
			{
				user_name.setText(nickname);
				Drawable photo=ImageUtil.getUserPhoto(this, idX);
				if (photo != null)
					user_photo.setImageDrawable(photo);
				else {
					if (mAddress.startsWith("[<GROUP>]"))
						user_photo.setImageResource(R.drawable.group_empty);
					else
						user_photo.setImageResource(R.drawable.bighead);
				}
			}
			else{
				user_photo.setVisibility(View.GONE);
				user_name.setVisibility(View.GONE);
				((LinearLayout)findViewById(R.id.header)).setVisibility(View.GONE);
			}
		}
	    msgcontent.setText(getIntent().getStringExtra("msgContent"));
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
    		
    		btn.setTextColor(0xffffffff);
            btn.setTextSize(26);
            LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,(int)(mDensity*80));
            a.bottomMargin=(int)(mDensity*8);
            a.topMargin=(int)(mDensity*8);
            a.leftMargin=(int)(mDensity*12);
            a.rightMargin=(int)(mDensity*12);
            a.weight=1;
            
            btn.setGravity(Gravity.CENTER);
            btn.setLayoutParams(a);
            btn.setMinimumWidth((int)(mDensity*190));
            btn.setTypeface(Typeface.DEFAULT_BOLD);
            btn.setText(Captions[i]);
            btn.setId(i);
    		
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
	    				Uri uri = Uri.parse("market://details?id=com.pingshow.airecenter");  
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
	    			else if (Results[v.getId()]==ADD_NEW_USER)
	    			{
	    				Log.i("tmlf ADD_NEW_USER broadcast>> Action_SearchPage_Adding");
	    				Intent it = new Intent(Global.Action_SearchPage_Adding);
	    				sendBroadcast(it);
	    			}
	    			finish();
	    		}}
	    	);
        }
	}
}