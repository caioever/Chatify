package com.daniel0x7cc.chatify;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daniel0x7cc.chatify.helpers.FirebaseHelper;
import com.daniel0x7cc.chatify.helpers.PreferenceManager;
import com.daniel0x7cc.chatify.interfaces.OnLoggedInListener;
import com.daniel0x7cc.chatify.models.User;
import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.GlobalUtils;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends BaseActivity implements OnLoggedInListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText etEmail = (EditText) findViewById(R.id.actLogin_etEmail);
        final EditText etPassword = (EditText) findViewById(R.id.actLogin_etPassword);
        Button btLogin = (Button) findViewById(R.id.actLogin_btLogin);
        final LinearLayout viewForgotPassword = (LinearLayout) findViewById(R.id.actMain_viewForgotPassword);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Entrar");
        }

        viewForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = etEmail.getText().toString();
                String password = etPassword.getText().toString();

                if (!(email.isEmpty() && password.isEmpty())) {
                    if (GlobalUtils.isValidEmail(email)) {
                        FirebaseHelper.getInstance().login(LoginActivity.this, LoginActivity.this, email, password);
                    } else {
                        showToast(getString(R.string.invalid_email_format));
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoggedInSuccessfully() {
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference();

        LogUtils.e(FirebaseHelper.getInstance().getUserId());

        DatabaseReference mostafa = ref.child(Consts.USER_ACCOUNT_PATH).child(FirebaseHelper.getInstance().getUserId());
        mostafa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                //showToast((String.format(getString(R.string.str_welcome), user.getUserName())), 5000);

                PreferenceManager.getInstance().setUserId(user.getUserId());
                PreferenceManager.getInstance().setUserEmail(user.getEmail());
                PreferenceManager.getInstance().setUsername(user.getUserName());

                Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

//        ref.child(Consts.USER_ACCOUNT_PATH + FirebaseHelper.getInstance().getUserId()).equalTo(FirebaseHelper.getInstance().getUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                User user = dataSnapshot.getChildren().iterator().next().getValue(User.class);
//                PreferenceManager.getInstance().setUserId(user.getUserId());
//                PreferenceManager.getInstance().setUsername(user.getUserName());
//
//
//
//                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
//                startActivity(intent);
//                finish();
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//                System.out.println("The read failed: " + databaseError.getCode());
//            }
//        });

    }

}
