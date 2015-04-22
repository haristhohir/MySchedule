package haris.app.myschedule.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import haris.app.myschedule.data.ScheduleContract.Schedule;

/**
 * Manages a local database for weather data.
 */
public class ScheduleDbHelper extends SQLiteOpenHelper {


    private static final int DATABASE_VERSION = 2;

    static final String DATABASE_NAME = "schedule.db";

    public ScheduleDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        final String SQL_CREATE_SCHEDULE_TABLE = "CREATE TABLE " + Schedule.TABLE_NAME + " (" +
                Schedule._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "+
                Schedule.COLUMN_ID + " INTEGER NOT NULL, "+
                Schedule.COLUMN_DAY + " TEXT NOT NULL, "+
                Schedule.COLUMN_TIME_START + " TEXT NOT NULL, "+
                Schedule.COLUMN_TIME_END + " TEXT NOT NULL, "+
                Schedule.COLUMN_CLASS + " TEXT NOT NULL, "+
                Schedule.COLUMN_ROOM + " TEXT NOT NULL, "+
                Schedule.COLUMN_LESSON_FULL  + " TEXT NOT NULL, "+
                Schedule.COLUMN_LESSON_SHORT + " TEXT NOT NULL, " +
                Schedule.COLUMN_DAY_ID + " TEXT NOT NULL);";

        sqLiteDatabase.execSQL(SQL_CREATE_SCHEDULE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Schedule.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }


}
