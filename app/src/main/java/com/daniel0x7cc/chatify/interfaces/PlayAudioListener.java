package com.daniel0x7cc.chatify.interfaces;

import android.media.MediaPlayer;
import com.daniel0x7cc.chatify.adapters.MessagesAdapter;
import java.util.Timer;

public interface PlayAudioListener {
    void onPlayAudio(MessagesAdapter.AudioMessageViewHolder holder, MediaPlayer mediaPlayer, Timer timer, boolean isOpponentMessage);
}
