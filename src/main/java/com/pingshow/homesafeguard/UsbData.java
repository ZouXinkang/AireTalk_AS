package com.pingshow.homesafeguard;
//li*** Home safeguard
public class UsbData {
	private int head;
	private int address;
	private int data;
	private int key;
	private int resistance;
	private String encode;
	private long time;
	public String getEncode() {
		return encode;
	}
	public void setEncode(String encode) {
		this.encode = encode;
	}
	
	public int getKey() {
		return key;
	}
	public void setKey(int key) {
		this.key = key;
	}
	public int getResistance() {
		return resistance;
	}
	public void setResistance(int resistance) {
		this.resistance = resistance;
	}
	public int getHead() {
		return head;
	}
	public void setHead(int head) {
		this.head = head;
	}
	public int getAddress() {
		return address;
	}
	public void setAddress(int address) {
		this.address = address;
	}
	public int getData() {
		return data;
	}
	public void setData(int data) {
		this.data = data;
	}
	
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	@Override
	public String toString() {
		return "UsbData [head=" + Integer.toHexString(head) + ", address=" + Integer.toHexString(address) + ", data="
				+ Integer.toHexString(data) + ", key=" + Integer.toHexString(key) + ", resistance=" + Integer.toHexString(resistance)
				+ ", encode=" + encode  + ", time=" + time + "]";
	}
	public UsbData(int head, int address, int data, int key, int resistance,
			String encode, long time) {
		super();
		this.head = head;
		this.address = address;
		this.data = data;
		this.key = key;
		this.resistance = resistance;
		this.encode = encode;
		this.time = time;
	}
}
