package haris.app.myschedule.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import haris.app.myschedule.MainActivity;
import haris.app.myschedule.R;
import haris.app.myschedule.ScheduleFragment;
import haris.app.myschedule.Utility;
import haris.app.myschedule.data.ScheduleContract;
import haris.app.myschedule.data.ScheduleDbHelper;

public class MyScheduleSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = MyScheduleSyncAdapter.class.getSimpleName();
    public final static String sLOG_TAG = MyScheduleSyncAdapter.class.getSimpleName();


    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            ScheduleContract.WeatherEntry.COLUMN_WEATHER_ID,
            ScheduleContract.WeatherEntry.COLUMN_MAX_TEMP,
            ScheduleContract.WeatherEntry.COLUMN_MIN_TEMP,
            ScheduleContract.WeatherEntry.COLUMN_SHORT_DESC
    };
    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;

    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;
    // Interval at which to sync with the weather, in seconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;

    public MyScheduleSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting sync");

        String locationQuery = Utility.getPreferredLocation(getContext());
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
// Construct the URL for the OpenWeatherMap query
// Possible parameters are avaiable at OWM's forecast API page, at
// http://openweathermap.org/API#forecast
//            final String FORECAST_BASE_URL =
//                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
//            final String QUERY_PARAM = "q";
//            final String FORMAT_PARAM = "mode";
//            final String UNITS_PARAM = "units";
//            final String DAYS_PARAM = "cnt";
//            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
//                    .appendQueryParameter(QUERY_PARAM, locationQuery)
//                    .appendQueryParameter(FORMAT_PARAM, format)
//                    .appendQueryParameter(UNITS_PARAM, units)
//                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
//                    .build();
//            URL url = new URL(builtUri.toString());
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
//            URL url = new URL("http://192.168.17.1/myschedule/index.php?id=" +
//                    ""+prefs.getString(getContext().getString(R.string.pref_user_id_key), getContext().getString(R.string.pref_user_id_default)));
//            URL url = new URL("http://192.168.0.101/myschedule/index.php");
            URL url = new URL("http://haris.esy.es/myschedule/index.php?id=" +
                            ""+prefs.getString(getContext().getString(R.string.pref_user_id_key), getContext().getString(R.string.pref_user_id_default)));
            Log.d(LOG_TAG, url.toString());


// Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
// Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
// Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
// Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
// But it does make debugging a *lot* easier if you print out the completed
// buffer for debugging.
                buffer.append(line + "\n");
            }
            if (buffer.length() == 0) {
// Stream was empty. No point in parsing.
                return;
            }
            forecastJsonStr = buffer.toString();
//            getWeatherDataFromJson(forecastJsonStr, locationQuery);

            getScheduleDataFromJson(forecastJsonStr);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
