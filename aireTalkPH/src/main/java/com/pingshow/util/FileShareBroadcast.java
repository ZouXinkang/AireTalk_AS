package com.pingshow.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.Log;
import com.pingshow.amper.MainActivity;
import com.pingshow.amper.SMS;
import com.pingshow.amper.VideoPlayerActivity;

/**
 * Created:         Hsia on 16/1/29.
 * Email:           xiaweifeng@pingshow.net
 * Description:     {TODO}(用一句话描述该文件做什么)
 */
public class FileShareBroadcast extends BroadcastReceiver {
    SMS msg=new SMS();
    @Override
    public void onReceive(Context context, Intent intent) {
//        String Sender=mADB.getAddressByIdx(idx);
//        msg.address=Sender;

        Bundle extras = intent.getExtras();
        String downUrl = extras.getString("downUrl");
        int result = extras.getInt("getResult");
        Log.d("下载链接地址："+downUrl);
        if (result==0){
            intent.setClass(context, OpenShareVideo.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
//            context.startActivity(new Intent(context, OpenShareVideo.class));
        }else if(result == 1){
            intent.setClass(context,OpenShareVideo.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }else if(result == -1){

        }
    }


}
