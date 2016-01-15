package com.pingshow.airecenter.contacts;

public class RelatedUserInfo {
	private Integer idx;
	private String nickname;
	private String address;
	private int jointfriends;

	public Integer getIdx() {
		return idx;
	}

	public void setIdx(Integer idx) {
		this.idx = idx;
	}

	public String getNickName() {
		return nickname;
	}
	
	public String getAddress() {
		return address;
	}

	public void setNickName(String name) {
		this.nickname = name;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}

	public int getjointfriends() {
		return jointfriends;
	}

	public void setjointfriends(int jointfriends) {
		this.jointfriends = jointfriends;
	}

}
