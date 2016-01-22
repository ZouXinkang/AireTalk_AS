package com.pingshow.amper;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;

import com.pingshow.network.MyNet;
import com.pingshow.util.MyUtil;

public class SendAgent {
	
	private Context mContext;
	private SMS msgsent;
	
	private int myidx;
	private int toidx;
	
	private String mSendeeNumber;
	private String mMsgText;
	private int mAttached;
	private String mSrcAudioPath;
	private String mSrcImagePath;
	
	public static long sentTime; 
	private long row_id=0;
	boolean noToastDisplay=false;
	private ArrayList <String> sendeeAddressList;
	private ArrayList <String> rowidList;
	private int mGroupID=0;
	
	public SendAgent(Context context,int my_idx,int to_idx,boolean hasHandler)
	{
		mContext=context;
		this.myidx=my_idx;
		this.toidx=to_idx;
	}
	
	private Object lock_200=new Object();
	
	public void setRowId(long id)
	{
		row_id=id;
		synchronized(lock_200){
			lock_200.notifyAll();
		}
	}
	
	public void setRowId(ArrayList <String> rowidlist)
	{
		rowidList=rowidlist;
		synchronized(lock_200){
			lock_200.notifyAll();
		}
	}
	
	public void setAsGroup(int groupID)
	{
		mGroupID=groupID;
	}
	
	public long getRowId()
	{
		return row_id;
	}
	
	Runnable mSendingTask = new Runnable() {
        public void run() {
        	UploadMessageToServer(false);
        }
    };
    
	
	private void UploadMessageToServer(boolean multiple)
	{
		String remoteAudioPath="";
		String remoteImagePath="";
		String Return="";
		int upload_done=0;
		
//		String phpIP = AireJupiter.myLocalPhpServer;
		String phpIP = null;
		if (AireJupiter.getInstance() != null) {  //tml*** china ip
			phpIP = AireJupiter.getInstance().getIsoPhp(1, true, null);
		} else {
			phpIP = AireJupiter.myLocalPhpServer;
		}
		Log.d("msgs.UploadMessageToServer phpIP=" + phpIP + " mAttached=" + mAttached + " multiple=" + multiple);
		
		int uploadSucess = 0; // 0 no attachment,-1 attachment upload fail, 1 attachment upload sucess
		if ((mAttached&1)==1 || ((mAttached&4)==4 && mSrcAudioPath!=null))
		{
			try
    		{
				int count=0;
				do{
					MyNet net = new MyNet(mContext);
					if (mSrcAudioPath.endsWith("amr")) {
						Return = net.doPostAttach("uploadvmemo_aire.php", myidx, toidx, 
								mSrcAudioPath, phpIP);
					} else if (mSrcAudioPath.endsWith("mp3")) {  //tml*** new vmsg
						Return = net.doPostAttach("uploadvmemomp3_aire.php", myidx, toidx, 
								mSrcAudioPath, phpIP);
					} else {
						Return = net.doPostAttach("uploadvmemo_aire.php", myidx, toidx, 
								mSrcAudioPath, phpIP);
					}
					if(Return.startsWith("Done"))
						break;
					count++;
					MyUtil.Sleep(1500);
				}while(count<4);
				
    		}catch(Exception e){}
    		
    		if (Return.startsWith("Done"))
	        {
    			uploadSucess = 1;
    			// delete voice file if voice is interphone
     			if((mAttached&4)==4){
    				File file = new File(mSrcAudioPath);
    				if(file.exists())
    					file.delete();
     			}
        		remoteAudioPath=Return.substring(5);
        		upload_done|=1;
	        }else{
	        	uploadSucess = -1;
	        	mAttached &= 0xFE;
	        	mMsgText = mMsgText.replace("(Vm)", ""); // clear voice because voide send failed
	        }
//    		Log.d("uploadvmemo.php Return="+Return);
		}
		
		if ((mAttached&2)==2)
		{
			try
    		{
				int count=0;
				do{
					MyNet net=new MyNet(mContext);
					Return=net.doPostAttach("uploadimage_aire.php", myidx, toidx,
							mSrcImagePath, phpIP); // httppost
					if(Return.startsWith("Done"))
						break;
					count++;
					MyUtil.Sleep(1500);
				}while(count<4);
    		}catch(Exception e){}
    		
    		if (Return.startsWith("Done"))
	        {
    			uploadSucess = 1;
        		remoteImagePath=Return.substring(5);
        		upload_done|=2;
	        }else{
	        	uploadSucess = -1;
	        	mAttached &= 0xFD;
	        	mMsgText = mMsgText.replace("(iMG)", ""); // clear iamge because image send failed
	        }
		}
		
		if (mAttached==8 || mAttached==9)
		{
			try
    		{
				String filename = mSrcAudioPath.substring(mSrcAudioPath.lastIndexOf("/")+1).replace(" ", "");
				int count=0;
				do{
					MyNet net=new MyNet(mContext);
					Return=net.doPostAttach8("uploadfiles_aire.php", myidx,
							URLEncoder.encode(filename, "UTF-8"), mSrcAudioPath, phpIP);
					if(Return.startsWith("Done"))
						break;
					count++;
					MyUtil.Sleep(1500);
				}while(count<4);
    		}catch(Exception e){}
    		if (Return.startsWith("Done"))
	        {
    			uploadSucess = 1;
    			remoteAudioPath=Return.substring(5);
        		upload_done|=8;
	        }else
	        	uploadSucess = -1;
    		Log.d("msgs.uploadSucess> " + uploadSucess);
		}

		synchronized(lock_200){
			try{
				lock_200.wait(1000);
			}catch (Exception e){}
		}
		
		if(uploadSucess == -1){
			// notify user sms send fail
			Intent it = new Intent(Global.Action_SMS_Fail);
			mContext.sendBroadcast(it);
			return;
		}
		
		if (!multiple)
		{
			Intent it = new Intent(Global.Action_InternalCMD);
			it.putExtra("Command", Global.CMD_TRIGGER_SENDEE);
			it.putExtra("Sendee", mSendeeNumber);
			it.putExtra("row_id", row_id);
			it.putExtra("MsgText", mMsgText);
			it.putExtra("Attached", mAttached==9?8:mAttached);// sendpendingmsg ,mAttached will is 9
			it.putExtra("remoteAudioPath", remoteAudioPath);
			it.putExtra("remoteImagePath", remoteImagePath);
			it.putExtra("phpIP",phpIP);
			mContext.sendBroadcast(it);
		}
		else{
			for (int i=0;i<sendeeAddressList.size();i++)
			{
				try{
					String sendeeAddress=sendeeAddressList.get(i);
					long rowid=row_id;
					if (mGroupID==0)
					{
						try{
							rowid=Long.parseLong(rowidList.get(i));
						}catch(Exception e){}
					}
					
					Intent it = new Intent(Global.Action_InternalCMD);
					it.putExtra("Command", Global.CMD_TRIGGER_SENDEE);
					it.putExtra("Sendee", sendeeAddress);
					it.putExtra("GroupID", mGroupID);
					it.putExtra("row_id", rowid);
					it.putExtra("MsgText", mMsgText);
					it.putExtra("Attached", mAttached==9?8:mAttached);// sendpendingmsg ,mAttached will is 9
					it.putExtra("remoteAudioPath", remoteAudioPath);
					it.putExtra("remoteImagePath", remoteImagePath);
					it.putExtra("phpIP",phpIP);
					mContext.sendBroadcast(it);
					
					Log.i("msgs.onMultipleSend: "+sendeeAddress);
					MyUtil.Sleep(200);
				}catch(Exception e){}
			}
		}
	}
	
