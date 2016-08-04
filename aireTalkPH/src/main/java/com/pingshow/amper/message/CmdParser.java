package com.pingshow.amper.message;

import android.content.Context;
import android.content.Intent;

import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.Global;
import com.pingshow.amper.Log;
import com.pingshow.util.LBMUtil;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 命令解析器
 *
 * @author li
 */
public class CmdParser {

    /**
     * 好友修改了nickname命令
     */
    public static final String UPDATE_NICKNAME = "UpdateNickname";
    /**
     * 智慧家庭命令.
     */
    public static final String CMD_SMART_HOME = "sh";

    private Context mContext;

    public CmdParser(Context context) {
        this.mContext = context;
    }

    /**
     * @param cmdStr
     */
    public void parseCmd(String cmdStr) {
        String[] items = cmdStr.split("/", 6);
        String jsonStr = items[5];
        try {
            JSONObject jo = new JSONObject(jsonStr);
            final String cmd = jo.getString("cmd");
            switch (cmd) {
                case UPDATE_NICKNAME:
                    final String nickname = jo.getString("nickname");
                    AireJupiter.getInstance().getFriendNicknames();
                    Log.i("bree updataNickname " + nickname);
                    break;
                case CMD_SMART_HOME:
                    Log.d("收到只能开关返回的socket: "+jo.toString());
                    String result = jo.optString("result");
                    Intent intent = new Intent(Global.ACTION_SMART_HOME);
                    intent.putExtra("result", result);
                    LBMUtil.sendBroadcast(mContext, intent);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
