package com.pingshow.homesafeguard;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.widget.Toast;

import com.pingshow.airecenter.Log;
import com.wch.wchusbdriver.CH34xAndroidDriver;
//li*** Home safeguard
public class HomeSafeguard {
	private static final String ACTION_USB_PERMISSION = "com.wch.wchusbdriver.USB_PERMISSION";
	private static HomeSafeguard mHomeSafeguard;
	private CH34xAndroidDriver uartInterface;
	private Context mContext;
	/* thread to read the data */
	public readThread handlerThread;
	public boolean read_enable = false;
	public boolean isConfiged = false;
	protected final Object ThreadLock = new Object();
	private Handler handler = new Handler(){
		public void handleMessage(Message msg) {
			UsbData data = (UsbData) msg.obj;
			if(mReadDataListener != null){
				mReadDataListener.onReadData(data);
			}
			if(mDeviceActiveListener != null ){
				UsbDevice device = deviceMatcher.getUsbDeviceByData(data);
				if(device != null &&device.isOpen()){
					mDeviceActiveListener.onDeviceActive(device);
				}
			}
		};
	};
	//异常监听
	private DeviceActiveListener mDeviceActiveListener;
	public void setDeviceActiveListener(
			DeviceActiveListener deviceActiveListener) {
		this.mDeviceActiveListener = deviceActiveListener;
	}
	public static interface DeviceActiveListener{
		public void onDeviceActive(UsbDevice device);
	}
	//数据监听
	private ReadDataListener mReadDataListener;
	public void setReadDataListener(
			ReadDataListener readDataListener) {
		this.mReadDataListener = readDataListener;
	}
	public static interface ReadDataListener{
		public void onReadData(UsbData data);
	}
	private HomeSafeguard(Context context) {
		if (context == null) {
			Log.e("construct: context is null!");
			return;
		}
		mContext = context;
		deviceMatcher = UsbDeviceMatcher.getInstance(context);
		
	}
	public static synchronized HomeSafeguard getInstance(Context context){
		if(mHomeSafeguard == null){
			mHomeSafeguard = new HomeSafeguard(context);
		}
		return mHomeSafeguard;
	}

	public void init() {
		if(uartInterface == null){
			uartInterface = new CH34xAndroidDriver(
					(UsbManager) mContext.getSystemService(Context.USB_SERVICE),
					mContext, ACTION_USB_PERMISSION);
		}
		if (!uartInterface.UsbFeatureSupported()) {
			Toast.makeText(mContext, "No Support USB host API",
					Toast.LENGTH_SHORT).show();
			uartInterface = null;
			return;
		}
		if (2 == uartInterface.ResumeUsbList()) {
			uartInterface.CloseDevice();
			Log.d( "Enter onResume Error");
		}
		config();
		
	}
	private void config() {
		if(!isConfiged){
//			Log.d("driver connection: " + uartInterface.isConnected());
			if (uartInterface.isConnected()) {
				if (!uartInterface.UartInit()) {
					Log.d( "Init Uart Error");
				} else {
					if (uartInterface.SetConfig(9600, (byte) 8, (byte) 1, (byte) 0,
							(byte) 0)) {
						Log.d("Configed");
						isConfiged = true;
					}
				}
			}
		}
	}

	private UsbDeviceMatcher deviceMatcher;	
	public void start() {
		if (read_enable == false) {
			read_enable = true;
			handlerThread = new readThread();
			handlerThread.start();
		}
	}
	public void stop(){
		if (read_enable == true) {
			read_enable = false;
		}
		isConfiged = false;
		handler.removeCallbacksAndMessages(null);
	}
	public void release(){
		stop();
		if (uartInterface != null) {
			if (uartInterface.isConnected()) {
				uartInterface.CloseDevice();
			}
			uartInterface = null;
		}
		mHomeSafeguard = null;
	}
	private class readThread extends Thread {

		char[] readBuffer = new char[8];
		int actualNumBytes = 0;
		long readCount = 0;
		
		public void run() {
			while (read_enable) {
				SystemClock.sleep(50);
				if(++readCount % 10000 == 0){
					Log.d("iterate config!");
					isConfiged = false;
				}
				synchronized (ThreadLock) {
					if (uartInterface != null) {
						config();
						actualNumBytes = uartInterface.ReadData(readBuffer, 8);
						if (actualNumBytes > 0) {
							UsbData usbData = parseData();
							if(usbData != null){
								Message msg = Message.obtain();
								msg.obj = usbData;
								handler.sendMessage(msg);
							}
						}
					}else{
						Log.e("Read failure! HomeSafeguard is not init!!");
						read_enable = false;
					}
				}
			}
		}
		private UsbData parseData() {
			if (actualNumBytes == 8) {
				
				int[] d = new int[8];
				for (int i = 0; i < d.length; i++) {
					d[i] = readBuffer[i] & 0xFF;
				}
				int head = (d[0] << 8) + d[1];
				
				int data = (d[2] << 24) + (d[3] << 16) + (d[4] << 8) + d[5];
				int resistance = d[6];
				String encode = "";
				int address = 0;
				int key = 0;
				if (d[7] == 0x27) {
					encode = "1527";
					address = (d[2] << 16) + (d[3]) << 8 + d[4];
					key = d[5];
				} else if (d[7] == 0x62) {
					encode = "2262";
					address = (d[2] << 8) + d[3];
					key = (d[4] << 8) + d[5];
				}
				actualNumBytes = 0;
				return new UsbData(head, address, data, key, resistance,encode,System.currentTimeMillis());
			}else{
				Log.w("The data from usb is wrong!");
				return null;
			}
		}
	}
}