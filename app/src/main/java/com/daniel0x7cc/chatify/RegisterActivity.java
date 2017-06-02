package com.daniel0x7cc.chatify;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.daniel0x7cc.chatify.helpers.FirebaseHelper;
import com.daniel0x7cc.chatify.models.User;
import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.daniel0x7cc.chatify.helpers.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends BaseActivity {

    private ProgressDialogFragment progressDialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        final EditText etName = (EditText) findViewById(R.id.actRegister_etName);
        final EditText etEmail = (EditText) findViewById(R.id.actRegister_etEmail);
        final EditText etPassword = (EditText) findViewById(R.id.actRegister_etPassword);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Criar conta");
        }


        Button btRegister = (Button) findViewById(R.id.actRegister_btRegister);
        btRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateRegister(etName.getText().toString(), etEmail.getText().toString(), etPassword.getText().toString());
            }
        });
    }

    private void validateRegister(String name, String email, String password){
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getBaseContext(), "Você deve preencher todos os campos.", Toast.LENGTH_SHORT).show();
        } else {
            register(name, email, password);
        }
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
    protected void onResume() {
        super.onResume();
        this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void register(final String name, final String email, String password){
        showProgress("Cadastrando. Por favor, aguarde...");

        FirebaseHelper.getInstance().getFirebaseAuth().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d("tes", "createUserWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                            LogUtils.e(task.getException().getMessage());

                        } else {
                            Toast.makeText(RegisterActivity.this, "Registrado com sucesso!",
                                    Toast.LENGTH_SHORT).show();
                            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
                            DatabaseReference reference = firebaseDatabase.getReference().child(Consts.USER_ACCOUNT_PATH +
                                    FirebaseHelper.getInstance().getUserId());

                            User user = new User(FirebaseHelper.getInstance().getUserId(), name, email, "", true);
                            reference.setValue(user);

                            PreferenceManager.getInstance().setUserId(user.getUserId());
                            PreferenceManager.getInstance().setUserEmail(user.getEmail());
                            PreferenceManager.getInstance().setUsername(user.getUserName());

                            Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        dismissProgress();

                        // ...
                    }
                });
    }

    private void showProgress(String name) {
        progressDialogFragment = ProgressDialogFragment.newInstance(name);
        progressDialogFragment.show(getSupportFragmentManager(), "Register");
    }

    private void dismissProgress() {
        if (progressDialogFragment != null) {
            progressDialogFragment.dismissAllowingStateLoss();
            progressDialogFragment = null;
        }
    }
}
