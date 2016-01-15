package com.pingshow.codec;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;

import com.pingshow.airecenter.Log;
import com.pingshow.network.RWTSocket;

public class RealTimeWT_WB {
	
	final int frame_rate=150; //50fx3s
	final int packageSize=25;
	
	private AudioRecord recorder;
	private Scodec codec;
	public static boolean bRecording=false;
	
	private byte[] encoded;
	private int data_offset=0;
	private int frameCount=0;
	
	private int total_size;
	private int calleeIdx;
	private RWTSocket rwtSocket;
	private int skip;

	public RealTimeWT_WB(int sendeeIdx, int netType, RWTSocket socket)
	{
		int bufSize = AudioRecord.getMinBufferSize(16000, 
					AudioFormat.CHANNEL_CONFIGURATION_MONO, 
					AudioFormat.ENCODING_PCM_16BIT);
		//Hack:
		if (Build.MODEL.toLowerCase().contains("i897"))
			bufSize=16000;
		try{
			recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000, 
					AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 
					bufSize*2);
		}catch (IllegalArgumentException e) {
			Log.e("Fail to create AudioRecord");
		}
		
		int encMode=5;
		if (netType>=3)
			encMode=5;
		else if (netType>=2)
			encMode=5;
		else
			encMode=3;
		try{
			codec=new Scodec();
			codec.load(1,encMode);
		}catch (Exception e){}
		
		total_size=320*(frame_rate+1);
		calleeIdx=sendeeIdx;
		rwtSocket=socket;
		
		skip=0;
	}
	
	public void setChannel(String iso, int ch)
	{
		rwtSocket.setChannel(iso,ch);
	}
	
	public int getCurrentChannel()
	{
		return rwtSocket.getCurrentChannel();
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
	
	final Runnable mEncodingSpeex=new Runnable()
	{
		public void run() {
			int packet_size=0;
			int seq=0;
			boolean firstTime=true;
			encDone=true;
			int dummycount=0;
			
			r_ptr=0;
			w_ptr=0;
			
			do{
				if (firstTime && r_ptr==w_ptr)
				{
					Sleep(20);
					firstTime=false;
				}else{
					encDone = false;
					if (((w_ptr-r_ptr+total_size)%total_size)==320 && bRecording){
						//Log.d("CODEC: sleep 20ms");
						Sleep(20);
						continue;
					}
					Ampitude.feedAmp(pcm,r_ptr,320);
					
					if (skip<6400)
					{
						packet_size=1;
						encoded[data_offset]=0x7C;
						r_ptr += 320;
						r_ptr %= total_size;
						skip+=320;
					}
					else
						packet_size=codec.encode(pcm, r_ptr, encoded, data_offset, 320);
					
					//Log.d("CODEC encoded:"+packet_size);
					if (packet_size>0)
					{
						r_ptr += 320;
						r_ptr %= total_size;
						
						data_offset += packet_size;
						
						frameCount++;
						
						if (frameCount>=packageSize)
						{
							dummycount++;
							if (dummycount>5)
							{
								if (rwtSocket!=null)
									rwtSocket.sendRaw(calleeIdx, encoded, seq, data_offset);
								seq++;
							}
							
							data_offset=0;
							frameCount=0;
						}
					}
					else
					{
						r_ptr += 320;
						r_ptr %= total_size;
					}
				}
			}while(bRecording);
			
			if (seq==0 && frameCount<packageSize)
			{
				//too short to send
			}
			else{
				encoded[data_offset+1]=-1;
				data_offset++;
				
				if (rwtSocket!=null)
					rwtSocket.sendRaw(calleeIdx, encoded, seq, data_offset);
			}
			
			//Log.d("CODEC: stop encoding");
			
			encDone=true;
		}
	};
	
	double smin = 200;
	int s;
	int nearend;
	void filter(short[] lin,int off,int len) {
		int i,j;
		double sm = 30000;
		for (i = 0; i < len; i += 5) {
			j = lin[i+off];
			s = (Math.abs(j)<<2 + 124*s)>>7;
			if (s < sm) sm = s;
			if (s > smin) nearend = 1200;
			else if (nearend > 0) nearend--;
		}
		if (sm > 2*smin || sm < smin/2)
			smin = sm*0.0016 + smin*0.9984;
	}
	
	final Runnable mRecording=new Runnable(){
		public void run(){
			try{
				pcm = new short[total_size];
			}catch(OutOfMemoryError e){
				total_size=320*30;
				pcm = new short[total_size];
			};
			
			bRecording=true;
			
			encoded=new byte[2048];
			Sleep(10);
			
			data_offset=0;
			r_ptr=0;
			w_ptr=0;
			
			Thread thr=new Thread(null,mEncodingSpeex,"mEncodingSpeex");
			thr.start();
			
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

			recorder.startRecording();
			do{
				recorder.read(pcm, w_ptr, 320);
				//filter(pcm, w_ptr, 320);
				w_ptr += 320;
				w_ptr %= total_size;
			}while(bRecording);
			
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
			
			recorder.stop();
			recorder.release(); // Now the object cannot be reused
		}
	};

	public void stop()
	{
		bRecording=false;
		
		int ic=0;
		while (!encDone && ic++<30)
		{
			Sleep(200);
		}
		codec.release();
		Ampitude.resetAmp();

		pcm=null;
		encoded=null;
		System.gc();
		System.gc();
	}
	
}
