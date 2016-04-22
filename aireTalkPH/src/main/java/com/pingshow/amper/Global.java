package com.pingshow.amper;

import android.os.Environment;

public class Global {
	public static final String STB_HeaderName = "ac";  //tml*** detect STB
	public static final String STB_Name1 = "m200";
	public static final String STB_Name2 = "mq10";
	public static final int STB_NameLength = 18;

	public static final String Temp_Parse = "[<802>]";
	public static final String Master_Parse = "[<AireNinja>]";

	public static final String MONITOR = Master_Parse + "monitor";
	public static final String Hi_AddFriend1 = "Hi";
	public static final String Hi_AddFriend2 = "Hi (802";  //Master_Parse + Hi_AddFriend1;
	public static final String Call_Conference = ":-) Call u? :-)";
	//	public static final String Call_Conference_Mute = Master_Parse + Call_Conference + "Mute";
//	public static final String Call_Conference_Speak = Master_Parse + Call_Conference + "Speak";
	public static final String Call_Conference_Switch = Master_Parse + Call_Conference + "switch!";

	public static final String Action_MsgSent="com.pingshow.amper.JustSentMessage";
	public static final String Action_MsgGot="com.pingshow.amper.NewMessageArrival";
	public static final String Action_Contact="com.pingshow.amper.ContactUpdate";
	public static final String Action_HistroyThread="com.pingshow.amper.HistoryUpdate";
	public static final String Action_InternalCMD="com.pingshow.amper.InternalCommand";
	public static final String Action_AnswerCall="com.pingshow.amper.AnswerCall";
	public static final String Action_Friends_Status_Updated="com.pingshow.amper.FriendsUpdated";
	public static final String Action_Download_And_Update="com.pingshow.amper.updateSilently";
	public static final String Action_Refresh_Gallery="com.pingshow.amper.RefreshGallery";
	public static final String Action_SMS_Fail = "com.pingshow.amper.smsFail";
	public static final String Action_InsertSMSToSYS = "com.pingshow.amper.smstosys";
	public static final String Action_FileDownload = "com.pingshow.amper.filedownload";
	public static final String Action_SD_AvailableSpare = "com.pingshow.amper.sdavailable";
	public static final String Action_Sip_Photo_Download_Complete = "com.pingshow.amper.sipPhotoCompleted";
	public static final String Action_Raw_Audio_Playback = "com.pingshow.amper.rawaudioplayback";
	public static final String Action_Chatroom_Members = "com.pingshow.amper.chatroom.members";

	//jack hide group icon
	public static final String Action_Hide_Group_Icon = "com.pingshow.amper.hidegroupicon";


	public static final String SdcardPath=Environment.getExternalStorageDirectory()+"/.com.amper/";
	public static final String SdcardPath_inbox=Environment.getExternalStorageDirectory()+"/.com.amper/inbox/";
	public static final String SdcardPath_sent=Environment.getExternalStorageDirectory()+"/.com.amper/sent/";
	public static final String SdcardPath_timeline=Environment.getExternalStorageDirectory()+"/.com.amper/time/";
	public static final String SdcardPath_downloads=Environment.getExternalStorageDirectory()+"/download/";

	public final static String ACTION_PLAY_AUDIO = "com.pingshow.amper.playAudio";
	final public static String ACTION_PLAY_OVER = "com.pingshow.amper.playAudioOver";
	public final static String ACTION_TUNING = "com.pingshow.amper.TuningUp";
	public final static String ACTION_TUNING_START = "com.pingshow.amper.TuningStart";

	public final static String MSG_UNREAD_YES = "com.pingshow.amper.unreadmsgyes";
	public final static String MSG_RETURN_NOM = "com.pingshow.amper.returnmsgnom";

	public static final int CMD_TRIGGER_SENDEE = 2;
	public static final int CMD_RECONNECT_SOCKET = 4;
	public static final int CMD_ON_SMS_COMING = 6;
	public static final int CMD_CHECK_ONLINE_FRIENDS = 8;
	public static final int CMD_SUDDENLY_NO_NETWORK = 10;
	public static final int CMD_CHECK_ONLINE_FRIENDS_NOW = 12;
	public static final int CMD_ONLINE_UPDATE = 14;
	public static final int CMD_LOCATION_SHARING = 16;
	public static final int CMD_SHARING_AGREE = 18;
	public static final int CMD_INCOMING_CALL = 20;
	public static final int CMD_MAKE_OUTGOING_CALL = 22;
	public static final int CMD_CALL_END = 24;
	public static final int CMD_LOGIN_FAILED = 26;
	public static final int CMD_TCP_CONNECTION_UPDATE = 28;
	public static final int CMD_TCP_MESSAGE_ARRIVAL = 30;
	public static final int CMD_UPDATE_SENT_SMS_TIME = 32;
	public static final int CMD_INTERPHONE_START = 34;
	public static final int CMD_UPLOAD_PROFILE_PHOTO = 36;
	public static final int CMD_UPLOAD_PROFILE_MOOD = 38;
	public static final int CMD_UPDATE_CALL_LOG = 40;
	public static final int CMD_QUERY_360 = 42;
	public static final int CMD_CALLEND_REF_GALLERY = 44;
	public static final int CMD_UPLOAD_FRIENDS = 46;
	public static final int CMD_DOWNLOAD_FRIENDS = 47;
	public static final int CMD_DOWNLOAD_FRIENDS_PART2 = 48;
	public static final int CMD_DOWNLOAD_PHOTO_FROMNET = 50;
	public static final int CMD_UPLOAD_PROFILE_EMAIL = 52;
	public static final int CMD_UPDATE_MY_NICKNAME = 54;
	public static final int CMD_STRANGER_COMING = 56;
	public static final int CMD_UPDATE_SIP_CREDIT = 58;
	public static final int CMD_SEARCH_POSSIBLE_FRIENDS = 60;
	public static final int CMD_JOIN_A_NEW_GROUP = 62;
	public static final int CMD_JOIN_A_NEW_GROUP_VERIFIED = 64;
	public static final int CMD_LEAVE_GROUP = 66;
	public static final int CMD_DELETE_GROUP = 68;
	public static final int CMD_ADD_AS_RELATED_FRIEND = 70;
	public static final int CMD_FILE_TRANSFERED = 72;
	public static final int CMD_GROUP_ADD_NEW_MEMBER = 74;
	public static final int CMD_CHECK_PAYPAL_AGAIN = 76;
	public static final int CMD_CONNECTION_POOR = 78;
	public static final int CMD_ADDF_280 = 282;
	public static final int CMD_GROUP_SENDEE = 80; //jack send group sendee


	public static final int CMD_TCP_COMMAND_ARRIVAL = 810;//li*** 收到命令消息。
	public static final int MAX_SUVS = 10;  //tml*** multi suvei
	public static final String SUV_ON = "GUARD";
	public static final String SUV_OFF = "GUARD REST";
	public static final String SUV_ON_IOTALL = SUV_ON + "IOTALL";
	public static final String SUV_OFF_IOTALL = SUV_OFF + "IOTALL";
	public static final String Call_Broadcast = "broadcast!";
	public static final String ISSECURITY = "issecurity!";
	public static final String ISNEWBROST = "isNewBrost!";
}
