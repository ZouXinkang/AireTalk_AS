package com.pingshow.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.contacts.RWTOnline;
import com.pingshow.codec.XTEA;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.VideoConf;

public class VideoConfSocket {
	
	static public final int MAX_USERS = 4;

	static int CLIENT_CONNECTION_TIMEOUT = 0;
//	static int TRANSIT_TIMEOUT = 15000; // 10 sec
	static int TRANSIT_TIMEOUT = 3000; // 10 sec //tml|alex|sw*** vid timeout/
	
	static int alter = 0;
	static int failCount = 0;

	static public String ServerIP = "74.3.162.130";
	
	private Socket clientSocket = null;
	private DataInputStream inFromServer;
	private DataOutputStream outToServer;
	
	private ArrayList<byte[]> mOutputQueue=new ArrayList<byte[]>();

	public int logged = 0;
	public int Logging = 0;
	
	public String myPhoneNumber;
	public String myPasswd;
	public String mySipServer;

	private Context mContext;
	private int myidx;
	private String myId="0";
	private SocketCommThread thrClient = null;
	private MyPreference mPrf;
	private byte[] buffer;
	private int chflag;
	
	private VideoConf videoConf;
	private byte [][] imageData;
	private int [] imagePos;
	
	private boolean sending=false;
	
	public VideoConfSocket(String phoneNumber, String passwd, Context context, VideoConf videoConf) {
		myPhoneNumber = phoneNumber;
		myPasswd = passwd;
		mContext = context;
		mPrf = new MyPreference(context);
		myId = mPrf.read("myID","");
		buffer = new byte[2146];
		this.videoConf=videoConf;
		
		imageData=new byte[MAX_USERS][256000];
		imagePos=new int[MAX_USERS];
		
		for (int i=0;i<MAX_USERS;i++)
			imagePos[i]=0;
	}

	public class SocketCommThread extends Thread {
		boolean ready;
		boolean ready450;
		boolean ready400;
		boolean Running = true;

		public void reset() {
			ready = false;
		}

		public boolean isReady() {
			return ready;
		}

		public void reset450() {
			ready450 = false;
		}
		
		public void reset400() {
			ready400 = false;
		}

		public boolean isReady450() {
			return ready450;
		}
		
		public boolean isReady400() {
			return ready400;
		}

		public void terminate() {
			Running = false;
		}

		public void run() {
			String fromServer;
			Running = true;
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
			
			do {
				fromServer = "";
				if (inFromServer==null || buffer==null)
				{
					Running = false;
					disconnect();
					return;
				}
				try {
					inFromServer.read(buffer,0,2);
					
					int len=XTEA.readShortInt(buffer, 0);
					if (len < 0 || len > 1560) { //tml|alex*** drop header/
						continue;
					}
					
					if (len>2)
						inFromServer.readFully(buffer,2,len-2);
					else
						continue;
					
					if (XTEA.readUnsignedInt(buffer, 2)==-1)
						recvRaw(buffer);
					else {
						if (len<64 && len>4)
							fromServer = MyUtil.decryptTCPCmd(buffer);
						else
						{
							int bytes=inFromServer.available();
							inFromServer.skip(bytes);
							continue;
						}
					}
				} catch (Exception e) {
					if (!Running) return;
					Log.i("fromServer timeout...");
					Running = false;
					disconnect();
					return;
				}

				if (fromServer == null)
					continue;

				Log.i(fromServer);

				if (fromServer.startsWith("460")) {
					// split friends:
					try{
						if (friendsIdx!=null && friendsIdx.length() > 0)
						{
							Pattern and = Pattern.compile("\\&");
							String[] idxs = and.split(friendsIdx);
		
							if (idxs.length > 0) {
								fromServer = fromServer.substring(4);
								for (int k = 0; k < fromServer.length(); k++) {
									if (k < idxs.length) {
										char a = fromServer.charAt(k);
										int online_type = 0;
										if (a >= '0' && a <= '4')
											online_type = a - '0';
										try{
											RWTOnline.setContactOnlineStatus(Integer.parseInt(idxs[k],16), online_type);
										}catch(Exception e){}
									}
								}
							}
						}
					}catch(Exception e){}
					friendsIdx = "";
					ready450 = true;
					synchronized (lock_450) {
						lock_450.notifyAll();
					}
				}
				else if (fromServer.startsWith("410"))
				{
					Log.i("<410> Enter Room: "+chflag);
					ready400 = true;
					synchronized (lock_400) {
						lock_400.notifyAll();
					}
				}
				else if (fromServer.startsWith("430"))
				{
					Log.i("<430> Leave Previous Room ");
				}
			} while (Running);
		}
	}

