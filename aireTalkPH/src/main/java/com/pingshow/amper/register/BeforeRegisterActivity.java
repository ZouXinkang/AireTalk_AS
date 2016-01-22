package com.pingshow.amper.register;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.CommonDialog;
import com.pingshow.amper.Global;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.ProfileActivity;
import com.pingshow.amper.R;
import com.pingshow.util.MyUtil;

public class BeforeRegisterActivity extends Activity {
	private MyPreference mPref;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
		setContentView(R.layout.before_register_page);
        this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        ((Button) findViewById(R.id.register)).setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
    			//tml*** airplane mode
    			boolean airplaneMode = MyUtil.isAirplaneModeOn(BeforeRegisterActivity.this);
    			if (airplaneMode) {
    				String errMsg = getString(R.string.airplane_mode);
    				Intent it = new Intent(BeforeRegisterActivity.this, CommonDialog.class);
    				it.putExtra("msgContent", errMsg);
    				it.putExtra("numItems", 1);
    				it.putExtra("ItemCaption0", getString(R.string.done));
    				it.putExtra("ItemResult0", RESULT_OK);
    				startActivity(it);
    				return;
    			}
				startActivityForResult(new Intent(BeforeRegisterActivity.this, RegisterActivity.class), 10);
			}
		});
        
        ((Button)findViewById(R.id.login)).setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		startActivityForResult(new Intent(BeforeRegisterActivity.this, LoginActivity.class), 20);
			}
		});
        
        ((Button)findViewById(R.id.facebook_login)).setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		startActivityForResult(new Intent(BeforeRegisterActivity.this, FacebookLoginDialog.class), 30);
			}
		});
        
        ((Button)findViewById(R.id.sina_login)).setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		startActivityForResult(new Intent(BeforeRegisterActivity.this, WeiboLoginDialog.class), 40);
        	}
        });
        
        mPref=new MyPreference(this);
        
        if (!MyUtil.isAppInstalled(this,"com.facebook.katana"))
        {
        	((Button)findViewById(R.id.facebook_login)).setVisibility(View.GONE);
        	((Button)findViewById(R.id.sina_login)).setVisibility(View.GONE);
        }
        else if (mPref.read("iso","tw").equals("cn"))
        {
        	((Button)findViewById(R.id.facebook_login)).setVisibility(View.GONE);
        	((Button)findViewById(R.id.sina_login)).setVisibility(View.VISIBLE);
        }
        else
        {
        	((Button)findViewById(R.id.facebook_login)).setVisibility(View.VISIBLE);
        	((Button)findViewById(R.id.sina_login)).setVisibility(View.GONE);
        }
        
        mPref.readBoolean("myPhotoUploaded", true);
        
        String versionName="1.0.0";
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {}
    	((TextView)findViewById(R.id.version)).setText("Version: " + versionName);
        
        try {
			new File(Global.SdcardPath).mkdir();
			new File(Global.SdcardPath_inbox).mkdir();
			new File(Global.SdcardPath_sent).mkdir();
			new File(Global.SdcardPath_downloads).mkdir();
		} catch (Exception e) {
			Toast.makeText(this, R.string.no_sdcard, Toast.LENGTH_LONG).show();
		}
        
        new Thread(parseServerIP).start();
	}
	
	Runnable parseServerIP=new Runnable()
	{
		public void run()
		{
			try {
				String domainName = "php.xingfafa.com.cn";
				String iso = mPref.read("iso", "cn");
				if (!iso.equals("cn"))
					domainName = "php.airetalk.org";
				AireJupiter.myPhpServer = InetAddress.getByName(domainName).getHostAddress();
				Log.d("myPhpServer=" + AireJupiter.myPhpServer);
			} catch (UnknownHostException e) {
				AireJupiter.myPhpServer = AireJupiter.myPhpServer_default;
			}
		}
	};
	@Override  
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		super.onActivityResult(requestCode, resultCode, data);  
		if (requestCode==10||requestCode==20||requestCode==30||requestCode==40)
		{
			if (resultCode==RESULT_OK) {
				Intent intent = new Intent(this, ProfileActivity.class);
				startActivity(intent);
				startServiceX();
				finish();
			}
		}
	}
	
	void startServiceX()
	{
		if (!MyUtil.CheckServiceExists(BeforeRegisterActivity.this, "com.pingshow.amper.AireJupiter"))//Start ServiceX
        {
    		Intent x=new Intent(BeforeRegisterActivity.this, AireJupiter.class);
    		startService(x);
    	}
	}

}
