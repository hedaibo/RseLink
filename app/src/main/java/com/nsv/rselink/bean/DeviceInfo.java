package com.nsv.rselink.bean;

public class DeviceInfo {

    private String ipAddress;
    private String Name;
    private long time;
    private String ip6Address;



    public DeviceInfo(String ipAddress, String name) {
        super();
        this.ipAddress = ipAddress;
        Name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getIp6Address() {
        return ip6Address;
    }

    public void setIp6Address(String ip6Address) {
        this.ip6Address = ip6Address;
    }

    public String getValidAddress(){
        if (ip6Address != null && ip6Address.length() > 5) return ip6Address;

        return ipAddress;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "ipAddress='" + ipAddress + '\'' +
                ", Name='" + Name + '\'' +
                ", time=" + time +
                ", ip6Address='" + ip6Address + '\'' +
                '}';
    }
}
