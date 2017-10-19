package com.khiancode.walleteazymoney;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

import io.fabric.sdk.android.Fabric;

public class RecoveryActivity extends BaseActivity {
    LinearLayout viewInput, viewSendSuccess;
    TextView txtEmail;
    EditText inputEmail;
    Button btnSendRecovery;

    private static final String TAG = "RecoveryActivity";

    private FirebaseAuth mAuth;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_right);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_recovery);

        mAuth = FirebaseAuth.getInstance();

        viewInput = findViewById(R.id.view_input);
        viewSendSuccess = findViewById(R.id.view_sendsuccess);
        txtEmail = findViewById(R.id.txt_email);
        inputEmail = findViewById(R.id.input_email);
        btnSendRecovery = findViewById(R.id.btn_sendrecovery);

    }

    public void onClickSendRecover(View view) {
        btnSendRecovery.setText("กำลังส่ง");
        btnSendRecovery.setEnabled(false);
        btnSendRecovery.setBackgroundColor(Color.parseColor("#D5D6D6"));
        btnSendRecovery.setTextColor(Color.parseColor("#A4A5A5"));
        showProgressDialog(LOAD);

        final String emailAddress = inputEmail.getText().toString().trim();
        mAuth.sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "sendEMail:success");
                            viewInput.setVisibility(View.INVISIBLE);
                            viewSendSuccess.setVisibility(View.VISIBLE);
                            txtEmail.setText(emailAddress);
                        } else {
                            Log.w(TAG, "sendEMail:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                dialogTM("ล้มเหลว", "ไม่พบที่อยู่อีเมล์ในระบบ หรืออาจถูกลบไปแล้ว กรุณาตรวจสอบอีเมล์ให้ถูกต้อง และลองใหม่อีกครั้ง");
                            } else if(task.getException() instanceof FirebaseNetworkException){
                                dialogTM("ไม่สามารถเชื่อมต่อได้", "กรุณาตรวจสอบอินเทอร์เน็ตของท่าน และลองใหม่อีกครั้ง");
                            } else {
                                dialogTM("ไม่สามารถเข้าสู่ระบบได้", "ขออภัย เกิดข้อผิดพลาดบางอย่าง กรุณาลองใหม่อีกครั้งหรือติดต่อผู้พัฒนา");
                                Crashlytics.logException(task.getException());
                            }
                            btnSendRecovery.setText("ส่งอีเมล์รีเซ็ตรหัสผ่าน");
                            btnSendRecovery.setEnabled(true);
                            btnSendRecovery.setBackgroundColor(Color.parseColor("#009a9a"));
                            btnSendRecovery.setTextColor(Color.parseColor("#ffffff"));
                        }

                        hideProgressDialog();
                    }
                });
    }

    public void onClickSendRecoverSuccess(View view) {
        finish();
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_right);
    }
}
