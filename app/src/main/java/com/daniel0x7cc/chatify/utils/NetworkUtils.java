package com.daniel0x7cc.chatify.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;


public class NetworkUtils {

    public static boolean isOnline(final Context context) {
        return isNetworkAvailable(context);
    }

    private static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        } catch (Exception e) {
            LogUtils.e("Erro ao obter endereço IP", e);
        }
        return true;
    }

    public static String getIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && inetAddress.isSiteLocalAddress()) {
                        String ip = inetAddress.getHostAddress();
                        LogUtils.i("***** IP=" + ip);
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e("Erro ao obter endereço IP", e);
        }
        return null;
    }
}
