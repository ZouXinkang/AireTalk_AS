package com.pingshow.util;

import com.pingshow.amper.Global;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;

import android.content.Context;
import android.text.TextUtils;

//tml*** profile manager
public class MyProfile {
	private static MyProfile instance;
	private Context mContext;

	private MyPreference mPref;
	private static final String KEY_registered = "Registered";
	private static final String KEY_profileCompleted = "ProfileCompleted";
	private static final String KEY_firstEnter = "firstEnter";
	private static final String KEY_shortcut2Created = "shortcut2Created";
	private static final String KEY_myPhotoUploaded = "myPhotoUploaded";
	private static final String KEY_myVersionCode = "versionCode";
	private static final String KEY_myBrandDeviceModelProduct = "myBrandDeviceModelProduct";
	private static final String KEY_myPhoneNumber = "myPhoneNumber";
	private static final String KEY_myNickname = "myNickname";
	private static final String KEY_myID = "myID";
	private static final String KEY_myPhotoPath = "myPhotoPath";
	private static final String KEY_myEmail = "email";
	private static final String KEY_myMood = "moodcontent";
	private static final String KEY_myISO = "iso";
	private static final String KEY_myRoamID = "myRoamId";
	private static final String KEY_myLongitude = "longitude";
	private static final String KEY_myLatitude = "latitude";
	private static final String KEY_myLocAccuracy = "accuracy";
	private static final String KEY_mySuveis = "Suvei";

	private static final int DEF_version = 1000;
	private static final String DEF_id = "0";
	private static final String DEF_iso = "cn";
	private static final String DEF_roam = "3";
	private static final Long DEF_long = (long) 39976279;
	private static final Long DEF_lat = (long) 116349386;
	
	private MyProfile (Context context) {
		mContext = context;
		mPref = new MyPreference(context);
	}
	
	public static void init(Context context) {
		if (context != null) {
			if (instance == null) {
				instance = new MyProfile(context);
//				instance = new MyProfile(context.getApplicationContext());
			}
			Log.i("profile init (" + context.getClass().getSimpleName() + ")");
		} else {
			throw new NullPointerException("MyProfile.init() NULL");
		}
	}
	
	public static MyProfile load() {
		if (instance == null) {
            throw new IllegalStateException("Call MyProfile.init() first");
        }

        return instance;
	}
	
	public boolean isRegistered() {
		boolean registered = mPref.readBoolean(KEY_registered, false);
		return registered;
	}
	
	public void okRegistered() {
		mPref.write(KEY_registered, true);
	}
	
	public boolean isProfileComplete() {
		boolean profileCompleted = mPref.readBoolean(KEY_profileCompleted, false);
		return profileCompleted;
	}
	
	public void okProfileComplete() {
		mPref.write(KEY_profileCompleted, true);
	}
	
	public boolean isFirstEnter() {
		boolean firstEnter = mPref.readBoolean(KEY_firstEnter, false);
		return firstEnter;
	}
	
	public void okFirstEnter() {
		mPref.write(KEY_firstEnter, true);
	}
	
	public boolean isShortcut2Created() {
		boolean shortcut2Created = mPref.readBoolean(KEY_shortcut2Created, false);
		return shortcut2Created;
	}
	
	public void okShortcut2Created() {
		mPref.write(KEY_shortcut2Created, true);
	}
	
	public boolean isMyPhotoUploaded() {
		boolean myPhotoUploaded = mPref.readBoolean(KEY_myPhotoUploaded, false);
		return myPhotoUploaded;
	}
	
	public void okMyPhotoUploaded() {
		mPref.write(KEY_myPhotoUploaded, true);
	}
	
	public int getMyVersionCode() {
		int myVersionCode = mPref.readInt(KEY_myVersionCode, DEF_version);
		return myVersionCode;
	}
	
	public void saveMyVersionCode(int version, boolean del) {
		if (del) {
			mPref.delect(KEY_myVersionCode);
			return;
		} else {
			mPref.write(KEY_myVersionCode, version);
		}
	}
	
	public String getWhatsMyDevice() {
		String myDevice = mPref.read(KEY_myBrandDeviceModelProduct, "---,---,---,---");
		return myDevice;
	}
	
	public void saveWhatsMyDevice(String device, boolean del) {
		if (del) {
			mPref.delect(KEY_myBrandDeviceModelProduct);
			return;
		} else {
			mPref.write(KEY_myBrandDeviceModelProduct, device);
		}
	}
	
	public String getMyPhoneNumber() {
		String myPhoneNumber = mPref.read(KEY_myPhoneNumber, "---");
		return myPhoneNumber;
	}
	
