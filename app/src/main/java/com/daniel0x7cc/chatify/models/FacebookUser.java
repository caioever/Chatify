package com.daniel0x7cc.chatify.models;

import org.json.JSONException;
import org.json.JSONObject;

public class FacebookUser {

    private String id;
    private String email;
    private String name;
    private String gender;
    private String birthday;
    private String location;
    private String avatarUrl;

    public FacebookUser(JSONObject json) throws JSONException {
        id = json.getString("id");
        email = json.getString("email");
        name = json.getString("name");
        try {
            gender = json.getString("gender");
        } catch (JSONException ignored) {}

        try {
            birthday = json.getString("birthday");
        } catch (JSONException ignored) {}

        avatarUrl = "https://graph.facebook.com/" + id + "/picture";

        try {
            JSONObject objLocation = json.getJSONObject("location");
            if (objLocation != null) {
                location = objLocation.getString("name");
            }
        } catch (JSONException ignored) {}
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public String getLocation() {
        return location;
    }

    public String getAvatarUrl(){
        return avatarUrl;
    }

}
