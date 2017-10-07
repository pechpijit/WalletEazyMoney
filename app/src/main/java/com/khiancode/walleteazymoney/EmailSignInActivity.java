package com.khiancode.walleteazymoney;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailSignInActivity extends BaseActivity {
    private FirebaseAuth mAuth;
    final private static String TAG = "EmailSignInActivity";
    private static int VERIFICATION_CODE = 786;

    EditText inputEmail,inputPassword;
    Button btnSignin;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VERIFICATION_CODE) {
            Log.d(TAG, "Logout");
            FirebaseAuth.getInstance().signOut();
            LoginManager.getInstance().logOut();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_right);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_email);

        mAuth = FirebaseAuth.getInstance();

        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        btnSignin = findViewById(R.id.btn_signin);

        BroadcastReceiver broadcast_reciever = new BroadcastReceiver() {

            @Override
            public void onReceive(Context arg0, Intent intent) {
                String action = intent.getAction();
                if (action.equals("finish_activity")) {
                    finish();
                }
            }
        };
        registerReceiver(broadcast_reciever, new IntentFilter("finish_activity"));

    }

    public void onClickSignUp(View view) {
        startActivity(new Intent(this,EmailSignUpActivity.class));
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
    }

    public void onClickRecovery(View view) {

    }

    public void onClickSignIn(View view) {
        Log.d(TAG, "Login");
        btnSignin.setEnabled(false);

        if (!validate()) {
            btnSignin.setEnabled(true);
            return;
        }

        showProgressDialog(AUTH);

        String email = inputEmail.getText().toString();
        String password = inputPassword.getText().toString();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            AddUserData(user);
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            if (task.getException().getMessage().contains("password is invalid")) {
                                loginFaile("ไม่สามารถเข้าสู่ระบบได้", "อีเมล์หรือรหัสผ่านไม่ถูกต้อง กรุณาลองใหม่อีกครั้ง");
                            } else {
                                loginFaile("ไม่สามารถเข้าสู่ระบบได้", "ขออภัย เกิดข้อผิดพลาดบางอย่าง กรุณาลองใหม่อีกครั้งหรือติดต่อผู้พัฒนา");
                            }
                        }
                        btnSignin.setEnabled(true);
                        hideProgressDialog();
                    }
                });
    }

    private void loginFaile(String title, String message) {
        new AlertDialog.Builder(this,R.style.AppTheme_Dark_Dialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("ตกลง", null)
                .setCancelable(true)
                .show();
    }

    private void AddUserData(FirebaseUser user) {
        checkVerifiEmail();
    }

    private void checkVerifiEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user.isEmailVerified()) {
            startActivity(new Intent(EmailSignInActivity.this, TestActivity.class));
            Intent intent = new Intent("finish_activity");
            sendBroadcast(intent);
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        } else {
            Intent intent = new Intent(EmailSignInActivity.this, VerifiEmailActivity.class);
            intent.putExtra("usremail",inputEmail.getText().toString());
            intent.putExtra("usrpass",inputPassword.getText().toString());
            startActivityForResult(intent,VERIFICATION_CODE);
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
        hideProgressDialog();
    }

    public boolean validate() {
        boolean valid = true;

        String email = inputEmail.getText().toString();
        String password = inputPassword.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("กรุณากรอกอีเมล์");
            valid = false;
        } else {
            inputEmail.setError(null);
        }

        if (password.isEmpty() || password.length() < 4) {
            inputPassword.setError("กรุณากรอกรหัสที่มากกว่าหรือเท่ากับ 4 ตัว");
            valid = false;
        } else {
            inputPassword.setError(null);
        }

        return valid;
    }
}
