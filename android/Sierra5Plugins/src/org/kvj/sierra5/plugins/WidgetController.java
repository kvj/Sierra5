package org.kvj.sierra5.plugins;

import java.util.ArrayList;
import java.util.HashMap;
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

	private static Pattern subs = Pattern
			.compile("\\$\\{(([^\\}]+?)(:([A-Za-z0-9]+))?)\\}");

	private void escape(String what, StringBuffer to) {
		for (int i = 0; i < what.length(); i++) { //
			if (what.charAt(i) == '.') { // replace with \\.
				to.append("\\.");
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

	private List<ItemInfo> parseItem(StringBuffer result, String text) {
		List<ItemInfo> items = new ArrayList<ItemInfo>();
		result.append('^');
		Matcher m = subs.matcher(text);
		while (m.find()) {
			StringBuffer left = new StringBuffer();
			m.appendReplacement(left, "");
			escape(left.toString(), result);
			String itemName = null == m.group(3) ? "i" + items.size() : m
					.group(4);
			if ("...".equals(m.group(1))) { // ... - any chars .* and skip
				result.append(".*");
			} else if ("*".equals(m.group(2))) { // *
				result.append("(.*)");
				items.add(new ItemInfo(itemName));
			} else if ("0".equals(m.group(2))) { // 0 any numbers \\d*
				result.append("(\\d*)");
				items.add(new ItemInfo(ITEM_NUMBER, itemName));
			} else if (m.group(2).matches("^\\?+$")) { // ???
				result.append("(");
				for (int i = 0; i < m.group(2).length(); i++) { // add .
					result.append(".");
				}
				result.append(")");
				items.add(new ItemInfo(itemName));
			}
		}
		StringBuffer tail = new StringBuffer();
		m.appendTail(tail);
		escape(tail.toString(), result);
		result.append("$");
		return items;
	}

	private boolean parseOneNode(boolean lastItem, String[] parts, int index,
			Node node, ParserListener listener, Map<String, Object> values)
			throws RemoteException {
		StringBuffer regexp = new StringBuffer();
		List<ItemInfo> items = parseItem(regexp, parts[index]);
		Pattern p = Pattern.compile(regexp.toString());
		Log.i(TAG, "parseOneNode: " + regexp);
		if (Node.TYPE_FILE == node.type) { // File - expand
			getRootService().expand(node);
		}
		if (null != node.children) { // Have children
			for (Node ch : node.children) { // ch = child
				Matcher m = p.matcher(ch.text);
				if (m.find()) { // Our case
					for (int i = 0; i < items.size(); i++) {
						// Add value
						ItemInfo itemInfo = items.get(i);
						if (itemInfo.type == ITEM_STRING) {
							// String - no conversion
							values.put(itemInfo.name, m.group(i + 1));
						} else if (itemInfo.type == ITEM_NUMBER) {
							// Parse number
							try { // Conversion error
								values.put(itemInfo.name,
										Integer.parseInt(m.group(i + 1), 10));
							} catch (Exception e) {
							}
						}
					}
					boolean itemOK = listener.onItem(lastItem, values, ch);
					Log.i(TAG, "Match: " + ch.text + ", " + itemOK + ", "
							+ values);
					if (itemOK && !lastItem) { // Jump in
						parseOneNode(index + 1 == parts.length - 1, parts,
								index + 1, ch, listener, values);
					}
					// } else {
					// Log.i(TAG, "Not match: " + ch.text);
				}
			}
		}
		return true;
	}

	public boolean parseNode(String exp, Node start, ParserListener listener) {
		try { // Remote and other errors
			String[] parts = exp.split("/");
			Map<String, Object> values = new HashMap<String, Object>();
			if (TextUtils.isEmpty(exp)) { // No exp
				parts = new String[0];
			}
			Log.i(TAG, "parseNode: " + exp + ", " + parts.length + ", "
					+ start.text);
			if (parts.length > 0) { // Have smth
				parseOneNode(parts.length == 1, parts, 0, start, listener,
						values);
			}
		} catch (Exception e) {
			Log.e(TAG, "Error in parse", e);
		}
		return false;
	}

	private Plugin.Stub widgetPlugin = new WidgetPlugin();

	private Plugin.Stub linkPlugin = new LinkPlugin(this);

	private Plugin.Stub checkboxPlugin = new CheckboxPlugin(this);

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
}
