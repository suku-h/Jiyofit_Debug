package in.jiyofit.basic_app;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

public class TipsFragment extends Fragment {
    private ArrayList<String> tip;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DBAdapter dbAdapter = new DBAdapter(getActivity());

        SharedPreferences loginPrefs = getActivity().getSharedPreferences("LoginInfo", Context.MODE_PRIVATE);
        String userID = loginPrefs.getString("userID","");

        Bundle args = getArguments();
        if(args==null){
            ArrayList<String> tipIDs = dbAdapter.getTipIDs();
            Random r = new Random();
            int tipNum = r.nextInt(tipIDs.size());
            tip = dbAdapter.getTip(tipIDs.get(tipNum));
        } else {
            String tipID = args.getString("tipID");
            AppApplication.getInstance().trackEvent("AppOpen", "Tips", "userID: " + userID + " TipID: " + tipID);
            tip = dbAdapter.getTip(tipID);
        }
        return inflater.inflate(R.layout.fragment_tips, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvType = (TextView) getActivity().findViewById(R.id.ftips_tv_type);
        TextView tvTip = (TextView) getActivity().findViewById(R.id.ftips_tv_tip);
        TextView tvTipText = (TextView) getActivity().findViewById(R.id.ftips_tv_text);
        tvType.setText(tip.get(1));
        tvTip.setText(tip.get(2));
        tvTipText.setText(tip.get(3));

        ImageView ivIcon = (ImageView) getActivity().findViewById(R.id.ftips_iv_icon);
        int resID = getActivity().getResources().getIdentifier(tip.get(4), "drawable", getActivity().getPackageName());
        ivIcon.setImageResource(resID);
    }
}
