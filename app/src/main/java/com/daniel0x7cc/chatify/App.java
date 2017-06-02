package com.daniel0x7cc.chatify;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.util.Log;

import com.daniel0x7cc.chatify.helpers.FirebaseHelper;
import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.daniel0x7cc.chatify.helpers.PreferenceManager;
import com.daniel0x7cc.chatify.helpers.SendbirdHelper;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class App extends Application {

    private static App instance;

    public App() {
        super();
        instance = this;
    }

    public synchronized static App getInstance() {
        if (instance == null) {
            new App();
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SendbirdHelper.init(this);
        PreferenceManager.initializePreferenceManager(getBaseContext());
        FacebookSdk.sdkInitialize(getApplicationContext());
        FirebaseHelper.getInstance().init();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        getHashKey();
    }

    public static String getStr(int resId) {
        return getInstance().getString(resId);
    }


    public static Context getContext(){
        return getInstance().getApplicationContext();
    }

    public Uri requestCaptureImageFromCamera(Activity activity, int requestCode,
                                             String imageFileName) {
        Uri photoUri = null;
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getInstance().getPackageManager()) != null) {
            File photoFile = null;
            try {
                File storageDir = getInstance().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);
            } catch (IOException e) {
                LogUtils.e("Erro ao criar arquivo.", e);
            }
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider", photoFile);
                List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                activity.startActivityForResult(intent, requestCode);
            }
        }
        return photoUri;
    }

    private void getHashKey() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.e("MY_KEY_HASH:",
                        Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
        } catch (NoSuchAlgorithmException e) {
        } }

    public void logout(){
        LoginManager.getInstance().logOut();
        FirebaseAuth.getInstance().signOut();
        SendbirdHelper.logout();
        PreferenceManager.getInstance().clean();
    }
}
