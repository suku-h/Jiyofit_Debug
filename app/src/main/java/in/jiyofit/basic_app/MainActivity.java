package in.jiyofit.basic_app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends BaseActivity implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {
    public static Integer defaultTarget = 4000, SIGN_IN_REQUIRED = 4, REQUEST_OAUTH = 1;
    private TextView tvTarget, tvSteps;
    private DonutProgress dpSteps;
    private static final String TAG = "CCC";
    private static final String AUTH_PENDING = "isAuthPending";
    private static Long dayMillis = 86400000L;
    private GoogleApiClient googleApiClient;
    private Boolean authInProgress = false, fragmentLaunched = false;
    private Long lastHistoryUpdatedTime;
    private ProgressDialog progressDialog;
    private SharedPreferences workoutPrefs, loginPrefs, userActionPrefs;
    private SharedPreferences.Editor workoutEditor, userActionEditor;
    private String versionName;
    private static String userID;
    private DBAdapter dbAdapter;
    private Integer stepsTarget;
    private Calendar cal;
    private ResultCallback<Status> subscribeResultCallback;
    private CallbackManager callbackManager;

    String regId ="";
    GoogleCloudMessaging gcm;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FacebookSdk.sdkInitialize(this);
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.activity_main, contentFrameLayout);
        callbackManager = CallbackManager.Factory.create();
        dbAdapter = new DBAdapter(this);
        loginPrefs = getSharedPreferences("LoginInfo", MODE_PRIVATE);
        userActionPrefs = getSharedPreferences("UserActions", MODE_PRIVATE);
        userID = loginPrefs.getString("userID","");

        versionName = AppApplication.versionName;
        if (loginPrefs.getString("Network", "").equals("")) {
            sendNetworkDetails(userID);
        }
        if (!loginPrefs.getBoolean("isAppListSent", false)) {
            new AppListAsync().execute();
        }
        if (loginPrefs.getLong("firstOpenTime", 0L) == 0L) {
            SharedPreferences.Editor loginEditor = loginPrefs.edit();
            loginEditor.putLong("firstOpenTime", System.currentTimeMillis());
            loginEditor.commit();
        }

        workoutPrefs = getSharedPreferences("Workout", MODE_PRIVATE);
        stepsTarget = workoutPrefs.getInt("stepsTarget", defaultTarget);

        tvTarget = (TextView) findViewById(R.id.amain_tv_target);
        String targetString = getString(R.string.target) + " " + String.valueOf(stepsTarget) + " " + getString(R.string.steps);
        Spannable targetText = new SpannableString(targetString);
        int startPoint = getString(R.string.target).length() + 1;
        targetText.setSpan(new ForegroundColorSpan(Color.BLUE), startPoint, startPoint + String.valueOf(stepsTarget).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvTarget.setText(targetText);
        tvSteps = (TextView) findViewById(R.id.amain_tv_currentsteps);

        dpSteps = (DonutProgress) findViewById(R.id.amain_dp);
        dpSteps.setMax(stepsTarget);
        dpSteps.setFinishedStrokeWidth(70);
        dpSteps.setUnfinishedStrokeWidth(45);

        AlarmReceiver.setReminder(this);

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        getRegisterationID();
    }

    /*********************** Start Data Capture*********************/
    private void getDailySteps() {
        PendingResult<DailyTotalResult> result = Fitness.HistoryApi.readDailyTotal(googleApiClient, DataType.TYPE_STEP_COUNT_DELTA);

        result.setResultCallback(new ResultCallback<DailyTotalResult>() {
            @Override
            public void onResult(@NonNull DailyTotalResult dailyTotalResult) {
                if (dailyTotalResult.getStatus().isSuccess()) {
                    DataSet totalSet = dailyTotalResult.getTotal();
                    if (totalSet.isEmpty()) {
                        dpSteps.setProgress(0);
                        tvSteps.setText("0 " + getString(R.string.steps));
                    } else {
                        progressDialog.dismiss();
                        Integer steps = totalSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                        dpSteps.setProgress(Math.min(steps, stepsTarget));
                        if (steps > stepsTarget) {
                            dpSteps.setFinishedStrokeColor(ContextCompat.getColor(MainActivity.this, R.color.positive));
                            if(!userActionPrefs.getBoolean("walkCompleteInformed", false) && AppApplication.isNetworkAvailable(MainActivity.this)) {
                                AppApplication.getInstance().trackEvent("Workout", "WalkComplete", "userID: " + userID + " Steps: " + steps + " StepsTarget: " + stepsTarget);
                                userActionEditor = userActionPrefs.edit();
                                userActionEditor.putBoolean("walkCompleteInformed", true);
                                userActionEditor.commit();
                            }

                            ImageView ivFacebook = (ImageView) findViewById(R.id.amain_iv_facebook);
                            ivFacebook.setVisibility(View.VISIBLE);
                            final ShareDialog shareDialog = new ShareDialog(MainActivity.this);
                            shareDialog.registerCallback(callbackManager, new

                                    FacebookCallback<Sharer.Result>() {
                                        @Override
                                        public void onSuccess(Sharer.Result result) {}

                                        @Override
                                        public void onCancel() {}

                                        @Override
                                        public void onError(FacebookException error) {}
                                    });

                            ivFacebook.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (ShareDialog.canShow(ShareLinkContent.class)) {
                                        ShareLinkContent linkContent = new ShareLinkContent.Builder()
                                                .setContentTitle("Testing Facebook")
                                                .setContentDescription("The 'Hello Facebook' sample  showcases simple Facebook integration")
                                                .setContentUrl(Uri.parse("http://developers.facebook.com/android"))
                                                .build();

                                        shareDialog.show(linkContent);
                                    }
                                }
                            });

                            /*
                            Create custom view put values and then convert it into bitmap
                            Bitmap image = ...
                            SharePhoto photo = new SharePhoto.Builder()
                                    .setBitmap(image)
                                    .build();
                            SharePhotoContent content = new SharePhotoContent.Builder()
                                    .addPhoto(photo)
                                    .build();
                             */

                            ImageView ivWhatsapp = (ImageView) findViewById(R.id.amain_iv_whatsapp);
                            ivWhatsapp.setVisibility(View.VISIBLE);
                            ivWhatsapp.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    SocialFragment socialFragment = new SocialFragment();
                                    Bundle b = new Bundle();
                                    b.putString("SocialApp", "Whatsapp");
                                    socialFragment.setArguments(b);
                                    socialFragment.show(getFragmentManager(), "socialFragment");
                                }
                            });
                        }

                        tvSteps.setText(steps + " " + getString(R.string.steps));
                        Log.d(TAG, "STEPS: " + steps + " MAX: " + dpSteps.getMax());
                        workoutEditor = workoutPrefs.edit();
                        workoutEditor.putLong("lastStepsUpdateTime", System.currentTimeMillis());
                        workoutEditor.putInt("lastUpdatedSteps", steps);
                        workoutEditor.commit();
                    }
                } else {
                    Log.d(TAG, "Failed daily steps");
                    Log.d(TAG, "Res: " + dailyTotalResult.toString());
                    if (AppApplication.isNetworkAvailable(MainActivity.this)) {
                        startApiClientConnect();
                    } else {
                        AppApplication.toastIt(MainActivity.this, R.string.networkunavailable);
                    }
                }
            }
        });
    }

    public static void showStoredSteps(Context ctx, DonutProgress dp, TextView tv) {
        SharedPreferences workoutPrefs = ctx.getSharedPreferences("Workout", MODE_PRIVATE);
        //always set Max before the progress because, if progress = 456 then it will show 56 only
        //as default max is 100; so it will do progress = 456 - 4*100 = 56
        dp.setMax(workoutPrefs.getInt("stepsTarget", defaultTarget));
        Calendar midnight = new GregorianCalendar();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 1);
        int steps = workoutPrefs.getInt("lastUpdatedSteps", 0);
        if (midnight.getTimeInMillis() < workoutPrefs.getLong("lastStepsUpdateTime", System.currentTimeMillis() - dayMillis)) {
            dp.setProgress(Math.min(steps, workoutPrefs.getInt("stepsTarget", MainActivity.defaultTarget)));
            if (steps > workoutPrefs.getInt("stepsTarget", MainActivity.defaultTarget)) {
                dp.setFinishedStrokeColor(ContextCompat.getColor(ctx, R.color.positive));
            }
        } else {
            steps = 0;
            dp.setProgress(0);
        }
        tv.setText(steps + " " + ctx.getString(R.string.steps));
    }

    private class UpdateWalkAsync extends AsyncTask<Void, Void, Calendar> {

        @Override
        protected Calendar doInBackground(Void... params) {
            Log.d(TAG, "reached here");
            Date date = new Date(lastHistoryUpdatedTime);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 1);
            while (cal.getTimeInMillis() + dayMillis < System.currentTimeMillis()) {
                DataReadRequest readRequest = new DataReadRequest.Builder()
                        .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                        .bucketByTime(1, TimeUnit.DAYS)
                        .setTimeRange(cal.getTimeInMillis(), cal.getTimeInMillis() + dayMillis, TimeUnit.MILLISECONDS)
                        .build();

                DataReadResult output = Fitness.HistoryApi.readData(googleApiClient, readRequest).await(1, TimeUnit.MINUTES);
                if (output.getBuckets().size() > 0) {
                    for (Bucket bucket : output.getBuckets()) {
                        List<DataSet> dataSets = bucket.getDataSets();
                        for (DataSet dataSet : dataSets) {
                            for (final DataPoint dp : dataSet.getDataPoints()) {
                                for (Field field : dp.getDataType().getFields()) {
                                    int steps = dp.getValue(field).asInt();
                                    String entryDate = AppApplication.dateFormat.format(cal.getTime());
                                    Log.d(TAG, "DATE daily  " + entryDate);
                                    if (!dbAdapter.checkDateExists(entryDate)) {
                                        dbAdapter.insertDate(entryDate);
                                    }
                                    dbAdapter.walkData(entryDate, steps, workoutPrefs.getInt("stepsTarget", defaultTarget));
                                    AppApplication.getInstance().trackEvent("Workout", "Walk", "userID: " + userID + " Date: " + entryDate + " Steps: " + steps + " StepsTarget: " + stepsTarget);
                                    cal.add(Calendar.DAY_OF_MONTH, 1);
                                }
                            }
                        }
                    }
                }
            }

            return cal;
        }

        @Override
        protected void onPostExecute(Calendar cal) {
            super.onPostExecute(cal);
            workoutEditor = workoutPrefs.edit();
            workoutEditor.putLong("lastHistoryUpdatedTime", cal.getTimeInMillis());
            workoutEditor.commit();
            userActionEditor = userActionPrefs.edit();
            userActionEditor.putBoolean("walkCompleteInformed", false);
            userActionEditor.commit();

            Log.d(TAG, "idhar aaya " + workoutPrefs.getLong("lastHistoryUpdatedTime", 100L));
        }
    }
    /********************** End Data Capture*********************/
    /****************** Start Connecting with GoogleFit****************/
    @Override
    protected void onStart() {
        super.onStart();
        if (AppApplication.isNetworkAvailable(this)) {
            startApiClientConnect();
        } else {
            AppApplication.toastIt(this, R.string.networkunavailable);
            if (!fragmentLaunched) {
                launchFragments();
                fragmentLaunched = true;
            }
            showStoredSteps(this, dpSteps, tvSteps);
        }
    }

    public GoogleApiClient googleFitBuild(Activity activity, GoogleApiClient.ConnectionCallbacks connectionCallbacks, GoogleApiClient.OnConnectionFailedListener failedListener) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE), new Scope(Scopes.FITNESS_LOCATION_READ))
                .build();

        return new GoogleApiClient.Builder(activity)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(failedListener)
                .addApi(Fitness.HISTORY_API)
                .addApi(Fitness.SESSIONS_API)
                .addApi(Fitness.RECORDING_API)
                .enableAutoManage(this, 0, this)
                .build();
    }

    public void googleFitConnect(final Activity activity, final GoogleApiClient mGoogleApiClient) {
        if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                    Log.d(TAG, "Google API connected");
                    progressDialog.dismiss();
                    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                    activity.startActivityForResult(signInIntent, REQUEST_OAUTH);
                    cal = new GregorianCalendar();
                    Log.d(TAG, cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + "Signin Event Started");
                }

                @Override
                public void onConnectionSuspended(int i) {
                    progressDialog.dismiss();
                    startApiClientConnect();
                    AppApplication.getInstance().trackEvent("Google Fit", "Issue" + " AppVersion: " + versionName, "userID: " + userID + " Fitconnect suspended i=" + i);
                    Log.d(TAG, "FITCONNECT suspended i=" + i);
                }
            });
            mGoogleApiClient.connect(GoogleApiClient.SIGN_IN_MODE_OPTIONAL);
        }

        subscribeResultCallback = new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                cal = new GregorianCalendar();
                Log.d(TAG, cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + "SubscribeCallback");
                if (!status.isSuccess()) {
                    Log.d(TAG, "Subscription failed");
                    showStoredSteps(MainActivity.this, dpSteps, tvSteps);
                    if(status.getStatusCode() != SIGN_IN_REQUIRED) {
                        AppApplication.getInstance().trackEvent("Google Fit", "Issue" + " AppVersion: " + versionName, "userID: " + userID + " Reason: " + status.toString());
                    }
                } else {
                    progressDialog.dismiss();
                    if (!dbAdapter.checkDateExists(AppApplication.dateFormat.format(new Date()))) {
                        dbAdapter.insertDate(AppApplication.dateFormat.format(new Date()));
                    }

                    lastHistoryUpdatedTime = workoutPrefs.getLong("lastHistoryUpdatedTime", loginPrefs.getLong("firstOpenTime", 0L));
                    Log.d(TAG, "LHUT: " + lastHistoryUpdatedTime);
                    //lastHistoryUpdatedTime = lastHistoryUpdatedTime - 4 * dayMillis;
                    //Log.d(TAG, "new LHUT: " + lastHistoryUpdatedTime);
                    if (googleApiClient != null && googleApiClient.isConnected() && lastHistoryUpdatedTime < System.currentTimeMillis() - dayMillis) {
                        new UpdateWalkAsync().execute();
                    }

                    if (!fragmentLaunched) {
                        launchFragments();
                        fragmentLaunched = true;
                    }
                    getDailySteps();
                }
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                progressDialog.dismiss();
                cal = new GregorianCalendar();
                Log.d(TAG, cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + "Result OK");
                if (!googleApiClient.isConnecting() && !googleApiClient.isConnected()) {
                    Log.d(TAG, "Calling googleApiClient.connect again");
                    googleApiClient.connect(GoogleApiClient.SIGN_IN_MODE_OPTIONAL);
                } else {
                    onConnected(null);
                }
            } else if (resultCode == RESULT_CANCELED) {
                progressDialog.dismiss();
                cal = new GregorianCalendar();
                Log.d(TAG, cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + "Result Canceled");
                startApiClientConnect();
                Log.d(TAG, "RESULT_CANCELED");
            }
        } else {
            progressDialog.dismiss();
            startApiClientConnect();
            Log.d(TAG, "requestCode NOT request_oauth");
        }

        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Fitness.RecordingApi.subscribe(googleApiClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(subscribeResultCallback);

        cal = new GregorianCalendar();
        Log.d(TAG, cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + "API Connection");
    }

    @Override
    public void onConnectionSuspended(int i) {
        progressDialog.dismiss();
        startApiClientConnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (!authInProgress) {
            try {
                authInProgress = true;
                Log.d(TAG, "op: " + connectionResult.toString());
                progressDialog.dismiss();
                if (connectionResult.getErrorCode() == FitnessStatusCodes.NEEDS_OAUTH_PERMISSIONS) {
                    connectionResult.startResolutionForResult(this, REQUEST_OAUTH);
                } else if (connectionResult.getErrorCode() == ConnectionResult.NETWORK_ERROR) {
                    connectionResult.startResolutionForResult(this, REQUEST_OAUTH);
                    Log.d(TAG, "network unavailable");
                } else {
                    AppApplication.getInstance().trackEvent("Google Fit", "Issue" + " AppVersion: " + versionName, "userID: " + userID + "ConnectionError: " + connectionResult.getErrorCode());
                    connectionResult.startResolutionForResult(this, REQUEST_OAUTH);
                }
                if (!fragmentLaunched) {
                    launchFragments();
                    fragmentLaunched = true;
                }
                showStoredSteps(this, dpSteps, tvSteps);
            } catch (IntentSender.SendIntentException e) {
                Log.d(TAG, "SIE err");
                AppApplication.getInstance().trackException(e);
            }

            cal = new GregorianCalendar();
            Log.d(TAG, cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + "API Connection Failed");
        }
    }

    public void startApiClientConnect() {
        //Sometimes the app gets stuck and then calls StartApiClientConnect then progress dialog gets created twice
        if (progressDialog == null) {
            //Check this question to understand why progressDialog works this way.
            //http://stackoverflow.com/questions/9444520/progressdialog-dismiss-is-not-working
            progressDialog = ProgressDialog.show(this, "", getString(R.string.loading));
        }

        if (googleApiClient == null) {
            googleApiClient = googleFitBuild(this, this, this);
        }
        if (!googleApiClient.isConnecting() || !googleApiClient.isConnected()) {
            googleFitConnect(this, googleApiClient);
        }
    }
    /*****************End Connecting with GoogleFit****************/
    /********************** Start General Methods********************/
    public void launchFragments() {
        Bundle extras = getIntent().getExtras();
        if (extras == null || (extras.getString("method") != null && extras.getString("method").equals("walkReminder"))) {
            if(extras != null){
                AppApplication.getInstance().trackEvent("Workout", "WalkReminded", "userID" + userID + " Steps: " + workoutPrefs.getInt("lastUpdatedSteps", 0) + " StepsTarget: " + workoutPrefs.getInt("stepsTarget", defaultTarget));
            }

            if (AppApplication.isNetworkAvailable(this)) {
                Random r = new Random();
                //nextInt(int max) returns an int between 0 inclusive and max exclusive.
                int fragNum = r.nextInt(3);
                switch (fragNum) {
                    //Can not perform this action after onSaveInstanceState
                    case 0:
                        getFragmentManager().beginTransaction().replace(R.id.amain_fl_container, new TipsFragment(), "0").commitAllowingStateLoss();
                        break;
                    case 1:
                        getFragmentManager().beginTransaction().replace(R.id.amain_fl_container, new ArticleFragment(), "1").commitAllowingStateLoss();
                        break;
                    case 2:
                        getFragmentManager().beginTransaction().replace(R.id.amain_fl_container, new ProductFragment(), "2").commitAllowingStateLoss();
                        break;
                }
            } else {
                getFragmentManager().beginTransaction().replace(R.id.amain_fl_container, new TipsFragment(), "0").commitAllowingStateLoss();
            }
        } else if (extras.getString("method") != null && extras.getString("method").equals("showTip")) {
            TipsFragment tipsFragment = new TipsFragment();
            Bundle bundle = new Bundle();
            bundle.putString("tipID", extras.getString("tipID"));
            tipsFragment.setArguments(bundle);
            getFragmentManager().beginTransaction().replace(R.id.amain_fl_container, tipsFragment, "0").commitAllowingStateLoss();
        }
    }

    /*******************End General Methods******************/
    /******************** Start Info Capture****************/
    public void sendNetworkDetails(String userID) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            String networkType = "Unknown";
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                networkType = "Wifi";
            } else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                networkType = getNetworkClass(this);
            }
            AppApplication.getInstance().trackEvent("UserInfo", "Network", "userID: " + userID + " " + networkType);
            SharedPreferences.Editor loginEditor = loginPrefs.edit();
            loginEditor.putString("Network", networkType);
            loginEditor.commit();
        }
    }

    public String getNetworkClass(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            default:
                return "Unknown";
        }
    }

    private class AppListAsync extends AsyncTask<String, String, List<String>> {

        @Override
        protected List<String> doInBackground(String... params) {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            List<String> installedApps = new ArrayList<String>();

            for (ApplicationInfo app : apps) {
                //it's a system app, not interested
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                    //Discard this one
                    //in this case, it should be a user-installed app
                } else {
                    installedApps.add((String) pm.getApplicationLabel(app));
                }
            }
            return installedApps;
        }

        @Override
        protected void onPostExecute(List<String> appList) {
            super.onPostExecute(appList);
            AppApplication.getInstance().trackEvent("UserInfo", "AppList", "UserID:" + userID + " " + "List:" + appList.toString());
            SharedPreferences.Editor loginEditor = loginPrefs.edit();
            loginEditor.putBoolean("isAppListSent", true);
            loginEditor.commit();
        }
    }
    /*****************End Info Capture******************/
    /******************** Start Simple Housekeeping****************/
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.d(TAG, "new intent");

        launchFragments();
        fragmentLaunched = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppApplication.getInstance().trackScreenView("MainActivity");
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        AppApplication.getInstance().trackEvent("Minimize", "BackPress", "userID:" + userID + " MainActivity-BackPress");
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            this.moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
        Log.d(TAG, "onsavedinstance");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }

    /*****************End Simple Housekeeping ****************/


    /****************Experiment*****************/

    public void getRegisterationID() {
        Log.d(TAG, "STarted getReg");

        new AsyncTask() {
            @Override
            protected Object doInBackground(Object... params) {
                Log.d(TAG, "Came here in async");
                // TODO Auto-generated method stub
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(MainActivity.this);
                    }
                    regId = gcm.register("29775915338");
                    Log.d("in async task", regId);

                    // try
                    msg = "Device registered, registration ID=" + regId;

                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();

                }
                return msg;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                Log.d(TAG, o.toString() + " see black");
            }
        }.execute();

    }
}