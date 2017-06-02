package com.daniel0x7cc.chatify;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.daniel0x7cc.chatify.helpers.SendbirdHelper;
import com.daniel0x7cc.chatify.interfaces.SendBirdEventListener;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.sendbird.android.SendBird;

public class SplashActivity extends AppCompatActivity implements SendBirdEventListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        SendbirdHelper.getInstance().login();
        SendbirdHelper.addConnectListener(this);
    }

    @Override
    public void onConnectSucceeded() {
        LogUtils.e("connected!" + SendBird.getCurrentUser().getUserId());
        Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onConnectFailed() {

    }

    @Override
    public void onAvatarsUrlLoaded() {

    }
}
