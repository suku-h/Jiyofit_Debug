package in.jiyofit.basic_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class LoginActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private CallbackManager callbackManager;
    private LoginButton loginButton;
    private LinearLayout llDisclaimer;
    private FrameLayout overlayContainer;
    private Button fbDisabledBtn;
    private DisclaimerFragment disclaimerFragment;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        LoginManager.getInstance().logOut();
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setIcon(R.mipmap.app_icon);
        }

        loginButton = (LoginButton) findViewById(R.id.alogin_btn_facebook);
        Typeface engFont = Typeface.createFromAsset(getAssets(), "fonts/arial.ttf");
        loginButton.setTypeface(engFont);
        loginButton.setEnabled(false);

        TextView tvAppName = (TextView) findViewById(R.id.alogin_tv_appname);
        tvAppName.setTypeface(engFont);

        TextView tvAgree = (TextView) findViewById(R.id.alogin_tv_agree);
        String strAgree = getString(R.string.alogin_agree1) + Character.toString((char) 60) + getString(R.string.alogin_agree2) + Character.toString((char) 62) + getString(R.string.alogin_agree3) + " ";
        tvAgree.setText(strAgree);
        TextView tvDisclaimer = (TextView) findViewById(R.id.alogin_tv_disclaimer);
        tvDisclaimer.setText(R.string.alogin_disclaimer);
        overlayContainer = (FrameLayout) findViewById(R.id.overlaycontainer);
        llDisclaimer = (LinearLayout) findViewById(R.id.alogin_ll_disclaimer);
        fbDisabledBtn = (Button) findViewById(R.id.alogin_btn_fbdisabled);
        final CheckBox cbDisclaimer = (CheckBox) findViewById(R.id.alogin_cb_disclaimer);
        cbDisclaimer.setOnCheckedChangeListener(this);
        disclaimerFragment = new DisclaimerFragment();

        loginButton.setReadPermissions(Arrays.asList("public_profile", "email", "user_friends"));
        callbackManager = CallbackManager.Factory.create();
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AccessToken accessToken = loginResult.getAccessToken();
                GraphRequest graphRequest = GraphRequest.newMeRequest(accessToken, new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        SharedPreferences loginPrefs = getSharedPreferences("LoginInfo", MODE_PRIVATE);
                        SharedPreferences.Editor loginEditor = loginPrefs.edit();
                        loginEditor.putBoolean("isLoggedIn", true);
                        loginEditor.putLong("firstOpenTime", System.currentTimeMillis());
                        String userID = "";
                        try {
                            userID = object.getString("id");
                            loginEditor.putString("userID", userID);
                            loginEditor.putString("gender", object.getString("gender"));
                            loginEditor.commit();
                        } catch (JSONException e) {
                            loginEditor.putString("userID", userID);
                            loginEditor.putString("gender", "male");
                            loginEditor.commit();
                            AppApplication.getInstance().trackException(e);
                        }

                        try {
                            AppApplication.getInstance().trackEvent("GraphData", "first_name", "userID:" + userID + " " + object.getString("first_name"));
                        } catch (JSONException e) {
                            AppApplication.getInstance().trackEvent("GraphData", "JSONException", "userID:" + userID + " info: first_name");
                        }
                        try {
                            AppApplication.getInstance().trackEvent("GraphData", "last_name", "userID:" + userID + " " + object.getString("last_name"));
                        } catch (JSONException e) {
                            AppApplication.getInstance().trackEvent("GraphData", "JSONException", "userID:" + userID + " info: last_name");
                        }
                        try {
                            AppApplication.getInstance().trackEvent("GraphData", "gender", "userID:" + userID + " " + object.getString("gender"));
                        } catch (JSONException e) {
                            AppApplication.getInstance().trackEvent("GraphData", "JSONException", "userID:" + userID + " info: gender");
                        }
                        try {
                            AppApplication.getInstance().trackEvent("GraphData", "email", "userID:" + userID + " " + object.getString("email"));
                        } catch (JSONException e) {
                            AppApplication.getInstance().trackEvent("GraphData", "JSONException", "userID:" + userID + " info: email");
                        }
                        try {
                            AppApplication.getInstance().trackEvent("GraphData", "link", "userID:" + userID + " " + object.getString("link"));
                        } catch (JSONException e) {
                            AppApplication.getInstance().trackEvent("GraphData", "JSONException", "userID:" + userID + " info: link");
                        }

                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    }
                });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id, first_name, last_name, email, gender, link");
                graphRequest.setParameters(parameters);
                graphRequest.executeAsync();
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException e) {
                Toast.makeText(LoginActivity.this, R.string.alogin_trylater, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
        } catch (Exception e){
            AppApplication.getInstance().trackException(e);
        }
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void launchDisclaimer(View v) {
        overlayContainer.setVisibility(View.VISIBLE);
        overlayContainer.bringToFront();
        getFragmentManager().beginTransaction().add(R.id.overlaycontainer, disclaimerFragment).commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if(disclaimerFragment.isVisible()){
                getFragmentManager().beginTransaction().remove(disclaimerFragment).commitAllowingStateLoss();
                overlayContainer.setVisibility(View.GONE);
                return false;
            } else {
                this.moveTaskToBack(true);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            loginButton.setEnabled(true);
            if (!AppApplication.isNetworkAvailable(this)) {
                Toast.makeText(this, R.string.networkunavailable, Toast.LENGTH_LONG).show();
            }
            llDisclaimer.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
            fbDisabledBtn.setVisibility(View.GONE);
        } else {
            Toast.makeText(this, R.string.alogin_toast, Toast.LENGTH_LONG).show();
            llDisclaimer.setBackgroundColor(ContextCompat.getColor(this, R.color.translucentPrimary));
            loginButton.setEnabled(false);
            fbDisabledBtn.setVisibility(View.VISIBLE);
        }
    }

    public void cbNotClickedMsg(View v) {
        Toast.makeText(this, R.string.alogin_toast, Toast.LENGTH_LONG).show();
        llDisclaimer.setBackgroundColor(ContextCompat.getColor(this, R.color.translucentPrimary));
    }
}
