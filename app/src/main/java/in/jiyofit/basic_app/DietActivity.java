package in.jiyofit.basic_app;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.wefika.flowlayout.FlowLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class DietActivity extends BaseActivity {
    String apiUrl = "https://api.myjson.com/bins/508gx";
    private static final String TAG = "CCC";
    private DBAdapter dbAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.activity_diet, contentFrameLayout);

        VolleySingleton volleySingleton = VolleySingleton.getInstance();
        RequestQueue requestQueue = volleySingleton.getRequestQueue();
        dbAdapter = new DBAdapter(this);

        final FlowLayout fl = (FlowLayout) findViewById(R.id.flowlayout);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, apiUrl,
                //Both both listeners running in main thread
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        ArrayList<String> foodList = null;
                        try {
                            foodList = parseJSONResponse(response);
                        } catch (JSONException e) {
                            Log.d(TAG, "jsonexception");
                            e.printStackTrace();
                        }
                        for (int i = 0; i < foodList.size(); i++) {
                            final TextView tvFood = new TextView(DietActivity.this);
                            FlowLayout.LayoutParams flLayoutParams = new FlowLayout.LayoutParams(
                                    FlowLayout.LayoutParams.WRAP_CONTENT, FlowLayout.LayoutParams.WRAP_CONTENT);
                            int margin = AppApplication.dpToPx(DietActivity.this, 5);
                            flLayoutParams.setMargins(margin, margin, margin, margin);
                            tvFood.setLayoutParams(flLayoutParams);
                            tvFood.setMinWidth(AppApplication.dpToPx(DietActivity.this, 45));
                            tvFood.setGravity(Gravity.CENTER);
                            int padding = AppApplication.dpToPx(DietActivity.this, 8);
                            tvFood.setPadding(padding, padding, padding, padding);
                            tvFood.setText(foodList.get(i));
                            tvFood.setTag("unclicked");
                            tvFood.setBackgroundResource(R.drawable.food_unclicked);
                            tvFood.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (tvFood.getTag().toString().equals("unclicked")) {
                                        tvFood.setBackgroundResource(R.drawable.food_clicked);
                                        tvFood.setTag("clicked");
                                        Toast.makeText(DietActivity.this, tvFood.getText().toString() + " is selected", Toast.LENGTH_SHORT).show();
                                    } else {
                                        tvFood.setBackgroundResource(R.drawable.food_unclicked);
                                        tvFood.setTag("unclicked");
                                        Toast.makeText(DietActivity.this, tvFood.getText().toString() + " is deselected", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                            fl.addView(tvFood);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                e.printStackTrace();
            }
        });

        requestQueue.add(stringRequest);
    }

    public ArrayList<String> parseJSONResponse(String response) throws JSONException {
        JSONArray jsonArray = new JSONArray(response);
        ArrayList<String> result = new ArrayList<String>();

        for (int i = 0; i < jsonArray.length(); i++) {
            Food food = new Food();
            JSONObject currentJSON = jsonArray.getJSONObject(i);
            result.add(currentJSON.getString(S.FOOD));

            food.setFoodID(currentJSON.getString(S.FOOD_ID));
            food.setFood(currentJSON.getString(S.FOOD));
            String type = currentJSON.getString(S.TYPE);
            food.setType(type);
            food.setHindiName(currentJSON.getString(S.HINDI_NAME));
            food.setImpFood(String.valueOf(true));
            String mealTimes = currentJSON.getString(S.MEAL_TIMES).replace("[", "").replace("]", "").replace("\"", "");
            food.setMealTimes(mealTimes);
            Integer otherMealTimePossibility = currentJSON.getInt(S.OTHER_MEAL_TIME);
            food.setOtherMealTimePossibility(otherMealTimePossibility);

            if (!currentJSON.getBoolean(S.HAS_SUBDISHES)) {
                //this store array as string without []" .
                // Inverted quote requires \ before it to denote it as string
                food.setSubdishes(S.NONE);
                JSONObject energyJSON = currentJSON.getJSONObject(S.ENERGY);
                food.setCalories(energyJSON.getInt(S.CALORIES));
                food.setUnit(energyJSON.getString(S.UNIT));
                JSONObject nutritionJSON = currentJSON.getJSONObject(S.NUTRITION);
                food.setProtein(nutritionJSON.getInt(S.PROTEIN));
                food.setCarbs(nutritionJSON.getInt(S.CARBS));
                food.setFat(nutritionJSON.getInt(S.FAT));
                food.setFibre(nutritionJSON.getInt(S.FIBRE));
                dbAdapter.insertFood(food);
                Log.d(TAG, "F: " + food.toString());
            } else {
                StringBuilder sb = new StringBuilder();
                JSONArray subdishArray = currentJSON.getJSONArray(S.SUBDISHES);
                for (int j = 0; j < subdishArray.length(); j++) {
                    ArrayList<String> subdish = new ArrayList<>();
                    JSONObject subdishJSON = subdishArray.getJSONObject(j);
                    subdish.add(subdishJSON.getString(S.FOOD_ID));
                    subdish.add(subdishJSON.getString(S.FOOD));
                    subdish.add(type);
                    subdish.add(subdishJSON.getString(S.HINDI_NAME));
                    subdish.add(String.valueOf(false));
                    subdish.add(mealTimes);
                    subdish.add(otherMealTimePossibility);
                    JSONObject energyJSON = subdishJSON.getJSONObject(S.ENERGY);
                    subdish.add(energyJSON.getString(S.CALORIES));
                    subdish.add(energyJSON.getString(S.UNIT));
                    JSONObject nutritionJSON = subdishJSON.getJSONObject(S.NUTRITION);
                    subdish.add(String.valueOf(nutritionJSON.getInt(S.PROTEIN)));
                    subdish.add(String.valueOf(nutritionJSON.getInt(S.CARBS)));
                    subdish.add(String.valueOf(nutritionJSON.getInt(S.FAT)));
                    subdish.add(String.valueOf(nutritionJSON.getInt(S.FIBRE)));

                    sb.append(subdish.get(0));
                    sb.append(S.COMMA);
                    Log.d(TAG, "S: " + subdish.toString());
                }
                //remove last "," from the string builder. And check for sb.length()>0 simultaneously
                sb.setLength(Math.max(sb.length() - 1, 0));
                food.add(sb.toString());
                Log.d(TAG, "FS: " + food.toString());
            }
        }
        return result;
    }

    public class Food {
        String foodID;
        String food;
        String type;
        String hindiName;
        String impFood;
        String mealTimes;
        Integer otherMealTimePossibility;
        String subdishes;
        Integer calories;
        String unit;
        Integer protein;
        Integer carbs;
        Integer fat;
        Integer fibre;

        public Food() {
        }

        public Food(Integer fibre, Integer fat, Integer carbs, Integer protein, String unit, Integer calories, String subdishes, Integer otherMealTimePossibility, String mealTimes, String impFood, String hindiName, String type, String food, String foodID) {
            this.fibre = fibre;
            this.fat = fat;
            this.carbs = carbs;
            this.protein = protein;
            this.unit = unit;
            this.calories = calories;
            this.subdishes = subdishes;
            this.otherMealTimePossibility = otherMealTimePossibility;
            this.mealTimes = mealTimes;
            this.impFood = impFood;
            this.hindiName = hindiName;
            this.type = type;
            this.food = food;
            this.foodID = foodID;
        }

        public String getFoodID() {
            return foodID;
        }

        public void setFoodID(String foodID) {
            this.foodID = foodID;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getHindiName() {
            return hindiName;
        }

        public void setHindiName(String hindiName) {
            this.hindiName = hindiName;
        }

        public String getImpFood() {
            return impFood;
        }

        public void setImpFood(String impFood) {
            this.impFood = impFood;
        }

        public Integer getOtherMealTimePossibility() {
            return otherMealTimePossibility;
        }

        public void setOtherMealTimePossibility(Integer otherMealTimePossibility) {
            this.otherMealTimePossibility = otherMealTimePossibility;
        }

        public Integer getCalories() {
            return calories;
        }

        public void setCalories(Integer calories) {
            this.calories = calories;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Integer getProtein() {
            return protein;
        }

        public void setProtein(Integer protein) {
            this.protein = protein;
        }

        public Integer getCarbs() {
            return carbs;
        }

        public void setCarbs(Integer carbs) {
            this.carbs = carbs;
        }

        public Integer getFat() {
            return fat;
        }

        public void setFat(Integer fat) {
            this.fat = fat;
        }

        public Integer getFibre() {
            return fibre;
        }

        public void setFibre(Integer fibre) {
            this.fibre = fibre;
        }

        public String getSubdishes() {
            return subdishes;
        }

        public void setSubdishes(String subdishes) {
            this.subdishes = subdishes;
        }

        public String getMealTimes() {
            return mealTimes;
        }

        public void setMealTimes(String mealTimes) {
            this.mealTimes = mealTimes;
        }

        public String getFood() {
            return food;
        }

        public void setFood(String food) {
            this.food = food;
        }
    }
}
