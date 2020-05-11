package com.nsv.rselink.display;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.nsv.rselink.Config;
import com.nsv.rselink.base.AbstractPresenter;
import com.nsv.rselink.services.AutoConnectService;
import com.nsv.rselink.services.ThreadPoolManager;
import com.nsv.rselink.utils.LogUtils;
import com.nsv.rselink.widget.HotspotManager.CheckHotspotChangTask;
import com.nsv.rselink.widget.HotspotManager.ClientScanResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;

/**
 * Created by Tianluhua on 2018/3/13.
 * 用于协调UI和数据
 */

public class DisplayPresenter extends AbstractPresenter<DisplayView> implements CheckHotspotChangTask.CallBack {

    public static final String TAG = "DisplayPresenter";

    private boolean isHotSpot = false;

    private String serverIp;
    private Socket touchSocket;
    private DataOutputStream dos;
    private DataInputStream dis;

    private DisplayMode displayMode;

    private Context mContext;
    private Timer checkHotSpotTimer;

    private Handler mHandler = new DisplayHandler(this);


    public DisplayPresenter(Context mContext) {

        this.mContext = mContext;
        displayMode = new DisplayMode(new DisplayMode.CallBack() {
            @Override
            public void loading() {
                if (getView() != null) {
                    getView().loading();
                }
            }

            @Override
            public void disPlayRemoteDesk(Bitmap bitmap) {
                if (getView() != null) {
                    getView().disPlayRemoteDesk(bitmap);
                } else {
                    //todo 这里需要处理
                    //throw new NullPointerException("DisplayPresenter ---> disPlayRemoteDesk --->getView()==null ");
                }
            }

            @Override
            public void fila() {
                if (getView() != null) {
                    getView().fila();
                }
            }

            @Override
            public void connectSucess() {
                if (getView() != null) {
                    getView().connectSucess();
                }
            }

        });

        checkHotSpotTimer = new Timer();


    }

    /**
     * 与远程服务端建立连接
     *
     * @param serverIp 远程服务端设备的
     */
    public void startDisPlayRomoteDesk(String serverIp) {
        LogUtils.i("hdb---startDisPlayRomoteDesk---ip:"+serverIp);
        this.serverIp = serverIp;
        startTouchServer();
        displayMode.startServer(serverIp);
    }

    /**
     * 监听热点连接情况
     */
    public void startChekcoutHotSpotChange() {
        LogUtils.e("tlh", "startChekcoutHotSpotChange--111");
        startChekcoutHotSpotChange(Config.SystemTime.CHECKOUT_DISPLAY_TIMEOUT_DELAY, Config.SystemTime.CHECKOUT_DISPLAY_TIMEOUT);
    }

    /**
     * 定时轮询检测热点是否断开连接
     *
     * @param delay
     * @param period
     */
    public void startChekcoutHotSpotChange(long delay, long period) {
        if (checkHotSpotTimer != null) {
            LogUtils.e("tlh", "startChekcoutHotSpotChange----222");
            CheckHotspotChangTask checkHotspotChangTask = new CheckHotspotChangTask(mContext);
            checkHotspotChangTask.setCallBack(this);
            checkHotSpotTimer.scheduleAtFixedRate(checkHotspotChangTask, delay, period);
        }
    }

    /**
     * 移除定时检测
     */
    public void removeChekcoutHotSpotChange() {
        if (checkHotSpotTimer != null) {
            LogUtils.e("tlh", "removeChekcoutHotSpotChange");
            checkHotSpotTimer.cancel();
        }

    }

