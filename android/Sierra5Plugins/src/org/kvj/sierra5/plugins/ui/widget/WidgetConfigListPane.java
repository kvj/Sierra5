package org.kvj.sierra5.plugins.ui.widget;

import org.kvj.bravo7.widget.WidgetList;
import org.kvj.bravo7.widget.WidgetList.WidgetInfo;
import org.kvj.bravo7.widget.WidgetPreferenceActivity;
import org.kvj.bravo7.widget.WidgetPreferences;
import org.kvj.sierra5.plugins.App;

public class WidgetConfigListPane extends WidgetPreferences {

	public WidgetConfigListPane() {
		super(new WidgetList(App.getInstance()));
	}

	@Override
	protected Class<? extends WidgetPreferenceActivity> getConfigActivity(
			WidgetInfo info) {
		// TODO Auto-generated method stub
		return null;
	}

}
