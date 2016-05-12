package com.pingshow.amper.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
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
public class SettingPopupWindowNew2 extends PopupWindow {
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
     * 内容的listview
     */
    private ListView mListView;

    private LayoutInflater mInflater;

    /**
     * @param context
     */
    public SettingPopupWindowNew2(Context context) {
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
//        mListView = (ListView) mConvertView.findViewById(R.id.lv_popup_setting);
        mListView.setAdapter(new MyItemsAdapter(context));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
//                        Toast.makeText(Usercontext, "0", Toast.LENGTH_SHORT).show();
                        Usercontext.startActivity(new Intent(Usercontext, ComposeActivity.class));
                        break;
                    case 1:
//                        Toast.makeText(Usercontext, "1", Toast.LENGTH_SHORT).show();
                        ((UsersActivity)Usercontext).startActivityForResult(new Intent(Usercontext, CreateGroupActivity.class), 2001);
                        break;
                    case 2:
//                        Toast.makeText(Usercontext, "2", Toast.LENGTH_SHORT).show();
                        ((UsersActivity)Usercontext).startActivityForResult(new Intent(Usercontext, SearchDialog.class), 2001);

                        break;
                    case 3:
//                        Toast.makeText(Usercontext, "3", Toast.LENGTH_SHORT).show();
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
                            Intent openCameraIntent = new Intent(Usercontext, CaptureActivity.class);
                            ((UsersActivity)Usercontext).startActivityForResult(openCameraIntent, 105);
                        } else {
                            Toast.makeText(Usercontext, Usercontext.getString(R.string.old_sdk), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 4:
//                        Toast.makeText(Usercontext, "4", Toast.LENGTH_SHORT).show();
                        Usercontext.startActivity(new Intent(Usercontext, SettingActivity.class));
                        break;
                }
                dismiss();
            }
        });
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

        mWidth = (int) (outMetrics.widthPixels * 0.4);
        mHeight = (int) (outMetrics.heightPixels * 0.35);
    }

    private class MyItemsAdapter extends BaseAdapter {

        public MyItemsAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return textitems.length;
        }

        @Override
        public Object getItem(int position) {
            return textitems[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.item_popup_main, parent, false);
                holder.mImg = (ImageView) convertView.findViewById(R.id.iv_img);
                holder.mText = (TextView) convertView.findViewById(R.id.tv_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mImg.setImageResource(imgs[position]);
            holder.mText.setText(Usercontext.getString(textitems[position]));
            return convertView;
        }

        private class ViewHolder {
            ImageView mImg;
            TextView mText;

        }
    }
}
