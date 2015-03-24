package haris.app.myschedule;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import haris.app.myschedule.data.ScheduleContract;
import haris.app.myschedule.data.ScheduleDbHelper;

/**
 * {@link ScheduleAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ScheduleAdapter extends CursorAdapter {

    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;
    public final String LOG_TAG = ScheduleAdapter.class.getSimpleName();
    private boolean mUseTodayLayout = true;


    public ScheduleAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }
    /**
     * Prepare the weather high/lows for presentation.
     */

    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;
        public final TextView dayView;
        public final TextView lessonName;
        public final TextView lessonStart;
        public final TextView lessonRoom;




        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
            dayView = (TextView) view.findViewById(R.id.list_item_day);
            lessonName = (TextView)view.findViewById(R.id.lesson_item_name);
            lessonStart = (TextView)view.findViewById(R.id.lesson_item_time_start);
            lessonRoom = (TextView)view.findViewById(R.id.lesson_item_room);


        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
//        View view = LayoutInflater.from(context).inflate(R.layout.list_item_forecast, parent, false);
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;

//        if(viewType == VIEW_TYPE_TODAY){
//            layoutId = R.layout.list_item_forecast_today;
//            layoutId = R.layout.list_item_schedule_next;
//        }else{
            layoutId = R.layout.list_item_schedule;
//        }


        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }
    /*
    This is where we fill-in the views with the contents of the cursor.
    */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
// our view is pretty simple here --- just a text view
// we'll keep the UI functional with a simple (and slow!) binding.
//        TextView tv = (TextView)view;
//        tv.setText(convertCursorRowToUXFormat(cursor));

        ViewHolder viewHolder = (ViewHolder)view.getTag();
        int viewType = getItemViewType(cursor.getPosition());
        switch (viewType){
            case VIEW_TYPE_TODAY :{
//                viewHolder.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(cursor.getInt(ScheduleFragment.COL_WEATHER_CONDITION_ID)));
                break;
            }
            case VIEW_TYPE_FUTURE_DAY:{
//                viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(cursor.getInt(ScheduleFragment.COL_WEATHER_CONDITION_ID)));
                break;
            }
        }



//        viewHolder.dayView.setText(cursor.getString(2));

        String day = cursor.getString(2);
        viewHolder.dayView.setText(day);
//        viewHolder.lessonView.setText(cursor.getString(7));
        ScheduleDbHelper mOpenHelper = new ScheduleDbHelper(context);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();


        Cursor cursorDay = db.rawQuery("SELECT * FROM "+ ScheduleContract.Schedule.TABLE_NAME+" WHERE day = '" + day+"';",null);

        viewHolder.lessonName.setText("");
        viewHolder.lessonStart.setText("");
        viewHolder.lessonRoom.setText("");


        int i = 0;
        if(cursorDay.moveToFirst()){
            do{
                if (i>0){
                    viewHolder.lessonName.append("\n");
                    viewHolder.lessonStart.append("\n");
                    viewHolder.lessonRoom.append("\n");

                }
                i++;
                viewHolder.lessonName.append(cursorDay.getString(8));
                viewHolder.lessonStart.append(cursorDay.getString(3));
                viewHolder.lessonRoom.append(cursorDay.getString(6));

                Log.d(LOG_TAG, " day "+ day + " Schedule "+cursorDay.getString(8));

//                ListView lessonsList = (ListView)view.findViewById(R.id.listView_lesson);
//                LessonAdapter lessonAdapter = new LessonAdapter(context,cursorDay,R.layout.list_item_lessons );
//                lessonsList.setAdapter(lessonAdapter);
            }while (cursorDay.moveToNext());
        }
        db.close();
    }

    public void setUseTodayLayout(boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

}