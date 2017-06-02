package com.daniel0x7cc.chatify.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.daniel0x7cc.chatify.App;
import com.daniel0x7cc.chatify.helpers.SendbirdHelper;
import com.daniel0x7cc.chatify.helpers.SendbirdNotificationHelper;
import com.sendbird.android.AdminMessage;
import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.FileMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.SendBird;
import com.sendbird.android.UserMessage;

public class MessageNotificationService extends Service {

    private static final String identifier = "ChatMessagesListener";

    @Override
    public void onCreate() {
        super.onCreate();
        SendBird.addChannelHandler(identifier, new SendBird.ChannelHandler() {
            @Override
            public void onMessageReceived(BaseChannel baseChannel, BaseMessage baseMessage) {
                if (baseChannel instanceof GroupChannel) {
                    GroupChannel groupChannel = (GroupChannel) baseChannel;

                    String message = null;
                    if (baseMessage instanceof UserMessage) {
                        message = ((UserMessage) baseMessage).getMessage();

                    } else if (baseMessage instanceof AdminMessage) {
                        message = ((AdminMessage) baseMessage).getMessage();

                    } else if (baseMessage instanceof FileMessage) {
                        message = "Arquivo";
                    }

                    if (message != null && groupChannel.getMembers().size() > 1) {
                        String nickname = SendbirdHelper.getOpponentNickname(groupChannel);
                        message = nickname + ": " + message;
                        SendbirdNotificationHelper.sendNotification(App.getContext(),
                                groupChannel.getUrl(), SendbirdHelper.getOpponentId(groupChannel),
                                nickname, message);
                    }
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        SendBird.removeChannelHandler(identifier);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
