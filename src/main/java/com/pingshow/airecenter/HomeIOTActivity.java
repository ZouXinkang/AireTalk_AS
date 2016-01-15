package com.pingshow.airecenter;

import java.util.List;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.browser.MainBrowser;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.beehive.BeeHiveService;
import com.pingshow.homesafeguard.HomeSafeguardService;
import com.pingshow.homesafeguard.HomeSafeguardService.HomeSafeguadBinder;
import com.pingshow.homesafeguard.HomeSafeguardService.ReadDataListener;
import com.pingshow.homesafeguard.UsbData;
import com.pingshow.homesafeguard.UsbDevice;
import com.pingshow.homesafeguard.UsbDeviceMatcher;
import com.pingshow.iot.BlueComm;
import com.pingshow.iot.BlueComm.OnDataAvailableListener;
import com.pingshow.iot.BlueComm.OnServiceDiscoverListener;
import com.pingshow.iot.BlueDeviceListAdapter;
import com.pingshow.iot.Utils;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;
@SuppressLint("NewApi")
//tml*** blue io
public class HomeIOTActivity extends Activity {
	public static final boolean ShowIOTUI = true;
	
	public static HomeIOTActivity _this;
	public static HomeIOTActivity instance () {
		return _this;
	}
	
	private boolean bluetooth_support = true;

	private static final long SCAN_PERIOD = 30000;
	private static final int SCAN_PERIODi = (int) (SCAN_PERIOD / 1000);
	private boolean iniAcquiringHR = false;
	
	public static final String VEEPOO = "veepoo";
	public static final String VEEPOO_tag = "VEEPOO_BLUE";
	public static final int MAX_VEEPOO_DEVICES = 3;
	private boolean readPW_VEEPOO = false;
	public static boolean readHR_VEEPOO = false;
	public static final UUID VEEPOO_DEVICE_SEV = UUID
			.fromString("F0080001-0451-4000-B000-000000000000");
	public static final UUID VEEPOO_DEVICE_DATA = UUID
			.fromString("F0080002-0451-4000-B000-000000000000");
	public static final UUID VEEPOO_DEVICE_CONF = UUID
			.fromString("F0080003-0451-4000-B000-000000000000");
	public static final UUID VEEPOO_HR_SEV = UUID
			.fromString("F0040001-0451-4000-B000-000000000000");
	public static final UUID VEEPOO_HR_DATA = UUID
			.fromString("F0040002-0451-4000-B000-000000000000");
	public static final UUID VEEPOO_HR_CONF = UUID
			.fromString("F0040003-0451-4000-B000-000000000000");
	
	private int getVeepooSaveSpot (String addr) {
		for (int i = 0; i < MAX_VEEPOO_DEVICES; i++) {
			String device = mPref.read(VEEPOO_tag + i, "");
			if (addr != null) {
				if (device.contains(addr)) {
//					Log.d("myblue  getVeepooSaveSpot+ADDR=" + i);
					return i;
				}
			} else {
				if (device.length() == 0) {
					Log.d("myblue  getVeepooSaveSpot=" + i);
					return i;
				}
			}
		}
		Log.e("myblue  getVeepooSaveSpot(" + addr + ")=n/a");
		return -1;
	}
	
	private String[] getVeepooDeviceInfo (String addr) {
		String[] info = new String[3];
		for (int i = 0; i < MAX_VEEPOO_DEVICES; i++) {
			String device = mPref.read(VEEPOO_tag + i, "");
			if (device.contains(addr)) {
				info[0] = device;
				info[1] = mPref.read(VEEPOO_tag + i + "P", "");
				info[2] = mPref.read(VEEPOO_tag + i + "S", "");
				return info;
			}
		}
		Log.e("myblue  getVeepooDeviceInfo !@#$ invalid (" + addr + ")");
		return null;
	}

	public String uidName (UUID uuid) {
		String name = "";
		if (uuid.equals(VEEPOO_DEVICE_SEV)) {
			name = "VEEPOO_DEV_SEV";
		} else if (uuid.equals(VEEPOO_DEVICE_DATA)) {
			name = "VEEPOO_DEV_DATA";
		} else if (uuid.equals(VEEPOO_DEVICE_CONF)) {
			name = "VEEPOO_DEV_CONF";
		} else if (uuid.equals(VEEPOO_HR_SEV)) {
			name = "VEEPOO_HR_SEV";
		} else if (uuid.equals(VEEPOO_HR_DATA)) {
			name = "VEEPOO_HR_DATA";
		} else if (uuid.equals(VEEPOO_HR_CONF)) {
			name = "VEEPOO_HR_CONF";
		} else {
			name = uuid.toString();
		}
		return name;
	}
	
