package com.pingshow.airecenter.register;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Random;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings.Secure;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.ShoppingActivity;
import com.pingshow.airecenter.SplashScreen;
import com.pingshow.network.MyNet;
import com.pingshow.util.MyUtil;

public class EnterWithoutRegister {

	private Context mContext;
	private MyPreference mPref;
	
	private String username = "";
	private String password = "";
//	private String email = "guo.alec@gmail.com";
	public static String email = "guo.alec@gmail.com";  //tml*** login fix
	public static String emailshort = "guo.alec";
	private String DeviceID = "";
	private String SubscribeId;
	private boolean done=false;
	
	public EnterWithoutRegister(Context context, MyPreference pref)
	{
		mContext=context;
		mPref=pref;
    	
    	DeviceID = Secure.getString(mContext.getContentResolver(), Secure.ANDROID_ID);
    	Log.d("EWG DeviceID="+DeviceID);
		
		SubscribeId = "0000000";
		try{
//			username=getMacAddress().replace(":", "");  //WLAN
			//tml|mj|wjx*** new mac
//			username = getLanMacAddress().replace(":", "");  //LAN
			WifiManager wifiMgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
			username = wifiMgr.getConnectionInfo().getMacAddress();
			
			if (username == null) {
				Log.e("!@#$ MAC CREATION !@#$");
				SplashScreen.errorCode = 1;
				return;
			} else {
				username = username.replace(":", "").toLowerCase();
			}
			
			String manuf = Build.MANUFACTURER.toLowerCase().trim();
			String model = Build.MODEL.toLowerCase().trim();
			Log.e("EWG1 manuf=" + manuf + " model=" + model);

			boolean isEnglish1 = true;
			boolean isEnglish2 = true;
			if (manuf != null) {
				for (char c : manuf.toCharArray()) {
					if (Character.UnicodeBlock.of(c) != Character.UnicodeBlock.BASIC_LATIN) {
						isEnglish1 = false;
					    break;
					}
				}
				if (!isEnglish1) {
					manuf = "1";
				}
				
				if (isEnglish1) manuf = cleanMAC(manuf);
				if (manuf.length() > 0) {
					manuf = manuf.substring(0, 1);
				} else if (manuf.equals("")) {
					manuf = "0";
				}
			} else {
				manuf = "0";
			}
			
			if (model != null) {
				for (char c : model.toCharArray()) {
					if (Character.UnicodeBlock.of(c) != Character.UnicodeBlock.BASIC_LATIN) {
						isEnglish2 = false;
					    break;
					}
				}
				if (!isEnglish2) {
					model = "987";
				}
				
				if (isEnglish2) model = cleanMAC(model);
				if (model.length() > 3) {
					model = model.substring(model.length() - 3, model.length());
				} else if (model.equals("")) {
					model = "000";
				}
			} else {
				model = "000";
			}
			Log.e("EWG2 manuf=" + manuf + isEnglish1 + " model=" + model + isEnglish2);
			
//			if (username.equals("null")) {
//				username = getMacAddress().replace(":", "");  //adapt to non-lan devices
//				Log.e("tml MAC = WLAN");
//			} else {
//				Log.e("tml MAC = LAN");
//			}
			
			String macprefix1 = manuf + model;
			mPref.write("macPrefix1", macprefix1);  //mac-prefix, before "ac"
			Log.e("EWG3 macprefix1=" + macprefix1);
			username = macprefix1 + username;
//			mPref.write("myMacAddress", username);
			//***tml
//			password=username.substring(4, 8);
			password = username.substring(username.length() - 8, username.length());  //tml|sw*** passwords/
			username = "ac" + username;
		} catch (Exception e) {
//			username="airecenter_dev";
			username="ac_exception";
			password="1111";
			SplashScreen.errorCode = 2;
			Log.e("!@#$ ACC CREATION !@#$");
			return;
		}
		
		if (DeviceID==null)
			DeviceID="";

		Log.d("EWG0 username:"+username);
		Log.d("EWG0 password:"+password);
		
		new Thread(parseServerIP).start();
	}
	
