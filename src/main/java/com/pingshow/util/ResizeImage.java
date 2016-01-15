package com.pingshow.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.widget.Toast;

import com.pingshow.airecenter.R;

public class ResizeImage {
	
	static float ratio_x = 4f;
	static float ratio_y = 4f;
	
	static public void ResizeXY(Context context,String SrcFilePath, String DstFilePath, int newHeight, int Quality)
	{
        Bitmap bitmapOrg = null;
		try {
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();  
			// Limit the filesize since 5MP pictures will kill you RAM
			bitmapOptions.inJustDecodeBounds=true;
			bitmapOrg = BitmapFactory.decodeFile(SrcFilePath, bitmapOptions);
			
			if (bitmapOptions.outHeight!=-1)
			{
				if (bitmapOptions.outHeight > newHeight)
					bitmapOptions.inSampleSize=bitmapOptions.outHeight/newHeight;
				else
					bitmapOptions.inSampleSize=1;
			}
			bitmapOptions.inPurgeable=true;
			bitmapOptions.inJustDecodeBounds=false;
			bitmapOrg = BitmapFactory.decodeFile(SrcFilePath, bitmapOptions);
		} catch (Exception e1) {
			Toast.makeText(context,context.getString(R.string.photo_large), Toast.LENGTH_LONG).show();
			return;
		} catch (OutOfMemoryError e) {
			Toast.makeText(context,context.getString(R.string.photo_large), Toast.LENGTH_LONG).show();
			return;
		}
        
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        
        int Lx,Ly,Rx,Ry;
        float ratio;
        int newWidth = (int)(ratio_x*newHeight/ratio_y);
        
        if (ratio_x*height>ratio_y*width)
        {
        	Lx=0;
        	Ly=(int)((height-ratio_y*width/ratio_x)/2);
        	Rx=width;
        	Ry=(int)(ratio_y*width/ratio_x);
        	ratio=((float)newWidth)/width;
        }
        else if (ratio_x*height<ratio_y*width)
        {
        	Lx=(int)((width-ratio_x*height/ratio_y)/2);
        	Ly=0;
        	Rx=(int)(ratio_x*height/ratio_y);
        	Ry=height;
        	ratio=((float)newHeight)/height;
        }
        else {
        	Lx=0;
        	Ly=0;
        	Rx=width;
        	Ry=height;
        	ratio=((float)newHeight)/height;
        }
        
        try{
            Matrix matrix = new Matrix();
            matrix.postScale(ratio, ratio);
           
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, Lx, Ly, 
                              Rx, Ry, matrix, true); 
            File myCaptureFile = new File(DstFilePath);
            BufferedOutputStream bos = new BufferedOutputStream(
            	new FileOutputStream(myCaptureFile));
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, Quality, bos);
            bos.flush();
            bos.close();
        }catch(Exception e){}
        catch (OutOfMemoryError e) {}
	}
	
	
	static public int ResizeXYFromStream(Context context,Intent data, String DstFilePath, int newHeight, int Quality)
	{
        Bitmap bitmapOrg = null;
		try {
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			InputStream is = context.getContentResolver().openInputStream(data.getData());
			bitmapOptions.inJustDecodeBounds=true;
		    bitmapOrg = BitmapFactory.decodeStream(is, null, bitmapOptions);
		    is.close();
		    if (bitmapOptions.outHeight!=-1)
			{
				if (bitmapOptions.outHeight > newHeight)
					bitmapOptions.inSampleSize=((bitmapOptions.outHeight+(newHeight-1))/newHeight);
				else
					bitmapOptions.inSampleSize=1;
			}
		    bitmapOptions.inJustDecodeBounds=false;
 			bitmapOptions.inPurgeable=true; 
 			is = context.getContentResolver().openInputStream(data.getData());
 			bitmapOrg = BitmapFactory.decodeStream(is, null, bitmapOptions);
		    is.close();
		} catch (Exception e1) {
			Toast.makeText(context,context.getString(R.string.photo_large), Toast.LENGTH_LONG).show();
			return -1;
		} catch (OutOfMemoryError e) {
			Toast.makeText(context,context.getString(R.string.photo_large), Toast.LENGTH_LONG).show();
			return -1;
		}
        
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        
        int Lx,Ly,Rx,Ry;
        float ratio;
        int newWidth = (int)(ratio_x*newHeight/ratio_y);
        
        if (ratio_x*height>ratio_y*width)
        {
        	Lx=0;
        	Ly=(int)((height-ratio_y*width/ratio_x)/2);
        	Rx=width;
        	Ry=(int)(ratio_y*width/ratio_x);
        	ratio=((float)newWidth)/width;
        }
        else if (ratio_x*height<ratio_y*width)
        {
        	Lx=(int)((width-ratio_x*height/ratio_y)/2);
        	Ly=0;
        	Rx=(int)(ratio_x*height/ratio_y);
        	Ry=height;
        	ratio=((float)newHeight)/height;
        }
        else {
        	Lx=0;
        	Ly=0;
        	Rx=width;
        	Ry=height;
        	ratio=((float)newHeight)/height;
        }
        
        try{
            Matrix matrix = new Matrix();
            matrix.postScale(ratio, ratio);
           
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, Lx, Ly, 
                              Rx, Ry, matrix, true); 
            File myCaptureFile = new File(DstFilePath);
            BufferedOutputStream bos = new BufferedOutputStream(
            	new FileOutputStream(myCaptureFile));
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, Quality, bos);
            bos.flush();
            bos.close();
            return 1;
        }catch(Exception e){}
        catch (OutOfMemoryError e) {}
        
        return 0;
	}
	
	static public int Resize(Context context,String SrcFilePath, String DstFilePath, int newWidth, int newHeight, int Quality)
	{
		Bitmap bitmapOrg = null;
		try {
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();  
			// Limit the filesize since 5MP pictures will kill you RAM
			bitmapOptions.inJustDecodeBounds=true;
			bitmapOrg = BitmapFactory.decodeFile(SrcFilePath, bitmapOptions);
			if (bitmapOptions.outHeight!=-1)
			{
				if (bitmapOptions.outHeight > newHeight)
					bitmapOptions.inSampleSize=bitmapOptions.outHeight/newHeight;
				else
					bitmapOptions.inSampleSize=1;
			}
			bitmapOptions.inJustDecodeBounds=false;
			bitmapOptions.inPurgeable=true; 
			bitmapOrg = BitmapFactory.decodeFile(SrcFilePath, bitmapOptions);
		} catch (Exception e1) {
			Toast.makeText(context,context.getString(R.string.photo_large), Toast.LENGTH_LONG).show();
			return -1;
		} catch (OutOfMemoryError e) {
			Toast.makeText(context,context.getString(R.string.photo_large), Toast.LENGTH_LONG).show();
			return -1;
		}
		
        if(bitmapOrg==null)
        {
        	return -1;
        }
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        
        float ratio;
        
        if (height<width)
        {
        	ratio=((float)newWidth)/width;
        }
        else
        {
        	ratio=((float)newHeight)/height;
        }
        
        try{
            Matrix matrix = new Matrix();
            matrix.postScale(ratio, ratio);
           
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, 
            		width, height, matrix, true); 
            File myCaptureFile = new File(DstFilePath);
            BufferedOutputStream bos = new BufferedOutputStream(
            	new FileOutputStream(myCaptureFile));
            
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, Quality, bos);
            bos.flush();
            bos.close();
        }catch(Exception e){} 
        catch (OutOfMemoryError e) {}
        
        return 0;
	}
	
	static public void ResizeXY(Context context, Bitmap bitmapOrg, String DstFilePath, int newHeight, int Quality)
	{
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        
        int Lx,Ly,Rx,Ry;
        float ratio;
        int newWidth = (int)(ratio_x*newHeight/ratio_y);
        
        if (ratio_x*height>ratio_y*width)
        {
        	Lx=0;
        	Ly=(int)((height-ratio_y*width/ratio_x)/2);
        	Rx=width;
        	Ry=(int)(ratio_y*width/ratio_x);
        	ratio=((float)newWidth)/width;
        }
        else if (ratio_x*height<ratio_y*width)
        {
        	Lx=(int)((width-ratio_x*height/ratio_y)/2);
        	Ly=0;
        	Rx=(int)(ratio_x*height/ratio_y);
        	Ry=height;
        	ratio=((float)newHeight)/height;
        }
        else {
        	Lx=0;
        	Ly=0;
        	Rx=width;
        	Ry=height;
        	ratio=((float)newHeight)/height;
        }
        
        try{
            Matrix matrix = new Matrix();
            matrix.postScale(ratio, ratio);
           
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, Lx, Ly, 
                              Rx, Ry, matrix, true); 
            File myCaptureFile = new File(DstFilePath);
            BufferedOutputStream bos = new BufferedOutputStream(
            	new FileOutputStream(myCaptureFile));
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, Quality, bos);
            bos.flush();
            bos.close();
        }catch(Exception e){}
        catch (OutOfMemoryError e) {}
	}

	static public int saveFromStream(Context context, Intent data, String DstFilePath, int newWidth, int newHeight, int Quality)
	{
		Bitmap bitmapOrg = null;
		try {
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			InputStream is = context.getContentResolver().openInputStream(data.getData());
		    bitmapOrg = BitmapFactory.decodeStream(is, null, bitmapOptions);
		    is.close();
		} catch (Exception e1) {
			Toast.makeText(context,context.getString(R.string.photo_large), Toast.LENGTH_LONG).show();
			return -1;
		} catch (OutOfMemoryError e) {
			Toast.makeText(context,context.getString(R.string.photo_large), Toast.LENGTH_LONG).show();
			return -1;
		}
		
        if(bitmapOrg==null)
        {
        	return -1;
        }
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        
        float ratio;
        
        if (height<width)
        {
        	ratio=((float)newWidth)/width;
        }
        else
        {
        	ratio=((float)newHeight)/height;
        }
        
        try{
            Matrix matrix = new Matrix();
            matrix.postScale(ratio, ratio);
           
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, 
            		width, height, matrix, true); 
            File myCaptureFile = new File(DstFilePath);
            BufferedOutputStream bos = new BufferedOutputStream(
            	new FileOutputStream(myCaptureFile));
            
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, Quality, bos);
            bos.flush();
            bos.close();
        }catch(Exception e){} 
        catch (OutOfMemoryError e) {}
        
        return 0;
	}
	
	static public void ResizeBitmapXY(Context context,Bitmap bitmapOrg, String DstFilePath, int newHeight, int Quality)
	{
		int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        
        int Lx,Ly,Rx,Ry;
        float ratio;
        int newWidth = (int)(ratio_x*newHeight/ratio_y);
        
        if (ratio_x*height>ratio_y*width)
        {
        	Lx=0;
        	Ly=(int)((height-ratio_y*width/ratio_x)/2);
        	Rx=width;
        	Ry=(int)(ratio_y*width/ratio_x);
        	ratio=((float)newWidth)/width;
        }
        else if (ratio_x*height<ratio_y*width)
        {
        	Lx=(int)((width-ratio_x*height/ratio_y)/2);
        	Ly=0;
        	Rx=(int)(ratio_x*height/ratio_y);
        	Ry=height;
        	ratio=((float)newHeight)/height;
        }
        else {
        	Lx=0;
        	Ly=0;
        	Rx=width;
        	Ry=height;
        	ratio=((float)newHeight)/height;
        }
        
        try{
            Matrix matrix = new Matrix();
            matrix.postScale(ratio, ratio);
           
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, Lx, Ly, 
                              Rx, Ry, matrix, true); 
            File myCaptureFile = new File(DstFilePath);
            BufferedOutputStream bos = new BufferedOutputStream(
            	new FileOutputStream(myCaptureFile));
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, Quality, bos);
            bos.flush();
            bos.close();
        }catch(Exception e){}
        catch (OutOfMemoryError e) {}
	}
}