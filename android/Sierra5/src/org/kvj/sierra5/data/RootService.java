package org.kvj.sierra5.data;

import org.kvj.bravo7.ipc.RemotelyBindableService;
import org.kvj.sierra5.App;

import android.os.Binder;

public class RootService extends RemotelyBindableService<Controller, App> {

	public RootService() {
		super(Controller.class);
	}

	@Override
	public Binder getStub() {
		return controller.getRootService();
	}

}
