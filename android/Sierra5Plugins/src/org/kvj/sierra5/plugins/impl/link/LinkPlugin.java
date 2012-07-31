package org.kvj.sierra5.plugins.impl.link;

import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.plugin.FormatSpan;
import org.kvj.sierra5.common.plugin.PluginInfo;
import org.kvj.sierra5.common.theme.Theme;
import org.kvj.sierra5.plugins.impl.DefaultPlugin;

import android.os.RemoteException;
import android.text.style.ForegroundColorSpan;

public class LinkPlugin extends DefaultPlugin {

	@Override
	public String getName() throws RemoteException {
		return "Link plugin";
	}

	@Override
	public int[] getCapabilities() throws RemoteException {
		return new int[] { PluginInfo.PLUGIN_CAN_FORMAT,
				PluginInfo.PLUGIN_CAN_PARSE };
	}

	@Override
	public FormatSpan[] format(int index, Theme theme, Node node, String text,
			boolean selected) throws RemoteException {
		switch (index) {
		case 0: // Name
			return new FormatSpan[] { new FormatSpan(text,
					new ForegroundColorSpan(theme.caLGreen)) };
		case 1: // Activity
			return new FormatSpan[] {
					new FormatSpan(text.substring(0, text.length() - 1),
							new ForegroundColorSpan(theme.ceLCyan)),
					new FormatSpan(":",
							new ForegroundColorSpan(theme.colorText)) };
		}
		return null;
	}

	@Override
	public int getFormatterCount() throws RemoteException {
		return 2;
	}

	@Override
	public String getPattern(int index, Node node, boolean selected)
			throws RemoteException {
		if (Node.TYPE_TEXT != node.type) { // Only for texts
			return null;
		}
		switch (index) {
		case 0: // Name
			return "@[A-Za-z0-9\\-]+";
		case 1: // Activity
			return "#[A-Za-z0-9\\-]+\\:";
		}
		return null;
	}

	@Override
	public void parse(Node node, Node parent) throws RemoteException {
		if (node.type == Node.TYPE_TEXT && node.text.endsWith(" /-")) { // Collapse
			node.collapsed = true;
		}
	}

}
