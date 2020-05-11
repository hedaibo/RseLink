package com.nsv.rselink.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.util.Log;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class IpUtils {

    public static final int IP_TYPE_NORMAL = 0;
    public static final int IP_TYPE_IPV4 = 1;
    public static final int IP_TYPE_IPV6 = 2;
    public static final int IP_TYPE_IPV46 = 3;

    public static String getIpAddress(Context context) {
        ConnectivityManager conMann = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        String ip = null;

        NetworkInfo mobileNetworkInfo = conMann
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifiNetworkInfo = conMann
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mobileNetworkInfo != null && mobileNetworkInfo.isConnected()) {
            ip = getLocalIpAddress();
        } else if (wifiNetworkInfo != null && wifiNetworkInfo.isConnected()) {

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            ip = intToIp(ipAddress);
        }

        return ip;
    }

    public static String getHostIP() {

        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            LogUtils.i("yao", "SocketException");
            e.printStackTrace();
        }
        return hostIp;

    }

    public static ArrayList<String> getHostListIP() {

        ArrayList<String> ips = new ArrayList<>();
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        String hostIp = ia.getHostAddress();
                        ips.add(hostIp);
                    }
                }
            }
        } catch (SocketException e) {
            LogUtils.i("yao", "SocketException");
            e.printStackTrace();
        }
        return ips;

    }

    public static String getLocalIpAddress() {
        try {
            String ipv4;
            ArrayList<NetworkInterface> nilist = Collections
                    .list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : nilist) {
                ArrayList<InetAddress> ialist = Collections.list(ni
                        .getInetAddresses());
                for (InetAddress address : ialist) {
                    if ((!address.isLoopbackAddress())
                            && (address instanceof Inet4Address)) {
                        return address.getHostAddress();
                    }
                }

            }

        } catch (SocketException ex) {
            LogUtils.e("localip", ex.toString());
        }
        return null;
    }

    public static String intToIp(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    public static InetAddress getBroadcastAddress(Context context)
            throws UnknownHostException {
        WifiManager wifi = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        if (dhcp == null) {
            return InetAddress.getByName("255.255.255.255");
        }
        String hostIP = getHostIP();
        if (hostIP != null && !hostIP.equals("")) {
            String substring = hostIP.substring(0, hostIP.lastIndexOf(".") + 1);
            LogUtils.i("123", "hdb------substring:" + substring);
            return InetAddress.getByName(substring + "255");
        }
        return null;
    }
    public static InetAddress getLocalIp() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    // logger.error("ip1       " + inetAddress);
                    String hostAddress = inetAddress.getHostAddress();
                    //LogUtils.i("hdb2  " + hostAddress);
                    if (inetAddress instanceof Inet6Address) {//ipv6
                        if (!hostAddress.startsWith("fe") && !hostAddress.startsWith("fc") && hostAddress.length() > 6) {
//                            LogUtils.i("hdb2---ipv6:" + hostAddress);
                            return inetAddress;
                        }
                    }else {//ipv4
                        if (!"127.0.0.1".equalsIgnoreCase(hostAddress)) {
//                            LogUtils.i("hdb2---ipv4:" + hostAddress);
                            return inetAddress;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
        }
        return null;
    }

    public static int getLocalIpType() {
        int ipType = IP_TYPE_NORMAL;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    // logger.error("ip1       " + inetAddress);
                    String hostAddress = inetAddress.getHostAddress();
                    Log.i("","hdb2  " + hostAddress);
                    if (inetAddress instanceof Inet6Address) {//ipv6
                        if (!hostAddress.startsWith("fe") && !hostAddress.startsWith("fc") && hostAddress.length() > 6) {
                            LogUtils.i("hdb2---ipv6:" + hostAddress);
                            ipType |= IP_TYPE_IPV6;
                        }
                    }else {//ipv4
                        if (!"127.0.0.1".equalsIgnoreCase(hostAddress)) {
                            LogUtils.i("hdb2---ipv4:" + hostAddress);
                            ipType |= IP_TYPE_IPV4;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
        }
        Log.i("","hdb2  " + ipType);
        return ipType;
    }

    public static NetworkInterface getIpv6NetworkInterface() {

//        String hostIp = null;
//        try {
//            Enumeration nis = NetworkInterface.getNetworkInterfaces();
//            InetAddress ia = null;
//            while (nis.hasMoreElements()) {
//                NetworkInterface ni = (NetworkInterface) nis.nextElement();
//                Enumeration<InetAddress> ias = ni.getInetAddresses();
//                while (ias.hasMoreElements()) {
//                    ia = ias.nextElement();
////                    if (ia instanceof Inet6Address) {
//                        hostIp = ia.getHostAddress();
//                        LogUtils.i("hdb--------ni:"+ni+"  hostIp:"+hostIp);
////                        if (hostIp.contains("softap0") || hostIp.contains("wlan0")) {
////                            return ni;
////                        }
////                        if (!hostIp.startsWith("fe") && !hostIp.startsWith("fc") && hostIp.length() > 6 && hostIp.contains(":")) {
////                            LogUtils.i("hdb2---ipv6:" + hostIp);
////                            return ni;
////                        }
//                        break;
////                    }
//                }
//            }
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }
//        return null;

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    // logger.error("ip1       " + inetAddress);
                    String hostAddress = inetAddress.getHostAddress();
                    LogUtils.i("hdb2  " + hostAddress);
                    if (inetAddress instanceof Inet6Address) {//ipv6
                        if (!hostAddress.startsWith("fe") && !hostAddress.startsWith("fc") && hostAddress.length() > 6) {
                            LogUtils.i("hdb2---ipv6:" + hostAddress);
                            return intf;
                        }
                    }else {//ipv4
//                        if (!"127.0.0.1".equalsIgnoreCase(hostAddress)) {
////                            LogUtils.i("hdb2---ipv4:" + hostAddress);
//                            return intf;
//                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
        }
        return null;

    }

    public static InetAddress getBroadcastAddress()
            throws IOException {
        // 获取本地所有网络接口
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
               continue;
           }
            // getInterfaceAddresses()方法返回绑定到该网络接口的所有 IP 的集合
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {

                InetAddress broadcast = interfaceAddress.getBroadcast();
                if (broadcast == null) {
                    continue;
                }
                return broadcast;
            }
        }
        return null;
    }

    public static List<InetAddress> getListBroadcastAddress()throws IOException {
        // 获取本地所有网络接口
        List<InetAddress> addList = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }
            // getInterfaceAddresses()方法返回绑定到该网络接口的所有 IP 的集合
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {

                InetAddress broadcast = interfaceAddress.getBroadcast();
                if (broadcast == null) {
                    continue;
                }
                addList.add(broadcast);
            }
        }
        return addList;
    }


    public static MulticastLock openWifiBrocast(Context context) {
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        MulticastLock multicastLock = wifiManager
                .createMulticastLock("MediaRender");
        if (multicastLock != null) {
            multicastLock.acquire();
        }
        return multicastLock;
    }

    public static String getMacAddressFromIp() {
        String mac_s= "";
        StringBuilder buf = new StringBuilder();
        try {
            byte[] mac;
            NetworkInterface ne= NetworkInterface.getByInetAddress(InetAddress.getByName(getHostIP()));
            mac = ne.getHardwareAddress();
            for (byte b : mac) {
                buf.append(String.format("%02X:", b));
            }
            if (buf.length() > 0) {
                buf.deleteCharAt(buf.length() - 1);
            }
            mac_s = buf.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        LogUtils.i("hdb-----mac_s:"+mac_s);
        return mac_s;
    }
}
