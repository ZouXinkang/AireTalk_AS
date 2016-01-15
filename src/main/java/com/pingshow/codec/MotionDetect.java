
package com.pingshow.codec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.SendAgent;
import com.pingshow.airecenter.SurveillanceDialog;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.network.MyNet;
import com.pingshow.util.MCrypt;
import com.pingshow.util.MyUtil;

public class MotionDetect {
	private MyPreference mPref;
	private Context mContext;
	private AmpUserDB mADB;
	
	static int TRIGGER_DIFF = 10000;  //tml|alec*** suv sense, was 8000/ old
	static int TRIGGER_SOUND = 2;  //tml|alec*** suv sense/ old
	final static int REST_TIME = 10000;  //10 sec
	final static int CAPTURE_NUM = 2;  //no# pics
	final static int FRAME_INTERVAL = 300;  //detection fps
	final static int INSTANT_CALL_INTERVAL=300000;  //call notify cooldown, 5 min
	
	static private boolean isloaded;
	public native int open(int w, int h);
	public native int detect(byte yuv[], int size);
	public native void close();
	
	private long detectTime, detectedTime;
	private boolean motionDetected;
	private int captured;
	private boolean rest = false;
	private String mAddress;
	private int mIdx;
	
	public static ArrayList<String> mImageQueue = new ArrayList<String>();
	private int width, height;
	private int cropdX, cropdY;
	private int cropdXhalf, cropdYhalf;
	private int pixels = 272384;  //(def 640x480->608x448=272384) set in load, pixels = cropdX * cropdY
	private int[] pixelsRGB0, pixelsRGB1, red_DIFF;
	private final static int MARGIN = 16;
	private boolean suvTLeft1 = true, suvTRight2 = true, suvBLeft3 = true, suvBRight4 = true;
	private double parTA = 1.0;
	private boolean checkFlicker = true;
	private int flickerLength = HALFROW_INDEX;
	private double parTT = 1.0;

	public boolean load(Context context, int w, int h, int to_idx, String to_address)
	{
		try {
			System.loadLibrary("motion");
			width = w;
			height = h;
			if (open(w, h) == 1) isloaded = true;
			mContext = context;
			mAddress = to_address;
			mIdx = to_idx;
			mPref = new MyPreference(mContext);
			mADB = new AmpUserDB(mContext);  //tml*** suv send more
			mADB.open();
			detectedTime = new Date().getTime();
			
			int padding = MARGIN * 2;
			cropdX = width - padding;
			cropdY = height - padding;
			pixels = cropdX * cropdY;  //640x480->608x448  272384
			pixelsRGB0 = new int[pixels];
			red_DIFF = new int[pixels];
			
			//tml*** suv mask
			cropdXhalf = cropdX / 2;
			cropdYhalf = cropdY / 2;
			suvTLeft1 = mPref.readBoolean("suvTLeft", true);
			suvTRight2 = mPref.readBoolean("suvTRight", true);
			suvBLeft3 = mPref.readBoolean("suvBLeft", true);
			suvBRight4 = mPref.readBoolean("suvBRight", true);
			String quad = "ALL";
			if (!(suvTLeft1 && suvTRight2 && suvBLeft3 && suvBRight4)) {
				int detectQds = 0;
				if (suvTLeft1) detectQds++;
				if (suvTRight2) detectQds++;
				if (suvBLeft3) detectQds++;
				if (suvBRight4) detectQds++;
				//threshold adjustments
				parTA = (double) detectQds / (double) 4;
				if (detectQds <= 1) parTT = 0.85;
				else if (detectQds <= 2) parTT = 0.90;
				else parTT = 1.0;
				//flicker modes
				if (!suvBLeft3 && !suvBRight4) checkFlicker = false;
				else checkFlicker = true;
				if ((!suvBLeft3 && suvBRight4) || (suvBLeft3 && !suvBRight4))  {
					flickerLength = HALFROW_INDEX;
				} else {
					flickerLength = cropdX;
				}
				//debug
				if (!suvBLeft3 && !suvBRight4) {  //T
					quad = "T";
					if (!suvTRight2) {  //L
						quad = quad + "L";
					} else if (!suvTLeft1) {  //R
						quad = quad + "R";
					}
				} else if (!suvTLeft1 && !suvTRight2) {  //B
					quad = "B";
					if (!suvBRight4) {  //L
						quad = quad + "L";
					} else if (!suvBLeft3) {  //R
						quad = quad + "R";
					}
				} else if (!suvTRight2 && !suvBRight4) {  //L
					quad = "L";
				} else if (!suvTLeft1 && !suvBLeft3) {  //R
					quad = "R";
				} else if (!suvTRight2 && !suvBLeft3) {  //'\'
					quad = "\\";
				} else if (!suvTLeft1 && !suvBRight4) {  //'/'
					quad = "/";
				} else if (!suvTLeft1 && suvTRight2 && suvBLeft3 && suvBRight4) {  //!TL
					quad = "!TL";
				} else if (suvTLeft1 && !suvTRight2 && suvBLeft3 && suvBRight4) {  //!TR
					quad = "!TR";
				} else if (suvTLeft1 && suvTRight2 && !suvBLeft3 && suvBRight4) {  //!BL
					quad = "!BL";
				} else if (suvTLeft1 && suvTRight2 && suvBLeft3 && !suvBRight4) {  //!BR
					quad = "!BR";
				}
			} else {
				parTA = 1.0;
				parTT = 1.0;
				checkFlicker = true;
				flickerLength = cropdX;
			}
			//***tml
			
			if (trackPerformance && TESTING) {
				timerCount = 0;
				totalyuvrgb = 0;
				totalimgdata = 0;
				totalobjectx = 0;
			}
			boolean testSUVntfy = mPref.readBoolean("testSUV", true);
			boolean testSUVrec = mPref.readBoolean("testSUV3", true);
			Log.e("  --- suv ****** NTFY" + testSUVntfy + " REC" + testSUVrec + " cX=" + cropdX + " cY=" + cropdY + " pix=" + pixels);
			Log.e("  --- suv ****** A:" + quad + " " + parTA + "/" + parTT + " " + suvTLeft1 + suvTRight2 + suvBLeft3 + suvBRight4);
		} catch (Throwable e) {
			Log.e("suv.Fail to load library libmotion");
		}
		return isloaded;
	}
	
