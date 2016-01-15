package com.pingshow.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.pingshow.airecenter.Log;

public class HdmiUtil {

	public static String getHdmiState() {
		String status0 = getStatus("/sys/class/switch/hdmi/state");
		if (status0 != null) {
			return status0;
		} else {
			return "0";
		}
	}

	public static String getHdmiCECState2() {
		String status0 = getStatus("/sys/class/amhdmitx/amhdmitx0/cec_led_state");
		if (status0 != null) {
			return status0;
		} else {
			return "-1";
		}
	}
	
	public static void setHdmiCECState(String param) {
		String cmd = "";
		if (param.equals("switch")) {
			cmd = "0x14";
		} else if (param.equals("on")) {
			cmd = "e 0";
		} else if (param.equals("off")) {
			cmd = "d 0";
		}
		
		String cecCMD = cmd;
		String cec00 = "0x00";
		String cec0F = "0x0B";
		
		setStatus(cecCMD, "/sys/class/amhdmitx/amhdmitx0/cec");
		setStatus(cec00, "/sys/class/amhdmitx/amhdmitx0/cec_config");
		setStatus(cec0F, "/sys/class/amhdmitx/amhdmitx0/cec_config");
		setStatus(cecCMD, "/sys/class/amhdmitx/amhdmitx0/cec");
		setStatus(cec00, "/sys/class/amhdmitx/amhdmitx0/cec_config");
		setStatus(cec0F, "/sys/class/amhdmitx/amhdmitx0/cec_config");
		setStatus(cecCMD, "/sys/class/amhdmitx/amhdmitx0/cec");
		setStatus(cec00, "/sys/class/amhdmitx/amhdmitx0/cec_config");
		setStatus(cec0F, "/sys/class/amhdmitx/amhdmitx0/cec_config");
	}

	public static void setStatus(String status, String patch) {
		BufferedWriter bfwriter;
		File fileDest = new File(patch);
		if (!fileDest.canWrite()) {
			Log.e("HDMIset cannot write:" + status + " " + patch);
			return;
		}
		
		try {
			bfwriter = new BufferedWriter(new FileWriter(patch));
			bfwriter.write(status);
			bfwriter.flush();
			bfwriter.close();
		} catch (FileNotFoundException e) {
			Log.e("HDMIset NotFound ERR " + e.getMessage());
		} catch (IOException e) {
			Log.e("HDMIset IO ERR " + e.getMessage());
		}
	}

	public static String getStatus(String file) {
		BufferedReader bfreader;
		String status = "0";
		try {
			bfreader = new BufferedReader(new FileReader(file));
			status = bfreader.readLine();
			bfreader.close();
		} catch (FileNotFoundException e) {
			Log.e("HDMIget NotFound ERR " + e.getMessage());
		} catch (IOException e) {
			Log.e("HDMIget IO ERR " + e.getMessage());
		}
		Log.d("HDMIset getstatus=" + status);
		return status;
	}
}
