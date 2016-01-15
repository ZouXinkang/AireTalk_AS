package com.pingshow.airecenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnGenericMotionListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.browser.MainBrowser;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.SmsDB;
import com.pingshow.beehive.BeeHiveService;
import com.pingshow.network.MyNet;
import com.pingshow.network.MySocket;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;
import com.pingshow.voip.VideoCallActivity;
import com.pingshow.voip.VideoConf;
import com.pingshow.voip.core.VoipCall;
import com.pingshow.voip.core.VoipCore;

public class MainActivity extends Activity {
	
	static public MainActivity _this;
	
	private SearchPage _searchPage;
	private GroupPage _groupPage;
	private UserPage _userPage;
	private AireCallPage _aireCallPage;
	private PickupPage _pickuppage;
	private SettingPage _settingPage;

	public static String actionPhoneNumber;
	
	private int currentIndex=-1;
	private View currentView;
	
	private Handler mHandler=new Handler();
	
	MyPreference mPref;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		if (getIntent().getBooleanExtra("launchFromSelf", false))
			this.overridePendingTransition(R.anim.freeze, R.anim.freeze);
		
		_this=this;
		Log.e("MainActivity _this SET1");
		neverSayNeverDie(_this, 0);  //tml|bj*** neverdie/
		mPref = new MyPreference(this);
		
		int jump = getIntent().getIntExtra("switchToInflate", 0);
		//tml*** conference shortcut
		boolean goConference = getIntent().getBooleanExtra("goConference", false);
		if (goConference) {
			jump = 4;
		}
		Log.e("*** !!! MAIN *** START START !!! *** " + jump + goConference);
		switchInflater(jump);
        
        ((Button)findViewById(R.id.back)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		        neverSayNeverDie(_this, 2);  //tml|bj*** neverdie/
				switchInflater(0);
			}
		});
		
		DialerFrame.setFrame(this, findViewById(android.R.id.content));
        
        ////// SideBar
        
        ((ImageView)findViewById(R.id.bar1)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				neverSayNeverDie(_this, 0);  //tml|bj*** neverdie/
				((DrawerLayout) findViewById(R.id.main_content)).closeDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));  //tml*** beta ui2
				switchInflater(0);
			}
		});
      //tml*** beta ui2 X
//        ((ImageView)findViewById(R.id.bar6)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Intent it=new Intent(MainActivity.this, ShoppingActivity.class);
//				it.putExtra("launchFromSelf", true);
//				startActivity(it);
//				finish();
//			}
//		});
        ((ImageView)findViewById(R.id.bar7)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it=new Intent(MainActivity.this, SecurityNewActivity.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
        //tml*** beta ui2
        ((ImageView)findViewById(R.id.bar9)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//tml*** alpha iot ui
//				Intent it=new Intent(MainActivity.this, HomeIOTActivity.class);
//				it.putExtra("launchFromSelf", true);
//				startActivity(it);
//				finish();
		        neverSayNeverDie(_this, 2);  //tml|bj*** neverdie/
				((DrawerLayout) findViewById(R.id.main_content)).closeDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));  //tml*** beta ui2
				switchInflater(5);
			}
		});
        ((ImageView)findViewById(R.id.bar9)).setImageAlpha(150);  //temp until icon-faded available
      //tml*** beta ui2 X
