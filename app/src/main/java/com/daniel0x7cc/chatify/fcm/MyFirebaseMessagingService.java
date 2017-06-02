package com.daniel0x7cc.chatify.fcm;

import com.daniel0x7cc.chatify.helpers.PreferenceManager;
import com.daniel0x7cc.chatify.helpers.SendbirdNotificationHelper;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.sendbird.android.shadow.com.google.gson.JsonElement;
import com.sendbird.android.shadow.com.google.gson.JsonParser;

import java.util.Map;

import me.leolin.shortcutbadger.ShortcutBadgeException;
import me.leolin.shortcutbadger.ShortcutBadger;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        LogUtils.d("FirebaseMessagingService.onMessageReceived - From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            LogUtils.d("FirebaseMessagingService.onMessageReceived - Data: " + remoteMessage.getData());
        }

        if (remoteMessage.getNotification() != null) {
            LogUtils.d("FirebaseMessagingService.onMessageReceived - Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        Map<String, String> notificationData = remoteMessage.getData();

        if (notificationData.get("sendbird") != null) {
            PreferenceManager.getInstance().setUnreadMessagesCount(PreferenceManager.getInstance().getUnreadMessagesCount() + 1);
            updateShortcutBadger(PreferenceManager.getInstance().getUnreadMessagesCount());
            JsonElement payload = new JsonParser().parse(notificationData.get("sendbird"));
            SendbirdNotificationHelper.sendNotification(this, remoteMessage.getData().get("message"), payload);
        }
    }


    private void updateShortcutBadger(int countNewUnreadMessages) {
        try {
            if (countNewUnreadMessages > 0) {
                ShortcutBadger.applyCountOrThrow(this, countNewUnreadMessages);
            } else {
                ShortcutBadger.removeCountOrThrow(this);
            }
        } catch (ShortcutBadgeException e) {
            LogUtils.e("Erro ao manipular badge do ícone do aplicativo.", e);
        }
    }
}