package com.nsv.rselink.searcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.nsv.rselink.Config;
import com.nsv.rselink.R;
import com.nsv.rselink.bean.DeviceInfo;
import com.nsv.rselink.searcher.adapter.DeviceAdapter;
import com.nsv.rselink.services.AutoConnectService;
import com.nsv.rselink.utils.SPUtils;
import com.nsv.rselink.utils.VersionNumberUtils;
import com.nsv.rselink.widget.RadarView;

import java.util.ArrayList;
import java.util.List;


public class ShowDeviceListActivity extends Activity {
    public static final String TAG = "ShowDeviceListActivity";
    private static final int REMOVE_SPLASH_IMAGE = 1;
    private static final long REMOVE_SPLASH_IMAGE_DELAY = 2000;

    private ListView display_remote_devices_list;
    private RadarView searCherView;
    private TextView searCherView_text;
    private TextView versionNumber;

    private SearcherPrecenter searcherPrecenter;
    private DeviceAdapter deviceAdapter;

    private List<DeviceInfo> remoteDeviceInfos;

    @Override
    protected void onResume() {
        reFindDevices();
        super.onResume();
    }

    private static ShowDeviceListActivity activity;
    private SharedPreferences sp;
    private TextView scanDevices;
    private ImageView ivSplash ;

