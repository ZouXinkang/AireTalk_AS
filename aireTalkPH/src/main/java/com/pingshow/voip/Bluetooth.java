package com.pingshow.voip;

import java.util.Set;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothClass.Device;
import android.bluetooth.BluetoothClass.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.media.AudioManager;


public class Bluetooth {
    static BluetoothAdapter ba;
    static AudioManager am;
    static  BluetoothSocket bs;
    public static void init() {
    	if (ba == null) {
        	try{
	            ba = BluetoothAdapter.getDefaultAdapter();
	            am = (AudioManager) AireVenus.instance().getSystemService(
	            		Context.AUDIO_SERVICE);
        	}catch(Exception e){}
        }
    }
    
	public static void enable(boolean mode) {
    	if (am==null) return;
    	if (mode)
    		am.startBluetoothSco();
        else
        	am.stopBluetoothSco();
    }
        
    @SuppressLint("NewApi") @SuppressWarnings("static-access")
	public static boolean isAvailable() {
    	if (ba == null) return false;
        if (!ba.isEnabled())
        	return false;
        Set<BluetoothDevice> devs = ba.getBondedDevices();
        //xwf  判断蓝牙是否连接设备
        int stateConnected = BluetoothProfile.STATE_CONNECTED;
        int profileConnectionState = ba.getProfileConnectionState(stateConnected);
        for (final BluetoothDevice dev : devs) {
            BluetoothClass cl = dev.getBluetoothClass();
            if (cl != null && profileConnectionState ==ba.STATE_CONNECTED  && (cl.hasService(Service.RENDER) ||
                            cl.getDeviceClass() == Device.AUDIO_VIDEO_HANDSFREE ||
                            cl.getDeviceClass() == Device.AUDIO_VIDEO_CAR_AUDIO ||
                            cl.getDeviceClass() == Device.AUDIO_VIDEO_WEARABLE_HEADSET))
            	return true;
        }
        return false;
    }
    
	public static boolean isSupported() {
    	if (am==null) return false;
    	return am.isBluetoothScoAvailableOffCall();
    }
}