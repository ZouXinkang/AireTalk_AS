package com.pingshow.airecenter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import com.pingshow.util.ImageUtil;

public class ConfirmDialog extends Activity {
	
	int [] Results;
	static public final int DELETE_THIS_USER=100;
	static public final int BLOCK_THIS_USER=102;
	static public final int UNBLOCK_THIS_USER=104;
	
	private TextView msgcontent;
	private Intent intent;
	private ImageView user_photo;
	private TextView user_name;
	
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.confirm_dialog);
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
    		btn.setTypeface(null, Typeface.BOLD);
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
            btn.setText(Captions[i]);
            btn.setId(i);
    		
            buttonlist.addView(btn);
    		
    		btn.setOnClickListener(new OnClickListener() {
	    		public void onClick(View v) {
	    			if (DELETE_THIS_USER==Results[v.getId()])
	    			{
	    				Intent intent = new Intent(Global.Action_UserPage_Command);
	    				intent.putExtra("Command",50);
	    				sendBroadcast(intent);
	    			}
	    			else if (BLOCK_THIS_USER==Results[v.getId()])
	    			{
	    				Intent intent = new Intent(Global.Action_UserPage_Command);
	    				intent.putExtra("Command",60);
	    				sendBroadcast(intent);
	    			}
	    			else if (UNBLOCK_THIS_USER==Results[v.getId()])
	    			{
	    				Intent intent = new Intent(Global.Action_UserPage_Command);
	    				intent.putExtra("Command",40);
	    				sendBroadcast(intent);
	    			}
	    			setResult(Results[v.getId()]);
	    			finish();
	    		}}
	    	);
        }
	}
}