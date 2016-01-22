
package com.pingshow.video.capture.hwconf;

import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Build;

import com.pingshow.amper.Log;
import com.pingshow.voip.core.Version;

public final class Hacks {

	private Hacks() {}


	public static boolean isGalaxySOrTabWithFrontCamera() {
		return isGalaxySOrTab() && !isGalaxySOrTabWithoutFrontCamera();
	}
	private static boolean isGalaxySOrTabWithoutFrontCamera() {
		return isSC02B() || isSGHI896();
	}


	public static boolean isGalaxySOrTab() {
		return isGalaxyS() || isGalaxyTab();
	}

	public static boolean isGalaxyTab() {
		return isGTP1000();
	}
	private static boolean isGalaxyS() {
		return isGT9000() || isSC02B() || isSGHI896() || isSPHD700();
	}
	
	public static final boolean hasTwoCamerasRear0Front1() {
		return isLGP970() || isSPHD700() || isADR6400() || isGTI997();
	}
	
	// HTC
	private static final boolean isADR6400() {
		return Build.MODEL.startsWith("ADR6400") || Build.DEVICE.startsWith("ADR6400");
	} // HTC Thunderbolt

	// Galaxy S variants
	private static final boolean isSPHD700() {return Build.DEVICE.startsWith("SPH-D700");} // Epic 
	private static boolean isSGHI896() {return Build.DEVICE.startsWith("SGH-I896");} // Captivate
	private static boolean isGT9000() {return Build.DEVICE.startsWith("GT-I9000");} // Galaxy S
	private static boolean isGTI9100() {return Build.DEVICE.startsWith("GT-I9100");} // Galaxy S II
	private static boolean isSC02B() {return Build.DEVICE.startsWith("SC-02B");} // Docomo
	private static boolean isGTP1000() {return Build.DEVICE.startsWith("GT-P1000");} // Tab
	private static boolean isGTI997() {return Build.DEVICE.startsWith("SGH-I997");} // I997

	// LG with two cameras
	private static final boolean isLGP970() {return Build.DEVICE.startsWith("LG-P970");}

/*	private static final boolean log(final String msg) {
		Log.d(msg);
		return true;
	}*/

	/* Not working as now
	 * Calling from Galaxy S to PC is "usable" even with no hack; other side is not even with this one*/
	public static void galaxySSwitchToCallStreamUnMuteLowerVolume(AudioManager am) {
		// Switch to call audio channel (Galaxy S)
		am.setSpeakerphoneOn(false);
		sleep(200);

		// Lower volume
		am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 1, 0);

		// Another way to select call channel
		am.setMode(AudioManager.MODE_NORMAL);
		sleep(200);

		// Mic is muted if not doing this
		am.setMicrophoneMute(true);
		sleep(200);
		am.setMicrophoneMute(false);
		sleep(200);
	}

	public static final void sleep(int time) {
		try  {
			Thread.sleep(time);
		} catch(InterruptedException ie){}
	}

	public static boolean needSoftvolume() {
		return isGalaxySOrTab();
	}

	public static boolean needRoutingAPI() {
		return Version.sdkStrictlyBelow(5);
	}

	public static boolean needGalaxySAudioHack() {
		return isGalaxySOrTab() && !isSC02B();
	}

	public static boolean needPausingCallForSpeakers() {
//		return false;
		return isGalaxySOrTab() && !isSC02B();
	}

	public static boolean hasCamera() {
		if (Version.sdkAboveOrEqual(9)) {
			int nb = 0;
			try {
				nb = (Integer) Camera.class.getMethod("getNumberOfCameras", (Class[])null).invoke(null);
			} catch (Exception e) {
				Log.e("Error getting number of cameras");
			}
			return nb > 0;
		}

		Log.i("assume there's a camera.");
		return true;
	}

	public static boolean hasBuiltInEchoCanceller() {
		return isGTI9100();
	}
}
