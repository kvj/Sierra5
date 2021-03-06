package org.kvj.sierra5.plugins.ui;

import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.ui.widget.NodeSelectPane;
import org.kvj.sierra5.plugins.ui.widget.WidgetConfigListPane;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

public class PluginsConfiguration extends PreferenceActivity {

	protected static final String TAG = "UI";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		App.getInstance();
		addPreferencesFromResource(R.xml.config);
		findPreference("widgets").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						Log.i(TAG, "Show widget list");
						Intent intent = new Intent(PluginsConfiguration.this,
								WidgetConfigListPane.class);
						startActivity(intent);
						return true;
					}
				});
		findPreference("contacts_node").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						NodeSelectPane.showConfig(PluginsConfiguration.this,
								null, null, "contacts_");
						return true;
					}
				});
		findPreference("calendar_node").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						NodeSelectPane.showConfig(PluginsConfiguration.this,
								null, null, "calendar_");
						return true;
					}
				});
	}

}
