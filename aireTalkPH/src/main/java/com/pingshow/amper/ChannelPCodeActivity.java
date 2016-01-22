package com.pingshow.amper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ChannelPCodeActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.channel_pcode_dialog);
	    
	    WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.3f;
		getWindow().setAttributes(lp);
		
		String storedPcode=getIntent().getStringExtra("storedPcode");
		if (storedPcode!=null)
		{
			((EditText)findViewById(R.id.pcode)).setText(storedPcode);
		}
		
	    Button button = (Button)findViewById(R.id.done);
        button.setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			EditText pco = (EditText)findViewById(R.id.pcode);
    			if("".equals(pco.getText().toString().trim())) return;
    			
    			String pcode=pco.getText().toString();
    			
    			Intent it=new Intent();
    			it.putExtra("pcode_input", pcode);
    			
    			InputMethodManager imm = (InputMethodManager)getSystemService(
    				      Context.INPUT_METHOD_SERVICE);
    			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    			
    			setResult(RESULT_OK, it);
				finish();
    		}}
        );
        
        button = (Button)findViewById(R.id.cancel);
        button.setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
				finish();
    		}}
        );
	}
}