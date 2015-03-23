package haris.app.myschedule.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MyScheduleSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static MyScheduleSyncAdapter sMyScheduleSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("MyScheduleSyncService", "onCreate - MyScheduleSyncService");
        synchronized (sSyncAdapterLock) {
            if (sMyScheduleSyncAdapter == null) {
                sMyScheduleSyncAdapter = new MyScheduleSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sMyScheduleSyncAdapter.getSyncAdapterBinder();
    }
}