//        ((ImageView)findViewById(R.id.bar8)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
////				Intent it=new Intent(MainActivity.this, LocationSettingActivity.class);
//				Intent it=new Intent(MainActivity.this, MainBrowser.class);  //tml*** browser save/
//				it.putExtra("launchFromSelf", true);
//				startActivity(it);
//				finish();
//			}
//		});
        //tml*** beta ui2
        ((RelativeLayout) findViewById(R.id.sidebar_ghost)).setOnGenericMotionListener(sideBarMotionListener);
        ((RelativeLayout) findViewById(R.id.sidebar_frame_drawer)).setOnGenericMotionListener(sideBarMotionListener2);
        ((ImageView) findViewById(R.id.menu_main)).setOnClickListener(new OnClickListener () {
			@Override
			public void onClick(View v) {
				((DrawerLayout) findViewById(R.id.main_content)).openDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));
			}
        });
        
        //tml*** cec
        mHandler.postDelayed(new Runnable () {
			@Override
			public void run() {
				boolean input = mPref.readBoolean("HDMIctrl_input", true);
				boolean tvon = mPref.readBoolean("HDMIctrl_tv", true);
				Log.d("check HDMI " + input + tvon);
				if ((input || tvon) && AireJupiter.getInstance() != null) {
					AireJupiter.hdmiCmdExecSetCEC();
				}
			}
        }, 2000);
	}
	
	public FrameLayout getNotificationLayout()
	{
		return (FrameLayout)findViewById(R.id.notification);
	}
	
	public void switchInflater(int index)
	{
		Log.d("tml switchInflater(" + currentIndex + " " + index + ")");
		if (currentIndex==index) return;

		if (currentView!=null)
		{
			Log.i("tml switchtag, was " + currentView.getTag().toString());
			Page page=(Page)currentView.getTag();
			page.destroy();
			((FrameLayout)currentView.getParent()).removeAllViews();
			currentView=null;
		}
		
		System.gc();
		System.gc();
		
		if (index==0)
			loadAsUserPage();
		else if (index==1)
			loadAsSerchPage();
		else if (index==2)
			loadAsCreateGroupPage();
		else if (index==3)
			loadAsAireCallPage();
		else if (index==4)
			loadAsConferencePage();
		else if (index==5)
			loadAsSettingPage();

		//tml*** beta ui2 X
//		((Button) findViewById(R.id.back)).setVisibility((index == 0) ? View.GONE : View.VISIBLE);
		//tml*** alpha ui
		if (index == 0) {
			((Button) findViewById(R.id.back)).setVisibility(View.GONE);
		} else if (index == 5) {
			((Button) findViewById(R.id.back)).setVisibility(View.GONE);
		} else {
			((Button) findViewById(R.id.back)).setVisibility(View.VISIBLE);
		}
		//***tml
		
		currentIndex=index;
		
//		refreshTab();  //tml*** beta ui2 X
	}
	
	private void refreshTab()
	{
//		((CheckedTextView)findViewById(R.id.add)).setChecked(currentIndex==1);
		//tml|phoebe*** alpha ui/
//		((CheckedTextView)findViewById(R.id.add)).setChecked(currentIndex==0 || currentIndex==1);
//		((CheckedTextView)findViewById(R.id.add_group)).setChecked(currentIndex==2);
//		((CheckedTextView)findViewById(R.id.airecall)).setChecked(currentIndex==3);
//		((CheckedTextView)findViewById(R.id.conference)).setChecked(currentIndex==4);
//		((CheckedTextView)findViewById(R.id.setting)).setChecked(currentIndex==5);
	}
	
	private void loadAsUserPage()
	{
//		((TextView)findViewById(R.id.topic)).setText(R.string.friends);
		((TextView)findViewById(R.id.topic)).setText(R.string.bar1_name);  //tml*** beta ui2
		FrameLayout content=(FrameLayout) findViewById(R.id.content);
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		currentView = inflater.inflate(R.layout.inflate_users, null, false);
        content.addView(currentView);
        _userPage=new UserPage(currentView);
		currentView.setTag(_userPage);
	    UserPage.forceRefresh = true;
	    Intent intent = new Intent(Global.Action_Refresh_Gallery);
	    sendBroadcast(intent);
	}
	
	private void loadAsSerchPage()
	{
		((TextView)findViewById(R.id.topic)).setText(R.string.add_contact);
		FrameLayout content=(FrameLayout) findViewById(R.id.content);
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		currentView = inflater.inflate(R.layout.inflate_search, null, false);
        content.addView(currentView);
        _searchPage=new SearchPage(currentView);
		currentView.setTag(_searchPage);
	}
	
	private void loadAsCreateGroupPage()
	{
		((TextView)findViewById(R.id.topic)).setText(R.string.create_group);
		FrameLayout content=(FrameLayout) findViewById(R.id.content);
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		currentView = inflater.inflate(R.layout.inflate_group, null, false);
        content.addView(currentView);
        _groupPage=new GroupPage(currentView);
		currentView.setTag(_groupPage);
	}
	
	private void loadAsAireCallPage()
	{
		((TextView)findViewById(R.id.topic)).setText(R.string.aire_call);
		FrameLayout content=(FrameLayout) findViewById(R.id.content);
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		currentView = inflater.inflate(R.layout.inflate_airecall, null, false);
        content.addView(currentView);
        _aireCallPage=new AireCallPage(currentView);
		currentView.setTag(_aireCallPage);
	}
	
	//yang
	private void loadAsConferencePage()
	{
		((TextView)findViewById(R.id.topic)).setText(R.string.conference);
		FrameLayout content=(FrameLayout) findViewById(R.id.content);
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		currentView = inflater.inflate(R.layout.inflate_conf_page, null, false);
        content.addView(currentView);
        _pickuppage=new PickupPage(currentView);
		currentView.setTag(_pickuppage);
	}
	
	private void loadAsSettingPage()
	{
//		((TextView)findViewById(R.id.topic)).setText(R.string.setting);
		((TextView)findViewById(R.id.topic)).setText(R.string.bar9_name);  //tml*** beta ui2
		FrameLayout content=(FrameLayout) findViewById(R.id.content);
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		currentView = inflater.inflate(R.layout.inflate_setting, null, false);
        content.addView(currentView);
        _settingPage=new SettingPage(currentView);
		currentView.setTag(_settingPage);	
	}
	
	
	@Override  
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (currentView != null)
		{
			Page page = (Page) currentView.getTag();
			if (page.getName().endsWith("UserPage")) {
				((UserPage) page).onActivityResult(requestCode, resultCode, data);
			} else if (page.getName().endsWith("SettingPage")) {
				((SettingPage) page).onActivityResult(requestCode, resultCode, data);
			} else if (page.getName().endsWith("SearchPage")) {  //tml|li*** qr addf
				((SearchPage) page).onActivityResult(requestCode, resultCode, data);
			}
		}
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		int source = event.getSource();
		int bstate = event.getButtonState();
		int devid = event.getDeviceId();
		if (action == MotionEvent.ACTION_DOWN)
			Log.d("tmltest onTouch Source=" + source + " ButtonState=" + bstate + " DevID=" + devid);
		return false;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		int action = event.getAction();
		int source = event.getSource();
		int hwscan = event.getScanCode();
		int devid = event.getDeviceId();
		int label = event.getDisplayLabel();
		if (action == MotionEvent.ACTION_DOWN)
			Log.d("test onKeyDown KeyCode=" + keyCode + " Source=" + source + " HWScan=" + hwscan + " DevID=" + devid + " Label=" + label);
		if (keyCode==KeyEvent.KEYCODE_BACK)
        {
			//tml*** backpress fix
			if (UserPage.onBackPressed()) {
				return true;
			} else if (currentIndex != 0) {
				switchInflater(0);
				return true;
			}
			super.onBackPressed();
    		return true;
        }
		if (currentView!=null)
		{
			Page page=(Page)currentView.getTag();
			if (page.getName().endsWith("AireCallPage"))
			{
				return ((AireCallPage)page).onKeyDown(keyCode, event);
			}
		}
		return false;
	}
	
	@Override
    protected void onRestart() {
    	super.onRestart();
    	
    	if (currentView!=null)
		{
			Page page=(Page)currentView.getTag();
			if (page.getName().endsWith("AireCallPage"))
			{
				((AireCallPage)page).onRestart();
			}
		}
    }

    //tml*** phone intent
    @Override
    protected void onNewIntent(Intent intent) {
    	//required to update NEW intents
        super.onNewIntent(intent);
        setIntent(intent);
    }
    
	@Override
    protected void onResume() {
    	super.onResume();
    	_this=this;
    	Log.e("MainActivity _this SET2");
    	DialerFrame.checkEmbeddedDialer(findViewById(android.R.id.content));
    	
    	if (currentView!=null)
		{
			Page page=(Page)currentView.getTag();
			if (page.getName().endsWith("UserPage"))
			{
				((UserPage)page).onResume();
			}
		}
        //tml*** phone intent
		if (getIntent().getData() != null) {
            String phoneNumber = getIntent().getData().toString();
            
            if (phoneNumber.startsWith("tel:")) {
            	phoneNumber = phoneNumber.replace("tel:","");
            } else if (phoneNumber.startsWith("voicemail:")) {
            	phoneNumber = phoneNumber.replace("voicemail:","");
            }
            phoneNumber = MyTelephony.cleanPhoneNumber2(phoneNumber);
            
            if (MyTelephony.isPhoneNumber(phoneNumber)) {
                actionPhoneNumber = phoneNumber;
            } else {
                actionPhoneNumber = null;
            }
        	getIntent().setData(null);
            Log.d("AireCall handling1 ph# " + phoneNumber + " > " + actionPhoneNumber);
        } else {
        	actionPhoneNumber = null;
        }
		if (actionPhoneNumber != null) {
			//tml|sw*** no airecall in china
			boolean isCN = MyUtil.isISO_China(_this, mPref, null);
			if (AireCallPage.OverrideShowAireCall) isCN = false;
			if (isCN) {
				actionPhoneNumber = MyTelephony.cleanPhoneNumber3(actionPhoneNumber);
	            UserPage.passKeyword = actionPhoneNumber;
	            switchInflater(1);
			} else {
				switchInflater(3);
			}
		}
		//***tml
		//tml*** return Dialer view
		if ((AireVenus.instance() != null) && (AireVenus.callstate_AV != null)) {
			Log.d("Main RESUME >> " + AireVenus.callstate_AV);
			if (AireVenus.callstate_AV.equals(VoipCall.State.Connected.toString())
					|| AireVenus.callstate_AV.equals(VoipCall.State.IncomingReceived.toString())) {
				Intent reconn = new Intent();
				VoipCore lVoipCore = AireVenus.instance().getVoipCore();
				boolean isVideoCall = lVoipCore.getVideoEnabled();
				if (DialerActivity.getDialer() == null) {
					Log.e("dialer never created, resume creation");
					String IncomingNumber = lVoipCore.getRemoteAddress().getUserName();
					reconn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					reconn.setClass(this, DialerActivity.class);
					reconn.putExtra("incomingCall", true);
					reconn.putExtra("PhoneNumber", IncomingNumber);
					reconn.putExtra("VideoCall", isVideoCall);
					startActivity(reconn);
				} else {
					Log.e("dialer resume");
					reconn.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					if (isVideoCall) {
						reconn.setClass(this, VideoCallActivity.class);
					} else {
						reconn.setClass(this, DialerActivity.class);
					}
					startActivity(reconn);
				}
			}
		}
		//***tml
	}
	
	Runnable refreshStatus=new Runnable()
	{
		public void run()
		{
			if ((DialerActivity.minimized && DialerActivity.getDialer()!=null) || VideoCallActivity.minimized)
			{
				LinearLayout oncallView=(LinearLayout) findViewById(R.id.oncall);
	    		((TextView)oncallView.findViewById(R.id.displayname)).setText(DialerActivity.getDialer().getCurrentOnCallName());
	    		((TextView)oncallView.findViewById(R.id.status)).setText(DialerActivity.getDialer().getCurrentOnCallStatus());
				mHandler.postDelayed(refreshStatus, 1000);
			}
			else
			{
				LinearLayout oncallView=(LinearLayout) findViewById(R.id.oncall);
				oncallView.setVisibility(View.GONE);
			}
		}
	};

	@Override
	protected void onDestroy() {
		if (currentView!=null)
		{
			Page page=(Page)currentView.getTag();
			page.destroy();
			((FrameLayout)currentView.getParent()).removeAllViews();
			currentView=null;
		}
		_this=null;
		super.onDestroy();
	}
	//tml*** preAV reg
	public void quitPreServiceY() {
		if (AireVenus.instance() != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					AireVenus.instance().quitServiceY();
				}
			});
		}
	}
	
	public void close()
	{
		finish();
	}
	
	//tml|bj*** neverdie
	public void neverSayNeverDie(Context context, int mode) {
		if (mode == 0 || mode == 1) {
			if (AireJupiter.getInstance() == null) {
				Log.e("MAIN AireJupiter.getInstance() is null, RESETTING");
				Intent vip0 = new Intent(context, BeeHiveService.class);
				context.stopService(vip0);
				Intent vip1 = new Intent(context, AireVenus.class);
				context.stopService(vip1);
				Intent vip2 = new Intent(context, AireJupiter.class);
				context.stopService(vip2);

				Intent vip00 = new Intent(context, AireJupiter.class);
				context.startService(vip00);
//				restartMain(this, 500);
			}
		}
		if (mode == 0 || mode == 2) {
			mHandler.postDelayed(new Runnable() {
				public void run() {
					UserPage.forceRefresh = true;
					Intent it = new Intent(Global.Action_Friends_Status_Updated);
					sendBroadcast(it);
					Log.i("tml aloha refresh friends online");
				}
			}, 1000);
		}
	}
	public void restartMain(Context context, int delay) {
	    if (delay == 0) {
	        delay = 1;
	    }
	    Log.e("MAIN restarting app");
	    Intent restartIntent = new Intent(context, MainActivity.class);
	    PendingIntent intent = PendingIntent.getActivity(context, 0,
	    		restartIntent, Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	    manager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, intent);
	    System.exit(2);
	}
	//***tml
	//tml|sw*** no airecall in china
