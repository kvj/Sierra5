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
import org.kvj.sierra5.data.Controller.ItemPattern;
import org.kvj.sierra5.data.Controller.SearchNodeResult;
import org.kvj.sierra5.ui.adapter.theme.ThemeProvider;
import org.kvj.sierra5.ui.fragment.ListViewFragment.MenuItemRecord;
import org.kvj.sierra5.ui.fragment.ListViewFragment.PluginMenuRecord;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class EditorViewFragment extends SherlockFragment {

	public static final String KEY_TEXT = "edit_text";
	public static final String KEY_TEXT_ORIG = "edit_text_orig";
	private static final String TAG = "EditorFragment";

	public static interface EditorViewFragmentListener {

		public void saved(Node node, boolean close);

		public void toggleLoad(boolean load);
	}

	private EditorViewFragmentListener listener = null;
	private Node parent = null;
	private Node node = null;
	private Node saveMe = null;
	private boolean isAdding = false;
	private Controller controller = null;
	private EditText editText = null;
	private ActionBar actionBar = null;
	private String oldText = null;
	List<MenuItemRecord<Node>> contextMenu = new ArrayList<MenuItemRecord<Node>>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		actionBar = getSherlockActivity().getSupportActionBar();
		setHasOptionsMenu(true);
		View view = inflater.inflate(R.layout.editorview_fragment, container,
				false);
		editText = (EditText) view.findViewById(R.id.editorview);
		String themeName = App.getInstance().getStringPreference(
				R.string.theme, R.string.themeDefault);
		Theme theme = ThemeProvider.getTheme(themeName);
		editText.setTextColor(theme.colorText);
		editText.setBackgroundColor(theme.colorBackground);
		editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, App.getInstance()
				.getIntPreference(R.string.docFont, R.string.docFontDefault));
		disable();
		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.editor_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return onMenuSelected(item);
	}

	/**
	 * Loads data
	 */
	public void setController(Bundle data, Controller controller,
			EditorViewFragmentListener listener) {
		this.listener = listener;
		this.controller = controller;
		String file = data.getString(Constants.EDITOR_INTENT_FILE);
		oldText = data.getString(KEY_TEXT_ORIG);
		isAdding = data.getBoolean(Constants.EDITOR_INTENT_ADD, false);
		if (null == file) { // No file - empty editor
			return;
		}
		boolean useTemplatePath = data.getBoolean(Constants.INTENT_TEMPLATE,
				false);
		loadNode(file, data.getStringArray(Constants.EDITOR_INTENT_ITEM),
				isAdding, data.getString(Constants.EDITOR_INTENT_ADD_TEMPLATE),
				useTemplatePath);
	}

	private void editNode(Node n, String template) {
		if (null == actionBar || null == editText) {
			return;
		}
		node = n;
		if (isAdding) { // Create new Node, switch to this node
			saveMe = node.createChild(
					Node.TYPE_TEXT,
					"",
					App.getInstance().getIntPreference(R.string.tabSize,
							R.string.tabSizeDefault));
		} else { // Existing Node
			saveMe = n;
		}
		if (isAdding) { // New item
			actionBar.setTitle('+' + n.text);
		} else { // Existing item
			actionBar.setTitle(n.text);
		}
		String text = "";
		int cursorPos = -1;
		if (null != template) { // Have template - convert
			ItemPattern pattern = controller.parsePattern(template, false);
			text = pattern.converted;
			int cursorIndex = text.indexOf("${|}");
			if (-1 != cursorIndex) { // Found cursor position
				cursorPos = cursorIndex;
				text = text.replace("${|}", "");
			}
		}
		// Log.i(TAG, "Edit node: " + text + ", " + template + ", " +
		// cursorPos);
		if (!isAdding) { // Text is empty
			text = controller.getEditableContents(node);
		}
		oldText = text;
		editText.setText(text);
		editText.setEnabled(true);
		if (-1 == cursorPos) { // Don't have position - move to the end
			cursorPos = text.length();
		}
		editText.setSelection(cursorPos);
	}

	public void onSaveState(Bundle outState) {
		if (null != node) { // Have node
			outState.putString(Constants.EDITOR_INTENT_FILE, node.file);
			if (null != node.textPath) { // Have text path
				outState.putStringArray(Constants.EDITOR_INTENT_ITEM,
						node.textPath.toArray(new String[0]));
			}
			outState.putString(KEY_TEXT, editText.getText().toString());
			outState.putString(KEY_TEXT_ORIG, oldText);
			outState.putBoolean(Constants.EDITOR_INTENT_ADD, isAdding);
		}
	}

	private void save(final boolean close) {
		if (null == node || null == parent) { // No node loaded
			SuperActivity.notifyUser(getActivity(), "No item loaded");
			return;
		}
		toggleProgress(true);
		final String text = editText.getText().toString();
		AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Void... params) {
				saveMe.raw = text;
				if (!controller.saveFile(parent, null)) { // Save failed
					return false; // Save failed
				}
				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				toggleProgress(false);
				if (!result) { // Save failed
					SuperActivity.notifyUser(getActivity(), "Save failed");
					return; // Save failed
				}
				SuperActivity.notifyUser(getActivity(), "Saved");
				oldText = text; // To detect changes
				isAdding = false; // Edit existing from now
				node = saveMe; // Save this
				actionBar.setTitle(node.text);
				if (null != listener) { // Report saved
					listener.saved(node, close);
				}
			}

		};
		task.execute();
	}

	public void disable() {
		editText.setEnabled(false);
		editText.setText("");
		oldText = "";
	}

	public boolean onMenuSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_save: // Save
			save(false);
			return true;
		case R.id.menu_save_close: // Save and close
			save(true);
			return true;
		case R.id.menu_edit_more: // Show menu
			showMenu(null);
			return true;
		}
		return false;
	}

	private void showMenu(final PluginMenuRecord parent) {
		contextMenu.clear();
		toggleProgress(true);
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try { // Get menus from plugins
					int menuIndex = 0;
					// Log.i(TAG, "Getting menu from plugins");
					for (Plugin plugin : controller
							.getPlugins(PluginInfo.PLUGIN_HAVE_EDIT_MENU)) {
						// menu from every plugin
						MenuItemInfo[] menus = plugin
								.getEditorMenu(
										null != parent ? parent.info.getId()
												: -1, node);
						if (null == menus) { // No menus
							continue;
						}
						for (MenuItemInfo info : menus) { // Add menus
							contextMenu.add(new PluginMenuRecord(menuIndex++,
									node, plugin, info));
						}
					}
				} catch (Exception e) {
					Log.w(TAG, "Error getting menus", e);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if (contextMenu.size() > 0) { // Show menu
					registerForContextMenu(editText);
					getActivity().openContextMenu(editText);
					unregisterForContextMenu(editText);
				}
				toggleProgress(false);
			}

		};
		task.execute();
	}

	public void loadNode(String file, String[] path, boolean newNode,
			String template, boolean useTemplatePath) {
		final Node n = controller.nodeFromPath(file, null, useTemplatePath);
		// File found
		if (null == n) { // Invalid file
			SuperActivity.notifyUser(getActivity(), "File not found");
			return;
		}
		SearchNodeResult res = controller.searchInNode(n, file, path,
				useTemplatePath);
		if (null == res || !res.found) { // Text not found
			SuperActivity.showQuestionDialog(getActivity(), "Append to file?",
					"Text not found. Append to file?", new Runnable() {

						@Override
						public void run() {
							parent = n;
							isAdding = true;
							editNode(n, null);
						}
					}, new Runnable() {

						@Override
						public void run() {
						}
					});
			return;
		}
		parent = n;
		isAdding = newNode;
		editNode(res.node, template);
	}

	public static boolean stringChanged(String s1, String s2,
			String... emptyStrings) {
		boolean s1empty = s1 == null || "".equals(s1.trim());
		boolean s2empty = s2 == null || "".equals(s2.trim());
		if (s1empty && s2empty) {
			return false;
		}
		if (s1empty && !s2empty && null != emptyStrings) {
			for (int i = 0; i < emptyStrings.length; i++) {
				if (emptyStrings[i].equals(s2.trim())) {
					return false;
				}
			}
		}
		if (!s1empty && !s2empty) {
			return !s1.trim().equals(s2.trim());
		}
		return true;
	}

	public boolean isModified() {
		if (null != oldText
				&& stringChanged(oldText, editText.getText().toString())) {
			// Input changed
			return true;
		}
		return false;
	}

	private void toggleProgress(boolean start) {
		if (null != listener) { // Have listener
			listener.toggleLoad(start);
		}
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
		if (item.getItemId() < contextMenu.size()) {
			onContextMenu(contextMenu.get(item.getItemId()));
		}
		return true;
	}

	private void onContextMenu(MenuItemRecord<Node> item) {
		if (item instanceof PluginMenuRecord) { // Plugin menu
			final PluginMenuRecord pitem = (PluginMenuRecord) item;
			if (pitem.info.getType() == MenuItemInfo.MENU_ITEM_SUBMENU) {
				// This is submenu
				showMenu(pitem);
				return;
			}
			final String text = editText.getText().toString();
			// Execute action
			AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

				@Override
				protected String doInBackground(Void... params) {
					try { // Remote errors
							// Log.i(TAG, "Before exec: " + node.text);
						return pitem.plugin.executeEditAction(
								pitem.info.getId(), text, node);
					} catch (Exception e) {
						Log.w(TAG, "Error executing action: " + pitem.title, e);
					}
					return null;
				}

				@Override
				protected void onPostExecute(String result) {
					if (null == result) { // Execute failed
						SuperActivity.notifyUser(getActivity(),
								"Error in action");
					} else {
						int cursorPos = editText.getSelectionStart();
						ItemPattern pattern = controller.parsePattern(result,
								false);
						String newText = pattern.converted;
						int cursorIndex = newText.indexOf("${|}");
						if (-1 != cursorIndex) { // Found cursor position
							newText = newText.replace("${|}", "");
						} else {
							cursorIndex = newText.length();
						}
						if (pitem.info.getType() == MenuItemInfo.MENU_ITEM_INSERT_TEXT) {
							// Insert template
							editText.getText().insert(cursorPos, newText);
							editText.setSelection(cursorPos + cursorIndex);
						} else if (pitem.info.getType() == MenuItemInfo.MENU_ITEM_REPLACE_TEXT) {
							// Replace text
							editText.setText(newText);
							editText.setSelection(cursorIndex);
						}

					}
				}
			};
			task.execute();
		}
	}

}
