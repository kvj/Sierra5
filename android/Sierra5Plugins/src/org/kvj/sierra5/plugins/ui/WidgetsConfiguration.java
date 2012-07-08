package org.kvj.sierra5.plugins.ui;

import org.kvj.sierra5.plugins.App;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class WidgetsConfiguration extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		App.getInstance();
	}

}
