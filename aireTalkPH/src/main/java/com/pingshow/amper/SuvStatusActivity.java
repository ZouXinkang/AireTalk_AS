package com.pingshow.amper;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.network.MyNet;
import com.pingshow.util.MyUtil;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
//tml*** iot status
public class SuvStatusActivity extends Activity {
	
	private MyPreference mPref;
	private Handler mHandler = new Handler();
	private ProgressDialog progressDialog;

	private String mIdx;
	private String mAddress;
	private String mNickname;
	private AmpUserDB mADB;
	
	private BaseAdapter deviceAdapter;
	private ListView deviceList;
	private static List<Map<String, String>> suvDevices;
	private boolean retrieveOk = true;
	private boolean retrieveOk2 = false;
	private String mReturn = "";
	private String date = "";
	
	private final String STATUS_OK_CODE = "0";
	private final String STATUS_ALERT_CODE = "1";
	private final String STATUS_OFF_CODE = "2";
	private final String STATUS_OK_CLR = "#00FF00";
	private final String STATUS_ALERT_CLR = "#FF0000";
	private final String STATUS_OFF_CLR = "#DDDDDD";
	private final String STATUS_UNKN_CLR = "#999999";
	private final String DEV_NAME = "device_name";
	private final String DEV_CAT = "device_category";
	private final String DEV_STAT = "device_status";
	
	private static SuvStatusActivity _this;
	public static SuvStatusActivity getInstance() {
		return _this;
	}
	
	@Override
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
		setContentView(R.layout.suv_status_page);
		this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
		_this = this;
		
		Intent intent = getIntent();
		mAddress = intent.getStringExtra("SendeeNumber");
		mNickname = intent.getStringExtra("SendeeDisplayname");
		
		mPref = new MyPreference(this);
		mADB = new AmpUserDB(SuvStatusActivity.this);
		mADB.open();
		
		int idx = mADB.getIdxByAddress(mAddress);
		mIdx = Integer.toHexString(idx);
		
		if (suvDevices == null) suvDevices = new ArrayList<Map<String, String>>();
		deviceList = (ListView) findViewById(R.id.suv_device_list);
		deviceAdapter = new DeviceAdapter(this);
		deviceList.setAdapter(deviceAdapter);
		
		((TextView) findViewById(R.id.topic)).setText(mNickname);
		((TextView) findViewById(R.id.suv_status_info)).setText(getString(R.string.suv_status));
		
