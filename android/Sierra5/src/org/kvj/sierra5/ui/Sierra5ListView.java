package org.kvj.sierra5.ui;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;
import org.kvj.bravo7.SuperActivity;
import org.kvj.sierra5.App;
import org.kvj.sierra5.R;
import org.kvj.sierra5.data.Controller;
import org.kvj.sierra5.data.ControllerService;
import org.kvj.sierra5.data.Node;
import org.kvj.sierra5.ui.fragment.EditorViewFragment;
import org.kvj.sierra5.ui.fragment.EditorViewFragment.EditorViewFragmentListener;
import org.kvj.sierra5.ui.fragment.ListViewFragment;
import org.kvj.sierra5.ui.fragment.ListViewFragment.ListViewFragmentListener;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;

public class Sierra5ListView extends FragmentActivity implements
		ControllerReceiver<Controller>, ListViewFragmentListener,
		EditorViewFragmentListener {

	public static final String KEY_EDITOR = "force_editor";

	private static final String TAG = "ListView";
	ControllerConnector<App, Controller, ControllerService> conn = null;
	private Controller controller = null;
	private ListViewFragment listViewFragment = null;
	private EditorViewFragment editorViewFragment = null;
	Bundle data = null;

	@Override
	protected void onCreate(final Bundle inData) {
		super.onCreate(inData);
		if (null != inData) { // Have data - restore state
			this.data = inData;
		} else { // Don't have - new run or from Intent
			if (getIntent() != null && getIntent().getExtras() != null) {
				// Have data in Intent
				this.data = getIntent().getExtras();
			} else { // No data - empty
				this.data = new Bundle();
			}
		}
		boolean isEditor = data.getBoolean(KEY_EDITOR, false);
		if (isEditor) { // Only editor
			setContentView(R.layout.listview_edit_only);
		} else {
			setContentView(R.layout.listview);
		}
		listViewFragment = (ListViewFragment) getSupportFragmentManager()
				.findFragmentById(R.id.listview_left);
		editorViewFragment = (EditorViewFragment) getSupportFragmentManager()
				.findFragmentById(R.id.listview_right);
	}

	@Override
	public void onController(Controller controller) {
		if (null != this.controller) { // Already connected
			return;
		}
		this.controller = controller;
		if (null != listViewFragment) { // Have left part
			listViewFragment.setController(data, controller, this);
		}
		if (null != editorViewFragment) { // Have right part
			editorViewFragment.setController(data, controller, this);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		conn = new ControllerConnector<App, Controller, ControllerService>(
				this, this);
		conn.connectController(ControllerService.class);
	}

	@Override
	protected void onStop() {
		super.onStop();
		conn.disconnectController();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_EDITOR, listViewFragment == null);
		if (null != listViewFragment) { // Have list
			listViewFragment.onSaveState(outState);
		}
		if (null != editorViewFragment) { // Have editor
			editorViewFragment.onSaveState(outState);
		}
	}

	@Override
	public void open(Node node) {
		Intent intent = new Intent(this, Sierra5ListView.class);
		intent.putExtra(ListViewFragment.KEY_ROOT, node.file);
		startActivityFromFragment(listViewFragment, intent, 0);
	}

	private void ifEditorNotChanged(Runnable ok) {
		if (editorViewFragment.isModified()) {
			// Editor modified - ask
			SuperActivity.showQuestionDialog(this, "Discard changes?",
					"Are sure want to discard changes?", ok);
		} else {
			ok.run();
		}
	}

	@Override
	public void edit(final Node node) {
		if (null != editorViewFragment) { // Double pane - load
			ifEditorNotChanged(new Runnable() {

				@Override
				public void run() {
					editorViewFragment.loadNode(
							node.file,
							node.textPath == null ? null : node.textPath
									.toArray(new String[0]), false);
				}
			});
		} else { // Single pane - new Activity
			Intent intent = new Intent(this, Sierra5ListView.class);
			intent.putExtra(KEY_EDITOR, true);
			intent.putExtra(EditorViewFragment.KEY_FILE, node.file);
			if (null != node.textPath) { // Have textPath
				intent.putExtra(EditorViewFragment.KEY_ITEM,
						node.textPath.toArray(new String[0]));
			}
			startActivityFromFragment(listViewFragment, intent, 0);
		}
	}

	@Override
	public void saved(Node node) {
		if (null != listViewFragment) { // Have list - update selection
			Log.i(TAG, "Reselect " + node.file + ", " + node.text + ", "
					+ node.textPath);
			listViewFragment.selectNode(node);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (null != listViewFragment) { // Call list fragment
			listViewFragment.onMenuSelected(item);
		}
		if (null != editorViewFragment) { // Call editor fragment
			editorViewFragment.onMenuSelected(item);
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			if (null != editorViewFragment) {
				ifEditorNotChanged(new Runnable() {

					@Override
					public void run() {
						finish();
					}
				});
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
}
