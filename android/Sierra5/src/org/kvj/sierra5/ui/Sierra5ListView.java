package org.kvj.sierra5.ui;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;
import org.kvj.sierra5.App;
import org.kvj.sierra5.R;
import org.kvj.sierra5.data.Controller;
import org.kvj.sierra5.data.ControllerService;
import org.kvj.sierra5.ui.fragment.EditorViewFragment;
import org.kvj.sierra5.ui.fragment.ListViewFragment;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class Sierra5ListView extends FragmentActivity implements
		ControllerReceiver<Controller> {

	private static final String TAG = "ListView";
	ControllerConnector<App, Controller, ControllerService> conn = null;
	private Controller controller = null;
	private ListViewFragment listViewFragment = null;
	private EditorViewFragment editorViewFragment = null;
	Bundle data = null;

	@Override
	protected void onCreate(Bundle data) {
		super.onCreate(data);
		if (null != data) { // Have data - restore state
			this.data = data;
		} else { // Don't have - new run or from Intent
			if (getIntent() != null && getIntent().getExtras() != null) {
				// Have data in Intent
				this.data = getIntent().getExtras();
			} else { // No data - empty
				data = new Bundle();
			}
		}
		setContentView(R.layout.listview2);
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
			listViewFragment.setController(data, controller);
		}
		if (null != editorViewFragment) { // Have right part
			editorViewFragment.setController(data, controller);
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
}