	private AmpUserDB mADB;
	private MyPreference mPref;
	
	int maskColor = 0xBBFF0000;
	
	private BluetoothAdapter mBluetoothAdapter;
	private BlueComm mBlueComm;
	private ListView deviceList_scan;
	private BlueDeviceListAdapter mBlueDeviceListAdapter;
	private boolean mScanning = false;
	private int scanTimer = 0;
	private ListView deviceList_active;
	
	static public HomeIOTActivity getInstance() {
		return _this;
	}
	
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		}
	};
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.homeiot_main);
		
		_this = this;
		Log.e("*** !!! HOMEIOT *** START START !!! ***");
		
		mPref = new MyPreference(this);

		boolean registered = mPref.readBoolean("AireRegistered", false);
		if (!registered) {
			Intent intent = new Intent();
			intent.setClass(HomeIOTActivity.this, SplashScreen.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			finish();
			return;
		}
		
		bluetooth_support = true;

		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Log.e("myblue not supported1");
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
			bluetooth_support = false;
		}

		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		if (mBluetoothAdapter == null) {
			Log.e("myblue not supported2");
			Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
			bluetooth_support = false;
		}

        mBlueComm = new BlueComm(this);
		if (!mBlueComm.initialize()) {
			Log.e("myblue  BlueComm initialize error");
			Toast.makeText(this, R.string.bluetooth_error, Toast.LENGTH_LONG).show();
			bluetooth_support = false;
			mBlueComm = null;
		}
		
		if (getIntent().getBooleanExtra("launchFromSelf", false))
			this.overridePendingTransition(R.anim.freeze, R.anim.freeze);
		
        neverSayNeverDie(_this);  //tml|bj*** neverdie/

	    mADB = new AmpUserDB(this);
		mADB.open();
		
		((TextView) findViewById(R.id.tab1)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				switchToPage(0);
				((TextView)findViewById(R.id.tab1)).requestFocus();
			}
	    });
		((TextView) findViewById(R.id.tab1)).requestFocus();
		
		((TextView) findViewById(R.id.tab2)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				switchToPage(1);
				((TextView) findViewById(R.id.tab2)).requestFocus();
			}
	    });
		
		((TextView) findViewById(R.id.tab3)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				switchToPage(2);
				((TextView)findViewById(R.id.tab3)).requestFocus();
			}
	    });
		
		((TextView) findViewById(R.id.tab4)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				switchToPage(3);
				((TextView)findViewById(R.id.tab4)).requestFocus();
			}
	    });
		//not finished
		((TextView) findViewById(R.id.tab3)).setVisibility(View.GONE);
		((TextView) findViewById(R.id.tab4)).setVisibility(View.GONE);
	    
		DialerFrame.setFrame(this, findViewById(android.R.id.content));
	    
