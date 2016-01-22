package com.pingshow.amper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

/* //tml*** phone intent
 * to intercept actual phonecall...
 * will require the following in Manifest:
 * 
 * <uses-permission android:name="android.permission.PROCESS_OUTGOING_CALLS" />
 * 
 * <receiver android:name=".AireCallReceiver">
 * 		<intent-filter>
 * 			<action android:name="android.intent.action.NEW_OUTGOING_CALL" />
 * 			<category android:name="android.intent.category.DEFAULT" />
 * 		</intent-filter>
 * </receiver>
 * 
 * (could implement on non-sim devices that have dialpad)
 */

public class AireCallReceiver extends BroadcastReceiver {
	private Context mContext;
	private String incPhoneNumber;

	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		
		String phoneNumber = getResultData();
//		Log.d("AireCallReceiver0 > " + phoneNumber);
	    if (phoneNumber == null) {
	    	phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
	    }
//		Log.d("AireCallReceiver1 > " + phoneNumber);
		
	    incPhoneNumber = phoneNumber;
	    
		Handler mHandler = new Handler();
		mHandler.post(new Runnable() {
			public void run () {
				Intent it = new Intent(mContext, SipCallActivity.class);
				it.putExtra("IncPhoneNumber", incPhoneNumber);
				it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(it);
			}
		});
	}
}