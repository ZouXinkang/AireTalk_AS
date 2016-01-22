package com.pingshow.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.pingshow.amper.Global;
import com.pingshow.amper.Log;
import com.pingshow.util.MyUtil;

public class RealTimeWTPlayer_WB {

	private Scodec codec;
	private boolean bRunning;
	private AudioTrack track;
	private int iMinBufSize;
	private Context context = null;
	private int netType=1;
	private boolean bPublic;
	
	private List<byte[]>bufferList=new ArrayList<byte[]>();

	
	public RealTimeWTPlayer_WB(Context context, int net_type, boolean publicWT)
	{
		this.context = context;
		netType=net_type;
		bPublic=publicWT;
	}
	
	public void append(byte [] data, int len)
	{
		if (bRunning)
		{
			byte [] newData=new byte [len-23];
			System.arraycopy(data, 23, newData, 0, len-23);
			bufferList.add(newData);
		}
	}
	
	public void run() throws IllegalArgumentException
	{
		iMinBufSize = AudioTrack.getMinBufferSize(16000, 
				AudioFormat.CHANNEL_CONFIGURATION_MONO, 
				AudioFormat.ENCODING_PCM_16BIT);
		
		track = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_CONFIGURATION_MONO, 
				AudioFormat.ENCODING_PCM_16BIT,
				iMinBufSize, AudioTrack.MODE_STREAM);
		
		iMinBufSize/=2;
		
		iMinBufSize=(iMinBufSize+319)/320*320;
		
		AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		am.setMode(AudioManager.MODE_NORMAL);
		
		codec=new Scodec();
		if (!codec.load(0,0))
			codec=null;
		try{
			start();
		}catch(IllegalStateException e){}
	}
	
	public void start()
	{
		if (bRunning) {
			Log.d("start *** bRunning  already running");
			release();
			throw new IllegalStateException("still running");
		}
		if (codec!=null && track!=null)//alec
		{
			Log.d("start *** starting");
			Thread thr=new Thread(mDecoding,"PlaybackingVoiceMemo");
			thr.start();
		}
		else{
			Log.d("***Not starting");
			release();
		}
	}
	
	void noise(short [] lin, int len, double power)
	{
		int i,r = (int)(power+power);
		Random a=new Random();
		short ran;

		for (i = 0; i < len; i += 8) {
			ran = (short)(a.nextInt(r+r)-r);
			lin[i] = ran;
			lin[i+1] = ran;
			lin[i+2] = ran;
			lin[i+3] = ran;
			lin[i+4] = ran;
			lin[i+5] = ran;
			lin[i+6] = ran;
			lin[i+7] = ran;
		}
	}
	
	final Runnable mDecoding=new Runnable() {
		public void run(){
			int ret=0;
			int outpos=0;
			
			bRunning=true;
				
			try{
				if (bufferList==null)
					bufferList=new ArrayList<byte[]>();
				
				short[] pcm=new short[iMinBufSize+640];
				byte [] buffer=null;
				int length=0;
				int pos=0;
				int errorCount=0;
				boolean reachEnd=false;
				
				int bufferFill=((netType==1)?4:((netType==2)?3:1));
				int timeOutLimit=((netType==1)?14:((netType==2)?12:10));
				
				while(bufferList.size()<bufferFill && errorCount++<10){
					MyUtil.Sleep(300);
				};
				
				errorCount=0;
				
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				
				while(bRunning && !reachEnd && errorCount<timeOutLimit){
					
					if (bufferList.size()>0)
					{
						errorCount=0;
						buffer=bufferList.remove(0);
						length=buffer.length;
						//Log.d("CODEC from buffer length="+length);
						pos=0;
					}
					else{
						errorCount++;
						buffer=new byte[12];
						for (int i=0;i<12;i++)
							buffer[i]=0x7C;
						pos=0;
						length=12;
					}
					
					if (buffer[pos]==-1)
						break;
					
					while(pos<length && bRunning)
					{
						if (buffer[pos]==0x7C)
						{
							noise(pcm,320,6);
							ret=1;
						}
						else{
							byte toc=buffer[pos];
							int s=0;
							if (toc>=64) s=15;
							else if (toc>=48) s=60;
							else if (toc>=40) s=42;
							else s=25;
						
							if (pos+s>length) s=length-pos;
							if (s==1)
							{
								reachEnd=true;
								break;
							}
							
							try{
								//60 bytes: mode 7: 48~55
								//42 bytes: mode 5: 40~47
								//25 bytes: mode 3: 24~31
								//15 bytes: mode 1: 64~71
								ret=codec.decode(buffer, pos, pcm, s, outpos);
								ret=s;
							}catch(Exception e){
							}
						}
						
						//Log.d("CODEC: ret="+ret);
						if (ret>0)
						{
							pos+=ret;
							outpos+=320;
	
							if (bRunning)
							{
								Ampitude.feedAmp(pcm, 0, 320);
								
								try{
									if (bPublic) 
										Volume.amplify(pcm, 0, 320);
									else
										Volume.amplify75(pcm, 320);
									track.write(pcm, 0, outpos);
									track.play();
								}catch(IllegalStateException e){
									break;
								}catch(Exception e){
									reachEnd=true;
									break;
								}
							}
							outpos=0;
						}
						else{
							Log.e("CODEC: DECODE FAILED ###");
							reachEnd=true;
							break;
						}
					}
					
					buffer=null;
				}
			
			}catch(Exception e){}
			
			if (bRunning)
				stop();
			
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
		}
	};
	
	static void Sleep(int ms)
    {
    	try{
			Thread.sleep(ms);
		}catch(Exception e){}
    }
	
	public void clear()
	{
		if (bufferList!=null) bufferList.clear();
	}
	
	public void stop()
	{
		bRunning=false;
		if (track!=null)
		{
			track.stop();
		}
		if (bufferList!=null) bufferList.clear();
		release();
	}
	
	public void release()
	{
		bRunning=false;
		Sleep(30);
		if (track!=null)
		{
			try{
				track.stop();
				track.release();
				track=null;
			}catch(Exception e){}
			
			Intent intent = new Intent();
			intent.setAction(Global.ACTION_PLAY_AUDIO);
			intent.putExtra("clear", 1);
			context.sendBroadcast(intent);
		}
		if (codec!=null)
		{
			codec.release();
			codec=null;
		}
		
		Ampitude.resetAmp();
		
		bufferList=null;
		
		System.gc();
		System.gc();
	}
	
	public boolean isPlaying()
	{
		return bRunning;
	}
	
}
