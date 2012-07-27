package org.kvj.sierra5.plugins.ui.widget;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;
import org.kvj.bravo7.widget.WidgetPreference;
import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.WidgetController;
import org.kvj.sierra5.plugins.service.UIService;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class NodeSelectPreference extends WidgetPreference implements
		ControllerReceiver<WidgetController> {

	TextView fileEdit = null, pathEdit = null;
	Button selectButton = null;
	ControllerConnector<App, WidgetController, UIService> cc = null;
	private WidgetController controller = null;

	public NodeSelectPreference(Context context, AttributeSet attrs) {
		super(context, attrs, R.layout.select_node);
	}

	@Override
	protected void onAttachedToActivity() {
		super.onAttachedToActivity();
		cc = new ControllerConnector<App, WidgetController, UIService>(
				getContext(), this);
		cc.connectController(UIService.class);
	}

	@Override
	protected void onPrepareForRemoval() {
		cc.disconnectController();
		super.onPrepareForRemoval();
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		View view = super.onCreateView(parent);
		fileEdit = (TextView) view.findViewById(R.id.bmark_file);
		pathEdit = (TextView) view.findViewById(R.id.bmark_item);
		selectButton = (Button) view.findViewById(R.id.bmark_select);
		selectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				selectNode();
			}
		});
		fileEdit.setText(getPreferenceManager().getSharedPreferences()
				.getString("file", ""));
		pathEdit.setText(getPreferenceManager().getSharedPreferences()
				.getString("path", ""));
		return view;
	}

	protected void selectNode() {
		Intent intent = new Intent(Constants.SELECT_ITEM_NS);
		activity.startActivityForResult(intent, 0);
	}

	@Override
	public void onController(WidgetController controller) {
		this.controller = controller;
	}

	@Override
	protected boolean onActivityResult(int requestCode, Intent data) {
		if (requestCode != 0) { // Not our case
			return false;
		}
		fileEdit.setText(data.getStringExtra(Constants.SELECT_ITEM_FILE));
		String[] path = data.getStringArrayExtra(Constants.SELECT_ITEM_ITEM);
		if (null != path) { // Create string
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < path.length; i++) { // Add with /
				if (i > 0) { // Not first
					sb.append('/');
				}
				sb.append(path[i]);
			}
			pathEdit.setText(sb);
		} else { // Empty path
			pathEdit.setText("");
		}
		return true;
	}

	@Override
	protected void onFinish() {
		Editor editor = getPreferenceManager().getSharedPreferences().edit();
		editor.putString("file", fileEdit.getText().toString().trim());
		editor.putString("path", pathEdit.getText().toString().trim());
		editor.commit();
		cc.disconnectController();
	}
}
