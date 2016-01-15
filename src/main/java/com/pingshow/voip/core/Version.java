
package com.pingshow.voip.core;

import android.os.Build;

import com.pingshow.airecenter.Log;
import com.pingshow.video.capture.hwconf.Hacks;

public class Version {

	private static native boolean nativeHasZrtp();
	private static native boolean nativeHasNeon();
	private static Boolean hasNeon;

	private static final int buildVersion = Integer.parseInt(Build.VERSION.SDK);

	public static final boolean sdkAboveOrEqual(int value) {
		return buildVersion >= value;
	}

	public static final boolean sdkStrictlyBelow(int value) {
		return buildVersion < value;
	}

	public static int sdk() {
		return buildVersion;
	}
	
	public static boolean isSamsung30Pad() {
		return buildVersion==12 && Build.MODEL.toString().toLowerCase().equals("gt-p7310");
	}

	public static boolean isArmv7() {
		try {
			return sdkAboveOrEqual(4)
			&& Build.class.getField("CPU_ABI").get(null).toString().startsWith("armeabi-v7");
		} catch (Throwable e) {}
		return false;
	}
	public static boolean hasNeon(){
		if (hasNeon == null) hasNeon = nativeHasNeon();
		return hasNeon;
	}
	public static boolean isVideoCapable() {
		boolean hasneon=Version.hasNeon();
		return !Version.sdkStrictlyBelow(5) && hasneon && Hacks.hasCamera() && isArmv7();
	}

	private static Boolean sCacheHasZrtp;
	public static boolean hasZrtp(){
		if (sCacheHasZrtp == null) {
			sCacheHasZrtp = nativeHasZrtp();
		}
		return sCacheHasZrtp;
	}

	public static void dumpCapabilities(){
		StringBuilder sb = new StringBuilder(" ==== Capabilities dump ====\n");
		sb.append("isArmv7: ").append(Boolean.toString(isArmv7())).append("\n");
		sb.append("Has neon: ").append(Boolean.toString(hasNeon())).append("\n");
		sb.append("Has ZRTP: ").append(Boolean.toString(hasZrtp())).append("\n");
		Log.i(sb.toString());
	}
}
