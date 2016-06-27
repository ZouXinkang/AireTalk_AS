package com.pingshow.amper.bean;

import com.pingshow.amper.view.indexableListView.IndexEntity;

/**
 * Created by jack on 2016/5/20.
 */

public class FriendsEntity extends IndexEntity {
    private String displayName;
    private String address;
    private String idx;
    private String imagePath;
    private String contactId;
    private String seperator;
    private String blocked;
    private String actual;

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    private String mood;

    public String getActual() {
        return actual;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getIdx() {
        return idx;
    }

    public void setIdx(String idx) {
        this.idx = idx;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getSeperator() {
        return seperator;
    }

    public void setSeperator(String seperator) {
        this.seperator = seperator;
    }

    public String getBlocked() {
        return blocked;
    }

    public void setBlocked(String blocked) {
        this.blocked = blocked;
    }

    @Override
    public String toString() {
        return "FriendsEntity{" +
                "displayName='" + displayName + '\'' +
                ", address='" + address + '\'' +
                ", idx='" + idx + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", contactId='" + contactId + '\'' +
                ", seperator='" + seperator + '\'' +
                ", blocked='" + blocked + '\'' +
                ", actual='" + actual + '\'' +
                '}';
    }
}
