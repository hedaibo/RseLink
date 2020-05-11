package com.nsv.rselink.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;

import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.nsv.rselink.Config;
import com.nsv.rselink.R;
import com.nsv.rselink.bean.DeviceInfo;
import com.nsv.rselink.display.DisPlayActivity;
import com.nsv.rselink.searcher.ShowDeviceListActivity;
import com.nsv.rselink.searcher.SplashActivity;
import com.nsv.rselink.utils.IpUtils;
import com.nsv.rselink.utils.LogUtils;
import com.nsv.rselink.utils.SPUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

;
//import android.support.v7.app.NotificationCompat;

public class AutoConnectService extends Service {
    private static final String TAG = "AutoConnectService";
    private static final int UPDATE_DEVICES = 1;
    private static final int UPDATE_DEVICES_DELAY = 1000;
    private static final int UPDATE_DEVICES_DELAY_OUT = 5500;
    private static final boolean needNotifi = false; //if need notification set true.
    private static final String IPV6_BROADCAST_ADDR = "FF02::1";
    //    private InetAddress broadcastAddress;
//    private DatagramSocket udpBack;
    private MulticastSocket multicastSocket;
    private MulticastSocket multicastSocketIpv6;
    //private DatagramPacket pack;
    //private byte[] data = new byte[100];
    //private String back;
    private String remoteServerIp;
    private String remoteName;
//    private Map<String, DeviceInfo> deviceInfos;
    private ArrayList<DeviceInfo> deviceList;
    private boolean isLoopSendBraodCast = true;
    private List<InetAddress> listBroadcastAddress;
    private ConnectivityManager connectivityManager;
    private MyWifiChangeReceiver wifiChangeReceiver;
    private NotificationManager notifiManager;

    private SharedPreferences sp;

    private Handler searcherHandler = new AutoConnectService.SearcherHandler(this);
    private static AutoConnectService mService;
    private int ipType;

    private InetAddress broadcastAddress6;

    public static AutoConnectService getInstance(){
        return mService;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean isRegisterReceiver = false;
    @Override
    public void onCreate() {
        LogUtils.d("hdb------AutoConnectService----onCreate-----");
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        notifiManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        sp = getSharedPreferences(SPUtils.SP_NAME, MODE_PRIVATE);
//        deviceInfos = new HashMap<>();
        deviceList = new ArrayList<DeviceInfo>();
        initNet(this);
        if (wifiChangeReceiver == null) {
            wifiChangeReceiver = new MyWifiChangeReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
            registerReceiver(wifiChangeReceiver, filter);
            isRegisterReceiver = true;
            searcherHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isRegisterReceiver = false;
                }
            },200);
        }
        searcherHandler.sendEmptyMessageDelayed(Config.HandlerGlod.IS_LOOP_SENDBROADCAST, 200);
