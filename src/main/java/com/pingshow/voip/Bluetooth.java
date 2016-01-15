package com.pingshow.voip;

import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothClass.Device;
import android.bluetooth.BluetoothClass.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;


public class Bluetooth {
    static BluetoothAdapter ba;
    static AudioManager am;
    
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
        
    public static boolean isAvailable() {
    	if (ba == null) return false;
        if (!ba.isEnabled())
        	return false;
        Set<BluetoothDevice> devs = ba.getBondedDevices();
        for (final BluetoothDevice dev : devs) {
            BluetoothClass cl = dev.getBluetoothClass();
            if (cl != null && (cl.hasService(Service.RENDER) ||
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