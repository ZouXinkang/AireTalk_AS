package com.pingshow.amper.bean;

/**
 * Created by jack on 2016/3/30.
 * 用于群组发送消息
 */
public class GroupMsg {
    private String cmd;
    private String attachment;
    private String attachmentURL;
    private String content;

    public GroupMsg(String cmd, String attachment, String attachmentURL, String content) {
        this.cmd = cmd;
        this.attachment = attachment;
        this.attachmentURL = attachmentURL;
        this.content = content;
    }

    public GroupMsg() {
    }

    public String getCMD() {
        return cmd;
    }

    public void setCMD(String cmd) {
        this.cmd = cmd;
    }

    public String getAttached() {
        return attachment;
    }

    public void setAttached(String attached) {
        this.attachment = attached;
    }

    public String getAttachmentURL() {
        return attachmentURL;
    }

    public void setAttachmentURL(String attachmentURL) {
        this.attachmentURL = attachmentURL;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
