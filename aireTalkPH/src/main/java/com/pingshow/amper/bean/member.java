package com.pingshow.amper.bean;

import android.graphics.drawable.Drawable;

/**
 * Created by jack on 2016/3/15.
 */
public class Member {
    private int idx;
    private Drawable photo;
    private String nickname;

    public int getChecked() {
        return checked;
    }

    public void setChecked(int checked) {
        this.checked = checked;
    }

    private int checked;

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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }


    @Override
    public String toString() {
        return "Member{" +
                "idx=" + idx +
                ", photo=" + photo +
                ", nickname='" + nickname + '\'' +
                ", checked=" + checked +
                '}';
    }
}
