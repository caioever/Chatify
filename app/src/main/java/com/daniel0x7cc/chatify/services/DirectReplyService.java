package com.daniel0x7cc.chatify.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;

import com.daniel0x7cc.chatify.ChatActivity;
import com.daniel0x7cc.chatify.utils.Consts;

public class DirectReplyService extends IntentService {

    public DirectReplyService() {
        super("direct_reply_service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String channelUrl = intent.getStringExtra(Consts.KEY_CHAT_CHANNEL_URL);
        CharSequence message = getMessageText(intent);
        if (message != null && message.length() > 0) {
            ChatActivity.sendUserMessage(message.toString(), channelUrl);
        }
    }

    private CharSequence getMessageText(Intent intent) {
        // Decode the reply text
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence(Consts.KEY_TEXT_REPLY);
        }
        return null;
    }
}
