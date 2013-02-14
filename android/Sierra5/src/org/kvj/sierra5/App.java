package org.kvj.sierra5;

import org.kvj.bravo7.ApplicationContext;
import org.kvj.sierra5.data.Controller;

public class App extends ApplicationContext {

	@Override
	protected void init() {
		publishBean(new Controller(this));
	}

}
