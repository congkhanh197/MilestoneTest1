package com.hasbrain.milestonetest;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SplashActivity extends AppCompatActivity {

    private static final String FACEBOOK_PERMISSIONS = "user_friends, user_photos, email";
    @Bind(R.id.bt_fb_login)
    LoginButton btFacebookLogin;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);

        if (isLoggedIn()){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    navigateIntoMainActivity();
                }
            }, 2000);
            return;
        }

        btFacebookLogin.setReadPermissions(FACEBOOK_PERMISSIONS);
        callbackManager = CallbackManager.Factory.create();
        btFacebookLogin.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AccessToken accessToken = loginResult.getAccessToken();
                Log.d("HasBrain", "Facebook accessToken " + accessToken);
                navigateIntoMainActivity();
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });
    }

    private boolean isLoggedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void navigateIntoMainActivity() {
        Intent startMainActivityIntent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(startMainActivityIntent);
        finish();
    }
}