//        LiveService instance = LiveService.getInstance();
//        if (instance == null ){
//            startService(new Intent(this,LiveService.class));
//        }
//        searcherHandler.sendEmptyMessageDelayed(TEST_LIVE,1000);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mService = this;
        return START_STICKY;
    }

    public void showNotification(String remoteServiceID, boolean connect){
        if (!needNotifi){
            return;
        }
        String channelId = "1";
        String channelName = "voxx";
        LogUtils.i("noti----showNotification-remoteServiceID:"+remoteServiceID);
        String name = sp.getString(SPUtils.SP_CONNECT_NAME, "");
        RemoteViews remoteView = new RemoteViews(this.getPackageName(), R.layout.notification_item);
        if (connect){
            remoteView.setTextViewText(R.id.tv_noti,name+"\n Connected");
    //        remoteView.setImageViewResource(R.id.iv_noti,R.drawable.paly_selector);
        }else{
            remoteView.setTextViewText(R.id.tv_noti,name+"\n Disconnected");
    //        remoteView.setImageViewResource(R.id.iv_noti,R.drawable.pause_selector);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, getDeviceListIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
        remoteView.setOnClickPendingIntent(R.id.rl_noti,pendingIntent);
        PendingIntent pendingIntentPlay = PendingIntent.getActivity(this, 1, getDisplayIntent(remoteServiceID), PendingIntent.FLAG_UPDATE_CURRENT);
        remoteView.setOnClickPendingIntent(R.id.iv_noti,pendingIntentPlay);
//        Notification.Builder builder = new Notification.Builder(this);

//        new android.support.v4.app.NotificationCompat.Builder(this,"");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,channelId);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContent(remoteView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(channelId,channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            notifiManager.createNotificationChannel(channel);
            builder.setChannelId(channelId);
        }
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setTicker("voxx");
//        builder.setAutoCancel(true);
//        builder.setShowWhen(true);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        /*builder.setPriority(NotificationCompat.PRIORITY_HIGH);*/
//        Notification build = builder.build();
//        build.flags =Notification.FLAG_ONGOING_EVENT&Notification.FLAG_AUTO_CANCEL;
//        build.flags |= Notification.FLAG_AUTO_CANCEL;
//        builder.setContentTitle("Connect");
//        builder.setContentText(name+" has Connect");
//        builder.setOngoing(true);

//        builder.setWhen(System.currentTimeMillis());
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "XXX_Name",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            notifiManager.createNotificationChannel(channel);
        }
        android.support.v4.app.NotificationCompat.Builder builder = new android.support.v4.app.NotificationCompat.Builder(getApplicationContext(), channelId).setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources
                (), R.drawable.ic_launcher)).setSmallIcon(R.drawable.ic_launcher).setContentIntent(pendingIntent).setContentTitle("").setContentText("")
                .setAutoCancel(true).setShowWhen(true).setVisibility(Notification.VISIBILITY_PUBLIC).setPriority(NotificationCompat.PRIORITY_HIGH);*/

        notifiManager.notify(1,builder.build());


    }

    private Intent getDeviceListIntent(){
        Intent intent = new Intent("com.sat.action.display.splash");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        Bundle bundle = new Bundle();
//        bundle.putString(Config.ActionKey.SHOW_SPLASH_KEY, Config.ActionKey.SHOW_SPLASH_HIDE);
//        intent.putExtras(bundle);
        return intent;
    }
    private Intent getDisplayIntent(String remoteServiceID){
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle bundle = new Bundle();
        bundle.putString(Config.ActionKey.CLIENT_IP_KEY, remoteServiceID);
//        bundle.putString(Config.ActionKey.SHOW_SPLASH_KEY, Config.ActionKey.SHOW_SPLASH_SHOW);
        intent.putExtras(bundle);
        return intent;
    }


