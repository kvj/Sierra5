package org.kvj.sierra5.plugins.impl;

import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.plugin.FormatSpan;
import org.kvj.sierra5.common.plugin.MenuItemInfo;
import org.kvj.sierra5.common.plugin.Plugin;
import org.kvj.sierra5.common.theme.Theme;

import android.os.RemoteException;
import android.widget.RemoteViews;

public class DefaultPlugin extends Plugin.Stub {

	@Override
	public String getName() throws RemoteException {
		return "Plugin";
	}

	@Override
	public int getFormatterCount() throws RemoteException {
		return 0;
	}

	@Override
	public String getPattern(int index, Node node, boolean selected)
			throws RemoteException {
		return null;
	}

	@Override
	public FormatSpan[] format(int index, Theme theme, Node node, String text,
			boolean selected) throws RemoteException {
		return null;
	}

	@Override
	public MenuItemInfo[] getMenu(int id, Node node) throws RemoteException {
		return null;
	}

	@Override
	public boolean executeAction(int id, Node node) throws RemoteException {
		return false;
	}

	@Override
	public void parse(Node node, Node parent) throws RemoteException {
	}

	@Override
	public int[] getCapabilities() throws RemoteException {
		return new int[0];
	}

	@Override
	public RemoteViews render(Node node, Theme theme, int width)
			throws RemoteException {
		return null;
	}

}
