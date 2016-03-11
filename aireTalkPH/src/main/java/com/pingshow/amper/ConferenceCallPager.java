package com.pingshow.amper;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.amper.bean.Person;

import org.w3c.dom.Text;

/**
 * Created by jack on 2016/3/2.
 */
public class ConferenceCallPager extends ConferenceBasePager {

    //jack init widget
    private TextView tv_select_country;
    private EditText et_num;
    private Button bt_add;
    private EditText et_iso;
    private RelativeLayout rl_conference_call;


    public ConferenceCallPager(Context context) {
        super((PickupActivity) context);
    }

    @Override
    public View initView() {
        view = View.inflate(context, R.layout.conference_container, null);
        //jack 禁止显示listview,显示拨号界面
        ((LinearLayout) view.findViewById(R.id.ll_conference_add)).setVisibility(View.VISIBLE);

        rl_conference_call = (RelativeLayout) view.findViewById(R.id.rl_conference_call);
        rl_conference_call.setVisibility(View.GONE);

        tv_select_country = (TextView) view.findViewById(R.id.tv_select_country);
        et_num = (EditText) view.findViewById(R.id.et_number);
        et_iso = (EditText) view.findViewById(R.id.et_iso);//国家号
        bt_add = (Button) view.findViewById(R.id.bt_add);

        return view;
    }

    @Override
    public void initData() {
        tv_select_country.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, CountryCodeActivity.class);
                context.startActivityForResult(intent, 0);
            }
        });

        //隐藏键盘
        if (!(et_num.isShown() || et_iso.isShown())) {
            et_num.clearFocus();
            et_iso.clearFocus();
        }

        et_num.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (et_num != null) {
                        InputMethodManager imanager = (InputMethodManager) context
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imanager.hideSoftInputFromWindow(et_num.getWindowToken(), 0);

                    }
                }
            }
        });

        et_iso.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (et_num != null) {
                        InputMethodManager imanager = (InputMethodManager) context
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imanager.hideSoftInputFromWindow(et_num.getWindowToken(), 0);

                    }
                }
            }
        });

        //按钮点击事件
        bt_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = et_num.getText().toString().trim();
                String iso = et_iso.getText().toString().trim();
                if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(iso)) {
                    Toast.makeText(context, context.getResources().getString(R.string.edit_input_empty), Toast.LENGTH_SHORT).show();
                    return;
                }
                String globalnumber = iso + phoneNumber;
                context.addPhoneCall(globalnumber);
            }
        });

    }

    @Override
    public void releaseSrc() {

    }

    @Override
    public void destory() {

    }

    public void setISO(String iso) {
        et_iso.setText("+" + iso);
    }

    public void setCountry(String country) {
        String str = context.getResources().getString(R.string.select_country).split(":")[0];
        tv_select_country.setText(str + ":" + country);
    }
}
