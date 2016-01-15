package com.pingshow.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.pingshow.airecenter.Log;

public class LedSpeakerUtil {

	public static void setLedOn() {
		//Log.e("setLedOn!!!");
		setStatus("1",
				"/sys/class/amhdmitx/amhdmitx0/phone_led_state");
		getStatus("phone_led_state : ",
				"/sys/class/amhdmitx/amhdmitx0/phone_led_state");
	}
	
	public static void setLedOff() {
		//Log.e("setLedOff!!!");
		setStatus("0",
				"/sys/class/amhdmitx/amhdmitx0/phone_led_state");
		getStatus("phone_led_state : ",
				"/sys/class/amhdmitx/amhdmitx0/phone_led_state");
	}
	
	public static void setSpeakerOn() {
		Log.d("setSpeakerOn!!!");
		setStatus("0",
			"/sys/class/amhdmitx/amhdmitx0/codec_mute_state");
		getStatus("codec_mute_state : ",
			"/sys/class/amhdmitx/amhdmitx0/codec_mute_state");
	}
	
	public static void setSpeakerOff() {
		Log.d("setSpeakerOff!!!");
		setStatus("1",
				"/sys/class/amhdmitx/amhdmitx0/codec_mute_state");
		getStatus("codec_mute_state : ",
				"/sys/class/amhdmitx/amhdmitx0/codec_mute_state");
	}
	
	//tml*** speaker
	public static String getSpeakerStatus() {
		String status = getStatus("codec_mute_state : ",
				"/sys/class/amhdmitx/amhdmitx0/codec_mute_state");
		Log.d("getSpeakerStatus=" + status);
		return status;
	}

	public static void setStatus(String status, String patch) {
		BufferedWriter bfwriter;
		File fileDest = new File(patch);
		if (!fileDest.canWrite()) {
			Log.e("LedSpeakerset cannot write:" + status + " " + patch);
			return;
		}
		try {
			bfwriter = new BufferedWriter(new FileWriter(patch));
			bfwriter.write(status);
			bfwriter.flush();
			bfwriter.close();
		} catch (FileNotFoundException e) {
			Log.e("LedSpeakerset NotFound ERR " + e.getMessage());
		} catch (IOException e) {
			Log.e("LedSpeakerset IO ERR " + e.getMessage());
		}
	}

	public static String getStatus(String patch, String file) {
		BufferedReader bfreader;
		String status = "0";
		try {
			bfreader = new BufferedReader(new FileReader(file));
			status = bfreader.readLine();
			//Log.i("getStatus===="+ status);
			bfreader.close();
		} catch (FileNotFoundException e) {
			Log.e("LedSpeakerget NotFound ERR " + e.getMessage());
		} catch (IOException e) {
			Log.e("LedSpeakerget IO ERR " + e.getMessage());
		}
		return status;
	}

}
