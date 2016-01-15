package com.pingshow.homesafeguard;
//li*** Home safeguard
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class UsbDeviceDBHelper extends SQLiteOpenHelper {
	private final static String DB_NAME = "usb_device.db";
	private final static int VERSION = 1;

	public UsbDeviceDBHelper(Context context) {
		this(context, DB_NAME, null, VERSION);
	}

	public UsbDeviceDBHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		 String sql = "CREATE TABLE IF NOT EXISTS device(" +
			      "address integer primary key," +
			      "name varchar(20)," +
			      "category integer," +
			      "isopen integer"+
			      ")";

			  db.execSQL(sql);
	}
	public void addUsbDevice(UsbDevice device){
		if(getDevice(device.getAddress())==null){
			getWritableDatabase().execSQL("insert into device values("+device.getAddress()+",'"+device.getName()+"',"+device.getCategory()+","+(device.isOpen()?1:0)+")");
		}
	}
	public List<UsbDevice> getAllUsbDevice(){
		Cursor cursor = getReadableDatabase().rawQuery("select * from device", null);
		List<UsbDevice> devices = new ArrayList<UsbDevice>();
		while(cursor.moveToNext()){
			UsbDevice device = getDevice(cursor);
			devices.add(device);
		}
		return devices;
	}
	public void deleteDevice(int address){
		getWritableDatabase().execSQL("delete from device where address="+address);
	}
	public UsbDevice getDevice(int address){
		Cursor cursor = getReadableDatabase().rawQuery("select * from device where address = " + address,null);
		if(cursor.moveToNext()){
			return getDevice(cursor);
		}
		return null;
	}
	public boolean updateDevice(UsbDevice device){
		if(getDevice(device.getAddress())!=null){
			int isOpen = device.isOpen() ? 1 : 0;
			getWritableDatabase().execSQL("update device set name='"+device.getName()+"',category="+device.getCategory()+",isopen="+isOpen+" where address = "+device.getAddress());
			return true;
		}
		return false;
	}

	private UsbDevice getDevice(Cursor cursor) {
		int address = cursor.getInt(cursor.getColumnIndex("address"));
		int category = cursor.getInt(cursor.getColumnIndex("category"));
		String name = cursor.getString(cursor.getColumnIndex("name"));
		boolean isOpen = cursor.getInt(cursor.getColumnIndex("isopen")) == 1;
		UsbDevice device = new UsbDevice(category, name, address, null,isOpen);
		return device;
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}
}
