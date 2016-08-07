package in.jiyofit.basic_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SettingsActivity extends BaseActivity implements AdapterView.OnItemSelectedListener{
    private Spinner ddFeet, ddInches, ddWorkoutType;
    private String sAge, sWeight;
    private static String userID;
    private EditText etAge, etWeight, etTarget;
    private RadioButton rb10, rb20;
    private int feetPos, inchesPos, goalPos, check, recoSteps = 4000, errorColor;
    private static String TAG = "CCC";
    private SharedPreferences profilePrefs, loginPrefs, workoutPrefs;
    private SharedPreferences.Editor workoutEditor, profileEditor;
    private TextView tvAge, tvWeight, tvBMI, tvCalculatedBMI, tvTarget, tvRecoTarget, tvDuration, tvCalculatedFat;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.activity_settings, contentFrameLayout);

        loginPrefs = getSharedPreferences("LoginInfo", MODE_PRIVATE);
        profilePrefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        workoutPrefs = getSharedPreferences("Workout", MODE_PRIVATE);

        userID = loginPrefs.getString("userID","");

        errorColor = ContextCompat.getColor(this, R.color.lightNegative);
        ddFeet = (Spinner) findViewById(R.id.asettings_dd_feet);
        ddInches = (Spinner) findViewById(R.id.asettings_dd_inches);
        ddWorkoutType = (Spinner) findViewById(R.id.asettings_dd_workouttype);
        ddFeet.setAdapter(new SpinnerAdapter(this, R.array.feet, AppApplication.engFont));
        ddFeet.setOnItemSelectedListener(this);
        ddFeet.setSelection(profilePrefs.getInt("feetPos", 0));
        ddInches.setAdapter(new SpinnerAdapter(this, R.array.inches, AppApplication.engFont));
        ddInches.setOnItemSelectedListener(this);
        ddInches.setSelection(profilePrefs.getInt("inchesPos", 0));
        ddWorkoutType.setAdapter(new SpinnerAdapter(this, R.array.workouttype, AppApplication.hindiFont));
        ddWorkoutType.setOnItemSelectedListener(this);
        goalPos = workoutPrefs.getInt("workoutType", 1);
        ddWorkoutType.setSelection(goalPos);

        tvAge = (TextView) findViewById(R.id.asettings_tv_age);
        tvWeight = (TextView) findViewById(R.id.asettings_tv_weight);
        tvBMI = (TextView) findViewById(R.id.asettings_tv_bmi);
        tvCalculatedBMI = (TextView) findViewById(R.id.asettings_tv_calculatedBMI);
        tvTarget = (TextView) findViewById(R.id.asettings_tv_target);
        tvRecoTarget = (TextView) findViewById(R.id.asettings_tv_recotarget);
        tvDuration = (TextView) findViewById(R.id.asettings_tv_duration);
        tvCalculatedFat = (TextView) findViewById(R.id.asettings_tv_calculatedFat);
        tvAge.setText(getString(R.string.age) + Character.toString((char)188) + getString(R.string.unit_year) + Character.toString((char)189) + "%");
        tvWeight.setText(getString(R.string.weight) + Character.toString((char)188) + getString(R.string.unit_kg) + Character.toString((char)189) + "%");
        tvTarget.setText("y{; " + Character.toString((char)188) + getString(R.string.steps) + Character.toString((char)189) + "%");
        tvDuration.setText(getString(R.string.exercise) + Character.toString((char)188) + getString(R.string.unit_minute) + Character.toString((char)189) + "%");

        Button btnCalculate = (Button) findViewById(R.id.asettings_btn_calculate);
        btnCalculate.setTypeface(AppApplication.engFont);
        btnCalculate.setTextSize(18);
        tvBMI.setTextSize(18);
        tvBMI.setTypeface(AppApplication.engFont);

        Button btnSave = (Button) findViewById(R.id.asettings_btn_submit);
        btnSave.setTypeface(AppApplication.engFont);
        btnSave.setTextSize(18);

        etAge = (EditText) findViewById(R.id.asettings_et_age);
        etWeight = (EditText) findViewById(R.id.asettings_et_weight);
        etTarget = (EditText) findViewById(R.id.asettings_et_target);
        
        if(profilePrefs.getInt("age", 0) != 0){
            etAge.setText(String.valueOf(profilePrefs.getInt("age", 0)));
        }
        if(profilePrefs.getInt("weight", 0) != 0){
            etWeight.setText(String.valueOf(profilePrefs.getInt("weight", 0)));
        }

        CheckBox cbWalkReminder = (CheckBox) findViewById(R.id.asettings_cb_walkreminder);
        cbWalkReminder.setChecked(workoutPrefs.getBoolean("walkReminderIsNeeded", true));
        cbWalkReminder.setAllCaps(false);
        cbWalkReminder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                workoutEditor = workoutPrefs.edit();
                if(isChecked){
                    Log.d(TAG, "checked");
                    workoutEditor.putBoolean("walkReminderIsNeeded", true);
                } else {
                    Log.d(TAG, "unchecked");
                    workoutEditor.putBoolean("walkReminderIsNeeded", false);
                }
                workoutEditor.commit();
                AppApplication.getInstance().trackEvent("Settings", "Reminder", "userID: " + userID + " WalkReminder: " + workoutPrefs.getBoolean("walkReminderIsNeeded", true));
            }
        });

        RadioGroup rgDuration = (RadioGroup) findViewById(R.id.asettings_rg);
        rb10 = (RadioButton) findViewById(R.id.asettings_rb_10);
        rb20 = (RadioButton) findViewById(R.id.asettings_rb_20);
        if (workoutPrefs.getInt("workoutDuration", 10)==20) {
            rgDuration.check(rb20.getId());
        } else {
            rgDuration.check(rb10.getId());
        }

        rgDuration.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                workoutEditor = workoutPrefs.edit();
                if(checkedId == rb10.getId()){
                    workoutEditor.putInt("workoutDuration", 10);
                } else {
                    workoutEditor.putInt("workoutDuration", 20);
                }
                workoutEditor.commit();
                AppApplication.getInstance().trackEvent("Settings", "Duration", "userID: " + userID + " WorkoutDuration: " + workoutPrefs.getInt("workoutDuration", 10));
            }
        });
    }

    public void calculateDetails(View v){
        check = 0;
        sAge = etAge.getText().toString();
        sWeight = etWeight.getText().toString();
        checkEditTextEmpty();
        checkSpinnerEmpty();
        if(check == 1){
            AppApplication.toastIt(this, R.string.asettings_fillformrequest);
        } else{
            double heightInMeters = (((feetPos + 3) * 12 + (inchesPos - 1)) * 2.54)/100;
            int weight = Integer.valueOf(sWeight);
            float bmi = (float) Math.round((weight / Math.pow(heightInMeters,2) * 10))/10;
            int age = Integer.valueOf(sAge);
            if(weight != profilePrefs.getInt("weight", 0)){
                AppApplication.getInstance().trackEvent("Settings", "Profile", "userID: " + userID + " Weight: " + weight + " Age: " + age);
            }
            profileEditor = profilePrefs.edit();
            profileEditor.putInt("age", age);
            profileEditor.putInt("weight", weight);
            profileEditor.commit();

            String gender = loginPrefs.getString("gender", "male");
            int recoWeightLoss = (int) Math.round(weight - 24 * Math.pow(heightInMeters,2));
            String strCalculatedBMI;
            if(bmi > 24){
                strCalculatedBMI = bmi + " " + Character.toString((char)188) + recoWeightLoss + " " + getString(R.string.unit_kg) + " " + getString(R.string.more) + Character.toString((char)189);
            } else if (bmi < 19){
                recoWeightLoss = (int) Math.round(19 * Math.pow(heightInMeters,2) - weight);
                strCalculatedBMI = bmi + " " + Character.toString((char)188) + recoWeightLoss + " " + getString(R.string.unit_kg) + " " + getString(R.string.increase) + Character.toString((char)189);
            } else {
                strCalculatedBMI = bmi + " " + Character.toString((char)188) + getString(R.string.weight) + " " + getString(R.string.proper) + Character.toString((char)189);
            }
            tvCalculatedBMI.setText(strCalculatedBMI.replace(".","-"));

            //http://halls.md/race-body-fat-percentage/
            int fatPercent, fatMin;
            if(gender.equals("male")){
                fatMin = 10;
                if(age > 18){
                    fatPercent = (int) Math.ceil((1.29 * bmi) + (0.2 * age) - 19.4);
                } else {
                    fatPercent = (int) Math.ceil((1.51 * bmi) - (0.7 * age) - 2.2);
                }
                if(age < 39){
                    if(fatPercent <= 7){
                        tvCalculatedFat.setText(fatPercent + " "  + getString(R.string.unit_percent) + " " + Character.toString((char)188) + getString(R.string.underfat) + "A 12" + " " + getString(R.string.unit_percent) + " " + getString(R.string.shouldbe)+ Character.toString((char)189));
                    } else if (fatPercent > 7 && fatPercent <= 16){
                        tvCalculatedFat.setText(fatPercent + " "  + getString(R.string.unit_percent) + " " + Character.toString((char)188) + getString(R.string.fit) + Character.toString((char)189));
                    } else if (fatPercent > 16 && fatPercent <= 20){
                        tvCalculatedFat.setText(fatPercent + " "  + getString(R.string.unit_percent) + " " + Character.toString((char)188) + getString(R.string.healthy) + "A 12" + " " + getString(R.string.unit_percent) + " " + getString(R.string.shouldbe) + Character.toString((char)189));
                    } else if (fatPercent > 20 && fatPercent <= 25){
                        tvCalculatedFat.setText(fatPercent + " "  + getString(R.string.unit_percent) + " " + Character.toString((char)188) + getString(R.string.overweight) + Character.toString((char)189));
                    }else {
                        tvCalculatedFat.setText(fatPercent + " "  + getString(R.string.unit_percent) + " " + Character.toString((char)188) + getString(R.string.obese) + Character.toString((char)189));
                    }
                } else if (age > 39){
                    if(fatPercent <= 10){
                        tvCalculatedFat.setText(fatPercent + " "  + getString(R.string.unit_percent) + " " + Character.toString((char)188) + getString(R.string.underfat) + "A 14" + " " + getString(R.string.unit_percent) + " " + getString(R.string.shouldbe) + Character.toString((char)189));
                    } else if (fatPercent > 10 && fatPercent <= 18){
                        tvCalculatedFat.setText(fatPercent + " "  + getString(R.string.unit_percent) + " " + Character.toString((char)188) + getString(R.string.fit) + Character.toString((char)189));
                    } else if (fatPercent > 18 && fatPercent <= 22){
                        tvCalculatedFat.setText(fatPercent + " "  + getString(R.string.unit_percent) + " " + Character.toString((char)188) + getString(R.string.healthy) + "A 14" + " " + getString(R.string.unit_percent) + " " + getString(R.string.shouldbe) + Character.toString((char)189));
                    } else if (fatPercent > 22 && fatPercent <= 28){
                        tvCalculatedFat.setText(fatPercent + " "  + getString(R.string.unit_percent) + " " + Character.toString((char)188) + getString(R.string.overweight) + Character.toString((char)189));
                    }else {
                        tvCalculatedFat.setText(fatPercent + " "  + getString(R.string.unit_percent) + " " + Character.toString((char)188) + getString(R.string.obese) + Character.toString((char)189));
                    }
                }
            } else {
                fatMin = 20;
                if(age > 18){
                    fatPercent = (int) Math.ceil((1.29 * bmi) + (0.20 * age) - 8);
                } else {
                    fatPercent = (int) Math.ceil((1.51 * bmi) - (0.7 * age) + 1.4);
                }
                if(age < 39){
                    if(fatPercent <= 20){
                        tvCalculatedFat.setText(fatPercent + Character.toString((char)37) + " " + Character.toString((char)188) + getString(R.string.underfat) + ", 24" + Character.toString((char)37) + " " + getString(R.string.shouldbe) + Character.toString((char)189));
                    } else if (fatPercent > 20 && fatPercent <= 28){
                        tvCalculatedFat.setText(fatPercent + Character.toString((char)37) + " " + Character.toString((char)188) + getString(R.string.fit) + Character.toString((char)189));
                    } else if (fatPercent > 28 && fatPercent <= 33){
                        tvCalculatedFat.setText(fatPercent + Character.toString((char)37) + " " + Character.toString((char)188) + getString(R.string.healthy) + ", 24" + Character.toString((char)37) + " " + getString(R.string.shouldbe) + Character.toString((char)189));
                    } else if (fatPercent > 33 && fatPercent <= 39){
                        tvCalculatedFat.setText(fatPercent + Character.toString((char)37) + " " + Character.toString((char)188) + getString(R.string.overweight) + Character.toString((char)189));
                    }else {
                        tvCalculatedFat.setText(fatPercent + Character.toString((char)37) + " " + Character.toString((char)188) + getString(R.string.obese) + Character.toString((char)189));
                    }
                } else if (age > 39){
                    if(fatPercent <= 22){
                        tvCalculatedFat.setText(fatPercent + Character.toString((char)37) + " " + Character.toString((char)188) + getString(R.string.underfat) + ", 26" + Character.toString((char)37) + " " + getString(R.string.shouldbe) + Character.toString((char)189));
                    } else if (fatPercent > 22 && fatPercent <= 30){
                        tvCalculatedFat.setText(fatPercent + Character.toString((char)37) + " " + Character.toString((char)188) + getString(R.string.fit) + Character.toString((char)189));
                    } else if (fatPercent > 30 && fatPercent <= 34){
                        tvCalculatedFat.setText(fatPercent + Character.toString((char)37) + " " + Character.toString((char)188) + getString(R.string.healthy) + ", 26" + Character.toString((char)37) + " " + getString(R.string.shouldbe) + Character.toString((char)189));
                    } else if (fatPercent > 34 && fatPercent <= 40){
                        tvCalculatedFat.setText(fatPercent + Character.toString((char)37) + " " + Character.toString((char)188) + getString(R.string.overweight) + Character.toString((char)189));
                    }else {
                        tvCalculatedFat.setText(fatPercent + Character.toString((char)37) + " " + Character.toString((char)188) + getString(R.string.obese) + Character.toString((char)189));
                    }
                }
            }

            if(fatPercent != profilePrefs.getInt("fatPercent", 20)){
                AppApplication.getInstance().trackEvent("Settings", "Fat%", "userID: " + userID + " Fat%: " + fatPercent);
                profileEditor = profilePrefs.edit();
                profileEditor.putInt("fatPercent", fatPercent);
                profileEditor.commit();
            }

            recoSteps = MainActivity.defaultTarget + (Math.max((fatPercent - fatMin), 0) * 6000 / 25);
            tvRecoTarget.setText(getString(R.string.reco) + "% " + recoSteps);
        }
    }

    public void saveDetails(View v){
        String strTarget = etTarget.getText().toString();
        etTarget.getBackground().clearColorFilter();
        check = 0;
        if(strTarget.length() == 0){
            strTarget = String.valueOf(recoSteps);
        }

        if(Integer.valueOf(strTarget) >= 1500){
            workoutEditor = workoutPrefs.edit();
            workoutEditor.putInt("stepsTarget", Integer.valueOf(strTarget));
            workoutEditor.commit();
            //so that only if it is not default target then record
            if(!strTarget.equals(String.valueOf(MainActivity.defaultTarget))) {
                AppApplication.getInstance().trackEvent("Settings", "Target", "userID: " + userID + " Target: " + strTarget);
            }

            if(workoutPrefs.getInt("workoutType", 1) != goalPos) {
                workoutEditor = workoutPrefs.edit();
                workoutEditor.putInt("workoutType", goalPos);
                workoutEditor.commit();
                AppApplication.getInstance().trackEvent("Settings", "WorkoutType", "userID: " + userID + " Position: " + goalPos);
            }

            AppApplication.toastIt(this, R.string.thanks);
            //hide the keypad
            InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
            startActivity(new Intent(this, MainActivity.class));
        } else {
            check = 1;
            etTarget.getBackground().setColorFilter(errorColor, PorterDuff.Mode.SRC_IN);
            AppApplication.toastIt(this, R.string.minimumSteps);
        }
    }

    public void checkEditTextEmpty(){
        //Here sAge gets checked for NPE also
        etAge.getBackground().clearColorFilter();
        etWeight.getBackground().clearColorFilter();
        if (sAge.length() == 0) {
            etAge.getBackground().setColorFilter(errorColor, PorterDuff.Mode.SRC_IN);
            check = 1;
        } else if (Integer.valueOf(sAge) > 70) {
            etAge.getBackground().setColorFilter(errorColor, PorterDuff.Mode.SRC_IN);
            check = 1;
            AppApplication.toastIt(this, R.string.asettings_maxage);
        } else if (Integer.valueOf(sAge) < 12) {
            etAge.getBackground().setColorFilter(errorColor, PorterDuff.Mode.SRC_IN);
            check = 1;
            AppApplication.toastIt(this, R.string.asettings_minage);
        }

        if (sWeight.length() == 0) {
            etWeight.getBackground().setColorFilter(errorColor, PorterDuff.Mode.SRC_IN);
            check = 1;
        } else if (Integer.valueOf(sWeight) > 150) {
            etWeight.getBackground().setColorFilter(errorColor, PorterDuff.Mode.SRC_IN);
            check = 1;
            AppApplication.toastIt(this, R.string.asettings_maxweight);
        } else if (Integer.valueOf(sWeight) < 40) {
            etWeight.getBackground().setColorFilter(errorColor, PorterDuff.Mode.SRC_IN);
            check = 1;
            AppApplication.toastIt(this, R.string.asettings_minweight);
        }
    }

    public void checkSpinnerEmpty() {
        ddFeet.getBackground().clearColorFilter();
        ddInches.getBackground().clearColorFilter();
        if (feetPos == 0) {
            ddFeet.getBackground().setColorFilter(errorColor, PorterDuff.Mode.SRC_IN);
            check = 1;
        }
        if (inchesPos == 0) {
            ddInches.getBackground().setColorFilter(errorColor, PorterDuff.Mode.SRC_IN);
            check = 1;
        }

        if(check != 1 && (profilePrefs.getInt("feetPos", 5) != feetPos || profilePrefs.getInt("inchesPos", 5) != inchesPos)) {
            profileEditor = profilePrefs.edit();
            profileEditor.putInt("feetPos", feetPos);
            profileEditor.putInt("inchesPos", inchesPos);
            profileEditor.commit();
            AppApplication.getInstance().trackEvent("Settings", "Height", "userID: " + userID + " feet: " + String.valueOf(feetPos + 3) + " inches: " + String.valueOf(inchesPos - 1));
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Spinner spinner = (Spinner) parent;

        if (spinner.getId() == R.id.asettings_dd_feet) {
            feetPos = position;
        }
        if (spinner.getId() == R.id.asettings_dd_inches) {
            inchesPos = position;
        }
        if (spinner.getId() == R.id.asettings_dd_workouttype) {
            goalPos = position;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
