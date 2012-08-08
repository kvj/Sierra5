package org.kvj.sierra5.plugins.providers.contact;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ContactSyncService extends Service {

	private static final Object sSyncAdapterLock = new Object();

	private static final String TAG = "ContactService";

	private static ContactSyncAdapter sSyncAdapter = null;

	@Override
	public void onCreate() {
		Log.i(TAG, "Created");
		synchronized (sSyncAdapterLock) {
			if (sSyncAdapter == null) {
				sSyncAdapter = new ContactSyncAdapter(getApplicationContext(),
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