	public Context getInstance() {
		return mContext;
	}
	
	void captureImg(byte[] data)
	{
		try {
			YuvImage yuv = null;
			yuv = new YuvImage(data, ImageFormat.NV21, width, height, null);
			String filename = Global.SdcardPath_temp + (new Date().getTime()) + ".jpg";
			FileOutputStream fout = new FileOutputStream(new File(filename));
			try {
	        	yuv.compressToJpeg(new Rect(0, 0, width, height), 90, fout);
	        	fout.flush();
			} catch (IOException e) {
				Log.e("suv.capture1 !@#$ " + e.getMessage());
			}
			try {
				fout.close();
				mImageQueue.add(filename);
				captured++;
			} catch (Exception e) {
				Log.e("suv.capture2 !@#$ " + e.getMessage());
			}
			Log.i("suv !! captureImg >> " + captured);
		} catch (Exception e) {
			Log.e("suv.capture !@#$ " + e.getMessage());
		}
	}
	
	private void triggerInstantCall()
	{
		boolean sendSMS = mPref.readBoolean("sendSMS", true);
		boolean sendCalls = mPref.readBoolean("sendCalls", true);
//		MCrypt mc = new MCrypt();  //tml|sw*** sec encrypt
		if (!sendSMS && !sendCalls) return;
		//tml*** suv send more
		long last = mPref.readLong("last_emergency_call", 0);
		long now = new Date().getTime();
		if (now - last < 30000) {  //30 sec
			Log.e("suv NO NOTIFY <30s (" + (now - last) + "s)");
			return;
		}
		mPref.writeLong("last_emergency_call", now);
		//***tml
		List<String> instants = mPref.readArray("instants");
		if (instants != null) {
//			long last=mPref.readLong("last_emergency_call", 0);
//			long now=new Date().getTime();
//			if (now-last<30000) return;//30 sec

//			mPref.writeLong("last_emergency_call", now);

			for (String number: instants) {
				Log.e("suv number=" + number);
				if (number != null && number.length() >= 7 && number.startsWith("+")) {
					if (number.startsWith("+")) number = number.substring(1);
					//tml*** suv send more
					onNewNotify(number);
					MyUtil.Sleep(100);
				}
			}
		}
	}
	
	//tml*** suv send more
	public void onNewNotify(String number) {
		TriggerNotifyRun notifyRunnable = new TriggerNotifyRun();
		if (notifyRunnable.setData(number))
			new Thread(notifyRunnable).start();
	}
	
	public class TriggerNotifyRun implements Runnable {
		private String _number;
		private String outNumber;
		private boolean sendSMS, sendCalls;
		private String myPhoneNumber, myPasswd, myNick, myIdx, myLang;
		private boolean securityOK = true;
		
		public boolean setData(String number)
		{
		    this._number = number;
		    sendSMS = mPref.readBoolean("sendSMS", true);
			sendCalls = mPref.readBoolean("sendCalls", true);
			//tml|sw*** new subs model
		    String securitySubscription = mPref.read("SecurityDueDate", "---");
			if (!securitySubscription.startsWith("success")) {
				securityOK = false;
			}
			MCrypt mc = new MCrypt();

			myPhoneNumber = mPref.read("myPhoneNumber", "++++");
			myPasswd = mPref.read("password", "1111");
			myNick = mPref.read("myNickname", "unknown");
			myIdx = mPref.read("myID", "0");
			myLang = mPref.read("ssendLang", "us_english");

			try {
				Log.i("suv " + myPhoneNumber + " " + myIdx + " " + myNick + " " + myPasswd + " " + _number + " " + myLang);
				myPhoneNumber = MCrypt.bytesToHex(mc.encrypt(myPhoneNumber)).toLowerCase();
				myIdx = MCrypt.bytesToHex(mc.encrypt(myIdx)).toLowerCase();
				myPasswd = MCrypt.bytesToHex(mc.encrypt(myPasswd)).toLowerCase();
				myNick = MCrypt.bytesToHex(mc.encrypt(myNick)).toLowerCase();
				outNumber = MCrypt.bytesToHex(mc.encrypt(_number)).toLowerCase();
				myLang = MCrypt.bytesToHex(mc.encrypt(myLang)).toLowerCase();
			} catch (Exception e) {
				Log.e("suv.notify encrypt (" + _number + ") !@#$ " + e.getMessage());
				return false;
			}
			return true;
		}
		
