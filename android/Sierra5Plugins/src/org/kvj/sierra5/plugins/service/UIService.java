package org.kvj.sierra5.plugins.service;

import org.kvj.bravo7.SuperService;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.WidgetController;

public class UIService extends SuperService<WidgetController, App> {

	public UIService() {
		super(WidgetController.class, "Plugins UI");
	}
}
