package org.kvj.sierra5.plugins.impl.widget;

import org.kvj.sierra5.plugins.impl.DefaultPlugin;

import android.os.RemoteException;

public class WidgetPlugin extends DefaultPlugin {

	@Override
	public String getName() throws RemoteException {
		return "Widget plugin - fake";
	}

}
