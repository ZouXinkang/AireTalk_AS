package com.pingshow.amper.message;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.ConversationActivity;
import com.pingshow.amper.Global;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.R;
import com.pingshow.amper.SMS;
import com.pingshow.amper.SettingActivity;
import com.pingshow.amper.WalkieTalkieDialog;
import com.pingshow.amper.bean.Group;
import com.pingshow.amper.bean.GroupMsg;
import com.pingshow.amper.bean.GroupUpdateMsg;
import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.RelatedUserDB;
import com.pingshow.amper.db.SmsDB;
import com.pingshow.network.MyNet;
import com.pingshow.network.NetInfo;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;
import com.pingshow.voip.core.VoipCore;

import org.json.JSONException;
import org.json.JSONObject;

public class ParseSmsLine {

    static public ArrayList<SMS> unknownList = new ArrayList<SMS>();
    static public ArrayList<String> ridList = new ArrayList<String>();
    private static final String[] securityon_strings = {"Security mode is on!",
            "تشغيل وضع مراقبة على",
            "El modo de seguridad está activado.",
            "Mode de sécurité est activé.",
            "監視モードがオンになっている。",
            "보안 모드가 있습니다!",
            "Modo de segurança está ligado.",
            "安全監控模式已開啟!",
            "安全监控模式已开启!"};
    private static final String[] securityoff_strings = {"Security mode is off",
            "مغلق طريقة الرصد",
            "El modo de seguridad está desactivado.",
            "Mode de sécurité est désactivé.",
            "監視モードでは閉じている。",
            "보안 모드가 꺼져 있습니다.",
            "Modo de segurança está desligada.",
            "安全監控模式關閉。",
            "安全监控模式关闭"};
    private static Gson gson;
    private SMS msg;

