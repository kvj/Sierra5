package org.kvj.sierra5.plugins.providers.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public class Authenticator extends AbstractAccountAuthenticator {

	private static final String TAG = "Authenticator";
	private Context context = null;

	public Authenticator(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response,
			String accountType, String authTokenType,
			String[] requiredFeatures, Bundle options)
			throws NetworkErrorException {
		Log.i(TAG, "addAccount: " + accountType + ", " + authTokenType + ", "
				+ requiredFeatures + ", " + options);
		if (null != requiredFeatures) { // Have features
			for (String f : requiredFeatures) { // f = feature
				Log.i(TAG, "feature: " + f);
			}
		}
		Bundle result = new Bundle();
		result.putString(AccountManager.KEY_ACCOUNT_NAME, "Sierra5");
		result.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
		Log.i(TAG, "Done");
		return result;
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response,
			Account account, Bundle options) throws NetworkErrorException {
		Log.i(TAG, "confirmCredentials: " + account.name);
		Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
		return result;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response,
			String accountType) {
		Log.i(TAG, "editProperties: " + accountType);
		response.onError(1, "Not implemented yet");
		return null;
	}

	@Override
	public Bundle getAccountRemovalAllowed(
			AccountAuthenticatorResponse response, Account account)
			throws NetworkErrorException {
		Log.i(TAG, "getAccountRemovalAllowed: " + account.name);
		Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
		return result;
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response,
			Account account, String authTokenType, Bundle options)
			throws NetworkErrorException {
		Log.i(TAG, "getAuthToken: " + account.name + ", " + authTokenType
				+ ", " + options);
		Bundle result = new Bundle();
		result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
		result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
		result.putString(AccountManager.KEY_AUTHTOKEN, "sierra5");
		return result;
	}

	@Override
	public String getAuthTokenLabel(String type) {
		Log.i(TAG, "getAuthTokenLabel: " + type);
		return "Sierra5";
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response,
			Account account, String[] features) throws NetworkErrorException {
		Log.i(TAG, "hasFeatures: " + account.name);
		for (String f : features) { // f = feature name
			Log.i(TAG, "Feature: " + f);
		}
		Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
		return result;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response,
			Account account, String authTokenType, Bundle options)
			throws NetworkErrorException {
		Log.i(TAG, "updateCredentials: " + account.name + ", " + authTokenType
				+ ", " + options);
		Bundle result = new Bundle();
		result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
		result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
		return result;
	}

}
