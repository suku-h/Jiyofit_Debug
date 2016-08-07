package in.jiyofit.basic_app;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteReadOnlyDatabaseException;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;

public class DBAdapter extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "workoutDB.sqlite";
    private static final String TAG = "CCC";
    private static final int DATABASE_VERSION = 11;
    private SQLiteDatabase db;
    private Context ctx;

    private static String HISTORY_DATA = "History_Data";
    private static String DATE = "Date";
    private static String WORKOUT_COMPLETION = "Workout_Completion";
    private static String DURATION = "Workout_Duration";
    private static String TYPE = "Type";
    private static String WALK_COMPLETION = "Walk_Completion";
    private static String STEPS = "Steps";
    private static String TARGET = "Target";

    private static String TIPS_LIST = "Tips_List";
    private static String TIP_ID = "id";
    private static String TIP_TYPE = "Type_Hindi";
    private static String TIP_TYPE_ENG = "Type_English";
    private static String TIP = "Hindi_Tip";
    private static String TEXT = "Hindi_Text";

    public DBAdapter(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        //if it is getReadableDatabase then it won't get updated on upgrading- throws exception
        //ctx = context should be before getWritableDatabase as getWritabledatabase calls
        // onUpgrade method and it won't find value assigned to ctx and will give an NPE
        ctx = context;
        try {
            db = getWritableDatabase();
        } catch (SQLiteReadOnlyDatabaseException e){
            ctx.startActivity(new Intent(ctx, MainActivity.class));
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion < 11) {
            ctx.deleteDatabase(DATABASE_NAME);
            new DBAdapter(ctx);
        } else if(oldVersion < 12){
            String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + S.FOOD_TABLE + " (" + S.FOOD_ID + " VARCHAR NOT NULL, "
                    + S.FOOD + " VARCHAR, " + S.HINDI_NAME + " VARCHAR, " + TYPE + " VARCHAR, "
                    + S.IMP_FOOD + " BOOLEAN, " + S.MEAL_TIMES + " TEXT, " + S.OTHER_MEAL_TIME + " INT, "
                    + S.SUBDISHES + " TEXT, " + S.CALORIES + " TEXT, " + S.UNIT + " TEXT, " + S.CALORIES + " TEXT, "
                    + S.UNIT + " TEXT, " + S.PROTEIN + " INT, " + S.CARBS + " INT, " + S.FAT + " INT, " + S.FIBRE + " INT)";
            db.execSQL(CREATE_TABLE);
        } else {
            super.onUpgrade(db, oldVersion, newVersion);
        }
    }

    public String getDayHistory(String date) {
        String result = "";
        String cols[] = {WORKOUT_COMPLETION, WALK_COMPLETION};
        Cursor cursor = db.query(HISTORY_DATA, cols, DATE + " = '" + date + "'", null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            for (String col : cols) {
                result = result + String.valueOf(cursor.getInt(cursor.getColumnIndex(col)));
            }
            cursor.close();
        }
        return result;
    }

    public ArrayList<Integer> getDayData(String date) {
        ArrayList<Integer> dayData = new ArrayList<>();
        String cols[] = {WORKOUT_COMPLETION, DURATION, TYPE, STEPS, TARGET};
        Cursor cursor = db.query(HISTORY_DATA, cols, DATE + " = '" + date + "'", null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                for (int i = 0; i < cols.length; i++) {
                    dayData.add(i, cursor.getInt(cursor.getColumnIndex(cols[i])));
                }
            }
            cursor.close();
        }
        return dayData;
    }

    public Boolean checkDateExists(String date) {
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + HISTORY_DATA + " WHERE " + DATE + "=?", new String[]{date});
        Boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public void workoutComplete(String date, int completion, int duration, int type) {
        ContentValues cv = new ContentValues();
        Log.d("CCC", "WKC CALLED");
        cv.put(WORKOUT_COMPLETION, completion);
        cv.put(DURATION, duration);
        cv.put(TYPE, type);
        db.update(HISTORY_DATA, cv, DATE + " = ?", new String[]{date});
    }

    public void walkData(String date, int steps, int target) {
        int completion = 1;
        Log.d("CCC", "wd: date:" + date + " s: " + steps + "tgt: " + target);
        if (steps < target) {
            completion = 0;
        }
        ContentValues cv = new ContentValues();
        cv.put(WALK_COMPLETION, completion);
        cv.put(STEPS, steps);
        cv.put(TARGET, target);
        db.update(HISTORY_DATA, cv, DATE + " = ?", new String[]{date});
    }

    public ArrayList<ArrayList<String>> getAll() {
        ArrayList<ArrayList<String>> playlist = new ArrayList<>();
        String cols[] = {DATE, WORKOUT_COMPLETION, DURATION, TYPE, WALK_COMPLETION, STEPS, TARGET};
        Cursor cursor = db.query(HISTORY_DATA, cols, null, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ArrayList<String> childlist = new ArrayList<>();
                for (String col : cols) {
                    if (col.equals(DATE)) {
                        childlist.add(cursor.getString(cursor.getColumnIndex(col)));
                    } else {
                        childlist.add(String.valueOf(cursor.getInt(cursor.getColumnIndex(col))));
                    }
                }
                playlist.add(childlist);
            }
            cursor.close();
        }
        return playlist;
    }

    public void insertDate(String date) {
        ContentValues cv = new ContentValues();
        cv.put(DATE, date);
        db.insert(HISTORY_DATA, null, cv);
    }

    public ArrayList<ArrayList<String>> getProducts(String category, String gender) {
        String notGender;
        if (gender.equals("male")) {
            notGender = "female";
        } else {
            notGender = "male";
        }
        Log.d(TAG, "Category: " + category);
        ArrayList<ArrayList<String>> products = new ArrayList<>();
        String cols[] = {"ProductID", "Product_Name_English", "Product_Name_Hindi", "Image_Link", "Affiliate_Link"};
        Cursor cursor = db.query("Product_List", cols, "Category = ? AND Gender !=? ", new String[]{category, notGender}, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ArrayList<String> childlist = new ArrayList<>();
                for (String col : cols) {
                    childlist.add(String.valueOf(cursor.getString(cursor.getColumnIndex(col))));
                }
                products.add(childlist);
            }
            cursor.close();
        }
        return products;
    }

    public ArrayList<ArrayList<String>> getRecommendedProducts(String gender) {
        String notGender;
        if (gender.equals("male")) {
            notGender = "female";
        } else {
            notGender = "male";
        }
        ArrayList<ArrayList<String>> products = new ArrayList<>();
        String cols[] = {"ProductID", "Product_Name_English", "Product_Name_Hindi", "Image_Link", "Affiliate_Link"};
        Cursor cursor = db.query("Product_List", cols, "Clicks > 0 AND Gender !=? ", new String[]{notGender}, null, null, " Clicks DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ArrayList<String> childlist = new ArrayList<>();
                for (String col : cols) {
                    childlist.add(String.valueOf(cursor.getString(cursor.getColumnIndex(col))));
                }
                products.add(childlist);
            }
            cursor.close();
        }
        Log.d(TAG, "PRODUCTS: " + products.toString());
        return products;
    }

    public ArrayList<String> getRecommendedProductIDs(String gender) {
        String notGender;
        if (gender.equals("male")) {
            notGender = "female";
        } else {
            notGender = "male";
        }
        ArrayList<String> productIDs = new ArrayList<>();
        Cursor cursor = db.query("Product_List", new String[] {"ProductID"}, "Clicks > 0 AND Gender !=? ", new String[]{notGender}, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                productIDs.add(cursor.getString(0));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return productIDs;
    }

    public ArrayList<String> getSingleProduct(String productID) {
        ArrayList<String> result = new ArrayList<>();
        String[] cols = {"ProductID",  "Product_Name_Hindi", "Image_Link", "Affiliate_Link"};
        Cursor cursor = db.query("Product_List", cols, "ProductID =? ", new String[]{productID}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            for (String col : cols) {
                result.add(cursor.getString(cursor.getColumnIndex(col)));
            }
            cursor.close();
        }
        return result;
    }

    public void addClick(String productID) {
        db.execSQL("UPDATE Product_List SET Clicks = Clicks + 2 WHERE ProductID = '" + productID + "'");
    }

    public ArrayList<String> getTipIDs() {
        ArrayList<String> tipIDList = new ArrayList<>();
        Cursor cursor = db.query(TIPS_LIST, new String[]{TIP_ID}, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                tipIDList.add(cursor.getString(0));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return tipIDList;
    }

    public ArrayList<String> getTip(String tipID) {
        ArrayList<String> result = new ArrayList<>();
        String[] cols = {TIP_ID, TIP_TYPE, TIP, TEXT, TIP_TYPE_ENG};
        Cursor cursor = db.query(TIPS_LIST, cols, TIP_ID + " =? ", new String[]{tipID}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            for (String col : cols) {
                result.add(cursor.getString(cursor.getColumnIndex(col)));
            }
            cursor.close();
        }
        return result;
    }

    public ArrayList<String> getArticleIDs(String gender) {
        String notGender;
        if (gender.equals("male")) {
            notGender = "female";
        } else {
            notGender = "male";
        }
        ArrayList<String> articleIDList = new ArrayList<>();
        Cursor cursor = db.query("Article_List", new String[]{"ArticleID"}, "Gender !=? ", new String[]{notGender}, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                articleIDList.add(cursor.getString(0));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return articleIDList;
    }

    public ArrayList<String> getArticle(String articleID) {
        ArrayList<String> result = new ArrayList<>();
        String[] cols = {"ArticleID", "Article_Link", "Image_Link"};
        Cursor cursor = db.query("Article_List", cols, "ArticleID =? ", new String[]{articleID}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            for (String col : cols) {
                result.add(cursor.getString(cursor.getColumnIndex(col)));
            }
            cursor.close();
        }
        return result;
    }

    public String getWorkoutID (String gender, Integer dayCode, Integer duration, String workoutType){
        Cursor cursor = db.query("Workout_List", new String[]{"Workout_Code"}, " Gender =? AND Day_Code =? AND Workout_Duration =? AND Workout_Type =? ",
                new String[] {gender, String.valueOf(dayCode), String.valueOf(duration), workoutType}, null, null, null);
        String result = null;
        if (cursor != null) {
            cursor.moveToFirst();
            result = cursor.getString(0);
            cursor.close();
        }
        return result;
    }

    public ArrayList<ArrayList<String>> getWorkout(String workoutID){
        ArrayList<ArrayList<String>> playlist = new ArrayList<>();
        String[] cols = {"Exercise_Name", "Duration", "Switch"};
        Cursor cursor = db.query("Exercise_Playlist", cols, " Workout_Code =? ", new String[]{workoutID}, null, null, " Exercise_Order ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ArrayList<String> childlist = new ArrayList<>();
                for (String col : cols) {
                    if(col.equals("Exercise_Name")) {
                        childlist.add(cursor.getString(cursor.getColumnIndex(col)));
                    } else {
                        childlist.add(String.valueOf(cursor.getInt(cursor.getColumnIndex(col))));
                    }
                }
                playlist.add(childlist);
            }
            cursor.close();
        }
        return playlist;
    }

    public ArrayList<String> getExerciseData (String exerciseName){
        ArrayList<String> result = new ArrayList<>();
        String[] cols = {"Display_Name", "Pre_Audio", "Audio1", "Audio2", "Audio3", "Primary_Target", "Secondary_Target"};
        Cursor cursor = db.query("Exercise_List", cols, "Exercise_Name =? ", new String[]{exerciseName}, null, null, null);
        if(cursor != null){
            while (cursor.moveToNext()) {
                for (String col : cols) {
                    result.add(cursor.getString(cursor.getColumnIndex(col)));
                }
            }
            cursor.close();
        }
        return result;
    }

    /*********************Diet Part**************************/
    public ArrayList<ArrayList<String>> getImpFoods(String mealTime){
        ArrayList<ArrayList<String>> result = new ArrayList<>();
        String cols[] = {S.FOOD_ID, S.FOOD};
        String query = "SELECT (" + cols[0] + ", " + cols[2] + ") FROM " + S.FOOD_TABLE + " WHERE "
                + S.IMP_FOOD + " = true AND (" + S.MEAL_TIMES + " LIKE %?% OR " + S.MEAL_TIMES
                + " LIKE %? OR " + S.MEAL_TIMES + " LIKE ?% OR " + S.MEAL_TIMES + " LIKE ?)";
        //+ " = true AND (" + MEAL_TIME + " LIKE '%" + mealTime + "%' OR " + MEAL_TIME + " LIKE '%" + mealTime + "' OR "
        //+ MEAL_TIME + " LIKE '" + mealTime + "%' OR " + MEAL_TIME + " LIKE '" + mealTime + "')";
        Cursor cursor = db.rawQuery(query, new String[]{mealTime, mealTime, mealTime, mealTime});
        if(cursor != null){
            while (cursor.moveToNext()) {
                ArrayList<String> childlist = new ArrayList<>();
                for (String col : cols) {
                    childlist.add(String.valueOf(cursor.getString(cursor.getColumnIndex(col))));
                }
                result.add(childlist);
            }
            cursor.close();
        }
        return result;
    }

    public void insertFood (DietActivity.Food food){
        /*String id, String food, String type, String hindiName, Boolean isImpFood,
                            String mealTime, int otherMealTimePossibility, String subdishes, String calories,
                            String unit, Integer protein, Integer carbs, Integer fat, Integer fibre
                            */
        ContentValues cv = new ContentValues();
        cv.put(S.FOOD_ID, food.getFoodID());
        cv.put(S.FOOD, food.getFood());
        cv.put(TYPE, food.getType());
        cv.put(S.HINDI_NAME, food.getHindiName());
        cv.put(S.IMP_FOOD, food.getImpFood());
        cv.put(S.MEAL_TIMES, food.getMealTimes());
        cv.put(S.OTHER_MEAL_TIME, food.getOtherMealTimePossibility());
        cv.put(S.SUBDISHES, food.getSubdishes());
        cv.put(S.CALORIES, food.getCalories());
        cv.put(S.UNIT, food.getUnit());
        cv.put(S.PROTEIN, food.getProtein());
        cv.put(S.CARBS, food.getCarbs());
        cv.put(S.FAT, food.getFat());
        cv.put(S.FIBRE, food.getFibre());
        db.insert(S.FOOD_TABLE, null, cv);
    }
}
