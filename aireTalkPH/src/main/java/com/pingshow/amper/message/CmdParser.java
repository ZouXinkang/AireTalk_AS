package com.pingshow.amper.message;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.Log;
import com.pingshow.util.MyUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * 命令解析器
 * 
 * @author li
 * 
 */
public class CmdParser {

	/**
	 * 好友修改了nickname命令
	 */
	public static final String UPDATE_NICKNAME = "UpdateNickname";
	private Context mContext;
	public CmdParser(Context context) {
		this.mContext = context;
	}

	/**
	 *
	 * @param cmdStr
	 */
	public void parseCmd(String cmdStr) {
		String[] items = cmdStr.split("/", 6);
		String jsonStr = items[5];
		try {
			JSONObject jo = new JSONObject(jsonStr);
			final String cmd = jo.getString("cmd");
		 if (UPDATE_NICKNAME.equals(cmd)) {
				final String nickname = jo.getString("nickname");
				AireJupiter.getInstance().getFriendNicknames();
				Log.i("bree updataNickname " + nickname);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