	static int VersionCode = 32;

	public synchronized boolean Login() {
		return Login(VersionCode);
	}
	
	public boolean Login(int versionCode) {
		VersionCode = versionCode;
		
		if (logged==1) return true;
		myId = mPrf.read("myID","");
		if (myId.length()==0) return false;
		if (myPhoneNumber.equals("----")) return false;
		
		String fromServer = "";
		logged = 0;
		Logging = 1;
		
		int count=0;
		boolean connected=false;
		while(count++<2 && !connected)
		{
			int port=mPrf.readInt("RWTServerPort",9335);
			ServerIP="74.3.162.130";
			
			Log.i("TCP Socket ServerIP=" + ServerIP + ":" + port);
			InetSocketAddress isa = new InetSocketAddress(ServerIP, port);
			
			clientSocket = new Socket();
			try {
				Log.i("Connecting...");
				
				clientSocket.setTcpNoDelay(true);
				clientSocket.setReuseAddress(true);
				clientSocket.setSoTimeout(0);
				clientSocket.setSendBufferSize(1280000);
				clientSocket.setReceiveBufferSize(1280000);
				
				clientSocket.connect(isa, 0);
	
				outToServer = new DataOutputStream(clientSocket.getOutputStream());
				inFromServer = new DataInputStream(clientSocket.getInputStream());
				
				connected=true;
			} catch (Exception e) {
				Log.e("Socket Create Failed :" + e.getMessage());
				Logging = 0;
				alter++;
			}
		}

		try {
			clientSocket.setSoTimeout(TRANSIT_TIMEOUT+TRANSIT_TIMEOUT);
			String xcmd = "101/" + myId + '/' + myPasswd + '/'
					+ new NetInfo(mContext).netType + "/a/" + versionCode;
			
			Log.i(xcmd);
			byte[] encryptStr = MyUtil.encryptTCPCmd(xcmd);
			outToServer.write(encryptStr);
			inFromServer.read(buffer);
			fromServer = MyUtil.decryptTCPCmd(buffer);
			Log.i("Login: " + fromServer);
		} catch (Exception e) {
			Log.e("Login Failed :" + e.getMessage());
			disconnect();
			Logging = 0;
			return false;
		}

		if (fromServer == null) // login failed
		{
			Log.e("Login, Failed :NULL");
			disconnect();
			Logging = 0;
			return false;
		} 
		else if (fromServer.startsWith("999") && !MyTelephony.isPhoneNumber(myPhoneNumber)) // login failed
		{
			Log.e("Login, Failed : 999/" + fromServer.substring(3));
			//disconnect();
			Logging = 0;
			return false;
		}

		if (fromServer.startsWith("110")) {
			
			logged=1;
			Log.i("RWT server login successfully." + fromServer);
			try{
				myidx=Integer.parseInt(myId, 16);
			}catch(Exception e)
			{
				myidx=0;
			}
			failCount=0;
			
			try {
				clientSocket.setSoTimeout(CLIENT_CONNECTION_TIMEOUT);
				clientSocket.setKeepAlive(true);
				clientSocket.setTrafficClass(4);
			} catch (Exception e) {}

			thrClient = new SocketCommThread();
			thrClient.start();
			
			new Thread(sendSocketThread).start();

			Intent it2 = new Intent(Global.Action_InternalCMD);
			it2.putExtra("Command", Global.CMD_TCP_CONNECTION_UPDATE);
			mContext.sendBroadcast(it2);

			Logging = 0;
			return true;
		}

		Logging = 0;
		return false;
	}

