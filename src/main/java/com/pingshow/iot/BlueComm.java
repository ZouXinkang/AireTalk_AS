package com.pingshow.iot;

import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.SecurityNewActivity;

//tml|li*** blue io
public class BlueComm {
	private Context mContext;
	private MyPreference mPref;
	
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;

	private OnConnectListener mOnConnectListener;
	private OnDisconnectListener mOnDisconnectListener;
	private OnServiceDiscoverListener mOnServiceDiscoverListener;
	private OnDataAvailableListener mOnDataAvailableListener;
	
	public BlueComm(Context c) {
		mContext = c;
		mPref = new MyPreference(mContext);
	}
	
	public interface OnConnectListener {
		public void onConnect(BluetoothGatt gatt);
	}

	public interface OnDisconnectListener {
		public void onDisconnect(BluetoothGatt gatt);
	}

	public interface OnServiceDiscoverListener {
		public void onServiceDiscover(BluetoothGatt gatt);
	}

	public interface OnDataAvailableListener {
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status);

		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic);
	}

	public void setOnConnectListener(OnConnectListener l) {
		mOnConnectListener = l;
	}

	public void setOnDisconnectListener(OnDisconnectListener l) {
		mOnDisconnectListener = l;
	}

	public void setOnServiceDiscoverListener(OnServiceDiscoverListener l) {
		mOnServiceDiscoverListener = l;
	}

	public void setOnDataAvailableListener(OnDataAvailableListener l) {
		mOnDataAvailableListener = l;
	}

	// Implements callback methods for GATT events that the app cares about. For
	// example,
	// connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				if (mOnConnectListener != null)
					mOnConnectListener.onConnect(gatt);
				Log.i("myblue  Connected to GATT server");

				// Attempts to discover services after successful connection.
				boolean discover = mBluetoothGatt.discoverServices();
				Log.i("myblue  Attempting to start service discovery=" + discover);
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				if (mOnDisconnectListener != null)
					mOnDisconnectListener.onDisconnect(gatt);
				
				String dName = gatt.getDevice().getName();
				String dAddr = gatt.getDevice().getAddress();
				
				if (dName == null || dAddr == null) {
					Log.e("myblue  state disconnect unknown=" + dName + " " + dAddr);
					return;
				}
				
				dName = dName.toLowerCase();
				int j = -1;
				if (dName.contains(SecurityNewActivity.VEEPOO))
				{
					for (int i = 0; i < SecurityNewActivity.MAX_VEEPOO_DEVICES; i++) {
						String device = mPref.read(SecurityNewActivity.VEEPOO_tag + i, "");
						if (device.contains(dAddr)) {
							j = i;
							SecurityNewActivity.readHR_VEEPOO = false;
							mPref.delect(SecurityNewActivity.VEEPOO_tag + i);
							break;
						}
					}
				}
				else if (dName.contains(SecurityNewActivity.ACCULIFE))
				{
					for (int i = 0; i < SecurityNewActivity.MAX_ACCULIFE_DEVICES; i++) {
						String device = mPref.read(SecurityNewActivity.ACCULIFE_tag + i, "");
						if (device.contains(dAddr)) {
							j = i;
							SecurityNewActivity.readHR_ACCULIFE = false;
							mPref.delect(SecurityNewActivity.ACCULIFE_tag + i);
							break;
						}
					}
				}
				close();
				
				Log.e("myblue  Disconnected from GATT server=" + j + " " + dName + " " + dAddr);
				if (SecurityNewActivity.getInstance() != null) {
					SecurityNewActivity.getInstance().onDisconnected(dAddr, dName, j);
				}
			} else {
				Log.e("myblue  onConnectionStateChange unknown=" + newState);
			}
		}

		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if (mOnDataAvailableListener != null)
				mOnDataAvailableListener.onCharacteristicWrite(gatt, characteristic);
		};

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS
					&& mOnServiceDiscoverListener != null) {
				mOnServiceDiscoverListener.onServiceDiscover(gatt);
			} else {
				Log.e("myblue  onServicesDiscovered received=" + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if (mOnDataAvailableListener != null)
				mOnDataAvailableListener.onCharacteristicRead(gatt, characteristic, status);
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			if (mOnDataAvailableListener != null)
				mOnDataAvailableListener.onCharacteristicWrite(gatt, characteristic);
		}
	};

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 * 
	 * @return Return true if the initialization is successful.
	 */
	public boolean initialize() {
		// For API level 18 and above, get a reference to BluetoothAdapter
		// through
		// BluetoothManager.
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				Log.e("myblue  Unable to initialize BluetoothManager");
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Log.e("myblue   Unable to obtain a BluetoothAdapter");
			return false;
		}

		return true;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 * 
	 * @param address
	 *            The device address of the destination device.
	 * 
	 * @return Return true if the connection is initiated successfully. The
	 *         connection result is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *         callback.
	 */
	public boolean connect(final String address) {
		if (mBluetoothAdapter == null || address == null) {
			Log.e("myblue  connect error, BluetoothAdapter not initialized or unspecified address");
			return false;
		}

		// Previously connected device. Try to reconnect.
		if (mBluetoothDeviceAddress != null
				&& address.equals(mBluetoothDeviceAddress)
				&& mBluetoothGatt != null) {
			if (mBluetoothGatt.connect()) {
				Log.i("myblue  Trying to use an existing mBluetoothGatt for connection");
				return true;
			} else {
				Log.e("myblue  Trying to use an existing mBluetoothGatt for connection - failed");
				return false;
			}
		}

		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			Log.e("myblue  Device null, unable to connect");
			return false;
		}
		// We want to directly connect to the device, so we are setting the
		// autoConnect
		// parameter to false.
		mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
		
		if (SecurityNewActivity.blueDisconnect) {
			disconnect();
			return false;
		}
		
		Log.i("myblue  Creating a new connection");
		mBluetoothDeviceAddress = address;
		return true;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.e("myblue  BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
		Log.e("myblue  BluetoothGATT disconnected");
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure
	 * resources are released properly.
	 */
	public void close() {
		//test disconnect
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
		Log.e("myblue  BluetoothGATT closed");
	}

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read
	 * result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 * 
	 * @param characteristic
	 *            The characteristic to read from.
	 */
	public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.e("myblue  readChar error, BluetoothAdapter not initialized");
			return false;
		}
		mBluetoothGatt.readCharacteristic(characteristic);
		return true;
	}

	/**
	 * Enables or disables notification on a give characteristic.
	 * 
	 * @param characteristic
	 *            Characteristic to act on.
	 * @param enabled
	 *            If true, enable notification. False otherwise.
	 */
	public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
			boolean enabled) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.e("myblue  setChar error, BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
	}

	public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		mBluetoothGatt.writeCharacteristic(characteristic);
	}

	/**
	 * Retrieves a list of supported GATT services on the connected device. This
	 * should be invoked only after {@code BluetoothGatt#discoverServices()}
	 * completes successfully.
	 * 
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null)
			return null;

		return mBluetoothGatt.getServices();
	}
	
	public BluetoothGattService getService(UUID uuid){
		if (mBluetoothGatt == null)
			return null;
		
		return mBluetoothGatt.getService(uuid);
	}
	
}
