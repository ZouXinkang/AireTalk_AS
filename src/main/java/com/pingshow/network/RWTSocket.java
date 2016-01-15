package com.pingshow.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.contacts.RWTOnline;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.WTHistoryDB;
import com.pingshow.codec.XTEA;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.DialerActivity;

public class RWTSocket {

	static int CLIENT_CONNECTION_TIMEOUT = 0;
	static int TRANSIT_TIMEOUT = 15000; // 10 sec
	
	static int alter = 0;
	static int failCount = 0;

	static public String ServerIP = "223.4.90.214";
	
	private Socket clientSocket = null;
	private DataInputStream inFromServer;
	private DataOutputStream outToServer;

	public int logged = 0;
	public int Logging = 0;
	
	public String myPhoneNumber;
	public String myPasswd;
	public String mySipServer;

	private Context mContext;
	private AmpUserDB mADB;
	private WTHistoryDB mWTDB;
	private int myidx;
	private String myId="xxxxx";
	private SocketCommThread thrClient = null;
	private MyPreference mPrf;
	private byte[] buffer;
	private String iso="en";
	
	public RWTSocket(String phoneNumber, String passwd, Context context, AmpUserDB ampDB, WTHistoryDB wtDB) {
		myPhoneNumber = phoneNumber;
		myPasswd = passwd;
		mContext = context;
		mADB = ampDB;
		mWTDB = wtDB;
		mPrf = new MyPreference(context);
		myId = mPrf.read("myID","");
		buffer = new byte[2146];
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
					Log.i("RWT fromServer timeout...");
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
					try {
						String items[] = null;
						items = fromServer.split("/");
						Intent it = new Intent(Global.Action_Chatroom_Members);
						it.putExtra("num", Integer.parseInt(items[1]));
						mContext.sendBroadcast(it);
						Log.i("<410> Enter Room: "+iso+", "+channel);
					}catch(Exception e){}
					
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
	
	public synchronized boolean Login(int versionCode) {
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
			ServerIP=mPrf.read("RWTServerIP","223.4.90.214");
			
			Log.i("TCP Socket ServerIP=" + ServerIP + ":" + port);
			InetSocketAddress isa = new InetSocketAddress(ServerIP, port);
			
			clientSocket = new Socket();
			try {
				Log.i("Connecting...");
				
				clientSocket.setTcpNoDelay(true);
				clientSocket.setReuseAddress(true);
				
				clientSocket.connect(isa, TRANSIT_TIMEOUT);
	
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
			myidx=Integer.parseInt(myId, 16);
			failCount=0;
			
			try {
				clientSocket.setSoTimeout(CLIENT_CONNECTION_TIMEOUT);
				clientSocket.setKeepAlive(true);
				clientSocket.setTrafficClass(4);
			} catch (Exception e) {}

			thrClient = new SocketCommThread();
			thrClient.start();

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
	
	public void setChannel(String lang, int ch)
	{
		this.iso=lang;
		this.channel=ch;
	}
	
	public int getCurrentChannel()
	{
		return (iso.charAt(0)<<24)|(iso.charAt(1)<<16)|this.channel;
	}
	
	public void sendRaw(int receiver, byte [] payload, int seq, int length) {
		if (Logging==1 || logged==0) return;
		
		int size=length+23;
		byte [] buffer=new byte[size];
		
		int flag=0;
		if (receiver==0)//public
		{
			flag=(iso.charAt(0)<<24)|(iso.charAt(1)<<16)|this.channel;
		}
		
		Random r = new Random();
		short key1=(short)r.nextInt(255);
		short key2=(short)(255^key1);
		
		buffer[0]=(byte) (size & 0xff);
		buffer[1]=(byte) ((size>>8) & 0xff);
		
		XTEA.writeUnsignedInt(0xFFFFFFFF,buffer,2);
		XTEA.writeUnsignedInt(myidx,buffer,6);
		XTEA.writeUnsignedInt(receiver,buffer,10);
		XTEA.writeUnsignedInt(flag,buffer,14);
		XTEA.writeShortInt(key1,buffer,18);
		XTEA.writeShortInt(key2,buffer,20);
		
		buffer[22]=(byte)seq;
		
		System.arraycopy(payload, 0, buffer, 23, length);
		try {
			outToServer.write(buffer);
			Log.i("Socket: sending Raw..."+seq+","+(length+23)+",iso="+iso+",channel:"+this.channel+",receiver="+receiver);
		} catch (Exception e) {
			Log.e("Fail to sendRaw...");
			disconnect();
		}
	}
	
	int curIdx;
	
	Runnable triggerPrivateWT=new Runnable(){
		public void run()
		{
			if (RWTOnline.getContactOnlineStatus(curIdx)==0)
				RWTOnline.setContactOnlineStatus(curIdx,3);
			String address=mADB.getAddressByIdx(curIdx);
			
			/*
			if (WalkieTalkieDialog.getInstance()==null)
			{
				if (mPrf.readBoolean("wtSoundOut", true))
				{
					Intent i = new Intent(mContext, WalkieTalkieDialog.class);
					i.putExtra("Address", address);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
					mContext.startActivity(i);
				}
				else
				{
					Intent it = new Intent(Global.Action_InternalCMD);
					long now=new Date().getTime()/1000;
					it.putExtra("Command", Global.CMD_TCP_MESSAGE_ARRIVAL);
					it.putExtra("originalSignal", "210/"+Integer.toHexString(curIdx)+"/"+Integer.toHexString((int)now)+"/<Z>(iPh)");
					mContext.sendBroadcast(it);
					return;
				}
			}
			else
			{
				Bundle b=new Bundle();
				b.putString("Address", address);
				WalkieTalkieDialog.getInstance().refresh(b);
			}*/
		}
	};
	
	public void recvRaw(byte [] buffer)
	{
		int len=(int)XTEA.readShortInt(buffer, 0);
		
		if (len<=23 || len>1560) return;
		
		if (DialerActivity.getDialer()==null)
		{
			int toMe=XTEA.readUnsignedInt(buffer, 10);
			int seq=buffer[22];
			int idx=XTEA.readUnsignedInt(buffer, 6);
			
			if (seq==0)//seq
			{
				if (toMe>0)//private
				{
					curIdx=idx;
					new Thread(triggerPrivateWT).start();
				}
				else
					return;//discard
			}
			
			Log.i("Raw data received seq="+seq+", "+len);
			
			if (len<=1523 && len>23)
			{
				Intent it=new Intent(Global.Action_Raw_Audio_Playback);
				it.putExtra("raw", buffer);
				it.putExtra("length", len);
				it.putExtra("from", idx);
				it.putExtra("seq",seq);
				it.putExtra("private", toMe>0);
				mContext.sendBroadcast(it);
			}
		};
	}


	static void Sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
		}
	}

	public boolean disconnect() {
		
		if (Logging==1) return false;
		
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

		Log.e("rwt Disconnected...");

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
	
	int channel=1;
	
	private final Object lock_400 = new Object();
	public boolean channelSwitch(int ch, String iso) {
		if (Logging==1 || logged==0) return false;
		
		this.channel=ch;
		this.iso=iso;
		int flag=(iso.charAt(0)<<24)|(iso.charAt(1)<<16)|ch;
		try {
			thrClient.reset400();
			String buffer = "400/" + myId + "/" + Integer.toHexString(flag);			
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
		if (Logging==1 || logged==0) return false;
		
		int flag=(iso.charAt(0)<<24)|(iso.charAt(1)<<16)|channel;
		try {
			String buffer = "420/" + myId + "/" + Integer.toHexString(flag);			
			Log.i(buffer);
			outToServer.write(MyUtil.encryptTCPCmd(buffer));
		} catch (Exception e) {
			Log.e("FailtoSendtoServer.420");
			return false;
		}
		return true;
	}

	private final Object lock_450 = new Object();
	private String friendsIdx = "";

	public synchronized boolean queryFriendsOnlineStatus() {
		if (logged != 1 || Logging==1) return false;
		long now = new Date().getTime();
		long last = mPrf.readLong("last_rwt_query_status", 0);
		if (now - last < 15000) // 15 sec
			return true;// no need to update

		friendsIdx = "";

		Log.i("Check RWT online friends...");
		Cursor cursor = mWTDB.fetchRecent(10);
		if (cursor.moveToFirst()) {
			do {
				if (friendsIdx.length() > 0) {
					friendsIdx += "&";
				}
				friendsIdx += Integer.toHexString(cursor.getInt(3));
			} while (cursor.moveToNext());
		}
		if(cursor!=null && !cursor.isClosed())
			cursor.close();

		if (friendsIdx.length() > 0) {

			Log.i("450/" + myId + "/" + friendsIdx);
			thrClient.reset450();
			try {
				outToServer.flush();//alec
				outToServer.write(MyUtil.encryptTCPCmd("450/" + myId + "/" + friendsIdx));
			} catch (Exception e) {
				Log.e("RWT Fail to getFriendsOnlineStatus");
				disconnect();
				return false;
			}

			synchronized (lock_450) {
				try {
					lock_450.wait(TRANSIT_TIMEOUT);
				} catch (InterruptedException e) {
				}
			}

			if (!thrClient.isReady450()) {
				Log.e("RWT getFriendsOnlineStatus Timeout");
				friendsIdx = "";
				disconnect();
				return false;
			}
		}

		mPrf.writeLong("last_rwt_query_status", now);
		return true;
	}
}
