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
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import java.util.ArrayList;
import java.util.Random;

public class ProductFragment extends Fragment {
    private SharedPreferences userActionPrefs;
    private SharedPreferences.Editor userActionEditor;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.product_row, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences loginPrefs = getActivity().getSharedPreferences("LoginInfo", Context.MODE_PRIVATE);
        final String userID = loginPrefs.getString("userID","");
        VolleySingleton volleySingleton = VolleySingleton.getInstance();
        ImageLoader imageLoader = volleySingleton.getImageLoader();

        final ImageView ivProduct = (ImageView) getActivity().findViewById(R.id.rproduct_iv);
        TextView tvProductName = (TextView) getActivity().findViewById(R.id.rproduct_tv);

        final DBAdapter dbAdapter = new DBAdapter(getActivity());
        ArrayList<String> productIDs = dbAdapter.getRecommendedProductIDs(loginPrefs.getString("gender", "male"));
        Random r = new Random();
        int productNum = r.nextInt(productIDs.size());
        final ArrayList<String> product = dbAdapter.getSingleProduct(productIDs.get(productNum));

        userActionPrefs = getActivity().getSharedPreferences("UserActions", Context.MODE_PRIVATE);
        userActionEditor = userActionPrefs.edit();
        userActionEditor.putInt("ProductShown", userActionPrefs.getInt("ProductShown", 0) + 1);
        userActionEditor.commit();

        View.OnClickListener productClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbAdapter.addClick(product.get(0));
                AppApplication.getInstance().trackEvent("Affiliate", "Product", "userID:" + userID + " ProductID: " + product.get(0) + " Category: MainActivity");
                userActionEditor = userActionPrefs.edit();
                userActionEditor.putInt("ProductClick", userActionPrefs.getInt("ProductClick", 0) + 1);
                userActionEditor.commit();
                AppApplication.getInstance().trackEvent("Actions", "ProductClicks", "userID:" + userID + " ProductShown: " + userActionPrefs.getInt("ProductShown", 0) + " ProductClicks: " + userActionPrefs.getInt("ProductClick", 0));
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(product.get(3)));
                startActivity(browserIntent);
            }
        };

        String imageURL = product.get(2);
        imageLoader.get(imageURL, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                ivProduct.setImageBitmap(response.getBitmap());
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                startActivity(new Intent(getActivity(), MainActivity.class));
            }
        });

        tvProductName.setText(product.get(1));
        tvProductName.setOnClickListener(productClick);
        ivProduct.setOnClickListener(productClick);
    }
}
