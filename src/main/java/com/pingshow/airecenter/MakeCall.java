package com.pingshow.airecenter;

import android.content.Context;
import android.content.Intent;

import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.util.MyTelephony;
import com.pingshow.voip.AireVenus;

public class MakeCall {
	
	static public void FileTransferCall(Context context, String PhoneNumber)
	{
		AireVenus.setCallType(AireVenus.CALLTYPE_FILETRANSFER);
		callByFafa(context, PhoneNumber, false, null);
	}
	
	static public void Call(Context context, String PhoneNumber, boolean bSipCall)
	{
//		Log.i("Call>> " + PhoneNumber + " " + bSipCall);  //tml***
		if (bSipCall==false)
			AireVenus.setCallType(AireVenus.CALLTYPE_FAFA);
		Call(context,PhoneNumber,false,bSipCall);
	}
	
	static public void SipCall(Context context, String PhoneNumber, String displayname, boolean withVideo)
	{
//		Log.i("SipCall>> " + PhoneNumber + " " + withVideo + " " + displayname);  //tml***
		callByFafa(context, PhoneNumber, withVideo, displayname);
	}

	/**
	 *
	 * @param context
	 * @param idx
	 * @param broadcast		广播1：主叫 0：被叫
	 * @param isBroadcast	是否是广播
	 * @return
	 */
	public static String ConferenceCall(Context context, String idx,int broadcast,boolean isBroadcast){
		
//		String dialNumber = isBroadcast ? "1008" : "1007";
		String dialNumber="";
		if (isBroadcast) {
			if (broadcast==1) {
				dialNumber="1007";
			}else if (broadcast==0) {
				dialNumber="1008";
			}
		}else{
			dialNumber	= "1007";
		}
		 
		for (int i = idx.length(); i < 7; i++)
			dialNumber += "0";
		dialNumber += idx;
		
		SipCall(context, dialNumber, context.getString(R.string.conference), false);
		return dialNumber;
	}
	static public void Call(Context context, String PhoneNumber, boolean withVideo, boolean bSipCall)
	{
//		Log.i("Call>> " + PhoneNumber + " " + withVideo + " " + bSipCall);  //tml***
		if (bSipCall)
			callByFafa(context, PhoneNumber, false, null);
		else {
			AireVenus.setCallType(AireVenus.CALLTYPE_FAFA);
			callByFafa(context, PhoneNumber, withVideo, null);
		}
	}

	private static void callByFafa(Context context,String PhoneNumber,boolean withVideo, String displayname)
	{
//		Log.i("callByFafa>> " + PhoneNumber + " " + withVideo + " " + displayname);  //tml***
		ContactsQuery cq = new ContactsQuery(context);
		long contact_id=cq.getContactIdByNumber(PhoneNumber);
		
		if (!PhoneNumber.startsWith("+"))
		{
			if (contact_id>0)
			{
				String phonebookNumber=cq.getPossibleGlobalNumberByContactId(contact_id, PhoneNumber);Log.e("tmlc5a mc");
				if (phonebookNumber.startsWith("+"))
					PhoneNumber=phonebookNumber;
			}
			PhoneNumber=MyTelephony.attachPrefix(context, PhoneNumber);
		}
		
		Intent it = new Intent(Global.Action_InternalCMD);
		it.putExtra("Command", Global.CMD_MAKE_OUTGOING_CALL);
		if (displayname!=null)
		{
			it.putExtra("Displayname", displayname);
			it.putExtra("Commercial", true);
		}
		it.putExtra("Callee", PhoneNumber);
		it.putExtra("Contact_id", contact_id);
		it.putExtra("VideoCall", withVideo);
		MyPreference mPref=new MyPreference(context);
		mPref.write("way", true);
		context.sendBroadcast(it);
		Log.d("Sending CMD_MAKE_OUTGOING_CALL... " + contact_id + " " + PhoneNumber);
	}
	
}