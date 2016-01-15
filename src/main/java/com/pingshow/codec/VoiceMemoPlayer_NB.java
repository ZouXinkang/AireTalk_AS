package com.pingshow.codec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;

public class VoiceMemoPlayer_NB {
	
	private Ncodec codec;
	static private boolean bRunning;
	private AudioTrack track;
	private FileInputStream fin;
	private int length;
	private int iMinBufSize;
	private Context context = null; 
	private String SrcFilePath = null;
	private boolean soundOut = false;
	
	public VoiceMemoPlayer_NB(Context context)
	{
		this.context = context;
	}
	
	public void setDataSource(String SrcFilePath) throws IOException
	{
		File f=new File(SrcFilePath);
		fin = new FileInputStream(f);
		length=(int)f.length();
		this.SrcFilePath = SrcFilePath;
	}
	
	public static final int BUFFER_SIZE = 1024;
	
	public void prepare() throws IllegalArgumentException
	{
		iMinBufSize = AudioTrack.getMinBufferSize(8000, 
				AudioFormat.CHANNEL_CONFIGURATION_MONO, 
				AudioFormat.ENCODING_PCM_16BIT);
		
		track = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, 
				AudioFormat.ENCODING_PCM_16BIT,
				iMinBufSize, AudioTrack.MODE_STREAM);
		
		iMinBufSize/=2;
		
		iMinBufSize=(iMinBufSize+319)/160*160;
		
		AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		am.setMode(AudioManager.MODE_NORMAL);
			
		codec=new Ncodec();
		if (!codec.load())
			codec=null;
	}
	
	public void start() throws IllegalStateException
	{
		if (bRunning) {
			Log.d("start *** bRunning  already running");
			release();
			throw new IllegalStateException("still running");
		}
		if (codec!=null && track!=null && fin!=null && length>6)//alec
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
			try{
				byte[] data = new byte[length];
				
				int pos=6,ret=0;
				int outpos=0;
				try{
					fin.read(data, 0, length);
					fin.close();
				}catch(IOException e){
					return;
				}catch(Exception e){
					return;
				}
				
				//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				bRunning=true;
				
				short[] pcm=new short[iMinBufSize+640];
				
				while(pos<length && bRunning){
					
					if (data[pos]==0x7C)
					{
						noise(pcm,160,12);
						ret=1;
					}
					else
						ret=codec.decode(data, pos, pcm, pos+96<=length?96:length-pos, outpos);
					
					if (ret>0)
					{
						pos+=ret;
						outpos+=160;
						try{
							track.write(pcm, 0, outpos);
							track.play();
						}catch(IllegalStateException e){
							break;
						}
						outpos=0;
						soundOut=true;
					}
					else
						break;
				}
				
				if (bRunning)
					stop();
			}catch (Exception e){}
		}
	};
	
	static void Sleep(int ms)
    {
    	try{
			Thread.sleep(ms);
		}catch(Exception e){}
    }
	
	public void stop() throws IllegalStateException
	{
		bRunning=false;
		Sleep(40);
		release();
	}
	
	public void release()
	{
		if (track!=null)
		{
			track.stop();
			track.release();
			track=null;
		}
		if (codec!=null)
		{
			codec.release();
			codec=null;
		}
		
		if (soundOut)
		{
			// send broadcast for clear button in InterphoneActivity
			if(SrcFilePath.contains("interphonevoice")){
				Intent intent = new Intent();
				intent.setAction(Global.ACTION_PLAY_AUDIO);
				intent.putExtra("clear", 1);
				context.sendBroadcast(intent);
				
				File file = new File(SrcFilePath);
				if(file.exists()){
					file.delete();
				}
			}
			else{
				Intent intent = new Intent();
				intent.setAction(Global.ACTION_PLAY_OVER);
				context.sendBroadcast(intent);
			}
		}
		
		System.gc();
		System.gc();
	}
	
	public boolean isPlaying()
	{
		return bRunning;
	}
	
}
