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

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

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
//        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_enable_notification_key)));
//        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_enable_ringtone_key)));
//        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_enable_vibration_key)));


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

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
            update(false);
        } else if(stringValue.length()>3){
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
            update(true);
        }
        return true;
    }

    public void update(boolean serverRequest){
        Intent alarmIntent = new Intent(getApplicationContext(), MyScheduleService.AlarmReceiver.class);
        String request;
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
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pi);



//        Intent sendIntent = new Intent(context, MyScheduleService.class);
//        sendIntent.putExtra(Intent.EXTRA_TEXT, request);
//        Log.d(sLOG_TAG, "INTENT  - Text extra update data " + request);
////        String request = intent.getStringExtra(Intent.EXTRA_TEXT);
//        sendIntent.putExtra(Intent.EXTRA_TEXT, request);
////            sendIntent.putExtra(MyScheduleService.LOCATION_QUERY_EXTRA, intent.getStringExtra(MyScheduleService.LOCATION_QUERY_EXTRA));
//        context.startService(sendIntent);

    }
}