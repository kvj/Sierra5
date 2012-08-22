package org.kvj.sierra5.ui.plugin.impl;

import org.kvj.sierra5.common.plugin.DefaultPlugin;
import org.kvj.sierra5.common.plugin.PluginInfo;
import org.kvj.sierra5.ui.plugin.LocalPlugin;

import android.os.RemoteException;

public class ClipboardPlugin extends DefaultPlugin implements LocalPlugin {

	private static final String TAG = "ClipboardPlugin";

	@Override
	public int[] getCapabilities() throws RemoteException {
		return new int[] { PluginInfo.PLUGIN_HAVE_MENU_UNSELECTED };
	}

}
