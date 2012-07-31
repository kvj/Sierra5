package org.kvj.sierra5.plugins.impl.link;

import org.kvj.bravo7.ipc.RemotelyBindableService;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.WidgetController;

import android.os.Binder;

public class LinkPluginService extends
		RemotelyBindableService<WidgetController, App> {

	public LinkPluginService() {
		super(WidgetController.class);
	}

	@Override
	public Binder getStub() {
		return controller.getLinkPlugin();
	}

}
