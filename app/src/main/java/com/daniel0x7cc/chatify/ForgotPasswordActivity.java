package com.daniel0x7cc.chatify;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.daniel0x7cc.chatify.utils.LogUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class ForgotPasswordActivity extends BaseActivity {

    private EditText etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        setTitle(getString(R.string.change_password));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etEmail = (EditText) findViewById(R.id.actForgotPassoword_etEmail);
        final Button btSendMail = (Button) findViewById(R.id.actForgotPassword_btChangePassword);

        etEmail.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            sendEmail();
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });

        btSendMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEmail();
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
        switch(item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default :
                return super.onOptionsItemSelected(item);
        }
    }

    private void sendEmail(){
        final String email = etEmail.getText().toString();

        if (!email.isEmpty()) {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(Task<Void> task) {
                            if (task.isSuccessful()) {
                                showToast(String.format(Locale.getDefault(), getString(R.string.email_sent_change_pass), email));
                                LogUtils.e("Email sent successfully!");
                                finish();
                            } else {
                                showToast(getString(R.string.failed_to_send_mail));
                                LogUtils.e("Error onComplete sendEmail: email not valid!");
                            }
                        }
                    });
        } else {
            showToast(getString(R.string.empty_email));
        }
    }

}




