package com.pingshow.amper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.amper.bean.Person;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.view.TabPageIndicator;
import com.pingshow.network.MyNet;
import com.pingshow.util.MCrypt;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;

public class PickupActivity extends FragmentActivity {

	private List<Map<String, String>> amperList;
	private AmpUserDB mADB;
	private MyPreference mPref;
	private boolean isConference = false;
	private ArrayList<String> chatroomMemberslist = new ArrayList<String>();

	private ViewPager mViewPager;
	private TabPageIndicator mIndicator;
	private ArrayList<String> titleList = new ArrayList<String>();
	private ArrayList<ConferenceBasePager> pageList = new ArrayList<ConferenceBasePager>();
	private PagerAdapter myAdapter;
	private TextView mCounts;
	private TextView mCancel;
	private ConferenceContactsPager contactsPager;
	private ConferenceCallPager callPager;
	private ConferenceFriendsPager friendsPager;
	private int aireCallCount;
	private int phoneCount;
	private int callCount;
	private List<Person> contactsList;
	private List<String> callList;

	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Toast.makeText(PickupActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
		}
	};
	//jack call init

	private String phpip;
	private String room;
	private int myIdx;
	private String serverIP;
	private MCrypt mc;
	private MyNet net;
	private String pass;
	private String myUsername;
	private String myPasswd;

	private void initCall() {
		neverSayNeverDie(PickupActivity.this);  //tml|bj*** neverdie/
		mADB = new AmpUserDB(this);
		mADB.open();

		mPref = new MyPreference(this);

		//加密使用
		mc = new MCrypt();

		net = new MyNet(PickupActivity.this);

		String myIdxHex = mPref.read("myID", "0");
		myIdx = Integer.parseInt(myIdxHex, 16);

		phpip = AireJupiter.getInstance().getIsoPhp(0, true, "74.3.165.66");
		room = String.format("%07d", myIdx);
		serverIP = mPref.read("conferenceSipServer",
				AireJupiter.myConfSipServer_default);
		if (AireJupiter.getInstance() != null) {
			serverIP = AireJupiter.getInstance().getIsoConf(serverIP); // tml***
			// china
			// ip
		}

		//		address = MCrypt.bytesToHex(mc.encrypt(globalnumber));
		pass = "aireping*$857";
		try {
			pass = MCrypt.bytesToHex(mc.encrypt(pass));
		} catch (Exception e) {
			e.printStackTrace();
		}

		myUsername = String.format("**%d", myIdx);
		myPasswd = mPref.read("password", "1111");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.pickup);
	    this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

		//init call
		initCall();

		//jack base
		titleList.add(getResources().getString(R.string.friends));
		friendsPager = new ConferenceFriendsPager(PickupActivity.this);
		friendsPager.destory();

		pageList.add(friendsPager);

	    amperList = new ArrayList<Map<String, String>>();
		contactsList = new ArrayList<Person>();
		callList = new ArrayList<String>();

	    isConference = getIntent().getBooleanExtra("conference", false);

		//jack 2.4.51 version
		mViewPager = (ViewPager) findViewById(R.id.vp_viewpager);
		mIndicator = (TabPageIndicator) findViewById(R.id.tpi_Indicator);

		mCounts = (TextView) findViewById(R.id.tv_count);
		mCancel = (TextView) findViewById(R.id.tv_cancel);

		mCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (amperList.size()!=0||contactsList.size()!=0||callList.size()!=0) {
					friendsPager.destory();
					friendsPager.initData();
					amperList.clear();
					aireCallCount = 0;

					contactsPager.destory();
					phoneCount = 0;

					callList.clear();
					callCount = 0;
					mCounts.setText(0 + "");

				}
			}
		});

		if (isConference)
	    	((TextView)findViewById(R.id.topic)).setText(getString(R.string.conference));

        ((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
				finish();
    		}}
        );
        ((ImageView)findViewById(R.id.done)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			StringBuffer idxBuffer = new StringBuffer("");
    			int count=0;
    			for (int i=0;i<amperList.size();i++)
    			{
    				Map<String, String> map = amperList.get(i);
    				if (map.get("checked").equals("1"))
    				{
    					idxBuffer.append(map.get("idx")+" ");
    					count++;
    				}
    			}
    			if (count>0)
    			{
	    			Intent it=new Intent();
	    			it.putExtra("idx", idxBuffer.toString());
	    			setResult(RESULT_OK,it);
    			}
				finish();
    		}}
        );

		//检查是否是多方会议
		checkedConference();

		if (myAdapter == null) {
			myAdapter = new MyViewAdapter();
			mViewPager.setAdapter(myAdapter);
		}else{
			myAdapter.notifyDataSetChanged();
		}

		//jack bind indicator and viewpager
		mIndicator.setViewPager(mViewPager);
		mIndicator.setCurrentItem(0);

		//jack initdata
		ConferenceBasePager basePager = pageList.get(0);
		basePager.initData();

		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
			@Override
			public void onPageSelected(int position) {
				mIndicator.setCurrentItem(position);
				ConferenceBasePager basePager = pageList.get(position);
				// TODO: 2016/4/11 不是输入电话号码页面 就关闭软键盘
				if (position!=2)
					((ConferenceCallPager)pageList.get(2)).hideKeyBoard();
				basePager.initData();
			}
		});
	}

	private void checkedConference() {
		//tml*** beta ui, conference
		if (isConference) {
            ((Button) findViewById(R.id.bFafauser)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(PickupActivity.this, UsersActivity.class));
                    finish();
                }
            });
            ((Button) findViewById(R.id.bMessage)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(PickupActivity.this, MessageActivity.class));
                    finish();
                }
            });
            ((Button) findViewById(R.id.bSearch)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(PickupActivity.this, PublicWalkieTalkie.class));
                    finish();
                }
            });
            ((Button) findViewById(R.id.bAireCall)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(PickupActivity.this, SipCallActivity.class));
                    finish();
                }
            });
            ((Button) findViewById(R.id.bSetting)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(PickupActivity.this, SettingActivity.class));
                    finish();
                }
            });
            //tml*** beta ui2
            if (mPref.read("iso", "cn").equals("cn")) {
                ((Button) findViewById(R.id.bSearch)).setVisibility(View.GONE);
                ((Button) findViewById(R.id.bSetting)).setVisibility(View.GONE);
            } else {
                ((Button) findViewById(R.id.bSearch)).setVisibility(View.GONE);
                ((Button) findViewById(R.id.bSetting)).setVisibility(View.GONE);
            }
            boolean largeScreen = (findViewById(R.id.large) != null);
            if (largeScreen) {
                ((Button) findViewById(R.id.bSetting)).setVisibility(View.GONE);
            }

            ((ImageView) findViewById(R.id.done_conf)).setOnClickListener(new OnClickListener() {
                                                                              public void onClick(View v) {
																				  //airecall
																				  StringBuffer idxBuffer = new StringBuffer("");
																				  int count = 0;
																				  for (int i = 0; i < amperList.size(); i++) {
																					  Map<String, String> map = amperList.get(i);
																					  if (map.get("checked").equals("1")) {
																						  idxBuffer.append(map.get("idx") + " ");
																						  count++;
																					  }
																				  }
																				  if (count > 0) {
																					  try {
																						  chatroomMemberslist.clear();
																						  String idxArray = idxBuffer.toString();
																						  String[] items = idxArray.split(" ");
																						  for (int i = 0; i < items.length; i++) {
																							  int idx = Integer.parseInt(items[i]);
																							  if (idx < 50)
																								  continue;
																							  chatroomMemberslist.add(items[i]);
																						  }

																						  if (chatroomMemberslist.size() > 0 && chatroomMemberslist.size() <= 9) {
																							  mPref.write("incomingChatroom", false);
																							  mPref.write("ChatroomHostIdx", myIdx);
																							  new Thread(sendNotifyForJoinChatroom).start();
																						  }

																					  } catch (Exception e) {
																					  }
																				  }

																				  //jack phone call
																				  for (Person p : contactsList) {
																					  if (p.getChecked() == 1) {
																						  String address = null;
																						  try {
																							  MyTelephony.init(PickupActivity.this);
																							  String globalnumber = p.getAddress();
																							  if (!p.getAddress().startsWith("+")) {
																								  globalnumber = MyTelephony.addPrefixWithCurrentISO(p.getAddress());
																							  }
																							  address = MCrypt.bytesToHex(mc.encrypt(globalnumber));
																							  net.doAnyPostHttp("http://" + phpip
																											  + "/onair/conference/customer/addcallandroid.php",
																									  "room=" + room + "&ip=" + serverIP + "&callee="
																											  + address + "&pass=" + pass + "&user="
																											  + myUsername + "&userpw=" + myPasswd);
																						  } catch (Exception e) {
																							  e.printStackTrace();
																						  }
																					  }
																				  }

																				  //jack input call
																				  for (String globalnumber : callList) {
																					  try {
																						  String address = MCrypt.bytesToHex(mc.encrypt(globalnumber));
																						  net.doAnyPostHttp("http://" + phpip
																										  + "/onair/conference/customer/addcallandroid.php",
																								  "room=" + room + "&ip=" + serverIP + "&callee="
																										  + address + "&pass=" + pass + "&user="
																										  + myUsername + "&userpw=" + myPasswd);
																					  } catch (Exception e) {
																						  e.printStackTrace();
																					  }
																				  }
																				  if (callList.size() > 0 || amperList.size() > 0 || contactsList.size() > 0) {
																					  AireVenus.setCallType(AireVenus.CALLTYPE_CHATROOM);
																					  MakeCall.ConferenceCall(getApplicationContext(), myIdx + "");
																				  }
																			  }
																		  }
            );

            //jack
            titleList.add(getResources().getString(R.string.contacts));
            titleList.add(getResources().getString(R.string.call));
            contactsPager = new ConferenceContactsPager(PickupActivity.this);
            pageList.add(contactsPager);
            callPager = new ConferenceCallPager(PickupActivity.this);
            pageList.add(callPager);

            ((ImageView) findViewById(R.id.cancel)).setVisibility(View.GONE);
            ((ImageView) findViewById(R.id.done)).setVisibility(View.GONE);
            ((FrameLayout) findViewById(R.id.options)).setVisibility(View.VISIBLE);
            ((FrameLayout) findViewById(R.id.done_frame)).setVisibility(View.VISIBLE);
            mPref.write("LastPage", 0);
		} else {
            mIndicator.setVisibility(View.GONE);
			((ImageView) findViewById(R.id.cancel)).setVisibility(View.VISIBLE);
			((ImageView) findViewById(R.id.done)).setVisibility(View.VISIBLE);
            ((FrameLayout) findViewById(R.id.options)).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.tv_cancel)).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.tv_selected)).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.tv_count)).setVisibility(View.GONE);
            ((FrameLayout) findViewById(R.id.done_frame)).setVisibility(View.GONE);
        }
	}

	@Override
	protected void onDestroy() {
		if (mADB != null && mADB.isOpen())
			mADB.close();
		amperList.clear();
		for (ConferenceBasePager pager:pageList){
			pager.destory();
		}
		System.gc();
		System.gc();
		super.onDestroy();
	}

	//tml*** beta ui, conference
	Runnable sendNotifyForJoinChatroom = new Runnable() {
		public void run() {
			String myIdxHex = mPref.read("myID", "0");

			String ServerIP = mPref.read("conferenceSipServer", AireJupiter.myConfSipServer_default);
			if (AireJupiter.getInstance() != null) {
				ServerIP = AireJupiter.getInstance().getIsoConf(ServerIP);  //tml*** china ip
			}
			long ip = MyUtil.ipToLong(ServerIP);
			String HexIP = Long.toHexString(ip);

			String content = Global.Call_Conference + "\n\n" + HexIP + "\n\n" + myIdxHex;

			for (int i = 0; i < chatroomMemberslist.size(); i++)
			{
				int idx = Integer.parseInt(chatroomMemberslist.get(i));
				if (idx < 50) continue;

				String address = mADB.getAddressByIdx(idx);

				if (AireJupiter.getInstance() != null && AireJupiter.getInstance().tcpSocket != null)
				{
					if (AireJupiter.getInstance().tcpSocket.isLogged(false))
					{
						if (i > 0) MyUtil.Sleep(500);
						Log.d("voip.inviteConf1 " + address + " " + ServerIP + " " + content);
						AireJupiter.getInstance().tcpSocket.send(address, content, 0, null, null, 0, null);
					}
				}
			}
		}
	};
	//tml|bj*** neverdie
	public void neverSayNeverDie(Context context) {
		if (AireJupiter.getInstance()==null) {
			Log.e("AireJupiter is NULL");
			Intent vip1 = new Intent(context, AireVenus.class);
			context.stopService(vip1);
			Intent vip2 = new Intent(context, AireJupiter.class);
			context.stopService(vip2);

			Intent vip00 = new Intent(context, AireJupiter.class);
			context.startService(vip00);
		}
	}

	//jack adapter
	private class MyViewAdapter extends PagerAdapter{
		@Override
		public int getCount() {
			return titleList.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return titleList.get(position);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view==object;
		}

		//inflate
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			((ViewPager)container).addView(pageList.get(position).getRootView());
			return  pageList.get(position).getRootView();
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			(pageList.get(position)).releaseSrc();
			((ViewPager)container).removeView((View) object);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode==1){
			String iso = data.getStringExtra("iso");
			String country = data.getStringExtra("country");
			((ConferenceCallPager)pageList.get(2)).setISO(iso);
			((ConferenceCallPager)pageList.get(2)).setCountry(country);

		}
	}


	//jack aire统计
	public void setAireCall(List<Map<String, String>> amperList, int mCount){
		this.amperList = amperList;
		this.aireCallCount = mCount;
		mCounts.setText((phoneCount + mCount+callCount) + "");
	}

	public void setPhoneCall(List<Person> contacts, int mPhoneCount) {
		this.contactsList = contacts;
		this.phoneCount = mPhoneCount;
		mCounts.setText((mPhoneCount+aireCallCount+callCount)+"");
	}


	public void addPhoneCall(String globalnumber){
		if (!callList.contains(globalnumber)){
			callList.add(globalnumber);
			callCount++;
			mCounts.setText((phoneCount+aireCallCount+callCount)+"");
		}
	}
}