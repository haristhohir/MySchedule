package haris.app.myschedule.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import haris.app.myschedule.MainActivity;
import haris.app.myschedule.R;
import haris.app.myschedule.ScheduleFragment;
import haris.app.myschedule.data.ScheduleContract;
import haris.app.myschedule.data.ScheduleDbHelper;

/**
 * Created by pi on 23/03/15.
 */
public class NotificationReceiver extends BroadcastReceiver {
    Context context;
    final String LOG_TAG = NotificationReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
//        Toast.makeText(context, "Get Ready to Next Lesson!",Toast.LENGTH_LONG).show();
        Log.d(LOG_TAG, "NOTIFICATION RECEIVER");
        createNotification();
    }

    private void createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String currentDateandTime = sdf.format(new Date());

        Intent i = new Intent(context, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(context, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(intent);
        builder.setTicker("Custom Notification");
        builder.setSmallIcon(R.drawable.ic_schedule_white);
        builder.setAutoCancel(true);
        builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        builder.setVibrate(new long[]{1000, 1000, 500, 0, 1000, 0, 500});
        Notification notification = builder.build();

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification);


        String currentTime = sdf.format(new Date());
        Calendar calendar = Calendar.getInstance();
        int toDay = calendar.get(Calendar.DAY_OF_WEEK);

        Log.d(LOG_TAG, "Today is " + toDay);
        Log.d(LOG_TAG, "SEKARANG " + currentTime);

        ScheduleDbHelper mOpenHelper = new ScheduleDbHelper(context);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor cursor;

        Cursor check = db.rawQuery("SELECT * FROM "+ ScheduleContract.Schedule.TABLE_NAME,null);

        cursor = db.rawQuery("SELECT * FROM "+ ScheduleContract.Schedule.TABLE_NAME+" WHERE day_id = '" + toDay+"' AND time_start > '"+ currentTime +"';",null);
        if(check.moveToFirst()){
            while (!cursor.moveToFirst()){
                if(toDay<7){
                    toDay++;
                }else {
                    toDay = 1;
                }
                cursor = db.rawQuery("SELECT * FROM "+ ScheduleContract.Schedule.TABLE_NAME+" WHERE day_id = '" + toDay +"' AND time_start > '"+ "00:00" +"';",null);
            }
        }


        if(cursor.moveToFirst()){
            do{
                Log.d(LOG_TAG,"LESSON " + cursor.getString(ScheduleFragment.COL_SCHEDULE_LESSON_FULL));

                contentView.setTextViewText(R.id.lesson_name_textView, cursor.getString(ScheduleFragment.COL_SCHEDULE_LESSON_FULL));
                contentView.setTextViewText(R.id.time_and_room_textView,"At "+ cursor.getString(ScheduleFragment.COL_SCHEDULE_TIME_START) +
                        " in " + cursor.getString(ScheduleFragment.COL_SCHEDULE_ROOM));
                contentView.setTextViewText(R.id.time_now_textView, currentDateandTime);
                RemoteViews expandedView = new RemoteViews(context.getPackageName(), R.layout.notification_expanded);
                if (Build.VERSION.SDK_INT >= 16) {
                    expandedView.setTextViewText(R.id.lesson_item_name, cursor.getString(ScheduleFragment.COL_SCHEDULE_LESSON_FULL));
                    expandedView.setTextViewText(R.id.lesson_item_class, "Class " + cursor.getString(ScheduleFragment.COL_SCHEDULE_CLASS));
                    expandedView.setTextViewText(R.id.lesson_item_time,
                            cursor.getString(ScheduleFragment.COL_SCHEDULE_DAY)+
                            " at " + cursor.getString(ScheduleFragment.COL_SCHEDULE_TIME_START) +
                                    " until "+ cursor.getString(ScheduleFragment.COL_SCHEDULE_TIME_END)
                    );
                    expandedView.setTextViewText(R.id.lesson_item_room, "In " + cursor.getString(ScheduleFragment.COL_SCHEDULE_ROOM));

                    if(Build.VERSION.SDK_INT >= 21){
                        Intent push = new Intent();
                        push.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        push.setClass(context, MainActivity.class);
//                        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
//                                push, PendingIntent.FLAG_CANCEL_CURRENT);
                        builder
                                .setContent(expandedView)
                                .setVibrate(new long[]{1000, 1000, 500})
                                .setFullScreenIntent(intent, true);
                    }
                    notification = builder.build();
                }
                if (Build.VERSION.SDK_INT >= 16) {
                    notification.bigContentView = expandedView;
                }

                if(Build.VERSION.SDK_INT<21){

                    LayoutInflater mInflater = LayoutInflater.from(context);
                    View layout = mInflater.inflate(R.layout.custom_toast, null);
                    TextView lessonName = (TextView) layout.findViewById(R.id.lesson_item_name);
                    TextView lessonClass = (TextView) layout.findViewById(R.id.lesson_item_class);
                    TextView lessonTime = (TextView) layout.findViewById(R.id.lesson_item_time);

                    lessonName.setText(cursor.getString(ScheduleFragment.COL_SCHEDULE_LESSON_FULL));
                    lessonClass.setText("Class " + cursor.getString(ScheduleFragment.COL_SCHEDULE_CLASS));
                    lessonTime.setText("At " + cursor.getString(ScheduleFragment.COL_SCHEDULE_TIME_START) +
                            " until "+ cursor.getString(ScheduleFragment.COL_SCHEDULE_TIME_END));

                    Toast toast = new Toast(context);
                    toast.setGravity(Gravity.TOP, 0, 0);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.setView(layout);
                    toast.show();
                }
                break;
            }while (cursor.moveToNext());
        }
        db.close();
        notification.contentView = contentView;
        NotificationManager nm = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        nm.notify(0, notification);
    }
}
