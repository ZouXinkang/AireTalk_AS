package com.pingshow.amper.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.pingshow.amper.ComposeActivity;
import com.pingshow.amper.CreateGroupActivity;
import com.pingshow.amper.R;
import com.pingshow.amper.SearchDialog;
import com.pingshow.amper.SettingActivity;
import com.pingshow.amper.UsersActivity;
import com.pingshow.qrcode.CaptureActivity;

/**
 * Created by jack on 2016/5/3.
 */
public class SettingPopupWindow extends PopupWindow implements View.OnClickListener{
    private Context Usercontext;
    /**
     * 显示的数据id
     */
    private int[] textitems = {R.string.pop_creating_chat, R.string.pop_create_group, R.string.add_friends_set, R.string.sweep_add, R.string.setting};

    /**
     * 对应的图片id
     */
    private int[] imgs = {R.drawable.add_pop_sms, R.drawable.add_call, R.drawable.add2, R.drawable.san_addfrends, R.drawable.setting_normal};

    /**
     * popupwindow的高和宽
     */
    private int mWidth;
    private int mHeight;
    /**
     * popupWindow的内容
     */
    private LinearLayout mConvertView;

    /**
     * 内容
     */
    private LinearLayout layout01,layout02,layout03,layout04,layout05;

    private LayoutInflater mInflater;

    /**
     * @param context
     */
    public SettingPopupWindow(Context context) {
        super(context);
        this.Usercontext = context;
        calWidthAndHeight(context);

        mConvertView = (LinearLayout)LayoutInflater.from(context).inflate(R.layout.popup_main,null );

        Log.d("SettingPopupWindowNew2", "mWidth:" + mWidth+" mHeight:"+mHeight);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mWidth, mHeight);
//        ((ListView)mConvertView.findViewById(R.id.lv_popup_setting)).setLayoutParams(lp);
        mConvertView.setLayoutParams(lp);
        //设置
        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());//设置了背景点击外面才会消失

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();//令popup消失
                    return true;
                }
                return false;
            }
        });
        initViews(context);
        initEvent();
    }

    private void initViews(Context context) {
        layout01 = (LinearLayout)mConvertView.findViewById(R.id.llayout01);
        layout02 = (LinearLayout)mConvertView.findViewById(R.id.llayout02);
        layout03 = (LinearLayout)mConvertView.findViewById(R.id.llayout03);
        layout04 = (LinearLayout)mConvertView.findViewById(R.id.llayout04);
        layout05 = (LinearLayout)mConvertView.findViewById(R.id.llayout05);

        layout01.setOnClickListener(this);
        layout02.setOnClickListener(this);
        layout03.setOnClickListener(this);
        layout04.setOnClickListener(this);
        layout05.setOnClickListener(this);

    }

    private void initEvent() {

    }

    /**
     * 计算popupWindow的宽度和高度
     *
     * @param context
     */
    private void calWidthAndHeight(Context context) {
//        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);

        mWidth = (int) (outMetrics.widthPixels * 0.43);
        mHeight = (int) (outMetrics.heightPixels * 0.4);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.llayout01:
//                        Toast.makeText(Usercontext, "0", Toast.LENGTH_SHORT).show();
                Usercontext.startActivity(new Intent(Usercontext, ComposeActivity.class));
                break;
            case R.id.llayout02:
//                        Toast.makeText(Usercontext, "1", Toast.LENGTH_SHORT).show();
                ((UsersActivity)Usercontext).startActivityForResult(new Intent(Usercontext, CreateGroupActivity.class), 2001);
                break;
            case R.id.llayout03:
//                        Toast.makeText(Usercontext, "2", Toast.LENGTH_SHORT).show();
                ((UsersActivity)Usercontext).startActivityForResult(new Intent(Usercontext, SearchDialog.class), 2001);
                break;
            case R.id.llayout04:
//                        Toast.makeText(Usercontext, "3", Toast.LENGTH_SHORT).show();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
                    Intent openCameraIntent = new Intent(Usercontext, CaptureActivity.class);
                    ((UsersActivity)Usercontext).startActivityForResult(openCameraIntent, 105);
                } else {
                    Toast.makeText(Usercontext, Usercontext.getString(R.string.old_sdk), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.llayout05:
//                        Toast.makeText(Usercontext, "4", Toast.LENGTH_SHORT).show();
                Usercontext.startActivity(new Intent(Usercontext, SettingActivity.class));
                break;

            default:
                break;
        }
        dismiss();
    }
}
