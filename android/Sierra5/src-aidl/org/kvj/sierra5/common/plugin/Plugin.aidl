package org.kvj.sierra5.common.plugin;

import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.plugin.FormatSpan;
import org.kvj.sierra5.common.plugin.MenuItemInfo;
import org.kvj.sierra5.common.theme.Theme;

interface Plugin {
	
	int[] getCapabilities();
	String getName();
	int getFormatterCount();
	String getPattern(int index, in Node node, boolean selected);
	FormatSpan[] format(int index, in Theme theme, in Node node, String text, boolean selected);
	MenuItemInfo[] getMenu(int id, in Node node);
	boolean executeAction(int id, inout Node node);
	void parse(inout Node node, in Node parent);
	RemoteViews render(in Node node, in Theme theme, int width);
}