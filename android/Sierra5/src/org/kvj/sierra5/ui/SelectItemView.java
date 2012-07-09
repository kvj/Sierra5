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
import org.kvj.sierra5.ui.fragment.ListViewFragment;
import org.kvj.sierra5.ui.fragment.ListViewFragment.EditType;
import org.kvj.sierra5.ui.fragment.ListViewFragment.ListViewFragmentListener;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class SelectItemView extends FragmentActivity implements
		ControllerReceiver<Controller>, ListViewFragmentListener {

	ControllerConnector<App, Controller, ControllerService> conn = null;
	private Controller controller = null;
	private ListViewFragment listViewFragment = null;
	Bundle data = null;

	@Override
	protected void onCreate(Bundle inData) {
		super.onCreate(inData);
		data = SuperActivity.getData(this, inData);
		setContentView(R.layout.select_item);
		listViewFragment = (ListViewFragment) getSupportFragmentManager()
				.findFragmentById(R.id.listview_left);
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
	public void open(Node node) {
		Intent data = new Intent();
		data.putExtra(Constants.SELECT_ITEM_FILE, node.file);
		if (null != node.textPath) { // Have path
			data.putExtra(Constants.SELECT_ITEM_ITEM,
					node.textPath.toArray(new String[0]));
		}
		data.putExtra(Constants.SELECT_ITEM_TYPE, node.type);
		setResult(RESULT_OK, data);
		finish();
	}

	@Override
	public void edit(Node node, EditType editType) {
	}

	@Override
	public void onController(Controller controller) {
		if (null != this.controller) { // Second call
			return;
		}
		this.controller = controller;
		listViewFragment.setController(data, controller, this, true);
	}

}
