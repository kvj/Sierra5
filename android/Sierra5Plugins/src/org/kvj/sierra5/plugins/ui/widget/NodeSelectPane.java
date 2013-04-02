package org.kvj.sierra5.plugins.ui.widget;

import org.kvj.bravo7.SuperActivity;
import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.WidgetController;
import org.kvj.sierra5.plugins.service.UIService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class NodeSelectPane extends SuperActivity<App, WidgetController, UIService> {

	public NodeSelectPane() {
		super(UIService.class);
	}

	private static final String TAG = "NodeSelect";
	TextView fileEdit = null;
	Button selectButton = null;
	SharedPreferences prefs = null;
	String prefix = "";

	@Override
	protected void onCreate(Bundle data) {
		super.onCreate(data);
		Bundle dataProvided = SuperActivity.getData(this, data);
		Log.i(TAG, "node for " + dataProvided.getInt("id"));
		if (dataProvided.containsKey("id")) { // Have id
			prefs = App.getInstance().getWidgetConfig(dataProvided.getInt("id"), dataProvided.getString("type"));
		} else {
			prefs = PreferenceManager.getDefaultSharedPreferences(this);
		}
		if (dataProvided.containsKey("prefix")) { // Have prefix
			prefix = dataProvided.getString("prefix");
		}
		setContentView(R.layout.select_node);
		fileEdit = (TextView) findViewById(R.id.bmark_file);
		selectButton = (Button) findViewById(R.id.bmark_select);
		selectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				selectNode();
			}
		});
		Log.i(TAG, "Load data " + fileEdit + ", " + prefs);
		if (null == prefs) { // Invalid config
			SuperActivity.notifyUser(this, "Invalid config");
			return;
		}
		fileEdit.setText(prefs.getString(prefix + "id", ""));
	};

	protected void selectNode() {
		Intent intent = new Intent(Constants.SELECT_ITEM_NS);
		startActivityForResult(intent, 0);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK && 0 == requestCode) {
			// Our case
			fileEdit.setText(controller.pathFromIntent(data.getExtras(), Constants.SELECT_ITEM_PATH));
		}
	}

	@Override
	public void onBackPressed() {
		if (null == prefs) { // Invalid config
			super.onBackPressed();
			return;
		}
		Editor editor = prefs.edit();
		editor.putString(prefix + "id", fileEdit.getText().toString().trim());
		editor.commit();
		super.onBackPressed();
	}

	public static void showConfig(Context context, Integer widgetID, String type, String prefix) {
		Intent intent = new Intent(context, NodeSelectPane.class);
		if (null != widgetID) { // This is for widget
			intent.putExtra("id", widgetID);
			intent.putExtra("type", type);
		}
		if (null != prefix) { // Have prefix
			intent.putExtra("prefix", prefix);
		}
		context.startActivity(intent);
	}

}
