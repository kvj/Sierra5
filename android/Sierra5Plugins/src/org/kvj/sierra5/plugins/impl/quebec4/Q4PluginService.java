package org.kvj.sierra5.plugins.impl.quebec4;

import org.kvj.bravo7.ipc.RemotelyBindableService;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.WidgetController;

import android.os.Binder;

public class Q4PluginService extends
		RemotelyBindableService<WidgetController, App> {

	public Q4PluginService() {
		super(WidgetController.class);
	}

	@Override
	public Binder getStub() {
		return controller.getQ4Plugin();
	}

}
