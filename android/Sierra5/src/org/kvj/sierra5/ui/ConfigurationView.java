package org.kvj.sierra5.ui;

import org.kvj.bravo7.SuperActivity;
import org.kvj.sierra5.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.lamerman.FileDialog;
import com.lamerman.SelectionMode;

public class ConfigurationView extends PreferenceActivity {

	private static final int REQUEST_OPEN = 101;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.config);
		findPreference(getResources().getString(R.string.rootFolder))
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						openFileDialog();
						return true;
					}

				});
	}

	private void openFileDialog() {
		Intent intent = new Intent(this, FileDialog.class);
		String state = Environment.getExternalStorageState();
		intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
		intent.putExtra(FileDialog.SELECTION_TYPE,
				SelectionMode.MODE_SELECT_FOLDER);
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			intent.putExtra(FileDialog.START_PATH, Environment
					.getExternalStorageDirectory().getAbsolutePath());
		}
		startActivityForResult(intent, REQUEST_OPEN);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == REQUEST_OPEN) {
				getPreferenceManager()
						.getSharedPreferences()
						.edit()
						.putString(
								getResources().getString(R.string.rootFolder),
								data.getStringExtra(FileDialog.RESULT_PATH))
						.commit();
				SuperActivity.notifyUser(
						ConfigurationView.this,
						"File path saved: "
								+ data.getStringExtra(FileDialog.RESULT_PATH));
			}
		}
	}
}
