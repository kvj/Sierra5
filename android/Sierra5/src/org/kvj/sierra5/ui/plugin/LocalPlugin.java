package org.kvj.sierra5.ui.plugin;

import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.plugin.Plugin;
import org.kvj.sierra5.common.theme.Theme;

import android.text.SpannableStringBuilder;
import android.view.View;

public interface LocalPlugin extends Plugin {

	public void customize(Theme theme, View view, Node node, SpannableStringBuilder text, boolean selected);
}