		@Override
		public void run()
		{
			try {
				MyNet net = new MyNet(mContext);
				//tml*** china ip
				String domain = AireJupiter.myAcDomain_default;
				if (AireJupiter.getInstance() != null) {
					domain = AireJupiter.getInstance().getIsoDomain();
				}
				//tml|sw*** new subs model
				if (!securityOK) {
					Log.e("suv subscription !@#$");
					//TODO expire message?
//					Intent intent = new Intent(Global.Action_InternalCMD);
//					intent.putExtra("Command", Global.CMD_SUV_CALLLIMIT);
//					mContext.sendBroadcast(intent);
					return;
				}
				
				if (sendSMS) {
					Log.e("suv EmergencySMS=" + _number);
					String Return = net.doAnyPost("http://" + domain + "/t/xmlrpc/customer/smsonly.php", 
							"username=" + myPhoneNumber
    						+ "&idx=" + myIdx
    						+ "&password=" + myPasswd
    						+ "&nickname=" + myNick
    						+ "&callee=" + outNumber
    						+ "&language=" + myLang);
					Log.i("suv ReturnSMS=" + Return);
				}
				
				if (sendCalls) {
					Log.e("suv EmergencyCALL=" + _number);
					String Return = net.doAnyPost("http://" + domain + "/t/xmlrpc/customer/callonly.php", 
							"username=" + myPhoneNumber
    						+ "&idx=" + myIdx
    						+ "&password=" + myPasswd
    						+ "&nickname=" + myNick
    						+ "&callee=" + outNumber
    						+ "&language=" + myLang);
					Log.i("suv ReturnCALL=" + Return);
					//tml*** suv call limit
					if (Return.equals("success")) {
						mPref.write("alertCallLimit", true);
						
						long now = new Date().getTime();
						long last = mPref.readLong("last_suv_maxalert_warn", 0);
						long elapsed = now - last;
						if (elapsed < 300000) {  //5m
							return;
						}
						mPref.writeLong("last_suv_maxalert_warn", now);
						
						Intent intent = new Intent(Global.Action_InternalCMD);
						intent.putExtra("Command", Global.CMD_SUV_CALLLIMIT);
						mContext.sendBroadcast(intent);
					} else if (Return.equals("successok")) {  //limit of calls per month reached
						mPref.write("alertCallLimit", false);
					}
				}
			} catch (Exception e) {
				Log.e("suv.notify php (" + _number + ") !@#$ " + e.getMessage());
			}
		}
	}
	//***tml
	
	private void startVideoRecording() {
		boolean recordingEnabled = mPref.readBoolean("recordingEnabled", true);
		if (!recordingEnabled) return;
		
		if (SurveillanceDialog.getInstance() != null) {
			SurveillanceDialog.getInstance().onVideoRecording();
		}
	}
	
	private void startSendingMsg() {
		if (mPref.readBoolean("testSUV3", true))
		new Thread(new Runnable() {
			public void run() {
				startVideoRecording();
			}
		}).start();
		
		boolean activateAlarm = mPref.readBoolean("AlarmNoise", false);
		if (activateAlarm) {  //tml*** suv alarm
			stopAlarm();
			MyUtil.Sleep(100);
			prepareAlarm();
		}
		
		new Thread(new Runnable() {
			public void run()
			{
				boolean sendPictures = mPref.readBoolean("sendPictures", true);
				if (sendPictures) {
					while(mImageQueue.size() > 0) {
						//tml*** suv send more
						String filename = mImageQueue.get(0);
						int myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
						List<String> instants = mPref.readArray("instants");
						
	        			if (instants != null) {
	        				for (String address : instants) {
	        					int suvIdx = mADB.getIdxByAddress(address);
	        					Log.e("suv (pic) hello=" + address + " " + suvIdx);
	        					if (suvIdx != -1) {
		    						SendAgent agent = new SendAgent(mContext, myIdx, suvIdx, true);
		    						agent.onSend(address, "(iMG) \nDetected!", 2, null, filename, true);
	        					}
	        					MyUtil.Sleep(100);
	        				}
	        			}
	        			mImageQueue.remove(0);
						MyUtil.Sleep(1000);
	        			//***tml
//						String filename=mImageQueue.remove(0);
//						
//						int myIdx=Integer.parseInt(mPref.read("myID","0"), 16);
//						Log.e("tmlsuv myIdx" + myIdx + " mIdx=" + mIdx + " mAddress=" + mAddress);
//						SendAgent agent=new SendAgent(mContext, myIdx, mIdx, true);
//						agent.onSend(mAddress, "(iMG) Detected!", 2, null, filename, true);
//						MyUtil.Sleep(1500);
					}
				}
				
				boolean useSound = mPref.readBoolean("SoundDetection", false);
				if (useSound) {
					int myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
					//tml*** suv send more
					List<String> instants = mPref.readArray("instants");
					if (instants != null) {
        				for (String address : instants) {
        					int suvIdx = mADB.getIdxByAddress(address);
        					Log.e("suv (snd) hello=" + address + " " + suvIdx);
        					if (suvIdx != -1) {
        						SendAgent agent = new SendAgent(mContext, myIdx, mIdx, true);
        						agent.onSend(mAddress, "Sound Detected!", 0, null, null, true);
        					}
        					MyUtil.Sleep(100);
        				}
        			}
					//***tml
//					SendAgent agent=new SendAgent(mContext, myIdx, mIdx, true);
//					agent.onSend(mAddress, "Detected!", 0, null, null, true);
				}
				rest = false;
			}
		}).start();
		
		new Thread(new Runnable() {
			public void run() {	
				triggerInstantCall();
				rest = false;
			}
		}).start();
	}