////// SideBar
        
        ((ImageView)findViewById(R.id.bar1)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it = new Intent(HomeIOTActivity.this, MainActivity.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
        
        ((ImageView)findViewById(R.id.bar7)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it = new Intent(HomeIOTActivity.this, SecurityNewActivity.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
        
        ((ImageView)findViewById(R.id.bar9)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//you are here, do nothing
			}
		});
        
        ((ImageView)findViewById(R.id.bar8)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it = new Intent(HomeIOTActivity.this, MainBrowser.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
        
        ((ImageView)findViewById(R.id.bar6)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it = new Intent(HomeIOTActivity.this, ShoppingActivity.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
	    
        ((Button) findViewById(R.id.blue_scan)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mScanning) {
					scanBlueDevice(false);
				} else {
					scanBlueDevice(true);
				}
			}
		});

		boolean jupiter = mPref.readBoolean("JUPITERSTART_BLUE", true);
		if (jupiter) {
			for (int i = 0; i < MAX_VEEPOO_DEVICES; i++) {
				mPref.delect(VEEPOO_tag + i);
			}
			mPref.write("JUPITERSTART_BLUE", false);
			Log.i("myblue  list cleared");
		}
        
        deviceList_scan = (ListView) findViewById(R.id.devicelist_scan);
        mBlueDeviceListAdapter = new BlueDeviceListAdapter(this);
        deviceList_scan.setAdapter(mBlueDeviceListAdapter);
        deviceList_scan.setOnItemClickListener(OnDeviceListClick);

		if (bluetooth_support) {
			mBluetoothAdapter.enable();
			mBlueComm.setOnServiceDiscoverListener(mOnServiceDiscover);
			mBlueComm.setOnDataAvailableListener(mOnDataAvailable);
		}
		
		//li*** Home safeguard
		initHomeSafeguard();
	}

//li*** Home safeguard
private ListView lv_devices;
private UsbDeviceMatcher deviceMatcher;
private List<UsbDevice> devices;
private BaseAdapter deviceAdapter;
private String[] deviceCategories;
private boolean isOpenDeviceManger = false;
private HomeSafeguadBinder homeSafeguadBinder;
private ServiceConnection conn = new ServiceConnection() {

	@Override
	public void onServiceDisconnected(ComponentName name) {
		
	}
	
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		homeSafeguadBinder = (HomeSafeguadBinder) service;
	}
};
private void initHomeSafeguard() {
	Intent it = new Intent(getApplicationContext(), HomeSafeguardService.class);
	bindService(it, conn, Context.BIND_AUTO_CREATE);
	
	deviceMatcher = UsbDeviceMatcher.getInstance(getApplicationContext());
	
	
	findViewById(R.id.tab_safeguard).setOnClickListener(new OnClickListener() {
		@Override
		public void onClick(View v) {
			switchToPage(4);
			((TextView)findViewById(R.id.tab_safeguard)).requestFocus();
		}
	});
	
	CheckBox cb_open_service = (CheckBox)findViewById(R.id.cb_open_service);
	final Button btn_add_device = (Button)findViewById(R.id.btn_add_device);
	final Button btn_manager_device = (Button)findViewById(R.id.btn_manager_device);
	lv_devices = (ListView) findViewById(R.id.lv_device);

	//li*** Home safeguard, open service
	boolean isOpenSafeguard = mPref.readBoolean("openHomeSafeguard", false);
	cb_open_service.setChecked(isOpenSafeguard);
	btn_add_device.setEnabled(isOpenSafeguard);
	btn_manager_device.setEnabled(isOpenSafeguard);
	
	cb_open_service.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			mPref.write("openHomeSafeguard", isChecked);
			if(isChecked){
				homeSafeguadBinder.startService();
				lv_devices.setAdapter(deviceAdapter);
			}else{
				homeSafeguadBinder.stopService();
				lv_devices.setAdapter(null);
			}
			btn_add_device.setEnabled(isChecked);
			btn_manager_device.setEnabled(isChecked);
		}
	});

	//li*** Home safeguard, add device
	btn_add_device.setOnClickListener(new OnClickListener() {
		UsbData  _data;
		private AlertDialog dialog;
		@Override
		public void onClick(View v) {
			Builder ad = new AlertDialog.Builder(_this);
			View view = View.inflate(_this, R.layout.add_device_dialog, null);
			final TextView tv1 = (TextView) view.findViewById(R.id.tv1);
			final EditText et_name = (EditText) view.findViewById(R.id.et_name);
			final Spinner s_category = (Spinner) view.findViewById(R.id.s_category);
			final Button btn_confirm = (Button) view.findViewById(R.id.btn_confirm);
			final Button btn_cancel = (Button) view.findViewById(R.id.btn_cancel);
			final ReadDataListener readDataListener = new ReadDataListener() {
				@Override
				public void onReadData(UsbData data) {
					_data = data;
					tv1.setText(getString(R.string.device_found)+ Integer.toHexString(data.getAddress()));
				}
			};
			homeSafeguadBinder.registerReadDataListener(readDataListener);
			btn_confirm.setOnClickListener( new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(_data == null){
						Toast.makeText(getApplicationContext(), getString(R.string.device_not_found), 0).show();
						return;
					}
					int address = _data.getAddress();
					String name = et_name.getText().toString().trim();
					int category = s_category.getSelectedItemPosition();
					if(TextUtils.isEmpty(name) || category == AdapterView.INVALID_POSITION){
						Toast.makeText(getApplicationContext(), getString(R.string.device_empty_notify), 0).show();
						return;
					}
					UsbDevice device = new UsbDevice(category, name, address, _data,true);
					if(deviceMatcher.addUsbDevice(device)){  
						devices.add(device);
						Toast.makeText(getApplicationContext(), getString(R.string.device_add_success), 0).show();
						Log.d(getString(R.string.device_add)+": " + device.toString());
						_data = null;
						homeSafeguadBinder.unregisterReadDataListener(readDataListener);
						deviceAdapter.notifyDataSetChanged();
						dialog.dismiss();
					}else{
						Toast.makeText(getApplicationContext(), getString(R.string.device_exist), 0).show();
						Log.d(getString(R.string.device_exist));
					}
				}

			});
			btn_cancel.setOnClickListener( new OnClickListener() {
				@Override
				public void onClick(View v) {
					homeSafeguadBinder.unregisterReadDataListener(readDataListener);
					dialog.dismiss();
				}
			});
			ad.setView(view);
			dialog = ad.show();
		}
	});
	
	btn_manager_device.setOnClickListener(new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			isOpenDeviceManger = !isOpenDeviceManger;
			deviceAdapter.notifyDataSetChanged();
		}
	});

	//li*** Home safeguard, list of device
	devices = deviceMatcher.getAllUsbDevice();
	deviceAdapter = new DeviceAdapter();
	deviceCategories = getResources().getStringArray(R.array.device);
	lv_devices.setAdapter(deviceAdapter);
	
}
class DeviceAdapter extends BaseAdapter{

