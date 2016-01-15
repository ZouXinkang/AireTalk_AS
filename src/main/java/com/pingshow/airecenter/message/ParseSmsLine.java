package com.pingshow.airecenter.message;

import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;

import com.pingshow.airecenter.AireApp;
import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.ConversationActivity;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MainActivity;
import com.pingshow.airecenter.MakeCall;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.SMS;
import com.pingshow.airecenter.SettingPage;
import com.pingshow.airecenter.cons.Key;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.RelatedUserDB;
import com.pingshow.airecenter.db.SmsDB;
import com.pingshow.network.MyNet;
import com.pingshow.network.NetInfo;
import com.pingshow.util.MCrypt;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;
import com.pingshow.voip.VideoCallActivity;
import com.pingshow.voip.core.VoipCore;

public class ParseSmsLine {
	static int idx;
	static String address;
	static public ArrayList<SMS> unknownList = new ArrayList<SMS>();
	static public ArrayList<String> ridList = new ArrayList<String>();

	static public ArrayList<SMS> Parse(Context context, String data,
			ContactsQuery cq, AmpUserDB mADB, RelatedUserDB mRDB,
			MyPreference mPref) {
		ArrayList<SMS> smslist = new ArrayList<SMS>();

		// Log.w("data:"+data);
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
					if (items[0].length() == 0
							|| mADB.isUserBlocked(items[0]) == 1)
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
						}
						continue;
					}
					// tml*** rowid check
					// String rowid = "0";
					// boolean isExist = false;
					// try {
					// if (items.length > 0) {
					// String tmp = items[items.length - 1];
					// if (tmp.startsWith("`"))
					// rowid = tmp.substring(1);
					// }
					// } catch (Exception e) {}
					// if ("0".equals(rowid)) {
					// isExist = false;
					// } else {
					// String ridItem = items[1] + ":" + rowid;
					// if (ridList.size() > 0) {
					// for (int k = 0; k < ridList.size(); k++) {
					// if (ridList.get(k).equals(ridItem)) {
					// isExist = true;
					// break;
					// }
					// }
					// }
					//
					// if (!isExist) {
					// ridList.add(ridItem);
					// }
					// }
					// if (isExist) {
					// Log.e("msgX isExist");
					// continue;
					// }
					// ***tml

					// String phpIP = AireJupiter.myPhpServer_default;
					String phpIP = null;
					if (AireJupiter.getInstance() != null) { // tml*** china ip
						phpIP = AireJupiter.getInstance().getIsoPhp(0, true,
								null);
					} else {
						phpIP = AireJupiter.myPhpServer_default;
					}
					SMS msg = new SMS();

					items[3] = URLDecoder.decode(items[3].replace('*', '%'),
							"UTF-8");

					msg.address = items[0];
					msg.content = items[3];// alec
					msg.time = Long.parseLong(items[2], 16) * 1000;
					msg.contactid = cq.getContactIdByNumber(msg.address);

					long checkVcurTime = new Date().getTime();
					if (msg.time > checkVcurTime) {
						msg.time = checkVcurTime;
					}

					if (msg.contactid > 0)
						msg.displayname = cq.getNameByContactId(msg.contactid);
					else
						msg.displayname = mADB
								.getNicknameByAddress(msg.address);

					if (msg.displayname == null)
						msg.displayname = context.getResources().getString(
								R.string.unknown_person);

					if (msg.content.startsWith("`[")) {// alec
						try {
							int c = msg.content.indexOf("]\n");
							int groupid = Integer.parseInt(msg.content
									.substring(2, c));
							msg.address = "[<GROUP>]" + groupid;
							msg.content = msg.content.substring(c + 2);
							msg.displayname = mADB
									.getNicknameByAddress(msg.address);
							msg.group_member = idx;

							if (msg.content.equals(":)(Y)")) {
								Intent it = new Intent(
										Global.Action_InternalCMD);
								it.putExtra("Command",
										Global.CMD_JOIN_A_NEW_GROUP);
								it.putExtra("GroupID", groupid);
								context.sendBroadcast(it);
								continue;
							} else if (msg.content.startsWith(":-o$_$")) {
								Intent it = new Intent(
										Global.Action_InternalCMD);
								it.putExtra("Command",
										Global.CMD_GROUP_ADD_NEW_MEMBER);
								it.putExtra("GroupID", groupid);
								try {
									String newMember = msg.content.substring(6);
									it.putExtra("idx",
											Integer.parseInt(newMember));
								} catch (Exception e) {
								}

								context.sendBroadcast(it);
								continue;
							} else if (msg.content.equals(":((Sk)")) {
								Intent it = new Intent(
										Global.Action_InternalCMD);
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
							e1.printStackTrace();// TE
						}
					}

					if (msg.content.startsWith(Global.Call_Conference)) {
						if (mDB != null && mDB.isOpen())
							mDB.close();
						return smslist;
					} else if (msg.content.startsWith(Global.Master_Parse)) {
						if (mDB != null && mDB.isOpen())
							mDB.close();
						return smslist;
					} else if (msg.content.startsWith("I am NOT your GUARD")) // tml***
																				// guard
																				// parse
					{
						Log.i("tml I am NOT your GUARD1> " + msg.address);
						if (mDB != null && mDB.isOpen())
							mDB.close();
						return smslist;
					} else if (msg.content.startsWith("I am your GUARD")) // tml***
																			// guard
																			// parse
					{
						Log.i("tml I am your GUARD1> " + msg.address);
						if (mDB != null && mDB.isOpen())
							mDB.close();
						return smslist;
					} else if (msg.content.startsWith("GUARD list IS FULL")) // tml***
																				// multi
																				// suvei,
																				// full
					{
						Log.i("tml GUARD list IS FULL1> " + msg.address);
						msg.content = context.getResources().getString(
								R.string.home_guard)
								+ "!\n"
								+ context.getResources().getString(
										R.string.suvmsg_full);
					} else if (msg.content.startsWith("REVOKE my GUARD access")) // tml***
																					// multi
																					// suvei,
																					// self
																					// remove
					{
						Log.i("tml REVOKE my GUARD access1> " + msg.address);
						msg.content = context.getResources().getString(
								R.string.home_guard)
								+ "!\n"
								+ context.getResources().getString(
										R.string.suvmsg_selfrmv);
					} else if (msg.content.startsWith(Global.SUV_OFF)) {
						if (mDB != null && mDB.isOpen())
							mDB.close();
						return smslist;
					} else if (msg.content.startsWith(Global.SUV_ON)) {
						if (mPref.readBoolean("securityEnabled", true) == false) {
							if (mDB != null && mDB.isOpen())
								mDB.close();
							return smslist;
						}

						mPref.write("SuvHostIDX", idx);
						mPref.write("SuvHostAddress", msg.address);
						if (mDB != null && mDB.isOpen())
							mDB.close();

						boolean found = false;

						List<String> instants = mPref.readArray("instants");

						if (instants != null) {
							for (String address : instants) {
								if (address.equals(msg.address)) {
									found = true;
									break;
								}
							}
						}

						if (found) {
							Intent intent = new Intent(
									Global.Action_Start_Surveillance);
							intent.putExtra("address", msg.address);
							context.sendBroadcast(intent);
						}
						return smslist;
					} else if (msg.content.equals(Global.Hi_AddFriend2)) { // tml***
																			// 200280
						if (mDB != null && mDB.isOpen())
							mDB.close();
						return smslist;
					} else if (msg.content.startsWith("[<CallFrom>]")) { // alex***
																			// callfrom
						Log.i("[<CallFrom>] apple was here");
						if (mDB != null && mDB.isOpen())
							mDB.close();
						return smslist;
					} else if (items[3].startsWith("[<LOCATIONSHARING>]")) {
						continue;
						/*
						 * alec Intent it = new
						 * Intent(Global.Action_InternalCMD);
						 * it.putExtra("Command", Global.CMD_LOCATION_SHARING);
						 * it.putExtra("Sender", items[0]); msg.content =
						 * String.
						 * format(context.getResources().getString(R.string
						 * .hasaskshare), msg.displayname);
						 * context.sendBroadcast(it);
						 */
					} else if (msg.content.startsWith("[<activeCall>]")) { // alex
																			// push
						Log.e("alex receive active call");
						return smslist;
					} else if (items[3].startsWith("[<hold>]")) {
						continue;
					} else if (items[3].startsWith("[<MISSEDREMIND>]")) {
						int missCount = 1;
						try {
							missCount = Integer
									.parseInt(items[3].substring(16));
						} catch (Exception e) {
						}
						msg.content = String.format(context.getResources()
								.getString(R.string.missed_call_remind_1),
								missCount);
						msg.content += " " + msg.displayname;
						String tFormat = DateUtils.formatDateTime(context,
								msg.time - 60000, DateUtils.FORMAT_SHOW_TIME
										| DateUtils.FORMAT_SHOW_DATE);
						String description2 = String
								.format(context.getResources().getString(
										R.string.missed_call_remind_2), tFormat);
						msg.content += description2;
					} else if (items[3].startsWith("[<NEWPHOTO>]")) {
						String localfile = Global.SdcardPath_inbox + "photo_"
								+ idx + ".jpg";
						if (AireJupiter.getInstance().downloadPhoto216(idx,
								localfile)) {
							// delete old bigger one
							new File(Global.SdcardPath_inbox + "photo_" + idx
									+ "b.jpg").delete();

							// alec: download big one as well
							localfile = Global.SdcardPath_inbox + "photo_"
									+ idx + "b.jpg";
							MyNet net = new MyNet(context);
							net.Download("profiles/photo_" + idx + ".jpg",
									localfile, null);

							Intent intent = new Intent();
							intent.setAction(Global.Action_Refresh_Gallery);
							context.sendBroadcast(intent);
						}
						continue;
					} else if (items[3].startsWith("[<NEWMOOD>]")) {
						String newMood = items[3].substring(11);
						int index = newMood.lastIndexOf("/");
						if (index != -1)
							newMood = newMood.substring(0, index);
						mADB.updateMoodByUID(idx, newMood);
						continue;
					} else if (items[3].startsWith("[<NEWUSERJOINS>]")) {
						msg.content = context.getResources().getString(
								R.string.auto_notify_joins);
					} else if (items[3].startsWith("[<AGREESHARE>]")) {
						String globalNumber = MyTelephony.attachPrefix(context,
								msg.address);
						try {
							int relation = Integer
									.valueOf(items[3].split(",")[2]);
							long timeout = MyUtil.getSharingTimeout(relation);
							mPref.writeLong(globalNumber, timeout);
							if (mPref.readLong("SpeedupMapMonitor", 0) < timeout * 1000)
								mPref.writeLong("SpeedupMapMonitor",
										timeout * 1000);
						} catch (Exception e) {
						}

					} else if (msg.content.startsWith("[<LOCATIONSHARING>]"))
						msg.content = String.format(context.getResources()
								.getString(R.string.hasaskshare),
								msg.displayname);
					else if (msg.content.startsWith("[EMOTION")) {
						int index = 1;
						try {
							index = Integer.valueOf(msg.content.substring(8,
									msg.content.length() - 1));
						} catch (Exception e) {
						}
						msg.content = "[EMOTION" + index + "]";
					} else if (msg.content.startsWith("here I am (")) {
						msg.longitudeE6 = mPref.readLong("longitude",
								Global.DEFAULT_LON);
						msg.latitudeE6 = mPref.readLong("latitude",
								Global.DEFAULT_LAT);
					}

					msg.status = -1;
					msg.type = 1;
					if (items.length > 4)
						msg.attached = Integer.parseInt(items[4]); // TODO
					else
						msg.attached = 0;
					if (msg.attached == 3) {
						try {
							phpIP = new NetInfo(context).longToIP(Long.valueOf(
									items[7], 16));
						} catch (Exception e) {
							if (AireJupiter.getInstance() != null) { // tml***
																		// china
																		// ip
								phpIP = AireJupiter.getInstance().getIsoPhp(0,
										true, null);
							} else {
								phpIP = AireJupiter.myPhpServer_default;
							}
						}
					} else if (msg.attached == 1 || msg.attached == 2
							|| msg.attached == 4 || msg.attached == 8) {
						try {
							phpIP = new NetInfo(context).longToIP(Long.valueOf(
									items[6], 16));
						} catch (Exception e) {
							if (AireJupiter.getInstance() != null) { // tml***
																		// china
																		// ip
								phpIP = AireJupiter.getInstance().getIsoPhp(0,
										true, null);
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
									success = net.Download("vmemo/" + items[5],
											msg.att_path_aud, msg.attached,
											phpIP);
									if (success)
										break;
									MyUtil.Sleep(500);
									count++;
								} while (count++ < 3);

								if (!success) {
									if (mDB != null && mDB.isOpen())
										mDB.close();// alec
									return smslist;
								}
							} else if ((msg.attached & 4) == 4) {
								// update msg.att_path_aud values
								int interphoneType = -1;
								if (msg.content.startsWith("(itph*"))
									interphoneType = Integer
											.parseInt(msg.content.substring(6)) - 1;
								else
									msg.att_path_aud = Global.SdcardPath_inbox
											+ "interphonevoice_" + items[5];

								boolean success = false;

								if (interphoneType == -1) {
									int count = 0;
									do {
										MyNet net = new MyNet(context);
										success = net.Download("vmemo/"
												+ items[5], msg.att_path_aud,
												msg.attached, phpIP);
										if (success)
											break;
										MyUtil.Sleep(500);
										count++;
									} while (count++ < 3);

									if (!success) {
										if (mDB != null && mDB.isOpen())
											mDB.close();// alec
										return smslist;
									}
								}

							}
						} else
							msg.content += "\n("
									+ context
											.getResources()
											.getString(
													R.string.no_sdcard_receive_voice_message)
									+ ")";
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
									success = net.Download("mms/" + iamgePath,
											msg.att_path_img, msg.attached,
											phpIP);
									if (success)
										break;
									MyUtil.Sleep(500);
									count++;
								} while (count++ < 3);

								/*
								 * if (success) { Bitmap
								 * bmp=ImageUtil.loadBitmapSafe(1,
								 * msg.att_path_img);
								 * MediaStore.Images.Media.insertImage
								 * (context.getContentResolver(), bmp,
								 * "AireCenter" , "AireCenter"); }
								 */
							} catch (Exception e) {
							}
						} else
							msg.content += "\n("
									+ context
											.getResources()
											.getString(
													R.string.no_sdcard_receive_picture_message)
									+ ")";
					}

					if (msg.attached == 8) { // file or video

						msg.att_path_aud = "ulfiles/"
								+ URLDecoder.decode(items[5].replace('*', '%'),
										"UTF-8");
						Log.d("msg.att_path_aud:" + msg.att_path_aud);

						if (msg.content.startsWith("(vdo)")) {
							msg.content = context.getString(R.string.video)
									+ " " + msg.content;
						} else if (msg.content.startsWith("(fl)")) {
							// msg.content =
							// context.getString(R.string.filememo_recv)+" "+msg.content;
							// alec:
							String filename = msg.att_path_aud
									.substring(msg.att_path_aud
											.lastIndexOf("/") + 1);
							String part2 = msg.content.substring(4);
							msg.content = "(fl)  " + filename + "  " + part2;
						}// zhao
					}

					boolean flag = ConversationActivity.sender != null
							&& MyTelephony.SameNumber(
									ConversationActivity.sender, msg.address);
					msg.read = (flag == true ? 1 : 0);

					if (!(msg.attached == 4)) {
						mDB.insertMessage(msg.address, msg.contactid, msg.time,
								msg.read, -1, 1, "", msg.content, msg.attached,
								msg.att_path_aud, msg.att_path_img, 0,
								msg.longitudeE6, msg.latitudeE6, 0,
								msg.displayname, phpIP, msg.group_member);
					}
					if (!msg.content.startsWith("[<LOCATIONSHARING>]"))
						smslist.add(msg);
				}
			} catch (Exception e) {
				Log.e("Exception in Parse1 " + e.getMessage());
				continue;
			}
		}
		if (mDB != null && mDB.isOpen())
			mDB.close();

		return smslist;
	}

	static public ArrayList<SMS> Parse2(Context context, String data,
			ContactsQuery cq, AmpUserDB mADB, MyPreference mPref) {
		ArrayList<SMS> smslist = new ArrayList<SMS>();

		boolean locationsharing = false;

		SmsDB mDB = new SmsDB(context);
		mDB.open();
		try {
			Pattern p1 = Pattern.compile("/");
			String[] items = p1.split(data, 20);
			Pattern p2 = Pattern.compile("<Z>");
			String[] cont = p2.split(data, 15);
			// String phpIP = AireJupiter.myPhpServer_default;
			String phpIP = null;
			if (AireJupiter.getInstance() != null) { // tml*** china ip
				phpIP = AireJupiter.getInstance().getIsoPhp(0, true, null);
			} else {
				phpIP = AireJupiter.myPhpServer_default;
			}

			String debug = "";
			for (int i = 0; i < cont.length; i++) { // tml*** debug
				debug = debug + " [" + i + "]" + cont[i];
			}
			Log.d("msgPARSE cont =" + debug);
			if (cont.length > 5) {
				try {
					phpIP = new NetInfo(context).longToIP(Long.valueOf(cont[5],
							16));
				} catch (Exception e) {
					phpIP = cont[5];
				}
			}

			idx = Integer.parseInt(items[1], 16);

			if (!mADB.isFafauser(idx)) {
				if (mDB != null && mDB.isOpen())
					mDB.close();
				return smslist;
			}

			SMS msg = new SMS();
			String Sender = mADB.getAddressByIdx(idx);

			msg.address = Sender;

			msg.contactid = cq.getContactIdByNumber(msg.address);
			if (msg.contactid > 0)
				msg.displayname = cq.getNameByContactId(msg.contactid);
			else
				msg.displayname = mADB.getNicknameByAddress(msg.address);

			if (msg.displayname == null)
				msg.displayname = context.getResources().getString(
						R.string.unknown_person);

			try {
				msg.content = cont[1].substring(0, cont[1].lastIndexOf("/`")); // drop
																				// rowid
				cont[1] = msg.content;
			} catch (Exception e1) {
				msg.content = cont[1];
			}

			if (msg.content.startsWith("`[")) {// alec
				try {
					int p = msg.content.indexOf("]\n");
					if (p > 3) {
						int groupid = Integer.parseInt(msg.content.substring(2,
								p));
						Sender = "[<GROUP>]" + groupid;
						msg.address = Sender;
						msg.content = msg.content.substring(p + 2);
						msg.displayname = mADB
								.getNicknameByAddress(msg.address);
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
							it.putExtra("Command",
									Global.CMD_GROUP_ADD_NEW_MEMBER);
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
					e1.printStackTrace();// TE
				}
			}
			// li*** close video view
			if (msg.content.startsWith(Global.Call_Conference_Video_Close)) {
				Log.i("收到关闭video消息 Msg = " + msg.content);
				//发送广播
				Intent intent = new Intent(Global.Action_Video_Close);
				context.sendBroadcast(intent);
				return smslist;
			}
			// li*** open video view
			else if (msg.content.startsWith(Global.Call_Conference_Video_Open)) {
				Log.i("收到开启video消息 Msg = " + msg.content);
				int groupIndex = Integer.parseInt(msg.content.split("&")[1]);
				//发送广播
				Intent intent = new Intent(Global.Action_Video_Open);
				intent.putExtra("Group_Index", groupIndex);
				context.sendBroadcast(intent);
				return smslist;
			}
			else if (msg.content.startsWith(Global.Call_Conference)) {
				// tml*** conf-200 offset
				msg.time = Long.parseLong(items[2], 16);
				long timeOffset = mPref.readLong("confServerOffset", 0);
				long my_time = (new Date().getTime() / 1000) + timeOffset;
				long timeDiff = msg.time - my_time;
				Log.i("incConf1 time this=" + msg.time + " my=" + my_time
						+ " diff=" + timeDiff);
				if (timeDiff <= 20) {
					try {
						String[] iItems = msg.content.split("\n\n");
						String ip = MyUtil.longToIPForServer(Long.valueOf(
								iItems[1], 16));
						int from = Integer.parseInt(iItems[2], 16);
						if (AireJupiter.getInstance() != null) {
							AireJupiter.getInstance().updateCallDebugStatus(
									true, null);
							AireJupiter.getInstance().updateCallDebugStatus(
									false, "\n<Conf " + ip + " " + from);
							Log.d("voip.invitedConf1 " + ip + " " + from);
							// tml*** broadcast
							boolean isBroadcast = false;
							if (DialerActivity.getDialer() == null) {
								isBroadcast = msg.content
										.contains(Global.Call_Broadcast);
								if (isBroadcast) {
									mPref.write(Key.BCAST_CONF, 0);
								} else {
									mPref.write(Key.BCAST_CONF, -1);
								}
							}
							AireJupiter.getInstance()
									.lanuchServiceYToJoinChatroom(ip, from,
											isBroadcast);
						}
					} catch (Exception e) {
					}
				}

				if (mDB != null && mDB.isOpen())
					mDB.close();
				return smslist;
			} else if (msg.content.startsWith(Global.Master_Parse)) {
				if (msg.content.endsWith("test")) {
					boolean checkDialer = false, checkVenus = false;
					String vstate = "";
					if (DialerActivity.getDialer() != null)
						checkDialer = true;
					if (AireVenus.instance() != null) {
						vstate = AireVenus.callstate_AV;
						checkVenus = true;
					}
					int vers = mPref.readInt("versionCode", 0);
					String lastCall = mPref.read("lastCallSip", "n/a");
					if (!Log.enDEBUG)
						lastCall = "***";
					if (AireJupiter.getInstance() != null
							&& AireJupiter.getInstance().tcpSocket
									.isLogged(false)) {
						AireJupiter.getInstance().tcpSocket.send(msg.address,
								"echo test " + vers + SettingPage.vlib + " D"
										+ checkDialer + " V" + checkVenus
										+ vstate + " " + lastCall, 0, null,
								null, 0, null);
					}
				}
				// bree
				else if (msg.content.startsWith(Global.MONITOR)) {
					try {
						int myIdx = 0;
						List<String> instants = mPref.readArray("instants");
						if (instants.contains(Sender)) {
							if (mPref.readBoolean(Key.SELFVIDIO, false)) {

							} else {
								// 1.设置标志位自动拨打
								mPref.write(Key.SELFVIDIO, true);// 自动拨打

								try {
									myIdx = Integer.parseInt(
											mPref.read("myID", "0"), 16);
									mPref.write("ChatroomHostIdx", myIdx);
								} catch (Exception e) {
								}

								mPref.write("ChatroomHostIdx", myIdx);
							}
							// 2.自动波打 判断1）是否是监护人1）是否正在自动拨打广播中
							AireVenus.setCallType(AireVenus.CALLTYPE_CHATROOM);
							mPref.write("incomingChatroom", false);
							MakeCall.ConferenceCall(AireApp.context, "" + myIdx,-1,false);
							new Thread(sendNotifyForJoinChatroom).start();
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (mDB != null && mDB.isOpen())
					mDB.close();
				return smslist;
			}

			else if (msg.content.startsWith("[<LOCATIONSHARING>]")) {
				Intent it = new Intent(Global.Action_InternalCMD);
				it.putExtra("Command", Global.CMD_LOCATION_SHARING);
				it.putExtra("Sender", Sender);
				context.sendBroadcast(it);
				msg.time = Long.parseLong(items[2], 16) * 1000;
			} else if (msg.content.startsWith("[<NEWPHOTO>]")) {
				String localfile = Global.SdcardPath_inbox + "photo_" + idx
						+ ".jpg";
				if (AireJupiter.getInstance().downloadPhoto216(idx, localfile)) {
					// delete old bigger one
					new File(Global.SdcardPath_inbox + "photo_" + idx + "b.jpg")
							.delete();

					// alec: download big one as well
					localfile = Global.SdcardPath_inbox + "photo_" + idx
							+ "b.jpg";
					MyNet net = new MyNet(context);
					net.Download("profiles/photo_" + idx + ".jpg", localfile,
							null);

					address = mADB.getAddressByIdx(idx);
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
			} else if (msg.content.startsWith("[<NEWMOOD>]")) {
				String newMood = msg.content.substring(11);
				int index = newMood.lastIndexOf("/");
				if (index != -1)
					newMood = newMood.substring(0, index);
				mADB.updateMoodByUID(idx, newMood);
				if (mDB != null && mDB.isOpen())
					mDB.close();
				return smslist;
			} else {
				msg.time = Long.parseLong(items[2], 16) * 1000;

				if (msg.content.startsWith("[<MISSEDREMIND>]")) {
					int n = Integer.parseInt(cont[1].substring(16));
					msg.content = String.format(context.getResources()
							.getString(R.string.missed_call_remind_1), n);
					msg.content += " " + msg.displayname;
					String tFormat = DateUtils.formatDateTime(context,
							msg.time - 60000, DateUtils.FORMAT_SHOW_TIME
									| DateUtils.FORMAT_SHOW_DATE);
					String description2 = String.format(context.getResources()
							.getString(R.string.missed_call_remind_2), tFormat);
					msg.content += description2;
				} else if (msg.content.startsWith("[<NEWUSERJOINS>]")) {
					int uid = Integer.parseInt(cont[1].substring(16), 16);
					msg.content = context.getResources().getString(
							R.string.auto_notify_joins);
					mADB.insertUser(msg.address, uid);
					msg.contactid = cq.getContactIdByNumber(msg.address);
					ContactsOnline.setContactOnlineStatus(msg.address, 2);
				} else if (msg.content.startsWith("[<AGREESHARE>]")) {
					try {
						msg.latitudeE6 = Long.parseLong(cont[1].split(",")[3]);
						msg.longitudeE6 = Long.parseLong(cont[1].split(",")[4]);
					} catch (Exception e) {
						msg.longitudeE6 = Global.DEFAULT_LON;
						msg.latitudeE6 = Global.DEFAULT_LAT;
					}
					String globalNumber = MyTelephony.attachPrefix(context,
							msg.address);
					try {
						int relation = Integer.valueOf(cont[1].split(",")[2]);
						long timeout = MyUtil.getSharingTimeout(relation);
						mPref.writeLong(globalNumber, timeout);
						mPref.writeLong("SpeedupMapMonitor", timeout);
					} catch (Exception e) {
					}
				} else if (msg.content.startsWith("here I am (")) {
					msg.longitudeE6 = mPref.readLong("longitude",
							Global.DEFAULT_LON);
					msg.latitudeE6 = mPref.readLong("latitude",
							Global.DEFAULT_LAT);
				} else if (msg.content.startsWith("[EMOTION")) {
					int index = 1;
					try {
						index = Integer.valueOf(msg.content.substring(8,
								msg.content.length() - 1));
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
				} else if (msg.content.startsWith("I am NOT your GUARD")) // tml***
																			// guard
																			// parse
				{
					Log.i("tml I am NOT your GUARD2> " + msg.address);
					if (mDB != null && mDB.isOpen())
						mDB.close();
					return smslist;
				} else if (msg.content.startsWith("I am your GUARD")) // tml***
																		// guard
																		// parse
				{
					Log.i("tml I am your GUARD2> " + msg.address);
					if (mDB != null && mDB.isOpen())
						mDB.close();
					return smslist;
				} else if (msg.content.startsWith("GUARD record start")) // tml***
																			// monitor
																			// record
				{
					Log.i("tml GUARD record start2> " + msg.address);
					if (mDB != null && mDB.isOpen())
						mDB.close();
					return smslist;
				} else if (msg.content.startsWith("GUARD record stop")) // tml***
																		// monitor
																		// record
				{
					Log.i("tml GUARD record stop2> " + msg.address);
					if (mDB != null && mDB.isOpen())
						mDB.close();
					return smslist;
				} else if (msg.content.startsWith("GUARD list IS FULL")) // tml***
																			// multi
																			// suvei,
																			// full
				{
					Log.i("tml GUARD list IS FULL2> " + msg.address);
					List<String> instants = mPref.readArray("instants");
					boolean llate = false;

					String sfinal = msg.content.substring(
							msg.content.length() - 4, msg.content.length());
					if (sfinal.equals("LATE"))
						llate = true; // confirmed del

					if (instants != null && !llate) {
						for (String add : instants) {
							if (msg.address.equals(add)) {
								instants.remove(add);
								mPref.writeArray("instants", instants);
								mPref.delect("autoAnswer:" + msg.address);
								mPref.delect("autoAnswer2:" + msg.address);

								Intent intent = new Intent();
								intent.setAction(Global.Action_SecurityActivity);
								intent.putExtra("Command", 33);
								context.sendBroadcast(intent);
								break;
							}
						}
					} else {
						if (mDB != null && mDB.isOpen())
							mDB.close();
						return smslist;
					}

					msg.content = context.getResources().getString(
							R.string.home_guard)
							+ "!\n"
							+ context.getResources().getString(
									R.string.suvmsg_full);

				} else if (msg.content.startsWith("REVOKE my GUARD access")) // tml***
																				// multi
																				// suvei,
																				// self
																				// remove
				{
					Log.i("tml REVOKE my GUARD access2> " + msg.address);
					List<String> instants = mPref.readArray("instants");
					boolean lastone = false;
					boolean lfinal = false;
					boolean found = false;

					String sfinal = msg.content.substring(
							msg.content.length() - 5, msg.content.length());
					if (sfinal.equals("FINAL"))
						lfinal = true; // confirmed del

					if (instants != null) {
						if (instants.size() == 1)
							lastone = true; // last remaining suv contact

						for (String add : instants) {
							if (msg.address.equals(add)) {
								if (!lastone) { // not last contact, ok
									found = true;
									instants.remove(add);
									mPref.writeArray("instants", instants);
									mPref.delect("autoAnswer:" + msg.address);
									mPref.delect("autoAnswer2:" + msg.address);

									Intent intent = new Intent();
									intent.setAction(Global.Action_SecurityActivity);
									intent.putExtra("Command", 33);
									context.sendBroadcast(intent);

									msg.content = context.getResources()
											.getString(R.string.home_guard)
											+ "!\n"
											+ context.getResources().getString(
													R.string.suvmsg_selfrmv);
								} else { // last contact, check
									found = true;
									if (lfinal) { // last, confirmed del + stop
													// suv
										instants.remove(add);
										mPref.writeArray("instants", instants);
										mPref.delect("autoAnswer:"
												+ msg.address);
										mPref.delect("autoAnswer2:"
												+ msg.address);

										Intent intent = new Intent();
										intent.setAction(Global.Action_SecurityActivity);
										intent.putExtra("Command", 33);
										context.sendBroadcast(intent);

										Intent intent2 = new Intent(
												Global.Action_End_Surveillance);
										intent2.putExtra("address", msg.address);
										context.sendBroadcast(intent2);

										msg.content = context.getResources()
												.getString(R.string.home_guard)
												+ "!\n"
												+ context
														.getResources()
														.getString(
																R.string.suvmsg_selfrmv);
									} else { // last, no del, re-add + send
												// confirm
										if (AireJupiter.getInstance() != null
												&& AireJupiter.getInstance().tcpSocket
														.isLogged(false)) {
											AireJupiter.getInstance().tcpSocket
													.send(msg.address,
															"I am your GUARD LAST",
															0, null, null, 0,
															null);
										}

										if (mDB != null && mDB.isOpen())
											mDB.close();
										return smslist;
									}
								}
								break;
							}
						}
						if (!found) {
							if (mDB != null && mDB.isOpen())
								mDB.close();
							return smslist;
						}
					} else {
						if (mDB != null && mDB.isOpen())
							mDB.close();
						return smslist;
					}
				} else if (msg.content.startsWith(Global.SUV_OFF)) {
					if (mDB != null && mDB.isOpen())
						mDB.close();

					boolean found = false;
					List<String> instants = mPref.readArray("instants");
					if (instants != null) {
						for (String address : instants) {
							if (address.equals(msg.address)) {
								found = true;
								break;
							}
						}
					}

					if (msg.content.equals(Global.SUV_OFF)) {
						if (found) {
							Intent intent = new Intent(
									Global.Action_End_Surveillance);
							intent.putExtra("address", msg.address);
							context.sendBroadcast(intent);
						}
					} else if (msg.content.equals(Global.SUV_OFF_IOTALL)) { // tml***
																			// iot
																			// control
						if (found) {
							Intent intent = new Intent(
									Global.Action_End_Homesensor);
							intent.putExtra("address", msg.address);
							context.sendBroadcast(intent);
						}
					}

					return smslist;
				} else if (msg.content.startsWith(Global.SUV_ON)) {
					if (mPref.readBoolean("securityEnabled", true) == false) {
						if (mDB != null && mDB.isOpen())
							mDB.close();
						return smslist;
					}

					mPref.write("SuvHostIDX", idx);
					mPref.write("SuvHostAddress", msg.address);
					if (mDB != null && mDB.isOpen())
						mDB.close();

					boolean found = false;

					List<String> instants = mPref.readArray("instants");

					if (instants != null) {
						for (String address : instants) {
							if (address.equals(msg.address)) {
								found = true;
								break;
							}
						}
					}

					if (msg.content.equals(Global.SUV_ON)) {
						// tml*** suv busy
						String calltype = null;
						if (AireVenus.instance() != null) {
							calltype = AireVenus.callstate_AV;
						}

						if (found) {
							Intent intent = new Intent(
									Global.Action_Start_Surveillance);
							intent.putExtra("address", msg.address);
							if (calltype != null) { // tml*** suv busy
								intent.putExtra("busy", true);
							}
							context.sendBroadcast(intent);
						}
					} else if (msg.content.equals(Global.SUV_ON_IOTALL)) { // tml***
																			// iot
																			// control
						if (found) {
							Intent intent = new Intent(
									Global.Action_Start_Homesensor);
							intent.putExtra("address", msg.address);
							context.sendBroadcast(intent);
						}
					}

					return smslist;
				} else if (msg.content.equals(Global.Hi_AddFriend2)) { // tml***
																		// 200280
					if (mDB != null && mDB.isOpen())
						mDB.close();
					return smslist;
				} else if (msg.content.startsWith("[<CallFrom>]")) { // alex***
																		// callfrom
					Log.i("[<CallFrom>] apple was here");
					if (mDB != null && mDB.isOpen())
						mDB.close();
					return smslist;
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
						msg.content += "\n("
								+ context
										.getResources()
										.getString(
												R.string.no_sdcard_receive_voice_message)
								+ ")";
					} else {
						if ((msg.attached & 1) == 1) {
							int count = 0;
							boolean success = false;
							do {
								MyNet net = new MyNet(context);
								success = net.Download("vmemo/" + cont[3],
										msg.att_path_aud, msg.attached, phpIP);
								if (success)
									break;
								MyUtil.Sleep(500);
							} while (count++ < 3);

							if (!success) {
								Log.e("*** Download failed ****");
								// forget this message, because we download
								// failed
								if (mDB != null && mDB.isOpen())
									mDB.close();// alec
								return smslist;
							}
						} else if ((msg.attached & 4) == 4) {
							// update msg.att_path_aud values

							int interphoneType = -1;
							if (msg.content.startsWith("(itph*"))
								interphoneType = Integer.parseInt(msg.content
										.substring(6)) - 1;
							else
								msg.att_path_aud = Global.SdcardPath_inbox
										+ "interphonevoice_" + cont[3];

							if (interphoneType == -1) {
								boolean success = false;
								int count = 0;
								do {
									MyNet net = new MyNet(context);
									success = net.Download("vmemo/" + cont[3],
											msg.att_path_aud, msg.attached,
											phpIP);
									if (success)
										break;
									MyUtil.Sleep(500);
									count++;
								} while (count++ < 3);

								if (!success) {
									Log.e("*** Download failed ****");
									// forget this message, because we download
									// failed
									if (mDB != null && mDB.isOpen())
										mDB.close();// alec
									return smslist;
								}
							}
						}
					}
				}
				if ((msg.attached & 2) == 2) {
					if (!cont[4].startsWith(".jpg"))
						cont[4] = cont[4].substring(0,
								cont[4].lastIndexOf(".jpg") + 4);
					msg.att_path_img = Global.SdcardPath_inbox + cont[4];
					if (!MyUtil.checkSDCard(context)) {
						msg.content += "\n("
								+ context
										.getResources()
										.getString(
												R.string.no_sdcard_receive_picture_message)
								+ ")";
					} else {
						int count = 0;
						boolean success = false;
						do {
							MyNet net = new MyNet(context);
							success = net.Download("mms/" + cont[4],
									msg.att_path_img, msg.attached, phpIP);
							if (success)
								break;
							MyUtil.Sleep(500);
						} while (count++ < 3);

						/*
						 * if (success) { Bitmap bmp=ImageUtil.loadBitmapSafe(1,
						 * msg.att_path_img);
						 * MediaStore.Images.Media.insertImage
						 * (context.getContentResolver(), bmp, "AireCenter" ,
						 * "AireCenter"); }
						 */
					}
				}
				if (msg.attached == 8) { // file: download directly
					msg.att_path_aud = Global.SdcardPath_inbox
							+ cont[3].substring(0, cont[3].lastIndexOf("_"));
					// msg.content =
					// context.getString(R.string.filememo_recv)+" "+msg.content;
					// alec:
					String filename = msg.att_path_aud
							.substring(msg.att_path_aud.lastIndexOf("/") + 1);
					String part2 = msg.content.substring(4);
					msg.content = "(fl)  " + filename + "  " + part2;

					if (!MyUtil.checkSDCard(context)) {
						msg.content += "\n("
								+ context.getResources().getString(
										R.string.no_sdcard) + ")";
					} else {
						int count = 0;
						boolean success = false;
						do {
							MyNet net = new MyNet(context);
							// alec*** non-eng file
							String encoded = "ulfiles/" + cont[3];
							try {
								encoded = "ulfiles/"
										+ URLEncoder.encode(cont[3], "UTF-8");
							} catch (Exception e) {
							}
							// success=net.Download("ulfiles/"+cont[3],
							// msg.att_path_aud, msg.attached,phpIP);
							success = net.Download(encoded, msg.att_path_aud,
									msg.attached, phpIP);
							// ***alec
							if (success)
								break;
							MyUtil.Sleep(500);
						} while (count++ < 3);
					}
				}
			}

			boolean flag = ConversationActivity.sender != null
					&& MyTelephony.SameNumber(ConversationActivity.sender,
							msg.address);
			msg.read = (flag == true ? 1 : 0);

			if (msg.content.startsWith("[<LOCATIONSHARING>]")) {
				locationsharing = true;
				msg.content = String.format(
						context.getResources().getString(R.string.hasaskshare),
						msg.displayname);
			}

			msg.obligate1 = phpIP;
			if (!(msg.attached == 4)) {
				mDB.insertMessage(msg.address, msg.contactid, msg.time,
						msg.read, -1, 1, "", msg.content, msg.attached,
						msg.att_path_aud, msg.att_path_img, 0, msg.longitudeE6,
						msg.latitudeE6, 0, msg.displayname, msg.obligate1,
						msg.group_member);
			}
			if (!locationsharing) // dont popup dialog
				smslist.add(msg);

			// tml*** chatview
			if (AireVenus.instance() != null && AireVenus.callstate_AV != null) {
				VoipCore lVoipCore = AireVenus.instance().getVoipCore();
				String inCallAddress = lVoipCore.getRemoteAddress()
						.getUserName();
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
		} catch (Exception e) {
			Log.e("Exception in Parse2 " + e.getMessage());
		}

		if (mDB != null && mDB.isOpen())
			mDB.close();
		return smslist;
	}

	static Runnable sendNotifyForJoinChatroom = new Runnable() {
		public void run() {
			MyPreference mPref = new MyPreference(AireApp.context);
			AmpUserDB mADB = new AmpUserDB(AireApp.context);
			mADB.open();
			String myIdxHex = mPref.read("myID", "0");

			String ServerIP = mPref.read("conferenceSipServer",
					AireJupiter.myConfSipServer_default);
			if (AireJupiter.getInstance() != null) {
				ServerIP = AireJupiter.getInstance().getIsoConf(ServerIP); // tml***
																			// china
																			// ip
			}
			long ip = MyUtil.ipToLong(ServerIP);
			String HexIP = Long.toHexString(ip);

			String content = Global.Call_Conference + "\n\n" + HexIP + "\n\n"
					+ myIdxHex;

			mPref.write(Key.BCAST_CONF, 1);
			content = Global.Call_Conference + Global.Call_Broadcast + "\n\n"
					+ HexIP + "\n\n" + myIdxHex;

			if (AireJupiter.getInstance() != null)
				AireJupiter.getInstance().updateCallDebugStatus(true, null);

			String address = mADB.getAddressByIdx(idx);

			if (AireJupiter.getInstance() != null
					&& AireJupiter.getInstance().tcpSocket() != null) {
				if (AireJupiter.getInstance().isLogged()) {
					if (AireJupiter.getInstance() != null)
						AireJupiter.getInstance().updateCallDebugStatus(false,
								"\n>Conf " + address);
					Log.d("voip.inviteConf1 " + address + " " + content);
					AireJupiter.getInstance().tcpSocket()
							.send(address, content, 0, null, null, 0, null);
				}
			}
		}
	};
}