	public boolean isLogged() {
		return (logged == 1);
	}
	
	public int getCurrentChannelFlag()
	{
		return this.chflag;
	}
	
	public int sendRaw(byte [] payload, int offset, int seq, int length, int frames) {
		if (Logging==1 || logged==0) return 0;
		
		if (this.chflag==0) return 0;
		
		int size=length+23;
		byte [] buffer=new byte[size];
		
		int flag=this.chflag;
		
		/*
		Random r = new Random();
		short key1=(short)r.nextInt(255);
		short key2=(short)(255^key1);
		*/
		
		buffer[0]=(byte) (size & 0xff);
		buffer[1]=(byte) ((size>>8) & 0xff);
		
		XTEA.writeUnsignedInt(0xFFFFFFFF,buffer,2);
		XTEA.writeUnsignedInt(myidx,buffer,6);
		XTEA.writeUnsignedInt(0,buffer,10);
		XTEA.writeUnsignedInt(flag,buffer,14);
		XTEA.writeUnsignedInt(frames,buffer,18);
		
		buffer[22]=(byte)seq;
		
		System.arraycopy(payload, offset, buffer, 23, length);
		mOutputQueue.add(buffer);
		//Log.i("Socket: sending Raw..."+seq+","+(length+23)+",flag:"+this.chflag+",receiver="+receiver);
		return 0;
	}
	
	int curIdx;
	
	Runnable sendSocketThread=new Runnable(){
		public void run()
		{
			sending=true;
			do {
				if (outToServer!=null && mOutputQueue.size()>0)
				{
					byte[] buffer=mOutputQueue.remove(0);
					try {
						outToServer.write(buffer);
						Sleep(1);
					} catch (Exception e) {
						Log.e("Fail to send socket... "+e.getLocalizedMessage());
					}
					
					buffer=null;
					System.gc();
				}
				else
					Sleep(3);
			}while(sending);
			Log.d("sendSocketThread stops");
			
			mOutputQueue.clear();
		}
	};
	
	private boolean numChanged=false;
	private int queues[]=new int[MAX_USERS];
	private int getQueueByIdx(int idx)
	{
		int a=0;
		for (int q:queues)
		{
			if (idx==q)
			{
				return a;
			}
			a++;
		}
		a=0;
		for (int q:queues)
		{
			if (q==0)
			{
				numChanged=true;
				queues[a]=idx;
				return a;
			}
			a++;
		}
		return 0;
	}
	
	public boolean isChanged()
	{
		return numChanged;
	}
	
	public void reset()
	{
		numChanged=false;
	}
	
	public void rearrangeMembers(List<Map<String,Object>> memberList)
	{
		int real[]=new int[MAX_USERS];
		int a=0;
		for (Map<String,Object> m : memberList)
		{
			String IDX=(String)m.get("idx");
			int idx=0;
			try{
				idx=Integer.parseInt(IDX);
				real[a]=idx;
				a++;
			}catch(Exception e){}
		}
		
		boolean found=false;
		a=0;
		for (int q:queues)
		{
			for (int r:real)
			{
				if (r==q)
				{
					found=true;
					break;
				}
			}
			
			if (!found)
			{
				queues[a]=0;
				numChanged=true;
				return;
			}
			a++;
		}
	}
	
	public int getNumOfStreams()
	{
		int a=0;
		for (int q:queues)
		{
			if (q==0)
			{
				return a;
			}
			a++;
		}
		return a;
	}
	