	public int picture_detect(byte yuv[], int size)
	{

//		if (new Date().getTime()-time<FRAME_INTERVAL || rest) return 0;
		if ((new Date().getTime() - detectTime < FRAME_INTERVAL || rest)
				&& (!motionDetected || captured > 0)) return 0;  //tml*** motion detect, dont wait for next frame to send
		detectTime = new Date().getTime();
		
		if (motionDetected) {
			captureImg(yuv);
			if (captured >= CAPTURE_NUM) {
				motionDetected = false;
				rest = true;
				captured = 0;
				startSendingMsg();
			}
			return 0;
		}
		
		int trigMdiff = mPref.readInt("SuvMotionSense", 2);
//		if (!mPref.readBoolean("testSUV2", true)) {
//			//old
//			//tml|alec*** suv sense, 130K, 100K, 60K, 40K, 20K
//			if (trigMdiff == 0) {
//				TRIGGER_DIFF = 130000;
//			} else if (trigMdiff == 1) {
//				TRIGGER_DIFF = 100000;
//			} else if (trigMdiff == 2) {
//				TRIGGER_DIFF = 60000;
//			} else if (trigMdiff == 3) {
//				TRIGGER_DIFF = 40000;
//			} else if (trigMdiff == 4) {
//				TRIGGER_DIFF = 20000;
//			} else {
//				TRIGGER_DIFF = 70000;
//			}
//		} else {
			//new
			if (trigMdiff == 0) {
				TRIGGER_DIFF = 600000;  //720p@2M, 480p@600k
			} else if (trigMdiff == 1) {
				TRIGGER_DIFF = 300000;  //720p@1M, 480p@300k
			} else if (trigMdiff == 2) {
				TRIGGER_DIFF = 120000;  //720p@400k, 480p@120k
			} else if (trigMdiff == 3) {
				TRIGGER_DIFF = 60000;  //720p@200k, 480p@60k
			} else if (trigMdiff == 4) {
				TRIGGER_DIFF = 30000;  //720p@100k, 480p@30000
			} else {
				TRIGGER_DIFF = 120000;  //trig 2
			}
//		}
		//***tml
		
		int diff = 0;
		boolean diff2 = false;
//		if (!mPref.readBoolean("testSUV2", true)) {
//			diff = detect(yuv, size);  //old
//		} else {
			diff2 = detect_V2(yuv, size, width, height, TRIGGER_DIFF, null);  //new
//		}
		
//		Log.e("SUV DIFF=" + diff + " DIFF2=" + diff2);
		
		if (mPref.readBoolean("testSUV", true)) {
			if (diff > TRIGGER_DIFF || diff2) {
				long passTime = new Date().getTime() - detectedTime;
				if (passTime < REST_TIME) {
					Log.e("suv xxxx VOID min time xxxx " + passTime + " <" + REST_TIME);
					return 0;
				}
				motionDetected = true;
				detectedTime = new Date().getTime();
				Log.e("!!!!!! SUV DETECTED !!!!!! _diff=" + diff + " _diff2=" + diff2);
			}
		}
		if (diff > TRIGGER_DIFF || diff2) {
			SurveillanceDialog.getInstance().detect_titlecolor(true);
		} else {
			SurveillanceDialog.getInstance().detect_titlecolor(false);
		}
		return 1;
	}
	
	//pix: *(1280x720)1248x688=858624  *(640x480)608x448=272384<<<
	private final static int NOISE_BREAK = 30;
	private final static int NOISE_BREAKMIN = 15;
//	private final static int RED_BREAK = red_break;  //720p@200k
//	private final static int RED_BREAK = testindex;  //720p@200k
	private final static int RED_MIN = 27;
	private final static int COLOR_MIN = 12;
	private final static int GRAY_DIV = 10;
	private final static int COLORLOSS_MIN = 8171520;  //pix*30, c < 30/255, 25758720|8171520
	private final static int COLOROVER_MAX = 59924480;  //pix*220, c > 220/255, 188897280|59924480
	private final static int HALFWAY_INDEX = 136192;  //pix/2, half down, 429312|136192
	private final static int HALFROW_INDEX = 304;  //w/2, half across, 624|304
	private final static int BOTTOM_EDGE_INDEX = 271776;  //pix - w, last row, 857376|271776
	private final static int OBJSIZE_MIN = 7566;  //pix/36, 23850|7566
	private final static int OBJSIZE_MAX = 54476;  //pix/5, 171724|54476
	private final static double RATIO_DIFF = 0.20;
	private final static double RATIO_DINC = 1.0 + RATIO_DIFF;
	private final static double RATIO_DDEC = 1.0 - RATIO_DIFF;
	private boolean trackPerformance = false;
	private int timerCount = 0;
	private long totalyuvrgb = 0;
	private long totalimgdata = 0;
	private long totalobjectx = 0;
	
