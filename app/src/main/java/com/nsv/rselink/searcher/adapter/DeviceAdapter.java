package com.nsv.rselink.searcher.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import com.nsv.rselink.R;
import com.nsv.rselink.bean.DeviceInfo;
import com.nsv.rselink.utils.LogUtils;
import com.nsv.rselink.utils.SPUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tianluhua on 2018/3/13.
 */

public class DeviceAdapter extends BaseAdapter {

    private static final String TAG = "DeviceAdapter";

    private Context mContext;
    private List<DeviceInfo> deviceInfos = new ArrayList();
    private List<DeviceInfo> devicesTemp = new ArrayList();

    private Object mLock = new Object();

    public DeviceAdapter(Context mContext) {
        this.mContext = mContext;
    }

    public void setDeviceInfos(List<DeviceInfo> remoteDeviceInfos) {
        this.deviceInfos.clear();
        this.deviceInfos.addAll(remoteDeviceInfos);
    }



    public List<DeviceInfo> getDeviceInfos() {
        return deviceInfos;
    }

    @Override
    public int getCount() {
        return (deviceInfos.size() > 0) ? deviceInfos.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vHolder;
        if (convertView == null) {
            vHolder = new ViewHolder();
            convertView = View.inflate(mContext,
                    R.layout.activity_searcher_listview_item, null);
            vHolder.deviceName = (TextView) convertView
                    .findViewById(R.id.tv_list_ip);
            convertView.setTag(vHolder);
        }
//        LogUtils.i(TAG, "hdb---name:" + deviceInfos.get(position).getName());
        vHolder = (ViewHolder) convertView.getTag();
        if (deviceInfos.size() > position) {
            DeviceInfo deviceInfo = deviceInfos.get(position);
            String name = deviceInfo.getName();
            String address = deviceInfo.getValidAddress();
            LogUtils.e("tlh---name.size():"+name.length());
            LogUtils.e("tlh---address.size():"+address.length());

            Spannable span = new SpannableString(name + "\n" + address);
            //让设备名称和IP地址有所区别
            span.setSpan(new AbsoluteSizeSpan(29), name.length(),  name.length()+address.length()+1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            vHolder.deviceName.setText(span);
//            String spName = mContext.getSharedPreferences(SPUtils.SP_NAME, Context.MODE_PRIVATE).getString(SPUtils.SP_CONNECT_NAME, "");
            String spName = mContext.getSharedPreferences(SPUtils.SP_NAME, Context.MODE_PRIVATE).getString(SPUtils.SP_CONNECT_IPADDR, "");
            if (address.equalsIgnoreCase(spName)){
                vHolder.deviceName.setTextColor(0xff00ff66);
            }else{
                vHolder.deviceName.setTextColor(Color.WHITE);
            }

        }

        return convertView;
    }

    class ViewHolder {
        TextView deviceName;
    }


//    private synchronized void updateLiveDevice(){
//        LogUtils.i("hdb---updateLiveDevice--"+deviceInfos.size());
//        if(deviceInfos.size() <= 0) return;
//        if (devicesTemp.size() > 0) devicesTemp.clear();
//
//        for(int i=0;i<deviceInfos.size();i++){
//            DeviceInfo deviceInfo = deviceInfos.get(i);
//            LogUtils.i("hdb---updateLiveDevice--"+deviceInfo.getTime()+"  :"+SystemClock.uptimeMillis()+"  "+(SystemClock.uptimeMillis() - deviceInfo.getTime()));
//            if ((SystemClock.uptimeMillis() - deviceInfo.getTime()) > 3000){
//                devicesTemp.add(deviceInfo);
//            }
//        }
//        if (devicesTemp.size() > 0){
//            deviceInfos.removeAll(devicesTemp);
//            LogUtils.i("hdb---updateLiveDevice--devicesTemp:"+devicesTemp.size());
//            AutoConnectService instance = AutoConnectService.getInstance();
//            if (instance != null){
//                instance.removeDeviceInfos(devicesTemp);
//            }
//
//            if(mContext != null && ShowDeviceListActivity.getInstance() != null){
//                ShowDeviceListActivity.getInstance().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        notifyDataSetChanged();
//                    }
//                });
//            }
//
//        }
//        if (!mHandler.hasMessages(DeviceHandler.UPDATE_DEVICES)){
//            mHandler.sendEmptyMessageDelayed(DeviceHandler.UPDATE_DEVICES,DeviceHandler.UPDATE_DEVICES_DELAY);
//        }
//    }




 }
