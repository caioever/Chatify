package com.daniel0x7cc.chatify.models;


public class User {
    private String userId;
    private String userName;
    private String email;
    private double longitude;
    private double latitude;
    private String avatarUrl;
    private boolean shown;

    public User(){}

    public User(String id, String name, String email, String avatarUrl, boolean isShown){
        this.userId = id;
        this.userName = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.shown = isShown;
    };

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public void setAvatarUrl(String avatarUrl){
        this.avatarUrl = avatarUrl;
    }

    public String getAvatarUrl(){
        return avatarUrl;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setIsShown(boolean isShown){
        this.shown = isShown;
    }

    public boolean isShown(){
        return this.shown;
    }
}
