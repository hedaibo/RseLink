package com.nsv.rselink.display;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;


import com.nsv.rselink.Config;
import com.nsv.rselink.R;
import com.nsv.rselink.base.AbstractMVPActivity;
import com.nsv.rselink.services.AutoConnectService;
import com.nsv.rselink.utils.HideSystemUIUtils;
import com.nsv.rselink.utils.LogUtils;
import com.nsv.rselink.utils.SPUtils;
import com.nsv.rselink.widget.ImageSurfaceView;
import com.nsv.rselink.widget.NetworkDialog;

import java.lang.ref.WeakReference;

/**
 * Created by Tianluhua on 2018/3/13.
 */

public class DisPlayActivity extends AbstractMVPActivity<DisplayView, DisplayPresenter> implements DisplayView {


    public static final String TAG = "DisPlayActivity";
    private static final int REMOVE_SPLASH_IMAGE = 1;
    private static final long REMOVE_SPLASH_IMAGE_DELAY = 2000;

    private ImageSurfaceView displayRemoteDeviceSurface;
    private ProgressBar displayRemoteDeviceWaitProgress;

    private String remoteServiceIP;
    private DisplayPresenter displayPresenter;

    private float densityX = 0;
    private float densityY = 0;
    private float rk3288X = 0;
    private float rk3288Y = 0;

    private int changeX = 0;
    private int changeY = 0;

    public boolean isDisplay = false;
    private FragmentManager fragmentManager;
    private ImageView ivSplash;

    private static  DisPlayActivity mActivity;
    private String secondIp = "";
    private SharedPreferences sp;

    public static  DisPlayActivity getInstance() {
        return mActivity;
    }

    private DisHandler disHandler = new DisHandler(DisPlayActivity.this);
    public static class DisHandler extends Handler {
        private WeakReference<DisPlayActivity> weakReference;

