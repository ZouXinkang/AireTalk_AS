package com.pingshow.homesafeguard;
//li*** Home safeguard
public class UsbDevice {
	public final static int DEV_TELECONTROLLER = 0;
	public final static int DEV_MAGNETISM_SENSOR = 1;
	public final static int DEV_WATCH_DOG = 2;
	public final static int DEV_SMOKE_SENSOR = 3;
	public final static int DEV_TEMPERATURE_SENSOR = 4;
	
	private int category;
	private String name;
	private int address;
	private UsbData data;
	private boolean isOpen;
	
	
	public UsbDevice(int category, String name, int address, UsbData data,
			boolean isOpen) {
		super();
		this.category = category;
		this.name = name;
		this.address = address;
		this.data = data;
		this.isOpen = isOpen;
	}
	public boolean isOpen() {
		return isOpen;
	}
	public void setOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getCategory() {
		return category;
	}
	public void setCategory(int category) {
		this.category = category;
	}
	public int getAddress() {
		return address;
	}
	public void setAddress(int address) {
		this.address = address;
	}
	public UsbData getData() {
		return data;
	}
	public void setData(UsbData data) {
		this.data = data;
	}
	
}