	//tml|sw*** motion detect
	private boolean TESTING = Log.enDEBUG;
	public boolean detect_V2 (byte yuv[], int size, int width, int height, int red_break, Bitmap bitmap)
	{
		Log.e("  --- SUV ---------------");
		long currentTimer = 0;
		if (trackPerformance && TESTING) {
			currentTimer = System.currentTimeMillis();
			timerCount++;
		}
		int RED_BREAK = red_break;
		boolean detectMotion = false;
		Bitmap ibitmap = null;
		pixelsRGB1 = new int[pixels];
		
		if (bitmap == null) {
			try {
				YuvImage iyuv = null;
				iyuv = new YuvImage(yuv, ImageFormat.NV21, width, height, null);
				
				ByteArrayOutputStream bAOut = new ByteArrayOutputStream();
				iyuv.compressToJpeg(new Rect(0, 0, width, height), 90, bAOut);
				
				byte[] ibytes = bAOut.toByteArray();
				ibitmap = BitmapFactory.decodeByteArray(ibytes, 0, ibytes.length);
				bAOut.flush();
				bAOut.close();
			} catch (IOException e) {
				Log.e("detectJV ERR " + e.getMessage());
				return false;
			}
		} else {
			ibitmap = bitmap;
		}
		ibitmap.getPixels(pixelsRGB1, 0, cropdX, MARGIN - 1, MARGIN - 1, cropdX, cropdY);  //row-wise, pixels[0 : pixels-1]
		if (ibitmap != null) ibitmap.recycle();
		ibitmap = null;
		if (trackPerformance && TESTING) totalyuvrgb = totalyuvrgb + (System.currentTimeMillis() - currentTimer);
		
		
		//******color-based detection******
		//720 | 480				600	| 188
		//YUV to RGB			100	| 51
		//GATHER RGB DATA		480	| 130
		//-color data				| 
		//-FLICKER/ERROR			| 
		//-shadow					| 
		//OBJECT DERIVATION		20	| 7
		//MOTION DETECTED, red based
		//-LACK/OVER COLOR
		//-OBJECT
		//-SHADOW
		
		int pixs = 1;
		int color0, color1;
		int red_value0 = 0, grn_value0 = 0, blu_value0 = 0;
		int red_value1 = 0, grn_value1 = 0, blu_value1 = 0;
		int red_SUM1 = 0, grn_SUM1 = 0, blu_SUM1 = 0;
		int red_AVG1 = 0, grn_AVG1 = 0, blu_AVG1 = 0;
		int red_SUMDIFF1 = 0;
		int red_ITEMS = 0;
		int red_AVGDIFF = 0;
		int grn_DIFF = 0, blu_DIFF = 0;
		int rg_DIFF = 0, gb_DIFF = 0, rb_DIFF = 0;
		int red_HIGHDIFF = 1;
		int flkrbottom_CNT = 0;
		int shadow_CNT = 0;
		boolean halfdown = false;
		boolean halfacross = false;
		int halfiRow = 0;
		int iRow = 0;
		//GATHER RGB DATA
		for (int i = 0; i < pixels; i++) {
			//tml*** suv mask
			//quadrant exclusions
			//camera left/right swapped
			if (!(suvTLeft1 && suvTRight2 && suvBLeft3 && suvBRight4)) {
				if (i >= HALFWAY_INDEX) halfdown = true;
				else halfdown = false;
				
				iRow = i / cropdX;
//				if (i % cropdX == 0) iRow++;
				halfiRow = HALFROW_INDEX + cropdX * iRow;
				if (i >= halfiRow) halfacross = false;  //true
				else halfacross = true;  //false
				
				if (!suvBLeft3 && !suvBRight4) {  //T
					if (halfdown) continue;
					if (!suvTRight2) {  //L
						if (halfacross) continue;
					} else if (!suvTLeft1) {  //R
						if (!halfacross) continue;
					}
				} else if (!suvTLeft1 && !suvTRight2) {  //B
					if (!halfdown) continue;
					if (!suvBRight4) {  //L
						if (halfacross) continue;
					} else if (!suvBLeft3) {  //R
						if (!halfacross) continue;
					}
				} else if (!suvTRight2 && !suvBRight4) {  //L
					if (halfacross) continue;
				} else if (!suvTLeft1 && !suvBLeft3) {  //R
					if (!halfacross) continue;
				} else if (!suvTRight2 && !suvBLeft3) {  //'\'
					if ((!halfdown && halfacross) || (halfdown && !halfacross)) continue;
				} else if (!suvTLeft1 && !suvBRight4) {  //'/'
					if ((halfdown && !halfacross) || (!halfdown && halfacross)) continue;
				} else if (!suvTLeft1 && suvTRight2 && suvBLeft3 && suvBRight4) {  //!TL
					if (!halfdown && !halfacross) continue;
				} else if (suvTLeft1 && !suvTRight2 && suvBLeft3 && suvBRight4) {  //!TR
					if (!halfdown && halfacross) continue;
				} else if (suvTLeft1 && suvTRight2 && !suvBLeft3 && suvBRight4) {  //!BL
					if (halfdown && !halfacross) continue;
				} else if (suvTLeft1 && suvTRight2 && suvBLeft3 && !suvBRight4) {  //!BR
					if (halfdown && halfacross) continue;
				}
				pixs++;
			}
			
			color0 = pixelsRGB0[i];
			red_value0 = Color.red(color0);
			grn_value0 = Color.green(color0);
			blu_value0 = Color.blue(color0);
			
			color1 = pixelsRGB1[i];
			red_value1 = Color.red(color1);
			grn_value1 = Color.green(color1);
			blu_value1 = Color.blue(color1);
			
			//rgb sums, diff between frame1(now) and frame0(prev)
			red_DIFF[i] = Math.abs(red_value0 - red_value1);
			grn_DIFF = Math.abs(grn_value0 - grn_value1);
			blu_DIFF = Math.abs(blu_value0 - blu_value1);
			
			//rgb sums
			red_SUM1 = red_SUM1 + red_value1;
			grn_SUM1 = grn_SUM1 + grn_value1;
			blu_SUM1 = blu_SUM1 + blu_value1;
			
			//threshold to gather red difference
			if (red_DIFF[i] >= NOISE_BREAK) {
				//colorful
				if ((grn_value1 > (0 + COLOR_MIN) && grn_value1 < (255 - COLOR_MIN))
						&& (blu_value1 > (0 + COLOR_MIN) && blu_value1 < (255 - COLOR_MIN))) {
					//non-gray
					rg_DIFF = Math.abs(red_value1 - grn_value1);
					gb_DIFF = Math.abs(grn_value1 - blu_value1);
					rb_DIFF = Math.abs(red_value1 - blu_value1);
					if (!(rg_DIFF < GRAY_DIV && gb_DIFF < GRAY_DIV && rb_DIFF < GRAY_DIV)) {
						
						red_ITEMS++;
						red_SUMDIFF1 = red_SUMDIFF1 +  red_DIFF[i];
						//find highest red difference
						if (red_HIGHDIFF < red_DIFF[i]) {
							red_HIGHDIFF = red_DIFF[i];
						}
						
					}
				}
			}
			
			//FLICKER/ERROR detection
			if (checkFlicker && i >= BOTTOM_EDGE_INDEX) {  //only bottom edge
				if (grn_value1 == 255) flkrbottom_CNT++;
				if (flkrbottom_CNT >= flickerLength) {  //suv mask $$$
					Log.e("suv xxxx FLICKER ERR xxxx @" + (i / flickerLength));
					detectMotion = false;
					return detectMotion;
				}
			}
//			if (i % cropdX == 0 || nRow) {  //any horizontal line
//				if ((red_value1 == 0 && grn_value1 == 255 && blu_value1 == 0)) {
//					nRow = true;
//					flkrbottom_CNT++;
//					if (flkrbottom_CNT == cropdX) {
//						Log.e("suv xxxx FLICKER ERR xxxx @" + (i / cropdX));
//						detectMotion = false;
//						return detectMotion;
//					}
//				} else {
//					nRow = false;
//					flkrbottom_CNT = 0;
//				}
//			}
			
			//shadow/tint/blurr, comp between frame1(now) and frame0(prev)
			if ((red_DIFF[i] > NOISE_BREAKMIN && grn_DIFF < NOISE_BREAKMIN && blu_DIFF < NOISE_BREAKMIN)
					|| (grn_DIFF > NOISE_BREAKMIN && red_DIFF[i] < NOISE_BREAKMIN && blu_DIFF < NOISE_BREAKMIN)
					|| (blu_DIFF > NOISE_BREAKMIN && grn_DIFF < NOISE_BREAKMIN && red_DIFF[i] < NOISE_BREAKMIN)) {  //shadow
				shadow_CNT++;
			} else if (red_DIFF[i] < NOISE_BREAKMIN && grn_DIFF > NOISE_BREAKMIN && blu_DIFF > NOISE_BREAKMIN) {  //tint
				//red tint
				double gb_RATCOMP = ((double) grn_value1 / ((double) blu_value1 + 1))
						/ (((double) grn_value0 / ((double) blu_value0 + 1) + 1));
				if ((gb_RATCOMP >= 1.0 && gb_RATCOMP < RATIO_DINC)
						|| (gb_RATCOMP > RATIO_DDEC && gb_RATCOMP <= 1.0)) {
					shadow_CNT++;
				}
			} else if (grn_DIFF < NOISE_BREAKMIN && red_DIFF[i] > NOISE_BREAKMIN && blu_DIFF > NOISE_BREAKMIN) {  //tint
				//green tint
				double rb_RATCOMP = ((double) red_value1 / ((double) blu_value1 + 1))
						/ (((double) red_value0 / ((double) blu_value0 + 1) + 1));
				if ((rb_RATCOMP >= 1.0 && rb_RATCOMP < RATIO_DINC)
						|| (rb_RATCOMP > RATIO_DDEC && rb_RATCOMP <= 1.0)) {
					shadow_CNT++;
				}
			} else if (blu_DIFF < NOISE_BREAKMIN && grn_DIFF > NOISE_BREAKMIN && red_DIFF[i] > NOISE_BREAKMIN) {  //tint
				//blue tint
				double gr_RATCOMP = ((double) grn_value1 / ((double) red_value1 + 1))
						/ (((double) grn_value0 / ((double) red_value0 + 1) + 1));
				if ((gr_RATCOMP >= 1.0 && gr_RATCOMP < RATIO_DINC)
						|| (gr_RATCOMP > RATIO_DDEC && gr_RATCOMP <= 1.0)) {
					shadow_CNT++;
				}
			}
			
		}
		//save current frame to previous/temp
		pixelsRGB0 = pixelsRGB1;
		pixelsRGB1 = null;
		
		//average rgb and red_diff
		red_AVG1 = red_SUM1 / (int) (pixels * parTA);
		grn_AVG1 = grn_SUM1 / (int) (pixels * parTA);
		blu_AVG1 = blu_SUM1 / (int) (pixels * parTA);
		if (red_ITEMS < 1) red_ITEMS = 1;
		red_AVGDIFF = red_SUMDIFF1 / red_ITEMS;
		if (trackPerformance && TESTING) totalimgdata = totalimgdata + (System.currentTimeMillis() - currentTimer);
		

		int obj_LEN = 0;
		int obj_SUMLEN = 0;
		int red_OBJCOMP = 1;
//		int obj_ITEMS = 0;
		int obj_START = 0;
		int obj_CONT = 0;
		//OBJECT DERIVATION
		//set threshold to gather red object
		red_OBJCOMP = red_AVGDIFF / 2;
		if (red_OBJCOMP < 1) {
			red_OBJCOMP = 1;
		}
		
		for (int iy = 0; iy < cropdY; iy++) {  //by rows
			for (int ix = 0; ix < cropdX; ix++) {
				int index = (iy * cropdY) + ix;  //set index 0,720,1420...
				
				//threshold to gather red object
				if (red_DIFF[index] >= red_OBJCOMP) {
//					obj_ITEMS++;  //count of object points
					//first instance of object
					if (obj_START == 0) {
						obj_START = ix;
					}
					//last instance of object
					obj_CONT = ix;
				}
			}
			//distance of length between first and last instance of object
			obj_LEN = obj_CONT - obj_START;
			obj_START = 0;
			//sum of lengths = general-area of object
			obj_SUMLEN = obj_SUMLEN + obj_LEN;
		}
		if (trackPerformance && TESTING) totalobjectx = totalobjectx + (System.currentTimeMillis() - currentTimer);
		
		
		String falseLog = "";
		String extraLog = "";
		//MOTION DETECTED - compare sum of red_diff to threshold
		//detection based on red
		if (red_SUMDIFF1 > (RED_BREAK * parTT)) {
			Log.e("suv **** MOTION DETECTED **** "
					+ "." + red_ITEMS + "x" + red_AVGDIFF + " : " + red_SUMDIFF1 + " >" + RED_BREAK);
			detectMotion = true;
		} else {
			extraLog = "[" + pixs + "] ";
			falseLog = "-- M " + "." + red_ITEMS + "x" + red_AVGDIFF + " : " + red_SUMDIFF1 + " " + extraLog;
			extraLog = "";
		}
		
		
		//LACK/OVER COLOR - if red color low, overall color low, all color high
		int red_LOSS = (red_AVG1 < RED_MIN) ? 1 : 0;
		int red_LOST = (red_SUM1 < (COLORLOSS_MIN * parTA)) ? 1 : 0;
		int grn_LOST = (grn_SUM1 < (COLORLOSS_MIN * parTA)) ? 1 : 0;
		int blu_LOST = (blu_SUM1 < (COLORLOSS_MIN * parTA)) ? 1 : 0;
		int red_OVER = (red_SUM1 > (COLOROVER_MAX * parTA)) ? 1 : 0;
		int grn_OVER = (grn_SUM1 > (COLOROVER_MAX * parTA)) ? 1 : 0;
		int blu_OVER = (blu_SUM1 > (COLOROVER_MAX * parTA)) ? 1 : 0;
		int color_LOSS = red_LOST + grn_LOST + blu_LOST;
		int color_OVER = red_OVER + grn_OVER + blu_OVER;
		if (red_LOSS == 1 || color_LOSS >= 2 || color_OVER == 3) {
			Log.e("suv xxxx --/++ COLOR xxxx "
					+ red_LOSS + "/" + color_LOSS + "/" + color_OVER + " "
					+ "r=" + red_AVG1 + "(" + red_HIGHDIFF + "!) g=" + grn_AVG1 + " b=" + blu_AVG1);
			detectMotion = false;
			if (!TESTING) return detectMotion;
		} else {
			falseLog = falseLog + "-- C " + "r" + red_AVG1 + "(" + red_HIGHDIFF + "!)" + " g" + grn_AVG1 + " b" + blu_AVG1 + " ";
		}
		
		
		//OBJECT1 - compare object's general-area to smallest possible true-area
		//set true-area of object if object was at highest red
		//OBJECT2 - minimum size
		int numPoint_MAX = (int) ((red_SUM1 * 4) / (red_HIGHDIFF * 5 + 1));
		if (obj_SUMLEN > numPoint_MAX) {
//			extraLog = " x" + String.format("%.2f", (double)obj_SUMLEN/(double)numPoint_MAX);
			Log.e("suv xxxx NOT OBJECT1 xxxx "
					+ obj_SUMLEN + " >" + numPoint_MAX + extraLog);
			detectMotion = false;
			if (!TESTING) return detectMotion;
		} else if (obj_SUMLEN < OBJSIZE_MIN) {
			Log.e("suv xxxx NOT OBJECT2 xxxx "
					+ obj_SUMLEN);
			detectMotion = false;
			if (!TESTING) return detectMotion;
		} else {
//			extraLog = " x" + String.format("%.2f", (double)obj_SUMLEN/(double)numPoint_MAX);
			falseLog = falseLog + "-- O " + obj_SUMLEN + " <" + numPoint_MAX + extraLog + " ";
//			extraLog = "";
		}
		
		
		//SHADOW - detect gradual differences (shadow/blurring)
		if (shadow_CNT > (OBJSIZE_MIN * parTT)) {
			Log.e("suv xxxx SHADOW/BLUR xxxx "
					+ shadow_CNT);
			detectMotion = false;
			if (!TESTING) return detectMotion;
		} else {
			falseLog = falseLog + "-- S " + shadow_CNT;
		}
		
		
		if (falseLog.length() > 0) {
			Log.e("suv " + falseLog);
		}
		return detectMotion;
	}
	//***tml

