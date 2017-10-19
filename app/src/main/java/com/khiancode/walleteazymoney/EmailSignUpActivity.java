package com.khiancode.walleteazymoney;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import io.fabric.sdk.android.Fabric;

public class EmailSignUpActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    final private static String TAG = "EmailSignInActivity";
    EditText inputName, inputEmail, inputPassword,inputConfirmpassword;
    Button btnSignup;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_right);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_sign_up_email);

        mAuth = FirebaseAuth.getInstance();

        inputName = findViewById(R.id.input_name);
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        inputConfirmpassword = findViewById(R.id.input_confirmpassword);
        btnSignup = findViewById(R.id.btn_signup);
    }

    public void onClickBackLogin(View view) {
        finish();
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_right);
    }

    public void onClickConfirmSignUp(View view) {
        Log.d(TAG, "Register");
        btnSignup.setEnabled(false);

        if (!validate()) {
            btnSignup.setEnabled(true);
            return;
        }

        showProgressDialog(REGIS);

        final String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {
                            Log.d(TAG, "signUpWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            AddUserData(user,name);
                        } else {
                            Log.w(TAG, "signUpWithEmail:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                dialogTM("ไม่สามารถใช้งานที่อยู่อีเมล์นี้ได้", "เนื่องจากที่อยู่อีเมลถูกใช้โดยบัญชีอื่นแล้ว กรุณาลองใหม่อีกครั้ง");
                            }else if(task.getException() instanceof FirebaseNetworkException){
                                dialogTM("ไม่สามารถเชื่อมต่อได้", "กรุณาตรวจสอบอินเทอร์เน็ตของท่าน และลองใหม่อีกครั้ง");
                            }else {
                                Crashlytics.logException(task.getException());
                                dialogTM("ไม่สามารถสมัครสมาชิกได้", "ขออภัย เกิดข้อผิดพลาดบางอย่าง กรุณาลองใหม่อีกครั้งหรือติดต่อผู้พัฒนา");
                            }
                            btnSignup.setEnabled(true);
                            hideProgressDialog();
                        }
                    }
                });
    }

    private void AddUserData(FirebaseUser user,String name) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile:success");
                        } else {
                            Log.w(TAG, "User profile:failure", task.getException());
                            Crashlytics.logException(task.getException());
                        }
                    }
                });

        onClickSendAgain(user);
    }

    public void onClickSendAgain(final FirebaseUser user) {

        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "sendEMail:success");
                            checkVerifiEmail(user);
                        } else {
                            Log.w(TAG, "sendEMail:failure", task.getException());
                            Crashlytics.logException(task.getException());
                            dialogSendEmailFailed();
                        }

                    }
                });
    }

    private void dialogSendEmailFailed() {
        new AlertDialog.Builder(this, R.style.AppTheme_Dark_Dialog)
                .setTitle("สมัครสมาชิกสำเร็จแล้ว")
                .setMessage("สมัครสมาชิกสำเร็จแล้ว แต่ระบบไม่สามารถส่งอีเมล์ยืนยันให้ท่านได้ เนื่องจากเกิดข้อผิดพลาดบางอย่าง กรุณาล็อคอินด้วยอีเมล์และรหัสผ่านที่ท่านได้ทำการสมัครเพื่อขอรับอีเมล์ยืนยัน ขออภัยมา ณ ที่นี้")
                .setPositiveButton("ตกลง", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    }
                })
                .setCancelable(true)
                .show();
    }

    private void checkVerifiEmail(FirebaseUser user) {
        if (user.isEmailVerified()) {
            startActivity(new Intent(EmailSignUpActivity.this, TestActivity.class));
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        } else {
            Intent intent = new Intent(EmailSignUpActivity.this, VerifyEmailActivity.class);
            intent.putExtra("usremail",inputEmail.getText().toString().trim());
            intent.putExtra("usrpass",inputPassword.getText().toString().trim());
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
        hideProgressDialog();
    }

    public boolean validate() {
        boolean valid = true;

        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmpassword = inputConfirmpassword.getText().toString().trim();

        if (name.isEmpty()) {
            inputName.setError("กรุณากรอกชื่อ");
            valid = false;
        } else {
            inputName.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("กรุณากรอกอีเมล์");
            valid = false;
        } else {
            inputEmail.setError(null);
        }

        if (password.isEmpty() || password.length() < 4) {
            inputPassword.setError("กรุณากรอกรหัสมากกว่าหรือเท่ากับ 4 ตัว");
            valid = false;
        } else {
            inputPassword.setError(null);
        }

        if (!password.equals(confirmpassword) || password.length() < 4) {
            inputConfirmpassword.setError("กรุณากรอกรหัสให้ตรงกัน");
            valid = false;
        } else {
            inputConfirmpassword.setError(null);
        }

        return valid;
    }
}