    /**
     * 和远程服务器建立TCP连接，用于屏幕事件的交互
     */
    private void startTouchServer() {


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    LogUtils.e(TAG, "tlh--startTouchServer-:" + serverIp);
                    if (touchSocket != null) return;
                    LogUtils.e(TAG, "tlh--startTouchServer111-:" + serverIp);
                    touchSocket = new Socket(serverIp,
                            Config.PortGlob.TOUCHPORT);
                    dos = new DataOutputStream(touchSocket.getOutputStream());
                    dis = new DataInputStream(touchSocket.getInputStream());
                } catch (Exception e) {
                    LogUtils.e(TAG, "hdb--touchServer-ex:" + e.toString());
                    mHandler.sendEmptyMessage(Config.HandlerGlod.TOUCH_EVENT_CONNECT_FAIL);
                }
                receiveAliveData();
            }
        }).start();


    }


    private void releseTouchServer(){
        LogUtils.i("hdb----onDestroy----releseTouchServer:");
        if (dos != null){
            try {
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }if (dis != null){
            try {
                dis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (touchSocket != null){
            try {
                touchSocket.shutdownInput();
                touchSocket.shutdownOutput();
                touchSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dos = null;
        dis = null;
        touchSocket = null;
    }

    public void releseDisplayServer(){
        releseTouchServer();
        if (displayMode != null){
            displayMode.releseDateServer();
         //   displayMode = null;
        }
    }

    public void reConnect(final String ip){
        Log.e(TAG,"hdb------reConnect----");
        releseDisplayServer();


        startDisPlayRomoteDesk(ip);

    }

    /**
     * 发送本地屏幕事件到远程服务端
     *
     * @param actionType 时间类型
     * @param changeX    事件对应的 X 值
     * @param changeY    事件对应的  值
     */
    public synchronized void sendTouchData(final int actionType, final int changeX, final int changeY) {
        LogUtils.i(TAG, "sendTouchData---action:" + actionType + "  changeX:" + changeX
                + "  changeY:" + changeY +"  dos:"+dos );
        /*ThreadPoolManager.newInstance().addExecuteTask(new Runnable() {
            @Override
            public void run() {
                if (dos != null) {
                    if (changeX >= 0 && changeX <= 1024 && changeY >= 0 && changeY <= 600) {
                        JSONObject jObject = new JSONObject();
                        try {
                            jObject.put(Config.MotionEventKey.JACTION, actionType);
                            jObject.put(Config.MotionEventKey.JX, changeX);
                            jObject.put(Config.MotionEventKey.JY, changeY);
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                        byte[] jBytes = jObject.toString().getBytes();
                        byte[] intToByte = new byte[1];
                        intToByte[0] = (byte) jBytes.length;
                        byte[] data = new byte[jBytes.length + 1];
                        System.arraycopy(intToByte, 0, data, 0, 1);
                        System.arraycopy(jBytes, 0, data, 1, jBytes.length);
                        LogUtils.i(TAG, "hdb----data:" + new String(data));
                        try {
                            dos.write(data);
                            dos.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                            LogUtils.i(TAG, "hdb----sendTouchData:" + e.toString());
                        }

                    }

                }

            }
        });*/
        ThreadPoolManager.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                if (dos != null) {
                    if (changeX >= 0 && changeY >= 0) {
                        JSONObject jObject = new JSONObject();
                        try {
                            jObject.put(Config.MotionEventKey.JACTION, actionType);
                            jObject.put(Config.MotionEventKey.JX, changeX);
                            jObject.put(Config.MotionEventKey.JY, changeY);
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                        byte[] jBytes = jObject.toString().getBytes();
                        byte[] intToByte = new byte[1];
                        intToByte[0] = (byte) jBytes.length;
                        byte[] data = new byte[jBytes.length + 1];
                        System.arraycopy(intToByte, 0, data, 0, 1);
                        System.arraycopy(jBytes, 0, data, 1, jBytes.length);
                        LogUtils.i(TAG, "hdb----data:" + new String(data));
                        writeTouchData(data);
                    }

                }
            }

        });

    }
    private synchronized void writeTouchData(byte[] data){
        try {
            if (dos != null){
                dos.write(data);
                dos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            LogUtils.i(TAG, "hdb----sendTouchData:" + e.toString());
        }
    }

    @Override
    public void hotspotHasChanged(ArrayList<ClientScanResult> clients) {
        LogUtils.e(TAG, "hotspotHasChanged--Config.isFullScreen:"+Config.isFullScreen);
        //如果app处于后台时，getView()会返回null
//        if (Config.isFullScreen)
            for (ClientScanResult clientScanResult : clients) {
                //代表设备是通热点链接到手机。因为设备的信息会保存到手机上，当设备连接手机的热点的时候。
                if (serverIp.equals(clientScanResult.getIpAddr())) {
                    isHotSpot = true;
                    //当设备断开热点后，信息可能还保存在手机里面。但是此时设备是不可达的
                    if (!clientScanResult.isReachable()) {
//                        if (getView() != null) {
//                            getView().displayTimeout();
//                        } else {
//                        throw new NullPointerException("getView() is null (hotspotHasChanged)");
                        AutoConnectService instance1 = AutoConnectService.getInstance();
                        instance1.removeDevices();
                        DisPlayActivity instance = DisPlayActivity.getInstance();
                        LogUtils.e(TAG, "hotspotHasChanged------instance:"+instance);
                            if (instance != null && instance.isDisplay){
                                if (getView() != null) {
                                    getView().displayTimeout();
                                }
                            }else{
                                instance.finish();
                            }

//                            LogUtils.i(TAG, "hotspotHasChanged:" + "getView() is null (hotspotHasChanged)");
//                        }
                    }
                }
            }
    }

    long time = 0;
    private void receiveAliveData(){
        try {
            while(true){
                time = SystemClock.uptimeMillis();
                byte[] aLive = new byte[5];
                if (dis == null){
                    return;
                }
                dis.read(aLive);
                LogUtils.i("hdb----timeLive:"+(SystemClock.uptimeMillis() - time));
                time = SystemClock.uptimeMillis();
                mHandler.removeMessages(Config.HandlerGlod.TIME_OUT);
                mHandler.sendEmptyMessageDelayed(Config.HandlerGlod.TIME_OUT,10000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void detachView() {
        super.detachView();
        if (displayMode != null) {
            displayMode.onDestroy();
        }
        if (checkHotSpotTimer != null) {
            LogUtils.i(TAG, "checkHotSpotTimer canceled");
            checkHotSpotTimer.cancel();
        }
    }


    public static class DisplayHandler extends Handler {
        private WeakReference<DisplayPresenter> weakReference;

        public DisplayHandler(DisplayPresenter displayPresenter) {
            weakReference = new WeakReference<DisplayPresenter>(displayPresenter);
        }

        @Override
        public void handleMessage(Message msg) {
            DisplayPresenter displayPresenter = weakReference.get();
            if (displayPresenter == null) return;
            switch (msg.what) {

                case Config.HandlerGlod.TOUCH_EVENT_CONNECT_FAIL:
                    if (displayPresenter.getView() != null) {
                        displayPresenter.getView().initTouchEventFila();
                    }
                    break;

                case Config.HandlerGlod.CONNET_SUCCESS:
                    if (displayPresenter.getView() != null) {
                        displayPresenter.getView().connectSucess();
                    }
                    break;
                case Config.HandlerGlod.TIME_OUT:
                    if (displayPresenter.getView() != null) {
                        displayPresenter.getView().displayTimeout();
                    }
                    break;

                default:
                    break;
            }

        }
    }


}
