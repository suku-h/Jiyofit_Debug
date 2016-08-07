package in.jiyofit.basic_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

public class PackageChangeReceiver extends BroadcastReceiver{
    private static String TAG = "CCC";
    private String userID;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences loginPrefs = context.getSharedPreferences("LoginInfo", Context.MODE_PRIVATE);
        userID = loginPrefs.getString("userID","");

        Uri data = intent.getData();
        String installedPackageName = data.getEncodedSchemeSpecificPart();
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = packageManager.getApplicationInfo(installedPackageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        //You can't get app name when it is being removed, u can only get the package name
        String applicationName = (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "unknown");

        //Intent.EXTRA_REPLACING is always added to the action.PACKAGE_REMOVED and action.PACKAGE_ADDED
        // when the app is updated. On normal installation and uninstallation this is false
        if(intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED) &&
                !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)){
            Log.d(TAG, "Installed: " + installedPackageName);
            Log.d(TAG, "Installed applicationName " + applicationName);
            AppApplication.getInstance().trackEvent("UserInfo", "Installed", "userID:" + userID + " AppName: " + applicationName + " PackageName: " + installedPackageName);
        }

        if(intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)
                && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)){
            Log.d(TAG, "Removed: " + installedPackageName);
            AppApplication.getInstance().trackEvent("UserInfo", "Uninstalled", "userID:" + userID + " PackageName: " + installedPackageName);
        }

        if(intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED)){
            Log.d(TAG, "Replaced my app");
            AppApplication.getInstance().trackEvent("UserInfo", "Updated", "userID:" + userID);
        }
    }
}
