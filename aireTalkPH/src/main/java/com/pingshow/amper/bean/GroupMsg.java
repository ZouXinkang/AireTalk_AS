package com.pingshow.amper.bean;

/**
 * Created by jack on 2016/3/30.
 * 用于群组发送消息
 */
public class GroupMsg {
    private String cmd;
    private String at;
    private String url;
    private String path;
    private String ct;

    /**
     *
     * @param cmd  命令行
     * @param attachment 附件类型标注
     * @param attachmentURL 附件URL(服务器端文件url)
     * @param content   文本信息
     * @param attachmentPath 本地文件名
     */
    public GroupMsg( String cmd, String attachment, String attachmentURL, String content,String attachmentPath) {
        this.path = attachmentPath;
        this.cmd = cmd;
        this.at = attachment;
        this.url = attachmentURL;
        this.ct = content;
    }
    public GroupMsg(){}

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getAt() {
        return at;
    }

    public void setAt(String at) {
        this.at = at;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCt() {
        return ct;
    }

    public void setCt(String ct) {
        this.ct = ct;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    // TODO: 2016/4/11 稍后删除
    @Override
    public String toString() {
        return "GroupMsg{" +
                "cmd='" + cmd + '\'' +
                ", at='" + at + '\'' +
                ", url='" + url + '\'' +
                ", path='" + path + '\'' +
                ", ct='" + ct + '\'' +
                '}';
    }
}
