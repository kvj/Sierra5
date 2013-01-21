package org.kvj.sierra5.plugins.ui.widget.show;

import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.widget.WidgetPreferenceActivity;
import org.kvj.sierra5.common.theme.Theme;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.WidgetController;
import org.kvj.sierra5.plugins.ui.widget.NodeSelectPane;

import android.os.Bundle;
import android.os.RemoteException;
import android.preference.ListPreference;
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
		findPreference("node").setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				NodeSelectPane.showConfig(ShowWidgetConfigPane.this, widgetID, widgetType, null);
				return true;
			}
		});
		ListPreference themes = (ListPreference) findPreference(getResources().getString(R.string.show_theme));
		WidgetController controller = App.getInstance().getBean(WidgetController.class);
		Theme[] themesArray = null;
		if (null != controller && null != controller.getRootService()) {
			// Both here
			try {
				themesArray = controller.getRootService().getThemes();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		if (null != themesArray && themesArray.length > 0) { // Loaded
			CharSequence[] entries = new CharSequence[themesArray.length];
			CharSequence[] entriyValues = new CharSequence[themesArray.length];
			for (int i = 0; i < themesArray.length; i++) { // Create
				entries[i] = themesArray[i].name;
				entriyValues[i] = themesArray[i].code;
			}
			themes.setEntries(entries);
			themes.setEntryValues(entriyValues);
		} else {
			SuperActivity.notifyUser(this, "Error loading theme data");
		}
	}

}
