package in.jiyofit.basic_app;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.DonutProgress;
import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class HistoryActivity extends BaseActivity {
    private DBAdapter dbAdapter;
    private SharedPreferences workoutPrefs;
    private DonutProgress dpStepsHistory;
    private static String TAG = "CCC";
    private TextView tvDate, tvWalkTgt, tvSteps, tvWorkoutCompletion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.activity_history, contentFrameLayout);

        dbAdapter = new DBAdapter(this);
        workoutPrefs = getSharedPreferences("Workout", MODE_PRIVATE);

        dpStepsHistory = (DonutProgress) findViewById(R.id.ahistory_dp);
        dpStepsHistory.setFinishedStrokeWidth(50);
        dpStepsHistory.setUnfinishedStrokeWidth(30);
        dpStepsHistory.setFinishedStrokeColor(Color.rgb(127,21,21));

        tvDate = (TextView) findViewById(R.id.ahistory_tv_date);
        tvWalkTgt = (TextView) findViewById(R.id.ahistory_tv_target);
        tvSteps = (TextView) findViewById(R.id.ahistory_tv_steps);
        tvWorkoutCompletion = (TextView) findViewById(R.id.ahistory_tv_completion);
        tvDate.setTypeface(AppApplication.hindiFont);
        tvWalkTgt.setTypeface(AppApplication.hindiFont);
        tvSteps.setTypeface(AppApplication.hindiFont);
        tvWorkoutCompletion.setTypeface(AppApplication.hindiFont);

        final CaldroidFragment caldroidFragment = new CaldroidFragment();
        Bundle args = new Bundle();
        args.putInt(CaldroidFragment.START_DAY_OF_WEEK, CaldroidFragment.MONDAY);
        caldroidFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().replace(R.id.ahistory_frl_calcontainer, caldroidFragment).commit();

        final ColorDrawable lightPositive = new ColorDrawable(ContextCompat.getColor(this, R.color.lightPositive));
        final ColorDrawable lightNegative = new ColorDrawable(ContextCompat.getColor(this, R.color.lightNegative));
        final ColorDrawable lightYellow = new ColorDrawable(ContextCompat.getColor(this, R.color.lightYellow));
        final ColorDrawable lightBlue = new ColorDrawable(ContextCompat.getColor(this, R.color.lightBlue));

        Log.d(TAG, "All: " + dbAdapter.getAll().toString());

        caldroidFragment.setMaxDate(new Date());
        SharedPreferences loginPrefs = getSharedPreferences("LoginInfo", MODE_PRIVATE);
        Long firstOpenTime = loginPrefs.getLong("firstOpenTime",System.currentTimeMillis());
        caldroidFragment.setMinDate(new Date(firstOpenTime));
        final CaldroidListener listener = new CaldroidListener() {

            @Override
            public void onChangeMonth(int month, int year) {
                super.onChangeMonth(month, year);

                Calendar cal = new GregorianCalendar();
                cal.set(year, month - 1, 1);
                int daysCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

                for (int i = 1; i <= daysCount; i++) {
                    String strMonth = String.valueOf(month);
                    String strDay = String.valueOf(i);
                    if(month < 10){
                        strMonth = "0" + String.valueOf(month);
                    }
                    if(i < 10){
                        strDay = "0" + String.valueOf(i);
                    }
                    String dateString = strDay + "-" + strMonth + "-" + String.valueOf(year);
                    String dayHistory = "";
                    if(dbAdapter.checkDateExists(dateString) && !dateString.equals(AppApplication.dateFormat.format(new Date()))){
                        dayHistory = dbAdapter.getDayHistory(dateString);
                    }
                    switch (dayHistory) {
                        case "00":
                            caldroidFragment.setBackgroundDrawableForDate(lightNegative, ParseDate(dateString));
                            break;
                        case "10":
                            caldroidFragment.setBackgroundDrawableForDate(lightBlue, ParseDate(dateString));
                            break;
                        case "11":
                            caldroidFragment.setBackgroundDrawableForDate(lightPositive, ParseDate(dateString));
                            break;
                        case "01":
                            caldroidFragment.setBackgroundDrawableForDate(lightYellow, ParseDate(dateString));
                            break;
                        default:
                            break;
                    }
                    caldroidFragment.refreshView();
                }
            }

            @Override
            public void onSelectDate(Date date, View view) {
                Log.d(TAG, "came here");
                String clickedDate = AppApplication.dateFormat.format(date);
                tvDate.setText(getString(R.string.date) + " " + clickedDate);
                if(dbAdapter.checkDateExists(clickedDate)){
                    if(dpStepsHistory.getVisibility() == View.INVISIBLE){
                        dpStepsHistory.setVisibility(View.VISIBLE);
                    }
                    dpStepsHistory.setFinishedStrokeColor(Color.rgb(127,21,21));
                    ArrayList<Integer> dayData = dbAdapter.getDayData(clickedDate);
                    if(dayData.get(0) == 1) {
                        tvWorkoutCompletion.setText(getString(R.string.ahistory_completion) + " " + getString(R.string.yes));
                    } else {
                        tvWorkoutCompletion.setText(getString(R.string.ahistory_completion) + " " + getString(R.string.no));
                    }

                    if(!AppApplication.dateFormat.format(date).equals(AppApplication.dateFormat.format(new Date()))){
                        dpStepsHistory.setMax(Integer.valueOf(dayData.get(4)));
                        Log.d(TAG, dpStepsHistory.getMax() + " Max");
                        dpStepsHistory.setProgress(Math.min(Integer.valueOf(dayData.get(3)),Integer.valueOf(dayData.get(4))));
                        Log.d(TAG, dpStepsHistory.getProgress() + " progress");
                        tvSteps.setText(dayData.get(3) + " " + getString(R.string.steps));
                        tvWalkTgt.setText(getString(R.string.target) + " " + dayData.get(4) + " " + getString(R.string.steps));
                        if(Integer.valueOf(dayData.get(3)) > Integer.valueOf(dayData.get(4))){
                            dpStepsHistory.setFinishedStrokeColor(ContextCompat.getColor(HistoryActivity.this, R.color.positive));
                        }
                    } else {
                        MainActivity.showStoredSteps(HistoryActivity.this, dpStepsHistory, tvSteps);
                        Log.d(TAG, "p: " + dpStepsHistory.getProgress());
                        Log.d(TAG, "p: " + dpStepsHistory.getProgress());
                        Log.d(TAG, "M: " + dpStepsHistory.getMax());
                        tvWalkTgt.setText(getString(R.string.target) + " " + workoutPrefs.getInt("stepsTarget", MainActivity.defaultTarget) + " " + getString(R.string.steps));
                    }
                } else if (date.getTime() < System.currentTimeMillis()){
                    dpStepsHistory.setVisibility(View.INVISIBLE);
                    tvSteps.setText(getString(R.string.ahistory_noapp));
                    tvWalkTgt.setText("");
                    tvWorkoutCompletion.setText("");
                } else {
                    dpStepsHistory.setVisibility(View.INVISIBLE);
                    tvSteps.setText(getString(R.string.ahistory_future));
                    tvWalkTgt.setText("");
                    tvWorkoutCompletion.setText("");
                }
            }

            @Override
            public void onCaldroidViewCreated() {
                super.onCaldroidViewCreated();
                caldroidFragment.getMonthTitleTextView().setTypeface(AppApplication.engFont);
            }
        };
        caldroidFragment.setCaldroidListener(listener);
        caldroidFragment.refreshView();
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Long dayOnOpen = Math.max(firstOpenTime, cal.getTimeInMillis());
        //caldroidFragment.setSelectedDate(date) doesn't work
        caldroidFragment.getCaldroidListener().onSelectDate(new Date(dayOnOpen), caldroidFragment.getView());
        Log.d(TAG, "dpP: " + dpStepsHistory.getProgress());
        Log.d(TAG, "dpM: " + dpStepsHistory.getMax());
    }

    public Date ParseDate(String date_str) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        Date dateStr = null;
        try {
            dateStr = formatter.parse(date_str);
        } catch (ParseException e) {
            AppApplication.getInstance().trackException(e);
        }
        return dateStr;
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppApplication.getInstance().trackScreenView("HistoryActivity");
    }
}
