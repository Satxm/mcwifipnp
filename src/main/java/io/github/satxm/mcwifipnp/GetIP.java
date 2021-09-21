package io.github.satxm.mcwifipnp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;

public class GetIP {
    public static String GetGlobalIPv4() {
        String ipv4 = null;
        try {
            URL url = new URL("https://api-ipv4.ip.sb/ip");
            URLConnection URLconnection = url.openConnection();
            InputStreamReader isr = new InputStreamReader(URLconnection.getInputStream());
            BufferedReader bufr = new BufferedReader(isr);
            String str;
            while ((str = bufr.readLine()) != null) {
                ipv4 = str;
            }
            bufr.close();
        } catch (Exception e) {
        }
        return ipv4;
    }

    public static String GetGlobalIPv6() {
        String ipv6 = null;
        try {
            URL url = new URL("https://api-ipv6.ip.sb/ip");
            URLConnection URLconnection = url.openConnection();
            InputStreamReader isr = new InputStreamReader(URLconnection.getInputStream());
            BufferedReader bufr = new BufferedReader(isr);
            String str;
            while ((str = bufr.readLine()) != null) {
                ipv6 = str;
            }
            bufr.close();
        } catch (Exception e) {
        }
        return ipv6;
    }

    public static String GetLocalIPv4() {
        String ip = null;
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                try {
                    NetworkInterface iface = ifaces.nextElement();
                    if (!iface.isUp() || iface.isLoopback() || iface.isVirtual() || iface.isPointToPoint()) {
                        continue;
                    }
                    Enumeration<InetAddress> addrs = iface.getInetAddresses();
                    if (addrs == null) {
                        continue;
                    }
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        if (addr instanceof Inet4Address) {
                            if (addr.getHostAddress().indexOf('%') == -1) {
                                ip = addr.getHostAddress();
                            }
                        } else {
                        }
                    }
                } catch (Throwable t) {
                }
            }
        } catch (Throwable t) {
        }
        return ip;
    }

    public static String GetLocalIPv6() {
        String ip = null;
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                try {
                    NetworkInterface iface = ifaces.nextElement();
                    if (!iface.isUp() || iface.isLoopback() || iface.isVirtual() || iface.isPointToPoint()) {
                        continue;
                    }
                    Enumeration<InetAddress> addrs = iface.getInetAddresses();
                    if (addrs == null) {
                        continue;
                    }
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        if (addr instanceof Inet6Address) {
                            if (addr.getHostAddress().indexOf('%') == -1) {
                                ip = addr.getHostAddress();
                            }
                        } else {
                        }
                    }
                } catch (Throwable t) {
                }
            }
        } catch (Throwable t) {
        }
        return ip;
    }
}
