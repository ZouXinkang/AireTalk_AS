package com.pingshow.amper;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.pingshow.amper.view.ProgressView;

public class MusicPlayerActivity extends Activity {

	private ImageView buttonPlayStop;
	private ArrayList<MediaPlayer> mediaPlayers = new ArrayList<MediaPlayer>();
	private SeekBar seekBar;
	private boolean playing=false;
	private Handler mHandler=new Handler();
	private String URL="";
	private TextView tvPosition;
	private TextView tvRemain;
	private ProgressView progress;
	
	private int duration;
	
	Runnable progressStep = new Runnable() {
        public void run() {
        	startPlayProgressUpdater();
		}
    };
    
    private MediaPlayer getCurrentPlayer()
    {
    	if (mediaPlayers.size()==0) return null;
    	MediaPlayer mp=mediaPlayers.get(mediaPlayers.size()-1);
    	return mp;
    }
    
	public void startPlayProgressUpdater() {
		
		try{
			int pos=0;
			MediaPlayer mp=getCurrentPlayer();
			if (mp!=null)
			{
				pos=mp.getCurrentPosition();
				seekBar.setProgress(pos);
				int sec=pos/1000;
				int remain=duration/1000-sec;
				if (remain<0) remain=0;
				tvPosition.setText(String.format("%d:%02d",sec/60,sec%60));
				tvRemain.setText(String.format("-%d:%02d",remain/60,remain%60));
				if (mp.isPlaying()) {
				    mHandler.postDelayed(progressStep,1000);
		    	}else{
		    		mp.pause();
		    		buttonPlayStop.setImageResource(R.drawable.play);
		    	}
			}
		}catch(Exception e){}
    }
	
	private void seekChange(View v){
		try{
			MediaPlayer mp=getCurrentPlayer();
	    	if(mp.isPlaying()){
		    	SeekBar sb = (SeekBar)v;
		    	mp.seekTo(sb.getProgress());
			}
		}catch(Exception e){}
    }

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_window);
		
		seekBar=(SeekBar)findViewById(R.id.seekbar);
		buttonPlayStop=(ImageView)findViewById(R.id.play);
		tvPosition=(TextView)findViewById(R.id.tick);
		tvRemain=(TextView)findViewById(R.id.remain);
		progress=(ProgressView)findViewById(R.id.progress);
		
        seekBar.setOnTouchListener(new OnTouchListener() {
        	@Override 
        	public boolean onTouch(View v, MotionEvent event) {
        		seekChange(v);
        		return false; 
			}
		});
		
		buttonPlayStop.setOnClickListener(new OnClickListener() {
			@Override 
			public void onClick(View v) {
				if (!playing) {
		            buttonPlayStop.setImageResource(R.drawable.pause);
		            MediaPlayer mp=getCurrentPlayer();
		            try{
		            	mp.start();
		                startPlayProgressUpdater();
		                playing=true;
		            }catch (IllegalStateException e) {
		            	mp.pause();
		            }
		        }else {
		        	playing=false;
		        	buttonPlayStop.setImageResource(R.drawable.play);
		        	try{
		        		MediaPlayer mp=getCurrentPlayer();
		        		mp.pause();
		        	}catch (IllegalStateException e) {
		            }
		        }
			}
		});
		
		try{
			Bitmap bm=BitmapFactory.decodeFile(getIntent().getStringExtra("art"));
			((ImageView)findViewById(R.id.img)).setImageBitmap(bm);
		}catch(Exception e){}
		catch(Error e){}
		
		URL=getIntent().getStringExtra("URL");
		
		mHandler.postDelayed(new Runnable(){
			public void run()
			{
				new Thread(startMedia).start();
			}
		}, 100);
	}
	
	Runnable startMedia=new Runnable()
	{
		public void run()
		{
			if (URL.length()==0 || URL.contains("NO FILE") || !URL.contains(".mp3"))
				return;
			
			Log.d("new MediaPlayer...");
			MediaPlayer mp = new MediaPlayer();
			mediaPlayers.add(mp);
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mHandler.post(new Runnable(){ 
             	public void run(){
             		seekBar.setVisibility(View.INVISIBLE);
                	buttonPlayStop.setVisibility(View.INVISIBLE);
                	tvPosition.setVisibility(View.INVISIBLE);
                	tvRemain.setVisibility(View.INVISIBLE);
             	}
			});
			
			try {
				mp.setDataSource(URL);
				Log.d("setDataSource... "+URL);
			} catch (IllegalArgumentException e) {
	        } catch (SecurityException e) {
	        } catch (IllegalStateException e) {
	        } catch (IOException e) {
	        }
			
			try {
				Log.d("prepare...");
	            mp.prepare();
	            try{
	            	mp.start();
	                mHandler.post(new Runnable(){ 
	                	public void run(){
	                		MediaPlayer mp=getCurrentPlayer();
	                		duration=mp.getDuration();
	                		Log.d("duration "+duration);
	                		if (duration==0) duration=90000;
	                		seekBar.setMax(duration);
	                		startPlayProgressUpdater();
		                	buttonPlayStop.setImageResource(R.drawable.pause);
		                	buttonPlayStop.setVisibility(View.VISIBLE);
		                	tvPosition.setVisibility(View.VISIBLE);
		                	tvRemain.setVisibility(View.VISIBLE);
	                	}
	                });
	                playing=true;
	            }catch (IllegalStateException e) {
	            	mp.pause();
	            	Log.e("IllegalStateException to start, then paused... " + e.getMessage());
	            }
	        } catch (IllegalStateException e) {
	        	Log.e("IllegalStateException prepare... " + e.getMessage());
	        } catch (IOException e) {
	        	Log.e("IOException prepare... " + e.getMessage());
	        } finally{
	        	mHandler.postDelayed(new Runnable(){ 
                	public void run(){
                		seekBar.setVisibility(View.VISIBLE);
	                	progress.setVisibility(View.INVISIBLE);
	                	progress.setImageResource(R.drawable.null_btn);
                	}
                }, 500);
	        }
		}
	};
	
	
	@Override
	protected void onDestroy() {
		closeMedia();
		mediaPlayers=null;
		super.onDestroy();
	}

	private void closeMedia()
	{
		try{
			URL="";
			for (MediaPlayer mp: mediaPlayers)
			{
				if (mp!=null)
				{
					try{
						Log.d("closeMedia.....");
						mp.stop();
						mp.release();
					}catch(Exception e){
						Log.e("Failed to stop/release mp " + e.getMessage());
					}
				}
			}
			mediaPlayers.clear();
		}catch(Exception e){
			Log.w("Exception closeMedia.....");
		}
	}
}