    public static ShowDeviceListActivity getInstance(){
        return activity;
    }
    public boolean isShow = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showdevicelist);
        sp = getSharedPreferences(SPUtils.SP_NAME, MODE_PRIVATE);
        activity = this;
        AutoConnectService instance = AutoConnectService.getInstance();
        if (null == instance){
            startService(new Intent(this,AutoConnectService.class));
        }else{
//            if(!instance.getSpDisplayBack()){
//                String name = sp.getString(SPUtils.SP_CONNECT_NAME, null);
//                String spCureentIp = instance.getSpCureentIp();
//                if (name != null && spCureentIp != null){
//                    startDispayRemoteByServiceID(spCureentIp);
//                    ShowDeviceListActivity.this.finish();
//                }
//
//            }
        }

        initView();
    }

    private String secondIp = "";
    private void initView() {
        searCherView_text = (TextView) findViewById(R.id.iv_search_text);
        scanDevices = (TextView) findViewById(R.id.tv_scan_devices);
        versionNumber = findViewById(R.id.iv_search_version_number);
        versionNumber.setText(VersionNumberUtils.getVersion(getApplicationContext()));
        display_remote_devices_list = (ListView) findViewById(R.id.remote_device_list);
        deviceAdapter = new DeviceAdapter(this);
        display_remote_devices_list.setAdapter(deviceAdapter);
        display_remote_devices_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DeviceInfo deviceInfo = remoteDeviceInfos.get(position);
                String serverIp = "";
                secondIp = "";
                if (deviceInfo.getIpAddress() != null && deviceInfo.getIp6Address() != null){
                    if (sp.getBoolean(SPUtils.SP_PRIORITY_IPV6,true)){
                        serverIp = deviceInfo.getIp6Address();
                        secondIp = deviceInfo.getIpAddress();
                    }else{
                        serverIp = deviceInfo.getIpAddress();
                        secondIp = deviceInfo.getIp6Address();
                    }

                }else if(deviceInfo.getIpAddress() != null){
                    serverIp = deviceInfo.getIpAddress();
                }else if(deviceInfo.getIp6Address() != null){
                    serverIp = deviceInfo.getIp6Address();
                }


                sp.edit().putString(SPUtils.SP_CONNECT_NAME,remoteDeviceInfos.get(position).getName()).commit();
                sp.edit().putString(SPUtils.SP_CONNECT_IPADDR,serverIp).commit();
//                Toast.makeText(getApplicationContext(),
//                        "serverIp:" + serverIp, Toast.LENGTH_SHORT).show();

                startDispayRemoteByServiceID(serverIp);
                ShowDeviceListActivity.this.finish();
            }
        });

        scanDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reFindDevices();
            }
        });
        ivSplash = findViewById(R.id.iv_splash);
    }

    private void reFindDevices() {
        AutoConnectService instance = AutoConnectService.getInstance();
        if (null != instance){
            instance.reScanDevices();
        }

    }

    /**
     * 通过IP启动显示远程设备桌面情况
     *
     * @param remoteServiceID 远程服务器IP
     */
    public void startDispayRemoteByServiceID(String remoteServiceID) {
        Intent intent = new Intent(Config.SystemAction.ACTIVITY_DISPAY_REMOTE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle bundle = new Bundle();
        bundle.putString(Config.ActionKey.CLIENT_IP_KEY, remoteServiceID);
        bundle.putString(Config.ActionKey.SECOND_IP,secondIp);
//        bundle.putString(Config.ActionKey.SHOW_SPLASH_KEY, Config.ActionKey.SHOW_SPLASH_HIDE);
        intent.putExtras(bundle);
        if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            try {
//                try {
//                    searcherMode.startRemoteService(Config.ActionKey.SERVICE_START_KEY);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    if (getView() != null) {
//                        getView().networkError();
//
//                    }
//                    return;
//
//                }
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Start Activity Error:" + remoteServiceID, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Not found DisplayActivity:" + remoteServiceID, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
//        showSplashImage();
//        mDvicesHandler.sendEmptyMessageDelayed(REMOVE_SPLASH_IMAGE,REMOVE_SPLASH_IMAGE_DELAY);
        super.onStart();
        isShow = true;
        AutoConnectService mService = AutoConnectService.getInstance();
        if(null != mService){
            ArrayList<DeviceInfo> deviceInfos = mService.getDeviceInfos();
            if (null != deviceInfos && deviceInfos.size() > 0){
                searchSuccess(deviceInfos);
            }
            mService.setIsLoopSendBraodCastCount(0);
        }
    }

    /*private void showSplashImage(){
        showTopStateBar(false);
        ivSplash.setVisibility(View.VISIBLE);
    }

    private void hideSplashImage(){
        ivSplash.setVisibility(View.GONE);
        showTopStateBar(true);
    }*/

    @Override
    protected void onStop() {
        super.onStop();
        isShow = false;
//        mDvicesHandler.removeMessages(REMOVE_SPLASH_IMAGE);
//        AutoConnectService instance1 = AutoConnectService.getInstance();
//        if (instance1 != null){
//            instance1.setSpDisplayBack(false);
//        }
    }


    public void searchSuccess(final ArrayList<DeviceInfo> deviceInfos) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                remoteDeviceInfos = deviceInfos;
                if (deviceAdapter != null) {
                    deviceAdapter.setDeviceInfos(deviceInfos);
                    deviceAdapter.notifyDataSetChanged();
                    if (display_remote_devices_list.getVisibility() != View.VISIBLE)
                        display_remote_devices_list.setVisibility(View.VISIBLE);
                }
            }
        });
    }

//    public void updateDevicesTime(Map<String, DeviceInfo> deviceInfos){
//        if (deviceAdapter != null) {
//            deviceAdapter.setDeviceInfos(remoteDeviceInfos);
//        }
//    }

    /*private  DevicesHandler mDvicesHandler = new DevicesHandler(this);
    private static class DevicesHandler extends Handler{
        WeakReference<ShowDeviceListActivity> mWf;
        DevicesHandler(ShowDeviceListActivity deviceListActivity){
            mWf = new WeakReference<ShowDeviceListActivity>(deviceListActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            ShowDeviceListActivity activity = mWf.get();
            if (activity == null || activity.isFinishing()) return;
            switch (msg.what){
                case REMOVE_SPLASH_IMAGE:
                    activity.hideSplashImage();
                    break;
            }
        }
    }

    private void showTopStateBar(boolean show) {
        if (!show) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        }
    }*/
}
