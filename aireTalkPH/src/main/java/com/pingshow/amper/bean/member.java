package com.pingshow.amper.bean;

import android.graphics.drawable.Drawable;

/**
 * Created by jack on 2016/3/15.
 */
public class Member {
    private int idx;

    public Drawable getPhoto() {
        return photo;
    }

    public void setPhoto(Drawable photo) {
        this.photo = photo;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    private Drawable photo;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    private String nickname;


}
