package org.kvj.sierra5.plugins.providers.contact;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

public class ContactSyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = "ContactSync";

	public ContactSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
	}

	@Override
	public void onPerformSync(Account account, Bundle data, String authority,
			ContentProviderClient client, SyncResult result) {
		Log.i(TAG, "Performing sync...");
	}

}
