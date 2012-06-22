package org.kvj.sierra5.ui.fragment;

import org.kvj.bravo7.SuperActivity;
import org.kvj.sierra5.R;
import org.kvj.sierra5.data.Controller;
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

	/**
	 * Loads data
	 */
	public void setController(Bundle data, Controller controller) {
		this.controller = controller;
		boolean rootSet = adapter.setRoot(
				controller.nodeFromPath("/mnt/sdcard/Documents"), false);
		Log.i(TAG, "rootSet: " + rootSet);
		if (rootSet) { // Root set - expand root
			rootFile = adapter.getRoot().file;
			collapseExpand(adapter.getRoot(), null);
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

	private void collapseExpand(Node node, final Boolean forceExpand) {
		AsyncTask<Node, Void, Boolean> task = new AsyncTask<Node, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Node... params) {
				if (null == controller) { // No controller - no refresh
					return false;
				}
				return controller.expand(params[0], forceExpand, null);
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
