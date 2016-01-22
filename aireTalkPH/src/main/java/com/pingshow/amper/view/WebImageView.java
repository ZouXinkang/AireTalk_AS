package com.pingshow.amper.view;

import java.io.File;
import java.util.Date;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.pingshow.amper.Global;
import com.pingshow.amper.MyPreference;
import com.pingshow.network.MyNet;
import com.pingshow.util.ImageUtil;

public class WebImageView extends ImageView {

	private Context mContext;
	private String urlString;
	private String localFilename;
	private Handler mHandler=new Handler();
	public WebImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext=context;
	}
	
	Runnable downloadImage=new Runnable()
	{
		public void run()
		{
			String filename=urlString.substring(urlString.lastIndexOf("/")+1);
			localFilename=Global.SdcardPath_inbox + filename;
			int count=0;
			int success=0;
			do{
				try{
					MyNet net = new MyNet(mContext);
					success=net.Download(urlString, localFilename, null);
					if (success==1) break;
				}catch(Exception e){}
			}while(++count<3);
			
			if (success==1)
			{
				mHandler.post(reloadImage);
				MyPreference mPref=new MyPreference(mContext);
				mPref.writeLong("LastDownload_"+filename,new Date().getTime());
			}
		}
	};
	
	Runnable reloadImage=new Runnable()
	{
		public void run()
		{
			try{
				Bitmap bmp=ImageUtil.loadBitmapSafe(1, localFilename);
				setImageBitmap(bmp);
			}catch(Exception e){}
			catch(Error e){}
		}
	};

	public void setURL(String urlString)
	{
		this.urlString=urlString;
		String filename=urlString.substring(urlString.lastIndexOf("/")+1);
		localFilename=Global.SdcardPath_inbox + filename;
		if (!new File(localFilename).exists())
		{
			new Thread(downloadImage).start();
			return;
		}
		else{
			//UPDATE Banner
			MyPreference mPref=new MyPreference(mContext);
		    long last=mPref.readLong("LastDownload_"+filename,0);
		    long now=new Date().getTime();
		    if (now-last>432000000)//5 days
		    {
		    	new Thread(downloadImage).start();
				return;
		    }
		}
		reloadImage.run();
	}
}
