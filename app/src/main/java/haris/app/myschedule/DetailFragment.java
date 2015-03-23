package haris.app.myschedule;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import haris.app.myschedule.data.ScheduleContract;
import haris.app.myschedule.data.ScheduleContract.Schedule;
import haris.app.myschedule.data.ScheduleContract.WeatherEntry;
import haris.app.myschedule.data.ScheduleDbHelper;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = DetailFragment.class.getSimpleName();
    private static final String SCHEDULE_SHARE_HASHTAG = "\n#MySchedule";
    static final String DETAIL_URI = "URI";
    private Uri mUri;
    public static String DAY = "";
    private ShareActionProvider mShareActionProvider;
    private String mSchedule;
    private static final int DETAIL_LOADER = 0;
    private static final String[] DETAIL_COLUMNS1 = {
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            WeatherEntry.COLUMN_HUMIDITY,
            WeatherEntry.COLUMN_PRESSURE,
            WeatherEntry.COLUMN_WIND_SPEED,
            WeatherEntry.COLUMN_DEGREES,
            WeatherEntry.COLUMN_WEATHER_ID,
// This works because the WeatherProvider returns location data joined with
// weather data, even though they're stored in two different tables.
            ScheduleContract.LocationEntry.COLUMN_LOCATION_SETTING
    };
    private static final String[] DETAIL_COLUMNS = {
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



    // These indices are tied to DETAIL_COLUMNS. If DETAIL_COLUMNS changes, these
// must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_WEATHER_HUMIDITY = 5;
    public static final int COL_WEATHER_PRESSURE = 6;
    public static final int COL_WEATHER_WIND_SPEED = 7;
    public static final int COL_WEATHER_DEGREES = 8;
    public static final int COL_WEATHER_CONDITION_ID = 9;

    private ScheduleDbHelper mOpenHelper;


    private ImageView mIconView;
    private TextView mFriendlyDateView;
    private TextView mDateView;
    private TextView mDescriptionView;
    private TextView mHighTempView;
    private TextView mLowTempView;
    private TextView mHumidityView;
    private TextView mWindView;
    private TextView mPressureView;
    private TextView mDay;
    private TextView mLessons;
    private ListView mListLessons;
    private LessonAdapter mLessonAdapter;
    private View rootView;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle argument = getArguments();
        if(argument != null){
            mUri = argument.getParcelable(DetailFragment.DETAIL_URI);
//            mOpenHelper.getReadableDatabase().query(DetailFragment.DETAIL_URI,null,null,null,null,null,null);
            Log.d(LOG_TAG, mUri.toString());
        }
        rootView = inflater.inflate(R.layout.shcedule_fragment_detail, container, false);
        mDay = (TextView) rootView.findViewById(R.id.day_textView);



        return rootView;
    }




    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
// Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment, menu);
// Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);
// Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
// If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if (mSchedule != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }

    void onLocationChanged(String newLocation){
        Uri uri = mUri;
        if(null !=  uri){
            long date = ScheduleContract.WeatherEntry.getDateFromUri(uri);
            Uri updateUri = ScheduleContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = updateUri;
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mSchedule + SCHEDULE_SHARE_HASHTAG);
        return shareIntent;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//        Log.v(LOG_TAG, "In onCreateLoader");
//        Intent intent = getActivity().getIntent();
//        if (intent == null || intent.getData() == null) {
//
//            return null;
//        }
//// Now create and return a CursorLoader that will take care of
//// creating a Cursor for the data being displayed.
//        return new CursorLoader(
//                getActivity(),
//                intent.getData(),
//                DETAIL_COLUMNS,
//                null,
//                null,
//                null
//        );
        if(null != mUri){
            Log.d(LOG_TAG, "Next "+mUri.getQueryParameter(Schedule.COLUMN_DAY));
            Log.d(LOG_TAG, "Next "+mUri.toString());

            return new CursorLoader(getActivity(),mUri,DETAIL_COLUMNS,Schedule.COLUMN_DAY+"= '"+ DAY +"' ",null,null);
        }
        return null;
    }
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//        mOpenHelper = new ScheduleDbHelper(getActivity());
//        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
//
//        Cursor cursor = db.rawQuery("SELECT * FROM "+Schedule.TABLE_NAME+" WHERE day = '" +DAY+ "';",null);
//        int scheduleId = 0;
//        String id = "";
        String day = "";
//        String timeStart = "";
//        String timeEnd = "";
//        String className = "";
//        String room = "";
//        String lessonFull = "";
//        String lessonShort = "";
//        mSchedule = "Share this";


        mLessonAdapter = new LessonAdapter(getActivity(), data, 0);
        mListLessons = (ListView)rootView.findViewById(R.id.list_detail_lessons_listView);
        mListLessons.setAdapter(mLessonAdapter);

        if(data.moveToFirst()){
            day = data.getString(COL_SCHEDULE_DAY);
            mSchedule = day+"\n";
//            int i = 0;
            do{
               mSchedule += data.getString(COL_SCHEDULE_LESSON_FULL)+"\n" +
                       "class " + data.getString(COL_SCHEDULE_CLASS) + "\n"+
                       "at " + data.getString(COL_SCHEDULE_TIME_START) +"\n" +
                       "in " + data.getString(COL_SCHEDULE_ROOM)+"\n";
//                if (i>0){
//                    lessonFull+="\n";
//                }
//                i++;
//                Log.d(LOG_TAG, data.getString(7));
//
//                id += data.getString(COL_SCHEDULE_ID);
//
//                timeStart += data.getString(COL_SCHEDULE_TIME_START);
//                timeEnd += data.getString(COL_SCHEDULE_TIME_END);
//                className += data.getString(COL_SCHEDULE_CLASS);
//                room += data.getString(COL_SCHEDULE_ROOM);
//                lessonFull += data.getString(COL_SCHEDULE_LESSON_FULL);
//                lessonShort += data.getString(COL_SCHEDULE_LESSON_SHORT);
//
            }while (data.moveToNext());
////            mFriendlyDateView.setText(lessonFull);
            mDay.setText(day);
//            mLessons.setText(lessonFull);
        }



        if (data != null && data.moveToFirst()) {
//            int scheduleId = data.getInt(COL_SCHEDULE_COLUMN_ID);
//            String id = data.getString(COL_SCHEDULE_ID);
//            String day = data.getString(COL_SCHEDULE_DAY);
//            String timeStart = data.getString(COL_SCHEDULE_TIME_START);
//            String timeEnd = data.getString(COL_SCHEDULE_TIME_END);
//            String className = data.getString(COL_SCHEDULE_CLASS);
//            String room = data.getString(COL_SCHEDULE_ROOM);
//            String lessonFull = data.getString(COL_SCHEDULE_LESSON_FULL);
//            String lessonShort = data.getString(COL_SCHEDULE_LESSON_SHORT);

//            mFriendlyDateView.setText(day+" "+lessonFull);

            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareForecastIntent());
            }
        }
    }
    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }


}