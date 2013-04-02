package org.kvj.sierra5.ui.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kvj.bravo7.format.PlainTextFormatter;
import org.kvj.sierra5.App;
import org.kvj.sierra5.R;
import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.plugin.FormatSpan;
import org.kvj.sierra5.common.plugin.Plugin;
import org.kvj.sierra5.common.plugin.PluginInfo;
import org.kvj.sierra5.common.theme.Theme;
import org.kvj.sierra5.data.Controller;
import org.kvj.sierra5.ui.adapter.theme.ThemeProvider;
import org.kvj.sierra5.ui.plugin.LocalPlugin;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.RemoteViews;
import android.widget.TextView;

public class ListViewAdapter implements ListAdapter {

	public static interface ListViewAdapterListener {
		public void itemSelected(int selected);
	}

	private static final String TAG = "Adapter";

	private static final int LEFT_GAP = 10;

	private Node root = null;
	private boolean showRoot = false;
	private DataSetObserver observer = null;
	private int selectedIndex = -1;
	private ListViewAdapterListener listener = null;
	private int textSize = 0;
	DefaultTextFormatter defaultTextFormatter = new DefaultTextFormatter();
	private PlainTextFormatter<Node> textFormatter = null;
	private Map<String, RemoteViews> remoteRenders = new HashMap<String, RemoteViews>();
	List<Plugin> remoteRenderPlugins = new ArrayList<Plugin>();
	List<LocalPlugin> localPlugins = new ArrayList<LocalPlugin>();

	private Theme theme = null;

	private Controller controller = null;

	class DefaultTextFormatter implements NodeTextFormatter {

		@Override
		public Pattern getPattern(Node note, boolean selected) {
			if (note.style == Node.STYLE_0) { // No style - let plugins decorate
				return null;
			}
			return eatAll;
		}

		@Override
		public void format(Node note, SpannableStringBuilder sb, Matcher m, String text, boolean selected) {
			int color = 0;
			switch (note.style) {
			case Node.STYLE_1: // Folder like
				color = theme.cbLYellow;
				if (note.text.length() > 1) { // Color only First letter
					PlainTextFormatter.addSpan(sb, text.substring(0, 1), new ForegroundColorSpan(color));
					PlainTextFormatter.addSpan(sb, text.substring(1), new ForegroundColorSpan(theme.colorText));
				} else {
					PlainTextFormatter.addSpan(sb, text, new ForegroundColorSpan(color));
				}
				break;
			case Node.STYLE_2: // File like
				color = theme.ccLBlue;
				if (note.text.length() > 1) { // Color only First letter
					PlainTextFormatter.addSpan(sb, text.substring(0, 1), new ForegroundColorSpan(color));
					PlainTextFormatter.addSpan(sb, text.substring(1), new ForegroundColorSpan(theme.colorText));
				} else {
					PlainTextFormatter.addSpan(sb, text, new ForegroundColorSpan(color));
				}
				break;
			}
		}

	}

	@SuppressWarnings("unchecked")
	public ListViewAdapter(ListViewAdapterListener listener, Theme theme) {
		this.listener = listener;
		this.theme = theme;
		textSize = App.getInstance().getIntPreference(R.string.docFont, R.string.docFontDefault);

		textFormatter = new PlainTextFormatter<Node>(defaultTextFormatter);
	}

	private static class SearchInTreeResult {
		int size = 1;
		Node foundNode = null;
	}

	private SearchInTreeResult moveThru(Node node, int searchIndex) {
		SearchInTreeResult result = new SearchInTreeResult();
		if (searchIndex == 0) { // That's what we are looking for
			result.foundNode = node;
		}
		if (node.collapsed) { // Collapsed - no children
			return result;
		}
		if (null == node.children) { // No children
			Log.w(TAG, "Expanded wout children: " + node.text);
			return result;
		}
		List<Node> children = node.children;
		for (Node child : children) { // Every children
			if (!child.visible) { // Not visible - skip
				continue;
			}
			SearchInTreeResult r = moveThru(child, searchIndex - result.size);
			if (null != r.foundNode) { // Node found
				return r; // Don't need to go thru anymore
			}
			result.size += r.size;
		}
		return result;
	}

	@Override
	public int getCount() {
		if (null == root) { // Root not created yet
			return 0;
		}
		if (!showRoot && root.collapsed) { // No data to show
			return 0; // No data visible
		}
		SearchInTreeResult r = moveThru(root, -1);
		int result = r.size;
		if (!showRoot) { // Root is not shown - decrease
			result--;
		}
		// Log.i(TAG, "getCount: " + result);
		return result;
	}