    static public ArrayList<SMS> Parse(Context context, String data, ContactsQuery cq, AmpUserDB mADB, RelatedUserDB mRDB, MyPreference mPref) {
        Log.d("进入Parse");
        ArrayList<SMS> smslist = new ArrayList<SMS>();

//		Log.w("data:"+data);
        Pattern p = Pattern.compile("<U>");
        String[] msgs = p.split(data, 100);
        Pattern pp = Pattern.compile("<Z>");

        SmsDB mDB = new SmsDB(context);
        mDB.open(false);
        for (int i = 0; i < msgs.length - 1; i++) {
            Log.i("PARSEolm [" + i + "] " + msgs[i]);
            try {
                if (msgs[i].length() > 0) {
                    String[] items = pp.split(msgs[i], 15);
                    if (items[0].length() == 0 || mADB.isUserBlocked(items[0]) == 1)
                        continue;

                    int idx = Integer.valueOf(items[1], 16);

                    if (!mADB.isFafauser(idx)) {
                        // unknown person
                        boolean found = false;
                        for (int j = 0; j < unknownList.size(); j++) {
                            if (unknownList.get(j).address.equals(items[0])) {
                                found = true;
                                unknownList.get(j).read++;
                                break;
                            }
                        }
                        if (!found) {
                            SMS msg = new SMS();
                            msg.address = items[0];
                            msg.type = idx;
                            msg.read = 0;
                            unknownList.add(msg);
//			    			if (mPrf.readBoolean("Inviting:" + msg.address, false)) {  //tml*** friend invite
//								mPrf.delect("Inviting:" + msg.address);
//							}
                        }
                        //continue;
                    }
                    //tml*** rowid check
//					String rowid = "0";
//					boolean isExist = false;
//					try {
//						if (items.length > 0) {
//							String tmp = items[items.length - 1];
//							if (tmp.startsWith("`"))
//								rowid = tmp.substring(1);
//						}
//					} catch (Exception e) {}
//					if ("0".equals(rowid)) {
//						isExist = false;
//					} else {
//						String ridItem = items[1] + ":" + rowid;
//						if (ridList.size() > 0) {
//							for (int k = 0; k < ridList.size(); k++) {
//								if (ridList.get(k).equals(ridItem)) {
//									isExist = true;
//									break;
//								}
//							}
//						}
//						
//						if (!isExist) {
//							ridList.add(ridItem);
//						}
//					}
//					if (isExist) {
//						Log.e("msgX isExist");
//						continue;
//					}
                    //***tml

//		    		String phpIP = AireJupiter.myPhpServer_default;
                    String phpIP = null;
                    if (AireJupiter.getInstance() != null) {  //tml*** china ip
                        phpIP = AireJupiter.getInstance().getIsoPhp(0, true, null);
                    } else {
                        phpIP = AireJupiter.myPhpServer_default;
                    }
                    SMS msg = new SMS();

                    items[3] = URLDecoder.decode(items[3].replace('*', '%'), "UTF-8");

                    msg.address = items[0];
                    msg.content = items[3];//alec
                    msg.time = Long.parseLong(items[2], 16) * 1000;
                    msg.contactid = cq.getContactIdByNumber(msg.address);

                    long checkVcurTime = new Date().getTime();
                    if (msg.time > checkVcurTime) {
                        msg.time = checkVcurTime;
                    }

                    if (msg.contactid > 0)
                        msg.displayname = cq.getNameByContactId(msg.contactid);
                    else
                        msg.displayname = mADB.getNicknameByAddress(msg.address);

                    if (msg.displayname == null)
                        msg.displayname = context.getResources().getString(R.string.unknown_person);
                    //tml*** suv onoff alert
                    boolean security_on = false;
                    boolean security_off = false;
                    for (int k = 0; k < securityon_strings.length; k++) {
                        if (msg.content.equals(securityon_strings[k])) {
                            security_on = true;
                            break;
                        }
                        if (msg.content.equals(securityoff_strings[k])) {
                            security_off = true;
                            break;
                        }
                    }

                    if (msg.content.startsWith("`[")) {//alec
                        try {
                            int c = msg.content.indexOf("]\n");
                            int groupid = Integer.parseInt(msg.content.substring(2, c));
                            msg.address = "[<GROUP>]" + groupid;
                            msg.content = msg.content.substring(c + 2);
                            msg.displayname = mADB.getNicknameByAddress(msg.address);
                            msg.group_member = idx;

                            if (msg.content.equals(":)(Y)")) {
                                Intent it = new Intent(Global.Action_InternalCMD);
                                it.putExtra("Command", Global.CMD_JOIN_A_NEW_GROUP);
                                it.putExtra("GroupID", groupid);
                                context.sendBroadcast(it);
                                continue;
                            } else if (msg.content.startsWith(":-o$_$")) {
                                Intent it = new Intent(Global.Action_InternalCMD);
                                it.putExtra("Command", Global.CMD_GROUP_ADD_NEW_MEMBER);
                                it.putExtra("GroupID", groupid);
                                try {
                                    String newMember = msg.content.substring(6);
                                    it.putExtra("idx", Integer.parseInt(newMember));
                                } catch (Exception e) {
                                }

                                context.sendBroadcast(it);
                                continue;
                            } else if (msg.content.equals(":((Sk)")) {
                                Intent it = new Intent(Global.Action_InternalCMD);
                                it.putExtra("Command", Global.CMD_LEAVE_GROUP);
                                it.putExtra("GroupID", groupid);
                                it.putExtra("idx", msg.group_member);
                                context.sendBroadcast(it);
                                continue;
                            } else {
                                if (!mADB.isFafauser(groupid + 100000000)) {
                                    if (mDB != null && mDB.isOpen())
                                        mDB.close();
                                    return smslist;
                                }
                            }
                        } catch (Exception e1) {
                        }
                    }

                    if (items[3].startsWith("[<LOCATIONSHARING>]")) {
                        continue;
                        /* alec
						Intent it = new Intent(Global.Action_InternalCMD);
						it.putExtra("Command", Global.CMD_LOCATION_SHARING);
						it.putExtra("Sender", items[0]);
						msg.content = String.format(context.getResources().getString(R.string.hasaskshare), msg.displayname);
						context.sendBroadcast(it);*/
                    } else if (msg.content.startsWith("[<activeCall>]")) {  //alex push
                        Log.i("receive active call");
                        return smslist;
                    } else if (items[3].startsWith("[<hold>]")) {
                        continue;
                    } else if (items[3].startsWith("[<MISSEDREMIND>]")) {
                        int missCount = 1;
                        try {
                            missCount = Integer.parseInt(items[3].substring(16));
                        } catch (Exception e) {
                        }
                        msg.content = String.format(context.getResources().getString(R.string.missed_call_remind_1), missCount);
                        msg.content += " " + msg.displayname;
                        String tFormat = DateUtils.formatDateTime(context, msg.time - 60000,
                                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE);
                        String description2 = String.format(context.getResources().getString(R.string.missed_call_remind_2), tFormat);
                        msg.content += description2;
                    } else if (items[3].startsWith("[<NEWPHOTO>]")) {
                        String localfile = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
                        if (AireJupiter.getInstance().downloadPhoto216(idx, localfile)) {
                            //delete old bigger one
                            new File(Global.SdcardPath_inbox + "photo_" + idx + "b.jpg").delete();

                            //alec: download big one as well
                            localfile = Global.SdcardPath_inbox + "photo_" + idx + "b.jpg";
                            MyNet net = new MyNet(context);
                            //bree
                            net.DownloadUserPhoto("profiles/photo_" + idx + ".jpg", localfile);
//							net.Download("profiles/photo_"+idx+".jpg", localfile, null);

                            Intent intent = new Intent();
                            intent.setAction(Global.Action_Refresh_Gallery);
                            context.sendBroadcast(intent);
                        }
                        continue;
                    } else if (items[3].startsWith("[<NEWMOOD>]")) {
                        String newMood = items[3].substring(11);
                        int index = newMood.lastIndexOf("/");
                        if (index != -1) newMood = newMood.substring(0, index);
                        mADB.updateMoodByUID(idx, newMood);
                        continue;
                    } else if (items[3].startsWith("[<NEWUSERJOINS>]")) {
                        msg.content = context.getResources().getString(R.string.auto_notify_joins);
                    } else if (items[3].startsWith("[<AGREESHARE>]")) {
                        String globalNumber = MyTelephony.attachPrefix(context, msg.address);
                        try {
                            int relation = Integer.valueOf(items[3].split(",")[2]);
                            long timeout = MyUtil.getSharingTimeout(relation);
                            mPref.writeLong(globalNumber, timeout);
                            if (mPref.readLong("SpeedupMapMonitor", 0) < timeout * 1000)
                                mPref.writeLong("SpeedupMapMonitor", timeout * 1000);
                        } catch (Exception e) {
                        }

                    } else if (msg.content.startsWith("[<LOCATIONSHARING>]"))
                        msg.content = String.format(context.getResources().getString(R.string.hasaskshare), msg.displayname);
                    else if (msg.content.startsWith("[EMOTION")) {
                        int index = 1;
                        try {
                            index = Integer.valueOf(msg.content.substring(8, msg.content.length() - 1));
                        } catch (Exception e) {
                        }
                        msg.content = "[EMOTION" + index + "]";
                    } else if (msg.content.startsWith("I am your GUARD")) {
//		        		int maxSuvei = Global.MAX_SUVS;
//		        		for (int j = 0; j < maxSuvei; j++)
//						{
//							String addr=mPrf.read("Suvei"+j);
//							if (!addr.equals(msg.address))
//							{
//								Log.i("tml I am your GUARD1> " + msg.address);  //refer to GUARD2
//								mPrf.write("Suvei"+j, msg.address);
//								break;
//							}
//						}
//						
//						if(mDB!=null && mDB.isOpen())
//							mDB.close();
//						return smslist;
                        //tml*** multi suvei
                        Log.i("tml I am your GUARD2> " + msg.address);
                        int maxSuvei = Global.MAX_SUVS;
                        boolean suveiAdd = false;
                        boolean suveiFull = true;
                        boolean lastone = false;

                        String last = msg.content.substring(msg.content.length() - 4, msg.content.length());
                        if (last.equals("LAST")) lastone = true;  //last contact, confirm del

                        for (int j = 0; j < maxSuvei; j++) {
                            String addr = mPref.read("Suvei" + j);
                            Log.i("tmlsuv see.Suvei[" + j + "]=" + addr);
                            if (addr.equals("")) {
                                Log.i("tmlsuv add.Suvei[" + j + "]=" + msg.address);
                                mPref.write("Suvei" + j, msg.address);
                                suveiAdd = true;
                                break;
                            } else if (addr.equals(msg.address)) {
                                Log.i("tmlsuv already exists!");
                                suveiAdd = false;
                                suveiFull = false;
                                break;
                            }
                        }

                        if (!suveiAdd && suveiFull) {  //full
                            Log.i("tmlsuv suvei full!");
                            if (AireJupiter.getInstance() != null
                                    && AireJupiter.getInstance().tcpSocket.isLogged(false)) {
                                AireJupiter.getInstance().tcpSocket
                                        .send(msg.address, "GUARD list IS FULL LATE", 0, null, null, 0, null);
                            }

                            String warning = "";
                            msg.content = context.getResources().getString(R.string.home_guard) + "!\n"
                                    + context.getResources().getString(R.string.maxsuvei1)
                                    + " (" + maxSuvei + "). "
                                    + context.getResources().getString(R.string.maxsuvei2)
                                    + warning;
                        } else if (!suveiAdd) {  //no add
                            if (mDB != null && mDB.isOpen()) mDB.close();
                            return smslist;
                        } else if (suveiAdd) {  //add
                            Intent intent = new Intent();
                            intent.setAction(Global.Action_Refresh_Gallery);
                            context.sendBroadcast(intent);
                            if (lastone) {  //last contact, warning msg
                                mPref.write("SuvDelWARN" + msg.address, true);
                                Log.i("SuvDelWARN " + msg.address);
                                msg.content = context.getResources().getString(R.string.home_guard) + "!\n"
                                        + context.getResources().getString(R.string.suvdelwarming);
                            } else {
                                if (mDB != null && mDB.isOpen()) mDB.close();
                                return smslist;
                            }
                        }
                        //***tml
                    } else if (security_on) {  //tml*** suv onoff alert
                        int maxSuvei = Global.MAX_SUVS;

                        for (int j = 0; j < maxSuvei; j++) {
                            String addr = mPref.read("Suvei" + j);
                            if (addr.equals(msg.address)) {
                                Log.i("security ON notified from " + msg.address);
                                mPref.write("SuveiON" + j, msg.address);
                                Intent intent = new Intent();
                                intent.setAction(Global.Action_Refresh_Gallery);
                                context.sendBroadcast(intent);
                            }
                        }
                    } else if (security_off) {  //tml*** suv onoff alert
                        int maxSuvei = Global.MAX_SUVS;

                        for (int j = 0; j < maxSuvei; j++) {
                            String addr = mPref.read("Suvei" + j);
                            if (addr.equals(msg.address)) {
                                Log.i("security OFF notified from " + msg.address);
                                mPref.delect("SuveiON" + j);
                                Intent intent = new Intent();
                                intent.setAction(Global.Action_Refresh_Gallery);
                                context.sendBroadcast(intent);
                            }
                        }
                    } else if (msg.content.startsWith(Global.Call_Conference)) {
                        if (mDB != null && mDB.isOpen())
                            mDB.close();
                        return smslist;
                    }
                    // bree
//					else if (msg.content.startsWith(Global.Call_Conference_Mute)) {
//						if (DialerActivity.getDialer() != null)
//							DialerActivity.getDialer().setMute(true);
//					} else if (msg.content.startsWith(Global.Call_Conference_Speak)) {
//						if (DialerActivity.getDialer() != null)
//							DialerActivity.getDialer().setMute(false);
//					}
                    else if (msg.content.startsWith(Global.Master_Parse)) {
                        if (mDB != null && mDB.isOpen()) mDB.close();
                        return smslist;
                    } else if (msg.content.startsWith("I am NOT your GUARD")) {
//		        		int maxSuvei = Global.MAX_SUVS;
//		        		for (int j = 0; j < maxSuvei; j++)
//						{
//							String addr=mPrf.read("Suvei"+j);
//							if (addr.equals(msg.address))
//							{
//								Log.i("tml I am NOT your GUARD1> " + msg.address);  //refer to GUARD2
//								mPrf.delect("Suvei"+j);
//								break;
//							}
//						}
//						
//						if(mDB!=null && mDB.isOpen())
//							mDB.close();
//						return smslist;
                        Log.i("tml I am NOT your GUARD2> " + msg.address);
                        int maxSuvei = Global.MAX_SUVS;
                        boolean suveiDel = false;
                        for (int j = 0; j < maxSuvei; j++) {
                            String addr = mPref.read("Suvei" + j);
//							Log.i("tmlsuv see.Suvei[" + j + "]=" + addr);
                            if (addr.equals(msg.address)) {
                                mPref.delect("Suvei" + j);
                                suveiDel = true;
                                Intent intent = new Intent();
                                intent.setAction(Global.Action_Refresh_Gallery);
                                context.sendBroadcast(intent);
//								Log.i("tmlsuv del.Suvei[" + j + "]=" + msg.address);
                                break;
                            }
                        }

                        if (mDB != null && mDB.isOpen()) mDB.close();
                        return smslist;
                    } else if (msg.content.equals(Global.Hi_AddFriend1)) {
//		    			if (mPrf.readBoolean("Inviting:" + msg.address, false)) {  //tml*** friend invite
//							mPrf.delect("Inviting:" + msg.address);
//						}
                    } else if (msg.content.equals(Global.Hi_AddFriend2)) {  //tml*** 200280
//		    			if (mPrf.readBoolean("Inviting:" + msg.address, false)) {  //tml*** friend invite
//							mPrf.delect("Inviting:" + msg.address);
//						}
                        if (mDB != null && mDB.isOpen()) mDB.close();
                        return smslist;
                    } else if (msg.content.startsWith("[<CallFrom>]")) {  //alex*** callfrom
                        Log.i("[<CallFrom>] apple was here");
                        if (mDB != null && mDB.isOpen()) mDB.close();
                        return smslist;
                    } else if (msg.content.startsWith("here I am (")) {
                        msg.longitudeE6 = mPref.readLong("longitude", 116349386);
                        msg.latitudeE6 = mPref.readLong("latitude", 39976279);
                    }

                    msg.status = -1;
                    msg.type = 1;
                    if (items.length > 4)
                        msg.attached = Integer.parseInt(items[4]);  //TODO
                    else
                        msg.attached = 0;
                    if (msg.attached == 3) {
                        try {
                            phpIP = new NetInfo(context).longToIP(Long.valueOf(items[7], 16));
                        } catch (Exception e) {
                            if (AireJupiter.getInstance() != null) {  //tml*** china ip
                                phpIP = AireJupiter.getInstance().getIsoPhp(0, true, null);
                            } else {
                                phpIP = AireJupiter.myPhpServer_default;
                            }
                        }
                    } else if (msg.attached == 1 || msg.attached == 2 || msg.attached == 4 || msg.attached == 8) {
                        try {
                            phpIP = new NetInfo(context).longToIP(Long.valueOf(items[6], 16));
                        } catch (Exception e) {
                            if (AireJupiter.getInstance() != null) {  //tml*** china ip
                                phpIP = AireJupiter.getInstance().getIsoPhp(0, true, null);
                            } else {
                                phpIP = AireJupiter.myPhpServer_default;
                            }
                        }
                    }
                    if ((msg.attached & 1) == 1 || (msg.attached & 4) == 4) {
                        msg.att_path_aud = Global.SdcardPath_inbox + items[5];
                        if (MyUtil.checkSDCard(context)) {

                            if ((msg.attached & 1) == 1) {
                                int count = 0;
                                boolean success = false;
                                do {
                                    MyNet net = new MyNet(context);
                                    success = net.Download("vmemo/" + items[5], msg.att_path_aud, msg.attached, phpIP);
                                    if (success) break;
                                    MyUtil.Sleep(500);
                                    count++;
                                } while (count++ < 3);

                                if (!success) {
                                    if (mDB != null && mDB.isOpen())
                                        mDB.close();//alec
                                    return smslist;
                                }
                            } else if ((msg.attached & 4) == 4) {
                                // update msg.att_path_aud values
                                int interphoneType = -1;
                                if (msg.content.startsWith("(itph*"))
                                    interphoneType = Integer.parseInt(msg.content.substring(6)) - 1;
                                else
                                    msg.att_path_aud = Global.SdcardPath_inbox + "interphonevoice_" + items[5];

                                boolean success = false;

                                if (interphoneType == -1) {
                                    int count = 0;
                                    do {
                                        MyNet net = new MyNet(context);
                                        success = net.Download("vmemo/" + items[5], msg.att_path_aud, msg.attached, phpIP);
                                        if (success) break;
                                        MyUtil.Sleep(500);
                                        count++;
                                    } while (count++ < 3);

                                    if (!success) {
                                        if (mDB != null && mDB.isOpen())
                                            mDB.close();//alec
                                        return smslist;
                                    }
                                }

                                WalkieTalkieDialog.addPlayObject(idx, interphoneType, msg.time, msg.att_path_aud);

                                if (WalkieTalkieDialog.getInstance() != null) {
                                    Intent intent = new Intent();
                                    intent.setAction(Global.ACTION_PLAY_AUDIO);
                                    intent.putExtra("clear", 0);
                                    context.sendBroadcast(intent);

                                    if (mDB != null && mDB.isOpen())
                                        mDB.close();//alec
                                    return smslist;
                                }
                            }
                        } else
                            msg.content += "\n(" + context.getResources().getString(R.string.no_sdcard_receive_voice_message) + ")";
                    }

                    if ((msg.attached & 2) == 2) {
                        String iamgePath = "";
                        if (msg.attached == 3)
                            iamgePath = items[6];
                        else
                            iamgePath = items[5];
                        msg.att_path_img = Global.SdcardPath_inbox + iamgePath;
                        if (MyUtil.checkSDCard(context)) {
                            try {
                                boolean success;
                                int count = 0;
                                do {
                                    MyNet net = new MyNet(context);
                                    success = net.Download("mms/" + iamgePath, msg.att_path_img, msg.attached, phpIP);
                                    if (success) break;
                                    MyUtil.Sleep(500);
                                    count++;
                                } while (count++ < 3);
                            } catch (Exception e) {
                            }
                        } else
                            msg.content += "\n(" + context.getResources().getString(R.string.no_sdcard_receive_picture_message) + ")";
                    }

                    if (msg.attached == 8) { // file or video

                        if (msg.content.startsWith("(vdo)")) {
                            msg.content = context.getString(R.string.video) + " " + msg.content;
                        } else if (msg.content.startsWith("(fl)")) {
                            msg.content = context.getString(R.string.filememo_recv) + " " + msg.content;
                        }// zhao
                        msg.att_path_aud = "ulfiles/" + URLDecoder.decode(items[5].replace('*', '%'), "UTF-8");
                        Log.d("msg.att_path_aud:" + msg.att_path_aud);

                    }

                    boolean flag = ConversationActivity.sender != null && MyTelephony.SameNumber(ConversationActivity.sender, msg.address);
                    msg.read = (flag == true ? 1 : 0);

                    if (!(msg.attached == 4)) {
                        mDB.insertMessage(msg.address, msg.contactid, msg.time,
                                msg.read, -1, 1, "", msg.content, msg.attached, msg.att_path_aud, msg.att_path_img,
                                0, msg.longitudeE6, msg.latitudeE6, 0, msg.displayname, phpIP, msg.group_member);
                    }
                    if (!msg.content.startsWith("[<LOCATIONSHARING>]"))
                        smslist.add(msg);
                }
            } catch (Exception e) {
                Log.i("Parse1 !@#$ " + e.getMessage());
                continue;
            }
        }
        if (mDB != null && mDB.isOpen())
            mDB.close();

        return smslist;
    }

