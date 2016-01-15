package com.pingshow.beehive;

import java.io.DataInputStream;

import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.ScreenSharing;
import com.pingshow.codec.XTEA;

public class WorkerThread extends Thread {
	private DataInputStream inStream;
	private byte [] imageData;
	private boolean running;
	private int index;
	private int dstPos;
	
	public WorkerThread(DataInputStream inStream, byte [] imageData, int i) {
		this.inStream=inStream;
		this.imageData=imageData;
		this.index=i;
		running=true;
	}
	
	public void run()
	{
		int seq;
		byte [] buffer=new byte[23];
		dstPos=1400*index;
		
		while(running)
		{
			try
			{
				inStream.readFully(buffer);
				seq=XTEA.readShortInt(buffer,21);
				ClientThread.byteTotal+=23;
				
				int len=XTEA.readShortInt(buffer,0)-23;
				if (len==0)
				{
					if (ScreenSharing.getInstance()!=null)
					{
						ScreenSharing.getInstance().Destroy();
						Log.i("Close ScreenSharing****");
					}
					break;
				}
				
				if (seq!=0)
				{
					inStream.readFully(imageData,dstPos,1400);
					Log.i("seq"+seq+"  ("+index+"): "+dstPos+"  "+dstPos/1400);
					dstPos+=14000;
					ClientThread.byteTotal+=1400;
				}else{
					inStream.readFully(imageData,dstPos,len);
					Log.i("seq"+seq+"  ("+index+"): "+dstPos+"  "+dstPos/1400);
					dstPos+=len;
					
					if (index==10)
					{
						try{
							byte[]jpg=new byte[dstPos];
							System.arraycopy(imageData, 0, jpg, 0, dstPos);
							ClientThread.mInputQueue.add(jpg);
							Log.i(">>>> jpegSize: "+jpg.length);
						}catch(OutOfMemoryError e){}
					}
					
					dstPos=1400*index;
				}
				
			}catch(Exception e){
				break;
			}
		}
		
	}
}
