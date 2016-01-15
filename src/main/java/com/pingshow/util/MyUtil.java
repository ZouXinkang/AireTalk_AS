package com.pingshow.util;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Debug;
import android.os.Environment;
import android.os.StatFs;
import android.util.Base64;
import android.widget.Toast;

import com.pingshow.airecenter.CommonDialog;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.LocationSettingActivity;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.codec.XTEA;
import com.pingshow.network.NetInfo;

public class MyUtil {
	
	static public boolean canHandleCameraIntent(Context context)
	{
		final Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		final List<ResolveInfo> results = context.getPackageManager().queryIntentActivities(intent, 0);
		return (results.size() > 0);
	}
	
	public static Boolean checkSDCard(Context context)
	{
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
		{
			if(!moreSDAvailableSpare()){
				Intent intent = new Intent();
				intent.setAction(Global.Action_SD_AvailableSpare);
				intent.putExtra("SDAvailable", 0);
				context.sendBroadcast(intent);
			}
			return true;
		}
		else
		{
			Intent intent = new Intent();
			intent.setAction(Global.Action_SD_AvailableSpare);
			intent.putExtra("SDAvailable", -1);
			context.sendBroadcast(intent);
			return false;
		}
    }
	public static boolean moreSDAvailableSpare(){
        File path =Environment.getExternalStorageDirectory(); 
        StatFs statfs=new StatFs(path.getPath()); 
        long blocSize=statfs.getBlockSize(); // byte
        long totalBlocks=statfs.getBlockCount(); 
        long availaBlock=statfs.getAvailableBlocks(); 
        long total = totalBlocks*blocSize; 
        long availale = availaBlock*blocSize;
        int arrMemorySD = (int)((total -availale)/1024); // kb
		return arrMemorySD>500;
	}
	public static boolean checkNetwork(Context context)
	{
		if (!new NetInfo(context).isConnected())
		{
			Intent it=new Intent(context, CommonDialog.class);
			it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_SINGLE_TOP);
	    	
	    	it.putExtra("msgContent", context.getResources().getString(R.string.net_error));
	    	it.putExtra("numItems", 2);
	    	it.putExtra("ItemCaption0",context.getResources(). getString(R.string.cancel));
	    	it.putExtra("ItemResult0", -1);
	    	it.putExtra("ItemCaption1",context.getResources(). getString(R.string.setnet));
	    	it.putExtra("ItemResult1", CommonDialog.SETNET);
	    	context.startActivity(it);
	    	return false;
		}
		return true;
    }
	
	// XTEA encode and decode TCP Protocol
	public static byte [] encryptTCPCmd(String cmd) {
		byte[] retByteArray = null;
		byte[] encoded = cmd.getBytes();
		int data_offset = encoded.length;
		int key[] = new int[4];
		Random r = new Random();
		key[0] = -120958121;
		key[1] = 719178301;
		key[2] = r.nextInt();
		key[3] = -897896317;

		XTEA.encode(encoded, data_offset, key);
		try {
			int len = data_offset + 6;
			retByteArray = new byte[len];
			retByteArray[1] = (byte) (len >> 8);
			retByteArray[0] = (byte) (len & 0xFF);
			for (int i = 0; i < data_offset; i++) {
				retByteArray[2 + i] = encoded[i];
			}
			
			XTEA.writeUnsignedInt(key[2], retByteArray, len-4);
			//Log.d("LEN[2]:"+retByteArray[1]+" "+retByteArray[0]);
		}catch(Exception e) 
		{
		}

		return retByteArray;
	}

	public static String decryptTCPCmd(byte [] cmdByte) {
		String retString = null;
		byte[] retByte = null;
		int lenTot = cmdByte.length;
		int len = (int) (cmdByte[0] & 0xFF | cmdByte[1] << 8);
//		Log.d("decryptTCPCmd.0  " + len + "/" + lenTot);  //2560
		
		int key[] = new int[4];
		if (len <= 6 || len > 2560) {
			return "";
		}
		key[0] = -120958121;
		key[1] = 719178301;
		key[2] = XTEA.readUnsignedInt(cmdByte, len-4);
		key[3] = -897896317;
		XTEA.decode(cmdByte, 2, len - 4, key);
		retByte = new byte[len - 6];
		for (int i = 0; i < len - 6; i++) {
			retByte[i] = cmdByte[i + 2];
		}
		try {
			retString = new String(retByte);
		} catch (Exception e) {
		}

		return retString;
	}
	//tml|alex*** tcp stuck bug
	public static ArrayList<String> decryptTCPCmd2(byte [] cmdByte) {
		final int header = 6, maxPkt = 2560;
		int countE = 0, countOk = 0;
		int key[] = new int[4];
		ArrayList<String> retString = new ArrayList<String>();
		byte[] retByte = null;
		
		int lenTotB = cmdByte.length;  //len of total valid pkt, initially always @ byte.length
		int lenP = (int) (cmdByte[0] & 0xFF | cmdByte[1] << 8);  //len of 1st piece of pkt
		
		while (lenP <= lenTotB) {

			if (lenP <= header || lenP > maxPkt) {  //junk = false
				if (countOk == 0) {
					retString.add("");
					countE++;
				}
				break;
			} else {
				if (countOk > 0) Log.d("decryptTCPCmd2.N[" + (countOk + 1) + "] " + lenP + "/" + lenTotB);  //log N+1 piece
				try {
					key[0] = -120958121;
					key[1] = 719178301;
					key[2] = XTEA.readUnsignedInt(cmdByte, lenP - 4);
					key[3] = -897896317;
					XTEA.decode(cmdByte, 2, lenP - 4, key);
					retByte = new byte[lenP - header];
					for (int i = 0; i < lenP - header; i++) {
						retByte[i] = cmdByte[i + 2];
					}
				
					retString.add(new String(retByte));  //add to queue
					countOk++;
				} catch (Exception e) {  //decode error
					retString.add("");
					countE++;
					Log.e("!fromServer :: decode !@#$");
					break;
				}
			}
			
			//checking if valid 2nd piece exists
			int lenRem = lenTotB - lenP;
			lenTotB = lenRem;  //len of remaining pkt (total valid pkt)
			if (lenRem > header && lenRem <= lenTotB - header) {
				//replace pkt with remaining pkt (pkt - prev piece)
				byte[] tempByte = new byte[lenRem];
				System.arraycopy(cmdByte, lenP, tempByte, 0, lenRem);
				cmdByte = tempByte;
				lenP = (int) (cmdByte[0] & 0xFF | cmdByte[1] << 8);  //len of N+1 piece of pkt
				//normal 1 piece/pkt, 2nd lenP will be junk
			} else {
				break;  //junk
			}
			
		}

		//logging for multi-piece pkts
		if (retString.size() > 1) {
			Log.e("decryptTCPCmd2.N=" + retString.size() + " e" + countE);
		}
		
		return retString;
	}
	
	public static long getSharingTimeout(int relation)
	{
		long timeout = new Date().getTime()/1000;
		switch (relation) {
		case 1:	// one hour
			timeout = timeout+3600;
			break;
		case 2: // two hour
			timeout = timeout+7200;
			break;
		case 3: // one day
			timeout = timeout+86400;
			break;
		case 4: // two day
			timeout = timeout+172800;
			break;
		case 5: // three day
			timeout = timeout+259200;
			break;
		}
		return timeout;
	}
	
	public static void renameFile(String src, String dst)
	{
		File fromFile=new File(src);
		if (!fromFile.exists()) {
			return;
		}
		if (!fromFile.isFile()) {     
			return ;
		}
		if (!fromFile.canRead()) {
			return ;
		}
		File toFile=new File(dst);
		if (!toFile.getParentFile().exists()) {
			toFile.getParentFile().mkdirs();
		}
		if (toFile.exists()) {
			toFile.delete();
		}
		fromFile.renameTo(toFile);
	}
	
	public static void copyFile(File fromFile, File toFile, Boolean rewrite, Context context)
	{
		copyFile(fromFile, toFile, rewrite, context, false);
	}
	
	public static void copyFile(File fromFile, File toFile, Boolean rewrite, Context context, boolean showToast)
	{
		if (!fromFile.exists()) {
			return;
		}
		if (!fromFile.isFile()) {     
	                   return ;
		}
		if (!fromFile.canRead()) {
			return ;
		}
		if (!toFile.getParentFile().exists()) {
			toFile.getParentFile().mkdirs();
		}
		if (toFile.exists() && rewrite) {
			toFile.delete();
		}
		try {
			java.io.FileInputStream fosfrom = new java.io.FileInputStream(fromFile);
			java.io.FileOutputStream fosto = new FileOutputStream(toFile);
			byte bt[] = new byte[1024];
			int c;
			while ((c = fosfrom.read(bt)) > 0) {
				fosto.write(bt, 0, c);
			}
			if (showToast)
				Toast.makeText(context, context.getResources().getString(R.string.copyfile)+toFile, Toast.LENGTH_LONG).show();
			fosto.flush();
			fosfrom.close();
			fosto.close();
		  
			Intent intent = new Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		    Uri uri = Uri.fromFile(new File(toFile.toString()));
		    intent.setData(uri);
		    context.sendBroadcast(intent);	          
		} catch (Exception ex) {
			Log.e(ex.getMessage());           
		}
	}
	
	static public boolean CheckServiceExists(Context context, String ServiceClass)
	{
		boolean mReturn=false;
		
		ActivityManager am=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> mServiceList = am.getRunningServices(80);
		for (ActivityManager.RunningServiceInfo mInfo : mServiceList)
		{
			String aa=mInfo.service.getClassName();
			if (aa.compareTo(ServiceClass)==0)
			{
				if (mInfo.started)
					mReturn=true;
				break;
			}
		}
		return mReturn;
	}
	
	public static boolean checkValidePhoneNumber(String number)
	{
		String [] p={"00000","11111","22222","33333","44444","55555","66666","77777","88888","99999"};
		for (int i=0;i<10;i++)
			if (number.contains(p[i]))
				return false;
		return true;
	}
	
	static public void Sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
		}
	}
	
	static public int getIntValue(String s, int def) {
		int v = def;
		try {
			v = Integer.parseInt(s.substring(s.indexOf("=") + 1));
		} catch (Exception e) {
		}
		return v;
	};

	static public String getStringValue(String s) {
		try {
			return s.substring(s.indexOf("=") + 1);
		} catch (Exception e) {
		}
		return "";
	};
	
	static public String longToIPForServer(long longIp){
        StringBuffer sb = new StringBuffer("");
        
        sb.append(String.valueOf((longIp & 0x000000FF)));
        sb.append(".");
        
        sb.append(String.valueOf((longIp & 0x0000FFFF) >>> 8));
        sb.append(".");
        
        sb.append(String.valueOf((longIp & 0x00FFFFFF) >>> 16));
        sb.append(".");
        
        sb.append(String.valueOf((longIp >>> 24)));
        return sb.toString();
    }
	
	static public long ipToLong(String strIp){
        long[] ip = new long[4];
        int position1 = strIp.indexOf(".");
        int position2 = strIp.indexOf(".", position1 + 1);
        int position3 = strIp.indexOf(".", position2 + 1);

        ip[0] = Long.parseLong(strIp.substring(0, position1));
        ip[1] = Long.parseLong(strIp.substring(position1+1, position2));
        ip[2] = Long.parseLong(strIp.substring(position2+1, position3));
        ip[3] = Long.parseLong(strIp.substring(position3+1));
        return (ip[3] << 24) + (ip[2] << 16) + (ip[1] << 8) + ip[0];
    }
	
	static public String getMD5(byte[] source)
	{
		String s = null;
		final char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
		try
		{
			java.security.MessageDigest md = java.security.MessageDigest.getInstance( "MD5" );
			md.update(source);
			byte tmp[] = md.digest();
			char str[] = new char[16 * 2];
			int k = 0;
			for (int i = 0; i < 16; i++) {
				byte byte0 = tmp[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];                                           
				str[k++] = hexDigits[byte0 & 0xf];
			}
			s = new String(str);
	   }catch(Exception e)
	   {
		   e.printStackTrace();
	   }
	   return s;
	}
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static boolean hasGoogleMap(boolean showToast, Context context)
	{
		boolean bHasGoogleMap=true;
		try {
			Class.forName("com.google.android.maps.MapActivity");
		} catch (ClassNotFoundException e) {
			bHasGoogleMap=false;
			if (showToast) Toast.makeText(context, R.string.nonsupport_googlemap, Toast.LENGTH_LONG).show();
		} catch (NoClassDefFoundError e) {
			bHasGoogleMap=false;
			if (showToast) Toast.makeText(context, R.string.nonsupport_googlemap, Toast.LENGTH_LONG).show();
		}
		return bHasGoogleMap;
	}
	
	//tml|yang*** setCPU
	public static String[] getCPU (boolean log) {
		String[] cpuInfo = new String[4];
		cpuInfo[0] = getStatus("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
		cpuInfo[1] = getStatus("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
		cpuInfo[2] = getStatus("/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq");
		cpuInfo[3] = getStatus("/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq");
		if (log) Log.i("getCPU " + cpuInfo[0] + " " + cpuInfo[1] + " " + cpuInfo[2] + " " + cpuInfo[3]);
		return cpuInfo;
	}
	
	public static void setCPU (boolean revert, String gov, String min, String max) {
		int wantMinCpu = 1608000;
		int wantMaxCpu = 1800000;  //1608000, 1800000, 1992000
		int curMinCpu1 = 0;
//		Log.i("setCPU setting!");
		
		if (revert) {
			setStatus(gov,   // userspace  // performance 
					"/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
			//let android control
//			setStatus(min,   // userspace  // performance 
//					"/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq");
			setStatus(max,   // userspace  // performance 
					"/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq");
			Log.i("setCPU return done! " + gov + " " + min + " " + max);
		} else {
			if (gov != null) {
				setStatus(gov,   // userspace  // performance 
						"/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
			} else {
				setStatus("hotplug",   // userspace, hotplug  // performance 
//				setStatus("performance",   // userspace, hotplug  // performance 
						"/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
			}
			if (max != null) {
				setStatus(max, // 1800 M
						"/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq");
			} else {
				setStatus(Integer.toString(wantMaxCpu), // 1800 M
						"/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq");
			}
			//let android control
//			String curMinCpu0 = getStatus("/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq");
//			try {
//				curMinCpu1 = Integer.parseInt(curMinCpu0);
//			} catch (Exception e) {
//				curMinCpu1 = wantMaxCpu;
//			}
			if (curMinCpu1 < wantMinCpu) {
				//let android control
//				setStatus(Integer.toString(wantMinCpu), // 1600 M
//						"/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq");
				Log.i("setCPU setting done!");
			} else {
				Log.i("setCPU setting done (only max)!");
			}
		}
	}
	//***tml

	public static boolean setStatus(String status, String file) {
		boolean setok = true;
		BufferedWriter bfwriter;
		File fileDest = new File(file);
		if (!fileDest.canWrite()) {
			Log.e("Utilset cannot write:" + status + " " + file);
			return false;
		}
		
		try {
			bfwriter = new BufferedWriter(new FileWriter(file));
			bfwriter.write(status);
			bfwriter.flush();
			bfwriter.close();
		} catch (FileNotFoundException e) {
			Log.e("Utilset NotFound ERR " + e.getMessage() + " <" + status);
			setok = false;
		} catch (IOException e) {
			Log.e("Utilset IO ERR " + e.getMessage() + " <" + status);
			setok = false;
		}
		return setok;
	}
	
	public static boolean setStatus2(String status, String file) {
		boolean setok = true;
		File fileDest = new File(file);
		if (!fileDest.canWrite()) {
			Log.e("Utilset2 cannot write:" + status + " " + file);
			return false;
		}
		
		String[] commandLine = {"sh", "-c", "echo " + status + " > " + file};
		try {
			StringBuffer output = new StringBuffer();
			Process proc = Runtime.getRuntime().exec(commandLine);
			BufferedReader breader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = "";
			while ((line = breader.readLine()) != null) {
				output.append(line + "\n");
				proc.waitFor();
			}
			breader.close();
		} catch (IOException e) {
			Log.e("Utilset2 IO ERR1 " + e.getMessage() + " <" + status);
			setok = false;
		} catch (InterruptedException e) {
			Log.e("Utilset2 IO ERR2 " + e.getMessage() + " <" + status);
			setok = false;
		}
		
		return setok;
	}
	
	public static String getStatus(String file) {
		BufferedReader bfreader;
		String status = "0";
		try {
			bfreader = new BufferedReader(new FileReader(file));
			status = bfreader.readLine();
			bfreader.close();
		} catch (FileNotFoundException e) {
			Log.e("Utilget NotFound ERR " + e.getMessage());
		} catch (IOException e) {
			Log.e("Utilget IO ERR " + e.getMessage());
		}
		return status;
	}
	
	public static String getDate(int format) {
		String date = "";
		SimpleDateFormat dateFormat;
		Calendar cal = Calendar.getInstance();
		if (format == 1) {
			dateFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.US);
			date = dateFormat.format(cal.getTime());
		} else if (format == 2) {
			dateFormat = new SimpleDateFormat("HH", Locale.US);
			date = dateFormat.format(cal.getTime());
		} else if (format == 3) {
			dateFormat = new SimpleDateFormat("HH:mm yyyy-MM-dd", Locale.US);
			date = dateFormat.format(cal.getTime());
		}
		return date;
	}
	
	public static String testFirmwareRW () {
		String msg = "";
		String[] files = {"/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor",
				"/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq",
				"/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq",
				"/sys/class/amhdmitx/amhdmitx0/phone_led_state",
				"/sys/class/amhdmitx/amhdmitx0/codec_mute_state",
				"/sys/class/amhdmitx/amhdmitx0/cec_config",
				"/sys/class/amhdmitx/amhdmitx0/cec",
				"/sys/class/amhdmitx/amhdmitx0/cec_led_state",
				"/sys/class/switch/hdmi/state",
				"/sys/devices/i2c-4/4-001b/mic_volume",
				"/system/etc/mixer_paths.xml"};
		for (int i = 0; i < files.length; i++) {
			File fileDest = new File(files[i]);
			if (!fileDest.exists()) {
				String msg1 = "DOES NOT EXIST!  " + files[i];
				Log.e("Firmware: " + msg1);
				msg = msg + "\n" + msg1;
			} else {
				boolean read = fileDest.canRead();
				boolean write = fileDest.canWrite();
				String msg2 = "RW " + read + " " + write + "  " + files[i];
				Log.i("Firmware: " + msg2);
				msg = msg + "\n" + msg2;
			}
		}
		return msg;
	}
	//tml*** cache
	public static void deleteCache(Context context) {
	    try {
	        File dir = context.getCacheDir();
//	        Log.i("cachedir=" + dir.getAbsolutePath());
	        if (dir != null && dir.isDirectory()) {
	            deleteDir(dir);
	        }
	    } catch (Exception e) {}
	}
	//tml*** memory
	public static boolean deleteDir(File dir) {
	    if (dir != null && dir.isDirectory()) {
	        String[] dirFiles = dir.list();
	        for (int i = 0; i < dirFiles.length; i++) {
	        	File oneFile = new File(dir, dirFiles[i]);
//	            boolean ok = deleteDir(childFile);
	        	Log.i("cachefile=" + dirFiles[i] + " " + oneFile.length());
//	            if (!ok) return false;
	        }
	    }
//	    return dir.delete();
	    return true;
	}
	//tml*** memory
	public static int[] getMemoryRAM(Context context, int params) {
		int[] memory = new int[3];
		ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo memInfo = new MemoryInfo();
		
		if (params < 1) {
			for (int m = 0; memory.length < 0; m++)
				memory[m] = 0;
			return memory;
		}
		
		actManager.getMemoryInfo(memInfo);
		if (params > 0) memory[0] = (int) (memInfo.availMem / 1048576L);
		if (params > 1) memory[1] = (int) (memInfo.totalMem / 1048576L);
		else memory[1] = 0;
		if (params > 2) memory[2] = (int) (memInfo.threshold / 1048576L);
		else memory[2] = 0;
		
		Log.i("memram=" + memory[0] + "/" + memory[1] + "/" + memory[2]);
		return memory;
	}
	//tml*** memory
	@SuppressLint("NewApi")
	public static int[] getMemoryHEAP(Context context, int pid, int params) {
		int[]memory = new int[13];
		int[] myPID = new int[1];
		ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		
		if (pid == 0 || params < 1) return null;
		myPID[0] = pid;
		
		Debug.MemoryInfo[] memoryInfoArray = actManager.getProcessMemoryInfo(myPID);
		if (params > 0) memory[0] = memoryInfoArray[0].dalvikPrivateDirty / 1024;
		if (params > 1) memory[1] = memoryInfoArray[0].dalvikPss / 1024;
		else memory[1] = 0;
		if (params > 2) memory[2] = memoryInfoArray[0].nativePrivateDirty / 1024;
		else memory[2] = 0;
		if (params > 3) memory[3] = memoryInfoArray[0].nativePss / 1024;
		else memory[3] = 0;
		if (params > 4) memory[4] = memoryInfoArray[0].otherPrivateDirty / 1024;
		else memory[4] = 0;
		if (params > 5) memory[5] = memoryInfoArray[0].otherPss / 1024;
		else memory[5] = 0;
		if (params > 6) memory[6] = memoryInfoArray[0].getTotalPrivateDirty() / 1024;
		else memory[6] = 0;
		if (params > 7) memory[7] = memoryInfoArray[0].getTotalPrivateClean() / 1024;
		else memory[7] = 0;
		if (params > 8) memory[8] = memoryInfoArray[0].getTotalPss() / 1024;
		else memory[8] = 0;
		
		Runtime runtime = Runtime.getRuntime();
		if (params > 9) memory[9] = (int) (runtime.totalMemory() / 1048576L);
		else memory[9] = 0;
		if (params > 10) memory[10] = (int) (runtime.maxMemory() / 1048576L);
		else memory[10] = 0;
		if (params > 11) memory[11] = actManager.getMemoryClass();
		else memory[11] = 0;
		if (params > 12) memory[12] = actManager.getLargeMemoryClass();
		else memory[12] = 0;
		
		Log.i("memheap=" + memory[0] + "/" + memory[1] + "," + memory[2] + "/"
				+ memory[3] + "," + memory[4] + "/" + memory[5] + "  "
				+ memory[6] + "/" + memory[7] + "/" + memory[8] + "  "
				+ memory[9] + "/" + memory[10] + "/" + memory[11] + "/" + memory[12]);
		return memory;
	}
	//tml*** memory
	public static int getAiretalkPID(Context context) {
		ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> runningAppProcesses = actManager.getRunningAppProcesses();

		int myPID = 0;
		for (RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
			if (runningAppProcessInfo.processName.contains("com.pingshow")) {
				Log.i("airetalk=" + runningAppProcessInfo.pid + "=" + runningAppProcessInfo.processName);
				myPID = runningAppProcessInfo.pid;
				break;
			}
		}
		return myPID;
	}
	//tml*** check china
	public static boolean isISO_China(Context context, MyPreference pref, String defaultiso) {
		MyPreference mPref;
		String iso = "cn";
		if (defaultiso != null) iso = defaultiso;
		if (pref == null)  {
			if (context != null) {
				mPref = new MyPreference(context);
				iso = mPref.read("iso", iso);
			} else {
				Log.e("isISO_China !@#$ both pref/context error, then cn");
			}
		} else {
			iso = pref.read("iso", iso);
		}
		boolean isChina = (iso.equals("cn") || iso.equals("hk"));
		return isChina;
	}
	public static String setBase64(String str){
		String strBase64 = new String(Base64.encode(str.getBytes(),
				Base64.DEFAULT));
//		for (int i = 0; i < 3; i++) {
//			strBase64 = Base64.encodeToString(strBase64.getBytes(),
//					Base64.DEFAULT);
//			i++;
//		}
		 String finalBase64Str = strBase64.replace("=", "");
		 return finalBase64Str;
		 
	}
	public static String getBase64(String str){
		String getBase64 = new String(Base64.decode(str.getBytes(),Base64.DEFAULT));
//		for (int i = 0; i < 3; i++) {
//			getBase64 = new String(Base64.decode(getBase64.getBytes(),Base64.DEFAULT));
//			i++;
//		}
		return getBase64;
		 
	}

}
