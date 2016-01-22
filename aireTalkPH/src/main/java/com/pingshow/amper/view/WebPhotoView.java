package com.pingshow.amper.view;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.pingshow.amper.Global;
import com.pingshow.amper.R;
import com.pingshow.network.MyNet;
import com.pingshow.util.ImageUtil;

public class WebPhotoView extends ImageView {

	private Context mContext;
	private String urlString;
	private String localFilename;
	private Handler mHandler=new Handler();
	public WebPhotoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext=context;
	}
	
	Runnable downloadImage=new Runnable()
	{
		public void run()
		{
			String filename=urlString.substring(urlString.lastIndexOf("/")+1);
			localFilename=Global.SdcardPath_inbox + filename;
			boolean success=false;
			try{
				MyNet net = new MyNet(mContext);
				success=net.anyDownload(urlString, localFilename);
			}catch(Exception e){}
			
			if (success)
			{
				mHandler.post(reloadImage);
			}
		}
	};
	
	Runnable reloadImage=new Runnable()
	{
		public void run()
		{
			try{
				Bitmap bmp=ImageUtil.loadBitmapSafe(1, localFilename);
				if (bmp!=null)
					setImageBitmap(bmp);
				else
					setImageResource(R.drawable.bighead);
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
			setImageResource(R.drawable.bighead);
			new Thread(downloadImage).start();
			return;
		}
		reloadImage.run();
	}
}
