package org.kvj.sierra5.plugins.providers.calendar;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class CalendarSyncService extends Service {

	private static final Object sSyncAdapterLock = new Object();

	private static final String TAG = "CalendarService";

	private static CalendarSyncAdapter sSyncAdapter = null;

	@Override
	public void onCreate() {
		Log.i(TAG, "Created");
		synchronized (sSyncAdapterLock) {
			if (sSyncAdapter == null) {
				sSyncAdapter = new CalendarSyncAdapter(getApplicationContext(),
						true);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "Bound");
		return sSyncAdapter.getSyncAdapterBinder();
	}

}
