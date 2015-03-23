/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package haris.app.myschedule;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import haris.app.myschedule.data.ScheduleContract;
import haris.app.myschedule.data.ScheduleContract.Schedule;
import haris.app.myschedule.data.ScheduleDbHelper;
import haris.app.myschedule.service.MyScheduleService;
import haris.app.myschedule.sync.MyScheduleSyncAdapter;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link android.widget.ListView} layout.
 */
public class ScheduleFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{



    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri dateUri);
    }

    //    private ArrayAdapter<String> mForecastAdapter;
    private static final int FORECAST_LOADER = 0;
    // For the forecast view we're showing only a small subset of the stored data.
// Specify the columns we need.
    private static final String[] FORECAST_COLUMNS = {
// In this case the id needs to be fully qualified with a table name, since
// the content provider joins the location & weather tables in the background
// (both have an _id column)
// On the one hand, that's annoying. On the other, you can search the weather table
// using the location set by the user, which is only in the Location table.
// So the convenience is worth it.
            ScheduleContract.WeatherEntry.TABLE_NAME + "." + ScheduleContract.WeatherEntry._ID,
            ScheduleContract.WeatherEntry.COLUMN_DATE,
            ScheduleContract.WeatherEntry.COLUMN_SHORT_DESC,
            ScheduleContract.WeatherEntry.COLUMN_MAX_TEMP,
            ScheduleContract.WeatherEntry.COLUMN_MIN_TEMP,
            ScheduleContract.LocationEntry.COLUMN_LOCATION_SETTING,
            ScheduleContract.WeatherEntry.COLUMN_WEATHER_ID,
            ScheduleContract.LocationEntry.COLUMN_COORD_LAT,
            ScheduleContract.LocationEntry.COLUMN_COORD_LONG

    };


    private static final String[] SCHEDULE_COLUMNS = {
            Schedule.TABLE_NAME + "." + Schedule._ID,
            Schedule.COLUMN_ID,
            Schedule.COLUMN_DAY,
            Schedule.COLUMN_TIME_START,
            Schedule.COLUMN_TIME_END,
            Schedule.COLUMN_CLASS,
            Schedule.COLUMN_ROOM,
            Schedule.COLUMN_LESSON_FULL,
            Schedule.COLUMN_LESSON_SHORT

    };

    public static final int COL_SCHEDULE_COLUMN_ID = 0;
    public static final int COL_SCHEDULE_ID = 1;
    public static final int COL_SCHEDULE_DAY = 2;
    public static final int COL_SCHEDULE_TIME_START = 3;
    public static final int COL_SCHEDULE_TIME_END = 4;
    public static final int COL_SCHEDULE_CLASS = 5;
    public static final int COL_SCHEDULE_ROOM = 6;
    public static final int COL_SCHEDULE_LESSON_FULL = 7;
    public static final int COL_SCHEDULE_LESSON_SHORT = 8;
    public static final int COL_SCHEDULE_DAY_ID = 9;




    // These indices are tied to FORECAST_COLUMNS. If FORECAST_COLUMNS changes, these
// must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;
    private ScheduleAdapter mScheduleAdapter;
    private ArrayAdapter mSchedule;
    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
    private static final String SELECTED_KEY = "selected_position";
    private boolean mUseTodayLayout;
    public static final String LOG_TAG = ScheduleFragment.class.getSimpleName();

    private TextView lessonName;
    private TextView time;
    private TextView room;
    private TextView nextLabel;
    View rootView;

    static final int SUNDAY_ID = 1;
    static final int MONDAY_ID = 2;
    static final int TUESDAY_ID = 3;
    static final int WEDNESDAY_ID = 4;
    static final int THURSDAY_ID = 5;
    static final int FRIDAY_ID = 6;
    static final int SATURDAY_ID = 7;

    public ScheduleFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
//            updateSchedule();
            update();
            return true;
        }