    static public ArrayList<SMS> Parse2(Context context, String data, ContactsQuery cq, AmpUserDB mADB, MyPreference mPref) {
        Log.d("进入Parse2");

        ArrayList<SMS> smslist = new ArrayList<SMS>();

        boolean locationsharing = false;

        SmsDB mDB = new SmsDB(context);
        mDB.open();
        try {
            Pattern p1 = Pattern.compile("/");
            String[] items = p1.split(data, 20);
            Pattern p2 = Pattern.compile("<Z>");
            String[] cont = p2.split(data, 15);
//    		String phpIP = AireJupiter.myPhpServer_default;
            String phpIP = null;
            if (AireJupiter.getInstance() != null) {  //tml*** china ip
                phpIP = AireJupiter.getInstance().getIsoPhp(0, true, null);
            } else {
                phpIP = AireJupiter.myPhpServer_default;
            }

            String debug = "";
            for (int i = 0; i < cont.length; i++) {  //tml*** debug
                debug = debug + " [" + i + "]" + cont[i];
            }
            Log.d("msgPARSE cont =" + debug);
            if (cont.length > 5) {
                try {
                    phpIP = new NetInfo(context).longToIP(Long.valueOf(cont[5], 16));
                } catch (Exception e) {
                    phpIP = cont[5];
                }
            }

            int idx = Integer.parseInt(items[1], 16);

            SMS msg = new SMS();
            String Sender = mADB.getAddressByIdx(idx);

            msg.address = Sender;

            if (!mADB.isFafauser(idx)) {
//    			if (mPrf.readBoolean("Inviting:" + msg.address, false)) {  //tml*** friend invite
//					mPrf.delect("Inviting:" + msg.address);
//				}
            }

            msg.contactid = cq.getContactIdByNumber(msg.address);

            if (msg.contactid > 0)
                msg.displayname = cq.getNameByContactId(msg.contactid);
            else
                msg.displayname = mADB.getNicknameByAddress(msg.address);

            if (msg.displayname == null)
                msg.displayname = context.getResources().getString(R.string.unknown_person);

            try {
                msg.content = cont[1].substring(0, cont[1].lastIndexOf("/`")); // drop rowid
                cont[1] = msg.content;
            } catch (Exception e1) {
                msg.content = cont[1];
            }
            //tml*** suv onoff alert
            boolean security_on = false;
            boolean security_off = false;
            for (int k = 0; k < securityon_strings.length; k++) {
                if (msg.content.equals(securityon_strings[k])) {
                    security_on = true;
                    break;
                }
                if (msg.content.equals(securityoff_strings[k])) {
                    security_off = true;
                    break;
                }
            }

            if (msg.content.startsWith("`[")) {//alec
                try {
                    int p = msg.content.indexOf("]\n");
                    Log.e("test parse `[ p=" + p);
                    if (p > 3) {
                        int groupid = Integer.parseInt(msg.content.substring(2, p));
                        Sender = "[<GROUP>]" + groupid;
                        msg.address = Sender;
                        msg.content = msg.content.substring(p + 2);
                        msg.displayname = mADB.getNicknameByAddress(msg.address);
                        msg.group_member = idx;

                        if (msg.content.equals(":)(Y)")) {
                            Intent it = new Intent(Global.Action_InternalCMD);
                            it.putExtra("Command", Global.CMD_JOIN_A_NEW_GROUP);
                            it.putExtra("GroupID", groupid);
                            context.sendBroadcast(it);

                            if (mDB != null && mDB.isOpen())
                                mDB.close();
                            return smslist;
                        } else if (msg.content.startsWith(":-o$_$")) {
                            Intent it = new Intent(Global.Action_InternalCMD);
                            it.putExtra("Command", Global.CMD_GROUP_ADD_NEW_MEMBER);
                            it.putExtra("GroupID", groupid);
                            try {
                                String newMember = msg.content.substring(6);
                                it.putExtra("idx", Integer.parseInt(newMember));
                            } catch (Exception e) {
                            }

                            context.sendBroadcast(it);

                            if (mDB != null && mDB.isOpen())
                                mDB.close();
                            return smslist;
                        } else if (msg.content.equals(":((Sk)")) {
                            Intent it = new Intent(Global.Action_InternalCMD);
                            it.putExtra("Command", Global.CMD_LEAVE_GROUP);
                            it.putExtra("GroupID", groupid);
                            it.putExtra("idx", msg.group_member);
                            context.sendBroadcast(it);

                            if (mDB != null && mDB.isOpen())
                                mDB.close();
                            return smslist;
                        } else {
                            if (!mADB.isFafauser(groupid + 100000000)) {
                                if (mDB != null && mDB.isOpen())
                                    mDB.close();
                                return smslist;
                            }
                        }
                    }
                } catch (Exception e1) {
                }
            }

            if (msg.content.startsWith(Global.Call_Conference)) {
                //tml*** conf-200 offset
                msg.time = Long.parseLong(items[2], 16);
                long timeOffset = mPref.readLong("confServerOffset", 0);
                long my_time = (new Date().getTime() / 1000) + timeOffset;
//				long timeDiff = msg.time -
                //Hsia:修正离线的多方通话时间戳
                long timeDiff = my_time - msg.time;
                Log.i("incConf1 time this=" + msg.time + " my=" + my_time + " diff=" + timeDiff);
                if (timeDiff <= 20) {
                    try {
                        String[] iItems = msg.content.split("\n\n");
                        String ip = MyUtil.longToIPForServer(Long.valueOf(iItems[1], 16));
                        int from = Integer.parseInt(iItems[2], 16);
                        if (AireJupiter.getInstance() != null) {
                            Log.d("voip.invitedConf1 " + ip + " " + from);
                            //xwf*** broadcast
                            boolean isBroadcast = msg.content.contains(Global.Call_Broadcast);
                            boolean isSecurity = msg.content.contains(Global.ISSECURITY);
                            boolean isNewBrost = msg.content.contains(Global.ISNEWBROST);
                            if (isSecurity) {
                                mPref.write("GuardYou", true);
                            } else {
                                mPref.write("GuardYou", false);
                            }
                            if (isNewBrost) {
                                mPref.write("pay", true);
                            } else {
                                mPref.write("pay", false);
                            }
                            if (DialerActivity.getDialer() == null) {
                                if (isBroadcast || isSecurity || isNewBrost) {
                                    mPref.write("BCAST_CONF", 0);
                                } else {
                                    mPref.write("BCAST_CONF", -1);

                                }

                            }
                            if ( ContactsOnline
                                    .getContactOnlineStatus(msg.address)==5){
                                mPref.write("pcCall",true);
                                mPref.write("pcAdress",msg.address);
                                Log.d("BREE PC客户端来电");
                            }
                            AireJupiter.getInstance().lanuchServiceYToJoinChatroom(ip, from, isBroadcast);
                        }
                    } catch (Exception e) {
                    }
                }

                if (mDB != null && mDB.isOpen()) mDB.close();
                return smslist;
            }else if(msg.content.startsWith(Global.Leave_Conference)){
                if (DialerActivity.getDialer() != null) {
                    Intent itcall = new Intent(Global.
                            Leave_Conference);
                    context.sendBroadcast(itcall);
                }
                return smslist;
            }
//			// bree
//			else if (msg.content.startsWith(Global.Call_Conference_Mute)) {
//				if (DialerActivity.getDialer() != null)
//					DialerActivity.getDialer().setMute(true);
//				return smslist;
//			} else if (msg.content.startsWith(Global.Call_Conference_Speak)) {
//				if (DialerActivity.getDialer() != null)
//				DialerActivity.getDialer().setMute(false);
//				return smslist;
//			}
            else if (msg.content.startsWith(Global.Master_Parse)) {

                if (msg.content.endsWith("test")) {
                    int vers = mPref.readInt("versionCode", 0);
                    if (AireJupiter.getInstance() != null
                            && AireJupiter.getInstance().tcpSocket.isLogged(false)) {
                        AireJupiter.getInstance().tcpSocket.send(msg.address, "echo test "
                                + vers + SettingActivity.vlib, 0, null, null, 0, null);
                    }
//    			} else if (msg.content.startsWith(Global.Call_Conference_Switch)) {  //tml*** switch conf, 1a
//    				try {
//    					Log.e("--------- SWITCH CALL --------- 200 switch");
//    					String [] iItems = msg.content.split("\n\n");
//    					String ip = MyUtil.longToIPForServer(Long.valueOf(iItems[1], 16));
//    					int from = Integer.parseInt(iItems[2], 16);
//    					if (AireJupiter.getInstance() != null) {
//    						Log.d("voip.switchConf " + ip + " " + from);
//    						AireJupiter.getInstance().setSwitchCall(true, "200conf in");
//    						AireJupiter.getInstance().lanuchServiceYToJoinChatroom(ip, from);
//    					}
//    				} catch (Exception e) {
//    				}
                }
                if (mDB != null && mDB.isOpen()) mDB.close();
                return smslist;
            } else if (msg.content.startsWith("[<LOCATIONSHARING>]")) {
                Intent it = new Intent(Global.Action_InternalCMD);
                it.putExtra("Command", Global.CMD_LOCATION_SHARING);
                it.putExtra("Sender", Sender);
                context.sendBroadcast(it);
                msg.time = Long.parseLong(items[2], 16) * 1000;
            } else if (msg.content.startsWith("[<NEWPHOTO>]")) {
                String localfile = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
                if (AireJupiter.getInstance().downloadPhoto216(idx, localfile)) {
                    //delete old bigger one
                    new File(Global.SdcardPath_inbox + "photo_" + idx + "b.jpg").delete();

                    //alec: download big one as well
                    localfile = Global.SdcardPath_inbox + "photo_" + idx + "b.jpg";
                    MyNet net = new MyNet(context);
                    net.Download("profiles/photo_" + idx + ".jpg", localfile, AireJupiter.myPhpServer_default2A);

                    String address = mADB.getAddressByIdx(idx);
                    Intent intent = new Intent();
                    intent.putExtra("address", address);
                    intent.putExtra("type", "update");
                    intent.putExtra("idx", idx);
                    intent.setAction(Global.Action_Refresh_Gallery);
                    context.sendBroadcast(intent);
                }
                if (mDB != null && mDB.isOpen())
                    mDB.close();
                return smslist;
            } else if (msg.content.startsWith("I am your GUARD")) {
//				for (int j=0;j<5;j++)
//				{
//					String addr=mPrf.read("Suvei"+j);
//					if (!addr.equals(msg.address))
//					{
//						Log.i("tml I am your GUARD2> " + msg.address);
//						mPrf.write("Suvei"+j, msg.address);
//						//tml*** beta ui, security
//					    Intent intent = new Intent();
//				        intent.setAction(Global.Action_Refresh_Gallery);
//				        context.sendBroadcast(intent);
//						break;
//					}
//				}
//
//				if(mDB!=null && mDB.isOpen())
//					mDB.close();
//				return smslist;
                //tml*** multi suvei
                Log.i("tml I am your GUARD2> " + msg.address);
                int maxSuvei = Global.MAX_SUVS;
                boolean suveiAdd = false;
                boolean suveiFull = true;
                boolean lastone = false;

                String last = msg.content.substring(msg.content.length() - 4, msg.content.length());
                if (last.equals("LAST")) lastone = true;  //last contact, confirm del

                for (int j = 0; j < maxSuvei; j++) {
                    String addr = mPref.read("Suvei" + j);
                    Log.i("tmlsuv see.Suvei[" + j + "]=" + addr);
                    if (addr.equals("")) {
                        Log.i("tmlsuv add.Suvei[" + j + "]=" + msg.address);
                        mPref.write("Suvei" + j, msg.address);
                        suveiAdd = true;
                        break;
                    } else if (addr.equals(msg.address)) {
                        Log.i("tmlsuv already exists!");
                        suveiAdd = false;
                        suveiFull = false;
                        break;
                    }
                }

                if (!suveiAdd && suveiFull) {  //full
                    Log.i("tmlsuv suvei full!");
                    if (AireJupiter.getInstance() != null
                            && AireJupiter.getInstance().tcpSocket.isLogged(false)) {
                        AireJupiter.getInstance().tcpSocket
                                .send(msg.address, "GUARD list IS FULL", 0, null, null, 0, null);
                    }

                    String warning = "";
                    if (lastone) {  //full, auto confirm del
                        if (AireJupiter.getInstance() != null
                                && AireJupiter.getInstance().tcpSocket.isLogged(false)) {
                            AireJupiter.getInstance().tcpSocket
                                    .send(msg.address, "REVOKE my GUARD access FINAL", 0, null, null, 0, null);
                        }
                        warning = "\n" + context.getResources().getString(R.string.suvlastonegone);
                    }

                    msg.content = context.getResources().getString(R.string.home_guard) + "!\n"
                            + context.getResources().getString(R.string.maxsuvei1)
                            + " (" + maxSuvei + "). "
                            + context.getResources().getString(R.string.maxsuvei2)
                            + warning;
                } else if (!suveiAdd) {  //no add
                    if (mDB != null && mDB.isOpen()) mDB.close();
                    return smslist;
                } else if (suveiAdd) {  //add
                    Intent intent = new Intent();
                    intent.setAction(Global.Action_Refresh_Gallery);
                    context.sendBroadcast(intent);
                    if (lastone) {  //last contact, warning msg
                        mPref.write("SuvDelWARN" + msg.address, true);
                        Log.i("SuvDelWARN " + msg.address);
                        msg.content = context.getResources().getString(R.string.home_guard) + "!\n"
                                + context.getResources().getString(R.string.suvdelwarming);
                    } else {
                        if (mDB != null && mDB.isOpen()) mDB.close();
                        return smslist;
                    }
                }
                //***tml
            } else if (msg.content.startsWith("I am NOT your GUARD")) {
//        		for (int j=0;j<5;j++)
//				{
//					String addr=mPrf.read("Suvei"+j);
//					if (addr.equals(msg.address))
//					{
//						Log.i("tml I am NOT your GUARD2> " + msg.address);
//						mPrf.delect("Suvei"+j);
//						//tml*** beta ui, security
//					    Intent intent = new Intent();
//				        intent.setAction(Global.Action_Refresh_Gallery);
//				        context.sendBroadcast(intent);
//						break;
//					}
//				}
//
//				if(mDB!=null && mDB.isOpen())
//					mDB.close();
//				return smslist;
                Log.i("tml I am NOT your GUARD2> " + msg.address);
                int maxSuvei = Global.MAX_SUVS;
                boolean suveiDel = false;
                for (int j = 0; j < maxSuvei; j++) {
                    String addr = mPref.read("Suvei" + j);
//					Log.i("tmlsuv see.Suvei[" + j + "]=" + addr);
                    if (addr.equals(msg.address)) {
                        mPref.delect("Suvei" + j);
                        suveiDel = true;
                        Intent intent = new Intent();
                        intent.setAction(Global.Action_Refresh_Gallery);
                        context.sendBroadcast(intent);
//						Log.i("tmlsuv del.Suvei[" + j + "]=" + msg.address);
                        break;
                    }
                }

                if (mDB != null && mDB.isOpen()) mDB.close();
                return smslist;
            } else if (security_on) {  //tml*** suv onoff alert
                int maxSuvei = Global.MAX_SUVS;

                for (int j = 0; j < maxSuvei; j++) {
                    String addr = mPref.read("Suvei" + j);
                    if (addr.equals(msg.address)) {
                        Log.i("security ON notified from " + msg.address);
                        mPref.write("SuveiON" + j, msg.address);
                        Intent intent = new Intent();
                        intent.setAction(Global.Action_Refresh_Gallery);
                        context.sendBroadcast(intent);
                    }
                }
            } else if (security_off) {  //tml*** suv onoff alert
                int maxSuvei = Global.MAX_SUVS;

                for (int j = 0; j < maxSuvei; j++) {
                    String addr = mPref.read("Suvei" + j);
                    if (addr.equals(msg.address)) {
                        Log.i("security OFF notified from " + msg.address);
                        mPref.delect("SuveiON" + j);
                        Intent intent = new Intent();
                        intent.setAction(Global.Action_Refresh_Gallery);
                        context.sendBroadcast(intent);
                    }
                }
            } else if (msg.content.equals(Global.Hi_AddFriend1)) {
//    			if (mPrf.readBoolean("Inviting:" + msg.address, false)) {  //tml*** friend invite
//					mPrf.delect("Inviting:" + msg.address);
//				}
            } else if (msg.content.equals(Global.Hi_AddFriend2)) {  //tml*** 200280
//    			if (mPrf.readBoolean("Inviting:" + msg.address, false)) {  //tml*** friend invite
//					mPrf.delect("Inviting:" + msg.address);
//				}
                if (mDB != null && mDB.isOpen()) mDB.close();
                return smslist;
            } else if (msg.content.startsWith("[<CallFrom>]")) {  //alex*** callfrom
                Log.i("[<CallFrom>] apple was here");
                if (mDB != null && mDB.isOpen()) mDB.close();
                return smslist;
            } else if (msg.content.startsWith("[<NEWMOOD>]")) {
                String newMood = msg.content.substring(11);
                int index = newMood.lastIndexOf("/");
                if (index != -1) newMood = newMood.substring(0, index);
                mADB.updateMoodByUID(idx, newMood);
                if (mDB != null && mDB.isOpen())
                    mDB.close();
                return smslist;
            } else {

                msg.time = Long.parseLong(items[2], 16) * 1000;

                if (msg.content.startsWith("[<MISSEDREMIND>]")) {
                    int n = Integer.parseInt(cont[1].substring(16));
                    msg.content = String.format(context.getResources().getString(R.string.missed_call_remind_1), n);
                    msg.content += " " + msg.displayname;
                    String tFormat = DateUtils.formatDateTime(context, msg.time - 60000,
                            DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE);
                    String description2 = String.format(context.getResources().getString(R.string.missed_call_remind_2), tFormat);
                    msg.content += description2;
                } else if (msg.content.startsWith("[<NEWUSERJOINS>]")) {
                    int uid = Integer.parseInt(cont[1].substring(16), 16);
                    msg.content = context.getResources().getString(R.string.auto_notify_joins);
                    mADB.insertUser(msg.address, uid);
                    msg.contactid = cq.getContactIdByNumber(msg.address);
                    ContactsOnline.setContactOnlineStatus(msg.address, 2);
                } else if (msg.content.startsWith("[<AGREESHARE>]")) {
                    try {
                        msg.latitudeE6 = Long.parseLong(cont[1].split(",")[3]);
                        msg.longitudeE6 = Long.parseLong(cont[1].split(",")[4]);
                    } catch (Exception e) {
                        msg.longitudeE6 = 116349386;
                        msg.latitudeE6 = 39976279;
                    }
                    String globalNumber = MyTelephony.attachPrefix(context, msg.address);
                    try {
                        int relation = Integer.valueOf(cont[1].split(",")[2]);
                        long timeout = MyUtil.getSharingTimeout(relation);
                        mPref.writeLong(globalNumber, timeout);
                        mPref.writeLong("SpeedupMapMonitor", timeout);
                    } catch (Exception e) {
                    }
                } else if (msg.content.startsWith("here I am (")) {
                    msg.longitudeE6 = mPref.readLong("longitude", 116349386);
                    msg.latitudeE6 = mPref.readLong("latitude", 39976279);
                } else if (msg.content.startsWith("[EMOTION")) {
                    int index = 1;
                    try {
                        index = Integer.valueOf(msg.content.substring(8, msg.content.length() - 1));
                    } catch (Exception e) {
                    }
                    msg.content = "[EMOTION" + index + "]";
                } else if (msg.content.startsWith("(iPh)")) {
                    if (cont.length <= 2) {
                        smslist.add(msg);
                        if (mDB != null && mDB.isOpen())
                            mDB.close();
                        return smslist;
                    }
                }
            }

            msg.status = -1;
            msg.type = 1;
            if (cont.length > 2) {
                try {
                    msg.attached = Integer.parseInt(cont[2]);
                } catch (Exception e) {
                    msg.attached = 0;
                }

                if ((msg.attached & 1) == 1 || (msg.attached & 4) == 4) {
                    msg.att_path_aud = Global.SdcardPath_inbox + cont[3];

                    if (!MyUtil.checkSDCard(context)) {
                        msg.content += "\n(" + context.getResources().getString(R.string.no_sdcard_receive_voice_message) + ")";
                    } else {
                        if ((msg.attached & 1) == 1) {
                            int count = 0;
                            boolean success = false;
                            do {
                                MyNet net = new MyNet(context);
                                success = net.Download("vmemo/" + cont[3], msg.att_path_aud, msg.attached, phpIP);
                                if (success) break;
                                MyUtil.Sleep(500);
                            } while (count++ < 3);

                            if (!success) {
                                Log.e("*** Download failed ****");
                                //forget this message, because we download failed
                                if (mDB != null && mDB.isOpen())
                                    mDB.close();//alec
                                return smslist;
                            }
                        } else if ((msg.attached & 4) == 4) {
                            // update msg.att_path_aud values
                            int interphoneType = -1;
                            if (msg.content.startsWith("(itph*"))
                                interphoneType = Integer.parseInt(msg.content.substring(6)) - 1;
                            else
                                msg.att_path_aud = Global.SdcardPath_inbox + "interphonevoice_" + cont[3];

                            if (interphoneType == -1) {
                                boolean success = false;
                                int count = 0;
                                do {
                                    MyNet net = new MyNet(context);
                                    success = net.Download("vmemo/" + cont[3], msg.att_path_aud, msg.attached, phpIP);
                                    if (success) break;
                                    MyUtil.Sleep(500);
                                    count++;
                                } while (count++ < 3);

                                if (!success) {
                                    Log.e("*** Download failed ****");
                                    //forget this message, because we download failed
                                    if (mDB != null && mDB.isOpen())
                                        mDB.close();//alec
                                    return smslist;
                                }
                            }

                            WalkieTalkieDialog.addPlayObject(idx, interphoneType, msg.time, msg.att_path_aud);

                            if (WalkieTalkieDialog.getInstance() != null) {
                                Intent intent = new Intent();
                                intent.setAction(Global.ACTION_PLAY_AUDIO);
                                intent.putExtra("clear", 0);
                                context.sendBroadcast(intent);

                                Bundle b = new Bundle();
                                b.putString("Address", msg.address);
                                b.putLong("Contact_id", msg.contactid);
                                b.putString("audioPath", msg.att_path_aud);
                                b.putInt("interphoneType", interphoneType);

                                WalkieTalkieDialog.getInstance().refresh(b);

                                if (mDB != null && mDB.isOpen())
                                    mDB.close();//alec
                                return smslist;
                            }
                        }
                    }
                }
                if ((msg.attached & 2) == 2) {
                    if (!cont[4].startsWith(".jpg"))
                        cont[4] = cont[4].substring(0, cont[4].lastIndexOf(".jpg") + 4);
                    msg.att_path_img = Global.SdcardPath_inbox + cont[4];
                    if (!MyUtil.checkSDCard(context)) {
                        msg.content += "\n(" + context.getResources().getString(R.string.no_sdcard_receive_picture_message) + ")";
                    } else {
                        int count = 0;
                        boolean success = false;
                        do {
                            MyNet net = new MyNet(context);
                            success = net.Download("mms/" + cont[4], msg.att_path_img, msg.attached, phpIP);
                            if (success) break;
                            MyUtil.Sleep(500);
                        } while (count++ < 3);
                    }
                }
                if (msg.attached == 8) { // file
                    msg.att_path_aud = Global.SdcardPath_inbox
                            + cont[3].substring(0, cont[3].lastIndexOf("_"));
                    Log.d("解析数据进入文件下载方面: 内容:" + msg.content+",下载地址:"+msg.att_path_aud);
                    // msg.content =
                    // context.getString(R.string.filememo_recv)+" "+msg.content;
                    // alec:
                    String filename = msg.att_path_aud
                            .substring(msg.att_path_aud.lastIndexOf("/") + 1);
                    Log.d("filename: "+filename);
                    String part2 = msg.content.substring(5);
//                    msg.content = "(fl)  " + filename + "  " + part2;
                    msg.content = "(fl)  "  +  part2;

                    Log.d("收到文件  新内容:" + msg.content + " ,msg.att_path_aud:" + msg.att_path_aud + " 结果");

                    if (msg.content.startsWith("(vdo)")) {
                        msg.content = context.getString(R.string.video) + " " + msg.content;
                    } else if (msg.content.startsWith("(fl)")) {
                        msg.content = context.getString(R.string.filememo_recv) + " " + msg.content;
                    }
                    msg.att_path_aud = "ulfiles/" + cont[3];

                    Log.d("收到文件  老内容:" + msg.content + " ,msg.att_path_aud:" + msg.att_path_aud + " 结果");
                } else if (msg.attached == 16) { //alec: MIXED

                    msg.att_path_img = Global.SdcardPath_inbox + cont[4];
                    Log.i("msg.att_path_img=" + msg.att_path_img);
                    if (cont.length > 5)
                        phpIP = cont[5];
                }
            }

            boolean flag = ConversationActivity.sender != null && MyTelephony.SameNumber(ConversationActivity.sender, msg.address);
            msg.read = (flag == true ? 1 : 0);

            if (msg.content.startsWith("[<LOCATIONSHARING>]")) {
                locationsharing = true;
                msg.content = String.format(context.getResources().getString(R.string.hasaskshare), msg.displayname);
            }

            msg.obligate1 = phpIP;
            if (!(msg.attached == 4)) {
                // TODO: 2016/4/5 之后删除
//				android.util.Log.d("Socket210", "msg.address  "+msg.address
//				+"  msg.contactid  "+msg.contactid
//				+"  msg.time  "+msg.time
//				+"  msg.read  "+msg.read
//				+"  msg.status  "+msg.status
//				+"  msg.type  "+msg.type
//				+"  msg.subject  为空串"
//				+"  msg.content  "+msg.content
//				+"  msg.attached  "+msg.attached
//				+"  msg.att_path_aud  "+msg.att_path_aud
//				+"  msg.att_path_img  "+msg.att_path_img
//				+"  msg.longitudeE6  "+msg.longitudeE6
//				+"  msg.latitudeE6  "+msg.latitudeE6
//				+"  msg.displayname  "+msg.displayname
//				+"  msg.obligate1  "+msg.obligate1
//				+"  msg.group_member  "+msg.group_member);
                mDB.insertMessage(msg.address, msg.contactid, msg.time,
                        msg.read, -1, 1, "", msg.content, msg.attached, msg.att_path_aud, msg.att_path_img,
                        0, msg.longitudeE6, msg.latitudeE6, 0, msg.displayname, msg.obligate1, msg.group_member);
            }
            if (!locationsharing) //dont popup dialog
                smslist.add(msg);

            //tml*** chatview
            if (AireVenus.instance() != null && AireVenus.callstate_AV != null) {
                VoipCore lVoipCore = AireVenus.instance().getVoipCore();
                String inCallAddress = lVoipCore.getRemoteAddress().getUserName();
                if (inCallAddress != null && inCallAddress.equals(msg.address)) {
                    int unread = mDB.getUnreadCountByAddress(msg.address);
                    if (unread > 0) {
                        Intent itcall = new Intent(Global.MSG_UNREAD_YES);
                        context.sendBroadcast(itcall);
                    }
                } else {
                    Log.w("inMsg NOT from inCall");
                }
            }
            //***tml
        } catch (Exception e) {
            Log.e("Parse2 !@#$ " + e.getMessage());
        }

        if (mDB != null && mDB.isOpen())
            mDB.close();
        return smslist;
    }

