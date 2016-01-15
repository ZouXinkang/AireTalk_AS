package com.pingshow.util;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class OpenDifferentFile {
	private Context mContext = null;
	public OpenDifferentFile(Context context){
		mContext = context;
	}
	public void openFile(String filePath) 
    {
		File file = new File(filePath);
		if(!file.exists()) return;
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);
      
		String type = getType(file);
		intent.setDataAndType(Uri.fromFile(file),type);
		try {
			mContext.startActivity(intent);
		} catch (Exception e) {
			type="*/*";
			intent.setDataAndType(Uri.fromFile(file),type);
			mContext.startActivity(intent);
		} 
    }

    private String getType(File file)  
    { 
    	String type="";
    	String fName=file.getName();
    	String end=fName.substring(fName.lastIndexOf(".")
    			+1,fName.length()).toLowerCase(); 
  
    	if(end.equals("wma")||end.equals("mp3")||end.equals("mid")||end.equals("wav")||end.equals("amr"))
    	{
    		type = "audio"; 
    	}else if(end.equals("3gp")||end.equals("mp4")||end.equals("avi")||end.equals("rmvb")){
    		type = "video/"+end;
    	}else if(end.equals("jpg")||end.equals("gif")||end.equals("png")||
    			end.equals("jpeg")||end.equals("bmp")) {
    		type = "image/"+end;
    	}else if(end.equals("apk")) { 
    		type = "application/vnd.android.package-archive"; 
    	}else if(end.equals("txt") || end.equals("java")) { 
    		type = "text/plain"; 
    	}else if(end.equals("pdf")) { 
    		type = "application/pdf"; 
    	}else if(end.equals("docx")||end.equals("doc")) { 
    		type = "application/msword"; 
    	}else if(end.equals("xls") || end.equals("xlsx") || end.equals("csv")) { 
    		type = "application/vnd.ms-excel"; 
    	}else if(end.equals("ppt")||end.equals("pptx")) { 
    		type = "application/vnd.ms-powerpoint"; 
    	}else {
    		type="*/*";
    	}
    	return type;  
    } 
}