//        if (id == R.id.action_map) {
//            openPreferredLocationInMap();
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }




    public void nextLesson(){
        lessonName = (TextView)rootView.findViewById(R.id.lesson_textView);
        time = (TextView)rootView.findViewById(R.id.time_textView);
        room = (TextView)rootView.findViewById(R.id.room_textView);
        nextLabel = (TextView)rootView.findViewById(R.id.next_lesson_textView);


//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        String currentTime = sdf.format(new Date());
        Calendar calendar = Calendar.getInstance();
        int toDay = calendar.get(Calendar.DAY_OF_WEEK);

        Log.d(LOG_TAG, "Today is " + toDay);
        Log.d(LOG_TAG, "SEKARANG " + currentTime);

        ScheduleDbHelper mOpenHelper = new ScheduleDbHelper(getActivity());
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor cursor;

        cursor = db.rawQuery("SELECT distinct("+Schedule.COLUMN_DAY+") FROM "+ Schedule.TABLE_NAME, null);
        if(cursor.moveToFirst()){
            do{
                Log.d(LOG_TAG, "DAY "+cursor.getString(0));
            }while (cursor.moveToNext());
        }

        Cursor check = db.rawQuery("SELECT * FROM "+Schedule.TABLE_NAME,null);
        cursor = db.rawQuery("SELECT * FROM "+ Schedule.TABLE_NAME+" WHERE day_id = '" + toDay+"' AND time_start > '"+ currentTime +"';",null);
        if(check.moveToFirst()){
            while (!cursor.moveToFirst()){
                if(toDay<7){
                    toDay++;
                }else {
                    toDay = 1;
                }
                Log.d(LOG_TAG, "Change day_id to "+toDay);
                cursor = db.rawQuery("SELECT * FROM "+ Schedule.TABLE_NAME+" WHERE day_id = '" + toDay +"' AND time_start > '"+ "00:00" +"';",null);
            }
        }else {
            nextLabel.setText("USER ID NOT FOUND!");
            lessonName.setText("");
            time.setText("");
            room.setText("");
        }

        if(cursor.moveToFirst()){
            nextLabel.setText("Next Lesson");
            do{
                Log.d(LOG_TAG,"LESSON " + cursor.getString(COL_SCHEDULE_LESSON_FULL));
                lessonName.setText(cursor.getString(COL_SCHEDULE_LESSON_FULL) + " " + cursor.getString(COL_SCHEDULE_CLASS));
                time.setText(cursor.getString(COL_SCHEDULE_DAY) + " at " + cursor.getString(COL_SCHEDULE_TIME_START)+ " until "+cursor.getString(COL_SCHEDULE_TIME_END));
                room.setText("in " + cursor.getString(COL_SCHEDULE_ROOM));
                break;
            }while (cursor.moveToNext());
        }else{
            nextLabel.setText("USER ID NOT FOUND!");
            lessonName.setText("");
            time.setText("");
            room.setText("");
        }
    }



    @Override
    public void onResume() {
        nextLesson();
        super.onResume();
    }

    public static LayoutInflater INFLATER;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        INFLATER = inflater;
        mScheduleAdapter = new ScheduleAdapter(getActivity(), null, 0);
        List<String> scheduleList = new ArrayList<>();

        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mListView.setAdapter(mScheduleAdapter);

        nextLesson();

//        lessonName = (TextView)rootView.findViewById(R.id.lesson_textView);
//        time = (TextView)rootView.findViewById(R.id.time_textView);
//        room = (TextView)rootView.findViewById(R.id.room_textView);
//
//        lessonName.setText("");
//        time.setText("");
//        room.setText("");
//
//        String toDay = "Monday";
//        String nextDay = "Tuesday";
//        String timeNow = "19:00";
//
//        ScheduleDbHelper mOpenHelper = new ScheduleDbHelper(getActivity());
//        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
//        Cursor cursor;
//
//        cursor = db.rawQuery("SELECT distinct("+Schedule.COLUMN_DAY+") FROM "+ Schedule.TABLE_NAME, null);
//        if(cursor.moveToFirst()){
//            do{
//                Log.d(LOG_TAG, "DAY "+cursor.getString(0));
//            }while (cursor.moveToNext());
//        }
//
//        cursor = db.rawQuery("SELECT * FROM "+ Schedule.TABLE_NAME+" WHERE day = '" + toDay+"' AND time_start > '"+ timeNow +"';",null);
//        if(!cursor.moveToFirst()){
//            cursor = db.rawQuery("SELECT * FROM "+ Schedule.TABLE_NAME+" WHERE day = '" + nextDay+"' AND time_start > '"+ "00:00" +"';",null);
//        }
//        if(cursor.moveToFirst()){
//            do{
//                Log.d(LOG_TAG,"LESSON " + cursor.getString(COL_SCHEDULE_LESSON_FULL));
//                lessonName.setText(cursor.getString(COL_SCHEDULE_LESSON_FULL) + " " + cursor.getString(COL_SCHEDULE_CLASS));
//                time.setText("at " + cursor.getString(COL_SCHEDULE_TIME_START)+ " until "+cursor.getString(COL_SCHEDULE_TIME_END));
//                room.setText("in " + cursor.getString(COL_SCHEDULE_ROOM));
//                break;
//            }while (cursor.moveToNext());
//        }else{
//
//        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
//                if (cursor != null) {
//                    String locationSetting = Utility.getPreferredLocation(getActivity());
////                    Intent intent = new Intent(getActivity(), DetailActivity.class)
////                            .setData(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
//                    ((Callback)getActivity())
//                            .onItemSelected(ScheduleContract.WeatherEntry.buildWeatherLocationWithDate(
//                                    locationSetting, cursor.getLong(COL_WEATHER_DATE)
//                            ));
                ((Callback)getActivity())
                        .onItemSelected(Schedule.CONTENT_URI.buildUpon().appendPath("day").appendPath(cursor.getString(COL_SCHEDULE_DAY)).build());
                        DetailFragment.DAY = cursor.getString(COL_SCHEDULE_DAY);