        public DisHandler(DisPlayActivity disPlayActivity) {
            weakReference = new WeakReference<DisPlayActivity>(disPlayActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            DisPlayActivity displayPresenter = weakReference.get();
            if (displayPresenter == null && displayPresenter.isFinishing()) return;
            switch (msg.what) {
                case Config.HandlerGlod.TIME_OUT_READ:
                    if(displayPresenter.isDisplay) {
                        displayPresenter.displayTimeout();
                    }
                    break;
                case REMOVE_SPLASH_IMAGE:
                    displayPresenter.ivSplash.setVisibility(View.GONE);
                    break;
                case Config.HandlerGlod.RECONNECT_SOCKET:
                    if (displayPresenter.secondIp != null && displayPresenter.secondIp.length() > 5){
                        if (displayPresenter.secondIp.contains(":")){
                            displayPresenter.sp.edit().putBoolean(SPUtils.SP_PRIORITY_IPV6,true).commit();
                        }else{
                            displayPresenter.sp.edit().putBoolean(SPUtils.SP_PRIORITY_IPV6,false).commit();
                        }
                        displayPresenter.displayPresenter.reConnect(displayPresenter.secondIp);
                    }

                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtils.e("tlh", "onCreate");
        mActivity = this;
//        HideSystemUIUtils.hideSystemUI(this);

        displayPresenter = getPresenter();
        fragmentManager = getFragmentManager();
        ivSplash = findViewById(R.id.iv_splash_display);
        //getscreenDensity();
        sp = getSharedPreferences(SPUtils.SP_NAME, MODE_PRIVATE);
    }

    private void getscreenDensity(){
        LogUtils.i("hdb-----"+getWindowManager().getDefaultDisplay().getHeight()+"  :"+getWindowManager().getDefaultDisplay().getWidth());
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display defaultDisplay = wm.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getRealMetrics(displayMetrics);

        int widthPixels = displayMetrics.widthPixels;
        int heightPixels = displayMetrics.heightPixels;
        /*DisplayMetrics dm = getResources().getDisplayMetrics();
        LogUtils.e("tlh", "onCreate：dm---:" + dm.toString());
        int widthPixels = dm.widthPixels;
        int heightPixels = dm.heightPixels;*/
        LogUtils.e("tlh", "onCreate：dm---:" +widthPixels+" heightPixels"+heightPixels);

        //出现获取的手机分辨率为反的情况，导致向车机传输坐标不准确的情况
        int temp;
        if (widthPixels < heightPixels) {
            {
                temp = widthPixels;
                widthPixels = heightPixels;
                heightPixels = temp;

            }
        }
//        densityX = 1024f / (float) widthPixels;
//        densityY = 600f / (float) heightPixels;
        densityX = rk3288X / (float) widthPixels;
        densityY = rk3288Y / (float) heightPixels;
        LogUtils.i("hdb----densityX:"+densityX+"  densityY:"+densityY);
    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        LogUtils.e("tlh", "onRestart");
    }

    @Override
    protected void onStart() {
        remoteServiceIP = getIntent().getExtras().getString(Config.ActionKey.CLIENT_IP_KEY);
        secondIp = getIntent().getExtras().getString(Config.ActionKey.SECOND_IP);
//        String showKey = getIntent().getExtras().getString(Config.ActionKey.SHOW_SPLASH_KEY);
//        if(Config.ActionKey.SHOW_SPLASH_HIDE.equalsIgnoreCase(showKey)){
//            ivSplash.setVisibility(View.GONE);
//        }else{
//            ivSplash.setVisibility(View.VISIBLE);
//            disHandler.sendEmptyMessageDelayed(REMOVE_SPLASH_IMAGE,REMOVE_SPLASH_IMAGE_DELAY);
//        }
        super.onStart();
        LogUtils.e("tlh", "onStart");

//        LogUtils.e(TAG, "remoteServiceIP:" + remoteServiceIP+"  showKey:"+showKey);

    }

    @Override
    protected void onResume() {
        super.onResume();
        HideSystemUIUtils.hideSystemUI(this);
        isDisplay = true;
        Config.isFullScreen = true;
        hasReadData = false;
        if (displayPresenter != null) {
            if (secondIp != null && secondIp.length() > 5){
                disHandler.removeMessages(Config.HandlerGlod.RECONNECT_SOCKET);
                disHandler.sendEmptyMessageDelayed(Config.HandlerGlod.RECONNECT_SOCKET,Config.HandlerGlod.RECONNECT_SOCKET_DELAY);
            }
            displayPresenter.startDisPlayRomoteDesk(remoteServiceIP);
//            displayPresenter.startChekcoutHotSpotChange();
        }
        disHandler.removeMessages(Config.HandlerGlod.TIME_OUT_READ);
        disHandler.sendEmptyMessageDelayed(Config.HandlerGlod.TIME_OUT_READ , 6000);
        AutoConnectService instance = AutoConnectService.getInstance();
        if (instance != null){
            instance.showNotification(remoteServiceIP,true);
            instance.setSpCureentIp(remoteServiceIP);
            //instance.setIsLoopSendBraodCast(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isDisplay = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtils.e("tlh", "onStop");
        if (displayPresenter != null) {
            displayPresenter.releseDisplayServer();
            displayPresenter.removeChekcoutHotSpotChange();
            displayPresenter = null;
        }
        AutoConnectService instance = AutoConnectService.getInstance();
        if (instance != null){
            instance.showNotification(remoteServiceIP,false);
            instance.removeDevices();
        //    instance.setIsLoopSendBraodCast(true);
        }
        disHandler.removeMessages(Config.HandlerGlod.TIME_OUT_READ);
        disHandler.removeMessages(Config.HandlerGlod.RECONNECT_SOCKET);
//        disHandler.removeMessages(REMOVE_SPLASH_IMAGE);
        System.exit(0);
//        AutoConnectService.getInstance().removeDevices();
    }

    @Override
    protected void onDestroy() {

        LogUtils.i("hdb----onDestroy----displayPresenter:"+displayPresenter);

        super.onDestroy();
        LogUtils.e("tlh", "onDestroy");
    }

    @Override
    protected int getContentViewID() {
        return R.layout.activity_display;
    }

    @Override
    protected void initView() {
        displayRemoteDeviceSurface = findViewById(R.id.dispaly_remote_service_surface);
        displayRemoteDeviceWaitProgress = findViewById(R.id.display_remote_service_wait);
    }

    @Override
    protected DisplayPresenter createPresenter() {
        return new DisplayPresenter(getApplicationContext());
    }


    @Override
    public void loading() {
        LogUtils.e(TAG, "loading");
        if (displayRemoteDeviceWaitProgress.getVisibility() != View.VISIBLE) {
            displayRemoteDeviceWaitProgress.setVisibility(View.VISIBLE);

        }

    }

    private void checkTimeout(){
        if (displayRemoteDeviceWaitProgress.getVisibility() == View.VISIBLE){
            displayTimeout();
        }
    }

    public boolean hasReadData = false;
    public void hasReadData(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isDisplay) return;
                if (disHandler.hasMessages(Config.HandlerGlod.TIME_OUT_READ)){
                    disHandler.removeMessages(Config.HandlerGlod.TIME_OUT_READ);
                }
                if (disHandler.hasMessages(Config.HandlerGlod.RECONNECT_SOCKET)){
                    disHandler.removeMessages(Config.HandlerGlod.RECONNECT_SOCKET);
                }
                if (displayRemoteDeviceWaitProgress.getVisibility() == View.VISIBLE) {
                    displayRemoteDeviceWaitProgress.setVisibility(View.GONE);
                }
//        LogUtils.e(TAG, "disPlayRemoteDesk---Bitmap:" + bitmap.getByteCount());
                cancelNetworkDialogFragment();
                hasReadData = true;
            }
        });

    }

