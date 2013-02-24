package org.kvj.sierra5.ui.plugin.impl;

import java.util.ArrayList;
import java.util.List;

import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.plugin.DefaultPlugin;
import org.kvj.sierra5.common.plugin.MenuItemInfo;
import org.kvj.sierra5.common.plugin.PluginInfo;
import org.kvj.sierra5.common.theme.Theme;
import org.kvj.sierra5.data.Controller;
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
			if (Node.TYPE_TEXT == node.type) { // Text - can mark
				result.add(new MenuItemInfo(0, MenuItemInfo.MENU_ITEM_ACTION, "Select"));
			}
			if (Node.TYPE_TEXT == node.type || Node.TYPE_FILE == node.type) {
				// Text or file - can mark children
				result.add(new MenuItemInfo(1, MenuItemInfo.MENU_ITEM_ACTION, "Select children"));
			}
			if (Node.TYPE_FOLDER != node.type && provider.getNodeCount() > 0) {
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
			if (node.collapsed) { // Collapsed - expand first
				controller.expand(node, null, Controller.EXPAND_ONE);
			}
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
		if (Node.TYPE_TEXT != node.type) { // Not text - ignore
			return false;
		}
		for (int i = 0; i < selected.size(); i++) { // Check parents
			int parentType = is1stIsParentOf2nd(selected.get(i), node);
			if (parentType == PARENT_SAME) { // Same selected again - remove
				selected.remove(i);
				return true;
			}
			if (parentType == PARENT_PARENT) { // Have parent already - ignore
				return false;
			}
		}
		// Opposite check
		for (int i = 0; i < selected.size(); i++) {
			// Check if node is parent
			int parentType = is1stIsParentOf2nd(node, selected.get(i));
			if (parentType == PARENT_PARENT) { // Node is parent - remove
				selected.remove(i);
				i--; // Check again
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

	public static int is1stIsParentOf2nd(Node first, Node second) {
		if (!first.file.equals(second.file)) { // Wrong files
			return PARENT_NOT;
		}
		String[] firstPath = Node.list2array(first.textPath, new String[0]);
		String[] secondPath = Node.list2array(second.textPath, new String[0]);
		for (int i = 0; i < firstPath.length; i++) {
			// Start from top of first
			if (i >= secondPath.length) { // second is shorter - not a parent
				return PARENT_NOT;
			}
			if (!firstPath[i].equals(secondPath[i])) {
				// Not equals - not a parent
				return PARENT_NOT;
			}
		}
		if (secondPath.length > firstPath.length) { // Second is longer - parent
			return PARENT_PARENT;
		}
		return PARENT_SAME; // No differences found
	}

	@Override
	public void customize(Theme theme, View view, Node node, SpannableStringBuilder text, boolean nodeSelected) {
		if (getItemCount() == 0 || Node.TYPE_TEXT != node.type) {
			// When no selection and not text - ignore
			return;
		}
		for (Node sel : selected) {
			int parentType = is1stIsParentOf2nd(sel, node);
			if (PARENT_NOT == parentType) { // Ignore, out of selection
				continue;
			}
			text.setSpan(new ForegroundColorSpan(parentType == PARENT_SAME ? theme.ceLCyan : theme.caLGreen), 0, 2, 0);
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

	private void addNode(Node to, Node what) {
		Node child = to.createChild(Node.TYPE_TEXT, what.text, controller.getTabSize());
		if (null != what.children) { // add children also
			for (Node ch : what.children) {
				addNode(child, ch);
			}
		}
	}

	private boolean removeNodes(List<Node> nodes) {
		for (Node node : nodes) {
			Node[] nn = controller.actualizeNode(node);
			if (nn.length == 2) { // Node actual
				controller.saveFile(nn[0], nn[1]);
			}
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
		Node[] nn = controller.actualizeNode(node);
		if (nn.length != 2) { // Node not found
			Log.w(TAG, "Can't paste, node not found");
		}
		List<Node> nodes = provider.paste();
		for (Node n : nodes) { // Paste node one by one
			addNode(nn[1], n);
		}
		// Save new file
		boolean result = controller.saveFile(nn[0], null);
		if (result && provider.wasCut()) { // Need to remove pasted nodes
			removeNodes(nodes);
			provider.clearCut();
		}
		return result;
	}
}
