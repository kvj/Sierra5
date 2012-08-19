package org.kvj.sierra5.plugins;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kvj.bravo7.ipc.RemoteServiceConnector;
import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.plugin.Plugin;
import org.kvj.sierra5.common.root.Root;
import org.kvj.sierra5.plugins.impl.check.CheckboxPlugin;
import org.kvj.sierra5.plugins.impl.link.LinkPlugin;
import org.kvj.sierra5.plugins.impl.quebec.Q4Plugin;
import org.kvj.sierra5.plugins.impl.widget.WidgetPlugin;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

public class WidgetController {

	private RemoteServiceConnector<Root> root = null;
	protected String TAG = "WidgetController";

	public WidgetController(Context ctx) {
		Log.i(TAG, "Starting controller");
		root = new RemoteServiceConnector<Root>(ctx, Constants.ROOT_NS, null) {

			@Override
			public Root castAIDL(IBinder binder) {
				return Root.Stub.asInterface(binder);
			}

			@Override
			public void onConnect() {
				super.onConnect();
				try {
					Log.i(TAG, "Root interface connected: "
							+ root.getRemote().getRoot());
					App.getInstance().updateWidgets(-1);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onDisconnect() {
				super.onDisconnect();
				Log.i(TAG, "Root interface disconnected");
			}
		};
	}

	public static interface ParserListener {

		public boolean onItem(boolean finalItem, Map<String, Object> values,
				Node node);
	}

	private void escape(String what, StringBuffer to) {
		for (int i = 0; i < what.length(); i++) { //
			if (what.charAt(i) == '.') { // replace with \\.
				to.append("\\.");
			} else if (what.charAt(i) == '*') { // replace with .*
				to.append(".*");
			} else if (what.charAt(i) == '?') { // replace with .
				to.append(".");
			} else {
				to.append(what.charAt(i));
			}
		}
	}

	private static final int ITEM_STRING = 0;
	private static final int ITEM_NUMBER = 1;

	private class ItemInfo {

		int type = ITEM_STRING;
		String name = null;

		public ItemInfo(String name) {
			this.name = name;
		}

		public ItemInfo(int type, String name) {
			this.name = name;
			this.type = type;
		}
	}

	private static Pattern subs = Pattern
			.compile("[\\$|\\?]\\{(([^\\}]+?)(:([A-Za-z0-9]+))?)\\}");

	private List<ItemInfo> parseItem(StringBuffer result, String text) {
		List<ItemInfo> items = new ArrayList<ItemInfo>();
		result.append('^');
		Matcher m = subs.matcher(text);
		while (m.find()) {
			StringBuffer left = new StringBuffer();
			m.appendReplacement(left, "");
			escape(left.toString(), result);
			String endChar = m.group().startsWith("?") ? "?" : "";
			String itemName = null == m.group(3) ? "i" + items.size() : m
					.group(4);
			if (m.group(2).matches("^\\?+$")) { // ???
				result.append("((");
				for (int i = 0; i < m.group(2).length(); i++) { // add .
					result.append(".");
				}
				result.append("))" + endChar);
				items.add(new ItemInfo(itemName));
			} else {
				// Use as is
				String pt = m.group(2);
				StringBuffer sb = new StringBuffer();
				boolean itemAdded = false;
				for (int i = 0; i < pt.length(); i++) {
					char ch = pt.charAt(i);
					if (ch == '*' && !itemAdded) { // Any char
						sb.append("(.+)");
						items.add(new ItemInfo(itemName));
						itemAdded = true;
					} else if (ch == '0' && !itemAdded) { // Any number
						sb.append("(\\d+)");
						items.add(new ItemInfo(ITEM_NUMBER, itemName));
						itemAdded = true;
					} else {
						// add as is
						sb.append(ch);
					}
				}
				// Log.i(TAG, "As is:" + m.group(2) + ", " + endChar);
				result.append("(" + sb + ")" + endChar);
			}
		}
		StringBuffer tail = new StringBuffer();
		m.appendTail(tail);
		escape(tail.toString(), result);
		result.append("$");
		return items;
	}

	private boolean parseOneNode(boolean lastItem, List<String> parts,
			int index, Node node, ParserListener listener,
			Map<String, Object> values) throws RemoteException {
		StringBuffer regexp = new StringBuffer();
		Map<String, Object> _values = new LinkedHashMap<String, Object>(values);
		List<ItemInfo> items = parseItem(regexp, parts.get(index));
		// Log.i(TAG, "parseOneNode: " + regexp + ", " + parts.get(index));
		Pattern p = Pattern.compile(regexp.toString());
		if (Node.TYPE_FILE == node.type && node.collapsed) { // File - expand
			getRootService().expand(node, true);
		}
		if (null != node.children) { // Have children
			for (Node ch : node.children) { // ch = child
				Matcher m = p.matcher(ch.text);
				// Log.i(TAG, "Matching: " + ch.text + " vs " + regexp);
				if (m.find()) { // Our case
					int groupIndex = 2;
					for (int i = 0; i < items.size(); i++, groupIndex += 2) {
						// Add value
						ItemInfo itemInfo = items.get(i);
						if (itemInfo.type == ITEM_STRING) {
							// String - no conversion
							_values.put(itemInfo.name, m.group(groupIndex));
						} else if (itemInfo.type == ITEM_NUMBER) {
							// Parse number
							try { // Conversion error
								_values.put(itemInfo.name, Integer.parseInt(
										m.group(groupIndex), 10));
							} catch (Exception e) {
							}
						}
					}
					boolean itemOK = listener.onItem(lastItem, _values, ch);
					// Log.i(TAG, "Match: " + ch.text + ", " + itemOK + ", "
					// + values);
					if (itemOK && !lastItem) { // Jump in
						parseOneNode(index + 1 == parts.size() - 1, parts,
								index + 1, ch, listener, _values);
					}
					// } else {
					// Log.i(TAG, "Not match: " + ch.text + ", " + regexp);
				}
			}
		}
		return true;
	}

	public boolean parseNode(String exp, Node start, ParserListener listener) {
		try { // Remote and other errors
			String[] parts = exp.split("/");
			Map<String, Object> values = new LinkedHashMap<String, Object>();
			if (TextUtils.isEmpty(exp)) { // No exp
				parts = new String[0];
			}
			// Log.i(TAG, "parseNode: " + exp + ", " + parts.length + ", "
			// + start.text);
			List<String> partsList = new ArrayList<String>();
			StringBuffer item = new StringBuffer();
			for (int i = 0; i < parts.length; i++) {
				String part = parts[i];
				if (TextUtils.isEmpty(part)) { // This is mask
					item.append("/");
					if (i < parts.length - 1) { // Have one more item
						i++;
						item.append(parts[i]);
					}
				} else {
					if (item.length() > 0) { // Have item
						partsList.add(item.toString());
						item.setLength(0);
					}
					item.append(part);
				}
			}
			if (item.length() > 0) { // Have item
				partsList.add(item.toString());
			}
			if (partsList.size() > 0) { // Have smth
				parseOneNode(partsList.size() == 1, partsList, 0, start,
						listener, values);
			}
		} catch (Exception e) {
			Log.e(TAG, "Error in parse", e);
		}
		return false;
	}

	private Plugin.Stub widgetPlugin = new WidgetPlugin();

	private Plugin.Stub linkPlugin = new LinkPlugin(this);

	private Plugin.Stub checkboxPlugin = new CheckboxPlugin(this);

	private Plugin.Stub q4Plugin = new Q4Plugin(this);

	public Root getRootService() {
		return root.getRemote();
	}

	public Plugin.Stub getWidgetPlugin() {
		return widgetPlugin;
	}

	public Plugin.Stub getLinkPlugin() {
		return linkPlugin;
	}

	public Binder getCheckboxPlugin() {
		return checkboxPlugin;
	}

	public Binder getQ4Plugin() {
		return q4Plugin;
	}
}
