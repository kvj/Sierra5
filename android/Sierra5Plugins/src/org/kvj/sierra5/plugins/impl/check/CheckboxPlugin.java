package org.kvj.sierra5.plugins.impl.check;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.plugin.DefaultPlugin;
import org.kvj.sierra5.common.plugin.FormatSpan;
import org.kvj.sierra5.common.plugin.MenuItemInfo;
import org.kvj.sierra5.common.plugin.PluginInfo;
import org.kvj.sierra5.common.theme.Theme;
import org.kvj.sierra5.plugins.WidgetController;

import android.graphics.Typeface;
import android.os.RemoteException;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

public class CheckboxPlugin extends DefaultPlugin {

	private WidgetController controller = null;
	private static final String regexp = "\\[(\\ |X)\\]";
	private static final String TAG = "Checkbox";

	public CheckboxPlugin(WidgetController controller) {
		this.controller = controller;
	}

	@Override
	public String getName() throws RemoteException {
		return "Checkboxes";
	}

	@Override
	public int[] getCapabilities() throws RemoteException {
		return new int[] { PluginInfo.PLUGIN_CAN_FORMAT,
				PluginInfo.PLUGIN_HAVE_MENU, PluginInfo.PLUGIN_HAVE_EDIT_MENU };
	}

	@Override
	public int getFormatterCount() throws RemoteException {
		return 1;
	}

	@Override
	public String getPattern(int index, Node node, boolean selected)
			throws RemoteException {
		return regexp;
	}

	@Override
	public FormatSpan[] format(int index, Theme theme, Node node, String text,
			boolean selected) throws RemoteException {
		if ("[X]".equals(text)) {
			// Checked
			return new FormatSpan[] { new FormatSpan(text, new ForegroundColorSpan(
					theme.c7White)) };
		}
		return new FormatSpan[] { new FormatSpan(text, new StyleSpan(
				Typeface.BOLD)) };
	}

	@Override
	public MenuItemInfo[] getMenu(int id, Node node) throws RemoteException {
		if (Node.TYPE_TEXT == node.type && id == -1) {
			// Root menu, text
			Matcher m = Pattern.compile(regexp).matcher(node.text);
			if (m.find()) { // Have checkbox
				return new MenuItemInfo[] { new MenuItemInfo(0,
						MenuItemInfo.MENU_ITEM_ACTION, "Toggle checkbox") };
			}
		}
		return null;
	}

	@Override
	public MenuItemInfo[] getEditorMenu(int id, Node node)
			throws RemoteException {
		return new MenuItemInfo[] { new MenuItemInfo(0,
				MenuItemInfo.MENU_ITEM_INSERT_TEXT, "Add checkbox") };
	}

	@Override
	public String executeEditAction(int id, String text, Node node)
			throws RemoteException {
		return "[ ] ";
	}

	@Override
	public boolean executeAction(int id, Node node) throws RemoteException {
		StringBuffer buffer = new StringBuffer();
		Matcher m = Pattern.compile(regexp).matcher(node.text);
		while (m.find()) {
			String state = m.group(1);
			state = " ".equals(state) ? "X" : " ";
			m.appendReplacement(buffer, "[" + state + "]");
		}
		m.appendTail(buffer);
		// Log.i(TAG, "Changing checkbox " + node.text + " => " + buffer);
		if (null != controller.getRootService()) { // Have connection
			boolean result = controller.getRootService().update(node,
					buffer.toString(), null);
			// Log.i(TAG, "Update result: " + result);
			node.setText(buffer.toString());
			return result;
		}
		return false;
	}
}
