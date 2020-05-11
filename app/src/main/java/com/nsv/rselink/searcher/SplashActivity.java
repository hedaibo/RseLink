package com.nsv.rselink.searcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;


import androidx.annotation.Nullable;

import com.nsv.rselink.Config;
import com.nsv.rselink.R;
import com.nsv.rselink.utils.HideSystemUIUtils;
import com.nsv.rselink.utils.LogUtils;

import java.lang.ref.WeakReference;

public class SplashActivity extends Activity {

    private static final int REMOVE_SPLASH_IMAGE = 1;
    private static final long REMOVE_SPLASH_IMAGE_DELAY = 2000;
    private DevicesHandler mDvicesHandler = new DevicesHandler(this);
    private String remoteServiceIP;

    private static class DevicesHandler extends Handler {
        WeakReference<SplashActivity> mWf;
        DevicesHandler(SplashActivity deviceListActivity){
            mWf = new WeakReference<SplashActivity>(deviceListActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            SplashActivity activity = mWf.get();
            if (activity == null || activity.isFinishing()) return;
            switch (msg.what){
                case REMOVE_SPLASH_IMAGE:
                    activity.startDeviceListActivity();
                    break;
            }
        }
    }

    private void startDeviceListActivity(){
        Bundle bundle = getIntent().getExtras();
        if (null != bundle){
            String remoteServiceIP = bundle.getString(Config.ActionKey.CLIENT_IP_KEY);
            LogUtils.i("hdb---startDeviceListActivity------remoteServiceIP:"+remoteServiceIP);
            if(null != remoteServiceIP){
                remoteServiceIP = getIntent().getExtras().getString(Config.ActionKey.CLIENT_IP_KEY);
                startActivity(getIntent(remoteServiceIP));
            }else{
                startActivity(new Intent(SplashActivity.this,ShowDeviceListActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        }else{
            startActivity(new Intent(SplashActivity.this,ShowDeviceListActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }



        finish();
    }

    private Intent getIntent(String remoteServiceID){
        Intent intent = new Intent(Config.SystemAction.ACTIVITY_DISPAY_REMOTE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle bundle = new Bundle();
        bundle.putString(Config.ActionKey.CLIENT_IP_KEY, remoteServiceID);
        intent.putExtras(bundle);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        HideSystemUIUtils.hideSystemUI(this);
        ShowDeviceListActivity instance = ShowDeviceListActivity.getInstance();
        if (instance != null && instance.isShow){
            startDeviceListActivity();
        }else{
            mDvicesHandler.sendEmptyMessageDelayed(REMOVE_SPLASH_IMAGE,REMOVE_SPLASH_IMAGE_DELAY);
        }

    }

    @Override
    protected void onPause() {
        mDvicesHandler.removeMessages(REMOVE_SPLASH_IMAGE);
        super.onPause();
    }
}
