package com.pingshow.amper;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.pingshow.amper.bean.GroupMsg;
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
		String attachmentURL="";
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
			//jack 2.4.51 不再群发200
			for (int i=0;i<sendeeAddressList.size();i++) {
				try {
					String sendeeAddress = sendeeAddressList.get(i);
					long rowid = row_id;
					if (mGroupID == 0) {
						try {
							rowid = Long.parseLong(rowidList.get(i));
						} catch (Exception e) {
						}
					}

					Intent it = new Intent(Global.Action_InternalCMD);
					it.putExtra("Command", Global.CMD_TRIGGER_SENDEE);
					it.putExtra("Sendee", sendeeAddress);
					it.putExtra("GroupID", mGroupID);
					it.putExtra("row_id", rowid);
					it.putExtra("MsgText", mMsgText);
					it.putExtra("Attached", mAttached == 9 ? 8 : mAttached);// sendpendingmsg ,mAttached will is 9
					it.putExtra("remoteAudioPath", remoteAudioPath);
					it.putExtra("remoteImagePath", remoteImagePath);
					it.putExtra("phpIP", phpIP);
					Log.i("msgs.onMultipleSend: " + sendeeAddress+mMsgText);
					mContext.sendBroadcast(it);
					MyUtil.Sleep(200);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// TODO: 2016/3/29 加好友和发送消息都是这个逻辑,暂时弃用
//					long rowid=row_id;
//					Intent it = new Intent(Global.Action_InternalCMD);
//					it.putExtra("Command", Global.CMD_GROUP_SENDEE);
//					it.putExtra("GroupID", mGroupID);
//					it.putExtra("row_id", rowid);
//					it.putExtra("MsgText", mMsgText);
//					it.putExtra("Attached", mAttached == 9 ? 8 : mAttached);// sendpendingmsg ,mAttached will is 9
//				//jack 因为图片和语音一次只会发送一个,所以做判断
//				if (!remoteAudioPath.isEmpty()) {
//					attachmentURL = remoteAudioPath;
//					it.putExtra("attachmentURL", attachmentURL);
//				}
//				if (!remoteImagePath.isEmpty()) {
//					attachmentURL = remoteAudioPath;
//					it.putExtra("attachmentURL", attachmentURL);
//				}
//					it.putExtra("phpIP",phpIP);
//			android.util.Log.d("SendAgent", "row_id" + rowid + " Attached" + (mAttached == 9 ? 8 : mAttached) + " remoteAudioPath" + remoteAudioPath + " remoteImagePath" + remoteImagePath + " phpIP" + phpIP);
//					mContext.sendBroadcast(it);
//					MyUtil.Sleep(200);
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

	//jack 2.4.51
	public boolean onGroupSend(final GroupMsg groupMsg) {
		android.util.Log.d("发送消息", "发送消息的内容: "+groupMsg.toString());
		if (groupMsg.getCt().length() == 0 && groupMsg.getAt().equals("0")) return false;
		// TODO: 2016/3/30  jack 发送 图片 文字 消息
		Thread thr = new Thread(new Runnable() {
			public void run() {
				UploadMessageToServer(groupMsg);
			}
		}, "mMultipleSendingThread...");
		thr.start();

		return true;
	}

	//jack 用于群组消息
	private void UploadMessageToServer(GroupMsg groupMsg) {
		String remoteFilePath="";
		String Return="";
		String url="";

		//1.jack 上传图片和文字
		String attached = groupMsg.getAt();
		String attachmentURL = groupMsg.getUrl();
		String content = groupMsg.getCt();

		android.util.Log.d("SendAgent", attached + "---" + attachmentURL + "---" + content);
		
		String phpIP = null;
		if (AireJupiter.getInstance() != null) {  //tml*** china ip
			phpIP = AireJupiter.getInstance().getIsoPhp(1, true, null);
		} else {
			phpIP = AireJupiter.myLocalPhpServer;
		}
		Log.d("msgs.UploadMessageToServer phpIP=" + phpIP + " mAttached=" + attached );

		int uploadSucess = 0; // 0 no attachment,-1 attachment upload fail, 1 attachment upload sucess

		//jack 发的语音
		if (attached.equals("1"))
		{
			try
			{
				int count=0;
				do{
					MyNet net = new MyNet(mContext);
					if (attachmentURL.endsWith("amr")) {
						Return = net.doPostAttach("uploadvmemo_aire.php", myidx, toidx,
								attachmentURL, phpIP);
					} else if (attachmentURL.endsWith("mp3")) {  //tml*** new vmsg
						Return = net.doPostAttach("uploadvmemomp3_aire.php", myidx, toidx,
								attachmentURL, phpIP);
					} else {
						Return = net.doPostAttach("uploadvmemo_aire.php", myidx, toidx,
								attachmentURL, phpIP);
					}
					if(Return.startsWith("Done"))
						break;
					count++;
					MyUtil.Sleep(1500);
				}while(count<4);

			}catch(Exception e){}

			android.util.Log.d("发送消息", "上传语音的结果:"+Return);
			if (Return.startsWith("Done"))
			{
				uploadSucess = 1;
				remoteFilePath=Return.substring(5);
				url="http://"+phpIP+"/onair/vmemo/"+remoteFilePath;
			}else{
				uploadSucess = -1;
				attached=String.valueOf(Integer.parseInt(attached)& 0xFE);
				content = content.replace("(Vm)", ""); // clear voice because voide send failed
			}
		}

		//图片 jack
		if (attached.equals("2"))
		{
			try
			{
				int count=0;
				do{
					MyNet net=new MyNet(mContext);
					Return=net.doPostAttach("uploadimage_aire.php", myidx, toidx,
							attachmentURL, phpIP); // httppost
					if(Return.startsWith("Done"))
						break;
					count++;
					MyUtil.Sleep(1500);
				}while(count<4);
			}catch(Exception e){}

			android.util.Log.d("发送消息", "上传图片的结果:"+Return);

			if (Return.startsWith("Done"))
			{
				uploadSucess = 1;
				remoteFilePath=Return.substring(5);
				url = "http://"+phpIP+"/onair/mms/"+remoteFilePath;
			}else{
				uploadSucess = -1;
				attached=String.valueOf(Integer.parseInt(attached)& 0xFD);
				content = content.replace("(iMG)", ""); // clear iamge because image send failed
			}
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
		//2.发送消息
		android.util.Log.d("发送消息", "groupMsg.setUrl(url): "+url);
		android.util.Log.d("发送消息", "groupMsg.setPath(remoteFilePath):  "+remoteFilePath);
		groupMsg.setUrl(url);
		groupMsg.setPath(remoteFilePath);
		Gson gson = new Gson();
		String msgJson = gson.toJson(groupMsg);
		android.util.Log.d("发送消息", "发送850的body: "+msgJson);
		AireJupiter.getInstance().tcpSocket.send850(Integer.toHexString(mGroupID),Integer.toHexString((int) row_id),msgJson);
	}
}
