package com.pingshow.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MainActivity;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.SMS;
import com.pingshow.airecenter.ServiceZ;
import com.pingshow.airecenter.SurveillanceDialog;
import com.pingshow.airecenter.UserPage;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.RelatedUserDB;
import com.pingshow.airecenter.db.SmsDB;
import com.pingshow.airecenter.map.LocationUpdate;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;
import com.pingshow.voip.P2P;
import com.pingshow.voip.VideoCallActivity;
import com.pingshow.voip.VideoConf;
import com.pingshow.voip.core.VoipAddress;
import com.pingshow.voip.core.VoipCall;
import com.pingshow.voip.core.VoipCore;

public class MySocket {
	private final boolean TESTNEW700 = false;

	private static final int CONN_TIMEOUT_TRIES = 2;
	private static int conn_try_count = 0;
	static int CLIENT_CONNECTION_TIMEOUT = 0;
	static int TRANSIT_TIMEOUT = 10000; // 10,15 sec //connectTO x1, socketTO x2
	static long minOutPeriod = 100;  //tml*** outToServer period/
	
	static public String ServerIP = AireJupiter.myFafaServer_default;
	public static String ServerDM_d = "", ServerIP_d = "";
	
	private Socket clientSocket = null;
	private DataInputStream inFromServer;
	private DataOutputStream outToServer;

	public boolean tcpStatus0 = true;
	private boolean tcpStatus1 = false;
	private boolean tcpStatus2 = false;
	public int logged = 0;
	public int Logging = 0;
	
	public String myPhoneNumber;
	public String myPasswd;
	public String mySipServer;
	private String SenderID;
	private String SendeeAddress;

	private Context mContext;
	private AmpUserDB mADB;
	private RelatedUserDB mRDB;
	private boolean RecvedCallACK = false;
	private boolean NotfoundACK = false;
	private boolean WaitForPush = false;
	private String myId="xxxxx";
	private SocketCommThread thrClient = null;
	private MyPreference mPref;
	private byte[] buffer;
	private String status460="";
	private boolean needUpdateStatus=false;
	private SmsDB mSmsDB;
	private ArrayList<String> ridList;
	
	public MySocket(String phoneNumber, String passwd, Context context,
			AmpUserDB ampDB, RelatedUserDB rdb,ContactsQuery cq,
			String tcpServerIP, SmsDB smsDB) {
		ServerIP = tcpServerIP;
		myPhoneNumber = phoneNumber;
		myPasswd = passwd;
		mContext = context;
		mADB = ampDB;
		mRDB = rdb;
		mSmsDB = smsDB;
		mPref = new MyPreference(context);
		myId = mPref.read("myID");
		buffer = new byte[2560];
		ridList = new ArrayList<String>();
	}

	public void updateMySipServer(String sipServer) {
		mPref.write("mySipServer", sipServer);
		if (AireJupiter.getInstance() != null)
			AireJupiter.getInstance().mySipServer = sipServer;
	}

	public class SocketCommThread extends Thread {
		boolean ready;
		boolean ready450;
		boolean ready470;
		boolean ready310;
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

		public boolean isReady450() {
			return ready450;
		}
		public void reset470() {
			ready470 = false;
		}

		public boolean isReady470() {
			return ready470;
		}
		public void reset310() {
			ready310 = false;
		}

		public boolean isReady310() {
			return ready310;
		}

		public void terminate() {
			Running = false;
		}

