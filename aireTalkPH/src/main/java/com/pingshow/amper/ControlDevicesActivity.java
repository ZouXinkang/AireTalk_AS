package com.pingshow.amper;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.amper.bean.SmartDeviceEntity;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.util.LBMUtil;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Created by jack on 16/3/18.
 */
public class ControlDevicesActivity extends Activity {
    private String mAddress;
    private String mNickname;

    private RecyclerView mDevice_RV;
    private ControlDevicesAdapter mAdapter;

    private List<SmartDeviceEntity> myDevices;
    private Map params = new HashMap<String, String>();

    private AmpUserDB mADB;
    private String myIdx_hex;

    private LoadingDialog loadingDialog;
    private String result;

    private OkHttpClient okHttpClient;
    private String uploadurl = "http://" + Global.Action_Smart_Home_Device + Global.Action_Smart_Home_Device_Path + "/trx.php";
    private String queryurl = "http://" + Global.Action_Smart_Home_Device + Global.Action_Smart_Home_Device_Path + "/trxstatus_json.php";
    private Request request;
    private TextView mTopic;

    /**
     * 接收广播更新UI
     */
    private BroadcastReceiver smartHomeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.removeCallbacksAndMessages(null);
            String result = intent.getStringExtra("result");
            Log.d("ControlDevicesActivity 收到广播,接收发送的结果: " + result);
            if ("success".equals(result)) {
                loadingDialog.dismiss();
                mAdapter.notifyDataSetChanged();

            } else if ("failed".equals(result)) {
                loadingDialog.dismiss();
                clickEntity.setStatus(originStatus);
                Toast.makeText(context, getString(R.string.device_doing_failed), Toast.LENGTH_SHORT).show();
            }
        }
    };
    private SmartDeviceEntity clickEntity;
    private int originStatus;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_device);

        initView();

        initData();

