package com.pingshow.amper;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by jack on 2016/4/25.
 */
public class LoadingDialog extends Dialog{
    private TextView tv;
    private String description;
    public LoadingDialog(Context context) {
        super(context,R.style.loadingDialogStyle);
    }

    public LoadingDialog(Context context, int theme) {
        super(context, theme);
    }

    public LoadingDialog(Context context,String description) {
        super(context,R.style.loadingDialogStyle);
        this.description =description;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_loading);
        tv = (TextView)findViewById(R.id.tv);
        //设置显示内容
        tv.setText(description);
        RelativeLayout mRelativeLayout = (RelativeLayout)this.findViewById(R.id.rl_dialog);
        mRelativeLayout.getBackground().setAlpha(210);
    }
}