// If the code didn't successfully get the weather data, there's no point in attempting
// to parse it.
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return;

    }

    private void getScheduleDataFromJson(String schedule) throws JSONException{
        try{
            JSONObject scheduleJson = new JSONObject(schedule);
            JSONObject user = scheduleJson.getJSONObject("user");
            String user_id = user.getString("id");

            Log.d(LOG_TAG, user_id);

            String name = user.getString("name");
            String nick_name = user.getString("nick_name");
            String email = user.getString("email");

            JSONArray scheduleArray = scheduleJson.getJSONArray("schedule");

            Log.d(LOG_TAG, "user id : " + user_id + " name : " + name + " nick name : " + nick_name + " email : " + email);

            Vector<ContentValues> cVVector = new Vector<ContentValues>(scheduleArray.length());

//            getContext().getContentResolver().delete(ScheduleContract.Schedule.SEQUENCE_URI, "", new String[]{});
            Log.d(LOG_TAG, ScheduleContract.Schedule.CONTENT_URI.toString());

            ScheduleDbHelper mOpenHelper = new ScheduleDbHelper(getContext());
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();

            db.execSQL("UPDATE sqlite_sequence set seq='0';");

            getContext().getContentResolver().delete(ScheduleContract.Schedule.CONTENT_URI, "",new String[]{});
            if(scheduleArray.length()==0){

            }
            Log.d(LOG_TAG, "ARRAY "+scheduleArray.length());
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

                Log.d(LOG_TAG, "id schedule : " + id +
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

                getContext().getContentResolver().insert(ScheduleContract.Schedule.CONTENT_URI, scheduleValues);

                cVVector.add(scheduleValues);

            }

            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);

                for (int i = 0; i < cvArray.length; i++){
                    Log.d(LOG_TAG, "Array "+ cvArray[i]);
                }
                notifyWeather();
            }
            Log.d(LOG_TAG, "MySchedule Service Complete. " + cVVector.size() + " Inserted");


        }catch(JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void getWeatherDataFromJson(String forecastJsonStr,
                                        String locationSetting)
            throws JSONException {
        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy: constructor takes the JSON string and converts it
        // into an Object hierarchy for us.
        // These are the names of the JSON objects that need to be extracted.
        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";
        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";
        // Weather information. Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";
        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";
        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);
            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);
            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);
            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());
            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.
            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.
            Time dayTime = new Time();
            dayTime.setToNow();
            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
            // now we work exclusively in UTC
            dayTime = new Time();
            for(int i = 0; i < weatherArray.length(); i++) {
                // These are the values that will be collected.
                long dateTime;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;
                double high;
                double low;
                String description;
                int weatherId;
                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                pressure = dayForecast.getDouble(OWM_PRESSURE);
                humidity = dayForecast.getInt(OWM_HUMIDITY);
                windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);
                // Description is in a child array called "weather", which is 1 element long.
                // That element also contains a weather code.
                JSONObject weatherObject =
                        dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);
                // Temperatures are in a child object called "temp". Try not to name variables
                // "temp" when working with temperature. It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                high = temperatureObject.getDouble(OWM_MAX);
                low = temperatureObject.getDouble(OWM_MIN);
                ContentValues weatherValues = new ContentValues();
                weatherValues.put(ScheduleContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(ScheduleContract.WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(ScheduleContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(ScheduleContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(ScheduleContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(ScheduleContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(ScheduleContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(ScheduleContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(ScheduleContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(ScheduleContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);
                cVVector.add(weatherValues);
            }
            int inserted = 0;
            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                getContext().getContentResolver().bulkInsert(ScheduleContract.WeatherEntry.CONTENT_URI, cvArray);

                // delete old data so we don't build up an endless history
                getContext().getContentResolver().delete(ScheduleContract.WeatherEntry.CONTENT_URI,
                        ScheduleContract.WeatherEntry.COLUMN_DATE + " <= ?",
                        new String[] {Long.toString(dayTime.setJulianDay(julianStartDay-1))});


                notifyWeather();
            }
            Log.d(LOG_TAG, "Sunshine Service Complete. " + cVVector.size() + " Inserted");
            createNotification();
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long locationId;
        // First, check if the location with this city name exists in the db
        Cursor locationCursor = getContext().getContentResolver().query(
                ScheduleContract.LocationEntry.CONTENT_URI,
                new String[]{ScheduleContract.LocationEntry._ID},
                ScheduleContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);
        if (locationCursor.moveToFirst()) {
            int locationIdIndex = locationCursor.getColumnIndex(ScheduleContract.LocationEntry._ID);
            locationId = locationCursor.getLong(locationIdIndex);
        } else {
            // Now that the content provider is set up, inserting rows of data is pretty simple.
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues locationValues = new ContentValues();
            // Then add the data, along with the corresponding name of the data type,
            // so the content provider knows what kind of value is being inserted.
            locationValues.put(ScheduleContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(ScheduleContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(ScheduleContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(ScheduleContract.LocationEntry.COLUMN_COORD_LONG, lon);
            // Finally, insert location data into the database.
            Uri insertedUri = getContext().getContentResolver().insert(
                    ScheduleContract.LocationEntry.CONTENT_URI,
                    locationValues
            );
            // The resulting URI contains the ID for the row. Extract the locationId from the Uri.
            locationId = ContentUris.parseId(insertedUri);
        }
        locationCursor.close();
        // Wait, that worked? Yes!
        return locationId;
    }


    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */

    static Context MyContext;

    public static void syncImmediately(Context context) {
        MyContext = context;
        Log.d(sLOG_TAG, "This is a message when sync Before");
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
        cancelAlarm();
        setAlarmNotification();
        Log.d(sLOG_TAG, "This is a message when sync After");
    }


    public static void setAlarmNotification(){
        Log.d(sLOG_TAG, "Set Alarm Notification");
        ScheduleDbHelper mOpenHelper = new ScheduleDbHelper(MyContext);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor cursor;
        cursor = db.rawQuery("SELECT * FROM "+ ScheduleContract.Schedule.TABLE_NAME, null);
        Calendar calendar=Calendar.getInstance();

        if(cursor.moveToFirst()){
            do {
                String hourMinute = cursor.getString(ScheduleFragment.COL_SCHEDULE_TIME_START);
                String hour = hourMinute.substring(0,2);
                String minute = hourMinute.substring(3);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MyContext);

                int intMinute = Integer.parseInt(minute);
                int intHour = Integer.parseInt(hour);
                String timeBefore =  prefs.getString(MyContext.getString(R.string.pref_notification_time_key),
                        MyContext.getString(R.string.pref_notification_time_default));

                int before = Integer.parseInt(timeBefore);
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
    }

    static int alarmRequestCode =1;

    public static void setScheduleAlarm(Calendar calendar){
//        Toast.makeText(MyContext, "Alarm actived at " + calendar.getTime(), Toast.LENGTH_LONG).show();

        AlarmManager alarmManager=(AlarmManager)MyContext.getSystemService(MyContext.ALARM_SERVICE);
        Intent i=new Intent(MyContext, NotificationReceiver.class);
        PendingIntent pendingIntent=PendingIntent.getBroadcast(MyContext, alarmRequestCode, i, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),alarmManager.INTERVAL_DAY * 7,pendingIntent);
        alarmRequestCode++;
    }

    private static void cancelAlarm(){
        Log.d(sLOG_TAG, "CLEAR ALL ALARM");
        ScheduleDbHelper mOpenHelper = new ScheduleDbHelper(MyContext);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM "+ ScheduleContract.Schedule.TABLE_NAME, null);

        Intent intent = new Intent(MyContext, MainActivity.class);
        for(int i=0; i<cursor.getCount(); i++){
            PendingIntent pendingIntent = PendingIntent.getBroadcast(MyContext, i, intent, 0);
            AlarmManager alarmManager = (AlarmManager)MyContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        }


    }



    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }


    private static void onAccountCreated(Account newAccount, Context context) {
        /*
        * Since we've created an account
        */
        MyScheduleSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
        * Without calling setSyncAutomatically, our periodic sync will not be enabled.
        */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
        * Finally, let's do a sync to get things started
        */
        syncImmediately(context);
    }


    private void createNotification() {
        Context context = getContext();
        // BEGIN_INCLUDE(notificationCompat)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        // END_INCLUDE(notificationCompat)

        // BEGIN_INCLUDE(intent)
        //Create Intent to launch this Activity again if the notification is clicked.
        Intent i = new Intent(context, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(context, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(intent);
        // END_INCLUDE(intent)

        // BEGIN_INCLUDE(ticker)
        // Sets the ticker text
        builder.setTicker("Custom Notification");

        // Sets the small icon for the ticker
        builder.setSmallIcon(R.drawable.ic_launcher);
        // END_INCLUDE(ticker)

        // BEGIN_INCLUDE(buildNotification)
        // Cancel the notification when clicked
        builder.setAutoCancel(true);

        builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        builder.setVibrate(new long[]{1000, 1000, 500, 0, 1000, 0, 500});

        // Build the notification
        Notification notification = builder.build();
        // END_INCLUDE(buildNotification)

        // BEGIN_INCLUDE(customLayout)
        // Inflate the notification layout as RemoteViews
        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification);

        // Set text on a TextView in the RemoteViews programmatically.
        String time = DateFormat.getTimeInstance().format(new Date()).toString();

        final String text = context.getResources().getString(R.string.collapsed, time);
        contentView.setTextViewText(R.id.lesson_name_textView,time);

        /* Workaround: Need to set the content view here directly on the notification.
         * NotificationCompatBuilder contains a bug that prevents this from working on platform
         * versions HoneyComb.
         * See https://code.google.com/p/android/issues/detail?id=30495
         */
        notification.contentView = contentView;

        // Add a big content view to the notification if supported.
        // Support for expanded notifications was added in API level 16.
        // (The normal contentView is shown when the notification is collapsed, when expanded the
        // big content view set here is displayed.)
        if (Build.VERSION.SDK_INT >= 16) {
            // Inflate and set the layout for the expanded notification view
            RemoteViews expandedView =
                    new RemoteViews(context.getPackageName(), R.layout.notification_expanded);
            notification.bigContentView = expandedView;
        }
        // END_INCLUDE(customLayout)

        // START_INCLUDE(notify)
        // Use the NotificationManager to show the notification
        NotificationManager nm = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        nm.notify(0, notification);
        // END_INCLUDE(notify)
    }

    private void notifyWeather() {
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));
        if ( displayNotifications ) {
            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastSync = prefs.getLong(lastNotificationKey, 0);
            if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
                // Last sync was more than 1 day ago, let's send a notification with the weather.
                String locationQuery = Utility.getPreferredLocation(context);
                Uri weatherUri = ScheduleContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());
                // we'll query our contentProvider, as always
                Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);
                if (cursor.moveToFirst()) {
                    int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                    double high = cursor.getDouble(INDEX_MAX_TEMP);
                    double low = cursor.getDouble(INDEX_MIN_TEMP);
                    String desc = cursor.getString(INDEX_SHORT_DESC);
                    int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
                    Resources resources = context.getResources();
                    Bitmap largeIcon = BitmapFactory.decodeResource(resources,
                            Utility.getArtResourceForWeatherCondition(weatherId));
                    String title = context.getString(R.string.app_name);
                    // Define the text of the forecast.
                    String contentText = String.format(context.getString(R.string.format_notification),
                            desc,
                            Utility.formatTemperature(context, high),
                            Utility.formatTemperature(context, low));
                    // NotificationCompatBuilder is a very convenient way to build backward-compatible
                    // notifications. Just throw in some data.
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(getContext())
                                    .setColor(resources.getColor(R.color.light_blue))
                                    .setSmallIcon(iconId)
                                    .setLargeIcon(largeIcon)
                                    .setContentTitle(title)
                                    .setContentText(contentText);
                    // Make something interesting happen when the user clicks on the notification.
                    // In this case, opening the app is sufficient.
                    Intent resultIntent = new Intent(context, MainActivity.class);
                    // The stack builder object will contain an artificial back stack for the
                    // started Activity.
                    // This ensures that navigating backward from the Activity leads out of
                    // your application to the Home screen.

                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    mBuilder.setContentIntent(resultPendingIntent);
                    NotificationManager mNotificationManager =
                            (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    // WEATHER_NOTIFICATION_ID allows you to update the notification later on.
                    mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());
                    //refreshing last sync
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastNotificationKey, System.currentTimeMillis());
                    editor.commit();
                }
                cursor.close();
            }
        }
    }
}