    // TODO: 2016/4/1 解析群组消息 已经在子线程中
    // TODO: 2016/4/5 下载语音/图片/文件
    public static ArrayList<SMS> ParseGroupSMS(Context context, String data, ContactsQuery cq, AmpUserDB mADB, MyPreference mPref) {
        Log.d("进入ParseGroupSMS");

        int index = data.indexOf("{");
        String json = data.substring(index);//body {"at":"8","cmd":"","ct":"(fl)345.07 KB","path":"pull-to-refreshview-demo.apk_1459907412861","url":"http://42.121.54.216/onair/ulfiles/pull-to-refreshview-demo.apk_1459907412861"}
        String command = data.substring(0, index);
        String[] cont = command.split("/");//command 860/129501/133ac1/9e56/1b/57046c53/

        ArrayList<SMS> smslist = new ArrayList<SMS>();
        SmsDB mDB = new SmsDB(context);
        mDB.open();

        if (gson == null) {
            gson = new Gson();
        }

        // TODO: 2016/4/28 根据不同的type,解析不同的content
        SMS msg = new SMS();
        GroupMsg groupMsg = new GroupMsg();
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (TextUtils.isEmpty(jsonObject.getString("cmd"))) {
                groupMsg = gson.fromJson(json, GroupMsg.class);
//				msg.content = groupMsg.getCt();
                android.util.Log.d("ParseSmsLine", "jinliale");
            } else {
                GroupUpdateMsg groupUpdateMsg = gson.fromJson(json, GroupUpdateMsg.class);
                android.util.Log.d("ParseSmsLine", "groupUpdateMsg:" + groupUpdateMsg);
                String content = parseType(context, groupUpdateMsg.getCt().getType(), groupUpdateMsg.getCt().getNicknames(), groupUpdateMsg.getCt().getIdxs());
                //设置值
                android.util.Log.d("ParseSmsLine", content);
                groupMsg.setCt(content);
                groupMsg.setAt(groupUpdateMsg.getAt());
                groupMsg.setPath(groupUpdateMsg.getPath());
                groupMsg.setUrl(groupUpdateMsg.getUrl());
                groupMsg.setCmd(groupUpdateMsg.getCmd());
            }
        } catch (JSONException e) {
            Log.d("json 解析出错");
            e.printStackTrace();
        }


