package com.pingshow.amper;

import android.content.Context;
import android.view.View;

/**
 * Created by jack on 2016/3/2.
 */
public abstract class ConferenceBasePager {
    public PickupActivity context;
    public View view;

    public ConferenceBasePager(PickupActivity context) {
        this.context = context;
        view = initView();
    }

    public View getRootView(){
        return view;
    }

    public abstract View initView();

    public abstract void initData();

    public abstract void releaseSrc();

    public abstract void destory();

}

