package org.kvj.sierra5.plugins.ui.widget.words;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.widget.WidgetProvider;
import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.WidgetController;
import org.kvj.sierra5.plugins.WidgetController.ParserListener;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class WordsWidgetController extends WidgetProvider {

	private static final String TAG = "WordsWidget";
	public static final String BCAST_ACTION = "org.kvj.sierra5plugins.WORDS_ACTION";
	public static final String BCAST_WIDGET_ID = "widgetID";
	private static final String WORD = "word";
	private static final String LINE = "line";
	private static final String COMMAND = "command";
	private static final String COMMAND_LINE_TOGGLE = "line_toggle";
	private static final String COMMAND_SHOW_ALL = "show_all";
	private static final String COMMAND_NEXT = "show_next";
	private static final String COMMAND_FORCE_LOAD = "show_force_load";

	public WordsWidgetController() {
		super(App.getInstance());
	}

	class Word {
		String[] lines;
	}

	class Words {
		List<Word> words = new ArrayList<Word>();
	}

	private static Map<Integer, Words> data = new HashMap<Integer, Words>();

	private Words getConfig(int id) {
		return data.get(id);
	}

	private Words loadConfig(int id, WidgetController controller, Node node, String exp, final int lines) {
		final Words words = new Words();
		controller.parseNode(exp, node, new ParserListener() {

			@Override
			public boolean onItem(boolean finalItem, Map<String, Object> values, Node node) {
				if (finalItem) { // Copy
					// Log.i(TAG, "Load word: " + values);
					Word word = new Word();
					word.lines = new String[lines];
					for (int i = 0; i < lines; i++) { // Copy values
						word.lines[i] = (String) values.get("i" + i);
					}
					words.words.add(word);
				}
				return true;
			}
		});
		data.put(id, words);
		SuperActivity.notifyUser(app, "Words loaded: " + words.words.size());
		Log.i(TAG, "Words loaded: " + words.words.size());
		return words;
	}

	@Override
	protected RemoteViews update(SharedPreferences preferences, int id, Bundle data) {
		WidgetController controller = app.getBean(WidgetController.class);
		if (null == controller) { // No controller
			Log.w(TAG, "No controller");
			return null;
		}
		RemoteViews widget = new RemoteViews(app.getPackageName(), R.layout.words_widget);
		String theme = getString(preferences, R.string.words_theme, R.string.words_themeDefault);
		// Log.i(TAG, "Widget theme: " + theme);
		boolean lite = "lite".equals(theme);
		widget.setImageViewResource(R.id.words_widget_bg, lite ? R.drawable.words_bg_lite : R.drawable.words_bg_dark);
		String command = data.getString(COMMAND);
		// Log.i(TAG, "Command: " + command);
		Words words = getConfig(id);
		boolean needLoadConfig = COMMAND_FORCE_LOAD.equals(command);
		if (null == words) { // No config - need to load
			needLoadConfig = true;
		}
		int lines = getInt(preferences, R.string.words_lines, R.string.words_linesDefault);
		int linesVisible = getInt(preferences, R.string.words_lines_visible, R.string.words_lines_visibleDefault);
		if (COMMAND_LINE_TOGGLE.equals(command)) { // Toggle visibility
			int mask = 1 << data.getInt(LINE, 0);
			// Log.i(TAG, "Toggle visible1: " + mask + ", "
			// + (linesVisible & mask) + ", " + linesVisible);
			if ((linesVisible & mask) != 0) { // Visible
				linesVisible = linesVisible & ~mask;
			} else { // Not visible
				linesVisible = linesVisible | mask;
			}
			// Log.i(TAG, "Toggle visible2: " + mask + ", "
			// + (linesVisible & mask) + ", " + linesVisible);
			setInt(preferences, R.string.words_lines_visible, linesVisible);
		}
		boolean lineVisibility[] = new boolean[lines];
		boolean allLinesVisible = true;
		boolean forceAllLines = COMMAND_SHOW_ALL.equals(command);
		for (int i = 0, mask = 1; i < lines; i++, mask <<= 1) { // Calc
			lineVisibility[i] = (linesVisible & mask) != 0;
			// Log.i(TAG, "Visibility: " + i + ", " + linesVisible + ", " + mask
			// + ", " + lineVisibility[i]);
			if (!lineVisibility[i]) { // At least one not visible
				allLinesVisible = false;
			}
		}
		if (needLoadConfig) { // Need to load
			Node node = controller.findNodeFromPreferences(preferences, "words_id");
			if (null == node) { // No node
				return null;
			}
			String exp = getString(preferences, R.string.words_exp, R.string.words_expDefault);
			if (TextUtils.isEmpty(exp)) { // FIXME: For debugging
				exp = "${#}#{\\s+}${#}#{\\s+}${*}";
			}
			words = loadConfig(id, controller, node, exp, lines);
		}
		if (null == words) { // No words still
			Log.w(TAG, "No words: " + id);
		}
		int currentWord = data.getInt(WORD, -1);
		Random random = new Random(System.currentTimeMillis());
		if (-1 == currentWord) { // Need current word
			currentWord = random.nextInt(words.words.size());
		}
		Word word = words.words.get(currentWord);
		// Create stars
		widget.removeAllViews(R.id.words_widget_icons);
		for (int i = 0; i < lines; i++) { // Create icons
			widget.addView(R.id.words_widget_icons, addIcon(id, i, currentWord, lineVisibility[i]));
		}
		String[] sizes = getString(preferences, R.string.words_sizes, R.string.words_sizesDefault).split(",");
		if (sizes.length != lines) { // Invalid sizes
			sizes = new String[lines];
			for (int i = 0; i < sizes.length; i++) {
				sizes[i] = "15";
			}
		}
		widget.removeAllViews(R.id.words_widget_lines);
		int color = lite ? Color.BLACK : Color.WHITE;
		// Log.i(TAG, "add line:" + lines + ", " + sizes.length + ", "
		// + lineVisibility.length);
		for (int i = 0; i < lines && i < word.lines.length && i < sizes.length && i < lineVisibility.length; i++) { // Create
																													// lines
			widget.addView(R.id.words_widget_lines,
					addLine(word.lines[i], sizes[i], lineVisibility[i] || forceAllLines, color));
		}
		Intent intent = new Intent();
		if (allLinesVisible || forceAllLines) { // All visible - next
			intent.putExtra(COMMAND, COMMAND_NEXT);
		} else {
			intent.putExtra(COMMAND, COMMAND_SHOW_ALL);
			intent.putExtra(WORD, currentWord);
		}
		widget.setOnClickPendingIntent(R.id.words_widget_root, createCommand(id, 1, intent));
		Intent loadIntent = new Intent();
		loadIntent.putExtra(COMMAND, COMMAND_FORCE_LOAD);
		widget.setOnClickPendingIntent(R.id.words_widget_reload, createCommand(id, 2, loadIntent));
		return widget;
	}

	private RemoteViews addLine(String line, String size, boolean visible, int color) {
		RemoteViews views = new RemoteViews(app.getPackageName(), R.layout.words_widget_line);
		views.setTextColor(R.id.words_widget_line, color);
		views.setTextViewText(R.id.words_widget_line, line);
		views.setViewVisibility(R.id.words_widget_line, visible ? View.VISIBLE : View.INVISIBLE);
		float dp = app.getResources().getDisplayMetrics().density;
		try { // Conversion errors
			float sz = Float.parseFloat(size);
			views.setFloat(R.id.words_widget_line, "setTextSize", sz * dp);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return views;
	}

	private RemoteViews addIcon(int id, int index, int word, boolean visible) {
		RemoteViews views = new RemoteViews(app.getPackageName(), R.layout.words_widget_icon);
		if (!visible) { // Not visible
			views.setImageViewResource(R.id.words_widget_icon, R.drawable.line_hide);
		}
		Intent intent = new Intent();
		intent.putExtra(COMMAND, COMMAND_LINE_TOGGLE);
		intent.putExtra(LINE, index);
		intent.putExtra(WORD, word);
		views.setOnClickPendingIntent(R.id.words_widget_icon, createCommand(id, 3 + index, intent));
		return views;
	}

	private PendingIntent createCommand(int id, int type, Intent intent) {
		intent.setAction(BCAST_ACTION);
		intent.putExtra(BCAST_WIDGET_ID, id);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(app, type, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		return pendingIntent;
	}

}
