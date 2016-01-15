package com.pingshow.airecenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.SecurityHistoryAdapter.UploadFileContent;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.view.MySurfaceView;
import com.pingshow.airecenter.view.ProgressBar;
import com.pingshow.codec.MotionDetect;
import com.pingshow.network.MyNet;
import com.pingshow.util.MyUtil;

public class SurveillanceDialog extends Activity implements OnInfoListener, OnErrorListener{
	private MyPreference mPref;
	private Handler mHandler=new Handler();
	private Surveillance suv;
	private SurfaceView mSurfaceView1;
	private MySurfaceView mSurfaceView2;
	static SurveillanceDialog instance;
	private AmpUserDB mADB;
	
	private MediaRecorder recorder;
	private SurfaceHolder holder;
	
	private boolean recordingMode=false;
	private boolean recording = false;
	private String filenameToRecord;
	private long startRecordingTime=0;
	private String datetime;
	
	private PowerManager.WakeLock mWakeLock;
	private Camera camera;
	
	static public SurveillanceDialog getInstance()
	{
		return instance;
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.surveillance);
		
		WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.40f;
		getWindow().setAttributes(lp);
		
		mPref = new MyPreference(this);
		mPref.write("emergencyCallIn", false);

		mSurfaceView1=(SurfaceView)findViewById(R.id.preview);
		mSurfaceView2=(MySurfaceView)findViewById(R.id.recording_view);
		
		instance=this;
		
		mADB = new AmpUserDB(this);  //tml*** suv ctrl id
		mADB.open();
		
		String nickname=getIntent().getStringExtra("nickname");
		if (nickname!=null)
			((TextView)findViewById(R.id.displayname)).setText(nickname);
		
