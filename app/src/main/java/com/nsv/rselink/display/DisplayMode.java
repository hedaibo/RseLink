package com.nsv.rselink.display;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import com.nsv.rselink.Config;
import com.nsv.rselink.base.BaseMode;
import com.nsv.rselink.utils.ByteUtils;
import com.nsv.rselink.utils.LogUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Socket;

/**
 * Created by Tianluhua on 2018/3/13.
 */

public class DisplayMode implements BaseMode {

    public static final String TAG = "DisplayMode";

    private Socket dataSocket;
    private boolean isRun = true;


    private Bitmap bm;
    private Handler mHandler = new DisPlayHander(this);
    private DataInputStream dis;
    private DataOutputStream dos;
    /**
     * 开启远程服务端,通过远程服务器的
     *
     * @param serverIp
     */
    public void startServer(final String serverIp) {
        if (callBack != null) {
            callBack.loading();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    LogUtils.i(TAG, "hdb---data--连接start");
                    if (dataSocket != null) {
                        LogUtils.i(TAG, "hdb---data--dataSocket != null");
                        return;
                    }
                    dataSocket = new Socket(serverIp, Config.PortGlob.DATAPORT);// 10.0.0.24
                    dataSocket.setKeepAlive(true);
                    dis = new DataInputStream(
                            dataSocket.getInputStream());
                    dos = new DataOutputStream(dataSocket.getOutputStream());
                    LogUtils.i(TAG, "hdb---data--连接成功");
                    mHandler.sendEmptyMessage(Config.HandlerGlod.CONNET_SUCCESS);
                    isRun = true;
                    while (isRun) {
                        readFile(dis);
                    }
                    mHandler.sendEmptyMessage(Config.HandlerGlod.TOUCH_EVENT_CONNECT_FAIL);
                } catch (Exception ex) {
                    LogUtils.e(TAG, "hdb--dataServer-ex:" + ex.toString());
                    mHandler.sendEmptyMessage(Config.HandlerGlod.TOUCH_EVENT_CONNECT_FAIL);
                }
            }
        }).start();

    }


    public void releseDateServer(){
        LogUtils.i("hdb----onDestroy----releseDateServer:");
        isRun = false;
        if (dis != null){
            try {
                dis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (dataSocket != null){
            try {
                dataSocket.shutdownInput();
                dataSocket.shutdownOutput();
                dataSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (dos != null){
            try {
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dos = null;
        dis = null;
        dataSocket = null;
    }

    /**
     * 读取远程通过TCP 传输过来的数据
     *
     * @param dis
     * @throws IOException
     */
    private synchronized void readFile(DataInputStream dis) throws IOException {
//        LogUtils.i(TAG, "hdb---readFile");
        long time1 = SystemClock.uptimeMillis();
        byte[] receveByteslen = new byte[4];
        byte[] datalen = new byte[3];
 //       dis.readFully(datalen);
        dis.readFully(receveByteslen);
        long time2 = SystemClock.uptimeMillis();
        LogUtils.i("hdb-----time1:"+(time2 - time1));
        sendAck(receveByteslen[0]);
        LogUtils.i("hdb-----time2:"+(SystemClock.uptimeMillis() - time2));
        LogUtils.i(TAG, "hdb---readFile--ack:" + (int)(receveByteslen[0] & 0xFF));
        System.arraycopy(receveByteslen, 1, datalen, 0, datalen.length);
        LogUtils.i("hdb---0:"+receveByteslen[0]+"  1:"+receveByteslen[1]+"  2:"+receveByteslen[2]+"  3:"+receveByteslen[3]);
        LogUtils.i("hdb-da--0:"+datalen[0]+"  1:"+datalen[1]+"  2:"+datalen[2]);
        int length = ByteUtils.bufferToInt(datalen);
        LogUtils.i(TAG, "hdb---readFile--length:" + length);
//        DisPlayActivity instance = DisPlayActivity.getInstance();
//        if (null != instance){
//            instance.checkReadTimeOut();
//        }
        DisPlayActivity instance = DisPlayActivity.getInstance();
        if (instance != null && !instance.hasReadData) {
            instance.hasReadData();
        }

        if(length != 0){
            byte[] receveBytes = new byte[length];
            dis.readFully(receveBytes);
            bm = BitmapFactory.decodeByteArray(receveBytes, 0, length);
            LogUtils.i(TAG, "hdb----接收文件<>成功-------bm:" + bm);
            if (bm != null) {
//            LogUtils.i(TAG, "hdb-------bm:" + bm.getByteCount());
                mHandler.sendEmptyMessage(Config.HandlerGlod.SHOW_IMAGEVIEW);
            }
            LogUtils.i("hdb-----time3:"+(SystemClock.uptimeMillis() - time1));
            receveBytes = null;
        }
//        receveByteslen = null;
        datalen = null;
    }

    private void sendAck(byte ack) throws IOException {
        if (null != dos){
            dos.write(ack);
            dos.flush();
        }
    }

    private CallBack callBack;

    public DisplayMode(CallBack callBack) {
        this.callBack = callBack;
    }

    public interface CallBack {

        public void loading();

        public void disPlayRemoteDesk(Bitmap bitmap);

        public void fila();

        public void connectSucess();

    }

    public void onDestroy() {
        dataSocket = null;
    }

    public static class DisPlayHander extends Handler {

        WeakReference<DisplayMode> weakReference;

        public DisPlayHander(DisplayMode mDisplayMode) {
            weakReference = new WeakReference<>(mDisplayMode);

        }

        @Override
        public void handleMessage(Message msg) {
            DisplayMode mDisplayMode = weakReference.get();
            if (mDisplayMode == null)
                return;

            switch (msg.what) {

                case Config.HandlerGlod.TOUCH_EVENT_CONNECT_FAIL:
                    if (mDisplayMode.callBack != null) {
                        mDisplayMode.callBack.fila();
                    }
                    break;

                case Config.HandlerGlod.CONNET_SUCCESS:
                    if (mDisplayMode.callBack != null) {
                        mDisplayMode.callBack.connectSucess();
                    }
//                    mDisplayMode.checkAlive();
                    break;
                case Config.HandlerGlod.SHOW_IMAGEVIEW:
                    if (mDisplayMode.callBack != null && mDisplayMode.bm != null) {
                        mDisplayMode.callBack.disPlayRemoteDesk(mDisplayMode.bm);
                    }
                    break;

                default:
                    break;
            }
        }
    }

//    private void checkAlive(){
//        ThreadPoolManager.getInstance().execute(new Runnable() {
//            @Override
//            public void run() {
//                isRun = true;
//                try {
//                    while (isRun) {
//                        LogUtils.i("hdb---------checkAlive--");
//                       dataSocket.sendUrgentData(0xFF);
//                        Thread.sleep(2000);
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    LogUtils.i("hdb---------checkAlive---error");
//                    isRun = false;
//                    mHandler.sendEmptyMessage(Config.HandlerGlod.TOUCH_EVENT_CONNECT_FAIL);
//                }
//
//            }
//        });
//    }

}
