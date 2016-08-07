package in.jiyofit.basic_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.DonutProgress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import pl.droidsonroids.gif.GifDrawable;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class WorkoutActivity extends Activity implements ChangeExercise,
        PauseCommunicator, GoBackwardForwardInterface, MediaPlayer.OnCompletionListener {
    private long totalMillis, totalTime;
    private int workoutExerciseNum, forwardCount = 0, workoutDuration, newDayCode;
    private int resID;
    private String workoutID;
    private PauseFragment pauseFragment;
    private WorkoutOverAlertFragment alertFragment;
    private TextView totalWorkoutTimer, tvExerciseName, tvMuscleTarget;
    private TotalTimeCounter timeCounter;
    private long totalTimerOnPauseMillis, counterOnPauseMillis;
    private SharedPreferences workoutPrefs;
    private ImageView ivGifView;
    private MediaPlayer instruction;
    private Counter timer;
    private GifDrawable exerciseGif;
    private Boolean isPaused, audioPaused, isPlayerReleased;
    private ArrayList<String> audioList;
    private ArrayList<ArrayList<String>> workoutExerciseData;
    private ArrayList<Long> remainingDuration;
    private static String userID;
    private DonutProgress dpDuration;
    private DBAdapter dbAdapter;
    private static  String TAG = "CCC";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        SharedPreferences loginPrefs = getSharedPreferences("LoginInfo", MODE_PRIVATE);
        userID = loginPrefs.getString("userID","");

        totalWorkoutTimer = (TextView) findViewById(R.id.aworkout_tv_totaltimer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        workoutPrefs = getSharedPreferences("Workout", MODE_PRIVATE);
        newDayCode = workoutPrefs.getInt("newDayCode", 1);
        workoutDuration = workoutPrefs.getInt("workoutDuration", 10);
        AppApplication.getInstance().trackEvent("Workout", "Started",  "userID:" + userID + " Started- " + newDayCode);
        dbAdapter = new DBAdapter(this);
        String gender = loginPrefs.getString("gender", "female");
        String workoutType = "Full Body";
        switch (workoutPrefs.getInt("workoutType", 1)){
            case 1:
                workoutType = "Full Body";
                break;
            case 2:
                workoutType = "Weight Loss";
                break;
            case 3:
                workoutType = "Muscle Gain";
                break;
        }
        Log.d(TAG, "WT: " + workoutType + " d: " + workoutDuration + " ncd: " + newDayCode + " g: " +gender);
        workoutID = dbAdapter.getWorkoutID(gender, newDayCode, workoutDuration,  workoutType);
        Log.d(TAG, "wID: " + workoutID);
        ArrayList<ArrayList<String>> workoutPlaylist = dbAdapter.getWorkout(workoutID);
        workoutExerciseData = new ArrayList<>();
        for (int i = 0; i < workoutPlaylist.size(); i++){
            ArrayList<String> tempArrayList = new ArrayList<>();
            tempArrayList.addAll(workoutPlaylist.get(i));
            ArrayList<String> exerciseData = dbAdapter.getExerciseData(workoutPlaylist.get(i).get(0));
            tempArrayList.addAll(exerciseData);
            workoutExerciseData.add(tempArrayList);
        }
        Log.d(TAG, "WED: " + workoutExerciseData.toString());

        remainingDuration = new ArrayList<>();
        Long durationPassed = 0L;
        for(int i = 0; i < workoutExerciseData.size(); i++){
            remainingDuration.add((long)(workoutDuration * 60 * 1000) - durationPassed);
            durationPassed = durationPassed + Long.valueOf(workoutExerciseData.get(i).get(1)) * 1000;
        }

        totalTime = (long)(workoutDuration * 60 * 1000);
        TotalTimerStartStop(totalTime, true);

        tvExerciseName = (TextView) findViewById(R.id.aworkout_tv_name);
        tvMuscleTarget = (TextView) findViewById(R.id.aworkout_tv_target);
        tvMuscleTarget.setTypeface(AppApplication.engFont);
        ivGifView = (ImageView) findViewById(R.id.aworkout_gv);

        dpDuration = (DonutProgress) findViewById(R.id.aworkout_dp_timer);
        dpDuration.setSuffixText("");

        workoutExerciseNum = 0;
        changeExercise(workoutExerciseNum);
        RelativeLayout workoutLayout = (RelativeLayout) findViewById(R.id.aworkout_layout);
        workoutLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exercisePlayPause(isPaused, workoutExerciseNum);
            }
        });

        AppApplication.getInstance().trackScreenView("WorkoutActivity");
    }

    public void changeExercise(int exerciseNumber) {
        if (exerciseNumber < workoutExerciseData.size()) {
            if (instruction != null) {
                instruction.release();
                instruction = null;
            }
            if (timer != null) {
                timer.cancel();
            }
            if(workoutExerciseData.get(exerciseNumber).get(0).equals("rest")){
                dpDuration.setMax(Integer.valueOf(workoutExerciseData.get(exerciseNumber).get(1)));
            } else {
                dpDuration.setMax(10);
            }
            dpDuration.setProgress(0);

            if(workoutExerciseData.get(exerciseNumber).get(9).length() == 0){
                tvMuscleTarget.setText(workoutExerciseData.get(exerciseNumber).get(8));
            } else {
                tvMuscleTarget.setText(workoutExerciseData.get(exerciseNumber).get(8) + ", " + workoutExerciseData.get(exerciseNumber).get(9));
            }

            workoutExerciseNum = exerciseNumber;
            tvExerciseName.setText(workoutExerciseData.get(exerciseNumber).get(3));
            tvExerciseName.setTypeface(AppApplication.engFont);
            resID = getResources().getIdentifier(workoutExerciseData.get(exerciseNumber).get(0), "drawable", getPackageName());
            ivGifView.setImageDrawable(ContextCompat.getDrawable(this, resID));
            audioList = new ArrayList<>();
            int colNumPreAudio = 4, audioNum = 4;
            if(Integer.valueOf(workoutExerciseData.get(exerciseNumber).get(1)) == 30){
                audioNum = 3;
            }
            for (int i = colNumPreAudio; i < colNumPreAudio + audioNum; i++) {
                audioList.add(workoutExerciseData.get(exerciseNumber).get(i));
            }

            Log.d(TAG, "EXNum: " + exerciseNumber + " al : " + audioList.toString());

            if (!workoutExerciseData.get(exerciseNumber).get(2).equals("-1")) {
                resID = getResources().getIdentifier(audioList.get(0), "raw", getPackageName());
                instruction = MediaPlayer.create(this, resID);
                instruction.start();
                instruction.setOnCompletionListener(this);
                isPlayerReleased = false;

                ivGifView.setScaleX(1);
            } else {
                resID = getResources().getIdentifier("cha", "raw", getPackageName());
                instruction = MediaPlayer.create(this, resID);
                instruction.start();
                instruction.setOnCompletionListener(this);
                isPlayerReleased = false;
                //reverse the gif
                ivGifView.setScaleX(-1);
            }

            Long durationLeft = (long) (Integer.valueOf(workoutExerciseData.get(exerciseNumber).get(1)) * 1000);
            timer = new Counter(this, exerciseNumber, workoutExerciseData.get(exerciseNumber).get(0), durationLeft, Integer.valueOf(workoutExerciseData.get(exerciseNumber).get(1)));
            timer.start();
            audioPaused = false;
            isPaused = false;
        } else {
            if (instruction != null) {
                instruction.release();
                instruction = null;
            }
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            if (timeCounter != null) {
                timeCounter.cancel();
                timeCounter = null;
            }

            int oldDayCode = workoutPrefs.getInt("newDayCode", 1);
            int newDayCode = oldDayCode + 1;
            if (oldDayCode == 5) {
                newDayCode = 1;
            }
            SharedPreferences.Editor workoutEditor = workoutPrefs.edit();
            workoutEditor.putInt("newDayCode", newDayCode);
            workoutEditor.commit();

            alertFragment = new WorkoutOverAlertFragment();
            Bundle bundle = new Bundle();
            if(forwardCount < Math.floor(0.2 * workoutExerciseData.size())){
                dbAdapter.workoutComplete(AppApplication.dateFormat.format(new Date()), 1,
                        workoutDuration, workoutPrefs.getInt("workoutType", 1));
                bundle.putBoolean("tooManyForwards", false);
                AppApplication.getInstance().trackEvent("Workout", "Complete", "userID: " + userID + " WorkoutID: " + workoutID);
            } else {
                bundle.putBoolean("tooManyForwards", true);
                AppApplication.getInstance().trackEvent("Workout", "TooManyForwards", "userID: " + userID + " WorkoutID: " + workoutID + " ForwardCount: " + forwardCount);
            }
            alertFragment.setArguments(bundle);
            //this prevents the dialog from being canceled by any method like
            // clicking on the activity (outside the dialog), pressing back
            alertFragment.setCancelable(false);
            alertFragment.show(getFragmentManager(), "workoutOver");
        }
    }

    @Override
    public void PassExerciseNum(int exerciseNum, Boolean isPaused) {
        if (!isPaused) {
            Bundle b = new Bundle();
            b.putInt("dayCode", newDayCode);
            pauseFragment = new PauseFragment();
            pauseFragment.getExNum(exerciseNum);
            pauseFragment.setArguments(b);
            getFragmentManager().beginTransaction().add(R.id.aworkout_layout, pauseFragment, "pause").commit();
            getFragmentManager().executePendingTransactions();
        } else {
            getFragmentManager().beginTransaction().remove(pauseFragment).commit();
            pauseFragment = null;
            if(workoutExerciseNum != workoutExerciseData.size()) {
                exercisePlayPause(true, exerciseNum);
            }
        }
    }

    public void TotalTimerStartStop(long totalTimeVal, Boolean isPaused) {
        if (!isPaused) {
            timeCounter.cancel();
        } else {
            timeCounter = new TotalTimeCounter(totalTimeVal, 1000);
            timeCounter.start();
        }
    }

    @Override
    public void GoBackwardForward(int exNum, String go) {
        getFragmentManager().beginTransaction().remove(pauseFragment).commit();
        if (go.equals("Back")) {
            changeExercise(exNum);
            timeCounter.cancel();
            timeCounter = new TotalTimeCounter(remainingDuration.get(exNum), 1000);
            timeCounter.start();
        } else if (go.equals("Ahead")) {
            forwardCount++;
            timeCounter.cancel();
            exNum++;
            if(exNum != workoutExerciseData.size()){
                timeCounter = new TotalTimeCounter(remainingDuration.get(exNum), 1000);
                timeCounter.start();
                changeExercise(exNum);
            } else {
                timer.onFinish();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                AppApplication.getInstance().trackEvent("Actions", "BackPress", "userID:" + userID + " Activity: WorkoutActivity");
                onStop();
                startActivity(new Intent(this, MainActivity.class));
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void exercisePlayPause(Boolean isPaused, int ex_num) {
        if (!isPaused) {
            totalTimerOnPauseMillis = totalMillis;
            counterOnPauseMillis = Counter.millis;
            timer.cancel();
            if (instruction != null) {
                if (!isPlayerReleased && instruction.isPlaying()) {
                    instruction.pause();
                    audioPaused = true;
                }
            }
            if (exerciseGif != null) {
                exerciseGif.pause();
            }
            TotalTimerStartStop(totalTimerOnPauseMillis, isPaused);
            PassExerciseNum(ex_num, isPaused);
            this.isPaused = true;
        } else {
            TotalTimerStartStop(totalTimerOnPauseMillis, this.isPaused);
            timer = new Counter(this, ex_num, workoutExerciseData.get(ex_num).get(0), counterOnPauseMillis,  Integer.valueOf(workoutExerciseData.get(ex_num).get(1)));
            timer.start();
            if (audioPaused && !isPlayerReleased) {
                try {
                    instruction.start();
                    audioPaused = false;
                } catch (NullPointerException npe) {
                    AppApplication.getInstance().trackException(npe);
                }
            }

            if (exerciseGif != null) {
                exerciseGif.start();
            }
            this.isPaused = false;
        }
    }

    @Override
    public void activateOnTrigger(int triggerNum) {
        if (!isPlayerReleased && instruction != null) {
            instruction.release();
            isPlayerReleased = true;
        }
        resID = getResources().getIdentifier(audioList.get(triggerNum), "raw", getPackageName());
        instruction = MediaPlayer.create(this, resID);
        instruction.start();
        instruction.setOnCompletionListener(this);
        isPlayerReleased = false;

        if (triggerNum == 1 && !workoutExerciseData.get(workoutExerciseNum).get(0).equals("rest")) {
            dpDuration.setMax(Integer.valueOf(workoutExerciseData.get(workoutExerciseNum).get(1)) - 10);
            dpDuration.setProgress(0);
            resID = getResources().getIdentifier(workoutExerciseData.get(workoutExerciseNum).get(0), "drawable", getPackageName());
            try {
                exerciseGif = new GifDrawable(getResources(), resID);
                ivGifView.setImageDrawable(exerciseGif);
                exerciseGif.start();
            } catch (IOException e) {
                AppApplication.getInstance().trackException(e);
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus && getFragmentManager().findFragmentByTag("pause") == null && timer != null) {
            //so that this is not called when the alertdialog is activated
            if(workoutExerciseNum != workoutExerciseData.size()) {
                AppApplication.getInstance().trackEvent("Actions", "FocusChanged", "userID:" + userID + " Activity: WorkoutActivity");
            }
            exercisePlayPause(isPaused, workoutExerciseNum);
            isPaused = !isPaused;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("CCC", "onpause");
        if (getFragmentManager().findFragmentByTag("pause") == null && timer != null) {
            if(workoutExerciseNum != workoutExerciseData.size()){
                exercisePlayPause(isPaused, workoutExerciseNum);
                isPaused = !isPaused;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //invalidate the DialogFragment to avoid memory leak
        if (alertFragment != null) {
            if (alertFragment .isVisible()) {
                alertFragment .dismiss();
            }
            alertFragment = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(timer != null){
            timer.cancel();
        }
        if(timeCounter != null){
            timeCounter.cancel();
        }
        if (!isPlayerReleased && instruction != null) {
            instruction.stop();
            instruction.release();
            isPlayerReleased = true;
            instruction = null;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.stop();
        mp.release();
        isPlayerReleased = true;
    }

    public class TotalTimeCounter extends CountDownTimer {
        private long millisInFuture;
        public TotalTimeCounter(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
            this.millisInFuture = millisInFuture;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            if((int)(millisUntilFinished/1000) != (int)(millisInFuture/1000)) {
                dpDuration.setProgress(dpDuration.getProgress() + 1);
            }
            totalMillis = millisUntilFinished;
            String total_hms;
            if(totalTime > 3600000L) {
                total_hms = String.format(Locale.ENGLISH, "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(totalMillis),
                        TimeUnit.MILLISECONDS.toMinutes(totalMillis) - TimeUnit.HOURS.toMinutes(TimeUnit.
                                MILLISECONDS.toHours(totalMillis)), TimeUnit.MILLISECONDS.toSeconds(totalMillis) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalMillis)));
            } else {
                total_hms = String.format(Locale.ENGLISH, "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(totalMillis),
                        TimeUnit.MILLISECONDS.toSeconds(totalMillis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalMillis)));
            }
            totalWorkoutTimer.setText(total_hms.replace(":", "%"));
        }

        @Override
        public void onFinish() {

        }
    }

    public static class WorkoutOverAlertFragment extends DialogFragment{
        private Activity workoutActivity;

        @Override
        public void onAttach(Activity activity) {
            if (activity instanceof WorkoutActivity) {
                workoutActivity = activity;
            }
            super.onAttach(activity);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            Boolean tooManyForwards = true;
            if(args != null) {
                tooManyForwards = args.getBoolean("tooManyForwards");
            }
            String title  = getString(R.string.aworkout_incompleteTitle);
            String message = getString(R.string.aworkout_incomplete);
            if(!tooManyForwards){
                title = getString(R.string.congratulations);
                message = getString(R.string.aworkout_str_over);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(workoutActivity);
            builder.setTitle(title)
                   .setMessage(message)
                   .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                    startActivity(new Intent(workoutActivity, MainActivity.class));
                                }
                            }
                   );

            final AlertDialog workoutOverDialog = builder.create();
            workoutOverDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    Button btnPositive = workoutOverDialog.getButton(Dialog.BUTTON_POSITIVE);
                    btnPositive.setTypeface(AppApplication.engFont);

                    Button btnNegative = workoutOverDialog.getButton(Dialog.BUTTON_NEGATIVE);
                    btnNegative.setEnabled(false);
                    btnNegative.setVisibility(View.GONE);
                }
            });

            return workoutOverDialog;
        }
    }
}
