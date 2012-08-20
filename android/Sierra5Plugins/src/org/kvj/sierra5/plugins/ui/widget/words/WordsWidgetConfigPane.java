package org.kvj.sierra5.plugins.ui.widget.words;

import org.kvj.bravo7.widget.WidgetPreferenceActivity;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.ui.widget.NodeSelectPane;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

public class WordsWidgetConfigPane extends WidgetPreferenceActivity {

	public WordsWidgetConfigPane() {
		super(App.getInstance(), "words", R.xml.words_widget_config);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		findPreference("node").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						NodeSelectPane.showConfig(WordsWidgetConfigPane.this,
								widgetID, widgetType, "words_");
						return true;
					}
				});
	}
}
