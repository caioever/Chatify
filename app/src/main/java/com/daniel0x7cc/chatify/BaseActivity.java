package com.daniel0x7cc.chatify;

import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.widget.Toast;


public class BaseActivity extends AppCompatActivity {
    ProgressDialogFragment progressDialogFragment;

    public void showToast(String message){
        Toast.makeText(App.getContext(), message, Toast.LENGTH_LONG).show();
    }

    public void showLoading(String name) {
        progressDialogFragment = ProgressDialogFragment.newInstance(name);
        progressDialogFragment.show(getSupportFragmentManager(), "key");
    }

    public void dismissLoading() {
        if (progressDialogFragment != null) {
            progressDialogFragment.dismissAllowingStateLoss();
            progressDialogFragment = null;
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        if (getSupportActionBar() != null && title != null && title.length() > 0) {
//            SpannableString titleFormatted = new SpannableString(title.toString());
//            titleFormatted.setSpan(new TypefaceSpan(this, "Greetings-Bold.ttf"), 0,
//                    titleFormatted.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            getSupportActionBar().setTitle(titleFormatted);
        }
    }

}