	@Override
	public Node getItem(int index) {
		if (-1 == index) { // Out of bounds
			return null;
		}
		SearchInTreeResult r = moveThru(root, showRoot ? index : index + 1);
		return r.foundNode;
	}

	@Override
	public long getItemId(int index) {
		return index;
	}

	@Override
	public int getItemViewType(int index) {
		return 0;
	}

	public SpannableStringBuilder customize(View view, Node node, boolean selected) {
		TextView textView = (TextView) view.findViewById(R.id.listview_item_text);
		// menuIcon.setVisibility(selected ? View.VISIBLE : View.GONE);
		textView.setTextColor(theme.colorText);
		// textView.setBackgroundColor(theme.colorBackground);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
		SpannableStringBuilder text = new SpannableStringBuilder();
		// float dens =
		// view.getContext().getResources().getDisplayMetrics().density;
		int levels = showRoot ? node.level : node.level - 1;
		StringBuffer left = new StringBuffer();
		for (int i = 0; i < levels; i++) {
			left.append(' ');
		}
		TextView leftView = (TextView) view.findViewById(R.id.listview_item_left);
		leftView.setText(left.toString());
		textFormatter.writePlainText(node, text, theme.colorText, node.text, selected);
		if (selected) { // Add italic
			text.setSpan(new StyleSpan(Typeface.ITALIC), 0, text.length(), 0);
		}
		if (node.collapsed) { // Underline - collapsed
			text.setSpan(new UnderlineSpan(), 0, text.length(), 0);
		}
		for (LocalPlugin plugin : localPlugins) {
			// Customize using local plugins
			plugin.customize(theme, view, node, text, selected);
		}
		textView.setText(text);
		return text;
	}

	@Override
	public View getView(int index, View view, ViewGroup parent) {
		Node node = getItem(index);
		if (null == node) { // Error case
			Log.w(TAG, "getView: is null " + index);
			return null;
		}
		LayoutInflater inflater = (LayoutInflater) parent.getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = inflater.inflate(R.layout.listview_item, parent, false);
		ViewGroup root = (ViewGroup) view; // Actually linear layout
		boolean selected = selectedIndex == index;
		view.setMinimumHeight((int) (28 * view.getContext().getResources().getDisplayMetrics().density));
		SpannableStringBuilder text = customize(view, node, index == selectedIndex);
		synchronized (remoteRenders) { // Lock for modifications
			RemoteViews rv = remoteRenders.get(node.text);
			if (null != rv) { // Have remote views
				// Log.i(TAG, "getView: " + node.text + ", remote found");
				View renderResult = rv.apply(parent.getContext(), root);
				root.addView(renderResult);
				if (!selected) { // Hide top part when not selected
				}
			} else {
				if (!remoteRenders.containsKey(node.text)) {
					// Not started yet - start
					remoteRender(node, root);
				}
			}
		}
		return view;
	}

	private static int getColor(Node node, boolean selected, Theme theme) {
		if (node.collapsed) { // Collapsed
			return theme.colorCollapsed;
		}
		return 0;
	}

