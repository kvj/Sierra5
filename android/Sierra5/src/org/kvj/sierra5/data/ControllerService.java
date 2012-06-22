package org.kvj.sierra5.data;

import org.kvj.bravo7.SuperService;
import org.kvj.sierra5.App;

public class ControllerService extends SuperService<Controller, App> {

	public ControllerService() {
		super(Controller.class, "Sierra5");
	}
}
