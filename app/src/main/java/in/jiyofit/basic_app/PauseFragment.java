package in.jiyofit.basic_app;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

public class PauseFragment extends Fragment{
    private PauseCommunicator pausecomm;
    private int exNum;
    private GoBackwardForwardInterface goBackAhead;
    private static String userID;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pause, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences loginPrefs = getActivity().getSharedPreferences("LoginInfo", Context.MODE_PRIVATE);
        userID = loginPrefs.getString("userID","");

        ImageButton forward = (ImageButton) getActivity().findViewById(R.id.fpause_btn_forward);
        ImageButton backward = (ImageButton) getActivity().findViewById(R.id.fpause_btn_backward);

        Button btnLeaveSession = (Button) getActivity().findViewById(R.id.fpause_btn_leaveworkout);
        AppApplication.changeBtnColor(getActivity(), btnLeaveSession, R.color.negative);
        RelativeLayout pauseFragmentLayout = (RelativeLayout) getActivity().findViewById(R.id.fpause_layout);
        pausecomm = (PauseCommunicator) getActivity();
        goBackAhead = (GoBackwardForwardInterface) getActivity();

        View.OnClickListener skipThis = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackAhead.GoBackwardForward(exNum, "Ahead");
            }
        };
        View.OnClickListener goToPrevious = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackAhead.GoBackwardForward(exNum, "Back");
            }
        };
        View.OnClickListener leaveSession = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppApplication.getInstance().trackEvent("Workout", "quitExercise", "userID:" + userID + " quitExercise@" + exNum);
                startActivity(new Intent(getActivity(),MainActivity.class));
            }
        };

        forward.setOnClickListener(skipThis);
        backward.setOnClickListener(goToPrevious);
        btnLeaveSession.setOnClickListener(leaveSession);
        pauseFragmentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pausecomm.PassExerciseNum(exNum, true);
            }
        });

        //removes the screen_on when the workout is paused to save unnecessary battery consumption
        getActivity().getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void getExNum(int ex_num){
        exNum = ex_num;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
