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

package haris.app.myschedule.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import haris.app.myschedule.MainActivity;
import haris.app.myschedule.R;
import haris.app.myschedule.ScheduleFragment;
import haris.app.myschedule.data.ScheduleContract;
import haris.app.myschedule.data.ScheduleDbHelper;


public class MyScheduleService extends IntentService {
    //    private ArrayAdapter<String> mForecastAdapter;
//    public static final String LOCATION_QUERY_EXTRA = "lqe";
    private final String LOG_TAG = MyScheduleService.class.getSimpleName();
    private  static final String sLOG_TAG = MyScheduleService.class.getSimpleName();

    private static TextView lessonName;
    private static TextView time;
    private static TextView room;
    private static TextView nextLabel;

    public MyScheduleService() {
        super("MySchedule");
    }
    static Context context;

    @Override
    protected void onHandleIntent(Intent intent) {
        context = getApplicationContext();
        if(intent.getStringExtra(Intent.EXTRA_TEXT) != "requestToServer"){
            setAlarmNotification();
            return;
        }

        Log.d(sLOG_TAG, "Starting sync");
// These two need to be declared outside the try/catch
// so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
// Will contain the raw JSON response as a string.
        String forecastJsonStr = null;
        String format = "json";
        String units = "metric";
        int numDays = 14;
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//            URL url = new URL("http://192.168.17.1/myschedule/index.php?id=" +
//                    ""+prefs.getString(getContext().getString(R.string.pref_user_id_key), getContext().getString(R.string.pref_user_id_default)));
//            URL url = new URL("http://192.168.0.101/myschedule/index.php");
            URL url = new URL("http://harisuddin.com/myschedule/index.php?id=" +
                    ""+prefs.getString(getApplicationContext().getString(R.string.pref_user_id_key), getApplicationContext().getString(R.string.pref_user_id_default)));
            Log.d(sLOG_TAG, url.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            Log.d(sLOG_TAG, "Connecting...");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }
            if (buffer.length() == 0) {

                return;
            }
            forecastJsonStr = buffer.toString();

            getDataJson(forecastJsonStr);

        } catch (IOException e) {
            Log.e(sLOG_TAG, "Error ", e);

        } catch (JSONException e) {
            Log.e(sLOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(sLOG_TAG, "Error closing stream", e);
                }
            }
        }
        return;
    }


    public static void setAlarmNotification(){
        cancelAlarm();
        Log.d(sLOG_TAG, "Set Alarm Notification");
        ScheduleDbHelper mOpenHelper = new ScheduleDbHelper(context);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor cursor;
        cursor = db.rawQuery("SELECT * FROM "+ ScheduleContract.Schedule.TABLE_NAME, null);
        Calendar calendar=Calendar.getInstance();

        if(cursor.moveToFirst()){
            do {
                String hourMinute = cursor.getString(ScheduleFragment.COL_SCHEDULE_TIME_START);
                String hour = hourMinute.substring(0,2);
                String minute = hourMinute.substring(3);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

                int intMinute = Integer.parseInt(minute);
                int intHour = Integer.parseInt(hour);
                String timeBefore =  prefs.getString(context.getString(R.string.pref_notification_time_key),
                        context.getString(R.string.pref_notification_time_default));

                int before = Integer.parseInt(timeBefore);
                Log.d(sLOG_TAG, "ALARM BEFORE "+before);
                if((intMinute-before) < 0){
                    intHour--;
                    intMinute = 60+(intMinute-before);
                }else {
                    intMinute -= before;
                }

                Log.d(sLOG_TAG, "DayId " + cursor.getString(ScheduleFragment.COL_SCHEDULE_DAY_ID) +
                        " startHour "+ intHour + " startMinute "+intMinute);



                calendar.set(Calendar.DAY_OF_WEEK, cursor.getInt(ScheduleFragment.COL_SCHEDULE_DAY_ID));
                calendar.set(Calendar.HOUR_OF_DAY, intHour);
                calendar.set(Calendar.MINUTE, intMinute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                setScheduleAlarm(calendar);
            }while (cursor.moveToNext());
        }
        db.close();
    }

    static int alarmRequestCode =1;

    public static void setScheduleAlarm(Calendar calendar){

        Log.d(sLOG_TAG, "Calendar hour : "+calendar.getTime().getHours()+", DATE : "+calendar.getTime().getDate());
        Log.d(sLOG_TAG, "SET ALARM AT 1 "+calendar.getTime());
        boolean passed = false;
        if(calendar.getTime().before(Calendar.getInstance().getTime())){
            passed = true;
            calendar.add(Calendar.DATE, 7);
            Log.d(sLOG_TAG, "TIME PASSED!!! at "+ calendar.getTime() );
        }
        Log.d(sLOG_TAG, "SET ALARM AT 2 "+calendar.getTime());
        AlarmManager alarmManager=(AlarmManager)context.getSystemService(context.ALARM_SERVICE);
        Intent i=new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent=PendingIntent.getBroadcast(context, alarmRequestCode, i, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),alarmManager.INTERVAL_DAY * 7,pendingIntent);
        alarmRequestCode++;
        if(passed){
            calendar.add(Calendar.DATE, -7);
            Log.d(sLOG_TAG, "TIME PASSED!!! change back "+ calendar.getTime() );
        }
    }

    public static boolean isPassed(Calendar calendar){
        Log.d(sLOG_TAG, "Input day " + calendar.getTime());
        Log.d(sLOG_TAG, "Current day " + Calendar.getInstance().getTime());

        if(calendar.getTime().getDate() < Calendar.getInstance().getTime().getDate()){
            Log.d(sLOG_TAG, "Compare date "+calendar.getTime().getDate()+" with "+Calendar.getInstance().getTime().getDate());
            return true;
        }else if(calendar.getTime().getDate() == Calendar.getInstance().getTime().getDate()){
            if(calendar.getTime().getHours() < Calendar.getInstance().getTime().getHours()){
                return true;
            }else if(calendar.getTime().getHours() == Calendar.getInstance().getTime().getHours()){
                if(calendar.getTime().getMinutes() < Calendar.getInstance().getTime().getMinutes()){
                    return true;
                }
            }
        }
        return false;
    }

    private static void cancelAlarm(){
        Log.d(sLOG_TAG, "CLEAR ALL ALARM");
        ScheduleDbHelper mOpenHelper = new ScheduleDbHelper(context);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM "+ ScheduleContract.Schedule.TABLE_NAME, null);

        Intent intent = new Intent(context, MainActivity.class);
        for(int i=0; i<cursor.getCount(); i++){
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, i, intent, 0);
            AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        }
        db.close();
    }


    private static void getDataJson(String schedule)  throws JSONException{
        try{
            JSONObject scheduleJson = new JSONObject(schedule);
            JSONObject user = scheduleJson.getJSONObject("user");
            String user_id = user.getString("id");

            Log.d(sLOG_TAG, user_id);

            String name = user.getString("name");
            String nick_name = user.getString("nick_name");
            String email = user.getString("email");

            JSONArray scheduleArray = scheduleJson.getJSONArray("schedule");

            Log.d(sLOG_TAG, "user id : " + user_id + " name : " + name + " nick name : " + nick_name + " email : " + email);

            Vector<ContentValues> cVVector = new Vector<ContentValues>(scheduleArray.length());

//            getContext().getContentResolver().delete(ScheduleContract.Schedule.SEQUENCE_URI, "", new String[]{});
            Log.d(sLOG_TAG, ScheduleContract.Schedule.CONTENT_URI.toString());

            ScheduleDbHelper mOpenHelper = new ScheduleDbHelper(context);
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();

            db.execSQL("UPDATE sqlite_sequence set seq='0';");

            db.execSQL("DELETE FROM  "+ScheduleContract.Schedule.TABLE_NAME + " WHERE 1");

//            context.getContentResolver().delete(ScheduleContract.Schedule.CONTENT_URI, "",new String[]{});
            if(scheduleArray.length()==0){

            }
            Log.d(sLOG_TAG, "ARRAY "+scheduleArray.length());
            for(int i =0; i < scheduleArray.length(); i++){
                JSONObject lessonSchedule = scheduleArray.getJSONObject(i);
                String id = lessonSchedule.getString("id");
                String day = lessonSchedule.getString("day");
                String day_id = lessonSchedule.getString("day_id");
                String timeStart = lessonSchedule.getString("time_start");
                String timeEnd = lessonSchedule.getString("time_end");
                String className = lessonSchedule.getString("class");
                String room = lessonSchedule.getString("room");
                String lessonFull = lessonSchedule.getString("lesson_name_full");
                String lessonShort = lessonSchedule.getString("lesson_name_short");

                Log.d(sLOG_TAG, "id schedule : " + id +
                                " day : " + day +
                                " day_id : " + day_id +
                                " time start : " + timeStart+
                                " time end : " + timeEnd +
                                " class name : " + className +
                                " room : " + room +
                                " lesson full : " + lessonFull +
                                " lesson short : " + lessonShort
                );

                ContentValues scheduleValues = new ContentValues();
                scheduleValues.put(ScheduleContract.Schedule.COLUMN_ID, id);
                scheduleValues.put(ScheduleContract.Schedule.COLUMN_DAY_ID, day_id);
                scheduleValues.put(ScheduleContract.Schedule.COLUMN_TIME_START, timeStart);
                scheduleValues.put(ScheduleContract.Schedule.COLUMN_TIME_END, timeEnd);
                scheduleValues.put(ScheduleContract.Schedule.COLUMN_CLASS, className);
                scheduleValues.put(ScheduleContract.Schedule.COLUMN_ROOM, room);
                scheduleValues.put(ScheduleContract.Schedule.COLUMN_LESSON_FULL, lessonFull);
                scheduleValues.put(ScheduleContract.Schedule.COLUMN_LESSON_SHORT, lessonShort);
                scheduleValues.put(ScheduleContract.Schedule.COLUMN_DAY, day);

                context.getContentResolver().insert(ScheduleContract.Schedule.CONTENT_URI, scheduleValues);

                cVVector.add(scheduleValues);

            }

            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);

                for (int i = 0; i < cvArray.length; i++){
                    Log.d(sLOG_TAG, "Array "+ cvArray[i]);
                }
            }
            Log.d(sLOG_TAG, "MySchedule Service Complete. " + cVVector.size() + " Inserted");

            db.close();

            setAlarmNotification();
            nextLesson();

        }catch(JSONException e) {
            Log.e(sLOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    public static void nextLesson(){
        LayoutInflater mInflater = LayoutInflater.from(context);
        View rootView = mInflater.inflate(R.layout.fragment_main, null);
        lessonName = (TextView)rootView.findViewById(R.id.lesson_textView);
        time = (TextView)rootView.findViewById(R.id.time_textView);
        room = (TextView)rootView.findViewById(R.id.room_textView);
        nextLabel = (TextView)rootView.findViewById(R.id.next_lesson_textView);


//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        String currentTime = sdf.format(new Date());
        Calendar calendar = Calendar.getInstance();
        int toDay = calendar.get(Calendar.DAY_OF_WEEK);

        Log.d(sLOG_TAG, "Today is " + toDay);
        Log.d(sLOG_TAG, "SEKARANG " + currentTime);

        ScheduleDbHelper mOpenHelper = new ScheduleDbHelper(context);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor cursor;

        cursor = db.rawQuery("SELECT distinct("+ ScheduleContract.Schedule.COLUMN_DAY+") FROM "+ ScheduleContract.Schedule.TABLE_NAME, null);
        if(cursor.moveToFirst()){
            do{
                Log.d(sLOG_TAG, "DAY "+cursor.getString(0));
            }while (cursor.moveToNext());
        }

        Cursor check = db.rawQuery("SELECT * FROM "+ ScheduleContract.Schedule.TABLE_NAME,null);
        cursor = db.rawQuery("SELECT * FROM "+ ScheduleContract.Schedule.TABLE_NAME+" WHERE day_id = '" + toDay+"' AND time_start > '"+ currentTime +"';",null);
        if(check.moveToFirst()){
            while (!cursor.moveToFirst()){
                if(toDay<7){
                    toDay++;
                }else {
                    toDay = 1;
                }
                Log.d(sLOG_TAG, "Change day_id to "+toDay);
                cursor = db.rawQuery("SELECT * FROM "+ ScheduleContract.Schedule.TABLE_NAME+" WHERE day_id = '" + toDay +"' AND time_start > '"+ "00:00" +"';",null);
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
                Log.d(sLOG_TAG,"LESSON " + cursor.getString(ScheduleFragment.COL_SCHEDULE_LESSON_FULL));
                lessonName.setText(cursor.getString(ScheduleFragment.COL_SCHEDULE_LESSON_FULL) +
                        " " + cursor.getString(ScheduleFragment.COL_SCHEDULE_CLASS));
                time.setText(cursor.getString(ScheduleFragment.COL_SCHEDULE_DAY) +
                        " at " + cursor.getString(ScheduleFragment.COL_SCHEDULE_TIME_START) +
                        " until "+cursor.getString(ScheduleFragment.COL_SCHEDULE_TIME_END));
                room.setText("in " + cursor.getString(ScheduleFragment.COL_SCHEDULE_ROOM));
                break;
            }while (cursor.moveToNext());
        }else{
            nextLabel.setText("USER ID NOT FOUND!");
            lessonName.setText("");
            time.setText("");
            room.setText("");
        }
        db.close();
    }

    public static class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(sLOG_TAG, "Receive Broadcast...");
            Intent sendIntent = new Intent(context, MyScheduleService.class);
            Log.d(sLOG_TAG, "INTENT  - Text extra update data "+intent.getStringExtra(Intent.EXTRA_TEXT));

            sendIntent.putExtra(Intent.EXTRA_TEXT, intent.getStringExtra(Intent.EXTRA_TEXT));
//            sendIntent.putExtra(MyScheduleService.LOCATION_QUERY_EXTRA, intent.getStringExtra(MyScheduleService.LOCATION_QUERY_EXTRA));
            context.startService(sendIntent);
            Log.d(sLOG_TAG, "Receive Broadcast.......");

        }
    }

}