        String phpIP = null;
        if (AireJupiter.getInstance() != null) {  //tml*** china ip
            phpIP = AireJupiter.getInstance().getIsoPhp(0, true, null);
        } else {
            phpIP = AireJupiter.myPhpServer_default;
        }

        String debug = "";
        for (int i = 0; i < cont.length; i++) {  //tml*** debug
            debug = debug + " [" + i + "]" + cont[i];
        }
        Log.d("GroupSMS msgPARSE cont =" + debug);


        int idx = Integer.parseInt(cont[1], 16);
        int groupid = Integer.parseInt(cont[3], 16);
        msg.address = "[<GROUP>]" + groupid;
        Log.d("860Socket  address --- " + msg.address);
        msg.displayname = mADB.getNicknameByAddress(msg.address);
        Log.d("ParseSmsLine  displayname --- " + msg.displayname);
        msg.group_member = idx;
        Log.d("860Socket  group_member --- " + msg.group_member);
        msg.time = Long.parseLong(cont[5], 16) * 1000;
        Log.d("860Socket  time--- " + msg.time);
        msg.contactid = cq.getContactIdByNumber(msg.address);
        Log.d("860Socket  contactid --- " + msg.contactid);


        if (msg.contactid > 0)
            msg.displayname = cq.getNameByContactId(msg.contactid);
        else
            msg.displayname = mADB.getNicknameByAddress(msg.address);

