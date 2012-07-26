package org.kvj.sierra5.plugins.ui.widget;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.WidgetController;
import org.kvj.sierra5.plugins.service.UIService;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class NodeSelectPreference extends Preference implements
		ControllerReceiver<WidgetController> {

	TextView fileEdit = null, pathEdit = null;
	Button selectButton = null;
	ControllerConnector<App, WidgetController, UIService> cc = null;
	private WidgetController controller = null;

	public NodeSelectPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayoutResource(R.layout.select_node);
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
		return view;
	}

	@Override
	public void onController(WidgetController controller) {
		this.controller = controller;
	}

}
