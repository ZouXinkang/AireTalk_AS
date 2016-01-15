package com.pingshow.beehive;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.URLConnection;
import java.util.ArrayList;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;

import com.pingshow.airecenter.FilePushActivity;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.ScreenSharing;
import com.pingshow.codec.XTEA;
import com.pingshow.voip.DialerActivity;

public class ClientThread extends Thread {
	private DataInputStream inStream;
	public DataOutputStream outStream;		//used for by other thread for broadcast
	public static ArrayList<byte[]> mInputQueue=new ArrayList<byte[]>();
	public static ArrayList<byte[]> mFileContentQueue=new ArrayList<byte[]>();
	private Handler mHandler;
	private Context mContext;
	public Socket soc;
	private byte[] buffer;
	private byte [] imageData;
	private int dstPos=0;
	static int PKSize=1400;
	static long byteTotal=0;
	private Socket comm;
	public static Bitmap videoImage;
	int sessionID;
	private int bufferLength;
	public boolean running = true;
	private boolean bRecvFileContent=false;
	private int mimeType;
	private int fileContentNum;
	private int AssignedFN;
	private boolean exiting=false;
	private boolean finished=false;
	private OpenVariousFile ovf=null;
	
	public ClientThread(Socket soc, Socket comm, Handler h, Context context) {
		this.mContext = context;
		this.soc = soc;
		this.comm = comm;
		this.mHandler = h;
		
		exiting=false;
		finished=false;
		
		try{
			imageData=new byte[640000];
			bufferLength=640000;
		}catch(OutOfMemoryError e){
			imageData=new byte[384000];
			bufferLength=384000;
		}
		catch(Exception e){
			imageData=new byte[384000];
			bufferLength=384000;
		}
		running=true;
		dstPos=0;
		byteTotal=0;
	}
	
	
	Runnable commandThread=new Runnable(){
		public void run()
		{
			DataInputStream inStreamComm=null;
			DataOutputStream outStreamComm=null;
			try {
				inStreamComm = new DataInputStream(comm.getInputStream());
				outStreamComm = new DataOutputStream(comm.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(e.getMessage());
			}
			
			byte [] control = new byte[32];
			
			while(running)
			{
				try
				{
					try{
						inStreamComm.readFully(control);
					}catch(EOFException e){
						Log.i("comm EOF-23");
						break;
					}
					byteTotal+=23;
					
					int len=XTEA.readShortInt(control,0)-23;
					if (len==0) break;
					
					if (XTEA.readUnsignedInt(control,2)==0xFF00FFFF)
					{	
						parseSignal(control);
					}
					else if (XTEA.readUnsignedInt(control,2)==0xFF77FFFF)
					{
						byte [] url = new byte[256];
						try{
							inStreamComm.read(url);
							parseURL(url);
						}catch(Exception e){
						}
					}
				}catch(Exception e){
					
				}
			}
		
			try {
				outStreamComm.close();
				inStreamComm.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};
	
	public void run()
	{
		Log.i("Bee workers:"+AcceptThread.users.size()+". Thread:"+this.getName());
		try {
			inStream = new DataInputStream(soc.getInputStream());
			//outStream = new DataOutputStream(w.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(e.getMessage());
		}
		
		new Thread(displayThread, "displayThread").start();
		new Thread(commandThread, "commandThread").start();
		
		buffer = new byte[23];
		int seq=0;
		boolean initonce = true;
		
		mInputQueue.clear();
		mFileContentQueue.clear();
		byteTotal=0;
		
		//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		
		byte [] control = new byte[32];
		while(running)
		{
			try
			{
				try{
					inStream.readFully(buffer);
				}catch(EOFException e){
					break;
				}
				seq=XTEA.readShortInt(buffer,21);
				byteTotal+=23;
				
				int len=XTEA.readShortInt(buffer,0)-23;
				if (len==0)
				{
					if (ScreenSharing.getInstance()!=null)
					{
						ScreenSharing.getInstance().Destroy();
						if (FilePushActivity.getInstance()!=null)
							FilePushActivity.getInstance().Destroy();
					}
					Log.i("Close ScreenSharing****");
					break;
				}
				
				if (XTEA.readUnsignedInt(buffer,2)==0xF0FFFFFF)
				{
					if (!bRecvFileContent)
					{
						mimeType=buffer[20];
						fileContentNum=XTEA.readShortInt(buffer, 18);
						AssignedFN=XTEA.readUnsignedInt(buffer, 6);
						bRecvFileContent=true;
						mHandler.post(startLaunching);
						
						new Thread(fileContentThread,"fileContentThread").start();
					}
				}
				else if (XTEA.readUnsignedInt(buffer,2)==0xFF00FFFF)
				{	
					try{
						inStream.readFully(control,0,9);
						byteTotal+=9;
						parseSignal(control);
						continue;
					}catch(EOFException e){
						Log.e("EOF");
						break;
					}
				}
				
				if (bRecvFileContent)
				{
					try{
						inStream.readFully(imageData,dstPos,len);
						dstPos+=len;
						byteTotal+=len;
					}catch(EOFException e){
						Log.e("EOF");
						break;
					}
					if (seq==0)
					{
						byte[]data=new byte[dstPos];
						System.arraycopy(imageData, 0, data, 0, dstPos);
						mFileContentQueue.add(data);
						dstPos=0;
					}
				}
				else//JPG sharing
				{
					if (dstPos+1400>=bufferLength)
						dstPos=0;
					
					if (seq!=0)
					{
						inStream.readFully(imageData,dstPos,1400);
						dstPos+=1400;
						byteTotal+=1400;
					}else{
						inStream.readFully(imageData,dstPos,len);
						dstPos+=len;
						
						try{
							byte[]jpg=new byte[dstPos];
							System.arraycopy(imageData, 0, jpg, 0, dstPos);
							mInputQueue.add(jpg);
							Log.d(">>>> jpegSize: "+jpg.length);
						}catch(OutOfMemoryError e){}
						
						byteTotal+=len;
						dstPos=0;
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				//Log.i(e.getMessage());
				
				try {
					Log.i("read Exception ****");
					inStream.close();
					soc.close();
				} catch (IOException e1) {
					Log.i("close Exception ****");
					e1.printStackTrace();
					Log.i(e1.getMessage());
				}
				running=false;
				break;
			}
		}
		
		//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
		
		running=false;
		
		Sleep(500);
		
		mInputQueue.clear();
		mFileContentQueue.clear();
		
		AcceptThread.notifyTransferring(false);
		
		try{
			AcceptThread.users.remove(this);
		}catch(Exception e2){}
		
		imageData=null;
		bRecvFileContent=false;
		
		if (ovf!=null)
		{
			Log.i("*** finalize in run");
			ovf.finalizeFile(!exiting);
			ovf=null;
		}
		
		Log.i("thread ends ****");
	}
	
	public void stopTransfer()
	{
		running=false;
		exiting=true;
		try {
			soc.shutdownInput();
		} catch (Exception e) {
		}
		
		try {
			soc.close();
		} catch (Exception e) {
		}
		
		try {
			comm.shutdownInput();
		} catch (Exception e) {
		}
		
		try {
			comm.close();
		} catch (Exception e) {
		}
	}
	
	int VolumeAdjust=0;
	int MediaControl=0;
	private void parseSignal(byte [] data)
	{
		VolumeAdjust=data[23];
		MediaControl=data[24];
		
		Log.d("MediaControl "+VolumeAdjust+", "+MediaControl);
		new Thread(parseMediaControl,"parseMediaControl").start();
	}
	
	private void parseURL(byte [] data)
	{
		String jumpURL=new String(data);
		if (ScreenSharing.getInstance()!=null)
			ScreenSharing.getInstance().launchDefaultPlayer(jumpURL);
	}
	
	Runnable parseMediaControl = new Runnable(){
		@Override
		public void run() {
			if (MediaControl==-1)
			{
				stopTransfer();
				Sleep(500);
				
				AcceptThread.notifyTransferring(false);
				
				try{
					AcceptThread.users.remove(this);
				}catch(Exception e2){}
				
				if (FilePushActivity.getInstance()!=null)
					FilePushActivity.getInstance().Destroy();
				bRecvFileContent=false;
				imageData=null;
			}
			else if (MediaControl==2)
			{
				if (FilePushActivity.getInstance()!=null)
				{
					FilePushActivity.getInstance().pause();
				}
			}
			else if (MediaControl==3)
			{
				if (FilePushActivity.getInstance()!=null)
				{
					FilePushActivity.getInstance().resume();
				}
			}
			else if (VolumeAdjust==1)
			{
				if (FilePushActivity.getInstance()!=null)
				{
					FilePushActivity.getInstance().volumeUp();
				}
			}
			else if (VolumeAdjust==-1)
			{
				if (FilePushActivity.getInstance()!=null)
				{
					FilePushActivity.getInstance().volumeDown();
				}
			}
		}
	};
	
	private void parseCommand(String command){
		FileBrowser fBrowser = new FileBrowser();
		JSONObject fjJsonObject = fBrowser.addFiles();
		try {
			outStream.write(fjJsonObject.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static public void Sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
		}
	}
	
	private Runnable startLaunching=new Runnable() {
		public void run() {
			if (ovf==null)
			{
				ovf=new OpenVariousFile(mContext, mimeType, AssignedFN);
				ovf.openFile();
			}
		}
	};
	
	private Runnable fileContentThread=new Runnable() {
		public void run() {
			int count=0;
			while(FilePushActivity.getInstance()==null || ovf==null)
				Sleep(100);
			while(!ovf.isReady())
				Sleep(100);
			
			ovf.setFileSize(fileContentNum*PKSize);
			
			boolean firstStart=true;
			do {
				if (mFileContentQueue.size()>0 && ovf!=null && ovf.isReady())
				{
					byte[] buffer=mFileContentQueue.remove(0);
					if (buffer!=null)
					{
						try{
							ovf.feedData(buffer, buffer.length);
						}catch(Exception e){}
					}
					count++;
					Log.d(count+"/"+fileContentNum);
					
					float percent=((float)count/fileContentNum);
					if (percent>=0.10 && firstStart)
					{
						ovf.start();
						firstStart=false;
					}
					if (count>=fileContentNum)
					{
						finished=true;
						break;
					}
				}
				else{
					Sleep(12);
					System.gc();
					System.gc();
				}
			}while(running);
			
			if (ovf!=null)
			{
				Log.d("*** finalize in fileContentThread");
				ovf.finalizeFile(!exiting);
				try{
					if (!finished)
						ovf.deleteFile();
				}catch(Exception e){}
				ovf=null;
			}
			
			bRecvFileContent=false;
			
			System.gc();
			System.gc();
		}
	};
	
	private Runnable displayThread = new Runnable() {
		public void run() {
			
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
			
			do {
				if (mInputQueue.size()>0)
				{
					try{
						byte[] buffer=mInputQueue.get(0);
						videoImage=BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
						if (videoImage!=null)
						{
							mHandler.post(freshThread);
						}
						buffer=null;
					}catch(Exception e){}
					
					try{
						mInputQueue.remove(0);
					}catch(Exception e){}
				}
				else{
					Sleep(12);
					System.gc();
				}
			}while(running);
			
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
			
			mInputQueue.clear();
		}
	};
	
	private Runnable freshThread=new Runnable(){
		public void run() {
			if (DialerActivity.getDialer()!=null)
			{
				stopTransfer();
				return;
			}
			if (ScreenSharing.getInstance()==null)
			{
				Intent it=new Intent(mContext,ScreenSharing.class);
				it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(it);
			}
		}
	};
}
