package com.pingshow.airecenter.bean;

import android.graphics.drawable.Drawable;

/**
 * 聊天室成员模型
 * 
 * @author li
 *
 */
public class ChatroomMember {
	private int idx;
	private String id;
	private boolean isSpeak;
	private String address;
	private String displayName;
	private String uuid;
	private Drawable photo;
	public int getIdx() {
		return idx;
	}
	public void setIdx(int idx) {
		this.idx = idx;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public boolean isSpeak() {
		return isSpeak;
	}
	public void setSpeak(boolean isSpeak) {
		this.isSpeak = isSpeak;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public Drawable getPhoto() {
		return photo;
	}
	public void setPhoto(Drawable photo) {
		this.photo = photo;
	}
	@Override
	public String toString() {
		return "ChatroomMember [idx=" + idx + ", id=" + id + ", isSpeak="
				+ isSpeak + ", address=" + address + ", displayName="
				+ displayName + ", uuid=" + uuid + ", photo=" + photo + "]";
	}
	
}
