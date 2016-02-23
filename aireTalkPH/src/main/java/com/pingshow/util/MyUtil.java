package com.pingshow.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.util.Base64;
import android.widget.Toast;

import com.pingshow.amper.CommonDialog;
import com.pingshow.amper.Global;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.R;
import com.pingshow.codec.XTEA;
import com.pingshow.network.NetInfo;

public class MyUtil {
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
	    	it.putExtra("ItemCaption1", context.getResources().getString(R.string.setnet));
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
		int len = (int) (cmdByte[0]&0xFF | cmdByte[1]<<8);
		
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

			if (lenP <= header || lenP > lenTotB) {  //junk?
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
//				if (lenP <= header || lenP > lenRem) {  //junk?
//					break;
//				} else {
//					Log.d("decryptTCPCmd2.n  " + (countOk + 1) + "/" + lenP + "/" + lenTotB);  //log N+1 piece
//				}
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
	
	public static void copyFile(File fromFile, File toFile,Boolean rewrite ,Context context)
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
			fosto.flush();
			fosfrom.close();
			fosto.close();
		  
			Intent intent = new Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		    Uri uri = Uri.fromFile(new File(toFile.toString()));
		    intent.setData(uri);
		    context.sendBroadcast(intent);	          
		} catch (Exception ex) {
			Log.e("copyFile1 " + ex.getMessage());
		}
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
	
	public static void copyFile(String src, String dst, Boolean rewrite)
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
			
			fosto.flush();
			fosfrom.close();
			fosto.close();         
		} catch (Exception ex) {
			Log.e("copyFile2 " + ex.getMessage());
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
//		String [] p={"00000","11111","22222","33333","44444","55555","66666","77777","88888","99999"};
//		for (int i=0;i<10;i++)
//			if (number.contains(p[i]))
//				return false;
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

	public static boolean hasGoogleMap(boolean showToast, Context context, String from)
	{
		boolean bHasGoogleMap = true;
		try {
			Log.w("checkmap -" + from + " ? com.google.android.maps");
			Class.forName("com.google.android.maps.MapActivity");
		} catch (ClassNotFoundException e) {
			bHasGoogleMap = false;
			if (showToast) Toast.makeText(context, R.string.nonsupport_googlemap, Toast.LENGTH_LONG).show();
		} catch (NoClassDefFoundError e) {
			bHasGoogleMap = false;
			if (showToast) Toast.makeText(context, R.string.nonsupport_googlemap, Toast.LENGTH_LONG).show();
		} catch (Error e) {
			bHasGoogleMap = false;
		}
		Log.w("hasGoogleMap ? com.google.android.maps " + bHasGoogleMap);
		return bHasGoogleMap;
	}
	
	static public boolean isAppInstalled(Context context, String packageName) {
	    PackageManager pm = context.getPackageManager();
	    boolean installed = false;
	    try {
	    	pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
	    	installed = true;
	    } catch (PackageManager.NameNotFoundException e) {
	    	installed = false;
	    }
	    return installed;
	}
	//tml*** airplane mode
	@SuppressWarnings("deprecation")
	public static boolean isAirplaneModeOn(Context context) {
		try {
		    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
		    	return Settings.System.getInt(context.getContentResolver(), 
		    			Settings.System.AIRPLANE_MODE_ON, 0) != 0;          
		    } else {
		    	return Settings.Global.getInt(context.getContentResolver(), 
		    			Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
		    }
		} catch (NoClassDefFoundError e) {
		} catch (Error e) {
		}
		return false;
	}
	
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
			Log.e("Utilset NotFound !@#$ " + e.getMessage() + " <" + status);
			setok = false;
		} catch (IOException e) {
			Log.e("Utilset IO !@#$ " + e.getMessage() + " <" + status);
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
			Log.e("Utilset2 IO1 !@#$ " + e.getMessage() + " <" + status);
			setok = false;
		} catch (InterruptedException e) {
			Log.e("Utilset2 IO2 !@#$ " + e.getMessage() + " <" + status);
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
			Log.e("Utilget NotFound !@#$ " + e.getMessage());
		} catch (IOException e) {
			Log.e("Utilget IO !@#$ " + e.getMessage());
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
		} else {
		}
		return date;
	}
	//tml*** useful timers
	public static boolean greaterThanTimer_sysMillis(Context context, String name, int limit, boolean log) {
		MyPreference mPref = new MyPreference(context);
		long now = System.currentTimeMillis();
		long last = mPref.readLong(name, now);
		long elapsed = now - last;
		if (!(elapsed < limit)) {
			mPref.writeLong(name, now);
			if (log) Log.w("#greaterThanTimer (" + name + ") HIT! " + elapsed + ">" + limit);
			return true;
		} else {
			mPref.writeLong(name, last);
			if (log) Log.w("#greaterThanTimer (" + name + ") " + elapsed + "<" + limit);
			return false;
		}
	}
	//tml*** useful timers
	public static boolean lessThanTimer_sysMillis(Context context, String name, int limit, boolean log) {
		MyPreference mPref = new MyPreference(context);
		long now = System.currentTimeMillis();
//		long last = mPref.readLong(name, now);
		long last = mPref.readLong(name, 0);
		long elapsed = now - last;
		if ((elapsed < limit)) {
			mPref.writeLong(name, now);
			if (log) Log.w("#lessThanTimer (" + name + ") HIT! " + elapsed + "<" + limit);
			return true;
		} else {
			mPref.writeLong(name, now);
			if (log) Log.w("#lessThanTimer (" + name + ") " + elapsed + ">" + limit);
			return false;
		}
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
	//base64加密
	public static String setBase64(String str){
		String strBase64 = new String(Base64.encode(str.getBytes(),
				Base64.DEFAULT));
//		for (int i = 0; i < 3; i++) {
//			strBase64 = Base64.encodeToString(strBase64.getBytes(),
//					Base64.DEFAULT);
//		}
		String finalBase64Str = strBase64.replace("=", "");
		return finalBase64Str;

	}
	public static String getBase64(String str){
		String getBase64 = new String(Base64.decode(str.getBytes(), Base64.DEFAULT));
//		for (int i = 0; i < 3; i++) {
//			getBase64 = new String(Base64.decode(getBase64.getBytes(),Base64.DEFAULT));
//		}
		return getBase64;

	}

	//bree：list去重
	public static List<Map<String, Object>> sigleList(List<Map<String, Object>> list){
		List<Map<String, Object>> sigleList=new ArrayList<Map<String,Object>>();
		List<String> idxList=new ArrayList<String>();
		for (int i = 0; i < list.size(); i++) {
			if (!idxList.contains((String)list.get(i).get("idx"))) {
				idxList.add((String)list.get(i).get("idx"));
				sigleList.add(list.get(i));
			}
		}
		return sigleList;
	}

}
