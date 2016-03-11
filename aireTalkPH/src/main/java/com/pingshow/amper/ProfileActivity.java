package com.pingshow.amper;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.pingshow.util.ImageUtil;
import com.pingshow.util.ResizeImage;
import com.pingshow.voip.AireVenus;

public class ProfileActivity extends Activity {
	private MyPreference mPref;
	private static final int TAKEPHOTO = 20;
	private static final int PHOTO_SIZE = 320;  //720
	private Uri uriOrig=null;
	private String photoPath = null;
	private ImageView photoView;
	private boolean photoChanged;
	private Handler mHandler=new Handler();
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.e("*** !!! PROFILE *** START START !!! ***");
        mPref=new MyPreference(this);
		//jack first login

		mPref.write("firstEnter", true);
        if (mPref.readBoolean("ProfileCompleted",false))
        {
        	Intent intent = new Intent();
        	int lastpage=mPref.readInt("LastPage");
			if (lastpage==1)
				intent.setClass(ProfileActivity.this, MessageActivity.class);
			else if (lastpage==2)
				intent.setClass(ProfileActivity.this, PublicWalkieTalkie.class);
			else if (lastpage==3)
				intent.setClass(ProfileActivity.this, SettingActivity.class);
			else if (lastpage==4)
				intent.setClass(ProfileActivity.this, SipCallActivity.class);
			else
				intent.setClass(ProfileActivity.this, UsersActivity.class);
			startActivity(intent);
			finish();
			return;
        }
        setContentView(R.layout.profile_page);
        this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        photoView = (ImageView)findViewById(R.id.photo);
        photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        photoView.setOnClickListener(new OnClickListener() {
	    	@Override
	    	public void onClick(View v) {
	    		onPickPictureOption();
	    	}
	    });

        if (mPref.readBoolean("LoginByFacebook")||mPref.readBoolean("LoginByWeibo"))
        {
	        String nickname;
	        if((nickname=mPref.read("myNickname"))!=null)
	        	((EditText)findViewById(R.id.nickname)).setText(nickname);
	        photoPath=Global.SdcardPath_sent + mPref.read("myPhoneNumber") + ".jpg";
//	        Drawable photo=ImageUtil.getBitmapAsRoundCorner(photoPath, 1, 15);
	        //xwf*** circle pic
			Bitmap photo = ImageUtil.getCircleBitmapPath(photoPath, 3, 10);
			photoView.setImageBitmap(photo);
	        if (photo!=null)
	        {
		        ((TextView)findViewById(R.id.my_photo_hint)).setVisibility(View.GONE);
//		        photoView.setImageDrawable(photo);
		        photoView.setImageBitmap(photo);  //xwf*** circle pic
		        photoChanged=true;
	        }
        }
        
        String gender=mPref.read("myGender",null);
        if (gender!=null)
        {
        	((RadioButton)findViewById(R.id.male)).setChecked(gender.equals("male")?true:false);
        	((RadioButton)findViewById(R.id.male)).setEnabled(false);
        	((RadioButton)findViewById(R.id.female)).setEnabled(false);
        }
        else
        	((RadioButton)findViewById(R.id.male)).setChecked(true);
        
        ((RadioButton)findViewById(R.id.male)).setOnCheckedChangeListener(new OnCheckedChangeListener(){
        	@Override
        	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        		mPref.write("myGender", isChecked?"male":"female");
			}
        });
        
        ((CheckBox)findViewById(R.id.read_contact)).setOnCheckedChangeListener(new OnCheckedChangeListener(){
        	@Override
        	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        		mPref.write("permissionReadContacts", isChecked);
			}
        });
        
        ((Button)findViewById(R.id.done)).setOnClickListener(new OnClickListener(){
        	@Override
			public void onClick(View v) {
        		
        		if (mPref.readBoolean("LoginByFacebook"))
        		{
	        		if (!mPref.readBoolean("fbSearched",false))
	        		{
	        			mPref.write("fbSearched",true);
	        			mHandler.post(doSearchFacebookFriends);
	        			return;
	        		}
        		}
        		
        		finishProfile.run();
			}
        });
        
