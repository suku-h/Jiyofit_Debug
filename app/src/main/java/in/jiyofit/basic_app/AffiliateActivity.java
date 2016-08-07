package in.jiyofit.basic_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import java.util.ArrayList;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class AffiliateActivity extends BaseActivity {
    private Spinner ddCategories;
    private RecyclerView rvProduct;
    private DBAdapter dbAdapter;
    private static String userID;
    private Boolean onCreateSelection = true;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.activity_affiliate, contentFrameLayout);

        SharedPreferences loginPrefs = getSharedPreferences("LoginInfo", MODE_PRIVATE);
        userID = loginPrefs.getString("userID","");
        final String gender = loginPrefs.getString("gender", "male");

        rvProduct = (RecyclerView) findViewById(R.id.aaffiliate_rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvProduct.setLayoutManager(layoutManager);

        TextView tvCategorySelect = (TextView) findViewById(R.id.aaffiliate_tv_categoryselect);
        tvCategorySelect.setTypeface(AppApplication.engFont);

        dbAdapter = new DBAdapter(this);
        final String[] categoriesMapper = getResources().getStringArray(R.array.categoriesMapper);
        ddCategories = (Spinner) findViewById(R.id.aaffiliate_dd_categories);
        ddCategories.setAdapter(new SpinnerAdapter(this, R.array.categories, AppApplication.hindiFont));
        ddCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // change the color of the text in the spinner, but not in the appearing list.
                ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);
                if (AppApplication.isNetworkAvailable(AffiliateActivity.this)) {
                    if (!onCreateSelection) {
                        AppApplication.getInstance().trackEvent("Affiliate", "Category", "userID:" + userID + " " + categoriesMapper[position]);
                    } else {
                        onCreateSelection = false;
                    }
                    ArrayList<ArrayList<String>> products;
                    if (position != 0 && position != 1) {
                        products = dbAdapter.getProducts(categoriesMapper[position], gender);
                    } else {
                        products = dbAdapter.getRecommendedProducts(gender);
                    }
                    ArrayList<ProductInfo> productListData = new ArrayList<ProductInfo>();
                    for (int i = 0; i < products.size(); i++) {
                        ProductInfo productInfo = new ProductInfo();
                        productInfo.setProductID(products.get(i).get(0));
                        productInfo.setTextEnglish(products.get(i).get(1));
                        productInfo.setTextHindi(products.get(i).get(2));
                        productInfo.setProductLink(products.get(i).get(3));
                        productInfo.setAffiliateLink(products.get(i).get(4));
                        productListData.add(productInfo);
                    }
                    ProductListAdapter productListAdapter = new ProductListAdapter(categoriesMapper[position]);
                    productListAdapter.setProductList(productListData);
                    rvProduct.setAdapter(productListAdapter);
                } else {
                    AppApplication.toastIt(AffiliateActivity.this, R.string.networkunavailable);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ddCategories.setSelection(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppApplication.getInstance().trackScreenView("AffiliateActivity");
    }

    public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ProductInfoViewHolder> {
        private LayoutInflater layoutInflater;
        private ArrayList<ProductInfo> productList = new ArrayList<>();
        private ImageLoader imageLoader;
        private DBAdapter dba;
        private String category;

        public ProductListAdapter(String category) {
            this.category = category;
            layoutInflater = LayoutInflater.from(AffiliateActivity.this);
            VolleySingleton volleySingleton = VolleySingleton.getInstance();
            imageLoader = volleySingleton.getImageLoader();
            dba = new DBAdapter(AffiliateActivity.this);
        }

        public void setProductList(ArrayList<ProductInfo> productInfo) {
            productList = productInfo;
            //update the adapter to reflect the new set of productlist
            notifyDataSetChanged();
        }

        @Override
        public ProductInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = layoutInflater.inflate(R.layout.product_row, parent, false);
            return new ProductInfoViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ProductInfoViewHolder holder, int position) {
            final ProductInfo currentProduct = productList.get(position);
            View.OnClickListener productClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dba.addClick(currentProduct.getProductID());
                    AppApplication.getInstance().trackEvent("Affiliate", "Product", "userID:" + userID + " ProductID: " + currentProduct.getProductID() + " Category: " + category);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentProduct.getAffiliateLink()));
                    startActivity(browserIntent);
                }
            };
            holder.tvName.setText(currentProduct.getTextHindi());
            holder.tvName.setOnClickListener(productClick);
            String imageURL = currentProduct.getProductLink();
            imageLoader.get(imageURL, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    holder.ivProduct.setImageBitmap(response.getBitmap());
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    holder.ivProduct.setImageResource(R.drawable.unavailable);
                }
            });
            holder.ivProduct.setOnClickListener(productClick);
        }

        @Override
        public int getItemCount() {
            return productList.size();
        }

        //cannot be static as it has to take reference of the context above
        class ProductInfoViewHolder extends RecyclerView.ViewHolder {
            ImageView ivProduct;
            TextView tvName;

            public ProductInfoViewHolder(View itemView) {
                super(itemView);
                ivProduct = (ImageView) itemView.findViewById(R.id.rproduct_iv);
                tvName = (TextView) itemView.findViewById(R.id.rproduct_tv);
            }
        }
    }

    private class ProductInfo {
        String productLink;
        String textHindi;
        String textEnglish;
        String productID;
        String affiliateLink;

        private ProductInfo() {
        }

        public String getProductLink() {
            return productLink;
        }

        public void setProductLink(String productLink) {
            this.productLink = productLink;
        }

        public String getTextHindi() {
            return textHindi;
        }

        public void setTextHindi(String textHindi) {
            this.textHindi = textHindi;
        }

        public String getTextEnglish() {
            return textEnglish;
        }

        public void setTextEnglish(String textEnglish) {
            this.textEnglish = textEnglish;
        }

        public String getProductID() {
            return productID;
        }

        public void setProductID(String productID) {
            this.productID = productID;
        }

        public String getAffiliateLink() {
            return affiliateLink;
        }

        public void setAffiliateLink(String affiliateLink) {
            this.affiliateLink = affiliateLink;
        }
    }
}