	private void remoteRender(final Node node, final View parent) {
		synchronized (remoteRenders) { // Lock
			remoteRenders.put(node.text, null); // No remote render at start
		}
		AsyncTask<Void, Void, RemoteViews> task = new AsyncTask<Void, Void, RemoteViews>() {

			@Override
			protected RemoteViews doInBackground(Void... params) {
				for (Plugin plugin : remoteRenderPlugins) { // every plugin
					RemoteViews res = null;
					try { // Remote error
						res = plugin.render(node, theme, parent.getWidth());
					} catch (Exception e) {
					}
					if (null != res) { // Got remote view
						return res;
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(RemoteViews result) {
				if (null != result) { // Got remote view
					synchronized (remoteRenders) { // Lock
						remoteRenders.put(node.text, result);
					}
					dataChanged();
				}
			}
		};
		task.execute();
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return getCount() == 0;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		this.observer = observer;
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		if (null != observer && observer.equals(this.observer)) { // De-register
			this.observer = null;
		}
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int arg0) {
		return true;
	}

	public void dataChanged() {
		if (null != observer) { // Have observer
			observer.onChanged();
		}
	}

	public void setController(Controller controller) {
		this.controller = controller;
		resetPlugins();
	}

	private void fixLevel(Node node, int level) {
		node.level = level;
		if (null != node.children) { // Have children
			List<Node> children = node.children;
			for (Node ch : children) { // ch = child
				fixLevel(ch, level + 1);
			}
		}
	}

	public boolean setRoot(Node node, boolean showRoot) {
		if (node == null) { // Invalid node
			return false;
		}
		root = node;
		fixLevel(root, 0);
		this.showRoot = showRoot;
		return true;
	}

	public Node getRoot() {
		return root;
	}

	public int getSelectedIndex() {
		return selectedIndex;
	}

	public void setSelectedIndex(int index) {
		selectedIndex = index;
		if (null != listener) { // Have listener
			listener.itemSelected(selectedIndex);
		}
	}

	public boolean isShowRoot() {
		return showRoot;
	}

	public static RemoteViews renderRemote(Controller controller, Node node, String left, String themeName) {
		RemoteViews result = new RemoteViews(App.getInstance().getPackageName(), R.layout.listview_item);
		Theme theme = ThemeProvider.getTheme(themeName);
		ListViewAdapter instance = new ListViewAdapter(null, theme);
		instance.setController(controller);
		SpannableStringBuilder text = new SpannableStringBuilder();
		instance.textFormatter.writePlainText(node, text, theme.colorText, node.text, false);
		// result.setViewVisibility(R.id.listview_item_menu_icon, View.GONE);
		result.setTextViewText(R.id.listview_item_text, text);
		StringBuffer leftBuffer = new StringBuffer();
		for (int i = 0; i < node.level; i++) {
			leftBuffer.append(' ');
		}
		result.setTextViewText(R.id.listview_item_left, leftBuffer.toString());
		return result;
	}

	class PluginFormatter implements NodeTextFormatter {

		private Plugin plugin;
		private int index;

		public PluginFormatter(Plugin plugin, int index) {
			this.plugin = plugin;
			this.index = index;
		}

		@Override
		public Pattern getPattern(Node note, boolean selected) {
			try { // Remote errors
				String pattern = plugin.getPattern(index, note, selected);
				// Log.i(TAG, "getPattern: " + note.text + ", " + pattern);
				if (null == pattern) { // Not needed
					return null;
				}
				return Pattern.compile(pattern);
			} catch (Exception e) {
				// Log.w(TAG, "Error getting pattern", e);
			}
			return null;
		}

		@Override
		public void format(Node note, SpannableStringBuilder sb, Matcher m, String text, boolean selected) {
			try { // Remote errors
				FormatSpan[] spans = plugin.format(index, theme, note, m.group(), selected);
				if (null == spans) { // No spans
					// Log.i(TAG, "No format");
					return;
				}
				// Log.i(TAG, "format: " + text + ", " + spans.length);
				for (FormatSpan span : spans) { // Have span
					PlainTextFormatter.addSpan(sb, span.getText(), span.getSpans());
				}
			} catch (Exception e) {
				// Log.w(TAG, "Error formatting");
			}
		}

	}

	private List<NodeTextFormatter> formattersFromPlugin(Plugin plugin) {
		List<NodeTextFormatter> result = new ArrayList<NodeTextFormatter>();
		try { // Remote errors
			int formatters = plugin.getFormatterCount();
			// Log.i(TAG, "formattersFromPlugin: " + formatters + ", " +
			// plugin);
			for (int i = 0; i < formatters; i++) { // Create new Formatter
				result.add(new PluginFormatter(plugin, i));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error creating plugin formatter", e);
		}
		return result;
	}

	public void resetPlugins() {
		remoteRenders.clear();
		remoteRenderPlugins = controller.getPlugins(PluginInfo.PLUGIN_CAN_RENDER);
		localPlugins = controller.getPlugins(LocalPlugin.class, PluginInfo.PLUGIN_ANY);
		List<NodeTextFormatter> formatters = new ArrayList<NodeTextFormatter>();
		List<Plugin> plugins = controller.getPlugins(PluginInfo.PLUGIN_CAN_FORMAT);
		// Log.i(TAG, "setController: plugins: " + plugins.size());
		for (Plugin plugin : plugins) { // Create formatters for every plugin
			formatters.addAll(formattersFromPlugin(plugin));
		}
		formatters.add(defaultTextFormatter);
		textFormatter.setFormatters(formatters.toArray(new NodeTextFormatter[0]));
	}

	public int find(Node node) {
		Log.i(TAG, "Search: " + node + ", " + getCount());
		for (int i = 0; i < getCount(); i++) { // Search
			Node n = getItem(i);
			Log.i(TAG, "Compare: " + node + " and " + n);
			if (n.id.equals(node.id)) { // Found
				return i;
			}
		}
		return -1;
	}

}
