package in.jiyofit.basic_app;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.CountDownTimer;

public class Counter extends CountDownTimer {
    private final Context ctx;
    private int exNum, i;
    private final ChangeExercise change;
    private final Long exerciseDuration;
    private Long[] triggerTimesList;
    static Long millis;
    private Long preExerciseDeductor;
    private final MediaPlayer beep_long;
    private final MediaPlayer beep_small;
    private static String TAG = "CCC";

    public Counter(Context ctx, int exNum, String exName, Long millisInFuture, int totalDuration) {
        super(millisInFuture, 1000);
        this.ctx = ctx;
        this.exNum = exNum;
        exerciseDuration = (long)(totalDuration * 1000);
        preExerciseDeductor = exerciseDuration - 10000L;
        change = (ChangeExercise) ctx;

        switch (totalDuration){
            case 40:
                triggerTimesList = new Long[]{10L, 20L, 30L};
                break;
            case 30:
                triggerTimesList = new Long[]{10L, 20L};
                break;
            case 60:
                triggerTimesList = new Long[]{10L, 30L, 45L};
                break;
            default:
                triggerTimesList = new Long[]{10L, Math.max(Math.round(((long)totalDuration - 10L) / 3) + 10L, 20L), Math.max(Math.round(((long)totalDuration - 10L) * 2 / 3) + 10L, 30L)};
        }

        i = 0;
        while(i < triggerTimesList.length && triggerTimesList[triggerTimesList.length - i - 1] * 1000 > millisInFuture){
            i++;
        }
        if (!exName.equals("rest") && millisInFuture < preExerciseDeductor) {
            preExerciseDeductor = 0L;
        }
        if(exName.equals("rest")) {
            preExerciseDeductor = 0L;
        }
        beep_long = MediaPlayer.create(ctx, R.raw.beep_long);
        beep_small = MediaPlayer.create(ctx, R.raw.beep_small);
    }

    @Override
    public void onTick(long millisUntilFinished) {
        millis = millisUntilFinished;
        int shownMillis = Math.round((millisUntilFinished - preExerciseDeductor)/ 1000);
        if (shownMillis == 3 || shownMillis == 2 || shownMillis == 1) {
            beep_small.start();
        }

        if (shownMillis == 0 && preExerciseDeductor == exerciseDuration - 10000L) {
            beep_long.start();
            preExerciseDeductor = 0L;
        }

        if(i < triggerTimesList.length ){
            if (Math.ceil((exerciseDuration - millis) / 1000) == triggerTimesList[i])  {
                change.activateOnTrigger(i + 1);
                i++;
            }
        }
    }

    @Override
    public void onFinish() {
        MediaPlayer beep_long = MediaPlayer.create(ctx, R.raw.beep_long);
        beep_long.start();
        beep_long.release();
        if(beep_small != null) beep_small.release();
        exNum++;
        change.changeExercise(exNum);
    }
}
