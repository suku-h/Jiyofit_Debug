package in.jiyofit.basic_app;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;
import java.util.Locale;

public class FeedbackFragment extends DialogFragment {
    private static String userID;

    public Dialog onCreateDialog(Bundle SavedInstanceState) {
        final SharedPreferences loginPrefs = getActivity().getSharedPreferences("LoginInfo", Context.MODE_PRIVATE);
        userID = loginPrefs.getString("userID","");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View titleView = inflater.inflate(R.layout.view_dialog_title, null);
        TextView tvTiltle = (TextView) titleView.findViewById(R.id.vdialogtitle_tv);
        tvTiltle.setText(R.string.ffeedback_title);
        tvTiltle.setTypeface(AppApplication.hindiFont);
        tvTiltle.setTextSize(30);
        ImageView ivTitleLogo = (ImageView) titleView.findViewById(R.id.vdialogtitle_iv);
        ivTitleLogo.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.feedback));
        builder.setCustomTitle(titleView);

        View v = inflater.inflate(R.layout.fragment_feedback, (ViewGroup) getView(), false);
        TextView tvLike = (TextView) v.findViewById(R.id.ffeedback_tv_like);
        TextView tvSuggestion = (TextView) v.findViewById(R.id.ffeedback_tv_suggestion);
        TextView tvPhoneNumber = (TextView) v.findViewById(R.id.ffeedback_tv_phonenumber);
        tvLike.setTypeface(AppApplication.hindiFont);
        tvSuggestion.setTypeface(AppApplication.hindiFont);
        tvPhoneNumber.setTypeface(AppApplication.hindiFont);
        final EditText etLike = (EditText) v.findViewById(R.id.ffeedback_et_like);
        final EditText etSuggestion = (EditText) v.findViewById(R.id.ffeedback_et_suggestion);
        final EditText etPhoneNumber = (EditText) v.findViewById(R.id.ffeedback_et_phonenumber);

        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        InputMethodSubtype inputMethodSubtype = inputMethodManager.getCurrentInputMethodSubtype();
        //to prevent NPE for inputMethodSubtype
        final String keyboardLanguage;
        if(inputMethodSubtype != null) {
            String localeString = inputMethodSubtype.getLocale();
            Locale locale = new Locale(localeString);
            //to prevent StringsOutOfBoundsException when getDisplayLanguage return ""
            if (locale.getDisplayLanguage().length() >= 2) {
                keyboardLanguage = locale.getDisplayLanguage().substring(0, 2);
            } else {
                keyboardLanguage = "en";
            }
        } else {
            keyboardLanguage = "en";
        }

        if(loginPrefs.getString("keyboardLang", "").length() == 0){
            AppApplication.getInstance().trackEvent("UserInfo", "Keyboard", "userId: " + userID + " KeyboardLang: " + keyboardLanguage);
            SharedPreferences.Editor loginEditor = loginPrefs.edit();
            loginEditor.putString("keyboardLang", keyboardLanguage);
            loginEditor.commit();
        }

        final SharedPreferences profilePrefs = getActivity().getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        if(profilePrefs.getString("phoneNumber", "").length() != 0){
            tvPhoneNumber.setVisibility(View.GONE);
            etPhoneNumber.setVisibility(View.GONE);
        }

        //sometimes the keyboard lang is "En"
        if (keyboardLanguage.equalsIgnoreCase("en")) {
            etLike.setTypeface(AppApplication.engFont);
            etLike.setTextSize(18);
            etSuggestion.setTypeface(AppApplication.engFont);
            etSuggestion.setTextSize(18);
        }

        builder.setView(v);

        builder.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strLike = etLike.getText().toString();
                String strSuggestion = etSuggestion.getText().toString();
                String strPhoneNumber = etPhoneNumber.getText().toString();
                if (strLike.length() != 0 || strSuggestion.length() != 0) {
                    AppApplication.getInstance().trackEvent("Actions", "Feedback", "userID: " + userID + "Date: " + AppApplication.dateFormat.format(new Date()) + " Like: " + strLike + " Suggestion: " + strSuggestion);
                    AppApplication.toastIt(getActivity(), R.string.ffeedback_thanks);
                } else {
                    AppApplication.toastIt(getActivity(), R.string.ffeedback_nofeedback);
                }

                if(strPhoneNumber.length() == 10){
                    AppApplication.getInstance().trackEvent("UserInfo", "PhoneNumber", "userID: " + userID + "PhoneNumber: " + strPhoneNumber);
                    SharedPreferences.Editor profileEditor = profilePrefs.edit();
                    profileEditor.putString("phoneNumber", strPhoneNumber);
                    profileEditor.commit();
                    dialog.dismiss();
                } else if (strPhoneNumber.length() == 0){
                    dialog.dismiss();
                } else {
                    AppApplication.toastIt(getActivity(), R.string.ffeedback_incompletemobile);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog feedbackAlert = builder.create();
        feedbackAlert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button btnPositive = feedbackAlert.getButton(Dialog.BUTTON_POSITIVE);
                btnPositive.setTextSize(25);
                btnPositive.setAllCaps(false);
                btnPositive.setTypeface(AppApplication.hindiFont);
                Button btnNegative = feedbackAlert.getButton(Dialog.BUTTON_NEGATIVE);
                btnNegative.setTextSize(25);
                btnNegative.setAllCaps(false);
                btnNegative.setTypeface(AppApplication.hindiFont);
            }
        });

        AppApplication.getInstance().trackScreenView("FeedbackFragment");
        return feedbackAlert;
    }
}