//	public void showAirecall(boolean visible) {
//		if (!visible) {
//			((CheckedTextView) findViewById(R.id.airecall)).setVisibility(View.GONE);
//		} else {
//			((CheckedTextView) findViewById(R.id.airecall)).setVisibility(View.VISIBLE);
//		}
//	}
	//tml*** beta ui2
	private OnGenericMotionListener sideBarMotionListener = new OnGenericMotionListener() {
		@Override
		public boolean onGenericMotion(View v, MotionEvent event) {
			int action = event.getAction();
			int source = event.getSource();
			
			if (source == InputDevice.SOURCE_MOUSE) {
				switch (action) {
					case MotionEvent.ACTION_HOVER_ENTER:
						if (v.getId() == R.id.sidebar_ghost) {
							Log.d("aloha mouse onto sidebar zone");
							if (!((DrawerLayout) findViewById(R.id.main_content)).isDrawerOpen((RelativeLayout) findViewById(R.id.sidebar_frame_drawer))) {
								((DrawerLayout) findViewById(R.id.main_content)).openDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));
							}
						}
						break;
					case MotionEvent.ACTION_HOVER_EXIT:
//						if (v.getId() == R.id.sidebar_ghost) {
//							Log.d("aloha mouse left sidebar zone");
//							if (((DrawerLayout) findViewById(R.id.main_content)).isDrawerOpen((RelativeLayout) findViewById(R.id.sidebar_frame_drawer))) {
//								mHandler.postDelayed(new Runnable() {
//									@Override
//									public void run() {
//										((DrawerLayout) findViewById(R.id.main_content)).closeDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));
//									}
//								}, 500);
//							}
//						}
						break;
				}
			}
			return false;
		}
	};
	private OnGenericMotionListener sideBarMotionListener2 = new OnGenericMotionListener() {
		@Override
		public boolean onGenericMotion(View v, MotionEvent event) {
			int action = event.getAction();
			int source = event.getSource();
			
			if (source == InputDevice.SOURCE_MOUSE) {
				switch (action) {
					case MotionEvent.ACTION_HOVER_EXIT:
						if (v.getId() == R.id.sidebar_frame_drawer) {
							Log.d("aloha mouse left sidebar zone");
							mHandler.postDelayed(new Runnable() {
								@Override
								public void run() {
									if (((DrawerLayout) findViewById(R.id.main_content)).isDrawerOpen((RelativeLayout) findViewById(R.id.sidebar_frame_drawer))) {
										((DrawerLayout) findViewById(R.id.main_content)).closeDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));
									}
								}
							}, 500);
						}
						break;
				}
			}
			return false;
		}
	};
}
