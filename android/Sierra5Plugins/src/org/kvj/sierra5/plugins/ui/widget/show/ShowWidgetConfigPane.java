package org.kvj.sierra5.plugins.ui.widget.show;

import org.kvj.bravo7.widget.WidgetPreferenceActivity;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;

import android.util.Log;

public class ShowWidgetConfigPane extends WidgetPreferenceActivity {

	private static final String TAG = "ShowWidget";

	public ShowWidgetConfigPane() {
		super(App.getInstance(), "show", R.xml.show_widget_config);
		Log.i(TAG, "Showing config");
	}

}
