package org.kvj.sierra5.ui.adapter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kvj.bravo7.format.PlainTextFormatter;
import org.kvj.bravo7.format.TextFormatter;
import org.kvj.sierra5.App;
import org.kvj.sierra5.R;
import org.kvj.sierra5.data.Node;
import org.kvj.sierra5.ui.adapter.theme.DarkTheme;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

public class ListViewAdapter implements ListAdapter {

	public static interface ListViewAdapterListener {
		public void itemSelected(int selected);
	}

	private static final String TAG = "Adapter";

	private Node root = null;
	private boolean showRoot = false;
	private DataSetObserver observer = null;
	private int selectedIndex = -1;
	private ListViewAdapterListener listener = null;
	private int textSize = 0;
	private PlainTextFormatter<Node> textFormatter = null;

	private DarkTheme theme = null;

	class DefaultTextFormatter implements NodeTextFormatter {

		@Override
		public Pattern getPattern(Node note, boolean selected) {
			if (Node.TYPE_TEXT == note.type) { // Text - skip
				return null;
			}
			return TextFormatter.eatAll;
		}

		@Override
		public void format(Node note, SpannableStringBuilder sb, Matcher m,
				String text, boolean selected) {
			int color = Node.TYPE_FOLDER == note.type ? theme.cbLYellow
					: theme.ccLBlue;
			PlainTextFormatter.addSpan(sb, m.group(0), new ForegroundColorSpan(
					color));
		}

	}

	@SuppressWarnings("unchecked")
	public ListViewAdapter(ListViewAdapterListener listener, DarkTheme theme) {
		this.listener = listener;
		this.theme = theme;
		textSize = App.getInstance().getIntPreference(R.string.docFont,
				R.string.docFontDefault);
		textFormatter = new PlainTextFormatter<Node>(new DefaultTextFormatter());
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
		for (Node child : node.children) { // Every children
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

	public void customize(View view, Node node, boolean selected) {
		TextView textView = (TextView) view
				.findViewById(R.id.listview_item_text);
		textView.setTextColor(theme.colorText);
		textView.setBackgroundColor(theme.colorBackground);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
		SpannableStringBuilder text = new SpannableStringBuilder();
		for (int i = 0; i < node.level; i++) { // Append level chars first
			text.append(" ");
		}
		textFormatter.writePlainText(node, text, theme.colorText, node.text,
				selected);
		if (selected) {
			text.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 0, text.length(),
					0);
		}
		textView.setText(text);
	}

	@Override
	public View getView(int index, View view, ViewGroup parent) {
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) parent.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.listview_item, parent, false);
		}
		Node node = getItem(index);
		if (null == node) { // Error case
			return null;
		}
		// Log.i(TAG, "getView: " + index + ", " + node);
		customize(view, node, index == selectedIndex);
		return view;
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

	public boolean setRoot(Node node, boolean showRoot) {
		if (node == null) { // Invalid node
			return false;
		}
		root = node;
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

}
