package com.pingshow.util;

import android.content.Context;

import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.ConversationActivity;
import com.pingshow.amper.Log;
import com.pingshow.amper.R;
import com.pingshow.amper.SendAgent;
import com.pingshow.amper.bean.GroupMsg;
import com.pingshow.amper.bean.GroupUpdateMsg;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.SmsDB;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by jack on 2016/4/21.
 */
public class GroupUpdateMessageSender {
    private String content;

    private GroupUpdateMessageSender() {

    }

    private static GroupUpdateMessageSender instance = new GroupUpdateMessageSender();

    public static GroupUpdateMessageSender getInstance() {
        return instance;
    }

    /**
     * @param context
     * @param idx
     * @param groupId
     * @param type    关键类型
     * @param arg1    参数1
     * @param arg2    参数2
     */
    public void send(Context context, int idx, int groupId, String type, String arg1, String arg2,String idxs) {
        String obligate1 = null;
        if (AireJupiter.getInstance() != null) {  //tml*** china ip
            obligate1 = AireJupiter.getInstance().getIsoPhp(0, true, null);
        } else {
            obligate1 = AireJupiter.myPhpServer_default;
        }
        String address = "[<GROUP>]" + groupId;

        ContactsQuery cq = new ContactsQuery(context);
        long contactid = cq.getContactIdByNumber(address);

        boolean flag = ConversationActivity.sender != null && MyTelephony.SameNumber(ConversationActivity.sender, "[<GROUP>]" + groupId);
        int read = (flag == true ? 1 : 0);

        //jack 国际化从分组删除人
        AmpUserDB mADB = new AmpUserDB(context);
        mADB.open();
        String displayname = mADB.getNicknameByAddress(address);
        mADB.close();
        SmsDB smsDB = new SmsDB(context);
        smsDB.open();

        //解析群信息变更
        GroupUpdateMsg.CtEntity ctEntity = parse(context, type, arg1, arg2,idxs);
//        String jsonString="";
//        try {
//            jsonString = (new JSONObject(parse(context, type, arg1, arg2))).toString();
//        } catch (JSONException e) {
//
//        }


        //type 2 发送,1 接收 status 2代表发送但不代表成功
        // TODO: 2016/4/26 末尾增加一个字段,1表示cmd
        long rowid = smsDB.insertMessageNew(address, contactid, (new Date()).getTime(), read, 2, 2, "", content, 0, null, null, 0, 0, 0, 0, displayname, obligate1, idx, 1);
        smsDB.close();
        int sendGroupIdx = 100000000 + groupId;

        SendAgent agent = new SendAgent(context, idx, sendGroupIdx, true);
        agent.setAsGroup(groupId);
        agent.setRowId(rowid);
//        GroupMsg groupAdd = new GroupMsg("groupUpdate", "0", "", jsonString, "");
        GroupUpdateMsg groupUpdate = new GroupUpdateMsg("groupUpdate", "0", "", "", ctEntity);
        agent.onGroupUpdateSend(groupUpdate);
//        agent.onGroupSend(groupAdd);
    }

    public GroupUpdateMsg.CtEntity parse(Context context, String type, String arg1, String arg2,String idxs) {
        GroupUpdateMsg.CtEntity ctEntity = new GroupUpdateMsg.CtEntity();
        ctEntity.setType(type);
        ctEntity.setIdxs(idxs);
        String str = null;
        switch (type) {
            case "Group_Remove":
                content = String.format(context.getResources().getString(R.string.group_removed_members), arg1);
                str = "{\"type\":" + "\"" + type + "\"" + ",\"nicknames\":" + "\"" + arg1 + "\"}";
                ctEntity.setNicknames(arg1);

                Log.d("Group_Remove  " + str);
                break;
            case "Group_Add":
                content = String.format(context.getResources().getString(R.string.group_invite_new_members), arg1, arg2);
                str = "{\"type\":" + "\"" + type + "\"" + ",\"nicknames\":" + "\"" + arg1 + "/" + arg2 + "\"}";
                Log.d("Group_Add  " + str);
                ctEntity.setNicknames(arg1 + "/" + arg2);
                break;
            case "Group_Left":
                content = String.format(context.getResources().getString(R.string.group_member_left), arg1);
                str = "{\"type\":" + "\"" + type + "\"" + ",\"nicknames\":" + "\"" + arg1 + "\"}";
                Log.d("Group_Left  " + str);
                ctEntity.setNicknames(arg1);
                break;
            case "Group_Switch":
                content = String.format(context.getResources().getString(R.string.group_creater_changed), arg1);
                str = "{\"type\":" + "\"" + type + "\"" + ",\"nicknames\":" + "\"" + arg1 + "\"}";
                Log.d("Group_Switch  " + str);
                ctEntity.setNicknames(arg1);
                break;
            case "Group_Photo":
                content = String.format(context.getResources().getString(R.string.group_photo_changed), arg1);
                str = "{\"type\":" + "\"" + type + "\"" + ",\"nicknames\":" + "\"" + arg1 + "\"}";
                Log.d("Group_Photo  " + str);
                ctEntity.setNicknames(arg1);
                break;
            case "Group_Name":
                content = String.format(context.getResources().getString(R.string.group_name_changed), arg1, arg2);
                str = "{\"type\":" + "\"" + type + "\"" + ",\"nicknames\":" + "\"" + arg1 + "/" + arg2 + "\"}";
                Log.d("Group_Name  " + str);
                ctEntity.setNicknames(arg1 + "/" + arg2);
                break;
        }
        return ctEntity;
    }
}
