package org.kvj.sierra5.plugins.impl.widget;

import org.kvj.bravo7.ipc.RemotelyBindableService;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.WidgetController;

import android.os.Binder;

public class WidgetPluginService extends
		RemotelyBindableService<WidgetController, App> {

	public WidgetPluginService() {
		super(WidgetController.class);
	}

	@Override
	public Binder getStub() {
		return controller.getWidgetPlugin();
	}

}
