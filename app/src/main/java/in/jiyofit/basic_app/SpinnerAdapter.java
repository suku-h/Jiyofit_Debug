package in.jiyofit.basic_app;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SpinnerAdapter extends BaseAdapter {
    private String[] array;
    private Context ctx;
    private Typeface typeface;

    public SpinnerAdapter(Context context, int arrayID, Typeface typeface) {
        this.ctx = context;
        this.array = ctx.getResources().getStringArray(arrayID);
        this.typeface = typeface;
    }

    @Override
    public int getCount() {
        return array.length;
    }

    @Override
    public Object getItem(int position) {
        return array[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView = new TextView(ctx);
        textView.setTypeface(typeface);
        textView.setText(array[position]);
        textView.setTextSize(22);
        if(typeface == AppApplication.engFont){
            textView.setTextSize(16);
        }

        textView.setPadding(10,5,10,5);
        if(position == 0){
            // Set the hint text color grey
            textView.setTextColor(ContextCompat.getColor(ctx, R.color.caldroid_middle_gray));
        } else {
            textView.setTextColor(ContextCompat.getColor(ctx, R.color.black));
        }
        return textView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView textView = new TextView(ctx);
        textView.setText(array[position]);
        textView.setTextSize(22);
        textView.setTypeface(typeface);
        if(typeface == AppApplication.engFont){
            textView.setTextSize(16);
        }

        textView.setPadding(10,5,10,5);
        //getColor without theme is deprecated. Hence, contextCompat method provides the compatibility
        textView.setBackgroundColor(ContextCompat.getColor(ctx, R.color.white));
        textView.setTextColor(ContextCompat.getColor(ctx, R.color.black));
        textView.setGravity(Gravity.CENTER);
        /*
        the adapter fills the number of elements based in the getCount
        so either getCount returns value conditionally for an array of different size in getDropDownView
        or the requisite value at position is hidden
        */
        if(position == 0){
            textView.setVisibility(View.GONE);
            //without setHeight the empty space at position is still visible
            textView.setHeight(0);
        }
        return textView;
    }
}