	//tml|mj|wjx*** new mac
	public String isMACok () {
		if (username == null) {
			return "null";
		} else {
			return username;
		}
	}
	
	public boolean isDone()
	{
		return done;
	}
	
	Runnable parseServerIP=new Runnable()
	{
		public void run()
		{
			try {
				String domainName = "php.xingfafa.com.cn";
				String iso = mPref.read("iso", "cn");
				if (!MyUtil.isISO_China(mContext, mPref, null))
					domainName = "php.airetalk.org";
				AireJupiter.myPhpServer = InetAddress.getByName(domainName).getHostAddress();
				Log.d("myPhpServer=" + AireJupiter.myPhpServer + " (" + iso + ")");
			} catch (UnknownHostException e) {
				AireJupiter.myPhpServer = AireJupiter.myPhpServer_default;
			}
			
			new Thread(mLoginTask).start();
		}
	};
	
	Runnable mLoginTask = new Runnable() {
		public void run() {
			
			try{
				int count=0;
				String Return="";
				do{
					MyNet net = new MyNet(mContext);
					String user1 = URLEncoder.encode(username,"UTF-8");
					String pw1 = URLEncoder.encode(password,"UTF-8");
					Return= net.doPostHttps("login_aire.php",
							"id=" + user1 + "&password=" + pw1, null);
					
					if (Return.length()!=0 && !Return.startsWith("Error"))//no network
						break;
					count++;
					MyUtil.Sleep(1500);
				}while(count<3);
				
				Return=Return.toLowerCase();
				
				if (Return.length() == 0) {
					SplashScreen.errorCode = 3;
					Return="fail,nonetwork";
				}
				if (Return.startsWith("ok,"))
				{
					mPref.write("AireRegistered", true);
					mPref.write("myPhoneNumber", username);
					mPref.write("password", password);
					mPref.write("myIMEI", (DeviceID==null)?"":DeviceID);
					mPref.write("SubscribeId", (SubscribeId==null)?"":SubscribeId);
					mPref.write("firstEnter", true);
					
					String []itms=Return.split(",");
					if (itms.length>2)
					{
						int myIdx=0;
						try{
							myIdx=Integer.parseInt(itms[2]);
							String hexIdx=Integer.toHexString(myIdx);
							if (myIdx>0)
							{
								mPref.write("myID", hexIdx);
							}
						}catch(Exception e){
						}
					}
					
					done=true;
				}
				else if (Return.startsWith("fail,nonmember"))
				{
					Log.d("nonmember");
					new Thread(mRegisterTask).start();
				}
				else
				{
					SplashScreen.errorCode = 4;
				}
			}catch(Exception e){
				SplashScreen.errorCode = 5;
			}
		}
	};
	
