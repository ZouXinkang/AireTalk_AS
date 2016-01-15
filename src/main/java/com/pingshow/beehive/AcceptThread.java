package com.pingshow.beehive;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;

import com.pingshow.airecenter.Log;


public class AcceptThread extends Thread {
	private ServerSocket srvSock;
	private ServerSocket srvSock2;
	private Context mContext;
	private Handler mHandler;
	static public boolean Running;
	private long lastBitrate=0;
	public static ArrayList<ClientThread>users=new ArrayList<ClientThread>();
	
	AcceptThread(ServerSocket socR, ServerSocket socR2, Handler h, Context context) {
		this.srvSock = socR;
		this.srvSock2 = socR2;
		this.mContext = context;
		this.mHandler = h;
		Running=true;
	}
	
	public void stopServer()
	{
		Running=false;
		
		lastBitrate=0;
		
		for(ClientThread user: users)
		{
			user.stopTransfer();
		}
		users.clear();
	}
	
	static public void notifyTransferring(boolean transfer)
	{
		BeeHiveService.notifyTransferring(transfer);
	}
	
	public void run() {
		
		while (Running) {
			Socket clientSocket = null;
			try {
				clientSocket = srvSock.accept();
                Log.i("a new bee come here.");
			}
			catch(IOException ioe) {
				return;
			}
			Socket commSocket=null;
			try{
				commSocket=srvSock2.accept();
			} catch (IOException e) {
				e.printStackTrace();
				//Log.e(e.getMessage());
			}
			
			ClientThread t = new ClientThread(clientSocket, commSocket, mHandler, mContext);
            users.add(t);
            t.start();
            
            notifyTransferring(true);
		}
		
		stopServer();
	}
	
}
