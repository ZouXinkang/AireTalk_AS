package com.pingshow.amper.bean;

/**
 * Created by HwH on 2016/7/22.
 */

public class SmartDeviceEntity {
    private String name;
    private String urlkey;
    private int status;
    private String openData;
    private String closeData;


    public SmartDeviceEntity() {
    }

    public SmartDeviceEntity(String name, String urlkey, int status, String openData, String closeData) {
        this.name = name;
        this.urlkey = urlkey;
        this.status = status;
        this.openData = openData;
        this.closeData = closeData;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrlkey() {
        return urlkey;
    }

    public void setUrlkey(String urlkey) {
        this.urlkey = urlkey;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getOpenData() {
        return openData;
    }

    public void setOpenData(String openData) {
        this.openData = openData;
    }

    public String getCloseData() {
        return closeData;
    }

    public void setCloseData(String closeData) {
        this.closeData = closeData;
    }
}