        ((ImageView) findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
				finish();
    		}}
        );

        ((ImageView) findViewById(R.id.refresh)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    	        new Thread(getSuvDeviceInfo).start();
    		}}
        );
        
        //test
        new Thread(getSuvDeviceInfo).start();
	}
	
	@Override
	protected void onDestroy() {
		_this = null;
		if (mADB != null && mADB.isOpen()) 
			mADB.close();
		super.onDestroy();
	}
	
	private Runnable getSuvDeviceInfo = new Runnable() {
		@Override
		public void run() {
			runOnUiThread(popupProgressDialog);

	        date = MyUtil.getDate(3);
	        
	        retrieveOk = false;

			String Return = "";
			
			//test
//			Return = "success:10;;"
//					+ "testname1:testcat1:0;"
//					+ "testname2:testcat2:1;"
//					+ "testname3:testcat3;"
//					+ "testname4:testcat4:   test4;"
//					+ "testname5: :2;"
//					+ "qwqweqwe;"
//					+ ":0;"
//					+ ":0;"
//					+ ":0";
//			retrieveOk = true;
			
			try {
				if (AireJupiter.getInstance() != null) {
					int count = 0;
					MyNet net = new MyNet(SuvStatusActivity.this);
					String domain = AireJupiter.getInstance().getIsoDomain();  //tml*** china ip
					String idx = URLEncoder.encode(mIdx, "UTF-8");
					
					do {
						Return = net.doAnyPostHttps("https://" + domain + "/webcall/acphp/iotstatus.php",
								"idx=" + idx);
						if (Return.startsWith("success")) break;
						MyUtil.Sleep(500);
					} while (++count < 3);
				}
				
				retrieveOk = Return.startsWith("success:");
				
			} catch (Exception e) {
				Log.e("uploadSuvStatus !@#$ " + e.getMessage());
			}
	        
			if (retrieveOk) {
				Return = Return.replaceFirst("success:", "");
				if (Return.equals(";;:;:;:;:;:;:;:;:;:") || Return.startsWith(";;") || Return.startsWith("0;;"))
					Return = "";
				Return = date + "<d>" + Return;
			} else {
				Return = mPref.read("SUVSTATUS", "");
				if (Return.length() == 0)
					Return = date + "<d>";
			}
			mPref.write("SUVSTATUS", Return);

			mReturn = Return.substring(Return.indexOf("<d>") + "<d>".length());

	        runOnUiThread(loadSuvDeviceInfo);
			
	        runOnUiThread(dismissProgressDialog);
		}
	};

	private Runnable loadSuvDeviceInfo = new Runnable() {
		@Override
		public void run() {
	        suvDevices.clear();
	        
			retrieveOk2 = parseSuvDevicePHP(mReturn);
	        deviceAdapter.notifyDataSetChanged();

			String status_info = "";
			String status_date = mPref.read("SUVSTATUS", date + "<d>");
			
	        if (retrieveOk2) {
	        	((TextView) findViewById(R.id.suv_status_info)).setTextColor(R.color.darkgray);
	        	status_info = getString(R.string.suv_status);
	        } else {
	        	((TextView) findViewById(R.id.suv_status_info)).setTextColor(Color.RED);
	        	if (status_date.substring(status_date.indexOf("<d>") + "<d>".length()).length() == 0) {
		        	status_info = getString(R.string.suv_status_empty);
	        	} else {
		        	status_info = getString(R.string.suv_status_error);
	        	}
	        }
	        
			status_date = status_date.substring(0, status_date.indexOf("<d>"));
        	status_date = getString(R.string.suv_status_date1) + "  " + status_date;
	        ((TextView) findViewById(R.id.suv_status_info)).setText(status_info);
	        ((TextView) findViewById(R.id.suv_status_date)).setText(status_date);
		}
	};
	
	private boolean parseSuvDevicePHP (String parseStr) {
		Log.i("parseSuvDevicePHP=" + parseStr);
        
		String[] parsedData0 = null;
		if (parseStr != null && parseStr.contains(";;")) {
			parsedData0 = parseStr.split(";;");
			//[0]=#devices [1]=devicedata
			int parsedDataZ0 = parsedData0.length;
			if (parsedDataZ0 != 2 || parsedData0[0].length() == 0) {
				Log.e("suvStatus parseSuvDevicePHP !@#$ no data0");
				return false;
			}
		} else {
			Log.e("suvStatus parseSuvDevicePHP !@#$ format0");
			return false;
		}
		
		String[] parsedData1 = null;
		if (parsedData0 != null && parsedData0[1].contains(";")) {
			parsedData1 = parsedData0[1].split(";");
		} else {
			Log.e("suvStatus parseSuvDevicePHP !@#$ format1");
			return false;
		}
		
		int parsedDataZ1 = 0;
		int actualParsedDataZ1 = 0;
		try {
			parsedDataZ1 = Integer.parseInt(parsedData0[0]);
			actualParsedDataZ1 = parsedData1.length;
		} catch (NumberFormatException e) {
			Log.e("suvStatus parseSuvDevicePHP !@#$ format1b");
			return false;
		}
		
		if (actualParsedDataZ1 < parsedDataZ1) {
			Log.e("suvStatus parseSuvDevicePHP !@#$ #devices " + actualParsedDataZ1 + "<" + parsedDataZ1);
			parsedDataZ1 = actualParsedDataZ1;
		}
		
		if (parsedDataZ1 > 0)
		{
			HashMap<String, String> map;
			
			for (int i = 0; i < parsedDataZ1; i++) {
				String[] suvInfoPerDevice = null;
				
				if (parsedData1[i] != null && parsedData1[i].contains(":")) {
					suvInfoPerDevice = parsedData1[i].split(":");
					int suvDeviceInfoZ = suvInfoPerDevice.length;
					
					if (suvDeviceInfoZ > 0) {
						map = new HashMap<String, String>();
						
						for (int j = 0; j < suvDeviceInfoZ; j++) {
							
							if (suvInfoPerDevice != null) {
								String info = suvInfoPerDevice[j].trim();
								
								switch (j) {
									case 0:
										map.put(DEV_NAME, info);
										break;
									case 1:
										map.put(DEV_CAT, info);
										break;
									case 2:
										map.put(DEV_STAT, info);
										break;
								}
								
							}
						}
						
						suvDevices.add(map);
					}
				} else {
					Log.w("suvStatus suvDevice !@#$ format2= " + parsedData1[i]);
				}
			}
			
		}
		else
		{
			Log.e("suvStatus parseSuvDevicePHP !@#$ no data2");
			return false;
		}
		
		return true;
	}
	
	class DeviceAdapter extends BaseAdapter {
		private Context _context;
		private int _devicePos;
		private String _deviceName;
		private String _deviceCategory;
		private String _deviceStatus;

		public DeviceAdapter(Context context) {
			_context = context;
		}
		
		class ViewHolder {
			public TextView deviceName;
			public TextView deviceCategory;
			public Button deviceStatus;
		}
		
		@Override
		public int getCount() {
			return suvDevices.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView (final int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			Map<String, String> map = null;
			
			try {
				map = suvDevices.get(position);
			} catch (Exception e) {
				if (convertView == null) {
					convertView = View.inflate(_context, R.layout.suv_device_cell, null);
					holder = new ViewHolder();
					convertView.setTag(holder);
				}
				return convertView;
			}
			
			if (convertView == null) {
				convertView = View.inflate(_context, R.layout.suv_device_cell, null);
				holder = new ViewHolder();
				
				holder.deviceName = (TextView) convertView.findViewById(R.id.device_name);
				holder.deviceCategory = (TextView) convertView.findViewById(R.id.device_category);
				holder.deviceStatus = (Button) convertView.findViewById(R.id.device_status);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			_devicePos = position + 1;
			_deviceName = map.get(DEV_NAME);
			_deviceCategory = map.get(DEV_CAT);
			_deviceStatus = map.get(DEV_STAT);
			
			if (_deviceName == null) _deviceName = "--";
			if (_deviceCategory == null) _deviceCategory = "--";
			if (_deviceStatus == null) _deviceStatus = "--";
			if (_deviceName.length() == 0) _deviceName = "?";
			if (_deviceCategory.length() == 0) _deviceCategory = "?";
			if (_deviceStatus.length() == 0) _deviceStatus = "?";
			
			holder.deviceName.setText(_devicePos + ". " + _deviceName);

			holder.deviceCategory.setText(_deviceCategory);
			
			if (_deviceStatus.equals(STATUS_OK_CODE)) {
				holder.deviceStatus.setBackgroundColor(Color.parseColor(STATUS_OK_CLR));
				holder.deviceStatus.setText(R.string.suv_status_ok);
			} else if (_deviceStatus.equals(STATUS_ALERT_CODE)) {
				holder.deviceStatus.setBackgroundColor(Color.parseColor(STATUS_ALERT_CLR));
				holder.deviceStatus.setText(R.string.suv_status_alert);
			} else if (_deviceStatus.equals(STATUS_OFF_CODE)) {
				holder.deviceStatus.setBackgroundColor(Color.parseColor(STATUS_OFF_CLR));
				holder.deviceStatus.setText(R.string.suv_status_off);
			} else if (_deviceStatus.equals("?")) {
				holder.deviceStatus.setBackgroundColor(Color.parseColor(STATUS_UNKN_CLR));
				holder.deviceStatus.setText(_deviceStatus);
			} else {
				holder.deviceStatus.setBackgroundColor(Color.parseColor(STATUS_UNKN_CLR));
				holder.deviceStatus.setText(_deviceStatus);
			}
			
			holder.deviceStatus.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (suvDevices != null) {
						Map<String, String> map = suvDevices.get(position);
						String deviceName = map.get(DEV_NAME);
						String deviceCategory = map.get(DEV_CAT);
						String deviceStatus = map.get(DEV_STAT);
						Log.i("deviceStatus (" + mIdx + " " + mAddress + " " + mNickname + ") button p:" + position + " n:" + deviceName + " c:" + deviceCategory + " s:" + deviceStatus);
						
						if (deviceStatus.equals("1")) {
							if (AireJupiter.getInstance() != null) {
								AireJupiter.getInstance().uploadSuvStatus(true, null, null, mIdx);
							}

							map.put(DEV_NAME, deviceName);
							map.put(DEV_CAT, deviceCategory);
							map.put(DEV_STAT, "0");
							suvDevices.set(position, map);
							
					        deviceAdapter.notifyDataSetChanged();
					        Toast.makeText(getApplicationContext(), getString(R.string.suv_status_reset), Toast.LENGTH_SHORT).show();
						}
					}
				}
			});
			
			return convertView;
		}
	}
	
	private Runnable popupProgressDialog = new Runnable() {
		@Override
		public void run() {
			try {
				if (progressDialog == null)
					progressDialog = ProgressDialog.show(SuvStatusActivity.this, "", getString(R.string.in_progress), true, false);
			} catch (Exception e) {}
		}
	};
	
	private Runnable dismissProgressDialog = new Runnable() {
		@Override
		public void run() {
			try {
				if (progressDialog != null && progressDialog.isShowing())
					progressDialog.dismiss();
				if (progressDialog != null) progressDialog.cancel();
					progressDialog = null;
			} catch (Exception e) {
			} finally {
				progressDialog = null;
			}
		}
	};
	
}