package com.daniel0x7cc.chatify.helpers;

import android.support.annotation.NonNull;
import android.widget.Toast;

import com.daniel0x7cc.chatify.App;
import com.daniel0x7cc.chatify.BaseActivity;
import com.daniel0x7cc.chatify.R;
import com.daniel0x7cc.chatify.interfaces.OnLoggedInListener;
import com.daniel0x7cc.chatify.models.User;
import com.daniel0x7cc.chatify.utils.Consts;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {

    public static FirebaseHelper instance;
    public FirebaseAuth firebaseAuth;

    public FirebaseHelper(){
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    public void init(){
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public FirebaseAuth getFirebaseAuth(){
        return firebaseAuth;
    }

    public String getUserId(){
        FirebaseUser firebaseUser = getInstance().getFirebaseAuth().getCurrentUser();
        return firebaseUser.getUid();
    }

    public void login(final OnLoggedInListener onLoggedInListener, final BaseActivity activity, String email, String password){
        activity.showLoading(App.getStr(R.string.logging_in));
        getFirebaseAuth().signInWithEmailAndPassword(email, password).addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                activity.dismissLoading();
                if (!task.isSuccessful()) {
                    Toast.makeText(activity, App.getStr(R.string.login_mismatch), Toast.LENGTH_LONG).show();
                } else {
                    onLoggedInListener.onLoggedInSuccessfully();
                }
            }
        });
    }

    public void loginUserWithFacebook(final OnLoggedInListener onLoggedInListener, String userId, String name, String email, String avatarUrl){
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference reference = firebaseDatabase.getReference().child(Consts.USER_ACCOUNT_PATH + FirebaseHelper.getInstance().getUserId());
        User user = new User(userId, name, email, avatarUrl, true);
        reference.setValue(user);
        onLoggedInListener.onLoggedInSuccessfully();
    }

}