    @Override
    public void disPlayRemoteDesk(Bitmap bitmap) {
//        if (!isDisplay) return;
//        if (displayRemoteDeviceWaitProgress.getVisibility() == View.VISIBLE) {
//            displayRemoteDeviceWaitProgress.setVisibility(View.GONE);
//        }
////        LogUtils.e(TAG, "disPlayRemoteDesk---Bitmap:" + bitmap.getByteCount());
//        cancelNetworkDialogFragment();
        if (densityX == 0 || densityY == 0){
            rk3288X = bitmap.getWidth();
            rk3288Y = bitmap.getHeight();
            Log.i(TAG,"hdb----rk3288X:"+rk3288X+"  rk3288Y:"+rk3288Y);
            getscreenDensity();
        }
        displayRemoteDeviceSurface.setBitmap(bitmap);
    }

    public void checkReadTimeOut(){
        if (disHandler.hasMessages(Config.HandlerGlod.TIME_OUT_READ)){
            disHandler.removeMessages(Config.HandlerGlod.TIME_OUT_READ);
        }
        if (isDisplay) {
            disHandler.sendEmptyMessageDelayed(Config.HandlerGlod.TIME_OUT_READ, 10000);
        }
    }


    @Override
    public void fila() {
        LogUtils.e(TAG, "fila");
        showNetworkDialogFragment(R.string.display_connect_fail, R.string.display_connect_fail_message);
    }

    @Override
    public void initTouchEventFila() {
        LogUtils.e(TAG, "initTouchEventFila");
        showNetworkDialogFragment(R.string.init_touch_event_fila, R.string.init_touch_event_fila_message);
    }


    @Override
    public void connectSucess() {
        LogUtils.e(TAG, "connectSucess");
        cancelNetworkDialogFragment();
    }

    @Override
    public void displayTimeout() {
        LogUtils.e(TAG, "displayTimeout");
        showNetworkDialogFragment(R.string.display_lost_host, R.string.network_please_check_the_network);
    }

    /**
     * 系统提示对话框
     *
     * @param titleID
     * @param messageID
     */
    private void showNetworkDialogFragment(int titleID, int messageID) {
        LogUtils.e(TAG, "showNetworkDialogFragment--111111111111111");
        if (Config.isFullScreen)
            if (fragmentManager.findFragmentByTag(Config.ErrorDialogKey.DISPALY_DIALOG_FRAGMENT) == null) {
                NetworkDialog dialog = new NetworkDialog();
                dialog.setTitle(titleID);
                dialog.setMessage(messageID);
                dialog.setPositoveButton(R.string.ok);
                dialog.setCancelable(false);
                dialog.setNetworkDialogInterface(new NetworkDialog.NetworkDialogInterface() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        DisPlayActivity.this.finish();
                    }
                });
                dialog.show(fragmentManager, Config.ErrorDialogKey.DISPALY_DIALOG_FRAGMENT);
            }
    }

    /**
     * 当服务器有数据时，确保错误对话框不显示
     */
    private void cancelNetworkDialogFragment() {
        if (Config.isFullScreen) {
            Fragment fragment = fragmentManager.findFragmentByTag(Config.ErrorDialogKey.DISPALY_DIALOG_FRAGMENT);
            if (fragment != null) {
                LogUtils.e(TAG, "cancelNetworkDialogFragment");
                fragmentManager.beginTransaction().remove(fragment).commit();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Config.isFullScreen = false;
        LogUtils.e(TAG, "onSaveInstanceState");
    }
    private int cureentTx = 0;
    private int cureentTy = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        changeX = (int) (x * densityX);
        changeY = (int) (y * densityY);
        LogUtils.i(TAG, "hdb----data:" + event.getAction()+"  x:"+changeX+"  y:"+changeY);
        if (displayPresenter == null)
            return super.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_MOVE){
            if ((cureentTx == changeX) && (cureentTy == changeY)){
                return super.onTouchEvent(event);
            }
        }
        cureentTx = changeX;
        cureentTy = changeY;
        displayPresenter.sendTouchData(event.getAction(), changeX, changeY);


        return super.onTouchEvent(event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


}
