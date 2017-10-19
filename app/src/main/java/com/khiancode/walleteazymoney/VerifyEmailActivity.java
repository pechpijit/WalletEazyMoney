package com.khiancode.walleteazymoney;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import io.fabric.sdk.android.Fabric;

public class VerifyEmailActivity extends BaseActivity {
    final private static String TAG = "VerifyEmailActivity";
    Button btnSendAgain;
    TextView txtEmail,txtSendAgain;

    private FirebaseAuth mAuth;

    private String email,password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_verifi_email);
        mAuth = FirebaseAuth.getInstance();

        Bundle i = getIntent().getExtras();
        email = i.getString("usremail");
        password = i.getString("usrpass");

        btnSendAgain = findViewById(R.id.btn_sendagain);
        txtEmail = findViewById(R.id.txt_email);
        txtSendAgain = findViewById(R.id.txt_sendagain);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        txtEmail.setText(currentUser.getEmail());

        new CountDownTimer(90000, 1000) {

            public void onTick(long millisUntilFinished) {
                btnSendAgain.setText(millisUntilFinished / 1000 + "วินาที");
            }

            public void onFinish() {
                txtSendAgain.setText("หากไม่ได้รับอีเมล์ยืนยัน \n สามารถส่งอีเมล์ยืนยันได้อีกครั้ง");
                btnSendAgain.setText("ส่งอีกครั้ง");
                btnSendAgain.setEnabled(true);
                btnSendAgain.setBackgroundColor(Color.parseColor("#009a9a"));
                btnSendAgain.setTextColor(Color.parseColor("#ffffff"));
            }
        }.start();

    }

    public void onClickSendAgain(View view) {
        btnSendAgain.setText("กำลังส่ง");
        btnSendAgain.setEnabled(false);
        btnSendAgain.setBackgroundColor(Color.parseColor("#D5D6D6"));
        btnSendAgain.setTextColor(Color.parseColor("#A4A5A5"));

        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        showProgressDialog(LOAD);
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "sendEMail:success");
                            btnSendAgain.setText("ส่งแล้ว");
                            dialogSendAgainSuccess(user);
                        } else if(task.getException() instanceof FirebaseNetworkException){
                            Log.w(TAG, "sendEMail:Internet failure.", task.getException());
                            dialogTM("ไม่สามารถเชื่อมต่อได้", "กรุณาตรวจสอบอินเทอร์เน็ตของท่าน และลองใหม่อีกครั้ง");
                        } else {
                            dialogTM("ล้มเหลว!","ขออภัย เกิดข้อผิดพลาดบางอย่าง กรุณาลองใหม่อีกครั้งหรือติดต่อผู้พัฒนา");
                            Crashlytics.logException(task.getException());
                            Log.w(TAG, "sendEMail:failure", task.getException());

                            btnSendAgain.setText("ส่งอีกครั้ง");
                            btnSendAgain.setEnabled(true);
                            btnSendAgain.setBackgroundColor(Color.parseColor("#009a9a"));
                            btnSendAgain.setTextColor(Color.parseColor("#ffffff"));
                        }

                        hideProgressDialog();
                    }
                });
    }

    public void onClickConfirmVerifiEmail(View view) {
        showProgressDialog(AUTH);
        FirebaseUser user = mAuth.getCurrentUser();
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "User re-authenticated.");
                        if (task.isSuccessful()) {
                            Log.d(TAG, "re-authenticated:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            checkVerifiEmail(user);
                        } else if(task.getException() instanceof FirebaseNetworkException){
                            Log.w(TAG, "re-authenticated:Internet failure.", task.getException());
                            hideProgressDialog();
                            dialogTM("ไม่สามารถเชื่อมต่อได้", "กรุณาตรวจสอบอินเทอร์เน็ตของท่าน และลองใหม่อีกครั้ง");
                        } else {
                            Crashlytics.logException(task.getException());
                            Log.w(TAG, "re-authenticated:failure", task.getException());
                            hideProgressDialog();
                            dialogTM("ไม่สามารถลงชื่อเข้าใช้ได้","กรุณายืนยันที่อยู่อีเมล์ก่อนเข้าใช้งาน หรือลองใหม่อีกครั้ง");
                        }
                    }
                });
    }

    private void checkVerifiEmail(FirebaseUser user) {
        if (user.isEmailVerified()) {
            startActivity(new Intent(VerifyEmailActivity.this, TestActivity.class));
            Intent intent = new Intent("finish_activity");
            sendBroadcast(intent);
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        } else {
            hideProgressDialog();
            dialogTM("ไม่สามารถลงชื่อเข้าใช้ได้","กรุณายืนยันที่อยู่อีเมล์ก่อนเข้าใช้งาน หรือลองใหม่อีกครั้ง");
        }
    }

    private void dialogSendAgainSuccess(FirebaseUser user) {
        new AlertDialog.Builder(this, R.style.AppTheme_Dark_Dialog)
                .setTitle("สำเร็จ!")
                .setMessage("ส่งข้อความเรียนร้อยแล้ว กรุณาตรวจสอบอีเมล์ '"+user.getEmail()+"'")
                .setPositiveButton("ตกลง", null)
                .setCancelable(true)
                .show();
    }

    public void onClickLogout(View view) {
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();
        finish();
    }
}
