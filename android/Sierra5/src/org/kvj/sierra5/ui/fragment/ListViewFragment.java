package org.kvj.sierra5.ui.fragment;

import org.kvj.bravo7.SuperActivity;
import org.kvj.sierra5.R;
import org.kvj.sierra5.data.Controller;
import org.kvj.sierra5.data.Controller.SearchNodeResult;
import org.kvj.sierra5.data.Node;
import org.kvj.sierra5.ui.adapter.ListViewAdapter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class ListViewFragment extends Fragment {

	public static final String KEY_ROOT = "list_root";
	public static final String KEY_FILE = "list_file";
	public static final String KEY_ITEM = "list_item";

	private static final String TAG = "ListFragment";
	private ListView listView = null;
	private ListViewAdapter adapter = null;
	protected Controller controller = null;
	private String rootFile = null;

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
		return view;
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
	public void setController(Bundle data, Controller controller) {
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
					controller.nodeFromPath("/mnt/sdcard/Documents"), true);
		}
		Log.i(TAG, "rootSet: " + rootSet);
		if (rootSet) { // Root set - expand root
			collapseExpand(adapter.getRoot(), null, data.getString(KEY_FILE),
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
				collapseExpand(node, true, null, null);
			} else { // Redraw
				adapter.dataChanged();
			}
		} else { // Same item - just toggle
			collapseExpand(adapter.getItem(index), null, null, null);
		}
	}

	private void collapseExpand(Node node, final Boolean forceExpand,
			final String file, final String[] path) {
		Log.i(TAG, "collapse: " + node.text + ", " + forceExpand + ", " + file
				+ ", " + path);
		AsyncTask<Node, Void, Boolean> task = new AsyncTask<Node, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Node... params) {
				if (null == controller) { // No controller - no refresh
					return false;
				}
				if (null == file && null == path) { // Just simple expand
					return controller.expand(params[0], forceExpand, null);
				} else {
					SearchNodeResult res = controller.searchInNode(params[0],
							file, path);
					if (null == res) { // Error expand
						return false;
					}
					Log.i(TAG, "serch result: " + res.found + ", " + res.index);
					adapter.setSelectedIndex(adapter.isShowRoot() ? res.index
							: res.index - 1);
				}
				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				super.onPostExecute(result);
				if (result) { // State changed - notify adapter
					adapter.dataChanged();
				}
			}
		};
		task.execute(node);
	}

}
