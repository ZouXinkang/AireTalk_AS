package com.pingshow.amper.bean;

/**
 * Created by jack on 2016/4/8.
 */
public class GroupEntity {

    private int idx;
    private String nn;

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public String getNn() {
        return nn;
    }

    public void setNn(String nn) {
        this.nn = nn;
    }

    // TODO: 2016/4/11 之后删除
    @Override
    public String toString() {
        return "GroupEntity{" +
                "idx=" + idx +
                ", nn='" + nn + '\'' +
                '}';
    }
}
