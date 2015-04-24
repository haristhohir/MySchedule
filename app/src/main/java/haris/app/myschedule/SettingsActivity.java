package haris.app.myschedule;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import haris.app.myschedule.service.MyScheduleService;

/**
 * A {@link android.preference.PreferenceActivity} that presents a set of application settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {
    private  static final String sLOG_TAG = SettingsActivity.class.getSimpleName();
    private static Context context;


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent(){
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        // Add 'general' preferences, defined in the XML file
        addPreferencesFromResource(R.xml.pref_general);

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_user_id_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_notification_time_key)));

    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();
        Log.d(sLOG_TAG, stringValue);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String idTmp = prefs.getString(getApplicationContext().getString(R.string.pref_user_id_key),
                getApplicationContext().getString(R.string.pref_user_id_default));
        String timeTmp = prefs.getString(getApplicationContext().getString(R.string.pref_notification_time_key),
                getApplicationContext().getString(R.string.pref_notification_time_default));

        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
            if(!stringValue.equals(timeTmp)){
                Log.d(sLOG_TAG, "setAlarmOnly");
                update(false);
            }
        } else if(stringValue.length()>3){
            preference.setSummary(stringValue);
            if(!stringValue.equals(idTmp)){
                Log.d(sLOG_TAG, "requestToServer");
                update(true);
            }
        }
        return true;
    }

    public void update(boolean serverRequest){
        Intent alarmIntent = new Intent(getApplicationContext(), MyScheduleService.AlarmReceiver.class);
        String request;
        alarmIntent.putExtra("result", resultReceiver);
        if(serverRequest){
            alarmIntent.putExtra(Intent.EXTRA_TEXT, "requestToServer");
            request =  "requestToServer";
        }else {
            alarmIntent.putExtra(Intent.EXTRA_TEXT, "setAlarmOnly");
            request = "setAlarmOnly";
        }

        //Wrap in a pending intent which only fires once.
        PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), 0,alarmIntent,PendingIntent.FLAG_ONE_SHOT);//getBroadcast(context, 0, i, 0);
        AlarmManager am=(AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        //Set the AlarmManager to wake up the system.
        Log.d(sLOG_TAG,"time millis "+System.currentTimeMillis() );
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pi);



//        Intent sendIntent = new Intent(context, MyScheduleService.class);
//        sendIntent.putExtra(Intent.EXTRA_TEXT, request);
//        Log.d(sLOG_TAG, "INTENT  - Text extra update data " + request);
////        String request = intent.getStringExtra(Intent.EXTRA_TEXT);
//        sendIntent.putExtra(Intent.EXTRA_TEXT, request);
////            sendIntent.putExtra(MyScheduleService.LOCATION_QUERY_EXTRA, intent.getStringExtra(MyScheduleService.LOCATION_QUERY_EXTRA));
//        context.startService(sendIntent);

    }
    Handler handler = new Handler();
    final ResultReceiver resultReceiver = new ResultReceiver(handler) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
                Toast.makeText(getApplicationContext(), resultData.getString("result"), Toast.LENGTH_SHORT).show();

        }
    };
}