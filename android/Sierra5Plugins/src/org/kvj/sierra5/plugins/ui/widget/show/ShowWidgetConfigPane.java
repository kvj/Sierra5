package org.kvj.sierra5.plugins.ui.widget.show;

import org.kvj.bravo7.widget.WidgetPreferenceActivity;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;

public class ShowWidgetConfigPane extends WidgetPreferenceActivity {

	private static final String TAG = "ShowWidget";

	public ShowWidgetConfigPane() {
		super(App.getInstance(), "show", R.xml.show_widget_config);
		setCustomPreferences("node");
	}

}
