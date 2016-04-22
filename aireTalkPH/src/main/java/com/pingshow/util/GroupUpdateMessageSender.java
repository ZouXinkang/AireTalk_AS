package com.pingshow.util;

import android.content.Context;

import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.ConversationActivity;
import com.pingshow.amper.R;
import com.pingshow.amper.SendAgent;
import com.pingshow.amper.bean.GroupMsg;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.SmsDB;

import java.util.Date;

/**
 * Created by jack on 2016/4/21.
 */
public class GroupUpdateMessageSender {
    private GroupUpdateMessageSender(){

    }
    private static GroupUpdateMessageSender instance = new GroupUpdateMessageSender();

    public static GroupUpdateMessageSender getInstance(){
        return instance;
    }

    public void send(Context context,int idx,int groupId,String content){
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
        smsDB.insertMessage(address, contactid, (new Date()).getTime(), read, -1, 2, "", content, 0, null, null, 0, 0, 0, 0, displayname, obligate1, idx);
        smsDB.close();

        SendAgent agent = new SendAgent(context, idx, 0, true);
        agent.setAsGroup(groupId);
        GroupMsg groupAdd = new GroupMsg("groupUpdate", "0", "", content, "");
        agent.onGroupSend(groupAdd);
    }
}
