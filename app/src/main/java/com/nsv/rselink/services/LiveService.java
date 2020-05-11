package com.nsv.rselink.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;


import androidx.annotation.Nullable;

import com.nsv.rselink.utils.LogUtils;

import java.lang.ref.WeakReference;

public class LiveService extends Service {
    private static final int TEST_LIVE = 1;
    private static LiveService service;
    public static LiveService getInstance(){
        return service;
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Handler searcherHandler = new LiveService.SearcherHandler(this);
    @Override
    public void onCreate() {
        LogUtils.d("hdb----LiveService-onCreate---");
        service = this;
        checkService();

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d("hdb----LiveService-onStartCommand---");
        return START_STICKY;
    }

    private void checkService(){
        AutoConnectService instance = AutoConnectService.getInstance();
        if (instance == null ){
            startService(new Intent(this,AutoConnectService.class));
        }
        searcherHandler.sendEmptyMessageDelayed(TEST_LIVE,10);
    }


    public static class SearcherHandler extends Handler {

        WeakReference<LiveService> weakReference;

        public SearcherHandler(LiveService mSearcherMode) {
            weakReference = new WeakReference<LiveService>(mSearcherMode);
        }

        @Override
        public void handleMessage(Message msg) {
            final LiveService searcherMode = weakReference.get();
            if (searcherMode == null)
                return;
            switch (msg.what) {
                case TEST_LIVE:
                    LogUtils.d("hdb---LiveService--live-------");

                    searcherMode.checkService();
                    break;



                default:
                    break;
            }


        }
    }
}