		((Button)findViewById(R.id.close)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				updateLength();
				mHandler.removeCallbacks(timeElapsed);
				if (suv!=null)
				{
					suv.stop();
					suv=null;
				}
				stopAlarm(); //tml*** suv alarm
				
				nicknameOff = mPref.read("myNickname", "");
				new Thread(sendSecurityOff).start();
				
				destroyRecorder();
				finish();
			}
		});
		
		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
				PowerManager.ACQUIRE_CAUSES_WAKEUP, "SurveillanceDlg");
		if (mWakeLock != null)
			mWakeLock.acquire();
		
		IntentFilter intentToReceiveFilter = new IntentFilter();
		intentToReceiveFilter.addAction(Global.Action_End_Surveillance);
		registerReceiver(parseCommand, intentToReceiveFilter);
		//tml*** suv mask
		if (mPref.readBoolean("suvTLeft", true)) ((LinearLayout) findViewById(R.id.previewTLeft)).setBackgroundColor(0);
		if (mPref.readBoolean("suvTRight", true)) ((LinearLayout) findViewById(R.id.previewTRight)).setBackgroundColor(0);
		if (mPref.readBoolean("suvBLeft", true)) ((LinearLayout) findViewById(R.id.previewBLeft)).setBackgroundColor(0);
		if (mPref.readBoolean("suvBRight", true)) ((LinearLayout) findViewById(R.id.previewBRight)).setBackgroundColor(0);
		
		mHandler.postDelayed(timeElapsed, 1000);
		
		//tml|sw*** new subs model
    	if (AireJupiter.getInstance() != null)
			AireJupiter.getInstance().requestSecuritySubscription();
    	
		startDetection();
	}
	
	String nicknameOff;
	Runnable sendSecurityOff=new Runnable() {
		public void run() {
			int count=0;
			List<String> instants=mPref.readArray("instants");
			if (instants!=null)
			{
				for (String address: instants)
				{
					if (AireJupiter.getInstance()!=null && AireJupiter.getInstance().tcpSocket()!=null && address.length()>0)
					{
						String suvNotify = getString(R.string.security_off)
								+ "  (" + getString(R.string.surveillance)
								+ ")  < " + nicknameOff;  //tml*** suv ctrl id
						if (mPref.readBoolean("emergencyCallIn",false)) {
							suvNotify = suvNotify + " (" + getString(R.string.in_call) + ")";
						}
						AireJupiter.getInstance().tcpSocket().send(address, suvNotify, 0, null, null, 0, null);
						count++;
						if (count>6) return;
						MyUtil.Sleep(1000);
					}
					else
						return;
				}
			}
		}
	};
	
	private void startDetection()
	{
		recordingMode=false;
		
		if (mPref.readBoolean("MotionDetection", true))
			mSurfaceView1.setVisibility(View.VISIBLE);
		else{
			float mDensity=this.getResources().getDisplayMetrics().density;
			RelativeLayout.LayoutParams lp=new RelativeLayout.LayoutParams((int)(106*mDensity), (int)(80*mDensity));
			mSurfaceView1.setLayoutParams(lp);
			mSurfaceView1.setVisibility(View.INVISIBLE);
		}
		
		if (mPref.readBoolean("SoundDetection", false))
		{
			((ProgressBar)findViewById(R.id.strength)).setVisibility(View.VISIBLE);
			((ProgressBar)findViewById(R.id.strength)).setImage(R.drawable.sound, R.drawable.sound_bg);
		}
		
		mPref.write("SecurityDetecting", true);
		
		mSurfaceView2.setVisibility(View.INVISIBLE);
		
		mHandler.postDelayed(new Runnable(){
			public void run()
			{
				suv=new Surveillance();
				
				int toIdx=mPref.readInt("SuvHostIDX");
				String toAddress=mPref.read("SuvHostAddress","");
				
				suv.start(SurveillanceDialog.this, toIdx, toAddress, mSurfaceView1, ((ProgressBar)findViewById(R.id.strength)));
				startTime=new Date().getTime();//alec
				stopAlarm();  //tml*** suv alarm
			}
		},50);
	}
	
	public void onVideoRecording()
	{
		mHandler.post(new Runnable(){
			public void run()
			{
				startRecording();
			}
		});
		
	}
	
	public void startRecording()
	{
		if (recording) return;
		
		if (suv!=null)
		{
			suv.stop();
			suv=null;
		}
		Log.e(" *** SUV *** startRecording startRecording");
		recordingMode=true;
		
		mSurfaceView2.setVisibility(View.VISIBLE);
		
		float mDensity=this.getResources().getDisplayMetrics().density;
		RelativeLayout.LayoutParams lp=new RelativeLayout.LayoutParams((int)(235*mDensity), (int)(192*mDensity));
		mSurfaceView1.setLayoutParams(lp);
		mSurfaceView1.setVisibility(View.INVISIBLE);
		
		mSurfaceView2.setVideoSize(640, 480);
		
        initRecorder();
        ((TextView)findViewById(R.id.status_label)).setTextColor(Color.RED);
	}
	
	private void initRecorder() {
		if (recording) {
			destroyRecorder();
		}
		init();
		try {
			recorder.setOnErrorListener(this);
			recorder.setOnInfoListener(this);
			recorder.prepare();
			Thread.sleep(500);
			recorder.start();
		} catch (Exception e) {
			destroyRecorder();
		} 
		recording = true;

		List<Map<String, String>> items = mPref.readMapArray("recordHistory");

		Map<String, String> map = new HashMap<String, String>();

		startRecordingTime = new Date().getTime();
		datetime = MyUtil.getDate(1);

		map.put("0", filenameToRecord);
		map.put("1", "" + startRecordingTime);
		map.put("2", "01:00");  //recorder.setMaxDuration(60000)
		map.put("3", datetime);  //tml*** upload suv

		items.add(0, map);

		mPref.writeMap("recordHistory", items);
		
		if (mPref.readInt("suvUP_" + filenameToRecord + "-" + datetime, 0) == 0) {  //tml*** upload suv
			mPref.write("suvUP_" + filenameToRecord + "-" + datetime, 0);
		}
	}

	public void init() {

		camera = mSurfaceView2.getCamera();
		camera.unlock();
		recorder = new MediaRecorder();
		recorder.setCamera(camera);
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		
//		CamcorderProfile cp=CamcorderProfile.get(0,CamcorderProfile.QUALITY_LOW);
		CamcorderProfile cp=CamcorderProfile.get(0,CamcorderProfile.QUALITY_LOW);  //alex*** security resolution ++/
		recorder.setProfile(cp);
		Log.d( "mProfile1 videoFrameRate::"+cp.videoFrameRate+ " videoBitRate::" + cp.videoBitRate + " audioBitRate::" + cp.audioBitRate + " fileformat::" + cp.fileFormat);
		//recorder.setVideoFrameRate(15);//需要设置帧率。
//		recorder.setVideoSize(640, 480);
		recorder.setVideoFrameRate(10);  //alex*** security resolution ++
		recorder.setVideoSize(1280, 720);
//		recorder.setVideoSize(640, 480);
//		recorder.setMaxDuration(180000);
		recorder.setMaxDuration(60000);  //map.put("2", "01:00")
		recorder.setMaxFileSize(0);
//		recorder.setVideoEncodingBitRate(1000000);
//		recorder.setAudioEncodingBitRate(bitRate);
//		recorder.setProfile(cp);
		Log.d( "mProfile2 videoFrameRate::"+cp.videoFrameRate+ " videoBitRate::" + cp.videoBitRate + " audioBitRate::" + cp.audioBitRate + " fileformat::" + cp.fileFormat);

		recorder.setPreviewDisplay(mSurfaceView2.getHolder().getSurface());

		filenameToRecord = generateFilename();
		recorder.setOutputFile(filenameToRecord);
	}
	
	private String generateFilename()
	{
		File[] files = new File(Global.SdcardPath_record).listFiles();
		//tml*** suv save dest
		if (mPref.readBoolean("suvsavedest", false)) {
			//TODO
			String suvsavedest_ext = mPref.read("suvsavedest_ext", getResources().getString(R.string.recording_external));
			File extpath = new File(suvsavedest_ext);
			String extpathName = extpath.getPath();
		} else {
			//TODO
		}
		int max=-1;
		for (File file : files)
		{
			String name=file.getName();
			String tmp;
			if(name.startsWith(".")) continue;
			if (!file.isDirectory() && name.contains("vid_"))
			{
				int dot=name.lastIndexOf(".");
				if (dot>3)
				{
					tmp=name.substring(dot-3, dot);
					int d=0;
					try{
						d=Integer.parseInt(tmp);
					}catch(Exception e)
					{}
					if (d>max)
						max=d;
				}
			}
		}
		
		max++;

		//tml*** suv save dest
		if (mPref.readBoolean("suvsavedest", false)) {
			//TODO
			String suvsavedest_ext = mPref.read("suvsavedest_ext", getResources().getString(R.string.recording_external));
			File extpath = new File(suvsavedest_ext);
			String extpathName = extpath.getPath();
		} else {
			//TODO
		}
		Log.d(Global.SdcardPath_record+"vid_"+String.format("%03d", max)+".mp4");
		return Global.SdcardPath_record+"vid_"+String.format("%03d", max)+".mp4";
	}
	
	
	private void destroyRecorder() {
		Log.e(" *** SUV *** destroyRecorder destroyRecorder");
		
		if (recorder != null) {
			if (recording) {
				try {
					recorder.setOnErrorListener(null);
					recorder.setOnInfoListener(null);
					recorder.stop();

				} catch (RuntimeException e) {
					e.printStackTrace();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						Log.e("sleep for second stop error!!");
					}
					try {
						recorder.stop();
					} catch (Exception e2) {
						e.printStackTrace();
						Log.e("stop fail2" );
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							Log.e("sleep for reset error Error");
						}
					}
				}
				recording = false;
			}
			
			recorder.reset();
			recorder.release();

			recorder = null;
	        ((TextView)findViewById(R.id.status_label)).setTextColor(Color.parseColor("#303030"));
	        //tml|sw*** new subs model
	        String securitySubscription = mPref.read("SecurityDueDate", "---");
	        if (securitySubscription.startsWith("success")) {
				//tml*** upload suv
				Log.i("auto upload suv " + !Running);
				if (mPref.readInt("suvUP_" + filenameToRecord + "-" + datetime, 0) == 0
						&& !Running) {
					fileTag = filenameToRecord + "-" + datetime;
					filePath = backCompatTagDate(fileTag, 0);
					fileDate = backCompatTagDate(fileTag, 1);
					fileTag = filePath;
					mPref.write("suvUP_" + fileTag, 10);
					new Thread(readingFileContent).start();
				}
	        }
		}
	}
	
	private long startTime;
	private Runnable timeElapsed = new Runnable() {
		@Override
		public void run() {
			long sec=(new Date().getTime()-startTime)/1000;
			((TextView)findViewById(R.id.status_label)).setText(DateUtils.formatElapsedTime(sec));
			mHandler.postDelayed(timeElapsed, 1000);
		}
	};
	
	private void updateLength()
	{
		if (startRecordingTime==0) return;
		
		List<Map<String, String>> items = mPref.readMapArray("recordHistory");
		
		if (items.size()>0)
		{
			Map<String, String> map = items.remove(0);
			
			long t=(new Date().getTime()-startRecordingTime)/1000;
	        String length=String.format("%02d:%02d", t/60, t%60);
	        map.put("2", length);
	        items.add(0, map);
	        
	        mPref.writeMap("recordHistory", items);
	        
	        if (SecurityNewActivity.getInstance()!=null)
				SecurityNewActivity.getInstance().refreshHistory();
		}
		
		startRecordingTime=0;
	}
	
	@Override
	public void onDestroy() {
		if (mADB != null && mADB.isOpen())
			mADB.close();
		updateLength();
		unregisterReceiver(parseCommand);
		mHandler.removeCallbacks(timeElapsed);
		if (mWakeLock!=null) mWakeLock.release();
		if (suv!=null)
		{
			suv.stop();
			suv=null;
		}
		stopAlarm(); //tml*** suv alarm
		destroyRecorder();
		instance=null;
		mPref.write("SecurityDetecting", false);
		if (SecurityNewActivity.getInstance() != null) {  //tml*** iot control
			SecurityNewActivity.getInstance().updateHomeIOTStatus();
		}
		super.onDestroy();
	}
	
	//tml*** suv offclick crash
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			//disables outside click
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//disables outside click
		return true;
	}
	//***tml
	
	BroadcastReceiver parseCommand = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context,final Intent intent) {
			if (intent.getAction().equals(Global.Action_End_Surveillance)) {
				
				destroyRecorder();
				
				String xaddress = intent.getStringExtra("address");
				if (xaddress != null && xaddress.length() > 0)
					nicknameOff = mADB.getNicknameByAddress(xaddress);  //tml*** suv ctrl id
				
				mHandler.removeCallbacks(timeElapsed);
				if (suv!=null)
				{
					suv.stop();
					suv=null;
				}
				stopAlarm(); //tml*** suv alarm
				
				new Thread(sendSecurityOff).start();

				finish();
			}
		}
	};

	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {
		switch (what) {
		case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
			Log.d("MEDIA_RECORDER_INFO_UNKNOWN");
			destroyRecorder();
			startDetection();
			break;
		case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
			Log.d("MEDIA_RECORDER_INFO_MAX_DURATION_REACHED");
			destroyRecorder();
			startDetection();
			break;
		case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
			Log.d("MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED");
			destroyRecorder();
			startDetection();
			break;
		}
		
	}

	@Override
	public void onError(MediaRecorder mr, int what, int extra) {
		recorder.stop();
		recorder.release();
		recorder = null;
		recording=false;
		Log.d("MediaRecorder err code=="+what);
		if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
			Log.d("MEDIA_RECORDER_ERROR_UNKNOWN");
			finish();
		}
	}
	
	//tml*** suv alarm
	private void stopAlarm () {
		Intent intent = new Intent(Global.Action_Start_Surveillance);
		intent.putExtra("Command", Global.CMD_SUVALARM_OFF);
		sendBroadcast(intent);
	}

	//tml*** upload suv
	private volatile boolean Running = false;
	private String fileTag, filePath, fileDate, fileName, fileDir;
	private volatile int partUse;
	private ArrayList<byte[]> mPartitionQueue = new ArrayList<byte[]>();
	private ArrayList<String> mFilePaths = new ArrayList<String>();
	Runnable readingFileContent = new Runnable() {
		public void run() {
			Log.e(" *** SUV *** beginRecordUpload beginRecordUpload");
			Running = true;
			boolean runParting = false;
			boolean partitionOK = false;
			
			if (mPartitionQueue != null && mPartitionQueue.size() > 0) mPartitionQueue.clear();
			if (mFilePaths != null && mFilePaths.size() > 0) mFilePaths.clear();
			
			File file = new File(filePath);
			long fileSize = file.length();
			if (fileSize <= 0) {
				mPref.write("suvUP_" + fileTag, 2);
				if (SecurityNewActivity.getInstance() != null)
					SecurityNewActivity.getInstance().notifyHistory(2);
				Running = false;
				return;
			}
			fileDir = filePath.substring(0, filePath.lastIndexOf("/") + 1);
			fileName = filePath.substring(fileDir.length());
			Log.d("readingFileContent prep fileDir=" + fileDir + " filePath=" + filePath);
			Log.d("readingFileContent prep fileName=" + fileName + " fileSize=" + fileSize);
			
			double partSize2 = 10000000;
			double partSize1 =  1000000;
			double partUnit  =  1000000;
			int numPartsRU, numPartsRD;
			numPartsRU = (int) Math.ceil((fileSize / partSize1));
			numPartsRD = (int) Math.ceil((fileSize / (int) partSize1));
			partUse = (int) (partSize1 / partUnit);
			if (numPartsRU > 100) {
				numPartsRU = (int) Math.ceil((fileSize / partSize2));
				numPartsRD = (int) Math.ceil((fileSize / (int) partSize2));
				partUse = (int) (partSize2 / partUnit);
			}
			Log.d("readingFileContent prep numParts(" + partUse + ")=" + numPartsRU + "," + numPartsRD);

			FileInputStream frIStream = null;
			int remainSize;
			int chunkSize = (int) (partUse * partUnit);
			int chunkRead = 0;
			int readLength;
			runParting = true;
			
			do {
				try {
					frIStream = new FileInputStream(new File(filePath));
				} catch (Exception e) {
					Log.e("FileInputStream ERR! " + e.getMessage());
					runParting = false;
					return;
				}

				for (int i = 0; i < numPartsRU; i++) {
					try {
						remainSize = (int) fileSize - i * chunkSize;
						if (remainSize < chunkSize) {
							chunkSize = remainSize;
						}
						byte[] fileContent = new byte[chunkSize];
						readLength = frIStream.read(fileContent);
						if (readLength >= 0) {
							chunkRead++;
							mPartitionQueue.add(fileContent);
							Log.d("readingFileContent read +" + chunkRead + " fileContent=" + readLength);
//							Thread.sleep(10 + mOutputQueue.size() * 20);
						}
						
//						if (frIStream != null) {
//							frIStream.close();
//							frIStream = null;
//						}
					} catch (Exception e) {
						Log.e("readFileContent ERR " + e.getMessage());
						runParting = false;
						break;
					}
				}

				try {
					if (frIStream != null) {
						frIStream.close();
						frIStream = null;
					}
				} catch (Exception e) {
					frIStream = null;
				} finally {
					if (!runParting) break;
				}

				FileOutputStream fwOStream = null;
				int partNum, resumSize = 0;
				
				for (int i = 0; i < numPartsRU; i++) {
					partNum = i + 1;
					String partNumS;
					if (partNum < 10) {
						partNumS = "_00" + Integer.toString(partNum);
					} else if (partNum < 100) {
						partNumS = "_0" + Integer.toString(partNum);
					} else {
						partNumS = "_" + Integer.toString(partNum);
					}
					String newPartFileDir = Global.SdcardPath_temp;
					String newPartFileNameN = fileName.substring(0, fileName.lastIndexOf(".")) + partNumS;
					String newPartFilePath = newPartFileDir + newPartFileNameN;
					mFilePaths.add(newPartFilePath);
//					String getNewPartFilePath = mFilePaths.get(i); //test
//					File newPartFile = new File(getNewPartFilePath);
					File newPartFile = new File(newPartFilePath);
					if (newPartFile.exists()) {
						newPartFile.delete();
					}
					
					try {
						byte[] newFileContent = mPartitionQueue.remove(0);
						fwOStream = new FileOutputStream(new File(newPartFilePath));
						fwOStream.write(newFileContent);
						
						if (fwOStream != null) {
							fwOStream.close();
							fwOStream = null;
						}
						
						int newPartFileLength = (int) newPartFile.length();
						resumSize = resumSize + newPartFileLength;
						Log.d("readingFileContent write newFilePath=" + newPartFilePath + " " + newPartFileLength);
					} catch (Exception e) {
						Log.e("writeFileContent ERR " + e.getMessage());
						break;
					}
				}

				try {
					if (fwOStream != null) {
						fwOStream.close();
						fwOStream = null;
					}
				} catch (Exception e) {
					fwOStream = null;
				}

				int totalPartFiles = 0;
				if (mFilePaths != null && mFilePaths.size() > 0) {
					totalPartFiles = mFilePaths.size();
				}
				
				if (runParting) partitionOK = true;
				Log.d("readingFileContent partition END" + runParting + " " + resumSize + " #" + totalPartFiles);
				
				runParting = false;
			} while (runParting);
			
			runParting = false;
			if (partitionOK) {
				readyUploadFileContent(0);
			} else {
				mPref.write("suvUP_" + fileTag, 2);
			}
		}
	};

	private void readyUploadFileContent(int offset) {
		uploadedCount = 0;
		abortupload = false;
		if (mFilePaths != null && mFilePaths.size() > 0) {
			String path;
			int countFilePaths = mFilePaths.size();

			mPref.write("suvUP_" + fileTag, 1);
			if (SecurityNewActivity.getInstance() != null)
				SecurityNewActivity.getInstance().notifyHistory(1);
			
			for (int i = offset; i < countFilePaths; i++) {
				UploadFileContent uploadFileContent = new UploadFileContent();
				path = mFilePaths.get(i);
				uploadFileContent.setData(countFilePaths, path);
				new Thread(uploadFileContent).start();
//				if (i == 3) abortupload = true; //test
				MyUtil.Sleep(300);
				if (abortupload) break;
			}
			if (abortupload) {
				mPref.write("suvUP_" + fileTag, 2);
				if (SecurityNewActivity.getInstance() != null)
					SecurityNewActivity.getInstance().notifyHistory(2);
				Running = false;
				for (int i = offset; i < countFilePaths; i++) {
					path = mFilePaths.get(i);
					MyNet net = new MyNet(SurveillanceDialog.this);
					net.stopUploading(path);
				}
			}
			mFilePaths.clear();
		} else {
			mPref.write("suvUP_" + fileTag, 2);
			if (SecurityNewActivity.getInstance() != null)
				SecurityNewActivity.getInstance().notifyHistory(2);
			Running = false;
		}
	}
	
	private volatile int uploadedCount = 0;
	private volatile boolean abortupload = false;
	public class UploadFileContent implements Runnable {
		private String uFilePath;
		private int fileCount;
		
		public void setData(int count, String path) {
			this.uFilePath = path;
			this.fileCount = count;
		}
		
		@Override
		public void run() {
			int myidx = Integer.valueOf(mPref.read("myID", "0"), 16);
			String myid = mPref.read("myPhoneNumber");
			String Return = "";
//			String Return = "Done"; //test
			try {
				File file = new File(uFilePath);
				if (file.exists()) {
					String filename = uFilePath.substring(uFilePath.lastIndexOf("/") + 1).replace(" ", "");
					filename = URLEncoder.encode(filename, "UTF-8");
					int count = 0;
					do {
						if (abortupload) return;
						MyNet net = new MyNet(SurveillanceDialog.this);
						Return = net.doPostAttachSUV("http://" + AireJupiter.myPhpServer_main + "/onair/airesecurityupload.php", myidx,
								filename, uFilePath);
						if (Return.startsWith("Done"))
							break;
						count++;
						MyUtil.Sleep(1000);
					} while (count < 1);
					file.delete();
				}
    		} catch (Exception e) {
    			Log.e("UploadFileContent ERR " + e.getMessage());
    		}
			
			if (Return.contains("Done")) {
    			uploadedCount++;
    			Log.d("UploadFileContent uploadedCount=" + uploadedCount + "/" + fileCount);
	        } else {
				mPref.write("suvUP_" + fileTag, 2);
				abortupload = true;
				Running = false;
				if (SecurityNewActivity.getInstance() != null)
					SecurityNewActivity.getInstance().notifyHistory(2);
				Running = false;
	        	Log.e("UploadFileContent FAIL");
	        }
			
			if (uploadedCount == fileCount) {
				new Thread(notifyUploadSUV).start();
			}
		}
	}
	
	Runnable notifyUploadSUV = new Runnable() {
		@Override
		public void run() {
			int myidx = Integer.valueOf(mPref.read("myID", "0"), 16);
			String myid = mPref.read("myPhoneNumber");
			String Return = "";
//			String Return = "Done"; //test
			try {
				String filename = filePath.substring(filePath.lastIndexOf("/") + 1).replace(" ", "");
				String fileext = filename.substring(filename.lastIndexOf("."));
				String filesrc = filename.substring(0, filename.lastIndexOf("."));
				filesrc = URLEncoder.encode(filesrc, "UTF-8");
				filename = filesrc + "-" + fileDate + fileext;
				filename = URLEncoder.encode(filename, "UTF-8");
				myid = URLEncoder.encode(myid, "UTF-8");
				int count = 0;
				do {
					MyNet net = new MyNet(SurveillanceDialog.this);
					Return = net.doAnyPostHttp("http://" + AireJupiter.myPhpServer_main + "/onair/merge.php",
							"id=" + myid
							+ "&source=" + filesrc
							+ "&output=" + filename);
					if (Return.startsWith("Done"))
						break;
					count++;
					MyUtil.Sleep(1000);
				} while (count < 3);
    		} catch (Exception e) {
    			Log.e("notifyUploadSUV ERR " + e.getMessage());
    		}
			
			if (Return.contains("Done")) {
				mPref.write("suvUP_" + fileTag, 3);
				if (SecurityNewActivity.getInstance() != null)
					SecurityNewActivity.getInstance().notifyHistory(3);
	        } else {
				mPref.write("suvUP_" + fileTag, 2);
				if (SecurityNewActivity.getInstance() != null)
					SecurityNewActivity.getInstance().notifyHistory(2);
	        	Log.e("UploadFileContent notify FAIL");
	        }
			Running = false;
		}
	};
	
	public String backCompatTagDate(String tag, int part) {
		String goodTag = "";
		String dateext = tag.substring(tag.length() - 13, tag.length());
		if (part == 0) {  //path
			if (dateext.contains("-")) {
				goodTag = tag.substring(0, tag.lastIndexOf("-"));
			} else {
				goodTag = tag;
			}
		} else if (part == 1) {  //date
			if (dateext.contains("-")) {
				goodTag = dateext.substring(dateext.indexOf("-") + 1);
			} else {
				goodTag = "nodate";
			}
		}
		return goodTag;
	}
	//***tml
	
	private long time = 0;
	public void detect_titlecolor (boolean change) {
		if (change) {
			time = new Date().getTime();
			((TextView) findViewById(R.id.titlesuv)).setTextColor(Color.RED);
		} else {
			if (new Date().getTime() - time < 500) return;
			time = new Date().getTime();
			((TextView) findViewById(R.id.titlesuv)).setTextColor(Color.BLACK);
		}
	}
	
}
