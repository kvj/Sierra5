package org.kvj.sierra5.plugins.ui.widget.show;

import org.kvj.bravo7.widget.WidgetUpdateReceiver;
import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.root.Root;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.WidgetController;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

public class ShowWidgetController extends AppWidgetProvider {

	private static final String TAG = "Show widget";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		// Log.i(TAG, "Updating widgets: " + appWidgetIds.length);
		WidgetController controller = App.getInstance().getBean(
				WidgetController.class);
		if (null == controller) { // No controller
			return;
		}
		for (int id : appWidgetIds) { // For every id
			SharedPreferences prefs = App.getInstance().getWidgetConfig(id);
			if (null == prefs || null == controller.getRootService()) {
				// Not enough data
				continue;
			}
			updateUI(id, controller.getRootService(), prefs, appWidgetManager);
		}
	}

	private void updateUI(int id, Root rootService, SharedPreferences prefs,
			AppWidgetManager appWidgetManager) {
		// Log.i(TAG, "Updating show widget: " + id);
		try { // Rendering exceptions
			String file = prefs.getString("file", "");
			String path = prefs.getString("path", "");
			String[] pathArray = null;
			if (!TextUtils.isEmpty(path)) { // Have path
				pathArray = path.split("/");
			}
			// Log.i(TAG, "Loading: " + file + ", " + path);
			Node node = rootService.getNode(file, pathArray, true);
			if (null == node) { // Error
				Log.w(TAG, "Error loading node " + file + ", " + path);
				return;
			}
			RemoteViews widget = new RemoteViews(App.getInstance()
					.getPackageName(), R.layout.show_widget);
			int bg = Integer.parseInt(prefs.getString("background", "4"));
			Log.i(TAG, "bg: " + bg + ", " + prefs.getString("background", "4"));
			int bgResource = android.R.drawable.screen_background_dark;
			switch (bg) {
			case 0:
				bgResource = android.R.color.transparent;
				break;
			case 1:
				bgResource = R.drawable.opacity0;
				break;
			case 2:
				bgResource = R.drawable.opacity1;
				break;
			case 3:
				bgResource = R.drawable.opacity2;
				break;
			}
			widget.setInt(R.id.show_widget_list, "setBackgroundResource",
					bgResource);
			Intent launchIntent = new Intent(Constants.SHOW_EDIT_ITEM_NS);
			launchIntent.putExtra(Constants.LIST_INTENT_ROOT, node.file);
			launchIntent.putExtra(Constants.LIST_INTENT_FILE, node.file);
			launchIntent.putExtra(Constants.LIST_INTENT_ITEM,
					Node.list2array(node.textPath, new String[0]));
			PendingIntent intent = PendingIntent.getActivity(App.getInstance(),
					0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			widget.setOnClickPendingIntent(R.id.show_widget_subroot, intent);
			widget.setOnClickPendingIntent(R.id.show_widget_reload,
					WidgetUpdateReceiver.createUpdateIntent(App.getInstance(),
							id));
			widget.removeAllViews(R.id.show_widget_list);
			renderLine(node, rootService, 0, widget);
			appWidgetManager.updateAppWidget(id, widget);
		} catch (Exception e) {
			Log.e(TAG, "Error rendering:", e);
		}
	}

	private void renderLine(Node node, Root rootService, int level,
			RemoteViews widget) {
		if (!node.visible) { // Not visible - skip
			return;
		}
		try { // Remote errors
			node.level = level;
			RemoteViews line = rootService.render(node, null, "dark");
			// Log.i(TAG, "Render line: " + node.text + ", " + node.collapsed
			// + ", " + node.children);
			widget.addView(R.id.show_widget_list, line);
			if (null != node.children && !node.collapsed) { // Have children
				for (Node ch : node.children) { // Render children
					renderLine(ch, rootService, level + 1, widget);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Error rendering", e);
		}
	}
}