	public void release() {
		try {
			if (trackPerformance && TESTING) Log.e("suv END ." + timerCount + " " + totalyuvrgb + " " + totalimgdata + " " + totalobjectx);
			close();
			stopSoundDetect();
		} catch (Exception e) {
			Log.e("suv.release !@#$ " + e.getMessage());
		}
		isloaded = false;
	}
	
	public boolean isReady() {
		return isloaded;
	}
	
	private int alarmThreshold = 500;
	private AudioRecord recorder;
	public static boolean bRecording = false;
	final int sframe_rate = 1500;  //50fx30s
	private int total_sndsize;
	private int r_ptr = 0, w_ptr = 0;
	private int lastAvg = 0;
	public int sAvg = 100;
	private int silenceAvg = 0;
	private boolean soundDetectNotify = false;
	private short[] pcm;
	private int training = 400;

	public void initSoundDetect()
	{
		int bufSize = AudioRecord.getMinBufferSize(8000, 
					AudioFormat.CHANNEL_CONFIGURATION_MONO, 
					AudioFormat.ENCODING_PCM_16BIT);
		
		if (Build.MODEL.toLowerCase().contains("i897")) bufSize=16000;
		try {
			recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, 
					AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 
					bufSize * 4);
		} catch (IllegalArgumentException e) {
			Log.e("suv.Fail to create AudioRecord");
		}
		
