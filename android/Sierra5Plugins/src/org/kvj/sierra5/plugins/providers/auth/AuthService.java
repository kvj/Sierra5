package org.kvj.sierra5.plugins.providers.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AuthService extends Service {

	private static final String TAG = "AuthService";
	private Authenticator mAuthenticator = null;

	@Override
	public void onCreate() {
		Log.i(TAG, "Created auth");
		mAuthenticator = new Authenticator(this);
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "Destroyed");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mAuthenticator.getIBinder();
	}
}
