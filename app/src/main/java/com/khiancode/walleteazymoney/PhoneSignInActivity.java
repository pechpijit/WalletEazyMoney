package com.khiancode.walleteazymoney;

import android.content.Intent;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;

public class PhoneSignInActivity extends BaseActivity {

    final private static String TAG = "PhoneSignInActivity";
    private static final String KEY_VERIFY_IN_PROGRESS = "key_verify_in_progress";

    private static final int STATE_CODE_SENT = 1;
    private static final int STATE_VERIFY_FAILED = 2;
    private static final int STATE_VERIFY_SUCCESS = 3;
    private static final int STATE_SIGNIN_FAILED = 4;
    private static final int STATE_SIGNIN_SUCCESS = 5;

    private FirebaseAuth mAuth;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private String mVerificationId;

    private EditText inputPhone,inputOtp;
    private Button btnSendPhone,btnSendphoneotp,btnSendphoneAgain;
    private LinearLayout viewInputphone,viewInputOtp;
    private TextView txtPhone,txtSendPhoneAgain;

    private boolean mVerificationInProgress = false;
    private int sendAgain = 0;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_right);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_phone_sign_in);

        mAuth = FirebaseAuth.getInstance();

        inputPhone = findViewById(R.id.input_phone);
        inputOtp = findViewById(R.id.input_otp);
        btnSendPhone = findViewById(R.id.btn_sendphone);
        btnSendphoneotp = findViewById(R.id.btn_sendphoneotp);
        btnSendphoneAgain = findViewById(R.id.btn_sendphoneagain);
        viewInputphone = findViewById(R.id.view_inputphone);
        viewInputOtp = findViewById(R.id.view_inputotp);
        txtPhone = findViewById(R.id.txt_phone);
        txtSendPhoneAgain = findViewById(R.id.txt_sendphoneagain);

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);
                mVerificationInProgress = false;
                updateUI(STATE_VERIFY_SUCCESS, credential);
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);
                mVerificationInProgress = false;
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    dialogTM("ล้มเหลว!", "หมายเลขโทรศัพท์ไม่ถูกต้อง");
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    dialogTM("ล้มเหลว!", "ขออภัย เนื่องจากมีการขอรหัสยืนยันเกินโควต้าแล้ว จึงถูกระงับการร้องขอ กรุณาลองใหม่ภายหลังหรือติดต่อผู้พัฒนา");
                } else if(e instanceof FirebaseNetworkException){
                    dialogTM("ไม่สามารถเชื่อมต่อได้", "กรุณาตรวจสอบอินเทอร์เน็ตของท่าน และลองใหม่อีกครั้ง");
                } else {
                    dialogTM("ล้มเหลว!", "ขออภัย เกิดข้อผิดพลาดบางอย่าง กรุณาลองใหม่อีกครั้งหรือติดต่อผู้พัฒนา");
                    Crashlytics.logException(e);
                }
                updateUI(STATE_VERIFY_FAILED);
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent:" + verificationId);

                mVerificationId = verificationId;
                mResendToken = token;
                updateUI(STATE_CODE_SENT);
            }
        };
    }

    public void onClickSendPhoneAgain(View view) {
        Log.d(TAG, "onClickSendPhoneAgain");
        disableViews(btnSendphoneAgain);
        btnSendphoneAgain.setText("กำลังส่ง");
        btnSendphoneAgain.setBackgroundColor(Color.parseColor("#D5D6D6"));
        btnSendphoneAgain.setTextColor(Color.parseColor("#A4A5A5"));
        showProgressDialog(VERIFY);

        String phoneNumber = inputPhone.getText().toString().trim();
        String phoneTH = "+66" + phoneNumber.substring(1);
        Log.d(TAG, "PhoneTH:"+phoneTH);

        resendVerificationCode(phoneTH, mResendToken);
    }

    public void onClickSendPhoneOtp(View view) {
        Log.d(TAG, "onClickSendPhoneOtp");
        String code = inputOtp.getText().toString().trim();

        disableViews(btnSendPhone);

        if (TextUtils.isEmpty(code) || code.length() < 6) {
            inputOtp.setError("รหัสยืนยันไม่ถูกต้อง");
            enableViews(btnSendPhone);
            return;
        }

        showProgressDialog(VERIFY);

        verifyPhoneNumberWithCode(mVerificationId, code);
    }

    public void onClickSendPhone(View view) {
        Log.d(TAG, "onClickSendPhone");
        disableViews(btnSendPhone);
        if (!validatePhoneNumber()) {
            enableViews(btnSendPhone);
            return;
        }

        showProgressDialog(VERIFY);

        String phoneNumber = inputPhone.getText().toString().trim();
        String phoneTH = "+66" + phoneNumber.substring(1);
        Log.d(TAG, "PhoneTH:"+phoneTH);

        startPhoneNumberVerification(phoneTH);
    }

    private boolean validatePhoneNumber() {
        Log.d(TAG, "validatePhoneNumber");
        String phoneNumber = inputPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() < 10) {
            inputPhone.setError("หมายเลขโทรศัพท์ไม่ถูกต้อง");
            return false;
        }

        return true;
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        Log.d(TAG, "verifyPhoneNumberWithCode");
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        Log.d(TAG, "startPhoneNumberVerification");
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                mCallbacks);

        mVerificationInProgress = true;
    }

    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {
        Log.d(TAG, "resendVerificationCode.");
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                mCallbacks,
                token);

    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        Log.d(TAG, "signInWithPhoneAuthCredential");
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(STATE_SIGNIN_SUCCESS, user);
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                dialogTM("ไม่สามารถเข้าสู่ระบบได้", "รหัสผ่านไม่ถูกต้อง กรุณาลองใหม่อีกครั้ง");
                            } else if(task.getException() instanceof FirebaseNetworkException){
                                dialogTM("ไม่สามารถเชื่อมต่อได้", "กรุณาตรวจสอบอินเทอร์เน็ตของท่าน และลองใหม่อีกครั้ง");
                            } else {
                                dialogTM("ไม่สามารถเข้าสู่ระบบได้", "ขออภัย เกิดข้อผิดพลาดบางอย่าง กรุณาลองใหม่อีกครั้งหรือติดต่อผู้พัฒนา");
                                Crashlytics.logException(task.getException());
                            }
                            updateUI(STATE_SIGNIN_FAILED);
                        }
                    }
                });
    }

    private void updateUI(int uiState) {
        updateUI(uiState, mAuth.getCurrentUser(), null);
    }

    private void updateUI(int uiState, FirebaseUser user) {
        updateUI(uiState, user, null);
    }

    private void updateUI(int uiState, PhoneAuthCredential cred) {
        updateUI(uiState, null, cred);
    }

    private void updateUI(int uiState, FirebaseUser user, PhoneAuthCredential cred) {
        switch (uiState) {
            case STATE_CODE_SENT:
                sendAgain += 1;
                invisibleView(viewInputphone);
                visibleView(viewInputOtp);

                String strPhone = inputPhone.getText().toString().trim();
                String phone = strPhone.substring(0,3)+"-"+strPhone.substring(3,6)+"-"+strPhone.substring(6);
                txtPhone.setText(phone);

                if (sendAgain == 1) {
                    countDownSendAgain();
                } else {
                    btnSendphoneAgain.setText("ส่งแล้ว");
                }

                hideProgressDialog();
                break;
            case STATE_VERIFY_FAILED:
                enableViews(btnSendPhone);
                hideProgressDialog();
                break;
            case STATE_VERIFY_SUCCESS:
                disableViews(btnSendPhone);
                hideProgressDialog();
                showProgressDialog(AUTH);

                if (cred != null) {
                    if (cred.getSmsCode() != null) {
                        inputOtp.setText(cred.getSmsCode());
                    } else {
                        inputOtp.setText("");
                    }
                }

                break;
            case STATE_SIGNIN_FAILED:
                enableViews(btnSendPhone);
                hideProgressDialog();
                break;
            case STATE_SIGNIN_SUCCESS:
                startActivity(new Intent(this, TestActivity.class));
                Intent intent = new Intent("finish_activity");
                sendBroadcast(intent);
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
        }
    }

    private void countDownSendAgain() {
        Log.d(TAG, "countDownSendAgain");
        new CountDownTimer(90000, 1000) {

            public void onTick(long millisUntilFinished) {
                btnSendphoneAgain.setText(millisUntilFinished / 1000 + "วินาที");
            }

            public void onFinish() {
                txtSendPhoneAgain.setText("หากไม่ได้ข้อความ (SMS) รหัสยืนยัน \n สามารถขอรหัสได้อีกครั้ง");
                btnSendphoneAgain.setText("ส่งอีกครั้ง");
                enableViews(btnSendphoneAgain);
                btnSendphoneAgain.setBackgroundColor(Color.parseColor("#009a9a"));
                btnSendphoneAgain.setTextColor(Color.parseColor("#ffffff"));
            }
        }.start();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_VERIFY_IN_PROGRESS, mVerificationInProgress);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mVerificationInProgress = savedInstanceState.getBoolean(KEY_VERIFY_IN_PROGRESS);
    }
}
