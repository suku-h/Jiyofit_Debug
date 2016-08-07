package in.jiyofit.basic_app;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import java.util.ArrayList;
import java.util.Random;

public class ArticleFragment extends Fragment {
    private SharedPreferences userActionPrefs;
    private SharedPreferences.Editor userActionEditor;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_article, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences loginPrefs = getActivity().getSharedPreferences("LoginInfo", Context.MODE_PRIVATE);
        final String userID = loginPrefs.getString("userID","");
        VolleySingleton volleySingleton = VolleySingleton.getInstance();
        ImageLoader imageLoader = volleySingleton.getImageLoader();

        DBAdapter dbAdapter = new DBAdapter(getActivity());
        ArrayList<String> articleIDs = dbAdapter.getArticleIDs(loginPrefs.getString("gender", "male"));
        Random r = new Random();
        int articleNum = r.nextInt(articleIDs.size());
        final ArrayList<String> article = dbAdapter.getArticle(articleIDs.get(articleNum));

        final ImageView ivSnippet = (ImageView) getActivity().findViewById(R.id.farticle_iv_snippet);

        String imageURL = article.get(2);
        imageLoader.get(imageURL, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                ivSnippet.setImageBitmap(response.getBitmap());
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                startActivity(new Intent(getActivity(), MainActivity.class));
            }
        });

        userActionPrefs = getActivity().getSharedPreferences("UserActions", Context.MODE_PRIVATE);
        userActionEditor = userActionPrefs.edit();
        userActionEditor.putInt("ArticleShown", userActionPrefs.getInt("ArticleShown", 0) + 1);
        userActionEditor.commit();

        ivSnippet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userActionEditor = userActionPrefs.edit();
                userActionEditor.putInt("ArticleClick", userActionPrefs.getInt("ArticleClick", 0) + 1);
                userActionEditor.commit();
                AppApplication.getInstance().trackEvent("Actions", "ArticleClicks", "userID:" + userID + " ArticleId: " + article.get(0) + " ArticleShown: " + userActionPrefs.getInt("ArticleShown", 0) + " ArticleClicks: " + userActionPrefs.getInt("ArticleClick", 0));
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.get(1)));
                startActivity(browserIntent);
            }
        });
    }
}
