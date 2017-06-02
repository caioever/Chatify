package com.daniel0x7cc.chatify.helpers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import com.daniel0x7cc.chatify.App;
import com.daniel0x7cc.chatify.ChatActivity;
import com.daniel0x7cc.chatify.R;
import com.daniel0x7cc.chatify.customviews.CircleTransform;
import com.daniel0x7cc.chatify.services.DirectReplyService;
import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.sendbird.android.shadow.com.google.gson.Gson;
import com.sendbird.android.shadow.com.google.gson.JsonElement;
import com.squareup.picasso.Picasso;

import java.io.Serializable;

public class SendbirdNotificationHelper {

    private static final long[] PATTERN = {500, 500, 500, 500, 500};
    private static boolean isAudioMessage = false;

    public static void sendNotification(Context context, String messageBody, JsonElement payload) {
        final PayloadParser payloadParser = new PayloadParser(payload);

        String senderId = payloadParser.getNotification().getSender().id;

        if (payloadParser.getNotification().getType().equals("FILE") && payloadParser.getNotification().getMessage().endsWith(".3gp")){
            messageBody = payloadParser.getNotification().getSender().name + ":" + " " + App.getStr(R.string.str_audio_message) + " (" +
            payloadParser.getNotification().getData() + ")";
            isAudioMessage = true;
        }

        sendNotification(context, payloadParser.getNotification().getChannel().channel_url,
                senderId,
                payloadParser.getNotification().getSender().name,
                messageBody);
    }

    public static void sendNotification(Context context, String channelUrl, String senderId,
                                        String senderName, String message) {
        Intent resultIntent = new Intent(context, ChatActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        resultIntent.putExtra(Consts.KEY_CHAT_OPENED_BY_NOTIFICATION, true);
        resultIntent.putExtra(Consts.KEY_CHAT_CHANNEL_URL, channelUrl);
        resultIntent.putExtra(Consts.KEY_CHAT_OPONNENT_NAME, senderName);
        resultIntent.putExtra(Consts.KEY_CHAT_OPONNENT_ID, senderId);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(ChatActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent;
        NotificationCompat.Action action;
        NotificationCompat.Builder notificationBuilder;
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Bitmap opponentBitmap;

        // TODO : REFACTOR PICASSO.LOAD() THREAD
        try {
            opponentBitmap = Picasso.with(context).load(PreferenceManager.getInstance().getOpponentAvatar(senderId))
                    .transform(new CircleTransform()).get();
        } catch (Exception ex) {
            LogUtils.e("Error Picasso: " + ex.getMessage());
            opponentBitmap = null;
        }

        resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_location_on)
                .setColor(ContextCompat.getColor(context, R.color.main_color))
                .setContentTitle(context.getString(R.string.app_name))
                .setPriority(Notification.PRIORITY_HIGH)
                .setVibrate(PATTERN)
                .setTicker(message)
                .setContentText(message)
                .setLights(Color.GREEN, 1, 1)
                .setAutoCancel(true)
                .setLargeIcon(opponentBitmap)
                .setSound(defaultSoundUri)
                .setContentIntent(resultPendingIntent);


//        Picasso.with(context).load(PrefsManager.getInstance().getOpponentAvatar(String.valueOf(senderId))).into(new Target() {
//            @Override
//            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
//                opponentBitmap = bitmap;
//                LogUtils.e("The image was obtained correctly");
//            }
//
//            @Override
//            public void onBitmapFailed(Drawable errorDrawable) {
//                LogUtils.e("The image was not obtained");
//            }
//
//            @Override
//            public void onPrepareLoad(Drawable placeHolderDrawable) {
//                LogUtils.e("Getting ready to get the image");
//                //Here you should place a loading gif in the ImageView to
//                //while image is being obtained.
//            }
//        });

        // only handle one pending intent at a time
        int flags = PendingIntent.FLAG_CANCEL_CURRENT;

        // TODO: REFATORAR ACTION DAS VERSÕES ABAIXO DE 7.0
        if (Build.VERSION.SDK_INT >= 24) {
            // Setup a DirectReply IntentService
            Intent directReplyIntent = new Intent(App.getContext(), DirectReplyService.class);

            // pass the notification ID -- it should be a UUID
            directReplyIntent.putExtra(Consts.KEY_NOTIFY_ID, 82);
            directReplyIntent.putExtra(Consts.KEY_CHAT_CHANNEL_URL, channelUrl);

            resultPendingIntent = PendingIntent.getService(App.getContext(), 0, directReplyIntent, flags);

            RemoteInput remoteInput = new RemoteInput.Builder(Consts.KEY_TEXT_REPLY).setLabel(App.getStr(R.string.type_hint_message)).build();

            action = new NotificationCompat.Action.Builder(
                    android.R.drawable.ic_dialog_email, String.format(App.getStr(R.string.str_reply_to), senderName), resultPendingIntent)
                    .addRemoteInput(remoteInput).build();

            notificationBuilder.addAction(action);
        }

//        Intent playAudioIntent = new Intent(App.getContext(), PlayAudioReceiver.class);
//        playAudioIntent.putExtra(Consts.KEY_CHAT_OPENED_BY_NOTIFICATION, true);
//        playAudioIntent.putExtra(Consts.KEY_CHAT_CHANNEL_URL, channelUrl);
//        playAudioIntent.putExtra(Consts.KEY_CHAT_OPONNENT_NAME, senderName);
//        playAudioIntent.putExtra(Consts.KEY_CHAT_OPONNENT_ID, senderId);
//        playAudioIntent.setAction(Consts.KEY_AUDIO_MESSAGE);
//        PendingIntent pendingIntentPlay = PendingIntent.getBroadcast(App.getContext(), 0, playAudioIntent, flags);
//
//        if (isAudioMessage) {
//            notificationBuilder.addAction(R.drawable.icon_microfone, App.getStr(R.string.str_play_audio), pendingIntentPlay);
//            isAudioMessage = false;
//        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());
    }

    private static class PayloadParser implements Serializable {
        private PushNotification notification;

        private PayloadParser(JsonElement payload) {
            Gson gson = new Gson();
            this.notification = gson.fromJson(payload.toString(), PushNotification.class);
        }

        private PushNotification getNotification() {
            return notification;
        }
    }

    private class PushNotification implements Serializable {
        private Channel channel;
        private Sender sender;
        private Recipient recipient;
        private String type;
        private String message;
        private String data;

        private Channel getChannel() {
            return channel;
        }

        private Sender getSender() {
            return sender;
        }

        private Recipient getRecipient() {
            return recipient;
        }

        private String getType(){
            return type;
        }

        private String getMessage(){
            return message;
        }

        private String getData(){
            return data;
        }
    }

    private class Channel {
        private String channel_url;
    }

    private class Sender {
        private String id;
        private String name;
    }

    private class Recipient {
        private String id;
        private String name;
    }
}
