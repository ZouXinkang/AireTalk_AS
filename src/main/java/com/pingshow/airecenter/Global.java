package com.pingshow.airecenter;

import android.os.Environment;

public class Global {
	public static final String STB_HeaderName = "ac";  //tml*** detect STB
	public static final String STB_Name1 = "m200";
	public static final int STB_NameLength = 18;
	
	public static final String Temp_Parse = "[<802>]";
	public static final String Master_Parse = "[<AireNinja>]";
	public static final String MONITOR = Master_Parse + "monitor";

	public static final String Hi_AddFriend1 = "Hi"; 
	public static final String Hi_AddFriend2 = "Hi (802";  //Master_Parse + Hi_AddFriend1;
	public static final String Call_Conference = ":-) Call u? :-)";
	public static final String Call_Conference_Switch = Master_Parse + Call_Conference + "switch!";
	public static final String Call_Conference_Video_Open = Master_Parse + Call_Conference + "open_video!";
	public static final String Call_Conference_Video_Close = Master_Parse + Call_Conference + "close_video!";
//	public static final String Call_Conference_Mute = Master_Parse + Call_Conference + "Mute";
//	public static final String Call_Conference_Speak = Master_Parse + Call_Conference + "Speak";
	public static final String Call_Broadcast = "broadcast!";
	public static final String SUV_ON = "GUARD";
	public static final String SUV_OFF = "GUARD REST";
	public static final String SUV_ON_IOTALL = SUV_ON + "IOTALL";
	public static final String SUV_OFF_IOTALL = SUV_OFF + "IOTALL";
	
	public static final String Action_MsgSent="com.pingshow.airecenter.JustSentMessage";
	public static final String Action_MsgGot="com.pingshow.airecenter.NewMessageArrival";
	public static final String Action_Contact="com.pingshow.airecenter.ContactUpdate";
	public static final String Action_HistroyThread="com.pingshow.airecenter.HistoryUpdate";
	public static final String Action_InternalCMD="com.pingshow.airecenter.InternalCommand";
	public static final String Action_AnswerCall="com.pingshow.airecenter.AnswerCall";
	public static final String Action_Friends_Status_Updated="com.pingshow.airecenter.FriendsUpdated";
	public static final String Action_Download_And_Update="com.pingshow.airecenter.updateSilently";
	public static final String Action_Refresh_Gallery="com.pingshow.airecenter.RefreshGallery";
	public static final String Action_SMS_Fail = "com.pingshow.airecenter.smsFail";
	public static final String Action_InsertSMSToSYS = "com.pingshow.airecenter.smstosys";
	public static final String Action_FileDownload = "com.pingshow.airecenter.filedownload";
	public static final String Action_SD_AvailableSpare = "com.pingshow.airecenter.sdavailable";
	public static final String Action_Sip_Photo_Download_Complete = "com.pingshow.airecenter.sipPhotoCompleted";
	public static final String Action_Raw_Audio_Playback = "com.pingshow.airecenter.rawaudioplayback";
	public static final String Action_Chatroom_Members = "com.pingshow.airecenter.chatroom.members";
	public static final String Action_Start_Surveillance = "com.pingshow.airecenter.surveillance";
	public static final String Action_End_Surveillance = "com.pingshow.airecenter.surveillance.off";
	public static final String Action_Create_Group = "com.pingshow.airecenter.group.create";
	public static final String Action_UserPage_Command = "com.pingshow.airecenter.userpage.command";
	public static final String Action_SearchPage_Adding = "com.pingshow.airecenter.searchrpage.command.add";
	public static final String Action_Refresh_LOCATIONTIMER="com.pingshow.airecenter.RefreshLocationTimer";
	public static final String Action_SecurityActivity="com.pingshow.airecenter.SecurityActivity";  //tml*** multi suvei
	public static final String Action_UsbPermission="com.pingshow.airecenter.UsbPermission";  //tml*** usb dongle
	public static final String Action_UsbPermissionActivity="com.pingshow.airecenter.UsbPermissionActivity";  //tml*** usb dongle
	public static final String Action_Start_Homesensor = "com.pingshow.airecenter.homesensor";  //tml*** iot control
	public static final String Action_End_Homesensor = "com.pingshow.airecenter.homesensor.off";  //tml*** iot control
	public static final String Action_Video_Open = "com.pingshow.airecenter.video.on";  //tml*** iot control
	public static final String Action_Video_Close = "com.pingshow.airecenter.video.off";  //tml*** iot control
	
	public static final String SdcardPath=Environment.getExternalStorageDirectory()+"/.com.airecenter/";
	public static final String SdcardPath_inbox=Environment.getExternalStorageDirectory()+"/.com.airecenter/inbox/";
	public static final String SdcardPath_sent=Environment.getExternalStorageDirectory()+"/.com.airecenter/sent/";
	public static final String SdcardPath_downloads=Environment.getExternalStorageDirectory()+"/Download/";
	public static final String SdcardPath_temp=Environment.getExternalStorageDirectory()+"/.com.airecenter/tmp/";
//	public static final String SdcardPath_record=Environment.getExternalStorageDirectory()+"/.com.airecenter/record/";
	//tml*** browser save
	public static final String SdcardPath_airetalk="/sdcard/AireTalkTV/";
	public static final String SdcardPath_record="/sdcard/AireTalkTV/security/";
	public static final String SdcardPath_video="/sdcard/AireTalkTV/video/";
	public static final String SdcardPath_image="/sdcard/AireTalkTV/images/";
	public static final String SdcardPath_files="/sdcard/AireTalkTV/files/";
	public static final String SdcardPath_music="/sdcard/AireTalkTV/music/";
	public static final String Storage_USBSD="/storage/external_storage/";
	public static final String Storage_USB2="/mnt/usb_storage/";
	public static final String Storage_SD2="/mnt/external_sd/";
	
	public final static String ACTION_PLAY_AUDIO = "com.pingshow.airecenter.playAudio";
	final public static String ACTION_PLAY_OVER = "com.pingshow.airecenter.playAudioOver";
	public final static String ACTION_TUNING = "com.pingshow.airecenter.TuningUp";
	public final static String ACTION_TUNING_START = "com.pingshow.airecenter.TuningStart";
	public final static String ACTION_JUMP_TO_URL = "com.pingshow.airecenter.jumpToURL";

	public final static String MSG_UNREAD_YES = "com.pingshow.airecenter.unreadmsgyes";
	public final static String MSG_RETURN_NOM = "com.pingshow.airecenter.returnmsgnom";
	
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
	public static final int CMD_INVITE_AIRE_USER = 76;
	public static final int CMD_CONNECTION_POOR = 78;
	public static final int CMD_PARTITION_FILE = 80;
	public static final int CMD_REFRESH_CONN = 82;
	public static final int CMD_SUVALARM_ON = 99;
	public static final int CMD_SUVALARM_OFF = 90;
	public static final int CMD_SUV_CALLLIMIT= 92;  //tml*** suv call limit

	public static final String KILL_dialer="com.pingshow.airecenter.DialerActivity.kill";
	public static final String KILL_videocall="com.pingshow.airecenter.VideoCallActivity.kill";
	
	public static final long DEFAULT_LAT = 37320332;
	public static final long DEFAULT_LON = -122032619;
	
	public static final String KEY="D53D782E6518B37D4D12BE31D2DA878DEBE1E4C3";
}
