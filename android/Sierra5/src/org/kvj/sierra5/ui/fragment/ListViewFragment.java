package org.kvj.sierra5.ui.fragment;

import java.util.ArrayList;
import java.util.List;

import org.kvj.bravo7.SuperActivity;
import org.kvj.sierra5.App;
import org.kvj.sierra5.R;
import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.plugin.MenuItemInfo;
import org.kvj.sierra5.common.plugin.Plugin;
import org.kvj.sierra5.common.plugin.PluginInfo;
import org.kvj.sierra5.common.theme.Theme;
import org.kvj.sierra5.data.Controller;
import org.kvj.sierra5.data.Controller.SearchNodeResult;
import org.kvj.sierra5.ui.ConfigurationView;
import org.kvj.sierra5.ui.adapter.ListViewAdapter;
import org.kvj.sierra5.ui.adapter.ListViewAdapter.ListViewAdapterListener;
import org.kvj.sierra5.ui.adapter.theme.ThemeProvider;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class ListViewFragment extends SherlockFragment implements
		ListViewAdapterListener {

	public enum EditType {
		Edit, Add, Remove
	};

	public static interface ListViewFragmentListener {

		public void open(Node node);

		public void edit(Node node, EditType editType);

		public void toggleLoad(boolean load);
	}

	static class MenuItemRecord<T> {

		public MenuItemRecord(int type, String title, T data) {
			this.type = type;
			this.title = title;
			this.data = data;
		}

		int type = 0;
		String title = "";
		T data = null;
	}

	static class PluginMenuRecord extends MenuItemRecord<Node> {

		Plugin plugin;
		MenuItemInfo info;

		public PluginMenuRecord(int index, Node node, Plugin plugin,
				MenuItemInfo info) {
			super(index, info.getText(), node);
			this.plugin = plugin;
			this.info = info;
		}

	}

	private static final int MENU_EDIT = 0;
	private static final int MENU_OPEN = 1;

	private static final String TAG = "ListFragment";
	private ListView listView = null;
	private ListViewAdapter adapter = null;
	protected Controller controller = null;
	private String rootFile = null;
	List<MenuItemRecord<Node>> contextMenu = new ArrayList<MenuItemRecord<Node>>();
	ListViewFragmentListener listener = null;
	private ActionBar actionBar = null;
	private boolean selectMode = false;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (!selectMode) { // Have menu only in normal mode
			inflater.inflate(R.menu.list_menu, menu);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return onMenuSelected(item);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		actionBar = getSherlockActivity().getSupportActionBar();
		setHasOptionsMenu(true);
		View view = inflater.inflate(R.layout.listview_fragment, container,
				false);
		listView = (ListView) view.findViewById(R.id.listview);
		String themeName = App.getInstance().getStringPreference(
				R.string.theme, R.string.themeDefault);
		Theme theme = ThemeProvider.getTheme(themeName);
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
				onLongClick(null, node, index);
				return true;
			}
		});
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

	private void onLongClick(final PluginMenuRecord parent, final Node node,
			final int index) {
		if (selectMode && null != listener) { // Have listener and select mode
			listener.open(node);
			return;
		}
		contextMenu.clear();
		toggleProgress(true);
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				if (Node.TYPE_FILE == node.type || Node.TYPE_TEXT == node.type) {
					// File or text - edit
					contextMenu.add(new MenuItemRecord<Node>(MENU_EDIT, "Edit",
							node));
				}
				if (Node.TYPE_FILE == node.type
						|| Node.TYPE_FOLDER == node.type) {
					// File or folder - open
					contextMenu.add(new MenuItemRecord<Node>(MENU_OPEN, "Open",
							node));
				}
				if (adapter.getSelectedIndex() == index) { // Menu on selected
															// index
					try { // Get menus from plugins
						int menuIndex = 2;
						// Log.i(TAG, "Getting menu from plugins");
						for (Plugin plugin : controller
								.getPlugins(PluginInfo.PLUGIN_HAVE_MENU)) {
							// menu from every plugin
							MenuItemInfo[] menus = plugin.getMenu(
									null != parent ? parent.info.getId() : -1,
									node);
							if (null == menus) { // No menus
								// Log.i(TAG, "Plugin: " + menuIndex +
								// " no menus");
								continue;
							}
							// Log.i(TAG, "Plugin: " + menuIndex + " menus: "
							// + menus.length);
							for (MenuItemInfo info : menus) { // Add menus
								contextMenu.add(new PluginMenuRecord(
										menuIndex++, node, plugin, info));
								// Log.i(TAG, "Plugin: " + menuIndex + " menu: "
								// + info.getText());
							}
						}
					} catch (Exception e) {
						Log.w(TAG, "Error getting menus", e);
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if (1 == contextMenu.size()) {
					onContextMenu(contextMenu.get(0));
				}
				if (contextMenu.size() > 1) { // Show menu
					registerForContextMenu(listView);
					getActivity().openContextMenu(listView);
					unregisterForContextMenu(listView);
				}
				toggleProgress(false);
			}

		};
		task.execute();
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
		if (!selectMode && null != actionBar) { // Create toolbar
			// getActivity().getMenuInflater().inflate(R.menu.list_menu,
			// actionBar.asMenu());
		}
		this.listener = listener;
		this.controller = controller;
		boolean useTemplatePath = data.getBoolean(Constants.INTENT_TEMPLATE,
				false);
		String file = data.getString(Constants.LIST_INTENT_ROOT);
		boolean rootSet = false;
		adapter.setController(controller);
		if (null != file) { // Have file in Activity parameters
			rootSet = adapter.setRoot(
					controller.nodeFromPath(file, null, useTemplatePath), true);
			if (rootSet) { // Result = OK - save
				rootFile = adapter.getRoot().file;
			}
		} else { // No file - show root
			String rootFolder = controller.getRootFolder();
			rootSet = adapter.setRoot(
					controller.nodeFromPath(rootFolder, null, false), false);
		}
		// Log.i(TAG, "rootSet: " + rootSet);
		adapter.setSelectedIndex(-1);
		if (rootSet) { // Root set - expand root
			adapter.getRoot().level = 0;
			actionBar.setTitle(adapter.getRoot().text);
			expandTree(adapter.getRoot(),
					data.getString(Constants.LIST_INTENT_FILE),
					data.getStringArray(Constants.LIST_INTENT_ITEM),
					useTemplatePath);
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
		toggleProgress(true);
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				controller.expand(node, forceExpand, false);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				adapter.dataChanged();
				toggleProgress(false);
			}

		};
		task.execute();
	}

	private void expandTree(final Node node, final String file,
			final String[] path, final boolean useTemplate) {
		if (null == controller) { // No controller - no refresh
			return;
		}
		// toggleProgress(true);
		// AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void,
		// Integer>() {
		//
		// @Override
		// protected Integer doInBackground(Void... params) {
		Integer result = null;
		if (null == file) { // Don't need to search
			boolean exp = controller.expand(node, true, false);
			result = -1;
		} else {
			SearchNodeResult res = controller.searchInNode(node, file, path,
					useTemplate);
			if (null == res) { // Error expand
				result = -1;
			} else {
				result = res.index;
			}
		}
		// return result;
		// }
		//
		// @Override
		// protected void onPostExecute(Integer result) {
		if (result != null) { // State changed - notify adapter
			if (-1 != result) { // Not expanded
				adapter.setSelectedIndex(adapter.isShowRoot() ? result
						: result - 1);
			}
			adapter.dataChanged();
		} else {
			SuperActivity.notifyUser(getActivity(), "Item not found");
		}
		// toggleProgress(false);
		// }
		//
		// }
		// task.execute();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.clear();
		for (int i = 0; i < contextMenu.size(); i++) {
			MenuItemRecord info = contextMenu.get(i);
			menu.add(0, i, i, info.title);
		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		Log.i(TAG, "Clicked on " + item.getItemId());
		if (item.getItemId() < contextMenu.size()) {
			onContextMenu(contextMenu.get(item.getItemId()));
		}
		return true;
	}

	private void onContextMenu(MenuItemRecord<Node> item) {
		// Log.i(TAG, "Context menu: " + item.type + ", " + item);
		switch (item.type) {
		case MENU_OPEN: // Open new Activity
			openNewList(item.data);
			return;
		case MENU_EDIT: // Edit item
			editItem(item.data, false);
			return;
		}
		if (item instanceof PluginMenuRecord) { // Plugin menu
			int index = adapter.getSelectedIndex();
			final Node node = adapter.getItem(index);
			final PluginMenuRecord pitem = (PluginMenuRecord) item;
			if (pitem.info.getType() == MenuItemInfo.MENU_ITEM_SUBMENU) {
				// This is submenu
				onLongClick(pitem, node, index);
				return;
			}
			// Execute action
			AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {

				@Override
				protected Boolean doInBackground(Void... params) {
					try { // Remote errors
							// Log.i(TAG, "Before exec: " + node.text);
						return pitem.plugin.executeAction(pitem.info.getId(),
								node);
					} catch (Exception e) {
						Log.w(TAG, "Error executing action: " + pitem.title, e);
					}
					return false;
				}

				@Override
				protected void onPostExecute(Boolean result) {
					if (!result) { // Execute failed
						SuperActivity.notifyUser(getActivity(),
								"Error in action");
					} else {
						// Update selection
						// Log.i(TAG, "After exec: " + node.text);
						refresh(node);
					}
				}
			};
			task.execute();
		}
	}

	public void selectNode(final Node node) {
		if (null == node) { // Nothing
			return;
		}
		if (null == adapter.getRoot()) { // Don't have root - stop
			return;
		}
		toggleProgress(true);
		AsyncTask<Void, Void, SearchNodeResult> task = new AsyncTask<Void, Void, Controller.SearchNodeResult>() {

			@Override
			protected SearchNodeResult doInBackground(Void... params) {
				return controller.searchInNode(adapter.getRoot(), node.file,
						Node.list2array(node.textPath, new String[0]), false);
			}

			@Override
			protected void onPostExecute(SearchNodeResult res) {
				if (null != res) { // Found
					// Log.i(TAG, "selectNode: " + node.file + ", "
					// + node.textPath + ", " + res.found);
					adapter.setSelectedIndex(adapter.isShowRoot() ? res.index
							: res.index - 1);
				}
				adapter.resetPlugins();
				adapter.dataChanged();
				toggleProgress(false);
			}

		};
		task.execute();
	}

	public boolean onMenuSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_config: // Show configuration
			showConfiguration();
			return true;
		case R.id.menu_add: // Start add
			editItem(adapter.getItem(adapter.getSelectedIndex()), true);
			return true;
		case R.id.menu_remove: // Remove
			removeItem(adapter.getItem(adapter.getSelectedIndex()));
			return true;
		case R.id.menu_reload:
			refresh(null);
			return true;
		}
		return false;
	}

	private void removeItem(Node item) {
		if (null != item && null != listener) { // Both item and listener here
			listener.edit(item, EditType.Remove);
		}
	}

	public void refresh(Node node) {
		selectNode(node != null ? node : adapter.getItem(adapter
				.getSelectedIndex()));
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
		// actionBar.findAction(R.id.menu_add).setVisible(canAdd);
		// actionBar.findAction(R.id.menu_remove).setVisible(canRemove);
	}

	private void toggleProgress(boolean start) {
		if (null != listener) { // Have listener
			listener.toggleLoad(start);
		}
	}
}
