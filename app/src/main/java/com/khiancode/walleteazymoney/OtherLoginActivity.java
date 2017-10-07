package com.khiancode.walleteazymoney;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GithubAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class OtherLoginActivity extends BaseActivity {

    final private static String TAG = "OtherLoginActivity";

    private static int RC_SIGN_IN = 1604;

    private FirebaseAuth mAuth;

    private GoogleApiClient mGoogleApiClient;

    private CallbackManager mCallbackManagerFB;

    private TwitterLoginButton mLoginButtonTwitter;

    private boolean twitterLogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        printHashKey();
        FacebookSdk.sdkInitialize(getApplicationContext());
        Twitter.initialize(this);
        setContentView(R.layout.activity_other_login);

        mAuth = FirebaseAuth.getInstance();

        mLoginButtonTwitter = findViewById(R.id.login_twitter);
        mLoginButtonTwitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                twitterLogin = true;
            }
        });
        mLoginButtonTwitter.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                Log.d(TAG, "twitterLogin:success" + result);
                handleTwitterSession(result.data);
            }

            @Override
            public void failure(TwitterException exception) {
                Log.w(TAG, "twitterLogin:failure", exception);
            }
        });

        mCallbackManagerFB = CallbackManager.Factory.create();
        LoginButton loginButton = findViewById(R.id.login_facebook);
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(mCallbackManagerFB, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(OtherLoginActivity.this, null)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        findViewById(R.id.login_google).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                Toast.makeText(this, "Google Sign In failed", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (twitterLogin) {
            twitterLogin = false;
            Toast.makeText(this, "twitter", Toast.LENGTH_SHORT).show();
            mLoginButtonTwitter.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Toast.makeText(this, "facebook", Toast.LENGTH_SHORT).show();
        mCallbackManagerFB.onActivityResult(requestCode, resultCode, data);
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        showProgressDialog(AUTH);
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(OtherLoginActivity.this,TestActivity.class));
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(OtherLoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        hideProgressDialog();
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);
        showProgressDialog(AUTH);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(OtherLoginActivity.this,TestActivity.class));
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        } else {
                            LoginManager.getInstance().logOut();
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(OtherLoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        hideProgressDialog();
                    }
                });
    }

    private void handleTwitterSession(TwitterSession session) {
        Log.d(TAG, "handleTwitterSession:" + session);
        showProgressDialog(AUTH);
        AuthCredential credential = TwitterAuthProvider.getCredential(
                session.getAuthToken().token,
                session.getAuthToken().secret);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(OtherLoginActivity.this,TestActivity.class));
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        } else {
                            LoginManager.getInstance().logOut();
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(OtherLoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        hideProgressDialog();
                    }
                });
    }

    public void onClickSignInGithub(View view) {

        showProgressDialog(AUTH);
        String token = "6b99ed84c3e8a6efdd3924f0625c40f594ddcfb0";
        AuthCredential credential = GithubAuthProvider.getCredential(token);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(OtherLoginActivity.this,TestActivity.class));
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        } else {
                            LoginManager.getInstance().logOut();
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(OtherLoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        hideProgressDialog();
                    }
                });
    }

    public void onClickSignInAnonymous(View view) {
        showProgressDialog(AUTH);
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(OtherLoginActivity.this,TestActivity.class));
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        } else {
                            LoginManager.getInstance().logOut();
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(OtherLoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        hideProgressDialog();
                    }
                });
    }

    public void onClickSignIn(View view) {
        startActivity(new Intent(this,EmailSignInActivity.class));
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
    }

    public void printHashKey() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String hashKey = new String(Base64.encode(md.digest(), 0));
                Log.i(TAG, "printHashKey() Hash Key: " + hashKey);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "printHashKey()", e);
        } catch (Exception e) {
            Log.e(TAG, "printHashKey()", e);
        }
    }
}
