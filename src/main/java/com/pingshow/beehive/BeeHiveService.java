package com.pingshow.beehive;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.voip.DialerActivity;

public class BeeHiveService extends Service {
    
	private int port = 9775;
	
	final static int multicastInterval=5000;
	
	private ServerSocket sockR = null;
	private ServerSocket sockR2 = null;
	private AcceptThread BeeHive;
	private Handler mHandler=new Handler();
	static private BeeHiveService instance;
	static public boolean transferring=false;
	private Handler mHandler2;
	
	static public BeeHiveService getInstance()
	{
		return instance;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		transferring=false;
		
		Log.i("AireShare Starts...");
		
		HandlerThread handlerThread = new HandlerThread("MultiSocketB");  
        handlerThread.start();  
        mHandler2 = new Handler(handlerThread.getLooper());  
        mHandler2.post(mRunnable);
        
        new Thread(new Runnable(){
        	public void run()
        	{
        		createServer();
        	}
        }).start();
        
        mHandler.postDelayed(closeServerRunnable, 180000);//3 mins
        
        instance=this;
	}
	
	private Runnable mRunnable = new Runnable() {  
        public void run() {
            try {
            	do{
            		if (DialerActivity.getDialer()!=null)
            		{
            			try{
            				Thread.sleep(10000);
            			}catch(Exception e){}
            		}
            		else
            			receiveMultiBroadcast();
            	}while(instance!=null);
            } catch (Exception e) {  
                e.printStackTrace();
            }  
        }
    };
    
    private int ticker = 15;
    protected void receiveMultiBroadcast() throws IOException {  
		MulticastSocket socket = new MulticastSocket(8601);  
		InetAddress address = InetAddress.getByName("224.0.0.1");  
		socket.joinGroup(address);
		  
		DatagramPacket packet;  
		   
		byte[] rev = new byte[512];  
		packet = new DatagramPacket(rev, rev.length); 
		socket.setSoTimeout(4000);
		try{
			socket.receive(packet);  
		}catch(Exception e){}
		
		String receiver = new String(packet.getData()).trim();
		if (receiver.length()>0)
		{
			Log.i("AireShare Receive: " + receiver);
		}
		
		MyPreference mPref=new MyPreference(this);
		String myNickname = mPref.read("myNickname");
		String localIpAddress=getLocalIpAddress();
		if (myNickname!=null && localIpAddress!=null)
		{
			localIpAddress=myNickname+"@"+localIpAddress;
		}
		byte[] buf = localIpAddress.getBytes();  
		packet = new DatagramPacket(buf, buf.length, address, 8600);  
		socket.send(packet);
//		Log.i("Response IP packet... '"+localIpAddress+"' done");
		//tml***  60s < 15x4s
		if ((ticker % 15) == 0) {
			Log.i("Response IP packet... '"+localIpAddress+"' done");
			ticker = 0;
		}
		ticker++;
		//***tml
		
		socket.leaveGroup(address);  
		socket.close();  
		
		if (receiver.length()>0)
		{
			if (receiver.contains("start"))
			{
				mHandler.removeCallbacks(closeServerRunnable);

				new Thread(new Runnable(){
		        	public void run()
		        	{
		        		createServer();
		        	}
		        }).start();
			}
			else if (receiver.contains("bye"))
			{
				Log.i("AireShare bye");
				mHandler.postDelayed(closeServerRunnable, 180000);//3 mins
			}
		}
    }
    
    Runnable closeServerRunnable=new Runnable()
    {
    	public void run()
    	{
    		closeServer();
    		Log.i("AireShare Server CLOSED... ");
    	}
    };
    
    
    public String getLocalIpAddress() {
		try {
			Enumeration<NetworkInterface> interfaces;
			interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()){
			    NetworkInterface current = interfaces.nextElement();
			    try{
			    	if (!current.isUp() || current.isLoopback() || current.isVirtual()) continue;
			    }catch(Error e){}
			    Enumeration<InetAddress> addresses = current.getInetAddresses();
			    while (addresses!=null && addresses.hasMoreElements()){
			        InetAddress current_addr = addresses.nextElement();
			        if (current_addr instanceof Inet4Address && !current_addr.isLoopbackAddress())
			        	return current_addr.getHostAddress();
			    }
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	    return null;
	}
	
	public boolean isConnectInternet(Context context) {
		ConnectivityManager conManager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = conManager.getActiveNetworkInfo();
		if (networkInfo != null ){
			return networkInfo.isAvailable();
		}
		return false ;
	}
	
	static public void notifyTransferring(boolean transfer)
	{
		transferring=transfer;
	}
	
	public void createServer()
	{
		if (!isConnectInternet(this)) return;
		
		if (sockR==null || sockR2==null)
		{
			try {
				sockR = new ServerSocket(port);
				sockR.setReuseAddress(true);
				sockR.setReceiveBufferSize(202999);
				sockR.setSoTimeout(0);		//time out infinite
				
				sockR2 = new ServerSocket(port+2);
				sockR2.setReuseAddress(true);
				sockR2.setSoTimeout(0);
				BeeHive=new AcceptThread(sockR, sockR2, mHandler, this);
				BeeHive.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			Log.i("Server listen READY... ");
		}
	}
	
	public void closeServer()
	{
		if (transferring) return;
		
		if (BeeHive!=null)
			BeeHive.stopServer();
		if (sockR!=null)
		{
			try{
				sockR.close();
			} catch (IOException e) {}
		}
		
		if (sockR2!=null)
		{
			try{
				sockR2.close();
			} catch (IOException e) {}
		}
		sockR=null;
		sockR2=null;
	}

	@Override
	public void onDestroy() {
		try {
			 closeServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		instance=null;
		Log.i("AireShare closed...");
		System.gc();
		System.gc();
		super.onDestroy();
	}
}