//    private Intent getintent(String remoteServiceID){
//        Intent intent = new Intent(Config.SystemAction.ACTIVITY_DISPAY_REMOTE);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        Bundle bundle = new Bundle();
//        bundle.putString(Config.ActionKey.CLIENT_IP_KEY, remoteServiceID);
//        bundle.putString(Config.ActionKey.SHOW_SPLASH_KEY, Config.ActionKey.SHOW_SPLASH_SHOW);
//        intent.putExtras(bundle);
//        return intent;
//    }

    private void cancelNoti(){
        if (!needNotifi){
            return;
        }
        notifiManager.cancel(1);
    }

   // private boolean isIPv6 = false;
    private void initNet(Context mContext) {
        LogUtils.i("hdb------initNet---");
//        try {
            close();

            sendBoradcastCount = 0;
            removeDevices();
            if (showNoti == false){
                cancelNoti();
                showNoti = true;
            }

            DisPlayActivity instance = DisPlayActivity.getInstance();
            LogUtils.i("hdb----MyWifiChangeReceiver----instance:"+instance);
            if (null != instance){
                instance.finish();
            }
            InetAddress localIp = IpUtils.getLocalIp();
            ipType = IpUtils.getLocalIpType();

            if (localIp == null || ipType == IpUtils.IP_TYPE_NORMAL){
                Log.i(TAG,"hdb---error-----localIp:"+localIp);
                return;
            }


            IpUtils.openWifiBrocast(mContext); // for some phone can


//        try {
//
//        //    multicastSocket.setBroadcast(true);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

            if((ipType & IpUtils.IP_TYPE_IPV4) == IpUtils.IP_TYPE_IPV4){//has ipv4 addr

                try {
                    if (multicastSocket == null) {
                        multicastSocket = new MulticastSocket(Config.PortGlob.MULTIPORT);
                    }
                    listBroadcastAddress = IpUtils.getListBroadcastAddress();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    multicastSocket.setBroadcast(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (listBroadcastAddress != null && listBroadcastAddress.size() > 0){
                    try {
                        multicastSocket.joinGroup(listBroadcastAddress.get(0));

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i(TAG, "hdb--111---IP_TYPE_IPV4--joinGroup-error-"+listBroadcastAddress.get(0));
                        //mHandler.removeMessages(GET_BROADCASTADDRESS);
                        //mHandler.sendEmptyMessageDelayed(GET_BROADCASTADDRESS, GET_BROADCASTADDRESS_DELAY);
                    }

                    try {

                        if (listBroadcastAddress.size() == 2 ){
                            multicastSocket.joinGroup(listBroadcastAddress.get(1));
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i(TAG, "hdb---222--IP_TYPE_IPV4--joinGroup-error-");
                        //mHandler.removeMessages(GET_BROADCASTADDRESS);
                        //mHandler.sendEmptyMessageDelayed(GET_BROADCASTADDRESS, GET_BROADCASTADDRESS_DELAY);
                    }
                }

                try {
                    multicastSocket.setLoopbackMode(true);
                //    multicastSocket.setTimeToLive(255);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                ThreadPoolManager.getInstance().execute(new Runnable() {
                    @Override
                    public void run() {
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                        isLoopSendBraodCast = true;
                        while(isLoopSendBraodCast){
                            receiverBack();
                        }
                        LogUtils.i(TAG,"hdb--ipv4-receiverBack--out----isLoopSendBraodCast:"+isLoopSendBraodCast);
                    }
                });
            }

            if ((ipType & IpUtils.IP_TYPE_IPV6) == IpUtils.IP_TYPE_IPV6) { //has ipv6 addr
                LogUtils.i("hdb----isIPv6---");
                try {
                    if (multicastSocketIpv6 == null) {
                        multicastSocketIpv6 = new MulticastSocket(Config.PortGlob.MULTIPORT_IPV6);
                    }
                    broadcastAddress6 = InetAddress.getByName(IPV6_BROADCAST_ADDR);
                    multicastSocketIpv6.setNetworkInterface(IpUtils.getIpv6NetworkInterface());
                    InetSocketAddress socketAddress = new InetSocketAddress(broadcastAddress6, Config.PortGlob.MULTIPORT_IPV6);
                    multicastSocketIpv6.joinGroup(socketAddress, IpUtils.getIpv6NetworkInterface());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "hdb-----IP_TYPE_IPV6--joinGroup-error-");
                }
                try {
                    multicastSocketIpv6.setLoopbackMode(true);
                //    multicastSocketIpv6.setTimeToLive(255);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }


                ThreadPoolManager.getInstance().execute(new Runnable() {
                    @Override
                    public void run() {
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                        isLoopSendBraodCast = true;
                        while(isLoopSendBraodCast){
                            receiverBackIpv6();
                        }
                        Log.i(TAG,"hdb--ipv6-receiverBack--out----isLoopSendBraodCast:"+isLoopSendBraodCast);
                    }
                });
            }

        searcherHandler.sendEmptyMessageDelayed(UPDATE_DEVICES,UPDATE_DEVICES_DELAY);





    }

    /**
     * 通过向广播地址发送信息，让服务端做出回应
     *
     * @throws IOException
     */
    private void sendBroadCast() throws IOException {
        DisPlayActivity instance = DisPlayActivity.getInstance();
        if (!isLoopSendBraodCast){
            return;
        }
//        String ipAddress = IpUtils.getHostIP();
        if ((ipType & IpUtils.IP_TYPE_IPV6) == IpUtils.IP_TYPE_IPV6 && multicastSocketIpv6 != null){
            InetAddress localIp = IpUtils.getLocalIp();
            if (localIp == null){
                return;
            }
            String hostAddress = localIp.getHostAddress();
            String[] split = hostAddress.split("%");
            byte[] data = ("phoneip:" + split[0]).getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length,
                    Inet6Address.getByName(IPV6_BROADCAST_ADDR), Config.PortGlob.MULTIPORT_IPV6);
            if(!isLoopSendBraodCast){
                return;
            }
            LogUtils.i(TAG, "hdb----send---data:" + new String(data));
            multicastSocketIpv6.send(packet);
        }
        if ((ipType & IpUtils.IP_TYPE_IPV4) == IpUtils.IP_TYPE_IPV4 && multicastSocket != null){
            ArrayList<String> hostListIP = IpUtils.getHostListIP();
//        String macAddress = IpUtils.getMacAddressFromIp();
            //       LogUtils.i(TAG, "hdb----send---hostListIP:" + hostListIP);
            LogUtils.i(TAG, "hdb----send---hostListIP:" + hostListIP+"  listBroadcastAddress:"+listBroadcastAddress);
            if (hostListIP != null && hostListIP.size() > 0 && listBroadcastAddress != null && listBroadcastAddress.size() > 0) {
                LogUtils.i(TAG, "hdb----send---hostListIP:" + hostListIP.size()+"  listBroadcastAddress:"+listBroadcastAddress.size());
                byte[] data = ("phoneip:" + hostListIP.get(0)).getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length,
                        listBroadcastAddress.get(0), Config.PortGlob.MULTIPORT);
                if(!isLoopSendBraodCast){
                    return;
                }
                multicastSocket.send(packet);
                if (listBroadcastAddress.size() > 1){

                    DatagramPacket packet1 = new DatagramPacket(data, data.length,
                            listBroadcastAddress.get(1), Config.PortGlob.MULTIPORT);
                    delay(100);
                    multicastSocket.send(packet1);


                }
//            receiverBack();

            }
        }


    }

    private void delay(long time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void close(){
        isLoopSendBraodCast = false;
        if (multicastSocket != null){
            if ((ipType & IpUtils.IP_TYPE_IPV4) == IpUtils.IP_TYPE_IPV4 && listBroadcastAddress != null && listBroadcastAddress.size() > 0) {
                try {
                    multicastSocket.leaveGroup(listBroadcastAddress.get(0));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (listBroadcastAddress.size() == 2){
                    try {
                        multicastSocket.leaveGroup(listBroadcastAddress.get(1));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            multicastSocket.close();
            multicastSocket = null;
        }

        if (multicastSocketIpv6 != null){

            if ((ipType & IpUtils.IP_TYPE_IPV6) == IpUtils.IP_TYPE_IPV6 && broadcastAddress6 != null) {
                try {
                    multicastSocketIpv6.leaveGroup(broadcastAddress6);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            multicastSocketIpv6.close();
            multicastSocketIpv6 = null;
        }
    }

//    public void setIsLoopSendBraodCast(boolean send){
//        isLoopSendBraodCast = send;
//        if (isLoopSendBraodCast){
//            searcherHandler.sendEmptyMessageDelayed(Config.HandlerGlod.IS_LOOP_SENDBROADCAST, 20);
//            com.sat.actionpic.services.ThreadPoolManager.getInstance().execute(new Runnable() {
//                @Override
//                public void run() {
//                    while(isLoopSendBraodCast){
//                        receiverBack();
//                    }
//                }
//            });
//        }
//    }

    public void setIsLoopSendBraodCastCount(int count){
        sendBoradcastCount = count;
        searcherHandler.sendEmptyMessageDelayed(Config.HandlerGlod.IS_LOOP_SENDBROADCAST, 20);
    }

    private long cureentTime;
    private boolean showNoti = true;

    private boolean hasName(String name){
        if (deviceList.size() == 0) return false;
        for (int i=0;i<deviceList.size();i++){
            if(name.equalsIgnoreCase(deviceList.get(i).getName())){
                return true;
            }
        }
        return false;
    }

    private boolean hasIp(String ip){
        if (deviceList.size() == 0) return false;
        for (int i=0;i<deviceList.size();i++){
            if(ip.equalsIgnoreCase(deviceList.get(i).getIpAddress()) || ip.equalsIgnoreCase(deviceList.get(i).getIp6Address())){
                return true;
            }
        }
        return false;
    }

    private DeviceInfo getDevice(String ip){
        if (deviceList.size() == 0) return null;
        for (int i=0;i<deviceList.size();i++){
            if(ip.equalsIgnoreCase(deviceList.get(i).getIpAddress()) || ip.equalsIgnoreCase(deviceList.get(i).getIp6Address())){
                return deviceList.get(i);
            }
        }
        return null;
    }

    /**
     * 接受远程设备反馈的信息，添加到deviceInfos
     */
    private void receiverBack() {
        //Log.i(TAG,"hdb---receiverBack--multicastSocket:"+multicastSocket);
        if (multicastSocket == null){return;}
            try {
                byte[] data = new byte[100];
                DatagramPacket pack = new DatagramPacket(data, data.length);
                if (!multicastSocket.isClosed())
                    multicastSocket.receive(pack);
                String back = new String(pack.getData(), pack.getOffset(),
                        pack.getLength());
                LogUtils.i(TAG,"hdb---receiverBack--"+back);
                handleReceiverData(back);
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.i(TAG, "tlh -------- " + e.toString());
                if (e instanceof SocketTimeoutException) {
//                searcherHandler.sendEmptyMessage(Config.HandlerGlod.TIME_OUT);
                }
            }
    }

    /**
     * 接受远程设备反馈的信息，添加到deviceInfos  ipv6
     */
    private void receiverBackIpv6() {
        if (multicastSocketIpv6 == null){return;}
        try {
            byte[] data = new byte[100];
            DatagramPacket pack = new DatagramPacket(data, data.length);

                multicastSocketIpv6.receive(pack);
            String back = new String(pack.getData(), pack.getOffset(),
                    pack.getLength());
            LogUtils.i(TAG,"hdb---receiverBackIpv6--"+back);
            handleReceiverData(back);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG, "tlh -------- " + e.toString());
        }
    }

    private synchronized void handleReceiverData(String back){
        if (back != null && ((back.startsWith("serverip:") || back.startsWith("server6ip")))) {

            cureentTime = SystemClock.uptimeMillis();
            if (back.startsWith("serverip:")){
                String[] split = back.split(":");
                if (split.length == 3){
                    remoteServerIp = split[1];
                    remoteName = split[2];
                    DeviceInfo deviceInfo = getDevice(remoteServerIp);
                    if (deviceInfo == null){
                        DeviceInfo deviceInfo1 = new DeviceInfo(remoteServerIp,remoteName);
                        deviceInfo1.setTime(cureentTime);
                        deviceList.add(deviceInfo1);
                        ShowDeviceListActivity activity = ShowDeviceListActivity.getInstance();
                        if (null != activity && activity.isShow) {
                            activity.searchSuccess(deviceList);
                        }
                    }else{
                        deviceInfo.setTime(cureentTime);
                    }
                }

            }else if(back.startsWith("server6ip:")){
                String[] split = back.split(":&&:");
                String remoteServerIp = split[2];
                remoteName = split[3];
                String ipv6Address = split[1];
                LogUtils.i(TAG,"hdb--------remoteServerIp:"+remoteServerIp+"  remoteName:"+remoteName+"  ipv6:"+ipv6Address);
                DeviceInfo deviceInfo = getDevice(remoteServerIp);
                DeviceInfo device6Info = getDevice(ipv6Address);
                if (deviceInfo == null && device6Info == null){
                    DeviceInfo deviceInfo1 = new DeviceInfo(remoteServerIp,remoteName);
                    deviceInfo1.setIp6Address(ipv6Address);
                    deviceInfo1.setTime(cureentTime);
                    deviceList.add(deviceInfo1);

                    ShowDeviceListActivity activity = ShowDeviceListActivity.getInstance();
                    if (null != activity && activity.isShow) {
                        activity.searchSuccess(deviceList);
                    }
                }else{
                    if (deviceInfo != null && device6Info == null){
                        deviceInfo.setTime(cureentTime);
                        deviceInfo.setIp6Address(ipv6Address);
                    }else if (deviceInfo == null && device6Info != null){
                        device6Info.setTime(cureentTime);
                        device6Info.setIpAddress(remoteServerIp);
                    }else if (deviceInfo != null && device6Info != null){
                        deviceInfo.setTime(cureentTime);
                    }
                }
            }
//            else{
//                remoteName = split[split.length - 1];
//                remoteServerIp = back.substring(10,back.length()- remoteName.length() -1);
//                LogUtils.i("hdb---remoteServerIp:"+remoteServerIp+"  remoteName:"+remoteName);
//            }

          //  synchronized (mLock) {
          /*      if (!deviceInfos.containsKey(remoteServerIp)) {
                    //if (hasName(remoteName)){
                    if(hasIp(remoteServerIp)){
                        DeviceInfo deviceInfo = deviceInfos.get(remoteServerIp);
                        if (deviceInfo != null){
                            deviceInfo.setTime(cureentTime);
                            deviceInfo.setName(remoteName);
                        }

                    }else {
                        DeviceInfo mDeviceInfo = new DeviceInfo(remoteServerIp, remoteName);
                        mDeviceInfo.setTime(cureentTime);
                        deviceInfos.put(remoteServerIp, mDeviceInfo);
                    }
                    ShowDeviceListActivity activity = ShowDeviceListActivity.getInstance();
                    if (null != activity && activity.isShow) {
                        activity.searchSuccess(deviceInfos);
                    }
                } else {
                    DeviceInfo deviceInfo = deviceInfos.get(remoteServerIp);
                    deviceInfo.setTime(cureentTime);
                }
         //   }
            LogUtils.i("noti-----showNoti:"+showNoti);
            if(showNoti && needNotifi){
                DeviceInfo deviceInfo = deviceInfos.get(remoteServerIp);
                String name = sp.getString(SPUtils.SP_CONNECT_NAME, "");
                if (name.equalsIgnoreCase(deviceInfo.getName())){
                    showNoti = false;
                    showNotification(remoteServerIp,false);
                }

            }*/
        }else if(back != null && back.startsWith("start")){
            String[] split = back.split(":");
            remoteServerIp = split[1];
            remoteName = split[2];
            DisPlayActivity activity = DisPlayActivity.getInstance();
            LogUtils.d("hdb---hasStart:"+hasStart+"  activity:"+activity);
            if (activity != null){
                LogUtils.d("hdb---activity.isDisplay:"+activity.isDisplay);
            }
            if (!hasStart && (null == activity || !activity.isDisplay)){
                hasStart = true;
                startDispayRemoteByServiceID(remoteServerIp);
                if (hasStart){
                    searcherHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            hasStart = false;
                        }
                    },1000);
                }
            }
        }
    }

    private boolean isConnectDevice(String remoteName) {
        for (int i = 0;i < deviceList.size();i++){
            String name = sp.getString(SPUtils.SP_CONNECT_NAME, "");
            if(name.endsWith(deviceList.get(i).getName())){
                return true;
            }
        }
        return false;
    }

    private boolean hasStart = false;

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

    public ArrayList<DeviceInfo> getDeviceInfos(){
        return deviceList;
    }
//    public void removeDeviceInfos(List<DeviceInfo> infos){
//        for (int i = 0;i<infos.size();i++){
//            if (deviceInfos.containsKey(infos.get(i).getIpAddress())){
//                deviceInfos.remove(infos.get(i).getIpAddress());
//            }
//        }
//    }


    public static class SearcherHandler extends Handler {
        WeakReference<AutoConnectService> weakReference;

        public SearcherHandler(AutoConnectService mSearcherMode) {
            weakReference = new WeakReference<AutoConnectService>(mSearcherMode);
        }

        @Override
        public void handleMessage(Message msg) {
            final AutoConnectService searcherMode = weakReference.get();
            if (searcherMode == null)
                return;
            switch (msg.what) {
                case Config.HandlerGlod.SCAN_DEVICE_SUCESS:
//                    if (searcherMode.callBack != null && searcherMode.deviceInfos != null && searcherMode.deviceInfos.size() > 0) {
//                        searcherMode.callBack.searchSuccess(searcherMode.deviceInfos);
//                        searcherMode.callBack.searchEnd();
//                    }

                    break;

                case Config.HandlerGlod.TIME_OUT:
//                    if (searcherMode.callBack != null) {
//                        searcherMode.callBack.searchOutTime();
//                    }

                    break;

                case Config.HandlerGlod.IS_LOOP_SENDBROADCAST:

                    ThreadPoolManager.getInstance().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                searcherMode.sendBroadCast();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    searcherMode.sendBoradcastCount ++;
                    if (searcherMode.isLoopSendBraodCast) {
                        if(hasMessages(Config.HandlerGlod.IS_LOOP_SENDBROADCAST)){
                            removeMessages(Config.HandlerGlod.IS_LOOP_SENDBROADCAST);
                        }
                        ShowDeviceListActivity instance = ShowDeviceListActivity.getInstance();
                        if (instance != null && instance.isShow){
                            sendEmptyMessageDelayed(Config.HandlerGlod.IS_LOOP_SENDBROADCAST, Config.HandlerGlod.IS_LOOP_SENDBROADCAST_DELAY);
                        }else{
                            if (needNotifi){
                                sendEmptyMessageDelayed(Config.HandlerGlod.IS_LOOP_SENDBROADCAST, Config.HandlerGlod.IS_LOOP_SENDBROADCAST_DELAY_LONG);
                            }

                        }

                    }

                    break;
                case Config.HandlerGlod.NET_ERROR:
//                    if (searcherMode.callBack != null) {
//                        searcherMode.callBack.networkError();
//                    }
                    break;

                case Config.HandlerGlod.SEARCHER_TIMEOUT:
//                    if (searcherMode.callBack != null) {
//                        searcherMode.callBack.searchOutTime();
//                    }
                    break;
                case UPDATE_DEVICES:
                    LogUtils.i("hdb------UPDATE_DEVICES-------");
                    searcherMode.updateDevices();
                    if (hasMessages(UPDATE_DEVICES)){
                        removeMessages(UPDATE_DEVICES);
                    }
                    ShowDeviceListActivity activity = ShowDeviceListActivity.getInstance();
                    if ( needNotifi || (null != activity && activity.isShow)) {
                        sendEmptyMessageDelayed(UPDATE_DEVICES,UPDATE_DEVICES_DELAY);
                    }

                    break;

                case Config.HandlerGlod.HOSTPOT_INITNET:
                    LogUtils.i("hdb-----HOSTPOT_INITNET----");
                    searcherMode.initNet(searcherMode.getApplicationContext());
                    sendEmptyMessageDelayed(Config.HandlerGlod.IS_LOOP_SENDBROADCAST, 200);
                    break;



                default:
                    break;
            }


        }
    }

    private long sendBoradcastCount = 0;

    public void removeDevices(){
        synchronized (mLock){
            deviceList.clear();
        }
        if (showNoti == false){
            cancelNoti();
            showNoti = true;
        }
        ShowDeviceListActivity activity = ShowDeviceListActivity.getInstance();
        if(null != activity && activity.isShow){
            activity.searchSuccess(deviceList);
        }
        sendBoradcastCount = 0;


    }

    public void reScanDevices(){
        LogUtils.i("hdb------reScanDevices---");
        initNet(this);
        searcherHandler.removeMessages(Config.HandlerGlod.IS_LOOP_SENDBROADCAST);
        searcherHandler.sendEmptyMessageDelayed(Config.HandlerGlod.IS_LOOP_SENDBROADCAST, 200);
        removeDevices();
    }
    private Object mLock = new Object();

    ArrayList<DeviceInfo> tempList = null;
    private void updateDevices(){
        synchronized (mLock){
            LogUtils.i("hdb---UPDATE_DEVICES---size:"+deviceList.size()+"  "+DisPlayActivity.getInstance());
            if (deviceList.size() <= 0) return;
//            if (DisPlayActivity.getInstance() != null) return;
           // ArrayList<DeviceInfo> devices = new ArrayList<>(deviceInfos.values());
            boolean needMove = false;
            if (tempList == null){
                tempList = new ArrayList<DeviceInfo>();
            }
            tempList.clear();
            for (int i=0;i<deviceList.size();i++){
                LogUtils.i("hdb---UPDATE_DEVICES---updatetime:"+(SystemClock.uptimeMillis() - deviceList.get(i).getTime()));
                if ((SystemClock.uptimeMillis() - deviceList.get(i).getTime()) > UPDATE_DEVICES_DELAY_OUT ){
                   // deviceList.remove(devices.get(i).getIpAddress());
                    needMove = true;
                    tempList.add(deviceList.get(i));
                    if (deviceList.get(i).getName().equalsIgnoreCase(sp.getString(SPUtils.SP_CONNECT_NAME,""))){
                        cancelNoti();
                        showNoti = true;
                    }
                }
            }
            if (needMove){
                deviceList.removeAll(tempList);
                tempList.clear();
                ShowDeviceListActivity activity = ShowDeviceListActivity.getInstance();
                if(null != activity && activity.isShow){
                    activity.searchSuccess(deviceList);
                }
            }
        }

    }

    private class MyWifiChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context content, Intent intent) {
            if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equalsIgnoreCase(intent.getAction())){
                LogUtils.i("hdb-----WIFI_AP_STATE_CHANGED----");
                if (isRegisterReceiver) return;
                sendBoradcastCount = 0;
                if (searcherHandler.hasMessages(Config.HandlerGlod.HOSTPOT_INITNET)){
                    searcherHandler.removeMessages(Config.HandlerGlod.HOSTPOT_INITNET);
                }
                searcherHandler.sendEmptyMessageDelayed(Config.HandlerGlod.HOSTPOT_INITNET, Config.HandlerGlod.HOSTPOT_INITNET_DELAY);

            }else if(ConnectivityManager.CONNECTIVITY_ACTION.equalsIgnoreCase(intent.getAction())){
                LogUtils.i("hdb--------CONNECTIVITY_ACTION----");
                NetworkInfo networkInfo = connectivityManager
                        .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (networkInfo != null && networkInfo.isConnected()) {
                    if (searcherHandler.hasMessages(Config.HandlerGlod.HOSTPOT_INITNET)){
                        searcherHandler.removeMessages(Config.HandlerGlod.HOSTPOT_INITNET);
                    }
                    searcherHandler.sendEmptyMessageDelayed(Config.HandlerGlod.HOSTPOT_INITNET, 200);
                } else {
                    //close();
                    if (searcherHandler.hasMessages(Config.HandlerGlod.HOSTPOT_INITNET)){
                        searcherHandler.removeMessages(Config.HandlerGlod.HOSTPOT_INITNET);
                    }
                    searcherHandler.sendEmptyMessageDelayed(Config.HandlerGlod.HOSTPOT_INITNET, 200);

                }
            }

        }

    }

    public void setSpDisplayBack(boolean value){
        sp.edit().putBoolean("back",value).commit();
    }
    public boolean getSpDisplayBack(){
        return sp.getBoolean("back",false);
    }

    public void setSpCureentIp( String ip){
        sp.edit().putString("cureentip",ip).commit();
    }

    public String getSpCureentIp(){
        return sp.getString("cureentip",null);
    }


    @Override
    public void onDestroy() {
        LogUtils.d("hdb-----onDestroy----");
        super.onDestroy();
    }
}