	public void resetImagePos(int index)
	{
		imagePos[index]=0;
	}
	
	public int getIdxFromIndex(int index)
	{
		return queues[index];
	}
	
	public void recvRaw(byte [] buffer)
	{
		int len=(int)XTEA.readShortInt(buffer, 0);
		if (len<=23 || len>1560) return;
		
		int payloadLength=len-23;
		
		int seq=(int)buffer[22];
		int fromIdx=XTEA.readShortInt(buffer,6);
		int index=getQueueByIdx(fromIdx);
		
		System.arraycopy(buffer, 23, imageData[index], imagePos[index], payloadLength);
		imagePos[index]+=payloadLength;
		
		if (seq==0)
		{
			try{
				int size=imagePos[index];
				byte[]jpg=new byte[size];
				System.arraycopy(imageData[index], 0, jpg, 0, size);
				videoConf.mInputQueue[index].add(jpg);
				//Log.d(">>>> jpegSize: "+size);
				jpg=null;
			}catch(OutOfMemoryError e){
				Log.e("OutOfMemoryError");
			}
			imagePos[index]=0;
		}
	}

	static void Sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
		}
	}

	public boolean disconnect() {
		
		if (Logging==1) return false;
		sending=false;
		
		if (thrClient != null)// alec
			thrClient.terminate();
		
		try {
			leaveChannel();
			if (outToServer != null)
			{
				Log.i("Say Goodbye to server");
				outToServer.write(MyUtil.encryptTCPCmd("300/" + myId));
			}
		} catch (Exception e) {
			Log.w("fail to say goodbye");
		}

		try {// alec
			if (clientSocket != null) {
				clientSocket.shutdownInput();
				clientSocket.shutdownOutput();
			}
		} catch (Exception e) {
		}

		try {
			if (inFromServer != null) {
				inFromServer.close();
				inFromServer = null;
			}
		} catch (Exception e) {
		}

		try {
			if (outToServer != null)
				outToServer.close();
			outToServer = null;
		} catch (Exception e) {
		}

		try {
			if (clientSocket != null)
				clientSocket.close();
			clientSocket = null;
		} catch (Exception e) {
		}
		logged = 0;

		Log.e("vcs Disconnected...");

		if (++failCount<5)
		{
			if (AireJupiter.getInstance() != null)
				AireJupiter.getInstance().notifyReconnectRWTServer();
		}
		else
			failCount=0;
		
		mPrf.writeLong("last_rwt_query_status", 0);
		return true;
	}
	
	private final Object lock_400 = new Object();
	
	public boolean channelSwitch(int chFlag) {
		if (Logging==1 || logged==0) return false;
		
		this.chflag=chFlag;
		
		try {
			thrClient.reset400();
			String buffer = "400/" + myId + "/" + Integer.toHexString(chFlag);			
			Log.i(buffer);
			outToServer.flush();//alec
			outToServer.write(MyUtil.encryptTCPCmd(buffer));
		} catch (Exception e) {
			Log.e("FailtoSendtoServer.400");
			disconnect();
		}
		
		synchronized (lock_400) {
			try {
				lock_400.wait(TRANSIT_TIMEOUT+5000);
			} catch (InterruptedException e) {
			}
		}

		if (!thrClient.isReady400()) {
			Log.e("RWT channelSwitch Timeout");
			disconnect();
			return false;
		}
		return true;
	}
	
	public boolean leaveChannel() {
		if (Logging==1 || logged==0 || this.chflag==0) return false;
		
		int flag=this.chflag;
		try {
			String buffer = "420/" + myId + "/" + Integer.toHexString(flag);			
			Log.i(buffer);
			outToServer.write(MyUtil.encryptTCPCmd(buffer));
		} catch (Exception e) {
			Log.e("FailtoSendtoServer.420");
			return false;
		}
		
		this.chflag=0;
		return true;
	}

	private final Object lock_450 = new Object();
	private String friendsIdx = "";
}
