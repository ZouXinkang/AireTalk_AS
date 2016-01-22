package com.pingshow.amper.register;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;

import com.pingshow.amper.R;

public class EulaDialog extends Activity {
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    setContentView(R.layout.eula);
	    this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
	    
		String destURL="/data/data/com.pingshow.amper/files/eula.html";
		
		WebView eula_view = (WebView) findViewById(R.id.eula);
		eula_view.getSettings().setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
		eula_view.getSettings().setBuiltInZoomControls(false);
		eula_view.getSettings().setUseWideViewPort(true);
		
		eula_view.loadUrl("file://"+destURL);
		
	    Button button = (Button)findViewById(R.id.next);
        button.setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			CheckBox eula_check_view = (CheckBox) findViewById(R.id.eula_check);
				Intent it=new Intent();
				it.putExtra("agreement", eula_check_view.isChecked()?1:0);
				setResult(RESULT_OK, it);
				finish();
    		}}
        );
        
        Intent it=new Intent();
		it.putExtra("agreement", 0);
		setResult(RESULT_CANCELED, it);
	}
}
