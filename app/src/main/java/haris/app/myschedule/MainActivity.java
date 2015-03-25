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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Calendar;

import haris.app.myschedule.service.NotificationReceiver;

public class MainActivity extends ActionBarActivity implements ScheduleFragment.Callback {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    private String mLocation;
    private boolean mTwoPane;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FontsOverride.setDefaultFont(this, "DEFAULT", "fonts/Roboto-Regular.ttf");
        FontsOverride.setDefaultFont(this, "MONOSPACE", "fonts/Roboto-Regular.ttf");
        FontsOverride.setDefaultFont(this, "SERIF", "fonts/Roboto-Regular.ttf");
        FontsOverride.setDefaultFont(this, "SANS_SERIF", "fonts/Roboto-Regular.ttf");
        setContentView(R.layout.activity_main);
        if(findViewById(R.id.schedule_detail_container)!=null){
            mTwoPane = true;
            if(savedInstanceState == null){
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.schedule_detail_container, new DetailFragment(),DETAILFRAGMENT_TAG)
                        .commit();
            }
        }else{
            mTwoPane = false;
            getSupportActionBar().setElevation(0f);
        }

        ScheduleFragment scheduleFragment = ((ScheduleFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_forecast));
        scheduleFragment.setUseTodayLayout(!mTwoPane);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
//        if(id == R.id.action_notification){
////            createNotification();
//            Calendar calendar=Calendar.getInstance();
//
//            SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
//            SimpleDateFormat minuteFormat = new SimpleDateFormat("mm");
//
//            String currentHour = hourFormat.format(new Date());
//            String currentMinute = minuteFormat.format(new Date());
//
//            int toDay = calendar.get(Calendar.DAY_OF_WEEK);
//
//            calendar.set(Calendar.DAY_OF_WEEK, toDay);
//            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(currentHour));
//            calendar.set(Calendar.MINUTE, Integer.parseInt(currentMinute)+1);
//            calendar.set(Calendar.SECOND, 0);
//            calendar.set(Calendar.MILLISECOND, 0);
//            setScheduleAlarm(calendar);
//            calendar.set(Calendar.DAY_OF_WEEK, toDay);
//            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(currentHour));
//            calendar.set(Calendar.MINUTE, Integer.parseInt(currentMinute)+2);
//            calendar.set(Calendar.SECOND, 0);
//            calendar.set(Calendar.MILLISECOND, 0);
//            setScheduleAlarm(calendar);
//
//        }
        return super.onOptionsItemSelected(item);
    }
    int alarmRequestCode =111;

    public void setScheduleAlarm(Calendar calendar){
        Toast.makeText(this, "Alarm actived at " + calendar.getTime(), Toast.LENGTH_LONG).show();

        AlarmManager alarmManager=(AlarmManager)getSystemService(ALARM_SERVICE);
        Intent i=new Intent(this, NotificationReceiver.class);
        i.putExtra(Intent.EXTRA_TEXT, "setAlarmOnly");
        PendingIntent pendingIntent=PendingIntent.getBroadcast(this, alarmRequestCode, i, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
//        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),alarmManager.INTERVAL_DAY * 7,pendingIntent);
        alarmRequestCode++;
    }


    @Override
    protected void onResume(){
        super.onResume();

    }

    @Override
    public void onItemSelected(Uri contentUri) {
        if(mTwoPane){
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.DETAIL_URI, contentUri);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.schedule_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        }else{
            Intent intent = new Intent(this, DetailActivity.class).setData(contentUri);
            startActivity(intent);
        }
    }


}
