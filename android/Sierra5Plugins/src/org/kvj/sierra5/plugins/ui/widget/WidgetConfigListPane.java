package org.kvj.sierra5.plugins.ui.widget;

import org.kvj.bravo7.widget.WidgetList;
import org.kvj.bravo7.widget.WidgetList.WidgetInfo;
import org.kvj.bravo7.widget.WidgetPreferenceActivity;
import org.kvj.bravo7.widget.WidgetPreferences;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.ui.widget.show.ShowWidgetConfigPane;
import org.kvj.sierra5.plugins.ui.widget.words.WordsWidgetConfigPane;

import android.util.Log;

public class WidgetConfigListPane extends WidgetPreferences {

	private static final String TAG = "Widget list";

	public WidgetConfigListPane() {
		super(new WidgetList(App.getInstance()));
		Log.i(TAG, "Created widget list");
	}

	@Override
	protected Class<? extends WidgetPreferenceActivity> getConfigActivity(
			WidgetInfo info) {
		if ("show".equals(info.type)) { // Show plugin
			return ShowWidgetConfigPane.class;
		}
		if ("words".equals(info.type)) { // Words plugin
			return WordsWidgetConfigPane.class;
		}
		return null;
	}

}
