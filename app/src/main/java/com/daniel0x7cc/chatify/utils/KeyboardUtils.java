package com.daniel0x7cc.chatify.utils;


import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class KeyboardUtils {

    private KeyboardUtils() {
    }

    public static void hideKeyboard(Activity activity) {
        if (activity != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                View view = activity.getCurrentFocus();
                if (view == null) {
                    imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                } else {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }
    }

    public static void hideKeyboard(Context context, final EditText editText) {
        if (context != null && editText != null) {
            final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            editText.postDelayed(new Runnable() {
                @Override
                public void run() {
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                }
            }, 100);
        }
    }

    public static void showKeyboard(Context context) {
        if (context != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    public static void showKeyboard(Context context, final EditText editText) {
        if (context != null && editText != null) {
            final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            editText.postDelayed(new Runnable() {
                @Override
                public void run() {
                    editText.requestFocus();
                    imm.showSoftInput(editText, 0);
                }
            }, 100);
        }
    }

}
