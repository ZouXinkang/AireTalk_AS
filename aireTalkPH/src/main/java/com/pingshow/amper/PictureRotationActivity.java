package com.pingshow.amper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import com.pingshow.util.ImageUtil;

public class PictureRotationActivity extends Activity {
	
	private MyPreference mPref;
	private String photopath;
	static private boolean changed=false;
	static private Bitmap photoBmp;
	
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.pic_rotation);
	    
	    mPref = new MyPreference(this);
	    photopath=getIntent().getStringExtra("photoPath");
	    
	    if (photopath==null)
	    {
		    int uid = Integer.valueOf(mPref.read("myID", "0"), 16);
		    photopath = Global.SdcardPath_sent + "myself_photo_" + uid + ".jpg";
	    }
        
	    if (photoBmp==null) photoBmp=ImageUtil.loadBitmapSafe(1, photopath);
        if (photoBmp!=null) ((ImageView)findViewById(R.id.photo)).setImageBitmap(photoBmp);
        
        ((Button)findViewById(R.id.done)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			if (changed)
    			{
	    			try{
	    	            File myCaptureFile = new File(photopath);
	    	            BufferedOutputStream bos = new BufferedOutputStream(
	    	            	new FileOutputStream(myCaptureFile));
	    	            photoBmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
	    	            bos.flush();
	    	            bos.close();
	    	        }catch(Exception e){}
	    	        catch (OutOfMemoryError e) {}
    			}
    			
    			photoBmp=null;
    			changed=false;
    	        
    			setResult(RESULT_OK);
				finish();
    		}}
        );
        
        ((Button)findViewById(R.id.rotateLeft)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			Matrix mat = new Matrix();
    			mat.preRotate(-90);
//    			photoBmp = Bitmap.createBitmap(photoBmp, 0, 0, 240, 240, mat, true);
    			//yang*** profpic fix
    			photoBmp = Bitmap.createBitmap(photoBmp, 0, 0,
    					SettingActivity.PHOTO_SIZE, SettingActivity.PHOTO_SIZE, mat, true);
    			if (photoBmp!=null)
    				((ImageView)findViewById(R.id.photo)).setImageBitmap(photoBmp);
    			changed=true;
    		}}
        );
        ((Button)findViewById(R.id.rotateRight)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			Matrix mat = new Matrix();
    			mat.preRotate(90);
//    			photoBmp = Bitmap.createBitmap(photoBmp, 0, 0, 240, 240, mat, true);
    			//yang*** profpic fix
    			photoBmp = Bitmap.createBitmap(photoBmp, 0, 0,
    					SettingActivity.PHOTO_SIZE, SettingActivity.PHOTO_SIZE, mat, true);
    			if (photoBmp!=null)
    				((ImageView)findViewById(R.id.photo)).setImageBitmap(photoBmp);
    			changed=true;
    		}}
        );
	}
}