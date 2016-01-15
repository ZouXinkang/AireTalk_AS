package com.pingshow.codec;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;

import com.pingshow.airecenter.Log;
import com.pingshow.util.MyUtil;

public class VoiceMemo_NB {
	
	private AudioRecord recorder;
	private Ncodec codec;
	public static boolean bRecording=false;
	private BufferedOutputStream out;
	final int frame_rate=1500; //50fx30s
	
	private byte[] encoded;
	private int data_offset=0;
	private int total_size;

	public VoiceMemo_NB(String path)
	{
		try{
			FileOutputStream outfile = new FileOutputStream(path);
	    	out = new BufferedOutputStream(outfile);
		}catch(Exception e){}
		
		int bufSize = AudioRecord.getMinBufferSize(8000, 
					AudioFormat.CHANNEL_CONFIGURATION_MONO, 
					AudioFormat.ENCODING_PCM_16BIT);
		//Log.d("FAFA","bufSize="+bufSize);
		
		//bufSize=16000;
		
		//Hack:
		if (Build.MODEL.toLowerCase().contains("i897"))
			bufSize=16000;
		try{
			recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, 
					AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 
					bufSize*4);
		}catch (IllegalArgumentException e) {
			Log.e("Fail to create AudioRecord");
		}
		
		//int buffer_Size=bufSize/2;
		//Log.d("FAFA","buffer_Size="+buffer_Size);
		
		try{
			codec=new Ncodec();
			codec.load();
		}catch (Exception e){}
		
		total_size=160*(frame_rate+1);
		
		//Log.d("FAFA","total_size="+total_size);
	}
	
	public void start()
	{
		if (recorder!=null && codec.isReady())
		{
			int c=0;
			while(recorder.getState()!=AudioRecord.STATE_INITIALIZED && c++<20)
				Sleep(10);
			if (c>=20)
			{
				recorder.release(); // Now the object cannot be reused
				stop();
				return;
			}
			
			Thread thr=new Thread(null,mRecording,"RecordingVoiceMemo");
			thr.start();
		}
	}
	
	int r_ptr=0;
	int w_ptr=0;
	private short [] pcm;
	private boolean encDone = true;
	
	static private void Sleep(int ms)
    {
    	try{
			Thread.sleep(ms);
		}catch(Exception e){}
    }
	
	final Runnable mEncoding=new Runnable()
	{
		public void run() {
			int packet_size=0;
			encDone=true;
			
			Sleep(500);
			
			do{
				encDone = false;
				if (Math.abs(w_ptr-r_ptr)<=160 && bRecording){
					//Log.d("CODEC: sleep 100ms");
					Sleep(100);
					continue;
				}
				
				int sum=0;
				for(int i=0;i<160;i++)
					sum+=Math.abs(pcm[r_ptr+i]);
				
				if (sum<12)  //sw*** vmemo, 12800 to 12
				{
					encoded[data_offset]=0x7C;
					packet_size=1;
				}
				else
					packet_size=codec.encode(pcm, r_ptr, encoded, data_offset, 160);
				
				//Log.d("CODEC:"+r_ptr/160+","+packet_size);
				if (packet_size>0)
				{
					r_ptr += 160;
					r_ptr %= total_size;
					
					if (encoded[data_offset]==68)
					{
						encoded[data_offset]=0x7c;
						data_offset += 1;
					}
					else
						data_offset += packet_size;
					
					if (data_offset>77950)
					{
						//Log.d("CODEC: Exceed the encoded size");
						break;
					}
				}
				else
				{
					r_ptr += 160;
					r_ptr %= total_size;
				}
			}while(bRecording);
			
			//Log.d("CODEC: stop encoding");
			
			encDone=true;
		}
	};
	
	
	final Runnable mRecording=new Runnable(){
		public void run(){
			try{
				pcm = new short[total_size];
			}catch(OutOfMemoryError e){
				total_size=160*300;
				pcm = new short[total_size];
			};
			
			bRecording=true;
			
			encoded=new byte[78000];
			Sleep(10);
			
			data_offset=0;
			r_ptr=0;
			w_ptr=0;
			
			Thread thr=new Thread(null,mEncoding,"EncodingVoiceMemo");
			thr.start();
			
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

			//try{
				recorder.startRecording();
				do{
					recorder.read(pcm, w_ptr, 160);
					w_ptr += 160;
					w_ptr %= total_size;
					//Log.d(""+w_ptr/160);
				}while(bRecording);
			//}catch(IllegalStateException e){}
			
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
			try{
				recorder.stop();
				recorder.release(); // Now the object cannot be reused
				recorder=null;
			}catch(IllegalStateException e){
			}
		}
	};

	public void stop()
	{
		bRecording=false;
		
		int ic=0;
		while (!encDone && ic++<30)
		{
			MyUtil.Sleep(200);
		}
		codec.release();
		if (data_offset==0) return;

		if (out!=null)
		{
			String s="#!AMR\n";
			byte [] head=s.getBytes();
			try{
				out.write(head, 0, 6);
				out.write(encoded, 0, data_offset);
				out.close();
			}catch(Exception e){}
		}
		pcm=null;
		encoded=null;
		System.gc();
		System.gc();
	}
	
}
