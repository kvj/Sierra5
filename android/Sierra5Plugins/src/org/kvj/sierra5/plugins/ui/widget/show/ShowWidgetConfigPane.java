package org.kvj.sierra5.plugins.ui.widget.show;

import org.kvj.bravo7.widget.WidgetPreferenceActivity;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.ui.widget.NodeSelectPane;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

public class ShowWidgetConfigPane extends WidgetPreferenceActivity {

	private static final String TAG = "ShowWidget";

	public ShowWidgetConfigPane() {
		super(App.getInstance(), "show", R.xml.show_widget_config);
		setCustomPreferences();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		findPreference("node").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						NodeSelectPane.showConfig(ShowWidgetConfigPane.this,
								widgetID, widgetType, null);
						return true;
					}
				});
	}

}
