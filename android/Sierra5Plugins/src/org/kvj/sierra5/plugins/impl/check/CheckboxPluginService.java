package org.kvj.sierra5.plugins.impl.check;

import org.kvj.bravo7.ipc.RemotelyBindableService;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.WidgetController;

import android.os.Binder;

public class CheckboxPluginService extends
		RemotelyBindableService<WidgetController, App> {

	public CheckboxPluginService() {
		super(WidgetController.class);
	}

	@Override
	public Binder getStub() {
		return controller.getCheckboxPlugin();
	}

}
