package com.pingshow.airecenter;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.ResizeImage;

public class GroupDialogActivity extends Activity {

	private String groupName;
	private ArrayList<String> chosenList;
	private AmpUserDB mADB;
	private AsyncImageLoader asyncImageLoader;
	private LinearLayout verticalView;
	private float mDensity = 1.f;
	
	private ImageView photoView;
	private String photoPath;
	private boolean photoAssigned;
	
	private static final int PHOTO_SIZE = 720;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.groupdialog);
		
		WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
	    mDensity = getResources().getDisplayMetrics().density;
	    lp.width=(int)(720.f*mDensity);
		lp.dimAmount = 0.3f;
		getWindow().setAttributes(lp);
		
		mADB = new AmpUserDB(this);
		mADB.open();
		
		asyncImageLoader = new AsyncImageLoader(getApplicationContext());
		
		chosenList = (ArrayList<String>) getIntent().getExtras().getSerializable("chosenList");
		groupName = getIntent().getStringExtra("groupname");
		if (groupName!=null)
			((TextView) findViewById(R.id.group_name)).setText(groupName);

		verticalView=(LinearLayout)findViewById(R.id.members);
		RelativeLayout h=null;
		if (chosenList != null) {
			for (int i = 0; i < chosenList.size(); i++) 
			{
				if (i==0 || i==5 || i==10)
				{
					h=new RelativeLayout(this);
					h.setPadding(0, 5, 0, 5);
					verticalView.addView(h,LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				}
				String IDX = chosenList.get(i);
				int idx=Integer.parseInt(IDX);
				
				ImageView photo=new ImageView(this);
				photo.setId(i*2+1);
				RelativeLayout.LayoutParams rp=new RelativeLayout.LayoutParams((int)(mDensity*60),(int)(mDensity*60));
				if ((i%5)>0) rp.addRule(RelativeLayout.RIGHT_OF, i*2+1-2);
				else rp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
					
				rp.leftMargin=(int)(mDensity*25);
				h.addView(photo,rp);
				
				String userphotoPath= Global.SdcardPath_inbox + "photo_" + idx + "b.jpg";
				if (!new File(userphotoPath).exists()) {
					userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
					if (!new File(userphotoPath).exists())
						userphotoPath = null;
				}
				photo.setTag(userphotoPath);
				
				Drawable cachedImage = asyncImageLoader.loadDrawable(userphotoPath,
						new ImageCallback() {
							public void imageLoaded(Drawable imageDrawable, String path) {
								ImageView imageViewByTag = (ImageView) verticalView.findViewWithTag(path);	
								if (imageViewByTag != null && imageDrawable!=null) {
									imageViewByTag.setImageDrawable(imageDrawable);
								}
							}
						});
	
				if (cachedImage != null && userphotoPath != null)
					photo.setImageDrawable(cachedImage);
				else {
					photo.setImageResource(R.drawable.bighead);
				}
				
				TextView name=new TextView(this);
				name.setText(mADB.getNicknameByIdx(idx));
				name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
				name.setTextColor(0xff202030);
				name.setId(i*2);
				name.setMaxHeight((int)(mDensity*34));
				name.setGravity(Gravity.CENTER);
				RelativeLayout.LayoutParams rp2=new RelativeLayout.LayoutParams((int)(mDensity*60), LayoutParams.WRAP_CONTENT);
				rp2.addRule(RelativeLayout.BELOW, i*2+1);
				rp2.addRule(RelativeLayout.ALIGN_LEFT, i*2+1);
				h.addView(name,rp2);
			}
			
			verticalView.recomputeViewAttributes(h);
		}
		

		((Button) findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});

		((Button) findViewById(R.id.creategroup)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Global.Action_Create_Group);
				intent.putExtra("photoAssigned",photoAssigned);
				intent.putExtra("groupPhotoPath",photoPath);
				sendBroadcast(intent);
				finish();
			}
		});
		
		photoView = (ImageView)findViewById(R.id.photo);
        photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		photoView.setOnClickListener(new OnClickListener() {
	    	@Override
	    	public void onClick(View v) {
	    		onPickPictureOption();
	    	}
	    });
		
		
		String lastPhotoFile=Global.SdcardPath_sent +"temp_group.jpg";
		if (new File(lastPhotoFile).exists())
		{
			Drawable photo = ImageUtil.loadBitmapSafe(lastPhotoFile, 3);// alec
			if (photo != null) {
				((TextView)findViewById(R.id.my_photo_hint)).setVisibility(View.GONE);
				photoView.setImageDrawable(photo);
				photoAssigned = true;
				photoPath = lastPhotoFile;
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mADB != null && mADB.isOpen())
			mADB.close();
	}
	
	private void onPickPictureOption()
	{
		final CharSequence[] items = {
				getResources().getString(R.string.photo_gallery),
				getResources().getString(R.string.takepicture)};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(this.getResources().getString(R.string.choose_photo_source)); 
		builder.setItems(items, new DialogInterface.OnClickListener() {     
			public void onClick(DialogInterface dialog, int item) {         
				if (item==0)
					onPickPicture();
				else if (item==1)
					onTakePicture();
				dialog.dismiss();
			} 
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {     
			public void onClick(DialogInterface dialog, int item) {         
				
			} 
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void onPickPicture()
	{
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
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
		try{
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			photoPath=Global.SdcardPath_sent + "tmp.jpg";
			intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(photoPath)));
			startActivityForResult(intent, 3);
		}catch(Exception e){
			Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) { 
	    if (requestCode == 1 || requestCode == 3 || requestCode == 7) 
		{
			if (resultCode == Activity.RESULT_OK) {
				if (requestCode == 1) {
					if (data == null) return;
					try {
						String outFilename=Global.SdcardPath_sent +"temp_group.jpg";
						
						ResizeImage.ResizeXYFromStream(this, data, outFilename, PHOTO_SIZE, 100);

						Drawable photo = ImageUtil.loadBitmapSafe(outFilename, 1);// alec
						if (photo != null) {
							((TextView)findViewById(R.id.my_photo_hint)).setVisibility(View.GONE);
							photoView.setImageDrawable(photo);
							photoAssigned = true;
							photoPath = outFilename;
						}

					} catch (Exception e) {
					}
				}
				else if (requestCode == 3) {				
					try {
						String outFilename=Global.SdcardPath_sent +"temp_group.jpg";
						
						ResizeImage.ResizeXY(this, photoPath, outFilename, PHOTO_SIZE, 100);
						
						Drawable photo = ImageUtil.loadBitmapSafe(outFilename, 1);// alec
						if (photo != null) {
							((TextView)findViewById(R.id.my_photo_hint)).setVisibility(View.GONE);
							photoView.setImageDrawable(photo);
							photoAssigned = true;
						}
					} catch (Exception e) {
					}
				}
			}
		}
//	    Log.e("tmlg requestCode=" + requestCode + " resultCode=" + resultCode + " photoAssigned=" + photoAssigned);
	}
}
