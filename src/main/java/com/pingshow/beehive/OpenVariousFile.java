package com.pingshow.beehive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.pingshow.airecenter.FilePushActivity;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;

public class OpenVariousFile {
	private Context mContext = null;
	private RandomAccessFile rf;
	private Intent intent;
	private boolean ready=false;
	private long cachedSize=0;
	private long lastCachedSize=0;
	private int mType;
	private int mAssignedName;
	private int INITIALSIZE;
	private String tempFilename;
	boolean alreadyExists=false;
	
	public OpenVariousFile(Context context, int type, int assignedName){
		mContext = context;
		mType = type;
		mAssignedName = assignedName;
		
		lastCachedSize = 0;
		
		switch(mType)
		{
		case 1://audio
			INITIALSIZE=400000;
			break;
		case 2://video
			INITIALSIZE=3200000;
			break;
		case 3://image
			INITIALSIZE=5000;
			break;
		default:
			INITIALSIZE=2000;
			break;
		}
	}
	
	public void openFile() 
    {
		tempFilename=Global.SdcardPath_temp+mAssignedName+".dat";
		//yang?
		//tempFilename="/mnt/sdcard/"+mAssignedName+".dat";
		alreadyExists=new File(tempFilename).exists();

		if (!alreadyExists)
		{
			try {
				rf=new RandomAccessFile(tempFilename,"rw");
			}
			catch(Exception e){}
		}
		
		if (FilePushActivity.getInstance()==null)
		{
			intent=new Intent(mContext, FilePushActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra("mimeType", mType);
			intent.putExtra("filePath", tempFilename);
			intent.putExtra("alreadyExists", alreadyExists);
			mContext.startActivity(intent);
		}
		else{
			intent=new Intent();
			intent.putExtra("mimeType", mType);
			intent.putExtra("filePath", tempFilename);
			intent.putExtra("alreadyExists", alreadyExists);
			FilePushActivity.getInstance().setup(intent);
		}
		
		ready=true;
    }
	
	public boolean isReady()
	{
		return ready;
	}
	
	public void finalizeFile(boolean replay)
	{
		try{
			if (!alreadyExists)
			{
				rf.close();
				Log.i("closed");
				ClientThread.Sleep(50);
				Log.i("closed done");
			}
			Log.d("FilePushActivity.getInstance()="+FilePushActivity.getInstance());
			if (replay && FilePushActivity.getInstance()!=null)
			{
				Log.d(" FilePushActivity finalize");
				FilePushActivity.getInstance().finalizeFile();
			}
		}catch(Exception e){}
	}
	
	public void deleteFile()
	{
		if (alreadyExists) return;
		try{
			File f=new File(tempFilename);
			f.delete();
		}catch(Exception e){}
	}
	
	
	public void setFileSize(long newLength)
	{
		if (rf!=null)
		{
			try {
				rf.setLength(newLength);
				rf.seek(0);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public void feedData(byte[]data, int size)
	{
		if (!alreadyExists)
		{
			try{
				rf.write(data, 0, size);
				cachedSize+=size;
				Log.i("cachedSize="+cachedSize);
				data=null;
			}catch(Exception e){
				e.printStackTrace();
				Log.e("Failed to write data");
			}
		}
		else{
			cachedSize+=size;
			Log.i("cachedSize="+cachedSize);
		}
		
		if (mType<3 && alreadyExists)
		{
			
		}
		else if (mType<3 && !alreadyExists && cachedSize-lastCachedSize>INITIALSIZE)
		{
			new Thread(startPlaying).start();
			lastCachedSize=cachedSize;
		}
		else if (mType>=3 && alreadyExists && intent!=null){
			new Thread(startShowingFile).start();
			intent=null;
		}
	}
	
	public void start()
	{
		new Thread(startPlaying).start();
	}
	
	Runnable startPlaying=new Runnable()
	{
		public void run()
		{
			Log.i("startPlaying");
			if (FilePushActivity.getInstance()!=null)
			{
				try {
					FilePushActivity.getInstance().tryToPlay();
				} catch (Exception e) {
					e.printStackTrace();
					Log.e("Failed to setSource()...");
				}
			}
			else
				Log.w("FilePushActivity==null");
		}
	};
	
	Runnable startShowingFile=new Runnable()
	{
		public void run()
		{
			if (FilePushActivity.getInstance()!=null)
			{
				try {
					FilePushActivity.getInstance().finalizeFile();
				} catch (Exception e) {
					e.printStackTrace();
					Log.e("Failed to finalizeFile()...");
				}
			}
		}
	};
}
