package com.pingshow.amper;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.pingshow.amper.bean.GroupMsg;
import com.pingshow.network.MyNet;
import com.pingshow.util.MyUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class SendFileAgent {
	
	private Context mContext;
	private int myidx;
	private SMS msgsent;
	private SMS curMsg;
	
	private String mSendeeNumber;
	private String mMsgText;
	private int mAttached;
	private String mSrcAudioPath;
	
	public static long sentTime; 
	private long row_id=0;
	boolean noToastDisplay=false;
	private ArrayList <String> sendeeAddressList;
	private ArrayList <String> rowidList;
	private int mGroupID=0;



	public SendFileAgent(Context context,int my_idx,boolean hasHandler)
	{
		mContext=context;
		this.myidx=my_idx;
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
		
		int uploadSucess = 0; // 0 no attachment,-1 attachment upload fail, 1 attachment upload sucess
		if (mAttached==8)
		{
			try
    		{
				String filename = mSrcAudioPath.substring(mSrcAudioPath.lastIndexOf("/")+1).replace(" ", "");
				int count=0;
				ConversationActivity.fileUploading = true;
				do{
					MyNet net=new MyNet(mContext);
					Return=net.doPostAttach8("uploadfiles_aire.php", myidx, 
							URLEncoder.encode(filename, "UTF-8"), mSrcAudioPath, phpIP);
					if(Return.startsWith("Done"))
						break;
					count++;
				}while(count<1);
    		}catch(Exception e){
    			Log.e("doPostAttach8 uploading !@#$  "+e.getMessage());
    		}
    		ConversationActivity.fileUploading = false;
    		Log.d("Return=原来的"+Return);
    		if (Return.startsWith("Done"))
	        {
    			uploadSucess = 1;
    			remoteAudioPath=Return.substring(5);
        		upload_done|=8;
	        }else
	        	uploadSucess = -1;
		}
		
		synchronized(lock_200){
			try{
				lock_200.wait(1000);
			}catch (Exception e){
			}
		}
		if(uploadSucess == -1){
			// notify user sms send fail
			Intent it = new Intent(Global.Action_SMS_Fail);
			mContext.sendBroadcast(it);
			ConversationActivity.fileUploading = false;
			return;
		}
		
		if (!multiple)
		{
			Log.d("Starts to CMD_TRIGGER_SENDEE");
			Intent it = new Intent(Global.Action_InternalCMD);
			it.putExtra("Command", Global.CMD_TRIGGER_SENDEE);
			it.putExtra("Sendee", mSendeeNumber);
			it.putExtra("row_id", row_id);
			it.putExtra("MsgText", mMsgText);
			it.putExtra("Attached", mAttached);
			it.putExtra("remoteAudioPath", remoteAudioPath);
			it.putExtra("remoteImagePath", remoteImagePath);
			it.putExtra("phpIP",phpIP);
			mContext.sendBroadcast(it);	
		}else{
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
					it.putExtra("Attached", mAttached);
					it.putExtra("remoteAudioPath", remoteAudioPath);
					it.putExtra("remoteImagePath", remoteImagePath);
					it.putExtra("phpIP",phpIP);
					mContext.sendBroadcast(it);
					MyUtil.Sleep(500);
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
		
		mSendeeNumber=sendeeNumber;
		mMsgText=msgtext;
		mAttached=attached;
		mSrcAudioPath=srcAudioPath;
		noToastDisplay=noToast;
		
		Thread thr = new Thread(null, mSendingThread, "CheckingMember...");
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

	//jack 2.4.51 group发送文件
	public boolean onGroupSend(final GroupMsg groupMsg) {
		Log.d("发送消息  发送消息的内容: "+groupMsg.toString());
		//真不喜欢这样编程,ios改动,强行修改
		groupMsg.setCt(groupMsg.getCt()+"\n"+groupMsg.getUrl().substring(groupMsg.getUrl().lastIndexOf("/")+1));
		if (groupMsg.getCt().length()==0 && groupMsg.getAt().equals("0")) return false;
		// TODO: 2016/3/30  jack 发送 文件 消息
		Thread thr = new Thread(new Runnable(){
			public void run()
			{
				UploadMessageToServer(groupMsg);
			}
		}, "mMultipleSendingThread...");
		thr.start();
		return true;

	}

	//将指定的文件发送到服务器
	private void UploadMessageToServer(GroupMsg groupMsg) {
		//1.上传文件
		String remoteFilePath="";
		String Return="";
		String url="";

		String phpIP = null;
		if (AireJupiter.getInstance() != null) {  //tml*** china ip
			phpIP = AireJupiter.getInstance().getIsoPhp(1, true, null);
		} else {
			phpIP = AireJupiter.myLocalPhpServer;
		}

		int uploadSucess = 0; // 0 no attachment,-1 attachment upload fail, 1 attachment upload sucess
			try
			{
				String filename = groupMsg.getUrl().substring(groupMsg.getUrl().lastIndexOf("/")+1).replace(" ", "");
				int count=0;
				ConversationActivity.fileUploading = true;
				do{
					MyNet net=new MyNet(mContext);
					Return=net.doPostAttach8("uploadfiles_aire.php", myidx,
							URLEncoder.encode(filename, "UTF-8"),  groupMsg.getUrl(), phpIP);
					if(Return.startsWith("Done"))
						break;
					count++;
				}while(count<1);
			}catch(Exception e){
				Log.e("doPostAttach8 uploading !@#$  "+e.getMessage());
			}

			ConversationActivity.fileUploading = false;
			Log.d("Return="+Return);
			Log.d("发送消息  上传文件的结果:"+Return);

			if (Return.startsWith("Done"))
			{
				uploadSucess = 1;
				remoteFilePath=Return.substring(5);

					url ="http://"+phpIP+"/onair/ulfiles/"+remoteFilePath;

			}else
				uploadSucess = -1;
		synchronized(lock_200){
			try{
				lock_200.wait(1000);
			}catch (Exception e){
			}
		}
		if(uploadSucess == -1){
			// notify user sms send fail
			Intent it = new Intent(Global.Action_SMS_Fail);
			mContext.sendBroadcast(it);
			ConversationActivity.fileUploading = false;
			return;
		}
		//2.发送消息
		Log.d("发送消息  groupMsg.setUrl(url): "+url);
		Log.d("发送消息  groupMsg.setPath(remoteFilePath):  "+remoteFilePath);
		groupMsg.setUrl(url);
		groupMsg.setPath(remoteFilePath);
		Gson gson = new Gson();
		String msgJson = gson.toJson(groupMsg);
		Log.d("发送消息  发送850的body: "+msgJson);
		AireJupiter.getInstance().tcpSocket.send850(Integer.toHexString(mGroupID), Integer.toHexString((int) row_id),msgJson);
	}
}
