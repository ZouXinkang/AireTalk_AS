package com.pingshow.amper.bean;

/**
 * Created by HwH on 2016/4/28.
 */
public class GroupUpdateMsg  {


    /**
     * at : 0
     * cmd : groupUpdate
     * ct : {"type":"","nicknames":""}
     * path :
     * url :
     */

    private String at;
    private String cmd;
    /**
     * type :
     * nicknames :
     */

    private CtEntity ct;
    private String path;
    private String url;

    public GroupUpdateMsg(String cmd, String at, String url, String path, CtEntity ct) {
        this.url = url;
        this.at = at;
        this.cmd = cmd;
        this.ct = ct;
        this.path = path;
    }

    public void setAt(String at) {
        this.at = at;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public void setCt(CtEntity ct) {
        this.ct = ct;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAt() {
        return at;
    }

    public String getCmd() {
        return cmd;
    }

    public CtEntity getCt() {
        return ct;
    }

    public String getPath() {
        return path;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "GroupUpdateMsg{" +
                "at='" + at + '\'' +
                ", cmd='" + cmd + '\'' +
                ", ct=" + ct +
                ", path='" + path + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    public static class CtEntity {
        private String idxs;
        private String type;
        private String nicknames;

        public void setIdxs(String idxs) {
            this.idxs = idxs;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setNicknames(String nicknames) {
            this.nicknames = nicknames;
        }

        public String getIdxs() {
            return idxs;
        }

        public String getType() {
            return type;
        }

        public String getNicknames() {
            return nicknames;
        }

        @Override
        public String toString() {
            return "CtEntity{" +
                    "idxs='" + idxs + '\'' +
                    ", type='" + type + '\'' +
                    ", nicknames='" + nicknames + '\'' +
                    '}';
        }
    }
}
