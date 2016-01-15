package com.pingshow.gif;

import java.io.InputStream;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import com.pingshow.airecenter.Log;
public class GifView extends View implements GifAction{

	private GifDecoder gifDecoder = null;
	private Bitmap currentImage = null;
	
	private boolean pause = false;
	int imageh,imagew;
	public int getImageh() {
		return imageh;
	}

	public void setImageSize(int width, int height) {
		this.imagew = width;
		this.imageh = height;
	}

	private int showWidth = -1;
	private int showHeight = -1;
	private Rect rect = null;
	
	private DrawThread drawThread = null;
	private Handler mHandler = new Handler();
	
	private GifImageType animationType = GifImageType.SYNC_DECODER;
	
	public enum GifImageType{
		WAIT_FINISH (0),
		SYNC_DECODER (1),
		COVER (2);
		
		GifImageType(int i){
			nativeInt = i;
		}
		final int nativeInt;
	}
	
	
	public GifView(Context context) {
        super(context);
        
    }
    
    public GifView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public GifView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
    }
    private void setGifDecoderImage(byte[] gif){
    	if(gifDecoder != null){  		
    		gifDecoder.free();
    		gifDecoder = null;
    	}
    	gifDecoder = new GifDecoder(gif,this);
    	gifDecoder.start();
    }
    
    private void setGifDecoderImage(InputStream is){
    	if(gifDecoder != null){
    		gifDecoder.free();
    		gifDecoder= null;
    		pause = false;
    	}
    	gifDecoder = new GifDecoder(is,this);
    	gifDecoder.start();
    }
    public void setGifImage(byte[] gif){
    	setGifDecoderImage(gif);
    }
    
    public void setGifImage(InputStream is){
    	setGifDecoderImage(is);
    }
    public void setGifImage(int resId){
    	final int resid=resId;
		new setgifimage().execute(resid);
    }
    
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(gifDecoder == null)
        	return;
        if(currentImage == null){
        	currentImage = gifDecoder.getImage();
        }
        if(currentImage == null){
        		return;
        }
        int saveCount = canvas.getSaveCount();
        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        if(showWidth == -1){
        	canvas.drawBitmap(currentImage, 0, 0, null);
        }else{
        	canvas.drawBitmap(currentImage, null, rect, null);
        }
        canvas.restoreToCount(saveCount);
    }
    
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	int pleft = getPaddingLeft();
        int pright = getPaddingRight();
        int ptop = getPaddingTop();
        int pbottom = getPaddingBottom();

        int w=0;
        int h=0;
        
        w = imagew + pleft + pright;
        h = imageh + ptop + pbottom;

        setMeasuredDimension(w, h);
    }
    public void showCover(){
    	if(gifDecoder == null)
    		return;
    	pause = true;
    	currentImage = gifDecoder.getImage();
    	invalidate();
    }
    public void showAnimation(){
    	pause = false;
    }
    public void setGifImageType(GifImageType type){
    	if(gifDecoder == null)
    		animationType = type;
    }
    public void setShowDimension(int width,int height){
    	if(width > 0 && height > 0){
	    	showWidth = width;
	    	showHeight = height;
	    	rect = new Rect();
	    	
			rect.left = 0;
			rect.top = 0;
			rect.right = width;
			rect.bottom = height;
    	}
    }
    
    public void parseOk(boolean parseStatus,int frameIndex){
    	if(parseStatus){
    		if(gifDecoder != null){
    			switch(animationType){
    			case WAIT_FINISH:
    				if(frameIndex == -1){
    					if(gifDecoder.getFrameCount() > 1){ 
    	    				DrawThread dt = new DrawThread();
    	    	    		dt.start();
    	    			}else{
    	    				reDraw();
    	    			}
    				}
    				break;
    			case COVER:
    				pause = true;
    				reDraw();
    				break;
    			case SYNC_DECODER:
    				pause = false;
    				if(frameIndex == 1){
    					currentImage = gifDecoder.getImage();
    					reDraw();
    				}else if(frameIndex == -1){
    					reDraw();
    				}else{
    					if(drawThread == null){
    						drawThread = new DrawThread();
    						drawThread.start();
    					}
    				}
    				break;
    			}
 
    		}else{
    			Log.e("gif parse error");
    		}
    	}
    }
    
    private void reDraw(){
    	if(redrawHandler != null){
			Message msg = redrawHandler.obtainMessage();
			redrawHandler.sendMessage(msg);
    	}
    }
    
    private Handler redrawHandler = new Handler(){
    	public void handleMessage(Message msg) {
    		invalidate();
    	}
    };
    public void stop(){
    	if (drawThread!=null)
    		drawThread.isRun = false;
    	if(gifDecoder != null){  		
    		gifDecoder.free();
    		gifDecoder = null;
    	}
    }
    
	public void destroyDrawingCache() {//alec
    	showCover();
		super.destroyDrawingCache();
	}

	private class DrawThread extends Thread {
		boolean isRun=true;
		int count=0;
    	public void run(){
    		if(gifDecoder == null){
    			return;
    		}
    		while(isRun && ++count<500){
    			if(pause == false && gifDecoder!=null){
//	    			if(gifDecoder.parseOk()){
	    				GifFrame frame = gifDecoder.next();
	    				if(frame==null){
	    					pause = true;
	    				}else{
		    				currentImage = frame.image;
		    				if(redrawHandler != null){
		    					Message msg = redrawHandler.obtainMessage();
		    					redrawHandler.sendMessage(msg);
		    					SystemClock.sleep(frame.delay+50); 
		    				}else{
		    					break;
		    				}
	    				}
//	    			}else{
//	    				currentImage = gifDecoder.getImage();
//	    				break;
//	    			}
    			}else{
    				SystemClock.sleep(250);
    			}
    		}
    	}
    }
	
	public Bitmap getBitmap()
	{
		return currentImage;
	}
	
	private class setgifimage extends
	AsyncTask<Integer, Integer, InputStream> {
		InputStream is ;
		@Override
		protected void onPostExecute(InputStream result) {
			super.onPostExecute(result);
			setGifDecoderImage(result);
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
		}

		@Override
		protected InputStream doInBackground(Integer... params) {
			Resources r = GifView.this.getResources();
			is = r.openRawResource(params[0]);
			return is;
		}
	}
}
