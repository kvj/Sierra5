package org.kvj.sierra5.ui;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;
import org.kvj.bravo7.SuperActivity;
import org.kvj.sierra5.App;
import org.kvj.sierra5.R;
import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.data.Controller;
import org.kvj.sierra5.data.ControllerService;
import org.kvj.sierra5.ui.fragment.EditorViewFragment;
import org.kvj.sierra5.ui.fragment.EditorViewFragment.EditorViewFragmentListener;
import org.kvj.sierra5.ui.fragment.ListViewFragment;
import org.kvj.sierra5.ui.fragment.ListViewFragment.EditType;
import org.kvj.sierra5.ui.fragment.ListViewFragment.ListViewFragmentListener;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

public class Sierra5ListView extends SherlockFragmentActivity implements
		ControllerReceiver<Controller>, ListViewFragmentListener,
		EditorViewFragmentListener {

	private static final String TAG = "ListView";
	ControllerConnector<App, Controller, ControllerService> conn = null;
	private Controller controller = null;
	private ListViewFragment listViewFragment = null;
	private EditorViewFragment editorViewFragment = null;
	Bundle data = null;
	private int progressCount = 0;

	public static final int RESULT_DONE = 102;

	@Override
	protected void onCreate(final Bundle inData) {
		requestWindowFeature(Window.FEATURE_PROGRESS);
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
		boolean isEditor = data.getBoolean(Constants.LIST_FORCE_EDITOR, false);
		if (isEditor) { // Only editor
			setContentView(R.layout.listview_edit_only);
		} else {
			setContentView(R.layout.listview);
		}
		setSupportProgressBarIndeterminateVisibility(false);
		listViewFragment = (ListViewFragment) getSupportFragmentManager()
				.findFragmentById(R.id.listview_left);
		if (null != listViewFragment && !listViewFragment.isInLayout()) {
			listViewFragment = null;
		}
		editorViewFragment = (EditorViewFragment) getSupportFragmentManager()
				.findFragmentById(R.id.listview_right);
		if (null != editorViewFragment && !editorViewFragment.isInLayout()) {
			editorViewFragment = null;
		}
	}

	@Override
	public void onController(Controller controller) {
		if (null != this.controller) { // Already connected
			return;
		}
		this.controller = controller;
		if (null != listViewFragment) { // Have left part
			listViewFragment.setController(data, controller, this, false);
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
		outState.putBoolean(Constants.LIST_FORCE_EDITOR,
				listViewFragment == null);
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
		intent.putExtra(Constants.LIST_INTENT_ROOT, node.file);
		startActivityForResult(intent, RESULT_DONE);
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
	public void edit(final Node node, final EditType editType) {
		if (EditType.Remove == editType) { // Do remove
			if (node == null || Node.TYPE_TEXT != node.type) { // Only for text
				SuperActivity.notifyUser(this, "Invalid item");
				return;
			}
			removeNode(node);
			return;
		}
		if (node == null || Node.TYPE_FOLDER == node.type) {
			// Only for text and file
			SuperActivity.notifyUser(this, "Invalid item");
			return;
		}
		if (null != editorViewFragment) { // Double pane - load
			ifEditorNotChanged(new Runnable() {

				@Override
				public void run() {
					editorViewFragment.loadNode(
							node.file,
							node.textPath == null ? null : node.textPath
									.toArray(new String[0]),
							editType == EditType.Add, null, false);
				}
			});
		} else { // Single pane - new Activity
			Intent intent = new Intent(this, Sierra5ListView.class);
			intent.putExtra(Constants.LIST_FORCE_EDITOR, true);
			intent.putExtra(Constants.EDITOR_INTENT_FILE, node.file);
			intent.putExtra(Constants.EDITOR_INTENT_ADD,
					editType == EditType.Add);
			if (null != node.textPath) { // Have textPath
				intent.putExtra(Constants.EDITOR_INTENT_ITEM,
						node.textPath.toArray(new String[0]));
			}
			startActivityForResult(intent, RESULT_DONE);
		}
	}

	@Override
	protected void onActivityResult(int request, int result, Intent intent) {
		if (result == RESULT_OK) { // Done
			if (listViewFragment != null) { // Have list - refresh
				Node changedNode = null;
				if (null != intent && null != intent.getExtras()) {
					// Have extras
					changedNode = controller.nodeFromPath(intent
							.getStringExtra(Constants.LIST_INTENT_FILE), intent
							.getStringArrayExtra(Constants.LIST_INTENT_ITEM),
							false);
				}
				listViewFragment.refresh(changedNode);
			}
			setResult(RESULT_OK);
		}
	}

	private void removeNode(final Node node) {
		if (null == node || node.type != Node.TYPE_TEXT) { // Invalid node
			return;
		}
		SuperActivity.showQuestionDialog(this, "Remove?", "Remove item ["
				+ node.text + "]?", new Runnable() {

			@Override
			public void run() {
				Node[] actual = controller.actualizeNode(node);
				if (2 != actual.length) { // Not a full result
					SuperActivity.notifyUser(Sierra5ListView.this,
							"Item not found");
					return;
				}
				if (!controller.removeNode(actual[0], actual[1])) { // Error
																	// removing
					SuperActivity.notifyUser(Sierra5ListView.this,
							"Error removing item");
					return;
				}
				if (null != listViewFragment) { // Reload list
					listViewFragment.selectNode(actual[1]);
				}
			}
		});
	}

	@Override
	public void saved(Node node, boolean close) {
		if (null != listViewFragment) { // Have list - update selection
			// Log.i(TAG, "Reselect " + node.file + ", " + node.text + ", "
			// + node.textPath);
			listViewFragment.selectNode(node);
		}
		Intent outData = new Intent();
		outData.putExtra(Constants.LIST_INTENT_FILE, node.file);
		outData.putExtra(Constants.LIST_INTENT_ITEM,
				Node.list2array(node.textPath, new String[0]));
		setResult(RESULT_OK, outData);
		if (close) {
			if (null == listViewFragment) { // Only editor
				// Editor and asked for close - close
				finish();
			} else {
				// Both here - disable editor
				editorViewFragment.disable();
			}
		}
	}

	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// if (null != listViewFragment) { // Call list fragment
	// listViewFragment.onMenuSelected(item);
	// }
	// if (null != editorViewFragment) { // Call editor fragment
	// editorViewFragment.onMenuSelected(item);
	// }
	// return true;
	// }

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

	private synchronized void toggleProgressIndicator(boolean load) {
		if (load) { // Inc counter
			progressCount++;
		} else { // Dec counter
			progressCount--;
		}
		if (load && progressCount == 1) { // Just started
			setSupportProgressBarIndeterminateVisibility(true);
		}
		if (!load && progressCount == 0) { // Just stopped
			setSupportProgressBarIndeterminateVisibility(false);
		}
	}

	@Override
	public void toggleLoad(boolean load) {
		toggleProgressIndicator(load);
	}
}
