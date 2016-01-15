package com.pingshow.airecenter.register;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.util.MyTelephony;

public class RetrievePwdActivity extends Activity {
	
	String globalNumber;
	boolean phoneNumberReadable=false;
	EditText edit_username = null;
	MyPreference mPref;
	
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.retrieve_password);
	    
	    mPref=new MyPreference(this);
	    
	    edit_username = (EditText)findViewById(R.id.username);
	    
	    edit_username.setText(mPref.read("tmpUsername", ""));
	    
	    phoneNumberReadable=getIntent().getBooleanExtra("phoneNumberReadable", false);
	    
	    globalNumber=getIntent().getStringExtra("globalNumber");
	    	
	    if (phoneNumberReadable==false)
        {
	    	edit_username.setVisibility(View.VISIBLE);
    		edit_username.setHint(R.string.findpwd);
    		edit_username.setSelection(edit_username.getText().toString().length());
        }
	    
	    WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.5f;
		getWindow().setAttributes(lp);
	    
	    Button button = (Button)findViewById(R.id.done);
        button.setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			String username;
    			if (phoneNumberReadable)
    				username=globalNumber;
    			else
    				username=MyTelephony.cleanPhoneNumber(edit_username.getText().toString().toLowerCase());
    			if (username.length()>=6)
    			{
	    			Intent it=new Intent();
	    			it.putExtra("username", username);
	    			
	    			mPref.write("tmpUsername", username);
	    			setResult(RESULT_OK, it);
					finish();
    			}
    		}}
        );
        
        edit_username.setOnFocusChangeListener(new OnFocusChangeListener()
		{
			//@Override
			public void onFocusChange(View v, boolean hasFocus) 
			{
				if(!hasFocus)
				{
					edit_username.setText(edit_username.getText().toString().toLowerCase());
				}
			}
		});
	}
}