		public void run() {
			ArrayList<String> fromServerA = new ArrayList<String>();
			String fromServer;
			int countSpam1 = 0, countSpam2 = 0;
			final int serverSpamLimit1 = 10, serverSpamLimit2 = 100000;
			Running = true;
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);
			do {
				fromServer = "";
				if (inFromServer==null || buffer==null)
				{
					Running = false;
					Log.e("!fromServer :: NULL1!");
					disconnect("null socket", true);
					return;
				}
				try {
					buffer[0] = 0;
					inFromServer.read(buffer);
//					fromServer = MyUtil.decryptTCPCmd(buffer);
					fromServerA.addAll(MyUtil.decryptTCPCmd2(buffer));  //tml|alex*** tcp stuck bug
				} catch (Exception e) {
					if (!Running) return;
					Log.e("!fromServer :: TIMEOUT...!");
					Running = false;
					disconnect("bad socket read", true);
					return;
				}
				//tml|alex*** tcp stuck bug
				while (fromServerA.size() > 0) {
					
					try {  //tml|alex*** tcp stuck bug
						fromServer = fromServerA.remove(0);
						if (fromServer == null) throw new IOException();
					} catch (Exception e) {
						Log.e("!fromServer :: Q!@#$!");
						fromServer = null;
						fromServerA.clear();
					}
					//tml*** tcp junk debug
					if (fromServer == null) {
						countSpam1++;
						Log.e("!fromServer(" + fromServerA.size() + "):: null!@#$ " + countSpam1);
						if (countSpam1 > serverSpamLimit1) {
							if (!Running) return;
							Log.e("!fromServer :: null ERROR!!!");
							Running = false;
							countSpam1 = 0;
							disconnectAndReconnect("fromServer null", true);
							return;
						}
						continue;
					} else if (fromServer.equals("")) {
						countSpam2++;
						if (countSpam2 == 100 || countSpam2 == 200
								|| countSpam2 == 500 || countSpam2 == 1000
								|| countSpam2 == 5000 || countSpam2 == 10000
								|| countSpam2 == 50000 || countSpam2 == 100000)
							Log.e("!fromServer(" + fromServerA.size() + "):: _!@#$ " + countSpam2);
						if (countSpam2 > serverSpamLimit2) {
							if (!Running) return;
							Log.e("!fromServer :: junk ERROR!!!");
							Running = false;
							fromServerA.clear();
							countSpam2 = 0;
							disconnectAndReconnect("fromServer junk", true);
							return;
						}
						continue;
					} else {
						if (countSpam1 > 0 || countSpam2 > 0)
							Log.w("!fromServer :: !@#$ recovered! " + countSpam1 + countSpam2);
						countSpam1 = 0; countSpam2 = 0;
					}

					Log.d("@fromServer(" + fromServerA.size() + "):: " + fromServer);

					if (fromServer.startsWith("210")) {
						String items[] = null;
						Boolean isExist=false;
						try {
							items = fromServer.split("/");
							if (fromServer.contains("[<NEW") || fromServer.contains("(iPh)<Z>4"))
							{
								// No need to feedback
							}
							else
							{
								String rowid = "0";
								try {
									if (items.length>0)
									{
										String tmp = items[items.length - 1];
										if (tmp.startsWith("`"))
											rowid = tmp.substring(1);
									}
								} catch (Exception e) {}
//								isExist=ridList.contains(rowid);
//								if ("0".equals(rowid)) {
//									isExist=false;
//								}
//								if (!isExist) {
//									ridList.add(rowid);
//								}
								//tml*** rowid check
								if ("0".equals(rowid)) {
									isExist = false;
								} else {
									String ridItem = items[1] + ":" + rowid;
									if (ridList.size() > 0) {
										for (int i = 0; i < ridList.size(); i++) {
											if (ridList.get(i).equals(ridItem)) {
												isExist = true;
												break;
											}
										}
									}
									
									if (!isExist) {
										ridList.add(ridItem);
									}
								}
								//***tml
								try{
									outToServerPeriod();  //tml*** outToServer period/
									outToServer.write(MyUtil.encryptTCPCmd("220/"
										+ items[1] + "/" + myId + "/" + rowid));
									Log.d("220/" + items[1] + "/" + myId + "/" + rowid);
								} catch (Exception e) {
									Log.e("Failed to send 220 ack back...");
									disconnect("fail 220", true);
									return;
								}
							}
						} catch (Exception e) {
							Log.e("data paring error...");
							continue;
						}

						if (items.length>1)
						{
							int idx = Integer.parseInt(items[1], 16);
							if (mADB.isOpen() && AireJupiter.getInstance()!=null)
							{
								Log.d("items210 = " + items[3] + " idx=" + idx);
								if (mADB.isUserBlocked(idx)==1 || mRDB.isUserBlocked(idx)==1) {//User in black list
									Log.e("addF.UserBlocked! UserBlocked!");
									continue;
								}
								if (items[3].startsWith("<Z>[<NEWUSERJOINS>]"))
								{
									try{
										String [] info=items[3].split("<Z>", 4);
										int uid=Integer.parseInt(info[1].substring(16),16);
					        			if (mADB.insertUser(info[2],uid,info[3])>0)
					        				mRDB.deleteContactByAddress(info[2]);
					        			ContactsOnline.setContactOnlineStatus(info[2], 2);
					        			
					        			String localfile = Global.SdcardPath_inbox + "photo_" + uid + ".jpg";
										File f=new File(localfile);
										if(!f.exists()) 
										{
											String remotefile = "profiles/thumbs/photo_" + uid + ".jpg";
											try{
												int success=0;
												int count=0;
												do {
													MyNet net = new MyNet(mContext);
													success = net.Download(remotefile, localfile, AireJupiter.myLocalPhpServer);
													if (success==1||success==0)
														break;
													MyUtil.Sleep(500);
												} while (++count < 2);
												
												if (success!=1)
												{
													count=0;
													do {
														MyNet net = new MyNet(mContext);
														success = net.Download(remotefile, localfile, null);
														if (success==1||success==0)
															break;
														MyUtil.Sleep(500);
													} while (++count < 2);
												}
											}catch(Exception e){}
										}
										
										Intent intent = new Intent(Global.Action_Refresh_Gallery);
										mContext.sendBroadcast(intent);
										
									}catch(Exception e){}
									continue;

								} else if(items[3].startsWith("<Z>[<activeCall>]")) {//alex push
									RecvedCallACK = true;
					    			Log.e("receive active call3");

//					    			if (items[3].startsWith("<Z>[<activeCall>]:2")){//request if call is active or not
//
//					    			} else if(items[3].startsWith("[<activeCall>]:1")) {//this may never be used on android //1 means that call is still active
//					    				
//					    			} else if(items[3].startsWith("[<activeCall>]:0")) {//0 call is not active
//					    				
//					    			}
					    			continue;
								}
								else if (!mADB.isFafauser(idx))//unknown person
								{
//									try {
//										outToServerPeriod();  //tml*** outToServer period/
//										Log.d("addF.unknown-200 > 380");
//										outToServer.write(MyUtil.encryptTCPCmd("380/" + items[1]));
//									} catch (IOException e) {
//										Log.e("write 380 fail");
//										disconnect("fail1 380", true);
//										return;
//									}
//									
//									try {
//										inFromServer.read(buffer);
//										String fromServer390 = MyUtil.decryptTCPCmd(buffer);
//										String address=fromServer390.split("/")[1];
//										responseStranger(address,idx);
//										//continue; //alec: allow stangers
//									} catch (Exception e) {
//										Log.e("read 390 fail");
//										disconnect("fail1 390", true);
//										return;
//									}
									responseStranger_onlyIdx(idx);  //tml*** getuserinfo
								}
							} else {
								Log.e("210 !@#$ mADB closed / AJ dead");
							}
						}
						if (!isExist) {
							responseMessageGot(fromServer);
						} else {
							Log.e("msgX isExist");
						}
					} else if (fromServer.startsWith("390"))
					{
						try{
							String address=fromServer.split("/")[1];
							responseStranger(address, mQueryIdx);
						}catch(Exception e){
						}
					} else if (fromServer.startsWith("460") && friends.length() > 0) {
						// split friends:
						try{
							if (friends!=null)
							{
								needUpdateStatus=(!fromServer.equals(status460));
								status460=fromServer;

								Pattern and = Pattern.compile("\\&");
								String[] numbers = and.split(friends, 100);
			
								if (numbers.length > 0) {
									fromServer = fromServer.substring(4);
									for (int k = 0; k < fromServer.length(); k++) {
										if (k < numbers.length) {
											char a = fromServer.charAt(k);
											int online_type = -1;
											if (a >= '0' && a <= '4')
												online_type = a - '0';
											ContactsOnline.setContactOnlineStatus(numbers[k], online_type);
										}
									}
								}
							}
						}catch(Exception e){}
						friends = null;
						ready450 = true;
						synchronized (lock_450) {
							lock_450.notifyAll();
						}
					
					} else if (fromServer.startsWith("230")  //tml upload7
							|| fromServer.startsWith("240")) {// 230/3f/X/4e928cc6
																// 240/X/4e928cf2
						try {
							String[] splits = fromServer.split("/");
							if (splits.length>=2)
							{
								try {
									int row_id = Integer.valueOf(splits[splits.length - 2], 16);
									long sentTime = Long.parseLong(splits[splits.length - 1], 16) * 1000;
									Log.d("row_id="+row_id);
									if (row_id >= 0 && mSmsDB.isOpen()) {
										mSmsDB.setMessageSentById(row_id,
												SMS.STATUS_SENT,
												sentTime);
									}
								} catch (Exception e) {}
							}
						} catch (Exception e) {}

						Intent it = new Intent(Global.Action_MsgSent);
						it.putExtra("SendeeAddress", SendeeAddress);
						mContext.sendBroadcast(it);

						//tml*** 200timeout
						SMSoK = true;
//						MyUtil.Sleep(TRANSIT_TIMEOUT + 1000);  //for lock200 testing
						synchronized (lock_200) {
							lock_200.notifyAll();
						}
						
						if (fromServer.startsWith("240"))//iPhone Push
						{
							SMS280oK = true;  //tml*** 280timeout, set false to test msg fail
							if (!(BufferedMsg.contains("[<CallFrom>]")
									|| BufferedMsg.contains(Global.Hi_AddFriend2)
									|| BufferedMsg.contains("[<NEWMOOD>]")
									|| BufferedMsg.contains("[<NEWPHOTO>]"))) {  //280 parse //alex*** callfrom
								try{
									String[] splits = fromServer.split("/");
									if (splits.length>1 && splits[1].equals("i"))
									{
										Log.e("apple doAppPushMsg?");
										doAppPushMsg();
									}
								}catch(Exception e){}
							} else {
								Log.e("apple no 240push");
							}
						}
					} 
					
					else if (fromServer.startsWith("710"))// Sender wants to call me
					{
//						int slash = fromServer.indexOf('/');
//						if (slash != -1)
//							SenderID = fromServer.substring(slash + 1);
						//tml*** 700 v2
						String CallID;
						String[] string700 = parse700(fromServer);
						if (string700 != null) {
							SenderID = string700[1];
							if (Integer.parseInt(string700[0]) == ids710 && TESTNEW700)
								CallID = string700[2];
						} else {
							continue;
						}
						
						if (AireJupiter.getInstance() != null)
							AireJupiter.getInstance().updateCallDebugStatus(true, null);
						displayCallStatus("<710");  //tml test
						Log.d("voip.(710) Incoming call from " + SenderID);
						
						//alec: for iphone active from push
						int idx=0;
						try{
							if (SenderID == null) {
								Log.e("(710) idx ERROR?!?!");
							} else {
								idx = Integer.parseInt(SenderID, 16);
							}
//							mPref.write("tempCheckSameIN", idx);  //tml*** sametime/
						}catch(Exception e){}
						/*
						if (idx==BufferedIdxForCall)
						{
							Log.d("idx==BufferedIdxForCall");
							if (AireJupiter.getInstance()!=null)
								AireJupiter.getInstance().calleeGotCallRequest=true;
							continue;
						}*/
						
						BufferedIdxForCall=0;
						
						boolean rejectDueToSecurity=false;
						//alec: handle incoming call while suveillence
						if (SurveillanceDialog.getInstance()!=null)
						{
							String Sender = mADB.getAddressByIdx(idx);
							boolean found=false;
							List<String> instants=mPref.readArray("instants");
		        			if (instants!=null)
		        			{
		        				for (String address: instants)
		        				{
		        					if (address.equals(Sender))
		        					{
		        						Log.d("address="+address+" Sender="+Sender);
		        						found=true;
		        						break;
		        					}
		        				}
		        			}
		        			
		        			if (!found)
		        				rejectDueToSecurity=true;
		        			
		        			if (!rejectDueToSecurity)
		        			{
								mPref.write("emergencyCallIn", true);
		        				Intent intent = new Intent(Global.Action_End_Surveillance);
								intent.putExtra("address", "");
								mContext.sendBroadcast(intent);
		        			}
						}
						
						boolean blackListUser=(mADB.isUserBlocked(idx)==1 || mRDB.isUserBlocked(idx)==1);//User in black list
						
						if (DialerActivity.getDialer()!=null || !mADB.isFafauser(idx) || blackListUser || rejectDueToSecurity)// in call (i am busy)
						{
							try {
								outToServerPeriod(); //tml*** outToServer period/
								displayCallStatus(">770");  //tml test
								Log.d("voip.770/" + myId + "/" +SenderID);
								outToServer.write(MyUtil.encryptTCPCmd("770/" + myId + "/" +SenderID));
								AireJupiter.getInstance().attemptCall = true;  //tml|alex*** airecall interrupted/
								//wait for new 700 to solve this
//								if (AireVenus.instance() != null) {
//									if (!AireVenus.instance().inCall) {
//										Log.e("!!!attempt to kill Dialer!!!");
//										DialerActivity.getDialer().killViewMode();
//									}
//								}
								
							} catch (IOException e) {
								Log.e("write 770 fail");
								disconnect("fail 770", true);
								return;
							}
							continue;  //tml|alex*** airecall interrupted/
						}
						
						if (blackListUser || rejectDueToSecurity) {
							Log.e("710 reject! " + blackListUser + rejectDueToSecurity);
							continue;
						}

						if (mADB.isFafauser(idx))
						{
							mPref.write("tempCheckSameIN", idx);  //tml*** sametime/
							String Sender = mADB.getAddressByIdx(idx);
							String debugN = "--";
							if (Sender != null && Sender.length() > 7)  //tml test
								debugN = Sender.substring(0, 5) + "..";
							mPref.write("lastCallSip", "I:" + debugN);
							mPref.write("curCall", Sender);
							if (mPref.readBoolean("autoAnswer:"+Sender, false))
								mPref.write("RecvOnly", true);
							else
								mPref.write("RecvOnly", false);
							
							notifyServiceXtoLanuchServiceY();
							continue;
						}

						try {
							if (mADB.isOpen() && AireJupiter.getInstance()!=null)
							{
								if (!mADB.isFafauser(idx)) {
//									try {
//										outToServerPeriod(); //tml*** outToServer period/
//										Log.d("addF.unknown-700 > 380");
//										outToServer.write(MyUtil.encryptTCPCmd("380/" + SenderID));
//									} catch (IOException e) {
//										Log.e("write 380 fail");
//										disconnect("fail2 380", true);
//										return;
//									}
//		
//									try {
//										inFromServer.read(buffer);
//										String fromServer390 = MyUtil.decryptTCPCmd(buffer);
//										if (fromServer390 != null && fromServer390.startsWith("390")) {
//											String[] addr = fromServer390.split("/");
//											responseStranger(addr[1],idx);
//										}
//									} catch (Exception e) {
//										Log.e("read 390 fail");
//										disconnect("fail2 390", true);
//										return;
//									}
									responseStranger_onlyIdx(idx);  //tml*** getuserinfo
								}
							}
						} catch (Exception e) {
							
						}
					} else if (fromServer.startsWith("730")) {
//						int slash = fromServer.indexOf('/');
//						if (slash != -1) {
//							SenderID = fromServer.substring(slash + 1);
//							
//						}

						//tml*** 700 v2
						String CallID;
						String sipServer;
						String[] string700 = parse700(fromServer);
						if (string700 != null) {
							SenderID = string700[1];
							if (Integer.parseInt(string700[0]) == ids730 && TESTNEW700) {
								CallID = string700[2];
								sipServer = new NetInfo(mContext).longToIPForServer(Long.valueOf(string700[3], 16));
							}
						} else {
							continue;
						}

						//tml*** ghost730
						boolean ghost730 = false;
						String sender0 = mADB.getAddressByIdx(Integer.valueOf(SenderID, 16));
						Log.e("730 sender " + sender0 + "=" + mPref.read("curCall", ""));
						if (!sender0.equals(mPref.read("curCall", ""))) {
							ghost730 = true;
							Log.e("730 ghost1");
							sendTerminateCommand(sender0);
						}
						
						if (!ghost730) {
							RecvedCallACK = true;
							displayCallStatus("<730");  //tml test
							Log.d("voip.(730) Callee got call request. Done ***");
							synchronized (lock_700) {
								lock_700.notifyAll();
							}
						}
						
					} else if (fromServer.startsWith("750")) {
						displayCallStatus("<750");  //tml test
						try{
							if (AireVenus.instance() != null) {
								if (AireVenus.instance().inCall) {
									AireJupiter.getInstance().attemptCall = true;  //"StreamsRunning"
								}
							} else {
								AireJupiter.getInstance().attemptCall = false;
							}
//							int slash = fromServer.indexOf('/');
//							if (slash != -1)
//								SenderID = fromServer.substring(slash + 1);
							//tml*** 700 v2
							String CallID;
							String[] string700 = parse700(fromServer);
							if (string700 != null) {
								SenderID = string700[1];
								if (Integer.parseInt(string700[0]) == ids750 && TESTNEW700)
									CallID = string700[2];
							} else {
								continue;
							}

							if (DialerActivity.getDialer() != null
									&& AireJupiter.getInstance() != null && mADB.isOpen()) {
								if (AireJupiter.getInstance().attemptCall == false) {
									String Sender = mADB.getAddressByIdx(Integer
											.valueOf(SenderID, 16));
									Log.e("voip.(750) Terminate Call by TCPSocket :" + Sender);
									if (Sender.equals(mPref.read("curCall", "")))
										if (DialerActivity.getDialer() != null)
											DialerActivity.getDialer().endUpDialer(Sender);
								} else {
									Log.e("voip.750 busy,cancelled,voicemail?");
									AireJupiter.getInstance().attemptCall = false;
								}
							} else {
								Log.d("voip.750 empty");
								mPref.write("tempCheckSameIN", 0);  //tml*** sametime
							}
							
							if (AireVenus.instance()!=null)
								AireVenus.instance().callStopRing();
							
						}catch(Exception e){}
						
						BufferedIdxForCall=0;

					} else if (fromServer.startsWith("790")) {
						NotfoundACK = true;
						displayCallStatus("<790");  //tml test
						Log.e("voip.(790) Callee not found");
//						String[] splits = fromServer.split("/");
//						if (splits.length>1 && splits[1].equals("i"))//iPhone Push
//						{
//							Log.d("voip.apple 790");
//							try{
//								doAppPushCall();
//								WaitForPush=true;
//								NotfoundACK=false;
//							}catch(Exception e){}
//						} else {
//							Log.d("NOTapple 790");
//							synchronized (lock_700) {
//								lock_700.notifyAll();
//							}
//						}

						//tml*** 700 v2
						String CalleeID;
						String CallID;
						String[] string700 = parse700(fromServer);
						if (string700 != null) {
							CalleeID = string700[1];
							if (Integer.parseInt(string700[0]) == ids790 && TESTNEW700)
								CallID = string700[2];
						} else {
							continue;
						}

						if (CalleeID.equals("i")) {
							Log.d("voip.apple 790");
							try {
								doAppPushCall();  //iPhone Push
								WaitForPush = true;
								NotfoundACK = false;
							} catch (Exception e) {}
						} else {
							Log.d("voip.NOTapple 790");
							synchronized (lock_700) {
								lock_700.notifyAll();
							}
						}
						
					} else if (fromServer.startsWith("780")) { //Line is busy
						StopCalling = true;
						displayCallStatus("<780");  //tml test
						Log.e("voip.780 Line is busy");
						//tml*** 700 v2
						String CalleeID;
						String CallID;
						String[] string700 = parse700(fromServer);
						if (string700 != null) {
							CalleeID = string700[1];
							if (Integer.parseInt(string700[0]) == ids780 && TESTNEW700)
								CallID = string700[2];
						} else {
							continue;
						}
						/*
						if (DialerActivity.getDialer()!=null)
							DialerActivity.getDialer().responseCallBusy();
						*/
						
					} else if (fromServer.startsWith("160")) {
						try{
							CalleeSipIP = fromServer.substring(4);
							if (CalleeSipIP.equals("0.0.0.0"))
								CalleeSipIP = null;
							synchronized (lock_150) {
								lock_150.notifyAll();
							}
						}catch(Exception e){}
					} else if (fromServer.startsWith("180")) {
						//tml*** xcountry sip
					} else if (fromServer.startsWith("009")) {
						// prelogin
						if (AireJupiter.getInstance() != null)
							AireJupiter.getInstance().getServers();
					}else if (fromServer.startsWith("195")) {
						//tml*** tcp test
						count190++;
						Log.d("190/195 ok! +" + count190);
						synchronized (lock_190) {
							lock_190.notifyAll();
						}
					}
					else if (fromServer.startsWith("510")) {  //alec*** vidconf
//						if (VideoConf.EN_VC)
//						try {
//							String sdp=fromServer;
//							if (fromServer.contains("<Z>"))
//								sdp = sdp.substring(fromServer.indexOf("<Z>")+3);
//							int idx=0;
//							try {
//								String [] items=fromServer.split("/");
//								idx=Integer.parseInt(items[1], 16);
//								Log.d("tmlvc 510 Recv p2p offer from:"+BufferedIdxForMsg);
//							}catch (Exception e) {}
//							
//							P2P pp=P2P.getInstance();
//							if (pp!=null)
//							{
//								Log.d("tmlvc 510 pp parseOffer");
//								String sdpAnswer=pp.parseOffer(sdp);
//								sendAnswerSDP(idx, sdpAnswer);
//							}
//							else {
//								Log.d("tmlvc 510 pp == null");
//							}
//							if (VideoConf.instance()!=null)
//							{
//								VideoConf.instance().addP2PMember(idx);
//							}
//						} catch (Exception e) {
//							Log.e("tmlvc 510 data paring error...");
//							continue;
//						}
					}else if (fromServer.startsWith("530")) {
//						if (VideoConf.EN_VC)
//						try {
//							String sdp=fromServer;
//							if (fromServer.contains("<Z>"))
//								sdp = sdp.substring(fromServer.indexOf("<Z>")+3);
//							P2P pp=P2P.getInstance();
//							if (pp!=null)
//							{
//								pp.parseAnswer(sdp);
//								pp.startEncoder(0);
//								if (VideoConf.instance()!=null)
//									VideoConf.instance().startCapture();
//							}
//						} catch (Exception e) {
//							Log.e("data paring error...");
//							continue;
//						}
					} else {
						Log.e("~~~fromServer mumbo jumbo~~~");
					}
					//***alec
				}
				
			} while (Running);
			fromServerA = null;
		}
	}
	
	//tml|alex*** iphone push
	public Runnable iphoneTimeout10 = new Runnable() {
		public void run() {
			try {
				Thread.sleep(TRANSIT_TIMEOUT * 3);
			} catch (InterruptedException e) {}
			if (AireVenus.callstate_AV != null) {
//				Log.e("apple Timeout state = " + AireVenus.callstate_AV);
				if (AireVenus.callstate_AV.contains("Outgoing")) {
					Log.e("tml apple 730Timeout doAppPushCall");
					doAppPushCall();
					WaitForPush = true;
					NotfoundACK = false;
				} else {
					Log.e("tml apple 730Timeout NO PUSH");
				}
			} else {
				Log.e("tml apple 730Timeout NO PUSH (nullstate)");
			}
		}
	};

	private final Object lock_190 = new Object();
	private volatile int sent190 = 0;
	private volatile int count190 = 0;
	public Runnable selfCheck = new Runnable() {
		public void run() {
			try {
				outToServerPeriod();  //tml*** outToServer period/
				outToServer.write(MyUtil.encryptTCPCmd("190/" + myId));
				
				//tml*** tcp test
				tcpStatus0 = true;
				tcpStatus1 = false;
				tcpStatus2 = false;
				sent190++;
				synchronized (lock_190) {
					try {
						lock_190.wait(TRANSIT_TIMEOUT / 2);
					} catch (Exception e) {}
				}
				
//				Log.d("190 attempt #" + sent190);
				if (sent190 < 3) {
					new Thread(selfCheck).start();
					return;
				}
				
				if (count190 == 0) {
					tcpStatus1 = false;  //warning
					tcpStatus2 = false;  //wifi reassociate
				} else if (count190 == 1) {
					tcpStatus1 = false;  //warning
					tcpStatus2 = true;
				} else if (count190 == 2) {
					//ok
					tcpStatus1 = true;
					tcpStatus2 = true;
				} else {  //count190 == 3
					//great
					tcpStatus1 = true;
					tcpStatus2 = true;
				}

				int[] wifiLevel = checkWifi();
				if (wifiLevel[0] < 2) {
//					tcpStatus1 = false;
				}
				
				if (!tcpStatus1) {
					tcpStatus0 = false;
					warningPoor(false, "190");
				}
				if (!tcpStatus2) {
					tcpStatus0 = false;
					Intent it = new Intent(Global.Action_InternalCMD);
					it.putExtra("Command", Global.CMD_REFRESH_CONN);
					mContext.sendBroadcast(it);
				}
				checkingKeepAlive = false;
				Log.d("(190) Keeping Alive: " + count190 + "/" + sent190 + tcpStatus1 + tcpStatus2 + " " + wifiLevel[0] + "," + wifiLevel[1]);
				//***tml
			} catch (Exception e) {
				checkingKeepAlive = false;
				Log.e("Failed to write 190");
				disconnectAndReconnect("fail 190", true);
				return;
			}
		}
	};
	
	private volatile boolean checkingKeepAlive = false;
	public void keepAlive(boolean force) {
		long now = new Date().getTime();
		long last = mPref.readLong("last_self_check_time", 0);
		long elapsed = now - last;
		if ((elapsed < 30000 && !force) || checkingKeepAlive) {  //30 sec
			Log.e("x190 not yet! " + elapsed + " " + force + " " + checkingKeepAlive);
			return;// no need to do it
		}
		if (!checkingKeepAlive) {
			mPref.writeLong("last_self_check_time", now);
		}
		
		checkingKeepAlive = true;
		sent190 = 0;
		count190 = 0;
		new Thread(selfCheck).start();
	}
	//tml*** tcp test
	public int[] checkWifi() {
		WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifi.getConnectionInfo();
		int[] wifiLevel = new int[2];
		int numLevels = 5;
		
		int rssi = wifiInfo.getRssi();
		int level = WifiManager.calculateSignalLevel(rssi, numLevels);
		int speed = wifiInfo.getLinkSpeed();
		wifiLevel[0] = level;
		wifiLevel[1] = speed;
		
		return wifiLevel;
	}
	
	private void warningPoor(boolean force, String from) {
		Intent it = new Intent(Global.Action_InternalCMD);
		it.putExtra("Command", Global.CMD_CONNECTION_POOR);
		it.putExtra("ForcePoor", force);
		it.putExtra("warnFrom", from);
		it.putExtra("length01", 1);
		mContext.sendBroadcast(it);
	}
	//***tml

	static int VersionCode = 32;

	public synchronized boolean Login() {
		return Login(VersionCode);
	}
	
	public synchronized boolean Login(int versionCode) {
		VersionCode = versionCode;
		
		if (logged == 1) {
			return true;
		} else {
//			disconnect(true);
		}
		if (myPhoneNumber.equals("----")) return false;
		LocationUpdate location = new LocationUpdate(mContext, mPref);
		String iso = mPref.read("iso", "");  //tml*** new get geo
		boolean noIso = iso.length() == 0;
	    location.getMyLocFromIpAddress(noIso);
	    
	    //tml*** tcp test
	  	conn_try_count++;
	  	if (conn_try_count > CONN_TIMEOUT_TRIES) {
	  		warningPoor(true, "login retries");
	  	}
		
		String fromServer = "";
		tcpStatus0 = false;
		logged = 0;
		Logging = 1;
		int alter = 0;
		int count = 0;
		boolean connected = false;
		
		while (count++ < 2 && !connected)
		{
			int port = ((alter % 2) == 0) ? 9135 : 80;
			InetAddress address;
			String domainName = "tcp.airetalk.org";
			iso = mPref.read("iso", "cn");
			
			try {
				if (MyUtil.isISO_China(mContext, mPref, null))
					domainName = "tcp.xingfafa.com.cn";
				address = InetAddress.getByName(domainName);
				ServerIP = address.toString().substring(address.toString().lastIndexOf("/") + 1);
			} catch (UnknownHostException e1) {
				Log.e("TCP domainName ERR " + e1.getMessage());
				ServerIP = "74.3.164.16";
			}

//			ServerIP = "115.29.185.116";  //cn
//			ServerIP = "74.3.162.130";  //us-old-temp
			if (mPref.readBoolean("TESTNEWSERVER", false)) {
				if (mPref.readBoolean("NEWTCPCHINA", false)) {
					ServerIP = "218.244.139.96";  //115.29.185.116
				} else if (mPref.readBoolean("NEWTCPUSA", false)) {
					ServerIP = "74.3.162.130";
				}
			}
			
			InetSocketAddress isa = new InetSocketAddress(ServerIP, port);
			clientSocket = new Socket();
			
			try {
				ServerDM_d = domainName;
				ServerIP_d = ServerIP;
				Log.e(conn_try_count + " ...CONNECTING !! TCP " + domainName + "  " + ServerIP + ":" + port + ":" + iso);
				
				clientSocket.setTcpNoDelay(true);
				clientSocket.setReuseAddress(true);
				clientSocket.connect(isa, TRANSIT_TIMEOUT * 2);
	
				outToServer = new DataOutputStream(clientSocket.getOutputStream());
				inFromServer = new DataInputStream(clientSocket.getInputStream());
				
				connected = true;
			} catch (Exception e) {
				Log.e("TCP Socket Create Failed: " + e.getMessage());
				tcpStatus0 = false;
				Logging = 0;
				alter++;
				return false;
			}
		}

		try {
			clientSocket.setSoTimeout(TRANSIT_TIMEOUT * 3);
			
			if (mPref.readBoolean("accountUpdated", false))
			{
				outToServerPeriod(); //tml*** outToServer period/
				String cmd = "120/" + myPhoneNumber + '/' + myPasswd;
				Log.d(cmd);
				byte[] encrypted = MyUtil.encryptTCPCmd(cmd);
				outToServer.write(encrypted);
				inFromServer.read(buffer);
				MyUtil.Sleep(1000);
			}
			
			String xcmd;
			myId = mPref.read("myID", "");
			if (myId.length()>0)
				xcmd = "101/" + myId + '/' + myPasswd + '/'
				+ new NetInfo(mContext).netType + "/a/" + versionCode;
			else//first login
				xcmd = "100/" + myPhoneNumber + '/' + myPasswd + '/'
				+ new NetInfo(mContext).netType + "/a/" + versionCode;

			outToServerPeriod(); //tml*** outToServer period/
			Log.d(xcmd);
			byte[] encryptStr = MyUtil.encryptTCPCmd(xcmd);
			outToServer.write(encryptStr);
			inFromServer.read(buffer);
			fromServer = MyUtil.decryptTCPCmd(buffer);
			Log.d("Login: " + fromServer);
		} catch (Exception e) {
			Log.e("Login Failed :" + e.getMessage());
			disconnect("fail 100 read", true);
			Logging = 0;
			return false;
		}

		if (fromServer == null) // login failed
		{
			Log.e("Login, Failed :NULL");
			disconnect("fail login", true);
			Logging = 0;
			return false;
		} else if (fromServer.startsWith("999") && !MyTelephony.isPhoneNumber(myPhoneNumber)) // login failed
		{
			Log.e("Login, Failed : 999/" + fromServer.substring(3));
			notifyServiceX_Login_fail_999();
			//disconnect();
			Logging = 0;
			return false;
		}

		if (fromServer.startsWith("110")) {
			String tmp1;
			tcpStatus0 = true;
			logged=1;
			Log.d("Login successfully 110." + logged);
			tmp1 = fromServer.substring(4);

			String tmp2[] = tmp1.split(",");
			String debug = "";
			for (int i = 0; i < tmp2.length; i++) {  //tml*** debug
				debug = debug + " [" + i + "]" + tmp2[i];
			}
			Log.e("Login debug.tmp2 =" + debug);
			if (tmp2[1].contentEquals("ok")) {
				myId = tmp2[0];
				mPref.write("myID", myId);
				mySipServer = tmp2[2];
				
				if(tmp2.length>4){
					AireJupiter.myLocalPhpServer = new NetInfo(mContext).longToIPForServer(Long.valueOf(tmp2[4],16));
					Log.d("myLocalPhpServer=" + AireJupiter.myLocalPhpServer);
				}
				
				if(tmp2.length>5){
					String stun_server = new NetInfo(mContext).longToIPForServer(Long.valueOf(tmp2[5],16));
					mPref.write("StunServer", stun_server);
					Log.d("stun_server=" + stun_server);
				}

				Log.d("mySipServer=" + mySipServer);
				updateMySipServer(mySipServer);
				//tml*** conf-200 offset
				if (tmp2.length > 6) {
					long server_time = Long.parseLong(tmp2[6], 16);
					long my_time = new Date().getTime() / 1000;
					long timeOffset = server_time - my_time;
					mPref.writeLong("confServerOffset", timeOffset);
					Log.d("time offset=" + timeOffset);
				}
				
				if (tmp2.length>3 && tmp2[3].equals("1"))//I got offline msg
				{
					if (AireJupiter.getInstance()!=null)
						AireJupiter.getInstance().offlineMessage();
				}
				
				if (mPref.readBoolean("accountUpdated",false))
					mPref.delect("accountUpdated");
				
			} else { // there are error login
				disconnect("error login", true);
				Logging = 0;
				return false;
			}
			try {
				clientSocket.setSoTimeout(CLIENT_CONNECTION_TIMEOUT);
				clientSocket.setKeepAlive(true);
				clientSocket.setTrafficClass(4);
			} catch (Exception e) {
			}

			thrClient = new SocketCommThread();
			thrClient.start();

			Intent it2 = new Intent(Global.Action_InternalCMD);
			it2.putExtra("Command", Global.CMD_TCP_CONNECTION_UPDATE);
			mContext.sendBroadcast(it2);

			Logging = 0;
			location.getMyRoamId();  //tml*** xcountry sip
			Log.d("Login DONE");
			return true;
		}

		Logging = 0;
		return false;
	}

	private final Object lock_150 = new Object();
	private String CalleeSipIP;

	/*
	 * Send : 150/calleeID success : 160/calleeSip failed : null or 999/result
	 */
	public String tcpGetCalleeSip(String Callee) {
		if (logged == 0)
			return null;

		if (!mADB.isOpen()) return null;
		int idx = mADB.getIdxByAddress(Callee);
		if (idx<0)
			return "nonmember";
		String uid=Integer.toHexString(idx);
		CalleeSipIP = null;
		try {
			outToServerPeriod(); //tml*** outToServer period/
			Log.d("150/" + myId + "/" + uid);
			outToServer.write(MyUtil.encryptTCPCmd("150/" + myId + "/"
					+ uid));
		} catch (Exception e) {
			Log.e("FailtoSendtoServer.150");
			disconnect("fail 150", true);
			return null;
		}

		synchronized (lock_150) {
			try {
				lock_150.wait(TRANSIT_TIMEOUT * 2);
			} catch (Exception e) {
				Log.e("tcpGetYourSip Timeout");
				return null;
			}
		}
		return CalleeSipIP;
	}
	//tml*** xcountry sip
	public boolean updateMyRoamSip(String roamid) {
		if (logged == 0) return false;
		try {
			outToServerPeriod();  //tml*** outToServer period/
			Log.d("170/" + myId + "/" + roamid);
			outToServer.write(MyUtil.encryptTCPCmd("170/" + myId + "/" + roamid));
		} catch (Exception e) {
			Log.e("FailtoSendtoServer.170");
			return false;
		}
		return true;
	}

	/* Send : 800/myid/lat/lon */
	public void tcpUpdateGeo(String lat, String lon) {
		if (logged == 0)
			return;
		try {
			outToServerPeriod(); //tml*** outToServer period/
			Log.d("800/" + myId + "/" + lat + "/" + lon);
			outToServer.write(MyUtil.encryptTCPCmd("800/" + myId + "/"
					+ lat + "/" + lon));
		} catch (Exception e) {
			Log.e("FailtoSendtoServer.800");
			disconnect("fail 800", true);
		}
	}

	public boolean isLogged(boolean force) {
//		return (logged == 1);
		boolean _logged = (logged == 1);
		Log.i("socket isLogged=" + _logged + force);
		if (force) {
			logged = 0;
			disconnectAndReconnect("force relog", true);
			return false;
		} else {
			return _logged;
		}
	}
	
	public boolean getTcpStatus() {
		return (tcpStatus0);
	}
	
	public boolean send(String Callee, String MsgTexg, int Attached,
			String remoteAudioPath, String remoteImagePath, long rowId, String phpIP) {
		return send(Callee, MsgTexg, Attached, remoteAudioPath, remoteImagePath, rowId, phpIP, 0);
	}
	//tml upload4
	private final Object lock_200 = new Object();
	private boolean SMSoK = false, SMS280oK = true;
	public boolean send(String Callee, String MsgTexg, int Attached,
			String remoteAudioPath, String remoteImagePath, long rowId, String phpIP, int groupID) {
		SMSoK = false;
		SMS280oK = true;
		if (!mADB.isOpen() || Logging==1 || logged==0) {
			Log.e("msgs.Fail1 > " + !mADB.isOpen() + " " + Logging + " " + logged);
			return false;
		}

		int idx = mADB.getIdxByAddress(Callee);
		if (idx < 0) {
			Log.e("msgs.Fail2 idx !@#$ > " + Callee + " " + idx);
			return false;
		}
		
		if (Attached == 8
				&& MsgTexg.startsWith(mContext.getString(R.string.video))
				&& MsgTexg.contains("(vdo)")) {
			MsgTexg = MsgTexg.substring(mContext.getString(R.string.video)
					.length());
		} else if (Attached == 8
				&& MsgTexg.startsWith(mContext
						.getString(R.string.filememo_send))
				&& MsgTexg.contains("(fl)")) {
			MsgTexg = MsgTexg.substring(mContext.getString(
					R.string.filememo_send).length());
		}
		SendeeAddress = Callee;
		String uid=Integer.toHexString(idx);
		String buffer;
		
		BufferedIdxForMsg=idx;
//		BufferedMsg=MsgTexg;
		
		if (groupID != 0) {
			buffer = "200/" + myId + "/" + uid + "/`[" + groupID + "]\n" + MsgTexg;
		} else if (MsgTexg.contains("[<CallFrom>]")) {  //280 direct //alex*** callfrom
			buffer = "280/" + myId + "/" + uid + "/" + MsgTexg;
		} else if (MsgTexg.startsWith(Global.Temp_Parse)) {
			MsgTexg = MsgTexg.substring(Global.Temp_Parse.length(), MsgTexg.length());
			buffer = "280/" + myId + "/" + uid + "/" + MsgTexg;
		} else {
			if (MsgTexg.equals("/")) {  //tml*** temp / fix
				MsgTexg = " /";
			}
			buffer = "200/" + myId + "/" + uid + "/" + MsgTexg;
		}
		
		BufferedMsg=MsgTexg;  //tml*** 200timeout
		
		try {
			if (Attached != 0){
				buffer += "<Z>" + Attached + "<Z>" + remoteAudioPath + "<Z>"
						+ remoteImagePath + "<Z>"+ Long.toHexString(new NetInfo(mContext).ipToLong(phpIP)) + "<Z>";
			}
		} catch (Exception e) {
			if (Attached != 0){
				buffer += "<Z>" + Attached + "<Z>" + remoteAudioPath + "<Z>"
						+ remoteImagePath + "<Z>"+ phpIP + "<Z>";
			}
		}
		
		try {
			buffer += "/`" + Integer.toHexString((int) rowId);
			outToServerPeriod(); //tml*** outToServer period/
			Log.i("!msgs :: " + buffer);
			outToServer.write(MyUtil.encryptTCPCmd(buffer));

			//tml*** 200timeout
			synchronized (lock_200) {
				try {
					lock_200.wait(TRANSIT_TIMEOUT * 2);
				} catch (Exception e) {}
			}
			
			Log.e("msgs.lock_200 SMSoK=" + SMSoK + " SMS280oK=" + SMS280oK);
			if (!SMSoK) {
				int versionCode = mContext.getPackageManager()
						.getPackageInfo(mContext.getPackageName(), 0).versionCode;
				if (!isLogged(false)) Login(versionCode);
				if (AireJupiter.getInstance() != null) {
					AireJupiter.getInstance().sendPendingSMS();
				}
				return false;
			} else if (!SMS280oK) {
				Intent it = new Intent(Global.Action_SMS_Fail);
				mContext.sendBroadcast(it);
				return false;
			}
			//***tml
		} catch (Exception e) {
			Log.e("FailtoSendtoServer.200");
			disconnect("fail 200", true);
		}
		return true;
	}
	
	//alec*** vidconf
	public boolean sendSDP(int idx, String sdpMsg) {
		if (!mADB.isOpen() || Logging==1 || logged==0) return false;
		
		Log.d("tmlvc Socket: p2p Inviting...");
		String uid=Integer.toHexString(idx);
		String buffer = "500/" + myId + "/" + uid + "/" + sdpMsg;
		
		try {
			outToServerPeriod(); //tml*** outToServer period/
			Log.d("tmlvc " + buffer);
			outToServer.write(MyUtil.encryptTCPCmd(buffer));
		} catch (Exception e) {
			Log.e("tmlvc FailtoSendtoServer.500");
			disconnect("fail 500", true);
		}
		return true;
	}
	
	public boolean sendAnswerSDP(int idx, String sdpMsg) {
		if (Logging==1 || logged==0) return false;
		
		Log.d("tmlvc Socket: p2p reponse...");
		String uid=Integer.toHexString(idx);
		String buffer = "520/" + myId + "/" + uid + "/" + sdpMsg;
		
		try {
			outToServerPeriod(); //tml*** outToServer period/
			Log.d("tmlvc " + buffer);
			outToServer.write(MyUtil.encryptTCPCmd(buffer));
		} catch (Exception e) {
			Log.e("tmlvc FailtoSendtoServer.520");
			disconnect("fail 520", true);
		}
		return true;
	}
	//***alec

	private boolean StopCalling=false;
	String mCallee;
	private final Object lock_700 = new Object();
	public int sendCallRequest(String Callee) {
		if (!mADB.isOpen()) return -1;
		RecvedCallACK = false;
		NotfoundACK = false;
		StopCalling = false;
		WaitForPush = false;
		for (int i = 0; i < 1; i++) {
			outToServerPeriod(); //tml*** outToServer period/
			try {
				mCallee = Callee;
				mPref.write("curCall", Callee);
				int idx=mADB.getIdxByAddress(Callee);
				if (idx<0) return -1;
				BufferedIdxForCall=idx;
				String uid=Integer.toHexString(idx);
				if (AireJupiter.getInstance() != null)
					AireJupiter.getInstance().updateCallDebugStatus(true, null);
				displayCallStatus(">700");  //tml test
				Log.d("voip.Socket: Sending Call Request.. 700 " + myId + "/" + uid);
				outToServer.write(MyUtil.encryptTCPCmd("700/" + myId + "/" + uid));
			} catch (Exception e) {
				Log.e("FailtoSendtoServer.700");
			}

			synchronized (lock_700) {
				try {
					lock_700.wait(TRANSIT_TIMEOUT * 2);
				} catch (Exception e) {}
			}
			
			Log.d("voip.lock_700 Stop=" + StopCalling + " ACK=" + RecvedCallACK 
					+ " NoACK=" + NotfoundACK + " Push=" + WaitForPush);
			if (WaitForPush)
				return 2;
			if (StopCalling) 
				return 0;
			if (RecvedCallACK)
				return 1;
			if (NotfoundACK)
				return 1;
			if (!RecvedCallACK) {  //tml|alex*** iphone push
				Log.d("voip.apple 700 still no 730");
				doAppPushCall();
				return 2;
			}
		}
		return 0;
	}
	
	//tml|alex*** iphone push
	public int sendCallRequestApple(String Callee) {
		if (!mADB.isOpen()) return -1;
		RecvedCallACK = false;
		NotfoundACK = false;
		StopCalling = false;
		WaitForPush = false;
		for (int i = 0; i < 1; i++) {

			synchronized (lock_700) {
				try {
					lock_700.wait(TRANSIT_TIMEOUT * 3);
				} catch (Exception e) {}
			}
			
			Log.d("voip.applelock_700x2 waitdone Stop=" + StopCalling + " ACK=" + RecvedCallACK 
					+ " NoACK=" + NotfoundACK + " Push=" + WaitForPush);
			if (StopCalling) 
				return 0;
			if (RecvedCallACK)
				return 1;
			if (NotfoundACK)
				return 1;
			if (WaitForPush)
				return 2;
		}
		return 0;
	}
	//***tml
	
	//Jerry, 031214, Bug 0000204, will be accessed by DialerActivity OnClick hangup.
	public static int BufferedIdxForCall=0;
	private int BufferedIdxForMsg;
	private String BufferedMsg="";
	public void doAppPushCall() {
		new Thread(new Runnable(){
			public void run()
			{
				int myIdx=Integer.parseInt(myId,16);
				//alex*** callfrom
				String nickname = mADB.getNicknameByIdx(myIdx);
				String pushMsg = "[<CallFrom>]//" + nickname;
				Log.d("voip.do Apple Push Call " + mCallee + " " + pushMsg);
				send(mCallee, pushMsg, 0, null, null, 0, null);
				//***alex
				MyNet net = new MyNet(mContext);
				net.doPost("apns.php","sender="+myIdx+
					"&idx="+BufferedIdxForCall+
					"&cmd=700"+
					"&msg=", null);
			}
		}).start();
	}
	
	public void doAppPushMsg() {
		Log.d("msgs.do App Push Msg");
		new Thread(new Runnable(){
			public void run()
			{
				int myIdx=Integer.parseInt(myId,16);
				MyNet net = new MyNet(mContext);
				try{
					net.doPost("apns.php","sender="+myIdx+
							"&idx="+BufferedIdxForMsg+
							"&cmd=200"+
							"&msg="+URLEncoder.encode(BufferedMsg,"UTF-8"), null);
				}catch(Exception e){}
			}
		}).start();
	}

	static void Sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
		}
	}

	public boolean disconnect(String from, boolean hidden) {
		
		if (Logging==1) return false;

		status460="";
		
		if (thrClient != null)// alec
			thrClient.terminate();
		
		try {
			outToServerPeriod(); //tml*** outToServer period
			mPref.writeLong("outToServerLast", 0);
			if (outToServer != null)
				outToServer.write(MyUtil.encryptTCPCmd("300/" + myId));
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
		tcpStatus0 = false;
		logged = 0;

		Log.e("Socket ***** Disconnected... ***** <" + from);

		if (AireJupiter.getInstance() != null) {
			AireJupiter.getInstance().notifyConnectionChanged();
			if (!hidden) {
				warningPoor(true, "disconnect");
			}
		} else {
			try {
				if (new MyPreference(mContext).readBoolean("AireRegistered",false))
				{
					Intent itx=new Intent(mContext, AireJupiter.class);
					mContext.startService(itx);
				}
			} catch (Exception e) {}
		}

		mPref.writeLong("last_dlf_status", 0);
		
		return true;
	}
	
	public boolean disconnectAndReconnect(String from, boolean hidden) {
		
		if (Logging==1) return false;

		status460="";
		
		if (thrClient != null)// alec
			thrClient.terminate();
		
		try {
			outToServerPeriod(); //tml*** outToServer period
			mPref.writeLong("outToServerLast", 0);
			if (outToServer != null)
				outToServer.write(MyUtil.encryptTCPCmd("300/" + myId));
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
		tcpStatus0 = false;
		logged = 0;

		Log.e("Socket ***** Disconnected...And Reconnect ***** <" + from);

		if (AireJupiter.getInstance() != null) {
			AireJupiter.getInstance().notifyReconnectTCP();
			if (!hidden) {
				warningPoor(true, "disconnectRE");
			}
		} else {
			try {
				if (new MyPreference(mContext).readBoolean("AireRegistered",false))
				{
					Intent itx=new Intent(mContext, AireJupiter.class);
					mContext.startService(itx);
				}
			} catch (Exception e) {}
		}

		mPref.writeLong("last_dlf_status", 0);
		
		return true;
	}

	private void responseMessageGot(String originalSignal) {
		Intent it = new Intent(Global.Action_InternalCMD);
		it.putExtra("Command", Global.CMD_TCP_MESSAGE_ARRIVAL);
		it.putExtra("originalSignal", originalSignal);
		mContext.sendBroadcast(it);
	}
	
	private void responseStranger(String address, int idx) {
		Log.d("addF.stranger! " + idx + " " + address);
		Intent it = new Intent(Global.Action_InternalCMD);
		it.putExtra("Command", Global.CMD_STRANGER_COMING);
		it.putExtra("Address", address);
		it.putExtra("Idx", idx);
		mContext.sendBroadcast(it);
	}
	//tml*** getuserinfo
	private void responseStranger_onlyIdx(int idx) {
		Log.d("addF.stranger2! " + idx);
		Intent it = new Intent(Global.Action_InternalCMD);
		it.putExtra("Command", Global.CMD_STRANGER_COMING);
		it.putExtra("Idx", idx);
		mContext.sendBroadcast(it);
	}
	
	private void notifyServiceXtoLanuchServiceY() {
		ServiceZ.acquireStaticLock(mContext);
		
		if (AireJupiter.getInstance()!=null)
			AireJupiter.getInstance().startServiceY(AireVenus.CALLTYPE_FAFA);

		(new Thread() {
			public void run() {
				
				int c=0;
				
				while(AireVenus.destroying && c++<30)
				{
					Sleep(100);
				}
				
				if (AireJupiter.getInstance()!=null)
					AireJupiter.getInstance().startServiceY(AireVenus.CALLTYPE_FAFA);
				
				c = 0;
				while (AireVenus.instance() == null
						|| !AireVenus.instance().registered) {
					if (c++ > 50)
						break;
					Sleep(100);
				}
				outToServerPeriod(); //tml*** outToServer period/
				displayCallStatus(">720");  //tml test
				Log.d("voip.(720) Response");
				
				try {
					outToServer.write(MyUtil.encryptTCPCmd("720/" + SenderID + "/" + myId));
				} catch (Exception e) {
					Log.e("Failed to write 720 ack");
					disconnect("fail 720", true);
					return;
				}
			}
		}).start();

		if (mADB.isOpen()){
			String Caller = mADB.getAddressByIdx(Integer.valueOf(SenderID, 16));
			if (ContactsOnline.getContactOnlineStatus(Caller) <= 0) {
				ContactsOnline.setContactOnlineStatus(Caller, 2);
			}
		}
	}

	private void notifyServiceX_Login_fail_999() {
		Intent it = new Intent(Global.Action_InternalCMD);
		it.putExtra("Command", Global.CMD_LOGIN_FAILED);
		mContext.sendBroadcast(it);
	}

	public void sendTerminateCommand(String Callee) {
		Log.e("Socket: sendTerminateCommand... >" + Callee);
		mPref.write("tempCheckSameIN", 0);  //tml*** sametime
		BufferedIdxForCall=0;//alec
		if (!mADB.isOpen()) return;
		int idx=mADB.getIdxByAddress(Callee);
		if (idx<0) return;
		try {
			String uid=Integer.toHexString(idx);
			outToServerPeriod(); //tml*** outToServer period/
			displayCallStatus(">740");  //tml test
			Log.d("voip.740/" + myId + "/" + uid);
			outToServer.write(MyUtil.encryptTCPCmd("740/" + myId + "/" + uid));
		} catch (Exception e) {
			Log.e("FailtoSendtoServer.740");
			disconnect("fail 740", true);
		}
	}
	
	//alec:
	int mQueryIdx;
	public void queryUserAddressByIdx(int idx)
	{
		try {
			mQueryIdx=idx;
			String uid=Integer.toHexString(idx);
			outToServerPeriod(); //tml*** outToServer period/
			outToServer.write(MyUtil.encryptTCPCmd("380/" + uid));
		} catch (IOException e) {
			Log.e("write 380 fail");
			disconnect("fail 380", true);
			return;
		}
	}

	private final Object lock_450 = new Object();
	private String friends = "";
	public synchronized boolean queryFriendsOnlineStatus(boolean forceCheck) {
		final int discFLimit = 1;
		Log.i("do queryFriendsOnlineStatus!!!?");
		if (logged != 1 || Logging==1 || !mADB.isOpen()) return false;
		String friendsIdx = "";
		long now = new Date().getTime();
		long last = mPref.readLong("last_dlf_status", 0);
		if (now - last < 5000 && !forceCheck) {  // 10 sec  //tml
			Log.d("Check online friends... NOT YET " + (now - last));
			return true;// no need to update
		}

		friends = "";

		int j = 0;
		int n = 0;
		Log.i("Check online friends...");
		Cursor cursor = mADB.getFafaFriends();
		if (cursor.moveToFirst()) {
			do {
				String address=cursor.getString(1);
				if (address.startsWith("[<GROUP>]")) continue;
				if (friends.length() > 0) {
					friends += "&";
					friendsIdx += "&";
				}
				friends += cursor.getString(1);
				friendsIdx += Integer.toHexString(cursor.getInt(3));
				j++;

				if (j >= 80) {
					thrClient.reset450();
					outToServerPeriod(); //tml*** outToServer period/
					Log.d("1.450/" + myId + "/" + friendsIdx);
					try {
						outToServer.write(MyUtil.encryptTCPCmd("450/"
								+ myId + "/" + friendsIdx));
					} catch (Exception e) {
						Log.e("fail to getFriendsOnlineStatus1");
						disconnect("fail fonline1", true);
						if(cursor!=null && !cursor.isClosed())
							cursor.close();
						return false;
					}

					synchronized (lock_450) {
						try {
							lock_450.wait(TRANSIT_TIMEOUT * 3);
						} catch (InterruptedException e) {
						}
					}

					if (!thrClient.isReady450()) {
						Log.e("getFriendsOnlineStatus Timeout1");
						friends = "";
						if(cursor!=null && !cursor.isClosed())
							cursor.close();
						disconnect("fail fonline1 timeout", true);
						return false;
					} else {
						
						if (needUpdateStatus && DialerActivity.getDialer()==null)
						{
							if (UserPage.sortMethod==2)
								UserPage.forceRefresh=true;
							Intent it = new Intent(Global.Action_Friends_Status_Updated);
							mContext.sendBroadcast(it);
						}
					}

					friends = "";
					friendsIdx = "";//alec
					j = 0;
					n++;
					if (n > 2)
						break;
				}
			} while (cursor.moveToNext());
		}
		if(cursor!=null && !cursor.isClosed())
			cursor.close();

		if (friends.length() > 0) {

			outToServerPeriod(); //tml*** outToServer period/
			Log.d("2.450/" + myId + "/" + friendsIdx);
			thrClient.reset450();
			try {
				outToServer.write(MyUtil.encryptTCPCmd("450/" + myId + "/"
						+ friendsIdx));
			} catch (Exception e) {
				Log.e("fail to getFriendsOnlineStatus2");
				disconnect("fail fonline2", true);
				return false;
			}

			synchronized (lock_450) {
				try {
					lock_450.wait(TRANSIT_TIMEOUT * 3);
				} catch (InterruptedException e) {
				}
			}

			if (!thrClient.isReady450()) {
				Log.e("getFriendsOnlineStatus Timeout2");
				friends = "";
				disconnect("fail fonline2 timeout", true);
				return false;
			} else {
				
				if (needUpdateStatus && DialerActivity.getDialer()==null)
				{
					if (UserPage.sortMethod==2)
						UserPage.forceRefresh=true;
					Intent it = new Intent(Global.Action_Friends_Status_Updated);
					mContext.sendBroadcast(it);
				}
			}
		}

		mPref.writeLong("last_dlf_status", now);
		return true;
	}
	
	//tml*** outToServer Period
	public void outToServerPeriod () {
		try {
			long now = System.currentTimeMillis();
			long last = mPref.readLong("outToServerLast", 0);
			long tpast = (now - last);
//			Log.i("tmlotsp " + tpast + "=" + now + "-" + last);
			if ((tpast < minOutPeriod) && (tpast >= 0)) {
				int sleept = (int) (minOutPeriod - tpast);
				Log.w("otsp sleep=" + sleept + "ms");
				MyUtil.Sleep(sleept);
			} else {
//				Log.d("otsp a-ok");
			}
			mPref.writeLong("outToServerLast", System.currentTimeMillis());
		} catch (Exception e) {
			Log.e("otsp ohuh " + e.getMessage());
		}
	}
	//***tml
	//tml test
	private void displayCallStatus (String s) {
		if (AireJupiter.getInstance() != null)
			AireJupiter.getInstance().updateCallDebugStatus(false, "\n" + s);
	}
	//tml*** 700 v2
	/*
	 * 700/callerIDX/calleeIDX/callID
	 * 710/callerIDX/callID
	 * 720/callerIDX/calleeIDX/callID/sipServer
	 * 730/calleeIDX/callID/sipServer
	 * 740/senderIDX/receiverIDX/callID
	 * 750/senderIDX/callID
	 * 770/senderIDX/receiverIDX/callID
	 * 780/senderIDX/callID
	 * 790/calleeIDX/callID
	 */
	private static final int ids710 = 2;
	private static final int ids730 = 3;
	private static final int ids750 = 2;
	private static final int ids780 = 2;
	private static final int ids790 = 2;
	private String[] parse700 (String tcpString) {
		String[] string700 = tcpString.split("/");
		int size = string700.length;
		if (size > 1) {  //7xx/yyy/...
			//replace 7xx with array size info
			string700[0] = Integer.toString(size - 1);
		} else {
			//missing ids, error?
			Log.e("7xx missing ids !@#$");
			string700 = null;
		}
		return string700;
	}
}
