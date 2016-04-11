package com.pingshow.amper.bean;

import java.util.List;

/**
 * Created by jack on 2016/3/31.
 */
public class Group {
    /**
     * code : 200
     * gname : 00噢噢噢噢噢噢噢噢in立军客户
     * members : [{"idx":"1260225","username":"+8613810988084","nickname":"huwenhao","rank":0},{"idx":"1264219","username":"hwh159357","nickname":"Uuuuuuuu","rank":1},{"idx":"1265280","username":"beijing","nickname":"beijing","rank":1},{"idx":"1217793","username":"chinesetalk","nickname":"Hhhhhhh","rank":1}]
     */

    private int code;
    private String gname;
    /**
     * idx : 1260225
     * username : +8613810988084
     * nickname : huwenhao
     * rank : 0
     */

    private List<MembersEntity> members;

    public void setCode(int code) {
        this.code = code;
    }

    public void setGname(String gname) {
        this.gname = gname;
    }

    public void setMembers(List<MembersEntity> members) {
        this.members = members;
    }

    public int getCode() {
        return code;
    }

    public String getGname() {
        return gname;
    }

    public List<MembersEntity> getMembers() {
        return members;
    }

    public static class MembersEntity {
        private String idx;
        private String username;
        private String nickname;
        private int rank;

        public void setIdx(String idx) {
            this.idx = idx;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public String getIdx() {
            return idx;
        }

        public String getUsername() {
            return username;
        }

        public String getNickname() {
            return nickname;
        }

        public int getRank() {
            return rank;
        }
    }
}
