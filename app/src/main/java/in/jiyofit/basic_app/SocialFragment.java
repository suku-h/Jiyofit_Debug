package in.jiyofit.basic_app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class SocialFragment extends DialogFragment {
    private static String userID;
    private String socialApp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null && args.getString("socialApp") != null) {
            socialApp = args.getString("socialApp");
        } else {
            socialApp = getString(R.string.whatsapp);
        }
    }

    public Dialog onCreateDialog(Bundle SavedInstanceState) {
        final SharedPreferences loginPrefs = getActivity().getSharedPreferences("LoginInfo", Context.MODE_PRIVATE);
        userID = loginPrefs.getString("userID", "");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View titleView = inflater.inflate(R.layout.view_dialog_title, null);
        TextView tvTiltle = (TextView) titleView.findViewById(R.id.vdialogtitle_tv);
        tvTiltle.setTextSize(24);
        tvTiltle.setText(socialApp);
        tvTiltle.setTypeface(AppApplication.engFont);
        ImageView ivTitleLogo = (ImageView) titleView.findViewById(R.id.vdialogtitle_iv);
        ivTitleLogo.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.whatsapp_icon));
        builder.setCustomTitle(titleView);

        View v = inflater.inflate(R.layout.fragment_social, (ViewGroup) getView(), false);
        final EditText etMessage = (EditText) v.findViewById(R.id.fsocial_et_message);
        etMessage.setText("Testing 123");
        etMessage.setTypeface(AppApplication.engFont);
        etMessage.setTextSize(18);
        builder.setView(v);

        builder.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strMessage = etMessage.getText().toString();
                if (strMessage.length() != 0) {
                    AppApplication.getInstance().trackEvent("Actions", "Social", "userID: " + userID + "Whatsapp Message: " + strMessage);
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, strMessage);
                    sendIntent.setType("text/plain");
                    sendIntent.setPackage("com.whatsapp");
                    startActivity(sendIntent);
                    dialog.dismiss();
                } else {
                    AppApplication.toastIt(getActivity(), R.string.fsocial_nomessage);
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

        AppApplication.getInstance().trackScreenView("SocialFragment");
        return feedbackAlert;
    }
}