	@Override
	public int getCount() {
		return devices.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if(convertView == null){
			convertView = View.inflate(getApplicationContext(), R.layout.item_device, null);
			holder = new ViewHolder();
			holder.tv_device = (TextView) convertView.findViewById(R.id.tv_device);
			holder.tv_category = (TextView) convertView.findViewById(R.id.tv_category);
			holder.cb_open_device = (CheckBox) convertView.findViewById(R.id.cb_open_device);
			holder.btn_edit = (Button) convertView.findViewById(R.id.btn_edit);
			holder.btn_delete = (Button) convertView.findViewById(R.id.btn_delete);
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		final UsbDevice device = devices.get(position);
		holder.tv_device.setText(device.getName());
		holder.tv_category.setText(deviceCategories[device.getCategory()]);
		holder.cb_open_device.setChecked(device.isOpen());
		
		holder.cb_open_device.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				device.setOpen(isChecked);
				deviceMatcher.updateUsbDevice(device);
			}
		});
		
		holder.btn_edit.setOnClickListener(new OnClickListener() {
			private AlertDialog dialog;

			@Override
			public void onClick(View v) {
				final int address = device.getAddress();
				int category = device.getCategory();
				String name = device.getName();
				
				Builder ad = new AlertDialog.Builder(_this);
				View view = View.inflate(_this, R.layout.add_device_dialog, null);
				final TextView tv1 = (TextView) view.findViewById(R.id.tv1);
				final TextView tv_title = (TextView) view.findViewById(R.id.tv_title);
				final EditText et_name = (EditText) view.findViewById(R.id.et_name);
				final Spinner s_category = (Spinner) view.findViewById(R.id.s_category);
				final Button btn_confirm = (Button) view.findViewById(R.id.btn_confirm);
				final Button btn_cancel = (Button) view.findViewById(R.id.btn_cancel);
				
				tv1.setText(getString(R.string.device)+ Integer.toHexString(address));
				et_name.setText(name);
				s_category.setSelection(category);
				tv_title.setText(getString(R.string.device_edit));
				btn_confirm.setText(getString(R.string.device_edit));
				btn_confirm.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if (address == 0) {
							Toast.makeText(getApplicationContext(), getString(R.string.device_not_found), 0).show();
							return;
						}
						String name = et_name.getText().toString().trim();
						int category = s_category.getSelectedItemPosition();
						if(TextUtils.isEmpty(name) || category == AdapterView.INVALID_POSITION){
							Toast.makeText(getApplicationContext(), R.string.device_empty_notify, 0).show();
							return;
						}
						device.setCategory(category);
						device.setName(name);
						if(deviceMatcher.updateUsbDevice(device)){  
							Toast.makeText(getApplicationContext(),getString( R.string.device_edit_success), 0).show();
							Log.d(getString(R.string.device_edit_success)+": " + device.toString());
							deviceAdapter.notifyDataSetChanged();
							dialog.dismiss();
						}else{
							Toast.makeText(getApplicationContext(), getString(R.string.device_exist), 0).show();
							Log.d(getString(R.string.device_exist));
						}
					}
				});
				btn_cancel.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				});
				ad.setView(view);
				dialog = ad.show();
			}
		});
		holder.btn_delete.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				deviceMatcher.deleteUsbDevice(devices.remove(position).getAddress());
				deviceAdapter.notifyDataSetChanged();
			}
		});
		
		holder.cb_open_device.setVisibility(isOpenDeviceManger ? View.VISIBLE : View.GONE);
		holder.btn_delete.setVisibility(isOpenDeviceManger ? View.VISIBLE : View.GONE);
		holder.btn_edit.setVisibility(isOpenDeviceManger ? View.VISIBLE : View.GONE);
		
		return convertView;
	}
	
	class ViewHolder{
		public CheckBox cb_open_device;
		public Button btn_delete;
		public Button btn_edit;
		public TextView tv_device;
		public TextView tv_category;
	}
}
	private void switchToPage(int i) {
		((TextView) findViewById(R.id.tab1)).setBackgroundResource((i==0)?R.drawable.tab_static:R.drawable.optionbtn);
		((TextView) findViewById(R.id.tab2)).setBackgroundResource((i==1)?R.drawable.tab_static:R.drawable.optionbtn);
		((TextView) findViewById(R.id.tab3)).setBackgroundResource((i==2)?R.drawable.tab_static:R.drawable.optionbtn);
		((TextView) findViewById(R.id.tab4)).setBackgroundResource((i==3)?R.drawable.tab_static:R.drawable.optionbtn);
		
		((TextView) findViewById(R.id.tab1)).setTextColor((i==0)?0xff3997d5:0xff2e3545);
		((TextView) findViewById(R.id.tab2)).setTextColor((i==1)?0xff3997d5:0xff2e3545);
		((TextView) findViewById(R.id.tab3)).setTextColor((i==2)?0xff3997d5:0xff2e3545);
		((TextView) findViewById(R.id.tab4)).setTextColor((i==3)?0xff3997d5:0xff2e3545);

		((LinearLayout) findViewById(R.id.monitoring)).setVisibility((i==0)?View.VISIBLE:View.GONE);
		((LinearLayout) findViewById(R.id.devices)).setVisibility((i==1)?View.VISIBLE:View.GONE);
		((LinearLayout) findViewById(R.id.history_page)).setVisibility((i==2)?View.VISIBLE:View.GONE);
		((LinearLayout) findViewById(R.id.setting_page)).setVisibility((i==3)?View.VISIBLE:View.GONE);
		
		((TextView)findViewById(R.id.tab_safeguard)).setBackgroundResource((i==4)?R.drawable.tab_static:R.drawable.optionbtn);
		((TextView)findViewById(R.id.tab_safeguard)).setTextColor((i==4)?0xff3997d5:0xff2e3545);
		((LinearLayout)findViewById(R.id.home_safeguard_page)).setVisibility((i==4)?View.VISIBLE:View.GONE);
		
		((Button) findViewById(R.id.blue_scan)).setText(getString(R.string.blue_scan));
		((TextView) findViewById(R.id.blue_scan_status)).setText("");
		if (mBlueDeviceListAdapter != null)
			mBlueDeviceListAdapter.clear();
	}

    private OnItemClickListener OnDeviceListClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if (bluetooth_support) {
				final BluetoothDevice device = mBlueDeviceListAdapter.getDevice(position);
				if (device == null) {
					Log.e("myblue  selected device null?");
					return;
				}
				
				String dName = device.getName();
				String dAddr = device.getAddress();
				Log.i("myblue  selected device=" + dName + " " + dAddr);
				
				if (dName == null || dAddr == null) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(HomeIOTActivity.this, R.string.blue_unknown_device, Toast.LENGTH_SHORT).show();
						}
					});
					return;
				}
				
				dName = dName.toLowerCase();
				if (dName.equals(VEEPOO)) {
					int pos = getVeepooSaveSpot(dAddr.toLowerCase());
					if (pos != -1) {
						Log.e("myblue  already connected");
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(HomeIOTActivity.this, R.string.blue_alreadyconn_device, Toast.LENGTH_SHORT).show();
							}
						});
						return;
					}
					pos = getVeepooSaveSpot(null);
					if (pos == -1) {
						Log.e("myblue  too many active Veepoo devices");
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(HomeIOTActivity.this, R.string.blue_max_device, Toast.LENGTH_SHORT).show();
							}
						});
						return;
					}
				} else {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(HomeIOTActivity.this, R.string.blue_unknown_device, Toast.LENGTH_SHORT).show();
						}
					});
					return;
				}
				
				if (mScanning) {
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					mScanning = false;
				}

				mBlueComm.connect(dAddr);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_connecting_device));
					}
				});
			}
		}
	};
	
	private BlueComm.OnDataAvailableListener mOnDataAvailable = new OnDataAvailableListener() {
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			String dName = gatt.getDevice().getName();
			final String dAddr = gatt.getDevice().getAddress();
			UUID uuid = characteristic.getUuid();
			
			if (dName == null || dAddr == null || uuid == null) {
				Log.e("myblue  onData read n/a? " + dName + " " + dAddr + " " + uidName(uuid));
				return;
			}
			
			dName = dName.toLowerCase();
			if (dName.equals(VEEPOO)
					|| uuid.equals(VEEPOO_DEVICE_DATA)
					|| uuid.equals(VEEPOO_HR_DATA))
			{
				byte[] value = characteristic.getValue();
				if (VEEPOO_DEVICE_DATA.equals(uuid))
				{
					Log.d("myblue  onData " + dName + " (" + uidName(uuid) + ") read <- " + Utils.bytesToHexString(value));
					readPW_VEEPOO = false;
					if ((byte) 1 == value[3]) {  //01
						int pos = getVeepooSaveSpot(null);
						Log.i("myblue  verified! saving to " + VEEPOO_tag + pos);
						mPref.write(VEEPOO_tag + pos, dName + "\n" + dAddr);
						
						if (_this != null)
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(HomeIOTActivity.this, R.string.blue_connected_device, Toast.LENGTH_SHORT).show();
									((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_connected_device));
									
									String[] info = getVeepooDeviceInfo(dAddr);
									if (info != null) {
										((TextView) findViewById(R.id.heart_device1_name)).setText(info[0]);
										((TextView) findViewById(R.id.heart_device1_info2)).setText(getString(R.string.blue_acquiring_data));
									}
									if (_this != null)
										mHandler.postDelayed(new Runnable() {
											@Override
											public void run() {
												switchToPage(0);
											}
										}, 500);
								}
							});
						readHeartRate(gatt, dName);
					} else {
						if (_this != null)
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(HomeIOTActivity.this, R.string.blue_connectfail_veepoo, Toast.LENGTH_SHORT).show();
									((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_connectfail_veepoo));
								}
							});
					}
					
				}
				else if (VEEPOO_HR_DATA.equals(uuid))
				{
					String pulseData = Utils.bytesToHexString(value);
					if (pulseData.length() >= 6)
					{
						int pulse = Integer.parseInt(pulseData.substring(2, 4), 16);
						int pulsestatus = Integer.parseInt(pulseData.substring(4, 6), 16);
						Log.d("myblue  onData " + dName + " (" + uidName(uuid) + ") read <- " + pulseData + " > " + pulse + " -" + pulsestatus);
						int pos = getVeepooSaveSpot(dAddr);
						if (pos == -1) return;
						
						if (pulse == 0) {
							mPref.write(VEEPOO_tag + pos + "P", Integer.toString(pulse));
						} else if (pulse == 1) {
							mPref.write(VEEPOO_tag + pos + "P", getString(R.string.heart_error) + " E01");
						} else if (pulse == 2) {
							mPref.write(VEEPOO_tag + pos + "P", getString(R.string.heart_error) + " E02");
						} else if (pulse == 3) {
							mPref.write(VEEPOO_tag + pos + "P", getString(R.string.heart_abnormal));
						} else {
							mPref.write(VEEPOO_tag + pos + "P", Integer.toString(pulse));
						}
						
						if (pulsestatus == 0) {
							mPref.write(VEEPOO_tag + pos + "S", getString(R.string.heart_normal));
						} else {
							mPref.write(VEEPOO_tag + pos + "S", getString(R.string.heart_arrhythmia));
						}
						
						if (pulse == 0 && iniAcquiringHR) return;
						iniAcquiringHR = false;
						
						if (_this != null)
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									String[] info = getVeepooDeviceInfo(dAddr);
									if (info != null) {
										((TextView) findViewById(R.id.heart_device1_info)).setText(info[1]);
										((TextView) findViewById(R.id.heart_device1_info2)).setText("(" + info[2] + ")");
									}
								}
							});
					}
					else
					{
						Log.e("myblue  onData " + dName + " (" + uidName(uuid) + ") read ERROR? " + pulseData);
					}
				}
				else
				{
					Log.e("myblue  onData read n/a? " + dName + " " + uidName(uuid));
				}
			}
			else
			{
				Log.e("myblue  onData read ... unknown: " + dName + " " + uuid.toString());
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			String dName = gatt.getDevice().getName();
			UUID uuid = characteristic.getUuid();

			if (dName == null || uuid == null) {
				Log.e("myblue  onData write n/a? " + dName + " " + uidName(uuid));
				return;
			}
			
			dName = dName.toLowerCase();
			if (dName.equals(VEEPOO)
					|| uuid.equals(VEEPOO_DEVICE_CONF)
					|| uuid.equals(VEEPOO_HR_CONF))
			{
				iniAcquiringHR = true;
				byte[] value = characteristic.getValue();
				Log.d("myblue  onData " + dName + " (" + uidName(uuid) + ") write -> " + Utils.bytesToHexString(value));
			}
			else
			{
				Log.e("myblue  onData write ... unknown: " + dName + " " + uuid.toString());
			}
		}
	};
	
	private void scanBlueDevice(final boolean enable) {
		if (bluetooth_support) {
			if (enable) {
				Log.i("myblue  begin device scanning");
				scanTimer = SCAN_PERIODi;
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						Log.i("myblue  device scanning timeout");
						mScanning = false;
						mBluetoothAdapter.stopLeScan(mLeScanCallback);
					}
				}, SCAN_PERIOD);

				mScanning = true;
				mHandler.post(scanCountdown);
				mBluetoothAdapter.startLeScan(mLeScanCallback);
			} else {
				Log.i("myblue  stop device scanning");
				mHandler.removeCallbacks(scanCountdown);
				if (_this != null)
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							((Button) findViewById(R.id.blue_scan)).setText(getString(R.string.blue_scan));
							((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_scanstop));
						}
					});
				mScanning = false;
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
			}
		}
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mBlueDeviceListAdapter.addDevice(device);
					mBlueDeviceListAdapter.notifyDataSetChanged();
				}
			});
		}
	};

	private BlueComm.OnServiceDiscoverListener mOnServiceDiscover = new OnServiceDiscoverListener() {
		@Override
		public void onServiceDiscover(BluetoothGatt gatt) {
			String dName = gatt.getDevice().getName();
			
			if (dName == null) {
				Log.e("myblue  service discover ... unknown: " + dName);
				return;
			}
			
			dName = dName.toLowerCase();
			if (dName.equals(VEEPOO)
					|| dName.equals(VEEPOO))
			{
				checkPassword(gatt, dName);
			}
			else
			{
				Log.e("myblue  service discover ... unknown: " + dName);
			}
		}
	};
	
	private Runnable scanCountdown = new Runnable () {
		@Override
		public void run() {
			if (mScanning) {
				((Button) findViewById(R.id.blue_scan)).setText(Integer.toString(scanTimer));
				if (scanTimer == SCAN_PERIODi)
					((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_scanning));
				scanTimer--;
				if (scanTimer >= 0) mHandler.postDelayed(scanCountdown, 1000);
			} else {
				((Button) findViewById(R.id.blue_scan)).setText(getString(R.string.blue_scan));
				((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_scanstop));
			}
		}
	};

	private void checkPassword(BluetoothGatt gatt, String devName) {
		if (devName.equals(VEEPOO))
		{
			final BluetoothGattService service = gatt.getService(VEEPOO_DEVICE_SEV);
			BluetoothGattCharacteristic pswChar = service.getCharacteristic(VEEPOO_DEVICE_CONF);
			byte[] psw = {(byte) 0xa1, 0x00, 0x00, 0x00};
			Log.i("myblue  set pw " + VEEPOO + " -> " + Utils.bytesToHexString(psw));
			
			pswChar.setValue(psw);
			mBlueComm.writeCharacteristic(pswChar);
			
			new Thread(new Runnable() {

				@Override
				public void run() {
					readPW_VEEPOO = true;
					BluetoothGattCharacteristic data = service.getCharacteristic(VEEPOO_DEVICE_DATA);

					Log.i("myblue  confirming pw: " + VEEPOO);
					if (_this != null)
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								((TextView) findViewById(R.id.blue_scan_status)).setText(getString(R.string.blue_verifying_device));
							}
						});
					
					boolean readok = true;
					while (readPW_VEEPOO && readok) {
						if (!readPW_VEEPOO) break;
						MyUtil.Sleep(2000);
						if (!readPW_VEEPOO) break;
						readok = mBlueComm.readCharacteristic(data);
					}
					
					Log.i("myblue  confirm pw - finished: " + VEEPOO + "  /" + readPW_VEEPOO + "/" + readok);
				}
			}).start();
		}
	}

	private void readHeartRate(BluetoothGatt gatt, String devName) {
		if (devName.equals(VEEPOO))
		{
			final BluetoothGattService gattService = gatt.getService(VEEPOO_HR_SEV);
			
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (readHR_VEEPOO) return;
					readHR_VEEPOO = true;
					BluetoothGattCharacteristic config = gattService.getCharacteristic(VEEPOO_HR_CONF);
					BluetoothGattCharacteristic data = gattService.getCharacteristic(VEEPOO_HR_DATA);
					byte[] value = new byte[2];
					value[0] = (byte) 0xaa;
					value[1] = (byte) 0x01;
					Log.i("myblue  request data " + VEEPOO + " -> " + Utils.bytesToHexString(value));
					
					config.setValue(value);
					mBlueComm.writeCharacteristic(config);
					
					boolean readok = true;
					while (readHR_VEEPOO && readok) {
						if (!readHR_VEEPOO) break;
						MyUtil.Sleep(1000);
						if (!readHR_VEEPOO) break;
						readok = mBlueComm.readCharacteristic(data);
					}

					Log.i("myblue  request data - finished: " + VEEPOO + "  /" + readHR_VEEPOO + "/" + readok);
				}
			}).start();
		}
	}
	
	public void onDisconnected (String addr, int pos) {
		String[] info = getVeepooDeviceInfo(addr);
		
		if (info[0].contains(VEEPOO + "0") || pos == 0) {
			mHandler.postDelayed(new Runnable () {
				@Override
				public void run() {
					((TextView) findViewById(R.id.heart_device1_info)).setText("--");
					((TextView) findViewById(R.id.heart_device1_info2)).setText(getString(R.string.blue_disconnected_device));
				}
			}, 500);
		}
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		scanBlueDevice(false);
	}
	
	@Override
	public void onDestroy() {
		mHandler.removeCallbacks(scanCountdown);
		
		scanBlueDevice(false);
		
		try {
			unregisterReceiver(HomeController);
		} catch (IllegalArgumentException e) {
		}
		
		if (mADB != null && mADB.isOpen())
			mADB.close();
		
		_this = null;
		
		super.onDestroy();
		unbindService(conn);
	}
	
    @Override
    protected void onNewIntent(Intent intent) {
    	//required to update NEW intents
        super.onNewIntent(intent);
        setIntent(intent);
    }
    
	@Override
    protected void onResume() {
    	super.onResume();
    	_this = this;
    	DialerFrame.checkEmbeddedDialer(findViewById(android.R.id.content));
		IntentFilter ifilter = new IntentFilter();
		this.registerReceiver(HomeController, ifilter);
		deviceAdapter.notifyDataSetChanged();  //li*** Home safeguard
	}
	
	@Override  
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	BroadcastReceiver HomeController = new BroadcastReceiver() {
		@Override
	    public void onReceive(Context context, Intent intent) {
			String iAction = intent.getAction();
		}
	};
	
	public void close() {
		finish();
	}
	
	public FrameLayout getNotificationLayout() {
		return (FrameLayout) findViewById(R.id.notification);
	}
	
	//tml|bj*** neverdie
	public void neverSayNeverDie(Context context) {
		if (AireJupiter.getInstance()==null) {
			Log.e("AireJupiter.getInstance() is null, RESETTING");
			Intent vip0 = new Intent(context, BeeHiveService.class);
			context.stopService(vip0);
			Intent vip1 = new Intent(context, AireVenus.class);
			context.stopService(vip1);
			Intent vip2 = new Intent(context, AireJupiter.class);
			context.stopService(vip2);
			
			Intent vip00 = new Intent(context, AireJupiter.class);
			context.startService(vip00);
		}
	}
	
}