//        if (!photoChanged || photoPath==null)
//        	mHandler.postDelayed(showTooltip, 1000);
	}
	
	Runnable finishProfile=new Runnable()
	{
		public void run()
		{
			if (photoChanged && photoPath!=null)
    		{
    			mPref.write("myPhotoPath", photoPath);
    			mPref.write("myPhotoUploaded", false);
    			Intent it = new Intent(Global.Action_InternalCMD);
    			it.putExtra("Command", Global.CMD_UPLOAD_PROFILE_PHOTO);
    			sendBroadcast(it);
    		}
    		
    		boolean male=((RadioButton)findViewById(R.id.male)).isChecked();
    		
    		String nickname=((EditText)findViewById(R.id.nickname)).getText().toString();
    		if (nickname!=null)
    		{
        		nickname=nickname.trim();
        		boolean chinese=nickname.toLowerCase().equals(nickname.toUpperCase());
        		
        		if (nickname.length()>=(chinese?2:6))
        		{
        			mPref.write("ProfileCompleted", true);
        			mPref.write("myNickname", nickname);
        			mPref.write("myGender",male?"male":"female");
        			
					Intent it = new Intent(Global.Action_InternalCMD);
					it.putExtra("Command", Global.CMD_UPDATE_MY_NICKNAME);
					sendBroadcast(it);
					
        			Intent intent = new Intent(ProfileActivity.this, UsersActivity.class);
        			startActivity(intent);
        			finish();
        		}
        		else
        		{
        	    	Intent int2 = new Intent(getApplicationContext(), CommonDialog.class);
        	    	int2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
        	    			| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        	    			| Intent.FLAG_ACTIVITY_SINGLE_TOP);
        	    	int2.putExtra("msgContent", getString(R.string.nickname_invalid));
        	    	int2.putExtra("numItems", 1);
        	    	int2.putExtra("ItemCaption0", getString(R.string.done));
        	    	int2.putExtra("ItemResult0", -1);
        	    	startActivity(int2);
        		}
    		}
		}
	};
	
	Runnable showTooltip=new Runnable(){
		public void run()
		{
			Intent it=new Intent(ProfileActivity.this,Tooltip.class);
	        it.putExtra("Content", getString(R.string.help_complete_profile));
		    startActivity(it);
		}
	};
	
	Runnable doSearchFacebookFriends=new Runnable()
	{
		public void run()
		{
			Intent it=new Intent(ProfileActivity.this, FacebookSearch.class);
			startActivityForResult(it, 70);
		}
	};
	
	@Override
	protected void onPause() {
    	mHandler.removeCallbacks(showTooltip);
//    	MobclickAgent.onPause(this);
    	super.onPause();
    }
	
	private void onPickPictureOption()
	{
		final CharSequence[] items = {
				getResources().getString(R.string.photo_gallery),
				getResources().getString(R.string.takepicture)};
		final CharSequence[] items_noCamera = {
				getResources().getString(R.string.photo_gallery)};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (!AmazonKindle.canHandleCameraIntent(this)){
			builder.setItems(items_noCamera, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
					if (item == 0) onPickPicture();
					dialog.dismiss();
				}
			});
		}
		else{
			builder.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
					if (item == 0)
						onPickPicture();
					else if (item == 1)
						onTakePicture();
					dialog.dismiss();
				}
			});
		}
		
		builder.setTitle(this.getResources().getString(R.string.choose_photo_source)); 
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {     
			public void onClick(DialogInterface dialog, int item) {         
				
			} 
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void onPickPicture()
	{
//		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		Intent intent = new Intent(Intent.ACTION_PICK);  //yang*** profpic fix
		intent.setType("image/*");
		
		if (AmazonKindle.IsKindle())
		{
			String title = getResources().getString( R.string.choose_photo_source);
			startActivityForResult(Intent.createChooser(intent, title), 16);
			return;
		}
		
		intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
//        intent.putExtra("outputX",PHOTO_SIZE+PHOTO_SIZE);
//        intent.putExtra("outputY",PHOTO_SIZE+PHOTO_SIZE);
		intent.putExtra("outputX", PHOTO_SIZE);  //yang*** profpic fix
		intent.putExtra("outputY", PHOTO_SIZE);
        intent.putExtra("return-data", true);
		//li*** fill picture
		intent.putExtra("scale", true);
		intent.putExtra("scaleUpIfNeeded", true);
        startActivityForResult(intent, 1);
	}
	
	public String getPath(Uri uri) {
		if(uri.toString().startsWith("content:")){
			try{
				String[] projection = { MediaStore.Images.Media.DATA }; 
				Cursor cursor = managedQuery(uri, projection, null, null, null); 
				int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA); 
				if (cursor!=null)
				{
					cursor.moveToFirst(); 
					String path=cursor.getString(column_index);
					return path;
				}
			}catch(Exception e){}
		}else if(uri.toString().startsWith("file:")){
			String uriStr = uri.toString();
			return uriStr.substring(uriStr.indexOf("sdcard"));
		}
		return "";
	}
	
	private void onTakePicture()
	{
		if (!AmazonKindle.canHandleCameraIntent(this)){
			Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
			return;
		}
		try{
			if (AmazonKindle.IsKindle())
			{
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				photoPath = Global.SdcardPath_sent + "tmp.jpg";
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(photoPath)));
				startActivityForResult(intent, 8);
			}else{
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				photoPath=Global.SdcardPath_sent + "tmp.jpg";
				intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(photoPath)));
				startActivityForResult(intent, TAKEPHOTO);
			}
		}catch(Exception e){
			Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) { 
	    if (resultCode == RESULT_OK) {
	    	if (requestCode == 16)
	    	{		
				try {
					Bitmap bitmap=null;
					try {
						Uri selectedImageUri = data.getData(); // URI of the photo
						bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri); 
					}catch (Exception e) {
					}
					
					if (bitmap!=null)
					{
						int uid = Integer.valueOf(mPref.read("myID", "0"), 16);
						String outFilename = Global.SdcardPath_sent + "myself_photo_" + uid + ".jpg";
						ResizeImage.ResizeBitmapXY(this, bitmap, outFilename, PHOTO_SIZE, 100);
						
						photoPath = outFilename;
						
//						Drawable photo = ImageUtil.getBitmapAsRoundCorner(photoPath, 3, 10);// alec
						//xwf*** circle pic
						Bitmap photo = ImageUtil.getCircleBitmapPath(photoPath, 3, 10);
						photoView.setImageBitmap(photo);
						if (photo != null) {
							TextView hint = (TextView) findViewById(R.id.my_photo_hint);
							hint.setVisibility(View.GONE);
//							photoView.setImageDrawable(photo);
							photoView.setImageBitmap(photo);  //xwf*** circle pic
							photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
							photoChanged = true;
						}
					}
				} catch (Exception e) {
				}
	    	}
	    	else if (requestCode == 8) {
	    		try{
	    			int uid = Integer.valueOf(mPref.read("myID","0"), 16);
		        	String outFilename=Global.SdcardPath_sent +"myself_photo_"+uid+".jpg";
		        	ResizeImage.ResizeXY(this, photoPath, outFilename, PHOTO_SIZE, 100);
		        	
		        	String outFilename2 = Global.SdcardPath_inbox + "photo_" + uid + "b.jpg";
					ResizeImage.ResizeXY(this, photoPath, outFilename2, PHOTO_SIZE, 100);
		        	
		        	photoPath=outFilename;
		        	
//		        	Drawable photo = ImageUtil.getBitmapAsRoundCorner(outFilename, 3, 10);// alec
		        	Bitmap photo = ImageUtil.getCircleBitmapPath(photoPath, 3, 10);  //xwf*** circle pic
					if (photo!=null){
		        		((TextView)findViewById(R.id.my_photo_hint)).setVisibility(View.GONE);
//			        	photoView.setImageDrawable(photo);
						photoView.setImageBitmap(photo);  //xwf*** circle pic
			        	photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
			        	photoChanged=true;
		        	}
	    		}catch(Exception e){}
	        	
	    	}else if(requestCode == TAKEPHOTO){
	    		boolean HDSize=false;
		    	try{
					BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
					bitmapOptions.inJustDecodeBounds=true;
					bitmapOptions.inPurgeable=true;
					BitmapFactory.decodeFile(photoPath, bitmapOptions);
					if (bitmapOptions.outHeight>2000)
						HDSize=true;
		    	}catch(Exception e){
		    	}catch(OutOfMemoryError e){
		    	}
				if (HDSize)
				{
					Bitmap bmp=ImageUtil.loadBitmapSafe(2, photoPath);
					try {
						uriOrig = Uri.parse(MediaStore.Images.Media.insertImage(
								getContentResolver(), bmp, null, null));
					} catch (Exception e) {}
				}else{
					try {
						uriOrig = Uri.parse(MediaStore.Images.Media.insertImage(
								getContentResolver(), photoPath, null, null));
					} catch (Exception e) {}
					catch(OutOfMemoryError e){}
				}
				startActivityForResult(getCropImageIntent(uriOrig), 3);
	    	}else if (requestCode == 7) {
//				Drawable photo = ImageUtil.getBitmapAsRoundCorner(photoPath, 3, 10);// alec
	        	Bitmap photo = ImageUtil.getCircleBitmapPath(photoPath, 3, 10);  //xwf*** circle pic
				if (photo!=null){
	        		((TextView)findViewById(R.id.my_photo_hint)).setVisibility(View.GONE);
//		        	photoView.setImageDrawable(photo);
					photoView.setImageBitmap(photo);  //xwf*** circle pic
		        	photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		        	photoChanged=true;
	        	}
	    	}else if(requestCode == 1 || requestCode == 3){
	    		if (data==null) return;
		    	String SrcImagePath="";
		    	try { 
		    		Uri uri=null;
		    		Bitmap bitmap = data.getParcelableExtra("data");
//					uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, null,null));
		    		//li*** profpic fix
		    		String uriString = MediaStore.Images.Media.insertImage(
							getContentResolver(), bitmap, null, null);
		    		uri = Uri.parse(uriString);
					SrcImagePath = getPath(uri);
		    	
		        	int uid = Integer.valueOf(mPref.read("myID","0"), 16);
		        	String outFilename=Global.SdcardPath_sent +"myself_photo_"+uid+".jpg";
		        	ResizeImage.ResizeXY(this, SrcImagePath, outFilename, PHOTO_SIZE, 100);
		        	
		        	photoPath = outFilename;
		        	
		        	if (uriOrig!=null) getContentResolver().delete(uriOrig, null, null);
		        	getContentResolver().delete(uri, null, null);
		        	
		        	if (requestCode==3)//taken from camera
					{
						Intent it = new Intent(ProfileActivity.this, PictureRotationActivity.class);
						startActivityForResult(it, 7);
					}else{
//						Drawable photo = ImageUtil.getBitmapAsRoundCorner(outFilename, 3, 10);// alec
			        	Bitmap photo = ImageUtil.getCircleBitmapPath(photoPath, 3, 10);  //xwf*** circle pic
						if (photo!=null){
			        		((TextView)findViewById(R.id.my_photo_hint)).setVisibility(View.GONE);
//				        	photoView.setImageDrawable(photo);
							photoView.setImageBitmap(photo);  //xwf*** circle pic
				        	photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
				        	photoChanged=true;
			        	}
					}
	        	
		    	} catch (Exception e) {}
		    }else if (requestCode == 70)
		    {
		    	mHandler.post(finishProfile);
		    }
	    }
	}
	

	public static Intent getCropImageIntent(Uri photoUri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(photoUri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("aspectX", 1);
     	intent.putExtra("aspectY", 1);
     	intent.putExtra("outputX", PHOTO_SIZE+PHOTO_SIZE);
     	intent.putExtra("outputY", PHOTO_SIZE+PHOTO_SIZE);
     	intent.putExtra("return-data", true);
     	return intent;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
//		MobclickAgent.onResume(this);
	}
}
