package com.nsv.rselink;

import android.app.Application;

import com.nsv.rselink.utils.MyException;

public class NsvApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
//        ZXingLibrary.initDisplayOpinion(this);
        MyException instance = MyException.instance();
        instance.initData(getApplicationContext());
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }
}