        if (msg.displayname == null)
            msg.displayname = context.getResources().getString(R.string.unknown_person);

        //赋值内容
        msg.content = groupMsg.getCt();
        Log.d("860Socket  content --- " + msg.content);

        //tml*** suv onoff alert
        boolean security_on = false;
        boolean security_off = false;
        for (int k = 0; k < securityon_strings.length; k++) {
            if (msg.content.equals(securityon_strings[k])) {
                security_on = true;
                break;
            }
            if (msg.content.equals(securityoff_strings[k])) {
                security_off = true;
                break;
            }
        }

        msg.status = -1;
        msg.type = 1;
        msg.attached = Integer.parseInt(groupMsg.getAt());
        switch (groupMsg.getAt()) {
            case "0":
                // TODO: 2016/4/1 群文字,表情,定位
                if (groupMsg.getCt().startsWith("here I am (")) {
                    //here I am (39.97628,116.34938)
                    int start = groupMsg.getCt().indexOf("(");
                    int end = groupMsg.getCt().indexOf(")");
                    String location = groupMsg.getCt().substring(start + 1, end);
                    String[] split = location.split(",");
                    Log.d("860Socket  longitudeE6" + split[1] + "     latitudeE6" + split[0]);
                    //去除空格
                    msg.longitudeE6 = Long.parseLong(split[1].replace(".", "").trim());
                    msg.latitudeE6 = Long.parseLong(split[0].replace(".", "").trim());
                } else {
                    msg.content = groupMsg.getCt();
                }
                break;
            case "1":
                // TODO: 2016/4/1 语音
                Log.d("860Socket  收到语音");
                msg.att_path_aud = Global.SdcardPath_inbox + groupMsg.getPath();
                if (!MyUtil.checkSDCard(context)) {
                    msg.content += "\n(" + context.getResources().getString(R.string.no_sdcard_receive_voice_message) + ")";
                } else {
                    int count = 0;
                    boolean success = false;
                    do {
                        //{"at":"8","cmd":"","ct":"(fl)345.07 KB","path":"pull-to-refreshview-demo.apk_1459907412861","url":"http://42.121.54.216/onair/ulfiles/pull-to-refreshview-demo.apk_1459907412861"}
                        MyNet net = new MyNet(context);
//						success=net.Download("vmemo/"+groupMsg.getUrl(), msg.att_path_aud,1,phpIP);
                        success = net.anyDownload(groupMsg.getUrl(), msg.att_path_aud);
                        if (success) break;
                        MyUtil.Sleep(500);
                    } while (count++ < 3);

                    if (!success) {
                        Log.e("*** Download failed ****");
                        //forget this message, because we download failed
                        if (mDB != null && mDB.isOpen())
                            mDB.close();//alec
                        return smslist;
                    }

                }
                break;
            case "2":
                // TODO: 2016/4/1 图片
                Log.d("860Socket  收到图片");
                //未知作用
//				if(!cont[4].startsWith(".jpg"))
//					cont[4] = cont[4].substring(0, cont[4].lastIndexOf(".jpg")+4);
                msg.att_path_img = Global.SdcardPath_inbox + groupMsg.getPath();

                if (!MyUtil.checkSDCard(context)) {
                    msg.content += "\n(" + context.getResources().getString(R.string.no_sdcard_receive_picture_message) + ")";
                } else {
                    int count = 0;
                    boolean success = false;
                    do {
                        MyNet net = new MyNet(context);
//						success=net.Download("mms/"+groupMsg.getUrl(), msg.att_path_img,2,phpIP);
                        success = net.anyDownload(groupMsg.getUrl(), msg.att_path_img);
                        if (success) break;
                        MyUtil.Sleep(500);
                    } while (count++ < 3);
                }
                break;
            case "8":
                Log.d("860Socket  收到文件");
                // TODO: 2016/4/1 文件
                /**
                 * IOS:
                 *
                 860/f73d6/129501/9e5f/5b/57046dbb/{
                 "at" : 8,
                 "ct" : "(fl)266KB\nAndroidChina_v1.0.apk",
                 "path" : "AndroidChina_v1.0.apk_1459924262904",
                 "url" : "http:\/\/115.29.171.94\/onair\/ulfiles\/AndroidChina_v1.0.apk_1459924262904",
                 "cmd" : ""


                 Android
                 860/129501/133ac1/9e56/1b/57046c53/{
                 "at":"8",
                 "cmd":"",
                 "ct":"(fl)345.07 KB",
                 "path":"pull-to-refreshview-demo.apk_1459907412861",
                 "url":"http://42.121.54.216/onair/ulfiles/pull-to-refreshview-demo.apk_1459907412861"}
                 }
//                 */
//                if (groupMsg.getCt().startsWith("(vdo)")) {
//                    // TODO: 2016/4/6  以为要与ios兼容,传递数据不兼容,所以暂时做此操作
//                    msg.content = context.getString(R.string.video) + " " + groupMsg.getCt().substring(0, groupMsg.getCt().indexOf("B") + 1);//(fl)693KB
////					msg.content = context.getString(R.string.video)+" "+groupMsg.getCt();
//                } else if (groupMsg.getCt().startsWith("(fl)")) {
//                    msg.content = context.getString(R.string.filememo_recv) + " " + groupMsg.getCt().substring(0, groupMsg.getCt().indexOf("B") + 1);//(fl)693KB
////					msg.content = context.getString(R.string.filememo_recv)+" "+groupMsg.getCt();
//                }
                String part2 = groupMsg.getCt().substring(5,groupMsg.getCt().indexOf("B") + 1);
                msg.content = "(fl)  " +  part2;
                Log.d("收到文件  新内容:"+msg.content+" ,msg.att_path_aud:"+msg.att_path_aud+" 结果");

                if (msg.content.startsWith("(vdo)")) {
                    msg.content = context.getString(R.string.video) + " " + msg.content;
                } else if (msg.content.startsWith("(fl)")) {
                    msg.content = context.getString(R.string.filememo_recv) + " " + msg.content;
                }

                // TODO: 2016/4/6 jack 不喜欢这样 编程
                //暂时解决方案 jack 下载时对话框弹出显示文字
                msg.att_path_aud = "ulfiles/" + groupMsg.getPath();
                //真是下载路径
                msg.att_path_img = groupMsg.getUrl();
                break;
        }
        boolean flag = ConversationActivity.sender != null && MyTelephony.SameNumber(ConversationActivity.sender, msg.address);
        msg.read = (flag == true ? 1 : 0);

