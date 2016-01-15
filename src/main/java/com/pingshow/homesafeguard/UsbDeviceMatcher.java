package com.pingshow.homesafeguard;
//li*** Home safeguard
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pingshow.airecenter.Log;

import android.content.Context;

public class UsbDeviceMatcher {
	private Map<Integer, UsbDevice> matchDevices;
	private UsbDeviceDBHelper dbHelper;
	private static UsbDeviceMatcher mUsbDeviceMatcher;

	private UsbDeviceMatcher(Context context) {
		matchDevices = new HashMap<Integer, UsbDevice>();
		dbHelper = new UsbDeviceDBHelper(context);
		refresh();
	}
	public static synchronized UsbDeviceMatcher getInstance(Context context){
		if(mUsbDeviceMatcher == null){
			mUsbDeviceMatcher = new UsbDeviceMatcher(context);
		}
		return mUsbDeviceMatcher;
	}

	private void refresh() {
		List<UsbDevice> devices = dbHelper.getAllUsbDevice();
		refresh(devices);
	}

	public void refresh(List<UsbDevice> devices) {
		Log.d("clear all devices");
		matchDevices.clear();
		for (int i = 0; i < devices.size(); i++) {
			UsbDevice device = devices.get(i);
			matchDevices.put(device.getAddress(), device);
			Log.d("add usb device " + device.toString());
		}
	}

	public void addUsbDevices(List<UsbDevice> devices) {
		for (int i = 0; i < devices.size(); i++) {
			UsbDevice device = devices.get(i);
			addUsbDevice(device);
		}
	}
	public void deleteUsbDevice(int address){
		matchDevices.remove(address);
		dbHelper.deleteDevice(address);
		Log.d("delete usb device address: " + address);
	}
	public boolean updateUsbDevice(UsbDevice device){
		if(matchDevices.containsKey(device.getAddress())){
			matchDevices.put(device.getAddress(), device);
			Log.d("update usb device : " + device.toString());
			dbHelper.updateDevice(device);
			return true;
		}
		return false;
	}
	public boolean addUsbDevice(UsbDevice device) {
		if(!matchDevices.containsKey(device.getAddress())){
			matchDevices.put(device.getAddress(), device);
			dbHelper.addUsbDevice(device);
			Log.d("add usb device " + device.toString());
			return true;
		}
		return false;
	}

	public UsbDevice getUsbDeviceByData(UsbData data) {
		UsbDevice device = matchDevices.get(data.getAddress());
		if(device != null){
			device.setData(data);
		}
		return device;
	}
	public List<UsbDevice> getAllUsbDevice(){
		if(dbHelper != null){
			List<UsbDevice> allUsbDevice = dbHelper.getAllUsbDevice();
			refresh(allUsbDevice);
			return allUsbDevice;
		}
		return null;
	}
}
