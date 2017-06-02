package com.daniel0x7cc.chatify.helpers;


import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.sendbird.android.shadow.com.google.gson.Gson;

public class PreferenceManager {

    private static SharedPreferences preferences;
    private static PreferenceManager instance;
    private final String PREFS_CHAT_OPPENED = "chat_oppened";
    private final String PREFS_UNREAD_MESSAGES = "unread_messages_count";

    public static synchronized  PreferenceManager getInstance(){
        if(instance == null){
            instance = new PreferenceManager();
        }
        return  instance;
    }

    public static void initializePreferenceManager(Context context) {
        preferences = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void clean(){
        setUserId(null);
        setUserEmail(null);
        setUsername(null);
        setAvatar(null);
        setLocation(null);
    }

    public void setUserId(String userId) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("userId", userId);
        editor.apply();
    }

    public void setUsername(String userName) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("userName", userName);
        editor.apply();
    }

    public void setUserEmail(String userEmail) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("userEmail", userEmail);
        editor.apply();
    }

    public void setLocation(Location location){
        Gson gson = new Gson();
        String json = gson.toJson(location);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("location", json);
        editor.commit();
    }

    public Location getLocation() {
        Gson gson = new Gson();
        SharedPreferences.Editor editor = preferences.edit();
        String json = preferences.getString("location", "");
        Location location = gson.fromJson(json, Location.class);
        return location;
    }

    public String getOpponentAvatar(String opponentId) {
        return preferences.getString(opponentId, null);
    }

    public void setOpponentAvatar(String userId, String avatar) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(userId, avatar);
        editor.apply();
    }

    public boolean hasChatOppened() {
        return preferences.getBoolean(PREFS_CHAT_OPPENED, false);
    }

    public void setChatOppened(boolean hasChatOppened) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREFS_CHAT_OPPENED, hasChatOppened);
        editor.apply();
    }

    public boolean isLogged() {
        return (getUserId() != null && !getUserId().isEmpty()
                && getUsername() != null && !getUsername().isEmpty());
    }

    public void setUnreadMessagesCount(int count){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(PREFS_UNREAD_MESSAGES, count);
        editor.apply();
    }

    public int getUnreadMessagesCount(){
        return preferences.getInt(PREFS_UNREAD_MESSAGES, 0);
    }

    public String getUserId(){
        return preferences.getString("userId", null);
    }

    public String getUsername() {
        return preferences.getString("userName", null);
    }

    public String getUserEmail() {
        return preferences.getString("userEmail", null);
    }

    public String getAvatar() {
        return preferences.getString(Consts.PREFS_USER_AVATAR, null);
    }

    public void setAvatar(String avatar) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Consts.PREFS_USER_AVATAR, avatar);
        editor.apply();
    }

    public void saveDistance(String userId, int distance){
        LogUtils.e("distancia: " + String.valueOf(distance));
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(userId + Consts.PREFS_DISTANCE, distance);
        editor.apply();
    }

    public int getDistanceFromUser(String userId){
        return  preferences.getInt(userId + Consts.PREFS_DISTANCE, 0);
    }

}
