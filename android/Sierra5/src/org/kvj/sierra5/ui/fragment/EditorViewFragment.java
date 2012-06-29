package org.kvj.sierra5.ui.fragment;

import org.kvj.bravo7.SuperActivity;
import org.kvj.sierra5.App;
import org.kvj.sierra5.R;
import org.kvj.sierra5.data.Controller;
import org.kvj.sierra5.data.Controller.SearchNodeResult;
import org.kvj.sierra5.data.Node;
import org.kvj.sierra5.ui.adapter.theme.DarkTheme;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.markupartist.android.widget.ActionBar;

public class EditorViewFragment extends Fragment {

	public static final String KEY_FILE = "edit_file";
	public static final String KEY_ITEM = "edit_item";
	public static final String KEY_TEXT = "edit_text";
	public static final String KEY_ADD = "edit_add";
	public static final String KEY_TEXT_ORIG = "edit_text_orig";
	private static final String TAG = "EditorFragment";

	public static interface EditorViewFragmentListener {
		public void saved(Node node);
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

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.editorview_fragment, container,
				false);
		editText = (EditText) view.findViewById(R.id.editorview);
		DarkTheme theme = DarkTheme.getTheme();
		editText.setTextColor(theme.colorText);
		editText.setBackgroundColor(theme.colorBackground);
		editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, App.getInstance()
				.getIntPreference(R.string.docFont, R.string.docFontDefault));
		actionBar = (ActionBar) view.findViewById(R.id.actionbar);
		getActivity().getMenuInflater().inflate(R.menu.editor_menu,
				actionBar.asMenu());
		return view;
	}

	/**
	 * Loads data
	 */
	public void setController(Bundle data, Controller controller,
			EditorViewFragmentListener listener) {
		this.listener = listener;
		this.controller = controller;
		String file = data.getString(KEY_FILE);
		oldText = data.getString(KEY_TEXT_ORIG);
		isAdding = data.getBoolean(KEY_ADD, false);
		if (null == file) { // No file - empty editor
			return;
		}
		loadNode(file, data.getStringArray(KEY_ITEM), isAdding);
	}

	private void editNode(Node n, String text) {
		node = n;
		if (isAdding) { // Create new Node, switch to this node
			saveMe = node.createChild(Node.TYPE_TEXT, "");
		} else { // Existing Node
			saveMe = n;
		}
		if (isAdding) { // New item
			actionBar.setTitle('+' + n.text);
		} else { // Existing item
			actionBar.setTitle(n.text);
		}
		if (null == text) { // Load from Node
			if (isAdding) { // Text is empty
				text = "";
			} else {
				text = controller.getEditableContents(node);
			}
			oldText = text;
		}
		editText.setText(text);
	}

	public void onSaveState(Bundle outState) {
		if (null != node) { // Have node
			outState.putString(KEY_FILE, node.file);
			if (null != node.textPath) { // Have text path
				outState.putStringArray(KEY_ITEM,
						node.textPath.toArray(new String[0]));
			}
			outState.putString(KEY_TEXT, editText.getText().toString());
			outState.putString(KEY_TEXT_ORIG, oldText);
			outState.putBoolean(KEY_ADD, isAdding);
		}
	}

	private void save() {
		if (null == node || null == parent) { // No node loaded
			SuperActivity.notifyUser(getActivity(), "No item loaded");
			return;
		}
		String text = editText.getText().toString();
		saveMe.raw = text;
		if (!controller.saveFile(parent, null)) { // Save failed
			SuperActivity.notifyUser(getActivity(), "Save failed");
			return; // Save failed
		}
		SuperActivity.notifyUser(getActivity(), "Saved");
		oldText = text; // To detect changes
		isAdding = false; // Edit existing from now
		node = saveMe; // Save this
		actionBar.setTitle(node.text);
		if (null != listener) { // Report saved
			listener.saved(node);
		}
	}

	public void onMenuSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_save: // Save
			save();
			break;
		}
	}

	public void loadNode(String file, String[] path, boolean newNode) {
		final Node n = controller.nodeFromPath(file); // File found
		if (null == n) { // Invalid file
			SuperActivity.notifyUser(getActivity(), "File not found");
			return;
		}
		SearchNodeResult res = controller.searchInNode(n, file, path);
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
		editNode(res.node, null);
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

}
