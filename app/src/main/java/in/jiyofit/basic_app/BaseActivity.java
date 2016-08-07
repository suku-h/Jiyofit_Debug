package in.jiyofit.basic_app;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class BaseActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    protected DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    protected ListView navList;
    protected FrameLayout contentFrameLayout;
    String[] activityNameArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        drawerLayout = (DrawerLayout) findViewById(R.id.abase_drawer_layout);
        contentFrameLayout = (FrameLayout) findViewById(R.id.abase_content_frame);
        navList = (ListView) findViewById(R.id.abase_lv);
        NavAdapter navAdapter = new NavAdapter();
        navList.setAdapter(navAdapter);
        navList.setOnItemClickListener(this);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.opendrawer, R.string.closedrawer) {
            /*
            This method is used because the activity may be opened through buttons & not just drawer.
            This would mean the navList.setItemChecked for that activity remains false
            Hence, detect which activity is open while opening drawer
            DrawerLayout.STATE_DRAGGING means dragging by user. So hamburger click is not recognized
            DrawerOpened works only if the drawer is completely open and not in process of opening
            */
            @Override
            public void onDrawerStateChanged(int newState) {
                if(newState == DrawerLayout.STATE_SETTLING) {
                    String[] activityNames = getResources().getStringArray(R.array.activity_names);
                    int i = 0;
                    //can't pass a class variable to instanceof. The instanceof operator works on reference
                    // types, like Integer, and not on objects, like new Integer(213)
                    //instanceof does not mean "y is an instance of Object x", it means "y is an instance of type X"
                    try {
                        Class<?> thisActivity = MainActivity.class;
                        while (!thisActivity.isInstance(BaseActivity.this)  && i < activityNames.length - 1) {
                            i++;
                            thisActivity = Class.forName(String.valueOf(getPackageName()) + "." + activityNames[i]);
                        }

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    navList.setItemChecked(i , true);
                }
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activityNameArray = getResources().getStringArray(R.array.activity_names);
    }

    //this takes care of orientation change during drawer opening and closing
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    //when a user switches between portrait and landscape sometimes a new activity is created.
    // savedInstance takes back up of whatever the user is doing and puts it back to
    // the switched view. So user loses no data
    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // this makes the hamburger icon into a button. This button now opens and closes the drawer
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return actionBarDrawerToggle.onOptionsItemSelected(item);
        }
        //need to do the return because break is used
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        navList.setItemChecked(position, true);
        drawerLayout.closeDrawers();
        //get name of current activity
        String currentActivityName = this.getClass().getSimpleName();
        String clickedActivityName = activityNameArray[position];
        if (!clickedActivityName.equals(currentActivityName)) {
            if(!clickedActivityName.equals("FeedbackFragment")) {
                Class<?> nextActivity;
                try {
                    nextActivity = Class.forName(String.valueOf(getPackageName()) + "." + clickedActivityName);
                    startActivity(new Intent(this, nextActivity));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                FeedbackFragment feedbackFragment = new FeedbackFragment();
                feedbackFragment.show(getFragmentManager(), "feedback");
            }
        }
    }

    private class NavAdapter extends BaseAdapter {
        String[] menu;
        int[] drawerImages = {R.drawable.home_icon, R.drawable.workout_icon, R.drawable.diet_icon,
                R.drawable.history_icon, R.drawable.affiliate_icon, R.drawable.settings_icon, R.drawable.feedback_icon};

        public NavAdapter (){
            menu = getResources().getStringArray(R.array.menu);
        }
        @Override
        public int getCount() {
            return menu.length;
        }

        @Override
        public Object getItem(int position) {
            return menu[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row;
            if (convertView == null){
                //this means that the view is being created for the first time, hence needs layout inflater
                LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.drawer_row, parent, false);
            } else {
                row = convertView;
            }
            TextView drawerTV = (TextView) row.findViewById(R.id.rdrawer_tv);
            ImageView drawerIV = (ImageView) row.findViewById(R.id.rdrawer_iv);
            drawerTV.setText(menu[position]);
            drawerTV.setTypeface(AppApplication.hindiFont);
            drawerTV.setTextSize(22);
            drawerIV.setImageResource(drawerImages[position]);
            return row;
        }
    }
}
