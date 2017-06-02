package com.daniel0x7cc.chatify;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;

public class ProgressDialogFragment extends DialogFragment {

    public static final String KEY_MESSAGE = "key_message";

    private ProgressDialog progressDialog;

    public ProgressDialogFragment() {
    }

    public static ProgressDialogFragment newInstance(final String message) {
        final ProgressDialogFragment dialogFragment = new ProgressDialogFragment();

        final Bundle bundle = new Bundle();
        bundle.putString(KEY_MESSAGE, message);
        dialogFragment.setArguments(bundle);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setCancelable(true);
        setRetainInstance(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String message = getArguments().getString(KEY_MESSAGE);

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(message);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminateDrawable(getContext().getResources().getDrawable(R.drawable.custom_loading));
        progressDialog.setCanceledOnTouchOutside(false);

        // Disable the back button
        final DialogInterface.OnKeyListener keyListener = new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    return true;
                }
                return false;
            }

        };
        progressDialog.setOnKeyListener(keyListener);
        return progressDialog;
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        final FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, "dialog_fragment");
        ft.commitAllowingStateLoss();
    }

    public void setMessage(CharSequence message) {
        if (progressDialog != null) {
            progressDialog.setMessage(message);
        }
    }

}
