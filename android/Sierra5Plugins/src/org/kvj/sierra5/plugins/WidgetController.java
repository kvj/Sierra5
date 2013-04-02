package org.kvj.sierra5.plugins;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
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
				Log.i(TAG, "Root interface connected: ");
				App.getInstance().updateWidgets(-1);
			}

			@Override
			public void onDisconnect() {
				super.onDisconnect();
				Log.i(TAG, "Root interface disconnected");
			}
		};

	}

	public static interface ParserListener {

		public boolean onItem(boolean finalItem, Map<String, Object> values, Node node);
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

	private static Pattern subs = Pattern.compile("[\\$|\\?|\\#]\\{(([^\\}]+?)(:([A-Za-z0-9\\+|\\-|\\=]+))?)\\}");
	private static Pattern datePattern = Pattern.compile("([a-zA-Z]+)((\\+|\\-|\\=)(\\d{1,3})(h|d|w|m|y|e))?");

	private String insertDateTimeValue(Date date, String format) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		Matcher m = datePattern.matcher(format);
		if (null == m || !m.find()) { // Invalid data
			Log.w(TAG, "Value: " + format + " is not correct date time definition");
			return null;
		}
		if (null != m.group(2)) { // Have date modifier
			int mul = 0;
			if ("+".equals(m.group(3))) { // +
				mul = 1;
			} else if ("-".equals(m.group(3))) { // -
				mul = -1;
			}
			int value = Integer.parseInt(m.group(4), 10);
			char type = ' ';
			if (m.group(5) != null) { // Have type
				type = m.group(5).charAt(0);
			}
			if (type == 'h') { // Hour
				if (0 == mul) { // Set
					c.set(Calendar.HOUR, value);
				} else {
					c.add(Calendar.HOUR, mul * value);
				}
			} else if (type == 'd') { // Day
				if (0 == mul) { // Set
					c.add(Calendar.DAY_OF_MONTH, value);
				} else {
					c.add(Calendar.DAY_OF_YEAR, mul * value);
				}
			} else if (type == 'w') { // Week
				if (0 == mul) { // Set
					c.set(Calendar.WEEK_OF_YEAR, value);
				} else {
					c.add(Calendar.DAY_OF_YEAR, mul * value * 7);
				}
			} else if (type == 'e') { // Day of week
				if (0 == mul) { // Set
					int nowE = c.get(Calendar.DAY_OF_WEEK) - 1;
					// 0 = SUNDAY
					if (nowE == 0) { // It's Sunday now
						// TODO: Make it configurable
						nowE = 7;
					}
					c.add(Calendar.DAY_OF_YEAR, value - nowE);
				}
			} else if (type == 'm') { // Month
				if (0 == mul) { // Set month (from zero)
					c.set(Calendar.MONTH, value - 1);
				} else {
					c.add(Calendar.MONTH, mul * value);
				}
			} else if (type == 'y') { // Year
				if (0 == mul) { // Set
					c.set(Calendar.YEAR, value);
				} else {
					c.add(Calendar.YEAR, mul * value);
				}
			}
		}
		SimpleDateFormat dt = new SimpleDateFormat(m.group(1), Locale.ENGLISH);
		return dt.format(c.getTime());
	}

	private List<ItemInfo> parseItem(StringBuffer result, String text) {
		List<ItemInfo> items = new ArrayList<ItemInfo>();
		result.append('^');
		Matcher m = subs.matcher(text);
		while (m.find()) {
			StringBuffer left = new StringBuffer();
			m.appendReplacement(left, "");
			escape(left.toString(), result);
			if (m.group().startsWith("#")) { // Direct injection
				result.append(m.group(2));
				continue;
			}
			String endChar = m.group().startsWith("?") ? "?" : "";
			String itemName = null == m.group(3) ? "i" + items.size() : m.group(4);
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
					} else if (ch == '#' && !itemAdded) { // Any non space
						sb.append("(\\S+)");
						items.add(new ItemInfo(itemName));
						itemAdded = true;
					} else if (ch == 'd' && !itemAdded) { // Current date/time
						String repl = insertDateTimeValue(new Date(), m.group(4));
						// Log.i(TAG, "For " + m.group(4) + " repl is " + repl);
						if (null != repl) { // Have replacement
							sb.append(repl);
						}
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

	private boolean parseOneNode(boolean lastItem, List<String> parts, int index, Node node, ParserListener listener,
			Map<String, Object> values) throws RemoteException {
		StringBuffer regexp = new StringBuffer();
		Map<String, Object> _values = new LinkedHashMap<String, Object>(values);
		List<ItemInfo> items = parseItem(regexp, parts.get(index));
		// Log.i(TAG, "parseOneNode: " + regexp + ", " + parts.get(index));
		Pattern p = Pattern.compile(regexp.toString());
		if (null == node.children) { // File - expand
			getRootService().expand(node, true);
		}
		if (null != node.children) { // Have children
			List<Node> children = node.children;
			for (Node ch : children) { // ch = child
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
								_values.put(itemInfo.name, Integer.parseInt(m.group(groupIndex), 10));
							} catch (Exception e) {
							}
						}
					}
					boolean itemOK = listener.onItem(lastItem, _values, ch);
					// Log.i(TAG, "Match: " + ch.text + ", " + itemOK + ", "
					// + values);
					if (itemOK && !lastItem) { // Jump in
						parseOneNode(index + 1 == parts.size() - 1, parts, index + 1, ch, listener, _values);
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
				parseOneNode(partsList.size() == 1, partsList, 0, start, listener, values);
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

	public Node findNodeFromPreferences(SharedPreferences preferences, String pathName) {
		String path = preferences.getString(pathName, "");
		String[] pathArray = null;
		if (!TextUtils.isEmpty(path)) { // Have path
			pathArray = path.split("/");
		}
		// Log.i(TAG, "Loading: " + file + ", " + path);
		Root rootService = root.getRemote();
		if (null == rootService) { // No root service
			Log.w(TAG, "findNodeFromPreferences: No root service");
			return null;
		}
		try {
			return rootService.getNode(pathArray);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String pathFromIntent(Bundle data, String key) {
		String[] path = data.getStringArray(key);
		if (null == path) { // Failed
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (String item : path) { // Copy
			sb.append('/');
			sb.append(item);
		}
		return sb.toString();
	}

	public String[] pathFromString(String path) {
		if (!TextUtils.isEmpty(path)) { // Have path
			String[] arr = path.split("/");
			List<String> result = new ArrayList<String>();
			for (int i = 0; i < arr.length; i++) { //
				String item = arr[i];
				if (i == 0 && TextUtils.isEmpty(item)) { // First item is empty - skip
					continue;
				}
				result.add(item);
			}
			return result.toArray(new String[0]);
		}
		return new String[0];
	}
}
