package com.khiancode.walleteazymoney;

import android.app.ProgressDialog;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class BaseActivity extends AppCompatActivity {

    String AUTH = "กำลังเข้าสู่ระบบ...";
    String REGIS = "กำลังสมัครสมาชิก...";
    String LOAD = "กำลังโหลดข้อมูล...";
    String VERIFY = "กำลังตรวจสอบ...";

    @VisibleForTesting
    public ProgressDialog mProgressDialog;

    public void showProgressDialog(String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this, R.style.AppTheme_Dark_Dialog);
            mProgressDialog.setMessage(message);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }

        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    public void enableViews(View... views) {
        for (View v : views) {
            v.setEnabled(true);
        }
    }

    public void disableViews(View... views) {
        for (View v : views) {
            v.setEnabled(false);
        }
    }

    public void invisibleView(View... views) {
        for (View v : views) {
            v.setVisibility(View.INVISIBLE);
        }
    }

    public void visibleView(View... views) {
        for (View v : views) {
            v.setVisibility(View.VISIBLE);
        }
    }

    public void dialogTM(String title, String message) {
        new AlertDialog.Builder(this,R.style.AppTheme_Dark_Dialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("ตกลง", null)
                .setCancelable(true)
                .show();
    }

    @Override
    public void onStop() {
        super.onStop();
        hideProgressDialog();
    }

}
