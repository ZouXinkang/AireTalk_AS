package com.pingshow.iot;

import java.util.ArrayList;

import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.SecurityNewActivity;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

//tml|li*** blue io
public class BlueDeviceListAdapter extends BaseAdapter {
	// Adapter for holding devices found through scanning.

	private ArrayList<BluetoothDevice> mLeDevices;
	private LayoutInflater mInflator;
	private Activity mContext;

	public BlueDeviceListAdapter(Activity c) {
		super();
		mContext = c;
		mLeDevices = new ArrayList<BluetoothDevice>();
		mInflator = mContext.getLayoutInflater();
	}
	//modify to include usb-dongle
	public void addDevice(BluetoothDevice device) {
		if (!mLeDevices.contains(device)) {
			mLeDevices.add(device);
		}
	}

	public BluetoothDevice getDevice(int position) {
		return mLeDevices.get(position);
	}

	public void clear() {
		mLeDevices.clear();
	}

	@Override
	public int getCount() {
		return mLeDevices.size();
	}

	@Override
	public Object getItem(int i) {
		return mLeDevices.get(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(final int i, View view, ViewGroup viewGroup) {
		ViewHolder viewHolder;
		// General ListView optimization code.
		if (view == null) {
			view = mInflator.inflate(R.layout.listitem_bluedevice, null);
			viewHolder = new ViewHolder();
			viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
			viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}

		BluetoothDevice device = mLeDevices.get(i);
		final String deviceName = device.getName();
		if (deviceName != null && deviceName.length() > 0) {
			viewHolder.deviceName.setText(deviceName);
		} else {
			viewHolder.deviceName.setText(R.string.blue_unknown_device);
		}
		viewHolder.deviceAddress.setText(device.getAddress());

		return view;
	}

	class ViewHolder {
		TextView deviceName;
		TextView deviceAddress;
	}
}
