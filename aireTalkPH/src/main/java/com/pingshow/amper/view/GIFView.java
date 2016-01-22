package com.pingshow.amper.view;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.View;

import com.pingshow.amper.MyPreference;
import com.pingshow.amper.R;
public class GIFView extends View implements Runnable{
	private Bitmap bmb;
	private GIFDecode decode;
	private int h = 100;
	private int w = 100;
	private boolean isRun = true;
	private long speed = 200; 
	public GIFView(android.content.Context context,int index,DisplayMetrics dm,String photoPath){
		super(context);
		decode=new GIFDecode();
		if(photoPath!=null)
		{
			try {
				decode.read(new FileInputStream(photoPath));
			 } catch (FileNotFoundException e) {
				 return;
			 }
		}else
			 decode.read(this.getResources().openRawResource(R.drawable.sm01+index+70));
		bmb=decode.getFrame(0);
		w = (dm.widthPixels-bmb.getWidth())/2;
		h = (dm.heightPixels-bmb.getHeight())/2;
		isRun = true;
		speed = new MyPreference(getContext()).readLong("gifSpeed", speed);
		Thread t=new Thread(this);
		t.start();
	 }

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawBitmap(bmb, w,h,new Paint());
		bmb=decode.next();
	}
	public void stop(){
		isRun = false;
	}
	public void changeSpeed(int type){
		if(type==0)
			speed = speed - 200;
		else if(type==1)
			speed = speed + 200;
		else if(type==2)
			speed = 200;
		if(speed<0)
			speed = 200;
		else if(speed>5000)
			speed = 5000;
	}
	public long getSpeed(){
		return speed;
	}
	@Override
	public void run() {
	   while(isRun){
		   try{
			   this.postInvalidate();
			   Thread.sleep(speed) ; 
		   }catch(Exception ex){}
	   }
	}
}