//        initEvent();
    }


    private void initView() {
        //初始化显示的标题
        mTopic = (TextView) findViewById(R.id.topic);

        //展示用的ListView
        mDevice_RV = (RecyclerView) findViewById(R.id.lv_device);
    }

    private void initData() {
        Intent intent = getIntent();
        mAddress = intent.getStringExtra("SendeeNumber");
        mNickname = intent.getStringExtra("SendeeDisplayname");
        Log.d("ControlDevicesActivity ID:" + mAddress + " Nickname:" + mNickname);
        mTopic.setText(String.format(getString(R.string.control_device), mNickname));

        handler = new Handler();

        myDevices = new ArrayList<>();
        mADB = new AmpUserDB(this);
        mADB.open();

        int idx = mADB.getIdxByAddress(mAddress);
        mADB.close();

        //注册广播接收者
        LBMUtil.registerReceiver(getApplicationContext(), smartHomeReceiver, new IntentFilter(Global.ACTION_SMART_HOME));

        myIdx_hex = Integer.toHexString(idx);
        Log.d("ControlDevicesActivity idx" + myIdx_hex);

        okHttpClient = new OkHttpClient();
        loadingDialog = new LoadingDialog(this, getString(R.string.waiting));
        loadingDialog.setCanceledOnTouchOutside(false);

        loadingDialog.show();
        // TODO: 2016/7/21 去請求网络,获取已经匹配好的智能硬件
        getDataFromNet(queryurl + "?idx=" + myIdx_hex, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.dismiss();
                        Toast.makeText(ControlDevicesActivity.this, getString(R.string.device_doing_failed), Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                result = response.body().string();
                Log.d("ControlDevicesActivity: " + result);
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    //设备数量
                    for (int i = 0; i < 5; i++) {
                        String data = jsonObject.optString("trx" + i);
                        Log.d("ControlDevicesActivity" + " 进入");
                        if (!"".equals(data)) {
                            Log.d("ControlDevicesActivity" + "进入数据解析");
                            String[] realData = data.split("_");
                            Log.d("ControlDevicesActivity " + realData[0]);
                            int openAddress = Integer.parseInt(realData[1]);
                            int openKey = Integer.parseInt(realData[2]);
                            int closeAddress = Integer.parseInt(realData[3]);
                            int closeKey = Integer.parseInt(realData[4]);
                            int status = jsonObject.optInt("trx" + i + "_s");
                            //只做数据的展示，不做数据的添加和删除
                            myDevices.add(0, new SmartDeviceEntity(realData[0], "trx" + i, status, openAddress + "_" + openKey, closeAddress + "_" + closeKey));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.dismiss();
                        //设备添加最多5个

                        mAdapter = new ControlDevicesAdapter(ControlDevicesActivity.this, myDevices);
                        mDevice_RV.setAdapter(mAdapter);
                        initEvent();
                    }
                });

            }
        });

    }

    /**
     * 请求数据
     *
     * @param url
     * @param callBack
     */
    private void getDataFromNet(String url, Callback callBack) {
        Log.d("ControlDevicesActivity 请的url: " + url + " myPhoneNumber:" + mAddress);
        request = new Request.Builder().url(url).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(callBack);
    }

    private void initEvent() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mDevice_RV.setLayoutManager(linearLayoutManager);
        mDevice_RV.setItemAnimator(new DefaultItemAnimator());


        mAdapter.setOnButtonClickListener(new ControlDevicesAdapter.OnButtonClickListener() {
            @Override
            public void onOpenClick(int position) {
                Log.d("ControlDevicesActivity 指定的position: " + position + " 打开 ");
                loadingDialog.show();

                clickEntity = myDevices.get(position);
                originStatus = clickEntity.getStatus();
                String name = clickEntity.getName();
                clickEntity.setStatus(1);

                String[] openData = clickEntity.getOpenData().split("_");
                Log.d("ControlDevicesActivity 发送Json" + "{\"cmd\":\"sh\",\"address\":\"" + openData[0] + "_" + name + "_" + 1 + "\",\"key\":\"" + openData[1] + "\"}");
                // TODO: 2016/8/1 发socket通知盒子 通知盒子打开电灯
                if (AireJupiter.getInstance().tcpSocket == null) {
                    Log.d("ControlDevicesActivity Socket连接不存在,重连");
                    AireJupiter.getInstance().new_tcp_socket();
                }
                AireJupiter.getInstance().tcpSocket.sendCmd(myIdx_hex, "{\"cmd\":\"sh\",\"address\":\"" + openData[0] + "_" + name + "_" + 1 + "\",\"key\":\"" + openData[1] + "\"}");

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.dismiss();
                        Toast.makeText(ControlDevicesActivity.this, getString(R.string.device_doing_failed), Toast.LENGTH_SHORT).show();
                    }
                }, 10000);
            }

            @Override
            public void onCloseClick(int position) {
                Log.d("ControlDevicesActivity 指定的position: " + position + " 关闭 ");
                loadingDialog.show();

                clickEntity = myDevices.get(position);
                originStatus = clickEntity.getStatus();
                String name = clickEntity.getName();
                clickEntity.setStatus(0);

                String[] closeData = clickEntity.getCloseData().split("_");
                Log.d("ControlDevicesActivity 发送Json" + "{\"cmd\":\"sh\",\"address\":\"" + closeData[0] + "_" + name + "_" + 0 + "\",\"key\":\"" + closeData[1] + "\"}");
                // TODO: 2016/8/1 发socket通知盒子 通知盒子关闭电灯
                if (AireJupiter.getInstance().tcpSocket == null) {
                    Log.d("ControlDevicesActivity Socket连接不存在,重连");
                    AireJupiter.getInstance().new_tcp_socket();
                }
                AireJupiter.getInstance().tcpSocket.sendCmd(myIdx_hex, "{\"cmd\":\"sh\",\"address\":\"" + closeData[0] + "_" + name + "_" + 0 + "\",\"key\":\"" + closeData[1] + "\"}");

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.dismiss();
                        Toast.makeText(ControlDevicesActivity.this, getString(R.string.device_doing_failed), Toast.LENGTH_SHORT).show();
                    }
                }, 10000);
            }
        });
    }

    /**
     * 更新智能开关状态
     *
     * @param callback
     */
    private void uploadDeviceStatus(Callback callback) {
        params.put("idx", myIdx_hex);
        params.put("id", mAddress);
        params.put("number", myDevices.size());
        for (SmartDeviceEntity myDevice : myDevices) {
            params.put(myDevice.getUrlkey(), myDevice.getName() + "_" + myDevice.getOpenData() + "_" + myDevice.getCloseData().split("_")[1]);
            params.put(myDevice.getUrlkey() + "_s", myDevice.getStatus());
        }
        String url = getNetUrl();

        getDataFromNet(url, callback);
    }

    /**
     * 拼接url
     *1,
     * @return
     */
    private String getNetUrl() {
        String paramStr = "";
        Iterator iter = params.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Object key = entry.getKey();
            String val = nullToString(entry.getValue());
            paramStr += paramStr = "&" + key + "=" + URLEncoder.encode(val);
        }
        String url = uploadurl;
        if (!paramStr.equals("")) {
            paramStr = paramStr.replaceFirst("&", "?");
            url += paramStr;
        }
        return url;
    }

    /**
     * 假如obj对象 是null返回""
     *
     * @param obj
     * @return
     */
    public static String nullToString(Object obj) {
        if (obj == null) {
            return "";
        }
        return obj.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("ControlDevicesActivity 进入到onResume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LBMUtil.unregisterReceiver(this, smartHomeReceiver);
    }
}
