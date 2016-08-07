package in.jiyofit.basic_app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemClock;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class AlarmReceiver extends BroadcastReceiver {
    private static String TAG ="CCC";
    private static int WALK_REMINDER = 1, TIP_NOTIFICATION = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        setReminder(context);

        String alarmType = (intent.getStringExtra("alarmType") != null ? intent.getStringExtra("alarmType") : "empty");

        TextPaint textPaint = getTextPaints(context);

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        //Bitmap bitmapTitle = Bitmap.createBitmap(width - 60, 60, Bitmap.Config.ARGB_8888);
        //Canvas canvasTitle = new Canvas(bitmapTitle);
        //the bitmap aligns to the bottom of the notification
        //hence smaller height makes it look like there is margin/padding above which is forcing the text to go down
        Bitmap bitmapText = Bitmap.createBitmap(width - 60, dpToPx(context, 70), Bitmap.Config.ARGB_8888);
        Canvas canvasText = new Canvas(bitmapText);

        Calendar cal = Calendar.getInstance();
        Long currentTime = System.currentTimeMillis();

        if(alarmType.equals("walkReminder")){
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.HOUR_OF_DAY, 6);
            Long lowerTime = cal.getTimeInMillis();
            cal.set(Calendar.HOUR_OF_DAY, 8);
            Long upperTime = cal.getTimeInMillis();

            Log.d(TAG, "walkreminder called");
            SharedPreferences workoutPrefs = context.getSharedPreferences("Workout", Context.MODE_PRIVATE);
            int target = workoutPrefs.getInt("stepsTarget", 4000);
            int steps = workoutPrefs.getInt("lastUpdatedSteps", 0);
            if (workoutPrefs.getBoolean("walkReminder", true) && steps < target
                    && currentTime < upperTime && currentTime > lowerTime) {

                Log.d(TAG, "Alarm walk");
                String reminderString = context.getString(R.string.walkrempre) + " " + (target - steps) + " " + context.getString(R.string.walkrempost);
                Spannable reminder = new SpannableString(reminderString);
                int startPoint = context.getString(R.string.walkrempre).length() + 1;
                reminder.setSpan(new ForegroundColorSpan(Color.RED), startPoint, startPoint + String.valueOf(target - steps).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                StaticLayout staticLayout = new StaticLayout(reminder, textPaint, canvasText.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                canvasText.translate(10, (canvasText.getHeight() - staticLayout.getHeight())/2);
                canvasText.save();
                staticLayout.draw(canvasText);
                canvasText.restore();

                Intent notificationIntent = new Intent(context, MainActivity.class);
                notificationIntent.putExtra("method", "walkReminder");
                // set intent so it does not start a new activity
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_row);
                contentView.setImageViewBitmap(R.id.rnotification_iv_text, bitmapText);
                launchNotification(context, notificationIntent, contentView);
            }
        } else if (alarmType.equals("tip")){
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.HOUR_OF_DAY, 6);
            Long lowerTime = cal.getTimeInMillis();
            cal.set(Calendar.HOUR_OF_DAY, 8);
            Long upperTime = cal.getTimeInMillis();
            Log.d(TAG, "Alarm tip");
            if(currentTime < upperTime && currentTime > lowerTime) {
                Log.d(TAG, "Tip condition met");
                DBAdapter dbAdapter = new DBAdapter(context);
                ArrayList<String> tipIDs = dbAdapter.getTipIDs();
                Random r = new Random();
                int tipNum = r.nextInt(tipIDs.size());
                ArrayList<String> tip = dbAdapter.getTip(tipIDs.get(tipNum));
                String tipText = tip.get(2);
                StaticLayout staticLayout = new StaticLayout(tipText, textPaint, canvasText.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                canvasText.translate(10, (canvasText.getHeight() - staticLayout.getHeight()) / 2);
                canvasText.save();
                staticLayout.draw(canvasText);
                //After that to reset the canvas back for everything else
                canvasText.restore();

                Intent notificationIntent = new Intent(context, MainActivity.class);
                notificationIntent.putExtra("method", "showTip");
                notificationIntent.putExtra("tipID", tip.get(0));
                notificationIntent.setData(Uri.parse("foobar://" + SystemClock.elapsedRealtime()));
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_row);
                contentView.setImageViewBitmap(R.id.rnotification_iv_text, bitmapText);
                launchNotification(context, notificationIntent, contentView);
            }
        }
    }

    public static void setReminder(Context ctx) {
        SharedPreferences workoutPrefs = ctx.getSharedPreferences("Workout", Context.MODE_PRIVATE);
        //instead of FLAG_NO_CREATE use FLAG_UPDATE_CURRENT to prevent securityexception on samsung phones
        boolean walkAlarmDown = (PendingIntent.getBroadcast(ctx, WALK_REMINDER,
                new Intent(ctx, AlarmReceiver.class), PendingIntent.FLAG_NO_CREATE) == null);

        if (walkAlarmDown && workoutPrefs.getBoolean("walkReminderIsNeeded", true)) {
            setWalkReminder(ctx);
        } else if (!walkAlarmDown && !workoutPrefs.getBoolean("walkReminderIsNeeded", true)) {
            cancelWalkReminder(ctx);
        }

        boolean tipAlarmDown = (PendingIntent.getBroadcast(ctx, TIP_NOTIFICATION,
                new Intent(ctx, AlarmReceiver.class), PendingIntent.FLAG_NO_CREATE) == null);

        Log.d(TAG, "TipAlarm " + !tipAlarmDown + " walkAlarm " + !walkAlarmDown);

        if (tipAlarmDown) {
            Log.d(TAG, "START TIPS");
            setTipsReminder(ctx);
        }
    }

    private static void setWalkReminder(Context ctx) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 15);
        calendar.set(Calendar.SECOND, 0);
        Intent walkIntent = new Intent(ctx, AlarmReceiver.class);
        Log.d(TAG, "Started WALK SET");
        walkIntent.putExtra("alarmType", "walkReminder");
        PendingIntent walkPendingIntent = PendingIntent.getBroadcast(ctx, WALK_REMINDER, walkIntent, PendingIntent.FLAG_NO_CREATE);
        AlarmManager walkAlarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        walkAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, walkPendingIntent);
    }

    private static void cancelWalkReminder(Context ctx) {
        Intent walkIntent = new Intent(ctx, AlarmReceiver.class);
        PendingIntent walkPendingIntent = PendingIntent.getBroadcast(ctx, WALK_REMINDER, walkIntent, PendingIntent.FLAG_NO_CREATE);
        Log.d(TAG, "Walk Canceled");
        AlarmManager walkAlarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        walkAlarmManager.cancel(walkPendingIntent);
        walkPendingIntent.cancel();
    }

    private static void setTipsReminder(Context ctx) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 12);
        calendar.set(Calendar.SECOND, 0);
        Intent tipIntent = new Intent(ctx, AlarmReceiver.class);
        tipIntent.putExtra("alarmType", "tip");
        PendingIntent tipPendingIntent = PendingIntent.getBroadcast(ctx, TIP_NOTIFICATION, tipIntent, PendingIntent.FLAG_NO_CREATE);
        AlarmManager tipAlarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        tipAlarmManager.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, tipPendingIntent);
    }

    public void launchNotification(Context context, Intent notificationIntent, RemoteViews contentView){
        int notificationID = (int) System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_NO_CREATE);
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification.Builder notificationBuilder = new Notification.Builder(context);
        Notification notification = notificationBuilder
                .setSmallIcon(R.drawable.small_icon)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSound(alarmSound)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setContent(contentView)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                .build();
        notificationManager.notify(notificationID, notification);
    }

    public TextPaint getTextPaints(Context context){
        int titleSp = context.getResources().getDimensionPixelSize(R.dimen.notification_title_textSize);

        TextPaint paintTitle = new TextPaint();
        paintTitle.setAntiAlias(true);
        paintTitle.setSubpixelText(true);
        paintTitle.setTypeface(AppApplication.hindiFont);
        paintTitle.setStyle(Paint.Style.FILL);
        paintTitle.setColor(Color.BLACK);
        paintTitle.setTextSize(titleSp);
        paintTitle.setFakeBoldText(true);
        paintTitle.setTextAlign(Paint.Align.LEFT);

        return paintTitle;
    }

    public int dpToPx(Context context, int dp){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int)((dp * displayMetrics.density) + 0.5);
    }
/*
    public int pxToDp(Context context, int px){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) ((px/displayMetrics.density)+0.5);
    }*/
}