	Runnable mRegisterTask = new Runnable() {
        public void run() {
			
        	Random random=new Random();
    		int pcode=Math.abs(random.nextInt()%9000)+1000;
    		
			try{
				int count=0;
				String Return="";
				do {
					MyNet net = new MyNet(mContext);
					String user2 = URLEncoder.encode(username,"UTF-8");
					String pw2 = URLEncoder.encode(password, "UTF-8");
					String email2 = URLEncoder.encode(email, "UTF-8");
					int code2 = pcode;
					String nickname2 = URLEncoder.encode(email.substring(0,email.indexOf('@')),"UTF-8");
					String imei2 = URLEncoder.encode(DeviceID, "UTF-8");
					Return=net.doPostHttps("preregister_aire.php",
							"id=" + user2
							+"&password="+ pw2
							+"&email=" + email2
							+"&eula=1"
							+"&pcode=" + code2
							+"&sms=0"
							+"&nickname=" + nickname2
							+"&imei=" + imei2
							+"&acct=0",null);
					
					if (Return.length()!=0 && !Return.startsWith("Error"))//no network
						break;
					MyUtil.Sleep(1500);
				}while(count++<3);
				
				Log.d("mRegisterTask... done");
				
				if (Return.startsWith("ok,by_email"))
				{
					(new Thread(doActualRegister)).start();
				}
				else
				{
					SplashScreen.errorCode = 6;
				}
				
			}catch(Exception e){
				SplashScreen.errorCode = 7;
			}
        }
    };
    
    
    Runnable doActualRegister=new Runnable()
	{
		@Override
		public void run() {
			MyNet net = new MyNet(mContext);
			String Return="";
			int count=0;
			do{
				try{
					String id3 = URLEncoder.encode(username,"UTF-8");
					Return=net.doPostHttps("register_aire.php", "id="+id3, null);
				}catch (Exception e){
					SplashScreen.errorCode = 8;
				}
				if (Return.length()!=0 && !Return.startsWith("Error"))//no network
					break;
				MyUtil.Sleep(1500);
			}while(++count<3);
			
			if (Return.length()==0) {
				SplashScreen.errorCode = 9;
				Return="fail,nonetwork";
			} else if (Return.startsWith("ok,registration")) {
				mPref.write("AireRegistered", true);
				mPref.write("myPhoneNumber", username);
				mPref.write("password", password);
				mPref.write("myIMEI", (DeviceID==null)?"":DeviceID);
				mPref.write("email", email);
				mPref.write("SubscribeId", SubscribeId);
				mPref.write("firstRegister", true);
				mPref.write("firstEnter", true);
				
				done=true;
			} else {
				SplashScreen.errorCode = 10;
			}
		}
	};

	private String cleanMAC(String mac) {
		mac = mac.trim();
		mac = mac.replace(" ", "");
		mac = mac.replace("~", "");
		mac = mac.replace("`", "");
		mac = mac.replace("!", "");
		mac = mac.replace("@", "");
		mac = mac.replace("#", "");
		mac = mac.replace("$", "");
		mac = mac.replace("%", "");
		mac = mac.replace("^", "");
		mac = mac.replace("&", "");
		mac = mac.replace("*", "");
		mac = mac.replace("-", "");
		mac = mac.replace("_", "");
		mac = mac.replace("+", "");
		mac = mac.replace("=", "");
		mac = mac.replace("(", "");
		mac = mac.replace(")", "");
		mac = mac.replace("[", "");
		mac = mac.replace("]", "");
		mac = mac.replace("{", "");
		mac = mac.replace("}", "");
		mac = mac.replace("|", "");
		mac = mac.replace("\\", "");
		mac = mac.replace(":", "");
		mac = mac.replace(";", "");
		mac = mac.replace("\"", "");
		mac = mac.replace("<", "");
		mac = mac.replace(",", "");
		mac = mac.replace(">", "");
		mac = mac.replace(".", "");
		mac = mac.replace("?", "");
		mac = mac.replace("/", "");
		return mac;
	}
	/** Get the STB MacAddress */ 
	public String getMacAddress() { 
		try { 
			Log.e("tml DO getMacAddress DO");
			//return loadFileAsString("/sys/class/net/eth0/address").toLowerCase().substring(0, 17);
			return loadFileAsString("/sys/class/net/wlan0/address").toLowerCase().substring(0, 17);
		} catch (IOException e) { 
			e.printStackTrace(); 
//			return null;
			Log.e("tml NO WLAN MAC");
			return "null";  //tml|mj|wjx*** new mac/
		} 
	}
	//tml|mj|wjx*** new mac
	public String getLanMacAddress() { 
		try {
			Log.e("tml DO getLanMacAddress DO");
			return loadFileAsString("/sys/class/net/eth0/address").toLowerCase().substring(0, 17);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("tml NO LAN MAC");
			return "null";
		} 
	}
	
	public static String loadFileAsString(String filePath) throws java.io.IOException { 
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath)); 
		char[] buf = new char[1024]; 
		int numRead = 0; 
		while ((numRead = reader.read(buf)) != -1) { 
			String readData = String.valueOf(buf, 0, numRead); 
			fileData.append(readData); 
		} 
		reader.close(); 
		return fileData.toString(); 
	}
}
