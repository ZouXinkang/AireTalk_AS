package com.pingshow;

import android.app.Application;
import android.content.Context;

import com.pingshow.util.CrashHandler;
import com.pingshow.util.WriteLogs;

/**
 * Created:         Hsia on 16/2/16.
 * Email:           xiaweifeng@pingshow.net
 * Description:     {TODO}(全局的context和全局的异常捕获)
 */
public class APP extends Application {
    public static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler instance = CrashHandler.getInstance();
        instance.init(this);
        WriteLogs.getInstance(this).start();
        context = getApplicationContext();
    }
}