		total_sndsize = 160 * (sframe_rate + 1);
	}
	
	public void startSoundDetect()
	{
		if (recorder != null) {
			//tml*** suv alarm
			Intent intent = new Intent(Global.Action_Start_Surveillance);
			intent.putExtra("Command", Global.CMD_SUVALARM_OFF);
			mContext.sendBroadcast(intent);
			
			int count = 0;
			while (recorder.getState() != AudioRecord.STATE_INITIALIZED && (count++ < 20))
				MyUtil.Sleep(10);
			
			if (count >= 20) {
				recorder.release(); // Now the object cannot be reused
				stopSoundDetect();
				return;
			}
			
			Thread thr = new Thread(null, mRecording, "RecordingVoiceMemo");
			thr.start();
		}
	}
	
	final Runnable mEncoding = new Runnable() {
		public void run()
		{
			MyUtil.Sleep(500);
			soundDetectNotify = false;
			
			do {
				if (Math.abs(w_ptr - r_ptr) <= 160 && bRecording) {
					MyUtil.Sleep(100);
					if (soundDetectNotify) {
						motionDetected = false;
						rest = true;
						startSendingMsg();
						captured = 0;
						soundDetectNotify = false;
//						MyUtil.Sleep(10000);
						MyUtil.Sleep(3000);  //tml|sw*** sec encrypt/
					}
					continue;
				}
				
				int sum = 0;
				for(int i = 0; i < 160; i++)
					sum += Math.abs(pcm[r_ptr + i]);
				
				sum = sum / 160;
				if (sum > 100) {
					sAvg=(int)(0.4f*sAvg+0.6*sum);
//					Log.i("Avg="+Avg);
				}
				
				r_ptr += 160;
				r_ptr %= total_sndsize;
				
				if (training > 0) {
					training--;
					if (training == 0) {
						//tml|alec*** suv sense, 2,3,4,5,6
						int trigSdiff = mPref.readInt("SuvSoundSense", 2);
						if (trigSdiff == 0) {
							TRIGGER_SOUND = 6;
						} else if (trigSdiff == 1) {
							TRIGGER_SOUND = 5;
						} else if (trigSdiff == 2) {
							TRIGGER_SOUND = 4;
						} else if (trigSdiff == 3) {
							TRIGGER_SOUND = 3;
						} else if (trigSdiff == 4) {
							TRIGGER_SOUND = 2;
						} else {
							TRIGGER_SOUND = 4;
						}
						//***tml
						silenceAvg = sAvg;
//						alarmThreshold=silenceAvg*3;
						alarmThreshold = silenceAvg * TRIGGER_SOUND;  //tml|alec*** suv sense
						Log.i("*** silenceAvg=" + silenceAvg);
					}
				}
				
				if (training == 0 && !rest && ((sAvg - silenceAvg) > alarmThreshold)) {
					soundDetectNotify = true;
				}
				
			} while (bRecording);
		}
	};
	
	final Runnable mRecording = new Runnable() {
		public void run()
		{
			bRecording = true;
			try {
				pcm = new short[total_sndsize];
			} catch (OutOfMemoryError e) {
				total_sndsize = 160 * 300;
				pcm = new short[total_sndsize];
			}
			
			MyUtil.Sleep(10);
			
			r_ptr = 0;
			w_ptr = 0;
			
			Thread thr = new Thread(null, mEncoding, "EncodingVoiceMemo");
			thr.start();
			
			recorder.startRecording();
			do {
				recorder.read(pcm, w_ptr, 160);
				w_ptr += 160;
				w_ptr %= total_sndsize;
				//Log.i(""+w_ptr/160);
			} while (bRecording);
		
			try {
				recorder.stop();
				recorder.release();  //Now the object cannot be reused
				recorder = null;
			} catch (Exception e) {
				Log.e("suv.mRecording stop !@#$ " + e.getMessage());
			}
		}
	};

	public void stopSoundDetect() {
		bRecording = false;
		System.gc();
//		System.gc();
	}
	
	//tml*** suv alarm
	private void prepareAlarm () {
		Intent intent = new Intent(Global.Action_Start_Surveillance);
		intent.putExtra("Command", Global.CMD_SUVALARM_ON);
		mContext.sendBroadcast(intent);
	}
	
	private void stopAlarm () {
		Intent intent = new Intent(Global.Action_Start_Surveillance);
		intent.putExtra("Command", Global.CMD_SUVALARM_OFF);
		mContext.sendBroadcast(intent);
	}
	//***tml
}
