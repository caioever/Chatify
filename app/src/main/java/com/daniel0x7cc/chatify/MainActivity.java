package com.daniel0x7cc.chatify;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.daniel0x7cc.chatify.helpers.FirebaseHelper;
import com.daniel0x7cc.chatify.helpers.PreferenceManager;
import com.daniel0x7cc.chatify.interfaces.OnLoggedInListener;
import com.daniel0x7cc.chatify.models.FacebookUser;
import com.daniel0x7cc.chatify.models.User;
import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

public class MainActivity extends BaseActivity implements OnLoggedInListener {

    private CallbackManager callbackManager;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferenceManager.getInstance().isLogged()) {
            Intent intent = new Intent(MainActivity.this, SplashActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Force logout/unregister on APIs
        App.getInstance().logout();

        setContentView(R.layout.activity_main);

        // Facebook login
        callbackManager = CallbackManager.Factory.create();
        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    LogUtils.e("onAuthStateChanged:signed_in:" + user.getUid());
                    LogUtils.e("onAuthStateChanged:signed_in:" + user.getEmail());
                } else {
                    // User is signed out
                    LogUtils.e("onAuthStateChanged:signed_out");
                }
                // ...
            }
        };

        final LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("email", "public_profile", "user_birthday", "user_location", "user_friends");
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                LogUtils.e("facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                LogUtils.e("facebook:onCancel");
                // ...
            }

            @Override
            public void onError(FacebookException error) {
                LogUtils.e("facebook:onError: " + error.getMessage());
                // ...
            }
        });

        final Button btLoginFacebook = (Button) findViewById(R.id.actMain_btLoginFacebook);
        btLoginFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginButton.performClick();
            }
        });

        LinearLayout viewRegister = (LinearLayout) findViewById(R.id.actMain_viewRegister);
        viewRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        final Button btLogin = (Button) findViewById(R.id.actMain_btLogin);
        btLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private void handleFacebookAccessToken(final AccessToken token) {
        LogUtils.e("handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        LogUtils.e("signInWithCredential:onComplete:" + task.isSuccessful());

                        registerUser(token);
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            LogUtils.e("signInWithCredential" + task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }

    private void registerUser(AccessToken token){
        GraphRequest request = GraphRequest.newMeRequest(token,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        LogUtils.i("FacebookLogin - GraphJSONObjectCallback.onCompleted: " + response.toString());
                        try {
                            FacebookUser facebookUser = new FacebookUser(object);
                            LogUtils.i("FacebookLogin user: " + facebookUser.getId() + " - " + facebookUser.getEmail() + "; " + facebookUser.getName());
                            final String email = facebookUser.getEmail();
                            if (email == null || email.isEmpty()) {
                                LogUtils.e("empty or null");
                            } else {
                                LogUtils.e("email: " + facebookUser.getEmail());
                            }

                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                            FirebaseHelper.getInstance().loginUserWithFacebook(MainActivity.this, user.getUid(), facebookUser.getName(), facebookUser.getEmail(), facebookUser.getAvatarUrl());

                            LogUtils.e(user.getUid());
                        } catch (Exception e) {
                            LogUtils.e("Erro ao obter informações de usuário via Facebook.", e);
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email,gender,picture,birthday,location");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onLoggedInSuccessfully() {
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference();

        DatabaseReference mostafa = ref.child(Consts.USER_ACCOUNT_PATH).child(FirebaseHelper.getInstance().getUserId());
        mostafa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                showToast((String.format(getString(R.string.welcome), user.getUserName())));

                PreferenceManager.getInstance().setUserId(user.getUserId());
                PreferenceManager.getInstance().setUserEmail(user.getEmail());
                PreferenceManager.getInstance().setUsername(user.getUserName());
                PreferenceManager.getInstance().setAvatar(user.getAvatarUrl());

                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
