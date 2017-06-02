package com.daniel0x7cc.chatify.fcm;

import com.daniel0x7cc.chatify.utils.LogUtils;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        LogUtils.i("FirebaseInstanceIdService.onTokenRefresh - token: " + refreshedToken);

        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String token) {
        LogUtils.i("FirebaseInstanceIdService - SendBird.registerPushTokenForCurrentUser token: " + FirebaseInstanceId.getInstance().getToken());
        SendBird.registerPushTokenForCurrentUser(token, new SendBird.RegisterPushTokenWithStatusHandler() {
            @Override
            public void onRegistered(SendBird.PushTokenRegistrationStatus pushTokenRegistrationStatus, SendBirdException e) {
                LogUtils.i("FirebaseInstanceIdService - SendBird.registerPushTokenForCurrentUser.onRegistered");
                if (e != null) {
                    LogUtils.e("SendBird: Erro ao registrar push token do usuário. Código: " + e.getCode(), e);
                }
            }
        });
    }
}
