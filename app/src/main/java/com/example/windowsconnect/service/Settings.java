package com.example.windowsconnect.service;

import com.example.windowsconnect.models.Device;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Settings {


    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }

    private static Device _device;
    public static Device getDevice(){
        if(_device == null){
            _device = new Device(android.os.Build.MODEL, getIPAddress(true) ,new Date());
        }
        return _device;
    }
    public static final int UDP_SEND_PORT = 5000;
    public static final int UDP_LISTEN_PORT = 5001;
    public static final int TCP_SEND_PORT = 5002;
    public static final int TCP_LISTEN_PORT = 5003;

}
