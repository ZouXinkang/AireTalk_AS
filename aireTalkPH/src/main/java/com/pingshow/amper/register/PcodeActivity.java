package com.pingshow.amper.register;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.pingshow.amper.R;

public class PcodeActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.pcode_dialog);
	    
	    WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.75f;
		getWindow().setAttributes(lp);
	    
	    Button button = (Button)findViewById(R.id.done);
        button.setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			EditText pco = (EditText)findViewById(R.id.pcode);
    			if("".equals(pco.getText().toString().trim())) return;
    			
    			int pcode=Integer.parseInt(pco.getText().toString());
    			
    			Intent it=new Intent();
    			it.putExtra("pcode_input", pcode);
    			
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