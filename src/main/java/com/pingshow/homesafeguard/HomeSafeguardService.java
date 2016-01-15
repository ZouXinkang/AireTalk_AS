package com.pingshow.homesafeguard;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.SendAgent;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.homesafeguard.HomeSafeguard.DeviceActiveListener;
import com.pingshow.network.MyNet;
import com.pingshow.util.MCrypt;
import com.pingshow.util.MyUtil;

public class HomeSafeguardService extends Service {
	private MyPreference mPref;
	private HomeSafeguard mHomeSafeguard;
	private String[] deviceCategories;
	private HomeSafeguadBinder binder = new HomeSafeguadBinder();
	private Handler mHandler = new Handler();
	
	
	//数据监听
	private Set<ReadDataListener> mReadDataListeners = new HashSet<ReadDataListener>();
	
	public static interface ReadDataListener{
		public void onReadData(UsbData data);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mPref = new MyPreference(getApplicationContext());
		Log.d("HomeSafeguardService is created!");
		mHomeSafeguard = HomeSafeguard.getInstance(getApplicationContext());
		mHomeSafeguard.setDeviceActiveListener(deviceActiveListener);
		mHomeSafeguard.setReadDataListener(readDataListener);
		deviceCategories = getResources().getStringArray(R.array.device);
		
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(mPref.readBoolean("openHomeSafeguard", false)){
			start();
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	public void start() {
		if (mHomeSafeguard != null) {
			mHomeSafeguard.init();
			mHomeSafeguard.start();
			Log.d("HomeSafeguardService is started!");
		}
	}

	public void stop() {
		if (mHomeSafeguard != null) {
			mHomeSafeguard.stop();
			mHomeSafeguard.release();
			Log.d("HomeSafeguardService is stoped!");
		}
	}
	HomeSafeguard.ReadDataListener readDataListener = new HomeSafeguard.ReadDataListener() {
		
		@Override
		public void onReadData(UsbData data) {
			if(mReadDataListeners != null){
				for(ReadDataListener readDataListener : mReadDataListeners){
					readDataListener.onReadData(data);
				}
			}
		}
		
	};
	
	DeviceActiveListener deviceActiveListener = new DeviceActiveListener() {
		private Toast toast;

		@Override
		public void onDeviceActive(UsbDevice device) {
			UsbData data = device.getData();
			SimpleDateFormat dateFormater = new SimpleDateFormat("HH:mm:ss  yyyy-MM-dd", Locale.US);
			long time = data.getTime();
			String timeStr = dateFormater.format(new Date(time));
			String deviceName = device.getName();
			String deviceCat = null;
			int deviceAddr = device.getAddress();
			String text = null;
			
			switch (device.getCategory()) {
				case UsbDevice.DEV_TELECONTROLLER:
					deviceCat = getString(R.string.device_home_remote);
					text = deviceName + "\n" + deviceCat + " : " + data.getKey() + "\n(" + timeStr + ")";
					break;
				case UsbDevice.DEV_MAGNETISM_SENSOR:
					deviceCat = getString(R.string.device_home_door);
					text = deviceName + "\n" + deviceCat + "\n(" + timeStr + ")";
					break;
				case UsbDevice.DEV_WATCH_DOG:
					deviceCat = getString(R.string.device_home_infrared);
					text = deviceName + "\n" + deviceCat + "\n(" + timeStr + ")";
					break;
				case UsbDevice.DEV_SMOKE_SENSOR:
					deviceCat = getString(R.string.device_home_smoke);
					text = deviceName + "\n" + deviceCat + "\n(" + timeStr + ")";
					break;
				case UsbDevice.DEV_TEMPERATURE_SENSOR:
					deviceCat = getString(R.string.device_home_temp);
					text = deviceName + "\n" + deviceCat + "\n(" + timeStr + ")";
					break;
				default:
					break;
			}

			//tml*** suv send more
			long last = mPref.readLong("usbLastAlert" + Integer.toString(deviceAddr), 0);
			long now = new Date().getTime();
			if (now - last < 10000) {  //15 sec
				Log.e("suv-home NO NOTIFY <10s (" + (now - last) + "s)");
				return;
			}
			mPref.writeLong("usbLastAlert" + Integer.toString(deviceAddr), now);
			//***tml
			
			boolean securityEnabled = mPref.readBoolean("securityEnabled", true);
			boolean securityHomeIOTEnabled = mPref.readBoolean("securityHomeIOT", false);
			if (securityEnabled) {
				if (text != null && text.length() > 0) {
					String datastr = setUsbDeviceStatus(HomeSafeguardService.this, deviceAddr, false);  //tml*** iot status
					if (AireJupiter.getInstance() != null) {
						AireJupiter.getInstance().uploadSuvStatus(datastr);
					}
					showToast(text);
					
					if (securityHomeIOTEnabled) {
						boolean activateAlarm = mPref.readBoolean("AlarmNoise", false);
						if (activateAlarm) {  //tml*** suv alarm
							mHandler.removeCallbacks(delayStopAlarm);
							stopAlarm();
							MyUtil.Sleep(100);
							prepareAlarm();
							mHandler.postDelayed(delayStopAlarm, 10000);
						}
					}
					
					if (securityHomeIOTEnabled) {
						startSendingMsg(deviceName, deviceCat);
					}
				}
				Log.e("suv-home detected=" + securityHomeIOTEnabled + " " + deviceAddr + " " + text);
			}
		}

		private void showToast(String text) {
			if (toast == null) {
				toast = Toast.makeText(getApplicationContext(), text,
						Toast.LENGTH_LONG);
			}
			toast.setText(text);
			toast.setGravity(Gravity.CENTER, 0, 0);
			try {
				LinearLayout tstLay1 = (LinearLayout) toast.getView();
				TextView tstTV1 = (TextView) tstLay1.getChildAt(0);
				tstTV1.setTextSize(30);
				tstTV1.setGravity(Gravity.CENTER);
				tstTV1.setTextColor(Color.RED);
			} catch (ClassCastException e) {}
			toast.show();
		}
	};
	//tml*** iot status
	private String setUsbDeviceStatus (Context context, int alertedDeviceAddr, boolean off) {
		UsbDeviceMatcher deviceMatcher = UsbDeviceMatcher.getInstance(context);
		List<UsbDevice> devices = deviceMatcher.getAllUsbDevice();
		String phpString = "";
		
		if (devices != null) {
			int z = devices.size();
			
			if (z >= 0) {
				phpString = "&number=" + z;
				String perDeviceString = "";
				String perDeviceStatus = "";
				
				for (int i = 0; i < z; i++) {
					UsbDevice device = devices.get(i);
					
					String name = device.getName();
					String category = deviceCategories[device.getCategory()];
					String namehash = name + ":" + category;

					boolean isOpen = device.isOpen();
					int status = isOpen ? 0 : 2;
					if (off) status = 2;
					int addr = device.getAddress();
					if (alertedDeviceAddr == addr) status = 1;
					
					if ((i + 1) < 10) {
						perDeviceString = "&device0" + (i + 1) + "=";
						perDeviceStatus = "&status0" + (i + 1) + "=";
					} else {
						perDeviceString = "&device" + (i + 1) + "=";
						perDeviceStatus = "&status" + (i + 1) + "=";
					}
					
					perDeviceString = perDeviceString + namehash + perDeviceStatus + status;
					
					phpString = phpString + perDeviceString;
				}
				
			}
		}

		Log.d("setUsbDeviceStatus = " + phpString);
		mPref.write("SUVSTATUS", phpString);
		return phpString;
	}
	//tml*** iot control
	private void startSendingMsg(final String deviceName, final String deviceCat)
	{	
		new Thread(new Runnable() {
			public void run() {
				triggerAireMsg(deviceName, deviceCat);
			}
		}).start();

		new Thread(new Runnable() {
			public void run() {
				triggerInstantCall();
			}
		}).start();
		
	}
	
	private void triggerAireMsg(String deviceName, String deviceCat)
	{
		AmpUserDB mADB = new AmpUserDB(HomeSafeguardService.this);  //tml*** suv send more
		mADB.open();
		
		int myIdx = Integer.parseInt(mPref.read("myID", "0"), 16);
		//tml*** suv send more
		List<String> instants = mPref.readArray("instants");
		if (instants != null) {
			for (String address : instants) {
				int suvIdx = mADB.getIdxByAddress(address);
				Log.e("suv-home hello=" + address + " " + suvIdx);
				if (suvIdx != -1) {
					SendAgent agent = new SendAgent(HomeSafeguardService.this, myIdx, suvIdx, true);
					agent.onSend(address, getString(R.string.home_iot_alert) 
							+ "  " + deviceName + " (" + deviceCat + ")"
							, 0, null, null, true);
				}
				MyUtil.Sleep(100);
			}
		}
		
		mADB.close();
	}
	
	private void triggerInstantCall()
	{
		boolean sendSMS = mPref.readBoolean("sendSMS", true);
		boolean sendCalls = mPref.readBoolean("sendCalls", true);
		if (!sendSMS && !sendCalls) return;
		//tml*** suv send more
		long last = mPref.readLong("last_emergency_call", 0);
		long now = new Date().getTime();
		if (now - last < 15000) {  //15 sec
			Log.e("suv-home NO NOTIFY <10s (" + (now - last) + "s)");
			return;
		}
		mPref.writeLong("last_emergency_call", now);
		//***tml
		List<String> instants = mPref.readArray("instants");
		if (instants != null) {
			for (String number: instants) {
				Log.e("suv-home number=" + number);
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
	private void onNewNotify(String number) {
		TriggerNotifyRun notifyRunnable = new TriggerNotifyRun();
		if (notifyRunnable.setData(number))
			new Thread(notifyRunnable).start();
	}
	
	public class TriggerNotifyRun implements Runnable {
		private String _number;
		private String outNumber;
		private boolean sendSMS, sendCalls;
		private String myPhoneNumber, myPasswd, myNick, myIdx, myLang;
		
		public boolean setData(String number)
		{
		    this._number = number;
		    sendSMS = mPref.readBoolean("sendSMS", true);
			sendCalls = mPref.readBoolean("sendCalls", true);
			MCrypt mc = new MCrypt();

			myPhoneNumber = mPref.read("myPhoneNumber", "++++");
			myPasswd = mPref.read("password", "1111");
			myNick = mPref.read("myNickname", "unknown");
			myIdx = mPref.read("myID", "0");
			myLang = mPref.read("ssendLang", "us_english");

			try {
				Log.i("suv-home " + myPhoneNumber + " " + myIdx + " " + myNick + " " + myPasswd + " " + _number + " " + myLang);
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
				MyNet net = new MyNet(getApplicationContext());
				//tml*** china ip
				String domain = AireJupiter.myAcDomain_default;
				if (AireJupiter.getInstance() != null) {
					domain = AireJupiter.getInstance().getIsoDomain();
				}
				
				if (sendSMS) {
					Log.e("suv-home EmergencySMS=" + _number);
					String Return = net.doAnyPost("http://" + domain + "/t/xmlrpc/customer/smsonly.php", 
							"username=" + myPhoneNumber
    						+ "&idx=" + myIdx
    						+ "&password=" + myPasswd
    						+ "&nickname=" + myNick
    						+ "&callee=" + outNumber
    						+ "&language=" + myLang);
					Log.i("suv-home ReturnSMS=" + Return);
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
					Log.i("suv-home ReturnCALL=" + Return);
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
						getApplicationContext().sendBroadcast(intent);
					} else if (Return.equals("successok")) {  //limit of calls per month reached
						mPref.write("alertCallLimit", false);
					}
				}
			} catch (Exception e) {
				Log.e("suv.notify php (" + _number + ") !@#$ " + e.getMessage());
			}
		}
	}

	private void prepareAlarm () {
		Intent intent = new Intent(Global.Action_Start_Surveillance);
		intent.putExtra("Command", Global.CMD_SUVALARM_ON);
		sendBroadcast(intent);
	}
	
	private void stopAlarm () {
		Intent intent = new Intent(Global.Action_Start_Surveillance);
		intent.putExtra("Command", Global.CMD_SUVALARM_OFF);
		sendBroadcast(intent);
	}
	
	private Runnable delayStopAlarm = new Runnable () {
		@Override
		public void run() {
			stopAlarm();
		}
	};
	//***tml
	@Override
	public void onDestroy() {
		super.onDestroy();
		mHandler.removeCallbacks(delayStopAlarm);
		stopAlarm();
		stop();
		Intent ImIntent=new Intent(getApplicationContext(), HomeSafeguardService.class);
		startService(ImIntent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return (IBinder) binder;
	}
	
	public class HomeSafeguadBinder extends Binder{

		public void startService(){
			start();
		}
		public void stopService(){
			stop();
		}
		/**
		 * 注册一个usb数据的监听器,<br/>
		 *    注意: 注册后,不用时,记得调用unregisterReadDataListener()方法取消注册.
		 * @param readDataListener
		 */
		public void registerReadDataListener(
				ReadDataListener readDataListener) {
			mReadDataListeners.add(readDataListener);
			Log.d("register ReadDataListener, count of listener: "+mReadDataListeners.size());
		}
		/**
		 * 
		 * @param 	readDataListener 注册时传入的监听器对象.
		 */
		public void unregisterReadDataListener(
				ReadDataListener readDataListener) {
			mReadDataListeners.remove(readDataListener);
			Log.d("unregister ReadDataListener, count of listener: "+mReadDataListeners.size());
		}
		public String setUsbDeviceStatus (Context context, int alertedDeviceAddr, boolean off) {
			return HomeSafeguardService.this.setUsbDeviceStatus(context, alertedDeviceAddr, off);
		}
		public void onNewNotify(String number) {
			HomeSafeguardService.this.onNewNotify(number);
		}
	}
}
