package com.pingshow.amper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.network.MyNet;
import com.pingshow.util.GroupUpdateMessageSender;
import com.pingshow.util.LBMUtil;
import com.pingshow.util.MyUtil;

import java.net.URLEncoder;

/**
 * Created by jack on 2016/4/21.
 */
public class GroupNameActivity extends Activity {

    private ImageView mCancel;
    private ImageView mDone;
    private EditText mGroupName;
    private int groupId;
    private String groupname;

    public static final int CHANGED = 3;
    private String newGroupName;
    private MyPreference mPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_name);

        initView();

        initData();

        logicProcess();
    }

    private void initView() {
        mCancel = (ImageView) findViewById(R.id.cancel);
        mDone = (ImageView) findViewById(R.id.done);
        mGroupName = (EditText) findViewById(R.id.et_groupname);
        //显示键盘
        mGroupName.setFocusable(true);
        InputMethodManager imm = (InputMethodManager) GroupNameActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void initData() {
        mPref = new MyPreference(this);
        Intent intent = getIntent();
        groupId = Integer.parseInt(intent.getStringExtra("groupId"));
        groupname = intent.getStringExtra("groupname");
        mGroupName.setText(groupname);
    }

    private void logicProcess() {
        mDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGroupName = mGroupName.getText().toString().trim();
                if (!newGroupName.isEmpty()) {
                    if (newGroupName.equals(groupname)) {
                        Toast.makeText(GroupNameActivity.this, getResources().getString(R.string.no_change), Toast.LENGTH_SHORT).show();
                    }else{
                        //请求php
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String Return="";
                                try {
                                    int c = 0;
                                    do {
                                        MyNet net = new MyNet(GroupNameActivity.this);
                                        Return = net.doPostHttps("update_group_name.php",
                                                "gid=" + groupId + "&name="
                                                        + URLEncoder.encode(newGroupName,"UTF-8"),
                                                null);
                                        Log.d("修改群昵称  修改群昵称: " + Return);
                                        if (Return.startsWith("sucess")) {
                                            //发送tcp消息
                                            String content = String.format(getString(R.string.group_name_changed), newGroupName);
                                            GroupUpdateMessageSender.getInstance().send(GroupNameActivity.this, Integer.parseInt(mPref.read("myIdx")), groupId, content);
                                            //发送本地广播
                                            Intent intent = new Intent(Global.Action_Refresh_Groupinfo);
                                            intent.putExtra("Command",Global.CMD_Refresh_Rename_Groupname);
                                            intent.putExtra("groupname", newGroupName);
                                            LBMUtil.sendBroadcast(GroupNameActivity.this, intent);

                                            break;
                                        }
                                        MyUtil.Sleep(2000);
                                    } while (++c < 3);
//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            if (result) {
//                                                Intent intent = new Intent();
//                                                intent.putExtra("groupname", newGroupName);
//                                                setResult(CHANGED, intent);
//                                            }else{
//                                                setResult(NOCHANGE);
//                                                Toast.makeText(GroupNameActivity.this, getResources().getString(R.string.poor_network_doesnt_change), Toast.LENGTH_SHORT).show();
//                                            }
//                                            GroupNameActivity.this.finish();
//                                        }
//                                    });
                                } catch (Exception e) {
                                }
                            }
                        }).start();
                        setResult(CHANGED);
                        finish();
                    }
                }else {
                    Toast.makeText(GroupNameActivity.this, getResources().getString(R.string.edit_input_empty), Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
