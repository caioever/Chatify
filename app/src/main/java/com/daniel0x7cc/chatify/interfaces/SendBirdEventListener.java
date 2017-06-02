package com.daniel0x7cc.chatify.interfaces;

public interface SendBirdEventListener {

    void onConnectSucceeded();

    void onConnectFailed();

    void onAvatarsUrlLoaded();
}
