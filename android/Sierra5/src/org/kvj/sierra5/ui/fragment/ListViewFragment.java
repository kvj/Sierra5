package org.kvj.sierra5.ui.fragment;

import java.util.ArrayList;
import java.util.List;

import org.kvj.bravo7.SuperActivity;
import org.kvj.sierra5.R;
import org.kvj.sierra5.data.Controller;
import org.kvj.sierra5.data.Controller.SearchNodeResult;
import org.kvj.sierra5.data.Node;
import org.kvj.sierra5.ui.ConfigurationView;
import org.kvj.sierra5.ui.adapter.ListViewAdapter;

import android.content.Intent;
import android.os.AsyncTask;
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

public class ListViewFragment extends Fragment {

	public static interface ListViewFragmentListener {

		public void open(Node node);

		public void edit(Node node);
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

	public static final String KEY_ROOT = "list_root";
	public static final String KEY_FILE = "list_file";
	public static final String KEY_ITEM = "list_item";

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

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.listview_fragment, container,
				false);
		listView = (ListView) view.findViewById(R.id.listview);
		adapter = new ListViewAdapter();
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
		getActivity().getMenuInflater().inflate(R.menu.list_menu,
				actionBar.asMenu());
		return view;
	}

	private void openNewList(Node node) {
		if (null != listener) { // Have listener
			listener.open(node);
		}
	}

	private void editItem(Node node) {
		if (null != listener) { // Have listener
			listener.edit(node);
		}
	}

	private void onLongClick(Node node) {
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
			outState.putString(KEY_ROOT, rootFile);
		}
		int selectedIndex = adapter.getSelectedIndex();
		Log.i(TAG, "onSaveInstanceState: " + rootFile + ", " + selectedIndex);
		if (-1 != selectedIndex) { // Have selected
			Node n = adapter.getItem(selectedIndex);
			outState.putString(KEY_FILE, n.file);
			if (null != n.textPath) { // Have path - text selected
				outState.putStringArray(KEY_ITEM,
						n.textPath.toArray(new String[0]));
			}
		}
	}

	/**
	 * Loads data
	 */
	public void setController(Bundle data, Controller controller,
			ListViewFragmentListener listener) {
		this.listener = listener;
		this.controller = controller;
		String file = data.getString(KEY_ROOT);
		boolean rootSet = false;
		if (null != file) { // Have file in Activity parameters
			rootSet = adapter.setRoot(controller.nodeFromPath(file), true);
			if (rootSet) { // Result = OK - save
				rootFile = adapter.getRoot().file;
			}
		} else { // No file - show root
			rootSet = adapter.setRoot(
					controller.nodeFromPath("/mnt/sdcard/Documents"), false);
		}
		Log.i(TAG, "rootSet: " + rootSet);
		if (rootSet) { // Root set - expand root
			actionBar.setTitle(adapter.getRoot().text);
			expandTree(adapter.getRoot(), data.getString(KEY_FILE),
					data.getStringArray(KEY_ITEM));
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
		AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				if (null == controller) { // No controller - no refresh
					return null;
				}
				controller.expand(node, forceExpand, null);
				return null;
			}

			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
				if (result == null) { // State changed - notify adapter
					adapter.dataChanged();
				} else {
					SuperActivity.notifyUser(getActivity(), result);
				}
			}
		};
		task.execute();
	}

	private void expandTree(final Node node, final String file,
			final String[] path) {
		AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				if (null == controller) { // No controller - no refresh
					return -1;
				}
				if (null == file) { // Don't need to search
					controller.expand(node, true, null);
					return -1;
				}
				SearchNodeResult res = controller
						.searchInNode(node, file, path);
				if (null == res) { // Error expand
					return -1;
				}
				return res.index;
			}

			@Override
			protected void onPostExecute(Integer result) {
				super.onPostExecute(result);
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
		};
		task.execute();
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
			editItem(item.data);
			break;
		}
	}

	public void selectNode(Node node) {
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
		}
	}

	private void showConfiguration() {
		Intent intent = new Intent(getActivity(), ConfigurationView.class);
		getActivity().startActivity(intent);
	}

}
