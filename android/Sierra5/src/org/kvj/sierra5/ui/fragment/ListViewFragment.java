package org.kvj.sierra5.ui.fragment;

import java.util.ArrayList;
import java.util.List;

import org.kvj.bravo7.SuperActivity;
import org.kvj.sierra5.App;
import org.kvj.sierra5.R;
import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.data.Controller;
import org.kvj.sierra5.data.Controller.SearchNodeResult;
import org.kvj.sierra5.ui.ConfigurationView;
import org.kvj.sierra5.ui.adapter.ListViewAdapter;
import org.kvj.sierra5.ui.adapter.ListViewAdapter.ListViewAdapterListener;
import org.kvj.sierra5.ui.adapter.theme.DarkTheme;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.markupartist.android.widget.ActionBar;

public class ListViewFragment extends Fragment implements
		ListViewAdapterListener {

	public enum EditType {
		Edit, Add, Remove
	};

	public static interface ListViewFragmentListener {

		public void open(Node node);

		public void edit(Node node, EditType editType);
	}

	class MenuItemInfo<T> {

		public MenuItemInfo(int type, String title, T data) {
			this.type = type;
			this.title = title;
			this.data = data;
		}

		int type = 0;
		String title = "";
		T data = null;
	}

	private static final int MENU_EDIT = 0;
	private static final int MENU_OPEN = 1;

	private static final String TAG = "ListFragment";
	private ListView listView = null;
	private ListViewAdapter adapter = null;
	protected Controller controller = null;
	private String rootFile = null;
	List<MenuItemInfo<Node>> contextMenu = new ArrayList<MenuItemInfo<Node>>();
	ListViewFragmentListener listener = null;
	private ActionBar actionBar = null;
	private boolean selectMode = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.listview_fragment, container,
				false);
		listView = (ListView) view.findViewById(R.id.listview);
		String themeName = App.getInstance().getStringPreference(
				R.string.theme, R.string.themeDefault);
		DarkTheme theme = DarkTheme.getTheme(themeName);
		if (null != listView) {
			listView.setBackgroundColor(theme.colorBackground);

		}
		adapter = new ListViewAdapter(this, theme);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> view, View arg1, int index,
					long id) {
				itemClick(index);
			}
		});
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> view, View arg1,
					int index, long id) {
				Node node = adapter.getItem(index);
				if (null == node) { // Node not found - strange
					return true;
				}
				onLongClick(node);
				return true;
			}
		});
		actionBar = (ActionBar) view.findViewById(R.id.actionbar);
		return view;
	}

	private void openNewList(Node node) {
		if (null != listener) { // Have listener
			listener.open(node);
		}
	}

	private void editItem(Node node, boolean addNew) {
		if (null != listener && null != node) { // Have listener
			listener.edit(node, addNew ? EditType.Add : EditType.Edit);
		}
	}

	private void onLongClick(Node node) {
		if (selectMode && null != listener) { // Have listener and select mode
			listener.open(node);
			return;
		}
		contextMenu.clear();
		if (Node.TYPE_FILE == node.type || Node.TYPE_TEXT == node.type) {
			// File or text - edit
			contextMenu.add(new MenuItemInfo<Node>(MENU_EDIT, "Edit", node));
		}
		if (Node.TYPE_FILE == node.type || Node.TYPE_FOLDER == node.type) {
			// File or folder - open
			contextMenu.add(new MenuItemInfo<Node>(MENU_OPEN, "Open", node));
		}
		if (1 == contextMenu.size()) {
			onContextMenu(contextMenu.get(0));
		}
		if (contextMenu.size() > 1) { // Show menu
			registerForContextMenu(listView);
			getActivity().openContextMenu(listView);
			unregisterForContextMenu(listView);
		}
	}

	public void onSaveState(Bundle outState) {
		// Save root, if have, file, +path
		if (null != rootFile) { // Save root
			outState.putString(Constants.LIST_INTENT_ROOT, rootFile);
		}
		int selectedIndex = adapter.getSelectedIndex();
		Log.i(TAG, "onSaveInstanceState: " + rootFile + ", " + selectedIndex);
		if (-1 != selectedIndex) { // Have selected
			Node n = adapter.getItem(selectedIndex);
			outState.putString(Constants.LIST_INTENT_FILE, n.file);
			if (null != n.textPath) { // Have path - text selected
				outState.putStringArray(Constants.LIST_INTENT_ITEM,
						n.textPath.toArray(new String[0]));
			}
		}
	}

	/**
	 * Loads data
	 */
	public void setController(Bundle data, Controller controller,
			ListViewFragmentListener listener, boolean selectMode) {
		this.selectMode = selectMode;
		if (!selectMode) { // Create toolbar
			getActivity().getMenuInflater().inflate(R.menu.list_menu,
					actionBar.asMenu());
		}
		this.listener = listener;
		this.controller = controller;
		String file = data.getString(Constants.LIST_INTENT_ROOT);
		boolean rootSet = false;
		if (null != file) { // Have file in Activity parameters
			rootSet = adapter.setRoot(controller.nodeFromPath(file), true);
			if (rootSet) { // Result = OK - save
				rootFile = adapter.getRoot().file;
			}
		} else { // No file - show root
			String rootFolder = controller.getRootFolder();
			rootSet = adapter.setRoot(controller.nodeFromPath(rootFolder),
					false);
		}
		Log.i(TAG, "rootSet: " + rootSet);
		adapter.setSelectedIndex(-1);
		if (rootSet) { // Root set - expand root
			actionBar.setTitle(adapter.getRoot().text);
			expandTree(adapter.getRoot(),
					data.getString(Constants.LIST_INTENT_FILE),
					data.getStringArray(Constants.LIST_INTENT_ITEM));
		} else {
			SuperActivity.notifyUser(getActivity(), "Invalid file/folder");
		}
	}

	private void itemClick(int index) {
		Node node = adapter.getItem(index);
		if (adapter.getSelectedIndex() != index) { // Selected not selected
			adapter.setSelectedIndex(index);
			// Select and force expand
			if (node.collapsed) { // Collapsed - expand
				collapseExpand(node, true);
			} else { // Redraw
				adapter.dataChanged();
			}
		} else { // Same item - just toggle
			collapseExpand(adapter.getItem(index), null);
		}
	}

	private void collapseExpand(final Node node, final Boolean forceExpand) {
		if (null == controller) { // No controller - no refresh
			return;
		}
		controller.expand(node, forceExpand, false);
		adapter.dataChanged();
	}

	private void expandTree(final Node node, final String file,
			final String[] path) {
		if (null == controller) { // No controller - no refresh
			return;
		}
		Integer result = null;
		if (null == file) { // Don't need to search
			controller.expand(node, true, false);
			result = -1;
		} else {
			SearchNodeResult res = controller.searchInNode(node, file, path);
			if (null == res) { // Error expand
				result = -1;
			} else {
				result = res.index;
			}
		}
		if (result != null) { // State changed - notify adapter
			if (-1 != result) { // Not expanded
				adapter.setSelectedIndex(adapter.isShowRoot() ? result
						: result - 1);
			}
			adapter.dataChanged();
		} else {
			SuperActivity.notifyUser(getActivity(), "Item not found");
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.clear();
		for (int i = 0; i < contextMenu.size(); i++) {
			MenuItemInfo info = contextMenu.get(i);
			menu.add(0, i, i, info.title);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Log.i(TAG, "Clicked on " + item.getItemId());
		if (item.getItemId() < contextMenu.size()) {
			onContextMenu(contextMenu.get(item.getItemId()));
		}
		return true;
	}

	private void onContextMenu(MenuItemInfo<Node> item) {
		switch (item.type) {
		case MENU_OPEN: // Open new Activity
			openNewList(item.data);
			break;
		case MENU_EDIT: // Edit item
			editItem(item.data, false);
			break;
		}
	}

	public void selectNode(Node node) {
		if (null == node) { // Nothing
			return;
		}
		if (null != adapter.getRoot()) { // Have root - expand
			SearchNodeResult res = controller.searchInNode(
					adapter.getRoot(),
					node.file,
					node.textPath != null ? node.textPath
							.toArray(new String[0]) : null);
			if (null != res) { // Found
				adapter.setSelectedIndex(adapter.isShowRoot() ? res.index
						: res.index - 1);
			}
			adapter.dataChanged();
		}
	}

	public void onMenuSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_config: // Show configuration
			showConfiguration();
			break;
		case R.id.menu_add: // Start add
			editItem(adapter.getItem(adapter.getSelectedIndex()), true);
			break;
		case R.id.menu_remove: // Remove
			removeItem(adapter.getItem(adapter.getSelectedIndex()));
			break;
		case R.id.menu_reload:
			refresh();
			break;
		}
	}

	private void removeItem(Node item) {
		if (null != item && null != listener) { // Both item and listener here
			listener.edit(item, EditType.Remove);
		}
	}

	public void refresh() {
		selectNode(adapter.getItem(adapter.getSelectedIndex()));
	}

	private void showConfiguration() {
		Intent intent = new Intent(getActivity(), ConfigurationView.class);
		getActivity().startActivity(intent);
	}

	@Override
	public void itemSelected(int selected) {
		if (selectMode) { // Skip
			return;
		}
		Node node = adapter.getItem(selected);
		boolean canAdd = null != node
				&& (Node.TYPE_FILE == node.type || Node.TYPE_TEXT == node.type);
		boolean canRemove = null != node && Node.TYPE_TEXT == node.type;
		actionBar.findAction(R.id.menu_add).setVisible(canAdd);
		actionBar.findAction(R.id.menu_remove).setVisible(canRemove);
	}
}
