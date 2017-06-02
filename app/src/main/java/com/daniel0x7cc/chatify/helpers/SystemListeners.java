package com.daniel0x7cc.chatify.helpers;

import com.daniel0x7cc.chatify.interfaces.OnUpdateUnreadMessageCount;

import java.util.ArrayList;
import java.util.List;


public class SystemListeners {

    private static final List<OnUpdateUnreadMessageCount> updateUnreadListeners = new ArrayList<>();

    public static void addUpdateUnreadMessageCountListener(final OnUpdateUnreadMessageCount listener) {
        if (listener != null && !updateUnreadListeners.contains(listener)) {
            updateUnreadListeners.add(listener);
        }
    }

    public static void removeUpdateUnreadMessageCountListener(final OnUpdateUnreadMessageCount listener) {
        if (listener != null && updateUnreadListeners.contains(listener)) {
            updateUnreadListeners.remove(listener);
        }
    }

    public static void notifyOnUpdateUnreadMessageCount(final int count) {
        if (!updateUnreadListeners.isEmpty()) {
            for (OnUpdateUnreadMessageCount listener : updateUnreadListeners) {
                listener.onUpdateUnreadMessageCount(count);
            }
        }
    }
}