	public void saveMyPhoneNumber(String phonenumber, boolean del) {
		if (del) {
			mPref.delect(KEY_myPhoneNumber);
			return;
		} else {
			mPref.write(KEY_myPhoneNumber, phonenumber);
		}
	}
	
	public String getMyNickname() {
		String myNickname = mPref.read(KEY_myNickname, "");
		return myNickname;
	}
	
	public void saveMyNickname(String nickname, boolean del) {
		if (del) {
			mPref.delect(KEY_myNickname);
			return;
		} else {
			mPref.write(KEY_myNickname, nickname);
		}
	}
	
	public int getMyIdx() {
		int myIdx = 0;
		try {
			String myIdxHex = getMyIdxHex();
			myIdx = Integer.parseInt(myIdxHex, 16);
		} catch (NumberFormatException e) {
			Log.e("getMyIdx parseInt !@#$ " + e.getMessage());
		}
		return myIdx;
	}
	
	public void saveMyIdx(int idx, boolean del) {
		saveMyIdxHex(Integer.toHexString(idx), del);
	}
	
	public String getMyIdxHex() {
		String myIdxHex = mPref.read(KEY_myID, DEF_id);
		return myIdxHex;
	}
	
	public void saveMyIdxHex(String idxhex, boolean del) {
		if (del) {
			mPref.delect(KEY_myID);
			return;
		} else {
			mPref.write(KEY_myID, idxhex);
		}
	}
	
	public String getMyPhotoPath() {
		String myPhotoPath = mPref.read(KEY_myPhotoPath, null);
		return myPhotoPath;
	}
	
	public void saveMyPhotoPath(String path, boolean del) {
		if (del) {
			mPref.delect(KEY_myPhotoPath);
			return;
		} else {
			mPref.write(KEY_myPhotoPath, path);
		}
	}
	
	public String getMyEmail() {
		String myEmail = mPref.read(KEY_myEmail, "");
		return myEmail;
	}
	
	public void saveMyEmail(String email, boolean del) {
		if (del) {
			mPref.delect(KEY_myEmail);
			return;
		} else {
			mPref.write(KEY_myEmail, email);
		}
	}
	
	public String getMyMood() {
		String myMood = mPref.read(KEY_myMood, "");
		return myMood;
	}
	
	public void saveMyMood(String mood, boolean del) {
		if (del) {
			mPref.delect(KEY_myMood);
			return;
		} else {
			mPref.write(KEY_myMood, mood);
		}
	}
	
	public String getMyISO() {
		String myISO = mPref.read(KEY_myISO, DEF_iso);
		return myISO;
	}
	
	public void saveMyISO(String iso, boolean del) {
		if (del) {
			mPref.delect(KEY_myISO);
			return;
		} else {
			mPref.write(KEY_myISO, iso);
		}
	}
	
	public String getMyRoamID() {
		String myRoamID = mPref.read(KEY_myRoamID, DEF_roam);
		return myRoamID;
	}
	
	public void saveMyRoamID(String roam, boolean del) {
		if (del) {
			mPref.delect(KEY_myRoamID);
			return;
		} else {
			mPref.write(KEY_myRoamID, roam);
		}
	}
	
	public Long getMyLongitude() {
		Long myLongitude = mPref.readLong(KEY_myLongitude, DEF_long);
		return myLongitude;
	}
	
	public void saveMyLongitude(Long longitude, boolean del) {
		if (del) {
			mPref.delect(KEY_myLongitude);
			return;
		} else {
			mPref.writeLong(KEY_myLongitude, longitude);
		}
	}
	
	public Long getMyLatitude() {
		Long myLatitude = mPref.readLong(KEY_myLatitude, DEF_lat);
		return myLatitude;
	}
	
	public void saveMyLatitude(Long latitude, boolean del) {
		if (del) {
			mPref.delect(KEY_myLatitude);
			return;
		} else {
			mPref.writeLong(KEY_myLatitude, latitude);
		}
	}
	
	public Float getMyLocAccuracy(Float defValue) {
		Float myLocAccuracy = mPref.readFloat(KEY_myLocAccuracy, defValue);
		return myLocAccuracy;
	}
	
	public void saveMyLocAccuracy(Float accuracy, boolean del) {
		if (del) {
			mPref.delect(KEY_myLocAccuracy);
			return;
		} else {
			mPref.writeFloat(KEY_myLocAccuracy, accuracy);
		}
	}
	
	public boolean givenSecurityAccess(String address) {
		boolean found = false;
		for (int i = 0; i < Global.MAX_SUVS; i++) {
			String suv_contacts = mPref.read(KEY_mySuveis + i);
			if (!TextUtils.isEmpty(suv_contacts)) {
				if (suv_contacts.equals(address)) {
					found = true;
					break;
				}
			}
		}
		return found;
	}
}
