package com.daniel0x7cc.chatify.utils;


import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.daniel0x7cc.chatify.App;
import com.daniel0x7cc.chatify.R;

public class LogUtils  {

    private static final String TAG_LOG = App.getStr(R.string.app_name);
    private static final int MAX_LENGTH = 1000;
    private static final boolean LOG = (0 != (App.getInstance().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));

    public static void i(final String string) {
        if (LOG) Log.i(TAG_LOG, string);
    }

    public static void d(final String string) {
        if (LOG) Log.d(TAG_LOG, string);
    }

    public static void v(final String string) {
        if (LOG) Log.v(TAG_LOG, string);
    }

    public static void w(final String string) {
        if (LOG) Log.w(TAG_LOG, string);
    }

    public static void e(final String string) {
        Log.e(TAG_LOG, string);
    }

    public static void e(final String string, final Throwable e) {
        if (LOG) Log.e(TAG_LOG, string, e);
    }

    public static void longInfo(String str) {
        if (LOG) {
            if (str.length() > MAX_LENGTH) {
                Log.i(TAG_LOG, str.substring(0, MAX_LENGTH));
                longInfo(str.substring(MAX_LENGTH));
            } else {
                Log.i(TAG_LOG, str);
            }
        }
    }

}
