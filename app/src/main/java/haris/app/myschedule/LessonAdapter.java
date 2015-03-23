package haris.app.myschedule;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by harry on 19/03/15.
 */
public class LessonAdapter extends CursorAdapter {


    public LessonAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_lessons, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder)view.getTag();

        viewHolder.lessonName.setText(cursor.getString(7));
        viewHolder.lessonClass.setText("Class " + cursor.getString(5));
        viewHolder.lessonTime.setText("At " + cursor.getString(3) + " until "+cursor.getString(4));
        viewHolder.lessonRoom.setText("In room "+cursor.getString(6));

    }

    public static class ViewHolder {
        public final TextView lessonName;
        public final TextView lessonClass;
        public final TextView lessonTime;
        public final TextView lessonRoom;

        public ViewHolder(View view) {
            lessonName = (TextView) view.findViewById(R.id.lesson_item_name);
            lessonClass = (TextView) view.findViewById(R.id.lesson_item_class);
            lessonTime = (TextView) view.findViewById(R.id.lesson_item_time);
            lessonRoom = (TextView) view.findViewById(R.id.lesson_item_room);

        }
    }
}
