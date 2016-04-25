package com.pingshow.amper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.amper.db.TimeLineDB;
import com.pingshow.network.MyNet;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;
import com.pingshow.util.ResizeImage;

public class TimeLineCompose extends Activity {
	
	private MyPreference mPref;
	private TimeLineDB mTL;

	public Cursor mCursor;
	
	private String mDisplayname;
	private String mAddress;
	private int mIdx;
	private int myIdx;
	private Handler mHandler=new Handler();
	private ArrayList<String> attachedList = new ArrayList<String>();
	private ArrayList<String> uploadedList = new ArrayList<String>();
	private ArrayList<String> thumbnailList = new ArrayList<String>();
	
	private String mContent="";
	private boolean didSend=false;
	private boolean didAttach=false;
	private float mDensity = 1.f;
	private String takePhotoPath;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timeline_compose);
        mPref=new MyPreference(this);
        
        mDensity = getResources().getDisplayMetrics().density;
        
        mTL = new TimeLineDB(this);
        mTL.open();
        
        mIdx=getIntent().getIntExtra("Idx", 0);
        mDisplayname=getIntent().getStringExtra("displayname");
        
        try{
        	myIdx=Integer.parseInt(mPref.read("myID","0"),16);
        }catch(Exception e){
        	myIdx=mIdx;
        }
        
        ((TextView)findViewById(R.id.sendee)).setText(mDisplayname);
        
        ((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
        
        ((ImageView)findViewById(R.id.attachment)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onPickPictureOption();
			}
		});
        
        ((Button)findViewById(R.id.sendmsg)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText edit = ((EditText)findViewById(R.id.content));
				mContent=((EditText)findViewById(R.id.content)).getText().toString();
				if (mContent.length()>0 || didAttach)
				{
					mHandler.post(popupProgressDialog);
					new Thread(doPostTimeline).start();
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
				}
			}
		});
	}
	
	ProgressDialog progress;
	Runnable popupProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				progress = ProgressDialog.show(TimeLineCompose.this, "", getString(R.string.in_progress), true, false);
			}catch(Exception e){}
		}
	};
	
	Runnable dismissProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				if (progress!=null)
				{
					if (progress.isShowing()){
						progress.dismiss();
					}
				}
			}catch(Exception e){}
		}
	};
	
	Runnable doPostTimeline=new Runnable()
	{
		public void run()
		{
			didSend=true;
			uploadedList.clear();
			String localPhpServer = AireJupiter.myLocalPhpServer;
			String Return="";
			int c=0;
			for (String filePath: attachedList)
			{
				do{
					try{
						MyNet net=new MyNet(TimeLineCompose.this);
						Log.d("Uploading "+filePath+" to "+localPhpServer);
						Return=net.doPostAttach("timelineupload.php", 0, 0, filePath, localPhpServer);
					}catch(Exception e){}
					Log.d("Uploading Return="+Return);
					
					if(Return.startsWith("Done"))
					{
						String ufile=Return.substring(5);
						uploadedList.add(ufile);
						if (ufile.endsWith(".mp4") || ufile.endsWith(".mp3"))
						{
							if (ufile.endsWith(".mp4"))
								saveVideoThumbnail(TimeLineCompose.this, filePath, Global.SdcardPath_timeline+ufile, mDensity);
							else if (ufile.endsWith(".mp3"))
								saveMp3AlbumPicture(TimeLineCompose.this, filePath, Global.SdcardPath_timeline+ufile);
							
							//alec: a small thumbnail file
							try{
								MyNet net=new MyNet(TimeLineCompose.this);
								String localTN=(ufile.substring(0, ufile.lastIndexOf(".")))+".jpg";
								MyUtil.copyFile(Global.SdcardPath_timeline+ufile, Global.SdcardPath_timeline+localTN, true);
								Return=net.doPostAttachFixed("timelinethumbnail.php",Global.SdcardPath_timeline+localTN, localTN, localPhpServer);
							}catch(Exception e){}
						}
						else 
							MyUtil.copyFile(filePath, Global.SdcardPath_timeline+ufile, true);
						break;
					}
					MyUtil.Sleep(1500);
				}while(++c<3);
			}
			
			c=0;
			Return="";
			String attaches="";
			for (String filename: uploadedList)
			{
				if (attaches.length()>0)
//					attaches+="<Z>"+filename;
					attaches += "<P>" + filename;  //li*** multi timepost fix
				else
					attaches=filename;
			}
			
			String myNickname = mPref.read("myNickname", "");
			
			do{
				try{
					MyNet net = new MyNet(TimeLineCompose.this);
					Return = net.doPost("timelinepost.php","id="+mIdx+
							"&writer="+myIdx+
							"&name="+URLEncoder.encode(myNickname,"UTF-8")+
							"&text="+URLEncoder.encode(mContent,"UTF-8")+
							"&pms=0"+
							"&srvr="+URLEncoder.encode(localPhpServer,"UTF-8")+
							"&attaches="+URLEncoder.encode(attaches,"UTF-8")
							,null);
				}catch(Exception e){}
				if (Return.length()>5) break;
				MyUtil.Sleep(500);
			}while(c++<3);
			
			if (Return.startsWith("Done="))
			{
				int post_id=Integer.parseInt(Return.substring(5));
				String attached=null;
				if (uploadedList.size()>0)
				{
					attached="";
					for (String filePath : uploadedList)
					{
						if (attached.length()>0)
//							attached+="<Z>"+filePath;
							attached += "<P>" + filePath;  //li*** multi timepost fix
						else
							attached=filePath;
					}
				}
				mTL.insert(post_id, mIdx, mIdx, myIdx, myNickname, 0, new Date().getTime(), mContent, attached, null, localPhpServer, 0, 0);
				mHandler.postDelayed(new Runnable(){
					public void run()
					{
						if (TimeLine.getInstance()!=null)
							TimeLine.getInstance().refresh();
						setResult(RESULT_OK);
						finish();
					}
				}, 500);
			}
			
			mHandler.post(dismissProgressDialog);
		}
	};
	
	@Override
	protected void onDestroy() {
		if (mTL != null && mTL.isOpen())
			mTL.close();
		if (!didSend)
		{
			for (String filePath: attachedList)
			{
				if (filePath.endsWith(".jpg") && filePath.contains(Global.SdcardPath_timeline))
				{
					File f=new File(filePath);
					f.delete();
				}
			}
		}
		for (String filePath: thumbnailList)
		{
			if (filePath.endsWith(".jpg") && filePath.contains(Global.SdcardPath_timeline))
			{
				File f=new File(filePath);
				f.delete();
			}
		}
		System.gc();
		System.gc();
		super.onDestroy();
	}
	
	private void onPickPicture() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_photo_source)), 1);
	}
	
	private void onPickVideo() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("video/*");
		startActivityForResult(Intent.createChooser(intent, getString(R.string.chose_file)), 101);
	}
	
	private void onPickMusic (){
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("audio/mpeg3");
		startActivityForResult(Intent.createChooser(intent, getString(R.string.chose_file)), 103);
	}
	
	private Uri outputFileUri;
	
	private void onTakePicture() {
		if (!AmazonKindle.canHandleCameraIntent(this)){
			Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
			return;
		}
		try{
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//			takePhotoPath = Global.SdcardPath_sent + "tmp.jpg";
//			intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
//					Uri.fromFile(new File(takePhotoPath)));
			startActivityForResult(intent, 3);
		}catch(Exception e){
			Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT).show();
		}
	}
	
	public String getPath(Uri uri) {
		if (uri.toString().startsWith("content:")) {
			String[] projection = { MediaStore.Images.Media.DATA };
			Cursor cursor = managedQuery(uri, projection, null, null, null);
			int column_index = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			String path = cursor.getString(column_index);
			return path;
		} else if (uri.toString().startsWith("file:")) {
			String uriStr = uri.toString();
			return uriStr.substring(uriStr.indexOf("sdcard"));
		}
		return "";
	}
	
	private void onPickPictureOption() {
		final CharSequence[] items = {
				getResources().getString(R.string.photo_gallery),
				getResources().getString(R.string.takepicture),
				getResources().getString(R.string.videomemo),
				getResources().getString(R.string.music)
				};
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
					else if (item == 2)
						onPickVideo();
					else if (item == 3)
						onPickMusic();
					dialog.dismiss();
				}
			});
		}
		builder.setTitle(getResources().getString(R.string.choose_photo_source));
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}
	

	private void arrangeThumbnails()
	{
		LinearLayout att=(LinearLayout)findViewById(R.id.attached);
		att.removeAllViews();
		for (String path:thumbnailList)
		{
			ImageView thumb=new ImageView(this);
			Drawable draw=null;
			if (path.endsWith(".jpg") && path.contains(Global.SdcardPath_timeline))
				draw=ImageUtil.loadBitmapSafe(path, 1);
			else if (path.endsWith(".mp4"))
				draw=getVideoThumbnail(this, path, mDensity);
			else if (path.endsWith(".mp3"))
				draw=getMp3AlbumPicture(this, path);
			thumb.setScaleType(ScaleType.CENTER_CROP);
			thumb.setImageDrawable(draw);
			thumb.setPadding(5, 5, 5, 5);
			LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams((int)(60.f*mDensity), (int)(60.f*mDensity));
			att.addView(thumb, lp);
		}
		att.setVisibility(View.VISIBLE);
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1 || requestCode == 3){
			if (resultCode == RESULT_OK) {
//				if (null==data.getData()) return;
				String filename = Global.SdcardPath_timeline + ConversationActivity.getRandomName() + ".jpg";
//				Uri uri = null;
//				try {
//					uri = Uri.parse(MediaStore.Images.Media
//							.insertImage(getContentResolver(), takePhotoPath,
//									null, null));
//				} catch (FileNotFoundException e) {
//					e.printStackTrace();
//				}
//				data.setData(uri);
				android.util.Log.d("TimeLineCompose", "requestCode " + requestCode + "resultCode " + resultCode + "----" + data);
				int result = ResizeImage.saveFromStream(this, data, filename, 1600, 1600, 95);
				String thumbnail = Global.SdcardPath_timeline + "thumb_" + ConversationActivity.getRandomName() + "s.jpg";
				ResizeImage.saveFromStream(this, data, thumbnail, 320, 320, 80);  //tml*** bitmap quality, 120>320, 50>80
				if (result != -1) {
					boolean found = false;
					for (String filePath : attachedList) {
						if (filePath.endsWith(".mp3") || filePath.endsWith(".mp4")) {
							found = true;
							break;
						}
					}

					if (found) {
						attachedList.clear();
						thumbnailList.clear();
					}

					attachedList.add(filename);
					thumbnailList.add(thumbnail);
					arrangeThumbnails();

					didAttach = true;
				}
			}
		}else if (requestCode == 101){
			if (resultCode == RESULT_OK) {
				attachedList.clear();
				thumbnailList.clear();
				Uri selectedUri = data.getData();
				String filePath = getPath(selectedUri).toString();
				if (filePath!=null)
				{
					attachedList.add(filePath);
					thumbnailList.add(filePath);
					arrangeThumbnails();
					
					didAttach=true;
				}
			}
		}else if (requestCode == 103){
			if (resultCode == RESULT_OK) {
				attachedList.clear();
				thumbnailList.clear();
				Uri selectedUri = data.getData();
				String filePath = getPath(selectedUri).toString();
				if (filePath!=null)
				{
					attachedList.add(filePath);
					thumbnailList.add(filePath);
					arrangeThumbnails();
					
					didAttach=true;
				}
			}
		}
	}
	
	public static Drawable getVideoThumbnail(Context context, String filePath, float density) {
		if (filePath == null)
			return null;
		Bitmap videobitmap=null;
		if(Integer.parseInt(Build.VERSION.SDK) >= 8)
			videobitmap = ThumbnailUtils.createVideoThumbnail(new File(filePath).getAbsolutePath(), Video.Thumbnails.MICRO_KIND);
		if (videobitmap == null)
			videobitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.novideo);
		
		return new BitmapDrawable(videobitmap);
	}
	
	public static void saveVideoThumbnail(Context context, String filePath, String thumbnailPath, float density) {
		Bitmap videobitmap=null;
		if(Integer.parseInt(Build.VERSION.SDK) >= 8)
			videobitmap = ThumbnailUtils.createVideoThumbnail(new File(filePath).getAbsolutePath(), Video.Thumbnails.MINI_KIND);
		if (videobitmap == null)
			videobitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.novideo);
		
		Bitmap play=BitmapFactory.decodeResource(context.getResources(), R.drawable.start_play);
		Bitmap thumb=ImageUtil.combineImages(context, videobitmap, play, true);
		try {
			File file = new File(thumbnailPath);
			FileOutputStream outStream = new FileOutputStream(file);
			thumb.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
		    outStream.flush();
		    outStream.close();
		} catch (Exception e) {
		}
	}
	
	public Drawable getMp3AlbumPicture(Context context, String filePath) {
		Bitmap videobitmap=null;
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		retriever.setDataSource(filePath);
		/*
		try{
			String albumName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
			String artistName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
			
			mContent=((EditText)findViewById(R.id.content)).getText().toString();
			if (mContent==null) mContent="";
			mContent += artistName + "\n" + albumName;
			((EditText)findViewById(R.id.content)).setText(mContent);
		}catch(Exception e){}*/
			
		try{
			Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
	                MediaStore.MediaColumns.DATA + "=?",
	                new String[] { filePath }, null);
			String title=null;
			if(c!=null && c.moveToFirst())
				title = c.getString(c.getColumnIndex(MediaStore.MediaColumns.TITLE));
			
			mContent=((EditText)findViewById(R.id.content)).getText().toString();
			if (mContent==null) mContent="";
			if (title!=null)
			{
				mContent += title;
				((EditText)findViewById(R.id.content)).setText(mContent);
				c.close();
			}
		}catch(Exception e){}
		
		byte[] art = retriever.getEmbeddedPicture();

		if( art != null )
			videobitmap=BitmapFactory.decodeByteArray(art, 0, art.length);
		else
			videobitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.noalbum);

		return new BitmapDrawable(videobitmap);
	}
	
	public void saveMp3AlbumPicture(Context context, String filePath, String thumbnailPath) {
		if (filePath == null) return;
		Bitmap videobitmap=null;
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		retriever.setDataSource(filePath);
		byte[] art = retriever.getEmbeddedPicture();

		if( art != null )
			videobitmap=BitmapFactory.decodeByteArray(art, 0, art.length);
		else
			videobitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.noalbum);
		
		Bitmap play=BitmapFactory.decodeResource(context.getResources(), R.drawable.play);
		Bitmap thumb=ImageUtil.combineImages(context, videobitmap, play, false);
		try {
			File file = new File(thumbnailPath);
			FileOutputStream outStream = new FileOutputStream(file);
			thumb.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
		    outStream.flush();
		    outStream.close();
		} catch (Exception e) {}
	}
}