        msg.obligate1 = phpIP;
        // TODO: 2016/4/5 之后删除
        Log.d("860Socket  msg.address  " + msg.address
                + "  msg.contactid  " + msg.contactid
                + "  msg.time  " + msg.time
                + "  msg.read  " + msg.read
                + "  msg.status  " + msg.status
                + "  msg.type  " + msg.type
                + "  msg.subject  为空串"
                + "  msg.content  " + msg.content
                + "  msg.attached  " + Integer.parseInt(groupMsg.getAt())
                + "  msg.att_path_aud  " + msg.att_path_aud
                + "  msg.att_path_img  " + msg.att_path_img
                + "  msg.longitudeE6  " + msg.longitudeE6
                + "  msg.latitudeE6  " + msg.latitudeE6
                + "  msg.displayname  " + msg.displayname
                + "  msg.obligate1  " + msg.obligate1
                + "  msg.group_member  " + msg.group_member);

        if (!(groupMsg.getAt().equals("4"))) {
            mDB.insertMessage(msg.address, msg.contactid, msg.time,
                    msg.read, -1, 1, "", msg.content, Integer.parseInt(groupMsg.getAt()), msg.att_path_aud, msg.att_path_img,
                    0, msg.longitudeE6, msg.latitudeE6, 0, msg.displayname, msg.obligate1, msg.group_member);
        }
//		if(!locationsharing) //dont popup dialog
        Log.d("860Socket  将信息加入信息集合");
        smslist.add(msg);

