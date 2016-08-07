package in.jiyofit.basic_app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.clevertap.android.sdk.ActivityLifecycleCallback;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.exceptions.CleverTapMetaDataNotFoundException;
import com.clevertap.android.sdk.exceptions.CleverTapPermissionsNotSatisfied;
import com.google.android.gms.analytics.ExceptionParser;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class AppApplication extends Application {
    private static AppApplication instance;
    public static String userID, versionName;
    public static Typeface engFont = null, hindiFont = null;
    public static SimpleDateFormat dateFormat;

    @Override
    public void onCreate() {
        //This step is extremely important and enables CleverTap to track notification opens,
        //display in-app notifications, track deep links, and other important user behaviour.
        //call ActivityLifecycleCallback.register(this); before super.onCreate() in your custom Application class.
        ActivityLifecycleCallback.register(this);
        super.onCreate();

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/mfdev010.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        instance = this;
        AnalyticsTrackers.initialize(this);
        AnalyticsTrackers.getInstance().get(AnalyticsTrackers.Target.APP);

        engFont = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/arial.ttf");
        hindiFont = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/mfdev010.ttf");

        dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);

        SharedPreferences loginPrefs = getSharedPreferences("LoginInfo", MODE_PRIVATE);
        userID = loginPrefs.getString("userID", "");

        try {
            versionName = getApplicationContext().getPackageManager()
                    .getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException nnf) {
            versionName = "NameNotFound";
        }

        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.i("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }

        ExceptionReporter myHandler =
                new ExceptionReporter(AnalyticsTrackers.getInstance().get(AnalyticsTrackers.Target.APP),
                        Thread.getDefaultUncaughtExceptionHandler(), this);

        ExceptionParser ep = new ExceptionParser() {
            @Override
            public String getDescription(String s, Throwable throwable) {
                Log.d("CCC", "THROWABLE: " + Log.getStackTraceString(throwable));
                return "UserID:" + userID + " AppVersion:" + versionName + " UncaughtException:" + Log.getStackTraceString(throwable);
            }
        };

        myHandler.setExceptionParser(ep);

        // Make myHandler the new default uncaught exception handler.
        Thread.setDefaultUncaughtExceptionHandler(myHandler);

        CleverTapAPI cleverTap;
        try {
            cleverTap = CleverTapAPI.getInstance(getApplicationContext());
        } catch (CleverTapMetaDataNotFoundException e) {
            // thrown if you haven't specified your CleverTap Account ID or Token in your AndroidManifest.xml
        } catch (CleverTapPermissionsNotSatisfied e) {
            // thrown if you haven’t requested the required permissions in your AndroidManifest.xml
        }
    }

    public static synchronized AppApplication getInstance() {
        return instance;
    }

    public synchronized Tracker getGoogleAnalyticsTracker() {
        AnalyticsTrackers analyticsTrackers = AnalyticsTrackers.getInstance();
        return analyticsTrackers.get(AnalyticsTrackers.Target.APP);
    }

    public void trackScreenView(String screenName) {
        Tracker t = getGoogleAnalyticsTracker();

        t.setScreenName(screenName);

        t.send(new HitBuilders.ScreenViewBuilder().build());

        GoogleAnalytics.getInstance(this).setLocalDispatchPeriod(15);
        GoogleAnalytics.getInstance(this).dispatchLocalHits();
    }

    public void trackException(Exception e) {
        if (e != null) {
            Tracker t = getGoogleAnalyticsTracker();

            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription(
                            new StandardExceptionParser(this, null)
                                    .getDescription(Thread.currentThread().getName(), e))
                    .setFatal(false)
                    .build()
            );

            DisplayMetrics metrics = new DisplayMetrics();
            //The error of the getWindowManager says that cannot find the method because
            // the root of this class doesn't have it.
            ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);

            if(userID.length() > 0){
                trackEvent("Exception", e.getClass().getSimpleName() + " AppVersion: " + versionName, "userID: " + userID + " Stacktrace: " + stringifyStacktrace(e, null));
                trackEvent("Exception", e.getClass().getSimpleName() + " AppVersion: " + versionName, "userID: " + userID + " SDK: " + Build.VERSION.SDK_INT + " Manu: " + Build.MANUFACTURER + " Model: " + Build.MODEL + " Display: " + metrics.widthPixels + "X" + metrics.heightPixels);
            } else {
                trackEvent("Exception", e.getClass().getSimpleName() + " AppVersion: " + versionName, "Stacktrace: " + stringifyStacktrace(e, null));
                trackEvent("Exception", e.getClass().getSimpleName() + " AppVersion: " + versionName, "SDK: " + Build.VERSION.SDK_INT + " Manu: " + Build.MANUFACTURER + " Model: " + Build.MODEL + " Display: " + metrics.widthPixels + "X" + metrics.heightPixels);
            }
        }
    }

    public void trackEvent(String category, String action, String label) {
        Tracker t = getGoogleAnalyticsTracker();
        t.send(new HitBuilders.EventBuilder().setCategory(category).setAction(action).setLabel(label).build());
    }

    private String stringifyStacktrace (Exception e, Throwable t){
        if(e != null) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return sw.toString() + e.getCause().toString();
        } else {
            return Arrays.toString(t.getStackTrace());
        }
    }

    public static void toastIt(Context ctx, int toastTextID) {
        String message = ctx.getString(toastTextID);
        Toast toast = Toast.makeText(ctx, message, Toast.LENGTH_LONG);
        TextView textView = new TextView(ctx);
        textView.setBackgroundColor(Color.DKGRAY);
        textView.setTextColor(Color.WHITE);
        textView.setTypeface(AppApplication.hindiFont);
        textView.setTextSize(22);
        textView.setText(message);
        textView.setPadding(30, 20, 30, 20);
        toast.setView(textView);
        toast.show();
    }

    public static Boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static Context getAppContext(){
        return instance.getApplicationContext();
    }

    public static void changeBtnColor(Context ctx, Button btn, int colorId) {
        btn.setBackground(ContextCompat.getDrawable(ctx, R.drawable.button_unclicked));
        GradientDrawable sd = (GradientDrawable) btn.getBackground().mutate();
        sd.setColor(ContextCompat.getColor(ctx, colorId));
        sd.invalidateSelf();
    }

    public static int dpToPx(Context context, int dp){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int)((dp * displayMetrics.density) + 0.5);
    }
}

