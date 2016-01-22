package com.pingshow.qrcode;

import java.io.IOException;

import com.pingshow.amper.AddAsFriendActivity;
import com.pingshow.amper.ComposeActivity;
import com.pingshow.amper.CreateGroupActivity;
import com.pingshow.amper.FunctionActivity;
import com.pingshow.amper.Global;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.R;
import com.pingshow.amper.SearchDialog;
import com.pingshow.amper.SendAgent;
import com.pingshow.amper.SettingActivity;
import com.pingshow.amper.UsersActivity;
import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.RelatedUserDB;
import com.pingshow.util.MCrypt;
import com.pingshow.util.MyUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.Toast;

public class PopwindowDialog extends Activity implements OnClickListener{
	private AmpUserDB mADB;
	private RelatedUserDB mRDB;
	private MyPreference mPref;
	private ContactsQuery cq;
	private LinearLayout layout01,layout02,layout03,layout04,layout05;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addpop);
		mADB = new AmpUserDB(this);
		mADB.open();
		cq = new ContactsQuery(this);
		mPref=new MyPreference(this);
		mRDB = new RelatedUserDB(this);
		mRDB.open();
		initView();
	}
	/**
	 * 初始化数据
	 */
	private void initView() {
		//得到布局组件对象并设置监听事件
				layout01 = (LinearLayout)findViewById(R.id.llayout01);
				layout02 = (LinearLayout)findViewById(R.id.llayout02);
				layout03 = (LinearLayout)findViewById(R.id.llayout03);
				layout04 = (LinearLayout)findViewById(R.id.llayout04);
				layout05 = (LinearLayout)findViewById(R.id.llayout05);

				layout01.setOnClickListener(this);
				layout02.setOnClickListener(this);
				layout03.setOnClickListener(this);
				layout04.setOnClickListener(this);
				layout05.setOnClickListener(this);
	}
	@Override
	public boolean onTouchEvent(MotionEvent event){
		finish();
		return true;
	}
	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.llayout01:
			startActivity(new Intent(PopwindowDialog.this, ComposeActivity.class));
			break;
		case R.id.llayout02:
			startActivityForResult(new Intent(PopwindowDialog.this,CreateGroupActivity.class),2001);
			break;
		case R.id.llayout03:
			startActivityForResult(new Intent(PopwindowDialog.this,SearchDialog.class),2001);
			break;
		case R.id.llayout04:
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
				Intent openCameraIntent = new Intent(PopwindowDialog.this, CaptureActivity.class);
				startActivityForResult(openCameraIntent, 105);
			} else {
				Toast.makeText(PopwindowDialog.this, getString(R.string.old_sdk), Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.llayout05:
			startActivity(new Intent(PopwindowDialog.this, SettingActivity.class));
			break;

		default:
			break;
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 105) {  //tml|li*** qr addf
			if (resultCode == RESULT_OK) {
				Bundle bundle = data.getExtras();
				String scanResult = bundle.getString("result");
				String[] items = null;
				boolean success = true;
				String falseScan = "";
				
				if (scanResult != null) {
					//li*** encrypt
					Log.d("qr preXN= " + scanResult);
					try {
						byte[] bytes = MCrypt.encrypt(Base64.decode(scanResult.getBytes(), Base64.DEFAULT));
						scanResult = new String(bytes);
					} catch (Exception e) {
						Log.e("qr decrypt !@#$ " + e.getMessage());
					}
					Log.d("qr postXN= " + scanResult);
					
					if (scanResult.contains(",")) {
						items = scanResult.split(",");
						
						if (items.length == 4 && items[0].equals("AddAireFriend")) {
							for (int i = 0; i < 4; i++) {
								if (items[i] == null || items[i].length() == 0) {
									success = false;
									break;
								}
								Log.d("scan friend item" + i + " " + items[i]);
							}
							
							try {
								if (!success) throw new IOException();
								String address = items[1];
								int idx = Integer.parseInt(items[2]);
								String nickname = items[3];
								if (mADB.isFafauser(address)) {
									Intent it = new Intent(PopwindowDialog.this, FunctionActivity.class);
									long contactId = cq.getContactIdByNumber(address);
									it.putExtra("Contact_id", contactId);
									it.putExtra("Address", address);
									it.putExtra("Nickname", nickname);
									it.putExtra("Idx", idx);
									startActivity(it);
								} else {
									Intent it = new Intent(PopwindowDialog.this, AddAsFriendActivity.class);
									it.putExtra("Address", address);
									it.putExtra("Nickname", nickname);
									it.putExtra("Idx", idx);
									startActivityForResult(it, 10);
								}
							} catch (Exception e) {
								e.printStackTrace();
								success = false;
							}
						} else {
							success = false;
//							falseScan = "\n" + scanResult;
						}
					} else {
						success = false;
//						falseScan = "\n" + scanResult;
					}
				}
				
				if (!success) {
					Toast.makeText(PopwindowDialog.this, getString(R.string.qr_invalid) + falseScan, Toast.LENGTH_SHORT).show();
				}
			}
		}else if (requestCode==10)
		{
			if (resultCode==RESULT_OK) {
				
				if (!MyUtil.checkNetwork(this)) return;
				
				String address=data.getExtras().getString("Address");
				String nickname=data.getExtras().getString("Nickname");
				int idx=data.getExtras().getInt("Idx");
				mADB.insertUser(address, idx, nickname);
				mRDB.deleteContactByAddress(address);
				
				ContactsOnline.setContactOnlineStatus(address, 0);
				UsersActivity.forceRefresh=true;
				
				Intent it = new Intent(Global.Action_InternalCMD);
				it.putExtra("Command", Global.CMD_UPLOAD_FRIENDS);
				it.putExtra("type", 1);//Single Friend
				it.putExtra("serverType", 1);//add
				it.putExtra("idxlist", idx+"");
				sendBroadcast(it);
				
				try {
					Log.d("SENDING ADDF REQUEST.original");
					int myIdx=Integer.parseInt(mPref.read("myID","0"),16);
					SendAgent agent = new SendAgent(this, myIdx, 0, false);
					agent.onSend(address, Global.Hi_AddFriend1, 0, null, null, true);
				} catch (Exception e) {}
				
				Intent it2 = new Intent(Global.Action_Refresh_Gallery);
				sendBroadcast(it2);
				
				Intent it3 = new Intent(Global.Action_InternalCMD);
				it3.putExtra("Command", Global.CMD_SEARCH_POSSIBLE_FRIENDS);
				sendBroadcast(it3);
			}
		}}
}