////                    startActivity(intent);

//                ((Callback)getActivity())
//                            .onItemSelected();
//                Intent intent = new Intent(getActivity(), DetailActivity.class).setData(Schedule.CONTENT_URI.buildUpon().appendPath("day").appendPath(cursor.getString(2)).build());
//                Intent intent = new Intent(getActivity(), DetailActivity.class).setData(Schedule.CONTENT_URI.buildUpon().appendPath("day").appendPath(cursor.getString(COL_SCHEDULE_DAY)).build());
//
//                startActivity(intent);
                Log.d(LOG_TAG, Schedule.CONTENT_URI.buildUpon().appendPath("day").appendPath(cursor.getString(COL_SCHEDULE_DAY)).build().toString());
//                }
                mPosition = position;
            }
        });

        if(savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)){
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        mScheduleAdapter.setUseTodayLayout(mUseTodayLayout);
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        if(mPosition != ListView.INVALID_POSITION){
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onLocationChanged(){
        updateSchedule();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }


    private void openPreferredLocationInMap() {
        if(null != mScheduleAdapter){
            Cursor c = mScheduleAdapter.getCursor();
            if(null != c){
                c.moveToPosition(0);
                String posLat = c.getString(COL_COORD_LAT);
                String posLong = c.getString(COL_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                    Log.d(LOG_TAG, "Call " + geoLocation.toString() + ", receiving apps installed!");
                } else {
                    Log.d(LOG_TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
                }

            }
        }




    }

    private void updateSchedule() {
        MyScheduleSyncAdapter.syncImmediately(getActivity());
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String locationSetting = Utility.getPreferredLocation(getActivity());

        // Sort order: Ascending, by date.
        String sortOrder = ScheduleContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = ScheduleContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());
        Uri schedule = Schedule.CONTENT_URI;

        ScheduleDbHelper mOpenHelper = new ScheduleDbHelper(getActivity());
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

//        Cursor cursorLoader = db.rawQuery("", SCHEDULE_COLUMNS);

        String filter = Schedule.COLUMN_DAY + " NOT LIKE '' GROUP BY " + Schedule.COLUMN_DAY;


//        return db.rawQuery("SELECT * FROM "+Schedule.TABLE_NAME +" ORDER BY "+Schedule.COLUMN_DAY, SCHEDULE_COLUMNS);
        return new CursorLoader(getActivity(),
                schedule,
                SCHEDULE_COLUMNS,
                filter,
                null,
                Schedule.COLUMN_ID);

//        return new CursorLoader(getActivity(),
//                weatherForLocationUri,
//                FORECAST_COLUMNS,
//                null,
//                null,
//                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mScheduleAdapter.swapCursor(data);
        if(mPosition != ListView.INVALID_POSITION){
            mListView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        mScheduleAdapter.swapCursor(null);
    }

    public void setUseTodayLayout(boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
        if(mScheduleAdapter != null){
            mScheduleAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }

    public void update(){
        Log.d(LOG_TAG, "Update...");
        Intent alarmIntent = new Intent(getActivity(), MyScheduleService.AlarmReceiver.class);

        //Wrap in a pending intent which only fires once.
        PendingIntent pi = PendingIntent.getBroadcast(getActivity(), 0,alarmIntent,PendingIntent.FLAG_ONE_SHOT);//getBroadcast(context, 0, i, 0);

        AlarmManager am=(AlarmManager)getActivity().getSystemService(Context.ALARM_SERVICE);

        //Set the AlarmManager to wake up the system.
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pi);
    }
}
