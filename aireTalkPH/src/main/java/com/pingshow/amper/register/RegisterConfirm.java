package com.pingshow.amper.register;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.pingshow.amper.MyPreference;
import com.pingshow.amper.R;

public class RegisterConfirm extends Activity {
	
	String globalNumber;
	boolean phoneNumberReadable=false;
	EditText edit_passwd = null;
	EditText edit_username = null;
	MyPreference mPref;
	
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.register_confirm);
	    
	    mPref=new MyPreference(this);
	    
	    String displayNumber=getIntent().getStringExtra("msgContent");
	    
	    TextView tv=(TextView)findViewById(R.id.msgcontent);
	    tv.setText(displayNumber);
	    
	    WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.75f;
		getWindow().setAttributes(lp);
	    
	    Button button = (Button)findViewById(R.id.done);
        button.setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			setResult(RESULT_OK);
    			finish();
    		}}
        );
        
        button = (Button)findViewById(R.id.cancel);
        button.setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			setResult(RESULT_CANCELED);
    			finish();
    		}}
        );
	}
}