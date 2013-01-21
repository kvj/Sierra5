package org.kvj.sierra5.plugins.ui.widget.show;

import java.util.Map;

import org.kvj.bravo7.widget.WidgetUpdateReceiver;
import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.root.Root;
import org.kvj.sierra5.common.theme.Theme;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.WidgetController;
import org.kvj.sierra5.plugins.WidgetController.ParserListener;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

public class ShowWidgetController extends AppWidgetProvider {

	private static final String TAG = "Show widget";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		// Log.i(TAG, "Updating widgets: " + appWidgetIds.length);
		WidgetController controller = App.getInstance().getBean(WidgetController.class);
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

	private void updateUI(int id, final Root rootService, SharedPreferences prefs, AppWidgetManager appWidgetManager) {
		// Log.i(TAG, "Updating show widget: " + id);
		final RemoteViews widget = new RemoteViews(App.getInstance().getPackageName(), R.layout.show_widget);
		int bg = Integer.parseInt(prefs.getString("background", "4"));
		// Log.i(TAG, "bg: " + bg + ", " + prefs.getString("background", "4"));
		int[] colors = { android.R.color.transparent, R.drawable.opacity0, R.drawable.opacity1, R.drawable.opacity2,
				android.R.drawable.screen_background_dark };
		widget.setOnClickPendingIntent(R.id.show_widget_reload,
				WidgetUpdateReceiver.createUpdateIntent(App.getInstance(), id));
		widget.removeAllViews(R.id.show_widget_list);
		try { // Rendering exceptions
			WidgetController controller = App.getInstance().getBean(WidgetController.class);
			final String themeCode = prefs.getString(App.getInstance().getResources().getString(R.string.show_theme),
					App.getInstance().getResources().getString(R.string.show_themeDefault));
			Theme theme = getTheme(controller, themeCode);
			if (null != theme) { // Redefine colors
				if (!theme.dark) { // Light
					colors[1] = R.drawable.opacity0_w;
					colors[2] = R.drawable.opacity1_w;
					colors[3] = R.drawable.opacity2_w;
					colors[4] = android.R.drawable.screen_background_light;
				}
			}
			int bgResource = colors[4];
			switch (bg) {
			case 0:
			case 1:
			case 2:
			case 3:
				bgResource = colors[bg];
				break;
			}
			widget.setImageViewResource(R.id.show_widget_bg, bgResource);
			String file = prefs.getString("file", "");
			String path = prefs.getString("path", "");
			String pattern = prefs.getString(App.getInstance().getResources().getString(R.string.show_pattern), App
					.getInstance().getResources().getString(R.string.show_patternDefault));
			String[] pathArray = null;
			if (!TextUtils.isEmpty(path)) { // Have path
				pathArray = path.split("/");
			}
			// Log.i(TAG, "Loading: " + file + ", " + path);
			Node node = rootService.getNode(file, pathArray, true);
			if (null == node) { // Error
				Log.w(TAG, "Error loading node " + file + ", " + path);
				throw new Exception("Node not found");
			}
			Intent launchIntent = new Intent(Constants.SHOW_EDIT_ITEM_NS);
			launchIntent.putExtra(Constants.LIST_INTENT_ROOT, node.file);
			launchIntent.putExtra(Constants.LIST_INTENT_FILE, node.file);
			launchIntent.putExtra(Constants.LIST_INTENT_ITEM, Node.list2array(node.textPath, new String[0]));
			PendingIntent intent = PendingIntent.getActivity(App.getInstance(), id, launchIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			widget.setOnClickPendingIntent(R.id.show_widget_subroot, intent);
			if (TextUtils.isEmpty(pattern)) { // No pattern
				renderLine(node, rootService, 0, widget, themeCode);
			} else { // Have pattern
				controller.parseNode(pattern, node, new ParserListener() {

					@Override
					public boolean onItem(boolean finalItem, Map<String, Object> values, Node node) {
						if (finalItem) { // Render
							renderLine(node, rootService, 0, widget, themeCode);
						}
						return true;
					}
				});
			}
			widget.addView(R.id.show_widget_list, new RemoteViews(App.getInstance().getPackageName(),
					R.layout.show_widget_last));
		} catch (Exception e) {
			Log.e(TAG, "Error rendering:", e);
		}
		appWidgetManager.updateAppWidget(id, widget);
	}

	private Theme getTheme(WidgetController controller, String code) throws RemoteException {
		if (null == controller.getRootService()) { // No root
			return null;
		}
		Theme[] themes = controller.getRootService().getThemes();
		if (themes.length == 0) { // No themes
			return null;
		}
		for (Theme theme : themes) { // Check code
			if (theme.code.equals(code)) { // Found
				return theme;
			}
		}
		return themes[0];
	}

	private void renderLine(Node node, Root rootService, int level, RemoteViews widget, String themeCode) {
		if (!node.visible) { // Not visible - skip
			return;
		}
		try { // Remote errors
			node.level = level;
			RemoteViews line = rootService.render(node, null, themeCode);
			// Log.i(TAG, "Render line: " + node.text + ", " + node.collapsed
			// + ", " + node.children);
			widget.addView(R.id.show_widget_list, line);
			if (null != node.children && !node.collapsed) { // Have children
				for (Node ch : node.children) { // Render children
					renderLine(ch, rootService, level + 1, widget, themeCode);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Error rendering", e);
		}
	}
}
