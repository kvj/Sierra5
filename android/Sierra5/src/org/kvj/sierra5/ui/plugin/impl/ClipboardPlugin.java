package org.kvj.sierra5.ui.plugin.impl;

import java.util.ArrayList;
import java.util.List;

import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.plugin.DefaultPlugin;
import org.kvj.sierra5.common.plugin.MenuItemInfo;
import org.kvj.sierra5.common.plugin.PluginInfo;
import org.kvj.sierra5.common.theme.Theme;
import org.kvj.sierra5.data.Controller;
import org.kvj.sierra5.data.provider.DataProvider.EditType;
import org.kvj.sierra5.ui.plugin.LocalPlugin;
import org.kvj.sierra5.ui.plugin.impl.clipboard.ClipboardProvider;
import org.kvj.sierra5.ui.plugin.impl.clipboard.GingerBreadProvider;

import android.os.RemoteException;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;

public class ClipboardPlugin extends DefaultPlugin implements LocalPlugin {

	private static final String TAG = "ClipboardPlugin";
	private Controller controller = null;
	private ClipboardProvider provider = null;

	private List<Node> selected = new ArrayList<Node>();

	public ClipboardPlugin(Controller controller) {
		this.controller = controller;
		// provider = Build.VERSION.SDK_INT >= 11 ? new HoneyCombProvider(
		// controller) : new GingerBreadProvider(controller);
		// Log.i(TAG, "Clipboard provider: " + provider.getClass().getName()
		// + ", " + Build.VERSION.SDK_INT);
		provider = new GingerBreadProvider(controller);
	}

	@Override
	public MenuItemInfo[] getMenu(int id, Node node) throws RemoteException {
		if (getItemCount() == 0 && id == -1) { // No items selected and top menu
			List<MenuItemInfo> result = new ArrayList<MenuItemInfo>();
			if (node.can(Node.CAPABILITY_REMOVE)) { // Removable - can mark
				result.add(new MenuItemInfo(0, MenuItemInfo.MENU_ITEM_ACTION, "Select"));
			}
			if (node.can(Node.CAPABILITY_EDIT)) {
				// Text or file - can mark children
				result.add(new MenuItemInfo(1, MenuItemInfo.MENU_ITEM_ACTION, "Select children"));
			}
			if (node.can(Node.CAPABILITY_ADD)) {
				// File or text, have nodes in clipboard
				result.add(new MenuItemInfo(2, MenuItemInfo.MENU_ITEM_ACTION, "Paste items: " + provider.getNodeCount()));
			}
			return result.toArray(new MenuItemInfo[0]);
		}
		return super.getMenu(id, node);
	}

	@Override
	public MenuItemInfo[] getEditorMenu(int id, Node node) throws RemoteException {
		if (provider.getNodeCount() > 0) { // Have items in clipboard
			return new MenuItemInfo[] { new MenuItemInfo(0, MenuItemInfo.MENU_ITEM_INSERT_TEXT, "Paste items: "
					+ provider.getNodeCount()) };
		}
		return super.getEditorMenu(id, node);
	}

	@Override
	public String executeEditAction(int id, String text, Node node) throws RemoteException {
		List<String> lines = provider.pasteText();
		StringBuffer sb = new StringBuffer();
		for (String line : lines) { // Add lines
			sb.append(line);
			sb.append('\n');
		}
		if (provider.wasCut()) { // Need to remove pasted nodes
			removeNodes(provider.paste());
			provider.clearCut();
		}
		return sb.toString();
	}

	@Override
	public boolean executeAction(int id, Node node) throws RemoteException {
		switch (id) {
		case 0: // Select first item
			// Log.i(TAG, "First selection: " + node.text + ", " +
			// node.textPath);
			selected.add(node);
			return true;
		case 1: // Select children
			if (null != node.children) { // Have children
				selected.addAll(node.children);
			}
			return true;
		case 2: // Paste items
			// Log.i(TAG, "Pasting items: " + provider.getNodeCount());
			return pasteNodes(node);
		}
		return false;
	}

	public boolean addSelection(Node node) {
		if (!node.can(Node.CAPABILITY_REMOVE)) { // Not text - ignore
			return false;
		}
		for (int i = 0; i < selected.size(); i++) { // Check parents
			if (selected.get(i).id.equals(node.id)) { // Same selected again -
														// remove
				selected.remove(i);
				return true;
			}
		}
		selected.add(node); // Finally add node
		return true;
	}

	@Override
	public int[] getCapabilities() throws RemoteException {
		return new int[] { PluginInfo.PLUGIN_HAVE_MENU_UNSELECTED, PluginInfo.PLUGIN_HAVE_MENU,
				PluginInfo.PLUGIN_HAVE_EDIT_MENU };
	}

	public int getItemCount() {
		return selected.size();
	}

	public void clear() {
		selected.clear();
	}

	public static final int PARENT_NOT = 0;
	public static final int PARENT_SAME = 1;
	public static final int PARENT_PARENT = 2;

	@Override
	public void customize(Theme theme, View view, Node node, SpannableStringBuilder text, boolean nodeSelected) {
		if (getItemCount() == 0) {
			// When no selection and not text - ignore
			return;
		}
		for (Node sel : selected) {
			if (node.id.equals(sel.id)) { // In selection
				text.setSpan(new ForegroundColorSpan(theme.ceLCyan), 0, 2, 0);
			}
		}
	}

	public boolean doCutCopy(boolean cut) {
		if (getItemCount() == 0) { // No data
			Log.w(TAG, "No items");
			return false;
		}
		boolean result = provider.cutCopy(selected, cut);
		if (!result) { // No result
			Log.w(TAG, "cutCopy failed");
			return result;
		}
		clear();
		return true; // Copied
	}

	public int getItemsCountInClipboard() {
		return provider.getNodeCount();
	}

	private boolean addNode(Node to, Node what) {
		return controller.editNode(EditType.Append, to, controller.getEditableContents(what)) != null;
	}

	private boolean removeNodes(List<Node> nodes) {
		for (Node node : nodes) {
			controller.removeNode(node);
		}
		return true; // Not implemented
	}

	public boolean remove() {
		boolean result = removeNodes(selected);
		if (result) { // Removed
			clear();
		}
		return result;
	}

	public boolean pasteNodes(Node node) {
		List<Node> nodes = provider.paste();
		for (Node n : nodes) { // Paste node one by one
			if (!addNode(node, n)) {
				return false;
			}
		}
		// Save new file
		if (provider.wasCut()) { // Need to remove pasted nodes
			removeNodes(nodes);
			provider.clearCut();
		}
		return true;
	}
}
