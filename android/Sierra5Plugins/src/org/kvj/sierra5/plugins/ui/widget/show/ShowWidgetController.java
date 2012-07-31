package org.kvj.sierra5.plugins.ui.widget.show;

import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.root.Root;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.WidgetController;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
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
		Log.i(TAG, "Updating widgets: " + appWidgetIds.length);
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
		Log.i(TAG, "Updating show widget: " + id);
		try { // Rendering exceptions
			String file = prefs.getString("file", "");
			String path = prefs.getString("path", "");
			String[] pathArray = null;
			if (!TextUtils.isEmpty(path)) { // Have path
				pathArray = path.split("/");
			}
			Log.i(TAG, "Loading: " + file + ", " + path);
			Node node = rootService.getNode(file, pathArray, true);
			if (null == node) { // Error
				Log.w(TAG, "Error loading node " + file + ", " + path);
				return;
			}
			RemoteViews widget = new RemoteViews(App.getInstance()
					.getPackageName(), R.layout.show_widget);
			widget.removeAllViews(R.id.show_widget_list);
			renderLine(node, rootService, 0, widget);
			appWidgetManager.updateAppWidget(id, widget);
		} catch (Exception e) {
			Log.e(TAG, "Error rendering:", e);
		}
	}

	private void renderLine(Node node, Root rootService, int level,
			RemoteViews widget) {
		try { // Remote errors
			node.level = level;
			RemoteViews line = rootService.render(node, null, "dark");
			Log.i(TAG, "Render line: " + node.text + ", " + node.collapsed
					+ ", " + node.children);
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