        if (AireVenus.instance() != null && AireVenus.callstate_AV != null) {
            VoipCore lVoipCore = AireVenus.instance().getVoipCore();
            String inCallAddress = lVoipCore.getRemoteAddress().getUserName();
            if (inCallAddress != null && inCallAddress.equals(msg.address)) {
                int unread = mDB.getUnreadCountByAddress(msg.address);
                if (unread > 0) {
                    Intent itcall = new Intent(Global.MSG_UNREAD_YES);
                    context.sendBroadcast(itcall);
                }
            } else {
                Log.w("inMsg NOT from inCall");
            }
        }

        if (mDB != null && mDB.isOpen())
            mDB.close();
        return smslist;
    }

    private static String parseType(Context context, String type, String nicknames, String idxs) {
        String str = null;
        switch (type) {
            case "Group_Remove":
                str = String.format(context.getResources().getString(R.string.group_removed_members), nicknames);
                Log.d("Group_Remove " + str);
                break;
            case "Group_Add":
                String[] args1 = nicknames.split("/");
                str = String.format(context.getResources().getString(R.string.group_invite_new_members), args1[0], args1[1]);
                Log.d("Group_Add  " + str);
                break;
            case "Group_Left":
                str = String.format(context.getResources().getString(R.string.group_member_left), nicknames);
                Log.d("Group_Left  " + str);
                break;
            case "Group_Switch":
                str = String.format(context.getResources().getString(R.string.group_creater_changed), nicknames);

                Log.d("Group_Switch");
                break;
            case "Group_Photo":
                str = String.format(context.getResources().getString(R.string.group_photo_changed), nicknames);
                Log.d("Group_Photo");
                break;
            case "Group_Name":
                String[] args2 = nicknames.split("/");
                str = String.format(context.getResources().getString(R.string.group_name_changed), args2[0], args2[1]);

                Log.d("Group_Name");
                break;
        }
        return str;
    }
}

