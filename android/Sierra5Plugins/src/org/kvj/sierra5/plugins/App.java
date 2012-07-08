package org.kvj.sierra5.plugins;

import org.kvj.bravo7.ApplicationContext;

public class App extends ApplicationContext {

	@Override
	protected void init() {
		publishBean(new WidgetController(this));
	}

}
