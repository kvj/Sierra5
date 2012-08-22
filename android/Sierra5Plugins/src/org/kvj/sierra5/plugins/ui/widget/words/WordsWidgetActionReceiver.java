package org.kvj.sierra5.plugins.ui.widget.words;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WordsWidgetActionReceiver extends BroadcastReceiver {

	private static final String TAG = "WordsAction";

	@Override
	public void onReceive(Context arg0, Intent intent) {
		int id = intent.getIntExtra(WordsWidgetController.BCAST_WIDGET_ID, -1);
		if (id == -1) { // No widget ID
			Log.w(TAG, "Invalid widgetID");
			return;
		}
		WordsWidgetController controller = new WordsWidgetController();
		controller.update(id, intent.getExtras());
	}

}