	final Runnable mSendingThread=new Runnable(){
		public void run()
		{
			UploadMessageToServer(false);
		}
	};

	public boolean onSend(String sendeeNumber, String msgtext, int attached, String srcAudioPath, String srcImagePath, boolean noToast)
	{
		if (msgtext.length()==0 && attached==0) return false;
		int charCount = 0;
		int byteSum = 0;
		try {
			if(msgtext.getBytes("UTF-8").length>=1277){
				for(int i = 0;i<msgtext.length();i++){
					String str = msgtext.substring(i,i+1);
					if(str.getBytes("UTF-8").length==str.length()){
						charCount++;
						byteSum += 1;
					}else{
						charCount++;
						byteSum += 3;
					}
					
					if(byteSum>=1277){
						if(byteSum!=1277){
							charCount--;
						}
						msgtext = msgtext.substring(0,charCount)+"...";
						break;
					}
				}
			}
		} catch (UnsupportedEncodingException e) {}
		mSendeeNumber=sendeeNumber;
		mMsgText=msgtext;
		mAttached=attached;
		mSrcAudioPath=srcAudioPath;
		mSrcImagePath=srcImagePath;
		noToastDisplay=noToast;
		
		Thread thr = new Thread(null, mSendingThread, "mSendingThread...");
		thr.start();
		
		return true;
	}
	
	public boolean onMultipleSend(ArrayList<String> list, String msgtext, int attached, String srcAudioPath, String srcImagePath)
	{
		sendeeAddressList=list;
		if (msgtext.length()==0 && attached==0) return false;
		int charCount = 0;
		int byteSum = 0;
		try {
			if(msgtext.getBytes("UTF-8").length>=1277){
				for(int i = 0;i<msgtext.length();i++){
					String str = msgtext.substring(i,i+1);
					if(str.getBytes("UTF-8").length==str.length()){
						charCount++;
						byteSum += 1;
					}else{
						charCount++;
						byteSum += 3;
					}
					
					if(byteSum>=1277){
						if(byteSum!=1277){
							charCount--;
						}
						msgtext = msgtext.substring(0,charCount)+"...";
						break;
					}
				}
			}
		} catch (UnsupportedEncodingException e) {}
		mMsgText=msgtext;
		mAttached=attached;
		mSrcAudioPath=srcAudioPath;
		mSrcImagePath=srcImagePath;
		
		Thread thr = new Thread(new Runnable(){
			public void run()
			{
				UploadMessageToServer(true);
			}
		}, "mMultipleSendingThread...");
		thr.start();
		
		return true;
	}
	
	public SMS getSentMsg()
	{
		if (msgsent!=null)
			msgsent.time=sentTime;
		return msgsent;
	}
}
